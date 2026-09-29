package t15_numbers;

import helpers.Check;

import java.math.BigInteger;
import java.util.Arrays;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: BigInteger — liczby całkowite dowolnej wielkości
 *        (big integer = duża liczba całkowita; overflow = przepełnienie; factorial = silnia)
 *
 * W SKRÓCIE:
 *   long mieści liczby do ok. 9,2 × 10^18. Silnia 21!, 2^100 czy klucze kryptograficzne są dużo większe.
 *   BigInteger nie ma górnej granicy (ogranicza go tylko pamięć). Jak BigDecimal jest niezmienny
 *   i zamiast operatorów + - * / ma metody add, subtract, multiply, divide.
 *
 * ANALOGIA: licznik kilometrów w starym samochodzie ma 6 bębenków — po 999 999 km pokazuje znowu 000 000.
 *   long to taki licznik z 64 „bębenkami” bitów: po przekroczeniu zakresu „przekręca się” (i to w liczby ujemne!).
 *   BigInteger to kartka, na której dopisujesz kolejne cyfry, ile tylko trzeba — nigdy się nie przekręci.
 *
 * JAK TO DZIAŁA:
 *   BigInteger przechowuje liczbę jako tablicę int-ów (kolejne „kawałki” po 32 bity) + znak.
 *     BigInteger.valueOf(20)            — z long;   new BigInteger("123456789012345678901234567890") — z napisu
 *     a.add(b) a.subtract(b) a.multiply(b) a.divide(b) a.pow(n) a.mod(m) a.modPow(e, m) a.gcd(b)
 *     a.compareTo(b) — porównanie;  a.bitLength() — ile bitów potrzeba;  a.longValueExact() — do long albo wyjątek
 *   Każda operacja zwraca NOWY obiekt — wynik trzeba przypisać.
 *
 * SŁÓWKA:
 *   big integer = duża liczba całkowita; factorial = silnia; overflow = przepełnienie; power (pow) = potęga;
 *   remainder = reszta; mod (modulo) = reszta nieujemna; gcd (greatest common divisor) = największy wspólny
 *   dzielnik; probable prime = prawdopodobnie pierwsza; certainty = pewność; exact = dokładny;
 *   bit length = długość w bitach; radix = podstawa systemu liczbowego; digit = cyfra.
 *
 * ZOBACZ TEŻ: t01_basics/Basics02PrimitiveTypes (zakresy int i long), t15_numbers/Numbers01BigDecimal
 *             (BigDecimal = BigInteger + skala), t15_numbers/Numbers05IntegerTricks (przepełnienie int/long,
 *             Math.multiplyExact), t05_methods/Methods03Recursion (silnia i Fibonacci rekurencyjnie).
 * </pre>
 */
public class Numbers03BigInteger {

    public static void main(String[] args) {
        title("Numbers03 — BigInteger: liczby całkowite bez limitu");

        longOverflow();             // long overflow = przepełnienie long
        creatingBigInteger();       // creating = tworzenie
        bigFactorials();            // big factorials = duże silnie
        arithmetic();               // arithmetic = działania
        modAndModPow();             // mod and modPow = reszta i potęga modulo
        gcdAndFractions();          // gcd and fractions = NWD i ułamki
        primes();                   // primes = liczby pierwsze
        conversions();              // conversions = konwersje
        compareAndBitLength();      // compare and bit length = porównywanie i długość w bitach
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. long SIĘ PRZEPEŁNIA: 21!
    // =================================================================================================

    /**
     * 1. Silnia n! = 1 × 2 × ... × n rośnie bardzo szybko. 20! jeszcze mieści się w long, 21! — już nie.
     * Java NIE zgłasza błędu przy przepełnieniu: wynik „zawija się” i wychodzi bzdura, często ujemna.
     */
    static void longOverflow() {
        section("1. long się przepełnia — silnia 21!");

        show("Long.MAX_VALUE", Long.MAX_VALUE);                       // MAX_VALUE = największa wartość
        // WYNIK: Long.MAX_VALUE → 9223372036854775807

        for (int n = 19; n <= 22; n++) {
            show(n + "! (long)", factorialLong(n));
        }
        // WYNIK: 19! (long) → 121645100408832000
        // WYNIK: 20! (long) → 2432902008176640000
        // WYNIK: 21! (long) → -4249290049419214848    ← silnia UJEMNA? Przepełnienie!
        // WYNIK: 22! (long) → -1250660718674968576

        // Math.multiplyExact (Java 8+) nie pozwala na ciche przepełnienie — rzuca wyjątek:
        expectThrows("20! × 21 przez Math.multiplyExact", () -> Math.multiplyExact(factorialLong(20), 21L));
        // WYNIK: ✔ 20! × 21 przez Math.multiplyExact → rzucono ArithmeticException: long overflow

        // PUŁAPKA: kompilator i JVM milczą. Test z małymi danymi (5!, 10!) przejdzie, a błąd pojawi się dopiero
        //   przy większych liczbach. Gdy wynik może przekroczyć long — BigInteger albo *Exact.
    }

    /** factorialLong = silnia na long — działa tylko do 20! */
    static long factorialLong(int n) {
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;                                              // przy 21 cicho się przepełni
        }
        return result;
    }

