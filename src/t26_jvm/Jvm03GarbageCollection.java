package t26_jvm;

import helpers.Check;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.ref.Cleaner;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Odśmiecanie pamięci (garbage collection, GC) — kto, kiedy i jak sprząta stertę
 *        (garbage = śmieci; collector = odśmiecacz; reachable = osiągalny; root = korzeń)
 *
 * W SKRÓCIE:
 *   GC usuwa obiekty NIEOSIĄGALNE — takie, do których nie prowadzi żaden łańcuch referencji od korzeni (GC roots).
 *   Nie wiesz, KIEDY to nastąpi, i nie wymusisz tego (System.gc() to tylko prośba). Twoja rola: nie trzymać
 *   niepotrzebnych referencji, zamykać zasoby przez close(), a nie liczyć na finalize().
 *
 * ANALOGIA: sprzątanie biura po godzinach.
 *   Sprzątacz zaczyna od biurek pracowników (korzenie) i idzie po sznurkach do teczek. Wyrzuca wszystko, do czego
 *   nie prowadzi żaden sznurek — nawet dwie teczki powiązane tylko ze sobą nawzajem. Przychodzi wtedy, gdy uzna
 *   za stosowne (np. kosz jest pełny), a nie w chwili, gdy upuścisz kartkę na podłogę.
 *
 * JAK TO DZIAŁA:
 *   1. Znakowanie (mark): od korzeni GC przechodzi po referencjach i zaznacza obiekty osiągalne.
 *      Korzenie: zmienne lokalne i parametry w ramkach aktywnych wątków, pola static załadowanych klas,
 *      aktywne wątki, referencje z kodu natywnego (JNI), obiekty zajęte przez synchronized.
 *   2. Sprzątanie: nieoznaczone = śmieci. Zależnie od kolektora: kopiowanie ocalałych w inne miejsce,
 *      zagęszczanie (compaction) albo zwolnienie regionu w całości.
 *   3. Pokolenia: nowe obiekty trafiają do młodego pokolenia (Eden), te, które przeżyją kilka zbiórek, awansują
 *      do starego (old). Większość obiektów „umiera młodo”, więc częste, tanie sprzątanie Edenu się opłaca.
 *   Kolektory w HotSpot (Java 17):
 *   kolektor   | flaga                   | cechy
 *   G1         | -XX:+UseG1GC            | DOMYŚLNY od Javy 9; sterta w regionach; cel pauzy 200 ms (-XX:MaxGCPauseMillis)
 *   Serial     | -XX:+UseSerialGC        | jeden wątek GC; JVM wybiera go sama na „małej” maszynie (mniej niż
 *              |                         | 2 procesory albo mniej niż ok. 1792 MB pamięci — także limit kontenera)
 *   Parallel   | -XX:+UseParallelGC      | wiele wątków GC, pauzy stop-the-world, nastawiony na przepustowość
 *   ZGC        | -XX:+UseZGC             | produkcyjny od Javy 15; pauzy rzędu milisekundy lub krótsze; w 17 bez pokoleń
 *   Shenandoah | -XX:+UseShenandoahGC    | krótkie pauzy; jest w buildach OpenJDK (np. Temurin), nie ma go w Oracle JDK
 *   Epsilon    | -XX:+UseEpsilonGC       | nic nie sprząta (testy); eksperymentalny: wymaga -XX:+UnlockExperimentalVMOptions
 *
 * SŁÓWKA:
 *   mark = zaznacz; sweep = zamieć; compaction = zagęszczanie; generation = pokolenie; young/old = młode/stare;
 *   survivor = ocalały; promotion = awans; pause = pauza; stop-the-world = „zatrzymaj świat”; hint = wskazówka;
 *   weak = słaby; soft = miękki; phantom = fantomowy; cleaner = sprzątacz; obsolete = przestarzały.
 *
 * ZOBACZ TEŻ: t26_jvm/Jvm01Memory (sterta i typowe wycieki), t26_jvm/Jvm04ToolsProfiling (podgląd GC narzędziami),
 *             t10_exceptions/Exceptions04TryWithResources (close() zamiast liczenia na GC).
 * </pre>
 */
public class Jvm03GarbageCollection {

