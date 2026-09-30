package t12_collections;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Employee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Klasa Collections — statyczne narzędzia do pracy z listami i kolekcjami
 *        (utility class = klasa narzędziowa; seed = ziarno losowości; in place = w miejscu, mutując oryginał)
 *
 * W SKRÓCIE:
 *   java.util.Collections to klasa narzędziowa (jak Math albo Arrays) — SAME statyczne metody działające na
 *   istniejących List/Set/Collection: sortowanie, odwracanie, tasowanie, szukanie ekstremów, liczenie wystąpień,
 *   binarySearch, zamiana miejscami, obrót, sprawdzanie rozłączności zbiorów, mini-fabryki pustej/jednoelementowej
 *   listy. Większość z nich działa W MIEJSCU (mutuje przekazaną listę) — to ważna różnica względem stream().
 *
 * ANALOGIA: skrzynka z narzędziami do „gotowej” listy.
 *   Masz już listę (ArrayList) — to jak masz już deskę. Collections to skrzynka z narzędziami: piła (sort),
 *   szlifierka (reverse), losowanie karty (shuffle) — wszystkie działają NA desce, którą im podasz, a nie tworzą
 *   nowej. Arrays to taka sama skrzynka, tylko do tablic zamiast list (sekcja 9).
 *
 * JAK TO DZIAŁA:
 *   Collections.sort(list);                       ← sortuje W MIEJSCU, zwraca void
 *   Collections.shuffle(list, new Random(42));     ← tasuje W MIEJSCU, z ziarnem = powtarzalnie
 *   T max = Collections.max(list, comparator);     ← zwraca element, nie zmienia listy
 *   int i = Collections.binarySearch(list, key);   ← lista MUSI być posortowana tym samym porządkiem!
 *
 * SŁÓWKA:
 *   in place = w miejscu (mutuje oryginał); seed = ziarno (startowa liczba generatora losowego);
 *   frequency = częstość (ile razy element występuje); disjoint = rozłączne (brak wspólnych elementów);
 *   rotate = obrót (przesunięcie cykliczne); swap = zamiana miejscami; singleton = jednoelementowy.
 *
 * ZOBACZ TEŻ: t03_arrays/Arrays03Utility (odpowiednik dla tablic), t12_collections/Collections07ComparableComparator
 *             (Comparator używany tu w max/min/sort), t16_streams/Streams06TerminalOps (stream().sorted()/max()
 *             jako alternatywa, która NIE mutuje źródła).
 * </pre>
 */
public class Collections09CollectionsUtility {

