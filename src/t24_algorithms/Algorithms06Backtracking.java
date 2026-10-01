package t24_algorithms;

import java.util.ArrayList;
import java.util.List;

import helpers.Check;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Nawracanie (backtracking) — wybieraj, eksploruj, cofaj
 *        (backtracking = nawracanie; choose = wybierz; explore = eksploruj; un-choose = cofnij wybór; prune = przytnij)
 *
 * W SKRÓCIE:
 *   Backtracking to systematyczne przeszukiwanie WSZYSTKICH możliwości przez budowanie rozwiązania krok po
 *   kroku: w każdym kroku PRÓBUJESZ wyboru, ECHODZISZ głębiej, a gdy droga się nie uda (albo zbadasz ją
 *   całą) — COFASZ wybór i próbujesz następnego. To rekurencja (Methods03Recursion) ze "sprzątaniem" po sobie.
 *
 * ANALOGIA: zwiedzanie labiryntu z kredą.
 *   Idziesz korytarzem, rysując kredą strzałkę. Trafiasz na ślepy zaułek — wracasz do ostatniej rozwidlonej
 *   ścieżki (cofasz wybór) i próbujesz innego korytarza. Kreda (stan) zawsze wraca do tego, co było PRZED wyborem.
 *
 * JAK TO DZIAŁA:
 *   Szablon:  void backtrack(stan) {
 *       if (stan kompletny) { zapisz rozwiązanie; return; }
 *       for (kandydat : możliwe wybory) {
 *           wybierz(kandydat);        // zmień stan
 *           backtrack(nowy stan);     // eksploruj głębiej
 *           cofnij(kandydat);         // KLUCZOWE: przywróć stan sprzed wyboru
 *       }
 *   }
 *   Przycinanie (pruning): jeśli kandydat NA PEWNO nie da poprawnego rozwiązania, pomiń go od razu —
 *   nie trzeba schodzić głębiej, żeby to sprawdzić.
 *
 * SŁÓWKA:
 *   backtracking = nawracanie; choose/un-choose = wybierz/cofnij wybór; state space = przestrzeń stanów;
 *   prune/pruning = przytnij/przycinanie (gałęzi bez sensu); permutation = permutacja; combination = kombinacja;
 *   subset = podzbiór; constraint = ograniczenie (warunek, który musi być spełniony).
 *
 * ZOBACZ TEŻ: t05_methods/Methods03Recursion (rekurencja, przypadek bazowy),
 *             t24_algorithms/Algorithms07DynamicProgramming (gdy te same stany się powtarzają — memoizacja),
 *             t11_generics/Generics03Methods (generyczne {@code List<List<Integer>>} w wynikach).
 * </pre>
 */
public class Algorithms06Backtracking {

