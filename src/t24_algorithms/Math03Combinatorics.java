package t24_algorithms;

import helpers.Check;

import java.util.ArrayList;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Kombinatoryka — silnia, permutacje, wariacje, kombinacje, trójkąt Pascala
 *        (combinatorics = kombinatoryka; factorial = silnia; permutation = permutacja; variation =
 *        wariacja; combination = kombinacja)
 *
 * W SKRÓCIE:
 *   Kombinatoryka liczy, NA ILE SPOSOBÓW można coś ułożyć albo wybrać — bez wypisywania wszystkich
 *   możliwości. To podstawa rachunku prawdopodobieństwa (szanse w Lotto), analizy złożoności algorytmów
 *   (ile jest permutacji do przejrzenia) i klasycznych zadań rekrutacyjnych (generuj wszystkie...).
 *
 * ANALOGIA: masz 3 koszulki i chcesz je ułożyć w rzędzie na półce — PERMUTACJA pyta "w ilu kolejnościach?"
 *   (3! = 6). Masz 5 koszulek i wybierasz 2 do plecaka, bez dbania o kolejność — KOMBINACJA pyta
 *   "ile różnych par?" (C(5,2) = 10). Gdyby kolejność wkładania do plecaka się liczyła — to WARIACJA.
 *
 * JAK TO DZIAŁA:
 *   Silnia:            n! = 1 × 2 × ... × n                               (permutacje n różnych elementów)
 *   Wariacja bez powt.: n! / (n-k)! = n × (n-1) × ... × (n-k+1)            (kolejność WAŻNA, bez powtórzeń)
 *   Wariacja z powt.:   n^k                                                (kolejność WAŻNA, powtórzenia OK)
 *   Kombinacja:         C(n,k) = n! / (k! × (n-k)!)                        (kolejność NIEWAŻNA, bez powtórzeń)
 *   Wzór mnożeniowy (bez przepełnienia): C(n,k) liczymy KROK PO KROKU, mnożąc i dzieląc na przemian —
 *     wynik pośredni jest zawsze liczbą całkowitą, więc nigdy nie trzeba liczyć ogromnej silni n!.
 *   Podzbiory:          zbiór n-elementowy ma dokładnie 2^n podzbiorów (każdy element: w podzbiorze albo nie)
 *
 * SŁÓWKA:
 *   factorial = silnia; permutation = permutacja (ustawienie w kolejności); variation = wariacja (wybór
 *   z uwzględnieniem kolejności); combination = kombinacja (wybór bez kolejności); with/without repetition
 *   = z powtórzeniami / bez powtórzeń; backtracking = metoda z nawrotami (cofanie się po ślepej uliczce);
 *   bitmask = maska bitowa; subset = podzbiór; multiset = multizbiór (zbiór z powtarzającymi się
 *   elementami); Pascal's triangle = trójkąt Pascala; odds = szanse.
 *
 * ZOBACZ TEŻ: t24_algorithms/Math01NumberTheory (NWD/NWW, wcześniejsza lekcja), t24_algorithms/
 *             Math08BigNumbers (silnia dużych liczb przez BigInteger — tu long przepełnia się już przy 21!),
 *             t24_algorithms/Algorithms06Backtracking (nawracanie jako technika algorytmiczna, szerzej),
 *             t15_numbers/Numbers05IntegerTricks (przepełnienie int/long).
 * </pre>
 */
public class Math03Combinatorics {

