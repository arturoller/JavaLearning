package t12_collections;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Product;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Mapa terenu — hierarchia kolekcji i jak wybrać właściwą
 *        (collection = kolekcja; hierarchy = hierarchia; implementation = implementacja)
 *
 * W SKRÓCIE:
 *   Java Collections Framework to zestaw INTERFEJSÓW (List, Set, Queue/Deque, Map) i ich IMPLEMENTACJI
 *   (ArrayList, HashSet, TreeMap, ...). Kod piszemy przeciwko interfejsom, a konkretną implementację
 *   dobieramy do potrzeb: czy ważna jest kolejność? czy mogą być duplikaty? jak szybki ma być dostęp?
 *
 * ANALOGIA: organizacja rzeczy w domu.
 *   List to szuflada — rzeczy leżą w kolejności, w jakiej je włożyłeś, i mogą się powtarzać (dwie identyczne
 *   koszulki). Set to wieszak na jeden komplet kluczy do danych drzwi — nie trzymasz dwóch identycznych
 *   kompletów. Map to szafka z podpisanymi, ponumerowanymi przegródkami (klucz → zawartość) — każdy numer
 *   ma dokładnie jedną przegródkę.
 *
 * JAK TO DZIAŁA:
 *   Iterable{@code <E>}
 *      └── Collection{@code <E>}
 *             ├── List{@code <E>}     (ArrayList, LinkedList)         — kolejność wstawiania, duplikaty OK
 *             ├── Set{@code <E>}      (HashSet, LinkedHashSet, TreeSet) — bez duplikatów
 *             └── Queue{@code <E>}    (ArrayDeque, PriorityQueue)
 *                    └── Deque{@code <E>} (ArrayDeque)                — kolejka DWUSTRONNA
 *
 *   Map{@code <K,V>}   ← OSOBNA hierarchia! Map NIE dziedziczy z Collection.
 *      (HashMap, LinkedHashMap, TreeMap)                              — pary klucz → wartość
 *
 *   „Programowanie do interfejsu”: {@code List<String> x = new ArrayList<>();} — typ zmiennej to interfejs,
 *   implementację można później podmienić w jednym miejscu (deklaracji), bez zmiany reszty kodu.
 *
 * SŁÓWKA:
 *   interface = interfejs; implementation = implementacja; hierarchy = hierarchia; duplicate = duplikat;
 *   order = kolejność; unspecified = nieokreślony (nieprzewidywalny); immutable = niemodyfikowalny;
 *   factory method = metoda fabrykująca; requirement = wymaganie/potrzeba.
 *
 * ZOBACZ TEŻ: t12_collections/Collections02Lists (listy), t12_collections/Collections04Sets (zbiory),
 *             t12_collections/Collections05Maps (mapy), t12_collections/Collections06QueuesDeques (kolejki),
 *             t12_collections/Collections07ComparableComparator (sortowanie własnymi regułami).
 * </pre>
 */
public class Collections01Overview {

