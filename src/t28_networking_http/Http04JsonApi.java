package t28_networking_http;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: API w formacie JSON — klient i serwer
 *        (JSON = JavaScript Object Notation, tekstowy format danych; API = Application Programming Interface,
 *         umowa „jak rozmawiać z programem”; validation = walidacja, sprawdzanie poprawności danych)
 *
 * W SKRÓCIE:
 *   Programy w sieci najczęściej wymieniają dane jako JSON: tekst z obiektami {"klucz": wartość}, listami [...],
 *   napisami, liczbami, true/false i null. Klient wysyła JSON w treści żądania, serwer odpowiada JSON-em
 *   i kodem statusu. W tej lekcji piszesz mały parser i zapis JSON „ręcznie” (żeby zrozumieć, co robi
 *   biblioteka), budujesz API /api/v1/produkty z walidacją, kodami 201/400/404/409/415 i wersjonowaniem
 *   — i porównujesz PUT z POST.
 *
 * ANALOGIA: formularz zamówienia w sklepie. JSON to wypełniony formularz: pola mają nazwy i wartości.
 *   Sprzedawca (serwer) sprawdza formularz: brak nazwiska? Odsyła z informacją, które pola poprawić (400).
 *   Zamówienie na towar, którego nie ma — „nie ma takiego produktu” (404). PUT to „ustaw ten zapis dokładnie
 *   tak” (powtórzenie niczego nie zmienia), POST to „dołóż nowe zamówienie” (powtórzenie = dwa zamówienia).
 *
 * JAK TO DZIAŁA:
 *   klient:  POST /api/v1/produkty   Content-Type: application/json; charset=utf-8
 *            {"sku":"ABC-001","nazwa":"Kubek","cena":19.90,"stan":3}
 *   serwer:  sprawdza typ treści (415) → parsuje JSON (400) → waliduje pola (400) → sprawdza konflikt (409)
 *            → zapisuje → 201 Created + Location: /api/v1/produkty/ABC-001 + JSON zapisanego zasobu
 *
 *   Błąd ma STAŁY kształt: {"blad":"KOD","komunikat":"opis","pola":["pole: problem", ...]}.
 *   Liczby w JSON czytamy jako BigDecimal (nie double), żeby nie tracić dokładności (np. ceny).
 *
 * SŁÓWKA: parse = rozbierz tekst na strukturę; serialize/write = zapisz strukturę jako tekst; object = obiekt;
 *   array = tablica; field = pole; escape = „ucieczka” znaków specjalnych (\" \n); payload = ładunek, treść;
 *   idempotent = idempotentny; version = wersja; conflict = konflikt; validation = walidacja;
 *   DTO = Data Transfer Object, prosty obiekt do przesyłania danych
 *
 * ZOBACZ TEŻ: t18_io_files/Io05JsonManual (pełny, ręczny parser JSON), t28_networking_http/Http03LocalServer
 *   (serwer i REST), t28_networking_http/Http01UriUrl (metody, kody statusu), t15_numbers/Numbers01BigDecimal (liczby),
 *   t34_toward_spring/Spring03RestConcepts (to samo w Springu)
 * </pre>
 */
public class Http04JsonApi {

    private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress(); // loopback = pętla zwrotna
    private static final java.nio.charset.Charset UTF_8 = StandardCharsets.UTF_8;
    private static final String JSON_TYPE = "application/json; charset=utf-8";

    public static void main(String[] args) throws Exception {
        title("Http04 — API w formacie JSON");

        jsonWriting();            // json writing = zapis JSON
        jsonParsing();            // json parsing = czytanie JSON
        recordsAndJson();         // records and json = rekordy i JSON
        try (ApiServer api = new ApiServer()) {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                    .version(HttpClient.Version.HTTP_1_1).build();
            readingResources(api, client);  // reading resources = odczyt zasobów
            creatingResources(api, client); // creating resources = tworzenie zasobów
            validationErrors(api, client);  // validation errors = błędy walidacji
            putVersusPost(api, client);     // put versus post = PUT kontra POST
            versioning(api, client);        // versioning = wersjonowanie
        }
        whatLibrariesDo();        // what libraries do = co robią biblioteki
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // MINI-JSON: zapis i odczyt (dla nauki — w prawdziwym projekcie użyj biblioteki, np. Jackson)
    // =================================================================================================

    /**
     * Minimalny JSON: obsługuje null, napisy, liczby (jako BigDecimal), true/false, mapy (obiekty), listy (tablice).
     * Nie obsługuje m.in. komentarzy ani zagnieżdżeń o głębokości tysięcy poziomów.
     * Pełniejszą wersję znajdziesz w t18_io_files/Io05JsonManual.
     */
    static final class Json {
        private final String text;
        private int pos; // pos = pozycja w tekście (position)

        private Json(String text) {
            this.text = text;
        }

        // ---------- zapis ----------

        /** Zamienia strukturę (Map, List, String, Number, Boolean, null) na tekst JSON. */
        static String write(Object value) {
            StringBuilder out = new StringBuilder(); // StringBuilder = budowniczy tekstu
            write(value, out);
            return out.toString();
        }

        private static void write(Object value, StringBuilder out) {
            if (value == null) {
                out.append("null");
            } else if (value instanceof String s) { // instanceof ze wzorcem (Java 16+)
                quote(s, out);
            } else if (value instanceof BigDecimal d) {
                out.append(d.toPlainString()); // bez notacji naukowej (1E+3)
            } else if (value instanceof Number || value instanceof Boolean) {
                out.append(value);
            } else if (value instanceof Map<?, ?> map) {
                out.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!first) {
                        out.append(',');
                    }
                    first = false;
                    quote(String.valueOf(entry.getKey()), out);
                    out.append(':');
                    write(entry.getValue(), out);
                }
                out.append('}');
            } else if (value instanceof Iterable<?> list) {
                out.append('[');
                boolean first = true;
                for (Object element : list) {
                    if (!first) {
                        out.append(',');
                    }
                    first = false;
                    write(element, out);
                }
                out.append(']');
            } else {
                throw new IllegalArgumentException("Nieobsługiwany typ: " + value.getClass().getSimpleName());
            }
        }

        /** Napis w cudzysłowie z „ucieczką” znaków specjalnych (cudzysłów, ukośnik, znaki sterujące). */
        private static void quote(String s, StringBuilder out) {
            out.append('"');
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    case '\n' -> out.append("\\n");
                    case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t");
                    default -> {
                        if (c < 0x20) { // pozostałe znaki sterujące zapisujemy jako sekwencję backslash-u i 4 cyfry hex
                            out.append(String.format("\\u%04x", (int) c));
                        } else {
                            out.append(c); // polskie litery zostają bez zmian — JSON jest w UTF-8
                        }
                    }
                }
            }
            out.append('"');
        }

        // ---------- odczyt ----------

        /** Zamienia tekst JSON na strukturę (LinkedHashMap, ArrayList, String, BigDecimal, Boolean, null). */
        static Object parse(String text) {
            Json parser = new Json(text);
            parser.skipSpaces();
            Object value = parser.value();
            parser.skipSpaces();
            if (parser.pos != text.length()) {
                throw parser.error("nadmiarowe znaki po wartości");
            }
            return value;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException("Błędny JSON na pozycji " + pos + ": " + message);
        }

        private void skipSpaces() { // skip = pomiń
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
        }

        private void expect(char expected) { // expect = oczekuj
            if (pos >= text.length() || text.charAt(pos) != expected) {
                throw error("oczekiwano znaku '" + expected + "'");
            }
            pos++;
        }

        private Object value() {
            if (pos >= text.length()) {
                throw error("nieoczekiwany koniec tekstu");
            }
            char c = text.charAt(pos);
            return switch (c) { // switch jako wyrażenie (Java 14+)
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> {
                    if (c == '-' || Character.isDigit(c)) {
                        yield number(); // yield = „to jest wartość tej gałęzi”
                    }
                    throw error("oczekiwano wartości");
                }
            };
        }

        private Object literal(String word, Object result) {
            if (!text.startsWith(word, pos)) {
                throw error("oczekiwano " + word);
            }
            pos += word.length();
            return result;
        }

        private Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>(); // zachowuje kolejność pól
            expect('{');
            skipSpaces();
            if (pos < text.length() && text.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipSpaces();
                String key = string();
                skipSpaces();
                expect(':');
                skipSpaces();
                map.put(key, value());
                skipSpaces();
                if (pos < text.length() && text.charAt(pos) == ',') {
                    pos++;
                } else {
                    expect('}');
                    return map;
                }
            }
        }

        private List<Object> array() {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipSpaces();
            if (pos < text.length() && text.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (true) {
                skipSpaces();
                list.add(value());
                skipSpaces();
                if (pos < text.length() && text.charAt(pos) == ',') {
                    pos++;
                } else {
                    expect(']');
                    return list;
                }
            }
        }

        private String string() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (pos < text.length()) {
                char c = text.charAt(pos++);
                if (c == '"') {
                    return result.toString();
                }
                if (c != '\\') {
                    result.append(c);
                    continue;
                }
                if (pos >= text.length()) {
                    break;
                }
                char escaped = text.charAt(pos++);
                switch (escaped) {
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    case '/' -> result.append('/');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'u' -> { // sekwencja backslash-u i cztery cyfry szesnastkowe = jeden znak
                        if (pos + 4 > text.length()) {
                            throw error("za krótka sekwencja unikodowa");
                        }
                        try {
                            result.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                        } catch (NumberFormatException e) {
                            throw error("zła sekwencja unikodowa");
                        }
                        pos += 4;
                    }
                    default -> throw error("nieznana sekwencja ucieczki");
                }
            }
            throw error("napis bez zamykającego cudzysłowu");
        }

        private BigDecimal number() {
            int start = pos;
            while (pos < text.length() && "+-0123456789.eE".indexOf(text.charAt(pos)) >= 0) {
                pos++;
            }
            try {
                return new BigDecimal(text.substring(start, pos)); // BigDecimal: dokładne liczby dziesiętne
            } catch (NumberFormatException e) {
                pos = start;
                throw error("zła liczba");
            }
        }
    }

    // =================================================================================================
    // MODEL I SERWER API
    // =================================================================================================

    /** DTO produktu: sku (kod), nazwa, cena, stan magazynowy. */
    record ProductDto(String sku, String name, BigDecimal price, int stock) { // record = rekord (Java 16+)
        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sku", sku);
            map.put("nazwa", name);
            map.put("cena", price);
            map.put("stan", stock);
            return map;
        }

        String toJson() {
            return Json.write(toMap());
        }
    }

    /** Sprawdza obiekt JSON i zwraca listę problemów (pusta lista = dane poprawne). */
    static List<String> validate(Map<String, Object> json) {
        List<String> problems = new ArrayList<>();
        if (!(json.get("sku") instanceof String sku) || !sku.matches("[A-Z]{3}-\\d{3}")) {
            problems.add("sku: wymagany kod w formacie AAA-123");
        }
        if (!(json.get("nazwa") instanceof String name) || name.isBlank() || name.length() > 50) {
            problems.add("nazwa: wymagany niepusty tekst do 50 znaków");
        }
        if (!(json.get("cena") instanceof BigDecimal price) || price.signum() < 0) {
            problems.add("cena: wymagana liczba nieujemna");
        }
        Object stock = json.getOrDefault("stan", BigDecimal.ZERO); // getOrDefault = pobierz albo wartość domyślna
        if (!(stock instanceof BigDecimal count) || count.signum() < 0 || count.stripTrailingZeros().scale() > 0) {
            problems.add("stan: wymagana nieujemna liczba całkowita");
        }
        return problems;
    }

    /** Zamienia POPRAWNY (zwalidowany) obiekt JSON na rekord. */
    static ProductDto toDto(Map<String, Object> json) {
        Object stock = json.getOrDefault("stan", BigDecimal.ZERO);
        return new ProductDto((String) json.get("sku"), (String) json.get("nazwa"),
                (BigDecimal) json.get("cena"), ((BigDecimal) stock).intValueExact()); // intValueExact: wyjątek, gdy nie int
    }

    /** Odpowiedź w wygodnej postaci. */
    record Reply(int status, String body, HttpHeaders headers) {
        String header(String name) {
            return headers.firstValue(name).orElse("brak");
        }

        Object json() { // json = treść odpowiedzi jako struktura
            return Json.parse(body);
        }
    }

    /**
     * Serwer API: /api/v1/produkty (GET, POST, PUT, DELETE), /api/v1/zamowienia (POST, GET),
     * /api/v2/produkty/{sku} (GET, inny kształt odpowiedzi).
     */
    static final class ApiServer implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor = Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "api-handler");
            thread.setDaemon(true);
            return thread;
        });
        private final Map<String, ProductDto> products = new ConcurrentSkipListMap<>();
        private final List<Map<String, Object>> orders = new CopyOnWriteArrayList<>(); // lista bezpieczna wątkowo
        private final AtomicInteger orderCounter = new AtomicInteger(); // licznik zamówień (bezpieczny wątkowo)

        ApiServer() throws IOException {
            for (Product product : SampleData.products()) {
                products.put(product.sku(), new ProductDto(product.sku(), product.name(), product.price(), product.stock()));
            }
            server = HttpServer.create(new InetSocketAddress(LOOPBACK, 0), 0);
            server.createContext("/api/v1/produkty", guarded(this::handleProductsV1));
            server.createContext("/api/v1/zamowienia", guarded(this::handleOrders));
            server.createContext("/api/v2/produkty", guarded(this::handleProductsV2));
            server.setExecutor(executor);
            server.start();
        }

        // ---------- pomocnicy odpowiedzi ----------

        private static void writeJson(HttpExchange exchange, int status, Object value) throws IOException {
            byte[] bytes = Json.write(value).getBytes(UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_TYPE);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }

        private static void writeError(HttpExchange exchange, int status, String code, String message, List<String> fields)
                throws IOException {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("blad", code);
            error.put("komunikat", message);
            error.put("pola", fields);
            writeJson(exchange, status, error);
        }

        private static void writeEmpty(HttpExchange exchange, int status) throws IOException {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        }

        /** Osłona: nieoczekiwany wyjątek → 500 w tym samym kształcie błędu. */
        private HttpHandler guarded(HttpHandler handler) {
            return exchange -> {
                try {
                    handler.handle(exchange);
                } catch (RuntimeException e) {
                    writeError(exchange, 500, "BLAD_SERWERA", "Wystąpił nieoczekiwany błąd", List.of());
                }
            };
        }

        private static String lastSegment(HttpExchange exchange, String prefix) {
            String rest = exchange.getRequestURI().getPath().substring(prefix.length());
            return rest.startsWith("/") ? rest.substring(1) : rest;
        }

        /**
         * Wczytuje treść jako obiekt JSON. Zwraca null (i sama odpowiada błędem), gdy typ treści nie jest
         * JSON-em (415) albo tekst jest niepoprawny (400).
         */
        private static Map<String, Object> readJsonObject(HttpExchange exchange) throws IOException {
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
                writeError(exchange, 415, "NIEOBSLUGIWANY_TYP", "Oczekiwano Content-Type: application/json", List.of());
                return null;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
            try {
                if (Json.parse(body) instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked") // parser zawsze zwraca Map<String, Object>
                    Map<String, Object> result = (Map<String, Object>) map;
                    return result;
                }
                writeError(exchange, 400, "NIEPOPRAWNY_JSON", "Oczekiwano obiektu JSON {...}", List.of());
            } catch (IllegalArgumentException e) {
                writeError(exchange, 400, "NIEPOPRAWNY_JSON", e.getMessage(), List.of());
            }
            return null;
        }

        // ---------- /api/v1/produkty ----------

        private void handleProductsV1(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String sku = lastSegment(exchange, "/api/v1/produkty");
            boolean collection = sku.isEmpty();
            if (method.equals("GET") && collection) {
                List<Object> all = new ArrayList<>();
                for (ProductDto dto : products.values()) {
                    all.add(dto.toMap());
                }
                writeJson(exchange, 200, all);
            } else if (method.equals("GET")) {
                ProductDto dto = products.get(sku);
                if (dto == null) {
                    writeError(exchange, 404, "NIE_ZNALEZIONO", "Nie ma produktu " + sku, List.of());
                } else {
                    writeJson(exchange, 200, dto.toMap());
                }
            } else if (method.equals("POST") && collection) {
                createProduct(exchange);
            } else if (method.equals("PUT") && !collection) {
                replaceProduct(exchange, sku);
            } else if (method.equals("DELETE") && !collection) {
                if (products.remove(sku) == null) {
                    writeError(exchange, 404, "NIE_ZNALEZIONO", "Nie ma produktu " + sku, List.of());
                } else {
                    writeEmpty(exchange, 204);
                }
            } else {
                exchange.getResponseHeaders().set("Allow", collection ? "GET, POST" : "GET, PUT, DELETE");
                writeError(exchange, 405, "METODA_NIEDOZWOLONA", "Metoda " + method + " nie jest tu dozwolona", List.of());
            }
        }

        private void createProduct(HttpExchange exchange) throws IOException {
            Map<String, Object> json = readJsonObject(exchange);
            if (json == null) {
                return;
            }
            List<String> problems = validate(json);
            if (!problems.isEmpty()) {
                writeError(exchange, 400, "BLEDNE_DANE", "Niepoprawne dane produktu", problems);
                return;
            }
            ProductDto dto = toDto(json);
            if (products.putIfAbsent(dto.sku(), dto) != null) { // atomowo: wstaw, jeśli nie ma
                writeError(exchange, 409, "KONFLIKT", "Produkt " + dto.sku() + " już istnieje", List.of());
                return;
            }
            exchange.getResponseHeaders().set("Location", "/api/v1/produkty/" + dto.sku());
            writeJson(exchange, 201, dto.toMap());
        }

        /** PUT: zapisz zasób DOKŁADNIE tak, jak przysłano (utwórz albo nadpisz). */
        private void replaceProduct(HttpExchange exchange, String sku) throws IOException {
            Map<String, Object> json = readJsonObject(exchange);
            if (json == null) {
                return;
            }
            List<String> problems = validate(json);
            if (!problems.isEmpty()) {
                writeError(exchange, 400, "BLEDNE_DANE", "Niepoprawne dane produktu", problems);
                return;
            }
            ProductDto dto = toDto(json);
            if (!dto.sku().equals(sku)) {
                writeError(exchange, 400, "BLEDNE_DANE", "SKU w adresie i w treści muszą być takie same",
                        List.of("sku: " + dto.sku() + " ≠ " + sku));
                return;
            }
            boolean existed = products.put(sku, dto) != null; // put zwraca poprzednią wartość albo null
            if (!existed) {
                exchange.getResponseHeaders().set("Location", "/api/v1/produkty/" + sku);
            }
            writeJson(exchange, existed ? 200 : 201, dto.toMap());
        }

        // ---------- /api/v1/zamowienia ----------

        private void handleOrders(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equals("GET")) {
                writeJson(exchange, 200, orders);
            } else if (method.equals("POST")) {
                Map<String, Object> json = readJsonObject(exchange);
                if (json == null) {
                    return;
                }
                List<String> problems = new ArrayList<>();
                if (!(json.get("sku") instanceof String sku) || !products.containsKey(sku)) {
                    problems.add("sku: wymagany kod istniejącego produktu");
                }
                if (!(json.get("ilosc") instanceof BigDecimal qty) || qty.signum() <= 0) {
                    problems.add("ilosc: wymagana liczba dodatnia");
                }
                if (!problems.isEmpty()) {
                    writeError(exchange, 400, "BLEDNE_DANE", "Niepoprawne zamówienie", problems);
                    return;
                }
                Map<String, Object> order = new LinkedHashMap<>();
                order.put("id", "ZAM-" + orderCounter.incrementAndGet()); // incrementAndGet = zwiększ i pobierz
                order.put("sku", json.get("sku"));
                order.put("ilosc", json.get("ilosc"));
                orders.add(order);
                exchange.getResponseHeaders().set("Location", "/api/v1/zamowienia/" + order.get("id"));
                writeJson(exchange, 201, order);
            } else {
                exchange.getResponseHeaders().set("Allow", "GET, POST");
                writeError(exchange, 405, "METODA_NIEDOZWOLONA", "Metoda " + method + " nie jest tu dozwolona", List.of());
            }
        }

        // ---------- /api/v2/produkty ----------

        /** Wersja 2 zmienia kształt odpowiedzi: cena jest obiektem {kwota, waluta}. Wersja 1 działa bez zmian. */
        private void handleProductsV2(HttpExchange exchange) throws IOException {
            String sku = lastSegment(exchange, "/api/v2/produkty");
            ProductDto dto = products.get(sku);
            if (!exchange.getRequestMethod().equals("GET") || dto == null) {
                writeError(exchange, dto == null ? 404 : 405, "NIE_ZNALEZIONO", "Brak zasobu lub metody", List.of());
                return;
            }
            Map<String, Object> price = new LinkedHashMap<>();
            price.put("kwota", dto.price());
            price.put("waluta", "PLN");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sku", dto.sku());
            body.put("nazwa", dto.name());
            body.put("cena", price);
            body.put("stan", dto.stock());
            writeJson(exchange, 200, body);
        }

        URI baseUri() {
            try {
                return new URI("http", null, LOOPBACK.getHostAddress(), server.getAddress().getPort(), "/", null, null);
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void close() {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    // =================================================================================================
    // POMOCNIK KLIENTA
    // =================================================================================================

    /** Wysyła żądanie z podanym typem treści (null = bez nagłówka Content-Type). */
    static Reply sendRaw(HttpClient client, URI base, String method, String path, String contentType, String body)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(base.resolve(path))
                .timeout(Duration.ofSeconds(2))
                .header("Accept", "application/json");
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        builder.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, UTF_8));
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(UTF_8));
        return new Reply(response.statusCode(), response.body(), response.headers());
    }

    /** Wysyła JSON (Content-Type: application/json; charset=utf-8). */
    static Reply sendJson(HttpClient client, URI base, String method, String path, String json) throws Exception {
        return sendRaw(client, base, method, path, JSON_TYPE, json);
    }

    // =================================================================================================
    // 1. ZAPIS JSON
    // =================================================================================================

    /**
     * 1. JSON ma tylko kilka rodzajów wartości: obiekt {...}, tablica [...], napis "...", liczba, true/false, null.
     * Nasz zapis zamienia mapy na obiekty, listy na tablice — resztą zajmuje się metoda write.
     */
    static void jsonWriting() {
        section("1. Zapis JSON: obiekty, tablice, znaki specjalne");

        Map<String, Object> product = new LinkedHashMap<>(); // LinkedHashMap = kolejność pól zostaje taka jak wstawiono
        product.put("sku", "KUB-001");
        product.put("nazwa", "Kubek");
        product.put("cena", new BigDecimal("19.90"));
        product.put("stan", 3);
        product.put("dostepny", true);
        product.put("opis", null);
        product.put("tagi", List.of("dom", "kuchnia"));
        show("obiekt", Json.write(product)); // write = zapisz
        // WYNIK: obiekt → {"sku":"KUB-001","nazwa":"Kubek","cena":19.90,"stan":3,"dostepny":true,"opis":null,"tagi":["dom","kuchnia"]}

        Map<String, Object> tricky = new LinkedHashMap<>();
        tricky.put("tekst", "Powiedział: \"cześć\"\nNowa linia\tTab i ukośnik \\");
        show("znaki specjalne", Json.write(tricky));
        // WYNIK: znaki specjalne → {"tekst":"Powiedział: \"cześć\"\nNowa linia\tTab i ukośnik \\"}

        // PRZED (zły sposób): sklejanie JSON-a z tekstów.
        String name = "Kubek \"XL\"";
        String concatenated = "{\"nazwa\":\"" + name + "\"}";
        show("sklejony tekst", concatenated);
        // WYNIK: sklejony tekst → {"nazwa":"Kubek "XL""}
        expectThrows("czytanie sklejonego tekstu", () -> Json.parse(concatenated));
        // WYNIK: ✔ czytanie sklejonego tekstu → rzucono IllegalArgumentException: Błędny JSON na pozycji 17: oczekiwano znaku '}'

        // PO: zapis przez Map + write — cudzysłowy są „uciekane” automatycznie.
        Map<String, Object> safe = new LinkedHashMap<>();
        safe.put("nazwa", name);
        show("poprawny zapis", Json.write(safe));
        // WYNIK: poprawny zapis → {"nazwa":"Kubek \"XL\""}

        // PUŁAPKA: ręczne sklejanie JSON-a z danych użytkownika to nie tylko błąd składni — to luka bezpieczeństwa
        // (wstrzyknięcie: ktoś wpisze nazwę z cudzysłowem i dopisze własne pola). Zawsze używaj zapisu, który
        // „ucieka” znaki specjalne (nasz write albo biblioteka).
        // DOBRA PRAKTYKA: JSON zawsze w UTF-8 (polskie litery zapisujemy wprost, nie jako sekwencje) i z
        // nagłówkiem Content-Type: application/json; charset=utf-8. Dlaczego: to standard — każdy klient go rozumie.
    }

    // =================================================================================================
    // 2. ODCZYT JSON
    // =================================================================================================

    /**
     * 2. Parser zamienia tekst na strukturę: obiekt → LinkedHashMap, tablica → ArrayList, napis → String,
     * liczba → BigDecimal, true/false → Boolean, null → null.
     */
    static void jsonParsing() {
        section("2. Czytanie JSON: parser i typy");

        String text = """
                {
                  "sku": "ELE-001",
                  "nazwa": "Laptop \\"Pro\\" 14",
                  "cena": 5499.99,
                  "stan": 7,
                  "dostepny": true,
                  "tagi": ["komputer", "praca"],
                  "producent": {"nazwa": "Zażółć S.A.", "kraj": null}
                }
                """;
        Object parsed = Json.parse(text); // parse = rozbierz tekst
        Map<?, ?> map = (Map<?, ?>) parsed;
        show("klucze", map.keySet());
        // WYNIK: klucze → [sku, nazwa, cena, stan, dostepny, tagi, producent]
        show("nazwa (z ucieczką)", map.get("nazwa"));
        // WYNIK: nazwa (z ucieczką) → Laptop "Pro" 14
        show("typ ceny", map.get("cena").getClass().getSimpleName());
        // WYNIK: typ ceny → BigDecimal
        show("tagi", map.get("tagi"));
        // WYNIK: tagi → [komputer, praca]
        show("producent", map.get("producent"));
        // WYNIK: producent → {nazwa=Zażółć S.A., kraj=null}

        // Błędy: komunikat zawiera pozycję w tekście.
        expectThrows("brak wartości", () -> Json.parse("{\"a\":}"));
        // WYNIK: ✔ brak wartości → rzucono IllegalArgumentException: Błędny JSON na pozycji 5: oczekiwano wartości
        expectThrows("nadmiarowe znaki", () -> Json.parse("{} x"));
        // WYNIK: ✔ nadmiarowe znaki → rzucono IllegalArgumentException: Błędny JSON na pozycji 3: nadmiarowe znaki po wartości
        expectThrows("niezamknięty napis", () -> Json.parse("{\"a\":\"abc"));
        // WYNIK: ✔ niezamknięty napis → rzucono IllegalArgumentException: Błędny JSON na pozycji 9: napis bez zamykającego cudzysłowu

        // PUŁAPKA: liczby w JSON nie mają typu — to tylko tekst cyfr. Czytanie ich jako double psuje ceny
        // (0.1 + 0.2 ≠ 0.3). Dlatego nasz parser zwraca BigDecimal (patrz t15_numbers/Numbers01BigDecimal).
        BigDecimal sum = (BigDecimal) Json.parse("0.1");
        show("0.1 jako BigDecimal + 0.2", sum.add((BigDecimal) Json.parse("0.2")));
        // WYNIK: 0.1 jako BigDecimal + 0.2 → 0.3
        show("0.1 + 0.2 jako double", 0.1 + 0.2);
        // WYNIK: 0.1 + 0.2 jako double → 0.30000000000000004

        // DOBRA PRAKTYKA: nie ufaj treści od klienta — zakładaj, że brakuje pól, mają zły typ albo wartość spoza zakresu.
        // Najpierw parsuj, potem WALIDUJ (następne sekcje), dopiero potem używaj danych.
    }

    // =================================================================================================
    // 3. REKORDY I JSON
    // =================================================================================================

    /**
     * 3. W Javie dane trzymamy w typowanych obiektach (rekordach), a w sieci wysyłamy JSON. Potrzebne są dwie
     * konwersje: rekord → JSON (toMap + write) i JSON → rekord (parse + walidacja + toDto).
     */
    static void recordsAndJson() {
        section("3. Rekord ↔ JSON i walidacja");

        ProductDto original = new ProductDto("KUB-001", "Kubek \"XL\"", new BigDecimal("19.90"), 3);
        String json = original.toJson();
        show("rekord → JSON", json);
        // WYNIK: rekord → JSON → {"sku":"KUB-001","nazwa":"Kubek \"XL\"","cena":19.90,"stan":3}

        @SuppressWarnings("unchecked") // wiemy, że to obiekt JSON, więc Map<String, Object>
        Map<String, Object> back = (Map<String, Object>) Json.parse(json);
        show("problemy walidacji", validate(back));
        // WYNIK: problemy walidacji → []
        ProductDto restored = toDto(back);
        show("JSON → rekord", restored);
        // WYNIK: JSON → rekord → ProductDto[sku=KUB-001, name=Kubek "XL", price=19.90, stock=3]
        show("rekord po drodze tam i z powrotem jest taki sam?", restored.equals(original));
        // WYNIK: rekord po drodze tam i z powrotem jest taki sam? → true

        @SuppressWarnings("unchecked")
        Map<String, Object> wrong = (Map<String, Object>) Json.parse("{\"sku\":\"ele-1\",\"nazwa\":\"\",\"cena\":-5,\"stan\":1.5}");
        showEach("problemy w błędnych danych", validate(wrong));
        // WYNIK: problemy w błędnych danych (liczba elementów: 4):
        // WYNIK:    • sku: wymagany kod w formacie AAA-123
        // WYNIK:    • nazwa: wymagany niepusty tekst do 50 znaków
        // WYNIK:    • cena: wymagana liczba nieujemna
        // WYNIK:    • stan: wymagana nieujemna liczba całkowita

        // Walidacja zwraca WSZYSTKIE problemy naraz, a nie tylko pierwszy — użytkownik poprawia formularz jednym razem.

        // PUŁAPKA: pole opcjonalne („stan” w naszym API) wymaga decyzji: brak pola = wartość domyślna (0),
        // ale pole z zepsutą wartością to błąd, a nie „brak”. Nie zamieniaj błędów w wartości domyślne po cichu.
        // DOBRA PRAKTYKA: oddziel DTO (kształt danych w sieci) od modelu domeny (Product z lekcji o OOP).
        // Dlaczego: API ma być stabilne, a model w programie może się zmieniać.
    }

    // =================================================================================================
    // 4. ODCZYT ZASOBÓW
    // =================================================================================================

    /**
     * 4. GET zwraca JSON; kod 200 dla sukcesu, 404 + JSON-owy opis błędu dla nieistniejącego zasobu.
     */
    static void readingResources(ApiServer api, HttpClient client) throws Exception {
        section("4. GET: lista, element i błąd 404 w JSON");

        URI base = api.baseUri();
        Reply list = sendJson(client, base, "GET", "/api/v1/produkty", null);
        show("kod i Content-Type", list.status() + ", " + list.header("Content-Type"));
        // WYNIK: kod i Content-Type → 200, application/json; charset=utf-8
        show("liczba produktów w tablicy", ((List<?>) list.json()).size());
        // WYNIK: liczba produktów w tablicy → 14

        Reply one = sendJson(client, base, "GET", "/api/v1/produkty/ELE-001", null);
        show("jeden produkt", one.body());
        // WYNIK: jeden produkt → {"sku":"ELE-001","nazwa":"Laptop Pro 14","cena":5499.99,"stan":7}

        Reply missing = sendJson(client, base, "GET", "/api/v1/produkty/ZZZ-999", null);
        show("brakujący produkt", missing.status() + " " + missing.body());
        // WYNIK: brakujący produkt → 404 {"blad":"NIE_ZNALEZIONO","komunikat":"Nie ma produktu ZZZ-999","pola":[]}

        // PUŁAPKA: ścieżka, której serwer w ogóle nie zna, dostaje domyślną odpowiedź 404 serwera JDK w formacie
        // HTML/tekstowym, a nie nasz JSON. Klient nie może zakładać, że KAŻDY błąd ma treść JSON-ową —
        // najpierw sprawdź kod statusu, a Content-Type, zanim zaczniesz parsować.
        Reply unknownPath = sendJson(client, base, "GET", "/api/v1/nie-ma", null);
        show("nieznana ścieżka: kod", unknownPath.status());
        // WYNIK: nieznana ścieżka: kod → 404
        show("nieznana ścieżka: czy treść jest JSON-em?", unknownPath.header("Content-Type").startsWith("application/json"));
        // WYNIK: nieznana ścieżka: czy treść jest JSON-em? → false

        // DOBRA PRAKTYKA: błędy API mają stały kształt (kod, komunikat, pola) — klient pisze jedną obsługę błędów.
    }

    // =================================================================================================
    // 5. TWORZENIE ZASOBÓW
    // =================================================================================================

    /**
     * 5. POST tworzy zasób: kod 201 Created, nagłówek Location z adresem nowego zasobu i JSON zapisanego obiektu.
     */
    static void creatingResources(ApiServer api, HttpClient client) throws Exception {
        section("5. POST: 201 Created i nagłówek Location");

        URI base = api.baseUri();
        String body = new ProductDto("KUB-001", "Kubek \"XL\" żółty", new BigDecimal("19.90"), 3).toJson();
        Reply created = sendJson(client, base, "POST", "/api/v1/produkty", body);
        show("kod", created.status());
        // WYNIK: kod → 201
        show("Location", created.header("Location"));
        // WYNIK: Location → /api/v1/produkty/KUB-001
        show("treść", created.body());
        // WYNIK: treść → {"sku":"KUB-001","nazwa":"Kubek \"XL\" żółty","cena":19.90,"stan":3}

        Reply fetched = sendJson(client, base, "GET", created.header("Location"), null);
        show("GET pod adresem z Location", fetched.status() + " " + fetched.body());
        // WYNIK: GET pod adresem z Location → 200 {"sku":"KUB-001","nazwa":"Kubek \"XL\" żółty","cena":19.90,"stan":3}

        Reply again = sendJson(client, base, "POST", "/api/v1/produkty", body);
        show("ten sam POST drugi raz", again.status() + " " + ((Map<?, ?>) again.json()).get("blad"));
        // WYNIK: ten sam POST drugi raz → 409 KONFLIKT

        // DOBRA PRAKTYKA: po utworzeniu zasobu klient korzysta z adresu z Location (nie składa go samodzielnie).
        // Dlaczego: serwer może zmienić schemat adresów, a klient nadal będzie działał.
        // PUŁAPKA: zwrócenie 200 zamiast 201 działa, ale odbiera klientowi i narzędziom informację,
        // że powstał nowy zasób (i gdzie go szukać).
    }

    // =================================================================================================
    // 6. BŁĘDY WALIDACJI
    // =================================================================================================

    /**
     * 6. Trzy rodzaje „złego żądania”: zły typ treści (415), niepoprawny JSON (400), poprawny JSON z błędnymi
     * danymi (400 z listą pól). Każdy ma własny kod błędu w treści odpowiedzi.
     */
    static void validationErrors(ApiServer api, HttpClient client) throws Exception {
        section("6. Błędy: 415, 400 (niepoprawny JSON) i 400 (błędne dane)");

        URI base = api.baseUri();
        Reply wrongType = sendRaw(client, base, "POST", "/api/v1/produkty", "text/plain", "to nie jest JSON");
        show("zły Content-Type", wrongType.status() + " " + ((Map<?, ?>) wrongType.json()).get("blad"));
        // WYNIK: zły Content-Type → 415 NIEOBSLUGIWANY_TYP

        Reply broken = sendJson(client, base, "POST", "/api/v1/produkty", "{\"sku\": ");
        show("zepsuty JSON", broken.status() + " " + ((Map<?, ?>) broken.json()).get("blad"));
        // WYNIK: zepsuty JSON → 400 NIEPOPRAWNY_JSON

        Reply invalid = sendJson(client, base, "POST", "/api/v1/produkty",
                "{\"sku\":\"zle\",\"nazwa\":\"Ok\",\"cena\":\"dużo\"}");
        show("błędne dane: kod", invalid.status());
        // WYNIK: błędne dane: kod → 400
        show("błędne dane: treść", invalid.body());
        // WYNIK: błędne dane: treść → {"blad":"BLEDNE_DANE","komunikat":"Niepoprawne dane produktu","pola":["sku: wymagany kod w formacie AAA-123","cena: wymagana liczba nieujemna"]}

        // 400 = „Twoje żądanie jest błędne”, a 500 = „to nasza wina”. Dane od klienta nigdy nie powinny
        // powodować 500.

        // PUŁAPKA: 200 z treścią {"error": "..."} — klient i pośrednicy czytają kod statusu, nie treść. Taki błąd
        // trafi do pamięci podręcznej jako „sukces”, a automaty monitorujące go nie zauważą.
        // DOBRA PRAKTYKA: waliduj od razu przy wejściu (najpierw typ treści, potem składnia, potem znaczenie)
        // i zwracaj wszystkie problemy naraz. Dlaczego: użytkownik poprawia dane jednym podejściem.
    }

    // =================================================================================================
    // 7. PUT KONTRA POST
    // =================================================================================================

    static int productCount(ApiServer api, HttpClient client) throws Exception {
        return ((List<?>) sendJson(client, api.baseUri(), "GET", "/api/v1/produkty", null).json()).size();
    }

    /**
     * 7. PUT jest idempotentny: ten sam PUT wysłany dwa razy daje ten sam stan serwera (pierwszy raz tworzy — 201,
     * drugi nadpisuje identycznymi danymi — 200). POST tworzy nowy zasób przy każdym wywołaniu.
     * Dlatego po awarii sieci PUT można bezpiecznie ponowić, a POST — nie.
     */
    static void putVersusPost(ApiServer api, HttpClient client) throws Exception {
        section("7. PUT (idempotentny) kontra POST (nie)");

        URI base = api.baseUri();
        int before = productCount(api, client);
        String body = new ProductDto("PUT-001", "Zeszyt", new BigDecimal("5.50"), 10).toJson();

        Reply first = sendJson(client, base, "PUT", "/api/v1/produkty/PUT-001", body);
        int afterFirst = productCount(api, client);
        Reply second = sendJson(client, base, "PUT", "/api/v1/produkty/PUT-001", body);
        int afterSecond = productCount(api, client);
        show("PUT 1: kod, przyrost liczby produktów", first.status() + ", +" + (afterFirst - before));
        // WYNIK: PUT 1: kod, przyrost liczby produktów → 201, +1
        show("PUT 2: kod, przyrost liczby produktów", second.status() + ", +" + (afterSecond - before));
        // WYNIK: PUT 2: kod, przyrost liczby produktów → 200, +1
        show("treść obu odpowiedzi taka sama?", first.body().equals(second.body()));
        // WYNIK: treść obu odpowiedzi taka sama? → true

        Reply mismatch = sendJson(client, base, "PUT", "/api/v1/produkty/INN-001", body);
        show("PUT z innym SKU w adresie", mismatch.status());
        // WYNIK: PUT z innym SKU w adresie → 400

        // POST na zasób z identyfikatorem nadawanym przez serwer: każde wywołanie to nowe zamówienie.
        String order = "{\"sku\":\"ELE-001\",\"ilosc\":2}";
        Reply order1 = sendJson(client, base, "POST", "/api/v1/zamowienia", order);
        Reply order2 = sendJson(client, base, "POST", "/api/v1/zamowienia", order);
        show("POST 1: kod i treść", order1.status() + " " + order1.body());
        // WYNIK: POST 1: kod i treść → 201 {"id":"ZAM-1","sku":"ELE-001","ilosc":2}
        show("POST 2: kod i treść", order2.status() + " " + order2.body());
        // WYNIK: POST 2: kod i treść → 201 {"id":"ZAM-2","sku":"ELE-001","ilosc":2}
        show("liczba zamówień", ((List<?>) sendJson(client, base, "GET", "/api/v1/zamowienia", null).json()).size());
        // WYNIK: liczba zamówień → 2

        Reply badOrder = sendJson(client, base, "POST", "/api/v1/zamowienia", "{\"sku\":\"NIE-000\",\"ilosc\":0}");
        show("zamówienie z błędami", badOrder.status() + " " + ((Map<?, ?>) badOrder.json()).get("pola"));
        // WYNIK: zamówienie z błędami → 400 [sku: wymagany kod istniejącego produktu, ilosc: wymagana liczba dodatnia]

        // PUŁAPKA: klient, który „na wszelki wypadek” ponawia POST po przekroczeniu czasu, może złożyć dwa
        // zamówienia (tu: ZAM-1 i ZAM-2 z tych samych danych). Odpowiedź mogła się po prostu zgubić.
        // DOBRA PRAKTYKA: ponawiaj automatycznie tylko metody idempotentne (GET, PUT, DELETE). Dla POST użyj
        // klucza idempotentności (nagłówek, który serwer zapamiętuje i odrzuca powtórki). Dlaczego: bez tego
        // ponowienie po awarii sieci duplikuje skutki.
    }

    // =================================================================================================
    // 8. WERSJONOWANIE
    // =================================================================================================

    /**
     * 8. Gdy trzeba zmienić kształt odpowiedzi (np. cena z liczby na obiekt), nie psujemy istniejących klientów:
     * wersję umieszczamy w ścieżce (/api/v1/..., /api/v2/...) i obsługujemy obie jednocześnie.
     */
    static void versioning(ApiServer api, HttpClient client) throws Exception {
        section("8. Wersjonowanie API w ścieżce: /api/v1 i /api/v2");

        URI base = api.baseUri();
        show("v1", sendJson(client, base, "GET", "/api/v1/produkty/KSI-001", null).body());
        // WYNIK: v1 → {"sku":"KSI-001","nazwa":"Czysty kod","cena":79.00,"stan":15}
        show("v2", sendJson(client, base, "GET", "/api/v2/produkty/KSI-001", null).body());
        // WYNIK: v2 → {"sku":"KSI-001","nazwa":"Czysty kod","cena":{"kwota":79.00,"waluta":"PLN"},"stan":15}

        // PUŁAPKA: zmiana typu pola (liczba → obiekt) lub jego nazwy w istniejącej wersji psuje wszystkich
        // klientów, którzy jeszcze jej używają — i to dopiero po wdrożeniu. Dodanie NOWEGO pola jest zwykle
        // bezpieczne, jeśli klienci ignorują nieznane pola.
        // DOBRA PRAKTYKA: zmiany niezgodne wstecz = nowa wersja (/v2), starą utrzymuj do końca ustalonego okresu.
        // Inne sposoby wersjonowania: nagłówek (Accept: application/vnd.sklep.v2+json) lub parametr — ścieżka
        // jest najprostsza do przetestowania w przeglądarce i w logach.
    }

    // =================================================================================================
    // 9. CO ROBIĄ BIBLIOTEKI
    // =================================================================================================

    /**
     * 9. Cały ten kod parsowania, zapisu, walidacji i obsługi błędów w prawdziwych projektach załatwiają Jackson
     * (JSON ↔ obiekty) oraz Spring MVC (routing, kody, błędy). Ta sekcja tylko pokazuje, jak mało zostaje do
     * napisania „po wierzchu” (szkic w Springu jest w komentarzu wewnątrz metody).
     */
    static void whatLibrariesDo() {
        section("9. Co automatyzują Jackson i Spring MVC");

        // Szkic kontrolera w Springu (komentarz — to inna biblioteka, więc tu się nie kompiluje):
        //   @RestController                                  // klasa obsługuje żądania HTTP i zwraca JSON
        //   @RequestMapping("/api/v1/produkty")              // wspólny początek ścieżki
        //   class ProduktController {
        //       @GetMapping("/{sku}")                        // GET /api/v1/produkty/ELE-001
        //       ProductDto get(@PathVariable String sku) { ... }   // JSON ↔ rekord robi Jackson
        //
        //       @PostMapping                                 // POST /api/v1/produkty
        //       ResponseEntity<ProductDto> create(@Valid @RequestBody ProductDto dto) {  // @Valid = walidacja
        //           return ResponseEntity.created(URI.create("/api/v1/produkty/" + dto.sku())).body(dto); // 201 + Location
        //       }
        //   }

        String[][] mapping = {
            {"Json.write / toMap", "ObjectMapper.writeValueAsString(dto)"},
            {"Json.parse + toDto", "ObjectMapper.readValue(json, ProductDto.class)"},
            {"validate(...)", "adnotacje @NotBlank, @PositiveOrZero + @Valid"},
            {"switch po metodzie i ścieżce", "@GetMapping, @PostMapping, @PathVariable"},
            {"writeError(404, ...)", "@ExceptionHandler / @ControllerAdvice"},
            {"nagłówek Location + 201", "ResponseEntity.created(uri)"},
        };
        for (String[] row : mapping) {
            note(row[0] + "  →  " + row[1]);
        }
        // WYNIK: ℹ Json.write / toMap  →  ObjectMapper.writeValueAsString(dto)
        // WYNIK: ℹ Json.parse + toDto  →  ObjectMapper.readValue(json, ProductDto.class)
        // WYNIK: ℹ validate(...)  →  adnotacje @NotBlank, @PositiveOrZero + @Valid
        // WYNIK: ℹ switch po metodzie i ścieżce  →  @GetMapping, @PostMapping, @PathVariable
        // WYNIK: ℹ writeError(404, ...)  →  @ExceptionHandler / @ControllerAdvice
        // WYNIK: ℹ nagłówek Location + 201  →  ResponseEntity.created(uri)

        // PUŁAPKA: biblioteka nie zwalnia z myślenia o kontrakcie API: nadal ustalasz kody, kształt błędów,
        // wersje i walidację. Znajomość „gołego” HTTP pomaga zrozumieć, co robi adnotacja i dlaczego dostajesz 415 lub 400.
        // DOBRA PRAKTYKA: w prawdziwym projekcie używaj sprawdzonej biblioteki JSON, nie własnego parsera
        // (nasz nie zna wszystkich przypadków brzegowych). Dlaczego: parsery to częsty cel ataków i źródło subtelnych błędów.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • JSON: obiekt {...}, tablica [...], napis, liczba, true/false, null; kodowanie UTF-8;
     *     Content-Type: application/json; charset=utf-8.
     *   • JSON zapisuj funkcją, która „ucieka” znaki specjalne — NIGDY nie sklejaj z tekstów.
     *   • Liczby czytaj jako BigDecimal (pieniądze!), nie double.
     *   • Kolejność przy POST/PUT: typ treści (415) → składnia (400) → walidacja pól (400) → konflikt (409) → zapis.
     *   • 201 + Location dla utworzonego, 204 dla usuniętego bez treści, 404 dla braku zasobu, 400 dla złych danych.
     *   • Błąd ma stały kształt (kod, komunikat, pola) i nigdy nie ujawnia szczegółów wewnętrznych.
     *   • PUT = idempotentny (zapisz dokładnie tak); POST = nowy zasób przy każdym wywołaniu.
     *   • Wersjonuj w ścieżce: zmiany niezgodne wstecz → /v2.
     *   • Jackson robi JSON ↔ obiekty, Spring MVC — routing, kody i błędy.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego nie wolno budować JSON-a przez sklejanie tekstów? Podaj dwa powody.
     *   2. Dlaczego parser zwraca liczby jako BigDecimal?
     *   3. Co wypisze:  Json.write(Map.of("a", List.of(1, 2)));  ?
     *   4. Co wypisze:  Json.parse("[1, 2.50, \"x\"]");  ?
     *   5. ZNAJDŹ BŁĄD:  serwer odpowiada kodem 200 i treścią {"blad":"brak produktu"} gdy produktu nie ma.
     *   6. Który kod zwrócisz: (a) POST bez nagłówka Content-Type, (b) POST z poprawnym JSON-em, ale ceną ujemną,
     *      (c) POST produktu, który już istnieje, (d) PUT tego samego produktu drugi raz?
     *   7. Dlaczego ponowienie PUT jest bezpieczne, a POST — nie? Jak zabezpieczyć POST?
     *   8. Co wypisze:  Json.write("a\"b");  ?
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
        try (ApiServer api = new ApiServer()) {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                    .version(HttpClient.Version.HTTP_1_1).build();
            ProductDto dto = new ProductDto("BIU-001", "Długopis żelowy", new BigDecimal("3.99"), 40);
            String expectedJson = "{\"sku\":\"KSI-009\",\"nazwa\":\"Kubek \\\"XL\\\"\",\"cena\":19.90,\"stan\":3}";
            ProductDto quoted = new ProductDto("KSI-009", "Kubek \"XL\"", new BigDecimal("19.90"), 3);
            String array = "[{\"sku\":\"A-1\",\"x\":1},{\"sku\":\"B-2\"},{\"sku\":\"C-3\"}]";

            section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
            Check.equal("ćw. 1: poprawny SKU", true, () -> exercise1("ELE-001"));
            Check.equal("ćw. 1: mała litera", false, () -> exercise1("ele-001"));
            Check.equal("ćw. 1: za dużo cyfr", false, () -> exercise1("ELE-0012"));
            Check.equal("ćw. 2: JSON produktu", expectedJson, () -> exercise2(quoted));
            Check.equal("ćw. 3: lista SKU", List.of("A-1", "B-2", "C-3"), () -> exercise3(array));
            Check.equal("ćw. 4: nazwa z serwera", "Długopis żelowy", () -> call(() -> exercise4(client, api.baseUri(), dto)));
            Check.summary();

            section("ĆWICZENIA — rozwiązania wzorcowe");
            Check.equal("ćw. 1 (wzorzec): poprawny", true, () -> solution1("ELE-001"));
            Check.equal("ćw. 1 (wzorzec): mała litera", false, () -> solution1("ele-001"));
            Check.equal("ćw. 1 (wzorzec): za dużo cyfr", false, () -> solution1("ELE-0012"));
            Check.equal("ćw. 2 (wzorzec)", expectedJson, () -> solution2(quoted));
            Check.equal("ćw. 3 (wzorzec)", List.of("A-1", "B-2", "C-3"), () -> solution3(array));
            Check.equal("ćw. 4 (wzorzec)", "Długopis żelowy", () -> call(() -> solution4(client, api.baseUri(), dto)));
            Check.summary();
            // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
        }
    }

    /**
     * ĆWICZENIE 1 (łatwe): czy tekst jest poprawnym kodem SKU — trzy wielkie litery, myślnik, dokładnie trzy cyfry
     * (np. ELE-001)?
     * Podpowiedź: String.matches z wyrażeniem regularnym {@code [A-Z]{3}-\d{3}} (w Javie: "[A-Z]{3}-\\d{3}").
     */
    static boolean exercise1(String sku) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zamień rekord na tekst JSON, używając Json.write (bez sklejania tekstów).
     * PRZEPISZ:
     * <pre>{@code
     * return "{\"sku\":\"" + dto.sku() + "\",\"nazwa\":\"" + dto.name() + "\",...}";   // pęknie przy cudzysłowie w nazwie
     * }</pre>
     * Podpowiedź: dto.toMap() już zwraca pola w dobrej kolejności.
     */
    static String exercise2(ProductDto dto) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): z tekstu JSON-owej tablicy obiektów wyciągnij listę wartości pola "sku" (w kolejności).
     * Podpowiedź: Json.parse zwraca {@code List<Object>} (rzutuj na {@code List<?>}), a elementy to {@code Map<?, ?>}.
     */
    static List<String> exercise3(String jsonArray) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): jako klient wyślij POST /api/v1/produkty z JSON-em produktu, a potem pobierz GET
     * pod adresem z nagłówka Location i zwróć pole "nazwa" z odpowiedzi. Gdy POST nie zwróci 201 — rzuć
     * IllegalStateException.
     * Podpowiedź: gotowe są metody sendJson(...) i Reply.json(); dto.toJson() daje treść żądania.
     */
    static String exercise4(HttpClient client, URI base, ProductDto dto) throws Exception {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(String sku) {
        return sku.matches("[A-Z]{3}-\\d{3}");
    }

    static String solution2(ProductDto dto) {
        return Json.write(dto.toMap());
    }

    static List<String> solution3(String jsonArray) {
        List<String> result = new ArrayList<>();
        for (Object element : (List<?>) Json.parse(jsonArray)) {
            result.add((String) ((Map<?, ?>) element).get("sku"));
        }
        return result;
    }

    static String solution4(HttpClient client, URI base, ProductDto dto) throws Exception {
        Reply created = sendJson(client, base, "POST", "/api/v1/produkty", dto.toJson());
        if (created.status() != 201) {
            throw new IllegalStateException("oczekiwano 201, jest " + created.status());
        }
        Reply fetched = sendJson(client, base, "GET", created.header("Location"), null);
        return (String) ((Map<?, ?>) fetched.json()).get("nazwa");
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. (a) Znaki specjalne w danych (cudzysłów, ukośnik, nowa linia) psują składnię; (b) to luka bezpieczeństwa —
     *      ktoś może wstrzyknąć własne pola lub zmienić znaczenie dokumentu. Zapis, który „ucieka” znaki, tego unika.
     *   2. Liczby w JSON to tekst cyfr bez typu. double nie zapisuje dokładnie wielu ułamków dziesiętnych
     *      (0.1 + 0.2 ≠ 0.3), co jest niedopuszczalne przy pieniądzach; BigDecimal przechowuje liczbę dokładnie.
     *   3. {"a":[1,2]}
     *   4. [1, 2.50, x] — lista z BigDecimal 1, BigDecimal 2.50 (zachowana skala) i napisem x (bez cudzysłowów,
     *      bo to wydruk listy Javy, a nie JSON).
     *   5. Kod 200 mówi „sukces”, a treść mówi o błędzie — klient i narzędzia czytają kod, więc uznają to za
     *      sukces. Powinno być 404 (z treścią błędu).
     *   6. (a) 415, (b) 400, (c) 409, (d) 200 (zasób już istniał; pierwszy PUT utworzył go z kodem 201).
     *   7. PUT zapisuje zasób dokładnie takim, jak przysłano — drugie wywołanie daje ten sam stan. POST przy
     *      każdym wywołaniu tworzy nowy zasób, więc ponowienie duplikuje skutek. Zabezpieczenie: klucz
     *      idempotentności (nagłówek, który serwer zapamiętuje i odrzuca powtórki) albo sprawdzenie duplikatów.
     *   8. "a\"b" — z cudzysłowami zewnętrznymi i cudzysłów wewnątrz poprzedzony ukośnikiem.
     */
    // </editor-fold>
}
