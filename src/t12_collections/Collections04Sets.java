package t12_collections;

import helpers.Check;
import helpers.SampleData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Set — HashSet, LinkedHashSet, TreeSet i algebra zbiorów
 *        (set = zbiór; navigable = z nawigacją, tu: „umie znaleźć sąsiadów”; algebra of sets = algebra zbiorów)
 *
 * W SKRÓCIE:
 *   Set to kolekcja BEZ DUPLIKATÓW. Trzy główne implementacje różnią się kolejnością: HashSet — nieokreślona
 *   (najszybszy), LinkedHashSet — kolejność wstawiania, TreeSet — zawsze posortowana i umie nawigować
 *   (najbliższy większy/mniejszy element). „Duplikat” w Secie oznacza dwa obiekty RÓWNE wg equals/hashCode.
 *
 * ANALOGIA: trzy rodzaje szatni.
 *   HashSet to szatnia bez numerków — rzeczy trafiają na dowolną wolną półkę wg jakiegoś wzoru, nie pytaj,
 *   na którą. LinkedHashSet to szatnia, gdzie półki są dodatkowo POWIĄZANE łańcuszkiem w kolejności, w jakiej
 *   ludzie przyszli — zawsze wiesz, kto był pierwszy. TreeSet to szatnia z półkami POSORTOWANYMI np.
 *   alfabetycznie po nazwisku — łatwo znaleźć „najbliższą wolną” półkę w dowolnym miejscu alfabetu.
 *
 * JAK TO DZIAŁA:
 *   add(x) w każdym Secie sprawdza: „czy jakiś istniejący element jest RÓWNY x (equals)?” — jeśli tak,
 *   add() zwraca false i NIC się nie dzieje. HashSet/LinkedHashSet używają hashCode() do szybkiego
 *   znalezienia „kubełka” z potencjalnie równymi elementami (Collections11HashingInternals — szczegóły).
 *   TreeSet używa compareTo/Comparator zamiast equals/hashCode do porównań (Collections07).
 *
 * SŁÓWKA:
 *   duplicate = duplikat; insertion order = kolejność wstawiania; sorted = posortowany; navigable = z
 *   nawigacją; union = suma (zbiorów); intersection = przecięcie; difference = różnica; floor = podłoga
 *   (największy ≤); ceiling = sufit (najmniejszy ≥); identity = tożsamość (ten sam obiekt, ==).
 *
 * ZOBACZ TEŻ: t12_collections/Collections01Overview (tabela implementacji), t12_collections/Collections05Maps
 *             (Map też polega na equals/hashCode kluczy), t12_collections/Collections07ComparableComparator
 *             (Comparable/Comparator dla TreeSet), t12_collections/Collections10Patterns (dedup, więcej
 *             przepisów), t12_collections/Collections11HashingInternals (jak HashSet działa w środku),
 *             t09_records/Records01Basics (equals/hashCode w rekordach).
 * </pre>
 */
public class Collections04Sets {

    public static void main(String[] args) {
        title("Collections04 — Set: HashSet, LinkedHashSet, TreeSet, algebra zbiorów");

        hashSetBasics();               // HashSet: podstawy
        linkedHashSetOrder();          // LinkedHashSet: kolejność wstawiania
        treeSetNavigable();            // TreeSet: sortowanie i nawigacja
        setAlgebra();                  // algebra zbiorów
        equalsHashCodeRequirement();   // wymóg equals/hashCode
        containsPerformance();         // wydajność contains
        listSetConversion();           // konwersje List <-> Set
        whichSet();                    // który Set wybrać
        exercises();                   // ćwiczenia
    }

    // =================================================================================================
    // 1. HashSet — PODSTAWY
    // =================================================================================================

