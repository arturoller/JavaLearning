package t12_collections;

import helpers.Check;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Queue, Deque, PriorityQueue — kolejki, stosy, kolejki priorytetowe
 *        (queue = kolejka; deque = kolejka dwustronna (double-ended queue); FIFO = pierwszy wszedł, pierwszy
 *        wyszedł; LIFO = ostatni wszedł, pierwszy wyszedł; priority = priorytet)
 *
 * W SKRÓCIE:
 *   Queue to interfejs kolejki (FIFO). Deque (kolejka DWUSTRONNA) dodaje/usuwa na OBU końcach — dzięki temu
 *   jedna klasa, ArrayDeque, świetnie gra i kolejkę (FIFO), i stos (LIFO, push/pop). PriorityQueue to
 *   kolejka, w której poll() zawsze zwraca NAJMNIEJSZY (albo — z Comparatorem — „najważniejszy”) element,
 *   niezależnie od kolejności wstawiania.
 *
 * ANALOGIA:
 *   Queue (FIFO) to kolejka w sklepie — kto stanął pierwszy, jest obsłużony pierwszy. Deque używana jako
 *   stos (LIFO) to stos talerzy — kładziesz na wierzch (push) i zdejmujesz z wierzchu (pop); talerz na dnie
 *   czeka najdłużej. PriorityQueue to izba przyjęć na ostrym dyżurze — kolejność WEJŚCIA nie ma znaczenia,
 *   obsługiwany jest zawsze najpilniejszy przypadek.
 *
 * JAK TO DZIAŁA:
 *   Queue ma DWA komplety metod o tym samym znaczeniu, różniące się zachowaniem przy pustej/pełnej kolejce:
 *                    zwraca specjalną wartość      rzuca wyjątek
 *     wstaw:         offer(e) → false               add(e) → IllegalStateException (kolejka pełna, rzadkie)
 *     usuń i zwróć:  poll() → null                   remove() → NoSuchElementException (kolejka pusta)
 *     podejrzyj:     peek() → null                   element() → NoSuchElementException (kolejka pusta)
 *   Deque jako stos: push(e) = addFirst(e); pop() = removeFirst(); peek() = peekFirst().
 *   PriorityQueue: wewnątrz to KOPIEC (binary heap) — tablica, NIE lista posortowana. Kolejność jest
 *   gwarantowana TYLKO przy pollowaniu, nie przy iteracji (sekcja 6).
 *
 * SŁÓWKA:
 *   FIFO = pierwszy wszedł, pierwszy wyszedł; LIFO = ostatni wszedł, pierwszy wyszedł; offer = zaproponuj
 *   (dodaj, zwróć false zamiast wyjątku); poll = pobierz (usuń i zwróć, albo null); peek = zerknij
 *   (podejrzyj bez usuwania); push = odłóż na stos; pop = zdejmij ze stosu; priority = priorytet;
 *   heap = kopiec (struktura danych).
 *
 * ZOBACZ TEŻ: t12_collections/Collections01Overview (tabela implementacji), t12_collections/Collections02Lists
 *             (dlaczego ArrayDeque, nie LinkedList), t12_collections/Collections07ComparableComparator
 *             (Comparator dla PriorityQueue), t12_collections/Collections10Patterns (top-N, okno przesuwne
 *             z ArrayDeque), t24_algorithms (kolejki priorytetowe w algorytmach grafowych).
 * </pre>
 */
public class Collections06QueuesDeques {

    public static void main(String[] args) {
        title("Collections06 — Queue, Deque, PriorityQueue");

        queueMethodPairs();        // offer/poll/peek vs add/remove/element
        arrayDequeAsQueue();       // ArrayDeque jako kolejka FIFO
        arrayDequeAsStack();       // ArrayDeque jako stos LIFO
        priorityQueueNatural();    // PriorityQueue: naturalny porządek
        priorityQueueComparator(); // PriorityQueue: Comparator (zadania wg priorytetu)
        priorityQueueIterationPitfall();   // pułapka: iteracja ≠ kolejność priorytetu
        undoHistoryExample();      // przykład: historia cofania (undo)
        whichQueue();              // podsumowanie: co wybrać
        exercises();                // ćwiczenia
    }

