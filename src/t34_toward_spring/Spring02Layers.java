package t34_toward_spring;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Product;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Warstwy aplikacji — kontroler, serwis, repozytorium (w czystej Javie, tak jak w Springu)
 *        (layer = warstwa; controller = kontroler; service = serwis/usługa; repository = repozytorium;
 *         DTO = Data Transfer Object = obiekt do przenoszenia danych; entity = encja)
 *
 * W SKRÓCIE:
 *   Aplikację dzielimy na warstwy o jednej odpowiedzialności: kontroler rozmawia ze światem (HTTP, walidacja,
 *   statusy), serwis pilnuje reguł biznesowych i transakcji, repozytorium przechowuje dane. Zależności płyną
 *   tylko w dół, a między warstwami krążą osobne typy: DTO na zewnątrz, encje w środku.
 *
 * ANALOGIA:
 *   Restauracja: kelner (kontroler) przyjmuje zamówienie i sprawdza, czy jest czytelne; kuchnia (serwis) wie,
 *   jak gotować i czego nie łączyć; spiżarnia (repozytorium) tylko przechowuje produkty. Kelner nie gotuje,
 *   kucharz nie wychodzi do gości, a spiżarnia nie zna menu. Karta dań (DTO) to nie to samo co przepis (encja).
 *
 * JAK TO DZIAŁA:
 *     żądanie (JSON) ─► KONTROLER ─► walidacja DTO ─► SERWIS (reguły + transakcja) ─► REPOZYTORIUM (dane)
 *                         ▲                               │
 *     odpowiedź (status) ◄┴── tłumacz wyjątków ◄──────────┘ (wyjątek domenowy → 404/409/400)
 *
 *   Warstwa     | Typy na wejściu/wyjściu      | Wie o...                  | Spring
 *   kontroler   | DTO żądania/odpowiedzi       | serwisie, HTTP            | @RestController
 *   serwis      | encje / obiekty domeny       | repozytoriach (interfejs) | @Service + @Transactional
 *   repozytorium| encje                        | bazie danych              | @Repository / Spring Data
 *
 * SŁÓWKA:
 *   request = żądanie; response = odpowiedź; mapper = odwzorowywacz (zamiana typów); validator = walidator;
 *   translate = przetłumacz; fake = atrapa; feature = funkcjonalność; place order = złóż zamówienie;
 *   stock = stan magazynowy; cancel = anuluj; rollback = wycofanie; commit = zatwierdzenie
 *
 * ZOBACZ TEŻ: t34_toward_spring/Spring01IocContainer (kto składa warstwy), t29_jdbc_databases/Jdbc05Dao (repozytorium na JDBC),
 *   t29_jdbc_databases/Jdbc04Transactions (prawdziwe transakcje), t19_annotations_reflection/Annotations04Validator (walidator),
 *   t34_toward_spring/Spring03RestConcepts (HTTP i JSON)
 * </pre>
 */
public class Spring02Layers {