    /**
     * 1. add() zwraca false, gdy element (wg equals) już jest w zbiorze. Kolejność iteracji HashSet NIE
     * jest częścią kontraktu Set — nigdy jej nie wypisujemy wprost.
     */
    static void hashSetBasics() {
        section("1. HashSet: add, duplikaty, rozmiar");

        Set<String> tags = new HashSet<>();   // tags = tagi/etykiety
        boolean firstAdd = tags.add("java");
        boolean secondAdd = tags.add("java");   // duplikat — ten sam String (wg equals)
        tags.add("stream");
        tags.add("lambda");

        show("dodanie \"java\" po raz pierwszy", firstAdd);
        // WYNIK: dodanie "java" po raz pierwszy → true
        show("dodanie \"java\" po raz drugi (duplikat)", secondAdd);
        // WYNIK: dodanie "java" po raz drugi (duplikat) → false
        show("rozmiar zbioru", tags.size());
        // WYNIK: rozmiar zbioru → 3
        show("contains(\"stream\")", tags.contains("stream"));
        // WYNIK: contains("stream") → true

        // PUŁAPKA: nigdy nie wypisuj HashSet wprost (println/show(label, hashSet)) — kolejność iteracji NIE
        //   jest częścią kontraktu Set (może zależeć od hashCode elementów, liczby kubełków, wersji JDK) i
        //   nie wolno na niej polegać. Wypisuj size()/contains(...), albo skopiuj do TreeSet (sekcja 3),
        //   jeśli potrzebujesz przewidywalnej kolejności.
    }

    // =================================================================================================
    // 2. LinkedHashSet — KOLEJNOŚĆ WSTAWIANIA
    // =================================================================================================

    /** 2. LinkedHashSet dodatkowo pamięta kolejność wstawiania (łańcuch powiązanych węzłów) — bezpiecznie ją wypisywać. */
    static void linkedHashSetOrder() {
        section("2. LinkedHashSet: kolejność wstawiania");

        Set<String> visitOrder = new LinkedHashSet<>();   // visit order = kolejność odwiedzin
        visitOrder.add("Warszawa");
        visitOrder.add("Kraków");
        visitOrder.add("Warszawa");   // duplikat — ignorowany, NIE przesuwa elementu na koniec
        visitOrder.add("Gdańsk");

        show("kolejność odwiedzin (LinkedHashSet)", visitOrder);
        // WYNIK: kolejność odwiedzin (LinkedHashSet) → [Warszawa, Kraków, Gdańsk]

        // DOBRA PRAKTYKA: LinkedHashSet = „chcę unikalność ORAZ przewidywalną kolejność wypisywania, bez
        //   sortowania”. Klasyczne zastosowanie: usuwanie duplikatów z zachowaniem kolejności pierwszego
        //   wystąpienia (sekcja 7, więcej przepisów: Collections10Patterns).
    }

    // =================================================================================================
    // 3. TreeSet — SORTOWANIE I NAWIGACJA (NavigableSet)
    // =================================================================================================

    /**
     * 3. TreeSet zawsze iteruje posortowany. NavigableSet dodaje metody „nawigacyjne”: first/last,
     * floor/ceiling (włącznie), lower/higher (ściśle), headSet/tailSet (widoki na fragment).
     */
    static void treeSetNavigable() {
        section("3. TreeSet: first, last, floor, ceiling, headSet, tailSet");

        NavigableSet<Integer> scores = new TreeSet<>(List.of(42, 17, 99, 3, 56, 71));   // scores = wyniki
        show("posortowany zbiór", scores);
        // WYNIK: posortowany zbiór → [3, 17, 42, 56, 71, 99]

        show("first() (najmniejszy)", scores.first());
        // WYNIK: first() (najmniejszy) → 3
        show("last() (największy)", scores.last());
        // WYNIK: last() (największy) → 99
        show("floor(50) (największy ≤ 50)", scores.floor(50));
        // WYNIK: floor(50) (największy ≤ 50) → 42
        show("ceiling(50) (najmniejszy ≥ 50)", scores.ceiling(50));
        // WYNIK: ceiling(50) (najmniejszy ≥ 50) → 56
        show("lower(42) (największy < 42, ściśle)", scores.lower(42));
        // WYNIK: lower(42) (największy < 42, ściśle) → 17
        show("higher(42) (najmniejszy > 42, ściśle)", scores.higher(42));
        // WYNIK: higher(42) (najmniejszy > 42, ściśle) → 56
        show("headSet(50) (wszystko < 50)", scores.headSet(50));
        // WYNIK: headSet(50) (wszystko < 50) → [3, 17, 42]
        show("tailSet(50) (wszystko ≥ 50)", scores.tailSet(50));
        // WYNIK: tailSet(50) (wszystko ≥ 50) → [56, 71, 99]

        // DOBRA PRAKTYKA: floor/ceiling/lower/higher zwracają null, gdy nie ma pasującego elementu (np.
        //   scores.lower(3) — nic nie jest mniejsze od najmniejszego) — sprawdzaj null przed użyciem.
    }

