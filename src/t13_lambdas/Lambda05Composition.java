package t13_lambdas;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Customer;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Product;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Składanie funkcji — łączenie małych lambd w większe zachowania
 *        (composition = kompozycja, składanie; compose = złóż)
 *
 * W SKRÓCIE:
 *   Interfejsy funkcyjne z Javy mają metody default, które łączą lambdy w nowe lambdy:
 *   warunki — and / or / negate / Predicate.not; funkcje — andThen / compose; akcje — Consumer.andThen;
 *   komparatory — comparing / thenComparing / reversed / nullsFirst. Zamiast jednej wielkiej lambdy piszesz
 *   kilka małych, nazwanych, i składasz je jak klocki. Każda metoda składająca zwraca NOWY obiekt — oryginały
 *   się nie zmieniają.
 *
 * ANALOGIA: klocki LEGO.
 *   Każdy mały predykat czy funkcja to jeden klocek: „tani”, „na stanie”, „dodaj VAT”. and / andThen to wypustki,
 *   którymi łączysz klocki w większą budowlę. Klocek zostaje klockiem — możesz go użyć w wielu budowlach.
 *
 * JAK TO DZIAŁA:
 *   {@code isCheap.and(inStock)}             → nowy Predicate: {@code p -> isCheap.test(p) && inStock.test(p)}
 *   {@code f.andThen(g)}                      → nowa Function:  {@code x -> g.apply(f.apply(x))}   (najpierw f, potem g)
 *   {@code f.compose(g)}                      → nowa Function:  {@code x -> f.apply(g.apply(x))}   (najpierw g, potem f)
 *   {@code comparing(A).thenComparing(B)}     → nowy Comparator: najpierw po A, przy remisie po B
 *   Łańcuch czytasz OD LEWEJ: {@code a.or(b).and(c)} to (a LUB b) I c — jak zwykłe wywołania metod po kolei.
 *
 * SŁÓWKA:
 *   and / or / negate / not = i / lub / zaprzecz / nie; isEqual = czy równy; andThen = a potem; compose = złóż;
 *   identity = tożsamość (funkcja „zwróć to, co dostałeś”); minBy / maxBy = najmniejszy / największy według;
 *   comparing = porównując (według); thenComparing = następnie porównując; reversed = odwrócony;
 *   naturalOrder / reverseOrder = porządek naturalny / odwrotny; nullsFirst / nullsLast = null na początku / na końcu;
 *   pipeline = potok; step = krok; rule = reguła; cheap = tani; audit = rejestr (dziennik zdarzeń).
 *
 * ZOBACZ TEŻ: Lambda03JavaUtilFunction (same interfejsy), t12_collections/Collections07ComparableComparator (Comparator,
 *             Collator), t16_streams/Streams03FilterMap (and/or w filter), t16_streams/Streams05SortDistinctLimit (sorted).
 * </pre>
 */
public class Lambda05Composition {

    /** HUNDRED = sto. Stała BigDecimal tworzona RAZ. */
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public static void main(String[] args) {
        title("Lambda05 — składanie funkcji");

        predicateAndOrNegate();      // predicate and/or/negate = łączenie predykatów
        combiningOrder();            // combining order = kolejność łączenia
        predicateNotAndIsEqual();    // Predicate.not and isEqual = Predicate.not i isEqual
        functionAndThenCompose();    // function andThen/compose = składanie funkcji
        consumerAndIdentity();       // consumer andThen and identity = łączenie akcji i funkcja tożsamości
        comparatorComposition();     // comparator composition = składanie komparatorów
        nullsInComparators();        // nulls in comparators = null w komparatorach
        minByMaxBy();                // minBy / maxBy = najmniejszy / największy według komparatora
        pipelineFromList();          // pipeline from list = potok z listy funkcji
        exercises();                 // exercises = ćwiczenia
    }

    /** productNames = nazwy produktów spełniających warunek (pomocnicza — pętla zamiast streamu). */
    static List<String> productNames(List<Product> products, Predicate<Product> condition) {
        List<String> result = new ArrayList<>();
        for (Product p : products) {
            if (condition.test(p)) {
                result.add(p.name());
            }
        }
        return result;
    }

