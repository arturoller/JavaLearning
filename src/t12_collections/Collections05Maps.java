package t12_collections;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Map — HashMap, TreeMap, LinkedHashMap: put, get, computeIfAbsent, merge
 *        (map = mapa; entry = wpis (para klucz-wartość); key = klucz; value = wartość)
 *
 * W SKRÓCIE:
 *   Map przechowuje PARY klucz → wartość, klucze są unikalne (jak w Secie), wartości mogą się powtarzać
 *   (jak w Liście). HashMap jest najszybszy, ale kolejność kluczy nieokreślona. TreeMap sortuje wg klucza
 *   i umie nawigować (firstKey, floorKey...). LinkedHashMap pamięta kolejność wstawiania — albo, opcjonalnie,
 *   kolejność DOSTĘPU, co pozwala zbudować prosty cache LRU.
 *
 * ANALOGIA: szatnia hotelowa z numerkami.
 *   Klucz to numerek, wartość to płaszcz na wieszaku pod tym numerkiem. Dwóch gości NIE MOŻE mieć tego
 *   samego numerka (klucz unikalny), ale dwaj różni goście MOGĄ mieć identyczne płaszcze (wartości mogą się
 *   powtarzać). get(numerek) zwraca płaszcz — albo nic, jeśli numerek nie istnieje ALBO jeśli pod numerkiem
 *   celowo nic nie powieszono (pusty wieszak) — sekcja 9 pokazuje, czemu to rozróżnienie ma znaczenie.
 *
 * JAK TO DZIAŁA:
 *   put(k, v)              → wstaw/nadpisz, zwraca POPRZEDNIĄ wartość (albo null).
 *   get(k)                 → wartość pod kluczem, albo null (klucza nie ma LUB wartość to null!).
 *   computeIfAbsent(k, f)  → jeśli klucza NIE MA, wstaw f(k) i zwróć; jeśli JEST, zwróć istniejącą wartość.
 *   merge(k, v, f)         → jeśli klucza NIE MA, wstaw v; jeśli JEST, wstaw f(stara, v).
 *   compute(k, f)          → wstaw f(k, staraWartośćAlboNull); f zwracające null USUWA wpis.
 *
 * SŁÓWKA:
 *   key = klucz; value = wartość; entry = wpis; put = wstaw; getOrDefault = pobierz albo domyślna;
 *   putIfAbsent = wstaw, jeśli nieobecny; computeIfAbsent = oblicz, jeśli nieobecny; merge = scal;
 *   compute = przelicz; access order = kolejność dostępu; eldest = najstarszy (tu: najdawniej używany).
 *
 * ZOBACZ TEŻ: t12_collections/Collections01Overview (Map jako osobna hierarchia), t12_collections/Collections04Sets
 *             (Set też polega na equals/hashCode kluczy), t12_collections/Collections07ComparableComparator
 *             (Comparable dla kluczy TreeMap), t12_collections/Collections10Patterns (więcej przepisów:
 *             odwracanie mapy, LRU, zliczanie), t12_collections/Collections11HashingInternals (HashMap
 *             w środku), t08_enums/Enums04EnumMapSet (EnumMap — mapa dla kluczy-enumów).
 * </pre>
 */
public class Collections05Maps {

    public static void main(String[] args) {
        title("Collections05 — Map: HashMap, TreeMap, LinkedHashMap");

        hashMapBasics();          // put/get/containsKey/remove/getOrDefault
        iterateEntrySet();        // iteracja po entrySet
        putIfAbsentDemo();        // putIfAbsent
        computeIfAbsentDemo();    // computeIfAbsent — mapa list
        mergeWordCount();         // merge — zliczanie słów
        computeDemo();            // compute
        treeMapNavigation();      // TreeMap: nawigacja
        linkedHashMapLru();       // LinkedHashMap: mini LRU
        nullPitfall();             // pułapka: null z brakującego klucza vs wartości null
        exercises();               // ćwiczenia
    }

    // =================================================================================================
    // 1. HashMap: put, get, containsKey, remove, getOrDefault
    // =================================================================================================

