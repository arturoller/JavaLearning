package t24_algorithms;

import helpers.Check;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Złożoność obliczeniowa — notacja O() (Big O)
 *        (complexity = złożoność; growth rate = tempo wzrostu; asymptotic = asymptotyczny)
 *
 * W SKRÓCIE:
 *   Notacja O() opisuje, jak rośnie LICZBA OPERACJI (nie czas w sekundach!), gdy rozmiar danych n rośnie.
 *   Dzięki temu można porównywać algorytmy niezależnie od komputera, języka czy obciążenia maszyny.
 *   W tej lekcji NIE mierzymy czasu — liczymy porównania, kopiowania, wywołania (patrz PUŁAPKA niżej).
 *
 * ANALOGIA: szukanie słowa w papierowym słowniku.
 *   Kartka po kartce (liniowo, O(n)) kontra otwieranie na środku i zawężanie (binarnie, O(log n)).
 *   Przy słowniku na 10 stron różnica jest niezauważalna. Przy 100 000 stron — kolosalna.
 *
 * JAK TO DZIAŁA:
 *   Liczymy OPERACJE, bo sekundy zależą od procesora, obciążenia systemu i JIT-a (kompilacji w locie)
 *   — ten sam kod raz policzy się szybciej, raz wolniej, a liczba operacji jest zawsze taka sama.
 *   Notacja O() opisuje tempo wzrostu przy n dążącym do nieskończoności, POMIJAJĄC stałe i wyrazy niższego rzędu:
 *   {@code 3n + 5} porównań → O(n) (stała 3 i +5 nic nie zmieniają przy dużym n).
 *   {@code n*n + n} → O(n²) (wyraz n*n DOMINUJE nad n, gdy n jest duże).
 *
 * SŁÓWKA:
 *   complexity = złożoność; growth rate = tempo wzrostu; asymptotic = asymptotyczny; worst/average/best case =
 *   przypadek pesymistyczny/średni/optymistyczny; amortized = zamortyzowany; space complexity = złożoność
 *   pamięciowa; dominant term = wyraz dominujący; logarithmic = logarytmiczny; exponential = wykładniczy.
 *
 * ZOBACZ TEŻ: t24_algorithms/Algorithms02Sorting (O(n log n) w praktyce — sortowanie przez scalanie),
 *             t24_algorithms/Algorithms03Searching (O(log n) — wyszukiwanie binarne, krok po kroku),
 *             t05_methods/Methods03Recursion (naiwny Fibonacci i jego koszt).
 * </pre>
 */
public class Algorithms01Complexity {

    /** fibCalls = licznik wywołań metody fibNaive — pokazuje, jak szybko rośnie koszt rekurencji wykładniczej. */
    private static long fibCalls = 0;

