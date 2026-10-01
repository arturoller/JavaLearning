package t24_algorithms;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import helpers.Check;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Grafy — reprezentacje, BFS, DFS, sortowanie topologiczne, Dijkstra
 *        (graph = graf; vertex/node = wierzchołek; edge = krawędź; adjacency = sąsiedztwo)
 *
 * W SKRÓCIE:
 *   Graf to wierzchołki (np. miasta) połączone krawędziami (np. drogami). BFS przeszukuje "warstwami" i daje
 *   najkrótszą ścieżkę BEZ wag. DFS schodzi najpierw w głąb. Sortowanie topologiczne porządkuje zadania
 *   z zależnościami. Dijkstra liczy najkrótsze ścieżki, gdy krawędzie MAJĄ wagi (koszt, odległość, czas).
 *
 * ANALOGIA: mapa miast i dróg.
 *   BFS to rozchodzenie się plotki: dzień 1 — sąsiedzi, dzień 2 — sąsiedzi sąsiadów, itd. (najkrótsza liczba
 *   "skoków"). Dijkstra to ta sama plotka, ale drogi mają różną długość — liczy się czas dotarcia, nie liczba skoków.
 *
 * JAK TO DZIAŁA:
 *   Lista sąsiedztwa: {@code Map<Wierzchołek, List<Sąsiad>>} — pamięciowo O(V+E), szybkie przejście "kogo mam obok".
 *   BFS: kolejka (FIFO) + zbiór odwiedzonych — O(V+E). DFS: rekurencja albo własny stos — też O(V+E).
 *   Dijkstra: kolejka priorytetowa zamiast zwykłej — zawsze przetwarzamy NAJBLIŻSZY nieprzetworzony wierzchołek
 *   — O((V+E) log V), bo każda operacja na kolejce priorytetowej to O(log V).
 *
 * SŁÓWKA:
 *   vertex/node = wierzchołek; edge = krawędź; adjacency list/matrix = lista/macierz sąsiedztwa; weighted = ważony;
 *   directed = skierowany; path = ścieżka; connected component = spójna składowa; topological order = porządek topologiczny.
 *
 * ZOBACZ TEŻ: t24_algorithms/Algorithms04DataStructures (kopiec = PriorityQueue pod spodem),
 *             t12_collections/Collections06QueuesDeques (Queue, PriorityQueue w praktyce),
 *             t24_algorithms/Algorithms01Complexity (notacja O()).
 * </pre>
 */
public class Algorithms08Graphs {

    public static void main(String[] args) {
        title("Algorithms08 — grafy: BFS, DFS, sortowanie topologiczne, Dijkstra");

        representations();       // representations = reprezentacje grafu
        breadthFirstSearch();    // BFS = przeszukiwanie wszerz
        depthFirstRecursive();   // DFS recursive = przeszukiwanie w głąb, rekurencyjnie
        depthFirstIterative();   // DFS iterative = przeszukiwanie w głąb, ze stosem + cykle
        topologicalSortDemo();   // topological sort = sortowanie topologiczne
        dijkstraDemo();          // Dijkstra = najkrótsze ścieżki w grafie ważonym
        exercises();             // exercises = ćwiczenia
    }

    /** cityGraph = nieważony graf nieskierowany (sąsiedztwo dróg) używany w sekcjach 1-4. */
    static Map<String, List<String>> cityGraph() {
        Map<String, List<String>> graph = new LinkedHashMap<>();
        graph.put("Warszawa", List.of("Krakow", "Gdansk", "Wroclaw"));
        graph.put("Krakow", List.of("Warszawa", "Wroclaw"));
        graph.put("Gdansk", List.of("Warszawa", "Poznan"));
        graph.put("Wroclaw", List.of("Warszawa", "Krakow", "Poznan"));
        graph.put("Poznan", List.of("Gdansk", "Wroclaw"));
        return graph;
    }

    // =================================================================================================
    // 1. REPREZENTACJE GRAFU — LISTA SĄSIEDZTWA I MACIERZ SĄSIEDZTWA
    // =================================================================================================

