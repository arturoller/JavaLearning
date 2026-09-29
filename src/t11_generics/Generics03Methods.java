package t11_generics;

import helpers.Check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Metody generyczne i wnioskowanie typu
 *        (generic method = metoda generyczna; type inference = wnioskowanie typu; type witness = jawne podanie typu)
 *
 * W SKRÓCIE:
 *   Nie tylko klasa może mieć parametr typu — pojedyncza METODA też. Deklaracja {@code <T>} stoi PRZED typem zwracanym:
 *   static {@code <T>} T firstOrDefault(List{@code <T>} list, T def). Przy wywołaniu zwykle nie podajesz T — kompilator
 *   sam go WYWNIOSKUJE z argumentów (albo z typu zmiennej, do której przypisujesz wynik). Tak działają List.of,
 *   Optional.of, Collections.max i setki innych metod JDK.
 *
 * ANALOGIA: uniwersalny klucz nasadowy.
 *   Klasa generyczna to cała skrzynka z narzędziami dopasowana do jednego rozmiaru śrub. Metoda generyczna to jeden
 *   klucz z grzechotką, który sam dopasowuje się do śruby, którą mu podasz (wnioskowanie typu).
 *
 * JAK TO DZIAŁA:
 *   static {@code <T>} T firstOrDefault(List{@code <T>} list, T def) {   ← {@code <T>} = „ta metoda ma parametr typu T”
 *       return list.isEmpty() ? def : list.get(0);
 *   }
 *   String s = firstOrDefault(List.of("a", "b"), "brak");     ← T = String (wywnioskowane)
 *   Integer n = firstOrDefault(List.of(), 0);                 ← T = Integer (z drugiego argumentu i typu zmiennej)
 *   Generics03Methods.{@code <String>}emptyList()                 ← jawne podanie typu (type witness) — rzadko potrzebne
 *
 * SŁÓWKA:
 *   generic method = metoda generyczna; infer = wywnioskować; inference = wnioskowanie; type witness = jawny argument typu;
 *   first or default = pierwszy albo domyślny; swap = zamień; repeat = powtórz; map = przekształć; zip = „zamek
 *   błyskawiczny” (łączenie dwóch list w pary); intersection type = typ przecięcia (wspólny nadtyp kilku typów).
 *
 * ZOBACZ TEŻ: t11_generics/Generics02Classes (klasy generyczne), t11_generics/Generics04Bounded (ograniczenia T),
 *             t13_lambdas/Lambda07HigherOrderFunctions (Function jako parametr), t16_streams/Streams03FilterMap (map, filter).
 * </pre>
 */
public class Generics03Methods {

    /** Pair = para (rekord generyczny) — do ćwiczenia zip. */
    record Pair<A, B>(A first, B second) {
    }

    /** Box = pudełko z GENERYCZNĄ METODĄ INSTANCJI map: ma własny parametr R, oprócz T klasy. */
    static final class Box<T> {
        private final T value;

        Box(T value) {
            this.value = value;
        }

        T get() {
            return value;
        }

        /** map = przekształć zawartość funkcją T → R i zwróć nowe pudełko Box{@code <R>}. R należy tylko do tej metody. */
        <R> Box<R> map(Function<T, R> function) {
            return new Box<>(function.apply(value));
        }

