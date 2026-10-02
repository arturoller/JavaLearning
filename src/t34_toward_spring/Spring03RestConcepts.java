package t34_toward_spring;

import com.sun.net.httpserver.HttpServer;
import helpers.Check;
import helpers.SampleData;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: REST od środka — zasady, mini-framework z adnotacjami, JSON i prawdziwy serwer HTTP
 *        (REST = Representational State Transfer = przesyłanie reprezentacji stanu; mapping = mapowanie;
 *         path variable = zmienna ścieżki; request param = parametr zapytania; request body = ciało żądania)
 *
 * W SKRÓCIE:
 *   REST to styl projektowania API: rzeczowniki (zasoby) w adresach, czasowniki w metodach HTTP, znaczenie
 *   w kodach statusu. Budujemy mini „Spring MVC”: adnotacje @GetMapping/@PostMapping na metodach, zmienne
 *   ścieżki {sku}, parametry zapytania, JSON z rekordów, jeden „doradca” błędów — i uruchamiamy to na
 *   HttpServer z JDK, wołając klientem HttpClient.
 *
 * ANALOGIA:
 *   Biblioteka: każda książka ma stałą sygnaturę (URI zasobu: /ksiazki/123). Czytelnik mówi, CO chce zrobić
 *   (pokaż = GET, dodaj = POST, podmień = PUT, wycofaj = DELETE), a bibliotekarz odpowiada krótkim kodem:
 *   „proszę” (200), „dopisane” (201), „nie ma” (404), „źle wypełniony rewers” (400).
 *
 * JAK TO DZIAŁA:
 *   HTTP ─► HttpServer ─► Router: (metoda, ścieżka) → metoda kontrolera z adnotacji
 *                          │   argumenty: {sku} z ścieżki, ?kategoria= z zapytania, JSON z ciała → rekord
 *                          ▼
 *                      kontroler zwraca rekord / listę / ResponseEntity
 *                          ▼
 *            Json.toJson(...) ─► status + Content-Type: application/json ─► klient
 *            wyjątek ─► ErrorAdvice ─► {"status":404,"error":"...","path":"..."}
 *
 * SŁÓWKA:
 *   resource = zasób; endpoint = punkt końcowy (adres + metoda); idempotent = idempotentny (powtórzenie daje ten
 *   sam stan); safe = bezpieczny (nie zmienia stanu); stateless = bezstanowy; header = nagłówek;
 *   content negotiation = negocjacja treści; advice = porada (tu: globalna obsługa błędów); route = trasa
 *
 * ZOBACZ TEŻ: t28_networking_http/Http03LocalServer (HttpServer), t28_networking_http/Http02HttpClient (klient),
 *   t28_networking_http/Http04JsonApi (JSON ręcznie), t19_annotations_reflection/Annotations05MiniFramework,
 *   t34_toward_spring/Spring02Layers (warstwy pod kontrolerem)
 * </pre>
 */
public class Spring03RestConcepts {

