package t32_junit_mockito;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.function.IntUnaryOperator;
import java.util.stream.Stream;

import static helpers.Console.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * <pre>
 * TEMAT: Testy sparametryzowane — jeden test, wiele zestawów danych
 *        (parameterized = sparametryzowany; source = źródło danych)
 *
 * W SKRÓCIE:
 *   Zamiast kopiować ten sam test z innymi liczbami, piszemy go RAZ z parametrami, a dane podajemy osobno:
 *   listą wartości, tabelką CSV, metodą zwracającą strumień albo wartościami enuma. JUnit uruchamia test
 *   dla każdego zestawu i raportuje każdy osobno. Do tego: testy powtarzane i dynamiczne.
 *
 * ANALOGIA:
 *   Kontrola biletów w tramwaju. Kontroler ma JEDNĄ procedurę („sprawdź datę, strefę, kasownik”), a stosuje ją
 *   do każdego pasażera po kolei. Nie pisze osobnej instrukcji dla każdej osoby — podmienia tylko dane (bilet).
 *
 * JAK TO DZIAŁA:
 *   1. Metoda z {@code @ParameterizedTest} (zamiast {@code @Test}) przyjmuje parametry.
 *   2. Adnotacja źródła mówi, skąd wziąć dane:
 *        {@code @ValueSource}       — jedna lista prostych wartości (1 parametr)
 *        {@code @NullAndEmptySource} — dodatkowo null i "" (albo pusta kolekcja/tablica)
 *        {@code @CsvSource}         — wiersze „a, b, c” → kilka parametrów (też jako blok tekstu)
 *        {@code @CsvFileSource}     — to samo, ale z pliku .csv w zasobach testów
 *        {@code @MethodSource}      — statyczna metoda zwracająca {@code Stream<Arguments>} (dowolne obiekty)
 *        {@code @EnumSource}        — stałe enuma (wszystkie, wybrane albo wszystkie poza wybranymi)
 *   3. Każdy zestaw = osobne „wywołanie” (invocation) z własną nazwą, np. „[2] 1234563218, true”.
 *   4. Tekst z CSV JUnit sam zamienia na typ parametru (int, boolean, BigDecimal, enum, LocalDate...).
 *
 * SŁÓWKA:
 *   value = wartość; argument = argument; invocation = wywołanie; accessor = akcesor (dostęp do argumentów);
 *   aggregator = agregator (składa argumenty w obiekt); repeated = powtarzany; factory = fabryka;
 *   dynamic = dynamiczny; boundary value = wartość brzegowa; equivalence class = klasa równoważności;
 *   mode = tryb; include/exclude = dołącz/wyklucz; text block = blok tekstu
 *
 * ZOBACZ TEŻ: t32_junit_mockito/JUnit01Basics (podstawy, runner), t25_testing/Testing01Concepts (dobór przypadków),
 *             t32_junit_mockito/JUnit03AssertJ (czytelniejsze asercje), t08_enums/Enums01Basics (enumy)
 * </pre>
 */
public class JUnit02Parameterized {

    public static void main(String[] args) {
        title("JUnit02 — testy sparametryzowane");

        valueSource();           // value source = źródło wartości
        nullAndEmpty();          // null and empty = null i pusty
        csvSource();             // csv source = źródło CSV
        methodSource();          // method source = źródło-metoda
        enumSource();            // enum source = źródło-enum
        boundaryValues();        // boundary values = wartości brzegowe
        accessorsAndAggregators(); // accessors and aggregators = akcesory i agregatory
        repeatedAndDynamic();    // repeated and dynamic = powtarzane i dynamiczne
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // KOD PRODUKCYJNY — to testujemy
    // =================================================================================================

    /** Kod pocztowy w formacie dd-ddd, np. 00-950 (postal code = kod pocztowy). */
    static boolean isValidPostalCode(String code) {
        return code != null && code.matches("\\d{2}-\\d{3}");   // matches = pasuje do wyrażenia regularnego
    }

    /**
     * NIP: 10 cyfr (myślniki dozwolone), suma kontrolna: cyfry 1–9 razy wagi 6,5,7,2,3,4,5,6,7, suma modulo 11
     * ma być równa 10. cyfrze; wynik 10 oznacza NIP nieprawidłowy. Numery w lekcji są wymyślone.
     */
    static boolean isValidNip(String nip) {
        if (nip == null) {
            return false;
        }
        String digits = nip.replace("-", "");
        if (!digits.matches("\\d{10}")) {
            return false;
        }
        int[] weights = {6, 5, 7, 2, 3, 4, 5, 6, 7};               // weights = wagi
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }
        int control = sum % 11;                                    // control = cyfra kontrolna
        return control != 10 && control == digits.charAt(9) - '0';
    }

    /** Rabat procentowy od liczby sztuk: 1–9 → 0%, 10–49 → 5%, 50–99 → 10%, od 100 → 15%; poniżej 1 → wyjątek. */
    static int discountPercent(int quantity) {   // discount percent = rabat w procentach; quantity = ilość
        if (quantity < 1) {
            throw new IllegalArgumentException("ilość musi być dodatnia: " + quantity);
        }
        if (quantity < 10) return 0;
        if (quantity < 50) return 5;
        if (quantity < 100) return 10;
        return 15;
    }

    /** Uproszczone stawki VAT (prawdziwe przepisy są bardziej złożone): żywność i książki 5%, reszta 23%. */
    static int vatRate(Category category) {      // vat rate = stawka VAT
        return switch (category) {               // wyrażenie switch (Java 14+)
            case SPOZYWCZE, KSIAZKI -> 5;
            default -> 23;
        };
    }

