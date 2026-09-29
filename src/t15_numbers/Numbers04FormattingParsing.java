package t15_numbers;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Formatowanie i wczytywanie liczb — kropka czy przecinek, spacje, waluty, procenty
 *        (formatting = formatowanie, zamiana liczby na tekst; parsing = parsowanie, zamiana tekstu na liczbę)
 *
 * W SKRÓCIE:
 *   Ta sama liczba wygląda inaczej w Polsce (1 234,56) i w USA (1,234.56). O wyglądzie decydują
 *   ustawienia regionalne (Locale). Zawsze podawaj Locale jawnie — inaczej wynik zależy od komputera,
 *   na którym program działa. Przy wczytywaniu uważaj na przecinki, spacje i wyjątek NumberFormatException.
 *
 * ANALOGIA: ta sama data „03/04” to 3 kwietnia w Polsce i 4 marca w USA. Liczby mają ten sam kłopot:
 *   „1,500” to półtora u nas, a tysiąc pięćset w Ameryce. Locale to „paszport” mówiący, według
 *   czyich zwyczajów czytać i pisać liczbę.
 *
 * JAK TO DZIAŁA:
 *   LICZBA → TEKST                                         TEKST → LICZBA
 *   String.format(Locale, "%.2f", x)                       Integer.parseInt("42"), Double.parseDouble("1.5")
 *   NumberFormat.getNumberInstance(Locale) / ...Currency   NumberFormat.parse("1234,56")  (ParseException)
 *   new DecimalFormat("#,##0.00", symbole)                 new BigDecimal("1234.56")      (tylko kropka!)
 *   Locale.ROOT → „neutralny”: kropka, bez spacji;  pl-PL → przecinek, spacja twarda (U+00A0) między tysiącami
 *
 * SŁÓWKA:
 *   format = sformatuj; parse = przetwórz tekst na liczbę; locale = ustawienia regionalne; root = korzeń
 *   (Locale.ROOT = ustawienia neutralne); grouping = grupowanie (tysięcy); separator = znak oddzielający;
 *   decimal separator = separator dziesiętny; symbols = symbole; pattern = wzorzec; padding = dopełnianie;
 *   width = szerokość; flag = flaga (opcja); currency = waluta; percent = procent; unparseable = niemożliwy
 *   do przetworzenia; position = pozycja; non-breaking space (NBSP) = spacja twarda (nie łamie wiersza).
 *
 * ZOBACZ TEŻ: t04_strings/Strings04Formatting (podstawy String.format), t15_numbers/Numbers01BigDecimal,
 *             t15_numbers/Numbers02MoneyValueObject (Money.display), t17_datetime/DateTime03Formatting
 *             (formatowanie dat — te same problemy z Locale), t10_exceptions/Exceptions02CheckedUnchecked.
 * </pre>
 */
public class Numbers04FormattingParsing {

    static final Locale PL = Locale.forLanguageTag("pl-PL");          // forLanguageTag = z oznaczenia języka

