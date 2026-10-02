package t21_concurrency;

import helpers.Check;
import helpers.SampleData;
import helpers.Sleep;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Pule wątków — ExecutorService, Callable, Future
 *        (executor = wykonawca, pool = pula, callable = zadanie z wynikiem, future = przyszły wynik,
 *         submit = zgłoś, shutdown = zamknij)
 *
 * W SKRÓCIE:
 *   Zamiast tworzyć nowy wątek dla każdego zadania, oddajemy zadania PULI: stała grupa wątków bierze je z kolejki
 *   i wykonuje po kolei. Wynik (albo wyjątek) zadania odbieramy przez Future. Pulę trzeba zawsze zamknąć.
 *
 * ANALOGIA:
 *   Restauracja nie zatrudnia nowego kelnera dla każdego gościa (i nie zwalnia go po obiedzie). Ma stały zespół
 *   kilku kelnerów i kolejkę zamówień. Zamówienie (zadanie) trafia do kolejki, wolny kelner (wątek) je obsługuje,
 *   a gość dostaje numerek (Future), z którym potem odbiera danie (wynik). Wieczorem restauracja się zamyka (shutdown):
 *   nowych gości nie przyjmuje, ale obsługuje tych, którzy już zamówili.
 *
 * JAK TO DZIAŁA:
 *   • ExecutorService pool = Executors.newFixedThreadPool(4, fabrykaWątków);   // 4 wątki wielokrotnego użytku
 *     Future<Integer> f = pool.submit(() -> oblicz());                        // zgłoszenie zadania
 *     Integer wynik = f.get();                                                // czeka na wynik
 *     pool.shutdown(); pool.awaitTermination(...);                            // ZAWSZE zamknij
 *   • Runnable  — void run(), bez wyniku, nie może rzucić wyjątku sprawdzanego.
 *     Callable<V> — V call() throws Exception: zwraca wynik i może rzucić dowolny wyjątek.
 *   • Cykl życia puli:  DZIAŁA ──shutdown()──▶ ZAMYKANA (kończy zgłoszone, odrzuca nowe) ──▶ ZAKOŃCZONA
 *                              └─shutdownNow()─▶ przerywa działające, zwraca nierozpoczęte
 *   • ThreadPoolExecutor (to, co jest w środku Executors.*):
 *       nowe zadanie → wątków < core? nowy wątek : kolejka nie pełna? do kolejki :
 *                      wątków < max? nowy wątek : ODRZUCENIE (RejectedExecutionHandler)
 *
 * SŁÓWKA:
 *   executor = wykonawca; executor service = usługa wykonawcza (pula z zarządzaniem); pool = pula; task = zadanie;
 *   callable = wywoływalny (zadanie z wynikiem); future = przyszłość (przyszły wynik); submit = zgłoś; execute = wykonaj;
 *   invoke all/any = wywołaj wszystkie/dowolne; shutdown = zamknięcie; await termination = czekaj na zakończenie;
 *   reject = odrzuć; core = rdzeń (podstawowa liczba wątków); keep alive = utrzymanie przy życiu; factory = fabryka;
 *   cancel = anuluj; timeout = limit czasu; caller runs = wołający wykonuje.
 *
 * ZOBACZ TEŻ: t21_concurrency/Concurrency01Threads (koszt pojedynczych wątków),
 *             t21_concurrency/Concurrency05CompletableFuture (łączenie wyników bez blokującego get()),
 *             t21_concurrency/Concurrency09ScheduledForkJoin (pule z harmonogramem, ForkJoinPool),
 *             t21_concurrency/Concurrency06ConcurrentCollections (kolejki blokujące w pulach).
 * </pre>
 */
public class Concurrency04Executors {

