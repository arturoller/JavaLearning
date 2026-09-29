package t11_generics;

import helpers.Check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wymazywanie typów (type erasure) i ograniczenia generyków; Class{@code <T>} i Supplier{@code <T>} jako obejście
 *        (erasure = wymazywanie; heap pollution = zanieczyszczenie sterty; type token = „żeton typu” (obiekt Class))
 *
 * W SKRÓCIE:
 *   Generyki istnieją tylko podczas KOMPILACJI. Kompilator sprawdza typy, wstawia potrzebne rzutowania, a potem
 *   WYMAZUJE argumenty typu: w działającym programie {@code List<String>} i {@code List<Integer>} to ta sama klasa
 *   ArrayList. Dlatego nie da się: sprawdzić instanceof {@code List<String>}, napisać new T(), new T[n], ani przeciążyć
 *   metody różniącej się tylko argumentem typu. Obejścia: przekazać Supplier{@code <T>} (jak utworzyć T), IntFunction
 *   (jak utworzyć tablicę T[]) albo Class{@code <T>} (typ dostępny w działaniu).
 *
 * ANALOGIA: etykiety na pudłach przy przeprowadzce.
 *   Pakując (kompilacja), pilnujesz etykiet: „książki”, „talerze”. Ciężarówka (JVM) wiezie już same pudła — etykiety
 *   zostały zdjęte. Kierowca nie sprawdzi, czy w pudle są talerze; musisz mu dać osobną kartkę (Class{@code <T>}).
 *
 * JAK TO DZIAŁA:
 *   List{@code <String>} a = new ArrayList{@code <>}();  List{@code <Integer>} b = new ArrayList{@code <>}();
 *   a.getClass() == b.getClass()     → true (obie to java.util.ArrayList)
 *   Po kompilacji: {@code List<String>} → List, T → Object (albo górne ograniczenie, np. Comparable), a przy odczycie
 *   kompilator dopisuje rzutowanie: String s = (String) a.get(0).
 *
 * SŁÓWKA:
 *   erasure = wymazywanie; runtime = czas działania; compile time = czas kompilacji; reifiable = „urzeczowiony”
 *   (typ w pełni znany w działaniu); heap pollution = zanieczyszczenie sterty; type token = żeton typu (Class{@code <T>});
 *   supplier = dostawca; factory = fabryka; name clash = konflikt nazw; is instance = czy jest instancją; cast = rzutuj.
 *
 * ZOBACZ TEŻ: t11_generics/Generics05Wildcards, t11_generics/Generics07Repository (Class{@code <T>} w praktyce),
 *             t19_annotations_reflection/Annotations03ReflectionBasics (klasa Class), t13_lambdas/Lambda03JavaUtilFunction (Supplier).
 * </pre>
 */
public class Generics06ErasureLimits {

    public static void main(String[] args) {
        title("Generics06 — wymazywanie typów i ograniczenia");

        sameClassAtRuntime();       // same class at runtime = ta sama klasa w działaniu
        noInstanceofGeneric();      // no instanceof generic = brak instanceof z argumentem typu
        noNewT();                   // no new T = nie da się new T()
        noGenericArray();           // no generic array = nie da się new T[n]
        noOverloadByTypeArg();      // no overload by type argument = brak przeciążania po argumencie typu
        heapPollution();            // heap pollution = zanieczyszczenie sterty
        classToken();               // class token = Class<T> jako żeton typu
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. W DZIAŁANIU TO TA SAMA KLASA
    // =================================================================================================

    /** 1. Argumenty typu znikają po kompilacji — obiekt nie „pamięta”, że był {@code ArrayList<String>}. */
    static void sameClassAtRuntime() {
        section("1. List<String> i List<Integer> to w działaniu ta sama klasa");

        List<String> strings = new ArrayList<>();
        List<Integer> integers = new ArrayList<>();
        show("ta sama klasa?", strings.getClass() == integers.getClass());
        show("nazwa klasy", strings.getClass().getName());
        // WYNIK: ta sama klasa? → true
        // WYNIK: nazwa klasy → java.util.ArrayList    ← bez <String>

        // Dlaczego tak? Zgodność wstecz: kod sprzed Javy 5 (bez generyków) musiał dalej działać z nowymi bibliotekami.
    }

    // =================================================================================================
    // 2. BRAK instanceof List<String>
    // =================================================================================================

    /** 2. Skoro w działaniu nie ma {@code <String>}, nie da się tego sprawdzić. Można sprawdzić tylko „czy to lista”. */
    static void noInstanceofGeneric() {
        section("2. instanceof List<String> — niemożliwe");

        Object o = List.of("a", "b");
        // if (o instanceof List<String>) { }   ← BŁĄD KOMPILACJI: Object cannot be safely cast to List<String>
        show("o instanceof List<?>", o instanceof List<?>);
        // WYNIK: o instanceof List<?> → true

        if (o instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof String first) {
            show("pierwszy element to String", first);   // sprawdzamy ELEMENT — to jest możliwe
        }
        // WYNIK: pierwszy element to String → a
    }

    // =================================================================================================
    // 3. BRAK new T() — przekaż Supplier<T>
    // =================================================================================================

    /**
     * createAll = utwórz n obiektów. Zamiast new T() (niemożliwe — nie wiadomo, jaki to typ i czy ma konstruktor)
     * dostajemy Supplier{@code <T>}: „przepis, jak zrobić T”. Najczęściej referencję do konstruktora: StringBuilder::new.
     */
    static <T> List<T> createAll(Supplier<T> factory, int n) {
        // T item = new T();                     ← BŁĄD KOMPILACJI: unexpected type (T to parametr, nie klasa)
        List<T> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add(factory.get());
        }
        return result;
    }

