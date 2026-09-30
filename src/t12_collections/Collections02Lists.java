package t12_collections;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: ArrayList — dodawanie, usuwanie, widoki, sortowanie w miejscu
 *        (view = widok; in place = w miejscu, bez tworzenia nowej kolekcji; fixed-size = stały rozmiar)
 *
 * W SKRÓCIE:
 *   ArrayList to lista oparta na TABLICY, która w razie potrzeby sama się powiększa. Dodawanie na końcu jest
 *   szybkie, ale dodawanie/usuwanie w środku wymaga przesunięcia reszty elementów. subList zwraca WIDOK
 *   (okno na tę samą listę), nie kopię — zmiany widać w obie strony.
 *
 * ANALOGIA: rząd ponumerowanych krzeseł w kinie.
 *   get(i) to spojrzenie na krzesło numer i — natychmiastowe, bez chodzenia (dlatego ArrayList.get jest
 *   szybkie). Wstawienie kogoś w środku rzędu (add(index, ...)) oznacza, że WSZYSCY siedzący dalej muszą
 *   przesunąć się o jedno miejsce — to kosztuje czas proporcjonalny do liczby osób za miejscem wstawienia.
 *
 * JAK TO DZIAŁA:
 *   add(x)          → na koniec, zwykle O(1) (czasem tablica musi się powiększyć i skopiować — rzadko).
 *   add(i, x)       → przesuwa elementy [i..koniec] o jedno w prawo, O(n).
 *   remove(i)       → przesuwa elementy [i+1..koniec] o jedno w lewo, O(n).
 *   get(i) / set(i) → bezpośredni dostęp do tabeli, O(1).
 *   subList(od, do) → WIDOK: nowy obiekt List, ale patrzy na TĘ SAMĄ tablicę co oryginał.
 *
 * SŁÓWKA:
 *   add = dodaj; remove = usuń; get = pobierz; set = ustaw; indexOf = indeks (pozycja) elementu;
 *   contains = zawiera; subList = podlista (widok); view = widok; sort = sortuj; replaceAll = zamień
 *   wszystkie (wg funkcji); removeIf = usuń, jeśli (wg warunku); structural change = zmiana strukturalna
 *   (dodanie/usunięcie, zmienia rozmiar) — w odróżnieniu od set(), które rozmiaru nie zmienia.
 *
 * ZOBACZ TEŻ: t12_collections/Collections01Overview (hierarchia), t12_collections/Collections03IterationModification
 *             (ConcurrentModificationException ze szczegółami), t12_collections/Collections06QueuesDeques
 *             (ArrayDeque jako lepszy zamiennik LinkedList), t12_collections/Collections07ComparableComparator
 *             (Comparator, sortowanie wielopoziomowe), t12_collections/Collections13Performance (policzone Big-O),
 *             t13_lambdas/Lambda04MethodReferences (String::toUpperCase).
 * </pre>
 */
public class Collections02Lists {

    public static void main(String[] args) {
        title("Collections02 — ArrayList: dodawanie, usuwanie, widoki, sortowanie");

        arrayListBasics();        // basics = podstawy
        getSetIndexContains();    // get/set/index/contains
        removePitfall();          // pułapka remove(index) vs remove(Object)
        subListView();            // subList = widok
        sortInPlace();            // sort w miejscu
        replaceAllDemo();         // replaceAll
        removeIfDemo();           // removeIf
        arraysAsListPitfall();    // Arrays.asList — pułapka stałego rozmiaru
        arrayListVsLinkedList();  // ArrayList kontra LinkedList
        exercises();              // ćwiczenia
    }

    // =================================================================================================
    // 1. ArrayList: TWORZENIE I DODAWANIE
    // =================================================================================================

