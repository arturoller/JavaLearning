package t13_lambdas;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Employee;
import helpers.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Referencje do metod — {@code Klasa::metoda} zamiast lambdy, która tylko woła jedną metodę
 *        (method reference = referencja do metody, odwołanie do metody)
 *
 * W SKRÓCIE:
 *   Jeśli lambda nie robi nic poza wywołaniem JEDNEJ istniejącej metody, możesz zamiast niej wskazać tę metodę:
 *   {@code s -> s.toUpperCase()}  to to samo co  {@code String::toUpperCase}.
 *   Znak :: czytamy „metoda ... z ...”. Referencja to wciąż obiekt interfejsu funkcyjnego — tylko krócej zapisany.
 *   Są CZTERY rodzaje: statyczna, związana z obiektem (bound), niezwiązana (unbound) i do konstruktora.
 *
 * ANALOGIA: skrót na pulpicie.
 *   Lambda to karteczka z instrukcją „otwórz Kalkulator i wpisz liczbę”. Referencja do metody to skrót na pulpicie
 *   prowadzący prosto do Kalkulatora — nic nie dopisujesz, po prostu wskazujesz, CO ma zostać uruchomione.
 *   Jeśli trzeba coś dodać (inne parametry, dodatkowy krok) — skrót nie wystarczy, wracasz do karteczki (lambdy).
 *
 * JAK TO DZIAŁA: są cztery rodzaje referencji
 *   1. statyczna          {@code Integer::parseInt}       =  {@code s -> Integer.parseInt(s)}
 *   2. bound (związana)   {@code System.out::println}     =  {@code x -> System.out.println(x)}    ← obiekt ustalony z góry
 *   3. unbound            {@code String::toUpperCase}     =  {@code s -> s.toUpperCase()}           ← obiekt = PIERWSZY parametr
 *   4. konstruktor        {@code ArrayList::new}          =  {@code () -> new ArrayList<>()}
 *                         {@code String[]::new}           =  {@code n -> new String[n]}
 *   Kompilator na podstawie TYPU DOCELOWEGO (jaki interfejs funkcyjny jest oczekiwany) ustala, ile parametrów
 *   przekazać i gdzie: jako argumenty metody, a w wersji unbound — pierwszy parametr staje się obiektem, na którym
 *   metoda jest wołana.
 *
 * SŁÓWKA:
 *   method reference = referencja do metody; bound = związany (z konkretnym obiektem); unbound = niezwiązany;
 *   receiver = odbiorca (obiekt, na którym wołamy metodę); constructor = konstruktor; parse = przetwórz (napis → liczbę);
 *   concat = sklej; abs (absolute) = wartość bezwzględna; ambiguous = niejednoznaczny; palindrome = palindrom;
 *   prefix = przedrostek; tag = etykieta; capacity = pojemność; evaluate = obliczyć (wartość wyrażenia).
 *
 * ZOBACZ TEŻ: Lambda03JavaUtilFunction (interfejsy, do których pasują referencje), Lambda05Composition
 *             (Comparator.comparing(Product::price)), t16_streams/Streams03FilterMap (map(Product::name)).
 * </pre>
 */
public class Lambda04MethodReferences {

    public static void main(String[] args) {
        title("Lambda04 — referencje do metod");

        whatIsMethodReference();      // what is method reference = czym jest referencja do metody
        staticReference();            // static reference = referencja do metody statycznej
        boundReference();             // bound reference = referencja związana z obiektem
        unboundReference();           // unbound reference = referencja niezwiązana
        constructorReference();       // constructor reference = referencja do konstruktora
        equivalenceTable();           // equivalence table = tabela równoważności
        whenLambdaIsClearer();        // when lambda is clearer = kiedy lambda jest czytelniejsza
        receiverEvaluatedOnce();      // receiver evaluated once = obiekt ustalany raz, przy tworzeniu
        ambiguityAndOverloads();      // ambiguity and overloads = niejednoznaczność i przeciążenia
        exercises();                  // exercises = ćwiczenia
    }

    // =================================================================================================
    // Metody pomocnicze używane w całej lekcji (mini-wersje filter i map ze streamów)
    // =================================================================================================

