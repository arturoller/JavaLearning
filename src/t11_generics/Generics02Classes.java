package t11_generics;

import helpers.Check;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Własne klasy i interfejsy generyczne — jeden i kilka parametrów typu, dziedziczenie
 *        (generic class = klasa generyczna; generic interface = interfejs generyczny; stack = stos)
 *
 * W SKRÓCIE:
 *   Klasę generyczną piszesz raz, z parametrem typu w nawiasach ostrych: class SimpleStack{@code <T>}. Wewnątrz T działa
 *   jak zwykły typ — w polach, parametrach i wynikach metod. Parametrów może być kilka (Pair{@code <K, V>}). Interfejs też
 *   może być generyczny (Converter{@code <F, T>}), a klasa, która go implementuje, może podać konkretny typ albo
 *   przekazać parametr dalej.
 *
 * ANALOGIA: foremka do ciastek.
 *   Klasa generyczna to jedna foremka (kształt, zachowanie), a argument typu to ciasto: ta sama foremka zrobi ciastko
 *   z ciasta czekoladowego (SimpleStack{@code <String>}) i owsianego (SimpleStack{@code <Integer>}).
 *
 * JAK TO DZIAŁA:
 *   class SimpleStack{@code <T>} {
 *       private final List{@code <T>} items = new ArrayList{@code <>}();
 *       void push(T item) { items.add(item); }
 *       T pop() { return items.remove(items.size() - 1); }
 *   }
 *   SimpleStack{@code <String>} s = new SimpleStack{@code <>}();  s.push("a");  String x = s.pop();
 *
 * SŁÓWKA:
 *   stack = stos; push = włóż (na wierzch); pop = zdejmij (z wierzchu); peek = podejrzyj (bez zdejmowania);
 *   pair = para; key = klucz; value = wartość; converter = konwerter (zamieniacz); result = wynik; ok = w porządku;
 *   error = błąd; is empty = czy pusty; size = rozmiar.
 *
 * ZOBACZ TEŻ: t11_generics/Generics01Why (po co generyki), t11_generics/Generics03Methods (metody generyczne),
 *             t09_records/Records03Advanced (rekord generyczny Pair), t12_collections/Collections06QueuesDeques (ArrayDeque jako stos).
 * </pre>
 */
public class Generics02Classes {

    // ---------------------------------------------------------------------------------------------
    // 1. Klasa z jednym parametrem typu: stos
    // ---------------------------------------------------------------------------------------------

    /**
     * SimpleStack = prosty stos („ostatni włożony — pierwszy wyjęty”). Nazwa inna niż java.util.Stack, żeby nie mylić
     * z klasą z JDK (w prawdziwym kodzie używa się ArrayDeque — t12_collections).
     */
    static final class SimpleStack<T> {
        private final List<T> items = new ArrayList<>();

        void push(T item) {
            items.add(item);
        }

        T pop() {
            if (items.isEmpty()) {
                throw new NoSuchElementException("Stos jest pusty");
            }
            return items.remove(items.size() - 1);
        }

        T peek() {
            if (items.isEmpty()) {
                throw new NoSuchElementException("Stos jest pusty");
            }
            return items.get(items.size() - 1);
        }

        boolean isEmpty() {
            return items.isEmpty();
        }

        int size() {
            return items.size();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 2. Klasa z dwoma parametrami typu: para
    // ---------------------------------------------------------------------------------------------

    /** Pair = para klucz–wartość dwóch dowolnych typów K i V. (Rekord zrobiłby to krócej — t09_records.) */
    static final class Pair<K, V> {
        private final K key;
        private final V value;

        Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        K getKey() {
            return key;
        }

        V getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Pair)) {
                return false;
            }
            Pair<?, ?> other = (Pair<?, ?>) o;     // ? = „dowolny typ” (Generics05Wildcards)
            return Objects.equals(key, other.key) && Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }

