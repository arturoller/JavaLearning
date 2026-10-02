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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Klient HTTP — java.net.http.HttpClient (Java 11+)
 *        (client = klient; request = żądanie; response = odpowiedź; redirect = przekierowanie)
 *
 * W SKRÓCIE:
 *   HttpClient wysyła żądania HTTP i zwraca odpowiedzi. Budujesz go raz (z limitami czasu), potem budujesz
 *   żądania (HttpRequest) i wysyłasz metodą send. Najważniejsza pułapka: odpowiedź 404 albo 500 to dla klienta
 *   zwykły sukces — wyjątku nie ma, trzeba samemu sprawdzić kod statusu.
 *   Wszystkie przykłady łączą się z małym serwerem uruchamianym w tej lekcji (adres lokalny, bez internetu).
 *
 * ANALOGIA: poczta. HttpClient to Ty przy okienku. HttpRequest to wypełniony formularz nadania
 *   (adres, rodzaj przesyłki, treść). HttpResponse to odpowiedź urzędnika: kod („przyjęto”, „brak takiego adresu”)
 *   i ewentualna zawartość. Urzędnik, który mówi „brak takiego adresu”, nie rzuca w Ciebie kamieniem (wyjątkiem) —
 *   po prostu odpowiada; to Ty musisz zrozumieć odpowiedź.
 *
 * JAK TO DZIAŁA:
 *   HttpClient client = HttpClient.newBuilder().connectTimeout(...).followRedirects(...).build();
 *   HttpRequest request = HttpRequest.newBuilder(uri).header(...).POST(BodyPublishers.ofString(...)).build();
 *   HttpResponse{@code <String>} response = client.send(request, BodyHandlers.ofString());
 *   response.statusCode() / response.headers() / response.body()
 *
 *   BodyPublishers = jak zamienić dane na treść żądania; BodyHandlers = jak zamienić treść odpowiedzi na obiekt
 *   (String, linie, bajty, nic).  Wyjątek leci tylko przy awarii połączenia lub przekroczeniu czasu.
 *
 * SŁÓWKA: builder = budowniczy (obiekt do składania obiektów krok po kroku); connect timeout = limit czasu
 *   nawiązania połączenia; request timeout = limit czasu na całą odpowiedź; publisher = nadawca treści;
 *   handler = obsługa treści odpowiedzi; status code = kod statusu; redirect = przekierowanie;
 *   latch = zatrzask (CountDownLatch); discarding = odrzucanie (treść jest pomijana)
 *
 * ZOBACZ TEŻ: t28_networking_http/Http01UriUrl (adresy, metody i kody), t28_networking_http/Http03LocalServer
 *   (własny serwer), t28_networking_http/Http05AsyncTimeouts (sendAsync), t21_concurrency/Concurrency07Synchronizers (latch)
 * </pre>
 */
public class Http02HttpClient {

    private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress(); // getLoopbackAddress = adres pętli zwrotnej
    private static final java.nio.charset.Charset UTF_8 = StandardCharsets.UTF_8;

    public static void main(String[] args) throws Exception {
        title("Http02 — HttpClient");

        try (TestServer server = new TestServer()) { // serwer testowy działa tylko w tym bloku
            buildingTheClient();           // building the client = budowa klienta
            simpleGet(server);             // simple get = proste GET
            postWithHeaders(server);       // post with headers = POST z nagłówkami
            bodyHandlers(server);          // body handlers = sposoby czytania treści
            errorsAreNotExceptions(server); // errors are not exceptions = błędy to nie wyjątki
            redirects(server);             // redirects = przekierowania
            sharedClient(server);          // shared client = jeden klient dla wielu żądań
            timeouts(server);              // timeouts = limity czasu
            serverLog(server);             // server log = co zobaczył serwer
        }
        exercises();                       // exercises = ćwiczenia
    }

    // =================================================================================================
    // SERWER TESTOWY (szczegóły w lekcji Http03LocalServer — tu traktuj go jak „czarną skrzynkę”)
    // =================================================================================================

    /**
     * Mały serwer HTTP w tym samym programie. Adresy: /hello, /echo, /naglowki, /linie, /stary (302 → /hello),
     * /brak (404), /awaria (500), /wolny (czeka na zatrzask, żeby test limitu czasu był deterministyczny).
     * Używa com.sun.net.httpserver — nazwa „com.sun” jest historyczna, to wspierane i wyeksportowane API JDK.
     */
    static final class TestServer implements AutoCloseable {
        private final HttpServer server; // HttpServer = serwer HTTP z JDK
        private final ExecutorService executor = Executors.newFixedThreadPool(6, runnable -> {
            Thread thread = new Thread(runnable, "http-handler");
            thread.setDaemon(true);
            return thread;
        });
        private final List<String> log = Collections.synchronizedList(new ArrayList<>());
        private final CountDownLatch slowGate = new CountDownLatch(1); // CountDownLatch = zatrzask odliczający

        TestServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress(LOOPBACK, 0), 0); // port 0 = wolny port
            route("/hello", exchange -> {
                exchange.getResponseHeaders().set("X-Wersja-Api", "1"); // własny nagłówek odpowiedzi
                reply(exchange, 200, "Cześć z serwera!");
            });
            route("/echo", exchange -> {
                String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8); // readAllBytes = wczytaj wszystko
                reply(exchange, 200, "metoda=" + exchange.getRequestMethod() + "; treść=" + body);
            });
            route("/naglowki", exchange -> reply(exchange, 200,
                    "typ=" + exchange.getRequestHeaders().getFirst("Content-Type")
                            + "; X-Klient=" + exchange.getRequestHeaders().getFirst("X-Klient")));
            route("/linie", exchange -> reply(exchange, 200, "pierwsza\ndruga\ntrzecia"));
            route("/stary", exchange -> {
                exchange.getResponseHeaders().set("Location", "/hello"); // dokąd iść dalej
                exchange.sendResponseHeaders(302, -1); // -1 = odpowiedź bez treści
                exchange.close();
            });
            route("/brak", exchange -> reply(exchange, 404, "Nie ma takiej strony"));
            route("/awaria", exchange -> reply(exchange, 500, "Awaria serwera"));
            route("/wolny", exchange -> {
                try {
                    slowGate.await(5, TimeUnit.SECONDS); // czekamy, aż lekcja otworzy zatrzask (maks. 5 s)
                    reply(exchange, 200, "spóźniona odpowiedź");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exchange.close();
                } catch (IOException e) {
                    exchange.close(); // klient dawno się rozłączył — nic więcej nie robimy
                }
            });
            server.setExecutor(executor);
            server.start();
        }

        /** Rejestruje obsługę ścieżki i dopisuje każde żądanie do dziennika. */
        private void route(String path, HttpHandler handler) {
            server.createContext(path, exchange -> {
                log.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
                try {
                    handler.handle(exchange);
                } catch (IOException e) {
                    exchange.close();
                }
            });
        }

        private static void reply(HttpExchange exchange, int status, String text) throws IOException {
            byte[] bytes = text.getBytes(UTF_8); // długość liczymy w BAJTACH, nie w znakach
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }

        URI uri(String path) {
            try {
                // konstruktor z częściami sam poprawnie złoży adres (także dla IPv6)
                return new URI("http", null, LOOPBACK.getHostAddress(), server.getAddress().getPort(), path, null, null);
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }

        /** Zwalnia zatrzask, na którym czekają wolne żądania. */
        void releaseSlow() {
            slowGate.countDown(); // countDown = odlicz (zatrzask otwiera się po dojściu do zera)
        }

        List<String> log() {
            synchronized (log) {
                return new ArrayList<>(log);
            }
        }

        @Override
        public void close() {
            slowGate.countDown();
            server.stop(0); // stop(0) = zatrzymaj od razu, bez czekania na zakończenie żądań
            executor.shutdownNow();
        }
    }

    // =================================================================================================
    // 1. BUDOWA KLIENTA
    // =================================================================================================

    /**
     * 1. Klienta budujemy raz. Domyślnie: wersja protokołu HTTP/2 (z awaryjnym powrotem do HTTP/1.1),
     * przekierowania wyłączone (NEVER), brak limitu czasu połączenia.
     */
    static void buildingTheClient() {
        section("1. Budowa klienta (HttpClient.newBuilder)");

        HttpClient defaultClient = HttpClient.newHttpClient(); // newHttpClient = klient z ustawieniami domyślnymi
        show("domyślna wersja protokołu", defaultClient.version());
        // WYNIK: domyślna wersja protokołu → HTTP_2
        show("domyślne przekierowania", defaultClient.followRedirects());
        // WYNIK: domyślne przekierowania → NEVER
        show("domyślny limit czasu połączenia", defaultClient.connectTimeout());
        // WYNIK: domyślny limit czasu połączenia → Optional.empty

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))             // connectTimeout = limit czasu połączenia
                .followRedirects(HttpClient.Redirect.NEVER)        // followRedirects = czy iść za przekierowaniem
                .version(HttpClient.Version.HTTP_1_1)              // version = wersja protokołu
                .build();                                          // build = zbuduj
        show("ustawiona wersja", client.version());
        // WYNIK: ustawiona wersja → HTTP_1_1
        show("ustawiony limit czasu połączenia", client.connectTimeout());
        // WYNIK: ustawiony limit czasu połączenia → Optional[PT2S]

        // PUŁAPKA: domyślnie NIE ma limitu czasu połączenia. Jeśli adres nie odpowiada, klient czeka bardzo długo.
        // DOBRA PRAKTYKA: zawsze ustawiaj connectTimeout przy budowie klienta oraz timeout przy każdym żądaniu.
        // Dlaczego: awarie sieci są „ciche” — bez limitów aplikacja wisi zamiast zgłosić problem.

        // HttpClient w Javie 17 nie ma metody close() — jego wątki pomocnicze nie blokują zakończenia programu.
        // (Java 21+: HttpClient jest AutoCloseable i można go zamykać w try-with-resources.)
    }

    // =================================================================================================
    // 2. PROSTE GET
    // =================================================================================================

    /** Klient używany w dalszych sekcjach: HTTP/1.1 (nasz serwer nie mówi HTTP/2), limit połączenia 2 s. */
    static HttpClient newClient(HttpClient.Redirect redirect) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .followRedirects(redirect)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * 2. GET: budujemy żądanie, wysyłamy metodą send (blokuje wątek do końca odpowiedzi),
     * odbieramy HttpResponse. BodyHandlers.ofString(UTF_8) zamienia bajty odpowiedzi na tekst.
     */
    static void simpleGet(TestServer server) throws Exception {
        section("2. Proste GET");

        HttpClient client = newClient(HttpClient.Redirect.NEVER);
        HttpRequest request = HttpRequest.newBuilder(server.uri("/hello")) // newBuilder(uri) = zacznij budować żądanie
                .GET()                                                     // GET = metoda GET (to też domyślna)
                .timeout(Duration.ofSeconds(2))                            // limit czasu całego żądania
                .header("Accept", "text/plain")                            // header = nagłówek
                .build();
        show("metoda żądania", request.method()); // method = metoda
        // WYNIK: metoda żądania → GET
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(UTF_8)); // send = wyślij
        show("kod statusu", response.statusCode()); // statusCode = kod statusu
        // WYNIK: kod statusu → 200
        show("treść odpowiedzi", response.body()); // body = treść
        // WYNIK: treść odpowiedzi → Cześć z serwera!
        show("Content-Type", response.headers().firstValue("Content-Type").orElse("brak")); // firstValue = pierwsza wartość
        // WYNIK: Content-Type → text/plain; charset=utf-8
        show("nagłówek własny X-Wersja-Api", response.headers().firstValue("x-wersja-api").orElse("brak"));
        // WYNIK: nagłówek własny X-Wersja-Api → 1
        show("wersja protokołu odpowiedzi", response.version());
        // WYNIK: wersja protokołu odpowiedzi → HTTP_1_1

        // Nazwy nagłówków w HttpHeaders są niewrażliwe na wielkość liter (stąd działa „x-wersja-api”).
        // orElse = „albo wartość zastępcza” — firstValue zwraca Optional, bo nagłówka może nie być.

        // PUŁAPKA: BodyHandlers.ofString() bez argumentu bierze kodowanie z nagłówka Content-Type odpowiedzi,
        // a gdy go brak — UTF-8. Serwer, który zapomni o charset, a wyśle np. Windows-1250, da krzaczki.
        // DOBRA PRAKTYKA: w kodzie klienta podawaj kodowanie jawnie tam, gdzie je znasz, a serwerom każ ustawiać
        // „Content-Type: ...; charset=utf-8”. Dlaczego: polskie litery zależą od zgodności obu stron.
    }

    // =================================================================================================
    // 3. POST Z TREŚCIĄ I NAGŁÓWKAMI
    // =================================================================================================

    /**
     * 3. POST wysyła treść. BodyPublishers.ofString(tekst, UTF_8) zamienia tekst na bajty w podanym
     * kodowaniu — a nagłówek Content-Type informuje serwer, co dostaje.
     */
    static void postWithHeaders(TestServer server) throws Exception {
        section("3. POST z treścią i własnymi nagłówkami");

        HttpClient client = newClient(HttpClient.Redirect.NEVER);
        HttpRequest post = HttpRequest.newBuilder(server.uri("/echo"))
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "text/plain; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString("Zażółć gęślą jaźń", UTF_8)) // BodyPublishers = nadawcy treści
                .build();
        HttpResponse<String> response = client.send(post, HttpResponse.BodyHandlers.ofString(UTF_8));
        show("odpowiedź na POST", response.body());
        // WYNIK: odpowiedź na POST → metoda=POST; treść=Zażółć gęślą jaźń

        // Inne metody: PUT(...), DELETE(), method("PATCH", publisher) — PATCH nie ma osobnego skrótu.
        HttpRequest patch = HttpRequest.newBuilder(server.uri("/echo"))
                .timeout(Duration.ofSeconds(2))
                .method("PATCH", HttpRequest.BodyPublishers.ofString("zmiana", UTF_8)) // method = dowolna metoda
                .build();
        show("PATCH przez method()", client.send(patch, HttpResponse.BodyHandlers.ofString(UTF_8)).body());
        // WYNIK: PATCH przez method() → metoda=PATCH; treść=zmiana
        HttpRequest delete = HttpRequest.newBuilder(server.uri("/echo")).timeout(Duration.ofSeconds(2)).DELETE().build();
        show("DELETE", client.send(delete, HttpResponse.BodyHandlers.ofString(UTF_8)).body());
        // WYNIK: DELETE → metoda=DELETE; treść=

        // Nagłówki: serwer odczytuje je po nazwie.
        HttpRequest withHeaders = HttpRequest.newBuilder(server.uri("/naglowki"))
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "application/json")
                .header("X-Klient", "lekcja-http02")
                .POST(HttpRequest.BodyPublishers.noBody()) // noBody = pusta treść
                .build();
        show("co serwer widział", client.send(withHeaders, HttpResponse.BodyHandlers.ofString(UTF_8)).body());
        // WYNIK: co serwer widział → typ=application/json; X-Klient=lekcja-http02

        // Żądania HttpRequest są niezmienne (immutable) i można je wysyłać wielokrotnie.

        // PUŁAPKA: BodyPublishers.ofString(tekst) bez kodowania używa UTF-8, ale nagłówek Content-Type i tak
        // musisz ustawić sam — klient nie zgaduje typu treści. Bez niego serwer (np. Spring) może odrzucić
        // żądanie kodem 415 (Unsupported Media Type) albo źle zinterpretować dane.
        // DOBRA PRAKTYKA: przy każdym żądaniu z treścią ustawiaj Content-Type, a przy odpowiedziach
        // JSON — nagłówek Accept. Dlaczego: to „umowa” o formacie między klientem a serwerem.
    }

    // =================================================================================================
    // 4. SPOSOBY CZYTANIA TREŚCI
    // =================================================================================================

    /**
     * 4. BodyHandler decyduje, w co zamienić treść odpowiedzi. Najczęstsze: ofString (tekst),
     * ofLines (strumień linii), ofByteArray (bajty, np. plik), discarding (interesuje nas tylko status).
     */
    static void bodyHandlers(TestServer server) throws Exception {
        section("4. BodyHandlers: ofString, ofLines, ofByteArray, discarding");

        HttpClient client = newClient(HttpClient.Redirect.NEVER);
        HttpRequest hello = HttpRequest.newBuilder(server.uri("/hello")).timeout(Duration.ofSeconds(2)).build();
        HttpRequest lines = HttpRequest.newBuilder(server.uri("/linie")).timeout(Duration.ofSeconds(2)).build();

        HttpResponse<byte[]> bytes = client.send(hello, HttpResponse.BodyHandlers.ofByteArray()); // bajty
        show("liczba znaków w treści", new String(bytes.body(), UTF_8).length());
        // WYNIK: liczba znaków w treści → 16
        show("liczba bajtów w treści", bytes.body().length);
        // WYNIK: liczba bajtów w treści → 18

        HttpResponse<Stream<String>> linesResponse = client.send(lines, HttpResponse.BodyHandlers.ofLines()); // ofLines
        try (Stream<String> stream = linesResponse.body()) { // strumień linii zamykamy po użyciu
            show("linie wielkimi literami", stream.map(String::toUpperCase).toList()); // toList = do listy (Java 16+)
            // WYNIK: linie wielkimi literami → [PIERWSZA, DRUGA, TRZECIA]
        }

        HttpResponse<Void> discarded = client.send(hello, HttpResponse.BodyHandlers.discarding()); // Void = „nic”
        show("status przy discarding", discarded.statusCode());
        // WYNIK: status przy discarding → 200
        show("treść przy discarding", discarded.body());
        // WYNIK: treść przy discarding → null

        // PUŁAPKA: Content-Length w nagłówku odpowiedzi liczy BAJTY (tu 18), a String.length() — znaki (16).
        // Polskie litery zajmują w UTF-8 po 2 bajty, więc liczby się różnią. Nigdy nie przycinaj ani nie
        // porównuj długości treści w znakach z Content-Length.
        show("Content-Length", hello(client, hello).headers().firstValue("Content-Length").orElse("brak"));
        // WYNIK: Content-Length → 18

        // DOBRA PRAKTYKA: do dużych odpowiedzi (pliki) użyj BodyHandlers.ofFile(ścieżka) albo ofInputStream —
        // ofString/ofByteArray trzymają całość w pamięci. Dlaczego: 2 GB pliku nie zmieści się w String.
    }

    private static HttpResponse<String> hello(HttpClient client, HttpRequest request) throws Exception {
        return client.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
    }

    // =================================================================================================
    // 5. BŁĘDY TO NIE WYJĄTKI
    // =================================================================================================

    /** Pomocnik: sukces = kod 2xx; w przeciwnym razie rzuca wyjątek z kodem w komunikacie. */
    static String bodyOrThrow(HttpResponse<String> response) {
        int code = response.statusCode();
        if (code < 200 || code > 299) {
            throw new IllegalStateException("serwer odpowiedział kodem " + code);
        }
        return response.body();
    }

    /**
     * 5. Kody 4xx i 5xx NIE rzucają wyjątku — client.send(...) kończy się normalnie, a Ty dostajesz
     * odpowiedź z kodem 404 albo 500. Wyjątki oznaczają awarię przesyłu (brak połączenia, przekroczony czas).
     */
    static void errorsAreNotExceptions(TestServer server) throws Exception {
        section("5. Kody 404 i 500 to NIE wyjątki");

        HttpClient client = newClient(HttpClient.Redirect.NEVER);
        for (String path : List.of("/hello", "/brak", "/awaria")) {
            HttpRequest request = HttpRequest.newBuilder(server.uri(path)).timeout(Duration.ofSeconds(2)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
            show(path, response.statusCode() + " → " + response.body());
        }
        // WYNIK: /hello → 200 → Cześć z serwera!
        // WYNIK: /brak → 404 → Nie ma takiej strony
        // WYNIK: /awaria → 500 → Awaria serwera

        // PUŁAPKA: kod, który bierze response.body() bez sprawdzenia statusu, potraktuje treść błędu
        // („Nie ma takiej strony”) jak prawdziwe dane. Błąd wychodzi dużo później i w zupełnie innym miejscu.
        HttpRequest missing = HttpRequest.newBuilder(server.uri("/brak")).timeout(Duration.ofSeconds(2)).build();
        HttpResponse<String> notFound = client.send(missing, HttpResponse.BodyHandlers.ofString(UTF_8));
        expectThrows("bodyOrThrow dla 404", () -> bodyOrThrow(notFound));
        // WYNIK: ✔ bodyOrThrow dla 404 → rzucono IllegalStateException: serwer odpowiedział kodem 404

        // DOBRA PRAKTYKA: po każdym send sprawdź statusCode() (najlepiej w jednej metodzie pomocniczej) —
        // zamień nieoczekiwane kody na własny wyjątek. Dlaczego: kontrolowany błąd w jednym miejscu jest
        // lepszy niż „nulle” i dziwne dane w dalszych krokach programu.
    }

    // =================================================================================================
    // 6. PRZEKIEROWANIA
    // =================================================================================================

    /**
     * 6. Odpowiedź 3xx z nagłówkiem Location mówi „zasób jest gdzie indziej”. Klient z polityką NEVER
     * oddaje Ci taką odpowiedź; z NORMAL sam idzie pod nowy adres (maksymalnie 5 razy).
     */
    static void redirects(TestServer server) throws Exception {
        section("6. Przekierowania (302 i Location)");

        HttpRequest request = HttpRequest.newBuilder(server.uri("/stary")).timeout(Duration.ofSeconds(2)).build();

        HttpResponse<String> notFollowed = newClient(HttpClient.Redirect.NEVER)
                .send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
        show("NEVER: kod", notFollowed.statusCode());
        // WYNIK: NEVER: kod → 302
        show("NEVER: Location", notFollowed.headers().firstValue("Location").orElse("brak"));
        // WYNIK: NEVER: Location → /hello

        HttpResponse<String> followed = newClient(HttpClient.Redirect.NORMAL)
                .send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
        show("NORMAL: kod", followed.statusCode());
        // WYNIK: NORMAL: kod → 200
        show("NORMAL: treść", followed.body());
        // WYNIK: NORMAL: treść → Cześć z serwera!
        show("NORMAL: końcowa ścieżka", followed.uri().getPath()); // uri = adres, z którego przyszła odpowiedź
        // WYNIK: NORMAL: końcowa ścieżka → /hello

        // Polityki: NEVER (nie idź), NORMAL (idź, ale nie z https na http), ALWAYS (idź zawsze, także z https na http).

        // PUŁAPKA: z domyślnym klientem (NEVER) odpowiedź 302 przychodzi „po cichu” z pustą treścią — kod, który
        // sprawdza tylko body(), „nic nie widzi”. Ustaw followRedirects(NORMAL), jeśli chcesz podążać za przekierowaniem.
        // DOBRA PRAKTYKA: nie używaj ALWAYS, chyba że świadomie akceptujesz zejście z https na http.
        // Dlaczego: przekierowanie na http wysyła dane (np. tokeny) otwartym tekstem, a atakujący może to wymusić.
    }

    // =================================================================================================
    // 7. JEDEN KLIENT DLA WIELU ŻĄDAŃ
    // =================================================================================================

    /**
     * 7. HttpClient jest bezpieczny wątkowo i utrzymuje pulę połączeń — buduj JEDEN i używaj wszędzie.
     */
    static void sharedClient(TestServer server) throws Exception {
        section("7. Jeden klient, wiele żądań (także z wielu wątków)");

        HttpClient shared = newClient(HttpClient.Redirect.NEVER);
        ExecutorService threads = Executors.newFixedThreadPool(4);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                int number = i;
                Callable<Integer> task = () -> {
                    HttpRequest request = HttpRequest.newBuilder(server.uri("/hello?n=" + number))
                            .timeout(Duration.ofSeconds(2)).build();
                    return shared.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                };
                results.add(threads.submit(task));
            }
            List<Integer> codes = new ArrayList<>();
            for (Future<Integer> future : results) {
                codes.add(future.get(5, TimeUnit.SECONDS));
            }
            show("kody z ośmiu równoległych żądań", codes);
            // WYNIK: kody z ośmiu równoległych żądań → [200, 200, 200, 200, 200, 200, 200, 200]
        } finally {
            threads.shutdownNow();
        }

        // PUŁAPKA: tworzenie nowego HttpClient dla każdego żądania. Każdy klient ma własną pulę połączeń i
        // wątki pomocnicze, więc za każdym razem nawiązujesz połączenie od zera (wolniej) i marnujesz zasoby.
        // DOBRA PRAKTYKA: jeden klient na aplikację (pole statyczne albo bean w Springu), żądania buduj osobno.
        // Dlaczego: klient jest niezmienny i bezpieczny wątkowo właśnie po to, żeby go współdzielić.
    }

    // =================================================================================================
    // 8. LIMITY CZASU
    // =================================================================================================

    /**
     * 8. Dwa rodzaje limitów. connectTimeout (klient) — ile czekać na nawiązanie połączenia.
     * timeout (żądanie) — ile czekać na odpowiedź. Po przekroczeniu leci HttpTimeoutException.
     * Wolną odpowiedź symulujemy zatrzaskiem (nie długim sleep), więc wynik jest powtarzalny.
     */
    static void timeouts(TestServer server) throws Exception {
        section("8. Limity czasu: HttpTimeoutException");

        HttpClient client = newClient(HttpClient.Redirect.NEVER);
        HttpRequest slow = HttpRequest.newBuilder(server.uri("/wolny")).timeout(Duration.ofMillis(300)).build();
        try {
            client.send(slow, HttpResponse.BodyHandlers.ofString(UTF_8));
            note("odpowiedź nadeszła (nie powinna)");
        } catch (HttpTimeoutException e) { // wyjątek: przekroczono limit czasu żądania
            show("typ wyjątku", e.getClass().getSimpleName());
        } finally {
            server.releaseSlow(); // otwieramy zatrzask — wątek serwera kończy pracę
        }
        // WYNIK: typ wyjątku → HttpTimeoutException

        // Po otwarciu zatrzasku ten sam adres odpowiada od razu:
        HttpRequest now = HttpRequest.newBuilder(server.uri("/wolny")).timeout(Duration.ofSeconds(2)).build();
        show("po otwarciu zatrzasku", client.send(now, HttpResponse.BodyHandlers.ofString(UTF_8)).body());
        // WYNIK: po otwarciu zatrzasku → spóźniona odpowiedź

        // Zamknięty port: nikt nie słucha. Nie ma limitu czasu — jest natychmiastowe odrzucenie (ConnectException).
        int closed;
        try (ServerSocket temporary = new ServerSocket(0, 1, LOOPBACK)) {
            closed = temporary.getLocalPort();
        }
        HttpClient patient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1).build(); // 5 s: na niektórych systemach odmowa trwa dłużej
        try {
            HttpRequest request = HttpRequest.newBuilder(
                    new URI("http", null, LOOPBACK.getHostAddress(), closed, "/", null, null)).build();
            patient.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
            note("połączono (nie powinno się udać)");
        } catch (IOException e) {
            show("zamknięty port, typ wyjątku", e.getClass().getSimpleName());
        }
        // WYNIK: zamknięty port, typ wyjątku → ConnectException

        // PUŁAPKA: HttpTimeoutException to coś innego niż kod 504 (Gateway Timeout) od serwera: wyjątek powstaje
        // PO STRONIE KLIENTA, gdy minie jego limit, a kod 504 to normalna odpowiedź. Pierwsza sytuacja nie
        // mówi, czy serwer żądanie wykonał — dlatego przy ponawianiu liczy się idempotentność metody.
        // DOBRA PRAKTYKA: timeout dobieraj do operacji (sekundy dla zwykłych zapytań, więcej dla raportów).
        // Dlaczego: za krótki limit daje fałszywe błędy, a za długi blokuje wątki i kolejki użytkowników.
        // Komunikaty tych wyjątków zależą od systemu — w kodzie reaguj na typ, nie na tekst.
    }

    // =================================================================================================
    // 9. CO ZOBACZYŁ SERWER
    // =================================================================================================

    /**
     * 9. Dziennik serwera pokazuje, jakie żądania faktycznie dotarły — wygodne do sprawdzania testów.
     * Liczymy tylko wybrane wpisy (dziennik jest wspólny dla wątków, więc kolejność nie jest gwarantowana).
     */
    static void serverLog(TestServer server) {
        section("9. Dziennik serwera");

        List<String> log = server.log();
        long hellos = log.stream().filter(line -> line.startsWith("GET /hello")).count();
        show("wszystkich żądań GET /hello…", hellos);
        // WYNIK: wszystkich żądań GET /hello… → 14
        show("żądań DELETE /echo", log.stream().filter(line -> line.equals("DELETE /echo")).count());
        // WYNIK: żądań DELETE /echo → 1
        show("czy był PATCH /echo?", log.contains("PATCH /echo"));
        // WYNIK: czy był PATCH /echo? → true

        // DOBRA PRAKTYKA: w testach klienta zbieraj żądania po stronie serwera i sprawdzaj je po zakończeniu
        // wywołań — nie wypisuj ich „na żywo” z wątków obsługi. Dlaczego: kolejność wypisów z wielu wątków
        // jest przypadkowa, a test ma dawać ten sam wynik za każdym razem.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • HttpClient.newBuilder().connectTimeout(...).followRedirects(...).version(...).build() — raz, współdzielony.
     *   • Żądanie: HttpRequest.newBuilder(uri).header(...).timeout(...).GET()/POST(publisher)/DELETE()/method(...).build().
     *   • Wysyłka: client.send(request, BodyHandlers.ofString(UTF_8) | ofLines() | ofByteArray() | discarding()).
     *   • response.statusCode(), response.headers().firstValue("..."), response.body().
     *   • 404 i 500 NIE rzucają wyjątku — zawsze sprawdzaj kod statusu.
     *   • Domyślnie: HTTP/2, przekierowania NEVER, brak limitu czasu połączenia (ustaw go!).
     *   • Limity: connectTimeout (klient) i timeout (żądanie → HttpTimeoutException); zamknięty port → ConnectException.
     *   • Treść żądania: BodyPublishers.ofString(tekst, UTF_8); ustaw Content-Type; PATCH przez method("PATCH", ...).
     *   • Długość treści w nagłówku to bajty, nie znaki.
     *
     * PYTANIA KONTROLNE:
     *   1. Co zwróci client.send dla odpowiedzi 404 — wyjątek czy odpowiedź? Skąd o tym wiesz w kodzie?
     *   2. Czym różni się connectTimeout od timeout żądania? Który wyjątek leci po przekroczeniu drugiego?
     *   3. Co wypisze:  HttpClient.newHttpClient().followRedirects();  ?
     *   4. ZNAJDŹ BŁĄD:  for (String id : ids) {
     *                        HttpClient client = HttpClient.newHttpClient();
     *                        String body = client.send(request(id), BodyHandlers.ofString()).body();
     *                    }   (wskaż dwa problemy)
     *   5. Dlaczego żądanie 302 dla klienta domyślnego kończy się „pustą” treścią?
     *   6. Co wypisze:  client.send(getForMissingPage, BodyHandlers.discarding()).body();  ?
     *   7. Dlaczego PUT można bezpiecznie ponowić po HttpTimeoutException, a POST — niekoniecznie?
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

    static void exercises() throws IOException {
        try (TestServer server = new TestServer()) {
            HttpClient client = newClient(HttpClient.Redirect.NEVER);

            section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
            Check.equal("ćw. 1: status /hello", 200, () -> call(() -> exercise1(client, server.uri("/hello"))));
            Check.equal("ćw. 1: status /brak", 404, () -> call(() -> exercise1(client, server.uri("/brak"))));
            Check.equal("ćw. 2: POST", "metoda=POST; treść=Cześć", () -> call(() -> exercise2(client, server.uri("/echo"), "Cześć")));
            Check.equal("ćw. 3: treść 200", "Cześć z serwera!", () -> call(() -> exercise3(client, server.uri("/hello"))));
            Check.throwsException("ćw. 3: 404 → wyjątek", IllegalStateException.class,
                    () -> exercise3(client, server.uri("/brak")));
            Check.equal("ćw. 4: Location", "/hello", () -> call(() -> exercise4(server.uri("/stary"))));
            Check.summary();

            section("ĆWICZENIA — rozwiązania wzorcowe");
            Check.equal("ćw. 1 (wzorzec): /hello", 200, () -> call(() -> solution1(client, server.uri("/hello"))));
            Check.equal("ćw. 1 (wzorzec): /brak", 404, () -> call(() -> solution1(client, server.uri("/brak"))));
            Check.equal("ćw. 2 (wzorzec)", "metoda=POST; treść=Cześć", () -> call(() -> solution2(client, server.uri("/echo"), "Cześć")));
            Check.equal("ćw. 3 (wzorzec): 200", "Cześć z serwera!", () -> call(() -> solution3(client, server.uri("/hello"))));
            Check.throwsException("ćw. 3 (wzorzec): 404 → wyjątek", IllegalStateException.class,
                    () -> solution3(client, server.uri("/brak")));
            Check.equal("ćw. 4 (wzorzec): Location", "/hello", () -> call(() -> solution4(server.uri("/stary"))));
            Check.summary();
            // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
        }
    }

    /**
     * ĆWICZENIE 1 (łatwe): wyślij GET pod podany adres i zwróć kod statusu odpowiedzi (nie rzucaj wyjątku dla 404).
     * Podpowiedź: BodyHandlers.discarding() wystarczy, jeśli treść Cię nie interesuje.
     */
    static int exercise1(HttpClient client, URI uri) throws Exception {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): wyślij POST z treścią {@code text} (UTF-8, Content-Type text/plain; charset=utf-8,
     * limit czasu 2 s) i zwróć treść odpowiedzi jako tekst.
     * Podpowiedź: BodyPublishers.ofString(text, UTF_8), BodyHandlers.ofString(UTF_8).
     */
    static String exercise2(HttpClient client, URI uri, String text) throws Exception {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): wyślij GET i zwróć treść, ale TYLKO dla kodów 2xx — w przeciwnym razie rzuć
     * IllegalStateException (z kodem w komunikacie).
     * PRZEPISZ (stary sposób bez sprawdzania statusu):
     * <pre>{@code
     * String body = client.send(request, BodyHandlers.ofString()).body();   // dla 404 też „zadziała”
     * return body;
     * }</pre>
     * Podpowiedź: zachowaj całą odpowiedź w zmiennej, sprawdź statusCode(), dopiero potem weź body().
     */
    static String exercise3(HttpClient client, URI uri) throws Exception {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zbuduj WŁASNEGO klienta (HTTP/1.1, connectTimeout 2 s, przekierowania NEVER),
     * wyślij GET pod adres i zwróć wartość nagłówka Location. Gdy nagłówka nie ma — rzuć IllegalStateException.
     * Podpowiedź: response.headers().firstValue("Location").orElseThrow(...) — orElseThrow = albo rzuć wyjątek.
     */
    static String exercise4(URI uri) throws Exception {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(HttpClient client, URI uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(2)).build();
        return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    static String solution2(HttpClient client, URI uri, String text) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "text/plain; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(text, UTF_8))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(UTF_8)).body();
    }

    static String solution3(HttpClient client, URI uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(2)).build();
        return bodyOrThrow(client.send(request, HttpResponse.BodyHandlers.ofString(UTF_8)));
    }

    static String solution4(URI uri) throws Exception {
        HttpClient own = newClient(HttpClient.Redirect.NEVER);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(2)).build();
        HttpResponse<Void> response = own.send(request, HttpResponse.BodyHandlers.discarding());
        return response.headers().firstValue("Location")
                .orElseThrow(() -> new IllegalStateException("brak nagłówka Location"));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Zwróci odpowiedź (HttpResponse) z kodem 404 — wyjątku nie ma. Wiesz o tym, bo trzeba sprawdzić
     *      response.statusCode(); wyjątki oznaczają awarię przesyłu (połączenie, limit czasu).
     *   2. connectTimeout (ustawiany w kliencie) ogranicza czas nawiązania połączenia; timeout żądania
     *      (HttpRequest.Builder.timeout) — czas oczekiwania na odpowiedź. Przekroczenie drugiego daje
     *      HttpTimeoutException (przekroczenie pierwszego — HttpConnectTimeoutException, jego podklasę).
     *   3. NEVER (domyślna polityka przekierowań).
     *   4. (a) nowy HttpClient w każdym obrocie pętli — marnuje połączenia i wątki (jeden klient wystarczy);
     *      (b) brak sprawdzenia statusCode() przed użyciem treści. (Dodatkowo: brak limitów czasu.)
     *   5. Domyślny klient nie podąża za przekierowaniami (NEVER), więc dostaje samą odpowiedź 302 z nagłówkiem
     *      Location i bez treści. Trzeba ustawić followRedirects(NORMAL).
     *   6. null — przy discarding() treść jest odrzucana, a typ odpowiedzi to HttpResponse<Void>.
     *   7. PUT jest idempotentny: drugie wywołanie z tą samą treścią zostawia zasób w tym samym stanie.
     *      POST zwykle tworzy nowy zasób, więc ponowienie może zdublować efekt (klient nie wie, czy pierwsze
     *      żądanie zdążyło dotrzeć do serwera).
     */
    // </editor-fold>
}
