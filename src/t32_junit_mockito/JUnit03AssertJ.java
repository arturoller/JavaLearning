package t32_junit_mockito;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Customer;
import helpers.model.Order;
import helpers.model.OrderStatus;
import helpers.model.Product;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.error.AssertJMultipleFailuresError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static helpers.Console.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * <pre>
 * TEMAT: AssertJ — płynne, czytelne asercje
 *        (fluent = płynny: wywołania łączone w łańcuch, który czyta się jak zdanie)
 *
 * W SKRÓCIE:
 *   AssertJ to biblioteka asercji używana razem z JUnit (Spring Boot dołącza ją domyślnie do testów).
 *   Zawsze zaczynamy od {@code assertThat(faktyczne)}, a potem IDE podpowiada metody pasujące do typu:
 *   inne dla tekstu, inne dla listy, mapy, Optional czy wyjątku. Komunikaty porażek są dużo bardziej opisowe.
 *
 * ANALOGIA:
 *   Lekarz z listą kontrolną. assertEquals to pytanie „czy temperatura = 36,6?” — tak/nie.
 *   AssertJ to karta badania: „pacjent: temperatura w normie, ciśnienie między X a Y, wyniki zawierają Z”.
 *   Gdy coś jest nie tak, karta mówi dokładnie co, zamiast samego „źle”.
 *
 * JAK TO DZIAŁA:
 *   1. assertThat(x) zwraca obiekt-asercję dopasowany do typu x (przeciążenia: String → AbstractStringAssert,
 *      List → ListAssert, Map → MapAssert, Optional → OptionalAssert, BigDecimal → BigDecimalAssert ...).
 *   2. Każda metoda (startsWith, hasSize, contains ...) sprawdza jedną rzecz i zwraca TEN SAM obiekt — stąd łańcuch.
 *   3. Pierwsze niespełnione sprawdzenie rzuca AssertionError z opisem (oczekiwane, faktyczne, różnica).
 *      Wyjątek od reguły: SoftAssertions zbierają wszystkie porażki i zgłaszają je razem na końcu.
 *
 *      JUnit                                   AssertJ
 *      -------------------------------------   ------------------------------------------------
 *      assertEquals(3, list.size())            assertThat(list).hasSize(3)
 *      assertTrue(s.startsWith("Ab"))          assertThat(s).startsWith("Ab")
 *      assertTrue(list.contains(x))            assertThat(list).contains(x)
 *      assertThrows(E.class, () -> ...)        assertThatThrownBy(() -> ...).isInstanceOf(E.class)
 *
 * SŁÓWKA:
 *   assert that = zapewnij, że; is equal to = jest równy; contains = zawiera; exactly = dokładnie;
 *   in any order = w dowolnej kolejności; extracting = wyciągając; filtered on = przefiltrowane po;
 *   close to = bliski; offset = przesunięcie, tolerancja; within = w granicach; soft = miękki (nieprzerywający);
 *   recursive comparison = porównanie rekurencyjne (pole po polu, w głąb); thrown by = rzucony przez; as = jako (opis)
 *
 * ZOBACZ TEŻ: t32_junit_mockito/JUnit01Basics (asercje JUnit, runner), t15_numbers/Numbers01BigDecimal (skala),
 *             t14_optional/Optional01Basics, t16_streams/Streams11GroupingBy, t32_junit_mockito/JUnit04Mockito
 * </pre>
 */
public class JUnit03AssertJ {