    // =================================================================================================
    // 1. @ParameterizedTest + @ValueSource
    // =================================================================================================

    /**
     * PRZED: trzy prawie identyczne testy różniące się tylko danymi. PO: jeden test sparametryzowany.
     * Atrybut {@code name} ustala nazwę każdego wywołania: {@code {index}} = numer, {@code {0}} = pierwszy argument.
     */
    static void valueSource() {
        section("1. Od kopiuj-wklej do @ParameterizedTest z @ValueSource");

        runTests(CopyPasteTests.class);
        // WYNIK: ✔ kod 00-950 jest poprawny
        // WYNIK: ✔ kod 31-042 jest poprawny
        // WYNIK: ✔ kod 80-001 jest poprawny
        // WYNIK: znalezione 3, zaliczone 3, niezaliczone 0, pominięte 0, przerwane 0

        runTests(ValueSourceTests.class);
        // WYNIK: ✔ poprawny kod pocztowy › [1] 00-950
        // WYNIK: ✔ poprawny kod pocztowy › [2] 31-042
        // WYNIK: ✔ poprawny kod pocztowy › [3] 80-001
        // WYNIK: ✔ zły kod pocztowy › [1] 00950
        // WYNIK: ✔ zły kod pocztowy › [2] 0-0950
        // WYNIK: ✔ zły kod pocztowy › [3] ab-cde
        // WYNIK: ✔ zły kod pocztowy › [4] 00-9500
        // WYNIK: znalezione 7, zaliczone 7, niezaliczone 0, pominięte 0, przerwane 0

        // Nowy przypadek = jedno słowo w tablicy, a nie nowa metoda. Każde wywołanie to osobny wynik w raporcie.
        // @ValueSource zna: strings, ints, longs, doubles, chars, booleans, classes, ... — zawsze JEDEN parametr.
        // PUŁAPKA: bez atrybutu name nazwa domyślna to „[1] 00-950”, a jeśli kompilator dostał opcję -parameters,
        // także z nazwą parametru („[1] code=00-950”). Wynik raportu zależy więc od ustawień kompilacji — w tej
        // lekcji zawsze podajemy name jawnie.
    }

    /** PRZED — kopiuj-wklej. Zmiana reguły = poprawianie trzech metod. */
    static class CopyPasteTests {
        @Test
        @DisplayName("kod 00-950 jest poprawny")
        void code1() { assertTrue(isValidPostalCode("00-950")); }

        @Test
        @DisplayName("kod 31-042 jest poprawny")
        void code2() { assertTrue(isValidPostalCode("31-042")); }

        @Test
        @DisplayName("kod 80-001 jest poprawny")
        void code3() { assertTrue(isValidPostalCode("80-001")); }
    }

    /** PO — jeden test na klasę danych poprawnych i jeden na błędne. */
    static class ValueSourceTests {

        @ParameterizedTest(name = "[{index}] {0}")
        @ValueSource(strings = {"00-950", "31-042", "80-001"})
        @DisplayName("poprawny kod pocztowy")
        void validCodes(String code) {
            assertTrue(isValidPostalCode(code));
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @ValueSource(strings = {"00950", "0-0950", "ab-cde", "00-9500"})
        @DisplayName("zły kod pocztowy")
        void invalidCodes(String code) {
            assertFalse(isValidPostalCode(code));
        }
    }

    // =================================================================================================
    // 2. @NullAndEmptySource
    // =================================================================================================

    /**
     * {@code @NullSource} dodaje wywołanie z null, {@code @EmptySource} — z pustym tekstem (albo pustą listą/tablicą),
     * {@code @NullAndEmptySource} — oba. Łączymy je z {@code @ValueSource}, żeby dorzucić np. same spacje.
     */
    static void nullAndEmpty() {
        section("2. @NullAndEmptySource — null, pusty tekst i spacje");

        runTests(NullAndEmptyTests.class);
        // WYNIK: ✔ pusty lub brak kodu jest odrzucany › [1] „null”
        // WYNIK: ✔ pusty lub brak kodu jest odrzucany › [2] „”
        // WYNIK: ✔ pusty lub brak kodu jest odrzucany › [3] „   ”
        // WYNIK: znalezione 3, zaliczone 3, niezaliczone 0, pominięte 0, przerwane 0

        // Cudzysłowy w name pozwalają ZOBACZYĆ pusty tekst i spacje — bez nich linie raportu wyglądałyby tak samo.
        // PUŁAPKA: null i "" to najczęstsze dane z formularzy i plików — a najrzadziej testowane. Dla parametrów
        // typu prostego (int) @NullSource nie zadziała — int nie może być null (błąd przy uruchamianiu testu).
    }

    static class NullAndEmptyTests {
        @ParameterizedTest(name = "[{index}] „{0}”")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("pusty lub brak kodu jest odrzucany")
        void rejectsBlank(String code) {
            assertFalse(isValidPostalCode(code));
        }
    }

    // =================================================================================================
    // 3. @CsvSource — WIELE ARGUMENTÓW
    // =================================================================================================