    // =================================================================================================
    // 1. Queue: DWA komplety metod — specjalna wartość kontra wyjątek
    // =================================================================================================

    /**
     * 1. offer/poll/peek NIGDY nie rzucają na pustej/pełnej kolejce (zwracają false/null).
     * add/remove/element rzucają wyjątek w tej samej sytuacji. Wybór zależy od tego, czy pusta kolejka
     * to „normalna sytuacja” (offer/poll/peek), czy błąd programu (add/remove/element).
     */
    static void queueMethodPairs() {
        section("1. Queue: offer/poll/peek kontra add/remove/element");

        Queue<String> empty = new ArrayDeque<>();
        show("poll() na pustej kolejce", empty.poll());
        // WYNIK: poll() na pustej kolejce → null
        show("peek() na pustej kolejce", empty.peek());
        // WYNIK: peek() na pustej kolejce → null
        show("offer(\"a\") — zawsze się udaje (kolejka nieograniczona)", empty.offer("a"));
        // WYNIK: offer("a") — zawsze się udaje (kolejka nieograniczona) → true

        expectThrows("remove() na pustej kolejce", () -> new ArrayDeque<String>().remove());
        // WYNIK: ✔ remove() na pustej kolejce → rzucono NoSuchElementException: (brak komunikatu)
        expectThrows("element() na pustej kolejce", () -> new ArrayDeque<String>().element());
        // WYNIK: ✔ element() na pustej kolejce → rzucono NoSuchElementException: (brak komunikatu)

        // DOBRA PRAKTYKA: w pętli „przetwarzaj, dopóki coś jest” używaj poll() (zwraca null na końcu —
        //   naturalny warunek stopu). remove()/element() są dla sytuacji, w których pusta kolejka to
        //   BŁĄD programu, a nie normalne zakończenie pracy.
    }

    // =================================================================================================
    // 2. ArrayDeque jako Queue — FIFO
    // =================================================================================================

    /** 2. offer dodaje na KONIEC, poll usuwa i zwraca z POCZĄTKU (czoła) — klasyczna kolejka FIFO. */
    static void arrayDequeAsQueue() {
        section("2. ArrayDeque jako kolejka FIFO");

        Queue<String> tasks = new ArrayDeque<>();   // tasks = zadania
        tasks.offer("zadanie1");
        tasks.offer("zadanie2");
        tasks.offer("zadanie3");

        show("peek() (podgląd czoła, bez usuwania)", tasks.peek());
        // WYNIK: peek() (podgląd czoła, bez usuwania) → zadanie1
        show("poll() (pobierz i usuń z czoła)", tasks.poll());
        // WYNIK: poll() (pobierz i usuń z czoła) → zadanie1
        show("kolejka po jednym poll()", tasks);
        // WYNIK: kolejka po jednym poll() → [zadanie2, zadanie3]
        show("poll()", tasks.poll());
        // WYNIK: poll() → zadanie2
        show("rozmiar na końcu", tasks.size());
        // WYNIK: rozmiar na końcu → 1
    }

    // =================================================================================================
    // 3. ArrayDeque jako Stack — LIFO (push/pop)
    // =================================================================================================

    /** 3. push dodaje na POCZĄTEK (wierzch stosu), pop usuwa i zwraca z POCZĄTKU — klasyczny stos LIFO. */
    static void arrayDequeAsStack() {
        section("3. ArrayDeque jako stos LIFO (push, pop)");

        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(1);
        stack.push(2);
        stack.push(3);   // push = addFirst — każdy kolejny ląduje NA WIERZCHU

        show("stos po push(1), push(2), push(3)", stack);
        // WYNIK: stos po push(1), push(2), push(3) → [3, 2, 1]
        show("pop() (zdejmij z wierzchu)", stack.pop());
        // WYNIK: pop() (zdejmij z wierzchu) → 3
        show("peek() (podgląd wierzchu, bez zdejmowania)", stack.peek());
        // WYNIK: peek() (podgląd wierzchu, bez zdejmowania) → 2
        show("stos na końcu", stack);
        // WYNIK: stos na końcu → [2, 1]

        // DOBRA PRAKTYKA: ArrayDeque zamiast java.util.Stack (klasa legacy z 1998 — dziedziczy po Vector,
        //   jest zsynchronizowana, więc wolniejsza niż to konieczne) i zamiast LinkedList (więcej pamięci
        //   na węzeł, gorsza lokalność w pamięci podręcznej procesora). ArrayDeque jest zalecanym wyborem
        //   zarówno jako kolejka, jak i jako stos.
    }