    public static void main(String[] args) throws ParseException {   // ParseException — sekcja 6
        title("Numbers04 — formatowanie i wczytywanie liczb");

        formatWithLocale();         // format with locale = formatowanie z Locale
        flagsAndPadding();          // flags and padding = flagi i dopełnianie
        numberFormatPl();           // NumberFormat pl = NumberFormat po polsku
        decimalFormatPatterns();    // decimal format patterns = wzorce DecimalFormat
        parsingBasics();            // parsing basics = podstawy wczytywania
        numberFormatParse();        // NumberFormat parse = wczytywanie przez NumberFormat
        polishAmountToBigDecimal(); // Polish amount to BigDecimal = polska kwota na BigDecimal
        handlingErrors();           // handling errors = obsługa błędów
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // POMOCNIKI: spacje twarde
    // =================================================================================================

    /** visible = uwidocznij spacje twarde: U+00A0 → [NBSP], U+202F → [NNBSP] (wąska spacja twarda). */
    static String visible(String text) {
        return text.replace("\u00A0", "[NBSP]").replace("\u202F", "[NNBSP]");
    }

    /** normalSpaces = zamień spacje twarde na zwykłe (do porównań i wypisywania). */
    static String normalSpaces(String text) {
        return text.replace('\u00A0', ' ').replace('\u202F', ' ');
    }

    // =================================================================================================
    // 1. String.format Z JAWNYM Locale
    // =================================================================================================

    /**
     * 1. {@code String.format(format, ...)} bez Locale używa ustawień KOMPUTERA. Ten sam program wypisze
     * „1234,50” u Ciebie i „1234.50” na serwerze w Irlandii. Dlatego zawsze podawaj Locale:
     * {@code Locale.ROOT} dla danych technicznych (pliki, logi, API), pl-PL dla ludzi w Polsce.
     */
    static void formatWithLocale() {
        section("1. String.format z jawnym Locale");

        double value = 1234.5;
        show("Locale.ROOT", String.format(Locale.ROOT, "%.2f", value));
        show("pl-PL", String.format(PL, "%.2f", value));
        show("Locale.US", String.format(Locale.US, "%.2f", value));
        show("Locale.GERMANY", String.format(Locale.GERMANY, "%.2f", value));
        // WYNIK: Locale.ROOT → 1234.50
        // WYNIK: pl-PL → 1234,50
        // WYNIK: Locale.US → 1234.50
        // WYNIK: Locale.GERMANY → 1234,50

        // String.format("%.2f", value) — bez Locale: (wynik zależy od uruchomienia), dlatego go tu nie wypisujemy.

        // formatted (Java 15+) to wygodny skrót: "wzór".formatted(argumenty). Nie ma jednak wersji z Locale —
        // zawsze używa ustawień komputera. Bezpieczny dla napisów, dla ułamków wybierz String.format(Locale, ...).
        show("formatted z napisami", "%s kupiła %s szt.".formatted("Ala", "3"));
        // WYNIK: formatted z napisami → Ala kupiła 3 szt.

        // PUŁAPKA: plik CSV zapisany z Locale pl-PL ma „1234,50” — a przecinek to w CSV separator kolumn!
        // DOBRA PRAKTYKA: Locale.ROOT dla maszyn (pliki, JSON, logi), Locale użytkownika — dla ludzi.
    }

    // =================================================================================================
    // 2. FLAGI I DOPEŁNIANIE: %,.2f  %10.2f  %-10s  %08.2f
    // =================================================================================================

    /**
     * 2. Budowa znacznika: {@code %[flagi][szerokość][.precyzja]typ}.
     * Flagi: {@code ,} grupowanie tysięcy; {@code -} do lewej; {@code 0} dopełnij zerami; {@code +} zawsze znak.
     * Szerokość = minimalna liczba znaków (dopełniana spacjami) — tak buduje się równe kolumny.
     */
    static void flagsAndPadding() {
        section("2. Flagi i dopełnianie: %,.2f  %10.2f  %-10s  %08.2f");

        show("%,.2f ROOT", String.format(Locale.ROOT, "%,.2f", 1234567.891));
        String plGrouped = String.format(PL, "%,.2f", 1234567.891);
        show("%,.2f pl-PL (widoczne spacje)", visible(plGrouped));
        show("%,.2f pl-PL (po zamianie)", normalSpaces(plGrouped));
        // WYNIK: %,.2f ROOT → 1,234,567.89
        // WYNIK: %,.2f pl-PL (widoczne spacje) → 1[NBSP]234[NBSP]567,89
        // WYNIK: %,.2f pl-PL (po zamianie) → 1 234 567,89
        // Po polsku tysiące oddziela SPACJA TWARDA (U+00A0), a nie zwykła — więcej w sekcji 3.

        System.out.println(String.format(Locale.ROOT, "   [%8.2f] [%-8.2f] [%08.2f]", 3.14159, 3.14159, 3.14159));
        System.out.println(String.format(Locale.ROOT, "   [%+d] [%5d] [%-5d] [%-10s] [%d%%]", 5, 42, 42, "Ala", 23));
        // WYNIK: [    3.14] [3.14    ] [00003.14]
        // WYNIK: [+5] [   42] [42   ] [Ala       ] [23%]
        // %% wypisuje sam znak procentu.

        // Praktyka: równe kolumny w raporcie (%f działa też z BigDecimal):
        for (Product p : SampleData.products().subList(0, 4)) {
            System.out.println(String.format(Locale.ROOT, "   %-16s %10.2f zł %5d szt.", p.name(), p.price(), p.stock()));
        }
        // WYNIK: Laptop Pro 14       5499.99 zł     7 szt.
        // WYNIK: Smartfon X          2999.00 zł     0 szt.
        // WYNIK: Słuchawki BT         349.90 zł    25 szt.
        // WYNIK: Monitor 27 cali     1299.00 zł     4 szt.

        // PUŁAPKA: szerokość to MINIMUM. Dłuższy napis nie zostanie obcięty i rozjedzie kolumnę:
        System.out.println(String.format(Locale.ROOT, "   [%-5s] [%.5s]", "Czekolada", "Czekolada"));
        // WYNIK: [Czekolada] [Czeko]    ← %.5s obcina do 5 znaków
    }

    // =================================================================================================
    // 3. NumberFormat: liczba, waluta, procent (pl-PL)
    // =================================================================================================

    /**
     * 3. {@code NumberFormat} zna zwyczaje danego kraju: separator dziesiętny, grupowanie, symbol waluty,
     * format procentu. Tworzysz go metodami fabrycznymi: getNumberInstance, getCurrencyInstance, getPercentInstance.
     * <p>
     * NumberFormat NIE jest bezpieczny wątkowo (thread-safe) — nie współdziel jednego obiektu między wątkami
     * (t21_concurrency/Concurrency08ThreadSafetyPatterns).
     */
    static void numberFormatPl() {
        section("3. NumberFormat po polsku: liczba, waluta, procent");

        NumberFormat number = NumberFormat.getNumberInstance(PL);
        NumberFormat currency = NumberFormat.getCurrencyInstance(PL);
        NumberFormat percent = NumberFormat.getPercentInstance(PL);

        String rawCurrency = currency.format(1234.5);
        show("liczba", normalSpaces(number.format(1234567.891)));
        show("waluta (widoczne spacje)", visible(rawCurrency));
        show("waluta", normalSpaces(rawCurrency));
        show("waluta z BigDecimal 0.10", normalSpaces(currency.format(new BigDecimal("0.10"))));
        show("procent 0.23", percent.format(0.23));
        // WYNIK: liczba → 1 234 567,891    ← domyślnie najwyżej 3 miejsca po przecinku
        // WYNIK: waluta (widoczne spacje) → 1[NBSP]234,50[NBSP]zł
        // WYNIK: waluta → 1 234,50 zł
        // WYNIK: waluta z BigDecimal 0.10 → 0,10 zł
        // WYNIK: procent 0.23 → 23%    ← procent mnoży przez 100

        percent.setMinimumFractionDigits(1);                          // set minimum fraction digits = min. cyfr po przecinku
        show("procent 0.1234 z 1 miejscem", percent.format(0.1234));
        // WYNIK: procent 0.1234 z 1 miejscem → 12,3%

        // PUŁAPKA: wynik wygląda jak "1 234,50 zł", ale NIE jest mu równy — w środku są spacje twarde.
        show("\"1 234,50 zł\".equals(surowy wynik)", "1 234,50 zł".equals(rawCurrency));
        show("po zamianie spacji twardych", "1 234,50 zł".equals(normalSpaces(rawCurrency)));
        show("kod znaku między 1 a 234", (int) rawCurrency.charAt(1));
        // WYNIK: "1 234,50 zł".equals(surowy wynik) → false
        // WYNIK: po zamianie spacji twardych → true
        // WYNIK: kod znaku między 1 a 234 → 160    ← 160 = U+00A0 (zwykła spacja ma kod 32)
        // Spacja twarda nie pozwala złamać wiersza w środku liczby („1” na końcu linii, „234,50 zł” w następnej).
        // W niektórych wersjach Javy i językach (np. francuski) pojawia się też wąska spacja twarda U+202F.

        // DOBRA PRAKTYKA: w testach i porównaniach zamieniaj U+00A0 i U+202F na zwykłą spację (normalSpaces).
    }

    // =================================================================================================
    // 4. DecimalFormat: WŁASNE WZORCE "#,##0.00", "0.00", "#.##"
    // =================================================================================================

    /**
     * 4. {@code DecimalFormat} pozwala napisać własny wzorzec. Znaki we wzorcu:
     * <pre>
     *   0  — cyfra zawsze (brakujące uzupełnia zerem)      #  — cyfra, jeśli jest (zera pomija)
     *   .  — miejsce separatora dziesiętnego                ,  — miejsce separatora grup (tysięcy)
     * </pre>
     * Jakie znaki pojawią się w wyniku (przecinek czy kropka), mówią symbole: {@code DecimalFormatSymbols}.
     * Domyślny tryb zaokrąglania DecimalFormat to HALF_EVEN (bankierski)!
     */
    static void decimalFormatPatterns() {
        section("4. DecimalFormat: wzorce \"#,##0.00\", \"0.00\", \"#.##\"");

        DecimalFormatSymbols plSymbols = DecimalFormatSymbols.getInstance(PL);
        show("separator dziesiętny pl-PL", plSymbols.getDecimalSeparator());
        show("kod separatora grup pl-PL", (int) plSymbols.getGroupingSeparator());
        // WYNIK: separator dziesiętny pl-PL → ,
        // WYNIK: kod separatora grup pl-PL → 160

        DecimalFormat grouped = new DecimalFormat("#,##0.00", plSymbols);
        DecimalFormat fixed = new DecimalFormat("0.00", plSymbols);
        DecimalFormat optional = new DecimalFormat("#.##", plSymbols);
        show("#,##0.00 → 1234567.891", normalSpaces(grouped.format(1234567.891)));
        show("#,##0.00 → 0.5", grouped.format(0.5));
        show("0.00 → 5", fixed.format(5));
        show("#.## → 5", optional.format(5));
        show("#.## → 1.5", optional.format(1.5));
        show("#.## → 0.456", optional.format(0.456));
        // WYNIK: #,##0.00 → 1234567.891 → 1 234 567,89
        // WYNIK: #,##0.00 → 0.5 → 0,50
        // WYNIK: 0.00 → 5 → 5,00
        // WYNIK: #.## → 5 → 5
        // WYNIK: #.## → 1.5 → 1,5
        // WYNIK: #.## → 0.456 → 0,46

        // PUŁAPKA: DecimalFormat domyślnie zaokrągla HALF_EVEN, a nie „szkolnie”.
        BigDecimal x = new BigDecimal("0.125");
        show("0.00 → 0.125 (domyślnie HALF_EVEN)", fixed.format(x));
        fixed.setRoundingMode(RoundingMode.HALF_UP);
        show("0.00 → 0.125 (HALF_UP)", fixed.format(x));
        // WYNIK: 0.00 → 0.125 (domyślnie HALF_EVEN) → 0,12    ← 2 jest parzyste
        // WYNIK: 0.00 → 0.125 (HALF_UP) → 0,13

        // Własne symbole: zwykła spacja zamiast twardej — wynik od razu nadaje się do porównań.
        DecimalFormatSymbols custom = DecimalFormatSymbols.getInstance(PL);
        custom.setGroupingSeparator(' ');                             // set grouping separator = ustaw separator grup
        DecimalFormat plain = new DecimalFormat("#,##0.00", custom);
        show("własne symbole, zwykła spacja", plain.format(1234567.891));
        show("   równe \"1 234 567,89\"?", "1 234 567,89".equals(plain.format(1234567.891)));
        // WYNIK: własne symbole, zwykła spacja → 1 234 567,89
        // WYNIK: równe "1 234 567,89"? → true

        // DOBRA PRAKTYKA: kwoty na fakturze — "#,##0.00" + setRoundingMode(HALF_UP); a jeszcze lepiej zaokrąglij
        //   BigDecimal (setScale) PRZED formatowaniem, żeby formatowanie tylko „ubierało” gotową liczbę.
    }

    // =================================================================================================
    // 5. WCZYTYWANIE: Integer.parseInt, Double.parseDouble
    // =================================================================================================

    /**
     * 5. {@code Integer.parseInt} i {@code Double.parseDouble} rozumieją TYLKO format „komputerowy”: cyfry,
     * opcjonalny minus, kropka jako separator. Każde odstępstwo → NumberFormatException (wyjątek niesprawdzany).
     */
    static void parsingBasics() {
        section("5. Wczytywanie: Integer.parseInt i Double.parseDouble");

        show("Integer.parseInt(\"42\")", Integer.parseInt("42"));     // parseInt = przetwórz na int
        show("Integer.parseInt(\"-7\")", Integer.parseInt("-7"));
        show("Double.parseDouble(\"1.5\")", Double.parseDouble("1.5"));
        show("Double.parseDouble(\"1e3\")", Double.parseDouble("1e3"));
        // WYNIK: Integer.parseInt("42") → 42
        // WYNIK: Integer.parseInt("-7") → -7
        // WYNIK: Double.parseDouble("1.5") → 1.5
        // WYNIK: Double.parseDouble("1e3") → 1000.0    ← notacja naukowa: 1 × 10^3

        expectThrows("Double.parseDouble(\"1,5\")", () -> Double.parseDouble("1,5"));
        // WYNIK: ✔ Double.parseDouble("1,5") → rzucono NumberFormatException: For input string: "1,5"
        // Polak wpisze „1,5” w formularzu — i program padnie. Trzeba to obsłużyć (sekcje 6–8).

        expectThrows("Integer.parseInt(\" 42\")", () -> Integer.parseInt(" 42"));
        expectThrows("Integer.parseInt(\"4 200\")", () -> Integer.parseInt("4 200"));
        expectThrows("Integer.parseInt(\"2147483648\")", () -> Integer.parseInt("2147483648"));
        // WYNIK: ✔ Integer.parseInt(" 42") → rzucono NumberFormatException: For input string: " 42"
        // WYNIK: ✔ Integer.parseInt("4 200") → rzucono NumberFormatException: For input string: "4 200"
        // WYNIK: ✔ Integer.parseInt("2147483648") → rzucono NumberFormatException: For input string: "2147483648"
        // Ostatni: liczba o 1 za duża na int (Integer.MAX_VALUE = 2147483647).

        // PUŁAPKA: niekonsekwencja JDK — parseDouble ignoruje spacje na brzegach, a parseInt nie!
        show("Double.parseDouble(\" 1.5 \")", Double.parseDouble(" 1.5 "));
        show("Integer.parseInt(\" 42 \".strip())", Integer.parseInt(" 42 ".strip()));
        // WYNIK: Double.parseDouble(" 1.5 ") → 1.5
        // WYNIK: Integer.parseInt(" 42 ".strip()) → 42    ← strip (Java 11+) = usuń białe znaki z brzegów

        // DOBRA PRAKTYKA: dane od człowieka najpierw „oczyść” (strip, zamiana przecinka), potem parsuj.
    }

    // =================================================================================================
    // 6. NumberFormat.parse — POLSKI ZAPIS
    // =================================================================================================

    /**
     * 6. {@code NumberFormat.parse} rozumie zapis danego kraju („1234,56” dla pl-PL). Uwaga:
     * <ul>
     *   <li>rzuca ParseException — wyjątek SPRAWDZANY (checked), stąd {@code throws ParseException},</li>
     *   <li>jest „tolerancyjny”: czyta od początku, dopóki może, a resztę tekstu po cichu IGNORUJE,</li>
     *   <li>zwraca Number — Long albo Double (nie BigDecimal!), zależnie od tekstu.</li>
     * </ul>
     */
    static void numberFormatParse() throws ParseException {
        section("6. NumberFormat.parse — polski zapis");

        NumberFormat plNumber = NumberFormat.getNumberInstance(PL);
        show("parse(\"1234,56\")", plNumber.parse("1234,56"));
        show("parse(\"42\") — typ wyniku", plNumber.parse("42").getClass().getSimpleName());
        // WYNIK: parse("1234,56") → 1234.56
        // WYNIK: parse("42") — typ wyniku → Long

        show("parse(\"1 234,56\") — zwykła spacja", plNumber.parse("1 234,56"));
        show("parse(... spacja twarda U+00A0 ...)", plNumber.parse("1\u00A0234,56"));
        show("parse(\"12,5 zł\")", plNumber.parse("12,5 zł"));
        // WYNIK: parse("1 234,56") — zwykła spacja → 1    ← zatrzymał się na spacji, reszta zignorowana!
        // WYNIK: parse(... spacja twarda U+00A0 ...) → 1234.56
        // WYNIK: parse("12,5 zł") → 12.5    ← „ zł” po cichu pominięte

        expectThrows("parse(\"abc\")", () -> plNumber.parse("abc"));
        // WYNIK: ✔ parse("abc") → rzucono ParseException: Unparseable number: "abc"

        // Ścisłe sprawdzenie: ParsePosition mówi, dokąd parser doczytał. Cały tekst = indeks równy długości.
        String input = "12,5 zł";
        ParsePosition position = new ParsePosition(0);                // position = pozycja startowa
        Number parsed = plNumber.parse(input, position);
        show("doczytano do indeksu / długość tekstu", position.getIndex() + " / " + input.length() + " (liczba " + parsed + ")");
        // WYNIK: doczytano do indeksu / długość tekstu → 4 / 7 (liczba 12.5)    ← 4 < 7, więc tekst NIE był samą liczbą

        // PUŁAPKA: "1 234,56" wpisane przez człowieka (zwykła spacja) daje 1 — bez żadnego wyjątku!
        // DOBRA PRAKTYKA: do danych od ludzi używaj parse z ParsePosition (i sprawdzaj indeks) albo własnej
        //   metody z sekcji 7. Double z parse nie nadaje się do pieniędzy — potrzebny BigDecimal.
    }

    // =================================================================================================
    // 7. „1 234,56” → BigDecimal BEZPIECZNIE
    // =================================================================================================

    /**
     * 7. Dwa sposoby na polską kwotę jako BigDecimal:
     * <ol>
     *   <li>Oczyść tekst (usuń wszystkie rodzaje spacji, zamień przecinek na kropkę) i użyj {@code new BigDecimal}.
     *       Proste, dokładne, a zły format kończy się NumberFormatException.</li>
     *   <li>{@code DecimalFormat} z {@code setParseBigDecimal(true)} — zwraca BigDecimal zamiast Double.
     *       Dobre, gdy format jest ściśle określony (np. plik z banku ze spacjami twardymi).</li>
     * </ol>
     */
    static void polishAmountToBigDecimal() throws ParseException {
        section("7. „1 234,56” → BigDecimal bezpiecznie");

        String[] inputs = {"1 234,56", "1\u00A0234,56", " 99,9 ", "1234.56", "0,05"};
        for (String in : inputs) {
            show("\"" + visible(in) + "\"", parsePolishAmount(in));
        }
        // WYNIK: "1 234,56" → 1234.56
        // WYNIK: "1[NBSP]234,56" → 1234.56
        // WYNIK: " 99,9 " → 99.9
        // WYNIK: "1234.56" → 1234.56    ← kropkę też przyjmujemy
        // WYNIK: "0,05" → 0.05

        expectThrows("parsePolishAmount(\"12,3,4\")", () -> parsePolishAmount("12,3,4"));
        expectThrows("parsePolishAmount(\"1.234,56\")", () -> parsePolishAmount("1.234,56"));
        expectThrows("parsePolishAmount(\"abc\")", () -> parsePolishAmount("abc"));
        expectThrows("parsePolishAmount(null)", () -> parsePolishAmount(null));
        // WYNIK: ✔ parsePolishAmount("12,3,4") → rzucono NumberFormatException: Character array contains more than one decimal point.
        // WYNIK: ✔ parsePolishAmount("1.234,56") → rzucono NumberFormatException: Character array contains more than one decimal point.
        // WYNIK: ✔ parsePolishAmount("abc") → rzucono NumberFormatException: Character a is neither a decimal digit number, decimal point, nor "e" notation exponential mark.
        // WYNIK: ✔ parsePolishAmount(null) → rzucono NumberFormatException: Brak tekstu kwoty (null)
        // „1.234,56” (kropka jako separator tysięcy) odrzucamy — lepiej wyjątek niż zgadywanie.

        // Sposób 2: DecimalFormat zwracający BigDecimal.
        DecimalFormat bankFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(PL));
        bankFormat.setParseBigDecimal(true);                          // set parse BigDecimal = zwracaj BigDecimal
        Number fromBank = bankFormat.parse("1\u00A0234,56");
        show("DecimalFormat.parse → typ i wartość", fromBank.getClass().getSimpleName() + " " + fromBank);
        // WYNIK: DecimalFormat.parse → typ i wartość → BigDecimal 1234.56

        // DOBRA PRAKTYKA: pieniądze z tekstu → od razu BigDecimal. Nigdy przez double (Numbers01BigDecimal).
    }

