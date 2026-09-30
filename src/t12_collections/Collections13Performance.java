package t12_collections;

import helpers.Check;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wydajność kolekcji — Big-O, policzone operacje (nie stoper!), typowe pułapki
 *        (Big-O = notacja dużego O, rząd złożoności; amortized = zamortyzowany (średnio, w dłuższym okresie))
 *
 * W SKRÓCIE:
 *   Ta sama linijka kodu (list.contains(x), list.get(i), list.remove(0)) może kosztować JEDEN krok albo TYSIĄCE
 *   kroków — zależnie od tego, JAKĄ implementację List/Set/Map wybrałeś. W tej lekcji NIE mierzymy czasu
 *   stoperem (to zwodnicze w Javie — sekcja 8) — LICZYMY operacje (equals(), przesunięcia elementów, skoki po
 *   wskaźnikach), żeby zobaczyć różnicę rzędu wielkości gołym okiem, w twardych liczbach.
 *
 * ANALOGIA: szukanie książki.
 *   ArrayList/List.contains to przeglądanie PÓŁKI książka po książce, od lewej, aż znajdziesz szukaną (albo
 *   dojdziesz do końca). HashSet.contains to katalog biblioteczny: znasz "sygnaturę" (hashCode), idziesz WPROST
 *   na właściwą półkę. TreeSet to biblioteka z książkami ułożonymi alfabetycznie — nie tak szybko jak katalog,
 *   ale za to zawsze w kolejności.
 *
 * JAK TO DZIAŁA:
 *   Struktura    | get(i)       | dodaj/usuń KONIEC | dodaj/usuń POCZĄTEK | contains
 *   ArrayList    | O(1)         | O(1) amortyzowane | O(n) (przesunięcie) | O(n)
 *   LinkedList   | O(n)         | O(1)               | O(1)                | O(n)
 *   ArrayDeque   | brak get(i)  | O(1) amortyzowane | O(1) amortyzowane   | O(n)
 *   HashSet/Map  | O(1) amortyzowane (get/put/contains), brak kolejności
 *   TreeSet/Map  | O(log n) (get/put/contains), ZAWSZE posortowane
 *
 * SŁÓWKA:
 *   amortized = zamortyzowany (średni koszt w długim okresie, mimo że pojedyncza operacja bywa droższa);
 *   shift = przesunięcie (elementu w tablicy); hop = skok (po wskaźniku next w liście wiązanej);
 *   growth factor = współczynnik wzrostu; initial capacity = pojemność początkowa; warm-up = rozgrzewka (JIT).
 *
 * ZOBACZ TEŻ: t12_collections/Collections02Lists (ArrayList vs LinkedList od strony API), t12_collections/
 *             Collections11HashingInternals (dlaczego HashMap/HashSet są średnio O(1)), t24_algorithms/
 *             Algorithms01Complexity (notacja Big-O od podstaw), t26_jvm/Jvm04ToolsProfiling (profilowanie, JMH).
 * </pre>
 */
public class Collections13Performance {

