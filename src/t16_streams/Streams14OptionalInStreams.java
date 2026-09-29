package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Customer;
import helpers.model.Order;
import helpers.model.OrderStatus;
import helpers.model.Product;
import helpers.model.Student;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Optional w strumieniach — findFirst, max, flatMap(Optional::stream), ofNullable
 *        (optional = opcjonalny, „pudełko, które może być puste”)
 *
 * W SKRÓCIE:
 *   Część operacji końcowych może NIE MIEĆ wyniku (pusty strumień, nic nie przeszło przez filtr).
 *   Zamiast null zwracają Optional. Uczysz się tu bezpiecznie go rozpakowywać (bez get()),
 *   wyrzucać puste Optionale ze strumienia i unikać Optionali tam, gdzie nie pasują.
 *
 * ANALOGIA: Paczkomat. Dostajesz kod skrytki (Optional), ale skrytka może być pusta.
 *   get() = szarpnięcie drzwiczek bez patrzenia — jak pusto, jest awantura (wyjątek).
 *   orElse(...) = „jak pusto, biorę paczkę zastępczą”. map(...) = „jeśli coś jest, od razu to rozpakuj”.
 *
 * JAK TO DZIAŁA:
 *   Operacja                       Wynik                Dlaczego Optional?
 *   findFirst() / findAny()        {@code Optional<T>}  strumień może być pusty
 *   min(cmp) / max(cmp)            {@code Optional<T>}  z pustego nie ma najmniejszego
 *   reduce(op)  (bez identity)     {@code Optional<T>}  nie ma od czego zacząć
 *   reduce(identity, op)           T                    pusty → identity (np. 0)
 *   IntStream.average()            OptionalDouble       średnia z niczego nie istnieje
 *   count(), sum(), toList()       wartość              pusty → 0 / pusta lista (to NIE brak wyniku)
 *
 *   Optional → strumień:   opt.stream()  (9+)  daje 0 albo 1 element
 *   null → strumień:       Stream.ofNullable(x)  (9+)  daje 0 albo 1 element
 *
 * SŁÓWKA:
 *   find first = znajdź pierwszy; find any = znajdź dowolny; empty = pusty; present = obecny;
 *   or else = albo (w przeciwnym razie); or else throw = albo rzuć; nullable = mogący być null;
 *   smell (code smell) = zapach kodu (sygnał, że coś jest nie tak); fallback = plan awaryjny
 *
 * ZOBACZ TEŻ: t14_optional/Optional01Basics (tworzenie Optional), t14_optional/Optional02Transform
 *   (map, flatMap, filter na Optional), t14_optional/Optional03BestPractices (czego nie robić),
 *   t16_streams/Streams13AdvancedCollectors (collectingAndThen + maxBy), t16_streams/Streams16Laziness
 *   (orElse kontra orElseGet)
 * </pre>
 */
public class Streams14OptionalInStreams {

    /** Stałe BigDecimal poza lambdami — tworzone raz. */
    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final BigDecimal TEN_THOUSAND = new BigDecimal("10000");

