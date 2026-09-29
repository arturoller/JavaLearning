package t13_lambdas;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Employee;
import helpers.model.Product;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Pakiet java.util.function — gotowe interfejsy funkcyjne na każdą okazję
 *        (java.util.function = pakiet „funkcje” w bibliotece standardowej Javy)
 *
 * W SKRÓCIE:
 *   Nie musisz pisać własnego interfejsu dla każdej lambdy. Java ma gotowe „gniazdka” na typowe kształty:
 *   warunek (Predicate), przekształcenie (Function), akcja (Consumer), dostawca (Supplier), operatory
 *   (UnaryOperator, BinaryOperator), ich wersje dwuargumentowe (Bi...) i wersje dla liczb prostych (Int..., Long..., Double...).
 *   Wystarczy zapamiętać sześć podstawowych i WZÓR nazewnictwa — resztę rozszyfrujesz z nazwy.
 *
 * ANALOGIA: skrzynka z końcówkami do wkrętarki.
 *   Nie dorabiasz końcówki do każdej śrubki — bierzesz standardową: krzyżak, płaska, torx.
 *   Predicate, Function, Consumer, Supplier to standardowe końcówki; Twoja lambda to „śrubka”, która do nich pasuje.
 *   Własną końcówkę (własny interfejs) robisz tylko wtedy, gdy standardowa nie pasuje albo nazwa ma dużo znaczyć.
 *
 * JAK TO DZIAŁA: każdy interfejs to inny „kształt” — przyjmuje → zwraca
 *   {@code Predicate<T>}        test(T)        T → boolean       „czy spełnia warunek?”
 *   {@code Function<T, R>}      apply(T)       T → R             „zamień T na R”
 *   {@code Consumer<T>}         accept(T)      T → nic (void)    „zrób coś z T”
 *   {@code Supplier<T>}         get()          nic → T           „daj mi T”
 *   {@code UnaryOperator<T>}    apply(T)       T → T             „zmień T w inne T” (Function o tym samym typie)
 *   {@code BinaryOperator<T>}   apply(T, T)    (T, T) → T        „połącz dwa T w jedno T”
 *   {@code BiFunction<T, U, R>} / {@code BiConsumer<T, U>} / {@code BiPredicate<T, U>} — to samo, ale z DWOMA argumentami.
 *
 * SŁÓWKA:
 *   predicate = predykat (warunek); test = sprawdź; function = funkcja; apply = zastosuj; consumer = konsument
 *   (ten, kto „zjada” wartość); accept = przyjmij; supplier = dostawca; get = pobierz, daj; unary = jednoargumentowy;
 *   binary = dwuargumentowy; operator = operator (działanie); bi- = dwu-; primitive = typ prosty; boxing = pakowanie
 *   (int → Integer); specialization = specjalizacja (wersja dla konkretnego typu); callable = „do wywołania”; call = wywołaj.
 *
 * ZOBACZ TEŻ: Lambda02FunctionalInterfaces (czym jest interfejs funkcyjny), Lambda05Composition (and, andThen...),
 *             t16_streams/Streams03FilterMap (Predicate w filter, Function w map), t01_basics/Basics06Wrappers (Integer, pakowanie).
 * </pre>
 */
public class Lambda03JavaUtilFunction {

