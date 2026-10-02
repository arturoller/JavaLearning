package t15_numbers;

import helpers.Check;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Ściągawka klasy Math — najczęściej używane funkcje w jednym miejscu
 *        (Math = matematyka; cheatsheet = ściągawka)
 *
 * W SKRÓCIE:
 *   Klasa java.lang.Math zawiera gotowe funkcje: wartość bezwzględna, potęgi, pierwiastki, logarytmy, trygonometrię,
 *   zaokrąglanie, dzielenie z poprawnym znakiem reszty i arytmetykę z wykrywaniem przepełnienia. Ta lekcja to
 *   ściągawka: tabele z wynikami, dzięki którym od razu widać różnice (np. round kontra rint kontra floor).
 *   Uczy też pułapek: Math.abs(Integer.MIN_VALUE) jest ujemne, round(-2.5) daje -2, a NaN nie jest równe samemu sobie.
 *
 * ANALOGIA: Math to szuflada z kalkulatorem naukowym. Każdy przycisk robi jedną rzecz i nie ma pamięci (metody
 *   statyczne, bez stanu). floor i ceil to podłoga i sufit budynku: -2,5 leży między piętrami -3 i -2.
 *   Licznik kilometrów w aucie po 999999 wraca do 000000 — tak działa przepełnienie int; metody
 *   addExact/multiplyExact to licznik z alarmem, który krzyczy, zamiast po cichu zaczynać od zera.
 *   ulp to najmniejszy krok na linijce: im dalej od zera, tym linijka ma rzadsze kreski.
 *
 * JAK TO DZIAŁA:
 *   1. Metody Math są statyczne: Math.abs(-3). Nie tworzysz obiektu Math.
 *   2. Większość ma przeciążenia (overloads) dla int, long, float i double. Wynik ma typ argumentów:
 *      Math.abs(int) zwraca int, a Math.round(double) zwraca long (nie int!).
 *   3. Funkcje zmiennoprzecinkowe nie rzucają wyjątków: błąd daje NaN (nie liczba) albo Infinity (nieskończoność).
 *   4. Funkcje całkowite z końcówką Exact rzucają ArithmeticException zamiast po cichu przepełnić wynik.
 *   5. Wyniki funkcji przestępnych (sin, cos, exp, log, pow) mogą się różnić o 1 ulp między platformami. Dlatego
 *      w tej lekcji wypisujemy je z kilkoma miejscami po przecinku, a nie z pełną dokładnością.
 *
 *   Zaokrąglanie (wartość x → wynik):   -2,7    -2,5    -0,5    0,5    2,5    2,7
 *     (int) x  (ucina część ułamkową)    -2      -2       0      0      2      2
 *     Math.floor (w dół)                 -3      -3      -1      0      2      2
 *     Math.ceil (w górę)                 -2      -2      -0      1      3      3
 *     Math.round (połówka w stronę +∞)   -3      -2       0      1      3      3
 *     Math.rint (połówka do parzystej)   -3      -2      -0      0      2      3
 *
 * SŁÓWKA:
 *   abs = wartość bezwzględna; min/max = najmniejsza/największa; pow = potęga; sqrt = pierwiastek kwadratowy;
 *   cbrt = pierwiastek sześcienny; hypot = przeciwprostokątna; exp = e do potęgi; log = logarytm;
 *   floor = podłoga; ceil = sufit; round = zaokrąglij; rint = zaokrąglij do najbliższej parzystej;
 *   signum = znak liczby; ulp = jednostka na ostatnim miejscu (unit in the last place); fma = pomnóż i dodaj
 *   (fused multiply-add); strict = ścisły; overflow = przepełnienie; radians = radiany;
 *   NaN = nie-liczba (not a number); Infinity = nieskończoność; seed = ziarno.
 *
 * ZOBACZ TEŻ: t01_basics/Basics07MathRandom (pierwsze kroki z Math i losowaniem),
 *   t01_basics/Basics08FloatingPoint (dlaczego double się myli), t15_numbers/Numbers01BigDecimal (dokładne zaokrąglanie),
 *   t15_numbers/Numbers05IntegerTricks (przepełnienie i bity), t15_numbers/Numbers04FormattingParsing (formatowanie liczb),
 *   t04_strings/Strings04Formatting (flagi String.format), t24_algorithms/Math04NumberSystems (systemy liczbowe)
 * </pre>
 */
public class Numbers06MathCheatsheet {

    public static void main(String[] args) {
        title("Numbers06 — ściągawka klasy Math");

        absMinMax();        // absMinMax = wartość bezwzględna, min, max
        powersRoots();      // powersRoots = potęgi i pierwiastki
        expLog();           // expLog = exp i logarytmy
        trigonometry();     // trigonometry = trygonometria
        rounding();         // rounding = zaokrąglanie
        divisionSigns();    // divisionSigns = dzielenie i znak reszty
        exactArithmetic();  // exactArithmetic = arytmetyka dokładna (z wykrywaniem przepełnienia)
        precisionUlp();     // precisionUlp = precyzja double, ulp, fma
        randomAndStrict();  // randomAndStrict = losowanie oraz Math kontra StrictMath
        nanAndInfinity();   // nanAndInfinity = NaN, nieskończoność i formatowanie
        exercises();        // exercises = ćwiczenia
    }

    /** Wypisuje wiersz tabeli z jawnym Locale.ROOT (żeby wynik był taki sam na każdym komputerze). */
    private static void table(String format, Object... args) {
        System.out.println(String.format(Locale.ROOT, format, args));
    }

    /** Formatuje liczbę z 6 miejscami po przecinku (format = sformatuj; ROOT = bez regionu, kropka dziesiętna). */
    private static String f6(double x) {
        return String.format(Locale.ROOT, "%.6f", x);
    }

    // =================================================================================================
    // 1. ABS, MIN, MAX
    // =================================================================================================

