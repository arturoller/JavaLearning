package t24_algorithms;

import java.util.Arrays;

import helpers.Check;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Klasyczne wzorce algorytmiczne — brute force kontra sprytniejsze O()
 *        (two pointers = dwa wskaźniki; sliding window = przesuwne okno; prefix sum = suma prefiksowa)
 *
 * W SKRÓCIE:
 *   Większość "sztuczek" z rozmów rekrutacyjnych to kilka powtarzalnych WZORCÓW: dwa wskaźniki biegnące
 *   ku sobie albo razem, okno przesuwające się po tablicy, suma prefiksowa liczona raz z góry. Każdy
 *   wzorzec zamienia naiwne O(n²) (albo gorzej) w O(n) — kosztem odrobiny sprytu, nie mocy obliczeniowej.
 *
 * ANALOGIA: skracanie drogi.
 *   Brute force to sprawdzenie WSZYSTKICH tras między dwoma miastami. Te wzorce to zauważenie, że mapa ma
 *   strukturę (drogi, rzeka, granica) i wykorzystanie jej, by od razu iść dobrą drogą.
 *
 * JAK TO DZIAŁA:
 *   Dwa wskaźniki: wykorzystują, że dane są POSORTOWANE, by przesuwać się w jedną stronę bez zawracania.
 *   Przesuwne okno: zamiast liczyć sumę okna od zera za każdym razem, DOKŁADA nowy element i ODEJMUJE stary.
 *   Suma prefiksowa: licz raz sumy "od początku do i", potem odejmowanie dwóch prefiksów = suma dowolnego zakresu.
 *
 * SŁÓWKA:
 *   two pointers = dwa wskaźniki; sliding window = przesuwne okno; prefix sum = suma prefiksowa;
 *   frequency count = zliczanie częstości; subarray = podtablica (ciągły fragment); in place = w miejscu.
 *
 * ZOBACZ TEŻ: t24_algorithms/Algorithms01Complexity (notacja O()), t24_algorithms/Algorithms02Sorting (merge
 *             — scalanie dwóch posortowanych połówek), t03_arrays/Arrays04Algorithms (algorytmy na tablicach).
 * </pre>
 */
public class Algorithms05Classics {