    // =================================================================================================
    // 4. ALGEBRA ZBIORÓW: suma, przecięcie, różnica
    // =================================================================================================

    /**
     * 4. addAll = suma (union), retainAll = przecięcie (intersection), removeAll = różnica (difference).
     * Wszystkie trzy MODYFIKUJĄ zbiór, na którym je wywołujesz — dlatego operujemy na KOPIACH.
     */
    static void setAlgebra() {
        section("4. Algebra zbiorów: suma, przecięcie, różnica");

        Set<String> group1 = new TreeSet<>(List.of("Ala", "Bartek", "Celina", "Darek"));
        Set<String> group2 = new TreeSet<>(List.of("Bartek", "Celina", "Ewa"));

        Set<String> union = new TreeSet<>(group1);   // KOPIA group1 — nie psujemy oryginału
        union.addAll(group2);
        show("suma (union)", union);
        // WYNIK: suma (union) → [Ala, Bartek, Celina, Darek, Ewa]

        Set<String> intersection = new TreeSet<>(group1);
        intersection.retainAll(group2);   // retainAll = zostaw tylko elementy obecne TAKŻE w group2
        show("przecięcie (intersection)", intersection);
        // WYNIK: przecięcie (intersection) → [Bartek, Celina]

        Set<String> difference = new TreeSet<>(group1);
        difference.removeAll(group2);   // removeAll = usuń elementy obecne w group2
        show("różnica group1 minus group2 (difference)", difference);
        // WYNIK: różnica group1 minus group2 (difference) → [Ala, Darek]

        show("group1 bez zmian?", group1);
        // WYNIK: group1 bez zmian? → [Ala, Bartek, Celina, Darek]

        // DOBRA PRAKTYKA: addAll/retainAll/removeAll modyfikują odbiorcę (this) w miejscu i zwracają
        //   boolean (czy coś się zmieniło), NIE nowy zbiór. Zawsze rób kopię (new TreeSet<>(oryginał)),
        //   jeśli chcesz zachować oryginalne zbiory nietknięte.
    }

    // =================================================================================================
    // 5. WYMÓG: equals/hashCode — record kontra zwykła klasa
    // =================================================================================================

    /** Punkt jako rekord — equals/hashCode WYGENEROWANE automatycznie na podstawie x i y. */
    private record PointR(int x, int y) {
    }

    /** Punkt jako zwykła klasa BEZ equals/hashCode — dziedziczy je z Object (porównanie po tożsamości, ==). */
    private static final class PointC {
        final int x;
        final int y;

