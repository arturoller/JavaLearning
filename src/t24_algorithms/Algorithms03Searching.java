package t24_algorithms;

import helpers.Check;
import java.util.Arrays;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wyszukiwanie — liniowe, binarne i "binarne po odpowiedzi"
 *        (search = wyszukiwanie; bound = granica/zakres; midpoint = środek przedziału; feasible = wykonalny)
 *
 * W SKRÓCIE:
 *   Wyszukiwanie liniowe sprawdza elementy po kolei — O(n), działa na KAŻDYCH danych. Wyszukiwanie binarne
 *   odrzuca połowę przedziału w każdym kroku — O(log n), ale wymaga POSORTOWANYCH danych. Ten sam pomysł
 *   (zawężanie przedziału) działa też, gdy szukamy nie elementu w tablicy, a LICZBY spełniającej warunek —
 *   to "wyszukiwanie binarne po odpowiedzi" (binary search on the answer).
 *
 * ANALOGIA: zgadywanka "czy liczba jest większa czy mniejsza".
 *   Ktoś myśli o liczbie od 1 do 100. Zamiast zgadywać po kolei (1, 2, 3...), pytasz "czy to więcej niż 50?"
 *   — za każdym pytaniem odrzucasz połowę możliwości. 100 liczb zgadniesz w maksymalnie 7 pytaniach.
 *
 * JAK TO DZIAŁA:
 *   low i high wyznaczają PRZEDZIAŁ poszukiwań; mid = środek; porównanie z mid zawęża przedział o połowę.
 *   Warunek końca: low > high (nic nie zostało) albo trafienie. Każdy krok to log2(n) — stąd nazwa.
 *
 * SŁÓWKA:
 *   linear search = wyszukiwanie liniowe; binary search = wyszukiwanie binarne; midpoint = środek przedziału;
 *   lower bound = pierwsze wystąpienie (dolna granica); upper bound = ostatnie wystąpienie (górna granica);
 *   insertion point = miejsce wstawienia; overflow = przepełnienie; feasible = wykonalny, spełniający warunek.
 *
 * ZOBACZ TEŻ: t24_algorithms/Algorithms01Complexity (dlaczego O(log n) jest tak dobre),
 *             t24_algorithms/Algorithms02Sorting (binarne wymaga POSORTOWANYCH danych),
 *             t03_arrays/Arrays03Utility (Arrays.binarySearch w praktyce).
 * </pre>
 */
public class Algorithms03Searching {

    public static void main(String[] args) {
        title("Algorithms03 — wyszukiwanie liniowe, binarne i po odpowiedzi");

        linearSearchDemo();       // linear search = wyszukiwanie liniowe
        binarySearchIterative();  // binary search iterative = wyszukiwanie binarne iteracyjne
        overflowBugDemo();        // overflow bug = błąd przepełnienia
        binarySearchRecursive();  // binary search recursive = wyszukiwanie binarne rekurencyjne
        firstLastOccurrence();    // first/last occurrence = pierwsze/ostatnie wystąpienie
        integerSquareRoot();      // integer square root = całkowity pierwiastek kwadratowy
        minimalCapacity();        // minimal capacity = minimalna pojemność
        arraysBinarySearchFacts(); // Arrays.binarySearch facts = fakty o Arrays.binarySearch
        exercises();              // exercises = ćwiczenia
    }

    /** sortedArray = wspólna posortowana tablica (12 elementów) używana w kilku demonstracjach tej lekcji. */
    static int[] sortedArray() {
        return new int[] {2, 5, 8, 12, 16, 19, 23, 27, 31, 38, 42, 47};
    }

    // =================================================================================================
    // 1. WYSZUKIWANIE LINIOWE (LINEAR SEARCH)
    // =================================================================================================

