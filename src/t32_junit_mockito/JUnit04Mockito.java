package t32_junit_mockito;

import helpers.Check;
import helpers.TempDir;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static helpers.Console.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * <pre>
 * TEMAT: Mockito — zaślepki, atrapy i weryfikacja interakcji
 *        (mock = atrapa: sztuczny obiekt udający prawdziwą zależność)
 *
 * W SKRÓCIE:
 *   Klasa usługi zwykle korzysta z innych obiektów: bazy danych, bramki płatności, wysyłki maili.
 *   W teście jednostkowym zastępujemy je dublerami (test double = dubler testowy). Mockito tworzy takie dublery
 *   jedną linijką: każemy im zwracać dane (when/thenReturn) i sprawdzamy, co z nimi zrobiono (verify).
 *
 * ANALOGIA:
 *   Próba teatralna bez części obsady. Za nieobecnego aktora czyta suflerka — mówi tylko swoje kwestie (stub),
 *   a reżyser notuje, czy aktor główny zwrócił się do niej we właściwym momencie (mock + verify).
 *   Premiera z pełną obsadą to test integracyjny — rzadziej, drożej, ale też potrzebna.
 *
 * JAK TO DZIAŁA:
 *   1. mock(Typ.class) tworzy obiekt-podklasę (albo implementację interfejsu), w którym KAŻDA metoda nic nie robi
 *      i zwraca wartość „pustą”: null, 0, false, pusty Optional, pustą listę.
 *   2. when(mock.metoda(arg)).thenReturn(wynik) — nagrywamy odpowiedź dla danych argumentów (stubbing = zaślepianie).
 *   3. Kod produkcyjny dostaje atrapę przez konstruktor (wstrzykiwanie zależności) i woła ją jak prawdziwy obiekt.
 *   4. Atrapa zapamiętuje każde wywołanie; verify(mock).metoda(arg) sprawdza, czy było (i ile razy).
 *
 *      dubler   co robi                                              przykład w tej lekcji
 *      ------   ---------------------------------------------------  -------------------------------
 *      dummy    tylko wypełnia parametr, nigdy nie jest używany      NO_MAIL (wysyłka, której nie będzie)
 *      stub     zwraca przygotowane odpowiedzi                       FixedGateway (zawsze „TX-1”)
 *      fake     działa naprawdę, ale uproszczenie (np. mapa zamiast  InMemoryOrderRepository
 *               bazy danych)
 *      spy      prawdziwy obiekt (lub dubler), który zapisuje        RecordingMailSender, Mockito spy()
 *               wywołania do późniejszego sprawdzenia
 *      mock     dubler z oczekiwaniami sprawdzanymi przez verify     mock(MailSender.class)
 *
 * SŁÓWKA:
 *   mock = atrapa; stub = zaślepka; fake = podróbka (działająca); spy = szpieg; dummy = manekin, wypełniacz;
 *   when = kiedy; then return = wtedy zwróć; then throw = wtedy rzuć; answer = odpowiedź; verify = zweryfikuj;
 *   times = razy; never = nigdy; in order = w kolejności; matcher = dopasowywacz argumentów; captor = łapacz;
 *   inject = wstrzyknąć; strict = ścisły; unnecessary stubbing = niepotrzebne zaślepienie; gateway = bramka
 *
 * ZOBACZ TEŻ: t25_testing/Testing02TestDoubles (dublery pisane ręcznie), t22_design_patterns/Patterns08DependencyInjection
 *             (wstrzykiwanie przez konstruktor — warunek łatwych testów), t32_junit_mockito/JUnit03AssertJ (asercje),
 *             t17_datetime/DateTime01LocalDateTime (Clock), t32_junit_mockito/JUnit05Tdd
 * </pre>
 */
public class JUnit04Mockito {

    public static void main(String[] args) throws IOException {
        title("JUnit04 — Mockito: dublery testowe");

        Path pluginDir = useSubclassMockMaker();   // use subclass mock maker = użyj „podklasowego” twórcy atrap
        try {
            handWrittenDoubles();   // hand-written doubles = dublery pisane ręcznie
            stubbing();             // stubbing = zaślepianie (nagrywanie odpowiedzi)
            verifying();            // verifying = weryfikacja wywołań
            matchers();             // matchers = dopasowywacze argumentów
            captors();              // captors = łapacze argumentów
            spies();                // spies = szpiedzy
            annotationsAndStrictness(); // annotations and strictness = adnotacje i ścisłość
            fakesOverMocks();       // fakes over mocks = podróbki zamiast atrap
            exercises();            // exercises = ćwiczenia
        } finally {
            restoreClassLoader();
            TempDir.deleteRecursively(pluginDir);
        }
    }

    // =================================================================================================
    // KOD PRODUKCYJNY — usługa zamówień i jej zależności
    // =================================================================================================

    /** Zamówienie w sklepie (record = niezmienny nośnik danych). */
    record ShopOrder(String id, String email, BigDecimal amount, String status, LocalDate paidOn) {
        ShopOrder withPayment(LocalDate date) {   // with payment = z płatnością (nowa kopia)
            return new ShopOrder(id, email, amount, "OPŁACONE", date);
        }
    }

    /** Repozytorium = dostęp do zapisanych zamówień (w produkcji: baza danych). */
    interface OrderRepository {
        Optional<ShopOrder> findById(String id);   // find by id = znajdź po identyfikatorze
        void save(ShopOrder order);                // save = zapisz
    }

    /** Bramka płatności (w produkcji: zewnętrzny serwis). Zwraca identyfikator transakcji. */
    interface PaymentGateway {
        String charge(String email, BigDecimal amount);   // charge = obciąż (pobierz opłatę)
    }

    /** Wysyłka maili. */
    interface MailSender {
        void send(String to, String subject, String body);   // to = do; subject = temat; body = treść
    }

