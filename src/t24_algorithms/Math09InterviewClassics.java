package t24_algorithms;

import helpers.Check;

import java.util.ArrayList;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Klasyki pytań rekrutacyjnych — silnia, Fibonacci, liczby pierwsze i inne
 *        (classic interview questions = klasyczne pytania rekrutacyjne)
 *
 * W SKRÓCIE:
 *   Katalog zadań, które od lat pojawiają się na rozmowach kwalifikacyjnych z Javy: silnia, Fibonacci,
 *   test pierwszości, liczby doskonałe/Armstronga, palindrom, potęga dwójki, rok przestępny, pierwiastek
 *   całkowity, FizzBuzz. Każde to osobna, malutka metoda + demonstracja.
 *
 * ANALOGIA: to jak podstawowe chwyty w sztukach walki — pojedynczo banalne, ale rekruter pyta o nie,
 *   bo sprawdzają, czy rozumiesz PRZEPEŁNIENIE, ZŁOŻONOŚĆ i PRZYPADKI BRZEGOWE (0, ujemne, bardzo duże).
 *
 * JAK TO DZIAŁA:
 *   Każda sekcja: pytanie w stylu rekrutacyjnym, rozwiązanie (czasem dwa — naiwne/szybsze), pułapka.
 *   PYTANIE REKRUTACYJNE oznacza: realnie pada na rozmowach, warto umieć bez IDE.
 *
 * SŁÓWKA:
 *   factorial = silnia; overflow = przepełnienie; trailing zeros = zera na końcu; memoization = memoizacja
 *   (zapamiętywanie wyników pośrednich); perfect/Armstrong number = liczba doskonała/Armstronga;
 *   palindrome = palindrom; leap year = rok przestępny; swap = zamiana miejscami.
 *
 * ZOBACZ TEŻ: t15_numbers/Numbers05IntegerTricks (przepełnienie, bity), t24_algorithms/Math08BigNumbers
 *             (silnia bez limitu — BigInteger), t24_algorithms/Math10PuzzlesEuler (kolejne łamigłówki).
 * </pre>
 */
public class Math09InterviewClassics {

    /** Licznik wywołań naiwnej rekurencji Fibonacciego — WYŁĄCZNIE do demonstracji (efekt uboczny!). */
    private static long naiveCallCount;
    /** Licznik wywołań wersji z memoizacją — jw., tylko do pomiaru w demie. */
    private static long memoCallCount;