    /** linearSearchCounted = sprawdzaj po kolei od początku — działa na KAŻDEJ tablicy, nawet nieposortowanej. */
    static int linearSearchCounted(int[] arr, int target, long[] comparisons) {
        for (int i = 0; i < arr.length; i++) {
            comparisons[0]++;
            if (arr[i] == target) {
                return i;
            }
        }
        return -1;
    }

    /** 1. Baza odniesienia: proste, działa zawsze, ale w najgorszym razie sprawdza WSZYSTKIE n elementów. */
    static void linearSearchDemo() {
        section("1. Wyszukiwanie liniowe (linear search)");

        int[] arr = sortedArray();
        long[] cmp = {0};
        int index = linearSearchCounted(arr, 23, cmp);
        show("indeks wartości 23", index);
        show("liczba porównań", cmp[0]);
        // WYNIK: indeks wartości 23 → 6
        // WYNIK: liczba porównań → 7

        cmp[0] = 0;
        linearSearchCounted(arr, 99, cmp);
        show("liczba porównań dla nieobecnej wartości 99", cmp[0]);
        // WYNIK: liczba porównań dla nieobecnej wartości 99 → 12

        note("12 elementów, 12 porównań dla nieobecnej wartości — zawsze CAŁA tablica w najgorszym razie, O(n)");
    }

    // =================================================================================================
    // 2. WYSZUKIWANIE BINARNE ITERACYJNE (BINARY SEARCH)
    // =================================================================================================

    /** binarySearchCounted = wymaga POSORTOWANEJ tablicy; porównaj ze środkiem, odrzuć połowę, powtórz. */
    static int binarySearchCounted(int[] sorted, int target, long[] comparisons) {
        int low = 0, high = sorted.length - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2; // bezpieczny środek — patrz sekcja 3
            comparisons[0]++;
            if (sorted[mid] == target) {
                return mid;
            } else if (sorted[mid] < target) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1;
    }

    /**
     * 2. Ślad dla szukania 23 w [2, 5, 8, 12, 16, 19, 23, 27, 31, 38, 42, 47] (indeksy 0..11):
     *   low=0 high=11 mid=5 → {@code tablica[5]=19 < 23} → szukaj w prawej połowie (1 porównanie)
     *   low=6 high=11 mid=8 → {@code tablica[8]=31 > 23} → szukaj w lewej połowie (2 porównanie)
     *   low=6 high=7 mid=6 → tablica[6]=23 → znaleziono (3 porównanie)
     */
    static void binarySearchIterative() {
        section("2. Wyszukiwanie binarne iteracyjne (binary search)");

        int[] sorted = sortedArray();
        long[] cmp = {0};
        int index = binarySearchCounted(sorted, 23, cmp);
        show("indeks wartości 23", index);
        show("liczba porównań", cmp[0]);
        // WYNIK: indeks wartości 23 → 6
        // WYNIK: liczba porównań → 3

        cmp[0] = 0;
        binarySearchCounted(sorted, 99, cmp);
        show("liczba porównań dla nieobecnej wartości 99", cmp[0]);
        // WYNIK: liczba porównań dla nieobecnej wartości 99 → 4

        note("4 porównania zamiast 12 — to różnica między O(log n) a O(n), i rośnie wraz z n");

        // PUŁAPKA: binarySearchCounted na NIEPOSORTOWANEJ tablicy da BŁĘDNY wynik BEZ ŻADNEGO ostrzeżenia —
        //   algorytm ufa, że dane są posortowane, i nie sprawdza tego założenia.
    }

    // =================================================================================================
    // 3. BEZPIECZNY ŚRODEK PRZEDZIAŁU I SŁYNNY BŁĄD PRZEPEŁNIENIA
    // =================================================================================================