    /** 1. put() zwraca POPRZEDNIĄ wartość pod kluczem (albo null, jeśli klucza nie było). */
    static void hashMapBasics() {
        section("1. HashMap: put, get, containsKey, remove, getOrDefault");

        Map<String, Integer> stock = new HashMap<>();   // stock = stan magazynowy
        stock.put("Laptop", 7);
        stock.put("Monitor", 4);
        Integer previous = stock.put("Laptop", 10);   // nadpisanie — zwraca STARĄ wartość
        show("poprzednia wartość dla \"Laptop\"", previous);
        // WYNIK: poprzednia wartość dla "Laptop" → 7

        show("get(\"Laptop\") po nadpisaniu", stock.get("Laptop"));
        // WYNIK: get("Laptop") po nadpisaniu → 10
        show("get(\"Klawiatura\") (klucza nie ma)", stock.get("Klawiatura"));
        // WYNIK: get("Klawiatura") (klucza nie ma) → null
        show("containsKey(\"Monitor\")", stock.containsKey("Monitor"));
        // WYNIK: containsKey("Monitor") → true

        Integer removed = stock.remove("Monitor");
        show("remove(\"Monitor\") zwróciło", removed);
        // WYNIK: remove("Monitor") zwróciło → 4
        show("getOrDefault(\"Monitor\", 0) — po usunięciu", stock.getOrDefault("Monitor", 0));
        // WYNIK: getOrDefault("Monitor", 0) — po usunięciu → 0
        show("getOrDefault(\"Laptop\", 0)", stock.getOrDefault("Laptop", 0));
        // WYNIK: getOrDefault("Laptop", 0) → 10

        // PUŁAPKA: nigdy nie wypisuj HashMap wprost (println/show(label, hashMapa)) — kolejność kluczy jest
        //   NIEOKREŚLONA. Do wypisywania użyj TreeMap (posortowana) albo LinkedHashMap (kolejność
        //   wstawiania) — sekcja 2.
    }

    // =================================================================================================
    // 2. ITERACJA po entrySet (przez TreeMap — deterministycznie)
    // =================================================================================================

    /** 2. entrySet() daje pary klucz-wartość naraz. TreeMap iteruje posortowany wg klucza — bezpiecznie wypisywać. */
    static void iterateEntrySet() {
        section("2. Iteracja po entrySet (TreeMap — deterministyczna kolejność)");

        Map<String, Integer> prices = new TreeMap<>();   // prices = ceny
        prices.put("banan", 3);
        prices.put("jabłko", 4);
        prices.put("gruszka", 5);

        for (Map.Entry<String, Integer> e : prices.entrySet()) {
            System.out.println(e.getKey() + " → " + e.getValue() + " zł");
        }
        // WYNIK: banan → 3 zł
        // WYNIK: gruszka → 5 zł
        // WYNIK: jabłko → 4 zł

        show("keySet()", prices.keySet());
        // WYNIK: keySet() → [banan, gruszka, jabłko]
        show("values()", prices.values());
        // WYNIK: values() → [3, 5, 4]

        // DOBRA PRAKTYKA: entrySet() jest wydajniejszy niż pętla „for (String k : map.keySet()) map.get(k)”
        //   — nie robi drugiego wyszukiwania po kluczu dla każdej wartości.
    }

    // =================================================================================================
    // 3. putIfAbsent
    // =================================================================================================

    /** 3. putIfAbsent(k, v) wstawia TYLKO gdy klucza jeszcze nie ma — inaczej nic nie robi (nie nadpisuje). */
    static void putIfAbsentDemo() {
        section("3. putIfAbsent — wstaw tylko, gdy klucza brak");

        Map<String, Integer> counts = new HashMap<>();
        counts.put("a", 1);
        counts.putIfAbsent("a", 99);   // "a" już jest — NIC się nie zmienia
        counts.putIfAbsent("b", 2);    // "b" nie było — dodaje

        show("\"a\" po putIfAbsent(\"a\", 99)", counts.get("a"));
        // WYNIK: "a" po putIfAbsent("a", 99) → 1
        show("\"b\" po putIfAbsent(\"b\", 2)", counts.get("b"));
        // WYNIK: "b" po putIfAbsent("b", 2) → 2
    }

