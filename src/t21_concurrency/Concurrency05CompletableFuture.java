package t21_concurrency;

import helpers.Check;
import helpers.SampleData;
import helpers.Sleep;
import helpers.model.Product;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: CompletableFuture — łańcuchy zadań asynchronicznych
 *        (completable future = przyszły wynik, który można ukończyć; async = asynchroniczny;
 *         supply = dostarcz; compose = złóż; combine = połącz)
 *
 * W SKRÓCIE:
 *   Future z poprzedniej lekcji umie tylko jedno: zablokować wątek w get(). CompletableFuture pozwala OPISAĆ, co ma
 *   się stać z wynikiem, gdy będzie gotowy: przekształć go, połącz z innym wynikiem, obsłuż błąd, ustaw limit czasu.
 *   Wątek główny nie czeka na każdy krok — czeka raz, na końcu (albo wcale, jak w serwerze).
 *
 * ANALOGIA:
 *   Zamawiasz meble w sklepie internetowym. Zamiast stać pod drzwiami (get()), zostawiasz instrukcje: „gdy przyjdzie
 *   paczka (thenApply) — wnieś ją; gdy przyjdą OBIE paczki, stół i krzesła (thenCombine) — złóż komplet; gdy kurier
 *   zgubi paczkę (exceptionally) — weź zwrot pieniędzy; jeśli nie dotrze w tydzień (orTimeout) — reklamacja”.
 *
 * JAK TO DZIAŁA:
 *   • Start:   CompletableFuture.supplyAsync(() -> pobierzCene(), pula)   // zadanie z wynikiem w puli
 *              CompletableFuture.runAsync(() -> zapiszLog(), pula)        // zadanie bez wyniku
 *   • Kroki (każdy zwraca NOWY CompletableFuture):
 *       thenApply(f)      T → U                 (jak map w strumieniach)
 *       thenAccept(c)     T → nic               (konsumuje wynik)
 *       thenRun(r)        nic → nic             (po prostu „potem zrób”)
 *       thenCompose(f)    T → CompletableFuture<U>, spłaszczone   (jak flatMap)
 *       thenCombine(o, f) (T, U) → V            (dwa NIEZALEŻNE wyniki razem)
 *       exceptionally(f)  błąd → wartość zastępcza;  handle(f) (wynik, błąd) → U;  whenComplete(c) — tylko podgląd
 *   • Wiele naraz: allOf(...) — gdy wszystkie skończą; anyOf(...) — gdy którykolwiek skończy.
 *   • Odbiór: join() — wyjątek niesprawdzany CompletionException; get() — sprawdzany ExecutionException.
 *   • Wariant *Async (thenApplyAsync itp.) zleca krok puli; wariant zwykły wykonuje go „gdzie się da” — w wątku,
 *     który ukończył poprzedni etap, albo w wątku, który dopiął krok do już ukończonego etapu.
 *
 * SŁÓWKA:
 *   complete = ukończ; supply = dostarcz; apply = zastosuj; accept = przyjmij; compose = złóż; combine = połącz;
 *   all of = wszystkie z; any of = którykolwiek z; exceptionally = w razie wyjątku; handle = obsłuż;
 *   when complete = gdy ukończony; join = dołącz (poczekaj i weź wynik); timeout = limit czasu; fallback = plan
 *   awaryjny (wartość zastępcza); pipeline = potok (łańcuch kroków); common pool = wspólna pula.
 *
 * ZOBACZ TEŻ: t21_concurrency/Concurrency04Executors (pule i Future — podstawa tej lekcji),
 *             t21_concurrency/Concurrency09ScheduledForkJoin (ForkJoinPool.commonPool),
 *             t14_optional/Optional01Basics (map/flatMap — ta sama idea co thenApply/thenCompose),
 *             t16_streams/Streams18Parallel (wspólna pula używana też przez strumienie równoległe).
 * </pre>
 */
public class Concurrency05CompletableFuture {

    public static void main(String[] args) {
        title("Concurrency05 — CompletableFuture: łańcuchy, łączenie, błędy, limity czasu");

        ExecutorService pool = Executors.newFixedThreadPool(4, new NamedThreadFactory("cf"));
        try {
            startingTasks(pool);          // starting tasks = uruchamianie zadań
            simpleSteps(pool);            // simple steps = proste kroki
            composeVersusApply(pool);     // compose versus apply = thenCompose kontra thenApply
            combiningTwo(pool);           // combining two = łączenie dwóch wyników
            allOfAnyOf(pool);             // all of / any of = wszystkie / którykolwiek
            handlingErrors(pool);         // handling errors = obsługa błędów
            timeouts(pool);               // timeouts = limity czasu
            joinVersusGet(pool);          // join versus get = join kontra get
            asyncVariants(pool);          // async variants = warianty Async
            realisticPipeline(pool);      // realistic pipeline = realistyczny potok
            exercises(pool);              // exercises = ćwiczenia
        } finally {
            shutdownAndAwait(pool);       // pulę zamykamy ZAWSZE — inaczej JVM się nie zakończy
        }
    }

    // =================================================================================================
    // 1. URUCHAMIANIE: supplyAsync, runAsync, ręczne complete()
    // =================================================================================================