    public static void main(String[] args) {
        title("Jvm03 — odśmiecanie pamięci (GC)");

        reachability();           // reachability = osiągalność
        obsoleteReferences();     // obsolete references = przestarzałe referencje
        generationsAndCollectors(); // generations and collectors = pokolenia i kolektory
        pausesAndLogs();          // pauses and logs = pauzy i logi
        explicitGcAndCleaner();   // explicit GC and cleaner = jawne GC i sprzątacz
        referenceTypes();         // reference types = rodzaje referencji
        weakHashMap();            // weak hash map = mapa ze słabymi kluczami
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. OSIĄGALNOŚĆ I KORZENIE GC
    // =================================================================================================

    /** heapModel = model sterty: obiekt (klucz) → obiekty, do których trzyma referencje (wartości). */
    static Map<String, List<String>> heapModel() {
        Map<String, List<String>> references = new LinkedHashMap<>();
        references.put("Order", List.of("Customer", "OrderLine"));
        references.put("OrderLine", List.of("Product"));
        references.put("Customer", List.of());
        references.put("Product", List.of());
        references.put("Session", List.of("Cart"));   // Session → Cart → Session: cykl
        references.put("Cart", List.of("Session"));
        references.put("TempText", List.of());
        return references;
    }

    /** reachable = osiągalne: faza znakowania (mark) — przejście wszerz od korzeni po referencjach. */
    static Set<String> reachable(Map<String, List<String>> references, List<String> roots) {
        Set<String> marked = new TreeSet<>();               // marked = zaznaczone; TreeSet = zbiór posortowany → stała kolejność wydruku
        Deque<String> toVisit = new ArrayDeque<>(roots);    // to visit = do odwiedzenia; Deque = kolejka dwustronna
        while (!toVisit.isEmpty()) {
            String current = toVisit.poll();                 // poll = pobierz i usuń pierwszy
            if (marked.add(current)) {                       // false = już zaznaczony → cykl nas nie zapętli
                toVisit.addAll(references.getOrDefault(current, List.of()));
            }
        }
        return marked;
    }

    /**
     * 1. GC nie liczy referencji do obiektu. Zaczyna od korzeni i zaznacza wszystko, do czego dojdzie. Reszta to
     * śmieci — także grupa obiektów, które wskazują tylko na siebie nawzajem (cykl).
     */
    static void reachability() {
        section("1. Osiągalność i korzenie GC — model sterty");

        Map<String, List<String>> heap = heapModel();
        List<String> roots = List.of("Order", "Product");   // Order: zmienna lokalna w main; Product: pole static CACHE
        Set<String> alive = reachable(heap, roots);          // alive = żywe
        Set<String> garbage = new TreeSet<>(heap.keySet());
        garbage.removeAll(alive);
        show("osiągalne (przeżyją GC)", alive);
        show("nieosiągalne (śmieci)", garbage);
        // WYNIK: osiągalne (przeżyją GC) → [Customer, Order, OrderLine, Product]
        // WYNIK: nieosiągalne (śmieci) → [Cart, Session, TempText]

        show("osiągalne, gdy zniknie zmienna order", reachable(heap, List.of("Product")));
        // WYNIK: osiągalne, gdy zniknie zmienna order → [Product]
        // Metoda main skończyła pracę z order (albo order = null) → Order, Customer i OrderLine też stają się
        // śmieciami. Product przeżyje, bo trzyma go pole static — korzeń żyjący zwykle do końca programu.

        // Session i Cart wskazują na siebie, ale żaden korzeń do nich nie prowadzi → śmieci. Liczenie referencji
        // (reference counting) nigdy by ich nie zwolniło (każdy ma licznik 1) — dlatego HotSpot śledzi osiągalność.
        // PUŁAPKA: „ustawiłem null, więc pamięć wróciła” — nie. null tylko odcina JEDNĄ ścieżkę. Obiekt jest
        //   śmieciem dopiero, gdy nie prowadzi do niego ŻADNA ścieżka od korzeni (np. druga referencja w liście
        //   albo w polu static trzyma go dalej).
    }

    // =================================================================================================
    // 2. PRZESTARZAŁE REFERENCJE — NIEOSIĄGALNY TO NIE ZNACZY „USUNIĘTY OD RAZU”
    // =================================================================================================

    /** LeakyStack = cieknący stos: pop() zmniejsza size, ale ZOSTAWIA referencję w tablicy (przestarzała referencja). */
    static final class LeakyStack {
        private final Object[] elements = new Object[16];
        private int size;

        void push(Object element) { elements[size++] = element; }   // push = włóż
        Object pop() { return elements[--size]; }                     // pop = zdejmij; BŁĄD CELOWY: brak = null
        int size() { return size; }

        /** heldReferences = ile referencji wciąż trzyma wewnętrzna tablica (tyle obiektów GC nie może usunąć). */
        long heldReferences() {
            return Arrays.stream(elements).filter(Objects::nonNull).count();
        }
    }

    /**
     * 2. Obiekt jest śmieciem dopiero wtedy, gdy JVM nie ma do niego żadnej ścieżki. Własne struktury danych
     * (stos, pula, bufor) łatwo zostawiają „przestarzałe” referencje, których program już nigdy nie użyje.
     */
    static void obsoleteReferences() {
        section("2. Przestarzałe referencje — nieosiągalny ≠ usunięty od razu");

        LeakyStack stack = new LeakyStack();
        for (String report : List.of("raport-1", "raport-2", "raport-3")) {
            stack.push(report);
        }
        stack.pop();
        stack.pop();
        stack.pop();
        show("size() po trzech pop()", stack.size());
        show("referencje wciąż trzymane w tablicy", stack.heldReferences());
        // WYNIK: size() po trzech pop() → 0
        // WYNIK: referencje wciąż trzymane w tablicy → 3
        // Dla programu stos jest pusty, ale dla GC trzy raporty są OSIĄGALNE (stos → tablica → raport). Gdyby były
        // to duże obiekty, zostałyby w pamięci tak długo jak stos. Naprawisz to w ćwiczeniu 2.

        // Kiedy GC naprawdę sprząta? Gdy uzna to za potrzebne — zwykle gdy zapełni się Eden (zbiórka młodego
        // pokolenia) albo gdy stare pokolenie zajmie określoną część sterty (w G1 na start 45%, potem próg jest
        // dostrajany adaptacyjnie) — wtedy rusza znakowanie starego pokolenia. Obiekt nieosiągalny może więc „leżeć” sekundy albo godziny.
        // Odwrotnie też bywa: JIT może uznać obiekt za nieosiągalny, zanim skończy się metoda, jeśli dalej nie jest
        // już używany. Stąd Reference.reachabilityFence(obj) (Java 9+) = „trzymaj obj osiągalnym do tego miejsca”.
        // DOBRA PRAKTYKA: nie wpisuj x = null „na wszelki wypadek” w zwykłych metodach — zmienna lokalna i tak
        //   znika z końcem metody, a null zaciemnia kod. Zeruj tylko sloty WŁASNYCH struktur (tablica w stosie,
        //   bufor, pula), bo tylko tam GC nie wie, które elementy są „martwe” dla programu.
        // Typowe przyczyny wycieków (Jvm01): statyczne kolekcje, niewypisani słuchacze, cache bez limitu,
        //   ThreadLocal w puli wątków, klasa wewnętrzna/lambda trzymająca obiekt zewnętrzny, przestarzałe
        //   referencje jak wyżej. W zrzucie sterty (Jvm04) szuka się ich po ŚCIEŻCE DO KORZENIA — ćwiczenie 3.
    }

    // =================================================================================================
    // 3. POKOLENIA I KOLEKTORY W HotSpot
    // =================================================================================================

    /**
     * 3. Hipoteza pokoleniowa: większość obiektów żyje bardzo krótko. Dlatego sterta jest podzielona na młode
     * i stare pokolenie, a młode sprząta się często i tanio. Z kodu podejrzysz, jaki kolektor działa (Jvm04).
     */
    static void generationsAndCollectors() {
        section("3. Pokolenia i kolektory w HotSpot");

        //   MŁODE POKOLENIE (young)                          STARE POKOLENIE (old)
        //   ┌───────────────┬───────────┐                    ┌────────────────────────────┐
        //   │ Eden          │ Survivor  │ ── awans (wiek) ──►│ obiekty długowieczne       │
        //   │ nowe obiekty  │ ocalałe   │                    │ (cache, sesje, singletony) │
        //   └───────────────┴───────────┘                    └────────────────────────────┘
        // • Alokacja jest tania: każdy wątek ma w Edenie własny bufor TLAB (thread-local allocation buffer =
        //   bufor alokacji wątku) — nowy obiekt to zwykle tylko przesunięcie wskaźnika.
        // • Zbiórka młodego pokolenia (w G1: „Pause Young”) zatrzymuje wątki aplikacji i KOPIUJE ocalałe obiekty.
        //   Koszt zależy od liczby OCALAŁYCH, nie śmieci — gdy prawie wszystko umarło, zbiórka jest błyskawiczna.
        // • Każda przeżyta zbiórka zwiększa wiek obiektu. Po osiągnięciu progu (maks. 15, -XX:MaxTenuringThreshold;
        //   G1 dobiera go dynamicznie) obiekt awansuje do starego pokolenia.
        // • G1 dzieli stertę na regiony (w Javie 17 po 1–32 MB). Obiekt zajmujący co najmniej pół regionu to obiekt
        //   „humongous” (olbrzymi) — trafia od razu do specjalnych regionów poza Edenem.
        // • Stare pokolenie G1 znakuje współbieżnie (w tle, obok aplikacji), a potem sprząta w zbiórkach mieszanych
        //   (mixed) — najpierw regiony z największą ilością śmieci. Stąd nazwa: Garbage First.
        // • Pełna zbiórka (Pause Full) całej sterty to w G1 awaryjne wyjście (poza jawnym System.gc()) — długa pauza
        //   i sygnał, że G1 nie nadąża: sterta za mała albo wyciek.

        for (GarbageCollectorMXBean collector : ManagementFactory.getGarbageCollectorMXBeans()) {   // kolektory GC
            note("kolektor: " + collector.getName());            // MXBean = obiekt zarządzania JVM (Jvm04)
        }
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {   // memory pool = pula pamięci
            if (pool.getType() == MemoryType.HEAP) {             // tylko pule sterty (bez metaspace, code cache)
                note("pula sterty: " + pool.getName());
            }
        }
        // (wynik zależy od uruchomienia) — zależy od kolektora, czyli od flag i maszyny. Domyślnie (G1):
        //   kolektory „G1 Young Generation” i „G1 Old Generation”; pule „G1 Eden Space”, „G1 Old Gen”, „G1 Survivor Space”.

        // PUŁAPKA: „mam 16 GB RAM, więc na pewno działa G1” — w kontenerze z limitem 1 GB i 1 procesorem JVM
        //   uzna maszynę za małą i wybierze Serial. Nie zgaduj: sprawdź nazwy kolektorów (jak wyżej) albo -Xlog:gc.
        // DOBRA PRAKTYKA: zacznij od domyślnego G1 bez strojenia. Zmieniaj kolektor lub flagi dopiero wtedy, gdy
        //   pomiary (logi GC, JFR — Jvm04) pokażą konkretny problem: za długie pauzy albo za niską przepustowość.
    }

    // =================================================================================================
    // 4. PAUZY STOP-THE-WORLD I LOGI GC
    // =================================================================================================

    /** SAMPLE_GC_LOG = przykładowy log z -Xlog:gc (format jak w G1 w Javie 17; liczby wymyślone do ćwiczeń). */
    static final List<String> SAMPLE_GC_LOG = List.of(
            "[0.015s][info][gc] Using G1",
            "[0.412s][info][gc] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 24M->3M(256M) 2.815ms",
            "[0.980s][info][gc] GC(1) Pause Young (Normal) (G1 Evacuation Pause) 27M->5M(256M) 3.102ms",
            "[1.733s][info][gc] GC(2) Pause Young (Concurrent Start) (G1 Humongous Allocation) 120M->98M(256M) 4.550ms",
            "[1.733s][info][gc] GC(3) Concurrent Mark Cycle",
            "[1.790s][info][gc] GC(3) Pause Remark 101M->101M(256M) 1.204ms",
            "[1.801s][info][gc] GC(3) Pause Cleanup 101M->99M(256M) 0.087ms",
            "[1.812s][info][gc] GC(3) Concurrent Mark Cycle 79.321ms",
            "[2.402s][info][gc] GC(4) Pause Full (System.gc()) 105M->12M(256M) 18.930ms");

    /** pauseMillis = czas pauzy w ms: ostatnie „słowo” linii, np. 2.815ms → 2.815. */
    static double pauseMillis(String line) {
        String last = line.substring(line.lastIndexOf(' ') + 1);
        return Double.parseDouble(last.replace("ms", ""));
    }

    /**
     * 4. Pauza stop-the-world: JVM zatrzymuje wszystkie wątki aplikacji w bezpiecznym punkcie (safepoint), robi
     * swoją pracę i je wznawia. Dla użytkownika to opóźnienie odpowiedzi. Logi GC mówią, ile trwały pauzy.
     */
    static void pausesAndLogs() {
        section("4. Pauzy stop-the-world i logi GC");

        // Jak włączyć:  java -Xlog:gc ...                (jedna linia na zbiórkę)
        //               java -Xlog:gc*:file=gc.log ...   (szczegóły do pliku)
        // Stare flagi -XX:+PrintGC / -XX:+PrintGCDetails są przestarzałe od Javy 9 — zastępuje je -Xlog (unified logging).
        // Format linii:  [czas od startu][poziom][tag] GC(numer) Rodzaj (przyczyna) przed->po(sterta) czas
        int pauses = 0;
        double totalMillis = 0;                      // total millis = łącznie milisekund
        String longest = "";                         // longest = najdłuższa
        double longestMillis = -1;
        for (String line : SAMPLE_GC_LOG) {
            if (!line.contains(" Pause ")) {
                continue;                            // „Using G1” i „Concurrent Mark Cycle” to nie pauzy
            }
            double millis = pauseMillis(line);
            pauses++;
            totalMillis += millis;
            if (millis > longestMillis) {
                longestMillis = millis;
                longest = line.substring(line.indexOf("GC("));
            }
        }
        show("liczba pauz", pauses);
        show("łączny czas pauz", String.format(Locale.ROOT, "%.3f ms", totalMillis));
        show("najdłuższa pauza", longest);
        // WYNIK: liczba pauz → 6
        // WYNIK: łączny czas pauz → 30.688 ms
        // WYNIK: najdłuższa pauza → GC(4) Pause Full (System.gc()) 105M->12M(256M) 18.930ms

        // Jak to czytać: „24M->3M(256M)” = sterta zajęta przed → po zbiórce (pojemność sterty w nawiasie).
        //   Pause Young (Normal) — zwykła zbiórka Edenu; Concurrent Start — przy okazji rusza znakowanie w tle;
        //   Remark i Cleanup — krótkie pauzy w trakcie cyklu znakowania; Concurrent Mark Cycle — praca W TLE (nie pauza).
        // PUŁAPKA: Pause Full (System.gc()) — ktoś w kodzie (albo biblioteka) woła System.gc(), a G1 domyślnie
        //   wykonuje wtedy PEŁNĄ zbiórkę z pauzą. Najdłuższa pauza w tym logu to właśnie ona.
        // DOBRA PRAKTYKA: w produkcji zapisuj logi GC do pliku (-Xlog:gc*:file=...) — kosztują niewiele, a gdy
        //   aplikacja „przymula”, od razu widać, czy winne są pauzy GC, czy coś innego.
    }

    // =================================================================================================
    // 5. System.gc() TO TYLKO PROŚBA; finalize() → Cleaner
    // =================================================================================================

    /** CLEANER = sprzątacz: jeden wątek sprzątający na całą aplikację (Cleaner, Java 9+). */
    private static final Cleaner CLEANER = Cleaner.create();

    /** TempResource = zasób tymczasowy (np. plik). close() sprząta od razu; Cleaner to zabezpieczenie, gdy ktoś zapomni. */
    static final class TempResource implements AutoCloseable {
        private final String name;
        private final Cleaner.Cleanable cleanable;   // cleanable = „do posprzątania”: uchwyt zarejestrowanej akcji

        TempResource(String name, List<String> log) {
            this.name = name;
            this.cleanable = CLEANER.register(this, new CleanupAction(name, log));   // register = zarejestruj
        }

        String name() { return name; }

        @Override
        public void close() {
            cleanable.clean();                        // clean = posprzątaj: wykonuje akcję NAJWYŻEJ raz
        }
    }

    /** CleanupAction = akcja sprzątająca. Klasa static bez referencji do TempResource — to warunek konieczny! */
    private static final class CleanupAction implements Runnable {
        private final String name;
        private final List<String> log;

        CleanupAction(String name, List<String> log) {
            this.name = name;
            this.log = log;
        }

        @Override
        public void run() {
            log.add("sprzątam " + name);
        }
    }

    /**
     * 5. System.gc() tylko SUGERUJE zbiórkę. finalize() jest przestarzałe od Javy 9 — jego następca to Cleaner
     * (Java 9+) oraz, przede wszystkim, jawne close() w try-with-resources.
     */
    static void explicitGcAndCleaner() {
        section("5. System.gc() to tylko prośba; finalize() → Cleaner");

        // System.gc(): dokumentacja mówi „sugeruje” — JVM może go zignorować (-XX:+DisableExplicitGC). W HotSpot
        //   z G1 domyślnie uruchamia PEŁNĄ zbiórkę z pauzą stop-the-world (albo cykl współbieżny przy
        //   -XX:+ExplicitGCInvokesConcurrent). Nie ma gwarancji, że konkretny obiekt zostanie usunięty.
        // finalize(): metoda z Object wołana przez GC „kiedyś” przed usunięciem obiektu. Przestarzała od Javy 9,
        //   a od Javy 18 przeznaczona do usunięcia. Wady: nie wiadomo kiedy (ani czy) się wykona, spowalnia GC
        //   (obiekt przeżywa co najmniej jedną dodatkową zbiórkę), może „wskrzesić” obiekt, wyjątki w niej giną.

        List<String> log = new ArrayList<>();                        // log = dziennik
        try (TempResource resource = new TempResource("raport.tmp", log)) {
            show("używam zasobu", resource.name());
        }
        show("dziennik po try-with-resources", log);
        // WYNIK: używam zasobu → raport.tmp
        // WYNIK: dziennik po try-with-resources → [sprzątam raport.tmp]

        TempResource twice = new TempResource("dane.tmp", log);      // twice = dwa razy
        twice.close();
        twice.close();                               // drugie close() niczego już nie robi
        show("dziennik po dwóch close()", log);
        // WYNIK: dziennik po dwóch close() → [sprzątam raport.tmp, sprzątam dane.tmp]

        // Gdyby ktoś zapomniał close(), Cleaner wykona akcję sam, gdy GC stwierdzi, że TempResource jest
        // nieosiągalny — ale NIE WIADOMO kiedy (może nigdy, jeśli program wcześniej się skończy). To siatka
        // bezpieczeństwa, a nie główny mechanizm.
        // PUŁAPKA: akcja sprzątająca jako lambda używająca this (albo niestatyczna klasa wewnętrzna) trzyma
        //   referencję do TempResource. Cleaner trzyma akcję → obiekt ZAWSZE osiągalny → nigdy nie zostanie
        //   posprzątany. Dlatego CleanupAction jest klasą static i dostaje tylko to, co potrzebne (name, log).
        // DOBRA PRAKTYKA: zasób = AutoCloseable + try-with-resources. Nie wołaj System.gc() w kodzie produkcyjnym
        //   (dodaje pełne pauzy) i nie „udowadniaj” nim niczego w testach — jego skutek zależy od JVM i flag.
    }

    // =================================================================================================
    // 6. REFERENCJE MIĘKKIE, SŁABE I FANTOMOWE
    // =================================================================================================

    /**
     * 6. Pakiet java.lang.ref daje referencje, które NIE zatrzymują obiektu w pamięci (w różnym stopniu).
     * Pokazujemy tylko fakty gwarantowane — to, KIEDY GC wyczyści taką referencję, zależy od uruchomienia.
     */
    static void referenceTypes() {
        section("6. Referencje miękkie, słabe i fantomowe");

        StringBuilder data = new StringBuilder("dane");           // zwykła (silna) referencja
        SoftReference<StringBuilder> soft = new SoftReference<>(data);
        WeakReference<StringBuilder> weak = new WeakReference<>(data);
        ReferenceQueue<StringBuilder> queue = new ReferenceQueue<>();   // kolejka powiadomień o usuniętych obiektach
        PhantomReference<StringBuilder> phantom = new PhantomReference<>(data, queue);

        show("soft.get() == data", soft.get() == data);
        show("weak.get() == data", weak.get() == data);
        show("weak.refersTo(data)", weak.refersTo(data));         // refersTo (Java 16+) = czy wskazuje na
        show("phantom.get()", phantom.get());
        // WYNIK: soft.get() == data → true
        // WYNIK: weak.get() == data → true
        // WYNIK: weak.refersTo(data) → true
        // WYNIK: phantom.get() → null
        // Dopóki istnieje silna referencja (data), GC NIE MOŻE wyczyścić soft ani weak — to jest gwarantowane.
        // phantom.get() ZAWSZE zwraca null: referencja fantomowa służy tylko do powiadomienia (przez kolejkę),
        // że obiekt stał się nieosiągalny. Na niej zbudowany jest Cleaner.

        weak.clear();                                              // clear = wyczyść (ręcznie)
        show("weak.get() po clear()", weak.get());
        // WYNIK: weak.get() po clear() → null
        Reference.reachabilityFence(data);          // data ma być osiągalne aż do tej linii (patrz sekcja 2)

        // rodzaj    | kiedy GC może wyczyścić                                 | typowe użycie
        // silna     | nigdy, dopóki obiekt jest silnie osiągalny              | zwykłe zmienne i pola
        // soft      | gdy brakuje pamięci; wszystkie przed OutOfMemoryError   | (rzadko) pamięć podręczna
        // weak      | przy zbiórce, która zobaczy, że zostały tylko słabe     | WeakHashMap, metadane obiektów
        // phantom   | get() zawsze null; tylko powiadomienie po „śmierci”     | Cleaner, sprzątanie zasobów
        // HotSpot trzyma obiekty miękko osiągalne tym dłużej, im więcej wolnej sterty
        //   (-XX:SoftRefLRUPolicyMSPerMB, domyślnie 1000 ms na każdy wolny MB) — trudno to przewidzieć.
        // PUŁAPKA: if (weak.get() != null) { weak.get().append("x"); } — między dwoma wywołaniami get() GC może
        //   wyczyścić referencję → NullPointerException. Pobierz RAZ do zmiennej lokalnej i sprawdzaj tę zmienną.
        // DOBRA PRAKTYKA: nie buduj cache na SoftReference — sprzątanie jest nieprzewidywalne i obciąża GC przy
        //   braku pamięci. Lepszy cache z limitem rozmiaru (LRU z Jvm01 albo gotowa biblioteka, np. Caffeine).
    }

    // =================================================================================================
    // 7. WeakHashMap — KLUCZE TRZYMANE SŁABO
    // =================================================================================================

    /**
     * 7. WeakHashMap trzyma KLUCZE przez słabe referencje: wpis może zniknąć, gdy klucz przestanie być silnie
     * osiągalny spoza mapy. Wartości trzyma normalnie (silnie).
     */
    static void weakHashMap() {
        section("7. WeakHashMap — klucze trzymane słabo");

        Map<StringBuilder, String> metadata = new WeakHashMap<>();   // metadata = metadane (dane o obiektach)
        StringBuilder sessionKey = new StringBuilder("sesja-1");     // session key = klucz sesji
        metadata.put(sessionKey, "koszyk: 3 produkty");
        show("size() — klucz silnie osiągalny", metadata.size());
        show("get(sessionKey)", metadata.get(sessionKey));
        // WYNIK: size() — klucz silnie osiągalny → 1
        // WYNIK: get(sessionKey) → koszyk: 3 produkty
        Reference.reachabilityFence(sessionKey);
        // Gdy sessionKey przestanie być używany, wpis zniknie „kiedyś” — przy zbiórce, która wyczyści słabą
        // referencję. Kiedy dokładnie — zależy od uruchomienia, więc tego nie wypisujemy.

        Map<String, String> literalKeys = new WeakHashMap<>();       // literal keys = klucze-literały
        literalKeys.put("stała", "nigdy nie zniknie");
        show("klucz-literał po put", literalKeys.get("stała"));
        // WYNIK: klucz-literał po put → nigdy nie zniknie
        // PUŁAPKA: literał napisowy jest w puli i trzymany przez klasę, która go używa → silnie osiągalny →
        //   wpis z takim kluczem praktycznie nigdy nie zniknie. Podobnie Integer z zakresu -128..127 (pula).
        // PUŁAPKA: wartość, która trzyma referencję do SWOJEGO klucza (np. obiekt z polem owner = klucz), sprawia,
        //   że klucz jest zawsze osiągalny (mapa → wartość → klucz) — wpis nie zniknie nigdy.
        // DOBRA PRAKTYKA: WeakHashMap tylko do „doczepiania” danych do cudzych obiektów o nieznanym czasie życia
        //   (klucz porównywany przez equals!). Do cache używaj limitu rozmiaru. WeakHashMap nie jest bezpieczna wątkowo.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • GC usuwa obiekty NIEOSIĄGALNE od korzeni (zmienne lokalne aktywnych wątków, pola static, wątki, JNI).
     *   • Cykle bez połączenia z korzeniem są śmieciami — HotSpot śledzi osiągalność, nie liczy referencji.
     *   • Nieosiągalny ≠ usunięty od razu: GC sprząta, gdy uzna za potrzebne (np. pełny Eden).
     *   • Przestarzałe referencje we własnych strukturach (stos, bufor) zeruj: elements[size] = null.
     *   • Pokolenia: Eden → Survivor → Old; zbiórka młodych kopiuje ocalałe (koszt ∝ ocalałe).
     *   • G1 domyślny od Javy 9 (na małych maszynach Serial); ZGC/Shenandoah = krótkie pauzy; Parallel = przepustowość.
     *   • -Xlog:gc (albo gc*) pokazuje pauzy: „przed->po(sterta) czas”. Pause Full = sygnał alarmowy.
     *   • System.gc() = prośba (w G1 domyślnie pełna pauza); finalize() przestarzałe → close() + Cleaner.
     *   • Soft: czyszczone przy braku pamięci; Weak: przy zbiórce; Phantom: get() == null, tylko powiadomienie.
     *   • WeakHashMap: słabe klucze, silne wartości; literał jako klucz nie zniknie.
     *
     * PYTANIA KONTROLNE:
     *   1. Co to są korzenie GC? Podaj trzy przykłady.
     *   2. Obiekty A i B wskazują tylko na siebie nawzajem, nic innego na nie nie wskazuje. Czy GC je usunie? Dlaczego?
     *   3. Co wypisze:  StringBuilder sb = new StringBuilder("x"); WeakReference<StringBuilder> w = new WeakReference<>(sb);
     *      System.out.println(w.get() == sb); w.clear(); System.out.println(w.get());  ?
     *   4. ZNAJDŹ BŁĄD:  if (cacheRef.get() != null) { return cacheRef.get().value(); }   (cacheRef to WeakReference)
     *   5. ZNAJDŹ BŁĄD:  Object pop() { return elements[--size]; }  — w stosie trzymającym duże obrazy.
     *   6. Czy po System.gc() masz pewność, że nieosiągalne obiekty zniknęły? Co robi System.gc() w HotSpot z G1?
     *   7. Co wypisze:  System.out.println(new PhantomReference<>(obj, queue).get());  — gdy obj jest osiągalny?
     *   8. Dlaczego finalize() zastąpiono przez close() i Cleaner? Podaj dwie wady finalize().
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        String youngLine = SAMPLE_GC_LOG.get(1);
        String fullLine = SAMPLE_GC_LOG.get(8);
        Map<String, List<String>> heapDump = heapDumpModel();
        List<List<String>> expectedPaths = List.of(List.of("Controller", "Service", "Cache", "Entry2", "BigReport"),
                List.of());

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: odzyskane MB", List.of(21, 93), () -> List.of(exercise1(youngLine), exercise1(fullLine)));
        Check.equal("ćw. 2: stos bez przestarzałych referencji", List.of("c", "b", 1L), () -> stackScenario(new ExerciseStack()));
        Check.equal("ćw. 3: ścieżka do korzenia", expectedPaths,
                () -> List.of(exercise3(heapDump, List.of("Controller"), "BigReport"),
                        exercise3(heapDump, List.of("Controller"), "Listener")));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of(21, 93), () -> List.of(solution1(youngLine), solution1(fullLine)));
        Check.equal("ćw. 2 (wzorzec)", List.of("c", "b", 1L), () -> stackScenario(new SolutionStack()));
        Check.equal("ćw. 3 (wzorzec)", expectedPaths,
                () -> List.of(solution3(heapDump, List.of("Controller"), "BigReport"),
                        solution3(heapDump, List.of("Controller"), "Listener")));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): z linii logu GC odczytaj, ile megabajtów odzyskała zbiórka: „przed − po”.
     * Przykład: "... GC(0) Pause Young (Normal) (G1 Evacuation Pause) 24M->3M(256M) 2.815ms" → 21.
     * Podpowiedź: znajdź indeks "M->" (indexOf); liczba „przed” zaczyna się po ostatniej spacji przed nim
     * (lastIndexOf(' ', indeks)), liczba „po” kończy się na "M(". Integer.parseInt zamieni tekst na liczbę.
     */
    static int exercise1(String gcLogLine) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /** StackLike = to, co potrafi stos w ćwiczeniu 2 (push, pop i liczba trzymanych referencji). */
    interface StackLike {
        void push(Object element);

