package t24_algorithms;

import helpers.Check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Struktury danych napisane ręcznie — jak działają pod spodem klasy z JDK
 *        (data structure = struktura danych; node = węzeł; bucket = kubełek; sift up/down = przesiej w górę/w dół)
 *
 * W SKRÓCIE:
 *   ArrayList, LinkedList, ArrayDeque, PriorityQueue, TreeMap, HashMap to gotowe narzędzia — ale WARTO wiedzieć,
 *   jak działają w środku. W tej lekcji budujemy uproszczone wersje sześciu klasycznych struktur i mierzymy
 *   ich złożoność, żeby świadomie wybierać gotową klasę z JDK w prawdziwym kodzie.
 *
 * ANALOGIA: samochód a silnik.
 *   Na co dzień wystarczy umieć prowadzić (używać ArrayList). Ale żeby zrozumieć, DLACZEGO coś jest wolne
 *   albo szybkie, warto raz zajrzeć pod maskę i zobaczyć silnik (własnoręczną implementację).
 *
 * JAK TO DZIAŁA:
 *   Każda struktura to inny kompromis: tablica = szybki dostęp po indeksie, wolne wstawianie w środku;
 *   lista wiązana = szybkie wstawianie na początku, wolny dostęp po indeksie; kopiec = szybki dostęp do
 *   minimum/maksimum; drzewo BST = szybkie wyszukiwanie, ALE tylko gdy jest zbalansowane; tablica haszująca =
 *   szybki dostęp po kluczu kosztem dodatkowej pamięci i kolizji.
 *
 * SŁÓWKA:
 *   node = węzeł; head = głowa (pierwszy węzeł); bucket = kubełek; chaining = łańcuchowanie (kolizji);
 *   sift up/down = przesiej w górę/w dół; circular buffer = bufor cykliczny; balanced = zbalansowany;
 *   skewed = zdegenerowany (przekrzywiony); collision = kolizja.
 *
 * ZOBACZ TEŻ: t24_algorithms/Algorithms01Complexity (notacja O()),
 *             t12_collections/Collections11HashingInternals (HashMap od środka, pełny obraz),
 *             t12_collections/Collections06QueuesDeques (ArrayDeque, PriorityQueue w praktyce).
 * </pre>
 */
public class Algorithms04DataStructures {