    public static void main(String[] args) {
        title("Spring03 — REST: zasady, mini-framework, JSON i HTTP");

        resourcesAndUris();      // resources and URIs = zasoby i adresy
        methodsAndStatuses();    // methods and statuses = metody i kody statusu
        routing();               // routing = trasowanie (wybór metody kontrolera)
        pathAndQuery();          // path and query = ścieżka i zapytanie
        json();                  // JSON = format wymiany danych
        idempotency();           // idempotency = idempotentność
        errorHandling();         // error handling = obsługa błędów
        realHttp();              // real HTTP = prawdziwe HTTP
        negotiationAndVersions(); // negotiation and versions = negocjacja treści i wersje
        compareWithSpringMvc();  // compare with Spring MVC = porównanie ze Spring MVC
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. ZASOBY I ADRESY (URI)
    // =================================================================================================

    /**
     * 1. Zasób = rzecz, o której mówi API (produkt, zamówienie). URI wskazuje zasób rzeczownikiem w liczbie
     * mnogiej; to, CO robimy, mówi metoda HTTP — nie adres. Hierarchia w ścieżce: /zamowienia/7/pozycje.
     */
    static void resourcesAndUris() {
        section("1. Zasoby i adresy: rzeczowniki w URI, czasowniki w metodzie");

        Map<String, String> good = new LinkedHashMap<>();
        good.put("GET /produkty", "lista produktów");
        good.put("GET /produkty/ELE-001", "jeden produkt");
        good.put("GET /produkty?kategoria=KSIAZKI", "lista przefiltrowana (parametr zapytania)");
        good.put("POST /produkty", "utwórz produkt (dane w ciele JSON)");
        good.put("PUT /produkty/ELE-001/stan", "podmień stan magazynowy");
        good.put("DELETE /produkty/ELE-001", "usuń produkt");
        showEach("dobrze", good);
        // WYNIK: dobrze (liczba kluczy: 6):
        // WYNIK: • GET /produkty → lista produktów
        // WYNIK: • GET /produkty/ELE-001 → jeden produkt
        // WYNIK: • GET /produkty?kategoria=KSIAZKI → lista przefiltrowana (parametr zapytania)
        // WYNIK: • POST /produkty → utwórz produkt (dane w ciele JSON)
        // WYNIK: • PUT /produkty/ELE-001/stan → podmień stan magazynowy
        // WYNIK: • DELETE /produkty/ELE-001 → usuń produkt
        show("źle", List.of("GET /pobierzProdukt?id=1", "POST /usunProdukt/1", "GET /produkty/1/usun"));
        // WYNIK: źle → [GET /pobierzProdukt?id=1, POST /usunProdukt/1, GET /produkty/1/usun]

        // PUŁAPKA: GET, który coś zmienia (np. GET /produkty/1/usun) — przeglądarki, roboty i pamięci podręczne
        // (cache) mogą go wywołać same, bo GET ma być bezpieczny. Usunięcie = DELETE.
        // Inne zasady REST: BEZSTANOWOŚĆ — każde żądanie niesie komplet informacji (np. token w nagłówku
        // Authorization), serwer nie pamięta „rozmowy”, więc dowolna kopia serwera może je obsłużyć;
        // HATEOAS (= hipermedia jako silnik stanu aplikacji) — odpowiedź zawiera linki do dalszych akcji,
        // np. "_links": {"anuluj": "/zamowienia/7/anulowanie"}; w praktyce stosowany rzadko.
        // W Springu: @RestController @RequestMapping("/produkty") class ProductController { ... };
        //            HATEOAS — moduł spring-boot-starter-hateoas (EntityModel, linkTo(...)).
    }

    // =================================================================================================
    // 2. METODY HTTP I KODY STATUSU
    // =================================================================================================

    /**
     * 2. Każda metoda ma znaczenie: bezpieczna = tylko czyta; idempotentna = wysłana N razy zostawia serwer
     * w tym samym stanie co raz. Kody statusu: 2xx sukces, 4xx błąd klienta, 5xx błąd serwera.
     */
    static void methodsAndStatuses() {
        section("2. Metody HTTP (bezpieczna? idempotentna?) i kody statusu");

        Map<String, String> methods = new LinkedHashMap<>();
        methods.put("GET", "odczyt; bezpieczna, idempotentna");
        methods.put("POST", "utworzenie / akcja; NIE idempotentna (2× = 2 zasoby)");
        methods.put("PUT", "podmiana całości; idempotentna");
        methods.put("PATCH", "zmiana części; zwykle NIE idempotentna");
        methods.put("DELETE", "usunięcie; idempotentna (stan po 2× ten sam)");
        showEach("metody", methods);
        // WYNIK: metody (liczba kluczy: 5):
        // WYNIK: • GET → odczyt; bezpieczna, idempotentna
        // WYNIK: • POST → utworzenie / akcja; NIE idempotentna (2× = 2 zasoby)
        // WYNIK: • PUT → podmiana całości; idempotentna
        // WYNIK: • PATCH → zmiana części; zwykle NIE idempotentna
        // WYNIK: • DELETE → usunięcie; idempotentna (stan po 2× ten sam)

        Map<Integer, String> statuses = new TreeMap<>();
        statuses.put(200, "OK — jest wynik");
        statuses.put(201, "Created — utworzono (często z nagłówkiem Location)");
        statuses.put(204, "No Content — sukces bez treści (np. DELETE)");
        statuses.put(400, "Bad Request — złe dane od klienta");
        statuses.put(401, "Unauthorized — nie wiem, kim jesteś");
        statuses.put(403, "Forbidden — wiem, ale nie wolno");
        statuses.put(404, "Not Found — nie ma zasobu");
        statuses.put(405, "Method Not Allowed — zasób jest, metoda nie");
        statuses.put(406, "Not Acceptable — nie umiem odpowiedzieć w żądanym formacie");
        statuses.put(409, "Conflict — konflikt ze stanem (duplikat, reguła)");
        statuses.put(500, "Internal Server Error — błąd po stronie serwera");
        showEach("statusy", statuses);
        // WYNIK: statusy (liczba kluczy: 11):
        // WYNIK: • 200 → OK — jest wynik
        // WYNIK: • 201 → Created — utworzono (często z nagłówkiem Location)
        // WYNIK: • 204 → No Content — sukces bez treści (np. DELETE)
        // WYNIK: • 400 → Bad Request — złe dane od klienta
        // WYNIK: • 401 → Unauthorized — nie wiem, kim jesteś
        // WYNIK: • 403 → Forbidden — wiem, ale nie wolno
        // WYNIK: • 404 → Not Found — nie ma zasobu
        // WYNIK: • 405 → Method Not Allowed — zasób jest, metoda nie
        // WYNIK: • 406 → Not Acceptable — nie umiem odpowiedzieć w żądanym formacie
        // WYNIK: • 409 → Conflict — konflikt ze stanem (duplikat, reguła)
        // WYNIK: • 500 → Internal Server Error — błąd po stronie serwera

        // PUŁAPKA: „200 OK” z treścią {"error": "nie znaleziono"} — klient (i monitoring) widzi sukces.
        // Status ma mówić prawdę; szczegóły dopiero w ciele.
        // W Springu: ResponseEntity.status(HttpStatus.CREATED).body(dto), ResponseEntity.noContent().build(),
        //            albo @ResponseStatus(HttpStatus.CREATED) na metodzie kontrolera.
    }

    // =================================================================================================
    // MINI-FRAMEWORK: ADNOTACJE, KONTROLER, ROUTER, JSON, OBSŁUGA BŁĘDÓW
    // =================================================================================================

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) @interface GetMapping { String value(); }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) @interface PostMapping { String value(); }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) @interface PutMapping { String value(); }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) @interface DeleteMapping { String value(); }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER) @interface PathVariable { String value(); }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER) @interface RequestParam {
        String value();
        boolean required() default true;                                    // required = wymagany
    }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER) @interface RequestBody { }

    /** DTO produktu w API. */
    record ProductDto(String sku, String name, String category, BigDecimal price, int stock) { }
    record NewProduct(String sku, String name, String category, BigDecimal price) { }
    record StockUpdate(int stock) { }                                       // stock update = zmiana stanu

    /** Odpowiedź z jawnym statusem (jak ResponseEntity w Springu). */
    record ResponseEntity(int status, Object body) { }

    /** Żądanie i odpowiedź „wewnątrz” frameworka (niezależne od HttpServer — łatwe do testów). */
    record Request(String method, String path, Map<String, String> query, String body, String accept) {
        static Request of(String method, String pathAndQuery, String body) {
            int q = pathAndQuery.indexOf('?');
            Map<String, String> query = new TreeMap<>();
            if (q >= 0) {
                for (String pair : pathAndQuery.substring(q + 1).split("&")) {
                    String[] kv = pair.split("=", 2);
                    query.put(decode(kv[0]), kv.length > 1 ? decode(kv[1]) : "");
                }
            }
            return new Request(method, q >= 0 ? pathAndQuery.substring(0, q) : pathAndQuery, query, body, "application/json");
        }
        Request withAccept(String type) { return new Request(method, path, query, body, type); }
    }

    record Response(int status, String contentType, String body) {
        @Override public String toString() { return status + " " + body; }
    }

    static String decode(String s) { return URLDecoder.decode(s, StandardCharsets.UTF_8); }   // (Java 10+)

    /** Wyjątek z gotowym statusem HTTP (jak ResponseStatusException). */
    static final class ApiException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final int status;
        ApiException(int status, String message) { super(message); this.status = status; }
    }

    /** Kontroler: zwykła klasa z adnotacjami. Dane w pamięci (TreeMap = stała kolejność). */
    static class ProductController {
        private final Map<String, ProductDto> products = new TreeMap<>();

        ProductController() {
            SampleData.products().forEach(p -> products.put(p.sku(),
                    new ProductDto(p.sku(), p.name(), p.category().name(), p.price(), p.stock())));
        }

        @GetMapping("/produkty")
        List<ProductDto> list(@RequestParam(value = "kategoria", required = false) String category,
                              @RequestParam(value = "maxCena", required = false) BigDecimal maxPrice) {
            return products.values().stream()
                    .filter(p -> category == null || p.category().equals(category))
                    .filter(p -> maxPrice == null || p.price().compareTo(maxPrice) <= 0)
                    .toList();                                              // (Java 16+)
        }

        @GetMapping("/produkty/{sku}")
        ProductDto getOne(@PathVariable("sku") String sku) {
            ProductDto p = products.get(sku);
            if (p == null) throw new NoSuchElementException("nie ma produktu " + sku);
            return p;
        }

        @PostMapping("/produkty")
        ResponseEntity create(@RequestBody NewProduct body) {
            if (products.containsKey(body.sku())) throw new ApiException(409, "produkt " + body.sku() + " już istnieje");
            ProductDto dto = new ProductDto(body.sku(), body.name(), body.category(), body.price(), 0);
            products.put(dto.sku(), dto);
            return new ResponseEntity(201, dto);
        }

        @PutMapping("/produkty/{sku}/stan")
        ProductDto setStock(@PathVariable("sku") String sku, @RequestBody StockUpdate update) {
            ProductDto p = getOne(sku);
            ProductDto changed = new ProductDto(p.sku(), p.name(), p.category(), p.price(), update.stock());
            products.put(sku, changed);
            return changed;
        }

        @DeleteMapping("/produkty/{sku}")
        ResponseEntity delete(@PathVariable("sku") String sku) {
            if (products.remove(sku) == null) throw new NoSuchElementException("nie ma produktu " + sku);
            return new ResponseEntity(204, null);
        }

        @GetMapping("/awaria")
        String crash() { throw new IllegalStateException("połączenie z bazą db-prod:5432 zerwane"); }   // crash = awaria
    }

    /** Globalna obsługa błędów (jak @RestControllerAdvice): wyjątek → status + JSON. */
    static final class ErrorAdvice {
        record ErrorBody(int status, String error, String path) { }

        Response handle(Throwable e, Request req) {
            int status;
            String message;
            if (e instanceof ApiException api) { status = api.status; message = api.getMessage(); }
            else if (e instanceof NoSuchElementException) { status = 404; message = e.getMessage(); }
            else if (e instanceof IllegalArgumentException) { status = 400; message = e.getMessage(); }
            else { status = 500; message = "błąd serwera"; }                // szczegóły tylko do logu
            return new Response(status, "application/json", Json.toJson(new ErrorBody(status, message, req.path())));
        }
    }

    /** Router: czyta adnotacje kontrolerów i dopasowuje żądania do metod. */
    static final class Router {
        record Route(String httpMethod, String template, Pattern regex, List<String> vars, Object controller, Method handler) { }

        private final List<Route> routes = new ArrayList<>();
        private final ErrorAdvice advice = new ErrorAdvice();

        Router(Object... controllers) {
            for (Object c : controllers) {
                List<Method> methods = Arrays.stream(c.getClass().getDeclaredMethods())
                        .sorted(Comparator.comparing(Method::getName)).toList();   // kolejność z refleksji → sortuj!
                for (Method m : methods) {
                    if (m.isAnnotationPresent(GetMapping.class)) add("GET", m.getAnnotation(GetMapping.class).value(), c, m);
                    if (m.isAnnotationPresent(PostMapping.class)) add("POST", m.getAnnotation(PostMapping.class).value(), c, m);
                    if (m.isAnnotationPresent(PutMapping.class)) add("PUT", m.getAnnotation(PutMapping.class).value(), c, m);
                    if (m.isAnnotationPresent(DeleteMapping.class)) add("DELETE", m.getAnnotation(DeleteMapping.class).value(), c, m);
                }
            }
            routes.sort(Comparator.comparing(Route::template).thenComparing(Route::httpMethod));
        }

        /** "/produkty/{sku}" → wyrażenie regularne "/produkty/([^/]+)" + lista nazw zmiennych [sku]. */
        private void add(String httpMethod, String template, Object controller, Method m) {
            List<String> vars = new ArrayList<>();
            Matcher matcher = Pattern.compile("\\{(\\w+)}").matcher(template);
            StringBuilder regex = new StringBuilder();
            int last = 0;
            while (matcher.find()) {
                regex.append(Pattern.quote(template.substring(last, matcher.start()))).append("([^/]+)");
                vars.add(matcher.group(1));
                last = matcher.end();
            }
            regex.append(Pattern.quote(template.substring(last)));
            routes.add(new Route(httpMethod, template, Pattern.compile(regex.toString()), vars, controller, m));
        }

        List<String> describe() {
            return routes.stream()
                    .map(r -> r.httpMethod() + " " + r.template() + " → " + r.handler().getName())
                    .toList();
        }

        Response handle(Request req) {
            try {
                List<Route> pathMatches = routes.stream().filter(r -> r.regex().matcher(req.path()).matches()).toList();
                if (pathMatches.isEmpty()) throw new ApiException(404, "brak zasobu " + req.path());
                Route route = pathMatches.stream().filter(r -> r.httpMethod().equals(req.method())).findFirst()
                        .orElseThrow(() -> new ApiException(405, "metoda " + req.method() + " niedozwolona dla " + req.path()));
                Object result = route.handler().invoke(route.controller(), arguments(route, req));
                return render(result, req);
            } catch (InvocationTargetException e) {
                return advice.handle(e.getCause(), req);                    // wyjątek z metody kontrolera
            } catch (Exception e) {
                return advice.handle(e, req);                               // nasz (404/405/400) albo refleksji
            }
        }

        private Object[] arguments(Route route, Request req) {
            Matcher m = route.regex().matcher(req.path());
            if (!m.matches()) throw new IllegalStateException("nie powinno się zdarzyć");
            Parameter[] params = route.handler().getParameters();
            Object[] args = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                Parameter p = params[i];
                if (p.isAnnotationPresent(PathVariable.class)) {
                    String name = p.getAnnotation(PathVariable.class).value();
                    args[i] = convert(m.group(route.vars().indexOf(name) + 1), p.getType(), name);
                } else if (p.isAnnotationPresent(RequestParam.class)) {
                    RequestParam rp = p.getAnnotation(RequestParam.class);
                    String raw = req.query().get(rp.value());
                    if (raw == null && rp.required()) throw new IllegalArgumentException("brak parametru '" + rp.value() + "'");
                    args[i] = raw == null ? null : convert(raw, p.getType(), rp.value());
                } else if (p.isAnnotationPresent(RequestBody.class)) {
                    args[i] = Json.fromJson(req.body(), p.getType().asSubclass(Record.class));
                }
            }
            return args;
        }

        private Response render(Object result, Request req) {
            int status = 200;
            Object body = result;
            if (result instanceof ResponseEntity re) { status = re.status(); body = re.body(); }
            if (body == null) return new Response(status, "", "");
            if (req.accept().equals("text/plain")) return new Response(status, "text/plain", String.valueOf(body));
            if (!req.accept().equals("application/json") && !req.accept().equals("*/*")) {
                throw new ApiException(406, "nieobsługiwany format " + req.accept());
            }
            return new Response(status, "application/json", Json.toJson(body));
        }
    }

    /** Zamiana tekstu z URI na typ parametru. */
    static Object convert(String raw, Class<?> type, String name) {
        try {
            if (type == String.class) return raw;
            if (type == int.class) return Integer.parseInt(raw);
            if (type == long.class) return Long.parseLong(raw);
            if (type == BigDecimal.class) return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("parametr '" + name + "': '" + raw + "' to nie liczba");
        }
        throw new IllegalStateException("nieobsługiwany typ " + type.getSimpleName());
    }

    /** Mini mapper JSON: rekordy, listy, mapy, teksty, liczby, logiczne, null. Czytanie: płaskie obiekty → rekord. */
    static final class Json {
        private Json() { }

        static String toJson(Object o) {
            if (o == null) return "null";
            if (o instanceof String s) return quote(s);
            if (o instanceof BigDecimal b) return b.toPlainString();         // bez notacji 1E+3
            if (o instanceof Number || o instanceof Boolean) return o.toString();
            if (o instanceof Enum<?> e) return quote(e.name());
            if (o instanceof Record r) {
                StringBuilder sb = new StringBuilder("{");
                for (RecordComponent rc : r.getClass().getRecordComponents()) {
                    if (sb.length() > 1) sb.append(',');
                    sb.append(quote(rc.getName())).append(':').append(toJson(read(rc, r)));
                }
                return sb.append('}').toString();
            }
            if (o instanceof Collection<?> c) {
                List<String> parts = new ArrayList<>();
                c.forEach(x -> parts.add(toJson(x)));
                return "[" + String.join(",", parts) + "]";
            }
            if (o instanceof Map<?, ?> m) {
                List<String> parts = new ArrayList<>();
                m.forEach((k, v) -> parts.add(quote(String.valueOf(k)) + ":" + toJson(v)));
                return "{" + String.join(",", parts) + "}";
            }
            throw new IllegalArgumentException("nie umiem zamienić na JSON: " + o.getClass().getSimpleName());
        }

        static String quote(String s) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
        }

        private static Object read(RecordComponent rc, Record r) {
            try {
                return rc.getAccessor().invoke(r);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        /** Płaski obiekt JSON → rekord przez kanoniczny konstruktor (canonical = kanoniczny, „główny”). */
        static <T extends Record> T fromJson(String json, Class<T> type) {
            Map<String, Object> fields = new FlatParser(json).parseObject();
            RecordComponent[] rcs = type.getRecordComponents();
            Object[] args = new Object[rcs.length];
            Class<?>[] types = new Class<?>[rcs.length];
            for (int i = 0; i < rcs.length; i++) {
                types[i] = rcs[i].getType();
                if (!fields.containsKey(rcs[i].getName())) throw new IllegalArgumentException("brak pola '" + rcs[i].getName() + "'");
                args[i] = fromValue(fields.get(rcs[i].getName()), types[i], rcs[i].getName());
            }
            try {
                Constructor<T> canonical = type.getDeclaredConstructor(types);
                return canonical.newInstance(args);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        private static Object fromValue(Object v, Class<?> type, String name) {
            if (type == String.class && (v == null || v instanceof String)) return v;
            if (type == boolean.class && v instanceof Boolean) return v;
            if (v instanceof BigDecimal b) {
                if (type == int.class) return b.intValueExact();
                if (type == long.class) return b.longValueExact();
                if (type == BigDecimal.class) return b;
            }
            throw new IllegalArgumentException("pole '" + name + "': zły typ wartości " + v);
        }
    }

    /** Bardzo mały parser płaskiego obiektu JSON: {"k": "tekst", "n": 12.5, "b": true, "x": null}. */
    static final class FlatParser {
        private final String s;
        private int i;
        FlatParser(String s) { this.s = s == null ? "" : s; }

        Map<String, Object> parseObject() {
            Map<String, Object> result = new LinkedHashMap<>();
            expect('{');
            skipSpaces();
            if (peek() == '}') { i++; return result; }
            do {
                skipSpaces();
                String key = parseString();
                expect(':');
                result.put(key, parseValue());
                skipSpaces();
            } while (tryConsume(','));
            expect('}');
            return result;
        }

        private Object parseValue() {
            skipSpaces();
            char c = peek();
            if (c == '"') return parseString();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (start == i) throw error("wartości");
            return new BigDecimal(s.substring(start, i));
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (i < s.length() && s.charAt(i) != '"') {
                char c = s.charAt(i++);
                if (c == '\\' && i < s.length()) {
                    char e = s.charAt(i++);
                    sb.append(e == 'n' ? '\n' : e);                         // \" → ", \\ → \, \n → nowa linia
                } else {
                    sb.append(c);
                }
            }
            expect('"');
            return sb.toString();
        }

        private void expect(char c) {
            skipSpaces();
            if (peek() != c) throw error("'" + c + "'");
            i++;
        }

        private boolean tryConsume(char c) {
            skipSpaces();
            if (peek() == c) { i++; return true; }
            return false;
        }

        private char peek() { return i < s.length() ? s.charAt(i) : '\0'; }
        private void skipSpaces() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        private IllegalArgumentException error(String what) {
            return new IllegalArgumentException("niepoprawny JSON: oczekiwano " + what + " na pozycji " + i);
        }
    }

    // =================================================================================================
    // 3. TRASOWANIE Z ADNOTACJI
    // =================================================================================================

    /**
     * 3. Router przegląda metody kontrolera (posortowane po nazwie — refleksja nie gwarantuje kolejności),
     * czyta adnotacje @GetMapping/@PostMapping/... i buduje tabelę tras. Szablon {sku} zamieniamy na wyrażenie
     * regularne, żeby dopasować np. /produkty/ELE-001.
     */
    static void routing() {
        section("3. Trasowanie: adnotacje na metodach → tabela tras");

        Router router = new Router(new ProductController());
        show("trasy", router.describe().size());
        // WYNIK: trasy → 6
        router.describe().forEach(r -> show("  trasa", r));
        // WYNIK: trasa → GET /awaria → crash
        // WYNIK: trasa → GET /produkty → list
        // WYNIK: trasa → POST /produkty → create
        // WYNIK: trasa → DELETE /produkty/{sku} → delete
        // WYNIK: trasa → GET /produkty/{sku} → getOne
        // WYNIK: trasa → PUT /produkty/{sku}/stan → setStock

        // PUŁAPKA: dwie metody z tą samą metodą HTTP i tym samym szablonem = niejednoznaczność. Nasz router
        // wziąłby pierwszą; Spring zgłasza błąd już przy starcie (Ambiguous mapping = niejednoznaczne mapowanie).
        // W Springu: @GetMapping("/{sku}") ProductDto getOne(@PathVariable String sku) w klasie z
        //            @RequestMapping("/produkty"); tabelę tras buduje RequestMappingHandlerMapping przy starcie.
    }

    // =================================================================================================
    // 4. ZMIENNE ŚCIEŻKI I PARAMETRY ZAPYTANIA
    // =================================================================================================

    /**
     * 4. Zmienna ścieżki ({sku}) identyfikuje JEDEN zasób. Parametr zapytania (?kategoria=...) filtruje,
     * sortuje, stronicuje listę. Tekst z URI framework sam zamienia na typ parametru (tu BigDecimal).
     */
    static void pathAndQuery() {
        section("4. Zmienne ścieżki {sku} i parametry zapytania ?klucz=wartość");
        Router router = new Router(new ProductController());

        show("GET /produkty/KSI-001", router.handle(Request.of("GET", "/produkty/KSI-001", "")));
        // WYNIK: GET /produkty/KSI-001 → 200 {"sku":"KSI-001","name":"Czysty kod","category":"KSIAZKI","price":79.00,"stock":15}
        show("GET kategoria=KSIAZKI&maxCena=100", router.handle(Request.of("GET", "/produkty?kategoria=KSIAZKI&maxCena=100", "")));
        // WYNIK: GET kategoria=KSIAZKI&maxCena=100 → 200 [{"sku":"KSI-001","name":"Czysty kod","category":"KSIAZKI","price":79.00,"stock":15},{"sku":"KSI-003","name":"Wzorce projektowe","category":"KSIAZKI","price":99.00,"stock":3}]
        show("GET maxCena=10", router.handle(Request.of("GET", "/produkty?maxCena=10", "")));
        // WYNIK: GET maxCena=10 → 200 [{"sku":"SPO-002","name":"Czekolada gorzka","category":"SPOZYWCZE","price":7.49,"stock":300}]
        show("GET maxCena=tanio", router.handle(Request.of("GET", "/produkty?maxCena=tanio", "")));
        // WYNIK: GET maxCena=tanio → 400 {"status":400,"error":"parametr 'maxCena': 'tanio' to nie liczba","path":"/produkty"}

        // PUŁAPKA: znaki spoza ASCII i spacje w URI muszą być zakodowane procentowo (%C5%82 = ł, + albo %20 = spacja).
        show("dekodowanie", decode("Ksi%C4%85%C5%BCki+i+kawa"));
        // WYNIK: dekodowanie → Książki i kawa
        // DOBRA PRAKTYKA: identyfikator → ścieżka (/produkty/KSI-001); filtr/sortowanie/strona → parametry
        //                 (?kategoria=KSIAZKI&sort=cena&strona=2).
        // W Springu: @PathVariable String sku; @RequestParam(required = false) BigDecimal maxCena;
        //            konwersję tekstu na typ robi ConversionService; zły format → 400 Bad Request.
    }

    // =================================================================================================
    // 5. JSON: REKORD ↔ TEKST
    // =================================================================================================

    /**
     * 5. JSON to tekst. Mapper zamienia rekord na JSON, czytając składniki rekordu refleksją (nazwy = klucze,
     * kolejność = nagłówek rekordu), a w drugą stronę woła konstruktor kanoniczny.
     */
    static void json() {
        section("5. JSON: rekord → tekst i tekst → rekord (mini mapper)");

        ProductDto dto = new ProductDto("X-1", "Kubek \"duży\"", "DOM", new BigDecimal("19.90"), 3);
        show("toJson(rekord)", Json.toJson(dto));
        // WYNIK: toJson(rekord) → {"sku":"X-1","name":"Kubek \"duży\"","category":"DOM","price":19.90,"stock":3}
        show("toJson(lista, mapa)", Json.toJson(List.of(1, true, new TreeMap<>(Map.of("k", "v")))));
        // WYNIK: toJson(lista, mapa) → [1,true,{"k":"v"}]

        NewProduct parsed = Json.fromJson("{\"name\": \"Kubek\", \"sku\": \"X-2\", \"price\": 12.50, \"category\": \"DOM\", \"kolor\": \"czerwony\"}",
                NewProduct.class);
        show("fromJson", parsed);
        // WYNIK: fromJson → NewProduct[sku=X-2, name=Kubek, category=DOM, price=12.50]
        // Kolejność kluczy w JSON nie ma znaczenia; nieznane pole „kolor” pominięto.

        expectThrows("brak pola", () -> Json.fromJson("{\"sku\": \"X-3\"}", NewProduct.class));
        // WYNIK: ✔ brak pola → rzucono IllegalArgumentException: brak pola 'name'
        expectThrows("uszkodzony JSON", () -> Json.fromJson("{\"stock\": }", StockUpdate.class));
        // WYNIK: ✔ uszkodzony JSON → rzucono IllegalArgumentException: niepoprawny JSON: oczekiwano wartości na pozycji 10

        // PUŁAPKA: kwoty jako double w JSON tracą dokładność (0.1 + 0.2). Trzymaj BigDecimal i wypisuj
        // toPlainString() — inaczej new BigDecimal("1E+3") trafi do JSON jako 1E+3.
        // PUŁAPKA: cudzysłów w tekście musi być poprzedzony \ — sklejanie JSON-a ręcznie przez "+" bez
        // escapowania psuje dokument (i bywa luką bezpieczeństwa). Używaj mappera.
        // W Springu: Jackson (ObjectMapper) robi to automatycznie dla @RequestBody i wyniku metody; rekordy
        //            obsługuje od wersji 2.12. Spring Boot domyślnie IGNORUJE nieznane pola (FAIL_ON_UNKNOWN_PROPERTIES
        //            wyłączone), czysty Jackson domyślnie zgłasza na nich błąd.
    }

    // =================================================================================================
    // 6. IDEMPOTENTNOŚĆ W PRAKTYCE
    // =================================================================================================

    /**
     * 6. Sieć zawodzi: klient nie dostał odpowiedzi i ponawia żądanie. Czy to bezpieczne? Dla PUT i DELETE —
     * tak (stan po 2× = stan po 1×). Dla POST — nie: drugi POST może utworzyć drugi zasób (albo konflikt).
     */
    static void idempotency() {
        section("6. Idempotentność: co się stanie, gdy klient ponowi żądanie?");
        Router router = new Router(new ProductController());

        String body = "{\"sku\": \"NEW-1\", \"name\": \"Kubek\", \"category\": \"DOM\", \"price\": 25}";
        show("POST 1×", router.handle(Request.of("POST", "/produkty", body)));
        // WYNIK: POST 1× → 201 {"sku":"NEW-1","name":"Kubek","category":"DOM","price":25,"stock":0}
        show("POST 2×", router.handle(Request.of("POST", "/produkty", body)));
        // WYNIK: POST 2× → 409 {"status":409,"error":"produkt NEW-1 już istnieje","path":"/produkty"}

        String stock = "{\"stock\": 40}";
        show("PUT 1×", router.handle(Request.of("PUT", "/produkty/NEW-1/stan", stock)).status());
        // WYNIK: PUT 1× → 200
        show("PUT 2×", router.handle(Request.of("PUT", "/produkty/NEW-1/stan", stock)));
        // WYNIK: PUT 2× → 200 {"sku":"NEW-1","name":"Kubek","category":"DOM","price":25,"stock":40}

        show("DELETE 1×", router.handle(Request.of("DELETE", "/produkty/NEW-1", "")).status());
        // WYNIK: DELETE 1× → 204
        show("DELETE 2×", router.handle(Request.of("DELETE", "/produkty/NEW-1", "")).status());
        // WYNIK: DELETE 2× → 404
        // Drugi DELETE dał 404 — ODPOWIEDŹ inna, ale STAN serwera ten sam (produktu nie ma). To nadal idempotentne.

        // DOBRA PRAKTYKA: dla POST, który klient może ponawiać (płatności!), stosuje się klucz idempotencji —
        // nagłówek Idempotency-Key; serwer pamięta klucz i drugi raz zwraca ten sam wynik zamiast tworzyć nowy.
        // W Springu: idempotentność to sprawa projektu API, framework jej nie zapewnia.
    }

    // =================================================================================================
    // 7. OBSŁUGA BŁĘDÓW → CIAŁO JSON
    // =================================================================================================

    /**
     * 7. Każdy błąd — z routera (brak trasy, zła metoda), z konwersji (zły JSON) i z kontrolera (brak produktu)
     * — trafia do JEDNEGO miejsca, ErrorAdvice, które zwraca spójne ciało JSON. 500 bez szczegółów!
     */
    static void errorHandling() {
        section("7. Obsługa błędów: jeden „doradca” → spójny JSON");
        Router router = new Router(new ProductController());

        show("brak produktu", router.handle(Request.of("GET", "/produkty/XXX", "")));
        // WYNIK: brak produktu → 404 {"status":404,"error":"nie ma produktu XXX","path":"/produkty/XXX"}
        show("brak trasy", router.handle(Request.of("GET", "/klienci", "")));
        // WYNIK: brak trasy → 404 {"status":404,"error":"brak zasobu /klienci","path":"/klienci"}
        show("zła metoda", router.handle(Request.of("PATCH", "/produkty/ELE-001", "")));
        // WYNIK: zła metoda → 405 {"status":405,"error":"metoda PATCH niedozwolona dla /produkty/ELE-001","path":"/produkty/ELE-001"}
        show("zły JSON", router.handle(Request.of("PUT", "/produkty/ELE-001/stan", "{stock: 5}")));
        // WYNIK: zły JSON → 400 {"status":400,"error":"niepoprawny JSON: oczekiwano '\"' na pozycji 1","path":"/produkty/ELE-001/stan"}
        show("awaria", router.handle(Request.of("GET", "/awaria", "")));
        // WYNIK: awaria → 500 {"status":500,"error":"błąd serwera","path":"/awaria"}
        // Prawdziwa przyczyna (adres bazy!) nie wyciekła — klient widzi tylko „błąd serwera”.

        // PUŁAPKA: każdy kontroler łapiący wyjątki po swojemu = różne formaty błędów w jednym API.
        // DOBRA PRAKTYKA: jeden format błędu dla całego API (status, komunikat, ścieżka, ewentualnie lista pól).
        // W Springu: @RestControllerAdvice class ApiErrors {
        //                @ExceptionHandler(NoSuchElementException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
        //                ErrorBody notFound(NoSuchElementException e, HttpServletRequest req) { ... } }
        //            Spring 6 ma też standard ProblemDetail (RFC 7807, dziś zastąpiony przez RFC 9457): {"type", "title", "status", "detail", "instance"}.
    }

    // =================================================================================================
    // 8. PRAWDZIWE HTTP: HttpServer + HttpClient
    // =================================================================================================

    /**
     * 8. Podpinamy router pod HttpServer z JDK (tylko loopback = ten komputer, port 0 = wolny port wybrany
     * przez system) i wołamy go klientem HttpClient. Serwer i pule wątków zamykamy w finally.
     */
    static void realHttp() {
        section("8. Prawdziwe HTTP: HttpServer z JDK + HttpClient");

        ExecutorService serverPool = Executors.newFixedThreadPool(2);
        ExecutorService clientPool = Executors.newFixedThreadPool(2);
        HttpServer server = null;
        try {
            server = startServer(new Router(new ProductController()), serverPool);
            String base = "http://localhost:" + server.getAddress().getPort();   // portu nie wypisujemy — za każdym razem inny
            HttpClient client = HttpClient.newBuilder()
                    .proxy(HttpClient.Builder.NO_PROXY)                      // localhost bez serwera pośredniczącego
                    .executor(clientPool)
                    .build();

            HttpResponse<String> one = client.send(HttpRequest.newBuilder(URI.create(base + "/produkty/ELE-003")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            show("GET status", one.statusCode());
            // WYNIK: GET status → 200
            show("GET Content-Type", one.headers().firstValue("Content-Type").orElse("?"));
            // WYNIK: GET Content-Type → application/json; charset=utf-8
            show("GET ciało", one.body());
            // WYNIK: GET ciało → {"sku":"ELE-003","name":"Słuchawki BT","category":"ELEKTRONIKA","price":349.90,"stock":25}

            HttpResponse<String> created = client.send(HttpRequest.newBuilder(URI.create(base + "/produkty"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("{\"sku\":\"NEW-9\",\"name\":\"Żółty kubek\",\"category\":\"DOM\",\"price\":30.00}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            show("POST", created.statusCode() + " " + created.body());
            // WYNIK: POST → 201 {"sku":"NEW-9","name":"Żółty kubek","category":"DOM","price":30.00,"stock":0}

            HttpResponse<String> deleted = client.send(HttpRequest.newBuilder(URI.create(base + "/produkty/NEW-9")).DELETE().build(),
                    HttpResponse.BodyHandlers.ofString());
            show("DELETE", deleted.statusCode() + " ciało: '" + deleted.body() + "'");
            // WYNIK: DELETE → 204 ciało: ''

            HttpResponse<String> missing = client.send(HttpRequest.newBuilder(URI.create(base + "/produkty/NEW-9")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            show("GET po DELETE", missing.statusCode() + " " + missing.body());
            // WYNIK: GET po DELETE → 404 {"status":404,"error":"nie ma produktu NEW-9","path":"/produkty/NEW-9"}
        } catch (IOException e) {
            show("błąd IO", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();                              // przywróć flagę przerwania
        } finally {
            if (server != null) server.stop(0);                             // stop = zatrzymaj (0 s czekania)
            serverPool.shutdown();
            clientPool.shutdown();
        }

        // PUŁAPKA: 204 No Content NIE może mieć ciała — w HttpServer długość -1 w sendResponseHeaders oznacza
        // „bez ciała”; podanie 0 przy innych statusach znaczy „ciało o nieznanej długości” (chunked)!
        // DOBRA PRAKTYKA: zawsze ustaw Content-Type z charset=utf-8, inaczej polskie litery mogą się rozjechać.
        // W Springu: serwer (Tomcat) jest wbudowany i startuje sam (Spring04); klient — RestClient (Spring 6.1+),
        //            WebClient albo zwykły HttpClient; w testach MockMvc albo TestRestTemplate.
    }

    /** Adapter: HttpExchange (świat HttpServer) ↔ Request/Response (świat naszego frameworka). */
    static HttpServer startServer(Router router, ExecutorService pool) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {                              // exchange = wymiana (żądanie+odpowiedź)
            try {
                Map<String, String> query = new TreeMap<>();
                String raw = exchange.getRequestURI().getRawQuery();
                if (raw != null) {
                    for (String pair : raw.split("&")) {
                        String[] kv = pair.split("=", 2);
                        query.put(decode(kv[0]), kv.length > 1 ? decode(kv[1]) : "");
                    }
                }
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String accept = Optional.ofNullable(exchange.getRequestHeaders().getFirst("Accept")).orElse("application/json");
                Response r = router.handle(new Request(exchange.getRequestMethod(), exchange.getRequestURI().getPath(), query, body, accept));
                byte[] bytes = r.body().getBytes(StandardCharsets.UTF_8);
                if (!r.contentType().isEmpty()) {
                    exchange.getResponseHeaders().set("Content-Type", r.contentType() + "; charset=utf-8");
                }
                exchange.sendResponseHeaders(r.status(), bytes.length == 0 ? -1 : bytes.length);
                if (bytes.length > 0) {
                    try (OutputStream out = exchange.getResponseBody()) {
                        out.write(bytes);
                    }
                }
            } finally {
                exchange.close();                                            // zawsze zamknij wymianę
            }
        });
        server.setExecutor(pool);
        server.start();
        return server;
    }

    // =================================================================================================
    // 9. NEGOCJACJA TREŚCI I WERSJONOWANIE
    // =================================================================================================

    /**
     * 9. Klient mówi nagłówkiem Accept, w jakim formacie chce odpowiedź; serwer wybiera format albo odmawia (406).
     * Nagłówek Content-Type mówi, w jakim formacie JEST wysłane ciało. Testujemy bez sieci — router to zwykły obiekt.
     */
    static void negotiationAndVersions() {
        section("9. Negocjacja treści (Accept) i wersjonowanie API");
        Router router = new Router(new ProductController());
        Request get = Request.of("GET", "/produkty/KSI-002", "");

        show("Accept: application/json", router.handle(get));
        // WYNIK: Accept: application/json → 200 {"sku":"KSI-002","name":"Java. Podstawy","category":"KSIAZKI","price":129.00,"stock":9}
        show("Accept: text/plain", router.handle(get.withAccept("text/plain")));
        // WYNIK: Accept: text/plain → 200 ProductDto[sku=KSI-002, name=Java. Podstawy, category=KSIAZKI, price=129.00, stock=9]
        show("Accept: application/xml", router.handle(get.withAccept("application/xml")));
        // WYNIK: Accept: application/xml → 406 {"status":406,"error":"nieobsługiwany format application/xml","path":"/produkty/KSI-002"}

        // Wersjonowanie — gdy trzeba zmienić kontrakt niezgodnie wstecz (usunąć pole, zmienić typ):
        //   • w ścieżce:      /api/v1/produkty → /api/v2/produkty   (najprostsze, najczęstsze, widoczne w logach)
        //   • w nagłówku:     X-API-Version: 2  albo  Accept: application/vnd.sklep.v2+json
        //   • w parametrze:   /produkty?wersja=2
        // DOBRA PRAKTYKA: dodawanie NOWYCH pól jest zgodne wstecz (klienci je ignorują) — nie wymaga nowej wersji.
        // W Springu: @GetMapping(value = "/{sku}", produces = "application/json") — produces/consumes sterują
        //            negocjacją; Jackson dodaje JSON, a XML wymaga dodatkowej biblioteki (jackson-dataformat-xml).
    }

    // =================================================================================================
    // 10. PORÓWNANIE ZE SPRING MVC
    // =================================================================================================

    /**
     * 10. Wszystko, co tu zbudowaliśmy, ma gotowy odpowiednik w Spring MVC — tylko dojrzalszy (walidacja,
     * bezpieczeństwo, wielowątkowy serwer, dokumentacja OpenAPI, testy).
     */
    static void compareWithSpringMvc() {
        section("10. Mini-framework a Spring MVC");

        Map<String, String> map = new LinkedHashMap<>();
        map.put("@GetMapping(\"/produkty/{sku}\")", "to samo + @RequestMapping na klasie");
        map.put("Router (tabela tras)", "DispatcherServlet + RequestMappingHandlerMapping");
        map.put("@PathVariable / @RequestParam / @RequestBody", "te same nazwy adnotacji");
        map.put("Json (mini mapper)", "Jackson ObjectMapper (HttpMessageConverter)");
        map.put("ErrorAdvice", "@RestControllerAdvice + @ExceptionHandler / ProblemDetail");
        map.put("ResponseEntity(status, body)", "ResponseEntity<T>");
        map.put("HttpServer z JDK", "wbudowany Tomcat (spring-boot-starter-web)");
        showEach("mini → Spring MVC", map);
        // WYNIK: mini → Spring MVC (liczba kluczy: 7):
        // WYNIK: • @GetMapping("/produkty/{sku}") → to samo + @RequestMapping na klasie
        // WYNIK: • Router (tabela tras) → DispatcherServlet + RequestMappingHandlerMapping
        // WYNIK: • @PathVariable / @RequestParam / @RequestBody → te same nazwy adnotacji
        // WYNIK: • Json (mini mapper) → Jackson ObjectMapper (HttpMessageConverter)
        // WYNIK: • ErrorAdvice → @RestControllerAdvice + @ExceptionHandler / ProblemDetail
        // WYNIK: • ResponseEntity(status, body) → ResponseEntity<T>
        // WYNIK: • HttpServer z JDK → wbudowany Tomcat (spring-boot-starter-web)

        // W Springu: cały kontroler z tej lekcji wygląda tak:
        //   @RestController @RequestMapping("/produkty")
        //   class ProductController {
        //       @GetMapping("/{sku}") ProductDto getOne(@PathVariable String sku) { ... }
        //       @PostMapping ResponseEntity<ProductDto> create(@Valid @RequestBody NewProduct body) {
        //           ... return ResponseEntity.created(URI.create("/produkty/" + dto.sku())).body(dto); }
        //       @DeleteMapping("/{sku}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable String sku) { ... }
        //   }
        // (bez nazwy w @PathVariable Spring bierze nazwę parametru — wymaga kompilacji z -parameters, co Spring Boot
        //  ustawia w swoich wtyczkach Maven/Gradle).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • URI = rzeczownik (zasób, liczba mnoga), metoda = czasownik: GET czyta, POST tworzy, PUT podmienia,
     *     PATCH zmienia część, DELETE usuwa.
     *   • Bezpieczne: GET (i HEAD, OPTIONS). Idempotentne: GET, PUT, DELETE (i HEAD, OPTIONS). POST — nie.
     *   • Statusy: 200, 201 (+Location), 204 (bez ciała), 400, 401, 403, 404, 405, 406, 409, 500.
     *   • {zmienna} w ścieżce = identyfikator; ?parametr = filtr/sortowanie/strona.
     *   • JSON: mapper (Jackson) zamiast sklejania tekstu; kwoty jako BigDecimal; nieznane pola — w Boot ignorowane.
     *   • Błędy: jedno miejsce (@RestControllerAdvice), jeden format; 500 bez szczegółów technicznych.
     *   • Bezstanowość: każde żądanie samowystarczalne; HATEOAS = linki w odpowiedzi (rzadko).
     *   • Accept → format odpowiedzi (406, gdy nie umiemy); Content-Type → format ciała; wersja: /api/v1/...
     *   • Serwer testowy: loopback + port 0, zamykaj w finally; HttpClient do localhost: NO_PROXY.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego DELETE jest idempotentne, choć drugie wywołanie zwraca 404 zamiast 204?
     *   2. Co wypisze (router z sekcji 3–7, świeży ProductController)?
     *        System.out.println(router.handle(Request.of("GET", "/produkty?kategoria=DOM&maxCena=500", "")));
     *   3. ZNAJDŹ BŁĄD:  @GetMapping("/zamowienia/{id}/anuluj") void cancel(@PathVariable("id") long id) { ... }
     *   4. Co wypisze:  System.out.println(Json.toJson(new StockUpdate(5)) + Json.toJson(List.of("a\"b")));  ?
     *   5. Czym różni się 401 od 403? A 404 od 405?
     *   6. ZNAJDŹ BŁĄD: odpowiedź serwera na błąd bazy:
     *        500 {"error": "org.postgresql.util.PSQLException: password authentication failed for user admin"}
     *   7. Klient wysłał POST /platnosci i nie dostał odpowiedzi (zerwane połączenie). Czy może bezpiecznie ponowić?
     *      Co można zrobić w projekcie API?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        runChecks(false);
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        runChecks(true);
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    record Mug(String sku, String name, BigDecimal price, boolean active) { }   // mug = kubek

    private static void runChecks(boolean ref) {
        String s = ref ? " (wzorzec)" : "";
        Check.equal("ćw. 1: idempotentne" + s, List.of("GET", "PUT", "DELETE"),
                () -> List.of("GET", "POST", "PUT", "PATCH", "DELETE").stream()
                        .filter(m -> ref ? solution1(m) : exercise1(m)).toList());

        Check.equal("ćw. 2: dopasowanie" + s, Optional.of(Map.of("id", "7", "nr", "2")),
                () -> ref ? solution2("/zamowienia/{id}/pozycje/{nr}", "/zamowienia/7/pozycje/2")
                          : exercise2("/zamowienia/{id}/pozycje/{nr}", "/zamowienia/7/pozycje/2"));
        Check.equal("ćw. 2: brak dopasowania" + s, Optional.empty(),
                () -> ref ? solution2("/zamowienia/{id}", "/zamowienia/7/pozycje") : exercise2("/zamowienia/{id}", "/zamowienia/7/pozycje"));

        Mug mug = new Mug("X-1", "Kubek \"duży\"", new BigDecimal("19.90"), true);
        Check.equal("ćw. 3: rekord → JSON" + s, "{\"sku\":\"X-1\",\"name\":\"Kubek \\\"duży\\\"\",\"price\":19.90,\"active\":true}",
                () -> ref ? solution3(mug) : exercise3(mug));

        String raw = "kategoria=KSIAZKI&tag=java&tag=nowo%C5%9B%C4%87&pusty=&fraza=czysty+kod";
        Map<String, List<String>> expected = new TreeMap<>();
        expected.put("fraza", List.of("czysty kod"));
        expected.put("kategoria", List.of("KSIAZKI"));
        expected.put("pusty", List.of(""));
        expected.put("tag", List.of("java", "nowość"));
        Check.equal("ćw. 4: parametry zapytania" + s, expected, () -> ref ? solution4(raw) : exercise4(raw));
    }

    /**
     * ĆWICZENIE 1 (łatwe): czy metoda HTTP jest idempotentna? (GET, PUT, DELETE, HEAD, OPTIONS — tak; POST, PATCH — nie).
     * Podpowiedź: switch z wieloma etykietami (Java 14+): case "GET", "PUT", ... -> true;
     */
    static boolean exercise1(String method) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): dopasuj ścieżkę do szablonu z {zmiennymi}. Zwróć mapę „nazwa → wartość” albo
     * Optional.empty(), gdy nie pasuje. Bez wyrażeń regularnych: porównuj segmenty po split("/").
     * Podpowiedź: różna liczba segmentów → brak; segment {x} pasuje do wszystkiego (zapamiętaj), inny musi być równy.
     */
    static Optional<Map<String, String>> exercise2(String template, String path) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zamień DOWOLNY płaski rekord (String, BigDecimal, boolean, int) na JSON jak nasz mapper:
     * klucze w kolejności składników, teksty w cudzysłowach z escapowaniem \" i \\, BigDecimal przez toPlainString.
     * Podpowiedź: r.getClass().getRecordComponents(), rc.getAccessor().invoke(r); StringJoiner(",", "{", "}").
     */
    static String exercise3(Record r) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): sparsuj surowe zapytanie (część URI po „?”) do TreeMap „klucz → lista wartości”:
     * powtarzające się klucze zbieraj w liście, klucz bez wartości → "", dekoduj %XX i „+” (URLDecoder, UTF-8).
     * Podpowiedź: split("&"), split("=", 2), computeIfAbsent(klucz, k -> new ArrayList<>()).add(wartość).
     */
    static Map<String, List<String>> exercise4(String rawQuery) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(String method) {
        return switch (method) {
            case "GET", "PUT", "DELETE", "HEAD", "OPTIONS" -> true;
            default -> false;
        };
    }

    static Optional<Map<String, String>> solution2(String template, String path) {
        String[] t = template.split("/");
        String[] p = path.split("/");
        if (t.length != p.length) return Optional.empty();
        Map<String, String> vars = new TreeMap<>();
        for (int i = 0; i < t.length; i++) {
            if (t[i].startsWith("{") && t[i].endsWith("}")) vars.put(t[i].substring(1, t[i].length() - 1), p[i]);
            else if (!t[i].equals(p[i])) return Optional.empty();
        }
        return Optional.of(vars);
    }

    static String solution3(Record r) {
        java.util.StringJoiner json = new java.util.StringJoiner(",", "{", "}");   // StringJoiner = łącznik tekstów
        for (RecordComponent rc : r.getClass().getRecordComponents()) {
            Object v;
            try {
                v = rc.getAccessor().invoke(r);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            String value;
            if (v instanceof String str) value = "\"" + str.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            else if (v instanceof BigDecimal b) value = b.toPlainString();
            else value = String.valueOf(v);
            json.add("\"" + rc.getName() + "\":" + value);
        }
        return json.toString();
    }

    static Map<String, List<String>> solution4(String rawQuery) {
        Map<String, List<String>> result = new TreeMap<>();
        for (String pair : rawQuery.split("&")) {
            if (pair.isEmpty()) continue;
            String[] kv = pair.split("=", 2);
            result.computeIfAbsent(decode(kv[0]), k -> new ArrayList<>()).add(kv.length > 1 ? decode(kv[1]) : "");
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Idempotentność dotyczy STANU serwera, nie treści odpowiedzi: po pierwszym i po drugim DELETE produktu
     *      nie ma — stan jest ten sam. Inny status (404) tylko informuje, że nie było już czego usuwać.
     *   2. „200 [{"sku":"DOM-002","name":"Lampka biurkowa","category":"DOM","price":129.00,"stock":18}]” — Ekspres
     *      do kawy (DOM-001, 1899.00) odpadł na filtrze ceny.
     *   3. Czasownik w URI i GET zmieniający stan. Anulowanie to zmiana: POST /zamowienia/{id}/anulowanie (akcja
     *      jako zasób) albo PATCH /zamowienia/{id} z {"status": "ANULOWANE"}. GET może wywołać robot lub cache.
     *   4. „{"stock":5}["a\"b"]” — liczba bez cudzysłowu, a cudzysłów w tekście poprzedzony ukośnikiem.
     *   5. 401 = nie wiadomo, kim jesteś (brak/zły token — zaloguj się); 403 = wiadomo, ale nie masz uprawnień.
     *      404 = zasobu nie ma; 405 = zasób jest, ale ta metoda HTTP nie jest dla niego obsługiwana.
     *   6. Wyciek szczegółów technicznych (typ bazy, nazwa użytkownika) — ułatwia atak. Klient powinien dostać
     *      ogólny komunikat („błąd serwera”, ewentualnie identyfikator zgłoszenia), a pełny wyjątek idzie do logu.
     *   7. Nie wiadomo, czy płatność się wykonała — ponowny POST może obciążyć klienta drugi raz (POST nie jest
     *      idempotentny). Rozwiązanie: klucz idempotencji (nagłówek Idempotency-Key z unikalnym id) — serwer
     *      pamięta wykonane klucze i na powtórkę zwraca zapisany wynik; albo klient najpierw sprawdza GET-em stan.
     */
    // </editor-fold>
}
