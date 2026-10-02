package t21_concurrency;

import helpers.Check;
import helpers.Sleep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Synchronizatory — CountDownLatch, CyclicBarrier, Semaphore, Phaser, Exchanger
 *        (synchronizer = synchronizator: obiekt, który koordynuje, KIEDY wątki mogą iść dalej)
 *
 * W SKRÓCIE:
 *   Blokada (lock) chroni dane. Synchronizator ustala KOLEJNOŚĆ i TEMPO: „poczekaj, aż 3 wątki skończą”,
 *   „ruszajcie wszyscy naraz”, „najwyżej 2 naraz w środku”, „spotkajmy się po każdym etapie”.
 *   Zamiast ręcznego wait/notify bierzesz gotowe, przetestowane narzędzie z java.util.concurrent.
 *
 * ANALOGIA:
 *   CountDownLatch = odliczanie przed startem rakiety: 3, 2, 1, 0 → start; drugi raz się nie odlicza.
 *   CyclicBarrier = wycieczka górska: na każdym schronisku grupa czeka na ostatniego, potem idzie dalej.
 *   Semaphore = parking z 2 miejscami i szlabanem: wjeżdżasz, gdy jest wolne miejsce.
 *   Phaser = wycieczka, do której ludzie mogą dołączać i odchodzić po drodze.
 *   Exchanger = dwóch kurierów wymieniających się paczkami w umówionym miejscu.
 *
 * JAK TO DZIAŁA:
 *   CountDownLatch(n)   countDown() zmniejsza licznik, await() czeka na 0. Jednorazowy.
 *   CyclicBarrier(n, a) await() czeka, aż n wątków dojdzie; ostatni wykonuje akcję a; bariera wraca do stanu
 *                       początkowego (cykliczna) i można jej użyć w kolejnej fazie.
 *   Semaphore(k)        acquire() bierze pozwolenie (czeka, gdy 0), release() oddaje. Pozwolenia nie mają właściciela.
 *   Phaser              jak bariera, ale liczba uczestników może się zmieniać (register / arriveAndDeregister).
 *   Exchanger           exchange(x) czeka na drugi wątek i zwraca jego obiekt.
 *   Gwarancja pamięci (z Javadoc): to, co wątek zrobił PRZED countDown()/await() bariery/release(), jest
 *   widoczne dla wątku PO powrocie z odpowiadającego await()/acquire() — tzw. happens-before (dzieje się przed).
 *
 * SŁÓWKA:
 *   latch = zatrzask; count down = odliczać w dół; barrier = bariera; party = uczestnik; permit = pozwolenie;
 *   acquire = nabyć/zająć; release = zwolnić; phase = faza; arrive = przybyć; deregister = wyrejestrować;
 *   exchange = wymienić; broken = zepsuty; gate = bramka; await = czekać na
 *
 * ZOBACZ TEŻ: t21_concurrency/Concurrency03Locks (blokady i wait/notify — niższy poziom),
 *   t21_concurrency/Concurrency04Executors (pule wątków), t21_concurrency/Concurrency05CompletableFuture
 *   (allOf — „czekaj na wszystkie” bez zatrzasku), t21_concurrency/Concurrency10MemoryModel (happens-before)
 * </pre>
 */
public class Concurrency07Synchronizers {

    public static void main(String[] args) {
        title("Concurrency07 — synchronizatory");

        latchFinishGate();     // latch finish gate = zatrzask jako bramka mety
        latchStartGate();      // latch start gate = zatrzask jako bramka startowa
        cyclicBarrierPhases(); // cyclic barrier phases = fazy z barierą cykliczną
        brokenBarrier();       // broken barrier = zepsuta bariera
        semaphoreLimit();      // semaphore limit = limit semaforem
        phaserBriefly();       // phaser briefly = Phaser w skrócie
        exchangerBriefly();    // exchanger briefly = Exchanger w skrócie
        whichToChoose();       // which to choose = co wybrać
        exercises();           // exercises = ćwiczenia
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI
    // =================================================================================================

    /** Pula z własnymi nazwami wątków. */
    static ExecutorService namedPool(String prefix, int threads) {
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory factory = r -> new Thread(r, prefix + "-" + counter.incrementAndGet());
        return Executors.newFixedThreadPool(threads, factory);
    }

    /** Zamyka pulę i czeka na zakończenie zadań. */
    static void shutdownAndAwait(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /** Uruchamia zadania w nazwanych wątkach i czeka na wszystkie. */
    static void runAll(String prefix, Runnable... tasks) {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < tasks.length; i++) {
            threads.add(new Thread(tasks[i], prefix + "-" + (i + 1)));
        }
        threads.forEach(Thread::start);
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /** await z limitem 5 s, opakowany tak, by dało się go użyć w lambdzie Runnable. */
    static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("przekroczono czas oczekiwania");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("przerwano", e);
        }
    }

