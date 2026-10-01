package t24_algorithms;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Employee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Statystyka opisowa — średnia, mediana, dominanta, wariancja, percentyle
 *        (statistics = statystyka; mean = średnia; median = mediana; mode = dominanta; variance = wariancja)
 *
 * W SKRÓCIE:
 *   Statystyka opisowa streszcza dużą listę liczb w kilka wartości: "gdzie jest środek" (średnia, mediana,
 *   dominanta), "jak bardzo dane są rozrzucone" (rozstęp, wariancja, odchylenie standardowe) i "jaka wartość
 *   dzieli dane na procentowe kawałki" (percentyle). To matematyczne cegiełki pod raporty, wykresy i uczenie maszynowe.
 *
 * ANALOGIA: nauczyciel ma 30 wyników sprawdzianu. Zamiast czytać wszystkie 30 liczb na zebraniu z rodzicami,
 *   mówi "średnia 68%, mediana 72%, najczęstszy wynik to 75%, rozstęp 40 punktów" — cztery liczby zamiast trzydziestu,
 *   a i tak wiadomo, jak poszła klasa.
 *
 * JAK TO DZIAŁA:
 *   średnia (mean)        suma / liczba elementów                           — wrażliwa na wartości odstające
 *   mediana (median)      środkowa wartość POSORTOWANYCH danych              — odporna na odstające
 *   dominanta (mode)      wartość (lub wartości) o największej liczności     — jedyna sensowna dla danych nieliczbowych
 *   rozstęp (range)       max − min                                         — najprostsza miara rozrzutu
 *   wariancja (variance)  średni kwadrat odchylenia od średniej              — populacyjna: / n, próbkowa: / (n − 1)
 *   odchylenie std.       pierwiastek z wariancji                           — w tych samych jednostkach co dane
 *   percentyl p           wartość, poniżej której leży p% danych            — mediana to percentyl 50
 *
 * SŁÓWKA:
 *   mean = średnia; median = mediana; mode = dominanta (wartość najczęstsza); range = rozstęp; variance = wariancja;
 *   standard deviation = odchylenie standardowe; percentile = percentyl; population = populacja (wszystkie dane);
 *   sample = próba (fragment populacji); outlier = wartość odstająca; moving average = średnia krocząca;
 *   summary statistics = statystyki podsumowujące; nearest rank = metoda najbliższej rangi; bias = obciążenie (błąd systematyczny).
 *
 * ZOBACZ TEŻ: t15_numbers/Numbers05IntegerTricks (przepełnienie przy sumowaniu), t16_streams/Streams11GroupingBy
 *             (grupowanie i agregacje strumieniowe), t24_algorithms/Math08BigNumbers (gdy nawet long nie wystarcza),
 *             t24_algorithms/Math09InterviewClassics (klasyczne pytania rekrutacyjne o liczbach).
 * </pre>
 */
public class Math06Statistics {