    public static void main(String[] args) {
        title("Math03 — kombinatoryka: silnia, permutacje, wariacje, kombinacje");

        factorialBasics();             // factorial basics = podstawy silni
        permutationsAndVariations();   // permutations and variations = permutacje i wariacje
        combinationsFormula();         // combinations formula = wzór na kombinacje
        pascalTriangle();              // Pascal triangle = trójkąt Pascala
        generateAllPermutations();     // generate all permutations = generowanie wszystkich permutacji
        generateAllCombinations();     // generate all combinations = generowanie wszystkich kombinacji
        subsetsViaBitmask();           // subsets via bitmask = podzbiory przez maskę bitową
        lotteryOdds();                 // lottery odds = szanse w Lotto
        exercises();                   // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. SILNIA (n!)
    // =================================================================================================

    /**
     * 1. {@code n!} to liczba sposobów ustawienia n RÓŻNYCH elementów w rzędzie. Rośnie oszałamiająco
     * szybko: 10! to ponad 3,6 miliona, a 20! to już ponad trylion. long przepełnia się już przy 21!.
     */
    static void factorialBasics() {
        section("1. Silnia (n!)");

        show("factorialIterative(0)", factorialIterative(0));
        show("factorialIterative(5)", factorialIterative(5));
        show("factorialIterative(10)", factorialIterative(10));
        show("factorialIterative(20)", factorialIterative(20));
        // WYNIK: factorialIterative(0) → 1
        // WYNIK: factorialIterative(5) → 120
        // WYNIK: factorialIterative(10) → 3628800
        // WYNIK: factorialIterative(20) → 2432902008176640000

        show("factorialRecursive(5)", factorialRecursive(5));
        show("factorialIterative(5) == factorialRecursive(5)?", factorialIterative(5) == factorialRecursive(5));
        // WYNIK: factorialRecursive(5) → 120
        // WYNIK: factorialIterative(5) == factorialRecursive(5)? → true

        // PUŁAPKA: 21! ≈ 5,1 × 10^19 — to WIĘCEJ niż Long.MAX_VALUE (≈9,22 × 10^18). Wynik "zawija się"
        //   po cichu (patrz t15_numbers/Numbers05IntegerTricks), bez żadnego wyjątku.
        show("factorialIterative(21) — PRZEPEŁNIENIE long!", factorialIterative(21));
        // WYNIK: factorialIterative(21) — PRZEPEŁNIENIE long! → -4249290049419214848

        // DOBRA PRAKTYKA: dla n > 20 licz silnię przez BigInteger (t24_algorithms/Math08BigNumbers) —
        //   tam nie ma górnego limitu rozmiaru liczby.
    }

    /** factorialIterative = n!, pętlą (szybsze, nie zużywa stosu). */
    static long factorialIterative(int n) {
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    /** factorialRecursive = n!, rekurencyjnie (n! = n × (n-1)!, z warunkiem brzegowym 0! = 1! = 1). */
    static long factorialRecursive(int n) {
        if (n <= 1) return 1;
        return n * factorialRecursive(n - 1);
    }

    // =================================================================================================
    // 2. PERMUTACJE I WARIACJE
    // =================================================================================================

    /**
     * 2. Permutacja n elementów = n! (wszystkie ustawione, kolejność ważna). Wariacja BEZ powtórzeń
     * wybiera i ustawia k spośród n (kolejność ważna): {@code n × (n-1) × ... × (n-k+1)}. Wariacja Z
     * powtórzeniami (np. kod PIN — te same cyfry mogą się powtarzać): {@code n^k}.
     */
    static void permutationsAndVariations() {
        section("2. Permutacje i wariacje");

        show("permutacje 5 książek na półce (5!)", factorialIterative(5));
        show("wariacja bez powt.: 3 z 5 książek w kolejności", variationsNoRepetition(5, 3));
        show("wariacja z powt.: 4-cyfrowy PIN (cyfry 0-9)", variationsWithRepetition(10, 4));
        // WYNIK: permutacje 5 książek na półce (5!) → 120
        // WYNIK: wariacja bez powt.: 3 z 5 książek w kolejności → 60
        // WYNIK: wariacja z powt.: 4-cyfrowy PIN (cyfry 0-9) → 10000

        // PUŁAPKA: "ile jest możliwych PIN-ów" to WARIACJA Z powtórzeniami (ta sama cyfra może wystąpić
        //   kilka razy), a "na ile sposobów rozdać 3 różne nagrody wśród 5 osób" to wariacja BEZ powtórzeń
        //   (jedna osoba nie dostanie dwóch nagród na raz) — łatwo je pomylić.
    }

    /** variationsNoRepetition = liczba sposobów wyboru I USTAWIENIA k z n elementów (bez powtórzeń). */
    static long variationsNoRepetition(int n, int k) {
        long result = 1;
        for (int i = 0; i < k; i++) {
            result *= (n - i);
        }
        return result;
    }

    /** variationsWithRepetition = liczba k-elementowych ciągów z n możliwości, z powtórzeniami (n^k). */
    static long variationsWithRepetition(long n, int k) {
        long result = 1;
        for (int i = 0; i < k; i++) {
            result *= n;
        }
        return result;
    }

    // =================================================================================================
    // 3. KOMBINACJE C(n, k) — WZÓR MNOŻENIOWY BEZ PRZEPEŁNIENIA
    // =================================================================================================

    /**
     * 3. {@code C(n,k) = n! / (k! × (n-k)!)} — ale liczenie PEŁNYCH silni dla dużych n (np. 49!) szybko
     * przepełnia nawet long. Trik: mnożymy i dzielimy NA PRZEMIAN, {@code result = result * (n-i) / (i+1)}
     * — po każdym kroku wynik jest ZAWSZE liczbą całkowitą (to współczynnik dwumianowy policzony
     * przyrostowo), więc nigdy nie trzeba trzymać olbrzymiej silni. PYTANIE REKRUTACYJNE: "policz C(n,k)
     * bez przepełnienia" to klasyczne pytanie o kombinatorykę i arytmetykę całkowitą.
     */
    static void combinationsFormula() {
        section("3. Kombinacje C(n, k) — wzór mnożeniowy bez przepełnienia");

        show("C(5, 2)", combinations(5, 2));
        show("C(6, 3)", combinations(6, 3));
        show("C(49, 6)", combinations(49, 6));
        show("C(5, 0)", combinations(5, 0));
        show("C(5, 5)", combinations(5, 5));
        // WYNIK: C(5, 2) → 10
        // WYNIK: C(6, 3) → 20
        // WYNIK: C(49, 6) → 13983816
        // WYNIK: C(5, 0) → 1
        // WYNIK: C(5, 5) → 1

        // DOBRA PRAKTYKA: k = Math.min(k, n - k) WYKORZYSTUJE symetrię C(n,k) = C(n,n-k) — dla C(49,6)
        //   liczymy 6 kroków zamiast 43, mniej mnożeń i mniejsze ryzyko przepełnienia.
    }

    /** combinations = C(n, k), wzorem mnożeniowym (bez liczenia pełnych silni). */
    static long combinations(int n, int k) {
        if (k < 0 || k > n) return 0;
        k = Math.min(k, n - k);                                       // symetria: C(n,k) == C(n, n-k)
        long result = 1;
        for (int i = 0; i < k; i++) {
            result = result * (n - i) / (i + 1);
        }
        return result;
    }

    // =================================================================================================
    // 4. TRÓJKĄT PASCALA (WIERSZE 0-6)
    // =================================================================================================

    /**
     * 4. Trójkąt Pascala: wiersz n zawiera wartości {@code C(n,0), C(n,1), ..., C(n,n)}. Każda liczba to
     * suma dwóch powyżej (poza brzegami, zawsze równymi 1) — to inny, rekurencyjny sposób liczenia tych
     * samych współczynników dwumianowych.
     */
    static void pascalTriangle() {
        section("4. Trójkąt Pascala (wiersze 0-6)");

        for (int row = 0; row <= 6; row++) {
            show("wiersz " + row, pascalRow(row));
        }
        // WYNIK: wiersz 0 → 1
        // WYNIK: wiersz 1 → 1 1
        // WYNIK: wiersz 2 → 1 2 1
        // WYNIK: wiersz 3 → 1 3 3 1
        // WYNIK: wiersz 4 → 1 4 6 4 1
        // WYNIK: wiersz 5 → 1 5 10 10 5 1
        // WYNIK: wiersz 6 → 1 6 15 20 15 6 1
    }

    /** pascalRow = wiersz n trójkąta Pascala jako napis "C(n,0) C(n,1) ... C(n,n)". */
    static String pascalRow(int row) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k <= row; k++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(combinations(row, k));
        }
        return sb.toString();
    }