    /**
     * Każdy wiersz CSV (comma-separated values = wartości rozdzielone przecinkami) to jedno wywołanie, kolumny =
     * kolejne parametry. Tekst „true” zamieni się na boolean, „15” na int itd. Blok tekstu (Java 15+) w atrybucie
     * {@code textBlock} pozwala zapisać dane jak tabelkę, z własnym separatorem i nagłówkiem.
     */
    static void csvSource() {
        section("3. @CsvSource — wiele argumentów, {0}/{1} w nazwie, textBlock");

        runTests(CsvTests.class);
        // WYNIK: ✔ NIP (tabelka) › [1] NIP = 123-456-32-18, poprawny = true
        // WYNIK: ✔ NIP (tabelka) › [2] NIP = 1234567890, poprawny = false
        // WYNIK: ✔ NIP (tabelka) › [3] NIP = , poprawny = false
        // WYNIK: ✔ NIP: wiersze CSV › 1234563218 → true
        // WYNIK: ✔ NIP: wiersze CSV › 1234563219 → false
        // WYNIK: ✔ NIP: wiersze CSV › 7770001111 → true
        // WYNIK: ✔ NIP: wiersze CSV › 951234567 → false
        // WYNIK: znalezione 7, zaliczone 7, niezaliczone 0, pominięte 0, przerwane 0

        // 1234567890 → suma modulo 11 wynosi 10, więc żadna 10. cyfra nie jest poprawna (ciekawy przypadek brzegowy).
        // Trzeci wiersz tabelki: '' to pusty tekst (w nazwie widać „NIP = ” i nic dalej).
        // W CSV pusty tekst zapisujemy jako '' (apostrofy), a wartość pominiętą (np. „a,,b”) JUnit zamienia na null.
        // PUŁAPKA: przecinek w danych (np. „1,5”) rozbije kolumnę — ujmij wartość w apostrofy '1,5' albo zmień
        // separator (delimiter = '|'). {@code @CsvFileSource(resources = "/nip.csv", numLinesToSkip = 1)} czyta to
        // samo z pliku src/test/resources/nip.csv (numLinesToSkip = 1 → pomija wiersz nagłówka) — wygodne przy
        // dziesiątkach wierszy danych.
    }

    static class CsvTests {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "1234563218, true",
                "1234563219, false",     // zła cyfra kontrolna
                "7770001111, true",
                "951234567,  false"      // tylko 9 cyfr
        })
        @DisplayName("NIP: wiersze CSV")
        void nipRows(String nip, boolean expected) {        // „true” z CSV → boolean
            assertEquals(expected, isValidNip(nip));
        }