    /** 1. add(element) dodaje na koniec. add(index, element) wstawia i przesuwa resztę w prawo. */
    static void arrayListBasics() {
        section("1. ArrayList: add(element), add(index, element)");

        List<String> fruits = new ArrayList<>();   // fruits = owoce
        fruits.add("jabłko");
        fruits.add("banan");
        fruits.add(1, "gruszka");   // wstaw na indeksie 1 — "banan" przesuwa się na indeks 2
        show("owoce", fruits);
        // WYNIK: owoce → [jabłko, gruszka, banan]

        // DOBRA PRAKTYKA: add(index, ...) i remove(index) na ArrayList przesuwają elementy w tablicy — to
        //   O(n). Dodawanie/usuwanie WYŁĄCZNIE na końcu jest tanie; częste operacje na POCZĄTKU listy to
        //   sygnał, że ArrayDeque (Collections06QueuesDeques) będzie lepszym wyborem.
    }

    // =================================================================================================
    // 2. get, set, indexOf, contains
    // =================================================================================================

    /** 2. get/set to bezpośredni dostęp do tablicy (szybkie). indexOf/contains przeszukują liniowo. */
    static void getSetIndexContains() {
        section("2. get, set, indexOf, contains");

        List<String> fruits = new ArrayList<>(List.of("jabłko", "gruszka", "banan"));

        show("element o indeksie 0", fruits.get(0));
        // WYNIK: element o indeksie 0 → jabłko

        fruits.set(0, "pomarańcza");   // set = ustaw (nadpisuje, NIE zmienia rozmiaru listy)
        show("po set(0, \"pomarańcza\")", fruits);
        // WYNIK: po set(0, "pomarańcza") → [pomarańcza, gruszka, banan]

        show("indexOf(\"banan\")", fruits.indexOf("banan"));
        // WYNIK: indexOf("banan") → 2
        show("indexOf(\"kiwi\") (nie ma)", fruits.indexOf("kiwi"));
        // WYNIK: indexOf("kiwi") (nie ma) → -1
        show("contains(\"gruszka\")", fruits.contains("gruszka"));
        // WYNIK: contains("gruszka") → true

        // PUŁAPKA: indexOf i contains przeszukują listę LINIOWO (od początku, po kolei, porównując equals) —
        //   O(n) na wywołanie. Dla dużych zbiorów danych, gdy liczy się szybkie contains, lepszy jest
        //   HashSet (Collections04Sets) albo HashMap (Collections05Maps) — O(1).
    }

    // =================================================================================================
    // 3. PUŁAPKA: remove(index) vs remove(Object) na List<Integer>
    // =================================================================================================

    /**
     * 3. List ma DWIE przeciążone metody remove: remove(int index) i remove(Object o). Dla List{@code <Integer>}
     * to źródło klasycznej pułapki, bo literał liczbowy jest typu int, nie Integer.
     */
    static void removePitfall() {
        section("3. Pułapka: remove(index) kontra remove(Object) — List<Integer>");

        List<Integer> numbers = new ArrayList<>(List.of(10, 20, 30, 40));
        show("lista liczb", numbers);
        // WYNIK: lista liczb → [10, 20, 30, 40]

        numbers.remove(1);                       // remove(int) — usuwa element NA INDEKSIE 1, czyli 20
        show("po remove(1) — to jest INDEKS", numbers);
        // WYNIK: po remove(1) — to jest INDEKS → [10, 30, 40]

        numbers.remove(Integer.valueOf(30));     // remove(Object) — usuwa WARTOŚĆ 30
        show("po remove(Integer.valueOf(30)) — to jest WARTOŚĆ", numbers);
        // WYNIK: po remove(Integer.valueOf(30)) — to jest WARTOŚĆ → [10, 40]

        // PUŁAPKA: numbers.remove(30) na List<Integer> wywoła remove(int index), NIE remove(Object) —
        //   kompilator wybiera przeciążenie remove(int), bo dopasowanie bez autoboxingu jest zawsze
        //   preferowane nad dopasowaniem WYMAGAJĄCYM autoboxingu (int 30 → Integer). Żeby usunąć WARTOŚĆ,
        //   trzeba jawnie opakować: remove(Integer.valueOf(30)) albo remove((Integer) 30).
        // DOBRA PRAKTYKA: jeśli usuwasz „element o danej wartości” z List<Integer>, zawsze pisz jawny
        //   Integer.valueOf(...) — inaczej kod kompiluje się, ale robi coś INNEGO niż zamierzałeś.
    }