    public static void main(String[] args) {
        title("Algorithms01 — złożoność obliczeniowa: notacja O()");

        constantTime();         // constant time = czas stały
        logarithmicTime();      // logarithmic time = czas logarytmiczny
        linearTime();           // linear time = czas liniowy
        linearithmicTime();     // linearithmic time = czas "n razy log n"
        quadraticTime();        // quadratic time = czas kwadratowy
        exponentialTime();      // exponential time = czas wykładniczy
        growthTable();          // growth table = tabela wzrostu
        bestAverageWorstCase(); // best/average/worst case = przypadek optymistyczny/średni/pesymistyczny
        spaceComplexity();      // space complexity = złożoność pamięciowa
        amortizedAnalysis();    // amortized analysis = analiza zamortyzowana
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. O(1) — CZAS STAŁY
    // =================================================================================================

    /** 1. Dostęp do elementu tablicy po indeksie to zawsze JEDNA operacja — niezależnie od rozmiaru tablicy. */
    static void constantTime() {
        section("1. O(1) — czas stały");

        int[] small = {10, 20, 30, 40, 50};
        int[] big = new int[1_000_000];
        big[0] = 111;
        big[big.length - 1] = 999;

        show("small[0]", small[0]);
        show("small[ostatni]", small[small.length - 1]);
        show("big[0] (tablica z milionem elementów)", big[0]);
        show("big[ostatni]", big[big.length - 1]);
        // WYNIK: small[0] → 10
        // WYNIK: small[ostatni] → 50
        // WYNIK: big[0] (tablica z milionem elementów) → 111
        // WYNIK: big[ostatni] → 999

        note("dostęp arr[i] to zawsze 1 operacja — tablica 5-elementowa czy milionowa, koszt jest ten sam → O(1)");

        // DOBRA PRAKTYKA: jeśli masz wybór, operacje O(1) (dostęp po indeksie, push/pop na stosie, dodanie na
        //   koniec ArrayList) są "darmowe" niezależnie od ilości danych — warto tak projektować struktury danych.
    }

    // =================================================================================================
    // 2. O(log n) — CZAS LOGARYTMICZNY
    // =================================================================================================

    /**
     * binarySearchCounted = wyszukiwanie binarne licznikiem porównań. Działa TYLKO na posortowanej tablicy:
     * za każdym krokiem odrzucamy połowę pozostałych elementów.
     * Ślad dla szukania 23 w {@code [2, 5, 8, 12, 16, 23, 38, 42]} (8 elementów, indeksy 0..7):
     *   low=0 high=7 mid=3 → tablica[3]=12, {@code 12 < 23} → szukaj w prawej połowie (1 porównanie)
     *   low=4 high=7 mid=5 → tablica[5]=23 → znaleziono (2 porównanie) — czyli 2 porównania zamiast 8.
     */
    static int binarySearchCounted(int[] sorted, int target, int[] comparisons) {
        int low = 0, high = sorted.length - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;   // bezpieczne liczenie środka — bez przepełnienia (Algorithms03Searching)
            comparisons[0]++;
            if (sorted[mid] == target) {
                return mid;
            } else if (sorted[mid] < target) {
                low = mid + 1;                  // szukaj w prawej połowie
            } else {
                high = mid - 1;                 // szukaj w lewej połowie
            }
        }
        return -1;
    }

