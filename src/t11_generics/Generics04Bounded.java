package t11_generics;

import helpers.Check;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Ograniczenia typu (bounded types): {@code <T extends Comparable<T>>}, {@code <N extends Number>}, kilka ograniczeń
 *        (bound = ograniczenie; upper bound = ograniczenie górne; extends = rozszerza / jest podtypem)
 *
 * W SKRÓCIE:
 *   Zwykłe T może być CZYMKOLWIEK, więc kompilator pozwala wywołać na nim tylko metody z Object (equals, toString).
 *   Ograniczenie {@code <T extends Comparable<T>>} mówi: „T to dowolny typ, ale taki, który umie się porównywać” —
 *   i wtedy można wywołać compareTo. {@code <N extends Number>} pozwala użyć doubleValue(). W ograniczeniach słowo
 *   extends oznacza „jest podtypem” — także dla interfejsów. Kilka ograniczeń łączy się znakiem &.
 *
 * ANALOGIA: ogłoszenie o pracę.
 *   „Szukamy osoby” (T) — przyjdzie każdy, ale nie wiadomo, co umie. „Szukamy osoby z prawem jazdy kat. B”
 *   ({@code T extends Kierowca}) — nadal wielu kandydatów, ale każdemu można dać kluczyki (wywołać drive()).
 *
 * JAK TO DZIAŁA:
 *   static {@code <T extends Comparable<T>>} T max(List{@code <T>} list) {
 *       T best = list.get(0);
 *       for (T x : list) if (x.compareTo(best) > 0) best = x;    ← compareTo dostępne dzięki ograniczeniu
 *       return best;
 *   }
 *   max(List.of(3, 9, 4)) → 9;   max(List.of("kot", "pies")) → "pies";   max(listaObiektówBezComparable) → BŁĄD KOMPILACJI
 *
 * SŁÓWKA:
 *   bound = ograniczenie; bounded = ograniczony; extends = rozszerza (tu: jest podtypem); comparable = porównywalny;
 *   compare to = porównaj z; number = liczba; clamp = przytnij do zakresu; priced = mający cenę; cheapest = najtańszy;
 *   average = średnia; stats (statistics) = statystyki; sorted = posortowany.
 *
 * ZOBACZ TEŻ: t11_generics/Generics03Methods (metody generyczne), t11_generics/Generics05Wildcards (? extends, ? super),
 *             t12_collections/Collections07ComparableComparator (Comparable i Comparator), t17_datetime/DateTime01LocalDateTime.
 * </pre>
 */
public class Generics04Bounded {

    // ---------------------------------------------------------------------------------------------
    // Metody z ograniczeniami
    // ---------------------------------------------------------------------------------------------

    /** max = największy element. Działa dla każdego typu porównywalnego: Integer, String, LocalDate... */
    static <T extends Comparable<T>> T max(List<T> list) {
        T best = list.get(0);
        for (T x : list) {
            if (x.compareTo(best) > 0) {
                best = x;
            }
        }
        return best;
    }

    /** sum = suma liczb dowolnego typu liczbowego. doubleValue() pochodzi z klasy Number. */
    static <N extends Number> double sum(List<N> numbers) {
        double total = 0;
        for (N n : numbers) {
            total += n.doubleValue();
        }
        return total;
    }

    /**
     * atMost = nie więcej niż limit. Dwa ograniczenia naraz: T jest liczbą (Number) I jest porównywalne (Comparable).
     * Zasada: najpierw klasa (co najwyżej jedna), potem interfejsy — połączone znakiem &.
     */
    static <T extends Number & Comparable<T>> T atMost(T value, T limit) {
        return value.compareTo(limit) > 0 ? limit : value;
    }

    // ---------------------------------------------------------------------------------------------
    // Klasa z ograniczonym parametrem
    // ---------------------------------------------------------------------------------------------

    /** Stats = statystyki listy liczb. N extends Number — nie da się utworzyć Stats{@code <String>}. */
    static final class Stats<N extends Number> {
        private final List<N> values;

        Stats(List<N> values) {
            this.values = List.copyOf(values);
        }

        double average() {
            return sum(values) / values.size();
        }