    /** 3. Supplier{@code <T>} to najprostsze obejście braku new T(). */
    static void noNewT() {
        section("3. new T() — niemożliwe; obejście: Supplier<T>");

        List<StringBuilder> builders = createAll(StringBuilder::new, 3);
        builders.get(0).append("pierwszy");
        show("liczba obiektów", builders.size());
        show("różne obiekty?", builders.get(0) != builders.get(1));
        show("zawartości", builders);
        // WYNIK: liczba obiektów → 3
        // WYNIK: różne obiekty? → true
        // WYNIK: zawartości → [pierwszy, , ]
    }

    // =================================================================================================
    // 4. BRAK new T[n] — przekaż IntFunction<T[]>
    // =================================================================================================

    /**
     * toArray = zamień listę na tablicę T[]. new T[n] jest niemożliwe (tablica musi znać swój typ elementów w działaniu).
     * Obejście z JDK: IntFunction{@code <T[]>} — „jak zrobić tablicę o rozmiarze n”: String[]::new.
     */
    static <T> T[] toArray(List<T> list, IntFunction<T[]> arrayFactory) {
        // T[] array = new T[list.size()];       ← BŁĄD KOMPILACJI: generic array creation
        T[] array = arrayFactory.apply(list.size());
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    /** 4. Tak robi JDK: list.toArray(String[]::new) (Java 11+), stream.toArray(String[]::new). */
    static void noGenericArray() {
        section("4. new T[n] — niemożliwe; obejście: IntFunction<T[]>");

        String[] array = toArray(List.of("x", "y"), String[]::new);
        show("toArray", array);
        show("typ tablicy", array.getClass().getSimpleName());
        // WYNIK: toArray → [x, y]
        // WYNIK: typ tablicy → String[]

        String[] fromJdk = List.of("a", "b", "c").toArray(String[]::new);
        show("List.toArray(String[]::new)", fromJdk);
        // WYNIK: List.toArray(String[]::new) → [a, b, c]

        // DOBRA PRAKTYKA: w kodzie generycznym wybieraj List<T> zamiast T[] — listy nie mają tego problemu.
    }

    // =================================================================================================
    // 5. BRAK PRZECIĄŻANIA PO ARGUMENCIE TYPU
    // =================================================================================================

    /** 5. Dwie metody różniące się tylko {@code <String>} / {@code <Integer>} mają po wymazaniu TĘ SAMĄ sygnaturę. */
    static void noOverloadByTypeArg() {
        section("5. Przeciążanie po argumencie typu — niemożliwe");

        // static void print(List<String> list) { }
        // static void print(List<Integer> list) { }   ← BŁĄD KOMPILACJI: name clash — both methods have same erasure
        note("po wymazaniu obie metody to print(List) — JVM nie odróżniłaby ich");
        // WYNIK:    ℹ po wymazaniu obie metody to print(List) — JVM nie odróżniłaby ich
        // Rozwiązanie: różne NAZWY metod (printNames, printNumbers) albo jedna metoda z List<?>.
        // Inne ograniczenia: klasa generyczna nie może dziedziczyć po Throwable (class MyEx<T> extends Exception — błąd),
        //   pola static nie używają T (Generics02Classes), T nie może być typem prostym (Generics01Why).
    }

    // =================================================================================================
    // 6. ZANIECZYSZCZENIE STERTY (heap pollution)
    // =================================================================================================

    /** sneakyAdd = „przemyca” Integer do listy napisów przez surowy typ. Dlatego ostrzeżenia unchecked są ważne! */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void sneakyAdd(List<String> strings) {
        List raw = strings;                      // surowy typ wyłącza kontrolę
        raw.add(42);                             // w działaniu nic nie protestuje — lista to zwykła ArrayList
    }

    /**
     * 6. Surowe typy i niesprawdzone rzutowania pozwalają włożyć do {@code List<String>} obcy element. Błąd wybucha
     * dopiero przy ODCZYCIE — tam, gdzie kompilator wstawił niewidoczne (String).
     */
    static void heapPollution() {
        section("6. Zanieczyszczenie sterty: obcy element w List<String>");

        List<String> names = new ArrayList<>(List.of("Ala"));
        sneakyAdd(names);
        show("rozmiar", names.size());
        show("println całej listy działa", names);
        // WYNIK: rozmiar → 2
        // WYNIK: println całej listy działa → [Ala, 42]

        expectThrows("String s = names.get(1)", () -> {
            String s = names.get(1);             // tu kompilator wstawił (String) — i tu wybucha
            System.out.println(s);
        });
        // WYNIK: ✔ String s = names.get(1) → rzucono ClassCastException: class java.lang.Integer cannot be cast to class java.lang.String (java.lang.Integer and java.lang.String are in module java.base of loader 'bootstrap')

        // DOBRA PRAKTYKA: nie ignoruj ostrzeżeń „unchecked”. @SuppressWarnings("unchecked") tylko na najmniejszym
        //   możliwym fragmencie i z komentarzem, DLACZEGO rzutowanie jest bezpieczne.
        // Uwaga: przy metodach z generycznymi varargs (T... items) kompilator ostrzega o możliwym heap pollution;
        //   bezpieczne metody oznacza się @SafeVarargs (jak List.of).
    }

    // =================================================================================================
    // 7. Class<T> JAKO ŻETON TYPU
    // =================================================================================================

    /**
     * onlyOfType = tylko elementy danego typu. Class{@code <T>} niesie typ DO DZIAŁANIA programu: type.isInstance(o)
     * sprawdza typ, type.cast(o) rzutuje bez ostrzeżenia unchecked.
     */
    static <T> List<T> onlyOfType(List<?> items, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Object o : items) {
            if (type.isInstance(o)) {
                result.add(type.cast(o));
            }
        }
        return result;
    }

