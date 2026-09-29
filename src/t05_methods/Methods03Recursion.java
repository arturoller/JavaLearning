package t05_methods;

import helpers.Check;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Rekurencja — metoda, która wywołuje samą siebie
 *        (recursion = rekurencja; base case = przypadek bazowy; recursive step = krok rekurencyjny)
 *
 * W SKRÓCIE:
 *   Metoda rekurencyjna rozwiązuje duży problem, wywołując SIEBIE dla mniejszego kawałka tego samego problemu.
 *   Każda rekurencja potrzebuje PRZYPADKU BAZOWEGO — sytuacji, w której odpowiedź jest znana od razu i wywołania
 *   się kończą. Bez niego metoda woła się w nieskończoność aż do StackOverflowError (przepełnienia stosu).
 *
 * ANALOGIA: matrioszka.
 *   Otwierasz lalkę, a w środku jest mniejsza lalka — robisz z nią to samo. Kończysz, gdy trafisz na najmniejszą,
 *   której nie da się otworzyć (przypadek bazowy). Potem „wracasz”, składając lalki z powrotem (zwracanie wyników).
 *
 * JAK TO DZIAŁA:
 *   static long factorial(int n) {
 *       {@code if (n <= 1) return 1;}       ← przypadek bazowy: 1! = 1
 *       return n * factorial(n - 1);       ← krok: n! = n × (n-1)!
 *   }
 *   factorial(3) = 3 × factorial(2) = 3 × (2 × factorial(1)) = 3 × (2 × 1) = 6
 *   Każde wywołanie to nowa ramka na stosie wywołań (Methods01Basics, sekcja 6) — dlatego głębokość jest ograniczona.
 *
 * SŁÓWKA:
 *   recursion = rekurencja; recursive = rekurencyjny; base case = przypadek bazowy; stack overflow = przepełnienie stosu;
 *   memoization = zapamiętywanie wyników (memoizacja); depth = głębokość; factorial = silnia; palindrome = palindrom;
 *   gcd (greatest common divisor) = NWD (największy wspólny dzielnik).
 *
 * ZOBACZ TEŻ: t05_methods/Methods01Basics (stos wywołań), t13_lambdas/Lambda07HigherOrderFunctions (memoizacja z mapą),
 *             t24_algorithms/Algorithms02Sorting (sortowanie przez scalanie — rekurencja w praktyce).
 * </pre>
 */
public class Methods03Recursion {

    /** fibCalls = licznik wywołań metody fib — żeby zobaczyć koszt naiwnej rekurencji. */
    private static long fibCalls = 0;