    public static void main(String[] args) {
        title("Collections13 — wydajność kolekcji: Big-O, policzone operacje, pułapki");

        bigOTable();                    // big o table = tabela złożoności
        containsEqualsCount();          // contains equals count = contains: policzone equals()
        countedShiftsForRemoveFirst();  // counted shifts for remove first = policzone przesunięcia przy remove(0)
        containsInLoopPitfall();        // contains in loop pitfall = pułapka contains() w pętli
        linkedListGetInLoopPitfall();   // linked list get in loop pitfall = pułapka LinkedList.get(i) w pętli
        removingFromFrontPitfall();     // removing from front pitfall = pułapka usuwania z przodu ArrayList
        initialCapacityMatters();       // initial capacity matters = pojemność początkowa ma znaczenie
        jmhMention();                   // jmh mention = wzmianka o JMH
        exercises();                     // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. TABELA ZŁOŻONOŚCI (Big-O)
    // =================================================================================================

    /** 1. Ściąga złożoności — pełne wyjaśnienia i dowody (policzone, nie zmierzone) w kolejnych sekcjach. */
    static void bigOTable() {
        section("1. Big-O: tabela złożoności najważniejszych operacji");

        note("Struktura      | get(i)        | dodaj/usuń KONIEC     | dodaj/usuń POCZĄTEK   | contains");
        note("ArrayList      | O(1)          | O(1) amortyzowane     | O(n) (przesunięcie)   | O(n)");
        note("LinkedList     | O(n)          | O(1)                  | O(1)                  | O(n)");
        note("ArrayDeque     | brak get(i)   | O(1) amortyzowane     | O(1) amortyzowane     | O(n)");
        note("HashSet        | —             | O(1) amortyzowane add/remove/contains, BRAK kolejności");
        note("TreeSet        | —             | O(log n) add/remove/contains, ZAWSZE posortowany");
        note("HashMap        | —             | O(1) amortyzowane get/put/remove/containsKey, BRAK kolejności");
        note("TreeMap        | —             | O(log n) get/put/remove/containsKey, ZAWSZE posortowana po kluczu");

        note("\"Amortyzowane\" = ŚREDNIO, w długim okresie. Pojedynczy ArrayList.add na końcu bywa O(n) (gdy");
        note("tablica wewnętrzna musi urosnąć — sekcja 7), ale takie sytuacje są na tyle rzadkie, że ŚREDNI koszt");
        note("N kolejnych add to wciąż O(1) na operację, nie O(n).");
    }

    // =================================================================================================
    // 2. POLICZONE equals(): List.contains KONTRA HashSet.contains
    // =================================================================================================

    /** CountedValue = int z DOBRYM hashCode (nie stałym jak BadKey w Collections11) — liczy tylko wywołania equals(). */
    static final class CountedValue {
        static int equalsCalls = 0;

        final int value;

        CountedValue(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            equalsCalls++;
            return o instanceof CountedValue other && other.value == value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }
    }

    /** 2. Ten sam element, ta sama liczba danych — ale zupełnie inna liczba porównań equals(). */
    static void containsEqualsCount() {
        section("2. Policzone equals(): List.contains kontra HashSet.contains");

        List<CountedValue> list = new ArrayList<>();
        Set<CountedValue> set = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            list.add(new CountedValue(i));
            set.add(new CountedValue(i));
        }

        CountedValue target = new CountedValue(99_999);   // wartość, której NA PEWNO nie ma w kolekcji

        CountedValue.equalsCalls = 0;
        boolean inList = list.contains(target);
        show("list.contains(brakujący element) — wynik", inList);
        show("equals() wywołane przez List.contains (1000 elementów)", CountedValue.equalsCalls);
        // WYNIK: list.contains(brakujący element) — wynik → false
        // WYNIK: equals() wywołane przez List.contains (1000 elementów) → 1000

        CountedValue.equalsCalls = 0;
        boolean inSet = set.contains(target);
        show("set.contains(brakujący element) — wynik", inSet);
        show("equals() wywołane przez HashSet.contains (1000 elementów)", CountedValue.equalsCalls);
        // WYNIK: set.contains(brakujący element) — wynik → false
        // WYNIK: equals() wywołane przez HashSet.contains (1000 elementów) → 0

        note("List.contains MUSI przejrzeć WSZYSTKIE 1000 elementów, żeby stwierdzić \"na pewno go tu nie ma\" — O(n).");
        note("HashSet.contains policzył hashCode targetu, poszedł WPROST do właściwego kubełka (Collections11) —");
        note("okazał się pusty, więc equals() w ogóle nie był potrzebny (0 wywołań) — praktycznie O(1).");
    }

    // =================================================================================================
    // 3. POLICZONE PRZESUNIĘCIA: remove(0) NA TABLICY (JAK W ArrayList)
    // =================================================================================================

    /**
     * Symulujemy TYLKO mechanizm przesunięć (to, co ArrayList.remove(0) robi wewnątrz przez System.arraycopy) —
     * ta metoda to pomoc dydaktyczna, nie część API kursu.
     */
    static int shiftsForRemoveFirst(int size) {
        Integer[] array = new Integer[size];
        for (int i = 0; i < size; i++) {
            array[i] = i;
        }
        int shifts = 0;
        for (int i = 1; i < size; i++) {
            array[i - 1] = array[i];   // dokładnie to, co ArrayList.remove(0) robi w środku
            shifts++;
        }
        return shifts;
    }