    /** 1. Lista sąsiedztwa: pamięć O(V+E), dobra gdy krawędzi jest MAŁO. Macierz: O(V²), ale O(1) sprawdzenie "czy jest krawędź". */
    static void representations() {
        section("1. Reprezentacje grafu — lista sąsiedztwa i macierz sąsiedztwa");

        Map<String, List<String>> graph = cityGraph();
        showEach("lista sąsiedztwa", graph);
        // WYNIK: lista sąsiedztwa (liczba kluczy: 5):
        // WYNIK:    • Warszawa → [Krakow, Gdansk, Wroclaw]
        // WYNIK:    • Krakow → [Warszawa, Wroclaw]
        // WYNIK:    • Gdansk → [Warszawa, Poznan]
        // WYNIK:    • Wroclaw → [Warszawa, Krakow, Poznan]
        // WYNIK:    • Poznan → [Gdansk, Wroclaw]

        String[] cities = graph.keySet().toArray(new String[0]);
        boolean[][] matrix = new boolean[cities.length][cities.length];
        for (int i = 0; i < cities.length; i++) {
            for (String neighbor : graph.get(cities[i])) {
                int j = List.of(cities).indexOf(neighbor);
                matrix[i][j] = true;
            }
        }
        for (int i = 0; i < cities.length; i++) {
            System.out.println("   " + cities[i] + ": " + Arrays.toString(matrix[i]));
        }
        // WYNIK:    Warszawa: [false, true, true, true, false]
        // WYNIK:    Krakow: [true, false, false, true, false]
        // WYNIK:    Gdansk: [true, false, false, false, true]
        // WYNIK:    Wroclaw: [true, true, false, false, true]
        // WYNIK:    Poznan: [false, false, true, true, false]

        note("ta sama informacja, dwa zapisy — lista jest zwarta, macierz szybko odpowiada \"czy X-Y są połączone\"");
    }

    // =================================================================================================
    // 2. PRZESZUKIWANIE WSZERZ (BFS) — NAJKRÓTSZA ŚCIEŻKA BEZ WAG
    // =================================================================================================

    /** bfsShortestPath = kolejka FIFO: odwiedzaj "warstwami" — pierwsze dotarcie do węzła to ZAWSZE najkrótsza droga. */
    static List<String> bfsShortestPath(Map<String, List<String>> graph, String start, String target) {
        Map<String, String> cameFrom = new LinkedHashMap<>();
        Set<String> visited = new LinkedHashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(target)) break;
            for (String neighbor : graph.getOrDefault(current, List.of())) {
                if (visited.add(neighbor)) { // add zwraca false, gdy element już był w zbiorze
                    cameFrom.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }
        List<String> path = new ArrayList<>();
        String at = target;
        while (at != null) {
            path.add(0, at);
            if (at.equals(start)) break;
            at = cameFrom.get(at);
        }
        return path;
    }

    /**
     * 2. Ślad: start=Warszawa. Warstwa 0: {Warszawa}. Warstwa 1 (sąsiedzi): Krakow, Gdansk, Wroclaw —
     * wszystkie w odległości 1 "skoku". Warstwa 2: Poznan (sąsiad Gdanska i Wroclawia) — odległość 2.
     * BFS gwarantuje, że Poznan zostanie znaleziony w NAJMNIEJSZEJ liczbie skoków, bo warstwy rosną po kolei.
     */
    static void breadthFirstSearch() {
        section("2. Przeszukiwanie wszerz (BFS) — najkrótsza ścieżka bez wag");

        List<String> path = bfsShortestPath(cityGraph(), "Warszawa", "Poznan");
        show("najkrótsza ścieżka Warszawa → Poznan", path);
        show("liczba skoków (krawędzi)", path.size() - 1);
        // WYNIK: najkrótsza ścieżka Warszawa → Poznan → [Warszawa, Gdansk, Poznan]
        // WYNIK: liczba skoków (krawędzi) → 2

        note("BFS znalazł trasę przez Gdańsk (2 skoki), choć Wrocław też prowadzi do Poznania — bo Gdańsk był odkryty wcześniej (ta sama warstwa, kolejność dodania)");
    }

    // =================================================================================================
    // 3. PRZESZUKIWANIE W GŁĄB (DFS), REKURENCYJNIE + SPÓJNE SKŁADOWE
    // =================================================================================================

    static void dfsRecursive(Map<String, List<String>> graph, String node, Set<String> visited, List<String> order) {
        if (!visited.add(node)) return;
        order.add(node);
        for (String neighbor : graph.getOrDefault(node, List.of())) {
            dfsRecursive(graph, neighbor, visited, order);
        }
    }

    /** countConnectedComponents = ile ODDZIELNYCH "wysp" jest w grafie — powtarzaj DFS z każdego nieodwiedzonego węzła. */
    static int countConnectedComponents(Map<String, List<String>> graph) {
        Set<String> visited = new LinkedHashSet<>();
        int components = 0;
        for (String node : graph.keySet()) {
            if (!visited.contains(node)) {
                dfsRecursive(graph, node, visited, new ArrayList<>());
                components++;
            }
        }
        return components;
    }

