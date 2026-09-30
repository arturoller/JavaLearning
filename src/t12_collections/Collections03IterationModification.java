package t12_collections;

import helpers.Check;
import helpers.SampleData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Iterowanie i modyfikacja — for-each, Iterator, ListIterator
 *        (iterate = iterować/przechodzić po elementach; modification = modyfikacja)
 *
 * W SKRÓCIE:
 *   for-each to wygodny skrót, ale NIE POZWALA bezpiecznie usuwać elementów w trakcie iterowania —
 *   próba kończy się ConcurrentModificationException. Iterator (i jego rozszerzenie ListIterator) daje
 *   pełną kontrolę: hasNext/next do odczytu, remove/set/add do bezpiecznej modyfikacji W TRAKCIE iterowania.
 *
 * ANALOGIA: kontroler biletów idący przez pociąg wagon po wagonie.
 *   for-each to pasażer, który tylko PATRZY przez okno — nie może nic zmienić w pociągu. Iterator to
 *   konduktor: idzie po kolei (hasNext/next), i JEŻELI usunie pasażera (remove()), robi to w sposób, który
 *   nie gubi rachunku, kto już był sprawdzony. Gdyby ktoś POZA konduktorem (inny wagon, inny kod) zaczął
 *   przestawiać pasażerów w trakcie kontroli — konduktor by się pogubił: to właśnie
 *   ConcurrentModificationException.
 *
 * JAK TO DZIAŁA:
 *   for (String s : list) { ... }        ≈  Iterator<String> it = list.iterator();
 *                                            while (it.hasNext()) { String s = it.next(); ... }
 *   Każda ArrayList ma modCount (licznik zmian strukturalnych). Iterator zapamiętuje modCount z chwili
 *   startu. Jeśli next() wykryje, że modCount się zmienił (ktoś zmienił rozmiar listy Z POMINIĘCIEM
 *   iteratora) — rzuca ConcurrentModificationException. it.remove()/it.add() same AKTUALIZUJĄ modCount
 *   iteratora, więc są bezpieczne.
 *
 * SŁÓWKA:
 *   iterate = iterować; hasNext = czy jest następny; next = następny (pobierz i przesuń kursor);
 *   remove = usuń (przez iterator); structural modification = zmiana strukturalna (dodanie/usunięcie);
 *   backwards = wstecz; cursor = kursor (pozycja między elementami).
 *
 * ZOBACZ TEŻ: t12_collections/Collections02Lists (subList i ConcurrentModificationException tam),
 *             t12_collections/Collections04Sets, t12_collections/Collections05Maps (iteracja po mapach),
 *             t13_lambdas/Lambda04MethodReferences (lambdy i referencje do metod — pełne omówienie).
 * </pre>
 */
public class Collections03IterationModification {

    public static void main(String[] args) {
        title("Collections03 — iterowanie i modyfikacja: for-each, Iterator, ListIterator");

        forEachBasics();                    // for-each: podstawy
        iteratorRemove();                   // Iterator.remove — poprawne usuwanie
        concurrentModificationPitfall();    // pułapka: usuwanie w for-each
        threeFixes();                       // trzy sposoby naprawy
        indexLoopSkipPitfall();             // pułapka: pętla po indeksie gubi elementy
        listIteratorDemo();                 // ListIterator: set/add/wstecz
        forEachLambdaPreview();             // forEach(lambda) — zapowiedź
        whenToUseWhat();                    // kiedy czego używać
        exercises();                        // ćwiczenia
    }

    // =================================================================================================
    // 1. for-each — PODSTAWY
    // =================================================================================================

    /** 1. for-each to skrót: pod spodem kompilator generuje Iterator i pętlę hasNext()/next(). */
    static void forEachBasics() {
        section("1. for-each: jak to działa pod spodem");

        List<String> fruits = new ArrayList<>(List.of("jabłko", "banan", "gruszka"));   // fruits = owoce
        for (String f : fruits) {
            System.out.println("owoc: " + f);
        }
        // WYNIK: owoc: jabłko
        // WYNIK: owoc: banan
        // WYNIK: owoc: gruszka

        note("for-each = cukier składniowy. Kompilator zamienia to na Iterator<String> it = fruits.iterator(); "
                + "while (it.hasNext()) { String f = it.next(); ... }");
        // WYNIK: ℹ for-each = cukier składniowy. Kompilator zamienia to na Iterator<String> it = fruits.iterator(); while (it.hasNext()) { String f = it.next(); ... }
    }