    public static void main(String[] args) {
        title("Spring02 — warstwy: kontroler, serwis, repozytorium");

        dependencyDirection();    // dependency direction = kierunek zależności
        entityVsDto();            // entity vs DTO = encja a DTO
        repositoryLayer();        // repository layer = warstwa repozytorium
        serviceRules();           // service rules = reguły w serwisie
        transactionBoundary();    // transaction boundary = granica transakcji
        validationAtEdge();       // validation at edge = walidacja na brzegu
        controllerAndErrors();    // controller and errors = kontroler i błędy
        packageLayout();          // package layout = układ pakietów
        testingWithFakes();       // testing with fakes = testy z atrapami
        whereAnnotationsGo();     // where annotations go = gdzie stoją adnotacje
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // MODEL: ENCJE (wnętrze) i DTO (brzeg)
    // =================================================================================================

    /** Encja pozycji: cena zapisana w chwili zakupu (unit price = cena jednostkowa). */
    record OrderLineEntity(String sku, int quantity, BigDecimal unitPrice) {
        BigDecimal total() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
    }

    /** Encja zamówienia: pola wewnętrzne (internalNote = notatka wewnętrzna) nie powinny wyciec do API. */
    record OrderEntity(long id, String customerEmail, List<OrderLineEntity> lines, String status, String internalNote) {
        OrderEntity {
            lines = List.copyOf(lines);
        }
        BigDecimal total() { return lines.stream().map(OrderLineEntity::total).reduce(BigDecimal.ZERO, BigDecimal::add); }
        OrderEntity withId(long newId) { return new OrderEntity(newId, customerEmail, lines, status, internalNote); }
        OrderEntity withStatus(String s) { return new OrderEntity(id, customerEmail, lines, s, internalNote); }
    }

    /** DTO żądania — to, co klient przysyła (z adnotacjami walidacji). */
    record CreateOrderRequest(@NotBlank @Email String customerEmail, @NotEmpty @Valid List<LineRequest> lines) { }

    record LineRequest(@NotBlank String sku, @Positive int quantity) { }

    /** DTO odpowiedzi — to, co klient dostaje (bez internalNote, z gotową sumą). */
    record OrderResponse(long id, String customer, int items, String total, String status) { }

    /** Odpowiedź „HTTP” w naszej symulacji: status + ciało. */
    record ApiResponse(int status, Object body) { }

    /** Ciało błędu (jak JSON {"error": ..., "details": [...]}). */
    record ErrorBody(String error, List<String> details) { }

    /** Mapper = zamiana typów między warstwami; statyczne metody, zero logiki biznesowej. */
    static final class OrderMapper {
        private OrderMapper() { }

        static OrderResponse toResponse(OrderEntity e) {
            int items = e.lines().stream().mapToInt(OrderLineEntity::quantity).sum();
            return new OrderResponse(e.id(), e.customerEmail(), items, e.total().toPlainString() + " zł", e.status());
        }
    }

    // =================================================================================================
    // 1. KIERUNEK ZALEŻNOŚCI
    // =================================================================================================

    /** Celowo zła klasa: repozytorium, które zna kontroler. */
    static class ReportingRepository {                                   // reporting = raportujące
        ReportingRepository(OrderController controller) { }
    }

    /**
     * 1. Zależności płyną TYLKO w dół: kontroler → serwis → repozytorium. Sprawdzamy to refleksją: patrzymy
     * na typy parametrów konstruktora każdej klasy i porównujemy „wysokość” warstwy.
     */
    static void dependencyDirection() {
        section("1. Kierunek zależności: kontroler → serwis → repozytorium");

        Map<Class<?>, Integer> layer = new LinkedHashMap<>();                // 3 = kontroler, 2 = serwis, 1 = repo
        layer.put(OrderController.class, 3);
        layer.put(OrderService.class, 2);
        layer.put(OrderRepository.class, 1);
        layer.put(StockRepository.class, 1);
        layer.put(ProductCatalog.class, 1);
        layer.put(ReportingRepository.class, 1);

        for (Class<?> c : List.of(OrderController.class, OrderService.class, ReportingRepository.class)) {
            List<String> deps = new ArrayList<>();
            List<String> violations = new ArrayList<>();                     // violations = naruszenia
            for (Class<?> p : c.getDeclaredConstructors()[0].getParameterTypes()) {
                deps.add(p.getSimpleName());
                if (layer.getOrDefault(p, 0) > layer.get(c)) violations.add(p.getSimpleName());
            }
            show(c.getSimpleName(), deps + (violations.isEmpty() ? " OK" : " NARUSZENIE: " + violations));
        }
        // WYNIK: OrderController → [OrderService, MiniValidator, ErrorTranslator] OK
        // WYNIK: OrderService → [OrderRepository, StockRepository, ProductCatalog, TxManager] OK
        // WYNIK: ReportingRepository → [OrderController] NARUSZENIE: [OrderController]

        // PUŁAPKA: repozytorium (albo encja) wołające kontroler/serwis tworzy pętlę warstw — zmiana w API
        // psuje dostęp do danych, a testy jednej warstwy wymagają całej aplikacji.
        // DOBRA PRAKTYKA: serwis zależy od INTERFEJSU repozytorium (OrderRepository), a nie od implementacji —
        // wtedy pamięć, JDBC czy JPA da się podmienić bez dotykania serwisu.
        // W Springu: tę samą regułę sprawdzają w testach biblioteki typu ArchUnit (np. „klasy z pakietu
        //            ..repository.. nie zależą od ..controller..”); kontener sam jej nie pilnuje.
    }

    // =================================================================================================
    // 2. ENCJA A DTO
    // =================================================================================================

    /**
     * 2. Encja opisuje dane tak, jak przechowuje je aplikacja (z polami wewnętrznymi). DTO opisuje kontrakt
     * API: co klient wysyła i co dostaje. Osobne typy = możesz zmienić bazę bez łamania klientów (i odwrotnie).
     */
    static void entityVsDto() {
        section("2. Encja (wnętrze) a DTO (brzeg) i mapowanie");

        OrderEntity entity = new OrderEntity(7, "ola@example.com",
                List.of(new OrderLineEntity("SPO-001", 2, new BigDecimal("64.99")),
                        new OrderLineEntity("KSI-001", 1, new BigDecimal("79.00"))),
                "NOWE", "klient prosił o fakturę");
        show("encja", entity);
        // WYNIK: encja → OrderEntity[id=7, customerEmail=ola@example.com, lines=[OrderLineEntity[sku=SPO-001, quantity=2, unitPrice=64.99], OrderLineEntity[sku=KSI-001, quantity=1, unitPrice=79.00]], status=NOWE, internalNote=klient prosił o fakturę]
        show("DTO odpowiedzi", OrderMapper.toResponse(entity));
        // WYNIK: DTO odpowiedzi → OrderResponse[id=7, customer=ola@example.com, items=3, total=208.98 zł, status=NOWE]
        // internalNote nie trafiła do odpowiedzi; suma policzona raz, w jednym miejscu.

        // PUŁAPKA: zwracanie encji prosto z kontrolera = wyciek pól wewnętrznych (notatki, hasła, ceny zakupu)
        // i „przyspawanie” API do schematu bazy: zmiana nazwy kolumny łamie klientów.
        // DOBRA PRAKTYKA: rekordy jako DTO (niezmienne, krótkie) + mapper bez logiki biznesowej.
        // W Springu: DTO to zwykłe rekordy (Jackson zamienia je na JSON); encja JPA to klasa z @Entity, @Id
        //            (musi mieć konstruktor bezargumentowy, więc NIE może być rekordem). Mapowanie ręczne
        //            albo biblioteką MapStruct.
    }

    // =================================================================================================
    // 3. REPOZYTORIUM: INTERFEJS + IMPLEMENTACJA W PAMIĘCI
    // =================================================================================================

    interface OrderRepository {
        OrderEntity save(OrderEntity order);                                 // save = zapisz
        Optional<OrderEntity> findById(long id);                             // find by id = znajdź po id
        List<OrderEntity> findAll();                                         // find all = znajdź wszystkie
    }

    interface StockRepository {
        int available(String sku);                                           // available = dostępne
        void change(String sku, int delta);                                  // change = zmień o delta
    }

    @FunctionalInterface
    interface ProductCatalog { Optional<Product> find(String sku); }        // catalog = katalog

    /** Wspólny „magazyn danych” w pamięci — z migawką (snapshot) do wycofywania transakcji. */
    static final class InMemoryStore {
        private TreeMap<Long, OrderEntity> orders = new TreeMap<>();
        private TreeMap<String, Integer> stock = new TreeMap<>();
        private long nextId = 1;

        Object[] snapshot() { return new Object[]{new TreeMap<>(orders), new TreeMap<>(stock), nextId}; }

        @SuppressWarnings("unchecked")                                       // suppress warnings = wycisz ostrzeżenia
        void restore(Object[] s) {
            orders = (TreeMap<Long, OrderEntity>) s[0];
            stock = (TreeMap<String, Integer>) s[1];
            nextId = (Long) s[2];
        }
    }

    static final class InMemoryOrderRepository implements OrderRepository {
        private final InMemoryStore store;
        InMemoryOrderRepository(InMemoryStore store) { this.store = store; }

        @Override public OrderEntity save(OrderEntity order) {
            OrderEntity saved = order.id() == 0 ? order.withId(store.nextId++) : order;   // 0 = jeszcze bez id
            store.orders.put(saved.id(), saved);
            return saved;
        }
        @Override public Optional<OrderEntity> findById(long id) { return Optional.ofNullable(store.orders.get(id)); }
        @Override public List<OrderEntity> findAll() { return List.copyOf(store.orders.values()); }
    }

    static final class InMemoryStockRepository implements StockRepository {
        private final InMemoryStore store;
        InMemoryStockRepository(InMemoryStore store) { this.store = store; }
        @Override public int available(String sku) { return store.stock.getOrDefault(sku, 0); }
        @Override public void change(String sku, int delta) { store.stock.merge(sku, delta, Integer::sum); }
    }

    /** Katalog z SampleData + stany magazynowe startowe. */
    static InMemoryStore newStore() {
        InMemoryStore store = new InMemoryStore();
        SampleData.products().forEach(p -> store.stock.put(p.sku(), p.stock()));
        return store;
    }

    static ProductCatalog sampleCatalog() {
        return sku -> SampleData.products().stream().filter(p -> p.sku().equals(sku)).findFirst();
    }

    /**
     * 3. Repozytorium to „kolekcja encji”: zapisz, znajdź po id, znajdź wszystkie. Nic o HTTP, nic o regułach.
     * Interfejs pozwala zacząć od pamięci, a potem przejść na JDBC (t29) bez zmiany serwisu.
     */
    static void repositoryLayer() {
        section("3. Repozytorium: interfejs + implementacja w pamięci");

        OrderRepository repo = new InMemoryOrderRepository(newStore());
        OrderEntity first = repo.save(new OrderEntity(0, "a@example.com", List.of(), "NOWE", ""));
        OrderEntity second = repo.save(new OrderEntity(0, "b@example.com", List.of(), "NOWE", ""));
        show("nadane id", first.id() + ", " + second.id());
        // WYNIK: nadane id → 1, 2
        show("findById(2)", repo.findById(2).map(OrderEntity::customerEmail));
        // WYNIK: findById(2) → Optional[b@example.com]
        show("findById(99)", repo.findById(99));
        // WYNIK: findById(99) → Optional.empty

        // PUŁAPKA: repozytorium zwracające null zamiast Optional przenosi NullPointerException do serwisu
        // (t14_optional/Optional01Basics). Optional w findById wymusza decyzję „co, jeśli brak”.
        // W Springu: interface OrderRepository extends JpaRepository<OrderEntity, Long> { } — Spring Data
        //            generuje save/findById/findAll sam (mechanizm pokazujemy w Spring04); @Repository na
        //            własnej implementacji dodaje tłumaczenie wyjątków bazy na DataAccessException.
    }

    // =================================================================================================
    // 4. SERWIS: REGUŁY BIZNESOWE I WYJĄTKI DOMENOWE
    // =================================================================================================

    /** Wyjątek domenowy = błąd w języku biznesu (nie HTTP, nie SQL). */
    static class DomainException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        DomainException(String message) { super(message); }
    }

