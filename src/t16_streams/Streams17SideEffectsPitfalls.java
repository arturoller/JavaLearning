package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Customer;
import helpers.model.Employee;
import helpers.model.Order;
import helpers.model.OrderLine;
import helpers.model.OrderStatus;
import helpers.model.Product;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Pułapki strumieni i efekty uboczne — katalog ŹLE → DOBRZE
 *        (side effect = efekt uboczny; pitfall = pułapka)
 *
 * W SKRÓCIE:
 *   Strumień ma OPISYWAĆ wynik („co chcę dostać”), a nie zmieniać świat dookoła.
 *   Ta lekcja zbiera najczęstsze błędy z całego rozdziału. Każdy pokazany jest jako
 *   ŹLE (często z prawdziwym wyjątkiem) i DOBRZE (poprawiona wersja).
 *
 * ANALOGIA:
 *   Strumień to przepis kuchenny: składniki → kroki → danie. Kucharz, który w połowie
 *   przepisu dosypuje coś do cudzego garnka (zewnętrzna lista), podbiera z miski, z której
 *   właśnie nabiera (modyfikacja źródła), albo chce gotować drugi raz z tych samych, już
 *   zjedzonych składników (ponowne użycie) — dostaje bałagan, a nie obiad.
 *
 * JAK TO DZIAŁA:
 *   Czysta lambda = wynik zależy tylko od argumentu i lambda nic nie zmienia na zewnątrz.
 *   ┌───────────────────────────────┬──────────────────────────────────────────┐
 *   │ ŹLE                           │ DOBRZE                                   │
 *   ├───────────────────────────────┼──────────────────────────────────────────┤
 *   │ forEach(lista::add)           │ toList() / collect(...)                  │
 *   │ źródło.add(...) w trakcie     │ nowa lista / removeIf                    │
 *   │ drugi raz ten sam Stream      │ Supplier albo kolekcja.stream()          │
 *   │ logika w peek                 │ osobny, jawny krok                       │
 *   │ toMap bez funkcji łączącej    │ toMap(klucz, wartość, merge)/groupingBy  │
 *   │ sorted() bez Comparable, null │ Comparator.comparing + nullsFirst/Last   │
 *   │ distinct bez equals/hashCode  │ record albo map(klucz).distinct()        │
 *   │ Integer == Integer, int sum   │ equals, mapToInt, mapToLong              │
 *   │ potok na 12 linii lambd       │ nazwane metody i predykaty               │
 *   └───────────────────────────────┴──────────────────────────────────────────┘
 *
 * SŁÓWKA:
 *   side effect = efekt uboczny; stateless = bezstanowy; pure = czysty;
 *   concurrent modification = równoczesna modyfikacja; duplicate key = zduplikowany klucz;
 *   merge = połącz; boxing = opakowanie (int → Integer); overflow = przepełnienie;
 *   readability = czytelność; checked exception = wyjątek kontrolowany
 *
 * ZOBACZ TEŻ: t16_streams/Streams16Laziness (dlaczego peek bywa pomijany),
 *             t16_streams/Streams18Parallel (jak efekty uboczne psują strumienie równoległe),
 *             t16_streams/Streams10CollectorsToMap (funkcja łącząca w toMap),
 *             t13_lambdas/Lambda08Pitfalls (wyjątki checked w lambdach),
 *             t12_collections/Collections03IterationModification (ConcurrentModificationException)
 * </pre>
 */
public class Streams17SideEffectsPitfalls {

    private static final BigDecimal HUNDRED = new BigDecimal("100");     // HUNDRED = sto
    private static final BigDecimal THOUSAND = new BigDecimal("1000");   // THOUSAND = tysiąc
    private static final BigDecimal BUDGET = new BigDecimal("500");      // BUDGET = budżet

    /** Nazwany predykat — czyta się jak zdanie: „czy pozycja to elektronika”. */
    private static final Predicate<OrderLine> IS_ELECTRONICS =             // IS_ELECTRONICS = czy elektronika
            line -> line.product().category() == Category.ELEKTRONIKA;

