package t32_junit_mockito;

import helpers.Check;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static helpers.Console.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * <pre>
 * TEMAT: TDD — programowanie sterowane testami (czerwony → zielony → refaktoryzacja)
 *        (TDD = test-driven development = wytwarzanie oprogramowania sterowane testami)
 *
 * W SKRÓCIE:
 *   W TDD test piszemy PRZED kodem. Najpierw test, który pada (czerwony), potem najprostszy kod, który go
 *   spełnia (zielony), potem porządki w kodzie przy wciąż zielonych testach (refaktoryzacja). I od nowa.
 *   Ta lekcja przechodzi taki cykl krok po kroku na kasie piekarni, a potem omawia zapachy testów,
 *   pokrycie kodu i to, jak wyglądają testy w Spring Boot.
 *
 * ANALOGIA:
 *   Budowa mostu z klockami i miarką. Najpierw stawiasz wymaganie („przęsło ma utrzymać 1 kg” — test),
 *   dokładasz najmniej klocków, żeby wytrzymało (kod), a potem wymieniasz krzywe klocki na proste, za każdym
 *   razem sprawdzając obciążeniem, że nadal stoi (refaktoryzacja). Nigdy nie budujesz „na zapas”.
 *
 * JAK TO DZIAŁA:
 *      ┌──────────────┐  najprostszy kod   ┌──────────────┐  porządki, testy   ┌──────────────┐
 *      │ 1. CZERWONY  │ ─────────────────► │ 2. ZIELONY   │ ─────────────────► │ 3. REFAKTOR  │
 *      │ test pada    │                    │ test przech. │   wciąż zielone    │ czysty kod   │
 *      └──────────────┘ ◄───────────────────────────────────────────────────── └──────────────┘
 *                                 następne wymaganie = następny test
 *   • Czerwony: test MUSI najpierw paść — inaczej nie wiesz, czy w ogóle coś sprawdza.
 *   • Zielony: najprostszy kod, nawet „oszukany” (fake it — na sztywno zwrócona wartość).
 *   • Triangulacja: drugi przykład z innymi danymi zmusza do uogólnienia oszukanego kodu.
 *   • Refaktoryzacja: zmiana struktury BEZ zmiany zachowania; testy pilnują, że nic się nie zepsuło.
 *   W lekcji każdy krok to para: klasa testów StepNTests + wersja kodu PricingN. main uruchamia testy
 *   najpierw na starej wersji (✘), potem na nowej (✔).
 *
 * SŁÓWKA:
 *   red/green = czerwony/zielony; refactor = refaktoryzować (porządkować kod bez zmiany zachowania);
 *   fake it = udawaj (zwróć wartość na sztywno); triangulation = triangulacja (namierzanie z kilku punktów);
 *   test smell = zapach testu (oznaka problemu); flaky = niestabilny; coverage = pokrycie kodu;
 *   pricing = wycena; item = pozycja; total = suma; discount = rabat; kata = ćwiczenie powtarzane dla wprawy
 *
 * ZOBACZ TEŻ: t32_junit_mockito/JUnit01Basics (JUnit), t32_junit_mockito/JUnit02Parameterized (wartości brzegowe),
 *             t32_junit_mockito/JUnit04Mockito (dublery), t27_clean_code_pitfalls/CleanCode01Principles (czysty kod),
 *             t30_build_modules/Build06QualityToolsJavadoc (JaCoCo i pokrycie kodu)
 * </pre>
 */
public class JUnit05Tdd {