        Object pop();

        long heldReferences();
    }

    /** stackScenario = scenariusz ćwiczenia 2: push a, b, c; dwa pop; zwraca [zdjęty1, zdjęty2, trzymane referencje]. */
    static List<Object> stackScenario(StackLike stack) {
        stack.push("a");
        stack.push("b");
        stack.push("c");
        Object first = stack.pop();
        Object second = stack.pop();
        return List.of(first, second, stack.heldReferences());
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ pop() w ExerciseStack tak, żeby nie zostawiał przestarzałej referencji.
     * Stary sposób (obecny kod, jak LeakyStack z sekcji 2):
     * <pre>{@code
     * public Object pop() { return elements[--size]; }   // slot dalej wskazuje na obiekt
     * }</pre>
     * Nowy sposób: zapamiętaj element w zmiennej lokalnej, wyzeruj slot (elements[size] = null), zwróć element.
     * Test: push a, b, c; dwa pop → [c, b] i w tablicy zostaje 1 referencja (do "a").
     * Podpowiedź: tak robi ArrayList.remove i ArrayDeque.pop w JDK — zajrzyj do ich kodu w IDE (Ctrl+klik).
     */
    static final class ExerciseStack implements StackLike {
        private final Object[] elements = new Object[16];
        private int size;

        @Override
        public void push(Object element) { elements[size++] = element; }

        @Override
        public Object pop() {
            // TODO: przepisz — wyzeruj zwolniony slot
            return elements[--size];
        }

        @Override
        public long heldReferences() { return Arrays.stream(elements).filter(Objects::nonNull).count(); }
    }

    /** heapDumpModel = uproszczony zrzut sterty do ćwiczenia 3: kto trzyma referencje do kogo. */
    static Map<String, List<String>> heapDumpModel() {
        Map<String, List<String>> references = new LinkedHashMap<>();
        references.put("Controller", List.of("Service", "Logger"));
        references.put("Service", List.of("Repository", "Cache"));
        references.put("Cache", List.of("Entry1", "Entry2"));
        references.put("Entry2", List.of("BigReport"));
        references.put("Listener", List.of("BigReport"));   // Listener nie jest osiągalny z korzenia
        return references;
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): narzędzia do analizy zrzutu sterty (VisualVM, Eclipse MAT) pokazują „ścieżkę do
     * korzenia GC” — łańcuch referencji, który trzyma podejrzany obiekt w pamięci. Zwróć NAJKRÓTSZĄ ścieżkę od
     * któregoś korzenia do obiektu target (od korzenia do target włącznie) albo pustą listę, gdy target jest
     * nieosiągalny. Przykład: Controller → Service → Cache → Entry2 → BigReport.
     * Podpowiedź: przejście wszerz jak w reachable() z sekcji 1, ale z mapą „skąd przyszedłem”
     * {@code Map<String, String> cameFrom}. Po dojściu do target cofaj się po cameFrom i odwróć listę.
     */
    static List<String> exercise3(Map<String, List<String>> references, List<String> roots, String target) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(String gcLogLine) {
        int arrow = gcLogLine.indexOf("M->");
        int before = Integer.parseInt(gcLogLine.substring(gcLogLine.lastIndexOf(' ', arrow) + 1, arrow));
        int after = Integer.parseInt(gcLogLine.substring(arrow + 3, gcLogLine.indexOf("M(", arrow)));
        return before - after;
    }

    static final class SolutionStack implements StackLike {
        private final Object[] elements = new Object[16];
        private int size;

        @Override
        public void push(Object element) { elements[size++] = element; }

        @Override
        public Object pop() {
            Object element = elements[--size];
            elements[size] = null;                  // przestarzała referencja usunięta → GC może sprzątnąć element
            return element;
        }

        @Override
        public long heldReferences() { return Arrays.stream(elements).filter(Objects::nonNull).count(); }
    }

    static List<String> solution3(Map<String, List<String>> references, List<String> roots, String target) {
        Map<String, String> cameFrom = new HashMap<>();     // cameFrom = skąd przyszedłem
        Set<String> visited = new TreeSet<>(roots);
        Deque<String> toVisit = new ArrayDeque<>(roots);
        while (!toVisit.isEmpty()) {
            String current = toVisit.poll();
            if (current.equals(target)) {
                List<String> path = new ArrayList<>();
                for (String step = target; step != null; step = cameFrom.get(step)) {
                    path.add(0, step);                      // dokładamy na początek → od korzenia do target
                }
                return path;
            }
            for (String next : references.getOrDefault(current, List.of())) {
                if (visited.add(next)) {
                    cameFrom.put(next, current);
                    toVisit.add(next);
                }
            }
        }
        return List.of();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Punkty startowe znakowania, które JVM uważa za „żywe” z definicji: zmienne lokalne i parametry w ramkach
     *      aktywnych wątków, pola static załadowanych klas, same aktywne wątki, referencje z kodu natywnego (JNI).
     *   2. Tak. GC w HotSpot śledzi osiągalność od korzeni; do A i B żadna ścieżka nie prowadzi, więc to śmieci,
     *      mimo że wskazują na siebie (liczenie referencji by tu zawiodło).
     *   3. "true", potem "null" — dopóki sb jest silną referencją, słaba nie może zostać wyczyszczona przez GC;
     *      clear() czyści ją ręcznie.
     *   4. Między dwoma wywołaniami get() GC może wyczyścić referencję → NullPointerException. Poprawnie:
     *      var value = cacheRef.get(); if (value != null) { return value.value(); }
     *   5. Slot elements[size] dalej wskazuje na zdjęty obraz → obraz jest osiągalny i nie zostanie usunięty
     *      (przestarzała referencja, wyciek). Poprawnie: zapamiętaj element, wyzeruj slot, zwróć element.
     *   6. Nie — to tylko prośba (JVM może ją zignorować, np. -XX:+DisableExplicitGC). W HotSpot z G1 domyślnie
     *      uruchamia pełną zbiórkę z pauzą stop-the-world, więc w kodzie produkcyjnym szkodzi.
     *   7. null — PhantomReference.get() zawsze zwraca null, niezależnie od stanu obiektu.
     *   8. Bo nie wiadomo, kiedy ani czy finalize() się wykona; spowalnia GC (dodatkowa zbiórka), może wskrzesić
     *      obiekt, a wyjątki w niej giną. close() sprząta od razu i przewidywalnie; Cleaner jest tylko siatką
     *      bezpieczeństwa (i nie pozwala wskrzesić obiektu).
     */
    // </editor-fold>
}
