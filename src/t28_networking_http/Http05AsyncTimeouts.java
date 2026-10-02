package t28_networking_http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import helpers.Check;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Żądania asynchroniczne, limity czasu, ponawianie i ograniczanie współbieżności
 *        (async = asynchroniczne, bez czekania w miejscu; retry = ponowienie; backoff = rosnąca przerwa
 *         między próbami; cancel = anuluj; concurrency = współbieżność)
 *
 * W SKRÓCIE:
 *   client.send(...) blokuje wątek aż do odpowiedzi. client.sendAsync(...) wraca od razu z CompletableFuture
 *   („obietnicą wyniku”), więc wiele żądań leci naraz, a Ty łączysz wyniki: allOf, thenApply, thenCompose.
 *   Sieć zawodzi, więc potrzebujesz limitów czasu, obsługi błędów, ponawiania (z głową!) i ograniczenia
 *   liczby żądań w locie.
 *
 * ANALOGIA: zamawianie jedzenia dla ekipy. Blokująco: zamawiasz pizzę, stoisz pod drzwiami aż przyjedzie,
 *   dopiero potem zamawiasz sushi. Asynchronicznie: dzwonisz do trzech lokali naraz i zajmujesz się czym innym;
 *   „obietnica” każdego zamówienia ma numer. Limit czasu: „jeśli pizza nie dotrze w 45 minut, rezygnuję”.
 *   Ponawianie: „zadzwonię drugi raz, ale nie od razu i nie w nieskończoność”. Semafor: „kurier zabierze
 *   najwyżej dwa zamówienia naraz”.
 *
 * JAK TO DZIAŁA:
 *   {@code CompletableFuture<HttpResponse<String>>} f = client.sendAsync(request, BodyHandlers.ofString());
 *   f.thenApply(HttpResponse::body)                // przekształć wynik, gdy nadejdzie
 *    .thenCompose(body -> następneŻądanie(body))   // wynik uruchamia kolejne ASYNCHRONICZNE zadanie
 *    .orTimeout(2, SECONDS)                         // limit czasu całej operacji (Java 9+)
 *    .exceptionally(ex -> zastępczaWartość)         // obsługa błędu
 *   CompletableFuture.allOf(f1, f2, f3).join();     // poczekaj na wszystkie
 *
 *   Trzy limity czasu: connectTimeout (klient: nawiązanie połączenia), timeout (żądanie: HttpTimeoutException),
 *   orTimeout (cała operacja asynchroniczna: TimeoutException).
 *
 * SŁÓWKA: future = obietnica wyniku; join = poczekaj i pobierz wynik; allOf = wszystkie; thenApply = „potem przekształć”;
 *   thenCompose = „potem uruchom kolejny future”; thenCombine = „połącz dwa wyniki”; exceptionally = „w razie wyjątku”;
 *   handle = „obsłuż wynik albo wyjątek”; semaphore = semafor (licznik zezwoleń); barrier = bariera (czeka na grupę);
 *   in flight = w locie (wysłane, bez odpowiedzi); virtual thread = wątek wirtualny (Java 21+)
 *
 * ZOBACZ TEŻ: t21_concurrency/Concurrency05CompletableFuture (podstawy futures), t21_concurrency/Concurrency07Synchronizers
 *   (semafor, bariera, latch), t28_networking_http/Http02HttpClient (send), t28_networking_http/Http03LocalServer (serwer)
 * </pre>
 */
public class Http05AsyncTimeouts {

    private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress(); // loopback = pętla zwrotna
    private static final java.nio.charset.Charset UTF_8 = StandardCharsets.UTF_8;

    public static void main(String[] args) throws Exception {
        title("Http05 — async, limity czasu, ponawianie");

        try (AsyncServer server = new AsyncServer()) {
            HttpClient client = newClient();
            sendAsyncBasics(server, client);   // send async basics = podstawy sendAsync
            parallelWithAllOf(server, client); // parallel with allOf = równolegle z allOf
            chaining(server, client);          // chaining = łączenie kroków
            errorHandling(server, client);     // error handling = obsługa błędów
            timeouts(server, client);          // timeouts = limity czasu
            cancellation(server, client);      // cancellation = anulowanie
            retries(server, client);           // retries = ponawianie
            limitingConcurrency(server, client); // limiting concurrency = ograniczanie współbieżności
            blockingVersusAsync(server, client); // blocking versus async = blokująco kontra asynchronicznie
        }
        exercises();                           // exercises = ćwiczenia
    }

    // =================================================================================================
    // SERWER TESTOWY I POMOCNIKI
    // =================================================================================================

