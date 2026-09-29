package t15_numbers;

import helpers.Check;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Sztuczki i pułapki liczb całkowitych — przepełnienie, dzielenie, systemy liczbowe, bity
 *        (integer = liczba całkowita; trick = sztuczka; overflow = przepełnienie; bit = najmniejsza porcja informacji)
 *
 * W SKRÓCIE:
 *   int i long mają stały rozmiar (32 i 64 bity). Gdy wynik się nie mieści, Java po cichu „zawija” liczbę —
 *   bez wyjątku. Dzielenie całkowite obcina ułamek, a reszta z dzielenia liczb ujemnych bywa ujemna.
 *   Ta lekcja pokazuje, jak te pułapki rozpoznać i obejść (Math.*Exact, floorMod, Integer.compare, grosze w long).
 *
 * ANALOGIA: licznik kilometrów w samochodzie. Po 999 999 pokazuje 000 000 — nie „wie”, że przejechałeś milion.
 *   int ma taki licznik, tylko że po największej wartości przeskakuje na NAJMNIEJSZĄ (ujemną).
 *   Math.addExact to licznik, który zamiast przeskoczyć — zapala czerwoną lampkę (wyjątek).
 *
 * JAK TO DZIAŁA:
 *   int: 32 bity, zakres −2 147 483 648 .. 2 147 483 647 (zapis w kodzie uzupełnień do dwóch, U2)
 *     Integer.MAX_VALUE + 1 == Integer.MIN_VALUE        (zawinięcie)
 *   Typ wyniku zależy od typów ARGUMENTÓW, nie od zmiennej, do której zapisujesz:
 *     long x = int * int;    → mnożenie na int (może się przepełnić), dopiero potem zamiana na long
 *   Dzielenie:   7 / 2 == 3 (obcięcie),   -7 / 2 == -3 (w stronę zera),   Math.floorDiv(-7, 2) == -4 (w dół)
 *   Reszta:      -7 % 3 == -1,   Math.floorMod(-7, 3) == 2
 *   Bity:        {@code n & 1} (parzystość), {@code n & (n - 1)} (potęga dwójki), {@code 1 << k} (2^k),
 *                {@code x >> 1} (dzielenie przez 2)
 *
 * SŁÓWKA:
 *   overflow = przepełnienie; exact = dokładny; floor = podłoga (zaokrąglenie w dół); ceil (ceiling) = sufit;
 *   div = dzielenie; mod = reszta (modulo); literal = literał (wartość wpisana w kodzie); binary = dwójkowy;
 *   hex (hexadecimal) = szesnastkowy; octal = ósemkowy; radix = podstawa systemu; shift = przesunięcie;
 *   mask = maska bitowa; flag = flaga; bit count = liczba jedynek; compare = porównaj; grosze = setne części złotego.
 *
 * ZOBACZ TEŻ: t01_basics/Basics02PrimitiveTypes (zakresy typów), t01_basics/Basics04Operators (operatory
 *             bitowe), t15_numbers/Numbers03BigInteger (liczby bez limitu), t12_collections/Collections07ComparableComparator
 *             (komparatory), t15_numbers/Numbers02MoneyValueObject (pieniądze jako obiekt).
 * </pre>
 */
public class Numbers05IntegerTricks {