    /**
     * 1. {@code supplyAsync(dostawca, pula)} uruchamia zadanie z wynikiem w podanej puli i od razu zwraca
     * CompletableFuture. Zawsze podawaj WŁASNĄ pulę (powód w PUŁAPCE poniżej).
     */
    static void startingTasks(ExecutorService pool) {
        section("1. supplyAsync, runAsync i ręczne complete()");

        CompletableFuture<Integer> answer = CompletableFuture.supplyAsync(() -> 6 * 7, pool);   // supplyAsync = dostarcz asynchronicznie
        show("supplyAsync(...).join()", answer.join());   // join = poczekaj i weź wynik
        // WYNIK: supplyAsync(...).join() → 42

        AtomicInteger saved = new AtomicInteger();
        CompletableFuture<Void> log = CompletableFuture.runAsync(saved::incrementAndGet, pool);   // runAsync = uruchom asynchronicznie (bez wyniku)
        log.join();
        show("runAsync — efekt widoczny po join()", saved.get());
        // WYNIK: runAsync — efekt widoczny po join() → 1

        // Gotowy wynik od razu — przydatne w testach i jako wartość zastępcza:
        show("completedFuture(\"gotowe\").join()", CompletableFuture.completedFuture("gotowe").join());   // completedFuture = ukończony przyszły wynik
        // WYNIK: completedFuture("gotowe").join() → gotowe

        // „Completable” = MOŻNA go ukończyć ręcznie. Tak łączy się stare API z wywołaniami zwrotnymi z CompletableFuture.
        CompletableFuture<String> manual = new CompletableFuture<>();   // manual = ręczny
        Thread callback = new Thread(() -> manual.complete("odpowiedź z serwera"), "wywołanie-zwrotne");   // complete = ukończ
        callback.start();
        show("ukończony ręcznie z innego wątku", manual.join());
        // WYNIK: ukończony ręcznie z innego wątku → odpowiedź z serwera
        show("drugie complete() zwraca", manual.complete("spóźniona odpowiedź"));
        // WYNIK: drugie complete() zwraca → false
        show("wynik się nie zmienił", manual.join());
        // WYNIK: wynik się nie zmienił → odpowiedź z serwera

        // PUŁAPKA: supplyAsync(zadanie) BEZ puli używa ForkJoinPool.commonPool() — wspólnej puli całej JVM (tej samej, której
        // używają strumienie równoległe). Dlaczego to źle przy zadaniach czekających na IO: wspólna pula ma zwykle tyle
        // wątków, ile rdzeni minus jeden; kilka zadań czekających na sieć zajmie je wszystkie i zablokuje inne części
        // programu. (Gdy rdzeni jest bardzo mało, JDK zamiast puli tworzy nowy wątek na KAŻDE zadanie.)
        // Wątki wspólnej puli są demonami — niedokończone zadania przepadają przy końcu JVM.
        // DOBRA PRAKTYKA: zawsze supplyAsync(zadanie, własnaPula) — z nazwanymi wątkami i rozmiarem dobranym do zadań.
    }

    // =================================================================================================
    // 2. PROSTE KROKI: thenApply, thenAccept, thenRun
    // =================================================================================================

    /**
     * 2. Kroki łańcucha opisują, co zrobić z wynikiem, gdy będzie gotowy. Każdy krok zwraca NOWY CompletableFuture —
     * jak operacje pośrednie w strumieniach.
     */
    static void simpleSteps(ExecutorService pool) {
        section("2. thenApply, thenAccept, thenRun");

        List<String> events = new ArrayList<>();   // events = zdarzenia (pisze łańcuch, czyta main po join)
        CompletableFuture<Void> chain = CompletableFuture
                .supplyAsync(() -> "  czysty kod  ", pool)
                .thenApply(String::strip)                    // thenApply = zastosuj funkcję (T → U); strip (Java 11+) = przytnij
                .thenApply(String::toUpperCase)              // toUpperCase = na wielkie litery
                .thenAccept(title -> events.add("tytuł: " + title))   // thenAccept = przyjmij wynik (T → nic)
                .thenRun(() -> events.add("zapisano do logu"));       // thenRun = potem uruchom (nic → nic)
        chain.join();
        showEach("zdarzenia", events);
        // WYNIK: zdarzenia (liczba elementów: 2):
        // WYNIK:    • tytuł: CZYSTY KOD
        // WYNIK:    • zapisano do logu
        // Kroki jednego łańcucha wykonują się PO KOLEI (każdy czeka na poprzedni), więc lista „events” nie wymaga
        // synchronizacji: ukończenie etapu daje happens-before dla kroku zależnego, a join() — dla main.

        // PUŁAPKA: wynik kroku trzeba ZACHOWAĆ — dlaczego: CompletableFuture jest jak niezmienny łańcuch; thenApply
        // nie zmienia starego obiektu, tylko zwraca nowy.
        CompletableFuture<Integer> base = CompletableFuture.completedFuture(10);   // base = baza
        base.thenApply(x -> x * 2);   // wynik wyrzucony!
        show("base.join() po zignorowanym thenApply", base.join());
        // WYNIK: base.join() po zignorowanym thenApply → 10
    }

    // =================================================================================================
    // 3. thenCompose KONTRA thenApply
    // =================================================================================================

    /** Udawany serwis: znajdź klienta po id — asynchronicznie. */
    static CompletableFuture<String> findCustomerName(long id, Executor pool) {   // find customer name = znajdź nazwę klienta
        return CompletableFuture.supplyAsync(() -> SampleData.customers().stream()
                .filter(c -> c.id() == id)
                .findFirst()
                .orElseThrow()      // orElseThrow (Java 10+) = albo rzuć NoSuchElementException
                .name(), pool);
    }

    /** Udawany serwis: policz zamówienia klienta o danej nazwie — też asynchronicznie. */
    static CompletableFuture<Long> countOrders(String customerName, Executor pool) {   // count orders = policz zamówienia
        return CompletableFuture.supplyAsync(() -> SampleData.orders().stream()
                .filter(o -> o.customer().name().equals(customerName))
                .count(), pool);
    }