    // =================================================================================================
    // 5. GENEROWANIE WSZYSTKICH PERMUTACJI — "ABC"
    // =================================================================================================

    /**
     * 5. Żeby WYPISAĆ wszystkie permutacje (nie tylko je policzyć), używamy cofania (backtracking):
     * na każdej pozycji próbujemy po kolei każdy jeszcze nieużyty element, idziemy głębiej, a po powrocie
     * COFAMY zamianę, żeby wypróbować następną możliwość. PYTANIE REKRUTACYJNE: "wygeneruj wszystkie
     * permutacje napisu" to klasyk wśród zadań z rekurencji/cofania.
     */
    static void generateAllPermutations() {
        section("5. Generowanie wszystkich permutacji — \"ABC\"");

        List<String> permutations = permutationsOf("ABC");
        showEach("permutacje \"ABC\"", permutations);
        show("ile permutacji (3!)", permutations.size());
        // WYNIK: permutacje "ABC" (liczba elementów: 6):
        // WYNIK:    • ABC
        // WYNIK:    • ACB
        // WYNIK:    • BAC
        // WYNIK:    • BCA
        // WYNIK:    • CBA
        // WYNIK:    • CAB
        // WYNIK: ile permutacji (3!) → 6
    }

    /** permutationsOf = wszystkie permutacje znaków napisu s (metodą zamian — swap-based backtracking). */
    static List<String> permutationsOf(String s) {
        List<String> result = new ArrayList<>();
        permutationsBacktrack(s.toCharArray(), 0, result);
        return result;
    }