    /**
     * 1. abs, min, max — proste, ale z dwiema pułapkami: najmniejsza liczba int nie ma „dodatniego brata”,
     * a dla zer ze znakiem i NaN min/max zachowują się inaczej, niż podpowiada intuicja.
     */
    static void absMinMax() {
        section("1. abs, min, max");

        int minInt = Integer.MIN_VALUE;
        show("Math.abs(-7)", Math.abs(-7));
        // WYNIK: Math.abs(-7) → 7
        show("Math.abs(-7.5)", Math.abs(-7.5));
        // WYNIK: Math.abs(-7.5) → 7.5
        show("Integer.MIN_VALUE", minInt);
        // WYNIK: Integer.MIN_VALUE → -2147483648

        // PUŁAPKA: int mieści liczby od -2147483648 do 2147483647. Dodatnie 2147483648 się nie mieści,
        // więc abs(MIN_VALUE) „przekręca licznik” i zwraca tę samą liczbę ujemną. Dlaczego to groźne:
        // kod „wartość bezwzględna jest zawsze nieujemna” przestaje być prawdą (to samo dla Long.MIN_VALUE).
        show("Math.abs(Integer.MIN_VALUE)", Math.abs(minInt));
        // WYNIK: Math.abs(Integer.MIN_VALUE) → -2147483648
        show("Math.abs(Long.MIN_VALUE)", Math.abs(Long.MIN_VALUE));
        // WYNIK: Math.abs(Long.MIN_VALUE) → -9223372036854775808

        // DOBRA PRAKTYKA: absExact (Java 15+; exact = dokładny) rzuca wyjątek zamiast po cichu zwrócić liczbę ujemną.
        // Albo rozszerz do long przed wzięciem abs: Math.abs((long) x) mieści każde abs z int.
        expectThrows("Math.absExact(Integer.MIN_VALUE)", () -> Math.absExact(minInt));
        // WYNIK: ✔ Math.absExact(Integer.MIN_VALUE) → rzucono ArithmeticException: Overflow to represent absolute value of Integer.MIN_VALUE
        show("Math.abs((long) minInt)", Math.abs((long) minInt));
        // WYNIK: Math.abs((long) minInt) → 2147483648

        // PUŁAPKA: Math.abs(hashCode) % n do wyboru „kubełka” bywa ujemne. Napis "polygenelubricants" ma hashCode
        // równy dokładnie Integer.MIN_VALUE — abs go nie naprawia. Użyj Math.floorMod (sekcja 6): zawsze 0..n-1.
        int hash = "polygenelubricants".hashCode();
        show("hashCode napisu", hash);
        // WYNIK: hashCode napisu → -2147483648
        show("Math.abs(hash) % 10", Math.abs(hash) % 10);
        // WYNIK: Math.abs(hash) % 10 → -8
        show("Math.floorMod(hash, 10)", Math.floorMod(hash, 10));
        // WYNIK: Math.floorMod(hash, 10) → 2

        show("Math.max(3, 9)", Math.max(3, 9));
        // WYNIK: Math.max(3, 9) → 9
        show("Math.min(-1.5, 2.5)", Math.min(-1.5, 2.5));
        // WYNIK: Math.min(-1.5, 2.5) → -1.5
        // Min/max dla double: -0.0 jest „mniejsze” od 0.0, a każde porównanie z NaN daje NaN.
        show("Math.min(0.0, -0.0)", Math.min(0.0, -0.0));
        // WYNIK: Math.min(0.0, -0.0) → -0.0
        show("Math.max(Double.NaN, 1.0)", Math.max(Double.NaN, 1.0));
        // WYNIK: Math.max(Double.NaN, 1.0) → NaN

        // Ograniczanie wartości do przedziału (clamp = przytnij): od Java 21+ jest Math.clamp(wartość, min, max).
        // W Javie 17 składamy je z min i max: max(dolna, min(górna, wartość)).
        show("clamp(15, 0, 10)", Math.max(0, Math.min(10, 15)));
        // WYNIK: clamp(15, 0, 10) → 10
        show("clamp(-3, 0, 10)", Math.max(0, Math.min(10, -3)));
        // WYNIK: clamp(-3, 0, 10) → 0
        show("clamp(5, 0, 10)", Math.max(0, Math.min(10, 5)));
        // WYNIK: clamp(5, 0, 10) → 5
    }

    // =================================================================================================
    // 2. POTĘGI I PIERWIASTKI
    // =================================================================================================

    /**
     * 2. pow, sqrt, cbrt, hypot. Wszystkie zwracają double. sqrt jest „dokładnie zaokrąglony”, więc daje ten sam
     * wynik na każdym komputerze; pozostałe wypisujemy z 6 miejscami.
     */
    static void powersRoots() {
        section("2. pow, sqrt, cbrt, hypot");

        show("Math.pow(2, 10)", Math.pow(2, 10));
        // WYNIK: Math.pow(2, 10) → 1024.0
        show("Math.pow(2, -1)", Math.pow(2, -1));
        // WYNIK: Math.pow(2, -1) → 0.5
        show("Math.pow(0, 0)", Math.pow(0, 0));
        // WYNIK: Math.pow(0, 0) → 1.0
        show("Math.pow(-8, 1.0 / 3)", Math.pow(-8, 1.0 / 3));
        // WYNIK: Math.pow(-8, 1.0 / 3) → NaN    ← potęga ułamkowa liczby ujemnej nie istnieje; użyj cbrt
        show("Math.sqrt(16)", Math.sqrt(16));
        // WYNIK: Math.sqrt(16) → 4.0
        show("Math.sqrt(-1)", Math.sqrt(-1));
        // WYNIK: Math.sqrt(-1) → NaN
        show("Math.cbrt(-8)", f6(Math.cbrt(-8)));
        // WYNIK: Math.cbrt(-8) → -2.000000
        show("Math.hypot(3, 4)", f6(Math.hypot(3, 4)));
        // WYNIK: Math.hypot(3, 4) → 5.000000    ← sqrt(3*3 + 4*4) bez przepełnienia pośrednich wyników

        // PUŁAPKA: pierwiastek podniesiony do kwadratu zwykle nie wraca do wyjściowej liczby (błąd zaokrąglenia).
        // Dlatego nie sprawdzaj „czy x jest kwadratem” przez sqrt(x) * sqrt(x) == x.
        double root = Math.sqrt(2);
        show("sqrt(2) * sqrt(2)", root * root);
        // WYNIK: sqrt(2) * sqrt(2) → 2.0000000000000004
        show("sqrt(2) * sqrt(2) == 2", root * root == 2);
        // WYNIK: sqrt(2) * sqrt(2) == 2 → false

        // PUŁAPKA: pow zwraca double, a double ma tylko 53 bity na cyfry. Duże całkowite potęgi (powyżej 2^53)
        // tracą dokładność. Liczba 2^53 + 1 po drodze przez double zamienia się w 2^53:
        long big = 9_007_199_254_740_993L; // 2^53 + 1
        show("(long) (double) (2^53 + 1)", (long) (double) big);
        // WYNIK: (long) (double) (2^53 + 1) → 9007199254740992

        // DOBRA PRAKTYKA: potęgę całkowitą licz pętlą z multiplyExact (sekcja 7) albo BigInteger.pow
        // (t15_numbers/Numbers03BigInteger). Math.pow zostaw na wyniki ułamkowe.
        long exact = 1;
        for (int i = 0; i < 18; i++) {
            exact = Math.multiplyExact(exact, 10L);
        }
        show("10^18 pętlą", exact);
        // WYNIK: 10^18 pętlą → 1000000000000000000
    }

    // =================================================================================================
    // 3. EXP I LOGARYTMY
    // =================================================================================================