    // =================================================================================================
    // 2. TWORZENIE BigInteger
    // =================================================================================================

    /**
     * 2. {@code BigInteger.valueOf(long)} — z liczby; {@code new BigInteger("...")} — z napisu (dowolnie długiego);
     * {@code new BigInteger("ff", 16)} — z napisu w innym systemie liczbowym. Stałe: ZERO, ONE, TWO (Java 9+), TEN.
     */
    static void creatingBigInteger() {
        section("2. Tworzenie: valueOf, z napisu, stałe");

        BigInteger small = BigInteger.valueOf(42);                    // valueOf = wartość z (long)
        BigInteger huge = new BigInteger("123456789012345678901234567890");
        BigInteger fromHex = new BigInteger("ff", 16);                // 16 = system szesnastkowy
        show("valueOf(42)", small);
        show("new BigInteger(\"1234...890\")", huge);
        show("new BigInteger(\"ff\", 16)", fromHex);
        // WYNIK: valueOf(42) → 42
        // WYNIK: new BigInteger("1234...890") → 123456789012345678901234567890
        // WYNIK: new BigInteger("ff", 16) → 255

        show("ZERO, ONE, TWO, TEN", BigInteger.ZERO + ", " + BigInteger.ONE + ", " + BigInteger.TWO + ", " + BigInteger.TEN);
        // WYNIK: ZERO, ONE, TWO, TEN → 0, 1, 2, 10    ← TWO dodano w Javie 9

        expectThrows("new BigInteger(\"12.5\")", () -> new BigInteger("12.5"));
        // WYNIK: ✔ new BigInteger("12.5") → rzucono NumberFormatException: For input string: "12.5"
        // BigInteger to TYLKO liczby całkowite. Ułamki → BigDecimal.

        // DOBRA PRAKTYKA: liczby mieszczące się w long twórz przez valueOf — dla małych wartości (−16..16)
        //   zwraca gotowe, współdzielone obiekty. Napis — gdy liczba przychodzi z pliku albo jest za duża na long.
    }

    // =================================================================================================
    // 3. DUŻE SILNIE: 21!, 25!, 50!
    // =================================================================================================

    /** 3. Silnia na BigInteger: ta sama pętla, tylko {@code result = result.multiply(...)} zamiast {@code *=}. */
    static void bigFactorials() {
        section("3. Duże silnie: 21!, 25!, 50!");

        show("21!", factorial(21));
        show("25!", factorial(25));
        // WYNIK: 21! → 51090942171709440000    ← poprawnie, zamiast ujemnej bzdury z sekcji 1
        // WYNIK: 25! → 15511210043330985984000000

        BigInteger f50 = factorial(50);
        show("50!", f50);
        show("liczba cyfr 50!", f50.toString().length());
        // WYNIK: 50! → 30414093201713378043612608166064768844377641568960512000000000000
        // WYNIK: liczba cyfr 50! → 65

        // Dla porównania: w long mieści się najwyżej 19 cyfr.
        show("1000! ma cyfr", factorial(1000).toString().length());
        // WYNIK: 1000! ma cyfr → 2568
        note("BigInteger liczy 1000! bez problemu — to liczba z 2568 cyframi.");
        // WYNIK: ℹ BigInteger liczy 1000! bez problemu — to liczba z 2568 cyframi.
    }