    /**
     * 3. DFS schodzi w głąb PIERWSZYM sąsiadem, aż utknie, potem wraca (rekurencja = stos wywołań,
     * Methods03Recursion). Spójna składowa = zbiór węzłów osiągalnych z siebie nawzajem — licząc, ile razy
     * trzeba ZACZĄĆ DFS od nowa (bo trafiliśmy na nieodwiedzony węzeł), poznajemy liczbę "wysp".
     */
    static void depthFirstRecursive() {
        section("3. Przeszukiwanie w głąb (DFS), rekurencyjnie + spójne składowe");

        List<String> order = new ArrayList<>();
        dfsRecursive(cityGraph(), "Warszawa", new LinkedHashSet<>(), order);
        show("kolejność DFS od Warszawy", order);
        // WYNIK: kolejność DFS od Warszawy → [Warszawa, Krakow, Wroclaw, Poznan, Gdansk]

        Map<String, List<String>> withIsland = new LinkedHashMap<>(cityGraph());
        withIsland.put("Szczecin", List.of("Lublin"));
        withIsland.put("Lublin", List.of("Szczecin"));
        show("liczba spójnych składowych (z dodaną wyspą Szczecin-Lublin)", countConnectedComponents(withIsland));
        // WYNIK: liczba spójnych składowych (z dodaną wyspą Szczecin-Lublin) → 2

        note("Szczecin i Lublin są połączone ZE SOBĄ, ale NIE z resztą kraju — osobna spójna składowa");
    }

    // =================================================================================================
    // 4. PRZESZUKIWANIE W GŁĄB ZE STOSEM (ITERACYJNIE) + WYKRYWANIE CYKLI
    // =================================================================================================