    /**
     * 3. exp, log, log10, log1p, expm1. Logarytm naturalny log ma podstawę e, a nie 10 (w odróżnieniu od
     * kalkulatorów, na których „log” to często log10). Wyniki wypisujemy z 6 miejscami po przecinku.
     */
    static void expLog() {
        section("3. exp, log, log10, log1p, expm1");

        show("Math.exp(1)", f6(Math.exp(1)));
        // WYNIK: Math.exp(1) → 2.718282    ← liczba e podniesiona do potęgi 1
        show("Math.log(Math.E)", f6(Math.log(Math.E)));
        // WYNIK: Math.log(Math.E) → 1.000000    ← log = logarytm naturalny (podstawa e)
        show("Math.log10(1000)", f6(Math.log10(1000)));
        // WYNIK: Math.log10(1000) → 3.000000    ← log10 = logarytm dziesiętny
        show("Math.log(0)", Math.log(0));
        // WYNIK: Math.log(0) → -Infinity
        show("Math.log(-1)", Math.log(-1));
        // WYNIK: Math.log(-1) → NaN

        // Zmiana podstawy: log_b(x) = log(x) / log(b). Dla potęg dwójki zamiast tego użyj operacji na bitach
        // (Integer.numberOfLeadingZeros, t15_numbers/Numbers05IntegerTricks) — bez błędów zaokrągleń.
        show("log2(1024) przez log/log", f6(Math.log(1024) / Math.log(2)));
        // WYNIK: log2(1024) przez log/log → 10.000000

        // log1p(x) = log(1 + x) oraz expm1(x) = exp(x) - 1 (log1p = logarytm z 1+x; expm1 = exp minus 1).
        // PUŁAPKA: dla bardzo małego x suma 1 + x traci cyfry (1 + 1e-15 zaokrągla się do najbliższej liczby
        // double, a to nie jest dokładnie 1,000000000000001). log1p liczy to bez takiej sumy.
        double tiny = 1e-15;
        table("log(1 + x)  dla x = 1e-15 → %.2e    (zły: błąd rzędu 10%%)", Math.log(1 + tiny));
        // WYNIK: log(1 + x)  dla x = 1e-15 → 1.11e-15    (zły: błąd rzędu 10%)
        table("Math.log1p(x) dla x = 1e-15 → %.2e    (dobry)", Math.log1p(tiny));
        // WYNIK: Math.log1p(x) dla x = 1e-15 → 1.00e-15    (dobry)
        table("Math.exp(x) - 1 dla x = 1e-15 → %.2e    (zły)", Math.exp(tiny) - 1);
        // WYNIK: Math.exp(x) - 1 dla x = 1e-15 → 1.11e-15    (zły)
        table("Math.expm1(x)   dla x = 1e-15 → %.2e    (dobry)", Math.expm1(tiny));
        // WYNIK: Math.expm1(x)   dla x = 1e-15 → 1.00e-15    (dobry)

        // Zastosowanie: kapitalizacja. Ciągła: K * e^(r*t); roczna: K * (1 + r)^t. Dla 1000 zł, 5%, 10 lat:
        double capital = 1000;
        double rate = 0.05;
        table("kapitalizacja roczna  : %.2f", capital * Math.pow(1 + rate, 10));
        // WYNIK: kapitalizacja roczna  : 1628.89
        table("kapitalizacja ciągła  : %.2f", capital * Math.exp(rate * 10));
        // WYNIK: kapitalizacja ciągła  : 1648.72

        // DOBRA PRAKTYKA: do prawdziwych pieniędzy używaj BigDecimal (t15_numbers/Numbers01BigDecimal);
        // Math z double nadaje się do statystyki, symulacji i grafiki, gdzie 1e-15 nie robi różnicy.
    }

    // =================================================================================================
    // 4. TRYGONOMETRIA
    // =================================================================================================

    /**
     * 4. Trygonometria. Math.sin/cos/tan przyjmują RADIANY, nie stopnie. Do zamiany służą toRadians
     * (na radiany) i toDegrees (na stopnie). Kąt wektora liczymy przez atan2, nie przez atan(y / x).
     */
    static void trigonometry() {
        section("4. Trygonometria (radiany!)");

        show("Math.PI", Math.PI);
        // WYNIK: Math.PI → 3.141592653589793
        show("Math.E", Math.E);
        // WYNIK: Math.E → 2.718281828459045
        show("Math.toRadians(180)", Math.toRadians(180));
        // WYNIK: Math.toRadians(180) → 3.141592653589793
        show("Math.toDegrees(Math.PI / 2)", f6(Math.toDegrees(Math.PI / 2)));
        // WYNIK: Math.toDegrees(Math.PI / 2) → 90.000000

        // PUŁAPKA: sin(30) to sinus 30 RADIANÓW (około 4,77 obrotu), nie 30 stopni.
        show("Math.sin(30)  (radiany!)", f6(Math.sin(30)));
        // WYNIK: Math.sin(30)  (radiany!) → -0.988032
        show("Math.sin(toRadians(30))", f6(Math.sin(Math.toRadians(30))));
        // WYNIK: Math.sin(toRadians(30)) → 0.500000

        table("%7s | %9s %9s", "stopnie", "sin", "cos");
        // WYNIK: stopnie |       sin       cos
        for (int deg : new int[] {0, 30, 45, 60, 90, 180}) {
            double rad = Math.toRadians(deg);
            table("%7d | %9s %9s", deg, f6(Math.sin(rad)), f6(Math.cos(rad)));
        }
        // WYNIK:       0 |  0.000000  1.000000
        // WYNIK:      30 |  0.500000  0.866025
        // WYNIK:      45 |  0.707107  0.707107
        // WYNIK:      60 |  0.866025  0.500000
        // WYNIK:      90 |  1.000000  0.000000
        // WYNIK:     180 |  0.000000 -1.000000

        show("Math.tan(toRadians(45))", f6(Math.tan(Math.toRadians(45))));
        // WYNIK: Math.tan(toRadians(45)) → 1.000000
        show("Math.asin(2)", Math.asin(2));
        // WYNIK: Math.asin(2) → NaN    ← sinus nigdy nie jest większy niż 1

        // PUŁAPKA: π w double jest tylko przybliżeniem, więc sin(PI) nie jest dokładnie 0 (jest około 1,2e-16).
        // Nie porównuj wyników trygonometrii przez ==; użyj tolerancji (różnica mniejsza niż np. 1e-9).
        show("sin(PI) == 0", Math.sin(Math.PI) == 0);
        // WYNIK: sin(PI) == 0 → false
        show("|sin(PI) - 0| < 1e-9", Math.abs(Math.sin(Math.PI)) < 1e-9);
        // WYNIK: |sin(PI) - 0| < 1e-9 → true

        // atan2(y, x) = kąt wektora (x, y) w radianach z przedziału od -PI do PI; zna ćwiartkę układu.
        // atan(y / x) widzi tylko iloraz, więc myli ćwiartki (np. (-1, 1) i (1, -1) dają ten sam iloraz -1).
        table("%8s | %9s | %12s", "(x, y)", "atan2", "atan(y / x)");
        // WYNIK: (x, y) |     atan2 |  atan(y / x)
        int[][] vectors = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};
        for (int[] v : vectors) {
            double x = v[0];
            double y = v[1];
            String viaAtan = x == 0 ? "dzielenie" : String.format(Locale.ROOT, "%.1f", Math.toDegrees(Math.atan(y / x)));
            table("%8s | %9.1f | %12s", "(" + v[0] + "," + v[1] + ")", Math.toDegrees(Math.atan2(y, x)), viaAtan);
        }
        // WYNIK:    (1,0) |       0.0 |          0.0
        // WYNIK:    (1,1) |      45.0 |         45.0
        // WYNIK:    (0,1) |      90.0 |    dzielenie
        // WYNIK:   (-1,1) |     135.0 |        -45.0
        // WYNIK:   (-1,0) |     180.0 |         -0.0
        // WYNIK:  (-1,-1) |    -135.0 |         45.0
        // WYNIK:   (0,-1) |     -90.0 |    dzielenie
        // WYNIK:   (1,-1) |     -45.0 |        -45.0
        // Ostatni wiersz tabeli pokazuje: dla (-1, -1) atan zwraca 45 zamiast -135. atan2 radzi sobie nawet z x = 0.