    // =================================================================================================
    // 4. subList — WIDOK, nie kopia
    // =================================================================================================

    /**
     * 4. subList(od, do) zwraca WIDOK na tę samą listę (indeks „do” jest wyłączny — jak w pętli for).
     * Zmiana przez widok jest widoczna w oryginale i odwrotnie. Zmiana ROZMIARU oryginału (add/remove poza
     * widokiem) psuje widok — kolejne użycie rzuca ConcurrentModificationException (więcej: Collections03).
     */
    static void subListView() {
        section("4. subList: widok, nie kopia");

        List<String> letters = new ArrayList<>(List.of("a", "b", "c", "d", "e"));
        List<String> view = letters.subList(1, 4);   // indeksy 1,2,3 (4 wyłącznie) — widok = fragment widoczny
        show("subList(1, 4)", view);
        // WYNIK: subList(1, 4) → [b, c, d]

        view.set(0, "B");   // zmiana PRZEZ WIDOK...
        show("oryginał po zmianie w widoku", letters);
        // WYNIK: oryginał po zmianie w widoku → [a, B, c, d, e]

        letters.set(2, "C");   // ...i zmiana w oryginale (set, nie zmienia rozmiaru) widoczna w widoku
        show("widok po zmianie w oryginale", view);
        // WYNIK: widok po zmianie w oryginale → [B, C, d]

        letters.add("f");   // ZMIANA STRUKTURALNA oryginału (nie przez widok) — rozmiar się zmienił
        expectThrows("użycie widoku po strukturalnej zmianie oryginału", () -> view.get(0));
        // WYNIK: ✔ użycie widoku po strukturalnej zmianie oryginału → rzucono ConcurrentModificationException: (brak komunikatu)

        // PUŁAPKA: subList wygląda jak kopia, ale nią NIE JEST. Jeśli potrzebujesz niezależnego fragmentu,
        //   skopiuj jawnie: new ArrayList<>(letters.subList(1, 4)) albo List.copyOf(...).
    }

    // =================================================================================================
    // 5. sort — SORTOWANIE W MIEJSCU
    // =================================================================================================

    /** 5. List.sort(Comparator) sortuje LISTĘ SAMĄ W SOBIE (nie tworzy nowej). Pełne Comparatory: Collections07. */
    static void sortInPlace() {
        section("5. sort — sortowanie w miejscu (Comparator wprowadzenie)");

        List<String> words = new ArrayList<>(List.of("banan", "Ananas", "czereśnia"));

        words.sort(Comparator.naturalOrder());   // naturalOrder = naturalny porządek (dla String: Unicode)
        show("posortowane (naturalny porządek)", words);
        // WYNIK: posortowane (naturalny porządek) → [Ananas, banan, czereśnia]
        note("Naturalny porządek Stringów porównuje wartości Unicode: WIELKIE litery mają MNIEJSZE wartości "
                + "niż małe (A=65, a=97) — dlatego „Ananas” jest przed „banan”. Sortowanie polskich napisów "
                + "„po ludzku” (ą, ł, ś, ż) to Collections07ComparableComparator (Collator).");
        // WYNIK: ℹ Naturalny porządek Stringów porównuje wartości Unicode: WIELKIE litery mają MNIEJSZE wartości niż małe (A=65, a=97) — dlatego „Ananas” jest przed „banan”. Sortowanie polskich napisów „po ludzku” (ą, ł, ś, ż) to Collections07ComparableComparator (Collator).

        words.sort(Comparator.reverseOrder());   // reverseOrder = porządek odwrotny
        show("posortowane malejąco", words);
        // WYNIK: posortowane malejąco → [czereśnia, banan, Ananas]
    }

    // =================================================================================================
    // 6. replaceAll — ZAMIEŃ KAŻDY ELEMENT
    // =================================================================================================

