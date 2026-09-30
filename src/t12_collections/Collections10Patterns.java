package t12_collections;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Product;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Przepisy kolekcyjne — gotowe wzorce z pętli, które warto rozpoznawać na pamięć
 *        (recipe/pattern = przepis/wzorzec; sliding window = okno przesuwne; heap = kopiec)
 *
 * W SKRÓCIE:
 *   Osiem klasycznych „przepisów” budowanych z tego, co już znasz (pętle, HashMap, TreeMap, LinkedHashSet,
 *   PriorityQueue, ArrayDeque) — bez streamów (te poznasz w t16, wtedy część z nich skróci się do jednej linii).
 *   Cel: żebyś rozpoznawał te kształty kodu od razu, bo pojawiają się wszędzie — na rozmowach o pracę też.
 *
 * ANALOGIA: przepisy kucharskie.
 *   Nie wymyślasz sosu beszamelowego od zera za każdym razem — znasz przepis i go stosujesz. Tak samo z tymi
 *   wzorcami: "zliczanie" to zawsze merge, "grupowanie" to zawsze computeIfAbsent, "top-N" to zawsze mały kopiec.
 *
 * JAK TO DZIAŁA:
 *   Zliczanie:    map.merge(klucz, 1, Integer::sum)
 *   Grupowanie:   map.computeIfAbsent(klucz, k -> new ArrayList<>()).add(wartość)
 *   Top-N:        PriorityQueue rozmiaru N (kopiec MIN) — gdy przekroczy N, wyrzuć najmniejszy (poll)
 *   Okno:         ArrayDeque z INDEKSAMI, usuwaj z przodu (wypadło z okna) i z tyłu (już nigdy nie będzie maksimum)
 *
 * SŁÓWKA:
 *   merge = scal; invert = odwróć (klucz ↔ wartość); deduplicate = usuń duplikaty; heap = kopiec;
 *   sliding window = okno przesuwne; monotonic = monotoniczny (tu: malejący w kolejce); two-sum = suma dwóch.
 *
 * ZOBACZ TEŻ: t12_collections/Collections05Maps (merge, computeIfAbsent — pełne wprowadzenie), t12_collections/
 *             Collections06QueuesDeques (PriorityQueue, ArrayDeque), t12_collections/Collections04Sets
 *             (LinkedHashSet), t16_streams/Streams11GroupingBy (te same przepisy jednym wywołaniem groupingBy).
 * </pre>
 */
public class Collections10Patterns {

