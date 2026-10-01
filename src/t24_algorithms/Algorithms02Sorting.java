package t24_algorithms;

import helpers.Check;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Algorytmy sortowania — od O(n²) do O(n log n)
 *        (sorting = sortowanie; comparison = porównanie; swap = zamiana; stability = stabilność; pivot = oś podziału)
 *
 * W SKRÓCIE:
 *   Sortowania "naiwne" (bąbelkowe, przez wybieranie, przez wstawianie) mają złożoność O(n²) i dobrze pokazują
 *   MECHANIZM sortowania. Sortowania "dziel i zwyciężaj" (przez scalanie, szybkie) osiągają O(n log n) —
 *   tego właśnie używają biblioteki standardowe.
 *
 * ANALOGIA: porządkowanie kart w ręku.
 *   Sortowanie przez wstawianie to dokładnie to, co robisz trzymając karty — bierzesz kolejną i wsuwasz
 *   ją na właściwe miejsce wśród już ułożonych. Sortowanie przez scalanie to rozdanie kart dwóm osobom,
 *   niech każda ułoży swoją połowę, a potem scalasz obie ułożone połówki w jedną.
 *
 * JAK TO DZIAŁA:
 *   Licznik porównań i zamian pokazuje KOSZT algorytmu niezależnie od szybkości komputera (Algorithms01Complexity).
 *   Dziel-i-zwyciężaj: dzielimy problem na mniejsze (rekurencja), rozwiązujemy je, scalamy wyniki —
 *   stąd O(n log n): log n poziomów podziału, na każdym poziomie O(n) pracy.
 *
 * SŁÓWKA:
 *   sort = sortowanie; comparison = porównanie; swap = zamiana; shift = przesunięcie; pivot = oś podziału;
 *   partition = podział (rozdzielenie); stable sort = sortowanie stabilne; merge = scalanie; divide and conquer =
 *   dziel i zwyciężaj; in-place = w miejscu (bez dodatkowej pamięci).
 *
 * ZOBACZ TEŻ: t24_algorithms/Algorithms01Complexity (notacja O(), liczenie operacji),
 *             t24_algorithms/Algorithms03Searching (wyszukiwanie binarne wymaga posortowanych danych),
 *             t12_collections/Collections07ComparableComparator (Comparator.comparing, thenComparing).
 * </pre>
 */
public class Algorithms02Sorting {

    public static void main(String[] args) {
        title("Algorithms02 — sortowanie: od O(n²) do O(n log n)");

        bubbleSortDemo();       // bubble sort = sortowanie bąbelkowe
        selectionSortDemo();    // selection sort = sortowanie przez wybieranie
        insertionSortDemo();    // insertion sort = sortowanie przez wstawianie
        comparisonTable();      // comparison table = tabela porównawcza
        mergeSortDemo();        // merge sort = sortowanie przez scalanie
        quickSortDemo();        // quick sort = szybkie sortowanie
        stabilityDemo();        // stability = stabilność sortowania
        jdkSortFacts();         // JDK sort facts = fakty o sortowaniu w JDK
        exercises();            // exercises = ćwiczenia
    }

    /** sourceArray = ten sam punkt startowy dla bąbelkowego, przez wybieranie i przez wstawianie — uczciwe porównanie. */
    static int[] sourceArray() {
        return new int[] {5, 2, 9, 1, 5, 6, 3, 8};
    }

    // =================================================================================================
    // 1. SORTOWANIE BĄBELKOWE (BUBBLE SORT)
    // =================================================================================================

    /** bubbleSortCounted = sąsiednie elementy zamieniamy, gdy są w złej kolejności; "większe wypływają w prawo". */
    static void bubbleSortCounted(int[] arr, long[] comparisons, long[] swaps) {
        boolean swapped = true;
        while (swapped) {
            swapped = false;
            for (int i = 0; i < arr.length - 1; i++) {
                comparisons[0]++;
                if (arr[i] > arr[i + 1]) {
                    int tmp = arr[i];
                    arr[i] = arr[i + 1];
                    arr[i + 1] = tmp;
                    swaps[0]++;
                    swapped = true;
                }
            }
        }
    }