    /** mapAll = przekształć wszystkie: nowa lista z wynikami funkcji. */
    static <T, R> List<R> mapAll(List<T> items, Function<? super T, ? extends R> mapper) {
        List<R> result = new ArrayList<>();
        for (T item : items) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    /** filterAll = przefiltruj wszystkie: nowa lista z elementami spełniającymi warunek. */
    static <T> List<T> filterAll(List<T> items, Predicate<? super T> condition) {
        List<T> result = new ArrayList<>();
        for (T item : items) {
            if (condition.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    // =================================================================================================
    // 1. CZYM JEST REFERENCJA DO METODY
    // =================================================================================================

    /**
     * 1. Lambda, która tylko przekazuje parametr do jednej metody, to „pośrednik bez pracy”. Referencja go usuwa.
     */
    static void whatIsMethodReference() {
        section("1. Czym jest referencja do metody");

        List<String> fruits = List.of("jabłko", "gruszka");              // fruits = owoce

        fruits.forEach(f -> System.out.println("   " + f));             // lambda — dopisuje wcięcie, więc jest potrzebna
        // WYNIK: jabłko
        // WYNIK: gruszka

        fruits.forEach(System.out::println);                             // referencja: „dla każdego — System.out.println”
        // WYNIK: jabłko
        // WYNIK: gruszka
        // Lambda  f -> System.out.println(f)  nic nie dodaje od siebie — bierze f i oddaje je println.
        // Dlatego można ją zastąpić wskazaniem metody: System.out::println.

        List<String> upper = mapAll(fruits, String::toUpperCase);        // = s -> s.toUpperCase()
        show("mapAll(fruits, String::toUpperCase)", upper);
        // WYNIK: mapAll(fruits, String::toUpperCase) → [JABŁKO, GRUSZKA]

        // Referencja to TEN SAM obiekt interfejsu funkcyjnego, co lambda. Nie wywołuje metody od razu —
        // tak jak lambda, „czeka”, aż ktoś zawoła apply/accept/test.
        // PUŁAPKA: po :: NIE ma nawiasów.  String::toUpperCase()  się nie skompiluje — wskazujesz metodę, a nie ją wołasz.
    }

    // =================================================================================================
    // 2. REFERENCJA DO METODY STATYCZNEJ
    // =================================================================================================

    /** isPalindrome = czy palindrom (czytany od tyłu jest taki sam). Nasza metoda statyczna. */
    static boolean isPalindrome(String word) {
        return new StringBuilder(word).reverse().toString().equals(word);
    }

    /**
     * 2. {@code Klasa::metodaStatyczna} — parametry lambdy stają się argumentami metody statycznej.
     * {@code Integer::parseInt}  =  {@code s -> Integer.parseInt(s)}.
     */
    static void staticReference() {
        section("2. Referencja do metody statycznej: Klasa::metodaStatyczna");

        Function<String, Integer> parse = Integer::parseInt;              // parseInt = zamień napis na int
        show("parse.apply(\"42\") + 1", parse.apply("42") + 1);
        // WYNIK: parse.apply("42") + 1 → 43

        show("mapAll(texts, Integer::parseInt)", mapAll(List.of("10", "-7", "3"), Integer::parseInt));
        // WYNIK: mapAll(texts, Integer::parseInt) → [10, -7, 3]

        IntUnaryOperator abs = Math::abs;                                 // abs = wartość bezwzględna
        show("abs.applyAsInt(-7)", abs.applyAsInt(-7));
        // WYNIK: abs.applyAsInt(-7) → 7

        // Własna metoda statyczna też się nadaje — tu jako Predicate<String>:
        List<String> words = List.of("kajak", "java", "oko", "lambda", "potop");
        show("palindromy", filterAll(words, Lambda04MethodReferences::isPalindrome));
        // WYNIK: palindromy → [kajak, oko, potop]

        // DOBRA PRAKTYKA: dłuższą logikę wyciągnij do metody z dobrą nazwą i podaj referencję.
        //   filterAll(words, Lambda04MethodReferences::isPalindrome) czyta się jak zdanie: „zostaw palindromy”.
    }

    // =================================================================================================
    // 3. REFERENCJA ZWIĄZANA Z OBIEKTEM (BOUND)
    // =================================================================================================

    /**
     * 3. {@code obiekt::metoda} — metoda będzie wołana na KONKRETNYM, z góry wskazanym obiekcie (receiver = odbiorca).
     * Parametry lambdy stają się argumentami tej metody.
     */
    static void boundReference() {
        section("3. Referencja związana z obiektem: obiekt::metoda (bound)");

        // System.out to obiekt (PrintStream). System.out::println = x -> System.out.println(x)
        Consumer<String> print = System.out::println;
        print.accept("   wypisane przez System.out::println");
        // WYNIK: wypisane przez System.out::println

        // "Pan "::concat — obiektem jest napis "Pan ", argument dokleja się na końcu (concat = sklej):
        Function<String, String> mister = "Pan "::concat;                // = s -> "Pan ".concat(s)
        show("mapAll(nazwiska, \"Pan \"::concat)", mapAll(List.of("Kowalski", "Nowak"), mister));
        // WYNIK: mapAll(nazwiska, "Pan "::concat) → [Pan Kowalski, Pan Nowak]

        // Zbiór jako warunek: promo::contains = sku -> promo.contains(sku)
        Set<String> promo = Set.of("ELE-003", "KSI-001", "SPO-002");      // promo = promocja; kody SKU w promocji
        List<String> allSkus = mapAll(SampleData.products(), Product::sku);   // Product::sku — rodzaj 3 (sekcja 4)
        show("kody w promocji", filterAll(allSkus, promo::contains));
        // WYNIK: kody w promocji → [ELE-003, SPO-002, KSI-001]    ← kolejność z listy produktów, nie ze zbioru

        // Dowolny obiekt z metodą: vat::multiply = net -> vat.multiply(net)  (multiply = pomnóż)
        BigDecimal vat = new BigDecimal("1.23");
        Function<BigDecimal, BigDecimal> gross = vat::multiply;           // gross = brutto
        show("brutto od 100.00", gross.apply(new BigDecimal("100.00")));
        // WYNIK: brutto od 100.00 → 123.0000    ← skala = 2 + 2 miejsca; zaokrąglanie: t15_numbers
    }

    // =================================================================================================
    // 4. REFERENCJA NIEZWIĄZANA (UNBOUND)
    // =================================================================================================

    /**
     * 4. {@code Klasa::metodaInstancji} — obiekt NIE jest ustalony z góry. Zostaje nim PIERWSZY parametr lambdy,
     * a pozostałe parametry (jeśli są) idą jako argumenty metody.
     * <ul>
     *   <li>{@code String::length} jako {@code ToIntFunction<String>} = {@code s -> s.length()}</li>
     *   <li>{@code String::compareToIgnoreCase} jako {@code Comparator<String>} = {@code (a, b) -> a.compareToIgnoreCase(b)}</li>
     * </ul>
     */
    static void unboundReference() {
        section("4. Referencja niezwiązana: Klasa::metodaInstancji (unbound)");

        ToIntFunction<String> length = String::length;                    // s -> s.length()
        show("length.applyAsInt(\"stream\")", length.applyAsInt("stream"));
        // WYNIK: length.applyAsInt("stream") → 6

        // Akcesory rekordów to zwykłe metody — idealne do referencji:
        show("nazwy (Product::name)", mapAll(SampleData.products().subList(0, 3), Product::name));
        show("na stanie (Product::inStock)", filterAll(SampleData.products(), Product::inStock).size());
        // WYNIK: nazwy (Product::name) → [Laptop Pro 14, Smartfon X, Słuchawki BT]
        // WYNIK: na stanie (Product::inStock) → 12

        // Dwa parametry: PIERWSZY to obiekt, DRUGI to argument metody.
        List<String> names = new ArrayList<>(List.of("ewa", "Anna", "bartek", "Celina"));
        names.sort(String::compareToIgnoreCase);                         // (a, b) -> a.compareToIgnoreCase(b)
        show("sort(String::compareToIgnoreCase)", names);
        // WYNIK: sort(String::compareToIgnoreCase) → [Anna, bartek, Celina, ewa]
        // (compareToIgnoreCase = porównaj, ignorując wielkość liter. Zwykłe compareTo dałoby [Anna, Celina, bartek, ewa] —
        //  wielkie litery przed małymi. Polskich liter i tak nie posortuje po polsku — do tego Collator.)

        BinaryOperator<String> glue = String::concat;                     // (a, b) -> a.concat(b); glue = klej
        show("glue.apply(\"Java\", \"17\")", glue.apply("Java", "17"));
        // WYNIK: glue.apply("Java", "17") → Java17

        // Jak odróżnić bound od unbound? Spójrz na lewą stronę ::
        //   System.out::println  — po lewej OBIEKT (zmienna, wyrażenie)  → bound, obiekt ustalony z góry
        //   String::length       — po lewej KLASA, metoda nie jest static → unbound, obiekt = pierwszy parametr
    }

    // =================================================================================================
    // 5. REFERENCJA DO KONSTRUKTORA
    // =================================================================================================

    /** Tag = etykieta. Mały rekord do pokazania referencji do konstruktora: Tag::new. */
    record Tag(String label) {
    }

    /**
     * 5. {@code Klasa::new} — referencja do konstruktora; parametry lambdy stają się argumentami konstruktora.
     * {@code Typ[]::new} — referencja do „konstruktora” tablicy: rozmiar → nowa tablica.
     */
    static void constructorReference() {
        section("5. Referencja do konstruktora: Klasa::new i Typ[]::new");

        Supplier<List<String>> newList = ArrayList::new;                  // () -> new ArrayList<>()
        List<String> list = newList.get();
        list.add("pierwszy");
        show("ArrayList::new jako Supplier", list);
        // WYNIK: ArrayList::new jako Supplier → [pierwszy]

        Function<String, StringBuilder> builder = StringBuilder::new;     // s -> new StringBuilder(s)
        show("StringBuilder::new + reverse", builder.apply("lambda").reverse());
        // WYNIK: StringBuilder::new + reverse → adbmal

        show("mapAll(napisy, Tag::new)", mapAll(List.of("java", "lambda"), Tag::new));   // s -> new Tag(s)
        // WYNIK: mapAll(napisy, Tag::new) → [Tag[label=java], Tag[label=lambda]]

        // Tablice: toArray(IntFunction) — Java 11+. String[]::new = n -> new String[n]
        String[] array = List.of("a", "b", "c").toArray(String[]::new);
        show("toArray(String[]::new)", array);
        IntFunction<int[]> newIntArray = int[]::new;                      // n -> new int[n]
        show("int[]::new dla 4", newIntArray.apply(4));
        // WYNIK: toArray(String[]::new) → [a, b, c]
        // WYNIK: int[]::new dla 4 → [0, 0, 0, 0]

        // PUŁAPKA: ArrayList::new w computeIfAbsent. Referencja dopasowuje się do KSZTAŁTU Function<K, V>,
        //   więc woła konstruktor Z KLUCZEM jako argumentem: new ArrayList(klucz)!
        //   • klucz String → nie ma konstruktora ArrayList(String) → błąd kompilacji „invalid constructor reference”,
        //   • klucz Integer → PASUJE konstruktor ArrayList(int initialCapacity) — kompiluje się i „działa” przypadkiem,
        //     używając klucza jako pojemności. Dla klucza ujemnego wybucha:
        Map<Integer, List<String>> byLength = new TreeMap<>();
        expectThrows("computeIfAbsent(-1, ArrayList::new)", () -> byLength.computeIfAbsent(-1, ArrayList::new));
        // WYNIK: ✔ computeIfAbsent(-1, ArrayList::new) → rzucono IllegalArgumentException: Illegal Capacity: -1
        // (Illegal Capacity = niedozwolona pojemność.)
        // DOBRA PRAKTYKA: w computeIfAbsent pisz lambdę, która ignoruje klucz:  k -> new ArrayList<>()
    }

    // =================================================================================================
    // 6. TABELA RÓWNOWAŻNOŚCI
    // =================================================================================================

    /**
     * 6. Każdą referencję da się zamienić na lambdę (odwrotnie — nie zawsze). Sprawdźmy to dla czterech rodzajów:
     * wynik lambdy i referencji musi być identyczny.
     */
    static void equivalenceTable() {
        section("6. Tabela równoważności: referencja = lambda");

        //   rodzaj        referencja                lambda
        //   statyczna     Integer::parseInt         s -> Integer.parseInt(s)
        //   bound         "Pan "::concat            s -> "Pan ".concat(s)
        //   unbound       String::toUpperCase       s -> s.toUpperCase()
        //   unbound (2)   String::compareTo         (a, b) -> a.compareTo(b)
        //   konstruktor   Tag::new                  s -> new Tag(s)
        //   tablica       String[]::new             n -> new String[n]

        Function<String, Integer> staticRef = Integer::parseInt;
        Function<String, Integer> staticLambda = s -> Integer.parseInt(s);
        Function<String, String> boundRef = "Pan "::concat;
        Function<String, String> boundLambda = s -> "Pan ".concat(s);
        Function<String, String> unboundRef = String::toUpperCase;
        Function<String, String> unboundLambda = s -> s.toUpperCase();
        Function<String, Tag> constructorRef = Tag::new;
        Function<String, Tag> constructorLambda = s -> new Tag(s);

        show("statyczna   równa lambdzie?", staticRef.apply("12").equals(staticLambda.apply("12")));
        show("bound       równa lambdzie?", boundRef.apply("Nowak").equals(boundLambda.apply("Nowak")));
        show("unbound     równa lambdzie?", unboundRef.apply("abc").equals(unboundLambda.apply("abc")));
        show("konstruktor równa lambdzie?", constructorRef.apply("x").equals(constructorLambda.apply("x")));
        // WYNIK: statyczna   równa lambdzie? → true
        // WYNIK: bound       równa lambdzie? → true
        // WYNIK: unbound     równa lambdzie? → true
        // WYNIK: konstruktor równa lambdzie? → true
        // (equals porównuje WYNIKI. Samych obiektów lambd i referencji przez equals nie porównujemy — Lambda08Pitfalls.)
    }

    // =================================================================================================
    // 7. KIEDY LAMBDA JEST CZYTELNIEJSZA
    // =================================================================================================

    /**
     * 7. Referencja jest krótsza, ale nie zawsze lepsza. Lambda wygrywa, gdy trzeba COKOLWIEK dodać:
     * inny argument, zaprzeczenie, kilka wywołań po kolei, stałą.
     */
    static void whenLambdaIsClearer() {
        section("7. Kiedy lambda jest czytelniejsza od referencji");

        List<Employee> employees = SampleData.employees().subList(0, 3);

        // a) Dodatkowy argument — referencja nie ma gdzie go wpisać:
        show("pierwsze 3 litery", mapAll(List.of("kolekcja", "lambda"), s -> s.substring(0, 3)));
        // WYNIK: pierwsze 3 litery → [kol, lam]

        // b) Łańcuch wywołań — to dwie metody, a referencja wskazuje jedną:
        show("nazwiska wielkimi literami", mapAll(employees, e -> e.name().toUpperCase()));
        // WYNIK: nazwiska wielkimi literami → [ANNA NOWAK, PIOTR KOWALSKI, KATARZYNA WIŚNIEWSKA]
        //   Da się referencjami, ale w dwóch krokach: mapAll(mapAll(employees, Employee::name), String::toUpperCase).

        // c) Zaprzeczenie:  !Product::inStock  → błąd „method reference not expected here”
        //    (= „referencja do metody nie jest tu oczekiwana”). Lambda albo Predicate.not(Product::inStock) (Java 11+):
        show("niedostępne (lambda z !)", mapAll(filterAll(SampleData.products(), p -> !p.inStock()), Product::name));
        // WYNIK: niedostępne (lambda z !) → [Smartfon X, Oliwa z oliwek]

        // DOBRA PRAKTYKA:
        //   • referencja, gdy lambda tylko przekazuje parametry dalej:  Product::name,  System.out::println,
        //   • lambda, gdy coś dodajesz albo gdy nazwa metody nic nie mówi (np. Helper::process — „co robi process?”),
        //   • nie „przepychaj” wszystkiego na referencje za wszelką cenę — czytelność > zwięzłość.
    }

    // =================================================================================================
    // 8. PUŁAPKA: OBIEKT W REFERENCJI BOUND JEST USTALANY RAZ — PRZY TWORZENIU
    // =================================================================================================

    /**
     * 8. W referencji {@code wyrażenie::metoda} wyrażenie po lewej jest OBLICZANE JEDEN RAZ — w chwili tworzenia
     * referencji. W lambdzie {@code x -> wyrażenie.metoda(x)} wyrażenie jest obliczane PRZY KAŻDYM wywołaniu.
     * Dwie konsekwencje: późniejsza zmiana zmiennej nie wpływa na referencję, a null wybucha od razu.
     */
    static void receiverEvaluatedOnce() {
        section("8. Pułapka: obiekt w referencji bound ustalany raz, przy tworzeniu");

        String greeting = "Dzień dobry";                                   // greeting = powitanie
        Function<String, String> greet = greeting::concat;                 // obiekt "Dzień dobry" zapamiętany TERAZ
        greeting = "Dobry wieczór";                                        // zmieniamy zmienną...
        show("greet.apply(\", Anno\")", greet.apply(", Anno"));
        show("zmienna greeting teraz", greeting);
        // WYNIK: greet.apply(", Anno") → Dzień dobry, Anno    ← referencja „nie widzi” zmiany zmiennej
        // WYNIK: zmienna greeting teraz → Dobry wieczór
        // Lambda  s -> greeting.concat(s)  w tym miejscu w ogóle by się NIE skompilowała: greeting jest zmieniana,
        // a lambda może używać tylko zmiennych „effectively final” (Lambda06ClosuresScope). Referencja może,
        // bo nie odwołuje się do ZMIENNEJ, tylko do OBIEKTU, który w niej był w chwili tworzenia.

        // Druga konsekwencja: null po lewej stronie :: wybucha już przy TWORZENIU referencji.
        String missing = null;                                             // missing = brakujący
        expectThrows("utworzenie referencji missing::length", () -> {
            Supplier<Integer> neverUsed = missing::length;                 // nawet nie wołamy get()!
        });
        // WYNIK: ✔ utworzenie referencji missing::length → rzucono NullPointerException: (brak komunikatu)

        Supplier<Integer> lazy = () -> missing.length();                   // lambda: tworzenie jest bezpieczne...
        note("Lambda z null w środku utworzona bez wyjątku.");
        // WYNIK: ℹ Lambda z null w środku utworzona bez wyjątku.
        expectThrows("wywołanie lambdy lazy.get()", lazy::get);            // ...wybucha dopiero przy wywołaniu
        // WYNIK: ✔ wywołanie lambdy lazy.get() → rzucono NullPointerException: Cannot invoke "String.length()" because "missing" is null
        // PUŁAPKA: przy referencji NPE nie ma „pomocnego komunikatu” (helpful NPE, Java 14+), który mówi, CO było null.
        //   Przy debugowaniu NPE warto tymczasowo zamienić referencję na lambdę.
        // (Zauważ: expectThrows(..., lazy::get) — sama referencja do metody get obiektu lazy też jest „bound”.)
    }

    // =================================================================================================
    // 9. NIEJEDNOZNACZNOŚĆ I PRZECIĄŻENIA
    // =================================================================================================

    /**
     * 9. Referencja wskazuje metodę tylko po NAZWIE. Gdy metod o tej nazwie jest kilka (przeciążenia),
     * kompilator wybiera tę, która pasuje do typu docelowego. Czasem pasują dwie — i to jest błąd.
     */
    static void ambiguityAndOverloads() {
        section("9. Niejednoznaczne referencje i metody przeciążone");

        // Integer ma DWIE metody toString, które pasują do Function<Integer, String>:
        //   • statyczną  Integer.toString(int i)         — jako referencja statyczna: i -> Integer.toString(i)
        //   • instancji  i.toString()                    — jako referencja unbound:   i -> i.toString()
        //   Function<Integer, String> f = Integer::toString;
        //   → błąd: „incompatible types: invalid method reference
        //            reference to toString is ambiguous
        //            both method toString(int) in Integer and method toString() in Integer match”
        //     (= „odwołanie do toString jest niejednoznaczne — pasują obie metody”)
        // Rozwiązania: lambda albo referencja, która nie ma dwóch znaczeń:
        Function<Integer, String> viaLambda = i -> i.toString();
        Function<Integer, String> viaValueOf = String::valueOf;            // valueOf = wartość jako napis
        Function<Integer, String> viaObject = Object::toString;            // Object ma tylko toString() bez parametrów
        show("lambda / String::valueOf / Object::toString",
                viaLambda.apply(7) + " / " + viaValueOf.apply(8) + " / " + viaObject.apply(9));
        // WYNIK: lambda / String::valueOf / Object::toString → 7 / 8 / 9

        // Gdy typ docelowy nie pozostawia wątpliwości, ta sama nazwa wybiera RÓŻNE przeciążenia:
        IntFunction<String> fromInt = Integer::toString;                   // int → String: tylko toString(int) pasuje
        BiFunction<Integer, Integer, String> inBase = Integer::toString;   // (liczba, podstawa) → toString(int, int)
        show("IntFunction: Integer::toString(255)", fromInt.apply(255));
        show("BiFunction: Integer::toString(255, 16)", inBase.apply(255, 16));
        // WYNIK: IntFunction: Integer::toString(255) → 255
        // WYNIK: BiFunction: Integer::toString(255, 16) → ff    ← 255 w systemie szesnastkowym

        IntUnaryOperator absInt = Math::abs;                               // wybiera abs(int)
        DoubleUnaryOperator absDouble = Math::abs;                         // wybiera abs(double)
        show("Math::abs dla int / double", absInt.applyAsInt(-5) + " / " + absDouble.applyAsDouble(-2.5));
        // WYNIK: Math::abs dla int / double → 5 / 2.5

        // DOBRA PRAKTYKA: gdy kompilator zgłasza „ambiguous”, nie walcz z nim — napisz zwykłą lambdę.
        // PUŁAPKA: System.out::println ma wiele przeciążeń (println(char[]), println(Object)...). Wypisując tablicę char[]
        //   przez Consumer<char[]> dostaniesz litery, a przez Consumer<Object> — coś w stylu „[C@1b6d3586”.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Referencja do metody = skrót lambdy, która TYLKO woła jedną metodę. Po :: bez nawiasów.
     *   • Cztery rodzaje:
     *       Klasa::metodaStatyczna      Integer::parseInt       s -> Integer.parseInt(s)
     *       obiekt::metoda (bound)      System.out::println     x -> System.out.println(x)
     *       Klasa::metoda (unbound)     String::toUpperCase     s -> s.toUpperCase()   (obiekt = 1. parametr)
     *                                   String::compareTo       (a, b) -> a.compareTo(b)
     *       Klasa::new                  ArrayList::new          () -> new ArrayList<>()
     *       Typ[]::new                  String[]::new           n -> new String[n]     (np. list.toArray(String[]::new), Java 11+)
     *   • To, ile parametrów i dokąd trafią, wynika z TYPU DOCELOWEGO (interfejsu funkcyjnego).
     *   • Bound: wyrażenie po lewej liczone RAZ, przy tworzeniu (późniejsza zmiana zmiennej nic nie zmienia;
     *     null → NullPointerException od razu, bez pomocnego komunikatu).
     *   • Niejednoznaczność (Integer::toString jako Function<Integer, String>) → użyj lambdy albo String::valueOf.
     *   • computeIfAbsent(k, ArrayList::new) — konstruktor dostaje KLUCZ (Integer = pojemność!) → pisz k -> new ArrayList<>().
     *   • Lambda lepsza, gdy dodajesz argument, zaprzeczenie (!), łańcuch wywołań albo gdy nazwa metody nic nie mówi.
     *
     * PYTANIA KONTROLNE:
     *   1. Podaj lambdę równoważną każdej referencji: Math::max (jako BinaryOperator<Integer>), "abc"::equals,
     *      String::isEmpty, HashMap::new.
     *   2. Czym różni się referencja bound od unbound? Jak je odróżnić po zapisie?
     *   3. Co wypisze ten kod?
     *          String prefix = "A";
     *          Function<String, String> f = prefix::concat;
     *          prefix = "B";
     *          System.out.println(f.apply("x"));
     *   4. ZNAJDŹ BŁĄD:
     *          Function<Integer, String> f = Integer::toString;
     *   5. ZNAJDŹ BŁĄD (kod się kompiluje, ale czasem wybucha, a czasem dziwnie działa):
     *          Map<Integer, List<String>> m = new HashMap<>();
     *          m.computeIfAbsent(len, ArrayList::new).add(word);
     *   6. Dlaczego  products.removeIf(!Product::inStock)  się nie kompiluje? Podaj dwie poprawne wersje.
     *   7. Kiedy lambda jest lepsza od referencji do metody?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        List<String> raw = List.of(" kot", "Pies ", "  ", "ala", "", "Zebra");
        List<Tag> expected3 = List.of(new Tag("JAVA"), new Tag("LAMBDA"), new Tag("STREAM"));
        List<String> candidates = List.of("pro", "gram", "program", "p", "prog", "owanie");

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: lambdy → referencje (porządki w liście)", List.of("ala", "kot", "Pies", "Zebra"), () -> exercise1(raw));
        Check.equal("ćw. 2: napisy → liczby bezwzględne", List.of(3, 7, 10, 0), () -> exercise2(List.of("-3", "7", "-10", "0")));
        Check.equal("ćw. 3: napisy → etykiety Tag (wielkie litery)", expected3, () -> exercise3(List.of("java", "lambda", "stream")));
        Check.equal("ćw. 4: prefiksy tekstu, od najdłuższego", List.of("program", "prog", "pro", "p"),
                () -> exercise4("programowanie", candidates));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("ala", "kot", "Pies", "Zebra"), () -> solution1(raw));
        Check.equal("ćw. 2 (wzorzec)", List.of(3, 7, 10, 0), () -> solution2(List.of("-3", "7", "-10", "0")));
        Check.equal("ćw. 3 (wzorzec)", expected3, () -> solution3(List.of("java", "lambda", "stream")));
        Check.equal("ćw. 4 (wzorzec)", List.of("program", "prog", "pro", "p"), () -> solution4("programowanie", candidates));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ LAMBDY NA REFERENCJE DO METOD. Poniższy kod porządkuje listę napisów:
     * usuwa puste (albo same spacje), obcina spacje z brzegów i sortuje bez względu na wielkość liter.
     * <pre>{@code
     *   List<String> copy = new ArrayList<>(raw);
     *   copy.removeIf(s -> s.isBlank());
     *   copy.replaceAll(s -> s.strip());
     *   copy.sort((a, b) -> a.compareToIgnoreCase(b));
     *   return copy;
     * }</pre>
     * Napisz to samo, zamieniając każdą lambdę na referencję do metody.
     * Podpowiedź: wszystkie trzy to referencje unbound do metod klasy String (isBlank i strip — Java 11+).
     */
    static List<String> exercise1(List<String> raw) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (łatwe): zamień napisy na liczby, a liczby na ich wartości bezwzględne: ["-3", "7"] → [3, 7].
     * Użyj dwa razy metody mapAll i dwóch referencji do metod STATYCZNYCH.
     * Podpowiedź: Integer::parseInt, potem Math::abs.
     */
    static List<Integer> exercise2(List<String> texts) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): zamień napisy na obiekty Tag z etykietą zapisaną WIELKIMI literami:
     * ["java"] → [Tag[label=JAVA]]. Użyj mapAll z referencją unbound, a potem mapAll z referencją do konstruktora.
     * Podpowiedź: String::toUpperCase, Tag::new.
     */
    static List<Tag> exercise3(List<String> labels) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć tych kandydatów, którzy są PREFIKSAMI (początkami) tekstu text,
     * posortowanych od NAJDŁUŻSZEGO. Dla "programowanie" i [pro, gram, program, p, prog, owanie]
     * → [program, prog, pro, p].
     * Podpowiedź: warunek „kandydat c jest początkiem tekstu” to {@code text.startsWith(c)} — obiektem jest TEKST,
     * a kandydat jest argumentem. To referencja BOUND: {@code text::startsWith} (a nie String::startsWith!).
     * Sortowanie: kopia listy i sort z lambdą {@code (a, b) -> Integer.compare(b.length(), a.length())}.
     */
    static List<String> exercise4(String text, List<String> candidates) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(List<String> raw) {
        List<String> copy = new ArrayList<>(raw);
        copy.removeIf(String::isBlank);
        copy.replaceAll(String::strip);
        copy.sort(String::compareToIgnoreCase);
        return copy;
    }

    static List<Integer> solution2(List<String> texts) {
        List<Integer> numbers = mapAll(texts, Integer::parseInt);
        return mapAll(numbers, Math::abs);                   // kompilator wybierze abs(int) (rozpakowanie Integer → int)
    }

    static List<Tag> solution3(List<String> labels) {
        return mapAll(mapAll(labels, String::toUpperCase), Tag::new);
    }

    static List<String> solution4(String text, List<String> candidates) {
        List<String> prefixes = filterAll(candidates, text::startsWith);   // c -> text.startsWith(c)
        prefixes.sort((a, b) -> Integer.compare(b.length(), a.length()));  // filterAll zwraca ArrayList — można sortować
        return prefixes;
        // Uwaga: String::startsWith pasowałoby do BiPredicate<String, String> ((a, b) -> a.startsWith(b)),
        // a nie do Predicate<String> — dlatego tu potrzebna jest wersja bound.
        // Krócej z Lambda05Composition:  prefixes.sort(Comparator.comparingInt(String::length).reversed());
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Math::max → (a, b) -> Math.max(a, b);  "abc"::equals → s -> "abc".equals(s);
     *      String::isEmpty → s -> s.isEmpty();  HashMap::new → () -> new HashMap<>().
     *   2. Bound: obiekt jest ustalony z góry (po lewej stoi obiekt/wyrażenie, np. System.out::println).
     *      Unbound: po lewej stoi KLASA, a metoda nie jest statyczna — obiektem staje się pierwszy parametr
     *      (String::length = s -> s.length()).
     *   3. Ax — wyrażenie prefix zostało obliczone przy tworzeniu referencji (obiekt "A"); późniejsza zmiana zmiennej
     *      nie ma wpływu. (Lambda s -> prefix.concat(s) by się tu nie skompilowała.)
     *   4. Niejednoznaczność: pasuje i statyczne Integer.toString(int), i instancyjne toString(). Poprawnie:
     *      i -> i.toString(),  String::valueOf  albo  Object::toString.
     *   5. ArrayList::new dostaje KLUCZ jako argument — dla klucza Integer wywoła new ArrayList(len), czyli ustawi
     *      pojemność; dla ujemnego klucza rzuci IllegalArgumentException. Poprawnie: k -> new ArrayList<>().
     *   6. Operatora ! nie da się zastosować do referencji („method reference not expected here”) — referencja
     *      nie jest wartością boolean. Poprawnie: removeIf(p -> !p.inStock())  albo  removeIf(Predicate.not(Product::inStock)).
     *   7. Gdy trzeba dodać argument, zaprzeczenie, kilka wywołań, stałą — albo gdy nazwa metody w referencji
     *      nic nie mówi i lambda lepiej pokazuje, co się dzieje.
     */
    // </editor-fold>
}