    static void permutationsBacktrack(char[] chars, int start, List<String> result) {
        if (start == chars.length) {
            result.add(new String(chars));
            return;
        }
        for (int i = start; i < chars.length; i++) {
            swap(chars, start, i);
            permutationsBacktrack(chars, start + 1, result);
            swap(chars, start, i);                                    // cofnięcie (backtrack) — przywróć kolejność
        }
    }

    static void swap(char[] chars, int i, int j) {
        char temp = chars[i];
        chars[i] = chars[j];
        chars[j] = temp;
    }

    // =================================================================================================
    // 6. GENEROWANIE WSZYSTKICH KOMBINACJI — 2-ELEMENTOWE Z {1,2,3,4}
    // =================================================================================================

    /**
     * 6. Kombinacje generujemy podobnie, ale pilnujemy, żeby każdy kolejny wybrany element miał WIĘKSZY
     * indeks niż poprzedni (parametr {@code start}) — to eliminuje duplikaty w innej kolejności (np.
     * [1,2] i [2,1] liczą się jako JEDNA kombinacja, nie dwie).
     */
    static void generateAllCombinations() {
        section("6. Generowanie wszystkich kombinacji — 2-elementowe z {1,2,3,4}");

        List<List<Integer>> combos = combinationsOf(List.of(1, 2, 3, 4), 2);
        showEach("2-elementowe kombinacje z {1,2,3,4}", combos);
        show("ile kombinacji (C(4,2))", combos.size());
        // WYNIK: 2-elementowe kombinacje z {1,2,3,4} (liczba elementów: 6):
        // WYNIK:    • [1, 2]
        // WYNIK:    • [1, 3]
        // WYNIK:    • [1, 4]
        // WYNIK:    • [2, 3]
        // WYNIK:    • [2, 4]
        // WYNIK:    • [3, 4]
        // WYNIK: ile kombinacji (C(4,2)) → 6
    }