    // =================================================================================================
    // 4. PriorityQueue — NATURALNY PORZĄDEK
    // =================================================================================================

    /** 4. poll() zawsze zwraca NAJMNIEJSZY element (wg naturalnego porządku), niezależnie od kolejności offer(). */
    static void priorityQueueNatural() {
        section("4. PriorityQueue: naturalny porządek");

        Queue<Integer> pq = new PriorityQueue<>();
        pq.offer(5);
        pq.offer(1);
        pq.offer(3);
        pq.offer(2);
        pq.offer(4);

        show("peek() (najmniejszy — ale NIE usuwa)", pq.peek());
        // WYNIK: peek() (najmniejszy — ale NIE usuwa) → 1

        StringBuilder order = new StringBuilder();
        while (!pq.isEmpty()) {
            order.append(pq.poll()).append(" ");
        }
        show("kolejność pollowania (rosnąco)", order.toString().trim());
        // WYNIK: kolejność pollowania (rosnąco) → 1 2 3 4 5
    }

    // =================================================================================================
    // 5. PriorityQueue z Comparatorem — ZADANIA WG PRIORYTETU
    // =================================================================================================

    /** Task = zadanie. Niższa liczba priority = zadanie PILNIEJSZE (konwencja jak w wielu systemach kolejkowych). */
    private record Task(String name, int priority) {
    }

    /** 5. Comparator.comparingInt daje PriorityQueue własną regułę porównywania — pełne omówienie: Collections07. */
    static void priorityQueueComparator() {
        section("5. PriorityQueue z Comparatorem: zadania wg priorytetu");

        Queue<Task> taskQueue = new PriorityQueue<>(Comparator.comparingInt(Task::priority));
        taskQueue.offer(new Task("wyślij fakturę", 3));
        taskQueue.offer(new Task("napraw awarię", 1));
        taskQueue.offer(new Task("odpowiedz na mail", 2));

        StringBuilder execOrder = new StringBuilder();
        while (!taskQueue.isEmpty()) {
            execOrder.append(taskQueue.poll().name()).append(" | ");
        }
        show("kolejność wykonania (1 = najpilniejsze)", execOrder.toString());
        // WYNIK: kolejność wykonania (1 = najpilniejsze) → napraw awarię | odpowiedz na mail | wyślij fakturę |
    }

    // =================================================================================================
    // 6. PUŁAPKA: iteracja po PriorityQueue ≠ kolejność priorytetu
    // =================================================================================================

    /**
     * 6. PriorityQueue trzyma elementy w TABLICY zorganizowanej jako kopiec (heap) — ta struktura gwarantuje
     * szybkie poll() najmniejszego elementu, ale NIE jest posortowana jako całość. for-each/iterator
     * przechodzi po tablicy w jej WEWNĘTRZNYM porządku, nie w porządku priorytetu.
     */
    static void priorityQueueIterationPitfall() {
        section("6. Pułapka: iteracja po PriorityQueue nie daje kolejności priorytetu");

        Queue<Integer> pq = new PriorityQueue<>(List.of(5, 1, 3, 2, 4));

        StringBuilder viaIteration = new StringBuilder();
        for (Integer n : pq) {
            viaIteration.append(n).append(" ");
        }
        show("for-each po PriorityQueue (WEWNĘTRZNY porządek kopca!)", viaIteration.toString().trim());
        // WYNIK: for-each po PriorityQueue (WEWNĘTRZNY porządek kopca!) → 1 2 3 5 4

        StringBuilder viaPoll = new StringBuilder();
        while (!pq.isEmpty()) {
            viaPoll.append(pq.poll()).append(" ");
        }
        show("poll() w pętli (POPRAWNIE posortowana)", viaPoll.toString().trim());
        // WYNIK: poll() w pętli (POPRAWNIE posortowana) → 1 2 3 4 5

        // PUŁAPKA: powyżej „5” wypadło PRZED „4” przy iteracji — to nie błąd, to jak działa kopiec.
        //   Jedyny poprawny sposób odczytania elementów PriorityQueue W KOLEJNOŚCI priorytetu to
        //   wielokrotne poll() (co oczywiście OPRÓŻNIA kolejkę — jeśli potrzebujesz zachować jej
        //   zawartość, pollój kopię: new PriorityQueue<>(oryginal)).
    }

