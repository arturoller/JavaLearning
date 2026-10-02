package t20_lombok;

import helpers.Check;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.val;
import lombok.extern.java.Log;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Lombok — reszta zestawu: @Cleanup, @SneakyThrows, @Synchronized, val, @Log, konfiguracja
 *        (checked exception = wyjątek kontrolowany; lock/monitor = zamek/monitor; logger = rejestrator zdarzeń)
 *
 * W SKRÓCIE:
 *   Ostatnia grupa adnotacji Lomboka: {@code @Cleanup} (starszy odpowiednik try-with-resources),
 *   {@code @SneakyThrows} (ukrywa wyjątki kontrolowane — ryzykowne), {@code @Synchronized} (bezpieczniejsza
 *   wersja słowa {@code synchronized}), {@code val} (lokalna zmienna z wnioskowanym typem, final),
 *   {@code @Log} (gotowe pole-rejestrator zdarzeń). Na koniec: jak skonfigurować Lomboka na cały projekt
 *   (lombok.config), jak podejrzeć wygenerowany kod (delombok) i jakie jest ryzyko tej technologii.
 *
 * ANALOGIA: zestaw narzędzi specjalistycznych.
 *   Po młotku i śrubokręcie (poprzednie lekcje: gettery, konstruktory, buildery) przychodzi czas na
 *   narzędzia do zadań szczególnych — lutownicę (@SneakyThrows: działa, ale źle użyta coś spali) i
 *   poziomicę (val: nie buduje niczego sama, tylko pilnuje, żebyś się nie pomylił co do typu).
 *
 * JAK TO DZIAŁA:
 *   @SneakyThrows(IOException.class)
 *   String readConfig() { throw new IOException("brak pliku"); }   ← metoda NIE deklaruje "throws IOException"
 *   // ale w czasie działania NADAL rzuca prawdziwy IOException — kompilator tylko przestaje o niego pytać.
 *
 * SŁÓWKA:
 *   checked exception = wyjątek kontrolowany (kompilator wymaga throws/catch); resource = zasób
 *   (np. plik, połączenie); lock/monitor = zamek/monitor (obiekt, na którym synchronizujemy się);
 *   type inference = wnioskowanie typu; logger = rejestrator zdarzeń; upgrade = aktualizacja.
 *
 * ZOBACZ TEŻ: t10_exceptions (wyjątki kontrolowane a niekontrolowane), t01_basics (var — wnioskowanie
 *             typu wbudowane w Javę), t19_annotations_reflection/Annotations07Processors (czym różni się
 *             Lombok od zwykłego procesora adnotacji — temat rozwinięty w sekcji 6 tej lekcji).
 * </pre>
 */
public class Lombok04Other {

    // ---------------------------------------------------------------------------------------------
    // Klasy używane w lekcji
    // ---------------------------------------------------------------------------------------------

    /** Resource = zasób do zamknięcia (plik, połączenie...). Implementuje AutoCloseable — pasuje do TWR i do @Cleanup. */
    static class Resource implements AutoCloseable {
        private final String name;
        private boolean closed;

        Resource(String name) {
            this.name = name;
        }

        String use() {
            return "używam " + name;
        }

        boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    /** RiskyOperations = operacje, które mogą rzucić wyjątek kontrolowany, ale "schowany" przez @SneakyThrows. */
    static class RiskyOperations {
        @SneakyThrows(IOException.class)
        static String readConfig(boolean fail) {
            if (fail) {
                throw new IOException("brak pliku konfiguracyjnego");
            }
            return "config-ok";
        }
    }

    /**
     * Counter = licznik używany z wielu wątków. {@code @Synchronized} blokuje na WŁASNYM, prywatnym
     * polu (nie na {@code this}) — bezpieczniej niż zwykłe {@code synchronized void metoda()}.
     */
    static class Counter {
        private int count;

        @Synchronized
        void increment() {
            count++;
        }

        @Synchronized
        int get() {
            return count;
        }
    }

    /** Worker = klasa z gotowym rejestratorem zdarzeń (logger) dzięki @Log. Celowo nic nie loguje w runtime (patrz sekcja 5). */
    @Log
    static class Worker {
        String describe() {
            return "Worker gotowy";
        }
    }

