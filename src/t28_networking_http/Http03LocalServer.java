package t28_networking_http;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import helpers.Check;
import helpers.SampleData;
import helpers.model.Product;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Własny serwer HTTP — com.sun.net.httpserver.HttpServer
 *        (server = serwer; handler = obsługa żądania; context = kontekst, czyli „ścieżka z przypisaną obsługą”;
 *         exchange = wymiana żądanie-odpowiedź; filter = filtr; route = trasa)
 *
 * W SKRÓCIE:
 *   JDK ma wbudowany, bardzo prosty serwer HTTP. Rejestrujesz „konteksty” (ścieżki) i dla każdego piszesz
 *   handler: odczytujesz żądanie (metoda, ścieżka, zapytanie, nagłówki, treść) i piszesz odpowiedź (kod,
 *   nagłówki, treść). To idealne narzędzie do nauki, testów klientów i małych narzędzi — i dobry sposób,
 *   żeby zrozumieć, co robią za Ciebie Tomcat i Spring.
 *
 * ANALOGIA: recepcja w biurowcu. Serwer to recepcjonista, a konteksty to tabliczki „dział kadr — piętro 2”,
 *   „księgowość — piętro 3”. Gość (żądanie) mówi, czego chce; recepcjonista patrzy na tabliczki (ścieżki)
 *   i kieruje do właściwego pracownika (handler). Filtr to ochroniarz przy wejściu: robi coś z każdym gościem,
 *   zanim ten dojdzie do pracownika. Gdy pracownik zachoruje (wyjątek), recepcjonista musi ładnie przeprosić (500).
 *
 * JAK TO DZIAŁA:
 *   HttpServer server = HttpServer.create(new InetSocketAddress(loopback, 0), 0);   // port 0, kolejka domyślna
 *   server.createContext("/produkty", handler);   // dopasowanie po PREFIKSIE ścieżki
 *   server.setExecutor(pula);                      // kto wykonuje handlery
 *   server.start();  ...  server.stop(0);          // stop(0) = zatrzymaj od razu
 *
 *   w handlerze:  exchange.getRequestMethod() / getRequestURI() / getRequestHeaders() / getRequestBody()
 *                 exchange.getResponseHeaders().set(...);
 *                 exchange.sendResponseHeaders(kod, długośćTreściWBajtach);   // -1 = bez treści, 0 = dowolna długość
 *                 exchange.getResponseBody().write(bajty);  ...close()
 *
 *   Nazwa „com.sun.net.httpserver” jest historyczna: to wspierane, udokumentowane API JDK (moduł jdk.httpserver),
 *   dostępne w Javie 17. Od Javy 18 JDK ma też gotowe narzędzie wiersza poleceń jwebserver (serwer plików
 *   statycznych). Aplikacje Spring Boot używają w środku pełnego serwera (domyślnie Tomcat) — patrz SpringLearning.
 *
 * SŁÓWKA: handler = obsługa; context = kontekst; filter = filtr; chain = łańcuch (kolejne kroki);
 *   route = trasa (dopasowanie żądania do kodu); resource = zasób; list = lista; create = utwórz; delete = usuń;
 *   chunked = porcjowany (długość odpowiedzi nieznana z góry); executor = wykonawca (pula wątków);
 *   access log = dziennik dostępu (kto, co i z jakim wynikiem)
 *
 * ZOBACZ TEŻ: t28_networking_http/Http02HttpClient (klient dla tego serwera), t28_networking_http/Http04JsonApi
 *   (JSON w API), t21_concurrency/Concurrency04Executors (pule wątków), t34_toward_spring/Spring03RestConcepts
 * </pre>
 */
public class Http03LocalServer {

    private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress(); // loopback = pętla zwrotna
    private static final java.nio.charset.Charset UTF_8 = StandardCharsets.UTF_8;

    public static void main(String[] args) throws Exception {
        title("Http03 — własny serwer HTTP");

        smallestServer();        // smallest server = najmniejszy serwer
        try (ShopServer shop = new ShopServer()) {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                    .version(HttpClient.Version.HTTP_1_1).build();
            readingTheRequest(shop, client);  // reading the request = odczyt żądania
            writingTheResponse(shop, client); // writing the response = zapis odpowiedzi
            routing(shop, client);            // routing = kierowanie żądań
            restResource(shop, client);       // rest resource = zasób w stylu REST
            executorAndThreads(shop, client); // executor and threads = pula i wątki
            filtersAndAccessLog(shop, client); // filters and access log = filtry i dziennik
            handlerFailures(shop, client);    // handler failures = awarie w handlerze
            stoppingTheServer(shop, client);  // stopping the server = zatrzymanie serwera
        }
        exercises();                          // exercises = ćwiczenia
    }

    // =================================================================================================
    // POMOCNIKI: serwer sklepu i klient
    // =================================================================================================

    /** Pozycja w „sklepie”: SKU (kod), nazwa i cena. Rekord = niezmienny nośnik danych (Java 16+). */
    record Item(String sku, String name, BigDecimal price) {
        String toLine() { // toLine = zamień na linię tekstu
            return sku + ";" + name + ";" + price.toPlainString();
        }
    }

    /** Odpowiedź w wygodnej postaci: kod, treść, nagłówki. */
    record Reply(int status, String body, HttpHeaders headers) {
        String header(String name) { // header = nagłówek; brak → „brak”
            return headers.firstValue(name).orElse("brak");
        }
    }