    static class NotFoundException extends DomainException {
        private static final long serialVersionUID = 1L;
        NotFoundException(String message) { super(message); }
    }

    static class BusinessRuleException extends DomainException {         // business rule = reguła biznesowa
        private static final long serialVersionUID = 1L;
        BusinessRuleException(String message) { super(message); }
    }

    static class ValidationException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final List<String> errors;
        ValidationException(List<String> errors) { super("błędne dane"); this.errors = List.copyOf(errors); }
    }

    /** Mini menedżer transakcji: migawka danych przed, przywrócenie przy wyjątku. */
    static final class TxManager {
        private final InMemoryStore store;
        final List<String> log = new ArrayList<>();
        TxManager(InMemoryStore store) { this.store = store; }

        <T> T inTransaction(String name, Supplier<T> work) {
            Object[] before = store.snapshot();
            log.add("BEGIN " + name);
            try {
                T result = work.get();
                log.add("COMMIT " + name);
                return result;
            } catch (RuntimeException e) {
                store.restore(before);
                log.add("ROLLBACK " + name + " (" + e.getMessage() + ")");
                throw e;
            }
        }
    }

    static class OrderService {
        private final OrderRepository orders;
        private final StockRepository stock;
        private final ProductCatalog catalog;
        private final TxManager tx;

        OrderService(OrderRepository orders, StockRepository stock, ProductCatalog catalog, TxManager tx) {
            this.orders = orders;
            this.stock = stock;
            this.catalog = catalog;
            this.tx = tx;
        }

        OrderEntity placeOrder(CreateOrderRequest request) {
            return tx.inTransaction("placeOrder", () -> placeOrderWithoutTransaction(request));
        }

        /** Sama logika; publicznie wołamy ją tylko w sekcji 5, żeby pokazać skutki braku transakcji. */
        OrderEntity placeOrderWithoutTransaction(CreateOrderRequest request) {
            List<OrderLineEntity> lines = new ArrayList<>();
            for (LineRequest line : request.lines()) {
                Product product = catalog.find(line.sku())
                        .orElseThrow(() -> new NotFoundException("nie ma produktu " + line.sku()));
                if (stock.available(line.sku()) < line.quantity()) {
                    throw new BusinessRuleException("za mało towaru: " + product.name() + " (jest " + stock.available(line.sku()) + ")");
                }
                stock.change(line.sku(), -line.quantity());
                lines.add(new OrderLineEntity(line.sku(), line.quantity(), product.price()));
            }
            return orders.save(new OrderEntity(0, request.customerEmail(), lines, "NOWE", "utworzone przez API"));
        }

        OrderEntity get(long id) {
            return orders.findById(id).orElseThrow(() -> new NotFoundException("nie ma zamówienia " + id));
        }

        OrderEntity cancel(long id) {
            return tx.inTransaction("cancel", () -> {
                OrderEntity order = get(id);
                if (!order.status().equals("NOWE")) {
                    throw new BusinessRuleException("nie można anulować zamówienia w stanie " + order.status());
                }
                order.lines().forEach(l -> stock.change(l.sku(), l.quantity()));    // zwrot na magazyn
                return orders.save(order.withStatus("ANULOWANE"));
            });
        }
    }

    /** Składanie całej aplikacji (to robi kontener z Spring01). */
    static final class App {
        final InMemoryStore store = newStore();
        final TxManager tx = new TxManager(store);
        final StockRepository stock = new InMemoryStockRepository(store);
        final OrderService service = new OrderService(new InMemoryOrderRepository(store), stock, sampleCatalog(), tx);
        final OrderController controller = new OrderController(service, new MiniValidator(), new ErrorTranslator());

        void printTx() {
            showEach("transakcje", tx.log);
            tx.log.clear();
        }
    }

    static CreateOrderRequest order(String email, String... skuQty) {
        List<LineRequest> lines = new ArrayList<>();
        for (int i = 0; i < skuQty.length; i += 2) lines.add(new LineRequest(skuQty[i], Integer.parseInt(skuQty[i + 1])));
        return new CreateOrderRequest(email, lines);
    }

    /**
     * 4. Serwis zna REGUŁY: produkt musi istnieć, stan musi wystarczyć, anulować można tylko NOWE. Łamanie
     * reguły = wyjątek domenowy z komunikatem w języku biznesu. Serwis NIE zna statusów HTTP.
     */
    static void serviceRules() {
        section("4. Serwis: reguły biznesowe i wyjątki domenowe");
        App app = new App();

        OrderEntity placed = app.service.placeOrder(order("ola@example.com", "ELE-004", "1", "DOM-002", "2"));
        show("złożone", OrderMapper.toResponse(placed));
        // WYNIK: złożone → OrderResponse[id=1, customer=ola@example.com, items=3, total=1557.00 zł, status=NOWE]
        show("stan ELE-004 po zakupie", app.stock.available("ELE-004"));
        // WYNIK: stan ELE-004 po zakupie → 3

        expectThrows("nieznany produkt", () -> app.service.placeOrder(order("ola@example.com", "XXX-999", "1")));
        // WYNIK: ✔ nieznany produkt → rzucono NotFoundException: nie ma produktu XXX-999
        expectThrows("za mało towaru", () -> app.service.placeOrder(order("ola@example.com", "DOM-001", "5")));
        // WYNIK: ✔ za mało towaru → rzucono BusinessRuleException: za mało towaru: Ekspres do kawy (jest 2)

        show("anulowane", OrderMapper.toResponse(app.service.cancel(placed.id())).status());
        // WYNIK: anulowane → ANULOWANE
        show("stan ELE-004 po anulowaniu", app.stock.available("ELE-004"));
        // WYNIK: stan ELE-004 po anulowaniu → 4
        expectThrows("ponowne anulowanie", () -> app.service.cancel(placed.id()));
        // WYNIK: ✔ ponowne anulowanie → rzucono BusinessRuleException: nie można anulować zamówienia w stanie ANULOWANE

        // PUŁAPKA: reguły w kontrolerze („if stock < qty return 409”) — wtedy drugi punkt wejścia (np. import CSV,
        // zadanie nocne) omija reguły. Reguła ma żyć w serwisie (albo w samej encji), żeby nie dało się jej obejść.
        // W Springu: @Service class OrderService { OrderService(OrderRepository r, ...) { ... } } — klasa jak tutaj,
        //            konstruktor wstrzykiwany automatycznie.
    }

    // =================================================================================================
    // 5. GRANICA TRANSAKCJI
    // =================================================================================================

    /**
     * 5. Zamówienie z dwiema pozycjami: pierwsza zmniejsza stan, druga się nie udaje. Bez transakcji pierwsza
     * zmiana ZOSTAJE (towar „znika” z magazynu). W transakcji wszystko albo nic — stan wraca do początku.
     */
    static void transactionBoundary() {
        section("5. Granica transakcji: wszystko albo nic (PRZED → PO)");

        CreateOrderRequest broken = order("ola@example.com", "KSI-003", "2", "DOM-001", "9");  // DOM-001: są 2 sztuki

        App noTx = new App();
        expectThrows("PRZED: bez transakcji", () -> noTx.service.placeOrderWithoutTransaction(broken));
        // WYNIK: ✔ PRZED: bez transakcji → rzucono BusinessRuleException: za mało towaru: Ekspres do kawy (jest 2)
        show("PRZED: stan KSI-003 (było 3)", noTx.stock.available("KSI-003"));
        // WYNIK: PRZED: stan KSI-003 (było 3) → 1

        App withTx = new App();
        expectThrows("PO: w transakcji", () -> withTx.service.placeOrder(broken));
        // WYNIK: ✔ PO: w transakcji → rzucono BusinessRuleException: za mało towaru: Ekspres do kawy (jest 2)
        show("PO: stan KSI-003 (było 3)", withTx.stock.available("KSI-003"));
        // WYNIK: PO: stan KSI-003 (było 3) → 3
        withTx.printTx();
        // WYNIK: transakcje (liczba elementów: 2):
        // WYNIK: • BEGIN placeOrder
        // WYNIK: • ROLLBACK placeOrder (za mało towaru: Ekspres do kawy (jest 2))

        // DOBRA PRAKTYKA: granica transakcji = jedna metoda serwisu = jeden przypadek użycia (use case).
        // Kontroler nie otwiera transakcji (nie zna reguł), repozytorium też nie (nie wie, co jest „całością”).
        // PUŁAPKA: w transakcji nie rób wolnych rzeczy z zewnątrz (wysyłka e-maila, wywołanie HTTP) — trzymają
        // połączenie z bazą i nie da się ich „wycofać”. Wyślij e-mail po zatwierdzeniu.
        // W Springu: @Transactional na publicznej metodzie serwisu (działa przez proxy — patrz pułapka
        //            samowywołania w Spring01); @Transactional(readOnly = true) dla odczytów; wysyłka po
        //            zatwierdzeniu: @TransactionalEventListener.
    }

    // =================================================================================================
    // 6. WALIDACJA NA BRZEGU (mini Bean Validation)
    // =================================================================================================

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.RECORD_COMPONENT) @interface NotBlank { }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.RECORD_COMPONENT) @interface NotEmpty { }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.RECORD_COMPONENT) @interface Positive { }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.RECORD_COMPONENT) @interface Email { }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.RECORD_COMPONENT) @interface Valid { }

    /** Czyta adnotacje z komponentów rekordu (kolejność = kolejność w nagłówku rekordu) i zbiera WSZYSTKIE błędy. */
    static final class MiniValidator {
        List<String> validate(Record dto) {
            List<String> errors = new ArrayList<>();
            check(dto, "", errors);
            return errors;
        }

        private void check(Record dto, String prefix, List<String> errors) {
            for (RecordComponent rc : dto.getClass().getRecordComponents()) {   // record component = składnik rekordu
                Object value;
                try {
                    value = rc.getAccessor().invoke(dto);                       // accessor = metoda dostępowa
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
                String path = prefix + rc.getName();
                if (rc.isAnnotationPresent(NotBlank.class) && (value == null || value.toString().isBlank())) {
                    errors.add(path + ": nie może być puste");
                } else if (rc.isAnnotationPresent(Email.class) && !value.toString().matches("[^@\\s]+@[^@\\s]+\\.[a-z]+")) {
                    errors.add(path + ": niepoprawny e-mail");
                }
                if (rc.isAnnotationPresent(NotEmpty.class) && (value == null || ((Collection<?>) value).isEmpty())) {
                    errors.add(path + ": lista nie może być pusta");
                }
                if (rc.isAnnotationPresent(Positive.class) && ((Integer) value) <= 0) {
                    errors.add(path + ": musi być większe od 0");
                }
                if (rc.isAnnotationPresent(Valid.class) && value instanceof List<?> list) {
                    for (int i = 0; i < list.size(); i++) {
                        check((Record) list.get(i), path + "[" + i + "].", errors);
                    }
                }
            }
        }
    }

    /**
     * 6. Dane z zewnątrz sprawdzamy NA BRZEGU (w kontrolerze, na DTO), zanim dotkną serwisu: format e-maila,
     * puste pola, liczby dodatnie. To walidacja „kształtu” danych. Reguły biznesowe (czy jest towar) to serwis.
     */
    static void validationAtEdge() {
        section("6. Walidacja na brzegu: adnotacje na DTO, wszystkie błędy naraz");
        MiniValidator validator = new MiniValidator();

        show("poprawne", validator.validate(order("ola@example.com", "ELE-001", "1")));
        // WYNIK: poprawne → []
        showEach("błędne", validator.validate(order("ola-at-example", "", "0", "ELE-001", "-2")));
        // WYNIK: błędne (liczba elementów: 4):
        // WYNIK: • customerEmail: niepoprawny e-mail
        // WYNIK: • lines[0].sku: nie może być puste
        // WYNIK: • lines[0].quantity: musi być większe od 0
        // WYNIK: • lines[1].quantity: musi być większe od 0
        show("pusta lista", validator.validate(new CreateOrderRequest(" ", List.of())));
        // WYNIK: pusta lista → [customerEmail: nie może być puste, lines: lista nie może być pusta]
        // Przy pustym e-mailu nie sprawdzamy już formatu (else if) — jeden błąd na pole jest czytelniejszy.

        // PUŁAPKA: walidacja przerywana na pierwszym błędzie zmusza użytkownika do poprawiania formularza
        // pole po polu. Zbieraj WSZYSTKIE błędy i oddaj je razem (status 400).
        // DOBRA PRAKTYKA: walidacja kształtu na brzegu (DTO), reguły biznesowe w serwisie — nie mieszaj.
        // W Springu: record CreateOrderRequest(@NotBlank @Email String customerEmail, @NotEmpty @Valid List<LineRequest> lines)
        //            z jakarta.validation.constraints (zależność spring-boot-starter-validation), a w kontrolerze
        //            create(@Valid @RequestBody CreateOrderRequest r) → błędy = MethodArgumentNotValidException → 400.
    }

    // =================================================================================================
    // 7. KONTROLER I TŁUMACZENIE WYJĄTKÓW NA STATUSY
    // =================================================================================================

    /** Jedno miejsce, które zamienia wyjątki na odpowiedzi (jak @RestControllerAdvice). */
    static final class ErrorTranslator {
        ApiResponse translate(RuntimeException e) {
            if (e instanceof ValidationException v) return new ApiResponse(400, new ErrorBody("VALIDATION", v.errors));
            if (e instanceof NotFoundException) return new ApiResponse(404, new ErrorBody("NOT_FOUND", List.of(e.getMessage())));
            if (e instanceof BusinessRuleException) return new ApiResponse(409, new ErrorBody("CONFLICT", List.of(e.getMessage())));
            return new ApiResponse(500, new ErrorBody("INTERNAL", List.of("błąd serwera")));   // szczegóły tylko do logu!
        }
    }

    static class OrderController {
        private final OrderService service;
        private final MiniValidator validator;
        private final ErrorTranslator errors;

        OrderController(OrderService service, MiniValidator validator, ErrorTranslator errors) {
            this.service = service;
            this.validator = validator;
            this.errors = errors;
        }

        /** POST /zamowienia */
        ApiResponse create(CreateOrderRequest request) {
            try {
                List<String> problems = validator.validate(request);
                if (!problems.isEmpty()) throw new ValidationException(problems);
                return new ApiResponse(201, OrderMapper.toResponse(service.placeOrder(request)));
            } catch (RuntimeException e) {
                return errors.translate(e);
            }
        }

        /** GET /zamowienia/{id} */
        ApiResponse get(long id) {
            try {
                return new ApiResponse(200, OrderMapper.toResponse(service.get(id)));
            } catch (RuntimeException e) {
                return errors.translate(e);
            }
        }
    }

    /**
     * 7. Kontroler: przyjmij DTO, zwaliduj, zawołaj serwis, zamień wynik na DTO odpowiedzi. Wyjątki każdej
     * warstwy tłumaczymy w JEDNYM miejscu: walidacja → 400, „nie ma” → 404, reguła biznesowa → 409, reszta → 500.
     */
    static void controllerAndErrors() {
        section("7. Kontroler i tłumaczenie wyjątków na statusy HTTP");
        App app = new App();

        show("POST poprawny", app.controller.create(order("ola@example.com", "SPO-002", "4")));
        // WYNIK: POST poprawny → ApiResponse[status=201, body=OrderResponse[id=1, customer=ola@example.com, items=4, total=29.96 zł, status=NOWE]]
        show("POST zły e-mail", app.controller.create(order("ola", "SPO-002", "4")));
        // WYNIK: POST zły e-mail → ApiResponse[status=400, body=ErrorBody[error=VALIDATION, details=[customerEmail: niepoprawny e-mail]]]
        show("POST za dużo", app.controller.create(order("ola@example.com", "ODZ-001", "50")));
        // WYNIK: POST za dużo → ApiResponse[status=409, body=ErrorBody[error=CONFLICT, details=[za mało towaru: Kurtka zimowa (jest 12)]]]
        show("GET 1", app.controller.get(1).status());
        // WYNIK: GET 1 → 200
        show("GET 42", app.controller.get(42));
        // WYNIK: GET 42 → ApiResponse[status=404, body=ErrorBody[error=NOT_FOUND, details=[nie ma zamówienia 42]]]

        // PUŁAPKA: 500 z treścią wyjątku (stack trace, SQL) w odpowiedzi to wyciek informacji o systemie.
        // Klient dostaje ogólny komunikat, a szczegóły idą do logu serwera.
        // DOBRA PRAKTYKA: wyjątki domenowe nie znają HTTP; mapowanie wyjątek → status jest w jednym miejscu.
        // W Springu: @RestControllerAdvice class ApiErrors { @ExceptionHandler(NotFoundException.class)
        //            @ResponseStatus(HttpStatus.NOT_FOUND) ErrorBody notFound(NotFoundException e) { ... } };
        //            Spring 6 ma też gotowy format błędu ProblemDetail (RFC 7807, dziś zastąpiony przez RFC 9457).
    }

    // =================================================================================================
    // 8. PACKAGE-BY-LAYER A PACKAGE-BY-FEATURE
    // =================================================================================================

    /**
     * 8. Dwa sposoby układania pakietów. Po warstwach: wszystkie kontrolery razem, wszystkie serwisy razem.
     * Po funkcjonalnościach: wszystko o zamówieniach w jednym pakiecie. Przy większej aplikacji wygrywa
     * zwykle „po funkcjonalnościach”: zmiana jednej funkcji dotyka jednego pakietu, a klasy mogą być
     * pakietowo-prywatne (bez public) — mniej przypadkowych zależności.
     */
    static void packageLayout() {
        section("8. Pakiety: po warstwach (by layer) a po funkcjonalnościach (by feature)");

        Map<String, String> byLayer = new LinkedHashMap<>();
        byLayer.put("sklep.controller", "OrderController, ProductController");
        byLayer.put("sklep.service", "OrderService, ProductService");
        byLayer.put("sklep.repository", "OrderRepository, ProductRepository");
        showEach("po warstwach", byLayer);
        // WYNIK: po warstwach (liczba kluczy: 3):
        // WYNIK: • sklep.controller → OrderController, ProductController
        // WYNIK: • sklep.service → OrderService, ProductService
        // WYNIK: • sklep.repository → OrderRepository, ProductRepository

        Map<String, String> byFeature = new LinkedHashMap<>();
        byFeature.put("sklep.order", "OrderController, OrderService, OrderRepository, OrderEntity, dto/");
        byFeature.put("sklep.product", "ProductController, ProductService, ProductRepository");
        byFeature.put("sklep.common", "ErrorTranslator, konfiguracja");
        showEach("po funkcjonalnościach", byFeature);
        // WYNIK: po funkcjonalnościach (liczba kluczy: 3):
        // WYNIK: • sklep.order → OrderController, OrderService, OrderRepository, OrderEntity, dto/
        // WYNIK: • sklep.product → ProductController, ProductService, ProductRepository
        // WYNIK: • sklep.common → ErrorTranslator, konfiguracja

        // PUŁAPKA: w „po warstwach” wszystko musi być public (inne pakiety!), więc nic nie broni przed tym, by
        // ProductController sięgnął wprost do OrderRepository.
        // W Springu: oba układy działają, byle wszystko leżało POD pakietem klasy z @SpringBootApplication
        //            (stamtąd startuje skanowanie komponentów). Klasy poza nim nie zostaną znalezione.
    }

    // =================================================================================================
    // 9. TESTOWANIE WARSTW ATRAPAMI
    // =================================================================================================

    /**
     * 9. Każdą warstwę testujemy osobno, podmieniając warstwę niżej na atrapę (fake). Dzięki interfejsom
     * i wstrzykiwaniu przez konstruktor nie potrzeba do tego ani bazy, ani serwera, ani Springa.
     */
    static void testingWithFakes() {
        section("9. Testowanie warstw atrapami (fake)");

        // Serwis: katalog-atrapa z jednym produktem (lambda!) i świeży magazyn.
        Product pen = new Product("TST-1", "Długopis", helpers.model.Category.DOM, new BigDecimal("2.50"), 3);
        InMemoryStore store = new InMemoryStore();
        store.stock.put("TST-1", 3);
        OrderService service = new OrderService(new InMemoryOrderRepository(store), new InMemoryStockRepository(store),
                sku -> sku.equals("TST-1") ? Optional.of(pen) : Optional.empty(), new TxManager(store));
        Check.equal("serwis: suma zamówienia", new BigDecimal("7.50"),
                () -> service.placeOrder(order("t@example.com", "TST-1", "3")).total());
        Check.equal("serwis: stan po zakupie", 0, () -> store.stock.get("TST-1"));
        // WYNIK: ✔ OK    serwis: suma zamówienia
        // WYNIK: ✔ OK    serwis: stan po zakupie

        // Kontroler: serwis-atrapa, który zawsze rzuca regułę biznesową — sprawdzamy tylko tłumaczenie na 409.
        OrderService failing = new OrderService(null, null, null, null) {
            @Override OrderEntity placeOrder(CreateOrderRequest request) { throw new BusinessRuleException("test"); }
        };
        OrderController controller = new OrderController(failing, new MiniValidator(), new ErrorTranslator());
        Check.equal("kontroler: status 409", 409, () -> controller.create(order("t@example.com", "X", "1")).status());
        Check.equal("kontroler: walidacja przed serwisem", 400, () -> controller.create(order("", "X", "1")).status());
        // WYNIK: ✔ OK    kontroler: status 409
        // WYNIK: ✔ OK    kontroler: walidacja przed serwisem
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD

        // DOBRA PRAKTYKA: test serwisu = atrapy repozytoriów (szybki, bez bazy); test repozytorium = prawdziwa
        // baza testowa (H2); test kontrolera = atrapa serwisu. Kilka testów „od końca do końca” na wierzch.
        // W Springu: test serwisu to zwykły JUnit + Mockito (t32_junit_mockito/JUnit04Mockito), bez Springa;
        //            @WebMvcTest(OrderController.class) + @MockBean (od Boot 3.4: @MockitoBean) testuje sam
        //            kontroler, @DataJpaTest — same repozytoria na bazie testowej.
    }

    // =================================================================================================
    // 10. GDZIE STOJĄ ADNOTACJE SPRINGA
    // =================================================================================================

    /**
     * 10. Mapa: klasa z tej lekcji → adnotacja, którą dostałaby w Springu. Wszystkie stereotypy (@Service,
     * @Repository, @RestController) to odmiany @Component — kontener tworzy z nich beany tak samo (Spring01).
     */
    static void whereAnnotationsGo() {
        section("10. Gdzie stoją adnotacje Springa");

        Map<String, String> map = new LinkedHashMap<>();
        map.put("OrderController", "@RestController + @RequestMapping(\"/zamowienia\")");
        map.put("create(...)", "@PostMapping + @Valid @RequestBody");
        map.put("OrderService", "@Service");
        map.put("placeOrder / cancel", "@Transactional");
        map.put("InMemoryOrderRepository", "@Repository (albo interfejs Spring Data)");
        map.put("ErrorTranslator", "@RestControllerAdvice + @ExceptionHandler");
        map.put("MiniValidator", "Bean Validation (jakarta.validation)");
        map.put("App (składanie)", "kontener + @SpringBootApplication");
        showEach("klasa → Spring", map);
        // WYNIK: klasa → Spring (liczba kluczy: 8):
        // WYNIK: • OrderController → @RestController + @RequestMapping("/zamowienia")
        // WYNIK: • create(...) → @PostMapping + @Valid @RequestBody
        // WYNIK: • OrderService → @Service
        // WYNIK: • placeOrder / cancel → @Transactional
        // WYNIK: • InMemoryOrderRepository → @Repository (albo interfejs Spring Data)
        // WYNIK: • ErrorTranslator → @RestControllerAdvice + @ExceptionHandler
        // WYNIK: • MiniValidator → Bean Validation (jakarta.validation)
        // WYNIK: • App (składanie) → kontener + @SpringBootApplication

        // PUŁAPKA: @Transactional na kontrolerze (transakcja trwa podczas zamiany na JSON i obsługi HTTP) albo
        // na prywatnej metodzie (proxy jej nie widzi). Miejsce: publiczne metody serwisu.
        // W Springu: @RestController = @Controller + @ResponseBody (wynik metody → JSON w ciele odpowiedzi).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Kontroler: DTO, walidacja kształtu, wywołanie serwisu, mapowanie na odpowiedź. Bez reguł biznesowych.
     *   • Serwis: reguły biznesowe + granica transakcji (jeden przypadek użycia = jedna metoda).
     *   • Repozytorium: zapis/odczyt encji, interfejs w serwisie → implementację można podmienić.
     *   • Zależności tylko w dół; encja nie wychodzi poza serwis/kontroler — na zewnątrz DTO (rekordy).
     *   • Wyjątek domenowy (NotFound, BusinessRule) → status (404, 409) w JEDNYM miejscu; walidacja → 400; reszta → 500
     *     bez szczegółów.
     *   • Transakcja = wszystko albo nic; bez niej częściowe zmiany zostają.
     *   • Pakiety: by feature skaluje się lepiej; wszystko pod pakietem @SpringBootApplication.
     *   • Testy: każda warstwa osobno z atrapą warstwy niżej.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego serwis zależy od interfejsu OrderRepository, a nie od InMemoryOrderRepository?
     *   2. Czym różni się walidacja „kształtu” danych od reguły biznesowej? Podaj po przykładzie i warstwę.
     *   3. Co wypisze (App z sekcji 4–5, DOM-001 ma 2 sztuki, ELE-003 ma 25)?
     *        App app = new App();
     *        System.out.println(app.controller.create(order("a@b.pl", "ELE-003", "1", "DOM-001", "3")).status()
     *            + " " + app.stock.available("ELE-003"));
     *   4. ZNAJDŹ BŁĄD:
     *        ApiResponse get(long id) { return new ApiResponse(200, repository.findById(id).get()); }   // w kontrolerze
     *   5. ZNAJDŹ BŁĄD: serwis rzuca  new ResponseStatusException(HttpStatus.CONFLICT, "brak towaru")  w regule
     *      biznesowej, a ta sama metoda jest wołana też przez nocny import CSV.
     *   6. Co wypisze:  System.out.println(new MiniValidator().validate(new LineRequest(" ", 0)));  ?
     *   7. Kiedy package-by-feature jest lepszy od package-by-layer i dlaczego?
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
        // WYNIK: PODSUMOWANIE: ✔ 9 OK, ✘ 0 BŁĄD
    }

    private static void runChecks(boolean ref) {
        String s = ref ? " (wzorzec)" : "";
        OrderEntity e = new OrderEntity(5, "jan@example.com",
                List.of(new OrderLineEntity("SPO-002", 10, new BigDecimal("7.49")), new OrderLineEntity("KSI-002", 1, new BigDecimal("129.00"))),
                "OPLACONE", "tajne");
        Check.equal("ćw. 1: encja → DTO" + s, new OrderResponse(5, "jan@example.com", 11, "203.90 zł", "OPLACONE"),
                () -> ref ? solution1(e) : exercise1(e));

        Check.equal("ćw. 2: walidacja → 400" + s, 400, () -> ref ? solution2(new ValidationException(List.of("x"))) : exercise2(new ValidationException(List.of("x"))));
        Check.equal("ćw. 2: brak → 404" + s, 404, () -> ref ? solution2(new NotFoundException("x")) : exercise2(new NotFoundException("x")));
        Check.equal("ćw. 2: reguła → 409" + s, 409, () -> ref ? solution2(new BusinessRuleException("x")) : exercise2(new BusinessRuleException("x")));
        Check.equal("ćw. 2: inny domenowy → 422" + s, 422, () -> ref ? solution2(new DomainException("x")) : exercise2(new DomainException("x")));
        Check.equal("ćw. 2: NPE → 500" + s, 500, () -> ref ? solution2(new NullPointerException()) : exercise2(new NullPointerException()));

        List<LineRequest> small = List.of(new LineRequest("SPO-001", 2), new LineRequest("SPO-002", 3));
        List<LineRequest> big = List.of(new LineRequest("ELE-004", 1), new LineRequest("SPO-002", 1));
        Check.equal("ćw. 3: bez rabatu" + s, new BigDecimal("152.45"), () -> ref ? solution3(small, sampleCatalog()) : exercise3(small, sampleCatalog()));
        Check.equal("ćw. 3: z rabatem 10%" + s, new BigDecimal("1175.84"), () -> ref ? solution3(big, sampleCatalog()) : exercise3(big, sampleCatalog()));

        Map<String, List<String>> deps = new LinkedHashMap<>();
        deps.put("OrderController", List.of("OrderService"));
        deps.put("OrderService", List.of("OrderRepository", "MailService"));
        deps.put("MailService", List.of("OrderController"));
        deps.put("OrderRepository", List.of("OrderService"));
        Map<String, Integer> layers = Map.of("OrderController", 3, "OrderService", 2, "MailService", 2, "OrderRepository", 1);
        Check.equal("ćw. 4: naruszenia" + s, List.of("MailService → OrderController", "OrderRepository → OrderService"),
                () -> ref ? solution4(deps, layers) : exercise4(deps, layers));
    }

    /**
     * ĆWICZENIE 1 (łatwe): zamień encję na DTO odpowiedzi: id, e-mail klienta, liczba sztuk (suma quantity),
     * suma jako {@code "203.90 zł"} (toPlainString), status. Bez internalNote!
     * Podpowiedź: mapToInt(OrderLineEntity::quantity).sum(), e.total().
     */
    static OrderResponse exercise1(OrderEntity e) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): rozbuduj tłumacza wyjątków: ValidationException → 400, NotFoundException → 404,
     * BusinessRuleException → 409, każdy INNY DomainException → 422, wszystko inne → 500.
     * Podpowiedź: kolejność sprawdzeń ma znaczenie — podklasy przed nadklasą DomainException (instanceof).
     */
    static int exercise2(RuntimeException e) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ obliczenie wartości koszyka z double na BigDecimal (t15_numbers/Numbers01BigDecimal)
     * i dodaj regułę biznesową: suma ponad 1000 zł → rabat 10% (wynik w skali 2, HALF_UP).
     * <pre>{@code
     * double total = 0;                                   // PRZED (błędy zaokrągleń!)
     * for (LineRequest l : lines) total += catalog.find(l.sku()).get().price().doubleValue() * l.quantity();
     * }</pre>
     * Podpowiedź: stream().map(l -> cena.multiply(BigDecimal.valueOf(qty))).reduce(BigDecimal.ZERO, BigDecimal::add);
     * porównanie przez compareTo; rabat: multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP).
     */
    static BigDecimal exercise3(List<LineRequest> lines, ProductCatalog catalog) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): mamy mapę zależności „klasa → od czego zależy” i numery warstw (3 = kontroler,
     * 2 = serwis, 1 = repozytorium). Zwróć posortowaną listę naruszeń {@code "A → B"}, gdy B leży WYŻEJ niż A.
     * Zależności w obrębie tej samej warstwy są dozwolone.
     * Podpowiedź: dwie pętle (klucz, jego zależności), warunek layers.get(b) > layers.get(a), na końcu sort.
     */
    static List<String> exercise4(Map<String, List<String>> deps, Map<String, Integer> layers) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static OrderResponse solution1(OrderEntity e) {
        int items = e.lines().stream().mapToInt(OrderLineEntity::quantity).sum();
        return new OrderResponse(e.id(), e.customerEmail(), items, e.total().toPlainString() + " zł", e.status());
    }

    static int solution2(RuntimeException e) {
        if (e instanceof ValidationException) return 400;
        if (e instanceof NotFoundException) return 404;
        if (e instanceof BusinessRuleException) return 409;
        if (e instanceof DomainException) return 422;
        return 500;
    }

    private static final BigDecimal THRESHOLD = new BigDecimal("1000");    // threshold = próg
    private static final BigDecimal AFTER_DISCOUNT = new BigDecimal("0.90");

    static BigDecimal solution3(List<LineRequest> lines, ProductCatalog catalog) {
        BigDecimal total = lines.stream()
                .map(l -> catalog.find(l.sku()).orElseThrow().price().multiply(BigDecimal.valueOf(l.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(THRESHOLD) > 0) total = total.multiply(AFTER_DISCOUNT);
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    static List<String> solution4(Map<String, List<String>> deps, Map<String, Integer> layers) {
        List<String> result = new ArrayList<>();
        deps.forEach((a, list) -> list.forEach(b -> {
            if (layers.get(b) > layers.get(a)) result.add(a + " → " + b);
        }));
        result.sort(null);
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Żeby serwis nie wiedział, gdzie leżą dane: w teście podajesz atrapę, w produkcji JDBC/JPA —
     *      bez zmiany jednej linii serwisu. To też pilnuje kierunku zależności (serwis zna tylko kontrakt).
     *   2. Kształt: czy pole nie jest puste, czy e-mail ma @, czy liczba > 0 — da się sprawdzić bez bazy,
     *      na brzegu (kontroler, DTO, 400). Reguła biznesowa: czy jest towar, czy zamówienie można anulować —
     *      wymaga stanu systemu, więc serwis (409).
     *   3. „409 25”. Pierwsza pozycja zmniejszyła stan ELE-003 do 24, druga (3 sztuki DOM-001, a są 2) rzuciła
     *      BusinessRuleException → transakcja przywróciła migawkę (25), a tłumacz zamienił wyjątek na 409.
     *   4. Trzy błędy: kontroler omija serwis i sięga do repozytorium; .get() na pustym Optional rzuci
     *      NoSuchElementException (→ 500 zamiast 404); zwraca encję zamiast DTO (wyciek pól wewnętrznych).
     *   5. Serwis zna HTTP (zły kierunek zależności): nocny import CSV dostaje wyjątek HTTP, który nic nie znaczy
     *      poza kontrolerem. Serwis powinien rzucić wyjątek domenowy (BusinessRuleException), a dopiero
     *      @RestControllerAdvice zamienić go na 409.
     *   6. „[sku: nie może być puste, quantity: musi być większe od 0]” — validate przyjmuje dowolny rekord
     *      (Record), a błędy idą w kolejności składników rekordu.
     *   7. Gdy aplikacja ma wiele funkcjonalności i rośnie: zmiana jednej funkcji dotyka jednego pakietu, klasy
     *      pomocnicze mogą być pakietowo-prywatne, a granice modułów są widoczne (łatwiej potem wydzielić moduł).
     */
    // </editor-fold>
}