    /**
     * 1. Ślad dla [5, 2, 9, 1] (4 elementy, pierwszy przebieg): porównaj (5,2)→zamiana→[2,5,9,1];
     * porównaj (5,9)→bez zamiany; porównaj (9,1)→zamiana→[2,5,1,9]. Po przebiegu największy (9) jest na końcu.
     * Jeśli cały przebieg nie zrobił ŻADNEJ zamiany — tablica jest posortowana, kończymy wcześniej.
     */
    static void bubbleSortDemo() {
        section("1. Sortowanie bąbelkowe (bubble sort)");

        int[] arr = sourceArray();
        show("przed sortowaniem", arr);
        long[] comparisons = {0};
        long[] swaps = {0};
        bubbleSortCounted(arr, comparisons, swaps);
        show("po sortowaniu", arr);
        show("liczba porównań", comparisons[0]);
        show("liczba zamian", swaps[0]);
        // WYNIK: przed sortowaniem → [5, 2, 9, 1, 5, 6, 3, 8]
        // WYNIK: po sortowaniu → [1, 2, 3, 5, 5, 6, 8, 9]
        // WYNIK: liczba porównań → 35
        // WYNIK: liczba zamian → 11

        note("35 porównań dla n=8 — to wciąż O(n²); flaga \"swapped\" kończy wcześniej, ale NIE skraca zakresu przebiegu");

        // PUŁAPKA: bez flagi "swapped" bąbelkowe sortowanie ZAWSZE robi n-1 przebiegów, nawet dla posortowanej tablicy.
        // DOBRA PRAKTYKA: SKRACAJ też zakres przebiegu o 1 (ostatni element już na miejscu) — dałoby to 28 zamiast 35.
    }

    // =================================================================================================
    // 2. SORTOWANIE PRZEZ WYBIERANIE (SELECTION SORT)
    // =================================================================================================

