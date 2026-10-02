package t21_concurrency;

import helpers.Check;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Zadania planowane i Fork/Join — ScheduledExecutorService, ForkJoinPool, strumienie równoległe
 *        (scheduled = zaplanowany; fork = rozwidlić; join = połączyć; pool = pula)
 *
 * W SKRÓCIE:
 *   ScheduledExecutorService uruchamia zadania PÓŹNIEJ albo CYKLICZNIE (co X ms) — następca klasy Timer.
 *   ForkJoinPool wykonuje zadania „dziel i zwyciężaj”: duży problem dzieli się na mniejsze, liczy równolegle
 *   i składa wyniki. Na nim działają strumienie równoległe (parallel streams) — przez wspólną pulę commonPool.
 *
 * ANALOGIA:
 *   Scheduled = budzik: „zadzwoń za 10 minut” (schedule), „dzwoń co godzinę” (fixed rate),
 *   „dzwoń godzinę po tym, jak wyłączę poprzedni” (fixed delay).
 *   Fork/Join = sprawdzanie stosu 1000 klasówek: nauczyciel dzieli stos na pół i oddaje połowę asystentowi,
 *   ten znowu dzieli... aż każdy ma kilka prac; potem wyniki się sumuje. Kto skończy wcześniej,
 *   „podkrada” prace z cudzego stosu (work stealing = kradzież pracy).
 *
 * JAK TO DZIAŁA:
 *   scheduleAtFixedRate(zad, 0, 100 ms):     start co 100 ms liczone od POCZĄTKU poprzedniego startu
 *     |zad|.......|zad|.......|zad|.......     (gdy zadanie trwa dłużej niż okres — następne rusza później,
 *     0          100         200               ale nigdy dwa naraz)
 *   scheduleWithFixedDelay(zad, 0, 100 ms):  przerwa 100 ms liczona od KOŃCA poprzedniego wykonania
 *     |zad--|..........|zad--|..........|
 *     0     30        130   160        260
 *   RecursiveTask.compute(): jeśli kawałek mały (≤ próg) → licz wprost; inaczej podziel na dwa,
 *   left.fork() (oddaj do kolejki), right.compute() (licz sam), left.join() (zbierz wynik).
 *
 * SŁÓWKA:
 *   schedule = zaplanuj; fixed rate = stała częstotliwość; fixed delay = stałe opóźnienie; period = okres;
 *   delay = opóźnienie; recursive = rekurencyjny; threshold = próg; work stealing = kradzież pracy;
 *   common pool = wspólna pula; parallelism = poziom równoległości; divide and conquer = dziel i zwyciężaj;
 *   cron = format harmonogramu (minuty, godziny, dni...)
 *
 * ZOBACZ TEŻ: t21_concurrency/Concurrency04Executors (pule i Future), t16_streams/Streams18Parallel
 *   (strumienie równoległe — podstawy), t21_concurrency/Concurrency05CompletableFuture (też używa commonPool),
 *   t21_concurrency/Concurrency07Synchronizers (CountDownLatch w demach)
 * </pre>
 */
public class Concurrency09ScheduledForkJoin {