    public static void main(String[] args) {
        title("Collections10 — przepisy kolekcyjne: zliczanie, grupowanie, top-N i inne");

        countingWithMerge();          // counting with merge = zliczanie przez merge
        groupingWithComputeIfAbsent();// grouping with computeIfAbsent = grupowanie przez computeIfAbsent
        invertingMap();                // inverting map = odwracanie mapy
        deduplicatePreservingOrder();  // deduplicate preserving order = usuwanie duplikatów z zachowaniem kolejności
        topNWithPriorityQueue();       // top n with priority queue = top-N przez PriorityQueue
        twoSumWithHashMap();           // two sum with hash map = suma dwóch przez HashMap
        slidingWindowMaximum();        // sliding window maximum = maksimum w oknie przesuwnym
        firstNonRepeatingChar();       // first non repeating char = pierwszy niepowtarzający się znak
        exercises();                    // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. ZLICZANIE — Map.merge
    // =================================================================================================

    /** 1. Klasyczny licznik wystąpień: dla każdego elementu merge(klucz, 1, Integer::sum). */
    static void countingWithMerge() {
        section("1. Zliczanie wystąpień — Map.merge");

        List<String> words = SampleData.words();
        Map<String, Integer> counts = new TreeMap<>();   // TreeMap = posortowany, deterministyczny wydruk
        for (String w : words) {
            counts.merge(w, 1, Integer::sum);
        }
        show("liczba wystąpień słów", counts);
        // WYNIK: liczba wystąpień słów → {enum=1, java=3, kolekcja=1, lambda=1, lista=1, mapa=1, optional=1, rekord=1, stream=2}

        // DOBRA PRAKTYKA: merge(klucz, 1, Integer::sum) w jednej linii robi to, co ręcznie wymagałoby
        //   if (map.containsKey(k)) map.put(k, map.get(k) + 1); else map.put(k, 1); — czytelniej i bez błędu przy pustej mapie.
    }

    // =================================================================================================
    // 2. GRUPOWANIE — Map<K, List<V>> przez computeIfAbsent
    // =================================================================================================

    /** 2. computeIfAbsent tworzy pustą listę TYLKO gdy klucza jeszcze nie ma, i od razu do niej dodaje. */
    static void groupingWithComputeIfAbsent() {
        section("2. Grupowanie — Map<K, List<V>> przez computeIfAbsent");

        Map<Category, List<String>> byCategory = new EnumMap<>(Category.class);   // klucz to enum → EnumMap (kit, reguła determinizmu)
        for (Product p : SampleData.products()) {
            byCategory.computeIfAbsent(p.category(), k -> new ArrayList<>()).add(p.name());
        }
        show("produkty w kategoriach", byCategory);
        // WYNIK: produkty w kategoriach → {ELEKTRONIKA=[Laptop Pro 14, Smartfon X, Słuchawki BT, Monitor 27 cali], SPOZYWCZE=[Kawa ziarnista 1kg, Czekolada gorzka, Oliwa z oliwek], KSIAZKI=[Czysty kod, Java. Podstawy, Wzorce projektowe], ODZIEZ=[Kurtka zimowa, T-shirt bawełniany], DOM=[Ekspres do kawy, Lampka biurkowa]}

        // PUŁAPKA: computeIfAbsent(klucz, k -> new ArrayList<>()) MUSI dostać funkcję TWORZĄCĄ nową listę za każdym
        //   razem (lambda), a nie gotowy obiekt (computeIfAbsent(klucz, new ArrayList<>())) — inaczej WSZYSTKIE
        //   klucze dzieliłyby jedną, wspólną listę (ten sam błąd co nCopies z zmiennym elementem — Collections09).
    }

    // =================================================================================================
    // 3. ODWRACANIE MAPY — klucz ↔ wartość
    // =================================================================================================

    /** 3. Odwrócenie ma sens tylko przy UNIKALNYCH wartościach — inaczej część danych "zniknie" (nadpisanie). */
    static void invertingMap() {
        section("3. Odwracanie mapy — klucz ↔ wartość");

        Map<String, String> skuToName = new TreeMap<>();
        for (Product p : SampleData.products()) {
            skuToName.put(p.sku(), p.name());
        }
        show("sku → nazwa", skuToName);
        // WYNIK: sku → nazwa → {DOM-001=Ekspres do kawy, DOM-002=Lampka biurkowa, ELE-001=Laptop Pro 14, ELE-002=Smartfon X, ELE-003=Słuchawki BT, ELE-004=Monitor 27 cali, KSI-001=Czysty kod, KSI-002=Java. Podstawy, KSI-003=Wzorce projektowe, ODZ-001=Kurtka zimowa, ODZ-002=T-shirt bawełniany, SPO-001=Kawa ziarnista 1kg, SPO-002=Czekolada gorzka, SPO-003=Oliwa z oliwek}

        Map<String, String> nameToSku = new TreeMap<>();
        for (Map.Entry<String, String> e : skuToName.entrySet()) {
            String previousSku = nameToSku.put(e.getValue(), e.getKey());   // put zwraca POPRZEDNIĄ wartość (albo null)
            if (previousSku != null) {
                note("UWAGA: nazwa \"" + e.getValue() + "\" miała już inny sku (" + previousSku + ") — nadpisana!");
            }
        }
        show("nazwa → sku (odwrócona)", nameToSku);
        // WYNIK: (patrz --show, kolejność alfabetyczna nazw — sprawdzona uruchomieniem)

        // PUŁAPKA: gdyby dwa produkty miały tę samą nazwę, drugie put nadpisałoby pierwsze — stracilibyśmy jeden
        //   sku bez żadnego ostrzeżenia. Zawsze sprawdzaj (jak wyżej: previousSku != null), czy odwrócenie jest
        //   bezstratne, zanim uznasz nameToSku za wiarygodne źródło danych.
    }

    // =================================================================================================
    // 4. USUWANIE DUPLIKATÓW Z ZACHOWANIEM KOLEJNOŚCI — LinkedHashSet
    // =================================================================================================

    /** 4. LinkedHashSet pamięta kolejność WSTAWIANIA (pierwszego wystąpienia) — bezpiecznie wypisywać wprost. */
    static void deduplicatePreservingOrder() {
        section("4. Usuwanie duplikatów z zachowaniem kolejności — LinkedHashSet");

        List<String> words = SampleData.words();
        Set<String> uniqueInOrder = new LinkedHashSet<>(words);
        show("unikalne słowa (kolejność pierwszego wystąpienia)", uniqueInOrder);
        // WYNIK: unikalne słowa (kolejność pierwszego wystąpienia) → [java, stream, lambda, kolekcja, mapa, lista, optional, rekord, enum]

        // DOBRA PRAKTYKA: new LinkedHashSet<>(lista) w jednej linii usuwa duplikaty I zachowuje kolejność
        //   pierwszego wystąpienia. Zwykły HashSet też usunąłby duplikaty, ale kolejność byłaby nieprzewidywalna
        //   (dlatego HashSet nigdy nie wypisujemy wprost — reguła kursu).
    }

    // =================================================================================================
    // 5. TOP-N — PriorityQueue (kopiec)
    // =================================================================================================

    /**
     * 5. Kopiec MIN rozmiaru N: gdy przekroczy N elementów, wyrzucamy najmniejszy. Po przejściu całej kolekcji
     * w kopcu zostaje dokładnie N NAJWIĘKSZYCH elementów — bez sortowania całości (przydatne przy dużych danych).
     */
    static void topNWithPriorityQueue() {
        section("5. Top-N najdroższych produktów — PriorityQueue (kopiec)");

        int n = 3;
        Comparator<Product> byPriceThenSku = Comparator.comparing(Product::price).thenComparing(Product::sku);
        PriorityQueue<Product> heap = new PriorityQueue<>(byPriceThenSku);   // domyślnie kopiec MIN

        for (Product p : SampleData.products()) {
            heap.offer(p);
            if (heap.size() > n) {
                heap.poll();   // wyrzuć aktualnie najtańszy — kopiec ma zawsze trzymać N NAJDROŻSZYCH
            }
        }

        List<Product> topN = new ArrayList<>();
        while (!heap.isEmpty()) {
            topN.add(heap.poll());   // pollowanie z kopca MIN daje kolejność ROSNĄCĄ cen
        }
        Collections.reverse(topN);   // odwracamy, żeby pokazać od najdroższego (Collections09)

        showEach("TOP " + n + " najdroższych produktów", topN);
        // WYNIK: (patrz --show — kolejność sprawdzona uruchomieniem)

        // PUŁAPKA: Comparator tylko po cenie (bez thenComparing(sku)) byłby niebezpieczny przy remisach — dwa
        //   produkty w tej samej cenie (tu: Java. Podstawy i Lampka biurkowa, oba 129.00 zł) mogłyby zamieniać
        //   się kolejnością między uruchomieniami, gdyby akurat leżały na granicy odcięcia top-N. Dopisany
        //   tiebreaker (sku) czyni porządek CAŁKOWICIE jednoznacznym niezależnie od danych wejściowych.
    }

    // =================================================================================================
    // 6. TWO-SUM — HashMap<wartość, indeks>
    // =================================================================================================

    /** 6. Klasyczne "two-sum": jedno przejście tablicy, HashMap pamięta, co już widzieliśmy i na jakim indeksie. */
    static void twoSumWithHashMap() {
        section("6. Two-sum — para sumująca się do zadanej wartości, HashMap");

        int[] numbers = {2, 7, 11, 15, 3};
        int target = 9;

        Map<Integer, Integer> seenAt = new HashMap<>();   // wartość → indeks (mapy tej nie wypisujemy — tylko wynik)
        int[] result = null;
        for (int i = 0; i < numbers.length; i++) {
            int need = target - numbers[i];
            if (seenAt.containsKey(need)) {
                result = new int[] {seenAt.get(need), i};
                break;
            }
            seenAt.put(numbers[i], i);
        }
        show("tablica", numbers);
        show("indeksy pary sumującej się do " + target, result);
        // WYNIK: tablica → [2, 7, 11, 15, 3]
        // WYNIK: indeksy pary sumującej się do 9 → [0, 1]

        // JAK TO DZIAŁA: zamiast dla każdego elementu przeszukiwać CAŁĄ resztę tablicy (O(n²), Collections13),
        //   pytamy HashMap "czy widziałem już liczbę, która razem z obecną da target?" — odpowiedź w czasie O(1).
        //   Całość: jedno przejście, O(n) — klasyczny przykład wymiany pamięci (mapa) na czas.
    }

    // =================================================================================================
    // 7. MAKSIMUM W OKNIE PRZESUWNYM — ArrayDeque
    // =================================================================================================

    /**
     * 7. Kolejka dwustronna trzyma INDEKSY, malejąco po wartości. Z przodu usuwamy to, co wypadło z okna.
     * Z tyłu usuwamy wszystko mniejsze/równe nowemu elementowi — te wartości nigdy już nie będą maksimum.
     */
    static void slidingWindowMaximum() {
        section("7. Maksimum w oknie przesuwnym — ArrayDeque (monotoniczna kolejka indeksów)");

        int[] numbers = {1, 3, -1, -3, 5, 3, 6, 7};
        int k = 3;

        List<Integer> maxima = new ArrayList<>();
        Deque<Integer> indices = new ArrayDeque<>();   // indeksy, wartości pod nimi malejąco od przodu

        for (int i = 0; i < numbers.length; i++) {
            while (!indices.isEmpty() && indices.peekFirst() <= i - k) {
                indices.pollFirst();               // wypadł z lewej strony okna
            }
            while (!indices.isEmpty() && numbers[indices.peekLast()] <= numbers[i]) {
                indices.pollLast();                // z tyłu usuń wszystko, co już nie może być maksimum
            }
            indices.offerLast(i);
            if (i >= k - 1) {
                maxima.add(numbers[indices.peekFirst()]);   // przód kolejki = indeks aktualnego maksimum
            }
        }

        show("tablica", numbers);
        show("maksima w oknach o rozmiarze " + k, maxima);
        // WYNIK: tablica → [1, 3, -1, -3, 5, 3, 6, 7]
        // WYNIK: maksima w oknach o rozmiarze 3 → [3, 3, 5, 5, 6, 7]

        // DOBRA PRAKTYKA: każdy indeks trafia do kolejki i wypada z niej co najwyżej raz — dlatego cały algorytm
        //   jest O(n), mimo że wygląda jak dwie zagnieżdżone pętle. Naiwne rozwiązanie (dla każdego okna szukaj
        //   maksimum od nowa) byłoby O(n·k) — wolniejsze dla dużych danych (Collections13Performance).
    }

    // =================================================================================================
    // 8. PIERWSZY NIEPOWTARZAJĄCY SIĘ ZNAK — LinkedHashMap
    // =================================================================================================

    /** 8. LinkedHashMap zapamiętuje kolejność wstawiania kluczy — idealne, gdy potrzebujesz "PIERWSZEGO" wyniku. */
    static void firstNonRepeatingChar() {
        section("8. Pierwszy niepowtarzający się znak — LinkedHashMap");

        String text = "zaznaczenie";
        Map<Character, Integer> counts = new LinkedHashMap<>();   // kolejność = kolejność pierwszego wystąpienia znaku
        for (char c : text.toCharArray()) {
            counts.merge(c, 1, Integer::sum);
        }

        Character first = null;
        for (Map.Entry<Character, Integer> e : counts.entrySet()) {
            if (e.getValue() == 1) {
                first = e.getKey();
                break;
            }
        }
        show("tekst", text);
        show("pierwszy niepowtarzający się znak", first);
        // WYNIK: tekst → zaznaczenie
        // WYNIK: pierwszy niepowtarzający się znak → c

        // JAK TO DZIAŁA: gdybyśmy użyli zwykłego HashMap, entrySet() iterowałby w nieprzewidywalnej kolejności —
        //   "pierwszy" przestałby mieć sens. LinkedHashMap gwarantuje, że iterujemy DOKŁADNIE w kolejności, w
        //   jakiej znaki pierwszy raz się pojawiły w tekście, więc pierwsze trafienie z licznikiem 1 to naprawdę
        //   pierwszy niepowtarzający się znak, a nie przypadkowy.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Zliczanie:  map.merge(klucz, 1, Integer::sum).
     *   • Grupowanie: map.computeIfAbsent(klucz, k -> new ArrayList<>()).add(wartość) — lambda, nie gotowy obiekt!
     *   • Odwracanie mapy: nowa mapa, put(wartość, klucz) — sprawdź, czy put nie nadpisuje (unikalność wartości).
     *   • Deduplikacja z kolejnością: new LinkedHashSet<>(lista).
     *   • Top-N: kopiec MIN rozmiaru N, po przekroczeniu N — poll(); na końcu odwróć wynik.
     *   • Two-sum: HashMap<wartość, indeks> w jednym przejściu, O(n) zamiast O(n²).
     *   • Okno przesuwne: ArrayDeque z indeksami, monotonicznie malejąca kolejka wartości.
     *   • Pierwszy niepowtarzający się: LinkedHashMap + pierwszy wpis z licznikiem 1.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego computeIfAbsent(klucz, k -> new ArrayList<>()) jest bezpieczniejsze niż computeIfAbsent(klucz,
     *      new ArrayList<>())?
     *   2. Co wypisze:
     *          Map<String, Integer> m = new TreeMap<>();
     *          for (String w : List.of("a", "b", "a", "a", "b")) {
     *              m.merge(w, 1, Integer::sum);
     *          }
     *          System.out.println(m);
     *   3. ZNAJDŹ BŁĄD:
     *          Map<Integer, String> odwrocona = new TreeMap<>();
     *          for (Map.Entry<String, Integer> e : oryginalna.entrySet()) {
     *              odwrocona.put(e.getKey().length(), e.getValue());   // klucz = długość napisu
     *          }
     *   4. Dlaczego w przepisie top-N używamy kopca MIN, a nie kopca MAX, skoro szukamy NAJWIĘKSZYCH elementów?
     *   5. Co wypisze:  new LinkedHashSet<>(List.of(3, 1, 3, 2, 1));  po wypisaniu (toString)?
     *   6. Dlaczego two-sum z HashMap jest szybsze niż dwie zagnieżdżone pętle porównujące każdą parę?
     *   7. Dlaczego do "pierwszego niepowtarzającego się znaku" nie nadaje się zwykły HashMap?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: zliczanie liter", Map.of('a', 2, 'b', 1), () -> exercise1("aab"));
        Check.equal("ćw. 2: dedup z kolejnością", List.of(1, 2, 3), () -> exercise2(List.of(1, 2, 2, 3, 1)));
        Check.equal("ćw. 3: bottom-N (najtańsze rosnąco)", List.of("Czekolada gorzka", "Oliwa z oliwek"),
                () -> exercise3(SampleData.products(), 2));
        Check.equal("ćw. 4: minima w oknie", List.of(-3, -3, -3, -3, 3), () -> exercise4(new int[] {1, 3, -1, -3, 5, 3, 6, 7}, 4));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", Map.of('a', 2, 'b', 1), () -> solution1("aab"));
        Check.equal("ćw. 2 (wzorzec)", List.of(1, 2, 3), () -> solution2(List.of(1, 2, 2, 3, 1)));
        Check.equal("ćw. 3 (wzorzec)", List.of("Czekolada gorzka", "Oliwa z oliwek"), () -> solution3(SampleData.products(), 2));
        Check.equal("ćw. 4 (wzorzec)", List.of(-3, -3, -3, -3, 3), () -> solution4(new int[] {1, 3, -1, -3, 5, 3, 6, 7}, 4));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): zwróć Map<Character, Integer> z liczbą wystąpień każdej litery w text. Podpowiedź: merge. */
    static Map<Character, Integer> exercise1(String text) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /** ĆWICZENIE 2 (łatwe): usuń duplikaty z listy, zachowując kolejność pierwszego wystąpienia. Podpowiedź: LinkedHashSet. */
    static <T> List<T> exercise2(List<T> source) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ wzorzec z sekcji 5): zwróć N NAJTAŃSZYCH produktów, rosnąco po cenie —
     * to lustrzane odbicie topNWithPriorityQueue (tam kopiec MIN trzymał N największych, tu ma trzymać N
     * najmniejszych — użyj odwróconego Comparatora). Podpowiedź: Comparator.comparing(Product::price).reversed()
     * jako kopiec MAX rozmiaru N, potem odwróć wynik pollowania.
     */
    static List<String> exercise3(List<Product> products, int n) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): analogicznie do slidingWindowMaximum, ale zwróć MINIMA w oknie o rozmiarze k.
     * Podpowiedź: ta sama monotoniczna kolejka z ArrayDeque, tylko warunek na tylnym końcu odwrócony
     * (usuwaj z tyłu wartości WIĘKSZE-RÓWNE nowemu elementowi zamiast mniejszych-równych).
     */
    static List<Integer> exercise4(int[] numbers, int k) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Map<Character, Integer> solution1(String text) {
        Map<Character, Integer> counts = new TreeMap<>();
        for (char c : text.toCharArray()) {
            counts.merge(c, 1, Integer::sum);
        }
        return counts;
    }

    static <T> List<T> solution2(List<T> source) {
        return new ArrayList<>(new LinkedHashSet<>(source));
    }

    static List<String> solution3(List<Product> products, int n) {
        Comparator<Product> byPriceDescThenSku = Comparator.comparing(Product::price).reversed().thenComparing(Product::sku);
        PriorityQueue<Product> heap = new PriorityQueue<>(byPriceDescThenSku);   // kopiec MAX (odwrócony comparator)

        for (Product p : products) {
            heap.offer(p);
            if (heap.size() > n) {
                heap.poll();   // wyrzuć aktualnie najdroższy — zostają N najtańszych
            }
        }

        List<Product> bottomN = new ArrayList<>();
        while (!heap.isEmpty()) {
            bottomN.add(heap.poll());   // pollowanie z kopca MAX daje kolejność MALEJĄCĄ
        }
        Collections.reverse(bottomN);   // odwracamy → rosnąco

        List<String> names = new ArrayList<>();
        for (Product p : bottomN) {
            names.add(p.name());
        }
        return names;
    }

    static List<Integer> solution4(int[] numbers, int k) {
        List<Integer> minima = new ArrayList<>();
        Deque<Integer> indices = new ArrayDeque<>();

        for (int i = 0; i < numbers.length; i++) {
            while (!indices.isEmpty() && indices.peekFirst() <= i - k) {
                indices.pollFirst();
            }
            while (!indices.isEmpty() && numbers[indices.peekLast()] >= numbers[i]) {
                indices.pollLast();
            }
            indices.offerLast(i);
            if (i >= k - 1) {
                minima.add(numbers[indices.peekFirst()]);
            }
        }
        return minima;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. computeIfAbsent(klucz, k -> new ArrayList<>()) tworzy NOWĄ listę tylko wtedy, gdy klucza faktycznie
     *      brakuje (lambda wywoływana "leniwie"). computeIfAbsent(klucz, new ArrayList<>()) tworzyłby JEDNĄ listę
     *      PRZED wywołaniem, za każdym razem tę samą — wszystkie klucze dzieliłyby wspólną listę.
     *   2. „{a=3, b=2}” — TreeMap sortuje klucze alfabetycznie, merge zlicza wystąpienia.
     *   3. Klucz (długość napisu) może się powtarzać dla różnych napisów o tej samej długości — put nadpisze
     *      poprzedni wpis bez ostrzeżenia, tracąc dane. Odwracanie po niepowtarzalnej cesze (jak w sekcji 3) jest
     *      bezpieczne; po długości — nie.
     *   4. Bo kopiec MIN pozwala szybko (O(log n)) sprawdzić i usunąć NAJMNIEJSZY z dotychczasowych N kandydatów —
     *      a to właśnie tego chcemy się pozbyć, gdy pojawi się coś większego. Kopiec MAX dawałby szybki dostęp do
     *      największego, co nie pomaga w podejmowaniu decyzji "kogo wyrzucić z aktualnej trójki".
     *   5. „[3, 1, 2]” — LinkedHashSet zachowuje kolejność PIERWSZEGO wystąpienia (3, potem 1, potem 2; drugie 3 i
     *      drugie 1 są pomijane jako duplikaty).
     *   6. Dwie zagnieżdżone pętle sprawdzają WSZYSTKIE pary — O(n²) porównań. HashMap pozwala zadać pytanie "czy
     *      widziałem już potrzebną wartość?" w czasie O(1), więc cały algorytm to jedno przejście — O(n).
     *   7. HashMap nie gwarantuje ŻADNEJ konkretnej kolejności iteracji — "pierwszy" wpis z licznikiem 1 byłby
     *      przypadkowy, a nie faktycznie pierwszym znakiem w tekście. LinkedHashMap gwarantuje kolejność wstawiania.
     */
    // </editor-fold>
}
