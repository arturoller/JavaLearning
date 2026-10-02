package t35_capstone;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import helpers.Check;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Projekt 4 — usługa REST „lista zadań” (TODO) na HttpServer + klient HttpClient
 *        (task = zadanie; repository = repozytorium; service = serwis; endpoint = punkt końcowy usługi)
 *
 * W SKRÓCIE:
 *   Mała, prawdziwa usługa HTTP: model Task z walidacją, repozytorium w pamięci za interfejsem, serwis
 *   z regułami (unikalne tytuły, tabela dozwolonych zmian statusu), warstwa HTTP z routingiem i poprawnymi
 *   kodami (200/201/204/400/404/405/409), JSON w obie strony i klient, który przechodzi scenariusz od początku
 *   do końca. Na koniec — jak dokładnie to samo zapisać w Spring Boot.
 *
 * ANALOGIA:
 *   Okienko na poczcie. Klient (HttpClient) podaje formularz (żądanie JSON), urzędnik w okienku (TodoApi)
 *   sprawdza, czy formularz jest kompletny, i przekazuje go do biura (TaskService), które pilnuje
 *   regulaminu i zagląda do szafy z aktami (TaskRepository). Klient dostaje pieczątkę (kod statusu)
 *   i odpowiedź — albo pismo z wyjaśnieniem, czemu odmówiono (treść błędu).
 *
 * JAK TO DZIAŁA:
 *   HttpClient ──HTTP──► HttpServer ─► adapter (HttpExchange ↔ ApiResponse) ─► TodoApi.handle(metoda, ścieżka, treść)
 *                                                                                │ routing + JSON + kody
 *                                                                           TaskService (reguły)
 *                                                                                │
 *                                                                     TaskRepository (interfejs) ─► InMemory…
 *   Wykorzystane działy kursu:
 *   • HttpServer, HttpClient, kody statusu, JSON API  → t28_networking_http/Http03LocalServer, t28_networking_http/Http04JsonApi
 *   • REST: zasoby, metody, idempotencja            → t34_toward_spring/Spring03RestConcepts
 *   • warstwy: kontroler – serwis – repozytorium     → t34_toward_spring/Spring02Layers
 *   • repozytorium generyczne / interfejs           → t11_generics/Generics07Repository
 *   • enum + EnumMap jako tabela przejść             → t08_enums/Enums04EnumMapSet, t22_design_patterns/Patterns13State
 *   • wyrażenia regularne (routing)                  → t04_strings/Strings05Regex
 *   • ręczny JSON                                    → t18_io_files/Io05JsonManual
 *   • wątki serwera, synchronized                    → t21_concurrency/Concurrency03Locks
 *
 * SŁÓWKA:
 *   request = żądanie; response = odpowiedź; status code = kod statusu; body = treść; header = nagłówek;
 *   route = trasa; handler = obsługa (procedura obsługi); mapper = przekształcacz; transition = przejście;
 *   loopback = interfejs pętli zwrotnej (127.0.0.1); duplicate = duplikat; scenario = scenariusz.
 *
 * ZOBACZ TEŻ: t28_networking_http/Http05AsyncTimeouts (limity czasu), t34_toward_spring/Spring04WhatSpringGives
 *             (co da nam Spring), t27_clean_code_pitfalls/CleanCode03Architecture (zależności do środka)
 * </pre>
 */
public class Capstone04RestTodo {

    public static void main(String[] args) throws IOException, InterruptedException {
        title("Capstone04 — usługa REST „lista zadań”");

        requirements();       // requirements = wymagania
        domainModel();        // domain model = model dziedziny
        repository();         // repository = repozytorium
        serviceRules();       // service rules = reguły serwisu
        jsonMapping();        // json mapping = mapowanie JSON
        httpLayer();          // http layer = warstwa HTTP
        endToEnd();           // end to end = od początku do końca
        behaviourTests();     // behaviour tests = testy zachowania
        springBootMapping();  // spring boot mapping = przełożenie na Spring Boot
        exercises();          // exercises = ćwiczenia
    }

    // =================================================================================================
    // MODEL DZIEDZINY
    // =================================================================================================

    /** Status zadania. Tabela dozwolonych przejść to dane (EnumMap), a nie rozsiane if-y. */
    enum TaskStatus {
        TODO, IN_PROGRESS, DONE, CANCELLED;                                         // do zrobienia, w toku, zrobione, anulowane

        private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED = new EnumMap<>(TaskStatus.class);

        static {
            ALLOWED.put(TODO, EnumSet.of(IN_PROGRESS, CANCELLED));
            ALLOWED.put(IN_PROGRESS, EnumSet.of(TODO, DONE, CANCELLED));
            ALLOWED.put(DONE, EnumSet.noneOf(TaskStatus.class));                    // noneOf = pusty zbiór
            ALLOWED.put(CANCELLED, EnumSet.noneOf(TaskStatus.class));
        }

        boolean canMoveTo(TaskStatus target) {                                      // can move to = może przejść do
            return ALLOWED.get(this).contains(target);
        }

        Set<TaskStatus> allowedTargets() {
            return Set.copyOf(ALLOWED.get(this));
        }
    }

    /** Zadanie — niezmienne; zmiana = nowy obiekt przez metody with…. */
    record Task(long id, String title, int priority, TaskStatus status) {
        static final int MAX_TITLE = 60;

        Task {
            if (id <= 0) {
                throw new IllegalArgumentException("id musi być dodatnie");
            }
            if (title == null || title.isBlank() || title.length() > MAX_TITLE) {   // isBlank (Java 11+)
                throw new IllegalArgumentException("niepoprawny tytuł");
            }
            if (priority < 1 || priority > 5) {
                throw new IllegalArgumentException("priorytet poza 1–5");
            }
            if (status == null) {
                throw new IllegalArgumentException("brak statusu");
            }
        }

        Task withStatus(TaskStatus newStatus) {                                      // with = „z innym …”
            return new Task(id, title, priority, newStatus);
        }

        Task withDetails(String newTitle, int newPriority) {
            return new Task(id, newTitle, newPriority, status);
        }
    }

    /** Dane od klienta (POST/PUT) — zanim staną się zadaniem, serwis je sprawdzi. */
    record TaskRequest(String title, int priority) { }

    /** Błędy biznesowe; warstwa HTTP zamienia je na kody statusu (to NIE jest wiedza dziedziny). */
    abstract static sealed class TodoException extends RuntimeException
            permits NotFoundException, ConflictException, ValidationException {
        private static final long serialVersionUID = 1L;

        TodoException(String message) { super(message); }
    }

    static final class NotFoundException extends TodoException {
        private static final long serialVersionUID = 1L;

        NotFoundException(String message) { super(message); }
    }

    static final class ConflictException extends TodoException {
        private static final long serialVersionUID = 1L;

        ConflictException(String message) { super(message); }
    }