    public static void main(String[] args) throws Exception {
        title("Concurrency04 — pule wątków: ExecutorService, Callable, Future");

        whyPools();                 // why pools = po co pule
        runnableVersusCallable();   // runnable versus callable = Runnable kontra Callable
        futureApi();                // future API = obsługa Future
        invokeAllAndAny();          // invoke all and any = wywołaj wszystkie i dowolne
        exceptionsInTasks();        // exceptions in tasks = wyjątki w zadaniach
        lifecycle();                // lifecycle = cykl życia puli
        threadPoolExecutorParams(); // thread pool executor params = parametry ThreadPoolExecutor
        sizingAndSpring();          // sizing and Spring = dobór rozmiaru i Spring
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // NARZĘDZIA: WŁASNA FABRYKA WĄTKÓW I POPRAWNE ZAMYKANIE PULI
    // =================================================================================================

    /** Fabryka wątków z czytelnymi nazwami: prefiks-1, prefiks-2, … */
    static class NamedThreadFactory implements ThreadFactory {   // named thread factory = fabryka nazwanych wątków
        private final String prefix;                              // prefix = przedrostek
        private final AtomicInteger counter = new AtomicInteger();

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {   // newThread = nowy wątek — pula woła to, gdy potrzebuje wątku
            return new Thread(r, prefix + "-" + counter.incrementAndGet());
        }
    }

    /** Wzorzec z dokumentacji ExecutorService: łagodne zamknięcie, a gdy nie wystarczy — twarde. */
    static void shutdownAndAwait(ExecutorService pool) {   // zamknij i poczekaj
        pool.shutdown();   // nie przyjmuj nowych zadań, dokończ zgłoszone
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {   // await termination = czekaj na zakończenie
                pool.shutdownNow();   // przerwij to, co wciąż działa
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // =================================================================================================
    // 1. PO CO PULE
    // =================================================================================================

    /**
     * 1. Pula tworzy wątki raz i używa ich wielokrotnie. 20 zadań wykona się na 2 wątkach — zamiast 20 nowych.
     * Zbieramy nazwy wątków do zbioru współbieżnego i wypisujemy je POSORTOWANE.
     */
    static void whyPools() throws Exception {
        section("1. Po co pule — wątki wielokrotnego użytku");

        ExecutorService pool = Executors.newFixedThreadPool(2, new NamedThreadFactory("pula"));   // newFixedThreadPool = nowa pula o stałym rozmiarze
        Set<String> usedThreads = ConcurrentHashMap.newKeySet();   // zbiór bezpieczny wątkowo (Concurrency06)
        AtomicInteger done = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 20; i++) {
                futures.add(pool.submit(() -> {               // submit = zgłoś zadanie
                    usedThreads.add(Thread.currentThread().getName());
                    done.incrementAndGet();
                }));
            }
            for (Future<?> f : futures) {
                f.get();   // czekamy na każde zadanie
            }
        } finally {
            shutdownAndAwait(pool);
        }
        show("wykonano zadań", done.get());
        // WYNIK: wykonano zadań → 20
        show("użyte wątki (posortowane)", new TreeSet<>(usedThreads));
        // WYNIK: użyte wątki (posortowane) → [pula-1, pula-2]
        // Dlaczego na pewno oba wątki: pula o stałym rozmiarze tworzy nowy wątek dla każdego z pierwszych zadań,
        // dopóki nie osiągnie rozmiaru 2 — i ten nowy wątek wykonuje właśnie to zadanie. Który wątek wykonał
        // ile pozostałych zadań — zależy od uruchomienia, więc tego nie wypisujemy.

        // Zalety puli: (1) brak kosztu tworzenia wątku dla każdego zadania; (2) LIMIT współbieżności — 1000 żądań
        // nie utworzy 1000 wątków, tylko poczeka w kolejce; (3) zarządzanie: zamknięcie, anulowanie, wyniki przez Future.
        // DOBRA PRAKTYKA: własna ThreadFactory z nazwami — dlaczego: w logach i zrzutach wątków od razu widać, do której
        // puli należy wątek („pula-zamowien-3” zamiast „pool-7-thread-3”).
    }

    // =================================================================================================
    // 2. RUNNABLE KONTRA CALLABLE; submit KONTRA execute
    // =================================================================================================

    /**
     * 2. {@code submit(Runnable)} daje {@code Future<?>}, którego {@code get()} zwraca {@code null} (brak wyniku).
     * {@code submit(Callable)} daje {@code Future<V>} z wynikiem. {@code execute(Runnable)} nie daje niczego.
     */
    static void runnableVersusCallable() throws Exception {
        section("2. Runnable kontra Callable, submit kontra execute");

        ExecutorService pool = Executors.newFixedThreadPool(2, new NamedThreadFactory("zadania"));
        try {
            Runnable logTask = () -> { };   // log task = zadanie logowania (nic nie zwraca)
            Future<?> f1 = pool.submit(logTask);
            show("Future z Runnable — get()", f1.get());
            // WYNIK: Future z Runnable — get() → null

            Callable<Integer> answer = () -> 6 * 7;   // Callable — zwraca wynik
            Future<Integer> f2 = pool.submit(answer);
            show("Future z Callable — get()", f2.get());
            // WYNIK: Future z Callable — get() → 42

            // Callable może rzucić wyjątek SPRAWDZANY (np. IOException) — Runnable nie może.
            Callable<String> readsFile = () -> {     // reads file = czyta plik
                if (Boolean.parseBoolean("false")) {   // warunek zawsze fałszywy — pokazujemy tylko, że WOLNO rzucić
                    throw new java.io.IOException("brak pliku");
                }
                return "zawartość pliku";
            };
            show("Callable z throws Exception", pool.submit(readsFile).get());
            // WYNIK: Callable z throws Exception → zawartość pliku

            // execute = wykonaj — tylko Runnable, bez Future: nie odbierzesz wyniku ani wyjątku (sekcja 5).
            CountDownLatch executed = new CountDownLatch(1);
            pool.execute(executed::countDown);
            show("execute() wykonało zadanie", executed.await(5, TimeUnit.SECONDS));
            // WYNIK: execute() wykonało zadanie → true
        } finally {
            shutdownAndAwait(pool);
        }
        // Jak kompilator wybiera: lambda zwracająca wartość ( () -> 42 ) pasuje do Callable, lambda bez wyniku
        // ( () -> lista.clear() z ciałem-instrukcją ) — do Runnable. Przy wątpliwościach przypisz lambdę do zmiennej
        // z jawnym typem, jak wyżej (logTask, answer).
    }

    // =================================================================================================
    // 3. FUTURE — get, get(limit), cancel, isDone
    // =================================================================================================

    /**
     * 3. {@code get()} blokuje do końca zadania. {@code get(czas, jednostka)} czeka najwyżej podany czas i rzuca
     * {@code TimeoutException}. {@code cancel(true)} anuluje zadanie i przerywa wątek, który je wykonuje.
     */
    static void futureApi() throws Exception {
        section("3. Future — get, get(limit), cancel, isDone");

        ExecutorService pool = Executors.newFixedThreadPool(2, new NamedThreadFactory("future"));
        CountDownLatch never = new CountDownLatch(1);   // never = nigdy (nikt go nie zwolni)
        try {
            Future<Integer> quick = pool.submit(() -> 6 * 7);   // quick = szybkie
            show("get()", quick.get());
            // WYNIK: get() → 42
            show("isDone() po get()", quick.isDone());   // isDone = czy zakończone
            // WYNIK: isDone() po get() → true

            // Zadanie, które czeka na zatrzask, którego nikt nie zwolni — limit czasu MUSI upłynąć.
            Future<String> stuck = pool.submit(() -> {   // stuck = utknięte
                never.await();
                return "nigdy";
            });
            expectThrows("get(200 ms) na zadaniu, które nie kończy się", () -> stuck.get(200, TimeUnit.MILLISECONDS));
            // WYNIK: ✔ get(200 ms) na zadaniu, które nie kończy się → rzucono TimeoutException: (brak komunikatu)
            show("isDone() po TimeoutException", stuck.isDone());
            // WYNIK: isDone() po TimeoutException → false
            // PUŁAPKA: TimeoutException z get() NIE zatrzymuje zadania — dlaczego: to tylko nasze czekanie się skończyło;
            // zadanie dalej zajmuje wątek puli. Jeśli wynik już nas nie obchodzi — anuluj.

            boolean cancelled = stuck.cancel(true);   // cancel(true) = anuluj i przerwij wątek (interrupt)
            show("cancel(true) się udał", cancelled);
            // WYNIK: cancel(true) się udał → true
            show("isCancelled()", stuck.isCancelled());   // isCancelled = czy anulowane
            // WYNIK: isCancelled() → true
            show("isDone() po anulowaniu", stuck.isDone());
            // WYNIK: isDone() po anulowaniu → true
            expectThrows("get() na anulowanym", stuck::get);
            // WYNIK: ✔ get() na anulowanym → rzucono CancellationException: (brak komunikatu)
            show("cancel na już zakończonym zwraca", quick.cancel(true));
            // WYNIK: cancel na już zakończonym zwraca → false
        } finally {
            never.countDown();   // na wszelki wypadek (gdyby anulowanie się nie udało)
            shutdownAndAwait(pool);
        }
        // PUŁAPKA: isDone() jest true także dla zadań anulowanych i zakończonych wyjątkiem — „done” znaczy
        // „już się nie wykonuje”, a nie „zakończone sukcesem”.
        // PUŁAPKA: cancel(true) tylko PRZERYWA wątek (interrupt). Zadanie, które nie sprawdza flagi ani nie woła metod
        // blokujących (np. długa pętla obliczeń), będzie liczyć dalej — anulowanie jest kooperacyjne (Concurrency01).
    }

    // =================================================================================================
    // 4. invokeAll I invokeAny
    // =================================================================================================

    /**
     * 4. {@code invokeAll} wykonuje listę zadań i zwraca listę Future W KOLEJNOŚCI ZADAŃ (nie zakończenia), dopiero gdy
     * wszystkie się skończą. {@code invokeAny} zwraca wynik JEDNEGO zadania zakończonego sukcesem, resztę anuluje.
     */
    static void invokeAllAndAny() throws Exception {
        section("4. invokeAll i invokeAny");

        ExecutorService pool = Executors.newFixedThreadPool(3, new NamedThreadFactory("invoke"));
        try {
            // Pierwsze zadanie jest najwolniejsze — a mimo to jego wynik jest pierwszy na liście.
            List<Callable<String>> tasks = List.of(
                    () -> { Sleep.ms(60); return "cena: 79 zł"; },
                    () -> { Sleep.ms(20); return "stan: 15 szt."; },
                    () -> "opinie: 4.8");
            List<Future<String>> results = pool.invokeAll(tasks);   // invokeAll = wywołaj wszystkie (czeka na wszystkie)
            List<String> values = new ArrayList<>();
            for (Future<String> f : results) {
                values.add(f.get());   // nie blokuje — po invokeAll wszystkie są isDone()
            }
            show("invokeAll — wyniki w kolejności zadań", values);
            // WYNIK: invokeAll — wyniki w kolejności zadań → [cena: 79 zł, stan: 15 szt., opinie: 4.8]

            // invokeAny = wywołaj dowolne: pierwszy UDANY wynik. Tu tylko jedno zadanie kończy się sukcesem,
            // więc wynik jest pewny. (Gdyby udały się dwa — nie wiadomo, które wygra.)
            List<Callable<String>> mirrors = List.of(   // mirrors = serwery lustrzane
                    () -> { throw new IllegalStateException("serwer A niedostępny"); },
                    () -> "plik z serwera B",
                    () -> { throw new IllegalStateException("serwer C niedostępny"); });
            show("invokeAny — pierwszy udany", pool.invokeAny(mirrors));
            // WYNIK: invokeAny — pierwszy udany → plik z serwera B

            // Gdy WSZYSTKIE zawiodą, invokeAny rzuca ExecutionException.
            List<Callable<String>> allFail = List.of(
                    () -> { throw new IllegalStateException("A"); },
                    () -> { throw new IllegalStateException("B"); });
            String outcome;
            try {
                outcome = pool.invokeAny(allFail);
            } catch (ExecutionException e) {
                outcome = "rzucono " + e.getClass().getSimpleName() + ", przyczyna: " + e.getCause().getClass().getSimpleName();
            }
            show("invokeAny — wszystkie zawiodły", outcome);
            // WYNIK: invokeAny — wszystkie zawiodły → rzucono ExecutionException, przyczyna: IllegalStateException
        } finally {
            shutdownAndAwait(pool);
        }
        // DOBRA PRAKTYKA: zbieraj Future w liście w kolejności zgłaszania i odczytuj w tej kolejności — dlaczego: dostajesz
        // deterministyczny porządek wyników, niezależnie od tego, które zadanie skończyło się pierwsze.
        // Gdy chcesz przetwarzać wyniki W KOLEJNOŚCI ZAKOŃCZENIA — ExecutorCompletionService (osobne narzędzie JDK).
    }

    // =================================================================================================
    // 5. WYJĄTKI W ZADANIACH
    // =================================================================================================

    /**
     * 5. submit(): wyjątek z zadania jest ZŁAPANY i schowany w Future — wyjdzie dopiero z {@code get()} jako
     * {@code ExecutionException} (przyczyna w {@code getCause()}). execute(): wyjątek trafia do obsługi
     * nieprzechwyconych wyjątków wątku, a domyślnie — na System.err.
     */
    static void exceptionsInTasks() throws Exception {
        section("5. Wyjątki w zadaniach — submit kontra execute");

        ExecutorService pool = Executors.newFixedThreadPool(2, new NamedThreadFactory("błędy"));
        try {
            Future<Integer> failing = pool.submit(() -> Integer.parseInt("dwa"));   // NumberFormatException w zadaniu
            show("zadanie się zakończyło (wyjątkiem), isDone()", waitDone(failing));
            // WYNIK: zadanie się zakończyło (wyjątkiem), isDone() → true
            try {
                failing.get();
            } catch (ExecutionException e) {   // execution exception = wyjątek wykonania (opakowanie)
                show("get() rzucił", e.getClass().getSimpleName());
                // WYNIK: get() rzucił → ExecutionException
                Throwable cause = e.getCause();   // getCause = pobierz przyczynę — prawdziwy wyjątek z zadania
                show("przyczyna", cause.getClass().getSimpleName() + ": " + cause.getMessage());
                // WYNIK: przyczyna → NumberFormatException: For input string: "dwa"
            }
        } finally {
            shutdownAndAwait(pool);
        }
        // PUŁAPKA: submit() i NIGDY get() = wyjątek przepada bez śladu — dlaczego: Future trzyma go do odbioru,
        // nikt nie odbiera, nic się nie wypisuje. Zadanie „po cichu nie działa”.

        // execute(): wyjątek „ucieka” z wątku puli. Gdyby nie nasza obsługa, JVM wypisałaby stos wywołań na System.err.
        // Fabryka ustawia każdemu wątkowi UncaughtExceptionHandler (Concurrency01Threads, sekcja 9).
        List<String> reported = new ArrayList<>();   // reported = zgłoszone (pisze obsługa, czyta main po await)
        CountDownLatch handled = new CountDownLatch(1);
        ThreadFactory withHandler = r -> {
            Thread t = new Thread(r, "execute-1");
            t.setUncaughtExceptionHandler((thread, ex) -> {
                synchronized (reported) {
                    reported.add(ex.getClass().getSimpleName() + ": " + ex.getMessage());
                }
                handled.countDown();
            });
            return t;
        };
        ExecutorService pool2 = Executors.newSingleThreadExecutor(withHandler);   // newSingleThreadExecutor = pula jednowątkowa
        try {
            pool2.execute(() -> {
                throw new IllegalStateException("błąd w execute()");
            });
            show("obsługa wyjątku zadziałała", handled.await(5, TimeUnit.SECONDS));
            // WYNIK: obsługa wyjątku zadziałała → true
            synchronized (reported) {
                show("zgłoszono", reported.get(0));
                // WYNIK: zgłoszono → IllegalStateException: błąd w execute()
            }
        } finally {
            shutdownAndAwait(pool2);
        }
        // Wątek, z którego uciekł wyjątek, umiera; pula tworzy na jego miejsce NOWY wątek (koszt + zmiana nazwy w logach).
        // DOBRA PRAKTYKA: łap wyjątki wewnątrz zadania i loguj je tam (najwięcej kontekstu) albo używaj submit()
        // i ZAWSZE odbieraj Future. W Springu dla @Async void ustawia się AsyncUncaughtExceptionHandler — ta sama idea.
    }

    /** Czeka (najwyżej 5 s), aż Future będzie zakończony. */
    static boolean waitDone(Future<?> future) {
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException | java.util.concurrent.CancellationException e) {
            // zakończone wyjątkiem lub anulowane — też „done”
        } catch (TimeoutException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return future.isDone();
    }

    // =================================================================================================
    // 6. CYKL ŻYCIA: shutdown, shutdownNow, awaitTermination
    // =================================================================================================

    /**
     * 6. Wątki puli NIE są demonami — niezamknięta pula trzyma JVM przy życiu w nieskończoność.
     * {@code shutdown()} = łagodnie (dokończ zgłoszone), {@code shutdownNow()} = twardo (przerwij i oddaj nierozpoczęte).
     */
    static void lifecycle() throws Exception {
        section("6. Cykl życia puli: shutdown, shutdownNow, awaitTermination");

        // a) shutdown(): kolejka zostaje dokończona, nowe zadania są odrzucane.
        ExecutorService pool = Executors.newFixedThreadPool(2, new NamedThreadFactory("cykl"));
        AtomicInteger finished = new AtomicInteger();
        for (int i = 0; i < 5; i++) {
            pool.submit(() -> {
                Sleep.ms(10);
                finished.incrementAndGet();
            });
        }
        pool.shutdown();
        show("isShutdown() po shutdown()", pool.isShutdown());   // isShutdown = czy zamykana
        // WYNIK: isShutdown() po shutdown() → true
        String rejected;
        try {
            pool.submit(() -> { });
            rejected = "przyjęto (nie powinno się zdarzyć)";
        } catch (RejectedExecutionException e) {
            // Komunikat zawiera opis zadania i puli z kodami skrótu (różne przy każdym uruchomieniu) — wypisujemy tylko typ.
            rejected = e.getClass().getSimpleName();
        }
        show("submit po shutdown()", rejected);
        // WYNIK: submit po shutdown() → RejectedExecutionException
        boolean terminated = pool.awaitTermination(10, TimeUnit.SECONDS);
        show("awaitTermination — zakończona w limicie", terminated);
        // WYNIK: awaitTermination — zakończona w limicie → true
        show("dokończono zadań zgłoszonych przed shutdown()", finished.get());
        // WYNIK: dokończono zadań zgłoszonych przed shutdown() → 5
        show("isTerminated()", pool.isTerminated());   // isTerminated = czy zakończona
        // WYNIK: isTerminated() → true

        // b) shutdownNow(): przerywa działające zadania i zwraca listę tych, które nie zdążyły wystartować.
        ExecutorService single = Executors.newSingleThreadExecutor(new NamedThreadFactory("twardy"));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch never = new CountDownLatch(1);
        String[] firstTask = new String[1];
        CountDownLatch firstEnded = new CountDownLatch(1);
        single.submit(() -> {
            started.countDown();
            try {
                never.await();
                firstTask[0] = "dokończone (nie powinno się zdarzyć)";
            } catch (InterruptedException e) {
                firstTask[0] = "przerwane przez shutdownNow()";
                Thread.currentThread().interrupt();
            } finally {
                firstEnded.countDown();
            }
        });
        for (int i = 0; i < 3; i++) {
            single.submit(() -> { });   // te czekają w kolejce za pierwszym
        }
        started.await();   // pewność, że pierwsze zadanie już działa (a 3 pozostałe są w kolejce)
        List<Runnable> notStarted = single.shutdownNow();   // shutdownNow = zamknij teraz
        show("shutdownNow() zwrócił nierozpoczętych zadań", notStarted.size());
        // WYNIK: shutdownNow() zwrócił nierozpoczętych zadań → 3
        firstEnded.await(5, TimeUnit.SECONDS);
        show("pierwsze zadanie", firstTask[0]);
        // WYNIK: pierwsze zadanie → przerwane przez shutdownNow()
        show("zakończona po shutdownNow()", single.awaitTermination(10, TimeUnit.SECONDS));
        // WYNIK: zakończona po shutdownNow() → true

        // PUŁAPKA: brak shutdown() → program nie kończy się po main() — dlaczego: wątki puli nie są demonami, a JVM
        // czeka na wszystkie wątki nie-demony. W aplikacji serwerowej pula żyje tyle co aplikacja (Spring zamyka swoje
        // pule przy zamykaniu kontekstu); w kodzie jednorazowym zamykaj w finally.
        // (Java 19+) ExecutorService jest AutoCloseable: try (var pool = Executors.newFixedThreadPool(4)) { ... }
        // — close() woła shutdown() i czeka na zakończenie. W Javie 17 trzeba robić to ręcznie, jak w shutdownAndAwait.
    }

    // =================================================================================================
    // 7. ThreadPoolExecutor — PARAMETRY I POLITYKI ODRZUCANIA
    // =================================================================================================

    /**
     * 7. Budujemy pulę ręcznie: 1 wątek podstawowy, maksymalnie 2, kolejka na 2 zadania. Zadania czekają na zatrzask,
     * więc możemy deterministycznie „zapełnić” pulę i zobaczyć każdy krok algorytmu przyjmowania zadań.
     */
    static void threadPoolExecutorParams() throws Exception {
        section("7. ThreadPoolExecutor — core, max, kolejka, odrzucanie");

        CountDownLatch release = new CountDownLatch(1);   // release = zwolnij
        Runnable blocking = () -> {                       // blocking = blokujące (czeka na zwolnienie)
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1,                                  // corePoolSize — wątki trzymane zawsze
                2,                                  // maximumPoolSize — górny limit wątków
                30, TimeUnit.SECONDS,               // keepAlive — ile żyje nadmiarowy bezczynny wątek
                new ArrayBlockingQueue<>(2),        // kolejka OGRANICZONA do 2 zadań
                new NamedThreadFactory("tpe"),
                new ThreadPoolExecutor.AbortPolicy());   // AbortPolicy = polityka przerwania: rzuć wyjątek (domyślna)
        List<String> steps = new ArrayList<>();
        try {
            pool.execute(blocking);   // 1: wątków (0) < core (1) → nowy wątek
            steps.add("zadanie 1 → wątków: " + pool.getPoolSize() + ", w kolejce: " + pool.getQueue().size());
            pool.execute(blocking);   // 2: core zajęty → do kolejki
            pool.execute(blocking);   // 3: do kolejki (teraz pełna)
            steps.add("zadania 2–3 → wątków: " + pool.getPoolSize() + ", w kolejce: " + pool.getQueue().size());
            pool.execute(blocking);   // 4: kolejka pełna, wątków (1) < max (2) → drugi wątek
            steps.add("zadanie 4 → wątków: " + pool.getPoolSize() + ", w kolejce: " + pool.getQueue().size());
            try {
                pool.execute(blocking);   // 5: kolejka pełna i wątków = max → odrzucenie
                steps.add("zadanie 5 przyjęte (nie powinno się zdarzyć)");
            } catch (RejectedExecutionException e) {
                steps.add("zadanie 5 → " + e.getClass().getSimpleName());
            }

            // CallerRunsPolicy = „wołający wykonuje”: odrzucone zadanie wykona wątek, który je zgłosił (tu main).
            // To naturalne hamowanie: zgłaszający jest zajęty pracą, więc wolniej zgłasza nowe zadania.
            pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
            String[] ranIn = new String[1];   // ran in = wykonane w
            pool.execute(() -> ranIn[0] = Thread.currentThread().getName());
            steps.add("zadanie 6 (CallerRunsPolicy) wykonał wątek: " + ranIn[0]);
        } finally {
            release.countDown();
            shutdownAndAwait(pool);
        }
        showEach("kroki", steps);
        // WYNIK: kroki (liczba elementów: 5):
        // WYNIK:    • zadanie 1 → wątków: 1, w kolejce: 0
        // WYNIK:    • zadania 2–3 → wątków: 1, w kolejce: 2
        // WYNIK:    • zadanie 4 → wątków: 2, w kolejce: 2
        // WYNIK:    • zadanie 5 → RejectedExecutionException
        // WYNIK:    • zadanie 6 (CallerRunsPolicy) wykonał wątek: main

        // Polityki odrzucania (ThreadPoolExecutor.*): AbortPolicy (wyjątek), CallerRunsPolicy (wykonuje wołający),
        // DiscardPolicy (po cichu wyrzuca nowe zadanie), DiscardOldestPolicy (wyrzuca najstarsze z kolejki).
        // PUŁAPKA: wątki ponad core powstają dopiero, gdy kolejka jest PEŁNA. Z nieograniczoną kolejką
        // (LinkedBlockingQueue bez limitu) maximumPoolSize nie ma żadnego znaczenia — dlaczego: kolejka nigdy się nie
        // zapełni, więc pula nigdy nie dojdzie do kroku „nowy wątek ponad core”.
        // PUŁAPKA: gotowe pule z Executors mają ukryte ryzyka:
        //   • newFixedThreadPool / newSingleThreadExecutor — kolejka BEZ LIMITU: przy zalewie zadań rośnie, aż
        //     zabraknie pamięci (OutOfMemoryError), a zadania czekają coraz dłużej;
        //   • newCachedThreadPool — wątków BEZ LIMITU (max = Integer.MAX_VALUE): przy zalewie powstają tysiące wątków.
        // DOBRA PRAKTYKA: w kodzie produkcyjnym twórz ThreadPoolExecutor jawnie: ograniczona kolejka, sensowne max,
        // nazwana fabryka wątków i świadomie wybrana polityka odrzucania.
    }

    // =================================================================================================
    // 8. DOBÓR ROZMIARU PULI I SPRING
    // =================================================================================================

    /**
     * 8. Ile wątków? Zależy od tego, czy zadania LICZĄ (CPU), czy CZEKAJĄ (IO). Reguła orientacyjna z książki
     * „Java Concurrency in Practice”: wątki ≈ rdzenie × (1 + czas czekania / czas liczenia).
     */
    static void sizingAndSpring() {
        section("8. Rozmiar puli i Spring");

        int cores = Runtime.getRuntime().availableProcessors();
        show("rdzeni > 0", cores > 0);
        // WYNIK: rdzeni > 0 → true
        // (liczba rdzeni zależy od maszyny — dlatego przykład liczymy na stałej wartości 8)
        int exampleCores = 8;            // example cores = przykładowa liczba rdzeni
        double waitMs = 90;              // zadanie czeka 90 ms na bazę danych…
        double computeMs = 10;           // …i liczy 10 ms
        long ioBound = Math.round(exampleCores * (1 + waitMs / computeMs));   // IO-bound = ograniczone przez IO
        show("zadania obliczeniowe (CPU) — wątków tyle, ile rdzeni", exampleCores);
        // WYNIK: zadania obliczeniowe (CPU) — wątków tyle, ile rdzeni → 8
        show("zadania czekające (90 ms IO, 10 ms CPU) — wątków około", ioBound);
        // WYNIK: zadania czekające (90 ms IO, 10 ms CPU) — wątków około → 80

        // Dlaczego: przy pracy czysto obliczeniowej więcej wątków niż rdzeni nic nie przyspiesza (tylko przełączanie).
        // Przy czekaniu na IO wątek przez większość czasu nie używa procesora, więc może ich być więcej.
        // To punkt startowy — prawdziwy rozmiar ustala się POMIAREM pod obciążeniem. Ogranicza go też zasób zewnętrzny:
        // 80 wątków nic nie da, jeśli pula połączeń do bazy ma 10 połączeń.

        // Spring (kurs SpringLearning):
        //   • Tomcat obsługuje żądania HTTP w swojej puli wątków (domyślnie do 200 wątków) — dlatego beany-singletony
        //     są używane przez wiele wątków naraz i muszą być bezpieczne wątkowo (Concurrency08ThreadSafetyPatterns).
        //   • ThreadPoolTaskExecutor — springowa nakładka na ThreadPoolExecutor (corePoolSize, maxPoolSize,
        //     queueCapacity, threadNamePrefix — dokładnie parametry z sekcji 7).
        //   • @Async na metodzie = „wykonaj w puli”: metoda zwraca od razu, a wynik oddaje jako CompletableFuture
        //     (Concurrency05CompletableFuture). @Scheduled = zadania okresowe (Concurrency09ScheduledForkJoin).
        note("pula = ograniczona liczba wątków + kolejka + polityka odrzucania");
        // WYNIK: ℹ pula = ograniczona liczba wątków + kolejka + polityka odrzucania
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Pula = wątki wielokrotnego użytku + kolejka zadań + limit współbieżności. Nazywaj wątki (ThreadFactory).
     *   • submit(Callable) → Future<V> z wynikiem; submit(Runnable) → Future<?> (get() = null); execute → brak Future.
     *   • get() blokuje; get(czas) → TimeoutException (zadanie dalej działa!); cancel(true) przerywa wątek.
     *   • isDone() = „już się nie wykonuje” (sukces, wyjątek albo anulowanie).
     *   • Wyjątek z zadania: submit → ExecutionException z get(), prawdziwy w getCause(); execute → UncaughtExceptionHandler.
     *   • invokeAll → Future w kolejności ZADAŃ; invokeAny → jeden udany wynik.
     *   • shutdown() łagodnie; shutdownNow() przerywa i zwraca nierozpoczęte; potem awaitTermination(limit).
     *   • Po shutdown() submit → RejectedExecutionException. Niezamknięta pula = JVM się nie kończy.
     *   • ThreadPoolExecutor: core → kolejka → max → odrzucenie. Kolejka bez limitu = max bez znaczenia.
     *   • newFixedThreadPool: kolejka bez limitu; newCachedThreadPool: wątki bez limitu — uważaj na zalew zadań.
     *   • Rozmiar: CPU ≈ rdzenie; IO ≈ rdzenie × (1 + czekanie/liczenie); potwierdź pomiarem.
     *
     * PYTANIA KONTROLNE:
     *   1. Podaj trzy powody, dla których pula wątków jest lepsza niż new Thread() dla każdego zadania.
     *   2. Co wypisze?
     *        Future<?> f = pool.submit(() -> { });
     *        System.out.println(f.get());
     *   3. ZNAJDŹ BŁĄD:
     *        ExecutorService pool = Executors.newFixedThreadPool(4);
     *        pool.submit(() -> wyslijRaport());
     *        // koniec main — program nie chce się zakończyć
     *   4. ZNAJDŹ BŁĄD:
     *        pool.submit(() -> { importuj(plik); });   // importuj rzuca RuntimeException; nikt nie woła get()
     *   5. Co wypisze? Pula: core 1, max 2, ArrayBlockingQueue(1). Zgłaszamy 3 zadania, które się nie kończą.
     *        System.out.println(pool.getPoolSize() + " " + pool.getQueue().size());
     *   6. Czym różni się shutdown() od shutdownNow()? Co zwraca shutdownNow()?
     *   7. W jakiej kolejności invokeAll zwraca wyniki? A ExecutorCompletionService?
     *   8. Dlaczego newCachedThreadPool i newFixedThreadPool bywają niebezpieczne na produkcji?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        List<Integer> expectedLengths = List.of(4, 6, 6, 8, 4, 4, 5, 6, 4, 8, 6, 4);
        List<String> inputs = List.of("10", "x", "30", "4,5");
        List<String> expectedParsed = List.of("OK: 10", "BŁĄD: NumberFormatException", "OK: 30", "BŁĄD: NumberFormatException");
        Check.equal("ćw. 1: suma kwadratów 1..100 przez Callable w puli", 338_350L, () -> exercise1(100));
        Check.equal("ćw. 2: długości słów przez invokeAll", expectedLengths, () -> exercise2(SampleData.words()));
        Check.equal("ćw. 3: parsowanie z obsługą ExecutionException", expectedParsed, () -> exercise3(inputs));
        Check.equal("ćw. 4: wyniki z limitem czasu", List.of("A", "TIMEOUT", "C"), Concurrency04Executors::exercise4);
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 338_350L, () -> solution1(100));
        Check.equal("ćw. 2 (wzorzec)", expectedLengths, () -> solution2(SampleData.words()));
        Check.equal("ćw. 3 (wzorzec)", expectedParsed, () -> solution3(inputs));
        Check.equal("ćw. 4 (wzorzec)", List.of("A", "TIMEOUT", "C"), Concurrency04Executors::solution4);
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): utwórz pulę 3 wątków. Dla każdego i od 1 do n zgłoś {@code Callable<Long>} liczący i².
     * Zbierz Future w liście, zsumuj wyniki {@code get()} i zwróć sumę. Zamknij pulę w finally.
     * Podpowiedź: {@code futures.add(pool.submit(() -> (long) x * x));} — x musi być efektywnie finalne.
     */
    static long exercise1(int n) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): dla każdego słowa zbuduj {@code Callable<Integer>} zwracający jego długość, wykonaj
     * wszystkie przez {@code invokeAll} i zwróć długości w kolejności słów.
     * Podpowiedź: listę zadań zbudujesz strumieniem: {@code words.stream().map(w -> (Callable<Integer>) w::length).toList()}
     * (rzutowanie podpowiada kompilatorowi typ lambdy).
     */
    static List<Integer> exercise2(List<String> words) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): dla każdego tekstu zgłoś zadanie {@code Integer.parseInt(tekst)}. Odbieraj wyniki w kolejności
     * zgłoszenia: udane jako {@code "OK: " + liczba}, a nieudane jako {@code "BŁĄD: " + prosta nazwa klasy przyczyny}.
     * Podpowiedź: złap {@code ExecutionException} i użyj {@code e.getCause().getClass().getSimpleName()}.
     */
    static List<String> exercise3(List<String> texts) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): PRZEPISZ kod, który czeka bez końca:
     * <pre>{@code
     * for (Future<String> f : futures) results.add(f.get());   // jedno zawieszone zadanie = zawieszony program
     * }</pre>
     * Zgłoś do puli 3 zadania: pierwsze zwraca "A", drugie czeka na {@code CountDownLatch}, którego nikt nie zwolni,
     * trzecie zwraca "C". Odbieraj wyniki przez {@code get(2, TimeUnit.SECONDS)}; przy {@code TimeoutException} dodaj
     * "TIMEOUT" i anuluj zadanie {@code cancel(true)}. Zwróć listę wyników. Pula musi się poprawnie zamknąć.
     * Podpowiedź: bez cancel(true) zawieszone zadanie blokowałoby zamknięcie puli (awaitTermination czekałby na nie).
     */
    static List<String> exercise4() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static long solution1(int n) {
        ExecutorService pool = Executors.newFixedThreadPool(3, new NamedThreadFactory("kwadraty"));
        try {
            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 1; i <= n; i++) {
                int x = i;
                futures.add(pool.submit(() -> (long) x * x));
            }
            long sum = 0;
            for (Future<Long> f : futures) {
                sum += f.get();
            }
            return sum;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        } finally {
            shutdownAndAwait(pool);
        }
    }

    static List<Integer> solution2(List<String> words) {
        ExecutorService pool = Executors.newFixedThreadPool(3, new NamedThreadFactory("słowa"));
        try {
            List<Callable<Integer>> tasks = words.stream().map(w -> (Callable<Integer>) w::length).toList();
            List<Integer> lengths = new ArrayList<>();
            for (Future<Integer> f : pool.invokeAll(tasks)) {
                lengths.add(f.get());
            }
            return lengths;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        } finally {
            shutdownAndAwait(pool);
        }
    }

    static List<String> solution3(List<String> texts) {
        ExecutorService pool = Executors.newFixedThreadPool(2, new NamedThreadFactory("parser"));
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (String t : texts) {
                futures.add(pool.submit(() -> Integer.parseInt(t)));
            }
            List<String> out = new ArrayList<>();
            for (Future<Integer> f : futures) {
                try {
                    out.add("OK: " + f.get());
                } catch (ExecutionException e) {
                    out.add("BŁĄD: " + e.getCause().getClass().getSimpleName());
                }
            }
            return out;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } finally {
            shutdownAndAwait(pool);
        }
    }

    static List<String> solution4() {
        ExecutorService pool = Executors.newFixedThreadPool(3, new NamedThreadFactory("limit"));
        CountDownLatch never = new CountDownLatch(1);
        try {
            List<Future<String>> futures = List.of(
                    pool.submit(() -> "A"),
                    pool.submit(() -> {
                        never.await();
                        return "B";
                    }),
                    pool.submit(() -> "C"));
            List<String> results = new ArrayList<>();
            for (Future<String> f : futures) {
                try {
                    results.add(f.get(2, TimeUnit.SECONDS));
                } catch (TimeoutException e) {
                    results.add("TIMEOUT");
                    f.cancel(true);   // przerywa await() w zadaniu — wątek puli się zwalnia
                } catch (ExecutionException e) {
                    results.add("BŁĄD: " + e.getCause().getClass().getSimpleName());
                }
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } finally {
            shutdownAndAwait(pool);
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Brak kosztu tworzenia wątku dla każdego zadania (wątki wielokrotnego użytku); limit współbieżności
     *      (nadmiar zadań czeka w kolejce zamiast tworzyć tysiące wątków); zarządzanie (Future z wynikiem i wyjątkiem,
     *      anulowanie, kontrolowane zamknięcie).
     *   2. null — Future z zadania Runnable nie niesie wyniku.
     *   3. Brak shutdown(): wątki puli nie są demonami, więc JVM czeka na nie w nieskończoność. Poprawka:
     *      try { ... } finally { pool.shutdown(); pool.awaitTermination(...); }
     *   4. Wyjątek zostaje schowany w Future, którego nikt nie odbiera — błąd przepada bez śladu. Poprawka: odebrać
     *      Future.get() i obsłużyć ExecutionException albo złapać i zalogować wyjątek wewnątrz zadania.
     *   5. „2 1” — zadanie 1 dostaje wątek podstawowy, zadanie 2 idzie do kolejki (pełna), zadanie 3 tworzy drugi
     *      wątek (do max = 2).
     *   6. shutdown() przestaje przyjmować nowe zadania, ale dokańcza zgłoszone. shutdownNow() dodatkowo przerywa
     *      (interrupt) działające zadania i zwraca listę zadań, które nie zdążyły wystartować.
     *   7. invokeAll — w kolejności zadań na liście wejściowej (nie zakończenia). ExecutorCompletionService — w kolejności
     *      zakończenia (take() zwraca najpierw te, które skończyły się pierwsze).
     *   8. newFixedThreadPool ma kolejkę bez limitu — przy zalewie zadań rośnie aż do OutOfMemoryError.
     *      newCachedThreadPool tworzy wątki bez limitu — przy zalewie powstają tysiące wątków (pamięć, przełączanie).
     */
    // </editor-fold>
}