    /**
     * 3. Gdy krok sam zwraca CompletableFuture (bo woła kolejny serwis asynchroniczny), {@code thenApply} dałby
     * zagnieżdżone pudełko {@code CompletableFuture<CompletableFuture<Long>>}. {@code thenCompose} je spłaszcza —
     * dokładnie jak {@code flatMap} w Optional i strumieniach.
     */
    static void composeVersusApply(ExecutorService pool) {
        section("3. thenCompose (flatMap) kontra thenApply (map)");

        // thenApply z funkcją zwracającą CompletableFuture → pudełko w pudełku:
        CompletableFuture<CompletableFuture<Long>> nested =
                findCustomerName(1, pool).thenApply(name -> countOrders(name, pool));
        show("thenApply → trzeba dwa razy join()", nested.join().join());
        // WYNIK: thenApply → trzeba dwa razy join() → 3

        // thenCompose = złóż: drugi krok zależy od wyniku pierwszego, a wynik jest płaski.
        CompletableFuture<Long> flat = findCustomerName(1, pool).thenCompose(name -> countOrders(name, pool));
        show("thenCompose → jeden join()", flat.join());
        // WYNIK: thenCompose → jeden join() → 3

        CompletableFuture<String> summary = findCustomerName(4, pool)
                .thenCompose(name -> countOrders(name, pool).thenApply(n -> name + ": " + n + " zamówienia"));
        show("łańcuch zależnych wywołań", summary.join());
        // WYNIK: łańcuch zależnych wywołań → Zofia Krawczyk: 2 zamówienia

        // Zasada: funkcja zwraca zwykłą wartość → thenApply; funkcja zwraca CompletableFuture → thenCompose.
        // DOBRA PRAKTYKA: thenCompose wyraża ZALEŻNOŚĆ („najpierw klient, potem jego zamówienia”) — kroki muszą iść
        // po kolei. Niezależne wywołania (cena i stan magazynu) uruchamiaj równolegle i łącz przez thenCombine (sekcja 4).
    }

    // =================================================================================================
    // 4. thenCombine — DWA NIEZALEŻNE WYNIKI
    // =================================================================================================

    /**
     * 4. {@code a.thenCombine(b, (x, y) -> ...)} czeka na OBA wyniki i łączy je funkcją. Oba zadania biegną równolegle.
     */
    static void combiningTwo(ExecutorService pool) {
        section("4. thenCombine — łączenie dwóch niezależnych wyników");

        Product book = SampleData.productBySku("KSI-002");
        CompletableFuture<BigDecimal> price = CompletableFuture.supplyAsync(book::price, pool);   // price = cena
        CompletableFuture<Integer> stock = CompletableFuture.supplyAsync(book::stock, pool);      // stock = stan magazynu
        CompletableFuture<BigDecimal> value = price.thenCombine(stock,                            // thenCombine = połącz z
                (p, s) -> p.multiply(BigDecimal.valueOf(s)));
        show("wartość magazynu „Java. Podstawy” (cena × stan)", value.join());
        // WYNIK: wartość magazynu „Java. Podstawy” (cena × stan) → 1161.00

        // thenAcceptBoth = przyjmij oba (bez wyniku), runAfterBoth = uruchom po obu — rzadziej potrzebne.
        List<String> line = new ArrayList<>();
        price.thenAcceptBoth(stock, (p, s) -> line.add(p + " zł × " + s + " szt.")).join();
        show("thenAcceptBoth", line.get(0));
        // WYNIK: thenAcceptBoth → 129.00 zł × 9 szt.
    }

    // =================================================================================================
    // 5. allOf I anyOf
    // =================================================================================================

    /**
     * 5. {@code allOf} zwraca {@code CompletableFuture<Void>} — sam NIE niesie wyników. Wyniki zbieramy z listy
     * pierwotnych CompletableFuture, w kolejności tej listy (nie zakończenia).
     */
    static void allOfAnyOf(ExecutorService pool) {
        section("5. allOf (wszystkie) i anyOf (którykolwiek)");

        List<String> skus = List.of("ELE-003", "SPO-001", "KSI-001", "DOM-002");
        List<CompletableFuture<String>> lookups = skus.stream()            // lookups = wyszukiwania
                .map(sku -> CompletableFuture.supplyAsync(() -> SampleData.productBySku(sku).name(), pool))
                .toList();
        CompletableFuture<Void> all = CompletableFuture.allOf(lookups.toArray(new CompletableFuture[0]));   // allOf = wszystkie z
        // Gdy „all” jest ukończony, każdy z lookups też jest — join() już nie blokuje.
        List<String> names = all.thenApply(ignored -> lookups.stream().map(CompletableFuture::join).toList()).join();
        show("allOf — nazwy w kolejności SKU", names);
        // WYNIK: allOf — nazwy w kolejności SKU → [Słuchawki BT, Kawa ziarnista 1kg, Czysty kod, Lampka biurkowa]

        // anyOf = którykolwiek z: wynik PIERWSZEGO ukończonego (typ Object!). Żeby wynik był pewny, dwa zadania czekają
        // na zatrzask, który zwolnimy dopiero po odczycie — więc „B” na pewno skończy pierwsze.
        CountDownLatch hold = new CountDownLatch(1);   // hold = wstrzymaj
        CompletableFuture<String> slowA = CompletableFuture.supplyAsync(() -> { awaitQuietly(hold); return "serwer A"; }, pool);
        CompletableFuture<String> fastB = CompletableFuture.supplyAsync(() -> "serwer B", pool);
        CompletableFuture<String> slowC = CompletableFuture.supplyAsync(() -> { awaitQuietly(hold); return "serwer C"; }, pool);
        Object first = CompletableFuture.anyOf(slowA, fastB, slowC).join();   // anyOf = którykolwiek z
        show("anyOf — pierwszy ukończony", first);
        // WYNIK: anyOf — pierwszy ukończony → serwer B
        hold.countDown();                       // zwalniamy pozostałe, żeby nie zajmowały wątków puli
        CompletableFuture.allOf(slowA, slowC).join();

        // PUŁAPKA: anyOf NIE anuluje pozostałych zadań — dalej zajmują wątki puli. Dlaczego: CompletableFuture nie
        // przerywa wątków (cancel(true) w CompletableFuture NIE wywołuje interrupt — inaczej niż w FutureTask z puli).
        // PUŁAPKA: anyOf zwraca CompletableFuture<Object> — trzeba rzutować; gdy pierwszy ukończy się WYJĄTKIEM,
        // anyOf też kończy się wyjątkiem (pierwszy ukończony, nie pierwszy udany — inaczej niż invokeAny).
    }