    // =================================================================================================
    // 2. Iterator.remove() — POPRAWNE USUWANIE PODCZAS ITERACJI
    // =================================================================================================

    /**
     * 2. Iterator.remove() usuwa OSTATNI element zwrócony przez next() i aktualizuje wewnętrzny licznik
     * iteratora — to JEDYNY bezpieczny sposób usuwania „ręczne” podczas iterowania po ArrayList.
     */
    static void iteratorRemove() {
        section("2. Iterator: hasNext, next, remove");

        List<Integer> numbers = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        Iterator<Integer> it = numbers.iterator();
        while (it.hasNext()) {
            int n = it.next();
            if (n % 3 == 0) {
                it.remove();   // remove = usuń OSTATNI zwrócony przez next() element
            }
        }
        show("po usunięciu wielokrotności 3 (Iterator.remove)", numbers);
        // WYNIK: po usunięciu wielokrotności 3 (Iterator.remove) → [1, 2, 4, 5, 7, 8, 10]

        // DOBRA PRAKTYKA: it.remove() zawsze WOŁAJ dopiero PO it.next() (usuwa „to, co właśnie przeczytałem”).
        //   Wywołanie remove() bez wcześniejszego next() (albo dwa remove() z rzędu) rzuca IllegalStateException.
    }

    // =================================================================================================
    // 3. PUŁAPKA: usuwanie w for-each → ConcurrentModificationException
    // =================================================================================================

    /**
     * 3. for-each NIE daje dostępu do iteratora — nie ma jak wywołać remove() na nim. Wywołanie
     * list.remove(...) BEZPOŚREDNIO na liście, w trakcie iterowania po niej, zmienia modCount „za plecami”
     * iteratora — kolejny next() to wykrywa i rzuca ConcurrentModificationException.
     */
    static void concurrentModificationPitfall() {
        section("3. Pułapka: usuwanie w for-each");

        List<Integer> numbers = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
        expectThrows("usuwanie elementu wewnątrz for-each", () -> {
            for (Integer n : numbers) {
                if (n % 2 == 0) {
                    numbers.remove(n);   // remove(Object) — n to Integer; zmiana strukturalna PODCZAS for-each
                }
            }
        });
        // WYNIK: ✔ usuwanie elementu wewnątrz for-each → rzucono ConcurrentModificationException: (brak komunikatu)

        // PUŁAPKA: kod się KOMPILUJE i czasem nawet działa „w miarę OK” na małych przykładach (co jest
        //   jeszcze gorsze — błąd bywa niewidoczny na testach). Rzuci wyjątek albo, gorzej, po cichu
        //   POMINIE elementy (patrz sekcja 5) — nigdy nie modyfikuj kolekcji przez referencję do niej,
        //   iterując po niej for-each.
    }

    // =================================================================================================
    // 4. TRZY SPOSOBY NAPRAWY
    // =================================================================================================

    /** 4. Trzy poprawne techniki: Iterator.remove() (sekcja 2), removeIf(), iterowanie po KOPII. */
    static void threeFixes() {
        section("4. Trzy sposoby naprawy: removeIf, iteracja po kopii");

        List<Integer> byRemoveIf = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
        byRemoveIf.removeIf(n -> n % 2 == 0);   // sposób 1: removeIf — najkrótszy, gdy warunek jest prosty
        show("sposób 1: removeIf(parzyste)", byRemoveIf);
        // WYNIK: sposób 1: removeIf(parzyste) → [1, 3, 5]

        List<Integer> byCopy = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
        for (Integer n : new ArrayList<>(byCopy)) {   // sposób 2: for-each po KOPII, modyfikujemy oryginał
            if (n % 2 == 0) {
                byCopy.remove(n);
            }
        }
        show("sposób 2: for-each po kopii + remove na oryginale", byCopy);
        // WYNIK: sposób 2: for-each po kopii + remove na oryginale → [1, 3, 5]

        note("Sposób 1 (Iterator.remove/removeIf) jest wydajniejszy — jeden przebieg. Sposób 2 (kopia) "
                + "jest prostszy do zrozumienia, ale kopiuje całą listę — gorszy dla dużych kolekcji.");
        // WYNIK: ℹ Sposób 1 (Iterator.remove/removeIf) jest wydajniejszy — jeden przebieg. Sposób 2 (kopia) jest prostszy do zrozumienia, ale kopiuje całą listę — gorszy dla dużych kolekcji.
    }