    public static void main(String[] args) {
        title("Numbers05 — sztuczki i pułapki liczb całkowitych");

        silentOverflow();           // silent overflow = ciche przepełnienie
        intTimesIntToLong();        // int times int to long = int razy int do long
        exactMethods();             // exact methods = metody „dokładne”
        absOfMinValue();            // abs of MIN_VALUE = wartość bezwzględna MIN_VALUE
        roundingUpDivision();       // rounding-up division = dzielenie z zaokrągleniem w górę
        divModNegatives();          // div/mod negatives = dzielenie i reszta dla liczb ujemnych
        numberSystems();            // number systems = systemy liczbowe
        bitTricks();                // bit tricks = sztuczki bitowe
        comparatorBySubtraction();  // comparator by subtraction = komparator przez odejmowanie
        moneyInGrosze();            // money in grosze = pieniądze w groszach
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. CICHE PRZEPEŁNIENIE: MAX_VALUE + 1
    // =================================================================================================

    /**
     * 1. Po przekroczeniu zakresu int „zawija się”: za największą liczbą jest najmniejsza.
     * Java nie rzuca wyjątku — to świadoma decyzja (szybkość), ale źródło wielu błędów.
     */
    static void silentOverflow() {
        section("1. Ciche przepełnienie: MAX_VALUE + 1");

        int max = Integer.MAX_VALUE;                                  // MAX_VALUE = największa wartość
        int min = Integer.MIN_VALUE;                                  // MIN_VALUE = najmniejsza wartość
        show("Integer.MAX_VALUE", max);
        show("Integer.MAX_VALUE + 1", max + 1);
        show("Integer.MIN_VALUE - 1", min - 1);
        // WYNIK: Integer.MAX_VALUE → 2147483647
        // WYNIK: Integer.MAX_VALUE + 1 → -2147483648    ← licznik przeskoczył na minimum
        // WYNIK: Integer.MIN_VALUE - 1 → 2147483647

        show("(byte) 200", (byte) 200);                               // byte: −128..127
        show("Long.MAX_VALUE + 1", Long.MAX_VALUE + 1);
        // WYNIK: (byte) 200 → -56    ← 200 − 256
        // WYNIK: Long.MAX_VALUE + 1 → -9223372036854775808

        // Realny przykład: licznik odwiedzin strony w int po ~2,1 mld wyświetleń pokaże liczbę ujemną.
        // PUŁAPKA: przepełnienie nie jest błędem kompilacji ani wyjątkiem — program liczy dalej, tylko źle.
    }

    // =================================================================================================
    // 2. int × int PRZEPEŁNIA SIĘ, ZANIM TRAFI DO long
    // =================================================================================================

    /**
     * 2. {@code long x = a * b;} — gdy a i b są typu int, mnożenie odbywa się na int. Przepełniony wynik
     * jest dopiero POTEM zamieniany na long. Zmienna long po lewej stronie niczego nie ratuje.
     */
    static void intTimesIntToLong() {
        section("2. int × int przepełnia się, zanim trafi do long");

        long millisPerYearBad = 365 * 24 * 60 * 60 * 1000;            // wszystko int!
        long millisPerYearGood = 365L * 24 * 60 * 60 * 1000;          // 365L — od początku long
        show("milisekund w roku (int)", millisPerYearBad);
        show("milisekund w roku (365L)", millisPerYearGood);
        // WYNIK: milisekund w roku (int) → 1471228928    ← zła liczba, choć zmienna jest long
        // WYNIK: milisekund w roku (365L) → 31536000000

        int a = 100_000;
        long productBad = a * a;
        long productGood = (long) a * a;                              // rzutowanie PIERWSZEGO argumentu
        long productAlsoBad = (long) (a * a);                         // za późno — nawias liczy się pierwszy
        show("a * a", productBad);
        show("(long) a * a", productGood);
        show("(long) (a * a)", productAlsoBad);
        // WYNIK: a * a → 1410065408
        // WYNIK: (long) a * a → 10000000000
        // WYNIK: (long) (a * a) → 1410065408    ← przepełnienie nastąpiło w nawiasie

        // DOBRA PRAKTYKA: gdy wynik może przekroczyć int, zamień na long PIERWSZY argument (365L, (long) a)
        //   — wtedy całe wyrażenie liczy się na long.
    }

    // =================================================================================================
    // 3. Math.addExact, multiplyExact, toIntExact
    // =================================================================================================

    /**
     * 3. Metody „Exact” (Java 8+) robią to samo co zwykłe operatory, ale przy przepełnieniu rzucają
     * ArithmeticException. Kosztują minimalnie więcej, a zamieniają cichy błąd w głośny.
     */
    static void exactMethods() {
        section("3. Math.addExact, multiplyExact, toIntExact — głośne przepełnienie");

        show("Math.addExact(2, 3)", Math.addExact(2, 3));
        // WYNIK: Math.addExact(2, 3) → 5

        expectThrows("Math.addExact(MAX_VALUE, 1)", () -> Math.addExact(Integer.MAX_VALUE, 1));
        expectThrows("Math.multiplyExact(100_000, 100_000)", () -> Math.multiplyExact(100_000, 100_000));
        expectThrows("Math.multiplyExact(long, long)", () -> Math.multiplyExact(Long.MAX_VALUE, 2L));
        // WYNIK: ✔ Math.addExact(MAX_VALUE, 1) → rzucono ArithmeticException: integer overflow
        // WYNIK: ✔ Math.multiplyExact(100_000, 100_000) → rzucono ArithmeticException: integer overflow
        // WYNIK: ✔ Math.multiplyExact(long, long) → rzucono ArithmeticException: long overflow

        // toIntExact — bezpieczna zamiana long → int (zwykłe rzutowanie (int) obcina po cichu):
        long big = 3_000_000_000L;
        show("(int) 3_000_000_000L", (int) big);
        expectThrows("Math.toIntExact(3_000_000_000L)", () -> Math.toIntExact(big));
        // WYNIK: (int) 3_000_000_000L → -1294967296
        // WYNIK: ✔ Math.toIntExact(3_000_000_000L) → rzucono ArithmeticException: integer overflow

        expectThrows("Math.negateExact(MIN_VALUE)", () -> Math.negateExact(Integer.MIN_VALUE));
        // WYNIK: ✔ Math.negateExact(MIN_VALUE) → rzucono ArithmeticException: integer overflow
        // (negate = zmień znak; dlaczego −MIN_VALUE się nie mieści — sekcja 4)

        // DOBRA PRAKTYKA: w obliczeniach na pieniądzach, licznikach i rozmiarach używaj *Exact.
        //   Wyjątek w teście jest o niebo lepszy niż ujemne saldo w raporcie.
    }

    // =================================================================================================
    // 4. Math.abs(Integer.MIN_VALUE) JEST UJEMNE
    // =================================================================================================

    /**
     * 4. Zakres int jest niesymetryczny: −2 147 483 648 .. 2 147 483 647. Liczba przeciwna do MIN_VALUE
     * (+2 147 483 648) się nie mieści, więc {@code Math.abs(MIN_VALUE)} zwraca... MIN_VALUE.
     */
    static void absOfMinValue() {
        section("4. Math.abs(Integer.MIN_VALUE) jest ujemne!");

        show("Math.abs(-5)", Math.abs(-5));
        show("Math.abs(Integer.MIN_VALUE)", Math.abs(Integer.MIN_VALUE));
        show("-Integer.MIN_VALUE", -Integer.MIN_VALUE);
        // WYNIK: Math.abs(-5) → 5
        // WYNIK: Math.abs(Integer.MIN_VALUE) → -2147483648    ← wartość bezwzględna UJEMNA!
        // WYNIK: -Integer.MIN_VALUE → -2147483648

        expectThrows("Math.absExact(MIN_VALUE) (Java 15+)", () -> Math.absExact(Integer.MIN_VALUE));
        // WYNIK: ✔ Math.absExact(MIN_VALUE) (Java 15+) → rzucono ArithmeticException: Overflow to represent absolute value of Integer.MIN_VALUE

        // Realna pułapka: numer „kubełka” (np. serwera) liczony z hashCode.
        int hash = "polygenelubricants".hashCode();                   // ten napis ma hashCode == MIN_VALUE
        show("\"polygenelubricants\".hashCode()", hash);
        show("Math.abs(hash) % 10", Math.abs(hash) % 10);
        show("Math.floorMod(hash, 10)", Math.floorMod(hash, 10));
        // WYNIK: "polygenelubricants".hashCode() → -2147483648
        // WYNIK: Math.abs(hash) % 10 → -8    ← ujemny numer kubełka → ArrayIndexOutOfBoundsException
        // WYNIK: Math.floorMod(hash, 10) → 2    ← zawsze 0..9

        // DOBRA PRAKTYKA: indeks z hashCode licz jako Math.floorMod(hash, n), nigdy Math.abs(hash) % n.
    }

    // =================================================================================================
    // 5. DZIELENIE Z ZAOKRĄGLENIEM W GÓRĘ: (a + b − 1) / b
    // =================================================================================================

    /**
     * 5. Ile stron potrzeba na 95 produktów po 10 na stronę? 95 / 10 = 9 — o jedną za mało, bo dzielenie
     * całkowite obcina. Klasyczna sztuczka dla liczb nieujemnych: {@code (a + b - 1) / b}.
     * W Javie 18+ jest gotowe {@code Math.ceilDiv(a, b)} — w Javie 17 go nie ma.
     */
    static void roundingUpDivision() {
        section("5. Dzielenie z zaokrągleniem w górę: (a + b − 1) / b");

        int items = 95;
        int perPage = 10;
        show("95 / 10", items / perPage);
        show("(95 + 10 - 1) / 10", (items + perPage - 1) / perPage);
        show("(100 + 10 - 1) / 10", (100 + perPage - 1) / perPage);
        // WYNIK: 95 / 10 → 9    ← 5 produktów bez strony!
        // WYNIK: (95 + 10 - 1) / 10 → 10
        // WYNIK: (100 + 10 - 1) / 10 → 10    ← równo 100 → dalej 10 stron, nie 11

        // PUŁAPKA: Math.ceil na wyniku dzielenia CAŁKOWITEGO nic nie da — ułamek zginął wcześniej.
        show("Math.ceil(95 / 10)", Math.ceil(items / perPage));
        show("Math.ceil(95 / 10.0)", Math.ceil(items / 10.0));
        // WYNIK: Math.ceil(95 / 10) → 9.0    ← 95 / 10 == 9 (int), ceil(9) == 9
        // WYNIK: Math.ceil(95 / 10.0) → 10.0    ← działa, ale przez double (i zwraca double)

        // Wersja bez ryzyka przepełnienia a + b − 1 (i działająca dla liczb ujemnych): −floorDiv(−a, b)
        show("-Math.floorDiv(-95, 10)", -Math.floorDiv(-items, perPage));
        // WYNIK: -Math.floorDiv(-95, 10) → 10
        // (Java 18+) Math.ceilDiv(95, 10) → 10 — gdy przejdziesz na nowszą Javę, użyj tej metody.
    }

    // =================================================================================================
    // 6. / I % KONTRA floorDiv I floorMod DLA LICZB UJEMNYCH
    // =================================================================================================

    /**
     * 6. Operator {@code /} zaokrągla w stronę ZERA, a {@code %} ma znak DZIELNEJ. Dla liczb ujemnych daje to
     * wyniki inne niż w matematyce. {@code Math.floorDiv} zaokrągla w DÓŁ, a {@code Math.floorMod} ma znak DZIELNIKA
     * (dla dodatniego dzielnika — zawsze 0..b−1).
     */
    static void divModNegatives() {
        section("6. / i % kontra floorDiv i floorMod dla liczb ujemnych");

        System.out.println(String.format(Locale.ROOT, "   %3s %3s | %4s %4s | %9s %9s", "a", "b", "a/b", "a%b", "floorDiv", "floorMod"));
        int[][] pairs = {{7, 3}, {-7, 3}, {7, -3}, {-7, -3}};
        for (int[] p : pairs) {
            int a = p[0];
            int b = p[1];
            System.out.println(String.format(Locale.ROOT, "   %3d %3d | %4d %4d | %9d %9d",
                    a, b, a / b, a % b, Math.floorDiv(a, b), Math.floorMod(a, b)));
        }
        // WYNIK: a   b |  a/b  a%b |  floorDiv  floorMod
        // WYNIK: 7   3 |    2    1 |         2         1
        // WYNIK: -7   3 |   -2   -1 |        -3         2
        // WYNIK: 7  -3 |   -2    1 |        -3        -2
        // WYNIK: -7  -3 |    2   -1 |         2        -1
        // Zawsze zachodzi: a == (a / b) * b + (a % b)  oraz  a == floorDiv(a, b) * b + floorMod(a, b).

        // Zastosowanie: dzień tygodnia 10 dni WSTECZ od środy (0 = pon, 1 = wt, 2 = śr, ..., 6 = nd).
        String[] days = {"pon", "wt", "śr", "czw", "pt", "sob", "nd"};
        int wednesday = 2;
        show("(2 - 10) % 7", (wednesday - 10) % 7);
        show("Math.floorMod(2 - 10, 7)", Math.floorMod(wednesday - 10, 7) + " = " + days[Math.floorMod(wednesday - 10, 7)]);
        // WYNIK: (2 - 10) % 7 → -1    ← days[-1] → ArrayIndexOutOfBoundsException
        // WYNIK: Math.floorMod(2 - 10, 7) → 6 = nd    ← 10 dni przed środą była niedziela

        // PUŁAPKA: sprawdzanie nieparzystości przez n % 2 == 1 nie działa dla liczb ujemnych.
        show("-3 % 2 == 1 ?", -3 % 2 == 1);
        show("-3 % 2 != 0 ?", -3 % 2 != 0);
        // WYNIK: -3 % 2 == 1 ? → false    ← -3 % 2 == -1, więc „−3 nie jest nieparzyste”?!
        // WYNIK: -3 % 2 != 0 ? → true
    }

    // =================================================================================================
    // 7. SYSTEMY LICZBOWE: LITERAŁY 0b, 0x, 0 I PODKREŚLENIA
    // =================================================================================================

    /**
     * 7. Literały: {@code 0b1010} (dwójkowy), {@code 0x1F} (szesnastkowy), {@code 017} (ÓSEMKOWY — uwaga na zero
     * na początku!), podkreślenia {@code 1_000_000} dla czytelności. Konwersje: {@code Integer.toBinaryString},
     * {@code toHexString}, {@code toOctalString} i odwrotnie {@code Integer.parseInt(tekst, podstawa)}.
     */
    static void numberSystems() {
        section("7. Systemy liczbowe: 0b, 0x, 0 i podkreślenia");

        show("0b1010", 0b1010);
        show("0x1F", 0x1F);
        show("017", 017);
        show("1_000_000", 1_000_000);
        // WYNIK: 0b1010 → 10
        // WYNIK: 0x1F → 31
        // WYNIK: 017 → 15    ← zero na początku = system ÓSEMKOWY: 1×8 + 7
        // WYNIK: 1_000_000 → 1000000
        // PUŁAPKA: kod pocztowy albo numer zapisany jako int 010 to w Javie... 8. Takie dane trzymaj jako String.
        // Podkreślenia nie mogą stać na początku, na końcu ani obok kropki: _1000, 1000_, 1_.5 — błąd kompilacji.

        show("Integer.toBinaryString(42)", Integer.toBinaryString(42));
        show("Integer.toHexString(255)", Integer.toHexString(255));
        show("Integer.toOctalString(8)", Integer.toOctalString(8));
        show("Integer.parseInt(\"101010\", 2)", Integer.parseInt("101010", 2));
        show("Integer.parseInt(\"ff\", 16)", Integer.parseInt("ff", 16));
        // WYNIK: Integer.toBinaryString(42) → 101010
        // WYNIK: Integer.toHexString(255) → ff
        // WYNIK: Integer.toOctalString(8) → 10
        // WYNIK: Integer.parseInt("101010", 2) → 42
        // WYNIK: Integer.parseInt("ff", 16) → 255

        // Liczby ujemne w U2 — same jedynki dla −1:
        show("Integer.toBinaryString(-1)", Integer.toBinaryString(-1));
        show("Integer.toHexString(-1)", Integer.toHexString(-1));
        // WYNIK: Integer.toBinaryString(-1) → 11111111111111111111111111111111
        // WYNIK: Integer.toHexString(-1) → ffffffff

        // toBinaryString nie dopisuje zer z przodu. Dopełnienie do 8 znaków:
        show("5 jako 8 bitów", String.format(Locale.ROOT, "%8s", Integer.toBinaryString(5)).replace(' ', '0'));
        // WYNIK: 5 jako 8 bitów → 00000101
    }

    // =================================================================================================
    // 8. SZTUCZKI BITOWE: n & 1, n & (n − 1), PRZESUNIĘCIA, FLAGI
    // =================================================================================================

    /**
     * 8. Operatory bitowe działają na pojedynczych bitach: {@code &} (i), {@code |} (lub), {@code ^} (albo),
     * {@code ~} (negacja), {@code <<} (w lewo), {@code >>} (w prawo ze znakiem), {@code >>>} (w prawo bez znaku).
     * Sztuczki warto rozpoznawać w cudzym kodzie — we własnym pisz czytelnie, chyba że liczy się wydajność.
     */
    static void bitTricks() {
        section("8. Sztuczki bitowe: n & 1, n & (n − 1), przesunięcia, flagi");

        // n & 1 — ostatni bit: 1 = nieparzysta (działa też dla ujemnych!)
        show("7 & 1 / 8 & 1 / -3 & 1", (7 & 1) + " / " + (8 & 1) + " / " + (-3 & 1));
        // WYNIK: 7 & 1 / 8 & 1 / -3 & 1 → 1 / 0 / 1

        // n & (n − 1) — kasuje najmłodszą jedynkę. Wynik 0 (dla n > 0) ⇔ n jest potęgą dwójki.
        for (int n : new int[]{64, 96, 1}) {
            show(n + " & " + (n - 1) + " == 0 ?", (n & (n - 1)) == 0);
        }
        // WYNIK: 64 & 63 == 0 ? → true    ← 1000000 & 0111111
        // WYNIK: 96 & 95 == 0 ? → false
        // WYNIK: 1 & 0 == 0 ? → true    ← 1 = 2^0

        show("1 << 10", 1 << 10);
        show("5 << 1", 5 << 1);
        show("40 >> 2", 40 >> 2);
        // WYNIK: 1 << 10 → 1024    ← 2^10
        // WYNIK: 5 << 1 → 10    ← przesunięcie w lewo o 1 = × 2
        // WYNIK: 40 >> 2 → 10    ← przesunięcie w prawo o 2 = / 4

        show("-7 >> 1 / -7 / 2", (-7 >> 1) + " / " + (-7 / 2));
        show("-8 >>> 28", -8 >>> 28);
        // WYNIK: -7 >> 1 / -7 / 2 → -4 / -3    ← >> zaokrągla w dół, / w stronę zera
        // WYNIK: -8 >>> 28 → 15    ← >>> wsuwa zera z lewej: liczba przestaje być ujemna

        show("Integer.bitCount(255)", Integer.bitCount(255));        // bit count = liczba jedynek
        // WYNIK: Integer.bitCount(255) → 8

        // Flagi w jednej liczbie (maska bitowa) — np. uprawnienia do pliku:
        final int read = 1;                                           // 001 — odczyt
        final int write = 2;                                          // 010 — zapis
        final int execute = 4;                                        // 100 — uruchamianie
        int permissions = read | write;                               // | ustawia bity → 011
        show("uprawnienia binarnie", Integer.toBinaryString(permissions));
        show("ma zapis?", (permissions & write) != 0);
        show("ma uruchamianie?", (permissions & execute) != 0);
        permissions &= ~write;                                        // & ~ kasuje bit zapisu → 001
        show("po odebraniu zapisu", Integer.toBinaryString(permissions));
        // WYNIK: uprawnienia binarnie → 11
        // WYNIK: ma zapis? → true
        // WYNIK: ma uruchamianie? → false
        // WYNIK: po odebraniu zapisu → 1
        // DOBRA PRAKTYKA: w zwykłym kodzie zamiast masek bitowych użyj EnumSet (t08_enums/Enums04EnumMapSet) —
        //   jest równie szybki, a dużo czytelniejszy.
    }

    // =================================================================================================
    // 9. KOMPARATOR PRZEZ ODEJMOWANIE → Integer.compare
    // =================================================================================================

    /**
     * 9. Stary „sprytny” komparator {@code (a, b) -> a - b} działa... dopóki odejmowanie się nie przepełni.
     * Dla dużych liczb o różnych znakach a − b zmienia znak i sortowanie wychodzi błędne.
     * {@code Integer.compare(a, b)} nie odejmuje, tylko porównuje — zawsze działa.
     */
    static void comparatorBySubtraction() {
        section("9. Komparator przez odejmowanie → Integer.compare");

        Comparator<Integer> bySubtraction = (a, b) -> a - b;          // ŹLE
        Comparator<Integer> byCompare = Integer::compare;              // DOBRZE

        show("bySubtraction.compare(MIN_VALUE, 1)", bySubtraction.compare(Integer.MIN_VALUE, 1));
        show("byCompare.compare(MIN_VALUE, 1)", byCompare.compare(Integer.MIN_VALUE, 1));
        // WYNIK: bySubtraction.compare(MIN_VALUE, 1) → 2147483647    ← „MIN_VALUE jest WIĘKSZE od 1”?!
        // WYNIK: byCompare.compare(MIN_VALUE, 1) → -1

        List<Integer> numbers = new ArrayList<>(List.of(5, Integer.MIN_VALUE, -3, Integer.MAX_VALUE, 0));
        List<Integer> sortedBad = new ArrayList<>(numbers);
        sortedBad.sort(bySubtraction);
        List<Integer> sortedGood = new ArrayList<>(numbers);
        sortedGood.sort(byCompare);
        show("sort przez odejmowanie", sortedBad);
        show("sort przez Integer.compare", sortedGood);
        // WYNIK: sort przez odejmowanie → [0, 5, 2147483647, -2147483648, -3]    ← liczby ujemne na końcu — chaos!
        // WYNIK: sort przez Integer.compare → [-2147483648, -3, 0, 5, 2147483647]
        // Uwaga: przy BŁĘDNYM komparatorze dokładna kolejność wyniku zależy od algorytmu sortowania w danej wersji JDK
        // (tu: Java 17). Na innej wersji „chaos” może wyglądać inaczej — ważne jest to, że wynik jest NIEPOPRAWNY.
        // Przy komparatorze łamiącym kontrakt sortowanie może nawet rzucić IllegalArgumentException
        // („Comparison method violates its general contract!”) — zwykle przy większych listach.

        // DOBRA PRAKTYKA: Integer.compare(a, b), Long.compare, Comparator.comparingInt(Klasa::pole).
        //   Nigdy a - b w compare/compareTo — nawet jeśli „u mnie działa” na małych liczbach.
    }

    // =================================================================================================
    // 10. PIENIĄDZE W GROSZACH JAKO long
    // =================================================================================================

    /**
     * 10. Alternatywa dla BigDecimal: przechowuj kwotę jako liczbę GROSZY w long. Dodawanie i mnożenie przez
     * ilość są dokładne i bardzo szybkie. long mieści ok. 92 biliardy złotych — wystarczy. Trzeba tylko uważać
     * przy dzieleniu (procenty, raty) i przy wypisywaniu (część złota i groszowa osobno).
     */
    static void moneyInGrosze() {
        section("10. Pieniądze w groszach jako long");

        long price = 19_99;                                           // 19,99 zł = 1999 gr (podkreślenie jako „przecinek”)
        long total = Math.multiplyExact(price, 3);                    // strażnik przepełnienia
        show("19,99 zł × 3 w groszach", total);
        show("do wyświetlenia", formatGrosze(total));
        // WYNIK: 19,99 zł × 3 w groszach → 5997
        // WYNIK: do wyświetlenia → 59,97 zł

        show("formatGrosze(5)", formatGrosze(5));
        show("formatGrosze(-150)", formatGrosze(-150));
        // WYNIK: formatGrosze(5) → 0,05 zł
        // WYNIK: formatGrosze(-150) → -1,50 zł

        // PUŁAPKA: przy liczbach ujemnych -150 / 100 == -1, a -150 % 100 == -50 → „-1,-50 zł”.
        //   Dlatego formatGrosze bierze wartość bezwzględną i dopisuje znak osobno.
        show("naiwnie: -150 / 100 + \",\" + -150 % 100", -150 / 100 + "," + -150 % 100);
        // WYNIK: naiwnie: -150 / 100 + "," + -150 % 100 → -1,-50

        // 10% rabatu od 19,99 zł: 1999 × 10 / 100 = 199.9 gr → dzielenie całkowite obcina do 199.
        long discount = price * 10 / 100;
        show("rabat 10% od 1999 gr (obcięty)", discount);
        show("rabat 10% zaokrąglony (+ 50) / 100", (price * 10 + 50) / 100);
        // WYNIK: rabat 10% od 1999 gr (obcięty) → 199
        // WYNIK: rabat 10% zaokrąglony (+ 50) / 100 → 200    ← „+ połowa dzielnika” = zaokrąglenie HALF_UP (dla ≥ 0)

        // DOBRA PRAKTYKA: grosze w long — gdy liczysz głównie sumy i iloczyny (koszyk, saldo) i zależy Ci na
        //   szybkości. BigDecimal/Money — gdy dużo procentów, dzielenia i walut (Numbers02MoneyValueObject).
    }

    /** formatGrosze = sformatuj grosze jako „złote,grosze zł” (poprawnie także dla kwot ujemnych). */
    static String formatGrosze(long grosze) {
        String sign = grosze < 0 ? "-" : "";
        long abs = Math.abs(grosze);                                  // Long.MIN_VALUE groszy nie obsługujemy
        return sign + abs / 100 + "," + String.format(Locale.ROOT, "%02d", abs % 100) + " zł";
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   PRZEPEŁNIENIE:  MAX_VALUE + 1 == MIN_VALUE — po cichu!  Strażnicy: Math.addExact, multiplyExact,
     *                   subtractExact, negateExact, toIntExact (long → int), absExact (Java 15+)
     *   long x = a * b  → liczone na int!  Pisz (long) a * b  albo  365L * 24 * ...
     *   Math.abs(Integer.MIN_VALUE) < 0  →  indeks z hashCode: Math.floorMod(hash, n)
     *   W GÓRĘ:         (a + b − 1) / b  (a ≥ 0);  −Math.floorDiv(−a, b);  Math.ceilDiv (Java 18+)
     *   UJEMNE:         / w stronę zera, % znak dzielnej;  floorDiv w dół, floorMod 0..b−1 (b > 0)
     *   NIEPARZYSTA:    n % 2 != 0  albo  (n & 1) == 1   — nigdy n % 2 == 1
     *   LITERAŁY:       0b1010 = 10, 0x1F = 31, 017 = 15 (ósemkowo!), 1_000_000
     *   KONWERSJE:      Integer.toBinaryString / toHexString / toOctalString;  Integer.parseInt(s, 2 / 16)
     *   BITY:           n & (n − 1) == 0 → potęga 2;  1 << k = 2^k;  >> ze znakiem, >>> bez;  bitCount
     *   FLAGI:          ustaw |, sprawdź (x & FLAG) != 0, skasuj &= ~FLAG  (w zwykłym kodzie: EnumSet)
     *   KOMPARATOR:     Integer.compare(a, b) — nigdy a − b
     *   GROSZE:         long; wypisywanie: znak osobno + abs / 100 + "," + %02d z abs % 100
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego long x = 50_000 * 50_000; daje złą wartość, choć x jest typu long? Jak to poprawić?
     *   2. Co wypisze:  System.out.println(-7 / 2 + " " + -7 % 2 + " " + Math.floorDiv(-7, 2) + " " + Math.floorMod(-7, 2));  ?
     *   3. ZNAJDŹ BŁĄD:
     *          int bucket = Math.abs(key.hashCode()) % buckets.length;
     *          buckets[bucket].add(key);
     *   4. ZNAJDŹ BŁĄD:
     *          Comparator<Account> byBalance = (a, b) -> a.balance() - b.balance();   // balance to int
     *   5. Co wypisze:  System.out.println(010 + 0x10 + 0b10);  ?
     *   6. ZNAJDŹ BŁĄD:  int pages = (int) Math.ceil(items / perPage);   // items i perPage to int
     *   7. Jak jednym wyrażeniem sprawdzić, czy dodatnie n jest potęgą dwójki? Dlaczego to działa?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Uruchamia ćwiczenia: najpierw TWOJE rozwiązania (✘ dopóki nie uzupełnisz), potem wzorcowe (✔). */
    static void exercises() {
        List<Integer> toSort = List.of(5, Integer.MIN_VALUE, -3, Integer.MAX_VALUE, 0);
        List<Integer> descending = List.of(Integer.MAX_VALUE, 5, 0, -3, Integer.MIN_VALUE);

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: 95 produktów po 10", 10, () -> exercise1(95, 10));
        Check.equal("ćw. 1b: 100 produktów po 10", 10, () -> exercise1(100, 10));
        Check.equal("ćw. 1c: 0 produktów", 0, () -> exercise1(0, 10));
        Check.equal("ćw. 2a: 1024 potęgą dwójki", true, () -> exercise2(1024));
        Check.equal("ćw. 2b: 12 nie", false, () -> exercise2(12));
        Check.equal("ćw. 2c: 0 nie", false, () -> exercise2(0));
        Check.equal("ćw. 2d: MIN_VALUE nie", false, () -> exercise2(Integer.MIN_VALUE));
        Check.equal("ćw. 3: sortowanie malejąco", descending, () -> sortedWith(toSort, exercise3()));
        Check.equal("ćw. 4a: środa + 10 dni", "sob", () -> exercise4(2, 10));
        Check.equal("ćw. 4b: poniedziałek − 1 dzień", "nd", () -> exercise4(0, -1));
        Check.equal("ćw. 4c: piątek − 30 dni", "śr", () -> exercise4(4, -30));
        Check.equal("ćw. 5a: 5 na 8 bitach", "00000101", () -> exercise5(5));
        Check.equal("ćw. 5b: 200 na 8 bitach", "11001000", () -> exercise5(200));
        Check.equal("ćw. 5c: -1 na 8 bitach", "11111111", () -> exercise5(-1));
        Check.equal("ćw. 5d: 259 na 8 bitach", "00000011", () -> exercise5(259));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", 10, () -> solution1(95, 10));
        Check.equal("ćw. 1b (wzorzec)", 10, () -> solution1(100, 10));
        Check.equal("ćw. 1c (wzorzec)", 0, () -> solution1(0, 10));
        Check.equal("ćw. 2a (wzorzec)", true, () -> solution2(1024));
        Check.equal("ćw. 2b (wzorzec)", false, () -> solution2(12));
        Check.equal("ćw. 2c (wzorzec)", false, () -> solution2(0));
        Check.equal("ćw. 2d (wzorzec)", false, () -> solution2(Integer.MIN_VALUE));
        Check.equal("ćw. 3 (wzorzec)", descending, () -> sortedWith(toSort, solution3()));
        Check.equal("ćw. 4a (wzorzec)", "sob", () -> solution4(2, 10));
        Check.equal("ćw. 4b (wzorzec)", "nd", () -> solution4(0, -1));
        Check.equal("ćw. 4c (wzorzec)", "śr", () -> solution4(4, -30));
        Check.equal("ćw. 5a (wzorzec)", "00000101", () -> solution5(5));
        Check.equal("ćw. 5b (wzorzec)", "11001000", () -> solution5(200));
        Check.equal("ćw. 5c (wzorzec)", "11111111", () -> solution5(-1));
        Check.equal("ćw. 5d (wzorzec)", "00000011", () -> solution5(259));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 15 OK, ✘ 0 BŁĄD
    }

    /** sortedWith = posortowana kopia listy (pomocnik do ćwiczenia 3). */
    private static List<Integer> sortedWith(List<Integer> numbers, Comparator<Integer> comparator) {
        List<Integer> copy = new ArrayList<>(numbers);
        copy.sort(comparator);
        return copy;
    }

    /**
     * ĆWICZENIE 1 (łatwe): ile stron potrzeba na {@code items} produktów, gdy na stronę mieści się {@code perPage}?
     * Liczby są nieujemne, perPage > 0. Bez double i bez Math.ceil.
     * Podpowiedź: (a + b − 1) / b.
     */
    static int exercise1(int items, int perPage) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 2 (łatwe): czy n jest potęgą dwójki (1, 2, 4, 8, ...)? Dla 0 i liczb ujemnych — false.
     * Podpowiedź: n większe od zera i {@code (n & (n - 1)) == 0}. Nawias wokół {@code n & (n - 1)} jest konieczny —
     * {@code ==} ma wyższy priorytet niż {@code &}!
     */
    static boolean exercise2(int n) {
        // TODO: twoje rozwiązanie
        return true;
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ komparator sortujący MALEJĄCO tak, żeby działał dla wszystkich int:
     * <pre>{@code
     * Comparator<Integer> descending = (a, b) -> b - a;     // psuje się dla MIN_VALUE i MAX_VALUE
     * }</pre>
     * Podpowiedź: Integer.compare(b, a) (zamieniona kolejność) albo Comparator.reverseOrder().
     */
    static Comparator<Integer> exercise3() {
        // TODO: twoje rozwiązanie
        return (a, b) -> b - a;
    }

    /**
     * ĆWICZENIE 4 (średnie): dni tygodnia mają numery 0 = "pon", 1 = "wt", 2 = "śr", 3 = "czw", 4 = "pt",
     * 5 = "sob", 6 = "nd". Zwróć skrót dnia, który wypada {@code days} dni po dniu {@code startIndex}
     * (days może być ujemne — dni wstecz).
     * Podpowiedź: tablica skrótów + Math.floorMod(startIndex + days, 7). Operator % dałby ujemny indeks.
     */
    static String exercise4(int startIndex, int days) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): zwróć 8 NAJMŁODSZYCH bitów liczby jako napis z zerami z przodu:
     * 5 → "00000101", 200 → "11001000", -1 → "11111111", 259 → "00000011" (259 = 256 + 3).
     * Podpowiedź: {@code value & 0xFF} zostawia tylko 8 ostatnich bitów; potem Integer.toBinaryString
     * i dopełnienie zerami: {@code String.format(Locale.ROOT, "%8s", tekst).replace(' ', '0')}.
     */
    static String exercise5(int value) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int items, int perPage) {
        return (items + perPage - 1) / perPage;
    }

    static boolean solution2(int n) {
        return n > 0 && (n & (n - 1)) == 0;                           // n > 0 odrzuca 0 i MIN_VALUE
    }

    static Comparator<Integer> solution3() {
        return (a, b) -> Integer.compare(b, a);                       // albo Comparator.reverseOrder()
    }

    static String solution4(int startIndex, int days) {
        String[] names = {"pon", "wt", "śr", "czw", "pt", "sob", "nd"};
        return names[Math.floorMod(startIndex + days, 7)];
    }

    static String solution5(int value) {
        String bits = Integer.toBinaryString(value & 0xFF);           // 0xFF = 11111111 — maska 8 bitów
        return String.format(Locale.ROOT, "%8s", bits).replace(' ', '0');
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 50_000 * 50_000 liczy się na int (oba argumenty to int) i przepełnia, a dopiero zły wynik trafia do long.
     *      Poprawnie: 50_000L * 50_000 albo (long) 50_000 * 50_000 → 2500000000.
     *   2. „-3 -1 -4 1” — / zaokrągla w stronę zera, % ma znak dzielnej; floorDiv zaokrągla w dół, floorMod ≥ 0.
     *   3. Dla hashCode równego Integer.MIN_VALUE Math.abs zwraca liczbę ujemną, więc bucket może być ujemny →
     *      ArrayIndexOutOfBoundsException. Poprawnie: Math.floorMod(key.hashCode(), buckets.length).
     *   4. Odejmowanie może się przepełnić (np. saldo 2 000 000 000 i −2 000 000 000) i komparator zwróci zły znak.
     *      Poprawnie: Integer.compare(a.balance(), b.balance()) albo Comparator.comparingInt(Account::balance).
     *   5. „26” — 010 to ósemkowo 8, 0x10 to 16, 0b10 to 2; 8 + 16 + 2 = 26.
     *   6. items / perPage to dzielenie całkowite — ułamek ginie PRZED Math.ceil. Poprawnie: (items + perPage − 1)
     *      / perPage albo Math.ceil((double) items / perPage).
     *   7. n > 0 && (n & (n − 1)) == 0. Potęga dwójki ma w zapisie dwójkowym jedną jedynkę (1000...0); n − 1
     *      to same jedynki w młodszych bitach (0111...1), więc iloczyn bitowy daje 0. Inne liczby mają ≥ 2 jedynki
     *      i najstarsza z nich przetrwa.
     */
    // </editor-fold>
}
