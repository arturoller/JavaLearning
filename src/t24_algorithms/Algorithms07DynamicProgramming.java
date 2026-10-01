package t24_algorithms;

import java.util.Arrays;

import helpers.Check;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Programowanie dynamiczne — pamiętaj, zamiast liczyć od nowa
 *        (dynamic programming = programowanie dynamiczne; memoization = zapamiętywanie; tabulation = tabelowanie)
 *
 * W SKRÓCIE:
 *   Programowanie dynamiczne (DP) stosuje się tam, gdzie naiwna rekurencja liczy TE SAME podproblemy wiele
 *   razy (nakładające się podproblemy) i gdzie rozwiązanie całości da się złożyć z rozwiązań części
 *   (optymalna podstruktura). Zamiast liczyć od nowa — zapamiętaj wynik raz i użyj go ponownie.
 *
 * ANALOGIA: notatnik zamiast pamięci.
 *   Naiwna rekurencja to liczenie w głowie za każdym razem od zera. DP to zapisanie wyniku w notatniku —
 *   gdy pytanie się powtórzy, zerkasz do notatnika zamiast liczyć ponownie.
 *
 * JAK TO DZIAŁA:
 *   Top-down (memoizacja): zwykła rekurencja + tablica/mapa "już policzone" sprawdzana NA WEJŚCIU do funkcji.
 *   Bottom-up (tabelowanie): pętla budująca tablicę wyników OD najmniejszych podproblemów do największego —
 *   bez rekurencji w ogóle. Często da się potem sprowadzić do O(1) pamięci, gdy liczy się tylko kilka ostatnich wyników.
 *   Rozpoznawanie DP: 1) da się podzielić na mniejsze, PODOBNE podproblemy? 2) te same podproblemy POWTARZAJĄ
 *   się w drzewie rekurencji? 3) optymalny wynik całości da się złożyć z optymalnych wyników części?
 *
 * SŁÓWKA:
 *   overlapping subproblems = nakładające się podproblemy; optimal substructure = optymalna podstruktura;
 *   memoization = zapamiętywanie (top-down); tabulation = tabelowanie (bottom-up); state = stan (co opisuje podproblem);
 *   transition = przejście (wzór łączący stany).
 *
 * ZOBACZ TEŻ: t05_methods/Methods03Recursion (naiwny Fibonacci i jego koszt),
 *             t24_algorithms/Algorithms01Complexity (notacja O(), amortyzacja),
 *             t24_algorithms/Algorithms06Backtracking (gdy stany się NIE powtarzają — zwykłe przeszukiwanie).
 * </pre>
 */
public class Algorithms07DynamicProgramming {