    public static void main(String[] args) {
        title("Streams17 — pułapki strumieni i efekty uboczne");

        externalCollection();      // external collection = zewnętrzna kolekcja
        modifyingSource();         // modifying source = modyfikacja źródła
        reuseAndFields();          // reuse and fields = ponowne użycie i pola
        peekLogic();               // peek logic = logika w peek (podglądaniu)
        toMapTraps();              // toMap traps = pułapki toMap
        sortingTraps();            // sorting traps = pułapki sortowania
        distinctWithoutEquals();   // distinct without equals = distinct bez equals
        boxingMath();              // boxing math = rachunki na opakowaniach (Integer)
        readability();             // readability = czytelność
        whenLoopIsBetter();        // when loop is better = kiedy pętla jest lepsza
        exercises();               // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. ZEWNĘTRZNA LISTA I LICZNIK
    // =================================================================================================

    /**
     * 1. Najczęstszy błąd: strumień, który zamiast ZWRACAĆ wynik, dopisuje go do listy z zewnątrz.
     * Działa… dopóki ktoś nie doda {@code .parallel()} albo nie przestawi kroków.
     */
    static void externalCollection() {
        section("1. Zewnętrzna lista w forEach → toList() / collect");
        List<Product> products = SampleData.products();

        // PRZED (ŹLE): lambda zmienia listę spoza strumienia = efekt uboczny
        List<String> cheapBad = new ArrayList<>();
        products.stream()
                .filter(p -> p.price().compareTo(HUNDRED) < 0)      // compareTo = porównaj z
                .forEach(p -> cheapBad.add(p.name()));              // forEach = dla każdego
        show("tanie (forEach + add)", cheapBad);
        // WYNIK: tanie (forEach + add) → [Kawa ziarnista 1kg, Czekolada gorzka, Oliwa z oliwek, Czysty kod, Wzorce projektowe, T-shirt bawełniany]

        // PO (DOBRZE): strumień sam buduje i ZWRACA listę
        List<String> cheapGood = products.stream()
                .filter(p -> p.price().compareTo(HUNDRED) < 0)
                .map(Product::name)
                .toList();                                          // toList = do listy (Java 16+)
        show("tanie (toList)", cheapGood);
        // WYNIK: tanie (toList) → [Kawa ziarnista 1kg, Czekolada gorzka, Oliwa z oliwek, Czysty kod, Wzorce projektowe, T-shirt bawełniany]
        show("ta sama zawartość?", cheapBad.equals(cheapGood));
        // WYNIK: ta sama zawartość? → true

        // PUŁAPKA: wynik jest ten sam, więc „działa”. Ale:
        //   (1) ArrayList nie jest bezpieczna wątkowo — po .parallel() elementy giną albo leci
        //       ArrayIndexOutOfBoundsException (pokaz w Streams18Parallel),
        //   (2) lista jest modyfikowalna i żyje dłużej niż strumień — ktoś ją jeszcze „dopisze”,
        //   (3) czytelnik musi szukać, GDZIE powstaje wynik.

        // Licznik „z boku” — ten sam problem w wersji liczbowej
        AtomicInteger stockBad = new AtomicInteger();               // AtomicInteger = liczba z atomowymi operacjami
        products.stream().forEach(p -> stockBad.addAndGet(p.stock()));   // addAndGet = dodaj i pobierz
        int stockGood = products.stream().mapToInt(Product::stock).sum(); // mapToInt = mapuj na int; sum = suma
        show("stan magazynu: licznik / sum()", stockBad.get() + " / " + stockGood);
        // WYNIK: stan magazynu: licznik / sum() → 595 / 595

        // PRZED (ŹLE): numerowanie licznikiem w tablicy — wynik zależy od KOLEJNOŚCI wywołań lambdy
        int[] counter = {0};
        List<String> numberedBad = products.stream()
                .limit(3)                                           // limit = ogranicz do
                .map(p -> (++counter[0]) + ". " + p.name())
                .toList();
        show("numeracja licznikiem", numberedBad);
        // WYNIK: numeracja licznikiem → [1. Laptop Pro 14, 2. Smartfon X, 3. Słuchawki BT]

        // PO (DOBRZE): numer wynika z INDEKSU, a nie ze „stanu” trzymanego obok
        List<String> numberedGood = IntStream.range(0, 3)           // range = zakres [0, 3)
                .mapToObj(i -> (i + 1) + ". " + products.get(i).name())   // mapToObj = mapuj na obiekt
                .toList();
        show("numeracja z indeksu", numberedGood);
        // WYNIK: numeracja z indeksu → [1. Laptop Pro 14, 2. Smartfon X, 3. Słuchawki BT]

        // DOBRA PRAKTYKA: lambdy w filter/map mają być bezstanowe (stateless) i bez efektów ubocznych.
        // Efekt uboczny ma sens tylko w operacji końcowej forEach — i to taki, który NIE buduje wyniku
        // (wypisanie na ekran, wysłanie maila). Wynik zawsze przez toList()/collect(...).
    }

    // =================================================================================================
    // 2. MODYFIKACJA ŹRÓDŁA
    // =================================================================================================

    /**
     * 2. Zmiana źródła w trakcie przechodzenia strumieniem. ArrayList wykrywa to dopiero po fakcie
     * i rzuca {@code ConcurrentModificationException} — a zmiany już się wykonały!
     */
    static void modifyingSource() {
        section("2. Modyfikacja źródła → ConcurrentModificationException");

        List<String> names = new ArrayList<>(List.of("Ala", "Ola", "Ela"));   // List.of = lista z (Java 9+)
        // PRZED (ŹLE): dopisujemy do listy, którą właśnie przechodzi strumień
        expectThrows("add do źródła w forEach", () -> names.stream().forEach(n -> names.add(n + "!")));
        // WYNIK: ✔ add do źródła w forEach → rzucono ConcurrentModificationException: (brak komunikatu)
        show("lista PO wyjątku", names);
        // WYNIK: lista PO wyjątku → [Ala, Ola, Ela, Ala!, Ola!, Ela!]

        // PUŁAPKA: wyjątek poleciał dopiero NA KOŃCU (ArrayList porównuje licznik zmian modCount
        // po przejściu wszystkich elementów), więc lista JEST już zmieniona. Program „wybuchł”,
        // a dane i tak są zepsute — najgorszy możliwy stan. Przy usuwaniu bywa jeszcze dziwniej:
        // elementy przesuwają się „pod rękami” strumienia i lambda może dostać null albo pominąć element.
        // To zachowanie jest NIEZDEFINIOWANE — nie wolno na nim polegać.

        // PO (DOBRZE) 1: chcesz nowej listy? Zbuduj ją strumieniem, źródła nie ruszaj.
        List<String> base = List.of("Ala", "Ola", "Ela");
        List<String> withBang = Stream.concat(base.stream(), base.stream().map(n -> n + "!"))   // concat = połącz
                .toList();
        show("nowa lista", withBang);
        // WYNIK: nowa lista → [Ala, Ola, Ela, Ala!, Ola!, Ela!]

        // PO (DOBRZE) 2: chcesz usunąć elementy z istniejącej listy? Metoda KOLEKCJI removeIf.
        List<String> editable = new ArrayList<>(withBang);
        editable.removeIf(n -> n.endsWith("!"));                    // removeIf = usuń jeśli; endsWith = kończy się na
        show("po removeIf", editable);
        // WYNIK: po removeIf → [Ala, Ola, Ela]

        // DOBRA PRAKTYKA: źródło strumienia traktuj jak „tylko do odczytu”, dopóki strumień nie skończy.
        // Zmiany rób PRZED albo PO strumieniu — nigdy w jego trakcie.
    }

    // =================================================================================================
    // 3. PONOWNE UŻYCIE I STRUMIEŃ W POLU
    // =================================================================================================

    /**
     * 3. Strumień jest jednorazowy. Zapisanie go w zmiennej lub polu i drugie użycie kończy się
     * {@code IllegalStateException}. Rozwiązanie: trzymaj kolekcję albo {@code Supplier<Stream<T>>}.
     */
    static void reuseAndFields() {
        section("3. Ponowne użycie strumienia i strumień w polu → IllegalStateException");
        List<Product> products = SampleData.products();

        // PRZED (ŹLE): zapisujemy strumień do zmiennej i używamy dwa razy
        Stream<Product> inStock = products.stream().filter(Product::inStock);   // inStock = na stanie
        show("ile na stanie", inStock.count());                                 // count = policz
        // WYNIK: ile na stanie → 12
        expectThrows("drugie użycie tego samego strumienia", () -> inStock.map(Product::name).toList());
        // WYNIK: ✔ drugie użycie tego samego strumienia → rzucono IllegalStateException: stream has already been operated upon or closed
        // (komunikat po polsku: „na strumieniu już wykonano operację albo został zamknięty”)

        // PO (DOBRZE) 1: przechowuj PRZEPIS na strumień — Supplier (dostawca)
        Supplier<Stream<Product>> inStockSupplier =                              // Supplier = dostawca
                () -> products.stream().filter(Product::inStock);
        show("ile na stanie (Supplier)", inStockSupplier.get().count());         // get = pobierz (nowy strumień)
        // WYNIK: ile na stanie (Supplier) → 12
        show("2 pierwsze nazwy (Supplier)", inStockSupplier.get().map(Product::name).limit(2).toList());
        // WYNIK: 2 pierwsze nazwy (Supplier) → [Laptop Pro 14, Słuchawki BT]

        // PO (DOBRZE) 2: najprościej — zbierz RAZ do listy, a potem twórz z niej ile chcesz strumieni
        List<Product> inStockList = products.stream().filter(Product::inStock).toList();
        show("z listy: rozmiar / pierwszy", inStockList.size() + " / " + inStockList.get(0).name());
        // WYNIK: z listy: rozmiar / pierwszy → 12 / Laptop Pro 14

        // Wariant tej samej pułapki: strumień jako POLE obiektu
        BadCatalog bad = new BadCatalog(products);                  // BadCatalog = zły katalog (klasa niżej)
        show("BadCatalog: na stanie (1. raz)", bad.countInStock()); // countInStock = policz na stanie
        // WYNIK: BadCatalog: na stanie (1. raz) → 12
        expectThrows("BadCatalog: na stanie (2. raz)", bad::countInStock);
        // WYNIK: ✔ BadCatalog: na stanie (2. raz) → rzucono IllegalStateException: stream has already been operated upon or closed

        GoodCatalog good = new GoodCatalog(products);               // GoodCatalog = dobry katalog
        show("GoodCatalog: 1. i 2. raz", good.countInStock() + " i " + good.countInStock());
        // WYNIK: GoodCatalog: 1. i 2. raz → 12 i 12

        // DOBRA PRAKTYKA: w polach trzymaj KOLEKCJE (najlepiej niemodyfikowalne), a strumień zwracaj
        // z metody — każde wywołanie stream() daje świeży, nieużyty strumień.
    }

    /** Klasa z błędem: strumień w polu — da się go użyć tylko RAZ. */
    static final class BadCatalog {
        private final Stream<Product> products;                    // ŹLE: pole typu Stream

        BadCatalog(List<Product> source) { this.products = source.stream(); }

        long countInStock() { return products.filter(Product::inStock).count(); }   // 2. raz → wyjątek
    }

    /** Wersja poprawna: pole to lista (copyOf = niemodyfikowalna kopia, Java 10+), strumień na żądanie. */
    static final class GoodCatalog {
        private final List<Product> products;

        GoodCatalog(List<Product> source) { this.products = List.copyOf(source); }

        Stream<Product> stream() { return products.stream(); }     // zawsze NOWY strumień

        long countInStock() { return stream().filter(Product::inStock).count(); }
    }

    // =================================================================================================
    // 4. LOGIKA W PEEK
    // =================================================================================================

    /**
     * 4. {@code peek} to podglądanie do debugowania. Logika w nim (liczenie, zapis) potrafi się
     * wykonać częściowo albo wcale — bo strumień wykonuje tylko tyle pracy, ile musi.
     */
    static void peekLogic() {
        section("4. Logika w peek → może się nie wykonać");
        List<String> words = SampleData.words();

        // PRZED (ŹLE): liczymy w peek
        AtomicInteger seen = new AtomicInteger();
        long count = words.stream()
                .map(String::toUpperCase)                           // toUpperCase = na wielkie litery
                .peek(w -> seen.incrementAndGet())                  // peek = podejrzyj; incrementAndGet = zwiększ i pobierz
                .count();
        show("count()", count);
        // WYNIK: count() → 12
        show("ile razy wykonał się peek", seen.get());
        // WYNIK: ile razy wykonał się peek → 0
        // PUŁAPKA: źródło zna swój rozmiar, a map/peek go nie zmieniają → count() (od Java 9)
        // zwraca rozmiar BEZ przechodzenia elementów. peek nie ma czego podglądać (Streams16Laziness).

        // PRZED (ŹLE): „zapis do bazy” w peek, a na końcu operacja skracająca
        List<String> saved = new ArrayList<>();
        boolean anyLong = words.stream()
                .peek(saved::add)
                .anyMatch(w -> w.length() > 5);                     // anyMatch = czy którykolwiek pasuje
        show("czy jest słowo dłuższe niż 5", anyLong);
        // WYNIK: czy jest słowo dłuższe niż 5 → true
        show("„zapisane” w peek", saved);
        // WYNIK: „zapisane” w peek → [java, stream]
        // PUŁAPKA: anyMatch skończył po 2. elemencie (short-circuit = skrócone obliczanie),
        // więc 10 pozostałych słów „nie zapisało się”. Nikt nie dostał żadnego błędu.

        // PO (DOBRZE): najpierw wynik, POTEM jawna akcja na całym wyniku
        List<String> upper = words.stream().map(String::toUpperCase).toList();
        List<String> database = new ArrayList<>();                  // udajemy bazę danych
        database.addAll(upper);                                     // addAll = dodaj wszystkie — jawny „zapis”
        show("zapisano / policzono", database.size() + " / " + upper.size());
        // WYNIK: zapisano / policzono → 12 / 12

        // DOBRA PRAKTYKA: peek tylko do podglądania przy debugowaniu. Nic, od czego zależy wynik
        // programu, nie może siedzieć w peek.
    }

    // =================================================================================================
    // 5. TOMAP: DUPLIKATY I NULL
    // =================================================================================================

    /**
     * 5. {@code Collectors.toMap} rzuca wyjątek przy powtórzonym kluczu i przy wartości null.
     * Oba błędy wychodzą dopiero przy KONKRETNYCH danych — testy na „ładnych” danych ich nie złapią.
     */
    static void toMapTraps() {
        section("5. toMap: zduplikowane klucze i wartości null");
        List<Employee> employees = SampleData.employees();

        // PRZED (ŹLE): klucz „pensja” nie jest unikalny (dwie osoby zarabiają 9800)
        expectThrows("toMap(pensja → osoba)",
                () -> employees.stream().collect(Collectors.toMap(Employee::salary, Employee::name)));
        // WYNIK: ✔ toMap(pensja → osoba) → rzucono IllegalStateException: Duplicate key 9800 (attempted merging values Piotr Kowalski and Paweł Dąbrowski)

        // PO (DOBRZE): 3. argument = merge function (funkcja łącząca) mówi, co zrobić przy kolizji
        Map<Integer, String> bySalary = employees.stream()
                .collect(Collectors.toMap(Employee::salary, Employee::name,
                        (first, second) -> first + ", " + second,  // dwie wartości pod jednym kluczem
                        TreeMap::new));                             // TreeMap = mapa posortowana po kluczu
        show("pensja 9800", bySalary.get(9800));
        // WYNIK: pensja 9800 → Piotr Kowalski, Paweł Dąbrowski
        show("liczba kluczy (dla 10 osób)", bySalary.size());
        // WYNIK: liczba kluczy (dla 10 osób) → 9
        // DOBRA PRAKTYKA: jeśli kolizje są NORMALNE (wiele osób → jedna pensja), to nie „łataj”
        // toMap, tylko użyj groupingBy (t16_streams/Streams11GroupingBy).

        List<Customer> customers = SampleData.customers();
        // PRZED (ŹLE): Maria Nowak i Ola Pawlak nie mają e-maila (null)
        expectThrows("toMap(klient → e-mail) z null",
                () -> customers.stream().collect(Collectors.toMap(Customer::name, Customer::email)));
        // WYNIK: ✔ toMap(klient → e-mail) z null → rzucono NullPointerException: (brak komunikatu)
        // PUŁAPKA: sam HashMap przyjmuje null jako wartość, ale Collectors.toMap — NIE.
        // Komunikat jest pusty, więc w dużych danych długo szukasz winnego elementu.

        // PO (DOBRZE): zamień null na wartość zastępczą (albo odfiltruj takich klientów)
        Map<String, String> emails = customers.stream()
                .collect(Collectors.toMap(Customer::name,
                        c -> c.findEmail().orElse("(brak e-maila)"),  // findEmail = znajdź e-mail; orElse = albo
                        (first, second) -> first,
                        LinkedHashMap::new));                       // LinkedHashMap = mapa w kolejności dodania
        showEach("e-maile klientów", emails);
        // WYNIK: e-maile klientów (liczba kluczy: 7):
        // WYNIK: • Jan Kowalski → jan@example.com
        // WYNIK: • Maria Nowak → (brak e-maila)
        // WYNIK: • Adam Mazur → adam.mazur@example.com
        // WYNIK: • Zofia Krawczyk → zofia@example.com
        // WYNIK: • Ola Pawlak → (brak e-maila)
        // WYNIK: • Marek Król → marek@example.com
        // WYNIK: • Ewa Lis → ewa.lis@example.com
    }

    // =================================================================================================
    // 6. SORTOWANIE: BRAK COMPARABLE I NULL
    // =================================================================================================

    /**
     * 6. {@code sorted()} bez argumentu wymaga elementów Comparable i bez null. Kompilator tego
     * nie sprawdzi — błąd wychodzi dopiero w czasie działania.
     */
    static void sortingTraps() {
        section("6. sorted(): brak Comparable i wartości null");
        List<Customer> customers = SampleData.customers();

        // PRZED (ŹLE): Customer nie implementuje Comparable (porównywalny) — jak go posortować?
        expectThrows("sorted() na Customer", () -> customers.stream().sorted().toList());   // sorted = posortowane
        // WYNIK: ✔ sorted() na Customer → rzucono ClassCastException: class helpers.model.Customer cannot be cast to class java.lang.Comparable (helpers.model.Customer is in unnamed module of loader 'app'; java.lang.Comparable is in module java.base of loader 'bootstrap')
        // (po polsku: „klasy Customer nie da się rzutować na Comparable”)

        // PO (DOBRZE): podaj Comparator (porównywacz) jawnie
        List<String> firstThree = customers.stream()
                .sorted(Comparator.comparing(Customer::name))       // comparing = porównując po
                .map(Customer::name).limit(3).toList();
        show("3 pierwsi alfabetycznie", firstThree);
        // WYNIK: 3 pierwsi alfabetycznie → [Adam Mazur, Ewa Lis, Jan Kowalski]

        // PRZED (ŹLE): null wśród elementów
        List<String> emails = customers.stream().map(Customer::email).toList();   // toList() przepuszcza null
        show("e-maile (z null)", emails);
        // WYNIK: e-maile (z null) → [jan@example.com, null, adam.mazur@example.com, zofia@example.com, null, marek@example.com, ewa.lis@example.com]
        expectThrows("sorted() z null", () -> emails.stream().sorted().toList());
        // WYNIK: ✔ sorted() z null → rzucono NullPointerException: Cannot invoke "java.lang.Comparable.compareTo(Object)" because "c1" is null
        // (Java 14+ „pomocny” komunikat NPE; c1 to parametr porównywacza naturalOrder w JDK)

        // PO (DOBRZE): powiedz, gdzie mają trafić null
        List<String> sortedEmails = emails.stream()
                .sorted(Comparator.nullsLast(Comparator.naturalOrder()))  // nullsLast = null na końcu; naturalOrder = porządek naturalny
                .toList();
        show("nullsLast", sortedEmails);
        // WYNIK: nullsLast → [adam.mazur@example.com, ewa.lis@example.com, jan@example.com, marek@example.com, zofia@example.com, null, null]

        // To samo przy sortowaniu OBIEKTÓW po polu, które bywa null
        expectThrows("comparing(Customer::email)",
                () -> customers.stream().sorted(Comparator.comparing(Customer::email)).toList());
        // WYNIK: ✔ comparing(Customer::email) → rzucono NullPointerException: Cannot invoke "java.lang.Comparable.compareTo(Object)" because the return value of "java.util.function.Function.apply(Object)" is null
        List<String> byEmail = customers.stream()
                .sorted(Comparator.comparing(Customer::email,
                        Comparator.nullsFirst(Comparator.naturalOrder())))   // nullsFirst = null na początku
                .map(Customer::name)
                .toList();
        show("klienci wg e-maila (null na początku)", byEmail);
        // WYNIK: klienci wg e-maila (null na początku) → [Maria Nowak, Ola Pawlak, Adam Mazur, Ewa Lis, Jan Kowalski, Marek Król, Zofia Krawczyk]

        // DOBRA PRAKTYKA: sortując po polu, które MOŻE być null, od razu pisz
        // comparing(pole, nullsFirst(naturalOrder())) albo nullsLast. Polskie napisy dla ludzi —
        // Collator z Locale pl (t16_streams/Streams05SortDistinctLimit).
    }

    // =================================================================================================
    // 7. DISTINCT BEZ EQUALS/HASHCODE
    // =================================================================================================

    /**
     * 7. {@code distinct()} (i HashSet, groupingBy, toMap) korzysta z {@code hashCode} i {@code equals}.
     * Klasa bez nich porównuje „czy to TEN SAM obiekt w pamięci” — duplikaty zostają.
     */
    static void distinctWithoutEquals() {
        section("7. distinct() na klasie bez equals/hashCode");

        // PRZED (ŹLE): LegacyTag nie ma equals/hashCode → każdy obiekt jest „inny”
        long legacyCount = Stream.of(new LegacyTag("java"), new LegacyTag("java"), new LegacyTag("sql"))
                .distinct()                                         // distinct = bez powtórzeń
                .count();
        show("distinct na LegacyTag (klasa)", legacyCount);        // LegacyTag = stary tag (klasa niżej)
        // WYNIK: distinct na LegacyTag (klasa) → 3

        // PO (DOBRZE) 1: record ma equals/hashCode po polach „za darmo”
        long recordCount = Stream.of(new Tag("java"), new Tag("java"), new Tag("sql"))   // Tag = etykieta
                .distinct()
                .count();
        show("distinct na Tag (record)", recordCount);
        // WYNIK: distinct na Tag (record) → 2

        // PO (DOBRZE) 2: nie możesz zmienić klasy? Wyciągnij KLUCZ i na nim rób distinct
        List<String> uniqueNames = Stream.of(new LegacyTag("java"), new LegacyTag("java"), new LegacyTag("sql"))
                .map(LegacyTag::name)
                .distinct()
                .toList();
        show("distinct po kluczu name", uniqueNames);
        // WYNIK: distinct po kluczu name → [java, sql]

        // (Chcesz zachować OBIEKTY unikalne po kluczu? toMap(klucz, t -> t, (a, b) -> a) — ćwiczenie 3.)

        // PUŁAPKA: distinct rozróżnia wielkość liter. „Java” i „java” to dwa różne napisy.
        long caseSensitive = Stream.of("Java", "java", "JAVA").distinct().count();
        long caseInsensitive = Stream.of("Java", "java", "JAVA")
                .map(s -> s.toLowerCase(Locale.ROOT))               // toLowerCase = na małe litery; ROOT = neutralny
                .distinct()
                .count();
        show("distinct: z wielkością liter / bez", caseSensitive + " / " + caseInsensitive);
        // WYNIK: distinct: z wielkością liter / bez → 3 / 1

        // PUŁAPKA: equals BEZ hashCode jest jeszcze gorsze — HashSet najpierw patrzy na hashCode,
        // więc wynik bywa przypadkowy. Zawsze oba albo record (t06_oop_basics/Oop05ObjectMethods).
    }

    /** Klasa „w starym stylu” — BEZ equals i hashCode. */
    static final class LegacyTag {
        private final String name;

        LegacyTag(String name) { this.name = name; }

        String name() { return name; }

        @Override
        public String toString() { return name; }
    }

    /** Record — equals, hashCode i toString generowane automatycznie (Java 16+). */
    record Tag(String name) { }

    // =================================================================================================
    // 8. OPAKOWANIA: ==, KOSZT, PRZEPEŁNIENIE
    // =================================================================================================

    /**
     * 8. Liczby w strumieniach: {@code Stream<Integer>} to obiekty, nie liczby. Porównanie ==,
     * niepotrzebne opakowywanie, przepełnienie int i dzielenie całkowite.
     */
    static void boxingMath() {
        section("8. Opakowania (boxing): ==, koszt i przepełnienie");
        List<Integer> salaries = SampleData.employees().stream().map(Employee::salary).toList();

        Integer piotr = salaries.get(1);                            // 9800
        Integer pawel = salaries.get(9);                            // też 9800
        show("piotr == pawel", piotr == pawel);
        // WYNIK: piotr == pawel → false
        show("piotr.equals(pawel)", piotr.equals(pawel));
        // WYNIK: piotr.equals(pawel) → true
        // PUŁAPKA: == na Integer porównuje REFERENCJE (adresy obiektów). Integer.valueOf ma pamięć
        // podręczną (cache) tylko dla -128..127, więc 9800 to dwa różne obiekty. Stąd błąd
        // w filter(s -> s == szukana), gdy obie strony są typu Integer.

        // Koszt: Stream<Integer> + reduce → rozpakuj i opakuj przy KAŻDYM kroku
        int boxedSum = salaries.stream().reduce(0, Integer::sum);   // reduce = zredukuj; Integer::sum = suma dwóch liczb
        int primitiveSum = SampleData.employees().stream().mapToInt(Employee::salary).sum();
        show("suma: reduce na Integer / mapToInt().sum()", boxedSum + " / " + primitiveSum);
        // WYNIK: suma: reduce na Integer / mapToInt().sum() → 106400 / 106400
        // DOBRA PRAKTYKA: liczysz? Od razu mapToInt/mapToLong/mapToDouble (Streams08PrimitiveStreams).

        // Przepełnienie: IntStream.sum() zwraca int
        int intSum = IntStream.rangeClosed(1, 100_000).sum();       // rangeClosed = zakres domknięty [1, 100000]
        long longSum = LongStream.rangeClosed(1, 100_000).sum();    // LongStream = strumień liczb long
        show("suma 1..100000 jako int", intSum);
        // WYNIK: suma 1..100000 jako int → 705082704
        show("suma 1..100000 jako long", longSum);
        // WYNIK: suma 1..100000 jako long → 5000050000
        // PUŁAPKA: int przepełnia się BEZ wyjątku — dostajesz „jakąś” liczbę. Duże sumy: mapToLong
        // albo asLongStream() (asLongStream = jako strumień long).

        // Dzielenie całkowite zamiast średniej
        List<Integer> numbers = SampleData.numbers();
        int sum = numbers.stream().mapToInt(Integer::intValue).sum();   // intValue = wartość int
        show("sum / size (int / int)", sum / numbers.size());
        // WYNIK: sum / size (int / int) → 5
        show("average()", numbers.stream().mapToInt(Integer::intValue).average().orElse(0));   // average = średnia
        // WYNIK: average() → 5.5
    }

    // =================================================================================================
    // 9. CZYTELNOŚĆ: DŁUGIE POTOKI I WYJĄTKI CHECKED
    // =================================================================================================

    /**
     * 9. Potok to nie konkurs na najdłuższą linię. Kroki z logiką biznesową dostają nazwy,
     * a wyjątki kontrolowane obsługujemy w metodzie pomocniczej — nie w środku lambdy.
     */
    static void readability() {
        section("9. Czytelność: za długi potok i wyjątki checked");
        List<Order> orders = SampleData.orders();

        // PRZED (ŹLE): wszystko w jednym ciągu lambd — trzeba czytać trzy razy
        BigDecimal bad = orders.stream()
                .filter(o -> o.status() != OrderStatus.ANULOWANE && o.customer().vip())
                .flatMap(o -> o.lines().stream())
                .filter(l -> l.product().category() == Category.ELEKTRONIKA)
                .map(l -> l.product().price().multiply(BigDecimal.valueOf(l.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        show("elektronika u VIP-ów (długi potok)", bad);
        // WYNIK: elektronika u VIP-ów (długi potok) → 8797.79

        // PO (DOBRZE): każdy krok ma NAZWĘ — potok czyta się jak zdanie
        BigDecimal good = orders.stream()
                .filter(Streams17SideEffectsPitfalls::isActiveVipOrder)   // isActiveVipOrder = czy aktywne zamówienie VIP
                .flatMap(o -> o.lines().stream())
                .filter(IS_ELECTRONICS)
                .map(OrderLine::total)                              // total = suma pozycji (gotowa metoda modelu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        show("elektronika u VIP-ów (nazwane kroki)", good);
        // WYNIK: elektronika u VIP-ów (nazwane kroki) → 8797.79
        show("równe (compareTo == 0)", bad.compareTo(good) == 0);
        // WYNIK: równe (compareTo == 0) → true
        // DOBRA PRAKTYKA: lambda dłuższa niż 1 linia albo warunek z && i || → metoda z nazwą.
        // Bonus: nazwaną metodę łatwo przetestować osobno.

        // Wyjątki checked: Function.apply nie deklaruje IOException, więc
        //   .map(name -> loadFile(name))   → błąd kompilacji: nieobsłużony wyjątek IOException
        // Rozwiązanie: metoda pomocnicza, która opakowuje go w wyjątek niekontrolowany.
        List<String> contents = Stream.of("a.txt", "b.txt")
                .map(Streams17SideEffectsPitfalls::loadUnchecked)   // loadUnchecked = wczytaj bez checked
                .toList();
        show("wczytane", contents);
        // WYNIK: wczytane → [treść a.txt, treść b.txt]
        expectThrows("wczytanie brakującego pliku",
                () -> Stream.of("a.txt", "brak.txt").map(Streams17SideEffectsPitfalls::loadUnchecked).toList());
        // WYNIK: ✔ wczytanie brakującego pliku → rzucono UncheckedIOException: java.io.IOException: brak pliku: brak.txt
        // Więcej sposobów (i dlaczego nie „połykać” wyjątku w catch): t13_lambdas/Lambda08Pitfalls.
    }

    /** Warunek biznesowy z nazwą: zamówienie nieanulowane i klient VIP. */
    static boolean isActiveVipOrder(Order order) {
        return order.status() != OrderStatus.ANULOWANE && order.customer().vip();
    }

    /** Udaje odczyt pliku (bez prawdziwego dysku) — rzuca wyjątek KONTROLOWANY. */
    static String loadFile(String name) throws IOException {       // loadFile = wczytaj plik
        if (name.startsWith("brak")) throw new IOException("brak pliku: " + name);   // startsWith = zaczyna się od
        return "treść " + name;
    }

    /** Opakowuje IOException w UncheckedIOException — taką metodę można podać do map(). */
    static String loadUnchecked(String name) {
        try {
            return loadFile(name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);                      // UncheckedIOException = niekontrolowany wyjątek IO
        }
    }

    // =================================================================================================
    // 10. KIEDY PĘTLA JEST LEPSZA
    // =================================================================================================

    /**
     * 10. Strumień nie jest celem samym w sobie. Porównanie sąsiadów, stan zmieniany krok po kroku
     * i przerwanie w połowie z kilku powodów — to naturalny teren zwykłej pętli.
     */
    static void whenLoopIsBetter() {
        section("10. Kiedy zwykła pętla jest lepsza");

        // Zadanie: kupuj od najtańszych, dopóki mieścisz się w budżecie 500 zł
        List<Product> cheapestFirst = SampleData.products().stream()
                .sorted(Comparator.comparing(Product::price))
                .toList();
        List<String> basket = new ArrayList<>();
        BigDecimal spent = BigDecimal.ZERO;
        for (Product p : cheapestFirst) {
            BigDecimal next = spent.add(p.price());
            if (next.compareTo(BUDGET) > 0) {
                break;                                              // stop: następny produkt przekroczy budżet
            }
            spent = next;
            basket.add(p.name());
        }
        show("koszyk", basket);
        // WYNIK: koszyk → [Czekolada gorzka, Oliwa z oliwek, T-shirt bawełniany, Kawa ziarnista 1kg, Czysty kod, Wzorce projektowe, Java. Podstawy]
        show("wydano", spent);
        // WYNIK: wydano → 471.47
        // Wersja strumieniowa potrzebowałaby w takeWhile licznika „spoza lambdy” — czyli dokładnie
        // efektu ubocznego z sekcji 1. Pętla jest tu czytelniejsza i poprawna.

        // DOBRA PRAKTYKA: wybierz pętlę, gdy:
        //   • porównujesz sąsiednie elementy (i-1, i+1) — IntStream.range(1, n) to tylko „przebrana pętla”,
        //   • aktualizujesz kilka zmiennych stanu naraz (suma, licznik, koszyk),
        //   • przerywasz w połowie z powodu, który zależy od DOTYCHCZASOWYCH wyników,
        //   • rzucasz/obsługujesz wyjątki checked albo debugujesz krok po kroku.
        // Strumień wybierz, gdy opisujesz przekształcenie: filtruj → mapuj → zbierz.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   Lista kontrolna przed oddaniem kodu ze strumieniem:
     *   [ ] Lambdy w filter/map są bezstanowe i niczego nie zmieniają na zewnątrz?
     *   [ ] Wynik powstaje przez toList()/collect(...), a nie forEach(lista::add)?
     *   [ ] Źródło NIE jest modyfikowane w trakcie? (removeIf albo nowa lista)
     *   [ ] Każdy strumień użyty RAZ? W polach kolekcje, a nie Stream? (Supplier, gdy trzeba)
     *   [ ] peek tylko do debugowania? (count() i anyMatch mogą go pominąć)
     *   [ ] toMap: klucze na pewno unikalne? wartości bez null? (merge, groupingBy, orElse)
     *   [ ] sorted(): typ jest Comparable? pole bywa null? (comparing + nullsFirst/nullsLast)
     *   [ ] distinct/HashSet/groupingBy na własnej klasie: equals I hashCode (albo record)?
     *   [ ] Liczby: mapToInt/mapToLong zamiast Stream<Integer>; long przy dużych sumach;
     *       equals zamiast == dla Integer; average() zamiast sum / size
     *   [ ] Lambda na 2+ linie albo złożony warunek → nazwana metoda / predykat
     *   [ ] Wyjątek checked w lambdzie → metoda pomocnicza (UncheckedIOException)
     *   [ ] Sąsiedzi, kilka zmiennych stanu, break zależny od wyników → zwykła pętla
     *
     * PYTANIA KONTROLNE:
     *   1. Skoro forEach(lista::add) daje ten sam wynik co toList(), to dlaczego jest gorszy?
     *   2. Co wypisze:
     *        Stream<String> s = Stream.of("a", "b");
     *        System.out.println(s.count());
     *        System.out.println(s.count());   ?
     *   3. ZNAJDŹ BŁĄD:
     *        Map<String, String> m = SampleData.customers().stream()
     *                .collect(Collectors.toMap(Customer::city, Customer::name));
     *   4. Co wypisze:
     *        AtomicInteger c = new AtomicInteger();
     *        long n = List.of(1, 2, 3).stream().peek(x -> c.incrementAndGet()).count();
     *        System.out.println(n + " " + c.get());   ?
     *   5. Co wypisze:  Integer a = 127, b = 127, x = 128, y = 128;
     *                   System.out.println((a == b) + " " + (x == y));   ?
     *   6. ZNAJDŹ BŁĄD:
     *        customers.stream().sorted(Comparator.comparing(Customer::email)).toList();
     *   7. Dlaczego pole typu Stream w klasie to zły pomysł i czym je zastąpić?
     *   8. Podaj trzy sytuacje, w których zwykła pętla jest lepsza niż strumień.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: drogie na stanie bez efektu ubocznego",
                List.of("Laptop Pro 14", "Monitor 27 cali", "Ekspres do kawy"),
                () -> exercise1(SampleData.products()));           // exercise = ćwiczenie
        Check.equal("ćw. 2: pensja 9800", "Piotr Kowalski i Paweł Dąbrowski",
                () -> exercise2(SampleData.employees()).get(9800));
        Check.equal("ćw. 2: 3 najniższe pensje (klucze)", List.of(7200, 7600, 8100),
                () -> exercise2(SampleData.employees()).keySet().stream().limit(3).toList());
        Check.equal("ćw. 3: unikalne tagi (bez wielkości liter)", List.of("Java", "SQL", "Docker"),
                () -> exercise3(legacyTags()));                     // legacyTags = stare tagi
        Check.equal("ćw. 4: e-maile wg miast", expectedEmailsByCity(),   // expected emails by city = oczekiwane e-maile wg miast
                () -> exercise4(SampleData.customers()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("Laptop Pro 14", "Monitor 27 cali", "Ekspres do kawy"),
                () -> solution1(SampleData.products()));           // solution = rozwiązanie
        Check.equal("ćw. 2 (wzorzec): pensja 9800", "Piotr Kowalski i Paweł Dąbrowski",
                () -> solution2(SampleData.employees()).get(9800));
        Check.equal("ćw. 2 (wzorzec): 3 najniższe pensje", List.of(7200, 7600, 8100),
                () -> solution2(SampleData.employees()).keySet().stream().limit(3).toList());
        Check.equal("ćw. 3 (wzorzec)", List.of("Java", "SQL", "Docker"), () -> solution3(legacyTags()));
        Check.equal("ćw. 4 (wzorzec)", expectedEmailsByCity(), () -> solution4(SampleData.customers()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /** Dane do ćwiczenia 3: tagi w „starej” klasie bez equals/hashCode. */
    static List<LegacyTag> legacyTags() {
        return List.of(new LegacyTag("Java"), new LegacyTag("java"), new LegacyTag("SQL"),
                new LegacyTag("Docker"), new LegacyTag("sql"), new LegacyTag("JAVA"));
    }

    /** Oczekiwany wynik ćwiczenia 4 (TreeMap — deterministyczna kolejność przy wypisywaniu). */
    static Map<String, List<String>> expectedEmailsByCity() {
        Map<String, List<String>> expected = new TreeMap<>();
        expected.put("Gdańsk", List.of("adam.mazur@example.com"));
        expected.put("Kraków", List.of("marek@example.com"));
        expected.put("Poznań", List.of());
        expected.put("Warszawa", List.of("jan@example.com", "zofia@example.com"));
        expected.put("Wrocław", List.of("ewa.lis@example.com"));
        return expected;
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ bez efektu ubocznego. Kod poniżej dopisuje do zewnętrznej listy
     * nazwy produktów NA STANIE i droższych niż 1000 zł. Zrób to jednym potokiem zakończonym toList().
     * <pre>{@code
     * List<String> result = new ArrayList<>();
     * products.stream()
     *         .filter(p -> p.inStock() && p.price().compareTo(THOUSAND) > 0)
     *         .forEach(p -> result.add(p.name()));
     * return result;
     * }</pre>
     * Podpowiedź: filter → map(Product::name) → toList(); stała THOUSAND jest na górze klasy.
     */
    static List<String> exercise1(List<Product> products) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (średnie): zbuduj mapę pensja → osoby, posortowaną rosnąco po pensji. Gdy dwie osoby
     * mają tę samą pensję, połącz nazwiska tekstem " i " (w kolejności z listy) — BEZ wyjątku
     * {@code IllegalStateException: Duplicate key}.
     * Podpowiedź: {@code Collectors.toMap(klucz, wartość, (a, b) -> a + " i " + b, TreeMap::new)}.
     */
    static Map<Integer, String> exercise2(List<Employee> employees) {
        // TODO: twoje rozwiązanie
        return new TreeMap<>();
    }

    /**
     * ĆWICZENIE 3 (średnie): klasa LegacyTag NIE ma equals/hashCode i nie wolno jej zmieniać.
     * Zwróć nazwy tagów unikalnych BEZ względu na wielkość liter („Java”, „java”, „JAVA” to jeden tag).
     * Zostaw pisownię PIERWSZEGO wystąpienia i kolejność pierwszych wystąpień.
     * Podpowiedź: toMap z kluczem {@code name().toLowerCase(Locale.ROOT)}, funkcją {@code (a, b) -> a}
     * i LinkedHashMap::new; potem {@code new ArrayList<>(mapa.values())}.
     */
    static List<String> exercise3(List<LegacyTag> tags) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): miasto → posortowana lista e-maili klientów z tego miasta.
     * Miasta posortowane alfabetycznie (TreeMap). Klienci bez e-maila (null!) są pomijani, ale ich
     * miasto ZOSTAJE w mapie (Poznań → pusta lista). Bez NullPointerException i bez forEach.
     * Podpowiedź: {@code groupingBy(Customer::city, TreeMap::new, flatMapping(c -> c.findEmail().stream(), ...))}
     * — flatMapping (Java 9+) i Optional.stream (Java 9+) znasz ze Streams13 i Streams14;
     * sortowanie listy w grupie: {@code collectingAndThen(toList(), l -> l.stream().sorted().toList())}.
     */
    static Map<String, List<String>> exercise4(List<Customer> customers) {
        // TODO: twoje rozwiązanie
        return new TreeMap<>();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(List<Product> products) {
        return products.stream()
                .filter(p -> p.inStock() && p.price().compareTo(THOUSAND) > 0)
                .map(Product::name)
                .toList();
    }

    static Map<Integer, String> solution2(List<Employee> employees) {
        return employees.stream()
                .collect(Collectors.toMap(Employee::salary, Employee::name,
                        (a, b) -> a + " i " + b,
                        TreeMap::new));
    }

    static List<String> solution3(List<LegacyTag> tags) {
        Map<String, String> firstByKey = tags.stream()
                .collect(Collectors.toMap(t -> t.name().toLowerCase(Locale.ROOT), LegacyTag::name,
                        (first, second) -> first,
                        LinkedHashMap::new));
        return new ArrayList<>(firstByKey.values());
    }

    static Map<String, List<String>> solution4(List<Customer> customers) {
        return customers.stream()
                .collect(Collectors.groupingBy(Customer::city, TreeMap::new,
                        Collectors.flatMapping(c -> c.findEmail().stream(),
                                Collectors.collectingAndThen(Collectors.toList(),
                                        list -> list.stream().sorted().toList()))));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo wynik powstaje przez efekt uboczny: ArrayList psuje się przy parallel() (giną elementy,
     *      wyjątki), lista jest modyfikowalna i „żyje obok”, a czytelnik nie widzi, gdzie jest wynik.
     *      toList() zwraca gotowy, niemodyfikowalny wynik wprost z potoku.
     *   2. Najpierw 2, potem IllegalStateException: stream has already been operated upon or closed —
     *      strumień jest jednorazowy. Poprawka: dwa razy Stream.of(...) albo Supplier<Stream<String>>.
     *   3. Warszawa i Kraków mają po dwóch klientów → IllegalStateException: Duplicate key Warszawa
     *      (attempted merging values Jan Kowalski and Zofia Krawczyk). Poprawka: groupingBy(Customer::city)
     *      albo toMap z funkcją łączącą, np. (a, b) -> a + ", " + b.
     *   4. "3 0" — źródło ma znany rozmiar, więc count() nie przechodzi elementów i peek się nie wykonuje.
     *   5. "true false" — 127 jest w cache Integer (ten sam obiekt), 128 już nie (dwa obiekty). Używaj equals.
     *   6. Maria Nowak i Ola Pawlak mają email == null → NullPointerException przy porównaniu.
     *      Poprawka: Comparator.comparing(Customer::email, Comparator.nullsFirst(Comparator.naturalOrder())).
     *   7. Strumień da się użyć tylko raz — drugie wywołanie metody rzuci IllegalStateException.
     *      W polu trzymaj kolekcję (List.copyOf), a strumień zwracaj z metody: return products.stream();
     *   8. Porównywanie sąsiadów (i-1, i+1); kilka zmiennych stanu naraz; break zależny od dotychczasowych
     *      wyników (budżet); wyjątki checked; debugowanie krok po kroku.
     */
    // </editor-fold>
}