    /**
     * parsePolishAmount = wczytaj polską kwotę. Usuwa spacje (zwykłe i twarde), zamienia przecinek na kropkę.
     * Zły format → NumberFormatException (z konstruktora BigDecimal).
     */
    static BigDecimal parsePolishAmount(String text) {
        if (text == null) {
            throw new NumberFormatException("Brak tekstu kwoty (null)");
        }
        String clean = text.replace(" ", "")
                .replace("\u00A0", "")
                .replace("\u202F", "")
                .replace(',', '.');
        return new BigDecimal(clean);
    }

    // =================================================================================================
    // 8. OBSŁUGA NumberFormatException
    // =================================================================================================

    /**
     * 8. NumberFormatException to wyjątek NIESPRAWDZANY (dziedziczy po IllegalArgumentException), więc kompilator
     * nie zmusi Cię do jego obsługi. Dane od użytkownika to jednak miejsce, gdzie błędy są NORMALNE.
     * Tu łapiemy wyjątek i zwracamy Optional (t14_optional/Optional01Basics): pusty = „to nie była liczba”.
     */
    static void handlingErrors() {
        section("8. Obsługa NumberFormatException — wynik jako Optional");

        expectThrows("Integer.parseInt(null)", () -> Integer.parseInt(null));
        // WYNIK: ✔ Integer.parseInt(null) → rzucono NumberFormatException: Cannot parse null string
        // Tłumaczenie: „nie można przetworzyć napisu null”.

        for (String input : Arrays.asList("42", " -7 ", "4 2", "", null, "12.5", "99999999999")) {
            show(input == null ? "null" : "\"" + input + "\"", parseIntOrEmpty(input));
        }
        // WYNIK: "42" → Optional[42]
        // WYNIK: " -7 " → Optional[-7]
        // WYNIK: "4 2" → Optional.empty
        // WYNIK: "" → Optional.empty
        // WYNIK: null → Optional.empty
        // WYNIK: "12.5" → Optional.empty    ← to nie jest liczba CAŁKOWITA
        // WYNIK: "99999999999" → Optional.empty    ← za duża na int

        // Komunikat dla człowieka zamiast stosu wywołań:
        String userInput = "dwa";
        try {
            int quantity = Integer.parseInt(userInput);
            show("ilość", quantity);
        } catch (NumberFormatException e) {
            show("komunikat dla użytkownika", "„" + userInput + "” to nie jest liczba. Wpisz np. 2.");
        }
        // WYNIK: komunikat dla użytkownika → „dwa” to nie jest liczba. Wpisz np. 2.

        // PUŁAPKA: catch (Exception e) {} „na wszelki wypadek” połyka też prawdziwe błędy programu.
        // DOBRA PRAKTYKA: łap KONKRETNIE NumberFormatException, możliwie blisko miejsca wczytania,
        //   i powiedz użytkownikowi, co jest nie tak i jak to poprawić.
    }