    public static void main(String[] args) {
        title("Math09 — klasyki pytań rekrutacyjnych");

        silnia();                  // silnia = factorial
        fibonacciTrzyWersje();      // fibonacci trzy wersje = Fibonacci three versions
        testPierwszosci();           // test pierwszosci = primality test
        liczbySpecjalne();            // liczby specjalne = special numbers (perfect, Armstrong)
        odwracanieISumaCyfrIPalindrom(); // odwracanie, suma cyfr i palindrom
        potegaDwojkiIBity();              // potega dwojki i bity = power of two and bits
        rokPrzestepnyISqrtISwap();         // rok przestepny i sqrt i swap = leap year, sqrt, swap
        fizzBuzz();                         // fizzbuzz
        exercises();                         // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. SILNIA (FACTORIAL) — ITERACYJNA, REKURENCYJNA, PRZEPEŁNIENIE, ZERA NA KOŃCU
    // =================================================================================================

    /**
     * 1. PYTANIE REKRUTACYJNE: silnia iteracyjnie i rekurencyjnie — i gdzie przepełni się int? Już przy 13!
     * (6 227 020 800 > Integer.MAX_VALUE); long dopiero przy 21! (t24_algorithms/Math08BigNumbers, sekcja 1).
     */
    static void silnia() {
        section("1. Silnia (factorial) — iteracyjna, rekurencyjna, przepełnienie");

        show("factorialIterative(10)", factorialIterative(10));
        show("factorialRecursive(10)", factorialRecursive(10));
        // WYNIK: factorialIterative(10) → 3628800
        // WYNIK: factorialRecursive(10) → 3628800

        show("factorialIterative(13) jako int (ŹLE)", factorialIterative(13));
        show("factorialLong(13) jako long (DOBRZE)", factorialLong(13));
        // WYNIK: factorialIterative(13) jako int (ŹLE) → 1932053504
        // WYNIK: factorialLong(13) jako long (DOBRZE) → 6227020800

        show("zera na końcu 20!", trailingZerosInFactorial(20));
        show("zera na końcu 100!", trailingZerosInFactorial(100));
        // WYNIK: zera na końcu 20! → 4
        // WYNIK: zera na końcu 100! → 24

        // PUŁAPKA: int factorialIterative(13) cicho przepełnia się i zwraca błędną, ale WIARYGODNIE wyglądającą liczbę.
        // Mechanizm zer: 10 = 2×5 w rozkładzie n! dodaje zero; dwójek jest więcej niż piątek, więc liczymy TYLKO piątki.
    }

    // =================================================================================================
    // 2. FIBONACCI: NAIWNA REKURENCJA KONTRA ITERACJA KONTRA MEMOIZACJA
    // =================================================================================================

    /**
     * 2. PYTANIE REKRUTACYJNE: n-ty Fibonacci — a czemu Twoje rozwiązanie jest wolne? Naiwna rekurencja liczy
     * te same podproblemy wielokrotnie (O(2^n)). Iteracja: O(n), O(1) pamięci. Memoizacja (zapamiętywanie
     * już policzonych wyników) sprowadza rekurencję do O(n) wywołań — każdy podproblem liczony raz.
     */
    static void fibonacciTrzyWersje() {
        section("2. Fibonacci: naiwna rekurencja kontra iteracja kontra memoizacja");

        naiveCallCount = 0;
        long naiveResult = fibNaive(30);
        show("fibNaive(30)", naiveResult);
        show("liczba wywołań (naiwnie)", naiveCallCount);
        // WYNIK: fibNaive(30) → 832040
        // WYNIK: liczba wywołań (naiwnie) → 2692537

        show("fibIterative(30)", fibIterative(30));
        // WYNIK: fibIterative(30) → 832040

        long memoResult = fibMemoized(30);
        show("fibMemoized(30)", memoResult);
        show("liczba wywołań (z memoizacją)", memoCallCount);
        // WYNIK: fibMemoized(30) → 832040
        // WYNIK: liczba wywołań (z memoizacją) → 59

        // DOBRA PRAKTYKA: 2,7 mln wywołań naiwnie kontra 59 z memoizacją — ten sam wynik, inny koszt o rzędy wielkości.
    }

    // =================================================================================================
    // 3. TEST PIERWSZOŚCI (isPrime)
    // =================================================================================================

    /**
     * 3. PYTANIE REKRUTACYJNE: sprawdź, czy liczba jest pierwsza. Wystarczy sprawdzić dzielniki do √n
     * (jeśli n = a·b i a ≤ b, to a ≤ √n) — sprawdzanie aż do n byłoby O(n), tu mamy O(√n).
     */
    static void testPierwszosci() {
        section("3. Test pierwszości (isPrime)");

        for (int n : new int[]{1, 2, 91, 97, 100}) {
            show("isPrime(" + n + ")", isPrime(n));
        }
        // WYNIK: isPrime(1) → false
        // WYNIK: isPrime(2) → true
        // WYNIK: isPrime(91) → false
        // WYNIK: isPrime(97) → true
        // WYNIK: isPrime(100) → false

        // PUŁAPKA: 1 NIE jest pierwsza (ma JEDEN dzielnik, pierwsza ma DOKŁADNIE dwa); 91=7×13 łatwo pomylić z pierwszą.
    }

    // =================================================================================================
    // 4. LICZBY SPECJALNE: DOSKONAŁE (6, 28, 496) I ARMSTRONGA (153, 370, 371, 407)
    // =================================================================================================

    /**
     * 4. PYTANIE REKRUTACYJNE (dwa klasyki naraz): liczba doskonała = suma jej dzielników właściwych
     * (6 = 1+2+3). Liczba Armstronga = suma cyfr podniesionych do potęgi równej LICZBIE CYFR daje z powrotem
     * tę liczbę (153 → 1³+5³+3³ = 153; działa dla dowolnej liczby cyfr — 9474 też jest Armstronga).
     * Obie szukane brute force'em, sprawdzając dzielniki/cyfry — bez magii.
     */
    static void liczbySpecjalne() {
        section("4. Liczby specjalne: doskonałe (6, 28, 496) i Armstronga (153, 370, 371, 407)");

        List<Integer> perfectNumbers = new ArrayList<>();
        for (int n = 2; n <= 500; n++) {
            if (isPerfect(n)) {
                perfectNumbers.add(n);
            }
        }
        show("liczby doskonałe ≤ 500", perfectNumbers);
        // WYNIK: liczby doskonałe ≤ 500 → [6, 28, 496]

        List<Integer> armstrong3Digit = new ArrayList<>();
        for (int n = 100; n <= 999; n++) {
            if (isArmstrong(n)) {
                armstrong3Digit.add(n);
            }
        }
        show("liczby Armstronga, 3-cyfrowe", armstrong3Digit);
        show("isArmstrong(9474) [4-cyfrowa]", isArmstrong(9474));
        // WYNIK: liczby Armstronga, 3-cyfrowe → [153, 370, 371, 407]
        // WYNIK: isArmstrong(9474) [4-cyfrowa] → true

        // DOBRA PRAKTYKA: obie funkcje liczą dzielniki/cyfry same — żadnych list wartości wpisanych na sztywno.
    }

    // =================================================================================================
    // 5. ODWRACANIE LICZBY, SUMA CYFR I PALINDROM — BEZ String
    // =================================================================================================

    /**
     * 5. PYTANIE REKRUTACYJNE: odwróć liczbę / zsumuj cyfry BEZ zamiany na String — rozkładamy liczbę
     * operatorami {@code % 10} (ostatnia cyfra) i {@code / 10} (reszta), jak licząc w pamięci. Palindrom
     * (12321 czytane wspak daje to samo) sprawdzamy, porównując n z jego odwróceniem — bez osobnego algorytmu.
     */
    static void odwracanieISumaCyfrIPalindrom() {
        section("5. Odwracanie liczby, suma cyfr i palindrom — bez String");

        show("digitSum(12345)", digitSum(12345));
        show("digitSum(-908)", digitSum(-908));
        // WYNIK: digitSum(12345) → 15
        // WYNIK: digitSum(-908) → 17

        show("reverseNumber(12345)", reverseNumber(12345));
        show("reverseNumber(-120)", reverseNumber(-120));
        // WYNIK: reverseNumber(12345) → 54321
        // WYNIK: reverseNumber(-120) → -21

        show("reverseNumber(Integer.MAX_VALUE) — PUŁAPKA przepełnienia", reverseNumber(Integer.MAX_VALUE));
        // WYNIK: reverseNumber(Integer.MAX_VALUE) — PUŁAPKA przepełnienia → -1126087180

        // PUŁAPKA: odwrócone cyfry mogą przekroczyć int, choć oryginał się mieścił — 2147483647 odwrócone to 7463847412.

        for (int n : new int[]{12321, 12345, 7, -121}) {
            show("isPalindromeNumber(" + n + ")", isPalindromeNumber(n));
        }
        // WYNIK: isPalindromeNumber(12321) → true
        // WYNIK: isPalindromeNumber(12345) → false
        // WYNIK: isPalindromeNumber(7) → true
        // WYNIK: isPalindromeNumber(-121) → false

        // PUŁAPKA: -121 nie jest palindromem — znak minus psuje symetrię ("-121" wspak to "121-"), więc ujemne odrzucamy od razu.
    }

    // =================================================================================================
    // 6. POTĘGA DWÓJKI I BITY: Integer.bitCount KONTRA RĘCZNE LICZENIE
    // =================================================================================================

    /**
     * 6. PYTANIE REKRUTACYJNE: czy liczba jest potęgą dwójki, bez pętli? To {@code n > 0 && (n & (n - 1)) == 0}
     * (szczegóły: t15_numbers/Numbers05IntegerTricks, sekcja 8). Druga klasyka: policz jedynki w zapisie
     * dwójkowym ręcznie i porównaj z gotowym Integer.bitCount.
     */
    static void potegaDwojkiIBity() {
        section("6. Potęga dwójki i bity: Integer.bitCount kontra ręczne liczenie");

        for (int n : new int[]{64, 96}) {
            show("czy " + n + " to potęga dwójki?", n > 0 && (n & (n - 1)) == 0);
        }
        // WYNIK: czy 64 to potęga dwójki? → true
        // WYNIK: czy 96 to potęga dwójki? → false

        for (int n : new int[]{0, 1, 255, -1}) {
            int manual = manualBitCount(n);
            int builtin = Integer.bitCount(n);
            show("manualBitCount(" + n + ") == Integer.bitCount?", manual + " == " + builtin + " -> " + (manual == builtin));
        }
        // WYNIK: manualBitCount(0) == Integer.bitCount? → 0 == 0 -> true
        // WYNIK: manualBitCount(1) == Integer.bitCount? → 1 == 1 -> true
        // WYNIK: manualBitCount(255) == Integer.bitCount? → 8 == 8 -> true
        // WYNIK: manualBitCount(-1) == Integer.bitCount? → 32 == 32 -> true

        // PUŁAPKA: manualBitCount używa {@code >>>} (bez znaku); {@code >>} na liczbie ujemnej wsuwałoby jedynki w nieskończoność.
    }

    // =================================================================================================
    // 7. ROK PRZESTĘPNY, PIERWIASTEK CAŁKOWITY (BINARY SEARCH), SWAP BEZ ZMIENNEJ POMOCNICZEJ
    // =================================================================================================

    /**
     * 7. PYTANIE REKRUTACYJNE (trzy naraz): rok przestępny — podzielny przez 4, ale NIE przez 100, chyba że
     * przez 400 też. Pierwiastek całkowity (floor) wyszukiwaniem binarnym — O(log n). Zamiana dwóch zmiennych
     * bez trzeciej — ciekawostka, nie styl do produkcji.
     */
    static void rokPrzestepnyISqrtISwap() {
        section("7. Rok przestępny, pierwiastek całkowity, swap bez zmiennej pomocniczej");

        for (int year : new int[]{2000, 1900, 2024, 2023}) {
            show("isLeapYear(" + year + ")", isLeapYear(year));
        }
        // WYNIK: isLeapYear(2000) → true
        // WYNIK: isLeapYear(1900) → false
        // WYNIK: isLeapYear(2024) → true
        // WYNIK: isLeapYear(2023) → false

        for (int n : new int[]{10, 16, 99, 100}) {
            show("integerSqrt(" + n + ")", integerSqrt(n));
        }
        // WYNIK: integerSqrt(10) → 3
        // WYNIK: integerSqrt(16) → 4
        // WYNIK: integerSqrt(99) → 9
        // WYNIK: integerSqrt(100) → 10

        int[] swapped = swapWithoutTemp(3, 9);
        show("swapWithoutTemp(3, 9)", swapped);
        // WYNIK: swapWithoutTemp(3, 9) → [9, 3]

        // PUŁAPKA: a+b PRZEPEŁNIA SIĘ dla dużych int; wersja XOR (a^=b; b^=a; a^=b;) zastosowana WPROST do
        //   tego samego elementu tablicy (arr[i] z arr[i]) wyzeruje go (x^x == 0) — oba "triki" mają pułapki.
        // DOBRA PRAKTYKA: zwykła trzecia zmienna jest czytelniejsza i bezpieczna — to ciekawostka, nie styl produkcyjny.
    }

    // =================================================================================================
    // 8. FizzBuzz — WARIANTY
    // =================================================================================================

    /**
     * 8. PYTANIE REKRUTACYJNE (najsłynniejsze): dla 1..n wypisz "Fizz" (wielokrotność 3), "Buzz" (5),
     * "FizzBuzz" (oba), inaczej liczbę. Dwa warianty: klasyczny if-else-if oraz "sklejanie" napisu
     * (łatwiej rozszerzyć o kolejne reguły, np. "Bazz" dla 7).
     */
    static void fizzBuzz() {
        section("8. FizzBuzz — warianty");

        show("fizzBuzzClassic(15)", fizzBuzzClassic(15));
        show("fizzBuzzConcat(15)", fizzBuzzConcat(15));
        // WYNIK: fizzBuzzClassic(15) → [1, 2, Fizz, 4, Buzz, Fizz, 7, 8, Fizz, Buzz, 11, Fizz, 13, 14, FizzBuzz]
        // WYNIK: fizzBuzzConcat(15) → [1, 2, Fizz, 4, Buzz, Fizz, 7, 8, Fizz, Buzz, 11, Fizz, 13, 14, FizzBuzz]

        Check.isTrue("obie wersje dają ten sam wynik", fizzBuzzClassic(50).equals(fizzBuzzConcat(50)));
        // WYNIK: ✔ OK    obie wersje dają ten sam wynik

        // DOBRA PRAKTYKA: wersja "sklejana" lepiej skaluje się przy kolejnych regułach — jeden warunek, nie nowa gałąź if-else.
    }

    // =================================================================================================
    // FUNKCJE POMOCNICZE
    // =================================================================================================

    static int factorialIterative(int n) {
        int result = 1;
        for (int i = 2; i <= n; i++) { result *= i; }
        return result;
    }

    static int factorialRecursive(int n) {
        if (n <= 1) { return 1; }
        return n * factorialRecursive(n - 1);
    }

    static long factorialLong(int n) {
        long result = 1;
        for (int i = 2; i <= n; i++) { result *= i; }
        return result;
    }

    /** trailingZerosInFactorial = liczba zer na końcu n!, licząc czynniki 5 (n/5 + n/25 + ...). */
    static int trailingZerosInFactorial(int n) {
        int count = 0;
        for (long power = 5; n / power >= 1; power *= 5) {
            count += n / power;
        }
        return count;
    }

    static long fibNaive(int n) {
        naiveCallCount++;
        if (n <= 1) { return n; }
        return fibNaive(n - 1) + fibNaive(n - 2);
    }

    static long fibIterative(int n) {
        if (n <= 1) { return n; }
        long previous = 0;
        long current = 1;
        for (int i = 2; i <= n; i++) {
            long next = previous + current;
            previous = current;
            current = next;
        }
        return current;
    }

    /** fibMemoized = Fibonacci z memoizacją (zapamiętywaniem policzonych wyników) — O(n) wywołań. */
    static long fibMemoized(int n) {
        memoCallCount = 0;
        long[] memo = new long[n + 1];
        java.util.Arrays.fill(memo, -1);                              // -1 = "jeszcze nie policzone"
        return fibMemoHelper(n, memo);
    }

    private static long fibMemoHelper(int n, long[] memo) {
        memoCallCount++;
        if (n <= 1) { return n; }
        if (memo[n] != -1) { return memo[n]; }
        long result = fibMemoHelper(n - 1, memo) + fibMemoHelper(n - 2, memo);
        memo[n] = result;
        return result;
    }

    /** isPrime = test pierwszości przez próbne dzielenie do √n. O(√n). */
    static boolean isPrime(int n) {
        if (n < 2) { return false; }
        if (n % 2 == 0) { return n == 2; }
        for (int i = 3; (long) i * i <= n; i += 2) {
            if (n % i == 0) { return false; }
        }
        return true;
    }

    /** isPerfect = czy n jest liczbą doskonałą (równa sumie swoich dzielników właściwych). */
    static boolean isPerfect(int n) {
        if (n < 2) { return false; }
        int sumOfDivisors = 1;                                        // 1 jest dzielnikiem właściwym każdego n > 1
        for (int i = 2; (long) i * i <= n; i++) {
            if (n % i == 0) {
                sumOfDivisors += i;
                int other = n / i;
                if (other != i) { sumOfDivisors += other; }
            }
        }
        return sumOfDivisors == n;
    }

    /** countDigits = liczba cyfr dziesiętnych liczby nieujemnej (0 ma 1 cyfrę). */
    private static int countDigits(int n) {
        if (n == 0) { return 1; }
        int count = 0;
        while (n > 0) {
            count++;
            n /= 10;
        }
        return count;
    }

    /** isArmstrong = czy suma cyfr podniesionych do potęgi (liczba cyfr) daje z powrotem n. */
    static boolean isArmstrong(int n) {
        if (n < 0) { return false; }
        int digitCount = countDigits(n);
        int sum = 0;
        int remaining = n;
        while (remaining > 0) {
            int digit = remaining % 10;
            sum += (int) Math.pow(digit, digitCount);
            remaining /= 10;
        }
        return sum == n;
    }

    /** digitSum = suma cyfr liczby (dla ujemnych — suma cyfr wartości bezwzględnej), bez String. */
    static int digitSum(int n) {
        n = Math.abs(n);
        int sum = 0;
        while (n > 0) {
            sum += n % 10;
            n /= 10;
        }
        return sum;
    }

    /** reverseNumber = odwraca kolejność cyfr, zachowując znak, bez String. */
    static int reverseNumber(int n) {
        boolean negative = n < 0;
        n = Math.abs(n);
        int reversed = 0;
        while (n > 0) {
            reversed = reversed * 10 + n % 10;
            n /= 10;
        }
        return negative ? -reversed : reversed;
    }

    /** isPalindromeNumber = czy liczba czytana od tyłu daje tę samą liczbę (ujemne — zawsze false). */
    static boolean isPalindromeNumber(int n) {
        if (n < 0) { return false; }
        return n == reverseNumber(n);
    }

    /** manualBitCount = ręczne liczenie jedynek w zapisie dwójkowym (>>> — przesunięcie BEZ znaku!). */
    static int manualBitCount(int n) {
        int count = 0;
        while (n != 0) {
            count += n & 1;
            n >>>= 1;
        }
        return count;
    }

    static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
    }

