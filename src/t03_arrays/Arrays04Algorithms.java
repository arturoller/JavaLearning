package t03_arrays;

import helpers.Check;

import java.util.Arrays;
import java.util.Random;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Algorytmy na tablicach pisane ręcznie
 *        (algorithm = algorytm, przepis krok po kroku; two pointers = dwa wskaźniki; complexity = złożoność)
 *
 * W SKRÓCIE:
 *   Klasa Arrays robi wiele rzeczy za nas, ale każdy programista powinien umieć samodzielnie napisać
 *   podstawowe algorytmy: odwracanie, wyszukiwanie liniowe i binarne, sortowanie bąbelkowe, zliczanie,
 *   przesuwanie, usuwanie duplikatów i scalanie. Przy okazji uczymy się oceniać, ILE PRACY wykonuje
 *   algorytm dla n elementów: O(n), O(log n), O(n²).
 *
 * ANALOGIA: szukanie słowa w słowniku.
 *   Wyszukiwanie liniowe = czytasz słownik od pierwszej strony, słowo po słowie (O(n)).
 *   Wyszukiwanie binarne = otwierasz w połowie, patrzysz „przed czy za?”, odrzucasz pół słownika
 *   i powtarzasz (O(log n)) — działa TYLKO dlatego, że słownik jest posortowany.
 *
 * JAK TO DZIAŁA:
 *   Złożoność (intuicyjnie) — jak rośnie liczba kroków, gdy danych przybywa:
 *     O(1)      stała liczba kroków (a[i], length)                 n = 1 000 000 →          1 krok
 *     O(log n)  za każdym razem połowa danych odpada (binarne)     n = 1 000 000 →        ~20 kroków
 *     O(n)      jeden przebieg po wszystkich (suma, max, liniowe)  n = 1 000 000 →  1 000 000 kroków
 *     O(n²)     pętla w pętli (bąbelkowe)                          n = 1 000 000 → 10^12 kroków (!)
 *   Dwa wskaźniki (two pointers): dwa indeksy idą po tablicy (z dwóch końców albo „czytający” i „piszący”),
 *   dzięki czemu wiele zadań da się zrobić w jednym przebiegu, bez dodatkowej tablicy.
 *
 * SŁÓWKA:
 *   linear search = wyszukiwanie liniowe; binary search = wyszukiwanie binarne; bubble sort = sortowanie bąbelkowe;
 *   pass = przebieg; swap = zamiana; frequency = częstość; rotate = obrót (przesunięcie cykliczne);
 *   duplicate = duplikat; merge = scalanie; in place = w miejscu (bez nowej tablicy); low/high/mid = dół/góra/środek
 *
 * ZOBACZ TEŻ: t03_arrays/Arrays03Utility (gotowe Arrays.sort i binarySearch),
 *   t24_algorithms/Algorithms01Complexity (złożoność dokładnie), t24_algorithms/Algorithms02Sorting,
 *   t24_algorithms/Algorithms03Searching
 * </pre>
 */
public class Arrays04Algorithms {