    public static void main(String[] args) {
        title("Algorithms07 — programowanie dynamiczne");

        fibonacciAllWays();     // Fibonacci = ciąg Fibonacciego, na cztery sposoby
        climbingStairs();       // climbing stairs = wchodzenie po schodach
        coinChangeMinCoins();   // coin change min coins = minimalna liczba monet
        coinChangeWaysCount();  // coin change ways = liczba sposobów wydania reszty
        longestCommonSubsequence(); // LCS = najdłuższy wspólny podciąg
        knapsack01Demo();           // 0/1 knapsack = problem plecakowy 0/1
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. FIBONACCI NA CZTERY SPOSOBY — OD NAIWNEGO DO O(1) PAMIĘCI
    // =================================================================================================

    static long fibCalls = 0;

    static long fibNaive(int n) {
        fibCalls++;
        if (n < 2) return n;
        return fibNaive(n - 1) + fibNaive(n - 2);
    }

    /** fibMemoTopDown = rekurencja + tablica "już policzone" (memo) sprawdzana NA WEJŚCIU — top-down. */
    static long fibMemoTopDown(int n, long[] memo) {
        fibCalls++;
        if (n < 2) return n;
        if (memo[n] != -1) return memo[n]; // już liczyliśmy — oddaj z notatnika
        memo[n] = fibMemoTopDown(n - 1, memo) + fibMemoTopDown(n - 2, memo);
        return memo[n];
    }

    /** fibTableBottomUp = BEZ rekurencji: budujemy tablicę wyników od najmniejszego podproblemu w górę. */
    static long fibTableBottomUp(int n, long[] steps) {
        long[] dp = new long[n + 1];
        dp[0] = 0;
        if (n >= 1) dp[1] = 1;
        for (int i = 2; i <= n; i++) { steps[0]++; dp[i] = dp[i - 1] + dp[i - 2]; }
        return dp[n];
    }

    /** fibConstantSpace = ten sam pomysł co bottom-up, ale pamiętamy TYLKO dwa ostatnie wyniki, nie całą tablicę. */
    static long fibConstantSpace(int n, long[] steps) {
        if (n < 2) return n;
        long prev = 0, curr = 1;
        for (int i = 2; i <= n; i++) {
            steps[0]++;
            long next = prev + curr;
            prev = curr;
            curr = next;
        }
        return curr;
    }

    /**
     * 1. Fibonacci: naiwnie liczy te same podproblemy MILIONY razy (nakładające się podproblemy —
     * fib(18) liczony wielokrotnie wewnątrz fib(20)). Memoizacja zapamiętuje każdy wynik RAZ. Tabelowanie
     * robi to samo bez rekurencji (i bez ryzyka StackOverflowError). Wersja O(1) pamięci zauważa, że do
     * policzenia fib(i) potrzeba TYLKO dwóch poprzednich wartości — po co trzymać całą tablicę?
     */
    static void fibonacciAllWays() {
        section("1. Fibonacci na cztery sposoby — od naiwnego do O(1) pamięci");

        int n = 20;
        fibCalls = 0;
        long naive = fibNaive(n);
        show("naiwnie — wynik / wywołań", naive + " / " + fibCalls);
        // WYNIK: naiwnie — wynik / wywołań → 6765 / 21891

        fibCalls = 0;
        long[] memo = new long[n + 1];
        Arrays.fill(memo, -1);
        long topDown = fibMemoTopDown(n, memo);
        show("top-down (memoizacja) — wynik / wywołań", topDown + " / " + fibCalls);
        // WYNIK: top-down (memoizacja) — wynik / wywołań → 6765 / 39

        long[] steps1 = {0};
        long bottomUp = fibTableBottomUp(n, steps1);
        show("bottom-up (tabela) — wynik / kroków pętli", bottomUp + " / " + steps1[0]);
        // WYNIK: bottom-up (tabela) — wynik / kroków pętli → 6765 / 19

        long[] steps2 = {0};
        long constantSpace = fibConstantSpace(n, steps2);
        show("O(1) pamięci — wynik / kroków pętli", constantSpace + " / " + steps2[0]);
        // WYNIK: O(1) pamięci — wynik / kroków pętli → 6765 / 19

        note("21891 wywołań → 39 → 19 → 19: ten sam wynik, drastycznie mniej pracy — DP nie zmienia ODPOWIEDZI, zmienia KOSZT");

        // DOBRA PRAKTYKA: zacznij od wersji naiwnej (poprawność), potem dodaj memoizację (top-down, najłatwiej
        //   przerobić z rekurencji), a dopiero potem — jeśli trzeba — tabelowanie i redukcję pamięci.
    }

    // =================================================================================================
    // 2. WCHODZENIE PO SCHODACH (CLIMBING STAIRS)
    // =================================================================================================

    /** climbStairsTable = ile jest sposobów wejścia na i-ty schodek, biorąc po 1 albo 2 stopnie naraz. */
    static int[] climbStairsTable(int n) {
        int[] dp = new int[n + 1];
        dp[0] = 1; // jeden sposób, by "wejść" na 0 schodków: nie robić nic
        if (n >= 1) dp[1] = 1;
        for (int i = 2; i <= n; i++) {
            dp[i] = dp[i - 1] + dp[i - 2]; // na i-ty schodek: z (i-1) jednym krokiem LUB z (i-2) dwoma krokami
        }
        return dp;
    }

    /**
     * 2. Ten sam wzór co Fibonacci! Liczba sposobów dojścia na schodek i to suma sposobów dojścia na
     * (i-1) i (i-2) — bo ostatni krok to był krok o 1 ALBO o 2. (PYTANIE REKRUTACYJNE: climbing stairs).
     */
    static void climbingStairs() {
        section("2. Wchodzenie po schodach (climbing stairs)");

        int[] tableFor5 = climbStairsTable(5);
        show("tabela sposobów dla 0..5 schodków", tableFor5);
        show("liczba sposobów na 10 schodków", climbStairsTable(10)[10]);
        // WYNIK: tabela sposobów dla 0..5 schodków → [1, 1, 2, 3, 5, 8]
        // WYNIK: liczba sposobów na 10 schodków → 89

        note("dp[5]=8 pasuje do fib(6) — rozpoznanie \"to jest Fibonacci pod inną nazwą\" to częsty klucz w DP");
    }

    // =================================================================================================
    // 3. WYDAWANIE RESZTY — MINIMALNA LICZBA MONET (COIN CHANGE, MIN COINS)
    // =================================================================================================

    /** minCoinsTable = dp[a] = minimalna liczba monet sumujących się do a (polskie nominały 1, 2, 5 zł). */
    static int minCoins(int amount, int[] coins) {
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, Integer.MAX_VALUE);
        dp[0] = 0;
        for (int a = 1; a <= amount; a++) {
            for (int coin : coins) {
                if (coin <= a && dp[a - coin] != Integer.MAX_VALUE) {
                    dp[a] = Math.min(dp[a], dp[a - coin] + 1);
                }
            }
        }
        return dp[amount];
    }