    /** Odmowa płatności (np. brak środków na karcie). */
    static class PaymentDeclinedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        PaymentDeclinedException(String reason) { super(reason); }
    }

    /** Usługa zamówień: wszystkie zależności dostaje przez konstruktor — dzięki temu łatwo podstawić dublery. */
    static class OrderService {
        private final OrderRepository repository;
        private final PaymentGateway gateway;
        private final MailSender mail;
        private final Clock clock;   // zegar wstrzyknięty — w teście ustawiamy stałą datę

        OrderService(OrderRepository repository, PaymentGateway gateway, MailSender mail, Clock clock) {
            this.repository = repository;
            this.gateway = gateway;
            this.mail = mail;
            this.clock = clock;
        }

        /** Opłaca zamówienie: pobiera pieniądze, zapisuje nowy status, wysyła potwierdzenie. Zwraca id transakcji. */
        String pay(String orderId) {
            ShopOrder order = repository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("brak zamówienia " + orderId));
            if (!order.status().equals("NOWE")) {
                throw new IllegalStateException("zamówienie " + orderId + " ma status " + order.status());
            }
            String transactionId;
            try {
                transactionId = gateway.charge(order.email(), order.amount());
            } catch (PaymentDeclinedException e) {
                mail.send(order.email(), "Płatność za " + orderId + " odrzucona", "Powód: " + e.getMessage());
                throw e;
            }
            repository.save(order.withPayment(LocalDate.now(clock)));
            mail.send(order.email(), "Zamówienie " + orderId + " opłacone",
                    "Transakcja " + transactionId + ", kwota " + order.amount() + " zł");
            return transactionId;
        }
    }

    /** Stały zegar dla testów: zawsze 2026-03-01 10:00 UTC (fixed = stały). */
    static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);

    static ShopOrder newOrder(String id) {
        return new ShopOrder(id, "ola@example.com", new BigDecimal("120.00"), "NOWE", null);
    }

    // =================================================================================================
    // 1. DUBLERY PISANE RĘCZNIE — TAKSONOMIA
    // =================================================================================================

    /** Fake: działające repozytorium w pamięci (LinkedHashMap zamiast bazy danych). */
    static class InMemoryOrderRepository implements OrderRepository {
        private final Map<String, ShopOrder> orders = new LinkedHashMap<>();

        @Override
        public Optional<ShopOrder> findById(String id) { return Optional.ofNullable(orders.get(id)); }

        @Override
        public void save(ShopOrder order) { orders.put(order.id(), order); }
    }

    /** Stub: bramka, która zawsze się zgadza i zwraca tę samą transakcję. */
    static class FixedGateway implements PaymentGateway {
        @Override
        public String charge(String email, BigDecimal amount) { return "TX-1"; }
    }

    /** Spy (ręczny): wysyłka, która tylko zapisuje tematy maili do listy. */
    static class RecordingMailSender implements MailSender {
        final List<String> subjects = new ArrayList<>();

        @Override
        public void send(String to, String subject, String body) { subjects.add(subject); }
    }

    /** Dummy: wypełnia parametr; gdyby ktoś go użył — to błąd testu. */
    static final MailSender NO_MAIL = (to, subject, body) -> {
        throw new AssertionError("ten test nie powinien wysyłać maili");
    };

    /**
     * Zanim sięgniemy po Mockito: wszystkie dublery można napisać ręcznie (t25_testing/Testing02TestDoubles).
     * Mockito oszczędza pisania, ale pojęcia zostają te same — warto je rozróżniać.
     */
    static void handWrittenDoubles() {
        section("1. Dublery pisane ręcznie — dummy, stub, fake, spy");

        runTests(HandWrittenTests.class);
        // WYNIK: ✔ brak zamówienia → wyjątek, mail niepotrzebny (dummy)
        // WYNIK: ✔ opłacenie: fake repozytorium + stub bramki + spy maili
        // WYNIK: znalezione 2, zaliczone 2, niezaliczone 0, pominięte 0, przerwane 0

        // DOBRA PRAKTYKA: ręczny dubler jest czytelny i odporny na refaktoryzację — przy prostych interfejsach
        // często lepszy niż Mockito. Mockito wygrywa, gdy interfejs ma wiele metod albo potrzebny jest verify.
    }

    static class HandWrittenTests {

        @Test
        @DisplayName("opłacenie: fake repozytorium + stub bramki + spy maili")
        void payWithHandWrittenDoubles() {
            InMemoryOrderRepository repository = new InMemoryOrderRepository();
            repository.save(newOrder("Z-1"));
            RecordingMailSender mail = new RecordingMailSender();
            OrderService service = new OrderService(repository, new FixedGateway(), mail, FIXED_CLOCK);

            String transaction = service.pay("Z-1");

            assertThat(transaction).isEqualTo("TX-1");
            assertThat(repository.findById("Z-1")).get()
                    .extracting(ShopOrder::status, ShopOrder::paidOn)
                    .containsExactly("OPŁACONE", LocalDate.of(2026, 3, 1));
            assertThat(mail.subjects).containsExactly("Zamówienie Z-1 opłacone");
        }

        @Test
        @DisplayName("brak zamówienia → wyjątek, mail niepotrzebny (dummy)")
        void missingOrder() {
            OrderService service = new OrderService(new InMemoryOrderRepository(), new FixedGateway(), NO_MAIL,
                    FIXED_CLOCK);
            assertThatThrownBy(() -> service.pay("Z-404"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("brak zamówienia Z-404");
        }
    }

    // =================================================================================================
    // 2. mock() I when/thenReturn/thenThrow/thenAnswer
    // =================================================================================================

    /**
     * Atrapa bez nagranych odpowiedzi zwraca wartości „puste”. Nagrywamy: thenReturn (wartość), thenThrow (wyjątek),
     * thenAnswer (odpowiedź liczona z argumentów). Kilka wartości w thenReturn = kolejne wywołania.
     */
    static void stubbing() {
        section("2. mock() i when(...).thenReturn / thenThrow / thenAnswer");

        OrderRepository repository = mock(OrderRepository.class);       // mock = atrapa
        PaymentGateway gateway = mock(PaymentGateway.class);
        show("findById bez nagrania", repository.findById("Z-1"));
        // WYNIK: findById bez nagrania → Optional.empty
        show("charge bez nagrania", gateway.charge("a@b.pl", BigDecimal.TEN));
        // WYNIK: charge bez nagrania → null

        when(repository.findById("Z-1")).thenReturn(Optional.of(newOrder("Z-1")));   // when = kiedy; thenReturn = zwróć
        show("findById(\"Z-1\")", repository.findById("Z-1").map(ShopOrder::status).orElse("?"));
        // WYNIK: findById("Z-1") → NOWE
        show("findById(\"Z-2\") — inny argument", repository.findById("Z-2"));
        // WYNIK: findById("Z-2") — inny argument → Optional.empty

        when(gateway.charge("ola@example.com", new BigDecimal("120.00")))
                .thenReturn("TX-1", "TX-2")                                           // 1. wywołanie, 2. i kolejne
                .thenThrow(new PaymentDeclinedException("limit dzienny"));            // ... a potem wyjątek
        show("charge #1", gateway.charge("ola@example.com", new BigDecimal("120.00")));
        // WYNIK: charge #1 → TX-1
        show("charge #2", gateway.charge("ola@example.com", new BigDecimal("120.00")));
        // WYNIK: charge #2 → TX-2
        expectThrows("charge #3", () -> gateway.charge("ola@example.com", new BigDecimal("120.00")));
        // WYNIK: ✔ charge #3 → rzucono PaymentDeclinedException: limit dzienny

        PaymentGateway echo = mock(PaymentGateway.class);
        when(echo.charge(anyString(), any())).thenAnswer(invocation ->                // thenAnswer = odpowiedz wyliczając
                "TX-" + invocation.getArgument(0, String.class).length());           // getArgument = pobierz argument
        show("thenAnswer", echo.charge("ewa@x.pl", BigDecimal.ONE));
        // WYNIK: thenAnswer → TX-8

        // PUŁAPKA: domyślne null z atrapy (np. String, BigDecimal) wybucha NullPointerException dalej w kodzie,
        // daleko od przyczyny. Gdy test pada z NPE — sprawdź, czy nagrałeś wszystkie potrzebne odpowiedzi.
        // PUŁAPKA: nagranie dotyczy DOKŁADNIE tych argumentów (porównanie przez equals). BigDecimal "120.00" ≠ "120"!
    }

    // =================================================================================================
    // 3. verify — CZY I ILE RAZY WYWOŁANO
    // =================================================================================================

    /**
     * verify(mock).metoda(argumenty) — „ta metoda została wywołana dokładnie raz, z tymi argumentami”.
     * Warianty: times(n), never(), atLeastOnce(), atMost(n); inOrder(...) — kolejność; verifyNoInteractions(mock).
     */
    static void verifying() {
        section("3. verify — times, never, inOrder");

        runTests(VerifyTests.class);
        // WYNIK: ✔ odmowa płatności: mail o odrzuceniu, zapis nigdy
        // WYNIK: ✔ opłacenie: najpierw zapis, potem mail (inOrder)
        // WYNIK: ✘ verify oczekuje 2 maili, był 1 — TooFewActualInvocations: mailSender.send(
        // WYNIK: ✔ zamówienie opłacone wcześniej: bramka i mail nietknięte
        // WYNIK: znalezione 4, zaliczone 3, niezaliczone 1, pominięte 0, przerwane 0

        // Komunikat Mockito (dalsze linie): „Wanted 2 times: -> at ... But was 1 time: -> at ...” — z numerami linii,
        // w których oczekiwano i w których faktycznie wywołano metodę. W IntelliJ te linie są klikalne.
        // DOBRA PRAKTYKA: weryfikuj EFEKTY, które mają znaczenie biznesowe (pieniądze pobrane, mail wysłany,
        // zamówienie zapisane), a nie każde wywołanie gettera. Zapytania (findById) wystarczy nagrać — jeśli kod
        // ich nie wywoła, test i tak padnie, bo nie dostanie danych.
    }

    static class VerifyTests {
        OrderRepository repository;
        PaymentGateway gateway;
        MailSender mailSender;
        OrderService service;

        @BeforeEach
        void setUp() {
            repository = mock(OrderRepository.class);
            gateway = mock(PaymentGateway.class);
            mailSender = mock(MailSender.class);
            service = new OrderService(repository, gateway, mailSender, FIXED_CLOCK);
            when(repository.findById("Z-1")).thenReturn(Optional.of(newOrder("Z-1")));
        }

        @Test
        @DisplayName("opłacenie: najpierw zapis, potem mail (inOrder)")
        void paidInOrder() {
            when(gateway.charge("ola@example.com", new BigDecimal("120.00"))).thenReturn("TX-9");

            service.pay("Z-1");

            InOrder order = inOrder(repository, mailSender);              // in order = w kolejności
            order.verify(repository).save(newOrder("Z-1").withPayment(LocalDate.of(2026, 3, 1)));
            order.verify(mailSender).send("ola@example.com", "Zamówienie Z-1 opłacone",
                    "Transakcja TX-9, kwota 120.00 zł");
        }

        @Test
        @DisplayName("odmowa płatności: mail o odrzuceniu, zapis nigdy")
        void declined() {
            when(gateway.charge(anyString(), any())).thenThrow(new PaymentDeclinedException("brak środków"));

            assertThatThrownBy(() -> service.pay("Z-1")).isInstanceOf(PaymentDeclinedException.class);

            verify(repository, never()).save(any());                      // never = nigdy
            verify(mailSender, times(1)).send("ola@example.com", "Płatność za Z-1 odrzucona", "Powód: brak środków");
        }

        @Test
        @DisplayName("zamówienie opłacone wcześniej: bramka i mail nietknięte")
        void alreadyPaid() {
            when(repository.findById("Z-2")).thenReturn(Optional.of(
                    newOrder("Z-2").withPayment(LocalDate.of(2026, 2, 1))));

            assertThatThrownBy(() -> service.pay("Z-2")).hasMessage("zamówienie Z-2 ma status OPŁACONE");

            verifyNoInteractions(gateway, mailSender);                    // żadnego wywołania
        }

        @Test
        @DisplayName("verify oczekuje 2 maili, był 1")
        void tooFew() {
            when(gateway.charge(anyString(), any())).thenReturn("TX-9");
            service.pay("Z-1");
            verify(mailSender, times(2)).send(anyString(), anyString(), anyString());   // CELOWO — porażka
        }
    }

    // =================================================================================================
    // 4. DOPASOWYWACZE ARGUMENTÓW — ZASADA „WSZYSTKIE ALBO ŻADEN”
    // =================================================================================================

    /**
     * Zamiast konkretnych wartości możemy podać dopasowywacze: any(), anyString(), eq(x), contains("tekst"),
     * argThat(warunek). Reguła: jeśli JEDEN argument jest dopasowywaczem, WSZYSTKIE muszą nimi być
     * (konkretną wartość opakowujemy w eq(...)).
     */
    static void matchers() {
        section("4. Dopasowywacze argumentów — any, eq, argThat i reguła „wszystkie albo żaden”");

        PaymentGateway gateway = mock(PaymentGateway.class);
        when(gateway.charge(anyString(), argThat(amount -> amount.compareTo(new BigDecimal("1000")) > 0)))
                .thenThrow(new PaymentDeclinedException("kwota powyżej limitu"));   // argThat = argument spełnia warunek
        when(gateway.charge(eq("vip@example.com"), any())).thenReturn("TX-VIP");    // eq = równy (dopasowywacz)

        show("vip, 50 zł", gateway.charge("vip@example.com", new BigDecimal("50")));
        // WYNIK: vip, 50 zł → TX-VIP
        show("zwykły klient, 50 zł", gateway.charge("jan@example.com", new BigDecimal("50")));
        // WYNIK: zwykły klient, 50 zł → null
        expectThrows("zwykły klient, 1500 zł", () -> gateway.charge("jan@example.com", new BigDecimal("1500")));
        // WYNIK: ✔ zwykły klient, 1500 zł → rzucono PaymentDeclinedException: kwota powyżej limitu

        // PUŁAPKA: mieszanie dopasowywacza z konkretną wartością:
        showFirstLine("anyString() + zwykła wartość", () ->
                when(gateway.charge(anyString(), new BigDecimal("10"))).thenReturn("X"));
        // WYNIK: anyString() + zwykła wartość → rzucono InvalidUseOfMatchersException: Invalid use of argument matchers!

        // Dalsze linie komunikatu: „2 matchers expected, 1 recorded” (oczekiwano 2 dopasowywaczy, zapisano 1),
        // numer linii i przykład: someMethod(any(), eq("String by matcher")). Poprawnie:
        //   when(gateway.charge(anyString(), eq(new BigDecimal("10")))).thenReturn("X");
        // Uwaga: przy kilku pasujących nagraniach wygrywa OSTATNIE — tu 1500 zł dla VIP-a dałoby „TX-VIP”.
        // PUŁAPKA: eq(new BigDecimal("10")) porównuje przez equals, więc NIE dopasuje 10.00 —
        // dla kwot bezpieczniej argThat(a -> a.compareTo(...) == 0).
    }

    /** Jak expectThrows, ale wypisuje tylko PIERWSZĄ niepustą linię komunikatu (bez numerów linii z dalszej części). */
    static void showFirstLine(String label, Runnable action) {
        try {
            action.run();
            System.out.println(label + " → brak wyjątku");
        } catch (RuntimeException e) {
            String first = String.valueOf(e.getMessage()).lines().map(String::strip)
                    .filter(l -> !l.isEmpty()).findFirst().orElse("(brak komunikatu)");
            System.out.println(label + " → rzucono " + e.getClass().getSimpleName() + ": " + first);
        }
    }

    // =================================================================================================
    // 5. ArgumentCaptor — PRZECHWYTYWANIE ARGUMENTÓW
    // =================================================================================================

    /**
     * Gdy argument jest złożonym obiektem tworzonym W ŚRODKU kodu produkcyjnego, łatwiej go „złapać” i sprawdzić
     * asercjami niż budować identyczny obiekt w teście. ArgumentCaptor (captor = łapacz) zapamiętuje argument
     * z verify; getValue() zwraca ostatni, getAllValues() — wszystkie.
     */
    static void captors() {
        section("5. ArgumentCaptor — złap argument i sprawdź jego pola");

        runTests(CaptorTests.class);
        // WYNIK: ✔ treść maila zawiera transakcję i kwotę
        // WYNIK: ✔ zapisane zamówienie ma status OPŁACONE i datę z zegara
        // WYNIK: znalezione 2, zaliczone 2, niezaliczone 0, pominięte 0, przerwane 0

        // DOBRA PRAKTYKA: captor + AssertJ = czytelne sprawdzenie wybranych pól (zamiast całego equals).
        // PUŁAPKA: captor używaj w verify, nie w when — w nagraniu zwykle wystarczy any().
        // Data „2026-03-01” jest pewna, bo usługa bierze czas z wstrzykniętego zegara (Clock.fixed), a nie z now().
        // Nie mockuj LocalDate.now() — wstrzyknij Clock (t17_datetime/DateTime01LocalDateTime).
    }

    static class CaptorTests {
        OrderRepository repository = mock(OrderRepository.class);
        PaymentGateway gateway = mock(PaymentGateway.class);
        MailSender mailSender = mock(MailSender.class);
        OrderService service = new OrderService(repository, gateway, mailSender, FIXED_CLOCK);

        @BeforeEach
        void setUp() {
            when(repository.findById("Z-7")).thenReturn(Optional.of(newOrder("Z-7")));
            when(gateway.charge(anyString(), any())).thenReturn("TX-77");
        }

        @Test
        @DisplayName("zapisane zamówienie ma status OPŁACONE i datę z zegara")
        void capturedOrder() {
            service.pay("Z-7");

            ArgumentCaptor<ShopOrder> saved = ArgumentCaptor.forClass(ShopOrder.class);   // for class = dla klasy
            verify(repository).save(saved.capture());                                       // capture = złap
            assertThat(saved.getValue().status()).isEqualTo("OPŁACONE");
            assertThat(saved.getValue().paidOn()).isEqualTo(LocalDate.of(2026, 3, 1));
        }

        @Test
        @DisplayName("treść maila zawiera transakcję i kwotę")
        void capturedMailBody() {
            service.pay("Z-7");

            ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
            verify(mailSender).send(eq("ola@example.com"), contains("opłacone"), body.capture());   // contains = zawiera
            assertThat(body.getValue()).contains("TX-77").endsWith("120.00 zł");
        }
    }

    // =================================================================================================
    // 6. spy() I doReturn
    // =================================================================================================

    /** Prawdziwa klasa z metodą „drogą” (np. pyta zewnętrzny serwis o kurs waluty). */
    static class PriceConverter {                       // price converter = przelicznik cen
        BigDecimal eurRate() {                          // eur rate = kurs euro
            throw new IllegalStateException("brak połączenia z serwisem kursów");
        }

        BigDecimal toEur(BigDecimal pln) {
            return pln.divide(eurRate(), 2, java.math.RoundingMode.HALF_UP);
        }
    }

    /**
     * spy(obiekt) opakowuje PRAWDZIWY obiekt: metody działają naprawdę, chyba że którąś podmienimy.
     * PUŁAPKA: when(spy.metoda()) WYWOŁUJE prawdziwą metodę już podczas nagrywania. Dla szpiegów używaj
     * składni doReturn(wartość).when(spy).metoda().
     */
    static void spies() {
        section("6. spy() — częściowa atrapa; doReturn zamiast when");

        PriceConverter converter = spy(new PriceConverter());   // spy = szpieg (prawdziwy obiekt pod obserwacją)
        expectThrows("when(converter.eurRate()) na szpiegu", () -> when(converter.eurRate()).thenReturn(BigDecimal.ONE));
        // WYNIK: ✔ when(converter.eurRate()) na szpiegu → rzucono IllegalStateException: brak połączenia z serwisem kursów

        doReturn(new BigDecimal("4.30")).when(converter).eurRate();   // doReturn = zwróć (bez wołania metody)
        show("toEur(43.00) — prawdziwa metoda, podmieniony kurs", converter.toEur(new BigDecimal("43.00")));
        // WYNIK: toEur(43.00) — prawdziwa metoda, podmieniony kurs → 10.00
        verify(converter, times(2)).eurRate();
        // DWA wywołania, nie jedno: pierwsze wykonało się podczas nieudanego when(converter.eurRate()) wyżej —
        // szpieg pamięta także wywołania z nagrywania. Kolejny powód, by przy szpiegach używać doReturn.

        List<String> list = spy(new ArrayList<String>());
        expectThrows("when(list.get(0)) na pustej liście-szpiegu", () -> when(list.get(0)).thenReturn("x"));
        // WYNIK: ✔ when(list.get(0)) na pustej liście-szpiegu → rzucono IndexOutOfBoundsException: Index 0 out of bounds for length 0
        doReturn("x").when(list).get(0);
        show("list.get(0) po doReturn", list.get(0));
        // WYNIK: list.get(0) po doReturn → x

        // PUŁAPKA: potrzeba szpiega to często sygnał złego projektu: klasa robi dwie rzeczy (liczy I łączy się
        // z serwisem). Lepiej wydzielić kurs do osobnego interfejsu (RateProvider) i wstrzyknąć go jak każdą zależność.
    }

    // =================================================================================================
    // 7. @Mock, @InjectMocks, MockitoExtension I ŚCISŁE ZAŚLEPKI
    // =================================================================================================

    /**
     * {@code @ExtendWith(MockitoExtension.class)} włącza integrację z JUnit 5: pola {@code @Mock} dostają świeże atrapy
     * przed KAŻDYM testem, a {@code @InjectMocks} tworzy testowany obiekt, wstrzykując atrapy przez konstruktor.
     * Rozszerzenie działa w trybie ŚCISŁYM (strict stubs): nagranie, którego test nie użył, kończy test błędem
     * UnnecessaryStubbingException — martwy kod w teście wprowadza czytelnika w błąd.
     */
    static void annotationsAndStrictness() {
        section("7. @Mock, @InjectMocks, MockitoExtension i ścisłe zaślepki");

        runTests(AnnotatedTests.class);
        // WYNIK: ✘ niepotrzebne nagranie → UnnecessaryStubbingException — UnnecessaryStubbingException: Unnecessary stubbings detected.
        // WYNIK: ✔ odmowa płatności z atrapami z adnotacji
        // WYNIK: znalezione 2, zaliczone 1, niezaliczone 1, pominięte 0, przerwane 0

        // „Unnecessary stubbings detected” = wykryto niepotrzebne nagrania. Dalsze linie: „Clean & maintainable test
        // code requires zero unnecessary code” + numer linii zbędnego when(...). Usuń nagranie albo (rzadko, świadomie)
        // oznacz je lenient().when(...) (lenient = pobłażliwy).
        // PUŁAPKA: @InjectMocks po cichu wstawia null, gdy dla parametru konstruktora nie ma atrapy. Tu Clock jest null,
        // więc w drugim teście ścieżka „opłacone” (LocalDate.now(clock)) rzuca NullPointerException — test to
        // sprawdza, żeby pokazać problem. W prawdziwym teście byłby to błąd, którego szukasz pół godziny.
        // DOBRA PRAKTYKA: wielu zespołom bardziej odpowiada jawne new OrderService(...) w @BeforeEach —
        // widać wszystkie zależności, a kompilator pilnuje zmian w konstruktorze.
    }

    @ExtendWith(MockitoExtension.class)
    static class AnnotatedTests {
        @Mock OrderRepository repository;     // świeża atrapa przed każdym testem
        @Mock PaymentGateway gateway;
        @Mock MailSender mailSender;
        @InjectMocks OrderService service;    // new OrderService(repository, gateway, mailSender, null)

        @Test
        @DisplayName("odmowa płatności z atrapami z adnotacji")
        void declined() {
            when(repository.findById("Z-3")).thenReturn(Optional.of(newOrder("Z-3")));
            when(gateway.charge(anyString(), any())).thenThrow(new PaymentDeclinedException("karta zablokowana"));

            assertThatThrownBy(() -> service.pay("Z-3")).hasMessage("karta zablokowana");
            verify(mailSender).send("ola@example.com", "Płatność za Z-3 odrzucona", "Powód: karta zablokowana");
        }

        @Test
        @DisplayName("niepotrzebne nagranie → UnnecessaryStubbingException")
        void unnecessary() {
            when(repository.findById("Z-3")).thenReturn(Optional.of(newOrder("Z-3")));
            when(gateway.charge(anyString(), any())).thenReturn("TX-1");
            when(repository.findById("Z-4")).thenReturn(Optional.empty()); // CELOWO zbędne — nikt nie pyta o Z-4

            // clock == null (patrz komentarz przy @InjectMocks) → LocalDate.now(null) rzuca NullPointerException
            assertThatThrownBy(() -> service.pay("Z-3")).isInstanceOf(NullPointerException.class);
        }
    }

    // =================================================================================================
    // 8. NADMIAR ATRAP — PODRÓBKI DLA REPOZYTORIÓW
    // =================================================================================================

    /**
     * Test z samymi atrapami sprawdza, JAK kod coś robi (które metody i w jakiej kolejności), a nie CO osiąga.
     * Po refaktoryzacji (np. zapis dwóch zmian jednym save) taki test pada, choć zachowanie jest poprawne.
     * Test z podróbką (fake) sprawdza stan końcowy — przetrwa zmianę implementacji.
     */
    static void fakesOverMocks() {
        section("8. Nadmiar atrap — podróbka repozytorium zamiast mocka");

        runTests(OverMockingTests.class);
        // WYNIK: ✔ DOBRE: podróbka repozytorium, sprawdzamy stan końcowy
        // WYNIK: ✔ ZŁE: atrapa repozytorium i sprawdzanie każdego wywołania
        // WYNIK: znalezione 2, zaliczone 2, niezaliczone 0, pominięte 0, przerwane 0

        // Oba testy są zielone — różnica wychodzi przy zmianach. Zasady:
        //   • atrapy (mock) dla granic systemu, przy których liczy się SAMO wywołanie: płatność, mail, kolejka;
        //   • podróbki (fake) dla repozytoriów i kolekcji danych — czytelniej i odporniej;
        //   • nie mockuj typów, których nie jesteś właścicielem (HttpClient, ResultSet, klasy z bibliotek): ich
        //     zachowanie znasz tylko z dokumentacji, a atrapa „kłamie” po każdej aktualizacji. Owiń je własnym
        //     interfejsem (jak PaymentGateway) i testuj integracyjnie;
        //   • nie mockuj rekordów i prostych obiektów danych — utwórz prawdziwe;
        //   • czas: wstrzyknięty Clock zamiast atrapy LocalDate.now();
        //   • więcej niż 3–4 atrapy w jednym teście = sygnał, że klasa ma za dużo zależności.
        // O ATRAPACH W TEJ LEKCJI: domyślny twórca atrap Mockito 5 („inline”) na JDK 17 podłącza w trakcie działania
        // agenta Javy, a maszyna wirtualna wypisuje wtedy ostrzeżenie na System.err. Dlatego main na początku
        // wybiera twórcę „subclass” (atrapa = wygenerowana podklasa) — tak samo jak w projekcie robi to plik
        // src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker z treścią mock-maker-subclass.
        // Koszt: subclass nie umie atrapować klas final, rekordów ani metod statycznych. Przy uruchamianiu
        // zielonym trójkątem w IntelliJ tego wyboru nie ma — ostrzeżenie może się pojawić, ale niczego nie psuje.
    }

    static class OverMockingTests {

        @Test
        @DisplayName("ZŁE: atrapa repozytorium i sprawdzanie każdego wywołania")
        void overMocked() {
            OrderRepository repository = mock(OrderRepository.class);
            PaymentGateway gateway = mock(PaymentGateway.class);
            MailSender mailSender = mock(MailSender.class);
            when(repository.findById("Z-5")).thenReturn(Optional.of(newOrder("Z-5")));
            when(gateway.charge(anyString(), any())).thenReturn("TX-5");

            new OrderService(repository, gateway, mailSender, FIXED_CLOCK).pay("Z-5");

            verify(repository, times(1)).findById("Z-5");     // szczegół implementacji — po co?
            verify(repository, times(1)).save(any());         // a co zapisano? nie wiadomo
            verify(gateway, times(1)).charge(anyString(), any());
            verify(mailSender, times(1)).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("DOBRE: podróbka repozytorium, sprawdzamy stan końcowy")
        void withFake() {
            InMemoryOrderRepository repository = new InMemoryOrderRepository();
            repository.save(newOrder("Z-5"));
            PaymentGateway gateway = mock(PaymentGateway.class);
            MailSender mailSender = mock(MailSender.class);
            when(gateway.charge("ola@example.com", new BigDecimal("120.00"))).thenReturn("TX-5");

            new OrderService(repository, gateway, mailSender, FIXED_CLOCK).pay("Z-5");

            assertThat(repository.findById("Z-5")).get().extracting(ShopOrder::status).isEqualTo("OPŁACONE");
            verify(mailSender).send(eq("ola@example.com"), eq("Zamówienie Z-5 opłacone"), anyString());
        }
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Dublery: dummy (wypełniacz), stub (odpowiedzi), fake (uproszczona działająca wersja), spy (zapisuje
     *     wywołania), mock (oczekiwania + verify).
     *   • mock(Typ.class) → metody zwracają null/0/false/Optional.empty/pustą listę.
     *   • when(m.f(x)).thenReturn(a, b).thenThrow(e); thenAnswer(inv -> ... inv.getArgument(0) ...).
     *   • verify(m).f(x); verify(m, times(2)/never()/atLeastOnce()).f(...); inOrder(m1, m2).verify(...);
     *     verifyNoInteractions(m).
     *   • Dopasowywacze: any(), anyString(), eq(x), contains("..."), argThat(warunek) — wszystkie albo żaden.
     *   • ArgumentCaptor.forClass(T.class); verify(m).f(captor.capture()); captor.getValue()/getAllValues().
     *   • spy(obiekt) — prawdziwe metody; podmiana: doReturn(x).when(spy).f() (NIE when(spy.f())).
     *   • @ExtendWith(MockitoExtension.class) + @Mock + @InjectMocks; tryb ścisły → UnnecessaryStubbingException.
     *   • Repozytoria → fake; mail/płatność → mock; czas → Clock.fixed; nie mockuj cudzych typów ani rekordów.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się stub od mocka? Podaj przykład każdego z usługi zamówień.
     *   2. Co wypisze:  OrderRepository r = mock(OrderRepository.class);  System.out.println(r.findById("A"));  ?
     *   3. ZNAJDŹ BŁĄD:  when(gateway.charge(anyString(), new BigDecimal("50"))).thenReturn("TX");
     *   4. ZNAJDŹ BŁĄD:  List<String> s = spy(new ArrayList<>());  when(s.get(0)).thenReturn("a");
     *   5. Kiedy użyjesz ArgumentCaptor zamiast podać oczekiwany obiekt w verify?
     *   6. Dlaczego repozytorium lepiej zastąpić podróbką (fake) niż atrapą (mock)?
     *   7. Co wypisze:  when(m.charge("a", ONE)).thenReturn("X", "Y");  a potem trzy razy m.charge("a", ONE)  ?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: atrapa bramki", List.of("TX-7", "TX-7", "odmowa: limit"), () -> probe(exercise1()));
        Check.equal("ćw. 2: test odmowy płatności",
                "znalezione 1, zaliczone 1, niezaliczone 0, pominięte 0, przerwane 0",
                () -> runQuietly(Exercise2Tests.class).brief());
        Check.equal("ćw. 3: temat złapany captorem", "Zamówienie Z-9 opłacone", JUnit04Mockito::exercise3);
        Check.equal("ćw. 4: PRZEPISZ na podróbkę — testy kontraktu",
                "znalezione 3, zaliczone 3, niezaliczone 0, pominięte 0, przerwane 0",
                () -> contractReport(Exercise4Repository::new));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("TX-7", "TX-7", "odmowa: limit"), () -> probe(solution1()));
        Check.equal("ćw. 2 (wzorzec)", "znalezione 1, zaliczone 1, niezaliczone 0, pominięte 0, przerwane 0",
                () -> runQuietly(Solution2Tests.class).brief());
        Check.equal("ćw. 3 (wzorzec)", "Zamówienie Z-9 opłacone", JUnit04Mockito::solution3);
        Check.equal("ćw. 4 (wzorzec)", "znalezione 3, zaliczone 3, niezaliczone 0, pominięte 0, przerwane 0",
                () -> contractReport(InMemoryOrderRepository::new));
        Check.summary();
        // WYNIK: ✔ OK    ćw. 1 (wzorzec)
        // WYNIK: ✔ OK    ćw. 2 (wzorzec)
        // WYNIK: ✔ OK    ćw. 3 (wzorzec)
        // WYNIK: ✔ OK    ćw. 4 (wzorzec)
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć atrapę PaymentGateway, która dla DOWOLNEGO e-maila i kwoty 100.00 zwraca „TX-7”,
     * a dla kwoty 5000.00 rzuca {@code new PaymentDeclinedException("limit")}.
     * Podpowiedź: when(g.charge(anyString(), eq(new BigDecimal("100.00")))).thenReturn(...); pamiętaj o regule
     * „wszystkie albo żaden”.
     */
    static PaymentGateway exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): w klasie Exercise2Tests napisz JEDEN test (zamiast todo()): zamówienie „Z-8” ze statusem
     * NOWE, bramka rzuca PaymentDeclinedException("brak środków"). Sprawdź: pay rzuca ten wyjątek, repozytorium
     * nigdy nie zapisuje, wysłano dokładnie jeden mail o temacie „Płatność za Z-8 odrzucona”.
     * Podpowiedź: wzoruj się na VerifyTests.declined(); do tematu: verify(mail).send(anyString(), eq("..."), anyString()).
     */
    static class Exercise2Tests {
        @Test
        void todo() {
            // TODO: twój test
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 3 (średnie): utwórz atrapy, nagraj zamówienie „Z-9” (newOrder("Z-9")) i dowolną transakcję, opłać
     * je przez OrderService (z FIXED_CLOCK), a potem ArgumentCaptorem złap TEMAT wysłanego maila i go zwróć.
     * Podpowiedź: ArgumentCaptor.forClass(String.class); verify(mail).send(anyString(), subject.capture(), anyString()).
     */
    static String exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): PRZEPISZ test z atrapą repozytorium na test z podróbką. Najpierw napisz podróbkę:
     * <pre>{@code
     * // PRZED — nagrywanie każdej odpowiedzi repozytorium:
     * when(repository.findById("Z-1")).thenReturn(Optional.of(order));
     * ... service.pay("Z-1") ...
     * verify(repository).save(argThat(o -> o.status().equals("OPŁACONE")));
     * // PO — działające repozytorium w pamięci:
     * repository.save(order);  ... service.pay("Z-1") ...
     * assertThat(repository.findById("Z-1")).get().extracting(ShopOrder::status).isEqualTo("OPŁACONE");
     * }</pre>
     * Uzupełnij klasę Exercise4Repository (findById, save) tak, by przeszły 3 testy kontraktu (ContractTests):
     * puste repozytorium nic nie znajduje, zapisane zamówienie da się odczytać, ponowny zapis tego samego id
     * zastępuje poprzednią wersję.
     * Podpowiedź: Map z kluczem id; Optional.ofNullable(map.get(id)).
     */
    static class Exercise4Repository implements OrderRepository {
        @Override
        public Optional<ShopOrder> findById(String id) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        public void save(ShopOrder order) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }
    }

    /** Sprawdzarka ćw. 1: dwa udane obciążenia i jedna odmowa. */
    static List<String> probe(PaymentGateway gateway) {
        List<String> results = new ArrayList<>();
        results.add(gateway.charge("ala@example.com", new BigDecimal("100.00")));
        results.add(gateway.charge("ola@example.com", new BigDecimal("100.00")));
        try {
            results.add(gateway.charge("ala@example.com", new BigDecimal("5000.00")));
        } catch (PaymentDeclinedException e) {
            results.add("odmowa: " + e.getMessage());
        }
        return results;
    }

    /** Repozytorium testowane przez ContractTests — sprawdzarka podstawia tu kolejne implementacje. */
    static Supplier<OrderRepository> repositoryUnderTest = InMemoryOrderRepository::new;

    static String contractReport(Supplier<OrderRepository> factory) {
        repositoryUnderTest = factory;
        String result = runQuietly(ContractTests.class).brief();
        repositoryUnderTest = InMemoryOrderRepository::new;
        return result;
    }

    /** Testy kontraktu (contract = umowa): każda implementacja OrderRepository musi je przejść. */
    static class ContractTests {
        OrderRepository repository = repositoryUnderTest.get();

        @Test
        @DisplayName("puste repozytorium nic nie znajduje")
        void emptyFindsNothing() { assertThat(repository.findById("Z-1")).isEmpty(); }

        @Test
        @DisplayName("zapisane zamówienie da się odczytać")
        void saveThenFind() {
            repository.save(newOrder("Z-1"));
            assertThat(repository.findById("Z-1")).contains(newOrder("Z-1"));
        }

        @Test
        @DisplayName("ponowny zapis zastępuje poprzednią wersję")
        void saveReplaces() {
            repository.save(newOrder("Z-1"));
            repository.save(newOrder("Z-1").withPayment(LocalDate.of(2026, 3, 1)));
            assertThat(repository.findById("Z-1")).get().extracting(ShopOrder::status).isEqualTo("OPŁACONE");
        }
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static PaymentGateway solution1() {
        PaymentGateway gateway = mock(PaymentGateway.class);
        when(gateway.charge(anyString(), eq(new BigDecimal("100.00")))).thenReturn("TX-7");
        when(gateway.charge(anyString(), eq(new BigDecimal("5000.00"))))
                .thenThrow(new PaymentDeclinedException("limit"));
        return gateway;
    }

    static class Solution2Tests {
        @Test
        @DisplayName("odmowa płatności: wyjątek, brak zapisu, jeden mail o odrzuceniu")
        void declined() {
            OrderRepository repository = mock(OrderRepository.class);
            PaymentGateway gateway = mock(PaymentGateway.class);
            MailSender mail = mock(MailSender.class);
            when(repository.findById("Z-8")).thenReturn(Optional.of(newOrder("Z-8")));
            when(gateway.charge(anyString(), any())).thenThrow(new PaymentDeclinedException("brak środków"));
            OrderService service = new OrderService(repository, gateway, mail, FIXED_CLOCK);

            assertThatThrownBy(() -> service.pay("Z-8"))
                    .isInstanceOf(PaymentDeclinedException.class)
                    .hasMessage("brak środków");
            verify(repository, never()).save(any());
            verify(mail, times(1)).send(anyString(), eq("Płatność za Z-8 odrzucona"), anyString());
        }
    }

    static String solution3() {
        OrderRepository repository = mock(OrderRepository.class);
        PaymentGateway gateway = mock(PaymentGateway.class);
        MailSender mail = mock(MailSender.class);
        when(repository.findById("Z-9")).thenReturn(Optional.of(newOrder("Z-9")));
        when(gateway.charge(anyString(), any())).thenReturn("TX-99");

        new OrderService(repository, gateway, mail, FIXED_CLOCK).pay("Z-9");

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        verify(mail).send(anyString(), subject.capture(), anyString());
        return subject.getValue();
    }

    // (wzorzec ćw. 4 = InMemoryOrderRepository z sekcji 1)

    // </editor-fold>

    // =================================================================================================
    // NARZĘDZIE LEKCJI 1: wybór twórcy atrap „subclass” (wyjaśnienie na końcu sekcji 8)
    // =================================================================================================

    private static ClassLoader originalLoader;   // original loader = pierwotny ładowacz klas

    /**
     * Tworzy w katalogu tymczasowym plik mockito-extensions/org.mockito.plugins.MockMaker („mock-maker-subclass”)
     * i dokłada ten katalog do ładowacza klas wątku — Mockito szuka tam swoich wtyczek przy pierwszym użyciu.
     */
    static Path useSubclassMockMaker() throws IOException {
        Path dir = TempDir.create("lekcja-mockito");
        Path file = dir.resolve("mockito-extensions").resolve("org.mockito.plugins.MockMaker");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "mock-maker-subclass");                   // writeString (Java 11+)
        originalLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[]{dir.toUri().toURL()}, originalLoader));
        return dir;
    }

    static void restoreClassLoader() throws IOException {
        ClassLoader ours = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(originalLoader);
        if (ours instanceof URLClassLoader urlLoader && ours != originalLoader) {   // wzorzec instanceof (Java 16+)
            urlLoader.close();
        }
    }

    // =================================================================================================
    // NARZĘDZIE LEKCJI 2: uruchamianie testów z main przez JUnit Platform Launcher (opis: JUnit01Basics)
    // =================================================================================================

    /** Liczniki jednego uruchomienia (found = znalezione, succeeded = zaliczone, failed = niezaliczone, ...). */
    record RunResult(long found, long succeeded, long failed, long skipped, long aborted) {
        String brief() {
            return String.format(Locale.ROOT, "znalezione %d, zaliczone %d, niezaliczone %d, pominięte %d, przerwane %d",
                    found, succeeded, failed, skipped, aborted);
        }
    }

    static RunResult runTests(Class<?> testClass) {
        return run(testClass, true);
    }

    static RunResult runQuietly(Class<?> testClass) {
        return run(testClass, false);
    }

    /** Porządek: metody i klasy {@code @Nested} alfabetycznie po nazwie wyświetlanej — stały na każdym komputerze. */
    static RunResult run(Class<?> testClass, boolean print) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(testClass))
                .configurationParameter("junit.jupiter.testmethod.order.default",
                        "org.junit.jupiter.api.MethodOrderer$DisplayName")
                .configurationParameter("junit.jupiter.testclass.order.default",
                        "org.junit.jupiter.api.ClassOrderer$DisplayName")
                .build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
        if (print) {
            launcher.execute(request, summaryListener, new PrintingListener());
        } else {
            launcher.execute(request, summaryListener);
        }
        TestExecutionSummary s = summaryListener.getSummary();
        RunResult result = new RunResult(s.getTestsFoundCount(), s.getTestsSucceededCount(), s.getTestsFailedCount(),
                s.getTestsSkippedCount(), s.getTestsAbortedCount());
        if (print) {
            System.out.println(result.brief());
        }
        return result;
    }

    /** Słuchacz wypisujący jedną linię na test — bez czasów i śladów stosu. */
    static class PrintingListener implements TestExecutionListener {
        private TestPlan plan;

        @Override
        public void testPlanExecutionStarted(TestPlan testPlan) { plan = testPlan; }

        @Override
        public void executionSkipped(TestIdentifier id, String reason) {
            System.out.println("⊘ " + path(id) + " — pominięty: " + reason);
        }

        @Override
        public void executionFinished(TestIdentifier id, TestExecutionResult result) {
            if (!id.isTest() && result.getStatus() == TestExecutionResult.Status.SUCCESSFUL) {
                return;
            }
            String name = (id.isTest() ? "" : "[kontener] ") + path(id);
            Optional<Throwable> error = result.getThrowable();
            switch (result.getStatus()) {
                case SUCCESSFUL -> System.out.println("✔ " + name);
                case ABORTED -> System.out.println("⊘ " + name + " — przerwany: " + firstLine(error));
                case FAILED -> System.out.println("✘ " + name + " — "
                        + error.map(e -> e.getClass().getSimpleName()).orElse("?") + ": " + firstLine(error));
            }
        }

        private String path(TestIdentifier id) {
            List<String> parts = new ArrayList<>();
            TestIdentifier current = id;
            while (true) {
                Optional<TestIdentifier> parent = plan.getParent(current);
                if (parent.isEmpty() || plan.getParent(parent.get()).isEmpty()) {
                    if (parts.isEmpty()) {
                        parts.add(current.getDisplayName());
                    }
                    break;
                }
                parts.add(0, current.getDisplayName());
                current = parent.get();
            }
            return String.join(" › ", parts);
        }

        private static String firstLine(Optional<Throwable> error) {
            return error.map(Throwable::getMessage)
                    .flatMap(m -> m.lines().map(String::strip).filter(l -> !l.isEmpty()).findFirst())
                    .orElse("(brak komunikatu)");
        }
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Stub tylko DOSTARCZA dane (np. bramka zawsze zwraca „TX-1”) — test sprawdza wynik kodu. Mock służy do
     *      sprawdzenia, czy kod WYWOŁAŁ zależność (np. verify(mailSender).send(...) — czy wysłano potwierdzenie).
     *   2. Optional.empty — atrapa zwraca dla Optional pusty Optional (nie null).
     *   3. Mieszanie dopasowywacza (anyString()) z konkretną wartością → InvalidUseOfMatchersException.
     *      Poprawnie: when(gateway.charge(anyString(), eq(new BigDecimal("50")))).thenReturn("TX");
     *   4. when(s.get(0)) wywołuje PRAWDZIWE get(0) na pustej liście → IndexOutOfBoundsException jeszcze przed
     *      nagraniem. Poprawnie: doReturn("a").when(s).get(0);
     *   5. Gdy argument jest tworzony wewnątrz kodu produkcyjnego (np. nowy ShopOrder z datą i statusem) i chcemy
     *      sprawdzić tylko kilka jego pól, albo gdy obiekt nie ma equals.
     *   6. Fake działa jak prawdziwe repozytorium, więc test sprawdza stan końcowy (co zapisano), a nie listę
     *      wywołań. Zmiana implementacji usługi (inne zapytania, mniej zapisów) nie psuje testu; test jest krótszy.
     *   7. X, potem Y, potem znowu Y — ostatnia wartość z thenReturn powtarza się dla kolejnych wywołań.
     */
    // </editor-fold>
}