    /** factorial = silnia na BigInteger (dla n ≥ 0). */
    static BigInteger factorial(int n) {
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));          // przypisanie! BigInteger jest niezmienny
        }
        return result;
    }

    // =================================================================================================
    // 4. DZIAŁANIA: add, subtract, multiply, divide, pow
    // =================================================================================================

    /**
     * 4. Podstawowe działania. divide to dzielenie CAŁKOWITE (część po przecinku ginie, jak {@code /} dla int).
     * {@code divideAndRemainder} zwraca od razu tablicę [iloraz, reszta].
     */
    static void arithmetic() {
        section("4. Działania: add, subtract, multiply, divide, pow");

        BigInteger a = new BigInteger("100000000000000000000");       // 10^20 — więcej niż long
        BigInteger b = BigInteger.valueOf(7);
        show("10^20 + 7", a.add(b));
        show("10^20 − 7", a.subtract(b));
        show("10^20 × 7", a.multiply(b));
        show("10^20 / 7", a.divide(b));
        // WYNIK: 10^20 + 7 → 100000000000000000007
        // WYNIK: 10^20 − 7 → 99999999999999999993
        // WYNIK: 10^20 × 7 → 700000000000000000000
        // WYNIK: 10^20 / 7 → 14285714285714285714    ← dzielenie całkowite, reszta ginie

        BigInteger[] qr = BigInteger.valueOf(17).divideAndRemainder(BigInteger.valueOf(5));
        show("17 divideAndRemainder 5", Arrays.toString(qr));        // [iloraz, reszta]
        // WYNIK: 17 divideAndRemainder 5 → [3, 2]

        show("2^100", BigInteger.TWO.pow(100));                       // pow = potęga (wykładnik to int)
        // WYNIK: 2^100 → 1267650600228229401496703205376

        expectThrows("10^20 / 0", () -> a.divide(BigInteger.ZERO));
        // WYNIK: ✔ 10^20 / 0 → rzucono ArithmeticException: BigInteger divide by zero

        // PUŁAPKA: jak przy BigDecimal — a.add(b) bez przypisania nic nie zmienia.
        a.add(b);                                                     // wynik wyrzucony!
        show("a po a.add(b) bez przypisania", a);
        // WYNIK: a po a.add(b) bez przypisania → 100000000000000000000
    }

    // =================================================================================================
    // 5. mod, remainder I modPow
    // =================================================================================================

    /**
     * 5. {@code remainder} działa jak {@code %} (znak jak u dzielnej, może być ujemny).
     * {@code mod} zwraca zawsze resztę NIEUJEMNĄ — tak jak w matematyce.
     * {@code modPow(e, m)} liczy (a^e) mod m bardzo szybko, bez tworzenia gigantycznej liczby a^e —
     * na tym opiera się m.in. szyfrowanie RSA.
     */
    static void modAndModPow() {
        section("5. mod, remainder i modPow");

        BigInteger minusSeven = BigInteger.valueOf(-7);
        BigInteger three = BigInteger.valueOf(3);
        show("-7 remainder 3", minusSeven.remainder(three));
        show("-7 mod 3", minusSeven.mod(three));
        show("-7 % 3 (int)", -7 % 3);
        // WYNIK: -7 remainder 3 → -1
        // WYNIK: -7 mod 3 → 2    ← zawsze w przedziale 0..m−1
        // WYNIK: -7 % 3 (int) → -1    ← remainder zachowuje się jak operator %

        expectThrows("7 mod -3", () -> BigInteger.valueOf(7).mod(BigInteger.valueOf(-3)));
        // WYNIK: ✔ 7 mod -3 → rzucono ArithmeticException: BigInteger: modulus not positive
        // (modulus = moduł, liczba, przez którą dzielimy — musi być dodatnia)

        BigInteger thousand = BigInteger.valueOf(1000);
        show("2^10 mod 1000", BigInteger.TWO.modPow(BigInteger.TEN, thousand));
        // WYNIK: 2^10 mod 1000 → 24    ← 1024 → trzy ostatnie cyfry to 024

        // Trzy ostatnie cyfry liczby 7^2026 — ta liczba ma ponad 1700 cyfr, ale modPow jej nie tworzy:
        show("7^2026 mod 1000", BigInteger.valueOf(7).modPow(BigInteger.valueOf(2026), thousand));
        show("to samo przez pow(...).mod(...)", BigInteger.valueOf(7).pow(2026).mod(thousand));
        // WYNIK: 7^2026 mod 1000 → 649
        // WYNIK: to samo przez pow(...).mod(...) → 649
        // Wynik ten sam, ale pow najpierw buduje liczbę z 1713 cyframi. Przy wykładnikach kryptograficznych
        // (liczby z setkami cyfr) pow zabrakłoby pamięci, a modPow liczy w ułamku sekundy.
    }

    // =================================================================================================
    // 6. gcd — NAJWIĘKSZY WSPÓLNY DZIELNIK I SKRACANIE UŁAMKÓW
    // =================================================================================================

    /** 6. {@code gcd} = największy wspólny dzielnik (NWD). Przydaje się do skracania ułamków i liczenia NWW. */
    static void gcdAndFractions() {
        section("6. gcd — największy wspólny dzielnik (NWD)");

        BigInteger a = BigInteger.valueOf(84);
        BigInteger b = BigInteger.valueOf(36);
        BigInteger gcd = a.gcd(b);
        show("gcd(84, 36)", gcd);
        // WYNIK: gcd(84, 36) → 12

        show("84/36 po skróceniu", a.divide(gcd) + "/" + b.divide(gcd));
        // WYNIK: 84/36 po skróceniu → 7/3

        BigInteger lcm = a.multiply(b).divide(gcd);                   // lcm = najmniejsza wspólna wielokrotność
        show("lcm(84, 36) = 84 × 36 / gcd", lcm);
        // WYNIK: lcm(84, 36) = 84 × 36 / gcd → 252

        show("gcd(0, 5)", BigInteger.ZERO.gcd(BigInteger.valueOf(5)));
        show("gcd(-84, 36)", a.negate().gcd(b));
        // WYNIK: gcd(0, 5) → 5
        // WYNIK: gcd(-84, 36) → 12    ← wynik zawsze nieujemny
    }

    // =================================================================================================
    // 7. LICZBY PIERWSZE: isProbablePrime, nextProbablePrime
    // =================================================================================================

    /**
     * 7. {@code isProbablePrime(certainty)} (certainty = pewność):
     * <ul>
     *   <li>false — liczba NA PEWNO jest złożona,</li>
     *   <li>true — liczba jest pierwsza z prawdopodobieństwem co najmniej 1 − 1/2^certainty
     *       (dla certainty = 50 szansa pomyłki jest mniejsza niż trafienie w totka kilka razy z rzędu).</li>
     * </ul>
     */
    static void primes() {
        section("7. Liczby pierwsze: isProbablePrime i nextProbablePrime");

        for (long n : new long[]{97, 91, 2_147_483_647L}) {
            show(n + " pierwsza?", BigInteger.valueOf(n).isProbablePrime(50));
        }
        // WYNIK: 97 pierwsza? → true
        // WYNIK: 91 pierwsza? → false    ← 91 = 7 × 13 — często mylona z pierwszą
        // WYNIK: 2147483647 pierwsza? → true    ← Integer.MAX_VALUE to liczba pierwsza (2^31 − 1)

        // Liczby Mersenne'a 2^p − 1. Dla p = 67 długo uważano, że to liczba pierwsza — w 1903 roku
        // Frank Cole pokazał jej rozkład na czynniki. Komputer sprawdza to w mgnieniu oka:
        BigInteger m61 = BigInteger.TWO.pow(61).subtract(BigInteger.ONE);
        BigInteger m67 = BigInteger.TWO.pow(67).subtract(BigInteger.ONE);
        show("2^61 − 1 = " + m61 + " pierwsza?", m61.isProbablePrime(50));
        show("2^67 − 1 = " + m67 + " pierwsza?", m67.isProbablePrime(50));
        show("193707721 × 761838257287", BigInteger.valueOf(193_707_721L).multiply(BigInteger.valueOf(761_838_257_287L)));
        // WYNIK: 2^61 − 1 = 2305843009213693951 pierwsza? → true
        // WYNIK: 2^67 − 1 = 147573952589676412927 pierwsza? → false
        // WYNIK: 193707721 × 761838257287 → 147573952589676412927    ← rozkład znaleziony przez Cole'a

        show("nextProbablePrime(100)", BigInteger.valueOf(100).nextProbablePrime());
        // WYNIK: nextProbablePrime(100) → 101

        // DOBRA PRAKTYKA: certainty 50–100 wystarcza w praktyce. Większa pewność = wolniejsze sprawdzanie.
    }

    // =================================================================================================
    // 8. KONWERSJE: longValue kontra longValueExact, toString(radix)
    // =================================================================================================

    /**
     * 8. Zamiana na typy proste:
     * <ul>
     *   <li>{@code longValue()} / {@code intValue()} — obcina po cichu starsze bity (jak rzutowanie). Niebezpieczne!</li>
     *   <li>{@code longValueExact()} / {@code intValueExact()} — rzucają ArithmeticException, gdy liczba się nie mieści.</li>
     * </ul>
     */
    static void conversions() {
        section("8. Konwersje: longValue kontra longValueExact, toString(radix)");

        BigInteger fits = BigInteger.valueOf(123_456_789L);
        show("123456789.longValueExact()", fits.longValueExact());
        // WYNIK: 123456789.longValueExact() → 123456789

        BigInteger tooBig = BigInteger.TWO.pow(64).add(BigInteger.valueOf(5));   // 2^64 + 5
        show("(2^64 + 5).longValue()", tooBig.longValue());
        // WYNIK: (2^64 + 5).longValue() → 5    ← obcięte starsze bity: wynik zupełnie inny, bez ostrzeżenia!

        expectThrows("(2^64 + 5).longValueExact()", () -> tooBig.longValueExact());
        // WYNIK: ✔ (2^64 + 5).longValueExact() → rzucono ArithmeticException: BigInteger out of long range
        expectThrows("factorial(13).intValueExact()", () -> factorial(13).intValueExact());
        // WYNIK: ✔ factorial(13).intValueExact() → rzucono ArithmeticException: BigInteger out of int range

        // toString(radix) — zapis w innym systemie liczbowym (radix = podstawa):
        BigInteger x = BigInteger.valueOf(255);
        show("255 dwójkowo", x.toString(2));
        show("255 szesnastkowo", x.toString(16));
        show("2^100 szesnastkowo", BigInteger.TWO.pow(100).toString(16));
        // WYNIK: 255 dwójkowo → 11111111
        // WYNIK: 255 szesnastkowo → ff
        // WYNIK: 2^100 szesnastkowo → 10000000000000000000000000

        // DOBRA PRAKTYKA: wracając z BigInteger do long/int, używaj wersji *Exact. Ciche obcięcie to błąd,
        //   którego możesz szukać tygodniami.
    }

    // =================================================================================================
    // 9. compareTo, equals, signum I bitLength
    // =================================================================================================

    /**
     * 9. BigInteger nie ma skali, więc equals działa „po ludzku” (w przeciwieństwie do BigDecimal).
     * Do porównań „większy/mniejszy” służy compareTo. {@code bitLength()} mówi, ile bitów potrzeba na zapis liczby
     * (bez bitu znaku) — liczba mieści się w long, gdy bitLength ≤ 63.
     */
    static void compareAndBitLength() {
        section("9. compareTo, equals, signum i bitLength");

        BigInteger a = new BigInteger("1000");
        BigInteger b = BigInteger.valueOf(1000);
        show("\"1000\" equals valueOf(1000)", a.equals(b));
        show("a == b", a == b);
        show("25! > 2^80 ?", factorial(25).compareTo(BigInteger.TWO.pow(80)) > 0);
        // WYNIK: "1000" equals valueOf(1000) → true    ← brak skali, equals jest bezpieczne
        // WYNIK: a == b → false    ← dwa różne obiekty; nigdy == dla obiektów
        // WYNIK: 25! > 2^80 ? → true

        show("signum(-5), signum(0), signum(5)", BigInteger.valueOf(-5).signum() + ", "
                + BigInteger.ZERO.signum() + ", " + BigInteger.valueOf(5).signum());
        // WYNIK: signum(-5), signum(0), signum(5) → -1, 0, 1

        BigInteger longMax = BigInteger.valueOf(Long.MAX_VALUE);
        BigInteger longMin = BigInteger.valueOf(Long.MIN_VALUE);
        show("bitLength: 255 / 256", BigInteger.valueOf(255).bitLength() + " / " + BigInteger.valueOf(256).bitLength());
        show("bitLength: Long.MAX_VALUE", longMax.bitLength());
        show("bitLength: Long.MAX_VALUE + 1", longMax.add(BigInteger.ONE).bitLength());
        show("bitLength: Long.MIN_VALUE", longMin.bitLength());
        show("bitLength: 50!", factorial(50).bitLength());
        // WYNIK: bitLength: 255 / 256 → 8 / 9    ← 255 = 11111111 (8 bitów), 256 = 100000000 (9 bitów)
        // WYNIK: bitLength: Long.MAX_VALUE → 63
        // WYNIK: bitLength: Long.MAX_VALUE + 1 → 64    ← już się nie zmieści w long
        // WYNIK: bitLength: Long.MIN_VALUE → 63    ← liczby ujemne mają o jedną wartość więcej
        // WYNIK: bitLength: 50! → 215

        // PUŁAPKA: BigInteger jest wolniejszy od long (obiekty, tablice, brak wsparcia procesora).
        // DOBRA PRAKTYKA: long, dopóki wynik na pewno się mieści (z Math.*Exact jako strażnikiem);
        //   BigInteger — gdy liczby naprawdę mogą być ogromne (silnie, kryptografia, bardzo duże liczniki).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   TWORZENIE:   BigInteger.valueOf(long), new BigInteger("..."), new BigInteger("ff", 16); ZERO ONE TWO(9+) TEN
     *   DZIAŁANIA:   add, subtract, multiply, divide (całkowite!), divideAndRemainder, pow(int) — przypisz wynik
     *   RESZTA:      remainder (jak %, może być ujemna) vs mod (zawsze 0..m−1, m > 0)
     *   modPow:      (a^e) mod m bez budowania a^e — kryptografia, ostatnie cyfry wielkich potęg
     *   gcd:         NWD; NWW = a × b / gcd; skracanie ułamków
     *   PIERWSZE:    isProbablePrime(50): false = na pewno złożona; nextProbablePrime()
     *   KONWERSJE:   longValueExact / intValueExact (wyjątek) zamiast longValue / intValue (ciche obcięcie)
     *                toString(2), toString(16) — inne systemy liczbowe
     *   PORÓWNANIE:  compareTo dla < >; equals OK (brak skali); nigdy ==
     *   bitLength:   ile bitów; mieści się w long ⇔ bitLength ≤ 63
     *   long przepełnia się PO CICHU (21! < 0) — strażnik: Math.multiplyExact / addExact
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego 21! policzone na long daje liczbę ujemną i dlaczego Java nie zgłasza błędu?
     *   2. Co wypisze:  System.out.println(BigInteger.valueOf(-7).mod(BigInteger.valueOf(3))
     *                          + " " + BigInteger.valueOf(-7).remainder(BigInteger.valueOf(3)));  ?
     *   3. ZNAJDŹ BŁĄD:
     *          BigInteger result = BigInteger.ONE;
     *          for (int i = 2; i <= n; i++) { result.multiply(BigInteger.valueOf(i)); }
     *          return result;   // zawsze 1
     *   4. ZNAJDŹ BŁĄD:  long id = hugeNumber.longValue();   // hugeNumber może mieć 30 cyfr
     *   5. Co znaczy wynik true, a co false z isProbablePrime(50)?
     *   6. Co wypisze:  System.out.println(BigInteger.valueOf(1024).bitLength());  ?
     *   7. Po co modPow, skoro można napisać a.pow(e).mod(m)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Uruchamia ćwiczenia: najpierw TWOJE rozwiązania (✘ dopóki nie uzupełnisz), potem wzorcowe (✔). */
    static void exercises() {
        BigInteger longMax = BigInteger.valueOf(Long.MAX_VALUE);
        BigInteger longMin = BigInteger.valueOf(Long.MIN_VALUE);

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: 0!", BigInteger.ONE, () -> exercise1(0));
        Check.equal("ćw. 1b: 25!", new BigInteger("15511210043330985984000000"), () -> exercise1(25));
        Check.equal("ćw. 2a: Long.MAX_VALUE mieści się", true, () -> exercise2(longMax));
        Check.equal("ćw. 2b: Long.MAX_VALUE + 1 nie mieści się", false, () -> exercise2(longMax.add(BigInteger.ONE)));
        Check.equal("ćw. 2c: Long.MIN_VALUE mieści się", true, () -> exercise2(longMin));
        Check.equal("ćw. 3a: suma cyfr 2^100", 115, () -> exercise3(BigInteger.TWO.pow(100)));
        Check.equal("ćw. 3b: suma cyfr 100!", 648, () -> exercise3(factorial(100)));
        Check.equal("ćw. 4a: fib(90)", new BigInteger("2880067194370816120"), () -> exercise4(90));
        Check.equal("ćw. 4b: fib(100)", new BigInteger("354224848179261915075"), () -> exercise4(100));
        Check.equal("ćw. 5a: zera na końcu 25!", 6, () -> exercise5(25));
        Check.equal("ćw. 5b: zera na końcu 100!", 24, () -> exercise5(100));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", BigInteger.ONE, () -> solution1(0));
        Check.equal("ćw. 1b (wzorzec)", new BigInteger("15511210043330985984000000"), () -> solution1(25));
        Check.equal("ćw. 2a (wzorzec)", true, () -> solution2(longMax));
        Check.equal("ćw. 2b (wzorzec)", false, () -> solution2(longMax.add(BigInteger.ONE)));
        Check.equal("ćw. 2c (wzorzec)", true, () -> solution2(longMin));
        Check.equal("ćw. 3a (wzorzec)", 115, () -> solution3(BigInteger.TWO.pow(100)));
        Check.equal("ćw. 3b (wzorzec)", 648, () -> solution3(factorial(100)));
        Check.equal("ćw. 4a (wzorzec)", new BigInteger("2880067194370816120"), () -> solution4(90));
        Check.equal("ćw. 4b (wzorzec)", new BigInteger("354224848179261915075"), () -> solution4(100));
        Check.equal("ćw. 5a (wzorzec)", 6, () -> solution5(25));
        Check.equal("ćw. 5b (wzorzec)", 24, () -> solution5(100));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 11 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć n! jako BigInteger (0! = 1).
     * Podpowiedź: start od BigInteger.ONE, pętla od 2 do n, {@code result = result.multiply(BigInteger.valueOf(i))}.
     */
    static BigInteger exercise1(int n) {
        // TODO: twoje rozwiązanie
        return BigInteger.ZERO;
    }

    /**
     * ĆWICZENIE 2 (łatwe): czy liczba zmieści się w long?
     * Podpowiedź: bitLength() ≤ 63. Albo: spróbuj longValueExact() w try/catch (ArithmeticException).
     * Uwaga na Long.MIN_VALUE — też musi dać true.
     */
    static boolean exercise2(BigInteger value) {
        // TODO: twoje rozwiązanie
        return false;
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć sumę cyfr liczby (nieujemnej), np. 2^100 → 115.
     * Podpowiedź: sposób 1 — toString() i pętla po znakach ({@code c - '0'} zamienia znak cyfry na liczbę).
     * Sposób 2 — pętla: {@code divideAndRemainder(BigInteger.TEN)}: reszta to ostatnia cyfra, iloraz to reszta liczby.
     */
    static int exercise3(BigInteger value) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (średnie): PRZEPISZ Fibonacciego z long na BigInteger (fib(0) = 0, fib(1) = 1).
     * <pre>{@code
     * static long fib(int n) {
     *     long a = 0, b = 1;
     *     for (int i = 0; i < n; i++) {
     *         long next = a + b;
     *         a = b;
     *         b = next;
     *     }
     *     return a;           // dla n = 93 i więcej: przepełnienie!
     * }
     * }</pre>
     * Podpowiedź: zmienne a, b, next typu BigInteger; {@code a + b} → {@code a.add(b)}.
     */
    static BigInteger exercise4(int n) {
        // TODO: twoje rozwiązanie
        return BigInteger.ZERO;
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): policz, iloma zerami kończy się n! (np. 25! = 15511210043330985984000000 → 6).
     * Podpowiedź: policz silnię jako BigInteger, potem w pętli: dopóki {@code x.mod(BigInteger.TEN)} jest zerem —
     * licznik++ i {@code x = x.divide(BigInteger.TEN)}. Sprawdzenie matematyczne: n/5 + n/25 + n/125 + ...
     */
    static int exercise5(int n) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static BigInteger solution1(int n) {
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }

    static boolean solution2(BigInteger value) {
        return value.bitLength() <= 63;                               // bitLength nie liczy bitu znaku
    }

    static int solution3(BigInteger value) {
        int sum = 0;
        for (char c : value.toString().toCharArray()) {
            sum += c - '0';                                           // '7' - '0' = 7
        }
        return sum;
    }

    static BigInteger solution4(int n) {
        BigInteger a = BigInteger.ZERO;
        BigInteger b = BigInteger.ONE;
        for (int i = 0; i < n; i++) {
            BigInteger next = a.add(b);
            a = b;
            b = next;
        }
        return a;
    }

    static int solution5(int n) {
        BigInteger x = solution1(n);
        int zeros = 0;
        while (x.signum() > 0 && x.mod(BigInteger.TEN).signum() == 0) {
            zeros++;
            x = x.divide(BigInteger.TEN);
        }
        return zeros;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 21! ≈ 5.1 × 10^19, a long mieści najwyżej ≈ 9.2 × 10^18. Mnożenie na long „zawija się” modulo 2^64
     *      i najstarszy bit (bit znaku) może się ustawić → liczba ujemna. Java celowo nie sprawdza przepełnienia
     *      (szybkość); sprawdzanie daje Math.multiplyExact albo BigInteger.
     *   2. „2 -1” — mod zwraca resztę nieujemną, remainder ma znak dzielnej (jak operator %).
     *   3. Wynik multiply nie jest przypisany — BigInteger jest niezmienny. Poprawnie: result = result.multiply(...).
     *   4. longValue() po cichu obcina starsze bity — id będzie zupełnie inną liczbą. Poprawnie: longValueExact()
     *      (wyjątek, gdy się nie mieści) albo zostań przy BigInteger.
     *   5. false — liczba na pewno jest złożona. true — jest pierwsza z prawdopodobieństwem ≥ 1 − 1/2^50
     *      (w praktyce pewność).
     *   6. „11” — 1024 = 2^10 = 10000000000 w zapisie dwójkowym (1 i dziesięć zer = 11 bitów).
     *   7. pow buduje całą liczbę a^e (przy dużych e — tysiące cyfr albo brak pamięci), a modPow liczy resztę
     *      krok po kroku, trzymając tylko małe liczby (mniejsze od m). Wynik ten sam, koszt nieporównywalnie mniejszy.
     */
    // </editor-fold>
}