    // =================================================================================================
    // 4. computeIfAbsent — MAPA LIST (grupowanie)
    // =================================================================================================

    /**
     * 4. computeIfAbsent(k, f) — jeśli klucza nie ma, WSTAWIA f(k) i go zwraca; jeśli jest, zwraca istniejącą
     * wartość. Klasyczne zastosowanie: grupowanie do Map{@code <Klucz, List<Element>>} bez ręcznego sprawdzania null.
     */
    static void computeIfAbsentDemo() {
        section("4. computeIfAbsent — grupowanie do mapy list");

        Map<Category, List<String>> byCategory = new TreeMap<>();
        for (Product p : SampleData.products()) {
            byCategory.computeIfAbsent(p.category(), k -> new ArrayList<>()).add(p.name());
        }
        show("produkty wg kategorii", byCategory);
        // WYNIK: produkty wg kategorii → {ELEKTRONIKA=[Laptop Pro 14, Smartfon X, Słuchawki BT, Monitor 27 cali], SPOZYWCZE=[Kawa ziarnista 1kg, Czekolada gorzka, Oliwa z oliwek], KSIAZKI=[Czysty kod, Java. Podstawy, Wzorce projektowe], ODZIEZ=[Kurtka zimowa, T-shirt bawełniany], DOM=[Ekspres do kawy, Lampka biurkowa]}

        // DOBRA PRAKTYKA: computeIfAbsent(k, x -> new ArrayList<>()).add(element) to IDIOM — jedna linia
        //   zamiast: „if (map.get(k) == null) map.put(k, new ArrayList<>()); map.get(k).add(element);”.
        //   Zobacz też ćwiczenie 4 (PRZEPISZ) na końcu lekcji.
    }

    // =================================================================================================
    // 5. merge — ZLICZANIE SŁÓW
    // =================================================================================================

    /** 5. merge(k, v, f) — jeśli klucza nie ma, wstawia v; jeśli jest, wstawia f(staraWartość, v). Klasyka: liczniki. */
    static void mergeWordCount() {
        section("5. merge — zliczanie wystąpień");

        Map<String, Integer> wordCount = new TreeMap<>();
        for (String w : SampleData.words()) {
            wordCount.merge(w, 1, Integer::sum);   // brak klucza -> 1; jest klucz -> stara + 1
        }
        show("liczba wystąpień słów", wordCount);
        // WYNIK: liczba wystąpień słów → {enum=1, java=3, kolekcja=1, lambda=1, lista=1, mapa=1, optional=1, rekord=1, stream=2}

        // DOBRA PRAKTYKA: merge(k, 1, Integer::sum) to najkrótszy sposób na licznik w mapie — krótszy niż
        //   computeIfAbsent(k, x -> 0) + put(k, get(k) + 1).
    }

    // =================================================================================================
    // 6. compute
    // =================================================================================================

    /** 6. compute(k, f) przelicza wartość funkcją f(klucz, staraWartośćAlboNull). Zwrócenie null z f USUWA wpis. */
    static void computeDemo() {
        section("6. compute — dowolne przeliczenie wartości");

        Map<String, Integer> inventory = new TreeMap<>();   // inventory = magazyn
        inventory.put("śruby", 100);
        inventory.compute("śruby", (k, v) -> v - 30);
        show("śruby po compute(v -> v - 30)", inventory.get("śruby"));
        // WYNIK: śruby po compute(v -> v - 30) → 70

        inventory.compute("gwoździe", (k, v) -> (v == null) ? 50 : v + 50);   // klucza nie było -> v to null!
        show("gwoździe po compute (klucza wcześniej nie było)", inventory.get("gwoździe"));
        // WYNIK: gwoździe po compute (klucza wcześniej nie było) → 50

        inventory.compute("gwoździe", (k, v) -> null);   // zwrócenie null z compute USUWA wpis
        show("gwoździe po compute(...-> null) — czy nadal jest?", inventory.containsKey("gwoździe"));
        // WYNIK: gwoździe po compute(...-> null) — czy nadal jest? → false

        // PUŁAPKA: w compute() funkcja MUSI obsłużyć v == null (klucza mogło nie być) — inaczej v - 30 na
        //   nieistniejącym kluczu rzuci NullPointerException (unboxing null Integer).
    }