    static final class ValidationException extends TodoException {
        private static final long serialVersionUID = 1L;

        ValidationException(String message) { super(message); }
    }

    // =================================================================================================
    // REPOZYTORIUM — interfejs + implementacja w pamięci
    // =================================================================================================

    interface TaskRepository {
        long nextId();

        Task save(Task task);

        Optional<Task> findById(long id);

        List<Task> findAll();

        boolean deleteById(long id);
    }

    /** W pamięci: TreeMap (stała kolejność po id), metody synchronized, bo serwer obsługuje żądania w wielu wątkach. */
    static final class InMemoryTaskRepository implements TaskRepository {
        private final Map<Long, Task> tasks = new TreeMap<>();
        private long lastId;

        @Override
        public synchronized long nextId() {
            return ++lastId;
        }

        @Override
        public synchronized Task save(Task task) {
            tasks.put(task.id(), task);
            return task;
        }

        @Override
        public synchronized Optional<Task> findById(long id) {
            return Optional.ofNullable(tasks.get(id));                               // ofNullable = pusty, gdy null
        }

        @Override
        public synchronized List<Task> findAll() {
            return List.copyOf(tasks.values());                                      // kopia — nie wypuszczamy wnętrza
        }

        @Override
        public synchronized boolean deleteById(long id) {
            return tasks.remove(id) != null;
        }
    }

    // =================================================================================================
    // SERWIS — reguły biznesowe, nic o HTTP ani JSON
    // =================================================================================================

    static final class TaskService {
        private final TaskRepository repository;

        TaskService(TaskRepository repository) {                                     // zależność przez konstruktor
            this.repository = repository;
        }

        synchronized Task create(TaskRequest request) {
            validate(request);
            requireUniqueTitle(request.title(), 0);
            Task task = new Task(repository.nextId(), request.title().strip(), request.priority(), TaskStatus.TODO);
            return repository.save(task);
        }

        Task get(long id) {
            return repository.findById(id).orElseThrow(() -> new NotFoundException("brak zadania " + id));
        }

        List<Task> list() {
            return repository.findAll();
        }

        synchronized Task replace(long id, TaskRequest request) {
            Task current = get(id);
            validate(request);
            requireUniqueTitle(request.title(), id);
            return repository.save(current.withDetails(request.title().strip(), request.priority()));
        }

        synchronized Task changeStatus(long id, TaskStatus target) {
            Task current = get(id);
            if (!current.status().canMoveTo(target)) {
                throw new ConflictException("niedozwolone przejście " + current.status() + " → " + target);
            }
            return repository.save(current.withStatus(target));
        }

        synchronized void delete(long id) {
            if (!repository.deleteById(id)) {
                throw new NotFoundException("brak zadania " + id);
            }
        }

        /** Zbiera WSZYSTKIE błędy naraz — klient poprawia formularz za jednym razem. */
        private static void validate(TaskRequest request) {
            List<String> errors = new ArrayList<>();
            if (request.title() == null || request.title().isBlank()) {
                errors.add("title: nie może być pusty");
            } else if (request.title().strip().length() > Task.MAX_TITLE) {
                errors.add("title: najwyżej " + Task.MAX_TITLE + " znaków");
            }
            if (request.priority() < 1 || request.priority() > 5) {
                errors.add("priority: musi być w zakresie 1–5");
            }
            if (!errors.isEmpty()) {
                throw new ValidationException(String.join("; ", errors));
            }
        }

        private void requireUniqueTitle(String title, long exceptId) {
            String normalized = title.strip().toLowerCase(Locale.ROOT);
            boolean taken = repository.findAll().stream()
                    .anyMatch(t -> t.id() != exceptId && t.title().toLowerCase(Locale.ROOT).equals(normalized));
            if (taken) {
                throw new ConflictException("zadanie o tytule „" + title.strip() + "” już istnieje");
            }
        }
    }

    // =================================================================================================
    // JSON — mały przekształcacz (zapis zadań, odczyt płaskich obiektów)
    // =================================================================================================

    static final class JsonMapper {
        private JsonMapper() {
        }

        static String write(Task t) {
            return "{\"id\":" + t.id() + ",\"title\":\"" + escape(t.title()) + "\",\"priority\":" + t.priority()
                    + ",\"status\":\"" + t.status() + "\"}";
        }

        static String write(List<Task> tasks) {
            return tasks.stream().map(JsonMapper::write).collect(Collectors.joining(",", "[", "]"));
        }

        static String error(int status, String message) {
            return "{\"status\":" + status + ",\"message\":\"" + escape(message) + "\"}";
        }

        static String escape(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t");
        }

        /** Odczyt płaskiego obiektu: {"klucz": "tekst" | liczba | true | false | null, ...}. */
        static Map<String, Object> parseObject(String json) {
            return new Parser(json).object();
        }

        static TaskRequest toRequest(String json) {
            Map<String, Object> map = parseObject(json);
            Object title = map.get("title");
            Object priority = map.get("priority");
            if (title != null && !(title instanceof String)) {
                throw new ValidationException("title: oczekiwano tekstu");
            }
            if (!(priority instanceof BigDecimal number)) {
                throw new ValidationException("priority: oczekiwano liczby");
            }
            try {
                return new TaskRequest((String) title, number.intValueExact());     // intValueExact = int albo wyjątek
            } catch (ArithmeticException e) {
                throw new ValidationException("priority: oczekiwano liczby całkowitej");
            }
        }

        /** Parser zstępujący: jedna metoda na jeden element gramatyki. */
        private static final class Parser {
            private final String s;
            private int pos;

            Parser(String s) {
                this.s = s == null ? "" : s;
            }

            Map<String, Object> object() {
                Map<String, Object> result = new LinkedHashMap<>();
                skipSpaces();
                expect('{');
                skipSpaces();
                if (peek() == '}') {
                    pos++;
                } else {
                    do {
                        skipSpaces();
                        String key = string();
                        skipSpaces();
                        expect(':');
                        skipSpaces();
                        result.put(key, value());
                        skipSpaces();
                    } while (tryConsume(','));
                    expect('}');
                }
                skipSpaces();
                if (pos != s.length()) {
                    throw fail("nadmiarowe znaki");
                }
                return result;
            }

            private Object value() {
                char c = peek();
                if (c == '"') {
                    return string();
                }
                if (c == '-' || Character.isDigit(c)) {
                    int start = pos;
                    while (pos < s.length() && "+-.eE0123456789".indexOf(s.charAt(pos)) >= 0) {
                        pos++;
                    }
                    try {
                        return new BigDecimal(s.substring(start, pos));
                    } catch (NumberFormatException e) {
                        throw fail("zła liczba");
                    }
                }
                for (String literal : List.of("true", "false", "null")) {
                    if (s.startsWith(literal, pos)) {
                        pos += literal.length();
                        return literal.equals("null") ? null : Boolean.valueOf(literal);
                    }
                }
                throw fail("oczekiwano wartości");
            }

