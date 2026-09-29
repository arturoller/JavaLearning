package t11_generics;

import helpers.Check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Symbole wieloznaczne (wildcards): {@code ?}, {@code ? extends T}, {@code ? super T} i zasada PECS
 *        (wildcard = symbol wieloznaczny, „dżoker”; invariance = niezmienniczość; producer = producent; consumer = konsument)
 *
 * W SKRÓCIE:
 *   Choć Integer jest podtypem Number, {@code List<Integer>} NIE jest podtypem {@code List<Number>} (generyki są
 *   niezmiennicze). Gdyby był, można by do listy liczb całkowitych dopisać 2.5. Żeby jedna metoda przyjęła różne listy,
 *   używamy dżokerów: {@code List<? extends Number>} — „lista czegoś, co jest Number” (bezpiecznie CZYTAMY Number),
 *   {@code List<? super Integer>} — „lista czegoś, co jest nadtypem Integer” (bezpiecznie DOPISUJEMY Integer).
 *   Reguła PECS: Producer Extends, Consumer Super — z czego czytasz: extends; do czego piszesz: super.
 *
 * ANALOGIA: kosz z owocami.
 *   „Kosz z jakimiś owocami” (? extends Owoc): możesz z niego WYJĄĆ owoc, ale nie wolno Ci nic dołożyć — może to kosz
 *   tylko na jabłka, a Ty masz gruszkę. „Kosz, do którego wolno wkładać jabłka” (? super Jabłko): WŁOŻYSZ jabłko
 *   (kosz przyjmuje jabłka albo w ogóle owoce, albo cokolwiek), ale wyjmując nie wiesz, co dostaniesz (Object).
 *
 * JAK TO DZIAŁA:
 *   List{@code <Number>} nums = new ArrayList{@code <Integer>}();          ← BŁĄD KOMPILACJI (niezmienniczość)
 *   double sum(List{@code <? extends Number>} list)   ← przyjmie List{@code <Integer>}, List{@code <Double>}; czyta Number; add — zabronione
 *   void fill(List{@code <? super Integer>} list)     ← przyjmie List{@code <Integer>}, List{@code <Number>}, List{@code <Object>}; add(Integer) — OK
 *   int size(List{@code <?>} list)                    ← dowolna lista; czyta Object
 *
 * SŁÓWKA:
 *   wildcard = symbol wieloznaczny (dżoker); unbounded = nieograniczony; upper bound = ograniczenie górne (extends);
 *   lower bound = ograniczenie dolne (super); invariant = niezmienniczy; covariant = kowariantny (zmienia się „razem”);
 *   producer = producent (źródło danych); consumer = konsument (cel danych); PECS = Producer Extends, Consumer Super;
 *   copy = kopiuj; destination = cel; source = źródło.
 *
 * ZOBACZ TEŻ: t11_generics/Generics04Bounded (ograniczenia), t11_generics/Generics06ErasureLimits (wymazywanie),
 *             t12_collections/Collections07ComparableComparator (Comparator{@code <? super T>}), t16_streams/Streams10CollectorsToMap.
 * </pre>
 */
public class Generics05Wildcards {