        // DOBRA PRAKTYKA: kąt „0..360” z atan2: floorMod(round(stopnie), 360) (sekcja 6) — patrz ćwiczenie 4.
    }

    // =================================================================================================
    // 5. ZAOKRĄGLANIE
    // =================================================================================================

    /**
     * 5. Rodzina zaokrągleń: rzutowanie (int), floor, ceil, round, rint. Pięć sposobów, pięć różnych wyników
     * dla liczb ujemnych i połówek (x,5). Tabela pokazuje wszystko naraz.
     */
    static void rounding() {
        section("5. Zaokrąglanie: (int), floor, ceil, round, rint");

        double[] xs = {-2.7, -2.5, -0.5, 0.5, 2.5, 2.7};
        table("%6s | %6s %6s %6s %6s %6s", "x", "(int)", "floor", "ceil", "round", "rint");
        // WYNIK:      x |  (int)  floor   ceil  round   rint
        for (double x : xs) {
            table("%6s | %6s %6s %6s %6s %6s", x, (int) x, Math.floor(x), Math.ceil(x), Math.round(x), Math.rint(x));
        }
        // WYNIK:   -2.7 |     -2   -3.0   -2.0     -3   -3.0
        // WYNIK:   -2.5 |     -2   -3.0   -2.0     -2   -2.0
        // WYNIK:   -0.5 |      0   -1.0   -0.0      0   -0.0
        // WYNIK:    0.5 |      0    0.0    1.0      1    0.0
        // WYNIK:    2.5 |      2    2.0    3.0      3    2.0
        // WYNIK:    2.7 |      2    2.0    3.0      3    3.0

        // Jak czytać tabelę:
        //  - (int) x ucina część ułamkową (w stronę zera): -2,7 → -2.
        //  - floor zwraca największą liczbę nie większą niż x (w dół, w stronę -∞); ceil najmniejszą nie mniejszą (w górę).
        //    Oba zwracają double, a nie int! ceil(-0.5) to "-0.0" (ujemne zero) — wygląda dziwnie, ale == 0.0.
        //  - round = floor(x + 0,5): połówka idzie w stronę +∞. Dlatego round(2.5) = 3, ale round(-2.5) = -2 (nie -3!).
        //    To NIE jest „zaokrąglanie od zera” (HALF_UP z BigDecimal działa inaczej dla ujemnych: -2,5 → -3).
        //  - rint (round to integer) zaokrągla połówkę do najbliższej liczby PARZYSTEJ: 0,5 → 0; 2,5 → 2; 3,5 → 4.
        //    To „zaokrąglanie bankowe” (HALF_EVEN w BigDecimal) — wyrównuje błędy przy sumowaniu wielu liczb.
        show("Math.round(-2.5)", Math.round(-2.5));
        // WYNIK: Math.round(-2.5) → -2
        show("Math.rint(3.5)", Math.rint(3.5));
        // WYNIK: Math.rint(3.5) → 4.0
        show("Math.rint(2.5)", Math.rint(2.5));
        // WYNIK: Math.rint(2.5) → 2.0

        // PUŁAPKA: Math.round(double) zwraca long, więc „int n = Math.round(2.6);” to błąd kompilacji
        // (możliwa utrata precyzji). Wersja dla float, Math.round(float), zwraca int:
        int fromFloat = Math.round(2.6f); // 2.6f = literał typu float (liczba zmiennoprzecinkowa pojedynczej precyzji)
        long fromDouble = Math.round(2.6);
        int narrowed = (int) Math.round(2.6); // jawne zawężenie — rób to świadomie, nie dla dużych liczb
        show("round(2.6f) → int", fromFloat);
        // WYNIK: round(2.6f) → int → 3
        show("round(2.6) → long", fromDouble);
        // WYNIK: round(2.6) → long → 3
        show("(int) Math.round(2.6)", narrowed);
        // WYNIK: (int) Math.round(2.6) → 3

        // Przypadki brzegowe round: NaN daje 0, a za duża liczba zostaje obcięta do granicy long (nasycenie).
        show("Math.round(NaN)", Math.round(Double.NaN));
        // WYNIK: Math.round(NaN) → 0
        show("Math.round(1e30)", Math.round(1e30));
        // WYNIK: Math.round(1e30) → 9223372036854775807
        show("Math.round(Infinity)", Math.round(Double.POSITIVE_INFINITY));
        // WYNIK: Math.round(Infinity) → 9223372036854775807

        // PUŁAPKA: znany sposób „zaokrąglij do 2 miejsc”: Math.round(x * 100) / 100.0. Zawodzi, bo 1.005 w double
        // to w rzeczywistości 1.00499999999999989341858963598497211933135986328125 — a więc poniżej połówki.
        // Wynik mnożenia przez 100 też bywa tuż pod połówką (tu 100.49999999999999) i round idzie w dół.
        double price = 1.005;
        show("1.005 * 100", price * 100);
        // WYNIK: 1.005 * 100 → 100.49999999999999
        show("Math.round(1.005 * 100) / 100.0", Math.round(price * 100) / 100.0);
        // WYNIK: Math.round(1.005 * 100) / 100.0 → 1.0
        // Uwaga: dla innych liczb to samo mnożenie bywa „szczęśliwe” — np. 2.675 * 100 daje dokładnie 267.5
        // i wynik wychodzi 2.68. Metoda jest więc zawodna w sposób nieprzewidywalny.

        // DOBRA PRAKTYKA: dla kwot i „ładnych” dziesiętnych zaokrągleń użyj BigDecimal z napisu i RoundingMode
        // (t15_numbers/Numbers01BigDecimal). HALF_UP zaokrągla połówkę „od zera”, HALF_EVEN do parzystej.
        BigDecimal exactPrice = new BigDecimal("1.005");
        show("BigDecimal HALF_UP", exactPrice.setScale(2, RoundingMode.HALF_UP));
        // WYNIK: BigDecimal HALF_UP → 1.01
        show("BigDecimal HALF_EVEN", exactPrice.setScale(2, RoundingMode.HALF_EVEN));
        // WYNIK: BigDecimal HALF_EVEN → 1.00    ← połówka do parzystej (0 jest parzyste)
        show("BigDecimal HALF_UP dla -2.5", new BigDecimal("-2.5").setScale(0, RoundingMode.HALF_UP));
        // WYNIK: BigDecimal HALF_UP dla -2.5 → -3    ← inaczej niż Math.round(-2.5) = -2
    }

    // =================================================================================================
    // 6. DZIELENIE I ZNAK RESZTY
    // =================================================================================================

    /**
     * 6. Operatory / i % ucinają wynik w stronę zera, więc reszta ma znak dzielnej (może być ujemna!).
     * floorDiv i floorMod zaokrąglają w dół, więc reszta ma znak dzielnika — to wersja „matematyczna”.
     */
    static void divisionSigns() {
        section("6. / i % kontra floorDiv i floorMod");

        table("%3s %3s | %4s %4s | %8s %8s", "a", "b", "a/b", "a%b", "floorDiv", "floorMod");
        // WYNIK:   a   b |  a/b  a%b | floorDiv floorMod
        int[][] pairs = {{7, 2}, {-7, 2}, {7, -2}, {-7, -2}};
        for (int[] p : pairs) {
            int a = p[0];
            int b = p[1];
            table("%3d %3d | %4d %4d | %8d %8d", a, b, a / b, a % b, Math.floorDiv(a, b), Math.floorMod(a, b));
        }
        // WYNIK:   7   2 |    3    1 |        3        1
        // WYNIK:  -7   2 |   -3   -1 |       -4        1
        // WYNIK:   7  -2 |   -3    1 |       -4       -1
        // WYNIK:  -7  -2 |    3   -1 |        3       -1
        // Tożsamość dla obu par: a == (a / b) * b + (a % b)  oraz  a == floorDiv(a, b) * b + floorMod(a, b).
        // Znak: a % b ma znak a (dzielnej); floorMod(a, b) ma znak b (dzielnika). Dla b > 0 wynik floorMod to 0..b-1.

        // PUŁAPKA: warunek „liczba jest nieparzysta”: x % 2 == 1 jest FAŁSZEM dla -3, bo -3 % 2 == -1.
        int odd = -3;
        show("-3 % 2 == 1", odd % 2 == 1);
        // WYNIK: -3 % 2 == 1 → false
        show("-3 % 2 != 0", odd % 2 != 0);
        // WYNIK: -3 % 2 != 0 → true    ← poprawny test nieparzystości

        // Zastosowanie 1: cykliczny indeks (zegar, tablica kołowa). Godzina 3 minus 5 godzin = 22 (poprzedni dzień).
        show("(3 - 5) % 24", (3 - 5) % 24);
        // WYNIK: (3 - 5) % 24 → -2
        show("floorMod(3 - 5, 24)", Math.floorMod(3 - 5, 24));
        // WYNIK: floorMod(3 - 5, 24) → 22

        // PRZEPISZ (stary sposób → nowy): ((i % n) + n) % n   →   Math.floorMod(i, n)
        int i = -1;
        int n = 5;
        show("((i % n) + n) % n", ((i % n) + n) % n);
        // WYNIK: ((i % n) + n) % n → 4
        show("Math.floorMod(i, n)", Math.floorMod(i, n));
        // WYNIK: Math.floorMod(i, n) → 4

        // Zastosowanie 2: dzielenie „w górę” dla liczb dodatnich — ile stron po 10 pozycji na 23 pozycje?
        // Od Java 18+ jest Math.ceilDiv(a, b); w Javie 17 użyj (a + b - 1) / b (tylko dla a >= 0 i b > 0).
        int items = 23;
        int perPage = 10;
        show("stron (a + b - 1) / b", (items + perPage - 1) / perPage);
        // WYNIK: stron (a + b - 1) / b → 3

        // Dzielenie całkowite przez zero rzuca wyjątek, a double daje Infinity/NaN (sekcja 10).
        int zero = 0;
        expectThrows("5 / zero (int)", () -> System.out.println(5 / zero));
        // WYNIK: ✔ 5 / zero (int) → rzucono ArithmeticException: / by zero
        expectThrows("5 % zero (int)", () -> System.out.println(5 % zero));
        // WYNIK: ✔ 5 % zero (int) → rzucono ArithmeticException: / by zero
        show("floorMod(long, int) zwraca int (Java 9+)", Math.floorMod(-7L, 3));
        // WYNIK: floorMod(long, int) zwraca int (Java 9+) → 2
    }

    // =================================================================================================
    // 7. ARYTMETYKA DOKŁADNA
    // =================================================================================================

    /**
     * 7. Zwykłe + - * na int i long przepełniają się po cichu (licznik wraca do początku). Metody z końcówką
     * Exact (dokładny) sprawdzają wynik i rzucają ArithmeticException. Dla double takich metod nie ma — double
     * przepełnia się do Infinity (sekcja 10).
     */
    static void exactArithmetic() {
        section("7. addExact, multiplyExact, toIntExact...");

        int max = Integer.MAX_VALUE;
        show("MAX_VALUE + 1 (po cichu)", max + 1);
        // WYNIK: MAX_VALUE + 1 (po cichu) → -2147483648
        expectThrows("Math.addExact(MAX_VALUE, 1)", () -> Math.addExact(max, 1));
        // WYNIK: ✔ Math.addExact(MAX_VALUE, 1) → rzucono ArithmeticException: integer overflow
        expectThrows("Math.subtractExact(MIN_VALUE, 1)", () -> Math.subtractExact(Integer.MIN_VALUE, 1));
        // WYNIK: ✔ Math.subtractExact(MIN_VALUE, 1) → rzucono ArithmeticException: integer overflow
        expectThrows("Math.multiplyExact(100_000, 100_000)", () -> Math.multiplyExact(100_000, 100_000));
        // WYNIK: ✔ Math.multiplyExact(100_000, 100_000) → rzucono ArithmeticException: integer overflow
        expectThrows("Math.multiplyExact(long)", () -> Math.multiplyExact(Long.MAX_VALUE, 2L));
        // WYNIK: ✔ Math.multiplyExact(long) → rzucono ArithmeticException: long overflow
        expectThrows("Math.negateExact(MIN_VALUE)", () -> Math.negateExact(Integer.MIN_VALUE));
        // WYNIK: ✔ Math.negateExact(MIN_VALUE) → rzucono ArithmeticException: integer overflow
        expectThrows("Math.incrementExact(MAX_VALUE)", () -> Math.incrementExact(max));
        // WYNIK: ✔ Math.incrementExact(MAX_VALUE) → rzucono ArithmeticException: integer overflow
        // Są też decrementExact (zmniejsz o 1) i negateExact (zmień znak).

        // toIntExact: bezpieczne zawężenie long → int (zwykłe (int) po cichu obcina bity).
        long tooBig = 3_000_000_000L;
        show("(int) 3_000_000_000L", (int) tooBig);
        // WYNIK: (int) 3_000_000_000L → -1294967296
        expectThrows("Math.toIntExact(3_000_000_000L)", () -> Math.toIntExact(tooBig));
        // WYNIK: ✔ Math.toIntExact(3_000_000_000L) → rzucono ArithmeticException: integer overflow
        show("Math.toIntExact(2_000_000_000L)", Math.toIntExact(2_000_000_000L));
        // WYNIK: Math.toIntExact(2_000_000_000L) → 2000000000

        // PUŁAPKA: int * int liczy się jako int, DOPIERO POTEM wynik jest rozszerzany do long — za późno.
        int a = 100_000;
        int b = 100_000;
        long wrong = a * b;
        long right = (long) a * b;
        show("long wrong = a * b", wrong);
        // WYNIK: long wrong = a * b → 1410065408
        show("long right = (long) a * b", right);
        // WYNIK: long right = (long) a * b → 10000000000
        // DOBRA PRAKTYKA: Math.multiplyFull(a, b) (Java 9+; full = pełny) mnoży dwa int do long bez przepełnienia.
        show("Math.multiplyFull(a, b)", Math.multiplyFull(a, b));
        // WYNIK: Math.multiplyFull(a, b) → 10000000000

        // Zastosowanie: silnia w long. Zamiast po cichu zwrócić nonsens, pętla zatrzymuje się przy przepełnieniu.
        long factorial = 1;
        int k = 1;
        try {
            while (true) {
                factorial = Math.multiplyExact(factorial, k);
                k++;
            }
        } catch (ArithmeticException e) {
            show("największa silnia w long", (k - 1) + "! = " + factorial);
            // WYNIK: największa silnia w long → 20! = 2432902008176640000
        }
        // Dalej użyj BigInteger (t15_numbers/Numbers03BigInteger).
    }

    // =================================================================================================
    // 8. PRECYZJA DOUBLE
    // =================================================================================================

    /**
     * 8. signum, copySign, ulp, nextUp i fma. ulp (unit in the last place) to odległość od liczby do następnej
     * możliwej liczby double. To wyjaśnia, dlaczego 0.1 + 0.2 != 0.3.
     */
    static void precisionUlp() {
        section("8. signum, copySign, ulp, nextUp, fma");

        show("Math.signum(-3.5)", Math.signum(-3.5));
        // WYNIK: Math.signum(-3.5) → -1.0
        show("Math.signum(0.0)", Math.signum(0.0));
        // WYNIK: Math.signum(0.0) → 0.0
        show("Math.copySign(5.0, -0.0)", Math.copySign(5.0, -0.0));
        // WYNIK: Math.copySign(5.0, -0.0) → -5.0    ← bierze wielkość z pierwszego, znak z drugiego argumentu
        show("Integer.signum(-42)", Integer.signum(-42));
        // WYNIK: Integer.signum(-42) → -1    ← dla int: klasa Integer, a nie Math

        // ulp rośnie wraz z wielkością liczby: double ma stałą liczbę cyfr (53 bity), a nie stałą liczbę miejsc po przecinku.
        show("Math.ulp(1.0)", Math.ulp(1.0));
        // WYNIK: Math.ulp(1.0) → 2.220446049250313E-16
        show("Math.ulp(1000.0)", Math.ulp(1000.0));
        // WYNIK: Math.ulp(1000.0) → 1.1368683772161603E-13
        show("Math.ulp(1e16)", Math.ulp(1e16));
        // WYNIK: Math.ulp(1e16) → 2.0
        show("Math.nextUp(1.0)", Math.nextUp(1.0));
        // WYNIK: Math.nextUp(1.0) → 1.0000000000000002    ← następna liczba po 1.0
        show("Math.nextDown(1.0)", Math.nextDown(1.0));
        // WYNIK: Math.nextDown(1.0) → 0.9999999999999999

        // PUŁAPKA: przy 1e16 kolejne liczby double dzieli już 2.0, więc dodanie 1 „znika”.
        show("1e16 + 1 == 1e16", 1e16 + 1 == 1e16);
        // WYNIK: 1e16 + 1 == 1e16 → true

        // Dlaczego 0.1 + 0.2 != 0.3: ani 0.1, ani 0.2, ani 0.3 nie mają skończonego zapisu dwójkowego. Suma 0.1 + 0.2
        // wypada dokładnie JEDEN ulp powyżej najbliższego double dla 0.3:
        double sum = 0.1 + 0.2;
        show("0.1 + 0.2", sum);
        // WYNIK: 0.1 + 0.2 → 0.30000000000000004
        show("0.1 + 0.2 == 0.3", sum == 0.3);
        // WYNIK: 0.1 + 0.2 == 0.3 → false
        show("Math.ulp(0.3)", Math.ulp(0.3));
        // WYNIK: Math.ulp(0.3) → 5.551115123125783E-17
        show("(0.1 + 0.2) - 0.3", sum - 0.3);
        // WYNIK: (0.1 + 0.2) - 0.3 → 5.551115123125783E-17    ← równo jeden ulp
        show("nextUp(0.3) == 0.1 + 0.2", Math.nextUp(0.3) == sum);
        // WYNIK: nextUp(0.3) == 0.1 + 0.2 → true

        // DOBRA PRAKTYKA: double porównuj z tolerancją. Bezwzględna (różnica < 1e-9) pasuje do liczb rzędu 1;
        // względna (kilka ulp) działa dla każdej wielkości.
        show("|sum - 0.3| < 1e-9", Math.abs(sum - 0.3) < 1e-9);
        // WYNIK: |sum - 0.3| < 1e-9 → true
        show("różnica <= 4 ulp", Math.abs(sum - 0.3) <= 4 * Math.ulp(0.3));
        // WYNIK: różnica <= 4 ulp → true

        // PUŁAPKA: dodawanie 0.1 dziesięć razy nie daje 1.0 — błędy się kumulują.
        double acc = 0;
        for (int i = 0; i < 10; i++) {
            acc += 0.1;
        }
        show("10 x 0.1", acc);
        // WYNIK: 10 x 0.1 → 0.9999999999999999

        // fma(a, b, c) (Java 9+) = a * b + c z JEDNYM zaokrągleniem na końcu. Zwykłe a * b + c zaokrągla dwa razy.
        // 0.1 w double jest odrobinę większe od 0.1, więc 0.1 * 10 - 1 powinno dać maleńką dodatnią resztę:
        show("0.1 * 10 - 1 (dwa zaokrąglenia)", 0.1 * 10 - 1);
        // WYNIK: 0.1 * 10 - 1 (dwa zaokrąglenia) → 0.0
        show("Math.fma(0.1, 10, -1)", Math.fma(0.1, 10, -1));
        // WYNIK: Math.fma(0.1, 10, -1) → 5.551115123125783E-17    ← jedno zaokrąglenie: widać prawdziwy błąd 0.1
    }

    // =================================================================================================
    // 9. LOSOWANIE I STRICTMATH
    // =================================================================================================

    /**
     * 9. Trzy sposoby losowania (Math.random, Random z ziarnem, RandomGenerator) oraz różnica Math kontra StrictMath.
     */
    static void randomAndStrict() {
        section("9. Losowanie oraz Math kontra StrictMath");

        // Math.random() zwraca double z przedziału od 0.0 (włącznie) do 1.0 (wyłącznie) i nie ma ziarna —
        // za każdym razem inna liczba, więc nie wypisujemy jej (wynik zależy od uruchomienia).
        double r = Math.random();
        show("0.0 <= Math.random() < 1.0", r >= 0.0 && r < 1.0);
        // WYNIK: 0.0 <= Math.random() < 1.0 → true
        // Wzór na liczbę całkowitą z przedziału: (int) (Math.random() * 6) + 1 — rzut kostką. Ale lepiej Random.

        // Random z ziarnem (seed = ziarno) daje zawsze ten sam ciąg: algorytm jest opisany w dokumentacji.
        // Używaj go w testach, symulacjach i lekcjach — wynik można powtórzyć.
        Random seeded = new Random(42);
        List<Integer> rolls = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            rolls.add(seeded.nextInt(100)); // nextInt(100) = następna liczba całkowita 0..99
        }
        show("Random(42): 5 liczb 0..99", rolls);
        // WYNIK: Random(42): 5 liczb 0..99 → [30, 63, 48, 84, 70]
        show("Random(42).nextInt(1, 7) (Java 17+)", new Random(42).nextInt(1, 7));
        // WYNIK: Random(42).nextInt(1, 7) (Java 17+) → 3
        // PUŁAPKA: Random bez ziarna ma ziarno z zegara — nie do powtórzenia. Do haseł i tokenów użyj SecureRandom
        // (java.security): Random jest przewidywalny. W wielu wątkach użyj ThreadLocalRandom.current().

        // RandomGenerator (Java 17+, JEP 356) to wspólny interfejs: Random go implementuje. Algorytm wybierasz po nazwie,
        // np. "L64X128MixRandom" (nowoczesny, szybki). Z ziarnem też jest powtarzalny.
        RandomGenerator generator = RandomGeneratorFactory.of("L64X128MixRandom").create(42L);
        List<Integer> viaGenerator = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            viaGenerator.add(generator.nextInt(100));
        }
        show("L64X128MixRandom(42)", viaGenerator);
        // WYNIK: L64X128MixRandom(42) → [98, 1, 80]
        // Więcej o losowaniu: t01_basics/Basics07MathRandom.

        // Math kontra StrictMath. StrictMath (strict = ścisły) zawsze daje wynik algorytmu fdlibm, taki sam
        // na każdej platformie. Math może użyć szybszej instrukcji procesora; specyfikacja dopuszcza różnicę do 1 ulp
        // dla sin, cos, exp, log, pow itd. Dla sqrt oraz + - * / wynik jest zawsze dokładnie zaokrąglony.
        show("sqrt: Math == StrictMath", Math.sqrt(2) == StrictMath.sqrt(2));
        // WYNIK: sqrt: Math == StrictMath → true
        table("Math.sin(1)       = %.10f", Math.sin(1));
        // WYNIK: Math.sin(1)       = 0.8414709848
        table("StrictMath.sin(1) = %.10f", StrictMath.sin(1));
        // WYNIK: StrictMath.sin(1) = 0.8414709848
        // Przy 10 miejscach oba wyglądają tak samo, ale pełne bity mogą się różnić — dlatego wypisujemy z zaokrągleniem.
        // JEP 306 (Java 17): od tej wersji całe działanie na float/double jest „ścisłe” — słowo strictfp jest zbędne,
        // a + - * / dają ten sam wynik wszędzie. Funkcje Math (sin, exp...) nadal mają tolerancję 1 ulp.
        // DOBRA PRAKTYKA: potrzebujesz tych samych bitów na każdym komputerze (np. gra sieciowa, test z oczekiwanym
        // wynikiem)? Użyj StrictMath. W pozostałych przypadkach Math jest szybszy i wystarczający.
    }

    // =================================================================================================
    // 10. NaN, NIESKOŃCZONOŚĆ I FORMATOWANIE
    // =================================================================================================

    /**
     * 10. double nie rzuca wyjątków przy dzieleniu przez zero: daje Infinity albo NaN. NaN to jedyna wartość,
     * która nie jest równa samej sobie. Na końcu: formatowanie double z jawnym Locale.
     */
    static void nanAndInfinity() {
        section("10. NaN, Infinity i formatowanie double");

        show("1 / 0.0", 1 / 0.0);
        // WYNIK: 1 / 0.0 → Infinity
        show("-1 / 0.0", -1 / 0.0);
        // WYNIK: -1 / 0.0 → -Infinity
        double nan = 0.0 / 0.0;
        show("0.0 / 0.0", nan);
        // WYNIK: 0.0 / 0.0 → NaN
        show("Double.MAX_VALUE * 2", Double.MAX_VALUE * 2);
        // WYNIK: Double.MAX_VALUE * 2 → Infinity    ← przepełnienie double
        show("Infinity - Infinity", Double.POSITIVE_INFINITY - Double.POSITIVE_INFINITY);
        // WYNIK: Infinity - Infinity → NaN

        // PUŁAPKA: NaN nie jest równe niczemu, nawet sobie samemu; porównania z NaN są zawsze fałszywe
        // (poza !=). Dlatego if (x == Double.NaN) nigdy się nie wykona — użyj Double.isNaN(x).
        show("nan == nan", nan == nan);
        // WYNIK: nan == nan → false
        show("nan != nan", nan != nan);
        // WYNIK: nan != nan → true
        show("nan == Double.NaN", nan == Double.NaN);
        // WYNIK: nan == Double.NaN → false
        show("Double.isNaN(nan)", Double.isNaN(nan));
        // WYNIK: Double.isNaN(nan) → true
        show("Double.isInfinite(1 / 0.0)", Double.isInfinite(1 / 0.0));
        // WYNIK: Double.isInfinite(1 / 0.0) → true
        show("Double.isFinite(nan)", Double.isFinite(nan));
        // WYNIK: Double.isFinite(nan) → false    ← sprawdza „nie NaN i nie nieskończoność” naraz

        // Double.compare i equals traktują NaN i zera inaczej niż ==: dają PEŁNY porządek (ważne dla sortowania i
        // kluczy w mapach): -0.0 jest mniejsze od 0.0, a NaN jest największe.
        show("0.0 == -0.0", 0.0 == -0.0);
        // WYNIK: 0.0 == -0.0 → true
        show("Double.compare(0.0, -0.0)", Double.compare(0.0, -0.0));
        // WYNIK: Double.compare(0.0, -0.0) → 1
        show("Double.compare(nan, 1.0)", Double.compare(nan, 1.0));
        // WYNIK: Double.compare(nan, 1.0) → 1
        show("Double.compare(nan, nan)", Double.compare(nan, nan));
        // WYNIK: Double.compare(nan, nan) → 0
        show("Double.valueOf(nan).equals(nan)", Double.valueOf(nan).equals(nan));
        // WYNIK: Double.valueOf(nan).equals(nan) → true

        // Formatowanie double: ZAWSZE podawaj Locale. Bez niego użyty zostanie język komputera (u Ciebie przecinek,
        // na serwerze kropka), więc wynik zależałby od maszyny. println i konkatenacja ("" + x) są niezależne od regionu.
        double amount = 1234567.891;
        Locale pl = Locale.forLanguageTag("pl-PL");
        show("ROOT  %,.2f", String.format(Locale.ROOT, "%,.2f", amount));
        // WYNIK: ROOT  %,.2f → 1,234,567.89
        // PUŁAPKA: polski separator tysięcy to twarda spacja U+00A0 (nie zwykła spacja) — porównanie z " " się nie powiedzie.
        // Zamieniamy ją na zwykłą spację tylko do wypisania (U+202F w innych wersjach danych regionalnych też).
        String polish = String.format(pl, "%,.2f", amount).replace((char) 0x00A0, ' ').replace((char) 0x202F, ' ');
        show("pl-PL %,.2f", polish);
        // WYNIK: pl-PL %,.2f → 1 234 567,89
        show("GERMANY %,.2f", String.format(Locale.GERMANY, "%,.2f", amount));
        // WYNIK: GERMANY %,.2f → 1.234.567,89
        show("ROOT  %e", String.format(Locale.ROOT, "%e", amount));
        // WYNIK: ROOT  %e → 1.234568e+06
        show("pl-PL %.1f", String.format(pl, "%.1f", 2.5));
        // WYNIK: pl-PL %.1f → 2,5
        show("println: 2.5", "" + 2.5);
        // WYNIK: println: 2.5 → 2.5

        // Zaokrąglenie w %.2f robi Formatter na dziesiętnym zapisie liczby z Double.toString (HALF_UP): 1.005 → 1.01,
        // choć dokładna wartość double jest odrobinę poniżej połówki. Wynik inny niż Math.round(x * 100) / 100.0 (sekcja 5):
        show("%.2f dla 1.005", String.format(Locale.ROOT, "%.2f", 1.005));
        // WYNIK: %.2f dla 1.005 → 1.01
        // Szczegóły formatowania: t15_numbers/Numbers04FormattingParsing i t04_strings/Strings04Formatting.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • abs(Integer.MIN_VALUE) jest ujemne; użyj absExact albo abs((long) x); floorMod zamiast abs(hash) % n.
     *   • min/max: -0.0 < 0.0, a NaN „zaraża” wynik. Przycięcie do przedziału: max(dół, min(góra, x)); Math.clamp to Java 21+.
     *   • pow zwraca double (potęgi całkowite licz pętlą lub BigInteger); sqrt dokładny; sqrt(-1) i log(-1) dają NaN.
     *   • sin/cos/tan liczą w RADIANACH: toRadians/toDegrees. Kąt wektora: atan2(y, x), nie atan(y / x).
     *   • Zaokrąglanie: (int) ucina; floor w dół; ceil w górę; round połówka do +∞ (round(-2.5) = -2, zwraca long);
     *     rint połówka do parzystej. Dla pieniędzy BigDecimal + RoundingMode.
     *   • / i % ucinają w stronę zera (reszta ma znak dzielnej); floorDiv/floorMod w dół (reszta ma znak dzielnika).
     *   • Exact (addExact, multiplyExact, toIntExact, negateExact) rzucają ArithmeticException; (long) a * b, nie a * b.
     *   • ulp = odstęp między sąsiednimi double; 0.1 + 0.2 to 0.3 plus 1 ulp; double porównuj z tolerancją.
     *   • fma (Java 9+): a * b + c z jednym zaokrągleniem. random() bez ziarna; Random(seed) powtarzalny; SecureRandom do haseł.
     *   • Math może różnić się 1 ulp między platformami, StrictMath nie. Od Java 17 działania + - * / są zawsze ścisłe.
     *   • NaN != NaN (użyj Double.isNaN); 1 / 0.0 = Infinity, ale 1 / 0 (int) rzuca wyjątek. Format: zawsze z Locale.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego Math.abs(Integer.MIN_VALUE) nie jest liczbą dodatnią? Podaj dwa sposoby, by tego uniknąć.
     *   2. Co wypisze:  System.out.println(Math.round(-2.5) + " " + Math.round(2.5) + " " + Math.rint(2.5) + " " + Math.rint(3.5));  ?
     *   3. Co wypisze:  System.out.println((-7 / 2) + " " + (-7 % 2) + " " + Math.floorDiv(-7, 2) + " " + Math.floorMod(-7, 2));  ?
     *   4. ZNAJDŹ BŁĄD:  double x = 2.6;  int n = Math.round(x);
     *   5. ZNAJDŹ BŁĄD:  int a = 50_000, b = 50_000;  long area = a * b;  — oczekiwano 2_500_000_000.
     *   6. Co wypisze:  double d = 0.0 / 0.0;  System.out.println((d == d) + " " + Double.isNaN(d));  ?
     *   7. Czym różni się Math.sin(30) od Math.sin(Math.toRadians(30)) i dlaczego sin(Math.PI) == 0 daje false?
     *   8. Kiedy użyjesz StrictMath zamiast Math, a kiedy BigDecimal zamiast obu?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: abs bez pułapki", 2147483648L, () -> exercise1(Integer.MIN_VALUE));
        Check.equal("ćw. 2: indeks cykliczny", 4, () -> exercise2(-1, 5));
        Check.equal("ćw. 3: bezpieczne mnożenie", "przepełnienie", () -> exercise3(100_000, 100_000));
        Check.equal("ćw. 4: kąt wektora", 315L, () -> exercise4(1, -1));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 2147483648L, () -> solution1(Integer.MIN_VALUE));
        Check.equal("ćw. 1 (wzorzec, zwykła liczba)", 5L, () -> solution1(-5));
        Check.equal("ćw. 2 (wzorzec)", 4, () -> solution2(-1, 5));
        Check.equal("ćw. 2 (wzorzec, za duży)", 2, () -> solution2(7, 5));
        Check.equal("ćw. 3 (wzorzec)", "przepełnienie", () -> solution3(100_000, 100_000));
        Check.equal("ćw. 3 (wzorzec, bez przepełnienia)", "1000000", () -> solution3(1000, 1000));
        Check.equal("ćw. 4 (wzorzec)", 315L, () -> solution4(1, -1));
        Check.equal("ćw. 4 (wzorzec, 135)", 135L, () -> solution4(-1, 1));
        Check.equal("ćw. 4 (wzorzec, 180)", 180L, () -> solution4(-1, 0));
        Check.equal("ćw. 4 (wzorzec, 0)", 0L, () -> solution4(1, 0));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 10 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć wartość bezwzględną liczby int jako long, poprawną także dla Integer.MIN_VALUE.
     * Podpowiedź: rozszerz do long PRZED wzięciem abs.
     */
    static long exercise1(int x) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ na floorMod. Indeks cykliczny w tablicy o rozmiarze size, także dla indeksów
     * ujemnych i większych niż rozmiar (np. -1 w tablicy o 5 elementach to ostatni, czyli 4).
     * <pre>{@code
     * // stary sposób:
     * int wrapped = ((index % size) + size) % size;
     * }</pre>
     * Podpowiedź: Math.floorMod(index, size).
     */
    static int exercise2(int index, int size) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): pomnóż dwa int. Gdy wynik mieści się w int, zwróć go jako tekst (np. "1000000");
     * gdy się nie mieści, zwróć tekst "przepełnienie".
     * Podpowiedź: Math.multiplyExact i try/catch (ArithmeticException).
     */
    static String exercise3(int a, int b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): kąt wektora (x, y) w pełnych stopniach z przedziału 0..359, liczony przeciwnie do ruchu
     * wskazówek zegara od osi X: (1, 0) → 0, (0, 1) → 90, (-1, 0) → 180, (0, -1) → 270, (1, -1) → 315.
     * Podpowiedź: atan2(y, x) → toDegrees → round (zwraca long) → floorMod(…, 360L), bo kąt bywa ujemny
     * (i 359,6 po zaokrągleniu daje 360, które ma wrócić na 0).
     */
    static long exercise4(double x, double y) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static long solution1(int x) {
        return Math.abs((long) x);
    }

    static int solution2(int index, int size) {
        return Math.floorMod(index, size);
    }

    static String solution3(int a, int b) {
        try {
            return String.valueOf(Math.multiplyExact(a, b));
        } catch (ArithmeticException e) {
            return "przepełnienie";
        }
    }

    static long solution4(double x, double y) {
        double degrees = Math.toDegrees(Math.atan2(y, x));
        return Math.floorMod(Math.round(degrees), 360L);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. int ma 2^32 wartości: od -2147483648 do 2147483647, więc +2147483648 się nie mieści i wynik „zawija się”
     *      do tej samej liczby ujemnej. Sposoby: Math.absExact (Java 15+, rzuca wyjątek) albo Math.abs((long) x).
     *   2. "-2 3 2.0 4.0". round: połówka w stronę +∞ (-2,5 → -2; 2,5 → 3); rint: połówka do parzystej (2,5 → 2; 3,5 → 4).
     *   3. "-3 -1 -4 1". Operatory / i % ucinają w stronę zera (-3 i reszta -1), floorDiv/floorMod zaokrąglają w dół
     *      (-4 i reszta 1, bo -4 * 2 + 1 = -7).
     *   4. Math.round(double) zwraca long, a nie int — błąd kompilacji (możliwa utrata precyzji). Popraw na
     *      long n = Math.round(x) albo int n = (int) Math.round(x); wersja Math.round(float) zwraca int.
     *   5. a * b liczy się w int i przepełnia się (daje -1794967296), a dopiero potem jest rozszerzane do long.
     *      Popraw: long area = (long) a * b; albo Math.multiplyFull(a, b).
     *   6. "false true". NaN nie jest równe samemu sobie; do wykrycia NaN służy Double.isNaN.
     *   7. sin(30) liczy sinus 30 RADIANÓW (-0,988…), a sin(toRadians(30)) = 0,5. sin(Math.PI) jest około 1,2e-16, bo PI w double
     *      to tylko przybliżenie liczby π — dlatego porównuj z tolerancją, nie przez ==.
     *   8. StrictMath, gdy potrzebujesz identycznych bitów wyniku na każdej platformie (Math może się różnić o 1 ulp).
     *      BigDecimal, gdy liczysz dokładne wartości dziesiętne (pieniądze) — Math i StrictMath liczą na double, czyli binarnie.
     */
    // </editor-fold>
}
