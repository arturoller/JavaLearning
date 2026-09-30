package t25_testing;

import helpers.Check;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Testy podwójne — dummy, stub, fake, spy, mock
 *        (test double = test podwójny; dependency = zależność)
 *
 * W SKRÓCIE:
 *   Klasa pod testami często potrzebuje INNYCH obiektów, żeby działać: bazy danych, bramki płatności, zegara.
 *   Prawdziwe wersje tych obiektów są wolne, niedeterministyczne albo po prostu niedostępne w teście. Test double
 *   (test podwójny) to WŁASNA, uproszczona implementacja tej samej zależności (tego samego interfejsu), podstawiona
 *   w teście zamiast prawdziwej. Pięć rodzajów: dummy (wypełniacz, nieużywany), stub (zwraca ustaloną odpowiedź),
 *   fake (działa naprawdę, tylko prościej), spy (zapamiętuje wywołania), mock (sam sprawdza oczekiwania na końcu).
 *
 * ANALOGIA: próby generalne w teatrze.
 *   Zamiast prawdziwej publiczności (produkcja) na próbie siedzi reżyser (test) i ustawia rekwizyty: plastikowy
 *   miecz zamiast ostrego (stub — bezpieczna, ustalona reakcja), asystent notujący każde wejście aktora (spy —
 *   zapamiętuje), zegar sceniczny przestawiony na "20:00" niezależnie od godziny za oknem (fake czasu — Clock).
 *   Nikt nie czeka na prawdziwy zachód słońca, żeby przećwiczyć scenę zachodu.
 *
 * JAK TO DZIAŁA:
 *   1. Zależność jest INTERFEJSEM (PaymentGateway, Notifier, OrderRepository), nie konkretną klasą.
 *   2. Testowana klasa (OrderService) dostaje zależności przez KONSTRUKTOR (constructor injection) — nie tworzy
 *      ich sama przez `new`.
 *   3. W teście podstawiamy WŁASNĄ implementację interfejsu — prostą klasę zagnieżdżoną albo lambdę.
 *   4. Czas i losowość też są "zależnościami": Clock zamiast LocalDate.now(), {@code Supplier<String>}/Random
 *      z ziarnem zamiast new Random() bez ziarna.
 *
 * SŁÓWKA:
 *   dummy = atrapa (wymagana przez sygnaturę, ale nieużywana); stub = zaślepka (zwraca ustaloną odpowiedź); fake =
 *   podróbka (uproszczona, ale DZIAŁAJĄCA implementacja); spy = szpieg (zapamiętuje wywołania do sprawdzenia
 *   później); mock = makieta z oczekiwaniami (sama sprawdza PASS/FAIL); verify = zweryfikuj (sprawdź oczekiwania);
 *   constructor injection = wstrzykiwanie przez konstruktor; seed = ziarno (losowości).
 *
 * ZOBACZ TEŻ: t25_testing/Testing01Concepts (mini-runner, który tu ponownie wykorzystujemy), t25_testing/Testing03TestableDesign
 *             (jak projektować klasy, żeby dało się do nich WSTRZYKNĄĆ testy podwójne), t17_datetime/DateTime01LocalDateTime
 *             (Clock, Instant, LocalDate).
 * </pre>
 */
public class Testing02TestDoubles {

    public static void main(String[] args) {
        title("Testing02 — testy podwójne (test doubles)");

        whyDependenciesAreHard();   // why dependencies are hard = dlaczego zależności utrudniają testy
        orderServiceUnderTest();     // order service under test = OrderService pod testami
        dummyDemo();                  // dummy demo = pokaz atrapy
        stubDemo();                   // stub demo = pokaz zaślepki
        fakeDemo();                   // fake demo = pokaz podróbki (działającej)
        spyDemo();                    // spy demo = pokaz szpiega
        mockDemo();                   // mock demo = pokaz makiety z oczekiwaniami
        timeAndRandomness();          // time and randomness = czas i losowość
        allTogether();                 // all together = wszystko razem (mini-runner)
        mockitoPointer();             // Mockito pointer = wskazówka do Mockito
        exercises();                   // exercises = ćwiczenia
    }

    // =================================================================================================
    // TYPY POD TESTAMI (zagnieżdżone klasy/interfejsy/rekordy — w prawdziwym projekcie: osobne pliki)
    // =================================================================================================

    /** Order = zamówienie: minimalny rekord na potrzeby tej lekcji (nie mylić z helpers.model.Order). */
    record Order(String id, String customerEmail, BigDecimal amount, LocalDate date) {
    }

    /** PaymentGateway = bramka płatności. W realu: wywołanie sieciowe do banku/operatora kart. */
    interface PaymentGateway {
        boolean charge(String customerEmail, BigDecimal amount);
    }