    // =================================================================================================
    // 5. PUŁAPKA: pętla po indeksie „gubi” elementy przy remove(i)
    // =================================================================================================

    /**
     * 5. remove(i) przesuwa wszystko PO indeksie i w lewo. Jeśli po usunięciu i tak zwiększymy i (i++),
     * element, który właśnie „wjechał” na indeks i, zostanie POMINIĘTY — nigdy niesprawdzony.
     */
    static void indexLoopSkipPitfall() {
        section("5. Pułapka: pętla po indeksie gubi elementy");

        List<Integer> forward = new ArrayList<>(List.of(1, 2, 2, 3, 2, 4));   // trzy dwójki: indeksy 1, 2, 4
        for (int i = 0; i < forward.size(); i++) {
            if (forward.get(i) == 2) {
                forward.remove(i);   // usuwamy, ale i i tak wzrośnie o 1 — pominiemy element, który się przesunął
            }
        }
        show("źle: pętla w przód, i++ mimo remove (chcemy usunąć wszystkie 2)", forward);
        // WYNIK: źle: pętla w przód, i++ mimo remove (chcemy usunąć wszystkie 2) → [1, 2, 3, 4]

        note("Jedna dwójka PRZEŻYŁA! Po usunięciu indeksu 1 druga dwójka (była na indeksie 2) „wjechała” na "
                + "indeks 1 — a pętla poszła dalej do i=2, więc ten element nigdy nie został sprawdzony.");
        // WYNIK: ℹ Jedna dwójka PRZEŻYŁA! Po usunięciu indeksu 1 druga dwójka (była na indeksie 2) „wjechała” na indeks 1 — a pętla poszła dalej do i=2, więc ten element nigdy nie został sprawdzony.

        List<Integer> backward = new ArrayList<>(List.of(1, 2, 2, 3, 2, 4));
        for (int i = backward.size() - 1; i >= 0; i--) {
            if (backward.get(i) == 2) {
                backward.remove(i);   // usuwanie WSTECZ: usunięcie indeksu i nie rusza indeksów < i (jeszcze nieodwiedzonych)
            }
        }
        show("dobrze: pętla WSTECZ (usuwamy wszystkie 2)", backward);
        // WYNIK: dobrze: pętla WSTECZ (usuwamy wszystkie 2) → [1, 3, 4]

        // DOBRA PRAKTYKA: gdy MUSISZ usuwać po indeksie w klasycznej pętli for, idź OD KOŃCA (i-- zamiast
        //   i++). Usuwanie elementu o indeksie i przesuwa tylko elementy PO nim — indeksy PRZED i
        //   (jeszcze nieodwiedzone przy iteracji wstecz) zostają nietknięte.
    }

    // =================================================================================================
    // 6. ListIterator — set, add, iterowanie WSTECZ
    // =================================================================================================

