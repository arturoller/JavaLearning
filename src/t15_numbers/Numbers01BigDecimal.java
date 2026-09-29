package t15_numbers;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Product;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: BigDecimal — dokładne liczby dziesiętne (pieniądze, stawki, ilości)
 *        (big decimal = duża liczba dziesiętna; rounding mode = tryb zaokrąglania; scale = skala)
 *
 * W SKRÓCIE:
 *   double liczy w systemie dwójkowym i nie potrafi dokładnie zapisać nawet 0.1. Dla pieniędzy to
 *   niedopuszczalne. BigDecimal przechowuje liczbę jako cyfry dziesiętne (jak na papierze), więc 0.1 + 0.2
 *   daje dokładnie 0.3. Płacisz za to wygodą: zamiast + - * / wywołujesz metody i sam decydujesz o zaokrąglaniu.
 *
 * ANALOGIA: spróbuj zapisać 1/3 jako ułamek dziesiętny: 0.3333... — nigdy nie skończysz, więc gdzieś utniesz.
 *   Komputer z typem double ma ten sam kłopot z 0.1 — w systemie dwójkowym to ułamek nieskończony.
 *   BigDecimal to księgowa z kartką w kratkę: zapisuje dokładnie te cyfry, które podałeś, a przy dzieleniu
 *   pyta: „do ilu miejsc po przecinku i jak zaokrąglić?”.
 *
 * JAK TO DZIAŁA:
 *   BigDecimal = unscaledValue × 10^(−scale)   (wartość bez przecinka + informacja, gdzie stoi przecinek)
 *     new BigDecimal("123.45")  →  unscaledValue = 12345, scale = 2
 *   Operacje zwracają NOWY obiekt (BigDecimal jest niezmienny — immutable):
 *     a.add(b)  a.subtract(b)  a.multiply(b)  a.divide(b, skala, RoundingMode.HALF_UP)
 *   Skala wyniku:  add/subtract → większa ze skal;  multiply → suma skal;  divide → trzeba podać!
 *   Porównywanie:  a.compareTo(b) == 0  (wartość),  a.equals(b)  (wartość I skala: 2.0 ≠ 2.00)
 *
 * SŁÓWKA:
 *   decimal = dziesiętny; add = dodaj; subtract = odejmij; multiply = pomnóż; divide = podziel;
 *   scale = skala (cyfry po przecinku); precision = precyzja (wszystkie cyfry znaczące); unscaled = bez skali;
 *   rounding = zaokrąglanie; half up = połówka w górę; half even = połówka do parzystej; ceiling = sufit;
 *   floor = podłoga; math context = kontekst obliczeń (precyzja + tryb); strip trailing zeros = usuń zera
 *   na końcu; plain string = zwykły napis (bez notacji E); signum = znak liczby; negate = zmień znak;
 *   abs = wartość bezwzględna; non-terminating = nieskończone; rounding necessary = zaokrąglenie konieczne.
 *
 * ZOBACZ TEŻ: t01_basics/Basics08FloatingPoint (jak działa double), t15_numbers/Numbers02MoneyValueObject
 *             (typ Money zbudowany na BigDecimal), t15_numbers/Numbers04FormattingParsing (wypisywanie kwot),
 *             t16_streams/Streams15BigDecimalMoney (sumowanie kwot w strumieniach).
 * </pre>
 */
public class Numbers01BigDecimal {

    /** VAT_RATE = stawka VAT 23%. Stałe BigDecimal tworzymy RAZ, poza pętlami i lambdami. */
    private static final BigDecimal VAT_RATE = new BigDecimal("0.23");
    /** GROSS_FACTOR = mnożnik brutto (1 + 0.23 = 1.23). */
    private static final BigDecimal GROSS_FACTOR = BigDecimal.ONE.add(VAT_RATE);