    public static void main(String[] args) {
        title("Algorithms06 — nawracanie (backtracking)");

        subsetsDemo();       // subsets = podzbiory
        permutationsDemo();  // permutations = permutacje
        combinationsDemo();  // combinations = kombinacje
        nQueensDemo();       // N-Queens = problem N hetmanów
        wordSearchDemo();    // word search = wyszukiwanie słowa w siatce
        pruningDemo();       // pruning = przycinanie
        exercises();         // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. SZABLON NAWRACANIA — WSZYSTKIE PODZBIORY (SUBSETS)
    // =================================================================================================

    static void backtrackSubsets(int[] nums, int index, List<Integer> current, List<List<Integer>> result) {
        if (index == nums.length) { result.add(new ArrayList<>(current)); return; } // stan kompletny — zapisz
        current.add(nums[index]);                       // WYBIERZ: dołącz element
        backtrackSubsets(nums, index + 1, current, result); // EKSPLORUJ z elementem
        current.remove(current.size() - 1);              // COFNIJ WYBÓR
        backtrackSubsets(nums, index + 1, current, result); // EKSPLORUJ bez elementu
    }

    /**
     * 1. Dla każdego elementu są DWA wybory: wziąć go albo nie — stąd 2^n podzbiorów (Algorithms01Complexity).
     * Ślad dla [1] (n=1): index=0 → WYBIERZ 1 → index=1 (koniec, zapisz [1]) → COFNIJ → index=1 (koniec, zapisz []).
     * Wynik: [[1], []] — dokładnie 2 = 2^1 podzbiory. (PYTANIE REKRUTACYJNE: wypisz wszystkie podzbiory zbioru).
     */
    static void subsetsDemo() {
        section("1. Szablon nawracania — wszystkie podzbiory (subsets)");

        List<List<Integer>> result = new ArrayList<>();
        backtrackSubsets(new int[] {1, 2, 3}, 0, new ArrayList<>(), result);
        show("liczba podzbiorów {1,2,3}", result.size());
        showEach("wszystkie podzbiory", result);
        // WYNIK: liczba podzbiorów {1,2,3} → 8
        // WYNIK: wszystkie podzbiory (liczba elementów: 8):
        // WYNIK:    • [1, 2, 3]
        // WYNIK:    • [1, 2]
        // WYNIK:    • [1, 3]
        // WYNIK:    • [1]
        // WYNIK:    • [2, 3]
        // WYNIK:    • [2]
        // WYNIK:    • [3]
        // WYNIK:    • []

        note("8 = 2^3 — każdy z 3 elementów ma 2 wybory (wziąć/nie wziąć), niezależnie od pozostałych");
    }

    // =================================================================================================
    // 2. PERMUTACJE (PERMUTATIONS)
    // =================================================================================================

    static void backtrackPermutations(char[] chars, boolean[] used, StringBuilder current, List<String> result) {
        if (current.length() == chars.length) { result.add(current.toString()); return; }
        for (int i = 0; i < chars.length; i++) {
            if (used[i]) continue; // ten znak jest już w bieżącej ścieżce — pomiń
            used[i] = true;               // WYBIERZ
            current.append(chars[i]);
            backtrackPermutations(chars, used, current, result); // EKSPLORUJ
            current.deleteCharAt(current.length() - 1);          // COFNIJ WYBÓR
            used[i] = false;
        }
    }

    /**
     * 2. Permutacja = inna KOLEJNOŚĆ tych samych elementów. Dla n elementów: n! permutacji (n wyborów na
     * pierwsze miejsce, n-1 na drugie, itd). (PYTANIE REKRUTACYJNE: wygeneruj wszystkie permutacje napisu).
     */
    static void permutationsDemo() {
        section("2. Permutacje (permutations)");

        List<String> result = new ArrayList<>();
        backtrackPermutations("ABC".toCharArray(), new boolean[3], new StringBuilder(), result);
        show("liczba permutacji \"ABC\"", result.size());
        showEach("wszystkie permutacje", result);
        // WYNIK: liczba permutacji "ABC" → 6
        // WYNIK: wszystkie permutacje (liczba elementów: 6):
        // WYNIK:    • ABC
        // WYNIK:    • ACB
        // WYNIK:    • BAC
        // WYNIK:    • BCA
        // WYNIK:    • CAB
        // WYNIK:    • CBA

        note("6 = 3! — tablica \"used\" pilnuje, by nie użyć tej samej litery dwa razy w JEDNEJ ścieżce");
    }

    // =================================================================================================
    // 3. KOMBINACJE K ELEMENTÓW (COMBINATIONS)
    // =================================================================================================

    static void backtrackCombinations(int[] nums, int start, int k, List<Integer> current, List<List<Integer>> result) {
        if (current.size() == k) { result.add(new ArrayList<>(current)); return; }
        for (int i = start; i < nums.length; i++) {
            current.add(nums[i]);                                    // WYBIERZ
            backtrackCombinations(nums, i + 1, k, current, result);   // EKSPLORUJ (start = i+1, bez powtórzeń i bez cofania się)
            current.remove(current.size() - 1);                      // COFNIJ WYBÓR
        }
    }

    /**
     * 3. Kombinacja (w odróżnieniu od permutacji) NIE rozróżnia kolejności — [1,2] i [2,1] to ta sama
     * kombinacja. Parametr "start" gwarantuje, że wybieramy elementy TYLKO w rosnącej kolejności indeksów.
     */
    static void combinationsDemo() {
        section("3. Kombinacje k elementów (combinations)");

        List<List<Integer>> result = new ArrayList<>();
        backtrackCombinations(new int[] {1, 2, 3, 4}, 0, 2, new ArrayList<>(), result);
        show("liczba 2-elementowych kombinacji z {1,2,3,4}", result.size());
        showEach("wszystkie kombinacje", result);
        // WYNIK: liczba 2-elementowych kombinacji z {1,2,3,4} → 6
        // WYNIK: wszystkie kombinacje (liczba elementów: 6):
        // WYNIK:    • [1, 2]
        // WYNIK:    • [1, 3]
        // WYNIK:    • [1, 4]
        // WYNIK:    • [2, 3]
        // WYNIK:    • [2, 4]
        // WYNIK:    • [3, 4]

        note("6 = C(4,2) — wzór dwumianowy z Math03Combinatorics; tu policzone przez wygenerowanie wszystkich");
    }

    // =================================================================================================
    // 4. PROBLEM N HETMANÓW (N-QUEENS)
    // =================================================================================================

    static boolean isSafe(int[] cols, int row, int col) {
        for (int prevRow = 0; prevRow < row; prevRow++) {
            int prevCol = cols[prevRow];
            if (prevCol == col || Math.abs(prevCol - col) == row - prevRow) return false; // ta sama kolumna albo przekątna
        }
        return true;
    }

    static int solveNQueens(int[] cols, int row, int n) {
        if (row == n) return 1; // wszystkie n hetmanów ustawione bezpiecznie — jedno rozwiązanie
        int count = 0;
        for (int col = 0; col < n; col++) {
            if (isSafe(cols, row, col)) {          // PRZYCINANIE: nie schodź w gałąź, która na pewno się nie uda
                cols[row] = col;                   // WYBIERZ
                count += solveNQueens(cols, row + 1, n); // EKSPLORUJ
                // COFNIJ WYBÓR: cols[row] zostanie po prostu nadpisane w kolejnej iteracji pętli
            }
        }
        return count;
    }

    /**
     * 4. Ustaw n hetmanów na szachownicy n×n tak, by żadne dwa się nie atakowały (ta sama linia, kolumna
     * albo przekątna). cols[row] = kolumna hetmana w danym wierszu — hetmani są więc automatycznie w RÓŻNYCH
     * wierszach, zostaje sprawdzić kolumny i przekątne. (Klasyczne PYTANIE REKRUTACYJNE: problem N hetmanów).
     */
    static void nQueensDemo() {
        section("4. Problem N hetmanów (N-Queens)");

        for (int n : new int[] {4, 5, 6}) {
            int solutions = solveNQueens(new int[n], 0, n);
            show("liczba rozwiązań dla n=" + n, solutions);
        }
        // WYNIK: liczba rozwiązań dla n=4 → 2
        // WYNIK: liczba rozwiązań dla n=5 → 10
        // WYNIK: liczba rozwiązań dla n=6 → 4

        note("n=6 ma MNIEJ rozwiązań niż n=5 — liczba rozwiązań NIE rośnie monotonicznie z n, co bywa zaskoczeniem");
    }

    // =================================================================================================
    // 5. WYSZUKIWANIE SŁOWA W SIATCE LITER (WORD SEARCH)
    // =================================================================================================

    static boolean searchFrom(char[][] grid, String word, int row, int col, int index) {
        if (index == word.length()) return true; // cały napis dopasowany
        if (row < 0 || row >= grid.length || col < 0 || col >= grid[0].length) return false;
        if (grid[row][col] != word.charAt(index)) return false;

        char original = grid[row][col];
        grid[row][col] = '#';                   // WYBIERZ: oznacz jako odwiedzone w TEJ ścieżce
        boolean found = searchFrom(grid, word, row + 1, col, index + 1)
                || searchFrom(grid, word, row - 1, col, index + 1)
                || searchFrom(grid, word, row, col + 1, index + 1)
                || searchFrom(grid, word, row, col - 1, index + 1); // EKSPLORUJ w 4 kierunkach
        grid[row][col] = original;              // COFNIJ WYBÓR: przywróć literę, żeby inne ścieżki mogły jej użyć
        return found;
    }

    static boolean wordSearch(char[][] grid, String word) {
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[0].length; col++) {
                if (searchFrom(grid, word, row, col, 0)) return true;
            }
        }
        return false;
    }

    /**
     * 5. Ten sam znak nie może być użyty DWA razy w jednej ścieżce — dlatego tymczasowo "wygaszamy" go
     * znakiem '#' na czas eksploracji i PRZYWRACAMY po powrocie (cofnięcie wyboru).
     * (PYTANIE REKRUTACYJNE: word search — znajdź słowo w siatce liter, ruchy góra/dół/lewo/prawo).
     */
    static void wordSearchDemo() {
        section("5. Wyszukiwanie słowa w siatce liter (word search)");

        char[][] grid = {
                {'A', 'B', 'C', 'E'},
                {'S', 'F', 'C', 'S'},
                {'A', 'D', 'E', 'E'},
        };
        show("czy jest \"ABCCED\"", wordSearch(grid, "ABCCED"));
        show("czy jest \"SEE\"", wordSearch(grid, "SEE"));
        show("czy jest \"ABCB\" (wymagałoby powtórzenia komórki)", wordSearch(grid, "ABCB"));
        // WYNIK: czy jest "ABCCED" → true
        // WYNIK: czy jest "SEE" → true
        // WYNIK: czy jest "ABCB" (wymagałoby powtórzenia komórki) → false

        note("ABCB nie istnieje jako ścieżka BEZ powtórzenia komórki — '#' skutecznie temu zapobiega");
    }

    // =================================================================================================
    // 6. PRZYCINANIE (PRUNING) — ILE STANÓW ODWIEDZAMY Z NIM, A ILE BEZ
    // =================================================================================================

    static long solveNQueensCounted(int[] cols, int row, int n, long[] visited) {
        visited[0]++; // odwiedzamy ten stan (częściowe ustawienie hetmanów)
        if (row == n) return 1;
        long count = 0;
        for (int col = 0; col < n; col++) {
            if (isSafe(cols, row, col)) { // bez tego warunku zeszlibyśmy w KAŻDĄ gałąź, także ślepe
                cols[row] = col;
                count += solveNQueensCounted(cols, row + 1, n, visited);
            }
        }
        return count;
    }

    /**
     * 6. "Bez przycinania" oznacza: zejść we WSZYSTKIE możliwe ustawienia kolumn w każdym wierszu, nie
     * sprawdzając po drodze, czy mają sens — to n^n liści drzewa decyzji. Z przycinaniem (isSafe PRZED
     * zejściem głębiej) odwiedzamy tylko stany, które JESZCZE mają szansę się udać.
     */
    static void pruningDemo() {
        section("6. Przycinanie (pruning) — ile stanów odwiedzamy z nim, a ile bez");

        int n = 6;
        long[] visited = {0};
        int solutions = (int) solveNQueensCounted(new int[n], 0, n, visited);
        long withoutPruning = (long) Math.pow(n, n);
        show("n=6 — rozwiązań", solutions);
        show("n=6 — odwiedzonych stanów Z przycinaniem", visited[0]);
        show("n=6 — liści drzewa BEZ przycinania (n^n)", withoutPruning);
        // WYNIK: n=6 — rozwiązań → 4
        // WYNIK: n=6 — odwiedzonych stanów Z przycinaniem → 153
        // WYNIK: n=6 — liści drzewa BEZ przycinania (n^n) → 46656

        note("153 odwiedzone stany kontra 46656 możliwych liści — przycinanie odrzuca WIĘKSZOŚĆ przestrzeni, zanim ją zbuduje");

        // DOBRA PRAKTYKA: sprawdzaj warunek (isSafe) PRZED wejściem w rekurencję, nie PO zbudowaniu całego
        //   rozwiązania — im wcześniej odrzucisz martwą gałąź, tym mniej pracy marnujesz.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Szablon: wybierz → eksploruj (rekurencja) → cofnij wybór. Zawsze w tej kolejności.
     *   • Podzbiory: 2 wybory na element → 2^n. Permutacje: n! (kolejność ma znaczenie). Kombinacje: C(n,k)
     *     (kolejność bez znaczenia, parametr "start" pilnuje rosnących indeksów).
     *   • N-Queens: stan = kolumna hetmana w każdym wierszu; przycinanie przez isSafe PRZED rekurencją.
     *   • Word search: '#' jako tymczasowy znacznik odwiedzenia, przywracany po cofnięciu wyboru.
     *   • Przycinanie: odrzuć gałąź, gdy WIADOMO, że się nie uda — bez tego przestrzeń przeszukiwania eksploduje.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego w szablonie backtrackingu "cofnięcie wyboru" jest równie ważne jak sam wybór?
     *   2. Co wypisze:  {@code List<List<Integer>> r = new ArrayList<>(); backtrackSubsets(new int[0], 0, new ArrayList<>(), r);
     *      System.out.println(r.size());}  ?
     *   3. ZNAJDŹ BŁĄD: w backtrackCombinations ktoś woła rekurencję z {@code start} zamiast {@code i + 1}.
     *      Co się popsuje?
     *   4. Dlaczego liczba rozwiązań N-Queens NIE rośnie monotonicznie wraz z n (n=6 ma mniej niż n=5)?
     *   5. Po co w word search przywracamy oryginalny znak po nieudanej (lub udanej) eksploracji, zamiast
     *      zostawić '#' na stałe?
     *   6. Czym różni się "przycinanie" od zwykłego sprawdzania poprawności na SAMYM KOŃCU (po zbudowaniu
     *      całego rozwiązania)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: liczba podzbiorów {1,2}", 4, () -> exercise1());
        Check.equal("ćw. 2: liczba permutacji \"AB\"", 2, () -> exercise2());
        Check.equal("ćw. 3: liczba rozwiązań N-Queens dla n=1", 1, () -> exercise3());
        Check.equal("ćw. 4: czy w siatce {{'X','Y'},{'Z','X'}} jest słowo \"XZXY\"", true, () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 4, () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", 2, () -> solution2());
        Check.equal("ćw. 3 (wzorzec)", 1, () -> solution3());
        Check.equal("ćw. 4 (wzorzec)", true, () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz liczbę podzbiorów zbioru {1, 2} (powinno wyjść 2^2 = 4).
     * Podpowiedź: wywołaj backtrackSubsets i zwróć result.size().
     */
    static int exercise1() {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (łatwe): policz liczbę permutacji napisu "AB" (powinno wyjść 2! = 2).
     * Podpowiedź: wywołaj backtrackPermutations i zwróć result.size().
     */
    static int exercise2() {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): policz liczbę rozwiązań N-Queens dla n=1 (jeden hetman na szachownicy 1×1 —
     * zawsze bezpieczny, bo nie ma z kim się bić).
     * Podpowiedź: wywołaj solveNQueens(new int[1], 0, 1).
     */
    static int exercise3() {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): sprawdź, czy w siatce {{'X','Y'},{'Z','X'}} istnieje ścieżka tworząca
     * słowo "XZXY" (bez powtarzania komórki): (0,0)→(1,0)→(1,1)→(0,1).
     * Podpowiedź: wywołaj wordSearch z podaną siatką i słowem.
     */
    static boolean exercise4() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1() {
        List<List<Integer>> result = new ArrayList<>();
        backtrackSubsets(new int[] {1, 2}, 0, new ArrayList<>(), result);
        return result.size();
    }

    static int solution2() {
        List<String> result = new ArrayList<>();
        backtrackPermutations("AB".toCharArray(), new boolean[2], new StringBuilder(), result);
        return result.size();
    }

    static int solution3() {
        return solveNQueens(new int[1], 0, 1);
    }

    static boolean solution4() {
        char[][] grid = {{'X', 'Y'}, {'Z', 'X'}};
        return wordSearch(grid, "XZXY");
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo bez cofnięcia stan "wyciekałby" do kolejnych gałęzi rekurencji — zamiast badać NASTĘPNY wariant
     *      od czystego stanu, badalibyśmy go z resztkami poprzedniego wyboru, dając błędne wyniki.
     *   2. 1 — pusty zbiór wejściowy ma dokładnie jeden podzbiór: zbiór pusty (2^0 = 1).
     *   3. {@code start} zamiast {@code i + 1} pozwoliłoby ponownie wybrać TEN SAM element (albo elementy
     *      z mniejszym indeksem) — dałoby to powtórzenia i znacznie więcej niż C(n,k) wyników.
     *   4. Liczba rozwiązań zależy od tego, jak przekątne "kolidują" ze sobą dla danego n — to własność
     *      kombinatoryczna problemu, nie ma prostego wzoru rosnącego z n (ciąg OEIS A000170).
     *   5. Gdyby '#' zostało na stałe, komórka byłaby "spalona" na zawsze — kolejne, INNE ścieżki (np.
     *      zaczynające się gdzie indziej) nie mogłyby już jej użyć, mimo że są od siebie niezależne.
     *   6. Sprawdzanie na końcu MARNUJE czas na budowanie całych, z góry skazanych na niepowodzenie gałęzi.
     *      Przycinanie odrzuca je WCZEŚNIEJ, więc nigdy nie schodzimy głębiej niż ma to sens.
     */
    // </editor-fold>
}