    /**
     * 6. ListIterator rozszerza Iterator o: set(x) — podmień ostatni zwrócony element; add(x) — wstaw
     * nowy element ZARAZ PO kursorze; hasPrevious()/previous() — iterowanie WSTECZ.
     */
    static void listIteratorDemo() {
        section("6. ListIterator: set, add, wstecz");

        List<String> names = new ArrayList<>(List.of("ala", "bob", "cela"));
        ListIterator<String> upper = names.listIterator();
        while (upper.hasNext()) {
            String s = upper.next();
            upper.set(s.toUpperCase());   // set = podmień element, który WŁAŚNIE zwrócił next() (indeks bez zmian)
        }
        show("po ListIterator.set(toUpperCase)", names);
        // WYNIK: po ListIterator.set(toUpperCase) → [ALA, BOB, CELA]

        List<Integer> nums = new ArrayList<>(List.of(1, 2, 3));
        ListIterator<Integer> inserter = nums.listIterator();
        while (inserter.hasNext()) {
            int n = inserter.next();
            if (n == 2) {
                inserter.add(99);   // add = wstaw NOWY element zaraz po kursorze (przed kolejnym next())
            }
        }
        show("po ListIterator.add(99) zaraz po elemencie 2", nums);
        // WYNIK: po ListIterator.add(99) zaraz po elemencie 2 → [1, 2, 99, 3]

        List<String> letters = new ArrayList<>(List.of("a", "b", "c"));
        ListIterator<String> back = letters.listIterator(letters.size());   // start NA KOŃCU listy
        StringBuilder reversed = new StringBuilder();
        while (back.hasPrevious()) {
            reversed.append(back.previous());   // previous = poprzedni (idziemy wstecz)
        }
        show("wstecz przez ListIterator", reversed);
        // WYNIK: wstecz przez ListIterator → cba

        // PUŁAPKA: ListIterator.add(x) wstawia PRZED elementem, który zwróciłby kolejny next() — jeśli
        //   dodajesz element zaraz po next(), on sam NIE zostanie odwiedzony ponownie w tej samej pętli
        //   (kursor przeskakuje za niego). To dobrze — inaczej pętla mogłaby się nie skończyć.
    }

    // =================================================================================================
    // 7. forEach(lambda) — ZAPOWIEDŹ
    // =================================================================================================

    /**
     * 7. Collection.forEach(lambda) to jeszcze krótszy zapis pętli — pełne omówienie lambd: t13_lambdas.
     * println wewnątrz lambdy to EFEKT UBOCZNY (side effect) — OK do demonstracji, unikaj w „prawdziwym” kodzie.
     */
    static void forEachLambdaPreview() {
        section("7. forEach(lambda) — zapowiedź");

        List<String> fruits = List.of("jabłko", "banan", "gruszka");
        fruits.forEach(f -> System.out.println("• " + f));   // forEach = wykonaj dla każdego elementu
        // WYNIK: • jabłko
        // WYNIK: • banan
        // WYNIK: • gruszka

        // PUŁAPKA: println wewnątrz lambdy to efekt uboczny (side effect) — lambda „po drodze” robi coś
        //   poza zwróceniem wyniku. Do wypisywania - OK. Do liczenia sumy w zewnętrznej zmiennej - unikaj
        //   (Collections10Patterns oraz cały rozdział t16_streams pokazują poprawne, „czyste” podejścia).
    }

    // =================================================================================================
    // 8. KIEDY CZEGO UŻYWAĆ
    // =================================================================================================