    // =================================================================================================
    // 7. PRZYKŁAD: historia cofania (undo)
    // =================================================================================================

    /** 7. Stos (ArrayDeque + push/pop) to naturalna struktura do „cofnij ostatnią akcję” (undo). */
    static void undoHistoryExample() {
        section("7. Przykład: historia cofania (undo)");

        Deque<String> undoHistory = new ArrayDeque<>();   // undo history = historia cofania
        undoHistory.push("napisano: Cześć");
        undoHistory.push("napisano: Cześć, jak się masz?");
        undoHistory.push("pogrubiono tekst");

        show("historia (od najnowszej akcji)", undoHistory);
        // WYNIK: historia (od najnowszej akcji) → [pogrubiono tekst, napisano: Cześć, jak się masz?, napisano: Cześć]

        show("undo (pop)", undoHistory.pop());
        // WYNIK: undo (pop) → pogrubiono tekst
        show("kolejne undo (pop)", undoHistory.pop());
        // WYNIK: kolejne undo (pop) → napisano: Cześć, jak się masz?
        show("co zostało w historii", undoHistory);
        // WYNIK: co zostało w historii → [napisano: Cześć]

        // JAK TO DZIAŁA: każda akcja użytkownika trafia na wierzch stosu (push). „Cofnij” to pop() —
        //   zdejmuje NAJNOWSZĄ akcję. To dokładnie odwrotna kolejność niż wykonywanie (LIFO) — i dokładnie
        //   tego oczekujemy od Ctrl+Z.
    }

    // =================================================================================================
    // 8. PODSUMOWANIE: CO WYBRAĆ?
    // =================================================================================================

    private record QueueChoice(String need, String recommendation) {
    }

    private static final List<QueueChoice> QUEUE_GUIDE = List.of(
            new QueueChoice("FIFO — kto pierwszy, ten pierwszy (kolejka zadań)", "ArrayDeque jako Queue"),
            new QueueChoice("LIFO — ostatni wchodzi, pierwszy wychodzi (stos, undo)", "ArrayDeque jako Deque (push/pop)"),
            new QueueChoice("zawsze potrzebny NAJMNIEJSZY/NAJWIĘKSZY element", "PriorityQueue"),
            new QueueChoice("kod z lat 90. używający Stack albo LinkedList jako kolejki", "przepisz na ArrayDeque (legacy)")
    );