    public static void main(String[] args) {
        title("Streams14 — Optional w strumieniach");

        operationsReturningOptional();   // operations returning Optional = operacje zwracające Optional
        insteadOfGet();                  // instead of get = zamiast get
        flatMapOptionalStream();         // flatMap Optional stream = spłaszcz Optional do strumienia
        optionalDoubleGrades();          // optional double grades = OptionalDouble ze średnich ocen
        mapOfOptionalsSmell();           // map of optionals smell = zapach: mapa Optionali
        streamOfNullable();              // stream of nullable = strumień z wartości, która może być null
        optionalListVsEmptyList();       // optional list vs empty list = Optional listy kontra pusta lista
        chainOfFallbacks();              // chain of fallbacks = łańcuch planów awaryjnych
        findTheBug();                    // find the bug = znajdź błąd
        exercises();                     // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. OPERACJE, KTÓRE ZWRACAJĄ OPTIONAL
    // =================================================================================================

    /**
     * 1. findFirst, findAny, min, max i reduce bez wartości startowej mogą „nic nie znaleźć”,
     * więc zwracają Optional. count, sum i toList zawsze mają wynik (dla pustego: 0 albo []).
     */
    static void operationsReturningOptional() {
        section("1. Operacje, które zwracają Optional");

        List<Product> products = SampleData.products();

        // findFirst = znajdź pierwszy (w kolejności strumienia)
        Optional<Product> firstExpensive = products.stream()
                .filter(p -> p.price().compareTo(THOUSAND) > 0)
                .findFirst();
        show("pierwszy droższy niż 1000 zł", firstExpensive);
        // WYNIK: pierwszy droższy niż 1000 zł → Optional[Laptop Pro 14 (5499.99 zł)]

        // findAny = znajdź dowolny. W strumieniu sekwencyjnym zwykle da pierwszy, ale NIE MA gwarancji
        // (w parallel może dać inny). Dlatego wypisujemy tylko, CZY coś znalazł. isPresent = czy obecny.
        boolean anyFound = products.stream()
                .filter(p -> p.price().compareTo(THOUSAND) > 0)
                .findAny()
                .isPresent();
        show("findAny coś znalazł?", anyFound);
        // WYNIK: findAny coś znalazł? → true

        // min / max z komparatorem (comparing = porównuj według)
        show("najtańszy", products.stream().min(Comparator.comparing(Product::price)));
        // WYNIK: najtańszy → Optional[Czekolada gorzka (7.49 zł)]
        show("najdroższy", products.stream().max(Comparator.comparing(Product::price)));
        // WYNIK: najdroższy → Optional[Laptop Pro 14 (5499.99 zł)]

        // reduce BEZ identity → Optional (nie ma od czego zacząć, gdy strumień pusty)
        show("suma liczb (reduce bez identity)", SampleData.numbers().stream().reduce(Integer::sum));
        // WYNIK: suma liczb (reduce bez identity) → Optional[66]
        show("suma pustego (reduce bez identity)", Stream.<Integer>empty().reduce(Integer::sum));
        // WYNIK: suma pustego (reduce bez identity) → Optional.empty
        show("suma pustego (reduce z identity 0)", Stream.<Integer>empty().reduce(0, Integer::sum));
        // WYNIK: suma pustego (reduce z identity 0) → 0

        // Dlaczego nie null? null nie mówi, CZY wynik mógł nie istnieć — łatwo o NullPointerException
        // trzy metody dalej. Optional w typie krzyczy: „sprawdź mnie!”.
        note("count(), sum(), toList() nie zwracają Optional — dla pustego strumienia: 0 albo []");
        // WYNIK: ℹ count(), sum(), toList() nie zwracają Optional — dla pustego strumienia: 0 albo []
    }

    // =================================================================================================
    // 2. ZAMIAST get(): map, filter, orElse, orElseThrow
    // =================================================================================================

    /**
     * 2. Optional rozpakowujemy „bezpiecznie”: map/filter działają tylko, gdy wartość jest,
     * a orElse/orElseThrow mówią wprost, co zrobić, gdy jej nie ma.
     */
    static void insteadOfGet() {
        section("2. Zamiast get(): map, filter, orElse, orElseThrow");

        List<Product> products = SampleData.products();

        // map = przekształć zawartość (jeśli jest); orElse = albo wartość zastępcza
        String cheapestHome = products.stream()
                .filter(p -> p.category() == Category.DOM)
                .min(Comparator.comparing(Product::price))
                .map(Product::name)
                .orElse("brak");
        show("najtańszy w DOM", cheapestHome);
        // WYNIK: najtańszy w DOM → Lampka biurkowa

        String superExpensive = products.stream()
                .filter(p -> p.price().compareTo(TEN_THOUSAND) > 0)
                .findFirst()
                .map(Product::name)
                .orElse("brak");
        show("droższy niż 10 000 zł", superExpensive);
        // WYNIK: droższy niż 10 000 zł → brak

        // filter na Optional: „zostaw wartość tylko, jeśli spełnia warunek”. inStock = na stanie.
        Optional<Product> smartphoneInStock = products.stream()
                .filter(p -> p.sku().equals("ELE-002"))
                .findFirst()
                .filter(Product::inStock);
        show("Smartfon X na stanie?", smartphoneInStock);
        // WYNIK: Smartfon X na stanie? → Optional.empty

        // orElseThrow() (Java 10+) — to samo co get(), ale NAZWA mówi prawdę: „albo rzuć”.
        expectThrows("orElseThrow() na pustym", () -> products.stream()
                .filter(p -> p.price().compareTo(TEN_THOUSAND) > 0)
                .findFirst()
                .orElseThrow());
        // WYNIK: ✔ orElseThrow() na pustym → rzucono NoSuchElementException: No value present

        // orElseThrow(dostawca wyjątku) — własny wyjątek z komunikatem, który coś mówi.
        expectThrows("orElseThrow(własny wyjątek)", () -> products.stream()
                .filter(p -> p.sku().equals("XYZ-999"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nie ma produktu o SKU XYZ-999")));
        // WYNIK: ✔ orElseThrow(własny wyjątek) → rzucono IllegalArgumentException: Nie ma produktu o SKU XYZ-999

        // DOBRA PRAKTYKA: wybór zakończenia
        //   brak wartości to normalna sytuacja  → orElse("...") / orElseGet(() -> ...)
        //   brak wartości to BŁĄD programu       → orElseThrow(() -> new ...Exception("co i dlaczego"))
        //   chcesz tylko coś zrobić, gdy jest   → ifPresent(...) / ifPresentOrElse(...) (9+)
        // PUŁAPKA: orElse(metoda()) wywołuje metodę ZAWSZE, nawet gdy wartość jest — kosztowne
        // wartości domyślne podawaj przez orElseGet (dokładny pomiar: Streams16Laziness).
    }

    // =================================================================================================
    // 3. flatMap(Optional::stream) (Java 9+) — wyrzucanie pustych
    // =================================================================================================

    /**
     * 3. Strumień Optionali to częsty widok: każdy klient ma {@code findEmail()} zwracające Optional.
     * {@code Optional.stream()} zamienia pełny Optional w strumień 1 elementu, a pusty w strumień pusty —
     * więc flatMap sam „wyparowuje” puste.
     */
    static void flatMapOptionalStream() {
        section("3. flatMap(Optional::stream) (Java 9+) — wyrzucanie pustych");

        List<Customer> customers = SampleData.customers();

        // findEmail = znajdź e-mail → Optional<String>
        List<Optional<String>> raw = customers.stream().map(Customer::findEmail).toList();
        show("surowo", raw);
        // WYNIK: surowo → [Optional[jan@example.com], Optional.empty, Optional[adam.mazur@example.com], Optional[zofia@example.com], Optional.empty, Optional[marek@example.com], Optional[ewa.lis@example.com]]

        // PRZED (Java 8): dwa kroki — filtruj obecne, potem get() (bezpieczne, bo tuż po filtrze).
        List<String> emailsOld = customers.stream()
                .map(Customer::findEmail)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        show("e-maile (Java 8)", emailsOld);
        // WYNIK: e-maile (Java 8) → [jan@example.com, adam.mazur@example.com, zofia@example.com, marek@example.com, ewa.lis@example.com]

        // PO (Java 9+): jeden krok. Optional::stream → 0 albo 1 element, flatMap skleja wszystko.
        List<String> emails = customers.stream()
                .map(Customer::findEmail)
                .flatMap(Optional::stream)
                .toList();
        show("e-maile (Java 9+)", emails);
        // WYNIK: e-maile (Java 9+) → [jan@example.com, adam.mazur@example.com, zofia@example.com, marek@example.com, ewa.lis@example.com]

        // A kto NIE ma e-maila? isEmpty (Java 11+) = czy pusty
        List<Customer> withoutEmail = customers.stream()
                .filter(c -> c.findEmail().isEmpty())
                .toList();
        show("bez e-maila", withoutEmail);
        // WYNIK: bez e-maila → [Maria Nowak (Kraków), Ola Pawlak (Poznań)]

        // PUŁAPKA: map zamiast flatMap na Optional → Optional w Optional.
        // Pierwsza klientka z Krakowa to Maria — bez e-maila.
        Optional<Optional<String>> nested = customers.stream()
                .filter(c -> c.city().equals("Kraków"))
                .findFirst()
                .map(Customer::findEmail);
        show("map → Optional w Optional", nested);
        // WYNIK: map → Optional w Optional → Optional[Optional.empty]

        Optional<String> flat = customers.stream()
                .filter(c -> c.city().equals("Kraków"))
                .findFirst()
                .flatMap(Customer::findEmail);
        show("flatMap → płaski Optional", flat);
        // WYNIK: flatMap → płaski Optional → Optional.empty

        // Uwaga na KOLEJNOŚĆ: „pierwszy klient z Krakowa → jego e-mail” to co innego niż
        // „pierwszy e-mail wśród klientów z Krakowa”. Tu Marek ratuje sytuację:
        Optional<String> firstKrakowEmail = customers.stream()
                .filter(c -> c.city().equals("Kraków"))
                .map(Customer::findEmail)
                .flatMap(Optional::stream)
                .findFirst();
        show("pierwszy e-mail z Krakowa", firstKrakowEmail);
        // WYNIK: pierwszy e-mail z Krakowa → Optional[marek@example.com]
    }

    // =================================================================================================
    // 4. OptionalDouble ze średniej ocen
    // =================================================================================================

    /**
     * 4. {@code Student.averageGrade()} zwraca OptionalDouble — pusty dla Henryka (brak ocen).
     * OptionalDouble NIE MA map ani filter, ale ma {@code stream()} (9+), który daje DoubleStream.
     */
    static void optionalDoubleGrades() {
        section("4. OptionalDouble ze średniej ocen");

        List<Student> students = SampleData.students();

        // averageGrade = średnia ocen. toString OptionalDouble: OptionalDouble[4.25] albo OptionalDouble.empty
        show("Ala", students.get(0).averageGrade());
        // WYNIK: Ala → OptionalDouble[4.25]
        show("Henryk", students.get(7).averageGrade());
        // WYNIK: Henryk → OptionalDouble.empty

        // Czytelny opis każdego ucznia. OptionalDouble nie ma map, więc: stream() → mapToObj → findFirst.
        // Locale.ROOT = format niezależny od ustawień komputera (zawsze kropka dziesiętna).
        List<String> described = students.stream()
                .map(s -> s.name() + ": " + s.averageGrade().stream()
                        .mapToObj(avg -> String.format(Locale.ROOT, "%.2f", avg))
                        .findFirst()
                        .orElse("brak ocen"))
                .toList();
        showEach("średnie", described);
        // WYNIK: średnie (liczba elementów: 8):
        // WYNIK: • Ala: 4.25
        // WYNIK: • Bartek: 3.00
        // WYNIK: • Celina: 4.75
        // WYNIK: • Darek: 2.50
        // WYNIK: • Ela: 4.50
        // WYNIK: • Filip: 3.50
        // WYNIK: • Gosia: 4.50
        // WYNIK: • Henryk: brak ocen

        // Średnia ze średnich — tylko uczniowie, którzy MAJĄ oceny. flatMapToDouble = spłaszcz do DoubleStream.
        double fair = students.stream()
                .map(Student::averageGrade)
                .flatMapToDouble(OptionalDouble::stream)
                .average()
                .orElse(0.0);
        show("średnia ze średnich (bez Henryka)", String.format(Locale.ROOT, "%.2f", fair));
        // WYNIK: średnia ze średnich (bez Henryka) → 3.86

        // PUŁAPKA: orElse(0.0) „w środku” zamienia brak ocen na zero → Henryk zaniża średnią całej szkoły.
        double unfair = students.stream()
                .mapToDouble(s -> s.averageGrade().orElse(0.0))
                .average()
                .orElse(0.0);
        show("BŁĘDNIE: Henryk jako 0.0", String.format(Locale.ROOT, "%.2f", unfair));
        // WYNIK: BŁĘDNIE: Henryk jako 0.0 → 3.38

        // Najlepszy uczeń: najpierw odfiltruj pustych, potem getAsDouble jest bezpieczne (tuż po filtrze).
        // comparingDouble = porównuj według double; getAsDouble = pobierz jako double
        Optional<Student> best = students.stream()
                .filter(s -> s.averageGrade().isPresent())
                .max(Comparator.comparingDouble(s -> s.averageGrade().getAsDouble()));
        show("najlepsza średnia", best);
        // WYNIK: najlepsza średnia → Optional[Celina [5, 5, 5, 4]]
    }

    // =================================================================================================
    // 5. ZAPACH: Map<K, Optional<V>>
    // =================================================================================================

    /**
     * 5. {@code groupingBy(..., maxBy(...))} daje mapę Optionali. To „zapach kodu”: każdy odczyt wymaga
     * dwóch sprawdzeń (czy klucz jest? czy Optional pełny?). Dwie poprawki: collectingAndThen albo toMap.
     */
    static void mapOfOptionalsSmell() {
        section("5. Zapach: Map<K, Optional<V>>");

        List<Product> products = SampleData.products();
        Comparator<Product> byPrice = Comparator.comparing(Product::price);

        Map<Category, Optional<Product>> smelly = products.stream()
                .collect(Collectors.groupingBy(Product::category, TreeMap::new, Collectors.maxBy(byPrice)));
        show("ODZIEZ (z Optional)", smelly.get(Category.ODZIEZ));
        // WYNIK: ODZIEZ (z Optional) → Optional[Kurtka zimowa (459.00 zł)]
        // Każdy odczyt: map.get(k) może dać null (brak klucza) ALBO Optional.empty — dwa rodzaje „nic”.

        // Poprawka 1: collectingAndThen(maxBy, Optional::get) — bezpieczne, bo grupy nie bywają puste (Streams13).
        Map<Category, Product> fix1 = products.stream()
                .collect(Collectors.groupingBy(Product::category, TreeMap::new,
                        Collectors.collectingAndThen(Collectors.maxBy(byPrice), Optional::get)));

        // Poprawka 2: toMap z funkcją łączącą BinaryOperator.maxBy — przy kolizji kluczy zostaje droższy.
        // identity = ten sam element; BinaryOperator.maxBy = „z dwóch wybierz większy”
        Map<Category, Product> fix2 = products.stream()
                .collect(Collectors.toMap(Product::category, Function.identity(),
                        BinaryOperator.maxBy(byPrice), TreeMap::new));
        showEach("najdroższy w kategorii (toMap + maxBy)", fix2);
        // WYNIK: najdroższy w kategorii (toMap + maxBy) (liczba kluczy: 5):
        // WYNIK: • ELEKTRONIKA → Laptop Pro 14 (5499.99 zł)
        // WYNIK: • SPOZYWCZE → Kawa ziarnista 1kg (64.99 zł)
        // WYNIK: • KSIAZKI → Java. Podstawy (129.00 zł)
        // WYNIK: • ODZIEZ → Kurtka zimowa (459.00 zł)
        // WYNIK: • DOM → Ekspres do kawy (1899.00 zł)

        show("obie poprawki dają to samo?", fix1.equals(fix2));
        // WYNIK: obie poprawki dają to samo? → true

        // DOBRA PRAKTYKA: Optional jest do ZWRACANIA z metody. Nie trzymaj go w mapie, liście ani polu.
    }

    // =================================================================================================
    // 6. Stream.ofNullable (Java 9+)
    // =================================================================================================

    /**
     * 6. {@code Stream.ofNullable(x)} daje pusty strumień dla null i jednoelementowy dla wartości.
     * Przydaje się przy starych API, które zamiast Optional zwracają null (np. {@code Map.get}).
     */
    static void streamOfNullable() {
        section("6. Stream.ofNullable (Java 9+)");

        List<Customer> customers = SampleData.customers();

        // email() = surowe pole rekordu — może być null (w przeciwieństwie do findEmail()).
        List<String> rawEmails = customers.stream().map(Customer::email).toList();
        show("surowe e-maile", rawEmails);
        // WYNIK: surowe e-maile → [jan@example.com, null, adam.mazur@example.com, zofia@example.com, null, marek@example.com, ewa.lis@example.com]

        // PUŁAPKA: null w strumieniu „jedzie dalej” i wybucha dopiero przy pierwszym użyciu.
        expectThrows("długości e-maili z nullami", () -> customers.stream()
                .map(Customer::email)
                .map(String::length)
                .toList());
        // WYNIK: ✔ długości e-maili z nullami → rzucono NullPointerException: (brak komunikatu)

        // Stream.ofNullable: null → 0 elementów, wartość → 1 element. flatMap skleja wszystko.
        List<Integer> lengths = customers.stream()
                .flatMap(c -> Stream.ofNullable(c.email()))
                .map(String::length)
                .toList();
        show("długości (ofNullable)", lengths);
        // WYNIK: długości (ofNullable) → [15, 22, 17, 17, 19]

        // Typowy przypadek: Map.get zwraca null, gdy klucza nie ma. Mapy NIE wypisujemy (Map.of — losowa kolejność).
        Map<String, List<String>> tagsBySku = Map.of(
                "ELE-001", List.of("laptop", "promocja"),
                "DOM-001", List.of("kawa"));
        List<String> tags = Stream.of("ELE-001", "ELE-002", "DOM-001")
                .flatMap(sku -> Stream.ofNullable(tagsBySku.get(sku)))   // ELE-002 → null → pomijamy
                .flatMap(List::stream)
                .toList();
        show("tagi dla trzech SKU", tags);
        // WYNIK: tagi dla trzech SKU → [laptop, promocja, kawa]

        // PUŁAPKA: Stream.of(null) to NIE jest pusty strumień — to strumień z JEDNYM elementem null.
        String missing = null;
        show("Stream.of(null).count()", Stream.of(missing).count());
        // WYNIK: Stream.of(null).count() → 1
        show("Stream.ofNullable(null).count()", Stream.ofNullable(missing).count());
        // WYNIK: Stream.ofNullable(null).count() → 0
    }

    // =================================================================================================
    // 7. Optional<List> kontra pusta lista
    // =================================================================================================

    /**
     * 7. Dla kolekcji „nic” to PUSTA kolekcja, nie Optional. {@code Optional<List<Order>>} ma aż trzy
     * rodzaje braku (null, Optional.empty, pusta lista) i zmusza każdego do dodatkowego rozpakowania.
     */
    static void optionalListVsEmptyList() {
        section("7. Optional<List> kontra pusta lista");

        // DOBRZE: metoda zwraca listę — dla klienta bez zamówień po prostu pustą.
        show("zamówienia klienta 1", ordersOf(1).stream().map(Order::id).toList());
        // WYNIK: zamówienia klienta 1 → [ZAM-001, ZAM-003, ZAM-010]
        show("zamówienia klienta 7 (Ewa Lis)", ordersOf(7));
        // WYNIK: zamówienia klienta 7 (Ewa Lis) → []
        show("liczba zamówień klienta 7", ordersOf(7).size());
        // WYNIK: liczba zamówień klienta 7 → 0

        // ŹLE: Optional<List<...>> — każdy wywołujący musi najpierw rozpakować Optional.
        int countBad = ordersOfBad(7).map(List::size).orElse(0);
        show("to samo przez Optional<List>", countBad);
        // WYNIK: to samo przez Optional<List> → 0
        // Ten sam wynik, ale więcej ceremonii — a i tak ktoś kiedyś zwróci Optional.of(List.of())
        // i wtedy „pusto” oznacza dwie różne rzeczy.

        // DOBRA PRAKTYKA: kolekcja, mapa, strumień → zwracaj PUSTE, nigdy null i nigdy Optional<kolekcja>.
    }

    /** DOBRZE: pusta lista, gdy klient nie ma zamówień. */
    static List<Order> ordersOf(long customerId) {
        return SampleData.orders().stream()
                .filter(o -> o.customer().id() == customerId)
                .toList();
    }

    /** ŹLE (tylko do porównania): Optional opakowujący listę. */
    static Optional<List<Order>> ordersOfBad(long customerId) {
        List<Order> found = ordersOf(customerId);
        return found.isEmpty() ? Optional.empty() : Optional.of(found);
    }

    // =================================================================================================
    // 8. Łańcuch planów awaryjnych — Optional.or (Java 9+)
    // =================================================================================================

    /**
     * 8. {@code opt.or(() -> innyOptional)} (Java 9+): jeśli pierwszy Optional jest pusty, spróbuj
     * następnego — wciąż jako Optional. Dopiero na końcu łańcucha decydujesz o orElse.
     */
    static void chainOfFallbacks() {
        section("8. Łańcuch planów awaryjnych — Optional.or (Java 9+)");

        // ANALOGIA: dzwonisz do szefa; nie odbiera → do zastępcy; nie odbiera → piszesz na skrzynkę biura.
        show("kontakt: Warszawa", contactFor("Warszawa"));
        // WYNIK: kontakt: Warszawa → jan@example.com
        show("kontakt: Kraków", contactFor("Kraków"));
        // WYNIK: kontakt: Kraków → marek@example.com
        show("kontakt: Łódź", contactFor("Łódź"));
        // WYNIK: kontakt: Łódź → biuro@example.com

        // Różnica: orElse kończy łańcuch (daje wartość), or daje kolejny Optional (łańcuch trwa dalej).
    }

    /** Najpierw e-mail VIP-a z miasta, potem dowolny e-mail z miasta, na końcu adres biura. */
    static String contactFor(String city) {
        return vipEmailIn(city)
                .or(() -> anyEmailIn(city))          // or = albo (spróbuj innego Optional)
                .orElse("biuro@example.com");
    }

    static Optional<String> vipEmailIn(String city) {
        return SampleData.customers().stream()
                .filter(c -> c.city().equals(city) && c.vip())
                .map(Customer::findEmail)
                .flatMap(Optional::stream)
                .findFirst();
    }

    static Optional<String> anyEmailIn(String city) {
        return SampleData.customers().stream()
                .filter(c -> c.city().equals(city))
                .map(Customer::findEmail)
                .flatMap(Optional::stream)
                .findFirst();
    }

    // =================================================================================================
    // 9. ZNAJDŹ BŁĄD: get() na pustym Optional
    // =================================================================================================

    /**
     * 9. Klasyka z code review: {@code findFirst().get()} „bo przecież zawsze coś jest”.
     * Aż do dnia, w którym nie ma.
     */
    static void findTheBug() {
        section("9. ZNAJDŹ BŁĄD: get() na pustym Optional");

        List<Customer> customers = SampleData.customers();

        // Kod z code review:
        //   String name = customers.stream().filter(c -> c.city().equals("Łódź")).findFirst().get().name();
        // Nie mamy klienta z Łodzi → pusty Optional → get() rzuca wyjątek.
        expectThrows("findFirst().get() — klient z Łodzi", () -> customers.stream()
                .filter(c -> c.city().equals("Łódź"))
                .findFirst()
                .get()
                .name());
        // WYNIK: ✔ findFirst().get() — klient z Łodzi → rzucono NoSuchElementException: No value present

        // Wersja 2 tego samego błędu: max po filtrze, który nic nie przepuścił (Ewa Lis nie ma zamówień).
        expectThrows("max().get() — największe zamówienie Ewy Lis", () -> SampleData.orders().stream()
                .filter(o -> o.customer().id() == 7)
                .max(Comparator.comparing(Order::total))
                .get());
        // WYNIK: ✔ max().get() — największe zamówienie Ewy Lis → rzucono NoSuchElementException: No value present

        // Poprawka A: wartość zastępcza.
        String name = customers.stream()
                .filter(c -> c.city().equals("Łódź"))
                .findFirst()
                .map(Customer::name)
                .orElse("(brak klienta)");
        show("poprawka A (orElse)", name);
        // WYNIK: poprawka A (orElse) → (brak klienta)

        // Poprawka B: ifPresentOrElse (Java 9+) = jeśli jest, zrób A, w przeciwnym razie zrób B.
        customers.stream()
                .filter(c -> c.city().equals("Łódź"))
                .findFirst()
                .ifPresentOrElse(
                        c -> note("znaleziono: " + c.name()),
                        () -> note("brak klienta z Łodzi — wysyłamy ofertę ogólną"));
        // WYNIK: ℹ brak klienta z Łodzi — wysyłamy ofertę ogólną

        // Poprawka C: jeśli brak to naprawdę błąd — orElseThrow z komunikatem, który pomoże przy awarii.
        // DOBRA PRAKTYKA: get() w nowym kodzie praktycznie nie istnieje. Jeśli piszesz get(),
        // zadaj sobie pytanie: „co ma się stać, gdy będzie pusto?” i zapisz to jawnie.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Optional zwracają: findFirst, findAny, min, max, reduce(op) — bo mogą nie mieć wyniku
     *   • nie zwracają: count, sum, toList, reduce(identity, op) — dla pustego: 0 / [] / identity
     *   • zamiast get(): map(...).orElse(...), orElseGet(() -> ...), orElseThrow(() -> new ...), ifPresentOrElse
     *   • strumień Optionali → .flatMap(Optional::stream) (9+) — puste znikają
     *   • Optional w Optional? → na Optional użyj flatMap zamiast map
     *   • OptionalDouble (average, averageGrade) nie ma map — użyj stream() albo isPresent + getAsDouble
     *   • orElse(0.0) przed average() fałszuje średnią — najpierw wyrzuć puste
     *   • Map<K, Optional<V>> = zapach → collectingAndThen(maxBy, Optional::get) albo
     *     toMap(klucz, identity(), BinaryOperator.maxBy(cmp), TreeMap::new)
     *   • null → Stream.ofNullable(x) (9+); Stream.of(null) to strumień z jednym nullem!
     *   • kolekcje zwracaj puste, nie Optional<List>; Optional nie do pól, parametrów ani map
     *   • łańcuch prób: opt1.or(() -> opt2).orElse(domyślna)  (or = 9+)
     *
     * PYTANIA KONTROLNE:
     *   1. Które operacje końcowe zwracają Optional i dlaczego akurat one?
     *   2. Co wypisze:  System.out.println(Stream.of(1, 2, 3).filter(n -> n > 5).findFirst());  ?
     *   3. Co wypisze:  System.out.println(Stream.of(Optional.of("a"), Optional.<String>empty(),
     *          Optional.of("b")).flatMap(Optional::stream).toList());  ?
     *   4. ZNAJDŹ BŁĄD:  customers.stream().map(Customer::findEmail).map(Optional::get).toList()
     *   5. Dlaczego Map<Category, Optional<Product>> to zapach kodu? Podaj dwie poprawki.
     *   6. Czym różni się Stream.of(x) od Stream.ofNullable(x), gdy x == null?
     *   7. Dlaczego metoda powinna zwracać pustą listę zamiast Optional<List<Order>>?
     *   8. ZNAJDŹ BŁĄD:  students.stream().mapToDouble(s -> s.averageGrade().orElse(0.0)).average()
     *      — dyrektor mówi, że średnia szkoły jest „dziwnie niska”.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        List<String> expected2 = List.of("adam.mazur@example.com", "marek@example.com", "ewa.lis@example.com");
        Map<String, String> expected3 = new TreeMap<>(Map.of(
                "Gdańsk", "Celina", "Kraków", "Ela", "Poznań", "Filip", "Warszawa", "Gosia"));

        Check.equal("ćw. 1: najdroższa książka", "Java. Podstawy", () -> exercise1(SampleData.products()));
        Check.equal("ćw. 1: pusta lista", "brak", () -> exercise1(List.of()));
        Check.equal("ćw. 2: e-maile klientów bez VIP", expected2, () -> exercise2(SampleData.customers()));
        Check.equal("ćw. 3: najlepszy uczeń w mieście", expected3, () -> exercise3(SampleData.students()));
        Check.equal("ćw. 4: DOSTARCZONE", Optional.of("jan@example.com"),
                () -> exercise4(SampleData.orders(), OrderStatus.DOSTARCZONE));
        Check.equal("ćw. 4: OPLACONE", Optional.of("zofia@example.com"),
                () -> exercise4(SampleData.orders(), OrderStatus.OPLACONE));
        Check.equal("ćw. 4: NOWE (klientka bez e-maila)", Optional.empty(),
                () -> exercise4(SampleData.orders(), OrderStatus.NOWE));
        Check.equal("ćw. 4: brak zamówień", Optional.empty(), () -> exercise4(List.of(), OrderStatus.NOWE));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "Java. Podstawy", () -> solution1(SampleData.products()));
        Check.equal("ćw. 1 pusta (wzorzec)", "brak", () -> solution1(List.of()));
        Check.equal("ćw. 2 (wzorzec)", expected2, () -> solution2(SampleData.customers()));
        Check.equal("ćw. 3 (wzorzec)", expected3, () -> solution3(SampleData.students()));
        Check.equal("ćw. 4 DOSTARCZONE (wzorzec)", Optional.of("jan@example.com"),
                () -> solution4(SampleData.orders(), OrderStatus.DOSTARCZONE));
        Check.equal("ćw. 4 OPLACONE (wzorzec)", Optional.of("zofia@example.com"),
                () -> solution4(SampleData.orders(), OrderStatus.OPLACONE));
        Check.equal("ćw. 4 NOWE (wzorzec)", Optional.empty(),
                () -> solution4(SampleData.orders(), OrderStatus.NOWE));
        Check.equal("ćw. 4 brak (wzorzec)", Optional.empty(), () -> solution4(List.of(), OrderStatus.NOWE));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 8 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć nazwę najdroższej książki (kategoria KSIAZKI) albo "brak",
     * gdy lista nie zawiera książek. Bez get()!
     * Podpowiedź: filter → max(Comparator.comparing(Product::price)) → map(Product::name) → orElse("brak").
     */
    static String exercise1(List<Product> products) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ na strumień z {@code findEmail()} i {@code flatMap(Optional::stream)}
     * — bez sprawdzania null ręcznie.
     * <pre>{@code
     * List<String> result = new ArrayList<>();
     * for (Customer c : customers) {
     *     if (!c.vip() && c.email() != null) {
     *         result.add(c.email());
     *     }
     * }
     * return result;
     * }</pre>
     * Podpowiedź: {@code filter(c -> !c.vip())} → map(Customer::findEmail) → flatMap(Optional::stream) → toList().
     */
    static List<String> exercise2(List<Customer> customers) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): dla każdego miasta (TreeMap) imię ucznia z najwyższą średnią.
     * Uczniów bez ocen pomiń (nie traktuj ich jak średniej 0). Wynik bez Optional w wartościach!
     * Podpowiedź: {@code filter(s -> s.averageGrade().isPresent())}, potem {@code groupingBy(Student::city, TreeMap::new,
     * collectingAndThen(maxBy(comparingDouble(...)), opt -> opt.map(Student::name).orElse("?")))}.
     */
    static Map<String, String> exercise3(List<Student> students) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): e-mail klienta, który złożył NAJDROŻSZE zamówienie o danym statusie.
     * Zwróć Optional.empty(), gdy nie ma zamówień o tym statusie ALBO gdy ten klient nie ma e-maila
     * (nie szukaj wtedy „następnego” zamówienia!).
     * Podpowiedź: filter → max(Comparator.comparing(Order::total)) → map(Order::customer)
     * → flatMap(Customer::findEmail). Uważaj: map zamiast flatMap da Optional w Optional.
     */
    static Optional<String> exercise4(List<Order> orders, OrderStatus status) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(List<Product> products) {
        return products.stream()
                .filter(p -> p.category() == Category.KSIAZKI)
                .max(Comparator.comparing(Product::price))
                .map(Product::name)
                .orElse("brak");
    }

    static List<String> solution2(List<Customer> customers) {
        return customers.stream()
                .filter(c -> !c.vip())
                .map(Customer::findEmail)
                .flatMap(Optional::stream)
                .toList();
    }

    static Map<String, String> solution3(List<Student> students) {
        return students.stream()
                .filter(s -> s.averageGrade().isPresent())
                .collect(Collectors.groupingBy(Student::city, TreeMap::new,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparingDouble(s -> s.averageGrade().getAsDouble())),
                                opt -> opt.map(Student::name).orElse("?"))));
    }