    public static void main(String[] args) {
        title("Numbers01 — BigDecimal: dokładne liczby dziesiętne");

        whyNotDouble();             // why not double = dlaczego nie double
        creatingBigDecimal();       // creating = tworzenie
        arithmetic();               // arithmetic = arytmetyka (działania)
        division();                 // division = dzielenie
        roundingModes();            // rounding modes = tryby zaokrąglania
        scaleAndPrecision();        // scale and precision = skala i precyzja
        equalsVsCompareTo();        // equals vs compareTo = equals kontra compareTo
        stripAndPlainString();      // strip and plain string = usuwanie zer i zwykły napis
        usefulMethods();            // useful methods = przydatne metody
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. DLACZEGO NIE double?
    // =================================================================================================

    /**
     * 1. double przechowuje liczbę w systemie dwójkowym. Liczby takie jak 0.1, 0.2 czy 1.10 mają w nim
     * nieskończone rozwinięcie, więc są zapisane z malutkim błędem. Przy wypisywaniu Java zwykle go ukrywa,
     * ale po dodawaniu i odejmowaniu błędy się kumulują i wychodzą na wierzch.
     */
    static void whyNotDouble() {
        section("1. Dlaczego nie double? Błędy zaokrągleń w systemie dwójkowym");

        double sum = 0.1 + 0.2;
        show("0.1 + 0.2 (double)", sum);
        show("0.1 + 0.2 == 0.3 ?", sum == 0.3);
        show("1.10 - 1.00 (double)", 1.10 - 1.00);
        // WYNIK: 0.1 + 0.2 (double) → 0.30000000000000004
        // WYNIK: 0.1 + 0.2 == 0.3 ? → false
        // WYNIK: 1.10 - 1.00 (double) → 0.10000000000000009

        double tenTimes = 0;
        for (int i = 0; i < 10; i++) {
            tenTimes += 0.1;                                          // 10 razy po 10 groszy
        }
        show("10 × 0.1 (double)", tenTimes);
        // WYNIK: 10 × 0.1 (double) → 0.9999999999999999    ← klient zapłacił 1 zł, a w kasie 0.99999...

        // To samo z BigDecimal — tworzonym z NAPISU:
        BigDecimal bdSum = new BigDecimal("0.1").add(new BigDecimal("0.2"));                // add = dodaj
        BigDecimal bdDiff = new BigDecimal("1.10").subtract(new BigDecimal("1.00"));       // subtract = odejmij
        show("0.1 + 0.2 (BigDecimal)", bdSum);
        show("1.10 - 1.00 (BigDecimal)", bdDiff);
        // WYNIK: 0.1 + 0.2 (BigDecimal) → 0.3
        // WYNIK: 1.10 - 1.00 (BigDecimal) → 0.10    ← nawet skala (2 miejsca) została zachowana

        // DOBRA PRAKTYKA: pieniądze, stawki procentowe, kursy walut → BigDecimal (albo grosze w long,
        //   t15_numbers/Numbers05IntegerTricks). double → pomiary i obliczenia naukowe, gdzie mały błąd nie szkodzi.
    }

    // =================================================================================================
    // 2. TWORZENIE: new BigDecimal("0.1") vs new BigDecimal(0.1) vs valueOf(0.1)
    // =================================================================================================

    /**
     * 2. Sposób tworzenia ma OGROMNE znaczenie:
     * <ul>
     *   <li>{@code new BigDecimal("0.1")} — z napisu: dokładnie to, co napisałeś. Najlepszy wybór.</li>
     *   <li>{@code new BigDecimal(0.1)} — z double: kopiuje błąd double co do cyfry. Prawie zawsze błąd!</li>
     *   <li>{@code BigDecimal.valueOf(0.1)} — z double przez {@code Double.toString}: daje "0.1".
     *       Bezpieczne dla literałów, ale nie naprawi błędu, który już powstał w obliczeniach na double.</li>
     *   <li>{@code BigDecimal.valueOf(long)} i {@code BigDecimal.valueOf(long, skala)} — z liczb całkowitych.</li>
     * </ul>
     */
    static void creatingBigDecimal() {
        section("2. Tworzenie: z napisu, z double i przez valueOf");

        show("new BigDecimal(\"0.1\")", new BigDecimal("0.1"));
        show("new BigDecimal(0.1)", new BigDecimal(0.1));
        show("BigDecimal.valueOf(0.1)", BigDecimal.valueOf(0.1));    // valueOf = wartość z
        // WYNIK: new BigDecimal("0.1") → 0.1
        // WYNIK: new BigDecimal(0.1) → 0.1000000000000000055511151231257827021181583404541015625
        // WYNIK: BigDecimal.valueOf(0.1) → 0.1
        // Konstruktor z double pokazuje PRAWDZIWĄ wartość zapisaną w double — to nie jest 0.1!

        show("BigDecimal.valueOf(5)", BigDecimal.valueOf(5));
        show("BigDecimal.valueOf(1999, 2)", BigDecimal.valueOf(1999, 2));   // 1999 × 10^(−2)
        // WYNIK: BigDecimal.valueOf(5) → 5
        // WYNIK: BigDecimal.valueOf(1999, 2) → 19.99    ← wygodne, gdy masz kwotę w groszach

        // PUŁAPKA: valueOf nie naprawi błędu, który już powstał w double.
        show("BigDecimal.valueOf(0.1 + 0.2)", BigDecimal.valueOf(0.1 + 0.2));
        // WYNIK: BigDecimal.valueOf(0.1 + 0.2) → 0.30000000000000004

        // PUŁAPKA: napis musi mieć KROPKĘ, nie przecinek (polski zapis wczytasz w Numbers04FormattingParsing).
        expectThrows("new BigDecimal(\"12,50\")", () -> new BigDecimal("12,50"));
        // WYNIK: ✔ new BigDecimal("12,50") → rzucono NumberFormatException: Character , is neither a decimal digit number, decimal point, nor "e" notation exponential mark.

        // DOBRA PRAKTYKA: new BigDecimal("...") dla stałych w kodzie i danych z plików; valueOf(long) dla
        //   liczb całkowitych; new BigDecimal(double) — nigdy (chyba że chcesz zobaczyć błąd double).
    }

    // =================================================================================================
    // 3. DZIAŁANIA I NIEZMIENNOŚĆ
    // =================================================================================================

    /**
     * 3. add, subtract, multiply zwracają NOWY obiekt. BigDecimal — tak jak String — jest niezmienny.
     * Wywołanie {@code price.add(x)} bez przypisania wyniku nic nie zmienia — to jeden z najczęstszych błędów.
     */
    static void arithmetic() {
        section("3. Działania: add, subtract, multiply — i niezmienność");

        BigDecimal a = new BigDecimal("19.99");
        BigDecimal b = new BigDecimal("3");
        show("19.99 + 3", a.add(b));
        show("19.99 - 3", a.subtract(b));
        show("19.99 × 3", a.multiply(b));                             // multiply = pomnóż
        show("1.5 × 1.25", new BigDecimal("1.5").multiply(new BigDecimal("1.25")));
        show("1.10 + 2.5", new BigDecimal("1.10").add(new BigDecimal("2.5")));
        // WYNIK: 19.99 + 3 → 22.99
        // WYNIK: 19.99 - 3 → 16.99
        // WYNIK: 19.99 × 3 → 59.97
        // WYNIK: 1.5 × 1.25 → 1.875    ← skala 1 + 2 = 3
        // WYNIK: 1.10 + 2.5 → 3.60    ← skala max(2, 1) = 2

        // PUŁAPKA: wynik działania trzeba PRZYPISAĆ.
        BigDecimal price = new BigDecimal("19.99");
        price.add(new BigDecimal("5.00"));                            // wynik wyrzucony do kosza!
        show("price po price.add(5.00) bez przypisania", price);
        // WYNIK: price po price.add(5.00) bez przypisania → 19.99
        price = price.add(new BigDecimal("5.00"));                    // tak jest dobrze
        show("price po price = price.add(5.00)", price);
        // WYNIK: price po price = price.add(5.00) → 24.99

        // Przykład z danych: wartość pozycji zamówienia = cena × ilość (ilość to int → valueOf).
        Product headphones = SampleData.productBySku("ELE-003");      // productBySku = produkt po SKU
        BigDecimal lineTotal = headphones.price().multiply(BigDecimal.valueOf(2));
        show(headphones.name() + " × 2", lineTotal);
        // WYNIK: Słuchawki BT × 2 → 699.80

        // Łańcuch działań czyta się od lewej: (19.99 + 5.01) × 2
        show("(19.99 + 5.01) × 2", a.add(new BigDecimal("5.01")).multiply(BigDecimal.valueOf(2)));
        // WYNIK: (19.99 + 5.01) × 2 → 50.00
    }

    // =================================================================================================
    // 4. DZIELENIE: ArithmeticException, skala + RoundingMode, MathContext
    // =================================================================================================

    /**
     * 4. Dzielenie jest wyjątkowe: 1/3 ma nieskończone rozwinięcie, a BigDecimal nie zaokrągla „sam z siebie”.
     * {@code divide(b)} bez informacji o zaokrągleniu rzuca ArithmeticException, gdy wynik jest nieskończony.
     * <ul>
     *   <li>{@code divide(b, skala, RoundingMode)} — tyle miejsc po przecinku; do pieniędzy,</li>
     *   <li>{@code divide(b, MathContext)} — tyle cyfr znaczących łącznie (precyzja); do obliczeń naukowych.</li>
     * </ul>
     */
    static void division() {
        section("4. Dzielenie: wyjątek, skala + RoundingMode, MathContext");

        BigDecimal one = BigDecimal.ONE;                              // ONE = stała 1
        BigDecimal three = new BigDecimal("3");

        show("10 / 4", BigDecimal.TEN.divide(new BigDecimal("4")));  // TEN = stała 10; wynik skończony → OK
        // WYNIK: 10 / 4 → 2.5

        expectThrows("1 / 3 bez zaokrąglenia", () -> one.divide(three));
        // WYNIK: ✔ 1 / 3 bez zaokrąglenia → rzucono ArithmeticException: Non-terminating decimal expansion; no exact representable decimal result.
        // Tłumaczenie: „nieskończone rozwinięcie dziesiętne; brak dokładnego wyniku”.
        // PUŁAPKA: 10 / 4 działało, więc test „przeszedł”. Błąd wyjdzie dopiero dla danych typu 1 / 3 — na produkcji.

        show("1 / 3, skala 2, HALF_UP", one.divide(three, 2, RoundingMode.HALF_UP));
        show("2 / 3, skala 4, HALF_UP", new BigDecimal("2").divide(three, 4, RoundingMode.HALF_UP));
        // WYNIK: 1 / 3, skala 2, HALF_UP → 0.33
        // WYNIK: 2 / 3, skala 4, HALF_UP → 0.6667

        show("1 / 3, MathContext.DECIMAL64", one.divide(three, MathContext.DECIMAL64));
        show("100 / 3, new MathContext(5)", new BigDecimal("100").divide(three, new MathContext(5)));
        // WYNIK: 1 / 3, MathContext.DECIMAL64 → 0.3333333333333333    ← 16 cyfr znaczących
        // WYNIK: 100 / 3, new MathContext(5) → 33.333    ← 5 cyfr ŁĄCZNIE, a nie 5 po przecinku

        // PUŁAPKA: divide(b, RoundingMode) BEZ skali bierze skalę dzielnej (tej liczby przed kropką).
        show("1 / 3 z samym HALF_UP", one.divide(three, RoundingMode.HALF_UP));
        show("1.00 / 3 z samym HALF_UP", new BigDecimal("1.00").divide(three, RoundingMode.HALF_UP));
        // WYNIK: 1 / 3 z samym HALF_UP → 0    ← skala 0 → wynik zaokrąglony do całości!
        // WYNIK: 1.00 / 3 z samym HALF_UP → 0.33

        expectThrows("1 / 0", () -> one.divide(BigDecimal.ZERO));
        // WYNIK: ✔ 1 / 0 → rzucono ArithmeticException: Division by zero

        // DOBRA PRAKTYKA: przy dzieleniu ZAWSZE podawaj skalę i RoundingMode (albo MathContext).
    }

    // =================================================================================================
    // 5. setScale I TABELA RoundingMode
    // =================================================================================================

    /**
     * 5. {@code setScale(n, tryb)} zmienia liczbę miejsc po przecinku. Dopisanie zer jest zawsze bezpieczne;
     * utrata cyfr wymaga trybu zaokrąglania (RoundingMode), inaczej leci ArithmeticException.
     * <p>
     * Najważniejsze tryby: HALF_UP (szkolny: połówka w górę), HALF_EVEN („bankierski”: połówka do parzystej —
     * przy wielu zaokrągleniach błędy się znoszą), UP/DOWN (od zera / do zera), CEILING/FLOOR (w górę / w dół
     * na osi liczbowej — różnią się od UP/DOWN dla liczb ujemnych).
     */
    static void roundingModes() {
        section("5. setScale i tryby zaokrąglania (RoundingMode)");

        show("2.5 → setScale(2)", new BigDecimal("2.5").setScale(2));
        // WYNIK: 2.5 → setScale(2) → 2.50    ← dopisanie zera: nic nie ginie, tryb niepotrzebny
        expectThrows("2.456 → setScale(2) bez trybu", () -> new BigDecimal("2.456").setScale(2));
        // WYNIK: ✔ 2.456 → setScale(2) bez trybu → rzucono ArithmeticException: Rounding necessary
        show("2.456 → setScale(2, HALF_UP)", new BigDecimal("2.456").setScale(2, RoundingMode.HALF_UP));
        // WYNIK: 2.456 → setScale(2, HALF_UP) → 2.46

        // Tabela: trzy liczby zaokrąglamy do całości (skala 0), a 2.45 do jednego miejsca (skala 1).
        String[] samples = {"2.5", "3.5", "-2.5"};
        RoundingMode[] modes = {RoundingMode.HALF_UP, RoundingMode.HALF_EVEN, RoundingMode.HALF_DOWN,
                RoundingMode.UP, RoundingMode.DOWN, RoundingMode.CEILING, RoundingMode.FLOOR};
        System.out.println(String.format(Locale.ROOT, "   %-10s %5s %5s %5s %8s", "tryb", "2.5", "3.5", "-2.5", "2.45→0.1"));
        for (RoundingMode mode : modes) {
            StringBuilder row = new StringBuilder(String.format(Locale.ROOT, "   %-10s", mode));
            for (String s : samples) {
                row.append(String.format(Locale.ROOT, " %5s", new BigDecimal(s).setScale(0, mode)));
            }
            row.append(String.format(Locale.ROOT, " %8s", new BigDecimal("2.45").setScale(1, mode)));
            System.out.println(row);
        }
        // WYNIK: tryb         2.5   3.5  -2.5 2.45→0.1
        // WYNIK: HALF_UP        3     4    -3      2.5
        // WYNIK: HALF_EVEN      2     4    -2      2.4
        // WYNIK: HALF_DOWN      2     3    -2      2.4
        // WYNIK: UP             3     4    -3      2.5
        // WYNIK: DOWN           2     3    -2      2.4
        // WYNIK: CEILING        3     4    -2      2.5
        // WYNIK: FLOOR          2     3    -3      2.4
        // Zauważ: HALF_EVEN daje 2 dla 2.5, ale 4 dla 3.5 — zawsze w stronę cyfry PARZYSTEJ.
        // CEILING dla -2.5 daje -2 (w górę osi), a UP daje -3 (dalej od zera).

        // PUŁAPKA: zaokrąglanie liczby utworzonej z double. 1.005 w double to naprawdę 1.00499999...
        show("new BigDecimal(1.005) → 2 miejsca HALF_UP", new BigDecimal(1.005).setScale(2, RoundingMode.HALF_UP));
        show("new BigDecimal(\"1.005\") → 2 miejsca HALF_UP", new BigDecimal("1.005").setScale(2, RoundingMode.HALF_UP));
        // WYNIK: new BigDecimal(1.005) → 2 miejsca HALF_UP → 1.00    ← grosz zgubiony przez double
        // WYNIK: new BigDecimal("1.005") → 2 miejsca HALF_UP → 1.01

        // DOBRA PRAKTYKA: faktury i paragony w Polsce — zwykle HALF_UP (tak liczy większość ludzi i urzędów).
        //   Duże sumy wielu zaokrągleń (banki, statystyka) — HALF_EVEN. Wybierz JEDEN tryb i trzymaj się go.
    }

    // =================================================================================================
    // 6. SKALA KONTRA PRECYZJA
    // =================================================================================================

    /**
     * 6. Skala (scale) = liczba cyfr PO przecinku. Precyzja (precision) = liczba WSZYSTKICH cyfr znaczących.
     * unscaledValue = cyfry bez przecinka. Skala może być ujemna: 1E+3 to 1 × 10^3 (skala −3).
     */
    static void scaleAndPrecision() {
        section("6. Skala (scale) kontra precyzja (precision)");

        for (String s : new String[]{"123.4500", "0.00123", "42", "1E+3"}) {
            BigDecimal x = new BigDecimal(s);
            System.out.println(String.format(Locale.ROOT, "   %-9s scale = %2d, precision = %d, unscaledValue = %s",
                    s, x.scale(), x.precision(), x.unscaledValue()));
        }
        // WYNIK: 123.4500  scale =  4, precision = 7, unscaledValue = 1234500
        // WYNIK: 0.00123   scale =  5, precision = 3, unscaledValue = 123
        // WYNIK: 42        scale =  0, precision = 2, unscaledValue = 42
        // WYNIK: 1E+3      scale = -3, precision = 1, unscaledValue = 1
        // 0.00123: zera na początku NIE są cyframi znaczącymi, więc precyzja to 3 (cyfry 1, 2, 3).

        // Skala to część WARTOŚCI obiektu: 123.45 i 123.4500 to „ta sama liczba”, ale różne obiekty BigDecimal.
        // Dlatego skalę kwot ustawiaj świadomie (np. zawsze 2 dla złotówek) — patrz Numbers02MoneyValueObject.
        note("Skala: divide, setScale, pieniądze. Precyzja: MathContext, obliczenia naukowe.");
        // WYNIK: ℹ Skala: divide, setScale, pieniądze. Precyzja: MathContext, obliczenia naukowe.
    }

    // =================================================================================================
    // 7. equals KONTRA compareTo — 2.0 vs 2.00
    // =================================================================================================

    /**
     * 7. {@code equals} w BigDecimal porównuje wartość ORAZ skalę: 2.0 i 2.00 NIE są równe.
     * {@code compareTo} porównuje tylko wartość: zwraca 0, gdy liczby są równe.
     * <p>
     * Konsekwencja dla kolekcji: HashSet i HashMap używają equals/hashCode, a TreeSet i TreeMap — compareTo.
     * Ta sama para liczb daje więc różne wyniki w różnych zbiorach (t12_collections/Collections04Sets).
     */
    static void equalsVsCompareTo() {
        section("7. equals kontra compareTo — 2.0 i 2.00");

        BigDecimal twoA = new BigDecimal("2.0");
        BigDecimal twoB = new BigDecimal("2.00");
        show("2.0 equals 2.00", twoA.equals(twoB));
        show("2.0 compareTo 2.00", twoA.compareTo(twoB));
        // WYNIK: 2.0 equals 2.00 → false    ← różna skala (1 i 2)
        // WYNIK: 2.0 compareTo 2.00 → 0    ← ta sama wartość

        Set<BigDecimal> hashSet = new HashSet<>(List.of(twoA, twoB));
        Set<BigDecimal> treeSet = new TreeSet<>(List.of(twoA, twoB));
        show("HashSet — liczba elementów", hashSet.size());
        show("TreeSet — liczba elementów", treeSet.size());
        show("hashSet.contains(2)", hashSet.contains(new BigDecimal("2")));
        // WYNIK: HashSet — liczba elementów → 2    ← equals: dwie różne liczby
        // WYNIK: TreeSet — liczba elementów → 1    ← compareTo: jedna liczba
        // WYNIK: hashSet.contains(2) → false    ← „2” ma skalę 0, więc nie pasuje ani do 2.0, ani do 2.00

        // Jak porównywać? compareTo zwraca liczbę ujemną / 0 / dodatnią:
        BigDecimal price = new BigDecimal("129.00");
        BigDecimal limit = new BigDecimal("100");
        show("129.00 > 100 ?", price.compareTo(limit) > 0);
        show("129.00 == 129 ?", price.compareTo(new BigDecimal("129")) == 0);
        // WYNIK: 129.00 > 100 ? → true
        // WYNIK: 129.00 == 129 ? → true

        // PUŁAPKA: if (price.equals(new BigDecimal("129"))) — false, choć cena „jest” 129 zł.
        // DOBRA PRAKTYKA: wartości porównuj przez compareTo:  a.compareTo(b) == 0,  a.compareTo(b) > 0.
        //   Jako klucze w HashMap/HashSet trzymaj liczby o JEDNAKOWEJ skali (albo użyj TreeMap/TreeSet).
        //   Check.equal w ćwiczeniach porównuje BigDecimal wartością (i ostrzega o różnej skali).
    }

    // =================================================================================================
    // 8. stripTrailingZeros I toPlainString — PUŁAPKA 1E+1
    // =================================================================================================

    /**
     * 8. {@code stripTrailingZeros()} usuwa zera na końcu (zmniejsza skalę). Dla liczb całkowitych skala staje się
     * UJEMNA i toString przechodzi na notację naukową: 10.00 → 1E+1. Do wyświetlania użyj {@code toPlainString()}.
     */
    static void stripAndPlainString() {
        section("8. stripTrailingZeros i toPlainString — pułapka 1E+1");

        BigDecimal ten = new BigDecimal("10.00");
        show("10.00 → stripTrailingZeros()", ten.stripTrailingZeros());
        show("   → toPlainString()", ten.stripTrailingZeros().toPlainString());
        show("2.50 → stripTrailingZeros()", new BigDecimal("2.50").stripTrailingZeros());
        // WYNIK: 10.00 → stripTrailingZeros() → 1E+1    ← „1 × 10^1” — klient nie zrozumie takiej ceny!
        // WYNIK: → toPlainString() → 10
        // WYNIK: 2.50 → stripTrailingZeros() → 2.5

        BigDecimal tiny = new BigDecimal("0.0000001");
        show("0.0000001 → toString()", tiny);
        show("0.0000001 → toPlainString()", tiny.toPlainString());
        // WYNIK: 0.0000001 → toString() → 1E-7    ← toString sam wybiera notację E dla bardzo małych liczb
        // WYNIK: 0.0000001 → toPlainString() → 0.0000001

        // Zastosowanie: porównanie przez equals „bez względu na skalę” (np. klucze w HashMap):
        show("2.0 i 2.00 po stripTrailingZeros — equals",
                new BigDecimal("2.0").stripTrailingZeros().equals(new BigDecimal("2.00").stripTrailingZeros()));
        // WYNIK: 2.0 i 2.00 po stripTrailingZeros — equals → true

        // DOBRA PRAKTYKA: kwoty do wyświetlenia: setScale(2, RoundingMode.HALF_UP) albo toPlainString(),
        //   a najlepiej formatowanie z Numbers04FormattingParsing. Nigdy sam stripTrailingZeros() + toString().
    }

    // =================================================================================================
    // 9. PRZYDATNE METODY I STAŁE; SUMOWANIE W PĘTLI
    // =================================================================================================

    /**
     * 9. Znak i wartość bezwzględna, większa/mniejsza z dwóch, stałe ZERO/ONE/TEN.
     * Sumowanie listy kwot: zaczynamy od {@code BigDecimal.ZERO} i w pętli PRZYPISUJEMY {@code total = total.add(...)}.
     */
    static void usefulMethods() {
        section("9. Przydatne metody, stałe i sumowanie w pętli");

        BigDecimal balance = new BigDecimal("-12.50");                // balance = saldo
        show("signum(-12.50)", balance.signum());                     // -1 / 0 / 1
        show("negate(-12.50)", balance.negate());
        show("abs(-12.50)", balance.abs());
        show("max(-12.50, ZERO)", balance.max(BigDecimal.ZERO));
        show("min(-12.50, ZERO)", balance.min(BigDecimal.ZERO));
        // WYNIK: signum(-12.50) → -1
        // WYNIK: negate(-12.50) → 12.50
        // WYNIK: abs(-12.50) → 12.50
        // WYNIK: max(-12.50, ZERO) → 0    ← np. „do zapłaty nie może być mniej niż 0”
        // WYNIK: min(-12.50, ZERO) → -12.50
        show("ZERO / ONE / TEN", BigDecimal.ZERO + " / " + BigDecimal.ONE + " / " + BigDecimal.TEN);
        // WYNIK: ZERO / ONE / TEN → 0 / 1 / 10
        // signum() == 0 to wygodny test „czy zero?” — działa niezależnie od skali (0.00 też daje 0).

        BigDecimal total = BigDecimal.ZERO;
        for (Product p : SampleData.products()) {
            total = total.add(p.price());                             // przypisanie!
        }
        show("suma cen 14 produktów", total);
        // WYNIK: suma cen 14 produktów → 13106.36

        // Stałe (VAT_RATE, GROSS_FACTOR) są na górze klasy — tworzone raz, a nie w każdym obrocie pętli.
        for (Product p : SampleData.products().subList(4, 6)) {
            BigDecimal gross = p.price().multiply(GROSS_FACTOR).setScale(2, RoundingMode.HALF_UP);
            show(p.name() + " netto → brutto", p.price() + " → " + gross);
        }
        // WYNIK: Kawa ziarnista 1kg netto → brutto → 64.99 → 79.94
        // WYNIK: Czekolada gorzka netto → brutto → 7.49 → 9.21

        // Zapowiedź t16: ta sama suma strumieniem (reduce = zredukuj do jednej wartości).
        BigDecimal streamTotal = SampleData.products().stream()
                .map(Product::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        show("suma strumieniem", streamTotal);
        // WYNIK: suma strumieniem → 13106.36
        // PUŁAPKA: new BigDecimal("1.23") wewnątrz lambdy tworzy nowy obiekt dla KAŻDEGO elementu — trzymaj stałą
        //   poza lambdą (t16_streams/Streams15BigDecimalMoney).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   TWORZENIE:    new BigDecimal("19.99")  ✔   BigDecimal.valueOf(long) / valueOf(1999, 2) → 19.99  ✔
     *                 new BigDecimal(0.1)  ✘ (błąd double)   BigDecimal.valueOf(0.1) → "0.1" (tylko z literałów)
     *   DZIAŁANIA:    a.add(b), a.subtract(b), a.multiply(b) — ZAWSZE przypisz wynik (niezmienność)
     *   DZIELENIE:    a.divide(b, 2, RoundingMode.HALF_UP)  albo  a.divide(b, MathContext.DECIMAL64)
     *                 samo a.divide(b) → ArithmeticException dla 1/3;  a.divide(b, tryb) → skala dzielnej!
     *   ZAOKRĄGLANIE: setScale(2, RoundingMode.HALF_UP); HALF_UP — szkolne; HALF_EVEN — bankierskie
     *   SKALA:        scale = cyfry po przecinku; precision = wszystkie cyfry znaczące
     *   PORÓWNANIE:   a.compareTo(b) == 0 / > 0 / < 0;  equals uwzględnia skalę (2.0 ≠ 2.00)
     *                 HashSet → equals (2 elementy), TreeSet → compareTo (1 element)
     *   WYŚWIETLANIE: toPlainString() zamiast toString() po stripTrailingZeros() (1E+1!)
     *   INNE:         signum, negate, abs, max, min; stałe ZERO, ONE, TEN; suma: total = total.add(x)
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego 0.1 + 0.2 w double nie daje dokładnie 0.3, a w BigDecimal daje?
     *   2. Co wypisze:  System.out.println(new BigDecimal("1.50").add(new BigDecimal("2.5")));  ?
     *   3. ZNAJDŹ BŁĄD:
     *          BigDecimal total = BigDecimal.ZERO;
     *          for (Product p : products) { total.add(p.price()); }
     *          return total;
     *   4. ZNAJDŹ BŁĄD:  BigDecimal perPerson = bill.divide(BigDecimal.valueOf(people));
     *   5. Co wypisze:  System.out.println(new BigDecimal("2.5").setScale(0, RoundingMode.HALF_EVEN)
     *                          + " " + new BigDecimal("-2.5").setScale(0, RoundingMode.CEILING));  ?
     *   6. Co wypisze:  System.out.println(new BigDecimal("5.0").equals(new BigDecimal("5.00"))
     *                          + " " + new BigDecimal("5.0").compareTo(new BigDecimal("5.00")));  ?
     *   7. Czym różni się skala od precyzji? Podaj obie dla 0.0450.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Uruchamia ćwiczenia: najpierw TWOJE rozwiązania (✘ dopóki nie uzupełnisz), potem wzorcowe (✔). */
    static void exercises() {
        List<Product> products = SampleData.products();
        List<BigDecimal> amounts = List.of(new BigDecimal("2.0"), new BigDecimal("2.00"), new BigDecimal("3"),
                new BigDecimal("3.000"), new BigDecimal("2.5"));

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: suma cen produktów", new BigDecimal("13106.36"), () -> exercise1(products));
        Check.equalExact("ćw. 2a: brutto z 64.99", new BigDecimal("79.94"), () -> exercise2(new BigDecimal("64.99")));
        Check.equalExact("ćw. 2b: brutto z 100", new BigDecimal("123.00"), () -> exercise2(new BigDecimal("100")));
        Check.equalExact("ćw. 3a: średnia cena", new BigDecimal("936.17"), () -> exercise3(products));
        Check.equal("ćw. 3b: średnia z pustej listy", BigDecimal.ZERO, () -> exercise3(List.of()));
        Check.equalExact("ćw. 4a: 129.00 minus 15%", new BigDecimal("109.65"),
                () -> exercise4(new BigDecimal("129.00"), 15));
        Check.equalExact("ćw. 4b: 49.99 minus 10%", new BigDecimal("44.99"), () -> exercise4(new BigDecimal("49.99"), 10));
        Check.equal("ćw. 5: różne wartości", 3, () -> exercise5(amounts));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", new BigDecimal("13106.36"), () -> solution1(products));
        Check.equalExact("ćw. 2a (wzorzec)", new BigDecimal("79.94"), () -> solution2(new BigDecimal("64.99")));
        Check.equalExact("ćw. 2b (wzorzec)", new BigDecimal("123.00"), () -> solution2(new BigDecimal("100")));
        Check.equalExact("ćw. 3a (wzorzec)", new BigDecimal("936.17"), () -> solution3(products));
        Check.equal("ćw. 3b (wzorzec)", BigDecimal.ZERO, () -> solution3(List.of()));
        Check.equalExact("ćw. 4a (wzorzec)", new BigDecimal("109.65"), () -> solution4(new BigDecimal("129.00"), 15));
        Check.equalExact("ćw. 4b (wzorzec)", new BigDecimal("44.99"), () -> solution4(new BigDecimal("49.99"), 10));
        Check.equal("ćw. 5 (wzorzec)", 3, () -> solution5(amounts));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 8 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć sumę cen wszystkich produktów (każdy produkt liczony raz, bez stanu magazynu).
     * Podpowiedź: zacznij od BigDecimal.ZERO; w pętli {@code total = total.add(p.price())} — pamiętaj o przypisaniu.
     */
    static BigDecimal exercise1(List<Product> products) {
        // TODO: twoje rozwiązanie
        return BigDecimal.ZERO;
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć cenę brutto (netto × 1.23) zaokrągloną do 2 miejsc trybem HALF_UP.
     * Podpowiedź: multiply przez stałą, potem setScale(2, RoundingMode.HALF_UP). Sprawdzamy przez
     * Check.equalExact, więc liczy się też SKALA: 123 to nie to samo co 123.00.
     */
    static BigDecimal exercise2(BigDecimal net) {
        // TODO: twoje rozwiązanie
        return BigDecimal.ZERO;
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć średnią cenę produktów z dokładnością do 2 miejsc (HALF_UP).
     * Dla pustej listy zwróć BigDecimal.ZERO (dzielenie przez zero rzuciłoby wyjątek).
     * Podpowiedź: suma jak w ćw. 1, potem {@code sum.divide(BigDecimal.valueOf(products.size()), 2, RoundingMode.HALF_UP)}.
     */
    static BigDecimal exercise3(List<Product> products) {
        // TODO: twoje rozwiązanie
        return BigDecimal.ONE;
    }

    /**
     * ĆWICZENIE 4 (średnie): PRZEPISZ metodę z double na BigDecimal. Wynik: skala 2, HALF_UP.
     * <pre>{@code
     * static double discountedPrice(double price, int percent) {
     *     return price - price * percent / 100;
     * }
     * }</pre>
     * Podpowiedź: rabat = price × percent / 100 (BigDecimal.valueOf(percent), divide ze skalą albo multiply
     * przez 0.01); wynik = price − rabat, na końcu setScale(2, RoundingMode.HALF_UP). Check.equalExact → skala 2!
     */
    static BigDecimal exercise4(BigDecimal price, int percent) {
        // TODO: twoje rozwiązanie
        return price;
    }

    /**
     * ĆWICZENIE 5 (trudniejsze, łączy BigDecimal ze zbiorami): policz, ile RÓŻNYCH wartości jest na liście,
     * ignorując skalę: [2.0, 2.00, 3, 3.000, 2.5] → 3 (bo 2, 3 i 2.5).
     * Podpowiedź: który zbiór używa compareTo zamiast equals? Albo: stripTrailingZeros() przed dodaniem do HashSet.
     */
    static int exercise5(List<BigDecimal> amounts) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static BigDecimal solution1(List<Product> products) {
        BigDecimal total = BigDecimal.ZERO;
        for (Product p : products) {
            total = total.add(p.price());
        }
        return total;
    }

    static BigDecimal solution2(BigDecimal net) {
        return net.multiply(GROSS_FACTOR).setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal solution3(List<Product> products) {
        if (products.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return solution1(products).divide(BigDecimal.valueOf(products.size()), 2, RoundingMode.HALF_UP);
    }

    static BigDecimal solution4(BigDecimal price, int percent) {
        BigDecimal discount = price.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100));  // /100 zawsze skończone
        return price.subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }

    static int solution5(List<BigDecimal> amounts) {
        return new TreeSet<>(amounts).size();                         // TreeSet porównuje przez compareTo
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. double zapisuje liczby dwójkowo, a 0.1 i 0.2 mają w tym systemie nieskończone rozwinięcie — są
     *      przybliżone, a błędy się sumują. BigDecimal przechowuje cyfry dziesiętne dokładnie tak, jak je podano.
     *   2. „4.00” — przy dodawaniu skala wyniku to większa ze skal (2), więc 1.50 + 2.5 = 4.00.
     *   3. Wynik total.add(...) nie jest przypisany — BigDecimal jest niezmienny, więc total zostaje 0.
     *      Poprawnie: total = total.add(p.price());
     *   4. divide bez skali i RoundingMode rzuci ArithmeticException, gdy wynik ma nieskończone rozwinięcie
     *      (np. 100 / 3). Poprawnie: bill.divide(BigDecimal.valueOf(people), 2, RoundingMode.HALF_UP)
     *      — a przy dzieleniu rachunku uważaj na zgubione grosze (Numbers02MoneyValueObject).
     *   5. „2 -2” — HALF_EVEN zaokrągla połówkę do cyfry parzystej (2), CEILING zawsze w górę osi (−2 leży wyżej niż −3).
     *   6. „false 0” — equals uwzględnia skalę (1 i 2), compareTo tylko wartość.
     *   7. Skala = liczba cyfr po przecinku, precyzja = liczba cyfr znaczących. 0.0450: skala 4, precyzja 3
     *      (cyfry 4, 5, 0 — zera na początku się nie liczą, zero na końcu tak).
     */
    // </editor-fold>
}