    /** 8. Krótki przewodnik decyzyjny — podsumowanie całej lekcji. */
    static void whichQueue() {
        section("8. Podsumowanie: którą strukturę wybrać?");

        for (QueueChoice c : QUEUE_GUIDE) {
            System.out.println("  " + c.need() + " → " + c.recommendation());
        }
        // WYNIK:   FIFO — kto pierwszy, ten pierwszy (kolejka zadań) → ArrayDeque jako Queue
        // WYNIK:   LIFO — ostatni wchodzi, pierwszy wychodzi (stos, undo) → ArrayDeque jako Deque (push/pop)
        // WYNIK:   zawsze potrzebny NAJMNIEJSZY/NAJWIĘKSZY element → PriorityQueue
        // WYNIK:   kod z lat 90. używający Stack albo LinkedList jako kolejki → przepisz na ArrayDeque (legacy)
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • offer/poll/peek: NIGDY nie rzucają — zwracają false/null na pustej/pełnej kolejce.
     *   • add/remove/element: rzucają wyjątek (IllegalStateException / NoSuchElementException) w tej samej
     *     sytuacji — używaj, gdy pusta kolejka to BŁĄD, nie normalna sytuacja.
     *   • ArrayDeque jako Queue: offer = na koniec, poll = z czoła (FIFO).
     *   • ArrayDeque jako Deque/stos: push = addFirst, pop/peek = removeFirst/peekFirst (LIFO).
     *   • ArrayDeque > LinkedList i > java.util.Stack jako implementacja kolejki/stosu (szybszy, mniej
     *     narzutu pamięciowego, bez zbędnej synchronizacji).
     *   • PriorityQueue: poll() zawsze zwraca najmniejszy (albo wg Comparatora) element. Iteracja
     *     (for-each) NIE jest posortowana — to wewnętrzna tablica kopca. Sortowanie = wielokrotne poll().
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się poll() od remove() na pustej kolejce?
     *   2. Co wypisze:
     *          Deque<Integer> d = new ArrayDeque<>();
     *          d.push(1);
     *          d.push(2);
     *          System.out.println(d.pop() + " " + d.peek());
     *      ?
     *   3. ZNAJDŹ BŁĄD (programista oczekuje wypisania 1 2 3 4 5):
     *          Queue<Integer> pq = new PriorityQueue<>(List.of(5, 3, 1, 4, 2));
     *          for (Integer n : pq) {
     *              System.out.print(n + " ");
     *          }
     *   4. Co wypisze:
     *          Queue<String> pq = new PriorityQueue<>();
     *          pq.offer("banan");
     *          pq.offer("ananas");
     *          pq.offer("czereśnia");
     *          System.out.println(pq.poll());
     *      ?
     *   5. Dlaczego ArrayDeque jest preferowany zamiast java.util.Stack i zamiast LinkedList jako
     *      implementacja stosu/kolejki?
     *   6. Jak NAPRAWDĘ pobrać elementy z PriorityQueue w kolejności priorytetu, skoro iteracja for-each
     *      tego nie gwarantuje?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: przetwórz FIFO", "a -> b -> c", () -> processFifo(List.of("a", "b", "c")));
        Check.equal("ćw. 2a: nawiasy zbalansowane", true, () -> isBalanced("(a[b]{c})"));
        Check.equal("ćw. 2b: nawiasy NIEzbalansowane", false, () -> isBalanced("(a[b)]"));
        Check.equal("ćw. 3: zadania wg priorytetu (PriorityQueue+Comparator)", List.of("A", "B", "C"),
                () -> processInPriorityOrder(List.of(new Task("C", 3), new Task("A", 1), new Task("B", 2))));
        Check.equal("ćw. 4: sortowanie przez PriorityQueue (PRZEPISZ)", List.of(1, 2, 3, 4, 5),
                () -> sortViaPriorityQueue(List.of(5, 1, 4, 2, 3)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "a -> b -> c", () -> solutionProcessFifo(List.of("a", "b", "c")));
        Check.equal("ćw. 2a (wzorzec)", true, () -> solutionIsBalanced("(a[b]{c})"));
        Check.equal("ćw. 2b (wzorzec)", false, () -> solutionIsBalanced("(a[b)]"));
        Check.equal("ćw. 3 (wzorzec)", List.of("A", "B", "C"), () -> solutionProcessInPriorityOrder(
                List.of(new Task("C", 3), new Task("A", 1), new Task("B", 2))));
        Check.equal("ćw. 4 (wzorzec)", List.of(1, 2, 3, 4, 5), () -> solutionSortViaPriorityQueue(List.of(5, 1, 4, 2, 3)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): przetwórz elementy w kolejności FIFO, zwracając je połączone przez {@code " -> "}.
     * Podpowiedź: {@code Queue<String> q = new ArrayDeque<>(items)}; pętla poll() aż do isEmpty().
     */
    static String processFifo(List<String> items) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (średnie): sprawdź, czy nawiasy ({@code ( ) [ ] { }}) w tekście s są poprawnie zbalansowane
     * (każdy otwierający ma swój zamykający, we właściwej kolejności). Podpowiedź: stos (ArrayDeque):
     * otwierający → push, zamykający → pop i porównaj typ; na końcu stos musi być pusty.
     */
    static boolean isBalanced(String s) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć nazwy zadań w kolejności wykonania (rosnąco wg priority — 1 pierwsze).
     * Podpowiedź: PriorityQueue z Comparator.comparingInt(Task::priority), potem pętla poll() — sekcja 5.
     */
    static List<String> processInPriorityOrder(List<Task> tasks) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze, PRZEPISZ): poniższy kod sortuje przez wielokrotne szukanie minimum w liście
     * (to w istocie sortowanie przez wybieranie, O(n²)):
     * <pre>{@code
     * List<Integer> copy = new ArrayList<>(numbers);
     * List<Integer> sorted = new ArrayList<>();
     * while (!copy.isEmpty()) {
     *     int minIndex = 0;
     *     for (int i = 1; i < copy.size(); i++) {
     *         if (copy.get(i) < copy.get(minIndex)) {
     *             minIndex = i;
     *         }
     *     }
     *     sorted.add(copy.remove(minIndex));
     * }
     * return sorted;
     * }</pre>
     * Przepisz to używając PriorityQueue: {@code new PriorityQueue<>(numbers)}, potem pętla poll() aż do
     * wyczerpania — kopiec sam znajduje minimum szybciej niż liniowe przeszukanie za każdym razem.
     */
    static List<Integer> sortViaPriorityQueue(List<Integer> numbers) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solutionProcessFifo(List<String> items) {
        Queue<String> queue = new ArrayDeque<>(items);
        StringBuilder sb = new StringBuilder();
        while (!queue.isEmpty()) {
            sb.append(queue.poll());
            if (!queue.isEmpty()) {
                sb.append(" -> ");
            }
        }
        return sb.toString();
    }

    static boolean solutionIsBalanced(String s) {
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            if (c == '(' || c == '[' || c == '{') {
                stack.push(c);
            } else if (c == ')' || c == ']' || c == '}') {
                if (stack.isEmpty()) {
                    return false;
                }
                char open = stack.pop();
                boolean matches = (c == ')' && open == '(') || (c == ']' && open == '[') || (c == '}' && open == '{');
                if (!matches) {
                    return false;
                }
            }
        }
        return stack.isEmpty();
    }

    static List<String> solutionProcessInPriorityOrder(List<Task> tasks) {
        Queue<Task> pq = new PriorityQueue<>(Comparator.comparingInt(Task::priority));
        pq.addAll(tasks);
        List<String> result = new ArrayList<>();
        while (!pq.isEmpty()) {
            result.add(pq.poll().name());
        }
        return result;
    }

    static List<Integer> solutionSortViaPriorityQueue(List<Integer> numbers) {
        Queue<Integer> pq = new PriorityQueue<>(numbers);
        List<Integer> sorted = new ArrayList<>();
        while (!pq.isEmpty()) {
            sorted.add(pq.poll());
        }
        return sorted;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. poll() na pustej kolejce zwraca null. remove() na pustej kolejce rzuca NoSuchElementException.
     *      Ten sam sens („pobierz i usuń”), różne zachowanie przy braku elementu.
     *   2. „2 1” — push(1) → [1]; push(2) → [2, 1] (2 na wierzchu). pop() zdejmuje i zwraca 2, zostaje [1].
     *      peek() pokazuje nowy wierzch: 1. Razem: "2 1".
     *   3. PriorityQueue nie jest posortowaną listą — to kopiec (tablica). for-each iteruje WEWNĘTRZNY
     *      układ tablicy, nie kolejność priorytetu. Żeby dostać posortowaną kolejność, trzeba wielokrotnie
     *      wywołać poll() (co opróżnia kolejkę).
     *   4. „ananas” — naturalny porządek Stringów to porównanie alfabetyczne (Unicode): "ananas" < "banan"
     *      < "czereśnia". poll() ZAWSZE zwraca najmniejszy element, niezależnie od wewnętrznego układu kopca.
     *   5. ArrayDeque jest szybszy niż LinkedList (tablica cykliczna, lepsza lokalność pamięci, brak
     *      narzutu na węzły z dwoma wskaźnikami) i szybszy niż java.util.Stack (klasa legacy, dziedziczy
     *      po zsynchronizowanym Vector — niepotrzebna synchronizacja w kodzie jednowątkowym).
     *   6. Wielokrotnie wywołując poll() w pętli aż do isEmpty() — każde poll() zwraca kolejny najmniejszy
     *      pozostały element. Jeśli trzeba zachować oryginalną kolejkę, pollować należy jej KOPIĘ
     *      (new PriorityQueue<>(oryginal)).
     */
    // </editor-fold>
}