    public static void main(String[] args) {
        title("Algorithms05 — klasyczne wzorce: brute force kontra sprytniejsze O()");

        twoPointersPairSum();     // pair sum = para o danej sumie
        twoPointersDeduplicate(); // deduplicate = usuń duplikaty
        slidingWindow();          // sliding window = przesuwne okno
        prefixSums();             // prefix sums = sumy prefiksowe
        frequencyCounting();      // frequency counting = zliczanie częstości
        kadaneMaxSubarray();      // Kadane's algorithm = algorytm Kadane'a
        dutchNationalFlag();      // Dutch national flag = flaga holenderska
        mergeSortedArrays();      // merge sorted arrays = scalanie posortowanych tablic
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. DWA WSKAŹNIKI — PARA O DANEJ SUMIE (TWO POINTERS)
    // =================================================================================================

    static int[] bruteForcePairSum(int[] arr, int target, long[] comparisons) {
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                comparisons[0]++;
                if (arr[i] + arr[j] == target) return new int[] {i, j};
            }
        }
        return new int[] {-1, -1};
    }

    /** twoPointerPairSum = wskaźniki z DWÓCH KOŃCÓW posortowanej tablicy, zbliżające się ku sobie. */
    static int[] twoPointerPairSum(int[] sorted, int target, long[] steps) {
        int left = 0, right = sorted.length - 1;
        while (left < right) {
            steps[0]++;
            int sum = sorted[left] + sorted[right];
            if (sum == target) return new int[] {left, right};
            if (sum < target) left++; else right--; // suma za mała → przesuń lewy w prawo; za duża → prawy w lewo
        }
        return new int[] {-1, -1};
    }

    /**
     * 1. Ślad dla [1, 2, 4, 7, 11, 15], target=9: left=0(1) right=5(15), {@code suma=16>9} → right-- (1 krok).
     * left=0(1) right=4(11), {@code suma=12>9} → right-- (2 krok). left=0(1) right=3(7), {@code suma=8<9} → left++ (3 krok).
     * left=1(2) right=3(7), suma=9 → ZNALEZIONO (4 krok). (PYTANIE REKRUTACYJNE: two sum na posortowanej tablicy).
     */
    static void twoPointersPairSum() {
        section("1. Dwa wskaźniki — para o danej sumie (two pointers)");

        int[] sorted = {1, 2, 4, 7, 11, 15};
        long[] bruteCmp = {0};
        int[] bruteResult = bruteForcePairSum(sorted, 9, bruteCmp);
        show("brute force — indeksy", Arrays.toString(bruteResult));
        show("brute force — porównań", bruteCmp[0]);
        // WYNIK: brute force — indeksy → [1, 3]
        // WYNIK: brute force — porównań → 7

        long[] twoPtrSteps = {0};
        int[] twoPtrResult = twoPointerPairSum(sorted, 9, twoPtrSteps);
        show("dwa wskaźniki — indeksy", Arrays.toString(twoPtrResult));
        show("dwa wskaźniki — kroków", twoPtrSteps[0]);
        // WYNIK: dwa wskaźniki — indeksy → [1, 3]
        // WYNIK: dwa wskaźniki — kroków → 4

        note("ten sam wynik, ale dwa wskaźniki to O(n) zamiast O(n²) — DZIAŁA TYLKO na POSORTOWANEJ tablicy");
    }

    // =================================================================================================
    // 2. DWA WSKAŹNIKI — USUWANIE DUPLIKATÓW W MIEJSCU (IN PLACE)
    // =================================================================================================

    /** removeDuplicatesInPlace = wskaźnik "write" pisze tylko NOWE wartości; "read" przegląda całość raz. */
    static int removeDuplicatesInPlace(int[] sorted) { // O(n), bez dodatkowej tablicy
        if (sorted.length == 0) return 0;
        int writeIndex = 1;
        for (int readIndex = 1; readIndex < sorted.length; readIndex++) {
            if (sorted[readIndex] != sorted[writeIndex - 1]) {
                sorted[writeIndex] = sorted[readIndex];
                writeIndex++;
            }
        }
        return writeIndex;
    }

    /** 2. Zamiast tworzyć NOWĄ tablicę (O(n) dodatkowej pamięci), nadpisujemy duplikaty W TEJ SAMEJ tablicy. */
    static void twoPointersDeduplicate() {
        section("2. Dwa wskaźniki — usuwanie duplikatów w miejscu (in place)");

        int[] withDuplicates = {1, 1, 2, 2, 2, 3, 4, 4, 5};
        int newLength = removeDuplicatesInPlace(withDuplicates);
        show("nowa długość", newLength);
        show("unikalny prefiks tablicy", Arrays.toString(Arrays.copyOf(withDuplicates, newLength)));
        // WYNIK: nowa długość → 5
        // WYNIK: unikalny prefiks tablicy → [1, 2, 3, 4, 5]

        note("reszta tablicy ZA newLength to teraz śmieci (stare wartości) — liczy się tylko prefiks [0, newLength)");

        // PUŁAPKA: ta sztuczka działa TYLKO na POSORTOWANEJ tablicy — duplikaty muszą sąsiadować ze sobą.
    }

    // =================================================================================================
    // 3. PRZESUWNE OKNO — MAKSYMALNA SUMA K KOLEJNYCH ELEMENTÓW (SLIDING WINDOW)
    // =================================================================================================

    static int bruteForceMaxWindowSum(int[] arr, int k, long[] additions) {
        int maxSum = Integer.MIN_VALUE;
        for (int start = 0; start + k <= arr.length; start++) {
            int sum = 0;
            for (int i = start; i < start + k; i++) { additions[0]++; sum += arr[i]; }
            maxSum = Math.max(maxSum, sum);
        }
        return maxSum;
    }

    /** slidingWindowMaxSum = jedna suma liczona raz, potem tylko DOKŁADAMY nowy element i ODEJMUJEMY stary. */
    static int slidingWindowMaxSum(int[] arr, int k, long[] additions) {
        int windowSum = 0;
        for (int i = 0; i < k; i++) { additions[0]++; windowSum += arr[i]; }
        int maxSum = windowSum;
        for (int i = k; i < arr.length; i++) {
            additions[0]++;
            windowSum += arr[i] - arr[i - k]; // dołóż nowy koniec okna, odejmij wypadający początek
            maxSum = Math.max(maxSum, windowSum);
        }
        return maxSum;
    }

    /** 3. Brute force liczy sumę KAŻDEGO okna od zera (O(n*k)). Przesuwne okno aktualizuje sumę w O(1) na krok. */
    static void slidingWindow() {
        section("3. Przesuwne okno — maksymalna suma k kolejnych elementów (sliding window)");

        int[] arr = {2, 1, 5, 1, 3, 2};
        int k = 3;
        long[] bruteAdds = {0};
        show("brute force — maksimum", bruteForceMaxWindowSum(arr, k, bruteAdds));
        show("brute force — dodawań", bruteAdds[0]);
        // WYNIK: brute force — maksimum → 9
        // WYNIK: brute force — dodawań → 12

        long[] windowAdds = {0};
        show("przesuwne okno — maksimum", slidingWindowMaxSum(arr, k, windowAdds));
        show("przesuwne okno — dodawań", windowAdds[0]);
        // WYNIK: przesuwne okno — maksimum → 9
        // WYNIK: przesuwne okno — dodawań → 6

        note("12 dodawań (brute force) kontra 6 (okno) — dla większego n różnica rośnie z O(n*k) do O(n)");
    }

    // =================================================================================================
    // 4. SUMY PREFIKSOWE — ZAPYTANIA O SUMĘ ZAKRESU (PREFIX SUMS)
    // =================================================================================================

    static int bruteForceRangeSum(int[] arr, int from, int toInclusive, long[] additions) {
        int sum = 0;
        for (int i = from; i <= toInclusive; i++) { additions[0]++; sum += arr[i]; }
        return sum;
    }

    /** buildPrefixSums = prefix[i] = suma elementów [0, i). Dzięki temu suma zakresu [from, to] to ODEJMOWANIE. */
    static int[] buildPrefixSums(int[] arr) {
        int[] prefix = new int[arr.length + 1];
        for (int i = 0; i < arr.length; i++) { prefix[i + 1] = prefix[i] + arr[i]; }
        return prefix;
    }

    static int prefixRangeSum(int[] prefix, int from, int toInclusive) { // O(1) PO zbudowaniu prefixu
        return prefix[toInclusive + 1] - prefix[from];
    }

    /**
     * 4. Zamiast za każdym zapytaniem sumować fragment od nowa (O(n) na zapytanie), liczymy prefiks RAZ
     * (O(n)) — każde kolejne zapytanie o sumę zakresu to już tylko odejmowanie dwóch liczb, O(1).
     */
    static void prefixSums() {
        section("4. Sumy prefiksowe — zapytania o sumę zakresu (prefix sums)");

        int[] arr = {3, 1, 4, 1, 5, 9, 2, 6};
        long[] bruteAdds = {0};
        int brute1 = bruteForceRangeSum(arr, 2, 5, bruteAdds);
        int brute2 = bruteForceRangeSum(arr, 0, 7, bruteAdds);
        show("brute force: suma [2..5] i [0..7]", brute1 + " i " + brute2);
        show("brute force — dodawań łącznie (2 zapytania)", bruteAdds[0]);
        // WYNIK: brute force: suma [2..5] i [0..7] → 19 i 31
        // WYNIK: brute force — dodawań łącznie (2 zapytania) → 12

        int[] prefix = buildPrefixSums(arr);
        int fast1 = prefixRangeSum(prefix, 2, 5);
        int fast2 = prefixRangeSum(prefix, 0, 7);
        show("prefiks: suma [2..5] i [0..7]", fast1 + " i " + fast2);
        // WYNIK: prefiks: suma [2..5] i [0..7] → 19 i 31

        note("im więcej zapytań, tym większa przewaga: 1000 zapytań to O(n) budowy + O(1000), nie O(1000*n)");
    }

    // =================================================================================================
    // 5. ZLICZANIE CZĘSTOŚCI — ANAGRAM I PIERWSZY NIEPOWTARZAJĄCY SIĘ ZNAK
    // =================================================================================================

    /** isAnagram = dwa napisy są anagramami, gdy mają TE SAME litery w TEJ SAMEJ liczbie (kolejność bez znaczenia). */
    static boolean isAnagram(String a, String b) { // O(n), tablica zliczeń zamiast sortowania (co byłoby O(n log n))
        if (a.length() != b.length()) return false;
        int[] counts = new int[26];
        for (char c : a.toCharArray()) { counts[c - 'a']++; }
        for (char c : b.toCharArray()) { counts[c - 'a']--; }
        for (int count : counts) { if (count != 0) return false; }
        return true;
    }

    /** firstUniqueIndex = indeks pierwszego znaku, który występuje DOKŁADNIE raz w całym napisie. */
    static int firstUniqueIndex(String text) { // O(n): jeden przebieg zliczający + jeden przebieg sprawdzający
        int[] counts = new int[26];
        for (char c : text.toCharArray()) { counts[c - 'a']++; }
        for (int i = 0; i < text.length(); i++) {
            if (counts[text.charAt(i) - 'a'] == 1) return i;
        }
        return -1;
    }

    /**
     * 5. Zamiast porównywać każdą literę z każdą (O(n²)) albo sortować napisy (O(n log n)), zliczamy
     * wystąpienia liter w tablicy o 26 komórkach (PYTANIE REKRUTACYJNE: sprawdź, czy dwa napisy to anagramy).
     */
    static void frequencyCounting() {
        section("5. Zliczanie częstości — anagram i pierwszy niepowtarzający się znak");

        show("isAnagram(\"listen\", \"silent\")", isAnagram("listen", "silent"));
        show("isAnagram(\"java\", \"ada\")", isAnagram("java", "ada"));
        show("firstUniqueIndex(\"swiss\")", firstUniqueIndex("swiss"));
        // WYNIK: isAnagram("listen", "silent") → true
        // WYNIK: isAnagram("java", "ada") → false
        // WYNIK: firstUniqueIndex("swiss") → 1

        note("tablica int[26] zamiast Map<Character, Integer> — szybsza i prostsza, gdy znaki to tylko a-z");
    }

    // =================================================================================================
    // 6. ALGORYTM KADANE'A — MAKSYMALNA SUMA PODTABLICY (MAXIMUM SUBARRAY)
    // =================================================================================================

    static int bruteForceMaxSubarray(int[] arr, long[] additions) {
        int best = arr[0];
        for (int i = 0; i < arr.length; i++) {
            int sum = 0;
            for (int j = i; j < arr.length; j++) {
                additions[0]++;
                sum += arr[j];
                best = Math.max(best, sum);
            }
        }
        return best;
    }

    /** kadaneMaxSubarray = w każdym kroku: albo ZACZNIJ nową podtablicę od arr[i], albo DOŁĄCZ go do poprzedniej. */
    static int kadaneMaxSubarray(int[] arr, long[] additions) { // O(n), jeden przebieg
        int best = arr[0];
        int currentSum = arr[0];
        for (int i = 1; i < arr.length; i++) {
            additions[0]++;
            currentSum = Math.max(arr[i], currentSum + arr[i]); // porzuć ujemny "bagaż" albo dołącz
            best = Math.max(best, currentSum);
        }
        return best;
    }

    /**
     * 6. Klasyczne PYTANIE REKRUTACYJNE: największa suma CIĄGŁEGO fragmentu tablicy (z ujemnymi liczbami).
     * Intuicja: jeśli suma "dotychczasowego ciągu" spadnie poniżej samego arr[i], lepiej zacząć od nowa.
     */
    static void kadaneMaxSubarray() {
        section("6. Algorytm Kadane'a — maksymalna suma podtablicy (maximum subarray)");

        int[] arr = {-2, 1, -3, 4, -1, 2, 1, -5, 4};
        long[] bruteAdds = {0};
        show("brute force — maksymalna suma", bruteForceMaxSubarray(arr, bruteAdds));
        show("brute force — dodawań", bruteAdds[0]);
        // WYNIK: brute force — maksymalna suma → 6
        // WYNIK: brute force — dodawań → 45

        long[] kadaneAdds = {0};
        show("Kadane — maksymalna suma", kadaneMaxSubarray(arr, kadaneAdds));
        show("Kadane — dodawań", kadaneAdds[0]);
        // WYNIK: Kadane — maksymalna suma → 6
        // WYNIK: Kadane — dodawań → 8

        note("45 dodawań (O(n²), wszystkie podtablice) kontra 8 (O(n), jeden przebieg) — fragment [4,-1,2,1] sumuje się do 6");
    }

    // =================================================================================================
    // 7. FLAGA HOLENDERSKA — PODZIAŁ NA TRZY GRUPY W JEDNYM PRZEBIEGU (DUTCH NATIONAL FLAG)
    // =================================================================================================

    static void swapInts(int[] arr, int a, int b) {
        int tmp = arr[a];
        arr[a] = arr[b];
        arr[b] = tmp;
    }

    static void bruteForceSortColors(int[] arr, long[] comparisons) { // selection sort — O(n²), nie wykorzystuje faktu "tylko 0/1/2"
        for (int i = 0; i < arr.length - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < arr.length; j++) {
                comparisons[0]++;
                if (arr[j] < arr[minIndex]) minIndex = j;
            }
            swapInts(arr, i, minIndex);
        }
    }

    /** dutchFlagSort = trzy wskaźniki (low/mid/high) grupują 0 na lewo, 2 na prawo, 1 zostaje w środku — JEDEN przebieg. */
    static void dutchFlagSort(int[] arr, long[] steps) { // O(n)
        int low = 0, mid = 0, high = arr.length - 1;
        while (mid <= high) {
            steps[0]++;
            if (arr[mid] == 0) { swapInts(arr, low, mid); low++; mid++; }
            else if (arr[mid] == 1) { mid++; }
            else { swapInts(arr, mid, high); high--; } // arr[mid] == 2 — NIE zwiększaj mid, trzeba sprawdzić "nowy" element
        }
    }

    /**
     * 7. Tablica złożona WYŁĄCZNIE z trzech wartości (0, 1, 2) — zamiast sortować ogólnie (O(n log n)
     * albo tu nawet O(n²) z selection sort), grupujemy w JEDNYM przebiegu, wykorzystując tę wiedzę.
     * (PYTANIE REKRUTACYJNE: posortuj tablicę kolorów 0/1/2 w jednym przebiegu, bez dodatkowej pamięci).
     */
    static void dutchNationalFlag() {
        section("7. Flaga holenderska — podział na trzy grupy w jednym przebiegu (Dutch national flag)");

        int[] forBrute = {2, 0, 2, 1, 1, 0, 0, 2, 1};
        long[] bruteCmp = {0};
        bruteForceSortColors(forBrute, bruteCmp);
        show("selection sort — wynik", Arrays.toString(forBrute));
        show("selection sort — porównań", bruteCmp[0]);
        // WYNIK: selection sort — wynik → [0, 0, 0, 1, 1, 1, 2, 2, 2]
        // WYNIK: selection sort — porównań → 36

        int[] forDutch = {2, 0, 2, 1, 1, 0, 0, 2, 1};
        long[] dutchSteps = {0};
        dutchFlagSort(forDutch, dutchSteps);
        show("flaga holenderska — wynik", Arrays.toString(forDutch));
        show("flaga holenderska — kroków", dutchSteps[0]);
        // WYNIK: flaga holenderska — wynik → [0, 0, 0, 1, 1, 1, 2, 2, 2]
        // WYNIK: flaga holenderska — kroków → 9

        note("36 porównań (O(n²)) kontra 9 kroków (O(n)) — przy tylko 3 możliwych wartościach POGRUPOWANIE to jednocześnie pełne posortowanie");
    }

    // =================================================================================================
    // 8. SCALANIE DWÓCH POSORTOWANYCH TABLIC (MERGE SORTED ARRAYS)
    // =================================================================================================

    static int[] bruteForceMerge(int[] a, int[] b, long[] comparisons) { // skopiuj obie, posortuj od nowa — O(n log n)
        int[] merged = new int[a.length + b.length];
        System.arraycopy(a, 0, merged, 0, a.length);
        System.arraycopy(b, 0, merged, a.length, b.length);
        for (int i = 0; i < merged.length - 1; i++) {
            for (int j = 0; j < merged.length - 1 - i; j++) {
                comparisons[0]++;
                if (merged[j] > merged[j + 1]) swapInts(merged, j, j + 1);
            }
        }
        return merged;
    }

    /** twoPointerMerge = dwa wskaźniki idące RAZEM po obu tablicach — bierz zawsze mniejszy z dwóch "czoła". */
    static int[] twoPointerMerge(int[] a, int[] b, long[] comparisons) { // O(n + m)
        int[] merged = new int[a.length + b.length];
        int i = 0, j = 0, k = 0;
        while (i < a.length && j < b.length) {
            comparisons[0]++;
            merged[k++] = (a[i] <= b[j]) ? a[i++] : b[j++];
        }
        while (i < a.length) { merged[k++] = a[i++]; }
        while (j < b.length) { merged[k++] = b[j++]; }
        return merged;
    }

    /**
     * 8. Obie tablice są JUŻ posortowane — brute force o tym zapomina i sortuje połączoną całość od nowa
     * (O(n log n)). Dwa wskaźniki wykorzystują, że oba "czoła" rosną — O(n + m), bez ponownego sortowania.
     */
    static void mergeSortedArrays() {
        section("8. Scalanie dwóch posortowanych tablic (merge sorted arrays)");

        int[] a = {1, 3, 5, 7};
        int[] b = {2, 4, 6, 8, 10};
        long[] bruteCmp = {0};
        show("brute force — wynik", Arrays.toString(bruteForceMerge(a, b, bruteCmp)));
        show("brute force — porównań", bruteCmp[0]);
        // WYNIK: brute force — wynik → [1, 2, 3, 4, 5, 6, 7, 8, 10]
        // WYNIK: brute force — porównań → 36

        long[] twoPtrCmp = {0};
        show("dwa wskaźniki — wynik", Arrays.toString(twoPointerMerge(a, b, twoPtrCmp)));
        show("dwa wskaźniki — porównań", twoPtrCmp[0]);
        // WYNIK: dwa wskaźniki — wynik → [1, 2, 3, 4, 5, 6, 7, 8, 10]
        // WYNIK: dwa wskaźniki — porównań → 7

        note("36 porównań (sortowanie od zera) kontra 7 (scalanie) — to DOKŁADNIE krok \"merge\" z Algorithms02Sorting");
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Dwa wskaźniki: para o sumie / usuwanie duplikatów — wymaga POSORTOWANYCH danych, O(n) zamiast O(n²).
     *   • Przesuwne okno: suma/maksimum okna o stałym k — aktualizuj zamiast liczyć od nowa, O(n) zamiast O(n*k).
     *   • Suma prefiksowa: wiele zapytań o sumę zakresu — policz raz (O(n)), potem każde zapytanie O(1).
     *   • Zliczanie częstości: tablica zliczeń zamiast sortowania/porównań parami — O(n) zamiast O(n²)/O(n log n).
     *   • Kadane: maksymalna suma podtablicy — O(n) zamiast O(n²) sprawdzania wszystkich fragmentów.
     *   • Flaga holenderska: sortowanie 3 wartości w jednym przebiegu — O(n) zamiast ogólnego sortowania.
     *   • Scalanie posortowanych: dwa wskaźniki razem — O(n+m) zamiast sortowania połączonej całości od nowa.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego dwa wskaźniki do szukania pary o danej sumie wymagają POSORTOWANEJ tablicy?
     *   2. Co wypisze:  int[] a = {4, 4, 4}; System.out.println(removeDuplicatesInPlace(a));  ?
     *   3. ZNAJDŹ BŁĄD: ktoś liczy sumę każdego okna od zera wewnątrz pętli po wszystkich oknach i nazywa
     *      to "sliding window". Czym to się różni od prawdziwego przesuwnego okna?
     *   4. Dlaczego suma prefiksowa opłaca się dopiero przy WIELU zapytaniach, a nie przy jednym?
     *   5. Na czym polega "zapominanie bagażu" w algorytmie Kadane'a (currentSum = Math.max(arr[i], currentSum + arr[i]))?
     *   6. ZNAJDŹ BŁĄD: w implementacji flagi holenderskiej ktoś po zamianie arr[mid] z arr[high] robi mid++
     *      zamiast zostawić mid bez zmian. Co się popsuje?
     *   7. Dlaczego scalanie dwóch POSORTOWANYCH tablic dwoma wskaźnikami jest szybsze niż ich połączenie i ponowne sortowanie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: dwa wskaźniki, para o sumie 10 w [1,3,4,6,8,10]",
                "[2, 3]", () -> exercise1());
        Check.equal("ćw. 2: maksymalna suma okna k=2 w [4,2,9,1,7]", 11, () -> exercise2());
        Check.equal("ćw. 3: isAnagram(\"rat\", \"tar\")", true, () -> exercise3());
        Check.equal("ćw. 4: Kadane na [5,-9,6,-2,3]", 7, () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "[2, 3]", () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", 11, () -> solution2());
        Check.equal("ćw. 3 (wzorzec)", true, () -> solution3());
        Check.equal("ćw. 4 (wzorzec)", 7, () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): dwoma wskaźnikami znajdź indeksy pary sumującej się do 10 w [1, 3, 4, 6, 8, 10]
     * i zwróć je jako tekst przez Arrays.toString.
     * Podpowiedź: skopiuj logikę twoPointerPairSum (możesz zignorować licznik kroków).
     */
    static String exercise1() {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (łatwe): policz maksymalną sumę okna o długości k=2 w tablicy [4, 2, 9, 1, 7].
     * Podpowiedź: skopiuj logikę slidingWindowMaxSum (możesz zignorować licznik dodawań).
     */
    static int exercise2() {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): sprawdź, czy "rat" i "tar" to anagramy.
     * Podpowiedź: wywołaj isAnagram.
     */
    static boolean exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): algorytmem Kadane'a policz maksymalną sumę podtablicy w [5, -9, 6, -2, 3].
     * Podpowiedź: skopiuj logikę kadaneMaxSubarray (możesz zignorować licznik dodawań).
     */
    static int exercise4() {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1() {
        return Arrays.toString(twoPointerPairSum(new int[] {1, 3, 4, 6, 8, 10}, 10, new long[1]));
    }

    static int solution2() {
        return slidingWindowMaxSum(new int[] {4, 2, 9, 1, 7}, 2, new long[1]);
    }

    static boolean solution3() {
        return isAnagram("rat", "tar");
    }

    static int solution4() {
        return kadaneMaxSubarray(new int[] {5, -9, 6, -2, 3}, new long[1]);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo decyzja "przesuń lewy czy prawy wskaźnik" opiera się na porównaniu sumy z targetem — to
     *      działa tylko, gdy wiadomo, że przesunięcie w prawo zwiększa sumę, a w lewo ją zmniejsza
     *      (czyli dane są posortowane).
     *   2. 1 (wszystkie trzy elementy są takie same — zostaje jeden unikalny).
     *   3. To wciąż O(n*k) — brute force z inną nazwą. Prawdziwe przesuwne okno NIE liczy sumy od zera,
     *      tylko AKTUALIZUJE poprzednią sumę (dodaje nowy element, odejmuje stary) w O(1) na krok.
     *   4. Budowa prefiksu kosztuje O(n) — przy JEDNYM zapytaniu to i tak O(n) + O(1), bez korzyści. Dopiero
     *      przy wielu zapytaniach ten jednorazowy koszt "rozkłada się" i każde kolejne zapytanie jest za darmo.
     *   5. Jeśli dotychczasowa suma (bagaż) jest UJEMNA, ciągnięcie jej ze sobą tylko pogarsza kolejną sumę —
     *      lepiej "porzucić bagaż" i zacząć nowy fragment od arr[i].
     *   6. Element 2 trafia na koniec (high), ale element, który tam WCZEŚNIEJ był, trafia na pozycję mid —
     *      i TEN element trzeba jeszcze sprawdzić. Zrobienie mid++ pominęłoby go bez sprawdzenia.
     *   7. Bo "posortowane" to już połowa roboty — dwa wskaźniki wykorzystują ten fakt i scalają w jednym
     *      przebiegu (O(n+m)), podczas gdy ponowne sortowanie IGNORUJE, że dane już były uporządkowane.
     */
    // </editor-fold>
}