    // =================================================================================================
    // 6. BŁĘDY: exceptionally, handle, whenComplete, CompletionException
    // =================================================================================================

    /**
     * 6. Wyjątek w kroku „płynie” łańcuchem: kolejne thenApply są pomijane, aż trafi na krok obsługi błędu.
     * Po drodze bywa opakowany w {@code CompletionException} — prawdziwy wyjątek jest w {@code getCause()}.
     */
    static void handlingErrors(ExecutorService pool) {
        section("6. exceptionally, handle, whenComplete — obsługa błędów");

        List<String> log = new ArrayList<>();
        CompletableFuture<Integer> failing = CompletableFuture.supplyAsync(() -> Integer.parseInt("sto"), pool);

        // exceptionally = w razie wyjątku: zamień błąd na wartość zastępczą. Na sukces nie reaguje.
        CompletableFuture<Integer> recovered = failing
                .thenApply(x -> x * 2)                          // POMINIĘTE — poprzedni etap zawiódł
                .exceptionally(ex -> {
                    log.add("exceptionally dostał: " + ex.getClass().getSimpleName()
                            + " ← przyczyna: " + unwrap(ex).getClass().getSimpleName());
                    return 0;                                   // fallback = wartość zastępcza
                });
        show("wynik po exceptionally", recovered.join());
        // WYNIK: wynik po exceptionally → 0
        show("log", log.get(0));
        // WYNIK: log → exceptionally dostał: CompletionException ← przyczyna: NumberFormatException
        // Wyjątek z zadania (NumberFormatException) dotarł OPAKOWANY w CompletionException. Dlatego w obsłudze błędu
        // najpierw „rozpakuj” wyjątek (metoda unwrap niżej), dopiero potem sprawdzaj typ.

        // handle = obsłuż: dostaje (wynik, wyjątek) — dokładnie jedno z nich jest różne od null… prawie: wynik
        // poprawny może też być null. Zwraca nową wartość w obu przypadkach.
        CompletableFuture<String> handledOk = CompletableFuture.supplyAsync(() -> Integer.parseInt("12"), pool)
                .handle((value, ex) -> ex == null ? "OK: " + value : "BŁĄD: " + unwrap(ex).getClass().getSimpleName());
        CompletableFuture<String> handledBad = failing
                .handle((value, ex) -> ex == null ? "OK: " + value : "BŁĄD: " + unwrap(ex).getClass().getSimpleName());
        show("handle na sukcesie", handledOk.join());
        // WYNIK: handle na sukcesie → OK: 12
        show("handle na błędzie", handledBad.join());
        // WYNIK: handle na błędzie → BŁĄD: NumberFormatException

        // whenComplete = gdy ukończony: tylko PODGLĄDA (np. logowanie, metryki); wynik i błąd przechodzą dalej bez zmian.
        List<String> audit = new ArrayList<>();   // audit = dziennik kontrolny
        CompletableFuture<Integer> observed = failing.whenComplete((value, ex) ->
                audit.add(ex == null ? "sukces" : "porażka: " + unwrap(ex).getMessage()));
        expectThrows("join() po whenComplete — błąd nadal jest", observed::join);
        // WYNIK: ✔ join() po whenComplete — błąd nadal jest → rzucono CompletionException: java.lang.NumberFormatException: For input string: "sto"
        show("whenComplete zanotował", audit.get(0));
        // WYNIK: whenComplete zanotował → porażka: For input string: "sto"

        // Wyjątek wewnątrz kroku obsługi też jest wyjątkiem łańcucha:
        CompletableFuture<Integer> doubleFailure = failing.exceptionally(ex -> {   // double failure = podwójna porażka
            throw new IllegalStateException("fallback też zawiódł");
        });
        String outcome;
        try {
            doubleFailure.join();
            outcome = "sukces";
        } catch (CompletionException e) {
            outcome = unwrap(e).getClass().getSimpleName() + ": " + unwrap(e).getMessage();
        }
        show("błąd w exceptionally", outcome);
        // WYNIK: błąd w exceptionally → IllegalStateException: fallback też zawiódł

        // PUŁAPKA: łańcuch bez exceptionally/handle i bez join() = błąd przepada bez śladu (nic się nie wypisze).
        // Dlaczego: wyjątek jest przechowywany w CompletableFuture, aż ktoś go odbierze — jak w Future z submit().
        // DOBRA PRAKTYKA: każdy łańcuch kończ obsługą błędu (exceptionally/handle) albo oddaj go komuś, kto ją zrobi
        // (np. Spring MVC sam obsłuży CompletableFuture zwrócony z kontrolera).
    }