    /** parseIntOrEmpty = wczytaj int albo zwróć pusty Optional (null, puste, nie-liczba, poza zakresem). */
    static Optional<Integer> parseIntOrEmpty(String text) {
        if (text == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(text.strip()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   String.format(Locale.ROOT, "%.2f", x) → 1234.50   |  String.format(PL, "%.2f", x) → 1234,50
     *   Flagi: %,.2f grupowanie; %10.2f szerokość 10; %-10s do lewej; %08.2f zera; %+d znak; %% procent; %.5s obcina
     *   "wzór".formatted(...) (Java 15+) — zawsze Locale komputera → dla ułamków lepiej String.format(Locale, ...)
     *   NumberFormat.getNumberInstance / getCurrencyInstance / getPercentInstance(Locale) — nie thread-safe
     *   pl-PL: separator grup = spacja twarda U+00A0 (czasem U+202F) → zamieniaj na ' ' przed porównaniem
     *   DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(PL)); 0 = cyfra zawsze, # = cyfra jeśli jest
     *   DecimalFormat zaokrągla HALF_EVEN → setRoundingMode(RoundingMode.HALF_UP)
     *   Integer.parseInt / Double.parseDouble — tylko kropka; parseInt nie toleruje spacji → strip()
     *   NumberFormat.parse — ParseException (checked), ignoruje resztę tekstu → sprawdzaj ParsePosition
     *   Kwota z tekstu → BigDecimal: usuń spacje, ',' → '.', new BigDecimal(...) albo setParseBigDecimal(true)
     *   NumberFormatException — niesprawdzany; łap konkretnie, blisko wejścia; wynik jako Optional
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego String.format("%.2f", x) bez Locale to błąd, choć „u mnie działa”?
     *   2. Co wypisze:  System.out.println(String.format(Locale.ROOT, "[%6.1f|%-4d]", 2.25, 7));  ?
     *   3. ZNAJDŹ BŁĄD:
     *          String price = NumberFormat.getCurrencyInstance(PL).format(9.99);
     *          assert price.equals("9,99 zł");     // test ciągle nie przechodzi
     *   4. ZNAJDŹ BŁĄD:  BigDecimal amount = new BigDecimal(userInput);   // użytkownik wpisał „12,50”
     *   5. Co zwróci  NumberFormat.getNumberInstance(PL).parse("1 500,00")  (zwykła spacja) i dlaczego to groźne?
     *   6. Co wypisze:  System.out.println(new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ROOT))
     *                          .format(new BigDecimal("2.345")));  ?
     *   7. Który wyjątek rzuca Integer.parseInt, a który NumberFormat.parse? Czym się różnią?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Uruchamia ćwiczenia: najpierw TWOJE rozwiązania (✘ dopóki nie uzupełnisz), potem wzorcowe (✔). */
    static void exercises() {
        Product laptop = SampleData.products().get(0);
        Product chocolate = SampleData.products().get(5);

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: 1234.5 po polsku", "1234,50 zł", () -> exercise1(new BigDecimal("1234.5")));
        Check.equal("ćw. 1b: 7.49 po polsku", "7,49 zł", () -> exercise1(new BigDecimal("7.49")));
        Check.equal("ćw. 2a: \"42\"", 42, () -> exercise2("42", -1));
        Check.equal("ćw. 2b: \"4 2\" → domyślna", -1, () -> exercise2("4 2", -1));
        Check.equal("ćw. 2c: null → domyślna", -1, () -> exercise2(null, -1));
        Check.equal("ćw. 3a: wiersz — laptop", "Laptop Pro 14       |   5499.99|    7", () -> exercise3(laptop));
        Check.equal("ćw. 3b: wiersz — czekolada", "Czekolada gorzka    |      7.49|  300", () -> exercise3(chocolate));
        Check.equal("ćw. 4a: \"1 234,56\"", new BigDecimal("1234.56"), () -> exercise4("1 234,56"));
        Check.equal("ćw. 4b: \" 12 000 \"", new BigDecimal("12000"), () -> exercise4(" 12 000 "));
        Check.equal("ćw. 4c: spacja twarda", new BigDecimal("1234.56"), () -> exercise4("1\u00A0234,56"));
        Check.equal("ćw. 5a: 123456789 gr", "1 234 567,89 zł", () -> exercise5(123_456_789L));
        Check.equal("ćw. 5b: 5 gr", "0,05 zł", () -> exercise5(5L));
        Check.equal("ćw. 5c: -150 gr", "-1,50 zł", () -> exercise5(-150L));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", "1234,50 zł", () -> solution1(new BigDecimal("1234.5")));
        Check.equal("ćw. 1b (wzorzec)", "7,49 zł", () -> solution1(new BigDecimal("7.49")));
        Check.equal("ćw. 2a (wzorzec)", 42, () -> solution2("42", -1));
        Check.equal("ćw. 2b (wzorzec)", -1, () -> solution2("4 2", -1));
        Check.equal("ćw. 2c (wzorzec)", -1, () -> solution2(null, -1));
        Check.equal("ćw. 3a (wzorzec)", "Laptop Pro 14       |   5499.99|    7", () -> solution3(laptop));
        Check.equal("ćw. 3b (wzorzec)", "Czekolada gorzka    |      7.49|  300", () -> solution3(chocolate));
        Check.equal("ćw. 4a (wzorzec)", new BigDecimal("1234.56"), () -> solution4("1 234,56"));
        Check.equal("ćw. 4b (wzorzec)", new BigDecimal("12000"), () -> solution4(" 12 000 "));
        Check.equal("ćw. 4c (wzorzec)", new BigDecimal("1234.56"), () -> solution4("1\u00A0234,56"));
        Check.equal("ćw. 5a (wzorzec)", "1 234 567,89 zł", () -> solution5(123_456_789L));
        Check.equal("ćw. 5b (wzorzec)", "0,05 zł", () -> solution5(5L));
        Check.equal("ćw. 5c (wzorzec)", "-1,50 zł", () -> solution5(-150L));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 13 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): sformatuj kwotę po polsku: 2 miejsca po przecinku, przecinek, BEZ grupowania tysięcy,
     * na końcu „ zł”. Przykład: 1234.5 → "1234,50 zł".
     * Podpowiedź: {@code String.format(PL, "%.2f zł", amount)} — %f przyjmuje także BigDecimal.
     */
    static String exercise1(BigDecimal amount) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (łatwe): wczytaj int z tekstu; gdy się nie da (null, spacje w środku, litery) — zwróć defaultValue.
     * Podpowiedź: try { Integer.parseInt(...) } catch (NumberFormatException e) { ... }. null obsłuż osobnym if-em
     * (albo zauważ, że parseInt(null) też rzuca NumberFormatException).
     */
    static int exercise2(String text, int defaultValue) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): zbuduj wiersz tabeli: nazwa do lewej na 20 znaków, pionowa kreska, cena do prawej
     * na 10 znaków z 2 miejscami po KROPCE, kreska, stan do prawej na 5 znaków.
     * Przykład: {@code "Laptop Pro 14       |   5499.99|    7"}.
     * Podpowiedź: {@code String.format(Locale.ROOT, "%-20s|%10.2f|%5d", ...)}.
     */
    static String exercise3(Product product) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 4 (średnie): zamień polski zapis kwoty na BigDecimal. Tekst może mieć spacje na brzegach i spacje
     * (zwykłe lub twarde U+00A0) między tysiącami, a przecinek jako separator dziesiętny.
     * Podpowiedź: usuń zwykłe spacje i znaki o kodzie U+00A0 (replace), zamień ',' na '.', potem new BigDecimal(...).
     * Porównanie przez Check.equal jest „po wartości”, więc 12000 i 12000.00 będą równe.
     */
    static BigDecimal exercise4(String text) {
        // TODO: twoje rozwiązanie
        return BigDecimal.ZERO;
    }

    /**
     * ĆWICZENIE 5 (trudniejsze, łączy formatowanie z kwotą w groszach): kwotę w groszach (long) sformatuj po polsku
     * z grupowaniem tysięcy ZWYKŁĄ spacją: 123456789 → "1 234 567,89 zł", 5 → "0,05 zł", -150 → "-1,50 zł".
     * Podpowiedź: {@code BigDecimal.valueOf(grosze, 2)} zamienia grosze na złote. Potem NumberFormat.getCurrencyInstance(PL)
     * + zamiana spacji twardych, albo DecimalFormat("#,##0.00") z własnymi symbolami (zwykła spacja) + " zł".
     */
    static String exercise5(long grosze) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(BigDecimal amount) {
        return String.format(PL, "%.2f zł", amount);
    }

    static int solution2(String text, int defaultValue) {
        if (text == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(text.strip());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    static String solution3(Product product) {
        return String.format(Locale.ROOT, "%-20s|%10.2f|%5d", product.name(), product.price(), product.stock());
    }

    static BigDecimal solution4(String text) {
        return parsePolishAmount(text.strip());                       // metoda z sekcji 7
    }

    static String solution5(long grosze) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(PL);
        symbols.setGroupingSeparator(' ');                            // zwykła spacja zamiast twardej
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return format.format(BigDecimal.valueOf(grosze, 2)) + " zł";
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bez Locale format używa ustawień komputera. U Ciebie (pl-PL) wyjdzie „3,14”, na serwerze (en-US) „3.14”
     *      — plik lub raport będzie wyglądał inaczej w zależności od maszyny. Podawaj Locale.ROOT albo konkretny.
     *   2. „[   2.3|7   ]” — %6.1f: szerokość 6, jedno miejsce (2.25 → 2.3, bo formatowanie zaokrągla HALF_UP),
     *      %-4d: do lewej na 4 znakach.
     *   3. NumberFormat dla pl-PL wstawia spację twardą (U+00A0) przed „zł”, a w teście jest zwykła spacja.
     *      Poprawnie: porównuj po zamianie price.replace('\u00A0', ' ').replace('\u202F', ' ').
     *   4. new BigDecimal rozumie tylko kropkę — „12,50” rzuci NumberFormatException. Najpierw oczyść tekst:
     *      zamień przecinek na kropkę i usuń spacje (parsePolishAmount z sekcji 7).
     *   5. Zwróci 1 (Long) — parse zatrzyma się na zwykłej spacji i po cichu zignoruje resztę. Groźne, bo zamiast
     *      błędu program przyjmie kwotę 1500 razy mniejszą. Pomaga ParsePosition (sprawdź indeks) lub sekcja 7.
     *   6. „2.34” — DecimalFormat domyślnie zaokrągla HALF_EVEN (4 jest parzyste). Z HALF_UP byłoby „2.35”.
     *   7. parseInt → NumberFormatException (niesprawdzany, nie trzeba go deklarować); NumberFormat.parse →
     *      ParseException (sprawdzany — try/catch albo throws są obowiązkowe).
     */
    // </editor-fold>
}