    public static void main(String[] args) {
        title("Collections01 — mapa terenu: hierarchia i wybór kolekcji");

        hierarchy();             // hierarchy = hierarchia
        implementationTable();   // implementation table = tabela implementacji
        programToInterface();    // program to interface = programowanie do interfejsu
        duplicatesRule();        // duplicates rule = reguła duplikatów
        nullRule();               // null rule = reguła dotycząca null
        immutableFactories();    // immutable factories = niemodyfikowalne fabryki
        decisionGuide();         // decision guide = przewodnik decyzyjny
        whatsNext();              // what's next = co dalej
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. HIERARCHIA: Iterable → Collection → List/Set/Queue; Map OSOBNO
    // =================================================================================================

    /**
     * 1. instanceof (czy obiekt JEST typu X) pokazuje hierarchię „na żywo”. Map celowo NIE implementuje
     * Collection ani Iterable — nie da się jej użyć wprost w pętli for-each.
     */
    static void hierarchy() {
        section("1. Hierarchia: Iterable → Collection → List/Set/Queue; Map osobno");

        List<String> list = new ArrayList<>();      // list = lista
        Set<String> set = new HashSet<>();           // set = zbiór
        Queue<String> queue = new ArrayDeque<>();    // queue = kolejka
        Map<String, String> map = new HashMap<>();   // map = mapa

        show("list instanceof Collection", list instanceof Collection);
        // WYNIK: list instanceof Collection → true
        show("list instanceof Iterable", list instanceof Iterable);
        // WYNIK: list instanceof Iterable → true
        show("set instanceof Collection", set instanceof Collection);
        // WYNIK: set instanceof Collection → true
        show("queue instanceof Collection", queue instanceof Collection);
        // WYNIK: queue instanceof Collection → true
        show("map instanceof Collection", map instanceof Collection);
        // WYNIK: map instanceof Collection → false
        show("map instanceof Iterable", map instanceof Iterable);
        // WYNIK: map instanceof Iterable → false

        note("Map to osobna hierarchia: klucz → wartość, a nie pojedyncze elementy do iterowania.");
        // WYNIK: ℹ Map to osobna hierarchia: klucz → wartość, a nie pojedyncze elementy do iterowania.

        // PUŁAPKA: skoro Map nie jest Iterable, nie da się napisać `for (var x : map)`. Trzeba iterować po
        //   map.entrySet(), map.keySet() albo map.values() — zobacz Collections05Maps.
    }

    // =================================================================================================
    // 2. TABELA IMPLEMENTACJI
    // =================================================================================================

    /** Jeden wiersz tabeli: interfejs, konkretna implementacja i jej reguły. */
    private record ImplInfo(String interfaceName, String implementation, String order, String duplicates,
                             String nulls) {
    }

    private static final List<ImplInfo> IMPLEMENTATIONS = List.of(
            new ImplInfo("List", "ArrayList", "wg indeksu (kolejność wstawiania)", "tak", "elementy: tak"),
            new ImplInfo("List", "LinkedList", "wg indeksu (kolejność wstawiania)", "tak", "elementy: tak"),
            new ImplInfo("Set", "HashSet", "nieokreślona, może się zmienić", "nie", "jeden null: tak"),
            new ImplInfo("Set", "LinkedHashSet", "kolejność wstawiania", "nie", "jeden null: tak"),
            new ImplInfo("Set", "TreeSet", "sortowana (naturalna lub Comparator)", "nie", "NIE (NullPointerException)"),
            new ImplInfo("Queue/Deque", "ArrayDeque", "FIFO (kolejka) albo LIFO (stos)", "tak", "NIE (NullPointerException)"),
            new ImplInfo("Queue", "PriorityQueue", "wg priorytetu TYLKO przy poll, nie w iteracji", "tak", "NIE (NullPointerException)"),
            new ImplInfo("Map", "HashMap", "kluczy: nieokreślona", "klucze: nie", "jeden null-klucz: tak"),
            new ImplInfo("Map", "LinkedHashMap", "kluczy: wstawiania (opcjonalnie dostępu)", "klucze: nie", "jeden null-klucz: tak"),
            new ImplInfo("Map", "TreeMap", "kluczy: sortowana wg klucza", "klucze: nie", "NIE (NullPointerException)")
    );

    /**
     * 2. Tabela — najczęściej używane implementacje i ich reguły. Kolejność, duplikaty i null to TRZY
     * pytania, które warto zadać sobie PRZED wyborem kolekcji.
     */
    static void implementationTable() {
        section("2. Tabela implementacji: kolejność, duplikaty, null");

        for (ImplInfo info : IMPLEMENTATIONS) {
            System.out.println("  " + info.interfaceName() + " / " + info.implementation() + " → kolejność: "
                    + info.order() + " | duplikaty: " + info.duplicates() + " | null: " + info.nulls());
        }
        // WYNIK:   List / ArrayList → kolejność: wg indeksu (kolejność wstawiania) | duplikaty: tak | null: elementy: tak
        // WYNIK:   List / LinkedList → kolejność: wg indeksu (kolejność wstawiania) | duplikaty: tak | null: elementy: tak
        // WYNIK:   Set / HashSet → kolejność: nieokreślona, może się zmienić | duplikaty: nie | null: jeden null: tak
        // WYNIK:   Set / LinkedHashSet → kolejność: kolejność wstawiania | duplikaty: nie | null: jeden null: tak
        // WYNIK:   Set / TreeSet → kolejność: sortowana (naturalna lub Comparator) | duplikaty: nie | null: NIE (NullPointerException)
        // WYNIK:   Queue/Deque / ArrayDeque → kolejność: FIFO (kolejka) albo LIFO (stos) | duplikaty: tak | null: NIE (NullPointerException)
        // WYNIK:   Queue / PriorityQueue → kolejność: wg priorytetu TYLKO przy poll, nie w iteracji | duplikaty: tak | null: NIE (NullPointerException)
        // WYNIK:   Map / HashMap → kolejność: kluczy: nieokreślona | duplikaty: klucze: nie | null: jeden null-klucz: tak
        // WYNIK:   Map / LinkedHashMap → kolejność: kluczy: wstawiania (opcjonalnie dostępu) | duplikaty: klucze: nie | null: jeden null-klucz: tak
        // WYNIK:   Map / TreeMap → kolejność: kluczy: sortowana wg klucza | duplikaty: klucze: nie | null: NIE (NullPointerException)

        // DOBRA PRAKTYKA: wybieraj implementację na podstawie tej tabeli (potrzeby), a nie przyzwyczajenia —
        //   „zawsze biorę ArrayList/HashMap” bywa OK, ale czasem TreeMap albo LinkedHashSet oszczędzi Ci
        //   ręcznego sortowania albo deduplikacji później.
    }

    // =================================================================================================
    // 3. PROGRAMOWANIE DO INTERFEJSU
    // =================================================================================================

    /**
     * 3. Zmienna typu List{@code <String>} nie wie (i nie musi wiedzieć), czy pod spodem jest ArrayList czy
     * LinkedList. List.equals porównuje ZAWARTOŚĆ, nie klasę implementacji.
     */
    static void programToInterface() {
        section("3. Programowanie do interfejsu");

        List<String> byArray = new ArrayList<>(List.of("Ala", "Bob"));
        List<String> byLinked = new LinkedList<>(List.of("Ala", "Bob"));

        show("ArrayList.equals(LinkedList)", byArray.equals(byLinked));
        // WYNIK: ArrayList.equals(LinkedList) → true

        note("Ta sama zawartość, różne implementacje — obie to po prostu List.");
        // WYNIK: ℹ Ta sama zawartość, różne implementacje — obie to po prostu List.

        // DOBRA PRAKTYKA: deklaruj zmienne i parametry jako interfejs (List<String>), nie jako konkretną
        //   klasę (ArrayList<String>). Gdy okaże się, że LinkedHashSet pasuje lepiej niż ArrayList, zmieniasz
        //   JEDNO miejsce (`new ...()`), a nie każdą sygnaturę metody w kodzie.
    }

    // =================================================================================================
    // 4. DUPLIKATY: List pozwala, Set nie
    // =================================================================================================

    /** 4. add() na liście zawsze się udaje. add() na secie zwraca false, gdy element już tam jest. */
    static void duplicatesRule() {
        section("4. Duplikaty: List pozwala, Set — nie");

        List<String> listWithDuplicate = new ArrayList<>();
        listWithDuplicate.add("Java");
        listWithDuplicate.add("Java");
        show("lista z duplikatem", listWithDuplicate);
        // WYNIK: lista z duplikatem → [Java, Java]

        Set<String> setNoDuplicate = new LinkedHashSet<>();   // LinkedHashSet = zbiór z kolejnością wstawiania
        boolean firstAdd = setNoDuplicate.add("Java");
        boolean secondAdd = setNoDuplicate.add("Java");       // ten sam element drugi raz
        show("pierwsze add() zwróciło", firstAdd);
        // WYNIK: pierwsze add() zwróciło → true
        show("drugie add() (duplikat) zwróciło", secondAdd);
        // WYNIK: drugie add() (duplikat) zwróciło → false
        show("rozmiar zbioru", setNoDuplicate.size());
        // WYNIK: rozmiar zbioru → 1

        // PUŁAPKA: „duplikat” w Secie zależy od equals()/hashCode() elementu. Dla własnych klas bez
        //   nadpisanego equals dwa obiekty o tych samych polach NIE są duplikatem (są różnymi obiektami) —
        //   Collections04Sets pokazuje to na przykładzie.
    }

    // =================================================================================================
    // 5. NULL: gdzie wolno, a gdzie nie
    // =================================================================================================

    /** 5. ArrayList i HashSet/HashMap tolerują null (HashSet/HashMap — tylko JEDEN). TreeSet i ArrayDeque — nie. */
    static void nullRule() {
        section("5. Null: gdzie wolno, a gdzie nie");

        List<String> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        show("ArrayList z null", listWithNull);
        // WYNIK: ArrayList z null → [null]

        Set<String> hashSetNull = new HashSet<>();
        show("HashSet.add(null)", hashSetNull.add(null));
        // WYNIK: HashSet.add(null) → true

        note("HashSet i HashMap pozwalają na JEDEN null (jako element / jako klucz) — to wyjątek, nie reguła.");
        // WYNIK: ℹ HashSet i HashMap pozwalają na JEDEN null (jako element / jako klucz) — to wyjątek, nie reguła.

        Set<String> treeSetNull = new TreeSet<>();
        expectThrows("TreeSet.add(null)", () -> treeSetNull.add(null));
        // WYNIK: ✔ TreeSet.add(null) → rzucono NullPointerException: Cannot invoke "java.lang.Comparable.compareTo(Object)" because "k1" is null

        Queue<String> dequeNull = new ArrayDeque<>();
        expectThrows("ArrayDeque.add(null)", () -> dequeNull.add(null));
        // WYNIK: ✔ ArrayDeque.add(null) → rzucono NullPointerException: (brak komunikatu)

        // PUŁAPKA: TreeSet i TreeMap muszą PORÓWNAĆ nowy element z istniejącymi (żeby wiedzieć, gdzie go
        //   wstawić posortowanego) — porównanie z null nie ma sensu, więc rzucają NullPointerException.
        //   ArrayDeque też odrzuca null (używa null wewnętrznie jako znacznika „brak elementu” w metodach
        //   typu poll/peek — null jako prawdziwy element zepsułby to rozróżnienie).
    }

    // =================================================================================================
    // 6. NIEMODYFIKOWALNE FABRYKI: List.of / Set.of / Map.of (Java 9+)
    // =================================================================================================

    /**
     * 6. List.of/Set.of/Map.of tworzą NIEMODYFIKOWALNE kolekcje o stałej treści: żadnych zmian, żadnego null.
     * (Java 9+)
     */
    static void immutableFactories() {
        section("6. List.of / Set.of / Map.of — niemodyfikowalne fabryki (Java 9+)");

        List<String> immutableList = List.of("Ala", "Bob", "Cela");
        show("List.of(...)", immutableList);
        // WYNIK: List.of(...) → [Ala, Bob, Cela]
        note("List.of ZACHOWUJE kolejność podania — w przeciwieństwie do Set.of/Map.of, bezpiecznie ją wypisywać.");
        // WYNIK: ℹ List.of ZACHOWUJE kolejność podania — w przeciwieństwie do Set.of/Map.of, bezpiecznie ją wypisywać.

        expectThrows("List.of(...).add(\"X\")", () -> immutableList.add("X"));
        // WYNIK: ✔ List.of(...).add("X") → rzucono UnsupportedOperationException: (brak komunikatu)

        expectThrows("List.of(\"A\", null)", () -> List.of("A", null));
        // WYNIK: ✔ List.of("A", null) → rzucono NullPointerException: (brak komunikatu)

        Set<String> immutableSet = Set.of("Ala", "Bob", "Cela");
        show("Set.of(...).size()", immutableSet.size());
        // WYNIK: Set.of(...).size() → 3
        show("Set.of(...).contains(\"Bob\")", immutableSet.contains("Bob"));
        // WYNIK: Set.of(...).contains("Bob") → true

        Map<String, Integer> immutableMap = Map.of("a", 1, "b", 2);
        show("Map.of(...).size()", immutableMap.size());
        // WYNIK: Map.of(...).size() → 2
        show("Map.of(...).get(\"a\")", immutableMap.get("a"));
        // WYNIK: Map.of(...).get("a") → 1

        // PUŁAPKA: NIGDY nie wypisuj Set.of(...) ani Map.of(...) wprost (System.out.println/show). Ich
        //   kolejność iteracji jest NIEOKREŚLONA — implementacja JDK celowo miesza kolejność losowym
        //   „solą” (inaczej przy każdym uruchomieniu JVM), żeby nikt nie polegał na przypadkowej kolejności.
        //   Wypisuj rozmiar/contains/get, albo skopiuj do TreeSet/TreeMap, jeśli kolejność jest Ci potrzebna.
    }

    // =================================================================================================
    // 7. PRZEWODNIK: KTÓRĄ KOLEKCJĘ WYBRAĆ?
    // =================================================================================================

    /** Jedna potrzeba (need) i jedna rekomendacja (recommendation). */
    private record Guide(String need, String recommendation) {
    }

    private static final List<Guide> GUIDE = List.of(
            new Guide("kolejność wstawiania, duplikaty OK", "ArrayList"),
            new Guide("częste wstawianie/usuwanie na początku i końcu", "ArrayDeque (nie LinkedList)"),
            new Guide("unikalność, kolejność nieważna", "HashSet"),
            new Guide("unikalność + kolejność wstawiania", "LinkedHashSet"),
            new Guide("unikalność + zawsze posortowane", "TreeSet"),
            new Guide("klucz → wartość, szybki odczyt", "HashMap"),
            new Guide("klucz → wartość + kolejność wstawiania", "LinkedHashMap"),
            new Guide("klucz → wartość + zawsze posortowane wg klucza", "TreeMap"),
            new Guide("kolejka FIFO albo stos LIFO", "ArrayDeque"),
            new Guide("zawsze potrzebny najmniejszy/największy element", "PriorityQueue")
    );

    /** 7. Przewodnik decyzyjny — najpierw pytanie „czego potrzebuję?”, potem odpowiedź z tabeli. */
    static void decisionGuide() {
        section("7. Przewodnik: którą kolekcję wybrać?");

        for (Guide g : GUIDE) {
            System.out.println("  " + g.need() + " → " + g.recommendation());
        }
        // WYNIK:   kolejność wstawiania, duplikaty OK → ArrayList
        // WYNIK:   częste wstawianie/usuwanie na początku i końcu → ArrayDeque (nie LinkedList)
        // WYNIK:   unikalność, kolejność nieważna → HashSet
        // WYNIK:   unikalność + kolejność wstawiania → LinkedHashSet
        // WYNIK:   unikalność + zawsze posortowane → TreeSet
        // WYNIK:   klucz → wartość, szybki odczyt → HashMap
        // WYNIK:   klucz → wartość + kolejność wstawiania → LinkedHashMap
        // WYNIK:   klucz → wartość + zawsze posortowane wg klucza → TreeMap
        // WYNIK:   kolejka FIFO albo stos LIFO → ArrayDeque
        // WYNIK:   zawsze potrzebny najmniejszy/największy element → PriorityQueue

        // DOBRA PRAKTYKA: „ArrayDeque zamiast LinkedList” pojawia się dwukrotnie — to nieprzypadkowe.
        //   Collections02Lists i Collections06QueuesDeques tłumaczą, dlaczego ArrayDeque prawie zawsze
        //   wygrywa z LinkedList jako kolejka/stos.
    }

    // =================================================================================================
    // 8. CO DALEJ
    // =================================================================================================

    /** 8. Krótka zapowiedź kolejnych lekcji — każdy temat z tabeli ma swoją, głębszą lekcję. */
    static void whatsNext() {
        section("8. Co dalej");

        note("Listy (ArrayList/LinkedList) → Collections02Lists.");
        // WYNIK: ℹ Listy (ArrayList/LinkedList) → Collections02Lists.
        note("Bezpieczna iteracja i modyfikacja → Collections03IterationModification.");
        // WYNIK: ℹ Bezpieczna iteracja i modyfikacja → Collections03IterationModification.
        note("Zbiory (Set) → Collections04Sets. Mapy → Collections05Maps.");
        // WYNIK: ℹ Zbiory (Set) → Collections04Sets. Mapy → Collections05Maps.
        note("Kolejki i stosy → Collections06QueuesDeques. Sortowanie własnymi regułami → Collections07ComparableComparator.");
        // WYNIK: ℹ Kolejki i stosy → Collections06QueuesDeques. Sortowanie własnymi regułami → Collections07ComparableComparator.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Iterable → Collection → List/Set/Queue(Deque). Map jest OSOBNO (nie Collection, nie Iterable).
     *   • List: kolejność wstawiania, duplikaty OK. Set: bez duplikatów. Queue/Deque: kolejka FIFO/LIFO.
     *   • Map: klucz → wartość, klucze unikalne (jak Set), wartości mogą się powtarzać (jak List).
     *   • HashSet/HashMap: kolejność nieokreślona. LinkedHashSet/LinkedHashMap: kolejność wstawiania.
     *     TreeSet/TreeMap: zawsze posortowane (potrzebują Comparable albo Comparatora).
     *   • ArrayDeque prawie zawsze lepszy niż LinkedList jako kolejka/stos (Collections02, Collections06).
     *   • TreeSet/TreeMap/ArrayDeque nie akceptują null. HashSet/HashMap — tylko JEDEN null.
     *   • List.of/Set.of/Map.of: niemodyfikowalne, odrzucają null. NIGDY nie wypisuj Set.of/Map.of wprost —
     *     kolejność iteracji jest nieokreślona.
     *   • „Programuj do interfejsu”: `List<String> x = new ArrayList<>();`, nie `ArrayList<String> x = ...`.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego Map nie jest Collection, mimo że też przechowuje wiele elementów?
     *   2. Co wypisze:  System.out.println(new ArrayList<Integer>().equals(new LinkedList<Integer>()));  ?
     *   3. ZNAJDŹ BŁĄD:
     *          Set<String> s = new TreeSet<>();
     *          s.add(null);
     *   4. Co wypisze:  System.out.println(List.of(1, 2, 3).equals(new ArrayList<>(List.of(1, 2, 3))));  ?
     *   5. Dlaczego nie należy nigdy wypisywać Set.of(...) ani Map.of(...) wprost, jeśli zależy nam
     *      na powtarzalności wyniku między uruchomieniami?
     *   6. Jaką kolekcję wybierzesz do: (a) unikalnych nazw w kolejności dodania, (b) kolejki FIFO zadań,
     *      (c) mapy klucz → wartość zawsze posortowanej wg klucza?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: nazwy niedostępnych produktów", List.of("Smartfon X", "Oliwa z oliwek"),
                () -> outOfStockNames(SampleData.products()));
        Check.equal("ćw. 2: pierwsze 3 SKU (niemodyfikowalne)", List.of("ELE-001", "ELE-002", "ELE-003"),
                () -> firstSkus(SampleData.products(), 3));
        Check.equal("ćw. 3a: potrzeba → TreeSet", "TreeSet", () -> suggestCollection("unikalne, posortowane"));
        Check.equal("ćw. 3b: potrzeba → LinkedHashSet", "LinkedHashSet",
                () -> suggestCollection("unikalne, kolejność wstawiania"));
        Check.equal("ćw. 3c: potrzeba → ArrayDeque", "ArrayDeque", () -> suggestCollection("fifo"));
        Check.equal("ćw. 3d: potrzeba → HashMap", "HashMap", () -> suggestCollection("klucz-wartość, szybki dostęp"));
        Check.equal("ćw. 4: unikalne kategorie (PRZEPISZ na LinkedHashSet)",
                List.of("Elektronika", "Spożywcze", "Książki", "Odzież", "Dom i ogród"),
                () -> uniqueCategoryNames(SampleData.products()));
        Check.equal("ćw. 5a: dodanie do listy modyfikowalnej", true,
                () -> tryAdd(new ArrayList<>(List.of("a")), "b"));
        Check.equal("ćw. 5b: dodanie do List.of (niemodyfikowalnej)", false,
                () -> tryAdd(List.of("a", "b"), "c"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("Smartfon X", "Oliwa z oliwek"),
                () -> solutionOutOfStockNames(SampleData.products()));
        Check.equal("ćw. 2 (wzorzec)", List.of("ELE-001", "ELE-002", "ELE-003"),
                () -> solutionFirstSkus(SampleData.products(), 3));
        Check.equal("ćw. 3a (wzorzec)", "TreeSet", () -> solutionSuggestCollection("unikalne, posortowane"));
        Check.equal("ćw. 3b (wzorzec)", "LinkedHashSet",
                () -> solutionSuggestCollection("unikalne, kolejność wstawiania"));
        Check.equal("ćw. 3c (wzorzec)", "ArrayDeque", () -> solutionSuggestCollection("fifo"));
        Check.equal("ćw. 3d (wzorzec)", "HashMap", () -> solutionSuggestCollection("klucz-wartość, szybki dostęp"));
        Check.equal("ćw. 4 (wzorzec)", List.of("Elektronika", "Spożywcze", "Książki", "Odzież", "Dom i ogród"),
                () -> solutionUniqueCategoryNames(SampleData.products()));
        Check.equal("ćw. 5a (wzorzec)", true, () -> solutionTryAdd(new ArrayList<>(List.of("a")), "b"));
        Check.equal("ćw. 5b (wzorzec)", false, () -> solutionTryAdd(List.of("a", "b"), "c"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 9 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć nazwy produktów, których NIE MA w magazynie (stock == 0), w kolejności
     * z listy wejściowej. Podpowiedź: Product.inStock() zwraca stock > 0 — potrzebujesz przeciwieństwa.
     */
    static List<String> outOfStockNames(List<Product> products) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć NIEMODYFIKOWALNĄ listę pierwszych n numerów SKU (w kolejności z listy
     * wejściowej). Podpowiedź: List.subList(0, n) + List.copyOf (subList to widok — sekcja Collections02).
     */
    static List<String> firstSkus(List<Product> products, int n) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): na podstawie opisu potrzeby (need) zwróć nazwę polecanej kolekcji — zgodnie
     * z przewodnikiem z sekcji 7. Podpowiedź: switch na String (case-sensitive!) z czterema przypadkami:
     * "unikalne, posortowane", "unikalne, kolejność wstawiania", "fifo", "klucz-wartość, szybki dostęp".
     */
    static String suggestCollection(String need) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 4 (trudniejsze, PRZEPISZ): poniższy kod działa, ale sprawdza {@code result.contains(name)}
     * w LIŚCIE przy każdym elemencie — to O(n) na sprawdzenie, więc O(n²) łącznie dla całej pętli:
     * <pre>{@code
     * List<String> result = new ArrayList<>();
     * for (Product p : products) {
     *     String name = p.category().getDisplayName();
     *     if (!result.contains(name)) {
     *         result.add(name);
     *     }
     * }
     * return result;
     * }</pre>
     * Przepisz to używając LinkedHashSet (sprawdzenie duplikatu w O(1)) — Set sam odrzuci duplikat, Ty
     * tylko dodajesz wszystkie nazwy kategorii po kolei. Zachowa to kolejność PIERWSZEGO wystąpienia.
     * Na końcu zwróć List (np. List.copyOf(zbior)). Podpowiedź: Category ma metodę getDisplayName().
     */
    static List<String> uniqueCategoryNames(List<Product> products) {
        // TODO: twoje rozwiązanie (LinkedHashSet zamiast result.contains(...))
        return List.of();
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): spróbuj dodać value do list. Jeśli lista jest niemodyfikowalna i add()
     * rzuci UnsupportedOperationException — złap wyjątek i zwróć false. Jeśli dodanie się uda — zwróć true.
     * Podpowiedź: try { list.add(value); return true; } catch (UnsupportedOperationException e) { ... }
     */
    static boolean tryAdd(List<String> list, String value) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solutionOutOfStockNames(List<Product> products) {
        List<String> result = new ArrayList<>();
        for (Product p : products) {
            if (!p.inStock()) {
                result.add(p.name());
            }
        }
        return result;
    }

    static List<String> solutionFirstSkus(List<Product> products, int n) {
        List<String> skus = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            skus.add(products.get(i).sku());
        }
        return List.copyOf(skus);
    }

    static String solutionSuggestCollection(String need) {
        return switch (need) {
            case "unikalne, posortowane" -> "TreeSet";
            case "unikalne, kolejność wstawiania" -> "LinkedHashSet";
            case "fifo" -> "ArrayDeque";
            case "klucz-wartość, szybki dostęp" -> "HashMap";
            default -> throw new IllegalArgumentException("Nieznana potrzeba: " + need);
        };
    }

    static List<String> solutionUniqueCategoryNames(List<Product> products) {
        Set<String> unique = new LinkedHashSet<>();
        for (Product p : products) {
            unique.add(p.category().getDisplayName());
        }
        return List.copyOf(unique);
    }

    static boolean solutionTryAdd(List<String> list, String value) {
        try {
            list.add(value);
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Map przechowuje PARY klucz-wartość, a nie pojedyncze elementy — projektanci JDK uznali, że
     *      iterowanie „po parach” to inny kontrakt niż iterowanie „po elementach”, więc Map dostał własną
     *      hierarchię (ale nadal ma entrySet()/keySet()/values(), które SĄ kolekcjami).
     *   2. „true” — obie listy są puste, a List.equals porównuje zawartość, nie klasę implementacji.
     *   3. TreeSet (i TreeMap) muszą porównać nowy element z istniejącymi, żeby wiedzieć, gdzie go
     *      wstawić posortowanego — porównanie z null nie ma sensu, więc s.add(null) rzuca
     *      NullPointerException.
     *   4. „true” — List.of tworzy List, a List.equals porównuje elementy w tej samej kolejności,
     *      niezależnie od tego, że jedna lista jest niemodyfikowalna, a druga zwykłym ArrayList.
     *   5. Bo kolejność iteracji Set.of/Map.of jest NIEOKREŚLONA i może się różnić między uruchomieniami
     *      JVM (celowe losowe mieszanie) — test albo log oparty na tej kolejności byłby niestabilny.
     *   6. (a) LinkedHashSet, (b) ArrayDeque, (c) TreeMap.
     */
    // </editor-fold>
}