    /** integerSqrt = podłoga z pierwiastka kwadratowego, wyszukiwaniem binarnym. O(log n). */
    static int integerSqrt(int n) {
        if (n < 0) { throw new IllegalArgumentException("n musi być nieujemne: " + n); }
        int lo = 0;
        int hi = n;
        int result = 0;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;                             // unika przepełnienia (lo + hi) dla dużych n
            long square = (long) mid * mid;                           // long, żeby mid*mid się nie przepełniło
            if (square == n) { return mid; }
            if (square < n) {
                result = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return result;
    }

    /** swapWithoutTemp = zamiana dwóch int bez trzeciej zmiennej (ciekawostka — patrz PUŁAPKA w sekcji 7). */
    static int[] swapWithoutTemp(int a, int b) {
        a = a + b;
        b = a - b;
        a = a - b;
        return new int[]{a, b};
    }

    static List<String> fizzBuzzClassic(int limit) {
        List<String> result = new ArrayList<>();
        for (int i = 1; i <= limit; i++) {
            if (i % 15 == 0) {
                result.add("FizzBuzz");
            } else if (i % 3 == 0) {
                result.add("Fizz");
            } else if (i % 5 == 0) {
                result.add("Buzz");
            } else {
                result.add(String.valueOf(i));
            }
        }
        return result;
    }

    static List<String> fizzBuzzConcat(int limit) {
        List<String> result = new ArrayList<>();
        for (int i = 1; i <= limit; i++) {
            String word = (i % 3 == 0 ? "Fizz" : "") + (i % 5 == 0 ? "Buzz" : "");
            result.add(word.isEmpty() ? String.valueOf(i) : word);
        }
        return result;
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   SILNIA           int przepełnia się przy 13!, long przy 21!; zera na końcu = n/5 + n/25 + ...
     *   FIBONACCI        naiwna rekurencja O(2^n) — memoizacja albo iteracja → O(n);  isPrime: dzielenie do √n → O(√n)
     *   PERFECT (6,28,496)      suma dzielników właściwych == n;  ARMSTRONG (153,370,371,407)  suma(cyfra^liczbaCyfr) == n
     *   digitSum/reverseNumber  % 10 i / 10, bez String, uwaga na przepełnienie;  PALINDROM: n == reverseNumber(n), ujemne = false
     *   POTĘGA DWÓJKI    n > 0 && (n & (n-1)) == 0;  bitCount: >>> (bez znaku!), nie >>
     *   ROK PRZESTĘPNY   %4==0 && (%100!=0 || %400==0);  PIERWIASTEK CAŁK. wyszukiwanie binarne O(log n), mid*mid na long
     *   SWAP BEZ TEMP    ciekawostka rekrutacyjna, nie styl produkcyjny;  FIZZBUZZ: if-else-if albo sklejanie napisu
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego naiwna rekurencja Fibonacciego jest wykładnicza, skoro liczy "tylko" dodawania?
     *   2. Co wypisze:  System.out.println(factorialIterative(13));  (jako int, bez Math.multiplyExact)?
     *   3. ZNAJDŹ BŁĄD:  for (int i = 2; i < n; i++) if (n % i == 0) return false; return true;
     *      (dla n = 1 zwraca true — dlaczego to błąd i jak go poprawić?)
     *   4. Co wypisze:  System.out.println(isArmstrong(0));  ? (0 ma 1 cyfrę, 0^1 = 0)
     *   5. ZNAJDŹ BŁĄD:  int r = reverseNumber(Integer.MAX_VALUE); System.out.println(r > 0);
     *      (ktoś zakłada, że odwrócenie dodatniej liczby jest dodatnie)
     *   6. Co wypisze:  int[] arr = {5}; int[] r = swapWithoutTemp(arr[0], arr[0]);  (ten sam element dwa razy)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: isPrime(97)", true, () -> exercise1(97));
        Check.equal("ćw. 1b: isPrime(91)", false, () -> exercise1(91));
        Check.equal("ćw. 1c: isPrime(1)", false, () -> exercise1(1));
        Check.equal("ćw. 2a: digitSum(12345)", 15, () -> exercise2(12345));
        Check.equal("ćw. 2b: digitSum(-908)", 17, () -> exercise2(-908));
        Check.equal("ćw. 3a: reverseNumber(12345)", 54321, () -> exercise3(12345));
        Check.equal("ćw. 3b: reverseNumber(-120)", -21, () -> exercise3(-120));
        Check.equal("ćw. 4a: isArmstrong(153)", true, () -> exercise4(153));
        Check.equal("ćw. 4b: isArmstrong(154)", false, () -> exercise4(154));
        Check.equal("ćw. 4c: isArmstrong(9474) [4-cyfrowa]", true, () -> exercise4(9474));
        Check.equal("ćw. 5a: integerSqrt(99)", 9, () -> exercise5(99));
        Check.equal("ćw. 5b: integerSqrt(100)", 10, () -> exercise5(100));
        Check.equal("ćw. 5c: integerSqrt(0)", 0, () -> exercise5(0));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", true, () -> solution1(97));
        Check.equal("ćw. 1b (wzorzec)", false, () -> solution1(91));
        Check.equal("ćw. 1c (wzorzec)", false, () -> solution1(1));
        Check.equal("ćw. 2a (wzorzec)", 15, () -> solution2(12345));
        Check.equal("ćw. 2b (wzorzec)", 17, () -> solution2(-908));
        Check.equal("ćw. 3a (wzorzec)", 54321, () -> solution3(12345));
        Check.equal("ćw. 3b (wzorzec)", -21, () -> solution3(-120));
        Check.equal("ćw. 4a (wzorzec)", true, () -> solution4(153));
        Check.equal("ćw. 4b (wzorzec)", false, () -> solution4(154));
        Check.equal("ćw. 4c (wzorzec)", true, () -> solution4(9474));
        Check.equal("ćw. 5a (wzorzec)", 9, () -> solution5(99));
        Check.equal("ćw. 5b (wzorzec)", 10, () -> solution5(100));
        Check.equal("ćw. 5c (wzorzec)", 0, () -> solution5(0));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 13 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): czy {@code n} jest liczbą pierwszą? Podpowiedź: próbne dzielenie do √n, uwaga na {@code n < 2}. */
    static boolean exercise1(int n) {
        // Pusty stub przypadkiem "zaliczyłby" jeden z testów — wyjątek pokazuje ✘, dopóki go nie zrobisz.
        throw new UnsupportedOperationException("TODO");
    }

    /** ĆWICZENIE 2 (łatwe): suma cyfr {@code n} (bez String, ujemne — z wartości bezwzględnej). Podpowiedź: {@code % 10} i {@code / 10}. */
    static int exercise2(int n) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /** ĆWICZENIE 3 (średnie): odwróć cyfry {@code n} (bez String), zachowaj znak. Podpowiedź: {@code reversed = reversed * 10 + cyfra}. */
    static int exercise3(int n) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /** ĆWICZENIE 4 (średnie): czy {@code n} jest liczbą Armstronga (dowolna liczba cyfr)? Podpowiedź: sumuj {@code cyfra^liczbaCyfr}. */
    static boolean exercise4(int n) {
        // TODO: twoje rozwiązanie
        return false;
    }

    /** ĆWICZENIE 5 (trudniejsze): podłoga z √n wyszukiwaniem binarnym, bez Math.sqrt, n ≥ 0. Podpowiedź: mid*mid na long. */
    static int exercise5(int n) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(int n) {
        return isPrime(n);
    }

    static int solution2(int n) {
        return digitSum(n);
    }

    static int solution3(int n) {
        return reverseNumber(n);
    }

    static boolean solution4(int n) {
        return isArmstrong(n);
    }

    static int solution5(int n) {
        return integerSqrt(n);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Każde wywołanie fibNaive(n) (n > 1) rozgałęzia się na DWA kolejne — drzewo wywołań ma wysokość n
     *      i w przybliżeniu 2^n węzłów. Dodawania są tanie, ale jest ich wykładniczo dużo.
     *   2. -1257485296 (albo inna "losowa" liczba ujemna) — 13! = 6 227 020 800 nie mieści się w incie
     *      (max 2 147 483 647), wynik cicho przepełnia się bez wyjątku.
     *   3. Dla n = 1 pętla "for (i=2; i<1; ...)" w ogóle się nie wykonuje, więc funkcja zwraca true — ale 1
     *      nie jest pierwsza! Brakuje {@code if (n < 2) return false;} na początku.
     *   4. true — 0 ma (umownie) 1 cyfrę, 0^1 = 0, suma (0) równa się n (0). Łatwy do przeoczenia przypadek brzegowy.
     *   5. Odwrócenie Integer.MAX_VALUE (2147483647 → 7463847412) przekracza zakres int i przepełnia się,
     *      dając wynik nieprzewidywalny co do znaku — założenie "dodatnie zostanie dodatnie" jest fałszywe.
     *   6. [5, 5] — arr[0] jest przekazywane DWA RAZY jako ta sama wartość (przekazanie przez wartość —
     *      swapWithoutTemp nie widzi, że to "ten sam" element tablicy): a=5+5=10; b=10-5=5; a=10-5=5 → [5, 5].
     *      Wersja XOR wykonana WPROST na arr[i] (arr[i]^=arr[i]; ...) faktycznie wyzerowałaby element.
     */
    // </editor-fold>
}