    /** Zdejmuje opakowania CompletionException/ExecutionException i zwraca prawdziwą przyczynę. */
    static Throwable unwrap(Throwable ex) {   // unwrap = rozpakuj
        Throwable current = ex;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    // =================================================================================================
    // 7. LIMITY CZASU: orTimeout, completeOnTimeout (Java 9+)
    // =================================================================================================

    /**
     * 7. {@code orTimeout(czas, jednostka)} kończy CompletableFuture wyjątkiem {@code TimeoutException}, jeśli nie
     * zdąży. {@code completeOnTimeout(wartość, czas, jednostka)} — zamiast wyjątku wstawia wartość zastępczą.
     */
    static void timeouts(ExecutorService pool) {
        section("7. orTimeout i completeOnTimeout (Java 9+)");

        // Ten CompletableFuture NIGDY nie zostanie ukończony (nikt nie wywoła complete) — limit MUSI upłynąć.
        CompletableFuture<String> neverDone = new CompletableFuture<>();   // never done = nigdy nieukończony
        CompletableFuture<String> withTimeout = neverDone.orTimeout(100, TimeUnit.MILLISECONDS);   // orTimeout = albo limit czasu
        String outcome;
        try {
            withTimeout.join();
            outcome = "zdążył";
        } catch (CompletionException e) {
            outcome = e.getClass().getSimpleName() + " ← przyczyna: " + unwrap(e).getClass().getSimpleName();
        }
        show("orTimeout(100 ms)", outcome);
        // WYNIK: orTimeout(100 ms) → CompletionException ← przyczyna: TimeoutException
        show("orTimeout zwraca TEN SAM obiekt", withTimeout == neverDone);
        // WYNIK: orTimeout zwraca TEN SAM obiekt → true

        CompletableFuture<String> slow = new CompletableFuture<>();
        String withDefault = slow.completeOnTimeout("cena z pamięci podręcznej", 100, TimeUnit.MILLISECONDS)   // completeOnTimeout = ukończ po limicie
                .join();
        show("completeOnTimeout(100 ms)", withDefault);
        // WYNIK: completeOnTimeout(100 ms) → cena z pamięci podręcznej

        // Zadanie, które zdąży (limit hojny: 5 s), nie jest zmieniane przez limit:
        String fast = CompletableFuture.supplyAsync(() -> "szybka odpowiedź", pool)
                .orTimeout(5, TimeUnit.SECONDS)
                .join();
        show("zdążył przed limitem", fast);
        // WYNIK: zdążył przed limitem → szybka odpowiedź

        // PUŁAPKA: orTimeout/completeOnTimeout NIE przerywają zadania, które liczy wynik — dlaczego: kończą tylko
        // CompletableFuture (jak complete z zewnątrz); wątek puli liczy dalej i jego wynik zostanie zignorowany.
        // Jeśli zadanie trzyma drogi zasób (połączenie), trzeba je przerwać inaczej (np. limit w samym kliencie HTTP).
        // DOBRA PRAKTYKA: każde wywołanie usługi zewnętrznej ma limit czasu — dlaczego: bez niego jedna zawieszona
        // usługa blokuje wątki aż do wyczerpania puli, a wtedy staje cała aplikacja.
    }

    // =================================================================================================
    // 8. join() KONTRA get(), getNow()
    // =================================================================================================

    /**
     * 8. {@code get()} pochodzi z interfejsu Future: rzuca wyjątki SPRAWDZANE (InterruptedException, ExecutionException).
     * {@code join()} rzuca niesprawdzany {@code CompletionException} — wygodny w lambdach i strumieniach.
     */
    static void joinVersusGet(ExecutorService pool) {
        section("8. join() kontra get(), getNow()");

        CompletableFuture<Integer> broken = CompletableFuture.supplyAsync(() -> {   // broken = zepsuty
            throw new IllegalArgumentException("ujemna ilość");
        }, pool);

        String viaGet;
        try {
            broken.get();   // wymaga obsługi InterruptedException i ExecutionException
            viaGet = "sukces";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            viaGet = "przerwano";
        } catch (ExecutionException e) {
            viaGet = e.getClass().getSimpleName() + " ← " + e.getCause().getClass().getSimpleName();
        }
        show("get() rzuca", viaGet);
        // WYNIK: get() rzuca → ExecutionException ← IllegalArgumentException

        String viaJoin;
        try {
            broken.join();  // bez obowiązkowego try/catch — wyjątek niesprawdzany
            viaJoin = "sukces";
        } catch (CompletionException e) {
            viaJoin = e.getClass().getSimpleName() + " ← " + e.getCause().getClass().getSimpleName();
        }
        show("join() rzuca", viaJoin);
        // WYNIK: join() rzuca → CompletionException ← IllegalArgumentException

        // getNow(wartość) = pobierz teraz: wynik, jeśli gotowy; w przeciwnym razie podana wartość. Nigdy nie blokuje.
        CompletableFuture<String> notYet = new CompletableFuture<>();   // not yet = jeszcze nie
        show("getNow na nieukończonym", notYet.getNow("jeszcze liczę…"));
        // WYNIK: getNow na nieukończonym → jeszcze liczę…
        notYet.complete("gotowe");
        show("getNow na ukończonym", notYet.getNow("jeszcze liczę…"));
        // WYNIK: getNow na ukończonym → gotowe
        show("isCompletedExceptionally() zepsutego", broken.isCompletedExceptionally());   // czy ukończony wyjątkiem
        // WYNIK: isCompletedExceptionally() zepsutego → true

        // DOBRA PRAKTYKA: w kodzie produkcyjnym blokuj (join/get) tylko na samym końcu — w main, w teście albo tam, gdzie
        // framework wymaga wartości. Dlaczego: blokowanie w środku łańcucha zajmuje wątek puli, który mógłby robić
        // coś pożytecznego; w skrajnym przypadku wszystkie wątki puli czekają na zadania, które nie mają już gdzie się
        // wykonać — zakleszczenie puli.
    }

    // =================================================================================================
    // 9. WARIANTY *Async — KTO WYKONUJE KROK?
    // =================================================================================================

    /**
     * 9. {@code thenApplyAsync(f, pula)} — krok na pewno w podanej puli. {@code thenApply(f)} — w wątku, który akurat
     * ukończył poprzedni etap, ALBO w wątku, który dopina krok do już ukończonego etapu. Który to będzie — nie jest
     * gwarantowane, więc nigdy na tym nie polegaj (i nie wypisujemy tego).
     */
    static void asyncVariants(ExecutorService pool) {
        section("9. Warianty *Async — który wątek wykonuje krok");

        CompletableFuture<Boolean> inPool = CompletableFuture.completedFuture("dane")
                .thenApplyAsync(s -> Thread.currentThread().getName().startsWith("cf-"), pool);   // thenApplyAsync = zastosuj asynchronicznie
        show("thenApplyAsync(f, pula) — krok w wątku naszej puli", inPool.join());
        // WYNIK: thenApplyAsync(f, pula) — krok w wątku naszej puli → true

        // Dla thenApply bez Async dokumentacja mówi tylko: krok może wykonać wątek, który ukończył poprzedni etap, albo
        // dowolny inny wątek wołający metodę łańcucha. W praktyce (implementacja JDK): gdy poprzedni etap jest JUŻ
        // ukończony, krok wykona od razu wątek wołający thenApply (np. main); gdy jeszcze trwa — wątek, który go ukończy.
        // (wynik zależy od uruchomienia) — w ogólnym przypadku nie wiesz, który wątek to będzie.
        // PUŁAPKA: ciężki lub blokujący krok w thenApply może wykonać się w wątku, który miał robić coś innego (np. w wątku
        // klienta HTTP odbierającego odpowiedzi, albo w main). Dlaczego to groźne: blokujesz cudzy wątek.
        // DOBRA PRAKTYKA: kroki lekkie (przekształcenie danych) — thenApply; kroki ciężkie lub blokujące — thenApplyAsync
        // z WŁASNĄ pulą. Wariant thenApplyAsync(f) bez puli znowu oznacza wspólną pulę ForkJoinPool.
        note("krok lekki → thenApply; ciężki/blokujący → thenApplyAsync(f, własnaPula)");
        // WYNIK: ℹ krok lekki → thenApply; ciężki/blokujący → thenApplyAsync(f, własnaPula)
    }

    // =================================================================================================
    // 10. REALISTYCZNY POTOK: PRZED (SYNCHRONICZNIE) I PO (ASYNCHRONICZNIE)
    // =================================================================================================

    /** Udawana usługa cen: krótka przerwa = „czas odpowiedzi sieci”. */
    static BigDecimal priceService(String sku) {   // price service = usługa cen
        Sleep.ms(40);
        return SampleData.productBySku(sku).price();
    }

    /** Udawana usługa magazynu — dla SKU zaczynającego się od „SPO” jest „niedostępna”. */
    static int stockService(String sku) {          // stock service = usługa magazynu
        Sleep.ms(40);
        if (sku.startsWith("SPO")) {
            throw new IllegalStateException("magazyn spożywczy nie odpowiada");
        }
        return SampleData.productBySku(sku).stock();
    }

    /**
     * 10. Strona produktu potrzebuje ceny i stanu magazynu z dwóch usług. PRZED: wołamy je po kolei (czas = suma).
     * PO: równolegle (czas ≈ dłuższa z dwóch), z planem awaryjnym, gdy usługa magazynu zawiedzie.
     */
    static void realisticPipeline(ExecutorService pool) {
        section("10. Realistyczny potok: cena + magazyn, równolegle, z planem awaryjnym");

        // PRZED: synchronicznie — jedno po drugim; wyjątek z magazynu wysadza całą stronę.
        String sku = "ELE-004";
        BigDecimal p = priceService(sku);
        int s = stockService(sku);
        show("PRZED (po kolei)", p + " zł, " + s + " szt.");
        // WYNIK: PRZED (po kolei) → 1299.00 zł, 4 szt.

        // PO: obie usługi naraz; awaria magazynu → wartość zastępcza zamiast błędu całej strony.
        List<String> skus = List.of("ELE-004", "SPO-001", "KSI-003");
        List<CompletableFuture<String>> pages = new ArrayList<>();   // pages = strony produktów
        for (String code : skus) {
            CompletableFuture<BigDecimal> price = CompletableFuture.supplyAsync(() -> priceService(code), pool);
            CompletableFuture<String> stock = CompletableFuture.supplyAsync(() -> stockService(code), pool)
                    .thenApply(n -> n + " szt.")
                    .exceptionally(ex -> "stan nieznany (" + unwrap(ex).getMessage() + ")");
            pages.add(price.thenCombine(stock, (pr, st) -> code + ": " + pr + " zł, " + st)
                    .orTimeout(5, TimeUnit.SECONDS));   // hojny limit na całą stronę
        }
        CompletableFuture.allOf(pages.toArray(new CompletableFuture[0])).join();
        List<String> results = pages.stream().map(CompletableFuture::join).toList();
        showEach("PO (równolegle, z planem awaryjnym)", results);
        // WYNIK: PO (równolegle, z planem awaryjnym) (liczba elementów: 3):
        // WYNIK:    • ELE-004: 1299.00 zł, 4 szt.
        // WYNIK:    • SPO-001: 64.99 zł, stan nieznany (magazyn spożywczy nie odpowiada)
        // WYNIK:    • KSI-003: 99.00 zł, 3 szt.
        // Czas: wersja PRZED dla 3 produktów to ok. 6 × 40 ms; wersja PO — ok. 2 × 40 ms przy 4 wątkach puli.
        // (wynik zależy od uruchomienia) — czasów nie wypisujemy, bo zależą od maszyny.

        // Spring i dalej: @Async metoda może zwracać CompletableFuture<T>; kontroler Spring MVC może zwrócić
        // CompletableFuture, a odpowiedź HTTP zostanie wysłana, gdy się ukończy. Reaktywny WebClient (Spring WebFlux)
        // używa podobnych pojęć (Mono = jeden przyszły wynik, map/flatMap zamiast thenApply/thenCompose), ale to osobny
        // świat — z własnymi zasadami (nigdy nie blokuj!). (Java 21+) Wątki wirtualne pozwalają znów pisać prosty kod
        // „po kolei” bez blokowania drogich wątków systemowych.
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI
    // =================================================================================================

    static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger();

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, prefix + "-" + counter.incrementAndGet());
        }
    }

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

    static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • supplyAsync(zadanie, własnaPula) / runAsync(...) — ZAWSZE z własną pulą (bez niej: ForkJoinPool.commonPool).
     *   • thenApply = map (T → U); thenCompose = flatMap (T → CompletableFuture<U>); thenAccept / thenRun — bez wyniku.
     *   • thenCombine — dwa niezależne wyniki uruchomione równolegle; thenCompose — kroki zależne, po kolei.
     *   • allOf → Void; wyniki zbierz z listy pierwotnych (kolejność listy). anyOf → Object, pierwszy UKOŃCZONY.
     *   • exceptionally (błąd → wartość), handle (wynik/błąd → wartość), whenComplete (tylko podgląd).
     *   • Wyjątki bywają opakowane w CompletionException → rozpakuj getCause() przed sprawdzaniem typu.
     *   • join() → CompletionException (niesprawdzany); get() → ExecutionException + InterruptedException (sprawdzane).
     *   • orTimeout / completeOnTimeout (Java 9+) — nie przerywają liczącego zadania.
     *   • thenX może wykonać się w dowolnym wątku; thenXAsync(f, pula) — w puli. Nie blokuj w środku łańcucha.
     *   • Każdy łańcuch kończ obsługą błędu; CompletableFuture.cancel(true) nie przerywa wątku.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego supplyAsync(zadanie) bez podanej puli bywa niebezpieczne dla zadań czekających na sieć?
     *   2. Co wypisze?
     *        CompletableFuture<Integer> f = CompletableFuture.completedFuture(5);
     *        f.thenApply(x -> x + 1);
     *        System.out.println(f.thenApply(x -> x * 10).join());
     *   3. Kiedy thenCompose, a kiedy thenApply? Jaki typ dałby thenApply z funkcją zwracającą CompletableFuture<Long>?
     *   4. ZNAJDŹ BŁĄD:
     *        CompletableFuture.supplyAsync(() -> zapiszZamowienie(z), pula)
     *                .thenApply(id -> wyslijMail(id));
     *        // brak join, exceptionally i handle — „czasem maile nie wychodzą i nie ma żadnego błędu w logach”
     *   5. Co wypisze?
     *        CompletableFuture<Integer> f = CompletableFuture.supplyAsync(() -> 10 / 0, pula)
     *                .thenApply(x -> x + 1)
     *                .exceptionally(ex -> -1);
     *        System.out.println(f.join());
     *   6. ZNAJDŹ BŁĄD:  if (ex instanceof ArithmeticException) { ... }   // wewnątrz exceptionally po supplyAsync
     *   7. Czym różni się join() od get()? Jakie wyjątki rzucają?
     *   8. Czy orTimeout(1, SECONDS) zatrzyma zadanie, które liczy wynik już od 10 sekund?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises(ExecutorService pool) {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        List<String> skus = List.of("KSI-003", "ODZ-001", "ELE-001");
        List<String> expectedNames = List.of("Wzorce projektowe", "Kurtka zimowa", "Laptop Pro 14");
        List<String> texts = List.of("12", "abc", "7");
        List<String> stockSkus = List.of("KSI-001", "XXX-999", "DOM-002");
        Check.equal("ćw. 1: suma liczb × 2 przez supplyAsync + thenApply", 132, () -> exercise1(SampleData.numbers(), pool));
        Check.equal("ćw. 2: nazwy produktów przez allOf (kolejność SKU)", expectedNames, () -> exercise2(skus, pool));
        Check.equal("ćw. 3: parsowanie z handle (błąd → -1)", List.of(12, -1, 7), () -> exercise3(texts, pool));
        Check.equal("ćw. 4: wartość magazynu cena × stan z planem awaryjnym", new BigDecimal("3507.00"),
                () -> exercise4(stockSkus, pool));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 132, () -> solution1(SampleData.numbers(), pool));
        Check.equal("ćw. 2 (wzorzec)", expectedNames, () -> solution2(skus, pool));
        Check.equal("ćw. 3 (wzorzec)", List.of(12, -1, 7), () -> solution3(texts, pool));
        Check.equal("ćw. 4 (wzorzec)", new BigDecimal("3507.00"), () -> solution4(stockSkus, pool));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): przez {@code supplyAsync(..., pool)} policz sumę liczb z listy, a krokiem {@code thenApply}
     * pomnóż ją przez 2. Zwróć wynik przez {@code join()}.
     * Podpowiedź: {@code numbers.stream().mapToInt(Integer::intValue).sum()} wewnątrz dostawcy.
     */
    static int exercise1(List<Integer> numbers, Executor pool) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): dla każdego SKU uruchom asynchronicznie {@code SampleData.productBySku(sku).name()}.
     * Poczekaj na wszystkie przez {@code allOf} i zwróć nazwy w kolejności SKU.
     * Podpowiedź: zbierz {@code List<CompletableFuture<String>>}, a po {@code allOf(...).join()} zrób
     * {@code map(CompletableFuture::join)} na tej liście.
     */
    static List<String> exercise2(List<String> skus, Executor pool) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): każdy tekst parsuj asynchronicznie ({@code Integer.parseInt}); krokiem {@code handle}
     * zamień błąd na -1. Zwróć listę liczb w kolejności tekstów.
     * Podpowiedź: {@code .handle((v, ex) -> ex == null ? v : -1)}.
     */
    static List<Integer> exercise3(List<String> texts, Executor pool) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): PRZEPISZ kod synchroniczny na asynchroniczny:
     * <pre>{@code
     * BigDecimal total = BigDecimal.ZERO;
     * for (String sku : skus) {
     *     Product p = SampleData.productBySku(sku);   // nieznane SKU → NoSuchElementException wysadza całość
     *     total = total.add(p.price().multiply(BigDecimal.valueOf(p.stock())));
     * }
     * }</pre>
     * Dla każdego SKU uruchom DWA zadania: cenę i stan magazynu ({@code supplyAsync}), połącz je {@code thenCombine}
     * (cena × stan), a nieznane SKU zamień na {@code BigDecimal.ZERO} przez {@code exceptionally}. Zsumuj wszystko.
     * Podpowiedź: wynik każdego SKU to {@code CompletableFuture<BigDecimal>}; na końcu
     * {@code futures.stream().map(CompletableFuture::join).reduce(BigDecimal.ZERO, BigDecimal::add)}.
     */
    static BigDecimal exercise4(List<String> skus, Executor pool) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(List<Integer> numbers, Executor pool) {
        return CompletableFuture.supplyAsync(() -> numbers.stream().mapToInt(Integer::intValue).sum(), pool)
                .thenApply(sum -> sum * 2)
                .join();
    }

    static List<String> solution2(List<String> skus, Executor pool) {
        List<CompletableFuture<String>> futures = skus.stream()
                .map(sku -> CompletableFuture.supplyAsync(() -> SampleData.productBySku(sku).name(), pool))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    static List<Integer> solution3(List<String> texts, Executor pool) {
        List<CompletableFuture<Integer>> futures = texts.stream()
                .map(t -> CompletableFuture.supplyAsync(() -> Integer.parseInt(t), pool)
                        .handle((v, ex) -> ex == null ? v : -1))
                .toList();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    static BigDecimal solution4(List<String> skus, Executor pool) {
        List<CompletableFuture<BigDecimal>> futures = new ArrayList<>();
        for (String sku : skus) {
            CompletableFuture<BigDecimal> price =
                    CompletableFuture.supplyAsync(() -> SampleData.productBySku(sku).price(), pool);
            CompletableFuture<Integer> stock =
                    CompletableFuture.supplyAsync(() -> SampleData.productBySku(sku).stock(), pool);
            futures.add(price.thenCombine(stock, (p, s) -> p.multiply(BigDecimal.valueOf(s)))
                    .exceptionally(ex -> BigDecimal.ZERO));   // nieznane SKU → 0 zamiast błędu całości
        }
        return futures.stream().map(CompletableFuture::join).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bez puli zadanie trafia do ForkJoinPool.commonPool() — wspólnej, małej puli (zwykle rdzenie − 1 wątków),
     *      z której korzystają też strumienie równoległe i inne biblioteki. Zadania czekające na sieć zajmą wszystkie jej
     *      wątki i zablokują resztę programu. Rozwiązanie: własna pula dobrana do zadań IO.
     *   2. 50 — pierwsze thenApply zwróciło nowy CompletableFuture, który został zignorowany; f nadal ma wartość 5.
     *   3. thenApply — gdy funkcja zwraca zwykłą wartość; thenCompose — gdy zwraca CompletableFuture (kolejne wywołanie
     *      asynchroniczne). thenApply dałby CompletableFuture<CompletableFuture<Long>>.
     *   4. Błąd (np. wyjątek z wyslijMail) zostaje schowany w CompletableFuture, którego nikt nie odbiera — przepada bez
     *      śladu. Poprawka: dodać exceptionally/handle (np. z logowaniem) albo zwrócić CompletableFuture wołającemu.
     *   5. -1 — dzielenie przez zero rzuca ArithmeticException, thenApply zostaje pominięte, exceptionally zwraca -1.
     *   6. Po supplyAsync exceptionally dostaje CompletionException opakowujący ArithmeticException, więc warunek jest
     *      fałszywy. Trzeba najpierw rozpakować: Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
     *   7. join() rzuca niesprawdzany CompletionException (przyczyna w getCause()), nie wymaga try/catch. get() pochodzi
     *      z Future i rzuca sprawdzane ExecutionException oraz InterruptedException (a z limitem — TimeoutException).
     *   8. Nie. orTimeout ukończy CompletableFuture wyjątkiem TimeoutException, ale wątek liczący zadanie nie zostanie
     *      przerwany — liczy dalej, a jego wynik zostanie zignorowany.
     */
    // </editor-fold>
}