        double range() {                               // range = rozstęp (max - min)
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (N v : values) {
                min = Math.min(min, v.doubleValue());
                max = Math.max(max, v.doubleValue());
            }
            return max - min;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Ograniczenie własnym interfejsem
    // ---------------------------------------------------------------------------------------------

    /** Priced = coś, co ma cenę (w groszach). */
    interface Priced {
        int priceGrosze();
    }

    /** Book = książka z ceną. Metoda dostępu rekordu priceGrosze() spełnia interfejs Priced. */
    record Book(String title, int priceGrosze) implements Priced {
    }

    /** Coffee = kawa z ceną. */
    record Coffee(String name, int priceGrosze) implements Priced {
    }

    /**
     * cheapest = najtańszy. Zwraca T — ten SAM typ, który przyszedł (Book, Coffee), a nie ogólne Priced.
     * Dzięki temu wywołujący może od razu użyć pól książki (title()) bez rzutowania.
     */
    static <T extends Priced> T cheapest(List<T> items) {
        T best = items.get(0);
        for (T item : items) {
            if (item.priceGrosze() < best.priceGrosze()) {
                best = item;
            }
        }
        return best;
    }

    public static void main(String[] args) {
        title("Generics04 — ograniczenia typu");

        problemWithoutBound();      // problem without bound = problem bez ograniczenia
        comparableBound();          // Comparable bound = ograniczenie Comparable
        numberBound();              // Number bound = ograniczenie Number
        boundedClass();             // bounded class = klasa z ograniczeniem
        multipleBounds();           // multiple bounds = kilka ograniczeń
        ownInterfaceBound();        // own interface bound = ograniczenie własnym interfejsem
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PROBLEM BEZ OGRANICZENIA
    // =================================================================================================

    /** 1. Bez ograniczenia T to „cokolwiek” — kompilator nie pozwoli wywołać compareTo ani doubleValue. */
    static void problemWithoutBound() {
        section("1. Bez ograniczenia: T zna tylko metody Object");

        // static <T> T maxBroken(List<T> list) {
        //     ... if (x.compareTo(best) > 0) ...      ← BŁĄD KOMPILACJI: cannot find symbol: method compareTo(T)
        // }
        note("T bez ograniczenia = Object: dostępne tylko equals, hashCode, toString, getClass");
        // WYNIK:    ℹ T bez ograniczenia = Object: dostępne tylko equals, hashCode, toString, getClass
    }

    // =================================================================================================
    // 2. <T extends Comparable<T>>
    // =================================================================================================

    /** 2. Jedna metoda max dla liczb, napisów i dat — każda z tych klas implementuje Comparable. */
    static void comparableBound() {
        section("2. <T extends Comparable<T>>");

        show("max(liczby)", max(List.of(3, 9, 4)));
        show("max(napisy)", max(List.of("kot", "pies", "chomik")));
        show("max(daty)", max(List.of(LocalDate.of(2024, 5, 1), LocalDate.of(2025, 1, 15), LocalDate.of(2023, 12, 31))));
        // WYNIK: max(liczby) → 9
        // WYNIK: max(napisy) → pies
        // WYNIK: max(daty) → 2025-01-15

        // max(List.of(new Object(), new Object()));   ← BŁĄD KOMPILACJI: Object nie implementuje Comparable
        // PUŁAPKA: porządek napisów to porządek Unicode — polskie litery (ą, ś, ż) trafiają za „z”.
    }

    // =================================================================================================
    // 3. <N extends Number>
    // =================================================================================================

    /** 3. Number to klasa bazowa Integer, Long, Double, BigDecimal... — ograniczenie daje dostęp do doubleValue(). */
    static void numberBound() {
        section("3. <N extends Number>");

        show("sum(Integer)", sum(List.of(1, 2, 3)));
        show("sum(Double)", sum(List.of(0.5, 0.25)));
        show("sum(Long)", sum(List.of(10_000_000_000L, 1L)));
        // WYNIK: sum(Integer) → 6.0
        // WYNIK: sum(Double) → 0.75
        // WYNIK: sum(Long) → 1.0000000001E10

        // sum(List.of("1", "2"));   ← BŁĄD KOMPILACJI: String nie jest podtypem Number
        // PUŁAPKA: doubleValue() dla BigDecimal traci dokładność — do pieniędzy nie sumuj przez double (t15_numbers).
    }

    // =================================================================================================
    // 4. KLASA Z OGRANICZENIEM
    // =================================================================================================

    /** 4. Ograniczenie w klasie: class Stats{@code <N extends Number>} — każda metoda klasy może liczyć na N. */
    static void boundedClass() {
        section("4. class Stats<N extends Number>");

        Stats<Integer> grades = new Stats<>(List.of(3, 4, 5, 5));
        show("średnia ocen", grades.average());
        show("rozstęp ocen", grades.range());
        // WYNIK: średnia ocen → 4.25
        // WYNIK: rozstęp ocen → 2.0

        // Stats<String> s;   ← BŁĄD KOMPILACJI: type argument String is not within bounds of type-variable N
    }

    // =================================================================================================
    // 5. KILKA OGRANICZEŃ
    // =================================================================================================

    /** 5. {@code <T extends Number & Comparable<T>>} — T musi spełniać OBA warunki. */
    static void multipleBounds() {
        section("5. Kilka ograniczeń: <T extends Number & Comparable<T>>");

        show("atMost(120, 100)", atMost(120, 100));
        show("atMost(2.5, 10.0)", atMost(2.5, 10.0));
        // WYNIK: atMost(120, 100) → 100
        // WYNIK: atMost(2.5, 10.0) → 2.5

        // PUŁAPKA: kolejność ma znaczenie — klasa (Number) PIERWSZA, potem interfejsy. <T extends Comparable<T> & Number>
        //   to błąd kompilacji (interface expected here).
    }

    // =================================================================================================
    // 6. OGRANICZENIE WŁASNYM INTERFEJSEM
    // =================================================================================================

    /** 6. {@code <T extends Priced>} — metoda działa dla książek i kaw, a zwraca DOKŁADNIE ten typ, który dostała. */
    static void ownInterfaceBound() {
        section("6. <T extends Priced>: wynik zachowuje konkretny typ");

        List<Book> books = List.of(new Book("Wiedźmin", 4999), new Book("Lalka", 2599));
        List<Coffee> coffees = List.of(new Coffee("latte", 1400), new Coffee("espresso", 900));

        Book cheapBook = cheapest(books);                 // wynik to Book — bez rzutowania
        Coffee cheapCoffee = cheapest(coffees);
        show("najtańsza książka", cheapBook.title());
        show("najtańsza kawa", cheapCoffee.name());
        // WYNIK: najtańsza książka → Lalka
        // WYNIK: najtańsza kawa → espresso

        // PORÓWNANIE: metoda Priced cheapestPriced(List<? extends Priced> items) też by działała, ale zwróciłaby Priced —
        //   żeby odczytać title(), trzeba by rzutować na Book. Generyczne T „przenosi” konkretny typ z wejścia na wyjście.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • <T> bez ograniczenia = Object: tylko equals/hashCode/toString.
     *   • <T extends Comparable<T>> → compareTo; <N extends Number> → doubleValue(), intValue()...
     *   • extends w ograniczeniu oznacza „jest podtypem” — także dla interfejsów (nie ma „implements” w ograniczeniach).
     *   • Kilka ograniczeń: <T extends Klasa & Interfejs1 & Interfejs2> — klasa pierwsza, co najwyżej jedna.
     *   • Klasa: class Stats<N extends Number> — nie da się utworzyć Stats<String>.
     *   • Zwracanie T (a nie typu bazowego) zachowuje konkretny typ dla wywołującego.
     *   • W JDK spotkasz <T extends Comparable<? super T>> — bardziej elastyczne (Generics05Wildcards).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego w <T> T max(List<T>) nie da się wywołać compareTo, a w <T extends Comparable<T>> — tak?
     *   2. Co wypisze:  System.out.println(max(List.of("Zenek", "adam", "Bartek")));  ?
     *   3. ZNAJDŹ BŁĄD:  static <T extends Comparable<T> & Number> T f(T x) { return x; }
     *   4. Co wypisze:  System.out.println(sum(List.of(1, 2)) + sum(List.of(0.5)));  ?
     *   5. Czym różni się <T extends Priced> T cheapest(List<T>) od Priced cheapest(List<? extends Priced>)?
     *   6. Czy można utworzyć new Stats<>(List.of("a", "b"))? Dlaczego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: min liczb", 2, () -> exercise1(List.of(7, 2, 9)));
        Check.equal("ćw. 1b: min napisów", "ala", () -> exercise1(List.of("ola", "ala", "ula")));
        Check.equal("ćw. 2a: posortowana", true, () -> exercise2(List.of(1, 2, 2, 5)));
        Check.equal("ćw. 2b: nieposortowana", false, () -> exercise2(List.of("b", "a")));
        Check.equal("ćw. 3: średnia", 5.0, () -> exercise3(List.of(2, 4, 9)));
        Check.equal("ćw. 4: clamp", "10,3,7,m", () -> exercise4(15, 3, 10) + "," + exercise4(1, 3, 10) + ","
                + exercise4(7, 3, 10) + "," + exercise4("z", "a", "m"));
        Check.equal("ćw. 5: tańsze niż 30 zł", List.of(new Book("Lalka", 2599)),
                () -> exercise5(List.of(new Book("Wiedźmin", 4999), new Book("Lalka", 2599)), 3000));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", 2, () -> solution1(List.of(7, 2, 9)));
        Check.equal("ćw. 1b (wzorzec)", "ala", () -> solution1(List.of("ola", "ala", "ula")));
        Check.equal("ćw. 2a (wzorzec)", true, () -> solution2(List.of(1, 2, 2, 5)));
        Check.equal("ćw. 2b (wzorzec)", false, () -> solution2(List.of("b", "a")));
        Check.equal("ćw. 3 (wzorzec)", 5.0, () -> solution3(List.of(2, 4, 9)));
        Check.equal("ćw. 4 (wzorzec)", "10,3,7,m", () -> solution4(15, 3, 10) + "," + solution4(1, 3, 10) + ","
                + solution4(7, 3, 10) + "," + solution4("z", "a", "m"));
        Check.equal("ćw. 5 (wzorzec)", List.of(new Book("Lalka", 2599)),
                () -> solution5(List.of(new Book("Wiedźmin", 4999), new Book("Lalka", 2599)), 3000));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 7 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): najmniejszy element listy. Wzoruj się na max. */
    static <T extends Comparable<T>> T exercise1(List<T> list) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /** ĆWICZENIE 2 (średnie): czy lista jest posortowana rosnąco (równe sąsiednie elementy są OK)? */
    static <T extends Comparable<T>> boolean exercise2(List<T> list) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** ĆWICZENIE 3 (łatwe): średnia liczb (lista niepusta). Podpowiedź: doubleValue(). */
    static <N extends Number> double exercise3(List<N> numbers) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (średnie): przytnij wartość do zakresu [min, max]: poniżej min → min, powyżej max → max,
     * w środku → bez zmian. Ma działać dla liczb i napisów. Podpowiedź: dwa porównania compareTo.
     */
    static <T extends Comparable<T>> T exercise4(T value, T min, T max) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): zwróć listę elementów tańszych niż limit (priceGrosze() {@code <} limit). Wynik ma być
     * {@code List<T>} — dla listy książek lista książek.
     */
    static <T extends Priced> List<T> exercise5(List<T> items, int limitGrosze) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static <T extends Comparable<T>> T solution1(List<T> list) {
        T best = list.get(0);
        for (T x : list) {
            if (x.compareTo(best) < 0) {
                best = x;
            }
        }
        return best;
    }

    static <T extends Comparable<T>> boolean solution2(List<T> list) {
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i - 1).compareTo(list.get(i)) > 0) {
                return false;
            }
        }
        return true;
    }

    static <N extends Number> double solution3(List<N> numbers) {
        return sum(numbers) / numbers.size();
    }

    static <T extends Comparable<T>> T solution4(T value, T min, T max) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }

    static <T extends Priced> List<T> solution5(List<T> items, int limitGrosze) {
        List<T> result = new ArrayList<>();
        for (T item : items) {
            if (item.priceGrosze() < limitGrosze) {
                result.add(item);
            }
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bez ograniczenia kompilator zna tylko metody Object. Ograniczenie gwarantuje, że każde T implementuje
     *      Comparable<T>, więc compareTo na pewno istnieje.
     *   2. „adam” — małe litery mają większe kody Unicode niż wielkie ('a' = 97 > 'Z' = 90 > 'B' = 66).
     *   3. Klasa musi być pierwsza: <T extends Number & Comparable<T>>.
     *   4. „3.5” — 3.0 + 0.5.
     *   5. Obie przyjmą listę książek, ale pierwsza zwróci Book (typ z wejścia), a druga tylko Priced — do title()
     *      trzeba by rzutować.
     *   6. Nie — String nie jest podtypem Number, a Stats<N extends Number> przyjmuje tylko liczby (błąd kompilacji).
     */
    // </editor-fold>
}