    public static void main(String[] args) {
        title("Generics05 — ?, ? extends, ? super, PECS");

        invariance();               // invariance = niezmienniczość
        arraysAreCovariant();       // arrays are covariant = tablice są kowariantne
        unboundedWildcard();        // unbounded wildcard = dżoker bez ograniczeń
        extendsWildcard();          // extends wildcard = ? extends (czytanie)
        superWildcard();            // super wildcard = ? super (pisanie)
        pecs();                     // PECS = Producer Extends, Consumer Super
        jdkSignatures();            // JDK signatures = sygnatury w JDK
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. NIEZMIENNICZOŚĆ
    // =================================================================================================

    /** 1. Integer jest Number, ale {@code List<Integer>} nie jest {@code List<Number>}. Kompilator broni listy przed „obcymi”. */
    static void invariance() {
        section("1. List<Integer> to nie List<Number>");

        List<Integer> integers = new ArrayList<>(List.of(1, 2));
        // List<Number> numbers = integers;        ← BŁĄD KOMPILACJI: incompatible types
        // Gdyby to przeszło:  numbers.add(2.5);   → w liście integers byłby Double,
        //                     int x = integers.get(2);  → ClassCastException gdzieś daleko.
        Number first = integers.get(0);           // pojedynczy element Integer JEST Number — to działa
        show("pierwszy jako Number", first);
        // WYNIK: pierwszy jako Number → 1
    }

    // =================================================================================================
    // 2. TABLICE SĄ KOWARIANTNE (i dlatego mniej bezpieczne)
    // =================================================================================================

    /**
     * 2. Tablice działają odwrotnie: Integer[] JEST Number[] (kowariancja). Kompilator to przepuszcza, a błąd wychodzi
     * dopiero w działaniu: ArrayStoreException. Generyki wybrały bezpieczeństwo w czasie kompilacji.
     */
    static void arraysAreCovariant() {
        section("2. Tablice: Integer[] to Number[] — błąd dopiero w działaniu");

        Number[] numbers = new Integer[3];        // kompiluje się!
        numbers[0] = 1;
        expectThrows("numbers[1] = 2.5", () -> numbers[1] = 2.5);
        // WYNIK: ✔ numbers[1] = 2.5 → rzucono ArrayStoreException: java.lang.Double
    }

    // =================================================================================================
    // 3. List<?> — DOWOLNA LISTA
    // =================================================================================================

    /** describe = opisz dowolną listę. {@code List<?>} = „lista nieznanego typu”: czytamy Object, nie dopisujemy. */
    static String describe(List<?> list) {
        // list.add("x");                         ← BŁĄD KOMPILACJI: nie wiadomo, jakiego typu elementy przyjmuje
        Object first = list.isEmpty() ? null : list.get(0);
        return list.size() + " el., pierwszy: " + first;
    }

    /** 3. {@code List<?>} — gdy metoda NIE interesuje się typem elementów (rozmiar, wypisanie, null-e). */
    static void unboundedWildcard() {
        section("3. List<?> — lista czegokolwiek");

        show("napisy", describe(List.of("a", "b")));
        show("liczby", describe(List.of(1.5, 2.5, 3.5)));
        // WYNIK: napisy → 2 el., pierwszy: a
        // WYNIK: liczby → 3 el., pierwszy: 1.5

        // PUŁAPKA: List<?> to NIE to samo co List<Object>. Do List<Object> nie przekażesz List<String> (niezmienniczość),
        //   a do List<?> — tak.
    }

    // =================================================================================================
    // 4. ? extends — CZYTANIE
    // =================================================================================================

    /** sumAll = suma. {@code ? extends Number}: lista jest PRODUCENTEM Number — czytamy, nie dopisujemy. */
    static double sumAll(List<? extends Number> numbers) {
        double total = 0;
        for (Number n : numbers) {                // każdy element na pewno JEST Number
            total += n.doubleValue();
        }
        // numbers.add(1);                         ← BŁĄD KOMPILACJI: to może być List<Double>
        return total;
    }

    /** 4. Jedna metoda przyjmuje {@code List<Integer>}, {@code List<Double>}, {@code List<Number>}... */
    static void extendsWildcard() {
        section("4. ? extends Number — czytamy Number");

        List<Integer> ints = List.of(1, 2, 3);
        List<Double> doubles = List.of(0.5, 1.5);
        show("sumAll(List<Integer>)", sumAll(ints));
        show("sumAll(List<Double>)", sumAll(doubles));
        // WYNIK: sumAll(List<Integer>) → 6.0
        // WYNIK: sumAll(List<Double>) → 2.0

        // Porównaj z Generics04Bounded: <N extends Number> double sum(List<N>) robi to samo. Gdy T występuje TYLKO
        //   raz (w parametrze), dżoker jest prostszy do przeczytania.
    }

    // =================================================================================================
    // 5. ? super — PISANIE
    // =================================================================================================

    /** addRange = dopisz liczby from..to. {@code ? super Integer}: lista jest KONSUMENTEM Integer — dopisujemy. */
    static void addRange(List<? super Integer> target, int from, int to) {
        for (int i = from; i <= to; i++) {
            target.add(i);                        // bezpieczne: lista przyjmuje Integer albo jego nadtyp
        }
        // Integer x = target.get(0);             ← BŁĄD KOMPILACJI: czytając, wiemy tylko, że to Object
    }

    /** 5. Do listy liczb, listy Number i listy Object — wszędzie da się dopisać Integer. */
    static void superWildcard() {
        section("5. ? super Integer — dopisujemy Integer");

        List<Integer> ints = new ArrayList<>();
        List<Number> numbers = new ArrayList<>(List.of(0.5));
        List<Object> objects = new ArrayList<>(List.of("start"));
        addRange(ints, 1, 3);
        addRange(numbers, 1, 2);
        addRange(objects, 7, 8);
        show("List<Integer>", ints);
        show("List<Number>", numbers);
        show("List<Object>", objects);
        // WYNIK: List<Integer> → [1, 2, 3]
        // WYNIK: List<Number> → [0.5, 1, 2]
        // WYNIK: List<Object> → [start, 7, 8]

        // addRange(new ArrayList<Double>(), 1, 2);  ← BŁĄD KOMPILACJI: Double nie jest nadtypem Integer
    }

    // =================================================================================================
    // 6. PECS
    // =================================================================================================

    /**
     * copy = kopiuj ze źródła (PRODUCENT → extends) do celu (KONSUMENT → super). Dokładnie taką sygnaturę ma
     * Collections.copy w JDK: {@code <T> void copy(List<? super T> dest, List<? extends T> src)}.
     */
    static <T> void copy(List<? super T> destination, List<? extends T> source) {
        for (T item : source) {
            destination.add(item);
        }
    }

    /** 6. PECS: Producer Extends, Consumer Super. Dzięki temu copy działa dla wielu kombinacji typów. */
    static void pecs() {
        section("6. PECS: Producer Extends, Consumer Super");

        List<Integer> source = List.of(10, 20);
        List<Number> destination = new ArrayList<>(List.of(1.5));
        copy(destination, source);                // T = Integer: źródło List<Integer>, cel List<Number>
        show("po copy", destination);
        // WYNIK: po copy → [1.5, 10, 20]

        note("czytasz z parametru → ? extends T;  zapisujesz do parametru → ? super T;  jedno i drugie → samo T");
        // WYNIK:    ℹ czytasz z parametru → ? extends T;  zapisujesz do parametru → ? super T;  jedno i drugie → samo T
    }

    // =================================================================================================
    // 7. SYGNATURY Z JDK
    // =================================================================================================

    /**
     * 7. List.sort(Comparator{@code <? super E>}) — listę Integer można posortować komparatorem dla Number,
     * bo komparator „konsumuje” elementy (super). Dzięki temu jeden komparator obsłuży wiele typów liczb.
     */
    static void jdkSignatures() {
        section("7. Comparator<? super T> w praktyce");

        Comparator<Number> byAbsoluteValue = Comparator.comparingDouble(n -> Math.abs(n.doubleValue()));
        List<Integer> ints = new ArrayList<>(Arrays.asList(-7, 3, -1, 5));
        List<Double> doubles = new ArrayList<>(Arrays.asList(2.5, -0.5, -4.0));
        ints.sort(byAbsoluteValue);               // Comparator<Number> pasuje do Comparator<? super Integer>
        doubles.sort(byAbsoluteValue);            // ...i do Comparator<? super Double>
        show("ints wg |x|", ints);
        show("doubles wg |x|", doubles);
        // WYNIK: ints wg |x| → [-1, 3, 5, -7]
        // WYNIK: doubles wg |x| → [-0.5, 2.5, -4.0]

        // Inne przykłady: Collections.max(Collection<? extends T>), Stream.map(Function<? super T, ? extends R>),
        //   Collections.addAll(Collection<? super T>, T...). Czytaj je z PECS w głowie — od razu wiadomo, co przyjmą.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Generyki są niezmiennicze: List<Integer> NIE jest List<Number>. Tablice są kowariantne (ArrayStoreException).
     *   • List<?> — dowolna lista; czytasz Object; nie dopisujesz (poza null).
     *   • List<? extends Number> — czytasz Number; nie dopisujesz. Źródło danych (Producer).
     *   • List<? super Integer> — dopisujesz Integer; czytasz Object. Cel danych (Consumer).
     *   • PECS: Producer Extends, Consumer Super; oba kierunki → zwykłe T.
     *   • JDK: copy(List<? super T>, List<? extends T>), sort(Comparator<? super E>), map(Function<? super T, ? extends R>).
     *   • Typ użyty raz → dżoker; typ łączy parametry/wynik → <T>.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego List<Integer> nie jest podtypem List<Number>, skoro Integer jest podtypem Number?
     *   2. ZNAJDŹ BŁĄD:  void addOne(List<? extends Number> list) { list.add(1); }
     *   3. Co wypisze:  List<Object> o = new ArrayList<>(); addRange(o, 1, 2); System.out.println(o);  ?
     *   4. Jaką sygnaturę dasz metodzie, która przenosi elementy z jednej listy do drugiej? Dlaczego?
     *   5. Czym różni się List<?> od List<Object>?
     *   6. Co się stanie:  Object[] arr = new String[1]; arr[0] = 1;  ?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: liczba nulli", 2, () -> exercise1(Arrays.asList("a", null, "b", null)));
        Check.equal("ćw. 2: największa jako double", 9.5, () -> exercise2(List.of(3, 7)) + exercise2(List.of(2.5)));
        Check.equal("ćw. 3: dopisz kwadraty", List.of("start", 1, 4, 9), () -> {
            List<Object> target = new ArrayList<>(List.of("start"));
            exercise3(target, 3);
            return target;
        });
        Check.equal("ćw. 4: copyInto", List.of(0.5, 1, 2), () -> {
            List<Number> dest = new ArrayList<>(List.of(0.5));
            exercise4(dest, List.of(1, 2));
            return dest;
        });
        Check.equal("ćw. 5: ile większych od progu", 2, () -> exercise5(List.of(5, 12, 8, 20), 10));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 2, () -> solution1(Arrays.asList("a", null, "b", null)));
        Check.equal("ćw. 2 (wzorzec)", 9.5, () -> solution2(List.of(3, 7)) + solution2(List.of(2.5)));
        Check.equal("ćw. 3 (wzorzec)", List.of("start", 1, 4, 9), () -> {
            List<Object> target = new ArrayList<>(List.of("start"));
            solution3(target, 3);
            return target;
        });
        Check.equal("ćw. 4 (wzorzec)", List.of(0.5, 1, 2), () -> {
            List<Number> dest = new ArrayList<>(List.of(0.5));
            solution4(dest, List.of(1, 2));
            return dest;
        });
        Check.equal("ćw. 5 (wzorzec)", 2, () -> solution5(List.of(5, 12, 8, 20), 10));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): policz elementy równe null w DOWOLNEJ liście (sygnatura z {@code List<?>}). */
    static int exercise1(List<?> list) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /** ĆWICZENIE 2 (łatwe): największa wartość z listy liczb dowolnego typu, jako double (lista niepusta). */
    static double exercise2(List<? extends Number> numbers) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /** ĆWICZENIE 3 (średnie): dopisz do listy kwadraty liczb 1..n (1, 4, 9...). Sygnatura z {@code ? super Integer}. */
    static void exercise3(List<? super Integer> target, int n) {
        // TODO: twoje rozwiązanie
    }