    public static void main(String[] args) {
        title("Collections09 — klasa Collections: sort, shuffle, max/min, binarySearch i więcej");

        sortAndReverse();          // sort and reverse = sortowanie i odwracanie
        shuffleDeterministic();    // shuffle deterministic = tasowanie powtarzalne
        maxMinWithComparator();    // max min with comparator = max/min z komparatorem
        frequencyAndNCopies();     // frequency and n copies = częstość i n kopii
        swapAndRotate();           // swap and rotate = zamiana i obrót
        binarySearchPitfall();     // binary search pitfall = pułapka binarySearch
        disjointAndAddAll();       // disjoint and add all = rozłączność i dodawanie hurtowe
        emptyAndSingleton();       // empty and singleton = pusta i jednoelementowa
        arraysVsCollections();     // arrays vs collections = Arrays kontra Collections
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. sort I reverse
    // =================================================================================================

    /** 1. Collections.sort sortuje listę W MIEJSCU (mutuje ją) — porządkiem naturalnym albo Comparatorem. */
    static void sortAndReverse() {
        section("1. Collections.sort i Collections.reverse");

        List<Integer> numbers = new ArrayList<>(SampleData.numbers());
        show("liczby (kopia z SampleData)", numbers);
        // WYNIK: liczby (kopia z SampleData) → [5, 3, 8, 1, 9, 2, 7, 3, 10, 6, 4, 8]

        Collections.sort(numbers);
        show("po sort()", numbers);
        // WYNIK: po sort() → [1, 2, 3, 3, 4, 5, 6, 7, 8, 8, 9, 10]

        Collections.reverse(numbers);
        show("po reverse() (lista była posortowana → efekt: malejąco)", numbers);
        // WYNIK: po reverse() (lista była posortowana → efekt: malejąco) → [10, 9, 8, 8, 7, 6, 5, 4, 3, 3, 2, 1]

        List<String> names = new ArrayList<>(List.of("Celina", "Ala", "Bartek"));
        Collections.sort(names, Comparator.reverseOrder());
        show("nazwiska malejąco (Comparator)", names);
        // WYNIK: nazwiska malejąco (Comparator) → [Celina, Bartek, Ala]

        // DOBRA PRAKTYKA: sort/reverse mutują przekazaną listę. Potrzebujesz oryginału bez zmian? Sortuj KOPIĘ
        //   (jak numbers powyżej, zrobiona z SampleData.numbers()) — samo SampleData.numbers() jest niezmienne.
        // PUŁAPKA: Collections.reverse to NIE „posortuj malejąco” — to zwykłe odwrócenie kolejności elementów.
        //   Reverse nieposortowanej listy da bałagan, nie porządek malejący (zobacz pytanie kontrolne 2).
    }

    // =================================================================================================
    // 2. shuffle — TASOWANIE Z ZIARNEM
    // =================================================================================================

    /**
     * 2. Collections.shuffle bez podanego Random używa globalnego generatora — wynik inny za każdym razem
     * (niedeterministyczny, więc bez WYNIK). Z podanym `new Random(seed)` wynik jest ZAWSZE taki sam dla tego seeda.
     */
    static void shuffleDeterministic() {
        section("2. Collections.shuffle — tasowanie z ziarnem (seed) = powtarzalne");

        List<Integer> deck = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8));
        Collections.shuffle(deck, new Random(42));
        show("potasowana talia (seed 42)", deck);
        // WYNIK: potasowana talia (seed 42) → [3, 7, 4, 2, 5, 1, 8, 6]

        List<String> names = new ArrayList<>(List.of("Ala", "Bartek", "Celina", "Darek", "Ela"));
        Collections.shuffle(names, new Random(7));
        show("potasowana lista imion (seed 7)", names);
        // WYNIK: potasowana lista imion (seed 7) → [Ela, Darek, Ala, Celina, Bartek]