    public static void main(String[] args) {
        title("Concurrency09 — zadania planowane i Fork/Join");

        oneShotSchedule();        // one-shot schedule = jednorazowe zaplanowanie
        periodicTasks();          // periodic tasks = zadania cykliczne
        exceptionStopsSchedule(); // exception stops schedule = wyjątek zatrzymuje harmonogram
        legacyTimer();            // legacy timer = przestarzały Timer
        forkJoinSum();            // fork-join sum = suma metodą Fork/Join
        workStealing();           // work stealing = kradzież pracy
        parallelStreamsPool();    // parallel streams pool = pula strumieni równoległych
        springScheduling();       // spring scheduling = planowanie w Springu
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI
    // =================================================================================================

    /** Fabryka wątków z własnymi nazwami. */
    static ThreadFactory named(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return r -> new Thread(r, prefix + "-" + counter.incrementAndGet());
    }

    /** Zamyka pulę i czeka na zakończenie. */
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

    /** Czeka na zatrzask z limitem; false = nie doczekaliśmy się. */
    static boolean await(CountDownLatch latch) {
        try {
            return latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // =================================================================================================
    // 1. JEDNORAZOWE ZAPLANOWANIE
    // =================================================================================================

    /**
     * 1. {@code schedule(zadanie, opóźnienie, jednostka)} — wykonaj raz, nie wcześniej niż za podany czas.
     * Zwraca ScheduledFuture: get() czeka na wynik jak zwykłe Future.
     */
    static void oneShotSchedule() {
        section("1. schedule — wykonaj raz, później");

        // newScheduledThreadPool = nowa pula planująca (tu 1 wątek, własna nazwa)
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, named("planista"));
        try {
            ScheduledFuture<String> report = scheduler.schedule(
                    () -> "raport z wątku " + Thread.currentThread().getName(), 50, TimeUnit.MILLISECONDS);
            show("wynik po ~50 ms", report.get(5, TimeUnit.SECONDS));
            // WYNIK: wynik po ~50 ms → raport z wątku planista-1
            show("isDone()", report.isDone());
            // WYNIK: isDone() → true

            // Zadanie zaplanowane i anulowane przed czasem — nie wykona się.
            AtomicInteger ran = new AtomicInteger();
            ScheduledFuture<?> late = scheduler.schedule(ran::incrementAndGet, 10, TimeUnit.SECONDS);
            show("cancel(false) przed terminem", late.cancel(false));   // cancel = anuluj
            // WYNIK: cancel(false) przed terminem → true
            show("isCancelled()", late.isCancelled());
            // WYNIK: isCancelled() → true
            show("wykonań anulowanego", ran.get());
            // WYNIK: wykonań anulowanego → 0
        } catch (Exception e) {
            show("błąd", e);
        } finally {
            shutdownAndAwait(scheduler);
        }
        // PUŁAPKA: opóźnienie to MINIMUM, nie dokładny czas. Dlaczego: gdy wszystkie wątki puli są zajęte albo
        // system przeciążony, zadanie ruszy później. Nigdy nie wypisuj zmierzonego czasu w wynikach.
        // DOBRA PRAKTYKA: anulowane zadania domyślnie zostają w kolejce do swojego terminu; przy wielu anulowaniach
        // ustaw ScheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true). Dlaczego: inaczej zajmują pamięć.
    }

    // =================================================================================================
    // 2. ZADANIA CYKLICZNE — FIXED RATE KONTRA FIXED DELAY
    // =================================================================================================

    /**
     * 2. Zadanie cykliczne działa, dopóki go nie anulujesz (albo nie rzuci wyjątku — sekcja 3). Liczymy przebiegi;
     * zadanie po 5 przebiegach przestaje cokolwiek robić, a main czeka na zatrzask — więc licznik = dokładnie 5.
     */
    static void periodicTasks() {
        section("2. scheduleAtFixedRate i scheduleWithFixedDelay");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, named("cykl"));
        try {
            show("fixed rate: przebiegów", runPeriodic(scheduler, true, 5));
            // WYNIK: fixed rate: przebiegów → 5
            show("fixed delay: przebiegów", runPeriodic(scheduler, false, 5));
            // WYNIK: fixed delay: przebiegów → 5
        } finally {
            shutdownAndAwait(scheduler);
        }
        // Różnica (patrz JAK TO DZIAŁA): fixed rate trzyma RYTM (start co okres, np. „pomiar co sekundę”),
        // fixed delay trzyma PRZERWĘ po zakończeniu (np. „odpytuj serwer, odczekaj 5 s, odpytaj znowu”).
        // Gwarancja z Javadoc (dla obu): kolejne wykonania TEGO SAMEGO zadania nigdy nie nakładają się na siebie.
        // PUŁAPKA: przy fixed rate zadanie wolniejsze od okresu nie „nadrobi” spóźnień równolegle — kolejne starty
        // przesuwają się i mogą następować tuż po sobie. Dlaczego: harmonogram liczy terminy od pierwszego startu.
        // DOBRA PRAKTYKA: dla odpytywania zewnętrznych systemów wybieraj fixed delay. Dlaczego: wolna odpowiedź
        // nie spowoduje serii zapytań jedno za drugim.
    }

    /** Uruchamia zadanie cykliczne co 20 ms, czeka na {@code times} przebiegów, anuluje i zwraca licznik. */
    static int runPeriodic(ScheduledExecutorService scheduler, boolean fixedRate, int times) {
        AtomicInteger runs = new AtomicInteger();          // runs = przebiegi
        CountDownLatch enough = new CountDownLatch(times); // enough = wystarczy
        Runnable task = () -> {
            if (runs.get() >= times) {
                return;                                    // nadmiarowe przebiegi (przed cancel) nic nie robią
            }
            runs.incrementAndGet();
            enough.countDown();
        };
        ScheduledFuture<?> handle = fixedRate               // handle = uchwyt
                ? scheduler.scheduleAtFixedRate(task, 0, 20, TimeUnit.MILLISECONDS)
                : scheduler.scheduleWithFixedDelay(task, 0, 20, TimeUnit.MILLISECONDS);
        await(enough);
        handle.cancel(false);                              // false = nie przerywaj trwającego przebiegu
        return runs.get();
    }

    // =================================================================================================
    // 3. PUŁAPKA: WYJĄTEK PO CICHU ZATRZYMUJE HARMONOGRAM
    // =================================================================================================

    /**
     * 3. Jeśli wykonanie zadania cyklicznego rzuci wyjątek, Javadoc mówi: kolejne wykonania są WSTRZYMANE.
     * Nic nie trafia do logu ani na konsolę — wyjątek leży w ScheduledFuture, którego nikt nie sprawdza.
     */
    static void exceptionStopsSchedule() {
        section("3. PUŁAPKA: wyjątek w zadaniu cyklicznym zatrzymuje je bez śladu");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, named("raport"));
        try {
            AtomicInteger runs = new AtomicInteger();
            ScheduledFuture<?> handle = scheduler.scheduleWithFixedDelay(() -> {
                int n = runs.incrementAndGet();
                if (n == 3) {
                    throw new IllegalStateException("awaria w 3. przebiegu");
                }
            }, 0, 10, TimeUnit.MILLISECONDS);

            // get() na zadaniu cyklicznym wraca TYLKO, gdy zadanie się skończyło: anulowanie albo wyjątek.
            expectThrows("handle.get()", () -> handle.get(5, TimeUnit.SECONDS));
            // WYNIK: ✔ handle.get() → rzucono ExecutionException: java.lang.IllegalStateException: awaria w 3. przebiegu
            show("isDone()", handle.isDone());
            // WYNIK: isDone() → true
            show("przebiegów (i już nie będzie więcej)", runs.get());
            // WYNIK: przebiegów (i już nie będzie więcej) → 3
            try {
                handle.get();
            } catch (ExecutionException e) {
                // ExecutionException = opakowanie; prawdziwa przyczyna w getCause()
                show("przyczyna", e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
                // WYNIK: przyczyna → IllegalStateException: awaria w 3. przebiegu
            }

            // Naprawa: łap wyjątki WEWNĄTRZ zadania — jeden zły przebieg nie zabija harmonogramu.
            AtomicInteger safeRuns = new AtomicInteger();
            AtomicInteger errors = new AtomicInteger();
            CountDownLatch fiveRuns = new CountDownLatch(5);
            ScheduledFuture<?> safe = scheduler.scheduleWithFixedDelay(() -> {
                if (safeRuns.get() >= 5) {
                    return;
                }
                try {
                    int n = safeRuns.incrementAndGet();
                    if (n == 3) {
                        throw new IllegalStateException("awaria w 3. przebiegu");
                    }
                } catch (RuntimeException e) {
                    errors.incrementAndGet();              // w prawdziwym kodzie: log.error(..., e)
                } finally {
                    fiveRuns.countDown();
                }
            }, 0, 10, TimeUnit.MILLISECONDS);
            await(fiveRuns);
            safe.cancel(false);
            show("z try/catch: przebiegów", safeRuns.get());
            // WYNIK: z try/catch: przebiegów → 5
            show("z try/catch: błędów złapanych", errors.get());
            // WYNIK: z try/catch: błędów złapanych → 1
        } catch (Exception e) {
            show("błąd", e);
        } finally {
            shutdownAndAwait(scheduler);
        }
        // DOBRA PRAKTYKA: ciało zadania cyklicznego zawsze w try { ... } catch (RuntimeException e) { log } .
        // Dlaczego: inaczej np. nocne czyszczenie danych przestanie działać po pierwszym błędzie i nikt się nie dowie.
    }

    // =================================================================================================
    // 4. TIMER — STARE API
    // =================================================================================================

    /**
     * 4. {@code java.util.Timer} (Java 1.3) — jeden wątek na wszystkie zadania. Działa, ale ma wady,
     * dlatego w nowym kodzie używamy ScheduledExecutorService.
     */
    static void legacyTimer() {
        section("4. Timer — przestarzały poprzednik");

        Timer timer = new Timer("stary-timer", true);      // true = wątek-demon (daemon)
        CountDownLatch done = new CountDownLatch(1);
        AtomicInteger ran = new AtomicInteger();
        timer.schedule(new TimerTask() {                   // TimerTask = zadanie timera (klasa abstrakcyjna)
            @Override
            public void run() {
                ran.incrementAndGet();
                done.countDown();
            }
        }, 20);
        show("TimerTask wykonany", await(done));
        // WYNIK: TimerTask wykonany → true
        show("wykonań", ran.get());
        // WYNIK: wykonań → 1
        timer.cancel();                                    // kończy wątek timera

        // Wady Timer (dlatego ScheduledExecutorService):
        //   • JEDEN wątek — wolne zadanie opóźnia wszystkie inne,
        //   • wyjątek w TimerTask zabija wątek timera i anuluje WSZYSTKIE jego zadania (kolejne schedule rzucają
        //     IllegalStateException), a stos wyjątku ląduje na System.err — dlatego tu tego nie uruchamiamy,
        //   • planowanie oparte na zegarze systemowym (zmiana czasu w systemie wpływa na terminy).
        // DOBRA PRAKTYKA: Executors.newScheduledThreadPool(n, fabryka) zamiast new Timer(). Dlaczego: wiele wątków,
        // wyjątek zatrzymuje tylko JEDNO zadanie, a nie całą pulę, i obsługa przez Future.
    }

    // =================================================================================================
    // 5. FORKJOINPOOL I RECURSIVETASK
    // =================================================================================================

    static final int THRESHOLD = 10_000;                   // threshold = próg: poniżej liczymy wprost

    /** Suma fragmentu tablicy metodą dziel i zwyciężaj. Liczy też liczbę „liści” (kawałków liczonych wprost). */
    static final class SumTask extends RecursiveTask<Long> {   // RecursiveTask = zadanie rekurencyjne z wynikiem
        private static final long serialVersionUID = 1L;   // RecursiveTask jest Serializable — bez tego pola -Xlint ostrzega
        private final long[] data;
        private final int from;                            // from = od (włącznie)
        private final int to;                              // to = do (wyłącznie)
        private final transient AtomicInteger leaves;      // leaves = liście (licznik do demonstracji)

        SumTask(long[] data, int from, int to, AtomicInteger leaves) {
            this.data = data;
            this.from = from;
            this.to = to;
            this.leaves = leaves;
        }

        @Override
        protected Long compute() {                         // compute = oblicz
            if (to - from <= THRESHOLD) {
                leaves.incrementAndGet();
                long sum = 0;
                for (int i = from; i < to; i++) {
                    sum += data[i];
                }
                return sum;
            }
            int mid = (from + to) >>> 1;                   // środek (>>> 1 = dzielenie przez 2 bez przepełnienia)
            SumTask left = new SumTask(data, from, mid, leaves);
            SumTask right = new SumTask(data, mid, to, leaves);
            left.fork();                                   // fork = oddaj lewą połowę do kolejki (ktoś może ukraść)
            long rightSum = right.compute();               // prawą liczymy SAMI, w tym wątku
            return left.join() + rightSum;                 // join = poczekaj na lewą i połącz
        }
    }

    /**
     * 5. Suma 1..1 000 000 przez ForkJoinPool. Tablica dzielona na pół, aż kawałek ≤ 10 000 elementów:
     * 1 000 000 / 2^7 ≈ 7812 → 128 liści. Liczba liści zależy tylko od rozmiaru i progu — jest deterministyczna.
     */
    static void forkJoinSum() {
        section("5. ForkJoinPool + RecursiveTask — suma tablicy");

        long[] data = LongStream.rangeClosed(1, 1_000_000).toArray();   // rangeClosed = zakres domknięty
        AtomicInteger leaves = new AtomicInteger();
        ForkJoinPool pool = new ForkJoinPool(4);           // własna pula z poziomem równoległości 4
        try {
            long sum = pool.invoke(new SumTask(data, 0, data.length, leaves));   // invoke = wywołaj i czekaj
            show("suma 1..1 000 000", sum);
            // WYNIK: suma 1..1 000 000 → 500000500000
            show("liczba liści (kawałków ≤ progu)", leaves.get());
            // WYNIK: liczba liści (kawałków ≤ progu) → 128
            show("zgodna ze wzorem n(n+1)/2", sum == 1_000_000L * 1_000_001L / 2);
            // WYNIK: zgodna ze wzorem n(n+1)/2 → true
        } finally {
            shutdownAndAwait(pool);
        }

        // PUŁAPKA: kolejność left.fork(); right.compute(); left.join(). Odwrotnie — left.fork(); left.join(); —
        // wątek czeka na lewą połowę, zamiast w tym czasie liczyć prawą: równoległość znika.
        // PUŁAPKA: za mały próg (np. 10) → miliony malutkich zadań i narzut większy niż zysk; za duży → mało
        // kawałków, rdzenie się nudzą. Dobierz próg pomiarem (zwykle tysiące – dziesiątki tysięcy operacji).
        // PUŁAPKA: w compute() nie blokuj (IO, sleep, lock na długo) — pula ma mało wątków, każdy jest cenny.
        // DOBRA PRAKTYKA: RecursiveTask<T> gdy jest wynik, RecursiveAction gdy nie ma (np. sortowanie w miejscu).
    }

    // =================================================================================================
    // 6. KRADZIEŻ PRACY (WORK STEALING)
    // =================================================================================================

    /**
     * 6. Każdy wątek ForkJoinPool ma własną kolejkę dwustronną (deque). Swoje zadania bierze z jednego końca
     * (LIFO — najświeższe, najmniejsze), a bezczynny wątek kradnie z DRUGIEGO końca cudzej kolejki (najstarsze,
     * zwykle największe kawałki). Dzięki temu wątki rzadko się blokują i obciążenie samo się wyrównuje.
     */
    static void workStealing() {
        section("6. Work stealing i wspólna pula commonPool");

        // Która część trafiła do którego wątku — zależy od uruchomienia; liczymy tylko fakty niezależne od przeplotu.
        ConcurrentHashMap<String, AtomicInteger> perThread = new ConcurrentHashMap<>();
        ForkJoinPool pool = new ForkJoinPool(3);
        try {
            int total = pool.submit(() -> IntStream.range(0, 3000).parallel()
                    .map(i -> {
                        perThread.computeIfAbsent(Thread.currentThread().getName(), k -> new AtomicInteger())
                                .incrementAndGet();
                        return 1;
                    })
                    .sum()).get(10, TimeUnit.SECONDS);
            show("przetworzonych elementów", total);
            // WYNIK: przetworzonych elementów → 3000
            show("suma z liczników wątków", perThread.values().stream().mapToInt(AtomicInteger::get).sum());
            // WYNIK: suma z liczników wątków → 3000
        } catch (Exception e) {
            show("błąd", e);
        } finally {
            shutdownAndAwait(pool);
        }
        // Uwaga: strumień równoległy uruchomiony wewnątrz pool.submit(...) jest na HotSpot liczony zwykle przez
        // TĘ pulę, a nie przez commonPool. To zachowanie implementacji, NIE udokumentowana gwarancja — nie opieraj
        // na nim krytycznego kodu. Ile wątków faktycznie pracowało — (wynik zależy od uruchomienia).

        ForkJoinPool common = ForkJoinPool.commonPool();   // commonPool = wspólna pula całej JVM
        show("commonPool: poziom równoległości > 0", common.getParallelism() > 0);   // getParallelism = pobierz poziom równoległości
        // WYNIK: commonPool: poziom równoległości > 0 → true
        show("dostępne procesory > 0", Runtime.getRuntime().availableProcessors() > 0);
        // WYNIK: dostępne procesory > 0 → true
        // commonPool ma zwykle (liczba procesorów − 1) wątków, bo wątek wołający też pomaga liczyć.
        // Nie zamyka się go (shutdown() na commonPool nic nie robi) — wątki są demonami.
    }

    // =================================================================================================
    // 7. STRUMIENIE RÓWNOLEGŁE — KIEDY POMAGAJĄ, KIEDY SZKODZĄ
    // =================================================================================================

    /**
     * 7. {@code parallelStream()} dzieli dane jak RecursiveTask i liczy je w commonPool (oraz w wątku wołającym).
     * Sprawdzamy fakt: każdy element przetworzył wątek main albo wątek wspólnej puli.
     */
    static void parallelStreamsPool() {
        section("7. Strumienie równoległe — commonPool, zyski i pułapki");

        Set<String> threadNames = ConcurrentHashMap.newKeySet();   // newKeySet = nowy zbiór kluczy (współbieżny Set)
        long sum = LongStream.rangeClosed(1, 200_000).parallel()
                .peek(x -> threadNames.add(Thread.currentThread().getName()))   // peek = podejrzyj (tu tylko demo!)
                .sum();
        show("suma równoległa 1..200 000", sum);
        // WYNIK: suma równoległa 1..200 000 → 20000100000
        boolean onlyMainOrCommon = threadNames.stream()
                .allMatch(n -> n.equals("main") || n.startsWith("ForkJoinPool.commonPool-worker-"));
        show("tylko main albo ForkJoinPool.commonPool-worker-*", onlyMainOrCommon);
        // WYNIK: tylko main albo ForkJoinPool.commonPool-worker-* → true
        // Ile wątków i które dokładnie — (wynik zależy od uruchomienia), dlatego wypisujemy tylko fakt.

        // PUŁAPKA: współdzielony stan w strumieniu równoległym. ArrayList.add z wielu wątków gubi elementy albo
        // rzuca ArrayIndexOutOfBoundsException (wynik zależy od uruchomienia — nie uruchamiamy).
        //     List<Integer> out = new ArrayList<>();
        //     IntStream.range(0, 1000).parallel().forEach(out::add);       // ŹLE
        // Poprawnie: niech strumień sam zbierze wynik (kolekcjoner łączy częściowe listy bezpiecznie).
        List<Integer> out = IntStream.range(0, 1000).parallel().boxed().toList();   // boxed = opakuj w Integer
        show("toList() z parallel: rozmiar", out.size());
        // WYNIK: toList() z parallel: rozmiar → 1000
        show("kolejność spotkania zachowana", out.get(0) + ", " + out.get(1) + " … " + out.get(999));
        // WYNIK: kolejność spotkania zachowana → 0, 1 … 999

        // Kiedy parallel POMAGA: dużo danych (setki tysięcy+), kosztowne obliczenia na element, łatwo dzielące się
        // źródło (tablica, ArrayList, IntStream.range), brak stanu współdzielonego i brak blokowania.
        // Kiedy SZKODZI:
        //   • mało danych — narzut dzielenia i łączenia większy niż praca,
        //   • LinkedList, Stream.iterate, BufferedReader.lines() — źle się dzielą,
        //   • blokujące IO w lambdzie — zajmuje wątki commonPool, z której korzystają WSZYSTKIE strumienie
        //     równoległe i CompletableFuture.supplyAsync bez executora → cała aplikacja zwalnia,
        //   • operacje zależne od kolejności (findFirst, limit, forEachOrdered) na dużych danych — dużo synchronizacji.
        // DOBRA PRAKTYKA: domyślnie strumień sekwencyjny; parallel tylko po pomiarze (JMH) pokazującym zysk.
        // Dlaczego: równoległość ma koszt i łatwo wprowadza błędy; w aplikacji webowej rdzenie i tak są zajęte
        // obsługą równoległych żądań.
    }

    // =================================================================================================
    // 8. SPRING: @Scheduled
    // =================================================================================================

    /**
     * 8. W Springu nie tworzysz ScheduledExecutorService ręcznie — wystarczy adnotacja.
     */
    static void springScheduling() {
        section("8. Spring — @Scheduled (tylko opis)");

        // W Springu (po @EnableScheduling):
        //     @Scheduled(fixedRate = 60_000)          → jak scheduleAtFixedRate (co minutę od startu do startu)
        //     @Scheduled(fixedDelay = 5_000)          → jak scheduleWithFixedDelay (5 s po zakończeniu)
        //     @Scheduled(cron = "0 0 2 * * *")        → codziennie o 2:00 (cron Springa: sekunda minuta godzina
        //                                               dzień miesiąc dzień-tygodnia)
        // PUŁAPKA: w Spring Boot domyślny planista ma JEDEN wątek — wolne zadanie opóźnia pozostałe (jak Timer).
        // Dlaczego: tak skonfigurowano ThreadPoolTaskScheduler; zwiększ spring.task.scheduling.pool.size.
        // PUŁAPKA: wyjątek w metodzie @Scheduled Spring loguje i NIE zatrzymuje kolejnych wywołań (inaczej niż
        // gołe scheduleAtFixedRate!) — ale i tak łap wyjątki, które umiesz obsłużyć.
        List<String> mapping = List.of(
                "schedule(zad, 5, SECONDS) → @Scheduled(initialDelay = ...) / TaskScheduler.schedule",
                "scheduleAtFixedRate → @Scheduled(fixedRate = ...)",
                "scheduleWithFixedDelay → @Scheduled(fixedDelay = ...)",
                "ForkJoinPool / parallel → zwykle niepotrzebne w aplikacji webowej");
        showEach("Java SE → Spring", mapping);
        // WYNIK: Java SE → Spring (liczba elementów: 4):
        // WYNIK: • schedule(zad, 5, SECONDS) → @Scheduled(initialDelay = ...) / TaskScheduler.schedule
        // WYNIK: • scheduleAtFixedRate → @Scheduled(fixedRate = ...)
        // WYNIK: • scheduleWithFixedDelay → @Scheduled(fixedDelay = ...)
        // WYNIK: • ForkJoinPool / parallel → zwykle niepotrzebne w aplikacji webowej
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Executors.newScheduledThreadPool(n, fabryka): schedule (raz), scheduleAtFixedRate (rytm od startu),
     *     scheduleWithFixedDelay (przerwa od końca). Przebiegi jednego zadania nigdy się nie nakładają.
     *   • Zadanie cykliczne kończy tylko cancel(), shutdown() puli albo WYJĄTEK — ten ostatni po cichu.
     *     → ciało zadania w try/catch(RuntimeException).
     *   • get() na zadaniu cyklicznym wraca dopiero po anulowaniu (CancellationException) albo wyjątku
     *     (ExecutionException z przyczyną w getCause()).
     *   • Timer: 1 wątek, wyjątek zabija wszystkie zadania — w nowym kodzie nie używaj.
     *   • RecursiveTask.compute(): mały kawałek → wprost; duży → left.fork(); right.compute(); left.join().
     *   • Work stealing: własna kolejka na wątek, bezczynni kradną z drugiego końca cudzych kolejek.
     *   • parallelStream() i supplyAsync() bez executora → commonPool (wspólna dla całej JVM).
     *   • parallel: tylko duże dane, kosztowne obliczenia, bez stanu współdzielonego i bez blokowania — po pomiarze.
     *   • Spring: @Scheduled(fixedRate / fixedDelay / cron); domyślnie 1 wątek planisty.
     *
     * PYTANIA KONTROLNE:
     *   1. Zadanie trwa 30 ms. Ile mniej więcej trwa cykl przy scheduleAtFixedRate(…, 100 ms), a ile przy
     *      scheduleWithFixedDelay(…, 100 ms)?
     *   2. ZNAJDŹ BŁĄD (raport co godzinę „czasem przestaje się generować”):
     *        scheduler.scheduleAtFixedRate(() -> reportService.generate(), 0, 1, TimeUnit.HOURS);
     *   3. Co wypisze (pula ForkJoin, próg 10, tablica 80 elementów, podział na pół, liczymy liście)?
     *        System.out.println(leaves.get());
     *   4. ZNAJDŹ BŁĄD w compute():
     *        left.fork(); long l = left.join(); long r = right.compute(); return l + r;
     *   5. Dlaczego blokujące IO w parallelStream() może spowolnić niezwiązane fragmenty aplikacji?
     *   6. Co wypisze:
     *        System.out.println(IntStream.rangeClosed(1, 4).parallel().boxed().toList());
     *   7. Czym różni się Timer od ScheduledExecutorService (dwie różnice)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        List<Integer> multiplesOf3 = List.of(3, 6, 9, 12, 15, 18, 21, 24, 27, 30);
        Check.equal("ćw. 1: schedule — iloczyn po 10 ms", 42, () -> exercise1(6, 7));
        Check.equal("ćw. 2: wielokrotności 3 równolegle (bez stanu współdzielonego)", multiplesOf3, () -> exercise2(30));
        Check.equal("ćw. 3: RecursiveTask — liczba parzystych w 1..100000", 50_000L, () -> exercise3(100_000, 1_000));
        Check.equal("ćw. 4: odporne zadanie cykliczne", "przebiegów: 6, błędów: 2", () -> exercise4(6));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 42, () -> solution1(6, 7));
        Check.equal("ćw. 2 (wzorzec)", multiplesOf3, () -> solution2(30));
        Check.equal("ćw. 3 (wzorzec)", 50_000L, () -> solution3(100_000, 1_000));
        Check.equal("ćw. 4 (wzorzec)", "przebiegów: 6, błędów: 2", () -> solution4(6));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zaplanuj w ScheduledExecutorService (1 wątek, własna nazwa) obliczenie a * b
     * za 10 ms i zwróć wynik. Pamiętaj o zamknięciu planisty.
     * Podpowiedź: scheduler.schedule(() -> a * b, 10, TimeUnit.MILLISECONDS).get(5, TimeUnit.SECONDS).
     */
    static int exercise1(int a, int b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ błędny kod z efektem ubocznym na bezpieczny strumień równoległy.
     * <pre>{@code
     * List<Integer> result = new ArrayList<>();
     * IntStream.rangeClosed(1, n).parallel().filter(x -> x % 3 == 0).forEach(result::add);   // wyścig!
     * }</pre>
     * Zwróć wielokrotności 3 z zakresu 1..n w kolejności rosnącej.
     * Podpowiedź: .boxed().toList() (Java 16+) zachowuje kolejność spotkania także w strumieniu równoległym.
     */
    static List<Integer> exercise2(int n) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): napisz RecursiveTask liczący PARZYSTE liczby w tablicy int[] wartości 1..n
     * (podział na pół, próg {@code threshold}) i uruchom go w new ForkJoinPool(4). Zwróć liczbę (long).
     * Podpowiedź: wzoruj się na SumTask; tablica: IntStream.rangeClosed(1, n).toArray(); możesz napisać
     * zagnieżdżoną klasę EvenCountTask albo użyć klasy z rozwiązania dopiero po próbie.
     */
    static long exercise3(int n, int threshold) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zaplanuj zadanie scheduleWithFixedDelay (co 5 ms), które w KAŻDYM przebiegu
     * zwiększa licznik, a w przebiegach 2 i 4 rzuca IllegalStateException. Zadanie ma przeżyć błędy i wykonać
     * dokładnie {@code runs} przebiegów (potem nic nie robi; main czeka na zatrzask i anuluje).
     * Zwróć tekst „przebiegów: X, błędów: Y”.
     * Podpowiedź: try/catch(RuntimeException) wewnątrz zadania, licznik błędów, countDown w finally,
     * na początku zadania „if (licznik ≥ runs) return;”.
     */
    static String exercise4(int runs) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int a, int b) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, named("cw1"));
        try {
            return scheduler.schedule(() -> a * b, 10, TimeUnit.MILLISECONDS).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            shutdownAndAwait(scheduler);
        }
    }

    static List<Integer> solution2(int n) {
        return IntStream.rangeClosed(1, n).parallel().filter(x -> x % 3 == 0).boxed().toList();
    }

    static final class EvenCountTask extends RecursiveTask<Long> {   // even count = liczba parzystych
        private static final long serialVersionUID = 1L;
        private final int[] data;
        private final int from;
        private final int to;
        private final int threshold;

        EvenCountTask(int[] data, int from, int to, int threshold) {
            this.data = data;
            this.from = from;
            this.to = to;
            this.threshold = threshold;
        }

        @Override
        protected Long compute() {
            if (to - from <= threshold) {
                long count = 0;
                for (int i = from; i < to; i++) {
                    if (data[i] % 2 == 0) {
                        count++;
                    }
                }
                return count;
            }
            int mid = (from + to) >>> 1;
            EvenCountTask left = new EvenCountTask(data, from, mid, threshold);
            EvenCountTask right = new EvenCountTask(data, mid, to, threshold);
            left.fork();
            long r = right.compute();
            return left.join() + r;
        }
    }

    static long solution3(int n, int threshold) {
        int[] data = IntStream.rangeClosed(1, n).toArray();
        ForkJoinPool pool = new ForkJoinPool(4);
        try {
            return pool.invoke(new EvenCountTask(data, 0, data.length, threshold));
        } finally {
            shutdownAndAwait(pool);
        }
    }

    static String solution4(int runs) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, named("cw4"));
        AtomicInteger count = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(runs);
        try {
            ScheduledFuture<?> handle = scheduler.scheduleWithFixedDelay(() -> {
                if (count.get() >= runs) {
                    return;
                }
                try {
                    int n = count.incrementAndGet();
                    if (n == 2 || n == 4) {
                        throw new IllegalStateException("awaria " + n);
                    }
                } catch (RuntimeException e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            }, 0, 5, TimeUnit.MILLISECONDS);
            await(done);
            handle.cancel(false);
        } finally {
            shutdownAndAwait(scheduler);
        }
        return "przebiegów: " + count.get() + ", błędów: " + errors.get();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Fixed rate: start co ~100 ms (30 ms pracy + 70 ms przerwy). Fixed delay: ~130 ms (30 ms pracy
     *      + 100 ms przerwy od końca).
     *   2. Jeśli generate() choć raz rzuci wyjątek (np. chwilowy brak bazy), kolejne wykonania zostaną wstrzymane
     *      bez żadnego komunikatu. Trzeba otoczyć ciało try/catch(RuntimeException) i logować błąd.
     *   3. 8 — 80 → 40 → 20 → 10; kawałek 10 ≤ progu, więc liście mają po 10 elementów: 80 / 10 = 8.
     *   4. join() zaraz po fork() czeka na lewą połowę, zanim zacznie liczyć prawą — praca idzie sekwencyjnie.
     *      Poprawnie: left.fork(); long r = right.compute(); return left.join() + r;
     *   5. Strumienie równoległe (i CompletableFuture bez executora) dzielą JEDNĄ commonPool z niewielką liczbą
     *      wątków. Wątki zablokowane na IO nie liczą nic innego, więc inne strumienie i zadania czekają w kolejce.
     *   6. [1, 2, 3, 4] — toList() zachowuje kolejność spotkania także w strumieniu równoległym.
     *   7. Timer ma jeden wątek (wolne zadanie opóźnia inne), a wyjątek w TimerTask zabija wątek i anuluje
     *      wszystkie zadania; ScheduledExecutorService może mieć wiele wątków, wyjątek zatrzymuje tylko to jedno
     *      zadanie, a wynik/wyjątek jest dostępny przez ScheduledFuture.
     */
    // </editor-fold>
}