    /** ĆWICZENIE 4 (średnie): skopiuj wszystkie elementy ze źródła do celu (PECS — sygnatura gotowa). */
    static <T> void exercise4(List<? super T> destination, List<? extends T> source) {
        // TODO: twoje rozwiązanie
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): policz elementy większe od progu. Sygnatura w stylu JDK:
     * {@code <T extends Comparable<? super T>>} — T porównywalne ze sobą albo z nadtypem. Podpowiedź: compareTo(threshold) > 0.
     */
    static <T extends Comparable<? super T>> int exercise5(List<? extends T> list, T threshold) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(List<?> list) {
        int nulls = 0;
        for (Object o : list) {
            if (o == null) {
                nulls++;
            }
        }
        return nulls;
    }

    static double solution2(List<? extends Number> numbers) {
        double max = -Double.MAX_VALUE;
        for (Number n : numbers) {
            max = Math.max(max, n.doubleValue());
        }
        return max;
    }

    static void solution3(List<? super Integer> target, int n) {
        for (int i = 1; i <= n; i++) {
            target.add(i * i);
        }
    }

    static <T> void solution4(List<? super T> destination, List<? extends T> source) {
        destination.addAll(source);              // addAll(Collection<? extends E>) — też PECS
    }

    static <T extends Comparable<? super T>> int solution5(List<? extends T> list, T threshold) {
        int count = 0;
        for (T x : list) {
            if (x.compareTo(threshold) > 0) {
                count++;
            }
        }
        return count;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Gdyby był, można by przypisać List<Integer> do zmiennej List<Number> i dopisać przez nią Double — w liście
     *      Integerów znalazłby się obcy element. Niezmienniczość chroni przed tym już podczas kompilacji.
     *   2. Do List<? extends Number> nie można dopisywać — to może być List<Double>, a 1 to Integer. Do dopisywania
     *      potrzebne ? super Integer.
     *   3. „[1, 2]”.
     *   4. <T> void move(List<? super T> dest, List<? extends T> src) — ze źródła czytamy (extends), do celu piszemy (super).
     *   5. List<?> to „lista jakiegoś nieznanego typu” — przyjmie List<String>, List<Integer>..., ale nie pozwoli dopisać.
     *      List<Object> to konkretnie lista Object — przyjmie tylko List<Object>, ale pozwala dopisać cokolwiek.
     *   6. Kompiluje się (tablice są kowariantne), ale w działaniu poleci ArrayStoreException: java.lang.Integer.
     */
    // </editor-fold>
}