    /**
     * 3. Naiwny wzór {@code mid = (low + high) / 2} wygląda niewinnie, ale dla DUŻYCH low i high suma
     * low + high może przekroczyć Integer.MAX_VALUE i PRZEPEŁNIĆ się (zawinąć do liczby ujemnej) — w Javie
     * int nie rzuca wyjątku przy przepełnieniu, po prostu "zawija się". Ten błąd siedział w
     * java.util.Arrays.binarySearch przez prawie 10 lat, zanim go znaleziono (Joshua Bloch, 2006).
     * Bezpieczny wzór: {@code mid = low + (high - low) / 2} — różnica (high - low) nigdy nie przepełnia się
     * w ten sposób, bo oba są nieujemne i {@code high >= low}.
     * (Klasyczne PYTANIE REKRUTACYJNE: dlaczego (low + high) / 2 jest błędne?).
     */
    static void overflowBugDemo() {
        section("3. Bezpieczny środek przedziału — słynny błąd przepełnienia");

        int low = 1, high = Integer.MAX_VALUE;
        int badMid = (low + high) / 2;              // PUŁAPKA: low + high przepełnia int!
        int safeMid = low + (high - low) / 2;        // bezpieczne: różnica się mieści
        show("low + high", low + high);
        show("(low + high) / 2 — BŁĘDNY środek", badMid);
        show("low + (high - low) / 2 — bezpieczny środek", safeMid);
        // WYNIK: low + high → -2147483648
        // WYNIK: (low + high) / 2 — BŁĘDNY środek → -1073741824
        // WYNIK: low + (high - low) / 2 — bezpieczny środek → 1073741824

        int[] tiny = {10, 20, 30};
        expectThrows("tiny[badMid] (ujemny indeks z przepełnienia)", () -> {
            int unused = tiny[badMid];
        });
        // WYNIK: ✔ tiny[badMid] (ujemny indeks z przepełnienia) → rzucono ArrayIndexOutOfBoundsException: Index -1073741824 out of bounds for length 3

        note("badMid wyszedł UJEMNY — użyty jako indeks tablicy, dałby ArrayIndexOutOfBoundsException zamiast poprawnego wyniku");

        // DOBRA PRAKTYKA: zawsze pisz {@code low + (high - low) / 2} zamiast {@code (low + high) / 2} —
        //   to jedna literka różnicy w kodzie, a ogromna różnica w poprawności dla dużych przedziałów.
    }

    // =================================================================================================
    // 4. WYSZUKIWANIE BINARNE REKURENCYJNE
    // =================================================================================================

    /** binarySearchRecursiveCounted = ta sama logika co iteracyjnie, ale przez wywołania samej siebie na połówce przedziału. */
    static int binarySearchRecursiveCounted(int[] sorted, int target, int low, int high, long[] comparisons) {
        if (low > high) {
            return -1;
        }
        int mid = low + (high - low) / 2;
        comparisons[0]++;
        if (sorted[mid] == target) {
            return mid;
        } else if (sorted[mid] < target) {
            return binarySearchRecursiveCounted(sorted, target, mid + 1, high, comparisons);
        } else {
            return binarySearchRecursiveCounted(sorted, target, low, mid - 1, comparisons);
        }
    }

    /** 4. Ta sama złożoność O(log n) co iteracyjnie — ale KAŻDE wywołanie to nowa ramka na stosie (Methods03Recursion). */
    static void binarySearchRecursive() {
        section("4. Wyszukiwanie binarne rekurencyjne");

        int[] sorted = sortedArray();
        long[] cmp = {0};
        int index = binarySearchRecursiveCounted(sorted, 42, 0, sorted.length - 1, cmp);
        show("indeks wartości 42", index);
        show("liczba porównań", cmp[0]);
        // WYNIK: indeks wartości 42 → 10
        // WYNIK: liczba porównań → 3

        note("ten sam wynik co iteracyjnie przy tych samych danych — rekurencja to inny ZAPIS tego samego algorytmu");

        // DOBRA PRAKTYKA: wersję iteracyjną wybieraj w kodzie produkcyjnym (brak ryzyka StackOverflowError
        //   dla bardzo głębokiej rekurencji) — rekurencyjna jest tu wyłącznie do nauki.
    }

