package t21_concurrency;

import helpers.Check;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wyścigi (race conditions) i jak je naprawić
 *        (race condition = wyścig / sytuacja wyścigu, shared mutable state = wspólny zmienny stan,
 *         lost update = utracona aktualizacja, atomic = niepodzielny)
 *
 * W SKRÓCIE:
 *   Gdy kilka wątków czyta i zmienia TEN SAM obiekt bez synchronizacji, wynik zależy od tego, jak przeplotą się
 *   ich kroki — to wyścig. Najprostszy przykład: count++ wykonywane przez dwa wątki gubi część zwiększeń.
 *   Naprawy: synchronized, klasy Atomic*, LongAdder, a najlepiej — brak współdzielenia (niezmienność, zamknięcie).
 *
 * ANALOGIA:
 *   Dwie osoby aktualizują ten sam zeszyt z liczbą gości. Każda: czyta liczbę (10), liczy w głowie +1, zapisuje (11).
 *   Jeśli obie przeczytały „10”, zanim któraś zapisała, w zeszycie będzie 11, choć przyszło dwóch gości.
 *   Rozwiązanie: zeszyt leży w pokoju z jednym kluczem — kto ma klucz, ten pisze (synchronized).
 *
 * JAK TO DZIAŁA:
 *   • count++ to NIE jedna operacja, tylko trzy: ODCZYT → DODANIE → ZAPIS (read-modify-write).
 *     Kod bajtowy dla pola obiektu (javap -c):  getfield count / iconst_1 / iadd / putfield count.
 *   • Przeplot, który gubi zwiększenie:
 *       wątek A: odczyt 0 ............... zapis 1
 *       wątek B: ........ odczyt 0 .............. zapis 1      → wynik 1 zamiast 2
 *   • Dwa typowe wzorce wyścigu:
 *       1) read-modify-write: count++, saldo = saldo - kwota;
 *       2) check-then-act („sprawdź, potem działaj”): if (!map.containsKey(k)) map.put(k, v);
 *   • Naprawy:
 *       synchronized       — blokada (monitor) obiektu: w danej chwili tylko jeden wątek w sekcji krytycznej;
 *       AtomicInteger/Long — niepodzielne operacje na jednej zmiennej (sprzętowe compare-and-set);
 *       LongAdder          — licznik do bardzo częstych zwiększeń z wielu wątków;
 *       niezmienność / zamknięcie w wątku — nie ma wspólnego zmiennego stanu, nie ma problemu.
 *
 * SŁÓWKA:
 *   race = wyścig; shared = wspólny; mutable = zmienny; immutable = niezmienny; lost update = utracona aktualizacja;
 *   check-then-act = sprawdź, potem działaj; critical section = sekcja krytyczna; monitor = monitor (blokada obiektu);
 *   atomic = niepodzielny; compare and set = porównaj i ustaw; adder = sumator; confinement = zamknięcie (w wątku);
 *   visibility = widoczność; volatile = ulotny (modyfikator widoczności).
 *
 * ZOBACZ TEŻ: t21_concurrency/Concurrency01Threads (wątki, join),
 *             t21_concurrency/Concurrency03Locks (blokady jawne, zakleszczenie),
 *             t21_concurrency/Concurrency06ConcurrentCollections (ConcurrentHashMap — atomowe compute/merge),
 *             t21_concurrency/Concurrency10MemoryModel (widoczność i volatile w szczegółach),
 *             t16_streams/Streams17SideEffectsPitfalls (efekty uboczne w strumieniach równoległych).
 * </pre>
 */
public class Concurrency02RaceConditions {

    static final int THREADS = 2;               // liczba wątków w pomiarach
    static final int PER_THREAD = 100_000;      // per thread = na wątek