    public static void main(String[] args) {
        title("JUnit03 — AssertJ: płynne asercje");

        strings();             // strings = teksty
        numbers();             // numbers = liczby
        bigDecimals();         // big decimals = BigDecimal
        collections();         // collections = kolekcje
        mapsAndOptionals();    // maps and optionals = mapy i Optional
        exceptions();          // exceptions = wyjątki
        softAndDescribed();    // soft and described = miękkie i opisane
        recursiveComparison(); // recursive comparison = porównanie rekurencyjne
        readableMessages();    // readable messages = czytelne komunikaty
        exercises();           // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. TEKSTY
    // =================================================================================================

    /** Tekst: startsWith, endsWith, contains, hasSize, matches, isEqualToIgnoringCase, isNotBlank ... */
    static void strings() {
        section("1. assertThat dla tekstu — łańcuch sprawdzeń");

        runTests(StringTests.class);
        // WYNIK: ✘ SKU zgodny ze wzorcem — AssertionError: Expecting actual:
        // WYNIK: ✔ SKU zgodny ze wzorcem (poprawiony)
        // WYNIK: ✔ nazwa laptopa: początek, koniec, długość
        // WYNIK: znalezione 3, zaliczone 2, niezaliczone 1, pominięte 0, przerwane 0

        // („SKU” przed „nazwa”: przy porządku alfabetycznym wielkie litery idą przed małymi — porównanie Unicode.)
        // Komunikaty AssertJ mają wiele linii; runner pokazuje tylko pierwszą. Całe komunikaty: sekcje 4 i 9.
        // DOBRA PRAKTYKA: jeden łańcuch = jeden obiekt. Łańcuch zatrzymuje się na pierwszym niespełnionym warunku.
        // PUŁAPKA: samo assertThat(x) bez żadnej metody NIC nie sprawdza — test przejdzie zawsze. IntelliJ zwykle
        // ostrzega o zignorowanym wyniku (AssertJ oznacza te metody adnotacją @CheckReturnValue).
    }

    static class StringTests {

        @Test
        @DisplayName("nazwa laptopa: początek, koniec, długość")
        void laptopName() {
            String name = SampleData.productBySku("ELE-001").name();
            assertThat(name)
                    .startsWith("Laptop")                       // startsWith = zaczyna się od
                    .endsWith("14")                             // endsWith = kończy się na
                    .contains("Pro")                            // contains = zawiera
                    .hasSize(13)                                // hasSize = ma długość
                    .isEqualToIgnoringCase("LAPTOP PRO 14")     // ignoring case = bez względu na wielkość liter
                    .isNotBlank();                              // is not blank = nie jest pusty
        }

        @Test
        @DisplayName("SKU zgodny ze wzorcem")
        void skuPattern() {
            assertThat("ele-001").matches("[A-Z]{3}-\\d{3}");   // CELOWO zły tekst (małe litery) — porażka
        }

        @Test
        @DisplayName("SKU zgodny ze wzorcem (poprawiony)")
        void skuPatternFixed() {
            assertThat("ELE-001").matches("[A-Z]{3}-\\d{3}");   // matches = pasuje do wyrażenia regularnego
        }
    }

    // =================================================================================================
    // 2. LICZBY
    // =================================================================================================

    /**
     * Liczby: isPositive, isBetween, isGreaterThan ... Dla double — isCloseTo(wartość, within(tolerancja))
     * albo withinPercentage(procent), bo wynik obliczeń zmiennoprzecinkowych rzadko jest DOKŁADNY.
     */
    static void numbers() {
        section("2. Liczby — isBetween, isCloseTo z tolerancją");

        runTests(NumberTests.class);
        // WYNIK: ✔ 0.1 + 0.2 isCloseTo 0.3 w granicach 1e-9
        // WYNIK: ✘ 0.1 + 0.2 isEqualTo 0.3 — AssertionFailedError: expected: 0.3
        // WYNIK: ✔ stan magazynu: dodatni, w przedziale, parzysty?
        // WYNIK: znalezione 3, zaliczone 2, niezaliczone 1, pominięte 0, przerwane 0

        // 0.1 + 0.2 = 0.30000000000000004 w double — dlatego isEqualTo pada (druga linia komunikatu: „but was: ...”).
        // AssertionFailedError (z biblioteki opentest4j, którą dołącza JUnit 5) — AssertJ rzuca go, gdy umie podać
        // „oczekiwane” i „faktyczne” osobno; IntelliJ pokazuje wtedy link „Click to see difference” (okno różnic).
        // Inne porażki AssertJ zgłasza zwykłym AssertionError. Oba to „niezaliczony test”.
        // within(1e-9) = tolerancja bezwzględna; withinPercentage(1) = tolerancja względna (1% wartości).
    }

    static class NumberTests {

        @Test
        @DisplayName("stan magazynu: dodatni, w przedziale, parzysty?")
        void stock() {
            int total = SampleData.products().stream().mapToInt(Product::stock).sum();
            assertThat(total)
                    .isPositive()                               // isPositive = dodatnia
                    .isBetween(500, 600)                        // isBetween = pomiędzy (włącznie z końcami)
                    .isGreaterThan(594)                         // isGreaterThan = większa niż
                    .isOdd();                                   // isOdd = nieparzysta (595)
        }

        @Test
        @DisplayName("0.1 + 0.2 isEqualTo 0.3")
        void doubleExact() {
            assertThat(0.1 + 0.2).isEqualTo(0.3);               // CELOWO — porażka
        }

        @Test
        @DisplayName("0.1 + 0.2 isCloseTo 0.3 w granicach 1e-9")
        void doubleClose() {
            assertThat(0.1 + 0.2).isCloseTo(0.3, within(1e-9));          // isCloseTo = jest bliska
            assertThat(101.0).isCloseTo(100.0, withinPercentage(1));      // różnica 1% → jeszcze OK
        }
    }

    // =================================================================================================
    // 3. BigDecimal — PUŁAPKA SKALI
    // =================================================================================================

    /**
     * {@code isEqualTo} dla BigDecimal używa equals, a equals porównuje też SKALĘ: 297 ≠ 297.00.
     * {@code isEqualByComparingTo} używa compareTo — porównuje samą wartość liczbową.
     */
    static void bigDecimals() {
        section("3. BigDecimal — isEqualByComparingTo zamiast isEqualTo");

        runTests(BigDecimalTests.class);
        // WYNIK: ✔ wartość magazynu: isEqualByComparingTo("297")
        // WYNIK: ✘ wartość magazynu: isEqualTo(297) — ŹLE — AssertionFailedError: expected: 297
        // WYNIK: ✔ wartość magazynu: isEqualTo(297.00) — ta sama skala
        // WYNIK: znalezione 3, zaliczone 2, niezaliczone 1, pominięte 0, przerwane 0

        // PUŁAPKA: komunikat brzmi „expected: 297 / but was: 297.00” — wygląda jak ta sama liczba, a test pada.
        // Powód: skala (liczba cyfr po przecinku) 0 kontra 2. Szczegóły: t15_numbers/Numbers01BigDecimal.
        // DOBRA PRAKTYKA: kwoty sprawdzaj przez isEqualByComparingTo("297.00") — wersja z tekstem jest najwygodniejsza.
    }

    static class BigDecimalTests {

        /** Wartość magazynu „Wzorce projektowe”: 99.00 × 3 = 297.00 (skala 2). */
        private final BigDecimal stockValue = SampleData.productBySku("KSI-003").stockValue();

        @Test
        @DisplayName("wartość magazynu: isEqualTo(297) — ŹLE")
        void wrongScale() {
            assertThat(stockValue).isEqualTo(new BigDecimal("297"));     // CELOWO — skala 0 ≠ 2
        }

        @Test
        @DisplayName("wartość magazynu: isEqualTo(297.00) — ta sama skala")
        void sameScale() {
            assertThat(stockValue).isEqualTo(new BigDecimal("297.00"));
        }

        @Test
        @DisplayName("wartość magazynu: isEqualByComparingTo(\"297\")")
        void byComparing() {
            assertThat(stockValue).isEqualByComparingTo("297");          // by comparing = przez porównanie (compareTo)
        }
    }

    // =================================================================================================
    // 4. KOLEKCJE
    // =================================================================================================

    /**
     * Najmocniejsza strona AssertJ. {@code containsExactly} — te elementy i W TEJ kolejności;
     * {@code containsExactlyInAnyOrder} — te same elementy, kolejność dowolna; {@code contains} — co najmniej te.
     * {@code extracting(Product::name)} zamienia listę obiektów na listę pól; {@code filteredOn(...)} zawęża listę;
     * {@code extracting(a, b)} + {@code tuple(...)} sprawdza kilka pól naraz.
     */
    static void collections() {
        section("4. Kolekcje — containsExactly, extracting, filteredOn, tuple");

        runTests(CollectionTests.class);
        // WYNIK: ✘ containsExactly: zła kolejność — AssertionFailedError: Expecting actual:
        // WYNIK: ✔ containsExactlyInAnyOrder: kolejność bez znaczenia
        // WYNIK: ✔ extracting + filteredOn: książki
        // WYNIK: ✔ extracting kilku pól + tuple
        // WYNIK: ✔ niedostępne produkty: allMatch, noneMatch, hasSize
        // WYNIK: znalezione 5, zaliczone 4, niezaliczone 1, pominięte 0, przerwane 0

        showFailure("cały komunikat containsExactly", () -> assertThat(List.of("Czysty kod", "Java. Podstawy"))
                .containsExactly("Java. Podstawy", "Czysty kod"), 8);
        // WYNIK: cały komunikat containsExactly → AssertionFailedError:
        // WYNIK: │ Expecting actual:
        // WYNIK: │ ["Czysty kod", "Java. Podstawy"]
        // WYNIK: │ to contain exactly (and in same order):
        // WYNIK: │ ["Java. Podstawy", "Czysty kod"]
        // WYNIK: │ but there were differences at these indexes:
        // WYNIK: │ - element at index 0: expected "Java. Podstawy" but was "Czysty kod"
        // WYNIK: │ - element at index 1: expected "Czysty kod" but was "Java. Podstawy"

        // Komunikat mówi, co jest nie tak: lista faktyczna, oczekiwana i różnice na każdej pozycji
        // („to contain exactly (and in same order)” = zawierać dokładnie (i w tej samej kolejności)).
        // PUŁAPKA: containsExactly na wyniku z HashSet/HashMap — kolejność nie jest gwarantowana, test może „migać”.
        // Dla zbiorów używaj containsExactlyInAnyOrder albo containsOnly.
    }

    static class CollectionTests {
        private final List<Product> products = SampleData.products();

        @Test
        @DisplayName("extracting + filteredOn: książki")
        void books() {
            assertThat(products)
                    .filteredOn(p -> p.category() == Category.KSIAZKI)   // zostaw tylko książki
                    .extracting(Product::name)                           // weź z nich nazwy
                    .containsExactly("Czysty kod", "Java. Podstawy", "Wzorce projektowe");
        }

        @Test
        @DisplayName("containsExactly: zła kolejność")
        void wrongOrder() {
            assertThat(products)
                    .filteredOn(p -> p.category() == Category.KSIAZKI)
                    .extracting(Product::name)
                    .containsExactly("Wzorce projektowe", "Czysty kod", "Java. Podstawy");   // CELOWO — inna kolejność
        }

        @Test
        @DisplayName("containsExactlyInAnyOrder: kolejność bez znaczenia")
        void anyOrder() {
            assertThat(products)
                    .filteredOn(p -> p.category() == Category.KSIAZKI)
                    .extracting(Product::name)
                    .containsExactlyInAnyOrder("Wzorce projektowe", "Czysty kod", "Java. Podstawy");
        }

        @Test
        @DisplayName("extracting kilku pól + tuple")
        void tuples() {
            assertThat(products)
                    .filteredOn(p -> p.category() == Category.DOM)
                    .extracting(Product::sku, Product::stock)            // dwa pola → krotki (tuple = krotka)
                    .containsExactly(tuple("DOM-001", 2), tuple("DOM-002", 18));
        }

        @Test
        @DisplayName("niedostępne produkty: allMatch, noneMatch, hasSize")
        void outOfStock() {
            assertThat(products)
                    .hasSize(14)
                    .filteredOn(p -> !p.inStock())
                    .hasSize(2)
                    .allMatch(p -> p.stock() == 0)                       // allMatch = wszystkie spełniają
                    .noneMatch(p -> p.price().signum() <= 0)             // noneMatch = żaden nie spełnia
                    .extracting(Product::name)
                    .containsOnly("Smartfon X", "Oliwa z oliwek")        // containsOnly = tylko te (kolejność dowolna)
                    .doesNotContain("Laptop Pro 14");                    // doesNotContain = nie zawiera
        }
    }

    // =================================================================================================
    // 5. MAPY I Optional
    // =================================================================================================

    /** Mapy: containsEntry, containsKeys, doesNotContainKey, hasSize. Optional: isPresent, contains, isEmpty. */
    static void mapsAndOptionals() {
        section("5. Mapy i Optional");

        runTests(MapAndOptionalTests.class);
        // WYNIK: ✘ Optional pusty, a oczekiwano wartości — AssertionError: Expecting Optional to contain:
        // WYNIK: ✔ e-mail klienta: Optional z wartością i pusty
        // WYNIK: ✔ liczba produktów w kategoriach
        // WYNIK: znalezione 3, zaliczone 2, niezaliczone 1, pominięte 0, przerwane 0

        // DOBRA PRAKTYKA: assertThat(optional).contains(x) zamiast assertEquals(x, optional.get()) — przy pustym
        // Optional dostajesz czytelny komunikat zamiast NoSuchElementException z get().
    }

    static class MapAndOptionalTests {

        @Test
        @DisplayName("liczba produktów w kategoriach")
        void categoryCounts() {
            Map<Category, Long> counts = SampleData.products().stream()
                    .collect(Collectors.groupingBy(Product::category,
                            () -> new EnumMap<>(Category.class), Collectors.counting()));   // t16_streams/Streams11GroupingBy
            assertThat(counts)
                    .hasSize(5)
                    .containsEntry(Category.ELEKTRONIKA, 4L)            // containsEntry = zawiera parę klucz–wartość
                    .contains(entry(Category.DOM, 2L), entry(Category.ODZIEZ, 2L))
                    .containsKeys(Category.KSIAZKI, Category.SPOZYWCZE) // containsKeys = zawiera klucze
                    .doesNotContainValue(0L);                           // żadna kategoria nie jest pusta
        }

        @Test
        @DisplayName("e-mail klienta: Optional z wartością i pusty")
        void emails() {
            List<Customer> customers = SampleData.customers();
            assertThat(customers.get(0).findEmail())
                    .isPresent()                                        // isPresent = jest wartość
                    .contains("jan@example.com");                       // contains = zawiera dokładnie tę wartość
            assertThat(customers.get(1).findEmail()).isEmpty();         // Maria Nowak nie ma e-maila
        }

        @Test
        @DisplayName("Optional pusty, a oczekiwano wartości")
        void emptyOptional() {
            Optional<String> email = SampleData.customers().get(4).findEmail();   // Ola Pawlak — brak e-maila
            assertThat(email).contains("ola@example.com");                         // CELOWO — porażka
        }
    }

    // =================================================================================================
    // 6. WYJĄTKI
    // =================================================================================================

    /**
     * {@code assertThatThrownBy(() -> kod)} łapie wyjątek i pozwala sprawdzić typ, komunikat, przyczynę.
     * {@code assertThatExceptionOfType(Typ.class).isThrownBy(() -> kod)} — ten sam efekt, typ podany na początku.
     * {@code assertThatCode(() -> kod).doesNotThrowAnyException()} — jawnie: „tu nie może polecieć wyjątek”.
     */
    static void exceptions() {
        section("6. Wyjątki — assertThatThrownBy, hasMessageContaining");

        runTests(ExceptionTests.class);
        // WYNIK: ✔ assertThatCode: poprawny SKU nie rzuca
        // WYNIK: ✔ assertThatExceptionOfType ... isThrownBy
        // WYNIK: ✔ assertThatThrownBy: nieznany SKU
        // WYNIK: ✘ oczekiwano wyjątku, a kod go nie rzucił — AssertionError: Expecting code to raise a throwable.
        // WYNIK: znalezione 4, zaliczone 3, niezaliczone 1, pominięte 0, przerwane 0

        // PUŁAPKA: hasMessage("...") wymaga IDENTYCZNEGO komunikatu — drobna zmiana tekstu psuje test.
        // Zwykle wystarczy hasMessageContaining z kluczowym fragmentem (np. SKU, kwota).
    }

    static class ExceptionTests {

        @Test
        @DisplayName("assertThatThrownBy: nieznany SKU")
        void unknownSku() {
            assertThatThrownBy(() -> SampleData.productBySku("XXX-999"))   // thrown by = rzucony przez
                    .isInstanceOf(NoSuchElementException.class)             // isInstanceOf = jest instancją typu
                    .hasMessageContaining("XXX-999");                       // komunikat zawiera fragment
        }

        @Test
        @DisplayName("assertThatExceptionOfType ... isThrownBy")
        void exceptionOfType() {
            assertThatExceptionOfType(UnsupportedOperationException.class)
                    .isThrownBy(() -> SampleData.products().add(null))     // lista z List.of — niezmienna
                    .withMessage(null);                                    // with message = z komunikatem (tu: brak)
        }

        @Test
        @DisplayName("assertThatCode: poprawny SKU nie rzuca")
        void noException() {
            assertThatCode(() -> SampleData.productBySku("ELE-001")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("oczekiwano wyjątku, a kod go nie rzucił")
        void noThrow() {
            assertThatThrownBy(() -> SampleData.productBySku("ELE-001"))   // CELOWO — istniejący SKU
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    // =================================================================================================
    // 7. as(...) I SoftAssertions
    // =================================================================================================

    /**
     * {@code as("opis")} dodaje do komunikatu nawias kwadratowy z opisem — wiadomo, KTÓRE sprawdzenie padło.
     * Musi stać PRZED metodą sprawdzającą. {@code SoftAssertions.assertSoftly(softly -> ...)} wykonuje wszystkie
     * sprawdzenia i zgłasza wszystkie porażki razem (odpowiednik assertAll z JUnit).
     */
    static void softAndDescribed() {
        section("7. Opis as(...) i miękkie asercje SoftAssertions");

        runTests(SoftTests.class);
        // WYNIK: ✘ as(...): opis w komunikacie — AssertionFailedError: [status zamówienia ZAM-001]
        // WYNIK: ✘ assertSoftly: dwie porażki naraz — AssertJMultipleFailuresError: Multiple Failures (2 failures)
        // WYNIK: ✔ assertSoftly: wszystko się zgadza
        // WYNIK: znalezione 3, zaliczone 1, niezaliczone 2, pominięte 0, przerwane 0

        // Pierwsza linia porażki to sam opis w nawiasie; dalej „expected: NOWE / but was: DOSTARCZONE”.
        // PUŁAPKA: assertThat(x).isEqualTo(y).as("opis") — opis PO sprawdzeniu nie zadziała (sprawdzenie już padło).
        // DOBRA PRAKTYKA: SoftAssertions do sprawdzania wielu pól jednego obiektu (formularz, odpowiedź REST),
        // zwykłe asercje — gdy dalsze kroki nie mają sensu po pierwszym błędzie.
    }

    static class SoftTests {
        private final Order order = SampleData.orders().get(0);      // ZAM-001, Jan Kowalski, DOSTARCZONE, 6199.79

        @Test
        @DisplayName("as(...): opis w komunikacie")
        void described() {
            assertThat(order.status()).as("status zamówienia %s", order.id()).isEqualTo(OrderStatus.NOWE);   // CELOWO
        }

        @Test
        @DisplayName("assertSoftly: wszystko się zgadza")
        void softOk() {
            SoftAssertions.assertSoftly(softly -> {                 // softly = „miękko”, nie przerywając
                softly.assertThat(order.id()).isEqualTo("ZAM-001");
                softly.assertThat(order.customer().name()).isEqualTo("Jan Kowalski");
                softly.assertThat(order.total()).isEqualByComparingTo("6199.79");
                softly.assertThat(order.lines()).hasSize(2);
            });
        }

        @Test
        @DisplayName("assertSoftly: dwie porażki naraz")
        void softTwoFailures() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(order.id()).isEqualTo("ZAM-001");                     // ✔
                softly.assertThat(order.status()).isEqualTo(OrderStatus.NOWE);          // ✘
                softly.assertThat(order.lines()).as("pozycje").hasSize(3);             // ✘
            });
        }
    }

    // =================================================================================================
    // 8. PORÓWNANIE REKURENCYJNE
    // =================================================================================================

    /** Koszyk bez equals (zwykła klasa) — isEqualTo porówna referencje (to ten sam obiekt?), a nie zawartość. */
    static class Cart {                                        // cart = koszyk
        String owner;
        List<String> items;                                    // items = pozycje

        Cart(String owner, List<String> items) {
            this.owner = owner;
            this.items = items;
        }

        @Override
        public String toString() { return "Koszyk " + owner + " " + items; }
    }

    /**
     * {@code usingRecursiveComparison()} porównuje obiekty POLE PO POLU (także pola obiektów zagnieżdżonych),
     * bez względu na to, czy klasa ma equals. {@code ignoringFields("pole")} pomija wybrane pola — np. id albo datę
     * wygenerowaną przez system.
     */
    static void recursiveComparison() {
        section("8. Porównanie rekurencyjne — usingRecursiveComparison");

        runTests(RecursiveTests.class);
        // WYNIK: ✘ Cart bez equals: isEqualTo porównuje referencje — AssertionFailedError: expected: "Koszyk Ola [kawa] (Cart@HASH)"
        // WYNIK: ✔ Cart: porównanie rekurencyjne pole po polu
        // WYNIK: ✔ zamówienie: ignoringFields("status")
        // WYNIK: znalezione 3, zaliczone 2, niezaliczone 1, pominięte 0, przerwane 0

        // Oba koszyki mają ten sam toString, więc AssertJ dopisuje „(Cart@liczba)” — skrót tożsamości obiektu —
        // żeby pokazać, że to DWA różne obiekty. Ta liczba szesnastkowa zmienia się przy każdym uruchomieniu,
        // dlatego runner tej lekcji zamienia ją na HASH (wynik ma być powtarzalny).
        // DOBRA PRAKTYKA: porównanie rekurencyjne przydaje się dla klas bez equals (encje JPA, obiekty DTO)
        // i gdy kilka pól trzeba pominąć. Rekordy mają equals z automatu, więc zwykle wystarczy isEqualTo.
        // PUŁAPKA: ignoringFields przyjmuje nazwy jako tekst — po zmianie nazwy pola test dalej się kompiluje.
    }

    static class RecursiveTests {

        @Test
        @DisplayName("Cart bez equals: isEqualTo porównuje referencje")
        void identity() {
            assertThat(new Cart("Ola", List.of("kawa"))).isEqualTo(new Cart("Ola", List.of("kawa")));   // CELOWO
        }

        @Test
        @DisplayName("Cart: porównanie rekurencyjne pole po polu")
        void recursive() {
            assertThat(new Cart("Ola", List.of("kawa"))).usingRecursiveComparison()
                    .isEqualTo(new Cart("Ola", List.of("kawa")));
        }

        @Test
        @DisplayName("zamówienie: ignoringFields(\"status\")")
        void orderIgnoringStatus() {
            Order original = SampleData.orders().get(2);                          // ZAM-003, WYSLANE
            Order delivered = new Order(original.id(), original.customer(), original.date(),
                    OrderStatus.DOSTARCZONE, original.lines());                   // ta sama treść, inny status
            assertThat(delivered).usingRecursiveComparison()
                    .ignoringFields("status")                                     // ignoring fields = ignorując pola
                    .isEqualTo(original);
        }
    }

    // =================================================================================================
    // 9. CZYTELNE KOMUNIKATY — JUnit KONTRA AssertJ
    // =================================================================================================

    /**
     * To samo sprawdzenie zapisane dwa razy. Łapiemy AssertionError i wypisujemy pierwsze linie komunikatu,
     * żeby porównać, ile każdy z nich mówi o przyczynie porażki.
     */
    static void readableMessages() {
        section("9. Czytelne komunikaty — JUnit kontra AssertJ");

        List<String> names = List.of("kawa", "herbata", "mleko");

        showFailure("JUnit  assertTrue(names.contains(\"cukier\"))",
                () -> assertTrue(names.contains("cukier")), 4);
        // WYNIK: JUnit  assertTrue(names.contains("cukier")) → AssertionFailedError:
        // WYNIK: │ expected: <true> but was: <false>

        showFailure("AssertJ assertThat(names).contains(\"cukier\")",
                () -> assertThat(names).contains("cukier"), 6);
        // WYNIK: AssertJ assertThat(names).contains("cukier") → AssertionError:
        // WYNIK: │ Expecting ListN:
        // WYNIK: │ ["kawa", "herbata", "mleko"]
        // WYNIK: │ to contain:
        // WYNIK: │ ["cukier"]
        // WYNIK: │ but could not find the following element(s):
        // WYNIK: │ ["cukier"]

        showFailure("JUnit  assertEquals(2, names.size())", () -> assertEquals(2, names.size()), 4);
        // WYNIK: JUnit  assertEquals(2, names.size()) → AssertionFailedError:
        // WYNIK: │ expected: <2> but was: <3>

        showFailure("AssertJ assertThat(names).hasSize(2)", () -> assertThat(names).hasSize(2), 6);
        // WYNIK: AssertJ assertThat(names).hasSize(2) → AssertionError:
        // WYNIK: │ Expected size: 2 but was: 3 in:
        // WYNIK: │ ["kawa", "herbata", "mleko"]

        // JUnit mówi „true ≠ false” — trzeba otworzyć kod, żeby zrozumieć. AssertJ pokazuje listę, szukany element
        // i czego brakuje. Przy 50 testach na czerwono ta różnica to godziny pracy.
        // „ListN” to wewnętrzna klasa JDK, którą zwraca List.of — AssertJ podaje prawdziwy typ obiektu.
        // DOBRA PRAKTYKA: w nowych projektach AssertJ do asercji, JUnit do struktury testów (@Test, @Nested ...).
        // Spring Boot (spring-boot-starter-test) ma oba w zestawie.
    }

    /** Uruchamia sprawdzenie i wypisuje typ błędu oraz najwyżej {@code maxLines} niepustych linii komunikatu. */
    static void showFailure(String label, Runnable check, int maxLines) {
        try {
            check.run();
            System.out.println(label + " → brak błędu");
        } catch (AssertionError e) {
            System.out.println(label + " → " + e.getClass().getSimpleName() + ":");
            e.getMessage().lines().map(String::strip).filter(l -> !l.isEmpty()).limit(maxLines)
                    .forEach(l -> System.out.println("│ " + l));
        }
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Zawsze assertThat(FAKTYCZNE).metoda(OCZEKIWANE) — kolejności nie da się pomylić.
     *   • Tekst: startsWith, endsWith, contains, hasSize, matches, isEqualToIgnoringCase, isBlank/isNotBlank.
     *   • Liczby: isPositive, isBetween(a, b), isGreaterThan; double: isCloseTo(x, within(0.001)) / withinPercentage.
     *   • BigDecimal: isEqualByComparingTo("12.50") (compareTo); isEqualTo patrzy też na skalę.
     *   • Lista: hasSize, contains, containsExactly (kolejność!), containsExactlyInAnyOrder, containsOnly,
     *     doesNotContain, allMatch/noneMatch, filteredOn(warunek), extracting(Klasa::pole), extracting(a, b) + tuple.
     *   • Mapa: containsEntry(k, v), contains(entry(k, v)), containsKeys, doesNotContainKey. Optional: contains, isEmpty.
     *   • Wyjątki: assertThatThrownBy(() -> ...).isInstanceOf(...).hasMessageContaining("..."); assertThatCode(...)
     *     .doesNotThrowAnyException(); assertThatExceptionOfType(T.class).isThrownBy(...).
     *   • as("opis %s", x) PRZED sprawdzeniem; SoftAssertions.assertSoftly(softly -> ...) = wszystkie błędy naraz.
     *   • usingRecursiveComparison().ignoringFields("id") — pole po polu, także bez equals.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego assertThat(list).hasSize(3) daje lepszy komunikat niż assertEquals(3, list.size())?
     *   2. Co wypisze (przejdzie czy nie?):  assertThat(new BigDecimal("1.0")).isEqualTo(new BigDecimal("1.00"));  ?
     *   3. ZNAJDŹ BŁĄD:  assertThat(order.total()).isEqualByComparingTo("6199.79").as("suma zamówienia");
     *   4. Czym różni się containsExactly od containsExactlyInAnyOrder i od contains?
     *   5. ZNAJDŹ BŁĄD:  @Test void name() { assertThat(product.name().startsWith("Lap")); }
     *   6. Co wypisze pierwsza linia porażki:  assertThat(0.1 + 0.2).isEqualTo(0.3);  ? Jak to naprawić?
     *   7. Kiedy porównanie rekurencyjne jest konieczne, a kiedy wystarczy isEqualTo?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — piszesz asercje; sprawdzarka podaje Twojej metodzie dane dobre (muszą przejść)
    // i złe (asercja musi je odrzucić błędem AssertionError)
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: format SKU", "dobre dane: przechodzą; złe dane wykryte: 3 z 3",
                () -> verdict(JUnit03AssertJ::exercise1, "ELE-001", List.of("ele-001", "ELE-01", "ELE001")));
        Check.equal("ćw. 2: PRZEPISZ na AssertJ", "dobre dane: przechodzą; złe dane wykryte: 2 z 2",
                () -> verdict(JUnit03AssertJ::exercise2, SampleData.products(), badProductLists()));
        Check.equal("ćw. 3: zamówienie ZAM-001", "dobre dane: przechodzą; złe dane wykryte: 2 z 2",
                () -> verdict(JUnit03AssertJ::exercise3, SampleData.orders().get(0), badOrders()));
        Check.equal("ćw. 4: miękkie sprawdzenie klienta", "dobry klient: przechodzi; zły klient: 2 porażki naraz",
                () -> softVerdict(JUnit03AssertJ::exercise4));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "dobre dane: przechodzą; złe dane wykryte: 3 z 3",
                () -> verdict(JUnit03AssertJ::solution1, "ELE-001", List.of("ele-001", "ELE-01", "ELE001")));
        Check.equal("ćw. 2 (wzorzec)", "dobre dane: przechodzą; złe dane wykryte: 2 z 2",
                () -> verdict(JUnit03AssertJ::solution2, SampleData.products(), badProductLists()));
        Check.equal("ćw. 3 (wzorzec)", "dobre dane: przechodzą; złe dane wykryte: 2 z 2",
                () -> verdict(JUnit03AssertJ::solution3, SampleData.orders().get(0), badOrders()));
        Check.equal("ćw. 4 (wzorzec)", "dobry klient: przechodzi; zły klient: 2 porażki naraz",
                () -> softVerdict(JUnit03AssertJ::solution4));
        Check.summary();
        // WYNIK: ✔ OK    ćw. 1 (wzorzec)
        // WYNIK: ✔ OK    ćw. 2 (wzorzec)
        // WYNIK: ✔ OK    ćw. 3 (wzorzec)
        // WYNIK: ✔ OK    ćw. 4 (wzorzec)
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): jednym łańcuchem AssertJ sprawdź, że SKU ma 7 znaków, zawiera myślnik na pozycji 4
     * i pasuje do wzorca: 3 wielkie litery, myślnik, 3 cyfry.
     * Podpowiedź: hasSize(7), matches("[A-Z]{3}-\\d{3}") — samo matches też by wystarczyło, ale hasSize daje lepszy komunikat.
     */
    static void exercise1(String sku) {
        // TODO: assertThat(sku)...
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ asercje JUnit na AssertJ (trzy łańcuchy albo jeden):
     * <pre>{@code
     * assertEquals(14, products.size());
     * assertTrue(products.stream().anyMatch(p -> p.name().equals("Czysty kod")));
     * assertEquals(2, products.stream().filter(p -> !p.inStock()).count());
     * }</pre>
     * Podpowiedź: hasSize, extracting(Product::name).contains(...), filteredOn(p -> !p.inStock()).hasSize(2).
     */
    static void exercise2(List<Product> products) {
        // TODO: twoje asercje AssertJ
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): sprawdź zamówienie: id „ZAM-001”, klient „Jan Kowalski”, suma 6199.79 (BigDecimal —
     * uważaj na skalę!), ilości w pozycjach dokładnie 1 i 2 (w tej kolejności), SKU produktów ELE-001 i ELE-003.
     * Podpowiedź: extracting(OrderLine::quantity) — albo lambda {@code line -> line.quantity()};
     * dla SKU: extracting(line -> line.product().sku()).
     */
    static void exercise3(Order order) {
        // TODO: twoje asercje AssertJ
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): miękko (SoftAssertions.assertSoftly) sprawdź klienta: id dodatnie, imię i nazwisko
     * niepuste, miasto niepuste. Sprawdzarka poda dobrego klienta (musi przejść) i klienta z id = 0 i pustym
     * miastem — Twój kod ma zgłosić OBIE porażki naraz.
     * Podpowiedź: softly.assertThat(c.id()).isPositive(); softly.assertThat(c.city()).isNotBlank(); ...
     */
    static void exercise4(Customer customer) {
        // TODO: SoftAssertions.assertSoftly(softly -> { ... });
        throw new UnsupportedOperationException("TODO");
    }

    /** Złe listy do ćw. 2: bez „Czysty kod” (13 elementów) oraz 14 produktów, z których tylko jeden jest niedostępny. */
    private static List<List<Product>> badProductLists() {
        List<Product> withoutBook = new ArrayList<>(SampleData.products());
        withoutBook.removeIf(p -> p.name().equals("Czysty kod"));
        List<Product> oneMissing = new ArrayList<>(SampleData.products());
        Product oil = SampleData.productBySku("SPO-003");
        oneMissing.set(oneMissing.indexOf(oil), new Product(oil.sku(), oil.name(), oil.category(), oil.price(), 5));
        return List.of(withoutBook, oneMissing);
    }

    /** Złe zamówienia do ćw. 3: ZAM-003 oraz ZAM-001 z pozycjami w odwrotnej kolejności. */
    private static List<Order> badOrders() {
        Order first = SampleData.orders().get(0);
        Order reversed = new Order(first.id(), first.customer(), first.date(), first.status(),
                List.of(first.lines().get(1), first.lines().get(0)));
        return List.of(SampleData.orders().get(2), reversed);
    }

    /** Sprawdzarka: dobre dane muszą przejść, każde złe — wywołać AssertionError. */
    static <T> String verdict(Consumer<T> assertion, T good, List<T> bad) {
        String goodResult;
        try {
            assertion.accept(good);
            goodResult = "przechodzą";
        } catch (Throwable e) {
            goodResult = "błąd " + e.getClass().getSimpleName();
        }
        int detected = 0;
        for (T data : bad) {
            try {
                assertion.accept(data);
            } catch (AssertionError e) {
                detected++;
            } catch (RuntimeException e) {
                // inny wyjątek (np. TODO) nie liczy się jako wykrycie
            }
        }
        return "dobre dane: " + goodResult + "; złe dane wykryte: " + detected + " z " + bad.size();
    }

    /** Sprawdzarka ćw. 4: zły klient musi dać AssertJMultipleFailuresError z dwiema porażkami. */
    static String softVerdict(Consumer<Customer> assertion) {
        String good;
        try {
            assertion.accept(SampleData.customers().get(0));
            good = "przechodzi";
        } catch (Throwable e) {
            good = "błąd " + e.getClass().getSimpleName();
        }
        String bad;
        try {
            assertion.accept(new Customer(0, "Jan Testowy", " ", "jan@example.com", false));
            bad = "brak błędu";
        } catch (AssertJMultipleFailuresError e) {
            bad = e.getFailures().size() + " porażki naraz";
        } catch (Throwable e) {
            bad = "błąd " + e.getClass().getSimpleName();
        }
        return "dobry klient: " + good + "; zły klient: " + bad;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static void solution1(String sku) {
        assertThat(sku).hasSize(7).matches("[A-Z]{3}-\\d{3}");
    }

    static void solution2(List<Product> products) {
        assertThat(products).hasSize(14);
        assertThat(products).extracting(Product::name).contains("Czysty kod");
        assertThat(products).filteredOn(p -> !p.inStock()).hasSize(2);
    }

    static void solution3(Order order) {
        assertThat(order.id()).isEqualTo("ZAM-001");
        assertThat(order.customer().name()).isEqualTo("Jan Kowalski");
        assertThat(order.total()).isEqualByComparingTo("6199.79");
        assertThat(order.lines()).extracting(line -> line.quantity()).containsExactly(1, 2);
        assertThat(order.lines()).extracting(line -> line.product().sku()).containsExactly("ELE-001", "ELE-003");
    }

    static void solution4(Customer customer) {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(customer.id()).as("id").isPositive();
            softly.assertThat(customer.name()).as("imię i nazwisko").isNotBlank();
            softly.assertThat(customer.city()).as("miasto").isNotBlank();
        });
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

    /** Uruchamia testy klasy i wypisuje po jednej linii na test oraz podsumowanie. */
    static RunResult runTests(Class<?> testClass) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(testClass))
                .configurationParameter("junit.jupiter.testmethod.order.default",
                        "org.junit.jupiter.api.MethodOrderer$DisplayName")
                .configurationParameter("junit.jupiter.testclass.order.default",
                        "org.junit.jupiter.api.ClassOrderer$DisplayName")
                .build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
        launcher.execute(request, summaryListener, new PrintingListener());
        TestExecutionSummary s = summaryListener.getSummary();
        RunResult result = new RunResult(s.getTestsFoundCount(), s.getTestsSucceededCount(), s.getTestsFailedCount(),
                s.getTestsSkippedCount(), s.getTestsAbortedCount());
        System.out.println(result.brief());
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