    /** combinationsOf = wszystkie k-elementowe kombinacje (podzbiory) listy items, metodą cofania. */
    static List<List<Integer>> combinationsOf(List<Integer> items, int k) {
        List<List<Integer>> result = new ArrayList<>();
        combinationsBacktrack(items, k, 0, new ArrayList<>(), result);
        return result;
    }

    static void combinationsBacktrack(List<Integer> items, int k, int start,
                                       List<Integer> current, List<List<Integer>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < items.size(); i++) {
            current.add(items.get(i));
            combinationsBacktrack(items, k, i + 1, current, result);
            current.remove(current.size() - 1);                       // cofnięcie — usuń ostatnio dodany element
        }
    }

    // =================================================================================================
    // 7. PODZBIORY ZBIORU — MASKA BITOWA, 2^n
    // =================================================================================================

    /**
     * 7. Zbiór n-elementowy ma dokładnie {@code 2^n} podzbiorów (łącznie z pustym i całym zbiorem) —
     * każdy element albo JEST w podzbiorze, albo NIE. Liczby 0..2^n-1 w zapisie binarnym reprezentują
     * WSZYSTKIE takie wybory: bit i ustawiony = i-ty element należy do podzbioru.
     */
    static void subsetsViaBitmask() {
        section("7. Podzbiory zbioru — maska bitowa, 2^n");

        List<List<Integer>> subsets = allSubsetsViaBitmask(List.of(1, 2, 3));
        showEach("wszystkie podzbiory {1,2,3} (kolejność = maska bitowa 0..7)", subsets);
        show("ile podzbiorów (2^3)", subsets.size());
        // WYNIK: wszystkie podzbiory {1,2,3} (kolejność = maska bitowa 0..7) (liczba elementów: 8):
        // WYNIK:    • []
        // WYNIK:    • [1]
        // WYNIK:    • [2]
        // WYNIK:    • [1, 2]
        // WYNIK:    • [3]
        // WYNIK:    • [1, 3]
        // WYNIK:    • [2, 3]
        // WYNIK:    • [1, 2, 3]
        // WYNIK: ile podzbiorów (2^3) → 8

        // PUŁAPKA: "1 << n" dla n ≥ 31 przepełnia TYP int (maks. przesunięcie sensowne to 1 << 30) —
        //   dla większych zbiorów licz jako long: "1L << n".
    }

    /** allSubsetsViaBitmask = wszystkie podzbiory items, enumerowane maskami bitowymi 0..2^n-1. */
    static List<List<Integer>> allSubsetsViaBitmask(List<Integer> items) {
        List<List<Integer>> result = new ArrayList<>();
        int n = items.size();
        int totalSubsets = 1 << n;                                     // 2^n
        for (int mask = 0; mask < totalSubsets; mask++) {
            List<Integer> subset = new ArrayList<>();
            for (int bit = 0; bit < n; bit++) {
                if ((mask & (1 << bit)) != 0) {
                    subset.add(items.get(bit));
                }
            }
            result.add(subset);
        }
        return result;
    }

    // =================================================================================================
    // 8. SZANSE W LOTTO: C(49, 6)
    // =================================================================================================

    /**
     * 8. W Lotto losuje się 6 liczb z 49, kolejność losowania NIE ma znaczenia — to KOMBINACJA, nie
     * wariacja. Liczba możliwych wyników to {@code C(49,6)}, a szansa trafienia KONKRETNEGO zestawu to
     * 1 do tej liczby.
     */
    static void lotteryOdds() {
        section("8. Szanse w Lotto: C(49, 6)");

        long combinationsCount = combinations(49, 6);
        show("C(49, 6) — liczba możliwych wyników Lotto", combinationsCount);
        show("szansa trafienia szóstki = 1 do", combinationsCount);
        // WYNIK: C(49, 6) — liczba możliwych wyników Lotto → 13983816
        // WYNIK: szansa trafienia szóstki = 1 do → 13983816

        // Dla porównania: to jakby zgadnąć TRAFNIE jedną konkretną sekundę z ponad 161 dni z rzędu.
        // DOBRA PRAKTYKA: w kodzie liczącym szanse/statystyki zawsze pisz WPROST, czy liczysz kombinację
        //   (kolejność nieważna) czy wariację (kolejność ważna) — to najczęstsze źródło błędnych wyników.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   SILNIA:        n! = 1×2×...×n                                    — permutacje n różnych elementów
     *   WARIACJA bez powt.: n×(n-1)×...×(n-k+1) = n!/(n-k)!              — kolejność WAŻNA, bez powtórzeń
     *   WARIACJA z powt.:   n^k                                          — kolejność WAŻNA, z powtórzeniami
     *   KOMBINACJA:    C(n,k) = n!/(k!(n-k)!), licz PRZYROSTOWO: result = result*(n-i)/(i+1)
     *   SYMETRIA:      C(n,k) == C(n, n-k)
     *   TRÓJKĄT PASCALA: C(n,k) = C(n-1,k-1) + C(n-1,k)  (każda liczba = suma dwóch nad nią)
     *   PODZBIORY:     zbiór n-elementowy ma 2^n podzbiorów; maska bitowa 0..2^n-1 = wszystkie wybory
     *   GENEROWANIE:   permutacje → zamiana (swap) + cofanie; kombinacje → wybór z rosnącym indeksem start
     *   PRZEPEŁNIENIE: long starcza do 20!; dla większych n → BigInteger (Math08BigNumbers)
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego wzór mnożeniowy liczy C(n,k) jako "result = result * (n-i) / (i+1)" krok po kroku,
     *      zamiast wprost n! / (k! * (n-k)!)?
     *   2. Co wypisze:  System.out.println(combinations(5, 0) + " " + combinations(5, 5));  ?
     *   3. ZNAJDŹ BŁĄD: factorialIterative(21) zwraca jakąś dziwną, częściowo ujemną liczbę — kod pętli
     *      jest poprawny, więc co jest przyczyną?
     *   4. Czym różni się wariacja od kombinacji? Podaj przykład z życia dla każdej.
     *   5. Co wypisze:  System.out.println(1 << 3);  ? Jak to się ma do liczby podzbiorów 3-elementowego zbioru?
     *   6. ZNAJDŹ BŁĄD: ktoś liczy liczbę podzbiorów zbioru 40-elementowego jako {@code 1 << 40} w zmiennej
     *      typu int. Co się stanie i jak to naprawić?
     *   7. Dlaczego C(n, k) == C(n, n-k)? Jak to widać w trójkącie Pascala?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Uruchamia ćwiczenia: najpierw TWOJE rozwiązania (✘ dopóki nie uzupełnisz), potem wzorcowe (✔). */
    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: 0!", 1L, () -> exercise1(0));
        Check.equal("ćw. 1b: 5!", 120L, () -> exercise1(5));
        Check.equal("ćw. 1c: 10!", 3628800L, () -> exercise1(10));
        Check.equal("ćw. 2a: C(6, 3)", 20L, () -> exercise2(6, 3));
        Check.equal("ćw. 2b: C(49, 6)", 13983816L, () -> exercise2(49, 6));
        Check.equal("ćw. 2c: C(5, 0)", 1L, () -> exercise2(5, 0));
        Check.equal("ćw. 3a: podzbiory {1,2,3,4} o 2 elementach", 6L, () -> exercise3(4, 2));
        Check.equal("ćw. 3b: podzbiory {1,2,3} o 0 elementach", 1L, () -> exercise3(3, 0));
        Check.equal("ćw. 3c: podzbiory {1,2,3,4,5} o 5 elementach", 1L, () -> exercise3(5, 5));
        Check.equal("ćw. 4a: permutacje \"AAB\"", 3L, () -> exercise4("AAB"));
        Check.equal("ćw. 4b: permutacje \"AAAB\"", 4L, () -> exercise4("AAAB"));
        Check.equal("ćw. 4c: permutacje \"ABC\"", 6L, () -> exercise4("ABC"));
        Check.equal("ćw. 5a: suma C(4,0..2)", 11L, () -> exercise5(4, 2));
        Check.equal("ćw. 5b: suma C(3,0..3)", 8L, () -> exercise5(3, 3));
        Check.equal("ćw. 5c: suma C(5,0..0)", 1L, () -> exercise5(5, 0));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", 1L, () -> solution1(0));
        Check.equal("ćw. 1b (wzorzec)", 120L, () -> solution1(5));
        Check.equal("ćw. 1c (wzorzec)", 3628800L, () -> solution1(10));
        Check.equal("ćw. 2a (wzorzec)", 20L, () -> solution2(6, 3));
        Check.equal("ćw. 2b (wzorzec)", 13983816L, () -> solution2(49, 6));
        Check.equal("ćw. 2c (wzorzec)", 1L, () -> solution2(5, 0));
        Check.equal("ćw. 3a (wzorzec)", 6L, () -> solution3(4, 2));
        Check.equal("ćw. 3b (wzorzec)", 1L, () -> solution3(3, 0));
        Check.equal("ćw. 3c (wzorzec)", 1L, () -> solution3(5, 5));
        Check.equal("ćw. 4a (wzorzec)", 3L, () -> solution4("AAB"));
        Check.equal("ćw. 4b (wzorzec)", 4L, () -> solution4("AAAB"));
        Check.equal("ćw. 4c (wzorzec)", 6L, () -> solution4("ABC"));
        Check.equal("ćw. 5a (wzorzec)", 11L, () -> solution5(4, 2));
        Check.equal("ćw. 5b (wzorzec)", 8L, () -> solution5(3, 3));
        Check.equal("ćw. 5c (wzorzec)", 1L, () -> solution5(5, 0));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 15 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz n! samodzielnie (pętlą, bez wywoływania factorialIterative z sekcji 1).
     * Podpowiedź: iloczyn liczb od 2 do n; dla n == 0 lub n == 1 wynik to 1.
     */
    static long exercise1(int n) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 2 (łatwe): policz C(n, k) wzorem mnożeniowym, bez przepełnienia (bez wywoływania
     * combinations z sekcji 3).
     * Podpowiedź: {@code result = result * (n - i) / (i + 1)} dla i = 0..k-1; wykorzystaj symetrię
     * C(n,k) = C(n, n-k), żeby liczyć mniej kroków.
     */
    static long exercise2(int n, int k) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 3 (średnie) — PRZEPISZ: zamiast wzoru C(n, k), policz tę samą wartość PRZEZ ZLICZANIE
     * masek bitowych z dokładnie k ustawionymi bitami, tak jak w sekcji 7:
     * <pre>{@code
     * static long oldWay(int n, int k) {
     *     return combinations(n, k);          // gotowy wzór — ale nie widać W NIM samej idei podzbiorów
     * }
     * }</pre>
     * Podpowiedź: dla mask = 0 do {@code (1 << n) - 1}, policz Integer.bitCount(mask) i licz te maski,
     * dla których bitCount == k.
     */
    static long exercise3(int n, int k) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 4 (średnie): policz liczbę RÓŻNYCH ustawień liter słowa złożonego z powtarzających się
     * liter (np. "AAB" ma tylko 3 różne ustawienia: AAB, ABA, BAA — a nie 3! = 6, bo dwa A są nie do
     * odróżnienia). Zakładamy same wielkie litery A-Z.
     * Podpowiedź: wzór to {@code n! / (powtórzenia_litery_1! × powtórzenia_litery_2! × ...)} — policz
     * liczność każdej litery (np. tablicą int[26]), potem podziel silnię długości słowa przez iloczyn
     * silni liczności.
     */
    static long exercise4(String word) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): policz sumę {@code C(n,0) + C(n,1) + ... + C(n,k)} — czyli ile jest
     * podzbiorów zbioru n-elementowego, które mają CO NAJWYŻEJ k elementów.
     * Podpowiedź: pętla i = 0..k, sumująca combinations(n, i) (można wywołać funkcję z sekcji 3).
     */
    static long exercise5(int n, int k) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static long solution1(int n) {
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    static long solution2(int n, int k) {
        if (k < 0 || k > n) return 0;
        k = Math.min(k, n - k);
        long result = 1;
        for (int i = 0; i < k; i++) {
            result = result * (n - i) / (i + 1);
        }
        return result;
    }

    static long solution3(int n, int k) {
        long count = 0;
        int totalMasks = 1 << n;
        for (int mask = 0; mask < totalMasks; mask++) {
            if (Integer.bitCount(mask) == k) count++;
        }
        return count;
    }

    static long solution4(String word) {
        int[] counts = new int[26];
        for (char c : word.toCharArray()) {
            counts[c - 'A']++;
        }
        long numerator = solution1(word.length());
        long denominator = 1;
        for (int count : counts) {
            denominator *= solution1(count);
        }
        return numerator / denominator;
    }

    static long solution5(int n, int k) {
        long sum = 0;
        for (int i = 0; i <= k; i++) {
            sum += solution2(n, i);
        }
        return sum;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. n! / (k!(n-k)!) wymagałoby policzenia pełnej silni n! — dla n = 49 to liczba z 63 cyframi,
     *      dawno przepełniająca long. Wzór przyrostowy "result * (n-i) / (i+1)" trzyma wynik pośredni
     *      MAŁY (to zawsze kolejny współczynnik dwumianowy C(n,i+1)), więc mieści się w long znacznie dłużej.
     *   2. "1 1" — C(n, 0) i C(n, n) to zawsze 1 (jest dokładnie jeden sposób wybrać "nic" albo "wszystko").
     *   3. 21! ≈ 5,1 × 10^19 przekracza Long.MAX_VALUE (≈9,22 × 10^18) — wynik przepełnia się (zawija po
     *      cichu), mimo że sama pętla mnożąca jest logicznie poprawna. Rozwiązanie: BigInteger dla n > 20.
     *   4. Wariacja: kolejność WAŻNA (np. podium 1-2-3 miejsce z 10 zawodników — kto jest pierwszy, a kto
     *      drugi, ma znaczenie). Kombinacja: kolejność NIEWAŻNA (np. wybór 3-osobowej reprezentacji
     *      z 10 osób — nie ma "pierwszej" osoby w drużynie).
     *   5. "8" — 1 << 3 to 2^3 = 8, dokładnie tyle, ile podzbiorów ma zbiór 3-elementowy (każdy z 3
     *      elementów: w podzbiorze albo nie, 2×2×2 = 8 kombinacji wyboru).
     *   6. {@code 1 << 40} na int przelicza się modulo 32 (przesunięcie bierze tylko 5 najmłodszych bitów
     *      wykładnika, więc "1 << 40" to w praktyce "1 << 8" = 256) — kompletnie zła odpowiedź, i to BEZ
     *      żadnego ostrzeżenia kompilatora. Trzeba użyć {@code long}: {@code 1L << 40}.
     *   7. Wybranie k elementów do podzbioru to to samo, co wybranie (n-k) elementów, które ZOSTAWIAMY
     *      poza nim — każdemu wyborowi "k do środka" odpowiada dokładnie jeden wybór "n-k na zewnątrz".
     *      W trójkącie Pascala widać to jako symetrię wiersza względem środka (np. wiersz 6: 1 6 15 20 15 6 1).
     */
    // </editor-fold>
}