    public static void main(String[] args) throws Exception {
        title("Concurrency02 — wyścigi: utracone aktualizacje i naprawy");

        sharedMutableState();      // shared mutable state = wspólny zmienny stan
        forcedLostUpdate();        // forced lost update = wymuszona utracona aktualizacja
        checkThenAct();            // check then act = sprawdź, potem działaj
        synchronizedFix();         // synchronized fix = naprawa przez synchronized
        atomicVariables();         // atomic variables = zmienne atomowe
        longAdderCounter();        // long adder counter = licznik LongAdder
        noSharingNoProblem();      // no sharing, no problem = brak współdzielenia, brak problemu
        wrongLockObject();         // wrong lock object = blokada na złym obiekcie
        visibilityPreview();       // visibility preview = zapowiedź problemu widoczności
        exercises();               // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. WSPÓLNY ZMIENNY STAN — LICZNIK BEZ OCHRONY
    // =================================================================================================

    /** Licznik bez żadnej ochrony — NIE jest bezpieczny wątkowo. */
    static class UnsafeCounter {   // unsafe counter = niebezpieczny licznik
        private int count;         // count = liczba

        void increment() {         // increment = zwiększ
            count++;               // trzy kroki: odczyt, +1, zapis — wątki mogą się wcisnąć pomiędzy
        }

        int get() {
            return count;
        }
    }

    /**
     * 1. Dwa wątki, każdy 100 000 razy {@code count++}. Wypisujemy tylko to, co PEWNE: wynik nie przekroczy 200 000.
     * Ile dokładnie zgubimy — zależy od maszyny, obciążenia i kompilatora JIT (może być nawet 0 zgubionych).
     */
    static void sharedMutableState() throws InterruptedException {
        section("1. Wspólny zmienny stan — count++ z dwóch wątków");

        UnsafeCounter counter = new UnsafeCounter();
        runInThreads(THREADS, () -> {
            for (int i = 0; i < PER_THREAD; i++) {
                counter.increment();
            }
        });
        int result = counter.get();
        show("oczekiwano", THREADS * PER_THREAD);
        // WYNIK: oczekiwano → 200000
        show("wynik ≤ 200000", result <= THREADS * PER_THREAD);
        // WYNIK: wynik ≤ 200000 → true
        // (wynik zależy od uruchomienia) — typowo coś w rodzaju 112 873 albo 163 402; czasem dokładnie 200 000.
        // Ten sam program raz działa, raz nie — i właśnie dlatego wyścigi są tak groźne: testy często przechodzą.

        // Dlaczego gubimy zwiększenia: count++ = odczyt → +1 → zapis. Jeśli dwa wątki odczytają tę samą wartość,
        // oba zapiszą tę samą wartość + 1, a jedno zwiększenie znika. Kod bajtowy pola obiektu:
        //     aload_0; dup; getfield count; iconst_1; iadd; putfield count
        // Między getfield a putfield drugi wątek może zrobić cokolwiek.
        // PUŁAPKA: „to tylko jedna linijka, więc jest atomowa” — NIE. Atomowość nie zależy od liczby linijek kodu
        // źródłowego, tylko od tego, czy operacja jest niepodzielna dla JVM. Pojedynczy odczyt lub zapis pola int
        // jest niepodzielny, ale count++, count += 5 i saldo = saldo - kwota — już nie.
    }

    // =================================================================================================
    // 2. WYMUSZONY PRZEPLOT — UTRACONA AKTUALIZACJA ZA KAŻDYM RAZEM
    // =================================================================================================

    /** Pole odczytywane i zapisywane przez dwa wątki w sekcji 2. */
    static int visitors;   // visitors = goście (licznik)

    /**
     * 2. Żeby POKAZAĆ utratę deterministycznie, rozbijamy {@code visitors++} na odczyt i zapis, a między nie wstawiamy
     * barierę ({@code CyclicBarrier} = bariera cykliczna, Concurrency07Synchronizers): oba wątki muszą najpierw
     * odczytać, dopiero potem któryś może zapisać. To jest DOKŁADNIE ten przeplot, który przy count++ zdarza się losowo.
     */
    static void forcedLostUpdate() throws InterruptedException {
        section("2. Wymuszony przeplot — utracona aktualizacja krok po kroku");

        visitors = 0;
        CyclicBarrier bothHaveRead = new CyclicBarrier(2);   // both have read = obaj przeczytali
        String[][] steps = new String[2][2];                 // steps = kroki; [wątek][0 = odczyt, 1 = zapis]
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            int me = i;
            String name = me == 0 ? "A" : "B";
            Thread t = new Thread(() -> {
                int read = visitors;                       // KROK 1: odczyt
                steps[me][0] = name + " odczytał " + read;
                awaitBarrier(bothHaveRead);                // czekamy, aż drugi też odczyta
                visitors = read + 1;                       // KROK 2 i 3: +1 i zapis starej wartości + 1
                steps[me][1] = name + " zapisał " + (read + 1);
            }, "gość-" + name);
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        show("krok", steps[0][0]);
        // WYNIK: krok → A odczytał 0
        show("krok", steps[1][0]);
        // WYNIK: krok → B odczytał 0
        show("krok", steps[0][1]);
        // WYNIK: krok → A zapisał 1
        show("krok", steps[1][1]);
        // WYNIK: krok → B zapisał 1
        show("dwa zwiększenia, a licznik", visitors);
        // WYNIK: dwa zwiększenia, a licznik → 1
        // Kolejność wydruku jest ustalona przez nas (najpierw odczyty, potem zapisy). Prawdziwa kolejność dwóch zapisów
        // jest nieznana — ale nie ma znaczenia: obaj zapisują 1. Bariera gwarantuje, że oba odczyty były przed zapisami.
    }

    // =================================================================================================
    // 3. CHECK-THEN-ACT — SPRAWDŹ, POTEM DZIAŁAJ
    // =================================================================================================

    /**
     * 3. Drugi klasyczny wyścig: najpierw sprawdzamy warunek, potem działamy na jego podstawie. Między sprawdzeniem
     * a działaniem inny wątek mógł zmienić stan — nasza decyzja opiera się na nieaktualnej informacji.
     */
    static void checkThenAct() throws InterruptedException {
        section("3. Check-then-act — „sprawdź, potem działaj”");

        // Zwykła HashMap jako „pamięć podręczna” połączeń: tworzenie połączenia jest drogie, ma się zdarzyć RAZ.
        Map<String, String> cache = new HashMap<>();    // cache = pamięć podręczna
        AtomicInteger created = new AtomicInteger();    // created = utworzono (licznik atomowy — sekcja 5)
        CyclicBarrier bothChecked = new CyclicBarrier(2);
        runInThreads(2, () -> {
            boolean absent;
            synchronized (cache) {                        // sam odczyt chronimy, by nie psuć HashMap
                absent = !cache.containsKey("baza");      // containsKey = czy zawiera klucz — SPRAWDŹ
            }
            awaitBarrier(bothChecked);                    // wymuszamy: obaj sprawdzili, zanim ktoś zadziałał
            if (absent) {
                created.incrementAndGet();                // „drogie tworzenie połączenia”
                synchronized (cache) {
                    cache.put("baza", "połączenie");      // DZIAŁAJ
                }
            }
        });
        show("ile razy utworzono połączenie (miało być 1)", created.get());
        // WYNIK: ile razy utworzono połączenie (miało być 1) → 2
        // PUŁAPKA: synchronizacja KAŻDEJ operacji z osobna nie wystarcza — dlaczego: sprawdzenie i działanie muszą być
        // JEDNĄ niepodzielną operacją. Tu każda z nich była chroniona, a wyścig i tak wystąpił (pomiędzy nimi).

        // NAPRAWA: operacja złożona wykonywana atomowo przez mapę współbieżną (szczegóły: Concurrency06).
        // computeIfAbsent = oblicz, jeśli brak; w ConcurrentHashMap funkcja wykona się najwyżej raz dla klucza.
        Map<String, String> safeCache = new ConcurrentHashMap<>();
        AtomicInteger createdSafely = new AtomicInteger();
        runInThreads(8, () -> safeCache.computeIfAbsent("baza", key -> {
            createdSafely.incrementAndGet();
            return "połączenie";
        }));
        show("computeIfAbsent z 8 wątków — utworzono razy", createdSafely.get());
        // WYNIK: computeIfAbsent z 8 wątków — utworzono razy → 1

        // Inne przykłady check-then-act z życia: „if (plik nie istnieje) utwórz plik”, „if (saldo >= kwota) wypłać”,
        // leniwa inicjalizacja „if (instance == null) instance = new …” (Concurrency08ThreadSafetyPatterns).
    }

    // =================================================================================================
    // 4. SYNCHRONIZED — METODA I BLOK
    // =================================================================================================

    /** Licznik bezpieczny wątkowo dzięki synchronized. */
    static class SynchronizedCounter {   // synchronized counter = licznik synchronizowany
        private int count;
        private final Object lock = new Object();   // lock = blokada (prywatny obiekt blokady)

        // synchronized na metodzie instancji = blokada na obiekcie this
        synchronized void increment() {
            count++;
        }

        // blok synchronized na prywatnym obiekcie — ta sama idea, ale blokadę widzi tylko ta klasa
        void add(int amount) {   // add = dodaj; amount = ilość
            synchronized (lock) {
                count += amount;   // UWAGA: inna blokada niż w increment() — patrz PUŁAPKA niżej
            }
        }

        synchronized int get() {   // odczyt też pod blokadą — dla WIDOCZNOŚCI najnowszej wartości
            return count;
        }
    }

    /**
     * 4. {@code synchronized} daje dwie rzeczy: wzajemne wykluczanie (tylko jeden wątek naraz w sekcji chronionej
     * TĄ SAMĄ blokadą) i widoczność (zwolnienie blokady → zapisy widoczne dla następnego, kto ją weźmie).
     */
    static void synchronizedFix() throws InterruptedException {
        section("4. synchronized — monitor obiektu");

        SynchronizedCounter counter = new SynchronizedCounter();
        runInThreads(THREADS, () -> {
            for (int i = 0; i < PER_THREAD; i++) {
                counter.increment();
            }
        });
        show("synchronized increment() z 2 wątków", counter.get());
        // WYNIK: synchronized increment() z 2 wątków → 200000

        // Jak to działa: każdy obiekt w Javie ma wbudowaną blokadę (monitor). Wejście do synchronized = zdobycie
        // blokady (inne wątki czekają w stanie BLOCKED), wyjście = zwolnienie — także przy wyjątku.
        //   synchronized void m()            → blokada na this
        //   static synchronized void m()     → blokada na obiekcie Class (np. SynchronizedCounter.class)
        //   synchronized (obj) { ... }       → blokada na obj
        // Jeden obiekt = jedna blokada. Dwa wątki wykluczają się TYLKO, gdy synchronizują się na tym samym obiekcie.
        // PUŁAPKA: w klasie SynchronizedCounter metoda increment() blokuje this, a add() — pole lock. To DWIE różne
        // blokady, więc increment() i add() mogą biec naraz i zgubić aktualizację. Dlaczego to błąd: chronimy to samo
        // pole count, więc musi je chronić JEDNA blokada. Zasada: jedno pole (jeden niezmiennik) = jedna blokada.
        // DOBRA PRAKTYKA: sekcja krytyczna jak najkrótsza (bez IO i wywołań cudzych metod) — dlaczego: inne wątki
        // w tym czasie stoją; długa sekcja zamienia program wielowątkowy w jednowątkowy, a cudzy kod może brać
        // inne blokady i doprowadzić do zakleszczenia (Concurrency03Locks).
        // DOBRA PRAKTYKA: synchronizuj na prywatnym finalnym obiekcie (jak lock wyżej), a nie na this — dlaczego:
        // na this może zsynchronizować się też obcy kod, który ma referencję do naszego obiektu.
    }

    // =================================================================================================
    // 5. ZMIENNE ATOMOWE — AtomicInteger, AtomicLong
    // =================================================================================================

    /**
     * 5. Klasy z pakietu {@code java.util.concurrent.atomic} wykonują operacje read-modify-write NIEPODZIELNIE, bez
     * blokad — dzięki instrukcji procesora porównaj-i-ustaw (CAS, compare-and-set).
     */
    static void atomicVariables() throws InterruptedException {
        section("5. AtomicInteger i AtomicLong");

        AtomicInteger counter = new AtomicInteger();   // startuje od 0
        runInThreads(THREADS, () -> {
            for (int i = 0; i < PER_THREAD; i++) {
                counter.incrementAndGet();   // incrementAndGet = zwiększ i pobierz (niepodzielne count++)
            }
        });
        show("AtomicInteger z 2 wątków", counter.get());
        // WYNIK: AtomicInteger z 2 wątków → 200000

        // Różnica jak między ++i a i++:
        AtomicInteger a = new AtomicInteger(10);
        show("getAndIncrement() zwraca", a.getAndIncrement());   // getAndIncrement = pobierz i zwiększ (stara wartość)
        // WYNIK: getAndIncrement() zwraca → 10
        show("incrementAndGet() zwraca", a.incrementAndGet());   // nowa wartość
        // WYNIK: incrementAndGet() zwraca → 12
        show("addAndGet(100)", a.addAndGet(100));                  // addAndGet = dodaj i pobierz
        // WYNIK: addAndGet(100) → 112
        show("updateAndGet(x -> x * 2)", a.updateAndGet(x -> x * 2));   // updateAndGet = zaktualizuj i pobierz
        // WYNIK: updateAndGet(x -> x * 2) → 224
        show("accumulateAndGet(500, Math::max)", a.accumulateAndGet(500, Math::max));   // accumulate = gromadź
        // WYNIK: accumulateAndGet(500, Math::max) → 500

        // compareAndSet(oczekiwana, nowa) = porównaj i ustaw: ustawia TYLKO gdy bieżąca == oczekiwana; zwraca, czy się udało.
        show("compareAndSet(500, 1) gdy jest 500", a.compareAndSet(500, 1));
        // WYNIK: compareAndSet(500, 1) gdy jest 500 → true
        show("compareAndSet(500, 2) gdy jest 1", a.compareAndSet(500, 2));
        // WYNIK: compareAndSet(500, 2) gdy jest 1 → false
        show("wartość", a.get());
        // WYNIK: wartość → 1

        // Pętla CAS — tak w środku działa updateAndGet. Liczymy maksimum z 4 wątków, każdy z innym zakresem.
        AtomicLong max = new AtomicLong(Long.MIN_VALUE);
        runInThreadsIndexed(4, index -> {
            for (long v = index * 1_000L; v < (index + 1) * 1_000L; v++) {
                long current;
                do {
                    current = max.get();                       // 1) odczytaj
                    if (v <= current) {
                        break;                                 // nasza wartość nie jest większa — nic do zrobienia
                    }
                } while (!max.compareAndSet(current, v));      // 2) ustaw, jeśli nikt nie zmienił w międzyczasie; inaczej ponów
            }
        });
        show("maksimum z pętli CAS (4 wątki)", max.get());
        // WYNIK: maksimum z pętli CAS (4 wątki) → 3999

        // PUŁAPKA: dwie operacje atomowe pod rząd to NIE jest jedna operacja atomowa — dlaczego: między nimi inny wątek
        // może zmienić wartość.  if (a.get() < 10) a.incrementAndGet();  to znowu check-then-act. Użyj updateAndGet
        // z warunkiem w funkcji albo pętli compareAndSet.
        // PUŁAPKA: funkcja w updateAndGet/accumulateAndGet może zostać wywołana KILKA razy (przy konflikcie CAS ponawia),
        // więc musi być bez efektów ubocznych (tak mówi dokumentacja AtomicInteger).
    }

    // =================================================================================================
    // 6. LongAdder — LICZNIK DO CZĘSTYCH ZWIĘKSZEŃ
    // =================================================================================================

    /**
     * 6. {@code LongAdder} (sumator) rozkłada zwiększenia na kilka komórek, gdy wiele wątków walczy o ten sam licznik,
     * a {@code sum()} je sumuje. Przy dużej rywalizacji jest szybszy od AtomicLong — kosztem pamięci.
     */
    static void longAdderCounter() throws InterruptedException {
        section("6. LongAdder — licznik statystyk");

        LongAdder requests = new LongAdder();   // requests = żądania (np. licznik odwiedzin strony)
        runInThreads(4, () -> {
            for (int i = 0; i < PER_THREAD; i++) {
                requests.increment();
            }
        });
        show("LongAdder z 4 wątków", requests.sum());   // sum = suma
        // WYNIK: LongAdder z 4 wątków → 400000
        requests.add(5);   // add = dodaj
        show("po add(5)", requests.sum());
        // WYNIK: po add(5) → 400005

        // PUŁAPKA: sum() wywołane W TRAKCIE zwiększeń nie jest migawką z jednej chwili (dokumentacja: zwracana wartość
        // NIE jest atomową migawką) — dlaczego: sumuje komórki po kolei, a wątki wciąż je zmieniają. Dokładny wynik
        // dostajemy, gdy zwiększenia się skończyły (u nas: po join()).
        // DOBRA PRAKTYKA: LongAdder do statystyk (liczniki żądań, błędów), AtomicLong gdy potrzebujesz
        // compareAndSet lub „zwiększ i od razu pobierz” (np. generator kolejnych numerów).
    }

    // =================================================================================================
    // 7. BRAK WSPÓŁDZIELENIA — NIEZMIENNOŚĆ I ZAMKNIĘCIE W WĄTKU
    // =================================================================================================

    /** Niezmienny rekord — można go bezpiecznie przekazywać między wątkami. */
    record Price(String sku, long grosze) {   // price = cena; grosze = kwota w groszach
    }

    /**
     * 7. Najlepsza synchronizacja to brak potrzeby synchronizacji. Wyścig wymaga trzech rzeczy naraz: stanu, który jest
     * WSPÓLNY, ZMIENNY i używany przez kilka wątków. Usuń jedną — problem znika.
     */
    static void noSharingNoProblem() throws InterruptedException {
        section("7. Brak współdzielenia — niezmienność i zamknięcie w wątku");

        // a) Zamknięcie w wątku (confinement): każdy wątek liczy na ZMIENNEJ LOKALNEJ (stos jest prywatny),
        //    a wynik oddaje raz, do własnej komórki tablicy. Synchronizacja niepotrzebna (poza join()).
        long[] partial = new long[4];
        runInThreadsIndexed(4, index -> {
            long local = 0;                                   // zmienna lokalna — widzi ją tylko ten wątek
            for (int i = 0; i < PER_THREAD; i++) {
                local++;
            }
            partial[index] = local;                           // jeden zapis na końcu, do własnej komórki
        });
        show("suma części policzonych lokalnie", java.util.Arrays.stream(partial).sum());
        // WYNIK: suma części policzonych lokalnie → 400000

        // b) Niezmienność: rekord z polami final — po zbudowaniu nikt go nie zmieni, więc wiele wątków może go czytać.
        //    „Zmiana” = nowy obiekt. (Gwarancje pól final przy publikowaniu: Concurrency10MemoryModel.)
        Price price = new Price("KSI-001", 7900);
        long[] read = new long[3];
        runInThreadsIndexed(3, index -> read[index] = price.grosze());
        show("trzy wątki odczytały tę samą cenę", read[0] + ", " + read[1] + ", " + read[2]);
        // WYNIK: trzy wątki odczytały tę samą cenę → 7900, 7900, 7900
        Price discounted = new Price(price.sku(), price.grosze() * 90 / 100);   // discounted = przeceniona
        show("„zmiana” to nowy obiekt", price.grosze() + " → " + discounted.grosze());
        // WYNIK: „zmiana” to nowy obiekt → 7900 → 7110

        // c) Strumień równoległy też dzieli pracę na wątki — i sam łączy wyniki częściowe (t16_streams/Streams18Parallel).
        long streamSum = IntStream.rangeClosed(1, 400_000).parallel().mapToLong(i -> 1L).sum();
        show("parallel().sum() — bez wspólnego stanu", streamSum);
        // WYNIK: parallel().sum() — bez wspólnego stanu → 400000
        // DOBRA PRAKTYKA: projektuj tak, by wątki NIE dzieliły zmiennego stanu: dane wejściowe niezmienne, praca na
        // zmiennych lokalnych, wynik oddawany na końcu (Future, join). Dlaczego: kod bez współdzielenia nie ma wyścigów
        // z definicji, a synchronizacja zawsze kosztuje — wydajność i łatwość popełnienia błędu.
    }

    // =================================================================================================
    // 8. PUŁAPKA: SYNCHRONIZACJA NA ZŁYM OBIEKCIE
    // =================================================================================================

    /** Za każdym razem nowy obiekt blokady — klasyczny błąd. */
    static Object lockForEachCall() {   // lock for each call = blokada dla każdego wywołania
        return new Object();
    }

    /**
     * 8. synchronized chroni tylko wtedy, gdy wszystkie wątki biorą blokadę TEGO SAMEGO obiektu. Pokazujemy to
     * deterministycznie: main trzyma „blokadę”, a drugi wątek i tak wchodzi do sekcji krytycznej.
     */
    static void wrongLockObject() throws InterruptedException {
        section("8. PUŁAPKA: synchronized na złym obiekcie");

        // a) new Object() przy każdym wywołaniu — każdy wątek ma WŁASNĄ blokadę, więc nikt na nikogo nie czeka.
        boolean[] entered = new boolean[1];   // entered = wszedł
        synchronized (lockForEachCall()) {     // main „trzyma blokadę”…
            Thread other = new Thread(() -> {
                synchronized (lockForEachCall()) {   // …a ten wątek bierze INNY obiekt
                    entered[0] = true;
                }
            }, "intruz");
            other.start();
            other.join(5_000);   // gdyby blokada działała, wątek by tu wisiał (BLOCKED), a join wróciłby po limicie
            show("drugi wątek wszedł, choć main trzyma „blokadę”", entered[0]);
            // WYNIK: drugi wątek wszedł, choć main trzyma „blokadę” → true
        }

        // Dla porównania: wspólny obiekt blokady — drugi wątek MUSI czekać.
        Object sharedLock = new Object();   // shared lock = wspólna blokada
        Thread waiting;
        synchronized (sharedLock) {
            waiting = new Thread(() -> {
                synchronized (sharedLock) {
                    // wejdzie dopiero, gdy main odda blokadę
                }
            }, "cierpliwy");
            waiting.start();
            waitForState(waiting, Thread.State.BLOCKED);
            show("ze wspólną blokadą drugi wątek jest", waiting.getState());
            // WYNIK: ze wspólną blokadą drugi wątek jest → BLOCKED
        }
        waiting.join();

        // b) Integer jako blokada: count++ na polu Integer tworzy NOWY obiekt (Integer jest niezmienny), więc wątki
        //    synchronizują się na coraz to innych obiektach. Do tego Integer.valueOf(-128..127) zwraca obiekty
        //    z pamięci podręcznej — wspólne z całym programem (także z cudzymi bibliotekami).
        Integer boxed = 1000;
        Integer before = boxed;
        boxed++;   // w środku: boxed = Integer.valueOf(boxed + 1) — inny obiekt, inna referencja
        show("po boxed++ to ten sam obiekt?", before == boxed);
        // WYNIK: po boxed++ to ten sam obiekt? → false
        show("Integer.valueOf(100) == Integer.valueOf(100)", Integer.valueOf(100) == Integer.valueOf(100));
        // WYNIK: Integer.valueOf(100) == Integer.valueOf(100) → true

        // c) Literał String jako blokada: literały są internowane (jeden obiekt na całą JVM), więc
        //    synchronized ("LOCK") w Twojej klasie i w zupełnie obcej klasie to TA SAMA blokada.
        String mine = "LOCK";
        String someoneElses = "LOCK";   // someone else's = czyjś
        show("dwa literały \"LOCK\" to ten sam obiekt?", mine == someoneElses);
        // WYNIK: dwa literały "LOCK" to ten sam obiekt? → true

        // PUŁAPKA: synchronized na new Object() w metodzie, na polu Integer/Long/Boolean, na literale String.
        // Dlaczego źle: (1) nowy obiekt = brak wykluczania; (2) zmieniana referencja = różne blokady w czasie;
        // (3) obiekt współdzielony z obcym kodem = niespodziewane czekanie, a nawet zakleszczenie.
        // DOBRA PRAKTYKA: private final Object lock = new Object(); — jedna, prywatna, niezmienna referencja.
    }

    // =================================================================================================
    // 9. ZAPOWIEDŹ: WIDOCZNOŚĆ I VOLATILE
    // =================================================================================================

    /** Flaga zatrzymania — volatile gwarantuje, że wątek roboczy zobaczy zmianę. */
    static volatile boolean running = true;   // running = działa

    /**
     * 9. Wyścig to nie jedyny problem. Drugi to WIDOCZNOŚĆ: zapis jednego wątku nie musi być nigdy widoczny dla innego,
     * jeśli nie ma między nimi synchronizacji. Tu pokazujemy wersję POPRAWNĄ (volatile); zepsutą tylko opisujemy.
     */
    static void visibilityPreview() throws InterruptedException {
        section("9. Zapowiedź: widoczność i volatile");

        running = true;
        long[] loops = new long[1];
        Thread worker = new Thread(() -> {
            while (running) {      // odczyt volatile — za każdym razem „świeża” wartość
                loops[0]++;
            }
        }, "pętla");
        worker.start();
        running = false;           // zapis volatile — gwarantuje widoczność dla kolejnych odczytów w innych wątkach
        worker.join(5_000);
        show("wątek zobaczył running = false i skończył", !worker.isAlive());
        // WYNIK: wątek zobaczył running = false i skończył → true

        // Gdyby pole NIE było volatile, model pamięci Javy NIE gwarantuje, że wątek kiedykolwiek zobaczy false.
        // W praktyce na HotSpot kompilator JIT potrafi „wyciągnąć” odczyt przed pętlę (while (true)), i pętla nie
        // kończy się nigdy. Dlatego tej wersji nie uruchamiamy — szczegóły w Concurrency10MemoryModel.
        // PUŁAPKA: volatile daje WIDOCZNOŚĆ, ale NIE atomowość: volatile int count; count++; nadal gubi zwiększenia.
        // Dlaczego: count++ to wciąż odczyt, +1, zapis — volatile sprawia tylko, że każdy z tych kroków widzi najświeższy
        // zapis, ale nie skleja ich w jedną operację.
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI
    // =================================================================================================

    /** Zadanie z numerem wątku (0, 1, 2…). */
    @FunctionalInterface
    interface IndexedTask {   // indexed task = zadanie z indeksem
        void run(int index);
    }

    /** Uruchamia {@code n} wątków z tym samym zadaniem i czeka na wszystkie. */
    static void runInThreads(int n, Runnable task) throws InterruptedException {
        runInThreadsIndexed(n, index -> task.run());
    }

    /** Uruchamia {@code n} wątków (z numerem) i czeka na wszystkie ({@code join}). */
    static void runInThreadsIndexed(int n, IndexedTask task) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int index = i;
            Thread t = new Thread(() -> task.run(index), "lekcja-" + (i + 1));
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
    }

    /** await() na barierze — wyjątki sprawdzane zamieniamy na IllegalStateException. */
    static void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (java.util.concurrent.BrokenBarrierException e) {   // broken barrier = zepsuta bariera
            throw new IllegalStateException(e);
        }
    }