    public static void main(String[] args) {
        title("Lombok04 — @Cleanup, @SneakyThrows, @Synchronized, val, @Log, konfiguracja");

        cleanupVsTryWithResources(); // cleanup vs try with resources = @Cleanup kontra try-with-resources
        sneakyThrowsPitfall();       // sneaky throws pitfall = pułapka @SneakyThrows
        synchronizedAnnotation();    // synchronized annotation = adnotacja @Synchronized
        valLocalVariable();          // val local variable = val jako zmienna lokalna
        logFamily();                 // log family = rodzina @Log
        configAndDelombok();         // config and delombok = konfiguracja i delombok
        prosConsUpgradeRisk();       // pros cons upgrade risk = plusy, minusy, ryzyko aktualizacji
        exercises();                 // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. @Cleanup kontra try-with-resources
    // =================================================================================================

    /**
     * 1. {@code @Cleanup} na zmiennej lokalnej owija RESZTĘ metody w try-finally i woła
     * {@code close()} na końcu. To starszy pomysł niż try-with-resources (Java 7) — dziś zwykle
     * lepszy jest TWR: krótszy zasięg zasobu, czytelniejszy, język to rozumie bez biblioteki.
     */
    static void cleanupVsTryWithResources() {
        section("1. @Cleanup kontra try-with-resources");

        Resource viaCleanup = new Resource("plik-cleanup.txt");
        useWithCleanup(viaCleanup);
        show("zamknięty po @Cleanup", viaCleanup.isClosed());
        // WYNIK: zamknięty po @Cleanup → true

        Resource viaTwr = new Resource("plik-twr.txt");
        try (viaTwr) {   // (Java 9+) istniejąca zmienna wprost w try(...) — bez ponownej deklaracji
            viaTwr.use();
        }
        show("zamknięty po try-with-resources", viaTwr.isClosed());
        // WYNIK: zamknięty po try-with-resources → true

        // Lombok generuje (odpowiednik "@Cleanup Resource r = outer; return r.use();"):
        //   Resource r = outer;
        //   try {
        //       return r.use();
        //   } finally {
        //       if (r != null) { r.close(); }
        //   }

        // DOBRA PRAKTYKA: wybieraj try-with-resources. @Cleanup ma pułapki: nie radzi sobie dobrze,
        //   gdy TRZEBA zamknąć kilka zasobów w konkretnej, odwrotnej kolejności przy wyjątku w trakcie
        //   inicjalizacji drugiego z nich, a jego zasięg (do końca METODY) bywa dłuższy niż potrzeba.
        //   TWR ma jasno ograniczony blok {} i obsługuje wiele zasobów oraz tłumione wyjątki poprawnie
        //   "z pudełka".
    }

    private static String useWithCleanup(Resource outer) {
        @Cleanup Resource r = outer;
        return r.use();
    }

    // =================================================================================================
    // 2. @SneakyThrows — pułapka
    // =================================================================================================

    /**
     * 2. @SneakyThrows pozwala RZUCIĆ wyjątek kontrolowany bez deklarowania go w {@code throws} i bez
     * owijania w wyjątek niekontrolowany. Wygodne, ale komplikuje łapanie wyjątku u wywołującego.
     */
    static void sneakyThrowsPitfall() {
        section("2. @SneakyThrows — pułapka");

        show("readConfig(false)", RiskyOperations.readConfig(false));
        // WYNIK: readConfig(false) → config-ok

        expectThrows("readConfig(true)", () -> RiskyOperations.readConfig(true));
        // WYNIK: ✔ readConfig(true) → rzucono IOException: brak pliku konfiguracyjnego

        // Lombok generuje w uproszczeniu (sztuczka z generyką, by ominąć sprawdzanie wyjątków kontrolowanych):
        //   static String readConfig(boolean fail) {
        //       try {
        //           if (fail) throw new IOException("brak pliku konfiguracyjnego");
        //           return "config-ok";
        //       } catch (Throwable t) {
        //           throw Lombok.sneakyThrow(t);   // rzuca TEN SAM wyjątek, ale bez "throws IOException" w sygnaturze
        //       }
        //   }

        // PUŁAPKA: skoro readConfig() nie deklaruje "throws IOException", kod WYWOŁUJĄCY go w INNEJ
        //   klasie nie może napisać "catch (IOException e) { ... }" wokół tego wywołania — kompilator
        //   zgłosi błąd w rodzaju „exception IOException is never thrown in body of corresponding try
        //   statement”, bo z punktu widzenia typów metoda niczego kontrolowanego nie rzuca. W runtime
        //   wyjątek i tak poleci (jak widać wyżej) — można go złapać tylko jako Exception/Throwable,
        //   nie po konkretnym typie kontrolowanym. DLACZEGO to ważne: @SneakyThrows przenosi decyzję
        //   "jak obsłużyć błąd" na wywołującego, ale ODBIERA mu narzędzie kompilatora, które
        //   przypominałoby o obsłudze — używaj oszczędnie, głównie gdy wiesz, że wyjątek i tak nigdy
        //   się nie zdarzy (np. konwersja znanego poprawnego tekstu), a nie jako sposób na "uciszenie"
        //   kompilatora przy prawdziwym ryzyku błędu.
    }

    // =================================================================================================
    // 3. @Synchronized — własny zamek zamiast this
    // =================================================================================================

    /**
     * 3. Zwykłe {@code synchronized void metoda()} blokuje na {@code this} — obiekcie widocznym na
     * zewnątrz, więc KTOKOLWIEK może się na nim zablokować (ryzyko zakleszczenia). {@code @Synchronized}
     * tworzy PRYWATNE pole-zamek, niedostępne spoza klasy.
     */
    static void synchronizedAnnotation() {
        section("3. @Synchronized — własny, prywatny zamek");

        Counter counter = new Counter();
        counter.increment();
        counter.increment();
        counter.increment();
        show("counter.get()", counter.get());
        // WYNIK: counter.get() → 3

        show("pola Counter (posortowane)", fieldNames(Counter.class));
        // WYNIK: pola Counter (posortowane) → $lock, count

        // Lombok generuje (fragment):
        //   private final Object $lock = new Object[0];       // prywatne, niedostępne spoza klasy pole-zamek
        //   void increment() { synchronized ($lock) { count++; } }
        //   int get() { synchronized ($lock) { return count; } }

        // DOBRA PRAKTYKA: @Synchronized > zwykłe "synchronized" na metodzie publicznej, bo nikt z
        //   zewnątrz nie może przypadkiem (albo złośliwie) zablokować się na TYM SAMYM obiekcie, na
        //   którym synchronizuje się Twoja klasa — a to klasyczna przyczyna trudnych do znalezienia
        //   zakleszczeń (deadlocków) w większych systemach.
    }

    // =================================================================================================
    // 4. val — lokalna zmienna z wnioskowanym typem
    // =================================================================================================

    /**
     * 4. {@code val} wnioskuje typ z wyrażenia po prawej (jak {@code var}), ale DODATKOWO czyni
     * zmienną {@code final}. Od Javy 10 mamy wbudowane {@code var} (bez automatycznego final) —
     * {@code val} nadal ma sens tam, gdzie chcesz jawnie podkreślić "ta zmienna się nie zmieni".
     */
    static void valLocalVariable() {
        section("4. val — final + wnioskowanie typu");

        val lista = new ArrayList<String>();
        lista.add("a");
        lista.add("b");
        show("lista zbudowana przez val", lista);
        // WYNIK: lista zbudowana przez val → [a, b]

        // Lombok generuje (val lista = new ArrayList<String>();):
        //   final ArrayList<String> lista = new ArrayList<String>();

        // PUŁAPKA: "lista = new ArrayList<>();" (ponowne przypisanie ZMIENNEJ lista) by się NIE
        //   skompilowało — val czyni zmienną final. WOLNO za to modyfikować OBIEKT, na który wskazuje
        //   (lista.add(...) działa, bo final dotyczy referencji, nie zawartości listy — identycznie
        //   jak przy zwykłym "final ArrayList<String> lista = ...").

        // CIEKAWOSTKA: Lombok ma też "lombok.var" (wnioskowany typ, ZMIENNA, bez final) — od Javy 10,
        //   gdy język dostał wbudowane "var" o identycznym znaczeniu, lombok.var stał się w praktyce
        //   zbędny (patrz t01_basics — tam poznajesz wbudowane "var"). "val" wciąż bywa używane, bo
        //   wbudowana Java nie ma odpowiednika "final + wnioskowany typ" w jednym słowie.
    }

    // =================================================================================================
    // 5. @Log — rodzina adnotacji logujących
    // =================================================================================================

    /**
     * 5. {@code @Log} (pakiet {@code lombok.extern.java}) dodaje pole {@code log} typu
     * {@code java.util.logging.Logger} (JUL — Java Util Logging, wbudowany w JDK). Domyślny handler
     * JUL pisze na STDERR — w tej lekcji NIE wywołujemy log.info/warning w runtime, żeby nic nie
     * trafiło na stderr (reguła kursu); samo ISTNIENIE pola wystarczy jako dowód.
     */
    static void logFamily() {
        section("5. @Log — gotowy rejestrator zdarzeń (JUL)");

        show("pola Worker (posortowane)", fieldNames(Worker.class));
        // WYNIK: pola Worker (posortowane) → log

        show("typ pola log", fieldType(Worker.class, "log"));
        // WYNIK: typ pola log → Logger

        // Lombok generuje:
        //   private static final java.util.logging.Logger log =
        //           java.util.logging.Logger.getLogger(Worker.class.getName());

        // PUŁAPKA: domyślny ConsoleHandler korzenia JUL pisze na System.err. Wywołanie log.info(...)
        //   "jak leci" w aplikacji konsolowej trafia na stderr — tu by to złamało regułę kursu i
        //   zepsuło weryfikację. BEZPIECZNE wyjście: nie logować w kodzie demonstracyjnym (jak tutaj),
        //   albo świadomie podłączyć WŁASNY java.util.logging.Handler piszący na System.out i wywołać
        //   log.setUseParentHandlers(false) (odłącza domyślny handler korzenia) — w kodzie produkcyjnym
        //   i tak najczęściej konfiguruje się logowanie scentralizowanie (plik logging.properties albo
        //   framework), a nie ręcznie w każdej klasie.

        // RODZINA @Log (wymagają różnych bibliotek na classpath — tu dostępne jest tylko JUL):
        //   @Log            → java.util.logging.Logger          (JUL, wbudowany w JDK — jedyny gotowy "z pudełka")
        //   @Slf4j          → org.slf4j.Logger                   (wymaga zależności slf4j-api)
        //   @Log4j2         → org.apache.logging.log4j.Logger    (wymaga zależności log4j-api)
        //   @CommonsLog     → org.apache.commons.logging.Log     (wymaga Apache Commons Logging)
        //   @Flogger        → com.google.common.flogger.FluentLogger (wymaga Flogger)
        // W realnym projekcie (np. Spring Boot — patrz SpringLearning) niemal zawsze wybiera się @Slf4j,
        //   bo SLF4J jest tylko FASADĄ — dociera do tego, co już skonfigurowane w projekcie (Logback itd.).
    }

    // =================================================================================================
    // 6. lombok.config i delombok
    // =================================================================================================

    /**
     * 6. Lombok czyta na starcie plik {@code lombok.config} (zwykły plik tekstowy w katalogu modułu) —
     * ustawienia obowiązują dla całego katalogu (i podkatalogów, chyba że ktoś ustawi stopBubbling).
     */
    static void configAndDelombok() {
        section("6. lombok.config i delombok (tylko w komentarzu — nie tworzymy pliku)");

        note("Przykładowy lombok.config (NIE tworzymy go w tym projekcie — wpłynąłby na cały katalog):");
        note("  config.stopBubbling = true — ten plik jest \"korzeniem\" (nie szukaj wyżej w drzewie katalogów)");
        note("  lombok.accessors.chain = true — settery zwracają \"this\" zamiast void (wywołania łańcuchowe)");
        note("  lombok.addLombokGeneratedAnnotation = true — oznacza generowany kod adnotacją @Generated (dla narzędzi pokrycia kodu)");
        // WYNIK: ℹ Przykładowy lombok.config (NIE tworzymy go w tym projekcie — wpłynąłby na cały katalog):
        // WYNIK: ℹ   config.stopBubbling = true — ten plik jest "korzeniem" (nie szukaj wyżej w drzewie katalogów)
        // WYNIK: ℹ   lombok.accessors.chain = true — settery zwracają "this" zamiast void (wywołania łańcuchowe)
        // WYNIK: ℹ   lombok.addLombokGeneratedAnnotation = true — oznacza generowany kod adnotacją @Generated (dla narzędzi pokrycia kodu)

        // DELOMBOK: w IntelliJ (z wtyczką Lombok, włączonym annotation processing — patrz Lombok01Accessors,
        //   sekcja 1) akcja "Refactor → Delombok" (albo z menu kontekstowego pliku) tworzy KOPIĘ klasy
        //   z rozwiniętymi adnotacjami jako zwykły kod źródłowy — dokładnie to, co dotąd pisaliśmy ręcznie
        //   w komentarzach "Lombok generuje". Świetne do nauki nowej adnotacji albo do debugowania
        //   zaskakującego zachowania — ale wygenerowanego pliku nie zostawia się w repozytorium.
    }

    // =================================================================================================
    // 7. Plusy, minusy, polityka zespołu, ryzyko aktualizacji
    // =================================================================================================

    /** 7. Podsumowanie decyzyjne: kiedy sięgać po Lomboka, kiedy po rekordy, i na co uważać przy aktualizacjach JDK. */
    static void prosConsUpgradeRisk() {
        section("7. Plusy, minusy i ryzyko aktualizacji JDK");

        note("PLUSY: mniej kodu szablonowego, mniej literówek w equals/hashCode, czytelne buildery.");
        note("MINUSY: kod \"niewidoczny\" w źródle (trzeba znać adnotacje), zależność od wtyczki IDE,");
        note("        ostrzeżenia/błędy Lomboka bywają mniej czytelne niż zwykłe błędy kompilacji.");
        note("POLITYKA ZESPOŁU: warto spisać, KTÓRYCH adnotacji używacie (np. @Getter/@Setter/@Builder tak,");
        note("        @SneakyThrows i @Cleanup nie) — spójność ważniejsza niż „wszystko, co Lombok potrafi”.");
        // WYNIK: ℹ PLUSY: mniej kodu szablonowego, mniej literówek w equals/hashCode, czytelne buildery.
        // WYNIK: ℹ MINUSY: kod "niewidoczny" w źródle (trzeba znać adnotacje), zależność od wtyczki IDE,
        // WYNIK: ℹ         ostrzeżenia/błędy Lomboka bywają mniej czytelne niż zwykłe błędy kompilacji.
        // WYNIK: ℹ POLITYKA ZESPOŁU: warto spisać, KTÓRYCH adnotacji używacie (np. @Getter/@Setter/@Builder tak,
        // WYNIK: ℹ         @SneakyThrows i @Cleanup nie) — spójność ważniejsza niż „wszystko, co Lombok potrafi”.

        // REKORDY JAKO WSPÓŁCZESNA ALTERNATYWA: dla prostych, niezmiennych nośników danych bez potrzeby
        //   dziedziczenia rekord (patrz t09_records, Lombok03DataValueBuilder sekcja 4) jest dziś często
        //   LEPSZYM pierwszym wyborem niż @Value — zero zależności, wsparcie języka i narzędzi (dekonstrukcja
        //   we wzorcach, Java 21+). Lombok pozostaje mocny tam, gdzie rekord nie wystarcza: klasy zmienne
        //   (@Data), dziedziczenie, @Builder/@Singular/@With na złożonych obiektach.

        // RYZYKO AKTUALIZACJI: Lombok NIE jest zwykłym procesorem adnotacji (patrz
        //   t19_annotations_reflection/Annotations07Processors) — podłącza się do WEWNĘTRZNYCH,
        //   niepublicznych klas kompilatora javac, żeby modyfikować drzewo składni w locie. Nowa wersja
        //   JDK może zmienić te wewnętrzne klasy i ZEPSUĆ starą wersję Lomboka (albo wymagać dodatkowych
        //   flag JVM w rodzaju --add-opens, żeby w ogóle dostać się do modułu kompilatora). DLACZEGO to
        //   ważne: po KAŻDEJ aktualizacji JDK projekt trzeba najpierw skompilować i przetestować z nową
        //   wersją Lomboka, zanim się na nią przesiądzie cały zespół — nie jest to automatyczne jak przy
        //   zwykłych bibliotekach.
    }

    // ---------------------------------------------------------------------------------------------
    // Pomocniki: posortowane nazwy pól oraz typ wybranego pola (dowód refleksją, patrz
    // t19_annotations_reflection/Annotations03ReflectionBasics).
    // ---------------------------------------------------------------------------------------------

    private static String fieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(Field::getName)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private static String fieldType(Class<?> type, String fieldName) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(f -> f.getName().equals(fieldName))
                .map(f -> f.getType().getSimpleName())
                .findFirst()
                .orElse("(brak pola)");
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @Cleanup var = ...; → try-finally z close() na końcu METODY. Preferuj try-with-resources
     *     (krótszy, jasny zasięg, lepsza obsługa wielu zasobów i tłumionych wyjątków).
     *   • @SneakyThrows(X.class) → rzuca X bez "throws X" w sygnaturze. Wywołujący NIE może złapać X
     *     po typie (tylko Exception/Throwable) — używaj oszczędnie.
     *   • @Synchronized → synchronizuje na PRYWATNYM polu ($lock/$LOCK), nie na this/Class — bezpieczniej
     *     niż zwykłe "synchronized" na metodzie publicznej.
     *   • val → final + wnioskowany typ (lokalnie). lombok.var jest dziś zbędne (mamy wbudowane var, Java 10+).
     *   • @Log (JUL) → pole "log" typu Logger. SLF4J/Log4j2/Commons Logging wymagają osobnej zależności.
     *     Domyślny handler JUL pisze na stderr — uważaj przy logowaniu w kodzie demonstracyjnym/testach.
     *   • lombok.config → ustawienia dla całego katalogu (accessors.chain, addLombokGeneratedAnnotation...).
     *   • delombok (IntelliJ) → podgląd wygenerowanego kodu jako zwykłego źródła, nie do commitowania.
     *   • Rekord > @Value, gdy nie trzeba dziedziczenia/buildera. Po aktualizacji JDK: zawsze przetestuj
     *     kompilację z nową wersją Lomboka PRZED przesiadką zespołu.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego try-with-resources jest dziś zwykle lepszym wyborem niż @Cleanup?
     *   2. Co wypisze:  RiskyOperations.readConfig(false)  ?
     *   3. ZNAJDŹ BŁĄD: ktoś w innej klasie pisze
     *      "try { RiskyOperations.readConfig(true); } catch (IOException e) { ... }" i dostaje błąd
     *      kompilacji. Jaki i dlaczego?
     *   4. Na czym (jakim obiekcie) synchronizuje się metoda oznaczona @Synchronized, a na czym zwykła
     *      metoda "synchronized"?
     *   5. Co wypisze:  val x = 10; x = 20; System.out.println(x);  — a raczej, co się stanie?
     *   6. Dlaczego w tej lekcji NIE wywołujemy log.info(...) w czasie działania programu?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: zamknięcie przez try-with-resources", true, () -> exercise1("dane.csv"));
        Check.equal("ćw. 2: bezpieczne odczytanie configu", "config-ok", () -> exercise2(false));
        Check.equal("ćw. 2b: obsłużony błąd configu", "BŁĄD: brak pliku konfiguracyjnego", () -> exercise2(true));
        Check.equal("ćw. 3 (PRZEPISZ): counter.increment() zamiast ręcznej synchronizacji", 5, () -> exercise3(5));
        Check.equal("ćw. 4: Worker ma dokładnie jedno pole Logger", true, () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", true, () -> solution1("dane.csv"));
        Check.equal("ćw. 2 (wzorzec)", "config-ok", () -> solution2(false));
        Check.equal("ćw. 2b (wzorzec)", "BŁĄD: brak pliku konfiguracyjnego", () -> solution2(true));
        Check.equal("ćw. 3 (wzorzec)", 5, () -> solution3(5));
        Check.equal("ćw. 4 (wzorzec)", true, () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): utwórz {@code new Resource(name)}, użyj go przez try-with-resources
     * (BEZ @Cleanup — ćwiczymy nowoczesny sposób) i zwróć, czy jest zamknięty po bloku.
     */
    static boolean exercise1(String name) {
        // TODO: twoje rozwiązanie
        return false;
    }

    /**
     * ĆWICZENIE 2 (łatwe): wywołaj RiskyOperations.readConfig(fail). Jeśli rzuci wyjątek — złap go
     * (catch Exception) i zwróć "BŁĄD: " + komunikat wyjątku. W przeciwnym razie zwróć wynik.
     * Podpowiedź: mimo @SneakyThrows metodę trzeba łapać jako Exception (nie IOException) — patrz sekcja 2.
     */
    static String exercise2(boolean fail) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ): stary, ręczny sposób synchronizacji wyglądał tak (poza klasą
     * Counter, na osobnym, DZIELONYM obiekcie zamka — łatwo o pomyłkę i zakleszczenie):
     * <pre>{@code
     *     Object lock = new Object();
     *     synchronized (lock) { count++; }   // trzeba pamiętać o "lock" przy KAŻDYM dostępie
     * }</pre>
     * Przepisz to na użycie {@code Counter} (ma już @Synchronized wewnątrz) — wywołaj increment() n
     * razy i zwróć wynik get().
     */
    static int exercise3(int n) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): refleksją sprawdź, czy Worker.class ma DOKŁADNIE jedno pole i czy
     * jego typ to "Logger". Podpowiedź: Worker.class.getDeclaredFields() (unchecked, w przeciwieństwie
     * do getDeclaredField(String), które rzuca kontrolowany NoSuchFieldException).
     */
    static boolean exercise4() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(String name) {
        Resource r = new Resource(name);
        try (r) {
            r.use();
        }
        return r.isClosed();
    }