    // =================================================================================================
    // 7. TreeMap — NAWIGACJA (NavigableMap)
    // =================================================================================================

    /** 7. NavigableMap: firstKey/lastKey, floorKey/ceilingKey (włącznie), headMap/tailMap (widoki na fragment). */
    static void treeMapNavigation() {
        section("7. TreeMap: firstKey, floorKey, headMap, tailMap");

        NavigableMap<Integer, String> schedule = new TreeMap<>();   // schedule = harmonogram
        schedule.put(8, "otwarcie");
        schedule.put(12, "przerwa");
        schedule.put(16, "spotkanie");
        schedule.put(20, "zamknięcie");

        show("firstKey()", schedule.firstKey());
        // WYNIK: firstKey() → 8
        show("lastKey()", schedule.lastKey());
        // WYNIK: lastKey() → 20
        show("floorKey(14) (największy klucz ≤ 14)", schedule.floorKey(14));
        // WYNIK: floorKey(14) (największy klucz ≤ 14) → 12
        show("ceilingKey(14) (najmniejszy klucz ≥ 14)", schedule.ceilingKey(14));
        // WYNIK: ceilingKey(14) (najmniejszy klucz ≥ 14) → 16
        show("headMap(16) (klucze < 16)", schedule.headMap(16));
        // WYNIK: headMap(16) (klucze < 16) → {8=otwarcie, 12=przerwa}
        show("tailMap(16) (klucze ≥ 16)", schedule.tailMap(16));
        // WYNIK: tailMap(16) (klucze ≥ 16) → {16=spotkanie, 20=zamknięcie}
    }

    // =================================================================================================
    // 8. LinkedHashMap — KOLEJNOŚĆ DOSTĘPU + removeEldestEntry = MINI LRU
    // =================================================================================================