    public static void main(String[] args) {
        title("Arrays04 — algorytmy na tablicach");

        sumAvgMinMax();         // sum avg min max = suma, średnia, minimum, maksimum
        reverseInPlace();       // reverse in place = odwracanie w miejscu
        linearSearchDemo();     // linear search demo = wyszukiwanie liniowe
        binarySearchDemo();     // binary search demo = wyszukiwanie binarne
        bubbleSortDemo();       // bubble sort demo = sortowanie bąbelkowe
        countingFrequencies();  // counting frequencies = zliczanie częstości
        rotateByOne();          // rotate by one = przesunięcie cykliczne o jeden
        removeDuplicatesDemo(); // remove duplicates demo = usuwanie duplikatów
        isSortedDemo();         // is sorted demo = czy posortowana
        mergeDemo();            // merge demo = scalanie dwóch posortowanych
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. SUMA, ŚREDNIA, MIN, MAX W JEDNYM PRZEBIEGU — O(n)
    // =================================================================================================

    /**
     * 1. Wszystkie cztery statystyki w jednej pętli. Każdy element oglądamy raz → O(n).
     */
    static void sumAvgMinMax() {
        section("1. Suma, średnia, min, max — jeden przebieg, O(n)");

        int[] data = {14, -3, 27, 8, 0, 19}; // data = dane
        int sum = 0;
        int min = data[0];
        int max = data[0];
        for (int x : data) {
            sum += x;
            if (x < min) {
                min = x;
            }
            if (x > max) {
                max = x;
            }
        }
        double avg = (double) sum / data.length; // avg = average = średnia
        System.out.println("suma=" + sum + ", średnia=" + avg + ", min=" + min + ", max=" + max);
        // WYNIK: suma=65, średnia=10.833333333333334, min=-3, max=27
        // Ostatnia cyfra średniej to efekt zapisu double (t01_basics/Basics08FloatingPoint) — do wyświetlania użyj printf.
    }

    // =================================================================================================
    // 2. ODWRACANIE W MIEJSCU — DWA WSKAŹNIKI
    // =================================================================================================

    /**
     * 2. Dwa wskaźniki: left od początku, right od końca. Zamieniamy elementy i idziemy do środka.
     * n/2 zamian, bez dodatkowej tablicy.
     */
    static void reverseInPlace() {
        section("2. Odwracanie w miejscu (dwa wskaźniki)");

        int[] a = {1, 2, 3, 4, 5, 6};
        int left = 0;              // left = lewy wskaźnik
        int right = a.length - 1;  // right = prawy wskaźnik
        while (left < right) {
            int tmp = a[left];     // zamiana (swap) przez zmienną tymczasową
            a[left] = a[right];
            a[right] = tmp;
            System.out.println("zamiana [" + left + "] ↔ [" + right + "] → " + Arrays.toString(a));
            left++;
            right--;
        }
        // WYNIK: zamiana [0] ↔ [5] → [6, 2, 3, 4, 5, 1]
        // WYNIK: zamiana [1] ↔ [4] → [6, 5, 3, 4, 2, 1]
        // WYNIK: zamiana [2] ↔ [3] → [6, 5, 4, 3, 2, 1]

        // PUŁAPKA: zamiana bez zmiennej tymczasowej  a[left] = a[right]; a[right] = a[left];
        // gubi jedną wartość — po pierwszym przypisaniu stara a[left] już nie istnieje.
        // PUŁAPKA: warunek left <= right też działa (środkowy element zamieni się sam ze sobą), ale pętla po
        // CAŁEJ tablicy (i od 0 do length - 1) odwróci ją dwa razy — czyli wróci do stanu początkowego!
    }

    // =================================================================================================
    // 3. WYSZUKIWANIE LINIOWE — O(n)
    // =================================================================================================

    /**
     * 3. Sprawdzamy element po elemencie. Działa na każdej tablicy (także nieposortowanej).
     */
    static void linearSearchDemo() {
        section("3. Wyszukiwanie liniowe — O(n)");

        int[] data = {15, 8, 23, 4, 42, 16};
        show("linearSearch(data, 42)", linearSearch(data, 42)); // linear search = wyszukiwanie liniowe
        // WYNIK: linearSearch(data, 42) → 4
        show("linearSearch(data, 7)", linearSearch(data, 7));
        // WYNIK: linearSearch(data, 7) → -1
        // Najgorszy przypadek (brak elementu) = n porównań. Dla 6 elementów to nic, dla 10 milionów — sporo.
    }

    /** Zwraca indeks pierwszego wystąpienia target albo -1. */
    static int linearSearch(int[] a, int target) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] == target) {
                return i;
            }
        }
        return -1;
    }

    // =================================================================================================
    // 4. WYSZUKIWANIE BINARNE — O(log n), ZE ŚLADEM KROKÓW
    // =================================================================================================

    /**
     * 4. Wyszukiwanie binarne w POSORTOWANEJ tablicy: patrzymy na środek zakresu i odrzucamy
     * połowę, w której elementu na pewno nie ma.
     */
    static void binarySearchDemo() {
        section("4. Wyszukiwanie binarne — O(log n)");

        int[] sorted = {3, 8, 12, 17, 23, 31, 42, 56, 67, 90};
        int found = binarySearchTraced(sorted, 12);
        // WYNIK: krok 1: low=0, high=9, mid=4 (23)
        // WYNIK: krok 2: low=0, high=3, mid=1 (8)
        // WYNIK: krok 3: low=2, high=3, mid=2 (12)
        show("12 znalezione na indeksie", found);
        // WYNIK: 12 znalezione na indeksie → 2

        int missing = binarySearchTraced(sorted, 50); // missing = brakujący
        // WYNIK: krok 1: low=0, high=9, mid=4 (23)
        // WYNIK: krok 2: low=5, high=9, mid=7 (56)
        // WYNIK: krok 3: low=5, high=6, mid=5 (31)
        // WYNIK: krok 4: low=6, high=6, mid=6 (42)
        show("wynik dla 50", missing);
        // WYNIK: wynik dla 50 → -1
        // 10 elementów → najwyżej 4 kroki. Milion elementów → najwyżej 20 kroków. To jest siła O(log n).

        // PUŁAPKA: mid = (low + high) / 2 dla OGROMNYCH tablic może przepełnić int (suma większa niż 2^31 - 1).
        // Bezpiecznie: mid = low + (high - low) / 2.
    }

    /** Wyszukiwanie binarne wypisujące każdy krok. Zwraca indeks albo -1. */
    static int binarySearchTraced(int[] sorted, int target) {
        int low = 0;                     // low = dolna granica zakresu
        int high = sorted.length - 1;    // high = górna granica zakresu
        int step = 0;                    // step = numer kroku
        while (low <= high) {
            int mid = low + (high - low) / 2;   // mid = środek
            step++;
            System.out.println("krok " + step + ": low=" + low + ", high=" + high + ", mid=" + mid + " (" + sorted[mid] + ")");
            if (sorted[mid] == target) {
                return mid;
            }
            if (sorted[mid] < target) {
                low = mid + 1;           // szukany jest większy → odrzucamy lewą połowę razem ze środkiem
            } else {
                high = mid - 1;          // szukany jest mniejszy → odrzucamy prawą połowę
            }
        }
        return -1;
    }

    // =================================================================================================
    // 5. SORTOWANIE BĄBELKOWE — O(n²), ZE ŚLADEM PRZEBIEGÓW
    // =================================================================================================

    /**
     * 5. Sortowanie bąbelkowe: porównujemy sąsiadów i zamieniamy, jeśli stoją w złej kolejności.
     * Po każdym przebiegu największy element „wypływa” na koniec jak bąbelek.
     */
    static void bubbleSortDemo() {
        section("5. Sortowanie bąbelkowe — O(n²)");

        int[] a = {5, 1, 4, 2, 8};
        for (int pass = 0; pass < a.length - 1; pass++) {         // pass = przebieg
            boolean swapped = false;                              // swapped = czy była zamiana
            for (int i = 0; i < a.length - 1 - pass; i++) {       // końcówka jest już posortowana
                if (a[i] > a[i + 1]) {
                    int tmp = a[i];
                    a[i] = a[i + 1];
                    a[i + 1] = tmp;
                    swapped = true;
                }
            }
            System.out.println("przebieg " + (pass + 1) + ": " + Arrays.toString(a));
            if (!swapped) {
                break;   // przebieg bez zamian = tablica posortowana, nie ma co dalej robić
            }
        }
        // WYNIK: przebieg 1: [1, 4, 2, 5, 8]
        // WYNIK: przebieg 2: [1, 2, 4, 5, 8]
        // WYNIK: przebieg 3: [1, 2, 4, 5, 8]
        // Przebieg 3 nie zamienił niczego — dzięki fladze swapped kończymy wcześniej (bez przebiegu 4).

        // Pętla w pętli po n elementach → około n² porównań. Dla 10 elementów to ~100, dla 100 000 — ~10 miliardów.
        // DOBRA PRAKTYKA: w prawdziwym kodzie używaj Arrays.sort (O(n log n)). Bąbelkowe pisze się dla nauki.
    }

    // =================================================================================================
    // 6. ZLICZANIE CZĘSTOŚCI — TABLICA LICZNIKÓW
    // =================================================================================================

    /**
     * 6. Gdy wartości to małe liczby całkowite (np. oczka kostki 1..6), tablica liczników counts[wartość]
     * zlicza wystąpienia w jednym przebiegu.
     */
    static void countingFrequencies() {
        section("6. Zliczanie częstości: rzuty kostką");

        Random random = new Random(42);   // ziarno 42 → zawsze te same „losowe” rzuty
        int[] counts = new int[7];        // counts = liczniki; indeksy 1..6 = oczka, indeks 0 nieużywany
        for (int i = 0; i < 60; i++) {
            int dice = random.nextInt(6) + 1; // dice = kostka
            counts[dice]++;                   // wartość rzutu JEST indeksem licznika
        }
        for (int face = 1; face <= 6; face++) { // face = ścianka kostki
            System.out.println(face + ": " + "*".repeat(counts[face]) + " (" + counts[face] + ")"); // repeat = powtórz (Java 11+)
        }
        // WYNIK: 1: *********** (11)
        // WYNIK: 2: *************** (15)
        // WYNIK: 3: ************* (13)
        // WYNIK: 4: ***** (5)
        // WYNIK: 5: ***** (5)
        // WYNIK: 6: *********** (11)
        // „Średnio” wypadłoby po 10, ale przy 60 rzutach losowe wahania są duże. Przy 60 000 rzutów byłoby równiej.

        // Ten sam trik: litery 'a'..'z' → counts[c - 'a'], oceny 1..6 → counts[ocena], wiek 0..120 → counts[wiek].
        // PUŁAPKA: wartość spoza zakresu (np. 7 na kostce) → ArrayIndexOutOfBoundsException. Sprawdź zakres danych.
    }

    // =================================================================================================
    // 7. PRZESUNIĘCIE CYKLICZNE O JEDEN
    // =================================================================================================

    /**
     * 7. Obrót w lewo: pierwszy element wędruje na koniec, reszta przesuwa się o jeden w lewo.
     * Obrót w prawo: odwrotnie — i pętla musi iść OD KOŃCA.
     */
    static void rotateByOne() {
        section("7. Przesunięcie cykliczne o jeden");

        int[] a = {1, 2, 3, 4, 5};
        int first = a[0];                          // zapamiętaj, zanim nadpiszesz
        for (int i = 0; i < a.length - 1; i++) {
            a[i] = a[i + 1];
        }
        a[a.length - 1] = first;
        show("obrót w lewo", Arrays.toString(a));
        // WYNIK: obrót w lewo → [2, 3, 4, 5, 1]

        int last = a[a.length - 1];
        for (int i = a.length - 1; i > 0; i--) {  // od końca! (patrz PUŁAPKA)
            a[i] = a[i - 1];
        }
        a[0] = last;
        show("obrót w prawo (powrót)", Arrays.toString(a));
        // WYNIK: obrót w prawo (powrót) → [1, 2, 3, 4, 5]

        // PUŁAPKA: przesuwanie w prawo pętlą OD POCZĄTKU nadpisuje elementy, zanim zostaną przeniesione:
        int[] b = {1, 2, 3, 4, 5};
        int lastB = b[b.length - 1];
        for (int i = 1; i < b.length; i++) {
            b[i] = b[i - 1];                       // b[1] = 1, potem b[2] = b[1] = 1, ...
        }
        b[0] = lastB;
        show("obrót w prawo pętlą od początku (błąd)", Arrays.toString(b));
        // WYNIK: obrót w prawo pętlą od początku (błąd) → [5, 1, 1, 1, 1]
    }

    // =================================================================================================
    // 8. USUWANIE DUPLIKATÓW Z POSORTOWANEJ TABLICY
    // =================================================================================================

    /**
     * 8. Dwa wskaźniki: read (czytający) idzie po wszystkich elementach, write (piszący) wskazuje miejsce
     * na następny unikalny element. Zwracamy nową „logiczną” długość.
     */
    static void removeDuplicatesDemo() {
        section("8. Usuwanie duplikatów z posortowanej tablicy (dwa wskaźniki)");

        int[] d = {1, 1, 2, 3, 3, 3, 7, 9, 9};
        int length = removeDuplicates(d); // length = nowa długość
        show("liczba unikalnych", length);
        // WYNIK: liczba unikalnych → 5
        show("unikalne (pierwsze length elementów)", Arrays.toString(Arrays.copyOf(d, length)));
        // WYNIK: unikalne (pierwsze length elementów) → [1, 2, 3, 7, 9]
        show("cała tablica po operacji", Arrays.toString(d));
        // WYNIK: cała tablica po operacji → [1, 2, 3, 7, 9, 3, 7, 9, 9]
        // Tablicy nie da się skrócić — za „logicznym końcem” zostają stare śmieci. Ważne jest tylko pierwsze length elementów.
        // Działa w O(n) TYLKO dla posortowanej tablicy — duplikaty stoją wtedy obok siebie.
    }

    /** Przenosi unikalne elementy na początek posortowanej tablicy; zwraca ich liczbę. */
    static int removeDuplicates(int[] sorted) {
        if (sorted.length == 0) {
            return 0;
        }
        int write = 1;                                         // write = indeks „piszący”
        for (int read = 1; read < sorted.length; read++) {     // read = indeks „czytający”
            if (sorted[read] != sorted[write - 1]) {           // nowa wartość (inna niż ostatnia zapisana)
                sorted[write] = sorted[read];
                write++;
            }
        }
        return write;
    }

    // =================================================================================================
    // 9. CZY TABLICA JEST POSORTOWANA?
    // =================================================================================================

    /**
     * 9. Wystarczy sprawdzić każdą parę sąsiadów. Pierwsza para w złej kolejności → od razu false.
     */
    static void isSortedDemo() {
        section("9. Czy tablica jest posortowana?");

        show("isSorted({1, 2, 2, 5})", isSorted(new int[] {1, 2, 2, 5})); // is sorted = czy posortowana
        // WYNIK: isSorted({1, 2, 2, 5}) → true
        show("isSorted({1, 3, 2})", isSorted(new int[] {1, 3, 2}));
        // WYNIK: isSorted({1, 3, 2}) → false
        show("isSorted({})", isSorted(new int[] {}));
        // WYNIK: isSorted({}) → true
        // Pusta tablica i tablica z jednym elementem są posortowane „z definicji” — nie ma pary w złej kolejności.
        // Przydatne przed binarySearch: jeśli nie wiesz, czy dane są posortowane — sprawdź (O(n)) albo posortuj.
    }

    /** true, gdy każdy element jest mniejszy lub równy następnemu. */
    static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) {   // od 1, bo porównujemy z poprzednim — brak wyjścia poza tablicę
            if (a[i - 1] > a[i]) {
                return false;
            }
        }
        return true;
    }

    // =================================================================================================
    // 10. SCALANIE DWÓCH POSORTOWANYCH TABLIC — O(n + m)
    // =================================================================================================

    /**
     * 10. Scalanie: dwa wskaźniki, po jednym na każdą tablicę. Zawsze bierzemy mniejszy z dwóch
     * „czołowych” elementów. Na końcu dopisujemy resztę tej tablicy, która się nie skończyła.
     */
    static void mergeDemo() {
        section("10. Scalanie dwóch posortowanych tablic");

        int[] a = {1, 4, 9};
        int[] b = {2, 3, 10, 12};
        show("merge(a, b)", Arrays.toString(merge(a, b))); // merge = scal
        // WYNIK: merge(a, b) → [1, 2, 3, 4, 9, 10, 12]
        // Każdy element trafia do wyniku raz → O(n + m). To serce sortowania przez scalanie (merge sort):
        // t24_algorithms/Algorithms02Sorting.
    }

    /** Scala dwie posortowane tablice w jedną posortowaną. */
    static int[] merge(int[] a, int[] b) {
        int[] result = new int[a.length + b.length];
        int i = 0; // wskaźnik w a
        int j = 0; // wskaźnik w b
        int k = 0; // wskaźnik w result
        while (i < a.length && j < b.length) {
            if (a[i] <= b[j]) {            // <= : przy remisie najpierw element z a (stabilność)
                result[k] = a[i];
                i++;
            } else {
                result[k] = b[j];
                j++;
            }
            k++;
        }
        while (i < a.length) {             // reszta z a (jeśli b skończyło się pierwsze)
            result[k++] = a[i++];          // k++ w indeksie: użyj k, potem zwiększ (t01_basics/Basics04Operators)
        }
        while (j < b.length) {             // reszta z b
            result[k++] = b[j++];
        }
        return result;
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   ┌──────────────────────────────┬───────────┬─────────────────────────────────────────┐
     *   │ algorytm                     │ złożoność │ klucz                                   │
     *   ├──────────────────────────────┼───────────┼─────────────────────────────────────────┤
     *   │ suma / min / max / średnia   │ O(n)      │ jeden przebieg, akumulatory             │
     *   │ odwracanie w miejscu         │ O(n)      │ left/right, zamiana przez tmp           │
     *   │ wyszukiwanie liniowe         │ O(n)      │ return i przy trafieniu, -1 na końcu    │
     *   │ wyszukiwanie binarne         │ O(log n)  │ TYLKO posortowana; low/high/mid         │
     *   │ sortowanie bąbelkowe         │ O(n²)     │ zamiany sąsiadów + flaga swapped        │
     *   │ zliczanie częstości          │ O(n)      │ counts[wartość]++                       │
     *   │ obrót o jeden                │ O(n)      │ zapamiętaj element; w prawo — od końca  │
     *   │ usuwanie duplikatów (sort.)  │ O(n)      │ read/write, zwróć nową długość          │
     *   │ czy posortowana              │ O(n)      │ porównuj a[i-1] z a[i]                  │
     *   │ scalanie posortowanych       │ O(n + m)  │ i/j/k, weź mniejszy, dopisz resztę      │
     *   └──────────────────────────────┴───────────┴─────────────────────────────────────────┘
     *   • Zamiana dwóch elementów zawsze przez zmienną tymczasową.
     *   • mid = low + (high - low) / 2  (bez ryzyka przepełnienia).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego wyszukiwanie binarne wymaga posortowanej tablicy?
     *   2. Ile najwyżej kroków potrzebuje wyszukiwanie binarne dla 1000 elementów? A liniowe?
     *   3. Co wypisze:  int[] a = {3, 1, 2};  int t = a[0];  a[0] = a[2];  a[2] = t;  System.out.println(Arrays.toString(a));  ?
     *   4. ZNAJDŹ BŁĄD (odwracanie):  for (int i = 0; i < a.length; i++) { int t = a[i]; a[i] = a[a.length - 1 - i]; a[a.length - 1 - i] = t; }
     *   5. ZNAJDŹ BŁĄD (obrót w prawo):  for (int i = 1; i < a.length; i++) { a[i] = a[i - 1]; }
     *   6. Po co w sortowaniu bąbelkowym flaga swapped?
     *   7. Co zwróci removeDuplicates dla {4, 4, 4, 4}? Co będzie w tablicy?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: ile większych od progu", "3, 0",
                () -> exercise1(new int[] {5, 12, 7, 20, 3}, 6) + ", " + exercise1(new int[] {}, 0));
        Check.equal("ćw. 2: druga największa (różna) wartość", "7, -5, 2",
                () -> exercise2(new int[] {5, 9, 3, 9, 7}) + ", " + exercise2(new int[] {-1, -5}) + ", "
                        + exercise2(new int[] {4, 4, 2}));
        Check.equal("ćw. 3: odwracanie w miejscu", "[4, 3, 2, 1] | [5, 4, 3, 2, 1] | []", () -> {
            int[] even = {1, 2, 3, 4};
            int[] odd = {1, 2, 3, 4, 5};
            int[] empty = {};
            exercise3(even);
            exercise3(odd);
            exercise3(empty);
            return Arrays.toString(even) + " | " + Arrays.toString(odd) + " | " + Arrays.toString(empty);
        });
        Check.equal("ćw. 4: część wspólna posortowanych", "[3, 7, 9] | []",
                () -> Arrays.toString(exercise4(new int[] {1, 3, 4, 7, 9}, new int[] {2, 3, 7, 8, 9, 10})) + " | "
                        + Arrays.toString(exercise4(new int[] {1, 2}, new int[] {3, 4})));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "3, 0",
                () -> solution1(new int[] {5, 12, 7, 20, 3}, 6) + ", " + solution1(new int[] {}, 0));
        Check.equal("ćw. 2 (wzorzec)", "7, -5, 2",
                () -> solution2(new int[] {5, 9, 3, 9, 7}) + ", " + solution2(new int[] {-1, -5}) + ", "
                        + solution2(new int[] {4, 4, 2}));
        Check.equal("ćw. 3 (wzorzec)", "[4, 3, 2, 1] | [5, 4, 3, 2, 1] | []", () -> {
            int[] even = {1, 2, 3, 4};
            int[] odd = {1, 2, 3, 4, 5};
            int[] empty = {};
            solution3(even);
            solution3(odd);
            solution3(empty);
            return Arrays.toString(even) + " | " + Arrays.toString(odd) + " | " + Arrays.toString(empty);
        });
        Check.equal("ćw. 4 (wzorzec)", "[3, 7, 9] | []",
                () -> Arrays.toString(solution4(new int[] {1, 3, 4, 7, 9}, new int[] {2, 3, 7, 8, 9, 10})) + " | "
                        + Arrays.toString(solution4(new int[] {1, 2}, new int[] {3, 4})));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz elementy większe od threshold (próg).
     * Podpowiedź: licznik + for-each + if — O(n).
     */
    static int exercise1(int[] a, int threshold) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć drugą największą RÓŻNĄ wartość w jednym przebiegu (bez sortowania).
     * {5, 9, 3, 9, 7} → 7 (druga 9 się nie liczy). Zakładamy co najmniej dwie różne wartości.
     * Podpowiedź: dwie zmienne first i second, start od Integer.MIN_VALUE. Nowy rekord: second = first, first = x.
     * Wartość mniejsza od first, ale większa od second: second = x. Równa first — pomiń.
     */
    static int exercise2(int[] a) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ tak, żeby odwracać W MIEJSCU (bez nowej tablicy), dwoma wskaźnikami.
     * Metoda nic nie zwraca — ma zmienić przekazaną tablicę.
     * <pre>{@code
     * int[] reversed = new int[a.length];
     * for (int i = 0; i < a.length; i++) {
     *     reversed[i] = a[a.length - 1 - i];
     * }
     * return reversed;
     * }</pre>
     * Podpowiedź: sekcja 2 — left, right, while (left < right), zamiana przez tmp.
     */
    static void exercise3(int[] a) {
        // TODO: twoje rozwiązanie
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): część wspólna dwóch POSORTOWANYCH tablic bez powtórzeń wewnątrz każdej.
     * {1, 3, 4, 7, 9} i {2, 3, 7, 8, 9, 10} → [3, 7, 9]. Wynik ma mieć dokładnie tyle elementów, ile trzeba.
     * Podpowiedź: dwa wskaźniki jak w merge — równe: zapisz i przesuń oba; mniejszy: przesuń tylko jego.
     * Tablica tymczasowa o długości Math.min(a.length, b.length), na koniec Arrays.copyOf(temp, licznik).
     */
    static int[] exercise4(int[] a, int[] b) {
        // TODO: twoje rozwiązanie
        return new int[0];
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int[] a, int threshold) {
        int count = 0;
        for (int x : a) {
            if (x > threshold) {
                count++;
            }
        }
        return count;
    }

    static int solution2(int[] a) {
        int first = Integer.MIN_VALUE;
        int second = Integer.MIN_VALUE;
        for (int x : a) {
            if (x > first) {
                second = first;
                first = x;
            } else if (x < first && x > second) {
                second = x;
            }
        }
        return second;
    }

    static void solution3(int[] a) {
        int left = 0;
        int right = a.length - 1;
        while (left < right) {
            int tmp = a[left];
            a[left] = a[right];
            a[right] = tmp;
            left++;
            right--;
        }
    }

    static int[] solution4(int[] a, int[] b) {
        int[] temp = new int[Math.min(a.length, b.length)];
        int count = 0;
        int i = 0;
        int j = 0;
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) {
                temp[count] = a[i];
                count++;
                i++;
                j++;
            } else if (a[i] < b[j]) {
                i++;
            } else {
                j++;
            }
        }
        return Arrays.copyOf(temp, count);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo po porównaniu ze środkiem odrzuca połowę, zakładając, że mniejsze elementy są po lewej,
     *      a większe po prawej. Bez sortowania to założenie jest fałszywe.
     *   2. Binarne: około 10 kroków (2^10 = 1024). Liniowe: do 1000 porównań.
     *   3. [2, 1, 3]
     *   4. Pętla idzie przez CAŁĄ tablicę, więc każda para zostaje zamieniona dwa razy — tablica wraca
     *      do stanu początkowego. Pętla powinna iść tylko do połowy (i < a.length / 2) albo left < right.
     *   5. Pętla od początku nadpisuje elementy, zanim zostaną przeniesione — wszystko staje się kopią a[0].
     *      Przy przesuwaniu w prawo idź od końca: for (int i = a.length - 1; i > 0; i--).
     *   6. Żeby zakończyć sortowanie, gdy przebieg nie zrobił żadnej zamiany (tablica już posortowana).
     *      Dla prawie posortowanych danych oszczędza to mnóstwo pracy.
     *   7. Zwróci 1. Tablica się nie zmieni: {4, 4, 4, 4} — ważny jest tylko pierwszy element.
     */
    // </editor-fold>
}
