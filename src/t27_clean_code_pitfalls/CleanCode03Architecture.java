package t27_clean_code_pitfalls;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Architektura — od jednej klasy do aplikacji
 *        (architecture = architektura; layer = warstwa; port = port; adapter = adapter;
 *         domain = dziedzina (domena); DDD, Domain-Driven Design = projektowanie sterowane dziedziną)
 *
 * W SKRÓCIE:
 *   Gdy program rośnie, „gdzie co położyć” staje się ważniejsze niż pojedyncza metoda. Architektura to zestaw
 *   decyzji: na jakie części dzielimy kod i KTO może zależeć od KOGO. Poznasz warstwy, architekturę heksagonalną
 *   (porty i adaptery) zbudowaną w miniaturze w jednym pliku oraz podstawowe pojęcia DDD.
 *   Uczciwie: dla prostej aplikacji typu CRUD większość tych konstrukcji to przesada.
 *
 * ANALOGIA:
 *   Restauracja. Kuchnia (dziedzina) gotuje według przepisów i nie obchodzi jej, czy zamówienie przyszło od kelnera,
 *   przez telefon czy z aplikacji. Kelner i telefon to „adaptery wejściowe”. Kuchnia potrzebuje dostaw, ale nie
 *   zależy od konkretnej hurtowni — ma „formularz zamówienia towaru” (port wyjściowy), a hurtownię (adapter) można
 *   zmienić bez przepisywania przepisów.
 *
 * JAK TO DZIAŁA:
 *   Warstwy (layered architecture = architektura warstwowa), np. w książce Erica Evansa (2003):
 *     prezentacja (UI) → aplikacja (przypadki użycia) → dziedzina (reguły biznesowe) → infrastruktura (baza, e-mail)
 *   Klasycznie każda warstwa zależy od warstw POD nią, więc dziedzina zależy od infrastruktury.
 *   Po odwróceniu zależności (DIP z CleanCode02Solid) dziedzina definiuje interfejsy, a infrastruktura je implementuje:
 *
 *        adapter wejściowy ──▶ [ port wejściowy ▶ serwis aplikacji ▶ DZIEDZINA ▶ port wyjściowy ] ◀── adapter wyjściowy
 *        (konsola, REST)        (interfejs przypadku użycia)           (interfejs)       (baza, płatności, e-mail)
 *
 *   To jest architektura heksagonalna (hexagonal architecture, inaczej ports and adapters = porty i adaptery),
 *   opisana przez Alistaira Cockburna (ok. 2005). Sześciokąt to tylko rysunek — liczba boków nic nie znaczy.
 *   Reguła zależności: strzałki „import” wskazują DO ŚRODKA; środek nie wie nic o adapterach.
 *   Korzeń kompozycji (composition root) — jedno miejsce na brzegu programu, gdzie tworzymy adaptery i wstrzykujemy je.
 *
 * SŁÓWKA:
 *   layered architecture = architektura warstwowa; presentation = prezentacja; application = aplikacja;
 *   infrastructure = infrastruktura; dependency rule = reguła zależności; use case = przypadek użycia;
 *   inbound/outbound = wejściowy/wyjściowy; composition root = korzeń kompozycji; ubiquitous language = język
 *   wszechobecny; entity = encja; value object = obiekt wartości; aggregate = agregat; aggregate root = korzeń agregatu;
 *   invariant = niezmiennik; domain event = zdarzenie domenowe; bounded context = kontekst ograniczony;
 *   anemic domain model = anemiczny model dziedziny; rich domain model = bogaty model dziedziny; fake = atrapa
 *   działająca (uproszczona implementacja); package by feature = pakiety według funkcji.
 *
 * ZOBACZ TEŻ: t27_clean_code_pitfalls/CleanCode02Solid (DIP — fundament tej lekcji),
 *   t22_design_patterns/Patterns08DependencyInjection (wstrzykiwanie zależności),
 *   t22_design_patterns/Patterns11Adapter (adapter jako wzorzec), t25_testing/Testing02TestDoubles (atrapy),
 *   t34_toward_spring/Spring02Layers (warstwy w Springu)
 * </pre>
 */
public class CleanCode03Architecture {

    /** Stały zegar: czasy zdarzeń są identyczne przy każdym uruchomieniu. fixed = stały. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);

    public static void main(String[] args) {
        title("CleanCode03 — architektura: warstwy, porty i adaptery, DDD");

        bigBallOfMud();          // big ball of mud = wielka kula błota (kod bez struktury)
        layers();                // layers = warstwy
        packaging();             // packaging = pakietowanie (podział na pakiety)
        valueObjectsAndEntities(); // value objects and entities = obiekty wartości i encje
        aggregates();            // aggregates = agregaty
        anemicVsRich();          // anemic vs rich = anemiczny kontra bogaty
        domainEvents();          // domain events = zdarzenia domenowe
        hexagonal();             // hexagonal = heksagonalna
        useCaseTests();          // use case tests = testy przypadku użycia
        boundedContextsAndOverkill(); // bounded contexts and overkill = konteksty ograniczone i przesada
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PUNKT WYJŚCIA: WSZYSTKO W JEDNEJ KLASIE
    // =================================================================================================

    /** „Baza danych” wersji PRZED — mutowalny stan statyczny to część problemu (database = baza danych). */
    private static final Map<String, String> DATABASE = new TreeMap<>();

    /**
     * 1. Jedna metoda robi wszystko: parsuje tekst, zna ceny, liczy, „zapisuje” i „wysyła e-mail”.
     * Działa — i właśnie dlatego takie kody żyją latami. Problem pojawia się przy zmianie i przy testach.
     */
    static void bigBallOfMud() {
        section("1. Punkt wyjścia: wszystko w jednej klasie");

        // processOrderAllInOne = przetwórz zamówienie, wszystko w jednym
        show("odpowiedź", processOrderAllInOne("ZAM-001;jan@example.com;KSI-001x2,ELE-003x1"));
        // WYNIK: [e-mail do jan@example.com] Dziękujemy! Zamówienie ZAM-001 na 507.90 zł
        // WYNIK: odpowiedź → OK ZAM-001 507.90 zł
        show("zawartość „bazy”", DATABASE);
        // WYNIK: zawartość „bazy” → {ZAM-001=jan@example.com|507.90}

        // PUŁAPKA: ta metoda ma co najmniej 5 powodów do zmiany (format wejścia, cennik, reguły, zapis, powiadomienia).
        //   Nie da się jej przetestować bez „wysłania e-maila” i zapisu do „bazy”, a zmiana bazy na prawdziwą
        //   oznacza grzebanie w środku reguł biznesowych. To łamie SRP i DIP z CleanCode02Solid.
        note("reguła biznesowa (suma) jest zakopana między parsowaniem a zapisem");
        // WYNIK: ℹ reguła biznesowa (suma) jest zakopana między parsowaniem a zapisem
    }

    static String processOrderAllInOne(String line) {
        String[] parts = line.split(";");                       // split = podziel
        BigDecimal total = BigDecimal.ZERO;
        for (String item : parts[2].split(",")) {
            String[] skuAndQty = item.split("x");
            BigDecimal price = SampleData.productBySku(skuAndQty[0]).price();
            total = total.add(price.multiply(BigDecimal.valueOf(Integer.parseInt(skuAndQty[1]))));
        }
        DATABASE.put(parts[0], parts[1] + "|" + total);          // „zapis do bazy”
        System.out.println("[e-mail do " + parts[1] + "] Dziękujemy! Zamówienie " + parts[0] + " na " + total + " zł");
        return "OK " + parts[0] + " " + total + " zł";
    }

    // =================================================================================================
    // 2. WARSTWY I REGUŁA ZALEŻNOŚCI
    // =================================================================================================

    /** Cztery klasyczne warstwy (kolejność od góry do dołu). */
    enum Layer { PRESENTATION, APPLICATION, DOMAIN, INFRASTRUCTURE }

    /** Klasyczne warstwy (wersja „luźna”): wolno zależeć od dowolnej warstwy NIŻEJ. classicAllows = klasycznie wolno. */
    static boolean classicAllows(Layer from, Layer to) {
        return from.ordinal() < to.ordinal();
    }