    // =================================================================================================
    // 5. PIERWSZE I OSTATNIE WYSTĄPIENIE (LOWER BOUND / UPPER BOUND)
    // =================================================================================================

    /** firstOccurrence = znajdź element, ale gdy go znajdziesz — SZUKAJ DALEJ W LEWO, czy jest wcześniejsza kopia. */
    static int firstOccurrence(int[] arr, int target) {
        int low = 0, high = arr.length - 1, result = -1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] == target) {
                result = mid;
                high = mid - 1; // nie przerywaj — może być wcześniejsze wystąpienie
            } else if (arr[mid] < target) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    /** lastOccurrence = symetrycznie: po trafieniu szukaj dalej W PRAWO. */
    static int lastOccurrence(int[] arr, int target) {
        int low = 0, high = arr.length - 1, result = -1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] == target) {
                result = mid;
                low = mid + 1; // szukaj dalej w prawo
            } else if (arr[mid] < target) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    /**
     * 5. Zwykłe wyszukiwanie binarne zwraca DOWOLNE trafione wystąpienie przy duplikatach. Lower bound (pierwsze
     * wystąpienie) i upper bound (ostatnie) modyfikują regułę: po trafieniu NIE kończymy, tylko zawężamy dalej
     * w tym samym kierunku, zapamiętując ostatnie udane trafienie. (PYTANIE REKRUTACYJNE: znajdź liczbę
     * wystąpień elementu w posortowanej tablicy w O(log n)).
     */
    static void firstLastOccurrence() {
        section("5. Pierwsze i ostatnie wystąpienie (lower bound / upper bound)");

        int[] withDuplicates = {1, 2, 2, 2, 3, 4, 4, 5, 5, 5, 5, 6};
        show("tablica z duplikatami", withDuplicates);
        show("pierwsze wystąpienie 5", firstOccurrence(withDuplicates, 5));
        show("ostatnie wystąpienie 5", lastOccurrence(withDuplicates, 5));
        int count = lastOccurrence(withDuplicates, 5) - firstOccurrence(withDuplicates, 5) + 1;
        show("liczba wystąpień 5", count);
        // WYNIK: tablica z duplikatami → [1, 2, 2, 2, 3, 4, 4, 5, 5, 5, 5, 6]
        // WYNIK: pierwsze wystąpienie 5 → 7
        // WYNIK: ostatnie wystąpienie 5 → 10
        // WYNIK: liczba wystąpień 5 → 4

        note("różnica (ostatnie - pierwsze + 1) daje LICZBĘ wystąpień bez przeglądania całej tablicy — wciąż O(log n)");
    }

    // =================================================================================================
    // 6. WYSZUKIWANIE BINARNE PO ODPOWIEDZI — CAŁKOWITY PIERWIASTEK KWADRATOWY
    // =================================================================================================

    /**
     * integerSqrtCounted = szukamy NIE elementu w tablicy, a NAJWIĘKSZEJ liczby x takiej, że {@code x*x <= n}.
     * Przestrzeń odpowiedzi [0, n] jest "posortowana" wg warunku ({@code x*x <= n} jest prawdą dla małych x,
     * fałszem dla dużych) — więc binarne zawężanie działa tak samo, jak na tablicy.
     */
    static int integerSqrtCounted(int n, long[] steps) {
        int low = 0, high = n, answer = 0;
        while (low <= high) {
            steps[0]++;
            int mid = low + (high - low) / 2;
            if ((long) mid * mid <= n) { // (long) chroni przed przepełnieniem mid*mid dla dużych n
                answer = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return answer;
    }

    /** 6. Zamiast wzoru Math.sqrt (double, przybliżenie) — binarne zawężanie daje DOKŁADNY wynik całkowity. */
    static void integerSquareRoot() {
        section("6. Wyszukiwanie binarne po odpowiedzi — całkowity pierwiastek kwadratowy");

        long[] steps = {0};
        int root50 = integerSqrtCounted(50, steps);
        show("całkowity pierwiastek z 50", root50);
        show("liczba kroków", steps[0]);
        // WYNIK: całkowity pierwiastek z 50 → 7
        // WYNIK: liczba kroków → 6

        steps[0] = 0;
        int rootBig = integerSqrtCounted(1_000_000_000, steps);
        show("całkowity pierwiastek z 1 000 000 000", rootBig);
        show("liczba kroków", steps[0]);
        // WYNIK: całkowity pierwiastek z 1 000 000 000 → 31622
        // WYNIK: liczba kroków → 30

        note("miliard sprawdzony w 30 krokach zamiast miliarda prób — tak działa binarne szukanie po odpowiedzi");
    }

    // =================================================================================================
    // 7. WYSZUKIWANIE BINARNE PO ODPOWIEDZI — MINIMALNA POJEMNOŚĆ
    // =================================================================================================

    /** canShipInGroups = czy przy pojemności "capacity" da się rozdzielić paczki na NIE WIĘCEJ niż maxGroups grup. */
    static boolean canShipInGroups(int[] weights, int capacity, int maxGroups) {
        int groups = 1;
        int currentLoad = 0;
        for (int weight : weights) {
            if (currentLoad + weight > capacity) {
                groups++;
                currentLoad = 0;
            }
            currentLoad += weight;
        }
        return groups <= maxGroups;
    }

    /**
     * minimalCapacityCounted = szukamy NAJMNIEJSZEJ pojemności, przy której canShipInGroups zwraca true.
     * Przestrzeń odpowiedzi [max(waga), suma(wag)] też jest "posortowana" wg warunku: dla małej pojemności
     * trzeba WIĘCEJ grup (false), dla dużej — MNIEJ (true od pewnego momentu w górę).
     */
    static int minimalCapacityCounted(int[] weights, int maxGroups, long[] checks) {
        int low = Arrays.stream(weights).max().orElse(0);
        int high = Arrays.stream(weights).sum();
        while (low < high) {
            checks[0]++;
            int mid = low + (high - low) / 2;
            if (canShipInGroups(weights, mid, maxGroups)) {
                high = mid;      // mid wystarcza — spróbuj mniejszej pojemności
            } else {
                low = mid + 1;   // mid za mała — potrzeba więcej
            }
        }
        return low;
    }

    /**
     * 7. Klasyczny problem "minimalna pojemność": podziel paczki o danych wagach na NIE WIĘCEJ niż k grup
     * (w kolejności, bez przestawiania), minimalizując największą sumę w grupie. Zamiast sprawdzać WSZYSTKIE
     * możliwe pojemności po kolei, zawężamy binarnie — sprawdzenie jednej pojemności kosztuje O(n).
     */
    static void minimalCapacity() {
        section("7. Wyszukiwanie binarne po odpowiedzi — minimalna pojemność");

        int[] weights = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int maxGroups = 5;
        long[] checks = {0};
        int capacity = minimalCapacityCounted(weights, maxGroups, checks);
        show("paczki", weights);
        show("max liczba grup", maxGroups);
        show("minimalna pojemność grupy", capacity);
        show("liczba sprawdzonych pojemności", checks[0]);
        // WYNIK: paczki → [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
        // WYNIK: max liczba grup → 5
        // WYNIK: minimalna pojemność grupy → 15
        // WYNIK: liczba sprawdzonych pojemności → 5

        note("zamiast sprawdzać pojemności 10, 11, 12... po kolei (O(suma)), zawężamy binarnie w kilka kroków");

        // DOBRA PRAKTYKA: wzorzec "sprawdź wykonalność dla danej wartości, potem zawężaj binarnie" pasuje do
        //   wielu problemów optymalizacyjnych — nie tylko do szukania w tablicy.
    }

    // =================================================================================================
    // 8. Arrays.binarySearch — WARTOŚĆ ZWRACANA DLA BRAKUJĄCEGO ELEMENTU
    // =================================================================================================

    /**
     * 8. java.util.Arrays.binarySearch zwraca INDEKS, gdy element jest w tablicy, a gdy go NIE MA —
     * ujemną liczbę: {@code -(punkt wstawienia) - 1}. Punkt wstawienia to indeks, pod który element
     * trafiłby, gdyby go wstawić zachowując sortowanie. Dzięki temu z wyniku ujemnego można ODZYSKAĆ
     * punkt wstawienia: {@code -(wynik) - 1}.
     */
    static void arraysBinarySearchFacts() {
        section("8. Arrays.binarySearch — wartość zwracana dla brakującego elementu");

        int[] sorted = {1, 3, 5, 7, 9};
        int present = Arrays.binarySearch(sorted, 7);
        show("Arrays.binarySearch(sorted, 7) — element obecny", present);
        // WYNIK: Arrays.binarySearch(sorted, 7) — element obecny → 3

        int missing = Arrays.binarySearch(sorted, 6);
        show("Arrays.binarySearch(sorted, 6) — element nieobecny", missing);
        int insertionPoint = -missing - 1;
        show("odzyskany punkt wstawienia", insertionPoint);
        // WYNIK: Arrays.binarySearch(sorted, 6) — element nieobecny → -4
        // WYNIK: odzyskany punkt wstawienia → 3

        note("6 wstawiłoby się na indeks 3 (między 5 a 7) — stąd -4 = -(3) - 1; ujemny wynik to NIE błąd, to informacja");

        // PUŁAPKA: Arrays.binarySearch na NIEPOSORTOWANEJ tablicy ma NIEOKREŚLONE zachowanie — może zwrócić
        //   dowolny wynik, bez ostrzeżenia. Zawsze sortuj (Algorithms02Sorting) przed użyciem.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Liniowe: O(n), działa zawsze. Binarne: O(log n), wymaga POSORTOWANYCH danych.
     *   • Bezpieczny środek: {@code low + (high - low) / 2}, NIGDY {@code (low + high) / 2} (przepełnienie!).
     *   • Lower bound / upper bound: po trafieniu nie przerywaj, zawężaj dalej w tym samym kierunku.
     *   • Binarne po odpowiedzi: gdy warunek jest "monotoniczny" (raz false, potem zawsze true) — zawężaj jak w tablicy.
     *   • Arrays.binarySearch: brak elementu → {@code -(punkt wstawienia) - 1}.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego wyszukiwanie binarne wymaga posortowanej tablicy, a liniowe — nie?
     *   2. Co wypisze:  int[] a = {1, 3, 5, 7}; System.out.println(java.util.Arrays.binarySearch(a, 4));  ?
     *   3. ZNAJDŹ BŁĄD:  int mid = (low + high) / 2;  — dla jakich wartości low i high to się psuje?
     *   4. Dlaczego firstOccurrence nie przerywa pętli od razu po trafieniu elementu?
     *   5. Co wypisze:  System.out.println(Integer.MAX_VALUE + 1);  ? Dlaczego to ważne dla wyszukiwania binarnego?
     *   6. Na czym polega "wyszukiwanie binarne po odpowiedzi" i czym różni się od wyszukiwania w tablicy?
     *   7. ZNAJDŹ BŁĄD: ktoś woła binarySearchCounted na tablicy posortowanej MALEJĄCO i dziwi się błędnym wynikom.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: bezpieczny środek dla low=10, high=20", 15, () -> exercise1(10, 20));
        Check.equal("ćw. 2: liczba kroków binarnego szukania 16 w sortedArray()",
                4L, () -> exercise2(sortedArray(), 16));
        Check.equal("ćw. 3: pierwsze wystąpienie 4 w [1,2,2,2,3,4,4,5,5,5,5,6]",
                5, () -> exercise3(new int[] {1, 2, 2, 2, 3, 4, 4, 5, 5, 5, 5, 6}, 4));
        Check.equal("ćw. 4: całkowity pierwiastek z 99", 9, () -> exercise4(99));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 15, () -> solution1(10, 20));
        Check.equal("ćw. 2 (wzorzec)", 4L, () -> solution2(sortedArray(), 16));
        Check.equal("ćw. 3 (wzorzec)", 5, () -> solution3(new int[] {1, 2, 2, 2, 3, 4, 4, 5, 5, 5, 5, 6}, 4));
        Check.equal("ćw. 4 (wzorzec)", 9, () -> solution4(99));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): oblicz bezpieczny środek przedziału [low, high] wzorem {@code low + (high - low) / 2}.
     * Podpowiedź: NIE używaj (low + high) / 2 — patrz sekcja 3.
     */
    static int exercise1(int low, int high) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć liczbę kroków (porównań), jakie wykona binarySearchCounted szukając target.
     * Podpowiedź: wywołaj binarySearchCounted z własnym licznikiem i odczytaj go po wywołaniu.
     */
    static long exercise2(int[] sorted, int target) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): znajdź indeks PIERWSZEGO wystąpienia target w posortowanej tablicy z duplikatami.
     * Podpowiedź: skopiuj logikę firstOccurrence (po trafieniu zawężaj w lewo, zapamiętując wynik).
     */
    static int exercise3(int[] arr, int target) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): policz całkowity pierwiastek kwadratowy z n metodą wyszukiwania binarnego
     * po odpowiedzi (największe x takie, że {@code x*x <= n}).
     * Podpowiedź: skopiuj logikę integerSqrtCounted (możesz zignorować licznik kroków).
     */
    static int exercise4(int n) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int low, int high) {
        return low + (high - low) / 2;
    }

    static long solution2(int[] sorted, int target) {
        long[] steps = {0};
        binarySearchCounted(sorted, target, steps);
        return steps[0];
    }

    static int solution3(int[] arr, int target) {
        return firstOccurrence(arr, target);
    }

    static int solution4(int n) {
        return integerSqrtCounted(n, new long[1]);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Binarne ZAWĘŻA przedział na podstawie porównania ze środkiem — to działa tylko, gdy kolejność
     *      elementów jest znana z góry (posortowane). Liniowe sprawdza każdy element osobno, więc kolejność
     *      nie ma znaczenia.
     *   2. -3 (4 wstawiłoby się na indeks 2, między 3 a 5; wynik to -(2) - 1 = -3).
     *   3. Psuje się, gdy low + high przekracza Integer.MAX_VALUE (bardzo duże indeksy/wartości) — suma
     *      przepełnia się i "zawija" do liczby ujemnej, dając błędny (ujemny) środek.
     *   4. Bo przy duplikatach może istnieć WCZEŚNIEJSZA kopia tego samego elementu — zawężenie w lewo
     *      (high = mid - 1) sprawdza, czy taka kopia istnieje, zamiast kończyć na pierwszym trafieniu.
     *   5. Integer.MIN_VALUE (-2147483648) — to klasyczne przepełnienie int. Ważne, bo (low + high) dla
     *      dużych low/high może przepełnić się dokładnie w ten sposób, dając błędny środek przedziału.
     *   6. Szukamy nie elementu w tablicy, a WARTOŚCI spełniającej pewien monotoniczny warunek (raz fałsz,
     *      od pewnego miejsca zawsze prawda) — zawężamy przedział możliwych odpowiedzi tak samo jak w tablicy.
     *   7. binarySearchCounted zakłada sortowanie ROSNĄCO — porównania {@code sorted[mid] < target} zawężają
     *      przedział w złą stronę dla danych malejących. Trzeba albo posortować rosnąco, albo odwrócić logikę.
     */
    // </editor-fold>
}