    public static void main(String[] args) {
        title("Math06 — statystyka opisowa");

        srednia();               // srednia = mean
        mediana();                // mediana = median
        dominanta();               // dominanta = mode
        rozstep();                 // rozstep = range
        wariancjaIOdchylenie();    // wariancja i odchylenie = variance and standard deviation
        percentyle();              // percentyle = percentiles
        sredniaKroczaca();         // srednia kroczaca = moving average
        statystykiZeStrumieni();   // statystyki ze strumieni = summary statistics from streams
        sredniaAWartosciOdstajace(); // srednia a wartosci odstajace = mean vs outliers
        exercises();               // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. ŚREDNIA ARYTMETYCZNA (MEAN)
    // =================================================================================================

    /**
     * 1. Średnia = suma wszystkich wartości podzielona przez ich liczbę. Złożoność O(n) — jeden przebieg.
     * Suma trzymana jest w long (nie int), żeby dla dłuższych tablic nie przepełnić się po cichu
     * (patrz t15_numbers/Numbers05IntegerTricks, sekcja 2).
     */
    static void srednia() {
        section("1. Średnia arytmetyczna (mean)");

        int[] data = toIntArray();
        show("dane", Arrays.toString(data));
        show("srednia(dane)", mean(data));
        // WYNIK: dane → [5, 3, 8, 1, 9, 2, 7, 3, 10, 6, 4, 8]
        // WYNIK: srednia(dane) → 5.5

        note("Złożoność: O(n) — jeden przebieg po tablicy, suma + dzielenie.");

        // PUŁAPKA: int sum = 0; przy dużej tablicy liczb bliskich Integer.MAX_VALUE mogłoby się przepełnić.
        //   Dlatego mean() sumuje do long, a dopiero na końcu dzieli jako double.
    }

    // =================================================================================================
    // 2. MEDIANA (MEDIAN) — ŚRODKOWA WARTOŚĆ POSORTOWANYCH DANYCH
    // =================================================================================================

    /**
     * 2. Mediana to wartość w ŚRODKU posortowanych danych: dla nieparzystej liczby elementów — środkowy
     * element, dla parzystej — średnia dwóch środkowych. Wymaga posortowania, więc złożoność to O(n log n),
     * a nie O(n) jak średnia.
     */
    static void mediana() {
        section("2. Mediana (median) — środkowa wartość posortowanych danych");

        int[] data = toIntArray();
        show("mediana(dane) [12 elementów, parzysta]", median(data));
        // WYNIK: mediana(dane) [12 elementów, parzysta] → 5.5

        int[] odd = {1, 3, 3, 6, 7, 8, 9};
        show("mediana([1,3,3,6,7,8,9]) [nieparzysta]", median(odd));
        // WYNIK: mediana([1,3,3,6,7,8,9]) [nieparzysta] → 6.0

        note("Złożoność: O(n log n) — sortowanie dominuje nad resztą pracy.");

        // DOBRA PRAKTYKA: mediana jest ODPORNA na wartości odstające (zobacz sekcję 9) — w raportach
        //   o pensjach, cenach mieszkań czy czasach ładowania strony często lepsza niż średnia.
    }

    // =================================================================================================
    // 3. DOMINANTA (MODE) — WARTOŚĆ NAJCZĘSTSZA
    // =================================================================================================

    /**
     * 3. Dominanta to wartość (lub wartości, przy remisie) występująca najczęściej. Liczymy wystąpienia
     * w mapie (TreeMap — deterministyczna kolejność kluczy), a potem szukamy maksimum. Złożoność O(n).
     */
    static void dominanta() {
        section("3. Dominanta (mode) — wartość najczęstsza");

        int[] data = toIntArray();
        show("dominanty(dane)", modes(data));
        // WYNIK: dominanty(dane) → [3, 8]
        note("Remis: 3 i 8 występują po dwa razy, reszta wartości — po razie. Obie są dominantami.");

        int[] single = {1, 1, 1, 2, 3};
        show("dominanty([1,1,1,2,3])", modes(single));
        // WYNIK: dominanty([1,1,1,2,3]) → [1]

        // PUŁAPKA: dla danych, w których każda wartość jest unikatowa, dominantą formalnie są WSZYSTKIE
        //   wartości (każda ma liczność 1) — w praktyce taki wynik jest bezużyteczny i warto go osobno obsłużyć.
    }

    // =================================================================================================
    // 4. ROZSTĘP (RANGE) — NAJPROSTSZA MIARA ROZRZUTU
    // =================================================================================================

    /**
     * 4. Rozstęp = max − min. Najprostsza miara tego, "jak szeroko rozciągają się dane", ale bardzo
     * wrażliwa na pojedynczą ekstremalną wartość — jeden odstający punkt psuje cały wynik.
     */
    static void rozstep() {
        section("4. Rozstęp (range) — max − min");

        int[] data = toIntArray();
        show("rozstep(dane)", range(data));
        // WYNIK: rozstep(dane) → 9

        // PUŁAPKA: rozstęp liczy TYLKO dwie skrajne wartości — ignoruje wszystko pomiędzy. Dane
        //   [1, 5, 5, 5, 5, 5, 5, 9] i [1, 9] mają identyczny rozstęp (8), choć wyglądają zupełnie inaczej.
        int[] sameRange1 = {1, 5, 5, 5, 5, 5, 5, 9};
        int[] sameRange2 = {1, 9};
        show("rozstep([1,5,5,5,5,5,5,9])", range(sameRange1));
        show("rozstep([1,9])", range(sameRange2));
        // WYNIK: rozstep([1,5,5,5,5,5,5,9]) → 8
        // WYNIK: rozstep([1,9]) → 8
    }

    // =================================================================================================
    // 5. WARIANCJA I ODCHYLENIE STANDARDOWE: POPULACJA KONTRA PRÓBA
    // =================================================================================================

    /**
     * 5. Wariancja to średni kwadrat odchylenia od średniej — mierzy rozrzut biorąc pod uwagę WSZYSTKIE
     * dane, nie tylko skrajne (w przeciwieństwie do rozstępu). Mamy dwa warianty:
     * <ul>
     * <li>populacyjna: dzielimy przez n — gdy dane TO cała populacja, którą nas interesuje;</li>
     * <li>próbkowa: dzielimy przez (n − 1) — gdy dane to tylko PRÓBA z większej populacji.</li>
     * </ul>
     * Dzielenie przez (n − 1) (poprawka Bessela) wyrównuje obciążenie: próba ma zwykle mniejszy rozrzut
     * niż cała populacja (bo nie trafiły do niej najbardziej skrajne przypadki), więc dzielenie przez
     * mniejszą liczbę trochę PODBIJA wynik, żeby estymator był nieobciążony (bias = obciążenie).
     * Odchylenie standardowe to pierwiastek z wariancji — wraca do jednostek oryginalnych danych
     * (wariancja pensji w zł² nie mówi wiele, odchylenie w zł — już tak).
     */
    static void wariancjaIOdchylenie() {
        section("5. Wariancja i odchylenie standardowe: populacja kontra próba");

        int[] data = toIntArray();
        double popVar = variance(data, false);
        double sampleVar = variance(data, true);
        show("wariancja populacyjna (/n)", popVar);
        show("wariancja probkowa (/(n-1))", sampleVar);
        show("odchylenie std. populacyjne", Math.sqrt(popVar));
        show("odchylenie std. probkowe", Math.sqrt(sampleVar));
        // WYNIK: wariancja populacyjna (/n) → 7.916666666666667
        // WYNIK: wariancja probkowa (/(n-1)) → 8.636363636363637
        // WYNIK: odchylenie std. populacyjne → 2.8136571693556887
        // WYNIK: odchylenie std. probkowe → 2.9387690682262932

        note("Złożoność: O(n) — jeden przebieg na średnią, drugi na sumę kwadratów odchyleń.");

        // DOBRA PRAKTYKA: gdy masz CAŁY zbiór danych (np. wszystkie zamówienia w systemie) — populacyjna.
        //   Gdy masz PRÓBKĘ i chcesz wnioskować o większej całości (np. ankieta 100 klientów na milion) — próbkowa.
        //   Dla dużych n różnica jest znikoma; dla małych n (jak tu, n=12) widać ją wyraźnie.
    }

    // =================================================================================================
    // 6. PERCENTYLE — METODA NAJBLIŻSZEJ RANGI (NEAREST RANK)
    // =================================================================================================

    /**
     * 6. Percentyl p to wartość, poniżej (lub przy niej) której leży p% posortowanych danych. Metoda
     * "najbliższej rangi": rangę liczymy jako {@code ceil(p / 100 * n)} i bierzemy tę pozycję z posortowanej
     * tablicy (indeksując od 1). Mediana to dokładnie percentyl 50.
     */
    static void percentyle() {
        section("6. Percentyle — metoda najbliższej rangi (nearest rank)");

        int[] data = toIntArray();
        int[] sorted = data.clone();
        Arrays.sort(sorted);
        show("dane posortowane", Arrays.toString(sorted));
        show("percentyl 25", percentileNearestRank(data, 25));
        show("percentyl 50 (= mediana)", percentileNearestRank(data, 50));
        show("percentyl 75", percentileNearestRank(data, 75));
        show("percentyl 90", percentileNearestRank(data, 90));
        // WYNIK: dane posortowane → [1, 2, 3, 3, 4, 5, 6, 7, 8, 8, 9, 10]
        // WYNIK: percentyl 25 → 3
        // WYNIK: percentyl 50 (= mediana) → 5
        // WYNIK: percentyl 75 → 8
        // WYNIK: percentyl 90 → 9

        // PUŁAPKA: percentyl 50 metodą najbliższej rangi NIE zawsze równa się mediana() z sekcji 2 —
        //   dla parzystej liczby elementów mediana UŚREDNIA dwie środkowe wartości, a metoda najbliższej
        //   rangi wybiera JEDNĄ konkretną pozycję. Tu akurat wyszło to samo (5 vs 5.5 — zaokrąglone w dół
        //   przez ceil), ale na innych danych mogą się różnić. Istnieją też inne definicje percentyla
        //   (interpolowana, używana np. przez Excel) — zawsze sprawdź, której metody oczekuje odbiorca raportu.
    }

    // =================================================================================================
    // 7. ŚREDNIA KROCZĄCA (MOVING AVERAGE)
    // =================================================================================================

    /**
     * 7. Średnia krocząca wygładza szereg danych: dla każdego okna o stałej długości liczy średnią, potem
     * okno się przesuwa o jeden element. Naiwnie to O(n·w) (dla każdej z n−w+1 pozycji sumujemy w elementów).
     * Sztuczka "przesuwnego okna" (sliding window): trzymamy sumę bieżącego okna i przy przesunięciu
     * TYLKO dodajemy nowy element i odejmujemy ten, który wypadł z okna — O(1) na krok, O(n) łącznie.
     */
    static void sredniaKroczaca() {
        section("7. Średnia krocząca (moving average)");

        int[] data = toIntArray();
        double[] avg3 = movingAverage(data, 3);
        show("dane", Arrays.toString(data));
        show("srednia kroczaca, okno=3", Arrays.toString(avg3));
        // WYNIK: dane → [5, 3, 8, 1, 9, 2, 7, 3, 10, 6, 4, 8]
        // WYNIK: srednia kroczaca, okno=3 → [5.333333333333333, 4.0, 6.0, 4.0, 6.0, 4.0, 6.666666666666667, 6.333333333333333, 6.666666666666667, 6.0]

        note("Zastosowanie: wygładzanie wykresu sprzedaży dzień po dniu, żeby zobaczyć trend, nie szum.");

        // DOBRA PRAKTYKA: licz sumę okna przyrostowo (dodaj/odejmij), nie sumuj od nowa dla każdej pozycji —
        //   różnica między O(n) a O(n·w) robi się zauważalna już przy tysiącach punktów danych.
    }

    // =================================================================================================
    // 8. GOTOWE STATYSTYKI ZE STRUMIENI: IntSummaryStatistics / DoubleSummaryStatistics
    // =================================================================================================

    /**
     * 8. Zamiast ręcznie liczyć count/sum/min/max/average osobnymi pętlami, strumień policzy je WSZYSTKIE
     * naraz, jednym przebiegiem: {@code IntStream.summaryStatistics()} (albo DoubleStream — dla danych
     * zmiennoprzecinkowych). To dokładnie te same wielkości co w sekcjach 1 i 4, tylko gotowe "z pudełka".
     */
    static void statystykiZeStrumieni() {
        section("8. Gotowe statystyki ze strumieni: IntSummaryStatistics / DoubleSummaryStatistics");

        int[] data = toIntArray();
        IntSummaryStatistics stats = IntStream.of(data).summaryStatistics();
        show("stats.getCount()", stats.getCount());
        show("stats.getSum()", stats.getSum());
        show("stats.getMin()", stats.getMin());
        show("stats.getMax()", stats.getMax());
        show("stats.getAverage()", stats.getAverage());
        show("stats (toString)", stats);
        // WYNIK: stats.getCount() → 12
        // WYNIK: stats.getSum() → 66
        // WYNIK: stats.getMin() → 1
        // WYNIK: stats.getMax() → 10
        // WYNIK: stats.getAverage() → 5.5
        // WYNIK: stats (toString) → IntSummaryStatistics{count=12, sum=66, min=1, average=5,500000, max=10}

        // PUŁAPKA: zwróć uwagę na PRZECINEK w "average=5,500000" — toString() tej klasy formatuje liczbę
        //   przez domyślny Locale systemu (tu: polski), więc na innym komputerze może wyjść "average=5.500000"
        //   z kropką. Nie parsuj tego tekstu w kodzie — czytaj stats.getAverage() jako double (jak wyżej).

        DoubleSummaryStatistics priceLikeStats = IntStream.of(data).asDoubleStream().summaryStatistics();
        // asDoubleStream = zamień na strumień typu double (potrzebne np. gdy miksujemy z cenami typu double)
        show("DoubleSummaryStatistics.getAverage()", priceLikeStats.getAverage());
        // WYNIK: DoubleSummaryStatistics.getAverage() → 5.5

        note("Złożoność: O(n), jeden przebieg po strumieniu — tyle samo co ręczna pętla, ale bez pisania jej samemu.");

        // DOBRA PRAKTYKA: gdy potrzebujesz kilku podstawowych statystyk naraz, summaryStatistics() jest
        //   krótsze i mniej podatne na błędy niż osobne wywołania sum()/min()/max()/average() (to byłyby
        //   4 osobne przebiegi po strumieniu, a strumień jednorazowy można skonsumować tylko raz!).
    }

    // =================================================================================================
    // 9. PUŁAPKA: ŚREDNIA JEST WRAŻLIWA NA WARTOŚCI ODSTAJĄCE (OUTLIERS)
    // =================================================================================================

    /**
     * 9. Jedna ekstremalna wartość potrafi całkowicie zniekształcić średnią, podczas gdy mediana prawie
     * się nie zmienia. To klasyczny argument za tym, by w raportach o pensjach, cenach czy czasach
     * odpowiedzi patrzeć na medianę, a nie tylko na średnią.
     */
    static void sredniaAWartosciOdstajace() {
        section("9. PUŁAPKA: średnia jest wrażliwa na wartości odstające (outliers)");

        List<Employee> employees = SampleData.employees();
        int[] salaries = employees.stream().mapToInt(Employee::salary).toArray(); // salary = pensja
        show("srednia pensji (10 osob)", mean(salaries));
        show("mediana pensji (10 osob)", median(salaries));
        // WYNIK: srednia pensji (10 osob) → 10640.0
        // WYNIK: mediana pensji (10 osob) → 9800.0

        int[] withOutlier = Arrays.copyOf(salaries, salaries.length + 1);
        withOutlier[withOutlier.length - 1] = 500_000;               // pensja prezesa — skrajnie odstająca
        show("srednia pensji (+ prezes 500 000 zl)", mean(withOutlier));
        show("mediana pensji (+ prezes 500 000 zl)", median(withOutlier));
        // WYNIK: srednia pensji (+ prezes 500 000 zl) → 55127.27272727273
        // WYNIK: mediana pensji (+ prezes 500 000 zl) → 9800.0

        note("Średnia podskoczyła ponad pięciokrotnie. Mediana w ogóle się nie zmieniła — dziewięciu na");
        note("jedenastu pracowników dalej zarabia mniej niż \"średnia\" sugerowałaby na pierwszy rzut oka.");

        // PUŁAPKA: nagłówek \"średnie wynagrodzenie w firmie to 55 127 zł\" jest PRAWDZIWY liczbowo, ale
        //   wprowadza w błąd — żaden ze zwykłych pracowników tyle nie zarabia. Dlatego w statystykach
        //   publicznych (GUS) obok średniej często podaje się medianę.
        // DOBRA PRAKTYKA: przy podejrzeniu wartości odstających pokazuj medianę ALBO średnią i medianę razem,
        //   nigdy samą średnią jako jedyny opis "typowej" wartości.
    }

    // =================================================================================================
    // FUNKCJE POMOCNICZE
    // =================================================================================================

    /** toIntArray = zamień List<Integer> z SampleData.numbers() na int[] (wygodniejsze do liczenia). */
    private static int[] toIntArray() {
        return SampleData.numbers().stream().mapToInt(Integer::intValue).toArray();
    }

    /** mean = średnia arytmetyczna. Suma w long chroni przed przepełnieniem przy większych danych. */
    static double mean(int[] data) {
        long sum = 0;
        for (int value : data) {
            sum += value;
        }
        return (double) sum / data.length;
    }

    /** median = mediana. Sortuje KOPIĘ tablicy (clone), oryginał zostaje nietknięty. */
    static double median(int[] data) {
        int[] sorted = data.clone();                                 // clone = sklonuj (płytka kopia — wystarczy dla int[])
        Arrays.sort(sorted);
        int n = sorted.length;
        if (n % 2 == 1) {
            return sorted[n / 2];
        }
        return (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
    }

    /** modes = dominanty. Zwraca WSZYSTKIE wartości o maksymalnej liczności, posortowane rosnąco. */
    static List<Integer> modes(int[] data) {
        Map<Integer, Integer> counts = new TreeMap<>();               // TreeMap = mapa posortowana po kluczu
        for (int value : data) {
            counts.merge(value, 1, Integer::sum);                     // merge = połącz: dodaj 1 do istniejącej liczności
        }
        int maxCount = 0;
        for (int count : counts.values()) {
            maxCount = Math.max(maxCount, count);
        }
        List<Integer> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == maxCount) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /** range = rozstęp (max − min). Dane muszą mieć co najmniej jeden element. */
    static int range(int[] data) {
        int min = data[0];
        int max = data[0];
        for (int value : data) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return max - min;
    }

    /** variance = wariancja. sample=true → dzielenie przez (n − 1) (poprawka Bessela), inaczej przez n. */
    static double variance(int[] data, boolean sample) {
        double m = mean(data);
        double sumSquares = 0;
        for (int value : data) {
            double diff = value - m;
            sumSquares += diff * diff;
        }
        int denominator = sample ? data.length - 1 : data.length;
        return sumSquares / denominator;
    }

    /** percentileNearestRank = percentyl metodą najbliższej rangi (p z zakresu 0..100). */
    static int percentileNearestRank(int[] data, int p) {
        int[] sorted = data.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        int rank = (int) Math.ceil(p / 100.0 * n);                    // ceil = zaokrąglenie w górę
        rank = Math.max(1, Math.min(rank, n));                        // obcięcie do 1..n (brzegi p=0 / p=100)
        return sorted[rank - 1];
    }

    /** movingAverage = średnia krocząca o stałym oknie, liczona przyrostowo (O(n), nie O(n·window)). */
    static double[] movingAverage(int[] data, int window) {
        if (window <= 0 || window > data.length) {
            throw new IllegalArgumentException("nieprawidlowy rozmiar okna: " + window);
        }
        double[] result = new double[data.length - window + 1];
        int windowSum = 0;
        for (int i = 0; i < window; i++) {
            windowSum += data[i];
        }
        result[0] = (double) windowSum / window;
        for (int i = window; i < data.length; i++) {
            windowSum += data[i] - data[i - window];                  // dodaj nowy element, odejmij ten, co wypadł z okna
            result[i - window + 1] = (double) windowSum / window;
        }
        return result;
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   ŚREDNIA (mean)        suma / n                           O(n)        wrażliwa na odstające
     *   MEDIANA (median)      środek posortowanych danych         O(n log n)  odporna na odstające
     *   DOMINANTA (mode)      wartość(ci) o max liczności          O(n)        jedyna sensowna dla danych nieliczbowych
     *   ROZSTĘP (range)       max − min                           O(n)        widzi tylko 2 skrajne wartości
     *   WARIANCJA             Σ(x − średnia)² / n  (populacja)
     *                         Σ(x − średnia)² / (n−1)  (próba — poprawka Bessela, estymator nieobciążony)
     *   ODCH. STANDARDOWE     √wariancja — w jednostkach danych, nie w kwadratach jednostek
     *   PERCENTYL p           ceil(p/100 · n)-ta pozycja w posortowanych danych; percentyl 50 ≈ mediana
     *   ŚREDNIA KROCZĄCA      sumuj przyrostowo (dodaj nowy, odejmij stary) → O(n), nie O(n·okno)
     *   IntSummaryStatistics  count/sum/min/max/average jednym przebiegiem strumienia
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego mediana jest bardziej odporna na wartości odstające niż średnia?
     *   2. Co wypisze:  System.out.println(Arrays.toString(new int[]{1,1,2,2,3}));  po przepuszczeniu przez modes()?
     *   3. ZNAJDŹ BŁĄD:
     *          int suma = 0;
     *          for (int x : bardzoDlugaTablicaDuzychLiczb) suma += x;
     *          double srednia = suma / bardzoDlugaTablicaDuzychLiczb.length;
     *   4. Dlaczego wariancja próbkowa dzieli przez (n − 1), a nie przez n?
     *   5. Co wypisze:  System.out.println(percentileNearestRank(new int[]{10,20,30,40}, 25));  ?
     *   6. ZNAJDŹ BŁĄD:  dla każdej pozycji okna liczymy sumę elementów okna od nowa w pętli wewnętrznej —
     *      jaki to koszt dla n = 1 000 000 i okna = 1000, i jak go obniżyć?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        int[] dataA = {2, 4, 4, 4, 5, 5, 7, 9};
        int[] dataOddMedian = {1, 3, 3, 6, 7, 8, 9};
        int[] dataEvenMedian = {2, 4, 6, 8};
        int[] dataTie = {1, 2, 2, 3, 3, 4};
        int[] dataPercentile = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: średnia [2,4,4,4,5,5,7,9]", 5.0, () -> exercise1(dataA));
        Check.equal("ćw. 2: rozstęp [2,4,4,4,5,5,7,9]", 7, () -> exercise2(dataA));
        Check.equal("ćw. 3a: mediana (nieparzysta liczba elementów)", 6.0, () -> exercise3(dataOddMedian));
        Check.equal("ćw. 3b: mediana (parzysta liczba elementów)", 5.0, () -> exercise3(dataEvenMedian));
        Check.equal("ćw. 4: dominanty (remis 2 i 3)", List.of(2, 3), () -> exercise4(dataTie));
        Check.equal("ćw. 5a: percentyl 25", 30, () -> exercise5(dataPercentile, 25));
        Check.equal("ćw. 5b: percentyl 90", 90, () -> exercise5(dataPercentile, 90));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 5.0, () -> solution1(dataA));
        Check.equal("ćw. 2 (wzorzec)", 7, () -> solution2(dataA));
        Check.equal("ćw. 3a (wzorzec)", 6.0, () -> solution3(dataOddMedian));
        Check.equal("ćw. 3b (wzorzec)", 5.0, () -> solution3(dataEvenMedian));
        Check.equal("ćw. 4 (wzorzec)", List.of(2, 3), () -> solution4(dataTie));
        Check.equal("ćw. 5a (wzorzec)", 30, () -> solution5(dataPercentile, 25));
        Check.equal("ćw. 5b (wzorzec)", 90, () -> solution5(dataPercentile, 90));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 7 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz średnią arytmetyczną tablicy {@code data}.
     * Podpowiedź: suma (w long, żeby nie przepełnić) podzielona przez długość, wynik jako double.
     */
    static double exercise1(int[] data) {
        // TODO: twoje rozwiązanie
        return -1.0;
    }

    /**
     * ĆWICZENIE 2 (łatwe): policz rozstęp (max − min) tablicy {@code data}.
     * Podpowiedź: jeden przebieg, pamiętaj min i max.
     */
    static int exercise2(int[] data) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 3 (średnie): policz medianę tablicy {@code data} — działa zarówno dla parzystej,
     * jak i nieparzystej liczby elementów. Oryginalnej tablicy NIE wolno modyfikować.
     * Podpowiedź: sklonuj tablicę, posortuj kopię.
     */
    static double exercise3(int[] data) {
        // TODO: twoje rozwiązanie
        return -1.0;
    }

    /**
     * ĆWICZENIE 4 (średnie): zwróć listę dominant (wartości o maksymalnej liczności), posortowaną rosnąco.
     * Przy remisie zwróć WSZYSTKIE wartości, które remisują.
     * Podpowiedź: policz liczności w TreeMap (daje posortowaną kolejność kluczy za darmo).
     */
    static List<Integer> exercise4(int[] data) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): policz percentyl {@code p} (0..100) metodą najbliższej rangi.
     * Podpowiedź: ranga = {@code ceil(p / 100.0 * n)}, indeksy od 1; obetnij rangę do zakresu 1..n.
     */
    static int exercise5(int[] data, int p) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static double solution1(int[] data) {
        return mean(data);
    }

    static int solution2(int[] data) {
        return range(data);
    }

    static double solution3(int[] data) {
        return median(data);
    }

    static List<Integer> solution4(int[] data) {
        return modes(data);
    }

    static int solution5(int[] data, int p) {
        return percentileNearestRank(data, p);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Mediana patrzy tylko na WARTOŚĆ ŚRODKOWĄ posortowanych danych — przesunięcie jednej skrajnej
     *      liczby o bardzo dużo wcale nie zmienia tego, co jest "w środku". Średnia sumuje wszystko, więc
     *      każda zmiana pojedynczej wartości wpływa na wynik proporcjonalnie do jej wielkości.
     *   2. [1, 2] — 1 i 2 występują po dwa razy, 3 jeden raz; remis dwóch dominant, posortowane rosnąco.
     *   3. int suma może się przepełnić dla bardzo dużych/długich danych (ta sama pułapka co w
     *      t15_numbers/Numbers05IntegerTricks, sekcja 1-2); dzielenie suma / length to dzielenie CAŁKOWITE
     *      (obcina ułamek) zanim trafi do double. Popraw: long suma = 0; ... (double) suma / length.
     *   4. Próba zwykle nie zawiera najbardziej skrajnych wartości z całej populacji, więc jej rozrzut
     *      "na surowo" (dzielenie przez n) byłby systematycznie ZANIŻONY. Dzielenie przez (n − 1) kompensuje
     *      to obciążenie (bias) i daje estymator nieobciążony wariancji populacji.
     *   5. 10 — posortowane [10,20,30,40], n=4, ranga = ceil(0.25 · 4) = ceil(1.0) = 1 → sorted[0] = 10.
     *   6. Dla każdej z (n − okno + 1) pozycji licząc sumę od nowa w pętli po "okno" elementów, koszt to
     *      O(n · okno) — dla n=1 000 000 i okno=1000 to miliard operacji. Sztuczka przesuwnego okna
     *      (dodaj nowy element, odejmij ten, który wypadł) obniża to do O(n).
     */
    // </editor-fold>
}