    /** Czeka, aż wątek osiągnie stan (awaryjnie 10 s). */
    static void waitForState(Thread thread, Thread.State expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (thread.getState() != expected) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("wątek " + thread.getName() + " nie osiągnął stanu " + expected);
            }
            Thread.sleep(1);
        }
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Wyścig = wynik zależy od przeplotu wątków. Warunek: stan WSPÓLNY + ZMIENNY + używany z wielu wątków.
     *   • count++ = odczyt, +1, zapis (3 kroki) → utracone aktualizacje. „Jedna linijka” ≠ atomowa.
     *   • Check-then-act: sprawdzenie i działanie muszą być jedną niepodzielną operacją (computeIfAbsent, synchronized).
     *   • synchronized = wzajemne wykluczanie + widoczność; działa tylko na TYM SAMYM obiekcie blokady.
     *   • synchronized metoda → this; static synchronized → obiekt Class; blok → wskazany obiekt.
     *   • Blokada: private final Object lock = new Object(); nigdy new Object() w metodzie, Integer, literał String.
     *   • AtomicInteger/AtomicLong: incrementAndGet, getAndIncrement, compareAndSet, updateAndGet (funkcja bez efektów ubocznych).
     *   • Dwie operacje atomowe pod rząd ≠ jedna operacja atomowa.
     *   • LongAdder: najszybszy licznik przy dużej rywalizacji; sum() dokładne dopiero, gdy zwiększenia się skończyły.
     *   • Najlepiej: brak współdzielenia — zmienne lokalne, niezmienne obiekty (rekordy), wynik oddany na końcu.
     *   • volatile = widoczność, NIE atomowość.
     *
     * PYTANIA KONTROLNE:
     *   1. Z jakich trzech kroków składa się count++ i dlaczego to prowadzi do utraconych aktualizacji?
     *   2. Co wypisze (wątki A i B startują z licznikiem 0, oba najpierw czytają, potem zapisują odczyt + 1)?
     *        System.out.println(licznik);   // po join() obu
     *   3. ZNAJDŹ BŁĄD:
     *        void add(Item item) { synchronized (new Object()) { items.add(item); } }
     *   4. ZNAJDŹ BŁĄD:
     *        if (!cache.containsKey(key)) { cache.put(key, load(key)); }   // cache = ConcurrentHashMap, 8 wątków
     *   5. Co wypisze?
     *        AtomicInteger a = new AtomicInteger(5);
     *        System.out.println(a.getAndIncrement() + " " + a.incrementAndGet() + " " + a.compareAndSet(6, 0) + " " + a.get());
     *   6. Kiedy wybierzesz LongAdder, a kiedy AtomicLong?
     *   7. Czy oznaczenie pola słowem volatile naprawia count++ z wielu wątków? Dlaczego?
     *   8. Wymień dwa sposoby uniknięcia wyścigu BEZ żadnej blokady i bez klas Atomic*.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        int[] values = IntStream.rangeClosed(1, 10_000).map(i -> (i * 7919) % 10_007).toArray();
        List<String> logins = List.of("ala", "bartek", "ala", "celina", "bartek", "darek", "ala", "ela");
        Check.equal("ćw. 1: licznik synchronized, 4 wątki × 10 000", 40_000, () -> exercise1(4, 10_000));
        Check.equal("ćw. 2: maksimum przez AtomicInteger z 4 wątków", 10_006, () -> exercise2(values, 4));
        Check.equal("ćw. 3: udane rejestracje loginów (bez duplikatów)", 5, () -> exercise3(logins, 4));
        Check.equal("ćw. 4: salda po przelewach tam i z powrotem", List.of(1000, 1000), () -> exercise4(4, 1000));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 40_000, () -> solution1(4, 10_000));
        Check.equal("ćw. 2 (wzorzec)", 10_006, () -> solution2(values, 4));
        Check.equal("ćw. 3 (wzorzec)", 5, () -> solution3(logins, 4));
        Check.equal("ćw. 4 (wzorzec)", List.of(1000, 1000), () -> solution4(4, 1000));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): uruchom {@code threads} wątków; każdy ma {@code perThread} razy zwiększyć wspólny licznik
     * chroniony przez {@code synchronized} na prywatnym obiekcie blokady. Zwróć końcową wartość licznika.
     * Podpowiedź: licznik może być polem w tablicy {@code int[1]}, a blokada — {@code final Object lock = new Object();}
     * Odczyt wyniku po {@code join()} wszystkich wątków.
     */
    static int exercise1(int threads, int perThread) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): znajdź maksimum tablicy {@code values} przy pomocy {@code threads} wątków. Każdy przegląda
     * swój kawałek i aktualizuje WSPÓLNY {@code AtomicInteger}. Nie używaj synchronized.
     * Podpowiedź: {@code max.accumulateAndGet(v, Math::max)} albo własna pętla compareAndSet. Kawałek wątku i: od
     * {@code i * n / threads} do {@code (i + 1) * n / threads}.
     */
    static int exercise2(int[] values, int threads) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ wersję z wyścigiem check-then-act na bezpieczną:
     * <pre>{@code
     * Set<String> taken = new HashSet<>();
     * // w każdym wątku, dla każdego loginu z jego części listy:
     * if (!taken.contains(login)) { taken.add(login); successes++; }
     * }</pre>
     * {@code threads} wątków dzieli listę {@code logins}; zwróć liczbę UDANYCH rejestracji (= liczbę różnych loginów).
     * Podpowiedź: {@code ConcurrentHashMap.newKeySet()} (zbiór współbieżny, Concurrency06) — jego {@code add} zwraca
     * {@code true} tylko temu wątkowi, który dodał element pierwszy; sukcesy licz w {@code AtomicInteger}.
     */
    static int exercise3(List<String> logins, int threads) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): dwa konta po 1000 zł (tablica {@code int[2]}). Każdy z {@code threads} wątków wykonuje
     * {@code rounds} razy: przelew 1 zł z konta 0 na 1, potem przelew 1 zł z konta 1 na 0. Przelew = zdjęcie z jednego
     * i dodanie do drugiego jako JEDNA niepodzielna operacja. Zwróć listę [saldo0, saldo1] po zakończeniu wszystkich.
     * Podpowiedź: metoda {@code transfer(from, to, amount)} z ciałem w {@code synchronized (lock)} — obie zmiany sald pod
     * tą samą blokadą. Bez synchronizacji salda „rozjadą się” (utracone aktualizacje), a suma przestanie wynosić 2000.
     */
    static List<Integer> exercise4(int threads, int rounds) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int threads, int perThread) {
        int[] counter = new int[1];
        Object lock = new Object();
        try {
            runInThreads(threads, () -> {
                for (int i = 0; i < perThread; i++) {
                    synchronized (lock) {
                        counter[0]++;
                    }
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return counter[0];   // join() w runInThreads gwarantuje widoczność
    }

    static int solution2(int[] values, int threads) {
        AtomicInteger max = new AtomicInteger(Integer.MIN_VALUE);
        int n = values.length;
        try {
            runInThreadsIndexed(threads, index -> {
                for (int k = index * n / threads; k < (index + 1) * n / threads; k++) {
                    max.accumulateAndGet(values[k], Math::max);
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return max.get();
    }

    static int solution3(List<String> logins, int threads) {
        java.util.Set<String> taken = ConcurrentHashMap.newKeySet();
        AtomicInteger successes = new AtomicInteger();
        int n = logins.size();
        try {
            runInThreadsIndexed(threads, index -> {
                for (int k = index * n / threads; k < (index + 1) * n / threads; k++) {
                    if (taken.add(logins.get(k))) {   // sprawdzenie i dodanie w JEDNEJ niepodzielnej operacji
                        successes.incrementAndGet();
                    }
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return successes.get();
    }

    static List<Integer> solution4(int threads, int rounds) {
        int[] balances = {1000, 1000};   // balances = salda
        Object lock = new Object();
        try {
            runInThreads(threads, () -> {
                for (int r = 0; r < rounds; r++) {
                    transfer(balances, lock, 0, 1, 1);
                    transfer(balances, lock, 1, 0, 1);
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return List.of(balances[0], balances[1]);
    }

    static void transfer(int[] balances, Object lock, int from, int to, int amount) {
        synchronized (lock) {   // obie zmiany pod JEDNĄ blokadą — nikt nie zobaczy stanu „w połowie przelewu”
            balances[from] -= amount;
            balances[to] += amount;
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Odczyt wartości, dodanie 1, zapis wyniku. Gdy dwa wątki odczytają tę samą wartość przed zapisem
     *      któregokolwiek, oba zapiszą „wartość + 1” — jedno zwiększenie przepada.
     *   2. 1 — oba odczytały 0 i oba zapisały 1 (sekcja 2 tej lekcji).
     *   3. new Object() przy każdym wywołaniu = każda wywołująca osoba ma własną blokadę, więc nic nie jest chronione.
     *      Poprawka: private final Object lock = new Object(); jako pole i synchronized (lock).
     *   4. Check-then-act: każda operacja ConcurrentHashMap jest bezpieczna osobno, ale między containsKey a put inny
     *      wątek może wstawić wartość — load() wykona się kilka razy. Poprawka: cache.computeIfAbsent(key, k -> load(k)).
     *   5. „5 7 false 7” — getAndIncrement zwraca 5 (jest 6), incrementAndGet zwraca 7, compareAndSet(6, 0) nie udaje się,
     *      bo jest 7, więc wartość zostaje 7.
     *   6. LongAdder — liczniki statystyk zwiększane bardzo często z wielu wątków (szybszy przy rywalizacji).
     *      AtomicLong — gdy potrzebujesz compareAndSet, updateAndGet lub wyniku „zwiększ i pobierz” (np. kolejne numery).
     *   7. Nie. volatile zapewnia widoczność najnowszego zapisu, ale count++ dalej składa się z trzech kroków, między
     *      które może wejść inny wątek. Potrzebny synchronized albo AtomicInteger.
     *   8. Np. zamknięcie w wątku (praca na zmiennych lokalnych i oddanie wyniku na końcu, łączone po join())
     *      oraz niezmienność (obiekty, których po utworzeniu nie da się zmienić — rekordy z polami niezmiennych typów).
     */
    // </editor-fold>
}