    /** 7. Gdy kod generyczny MUSI znać typ w działaniu — przekaż Class{@code <T>} (String.class, Integer.class). */
    static void classToken() {
        section("7. Class<T> — typ dostępny w działaniu");

        List<Object> mixed = List.of(1, "dwa", 3.0, "cztery", 5);
        List<String> texts = onlyOfType(mixed, String.class);
        List<Integer> ints = onlyOfType(mixed, Integer.class);
        show("String.class", texts);
        show("Integer.class", ints);
        // WYNIK: String.class → [dwa, cztery]
        // WYNIK: Integer.class → [1, 5]

        // Tak działają m.in. EnumSet.allOf(Day.class), new EnumMap<>(Day.class) (t08_enums) i biblioteki JSON
        //   (mapper.readValue(json, Customer.class)).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Type erasure: argumenty typu istnieją tylko w kompilacji; w działaniu List<String> == List<Integer> (ArrayList).
     *   • Kompilator dopisuje rzutowania przy odczycie → ClassCastException „w dziwnym miejscu” przy heap pollution.
     *   • Nie da się: instanceof List<String> (tylko List<?>), new T(), new T[n], static T, przeciążyć po <...>,
     *     generycznej podklasy Throwable, typów prostych jako T.
     *   • Obejścia: Supplier<T> (StringBuilder::new), IntFunction<T[]> (String[]::new), Class<T> (isInstance, cast).
     *   • Ostrzeżenia unchecked traktuj poważnie; @SuppressWarnings tylko lokalnie i z uzasadnieniem.
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  System.out.println(new ArrayList<String>().getClass() == new ArrayList<Double>().getClass());  ?
     *   2. Dlaczego nie można napisać new T() w klasie generycznej? Jak to obejść?
     *   3. ZNAJDŹ BŁĄD:
     *          static void save(List<Customer> c) { }
     *          static void save(List<Order> o) { }
     *   4. Gdzie wybucha ClassCastException przy zanieczyszczeniu sterty — przy dodawaniu czy przy odczycie? Dlaczego?
     *   5. Co wypisze:  System.out.println(onlyOfType(List.of(1, 2.0, 3), Double.class));  ?
     *   6. Po co metodzie List.toArray argument String[]::new?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: n obiektów z dostawcy", List.of(5, 6, 7), () -> {
            Iterator<Integer> it = List.of(5, 6, 7, 8).iterator();
            return exercise1(it::next, 3);
        });
        Check.equal("ćw. 2: tylko napisy", List.of("a", "b"), () -> exercise2(List.of(1, "a", 2.5, "b"), String.class));
        Check.equal("ćw. 3: tablica", "[x, y, z]", () -> Arrays.toString(exercise3(List.of("x", "y", "z"), String[]::new)));
        Check.equal("ćw. 4: liczniki typów", "{Integer=2, String=1, Double=1}",
                () -> String.valueOf(exercise4(List.of(1, "a", 2, 3.0))));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of(5, 6, 7), () -> {
            Iterator<Integer> it = List.of(5, 6, 7, 8).iterator();
            return solution1(it::next, 3);
        });
        Check.equal("ćw. 2 (wzorzec)", List.of("a", "b"), () -> solution2(List.of(1, "a", 2.5, "b"), String.class));
        Check.equal("ćw. 3 (wzorzec)", "[x, y, z]", () -> Arrays.toString(solution3(List.of("x", "y", "z"), String[]::new)));
        Check.equal("ćw. 4 (wzorzec)", "{Integer=2, String=1, Double=1}",
                () -> String.valueOf(solution4(List.of(1, "a", 2, 3.0))));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): zwróć listę n obiektów pobranych z dostawcy (factory.get()). Wzoruj się na createAll. */
    static <T> List<T> exercise1(Supplier<T> factory, int n) {
        // TODO: twoje rozwiązanie
        return new ArrayList<>();
    }

    /** ĆWICZENIE 2 (średnie): zwróć elementy danego typu. Użyj type.isInstance i type.cast (bez @SuppressWarnings). */
    static <T> List<T> exercise2(List<?> items, Class<T> type) {
        // TODO: twoje rozwiązanie
        return new ArrayList<>();
    }

    /** ĆWICZENIE 3 (średnie): zamień listę na tablicę przy pomocy arrayFactory (np. String[]::new). */
    static <T> T[] exercise3(List<T> list, IntFunction<T[]> arrayFactory) {
        // TODO: twoje rozwiązanie
        return arrayFactory.apply(0);
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): policz elementy według ich KLASY w działaniu (getClass().getSimpleName()), w kolejności
     * pierwszego wystąpienia. [1, "a", 2, 3.0] → {Integer=2, String=1, Double=1}. Podpowiedź: LinkedHashMap i merge.
     */
    static Map<String, Integer> exercise4(List<?> items) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static <T> List<T> solution1(Supplier<T> factory, int n) {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add(factory.get());
        }
        return result;
    }

    static <T> List<T> solution2(List<?> items, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Object o : items) {
            if (type.isInstance(o)) {
                result.add(type.cast(o));
            }
        }
        return result;
    }

    static <T> T[] solution3(List<T> list, IntFunction<T[]> arrayFactory) {
        T[] array = arrayFactory.apply(list.size());
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    static Map<String, Integer> solution4(List<?> items) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Object o : items) {
            counts.merge(o.getClass().getSimpleName(), 1, Integer::sum);
        }
        return counts;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. „true” — po wymazaniu obie to java.util.ArrayList.
     *   2. W działaniu T nie jest znane (wymazane do Object/ograniczenia), a nawet gdyby było — nie wiadomo, czy ma
     *      konstruktor bezargumentowy. Obejście: przekazać Supplier<T> (np. Customer::new) albo Class<T>.
     *   3. Po wymazaniu obie metody mają sygnaturę save(List) — „name clash”, błąd kompilacji. Nadaj różne nazwy
     *      (saveCustomers, saveOrders).
     *   4. Przy ODCZYCIE — dodanie przez surowy typ nie jest sprawdzane (lista w działaniu przechowuje Object),
     *      a rzutowanie (String) kompilator wstawia dopiero tam, gdzie element jest odczytywany jako String.
     *   5. „[2.0]”.
     *   6. Metoda generyczna nie może sama utworzyć tablicy T[] (wymazywanie) — String[]::new mówi jej, jak utworzyć
     *      tablicę właściwego typu i rozmiaru.
     */
    // </editor-fold>
}