    /** Wysyła żądanie dowolną metodą (body == null → bez treści). */
    static Reply send(HttpClient client, URI base, String method, String path, String body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, UTF_8);
        HttpRequest request = HttpRequest.newBuilder(base.resolve(path))
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "text/plain; charset=utf-8")
                .method(method, publisher)
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
        return new Reply(response.statusCode(), response.body(), response.headers());
    }

    /** Zapisuje odpowiedź tekstową: długość liczona w BAJTACH. */
    static void writeText(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    /**
     * Serwer „sklepu”: kilka adresów pokazujących różne techniki + zasób /produkty. Wpis do dziennika
     * dostępu robimy PRZED wysłaniem treści — wtedy klient, który dostał odpowiedź, na pewno widzi wpis.
     */
    static final class ShopServer implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor = Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "sklep-handler"); // nazwa wątku widoczna w sekcji 6
            thread.setDaemon(true);
            return thread;
        });
        private final Map<String, Item> items = new ConcurrentSkipListMap<>(); // posortowana mapa bezpieczna wątkowo
        private final List<String> accessLog = Collections.synchronizedList(new ArrayList<>());
        private final AtomicBoolean closed = new AtomicBoolean(); // AtomicBoolean = flaga bezpieczna wątkowo

        ShopServer() throws IOException {
            for (Product product : SampleData.products()) { // dane początkowe z klasy pomocniczej kursu
                items.put(product.sku(), new Item(product.sku(), product.name(), product.price()));
            }
            server = HttpServer.create(new InetSocketAddress(LOOPBACK, 0), 0);

            HttpContext info = server.createContext("/info", safe(this::handleInfo)); // HttpContext = kontekst (ścieżka)
            HttpContext length = server.createContext("/dlugosc", safe(exchange ->
                    respond(exchange, 200, "Zażółć gęślą jaźń")));
            HttpContext chunked = server.createContext("/porcje", safe(exchange -> {
                logAccess(exchange, 200);
                exchange.sendResponseHeaders(200, 0); // 0 = długość nieznana, odpowiedź będzie porcjowana (chunked)
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write("po".getBytes(UTF_8));
                    out.write("rcje".getBytes(UTF_8));
                }
            }));
            HttpContext empty = server.createContext("/pusty", safe(exchange -> {
                logAccess(exchange, 200);
                exchange.sendResponseHeaders(200, -1); // -1 = brak treści
                exchange.close();
            }));
            HttpContext products = server.createContext("/produkty", safe(this::handleProducts));
            HttpContext thread = server.createContext("/watek", safe(exchange ->
                    respond(exchange, 200, Thread.currentThread().getName())));
            HttpContext boom = server.createContext("/bomba", safe(exchange -> {
                throw new IllegalStateException("celowa awaria");
            }));
            // bez osłony: wyjątek wycieknie z handlera (sekcja 8)
            server.createContext("/bomba-bez-oslony", exchange -> {
                throw new IllegalStateException("celowa awaria bez osłony");
            });

            Filter addHeader = new Filter() { // Filter = filtr wykonywany przed handlerem i po nim
                @Override
                public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
                    exchange.getResponseHeaders().set("X-Serwer", "lekcja-http03"); // przed handlerem
                    chain.doFilter(exchange); // doFilter = „przekaż dalej”: kolejny filtr albo handler
                }

                @Override
                public String description() { // description = opis filtra
                    return "dodaje nagłówek X-Serwer";
                }
            };
            for (HttpContext context : List.of(info, length, chunked, empty, products, thread, boom)) {
                context.getFilters().add(addHeader); // getFilters = lista filtrów kontekstu
            }
            server.setExecutor(executor); // setExecutor = ustaw pulę wykonującą handlery
            server.start();
        }

        /** Osłona na wyjątki: awaria handlera → 500 zamiast zerwanego połączenia. */
        private HttpHandler safe(HttpHandler handler) {
            return exchange -> {
                try {
                    handler.handle(exchange);
                } catch (RuntimeException e) {
                    respond(exchange, 500, "Błąd wewnętrzny serwera");
                }
            };
        }

        private void logAccess(HttpExchange exchange, int status) {
            accessLog.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath() + " → " + status);
        }

        private void respond(HttpExchange exchange, int status, String text) throws IOException {
            logAccess(exchange, status);
            writeText(exchange, status, text);
        }

        private void respondEmpty(HttpExchange exchange, int status) throws IOException {
            logAccess(exchange, status);
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        }

        private void handleInfo(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String query = uri.getRawQuery() == null ? "(brak)" : uri.getRawQuery();
            String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
            String xTest = exchange.getRequestHeaders().getFirst("X-Test"); // getFirst = pierwsza wartość nagłówka
            respond(exchange, 200, "metoda=" + exchange.getRequestMethod() + "; ścieżka=" + uri.getPath()
                    + "; zapytanie=" + query + "; X-Test=" + xTest + "; treść=" + body);
        }

        private void handleProducts(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String rest = exchange.getRequestURI().getPath().substring("/produkty".length()); // "", "/", "/ELE-001"
            String sku = rest.startsWith("/") ? rest.substring(1) : rest;
            boolean collection = sku.isEmpty(); // kolekcja = cała lista (a nie pojedynczy element)

            if (method.equals("GET") && collection) {
                respond(exchange, 200, items.values().stream().map(Item::toLine).collect(Collectors.joining("\n")));
            } else if (method.equals("GET")) {
                Item item = items.get(sku);
                if (item == null) {
                    respond(exchange, 404, "Nie ma produktu " + sku);
                } else {
                    respond(exchange, 200, item.toLine());
                }
            } else if (method.equals("POST") && collection) {
                createItem(exchange);
            } else if (method.equals("DELETE") && !collection) {
                if (items.remove(sku) == null) {
                    respond(exchange, 404, "Nie ma produktu " + sku);
                } else {
                    respondEmpty(exchange, 204); // 204 No Content = sukces bez treści
                }
            } else {
                // 405 Method Not Allowed + nagłówek Allow (które metody są dozwolone) — wymaga tego specyfikacja HTTP
                exchange.getResponseHeaders().set("Allow", collection ? "GET, POST" : "GET, DELETE");
                respond(exchange, 405, "Metoda " + method + " nie jest tu dozwolona");
            }
        }

        private void createItem(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
            String[] parts = body.split(";"); // oczekujemy: SKU;nazwa;cena
            if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank()) {
                respond(exchange, 400, "Oczekiwano treści: SKU;nazwa;cena");
                return;
            }
            BigDecimal price;
            try {
                price = new BigDecimal(parts[2].trim());
            } catch (NumberFormatException e) {
                respond(exchange, 400, "Cena nie jest liczbą: " + parts[2]);
                return;
            }
            if (price.signum() < 0) { // signum = znak liczby (-1, 0, 1)
                respond(exchange, 400, "Cena nie może być ujemna");
                return;
            }
            Item item = new Item(parts[0].trim(), parts[1].trim(), price);
            if (items.putIfAbsent(item.sku(), item) != null) { // putIfAbsent = wstaw, jeśli klucza jeszcze nie ma
                respond(exchange, 409, "Produkt " + item.sku() + " już istnieje");
                return;
            }
            exchange.getResponseHeaders().set("Location", "/produkty/" + item.sku()); // gdzie znaleźć nowy zasób
            respond(exchange, 201, item.toLine());
        }

        URI baseUri() {
            try {
                return new URI("http", null, LOOPBACK.getHostAddress(), server.getAddress().getPort(), "/", null, null);
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }

        List<String> accessLog() {
            synchronized (accessLog) {
                return new ArrayList<>(accessLog);
            }
        }

        void clearAccessLog() {
            accessLog.clear();
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) { // zamykamy tylko raz
                server.stop(0);
                executor.shutdownNow();
            }
        }
    }

    // =================================================================================================
    // 1. NAJMNIEJSZY SERWER
    // =================================================================================================

    /**
     * 1. Cztery kroki: utwórz, zarejestruj kontekst, uruchom, (na końcu) zatrzymaj. Zatrzymanie robimy w finally —
     * inaczej wątki serwera mogłyby utrzymać program przy życiu.
     */
    static void smallestServer() throws Exception {
        section("1. Najmniejszy serwer");

        HttpServer server = HttpServer.create(new InetSocketAddress(LOOPBACK, 0), 0); // 0 = wolny port, 0 = domyślna kolejka
        server.createContext("/witaj", exchange -> writeText(exchange, 200, "Witaj z własnego serwera!"));
        server.start(); // start = uruchom (działa w tle, na własnym wątku)
        try {
            URI uri = new URI("http", null, LOOPBACK.getHostAddress(), server.getAddress().getPort(), "/witaj", null, null);
            HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(2)).build();
            Reply reply = send(client, uri, "GET", "/witaj", null);
            show("kod", reply.status());
            // WYNIK: kod → 200
            show("treść", reply.body());
            // WYNIK: treść → Witaj z własnego serwera!
            show("port jest większy od 0?", server.getAddress().getPort() > 0); // getAddress = adres, na którym serwer słucha
            // WYNIK: port jest większy od 0? → true
        } finally {
            server.stop(0); // stop(0) = zatrzymaj natychmiast (liczba = ile sekund czekać na trwające żądania)
        }

        // PUŁAPKA: zapomniany stop() — wątek dyspozytora serwera nie jest demonem, więc program się nie kończy.
        // Port zostaje też zajęty do końca programu.
        // DOBRA PRAKTYKA: start() i stop() w parze try/finally (albo własna klasa AutoCloseable, jak ShopServer).
        // Dlaczego: testy, które nie sprzątają, zostawiają zajęte porty i „wiszące” procesy.
    }

    // =================================================================================================
    // 2. CO WIDZI HANDLER
    // =================================================================================================

    /**
     * 2. HttpExchange to jedna wymiana: żądanie wchodzi, odpowiedź wychodzi. Poniższy handler
     * /info odsyła wszystko, co zobaczył. Kontekst „/info” dopasowuje się po PREFIKSIE, więc
     * obsłuży także /info/cokolwiek/dalej.
     */
    static void readingTheRequest(ShopServer shop, HttpClient client) throws Exception {
        section("2. Odczyt żądania: metoda, ścieżka, zapytanie, nagłówki, treść");

        shop.clearAccessLog();
        HttpRequest request = HttpRequest.newBuilder(shop.baseUri().resolve("/info/a/b?x=1&imie=Za%C5%BC%C3%B3%C5%82%C4%87"))
                .timeout(Duration.ofSeconds(2))
                .header("X-Test", "abc")
                .POST(HttpRequest.BodyPublishers.ofString("treść żądania", UTF_8))
                .build();
        show("odpowiedź", client.send(request, HttpResponse.BodyHandlers.ofString(UTF_8)).body());
        // WYNIK: odpowiedź → metoda=POST; ścieżka=/info/a/b; zapytanie=x=1&imie=Za%C5%BC%C3%B3%C5%82%C4%87; X-Test=abc; treść=treść żądania

        show("GET bez zapytania", send(client, shop.baseUri(), "GET", "/info", null).body());
        // WYNIK: GET bez zapytania → metoda=GET; ścieżka=/info; zapytanie=(brak); X-Test=null; treść=

        // Nagłówek, którego klient nie wysłał, to null (getFirst) — sprawdzaj to, zanim użyjesz wartości.

        // PUŁAPKA: getRequestURI().getQuery() zwraca zapytanie JUŻ odkodowane (patrz Http01UriUrl: nie da się
        // wtedy rozdzielić parametrów). Do parsowania bierz getRawQuery() i dekoduj wartości osobno.
        // PUŁAPKA: ciało żądania to strumień — czytasz je raz (readAllBytes) i z jawnym UTF-8.
        // DOBRA PRAKTYKA: w handlerze zawsze zakładaj, że dane od klienta mogą być błędne lub puste.
        // Dlaczego: serwer jest wystawiony na cudze żądania i musi odpowiadać kodem 4xx, a nie awarią.
    }

    // =================================================================================================
    // 3. ZAPIS ODPOWIEDZI
    // =================================================================================================

    /**
     * 3. sendResponseHeaders(kod, długość) wysyła kod i nagłówki. Długość to LICZBA BAJTÓW treści:
     * dodatnia = dokładnie tyle bajtów, -1 = brak treści, 0 = długość nieznana (porcjowanie — chunked).
     * Dopiero potem piszemy treść do getResponseBody() i zamykamy strumień.
     */
    static void writingTheResponse(ShopServer shop, HttpClient client) throws Exception {
        section("3. Zapis odpowiedzi: długość w bajtach, -1 i 0");

        String text = "Zażółć gęślą jaźń";
        show("długość w znakach", text.length());
        // WYNIK: długość w znakach → 17
        show("długość w bajtach UTF-8", text.getBytes(UTF_8).length);
        // WYNIK: długość w bajtach UTF-8 → 26

        Reply length = send(client, shop.baseUri(), "GET", "/dlugosc", null);
        show("Content-Length z serwera", length.header("Content-Length"));
        // WYNIK: Content-Length z serwera → 26
        show("treść", length.body());
        // WYNIK: treść → Zażółć gęślą jaźń
        show("Content-Type", length.header("Content-Type"));
        // WYNIK: Content-Type → text/plain; charset=utf-8

        Reply chunked = send(client, shop.baseUri(), "GET", "/porcje", null);
        show("porcjowana: treść", chunked.body());
        // WYNIK: porcjowana: treść → porcje
        show("porcjowana: Transfer-Encoding", chunked.header("Transfer-Encoding"));
        // WYNIK: porcjowana: Transfer-Encoding → chunked
        show("porcjowana: Content-Length", chunked.header("Content-Length"));
        // WYNIK: porcjowana: Content-Length → brak

        Reply empty = send(client, shop.baseUri(), "GET", "/pusty", null);
        show("pusta: kod i długość treści", empty.status() + ", " + empty.body().length());
        // WYNIK: pusta: kod i długość treści → 200, 0

        // PRZED (błąd):  exchange.sendResponseHeaders(200, tekst.length());   // znaki!
        // PO (dobrze):   byte[] bajty = tekst.getBytes(UTF_8);
        //                exchange.sendResponseHeaders(200, bajty.length);      // bajty
        // PUŁAPKA: długość w znakach jest mniejsza niż w bajtach, gdy w tekście są polskie litery (tu 17 zamiast 26).
        // Serwer obiecał 17 bajtów, a próba zapisania 26 kończy się IOException po jego stronie
        // („too many bytes to write to stream”); klient dostaje urwaną odpowiedź albo czeka na brakujące
        // bajty. Dlatego tej pomyłki nie uruchamiamy w lekcji. Zasada: najpierw getBytes(UTF_8), potem długość.
        // PUŁAPKA: brak Content-Type z charset sprawia, że klient zgaduje kodowanie (i psuje polskie litery).

        // DOBRA PRAKTYKA: zawsze zamykaj getResponseBody() (try-with-resources) — dopiero zamknięcie kończy
        // wymianę. Dlaczego: niezamknięta odpowiedź trzyma połączenie, aż klient przekroczy limit czasu.
    }

    // =================================================================================================
    // 4. KIEROWANIE PO METODZIE I ŚCIEŻCE
    // =================================================================================================

    /**
     * 4. Serwer dopasowuje kontekst po prefiksie ścieżki, a metodą HTTP musi zająć się handler.
     * Zła metoda → 405 Method Not Allowed i nagłówek Allow z listą dozwolonych metod.
     */
    static void routing(ShopServer shop, HttpClient client) throws Exception {
        section("4. Kierowanie żądań: ścieżka, metoda, 405 i Allow");

        Reply wrongMethod = send(client, shop.baseUri(), "PUT", "/produkty", "x");
        show("PUT /produkty", wrongMethod.status() + ", Allow: " + wrongMethod.header("Allow"));
        // WYNIK: PUT /produkty → 405, Allow: GET, POST
        Reply wrongOnElement = send(client, shop.baseUri(), "POST", "/produkty/ELE-001", "x");
        show("POST /produkty/ELE-001", wrongOnElement.status() + ", Allow: " + wrongOnElement.header("Allow"));
        // WYNIK: POST /produkty/ELE-001 → 405, Allow: GET, DELETE
        show("nieznana ścieżka", send(client, shop.baseUri(), "GET", "/nie-ma-takiej", null).status());
        // WYNIK: nieznana ścieżka → 404

        // Nieznana ścieżka (brak kontekstu) → serwer sam odpowiada 404. Ścieżka „/produkty/ELE-001” trafia do
        // kontekstu „/produkty” (prefiks), a rozbiór reszty ścieżki to już zadanie naszego handlera.

        // PUŁAPKA: kontekst „/produkty” dopasuje się też do „/produkty-tajne” (to prefiks tekstu, nie segmentu
        // ścieżki). Jeśli to ma znaczenie, sprawdź w handlerze dokładną ścieżkę.
        // DOBRA PRAKTYKA: dla złej metody zwracaj 405 z nagłówkiem Allow (a nie 404 ani 400). Dlaczego: klient
        // dowiaduje się, że ADRES jest dobry, tylko metoda zła — i wie, których metod użyć.
    }

    // =================================================================================================
    // 5. ZASÓB W STYLU REST
    // =================================================================================================

    /**
     * 5. REST w pigułce: adres wskazuje ZASÓB (rzeczownik: /produkty/ELE-001), a metoda mówi, co z nim zrobić.
     * GET lista, GET element, POST utwórz (201 + Location), DELETE usuń (204). Treści są tu prostym
     * tekstem „SKU;nazwa;cena”; format JSON poznasz w następnej lekcji (Http04JsonApi).
     */
    static void restResource(ShopServer shop, HttpClient client) throws Exception {
        section("5. Mały zasób REST: /produkty");

        URI base = shop.baseUri();
        Reply list = send(client, base, "GET", "/produkty", null);
        show("GET /produkty: kod", list.status());
        // WYNIK: GET /produkty: kod → 200
        List<String> lines = list.body().lines().toList(); // lines = podziel na linie (Java 11+)
        show("liczba produktów", lines.size());
        // WYNIK: liczba produktów → 14
        show("pierwszy (wg SKU)", lines.get(0));
        // WYNIK: pierwszy (wg SKU) → DOM-001;Ekspres do kawy;1899.00

        Reply one = send(client, base, "GET", "/produkty/KSI-001", null);
        show("GET /produkty/KSI-001", one.status() + " " + one.body());
        // WYNIK: GET /produkty/KSI-001 → 200 KSI-001;Czysty kod;79.00
        Reply missing = send(client, base, "GET", "/produkty/XXX-999", null);
        show("GET /produkty/XXX-999", missing.status() + " " + missing.body());
        // WYNIK: GET /produkty/XXX-999 → 404 Nie ma produktu XXX-999

        Reply created = send(client, base, "POST", "/produkty", "TST-001;Zeszyt w kratkę;12.50");
        show("POST: kod", created.status());
        // WYNIK: POST: kod → 201
        show("POST: Location", created.header("Location"));
        // WYNIK: POST: Location → /produkty/TST-001
        show("POST: treść", created.body());
        // WYNIK: POST: treść → TST-001;Zeszyt w kratkę;12.50
        show("GET po utworzeniu", send(client, base, "GET", created.header("Location"), null).body());
        // WYNIK: GET po utworzeniu → TST-001;Zeszyt w kratkę;12.50

        show("POST drugi raz (konflikt)", send(client, base, "POST", "/produkty", "TST-001;Inny;1").status());
        // WYNIK: POST drugi raz (konflikt) → 409
        show("POST ze złą ceną", send(client, base, "POST", "/produkty", "TST-002;Zły;abc").body());
        // WYNIK: POST ze złą ceną → Cena nie jest liczbą: abc
        show("POST w złym formacie", send(client, base, "POST", "/produkty", "tylko-tekst").status());
        // WYNIK: POST w złym formacie → 400

        Reply deleted = send(client, base, "DELETE", "/produkty/TST-001", null);
        show("DELETE: kod i długość treści", deleted.status() + ", " + deleted.body().length());
        // WYNIK: DELETE: kod i długość treści → 204, 0
        show("DELETE drugi raz", send(client, base, "DELETE", "/produkty/TST-001", null).status());
        // WYNIK: DELETE drugi raz → 404

        // PUŁAPKA: ten sam POST dwa razy to konflikt (409) lub dwa zasoby — POST nie jest idempotentny
        // (Http01UriUrl). W lekcji Http04JsonApi porównamy to z PUT.
        // DOBRA PRAKTYKA: po utworzeniu zasobu zwracaj 201 i nagłówek Location z jego adresem; po usunięciu — 204.
        // Dlaczego: klient dostaje adres nowego zasobu bez zgadywania, a kody mówią dokładnie, co się stało.
        // Dane w pamięci znikają z końcem programu — „prawdziwe” API zapisuje je w bazie danych (t29_jdbc_databases).
    }

    // =================================================================================================
    // 6. EXECUTOR I WĄTKI
    // =================================================================================================

    /**
     * 6. Bez setExecutor serwer wykonuje handlery na jednym własnym wątku — wolny handler zatrzymuje
     * wszystkie pozostałe żądania. Z pulą wątków (executor) żądania obsługuje wiele wątków naraz.
     */
    static void executorAndThreads(ShopServer shop, HttpClient client) throws Exception {
        section("6. Executor: kto wykonuje handlery");

        String threadName = send(client, shop.baseUri(), "GET", "/watek", null).body();
        show("handler działa na wątku z naszej puli?", threadName.startsWith("sklep-handler"));
        // WYNIK: handler działa na wątku z naszej puli? → true
        show("a nie na wątku, który uruchomił serwer?", !threadName.equals(Thread.currentThread().getName()));
        // WYNIK: a nie na wątku, który uruchomił serwer? → true

        // PUŁAPKA: współdzielone dane w handlerach (nasza mapa items) są dostępne z wielu wątków naraz.
        // Dlatego użyliśmy ConcurrentSkipListMap i operacji atomowej putIfAbsent, a nie zwykłego HashMap
        // z „sprawdź, a potem wstaw” (wyścig: dwa żądania mogą jednocześnie uznać, że klucza nie ma).
        // DOBRA PRAKTYKA: ustaw executor o ograniczonej liczbie wątków i zatrzymaj go razem z serwerem.
        // Dlaczego: ograniczona pula chroni serwer przed przeciążeniem; niezatrzymana — blokuje zakończenie programu.
        // (Java 21+: Executors.newVirtualThreadPerTaskExecutor() daje każdemu żądaniu tani wątek wirtualny.)
    }

    // =================================================================================================
    // 7. FILTRY I DZIENNIK DOSTĘPU
    // =================================================================================================

    /**
     * 7. Filtr działa przed handlerem (i po nim) dla każdego żądania w kontekście — dobre miejsce na wspólne
     * nagłówki, uwierzytelnianie czy pomiar czasu. Nasz filtr dodaje nagłówek X-Serwer.
     * Dziennik dostępu zapisujemy w handlerze (przed wysłaniem treści), żeby jego zawartość była deterministyczna.
     */
    static void filtersAndAccessLog(ShopServer shop, HttpClient client) throws Exception {
        section("7. Filtry i dziennik dostępu");

        shop.clearAccessLog();
        Reply reply = send(client, shop.baseUri(), "GET", "/produkty/ELE-001", null);
        show("nagłówek dodany przez filtr", reply.header("X-Serwer"));
        // WYNIK: nagłówek dodany przez filtr → lekcja-http03
        send(client, shop.baseUri(), "GET", "/produkty/NIE-MA", null);
        send(client, shop.baseUri(), "POST", "/produkty", "LOG-001;Test;1");
        send(client, shop.baseUri(), "DELETE", "/produkty/LOG-001", null);
        send(client, shop.baseUri(), "PUT", "/produkty", "x");
        showEach("dziennik dostępu", shop.accessLog());
        // WYNIK: dziennik dostępu (liczba elementów: 5):
        // WYNIK:    • GET /produkty/ELE-001 → 200
        // WYNIK:    • GET /produkty/NIE-MA → 404
        // WYNIK:    • POST /produkty → 201
        // WYNIK:    • DELETE /produkty/LOG-001 → 204
        // WYNIK:    • PUT /produkty → 405

        // PUŁAPKA: wpis do dziennika w filtrze PO chain.doFilter (żeby poznać kod odpowiedzi) może pojawić się
        // dopiero PO tym, jak klient dostał odpowiedź — test, który czyta dziennik tuż po żądaniu, będzie
        // czasem widział niepełne dane (wyścig). Dlatego u nas wpis powstaje przed wysłaniem treści.
        // DOBRA PRAKTYKA: wspólne zachowania (nagłówki, logi, uwierzytelnianie) wyciągaj do filtrów, a nie
        // kopiuj do każdego handlera. Dlaczego: jedno miejsce zmian i brak zapomnianych handlerów.
    }

    // =================================================================================================
    // 8. AWARIE W HANDLERZE
    // =================================================================================================

    /**
     * 8. Wyjątek, który wyleci z handlera, nie zamienia się sam w czytelną odpowiedź 500. Nasza osłona
     * (safe) łapie RuntimeException i odpowiada 500. Bez niej klient dostaje zerwane połączenie.
     */
    static void handlerFailures(ShopServer shop, HttpClient client) throws Exception {
        section("8. Wyjątek w handlerze: 500 zamiast zerwanego połączenia");

        Reply guarded = send(client, shop.baseUri(), "GET", "/bomba", null);
        show("z osłoną", guarded.status() + " " + guarded.body());
        // WYNIK: z osłoną → 500 Błąd wewnętrzny serwera

        try {
            send(client, shop.baseUri(), "GET", "/bomba-bez-oslony", null);
            note("klient dostał odpowiedź (nie powinien)");
        } catch (IOException e) {
            show("bez osłony: klient dostaje wyjątek typu", e.getClass().getSimpleName());
            // WYNIK: bez osłony: klient dostaje wyjątek typu → IOException
        }

        // PUŁAPKA: nie ujawniaj klientowi szczegółów wyjątku (ślad stosu, nazwy klas, ścieżki plików) —
        // to podpowiedzi dla atakującego. Szczegóły zapisz w logu serwera, klientowi zwróć ogólny komunikat.
        // DOBRA PRAKTYKA: jedna osłona (lub filtr) na wszystkie handlery, która zamienia nieoczekiwane wyjątki
        // na 500, a znane błędy walidacji na 400/404/409. Dlaczego: klient zawsze dostaje poprawną odpowiedź HTTP.
        // Tak działa @ExceptionHandler / @ControllerAdvice w Springu.
    }

    // =================================================================================================
    // 9. ZATRZYMANIE SERWERA
    // =================================================================================================

    /**
     * 9. Po stop(0) serwer przestaje przyjmować połączenia. Klient dostaje ConnectException — to ten sam
     * błąd, który znasz z lekcji Net01SocketsTcpUdp (zamknięty port).
     */
    static void stoppingTheServer(ShopServer shop, HttpClient client) throws Exception {
        section("9. Zatrzymanie serwera");

        URI base = shop.baseUri();
        show("przed zatrzymaniem", send(client, base, "GET", "/produkty/ELE-001", null).status());
        // WYNIK: przed zatrzymaniem → 200
        shop.close();
        HttpClient patient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1).build(); // 5 s: na niektórych systemach odmowa trwa dłużej
        try {
            send(patient, base, "GET", "/produkty/ELE-001", null);
            note("serwer jeszcze odpowiada (nie powinien)");
        } catch (IOException e) {
            show("po zatrzymaniu, typ wyjątku", e.getClass().getSimpleName());
        }
        // WYNIK: po zatrzymaniu, typ wyjątku → ConnectException

        // Co dalej? To, co zbudowaliśmy, to „serwer z jednym wątkiem akceptującym + pula”. Prawdziwe serwery
        // (Tomcat, Jetty, Netty) dodają: obsługę HTTPS i HTTP/2, limity rozmiarów, sesje, dostrajanie wątków,
        // kompresję, i wiele innych. Spring Boot ukrywa je za adnotacjami — most do tego świata
        // zbudujesz w t34_toward_spring.
        // JDK 18+ ma też narzędzie jwebserver (polecenie w terminalu): uruchamia serwer plików statycznych
        // z bieżącego katalogu — wygodne do szybkich prób, ale nie do aplikacji.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • HttpServer.create(new InetSocketAddress(loopback, 0), 0) → createContext(ścieżka, handler) →
     *     setExecutor(pula) → start(); na końcu stop(0) w finally. Port 0 = wolny port.
     *   • Kontekst dopasowuje się po PREFIKSIE ścieżki; resztę ścieżki i metodę rozbiera handler.
     *   • Odczyt: getRequestMethod, getRequestURI (getPath, getRawQuery), getRequestHeaders().getFirst, getRequestBody.
     *   • Odpowiedź: nagłówki → sendResponseHeaders(kod, długość w BAJTACH; -1 brak treści; 0 porcje) → treść → zamknij.
     *   • Content-Type z charset=utf-8; długość z getBytes(UTF_8).length, nie z length().
     *   • Zła metoda → 405 + Allow; nowy zasób → 201 + Location; usunięcie → 204; brak zasobu → 404.
     *   • Dane współdzielone przez handlery muszą być bezpieczne wątkowo (ConcurrentSkipListMap, putIfAbsent).
     *   • Filtr (Filter.doFilter) dla wspólnych zachowań; własna osłona na wyjątki → 500.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego sendResponseHeaders dostaje długość w bajtach? Co oznaczają wartości -1 i 0?
     *   2. Co wypisze:  System.out.println("żółć".length() + " " + "żółć".getBytes(StandardCharsets.UTF_8).length);  ?
     *   3. ZNAJDŹ BŁĄD:  exchange.sendResponseHeaders(200, tekst.length());
     *                    exchange.getResponseBody().write(tekst.getBytes());
     *                    (wskaż dwa problemy)
     *   4. Co zwróci serwer dla DELETE /produkty (bez SKU) w naszym API i z jakim nagłówkiem?
     *   5. Dlaczego wpis do dziennika dostępu robimy przed wysłaniem treści odpowiedzi?
     *   6. Co się stanie z klientem, gdy handler rzuci wyjątek, którego nikt nie złapał?
     *   7. Dlaczego mapa produktów to ConcurrentSkipListMap, a nie HashMap?
     *   8. Kontekst „/api” — czy obsłuży żądanie GET /api/v1/produkty? A GET /apixyz?
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

    /** Uruchamia tymczasowy serwer z JEDNYM handlerem pod /test i wysyła żądanie (dla ćwiczeń). */
    static Reply callHandler(HttpHandler handler, String method, String pathAndQuery, String body) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(LOOPBACK, 0), 0);
        server.createContext("/test", handler);
        server.start();
        try {
            URI base = new URI("http", null, LOOPBACK.getHostAddress(), server.getAddress().getPort(), "/", null, null);
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                    .version(HttpClient.Version.HTTP_1_1).build();
            return send(client, base, method, pathAndQuery, body);
        } finally {
            server.stop(0);
        }
    }

    static void exercises() throws Exception {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        runChecks(false);
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        runChecks(true);
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 8 OK, ✘ 0 BŁĄD
    }

    private static void runChecks(boolean reference) {
        String tag = reference ? " (wzorzec)" : "";
        Check.equal("ćw. 1: bajty „zażółć”" + tag, 10, () -> reference ? solution1("zażółć") : exercise1("zażółć"));
        Check.equal("ćw. 1: bajty „abc”" + tag, 3, () -> reference ? solution1("abc") : exercise1("abc"));
        HttpHandler greeter = reference ? solution2() : exercise2();
        Check.equal("ćw. 2: powitanie" + tag, "200 Witaj, Zażółć!",
                () -> call(() -> {
                    Reply reply = callHandler(greeter, "GET", "/test?imie=Za%C5%BC%C3%B3%C5%82%C4%87", null);
                    return reply.status() + " " + reply.body();
                }));
        Check.equal("ćw. 2: brak imienia" + tag, "400 Podaj imię w zapytaniu: ?imie=...",
                () -> call(() -> {
                    Reply reply = callHandler(greeter, "GET", "/test", null);
                    return reply.status() + " " + reply.body();
                }));
        HttpHandler onlyGet = reference ? solution3() : exercise3();
        Check.equal("ćw. 3: GET dozwolony" + tag, "200 ok", () -> call(() -> {
            Reply reply = callHandler(onlyGet, "GET", "/test", null);
            return reply.status() + " " + reply.body();
        }));
        Check.equal("ćw. 3: POST odrzucony" + tag, "405 Allow: GET", () -> call(() -> {
            Reply reply = callHandler(onlyGet, "POST", "/test", "x");
            return reply.status() + " Allow: " + reply.header("Allow");
        }));
        HttpHandler summer = reference ? solution4() : exercise4();
        Check.equal("ćw. 4: suma" + tag, "200 42", () -> call(() -> {
            Reply reply = callHandler(summer, "POST", "/test", "10,20,12");
            return reply.status() + " " + reply.body();
        }));
        Check.equal("ćw. 4: błędne dane" + tag, "400 Błędne dane", () -> call(() -> {
            Reply reply = callHandler(summer, "POST", "/test", "1,a,3");
            return reply.status() + " " + reply.body();
        }));
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć liczbę bajtów tekstu w UTF-8 — tyle trzeba podać w sendResponseHeaders.
     * PRZEPISZ (błędna wersja): <pre>{@code exchange.sendResponseHeaders(200, text.length());}</pre>
     * Podpowiedź: getBytes(UTF_8).length (stała UTF_8 jest zdefiniowana w tej klasie).
     */
    static int exercise1(String text) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): handler odpowiadający {@code Witaj, <imię>!} dla żądania z zapytaniem {@code ?imie=...}
     * (kod 200). Gdy parametru brak, odpowiedz kodem 400 i tekstem
     * {@code Podaj imię w zapytaniu: ?imie=...}. Użyj writeText(exchange, kod, tekst).
     * Podpowiedź: getRequestURI().getQuery() jest już odkodowane (przy jednym parametrze wystarczy to, ale
     * pamiętaj o PUŁAPCE z sekcji 2); sprawdź, czy zaczyna się od "imie=".
     */
    static HttpHandler exercise2() {
        return exchange -> {
            throw new UnsupportedOperationException("TODO");
        };
    }

    /**
     * ĆWICZENIE 3 (średnie): handler, który na GET odpowiada 200 i tekstem {@code ok}, a na każdą inną metodę —
     * kodem 405 z nagłówkiem {@code Allow: GET}.
     * Podpowiedź: exchange.getResponseHeaders().set("Allow", "GET"); potem writeText(exchange, 405, ...).
     */
    static HttpHandler exercise3() {
        return exchange -> {
            throw new UnsupportedOperationException("TODO");
        };
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): handler dla POST: treść to liczby całkowite rozdzielone przecinkami
     * („10,20,12”); odpowiedz 200 i sumą jako tekstem („42”). Gdy któryś element nie jest liczbą — 400
     * i tekst {@code Błędne dane}.
     * Podpowiedź: new String(exchange.getRequestBody().readAllBytes(), UTF_8), split(","), Integer.parseInt
     * w try/catch (NumberFormatException).
     */
    static HttpHandler exercise4() {
        return exchange -> {
            throw new UnsupportedOperationException("TODO");
        };
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(String text) {
        return text.getBytes(UTF_8).length;
    }

    static HttpHandler solution2() {
        return exchange -> {
            String query = exchange.getRequestURI().getQuery();
            if (query == null || !query.startsWith("imie=")) {
                writeText(exchange, 400, "Podaj imię w zapytaniu: ?imie=...");
            } else {
                writeText(exchange, 200, "Witaj, " + query.substring("imie=".length()) + "!");
            }
        };
    }

    static HttpHandler solution3() {
        return exchange -> {
            if (exchange.getRequestMethod().equals("GET")) {
                writeText(exchange, 200, "ok");
            } else {
                exchange.getResponseHeaders().set("Allow", "GET");
                writeText(exchange, 405, "Metoda niedozwolona");
            }
        };
    }

    static HttpHandler solution4() {
        return exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
            try {
                int sum = 0;
                for (String part : body.split(",")) {
                    sum += Integer.parseInt(part.trim());
                }
                writeText(exchange, 200, String.valueOf(sum));
            } catch (NumberFormatException e) {
                writeText(exchange, 400, "Błędne dane");
            }
        };
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Klient musi wiedzieć, ile BAJTÓW treści ma przeczytać (Content-Length); polskie litery to 2 bajty,
     *      więc liczba znaków nie wystarczy. -1 = brak treści w odpowiedzi; 0 = długość nieznana, treść idzie
     *      porcjami (chunked) aż do zamknięcia strumienia.
     *   2. „4 8”: „żółć” to 4 znaki, a każda z liter ż, ó, ł, ć zajmuje w UTF-8 po 2 bajty (razem 8).
     *   3. (a) długość w znakach zamiast w bajtach — dla polskich liter będzie za mała i zapis się nie uda;
     *      (b) getBytes() bez jawnego UTF_8 użyje kodowania domyślnego systemu (a Content-Type nie ma charset).
     *   4. 405 Method Not Allowed z nagłówkiem Allow: GET, POST. DELETE jest dozwolone tylko na pojedynczym
     *      elemencie (DELETE /produkty/SKU); bez SKU żądanie dotyczy kolekcji i trafia do ostatniej gałęzi handlera.
     *   5. Żeby klient, który otrzymał odpowiedź, na pewno widział wpis — inaczej wpis mógłby powstać chwilę
     *      po odpowiedzi i test (albo czytelnik dziennika) widziałby dane niekompletne (wyścig).
     *   6. Serwer nie wyśle poprawnej odpowiedzi — połączenie zostanie zerwane, a klient dostanie IOException
     *      (u nas „HTTP/1.1 header parser received no bytes”). Dlatego osłona zamienia wyjątki na kod 500.
     *   7. Handlery działają na wielu wątkach naraz; ConcurrentSkipListMap jest bezpieczna wątkowo (i posortowana),
     *      a HashMap przy równoległych zapisach może się uszkodzić lub gubić dane.
     *   8. Tak dla GET /api/v1/produkty (prefiks ścieżki); GET /apixyz też — dopasowanie jest po prefiksie TEKSTU,
     *      nie po segmentach, więc „/apixyz” zaczyna się od „/api”. Jeśli to niepożądane, sprawdź ścieżkę w handlerze.
     */
    // </editor-fold>
}