    public static void main(String[] args) {
        title("Methods03 — rekurencja");

        countdownDemo();        // countdown demo = odliczanie
        factorialWithTrace();   // factorial with trace = silnia ze śladem wywołań
        naiveVsMemoFibonacci(); // naive vs memo Fibonacci = Fibonacci naiwny kontra z pamięcią
        recursionOnStrings();   // recursion on strings = rekurencja na napisach
        recursionOnArrays();    // recursion on arrays = rekurencja na tablicach
        stackOverflow();        // stack overflow = przepełnienie stosu
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PIERWSZA REKURENCJA — ODLICZANIE
    // =================================================================================================

    /** countdown = odliczanie. Przypadek bazowy: n == 0. Krok: wypisz n i odlicz od n - 1. */
    static void countdown(int n) {
        if (n == 0) {
            System.out.println("   Start!");
            return;
        }
        System.out.println("   " + n);
        countdown(n - 1);
    }

    /** 1. Najprostsza rekurencja: każde wywołanie robi mały krok i przekazuje „resztę pracy” samemu sobie. */
    static void countdownDemo() {
        section("1. Odliczanie rekurencyjne");

        countdown(3);
        // WYNIK: 3
        // WYNIK: 2
        // WYNIK: 1
        // WYNIK: Start!
    }

    // =================================================================================================
    // 2. SILNIA — ŚLAD WYWOŁAŃ
    // =================================================================================================

    /** factorialTraced = silnia ze śladem. depth (głębokość) steruje wcięciem, żeby było widać zagnieżdżenie. */
    static long factorialTraced(int n, int depth) {
        String indent = "   " + "  ".repeat(depth);
        System.out.println(indent + "factorial(" + n + ")");
        long result = (n <= 1) ? 1 : n * factorialTraced(n - 1, depth + 1);
        System.out.println(indent + "= " + result);
        return result;
    }

    /**
     * 2. Silnia (factorial): n! = n × (n-1)!, a 1! = 1. Ślad pokazuje, że najpierw wywołania „schodzą w dół”
     * do przypadku bazowego, a dopiero potem wyniki „wracają w górę” i są mnożone.
     */
    static void factorialWithTrace() {
        section("2. Silnia — wywołania schodzą w dół, wyniki wracają w górę");

        long result = factorialTraced(3, 0);
        show("3!", result);
        // WYNIK: factorial(3)
        // WYNIK: factorial(2)
        // WYNIK: factorial(1)
        // WYNIK: = 1
        // WYNIK: = 2
        // WYNIK: = 6
        // WYNIK: 3! → 6
    }

    // =================================================================================================
    // 3. FIBONACCI: NAIWNIE KONTRA Z PAMIĘCIĄ
    // =================================================================================================

    /** fib = n-ty wyraz ciągu Fibonacciego (0, 1, 1, 2, 3, 5, 8...). Naiwnie: liczy te same wartości wiele razy. */
    static long fib(int n) {
        fibCalls++;
        if (n < 2) {
            return n;
        }
        return fib(n - 1) + fib(n - 2);
    }

    /** fibMemo = Fibonacci z pamięcią. memo[n] przechowuje już policzony wynik (0 = jeszcze nie liczony). */
    static long fibMemo(int n, long[] memo) {
        fibCalls++;
        if (n < 2) {
            return n;
        }
        if (memo[n] != 0) {
            return memo[n];                          // już liczyliśmy — oddaj z pamięci
        }
        memo[n] = fibMemo(n - 1, memo) + fibMemo(n - 2, memo);
        return memo[n];
    }

    /**
     * 3. Naiwna rekurencja Fibonacciego liczy fib(18) dwa razy, fib(17) trzy razy... Liczba wywołań rośnie
     * wykładniczo. Memoizacja (zapamiętywanie wyników) sprowadza to do kilkudziesięciu wywołań.
     */
    static void naiveVsMemoFibonacci() {
        section("3. Fibonacci: naiwnie kontra z pamięcią (memoizacja)");

        fibCalls = 0;
        long naive = fib(20);
        show("fib(20) naiwnie — wynik / wywołań", naive + " / " + fibCalls);
        // WYNIK: fib(20) naiwnie — wynik / wywołań → 6765 / 21891

        fibCalls = 0;
        long memo = fibMemo(20, new long[21]);
        show("fib(20) z pamięcią — wynik / wywołań", memo + " / " + fibCalls);
        // WYNIK: fib(20) z pamięcią — wynik / wywołań → 6765 / 39

        // PUŁAPKA: fib(50) naiwnie wykonałby ok. 40 miliardów wywołań — program „zawiesiłby się” na długie minuty.
        // DOBRA PRAKTYKA: gdy rekurencja liczy te same podproblemy wiele razy — zapamiętuj wyniki albo użyj pętli.
    }

    // =================================================================================================
    // 4. REKURENCJA NA NAPISACH
    // =================================================================================================

    /** reverse = odwróć napis: ostatnia litera + odwrócona reszta. Przypadek bazowy: napis pusty albo 1-znakowy. */
    static String reverse(String text) {
        if (text.length() <= 1) {
            return text;
        }
        return text.charAt(text.length() - 1) + reverse(text.substring(0, text.length() - 1));
    }

    /** isPalindrome = czy palindrom: pierwsza i ostatnia litera równe ORAZ środek też jest palindromem. */
    static boolean isPalindrome(String text) {
        if (text.length() <= 1) {
            return true;
        }
        if (text.charAt(0) != text.charAt(text.length() - 1)) {
            return false;
        }
        return isPalindrome(text.substring(1, text.length() - 1));
    }

    /** 4. Napis też można „zmniejszać” — odcinając pierwszą albo ostatnią literę (substring = podnapis). */
    static void recursionOnStrings() {
        section("4. Rekurencja na napisach");

        show("reverse(\"Java\")", reverse("Java"));
        show("isPalindrome(\"kajak\")", isPalindrome("kajak"));
        show("isPalindrome(\"java\")", isPalindrome("java"));
        // WYNIK: reverse("Java") → avaJ
        // WYNIK: isPalindrome("kajak") → true
        // WYNIK: isPalindrome("java") → false
    }

    // =================================================================================================
    // 5. REKURENCJA NA TABLICACH
    // =================================================================================================

    /** sumFrom = suma od indeksu. Zamiast kopiować tablicę, przekazujemy indeks, od którego liczymy. */
    static int sumFrom(int[] numbers, int index) {
        if (index == numbers.length) {
            return 0;                                           // przypadek bazowy: nic nie zostało do dodania
        }
        return numbers[index] + sumFrom(numbers, index + 1);
    }

    /** binarySearch = wyszukiwanie binarne w POSORTOWANEJ tablicy: sprawdź środek i szukaj w jednej połowie. */
    static int binarySearch(int[] sorted, int target, int from, int to) {
        if (from > to) {
            return -1;                                          // przedział pusty — nie ma takiej liczby
        }
        int middle = (from + to) / 2;
        if (sorted[middle] == target) {
            return middle;
        }
        if (sorted[middle] < target) {
            return binarySearch(sorted, target, middle + 1, to);   // szukaj w prawej połowie
        }
        return binarySearch(sorted, target, from, middle - 1);     // szukaj w lewej połowie
    }

    /** 5. Na tablicach „zmniejszamy” problem, przesuwając indeks albo zawężając przedział — bez kopiowania danych. */
    static void recursionOnArrays() {
        section("5. Rekurencja na tablicach");

        int[] numbers = {1, 2, 3, 4};
        show("suma [1, 2, 3, 4]", sumFrom(numbers, 0));
        // WYNIK: suma [1, 2, 3, 4] → 10

        int[] sorted = {1, 3, 5, 7, 9, 11};
        show("indeks 7 w [1, 3, 5, 7, 9, 11]", binarySearch(sorted, 7, 0, sorted.length - 1));
        show("indeks 4 (brak)", binarySearch(sorted, 4, 0, sorted.length - 1));
        // WYNIK: indeks 7 w [1, 3, 5, 7, 9, 11] → 3
        // WYNIK: indeks 4 (brak) → -1
    }

    // =================================================================================================
    // 6. BRAK PRZYPADKU BAZOWEGO → StackOverflowError
    // =================================================================================================

    /** forever = w nieskończoność. BŁĄD CELOWY: brak przypadku bazowego. */
    static int forever(int n) {
        return forever(n + 1);
    }

    /**
     * 6. Każde wywołanie zajmuje miejsce na stosie. Rekurencja bez końca (albo po prostu za głęboka — np. kilkaset
     * tysięcy poziomów) wyczerpuje stos i JVM rzuca StackOverflowError. To Error, a nie Exception — zwykle się go
     * nie łapie, tylko poprawia kod. Tu łapiemy go wyłącznie po to, żeby pokazać.
     */
    static void stackOverflow() {
        section("6. Brak przypadku bazowego → StackOverflowError");

        expectThrows("forever(0)", () -> forever(0));
        // WYNIK: ✔ forever(0) → rzucono StackOverflowError: (brak komunikatu)

        // DOBRA PRAKTYKA: rekurencję stosuj tam, gdzie problem jest naturalnie „zagnieżdżony” (drzewa folderów,
        //   dziel i zwyciężaj: sortowanie przez scalanie, wyszukiwanie binarne). Proste powtarzanie (suma, silnia
        //   dużych liczb) lepiej robić pętlą — Java nie optymalizuje rekurencji ogonowej, więc głębokość jest ograniczona.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Rekurencja = metoda woła samą siebie dla MNIEJSZEGO problemu.
     *   • Zawsze: przypadek bazowy (koniec) + krok zbliżający do przypadku bazowego.
     *   • Wywołania schodzą w dół do przypadku bazowego, wyniki wracają w górę.
     *   • Naiwny Fibonacci: wykładnicza liczba wywołań → memoizacja (tablica/mapa wyników) albo pętla.
     *   • Na napisach: substring; na tablicach: indeks albo przedział (from, to) zamiast kopiowania.
     *   • Brak/za głęboka rekurencja → StackOverflowError. Proste powtórzenia — lepiej pętlą.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie dwa elementy musi mieć każda metoda rekurencyjna?
     *   2. Co wypisze:  static int f(int n) { if (n == 0) return 0; return n + f(n - 1); }   System.out.println(f(4));  ?
     *   3. ZNAJDŹ BŁĄD:  static int down(int n) { if (n == 0) return 0; return down(n + 1); }   down(5);
     *   4. Dlaczego naiwny fib(40) liczy się bardzo długo, a wersja z pamięcią błyskawicznie?
     *   5. Co się stanie przy bardzo głębokiej rekurencji (np. 1 000 000 poziomów)?
     *   6. Kiedy lepiej użyć pętli zamiast rekurencji?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: suma cyfr 1234", 10, () -> exercise1(1234));
        Check.equal("ćw. 2: 2 do potęgi 20", 1_048_576L, () -> exercise2(2, 20));
        Check.equal("ćw. 3: ile 'a' w \"banana\"", 3, () -> exercise3("banana", 'a'));
        Check.equal("ćw. 4: NWD(48, 18)", 6, () -> exercise4(48, 18));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 10, () -> solution1(1234));
        Check.equal("ćw. 2 (wzorzec)", 1_048_576L, () -> solution2(2, 20));
        Check.equal("ćw. 3 (wzorzec)", 3, () -> solution3("banana", 'a'));
        Check.equal("ćw. 4 (wzorzec)", 6, () -> solution4(48, 18));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): rekurencyjnie policz sumę cyfr liczby nieujemnej (1234 → 10).
     * Podpowiedź: ostatnia cyfra to {@code n % 10}, reszta liczby to {@code n / 10}; przypadek bazowy: n == 0.
     */
    static int exercise1(int n) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): rekurencyjnie policz base do potęgi exp (exp ≥ 0), wynik typu long.
     * Podpowiedź: base^0 = 1; base^exp = base × base^(exp-1).
     */
    static long exercise2(long base, int exp) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): rekurencyjnie policz, ile razy znak c występuje w napisie.
     * Podpowiedź: pusty napis → 0; w przeciwnym razie (czy pierwszy znak to c ? 1 : 0) + wynik dla {@code text.substring(1)}.
     */
    static int exercise3(String text, char c) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): NWD (największy wspólny dzielnik) algorytmem Euklidesa, rekurencyjnie.
     * NWD(a, 0) = a; NWD(a, b) = NWD(b, a % b). Dla (48, 18): NWD(18, 12) → NWD(12, 6) → NWD(6, 0) = 6.
     */
    static int exercise4(int a, int b) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int n) {
        if (n == 0) {
            return 0;
        }
        return n % 10 + solution1(n / 10);
    }

    static long solution2(long base, int exp) {
        if (exp == 0) {
            return 1;
        }
        return base * solution2(base, exp - 1);
    }

    static int solution3(String text, char c) {
        if (text.isEmpty()) {
            return 0;
        }
        return (text.charAt(0) == c ? 1 : 0) + solution3(text.substring(1), c);
    }

    static int solution4(int a, int b) {
        if (b == 0) {
            return a;
        }
        return solution4(b, a % b);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Przypadek bazowy (kiedy przestać) i krok rekurencyjny, który ZBLIŻA do przypadku bazowego.
     *   2. 10 (4 + 3 + 2 + 1 + 0).
     *   3. down(n + 1) oddala się od przypadku bazowego (n == 0) — rekurencja nigdy się nie skończy → StackOverflowError.
     *      Poprawnie: down(n - 1).
     *   4. Naiwna wersja liczy te same wartości miliony razy (liczba wywołań rośnie wykładniczo); z pamięcią każda
     *      wartość jest liczona raz.
     *   5. StackOverflowError — stos wywołań ma ograniczony rozmiar.
     *   6. Gdy to proste powtarzanie (suma, licznik) albo gdy głębokość może być bardzo duża — pętla nie grozi
     *      przepełnieniem stosu i zwykle jest szybsza.
     */
    // </editor-fold>
}