    /** Po odwróceniu zależności: wszystko wskazuje na dziedzinę; dziedzina nie zależy od niczego. invertedAllows = po odwróceniu wolno. */
    static boolean invertedAllows(Layer from, Layer to) {
        switch (from) {                                          // klasyczny switch — działa w każdej Javie
            case DOMAIN:         return false;
            case APPLICATION:    return to == Layer.DOMAIN;
            case PRESENTATION:
            case INFRASTRUCTURE: return to == Layer.APPLICATION || to == Layer.DOMAIN;
            default:             throw new IllegalArgumentException("Nieznana warstwa: " + from);
        }
    }

    /**
     * 2. Warstwy: prezentacja (UI, REST), aplikacja (przypadki użycia — koordynacja, transakcje), dziedzina (reguły),
     * infrastruktura (baza, pliki, e-mail, zewnętrzne API). Najważniejsza jest REGUŁA ZALEŻNOŚCI: kto kogo importuje.
     */
    static void layers() {
        section("2. Warstwy i reguła zależności");

        Layer[][] pairs = {
                {Layer.PRESENTATION, Layer.APPLICATION},
                {Layer.APPLICATION, Layer.DOMAIN},
                {Layer.DOMAIN, Layer.INFRASTRUCTURE},
                {Layer.INFRASTRUCTURE, Layer.DOMAIN},
        };
        for (Layer[] p : pairs) {
            System.out.printf("%-14s → %-14s klasycznie: %-3s po odwróceniu: %s%n", p[0], p[1],
                    yesNo(classicAllows(p[0], p[1])), yesNo(invertedAllows(p[0], p[1])));
        }
        // WYNIK: PRESENTATION   → APPLICATION    klasycznie: TAK po odwróceniu: TAK
        // WYNIK: APPLICATION    → DOMAIN         klasycznie: TAK po odwróceniu: TAK
        // WYNIK: DOMAIN         → INFRASTRUCTURE klasycznie: TAK po odwróceniu: NIE
        // WYNIK: INFRASTRUCTURE → DOMAIN         klasycznie: NIE po odwróceniu: TAK

        // Kluczowa różnica to dwa ostatnie wiersze. Klasycznie dziedzina woła np. klasę DAO z SQL-em, więc test reguły
        // „rabat 10% dla VIP” wymaga bazy. Po odwróceniu dziedzina mówi: „potrzebuję czegoś, co umie save(order)”
        // (interfejs), a klasa z SQL-em w infrastrukturze ten interfejs implementuje.

        // PUŁAPKA: „mamy pakiety controller/service/repository, więc mamy architekturę” — nie. Jeśli klasa dziedziny
        //   importuje klasę z pakietu infrastruktury (albo adnotacje bazy danych), reguła zależności jest złamana,
        //   niezależnie od nazw pakietów.
        // DOBRA PRAKTYKA: regułę zależności da się sprawdzać automatycznie testem (np. biblioteka ArchUnit) —
        //   wtedy nie zależy ona od pamięci zespołu. Prostą wersję takiego sprawdzania napiszesz w ćwiczeniu 3.
    }

    static String yesNo(boolean b) {
        return b ? "TAK" : "NIE";
    }

    // =================================================================================================
    // 3. PAKIETY: WEDŁUG WARSTW CZY WEDŁUG FUNKCJI
    // =================================================================================================

    /**
     * 3. Ten sam kod można pociąć na pakiety na dwa sposoby. Pakiety według warstw (package by layer) grupują
     * „wszystkie kontrolery”, „wszystkie serwisy”. Pakiety według funkcji (package by feature) grupują wszystko,
     * co dotyczy jednej funkcji biznesowej.
     */
    static void packaging() {
        section("3. Pakiety: według warstw czy według funkcji");

        showEach("według warstw", List.of(
                "controller/ OrderController, InvoiceController",
                "service/    OrderService, InvoiceService",
                "repository/ OrderRepository, InvoiceRepository"));
        // WYNIK: według warstw (liczba elementów: 3):
        // WYNIK: • controller/ OrderController, InvoiceController
        // WYNIK: • service/    OrderService, InvoiceService
        // WYNIK: • repository/ OrderRepository, InvoiceRepository

        showEach("według funkcji", List.of(
                "order/   OrderController, OrderService, OrderRepository, Order",
                "invoice/ InvoiceController, InvoiceService, InvoiceRepository, Invoice"));
        // WYNIK: według funkcji (liczba elementów: 2):
        // WYNIK: • order/   OrderController, OrderService, OrderRepository, Order
        // WYNIK: • invoice/ InvoiceController, InvoiceService, InvoiceRepository, Invoice

        // Według warstw: łatwo zacząć, ale zmiana jednej funkcji dotyka 3 pakietów, a wszystkie klasy muszą być
        //   public (bo żyją w różnych pakietach) — każdy może wołać każdego.
        // Według funkcji: zmiana „zamówień” zostaje w pakiecie order/, a klasy pomocnicze mogą mieć dostęp
        //   pakietowy (bez public) — kompilator pilnuje granic. Wewnątrz funkcji nadal możesz mieć podpakiety warstw.
        // DOBRA PRAKTYKA: w większych projektach zacznij od funkcji na najwyższym poziomie, a warstwy trzymaj w środku.
        //   Struktura pakietów powinna „krzyczeć”, CO robi aplikacja, a nie jakiego frameworka używa.
        note("w tym pliku wszystko jest klasami zagnieżdżonymi — granice pokazują komentarze i interfejsy");
        // WYNIK: ℹ w tym pliku wszystko jest klasami zagnieżdżonymi — granice pokazują komentarze i interfejsy
    }

    // =================================================================================================
    // 4. DDD: JĘZYK WSZECHOBECNY, ENCJE I OBIEKTY WARTOŚCI
    // =================================================================================================
    // ---------------------------- DZIEDZINA (czysta Java, zero frameworków) ----------------------------