        @ParameterizedTest(name = "[{index}] {arguments}")
        @CsvSource(useHeadersInDisplayName = true, delimiter = '|', textBlock = """
                NIP           | poprawny
                123-456-32-18 | true
                1234567890    | false
                ''            | false
                """)
        @DisplayName("NIP (tabelka)")
        void nipTable(String nip, boolean expected) {
            assertEquals(expected, isValidNip(nip));
        }
    }

    // =================================================================================================
    // 4. @MethodSource — DOWOLNE OBIEKTY
    // =================================================================================================

    /**
     * Gdy dane to obiekty (rekordy, listy, BigDecimal), wskazujemy STATYCZNĄ metodę zwracającą
     * {@code Stream<Arguments>} — każdy {@code Arguments.of(...)} (albo skrót {@code arguments(...)}) to jedno wywołanie.
     * Dane z innej klasy: {@code @MethodSource("pakiet.Klasa#metoda")}.
     */
    static void methodSource() {
        section("4. @MethodSource — strumień Arguments z obiektami");

        runTests(MethodSourceTests.class);
        // WYNIK: ✔ dostępność produktu › Laptop Pro 14 (5499.99 zł) → dostępny: true
        // WYNIK: ✔ dostępność produktu › Smartfon X (2999.00 zł) → dostępny: false
        // WYNIK: ✔ dostępność produktu › Oliwa z oliwek (42.00 zł) → dostępny: false
        // WYNIK: ✔ dostępność produktu › Czysty kod (79.00 zł) → dostępny: true
        // WYNIK: ✔ wartość magazynu › [1] KSI-003 → 297.00
        // WYNIK: ✔ wartość magazynu › [2] DOM-001 → 3798.00
        // WYNIK: znalezione 6, zaliczone 6, niezaliczone 0, pominięte 0, przerwane 0

        // Kolejność wywołań = kolejność elementów strumienia (porządek alfabetyczny dotyczy metod, nie danych).
        // Nazwa wywołania używa toString() obiektu — rekord Product ma czytelny toString, więc raport też jest czytelny.
        // DOBRA PRAKTYKA: metoda-źródło obok testu, z nazwą mówiącą, co zawiera (np. outOfStockProducts).
        // PUŁAPKA: metoda-źródło musi być static (chyba że klasa ma @TestInstance(PER_CLASS)), a jej nazwa w adnotacji
        // to zwykły tekst — literówka wyjdzie dopiero przy uruchomieniu (błąd „Could not find factory method”).
    }

    static class MethodSourceTests {

        /** Źródło danych: produkt + oczekiwana dostępność (dwa z magazynem, dwa bez). */
        static Stream<Arguments> availability() {                 // availability = dostępność
            return Stream.of(
                    arguments(SampleData.productBySku("ELE-001"), true),
                    arguments(SampleData.productBySku("ELE-002"), false),
                    arguments(SampleData.productBySku("SPO-003"), false),
                    arguments(SampleData.productBySku("KSI-001"), true));
        }

        @ParameterizedTest(name = "{0} → dostępny: {1}")
        @MethodSource("availability")
        @DisplayName("dostępność produktu")
        void inStock(Product product, boolean expected) {
            assertEquals(expected, product.inStock());
        }

        static Stream<Arguments> stockValues() {                  // stock values = wartości magazynu
            return Stream.of(
                    arguments("KSI-003", new BigDecimal("297.00")),    // 99.00 × 3
                    arguments("DOM-001", new BigDecimal("3798.00")));  // 1899.00 × 2
        }

        @ParameterizedTest(name = "[{index}] {0} → {1}")
        @MethodSource("stockValues")
        @DisplayName("wartość magazynu")
        void stockValue(String sku, BigDecimal expected) {
            // BigDecimal porównujemy przez compareTo (equals patrzy też na skalę) — t15_numbers/Numbers01BigDecimal
            assertEquals(0, expected.compareTo(SampleData.productBySku(sku).stockValue()));
        }
    }

    // =================================================================================================
    // 5. @EnumSource
    // =================================================================================================

    /**
     * {@code @EnumSource(Category.class)} — wszystkie stałe; {@code names = {...}} — tylko wybrane;
     * {@code mode = EXCLUDE} — wszystkie OPRÓCZ wybranych. Dzięki temu nowa stała enuma automatycznie trafi do testu.
     */
    static void enumSource() {
        section("5. @EnumSource — names i mode = EXCLUDE");

        runTests(EnumSourceTests.class);
        // WYNIK: ✔ każda kategoria ma stawkę 5 albo 23 › [1] ELEKTRONIKA
        // WYNIK: ✔ każda kategoria ma stawkę 5 albo 23 › [2] SPOZYWCZE
        // WYNIK: ✔ każda kategoria ma stawkę 5 albo 23 › [3] KSIAZKI
        // WYNIK: ✔ każda kategoria ma stawkę 5 albo 23 › [4] ODZIEZ
        // WYNIK: ✔ każda kategoria ma stawkę 5 albo 23 › [5] DOM
        // WYNIK: ✔ stawka obniżona 5% › [1] SPOZYWCZE
        // WYNIK: ✔ stawka obniżona 5% › [2] KSIAZKI
        // WYNIK: ✔ stawka podstawowa 23% › [1] ELEKTRONIKA
        // WYNIK: ✔ stawka podstawowa 23% › [2] ODZIEZ
        // WYNIK: ✔ stawka podstawowa 23% › [3] DOM
        // WYNIK: znalezione 10, zaliczone 10, niezaliczone 0, pominięte 0, przerwane 0

        // DOBRA PRAKTYKA: „stawka podstawowa” przez EXCLUDE — gdy ktoś doda kategorię ZABAWKI, test sam ją obejmie
        // (i wyłapie, jeśli zapomniano o niej w switch). Lista names to zwykłe teksty: literówka = błąd przy uruchomieniu.
    }

    static class EnumSourceTests {

        @ParameterizedTest(name = "[{index}] {0}")
        @EnumSource(Category.class)
        @DisplayName("każda kategoria ma stawkę 5 albo 23")
        void everyCategoryHasRate(Category category) {
            int rate = vatRate(category);
            assertTrue(rate == 5 || rate == 23, () -> "stawka " + rate);
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @EnumSource(value = Category.class, names = {"SPOZYWCZE", "KSIAZKI"})
        @DisplayName("stawka obniżona 5%")
        void reducedRate(Category category) {
            assertEquals(5, vatRate(category));
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @EnumSource(value = Category.class, names = {"SPOZYWCZE", "KSIAZKI"}, mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("stawka podstawowa 23%")
        void standardRate(Category category) {
            assertEquals(23, vatRate(category));
        }
    }

    // =================================================================================================
    // 6. WARTOŚCI BRZEGOWE I KLASY RÓWNOWAŻNOŚCI
    // =================================================================================================

    /**
     * Jak wybrać dane? Dzielimy wejście na KLASY RÓWNOWAŻNOŚCI (zakresy, w których kod ma się zachowywać tak samo),
     * a z każdej bierzemy wartości BRZEGOWE (tuż przy granicach), bo tam najczęściej mieszkają błędy „o jeden”.
     * <pre>{@code
     *   ilość:   ... -1 0 | 1 ......... 9 | 10 ....... 49 | 50 ....... 99 | 100 ...
     *   klasa:    wyjątek |      0%       |      5%       |      10%      |   15%
     *   brzegi:     0     |   1       9   |  10       49  |  50       99  |  100
     * }</pre>
     * Pokazujemy to na zepsutej wersji rabatu (warunek {@code quantity < 51} zamiast {@code quantity < 50}): testy „ze środka” zakresów
     * są zielone, a błąd wyłapuje dopiero wartość brzegowa 50.
     */
    static void boundaryValues() {
        section("6. Wartości brzegowe i klasy równoważności");

        discountUnderTest = JUnit02Parameterized::buggyDiscount;
        note("zepsuty rabat — testy wartości ze środka zakresów:");
        // WYNIK: ℹ zepsuty rabat — testy wartości ze środka zakresów:
        runTests(MiddleValuesTests.class);
        // WYNIK: ✔ środek zakresów › 5 szt. → 0%
        // WYNIK: ✔ środek zakresów › 30 szt. → 5%
        // WYNIK: ✔ środek zakresów › 75 szt. → 10%
        // WYNIK: ✔ środek zakresów › 150 szt. → 15%
        // WYNIK: znalezione 4, zaliczone 4, niezaliczone 0, pominięte 0, przerwane 0

        note("zepsuty rabat — testy wartości brzegowych:");
        // WYNIK: ℹ zepsuty rabat — testy wartości brzegowych:
        runTests(BoundaryTests.class);
        // WYNIK: ✔ 0 szt. → wyjątek
        // WYNIK: ✔ brzegi zakresów › 1 szt. → 0%
        // WYNIK: ✔ brzegi zakresów › 9 szt. → 0%
        // WYNIK: ✔ brzegi zakresów › 10 szt. → 5%
        // WYNIK: ✔ brzegi zakresów › 49 szt. → 5%
        // WYNIK: ✘ brzegi zakresów › 50 szt. → 10% — AssertionFailedError: expected: <10> but was: <5>
        // WYNIK: ✔ brzegi zakresów › 99 szt. → 10%
        // WYNIK: ✔ brzegi zakresów › 100 szt. → 15%
        // WYNIK: znalezione 8, zaliczone 7, niezaliczone 1, pominięte 0, przerwane 0
        discountUnderTest = JUnit02Parameterized::discountPercent;

        // DOBRA PRAKTYKA: z każdej klasy co najmniej jedna wartość, a przy każdej granicy obie strony (49 i 50).
        // Dodatkowo „dziwne” wejścia: 0, wartości ujemne, Integer.MAX_VALUE, null, pusty tekst.
        // PUŁAPKA: więcej przypadków ≠ lepsze testy. 100 wartości ze środka zakresu nie wykryje tego błędu,
        // jedna wartość 50 — tak.
    }

    /** Testowana funkcja rabatu (discount under test = rabat pod testem) — podmieniamy ją na zepsutą wersję. */
    static IntUnaryOperator discountUnderTest = JUnit02Parameterized::discountPercent;

    /** Zepsuta wersja: {@code quantity < 51} zamiast {@code quantity < 50} — typowy błąd „o jeden” (off-by-one). */
    static int buggyDiscount(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("ilość musi być dodatnia: " + quantity);
        }
        if (quantity < 10) return 0;
        if (quantity < 51) return 5;      // BŁĄD
        if (quantity < 100) return 10;
        return 15;
    }

    static class MiddleValuesTests {
        @ParameterizedTest(name = "{0} szt. → {1}%")
        @CsvSource({"5, 0", "30, 5", "75, 10", "150, 15"})
        @DisplayName("środek zakresów")
        void middle(int quantity, int expectedPercent) {
            assertEquals(expectedPercent, discountUnderTest.applyAsInt(quantity));
        }
    }

    static class BoundaryTests {
        @ParameterizedTest(name = "{0} szt. → {1}%")
        @CsvSource({"1, 0", "9, 0", "10, 5", "49, 5", "50, 10", "99, 10", "100, 15"})
        @DisplayName("brzegi zakresów")
        void boundaries(int quantity, int expectedPercent) {
            assertEquals(expectedPercent, discountUnderTest.applyAsInt(quantity));
        }

        @Test
        @DisplayName("0 szt. → wyjątek")
        void zeroRejected() {
            assertThrows(IllegalArgumentException.class, () -> discountUnderTest.applyAsInt(0));
        }
    }

    // =================================================================================================
    // 7. ArgumentsAccessor I AGREGATORY
    // =================================================================================================

    /**
     * Gdy kolumn jest dużo, zamiast 6 parametrów można przyjąć jeden {@code ArgumentsAccessor} i czytać kolumny
     * po numerze ({@code getString(0)}, {@code getInteger(1)}, {@code get(2, BigDecimal.class)}). Jeszcze czytelniej:
     * własny {@code ArgumentsAggregator} składa wiersz w gotowy obiekt, a test dostaje np. rekord Product.
     */
    static void accessorsAndAggregators() {
        section("7. ArgumentsAccessor i @AggregateWith — wiersz CSV jako obiekt");

        runTests(AggregatorTests.class);
        // WYNIK: ✔ agregator: wiersz → Product › [1] X-1, Długopis, 2.50, 4
        // WYNIK: ✔ agregator: wiersz → Product › [2] X-2, Zeszyt, 6.00, 0
        // WYNIK: ✔ akcesor: wartość magazynu › [1] X-1, Długopis, 2.50, 4, 10.00
        // WYNIK: ✔ akcesor: wartość magazynu › [2] X-2, Zeszyt, 6.00, 0, 0.00
        // WYNIK: znalezione 4, zaliczone 4, niezaliczone 0, pominięte 0, przerwane 0

        // {arguments} w nazwie = wszystkie argumenty po przecinku.
        // DOBRA PRAKTYKA: akcesor/agregator stosuj dopiero przy wielu kolumnach; przy 2–3 zwykłe parametry są czytelniejsze.
    }

    /** Agregator: składa kolumny (sku, nazwa, cena, stan) w rekord Product (kategoria stała — to tylko przykład). */
    static class ProductAggregator implements ArgumentsAggregator {
        @Override
        public Object aggregateArguments(ArgumentsAccessor row, ParameterContext context) {
            return new Product(row.getString(0), row.getString(1), Category.DOM,
                    row.get(2, BigDecimal.class), row.getInteger(3));
        }
    }

    static class AggregatorTests {

        @ParameterizedTest(name = "[{index}] {arguments}")
        @CsvSource({"X-1, Długopis, 2.50, 4, 10.00", "X-2, Zeszyt, 6.00, 0, 0.00"})
        @DisplayName("akcesor: wartość magazynu")
        void withAccessor(ArgumentsAccessor row) {
            BigDecimal price = row.get(2, BigDecimal.class);       // tekst „2.50” → BigDecimal
            int stock = row.getInteger(3);
            BigDecimal expected = row.get(4, BigDecimal.class);
            assertEquals(0, expected.compareTo(price.multiply(BigDecimal.valueOf(stock))));
        }

        @ParameterizedTest(name = "[{index}] {arguments}")
        @CsvSource({"X-1, Długopis, 2.50, 4", "X-2, Zeszyt, 6.00, 0"})
        @DisplayName("agregator: wiersz → Product")
        void withAggregator(@AggregateWith(ProductAggregator.class) Product product) {
            assertEquals(product.stock() > 0, product.inStock());
        }
    }

    // =================================================================================================
    // 8. @RepeatedTest I TESTY DYNAMICZNE (@TestFactory)
    // =================================================================================================

    /**
     * {@code @RepeatedTest(n)} uruchamia ten sam test n razy ({@code RepetitionInfo} mówi, które to powtórzenie).
     * {@code @TestFactory} to metoda, która dopiero W CZASIE DZIAŁANIA tworzy testy ({@code DynamicTest}) — np. po
     * jednym na każdy produkt z listy. Liczba testów zależy więc od danych, a nie od liczby metod.
     */
    static void repeatedAndDynamic() {
        section("8. @RepeatedTest i testy dynamiczne (@TestFactory)");

        runTests(RepeatedAndDynamicTests.class);
        // WYNIK: ✔ losowa ilość ma rabat 0–15% › powtórzenie 1 z 3
        // WYNIK: ✔ losowa ilość ma rabat 0–15% › powtórzenie 2 z 3
        // WYNIK: ✔ losowa ilość ma rabat 0–15% › powtórzenie 3 z 3
        // WYNIK: ✔ produkty z ceną poniżej 100 zł › Kawa ziarnista 1kg ma cenę > 0
        // WYNIK: ✔ produkty z ceną poniżej 100 zł › Czekolada gorzka ma cenę > 0
        // WYNIK: ✔ produkty z ceną poniżej 100 zł › Oliwa z oliwek ma cenę > 0
        // WYNIK: ✔ produkty z ceną poniżej 100 zł › Czysty kod ma cenę > 0
        // WYNIK: ✔ produkty z ceną poniżej 100 zł › Wzorce projektowe ma cenę > 0
        // WYNIK: ✔ produkty z ceną poniżej 100 zł › T-shirt bawełniany ma cenę > 0
        // WYNIK: znalezione 9, zaliczone 9, niezaliczone 0, pominięte 0, przerwane 0

        // PUŁAPKA: @RepeatedTest z prawdziwą losowością (new Random() bez ziarna) daje testy, których porażki
        // nie da się powtórzyć. Tu ziarno = numer powtórzenia, więc każde uruchomienie losuje to samo.
        // PUŁAPKA: testy dynamiczne NIE przechodzą przez @BeforeEach/@AfterEach osobno — te metody wołane są raz,
        // wokół całej metody @TestFactory.
        // DOBRA PRAKTYKA: najpierw @ParameterizedTest; @TestFactory dopiero, gdy przypadki naprawdę trzeba wyliczyć.
    }

    static class RepeatedAndDynamicTests {

        @RepeatedTest(value = 3, name = "powtórzenie {currentRepetition} z {totalRepetitions}")
        @DisplayName("losowa ilość ma rabat 0–15%")
        void randomQuantity(RepetitionInfo info) {                    // repetition info = informacja o powtórzeniu
            Random random = new Random(info.getCurrentRepetition());  // ziarno → powtarzalne „losowanie”
            int quantity = 1 + random.nextInt(500);
            int percent = discountPercent(quantity);
            assertTrue(percent >= 0 && percent <= 15);
        }

        @TestFactory
        @DisplayName("produkty z ceną poniżej 100 zł")
        Stream<DynamicTest> cheapProducts() {
            return SampleData.products().stream()
                    .filter(p -> p.price().compareTo(new BigDecimal("100")) < 0)
                    .map(p -> DynamicTest.dynamicTest(p.name() + " ma cenę > 0",   // dynamicTest = test dynamiczny
                            () -> assertTrue(p.price().signum() > 0)));             // signum = znak liczby (1, 0, -1)
        }
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @ParameterizedTest(name = "[{index}] {0}") zamiast @Test + adnotacja źródła danych; name podawaj jawnie.
     *   • @ValueSource(strings/ints/...) — 1 parametr; @NullSource/@EmptySource/@NullAndEmptySource — null i "".
     *   • @CsvSource({"a, 1", "b, 2"}) — kilka parametrów, automatyczna konwersja; '' = pusty tekst, puste pole = null.
     *   • @CsvSource(textBlock = """...""", delimiter = '|', useHeadersInDisplayName = true) — dane jak tabelka.
     *   • @CsvFileSource(resources = "/plik.csv", numLinesToSkip = 1) — dane z src/test/resources.
     *   • @MethodSource("nazwa") → static Stream<Arguments>; arguments(obiekt, oczekiwane).
     *   • @EnumSource(Typ.class), names = {...}, mode = EXCLUDE — nowe stałe trafiają do testu same.
     *   • ArgumentsAccessor (getString/getInteger/get(i, Typ.class)), @AggregateWith(MójAgregator.class).
     *   • @RepeatedTest(n) + RepetitionInfo; @TestFactory → Stream<DynamicTest> (bez @BeforeEach na każdy).
     *   • Dobór danych: klasy równoważności + wartości brzegowe po obu stronach każdej granicy + null/0/puste.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się @Test od @ParameterizedTest i ile wyników da @ValueSource(ints = {1, 2, 3})?
     *   2. Co wypisze raport dla  @ParameterizedTest(name = "{1} dla {0}")  i wiersza  @CsvSource("ABC, 3")  ?
     *   3. ZNAJDŹ BŁĄD:
     *        @ParameterizedTest @MethodSource("data")
     *        void test(String s, int n) { ... }
     *        Stream<Arguments> data() { return Stream.of(arguments("a", 1)); }      // w zwykłej klasie testowej
     *   4. Rabat zmienia się przy 10, 50 i 100 sztukach. Jakie ilości wybierzesz do testu i dlaczego właśnie te?
     *   5. ZNAJDŹ BŁĄD:  @CsvSource({"1,5, 2,5"}) void test(double a, double b) — chcemy a = 1,5 i b = 2,5.
     *   6. Kiedy @EnumSource z mode = EXCLUDE jest lepszy od wypisania stałych w names?
     *   7. Kiedy sięgnąć po @TestFactory zamiast @ParameterizedTest?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: 4 złe kody pocztowe w @ValueSource",
                "znalezione 4, zaliczone 4, niezaliczone 0, pominięte 0, przerwane 0",
                () -> runQuietly(Exercise1Tests.class).brief());
        Check.equal("ćw. 2: PRZEPISZ 4 testy NIP na @CsvSource",
                "znalezione 4, zaliczone 4, niezaliczone 0, pominięte 0, przerwane 0",
                () -> runQuietly(Exercise2Tests.class).brief());
        Check.equal("ćw. 3: brzegi rabatu zabijają mutanty",
                "poprawna wersja: bez błędów; wykryte mutanty: 4 z 4",
                () -> mutationReport(Exercise3Tests.class));
        Check.equal("ćw. 4: walidator PESEL",
                List.of(true, true, false, false, false, false),
                () -> PESELS.stream().map(JUnit02Parameterized::exercise4).toList());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "znalezione 4, zaliczone 4, niezaliczone 0, pominięte 0, przerwane 0",
                () -> runQuietly(Solution1Tests.class).brief());
        Check.equal("ćw. 2 (wzorzec)", "znalezione 4, zaliczone 4, niezaliczone 0, pominięte 0, przerwane 0",
                () -> runQuietly(Solution2Tests.class).brief());
        Check.equal("ćw. 3 (wzorzec)", "poprawna wersja: bez błędów; wykryte mutanty: 4 z 4",
                () -> mutationReport(Solution3Tests.class));
        Check.equal("ćw. 4 (wzorzec)", List.of(true, true, false, false, false, false),
                () -> PESELS.stream().map(JUnit02Parameterized::solution4).toList());
        Check.summary();
        // WYNIK: ✔ OK    ćw. 1 (wzorzec)
        // WYNIK: ✔ OK    ćw. 2 (wzorzec)
        // WYNIK: ✔ OK    ćw. 3 (wzorzec)
        // WYNIK: ✔ OK    ćw. 4 (wzorzec)
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zastąp metodę todo() JEDNYM testem {@code @ParameterizedTest} z {@code @ValueSource},
     * który podaje DOKŁADNIE 4 różne błędne kody pocztowe (inne niż w sekcji 1) i sprawdza assertFalse.
     * Podpowiedź: pomyśl o literze w środku, spacji zamiast myślnika, myślniku w złym miejscu, zbyt krótkim kodzie.
     */
    static class Exercise1Tests {
        @Test
        void todo() {
            // TODO: twój @ParameterizedTest
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ cztery testy na JEDEN {@code @ParameterizedTest} z {@code @CsvSource}
     * (kolumny: nip, oczekiwany wynik):
     * <pre>{@code
     * @Test void nip1() { assertTrue(isValidNip("9512345675")); }
     * @Test void nip2() { assertTrue(isValidNip("106-000-00-04")); }
     * @Test void nip3() { assertFalse(isValidNip("9512345670")); }
     * @Test void nip4() { assertFalse(isValidNip("95123456751")); }
     * }</pre>
     * Podpowiedź: wiersz "9512345675, true"; parametry (String nip, boolean expected); name = "{0} → {1}".
     */
    static class Exercise2Tests {
        @Test
        void todo() {
            // TODO: twój @ParameterizedTest z @CsvSource
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 3 (średnie): napisz testy rabatu, wołając go WYŁĄCZNIE przez {@code discountUnderTest.applyAsInt(...)}.
     * Sprawdzarka uruchomi je na poprawnej wersji i na 4 mutantach: granice 10, 50 i 100 przesunięte o jeden
     * (np. {@code quantity <= 10} zamiast {@code quantity < 10}) oraz brak wyjątku dla ilości 0.
     * Każdy mutant musi oblać co najmniej jeden test.
     * Podpowiedź: jeden @ParameterizedTest z @CsvSource z wartościami brzegowymi + jeden @Test na wyjątek (sekcja 6).
     */
    static class Exercise3Tests {
        @Test
        void todo() {
            // TODO: twoje testy z discountUnderTest.applyAsInt(...)
            throw new UnsupportedOperationException("TODO");
        }
    }

    /** Dane do ćwiczenia 4: dwa poprawne, potem: zła cyfra kontrolna, 10 cyfr, litera, null. */
    private static final List<String> PESELS = Arrays.asList(   // asList dopuszcza null (List.of — nie)
            "90010100009", "85122405126", "90010100008", "9001010000", "9001010000A", null);

    /**
     * ĆWICZENIE 4 (trudniejsze): zaimplementuj walidator PESEL (bez sprawdzania daty): dokładnie 11 cyfr; cyfry 1–10
     * mnożymy przez wagi 1,3,7,9,1,3,7,9,1,3 i sumujemy; cyfra kontrolna = (10 - suma % 10) % 10 i musi równać się
     * 11. cyfrze. null → false. Numery w ćwiczeniu są wymyślone. Potem (dla siebie) napisz do niego @ParameterizedTest.
     * Podpowiedź: wzoruj się na isValidNip; {@code (c - '0')} zamienia znak cyfry na liczbę.
     */
    static boolean exercise4(String pesel) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Mutanty rabatu do ćwiczenia 3. */
    private static final List<IntUnaryOperator> MUTANTS = List.of(
            q -> q < 1 ? fail(q) : q <= 10 ? 0 : q < 50 ? 5 : q < 100 ? 10 : 15,
            q -> q < 1 ? fail(q) : q < 10 ? 0 : q <= 50 ? 5 : q < 100 ? 10 : 15,
            q -> q < 1 ? fail(q) : q < 10 ? 0 : q < 50 ? 5 : q <= 100 ? 10 : 15,
            q -> q < 10 ? 0 : q < 50 ? 5 : q < 100 ? 10 : 15);

    private static int fail(int quantity) {
        throw new IllegalArgumentException("ilość musi być dodatnia: " + quantity);
    }

    /** Uruchamia testy na poprawnej wersji rabatu i na każdym mutancie (jak w JUnit01Basics). */
    static String mutationReport(Class<?> testClass) {
        discountUnderTest = JUnit02Parameterized::discountPercent;
        RunResult onCorrect = runQuietly(testClass);
        int killed = 0;
        for (IntUnaryOperator mutant : MUTANTS) {
            discountUnderTest = mutant;
            if (runQuietly(testClass).failed() > 0) {
                killed++;
            }
        }
        discountUnderTest = JUnit02Parameterized::discountPercent;
        String correct = onCorrect.failed() == 0 && onCorrect.succeeded() > 0
                ? "bez błędów" : "niezaliczone " + onCorrect.failed() + " z " + onCorrect.found();
        return "poprawna wersja: " + correct + "; wykryte mutanty: " + killed + " z " + MUTANTS.size();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static class Solution1Tests {
        @ParameterizedTest(name = "[{index}] {0}")
        @ValueSource(strings = {"00-9a0", "00 950", "009-50", "0-95"})
        @DisplayName("błędne kody pocztowe")
        void invalid(String code) {
            assertFalse(isValidPostalCode(code));
        }
    }

    static class Solution2Tests {
        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({"9512345675, true", "106-000-00-04, true", "9512345670, false", "95123456751, false"})
        @DisplayName("walidacja NIP")
        void nip(String nip, boolean expected) {
            assertEquals(expected, isValidNip(nip));
        }
    }

    static class Solution3Tests {
        @ParameterizedTest(name = "{0} szt. → {1}%")
        @CsvSource({"1, 0", "9, 0", "10, 5", "49, 5", "50, 10", "99, 10", "100, 15"})
        @DisplayName("rabat na granicach")
        void boundaries(int quantity, int expectedPercent) {
            assertEquals(expectedPercent, discountUnderTest.applyAsInt(quantity));
        }

        @Test
        @DisplayName("0 szt. → wyjątek")
        void zeroRejected() {
            assertThrows(IllegalArgumentException.class, () -> discountUnderTest.applyAsInt(0));
        }
    }

    static boolean solution4(String pesel) {
        if (pesel == null || !pesel.matches("\\d{11}")) {
            return false;
        }
        int[] weights = {1, 3, 7, 9, 1, 3, 7, 9, 1, 3};
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += (pesel.charAt(i) - '0') * weights[i];
        }
        int control = (10 - sum % 10) % 10;
        return control == pesel.charAt(10) - '0';
    }

    // </editor-fold>

    // =================================================================================================
    // NARZĘDZIE LEKCJI: uruchamianie testów z main przez JUnit Platform Launcher (opis: JUnit01Basics)
    // =================================================================================================

    /** Liczniki jednego uruchomienia (found = znalezione, succeeded = zaliczone, failed = niezaliczone, ...). */
    record RunResult(long found, long succeeded, long failed, long skipped, long aborted) {
        String brief() {   // brief = krótki opis
            return String.format(Locale.ROOT, "znalezione %d, zaliczone %d, niezaliczone %d, pominięte %d, przerwane %d",
                    found, succeeded, failed, skipped, aborted);
        }
    }

    /** Uruchamia testy klasy i wypisuje po jednej linii na test oraz podsumowanie. */
    static RunResult runTests(Class<?> testClass) {
        return run(testClass, true);
    }

    /** Uruchamia testy bez wypisywania — tylko liczniki (dla sprawdzarek ćwiczeń). */
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

        /** „grupa › test” — bez nazwy samej klasy testowej. */
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
     *   1. @Test = jedna metoda, jedno wykonanie, bez parametrów. @ParameterizedTest = metoda z parametrami, wykonywana
     *      raz na każdy zestaw danych ze źródła; @ValueSource(ints = {1, 2, 3}) → 3 osobne wyniki w raporcie.
     *   2. „3 dla ABC” — {1} to drugi argument, {0} pierwszy (numeracja od zera).
     *   3. Metoda-źródło musi być static (w domyślnym trybie PER_METHOD JUnit woła ją bez obiektu klasy testowej):
     *      static Stream<Arguments> data(). Bez static test kończy się błędem przy uruchamianiu.
     *   4. Klasy: poniżej 1 (wyjątek), 1–9, 10–49, 50–99, od 100. Wartości: 0, 1, 9, 10, 49, 50, 99, 100 —
     *      po obu stronach każdej granicy, bo błędy „o jeden” (< zamiast <=) siedzą właśnie tam.
     *   5. Przecinek jest separatorem, więc wiersz ma 4 kolumny („1”, „5”, „2”, „5”), a test 2 parametry → błąd
     *      przy uruchomieniu. Do tego konwersja na double przyjmuje tylko kropkę (jak Double.parseDouble),
     *      więc apostrofy '1,5' nie pomogą. Poprawnie: @CsvSource({"1.5, 2.5"}).
     *   6. Gdy reguła dotyczy „wszystkich pozostałych” stałych — nowa stała enuma automatycznie trafi do testu,
     *      a lista names wymagałaby ręcznego dopisania (i łatwo o tym zapomnieć).
     *   7. Gdy przypadki trzeba wyliczyć w czasie działania (np. jeden test na każdy plik w katalogu albo każdy
     *      element listy z danych) lub gdy potrzebne są zagnieżdżone grupy (dynamicContainer). Do stałej tabelki
     *      danych wystarczy @ParameterizedTest — jest prostszy i lepiej wspierany przez narzędzia.
     */
    // </editor-fold>
}