    public static void main(String[] args) {
        title("JUnit05 — TDD: czerwony, zielony, refaktoryzacja");

        step1EmptyCart();         // step 1 empty cart = krok 1, pusty koszyk
        step2FakeIt();            // step 2 fake it = krok 2, udawaj
        step3Triangulate();       // step 3 triangulate = krok 3, trianguluj
        step4Refactor();          // step 4 refactor = krok 4, refaktoryzuj
        step5Discount();          // step 5 discount = krok 5, rabat
        testSmells();             // test smells = zapachy testów
        coverageVsQuality();      // coverage vs quality = pokrycie a jakość
        springBootTests();        // spring boot tests = testy w Spring Boot
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // DZIEDZINA: pozycja koszyka i interfejs wyceny (kwoty w groszach, żeby nie zaciemniać przykładu)
    // =================================================================================================

    /** Pozycja koszyka: nazwa, cena jednostkowa w groszach, ilość. */
    record Item(String name, int unitPrice, int quantity) { }   // unit price = cena jednostkowa

    /** Wycena koszyka — kolejne kroki TDD to kolejne implementacje tego interfejsu. */
    interface Pricing {
        int total(List<Item> items);   // total = suma do zapłaty (w groszach)
    }

    /** Wersja aktualnie testowana — main podstawia tu kolejne wersje (pricing under test = wycena pod testem). */
    static Pricing pricingUnderTest;

    static final Item BREAD = new Item("chleb", 450, 1);      // bread = chleb, 4,50 zł
    static final Item BUTTER = new Item("masło", 799, 1);     // butter = masło, 7,99 zł

    static Item times(Item item, int quantity) {              // times = razy (ta sama pozycja, inna ilość)
        return new Item(item.name(), item.unitPrice(), quantity);
    }

    /** Uruchamia testy na starej wersji (oczekujemy ✘), potem na nowej (oczekujemy ✔). */
    static void redThenGreen(Class<?> tests, Pricing before, Pricing after) {
        note("CZERWONY — nowy test na starym kodzie:");
        pricingUnderTest = before;
        runTests(tests);
        note("ZIELONY — ten sam test na nowym kodzie:");
        pricingUnderTest = after;
        runTests(tests);
    }

    // =================================================================================================
    // 1. KROK 1 — PIERWSZY TEST: PUSTY KOSZYK
    // =================================================================================================

    /** Wersja 0: jeszcze nic nie ma. */
    static final Pricing PRICING_0 = items -> {
        throw new UnsupportedOperationException("jeszcze nie zaimplementowano");
    };

    /** Wersja 1: najprostszy kod, który spełnia test „pusty koszyk = 0”. Tak, na sztywno! */
    static final Pricing PRICING_1 = items -> 0;

    /**
     * Zaczynamy od NAJPROSTSZEGO przypadku: pusty koszyk kosztuje 0. Test piszemy, zanim powstanie kod —
     * już on wymusza decyzje projektowe: nazwa metody, typ parametru, jednostka (grosze).
     */
    static void step1EmptyCart() {
        section("1. Krok 1 — czerwony, zielony: pusty koszyk kosztuje 0");

        redThenGreen(Step1Tests.class, PRICING_0, PRICING_1);
        // WYNIK: ℹ CZERWONY — nowy test na starym kodzie:
        // WYNIK: ✘ pusty koszyk kosztuje 0 zł — UnsupportedOperationException: jeszcze nie zaimplementowano
        // WYNIK: znalezione 1, zaliczone 0, niezaliczone 1, pominięte 0, przerwane 0
        // WYNIK: ℹ ZIELONY — ten sam test na nowym kodzie:
        // WYNIK: ✔ pusty koszyk kosztuje 0 zł
        // WYNIK: znalezione 1, zaliczone 1, niezaliczone 0, pominięte 0, przerwane 0

        // „return 0” wygląda na żart, ale to celowe: kod ma robić DOKŁADNIE tyle, ile wymagają testy.
        // Nadmiarowy kod „na przyszłość” nie jest niczym sprawdzony.
        // PUŁAPKA: pominięcie fazy czerwonej. Test, który od razu przeszedł, mógł nic nie sprawdzać
        // (np. zła metoda, brak asercji) — zobacz go czerwonego choć raz.
    }

    static class Step1Tests {
        @Test
        @DisplayName("pusty koszyk kosztuje 0 zł")
        void emptyCart() {
            assertThat(pricingUnderTest.total(List.of())).isZero();
        }
    }

    // =================================================================================================
    // 2. KROK 2 — FAKE IT: JEDNA POZYCJA
    // =================================================================================================

    /** Wersja 2: „oszukana” — zwraca cenę pierwszej pozycji. Wystarcza na oba dotychczasowe testy. */
    static final Pricing PRICING_2 = items -> items.isEmpty() ? 0 : items.get(0).unitPrice();

    /**
     * Nowy test: jeden chleb kosztuje 4,50 zł. Najmniejsza zmiana, żeby przeszedł — zwrócić cenę pierwszej
     * pozycji. Wiemy, że to za mało, ale nie mamy jeszcze testu, który by to wykazał.
     */
    static void step2FakeIt() {
        section("2. Krok 2 — udawaj (fake it): jeden chleb");

        redThenGreen(Step2Tests.class, PRICING_1, PRICING_2);
        // WYNIK: ℹ CZERWONY — nowy test na starym kodzie:
        // WYNIK: ✘ jeden chleb kosztuje 4,50 zł — AssertionFailedError: expected: 450
        // WYNIK: ✔ pusty koszyk kosztuje 0 zł
        // WYNIK: znalezione 2, zaliczone 1, niezaliczone 1, pominięte 0, przerwane 0
        // WYNIK: ℹ ZIELONY — ten sam test na nowym kodzie:
        // WYNIK: ✔ jeden chleb kosztuje 4,50 zł
        // WYNIK: ✔ pusty koszyk kosztuje 0 zł
        // WYNIK: znalezione 2, zaliczone 2, niezaliczone 0, pominięte 0, przerwane 0

        // Step2Tests zawiera też test z kroku 1 — testy się kumulują i pilnują wszystkiego, co już działa.
        // DOBRA PRAKTYKA: małe kroki. Gdy test pada, wiesz, że winna jest ostatnia, kilkulinijkowa zmiana.
    }

    static class Step2Tests extends Step1Tests {    // dziedziczy test pustego koszyka
        @Test
        @DisplayName("jeden chleb kosztuje 4,50 zł")
        void oneBread() {
            assertThat(pricingUnderTest.total(List.of(BREAD))).isEqualTo(450);
        }
    }

    // =================================================================================================
    // 3. KROK 3 — TRIANGULACJA
    // =================================================================================================

    /** Wersja 3: uogólniona — suma cena × ilość w pętli. */
    static final Pricing PRICING_3 = items -> {
        int sum = 0;
        for (Item item : items) {
            sum = sum + item.unitPrice() * item.quantity();
        }
        return sum;
    };

    /**
     * Triangulacja: drugi, INNY przykład (dwa chleby i masło) obala „oszukaną” wersję i wymusza prawdziwy algorytm.
     * Jak w nawigacji — z dwóch punktów da się wyznaczyć położenie, z jednego nie.
     */
    static void step3Triangulate() {
        section("3. Krok 3 — triangulacja: drugi przykład wymusza uogólnienie");

        redThenGreen(Step3Tests.class, PRICING_2, PRICING_3);
        // WYNIK: ℹ CZERWONY — nowy test na starym kodzie:
        // WYNIK: ✘ 2 chleby i masło kosztują 16,99 zł — AssertionFailedError: expected: 1699
        // WYNIK: ✔ jeden chleb kosztuje 4,50 zł
        // WYNIK: ✔ pusty koszyk kosztuje 0 zł
        // WYNIK: znalezione 3, zaliczone 2, niezaliczone 1, pominięte 0, przerwane 0
        // WYNIK: ℹ ZIELONY — ten sam test na nowym kodzie:
        // WYNIK: ✔ 2 chleby i masło kosztują 16,99 zł
        // WYNIK: ✔ jeden chleb kosztuje 4,50 zł
        // WYNIK: ✔ pusty koszyk kosztuje 0 zł
        // WYNIK: znalezione 3, zaliczone 3, niezaliczone 0, pominięte 0, przerwane 0

        // „expected: 1699” — a w drugiej linii komunikatu „but was: 450”: oszukana wersja wzięła tylko pierwszą pozycję.
        // DOBRA PRAKTYKA: gdy prawdziwe rozwiązanie jest oczywiste, możesz je od razu napisać („obvious
        // implementation”). Udawanie i triangulacja są na chwile, gdy nie masz pewności, jak kod powinien wyglądać.
    }

    static class Step3Tests extends Step2Tests {
        @Test
        @DisplayName("2 chleby i masło kosztują 16,99 zł")
        void twoBreadsAndButter() {
            assertThat(pricingUnderTest.total(List.of(times(BREAD, 2), BUTTER))).isEqualTo(1699);
        }
    }

    // =================================================================================================
    // 4. KROK 4 — REFAKTORYZACJA POD OSŁONĄ TESTÓW
    // =================================================================================================

    /** Wersja 4: to samo zachowanie, czytelniejszy zapis (strumień) i metoda pomocnicza o dobrej nazwie. */
    static final Pricing PRICING_4 = items -> items.stream().mapToInt(JUnit05Tdd::lineTotal).sum();

    static int lineTotal(Item item) {   // line total = wartość pozycji
        return item.unitPrice() * item.quantity();
    }

    /**
     * Refaktoryzacja = zmiana struktury bez zmiany zachowania. NIE dodajemy wtedy nowych funkcji i NIE piszemy
     * nowych testów — uruchamiamy stare. Wszystkie zielone → refaktoryzacja bezpieczna.
     */
    static void step4Refactor() {
        section("4. Krok 4 — refaktoryzacja: inny kod, te same zielone testy");

        pricingUnderTest = PRICING_4;
        runTests(Step3Tests.class);
        // WYNIK: ✔ 2 chleby i masło kosztują 16,99 zł
        // WYNIK: ✔ jeden chleb kosztuje 4,50 zł
        // WYNIK: ✔ pusty koszyk kosztuje 0 zł
        // WYNIK: znalezione 3, zaliczone 3, niezaliczone 0, pominięte 0, przerwane 0

        // Refaktoryzujemy też TESTY: stała BREAD i metoda times(...) powstały, gdy w testach pojawiło się powtórzenie.
        // PUŁAPKA: refaktoryzacja na czerwonych testach — nie wiesz, czy psujesz, czy naprawiasz. Najpierw zielono.
        // DOBRA PRAKTYKA: w IntelliJ refaktoryzuj narzędziami (Shift+F6 zmiana nazwy, Ctrl+Alt+M wydziel metodę)
        // i uruchamiaj testy po każdym kroku (Ctrl+F5 = uruchom ponownie ostatnią konfigurację).
    }

    // =================================================================================================
    // 5. KROK 5 — NOWA REGUŁA: RABAT OD 100 ZŁ
    // =================================================================================================

    /** Wersja 5: od 100 zł (10000 gr) rabat 10%. Rabat zaokrąglamy w dół do pełnego grosza (dzielenie całkowite). */
    static final Pricing PRICING_5 = items -> {
        int sum = items.stream().mapToInt(JUnit05Tdd::lineTotal).sum();
        return sum >= 10_000 ? sum - sum / 10 : sum;
    };

    /**
     * Wymaganie: „od 100 zł rabat 10%”. Każde słowo to pytanie do testu: czy dokładnie 100 zł już się łapie?
     * Testy z wartościami brzegowymi (99,99 / 100,00) odpowiadają na nie jednoznacznie — test to też dokumentacja.
     */
    static void step5Discount() {
        section("5. Krok 5 — nowa reguła: rabat 10% od 100 zł (wartości brzegowe)");

        redThenGreen(Step5Tests.class, PRICING_4, PRICING_5);
        // WYNIK: ℹ CZERWONY — nowy test na starym kodzie:
        // WYNIK: ✔ 2 chleby i masło kosztują 16,99 zł
        // WYNIK: ✔ jeden chleb kosztuje 4,50 zł
        // WYNIK: ✔ pusty koszyk kosztuje 0 zł
        // WYNIK: ✔ rabat od 100 zł › 9999 gr → 9999 gr
        // WYNIK: ✘ rabat od 100 zł › 10000 gr → 9000 gr — AssertionFailedError: expected: 9000
        // WYNIK: ✘ rabat od 100 zł › 11250 gr → 10125 gr — AssertionFailedError: expected: 10125
        // WYNIK: znalezione 6, zaliczone 4, niezaliczone 2, pominięte 0, przerwane 0
        // WYNIK: ℹ ZIELONY — ten sam test na nowym kodzie:
        // WYNIK: ✔ 2 chleby i masło kosztują 16,99 zł
        // WYNIK: ✔ jeden chleb kosztuje 4,50 zł
        // WYNIK: ✔ pusty koszyk kosztuje 0 zł
        // WYNIK: ✔ rabat od 100 zł › 9999 gr → 9999 gr
        // WYNIK: ✔ rabat od 100 zł › 10000 gr → 9000 gr
        // WYNIK: ✔ rabat od 100 zł › 11250 gr → 10125 gr
        // WYNIK: znalezione 6, zaliczone 6, niezaliczone 0, pominięte 0, przerwane 0

        // Przypadek 9999 gr przeszedł już na starym kodzie — to normalne: test brzegowy „poniżej progu” chroni
        // przed błędem w PRZYSZŁOŚCI (np. ktoś zmieni >= na > albo próg na 99 zł).
        // PUŁAPKA: rabat 10% od 10001 gr to 1000,1 gr — musisz zdecydować o zaokrągleniu i zapisać to testem.
        // Tu: dzielenie całkowite obcina część ułamkową, więc rabat = 1000 gr, a do zapłaty 9001 gr.
    }

    static class Step5Tests extends Step3Tests {
        @ParameterizedTest(name = "{0} gr → {1} gr")
        @CsvSource({"9999, 9999", "10000, 9000", "11250, 10125"})
        @DisplayName("rabat od 100 zł")
        void discountFrom100(int cartValue, int expected) {
            // koszyk o zadanej wartości: jedna pozycja z ceną = wartość, ilość 1
            assertThat(pricingUnderTest.total(List.of(new Item("zakupy", cartValue, 1)))).isEqualTo(expected);
        }
    }

    // =================================================================================================
    // 6. NAZWY TESTÓW I ZAPACHY TESTÓW
    // =================================================================================================

    /** Wersja z błędem: rabat 20% zamiast 10%. Który test to wyłapie? */
    static final Pricing PRICING_BUGGY = items -> {
        int sum = items.stream().mapToInt(JUnit05Tdd::lineTotal).sum();
        return sum >= 10_000 ? sum - sum / 5 : sum;     // BŁĄD: 20%
    };

    /**
     * Zapach testu (test smell) = cecha, która nie psuje testu od razu, ale czyni go mało wartościowym.
     * Najgroźniejszy: LOGIKA W TEŚCIE — test liczy oczekiwany wynik tym samym wzorem co kod produkcyjny,
     * więc powtarza jego błędy. Pokazujemy to na wersji z błędnym rabatem 20%.
     */
    static void testSmells() {
        section("6. Zapachy testów — logika w teście, za dużo atrap, niestabilny czas");

        pricingUnderTest = PRICING_BUGGY;
        runTests(SmellyVsClearTests.class);
        // WYNIK: ✘ DOBRZE: dokładna wartość z wymagań — 200 zł → 180 zł — AssertionFailedError: expected: 18000
        // WYNIK: ✔ ŹLE: oczekiwany wynik liczony w teście (logika w teście)
        // WYNIK: znalezione 2, zaliczone 1, niezaliczone 1, pominięte 0, przerwane 0
        pricingUnderTest = PRICING_5;

        // Test z logiką jest ZIELONY mimo błędu — jego autor „zaktualizował” wzór razem z kodem.
        // Test z liczbą wpisaną na sztywno (18000 = wartość wyliczona ręcznie z wymagań) wyłapał błąd.
        // Inne zapachy:
        //   • za dużo atrap — test zna każde wywołanie w środku kodu; pada po każdej refaktoryzacji (JUnit04Mockito);
        //   • niestabilny czas — LocalDate.now() / Thread.sleep w teście: wynik zależy od dnia i szybkości komputera;
        //     lekarstwo: wstrzyknięty Clock.fixed i brak czekania „na wszelki wypadek”;
        //   • wiele niezwiązanych sprawdzeń w jednym teście — nazwa nie mówi, co padło;
        //   • testy zależne od kolejności lub wspólnego stanu static (JUnit01Basics, sekcja 7);
        //   • test bez asercji albo z try/catch połykającym wyjątek — zawsze zielony.
        // DOBRA PRAKTYKA: w nazwach testów opisuj zachowanie i warunek, nie metodę: „rabat od 100 zł › 10000 gr → 9000 gr”,
        // a nie „testTotal2”. Z samych nazw testów powinna dać się odtworzyć specyfikacja.
    }

    static class SmellyVsClearTests {
        @Test
        @DisplayName("ŹLE: oczekiwany wynik liczony w teście (logika w teście)")
        void smelly() {
            List<Item> items = List.of(new Item("tort", 20_000, 1));
            int expected = 0;
            for (Item item : items) {                       // kopia algorytmu z kodu produkcyjnego...
                expected += item.unitPrice() * item.quantity();
            }
            if (expected >= 10_000) {
                expected = expected - expected / 5;         // ...razem z jego błędem
            }
            assertThat(pricingUnderTest.total(items)).isEqualTo(expected);
        }

        @Test
        @DisplayName("DOBRZE: dokładna wartość z wymagań — 200 zł → 180 zł")
        void clear() {
            assertThat(pricingUnderTest.total(List.of(new Item("tort", 20_000, 1)))).isEqualTo(18_000);
        }
    }

    // =================================================================================================
    // 7. POKRYCIE KODU A JAKOŚĆ TESTÓW
    // =================================================================================================

    /**
     * Pokrycie kodu (code coverage, narzędzie JaCoCo — t30_build_modules/Build06QualityToolsJavadoc) mówi, które
     * linie WYKONAŁY się podczas testów. Nie mówi, czy cokolwiek SPRAWDZONO. Poniższy test wykonuje każdą linię
     * PRICING_5 (100% pokrycia), ale nie ma asercji — przechodzi także na zepsutej wersji.
     */
    static void coverageVsQuality() {
        section("7. Pokrycie kodu a jakość — 100% linii, zero sprawdzeń");

        pricingUnderTest = PRICING_BUGGY;
        runTests(CoverageOnlyTests.class);
        // WYNIK: ✔ wywołuje obie gałęzie rabatu (bez asercji!)
        // WYNIK: znalezione 1, zaliczone 1, niezaliczone 0, pominięte 0, przerwane 0
        pricingUnderTest = PRICING_5;

        // Zielony test na zepsutym kodzie, a raport pokrycia pokazałby 100%. Pokrycie to dobry wskaźnik tego,
        // czego NIE testujesz (0% = nikt tego kodu nie uruchomił), ale słaby wskaźnik jakości testów.
        // DOBRA PRAKTYKA: zamiast ścigać 100%, sprawdzaj, czy testy łapią błędy: zepsuj kod i patrz, czy coś
        // zaczerwieni się (testy mutacyjne, np. narzędzie PIT, robią to automatycznie — jak sprawdzarki ćwiczeń
        // w JUnit01Basics i JUnit02Parameterized). TDD daje wysokie pokrycie „przy okazji”, bo kod powstaje tylko
        // po to, by spełnić test.
    }

    static class CoverageOnlyTests {
        @Test
        @DisplayName("wywołuje obie gałęzie rabatu (bez asercji!)")
        void touchesEverything() {
            pricingUnderTest.total(List.of(new Item("bułka", 100, 1)));      // gałąź „bez rabatu”
            pricingUnderTest.total(List.of(new Item("tort", 20_000, 1)));    // gałąź „z rabatem”
        }
    }

    // =================================================================================================
    // 8. JAK WYGLĄDAJĄ TESTY W SPRING BOOT
    // =================================================================================================

    /**
     * Zapowiedź kursu SpringLearning. Spring Boot używa tego samego JUnit 5, AssertJ i Mockito
     * (zależność spring-boot-starter-test), dokłada tylko adnotacje, które uruchamiają kawałek aplikacji:
     * <pre>{@code
     * @SpringBootTest                         // cała aplikacja (wszystkie beany) — wolne, test integracyjny
     * class ShopApplicationTests {
     *     @Autowired OrderService service;    // prawdziwy bean z kontekstu Springa
     *     @Test void contextLoads() { assertThat(service).isNotNull(); }
     * }
     *
     * @WebMvcTest(OrderController.class)      // tylko warstwa webowa: kontroler + MockMvc, bez bazy
     * class OrderControllerTest {
     *     @Autowired MockMvc mockMvc;         // udawane żądania HTTP, bez prawdziwego serwera
     *     @MockitoBean OrderService service;  // atrapa Mockito w kontekście Springa (starsze wersje: @MockBean)
     *
     *     @Test void returnsOrder() throws Exception {
     *         when(service.find("Z-1")).thenReturn(new OrderDto("Z-1", "NOWE"));
     *         mockMvc.perform(get("/api/orders/Z-1"))
     *                .andExpect(status().isOk())
     *                .andExpect(jsonPath("$.status").value("NOWE"));
     *     }
     * }
     * }</pre>
     * Do tego {@code @DataJpaTest} (tylko repozytoria JPA z bazą testową) i Testcontainers (prawdziwa baza w Dockerze).
     */
    static void springBootTests() {
        section("8. Testy w Spring Boot — zapowiedź (szczegóły w kursie SpringLearning)");

        showEach("piramida testów (od dołu: dużo, szybkie → na górze: mało, wolne)", List.of(
                "jednostkowe: JUnit + AssertJ + Mockito, milisekundy (ten dział)",
                "wycinkowe Springa: @WebMvcTest, @DataJpaTest — fragment aplikacji, sekundy",
                "integracyjne: @SpringBootTest, Testcontainers — cała aplikacja, dziesiątki sekund",
                "end-to-end: przeglądarka/klient HTTP na wdrożonej aplikacji — minuty"));
        // WYNIK: piramida testów (od dołu: dużo, szybkie → na górze: mało, wolne) (liczba elementów: 4):
        // WYNIK: • jednostkowe: JUnit + AssertJ + Mockito, milisekundy (ten dział)
        // WYNIK: • wycinkowe Springa: @WebMvcTest, @DataJpaTest — fragment aplikacji, sekundy
        // WYNIK: • integracyjne: @SpringBootTest, Testcontainers — cała aplikacja, dziesiątki sekund
        // WYNIK: • end-to-end: przeglądarka/klient HTTP na wdrożonej aplikacji — minuty

        // DOBRA PRAKTYKA: logikę biznesową trzymaj w zwykłych klasach z zależnościami w konstruktorze — wtedy
        // testujesz ją jak w tym dziale, bez uruchamiania Springa. @SpringBootTest zostaw dla kilku testów „czy
        // wszystko się ze sobą łączy”.
        // PUŁAPKA: @SpringBootTest w każdym teście = zestaw testów trwający minuty, więc nikt go nie uruchamia.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Cykl TDD: CZERWONY (test pada) → ZIELONY (najprostszy kod) → REFAKTOR (porządki przy zielonych testach).
     *   • Zaczynaj od najprostszego przypadku (pusty koszyk), potem kolejne małe kroki.
     *   • Fake it: zwróć wartość na sztywno; triangulacja: drugi przykład wymusza uogólnienie.
     *   • Refaktoryzacja: zmiana struktury, nie zachowania; tylko przy zielonych testach; refaktoryzuj też testy.
     *   • Wymaganie z progiem → testy po obu stronach progu; decyzje (zaokrąglenia) zapisuj testem.
     *   • Zapachy: logika w teście, za dużo atrap, now()/sleep, wiele niezwiązanych sprawdzeń, zależność od kolejności,
     *     brak asercji. Oczekiwane wartości wpisuj na sztywno (policzone z wymagań, nie z kodu).
     *   • Pokrycie kodu mówi, co się wykonało, nie co sprawdzono; sprawdzaj testy mutacjami (zepsuj kod → czerwono?).
     *   • Spring Boot: te same JUnit/AssertJ/Mockito + @SpringBootTest, @WebMvcTest + MockMvc, @MockitoBean, @DataJpaTest.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego w TDD test musi najpierw paść? Co by było, gdyby od razu przeszedł?
     *   2. Co wypisze krok „CZERWONY”, jeśli PRICING_1 (items -> 0) sprawdzimy testem
     *      assertThat(pricing.total(List.of(BREAD))).isEqualTo(450)?  (pierwsza linia komunikatu)
     *   3. Czym jest triangulacja i jaki problem rozwiązuje?
     *   4. ZNAJDŹ BŁĄD (zapach):
     *        @Test void total() {
     *            int expected = items.stream().mapToInt(i -> i.unitPrice() * i.quantity()).sum();
     *            assertThat(pricing.total(items)).isEqualTo(expected);
     *        }
     *   5. Czy 100% pokrycia kodu oznacza, że kod jest dobrze przetestowany? Uzasadnij.
     *   6. ZNAJDŹ BŁĄD:  @Test void discountToday() { assertThat(promo.isActive(LocalDate.now())).isTrue(); }
     *   7. Co wolno, a czego nie wolno robić w fazie refaktoryzacji?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: ZIELONY — kod spełnia gotowe testy",
                "znalezione 8, zaliczone 8, niezaliczone 0, pominięte 0, przerwane 0",
                () -> runWith(Exercise1Spec.class, JUnit05Tdd::exercise1));
        Check.equal("ćw. 2: CZERWONY — testy wyłapują mutanty",
                "poprawna wersja: bez błędów; wykryte mutanty: 3 z 3", () -> mutationReport(Exercise2Tests.class));
        Check.equal("ćw. 3: PRZEPISZ test z logiką na wartości z wymagań",
                "znalezione 3, zaliczone 3, niezaliczone 0, pominięte 0, przerwane 0",
                () -> runWith(Exercise3Tests.class, PRICING_5));
        Check.equal("ćw. 4: grosze jako tekst w złotych",
                List.of("0,00 zł", "0,05 zł", "12,50 zł", "1 234,00 zł", "-3,10 zł"),
                () -> AMOUNTS.stream().map(JUnit05Tdd::exercise4).toList());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "znalezione 8, zaliczone 8, niezaliczone 0, pominięte 0, przerwane 0",
                () -> runWith(Exercise1Spec.class, JUnit05Tdd::solution1));
        Check.equal("ćw. 2 (wzorzec)", "poprawna wersja: bez błędów; wykryte mutanty: 3 z 3",
                () -> mutationReport(Solution2Tests.class));
        Check.equal("ćw. 3 (wzorzec)", "znalezione 3, zaliczone 3, niezaliczone 0, pominięte 0, przerwane 0",
                () -> runWith(Solution3Tests.class, PRICING_5));
        Check.equal("ćw. 4 (wzorzec)", List.of("0,00 zł", "0,05 zł", "12,50 zł", "1 234,00 zł", "-3,10 zł"),
                () -> AMOUNTS.stream().map(JUnit05Tdd::solution4).toList());
        Check.summary();
        // WYNIK: ✔ OK    ćw. 1 (wzorzec)
        // WYNIK: ✔ OK    ćw. 2 (wzorzec)
        // WYNIK: ✔ OK    ćw. 3 (wzorzec)
        // WYNIK: ✔ OK    ćw. 4 (wzorzec)
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** Uruchamia testy po cichu na podanej wersji wyceny. */
    static String runWith(Class<?> tests, Pricing pricing) {
        pricingUnderTest = pricing;
        String result = runQuietly(tests).brief();
        pricingUnderTest = PRICING_5;
        return result;
    }