    /** 6. replaceAll(funkcja) podmienia KAŻDY element wynikiem funkcji — w miejscu. (proste użycie, pełnia w t13_lambdas) */
    static void replaceAllDemo() {
        section("6. replaceAll — przekształć każdy element w miejscu");

        List<String> names = new ArrayList<>(List.of("ala", "bob", "cela"));
        names.replaceAll(String::toUpperCase);   // String::toUpperCase = referencja do metody (t13_lambdas/Lambda04MethodReferences)
        show("po replaceAll(String::toUpperCase)", names);
        // WYNIK: po replaceAll(String::toUpperCase) → [ALA, BOB, CELA]
    }

    // =================================================================================================
    // 7. removeIf — USUŃ WEDŁUG WARUNKU
    // =================================================================================================

    /** 7. removeIf(predykat) usuwa WSZYSTKIE elementy spełniające warunek — jedno wywołanie, bez ręcznej pętli. */
    static void removeIfDemo() {
        section("7. removeIf — usuń elementy spełniające warunek");

        List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        nums.removeIf(n -> n % 2 == 0);   // removeIf = usuń, jeśli (predykat: warunek zwracający true/false)
        show("po removeIf (usuń parzyste)", nums);
        // WYNIK: po removeIf (usuń parzyste) → [1, 3, 5, 7, 9]

        // DOBRA PRAKTYKA: removeIf zamiast ręcznej pętli z Iterator.remove() — krócej, czytelniej, i nie da
        //   się przypadkowo popełnić błędu ConcurrentModificationException (Collections03IterationModification).
    }

    // =================================================================================================
    // 8. PUŁAPKA: Arrays.asList — LISTA O STAŁYM ROZMIARZE
    // =================================================================================================

    /**
     * 8. Arrays.asList(...) zwraca listę o STAŁYM ROZMIARZE — to WIDOK na tablicę. set() działa (nadpisuje
     * komórkę tablicy), ale add/remove zmieniałyby rozmiar tablicy — niemożliwe, więc rzucają wyjątek.
     */
    static void arraysAsListPitfall() {
        section("8. Pułapka: Arrays.asList — stały rozmiar");

        List<String> fixedSize = Arrays.asList("x", "y", "z");   // fixed size = stały rozmiar
        fixedSize.set(0, "X");   // set działa — nadpisuje komórkę tablicy, rozmiar bez zmian
        show("po set(0, \"X\") — działa", fixedSize);
        // WYNIK: po set(0, "X") — działa → [X, y, z]

        expectThrows("Arrays.asList(...).add(\"W\")", () -> fixedSize.add("W"));
        // WYNIK: ✔ Arrays.asList(...).add("W") → rzucono UnsupportedOperationException: (brak komunikatu)

        // PUŁAPKA: Arrays.asList NIE jest w pełni modyfikowalną listą — to widok o stałym rozmiarze na
        //   tablicę (dlatego set działa, a add/remove — nie). Żeby dostać zwykłą, w pełni modyfikowalną
        //   listę, opakuj jawnie: new ArrayList<>(Arrays.asList("x", "y", "z")).
    }

    // =================================================================================================
    // 9. ArrayList kontra LinkedList
    // =================================================================================================