        /** Pierwsza niepusta linia komunikatu; „@1a2b3c” (skrót tożsamości obiektu) zamieniamy na „@HASH”. */
        private static String firstLine(Optional<Throwable> error) {
            return error.map(Throwable::getMessage)
                    .flatMap(m -> m.lines().map(String::strip).filter(l -> !l.isEmpty()).findFirst())
                    .map(l -> l.replaceAll("@[0-9a-f]{1,8}\\b", "@HASH"))
                    .orElse("(brak komunikatu)");
        }
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. assertEquals widzi tylko dwie liczby („expected: <3> but was: <4>”). AssertJ widzi całą listę, więc
     *      pisze „Expected size: 3 but was: 4 in: [...]” — od razu widać, który element jest nadmiarowy.
     *   2. Nie przejdzie: isEqualTo dla BigDecimal używa equals, a 1.0 (skala 1) i 1.00 (skala 2) nie są równe
     *      według equals. Poprawnie: isEqualByComparingTo(new BigDecimal("1.00")) albo isEqualByComparingTo("1").
     *   3. as(...) stoi PO sprawdzeniu — gdy sprawdzenie padnie, opis jeszcze nie jest ustawiony i nie trafi do
     *      komunikatu. Poprawnie: assertThat(order.total()).as("suma zamówienia").isEqualByComparingTo("6199.79").
     *   4. containsExactly — dokładnie te elementy, w tej kolejności, nic więcej. containsExactlyInAnyOrder — dokładnie
     *      te elementy (z powtórzeniami), kolejność dowolna. contains — co najmniej te (mogą być inne, kolejność dowolna).
     *   5. assertThat(boolean) bez metody sprawdzającej nic nie sprawdza — test przechodzi zawsze. Poprawnie:
     *      assertThat(product.name()).startsWith("Lap")   (albo assertThat(product.name().startsWith("Lap")).isTrue(),
     *      ale to gorszy komunikat).
     *   6. „expected: 0.3” (a w drugiej linii „but was: 0.30000000000000004”). Naprawa: isCloseTo(0.3, within(1e-9)).
     *   7. Konieczne dla klas bez equals (porównałyby się referencje) i gdy trzeba pominąć pola (ignoringFields),
     *      np. wygenerowane id czy datę. Dla rekordów i klas z poprawnym equals zwykle wystarcza isEqualTo.
     */
    // </editor-fold>
}