        @Override
        public String toString() {
            return "Box(" + value + ")";
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Metody generyczne z lekcji
    // ---------------------------------------------------------------------------------------------

    /** firstOrDefault = pierwszy element albo wartość domyślna, gdy lista jest pusta. */
    static <T> T firstOrDefault(List<T> list, T defaultValue) {
        return list.isEmpty() ? defaultValue : list.get(0);
    }

    /** swap = zamień elementy tablicy dowolnego typu (referencyjnego). */
    static <T> void swap(T[] array, int i, int j) {
        T tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /** repeat = lista z wartością powtórzoną n razy. */
    static <T> List<T> repeat(T value, int times) {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            result.add(value);
        }
        return result;
    }

    /** lastOf = ostatni element w Optional (pusta lista → Optional.empty()). */
    static <T> Optional<T> lastOf(List<T> list) {
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
    }

    /** emptyList = pusta lista dowolnego typu. Typu nie da się wywnioskować z argumentów (bo ich nie ma). */
    static <T> List<T> emptyList() {
        return new ArrayList<>();
    }

    public static void main(String[] args) {
        title("Generics03 — metody generyczne i wnioskowanie typu");

        syntax();                   // syntax = składnia
        inference();                // inference = wnioskowanie
        utilityMethods();           // utility methods = metody narzędziowe
        methodInGenericClass();     // method in generic class = metoda generyczna w klasie generycznej
        jdkExamples();              // JDK examples = przykłady z JDK
        mixedTypesPitfall();        // mixed types pitfall = pułapka typów mieszanych
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. SKŁADNIA
    // =================================================================================================

    /** 1. {@code <T>} przed typem zwracanym deklaruje parametr typu METODY. Ta sama metoda działa dla każdego T. */
    static void syntax() {
        section("1. Składnia: static <T> T metoda(...)");

        show("firstOrDefault(słowa)", firstOrDefault(List.of("java", "kod"), "brak"));
        show("firstOrDefault(pusta)", firstOrDefault(List.of(), "brak"));
        show("firstOrDefault(liczby)", firstOrDefault(List.of(7, 8), 0) + 100);
        // WYNIK: firstOrDefault(słowa) → java
        // WYNIK: firstOrDefault(pusta) → brak
        // WYNIK: firstOrDefault(liczby) → 107    ← T = Integer, więc + 100 to dodawanie, nie sklejanie
    }

    // =================================================================================================
    // 2. WNIOSKOWANIE TYPU
    // =================================================================================================

    /** 2. Kompilator ustala T z argumentów i z typu, do którego przypisujesz wynik. Gdy się nie da — podajesz jawnie. */
    static void inference() {
        section("2. Wnioskowanie typu i jawne podanie typu");

        List<String> a = emptyList();                        // T = String — z typu zmiennej po lewej
        a.add("ok");
        show("emptyList() jako List<String>", a);
        // WYNIK: emptyList() jako List<String> → [ok]

        int size = Generics03Methods.<Integer>emptyList().size();   // jawne <Integer> — bo nie ma skąd wywnioskować
        show("jawne <Integer>", size);
        // WYNIK: jawne <Integer> → 0

        // PUŁAPKA: jawne podanie typu wymaga „czegoś przed kropką”: Generics03Methods.<Integer>emptyList() albo
        //   this.<Integer>metoda(). Samo <Integer>emptyList() to błąd składni.
    }

    // =================================================================================================
    // 3. METODY NARZĘDZIOWE
    // =================================================================================================

    /** 3. Metody generyczne w klasie NIEgenerycznej — typowe „narzędzia” (utility methods). */
    static void utilityMethods() {
        section("3. Generyczne metody narzędziowe");

        String[] names = {"Ala", "Ola", "Ula"};
        swap(names, 0, 2);
        show("swap(names, 0, 2)", names);
        // WYNIK: swap(names, 0, 2) → [Ula, Ola, Ala]

        Integer[] numbers = {1, 2, 3};
        swap(numbers, 0, 1);
        show("swap(numbers, 0, 1)", numbers);
        // WYNIK: swap(numbers, 0, 1) → [2, 1, 3]

        show("repeat(\"ho\", 3)", repeat("ho", 3));
        show("lastOf([5, 6, 7])", lastOf(List.of(5, 6, 7)));
        show("lastOf([])", lastOf(List.of()));
        // WYNIK: repeat("ho", 3) → [ho, ho, ho]
        // WYNIK: lastOf([5, 6, 7]) → Optional[7]
        // WYNIK: lastOf([]) → Optional.empty

        // PUŁAPKA: swap(T[] ...) nie zadziała z int[] — T musi być typem referencyjnym (int[] to nie Integer[]).
    }

    // =================================================================================================
    // 4. METODA GENERYCZNA W KLASIE GENERYCZNEJ
    // =================================================================================================

    /** 4. Box{@code <T>}.map ma WŁASNY parametr R: z Box{@code <String>} może zrobić Box{@code <Integer>}. */
    static void methodInGenericClass() {
        section("4. <R> Box<R> map(Function<T, R>)");

        Box<String> word = new Box<>("generyki");
        Box<Integer> length = word.map(String::length);          // T = String, R = Integer
        Box<Boolean> isLong = length.map(n -> n > 5);             // T = Integer, R = Boolean
        show("word", word);
        show("length", length);
        show("isLong", isLong);
        // WYNIK: word → Box(generyki)
        // WYNIK: length → Box(8)
        // WYNIK: isLong → Box(true)

        // Tak samo działają Optional.map i Stream.map: <R> Optional<R> map(Function<? super T, ? extends R> mapper).
        //   Znaki ? super / ? extends wyjaśnia Generics05Wildcards.
    }

    // =================================================================================================
    // 5. PRZYKŁADY Z JDK
    // =================================================================================================

    /** 5. Metody generyczne używasz na co dzień — często nie zauważając, że T jest wnioskowane. */
    static void jdkExamples() {
        section("5. Metody generyczne z JDK");

        List<String> fruits = List.of("gruszka", "jabłko", "banan");         // static <E> List<E> of(E... elements)
        show("Collections.max(fruits)", Collections.max(fruits));            // static <T ...> T max(Collection<...> coll)
        show("Objects.requireNonNullElse(null, \"domyślny\")",
                Objects.requireNonNullElse(null, "domyślny"));                // static <T> T requireNonNullElse(T obj, T def)
        show("Collections.nCopies(3, 'x')", Collections.nCopies(3, 'x'));   // static <T> List<T> nCopies(int n, T o)
        // WYNIK: Collections.max(fruits) → jabłko
        // WYNIK: Objects.requireNonNullElse(null, "domyślny") → domyślny
        // WYNIK: Collections.nCopies(3, 'x') → [x, x, x]

        // PUŁAPKA: Collections.max porównuje napisy wg kodów Unicode — „jabłko” > „gruszka” > „banan” tu się zgadza,
        //   ale polskie litery (ą, ł, ż) trafiają na koniec. Do polskiego porządku: Collator (t04_strings/Strings02Methods).
    }

    // =================================================================================================
    // 6. PUŁAPKA: WNIOSKOWANIE PRZY RÓŻNYCH TYPACH
    // =================================================================================================

    /**
     * 6. Gdy argumenty mają różne typy, kompilator szuka ich WSPÓLNEGO nadtypu. Dla 1 i 2.5 to „Number i Comparable”
     * (typ przecięcia). Kod się kompiluje, ale typ jest mało użyteczny — i łatwo przegapić pomyłkę.
     */
    static void mixedTypesPitfall() {
        section("6. Pułapka: mieszane typy argumentów");

        var mixed = Arrays.asList(1, 2.5, 3L);        // T = Number & Comparable<...> — wspólny nadtyp
        show("mixed", mixed);
        show("mixed.get(0).getClass()", mixed.get(0).getClass().getSimpleName());
        show("mixed.get(1).getClass()", mixed.get(1).getClass().getSimpleName());
        // WYNIK: mixed → [1, 2.5, 3]
        // WYNIK: mixed.get(0).getClass() → Integer
        // WYNIK: mixed.get(1).getClass() → Double

        double sum = 0;
        for (Number n : mixed) {                       // da się iterować jako Number
            sum += n.doubleValue();
        }
        show("suma jako double", sum);
        // WYNIK: suma jako double → 6.5

        // DOBRA PRAKTYKA: gdy chcesz konkretnego typu, powiedz to wprost: List<Double> d = List.of(1.0, 2.5, 3.0);
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • static <T> T metoda(List<T> lista, T x) — <T> PRZED typem zwracanym.
     *   • T wnioskowane z argumentów i z typu zmiennej; jawnie: Klasa.<Typ>metoda().
     *   • Kilka parametrów: static <K, V> ..., <T, R> List<R> mapAll(List<T>, Function<T, R>).
     *   • W klasie generycznej metoda może mieć WŁASNY parametr: <R> Box<R> map(Function<T, R> f).
     *   • JDK: List.of, Optional.of, Collections.max, Objects.requireNonNullElse, Arrays.asList.
     *   • Różne typy argumentów → wspólny nadtyp (np. Number & Comparable) — lepiej podać typ wprost.
     *
     * PYTANIA KONTROLNE:
     *   1. Gdzie w deklaracji metody generycznej stoi <T>?
     *   2. Co wypisze:  System.out.println(firstOrDefault(List.of(), 5) + 1);  ?
     *   3. ZNAJDŹ BŁĄD:  int[] t = {1, 2, 3}; swap(t, 0, 1);
     *   4. Kiedy trzeba jawnie podać argument typu (Klasa.<Typ>metoda())?
     *   5. Co wypisze:  System.out.println(new Box<>("abc").map(String::length).map(n -> n * 2));  ?
     *   6. Czym różni się <T> w class Box<T> od <R> w metodzie <R> Box<R> map(...)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: ostatni napis", "c", () -> exercise1(List.of("a", "b", "c"), "brak"));
        Check.equal("ćw. 1b: pusta lista liczb", -1, () -> exercise1(List.<Integer>of(), -1));
        Check.equal("ćw. 2: powtórz każdy", List.of("a", "a", "b", "b"), () -> exercise2(List.of("a", "b"), 2));
        Check.equal("ćw. 3: ile spełnia warunek", 2, () -> exercise3(List.of(3, 8, 10, 1), n -> n > 5));
        Check.equal("ćw. 4: własne map", List.of(4, 2, 7), () -> exercise4(List.of("java", "ok", "generyk"), String::length));
        Check.equal("ćw. 5: zip", "[Pair[first=a, second=1], Pair[first=b, second=2]]",
                () -> String.valueOf(exercise5(List.of("a", "b", "c"), List.of(1, 2))));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", "c", () -> solution1(List.of("a", "b", "c"), "brak"));
        Check.equal("ćw. 1b (wzorzec)", -1, () -> solution1(List.<Integer>of(), -1));
        Check.equal("ćw. 2 (wzorzec)", List.of("a", "a", "b", "b"), () -> solution2(List.of("a", "b"), 2));
        Check.equal("ćw. 3 (wzorzec)", 2, () -> solution3(List.of(3, 8, 10, 1), n -> n > 5));
        Check.equal("ćw. 4 (wzorzec)", List.of(4, 2, 7), () -> solution4(List.of("java", "ok", "generyk"), String::length));
        Check.equal("ćw. 5 (wzorzec)", "[Pair[first=a, second=1], Pair[first=b, second=2]]",
                () -> String.valueOf(solution5(List.of("a", "b", "c"), List.of(1, 2))));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): ostatni element listy albo wartość domyślna, gdy lista jest pusta. Sygnatura już gotowa. */
    static <T> T exercise1(List<T> list, T defaultValue) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** ĆWICZENIE 2 (łatwe): każdy element powtórzony times razy, z zachowaniem kolejności: [a, b], 2 → [a, a, b, b]. */
    static <T> List<T> exercise2(List<T> items, int times) {
        // TODO: twoje rozwiązanie
        return new ArrayList<>();
    }

    /** ĆWICZENIE 3 (średnie): policz elementy spełniające warunek (test.test(x)). Predicate{@code <T>} — t13_lambdas. */
    static <T> int exercise3(List<T> items, Predicate<T> test) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (średnie): napisz własne „map”: nowa lista z wynikami function.apply(x) dla każdego elementu.
     * Tak działa w środku Stream.map (t16_streams).
     */
    static <T, R> List<R> exercise4(List<T> items, Function<T, R> function) {
        // TODO: twoje rozwiązanie
        return new ArrayList<>();
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): „zip” — połącz dwie listy w pary (Pair{@code <A, B>}) po indeksach; długość wyniku =
     * długość KRÓTSZEJ listy. [a, b, c] + [1, 2] → [Pair(a, 1), Pair(b, 2)]. Podpowiedź: Math.min(a.size(), b.size()).
     */
    static <A, B> List<Pair<A, B>> exercise5(List<A> first, List<B> second) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static <T> T solution1(List<T> list, T defaultValue) {
        return list.isEmpty() ? defaultValue : list.get(list.size() - 1);
    }

    static <T> List<T> solution2(List<T> items, int times) {
        List<T> result = new ArrayList<>();
        for (T item : items) {
            for (int i = 0; i < times; i++) {
                result.add(item);
            }
        }
        return result;
    }

    static <T> int solution3(List<T> items, Predicate<T> test) {
        int count = 0;
        for (T item : items) {
            if (test.test(item)) {
                count++;
            }
        }
        return count;
    }

    static <T, R> List<R> solution4(List<T> items, Function<T, R> function) {
        List<R> result = new ArrayList<>();
        for (T item : items) {
            result.add(function.apply(item));
        }
        return result;
    }

    static <A, B> List<Pair<A, B>> solution5(List<A> first, List<B> second) {
        List<Pair<A, B>> result = new ArrayList<>();
        int n = Math.min(first.size(), second.size());
        for (int i = 0; i < n; i++) {
            result.add(new Pair<>(first.get(i), second.get(i)));
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Po modyfikatorach (public, static), a PRZED typem zwracanym: public static <T> T metoda(...).
     *   2. „6” — lista pusta, więc wynik to 5 (T = Integer), a + 1 to dodawanie.
     *   3. T musi być typem referencyjnym, a int[] to tablica typu prostego — nie pasuje do T[]. Trzeba Integer[].
     *   4. Gdy kompilator nie ma skąd wywnioskować typu — np. metoda bez argumentów użyta w wyrażeniu, a nie przypisana
     *      do zmiennej o znanym typie (Generics03Methods.<Integer>emptyList().size()).
     *   5. „Box(6)” — "abc" → 3 → 6.
     *   6. T należy do KLASY (ustalone przy tworzeniu obiektu, wspólne dla wszystkich jego metod). R należy tylko do
     *      tej jednej METODY i jest ustalane osobno przy każdym wywołaniu.
     */
    // </editor-fold>
}