    /** makeSorted = tablica posortowana rosnąco [0, 1, 2, ..., n-1] — wygodna, bo wartość == indeks. */
    static int[] makeSorted(int n) {
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) {
            arr[i] = i;
        }
        return arr;
    }

    /** 2. Wyszukiwanie binarne: podwojenie rozmiaru danych dodaje tylko JEDNO porównanie więcej. */
    static void logarithmicTime() {
        section("2. O(log n) — czas logarytmiczny");

        int[] sorted = {2, 5, 8, 12, 16, 23, 38, 42};
        int[] comparisons = {0};
        int index = binarySearchCounted(sorted, 23, comparisons);
        show("indeks wartości 23 w tablicy 8-elementowej", index);
        show("liczba porównań", comparisons[0]);
        // WYNIK: indeks wartości 23 w tablicy 8-elementowej → 5
        // WYNIK: liczba porównań → 2

        comparisons[0] = 0;
        int bigIndex = binarySearchCounted(makeSorted(1024), 777, comparisons);
        show("indeks wartości 777 w tablicy 1024-elementowej", bigIndex);
        show("liczba porównań", comparisons[0]);
        // WYNIK: indeks wartości 777 w tablicy 1024-elementowej → 777
        // WYNIK: liczba porównań → 9

        note("tablica 128 razy większa (8 → 1024), a porównań zaledwie kilka więcej (2 → 9) — bo koszt rośnie jak log2(n), nie jak n");

        // DOBRA PRAKTYKA: wyszukiwanie binarne wymaga POSORTOWANYCH danych, bo odrzuca połówki względem środka —
        //   na nieposortowanej tablicy da błędny wynik bez ostrzeżenia (szczegóły: Algorithms03Searching).
    }

    // =================================================================================================
    // 3. O(n) — CZAS LINIOWY (+ ZASADA UPRASZCZANIA)
    // =================================================================================================

    /** sumCounted = suma elementów z licznikiem odwiedzin — każdy element odwiedzamy dokładnie raz. */
    static long sumCounted(int[] arr, long[] visits) {
        long sum = 0;
        for (int value : arr) {
            visits[0]++;
            sum += value;
        }
        return sum;
    }

    /**
     * 3. Pojedyncza pętla po n elementach: liczba operacji rośnie DOKŁADNIE proporcjonalnie do n.
     * Zasada upraszczania: {@code 3n + 5} operacji to i tak O(n) — stałą 3 i +5 pomijamy, bo nie mają znaczenia przy dużym n.
     */
    static void linearTime() {
        section("3. O(n) — czas liniowy");

        int[] ten = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        long[] visits = {0};
        long sum = sumCounted(ten, visits);
        show("suma 10 elementów", sum);
        show("liczba odwiedzin dla n=10", visits[0]);
        // WYNIK: suma 10 elementów → 55
        // WYNIK: liczba odwiedzin dla n=10 → 10

        visits[0] = 0;
        sumCounted(new int[1000], visits);
        show("liczba odwiedzin dla n=1000", visits[0]);
        // WYNIK: liczba odwiedzin dla n=1000 → 1000

        note("n=10 → 10 odwiedzin, n=1000 → 1000 odwiedzin: podwojenie n DOKŁADNIE podwaja koszt — to cecha O(n)");

        // PUŁAPKA: kod z JEDNĄ widoczną pętlą, ale wywołujący wewnątrz operację kosztowną (np. list.contains
        //   w pętli), wcale nie jest liniowy — prawdziwy koszt liczy się z kosztu operacji w środku (Algorithms05Classics).
    }

    // =================================================================================================
    // 4. O(n log n) — CZAS "LINIOWO-LOGARYTMICZNY"
    // =================================================================================================

    /**
     * 4. Jeśli n razy wykonujemy operację kosztującą O(log n), razem wychodzi O(n log n).
     * Tu: n wyszukiwań binarnych w tablicy n-elementowej. Tak samo działa sortowanie przez scalanie
     * (merge sort, Algorithms02Sorting) — n elementów, każdy "wstawiany" kosztem log n.
     */
    static void linearithmicTime() {
        section("4. O(n log n) — czas liniowo-logarytmiczny");

        int n = 1000;
        int[] sorted = makeSorted(n);
        int[] cmp = {0};
        long totalComparisons = 0;
        for (int target = 0; target < n; target++) {
            cmp[0] = 0;
            binarySearchCounted(sorted, target, cmp);
            totalComparisons += cmp[0];
        }
        show("suma porównań dla n=1000 wyszukiwań binarnych", totalComparisons);
        long approxNLogN = Math.round(n * (Math.log(n) / Math.log(2)));
        show("przybliżenie wzorem n·log2(n)", approxNLogN);
        // WYNIK: suma porównań dla n=1000 wyszukiwań binarnych → 8987
        // WYNIK: przybliżenie wzorem n·log2(n) → 9966

        note("liczby bliskie sobie (8987 ≈ 9966) — to potwierdza wzrost rzędu n·log2(n), nie n ani n²");
    }

    // =================================================================================================
    // 5. O(n²) — CZAS KWADRATOWY
    // =================================================================================================

    /** countDuplicatePairsNaive = naiwne liczenie par duplikatów: porównujemy KAŻDY element z KAŻDYM dalszym. */
    static int countDuplicatePairsNaive(int[] arr, long[] comparisons) {
        int duplicates = 0;
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                comparisons[0]++;
                if (arr[i] == arr[j]) {
                    duplicates++;
                }
            }
        }
        return duplicates;
    }

    /** 5. Dwie zagnieżdżone pętle po tych samych danych: liczba porównań rośnie jak n*(n-1)/2 — rząd n². */
    static void quadraticTime() {
        section("5. O(n²) — czas kwadratowy");

        int[] arr8 = {3, 1, 4, 1, 5, 9, 2, 6};
        long[] cmp = {0};
        int duplicates = countDuplicatePairsNaive(arr8, cmp);
        show("znalezione pary duplikatów (naiwnie, i < j)", duplicates);
        show("liczba porównań dla n=8", cmp[0]);
        // WYNIK: znalezione pary duplikatów (naiwnie, i < j) → 1
        // WYNIK: liczba porównań dla n=8 → 28

        cmp[0] = 0;
        countDuplicatePairsNaive(makeSorted(16), cmp);
        show("liczba porównań dla n=16 (podwojone n)", cmp[0]);
        // WYNIK: liczba porównań dla n=16 (podwojone n) → 120

        note("n=8 → 28 porównań, n=16 → 120: podwojenie n dało ~4,3× więcej porównań — charakterystyczne dla O(n²)");

        // PUŁAPKA: zagnieżdżona pętla "ukryta" w wywołaniu metody (np. list.contains(x) WEWNĄTRZ pętli for)
        //   też daje O(n²), mimo że w kodzie widać tylko jedną pętlę — prawdziwy koszt kryje się w contains
        //   (szczegóły: t12_collections/Collections13Performance).
    }

    // =================================================================================================
    // 6. O(2^n) — CZAS WYKŁADNICZY
    // =================================================================================================

    /** fibNaive = n-ty wyraz Fibonacciego, naiwnie — liczy te same podproblemy wielokrotnie (Methods03Recursion). */
    static long fibNaive(int n) {
        fibCalls++;
        if (n < 2) {
            return n;
        }
        return fibNaive(n - 1) + fibNaive(n - 2);
    }

    /** 6. Każdy +1 do n MNOŻY liczbę wywołań razy ok. 1,618 (złota liczba) — to wciąż wzrost wykładniczy. */
    static void exponentialTime() {
        section("6. O(2^n) — czas wykładniczy");

        for (int n : new int[] {10, 20, 30}) {
            fibCalls = 0;
            long result = fibNaive(n);
            show("fib(" + n + ") naiwnie — wynik / wywołań", result + " / " + fibCalls);
        }
        // WYNIK: fib(10) naiwnie — wynik / wywołań → 55 / 177
        // WYNIK: fib(20) naiwnie — wynik / wywołań → 6765 / 21891
        // WYNIK: fib(30) naiwnie — wynik / wywołań → 832040 / 2692537

        note("n=10→177, n=20→21891, n=30→2692537: każde +10 do n mnoży liczbę wywołań ponad setki razy");

        // PUŁAPKA: naiwny fib(50) wykonałby ok. 40 miliardów wywołań (Methods03Recursion) — program "zawiesiłby
        //   się" na długie minuty. Lekarstwo: memoizacja albo wersja iteracyjna (t24_algorithms/Algorithms07DynamicProgramming).
    }

    // =================================================================================================
    // 7. TABELA WZROSTU
    // =================================================================================================

    /** ceilLog2 = najmniejsza liczba podwojeń, by osiągnąć/przekroczyć n — czyli ⌈log2(n)⌉, liczona przez liczenie. */
    static int ceilLog2(int n) {
        long value = 1;
        int power = 0;
        while (value < n) {
            value *= 2;
            power++;
        }
        return power;
    }

    /** powerOfTwoOrNote = 2^n jako tekst — dla małych n liczba, dla dużych notatka (nie da się zapisać). */
    static String powerOfTwoOrNote(int n) {
        if (n <= 20) {
            return Long.toString(1L << n);
        }
        return "n/d (więcej niż atomów we wszechświecie)";
    }

    /** 7. Zestawienie tempa wzrostu dla n = 10 / 1000 / 1 000 000 — widać, jak rozjeżdżają się kolumny. */
    static void growthTable() {
        section("7. Tabela wzrostu dla n = 10 / 1000 / 1 000 000");

        System.out.println(String.format(Locale.ROOT, "%-12s %9s %13s %15s   %s",
                "n", "~log2(n)", "n*log2(n)", "n^2", "2^n"));
        // WYNIK: n             ~log2(n)     n*log2(n)             n^2   2^n

        for (int n : new int[] {10, 1_000, 1_000_000}) {
            int log2 = ceilLog2(n);
            long nLogN = (long) n * log2;
            long nSquared = (long) n * n;
            String pow2 = powerOfTwoOrNote(n);
            System.out.println(String.format(Locale.ROOT, "%-12d %9d %13d %15d   %s", n, log2, nLogN, nSquared, pow2));
        }
        // WYNIK: 10                   4            40             100   1024
        // WYNIK: 1000                10         10000         1000000   n/d (więcej niż atomów we wszechświecie)
        // WYNIK: 1000000             20      20000000   1000000000000   n/d (więcej niż atomów we wszechświecie)

        note("kolumna O(n²) i O(2^n) rosną najszybciej — przy milionie elementów O(n²) to już bilion operacji");
    }

    // =================================================================================================
    // 8. PRZYPADEK OPTYMISTYCZNY, ŚREDNI I PESYMISTYCZNY
    // =================================================================================================

    /** linearSearchCounted = szukanie liniowe z licznikiem porównań — przegląda od początku, aż trafi albo skończy. */
    static int linearSearchCounted(int[] arr, int target, int[] comparisons) {
        for (int i = 0; i < arr.length; i++) {
            comparisons[0]++;
            if (arr[i] == target) {
                return i;
            }
        }
        return -1;
    }

    /** 8. Ta sama metoda, trzy różne koszty — zależnie od TEGO, gdzie (i czy) jest szukany element. */
    static void bestAverageWorstCase() {
        section("8. Przypadek optymistyczny / średni / pesymistyczny (best / average / worst case)");

        int[] arr = {7, 3, 9, 1, 8, 2, 6, 4, 5, 0}; // 10 elementów, nieposortowane
        int[] cmp = {0};

        linearSearchCounted(arr, 7, cmp); // element na początku
        show("przypadek optymistyczny: element na pozycji 0 — porównań", cmp[0]);
        // WYNIK: przypadek optymistyczny: element na pozycji 0 — porównań → 1

        cmp[0] = 0;
        linearSearchCounted(arr, 99, cmp); // element nieobecny
        show("przypadek pesymistyczny: element nieobecny — porównań", cmp[0]);
        // WYNIK: przypadek pesymistyczny: element nieobecny — porównań → 10

        note("przypadek średni: licząc dla wszystkich możliwych pozycji elementu, wychodzi ok. (n+1)/2 = 5,5 porównania");

        // DOBRA PRAKTYKA: notacja O() opisuje zwykle przypadek PESYMISTYCZNY (worst case), bo daje GWARANCJĘ
        //   górnej granicy — "nigdy nie będzie gorzej niż..." jest bardziej przydatne niż "zwykle jest nieźle".
    }

    // =================================================================================================
    // 9. ZŁOŻONOŚĆ PAMIĘCIOWA (SPACE COMPLEXITY)
    // =================================================================================================

    /** reverseInPlace = odwrócenie tablicy BEZ dodatkowej tablicy — tylko dwa indeksy (O(1) dodatkowej pamięci). */
    static int[] reverseInPlace(int[] arr) {
        for (int i = 0, j = arr.length - 1; i < j; i++, j--) {
            int tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
        return arr;
    }

    /** reverseToNewArray = odwrócenie DO NOWEJ tablicy — oryginał nietknięty, ale potrzeba n dodatkowych komórek. */
    static int[] reverseToNewArray(int[] arr) {
        int[] copy = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            copy[i] = arr[arr.length - 1 - i];
        }
        return copy;
    }

    /** 9. Ten sam wynik (odwrócona tablica), różny koszt pamięciowy — O(1) kontra O(n) dodatkowej pamięci. */
    static void spaceComplexity() {
        section("9. Złożoność pamięciowa (space complexity)");

        int[] inPlaceSource = {1, 2, 3, 4, 5};
        int[] reversedInPlace = reverseInPlace(inPlaceSource);
        show("odwrócona w miejscu, O(1) dodatkowej pamięci", reversedInPlace);
        // WYNIK: odwrócona w miejscu, O(1) dodatkowej pamięci → [5, 4, 3, 2, 1]

        int[] original = {1, 2, 3, 4, 5};
        int[] reversedCopy = reverseToNewArray(original);
        show("odwrócona do nowej tablicy, O(n) dodatkowej pamięci", reversedCopy);
        show("oryginał niezmieniony (bo kopiowaliśmy)", original);
        // WYNIK: odwrócona do nowej tablicy, O(n) dodatkowej pamięci → [5, 4, 3, 2, 1]
        // WYNIK: oryginał niezmieniony (bo kopiowaliśmy) → [1, 2, 3, 4, 5]

        note("czas i pamięć to DWA różne wymiary złożoności — algorytm może być szybki, ale kosztowny pamięciowo");

        // PUŁAPKA: rekurencja też zużywa pamięć — każde wywołanie to nowa ramka na stosie (Methods03Recursion),
        //   więc rekurencyjne rozwiązanie z głębokością n ma O(n) złożoności pamięciowej, nawet gdy czasowo jest O(n).
    }

    // =================================================================================================
    // 10. ANALIZA ZAMORTYZOWANA — ArrayList.add W PRAKTYCE
    // =================================================================================================

    /** TinyDynamicArray = uproszczona wersja ArrayList: gdy brakuje miejsca, podwaja pojemność i kopiuje elementy. */
    static class TinyDynamicArray {
        private int[] data = new int[1];
        private int size = 0;
        private long totalCopies = 0; // totalCopies = suma elementów skopiowanych przy wszystkich powiększeniach

        void add(int value) {
            if (size == data.length) {
                int[] bigger = new int[data.length * 2];
                for (int i = 0; i < size; i++) {
                    bigger[i] = data[i];
                    totalCopies++;
                }
                data = bigger;
            }
            data[size++] = value;
        }

        long totalCopies() {
            return totalCopies;
        }
    }

    /**
     * 10. Pojedyncze dodanie CZASAMI kosztuje O(n) (powiększenie tablicy), ale ŚREDNIO (zamortyzowanie)
     * wychodzi O(1) — bo powiększenia są coraz rzadsze. Tak właśnie działa java.util.ArrayList (rośnie ×1,5,
     * nie ×2, ale zasada jest identyczna).
     */
    static void amortizedAnalysis() {
        section("10. Analiza zamortyzowana — ArrayList.add w praktyce");

        TinyDynamicArray dyn = new TinyDynamicArray();
        for (int i = 0; i < 1000; i++) {
            dyn.add(i);
        }
        show("dodano 1000 elementów — łączna liczba skopiowanych elementów przy powiększeniach", dyn.totalCopies());
        double perAdd = dyn.totalCopies() / 1000.0;
        show("średnio skopiowanych elementów na jedno dodanie", perAdd);
        // WYNIK: dodano 1000 elementów — łączna liczba skopiowanych elementów przy powiększeniach → 1023
        // WYNIK: średnio skopiowanych elementów na jedno dodanie → 1.023

        note("1023 kopiowania na 1000 dodań = ok. 1 kopiowanie na dodanie ŚREDNIO — mimo że pojedyncze powiększenie kopiuje setki elementów naraz");

        // DOBRA PRAKTYKA: jeśli znasz w przybliżeniu docelowy rozmiar listy, użyj new ArrayList<>(capacity) —
        //   unikniesz powiększeń w ogóle, bo rezerwujesz miejsce z góry.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • O() opisuje TEMPO WZROSTU liczby operacji względem n — nie sekundy, nie konkretny komputer.
     *   • Pomijamy stałe i składniki niższego rzędu: 3n + 5 → O(n); n² + n → O(n²).
     *   • O(1) stały, O(log n) logarytmiczny (wyszukiwanie binarne), O(n) liniowy (jedna pętla),
     *     O(n log n) (sortowanie przez scalanie), O(n²) kwadratowy (zagnieżdżone pętle), O(2^n) wykładniczy
     *     (naiwny Fibonacci).
     *   • Zwykle podaje się przypadek PESYMISTYCZNY (worst case) — gwarantuje górną granicę kosztu.
     *   • Złożoność PAMIĘCIOWA to osobny wymiar — algorytm szybki czasowo może być kosztowny pamięciowo.
     *   • Analiza ZAMORTYZOWANA: pojedyncza operacja czasem drożej (O(n)), ale średnio na wiele wywołań — O(1).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się O(n) od O(n²) w praktyce przy milionie elementów?
     *   2. Co wypisze:  int ops = 0; for (int i = 0; i < 5; i++) { for (int j = 0; j < 5; j++) { ops++; } }
     *      System.out.println(ops);  ?
     *   3. ZNAJDŹ BŁĄD: ktoś twierdzi, że poniższa metoda jest O(1), bo "ma tylko jeden return":
     *      {@code int sumAll(int[] arr) { int s = 0; for (int x : arr) s += x; return s; } }
     *   4. Dlaczego notacja O() pomija stałe (np. pisze O(n) zamiast O(3n + 5))?
     *   5. Czym jest przypadek pesymistyczny i czemu to on jest najczęściej podawaną złożonością?
     *   6. Co to znaczy, że ArrayList.add ma złożoność ZAMORTYZOWANĄ O(1), skoro pojedyncze wywołanie
     *      czasem kosztuje O(n)?
     *   7. ZNAJDŹ BŁĄD: ktoś twierdzi, że wyszukiwanie binarne dla n = 1 000 000 jest "prawie tak szybkie
     *      jak dla n = 10, bo to ciągle ta sama metoda" — czy to dobre uzasadnienie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: liczba operacji O(1) (dostęp do pierwszego i ostatniego elementu)",
                2, () -> exercise1(new int[] {1, 2, 3, 4, 5}));
        Check.equal("ćw. 2: liczba iteracji pojedynczej pętli dla n=250", 250, () -> exercise2(250));
        Check.equal("ćw. 3: liczba iteracji podwójnej pętli dla n=12", 144, () -> exercise3(12));
        Check.equal("ćw. 4: liczba kroków wyszukiwania binarnego dla n=500", 9, () -> exercise4(500));
        Check.equal("ćw. 5: czy tablica ma duplikat (wersja O(n) przez zbiór)",
                true, () -> exercise5(new int[] {5, 3, 9, 1, 3, 7}));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 2, () -> solution1(new int[] {1, 2, 3, 4, 5}));
        Check.equal("ćw. 2 (wzorzec)", 250, () -> solution2(250));
        Check.equal("ćw. 3 (wzorzec)", 144, () -> solution3(12));
        Check.equal("ćw. 4 (wzorzec)", 9, () -> solution4(500));
        Check.equal("ćw. 5 (wzorzec)", true, () -> solution5(new int[] {5, 3, 9, 1, 3, 7}));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć liczbę operacji O(1) wykonanych, by odczytać pierwszy i ostatni element
     * tablicy (zawsze 2, niezależnie od rozmiaru tablicy).
     * Podpowiedź: odczytaj arr[0] i arr[arr.length - 1], policz odczyty.
     */
    static int exercise1(int[] arr) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (łatwe): rekurencją tu nie trzeba — zwróć liczbę iteracji pojedynczej pętli {@code for (int i = 0; i < n; i++)}.
     * Podpowiedź: policz, ile razy pętla wykona ciało (powinno wyjść dokładnie n).
     */
    static int exercise2(int n) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć liczbę iteracji DWÓCH zagnieżdżonych pętli {@code for (i: 0..n) for (j: 0..n)}.
     * Podpowiedź: powinno wyjść n*n — to właśnie O(n²).
     */
    static int exercise3(int n) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć liczbę kroków potrzebnych, by przez wielokrotne PODWAJANIE liczby
     * startującej od 1 osiągnąć lub przekroczyć n (czyli {@code ⌈log2(n)⌉}) — to liczba porównań wyszukiwania
     * binarnego w tablicy n-elementowej.
     * Podpowiedź: {@code long value = 1; int steps = 0; while (value < n) { value *= 2; steps++; } }
     */
    static int exercise4(int n) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 5 (trudniejsze, PRZEPISZ): naiwne sprawdzanie duplikatu w tablicy wygląda tak (O(n²)):
     * <pre>{@code
     * boolean hasDuplicateNaive(int[] arr) {
     *     for (int i = 0; i < arr.length; i++)
     *         for (int j = i + 1; j < arr.length; j++)
     *             if (arr[i] == arr[j]) return true;
     *     return false;
     * }
     * }</pre>
     * Przepisz to na wersję O(n) z użyciem zbioru (Set, t12_collections/Collections04Sets) — jeden przebieg,
     * zapamiętywanie odwiedzonych wartości.
     * Podpowiedź: {@code Set<Integer> seen = new HashSet<>(); } i metoda {@code seen.add(x)} zwraca false,
     * gdy element już tam był.
     */
    static boolean exercise5(int[] arr) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int[] arr) {
        int ops = 0;
        int first = arr[0]; ops++;
        int last = arr[arr.length - 1]; ops++;
        return ops;
    }

    static int solution2(int n) {
        int ops = 0;
        for (int i = 0; i < n; i++) { ops++; }
        return ops;
    }

    static int solution3(int n) {
        int ops = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) { ops++; }
        }
        return ops;
    }

    static int solution4(int n) {
        long value = 1;
        int steps = 0;
        while (value < n) { value *= 2; steps++; }
        return steps;
    }

    static boolean solution5(int[] arr) {
        Set<Integer> seen = new HashSet<>();
        for (int value : arr) {
            if (!seen.add(value)) {  // add zwraca false, gdy element już był w zbiorze
                return true;
            }
        }
        return false;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Dla miliona elementów O(n) to milion operacji, a O(n²) to bilion (10^12) — w praktyce różnica
     *      między "działa natychmiast" a "nie doczekasz się wyniku".
     *   2. 25 (5 × 5 — dwie zagnieżdżone pętle po 5 to n² dla n=5).
     *   3. Błąd: metoda MA jeden return, ale wewnątrz jest pętla for po całej tablicy — to O(n), nie O(1).
     *      Liczba returnów nie ma związku ze złożonością; liczy się, ile razy wykonuje się kod.
     *   4. Bo dla dużego n stałe i składniki niższego rzędu nie wpływają na TEMPO WZROSTU — O(n) i O(3n + 5)
     *      rosną "tak samo" (liniowo), różnią się tylko stałym czynnikiem, który zależy od implementacji.
     *   5. Przypadek pesymistyczny (worst case) to maksymalny możliwy koszt dla danego n. Podaje się jego
     *      złożoność, bo daje GWARANCJĘ — "nigdy nie będzie gorzej" jest bardziej użyteczne niż "zwykle nieźle".
     *   6. Pojedyncze wywołanie może skopiować całą tablicę (O(n)), ale takie powiększenia zdarzają się coraz
     *      rzadziej (po podwojeniu pojemności) — uśredniając koszt na WIELU wywołaniach, wychodzi O(1) na dodanie.
     *   7. Nie — to "prawie tak szybkie" jest mylące. log2(10) ≈ 3,3, a log2(1 000 000) ≈ 20 — sześć razy więcej
     *      porównań. To WCIĄŻ dużo lepsze niż liniowo (milion porównań), ale nie "to samo", co dla n=10.
     */
    // </editor-fold>
}