    /** dfsIterative = ten sam DFS, ale WŁASNY stos (ArrayDeque) zamiast stosu wywołań — brak ryzyka StackOverflowError. */
    static List<String> dfsIterative(Map<String, List<String>> graph, String start) {
        List<String> order = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        ArrayDeque<String> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            String current = stack.pop();
            if (!visited.add(current)) continue;
            order.add(current);
            for (String neighbor : graph.getOrDefault(current, List.of())) {
                if (!visited.contains(neighbor)) stack.push(neighbor);
            }
        }
        return order;
    }

    static boolean dfsHasCycle(Map<String, List<String>> graph, String node, Set<String> visited, Set<String> onPath) {
        visited.add(node);
        onPath.add(node);
        for (String neighbor : graph.getOrDefault(node, List.of())) {
            if (onPath.contains(neighbor)) return true; // powrót do węzła z BIEŻĄCEJ ścieżki = cykl
            if (!visited.contains(neighbor) && dfsHasCycle(graph, neighbor, visited, onPath)) return true;
        }
        onPath.remove(node); // wychodzimy z węzła — nie jest już "na bieżącej ścieżce"
        return false;
    }

    static boolean hasCycleDirected(Map<String, List<String>> graph) {
        Set<String> visited = new HashSet<>();
        for (String node : graph.keySet()) {
            if (!visited.contains(node) && dfsHasCycle(graph, node, visited, new HashSet<>())) return true;
        }
        return false;
    }

    /**
     * 4. Iteracyjny DFS zastępuje stos WYWOŁAŃ własnym stosem (ArrayDeque) — identyczny efekt, bez limitu
     * głębokości rekurencji. Wykrywanie cyklu (w grafie SKIEROWANYM): jeśli podczas DFS trafimy na węzeł,
     * który jest NA BIEŻĄCEJ ścieżce (onPath), to mamy cykl — to NIE to samo, co "już odwiedzony kiedykolwiek".
     */
    static void depthFirstIterative() {
        section("4. Przeszukiwanie w głąb ze stosem (iteracyjnie) + wykrywanie cykli");

        show("kolejność DFS iteracyjnego od Warszawy", dfsIterative(cityGraph(), "Warszawa"));
        // WYNIK: kolejność DFS iteracyjnego od Warszawy → [Warszawa, Wroclaw, Poznan, Gdansk, Krakow]

        Map<String, List<String>> withCycle = new LinkedHashMap<>();
        withCycle.put("A", List.of("B"));
        withCycle.put("B", List.of("C"));
        withCycle.put("C", List.of("A")); // C wraca do A — cykl
        Map<String, List<String>> withoutCycle = new LinkedHashMap<>();
        withoutCycle.put("A", List.of("B"));
        withoutCycle.put("B", List.of("C"));
        withoutCycle.put("C", List.of());
        show("czy A→B→C→A ma cykl", hasCycleDirected(withCycle));
        show("czy A→B→C (bez powrotu) ma cykl", hasCycleDirected(withoutCycle));
        // WYNIK: czy A→B→C→A ma cykl → true
        // WYNIK: czy A→B→C (bez powrotu) ma cykl → false

        note("stos (LIFO) odwiedza sąsiadów w ODWROTNEJ kolejności niż rekurencja — stąd inna kolejność niż w sekcji 3, mimo tego samego grafu");
    }

    // =================================================================================================
    // 5. SORTOWANIE TOPOLOGICZNE (TOPOLOGICAL SORT) — ZALEŻNOŚCI MIĘDZY ZADANIAMI
    // =================================================================================================

    /** topologicalSort = algorytm Kahna: zacznij od węzłów BEZ zależności (in-degree 0), usuwaj je i odsłaniaj kolejne. */
    static List<String> topologicalSort(Map<String, List<String>> graph) {
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        for (String node : graph.keySet()) inDegree.putIfAbsent(node, 0);
        for (List<String> neighbors : graph.values()) {
            for (String neighbor : neighbors) inDegree.merge(neighbor, 1, Integer::sum);
        }
        Queue<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) queue.add(entry.getKey());
        }
        List<String> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            order.add(current);
            for (String neighbor : graph.getOrDefault(current, List.of())) {
                inDegree.merge(neighbor, -1, Integer::sum);
                if (inDegree.get(neighbor) == 0) queue.add(neighbor);
            }
        }
        return order; // order.size() < inDegree.size() → graf miał cykl (niemożliwe uszeregowanie)
    }

    /**
     * 5. Krawędź "Podstawy → OOP" oznacza "Podstawy przed OOP" (wymaganie wstępne). in-degree węzła =
     * ile zależności musi być spełnionych najpierw. Zaczynamy od zależności BEZ wymagań (in-degree 0).
     * (PYTANIE REKRUTACYJNE: uszereguj zadania z zależnościami — kolejność zaliczania przedmiotów).
     */
    static void topologicalSortDemo() {
        section("5. Sortowanie topologiczne — zależności między zadaniami (course prerequisites)");

        Map<String, List<String>> courses = new LinkedHashMap<>();
        courses.put("Podstawy", List.of("OOP"));
        courses.put("OOP", List.of("Kolekcje", "Wyjatki"));
        courses.put("Kolekcje", List.of("Strumienie"));
        courses.put("Wyjatki", List.of("Strumienie"));
        courses.put("Strumienie", List.of());

        List<String> order = topologicalSort(courses);
        show("poprawna kolejność zaliczania", order);
        // WYNIK: poprawna kolejność zaliczania → [Podstawy, OOP, Kolekcje, Wyjatki, Strumienie]

        note("Kolekcje i Wyjątki mogłyby zamienić się miejscami (oba zależą tylko od OOP) — sortowanie topologiczne NIE jest jednoznaczne, gdy jest kilka poprawnych kolejności");
    }

    // =================================================================================================
    // 6. DIJKSTRA — NAJKRÓTSZE ŚCIEŻKI W GRAFIE WAŻONYM
    // =================================================================================================

    record Edge(String to, int weight) {
    }

    record DistNode(int distance, String city) {
    }

    static Map<String, List<Edge>> weightedCityGraph() {
        Map<String, List<Edge>> graph = new LinkedHashMap<>();
        graph.put("Warszawa", List.of(new Edge("Lodz", 130), new Edge("Krakow", 290)));
        graph.put("Lodz", List.of(new Edge("Warszawa", 130), new Edge("Krakow", 160), new Edge("Wroclaw", 190), new Edge("Poznan", 200)));
        graph.put("Krakow", List.of(new Edge("Warszawa", 290), new Edge("Lodz", 160), new Edge("Wroclaw", 270)));
        graph.put("Wroclaw", List.of(new Edge("Lodz", 190), new Edge("Krakow", 270), new Edge("Poznan", 180)));
        graph.put("Poznan", List.of(new Edge("Lodz", 200), new Edge("Wroclaw", 180)));
        return graph;
    }

    /** dijkstra = zawsze przetwarzaj NAJBLIŻSZY nieustalony wierzchołek (kolejka priorytetowa) — poprawiaj odległości sąsiadów (relaksacja). */
    static Map<String, Integer> dijkstra(Map<String, List<Edge>> graph, String start) {
        Map<String, Integer> dist = new LinkedHashMap<>();
        for (String node : graph.keySet()) dist.put(node, Integer.MAX_VALUE);
        dist.put(start, 0);
        Set<String> settled = new HashSet<>();
        PriorityQueue<DistNode> queue = new PriorityQueue<>(Comparator.comparingInt(DistNode::distance));
        queue.add(new DistNode(0, start));
        while (!queue.isEmpty()) {
            String current = queue.poll().city();
            if (!settled.add(current)) continue; // stary, nieaktualny wpis w kolejce — pomiń
            for (Edge edge : graph.getOrDefault(current, List.of())) {
                int candidate = dist.get(current) + edge.weight();
                if (candidate < dist.get(edge.to())) {
                    dist.put(edge.to(), candidate);
                    queue.add(new DistNode(candidate, edge.to())); // nowy, lepszy wpis — stary zostanie zignorowany
                }
            }
        }
        return dist;
    }

    /**
     * 6. Odległości między polskimi miastami (zmyślone, ale stałe, w km). Dijkstra z Warszawy: najpierw
     * przetwarzamy Łódź (130, najbliżej), potem Kraków (290), potem Wrocław (130+190=320, przez Łódź),
     * na końcu Poznań (130+200=330, przez Łódź — bliżej niż 320+180=500 przez Wrocław).
     */
    static void dijkstraDemo() {
        section("6. Dijkstra — najkrótsze ścieżki w grafie ważonym (polskie miasta)");

        Map<String, Integer> distances = dijkstra(weightedCityGraph(), "Warszawa");
        showEach("odległości od Warszawy (km)", new TreeMap<>(distances));
        // WYNIK: odległości od Warszawy (km) (liczba kluczy: 5):
        // WYNIK:    • Krakow → 290
        // WYNIK:    • Lodz → 130
        // WYNIK:    • Poznan → 330
        // WYNIK:    • Warszawa → 0
        // WYNIK:    • Wroclaw → 320

        note("najkrótsza droga do Poznania idzie PRZEZ Łódź (330 km), mimo że Poznań sąsiaduje też z Wrocławiem — suma wag decyduje, nie liczba skoków");

        // DOBRA PRAKTYKA: Dijkstra NIE działa poprawnie z UJEMNYMI wagami (algorytm Bellmana-Forda sobie z tym radzi,
        //   kosztem O(V*E)) — zakłada, że dodanie krawędzi nigdy nie SKRACA already ustalonej najkrótszej drogi.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Lista sąsiedztwa: O(V+E) pamięci. Macierz: O(V²), za to O(1) sprawdzenie krawędzi.
     *   • BFS: kolejka FIFO, warstwami — najkrótsza ścieżka BEZ wag, O(V+E).
     *   • DFS: rekurencja albo własny stos, schodzi w głąb — O(V+E); spójne składowe = ile razy zaczynamy od nowa.
     *   • Wykrywanie cyklu (graf skierowany): węzeł NA BIEŻĄCEJ ścieżce (onPath), nie tylko "kiedykolwiek odwiedzony".
     *   • Sortowanie topologiczne: algorytm Kahna, in-degree 0 jako start; niemożliwe, gdy graf ma cykl.
     *   • Dijkstra: kolejka priorytetowa zamiast FIFO — najkrótsze ścieżki z WAGAMI, O((V+E) log V). Nie działa z ujemnymi wagami.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego BFS gwarantuje najkrótszą ścieżkę w grafie BEZ wag, a DFS — nie?
     *   2. Co wypisze:  {@code Map<String, List<String>> g = new LinkedHashMap<>(); g.put("X", List.of());
     *      System.out.println(topologicalSort(g));}  ?
     *   3. ZNAJDŹ BŁĄD: ktoś wykrywa cykl, sprawdzając tylko "czy węzeł już kiedyś odwiedzony" (zbiór visited),
     *      bez osobnego zbioru "na bieżącej ścieżce". Jaki graf da fałszywy alarm?
     *   4. Dlaczego sortowanie topologiczne nie zawsze daje JEDNOZNACZNĄ kolejność?
     *   5. Dlaczego Dijkstra używa kolejki PRIORYTETOWEJ, a BFS zwykłej kolejki FIFO?
     *   6. ZNAJDŹ BŁĄD: ktoś używa Dijkstry na grafie z ujemną wagą krawędzi i dziwi się błędnym wynikom.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: BFS Warszawa → Krakow, liczba skoków", 1, () -> exercise1());
        Check.equal("ćw. 2: DFS od \"A\" w A→B, A→C, B→D", "[A, B, D, C]", () -> exercise2());
        Check.equal("ćw. 3: czy D→E→F→D ma cykl", true, () -> exercise3());
        Check.equal("ćw. 4: Dijkstra, odległość Warszawa → Wroclaw", 320, () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 1, () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", "[A, B, D, C]", () -> solution2());
        Check.equal("ćw. 3 (wzorzec)", true, () -> solution3());
        Check.equal("ćw. 4 (wzorzec)", 320, () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz liczbę skoków BFS z Warszawy do Krakowa w cityGraph().
     * Podpowiedź: wywołaj bfsShortestPath i zwróć path.size() - 1.
     */
    static int exercise1() {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 2 (łatwe): zbuduj graf A→[B,C], B→[D] i zwróć kolejność DFS (rekurencyjnie) od "A" jako tekst.
     * Podpowiedź: dfsRecursive + Arrays.toString albo zwykłe show-like formatowanie (tu: List.toString()).
     */
    static String exercise2() {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 3 (średnie): sprawdź, czy graf D→E, E→F, F→D ma cykl.
     * Podpowiedź: wywołaj hasCycleDirected.
     */
    static boolean exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): policz Dijkstrą odległość z Warszawy do Wroclawia w weightedCityGraph().
     * Podpowiedź: wywołaj dijkstra i odczytaj wynik dla klucza "Wroclaw".
     */
    static int exercise4() {
        // TODO: twoje rozwiązanie
        return -1;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1() {
        return bfsShortestPath(cityGraph(), "Warszawa", "Krakow").size() - 1;
    }

    static String solution2() {
        Map<String, List<String>> graph = new LinkedHashMap<>();
        graph.put("A", List.of("B", "C"));
        graph.put("B", List.of("D"));
        List<String> order = new ArrayList<>();
        dfsRecursive(graph, "A", new LinkedHashSet<>(), order);
        return order.toString();
    }

    static boolean solution3() {
        Map<String, List<String>> graph = new LinkedHashMap<>();
        graph.put("D", List.of("E"));
        graph.put("E", List.of("F"));
        graph.put("F", List.of("D"));
        return hasCycleDirected(graph);
    }

    static int solution4() {
        return dijkstra(weightedCityGraph(), "Warszawa").get("Wroclaw");
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. BFS odwiedza węzły WARSTWAMI (rosnąca liczba skoków) — pierwsze dotarcie do celu jest więc
     *      zawsze najkrótsze. DFS może zejść długą, krętą ścieżką i trafić do celu PO wielu zbędnych skokach.
     *   2. [X] — pojedynczy węzeł bez zależności ma trywialny porządek topologiczny.
     *   3. Graf A→B, A→C, B→C: DFS od A odwiedza B (visited={A,B}), potem wraca i idzie do C — C NIE jest
     *      cyklem, ale jeśli sprawdzalibyśmy tylko "czy C odwiedzony" zamiast "czy C na bieżącej ścieżce",
     *      wynik akurat tu byłby OK; prawdziwy fałszywy alarm pojawia się w grafach, gdzie dwie gałęzie
     *      prowadzą do tego samego, już odwiedzonego (ale zakończonego) węzła — bez onPath nie odróżnimy
     *      tego od prawdziwego cyklu.
     *   4. Gdy kilka węzłów ma jednocześnie in-degree 0 (brak zależności między nimi), kolejność ich
     *      przetworzenia jest dowolna — oba porządki są równie poprawne.
     *   5. BFS zakłada, że każda krawędź "kosztuje" tyle samo (1 skok) — kolejność FIFO wystarcza. Dijkstra
     *      ma różne wagi krawędzi, więc musi zawsze przetwarzać NAJBLIŻSZY (nie najwcześniej odkryty) wierzchołek.
     *   6. Dijkstra zakłada, że odległość do węzła tylko ROŚNIE w miarę dodawania krawędzi na ścieżce —
     *      ujemna waga łamie to założenie, bo "dłuższa" ścieżka może nagle stać się krótsza po dodaniu
     *      ujemnej krawędzi, już PO ustaleniu (settled) wcześniejszego wierzchołka.
     */
    // </editor-fold>
}