    /**
     * 9. LinkedList implementuje zarówno List, jak i Deque. Jako List jest PRAWIE ZAWSZE gorszym wyborem
     * niż ArrayList: get(i) wędruje węzeł po węźle (O(n)), a każdy węzeł to dodatkowa pamięć na dwa
     * wskaźniki (prev/next). Policzone porównanie: Collections13Performance.
     */
    static void arrayListVsLinkedList() {
        section("9. ArrayList kontra LinkedList");

        List<String> linked = new LinkedList<>();
        linked.add("a");
        linked.add("b");
        show("LinkedList jako List — działa jak List", linked);
        // WYNIK: LinkedList jako List — działa jak List → [a, b]

        note("LinkedList.get(i) idzie węzeł po węźle od początku albo końca (O(n) na wywołanie); "
                + "ArrayList.get(i) to bezpośredni dostęp do tablicy (O(1)). Policzone porównanie: "
                + "Collections13Performance.");
        // WYNIK: ℹ LinkedList.get(i) idzie węzeł po węźle od początku albo końca (O(n) na wywołanie); ArrayList.get(i) to bezpośredni dostęp do tablicy (O(1)). Policzone porównanie: Collections13Performance.

        // DOBRA PRAKTYKA: w większości programów wystarczy ArrayList. Gdy naprawdę potrzebujesz kolejki
        //   albo stosu (dodawanie/usuwanie na OBU końcach), użyj ArrayDeque (Collections06QueuesDeques) —
        //   jest szybszy niż LinkedList i nie kusi Cię przypadkowym get(i) w pętli.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • add(x): koniec, ~O(1). add(i,x)/remove(i): przesunięcie, O(n). get(i)/set(i): O(1).
     *   • remove(int) usuwa wg INDEKSU. remove(Object) usuwa wg WARTOŚCI. Dla List<Integer> pisz
     *     remove(Integer.valueOf(x)), żeby usunąć wartość x.
     *   • subList(od, do) to WIDOK (nie kopia!) — zmiany widać w obie strony. Strukturalna zmiana
     *     oryginału (add/remove poza widokiem) psuje widok: ConcurrentModificationException.
     *   • sort(Comparator), replaceAll(funkcja), removeIf(predykat) działają W MIEJSCU (modyfikują listę).
     *   • Arrays.asList(...): STAŁY ROZMIAR — set() działa, add()/remove() rzucają
     *     UnsupportedOperationException.
     *   • ArrayList — domyślny wybór dla List. LinkedList — prawie nigdy (użyj ArrayDeque jako
     *     kolejki/stosu, Collections06QueuesDeques).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego dla List<Integer> trzeba napisać remove(Integer.valueOf(x)), żeby usunąć WARTOŚĆ x,
     *      a nie element o indeksie x?
     *   2. Co wypisze:
     *          List<Integer> l = new ArrayList<>(List.of(5, 6, 7));
     *          l.remove(1);
     *          System.out.println(l);
     *      ?
     *   3. ZNAJDŹ BŁĄD:
     *          List<String> a = Arrays.asList("x", "y");
     *          a.add("z");
     *   4. Co wypisze:
     *          List<Integer> src = new ArrayList<>(List.of(1, 2, 3, 4, 5));
     *          List<Integer> view = src.subList(1, 3);
     *          src.set(1, 99);
     *          System.out.println(view);
     *      ?
     *   5. Dlaczego ArrayDeque jest zwykle lepszym wyborem niż LinkedList jako kolejka albo stos?
     *   6. Czym różni się replaceAll od removeIf (jedno zdanie)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: produkty droższe niż 1000 zł",
                List.of("Laptop Pro 14", "Smartfon X", "Monitor 27 cali", "Ekspres do kawy"),
                () -> namesAbove(SampleData.products(), new BigDecimal("1000")));
        Check.equal("ćw. 2: usuń WARTOŚĆ 1 (pierwsze wystąpienie)", List.of(3, 4, 1, 5, 9, 2, 6),
                () -> removeValue(new ArrayList<>(List.of(3, 1, 4, 1, 5, 9, 2, 6)), 1));
        Check.equal("ćw. 3: środek listy (bez pierwszego i ostatniego)", List.of("b", "c", "d"),
                () -> middle(List.of("a", "b", "c", "d", "e")));
        Check.equal("ćw. 4: długie słowa, WIELKIMI (PRZEPISZ na removeIf+replaceAll)", List.of("JAVA", "LAMBDA"),
                () -> longWordsUpper(List.of("ja", "java", "to", "lambda", "fun")));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("Laptop Pro 14", "Smartfon X", "Monitor 27 cali", "Ekspres do kawy"),
                () -> solutionNamesAbove(SampleData.products(), new BigDecimal("1000")));
        Check.equal("ćw. 2 (wzorzec)", List.of(3, 4, 1, 5, 9, 2, 6),
                () -> solutionRemoveValue(new ArrayList<>(List.of(3, 1, 4, 1, 5, 9, 2, 6)), 1));
        Check.equal("ćw. 3 (wzorzec)", List.of("b", "c", "d"), () -> solutionMiddle(List.of("a", "b", "c", "d", "e")));
        Check.equal("ćw. 4 (wzorzec)", List.of("JAVA", "LAMBDA"),
                () -> solutionLongWordsUpper(List.of("ja", "java", "to", "lambda", "fun")));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć nazwy produktów DROŻSZYCH niż minPrice ({@code price > minPrice}), w
     * kolejności z listy wejściowej. Podpowiedź: BigDecimal porównujemy przez compareTo, nie equals
     * ani operatorem {@code >}.
     */
    static List<String> namesAbove(List<Product> products, BigDecimal minPrice) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (łatwe): usuń z listy PIERWSZE wystąpienie WARTOŚCI value (nie elementu o indeksie value!)
     * i zwróć tę samą listę. Podpowiedź: remove(Integer.valueOf(value)) — sekcja 3.
     */
    static List<Integer> removeValue(List<Integer> numbers, int value) {
        // TODO: twoje rozwiązanie
        return numbers;
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć NIEMODYFIKOWALNĄ listę bez pierwszego i ostatniego elementu.
     * Podpowiedź: subList(1, list.size() - 1) + List.copyOf (subList to widok — skopiuj go).
     */
    static List<String> middle(List<String> list) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze, PRZEPISZ): poniższy kod działa, ale to ręczna pętla budująca nową listę:
     * <pre>{@code
     * List<String> result = new ArrayList<>();
     * for (String w : words) {
     *     if (w.length() > 3) {
     *         result.add(w.toUpperCase());
     *     }
     * }
     * return result;
     * }</pre>
     * Przepisz to na: kopię wejściowej listy ({@code new ArrayList<>(words)}), potem
     * {@code removeIf(w -> w.length() <= 3)} i {@code replaceAll(String::toUpperCase)} — dwa wywołania
     * biblioteczne zamiast ręcznej pętli.
     */
    static List<String> longWordsUpper(List<String> words) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solutionNamesAbove(List<Product> products, BigDecimal minPrice) {
        List<String> result = new ArrayList<>();
        for (Product p : products) {
            if (p.price().compareTo(minPrice) > 0) {
                result.add(p.name());
            }
        }
        return result;
    }

    static List<Integer> solutionRemoveValue(List<Integer> numbers, int value) {
        numbers.remove(Integer.valueOf(value));
        return numbers;
    }

    static List<String> solutionMiddle(List<String> list) {
        return List.copyOf(list.subList(1, list.size() - 1));
    }

    static List<String> solutionLongWordsUpper(List<String> words) {
        List<String> copy = new ArrayList<>(words);
        copy.removeIf(w -> w.length() <= 3);
        copy.replaceAll(String::toUpperCase);
        return copy;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo List ma dwa przeciążenia: remove(int index) i remove(Object o). Literał liczbowy (np. 1) jest
     *      typu int, więc kompilator wybiera remove(int) — dopasowanie bez autoboxingu jest zawsze
     *      preferowane. Żeby wywołać remove(Object), trzeba jawnie opakować: Integer.valueOf(x).
     *   2. „[5, 7]” — remove(1) usuwa element o INDEKSIE 1 (wartość 6), zostają 5 i 7.
     *   3. Arrays.asList zwraca listę o STAŁYM ROZMIARZE (widok na tablicę) — add() rzuca
     *      UnsupportedOperationException. Poprawnie: new ArrayList<>(Arrays.asList("x", "y")).add("z").
     *   4. „[99, 3]” — set(1, 99) NIE jest zmianą strukturalną (rozmiar bez zmian), więc widok src.subList(1, 3)
     *      (indeksy 1 i 2) widzi zmianę: indeks 1 to teraz 99, indeks 2 to nadal 3.
     *   5. ArrayDeque jest szybszy (tablica cykliczna, bez narzutu na węzły z dwoma wskaźnikami) i nie
     *      kusi przypadkowym get(i) w pętli tak jak LinkedList, który jest jednocześnie (mylącym) List-em.
     *   6. replaceAll PODMIENIA każdy element wynikiem funkcji (rozmiar listy bez zmian); removeIf USUWA
     *      elementy spełniające warunek (rozmiar listy maleje).
     */
    // </editor-fold>
}