    static HttpClient newClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2)) // limit czasu nawiązania połączenia
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    static HttpRequest get(URI uri) {
        return HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3)).GET().build(); // limit czasu żądania: 3 s
    }

    /** Rozpakowuje wyjątki-opakowania (CompletionException, ExecutionException) do właściwej przyczyny. */
    static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof ExecutionException) && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    /** Nazwa typu właściwej przyczyny — ją wypisujemy zamiast treści komunikatu (komunikaty zależą od systemu). */
    static String typeOf(Throwable throwable) {
        return unwrap(throwable).getClass().getSimpleName();
    }

    /**
     * Serwer testowy. Adresy:
     * /szybki?n=X (od razu), /wolny (czeka na zatrzask), {@code /bariera?g=G&n=X} (odpowiada dopiero, gdy 3 żądania
     * z tej samej grupy G będą w toku naraz — dowód równoległości), /niestabilny?k=KLUCZ (dwa razy 503, potem 200),
     * /zajety (zlicza żądania w toku).
     */
    static final class AsyncServer implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "async-handler");
            thread.setDaemon(true);
            return thread;
        });
        private final CountDownLatch slowGate = new CountDownLatch(1);
        private final Map<String, CyclicBarrier> barriers = new ConcurrentHashMap<>();
        private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
        private final AtomicInteger inFlight = new AtomicInteger();    // ile żądań /zajety trwa teraz
        private final AtomicInteger maxInFlight = new AtomicInteger(); // największa zaobserwowana liczba
        private final AtomicInteger busyServed = new AtomicInteger();  // ile żądań /zajety obsłużono

        AsyncServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress(LOOPBACK, 0), 0);
            route("/szybki", exchange -> reply(exchange, 200, "odpowiedź " + param(exchange, "n")));
            route("/wolny", exchange -> {
                try {
                    slowGate.await(5, TimeUnit.SECONDS); // czekamy na otwarcie zatrzasku (maks. 5 s)
                    reply(exchange, 200, "spóźniona");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exchange.close();
                } catch (IOException e) {
                    exchange.close(); // klient już odszedł (limit czasu lub anulowanie)
                }
            });
            route("/bariera", exchange -> {
                CyclicBarrier barrier = barriers.computeIfAbsent(param(exchange, "g"), group -> new CyclicBarrier(3));
                try {
                    barrier.await(5, TimeUnit.SECONDS); // await = czekaj, aż dojdą wszyscy (tu: trzy żądania)
                    reply(exchange, 200, "bariera " + param(exchange, "n"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exchange.close();
                } catch (BrokenBarrierException | TimeoutException e) {
                    reply(exchange, 500, "bariera pęknięta");
                }
            });
            route("/niestabilny", exchange -> {
                int attempt = attempts.computeIfAbsent(param(exchange, "k"), key -> new AtomicInteger()).incrementAndGet();
                if (attempt <= 2) {
                    reply(exchange, 503, "chwilowa awaria");
                } else {
                    reply(exchange, 200, "ok po " + attempt + " próbach");
                }
            });
            route("/zajety", exchange -> {
                int now = inFlight.incrementAndGet();
                maxInFlight.accumulateAndGet(now, Math::max); // zapamiętaj maksimum (atomowo)
                try {
                    Thread.sleep(30); // krótka „praca”, żeby żądania miały szansę się zazębić
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                inFlight.decrementAndGet(); // zmniejszamy PRZED odpowiedzią — klient, który ją dostał, widzi już mniejszy licznik
                busyServed.incrementAndGet();
                reply(exchange, 200, "zajęty");
            });
            server.setExecutor(executor);
            server.start();
        }

        private void route(String path, HttpHandler handler) {
            server.createContext(path, exchange -> {
                try {
                    handler.handle(exchange);
                } catch (RuntimeException e) {
                    exchange.close();
                }
            });
        }

        /** Odczytuje parametr zapytania (po odkodowaniu); brak → „?”. */
        private static String param(HttpExchange exchange, String name) {
            String query = exchange.getRequestURI().getRawQuery();
            if (query != null) {
                for (String pair : query.split("&")) {
                    String[] parts = pair.split("=", 2);
                    if (parts[0].equals(name) && parts.length == 2) {
                        return URLDecoder.decode(parts[1], UTF_8);
                    }
                }
            }
            return "?";
        }

        private static void reply(HttpExchange exchange, int status, String text) throws IOException {
            byte[] bytes = text.getBytes(UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }

        URI uri(String pathAndQuery) {
            try {
                return new URI("http", null, LOOPBACK.getHostAddress(), server.getAddress().getPort(), "/", null, null)
                        .resolve(pathAndQuery);
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }

        void releaseSlow() {
            slowGate.countDown();
        }

        int maxInFlight() {
            return maxInFlight.get();
        }

        int busyServed() {
            return busyServed.get();
        }

        @Override
        public void close() {
            slowGate.countDown();
            server.stop(0);
            executor.shutdownNow();
        }
    }

    // =================================================================================================
    // 1. PODSTAWY sendAsync
    // =================================================================================================

    /**
     * 1. sendAsync nie czeka na odpowiedź — zwraca CompletableFuture. Ty mówisz, co zrobić z wynikiem
     * (thenApply), a „join()” pozwala poczekać, gdy wynik jest już naprawdę potrzebny.
     */
    static void sendAsyncBasics(AsyncServer server, HttpClient client) {
        section("1. sendAsync i CompletableFuture");

        CompletableFuture<HttpResponse<String>> future =
                client.sendAsync(get(server.uri("/szybki?n=1")), HttpResponse.BodyHandlers.ofString(UTF_8)); // sendAsync = wyślij asynchronicznie
        CompletableFuture<String> body = future.thenApply(HttpResponse::body); // thenApply = potem przekształć
        show("treść (po join)", body.join()); // join = poczekaj na wynik i go pobierz
        // WYNIK: treść (po join) → odpowiedź 1
        show("czy future jest zakończony?", body.isDone()); // isDone = czy gotowe
        // WYNIK: czy future jest zakończony? → true

        // join() rzuca niekontrolowany CompletionException; get() — kontrolowany ExecutionException i InterruptedException.
        // W kodzie z lambdami wygodniejszy jest join().

        // PUŁAPKA: sendAsync wykonuje się na wątkach pomocniczych klienta (domyślnie wspólna pula) — jeśli w
        // thenApply zrobisz coś długiego lub blokującego, zablokujesz te wątki dla WSZYSTKICH żądań klienta.
        // DOBRA PRAKTYKA: w kroku thenApply rób tylko szybkie przekształcenia; ciężką pracę oddaj własnemu executorowi
        // (thenApplyAsync(funkcja, executor)). Dlaczego: współdzielone wątki klienta mają obsługiwać sieć, nie obliczenia.
    }

    // =================================================================================================
    // 2. RÓWNOLEGLE Z allOf
    // =================================================================================================

    /**
     * 2. Trzy żądania lecą naraz, a wyniki zbieramy w KOLEJNOŚCI ŻĄDAŃ (nie w kolejności nadejścia).
     * Adres /bariera odpowiada dopiero, gdy wszystkie trzy żądania jednocześnie dotrą do serwera —
     * więc sam fakt, że dostaliśmy odpowiedzi, dowodzi, że poszły równolegle (sekwencyjnie nigdy by nie odpowiedziały).
     */
    static void parallelWithAllOf(AsyncServer server, HttpClient client) {
        section("2. Równoległe żądania: allOf, wyniki w kolejności żądań");

        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int n = 1; n <= 3; n++) {
            futures.add(client.sendAsync(get(server.uri("/bariera?g=A&n=" + n)), HttpResponse.BodyHandlers.ofString(UTF_8))
                    .thenApply(HttpResponse::body));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join(); // allOf = czeka na wszystkie
        List<String> results = futures.stream().map(CompletableFuture::join).toList(); // toList = do listy (Java 16+)
        show("wyniki w kolejności żądań", results);
        // WYNIK: wyniki w kolejności żądań → [bariera 1, bariera 2, bariera 3]

        // allOf zwraca CompletableFuture<Void> — sam nie niesie wyników. Dlatego trzymamy listę futures
        // i po allOf wołamy join() na każdym (wynik jest już gotowy, więc nic nie blokuje).

        // PRZED (sekwencyjnie, czas = suma czasów):
        //   for (URI uri : adresy) { wyniki.add(client.send(get(uri), ofString()).body()); }
        // PO (równolegle, czas ≈ najwolniejsze żądanie):
        //   var futures = adresy.stream().map(uri -> client.sendAsync(get(uri), ofString()).thenApply(HttpResponse::body)).toList();
        //   allOf(...).join();  wyniki = futures.stream().map(CompletableFuture::join).toList();

        // PUŁAPKA: wystarczy, że JEDNO żądanie z allOf zawiedzie, a allOf też kończy się wyjątkiem (po zakończeniu
        // wszystkich pozostałych). Jeśli chcesz zachować częściowe wyniki, na każdym future użyj najpierw
        // handle/exceptionally (sekcja 4).
        // DOBRA PRAKTYKA: zawsze zbieraj futures do listy PRZED czekaniem — nie wołaj join() w pętli tworzącej żądania.
        // Dlaczego: join() w tej samej pętli zamienia kod z powrotem w sekwencyjny.
    }

    // =================================================================================================
    // 3. ŁĄCZENIE KROKÓW
    // =================================================================================================

    /**
     * 3. thenApply — przekształć wynik zwykłą funkcją; thenCompose — wynik uruchamia KOLEJNE asynchroniczne
     * zadanie (spłaszcza future w future); thenCombine — połącz wyniki dwóch niezależnych futures.
     */
    static void chaining(AsyncServer server, HttpClient client) {
        section("3. thenApply, thenCompose, thenCombine");

        CompletableFuture<Integer> length = client
                .sendAsync(get(server.uri("/szybki?n=12345")), HttpResponse.BodyHandlers.ofString(UTF_8))
                .thenApply(HttpResponse::body)
                .thenApply(String::length);
        show("długość treści (thenApply)", length.join());
        // WYNIK: długość treści (thenApply) → 15

        // thenCompose: pierwsze żądanie zwraca „odpowiedź 5”, a jego długość (11) trafia do drugiego żądania.
        CompletableFuture<String> second = client
                .sendAsync(get(server.uri("/szybki?n=5")), HttpResponse.BodyHandlers.ofString(UTF_8))
                .thenApply(HttpResponse::body)
                .thenCompose(first -> client
                        .sendAsync(get(server.uri("/szybki?n=" + first.length())), HttpResponse.BodyHandlers.ofString(UTF_8))
                        .thenApply(HttpResponse::body));
        show("żądanie zależne od poprzedniego (thenCompose)", second.join());
        // WYNIK: żądanie zależne od poprzedniego (thenCompose) → odpowiedź 11

        CompletableFuture<String> left = client
                .sendAsync(get(server.uri("/szybki?n=L")), HttpResponse.BodyHandlers.ofString(UTF_8)).thenApply(HttpResponse::body);
        CompletableFuture<String> right = client
                .sendAsync(get(server.uri("/szybki?n=P")), HttpResponse.BodyHandlers.ofString(UTF_8)).thenApply(HttpResponse::body);
        show("dwa niezależne wyniki (thenCombine)", left.thenCombine(right, (l, r) -> l + " + " + r).join());
        // WYNIK: dwa niezależne wyniki (thenCombine) → odpowiedź L + odpowiedź P

        // PUŁAPKA: thenApply z funkcją zwracającą future daje CompletableFuture<CompletableFuture<...>> — „future w future”.
        // Gdy kolejny krok sam jest asynchroniczny, użyj thenCompose (to jak flatMap w strumieniach/Optional).
        // DOBRA PRAKTYKA: zależne kroki łącz thenCompose, niezależne uruchamiaj od razu i łącz thenCombine/allOf.
        // Dlaczego: niezależne żądania nie powinny czekać jedno na drugie.
    }

    // =================================================================================================
    // 4. OBSŁUGA BŁĘDÓW
    // =================================================================================================

    /** Port, na którym nikt nie słucha (do demonstracji błędu połączenia). */
    static int closedPort() throws IOException {
        try (ServerSocket temporary = new ServerSocket(0, 1, LOOPBACK)) {
            return temporary.getLocalPort();
        }
    }

    /**
     * 4. Błąd w future nie leci jako zwykły wyjątek: future kończy się „wyjątkowo”. Dopiero join() rzuca
     * CompletionException, a PRAWDZIWA przyczyna siedzi w getCause(). Wypisujemy tylko TYPY (komunikaty zależą od systemu).
     */
    static void errorHandling(AsyncServer server, HttpClient client) throws Exception {
        section("4. Błędy: CompletionException, exceptionally, handle");

        int closed = closedPort();
        HttpClient patient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1).build(); // 5 s: na niektórych systemach odmowa trwa dłużej
        URI deadUri = new URI("http", null, LOOPBACK.getHostAddress(), closed, "/", null, null);

        CompletableFuture<String> failing = patient
                .sendAsync(get(deadUri), HttpResponse.BodyHandlers.ofString(UTF_8))
                .thenApply(HttpResponse::body);
        try {
            failing.join();
            note("udało się (nie powinno)");
        } catch (CompletionException e) { // CompletionException = opakowanie błędu z future
            show("join rzuca", e.getClass().getSimpleName());
            // WYNIK: join rzuca → CompletionException
            show("prawdziwa przyczyna", e.getCause().getClass().getSimpleName()); // getCause = pobierz przyczynę
            // WYNIK: prawdziwa przyczyna → ConnectException
        }

        // exceptionally: wartość zastępcza przy błędzie (funkcja dostaje wyjątek, zwykle opakowany).
        String fallback = patient
                .sendAsync(get(deadUri), HttpResponse.BodyHandlers.ofString(UTF_8))
                .thenApply(HttpResponse::body)
                .exceptionally(ex -> "(błąd: " + typeOf(ex) + ")") // exceptionally = w razie wyjątku
                .join();
        show("exceptionally", fallback);
        // WYNIK: exceptionally → (błąd: ConnectException)

        // handle: dostaje wynik ALBO wyjątek (jeden z nich jest null) — dobre do ujednolicenia obu przypadków.
        CompletableFuture<String> ok = client
                .sendAsync(get(server.uri("/szybki?n=7")), HttpResponse.BodyHandlers.ofString(UTF_8))
                .handle((response, ex) -> ex == null ? "OK " + response.statusCode() : "BŁĄD " + typeOf(ex)); // handle = obsłuż oba
        show("handle (sukces)", ok.join());
        // WYNIK: handle (sukces) → OK 200

        // Zachowanie częściowych wyników: każdy future ma własne exceptionally, więc allOf nie zawiedzie.
        List<CompletableFuture<String>> mixed = List.of(
                client.sendAsync(get(server.uri("/szybki?n=1")), HttpResponse.BodyHandlers.ofString(UTF_8))
                        .thenApply(HttpResponse::body).exceptionally(ex -> "błąd"),
                patient.sendAsync(get(deadUri), HttpResponse.BodyHandlers.ofString(UTF_8))
                        .thenApply(HttpResponse::body).exceptionally(ex -> "błąd"));
        CompletableFuture.allOf(mixed.toArray(new CompletableFuture<?>[0])).join();
        show("wyniki częściowe", mixed.stream().map(CompletableFuture::join).toList());
        // WYNIK: wyniki częściowe → [odpowiedź 1, błąd]

        // PUŁAPKA: 404 i 500 to nie błędy future (patrz Http02HttpClient) — future kończy się normalnie z odpowiedzią.
        // Wyjątkiem kończą się tylko awarie przesyłu (ConnectException, HttpTimeoutException...). Sprawdzaj kod statusu
        // w thenApply albo thenCompose.
        // PUŁAPKA: komunikat z exceptionally to zwykle CompletionException z przyczyną w środku. Porównywanie
        // ex.getClass() z ConnectException zawsze da false — najpierw „rozpakuj” (nasza metoda unwrap).
        // DOBRA PRAKTYKA: na końcu każdego łańcucha zadbaj o obsługę błędu (exceptionally/handle/whenComplete) albo
        // świadomie zwróć future wywołującemu. Dlaczego: niezauważony błąd w tle po prostu znika.
    }

    // =================================================================================================
    // 5. LIMITY CZASU
    // =================================================================================================

    /**
     * 5. Trzy limity działają na różnych poziomach:
     * <ul>
     *   <li>connectTimeout (klient) — nawiązanie połączenia → HttpConnectTimeoutException,</li>
     *   <li>timeout (żądanie) — czas do odpowiedzi → HttpTimeoutException,</li>
     *   <li>orTimeout (future, Java 9+) — cała operacja asynchroniczna wraz z krokami po drodze → TimeoutException.</li>
     * </ul>
     * Wolną odpowiedź wymusza zatrzask na serwerze (zamiast długich sleep), więc wyniki są powtarzalne.
     */
    static void timeouts(AsyncServer server, HttpClient client) {
        section("5. Limity czasu: timeout żądania, orTimeout, completeOnTimeout");

        HttpRequest withTimeout = HttpRequest.newBuilder(server.uri("/wolny")).timeout(Duration.ofMillis(300)).build();
        CompletableFuture<HttpResponse<String>> byRequest = client.sendAsync(withTimeout, HttpResponse.BodyHandlers.ofString(UTF_8));
        try {
            byRequest.join();
        } catch (CompletionException e) {
            show("timeout żądania, typ", typeOf(e));
        }
        // WYNIK: timeout żądania, typ → HttpTimeoutException

        HttpRequest noLimit = HttpRequest.newBuilder(server.uri("/wolny")).build(); // żądanie bez własnego limitu
        CompletableFuture<HttpResponse<String>> byFuture = client
                .sendAsync(noLimit, HttpResponse.BodyHandlers.ofString(UTF_8))
                .orTimeout(300, TimeUnit.MILLISECONDS); // orTimeout = zakończ wyjątkiem po czasie (Java 9+)
        try {
            byFuture.join();
        } catch (CompletionException e) {
            show("orTimeout, typ", typeOf(e));
        }
        // WYNIK: orTimeout, typ → TimeoutException

        String fallback = client
                .sendAsync(noLimit, HttpResponse.BodyHandlers.ofString(UTF_8))
                .thenApply(HttpResponse::body)
                .completeOnTimeout("(brak odpowiedzi)", 300, TimeUnit.MILLISECONDS) // wartość zastępcza po czasie (Java 9+)
                .join();
        show("completeOnTimeout", fallback);
        // WYNIK: completeOnTimeout → (brak odpowiedzi)

        // PUŁAPKA: orTimeout/completeOnTimeout kończą FUTURE, ale nie gwarantują zatrzymania żądania HTTP —
        // serwer może dalej pracować, a połączenie zostaje zajęte. Do ograniczenia czasu samego żądania służy
        // HttpRequest.Builder.timeout(...). orTimeout jest dobry jako limit „całej operacji” złożonej z wielu kroków.
        // PUŁAPKA: brak limitu czasu żądania oznacza czekanie bez końca na zawieszony serwer.
        // DOBRA PRAKTYKA: ustaw connectTimeout (klient) i timeout (żądanie) ZAWSZE, a orTimeout dodaj dla całej operacji.
        // Dlaczego: każdy poziom łapie inną awarię (brak połączenia, wolny serwer, powolny łańcuch kroków).
        // Ostatnie trzy żądania wiszą na serwerze, aż otworzymy zatrzask — zrobimy to na końcu następnej sekcji.
    }

    // =================================================================================================
    // 6. ANULOWANIE
    // =================================================================================================

    /**
     * 6. cancel(true) kończy future przez CancellationException. Dla HttpClient anulowanie future to prośba
     * o przerwanie żądania — nie ma gwarancji, że serwer natychmiast przestanie pracować.
     */
    static void cancellation(AsyncServer server, HttpClient client) {
        section("6. Anulowanie future (cancel)");

        CompletableFuture<HttpResponse<String>> pending = client
                .sendAsync(HttpRequest.newBuilder(server.uri("/wolny")).build(), HttpResponse.BodyHandlers.ofString(UTF_8));
        try {
            show("cancel zwróciło", pending.cancel(true)); // cancel = anuluj
            // WYNIK: cancel zwróciło → true
            show("isCancelled", pending.isCancelled()); // isCancelled = czy anulowany
            // WYNIK: isCancelled → true
            try {
                pending.join();
                note("join zadziałał (nie powinien)");
            } catch (CancellationException e) { // CancellationException = future został anulowany
                show("join po anulowaniu rzuca", e.getClass().getSimpleName());
                // WYNIK: join po anulowaniu rzuca → CancellationException
            }
        } finally {
            server.releaseSlow(); // otwieramy zatrzask: „wiszące” żądania /wolny na serwerze się kończą
        }

        // PUŁAPKA: CancellationException to wyjątek NIEkontrolowany i inny niż CompletionException. Kod, który łapie
        // tylko CompletionException z join(), nie obsłuży anulowania.
        // DOBRA PRAKTYKA: anuluj future, gdy wynik przestał być potrzebny (użytkownik zamknął ekran, inny
        // wątek już dostał odpowiedź) i traktuj to jako zwykły scenariusz. Dlaczego: zwalnia zasoby i wątki.
    }

    // =================================================================================================
    // 7. PONAWIANIE
    // =================================================================================================

    /**
     * Wysyła żądanie, a przy odpowiedzi 5xx ponawia (najwyżej {@code max} prób) z rosnącą przerwą (backoff:
     * 10, 20, 40... ms). Zapisuje przebieg w dzienniku. Przerwy realizuje delayedExecutor (Java 9+), bez blokowania wątku.
     */
    static CompletableFuture<HttpResponse<String>> sendWithRetry(HttpClient client, HttpRequest request, int attempt,
            int max, long baseDelayMs, List<String> journal) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(UTF_8)).thenCompose(response -> {
            journal.add("próba " + attempt + " → " + response.statusCode());
            if (response.statusCode() >= 500 && attempt < max) {
                long delay = baseDelayMs * (1L << (attempt - 1)); // 1L << n = 2 do potęgi n: przerwy 10, 20, 40...
                journal.add("czekam " + delay + " ms");
                // delayedExecutor = executor, który uruchamia zadanie dopiero po opóźnieniu
                return CompletableFuture.supplyAsync(() -> "dalej", CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS))
                        .thenCompose(ignored -> sendWithRetry(client, request, attempt + 1, max, baseDelayMs, journal));
            }
            return CompletableFuture.completedFuture(response);
        });
    }

    /**
     * 7. Ponawianie to broń obosieczna: pomaga przy chwilowych awariach (503), ale bez limitu i przerw potrafi
     * zalać chory serwer żądaniami. Zasady: limit prób, rosnące przerwy (backoff), tylko metody idempotentne,
     * tylko błędy „chwilowe” (5xx, błędy połączenia — nie 4xx!).
     */
    static void retries(AsyncServer server, HttpClient client) {
        section("7. Ponawianie z limitem i backoffem");

        List<String> journal = Collections.synchronizedList(new ArrayList<>());
        HttpResponse<String> response = sendWithRetry(client, get(server.uri("/niestabilny?k=a")), 1, 5, 10, journal).join();
        showEach("przebieg", journal);
        // WYNIK: przebieg (liczba elementów: 5):
        // WYNIK:    • próba 1 → 503
        // WYNIK:    • czekam 10 ms
        // WYNIK:    • próba 2 → 503
        // WYNIK:    • czekam 20 ms
        // WYNIK:    • próba 3 → 200
        show("wynik końcowy", response.statusCode() + " " + response.body());
        // WYNIK: wynik końcowy → 200 ok po 3 próbach

        List<String> journal2 = Collections.synchronizedList(new ArrayList<>());
        HttpResponse<String> gaveUp = sendWithRetry(client, get(server.uri("/niestabilny?k=b")), 1, 2, 10, journal2).join();
        show("limit 2 próby: przebieg", journal2);
        // WYNIK: limit 2 próby: przebieg → [próba 1 → 503, czekam 10 ms, próba 2 → 503]
        show("limit 2 próby: wynik końcowy", gaveUp.statusCode());
        // WYNIK: limit 2 próby: wynik końcowy → 503

        // PUŁAPKA: ponawianie POST może zdublować skutek (dwa zamówienia — Http04JsonApi). Ponawiaj tylko
        // GET, PUT, DELETE (idempotentne), a POST wyłącznie z kluczem idempotentności.
        // PUŁAPKA: ponawianie bez przerw i bez limitu przy awarii serwera zwielokrotnia ruch (każdy klient
        // ponawia natychmiast) i pogłębia awarię. Dodaj losowe „drżenie” (jitter) do przerw, żeby klienci nie
        // uderzali w tej samej chwili — tutaj pomijamy je dla powtarzalności wyniku.
        // DOBRA PRAKTYKA: limit prób (np. 3–5), przerwa rosnąca wykładniczo (backoff), ponawianie tylko błędów chwilowych.
        // Dlaczego: błąd 4xx (np. 400, 404) nie zniknie po ponowieniu, a serwer w kłopotach potrzebuje wytchnienia.
    }

    // =================================================================================================
    // 8. OGRANICZANIE WSPÓŁBIEŻNOŚCI
    // =================================================================================================

    /**
     * 8. Semaphore (semafor) to licznik „zezwoleń”: acquire() zabiera zezwolenie (czeka, gdy brak),
     * release() je oddaje. Dzięki temu w locie jest najwyżej N żądań, nawet gdy do wysłania jest ich setki.
     */
    static void limitingConcurrency(AsyncServer server, HttpClient client) throws Exception {
        section("8. Ograniczanie liczby żądań w locie (Semaphore)");

        Semaphore permits = new Semaphore(2); // dwa zezwolenia = najwyżej dwa żądania naraz
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            permits.acquire(); // acquire = weź zezwolenie (czeka, aż któreś zostanie zwolnione)
            futures.add(client.sendAsync(get(server.uri("/zajety")), HttpResponse.BodyHandlers.ofString(UTF_8))
                    .whenComplete((response, ex) -> permits.release()) // release = oddaj zezwolenie ZAWSZE (też przy błędzie)
                    .thenApply(HttpResponse::statusCode));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
        show("kody odpowiedzi", futures.stream().map(CompletableFuture::join).toList());
        // WYNIK: kody odpowiedzi → [200, 200, 200, 200, 200, 200]
        show("obsłużone przez serwer", server.busyServed());
        // WYNIK: obsłużone przez serwer → 6
        show("serwer widział najwyżej 2 żądania naraz?", server.maxInFlight() <= 2);
        // WYNIK: serwer widział najwyżej 2 żądania naraz? → true

        // PUŁAPKA: zezwolenie MUSI wrócić także przy błędzie — dlatego release jest w whenComplete (wykona się
        // zawsze). Release tylko w ścieżce sukcesu „gubi” zezwolenia, a po kilku błędach program zawiesza się na acquire().
        // DOBRA PRAKTYKA: ogranicz równoległość wobec cudzych serwerów (kilka–kilkanaście żądań) i użyj puli o stałej
        // liczbie wątków albo Semaphore. Dlaczego: setki równoległych żądań to przeciążenie serwera, blokady od
        // dostawcy API (kod 429 Too Many Requests) i wyczerpanie własnych zasobów.
    }

    // =================================================================================================
    // 9. BLOKUJĄCO KONTRA ASYNCHRONICZNIE
    // =================================================================================================

    /**
     * 9. Ten sam efekt można uzyskać blokującym send() w puli wątków — każdy wątek czeka na swoją odpowiedź.
     * Różnica: asynchroniczny klient nie zajmuje wątku na czas oczekiwania.
     */
    static void blockingVersusAsync(AsyncServer server, HttpClient client) throws Exception {
        section("9. Blokujące send w puli wątków kontra sendAsync");

        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int n = 1; n <= 3; n++) {
                URI uri = server.uri("/bariera?g=B&n=" + n);
                Callable<String> task = () -> client.send(get(uri), HttpResponse.BodyHandlers.ofString(UTF_8)).body();
                futures.add(pool.submit(task)); // trzy wątki naraz → bariera na serwerze się otwiera
            }
            List<String> results = new ArrayList<>();
            for (Future<String> future : futures) {
                results.add(future.get(5, TimeUnit.SECONDS));
            }
            show("blokująco w puli 3 wątków", results);
            // WYNIK: blokująco w puli 3 wątków → [bariera 1, bariera 2, bariera 3]
        } finally {
            pool.shutdownNow();
        }

        // Porównanie:
        //   send w puli wątków   | prosty kod, jeden wątek na jedno oczekujące żądanie (przy 10 000 żądań — 10 000 wątków)
        //   sendAsync + future   | mało wątków, ale kod „łańcuchowy”, trudniejszy w debugowaniu (błędy w CompletionException)
        // (Java 21+: wątki wirtualne — blokujące send() w każdym wątku wirtualnym jest tanie, więc można pisać prosty
        // kod blokujący i mieć skalowalność zbliżoną do async. W Javie 17 ta opcja jeszcze nie jest dostępna.)

        // PUŁAPKA: pula wątków mniejsza niż liczba zadań, które muszą działać RÓWNOCZEŚNIE (tu: 3 żądania do
        // bariery), kończy się zawieszeniem — to samo, co sekwencyjne send() w pętli. Rozmiar puli musi wynikać z potrzeb.
        // DOBRA PRAKTYKA: do kilku-kilkunastu żądań wystarczy pula + send(); do wielu tysięcy lub gdy łączysz
        // wiele kroków — sendAsync. Wybierz prostsze rozwiązanie, które spełnia wymagania. Dlaczego: kod ma
        // być zrozumiały dla następnej osoby (za pół roku — Ciebie).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • sendAsync → CompletableFuture; thenApply (przekształć), thenCompose (zależne żądanie), thenCombine (dwa wyniki),
     *     allOf (wszystkie; zwraca Void — wyniki z listy futures po join), exceptionally/handle/whenComplete (błędy).
     *   • join() rzuca CompletionException (przyczyna w getCause()); get() — ExecutionException; po cancel — CancellationException.
     *   • Limity czasu: connectTimeout (klient), timeout (żądanie → HttpTimeoutException), orTimeout/completeOnTimeout (future).
     *   • 404/500 to nie wyjątki — sprawdzaj kod statusu.
     *   • Ponawianie: limit prób + backoff + tylko idempotentne metody + tylko błędy chwilowe (5xx, połączenie).
     *   • Semaphore: acquire przed wysłaniem, release w whenComplete (zawsze); ogranicza liczbę żądań w locie.
     *   • Pula wątków z send() jest prostsza; sendAsync oszczędza wątki; (Java 21+) wątki wirtualne łączą prostotę i skalę.
     *   • W wynikach i testach wypisuj TYPY wyjątków, nie ich komunikaty.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się thenApply od thenCompose? Kiedy thenApply da „future w future”?
     *   2. Dlaczego allOf zwraca future bez wyniku (Void) i jak odebrać wyniki w kolejności żądań?
     *   3. Co wypisze:  f.join()  gdy future zakończył się ConnectException — jaki typ wyjątku poleci i gdzie jest ConnectException?
     *   4. ZNAJDŹ BŁĄD:  for (URI u : adresy) { wyniki.add(client.sendAsync(req(u), h).join()); }
     *                    (czy to jest równolegle?)
     *   5. Czym różnią się: connectTimeout, timeout żądania i orTimeout? Który wyjątek odpowiada każdemu?
     *   6. ZNAJDŹ BŁĄD:  semafor.acquire(); client.sendAsync(...).thenApply(r -> { semafor.release(); return r; });
     *                    (co się stanie, gdy żądanie zakończy się wyjątkiem?)
     *   7. Dlaczego nie ponawiamy automatycznie POST i odpowiedzi 404?
     *   8. Dlaczego pula wątków o rozmiarze 2 zawiesi test, w którym trzy żądania czekają na siebie nawzajem?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Pomocnik: Check.equal przyjmuje Supplier bez wyjątków kontrolowanych, więc je opakowujemy. */
    static <T> T call(Callable<T> action) {
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e.getClass().getSimpleName(), e);
        }
    }

    static void exercises() throws Exception {
        try (AsyncServer server = new AsyncServer()) {
            HttpClient client = newClient();
            List<URI> three = List.of(server.uri("/bariera?g=E1&n=1"), server.uri("/bariera?g=E1&n=2"), server.uri("/bariera?g=E1&n=3"));
            List<URI> threeRef = List.of(server.uri("/bariera?g=E2&n=1"), server.uri("/bariera?g=E2&n=2"), server.uri("/bariera?g=E2&n=3"));

            section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
            Check.equal("ćw. 1: treść", "odpowiedź 1", () -> call(() -> exercise1(client, server.uri("/szybki?n=1")).join()));
            Check.equal("ćw. 2: równolegle, w kolejności", List.of("bariera 1", "bariera 2", "bariera 3"),
                    () -> call(() -> exercise2(client, three)));
            Check.equal("ćw. 3: szybki", "odpowiedź 5", () -> call(() -> exercise3(client, server.uri("/szybki?n=5"), Duration.ofMillis(500))));
            Check.equal("ćw. 3: wolny", "(brak)", () -> call(() -> exercise3(client, server.uri("/wolny"), Duration.ofMillis(300))));
            Check.equal("ćw. 4: trzecia próba", 3, () -> call(() -> exercise4(client, server.uri("/niestabilny?k=e4a"), 5)));
            Check.equal("ćw. 4: za mało prób", -1, () -> call(() -> exercise4(client, server.uri("/niestabilny?k=e4b"), 2)));
            Check.summary();

            section("ĆWICZENIA — rozwiązania wzorcowe");
            Check.equal("ćw. 1 (wzorzec)", "odpowiedź 1", () -> call(() -> solution1(client, server.uri("/szybki?n=1")).join()));
            Check.equal("ćw. 2 (wzorzec)", List.of("bariera 1", "bariera 2", "bariera 3"), () -> call(() -> solution2(client, threeRef)));
            Check.equal("ćw. 3 (wzorzec): szybki", "odpowiedź 5", () -> call(() -> solution3(client, server.uri("/szybki?n=5"), Duration.ofMillis(500))));
            Check.equal("ćw. 3 (wzorzec): wolny", "(brak)", () -> call(() -> solution3(client, server.uri("/wolny"), Duration.ofMillis(300))));
            Check.equal("ćw. 4 (wzorzec): trzecia próba", 3, () -> call(() -> solution4(client, server.uri("/niestabilny?k=r4a"), 5)));
            Check.equal("ćw. 4 (wzorzec): za mało prób", -1, () -> call(() -> solution4(client, server.uri("/niestabilny?k=r4b"), 2)));
            Check.summary();
            // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
        }
    }

    /**
     * ĆWICZENIE 1 (łatwe): wyślij GET asynchronicznie i zwróć future z samą treścią odpowiedzi (String).
     * Podpowiedź: client.sendAsync(get(uri), BodyHandlers.ofString(UTF_8)).thenApply(HttpResponse::body).
     */
    static CompletableFuture<String> exercise1(HttpClient client, URI uri) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): pobierz wszystkie adresy RÓWNOLEGLE i zwróć treści w kolejności adresów.
     * PRZEPISZ (wersja sekwencyjna — tu zawiesi się, bo serwer czeka na trzy żądania naraz):
     * <pre>{@code
     * List<String> results = new ArrayList<>();
     * for (URI uri : uris) {
     *     results.add(client.send(get(uri), BodyHandlers.ofString(UTF_8)).body());
     * }
     * }</pre>
     * Podpowiedź: najpierw lista futures (sendAsync + thenApply), potem allOf(...).join(), na końcu join() każdego.
     */
    static List<String> exercise2(HttpClient client, List<URI> uris) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć treść odpowiedzi, ale jeśli żądanie nie zakończy się w podanym czasie —
     * tekst {@code (brak)}. Użyj mechanizmu future (nie samego timeout żądania).
     * Podpowiedź: completeOnTimeout("(brak)", czas w ms, TimeUnit.MILLISECONDS) na future z treścią, potem join().
     */
    static String exercise3(HttpClient client, URI uri, Duration limit) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): wysyłaj GET pod adres (blokująco, send), aż dostaniesz kod 200 — najwyżej
     * {@code maxAttempts} razy. Zwróć numer próby, w której się udało, albo -1, gdy się nie udało.
     * Dla adresu /niestabilny dwie pierwsze próby dają 503. Przerwa między próbami: 10 ms * numer próby.
     * Podpowiedź: pętla for po próbach; Thread.sleep w try/catch InterruptedException (przywróć flagę przerwania).
     */
    static int exercise4(HttpClient client, URI uri, int maxAttempts) throws Exception {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static CompletableFuture<String> solution1(HttpClient client, URI uri) {
        return client.sendAsync(get(uri), HttpResponse.BodyHandlers.ofString(UTF_8)).thenApply(HttpResponse::body);
    }

    static List<String> solution2(HttpClient client, List<URI> uris) {
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (URI uri : uris) {
            futures.add(solution1(client, uri));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    static String solution3(HttpClient client, URI uri, Duration limit) {
        return solution1(client, uri).completeOnTimeout("(brak)", limit.toMillis(), TimeUnit.MILLISECONDS).join();
    }

    static int solution4(HttpClient client, URI uri, int maxAttempts) throws Exception {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            HttpResponse<Void> response = client.send(get(uri), HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 200) {
                return attempt;
            }
            try {
                Thread.sleep(10L * attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }
        return -1;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. thenApply stosuje zwykłą funkcję do wyniku (T → U). thenCompose stosuje funkcję, która sama zwraca
     *      CompletableFuture (T → CompletableFuture<U>) i „spłaszcza” wynik. Użycie thenApply z funkcją zwracającą future
     *      daje CompletableFuture<CompletableFuture<U>>.
     *   2. Bo allOf czeka tylko na zakończenie wszystkich i nie wie, jakiego typu są wyniki. Trzymasz listę futures
     *      w kolejności żądań, a po allOf(...).join() wołasz join() na każdym (wyniki są już gotowe).
     *   3. join() rzuci CompletionException; ConnectException jest jego przyczyną (getCause()).
     *   4. Nie — join() w pętli zaraz po sendAsync czeka na wynik, więc żądania idą jedno po drugim (sekwencyjnie).
     *      Najpierw zbierz futures do listy, potem czekaj.
     *   5. connectTimeout (klient) — czas nawiązania połączenia (HttpConnectTimeoutException); timeout żądania —
     *      czas do odpowiedzi (HttpTimeoutException); orTimeout — limit dla całego future (TimeoutException).
     *   6. Przy wyjątku thenApply w ogóle się nie wykona, więc release nie zostanie wywołane i zezwolenie przepadnie;
     *      po kilku takich błędach acquire() zablokuje się na zawsze. Release trzeba dać w whenComplete (zawsze).
     *   7. POST nie jest idempotentny — ponowienie może zdublować skutek. 404 oznacza „nie ma zasobu” — ponowienie
     *      go nie stworzy; ponawiamy tylko błędy chwilowe (5xx, awarie połączenia).
     *   8. Trzy żądania muszą być w toku jednocześnie, żeby bariera się otworzyła; pula o dwóch wątkach wykona
     *      tylko dwa naraz, a one będą czekać na trzecie, które nigdy nie wystartuje (zakleszczenie, przerwane limitem czasu).
     */
    // </editor-fold>
}