    public static void main(String[] args) {
        title("Algorithms04 — struktury danych napisane ręcznie");

        dynamicArrayDemo();     // dynamic array = tablica dynamiczna
        linkedListDemo();       // linked list = lista wiązana
        stackAndQueueDemo();    // stack/queue = stos/kolejka
        binaryHeapDemo();       // binary heap = kopiec binarny
        binarySearchTreeDemo(); // binary search tree = drzewo poszukiwań binarnych
        hashTableDemo();        // hash table = tablica haszująca
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. TABLICA DYNAMICZNA (DYNAMIC ARRAY, ROŚNIE ×2)
    // =================================================================================================

    /** DynamicArray = tablica, która SAMA powiększa się ×2, gdy zabraknie miejsca (tak jak ArrayList). */
    static class DynamicArray {
        private int[] data = new int[1];
        private int size = 0;

        int size() { return size; }

        int get(int index) { // O(1)
            if (index < 0 || index >= size) throw new IndexOutOfBoundsException("indeks " + index + " przy rozmiarze " + size);
            return data[index];
        }

        void add(int value) { // O(1) zamortyzowane (Algorithms01Complexity, sekcja 10)
            if (size == data.length) data = Arrays.copyOf(data, data.length * 2);
            data[size++] = value;
        }

        void insertAt(int index, int value) { // O(n) — trzeba przesunąć resztę w prawo
            if (size == data.length) data = Arrays.copyOf(data, data.length * 2);
            for (int i = size; i > index; i--) { data[i] = data[i - 1]; }
            data[index] = value;
            size++;
        }

        String snapshot() { return Arrays.toString(Arrays.copyOf(data, size)); }
    }

    /** 1. get po indeksie: O(1). add na końcu: O(1) zamortyzowane. insertAt w środku: O(n) (przesunięcie). */
    static void dynamicArrayDemo() {
        section("1. Tablica dynamiczna (dynamic array)");

        DynamicArray arr = new DynamicArray();
        for (int value = 1; value <= 5; value++) { arr.add(value); }
        show("po pięciu add", arr.snapshot());
        show("get(2)", arr.get(2));
        arr.insertAt(0, 99);
        show("po insertAt(0, 99)", arr.snapshot());
        // WYNIK: po pięciu add → [1, 2, 3, 4, 5]
        // WYNIK: get(2) → 3
        // WYNIK: po insertAt(0, 99) → [99, 1, 2, 3, 4, 5]

        note("w prawdziwym kodzie: java.util.ArrayList<Integer> — identyczna idea (tablica rosnąca ×1,5), gotowa i przetestowana");
    }

    // =================================================================================================
    // 2. LISTA JEDNOKIERUNKOWA (SINGLY LINKED LIST)
    // =================================================================================================

    static class Node {
        int value;
        Node next;

        Node(int value) { this.value = value; }
    }

    /** SinglyLinkedList = łańcuch węzłów; każdy węzeł zna TYLKO swojego następcę (brak wskaźnika wstecz). */
    static class SinglyLinkedList {
        Node head;

        void addFirst(int value) { // O(1)
            Node node = new Node(value);
            node.next = head;
            head = node;
        }

        void addLast(int value) { // O(n) — bez wskaźnika na ogon trzeba dojść do końca
            Node node = new Node(value);
            if (head == null) { head = node; return; }
            Node current = head;
            while (current.next != null) { current = current.next; }
            current.next = node;
        }

        boolean remove(int value) { // O(n)
            if (head == null) return false;
            if (head.value == value) { head = head.next; return true; }
            Node current = head;
            while (current.next != null) {
                if (current.next.value == value) { current.next = current.next.next; return true; }
                current = current.next;
            }
            return false;
        }

        /** reverse = odwróć kierunek wszystkich wskaźników next, iteracyjnie. (PYTANIE REKRUTACYJNE: klasyk.) */
        void reverse() { // O(n)
            Node previous = null;
            Node current = head;
            while (current != null) {
                Node next = current.next;
                current.next = previous;
                previous = current;
                current = next;
            }
            head = previous;
        }

        String snapshot() {
            StringBuilder sb = new StringBuilder("[");
            for (Node current = head; current != null; current = current.next) {
                sb.append(current.value);
                if (current.next != null) sb.append(", ");
            }
            return sb.append("]").toString();
        }
    }

    /**
     * 2. addFirst: O(1) (tylko przepina head). addLast/remove: O(n) (trzeba dojść do właściwego miejsca).
     * reverse: O(n), bez dodatkowej pamięci — tylko przepinamy wskaźniki next w drugą stronę.
     */
    static void linkedListDemo() {
        section("2. Lista jednokierunkowa (singly linked list)");

        SinglyLinkedList list = new SinglyLinkedList();
        list.addLast(1);
        list.addLast(2);
        list.addLast(3);
        list.addFirst(0);
        show("po addLast(1,2,3) i addFirst(0)", list.snapshot());
        list.remove(2);
        show("po remove(2)", list.snapshot());
        list.reverse();
        show("po reverse()", list.snapshot());
        // WYNIK: po addLast(1,2,3) i addFirst(0) → [0, 1, 2, 3]
        // WYNIK: po remove(2) → [0, 1, 3]
        // WYNIK: po reverse() → [3, 1, 0]

        note("java.util.LinkedList to lista DWUKIERUNKOWA z pamiętanym ogonem — dlatego JEJ addLast jest O(1), nie O(n) jak tutaj");

        // PUŁAPKA: ta uproszczona wersja NIE pamięta ogona (tail) — dlatego addLast kosztuje O(n). Prawdziwa
        //   java.util.LinkedList trzyma wskaźnik na ostatni węzeł właśnie po to, by addLast było O(1).
    }

    // =================================================================================================
    // 3. STOS I KOLEJKA NA TABLICY (BUFOR CYKLICZNY)
    // =================================================================================================

    /** ArrayStack = stos (LIFO) na rosnącej tablicy — push/pop na KOŃCU tablicy. */
    static class ArrayStack {
        private int[] data = new int[1];
        private int size = 0;

        void push(int value) { // O(1) zamortyzowane
            if (size == data.length) data = Arrays.copyOf(data, data.length * 2);
            data[size++] = value;
        }

        int pop() { // O(1)
            if (size == 0) throw new NoSuchElementException("pusty stos");
            return data[--size];
        }

        int peek() { // O(1)
            if (size == 0) throw new NoSuchElementException("pusty stos");
            return data[size - 1];
        }
    }

    /** CircularQueue = kolejka (FIFO) na STAŁEJ tablicy; front i rear "zawijają się" na początek (bufor cykliczny). */
    static class CircularQueue {
        private final int[] data;
        private int front = 0;
        private int rear = 0;
        private int size = 0;

        CircularQueue(int capacity) { data = new int[capacity]; }

        void enqueue(int value) { // O(1)
            if (size == data.length) throw new IllegalStateException("kolejka pełna");
            data[rear] = value;
            rear = (rear + 1) % data.length; // zawinięcie na początek tablicy
            size++;
        }

        int dequeue() { // O(1)
            if (size == 0) throw new NoSuchElementException("pusta kolejka");
            int value = data[front];
            front = (front + 1) % data.length;
            size--;
            return value;
        }
    }

    /**
     * 3. Stos: push/pop na końcu tablicy, oba O(1). Kolejka: bez bufora cyklicznego dequeue z przodu
     * kosztowałoby O(n) (przesunięcie reszty) — zawijanie front/rear modulo pojemności daje O(1).
     */
    static void stackAndQueueDemo() {
        section("3. Stos i kolejka na tablicy (bufor cykliczny)");

        ArrayStack stack = new ArrayStack();
        stack.push(1);
        stack.push(2);
        stack.push(3);
        show("stos: pop()", stack.pop());
        show("stos: peek()", stack.peek());
        // WYNIK: stos: pop() → 3
        // WYNIK: stos: peek() → 2

        CircularQueue queue = new CircularQueue(4);
        queue.enqueue(1);
        queue.enqueue(2);
        queue.enqueue(3);
        queue.dequeue(); // zwalnia miejsce na początku bufora
        queue.enqueue(4);
        queue.enqueue(5); // "zawija się" na zwolnione miejsce
        StringBuilder order = new StringBuilder();
        while (true) {
            try {
                order.append(queue.dequeue()).append(" ");
            } catch (NoSuchElementException e) {
                break;
            }
        }
        show("kolejność opróżniania kolejki (capacity=4, z zawinięciem)", order.toString().trim());
        // WYNIK: kolejność opróżniania kolejki (capacity=4, z zawinięciem) → 2 3 4 5

        note("java.util.ArrayDeque jest WŁAŚNIE buforem cyklicznym pod spodem — dlatego jest szybsza niż Stack/LinkedList jako stos i kolejka");
    }

    // =================================================================================================
    // 4. KOPIEC BINARNY (BINARY HEAP, MIN-HEAP)
    // =================================================================================================

    /** MinHeap = tablica, w której KAŻDY rodzic jest ≤ swoich dzieci — korzeń (indeks 0) to zawsze minimum. */
    static class MinHeap {
        private int[] data = new int[1];
        private int size = 0;

        void insert(int value) { // O(log n)
            if (size == data.length) data = Arrays.copyOf(data, data.length * 2);
            data[size] = value;
            siftUp(size);
            size++;
        }

        /** siftUp = dopóki dziecko jest MNIEJSZE od rodzica, zamieniaj je miejscami (idź w górę drzewa). */
        private void siftUp(int i) {
            while (i > 0) {
                int parent = (i - 1) / 2;
                if (data[parent] <= data[i]) break;
                swap(parent, i);
                i = parent;
            }
        }

        int extractMin() { // O(log n)
            if (size == 0) throw new NoSuchElementException("pusty kopiec");
            int min = data[0];
            size--;
            data[0] = data[size];
            siftDown(0);
            return min;
        }

        /** siftDown = dopóki jakieś dziecko jest MNIEJSZE od rodzica, zamień z mniejszym z nich (idź w dół). */
        private void siftDown(int i) {
            while (true) {
                int left = 2 * i + 1, right = 2 * i + 2, smallest = i;
                if (left < size && data[left] < data[smallest]) smallest = left;
                if (right < size && data[right] < data[smallest]) smallest = right;
                if (smallest == i) break;
                swap(i, smallest);
                i = smallest;
            }
        }

        private void swap(int a, int b) {
            int tmp = data[a];
            data[a] = data[b];
            data[b] = tmp;
        }

        int size() { return size; }
    }

    /**
     * 4. insert: dopisz na koniec, potem "przesiej w górę" (siftUp) — O(log n), bo wysokość kopca to log2(n).
     * extractMin: zabierz korzeń, wstaw na jego miejsce OSTATNI element, "przesiej w dół" (siftDown) — O(log n).
     * Powtarzane extractMin zwraca elementy w kolejności ROSNĄCEJ — to podstawa sortowania przez kopcowanie.
     */
    static void binaryHeapDemo() {
        section("4. Kopiec binarny (binary heap, min-heap)");

        MinHeap heap = new MinHeap();
        for (int value : new int[] {5, 2, 8, 1, 9, 3}) { heap.insert(value); }
        StringBuilder sorted = new StringBuilder();
        while (heap.size() > 0) { sorted.append(heap.extractMin()).append(" "); }
        show("kolejne extractMin (zawsze rosnąco)", sorted.toString().trim());
        // WYNIK: kolejne extractMin (zawsze rosnąco) → 1 2 3 5 8 9

        note("java.util.PriorityQueue to DOKŁADNIE ta sama struktura (tablicowy kopiec) — poll() zawsze zwraca najmniejszy");
    }

    // =================================================================================================
    // 5. DRZEWO POSZUKIWAŃ BINARNYCH (BINARY SEARCH TREE)
    // =================================================================================================

    static class TreeNode {
        int value;
        TreeNode left, right;

        TreeNode(int value) { this.value = value; }
    }

    /** BinarySearchTree = w każdym węźle: lewe poddrzewo MA SAME mniejsze wartości, prawe — same większe. */
    static class BinarySearchTree {
        TreeNode root;

        void insert(int value) { root = insertRec(root, value); }

        private TreeNode insertRec(TreeNode node, int value) {
            if (node == null) return new TreeNode(value);
            if (value < node.value) node.left = insertRec(node.left, value);
            else if (value > node.value) node.right = insertRec(node.right, value);
            return node;
        }

        boolean contains(int value) {
            TreeNode current = root;
            while (current != null) {
                if (value == current.value) return true;
                current = (value < current.value) ? current.left : current.right;
            }
            return false;
        }

        List<Integer> inOrder() {
            List<Integer> result = new ArrayList<>();
            inOrderRec(root, result);
            return result;
        }

        private void inOrderRec(TreeNode node, List<Integer> acc) {
            if (node == null) return;
            inOrderRec(node.left, acc);
            acc.add(node.value);
            inOrderRec(node.right, acc);
        }

        int height() { return heightRec(root); }

        private int heightRec(TreeNode node) {
            return (node == null) ? 0 : 1 + Math.max(heightRec(node.left), heightRec(node.right));
        }
    }

    /**
     * 5. insert/contains: schodzimy w lewo albo w prawo zależnie od porównania — O(wysokość drzewa).
     * in-order (lewo, węzeł, prawo) zawsze daje elementy w kolejności ROSNĄCEJ — to dowód poprawności struktury.
     * Gdy drzewo jest ZBALANSOWANE, wysokość ≈ log2(n) → O(log n). Gdy wstawiamy dane JUŻ posortowane,
     * drzewo degeneruje się do listy (każdy węzeł ma tylko prawe dziecko) → wysokość = n → O(n), najgorszy przypadek.
     */
    static void binarySearchTreeDemo() {
        section("5. Drzewo poszukiwań binarnych (binary search tree)");

        BinarySearchTree balanced = new BinarySearchTree();
        for (int value : new int[] {5, 2, 8, 1, 3, 7, 9}) { balanced.insert(value); }
        show("in-order (zawsze posortowane)", balanced.inOrder());
        show("contains(7)", balanced.contains(7));
        show("contains(4)", balanced.contains(4));
        show("wysokość drzewa (7 elementów, wstawianych w dobrej kolejności)", balanced.height());
        // WYNIK: in-order (zawsze posortowane) → [1, 2, 3, 5, 7, 8, 9]
        // WYNIK: contains(7) → true
        // WYNIK: contains(4) → false
        // WYNIK: wysokość drzewa (7 elementów, wstawianych w dobrej kolejności) → 3

        BinarySearchTree skewed = new BinarySearchTree();
        for (int value = 1; value <= 7; value++) { skewed.insert(value); }
        show("wysokość drzewa (te same 7 elementów, ale JUŻ posortowane)", skewed.height());
        // WYNIK: wysokość drzewa (te same 7 elementów, ale JUŻ posortowane) → 7

        note("te same 7 elementów: wysokość 3 (zbalansowane) kontra 7 (zdegenerowane do listy) — ogromna różnica dla dużego n");

        // PUŁAPKA: wstawianie JUŻ POSORTOWANYCH danych do naiwnego BST daje najgorszy przypadek O(n).
        // DOBRA PRAKTYKA: java.util.TreeMap/TreeSet używają drzewa CZERWONO-CZARNEGO, które SAMO się
        //   balansuje przy wstawianiu — gwarantują O(log n) zawsze, niezależnie od kolejności danych.
    }

    // =================================================================================================
    // 6. TABLICA HASZUJĄCA Z ŁAŃCUCHOWANIEM (HASH TABLE, CHAINING)
    // =================================================================================================

    static class Entry {
        String key;
        int value;
        Entry next;

        Entry(String key, int value, Entry next) { this.key = key; this.value = value; this.next = next; }
    }

    /**
     * HashTableChaining = tablica "kubełków"; każdy klucz trafia do kubełka wg hashCode() % pojemność.
     * Dwa różne klucze mogą trafić do TEGO SAMEGO kubełka (kolizja) — wtedy tworzą listę (łańcuchowanie).
     * (PYTANIE REKRUTACYJNE: zaimplementuj prostą tablicę haszującą z obsługą kolizji).
     */
    static class HashTableChaining {
        private final Entry[] buckets;

        HashTableChaining(int capacity) { buckets = new Entry[capacity]; }

        private int bucketIndex(String key) {
            return Math.floorMod(key.hashCode(), buckets.length); // floorMod = zawsze nieujemny wynik
        }

        void put(String key, int value) { // średnio O(1), najgorzej O(n) przy samych kolizjach
            int idx = bucketIndex(key);
            for (Entry e = buckets[idx]; e != null; e = e.next) {
                if (e.key.equals(key)) { e.value = value; return; }
            }
            buckets[idx] = new Entry(key, value, buckets[idx]);
        }

        Integer get(String key) { // średnio O(1)
            for (Entry e = buckets[bucketIndex(key)]; e != null; e = e.next) {
                if (e.key.equals(key)) return e.value;
            }
            return null;
        }
    }

    /** 6. Mała pojemność (4) na 5 kluczy CELOWO wymusza przynajmniej jedną kolizję — widać łańcuchowanie w akcji. */
    static void hashTableDemo() {
        section("6. Tablica haszująca z łańcuchowaniem (hash table, chaining)");

        HashTableChaining table = new HashTableChaining(4);
        String[] words = {"java", "stream", "lambda", "kolekcja", "mapa"};
        for (String word : words) { table.put(word, word.length()); }
        show("get(\"java\")", table.get("java"));
        show("get(\"kolekcja\")", table.get("kolekcja"));
        show("get(\"brak\") — klucza nie ma", table.get("brak"));
        // WYNIK: get("java") → 4
        // WYNIK: get("kolekcja") → 8
        // WYNIK: get("brak") — klucza nie ma → null

        note("java.util.HashMap robi dokładnie to samo (hashCode → kubełek → łańcuch), plus automatyczne powiększanie"
                + " pojemności i zamianę długiego łańcucha na drzewo (treeify) — pełny obraz: Collections11HashingInternals");
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Tablica dynamiczna: get O(1), add (koniec) O(1) zamortyzowane, insert (środek) O(n). JDK: ArrayList.
     *   • Lista jednokierunkowa: addFirst O(1), addLast/remove/szukanie O(n). JDK: LinkedList (dwukierunkowa, z ogonem).
     *   • Stos/kolejka na tablicy: wszystkie operacje O(1) (bufor cykliczny). JDK: ArrayDeque.
     *   • Kopiec binarny: insert/extractMin O(log n), podgląd minimum O(1). JDK: PriorityQueue.
     *   • BST: O(log n) średnio, O(n) w najgorszym razie (dane posortowane = zdegenerowane drzewo). JDK: TreeMap/TreeSet
     *     (samobalansujące, zawsze O(log n)).
     *   • Tablica haszująca: O(1) średnio, O(n) przy samych kolizjach. JDK: HashMap/HashSet.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego addFirst na liście jednokierunkowej jest O(1), a addLast (bez wskaźnika na ogon) — O(n)?
     *   2. Co wypisze:  MinHeap h = new MinHeap(); h.insert(3); h.insert(1); h.insert(2);
     *      System.out.println(h.extractMin());  ?
     *   3. ZNAJDŹ BŁĄD: ktoś wstawia do naiwnego BST dane w kolejności 1, 2, 3, 4, 5 i dziwi się, że
     *      wyszukiwanie jest wolne (O(n) zamiast O(log n)). Co poszło nie tak?
     *   4. Dlaczego java.util.ArrayDeque jest zalecany zamiast java.util.Stack jako stos?
     *   5. Co to jest kolizja w tablicy haszującej i jak łańcuchowanie sobie z nią radzi?
     *   6. ZNAJDŹ BŁĄD: ktoś twierdzi, że TreeMap ma taką samą złożoność najgorszego przypadku jak
     *      ręcznie pisane BST z tej lekcji. Czy to prawda?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: DynamicArray po dodaniu 1..4 i get(1)", 2, () -> exercise1());
        Check.equal("ćw. 2: SinglyLinkedList odwrócona po addLast(1,2,3)", "[3, 2, 1]", () -> exercise2());
        Check.equal("ćw. 3: trzy najmniejsze z MinHeap dla [9,4,7,1,8,2]", "1 2 4", () -> exercise3());
        Check.equal("ćw. 4: wysokość BST po wstawieniu 4,2,6,1,3,5,7 (zbalansowane)", 3, () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 2, () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", "[3, 2, 1]", () -> solution2());
        Check.equal("ćw. 3 (wzorzec)", "1 2 4", () -> solution3());
        Check.equal("ćw. 4 (wzorzec)", 3, () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): utwórz DynamicArray, dodaj kolejno 1, 2, 3, 4 i zwróć get(1).
     * Podpowiedź: nowy obiekt DynamicArray, cztery wywołania add, potem get.
     */
    static int exercise1() {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (łatwe): utwórz SinglyLinkedList, dodaj addLast(1), addLast(2), addLast(3), odwróć ją
     * (reverse) i zwróć snapshot().
     * Podpowiedź: addLast × 3, potem reverse(), potem snapshot().
     */
    static String exercise2() {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 3 (średnie): wstaw do MinHeap wartości 9, 4, 7, 1, 8, 2 i zwróć TRZY najmniejsze (po jednej
     * spacji, bez spacji na końcu), wyciągając je kolejno przez extractMin.
     * Podpowiedź: trzy wywołania extractMin do StringBuilder, trim na końcu.
     */
    static String exercise3() {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): wstaw do BinarySearchTree wartości w kolejności 4, 2, 6, 1, 3, 5, 7
     * (celowo "od środka" — tak, by drzewo wyszło zbalansowane) i zwróć height().
     * Podpowiedź: siedem insert w podanej kolejności, potem height().
     */
    static int exercise4() {
        // TODO: twoje rozwiązanie
        return -1;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1() {
        DynamicArray arr = new DynamicArray();
        arr.add(1);
        arr.add(2);
        arr.add(3);
        arr.add(4);
        return arr.get(1);
    }

    static String solution2() {
        SinglyLinkedList list = new SinglyLinkedList();
        list.addLast(1);
        list.addLast(2);
        list.addLast(3);
        list.reverse();
        return list.snapshot();
    }

    static String solution3() {
        MinHeap heap = new MinHeap();
        for (int value : new int[] {9, 4, 7, 1, 8, 2}) { heap.insert(value); }
        return heap.extractMin() + " " + heap.extractMin() + " " + heap.extractMin();
    }

    static int solution4() {
        BinarySearchTree tree = new BinarySearchTree();
        for (int value : new int[] {4, 2, 6, 1, 3, 5, 7}) { tree.insert(value); }
        return tree.height();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. addFirst tylko przepina head na nowy węzeł (stała liczba operacji). addLast bez zapamiętanego
     *      ogona musi PRZEJŚĆ całą listę, by znaleźć ostatni węzeł — to O(n).
     *   2. 1 (extractMin zawsze zwraca aktualnie najmniejszy element kopca).
     *   3. Dane JUŻ posortowane sprawiają, że każdy nowy element trafia jako prawe dziecko poprzedniego —
     *      drzewo degeneruje się do listy jednokierunkowej, wysokość = n zamiast log n.
     *   4. java.util.Stack jest przestarzały (dziedziczy po Vector, jest zsynchronizowany — niepotrzebny
     *      koszt) — ArrayDeque jest szybszy i to właśnie jego zaleca dokumentacja JDK.
     *   5. Kolizja to sytuacja, gdy dwa różne klucze dają ten sam indeks kubełka. Łańcuchowanie radzi sobie,
     *      trzymając w kubełku LISTĘ wszystkich trafiających tam par klucz-wartość zamiast jednej.
     *   6. Nie — TreeMap/TreeSet są SAMOBALANSUJĄCE (drzewo czerwono-czarne), więc gwarantują O(log n) ZAWSZE,
     *      nawet dla posortowanych danych wejściowych. Nasze proste BST w takim przypadku degeneruje się do O(n).
     */
    // </editor-fold>
}