    /**
     * 3. Dla każdej kwoty a sprawdzamy WSZYSTKIE monety: jeśli moneta c pasuje, to dp[a] = dp[a-c] + 1
     * (jedna moneta c, plus najlepszy sposób na resztę). Bierzemy MINIMUM po wszystkich monetach.
     * (PYTANIE REKRUTACYJNE: minimalna liczba monet sumujących się do kwoty).
     */
    static void coinChangeMinCoins() {
        section("3. Wydawanie reszty — minimalna liczba monet (coin change, min coins)");

        int[] polishCoins = {1, 2, 5};
        show("monety", polishCoins);
        show("minimalna liczba monet na 11 zł", minCoins(11, polishCoins));
        show("minimalna liczba monet na 7 zł", minCoins(7, polishCoins));
        // WYNIK: monety → [1, 2, 5]
        // WYNIK: minimalna liczba monet na 11 zł → 3
        // WYNIK: minimalna liczba monet na 7 zł → 2

        note("11 zł = 5+5+1 (3 monety); 7 zł = 5+2 (2 monety) — zachłanne \"bierz największą monetę\" akurat tu też by zadziałało,"
                + " ale DP działa ZAWSZE, nawet dla nominałów, gdzie zachłanność zawodzi");
    }

    // =================================================================================================
    // 4. WYDAWANIE RESZTY — LICZBA SPOSOBÓW (COIN CHANGE, COUNT WAYS)
    // =================================================================================================

    /** waysToMakeAmount = ile RÓŻNYCH zestawów monet (bez względu na kolejność) sumuje się do amount. */
    static long waysToMakeAmount(int amount, int[] coins) {
        long[] dp = new long[amount + 1];
        dp[0] = 1; // jeden sposób na zebranie 0: nie brać żadnej monety
        for (int coin : coins) {          // moneta w pętli ZEWNĘTRZNEJ — stąd liczymy ZESTAWY, nie uporządkowane ciągi
            for (int a = coin; a <= amount; a++) {
                dp[a] += dp[a - coin];
            }
        }
        return dp[amount];
    }

    /**
     * 4. PUŁAPKA klasyczna: jeśli moneta byłaby w pętli WEWNĘTRZNEJ, policzylibyśmy "1+2" i "2+1" jako dwa
     * różne sposoby (to byłoby liczenie UPORZĄDKOWANYCH ciągów). Moneta na ZEWNĄTRZ gwarantuje, że liczymy
     * każdy ZESTAW monet raz, bez względu na kolejność.
     */
    static void coinChangeWaysCount() {
        section("4. Wydawanie reszty — liczba sposobów (coin change, count ways)");

        int[] polishCoins = {1, 2, 5};
        show("liczba sposobów na 11 zł monetami 1/2/5", waysToMakeAmount(11, polishCoins));
        // WYNIK: liczba sposobów na 11 zł monetami 1/2/5 → 11

        note("11 różnych zestawów monet 1/2/5 sumuje się do 11 zł — sprawdź sam na kartce dla mniejszej kwoty, np. 5 zł");
    }

    // =================================================================================================
    // 5. NAJDŁUŻSZY WSPÓLNY PODCIĄG (LONGEST COMMON SUBSEQUENCE, LCS)
    // =================================================================================================