    /**
     * 8. LinkedHashMap(pojemność, loadFactor, accessOrder=true) sortuje iterację wg kolejności DOSTĘPU
     * (najdawniej używany na początku). Nadpisanie removeEldestEntry pozwala zbudować cache o stałym
     * rozmiarze: LRU = Least Recently Used (najdawniej używany wylatuje pierwszy).
     */
    static void linkedHashMapLru() {
        section("8. LinkedHashMap: kolejność dostępu, mini cache LRU");

        Map<String, Integer> lru = new LinkedHashMap<String, Integer>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                return size() > 3;   // pojemność cache = 3 elementy
            }
        };
        lru.put("a", 1);
        lru.put("b", 2);
        lru.put("c", 3);
        show("po wstawieniu a, b, c", lru.keySet());
        // WYNIK: po wstawieniu a, b, c → [a, b, c]

        lru.get("a");     // dostęp do "a" — przesuwa go na koniec (najświeższy)
        lru.put("d", 4);   // 4. element — przekracza limit 3, usuwa NAJDAWNIEJ używany ("b")
        show("po get(\"a\") i put(\"d\") — limit=3", lru.keySet());
        // WYNIK: po get("a") i put("d") — limit=3 → [c, a, d]

        // JAK TO DZIAŁA: każdy get()/put() na kluczu istniejącym przesuwa go na KONIEC kolejności. Nowy
        //   wpis też ląduje na końcu. removeEldestEntry jest wołane PO KAŻDYM wstawieniu — zwrócenie true
        //   usuwa element na POCZĄTKU (najdawniej używany). To CAŁA implementacja LRU w kilku liniach.
    }

    // =================================================================================================
    // 9. PUŁAPKA: null z brakującego klucza kontra klucz z wartością null
    // =================================================================================================

    /**
     * 9. get(k) zwraca null w DWÓCH różnych sytuacjach: klucza nie ma ALBO klucz jest, ale jego wartość TO
     * null. containsKey rozróżnia te sytuacje. getOrDefault NIE chroni przed wartością null pod kluczem!
     */
    static void nullPitfall() {
        section("9. Pułapka: null z brakującego klucza a klucz z wartością null");

        Map<String, String> emails = new HashMap<>();
        emails.put("Jan", "jan@example.com");
        emails.put("Anna", null);   // klucz JEST, ale świadomie (albo przez błąd) wartość to null

        show("get(\"Jan\")", emails.get("Jan"));
        // WYNIK: get("Jan") → jan@example.com
        show("get(\"Anna\") — klucz JEST, wartość null", emails.get("Anna"));
        // WYNIK: get("Anna") — klucz JEST, wartość null → null
        show("get(\"Ewa\") — klucza W OGÓLE nie ma", emails.get("Ewa"));
        // WYNIK: get("Ewa") — klucza W OGÓLE nie ma → null

        note("Oba powyższe get() zwróciły null — ale to DWIE różne sytuacje. Rozróżnia je containsKey:");
        // WYNIK: ℹ Oba powyższe get() zwróciły null — ale to DWIE różne sytuacje. Rozróżnia je containsKey:
        show("containsKey(\"Anna\")", emails.containsKey("Anna"));
        // WYNIK: containsKey("Anna") → true
        show("containsKey(\"Ewa\")", emails.containsKey("Ewa"));
        // WYNIK: containsKey("Ewa") → false

        show("getOrDefault(\"Anna\", \"brak\") — UWAGA", emails.getOrDefault("Anna", "brak"));
        // WYNIK: getOrDefault("Anna", "brak") — UWAGA → null
        show("getOrDefault(\"Ewa\", \"brak\")", emails.getOrDefault("Ewa", "brak"));
        // WYNIK: getOrDefault("Ewa", "brak") → brak

        // PUŁAPKA: getOrDefault CHRONI tylko przed BRAKIEM klucza, nie przed wartością null POD kluczem —
        //   jeśli klucz istnieje i jego wartość to null, getOrDefault i tak zwróci null (bo klucz „jest”).
        //   Jeśli Twoja mapa może mieć wartości null, zawsze sprawdzaj containsKey ALBO — lepiej —
        //   unikaj wkładania null jako wartości (użyj Optional albo osobnej flagi).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Map: klucze unikalne (jak Set), wartości mogą się powtarzać (jak List). put() zwraca STARĄ
     *     wartość (albo null).
     *   • Nigdy nie wypisuj HashMap wprost — kolejność kluczy nieokreślona. Użyj TreeMap (posortowana)
     *     albo LinkedHashMap (kolejność wstawiania/dostępu).
     *   • putIfAbsent: wstaw tylko gdy brak klucza. computeIfAbsent: jw., ale wstawia WYNIK FUNKCJI —
     *     idealne do map list (grupowanie). merge: licznik / scalanie wartości. compute: dowolna zmiana
     *     (funkcja zwracająca null USUWA wpis).
     *   • TreeMap (NavigableMap): firstKey/lastKey, floorKey/ceilingKey (włącznie), headMap/tailMap.
     *   • LinkedHashMap(pojemność, loadFactor, true) + removeEldestEntry = mini cache LRU w kilku liniach.
     *   • get(k) == null: klucza NIE MA albo wartość pod kluczem TO null — rozróżnia containsKey.
     *     getOrDefault NIE chroni przed wartością null pod istniejącym kluczem.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego map.get(klucz) zwracające null NIE zawsze oznacza, że klucza nie ma w mapie?
     *   2. Co wypisze:
     *          Map<String, Integer> m = new HashMap<>();
     *          m.put("x", null);
     *          System.out.println(m.getOrDefault("x", 99));
     *      ?
     *   3. ZNAJDŹ BŁĄD:
     *          Map<String, List<String>> byCategory = new HashMap<>();
     *          byCategory.get(category).add(name);
     *   4. Co wypisze:
     *          Map<String, Integer> counts = new TreeMap<>();
     *          counts.merge("a", 1, Integer::sum);
     *          counts.merge("a", 1, Integer::sum);
     *          counts.merge("a", 1, Integer::sum);
     *          System.out.println(counts.get("a"));
     *      ?
     *   5. Czym różni się put() od putIfAbsent()?
     *   6. Do czego służy trzeci parametr konstruktora LinkedHashMap (accessOrder=true) w połączeniu
     *      z nadpisanym removeEldestEntry?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: stan magazynowy wg SKU dla KSIAZKI", Map.of("KSI-001", 15, "KSI-002", 9, "KSI-003", 3),
                () -> stockBySku(SampleData.products(), Category.KSIAZKI));
        Check.equal("ćw. 2: pracownicy wg działu (computeIfAbsent)",
                Map.of(Department.IT, List.of("Anna Nowak", "Piotr Kowalski", "Michał Lewandowski", "Ewa Woźniak"),
                        Department.HR, List.of("Katarzyna Wiśniewska"),
                        Department.SPRZEDAZ, List.of("Tomasz Wójcik", "Krzysztof Szymański"),
                        Department.KSIEGOWOSC, List.of("Magdalena Kamińska"),
                        Department.MARKETING, List.of("Agnieszka Zielińska", "Paweł Dąbrowski")),
                () -> namesByDepartment(SampleData.employees()));
        Check.equal("ćw. 3: liczba produktów wg kategorii (merge)",
                Map.of(Category.ELEKTRONIKA, 4, Category.SPOZYWCZE, 3, Category.KSIAZKI, 3, Category.ODZIEZ, 2, Category.DOM, 2),
                () -> countByCategory(SampleData.products()));
        Check.equal("ćw. 4: produkty wg kategorii (PRZEPISZ na computeIfAbsent)",
                Map.of(Category.ELEKTRONIKA, List.of("Laptop Pro 14", "Smartfon X", "Słuchawki BT", "Monitor 27 cali"),
                        Category.SPOZYWCZE, List.of("Kawa ziarnista 1kg", "Czekolada gorzka", "Oliwa z oliwek"),
                        Category.KSIAZKI, List.of("Czysty kod", "Java. Podstawy", "Wzorce projektowe"),
                        Category.ODZIEZ, List.of("Kurtka zimowa", "T-shirt bawełniany"),
                        Category.DOM, List.of("Ekspres do kawy", "Lampka biurkowa")),
                () -> groupByCategory(SampleData.products()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", Map.of("KSI-001", 15, "KSI-002", 9, "KSI-003", 3),
                () -> solutionStockBySku(SampleData.products(), Category.KSIAZKI));
        Check.equal("ćw. 2 (wzorzec)",
                Map.of(Department.IT, List.of("Anna Nowak", "Piotr Kowalski", "Michał Lewandowski", "Ewa Woźniak"),
                        Department.HR, List.of("Katarzyna Wiśniewska"),
                        Department.SPRZEDAZ, List.of("Tomasz Wójcik", "Krzysztof Szymański"),
                        Department.KSIEGOWOSC, List.of("Magdalena Kamińska"),
                        Department.MARKETING, List.of("Agnieszka Zielińska", "Paweł Dąbrowski")),
                () -> solutionNamesByDepartment(SampleData.employees()));
        Check.equal("ćw. 3 (wzorzec)",
                Map.of(Category.ELEKTRONIKA, 4, Category.SPOZYWCZE, 3, Category.KSIAZKI, 3, Category.ODZIEZ, 2, Category.DOM, 2),
                () -> solutionCountByCategory(SampleData.products()));
        Check.equal("ćw. 4 (wzorzec)",
                Map.of(Category.ELEKTRONIKA, List.of("Laptop Pro 14", "Smartfon X", "Słuchawki BT", "Monitor 27 cali"),
                        Category.SPOZYWCZE, List.of("Kawa ziarnista 1kg", "Czekolada gorzka", "Oliwa z oliwek"),
                        Category.KSIAZKI, List.of("Czysty kod", "Java. Podstawy", "Wzorce projektowe"),
                        Category.ODZIEZ, List.of("Kurtka zimowa", "T-shirt bawełniany"),
                        Category.DOM, List.of("Ekspres do kawy", "Lampka biurkowa")),
                () -> solutionGroupByCategory(SampleData.products()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć mapę sku → stock, TYLKO dla produktów z podanej kategorii, posortowaną
     * wg klucza. Podpowiedź: {@code new TreeMap<>()} + pętla z if (p.category() == category) + put.
     */
    static Map<String, Integer> stockBySku(List<Product> products, Category category) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 2 (średnie): zgrupuj imiona i nazwiska pracowników wg działu, posortowane wg działu.
     * Podpowiedź: {@code computeIfAbsent(e.department(), k -> new ArrayList<>()).add(e.name())} — sekcja 4.
     */
    static Map<Department, List<String>> namesByDepartment(List<Employee> employees) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): policz, ile produktów jest w każdej kategorii. Podpowiedź:
     * {@code counts.merge(p.category(), 1, Integer::sum)} — sekcja 5.
     */
    static Map<Category, Integer> countByCategory(List<Product> products) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze, PRZEPISZ): poniższy kod grupuje produkty wg kategorii ręcznym sprawdzaniem
     * null — działa, ale to więcej kodu niż potrzeba:
     * <pre>{@code
     * Map<Category, List<String>> map = new TreeMap<>();
     * for (Product p : products) {
     *     List<String> list = map.get(p.category());
     *     if (list == null) {
     *         list = new ArrayList<>();
     *         map.put(p.category(), list);
     *     }
     *     list.add(p.name());
     * }
     * return map;
     * }</pre>
     * Przepisz to na JEDNĄ linię wewnątrz pętli, używając computeIfAbsent (sekcja 4).
     */
    static Map<Category, List<String>> groupByCategory(List<Product> products) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Map<String, Integer> solutionStockBySku(List<Product> products, Category category) {
        Map<String, Integer> result = new TreeMap<>();
        for (Product p : products) {
            if (p.category() == category) {
                result.put(p.sku(), p.stock());
            }
        }
        return result;
    }

    static Map<Department, List<String>> solutionNamesByDepartment(List<Employee> employees) {
        Map<Department, List<String>> result = new TreeMap<>();
        for (Employee e : employees) {
            result.computeIfAbsent(e.department(), k -> new ArrayList<>()).add(e.name());
        }
        return result;
    }

    static Map<Category, Integer> solutionCountByCategory(List<Product> products) {
        Map<Category, Integer> counts = new TreeMap<>();
        for (Product p : products) {
            counts.merge(p.category(), 1, Integer::sum);
        }
        return counts;
    }

    static Map<Category, List<String>> solutionGroupByCategory(List<Product> products) {
        Map<Category, List<String>> map = new TreeMap<>();
        for (Product p : products) {
            map.computeIfAbsent(p.category(), k -> new ArrayList<>()).add(p.name());
        }
        return map;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo get() zwraca null w dwóch różnych sytuacjach: klucza nie ma w mapie ALBO klucz jest, ale jego
     *      wartość to (celowo albo przez błąd) null. Rozróżnia je containsKey(klucz).
     *   2. „null” — klucz "x" ISTNIEJE (wartość to null), więc getOrDefault zwraca WARTOŚĆ spod klucza
     *      (null), a nie domyślną (99). getOrDefault chroni tylko przed BRAKIEM klucza.
     *   3. Jeśli klucza category jeszcze nie ma w mapie, get(category) zwraca null, a wywołanie .add(name)
     *      na null rzuca NullPointerException. Popraw: byCategory.computeIfAbsent(category,
     *      k -> new ArrayList<>()).add(name).
     *   4. „3” — trzy wywołania merge("a", 1, Integer::sum) dodają kolejno: 1 (klucza nie było), potem
     *      1+1=2, potem 2+1=3.
     *   5. put() ZAWSZE wstawia/nadpisuje wartość pod kluczem (zwraca starą wartość). putIfAbsent()
     *      wstawia TYLKO, gdy klucza jeszcze nie ma — jeśli jest, nic nie robi i zwraca ISTNIEJĄCĄ wartość.
     *   6. accessOrder=true sprawia, że LinkedHashMap iteruje wg kolejności OSTATNIEGO DOSTĘPU (get/put),
     *      a nie wstawiania — najdawniej używany element jest pierwszy. removeEldestEntry, wołane po
     *      każdym wstawieniu, pozwala automatycznie usunąć ten pierwszy (najdawniej używany) element,
     *      gdy mapa przekroczy limit rozmiaru — to cały mechanizm cache LRU.
     */
    // </editor-fold>
}