            private String string() {
                expect('"');
                StringBuilder sb = new StringBuilder();
                while (true) {
                    char c = next();
                    if (c == '"') {
                        return sb.toString();
                    }
                    if (c == '\\') {
                        char e = next();
                        switch (e) {
                            case '"', '\\', '/' -> sb.append(e);
                            case 'n' -> sb.append('\n');
                            case 't' -> sb.append('\t');
                            default -> throw fail("nieobsługiwana sekwencja \\" + e);
                        }
                    } else {
                        sb.append(c);
                    }
                }
            }

            private char peek() {
                return pos < s.length() ? s.charAt(pos) : '\0';
            }

            private char next() {
                if (pos >= s.length()) {
                    throw fail("nieoczekiwany koniec");
                }
                return s.charAt(pos++);
            }

            private void expect(char c) {
                if (peek() != c) {
                    throw fail("oczekiwano '" + c + "'");
                }
                pos++;
            }

            private boolean tryConsume(char c) {
                if (peek() == c) {
                    pos++;
                    return true;
                }
                return false;
            }

            private void skipSpaces() {
                while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                    pos++;
                }
            }

            private ValidationException fail(String what) {
                return new ValidationException("niepoprawny JSON: " + what + " (pozycja " + pos + ")");
            }
        }
    }

    // =================================================================================================
    // WARSTWA HTTP — routing, kody statusu; czysta funkcja handle + cienki adapter na HttpServer
    // =================================================================================================

    /** Odpowiedź niezależna od serwera: kod, treść JSON i nagłówki (np. Location). */
    record ApiResponse(int status, String body, Map<String, String> headers) {
        ApiResponse {
            headers = Map.copyOf(headers);                                           // copyOf = niezmienna kopia (Java 10+)
        }

        static ApiResponse json(int status, String body) {
            return new ApiResponse(status, body, Map.of());
        }
    }

    static final class TodoApi {
        private static final Pattern TASKS = Pattern.compile("/tasks/?");
        private static final Pattern TASK = Pattern.compile("/tasks/(\\d+)");       // \d+ = jedna lub więcej cyfr
        private static final Pattern TASK_STATUS = Pattern.compile("/tasks/(\\d+)/status");

        private final TaskService service;

        TodoApi(TaskService service) {
            this.service = service;
        }

        /** handle = obsłuż: (metoda, ścieżka, treść) → odpowiedź. Żadnych gniazd — łatwo testować. */
        ApiResponse handle(String method, String path, String body) {
            try {
                return route(method, path, body);
            } catch (ValidationException e) {
                return ApiResponse.json(400, JsonMapper.error(400, e.getMessage()));
            } catch (NotFoundException e) {
                return ApiResponse.json(404, JsonMapper.error(404, e.getMessage()));
            } catch (ConflictException e) {
                return ApiResponse.json(409, JsonMapper.error(409, e.getMessage()));
            }
        }

        private ApiResponse route(String method, String path, String body) {
            if (TASKS.matcher(path).matches()) {                                     // matches = pasuje w całości
                return switch (method) {
                    case "GET" -> ApiResponse.json(200, JsonMapper.write(service.list()));
                    case "POST" -> created(service.create(JsonMapper.toRequest(body)));
                    default -> methodNotAllowed("GET, POST");
                };
            }
            Matcher task = TASK.matcher(path);
            if (task.matches()) {
                long id = Long.parseLong(task.group(1));                             // group(1) = pierwszy nawias
                return switch (method) {
                    case "GET" -> ApiResponse.json(200, JsonMapper.write(service.get(id)));
                    case "PUT" -> ApiResponse.json(200, JsonMapper.write(service.replace(id, JsonMapper.toRequest(body))));
                    case "DELETE" -> {
                        service.delete(id);
                        yield new ApiResponse(204, "", Map.of());                    // yield = zwróć z bloku case
                    }
                    default -> methodNotAllowed("GET, PUT, DELETE");
                };
            }
            Matcher status = TASK_STATUS.matcher(path);
            if (status.matches()) {
                if (!method.equals("PATCH")) {
                    return methodNotAllowed("PATCH");
                }
                long id = Long.parseLong(status.group(1));
                return ApiResponse.json(200, JsonMapper.write(service.changeStatus(id, parseStatus(body))));
            }
            throw new NotFoundException("nieznana ścieżka " + path);
        }

        private static ApiResponse created(Task task) {
            return new ApiResponse(201, JsonMapper.write(task), Map.of("Location", "/tasks/" + task.id()));
        }

        private static ApiResponse methodNotAllowed(String allowed) {
            return new ApiResponse(405, JsonMapper.error(405, "dozwolone metody: " + allowed), Map.of("Allow", allowed));
        }

        private static TaskStatus parseStatus(String body) {
            Object value = JsonMapper.parseObject(body).get("status");
            try {
                return TaskStatus.valueOf(String.valueOf(value));                    // valueOf = stała po nazwie
            } catch (IllegalArgumentException e) {
                throw new ValidationException("status: nieznana wartość " + value);
            }
        }
    }

    /** Cienki adapter: tłumaczy HttpExchange na wywołanie TodoApi.handle i z powrotem. */
    static final class TodoHttpServer {
        private final HttpServer server;
        private final ExecutorService pool;

        private TodoHttpServer(HttpServer server, ExecutorService pool) {
            this.server = server;
            this.pool = pool;
        }

        static TodoHttpServer start(TodoApi api) throws IOException {
            // loopback + port 0: tylko ten komputer, a wolny port wybiera system (nigdy nie wypisujemy go).
            HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            server.setExecutor(pool);                                                // setExecutor = ustaw pulę wątków
            server.createContext("/", exchange -> serve(api, exchange));             // createContext = zarejestruj obsługę
            server.start();
            return new TodoHttpServer(server, pool);
        }

        URI baseUri() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        }

        void stop() throws InterruptedException {
            server.stop(0);                                                          // stop(0) = zatrzymaj bez czekania
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }

        private static void serve(TodoApi api, HttpExchange exchange) throws IOException {
            try (exchange) {                                                         // HttpExchange jest AutoCloseable
                String body;
                try (InputStream in = exchange.getRequestBody()) {
                    body = new String(in.readAllBytes(), StandardCharsets.UTF_8);    // readAllBytes (Java 9+)
                }
                ApiResponse response;
                try {
                    response = api.handle(exchange.getRequestMethod(), exchange.getRequestURI().getPath(), body);
                } catch (RuntimeException e) {                                       // błąd programisty → 500, bez szczegółów
                    response = ApiResponse.json(500, JsonMapper.error(500, "błąd serwera"));
                }
                response.headers().forEach((name, value) -> exchange.getResponseHeaders().set(name, value));
                byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
                if (bytes.length > 0) {
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                }
                exchange.sendResponseHeaders(response.status(), bytes.length == 0 ? -1 : bytes.length);
                if (bytes.length > 0) {
                    try (OutputStream out = exchange.getResponseBody()) {
                        out.write(bytes);
                    }
                }
            }
        }
    }

    /** Klient: wysyła żądanie i zwraca jedną linię „kod treść (Location: …)”. Adresu z portem nie wypisuje. */
    static final class TodoClient {
        private final HttpClient http;
        private final URI base;

        TodoClient(HttpClient http, URI base) {
            this.http = http;
            this.base = base;
        }

        String call(String method, String path, String json) throws IOException, InterruptedException {
            HttpRequest.BodyPublisher publisher = json == null
                    ? HttpRequest.BodyPublishers.noBody()                            // noBody = bez treści
                    : HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(base.resolve(path))
                    .timeout(Duration.ofSeconds(10))                                 // timeout = limit czasu
                    .header("Content-Type", "application/json; charset=utf-8")
                    .method(method, publisher)                                       // method = dowolna metoda, np. PATCH
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String location = response.headers().firstValue("Location").map(l -> " (Location: " + l + ")").orElse("");
            return response.statusCode() + " " + response.body() + location;
        }
    }

    static TodoApi newApi() {
        return new TodoApi(new TaskService(new InMemoryTaskRepository()));
    }

    // =================================================================================================
    // 1. WYMAGANIA I KONTRAKT API
    // =================================================================================================

    /**
     * 1. Kontrakt API spisany przed kodem: zasób {@code /tasks}, metody HTTP i kody odpowiedzi.
     * To jest umowa z klientami — zmieniać ją wolno tylko świadomie (wersjonowanie).
     */
    static void requirements() {
        section("1. Wymagania i kontrakt API");

        List<String> contract = List.of(
                "GET    /tasks             → 200 lista",
                "POST   /tasks             → 201 + Location | 400 | 409 duplikat tytułu",
                "GET    /tasks/{id}        → 200 | 404",
                "PUT    /tasks/{id}        → 200 | 400 | 404 | 409",
                "PATCH  /tasks/{id}/status → 200 | 400 | 404 | 409 niedozwolone przejście",
                "DELETE /tasks/{id}        → 204 | 404");
        contract.forEach(line -> note(line));
        // WYNIK: ℹ GET    /tasks             → 200 lista
        // WYNIK: ℹ POST   /tasks             → 201 + Location | 400 | 409 duplikat tytułu
        // WYNIK: ℹ GET    /tasks/{id}        → 200 | 404
        // WYNIK: ℹ PUT    /tasks/{id}        → 200 | 400 | 404 | 409
        // WYNIK: ℹ PATCH  /tasks/{id}/status → 200 | 400 | 404 | 409 niedozwolone przejście
        // WYNIK: ℹ DELETE /tasks/{id}        → 204 | 404

        // DOBRA PRAKTYKA: zmiana statusu jako osobny zasób (PATCH /tasks/{id}/status), a nie część PUT.
        //   Dlaczego? Przejścia mają własne reguły (tabela), a PUT zastępuje dane, które użytkownik edytuje.
    }

    // =================================================================================================
    // 2. MODEL DZIEDZINY
    // =================================================================================================

    /**
     * 2. Task jest niezmienny; status zmienia się tylko przez tabelę przejść. Tabela w EnumMap to dane —
     * można ją wypisać, przetestować i pokazać w dokumentacji.
     */
    static void domainModel() {
        section("2. Model dziedziny");

        for (TaskStatus status : TaskStatus.values()) {
            show(status + " → może przejść do", status.allowedTargets().stream().sorted().toList());
        }
        // WYNIK: TODO → może przejść do → [IN_PROGRESS, CANCELLED]
        // WYNIK: IN_PROGRESS → może przejść do → [TODO, DONE, CANCELLED]
        // WYNIK: DONE → może przejść do → []
        // WYNIK: CANCELLED → może przejść do → []

        Task task = new Task(1, "Kupić mleko", 2, TaskStatus.TODO);
        show("po zmianie statusu", task.withStatus(TaskStatus.IN_PROGRESS));
        // WYNIK: po zmianie statusu → Task[id=1, title=Kupić mleko, priority=2, status=IN_PROGRESS]
        show("oryginał bez zmian", task.status());
        // WYNIK: oryginał bez zmian → TODO
        expectThrows("priorytet 9", () -> new Task(1, "X", 9, TaskStatus.TODO));
        // WYNIK: ✔ priorytet 9 → rzucono IllegalArgumentException: priorytet poza 1–5

        // PUŁAPKA: Set.copyOf (i Set.of) nie gwarantuje kolejności — dlatego przed wydrukiem sorted().
        //   Enumy sortują się w kolejności deklaracji (compareTo = ordinal), a nie alfabetycznie.
    }

    // =================================================================================================
    // 3. REPOZYTORIUM
    // =================================================================================================

    /**
     * 3. Serwis zna tylko interfejs {@code TaskRepository}. Dziś to mapa w pamięci, jutro baza (JDBC, JPA) —
     * serwis się nie zmieni. Metody są synchronized, bo HttpServer obsługuje żądania w kilku wątkach.
     */
    static void repository() {
        section("3. Repozytorium");

        TaskRepository repo = new InMemoryTaskRepository();
        repo.save(new Task(repo.nextId(), "Pierwsze", 3, TaskStatus.TODO));
        repo.save(new Task(repo.nextId(), "Drugie", 1, TaskStatus.TODO));
        show("findAll (id)", repo.findAll().stream().map(Task::id).toList());
        // WYNIK: findAll (id) → [1, 2]
        show("findById(2)", repo.findById(2).map(Task::title).orElse("brak"));
        // WYNIK: findById(2) → Drugie
        show("deleteById(7)", repo.deleteById(7));
        // WYNIK: deleteById(7) → false
        expectThrows("modyfikacja wyniku findAll", () -> repo.findAll().clear());
        // WYNIK: ✔ modyfikacja wyniku findAll → rzucono UnsupportedOperationException: (brak komunikatu)

        // DOBRA PRAKTYKA: findById zwraca Optional, a o kodzie 404 decyduje serwis/HTTP. Dlaczego? Repozytorium
        //   tylko odpowiada „jest / nie ma” — czy brak to błąd, zależy od przypadku użycia.
    }

    // =================================================================================================
    // 4. SERWIS Z REGUŁAMI
    // =================================================================================================

    /**
     * 4. Reguły biznesowe testujemy bez HTTP: unikalny tytuł (bez względu na wielkość liter), walidacja
     * zbierająca wszystkie błędy i tabela przejść statusów.
     */
    static void serviceRules() {
        section("4. Serwis z regułami");

        TaskService service = new TaskService(new InMemoryTaskRepository());
        Task milk = service.create(new TaskRequest("  Kupić mleko ", 2));
        show("utworzone", milk);
        // WYNIK: utworzone → Task[id=1, title=Kupić mleko, priority=2, status=TODO]
        expectThrows("duplikat tytułu", () -> service.create(new TaskRequest("KUPIĆ MLEKO", 1)));
        // WYNIK: ✔ duplikat tytułu → rzucono ConflictException: zadanie o tytule „KUPIĆ MLEKO” już istnieje
        expectThrows("dwa błędy naraz", () -> service.create(new TaskRequest(" ", 0)));
        // WYNIK: ✔ dwa błędy naraz → rzucono ValidationException: title: nie może być pusty; priority: musi być w zakresie 1–5
        service.changeStatus(1, TaskStatus.IN_PROGRESS);
        show("po DONE", service.changeStatus(1, TaskStatus.DONE).status());
        // WYNIK: po DONE → DONE
        expectThrows("DONE → TODO", () -> service.changeStatus(1, TaskStatus.TODO));
        // WYNIK: ✔ DONE → TODO → rzucono ConflictException: niedozwolone przejście DONE → TODO
        expectThrows("usuń 42", () -> service.delete(42));
        // WYNIK: ✔ usuń 42 → rzucono NotFoundException: brak zadania 42

        // PUŁAPKA: „sprawdź unikalność, potem zapisz” to dwie operacje. Dwa równoczesne POST z tym samym tytułem
        //   mogłyby oba przejść sprawdzenie. Tu chroni nas synchronized na metodach zmieniających serwisu;
        //   w bazie danych tę samą rolę pełni ograniczenie UNIQUE (t29_jdbc_databases/Jdbc07SqlAggregationIndexes).
    }

    // =================================================================================================
    // 5. JSON W OBIE STRONY
    // =================================================================================================

    /**
     * 5. Zapis zadania do JSON to sklejanie tekstu z ucieczką znaków; odczyt to mały parser zstępujący.
     * Liczby czytamy jako BigDecimal i dopiero potem sprawdzamy, czy to int.
     */
    static void jsonMapping() {
        section("5. JSON w obie strony");

        show("zapis", JsonMapper.write(new Task(3, "Cytat \"Lema\"", 4, TaskStatus.TODO)));
        // WYNIK: zapis → {"id":3,"title":"Cytat \"Lema\"","priority":4,"status":"TODO"}
        show("odczyt", JsonMapper.parseObject("{ \"title\": \"Kupić chleb\", \"priority\": 3, \"pilne\": true }"));
        // WYNIK: odczyt → {title=Kupić chleb, priority=3, pilne=true}
        show("na TaskRequest", JsonMapper.toRequest("{\"title\":\"Kupić chleb\",\"priority\":3}"));
        // WYNIK: na TaskRequest → TaskRequest[title=Kupić chleb, priority=3]
        expectThrows("ucięty JSON", () -> JsonMapper.parseObject("{\"title\": \"x"));
        // WYNIK: ✔ ucięty JSON → rzucono ValidationException: niepoprawny JSON: nieoczekiwany koniec (pozycja 12)
        expectThrows("priorytet 2.5", () -> JsonMapper.toRequest("{\"title\":\"x\",\"priority\":2.5}"));
        // WYNIK: ✔ priorytet 2.5 → rzucono ValidationException: priority: oczekiwano liczby całkowitej

        // PUŁAPKA: liczby z JSON czytane jako double i rzutowane na int: 2.5 po cichu staje się 2.
        //   intValueExact() rzuca wyjątek dla części ułamkowej i dla liczb spoza zakresu int.

        // DOBRA PRAKTYKA: błąd parsowania to ValidationException → 400 z pozycją w tekście. Dlaczego?
        //   Autor klienta od razu widzi, gdzie zepsuł JSON, zamiast zgadywać na podstawie „Bad Request”.
    }

    // =================================================================================================
    // 6. WARSTWA HTTP — routing i kody bez sieci
    // =================================================================================================

    /**
     * 6. Najważniejsza decyzja projektu: logika HTTP (routing, kody, JSON) to czysta funkcja
     * {@code handle(metoda, ścieżka, treść)}. Adapter na HttpServer ma kilkanaście linii i niczego nie decyduje.
     */
    static void httpLayer() {
        section("6. Warstwa HTTP — routing i kody");

        TodoApi api = newApi();
        ApiResponse created = api.handle("POST", "/tasks", "{\"title\":\"Pierwsze\",\"priority\":1}");
        show("POST → kod / Location", created.status() + " / " + created.headers().get("Location"));
        // WYNIK: POST → kod / Location → 201 / /tasks/1
        show("GET /tasks/1/status (zła metoda)", api.handle("GET", "/tasks/1/status", "").status());
        // WYNIK: GET /tasks/1/status (zła metoda) → 405
        show("GET /zadania", api.handle("GET", "/zadania", "").body());
        // WYNIK: GET /zadania → {"status":404,"message":"nieznana ścieżka /zadania"}
        show("PATCH zły status", api.handle("PATCH", "/tasks/1/status", "{\"status\":\"ZROBIONE\"}").body());
        // WYNIK: PATCH zły status → {"status":400,"message":"status: nieznana wartość ZROBIONE"}

        // PUŁAPKA: 404 dla „nie ma takiego zadania” i 405 dla „jest taka ścieżka, ale nie ta metoda”. Mieszanie ich
        //   myli klientów. Do 405 dołączamy nagłówek Allow z listą metod — tego wymaga specyfikacja HTTP.

        // DOBRA PRAKTYKA: mapowanie wyjątek → kod w JEDNYM miejscu (handle). Dlaczego? Serwis nie zna HTTP,
        //   a zasada „NotFound = 404” nie jest powtarzana w każdej metodzie. W Springu: @RestControllerAdvice.
    }

    // =================================================================================================
    // 7. OD POCZĄTKU DO KOŃCA: prawdziwy serwer + HttpClient
    // =================================================================================================

    /**
     * 7. Serwer na interfejsie loopback i porcie 0, klient HttpClient i scenariusz krok po kroku.
     * Wypisujemy metodę, ścieżkę, kod i treść — nigdy portu (inny przy każdym uruchomieniu).
     */
    static void endToEnd() throws IOException, InterruptedException {
        section("7. Od początku do końca: serwer + klient");

        TodoHttpServer server = TodoHttpServer.start(newApi());
        ExecutorService clientPool = Executors.newSingleThreadExecutor();
        try {
            HttpClient http = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .proxy(HttpClient.Builder.NO_PROXY)                              // NO_PROXY = bez serwera pośredniczącego
                    .connectTimeout(Duration.ofSeconds(5))
                    .executor(clientPool)
                    .build();
            TodoClient client = new TodoClient(http, server.baseUri());
            String[][] scenario = {
                    {"POST", "/tasks", "{\"title\":\"Kupić mleko\",\"priority\":2}"},
                    {"POST", "/tasks", "{\"title\":\"Napisać raport\",\"priority\":4}"},
                    {"POST", "/tasks", "{\"title\":\"kupić MLEKO\",\"priority\":1}"},
                    {"POST", "/tasks", "{\"title\":\"\",\"priority\":9}"},
                    {"POST", "/tasks", "{\"title\":\"x\""},
                    {"GET", "/tasks", null},
                    {"GET", "/tasks/99", null},
                    {"PATCH", "/tasks/1/status", "{\"status\":\"IN_PROGRESS\"}"},
                    {"PATCH", "/tasks/1/status", "{\"status\":\"DONE\"}"},
                    {"PATCH", "/tasks/1/status", "{\"status\":\"TODO\"}"},
                    {"PUT", "/tasks/2", "{\"title\":\"Napisać raport roczny\",\"priority\":5}"},
                    {"DELETE", "/tasks/2", null},
                    {"DELETE", "/tasks/2", null},
                    {"DELETE", "/tasks", null},
                    {"GET", "/tasks", null}};
            for (String[] step : scenario) {
                show(step[0] + " " + step[1], client.call(step[0], step[1], step[2]));
            }
        } finally {
            server.stop();
            clientPool.shutdownNow();
        }
        // WYNIK: POST /tasks → 201 {"id":1,"title":"Kupić mleko","priority":2,"status":"TODO"} (Location: /tasks/1)
        // WYNIK: POST /tasks → 201 {"id":2,"title":"Napisać raport","priority":4,"status":"TODO"} (Location: /tasks/2)
        // WYNIK: POST /tasks → 409 {"status":409,"message":"zadanie o tytule „kupić MLEKO” już istnieje"}
        // WYNIK: POST /tasks → 400 {"status":400,"message":"title: nie może być pusty; priority: musi być w zakresie 1–5"}
        // WYNIK: POST /tasks → 400 {"status":400,"message":"niepoprawny JSON: oczekiwano '}' (pozycja 12)"}
        // WYNIK: GET /tasks → 200 [{"id":1,"title":"Kupić mleko","priority":2,"status":"TODO"},{"id":2,"title":"Napisać raport","priority":4,"status":"TODO"}]
        // WYNIK: GET /tasks/99 → 404 {"status":404,"message":"brak zadania 99"}
        // WYNIK: PATCH /tasks/1/status → 200 {"id":1,"title":"Kupić mleko","priority":2,"status":"IN_PROGRESS"}
        // WYNIK: PATCH /tasks/1/status → 200 {"id":1,"title":"Kupić mleko","priority":2,"status":"DONE"}
        // WYNIK: PATCH /tasks/1/status → 409 {"status":409,"message":"niedozwolone przejście DONE → TODO"}
        // WYNIK: PUT /tasks/2 → 200 {"id":2,"title":"Napisać raport roczny","priority":5,"status":"TODO"}
        // WYNIK: DELETE /tasks/2 → 204
        // WYNIK: DELETE /tasks/2 → 404 {"status":404,"message":"brak zadania 2"}
        // WYNIK: DELETE /tasks → 405 {"status":405,"message":"dozwolone metody: GET, POST"}
        // WYNIK: GET /tasks → 200 [{"id":1,"title":"Kupić mleko","priority":2,"status":"DONE"}]

        // PUŁAPKA: sendResponseHeaders(204, 0). Zero oznacza w HttpServer „długość nieznana, wyślę kawałkami”,
        //   a odpowiedź 204 nie może mieć treści. Bez treści podajemy -1.

        // PUŁAPKA: body.getBytes() bez Charset i Content-Length policzone z długości STRINGA. „ć” to 1 znak, ale
        //   2 bajty w UTF-8 — długość w bajtach musi pochodzić z tablicy bajtów, a kodowanie musi być jawne.

        // DOBRA PRAKTYKA: zatrzymanie serwera i pul w finally. Dlaczego? Wyjątek w środku scenariusza nie może
        //   zostawić zajętego portu ani działających wątków, które nie pozwolą JVM się zakończyć.
    }

    // =================================================================================================
    // 8. TESTY ZACHOWANIA
    // =================================================================================================

    /** 8. Testy na czystej funkcji handle — szybkie, bez sieci; kontrakt API sprawdzony kod po kodzie. */
    static void behaviourTests() {
        section("8. Testy zachowania");

        TodoApi api = newApi();
        api.handle("POST", "/tasks", "{\"title\":\"A\",\"priority\":1}");
        Check.equal("POST → 201", 201, api.handle("POST", "/tasks", "{\"title\":\"B\",\"priority\":2}").status());
        Check.equal("PUT na cudzy tytuł → 409", 409, api.handle("PUT", "/tasks/2", "{\"title\":\"a\",\"priority\":2}").status());
        Check.equal("PUT z własnym tytułem → 200", 200, api.handle("PUT", "/tasks/2", "{\"title\":\"B\",\"priority\":5}").status());
        Check.equal("DELETE → 204 bez treści", "204:", () -> {
            ApiResponse r = api.handle("DELETE", "/tasks/1", "");
            return r.status() + ":" + r.body();
        });
        Check.equal("405 ma nagłówek Allow", "PATCH", api.handle("PUT", "/tasks/2/status", "").headers().get("Allow"));
        Check.equal("brak priority → 400", 400, api.handle("POST", "/tasks", "{\"title\":\"C\"}").status());
        Check.equal("CANCELLED jest końcowy", false, TaskStatus.CANCELLED.canMoveTo(TaskStatus.TODO));
        Check.equal("ukośnik na końcu /tasks/ działa", 200, api.handle("GET", "/tasks/", "").status());
        Check.summary();
        // WYNIK: ✔ OK    POST → 201
        // WYNIK: ✔ OK    PUT na cudzy tytuł → 409
        // WYNIK: ✔ OK    PUT z własnym tytułem → 200
        // WYNIK: ✔ OK    DELETE → 204 bez treści
        // WYNIK: ✔ OK    405 ma nagłówek Allow
        // WYNIK: ✔ OK    brak priority → 400
        // WYNIK: ✔ OK    CANCELLED jest końcowy
        // WYNIK: ✔ OK    ukośnik na końcu /tasks/ działa
        // WYNIK: PODSUMOWANIE: ✔ 8 OK, ✘ 0 BŁĄD

        // DOBRA PRAKTYKA: większość testów bez sieci (szybkie, stabilne), jeden scenariusz przez prawdziwy HTTP
        //   (sekcja 7) — sprawdza adapter, nagłówki i kodowanie. To samo robi Spring: MockMvc + kilka testów
        //   z @SpringBootTest(webEnvironment = RANDOM_PORT).
    }

    // =================================================================================================
    // 9. PRZEŁOŻENIE NA SPRING BOOT
    // =================================================================================================

    /**
     * 9. Każda nasza klasa ma odpowiednik w Spring Boot. Kod poniżej (w komentarzu) to szkic kontrolera —
     * porównaj go z TodoApi: routing i mapowanie JSON robi za nas framework.
     * <pre>{@code
     * @RestController
     * @RequestMapping("/tasks")
     * class TaskController {
     *     private final TaskService service;                  // wstrzyknięty przez konstruktor
     *     TaskController(TaskService service) { this.service = service; }
     *
     *     @GetMapping List<Task> list() { return service.list(); }
     *
     *     @PostMapping
     *     ResponseEntity<Task> create(@Valid @RequestBody TaskRequest request) {
     *         Task task = service.create(request);
     *         return ResponseEntity.created(URI.create("/tasks/" + task.id())).body(task);
     *     }
     *
     *     @PatchMapping("/{id}/status")
     *     Task changeStatus(@PathVariable long id, @RequestBody StatusRequest body) {
     *         return service.changeStatus(id, body.status());
     *     }
     *
     *     @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
     *     void delete(@PathVariable long id) { service.delete(id); }
     * }
     *
     * @RestControllerAdvice
     * class ErrorHandler {
     *     @ExceptionHandler(NotFoundException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
     *     ErrorBody notFound(NotFoundException e) { return new ErrorBody(404, e.getMessage()); }
     * }
     * }</pre>
     */
    static void springBootMapping() {
        section("9. Przełożenie na Spring Boot");

        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("TodoApi.route", "@RestController + @GetMapping/@PostMapping/...");
        mapping.put("TaskService", "@Service (+ @Transactional przy bazie)");
        mapping.put("InMemoryTaskRepository", "@Repository / interface TaskRepository extends JpaRepository<Task, Long>");
        mapping.put("JsonMapper", "Jackson — automatycznie dla @RequestBody / wartości zwracanej");
        mapping.put("validate(...)", "@Valid + @NotBlank, @Size(max = 60), @Min(1) @Max(5) na TaskRequest");
        mapping.put("handle: wyjątek → kod", "@RestControllerAdvice + @ExceptionHandler");
        mapping.put("ApiResponse(201, Location)", "ResponseEntity.created(uri).body(task)");
        mapping.put("TodoHttpServer", "wbudowany Tomcat uruchamiany przez SpringApplication.run");
        showEach("nasza klasa → Spring Boot", mapping);
        // WYNIK: nasza klasa → Spring Boot (liczba kluczy: 8):
        // WYNIK: • TodoApi.route → @RestController + @GetMapping/@PostMapping/...
        // WYNIK: • TaskService → @Service (+ @Transactional przy bazie)
        // WYNIK: • InMemoryTaskRepository → @Repository / interface TaskRepository extends JpaRepository<Task, Long>
        // WYNIK: • JsonMapper → Jackson — automatycznie dla @RequestBody / wartości zwracanej
        // WYNIK: • validate(...) → @Valid + @NotBlank, @Size(max = 60), @Min(1) @Max(5) na TaskRequest
        // WYNIK: • handle: wyjątek → kod → @RestControllerAdvice + @ExceptionHandler
        // WYNIK: • ApiResponse(201, Location) → ResponseEntity.created(uri).body(task)
        // WYNIK: • TodoHttpServer → wbudowany Tomcat uruchamiany przez SpringApplication.run

        // Do powtórki przed kursem SpringLearning: t34_toward_spring/Spring03RestConcepts,
        //   t34_toward_spring/Spring01IocContainer, t28_networking_http/Http04JsonApi, t32_junit_mockito/JUnit04Mockito.
        // DOBRA PRAKTYKA: w Springu zostaw TaskService prawie bez zmian — reguły już są niezależne od HTTP.
        //   Przenosisz brzegi (kontroler, repozytorium), a testy reguł działają dalej.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Warstwy: HTTP (routing, JSON, kody) → serwis (reguły) → repozytorium (interfejs) → implementacja.
     *   • Logika HTTP jako czysta funkcja handle(metoda, ścieżka, treść); HttpServer tylko jako cienki adapter.
     *   • Kody: 200 OK, 201 Created + Location, 204 No Content (bez treści, długość -1), 400 zły format/walidacja,
     *     404 brak zasobu, 405 zła metoda + Allow, 409 konflikt z regułą (duplikat, przejście).
     *   • Wyjątki domenowe w zamkniętej hierarchii; mapowanie na kody w JEDNYM miejscu.
     *   • Tabela przejść statusów: EnumMap<Status, EnumSet<Status>> + canMoveTo.
     *   • JSON: ucieczka " \ \n; liczby jako BigDecimal → intValueExact; błąd parsowania → 400 z pozycją.
     *   • Serwer: loopback + port 0, pula wątków, stop i shutdown w finally; repozytorium/serwis bezpieczne dla wątków.
     *   • Klient: HttpClient z NO_PROXY dla localhost, timeout, BodyPublishers.ofString(json, UTF_8), method("PATCH", ...).
     *   • Treść zawsze w UTF-8, Content-Length z liczby BAJTÓW.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego TodoApi.handle nie dostaje HttpExchange, tylko (metoda, ścieżka, treść)?
     *   2. Co wypisze:  System.out.println("Kupić".length() + " " + "Kupić".getBytes(StandardCharsets.UTF_8).length);  ?
     *   3. ZNAJDŹ BŁĄD:  exchange.sendResponseHeaders(204, 0);   // odpowiedź na DELETE bez treści
     *   4. Kiedy zwrócić 404, kiedy 405, a kiedy 409? Podaj przykład z tego projektu dla każdego.
     *   5. Co wypisze:  System.out.println(TaskStatus.IN_PROGRESS.canMoveTo(TaskStatus.TODO) + " "
     *                                      + TaskStatus.DONE.canMoveTo(TaskStatus.CANCELLED));  ?
     *   6. ZNAJDŹ BŁĄD:  int priority = (int) Double.parseDouble(json.get("priority"));   // "2.5" → ?
     *   7. Po co serwer nasłuchuje na InetAddress.getLoopbackAddress() i porcie 0?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        List<Task> tasks = sampleTasks();
        List<Object[]> moves = List.of(new Object[] {2L, TaskStatus.DONE}, new Object[] {1L, TaskStatus.IN_PROGRESS},
                new Object[] {4L, TaskStatus.IN_PROGRESS}, new Object[] {5L, TaskStatus.TODO}, new Object[] {9L, TaskStatus.DONE});
        Check.equal("ćw. 1: TODO wg priorytetu", List.of(4L, 1L, 6L), () -> exercise1(tasks, TaskStatus.TODO));
        Check.equal("ćw. 2: kody nowej reguły", List.of(200, 409, 409, 409, 404),
                () -> moves.stream().map(m -> exercise2(tasks, (Long) m[0], (TaskStatus) m[1])).toList());
        Check.equal("ćw. 3: tablica JSON", expectedJsonArray(), () -> exercise3(tasks.subList(0, 2)));
        Check.equal("ćw. 4: parametry zapytania", expectedQuery(), () -> exercise4("status=DONE&title=kup%20mleko"));
        Check.throwsException("ćw. 4: nieznany parametr", IllegalArgumentException.class, () -> exercise4("kolor=red"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of(4L, 1L, 6L), () -> solution1(tasks, TaskStatus.TODO));
        Check.equal("ćw. 2 (wzorzec)", List.of(200, 409, 409, 409, 404),
                () -> moves.stream().map(m -> solution2(tasks, (Long) m[0], (TaskStatus) m[1])).toList());
        Check.equal("ćw. 3 (wzorzec)", expectedJsonArray(), () -> solution3(tasks.subList(0, 2)));
        Check.equal("ćw. 4 (wzorzec)", expectedQuery(), () -> solution4("status=DONE&title=kup%20mleko"));
        Check.throwsException("ćw. 4: nieznany parametr (wzorzec)", IllegalArgumentException.class,
                () -> solution4("kolor=red"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    static List<Task> sampleTasks() {
        return List.of(
                new Task(1, "Kupić mleko", 2, TaskStatus.TODO),
                new Task(2, "Napisać raport", 4, TaskStatus.IN_PROGRESS),
                new Task(3, "Umyć auto", 1, TaskStatus.IN_PROGRESS),
                new Task(4, "Zapłacić rachunki", 5, TaskStatus.TODO),
                new Task(5, "Stare zadanie", 3, TaskStatus.DONE),
                new Task(6, "Podlać kwiaty", 2, TaskStatus.TODO));
    }

    static String expectedJsonArray() {
        return "[{\"id\":1,\"title\":\"Kupić mleko\",\"priority\":2,\"status\":\"TODO\"},"
                + "{\"id\":2,\"title\":\"Napisać raport\",\"priority\":4,\"status\":\"IN_PROGRESS\"}]";
    }

    static Map<String, String> expectedQuery() {
        return new TreeMap<>(Map.of("status", "DONE", "title", "kup mleko"));
    }

    /**
     * ĆWICZENIE 1 (łatwe): logika nowego punktu końcowego {@code GET /tasks?status=TODO} — id zadań o danym
     * statusie, posortowane malejąco po priorytecie, a przy remisie rosnąco po id.
     * Podpowiedź: Comparator.comparingInt(Task::priority).reversed().thenComparingLong(Task::id).
     */
    static List<Long> exercise1(List<Task> tasks, TaskStatus status) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): nowa reguła serwisu — najwyżej 2 zadania naraz mogą być IN_PROGRESS.
     * Zwróć kod HTTP, jaki dałby PATCH: 404 (brak zadania), 409 (przejście spoza tabeli albo przekroczony limit),
     * 200 (dozwolone). Przykłady na sampleTasks(): 2→DONE = 200; 1→IN_PROGRESS = 409 (już są dwa); 5→TODO = 409.
     * Podpowiedź: najpierw szukaj zadania (Optional), potem canMoveTo, na końcu policz IN_PROGRESS.
     */
    static int exercise2(List<Task> tasks, long id, TaskStatus target) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, łączy z t16_streams/Streams09CollectorsBasic): zapisz listę zadań jako tablicę JSON
     * bez spacji, używając JsonMapper.write(Task) dla elementów: {@code [{...},{...}]}; pusta lista → {@code []}.
     * Podpowiedź: Collectors.joining(",", "[", "]").
     */
    static String exercise3(List<Task> tasks) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): odczyt parametrów zapytania (query string) dla {@code GET /tasks?...}.
     * Dozwolone klucze: status, title, minPriority. Wartości dekoduj z URL (%20 → spacja) w UTF-8.
     * Nieznany klucz albo para bez '=' → IllegalArgumentException. Wynik: TreeMap.
     * Podpowiedź: split("&"), potem split("=", 2), URLDecoder.decode(tekst, StandardCharsets.UTF_8) (Java 10+).
     */
    static Map<String, String> exercise4(String rawQuery) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<Long> solution1(List<Task> tasks, TaskStatus status) {
        return tasks.stream()
                .filter(t -> t.status() == status)
                .sorted(Comparator.comparingInt(Task::priority).reversed().thenComparingLong(Task::id))
                .map(Task::id)
                .toList();
    }

    static int solution2(List<Task> tasks, long id, TaskStatus target) {
        final int maxInProgress = 2;
        Optional<Task> found = tasks.stream().filter(t -> t.id() == id).findFirst();
        if (found.isEmpty()) {                                                       // isEmpty (Java 11+)
            return 404;
        }
        Task task = found.get();
        if (!task.status().canMoveTo(target)) {
            return 409;
        }
        long inProgress = tasks.stream().filter(t -> t.status() == TaskStatus.IN_PROGRESS).count();
        if (target == TaskStatus.IN_PROGRESS && inProgress >= maxInProgress) {
            return 409;
        }
        return 200;
    }

    static String solution3(List<Task> tasks) {
        return tasks.stream().map(JsonMapper::write).collect(Collectors.joining(",", "[", "]"));
    }

    static Map<String, String> solution4(String rawQuery) {
        Set<String> allowed = Set.of("status", "title", "minPriority");
        Map<String, String> result = new TreeMap<>();
        for (String pair : rawQuery.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length != 2 || !allowed.contains(kv[0])) {
                throw new IllegalArgumentException("nieznany lub niepełny parametr: " + pair);
            }
            result.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo wtedy logika HTTP nie zależy od konkretnego serwera: testujemy ją bez sieci i portów, a zmiana
     *      HttpServer na inny serwer (albo na Springa) dotyczy tylko kilkunastu linii adaptera.
     *   2. 5 6 — „ć” to jeden znak (char), ale w UTF-8 zajmuje 2 bajty. Dlatego Content-Length liczymy z bajtów.
     *   3. 0 w sendResponseHeaders znaczy „treść o nieznanej długości, kodowanie kawałkami (chunked)”, a 204 nie
     *      może mieć treści. Dla odpowiedzi bez treści podajemy -1.
     *   4. 404 — nie ma zasobu (GET /tasks/99, DELETE już usuniętego). 405 — ścieżka istnieje, ale nie obsługuje
     *      metody (DELETE /tasks). 409 — żądanie poprawne, ale łamie regułę stanu (duplikat tytułu, DONE → TODO).
     *   5. true false — z IN_PROGRESS wolno wrócić do TODO, a DONE jest stanem końcowym (pusty zbiór przejść).
     *   6. Rzutowanie (int) 2.5 daje po cichu 2 — klient myśli, że zapisał 2.5, a zapisało się 2. Poprawnie:
     *      new BigDecimal(tekst).intValueExact() i zamiana ArithmeticException na 400.
     *   7. Loopback = serwer dostępny tylko z tego komputera (bezpieczne, bez zapory i sieci). Port 0 = system
     *      wybiera wolny port, więc lekcja nie koliduje z innym programem; port odczytujemy z getAddress().getPort().
     */
    // </editor-fold>
}