    /** 3. Usunięcie PIERWSZEGO elementu z tablicy o n elementach wymaga n-1 przesunięć pozostałych — O(n). */
    static void countedShiftsForRemoveFirst() {
        section("3. Policzone przesunięcia: remove(0) na tablicy (tak jak w ArrayList)");

        show("przesunięcia przy remove(0) z tablicy 1000 elementów", shiftsForRemoveFirst(1000));
        // WYNIK: przesunięcia przy remove(0) z tablicy 1000 elementów → 999

        show("przesunięcia przy remove(0) z tablicy 10 elementów", shiftsForRemoveFirst(10));
        // WYNIK: przesunięcia przy remove(0) z tablicy 10 elementów → 9

        note("ArrayList.remove(0) robi WEWNĄTRZ dokładnie to samo: każdy element o indeksie > 0 przesuwa się o");
        note("jedno miejsce w lewo (System.arraycopy). Dla n elementów to n-1 przesunięć — O(n). remove(size-1)");
        note("(usunięcie OSTATNIEGO elementu) nie przesuwa NIC — O(1), bo nie ma czego przesuwać.");
    }

    // =================================================================================================
    // 4. PUŁAPKA: contains() W PĘTLI
    // =================================================================================================

    /** 4. Sprawdzanie przynależności WIELU elementów do dużej listy w pętli — klasyczne O(n²) w przebraniu. */
    static void containsInLoopPitfall() {
        section("4. Pułapka: contains() w pętli — O(n²) na liście, prawie O(n) na secie");

        List<CountedValue> haystackList = new ArrayList<>();
        Set<CountedValue> haystackSet = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            haystackList.add(new CountedValue(i));
            haystackSet.add(new CountedValue(i));
        }

        List<CountedValue> needles = new ArrayList<>();
        for (int i = 150; i < 200; i++) {
            needles.add(new CountedValue(i));   // 50 wartości, obecne, ale bliżej KOŃCA listy (gorszy przypadek)
        }

        CountedValue.equalsCalls = 0;
        int foundInList = 0;
        for (CountedValue needle : needles) {
            if (haystackList.contains(needle)) {
                foundInList++;
            }
        }
        show("znaleziono (haystackList, 200 elementów, 50 zapytań)", foundInList);
        show("equals() łącznie: 50× List.contains w pętli", CountedValue.equalsCalls);
        // WYNIK: znaleziono (haystackList, 200 elementów, 50 zapytań) → 50
        // WYNIK: equals() łącznie: 50× List.contains w pętli → 8775

        CountedValue.equalsCalls = 0;
        int foundInSet = 0;
        for (CountedValue needle : needles) {
            if (haystackSet.contains(needle)) {
                foundInSet++;
            }
        }
        show("znaleziono (haystackSet, 200 elementów, 50 zapytań)", foundInSet);
        show("equals() łącznie: 50× HashSet.contains w pętli", CountedValue.equalsCalls);
        // WYNIK: znaleziono (haystackSet, 200 elementów, 50 zapytań) → 50
        // WYNIK: equals() łącznie: 50× HashSet.contains w pętli → 50