        note("Collections.shuffle(list) BEZ drugiego argumentu też działa — ale wtedy wynik jest inny przy każdym");
        note("uruchomieniu (globalny generator). Do testów i przykładów w tym kursie ZAWSZE podajemy Random z ziarnem.");
    }

    // =================================================================================================
    // 3. max / min Z COMPARATOREM
    // =================================================================================================

    /** 3. Collections.max/min przeszukuje kolekcję i zwraca element ekstremalny — bez mutowania jej. */
    static void maxMinWithComparator() {
        section("3. Collections.max / min z Comparatorem");

        List<Employee> employees = SampleData.employees();

        Employee highestPaid = Collections.max(employees, Comparator.comparingInt(Employee::salary));
        show("najlepiej zarabiający", highestPaid);
        // WYNIK: najlepiej zarabiający → Michał Lewandowski (IT, 17200 zł)

        Employee youngest = Collections.min(employees, Comparator.comparingInt(Employee::age));
        show("najmłodszy", youngest);
        // WYNIK: najmłodszy → Piotr Kowalski (IT, 9800 zł)

        // DOBRA PRAKTYKA: max/min(kolekcja, comparator) to wersja „kolekcyjna”. Jeśli dane masz już jako Stream,
        //   wygodniej jest użyć stream.max(comparator) (t16_streams/Streams06TerminalOps) — ten sam pomysł,
        //   inny punkt wejścia. Obie zwracają element, żadna nie zmienia źródła.
    }

    // =================================================================================================
    // 4. frequency I nCopies
    // =================================================================================================

    /** 4. frequency liczy wystąpienia elementu (equals). nCopies tworzy niemodyfikowalną listę powtórzeń. */
    static void frequencyAndNCopies() {
        section("4. Collections.frequency i Collections.nCopies");

        List<Integer> numbers = SampleData.numbers();
        show("ile razy występuje 8?", Collections.frequency(numbers, 8));
        // WYNIK: ile razy występuje 8? → 2
        show("ile razy występuje 3?", Collections.frequency(numbers, 3));
        // WYNIK: ile razy występuje 3? → 2
        show("ile razy występuje 100 (go nie ma)?", Collections.frequency(numbers, 100));
        // WYNIK: ile razy występuje 100 (go nie ma)? → 0

        List<String> padding = Collections.nCopies(4, "x");
        show("nCopies(4, \"x\")", padding);
        // WYNIK: nCopies(4, "x") → [x, x, x, x]

        expectThrows("nCopies(...).set(0, \"y\")", () -> padding.set(0, "y"));
        // WYNIK: ✔ nCopies(...).set(0, "y") → rzucono UnsupportedOperationException: (brak komunikatu)

        // PUŁAPKA: nCopies zwraca niemodyfikowalną listę, w której WSZYSTKIE "kopie" to w rzeczywistości JEDNA,
        //   powtórzona n razy referencja. Dla niezmiennych elementów (String, liczby) to nieszkodliwe. Dla
        //   zmiennego obiektu (np. własna klasa z setterem, StringBuilder) byłaby to jedna wspólna pułapka:
        //   zmiana "jednej kopii" (przez ten sam obiekt) zmieniłaby WSZYSTKIE na raz.
    }

    // =================================================================================================
    // 5. swap I rotate
    // =================================================================================================

    /** 5. swap zamienia dwa elementy miejscami. rotate przesuwa wszystkie cyklicznie o distance pozycji. */
    static void swapAndRotate() {
        section("5. Collections.swap i Collections.rotate");

        List<String> podium = new ArrayList<>(List.of("złoto", "srebro", "brąz"));
        Collections.swap(podium, 0, 2);
        show("po swap(podium, 0, 2)", podium);
        // WYNIK: po swap(podium, 0, 2) → [brąz, srebro, złoto]

        List<Integer> queue = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        Collections.rotate(queue, 2);
        show("po rotate(queue, 2)", queue);
        // WYNIK: po rotate(queue, 2) → [4, 5, 1, 2, 3]

        Collections.rotate(queue, -2);   // cofnięcie o tyle samo — wraca do poprzedniej kolejności
        show("po rotate(queue, -2) (z powrotem)", queue);
        // WYNIK: po rotate(queue, -2) (z powrotem) → [1, 2, 3, 4, 5]

        // ANALOGIA: rotate to przesuwanie talerzy na okrągłym, obrotowym stole (jak w chińskiej restauracji) —
        //   element, który "wypadłby" z końca, wraca na początek. distance dodatni = w prawo, ujemny = w lewo.
    }

    // =================================================================================================
    // 6. PUŁAPKA: binarySearch NA NIEPOSORTOWANEJ LIŚCIE
    // =================================================================================================

    /**
     * 6. Collections.binarySearch zakłada, że lista jest posortowana TYM SAMYM porządkiem, którego (opcjonalnie)
     * używasz do wyszukiwania. Jeśli nie jest — wynik jest NIEOKREŚLONY (może być błędny, jak niżej).
     */
    static void binarySearchPitfall() {
        section("6. Collections.binarySearch — wymaga POSORTOWANEJ listy");

        List<Integer> sorted = new ArrayList<>(List.of(1, 3, 4, 6, 8, 9, 10));
        show("posortowana lista", sorted);
        // WYNIK: posortowana lista → [1, 3, 4, 6, 8, 9, 10]

        show("binarySearch(8) — jest na indeksie", Collections.binarySearch(sorted, 8));
        // WYNIK: binarySearch(8) — jest na indeksie → 4

        show("binarySearch(5) — nie ma, ujemny wynik", Collections.binarySearch(sorted, 5));
        // WYNIK: binarySearch(5) — nie ma, ujemny wynik → -4
        note("Ujemny wynik to -(punkt wstawienia) - 1: 5 wstawiłoby się na indeks 3 (przed 6), więc -(3)-1 = -4.");

        List<Integer> unsorted = new ArrayList<>(List.of(5, 3, 8, 1, 9, 2, 7));
        show("lista NIEposortowana", unsorted);
        // WYNIK: lista NIEposortowana → [5, 3, 8, 1, 9, 2, 7]

        show("czy 3 naprawdę jest w liście? (contains)", unsorted.contains(3));
        // WYNIK: czy 3 naprawdę jest w liście? (contains) → true

        show("binarySearch(unsorted, 3) — ZŁY wynik!", Collections.binarySearch(unsorted, 3));
        // WYNIK: binarySearch(unsorted, 3) — ZŁY wynik! → -7

        // PUŁAPKA: 3 JEST w liście (na indeksie 1), a mimo to binarySearch twierdzi, że go nie ma (wynik ujemny)!
        //   Algorytm skacze po indeksach zakładając porządek rosnący — na nieposortowanych danych "gubi" element,
        //   którego akurat nie odwiedził. To NIE jest wyjątek ani wyraźny błąd — to CICHO zły wynik, dlatego jest
        //   groźniejsze niż np. ConcurrentModificationException. Zawsze sortuj (albo sortuj kopię — ćwiczenie 4)
        //   przed binarySearch, i to tym samym porządkiem (naturalnym albo tym samym Comparatorem).
    }

    // =================================================================================================
    // 7. disjoint I addAll
    // =================================================================================================

    /** 7. disjoint sprawdza brak wspólnych elementów dwóch kolekcji. addAll dodaje wiele elementów naraz. */
    static void disjointAndAddAll() {
        section("7. Collections.disjoint i Collections.addAll");

        List<String> a = List.of("Java", "Python", "Go");
        List<String> b = List.of("Rust", "C++");
        List<String> c = List.of("Go", "Kotlin");

        show("disjoint(a, b) — brak wspólnych?", Collections.disjoint(a, b));
        // WYNIK: disjoint(a, b) — brak wspólnych? → true
        show("disjoint(a, c) — brak wspólnych?", Collections.disjoint(a, c));
        // WYNIK: disjoint(a, c) — brak wspólnych? → false

        List<String> target = new ArrayList<>(List.of("start"));
        Collections.addAll(target, "x", "y", "z");   // szybszy zapis niż trzy razy target.add(...)
        show("po addAll(target, \"x\", \"y\", \"z\")", target);
        // WYNIK: po addAll(target, "x", "y", "z") → [start, x, y, z]

        // DOBRA PRAKTYKA: Collections.addAll(kolekcja, elementy...) jest szybsze do zapisania niż seria add() i,
        //   w przeciwieństwie do target.addAll(List.of(...)), nie tworzy po drodze dodatkowej, tymczasowej listy.
    }

    // =================================================================================================
    // 8. emptyList, singletonList
    // =================================================================================================

    /** 8. Mini-fabryki sprzed Javy 9 — dziś częściej zastępowane przez List.of() / List.of(x) (Collections08). */
    static void emptyAndSingleton() {
        section("8. Collections.emptyList i Collections.singletonList");

        List<String> empty = Collections.emptyList();
        show("emptyList()", empty);
        show("rozmiar", empty.size());
        // WYNIK: emptyList() → []
        // WYNIK: rozmiar → 0

        List<String> single = Collections.singletonList("jedyny");
        show("singletonList(\"jedyny\")", single);
        // WYNIK: singletonList("jedyny") → [jedyny]

        expectThrows("singletonList.add(...)", () -> single.add("drugi"));
        // WYNIK: ✔ singletonList.add(...) → rzucono UnsupportedOperationException: (brak komunikatu)

        note("Dziś częściej pisze się po prostu List.of() i List.of(\"jedyny\") (Java 9+, Collections08) — te metody");
        note("z Collections są starsze (od Javy 1.2), wciąż działają, ale List.of jest bardziej spójne i czytelne.");
    }

    // =================================================================================================
    // 9. Arrays kontra Collections
    // =================================================================================================

    /** 9. Dwie równoległe „skrzynki z narzędziami”: Arrays na tablicach, Collections na kolekcjach. */
    static void arraysVsCollections() {
        section("9. Arrays vs Collections — dwie równoległe skrzynki z narzędziami");

        int[] primitives = {5, 3, 8, 1};
        Arrays.sort(primitives);
        show("Arrays.sort(int[])", primitives);
        // WYNIK: Arrays.sort(int[]) → [1, 3, 5, 8]

        List<Integer> boxedList = new ArrayList<>(List.of(5, 3, 8, 1));
        Collections.sort(boxedList);
        show("Collections.sort(List<Integer>)", boxedList);
        // WYNIK: Collections.sort(List<Integer>) → [1, 3, 5, 8]

        note("Arrays działa na TABLICACH (int[], String[]...) — t03_arrays/Arrays03Utility.");
        note("Collections działa na KOLEKCJACH (List, Set, Collection...) — ta lekcja.");
        note("Obie to klasy narzędziowe: same statyczne metody, prywatny konstruktor — nie da się ich stworzyć.");

        // PUŁAPKA: „new Collections()” ani „new Arrays()” się nie skompiluje (błąd kompilacji) — konstruktor
        //   jest prywatny, dokładnie jak w Math i Objects. Te klasy nie reprezentują NICZEGO, co miałoby stan —
        //   są tylko workiem funkcji, więc autorzy JDK świadomie zablokowali tworzenie ich instancji.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Collections.sort/reverse/shuffle/swap/rotate — działają W MIEJSCU (mutują przekazaną listę, zwracają void).
     *   • Collections.max/min/frequency/disjoint/binarySearch — TYLKO odczytują, zwracają wynik.
     *   • binarySearch wymaga listy POSORTOWANEJ tym samym porządkiem — inaczej wynik jest niezdefiniowany (cicho zły!).
     *   • nCopies(n, x) — n razy TA SAMA referencja x, niemodyfikowalna lista; uważaj przy zmiennym x.
     *   • emptyList()/singletonList(x) — starsze odpowiedniki List.of()/List.of(x) (Collections08).
     *   • Arrays = narzędzia dla tablic, Collections = narzędzia dla kolekcji — API często lustrzane (sort, binarySearch).
     *   • Bez WYNIK: Collections.shuffle(list) bez Random — niedeterministyczne; z Random(seed) — powtarzalne.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się Collections.sort(list) od list.stream().sorted().toList() pod względem tego, co się
     *      dzieje z listą list?
     *   2. Co wypisze:
     *          List<Integer> l = new ArrayList<>(List.of(3, 1, 2));
     *          Collections.reverse(l);
     *          System.out.println(l);
     *   3. ZNAJDŹ BŁĄD (a raczej: dlaczego wynik jest niewiarygodny):
     *          List<Integer> l = new ArrayList<>(List.of(9, 2, 5));
     *          int idx = Collections.binarySearch(l, 5);
     *   4. Co wypisze:  System.out.println(Collections.frequency(List.of(1, 1, 2, 3, 1), 1));  ?
     *   5. Dlaczego Collections.nCopies bywa niebezpieczne z zmiennymi elementami?
     *   6. Dlaczego klasy Collections i Arrays mają prywatne konstruktory?
     *   7. Kiedy wybrać Collections.max(list, comparator), a kiedy list.stream().max(comparator)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: posortowana malejąco kopia", List.of(3, 2, 1), () -> exercise1(List.of(2, 3, 1)));
        Check.equal("ćw. 2: najlepiej zarabiający (Collections.max)", "Michał Lewandowski (IT, 17200 zł)",
                () -> exercise2(SampleData.employees()).toString());
        Check.equal("ćw. 3: obrót tak, by element trafił na początek", List.of("c", "d", "a", "b"),
                () -> exercise3(List.of("a", "b", "c", "d"), 2));
        Check.equal("ćw. 4a: bezpieczne wyszukiwanie (jest)", true, () -> exercise4(List.of(5, 3, 8, 1), 8));
        Check.equal("ćw. 4b: bezpieczne wyszukiwanie (nie ma)", false, () -> exercise4(List.of(5, 3, 8, 1), 100));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of(3, 2, 1), () -> solution1(List.of(2, 3, 1)));
        Check.equal("ćw. 2 (wzorzec)", "Michał Lewandowski (IT, 17200 zł)",
                () -> solution2(SampleData.employees()).toString());
        Check.equal("ćw. 3 (wzorzec)", List.of("c", "d", "a", "b"), () -> solution3(List.of("a", "b", "c", "d"), 2));
        Check.equal("ćw. 4a (wzorzec)", true, () -> solution4(List.of(5, 3, 8, 1), 8));
        Check.equal("ćw. 4b (wzorzec)", false, () -> solution4(List.of(5, 3, 8, 1), 100));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć NOWĄ listę z elementami source posortowanymi malejąco — source ma zostać
     * nietknięte. Podpowiedź: new ArrayList<>(source), potem Collections.sort z Comparator.reverseOrder().
     */
    static List<Integer> exercise1(List<Integer> source) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (łatwe, PRZEPISZ): poniższa pętla ręcznie szuka najlepiej zarabiającego pracownika:
     * <pre>{@code
     * Employee best = employees.get(0);
     * for (Employee e : employees) {
     *     if (e.salary() > best.salary()) {
     *         best = e;
     *     }
     * }
     * }</pre>
     * Przepisz to na jedno wywołanie Collections.max z odpowiednim Comparatorem.
     */
    static Employee exercise2(List<Employee> employees) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć KOPIĘ listy obróconą tak, by element z podanego indeksu trafił na pozycję 0
     * (kolejność cykliczna zachowana). Podpowiedź: new ArrayList<>(list), Collections.rotate(kopia, -index).
     */
    static <T> List<T> exercise3(List<T> list, int index) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze, łączy z Collections08): sprawdź, czy key jest w liście, sortując KOPIĘ (żeby nie
     * zepsuć kolejności oryginału — kopia obronna z Collections08!) i robiąc na niej Collections.binarySearch.
     * Podpowiedź: new ArrayList<>(list), Collections.sort(kopia), Collections.binarySearch(kopia, key) >= 0.
     */
    static boolean exercise4(List<Integer> list, int key) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<Integer> solution1(List<Integer> source) {
        List<Integer> copy = new ArrayList<>(source);
        Collections.sort(copy, Comparator.reverseOrder());
        return copy;
    }

    static Employee solution2(List<Employee> employees) {
        return Collections.max(employees, Comparator.comparingInt(Employee::salary));
    }

    static <T> List<T> solution3(List<T> list, int index) {
        List<T> copy = new ArrayList<>(list);
        Collections.rotate(copy, -index);
        return copy;
    }

    static boolean solution4(List<Integer> list, int key) {
        List<Integer> sortedCopy = new ArrayList<>(list);
        Collections.sort(sortedCopy);
        return Collections.binarySearch(sortedCopy, key) >= 0;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Collections.sort mutuje list W MIEJSCU (ta sama lista, void). stream().sorted().toList() zostawia list
     *      bez zmian i zwraca ZUPEŁNIE NOWĄ, niezmienną listę (t16_streams).
     *   2. „[2, 1, 3]” — reverse to zwykłe odwrócenie kolejności, a nie sortowanie malejące. Lista nie była
     *      posortowana, więc wynik nie jest uporządkowany malejąco.
     *   3. Wynik jest niewiarygodny: [9, 2, 5] nie jest posortowana, więc Collections.binarySearch nie ma prawa
     *      działać poprawnie — może zwrócić dowolny indeks albo (jak w sekcji 6) fałszywie ujemny wynik.
     *   4. „3” — 1 występuje trzy razy w [1, 1, 2, 3, 1].
     *   5. Wszystkie "kopie" w nCopies to w rzeczywistości JEDNA, wspólna referencja powtórzona n razy. Dla
     *      zmiennego obiektu zmiana jednej "kopii" (przez tę referencję) widoczna byłaby we WSZYSTKICH pozycjach
     *      na raz, bo to fizycznie ten sam obiekt.
     *   6. Nie reprezentują żadnego stanu — są tylko zbiorem funkcji statycznych. Utworzenie instancji nie miałoby
     *      sensu (co by taki obiekt "przechowywał"?), więc prywatny konstruktor jawnie to blokuje.
     *   7. Collections.max ma sens, gdy dane masz już jako zwykłą kolekcję (List/Set) i nie budujesz z nich innego
     *      przetwarzania. stream().max jest wygodniejszy, gdy max jest jednym z kroków dłuższego potoku (np. po
     *      filter/map) — nie trzeba wtedy materializować pośredniej listy tylko po to, by wywołać Collections.max.
     */
    // </editor-fold>
}