        @Override
        public String toString() {
            return "(" + key + ", " + value + ")";
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 3. Interfejs generyczny
    // ---------------------------------------------------------------------------------------------

    /** Converter = konwerter z typu F (from = z) na typ T (to = na). Interfejs funkcyjny — pasuje do lambdy. */
    interface Converter<F, T> {
        T convert(F from);
    }

    /** LengthConverter = implementacja z KONKRETNYMI typami: String → Integer. */
    static final class LengthConverter implements Converter<String, Integer> {
        @Override
        public Integer convert(String from) {
            return from.length();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 4. Dziedziczenie po klasie generycznej
    // ---------------------------------------------------------------------------------------------

    /** Holder = pojemnik na jedną wartość (klasa bazowa). */
    static class Holder<T> {
        protected T value;

        Holder(T value) {
            this.value = value;
        }

        T get() {
            return value;
        }
    }

    /** CounterHolder = podklasa USTALAJĄCA typ: to zawsze Holder{@code <Integer>}. Może dodać metody znające ten typ. */
    static final class CounterHolder extends Holder<Integer> {
        CounterHolder(int start) {
            super(start);
        }

        void increment() {
            value = value + 1;                    // value to Integer — wiadomo, że można dodawać
        }
    }

    /** LabeledHolder = podklasa PRZEKAZUJĄCA parametr dalej: nadal generyczna, dodaje etykietę. */
    static final class LabeledHolder<T> extends Holder<T> {
        private final String label;

        LabeledHolder(String label, T value) {
            super(value);
            this.label = label;
        }

        String describe() {
            return label + ": " + value;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 5. Praktyczna klasa generyczna: wynik operacji (wartość ALBO błąd)
    // ---------------------------------------------------------------------------------------------

    /**
     * Result = wynik operacji: albo wartość typu T, albo komunikat błędu. Popularny wzorzec (w innych językach
     * wbudowany): metoda nie rzuca wyjątku, tylko zwraca wynik, który trzeba sprawdzić.
     */
    static final class Result<T> {
        private final T value;
        private final String error;

        private Result(T value, String error) {
            this.value = value;
            this.error = error;
        }

        /** ok = udany wynik. {@code <T>} przed typem zwracanym = metoda generyczna (Generics03Methods). */
        static <T> Result<T> ok(T value) {
            return new Result<>(value, null);
        }

        /** error = nieudany wynik z komunikatem. */
        static <T> Result<T> error(String message) {
            return new Result<>(null, message);
        }

        boolean isOk() {
            return error == null;
        }

        T getValue() {
            if (!isOk()) {
                throw new IllegalStateException("Brak wartości — błąd: " + error);
            }
            return value;
        }

        String getError() {
            return error;
        }

        @Override
        public String toString() {
            return isOk() ? "Ok(" + value + ")" : "Error(" + error + ")";
        }
    }

    /** parseAge = odczytaj wiek. Zamiast wyjątku zwraca {@code Result<Integer>}. */
    static Result<Integer> parseAge(String text) {
        try {
            int age = Integer.parseInt(text.strip());
            return age < 0 ? Result.error("ujemny wiek: " + age) : Result.ok(age);
        } catch (NumberFormatException e) {
            return Result.error("to nie liczba: " + text);
        }
    }

    public static void main(String[] args) {
        title("Generics02 — własne klasy i interfejsy generyczne");

        oneTypeParameter();     // one type parameter = jeden parametr typu
        twoTypeParameters();    // two type parameters = dwa parametry typu
        genericInterface();     // generic interface = interfejs generyczny
        inheritance();          // inheritance = dziedziczenie
        resultType();           // result type = typ wyniku
        staticPitfall();        // static pitfall = pułapka static
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. JEDEN PARAMETR TYPU
    // =================================================================================================

    /** 1. SimpleStack{@code <T>} działa z każdym typem — kompilator pilnuje, co wkładasz i co wyjmujesz. */
    static void oneTypeParameter() {
        section("1. SimpleStack<T>");

        SimpleStack<String> pages = new SimpleStack<>();     // historia przeglądarki
        pages.push("start");
        pages.push("produkty");
        pages.push("koszyk");
        show("peek", pages.peek());
        show("pop (wstecz)", pages.pop());
        show("pop (wstecz)", pages.pop());
        show("zostało", pages.size());
        // WYNIK: peek → koszyk
        // WYNIK: pop (wstecz) → koszyk
        // WYNIK: pop (wstecz) → produkty
        // WYNIK: zostało → 1

        SimpleStack<Integer> numbers = new SimpleStack<>();
        numbers.push(1);
        numbers.push(2);
        show("suma dwóch zdjętych", numbers.pop() + numbers.pop());
        // WYNIK: suma dwóch zdjętych → 3

        expectThrows("pop z pustego", numbers::pop);
        // WYNIK: ✔ pop z pustego → rzucono NoSuchElementException: Stos jest pusty
    }

    // =================================================================================================
    // 2. DWA PARAMETRY TYPU
    // =================================================================================================

    /** 2. Pair{@code <K, V>} — parametry oddzielone przecinkiem; każdy może być innym typem. */
    static void twoTypeParameters() {
        section("2. Pair<K, V>");

        Pair<String, Integer> score = new Pair<>("Ala", 95);
        Pair<Integer, List<String>> group = new Pair<>(3, List.of("Ola", "Jan"));
        show("score", score);
        show("group", group);
        // WYNIK: score → (Ala, 95)
        // WYNIK: group → (3, [Ola, Jan])

        int bonus = score.getValue() + 5;          // getValue() zwraca Integer — bez rzutowania
        show("wynik z bonusem", bonus);
        // WYNIK: wynik z bonusem → 100

        show("equals", score.equals(new Pair<>("Ala", 95)));
        // WYNIK: equals → true
    }

    // =================================================================================================
    // 3. INTERFEJS GENERYCZNY
    // =================================================================================================

    /** 3. Converter{@code <F, T>} — implementacja klasą (LengthConverter) albo lambdą. */
    static void genericInterface() {
        section("3. Interfejs generyczny Converter<F, T>");

        Converter<String, Integer> byClass = new LengthConverter();
        Converter<String, String> byLambda = text -> text.strip().toUpperCase();
        Converter<Integer, String> stars = n -> "*".repeat(n);

        show("LengthConverter(\"generyki\")", byClass.convert("generyki"));
        show("lambda(\"  ala \")", byLambda.convert("  ala "));
        show("stars(5)", stars.convert(5));
        // WYNIK: LengthConverter("generyki") → 8
        // WYNIK: lambda("  ala ") → ALA
        // WYNIK: stars(5) → *****

        // Tak wyglądają interfejsy z java.util.function: Function<T, R>, Predicate<T>, Supplier<T> (t13_lambdas).
    }

    // =================================================================================================
    // 4. DZIEDZICZENIE PO KLASIE GENERYCZNEJ
    // =================================================================================================

    /** 4. Podklasa może USTALIĆ typ (extends Holder{@code <Integer>}) albo PRZEKAZAĆ parametr (extends Holder{@code <T>}). */
    static void inheritance() {
        section("4. Dziedziczenie: ustalony typ albo przekazany parametr");

        CounterHolder counter = new CounterHolder(10);
        counter.increment();
        counter.increment();
        show("CounterHolder", counter.get());
        // WYNIK: CounterHolder → 12

        LabeledHolder<Double> temp = new LabeledHolder<>("temperatura", 21.5);
        show("LabeledHolder", temp.describe());
        // WYNIK: LabeledHolder → temperatura: 21.5

        Holder<Integer> asBase = counter;          // CounterHolder JEST Holder<Integer>
        show("jako Holder<Integer>", asBase.get());
        // WYNIK: jako Holder<Integer> → 12
    }

    // =================================================================================================
    // 5. Result{@code <T>} — wartość albo błąd
    // =================================================================================================

    /** 5. Result{@code <T>} zmusza do sprawdzenia isOk() przed użyciem wartości — błąd nie „ucieka” niezauważony. */
    static void resultType() {
        section("5. Result<T>: wartość albo błąd");

        for (String text : List.of("30", "-4", "trzydzieści")) {
            Result<Integer> r = parseAge(text);
            show("parseAge(\"" + text + "\")", r);
        }
        // WYNIK: parseAge("30") → Ok(30)
        // WYNIK: parseAge("-4") → Error(ujemny wiek: -4)
        // WYNIK: parseAge("trzydzieści") → Error(to nie liczba: trzydzieści)

        expectThrows("getValue() na błędzie", () -> parseAge("x").getValue());
        // WYNIK: ✔ getValue() na błędzie → rzucono IllegalStateException: Brak wartości — błąd: to nie liczba: x

        // DOBRA PRAKTYKA: Result pasuje, gdy błąd to NORMALNY wynik (walidacja danych od użytkownika).
        //   Dla sytuacji wyjątkowych zostaw wyjątki (t10_exceptions); dla „brak wartości” — Optional (t14_optional).
    }

    // =================================================================================================
    // 6. PUŁAPKA: T nie istnieje w kontekście statycznym
    // =================================================================================================

    /**
     * 6. T należy do OBIEKTU (SimpleStack{@code <String>} i SimpleStack{@code <Integer>} to różne T), a pola i metody
     * statyczne są wspólne dla całej klasy — więc nie mogą używać T.
     */
    static void staticPitfall() {
        section("6. Pułapka: T w polach statycznych");

        // class SimpleStack<T> { static T lastPushed; }         ← BŁĄD KOMPILACJI:
        //   non-static type variable T cannot be referenced from a static context
        // Metoda statyczna może mieć WŁASNY parametr typu: static <T> Result<T> ok(T value) — jak w Result (Generics03).
        note("pole static jest jedno dla wszystkich SimpleStack<String>, SimpleStack<Integer>... — jakie miałoby T?");
        // WYNIK:    ℹ pole static jest jedno dla wszystkich SimpleStack<String>, SimpleStack<Integer>... — jakie miałoby T?
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • class Nazwa<T> { T pole; T metoda(); void metoda(T x); } — T to parametr typu.
     *   • Kilka parametrów: class Pair<K, V>; użycie: new Pair<>("a", 1).
     *   • interface Converter<F, T> { T convert(F from); } — implementacja klasą lub lambdą.
     *   • Dziedziczenie: extends Holder<Integer> (typ ustalony) albo LabeledHolder<T> extends Holder<T> (przekazany).
     *   • W equals klasy generycznej rzutuj na Pair<?, ?> (nie da się sprawdzić argumentów typu w działaniu).
     *   • static nie może używać T klasy; metoda statyczna może mieć własne <T>.
     *   • Nie nazywaj klas jak klasy JDK (Stack, List...).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się class CounterHolder extends Holder<Integer> od class LabeledHolder<T> extends Holder<T>?
     *   2. Co wypisze:
     *          SimpleStack<String> s = new SimpleStack<>(); s.push("a"); s.push("b"); s.push("c");
     *          s.pop(); System.out.println(s.pop() + s.peek());
     *   3. ZNAJDŹ BŁĄD:  class Cache<T> { private static T last; }
     *   4. Jak zaimplementować Converter<Integer, Boolean> sprawdzający parzystość lambdą?
     *   5. Co wypisze:  System.out.println(parseAge(" 7 "));  ?
     *   6. Dlaczego w equals klasy Pair rzutujemy na Pair<?, ?>, a nie na Pair<K, V>?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: odwróć kolejność słów", "kota ma ala", () -> exercise1("ala ma kota"));
        Check.equal("ćw. 2: klucz pary z największą wartością", "Ewa",
                () -> exercise2(List.of(new Pair<>("Ala", 70), new Pair<>("Ewa", 92), new Pair<>("Jan", 85))));
        Check.equal("ćw. 3: konwerter inicjałów", "JMR", () -> exercise3().convert("Jan Maria Rokita"));
        Check.equal("ćw. 4: raport z Result", "suma=45, błędy=2",
                () -> exercise4(List.of("20", "abc", "25", "-3")));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "kota ma ala", () -> solution1("ala ma kota"));
        Check.equal("ćw. 2 (wzorzec)", "Ewa",
                () -> solution2(List.of(new Pair<>("Ala", 70), new Pair<>("Ewa", 92), new Pair<>("Jan", 85))));
        Check.equal("ćw. 3 (wzorzec)", "JMR", () -> solution3().convert("Jan Maria Rokita"));
        Check.equal("ćw. 4 (wzorzec)", "suma=45, błędy=2", () -> solution4(List.of("20", "abc", "25", "-3")));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): odwróć kolejność słów zdania przy użyciu SimpleStack{@code <String>}: włóż wszystkie słowa
     * (split(" ")), potem zdejmuj i sklejaj ze spacją. "ala ma kota" → "kota ma ala".
     */
    static String exercise1(String sentence) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /** ĆWICZENIE 2 (średnie): zwróć KLUCZ pary z największą wartością. Podpowiedź: pętla, getValue() to Integer. */
    static String exercise2(List<Pair<String, Integer>> pairs) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć Converter{@code <String, String>} (lambdę) zamieniający imiona i nazwiska na inicjały:
     * "Jan Maria Rokita" → "JMR". Podpowiedź: split(" "), charAt(0), StringBuilder.
     */
    static Converter<String, String> exercise3() {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): dla każdego tekstu wywołaj parseAge; zsumuj poprawne wartości i policz błędy.
     * Zwróć "suma=S, błędy=B". Podpowiedź: if (r.isOk()) sum += r.getValue(); else errors++;
     */
    static String exercise4(List<String> texts) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String sentence) {
        SimpleStack<String> stack = new SimpleStack<>();
        for (String word : sentence.split(" ")) {
            stack.push(word);
        }
        StringBuilder sb = new StringBuilder();
        while (!stack.isEmpty()) {
            sb.append(stack.pop());
            if (!stack.isEmpty()) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    static String solution2(List<Pair<String, Integer>> pairs) {
        Pair<String, Integer> best = pairs.get(0);
        for (Pair<String, Integer> p : pairs) {
            if (p.getValue() > best.getValue()) {
                best = p;
            }
        }
        return best.getKey();
    }

    static Converter<String, String> solution3() {
        return fullName -> {
            StringBuilder initials = new StringBuilder();
            for (String part : fullName.split(" ")) {
                initials.append(part.charAt(0));
            }
            return initials.toString();
        };
    }

    static String solution4(List<String> texts) {
        int sum = 0;
        int errors = 0;
        for (String t : texts) {
            Result<Integer> r = parseAge(t);
            if (r.isOk()) {
                sum += r.getValue();
            } else {
                errors++;
            }
        }
        return "suma=" + sum + ", błędy=" + errors;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. CounterHolder ustala typ na Integer — sam nie jest generyczny i może korzystać z tego, że wartość to liczba.
     *      LabeledHolder<T> pozostaje generyczny: typ poda dopiero jego użytkownik (LabeledHolder<Double>).
     *   2. „ba” — pierwszy pop() zdejmuje c, drugi zwraca b, a peek() pokazuje to, co zostało na wierzchu: a.
     *   3. Pole statyczne nie może mieć typu T (T należy do obiektu, a static do całej klasy) — błąd kompilacji.
     *   4. Converter<Integer, Boolean> isEven = n -> n % 2 == 0;
     *   5. „Ok(7)” — strip() usuwa spacje przed parseInt.
     *   6. W działaniu programu argumenty typu nie istnieją (wymazywanie — Generics06ErasureLimits), więc nie da się
     *      sprawdzić, czy obiekt to Pair<K, V>; Pair<?, ?> mówi „para czegokolwiek” i nie wymaga niesprawdzonego rzutowania.
     */
    // </editor-fold>
}