    /** lcsTable = dp[i][j] = długość LCS pierwszych i znaków a i pierwszych j znaków b. */
    static int[][] lcsTable(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1; // znaki się zgadzają — wydłuż wspólny podciąg o 1
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]); // nie zgadzają się — weź lepszy z dwóch wariantów
                }
            }
        }
        return dp;
    }

    /**
     * 5. Podciąg (subsequence) NIE musi być ciągły — tylko zachowywać kolejność. dp[i][j] rośnie po przekątnej,
     * gdy znaki pasują, inaczej "dziedziczy" lepszy wynik z lewej albo z góry. (PYTANIE REKRUTACYJNE: LCS —
     * klasyka, baza algorytmu "diff" porównującego pliki/wersje tekstu).
     */
    static void longestCommonSubsequence() {
        section("5. Najdłuższy wspólny podciąg (longest common subsequence, LCS)");

        String a = "ABCBDAB";
        String b = "BDCABA";
        int[][] dp = lcsTable(a, b);
        show("a", a);
        show("b", b);
        for (int[] row : dp) {
            System.out.println("   " + Arrays.toString(row));
        }
        show("długość LCS", dp[a.length()][b.length()]);
        // WYNIK: a → ABCBDAB
        // WYNIK: b → BDCABA
        // WYNIK:    [0, 0, 0, 0, 0, 0, 0]
        // WYNIK:    [0, 0, 0, 0, 1, 1, 1]
        // WYNIK:    [0, 1, 1, 1, 1, 2, 2]
        // WYNIK:    [0, 1, 1, 2, 2, 2, 2]
        // WYNIK:    [0, 1, 1, 2, 2, 3, 3]
        // WYNIK:    [0, 1, 2, 2, 2, 3, 3]
        // WYNIK:    [0, 1, 2, 2, 3, 3, 4]
        // WYNIK:    [0, 1, 2, 2, 3, 4, 4]
        // WYNIK: długość LCS → 4

        note("prawy dolny róg tabeli (4) to odpowiedź — np. \"BCBA\" albo \"BDAB\" to wspólne podciągi długości 4");
    }

    // =================================================================================================
    // 6. PROBLEM PLECAKOWY 0/1 (0/1 KNAPSACK)
    // =================================================================================================

    /** knapsack01 = dla każdego przedmiotu: wziąć go (jeśli się mieści) albo nie — maksymalizujemy WARTOŚĆ. */
    static int knapsack01(int[] weights, int[] values, int capacity) {
        int n = weights.length;
        int[][] dp = new int[n + 1][capacity + 1];
        for (int i = 1; i <= n; i++) {
            for (int w = 0; w <= capacity; w++) {
                dp[i][w] = dp[i - 1][w]; // nie bierzemy przedmiotu i
                if (weights[i - 1] <= w) {
                    dp[i][w] = Math.max(dp[i][w], dp[i - 1][w - weights[i - 1]] + values[i - 1]); // bierzemy go
                }
            }
        }
        return dp[n][capacity];
    }

    /**
     * 6. "0/1" = każdy przedmiot bierzemy CAŁY albo wcale (w odróżnieniu od plecaka ułamkowego). Stan to
     * para (który przedmiot rozważamy, ile miejsca zostało) — stąd dwuwymiarowa tabela.
     * (Klasyczne PYTANIE REKRUTACYJNE: problem plecakowy 0/1).
     */
    static void knapsack01Demo() {
        section("6. Problem plecakowy 0/1 (0/1 knapsack)");

        int[] weights = {1, 3, 4, 5};
        int[] values = {1, 4, 5, 7};
        int capacity = 7;
        show("wagi", weights);
        show("wartości", values);
        show("pojemność plecaka", capacity);
        show("maksymalna wartość", knapsack01(weights, values, capacity));
        // WYNIK: wagi → [1, 3, 4, 5]
        // WYNIK: wartości → [1, 4, 5, 7]
        // WYNIK: pojemność plecaka → 7
        // WYNIK: maksymalna wartość → 9

        note("9 = 4+5: przedmioty o wagach 3 i 4 (razem 7, dokładnie tyle co pojemność) dają więcej niż jakikolwiek inny wybór");
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • DP = nakładające się podproblemy + optymalna podstruktura. Rozpoznanie: podział na podobne
     *     mniejsze problemy, które się POWTARZAJĄ w drzewie rekurencji.
     *   • Top-down (memoizacja): rekurencja + tablica "już policzone"; łatwo przerobić z wersji naiwnej.
     *   • Bottom-up (tabelowanie): pętla budująca wyniki od najmniejszego podproblemu; bez rekurencji.
     *   • Często da się zredukować tabelę do O(1) pamięci, licząc tylko ostatnie potrzebne wyniki.
     *   • Coin change: moneta w pętli ZEWNĘTRZNEJ przy liczeniu SPOSOBÓW (nie permutacji).
     *   • LCS: tabela 2D, przekątna gdy znaki pasują, max(lewo, góra) gdy nie. Knapsack 0/1: wziąć/nie wziąć.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się "nakładające się podproblemy" od zwykłej rekurencji (jak w Methods03Recursion)?
     *   2. Co wypisze:  System.out.println(java.util.Arrays.toString(climbStairsTable(4)));  ?
     *   3. ZNAJDŹ BŁĄD: w waysToMakeAmount ktoś zamienia kolejność pętli (moneta WEWNĄTRZ, kwota NA ZEWNĄTRZ).
     *      Co się zepsuje?
     *   4. Dlaczego warto zacząć projektowanie rozwiązania DP od wersji top-down (memoizacja), a nie od razu
     *      od bottom-up (tabelowanie)?
     *   5. W LCS: co oznacza wartość dp[i][j], gdy ostatnie znaki a.charAt(i-1) i b.charAt(j-1) się NIE zgadzają?
     *   6. ZNAJDŹ BŁĄD: ktoś próbuje rozwiązać problem plecakowy 0/1 zachłannie (bierze najpierw przedmioty
     *      o najwyższej wartości na jednostkę wagi). Dlaczego to nie zawsze daje optymalny wynik?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: fib(10) bottom-up", 55L, () -> exercise1());
        Check.equal("ćw. 2: liczba sposobów wejścia na 6 schodków", 13, () -> exercise2());
        Check.equal("ćw. 3: minimalna liczba monet (1,2,5) na 13 zł", 4, () -> exercise3());
        Check.equal("ćw. 4: długość LCS(\"AGGTAB\", \"GXTXAYB\")", 4, () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 55L, () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", 13, () -> solution2());
        Check.equal("ćw. 3 (wzorzec)", 4, () -> solution3());
        Check.equal("ćw. 4 (wzorzec)", 4, () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz fib(10) metodą bottom-up (tabela).
     * Podpowiedź: wywołaj fibTableBottomUp(10, new long[1]).
     */
    static long exercise1() {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (łatwe): policz liczbę sposobów wejścia na 6 schodków (po 1 lub 2 stopnie naraz).
     * Podpowiedź: wywołaj climbStairsTable(6)[6].
     */
    static int exercise2() {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): policz minimalną liczbę monet (1, 2, 5) potrzebną na 13 zł.
     * Podpowiedź: wywołaj minCoins(13, new int[]{1, 2, 5}).
     */
    static int exercise3() {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): policz długość LCS napisów "AGGTAB" i "GXTXAYB" (klasyczny przykład, LCS = "GTAB").
     * Podpowiedź: wywołaj lcsTable i odczytaj prawy dolny róg tabeli.
     */
    static int exercise4() {
        // TODO: twoje rozwiązanie
        return -1;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static long solution1() {
        return fibTableBottomUp(10, new long[1]);
    }

    static int solution2() {
        return climbStairsTable(6)[6];
    }

    static int solution3() {
        return minCoins(13, new int[] {1, 2, 5});
    }

    static int solution4() {
        String a = "AGGTAB", b = "GXTXAYB";
        int[][] dp = lcsTable(a, b);
        return dp[a.length()][b.length()];
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Zwykła rekurencja też dzieli problem na mniejsze, ale w Fibonaccim te SAME mniejsze podproblemy
     *      (np. fib(18)) pojawiają się WIELOKROTNIE w różnych gałęziach drzewa wywołań — to właśnie nazywamy
     *      nakładaniem się podproblemów, i to ono uzasadnia zapamiętywanie wyników.
     *   2. [1, 1, 2, 3, 5] (dp[0..4], ten sam wzór co Fibonacci).
     *   3. Dla każdej monety z osobna liczylibyśmy wszystkie UPORZĄDKOWANE sposoby jej użycia na każdej
     *      pozycji — zestaw {1, 2} i {2, 1} zostałyby policzone jako dwa różne sposoby zamiast jednego.
     *   4. Top-down jest łatwiejsze do wyprowadzenia wprost z wersji naiwnej (dopisujesz tylko sprawdzenie
     *      memo na wejściu) — bottom-up wymaga z góry wymyślenia kolejności wypełniania tabeli, co bywa trudniejsze.
     *   5. Oznacza to, że nie da się wydłużyć wspólnego podciągu o te konkretne znaki — bierzemy lepszy
     *      wynik z pominięcia ostatniego znaku a (dp[i-1][j]) albo ostatniego znaku b (dp[i][j-1]).
     *   6. Zachłanne wybieranie "najlepszego teraz" może zapełnić plecak przedmiotami, które razem NIE
     *      wykorzystują pojemności optymalnie — DP sprawdza WSZYSTKIE kombinacje wziąć/nie wziąć, zachłanność tylko jedną ścieżkę.
     */
    // </editor-fold>
}