    /** selectionSortCounted = w każdym przebiegu szukamy NAJMNIEJSZEGO w nieposortowanej reszcie i wstawiamy go na początek. */
    static void selectionSortCounted(int[] arr, long[] comparisons, long[] swaps) {
        for (int i = 0; i < arr.length - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < arr.length; j++) {
                comparisons[0]++;
                if (arr[j] < arr[minIndex]) {
                    minIndex = j;
                }
            }
            if (minIndex != i) {
                int tmp = arr[i];
                arr[i] = arr[minIndex];
                arr[minIndex] = tmp;
                swaps[0]++;
            }
        }
    }

    /**
     * 2. Ślad dla [5, 2, 9, 1]: i=0 szukamy minimum w całej tablicy → 1 (indeks 3) → zamiana z pozycją 0
     * → [1, 2, 9, 5]. i=1 minimum w [2, 9, 5] → 2 (już na miejscu, bez zamiany). i=2 minimum w [9, 5] → 5
     * (indeks 3) → zamiana → [1, 2, 5, 9]. Zawsze dokładnie n-1 ZAMIAN (co najwyżej), ale n*(n-1)/2 PORÓWNAŃ.
     */
    static void selectionSortDemo() {
        section("2. Sortowanie przez wybieranie (selection sort)");

        int[] arr = sourceArray();
        long[] comparisons = {0};
        long[] swaps = {0};
        selectionSortCounted(arr, comparisons, swaps);
        show("po sortowaniu", arr);
        show("liczba porównań", comparisons[0]);
        show("liczba zamian", swaps[0]);
        // WYNIK: po sortowaniu → [1, 2, 3, 5, 5, 6, 8, 9]
        // WYNIK: liczba porównań → 28
        // WYNIK: liczba zamian → 3

        note("28 = n*(n-1)/2 porównań — dokładnie tyle, ile wynosi teoretyczne minimum dla n=8, i tylko 3 zamiany");

        // DOBRA PRAKTYKA: selection sort ma STAŁĄ liczbę porównań niezależnie od tego, czy dane są posortowane,
        //   czy nie — w przeciwieństwie do bubble/insertion sort, które na "prawie posortowanych" danych są szybsze.
    }

    // =================================================================================================
    // 3. SORTOWANIE PRZEZ WSTAWIANIE (INSERTION SORT)
    // =================================================================================================

    /** insertionSortCounted = bierzemy kolejny element i WSUWAMY go na właściwe miejsce wśród już posortowanych. */
    static void insertionSortCounted(int[] arr, long[] comparisons, long[] shifts) {
        for (int i = 1; i < arr.length; i++) {
            int key = arr[i];
            int j = i - 1;
            while (j >= 0) {
                comparisons[0]++;
                if (arr[j] <= key) {
                    break;
                }
                arr[j + 1] = arr[j];
                shifts[0]++;
                j--;
            }
            arr[j + 1] = key;
        }
    }

    /**
     * 3. Ślad dla [5, 2, 9, 1]: i=1, key=2, przesuń 5 w prawo → [5, 5, 9, 1], wstaw 2 → [2, 5, 9, 1].
     * i=2, key=9, 9 ≥ 5 — zostaje na miejscu → [2, 5, 9, 1]. i=3, key=1, przesuń 9, 5, 2 w prawo, wstaw 1
     * na początek → [1, 2, 5, 9]. Im bardziej dane są "prawie posortowane", tym MNIEJ przesunięć.
     */
    static void insertionSortDemo() {
        section("3. Sortowanie przez wstawianie (insertion sort)");

        int[] arr = sourceArray();
        long[] comparisons = {0};
        long[] shifts = {0};
        insertionSortCounted(arr, comparisons, shifts);
        show("po sortowaniu", arr);
        show("liczba porównań", comparisons[0]);
        show("liczba przesunięć", shifts[0]);
        // WYNIK: po sortowaniu → [1, 2, 3, 5, 5, 6, 8, 9]
        // WYNIK: liczba porównań → 16
        // WYNIK: liczba przesunięć → 11

        int[] almostSorted = {1, 2, 3, 4, 6, 5, 7, 8};
        comparisons[0] = 0;
        shifts[0] = 0;
        insertionSortCounted(almostSorted, comparisons, shifts);
        show("prawie posortowana tablica — porównań", comparisons[0]);
        show("prawie posortowana tablica — przesunięć", shifts[0]);
        // WYNIK: prawie posortowana tablica — porównań → 8
        // WYNIK: prawie posortowana tablica — przesunięć → 1

        note("na niemal posortowanych danych insertion sort jest BLISKI O(n) — to jego wielka zaleta nad selection sort");

        // DOBRA PRAKTYKA: dlatego Collections.sort/Arrays.sort (TimSort) na KRÓTKICH odcinkach i tak przełączają
        //   się na insertion sort — przy małym n stała kosztu bywa ważniejsza niż rząd wielkości.
    }

    // =================================================================================================
    // 4. TABELA PORÓWNAWCZA (TE SAME DANE WEJŚCIOWE)
    // =================================================================================================

    /** 4. Trzy algorytmy, jedna tablica startowa [5, 2, 9, 1, 5, 6, 3, 8] — porównania i zamiany obok siebie. */
    static void comparisonTable() {
        section("4. Tabela porównawcza — te same dane, trzy algorytmy");

        long[] bubbleCmp = {0}, bubbleSwp = {0};
        bubbleSortCounted(sourceArray(), bubbleCmp, bubbleSwp);
        long[] selectionCmp = {0}, selectionSwp = {0};
        selectionSortCounted(sourceArray(), selectionCmp, selectionSwp);
        long[] insertionCmp = {0}, insertionShf = {0};
        insertionSortCounted(sourceArray(), insertionCmp, insertionShf);

        System.out.println(String.format("%-20s %11s %15s", "algorytm", "porównania", "zamiany/przes."));
        System.out.println(String.format("%-20s %11d %15d", "bąbelkowe", bubbleCmp[0], bubbleSwp[0]));
        System.out.println(String.format("%-20s %11d %15d", "przez wybieranie", selectionCmp[0], selectionSwp[0]));
        System.out.println(String.format("%-20s %11d %15d", "przez wstawianie", insertionCmp[0], insertionShf[0]));
        // WYNIK: algorytm              porównania  zamiany/przes.
        // WYNIK: bąbelkowe                     35              11
        // WYNIK: przez wybieranie              28               3
        // WYNIK: przez wstawianie              16              11

        note("wszystkie trzy to O(n²) — różnią się STAŁYM czynnikiem i zachowaniem na danych już prawie posortowanych");
    }

    // =================================================================================================
    // 5. SORTOWANIE PRZEZ SCALANIE (MERGE SORT)
    // =================================================================================================

    /** merge = scal dwie POSORTOWANE połówki arr[low..mid] i arr[mid+1..high] w jedną posortowaną całość. */
    static void merge(int[] arr, int low, int mid, int high, long[] comparisons) {
        int[] left = Arrays.copyOfRange(arr, low, mid + 1);
        int[] right = Arrays.copyOfRange(arr, mid + 1, high + 1);
        int i = 0, j = 0, k = low;
        while (i < left.length && j < right.length) {
            comparisons[0]++;
            arr[k++] = (left[i] <= right[j]) ? left[i++] : right[j++];
        }
        while (i < left.length) { arr[k++] = left[i++]; }
        while (j < right.length) { arr[k++] = right[j++]; }
    }

    static void mergeSortTraced(int[] arr, int low, int high, int depth, long[] comparisons) {
        if (low >= high) {
            return;
        }
        String indent = "   " + "  ".repeat(depth);
        System.out.println(indent + "podziel " + Arrays.toString(Arrays.copyOfRange(arr, low, high + 1)));
        int mid = low + (high - low) / 2;
        mergeSortTraced(arr, low, mid, depth + 1, comparisons);
        mergeSortTraced(arr, mid + 1, high, depth + 1, comparisons);
        merge(arr, low, mid, high, comparisons);
        System.out.println(indent + "scal    " + Arrays.toString(Arrays.copyOfRange(arr, low, high + 1)));
    }

    static void mergeSortCounted(int[] arr, int low, int high, long[] comparisons) {
        if (low >= high) {
            return;
        }
        int mid = low + (high - low) / 2;
        mergeSortCounted(arr, low, mid, comparisons);
        mergeSortCounted(arr, mid + 1, high, comparisons);
        merge(arr, low, mid, high, comparisons);
    }

    /**
     * 5. Dziel i zwyciężaj: rozdziel tablicę na POŁOWY, aż zostaną 1-elementowe (już posortowane), potem
     * SCALAJ pary rosnąco. log2(n) poziomów podziału × O(n) pracy scalania na poziom = O(n log n).
     */
    static void mergeSortDemo() {
        section("5. Sortowanie przez scalanie (merge sort)");

        int[] small = {8, 3, 5, 1};
        mergeSortTraced(small, 0, small.length - 1, 0, new long[1]);
        show("wynik na małej tablicy", small);
        // WYNIK: podziel [8, 3, 5, 1]
        // WYNIK:    podziel [8, 3]
        // WYNIK:    scal    [3, 8]
        // WYNIK:    podziel [5, 1]
        // WYNIK:    scal    [1, 5]
        // WYNIK: scal    [1, 3, 5, 8]
        // WYNIK: wynik na małej tablicy → [1, 3, 5, 8]

        int[] arr = sourceArray();
        long[] comparisons = {0};
        mergeSortCounted(arr, 0, arr.length - 1, comparisons);
        show("po sortowaniu (n=8)", arr);
        show("liczba porównań przy scalaniu", comparisons[0]);
        // WYNIK: po sortowaniu (n=8) → [1, 2, 3, 5, 5, 6, 8, 9]
        // WYNIK: liczba porównań przy scalaniu → 17

        note("17 porównań dla n=8 — znacznie mniej niż 28 w sortowaniach O(n²); to właśnie przewaga O(n log n)");

        // DOBRA PRAKTYKA: merge sort potrzebuje DODATKOWEJ tablicy do scalania (O(n) pamięci) — to kompromis
        //   za gwarantowane O(n log n) NIEZALEŻNIE od danych wejściowych (nawet dla już posortowanej tablicy).
    }

    // =================================================================================================
    // 6. SZYBKIE SORTOWANIE (QUICK SORT) — WYBÓR OSI PODZIAŁU
    // =================================================================================================

    static void swap(int[] arr, int a, int b) {
        int tmp = arr[a];
        arr[a] = arr[b];
        arr[b] = tmp;
    }

    /** partitionTraced = schemat Lomuto: ostatni element to oś (pivot); mniejsze od niej trafiają na lewo. */
    static int partitionTraced(int[] arr, int low, int high, String indent) {
        int pivot = arr[high];
        System.out.println(indent + "oś podziału (pivot) = " + pivot + " (ostatni element [" + low + ".." + high + "])");
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (arr[j] < pivot) {
                i++;
                swap(arr, i, j);
            }
        }
        swap(arr, i + 1, high);
        System.out.println(indent + "po podziale " + Arrays.toString(Arrays.copyOfRange(arr, low, high + 1))
                + " — pivot trafił na indeks " + (i + 1 - low) + " (licząc od " + low + ")");
        return i + 1;
    }

    static void quickSortTraced(int[] arr, int low, int high, int depth) {
        if (low >= high) {
            return;
        }
        String indent = "   " + "  ".repeat(depth);
        int p = partitionTraced(arr, low, high, indent);
        quickSortTraced(arr, low, p - 1, depth + 1);
        quickSortTraced(arr, p + 1, high, depth + 1);
    }

    static int partitionCounted(int[] arr, int low, int high, long[] comparisons) {
        int pivot = arr[high];
        int i = low - 1;
        for (int j = low; j < high; j++) {
            comparisons[0]++;
            if (arr[j] < pivot) {
                i++;
                swap(arr, i, j);
            }
        }
        swap(arr, i + 1, high);
        return i + 1;
    }

    static void quickSortCounted(int[] arr, int low, int high, long[] comparisons) {
        if (low < high) {
            int p = partitionCounted(arr, low, high, comparisons);
            quickSortCounted(arr, low, p - 1, comparisons);
            quickSortCounted(arr, p + 1, high, comparisons);
        }
    }

    /**
     * 6. Wybieramy OŚ (pivot) — tu zawsze ostatni element. Przesuwamy mniejsze elementy na lewo od osi,
     * większe na prawo (partition = podział), oś ląduje na SWOIM docelowym miejscu, dalej rekurencyjnie
     * sortujemy lewą i prawą część OSOBNO. (Klasyczne PYTANIE REKRUTACYJNE: zaimplementuj quicksort
     * i wyjaśnij, od czego zależy jego najgorszy przypadek — patrz druga część tej sekcji).
     */
    static void quickSortDemo() {
        section("6. Szybkie sortowanie (quick sort) — oś podziału (pivot)");

        int[] small = {5, 2, 9, 1, 6};
        quickSortTraced(small, 0, small.length - 1, 0);
        show("wynik", small);
        // WYNIK: oś podziału (pivot) = 6 (ostatni element [0..4])
        // WYNIK: po podziale [5, 2, 1, 6, 9] — pivot trafił na indeks 3 (licząc od 0)
        // WYNIK:    oś podziału (pivot) = 1 (ostatni element [0..2])
        // WYNIK:    po podziale [1, 2, 5] — pivot trafił na indeks 0 (licząc od 0)
        // WYNIK:       oś podziału (pivot) = 5 (ostatni element [1..2])
        // WYNIK:       po podziale [2, 5] — pivot trafił na indeks 1 (licząc od 1)
        // WYNIK: wynik → [1, 2, 5, 6, 9]

        note("oś (6) od razu wylądowała na indeksie 3 — na swoim OSTATECZNYM miejscu w posortowanej tablicy");

        section("7. Quick sort — przypadek pesymistyczny (worst case): pivot = ostatni element na JUŻ posortowanej tablicy "
                + "daje skrajnie nierówny podział za każdym razem (n poziomów rekurencji zamiast log n) → O(n²), tak jak bubble sort");

        int[] alreadySorted = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        long[] cmp1 = {0};
        quickSortCounted(alreadySorted, 0, alreadySorted.length - 1, cmp1);
        show("już posortowana tablica (n=10) — porównań", cmp1[0]);
        // WYNIK: już posortowana tablica (n=10) — porównań → 45

        int[] shuffled = {5, 3, 8, 1, 9, 2, 7, 4, 10, 6};
        long[] cmp2 = {0};
        quickSortCounted(shuffled, 0, shuffled.length - 1, cmp2);
        show("przetasowana tablica (n=10) — porównań", cmp2[0]);
        // WYNIK: przetasowana tablica (n=10) — porównań → 19

        note("45 porównań to n*(n-1)/2 — maksimum możliwe dla n=10; przetasowane dane dały ponad dwa razy mniej");

        // PUŁAPKA: pivot = "zawsze ostatni element" to częsty błąd początkujących — na już posortowanych lub
        //   odwrotnie posortowanych danych (typowe dane wejściowe w praktyce!) daje najgorszy możliwy przypadek.
        // DOBRA PRAKTYKA: wybieraj pivot losowo albo jako medianę z trzech (pierwszy/środkowy/ostatni) —
        //   to praktycznie eliminuje ryzyko trafienia na pechowe dane.
    }

    // =================================================================================================
    // 8. STABILNOŚĆ SORTOWANIA (STABILITY)
    // =================================================================================================

    /** Person = mała rekordowa klasa pomocnicza (name = imię, group = grupa) — tylko do demonstracji stabilności. */
    record Person(String name, int group) {
    }

    /** unstableSelectionSortByGroup = ta sama selection sort co wcześniej, ale wg klucza "group" obiektów Person. */
    static void unstableSelectionSortByGroup(Person[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < arr.length; j++) {
                if (arr[j].group() < arr[minIndex].group()) {
                    minIndex = j;
                }
            }
            if (minIndex != i) {
                Person tmp = arr[i];
                arr[i] = arr[minIndex];
                arr[minIndex] = tmp;
            }
        }
    }

    /**
     * 8. Sortowanie STABILNE zachowuje WZGLĘDNĄ kolejność elementów o RÓWNYM kluczu sortowania.
     * Dzięki temu można posortować najpierw wg drugorzędnego klucza (imię), a potem stabilnie wg
     * pierwszorzędnego (grupa) — w efekcie w obrębie grupy zostaje kolejność alfabetyczna.
     * (To klasyczne PYTANIE REKRUTACYJNE: podaj przykład sortowania stabilnego i niestabilnego).
     */
    static void stabilityDemo() {
        section("8. Stabilność sortowania — sortowanie po dwóch kluczach");

        Person[] stable = {
                new Person("Ala", 2), new Person("Bartek", 1),
                new Person("Celina", 2), new Person("Darek", 1),
        };
        // krok 1: posortuj wg drugorzędnego klucza (imię) — alfabetycznie Ala, Bartek, Celina, Darek (już tak jest)
        // krok 2: stabilnie posortuj wg pierwszorzędnego klucza (grupa) — Arrays.sort na Object[] to TimSort (stabilny)
        Arrays.sort(stable, Comparator.comparingInt(Person::group));
        showEach("stabilnie wg grupy (Arrays.sort, TimSort)", List.of(stable));
        // WYNIK: stabilnie wg grupy (Arrays.sort, TimSort) (liczba elementów: 4):
        // WYNIK:    • Person[name=Bartek, group=1]
        // WYNIK:    • Person[name=Darek, group=1]
        // WYNIK:    • Person[name=Ala, group=2]
        // WYNIK:    • Person[name=Celina, group=2]

        Person[] unstable = {
                new Person("Ala", 2), new Person("Bartek", 1),
                new Person("Celina", 2), new Person("Darek", 1),
        };
        unstableSelectionSortByGroup(unstable);
        showEach("niestabilnie wg grupy (nasz selection sort)", List.of(unstable));
        // WYNIK: niestabilnie wg grupy (nasz selection sort) (liczba elementów: 4):
        // WYNIK:    • Person[name=Bartek, group=1]
        // WYNIK:    • Person[name=Darek, group=1]
        // WYNIK:    • Person[name=Celina, group=2]
        // WYNIK:    • Person[name=Ala, group=2]

        note("stabilnie: Ala→Celina (oryginalna kolejność); niestabilnie: Celina→Ala — zamiana przy wyborze minimum przeskoczyła nad Alą");

        // PUŁAPKA: selection sort i quick sort są zwykle NIESTABILNE (zamiana "na odległość" gubi kolejność
        //   równych kluczy); insertion sort i merge sort (z warunkiem <=, jak tutaj) SĄ stabilne.
    }

    // =================================================================================================
    // 9. CO UŻYWA JDK: Arrays.sort I Collections.sort
    // =================================================================================================

    /**
     * 9. java.util.Arrays.sort(int[]...) dla typów PRYMITYWNYCH używa dwuoścadłowego szybkiego sortowania
     * (dual-pivot quicksort, dwa piwoty zamiast jednego) — szybkie, ale NIESTABILNE (nie ma znaczenia:
     * prymitywy nie mają "tożsamości" poza wartością). Arrays.sort(Object[]...) i Collections.sort/List.sort
     * używają TimSort — hybrydy merge sort i insertion sort, ZAWSZE stabilnej — bo obiekty (rekordy, encje)
     * zwykle MAJĄ znaczenie poza kluczem sortowania.
     */
    static void jdkSortFacts() {
        section("9. Co używa JDK: Arrays.sort i Collections.sort");

        int[] primitives = {5, 2, 9, 1, 5, 6, 3, 8};
        Arrays.sort(primitives); // dual-pivot quicksort — szybkie, niestabilne (nieistotne dla int)
        show("Arrays.sort(int[]) — dual-pivot quicksort", primitives);
        // WYNIK: Arrays.sort(int[]) — dual-pivot quicksort → [1, 2, 3, 5, 5, 6, 8, 9]

        List<Integer> boxed = new ArrayList<>(List.of(5, 2, 9, 1, 5, 6, 3, 8));
        boxed.sort(Comparator.naturalOrder()); // List.sort / Collections.sort — TimSort, stabilne
        show("list.sort(Integer) — TimSort", boxed);
        // WYNIK: list.sort(Integer) — TimSort → [1, 2, 3, 5, 5, 6, 8, 9]

        note("ten sam wynik liczbowo, ale różne algorytmy pod spodem — wybór zależy od tego, czy sortujemy prymitywy, czy obiekty");

        // DOBRA PRAKTYKA: nie implementuj własnego sortowania produkcyjnie — Arrays.sort/Collections.sort są przetestowane i szybsze.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Bąbelkowe / przez wybieranie / przez wstawianie: O(n²), proste, uczą mechanizmu.
     *   • Przez wybieranie: zawsze n*(n-1)/2 porównań, mało zamian (dobre, gdy zamiana jest kosztowna).
     *   • Przez wstawianie: szybkie na niemal posortowanych danych (bliskie O(n)).
     *   • Przez scalanie: zawsze O(n log n), potrzebuje O(n) dodatkowej pamięci, stabilne.
     *   • Szybkie: średnio O(n log n), w najgorszym razie O(n²) (zły wybór osi na posortowanych danych).
     *   • Stabilność: równe klucze zachowują względną kolejność — ważne przy sortowaniu po wielu kluczach.
     *   • JDK: prymitywy → dual-pivot quicksort; obiekty (Arrays.sort(Object[]), Collections.sort, List.sort) → TimSort (stabilne).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego selection sort zawsze robi n*(n-1)/2 porównań, niezależnie od danych wejściowych?
     *   2. Co wypisze:  int[] a = {3, 1, 2}; insertionSortCounted(a, new long[1], new long[1]);
     *      System.out.println(java.util.Arrays.toString(a));  ?
     *   3. ZNAJDŹ BŁĄD: ktoś pisze quicksort z pivotem = arr[low] (pierwszy element) i testuje go TYLKO
     *      na losowych danych, uznając wydajność za dobrą. Czego nie sprawdził?
     *   4. Dlaczego merge sort gwarantuje O(n log n) NIEZALEŻNIE od danych, a quicksort — nie?
     *   5. Czym różni się sortowanie stabilne od niestabilnego i kiedy ma to znaczenie w praktyce?
     *   6. ZNAJDŹ BŁĄD: "quicksort jest zawsze szybszy niż merge sort, bo się tak nazywa" — co jest nie tak
     *      w tym rozumowaniu?
     *   7. Dlaczego Arrays.sort używa różnych algorytmów dla int[] i dla Integer[]/List<Integer>?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: liczba zamian bąbelkowego sortowania [3, 2, 1]",
                3L, () -> exercise1(new int[] {3, 2, 1}));
        Check.equal("ćw. 2: posortowana tablica przez wstawianie", "[1, 2, 3, 5, 8]",
                () -> Arrays.toString(exercise2(new int[] {8, 3, 5, 1, 2})));
        Check.equal("ćw. 3: indeks osi po jednym podziale quicksort [4, 1, 7, 3, 2] (pivot=2)",
                1, () -> exercise3(new int[] {4, 1, 7, 3, 2}));
        Check.equal("ćw. 4: czy podana para sortowań (stabilne, niestabilne) różni się kolejnością 'A1'/'A2'",
                true, () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 3L, () -> solution1(new int[] {3, 2, 1}));
        Check.equal("ćw. 2 (wzorzec)", "[1, 2, 3, 5, 8]", () -> Arrays.toString(solution2(new int[] {8, 3, 5, 1, 2})));
        Check.equal("ćw. 3 (wzorzec)", 1, () -> solution3(new int[] {4, 1, 7, 3, 2}));
        Check.equal("ćw. 4 (wzorzec)", true, () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć liczbę ZAMIAN, jakie wykona bubbleSortCounted na podanej tablicy.
     * Podpowiedź: wywołaj bubbleSortCounted z własnymi licznikami i odczytaj swaps[0].
     */
    static long exercise1(int[] arr) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (łatwe): posortuj tablicę metodą przez wstawianie i zwróć ją (bez liczenia operacji).
     * Podpowiedź: skopiuj logikę insertionSortCounted, ale możesz zignorować liczniki (np. użyj lokalnych long[1]).
     */
    static int[] exercise2(int[] arr) {
        // TODO: twoje rozwiązanie
        return new int[0];
    }

    /**
     * ĆWICZENIE 3 (średnie): wykonaj JEDEN podział (partition, schemat Lomuto, pivot = ostatni element)
     * na podanej tablicy i zwróć indeks, na którym wylądował pivot.
     * Podpowiedź: skopiuj partitionCounted (możesz zignorować licznik porównań).
     */
    static int exercise3(int[] arr) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zbuduj dwie tablice Person: [Person("A1", 5), Person("A2", 5)] (dwa REKORDY
     * o RÓWNYM kluczu grupy, różniące się tylko imieniem) i sprawdź, czy po posortowaniu stabilnym
     * (Arrays.sort z Comparator.comparingInt(Person::group)) kolejność "A1" przed "A2" jest ZACHOWANA.
     * Zwróć true, jeśli po sortowaniu stable[0].name() nadal równa się "A1".
     * Podpowiedź: dla sortowania stabilnego kolejność elementów o równym kluczu NIGDY się nie zmienia.
     */
    static boolean exercise4() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static long solution1(int[] arr) {
        long[] comparisons = {0};
        long[] swaps = {0};
        bubbleSortCounted(arr, comparisons, swaps);
        return swaps[0];
    }

    static int[] solution2(int[] arr) {
        insertionSortCounted(arr, new long[1], new long[1]);
        return arr;
    }

    static int solution3(int[] arr) {
        return partitionCounted(arr, 0, arr.length - 1, new long[1]);
    }

    static boolean solution4() {
        Person[] stable = {new Person("A1", 5), new Person("A2", 5)};
        Arrays.sort(stable, Comparator.comparingInt(Person::group));
        return stable[0].name().equals("A1");
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo dla KAŻDEGO i przeszukuje CAŁĄ resztę tablicy w poszukiwaniu minimum — ta pętla wewnętrzna
     *      wykonuje się tyle samo razy niezależnie od tego, czy dane są posortowane, czy nie.
     *   2. [1, 2, 3] — insertion sort poprawnie sortuje każdą tablicę, niezależnie od liczników.
     *   3. Nie sprawdził przypadku pesymistycznego: już posortowanych (lub odwrotnie posortowanych) danych,
     *      na których pivot = pierwszy/ostatni element daje O(n²) zamiast O(n log n).
     *   4. Merge sort ZAWSZE dzieli dokładnie na pół, niezależnie od wartości — gwarantuje log n poziomów.
     *      Quicksort dzieli według WARTOŚCI pivota — przy złym wyborze podział bywa skrajnie nierówny (1 do n-1).
     *   5. Stabilne zachowuje względną kolejność elementów o równym kluczu; ma znaczenie przy sortowaniu
     *      po kilku kluczach (np. najpierw wg imienia, potem stabilnie wg działu) — bez stabilności kolejność
     *      drugorzędna ginie.
     *   6. Nazwa nic nie gwarantuje — na już posortowanych danych ze złym wyborem pivota quicksort jest
     *      WOLNIEJSZY (O(n²)) niż merge sort (zawsze O(n log n)). "Szybkie" to nazwa historyczna, nie gwarancja.
     *   7. Bo int[] przechowuje same wartości (stabilność nic by nie znaczyła) — można użyć szybszego,
     *      niestabilnego dual-pivot quicksort. Integer[]/List<Integer> to OBIEKTY, które często niosą dodatkowe
     *      znaczenie poza kluczem — tam stabilność (TimSort) bywa ważna.
     */
    // </editor-fold>
}