    /**
     * ĆWICZENIE 1 (łatwe): faza ZIELONA. Testy już są (Exercise1Spec: wszystkie z kroków 1–5 + dwa nowe):
     * ujemna ilość albo ujemna cena → IllegalArgumentException z komunikatem zawierającym nazwę produktu.
     * Napisz kod, który je spełni (wolno skopiować PRICING_5 i dopisać sprawdzenie).
     * Podpowiedź: {@code for (Item i : items) if (i.quantity() < 0 || ...) throw new IllegalArgumentException("... " + i.name());}
     */
    static int exercise1(List<Item> items) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Gotowe testy do ćw. 1 (specyfikacja = spec). */
    static class Exercise1Spec extends Step5Tests {
        @Test
        @DisplayName("ujemna ilość → wyjątek z nazwą produktu")
        void negativeQuantity() {
            assertThatThrownBy(() -> pricingUnderTest.total(List.of(times(BREAD, -1))))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("chleb");
        }

        @Test
        @DisplayName("ujemna cena → wyjątek z nazwą produktu")
        void negativePrice() {
            assertThatThrownBy(() -> pricingUnderTest.total(List.of(new Item("masło", -1, 1))))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("masło");
        }
    }

    /**
     * ĆWICZENIE 2 (średnie): faza CZERWONA. Napisz testy reguły rabatu (wołaj {@code pricingUnderTest.total(...)}).
     * Sprawdzarka uruchomi je na PRICING_5 (muszą przejść) i na 3 mutantach: próg {@code > 10000} zamiast {@code >= 10000},
     * rabat 20% zamiast 10% oraz rabat naliczany zawsze, także poniżej progu. Każdy mutant musi oblać jakiś test.
     * Podpowiedź: wystarczą trzy przypadki — jeden poniżej progu, jeden dokładnie na progu, jeden wyraźnie powyżej.
     */
    static class Exercise2Tests {
        @Test
        void todo() {
            // TODO: twoje testy (pricingUnderTest.total(...))
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ test z logiką na {@code @ParameterizedTest} z wartościami policzonymi ręcznie
     * (DOKŁADNIE 3 wywołania: 3 bułki po 0,80 zł; 30 chlebów po 4,50 zł; 1 tort za 100,00 zł):
     * <pre>{@code
     * @Test void totals() {
     *     for (Item item : List.of(new Item("bułka", 80, 3), new Item("chleb", 450, 30), new Item("tort", 10000, 1))) {
     *         int expected = item.unitPrice() * item.quantity();
     *         if (expected >= 10000) expected = expected - expected / 10;
     *         assertThat(pricingUnderTest.total(List.of(item))).isEqualTo(expected);
     *     }
     * }
     * }</pre>
     * Podpowiedź: @CsvSource({"bułka, 80, 3, 240", ...}) — ostatnia kolumna to wynik policzony na kartce.
     */
    static class Exercise3Tests {
        @Test
        void todo() {
            // TODO: twój @ParameterizedTest
            throw new UnsupportedOperationException("TODO");
        }
    }

    /** Dane do ćw. 4 (w groszach). */
    private static final List<Integer> AMOUNTS = List.of(0, 5, 1250, 123_400, -310);

    /**
     * ĆWICZENIE 4 (trudniejsze): metodą TDD (najpierw test dla 0, potem 5, potem 1250 ...) napisz formatowanie
     * kwoty w groszach: „12,50 zł”, grosze zawsze dwiema cyframi, tysiące oddzielone spacją („1 234,00 zł”),
     * minus przed liczbą („-3,10 zł”). Bez NumberFormat — tylko dzielenie, reszta i String.format.
     * Podpowiedź: Math.abs, złote = abs / 100, grosze = abs % 100; String.format(Locale.ROOT, "%,d", zlote)
     * daje „1,234” — zamień przecinek na spację; „%02d” dopełnia grosze zerem.
     */
    static String exercise4(int grosze) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Mutanty reguły rabatu do ćw. 2. */
    private static final List<Pricing> MUTANTS = List.of(
            items -> { int s = items.stream().mapToInt(JUnit05Tdd::lineTotal).sum(); return s > 10_000 ? s - s / 10 : s; },
            items -> { int s = items.stream().mapToInt(JUnit05Tdd::lineTotal).sum(); return s >= 10_000 ? s - s / 5 : s; },
            items -> { int s = items.stream().mapToInt(JUnit05Tdd::lineTotal).sum(); return s - s / 10; });

    static String mutationReport(Class<?> testClass) {
        pricingUnderTest = PRICING_5;
        RunResult onCorrect = runQuietly(testClass);
        int killed = 0;
        for (Pricing mutant : MUTANTS) {
            pricingUnderTest = mutant;
            if (runQuietly(testClass).failed() > 0) {
                killed++;
            }
        }
        pricingUnderTest = PRICING_5;
        String correct = onCorrect.failed() == 0 && onCorrect.succeeded() > 0
                ? "bez błędów" : "niezaliczone " + onCorrect.failed() + " z " + onCorrect.found();
        return "poprawna wersja: " + correct + "; wykryte mutanty: " + killed + " z " + MUTANTS.size();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(List<Item> items) {
        for (Item item : items) {
            if (item.quantity() < 0 || item.unitPrice() < 0) {
                throw new IllegalArgumentException("ujemna ilość lub cena: " + item.name());
            }
        }
        return PRICING_5.total(items);
    }

    static class Solution2Tests {
        @ParameterizedTest(name = "{0} gr → {1} gr")
        @CsvSource({"9999, 9999", "10000, 9000", "20000, 18000"})
        @DisplayName("rabat 10% od 100 zł")
        void discount(int cartValue, int expected) {
            assertThat(pricingUnderTest.total(List.of(new Item("zakupy", cartValue, 1)))).isEqualTo(expected);
        }
    }

    static class Solution3Tests {
        @ParameterizedTest(name = "{2} × {0} po {1} gr → {3} gr")
        @CsvSource({"bułka, 80, 3, 240", "chleb, 450, 30, 12150", "tort, 10000, 1, 9000"})
        @DisplayName("suma z rabatem")
        void totals(String name, int unitPrice, int quantity, int expected) {
            assertThat(pricingUnderTest.total(List.of(new Item(name, unitPrice, quantity)))).isEqualTo(expected);
        }
    }

    static String solution4(int grosze) {
        int abs = Math.abs(grosze);
        String zlote = String.format(Locale.ROOT, "%,d", abs / 100).replace(',', ' ');
        return (grosze < 0 ? "-" : "") + zlote + "," + String.format(Locale.ROOT, "%02d", abs % 100) + " zł";
    }

    // </editor-fold>

    // =================================================================================================
    // NARZĘDZIE LEKCJI: uruchamianie testów z main przez JUnit Platform Launcher (opis: JUnit01Basics)
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

    /** Porządek: metody (także odziedziczone) alfabetycznie po nazwie wyświetlanej — stały na każdym komputerze. */
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
     *   1. Czerwony test dowodzi, że test w ogóle coś sprawdza i że brakująca funkcja naprawdę nie istnieje.
     *      Test zielony od początku mógł być źle napisany (brak asercji, zła metoda, zły obiekt) — fałszywe
     *      poczucie bezpieczeństwa.
     *   2. ✘ ... — AssertionFailedError: expected: 450   (druga linia komunikatu: „but was: 0”).
     *   3. Triangulacja = dodanie drugiego (trzeciego) przykładu z innymi danymi, który obala uproszczoną, „oszukaną”
     *      implementację i wymusza ogólny algorytm. Chroni przed kodem, który działa tylko dla jednego przypadku.
     *   4. Logika w teście: oczekiwany wynik liczony tym samym wzorem co kod produkcyjny — błąd we wzorze będzie
     *      w obu miejscach i test go nie wykryje. Poprawnie: wartość policzona ręcznie z wymagań, np. isEqualTo(1699).
     *   5. Nie. Pokrycie mówi tylko, że linie się wykonały. Test bez asercji (sekcja 7) daje 100% pokrycia i nic
     *      nie sprawdza. Jakość testów lepiej mierzyć tym, czy wyłapują celowo wprowadzone błędy (mutacje).
     *   6. Test zależy od daty uruchomienia (niestabilny): dziś przejdzie, za miesiąc padnie. Poprawnie: podać
     *      konkretną datę, np. promo.isActive(LocalDate.of(2026, 3, 1)), albo wstrzyknąć Clock.fixed(...).
     *   7. Wolno zmieniać strukturę: nazwy, wydzielać metody/klasy, usuwać powtórzenia (w kodzie i w testach).
     *      Nie wolno zmieniać zachowania ani dodawać nowych funkcji; nie refaktoryzuje się przy czerwonych testach.
     */
    // </editor-fold>
}