        PointC(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * 5. Set (i klucze Map) rozpoznają duplikat WYŁĄCZNIE przez equals()+hashCode(). record generuje obie
     * metody automatycznie z pól. Klasa bez nadpisanych equals/hashCode dziedziczy je z Object — dwa
     * obiekty o identycznych polach są dla Seta DWOMA różnymi elementami.
     */
    static void equalsHashCodeRequirement() {
        section("5. equals/hashCode: record kontra zwykła klasa");

        Set<PointR> recordPoints = new HashSet<>();
        recordPoints.add(new PointR(1, 2));
        recordPoints.add(new PointR(1, 2));   // te same x,y — record: RÓWNE (duplikat)
        show("HashSet<record>.size() (dwa identyczne punkty)", recordPoints.size());
        // WYNIK: HashSet<record>.size() (dwa identyczne punkty) → 1

        Set<PointC> classPoints = new HashSet<>();
        classPoints.add(new PointC(1, 2));
        classPoints.add(new PointC(1, 2));   // dwa RÓŻNE obiekty — klasa bez equals: NIERÓWNE
        show("HashSet<klasa bez equals>.size() (dwa identyczne punkty)", classPoints.size());
        // WYNIK: HashSet<klasa bez equals>.size() (dwa identyczne punkty) → 2

        // PUŁAPKA: brak equals/hashCode w klasie używanej jako element Seta (albo klucz Map) to cichy błąd —
        //   kod się kompiluje, testy na małych przykładach mogą przypadkiem „wyjść”, a w produkcji zbiór
        //   pęcznieje „duplikatami”, które programista uważał za jeden element. record eliminuje ten błąd
        //   z definicji (t09_records/Records01Basics).
    }

    // =================================================================================================
    // 6. WYDAJNOŚĆ: contains w Secie
    // =================================================================================================

    /** 6. HashSet.add/remove/contains to (średnio) O(1) — stała liczba kroków niezależnie od rozmiaru zbioru. */
    static void containsPerformance() {
        section("6. Wydajność: contains w HashSet");

        Set<Integer> big = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            big.add(i);
        }
        show("rozmiar po dodaniu 1000 unikalnych liczb", big.size());
        // WYNIK: rozmiar po dodaniu 1000 unikalnych liczb → 1000
        show("contains(500)", big.contains(500));
        // WYNIK: contains(500) → true
        show("contains(-1) (nie ma)", big.contains(-1));
        // WYNIK: contains(-1) (nie ma) → false

        note("HashSet.contains to średnio O(1) (stała liczba kroków), podczas gdy List.contains przeszukuje "
                + "liniowo — O(n). Policzone porównanie (bez pomiaru czasu!): Collections13Performance.");
        // WYNIK: ℹ HashSet.contains to średnio O(1) (stała liczba kroków), podczas gdy List.contains przeszukuje liniowo — O(n). Policzone porównanie (bez pomiaru czasu!): Collections13Performance.
    }

    // =================================================================================================
    // 7. KONWERSJE: List ↔ Set (deduplikacja)
    // =================================================================================================

    /** 7. Konstruktor Set(Collection) usuwa duplikaty. LinkedHashSet zachowuje przy tym kolejność pierwszego wystąpienia. */
    static void listSetConversion() {
        section("7. Konwersje List ↔ Set: deduplikacja z zachowaniem kolejności");

        List<String> withDuplicates = SampleData.words();
        show("słowa z duplikatami", withDuplicates);
        // WYNIK: słowa z duplikatami → [java, stream, lambda, kolekcja, java, mapa, lista, stream, java, optional, rekord, enum]

        List<String> unique = new ArrayList<>(new LinkedHashSet<>(withDuplicates));
        show("unikalne (kolejność pierwszego wystąpienia)", unique);
        // WYNIK: unikalne (kolejność pierwszego wystąpienia) → [java, stream, lambda, kolekcja, mapa, lista, optional, rekord, enum]

        // DOBRA PRAKTYKA: new LinkedHashSet<>(lista) + new ArrayList<>(zbior) to najkrótszy sposób na
        //   „usuń duplikaty, zachowaj kolejność”. Więcej przepisów na deduplikację: Collections10Patterns.
    }

    // =================================================================================================
    // 8. PODSUMOWANIE: KTÓRY Set WYBRAĆ?
    // =================================================================================================

    private record SetChoice(String need, String recommendation) {
    }

    private static final List<SetChoice> SET_GUIDE = List.of(
            new SetChoice("szybka unikalność, kolejność nieważna", "HashSet"),
            new SetChoice("unikalność + kolejność wstawiania", "LinkedHashSet"),
            new SetChoice("unikalność + zawsze posortowane / nawigacja (floor, ceiling...)", "TreeSet")
    );