    /** await bariery z limitem 5 s, bez wyjątków kontrolowanych. */
    static void await(CyclicBarrier barrier) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("przerwano", e);
        } catch (BrokenBarrierException | TimeoutException e) {
            throw new IllegalStateException("bariera: " + e.getClass().getSimpleName(), e);
        }
    }

    // =================================================================================================
    // 1. COUNTDOWNLATCH — BRAMKA METY
    // =================================================================================================

    /**
     * 1. Najczęstsze użycie zatrzasku: main uruchamia N prac i czeka, aż WSZYSTKIE zgłoszą koniec.
     * Każdy pracownik wywołuje countDown() — w finally, żeby wyjątek nie zablokował main.
     */
    static void latchFinishGate() {
        section("1. CountDownLatch — czekamy, aż wszyscy skończą");

        int workers = 3;
        CountDownLatch done = new CountDownLatch(workers);   // done = gotowe; licznik startuje od 3
        long[] partialSums = new long[workers];               // każdy wątek pisze TYLKO do swojej komórki
        ExecutorService pool = namedPool("sumator", workers);
        try {
            for (int w = 0; w < workers; w++) {
                int index = w;
                pool.execute(() -> {                         // execute = wykonaj (bez Future)
                    try {
                        long sum = 0;
                        for (int i = index * 100 + 1; i <= (index + 1) * 100; i++) {
                            sum += i;
                        }
                        partialSums[index] = sum;
                    } finally {
                        done.countDown();                    // ZAWSZE, nawet po wyjątku
                    }
                });
            }
            boolean finished = done.await(5, TimeUnit.SECONDS);   // true = licznik doszedł do 0
            show("await zakończone w czasie", finished);
            // WYNIK: await zakończone w czasie → true
            // Odczyt tablicy jest bezpieczny: zapis przed countDown() „dzieje się przed” powrotem z await().
            show("sumy częściowe", Arrays.toString(partialSums));
            // WYNIK: sumy częściowe → [5050, 15050, 25050]
            show("suma 1..300", Arrays.stream(partialSums).sum());
            // WYNIK: suma 1..300 → 45150
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            shutdownAndAwait(pool);
        }

        // PUŁAPKA: countDown() poza finally. Jeśli praca rzuci wyjątek, licznik nigdy nie dojdzie do 0,
        // a main czeka w await() bez końca. Symulujemy: drugi pracownik „pada” i pomija countDown().
        CountDownLatch fragile = new CountDownLatch(2);      // fragile = kruchy
        runAll("kruchy",
                fragile::countDown,
                () -> {
                    try {
                        throw new IllegalStateException("awaria przed countDown()");
                    } catch (IllegalStateException e) {
                        // celowo „połykamy” — symulacja błędu; countDown() się nie wykona
                    }
                });
        try {
            show("await(200 ms) po awarii", fragile.await(200, TimeUnit.MILLISECONDS));
            // WYNIK: await(200 ms) po awarii → false
            show("getCount() — utknął na", fragile.getCount());   // getCount = pobierz licznik
            // WYNIK: getCount() — utknął na → 1
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // DOBRA PRAKTYKA: countDown() w finally + await Z LIMITEM CZASU. Dlaczego: jedno chroni przed wiecznym
        // czekaniem przy wyjątku, drugie — przy błędzie, którego nie przewidziałeś (np. zadanie w ogóle nie ruszyło).
    }

    // =================================================================================================
    // 2. COUNTDOWNLATCH — BRAMKA STARTOWA, JEDNORAZOWOŚĆ
    // =================================================================================================

    /**
     * 2. Zatrzask z licznikiem 1 to „pistolet startowy”: wszyscy czekają, main strzela raz — ruszają razem.
     * Przydaje się w testach współbieżności (maksymalny tłok w jednej chwili).
     */
    static void latchStartGate() {
        section("2. CountDownLatch — wspólny start i jednorazowość");

        CountDownLatch startGate = new CountDownLatch(1);    // start gate = bramka startowa
        CountDownLatch ready = new CountDownLatch(3);        // ready = gotowi (stoją przy bramce)
        CountDownLatch finish = new CountDownLatch(3);       // finish = meta
        AtomicInteger started = new AtomicInteger();         // ilu przeszło przez bramkę

        List<Thread> runners = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            runners.add(new Thread(() -> {
                ready.countDown();
                await(startGate);                            // wszyscy stoją tutaj
                started.incrementAndGet();
                finish.countDown();
            }, "biegacz-" + i));
        }
        runners.forEach(Thread::start);
        await(ready);                                        // wszyscy przy bramce
        show("przed strzałem wystartowało", started.get());
        // WYNIK: przed strzałem wystartowało → 0
        startGate.countDown();                               // START!
        await(finish);
        show("po strzale wystartowało", started.get());
        // WYNIK: po strzale wystartowało → 3

        // Zatrzask jest JEDNORAZOWY: licznik nie wraca do początku.
        show("getCount() bramki", startGate.getCount());
        // WYNIK: getCount() bramki → 0
        startGate.countDown();                               // przy zerze nic nie robi
        show("countDown() przy 0 → nadal", startGate.getCount());
        // WYNIK: countDown() przy 0 → nadal → 0
        try {
            show("await() przy 0 wraca od razu", startGate.await(1, TimeUnit.MILLISECONDS));
            // WYNIK: await() przy 0 wraca od razu → true
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // PUŁAPKA: nie da się „zresetować” CountDownLatch. Potrzebujesz powtarzalnych etapów → CyclicBarrier
        // albo Phaser. Dlaczego: zatrzask nie ma metody reset; po zejściu do 0 zostaje otwarty na zawsze.
        for (Thread t : runners) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // =================================================================================================
    // 3. CYCLICBARRIER — FAZY I AKCJA BARIERY
    // =================================================================================================

    /**
     * 3. {@code CyclicBarrier(3, akcja)}: trzy wątki liczą swoją część etapu; ostatni, który dojdzie do bariery,
     * wykonuje akcję (tu: podsumowanie etapu), po czym wszyscy ruszają do następnego etapu. Bariera się „odnawia”.
     */
    static void cyclicBarrierPhases() {
        section("3. CyclicBarrier — etapy z akcją podsumowującą");

        int parties = 3;                                     // parties = uczestnicy
        int[] results = new int[parties];
        List<String> phaseLog = Collections.synchronizedList(new ArrayList<>());   // phase log = dziennik faz
        AtomicInteger phase = new AtomicInteger();
        // Akcja bariery: wykonuje ją JEDEN wątek (ostatni przybyły), zanim pozostali zostaną zwolnieni.
        Runnable summary = () -> phaseLog.add("etap " + phase.incrementAndGet()
                + ": " + Arrays.toString(results) + " suma " + Arrays.stream(results).sum());
        CyclicBarrier barrier = new CyclicBarrier(parties, summary);

        Runnable[] workers = new Runnable[parties];
        for (int w = 0; w < parties; w++) {
            int index = w;
            workers[w] = () -> {
                for (int p = 1; p <= 3; p++) {
                    results[index] = (index + 1) * 10 * p;   // praca etapu p
                    await(barrier);                          // czekamy na resztę; ostatni robi podsumowanie
                }
            };
        }
        runAll("etapowiec", workers);
        showEach("dziennik etapów", phaseLog);
        // WYNIK: dziennik etapów (liczba elementów: 3):
        // WYNIK: • etap 1: [10, 20, 30] suma 60
        // WYNIK: • etap 2: [20, 40, 60] suma 120
        // WYNIK: • etap 3: [30, 60, 90] suma 180
        // Dlaczego wynik jest pewny: akcja rusza dopiero, gdy WSZYSCY zapisali swoją komórkę i weszli w await;
        // a nikt nie nadpisze komórki w następnym etapie, zanim akcja się nie skończy.

        // PUŁAPKA: KTÓRY wątek wykona akcję bariery — nie wiadomo (ostatni przybyły). Nie wypisuj jego nazwy
        // i nie zakładaj, że to zawsze ten sam wątek.
        // DOBRA PRAKTYKA: akcja bariery ma być krótka — w tym czasie wszyscy uczestnicy stoją.
    }

    // =================================================================================================
    // 4. ZEPSUTA BARIERA
    // =================================================================================================

    /**
     * 4. Gdy jeden uczestnik przekroczy czas, zostanie przerwany albo akcja bariery rzuci wyjątek,
     * bariera staje się „broken” (zepsuta): wszyscy czekający dostają BrokenBarrierException.
     */
    static void brokenBarrier() {
        section("4. Zepsuta bariera: TimeoutException, BrokenBarrierException, reset()");

        CyclicBarrier pair = new CyclicBarrier(2);            // potrzeba 2, a przychodzi tylko main
        expectThrows("await(100 ms), drugi uczestnik nie przyszedł", () -> pair.await(100, TimeUnit.MILLISECONDS));
        // WYNIK: ✔ await(100 ms), drugi uczestnik nie przyszedł → rzucono TimeoutException: (brak komunikatu)
        show("isBroken()", pair.isBroken());                   // is broken = czy zepsuta
        // WYNIK: isBroken() → true
        expectThrows("kolejne await() na zepsutej", () -> pair.await(100, TimeUnit.MILLISECONDS));
        // WYNIK: ✔ kolejne await() na zepsutej → rzucono BrokenBarrierException: (brak komunikatu)
        pair.reset();                                          // reset = przywróć stan początkowy
        show("po reset() isBroken()", pair.isBroken());
        // WYNIK: po reset() isBroken() → false

        // DOBRA PRAKTYKA: zawsze await z limitem czasu i obsługa BrokenBarrierException. Dlaczego: jeden
        // „zgubiony” uczestnik bez limitu zatrzymałby wszystkich pozostałych na zawsze.
        // PUŁAPKA: reset() przy czekających wątkach wywoła u nich BrokenBarrierException — używaj go dopiero,
        // gdy wiesz, że nikt nie czeka.
    }

    // =================================================================================================
    // 5. SEMAPHORE — NAJWYŻEJ K NARAZ
    // =================================================================================================

    /**
     * 5. {@code Semaphore(2)} — najwyżej 2 wątki naraz w sekcji chronionej (np. 2 połączenia do bazy).
     * Mierzymy największą liczbę wątków naraz w środku. Ile dokładnie wyszło (1 czy 2) — zależy od
     * uruchomienia, ale „najwyżej 2” jest GWARANTOWANE.
     */
    static void semaphoreLimit() {
        section("5. Semaphore — limit równoczesnego dostępu");

        Semaphore connections = new Semaphore(2);             // 2 pozwolenia = 2 „połączenia”
        AtomicInteger inside = new AtomicInteger();           // inside = w środku
        AtomicInteger maxInside = new AtomicInteger();
        AtomicInteger served = new AtomicInteger();           // served = obsłużone
        ExecutorService pool = namedPool("zapytanie", 6);
        try {
            for (int i = 0; i < 6; i++) {
                pool.execute(() -> {
                    try {
                        connections.acquire();                // czeka, jeśli oba pozwolenia zajęte
                        try {
                            int now = inside.incrementAndGet();
                            maxInside.accumulateAndGet(now, Math::max);   // accumulate = zakumuluj (tu: maksimum)
                            Sleep.ms(20);                     // „zapytanie do bazy” — tylko po to, by był tłok
                            served.incrementAndGet();
                            inside.decrementAndGet();
                        } finally {
                            connections.release();            // ZAWSZE oddaj pozwolenie
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        } finally {
            shutdownAndAwait(pool);
        }
        show("obsłużonych zapytań", served.get());
        // WYNIK: obsłużonych zapytań → 6
        show("najwięcej naraz ≤ 2", maxInside.get() <= 2);
        // WYNIK: najwięcej naraz ≤ 2 → true
        // Zwykle maxInside = 2 (wynik zależy od uruchomienia) — na wolnej maszynie może wyjść 1.

        // tryAcquire = spróbuj zająć: nie czeka, od razu zwraca true/false.
        Semaphore parking = new Semaphore(2);
        show("tryAcquire #1", parking.tryAcquire());
        // WYNIK: tryAcquire #1 → true
        show("tryAcquire #2", parking.tryAcquire());
        // WYNIK: tryAcquire #2 → true
        show("tryAcquire #3 (brak miejsc)", parking.tryAcquire());
        // WYNIK: tryAcquire #3 (brak miejsc) → false
        parking.release();
        show("availablePermits() po release", parking.availablePermits());   // available permits = dostępne pozwolenia
        // WYNIK: availablePermits() po release → 1

        // PUŁAPKA: pozwolenia NIE mają właściciela — release() może wywołać każdy, nawet ten, kto nic nie wziął,
        // i licznik ROŚNIE ponad wartość początkową. Dlaczego: Semaphore to tylko licznik, nie blokada.
        Semaphore one = new Semaphore(1);
        one.release();                                        // błąd logiczny: release bez acquire
        show("Semaphore(1) po zbędnym release()", one.availablePermits());
        // WYNIK: Semaphore(1) po zbędnym release() → 2
        // DOBRA PRAKTYKA: wzorzec acquire(); try { ... } finally { release(); } — release tylko po udanym acquire
        // (dlatego acquire jest PRZED try). new Semaphore(k, true) = sprawiedliwy (fair): kolejność FIFO,
        // wolniejszy, ale nikt nie czeka w nieskończoność. Spring/HikariCP: pula połączeń ma podobny limit.
    }

    // =================================================================================================
    // 6. PHASER — W SKRÓCIE
    // =================================================================================================

    /**
     * 6. {@code Phaser} = elastyczna bariera: uczestników można dodawać (register) i usuwać
     * (arriveAndDeregister) w trakcie, a metoda onAdvance (przy przejściu fazy) decyduje, czy kończyć.
     */
    static void phaserBriefly() {
        section("6. Phaser — bariera ze zmienną liczbą uczestników");

        List<String> log = Collections.synchronizedList(new ArrayList<>());
        Phaser phaser = new Phaser(1) {                      // 1 = main jest pierwszym uczestnikiem
            @Override
            protected boolean onAdvance(int phase, int registeredParties) {   // on advance = przy przejściu
                log.add("koniec fazy " + phase + ", uczestników dalej: " + registeredParties);
                return registeredParties == 0;               // true = zakończ Phaser
            }
        };
        List<Thread> workers = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            phaser.register();                               // register = zarejestruj (dołącz uczestnika)
            workers.add(new Thread(() -> {
                phaser.arriveAndAwaitAdvance();              // faza 0: czekaj na wszystkich
                phaser.arriveAndAwaitAdvance();              // faza 1
                phaser.arriveAndDeregister();                // faza 2: zgłoś przybycie i odejdź
            }, "uczestnik-" + i));
        }
        workers.forEach(Thread::start);
        phaser.arriveAndAwaitAdvance();                      // main w fazie 0
        phaser.arriveAndAwaitAdvance();                      // main w fazie 1
        phaser.arriveAndDeregister();                        // main odchodzi
        for (Thread t : workers) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        showEach("przejścia faz", log);
        // WYNIK: przejścia faz (liczba elementów: 3):
        // WYNIK: • koniec fazy 0, uczestników dalej: 3
        // WYNIK: • koniec fazy 1, uczestników dalej: 3
        // WYNIK: • koniec fazy 2, uczestników dalej: 0
        show("isTerminated()", phaser.isTerminated());        // is terminated = czy zakończony
        // WYNIK: isTerminated() → true
        // DOBRA PRAKTYKA: sięgaj po Phaser dopiero, gdy CountDownLatch i CyclicBarrier nie wystarczają (zmienna
        // liczba uczestników). Dlaczego: ma bogatsze, łatwiejsze do pomylenia API.
    }

    // =================================================================================================
    // 7. EXCHANGER — W SKRÓCIE
    // =================================================================================================

    /**
     * 7. {@code Exchanger} — punkt spotkania DWÓCH wątków: każdy oddaje obiekt i dostaje obiekt drugiego.
     * Klasyczny przykład: producent oddaje pełny bufor, konsument — pusty.
     */
    static void exchangerBriefly() {
        section("7. Exchanger — wymiana obiektów między dwoma wątkami");

        Exchanger<String> exchanger = new Exchanger<>();
        Map<String, String> received = new ConcurrentHashMap<>();   // received = otrzymane
        Runnable producer = () -> {
            try {
                received.put("producent dostał", exchanger.exchange("pełny bufor", 5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (TimeoutException e) {
                received.put("producent", "limit czasu");
            }
        };
        Runnable consumer = () -> {
            try {
                received.put("konsument dostał", exchanger.exchange("pusty bufor", 5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (TimeoutException e) {
                received.put("konsument", "limit czasu");
            }
        };
        runAll("wymiana", producer, consumer);
        showEach("po wymianie", new TreeMap<>(received));
        // WYNIK: po wymianie (liczba kluczy: 2):
        // WYNIK: • konsument dostał → pełny bufor
        // WYNIK: • producent dostał → pusty bufor
        // Exchanger to rzadkie narzędzie — w praktyce częściej wystarczy BlockingQueue (t21_concurrency/Concurrency06ConcurrentCollections).
    }

    // =================================================================================================
    // 8. CO WYBRAĆ?
    // =================================================================================================

    /**
     * 8. Tabela wyboru i nowocześniejsze zamienniki.
     */
    static void whichToChoose() {
        section("8. Który synchronizator wybrać?");

        // potrzeba                                     → narzędzie
        // ---------------------------------------------+-------------------------------------------------
        // main czeka, aż N zadań skończy (raz)         → CountDownLatch(N)  (albo invokeAll / allOf)
        // N wątków ma ruszyć dokładnie razem           → CountDownLatch(1) jako bramka startowa
        // N wątków spotyka się po KAŻDYM etapie        → CyclicBarrier(N, akcja)
        // najwyżej K naraz (połączenia, limity API)    → Semaphore(K)
        // etapy + uczestnicy dochodzą i odchodzą       → Phaser
        // dwa wątki wymieniają się obiektami           → Exchanger
        // przekazywanie danych producent → konsument   → BlockingQueue (Concurrency06)
        List<String> rows = List.of(
                "jednorazowe czekanie na N → CountDownLatch",
                "powtarzalne etapy N wątków → CyclicBarrier",
                "limit K równoczesnych → Semaphore",
                "zmienna liczba uczestników → Phaser",
                "wymiana między 2 wątkami → Exchanger");
        showEach("ściąga wyboru", rows);
        // WYNIK: ściąga wyboru (liczba elementów: 5):
        // WYNIK: • jednorazowe czekanie na N → CountDownLatch
        // WYNIK: • powtarzalne etapy N wątków → CyclicBarrier
        // WYNIK: • limit K równoczesnych → Semaphore
        // WYNIK: • zmienna liczba uczestników → Phaser
        // WYNIK: • wymiana między 2 wątkami → Exchanger

        // DOBRA PRAKTYKA: jeśli zadania ZWRACAJĄ wynik, zamiast zatrzasku zwykle prościej użyć
        // ExecutorService.invokeAll (Concurrency04) albo CompletableFuture.allOf (Concurrency05). Dlaczego: wynik
        // i wyjątek wracają razem z Future, nie trzeba współdzielonej tablicy ani pamiętać o countDown().
        // PUŁAPKA: synchronizatory nie chronią danych! Dwa wątki piszące do TEJ SAMEJ zmiennej między barierami
        // nadal tworzą wyścig. Bariera tylko porządkuje etapy.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • CountDownLatch(n): countDown() w finally, await(czas, jednostka) → boolean; jednorazowy, bez reset.
     *   • Bramka startowa = CountDownLatch(1); bramka mety = CountDownLatch(N).
     *   • CyclicBarrier(n, akcja): ostatni przybyły wykonuje akcję; bariera wielokrotnego użytku;
     *     limit czasu/przerwanie → bariera zepsuta (BrokenBarrierException u reszty), naprawa: reset().
     *   • Semaphore(k): acquire(); try { ... } finally { release(); }; tryAcquire() bez czekania;
     *     pozwolenia bez właściciela — zbędny release() zwiększa ich liczbę.
     *   • Phaser: register / arriveAndAwaitAdvance / arriveAndDeregister; onAdvance → true kończy.
     *   • Exchanger: exchange(x, czas, jednostka) — tylko dla PARY wątków.
     *   • Gwarancja widoczności: akcje przed countDown()/await bariery → widoczne po powrocie z await().
     *   • Wynik wypisuj PO czekaniu, z danych zebranych przez wątki — nie z wnętrza wątków.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się CountDownLatch od CyclicBarrier (dwie różnice)?
     *   2. Co wypisze:
     *        CountDownLatch l = new CountDownLatch(2);
     *        l.countDown(); l.countDown(); l.countDown();
     *        System.out.println(l.getCount() + " " + l.await(0, TimeUnit.SECONDS));
     *   3. ZNAJDŹ BŁĄD:
     *        pool.execute(() -> { doWork(); latch.countDown(); });
     *        latch.await();
     *   4. Co wypisze:
     *        Semaphore s = new Semaphore(1);
     *        s.acquire(); s.release(); s.release();
     *        System.out.println(s.availablePermits());
     *   5. Który wątek wykonuje akcję przekazaną do new CyclicBarrier(3, akcja)?
     *   6. ZNAJDŹ BŁĄD:
     *        try { sem.acquire(); doWork(); } finally { sem.release(); }
     *   7. Jak ograniczyć do 5 liczbę równoczesnych wywołań zewnętrznego API z wielu wątków?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: suma 1..10000 przez 4 wątki i CountDownLatch", 50005000L, () -> exercise1(10_000, 4));
        Check.equal("ćw. 2: 10 zadań, Semaphore(3) — wszystkie obsłużone i ≤ 3 naraz", true, () -> exercise2(10, 3));
        Check.equal("ćw. 3: sumy etapów (3 wątki, 3 etapy)", List.of(6, 12, 18), () -> exercise3(3, 3));
        Check.equal("ćw. 4: usługi gotowe przed startem aplikacji", List.of("baza", "cache", "kolejka"),
                () -> exercise4(List.of("kolejka", "baza", "cache")));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 50005000L, () -> solution1(10_000, 4));
        Check.equal("ćw. 2 (wzorzec)", true, () -> solution2(10, 3));
        Check.equal("ćw. 3 (wzorzec)", List.of(6, 12, 18), () -> solution3(3, 3));
        Check.equal("ćw. 4 (wzorzec)", List.of("baza", "cache", "kolejka"),
                () -> solution4(List.of("kolejka", "baza", "cache")));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz sumę 1..n w {@code workers} wątkach. Wątek i liczy przedział
     * od i*n/workers+1 do (i+1)*n/workers i zapisuje wynik do partial[i]; na końcu countDown() w finally.
     * Main czeka na zatrzask (z limitem) i sumuje tablicę.
     * Podpowiedź: long[] partial = new long[workers]; CountDownLatch done = new CountDownLatch(workers).
     */
    static long exercise1(int n, int workers) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): uruchom {@code tasks} zadań w puli 8 wątków; każde zajmuje pozwolenie
     * z Semaphore(permits), zwiększa licznik „w środku”, zapamiętuje maksimum, śpi 5 ms, zmniejsza licznik
     * i oddaje pozwolenie. Zwróć true, jeśli obsłużono wszystkie zadania i maksimum ≤ permits.
     * Podpowiedź: AtomicInteger + accumulateAndGet(now, Math::max); release w finally; shutdownAndAwait.
     */
    static boolean exercise2(int tasks, int permits) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): {@code parties} wątków przechodzi {@code phases} etapów. W etapie p (od 1) wątek i
     * (od 0) wpisuje do results[i] wartość (i+1)*p. Akcja CyclicBarrier dopisuje sumę tablicy do listy.
     * Zwróć listę sum etapów (dla 3 wątków i 3 etapów: [6, 12, 18]).
     * Podpowiedź: lista synchronizowana albo CopyOnWriteArrayList; await bariery z limitem (metoda await z lekcji).
     */
    static List<Integer> exercise3(int parties, int phases) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): PRZEPISZ niskopoziomowe wait/notify na CountDownLatch. Każda usługa startuje
     * we własnym wątku i zgłasza gotowość (dopisuje swoją nazwę do kolekcji bezpiecznej wątkowo).
     * Main czeka (z limitem 5 s), aż WSZYSTKIE zgłoszą gotowość, i zwraca posortowaną listę gotowych usług.
     * Stary kod:
     * <pre>{@code
     * synchronized (lock) {
     *     while (readyCount < services.size()) {
     *         lock.wait();            // usługi robią: synchronized(lock) { readyCount++; lock.notifyAll(); }
     *     }
     * }
     * }</pre>
     * Podpowiedź: CountDownLatch(services.size()); każda usługa: ready.add(name) i countDown() w finally;
     * main: await(5, SECONDS), potem sortowanie (stream().sorted().toList() — Java 16+).
     */
    static List<String> exercise4(List<String> services) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static long solution1(int n, int workers) {
        long[] partial = new long[workers];
        CountDownLatch done = new CountDownLatch(workers);
        Runnable[] tasks = new Runnable[workers];
        for (int w = 0; w < workers; w++) {
            int i = w;
            tasks[w] = () -> {
                try {
                    long sum = 0;
                    for (long k = (long) i * n / workers + 1; k <= (long) (i + 1) * n / workers; k++) {
                        sum += k;
                    }
                    partial[i] = sum;
                } finally {
                    done.countDown();
                }
            };
        }
        List<Thread> threads = new ArrayList<>();
        for (int w = 0; w < workers; w++) {
            Thread t = new Thread(tasks[w], "cw1-" + w);
            threads.add(t);
            t.start();
        }
        await(done);
        return Arrays.stream(partial).sum();
    }

    static boolean solution2(int tasks, int permits) {
        Semaphore semaphore = new Semaphore(permits);
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger max = new AtomicInteger();
        AtomicInteger served = new AtomicInteger();
        ExecutorService pool = namedPool("cw2", 8);
        try {
            for (int i = 0; i < tasks; i++) {
                pool.execute(() -> {
                    try {
                        semaphore.acquire();
                        try {
                            max.accumulateAndGet(inside.incrementAndGet(), Math::max);
                            Sleep.ms(5);
                            served.incrementAndGet();
                            inside.decrementAndGet();
                        } finally {
                            semaphore.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        } finally {
            shutdownAndAwait(pool);
        }
        return served.get() == tasks && max.get() <= permits;
    }

    static List<Integer> solution3(int parties, int phases) {
        int[] results = new int[parties];
        List<Integer> sums = Collections.synchronizedList(new ArrayList<>());
        CyclicBarrier barrier = new CyclicBarrier(parties, () -> sums.add(Arrays.stream(results).sum()));
        Runnable[] workers = new Runnable[parties];
        for (int w = 0; w < parties; w++) {
            int i = w;
            workers[w] = () -> {
                for (int p = 1; p <= phases; p++) {
                    results[i] = (i + 1) * p;
                    await(barrier);
                }
            };
        }
        runAll("cw3", workers);
        return List.copyOf(sums);
    }

    static List<String> solution4(List<String> services) {
        CountDownLatch allReady = new CountDownLatch(services.size());
        List<String> ready = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();
        for (String name : services) {
            Thread t = new Thread(() -> {
                try {
                    ready.add(name);                 // „start usługi”
                } finally {
                    allReady.countDown();
                }
            }, "usluga-" + name);
            threads.add(t);
            t.start();
        }
        await(allReady);
        return ready.stream().sorted().toList();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. (a) CountDownLatch jest jednorazowy, CyclicBarrier odnawia się po każdym etapie; (b) w zatrzasku
     *      odliczają jedni (countDown), a czekają inni (await) — w barierze te same wątki i czekają, i są liczone.
     *      Bariera ma też akcję wykonywaną na końcu etapu.
     *   2. „0 true” — licznik nie schodzi poniżej zera; przy zerze await zwraca od razu true.
     *   3. countDown() nie jest w finally — gdy doWork() rzuci wyjątek, licznik nie dojdzie do 0, a await() bez
     *      limitu będzie czekać wiecznie. Poprawnie: try { doWork(); } finally { latch.countDown(); } oraz
     *      latch.await(czas, jednostka) ze sprawdzeniem wyniku.
     *   4. 2 — Semaphore to licznik bez właściciela; zbędny release() podnosi liczbę pozwoleń ponad początkową.
     *   5. Ostatni wątek, który wywołał await() w danym etapie — przed zwolnieniem pozostałych. Który to będzie,
     *      nie wiadomo z góry.
     *   6. acquire() jest WEWNĄTRZ try: jeśli zostanie przerwany (InterruptedException), finally i tak wywoła
     *      release(), oddając pozwolenie, którego nie wziął → liczba pozwoleń rośnie. Poprawnie: acquire()
     *      przed try, a w try tylko praca.
     *   7. Wspólny Semaphore(5): przed wywołaniem acquire() (albo tryAcquire(czas, jednostka) z obsługą false),
     *      po wywołaniu release() w finally.
     */
    // </editor-fold>
}