    /** 8. Krótki przewodnik decyzyjny podsumowujący całą lekcję. */
    static void whenToUseWhat() {
        section("8. Kiedy czego używać");

        note("Tylko CZYTASZ, bez modyfikacji → for-each (najprostsze, najczytelniejsze).");
        // WYNIK: ℹ Tylko CZYTASZ, bez modyfikacji → for-each (najprostsze, najczytelniejsze).
        note("Usuwasz PODCZAS iterowania, prosty warunek → removeIf.");
        // WYNIK: ℹ Usuwasz PODCZAS iterowania, prosty warunek → removeIf.
        note("Usuwasz z bardziej złożoną logiką (np. licznik, wczesne przerwanie) → Iterator.remove().");
        // WYNIK: ℹ Usuwasz z bardziej złożoną logiką (np. licznik, wczesne przerwanie) → Iterator.remove().
        note("Podmieniasz elementy pod ich indeksami, wstawiasz w trakcie iteracji, albo idziesz wstecz → ListIterator.");
        // WYNIK: ℹ Podmieniasz elementy pod ich indeksami, wstawiasz w trakcie iteracji, albo idziesz wstecz → ListIterator.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • for-each = Iterator pod spodem. Nie pozwala bezpiecznie modyfikować kolekcji w trakcie iteracji.
     *   • Usuwanie list.remove(x) WEWNĄTRZ for-each po tej samej liście → ConcurrentModificationException
     *     (albo, gorzej, po cichu pomija elementy przy pętli po indeksie — sekcja 5).
     *   • Bezpieczne usuwanie podczas iteracji: it.remove() (po next()), removeIf(predykat), albo
     *     iterowanie po KOPII przy modyfikacji oryginału.
     *   • Pętla po indeksie z remove(i): idź WSTECZ (i od size-1 do 0), inaczej gubisz elementy.
     *   • ListIterator: set(x) podmienia ostatni zwrócony next(), add(x) wstawia zaraz po kursorze,
     *     hasPrevious()/previous() pozwalają iterować wstecz.
     *   • Collection.forEach(lambda) to krótszy zapis pętli tylko do odczytu/efektów ubocznych (drukowanie).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego for-each nie pozwala bezpiecznie usuwać elementów z kolekcji, po której iterujemy?
     *   2. Co wypisze:
     *          List<Integer> l = new ArrayList<>(List.of(1, 2, 2, 3));
     *          for (int i = 0; i < l.size(); i++) {
     *              if (l.get(i) == 2) {
     *                  l.remove(i);
     *              }
     *          }
     *          System.out.println(l);
     *      ?
     *   3. ZNAJDŹ BŁĄD:
     *          for (String s : list) {
     *              if (s.isEmpty()) {
     *                  list.remove(s);
     *              }
     *          }
     *   4. Co wypisze:
     *          List<String> l = new ArrayList<>(List.of("x", "y", "z"));
     *          ListIterator<String> it = l.listIterator();
     *          it.next();
     *          it.remove();
     *          System.out.println(l);
     *      ?
     *   5. Czym różni się Iterator.remove() od ListIterator.add() — gdzie dokładnie ląduje nowy element
     *      po add()?
     *   6. Dlaczego iterowanie WSTECZ (od size-1 do 0) naprawia pułapkę z pytania 2, a iterowanie
     *      w przód — nie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: usuń wielokrotności 2 (Iterator)", List.of(5, 3, 1, 9, 7, 3),
                () -> removeMultiplesOf(SampleData.numbers(), 2));
        Check.equal("ćw. 2: wielkie litery co drugi element (ListIterator)", List.of("A", "b", "C", "d", "E"),
                () -> upperEveryOther(List.of("a", "b", "c", "d", "e")));
        Check.equal("ćw. 3: usuń WSZYSTKIE wystąpienia (PRZEPISZ pętlę wstecz)", List.of(1, 3),
                () -> removeAllOccurrences(new ArrayList<>(List.of(2, 1, 2, 2, 3, 2)), 2));
        Check.equal("ćw. 4: wstaw znacznik po każdym elemencie (ListIterator.add)", List.of("a", "-", "b", "-"),
                () -> insertAfterEach(List.of("a", "b"), "-"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of(5, 3, 1, 9, 7, 3), () -> solutionRemoveMultiplesOf(SampleData.numbers(), 2));
        Check.equal("ćw. 2 (wzorzec)", List.of("A", "b", "C", "d", "E"),
                () -> solutionUpperEveryOther(List.of("a", "b", "c", "d", "e")));
        Check.equal("ćw. 3 (wzorzec)", List.of(1, 3),
                () -> solutionRemoveAllOccurrences(new ArrayList<>(List.of(2, 1, 2, 2, 3, 2)), 2));
        Check.equal("ćw. 4 (wzorzec)", List.of("a", "-", "b", "-"), () -> solutionInsertAfterEach(List.of("a", "b"), "-"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć KOPIĘ listy bez elementów podzielnych przez divisor. Podpowiedź:
     * Iterator.remove() na kopii wejściowej listy (sekcja 2) — nie modyfikuj oryginału.
     */
    static List<Integer> removeMultiplesOf(List<Integer> numbers, int divisor) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć KOPIĘ listy, w której elementy na PARZYSTYCH indeksach (0, 2, 4, ...)
     * są zamienione na wielkie litery. Podpowiedź: ListIterator.nextIndex() mówi, jaki indeks zaraz
     * zwróci next(); ListIterator.set(...) podmienia ostatni zwrócony element.
     */
    static List<String> upperEveryOther(List<String> words) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (trudniejsze, PRZEPISZ): poniższy kod ma pułapkę z sekcji 5 — pętla w przód z i++ mimo
     * remove(i) gubi elementy:
     * <pre>{@code
     * for (int i = 0; i < numbers.size(); i++) {
     *     if (numbers.get(i).equals(value)) {
     *         numbers.remove(i);
     *     }
     * }
     * return numbers;
     * }</pre>
     * Przepisz tę pętlę, żeby iterowała WSTECZ (od {@code numbers.size() - 1} do 0) i poprawnie usuwała
     * WSZYSTKIE wystąpienia value. Metoda dostaje MODYFIKOWALNĄ listę i ma zwrócić TĘ SAMĄ listę.
     */
    static List<Integer> removeAllOccurrences(List<Integer> numbers, int value) {
        // TODO: twoje rozwiązanie
        return numbers;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć KOPIĘ listy words, w której po KAŻDYM elemencie wstawiono marker.
     * Podpowiedź: ListIterator.add(x) wstawia x zaraz po elemencie, który przed chwilą zwrócił next().
     */
    static List<String> insertAfterEach(List<String> words, String marker) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<Integer> solutionRemoveMultiplesOf(List<Integer> numbers, int divisor) {
        List<Integer> copy = new ArrayList<>(numbers);
        Iterator<Integer> it = copy.iterator();
        while (it.hasNext()) {
            if (it.next() % divisor == 0) {
                it.remove();
            }
        }
        return copy;
    }

    static List<String> solutionUpperEveryOther(List<String> words) {
        List<String> copy = new ArrayList<>(words);
        ListIterator<String> it = copy.listIterator();
        while (it.hasNext()) {
            int idx = it.nextIndex();
            String w = it.next();
            if (idx % 2 == 0) {
                it.set(w.toUpperCase());
            }
        }
        return copy;
    }

    static List<Integer> solutionRemoveAllOccurrences(List<Integer> numbers, int value) {
        for (int i = numbers.size() - 1; i >= 0; i--) {
            if (numbers.get(i).equals(value)) {
                numbers.remove(i);
            }
        }
        return numbers;
    }

    static List<String> solutionInsertAfterEach(List<String> words, String marker) {
        List<String> copy = new ArrayList<>(words);
        ListIterator<String> it = copy.listIterator();
        while (it.hasNext()) {
            it.next();
            it.add(marker);
        }
        return copy;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. for-each nie daje dostępu do iteratora, więc nie ma jak wywołać remove() na nim. Usunięcie
     *      elementu bezpośrednio z listy (list.remove(...)) zmienia jej modCount „za plecami” iteratora —
     *      kolejne next() wykrywa niezgodność i rzuca ConcurrentModificationException.
     *   2. „[1, 2, 3]” — jedna dwójka przeżywa (ten sam mechanizm co w sekcji 5): po usunięciu indeksu 1
     *      druga dwójka wjeżdża na indeks 1, ale pętla idzie dalej do i=2.
     *   3. To ta sama pułapka co sekcja 3: usuwanie z listy WEWNĄTRZ for-each po tej samej liście rzuca
     *      ConcurrentModificationException. Popraw: iterator.remove(), removeIf(String::isEmpty), albo
     *      iteruj po kopii.
     *   4. „[y, z]” — it.next() zwraca "x" (kursor za "x"), it.remove() usuwa OSTATNI zwrócony element,
     *      czyli "x".
     *   5. Iterator.remove() USUWA ostatni element zwrócony przez next(). ListIterator.add(x) NIC nie
     *      usuwa — WSTAWIA nowy element x zaraz po kursorze (czyli zaraz po elemencie zwróconym przez
     *      ostatni next(), przed elementem, który zwróci KOLEJNY next()).
     *   6. Usuwanie elementu o indeksie i przesuwa TYLKO elementy o indeksach WIĘKSZYCH niż i. Idąc wstecz,
     *      elementy o mniejszych indeksach (jeszcze nieodwiedzone) nigdy się nie przesuwają — nic nie ginie.
     *      Idąc w przód, właśnie odwiedzany fragment się przesuwa, a i i tak rośnie — element „wjeżdżający”
     *      na bieżący indeks zostaje pominięty.
     */
    // </editor-fold>
}