    static Optional<String> solution4(List<Order> orders, OrderStatus status) {
        return orders.stream()
                .filter(o -> o.status() == status)
                .max(Comparator.comparing(Order::total))
                .map(Order::customer)
                .flatMap(Customer::findEmail);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. findFirst, findAny, min, max i reduce bez identity — dla pustego strumienia nie istnieje
     *      „pierwszy”, „największy” ani „suma bez punktu startu”. Optional wymusza obsłużenie braku.
     *   2. Optional.empty  — filtr nic nie przepuścił.
     *   3. [a, b]  — Optional::stream daje 1 element dla pełnego i 0 dla pustego.
     *   4. Maria Nowak i Ola Pawlak nie mają e-maila → Optional::get na pustym rzuca NoSuchElementException.
     *      Poprawnie: .map(Customer::findEmail).flatMap(Optional::stream).toList().
     *   5. Każdy odczyt ma dwa rodzaje braku (null z get(klucz) i Optional.empty), trzeba rozpakowywać.
     *      Poprawki: groupingBy(..., collectingAndThen(maxBy(cmp), Optional::get)) albo
     *      toMap(klucz, Function.identity(), BinaryOperator.maxBy(cmp), TreeMap::new).
     *   6. Stream.of(null) → strumień z JEDNYM elementem null (count = 1, NPE przy pierwszym użyciu);
     *      Stream.ofNullable(null) → pusty strumień (count = 0).
     *   7. Pusta lista już znaczy „nic” — wywołujący może od razu iterować, liczyć, streamować.
     *      Optional<List> dodaje ceremonię i trzeci rodzaj braku (pusty Optional kontra pusta lista).
     *   8. orElse(0.0) zamienia „brak ocen” (Henryk) na średnią 0.0 i zaniża wynik (3.38 zamiast 3.86).
     *      Poprawnie: .map(Student::averageGrade).flatMapToDouble(OptionalDouble::stream).average().
     */
    // </editor-fold>
}