    /** keep = zostaw. Nowa lista z elementami, które spełniają warunek (pomocnicza — pętla zamiast streamu). */
    static <T> List<T> keep(List<T> items, Predicate<? super T> condition) {
        List<T> result = new ArrayList<>();
        for (T item : items) {
            if (condition.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /** employeeNames = imiona i nazwiska pracowników (w kolejności z listy). */
    static List<String> employeeNames(List<Employee> employees) {
        List<String> result = new ArrayList<>();
        for (Employee e : employees) {
            result.add(e.name());
        }
        return result;
    }

    // =================================================================================================
    // 1. PREDICATE: and / or / negate
    // =================================================================================================

    /**
     * 1. Predykaty łączymy metodami default: and (i), or (lub), negate (zaprzeczenie).
     * Działają jak {@code &&}, {@code ||} i {@code !} — łącznie ze skróconym wyliczaniem (short-circuit).
     */
    static void predicateAndOrNegate() {
        section("1. Predicate: and, or, negate");

        List<Product> products = SampleData.products();
        Predicate<Product> isCheap = p -> p.price().compareTo(HUNDRED) < 0;   // is cheap = czy tani (poniżej 100 zł)
        Predicate<Product> inStock = Product::inStock;                        // in stock = na stanie
        Predicate<Product> isBook = p -> p.category() == Category.KSIAZKI;    // is book = czy książka

        show("tanie I na stanie", productNames(products, isCheap.and(inStock)));
        show("książki LUB na stanie (liczba)", productNames(products, isBook.or(inStock)).size());
        show("NIE na stanie (negate)", productNames(products, inStock.negate()));
        // WYNIK: tanie I na stanie → [Kawa ziarnista 1kg, Czekolada gorzka, Czysty kod, Wzorce projektowe, T-shirt bawełniany]
        // WYNIK: książki LUB na stanie (liczba) → 12
        // WYNIK: NIE na stanie (negate) → [Smartfon X, Oliwa z oliwek]

        // Short-circuit: and NIE sprawdza drugiego warunku, gdy pierwszy dał false (tak jak &&).
        // Sprawdźmy predykatem, który „mówi”, kiedy działa (println = efekt uboczny, tylko do demonstracji):
        Predicate<Product> loudStock = p -> {
            System.out.println("   sprawdzam stan: " + p.name());
            return p.inStock();
        };
        Product laptop = products.get(0);
        Product cleanCode = products.get(7);
        show("isBook.and(loudStock) dla laptopa", isBook.and(loudStock).test(laptop));
        // WYNIK: isBook.and(loudStock) dla laptopa → false    ← „sprawdzam stan” się NIE wypisało
        show("isBook.and(loudStock) dla „Czysty kod”", isBook.and(loudStock).test(cleanCode));
        // WYNIK: sprawdzam stan: Czysty kod
        // WYNIK: isBook.and(loudStock) dla „Czysty kod” → true

        // DOBRA PRAKTYKA: nazwane predykaty (isCheap, inStock) + and/or czytają się jak zdanie:
        //   isCheap.and(inStock)  =  „tani i na stanie”. Tani warunek stawiaj PIERWSZY — drogi może się nie wykonać.
        // Oryginały się nie zmieniają: isCheap.and(inStock) tworzy NOWY predykat, isCheap dalej znaczy „tani”.
    }

    // =================================================================================================
    // 2. KOLEJNOŚĆ ŁĄCZENIA
    // =================================================================================================

    /**
     * 2. Metody wywołują się po kolei OD LEWEJ, więc {@code a.or(b).and(c)} to (a LUB b) I c.
     * Żeby dostać a LUB (b I c), trzeba zagnieździć: {@code a.or(b.and(c))}.
     */
    static void combiningOrder() {
        section("2. Kolejność łączenia ma znaczenie");

        List<Product> products = SampleData.products();
        Predicate<Product> isCheap = p -> p.price().compareTo(HUNDRED) < 0;
        Predicate<Product> isHome = p -> p.category() == Category.DOM;       // is home = czy z kategorii Dom
        Predicate<Product> inStock = Product::inStock;

        List<String> version1 = productNames(products, isCheap.or(isHome).and(inStock));   // (tani LUB dom) I na stanie
        List<String> version2 = productNames(products, isCheap.or(isHome.and(inStock)));   // tani LUB (dom I na stanie)
        show("(tani LUB dom) I na stanie", version1.size() + " " + version1);
        show("tani LUB (dom I na stanie)", version2.size() + " " + version2);
        // WYNIK: (tani LUB dom) I na stanie → 7 [Kawa ziarnista 1kg, Czekolada gorzka, Czysty kod, Wzorce projektowe, T-shirt bawełniany, Ekspres do kawy, Lampka biurkowa]
        // WYNIK: tani LUB (dom I na stanie) → 8 [Kawa ziarnista 1kg, Czekolada gorzka, Oliwa z oliwek, Czysty kod, Wzorce projektowe, T-shirt bawełniany, Ekspres do kawy, Lampka biurkowa]
        // Różnica: „Oliwa z oliwek” — tania, ale NIE na stanie. W wersji 2 warunek „na stanie” dotyczy tylko kategorii Dom.

        // PUŁAPKA: łańcuch  a.or(b).and(c)  wygląda jak  a || b && c,  ale w Javie && ma wyższy priorytet niż ||,
        //   więc  a || b && c  znaczy  a || (b && c)!  Łańcuch metod NIE zna priorytetów — liczy się kolejność wywołań.
        // DOBRA PRAKTYKA: przy mieszaniu and/or nazwij części pośrednie:
        //   Predicate<Product> cheapOrHome = isCheap.or(isHome);  →  cheapOrHome.and(inStock)
    }

    // =================================================================================================
    // 3. Predicate.not i Predicate.isEqual
    // =================================================================================================

    /**
     * 3. Metody STATYCZNE interfejsu Predicate:
     * {@code Predicate.not(p)} (Java 11+) — zaprzeczenie, wygodne dla referencji do metod;
     * {@code Predicate.isEqual(x)} — „czy równy x” (przez equals, bezpieczny dla null).
     */
    static void predicateNotAndIsEqual() {
        section("3. Predicate.not (Java 11+) i Predicate.isEqual");

        // Chcemy ZOSTAWIĆ niepuste napisy, czyli przekazać warunek „NIE isBlank”.
        //   !String::isBlank  się nie skompiluje („method reference not expected here”). Opcje:
        //   s -> !s.isBlank()                                   — lambda
        //   ((Predicate<String>) String::isBlank).negate()      — działa, ale rzutowanie jest brzydkie
        //   Predicate.not(String::isBlank)                      — najczytelniej
        List<String> raw = List.of("kawa", "  ", "", "herbata");
        show("niepuste (Predicate.not)", keep(raw, Predicate.not(String::isBlank)));
        show("niedostępne produkty (Predicate.not)", productNames(SampleData.products(), Predicate.not(Product::inStock)));
        // WYNIK: niepuste (Predicate.not) → [kawa, herbata]
        // WYNIK: niedostępne produkty (Predicate.not) → [Smartfon X, Oliwa z oliwek]
        // DOBRA PRAKTYKA: Predicate.not przydaje się tam, gdzie podajesz, co ZOSTAWIĆ (keep, filter w streamach).
        //   W removeIf podajesz, co USUNĄĆ — tam zwykle wystarczy sama referencja:  list.removeIf(String::isBlank).

        // isEqual — np. usuń wszystkie wystąpienia słowa "java":
        List<String> words = new ArrayList<>(SampleData.words());
        words.removeIf(Predicate.isEqual("java"));                 // = w -> "java".equals(w)
        show("słowa bez \"java\"", words);
        // WYNIK: słowa bez "java" → [stream, lambda, kolekcja, mapa, lista, stream, optional, rekord, enum]

        // isEqual jest bezpieczny dla null: Predicate.isEqual(null) przepuszcza tylko null.
        // Arrays.asList dopuszcza null (List.of — nie!) i pozwala zmieniać elementy, ale nie rozmiar — dlatego kopia.
        List<String> withNulls = new ArrayList<>(Arrays.asList("a", null, "b", null));
        withNulls.removeIf(Predicate.isEqual(null));               // = Objects::isNull
        show("bez null (isEqual(null))", withNulls);
        // WYNIK: bez null (isEqual(null)) → [a, b]
    }

    // =================================================================================================
    // 4. FUNCTION: andThen, compose, identity
    // =================================================================================================

    /**
     * 4. {@code f.andThen(g)} = najpierw f, POTEM g  →  g(f(x)).
     * {@code f.compose(g)} = najpierw g, potem f  →  f(g(x))  — zapis jak w matematyce: (f ∘ g)(x).
     */
    static void functionAndThenCompose() {
        section("4. Function: andThen, compose, identity");

        Function<Integer, Integer> plusOne = x -> x + 1;             // plus one = dodaj 1
        Function<Integer, Integer> timesTwo = x -> x * 2;            // times two = razy 2

        show("plusOne.andThen(timesTwo).apply(5)", plusOne.andThen(timesTwo).apply(5));
        show("plusOne.compose(timesTwo).apply(5)", plusOne.compose(timesTwo).apply(5));
        // WYNIK: plusOne.andThen(timesTwo).apply(5) → 12    ← (5 + 1) * 2: najpierw plusOne, potem timesTwo
        // WYNIK: plusOne.compose(timesTwo).apply(5) → 11    ← 5 * 2 + 1: najpierw timesTwo, potem plusOne
        // ZAPAMIĘTAJ: andThen = „a potem” — czytasz od lewej do prawej, w kolejności wykonania.
        //   compose robi odwrotnie. W praktyce prawie zawsze używa się andThen — jest czytelniejsze.

        // andThen może ZMIENIAĆ typ na każdym etapie: Product → String → Integer
        Function<Product, String> name = Product::name;
        Function<String, Integer> length = String::length;
        Function<Product, Integer> nameLength = name.andThen(length);     // name length = długość nazwy
        show("długość nazwy pierwszego produktu", nameLength.apply(SampleData.products().get(0)));
        // WYNIK: długość nazwy pierwszego produktu → 13    ← "Laptop Pro 14"

        // Function.identity() = funkcja „zwróć to, co dostałeś” (x -> x). Przydaje się jako „pusty krok”
        // albo punkt startowy przy składaniu w pętli (sekcja 9).
        Function<String, String> same = Function.identity();
        show("Function.identity().apply(\"bez zmian\")", same.apply("bez zmian"));
        // WYNIK: Function.identity().apply("bez zmian") → bez zmian
    }

    // =================================================================================================
    // 5. CONSUMER.andThen I UnaryOperator.identity
    // =================================================================================================

    /**
     * 5. {@code c1.andThen(c2)} — jedna akcja, która wykonuje c1, a potem c2 na TYM SAMYM elemencie.
     * {@code UnaryOperator.identity()} — to samo co {@code Function.identity()}, ale zwraca typ {@code UnaryOperator<T>}.
     */
    static void consumerAndIdentity() {
        section("5. Consumer.andThen i UnaryOperator.identity");

        List<String> audit = new ArrayList<>();                      // audit = rejestr zdarzeń
        Consumer<Product> print = p -> System.out.println("   wysyłam: " + p.name());
        Consumer<Product> register = p -> audit.add(p.sku());        // register = zarejestruj
        Consumer<Product> printAndRegister = print.andThen(register);

        SampleData.products().subList(0, 2).forEach(printAndRegister);
        show("rejestr", audit);
        // WYNIK: wysyłam: Laptop Pro 14
        // WYNIK: wysyłam: Smartfon X
        // WYNIK: rejestr → [ELE-001, ELE-002]
        // (Dodawanie do listy z zewnątrz to efekt uboczny — w tej roli Consumer jest OK, ale uważaj na wątki: Lambda06ClosuresScope.)

        // UnaryOperator.identity() — gdy potrzebujesz typu UnaryOperator (np. dla List.replaceAll):
        UnaryOperator<String> noChange = UnaryOperator.identity();
        List<String> letters = new ArrayList<>(List.of("a", "b"));
        letters.replaceAll(noChange);                                // replaceAll wymaga UnaryOperator — Function by nie przeszła
        show("replaceAll(UnaryOperator.identity())", letters);
        // WYNIK: replaceAll(UnaryOperator.identity()) → [a, b]
        // PUŁAPKA: Function.identity() zwraca Function<T, T>, a nie UnaryOperator<T> — nie przekażesz jej do replaceAll.
    }

    // =================================================================================================
    // 6. SKŁADANIE KOMPARATORÓW
    // =================================================================================================

    /**
     * 6. Comparator ma metody fabrykujące i składające:
     * {@code comparing(klucz)}, {@code comparingInt(klucz)}, {@code thenComparing(...)}, {@code reversed()},
     * {@code naturalOrder()}, {@code reverseOrder()}. Zamiast pisać compare ręcznie, opisujesz KLUCZE sortowania.
     */
    static void comparatorComposition() {
        section("6. Składanie komparatorów: comparing, thenComparing, reversed");

        List<Employee> employees = new ArrayList<>(SampleData.employees());

        // comparingInt(klucz typu int) — bez pakowania do Integer:
        employees.sort(Comparator.comparingInt(Employee::age));
        show("według wieku (3 najmłodszych)", employeeNames(employees.subList(0, 3)));
        // WYNIK: według wieku (3 najmłodszych) → [Piotr Kowalski, Agnieszka Zielińska, Ewa Woźniak]

        // reversed() — odwrotnie:
        employees.sort(Comparator.comparingInt(Employee::salary).reversed());
        show("według pensji malejąco (3)", employeeNames(employees.subList(0, 3)));
        // WYNIK: według pensji malejąco (3) → [Michał Lewandowski, Anna Nowak, Ewa Woźniak]

        // thenComparing — drugi klucz przy remisie. Dział (enum: kolejność stałych), potem nazwisko:
        employees.sort(Comparator.comparing(Employee::department).thenComparing(Employee::name));
        show("dział, potem imię i nazwisko", employeeNames(employees));
        // WYNIK: dział, potem imię i nazwisko → [Anna Nowak, Ewa Woźniak, Michał Lewandowski, Piotr Kowalski, Katarzyna Wiśniewska, Krzysztof Szymański, Tomasz Wójcik, Magdalena Kamińska, Agnieszka Zielińska, Paweł Dąbrowski]

        // PUŁAPKA: reversed() na KOŃCU łańcucha odwraca WSZYSTKIE klucze naraz:
        employees.sort(Comparator.comparing(Employee::department).thenComparing(Employee::name).reversed());
        show("…reversed() na końcu (3)", employeeNames(employees.subList(0, 3)));
        // WYNIK: …reversed() na końcu (3) → [Paweł Dąbrowski, Agnieszka Zielińska, Magdalena Kamińska]    ← dział też odwrócony!
        // Żeby odwrócić TYLKO drugi klucz, podaj mu osobny komparator:
        employees.sort(Comparator.comparing(Employee::department)
                .thenComparing(Employee::salary, Comparator.reverseOrder()));   // w dziale: pensja malejąco
        show("dział rosnąco, pensja malejąco (4)", employeeNames(employees.subList(0, 4)));
        // WYNIK: dział rosnąco, pensja malejąco (4) → [Michał Lewandowski, Anna Nowak, Ewa Woźniak, Piotr Kowalski]

        // naturalOrder / reverseOrder — porządek naturalny (compareTo) i odwrotny:
        List<String> words = new ArrayList<>(List.of("lambda", "java", "stream"));
        words.sort(Comparator.reverseOrder());
        show("reverseOrder()", words);
        // WYNIK: reverseOrder() → [stream, lambda, java]

        // PUŁAPKA: lambda zamiast referencji + reversed() = błąd kompilacji:
        //   Comparator<Product> c = Comparator.comparing(p -> p.price()).reversed();
        //   → „cannot find symbol  symbol: method price()  location: variable p of type Object”
        //   Dlaczego? Wywołanie .reversed() „odcina” typ docelowy — kompilator musi wydedukować typ p z samego
        //   comparing(...), więc przyjmuje Object. Rozwiązania: referencja Product::price, jawny typ (Product p) -> p.price()
        //   albo Comparator.<Product, BigDecimal>comparing(p -> p.price()).reversed().

        // Polskie litery: porządek naturalny to NIE alfabet polski. Collator (java.text) zna reguły języka:
        List<String> names = new ArrayList<>(List.of("Żaneta", "Adam", "Łukasz", "Zenon", "Ewa"));
        names.sort(Comparator.naturalOrder());
        show("naturalOrder()", names);
        names.sort(Collator.getInstance(Locale.forLanguageTag("pl-PL")));   // Collator to też Comparator
        show("Collator pl-PL", names);
        // WYNIK: naturalOrder() → [Adam, Ewa, Zenon, Łukasz, Żaneta]    ← Ł i Ż po wszystkich literach łacińskich
        // WYNIK: Collator pl-PL → [Adam, Ewa, Łukasz, Zenon, Żaneta]
        // Z kluczem:  Comparator.comparing(Employee::name, Collator.getInstance(...))  (t12_collections/Collections07ComparableComparator).
    }

    // =================================================================================================
    // 7. NULL W KOMPARATORACH
    // =================================================================================================

    /**
     * 7. naturalOrder() i comparing(...) nie znoszą null — rzucają NullPointerException.
     * {@code Comparator.nullsFirst(c)} / {@code nullsLast(c)} opakowują komparator: null trafia na początek / koniec,
     * a resztę porównuje c.
     */
    static void nullsInComparators() {
        section("7. null w komparatorach: nullsFirst, nullsLast");

        List<String> emails = new ArrayList<>();
        for (Customer c : SampleData.customers()) {
            emails.add(c.email());                                  // dwóch klientów nie ma e-maila (null)
        }

        expectThrows("sort(naturalOrder()) z null w liście", () -> emails.sort(Comparator.naturalOrder()));
        // WYNIK: ✔ sort(naturalOrder()) z null w liście → rzucono NullPointerException: Cannot invoke "java.lang.Comparable.compareTo(Object)" because "c1" is null
        // („nie można wywołać compareTo, bo c1 jest null” — c1 to nazwa parametru WEWNĄTRZ komparatora z biblioteki Javy.)

        emails.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
        show("nullsFirst", emails);
        emails.sort(Comparator.nullsLast(Comparator.naturalOrder()));
        show("nullsLast", emails);
        // WYNIK: nullsFirst → [null, null, adam.mazur@example.com, ewa.lis@example.com, jan@example.com, marek@example.com, zofia@example.com]
        // WYNIK: nullsLast → [adam.mazur@example.com, ewa.lis@example.com, jan@example.com, marek@example.com, zofia@example.com, null, null]

        // Gdy null może być KLUCZEM sortowania (pole obiektu), podaj nullsLast jako drugi argument comparing:
        //   Comparator.comparing(Customer::email, Comparator.nullsLast(Comparator.naturalOrder()))
        // PUŁAPKA: Comparator.nullsLast(...).thenComparing(...) chroni tylko przed null-owymi OBIEKTAMI (całymi klientami),
        //   a nie przed null w polu — to dwie różne sytuacje.
    }

    // =================================================================================================
    // 8. BinaryOperator.minBy / maxBy
    // =================================================================================================

    /**
     * 8. {@code BinaryOperator.minBy(komparator)} i {@code maxBy(komparator)} zamieniają Comparator
     * w operator „wybierz mniejszy / większy z dwóch”. Przydają się w pętli i w Map.merge.
     */
    static void minByMaxBy() {
        section("8. BinaryOperator.minBy i maxBy");

        List<Product> products = SampleData.products();
        Comparator<Product> byPrice = Comparator.comparing(Product::price);   // BigDecimal jest Comparable
        BinaryOperator<Product> cheaper = BinaryOperator.minBy(byPrice);      // cheaper = tańszy (z dwóch)

        Product cheapest = products.get(0);
        for (Product p : products) {
            cheapest = cheaper.apply(cheapest, p);
        }
        show("najtańszy produkt", cheapest);
        // WYNIK: najtańszy produkt → Czekolada gorzka (7.49 zł)

        // maxBy w Map.merge: najdroższy produkt w każdej kategorii (EnumMap — kolejność stałych enuma):
        Map<Category, Product> mostExpensive = new EnumMap<>(Category.class);
        for (Product p : products) {
            mostExpensive.merge(p.category(), p, BinaryOperator.maxBy(byPrice));
        }
        showEach("najdroższy w kategorii", mostExpensive);
        // WYNIK: najdroższy w kategorii (liczba kluczy: 5):
        // WYNIK: • ELEKTRONIKA → Laptop Pro 14 (5499.99 zł)
        // WYNIK: • SPOZYWCZE → Kawa ziarnista 1kg (64.99 zł)
        // WYNIK: • KSIAZKI → Java. Podstawy (129.00 zł)
        // WYNIK: • ODZIEZ → Kurtka zimowa (459.00 zł)
        // WYNIK: • DOM → Ekspres do kawy (1899.00 zł)
        // Przy remisie minBy i maxBy zostawiają PIERWSZY argument (tu: produkt, który był wcześniej).
        // W streamach to samo: reduce(BinaryOperator.minBy(...)) albo min(komparator) — t16_streams/Streams07Reduce.
    }

    // =================================================================================================
    // 9. POTOK Z LISTY FUNKCJI
    // =================================================================================================

    /**
     * 9. Skoro lambdy to obiekty, można je trzymać na liście i SKŁADAĆ w pętli w jedną funkcję.
     * Start: {@code Function.identity()} (dla funkcji) albo {@code p -> true} (dla warunków „wszystkie muszą być spełnione”).
     */
    static void pipelineFromList() {
        section("9. Potok z listy funkcji — składanie w pętli");

        List<UnaryOperator<String>> steps = List.of(
                String::strip,                                        // 1. usuń spacje z brzegów
                s -> s.toLowerCase(Locale.ROOT),                      // 2. małe litery
                s -> s.replaceAll("\\s+", " "),                       // 3. wiele spacji → jedna (regex \s+ = 1 lub więcej białych znaków)
                s -> s.replace(" ", "-"));                            // 4. spacje → myślniki

        Function<String, String> pipeline = Function.identity();      // start: „nic nie zmieniaj”
        for (UnaryOperator<String> step : steps) {
            pipeline = pipeline.andThen(step);                        // doklej kolejny krok na koniec
        }
        show("slug", pipeline.apply("  Zażółć   GĘŚLĄ  jaźń "));
        // WYNIK: slug → zażółć-gęślą-jaźń

        // To samo dla warunków — lista reguł, które produkt musi spełnić WSZYSTKIE:
        List<Predicate<Product>> rules = List.of(
                Product::inStock,
                p -> p.stock() < 20,
                p -> p.price().compareTo(HUNDRED) > 0);
        Predicate<Product> allRules = p -> true;                      // start: „przepuść wszystko”
        for (Predicate<Product> rule : rules) {
            allRules = allRules.and(rule);
        }
        show("spełnia wszystkie reguły", productNames(SampleData.products(), allRules));
        // WYNIK: spełnia wszystkie reguły → [Laptop Pro 14, Monitor 27 cali, Java. Podstawy, Kurtka zimowa, Ekspres do kawy, Lampka biurkowa]

        // DOBRA PRAKTYKA: reguły z listy (a nawet z pliku konfiguracyjnego) dają „konfigurowalne” zachowanie:
        //   dodanie reguły = dodanie elementu listy, bez zmiany pętli. W streamach: reduce(Predicate::and) — t16_streams.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   Predicate:   a.and(b)   a.or(b)   a.negate()   Predicate.not(p) (Java 11+)   Predicate.isEqual(x)
     *                Łańcuch liczony OD LEWEJ: a.or(b).and(c) = (a LUB b) I c. and/or mają short-circuit.
     *   Function:    f.andThen(g) = g(f(x)) — najpierw f;   f.compose(g) = f(g(x)) — najpierw g;   Function.identity()
     *   Consumer:    c1.andThen(c2) — obie akcje po kolei na tym samym elemencie.
     *   UnaryOperator.identity() — jak Function.identity(), ale typu UnaryOperator (np. dla replaceAll).
     *   BinaryOperator.minBy(cmp) / maxBy(cmp) — „mniejszy / większy z dwóch”; przy remisie zostaje pierwszy.
     *   Comparator:  comparing(klucz)  comparingInt(klucz)  thenComparing(klucz)  thenComparing(klucz, cmp)
     *                reversed()  naturalOrder()  reverseOrder()  nullsFirst(cmp)  nullsLast(cmp)
     *                reversed() na końcu odwraca WSZYSTKIE klucze; lambda + reversed() → podaj typ lub użyj ::.
     *   Collator.getInstance(Locale.forLanguageTag("pl-PL")) — sortowanie po polsku (Collator to Comparator).
     *   Składanie w pętli: start = Function.identity() / p -> true, potem f = f.andThen(krok) / p = p.and(reguła).
     *   Każda metoda składająca zwraca NOWY obiekt; oryginały się nie zmieniają.
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze ten kod?
     *          Function<Integer, Integer> f = x -> x - 3;
     *          Function<Integer, Integer> g = x -> x * x;
     *          System.out.println(f.andThen(g).apply(5) + " " + f.compose(g).apply(5));
     *   2. Czym różni się  a.or(b).and(c)  od  a.or(b.and(c))?  Który zapis odpowiada  a || b && c?
     *   3. ZNAJDŹ BŁĄD:
     *          Comparator<Product> byPriceDesc = Comparator.comparing(p -> p.price()).reversed();
     *   4. ZNAJDŹ BŁĄD (sortujemy pracowników: dział rosnąco, a w dziale pensja malejąco):
     *          Comparator.comparing(Employee::department).thenComparing(Employee::salary).reversed()
     *   5. Dlaczego  list.removeIf(!String::isBlank)  się nie kompiluje i czym to zastąpić?
     *   6. Co się stanie przy sortowaniu listy e-maili z null przez Comparator.naturalOrder()? Jak to naprawić?
     *   7. Jak z listy List<Predicate<T>> zrobić jeden predykat „wszystkie spełnione”? Od czego zacząć?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        List<String> expected1 = List.of("Piotr Kowalski", "Agnieszka Zielińska", "Paweł Dąbrowski");
        List<String> expected3 = List.of("Michał Lewandowski", "Anna Nowak", "Ewa Woźniak", "Piotr Kowalski",
                "Katarzyna Wiśniewska", "Krzysztof Szymański", "Tomasz Wójcik", "Magdalena Kamińska",
                "Paweł Dąbrowski", "Agnieszka Zielińska");
        List<String> expected4 = List.of("Adam Mazur", "Ewa Lis", "Jan Kowalski", "Marek Król", "Zofia Krawczyk",
                "Maria Nowak", "Ola Pawlak");
        List<UnaryOperator<String>> steps = List.of(String::strip, String::toUpperCase, s -> s + "!");

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: (IT LUB MARKETING) I pensja < 10 000", expected1, () -> exercise1(SampleData.employees()));
        Check.equal("ćw. 2: compose — najpierw kwadrat, potem -1", List.of(24, 8),
                () -> List.of(exercise2().apply(5), exercise2().apply(-3)));
        Check.equal("ćw. 3: klasa anonimowa → złożony komparator", expected3, () -> exercise3(SampleData.employees()));
        Check.equal("ćw. 4: klienci po e-mailu (null na końcu), potem po imieniu i nazwisku", expected4,
                () -> exercise4(SampleData.customers()));
        Check.equal("ćw. 5a: potok z listy kroków", "HEJ!", () -> exercise5(steps).apply("  hej "));
        Check.equal("ćw. 5b: pusta lista kroków = bez zmian", "abc", () -> exercise5(List.of()).apply("abc"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", expected1, () -> solution1(SampleData.employees()));
        Check.equal("ćw. 2 (wzorzec)", List.of(24, 8), () -> List.of(solution2().apply(5), solution2().apply(-3)));
        Check.equal("ćw. 3 (wzorzec)", expected3, () -> solution3(SampleData.employees()));
        Check.equal("ćw. 4 (wzorzec)", expected4, () -> solution4(SampleData.customers()));
        Check.equal("ćw. 5a (wzorzec)", "HEJ!", () -> solution5(steps).apply("  hej "));
        Check.equal("ćw. 5b (wzorzec)", "abc", () -> solution5(List.of()).apply("abc"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć imiona i nazwiska pracowników, którzy są z działu IT LUB MARKETING
     * i jednocześnie zarabiają MNIEJ niż 10 000 zł. Kolejność jak na liście.
     * Podpowiedź: trzy nazwane predykaty (isIt, isMarketing, lowSalary) połączone or/and — uważaj na kolejność (sekcja 2).
     * Imiona zbierzesz w pętli albo przez kopię listy + removeIf z negate().
     */
    static List<String> exercise1(List<Employee> employees) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć funkcję, która najpierw podnosi liczbę do kwadratu, a potem odejmuje 1
     * (5 → 24, -3 → 8). Zbuduj ją z dwóch funkcji square (x → x * x) i minusOne (x → x - 1),
     * używając compose (nie andThen).
     * Podpowiedź: {@code a.compose(b)} wykonuje NAJPIERW b. Która funkcja ma być „a”?
     */
    static Function<Integer, Integer> exercise2() {
        // TODO: twoje rozwiązanie
        return x -> 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ KLASĘ ANONIMOWĄ NA ZŁOŻONY KOMPARATOR. Poniższy komparator sortuje:
     * dział (kolejność stałych enuma), w dziale pensja MALEJĄCO, przy równej pensji — imię i nazwisko.
     * <pre>{@code
     *   copy.sort(new Comparator<Employee>() {
     *       @Override
     *       public int compare(Employee a, Employee b) {
     *           int byDepartment = a.department().compareTo(b.department());
     *           if (byDepartment != 0) {
     *               return byDepartment;
     *           }
     *           int bySalary = Integer.compare(b.salary(), a.salary());
     *           if (bySalary != 0) {
     *               return bySalary;
     *           }
     *           return a.name().compareTo(b.name());
     *       }
     *   });
     * }</pre>
     * Posortuj kopię listy łańcuchem comparing / thenComparing i zwróć imiona i nazwiska.
     * Podpowiedź: {@code Comparator.comparing(Employee::department)}, potem
     * {@code .thenComparing(Employee::salary, Comparator.reverseOrder())}, potem {@code .thenComparing(Employee::name)}.
     */
    static List<String> exercise3(List<Employee> employees) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 4 (średnie): posortuj klientów według e-maila (alfabetycznie), a klientów BEZ e-maila (null)
     * umieść na końcu; przy remisie (np. dwa null) — według imienia i nazwiska. Zwróć imiona i nazwiska.
     * Podpowiedź: {@code Comparator.comparing(Customer::email, Comparator.nullsLast(Comparator.naturalOrder()))}
     * i dalej {@code thenComparing(Customer::name)}. Pamiętaj o kopii listy.
     */
    static List<String> exercise4(List<Customer> customers) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): złóż listę kroków w JEDNĄ funkcję, która wykonuje je po kolei (od pierwszego).
     * Dla pustej listy funkcja ma zwracać tekst bez zmian.
     * Podpowiedź: sekcja 9 — start od {@code Function.identity()}, w pętli {@code f = f.andThen(krok)}.
     */
    static Function<String, String> exercise5(List<UnaryOperator<String>> steps) {
        // TODO: twoje rozwiązanie
        return text -> "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(List<Employee> employees) {
        Predicate<Employee> isIt = e -> e.department() == Department.IT;
        Predicate<Employee> isMarketing = e -> e.department() == Department.MARKETING;
        Predicate<Employee> lowSalary = e -> e.salary() < 10_000;
        Predicate<Employee> wanted = isIt.or(isMarketing).and(lowSalary);    // (IT LUB MARKETING) I niska pensja

        List<String> result = new ArrayList<>();
        for (Employee e : employees) {
            if (wanted.test(e)) {
                result.add(e.name());
            }
        }
        return result;
        // Błędna wersja  isIt.or(isMarketing.and(lowSalary))  dałaby też Annę, Michała i Ewę z IT (zarabiają ≥ 10 000).
    }

    static Function<Integer, Integer> solution2() {
        Function<Integer, Integer> square = x -> x * x;
        Function<Integer, Integer> minusOne = x -> x - 1;
        return minusOne.compose(square);          // najpierw square, potem minusOne = square.andThen(minusOne)
    }

    static List<String> solution3(List<Employee> employees) {
        List<Employee> copy = new ArrayList<>(employees);
        copy.sort(Comparator.comparing(Employee::department)
                .thenComparing(Employee::salary, Comparator.reverseOrder())
                .thenComparing(Employee::name));
        return employeeNames(copy);
    }

    static List<String> solution4(List<Customer> customers) {
        List<Customer> copy = new ArrayList<>(customers);
        copy.sort(Comparator.comparing(Customer::email, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Customer::name));
        List<String> names = new ArrayList<>();
        for (Customer c : copy) {
            names.add(c.name());
        }
        return names;
    }

    static Function<String, String> solution5(List<UnaryOperator<String>> steps) {
        Function<String, String> result = Function.identity();
        for (UnaryOperator<String> step : steps) {
            result = result.andThen(step);
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. „4 22”. andThen: najpierw f (5 - 3 = 2), potem g (2 * 2 = 4). compose: najpierw g (5 * 5 = 25), potem f (25 - 3 = 22).
     *   2. Pierwszy = (a LUB b) I c, drugi = a LUB (b I c). Zapis  a || b && c  to  a || (b && c)  (&& ma wyższy
     *      priorytet), czyli odpowiada DRUGIEMU. Łańcuch metod priorytetów nie zna — liczy się kolejność wywołań.
     *   3. Przez .reversed() kompilator nie zna typu docelowego dla comparing(...) i przyjmuje p jako Object
     *      („cannot find symbol: method price()”). Poprawnie: Comparator.comparing(Product::price).reversed()
     *      albo Comparator.comparing((Product p) -> p.price()).reversed().
     *   4. reversed() na końcu odwraca OBA klucze — działy też będą malejąco. Poprawnie:
     *      Comparator.comparing(Employee::department).thenComparing(Employee::salary, Comparator.reverseOrder()).
     *   5. Operator ! nie działa na referencji do metody („method reference not expected here”).
     *      Zastąp:  list.removeIf(s -> !s.isBlank())  albo  list.removeIf(Predicate.not(String::isBlank)).
     *   6. NullPointerException — naturalOrder wywołuje compareTo na null. Naprawa:
     *      Comparator.nullsFirst(Comparator.naturalOrder()) albo nullsLast(...).
     *   7. Zacznij od predykatu „zawsze true” (p -> true) i w pętli: result = result.and(reguła).
     *      (Dla „którykolwiek spełniony”: start p -> false i or.)
     */
    // </editor-fold>
}