    public static void main(String[] args) {
        title("Lambda03 — pakiet java.util.function");

        whyStandardInterfaces();     // why standard interfaces = po co gotowe interfejsy
        predicate();                 // predicate = predykat (warunek)
        function();                  // function = funkcja (przekształcenie)
        consumerAndSupplier();       // consumer and supplier = konsument i dostawca
        operators();                 // operators = operatory (UnaryOperator, BinaryOperator)
        twoArgumentVersions();       // two argument versions = wersje dwuargumentowe (Bi...)
        primitiveSpecializations();  // primitive specializations = wersje dla typów prostych
        outsideThePackage();         // outside the package = poza pakietem (Runnable, Callable, Comparator)
        chooseTheInterface();        // choose the interface = dobierz interfejs
        exercises();                 // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PO CO GOTOWE INTERFEJSY
    // =================================================================================================

    /** TextTransformer = przekształcacz tekstu. Nasz własny interfejs z Lambda02 — dla porównania. */
    @FunctionalInterface
    interface TextTransformer {
        String transform(String text);
    }

    /**
     * 1. Własny interfejs a gotowy — ta sama lambda, ten sam efekt. Gotowy ma dodatkowo metody do łączenia
     * (andThen, compose — Lambda05Composition) i jest zrozumiały dla każdego programisty Javy.
     */
    static void whyStandardInterfaces() {
        section("1. Po co gotowe interfejsy — własny kontra standardowy");

        TextTransformer own = s -> s.trim().toUpperCase();          // własny (Lambda02)
        UnaryOperator<String> standard = s -> s.trim().toUpperCase();   // gotowy: String → String

        show("własny TextTransformer", own.transform("  java  "));
        show("gotowy UnaryOperator<String>", standard.apply("  java  "));
        // WYNIK: własny TextTransformer → JAVA
        // WYNIK: gotowy UnaryOperator<String> → JAVA

        // Biblioteka Javy SAMA używa tych interfejsów w swoich metodach. Przykłady, które już znasz:
        //   list.removeIf(Predicate<? super E>)       list.forEach(Consumer<? super E>)
        //   list.replaceAll(UnaryOperator<E>)         map.computeIfAbsent(K, Function<? super K, ? extends V>)
        //   map.merge(K, V, BiFunction<...>)          map.forEach(BiConsumer<? super K, ? super V>)
        // Streamy (t16_streams) to samo: filter(Predicate), map(Function), forEach(Consumer), reduce(BinaryOperator)...
        // Znasz te interfejsy = rozumiesz sygnatury połowy biblioteki standardowej.

        // WZÓR NAZEWNICTWA (klucz do ok. 40 interfejsów pakietu):
        //   Bi...          = dwa argumenty                 (BiFunction, BiConsumer, BiPredicate)
        //   Int/Long/Double... = przyjmuje typ prosty       (IntPredicate, LongFunction, DoubleConsumer)
        //   ToInt/ToLong/ToDouble... = zwraca typ prosty    (ToIntFunction, ToDoubleBiFunction)
        //   Obj...Consumer = obiekt + typ prosty            (ObjIntConsumer)
        //   ...Operator    = wejście i wyjście tego samego typu (UnaryOperator, IntBinaryOperator)
        note("Sześć podstawowych: Predicate, Function, Consumer, Supplier, UnaryOperator, BinaryOperator.");
        // WYNIK: ℹ Sześć podstawowych: Predicate, Function, Consumer, Supplier, UnaryOperator, BinaryOperator.
    }

    // =================================================================================================
    // 2. PREDICATE — WARUNEK
    // =================================================================================================

    /**
     * 2. {@code Predicate<T>}: przyjmuje T, zwraca boolean. Metoda: test.
     * Wszędzie, gdzie trzeba odpowiedzieć „tak/nie” o elemencie: filtrowanie, usuwanie, walidacja.
     */
    static void predicate() {
        section("2. Predicate<T> — warunek: T → boolean (test)");

        Predicate<Integer> isEven = n -> n % 2 == 0;                 // is even = czy parzysta
        Predicate<String> isLong = s -> s.length() > 6;              // is long = czy długie
        show("isEven.test(10)", isEven.test(10));
        show("isLong.test(\"lambda\")", isLong.test("lambda"));
        // WYNIK: isEven.test(10) → true
        // WYNIK: isLong.test("lambda") → false    ← 6 liter to nie „więcej niż 6”

        // removeIf przyjmuje właśnie Predicate — możemy podać gotową zmienną:
        List<String> words = new ArrayList<>(SampleData.words());
        words.removeIf(isLong);
        show("słowa po removeIf(isLong)", words);
        // WYNIK: słowa po removeIf(isLong) → [java, stream, lambda, java, mapa, lista, stream, java, rekord, enum]

        // Predicate o obiektach — warunek na produkcie:
        Predicate<Product> isBook = p -> p.category() == Category.KSIAZKI;
        show("pierwszy produkt to książka?", isBook.test(SampleData.products().get(0)));
        // WYNIK: pierwszy produkt to książka? → false
        // Predicate ma też metody do łączenia: and, or, negate, Predicate.not — Lambda05Composition.
    }

    // =================================================================================================
    // 3. FUNCTION — PRZEKSZTAŁCENIE
    // =================================================================================================

    /**
     * 3. {@code Function<T, R>}: przyjmuje T, zwraca R (typ wyniku może być INNY niż typ wejścia). Metoda: apply.
     * Pierwszy parametr typu = wejście, drugi = wyjście:  {@code Function<Product, String>} = „z produktu robi napis”.
     */
    static void function() {
        section("3. Function<T, R> — przekształcenie: T → R (apply)");

        Function<String, Integer> length = s -> s.length();          // String → Integer
        Function<Product, String> label = p -> p.name() + " [" + p.sku() + "]";   // Product → String; label = etykieta
        show("length.apply(\"kolekcja\")", length.apply("kolekcja"));
        show("label.apply(pierwszy produkt)", label.apply(SampleData.products().get(0)));
        // WYNIK: length.apply("kolekcja") → 8
        // WYNIK: label.apply(pierwszy produkt) → Laptop Pro 14 [ELE-001]

        // Prawdziwe użycie: Map.computeIfAbsent(klucz, Function) — „jeśli klucza nie ma, UTWÓRZ wartość funkcją”.
        // Klasyczny wzorzec grupowania bez streamów (EnumMap = mapa z kluczami enum, w kolejności stałych):
        Map<Category, List<String>> byCategory = new EnumMap<>(Category.class);
        for (Product p : SampleData.products()) {
            byCategory.computeIfAbsent(p.category(), category -> new ArrayList<>()).add(p.name());
        }
        showEach("produkty według kategorii", byCategory);
        // WYNIK: produkty według kategorii (liczba kluczy: 5):
        // WYNIK: • ELEKTRONIKA → [Laptop Pro 14, Smartfon X, Słuchawki BT, Monitor 27 cali]
        // WYNIK: • SPOZYWCZE → [Kawa ziarnista 1kg, Czekolada gorzka, Oliwa z oliwek]
        // WYNIK: • KSIAZKI → [Czysty kod, Java. Podstawy, Wzorce projektowe]
        // WYNIK: • ODZIEZ → [Kurtka zimowa, T-shirt bawełniany]
        // WYNIK: • DOM → [Ekspres do kawy, Lampka biurkowa]
        // Funkcja  category -> new ArrayList<>()  jest wołana TYLKO dla nowego klucza (5 razy, a nie 14).
        // W t16_streams/Streams11GroupingBy to samo zrobisz jednym wywołaniem groupingBy.
    }

    // =================================================================================================
    // 4. CONSUMER I SUPPLIER
    // =================================================================================================

    /**
     * 4. {@code Consumer<T>}: przyjmuje T, nic nie zwraca (accept) — „zrób coś z tym”: wypisz, zapisz, wyślij.
     * {@code Supplier<T>}: nic nie przyjmuje, zwraca T (get) — „daj mi coś”: nowy obiekt, wartość domyślną, kolejną liczbę.
     * To lustrzane odbicia: Consumer tylko bierze, Supplier tylko daje.
     */
    static void consumerAndSupplier() {
        section("4. Consumer<T> (T → nic) i Supplier<T> (nic → T)");

        // Consumer — forEach przyjmuje właśnie Consumer:
        Consumer<Employee> printCard = e -> System.out.println("   [" + e.department() + "] " + e.name());   // print card = wypisz wizytówkę
        SampleData.employees().subList(0, 2).forEach(printCard);
        // WYNIK: [IT] Anna Nowak
        // WYNIK: [IT] Piotr Kowalski

        // Supplier — każde get() może dać NOWY obiekt:
        Supplier<List<String>> newCart = () -> new ArrayList<>();    // new cart = nowy koszyk
        List<String> cart1 = newCart.get();
        List<String> cart2 = newCart.get();
        cart1.add("kawa");
        show("dwa get() — dwa różne obiekty?", (cart1 != cart2) + ", koszyk 2: " + cart2);
        // WYNIK: dwa get() — dwa różne obiekty? → true, koszyk 2: []

        // Supplier jako źródło kolejnych wartości. Random z ziarnem (seed) 42 — zawsze te same „losowe” liczby:
        Random random = new Random(42);
        Supplier<Integer> dice = () -> random.nextInt(6) + 1;        // dice = kostka; nextInt(6) = od 0 do 5
        List<Integer> rolls = new ArrayList<>();                      // rolls = rzuty
        for (int i = 0; i < 5; i++) {
            rolls.add(dice.get());
        }
        show("5 rzutów kostką", rolls);
        // WYNIK: 5 rzutów kostką → [3, 4, 1, 3, 1]

        // Najważniejsza cecha Suppliera: kod w środku wykonuje się dopiero przy get() — i tylko wtedy.
        // Dzięki temu można odłożyć kosztowne obliczenia „na później, jeśli będą potrzebne”
        // (Lambda07HigherOrderFunctions, sekcja 5; Optional.orElseGet w t14_optional).
    }

    // =================================================================================================
    // 5. OPERATORY — WEJŚCIE I WYJŚCIE TEGO SAMEGO TYPU
    // =================================================================================================

    /**
     * 5. {@code UnaryOperator<T>} to {@code Function<T, T>} (dziedziczy po niej) — typ wejścia = typ wyjścia.
     * {@code BinaryOperator<T>} to {@code BiFunction<T, T, T>} — dwa T na wejściu, jedno T na wyjściu.
     * Istnieją, bo krócej się je pisze: {@code UnaryOperator<String>} zamiast {@code Function<String, String>}.
     */
    static void operators() {
        section("5. UnaryOperator<T> (T → T) i BinaryOperator<T> ((T, T) → T)");

        // UnaryOperator — List.replaceAll przyjmuje właśnie UnaryOperator:
        UnaryOperator<String> capitalize = s -> s.substring(0, 1).toUpperCase() + s.substring(1);   // capitalize = wielka pierwsza litera
        List<String> names = new ArrayList<>(List.of("anna", "piotr", "ewa"));
        names.replaceAll(capitalize);
        show("replaceAll(capitalize)", names);
        // WYNIK: replaceAll(capitalize) → [Anna, Piotr, Ewa]

        // BinaryOperator — „połącz dwa w jedno”. Sumowanie cen (add = dodaj) w zwykłej pętli:
        BinaryOperator<BigDecimal> plus = (a, b) -> a.add(b);
        BigDecimal total = BigDecimal.ZERO;                           // ZERO = 0 (stała BigDecimal)
        for (Product p : SampleData.products()) {
            if (p.category() == Category.KSIAZKI) {
                total = plus.apply(total, p.price());
            }
        }
        show("suma cen książek", total);
        // WYNIK: suma cen książek → 307.00

        BinaryOperator<String> longer = (a, b) -> a.length() >= b.length() ? a : b;   // longer = dłuższy
        show("dłuższe z \"kot\" i \"chomik\"", longer.apply("kot", "chomik"));
        // WYNIK: dłuższe z "kot" i "chomik" → chomik
        // Dokładnie taki BinaryOperator przyjmuje reduce w streamach (t16_streams/Streams07Reduce).
    }

    // =================================================================================================
    // 6. WERSJE DWUARGUMENTOWE: BiFunction, BiConsumer, BiPredicate
    // =================================================================================================

    /**
     * 6. Przedrostek Bi = dwa argumenty (mogą być RÓŻNYCH typów). Ostatni parametr typu w BiFunction to wynik:
     * {@code BiFunction<BigDecimal, Integer, BigDecimal>} = „(cena, ilość) → kwota”.
     * Uwaga: nie ma TriFunction — przy trzech argumentach piszesz własny interfejs (albo przekazujesz rekord).
     */
    static void twoArgumentVersions() {
        section("6. BiFunction, BiConsumer, BiPredicate — dwa argumenty");

        // BiFunction<T, U, R>: (cena, ilość) → wartość pozycji; multiply = pomnóż; valueOf = zamień int na BigDecimal
        BiFunction<BigDecimal, Integer, BigDecimal> lineTotal = (price, qty) -> price.multiply(BigDecimal.valueOf(qty));
        show("lineTotal.apply(64.99, 3)", lineTotal.apply(new BigDecimal("64.99"), 3));
        // WYNIK: lineTotal.apply(64.99, 3) → 194.97

        // BiPredicate<T, U>: (tekst, limit) → czy dłuższy niż limit
        BiPredicate<String, Integer> longerThan = (text, limit) -> text.length() > limit;
        show("longerThan.test(\"lambda\", 5)", longerThan.test("lambda", 5));
        // WYNIK: longerThan.test("lambda", 5) → true

        // Map.merge(klucz, wartość, BiFunction) — licznik wystąpień słów. merge = scal:
        // gdy klucza nie ma, wstawia 1; gdy jest, woła funkcję (stara wartość, nowa wartość) i zapisuje jej wynik.
        Map<String, Integer> counts = new TreeMap<>();               // TreeMap = klucze posortowane
        for (String w : SampleData.words()) {
            counts.merge(w, 1, (oldCount, one) -> oldCount + one);
        }
        show("liczniki słów", counts);
        // WYNIK: liczniki słów → {enum=1, java=3, kolekcja=1, lambda=1, lista=1, mapa=1, optional=1, rekord=1, stream=2}

        // BiConsumer<K, V> — Map.forEach przyjmuje właśnie BiConsumer (klucz, wartość):
        BiConsumer<String, Integer> printRepeated = (word, count) -> {   // print repeated = wypisz powtórzone
            if (count > 1) {
                System.out.println("   " + word + " × " + count);
            }
        };
        counts.forEach(printRepeated);
        // WYNIK: java × 3
        // WYNIK: stream × 2
    }

    // =================================================================================================
    // 7. WERSJE DLA TYPÓW PROSTYCH — I DLACZEGO ISTNIEJĄ
    // =================================================================================================

    /**
     * 7. Generyki działają tylko z obiektami: {@code Function<int, int>} się nie skompiluje, trzeba pisać
     * {@code Function<Integer, Integer>}. Każde wywołanie wtedy PAKUJE int w obiekt Integer (boxing) i rozpakowuje
     * (unboxing). To kosztuje: nowe obiekty w pamięci, praca dla odśmiecacza (garbage collector).
     * Dlatego są wersje dla int, long i double, bez pakowania.
     */
    static void primitiveSpecializations() {
        section("7. Wersje dla typów prostych: IntPredicate, ToIntFunction, IntUnaryOperator...");

        IntPredicate isPositive = n -> n > 0;                        // int → boolean   (test)
        IntUnaryOperator square = n -> n * n;                        // int → int       (applyAsInt)
        IntBinaryOperator maxOfTwo = (a, b) -> Math.max(a, b);       // (int, int) → int (applyAsInt); Math.max = większa
        ToIntFunction<String> length = s -> s.length();              // T → int         (applyAsInt)
        IntFunction<String> stars = n -> "*".repeat(n);              // int → R         (apply); stars = gwiazdki
        IntSupplier answer = () -> 42;                               // nic → int       (getAsInt); answer = odpowiedź
        ObjIntConsumer<StringBuilder> appendTimes = (sb, n) -> sb.append(n).append("×");   // (T, int) → nic (accept)

        StringBuilder sb = new StringBuilder();
        appendTimes.accept(sb, 3);
        show("IntPredicate / IntUnaryOperator", isPositive.test(-5) + " / " + square.applyAsInt(7));
        show("IntBinaryOperator / ToIntFunction", maxOfTwo.applyAsInt(3, 9) + " / " + length.applyAsInt("stream"));
        show("IntFunction / IntSupplier / ObjIntConsumer", stars.apply(5) + " / " + answer.getAsInt() + " / " + sb);
        // WYNIK: IntPredicate / IntUnaryOperator → false / 49
        // WYNIK: IntBinaryOperator / ToIntFunction → 9 / 6
        // WYNIK: IntFunction / IntSupplier / ObjIntConsumer → ***** / 42 / 3×
        // PUŁAPKA: metody NIE nazywają się tak samo jak w wersjach obiektowych: applyAsInt, getAsInt, applyAsDouble...
        //   (as int = „jako int”). IDE podpowie, ale warto wiedzieć, czego szukać.

        // Dowód, że Function<Integer, Integer> tworzy OBIEKTY: porównanie przez == (porównuje referencje, nie wartości!)
        Function<Integer, Integer> boxedDouble = n -> n * 2;          // boxed = opakowany
        IntUnaryOperator primitiveDouble = n -> n * 2;
        show("boxed: apply(500) == apply(500)", boxedDouble.apply(500) == boxedDouble.apply(500));
        show("boxed: apply(50) == apply(50)", boxedDouble.apply(50) == boxedDouble.apply(50));
        show("int:   applyAsInt(500) == applyAsInt(500)", primitiveDouble.applyAsInt(500) == primitiveDouble.applyAsInt(500));
        // WYNIK: boxed: apply(500) == apply(500) → false    ← dwa RÓŻNE obiekty Integer z wartością 1000
        // WYNIK: boxed: apply(50) == apply(50) → true       ← 100 mieści się w pamięci podręcznej Integer (-128..127)
        // WYNIK: int:   applyAsInt(500) == applyAsInt(500) → true    ← zwykłe liczby int, bez obiektów
        // PUŁAPKA: obiekty Integer porównuj przez equals, nie ==. Dla małych liczb == „działa” (cache), dla dużych — nie
        //   (t01_basics/Basics06Wrappers).
        // DOBRA PRAKTYKA: w kodzie liczącym dużo liczb (pętle po milionach elementów) wybieraj Int.../Long.../Double...
        //   W zwykłym kodzie biznesowym różnica jest pomijalna — liczy się czytelność.
    }

    // =================================================================================================
    // 8. INTERFEJSY FUNKCYJNE SPOZA java.util.function
    // =================================================================================================

    /**
     * 8. Interfejs funkcyjny to KAŻDY interfejs z jedną metodą abstrakcyjną — nie tylko te z java.util.function.
     * Najważniejsze „starsze”: Runnable (java.lang), {@code Callable<V>} (java.util.concurrent), {@code Comparator<T>} (java.util).
     */
    static void outsideThePackage() {
        section("8. Runnable, Callable, Comparator — interfejsy funkcyjne spoza pakietu");

        Runnable hello = () -> System.out.println("   Runnable: nic nie przyjmuję, nic nie zwracam");
        hello.run();
        // WYNIK: Runnable: nic nie przyjmuję, nic nie zwracam

        // Callable<V>: jak Supplier (nic → V), ale metoda call() MOŻE rzucać wyjątki sprawdzane (throws Exception).
        Callable<String> loadConfig = () -> {
            throw new IOException("Brak pliku config.txt");              // wolno! Supplier by na to nie pozwolił
        };
        try {
            loadConfig.call();
        } catch (Exception e) {                                         // call() deklaruje throws Exception → trzeba obsłużyć
            show("Callable rzucił", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        // WYNIK: Callable rzucił → IOException: Brak pliku config.txt
        // Callable przekazuje się np. do puli wątków: executor.submit(callable) — t21_concurrency/Concurrency04Executors.
        // Wyjątki sprawdzane w lambdach to osobny temat: Lambda08Pitfalls.

        // Comparator<T>: (T, T) → int. Sortujemy produkty po stanie magazynowym malejąco:
        Comparator<Product> byStockDesc = (a, b) -> Integer.compare(b.stock(), a.stock());
        List<Product> products = new ArrayList<>(SampleData.products());
        products.sort(byStockDesc);
        show("największe zapasy (3)", products.subList(0, 3));
        // WYNIK: największe zapasy (3) → [Czekolada gorzka (7.49 zł), Kawa ziarnista 1kg (64.99 zł), T-shirt bawełniany (49.99 zł)]

        // Porównanie (pełna tabela „przyjmuje → zwraca” jest w ŚCIĄDZE na końcu pliku):
        //   Runnable  run()        nic → nic              Supplier<T>  get()   nic → T
        //   Callable  call()       nic → V (+ wyjątki)    Comparator   compare (T, T) → int
    }

    // =================================================================================================
    // 9. DOBIERZ INTERFEJS
    // =================================================================================================

    /**
     * 9. Jak wybrać interfejs? Zadaj dwa pytania: ILE i CO przyjmuje? CO zwraca?
     * <ul>
     *   <li>zwraca boolean → Predicate (dwa argumenty: BiPredicate),</li>
     *   <li>nic nie zwraca → Consumer (dwa argumenty: BiConsumer; zero argumentów: Runnable),</li>
     *   <li>nic nie przyjmuje, coś zwraca → Supplier (z wyjątkami sprawdzanymi: Callable),</li>
     *   <li>przyjmuje i zwraca ten sam typ → UnaryOperator / BinaryOperator,</li>
     *   <li>przyjmuje jedno, zwraca coś innego → Function (dwa argumenty: BiFunction),</li>
     *   <li>w grę wchodzi int/long/double → rozważ wersję Int.../ToInt...</li>
     * </ul>
     */
    static void chooseTheInterface() {
        section("9. Dobierz interfejs — siedem zadań, siedem interfejsów");

        // a) „czy e-mail zawiera @?”  — String → boolean
        Predicate<String> hasAt = email -> email.contains("@");
        // b) „utwórz pusty koszyk”  — nic → List
        Supplier<List<String>> emptyCart = () -> new ArrayList<>();
        // c) „wypisz produkt”  — Product → nic
        Consumer<Product> printName = p -> System.out.println("   produkt: " + p.name());
        // d) „cena × ilość”  — (BigDecimal, Integer) → BigDecimal
        BiFunction<BigDecimal, Integer, BigDecimal> times = (price, qty) -> price.multiply(BigDecimal.valueOf(qty));
        // e) „większa z dwóch kwot”  — (BigDecimal, BigDecimal) → BigDecimal; max = większa
        BinaryOperator<BigDecimal> bigger = (a, b) -> a.max(b);
        // f) „długość słowa”  — String → int (bez pakowania)
        ToIntFunction<String> wordLength = w -> w.length();
        // g) „usuń spacje z brzegów”  — String → String
        UnaryOperator<String> strip = s -> s.strip();                 // strip = obierz (usuń białe znaki; Java 11+)

        show("a) Predicate", hasAt.test("jan@example.com"));
        show("b) Supplier", emptyCart.get());
        printName.accept(SampleData.products().get(2));
        show("d) BiFunction", times.apply(new BigDecimal("7.49"), 10));
        show("e) BinaryOperator", bigger.apply(new BigDecimal("129.00"), new BigDecimal("99.00")));
        show("f) ToIntFunction", wordLength.applyAsInt("optional"));
        show("g) UnaryOperator", "[" + strip.apply("  kawa  ") + "]");
        // WYNIK: a) Predicate → true
        // WYNIK: b) Supplier → []
        // WYNIK: produkt: Słuchawki BT
        // WYNIK: d) BiFunction → 74.90
        // WYNIK: e) BinaryOperator → 129.00
        // WYNIK: f) ToIntFunction → 8
        // WYNIK: g) UnaryOperator → [kawa]

        // DOBRA PRAKTYKA: gdy żaden gotowy interfejs nie pasuje (np. 3 argumenty albo wyjątek sprawdzany),
        //   napisz własny z @FunctionalInterface (Lambda02FunctionalInterfaces, Lambda08Pitfalls).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA: przyjmuje → zwraca
     *   Interfejs               metoda        przyjmuje     zwraca       przykład lambdy
     *   Predicate<T>            test          T             boolean      s -> s.isEmpty()
     *   Function<T, R>          apply         T             R            p -> p.name()
     *   Consumer<T>             accept        T             nic (void)   s -> System.out.println(s)
     *   Supplier<T>             get           nic           T            () -> new ArrayList<>()
     *   UnaryOperator<T>        apply         T             T            s -> s.trim()
     *   BinaryOperator<T>       apply         T, T          T            (a, b) -> a.add(b)
     *   BiFunction<T, U, R>     apply         T, U          R            (price, qty) -> price.multiply(...)
     *   BiConsumer<T, U>        accept        T, U          nic          (k, v) -> System.out.println(k + v)
     *   BiPredicate<T, U>       test          T, U          boolean      (s, n) -> s.length() > n
     *   IntPredicate            test          int           boolean      n -> n > 0
     *   IntUnaryOperator        applyAsInt    int           int          n -> n * n
     *   IntBinaryOperator       applyAsInt    int, int      int          (a, b) -> a + b
     *   ToIntFunction<T>        applyAsInt    T             int          s -> s.length()
     *   IntFunction<R>          apply         int           R            n -> "*".repeat(n)
     *   IntSupplier             getAsInt      nic           int          () -> 42
     *   ObjIntConsumer<T>       accept        T, int        nic          (sb, n) -> sb.append(n)
     *   Runnable (java.lang)    run           nic           nic          () -> doWork()
     *   Callable<V> (concurrent) call         nic           V (+ wyjątki sprawdzane)
     *   Comparator<T> (java.util) compare     T, T          int          (a, b) -> a.compareTo(b)
     *   • Wzór nazw: Bi = 2 argumenty; Int/Long/Double = przyjmuje typ prosty; ToInt... = zwraca typ prosty.
     *   • Wersje dla typów prostych istnieją, bo generyki nie działają z int → Integer = pakowanie (koszt, pułapka ==).
     *
     * PYTANIA KONTROLNE:
     *   1. Jaki interfejs wybierzesz dla: „czy pracownik jest z działu IT?”, „wypisz zamówienie”, „utwórz nową mapę”,
     *      „zamień produkt na jego cenę”?
     *   2. Czym różni się UnaryOperator<String> od Function<String, String>? Czy jeden można przekazać tam, gdzie
     *      oczekiwany jest drugi?
     *   3. Co wypisze ten kod?
     *          Function<Integer, Integer> f = n -> n + 1;
     *          System.out.println(f.apply(999) == f.apply(999));
     *          System.out.println(f.apply(99) == f.apply(99));
     *   4. ZNAJDŹ BŁĄD:
     *          Supplier<String> s = name -> "Cześć, " + name;
     *   5. ZNAJDŹ BŁĄD:
     *          IntUnaryOperator twice = n -> n * 2;
     *          int x = twice.apply(21);
     *   6. Czym różni się Callable od Supplier? Kiedy użyjesz którego?
     *   7. Który interfejs przyjmuje metoda map.merge jako trzeci argument i co ta funkcja dostaje na wejściu?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        List<Employee> firstThree = SampleData.employees().subList(0, 3);
        List<String> expected2 = List.of("AN (IT)", "PK (IT)", "KW (HR)");
        Map<Integer, List<String>> expected3 = Map.of(
                4, List.of("java", "java", "mapa", "java", "enum"),
                5, List.of("lista"),
                6, List.of("stream", "lambda", "stream", "rekord"),
                8, List.of("kolekcja", "optional"));

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: Predicate — dłuższe niż 5 i kończy się na „a”", List.of("lambda", "kolekcja"),
                () -> keep(SampleData.words(), exercise1()));
        Check.equal("ćw. 2: klasa anonimowa → lambda (inicjały)", expected2, () -> mapAll(firstThree, exercise2()));
        Check.equal("ćw. 3: computeIfAbsent — słowa według długości", expected3, () -> exercise3(SampleData.words()));
        Check.equal("ćw. 4a: suma stanów większych niż 10", 570,
                () -> exercise4(SampleData.products(), p -> p.stock(), n -> n > 10));
        Check.equal("ćw. 4b: suma cen (w pełnych zł) poniżej 100", 340,
                () -> exercise4(SampleData.products(), p -> p.price().intValue(), n -> n < 100));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("lambda", "kolekcja"), () -> keep(SampleData.words(), solution1()));
        Check.equal("ćw. 2 (wzorzec)", expected2, () -> mapAll(firstThree, solution2()));
        Check.equal("ćw. 3 (wzorzec)", expected3, () -> solution3(SampleData.words()));
        Check.equal("ćw. 4a (wzorzec)", 570, () -> solution4(SampleData.products(), p -> p.stock(), n -> n > 10));
        Check.equal("ćw. 4b (wzorzec)", 340, () -> solution4(SampleData.products(), p -> p.price().intValue(), n -> n < 100));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /** keep = zostaw. Pomocnicza: kopia listy bez elementów, które NIE spełniają warunku. */
    static <T> List<T> keep(List<T> items, Predicate<T> condition) {
        List<T> copy = new ArrayList<>(items);
        copy.removeIf(condition.negate());          // negate = zaprzeczenie: usuń te, które NIE spełniają
        return copy;
    }

    /** mapAll = przekształć wszystkie. Pomocnicza: nowa lista z wynikami funkcji dla każdego elementu. */
    static <T, R> List<R> mapAll(List<T> items, Function<T, R> mapper) {
        List<R> result = new ArrayList<>();
        for (T item : items) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć {@code Predicate<String>}, który przepuszcza słowa DŁUŻSZE niż 5 liter
     * i jednocześnie kończące się na literę „a”.
     * Podpowiedź: {@code s.length() > 5 && s.endsWith("a")} (endsWith = kończy się na).
     */
    static Predicate<String> exercise1() {
        // TODO: twoje rozwiązanie
        return s -> false;
    }

    /**
     * ĆWICZENIE 2 (łatwe): PRZEPISZ KLASĘ ANONIMOWĄ NA LAMBDĘ. Funkcja zamienia pracownika na jego inicjały i dział,
     * np. "Anna Nowak" z IT → "AN (IT)".
     * <pre>{@code
     *   return new Function<Employee, String>() {
     *       @Override
     *       public String apply(Employee e) {
     *           String[] parts = e.name().split(" ");
     *           return "" + parts[0].charAt(0) + parts[1].charAt(0) + " (" + e.department() + ")";
     *       }
     *   };
     * }</pre>
     * Podpowiedź: dwie instrukcje → lambda z ciałem blokowym (klamry i return). Dlaczego na początku jest ""?
     * Bez niego {@code charAt(0) + charAt(0)} DODAŁOBY kody dwóch znaków jako liczby (char + char = int)!
     */
    static Function<Employee, String> exercise2() {
        // TODO: twoje rozwiązanie
        return e -> "";
    }

    /**
     * ĆWICZENIE 3 (średnie): pogrupuj słowa według długości: klucz = długość, wartość = lista słów tej długości
     * (w kolejności z listy, z duplikatami). Zwróć TreeMap (klucze rosnąco). Łączy lambdy z kolekcjami (t12_collections).
     * Podpowiedź: {@code map.computeIfAbsent(w.length(), len -> new ArrayList<>()).add(w)} w pętli po słowach.
     */
    static Map<Integer, List<String>> exercise3(List<String> words) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): policz sumę liczb, które funkcja metric wylicza z produktów — ale tylko tych
     * liczb, które spełniają condition. Przykład: metric = stan magazynowy, condition = „większe niż 10” → 570.
     * Podpowiedź: wersje dla int — {@code metric.applyAsInt(p)} i {@code condition.test(value)}; suma w zwykłej pętli.
     * Dzięki ToIntFunction i IntPredicate nigdzie nie powstaje obiekt Integer.
     */
    static int exercise4(List<Product> products, ToIntFunction<Product> metric, IntPredicate condition) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Predicate<String> solution1() {
        return s -> s.length() > 5 && s.endsWith("a");
    }

    static Function<Employee, String> solution2() {
        return e -> {
            String[] parts = e.name().split(" ");                    // split = podziel (po spacji)
            return "" + parts[0].charAt(0) + parts[1].charAt(0) + " (" + e.department() + ")";
        };
    }

    static Map<Integer, List<String>> solution3(List<String> words) {
        Map<Integer, List<String>> byLength = new TreeMap<>();
        for (String w : words) {
            byLength.computeIfAbsent(w.length(), len -> new ArrayList<>()).add(w);
        }
        return byLength;
    }

    static int solution4(List<Product> products, ToIntFunction<Product> metric, IntPredicate condition) {
        int sum = 0;
        for (Product p : products) {
            int value = metric.applyAsInt(p);
            if (condition.test(value)) {
                sum += value;
            }
        }
        return sum;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Predicate<Employee>; Consumer<Order>; Supplier<Map<K, V>>; Function<Product, BigDecimal>.
     *   2. UnaryOperator<String> DZIEDZICZY po Function<String, String> — to Function z tym samym typem wejścia i wyjścia.
     *      UnaryOperator można przekazać tam, gdzie oczekiwana jest Function<String, String> (jest jej podtypem),
     *      ale nie odwrotnie.
     *   3. false, a potem true. 1000 to dwa różne obiekty Integer (== porównuje referencje); 100 mieści się
     *      w pamięci podręcznej Integer (-128..127), więc oba razy to ten sam obiekt. Wniosek: equals, nie ==.
     *   4. Supplier nic nie przyjmuje (get()), a lambda ma parametr name. Potrzebna Function<String, String>
     *      (albo UnaryOperator<String>).
     *   5. IntUnaryOperator nie ma metody apply — nazywa się applyAsInt: twice.applyAsInt(21).
     *   6. Oba: nic → wynik. Callable.call() może rzucać wyjątki sprawdzane (throws Exception), Supplier.get() — nie.
     *      Callable — zadania dla wątków i kod z wyjątkami sprawdzanymi; Supplier — zwykłe „daj wartość” (np. orElseGet).
     *   7. BiFunction (dokładniej BiFunction<? super V, ? super V, ? extends V>) — dostaje STARĄ wartość z mapy
     *      i NOWĄ wartość podaną w merge; jej wynik zostaje zapisany pod kluczem.
     */
    // </editor-fold>
}