    /** Wyjątek naruszenia reguły biznesowej (domain exception = wyjątek dziedzinowy). */
    static final class DomainException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        DomainException(String message) {
            super(message);
        }
    }

    /**
     * Obiekt wartości: kwota w złotych. Równość po WARTOŚCI, niezmienny, sam pilnuje poprawności.
     * Money = pieniądze; amount = kwota; of = z (fabryka); plus = dodaj; times = razy.
     */
    record Money(BigDecimal amount) {
        static final Money ZERO = new Money(BigDecimal.ZERO);

        Money {
            Objects.requireNonNull(amount, "amount");             // requireNonNull = wymagaj nie-null
            if (amount.signum() < 0) {
                throw new DomainException("Kwota nie może być ujemna: " + amount);
            }
            amount = amount.setScale(2, RoundingMode.UNNECESSARY); // UNNECESSARY = zaokrąglanie zbędne (inaczej wyjątek)
        }

        static Money of(String amount) {
            return new Money(new BigDecimal(amount));
        }

        Money plus(Money other) {
            return new Money(amount.add(other.amount));
        }

        Money times(int quantity) {
            return new Money(amount.multiply(BigDecimal.valueOf(quantity)));
        }

        boolean isGreaterThan(Money other) {                     // is greater than = jest większa niż
            return amount.compareTo(other.amount) > 0;
        }

        @Override
        public String toString() {
            return amount + " zł";
        }
    }

    /** Obiekt wartości: identyfikator zamówienia z walidacją formatu. OrderId = identyfikator zamówienia. */
    record OrderId(String value) {
        OrderId {
            if (value == null || !value.matches("ZAM-\\d{3}")) {
                throw new DomainException("Zły numer zamówienia: " + value);
            }
        }
    }

    /** Obiekt wartości: pozycja zamówienia. OrderLine = pozycja zamówienia; unitPrice = cena jednostkowa. */
    record OrderLine(String sku, int quantity, Money unitPrice) {
        Money total() {
            return unitPrice.times(quantity);
        }
    }

    /**
     * 4. Język wszechobecny (ubiquitous language, Eric Evans): programiści i ludzie biznesu używają TYCH SAMYCH słów,
     * a te słowa są w kodzie. Jeśli dział sprzedaży mówi „zamówienie opłacone”, w kodzie jest {@code order.pay()},
     * a nie {@code setStatusFlag(2)}. Encja (entity) ma tożsamość — zostaje „tą samą” mimo zmian (zamówienie ZAM-001).
     * Obiekt wartości (value object) nie ma tożsamości — liczy się tylko wartość (10 zł to 10 zł).
     */
    static void valueObjectsAndEntities() {
        section("4. DDD: język wszechobecny, encje i obiekty wartości");

        Money a = Money.of("10.0");
        Money b = Money.of("10.00");
        show("Money 10.0 equals 10.00", a.equals(b));
        // WYNIK: Money 10.0 equals 10.00 → true
        show("BigDecimal 10.0 equals 10.00", new BigDecimal("10.0").equals(new BigDecimal("10.00")));
        // WYNIK: BigDecimal 10.0 equals 10.00 → false
        // Rekord porównuje pola przez equals, a BigDecimal.equals uwzględnia skalę. Dlatego konstruktor kompaktowy
        // Money ujednolica skalę do 2 — obiekt wartości sam dba o sensowną równość (t15_numbers/Numbers01BigDecimal).

        expectThrows("kwota ujemna", () -> Money.of("-1"));
        // WYNIK: ✔ kwota ujemna → rzucono DomainException: Kwota nie może być ujemna: -1
        expectThrows("ułamek grosza", () -> Money.of("0.001"));
        // WYNIK: ✔ ułamek grosza → rzucono ArithmeticException: Rounding necessary
        expectThrows("zły numer", () -> new OrderId("123"));
        // WYNIK: ✔ zły numer → rzucono DomainException: Zły numer zamówienia: 123

        // Encja: równość po identyfikatorze, a nie po wszystkich polach.
        Order first = Order.place(new OrderId("ZAM-001"), "jan@example.com", CLOCK);
        Order sameIdOtherState = Order.place(new OrderId("ZAM-001"), "jan@example.com", CLOCK);
        sameIdOtherState.addLine("KSI-001", 1, Money.of("79.00"));
        show("encje z tym samym id są równe", first.equals(sameIdOtherState));
        // WYNIK: encje z tym samym id są równe → true

        // DOBRA PRAKTYKA: zamiast „gołych” String/BigDecimal używaj małych obiektów wartości (Money, OrderId, Email).
        //   Walidacja jest w jednym miejscu, a kompilator nie pozwoli pomylić kwoty z ilością czy id z e-mailem.
        // PUŁAPKA: rekord to świetny obiekt wartości, ale ZŁA encja — jego equals porównuje wszystkie pola, więc
        //   „to samo zamówienie po dodaniu pozycji” byłoby „innym” obiektem. Encje to zwykle klasy z equals po id.
    }

    // =================================================================================================
    // 5. AGREGAT I JEGO NIEZMIENNIKI
    // =================================================================================================

    /** Stan zamówienia. Status = stan; NEW = nowe; PAID = opłacone; CANCELLED = anulowane. */
    enum Status { NEW, PAID, CANCELLED }

    /** Zdarzenie domenowe: „coś ważnego już się stało” — nazwa w czasie przeszłym. describe = opisz. */
    sealed interface DomainEvent permits OrderPlaced, OrderPaid, OrderCancelled {
        OrderId orderId();

        Instant occurredAt();                                    // occurred at = kiedy zaszło

        String describe();
    }

    /** OrderPlaced = zamówienie złożone. */
    record OrderPlaced(OrderId orderId, Instant occurredAt) implements DomainEvent {
        public String describe() {
            return "Przyjęliśmy zamówienie " + orderId.value();
        }
    }

    /** OrderPaid = zamówienie opłacone. */
    record OrderPaid(OrderId orderId, Money total, Instant occurredAt) implements DomainEvent {
        public String describe() {
            return "Opłacono zamówienie " + orderId.value() + " na " + total;
        }
    }

    /** OrderCancelled = zamówienie anulowane; reason = powód. */
    record OrderCancelled(OrderId orderId, String reason, Instant occurredAt) implements DomainEvent {
        public String describe() {
            return "Anulowano zamówienie " + orderId.value() + " (" + reason + ")";
        }
    }

    /**
     * Agregat (aggregate) z korzeniem Order: grupa obiektów (zamówienie + pozycje), którą zmieniamy WYŁĄCZNIE przez
     * korzeń, więc korzeń może zawsze pilnować niezmienników. Bogaty model: dane i reguły w jednym miejscu.
     * place = złóż; addLine = dodaj pozycję; pay = opłać; cancel = anuluj; pullEvents = zabierz zdarzenia.
     */
    static final class Order {
        static final int MAX_LINES = 3;
        static final int MAX_QUANTITY = 100;

        private final OrderId id;
        private final String customerEmail;                     // customer e-mail = e-mail klienta
        private final Clock clock;
        private final List<OrderLine> lines = new ArrayList<>();
        private final List<DomainEvent> events = new ArrayList<>();
        private Status status = Status.NEW;

        private Order(OrderId id, String customerEmail, Clock clock) {
            this.id = Objects.requireNonNull(id);
            this.customerEmail = Objects.requireNonNull(customerEmail);
            this.clock = clock;
        }

        static Order place(OrderId id, String customerEmail, Clock clock) {
            Order order = new Order(id, customerEmail, clock);
            order.events.add(new OrderPlaced(id, clock.instant()));
            return order;
        }

        void addLine(String sku, int quantity, Money unitPrice) {
            requireStatus(Status.NEW, "dodać pozycji");
            for (int i = 0; i < lines.size(); i++) {
                OrderLine existing = lines.get(i);
                if (existing.sku().equals(sku)) {               // ten sam produkt → łączymy pozycje
                    int merged = existing.quantity() + quantity;
                    requireQuantity(merged);
                    lines.set(i, new OrderLine(sku, merged, existing.unitPrice()));
                    return;
                }
            }
            requireQuantity(quantity);
            if (lines.size() == MAX_LINES) {
                throw new DomainException("Zamówienie może mieć najwyżej " + MAX_LINES + " pozycje");
            }
            lines.add(new OrderLine(sku, quantity, unitPrice));
        }

        void pay() {
            requireStatus(Status.NEW, "opłacić");
            if (lines.isEmpty()) {
                throw new DomainException("Nie można opłacić pustego zamówienia");
            }
            status = Status.PAID;
            events.add(new OrderPaid(id, total(), clock.instant()));
        }

        void cancel(String reason) {
            requireStatus(Status.NEW, "anulować");
            status = Status.CANCELLED;
            events.add(new OrderCancelled(id, reason, clock.instant()));
        }

        Money total() {
            Money sum = Money.ZERO;
            for (OrderLine line : lines) {
                sum = sum.plus(line.total());
            }
            return sum;
        }

        List<OrderLine> lines() {
            return List.copyOf(lines);                           // kopia niezmienna — nikt nie ominie korzenia
        }

        List<DomainEvent> pullEvents() {
            List<DomainEvent> copy = List.copyOf(events);
            events.clear();
            return copy;
        }

        OrderId id() {
            return id;
        }

        String customerEmail() {
            return customerEmail;
        }

        Status status() {
            return status;
        }

        private void requireStatus(Status expected, String action) {
            if (status != expected) {
                throw new DomainException("Nie można " + action + " zamówienia w stanie " + status);
            }
        }

        private static void requireQuantity(int quantity) {
            if (quantity < 1 || quantity > MAX_QUANTITY) {
                throw new DomainException("Ilość musi być w zakresie 1–" + MAX_QUANTITY + ", jest: " + quantity);
            }
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Order other && id.equals(other.id); // encja: równość po identyfikatorze
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return id.value() + " [" + status + ", " + total() + ", pozycji: " + lines.size() + "]";
        }
    }

    /**
     * 5. Niezmiennik (invariant) to reguła, która MUSI być prawdziwa zawsze, np. „najwyżej 3 pozycje”,
     * „opłaconego zamówienia nie zmieniamy”, „ilość 1–100”. Agregat to granica, w której takie reguły pilnujemy.
     */
    static void aggregates() {
        section("5. Agregat i jego niezmienniki");

        Order order = Order.place(new OrderId("ZAM-002"), "maria@example.com", CLOCK);
        order.addLine("KSI-001", 2, Money.of("79.00"));
        order.addLine("ELE-003", 1, Money.of("349.90"));
        order.addLine("KSI-001", 1, Money.of("79.00"));          // ten sam SKU → połączenie
        show("zamówienie", order);
        // WYNIK: zamówienie → ZAM-002 [NEW, 586.90 zł, pozycji: 2]
        show("pozycje", order.lines());
        // WYNIK: pozycje → [OrderLine[sku=KSI-001, quantity=3, unitPrice=79.00 zł], OrderLine[sku=ELE-003, quantity=1, unitPrice=349.90 zł]]

        expectThrows("ilość 0", () -> order.addLine("DOM-001", 0, Money.of("1899.00")));
        // WYNIK: ✔ ilość 0 → rzucono DomainException: Ilość musi być w zakresie 1–100, jest: 0
        order.addLine("DOM-002", 1, Money.of("129.00"));
        expectThrows("czwarta pozycja", () -> order.addLine("SPO-002", 1, Money.of("7.49")));
        // WYNIK: ✔ czwarta pozycja → rzucono DomainException: Zamówienie może mieć najwyżej 3 pozycje
        expectThrows("ominięcie korzenia", () -> order.lines().add(new OrderLine("X", 1, Money.ZERO)));
        // WYNIK: ✔ ominięcie korzenia → rzucono UnsupportedOperationException: (brak komunikatu)

        order.pay();
        expectThrows("zmiana po opłaceniu", () -> order.addLine("SPO-002", 1, Money.of("7.49")));
        // WYNIK: ✔ zmiana po opłaceniu → rzucono DomainException: Nie można dodać pozycji zamówienia w stanie PAID
        show("po opłaceniu", order);
        // WYNIK: po opłaceniu → ZAM-002 [PAID, 715.90 zł, pozycji: 3]

        // DOBRA PRAKTYKA: reguły agregatów (pojęcie agregatu: Eric Evans; praktyczne reguły spisał Vaughn Vernon) — zmieniaj agregat tylko przez korzeń; w jednej
        //   transakcji zmieniaj jeden agregat; inne agregaty wskazuj przez ID (np. customerId), a nie przez referencję
        //   do obiektu; agregaty trzymaj małe. Repozytorium istnieje dla korzenia (OrderRepository), nie dla pozycji.
        // PUŁAPKA: getter zwracający wewnętrzną ArrayList (return lines;) otwiera tylne drzwi — ktoś doda pozycję
        //   z pominięciem reguł. Zwracaj List.copyOf (Java 10+) albo niezmienny widok.
    }

    // =================================================================================================
    // 6. MODEL ANEMICZNY VS BOGATY (PRZED/PO)
    // =================================================================================================

    /** PRZED: „worek na dane” — same gettery i settery, zero reguł. AnemicOrder = anemiczne zamówienie. */
    static final class AnemicOrder {
        private String status = "NEW";
        private final List<OrderLine> lines = new ArrayList<>();

        String getStatus() {                                     // get status = pobierz stan
            return status;
        }

        void setStatus(String status) {                          // set status = ustaw stan
            this.status = status;
        }

        List<OrderLine> getLines() {                             // get lines = pobierz pozycje
            return lines;
        }
    }

    /** PRZED: cała logika w serwisie, który „operuje na danych”. AnemicOrderService = serwis modelu anemicznego. */
    static final class AnemicOrderService {
        Money total(AnemicOrder order) {
            Money sum = Money.ZERO;
            for (OrderLine line : order.getLines()) {
                sum = sum.plus(line.total());
            }
            return sum;
        }

        void pay(AnemicOrder order) {
            order.setStatus("PAID");                              // a sprawdzenie pustego zamówienia? zapomniane
        }
    }

    /**
     * 6. Anemiczny model dziedziny (anemic domain model) — nazwa Martina Fowlera (2003) na antywzorzec: obiekty
     * dziedziny są tylko strukturami danych, a reguły leżą w serwisach. Każdy serwis może zapomnieć o regule.
     */
    static void anemicVsRich() {
        section("6. Model anemiczny vs bogaty (PRZED/PO)");

        AnemicOrderService service = new AnemicOrderService();
        AnemicOrder anemic = new AnemicOrder();
        anemic.getLines().add(new OrderLine("KSI-001", 2, Money.of("79.00")));
        service.pay(anemic);
        show("PRZED: opłacono kwotę", service.total(anemic));
        // WYNIK: PRZED: opłacono kwotę → 158.00 zł
        anemic.getLines().add(new OrderLine("DOM-001", 1, Money.of("1899.00"))); // nikt nie protestuje
        anemic.setStatus("NOWE?!");                                          // literówka w stanie — też przejdzie
        show("PRZED: suma po „opłaceniu”", service.total(anemic) + ", stan: " + anemic.getStatus());
        // WYNIK: PRZED: suma po „opłaceniu” → 2057.00 zł, stan: NOWE?!

        Order rich = Order.place(new OrderId("ZAM-003"), "adam@example.com", CLOCK);
        expectThrows("PO: opłacenie pustego", rich::pay);
        // WYNIK: ✔ PO: opłacenie pustego → rzucono DomainException: Nie można opłacić pustego zamówienia
        rich.addLine("KSI-001", 2, Money.of("79.00"));
        rich.pay();
        expectThrows("PO: dopisanie po opłaceniu", () -> rich.addLine("DOM-001", 1, Money.of("1899.00")));
        // WYNIK: ✔ PO: dopisanie po opłaceniu → rzucono DomainException: Nie można dodać pozycji zamówienia w stanie PAID
        show("PO: zamówienie", rich);
        // WYNIK: PO: zamówienie → ZAM-003 [PAID, 158.00 zł, pozycji: 1]

        // Zauważ: w modelu bogatym NIE MA setStatus. Stan zmienia się tylko przez czynności z języka biznesu
        //   (pay, cancel), a enum Status wyklucza literówki.
        // DOBRA PRAKTYKA: serwis aplikacji ma być „cienki”: pobierz agregat → wywołaj jego metodę → zapisz → ogłoś.
        //   Reguła „czy wolno” należy do agregatu. Serwis z 300 liniami if-ów i encje z samymi setterami to sygnał
        //   modelu anemicznego.
        // PUŁAPKA: model anemiczny nie jest „zakazany” — w prostym CRUD-zie (formularz → tabela) reguł prawie nie ma
        //   i bogaty model niczego nie daje. Problem zaczyna się, gdy reguł przybywa, a każda jest w innym serwisie.
    }

    // =================================================================================================
    // 7. ZDARZENIA DOMENOWE
    // =================================================================================================

    /**
     * 7. Zdarzenie domenowe (domain event) zapisuje fakt, który zaszedł w dziedzinie: „zamówienie opłacone”.
     * Agregat tylko je ZBIERA; publikuje je serwis aplikacji po udanym zapisie. Dzięki temu Order nie wie nic
     * o e-mailach, magazynie ani fakturach — każdy z nich może zareagować na zdarzenie po swojemu.
     */
    static void domainEvents() {
        section("7. Zdarzenia domenowe");

        Order order = Order.place(new OrderId("ZAM-004"), "zofia@example.com", CLOCK);
        order.addLine("ELE-004", 2, Money.of("1299.00"));
        order.pay();
        for (DomainEvent event : order.pullEvents()) {
            System.out.println(event.occurredAt() + " " + event.describe());
        }
        // WYNIK: 2026-03-01T10:00:00Z Przyjęliśmy zamówienie ZAM-004
        // WYNIK: 2026-03-01T10:00:00Z Opłacono zamówienie ZAM-004 na 2598.00 zł
        show("drugie pobranie", order.pullEvents());
        // WYNIK: drugie pobranie → []

        // pullEvents „opróżnia skrzynkę” — każde zdarzenie publikujemy raz. Czas pochodzi z Clock.fixed, więc wynik
        // jest powtarzalny (t17_datetime/DateTime01LocalDateTime).
        // DOBRA PRAKTYKA: nazywaj zdarzenia w czasie przeszłym (OrderPaid, nie PayOrder — to byłoby polecenie) i rób je
        //   niezmiennymi rekordami. sealed (Java 17+) pokazuje pełną listę możliwych zdarzeń w jednym miejscu.
        // PUŁAPKA: wysyłanie e-maila wprost z metody pay() wiąże dziedzinę z infrastrukturą i wysyła wiadomość, nawet
        //   gdy zapis do bazy się potem nie uda. Zdarzenia publikuj PO zapisie (w prawdziwych systemach często po
        //   zatwierdzeniu transakcji).
    }

    // =================================================================================================
    // 8. ARCHITEKTURA HEKSAGONALNA: PORTY I ADAPTERY + KORZEŃ KOMPOZYCJI
    // =================================================================================================
    // ---------------------------- PORT WEJŚCIOWY (to, co aplikacja oferuje) ----------------------------

    /** Dane wejściowe przypadku użycia. LineRequest = żądana pozycja; PlaceOrderCommand = polecenie złożenia zamówienia. */
    record LineRequest(String sku, int quantity) { }

    record PlaceOrderCommand(String orderId, String customerEmail, List<LineRequest> lines) { }

    /** Wynik przypadku użycia. OrderConfirmation = potwierdzenie zamówienia. */
    record OrderConfirmation(String orderId, Status status, Money total) { }

    /** Port wejściowy = interfejs przypadku użycia. PlaceOrderUseCase = przypadek użycia „złóż zamówienie”. */
    interface PlaceOrderUseCase {
        OrderConfirmation placeOrder(PlaceOrderCommand command);
    }

    // ---------------------------- PORTY WYJŚCIOWE (to, czego aplikacja potrzebuje) ----------------------------

    /** OrderRepository = repozytorium zamówień; save = zapisz; findById = znajdź po id. */
    interface OrderRepository {
        void save(Order order);

        Optional<Order> findById(OrderId id);
    }

    /** PriceCatalog = cennik; priceOf = cena produktu. */
    interface PriceCatalog {
        Optional<Money> priceOf(String sku);
    }

    /** PaymentGateway = bramka płatności; charge = obciąż (true = płatność przyjęta). */
    interface PaymentGateway {
        boolean charge(OrderId orderId, Money amount);
    }

    /** Notifier = powiadamiacz; send = wyślij. */
    interface Notifier {
        void send(String recipient, String message);
    }

    // ---------------------------- RDZEŃ APLIKACJI: serwis implementuje port wejściowy ----------------------------

    /** PlaceOrderService = serwis składania zamówień. Zna tylko interfejsy portów — żadnej konkretnej technologii. */
    static final class PlaceOrderService implements PlaceOrderUseCase {
        private final OrderRepository orders;
        private final PriceCatalog prices;
        private final PaymentGateway payments;
        private final Notifier notifier;
        private final Clock clock;

        PlaceOrderService(OrderRepository orders, PriceCatalog prices, PaymentGateway payments,
                          Notifier notifier, Clock clock) {
            this.orders = orders;
            this.prices = prices;
            this.payments = payments;
            this.notifier = notifier;
            this.clock = clock;
        }

        @Override
        public OrderConfirmation placeOrder(PlaceOrderCommand command) {
            OrderId id = new OrderId(command.orderId());
            if (orders.findById(id).isPresent()) {
                throw new DomainException("Zamówienie " + id.value() + " już istnieje");
            }
            Order order = Order.place(id, command.customerEmail(), clock);
            for (LineRequest request : command.lines()) {
                Money price = prices.priceOf(request.sku())
                        .orElseThrow(() -> new DomainException("Nieznany produkt: " + request.sku()));
                order.addLine(request.sku(), request.quantity(), price);
            }
            if (payments.charge(id, order.total())) {
                order.pay();
            } else {
                order.cancel("płatność odrzucona");
            }
            orders.save(order);                                   // najpierw zapis…
            for (DomainEvent event : order.pullEvents()) {        // …potem publikacja zdarzeń
                notifier.send(order.customerEmail(), event.describe());
            }
            return new OrderConfirmation(id.value(), order.status(), order.total());
        }
    }

    // ---------------------------- ADAPTERY WYJŚCIOWE (infrastruktura) ----------------------------

    /** Repozytorium w pamięci (in-memory = w pamięci). TreeMap — stała kolejność przy wypisywaniu. */
    static final class InMemoryOrderRepository implements OrderRepository {
        private final Map<String, Order> storage = new TreeMap<>(); // storage = magazyn danych

        @Override
        public void save(Order order) {
            storage.put(order.id().value(), order);
        }

        @Override
        public Optional<Order> findById(OrderId id) {
            return Optional.ofNullable(storage.get(id.value()));
        }

        List<Order> all() {                                      // all = wszystkie
            return List.copyOf(storage.values());
        }
    }

    /** Cennik oparty na SampleData — adapter do „cudzego” modelu Product (t22_design_patterns/Patterns11Adapter). */
    static final class SampleDataPriceCatalog implements PriceCatalog {
        @Override
        public Optional<Money> priceOf(String sku) {
            return SampleData.products().stream()
                    .filter(p -> p.sku().equals(sku))
                    .map(Product::price)
                    .map(Money::new)
                    .findFirst();
        }
    }

    /** Udawana bramka: przyjmuje kwoty do limitu i zapamiętuje obciążenia. FakePaymentGateway = udawana bramka płatności. */
    static final class FakePaymentGateway implements PaymentGateway {
        private final Money limit;
        private final List<String> charges = new ArrayList<>();   // charges = obciążenia

        FakePaymentGateway(Money limit) {
            this.limit = limit;
        }

        @Override
        public boolean charge(OrderId orderId, Money amount) {
            charges.add(orderId.value() + " " + amount);
            return !amount.isGreaterThan(limit);
        }

        List<String> charges() {
            return List.copyOf(charges);
        }
    }

    /** Powiadomienia na konsolę. ConsoleNotifier = powiadamiacz konsolowy. */
    static final class ConsoleNotifier implements Notifier {
        @Override
        public void send(String recipient, String message) {
            System.out.println("   ✉ do " + recipient + ": " + message);
        }
    }

    /** Powiadamiacz-szpieg do testów: zapamiętuje wiadomości. RecordingNotifier = powiadamiacz nagrywający. */
    static final class RecordingNotifier implements Notifier {
        private final List<String> sent = new ArrayList<>();      // sent = wysłane

        @Override
        public void send(String recipient, String message) {
            sent.add(recipient + ": " + message);
        }

        List<String> sent() {
            return List.copyOf(sent);
        }
    }

    // ---------------------------- ADAPTER WEJŚCIOWY (prezentacja) ----------------------------

    /**
     * Tekstowy „kontroler”: tłumaczy format zewnętrzny na polecenie i wynik na odpowiedź. W Springu tę rolę pełni
     * {@code @RestController}. TextOrderController = tekstowy kontroler zamówień; handle = obsłuż.
     */
    static final class TextOrderController {
        private final PlaceOrderUseCase placeOrder;               // zależy od PORTU, nie od klasy serwisu

        TextOrderController(PlaceOrderUseCase placeOrder) {
            this.placeOrder = placeOrder;
        }

        String handle(String line) {
            String[] parts = line.split(";");
            List<LineRequest> requests = new ArrayList<>();
            for (String item : parts[2].split(",")) {
                String[] skuAndQty = item.split("x");
                requests.add(new LineRequest(skuAndQty[0], Integer.parseInt(skuAndQty[1])));
            }
            try {
                OrderConfirmation c = placeOrder.placeOrder(new PlaceOrderCommand(parts[0], parts[1], requests));
                return c.status() + " " + c.orderId() + " " + c.total();
            } catch (DomainException e) {
                return "BŁĄD: " + e.getMessage();                 // wyjątek dziedziny → odpowiedź dla klienta
            }
        }
    }

    /**
     * 8. Składamy całość w korzeniu kompozycji (composition root). To JEDYNE miejsce, które zna konkretne klasy
     * adapterów i wie, co z czym połączyć. Reszta kodu zna tylko interfejsy portów.
     */
    static void hexagonal() {
        section("8. Architektura heksagonalna: porty i adaptery + korzeń kompozycji");

        // --- korzeń kompozycji: tworzenie adapterów i wstrzykiwanie przez konstruktory ---
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        PlaceOrderUseCase useCase = new PlaceOrderService(repository, new SampleDataPriceCatalog(),
                new FakePaymentGateway(Money.of("1000.00")), new ConsoleNotifier(), CLOCK);
        TextOrderController controller = new TextOrderController(useCase);

        show("odpowiedź", controller.handle("ZAM-001;jan@example.com;KSI-001x2,ELE-003x1"));
        // WYNIK: ✉ do jan@example.com: Przyjęliśmy zamówienie ZAM-001
        // WYNIK: ✉ do jan@example.com: Opłacono zamówienie ZAM-001 na 507.90 zł
        // WYNIK: odpowiedź → PAID ZAM-001 507.90 zł
        show("odpowiedź", controller.handle("ZAM-002;zofia@example.com;DOM-001x1"));
        // WYNIK: ✉ do zofia@example.com: Przyjęliśmy zamówienie ZAM-002
        // WYNIK: ✉ do zofia@example.com: Anulowano zamówienie ZAM-002 (płatność odrzucona)
        // WYNIK: odpowiedź → CANCELLED ZAM-002 1899.00 zł
        show("odpowiedź", controller.handle("ZAM-003;adam@example.com;NIE-ISTNIEJE-001x1"));
        // WYNIK: odpowiedź → BŁĄD: Nieznany produkt: NIE-ISTNIEJE-001
        show("odpowiedź", controller.handle("ZAM-001;ola@example.com;SPO-002x1"));
        // WYNIK: odpowiedź → BŁĄD: Zamówienie ZAM-001 już istnieje
        showEach("repozytorium", repository.all());
        // WYNIK: repozytorium (liczba elementów: 2):
        // WYNIK: • ZAM-001 [PAID, 507.90 zł, pozycji: 2]
        // WYNIK: • ZAM-002 [CANCELLED, 1899.00 zł, pozycji: 1]

        // Porównaj z sekcją 1: ta sama funkcja, ale każdą część da się wymienić osobno. Repozytorium w pamięci →
        //   JDBC (t29_jdbc_databases), konsola → e-mail, kontroler tekstowy → HTTP — serwis i Order się nie zmieniają.
        // DOBRA PRAKTYKA: porty definiuj w języku dziedziny i po stronie aplikacji (OrderRepository przyjmuje Order,
        //   nie wiersz tabeli). Adapter tłumaczy model dziedziny na format technologii — i z powrotem.
        // PUŁAPKA: korzeń kompozycji rozproszony po kodzie (new FakePaymentGateway() w środku serwisu, statyczne
        //   „singletony”) niszczy całą zaletę — znów nie da się podmienić zależności w teście.
        //   Wzorzec wstrzykiwania: t22_design_patterns/Patterns08DependencyInjection.
    }

    // =================================================================================================
    // 9. TESTY PRZYPADKU UŻYCIA Z ATRAPAMI
    // =================================================================================================

    /**
     * 9. Największa praktyczna korzyść portów: przypadek użycia testujemy w milisekundach, bez bazy, sieci i
     * frameworka. Atrapy: repozytorium w pamięci i bramka z limitem to fake (działająca, uproszczona implementacja),
     * RecordingNotifier to spy (szpieg — zapisuje wywołania do sprawdzenia), cennik z mapy to stub (zwraca gotowe dane).
     */
    static void useCaseTests() {
        section("9. Testy przypadku użycia z atrapami");

        // Dane testowe niezależne od SampleData — test opisuje własny mały świat.
        Map<String, Money> testPrices = new TreeMap<>(Map.of("A-1", Money.of("10.00"), "B-2", Money.of("600.00")));
        PriceCatalog stubCatalog = sku -> Optional.ofNullable(testPrices.get(sku)); // lambda jako stub portu

        // Test 1: płatność przyjęta → PAID, zapisane, dwa powiadomienia.
        InMemoryOrderRepository repo1 = new InMemoryOrderRepository();
        RecordingNotifier spy1 = new RecordingNotifier();
        PlaceOrderUseCase service1 = new PlaceOrderService(repo1, stubCatalog,
                new FakePaymentGateway(Money.of("100.00")), spy1, CLOCK);
        OrderConfirmation ok = service1.placeOrder(
                new PlaceOrderCommand("ZAM-101", "t@example.com", List.of(new LineRequest("A-1", 3))));
        Check.equal("przyjęta płatność → PAID 30.00", new OrderConfirmation("ZAM-101", Status.PAID, Money.of("30")), ok);
        Check.equal("zamówienie zapisane", Status.PAID,
                () -> repo1.findById(new OrderId("ZAM-101")).map(Order::status).orElse(null));
        Check.equal("dwa powiadomienia", List.of(
                "t@example.com: Przyjęliśmy zamówienie ZAM-101",
                "t@example.com: Opłacono zamówienie ZAM-101 na 30.00 zł"), spy1.sent());
        // WYNIK: ✔ OK    przyjęta płatność → PAID 30.00
        // WYNIK: ✔ OK    zamówienie zapisane
        // WYNIK: ✔ OK    dwa powiadomienia

        // Test 2: kwota ponad limit → CANCELLED, ale zamówienie i tak zapisane (ślad dla obsługi klienta).
        FakePaymentGateway gateway2 = new FakePaymentGateway(Money.of("100.00"));
        InMemoryOrderRepository repo2 = new InMemoryOrderRepository();
        PlaceOrderUseCase service2 = new PlaceOrderService(repo2, stubCatalog, gateway2, new RecordingNotifier(), CLOCK);
        Check.equal("odrzucona płatność → CANCELLED", Status.CANCELLED, () -> service2.placeOrder(
                new PlaceOrderCommand("ZAM-102", "t@example.com", List.of(new LineRequest("B-2", 1)))).status());
        Check.equal("bramkę obciążono raz", List.of("ZAM-102 600.00 zł"), gateway2.charges());
        // WYNIK: ✔ OK    odrzucona płatność → CANCELLED
        // WYNIK: ✔ OK    bramkę obciążono raz

        // Test 3: nieznany produkt → wyjątek, NIC nie zapisano i NIKOGO nie obciążono.
        FakePaymentGateway gateway3 = new FakePaymentGateway(Money.of("100.00"));
        InMemoryOrderRepository repo3 = new InMemoryOrderRepository();
        PlaceOrderUseCase service3 = new PlaceOrderService(repo3, stubCatalog, gateway3, new RecordingNotifier(), CLOCK);
        Check.throwsException("nieznany produkt → DomainException", DomainException.class, () -> service3.placeOrder(
                new PlaceOrderCommand("ZAM-103", "t@example.com", List.of(new LineRequest("ZZZ", 1)))));
        Check.equal("bez zapisu i bez obciążenia", "0/0", () -> repo3.all().size() + "/" + gateway3.charges().size());
        // WYNIK: ✔ OK    nieznany produkt → DomainException
        // WYNIK: ✔ OK    bez zapisu i bez obciążenia
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 7 OK, ✘ 0 BŁĄD

        // DOBRA PRAKTYKA: dziedzinę (Order, Money) testuj bezpośrednio — to czyste obiekty; przypadki użycia testuj
        //   z atrapami portów; adaptery (prawdziwa baza, HTTP) testuj osobno testami integracyjnymi.
        //   Więcej o atrapach: t25_testing/Testing02TestDoubles; Mockito: t32_junit_mockito/JUnit04Mockito.
        // PUŁAPKA: test, który sprawdza tylko „metoda save została wywołana”, niewiele mówi. Lepiej sprawdzić skutek
        //   (co jest w repozytorium, jakie wiadomości wysłano) — fake i spy to umożliwiają.
    }

    // =================================================================================================
    // 10. KONTEKSTY OGRANICZONE, KIEDY TO PRZESADA I JAK TO WYGLĄDA W SPRINGU
    // =================================================================================================

    /** Ten sam „produkt” w dwóch kontekstach — dwa RÓŻNE modele. CatalogProduct = produkt w katalogu. */
    record CatalogProduct(String sku, String name, Money price) { }

    /** WarehouseItem = towar w magazynie; shelf = regał (półka); quantity = ilość. */
    record WarehouseItem(String sku, String shelf, int quantity) { }

    /**
     * 10. Kontekst ograniczony (bounded context, Evans): granica, wewnątrz której słowo ma jedno, ścisłe znaczenie.
     * „Produkt” dla katalogu to nazwa, opis i cena; dla magazynu — regał, waga i stan. Zamiast jednej gigantycznej
     * klasy Product dla całej firmy, każdy kontekst ma własny model, a łączy je wspólny identyfikator (SKU) i zdarzenia.
     */
    static void boundedContextsAndOverkill() {
        section("10. Konteksty ograniczone, kiedy to przesada i jak to wygląda w Springu");

        show("katalog", new CatalogProduct("KSI-001", "Czysty kod", Money.of("79.00")));
        // WYNIK: katalog → CatalogProduct[sku=KSI-001, name=Czysty kod, price=79.00 zł]
        show("magazyn", new WarehouseItem("KSI-001", "R-12", 15));
        // WYNIK: magazyn → WarehouseItem[sku=KSI-001, shelf=R-12, quantity=15]

        showEach("Spring a porty i adaptery", List.of(
                "adapter wejściowy   → @RestController",
                "port wejściowy      → interfejs przypadku użycia (zwykła Java)",
                "serwis aplikacji    → klasa z @Service, często z @Transactional",
                "port wyjściowy      → interfejs w rdzeniu aplikacji (np. OrderRepository)",
                "adapter wyjściowy   → klasa z @Repository / @Component (JPA, JDBC, klient HTTP)",
                "korzeń kompozycji   → kontener IoC: skanowanie komponentów, @Configuration + @Bean",
                "zdarzenia domenowe  → ApplicationEventPublisher + @EventListener"));
        // WYNIK: Spring a porty i adaptery (liczba elementów: 7):
        // WYNIK: • adapter wejściowy   → @RestController
        // WYNIK: • port wejściowy      → interfejs przypadku użycia (zwykła Java)
        // WYNIK: • serwis aplikacji    → klasa z @Service, często z @Transactional
        // WYNIK: • port wyjściowy      → interfejs w rdzeniu aplikacji (np. OrderRepository)
        // WYNIK: • adapter wyjściowy   → klasa z @Repository / @Component (JPA, JDBC, klient HTTP)
        // WYNIK: • korzeń kompozycji   → kontener IoC: skanowanie komponentów, @Configuration + @Bean
        // WYNIK: • zdarzenia domenowe  → ApplicationEventPublisher + @EventListener

        // KIEDY TO PRZESADA (uczciwie):
        //   • aplikacja CRUD: formularz → walidacja → tabela, prawie bez reguł. Zwykłe warstwy controller → service →
        //     repository (nawet z encjami JPA w roli modelu) są prostsze i w zupełności wystarczą;
        //   • prototyp, skrypt, mały projekt jednej osoby — porty dla każdej klasy to ceremonia bez zysku (YAGNI);
        //   • interfejs z jedną implementacją „na zapas” tylko po to, by „było heksagonalnie”.
        // KIEDY SIĘ OPŁACA: dużo reguł biznesowych, które żyją latami; kilka kanałów wejścia (REST, kolejka, CLI);
        //   wymienne integracje (bramki płatności, dostawcy); potrzeba szybkich testów logiki bez infrastruktury.
        // PUŁAPKA: w Springu łatwo „przemycić” framework do dziedziny — encja z adnotacjami JPA jako model dziedziny
        //   albo interfejs portu dziedzicący po JpaRepository. To świadomy kompromis (mniej kodu, mocniejsze powiązanie),
        //   a nie błąd — ważne, żeby to była decyzja, a nie przypadek.
        // DOBRA PRAKTYKA: zaczynaj prosto (pakiety według funkcji, cienkie serwisy, obiekty wartości), a porty
        //   wprowadzaj tam, gdzie pojawia się realna potrzeba wymiany lub testowania. Spring w praktyce: kurs
        //   SpringLearning oraz t34_toward_spring/Spring02Layers.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Warstwy: prezentacja → aplikacja → dziedzina → infrastruktura. Klasycznie zależność w dół;
     *     po odwróceniu (DIP) wszystko wskazuje na dziedzinę, a dziedzina nie zależy od niczego.
     *   • Pakiety według funkcji (order/, invoice/) zwykle lepiej skalują niż według warstw (controller/, service/).
     *   • Heksagonalna = porty i adaptery (Alistair Cockburn). Port wejściowy = interfejs przypadku użycia;
     *     port wyjściowy = interfejs potrzeby (repozytorium, płatność, powiadomienie); adapter = implementacja
     *     dla konkretnej technologii. Korzeń kompozycji = jedno miejsce, gdzie łączymy wszystko.
     *   • DDD (Eric Evans): język wszechobecny; encja = tożsamość (equals po id); obiekt wartości = wartość
     *     (rekord, niezmienny, sam się waliduje); agregat = granica niezmienników, zmiany tylko przez korzeń;
     *     zdarzenie domenowe = fakt w czasie przeszłym, publikowany po zapisie; kontekst ograniczony = granica
     *     znaczenia słów.
     *   • Model anemiczny (dane + serwisy z regułami) vs bogaty (reguły w obiektach). Bogaty — gdy reguł jest dużo.
     *   • Getter kolekcji w agregacie → List.copyOf, nigdy wewnętrzna lista.
     *   • Testy: dziedzina bezpośrednio, przypadki użycia z atrapami portów, adaptery testami integracyjnymi.
     *   • CRUD z małą liczbą reguł → zwykłe warstwy wystarczą; heksagonalna to narzędzie, nie obowiązek.
     *
     * PYTANIA KONTROLNE:
     *   1. Na czym polega reguła zależności w architekturze heksagonalnej? Która część kodu nie importuje niczego
     *      z pozostałych?
     *   2. Co wypisze:  System.out.println(Money.of("5.0").equals(Money.of("5.00")) + " "
     *                       + new BigDecimal("5.0").equals(new BigDecimal("5.00")));  ?
     *   3. Co wypisze (Order z tej lekcji):
     *        Order a = Order.place(new OrderId("ZAM-500"), "x@example.com", CLOCK);
     *        Order b = Order.place(new OrderId("ZAM-500"), "y@example.com", CLOCK);
     *        b.addLine("A-1", 2, Money.of("1.00"));
     *        System.out.println(a.equals(b) + " " + a.total().equals(b.total()));  ?
     *   4. ZNAJDŹ BŁĄD (port wyjściowy w rdzeniu aplikacji):
     *        interface OrderRepository {
     *            void save(OrderJpaEntity row);              // OrderJpaEntity — klasa z adnotacjami JPA
     *            ResultSet findById(String id);              // java.sql.ResultSet
     *        }
     *   5. ZNAJDŹ BŁĄD (agregat):
     *        final class Order {
     *            private final List<OrderLine> lines = new ArrayList<>();
     *            List<OrderLine> lines() { return lines; }
     *            void addLine(OrderLine l) { if (lines.size() == 3) throw new DomainException("max 3"); lines.add(l); }
     *        }
     *   6. Czym różni się encja od obiektu wartości? Dlaczego rekord pasuje do obiektu wartości, a do encji zwykle nie?
     *   7. Podaj dwie sytuacje, w których architektura heksagonalna to przesada.
     *   8. Co to jest korzeń kompozycji i co pełni tę rolę w aplikacji Springa?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: suma kwot", Money.of("123.45"), () -> exercise1(List.of("100", "20.5", "2.95")));
        Check.equal("ćw. 2: bramka z limitem", List.of(true, true, false),
                () -> chargeAll(exercise2(Money.of("500.00")), List.of("0.00", "500.00", "500.01")));
        Check.equal("ćw. 3: naruszenia reguły zależności", EXPECTED_VIOLATIONS, () -> exercise3(DEPENDENCIES));
        Check.equal("ćw. 4: przypadek użycia „anuluj”", EXPECTED_CANCEL, () -> runCancelScenario(
                (repo, notifier, id) -> exercise4(repo, notifier, id, "rezygnacja klienta")));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", Money.of("123.45"), () -> solution1(List.of("100", "20.5", "2.95")));
        Check.equal("ćw. 2 (wzorzec)", List.of(true, true, false),
                () -> chargeAll(solution2(Money.of("500.00")), List.of("0.00", "500.00", "500.01")));
        Check.equal("ćw. 3 (wzorzec)", EXPECTED_VIOLATIONS, () -> solution3(DEPENDENCIES));
        Check.equal("ćw. 4 (wzorzec)", EXPECTED_CANCEL, () -> runCancelScenario(
                (repo, notifier, id) -> solution4(repo, notifier, id, "rezygnacja klienta")));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** chargeAll = obciąż wszystkie: woła bramkę dla kolejnych kwot (ZAM-900, ZAM-901, …) i zbiera odpowiedzi. */
    static List<Boolean> chargeAll(PaymentGateway gateway, List<String> amounts) {
        List<Boolean> results = new ArrayList<>();
        for (int i = 0; i < amounts.size(); i++) {
            results.add(gateway.charge(new OrderId(String.format("ZAM-%03d", 900 + i)), Money.of(amounts.get(i))));
        }
        return results;
    }

    static final List<String> DEPENDENCIES = List.of(
            "APPLICATION->DOMAIN", "DOMAIN->INFRASTRUCTURE", "INFRASTRUCTURE->DOMAIN",
            "PRESENTATION->INFRASTRUCTURE", "DOMAIN->APPLICATION", "PRESENTATION->APPLICATION");

    static final List<String> EXPECTED_VIOLATIONS = List.of(
            "DOMAIN->APPLICATION", "DOMAIN->INFRASTRUCTURE", "PRESENTATION->INFRASTRUCTURE");

    /** Funkcja z trzema argumentami dla ćwiczenia 4. CancelAction = akcja anulowania; apply = zastosuj. */
    @FunctionalInterface
    interface CancelAction {
        String apply(OrderRepository orders, Notifier notifier, String orderId);
    }

    static final List<String> EXPECTED_CANCEL = List.of(
            "ANULOWANO ZAM-201",
            "ODMOWA ZAM-202: Nie można anulować zamówienia w stanie PAID",
            "BRAK ZAM-999",
            "stan ZAM-201: CANCELLED",
            "a@example.com: Anulowano zamówienie ZAM-201 (rezygnacja klienta)");

    /** runCancelScenario = uruchom scenariusz anulowania: przygotowuje atrapy i zbiera wyniki do porównania. */
    static List<String> runCancelScenario(CancelAction action) {
        InMemoryOrderRepository repo = new InMemoryOrderRepository();
        Order fresh = Order.place(new OrderId("ZAM-201"), "a@example.com", CLOCK);
        fresh.addLine("A-1", 1, Money.of("10.00"));
        Order paid = Order.place(new OrderId("ZAM-202"), "b@example.com", CLOCK);
        paid.addLine("A-1", 1, Money.of("10.00"));
        paid.pay();
        fresh.pullEvents();
        paid.pullEvents();                                        // zdarzenia z przygotowania nas nie interesują
        repo.save(fresh);
        repo.save(paid);
        RecordingNotifier spy = new RecordingNotifier();

        List<String> results = new ArrayList<>();
        results.add(action.apply(repo, spy, "ZAM-201"));
        results.add(action.apply(repo, spy, "ZAM-202"));
        results.add(action.apply(repo, spy, "ZAM-999"));
        results.add("stan ZAM-201: " + repo.findById(new OrderId("ZAM-201")).map(Order::status).orElse(null));
        results.addAll(spy.sent());
        return results;
    }

    /**
     * ĆWICZENIE 1 (łatwe): zsumuj kwoty podane jako tekst, używając obiektu wartości Money (Money.of, plus).
     * Dla ["100", "20.5", "2.95"] wynik to Money 123.45.
     * Podpowiedź: zacznij od Money.ZERO i w pętli dodawaj Money.of(tekst) — nie licz na „gołych” BigDecimal.
     */
    static Money exercise1(List<String> amounts) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): napisz adapter wyjściowy portu PaymentGateway, który przyjmuje płatność (true), gdy kwota
     * jest mniejsza lub równa limitowi, a odrzuca (false), gdy go przekracza. Zwróć go jako obiekt portu.
     * Podpowiedź: PaymentGateway ma jedną metodę abstrakcyjną, więc wystarczy lambda {@code (id, amount) -> ...};
     * użyj Money.isGreaterThan.
     */
    static PaymentGateway exercise2(Money limit) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, łączy ze strumieniami t16): mały „test architektury”. Dostajesz zależności w formacie
     * "OD->DO" (nazwy z enum Layer). Zwróć posortowaną alfabetycznie listę tych, które łamią regułę po odwróceniu
     * zależności (metoda invertedAllows z sekcji 2).
     * Podpowiedź: {@code split("->")}, {@code Layer.valueOf(...)}, filter, sorted, toList (Java 16+).
     */
    static List<String> exercise3(List<String> dependencies) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): PRZEPISZ kod proceduralny na przypadek użycia oparty na portach i agregacie.
     * Stary kod („mapa jako baza”, reguła w if-ie, e-mail wprost):
     * <pre>{@code
     * static String cancel(Map<String, Map<String, Object>> db, String id) {
     *     Map<String, Object> row = db.get(id);
     *     if (row == null) return "BRAK " + id;
     *     if (!"NEW".equals(row.get("status"))) return "ODMOWA " + id;
     *     row.put("status", "CANCELLED");
     *     System.out.println("Wysyłam e-mail do " + row.get("email"));
     *     return "ANULOWANO " + id;
     * }
     * }</pre>
     * Nowa wersja: znajdź zamówienie przez orders.findById; brak → "BRAK id"; wywołaj order.cancel(reason) — regułę
     * sprawdza agregat, a jego DomainException zamień na "ODMOWA id: komunikat"; po sukcesie zapisz (save), potem
     * wyślij przez notifier opisy zdarzeń z pullEvents() na e-mail klienta i zwróć "ANULOWANO id".
     * Podpowiedź: wzoruj się na PlaceOrderService.placeOrder — najpierw zapis, potem zdarzenia.
     */
    static String exercise4(OrderRepository orders, Notifier notifier, String orderId, String reason) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Money solution1(List<String> amounts) {
        Money sum = Money.ZERO;
        for (String amount : amounts) {
            sum = sum.plus(Money.of(amount));
        }
        return sum;
    }

    static PaymentGateway solution2(Money limit) {
        return (orderId, amount) -> !amount.isGreaterThan(limit);
    }

    static List<String> solution3(List<String> dependencies) {
        return dependencies.stream()
                .filter(d -> {
                    String[] parts = d.split("->");
                    return !invertedAllows(Layer.valueOf(parts[0]), Layer.valueOf(parts[1]));
                })
                .sorted()
                .toList();
    }

    static String solution4(OrderRepository orders, Notifier notifier, String orderId, String reason) {
        Optional<Order> found = orders.findById(new OrderId(orderId));
        if (found.isEmpty()) {                                    // isEmpty (Java 11+) = jest pusty
            return "BRAK " + orderId;
        }
        Order order = found.get();
        try {
            order.cancel(reason);
        } catch (DomainException e) {
            return "ODMOWA " + orderId + ": " + e.getMessage();
        }
        orders.save(order);
        for (DomainEvent event : order.pullEvents()) {
            notifier.send(order.customerEmail(), event.describe());
        }
        return "ANULOWANO " + orderId;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Zależności (importy) wskazują do środka: adaptery → porty/aplikacja → dziedzina. Dziedzina nie importuje
     *      niczego z aplikacji ani adapterów (ani frameworków); serwis aplikacji zna tylko interfejsy portów.
     *   2. "true false" — Money ujednolica skalę do 2 w konstruktorze kompaktowym, więc rekordy są równe;
     *      BigDecimal.equals porównuje też skalę (5.0 ma skalę 1, 5.00 — skalę 2).
     *   3. "true false" — Order to encja z equals po id (oba ZAM-500), ale stan jest różny: suma 0.00 zł vs 2.00 zł.
     *   4. Port wyjściowy przecieka technologią: rdzeń aplikacji zależy od JPA i JDBC, więc reguła zależności jest
     *      złamana, a atrapy w testach muszą udawać ResultSet. Port mówi językiem dziedziny:
     *      {@code void save(Order order); Optional<Order> findById(OrderId id);} — tłumaczenie robi adapter.
     *   5. lines() zwraca wewnętrzną listę, więc order.lines().add(...) dodaje czwartą pozycję z pominięciem
     *      niezmiennika. Poprawka: return List.copyOf(lines); (albo Collections.unmodifiableList).
     *   6. Encja ma tożsamość trwającą mimo zmian stanu (równość po id), obiekt wartości — nie (równość po wartości,
     *      niezmienny, wymienny na inny o tej samej wartości). Rekord generuje equals po WSZYSTKICH polach i jest
     *      niezmienny — idealny dla wartości, a dla encji dałby „inne zamówienie” po każdej zmianie.
     *   7. Np.: prosta aplikacja CRUD bez reguł biznesowych; prototyp / skrypt / mały projekt jednej osoby;
     *      tworzenie interfejsów z jedną implementacją „na zapas”.
     *   8. Jedno miejsce na brzegu programu, gdzie tworzy się konkretne adaptery i łączy je z serwisami przez
     *      konstruktory (tu: początek metody hexagonal()). W Springu robi to kontener IoC (ApplicationContext) na
     *      podstawie skanowania komponentów i klas @Configuration z metodami @Bean.
     */
    // </editor-fold>
}