    /** Notifier = powiadamiacz klienta. W realu: wysyłka e-maila/SMS-a. */
    interface Notifier {
        void sendConfirmation(String customerEmail, String orderId);
    }

    /** OrderRepository = zapis zamówień. W realu: tabela w bazie danych. */
    interface OrderRepository {
        void save(Order order);

        Optional<Order> findById(String id);
    }

    /** AuditLog = log audytowy (kto/co/kiedy). W metodzie placeOrder poniżej celowo GO NIE UŻYWAMY. */
    interface AuditLog {
        void write(String message);
    }

    /** PaymentDeclinedException = płatność odrzucona — normalny, spodziewany błąd biznesowy. */
    static class PaymentDeclinedException extends RuntimeException {
        PaymentDeclinedException(String message) {
            super(message);
        }
    }

    /**
     * OrderService = klasa POD TESTAMI. Wszystkie zależności przychodzą przez konstruktor
     * (constructor injection) — klasa sama nie tworzy ŻADNEJ z nich przez {@code new}, więc w teście możemy
     * podstawić dowolne testy podwójne.
     */
    static class OrderService {
        private final PaymentGateway paymentGateway;
        private final Notifier notifier;
        private final OrderRepository repository;
        private final Clock clock;
        private final Supplier<String> idGenerator;
        private final AuditLog auditLog;   // dummy = wymagany przez konstruktor, ale NIEUŻYWANY w placeOrder

        OrderService(PaymentGateway paymentGateway, Notifier notifier, OrderRepository repository,
                     Clock clock, Supplier<String> idGenerator, AuditLog auditLog) {
            this.paymentGateway = paymentGateway;
            this.notifier = notifier;
            this.repository = repository;
            this.clock = clock;
            this.idGenerator = idGenerator;
            this.auditLog = auditLog;
        }

        /** placeOrder = złóż zamówienie: obciąż płatność, zapisz, powiadom. auditLog świadomie pominięty. */
        String placeOrder(String customerEmail, BigDecimal amount) {
            if (!paymentGateway.charge(customerEmail, amount)) {
                throw new PaymentDeclinedException("Płatność odrzucona dla " + customerEmail + ": " + amount);
            }
            String id = idGenerator.get();
            repository.save(new Order(id, customerEmail, amount, LocalDate.now(clock)));
            notifier.sendConfirmation(customerEmail, id);
            return id;
        }
    }

    // ---- testy podwójne -------------------------------------------------------------------------------

    /** StubPaymentGateway = zaślepka: zawsze zwraca TO SAMO, z góry ustalone `approve`, bez żadnej logiki. */
    static class StubPaymentGateway implements PaymentGateway {
        private final boolean approve;

        StubPaymentGateway(boolean approve) {
            this.approve = approve;
        }

        @Override
        public boolean charge(String customerEmail, BigDecimal amount) {
            return approve;
        }
    }

    /** InMemoryOrderRepository = fake: NAPRAWDĘ zapisuje i odczytuje, tylko w pamięci (LinkedHashMap), nie w bazie. */
    static class InMemoryOrderRepository implements OrderRepository {
        private final Map<String, Order> storage = new LinkedHashMap<>();

        @Override
        public void save(Order order) {
            storage.put(order.id(), order);
        }

        @Override
        public Optional<Order> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }

        int size() {
            return storage.size();
        }
    }

    /** SpyNotifier = szpieg: robi to samo co prawdziwy (nic nie robi na zewnątrz), ale ZAPAMIĘTUJE wywołania. */
    static class SpyNotifier implements Notifier {
        private final List<String> sent = new ArrayList<>();

        @Override
        public void sendConfirmation(String customerEmail, String orderId) {
            sent.add(customerEmail + " (" + orderId + ")");
        }

        List<String> sentMessages() {
            return List.copyOf(sent);
        }
    }

    /** MockPaymentGateway = makieta: z góry deklarujemy oczekiwania (expectChargeFor), verify() sam ocenia PASS/FAIL. */
    static class MockPaymentGateway implements PaymentGateway {
        private final Set<String> expectedCustomers = new LinkedHashSet<>();
        private final List<String> actuallyCharged = new ArrayList<>();

        void expectChargeFor(String customerEmail) {
            expectedCustomers.add(customerEmail);
        }

        @Override
        public boolean charge(String customerEmail, BigDecimal amount) {
            actuallyCharged.add(customerEmail);
            return true;
        }

        /** verify = zweryfikuj: rzuca AssertionError, jeśli KTÓRYŚ oczekiwany klient nie został obciążony. */
        void verify() {
            for (String email : expectedCustomers) {
                if (!actuallyCharged.contains(email)) {
                    throw new AssertionError("Oczekiwano obciążenia klienta " + email + ", ale nie wystąpiło");
                }
            }
        }
    }

    // ---- stałe: czas i identyfikator wspólne dla większości demo (mniej powtórzeń w kodzie) -----------

    static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-03-10T09:15:00Z"), ZoneOffset.UTC);
    static final Supplier<String> FIXED_ID_GENERATOR = () -> "ZAM-TEST";
    static final AuditLog THROWING_AUDIT_LOG = message -> {
        throw new AssertionError("AuditLog (dummy) nie powinien zostać wywołany, a dostał: " + message);
    };

    /** buildService = złóż OrderService z podanymi trzema zależnościami + wspólny zegar/id/dummy-audit powyżej. */
    static OrderService buildService(PaymentGateway paymentGateway, Notifier notifier, OrderRepository repository) {
        return new OrderService(paymentGateway, notifier, repository, FIXED_CLOCK, FIXED_ID_GENERATOR, THROWING_AUDIT_LOG);
    }

    // =================================================================================================
    // 1. DLACZEGO ZALEŻNOŚCI UTRUDNIAJĄ TESTOWANIE
    // =================================================================================================

    static void whyDependenciesAreHard() {
        section("1. Dlaczego zależności utrudniają testowanie");

        List<String> reasons = List.of(
                "baza danych (DB) — trzeba ją uruchomić, wypełnić danymi i posprzątać po teście; testy stają się wolne",
                "sieć (network) — połączenie może nie działać, być wolne albo niedostępne akurat podczas testu",
                "czas (LocalDate.now(), Instant.now()) — wynik zależy od DNIA uruchomienia, test nie jest powtarzalny",
                "losowość (new Random() bez ziarna) — inny wynik za każdym razem, nie da się przewidzieć oczekiwanej wartości");
        for (int i = 0; i < reasons.size(); i++) {
            System.out.println("   " + (i + 1) + ". " + reasons.get(i));
        }
        // WYNIK:    1. baza danych (DB) — trzeba ją uruchomić, wypełnić danymi i posprzątać po teście; testy stają się wolne
        // WYNIK:    2. sieć (network) — połączenie może nie działać, być wolne albo niedostępne akurat podczas testu
        // WYNIK:    3. czas (LocalDate.now(), Instant.now()) — wynik zależy od DNIA uruchomienia, test nie jest powtarzalny
        // WYNIK:    4. losowość (new Random() bez ziarna) — inny wynik za każdym razem, nie da się przewidzieć oczekiwanej wartości

        note("Rozwiązanie: testowana klasa NIE tworzy tych zależności sama (new ...), tylko dostaje je z zewnątrz");
        note("(constructor injection) — w testach podstawiamy WŁASNE, proste implementacje: testy podwójne.");
        // WYNIK:    ℹ Rozwiązanie: testowana klasa NIE tworzy tych zależności sama (new ...), tylko dostaje je z zewnątrz
        // WYNIK:    ℹ (constructor injection) — w testach podstawiamy WŁASNE, proste implementacje: testy podwójne.
    }

    // =================================================================================================
    // 2. OrderService — KLASA POD TESTAMI
    // =================================================================================================

    static void orderServiceUnderTest() {
        section("2. OrderService — klasa pod testami");

        note("OrderService.placeOrder(email, kwota) korzysta z PIĘCIU zależności (wszystkie przez konstruktor):");
        note("  PaymentGateway — obciąża płatność (prawdziwa: sieć, może się nie udać)");
        note("  Notifier — wysyła potwierdzenie (prawdziwa: e-mail/SMS, sieć)");
        note("  OrderRepository — zapisuje zamówienie (prawdziwa: baza danych)");
        note("  Clock — 'teraz' do daty zamówienia (prawdziwy: LocalDate.now() zmienia się codziennie)");
        note("  AuditLog — log audytowy (w placeOrder akurat NIEUŻYWANY — to będzie nasz 'dummy')");
        // WYNIK:    ℹ OrderService.placeOrder(email, kwota) korzysta z PIĘCIU zależności (wszystkie przez konstruktor):
        // WYNIK:    ℹ   PaymentGateway — obciąża płatność (prawdziwa: sieć, może się nie udać)
        // WYNIK:    ℹ   Notifier — wysyła potwierdzenie (prawdziwa: e-mail/SMS, sieć)
        // WYNIK:    ℹ   OrderRepository — zapisuje zamówienie (prawdziwa: baza danych)
        // WYNIK:    ℹ   Clock — 'teraz' do daty zamówienia (prawdziwy: LocalDate.now() zmienia się codziennie)
        // WYNIK:    ℹ   AuditLog — log audytowy (w placeOrder akurat NIEUŻYWANY — to będzie nasz 'dummy')
    }

    // =================================================================================================
    // 3. DUMMY
    // =================================================================================================

    static void dummyDemo() {
        section("3. Dummy — zależność wymagana, ale nieużywana");

        OrderService service = buildService(new StubPaymentGateway(true), new SpyNotifier(), new InMemoryOrderRepository());
        String id = service.placeOrder("jan@example.com", new BigDecimal("199.99"));
        show("placeOrder(...) → id zamówienia", id);
        // WYNIK: placeOrder(...) → id zamówienia → ZAM-TEST

        note("THROWING_AUDIT_LOG miał rzucić błąd, gdyby ktokolwiek go wywołał — a program poszedł dalej bez wyjątku.");
        note("To dowód, że placeOrder w OGÓLE nie korzysta z AuditLog w tej ścieżce — to właśnie jest 'dummy'.");
        // WYNIK:    ℹ THROWING_AUDIT_LOG miał rzucić błąd, gdyby ktokolwiek go wywołał — a program poszedł dalej bez wyjątku.
        // WYNIK:    ℹ To dowód, że placeOrder w OGÓLE nie korzysta z AuditLog w tej ścieżce — to właśnie jest 'dummy'.

        // DOBRA PRAKTYKA: jeśli dummy MUSIAŁBY zostać wywołany, żeby test przeszedł — to nie jest dummy, tylko
        //   zwykła zależność, którą trzeba potraktować jak stub/fake/spy/mock.
    }

    // =================================================================================================
    // 4. STUB
    // =================================================================================================

    static void stubDemo() {
        section("4. Stub — z góry ustalona odpowiedź, bez logiki");

        OrderService approving = buildService(new StubPaymentGateway(true), new SpyNotifier(), new InMemoryOrderRepository());
        show("stub zatwierdzający: placeOrder(...)", approving.placeOrder("anna@example.com", new BigDecimal("50.00")));
        // WYNIK: stub zatwierdzający: placeOrder(...) → ZAM-TEST

        OrderService declining = buildService(new StubPaymentGateway(false), new SpyNotifier(), new InMemoryOrderRepository());
        expectThrows("stub odrzucający: placeOrder(...)", () -> declining.placeOrder("anna@example.com", new BigDecimal("50.00")));
        // WYNIK: ✔ stub odrzucający: placeOrder(...) → rzucono PaymentDeclinedException: Płatność odrzucona dla anna@example.com: 50.00

        note("Stub NIE SPRAWDZA nic — po prostu zwraca to, co mu kazano. To wystarcza, żeby sterować testowanym kodem.");
        // WYNIK:    ℹ Stub NIE SPRAWDZA nic — po prostu zwraca to, co mu kazano. To wystarcza, żeby sterować testowanym kodem.
    }

    // =================================================================================================
    // 5. FAKE
    // =================================================================================================

    static void fakeDemo() {
        section("5. Fake — działająca, ale uproszczona implementacja");

        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        OrderService service = buildService(new StubPaymentGateway(true), new SpyNotifier(), repository);
        String id = service.placeOrder("celina@example.com", new BigDecimal("75.50"));

        show("repository.findById(id).isPresent()", repository.findById(id).isPresent());
        show("repository.findById(id).orElseThrow()", repository.findById(id).orElseThrow());
        // WYNIK: repository.findById(id).isPresent() → true
        // WYNIK: repository.findById(id).orElseThrow() → Order[id=ZAM-TEST, customerEmail=celina@example.com, amount=75.50, date=2026-03-10]

        note("Fake NAPRAWDĘ działa (zapisuje i odczytuje) — tylko prościej niż prawdziwa baza: bez pliku, bez sieci, ginie po teście.");
        // WYNIK:    ℹ Fake NAPRAWDĘ działa (zapisuje i odczytuje) — tylko prościej niż prawdziwa baza: bez pliku, bez sieci, ginie po teście.
    }

    // =================================================================================================
    // 6. SPY
    // =================================================================================================

    static void spyDemo() {
        section("6. Spy — zapamiętuje wywołania do sprawdzenia PÓŹNIEJ");

        SpyNotifier spy = new SpyNotifier();
        OrderService service = buildService(new StubPaymentGateway(true), spy, new InMemoryOrderRepository());
        service.placeOrder("darek@example.com", new BigDecimal("120.00"));

        showEach("spy.sentMessages()", spy.sentMessages());
        // WYNIK: spy.sentMessages() (liczba elementów: 1):
        // WYNIK:    • darek@example.com (ZAM-TEST)

        note("Spy nie decyduje sam, czy test przeszedł — to TEST sprawdza później, co spy zapamiętał (tu: jedna wiadomość).");
        // WYNIK:    ℹ Spy nie decyduje sam, czy test przeszedł — to TEST sprawdza później, co spy zapamiętał (tu: jedna wiadomość).
    }

    // =================================================================================================
    // 7. MOCK
    // =================================================================================================

    static void mockDemo() {
        section("7. Mock — oczekiwania sprawdzane na końcu (verify)");

        MockPaymentGateway mock = new MockPaymentGateway();
        mock.expectChargeFor("ewa@example.com");
        OrderService service = buildService(mock, new SpyNotifier(), new InMemoryOrderRepository());
        service.placeOrder("ewa@example.com", new BigDecimal("300.00"));
        mock.verify();
        note("mock.verify() przeszło bez wyjątku — obciążenie klienta ewa@example.com faktycznie wystąpiło.");
        // WYNIK:    ℹ mock.verify() przeszło bez wyjątku — obciążenie klienta ewa@example.com faktycznie wystąpiło.

        MockPaymentGateway unmetMock = new MockPaymentGateway();
        unmetMock.expectChargeFor("nikt@example.com");   // nikt taki nie zostanie obciążony w tym demo
        expectThrows("unmetMock.verify() bez wywołania obciążenia", unmetMock::verify);
        // WYNIK: ✔ unmetMock.verify() bez wywołania obciążenia → rzucono AssertionError: Oczekiwano obciążenia klienta nikt@example.com, ale nie wystąpiło

        note("Mock = spy + wbudowana asercja: verify() SAM decyduje PASS/FAIL na podstawie oczekiwań ustawionych z góry.");
        // WYNIK:    ℹ Mock = spy + wbudowana asercja: verify() SAM decyduje PASS/FAIL na podstawie oczekiwań ustawionych z góry.
    }

    // =================================================================================================
    // 8. CZAS I LOSOWOŚĆ JAKO ZALEŻNOŚCI DO WSTRZYKNIĘCIA
    // =================================================================================================

    static void timeAndRandomness() {
        section("8. Czas i losowość jako zależności do wstrzyknięcia");

        show("LocalDate.now(FIXED_CLOCK)", LocalDate.now(FIXED_CLOCK));
        // WYNIK: LocalDate.now(FIXED_CLOCK) → 2026-03-10

        Random rng = new Random(42);   // ziarno (seed) 42 → ZAWSZE ta sama sekwencja liczb
        Supplier<String> randomIdGenerator = () -> "ZAM-" + (1000 + rng.nextInt(9000));
        show("randomIdGenerator.get() — Random(42), 1. wywołanie", randomIdGenerator.get());
        show("randomIdGenerator.get() — Random(42), 2. wywołanie", randomIdGenerator.get());
        // WYNIK: randomIdGenerator.get() — Random(42), 1. wywołanie → ZAM-5130
        // WYNIK: randomIdGenerator.get() — Random(42), 2. wywołanie → ZAM-6763

        note("Clock.fixed(...) i new Random(SEED) dają ZAWSZE ten sam wynik — bez tego testy łamałyby Repeatable (FIRST, Testing01).");
        // WYNIK:    ℹ Clock.fixed(...) i new Random(SEED) dają ZAWSZE ten sam wynik — bez tego testy łamałyby Repeatable (FIRST, Testing01).
    }

    // =================================================================================================
    // 9. WSZYSTKO RAZEM — MINI-RUNNER (kopia z Testing01, w skróconej wersji)
    // =================================================================================================

    /** TestCase = przypadek testowy: nazwa + treść (szczegóły w Testing01Concepts, sekcja 5). */
    record TestCase(String name, Runnable body) {
    }

    static void assertEquals(String message, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " — oczekiwano: " + expected + ", jest: " + actual);
        }
    }

    static void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void assertThrows(String message, Class<? extends Throwable> expectedType, Runnable body) {
        try {
            body.run();
        } catch (Throwable t) {
            if (expectedType.isInstance(t)) {
                return;
            }
            throw new AssertionError(message + " — spodziewano się " + expectedType.getSimpleName()
                    + ", rzucono " + t.getClass().getSimpleName(), t);
        }
        throw new AssertionError(message + " — spodziewano się wyjątku " + expectedType.getSimpleName() + ", nic nie rzucono");
    }

    static int runSuite(List<TestCase> tests) {
        int passed = 0;
        int failed = 0;
        for (TestCase test : tests) {
            try {
                test.body().run();
                System.out.println("   ✔ " + test.name());
                passed++;
            } catch (AssertionError e) {
                System.out.println("   ✘ " + test.name() + " — " + e.getMessage());
                failed++;
            }
        }
        note("wynik: " + passed + " OK, " + failed + " BŁĄD");
        return failed;
    }

    /** 9. Pięć testów łączących wszystkie testy podwójne z tej lekcji — w tym jeden celowo zły. */
    static void allTogether() {
        section("9. Wszystko razem: pełny zestaw testów OrderService");

        List<TestCase> tests = List.of(
                new TestCase("zatwierdzona płatność → zamówienie zapisane w repozytorium", () -> {
                    InMemoryOrderRepository repository = new InMemoryOrderRepository();
                    OrderService service = buildService(new StubPaymentGateway(true), new SpyNotifier(), repository);
                    String id = service.placeOrder("test1@example.com", new BigDecimal("10.00"));
                    assertTrue("zamówienie w repozytorium", repository.findById(id).isPresent());
                }),
                new TestCase("zatwierdzona płatność → dokładnie jedno powiadomienie", () -> {
                    SpyNotifier spy = new SpyNotifier();
                    OrderService service = buildService(new StubPaymentGateway(true), spy, new InMemoryOrderRepository());
                    service.placeOrder("test2@example.com", new BigDecimal("10.00"));
                    assertEquals("liczba powiadomień", 1, spy.sentMessages().size());
                }),
                new TestCase("odrzucona płatność → wyjątek i BRAK zapisu", () -> {
                    InMemoryOrderRepository repository = new InMemoryOrderRepository();
                    OrderService service = buildService(new StubPaymentGateway(false), new SpyNotifier(), repository);
                    assertThrows("placeOrder przy odrzuceniu", PaymentDeclinedException.class,
                            () -> service.placeOrder("test3@example.com", new BigDecimal("10.00")));
                    assertEquals("repozytorium puste", 0, repository.size());
                }),
                new TestCase("odrzucona płatność → BRAK powiadomienia", () -> {
                    SpyNotifier spy = new SpyNotifier();
                    OrderService service = buildService(new StubPaymentGateway(false), spy, new InMemoryOrderRepository());
                    assertThrows("placeOrder przy odrzuceniu", PaymentDeclinedException.class,
                            () -> service.placeOrder("test4@example.com", new BigDecimal("10.00")));
                    assertTrue("spy pusty", spy.sentMessages().isEmpty());
                }),
                new TestCase("celowo zła liczba powiadomień — pokazuje ✘", () -> {
                    SpyNotifier spy = new SpyNotifier();
                    OrderService service = buildService(new StubPaymentGateway(true), spy, new InMemoryOrderRepository());
                    service.placeOrder("test5@example.com", new BigDecimal("10.00"));
                    assertEquals("liczba powiadomień (celowo źle)", 2, spy.sentMessages().size());
                }));
        runSuite(tests);
        // WYNIK:    ✔ zatwierdzona płatność → zamówienie zapisane w repozytorium
        // WYNIK:    ✔ zatwierdzona płatność → dokładnie jedno powiadomienie
        // WYNIK:    ✔ odrzucona płatność → wyjątek i BRAK zapisu
        // WYNIK:    ✔ odrzucona płatność → BRAK powiadomienia
        // WYNIK:    ✘ celowo zła liczba powiadomień — pokazuje ✘ — liczba powiadomień (celowo źle) — oczekiwano: 2, jest: 1
        // WYNIK:    ℹ wynik: 4 OK, 1 BŁĄD
    }

    // =================================================================================================
    // 10. W PRAKTYCE: MOCKITO
    // =================================================================================================

    static void mockitoPointer() {
        section("10. W praktyce: Mockito");

        note("Ręczne pisanie stubów/mocków (jak wyżej) uczy, JAK to działa — w większych projektach jest żmudne.");
        note("Framework Mockito (dział t32_junit_mockito) generuje takie obiekty automatycznie: mock(PaymentGateway.class),");
        note("when(gateway.charge(...)).thenReturn(true), verify(gateway).charge(...) — te same idee, mniej ręcznego kodu.");
        // WYNIK:    ℹ Ręczne pisanie stubów/mocków (jak wyżej) uczy, JAK to działa — w większych projektach jest żmudne.
        // WYNIK:    ℹ Framework Mockito (dział t32_junit_mockito) generuje takie obiekty automatycznie: mock(PaymentGateway.class),
        // WYNIK:    ℹ when(gateway.charge(...)).thenReturn(true), verify(gateway).charge(...) — te same idee, mniej ręcznego kodu.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Zależność = interfejs; testowana klasa dostaje ją przez konstruktor (constructor injection).
     *   • dummy — wymagany, ale nieużywany w danej ścieżce (dowód: rzuca błąd, jeśli jednak wywołany).
     *   • stub — zwraca z góry ustaloną odpowiedź, bez logiki (StubPaymentGateway).
     *   • fake — DZIAŁA naprawdę, tylko prościej (InMemoryOrderRepository zamiast bazy danych).
     *   • spy — zapamiętuje wywołania; TEST sprawdza je później (SpyNotifier.sentMessages()).
     *   • mock — sam sprawdza oczekiwania (verify()); ustawiane PRZED akcją (MockPaymentGateway).
     *   • czas i losowość też są zależnościami: Clock.fixed(...), new Random(SEED) — bez nich testy nie są Repeatable.
     *   • Mockito (t32) generuje testy podwójne automatycznie — te same idee, mniej ręcznego kodu.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się stub od mocka — kto ostatecznie decyduje, czy test przeszedł?
     *   2. Dlaczego dummy AuditLog w tej lekcji rzuca wyjątek zamiast po prostu nic nie robić?
     *   3. Co wypisze poniższy fragment (StubPaymentGateway zawsze true)?
     *          OrderService s = buildService(new StubPaymentGateway(true), new SpyNotifier(), new InMemoryOrderRepository());
     *          System.out.println(s.placeOrder("x@example.com", new BigDecimal("1.00")));
     *   4. ZNAJDŹ BŁĄD: ten "spy" tak naprawdę jest stubem — dlaczego?
     *          static class BadSpyNotifier implements Notifier {
     *              public void sendConfirmation(String email, String orderId) { }
     *          }
     *   5. Dlaczego fake InMemoryOrderRepository jest lepszym wyborem do testów niż podłączenie prawdziwej bazy danych?
     *   6. Co by się stało z testem "odrzucona płatność...", gdyby OrderService NAJPIERW zapisywał zamówienie,
     *      a DOPIERO POTEM sprawdzał płatność (odwrotna kolejność)?
     *   7. Dlaczego new Random() BEZ ziarna w teście łamie zasadę Repeatable z FIRST (Testing01)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");

        Check.equal("ćw. 1: stub jako lambda — zatwierdza", true, () -> approvingGateway().charge("a@example.com", BigDecimal.TEN));
        Check.equal("ćw. 2a: mock spełniony → id zamówienia", "ZAM-TEST", () -> placeOrderWithApproval("jan@example.com"));
        Check.equal("ćw. 2b: odrzucona płatność → wyjątek", true, () -> placeOrderThrowsWhenDeclined());
        Check.equal("ćw. 3: treść wysłana przez spy", List.of("celina@example.com (ZAM-TEST)"),
                () -> sentMessagesAfterOrder("celina@example.com"));
        Check.equal("ćw. 4a: mock — oczekiwanie spełnione", true, () -> verifyMockAfterOrder("a@x.com", "a@x.com"));
        Check.equal("ćw. 4b: mock — oczekiwanie NIESPEŁNIONE", false, () -> verifyMockAfterOrder("a@x.com", "b@x.com"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", true, () -> approvingGatewaySolution().charge("a@example.com", BigDecimal.TEN));
        Check.equal("ćw. 2a (wzorzec)", "ZAM-TEST", () -> placeOrderWithApprovalSolution("jan@example.com"));
        Check.equal("ćw. 2b (wzorzec)", true, () -> placeOrderThrowsWhenDeclinedSolution());
        Check.equal("ćw. 3 (wzorzec)", List.of("celina@example.com (ZAM-TEST)"),
                () -> sentMessagesAfterOrderSolution("celina@example.com"));
        Check.equal("ćw. 4a (wzorzec)", true, () -> verifyMockAfterOrderSolution("a@x.com", "a@x.com"));
        Check.equal("ćw. 4b (wzorzec)", false, () -> verifyMockAfterOrderSolution("a@x.com", "b@x.com"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): PaymentGateway to interfejs z JEDNĄ metodą — napisz go jako LAMBDĘ, która ZAWSZE
     * zatwierdza płatność (zwraca true), bez tworzenia osobnej klasy jak StubPaymentGateway.
     */
    static PaymentGateway approvingGateway() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2a (średnie): złóż zamówienie dla podanego e-maila, używając buildService z zatwierdzającym
     * stubem, spy notifierem i fake repozytorium (jak w sekcjach 4-6) — zwróć id zamówienia.
     */
    static String placeOrderWithApproval(String email) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2b (średnie): zbuduj OrderService z ODRZUCAJĄCYM stubem i spróbuj złożyć zamówienie dla
     * dowolnego e-maila. Zwróć true, jeśli placeOrder rzucił PaymentDeclinedException, false w przeciwnym razie.
     * Podpowiedź: try/catch, tak jak w exercise2NoThrow z Testing01Concepts.
     */
    static boolean placeOrderThrowsWhenDeclined() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, „PRZEPISZ”): stara wersja OrderService po prostu WYPISYWAŁA potwierdzenie na konsolę:
     * <pre>{@code
     * System.out.println("Wysłano potwierdzenie do " + email);   // nie da się tego sprawdzić w teście!
     * }</pre>
     * Przepisz podejście na testowalne: złóż jedno zamówienie dla podanego e-maila (stub zatwierdzający, fake
     * repozytorium, ŚWIEŻY SpyNotifier) i zwróć spy.sentMessages() zamiast cokolwiek wypisywać.
     */
    static List<String> sentMessagesAfterOrder(String email) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zbuduj MockPaymentGateway, zadeklaruj oczekiwanie mock.expectChargeFor(expectedEmail),
     * złóż JEDNO zamówienie dla actualEmail (stub tu niepotrzebny — płatnością steruje mock), wywołaj mock.verify().
     * Zwróć true, jeśli verify() przeszło bez wyjątku, false, jeśli rzuciło AssertionError.
     */
    static boolean verifyMockAfterOrder(String expectedEmail, String actualEmail) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static PaymentGateway approvingGatewaySolution() {
        return (customerEmail, amount) -> true;
    }

    static String placeOrderWithApprovalSolution(String email) {
        OrderService service = buildService(new StubPaymentGateway(true), new SpyNotifier(), new InMemoryOrderRepository());
        return service.placeOrder(email, new BigDecimal("10.00"));
    }

    static boolean placeOrderThrowsWhenDeclinedSolution() {
        OrderService service = buildService(new StubPaymentGateway(false), new SpyNotifier(), new InMemoryOrderRepository());
        try {
            service.placeOrder("ktoś@example.com", new BigDecimal("10.00"));
            return false;
        } catch (PaymentDeclinedException e) {
            return true;
        }
    }

    static List<String> sentMessagesAfterOrderSolution(String email) {
        SpyNotifier spy = new SpyNotifier();
        OrderService service = buildService(new StubPaymentGateway(true), spy, new InMemoryOrderRepository());
        service.placeOrder(email, new BigDecimal("10.00"));
        return spy.sentMessages();
    }

    static boolean verifyMockAfterOrderSolution(String expectedEmail, String actualEmail) {
        MockPaymentGateway mock = new MockPaymentGateway();
        mock.expectChargeFor(expectedEmail);
        OrderService service = buildService(mock, new SpyNotifier(), new InMemoryOrderRepository());
        service.placeOrder(actualEmail, new BigDecimal("10.00"));
        try {
            mock.verify();
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Stub jest bierny — sam nie ocenia niczego, to TEST później sprawdza wynik działania testowanej klasy.
     *      Mock ma wbudowaną ocenę: verify() SAM rzuca błąd, gdy oczekiwania (ustawione z góry) się nie spełniły.
     *   2. Żeby UDOWODNIĆ, że placeOrder w ogóle z niego nie korzysta — gdyby AuditLog był kiedykolwiek wywołany,
     *      test od razu by o tym poinformował (zamiast cicho przejść, mimo błędnego założenia co do zależności).
     *   3. Wypisze "ZAM-TEST" — FIXED_ID_GENERATOR zawsze zwraca tę samą stałą wartość.
     *   4. Bo NIC nie zapamiętuje (pusta metoda) — spy z definicji ZAPISUJE wywołania do późniejszego sprawdzenia;
     *      obiekt, który tylko "udaje", że coś robi, i nic nie pamięta, jest bliższy stubowi/dummy niż spy'owi.
     *   5. Prawdziwa baza wymaga uruchomienia, konfiguracji i sprzątania po teście — jest wolna i może zawieść
     *      z powodów niezwiązanych z testowaną logiką. Fake w pamięci jest szybki, prosty i w pełni pod kontrolą testu.
     *   6. Test "wyjątek i BRAK zapisu" by padł: repository.size() wynosiłoby 1 zamiast 0 — zamówienie zostałoby
     *      zapisane MIMO odrzuconej płatności, co jest realnym błędem biznesowym (pieniądze nigdy nie wpłynęły).
     *   7. Bo za każdym uruchomieniem dałby INNĄ wartość — ten sam test raz by przeszedł, raz nie, bez zmiany
     *      kodu produkcyjnego, więc nie byłby powtarzalny (Repeatable).
     */
    // </editor-fold>
}