        // PUŁAPKA: "dla każdego needle sprawdź, czy jest w haystack" wygląda niewinnie, ale gdy haystack to List,
        //   KAŻDE wywołanie contains to osobne przejście O(n) — łącznie O(needles × haystack). 8775 kontra 50
        //   equals() dla tych samych danych to nie przypadek, to właśnie różnica O(n²) kontra ~O(n).
        // DOBRA PRAKTYKA: jeśli wielokrotnie pytasz "czy X jest w tym zbiorze danych" — zbuduj HashSet RAZ
        //   (poza pętlą pytań), zamiast trzymać dane jako List i wywoływać contains w kółko.
    }

    // =================================================================================================
    // 5. PUŁAPKA: LinkedList.get(i) W PĘTLI PO INDEKSIE
    // =================================================================================================

    /** Node = pojedynczy węzeł jednokierunkowej listy — symulacja mechanizmu, po to by POLICZYĆ skoki po next. */
    static final class Node {
        final int value;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    static Node buildChain(int n) {
        Node head = new Node(0);
        Node current = head;
        for (int i = 1; i < n; i++) {
            current.next = new Node(i);
            current = current.next;
        }
        return head;
    }

    static int hopsForGet(Node head, int index) {
        int hops = 0;
        Node current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
            hops++;
        }
        return hops;
    }

    /** 5. get(i) na liście wiązanej wymaga i "skoków" od głowy. Wywołane dla WSZYSTKICH indeksów w pętli — O(n²). */
    static void linkedListGetInLoopPitfall() {
        section("5. Pułapka: LinkedList.get(i) w pętli po indeksie — O(n²)");

        int n = 500;
        Node head = buildChain(n);

        int totalHops = 0;
        for (int i = 0; i < n; i++) {
            totalHops += hopsForGet(head, i);
        }
        show("suma skoków po next dla get(0)..get(" + (n - 1) + ") — jak w LinkedList", totalHops);
        // WYNIK: suma skoków po next dla get(0)..get(499) — jak w LinkedList → 124750

        int arrayEquivalentSteps = n;   // ArrayList.get(i) to O(1) — n wywołań to n kroków, nie n²
        show("dla porównania: ArrayList.get(i) w tej samej pętli — kroków łącznie", arrayEquivalentSteps);
        // WYNIK: dla porównania: ArrayList.get(i) w tej samej pętli — kroków łącznie → 500

        note("Prawdziwy java.util.LinkedList.get(i) jest odrobinę mądrzejszy — zaczyna od BLIŻSZEGO końca (head");
        note("albo tail), więc pojedyncze wywołanie kosztuje średnio n/4, nie n/2 jak nasza uproszczona symulacja.");
        note("Ale wywoływane W PĘTLI po kolejnych indeksach i tak sumuje się do rzędu n² (tylko ze stałą 2 razy");
        note("mniejszą) — wniosek jest identyczny: get(i) w pętli po indeksie na LinkedList to zawsze O(n²).");

        // PUŁAPKA: "for (int i = 0; i < list.size(); i++) { ... list.get(i) ... }" wygląda identycznie dla
        //   ArrayList i LinkedList, ale dla LinkedList to katastrofa wydajnościowa przy dużych listach. Lekarstwo:
        //   for-each (używa Iteratora, O(1) na krok, O(n) łącznie) albo po prostu ArrayList do dostępu po indeksie
        //   (Collections02Lists: "LinkedList rzadko jest właściwym wyborem").
    }

    // =================================================================================================
    // 6. PUŁAPKA: USUWANIE Z PRZODU ArrayList W PĘTLI
    // =================================================================================================

    /** 6. Usunięcie WSZYSTKICH elementów z przodu, jeden po drugim, sumuje przesunięcia do rzędu n²/2. */
    static void removingFromFrontPitfall() {
        section("6. Pułapka: usuwanie z PRZODU ArrayList w pętli — O(n²) → ArrayDeque");

        int n = 300;
        int totalShifts = 0;
        int remaining = n;
        while (remaining > 1) {
            totalShifts += remaining - 1;   // remove(0) z listy o "remaining" elementach przesuwa remaining-1 elementów
            remaining--;
        }
        show("suma przesunięć: usunięcie WSZYSTKICH " + n + " elementów z przodu ArrayList, jeden po drugim", totalShifts);
        // WYNIK: suma przesunięć: usunięcie WSZYSTKICH 300 elementów z przodu ArrayList, jeden po drugim → 44850

        int dequeOperations = n;   // ArrayDeque.pollFirst() to O(1) — n usunięć to n kroków, nie n²
        show("dla porównania: to samo przez ArrayDeque.pollFirst() w pętli — kroków łącznie", dequeOperations);
        // WYNIK: dla porównania: to samo przez ArrayDeque.pollFirst() w pętli — kroków łącznie → 300

        note("44850 przesunięć kontra 300 kroków dla DOKŁADNIE tego samego zadania (\"usuń wszystko od przodu\") —");
        note("to nie drobna różnica wydajności, to różnica RZĘDU WIELKOŚCI (O(n²) kontra O(n)). Dla n = 300 to");
        note("już ~150x więcej pracy; dla n = 100 000 różnica byłaby rzędu ~50 000x.");

        // DOBRA PRAKTYKA: potrzebujesz dodawać/usuwać na OBU końcach (kolejka, stos, historia undo)? Użyj
        //   ArrayDeque, nie ArrayList i nie LinkedList (Collections06QueuesDeques) — ciągła tablica cykliczna ma
        //   lepszą lokalność pamięci niż węzły rozrzucone po stercie, więc ArrayDeque bywa szybszy nawet od LinkedList.
    }

    // =================================================================================================
    // 7. POCZĄTKOWA POJEMNOŚĆ (initial capacity)
    // =================================================================================================

    /** Symuluje wzór wzrostu ArrayList: newCapacity = oldCapacity + oldCapacity/2 (czyli ok. razy 1.5). */
    static int countArrayListGrows(int startCapacity, int targetSize) {
        int capacity = startCapacity;
        int grows = 0;
        while (capacity < targetSize) {
            capacity += capacity / 2;
            grows++;
        }
        return grows;
    }

    /** 7. Znajomy z góry docelowy rozmiar? Podanie initial capacity oszczędza wiele kosztownych realokacji. */
    static void initialCapacityMatters() {
        section("7. Początkowa pojemność (initial capacity) — mniej realokacji");

        int growsDefault = countArrayListGrows(10, 10_000);       // domyślna startowa pojemność ArrayList to 10
        int growsPresized = countArrayListGrows(10_000, 10_000);  // z góry podana pojemność docelowa

        show("realokacje: new ArrayList<>() (start 10) rosnąca do 10 000 elementów", growsDefault);
        show("realokacje: new ArrayList<>(10_000) rosnąca do 10 000 elementów", growsPresized);
        // WYNIK: realokacje: new ArrayList<>() (start 10) rosnąca do 10 000 elementów → 18
        // WYNIK: realokacje: new ArrayList<>(10_000) rosnąca do 10 000 elementów → 0

        note("Każda realokacja to: nowa, większa tablica + skopiowanie WSZYSTKICH dotychczasowych elementów");
        note("(Arrays.copyOf) — to koszt O(bieżący rozmiar), nie stały. 18 takich kopiowań \"po drodze\" do 10 000");
        note("elementów to realna, mierzalna praca, całkowicie unikniona przez new ArrayList<>(10_000).");

        // DOBRA PRAKTYKA: znasz w przybliżeniu docelowy rozmiar? new ArrayList<>(oczekiwanyRozmiar) albo
        //   new HashMap<>(oczekiwanaLiczbaKluczy) (Collections11: HashMap też rośnie skokowo, próg 0.75).
        //   To mikrooptymalizacja — nie warto jej robić "na wszelki wypadek" wszędzie, ale w gorących pętlach
        //   budujących duże kolekcje bywa zauważalna.
    }

    // =================================================================================================
    // 8. A CO Z PRAWDZIWYM POMIAREM CZASU? — JMH
    // =================================================================================================

    /** 8. Dlaczego w całej lekcji liczyliśmy operacje, a nie mierzyliśmy czasu stoperem. */
    static void jmhMention() {
        section("8. A co z prawdziwym pomiarem czasu? — JMH");

        note("W tej lekcji celowo NIGDY nie mierzyliśmy czasu (System.currentTimeMillis/nanoTime) — mikrobenchmarki");
        note("pisane \"na piechotę\" w Javie są ZWODNICZE: JIT kompiluje kod \"na gorąco\" dopiero po wielu");
        note("powtórzeniach (warm-up), Garbage Collector może uruchomić się akurat w trakcie pomiaru, a martwy");
        note("kod bywa przez JIT całkiem USUNIĘTY (dead code elimination), jeśli wynik nigdzie się nie \"liczy\".");
        note("Stąd LICZENIE OPERACJI (equals, przesunięcia, skoki) w tej lekcji — daje wiarygodny, w 100%");
        note("powtarzalny obraz złożoności, bez żadnej z tych pułapek.");
        note("Do PRAWDZIWYCH pomiarów czasu służy JMH (Java Microbenchmark Harness, projekt OpenJDK) — sam robi");
        note("warm-up, wiele iteracji i izolację od dead-code-elimination. Profilowanie na żywo: t26_jvm/Jvm04ToolsProfiling.");
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • ArrayList: get O(1), contains O(n), add/remove KONIEC O(1) amort., add/remove POCZĄTEK O(n) (przesunięcie).
     *   • LinkedList: get O(n) (skoki po next), add/remove NA KOŃCACH O(1), contains O(n).
     *   • ArrayDeque: brak get(i), add/remove na OBU końcach O(1) amort. — najlepszy wybór jako stos/kolejka.
     *   • HashSet/HashMap: O(1) amortyzowane (dobry hashCode!), brak kolejności (Collections11).
     *   • TreeSet/TreeMap: O(log n), zawsze posortowane.
     *   • contains() w pętli po Liście = O(n²) — zbuduj HashSet RAZ, zamiast pytać List w kółko.
     *   • get(i) w pętli po indeksie na LinkedList = O(n²) — użyj for-each albo ArrayList.
     *   • Usuwanie z przodu ArrayList w pętli = O(n²) — użyj ArrayDeque.
     *   • Nie mierzymy czasu stoperem (JIT/GC zakłócają pomiar) — do prawdziwych benchmarków służy JMH.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego w tej lekcji liczymy operacje (equals, przesunięcia, skoki), zamiast mierzyć czas stoperem
     *      (System.currentTimeMillis)?
     *   2. Co wypisze:  System.out.println(shiftsForRemoveFirst(1));  (metoda z sekcji 3)?
     *   3. ZNAJDŹ BŁĄD (wydajnościowy):
     *          List<String> big = new LinkedList<>();
     *          // ... dodano 100 000 elementów
     *          int sum = 0;
     *          for (int i = 0; i < big.size(); i++) {
     *              sum += big.get(i).length();
     *          }
     *   4. Co wypisze:  System.out.println(500 * 499 / 2);  (ten sam wzór co suma skoków w sekcji 5, dla n = 500)?
     *   5. Dlaczego ArrayList.remove(list.size() - 1) (usunięcie OSTATNIEGO elementu) jest O(1), a remove(0) — O(n)?
     *   6. Dlaczego HashSet.contains jest średnio O(1), a List.contains zawsze O(n), skoro obie metody
     *      "sprawdzają, czy element jest w kolekcji"?
     *   7. Po co podawać initial capacity (np. new ArrayList<>(10_000)), skoro ArrayList i tak sam urośnie w razie potrzeby?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: przesunięcia przy remove(index) z tablicy 10-elementowej", 9, () -> exercise1(10, 0));
        Check.equal("ćw. 1b: przesunięcia przy remove(ostatni indeks)", 0, () -> exercise1(10, 9));
        Check.equal("ćw. 2: suma skoków get(0)..get(9) na łańcuchu 10 węzłów", 45, () -> exercise2(10));
        Check.equal("ćw. 3: PRZEPISZ contains-w-pętli na HashSet", 3,
                () -> exercise3(List.of(1, 2, 3, 4, 5), List.of(2, 4, 6, 5)));
        Check.equal("ćw. 4: liczba realokacji ArrayList (10 → 22)", 2, () -> exercise4(10, 22));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 9, () -> solution1(10, 0));
        Check.equal("ćw. 1b (wzorzec)", 0, () -> solution1(10, 9));
        Check.equal("ćw. 2 (wzorzec)", 45, () -> solution2(10));
        Check.equal("ćw. 3 (wzorzec)", 3, () -> solution3(List.of(1, 2, 3, 4, 5), List.of(2, 4, 6, 5)));
        Check.equal("ćw. 4 (wzorzec)", 2, () -> solution4(10, 22));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć liczbę przesunięć potrzebnych do usunięcia elementu o podanym indeksie z tablicy
     * o podanym rozmiarze (jak przy ArrayList.remove(index)). Podpowiedź: przesuwają się tylko elementy PO
     * usuwanym indeksie — wzór: size - 1 - index.
     */
    static int exercise1(int size, int index) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 2 (łatwe): zbuduj łańcuch n węzłów (buildChain) i zwróć SUMĘ skoków (hopsForGet) dla
     * get(0), get(1), ..., get(n-1) — czyli policz to samo, co sekcja 5, ale dla mniejszego n, w pętli.
     */
    static int exercise2(int n) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ): poniższy kod sprawdza, ile elementów z needles jest w haystack, wywołując
     * haystack.contains(...) w pętli — dla dużego haystack to O(needles × haystack):
     * <pre>{@code
     * int count = 0;
     * for (Integer needle : needles) {
     *     if (haystack.contains(needle)) {   // haystack to List<Integer> — O(n) za KAŻDYM razem
     *         count++;
     *     }
     * }
     * }</pre>
     * Przepisz tak, by najpierw zbudować HashSet z haystack (RAZ), a dopiero potem pytać go w pętli. Zwróć liczbę
     * trafień. Podpowiedź: new HashSet<>(haystack) poza pętlą.
     */
    static int exercise3(List<Integer> haystack, List<Integer> needles) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): odtwórz countArrayListGrows z sekcji 7 — zwróć, ile razy pojemność wzrośnie
     * (wzór 1.5x: capacity += capacity / 2), zaczynając od startCapacity, aż osiągnie/przekroczy targetSize.
     */
    static int exercise4(int startCapacity, int targetSize) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int size, int index) {
        return size - 1 - index;
    }

    static int solution2(int n) {
        Node head = buildChain(n);
        int total = 0;
        for (int i = 0; i < n; i++) {
            total += hopsForGet(head, i);
        }
        return total;
    }

    static int solution3(List<Integer> haystack, List<Integer> needles) {
        Set<Integer> haystackSet = new HashSet<>(haystack);   // budujemy RAZ, poza pętlą pytań
        int count = 0;
        for (Integer needle : needles) {
            if (haystackSet.contains(needle)) {
                count++;
            }
        }
        return count;
    }

    static int solution4(int startCapacity, int targetSize) {
        return countArrayListGrows(startCapacity, targetSize);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo pomiar czasu w Javie zależy od JIT (warm-up), Garbage Collectora i optymalizacji typu dead-code
     *      elimination — te same 10 linii kodu może dać zupełnie różne czasy przy dwóch uruchomieniach. Liczba
     *      operacji (equals, przesunięcia, skoki) jest DETERMINISTYCZNA i powtarzalna, więc lepiej pokazuje
     *      rzeczywistą złożoność algorytmu.
     *   2. „0” — tablica jednoelementowa, nie ma nic do przesunięcia po usunięciu jedynego elementu.
     *   3. LinkedList.get(i) w pętli po indeksie to O(n²) (n wywołań get, każde O(n) — sekcja 5). Naprawa: for-each
     *      (for (String s : big)) albo ListIterator — obie opcje kosztują O(n) łącznie, nie O(n²).
     *   4. „124750” — dokładnie ta sama liczba, co suma skoków w sekcji 5 (to ten sam wzór: n*(n-1)/2 dla n=500).
     *   5. remove(size - 1) usuwa element z SAMEGO KOŃCA — nic po nim nie ma, więc nie ma czego przesuwać (O(1)).
     *      remove(0) usuwa PIERWSZY element — wszystkie pozostałe size-1 elementów muszą przesunąć się o jedno
     *      miejsce w lewo, żeby nie zostawić "dziury" (O(n)).
     *   6. List.contains sprawdza równość PO KOLEI z każdym elementem, bo nie ma żadnej wskazówki, gdzie szukać —
     *      musi przejrzeć wszystko (O(n)). HashSet.contains liczy hashCode szukanego elementu i idzie WPROST do
     *      właściwego kubełka (Collections11) — sprawdza tylko garstkę elementów w TYM kubełku, nie całą kolekcję.
     *   7. Bez initial capacity ArrayList rośnie stopniowo (1.5x za każdym razem, sekcja 7), co oznacza WIELE
     *      kosztownych realokacji (nowa tablica + skopiowanie wszystkiego) po drodze do docelowego rozmiaru. Gdy
     *      znasz docelowy rozmiar z góry, jedna od razu odpowiednio duża tablica oszczędza tę powtarzalną pracę.
     */
    // </editor-fold>
}