    static String solution2(boolean fail) {
        try {
            return RiskyOperations.readConfig(fail);
        } catch (Exception e) {
            return "BŁĄD: " + e.getMessage();
        }
    }

    static int solution3(int n) {
        Counter counter = new Counter();
        for (int i = 0; i < n; i++) {
            counter.increment();
        }
        return counter.get();
    }

    static boolean solution4() {
        Field[] fields = Worker.class.getDeclaredFields();
        return fields.length == 1 && fields[0].getType().getSimpleName().equals("Logger");
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. try-with-resources ma krótszy, jasno ograniczony zasięg (blok {}), obsługuje WIELE zasobów
     *      i poprawnie łączy (tłumi) wyjątek z close() z wyjątkiem z bloku — @Cleanup trzyma zasób
     *      żywy do końca całej metody i gorzej radzi sobie z kilkoma zasobami naraz.
     *   2. "config-ok".
     *   3. "exception IOException is never thrown in body of corresponding try statement" — readConfig
     *      nie deklaruje "throws IOException" (bo @SneakyThrows je ukrywa), więc z punktu widzenia
     *      kompilatora nic kontrolowanego tam nie leci; trzeba złapać Exception/Throwable zamiast IOException.
     *   4. @Synchronized synchronizuje się na WŁASNYM, prywatnym polu ($lock) niewidocznym spoza klasy;
     *      zwykłe "synchronized" na metodzie instancyjnej synchronizuje się na "this" — obiekcie, na
     *      którym może się zablokować też kod z zewnątrz.
     *   5. Błąd kompilacji — "x" utworzone przez val jest final, ponowne przypisanie "x = 20" jest niedozwolone.
     *   6. Domyślny handler java.util.logging pisze na System.err (stderr), a reguła kursu (i weryfikator)
     *      zabrania wypisywania czegokolwiek na stderr — dowodzimy istnienia pola "log" refleksją,
     *      zamiast je realnie wywoływać.
     */
    // </editor-fold>
}