    /** 8. Krótki przewodnik decyzyjny — podsumowanie całej lekcji. */
    static void whichSet() {
        section("8. Podsumowanie: który Set wybrać?");

        for (SetChoice c : SET_GUIDE) {
            System.out.println("  " + c.need() + " → " + c.recommendation());
        }
        // WYNIK:   szybka unikalność, kolejność nieważna → HashSet
        // WYNIK:   unikalność + kolejność wstawiania → LinkedHashSet
        // WYNIK:   unikalność + zawsze posortowane / nawigacja (floor, ceiling...) → TreeSet
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Set = kolekcja bez duplikatów. „Duplikat” = element RÓWNY wg equals()/hashCode().
     *   • HashSet: najszybszy, kolejność NIEOKREŚLONA — nigdy nie wypisuj wprost.
     *   • LinkedHashSet: kolejność wstawiania — bezpiecznie wypisywać, dobre do deduplikacji.
     *   • TreeSet (NavigableSet): zawsze posortowany; first/last, floor/ceiling (włącznie),
     *     lower/higher (ściśle), headSet/tailSet (widoki na fragment).
     *   • union = addAll, intersection = retainAll, difference = removeAll — na KOPIACH, bo modyfikują
     *     odbiorcę w miejscu.
     *   • Element Seta (i klucz Map) MUSI mieć poprawne equals/hashCode. record ma je za darmo; zwykła
     *     klasa bez nich porównuje po tożsamości (==) — cichy błąd „duplikatów” w zbiorze.
     *   • HashSet.contains to średnio O(1); List.contains to O(n) — dla dużych zbiorów różnica jest ogromna.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego nigdy nie należy wypisywać HashSet wprost, jeśli zależy nam na powtarzalności wyniku?
     *   2. Co wypisze:
     *          TreeSet<Integer> t = new TreeSet<>(List.of(10, 5, 20, 15));
     *          System.out.println(t.floor(12) + " " + t.ceiling(12));
     *      ?
     *   3. ZNAJDŹ BŁĄD (programista oczekuje 1):
     *          class Point { int x, y; Point(int x, int y) { this.x = x; this.y = y; } }
     *          Set<Point> set = new HashSet<>();
     *          set.add(new Point(1, 1));
     *          set.add(new Point(1, 1));
     *          System.out.println(set.size());
     *   4. Co wypisze:
     *          Set<String> a = new LinkedHashSet<>(List.of("x", "y", "x", "z"));
     *          System.out.println(a);
     *      ?
     *   5. Jak wyliczyć różnicę (A minus B) dwóch zbiorów, nie psując żadnego z oryginałów?
     *   6. Dlaczego record jest wygodnym wyborem jako element Seta albo klucz Mapy, a zwykła klasa —
     *      tylko jeśli sama nadpisze equals/hashCode?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: unikalne słowa (kolejność pierwszego wystąpienia)",
                List.of("java", "stream", "lambda", "kolekcja", "mapa", "lista", "optional", "rekord", "enum"),
                () -> distinctPreserveOrder(SampleData.words()));
        Check.equal("ćw. 2: wspólne SKU (przecięcie)", Set.of("ELE-002", "KSI-001"),
                () -> commonSkus(Set.of("ELE-001", "ELE-002", "KSI-001"), Set.of("ELE-002", "KSI-001", "DOM-001")));
        Check.equal("ćw. 3: liczba różnych punktów", 3,
                () -> countDistinct(List.of(new PointR(1, 1), new PointR(2, 2), new PointR(1, 1),
                        new PointR(3, 3), new PointR(2, 2))));
        Check.equal("ćw. 4a: ma duplikat (PRZEPISZ na HashSet)", false, () -> hasDuplicate(List.of(1, 2, 3, 4)));
        Check.equal("ćw. 4b: ma duplikat", true, () -> hasDuplicate(List.of(1, 2, 3, 2)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)",
                List.of("java", "stream", "lambda", "kolekcja", "mapa", "lista", "optional", "rekord", "enum"),
                () -> solutionDistinctPreserveOrder(SampleData.words()));
        Check.equal("ćw. 2 (wzorzec)", Set.of("ELE-002", "KSI-001"), () -> solutionCommonSkus(
                Set.of("ELE-001", "ELE-002", "KSI-001"), Set.of("ELE-002", "KSI-001", "DOM-001")));
        Check.equal("ćw. 3 (wzorzec)", 3, () -> solutionCountDistinct(List.of(new PointR(1, 1), new PointR(2, 2),
                new PointR(1, 1), new PointR(3, 3), new PointR(2, 2))));
        Check.equal("ćw. 4a (wzorzec)", false, () -> solutionHasDuplicate(List.of(1, 2, 3, 4)));
        Check.equal("ćw. 4b (wzorzec)", true, () -> solutionHasDuplicate(List.of(1, 2, 3, 2)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć listę BEZ duplikatów, zachowując kolejność PIERWSZEGO wystąpienia.
     * Podpowiedź: {@code new ArrayList<>(new LinkedHashSet<>(items))} — sekcja 7.
     */
    static List<String> distinctPreserveOrder(List<String> items) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć PRZECIĘCIE dwóch zbiorów (elementy obecne w obu), nie modyfikując a ani b.
     * Podpowiedź: skopiuj a, wywołaj retainAll(b) na kopii — sekcja 4.
     */
    static Set<String> commonSkus(Set<String> a, Set<String> b) {
        // TODO: twoje rozwiązanie
        return Set.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): policz, ile RÓŻNYCH punktów (wg equals) jest na liście points.
     * Podpowiedź: dodaj wszystkie do {@code HashSet<PointR>} i zwróć jego rozmiar.
     */
    static int countDistinct(List<PointR> points) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze, PRZEPISZ): poniższy kod sprawdza duplikat w O(n²) — dwie zagnieżdżone pętle:
     * <pre>{@code
     * for (int i = 0; i < numbers.size(); i++) {
     *     for (int j = i + 1; j < numbers.size(); j++) {
     *         if (numbers.get(i).equals(numbers.get(j))) {
     *             return true;
     *         }
     *     }
     * }
     * return false;
     * }</pre>
     * Przepisz to na JEDNO przejście z HashSet: dla każdej liczby wywołaj {@code set.add(n)} — jeśli add()
     * zwróci false, liczba już tam była (duplikat) — zwróć true od razu. Jeśli pętla się skończy bez
     * duplikatu — zwróć false.
     */
    static boolean hasDuplicate(List<Integer> numbers) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solutionDistinctPreserveOrder(List<String> items) {
        return new ArrayList<>(new LinkedHashSet<>(items));
    }

    static Set<String> solutionCommonSkus(Set<String> a, Set<String> b) {
        Set<String> copy = new HashSet<>(a);
        copy.retainAll(b);
        return copy;
    }

    static int solutionCountDistinct(List<PointR> points) {
        return new HashSet<>(points).size();
    }

    static boolean solutionHasDuplicate(List<Integer> numbers) {
        Set<Integer> seen = new HashSet<>();   // seen = już widziane
        for (Integer n : numbers) {
            if (!seen.add(n)) {
                return true;
            }
        }
        return false;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Kolejność iteracji HashSet nie jest częścią kontraktu Set — zależy od hashCode elementów,
     *      liczby kubełków i wersji JDK, więc test/log oparty na niej byłby niestabilny.
     *   2. „10 15” — floor(12) to największy element ≤ 12 (czyli 10), ceiling(12) to najmniejszy ≥ 12
     *      (czyli 15).
     *   3. Klasa Point nie nadpisuje equals/hashCode, więc dziedziczy je z Object — porównanie po
     *      tożsamości (==). Dwa różne obiekty o tych samych polach to dla Seta dwa różne elementy:
     *      wypisze 2. Poprawka: nadpisać equals i hashCode (albo użyć record).
     *   4. „[x, y, z]” — LinkedHashSet zachowuje kolejność PIERWSZEGO wystąpienia; drugie "x" jest
     *      duplikatem i zostaje zignorowane, nie przesuwa elementu na koniec.
     *   5. Skopiować pierwszy zbiór (np. new TreeSet<>(a)) i wywołać removeAll(b) na kopii — oryginały a
     *      i b zostają nietknięte.
     *   6. record generuje equals/hashCode automatycznie na podstawie WSZYSTKICH pól — od razu poprawne
     *      porównanie „po wartości”. Zwykła klasa dziedziczy equals/hashCode z Object (porównanie po
     *      tożsamości), więc bez własnej implementacji dwa obiekty o tych samych polach są „różne”.
     */
    // </editor-fold>
}
