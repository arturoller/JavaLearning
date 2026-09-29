package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Customer;
import helpers.model.Employee;
import helpers.model.Order;
import helpers.model.Product;
import helpers.model.Student;

import java.math.BigDecimal;
import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: sorted, distinct, limit, skip, takeWhile, dropWhile — porządek, unikalność i „wycinanie”
 *        (sorted = posortowany; distinct = odrębny, bez powtórzeń; limit = ogranicz; skip = pomiń;
 *         takeWhile = bierz dopóki; dropWhile = odrzucaj dopóki)
 *
 * W SKRÓCIE:
 *   Te operacje pośrednie nie zmieniają elementów — zmieniają ich KOLEJNOŚĆ albo LICZBĘ.
 *   sorted układa, distinct usuwa duplikaty (wg equals), limit/skip wycinają fragment (np. stronę wyników),
 *   takeWhile/dropWhile tną strumień w miejscu, gdzie warunek pierwszy raz przestaje być spełniony.
 *
 * ANALOGIA: Kolejka w sklepie. sorted = ustaw ludzi od najniższego; distinct = wyproś osoby, które stoją
 *   drugi raz; limit(3) = „obsłużę tylko trzy pierwsze osoby”; skip(3) = „trzy pierwsze proszę do innej kasy”;
 *   takeWhile(dorosły) = obsługuj od początku, aż trafisz na dziecko — wtedy zamykasz kasę, nawet jeśli
 *   dalej stoją dorośli.
 *
 * JAK TO DZIAŁA:
 *   operacja          │ bezstanowa?   │ uwagi
 *   ──────────────────┼───────────────┼──────────────────────────────────────────────────────────
 *   sorted()          │ NIE           │ musi zobaczyć WSZYSTKIE elementy, zanim odda pierwszy
 *   sorted(cmp)       │ NIE           │ jw.; sortowanie stabilne (równe zostają w starej kolejności)
 *   distinct()        │ NIE           │ pamięta widziane elementy (equals + hashCode); zostaje PIERWSZY
 *   limit(n)          │ NIE (licznik) │ krótkie spięcie: po n elementach przestaje pobierać dalej
 *   skip(n)           │ NIE (licznik) │ wyrzuca n pierwszych
 *   takeWhile(p)      │ tak           │ (Java 9+) bierze od początku, STOP przy pierwszym „nie”
 *   dropWhile(p)      │ tak           │ (Java 9+) wyrzuca od początku, od pierwszego „nie” bierze wszystko
 *
 * SŁÓWKA:
 *   comparator = komparator (obiekt porównujący); comparing = porównując (po kluczu); then = potem;
 *   reversed = odwrócony; natural order = porządek naturalny; nulls first/last = null na początku/końcu;
 *   collator = porównywacz tekstów wg reguł języka; key = klucz; seen = widziane; page = strona;
 *   top-N = N najlepszych
 *
 * ZOBACZ TEŻ: t12_collections/Collections07ComparableComparator (Comparable i Comparator od podstaw),
 *   t16_streams/Streams04FlatMap (distinct po spłaszczeniu), t16_streams/Streams06TerminalOps (min/max z komparatorem),
 *   t16_streams/Streams10CollectorsToMap (toMap z funkcją łączącą), t16_streams/Streams17SideEffectsPitfalls
 * </pre>
 */
public class Streams05SortDistinctLimit {

    /** Polskie ustawienia regionalne — dla Collatora (porównywacza tekstów wg reguł języka polskiego). */
    private static final Locale PL = Locale.forLanguageTag("pl-PL");

    /** Stała data „granica” — trzymana poza lambdą, żeby nie tworzyć jej przy każdym elemencie. */
    private static final LocalDate MARCH_1 = LocalDate.of(2026, 3, 1);

    public static void main(String[] args) {
        title("Streams05 — sorted, distinct, limit, skip, takeWhile, dropWhile");

        naturalOrder();          // natural order = porządek naturalny (+ Unicode vs Collator)
        sortWithComparator();    // sort with comparator = sortowanie komparatorem
        thenComparingChain();    // then comparing chain = łańcuch „potem porównaj”
        nullsInSorting();        // nulls in sorting = null w sortowaniu
        sortByBigDecimal();      // sort by BigDecimal = sortowanie po cenie BigDecimal
        distinctBasics();        // distinct basics = podstawy distinct
        distinctByKey();         // distinct by key = unikalne wg klucza
        limitSkipPaging();       // limit skip paging = limit, skip i stronicowanie
        takeWhileDropWhile();    // takeWhile/dropWhile (Java 9+) = bierz/odrzucaj dopóki
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. sorted() — PORZĄDEK NATURALNY; UNICODE vs COLLATOR
    // =================================================================================================

    /**
     * 1. {@code sorted()} bez argumentu używa porządku naturalnego ({@code Comparable}): liczby rosnąco,
     * teksty wg kodów Unicode. Dla polskich słów to ZŁY porządek — potrzebny {@code Collator}.
     */
    static void naturalOrder() {
        section("1. sorted() — porządek naturalny; Unicode vs Collator pl-PL");

        show("liczby", SampleData.numbers().stream().sorted().toList()); // toList (Java 16+) = do listy
        // WYNIK: liczby → [1, 2, 3, 3, 4, 5, 6, 7, 8, 8, 9, 10]
        show("słowa", SampleData.words().stream().sorted().toList());
        // WYNIK: słowa → [enum, java, java, java, kolekcja, lambda, lista, mapa, optional, rekord, stream, stream]

        List<String> cities = List.of("Łódź", "Zakopane", "Kraków", "Śrem", "Sopot", "Lublin", "elbląg", "Ełk", "Żory", "Opole");  // List.of (Java 9+)

        // PUŁAPKA: String porównuje KODY znaków. Wielkie A–Z (65–90) < małe a–z (97–122) < polskie Ł, Ś, Ż (ponad 300).
        show("sorted() — Unicode", cities.stream().sorted().toList());
        // WYNIK: sorted() — Unicode → [Ełk, Kraków, Lublin, Opole, Sopot, Zakopane, elbląg, Łódź, Śrem, Żory]
        show("kod 'Z', 'e', 'Ł'", List.of((int) 'Z', (int) 'e', (int) 'Ł'));
        // WYNIK: kod 'Z', 'e', 'Ł' → [90, 101, 321]

        // CASE_INSENSITIVE_ORDER (bez rozróżniania wielkości liter) naprawia „elbląg”, ale NIE polskie litery.
        show("CASE_INSENSITIVE_ORDER", cities.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList());
        // WYNIK: CASE_INSENSITIVE_ORDER → [elbląg, Ełk, Kraków, Lublin, Opole, Sopot, Zakopane, Łódź, Śrem, Żory]

        // DOBRA PRAKTYKA: tekst dla człowieka sortuj Collatorem dla właściwego języka.
        Collator polish = Collator.getInstance(PL);   // getInstance = pobierz egzemplarz (dla danego języka)
        show("Collator pl-PL", cities.stream().sorted(polish).toList());
        // WYNIK: Collator pl-PL → [elbląg, Ełk, Kraków, Lublin, Łódź, Opole, Sopot, Śrem, Zakopane, Żory]

        // sorted() na obiektach, które NIE są Comparable → ClassCastException (dopiero w czasie działania!).
        expectThrows("sorted() na Object bez Comparable", () -> Stream.of(new Object(), new Object()).sorted().toList());
        // WYNIK: ✔ sorted() na Object bez Comparable → rzucono ClassCastException: class java.lang.Object cannot be cast to class java.lang.Comparable (java.lang.Object and java.lang.Comparable are in module java.base of loader 'bootstrap')
    }

    // =================================================================================================
    // 2. sorted(Comparator) — comparing, comparingInt, reversed
    // =================================================================================================

    /**
     * 2. {@code sorted(Comparator)} — sami mówimy, jak porównywać. Komparator budujemy fabrykami:
     * {@code Comparator.comparing(klucz)}, {@code comparingInt(klucz)}, a kierunek odwracamy {@code reversed()}.
     */
    static void sortWithComparator() {
        section("2. sorted(Comparator) — comparing, comparingInt, reversed");

        List<Employee> employees = SampleData.employees();

        List<String> byName = employees.stream()
                .sorted(Comparator.comparing(Employee::name))  // comparing = porównuj po kluczu (tu: imię i nazwisko)
                .map(Employee::name)
                .limit(3)
                .toList();
        show("po nazwie — pierwsze 3", byName);
        // WYNIK: po nazwie — pierwsze 3 → [Agnieszka Zielińska, Anna Nowak, Ewa Woźniak]

        // comparingInt — dla kluczy int: bez pakowania do Integer (szybciej, mniej obiektów).
        List<String> bySalary = employees.stream()
                .sorted(Comparator.comparingInt(Employee::salary))
                .map(e -> e.name() + " " + e.salary())
                .toList();
        showEach("po pensji rosnąco", bySalary);
        // WYNIK: po pensji rosnąco (liczba elementów: 10):
        // WYNIK: • Katarzyna Wiśniewska 7200
        // WYNIK: • Agnieszka Zielińska 7600
        // WYNIK: • Magdalena Kamińska 8100
        // WYNIK: • Tomasz Wójcik 8900
        // WYNIK: • Piotr Kowalski 9800
        // WYNIK: • Paweł Dąbrowski 9800
        // WYNIK: • Krzysztof Szymański 11200
        // WYNIK: • Ewa Woźniak 12100
        // WYNIK: • Anna Nowak 14500
        // WYNIK: • Michał Lewandowski 17200
        // Sortowanie jest STABILNE: Piotr i Paweł mają po 9800 i zostają w kolejności z listy (Piotr był wcześniej).

        List<String> bySalaryDesc = employees.stream()
                .sorted(Comparator.comparingInt(Employee::salary).reversed()) // reversed = odwrócony (malejąco)
                .map(Employee::name)
                .limit(3)
                .toList();
        show("po pensji malejąco — 3", bySalaryDesc);
        // WYNIK: po pensji malejąco — 3 → [Michał Lewandowski, Anna Nowak, Ewa Woźniak]

        // Dla porządku naturalnego: Comparator.reverseOrder() = odwrócony porządek naturalny.
        show("liczby malejąco", SampleData.numbers().stream().sorted(Comparator.reverseOrder()).toList());
        // WYNIK: liczby malejąco → [10, 9, 8, 8, 7, 6, 5, 4, 3, 3, 2, 1]

        // PUŁAPKA: komparator przez odejmowanie (a, b) -> a - b może się przepełnić dla dużych liczb.
        // Integer.MIN_VALUE - 1 „zawija się” na wielką liczbę dodatnią → zły znak wyniku.
        Comparator<Integer> bySubtraction = (a, b) -> a - b;
        show("odejmowanie: MIN_VALUE vs 1", bySubtraction.compare(Integer.MIN_VALUE, 1));
        // WYNIK: odejmowanie: MIN_VALUE vs 1 → 2147483647    ← dodatni = „MIN_VALUE jest większe”?!
        show("Integer.compare: MIN_VALUE vs 1", Integer.compare(Integer.MIN_VALUE, 1));
        // WYNIK: Integer.compare: MIN_VALUE vs 1 → -1
        // DOBRA PRAKTYKA: używaj comparingInt / Integer.compare, nigdy odejmowania.
    }

    // =================================================================================================
    // 3. thenComparing — KILKA KRYTERIÓW; PUŁAPKA reversed() NA CAŁYM ŁAŃCUCHU
    // =================================================================================================

    /**
     * 3. {@code thenComparing} = „przy remisie porównaj jeszcze po...”. {@code reversed()} odwraca
     * CAŁY komparator zbudowany do tego miejsca — to najczęstszy błąd przy sortowaniu wielopoziomowym.
     */
    static void thenComparingChain() {
        section("3. thenComparing i pułapka reversed() na całym łańcuchu");

        List<Employee> employees = SampleData.employees();

        // CEL: działy wg kolejności w enumie (IT, HR, SPRZEDAZ, ...), w dziale pensje od NAJWYŻSZEJ.
        Comparator<Employee> good = Comparator.comparing(Employee::department)          // department = dział
                .thenComparing(Comparator.comparingInt(Employee::salary).reversed()); // reversed TYLKO dla pensji
        showEach("dobrze: dział ↑, pensja ↓", employees.stream().sorted(good).toList());
        // WYNIK: dobrze: dział ↑, pensja ↓ (liczba elementów: 10):
        // WYNIK: • Michał Lewandowski (IT, 17200 zł)
        // WYNIK: • Anna Nowak (IT, 14500 zł)
        // WYNIK: • Ewa Woźniak (IT, 12100 zł)
        // WYNIK: • Piotr Kowalski (IT, 9800 zł)
        // WYNIK: • Katarzyna Wiśniewska (HR, 7200 zł)
        // WYNIK: • Krzysztof Szymański (SPRZEDAZ, 11200 zł)
        // WYNIK: • Tomasz Wójcik (SPRZEDAZ, 8900 zł)
        // WYNIK: • Magdalena Kamińska (KSIEGOWOSC, 8100 zł)
        // WYNIK: • Paweł Dąbrowski (MARKETING, 9800 zł)
        // WYNIK: • Agnieszka Zielińska (MARKETING, 7600 zł)

        // PUŁAPKA: reversed() na końcu odwraca WSZYSTKO — także kolejność działów. IT ląduje na końcu.
        Comparator<Employee> bad = Comparator.comparing(Employee::department)
                .thenComparingInt(Employee::salary)
                .reversed();                                        // ← odwraca dział I pensję
        show("źle — pierwsze 3", employees.stream().sorted(bad).limit(3).toList());
        // WYNIK: źle — pierwsze 3 → [Paweł Dąbrowski (MARKETING, 9800 zł), Agnieszka Zielińska (MARKETING, 7600 zł), Magdalena Kamińska (KSIEGOWOSC, 8100 zł)]

        // DOBRA PRAKTYKA: dodaj ostatnie kryterium „rozstrzygające remis” (np. nazwisko) — wynik jest wtedy
        // jednoznaczny i nie zależy od kolejności na liście wejściowej.
        List<String> salaryThenName = employees.stream()
                .sorted(Comparator.comparingInt(Employee::salary).reversed()
                        .thenComparing(Employee::name))                // przy równej pensji: po nazwie rosnąco
                .map(e -> e.name() + " " + e.salary())
                .skip(4)                                               // skip = pomiń (tu: 4 najlepiej zarabiających)
                .limit(2)
                .toList();
        show("pensja ↓, potem nazwa ↑ (miejsca 5–6)", salaryThenName);
        // WYNIK: pensja ↓, potem nazwa ↑ (miejsca 5–6) → [Paweł Dąbrowski 9800, Piotr Kowalski 9800]
    }

    // =================================================================================================
    // 4. null W SORTOWANIU — nullsFirst / nullsLast
    // =================================================================================================

    /**
     * 4. Komparatory z {@code comparing} nie znoszą null w kluczu → NPE. {@code Comparator.nullsFirst/nullsLast}
     * opakowuje komparator i mówi, gdzie ustawić null.
     */
    static void nullsInSorting() {
        section("4. null w sortowaniu — nullsFirst / nullsLast");

        List<Customer> customers = SampleData.customers();

        // PUŁAPKA: Maria Nowak i Ola Pawlak nie mają e-maila (null) → compareTo na null → NPE.
        expectThrows("comparing(Customer::email) z null", () -> customers.stream()
                .sorted(Comparator.comparing(Customer::email))
                .toList());
        // WYNIK: ✔ comparing(Customer::email) z null → rzucono NullPointerException: Cannot invoke "java.lang.Comparable.compareTo(Object)" because the return value of "java.util.function.Function.apply(Object)" is null

        // Drugi argument comparing = komparator dla KLUCZA. nullsLast(naturalOrder()) = null na koniec, reszta naturalnie.
        List<String> byEmailNullsLast = customers.stream()
                .sorted(Comparator.comparing(Customer::email, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(Customer::name)
                .toList();
        show("po e-mailu, null na końcu", byEmailNullsLast);
        // WYNIK: po e-mailu, null na końcu → [Adam Mazur, Ewa Lis, Jan Kowalski, Marek Król, Zofia Krawczyk, Maria Nowak, Ola Pawlak]

        List<String> byEmailNullsFirst = customers.stream()
                .sorted(Comparator.comparing(Customer::email, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(Customer::name)
                .limit(3)
                .toList();
        show("po e-mailu, null na początku — 3", byEmailNullsFirst);
        // WYNIK: po e-mailu, null na początku — 3 → [Maria Nowak, Ola Pawlak, Adam Mazur]

        // Gdy null są SAME elementy (a nie klucze): sorted() → NPE; nullsFirst(naturalOrder()) → OK.
        List<String> withNull = new ArrayList<>(List.of("b", "a"));
        withNull.add(1, null);                                   // lista: [b, null, a]
        expectThrows("sorted() z elementem null", () -> withNull.stream().sorted().toList());
        // WYNIK: ✔ sorted() z elementem null → rzucono NullPointerException: Cannot invoke "java.lang.Comparable.compareTo(Object)" because "c1" is null
        // („c1” to nazwa parametru w komparatorze z JDK — to pierwszy z dwóch porównywanych elementów, tu null.)
        show("nullsFirst(naturalOrder())", withNull.stream().sorted(Comparator.nullsFirst(Comparator.naturalOrder())).toList());
        // WYNIK: nullsFirst(naturalOrder()) → [null, a, b]
    }

    // =================================================================================================
    // 5. SORTOWANIE PO CENIE (BigDecimal)
    // =================================================================================================

    /**
     * 5. {@code BigDecimal} jest {@code Comparable} (porównuje wartość przez compareTo), więc
     * {@code Comparator.comparing(Product::price)} działa od razu. Nie sortuj po {@code doubleValue()}.
     */
    static void sortByBigDecimal() {
        section("5. Sortowanie po cenie (BigDecimal)");

        List<Product> products = SampleData.products();

        show("3 najtańsze", products.stream()
                .sorted(Comparator.comparing(Product::price))            // price = cena (BigDecimal)
                .limit(3)
                .toList());
        // WYNIK: 3 najtańsze → [Czekolada gorzka (7.49 zł), Oliwa z oliwek (42.00 zł), T-shirt bawełniany (49.99 zł)]

        show("3 najdroższe dostępne", products.stream()
                .filter(Product::inStock)                                // inStock = jest na stanie
                .sorted(Comparator.comparing(Product::price).reversed())
                .limit(3)
                .toList());
        // WYNIK: 3 najdroższe dostępne → [Laptop Pro 14 (5499.99 zł), Ekspres do kawy (1899.00 zł), Monitor 27 cali (1299.00 zł)]

        // Dwa produkty kosztują 129.00 — remis rozstrzygamy nazwą (Collatorem, bo to tekst dla człowieka).
        Collator polish = Collator.getInstance(PL);
        show("cena ↓, potem nazwa (miejsca 5–7)", products.stream()
                .sorted(Comparator.comparing(Product::price).reversed()
                        .thenComparing(Product::name, polish))           // thenComparing(klucz, komparator klucza)
                .skip(4)
                .limit(3)
                .toList());
        // WYNIK: cena ↓, potem nazwa (miejsca 5–7) → [Kurtka zimowa (459.00 zł), Słuchawki BT (349.90 zł), Java. Podstawy (129.00 zł)]

        // PUŁAPKA: BigDecimal.equals widzi różnicę skali (129.0 ≠ 129.00), ale compareTo NIE (oba = 0).
        // Komparator używa compareTo — więc w sortowaniu 129.0 i 129.00 to remis. I dobrze.
        show("new BigDecimal(\"129.0\").compareTo(129.00)", new BigDecimal("129.0").compareTo(new BigDecimal("129.00")));
        // WYNIK: new BigDecimal("129.0").compareTo(129.00) → 0
    }

    // =================================================================================================
    // 6. distinct() — equals, ZOSTAJE PIERWSZY
    // =================================================================================================

    /** Rekord (Java 16+): equals i hashCode porównują składniki — distinct działa. */
    record PointRecord(int x, int y) { }

    /** Zwykła klasa BEZ equals/hashCode — dwa obiekty o tych samych polach są dla Javy RÓŻNE. */
    static final class PointClass {
        private final int x;
        private final int y;

        PointClass(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    /**
     * 6. {@code distinct()} usuwa duplikaty wg {@code equals}/{@code hashCode}. Z powtórzeń zostaje PIERWSZE
     * wystąpienie, a kolejność pozostałych się nie zmienia.
     */
    static void distinctBasics() {
        section("6. distinct() — wg equals, zostaje pierwsze wystąpienie");

        show("liczby", SampleData.numbers());
        // WYNIK: liczby → [5, 3, 8, 1, 9, 2, 7, 3, 10, 6, 4, 8]
        show("distinct()", SampleData.numbers().stream().distinct().toList());
        // WYNIK: distinct() → [5, 3, 8, 1, 9, 2, 7, 10, 6, 4]
        show("słowa distinct()", SampleData.words().stream().distinct().toList());
        // WYNIK: słowa distinct() → [java, stream, lambda, kolekcja, mapa, lista, optional, rekord, enum]

        long records = Stream.of(new PointRecord(1, 2), new PointRecord(1, 2), new PointRecord(3, 4)).distinct().count();
        show("rekordy: ile unikalnych", records);
        // WYNIK: rekordy: ile unikalnych → 2

        // PUŁAPKA: klasa bez equals/hashCode → distinct porównuje ADRESY obiektów → nic nie usuwa.
        List<PointClass> classes = Stream.of(new PointClass(1, 2), new PointClass(1, 2), new PointClass(3, 4))
                .distinct()
                .toList();
        show("klasa bez equals: po distinct()", classes);
        // WYNIK: klasa bez equals: po distinct() → [(1, 2), (1, 2), (3, 4)]

        // DOBRA PRAKTYKA: najpierw distinct, potem sorted — mniej elementów do sortowania (wynik ten sam).
        show("distinct().sorted()", SampleData.numbers().stream().distinct().sorted().toList());
        // WYNIK: distinct().sorted() → [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
    }

    // =================================================================================================
    // 7. UNIKALNE WG KLUCZA — toMap / TreeSet / filter(seen::add)
    // =================================================================================================

    /**
     * 7. Stream nie ma {@code distinctBy(klucz)}. Są trzy sposoby na „jeden element na klucz”
     * (np. pierwszy klient z każdego miasta): toMap, TreeSet z komparatorem, filter z pamięcią „widzianych”.
     */
    static void distinctByKey() {
        section("7. Unikalne wg klucza — pierwszy klient z każdego miasta");

        List<Customer> customers = SampleData.customers();

        // Sposób 1: toMap(klucz, wartość, (pierwszy, drugi) -> pierwszy, LinkedHashMap::new) — kolejność wystąpienia.
        List<Customer> viaToMap = new ArrayList<>(customers.stream()
                .collect(Collectors.toMap(Customer::city, c -> c, (first, second) -> first, LinkedHashMap::new))
                .values());                                          // values = wartości mapy
        showEach("toMap + LinkedHashMap", viaToMap);
        // WYNIK: toMap + LinkedHashMap (liczba elementów: 5):
        // WYNIK: • Jan Kowalski (Warszawa, VIP)
        // WYNIK: • Maria Nowak (Kraków)
        // WYNIK: • Adam Mazur (Gdańsk)
        // WYNIK: • Ola Pawlak (Poznań)
        // WYNIK: • Ewa Lis (Wrocław)

        // Sposób 2: TreeSet z komparatorem po kluczu — „równy” (ten sam klucz) nie wejdzie drugi raz.
        // Wynik jest POSORTOWANY wg klucza (miasta), a nie w kolejności wystąpienia.
        TreeSet<Customer> viaTreeSet = customers.stream()
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Customer::city))));
        show("TreeSet po mieście", viaTreeSet);
        // WYNIK: TreeSet po mieście → [Adam Mazur (Gdańsk), Maria Nowak (Kraków), Ola Pawlak (Poznań), Jan Kowalski (Warszawa, VIP), Ewa Lis (Wrocław)]

        // Sposób 3: filter(seen::add) — Set.add zwraca true tylko za PIERWSZYM razem dla danego klucza.
        Set<String> seen = new HashSet<>();                          // seen = widziane (miasta)
        List<String> viaSeen = customers.stream()
                .filter(c -> seen.add(c.city()))
                .map(Customer::name)
                .toList();
        show("filter(seen.add(miasto))", viaSeen);
        // WYNIK: filter(seen.add(miasto)) → [Jan Kowalski, Maria Nowak, Adam Mazur, Ola Pawlak, Ewa Lis]

        // PUŁAPKA: to lambda ze STANEM (efekt uboczny: zmienia zbiór seen). Uruchom drugi raz z tym samym seen...
        List<String> secondRun = customers.stream().filter(c -> seen.add(c.city())).map(Customer::name).toList();
        show("drugie uruchomienie z tym samym seen", secondRun);
        // WYNIK: drugie uruchomienie z tym samym seen → []    ← wszystkie miasta już „widziane”
        // Do tego HashSet nie jest bezpieczny wątkowo → w parallel() wynik byłby losowy.
        // DOBRA PRAKTYKA: domyślnie toMap albo TreeSet; filter(seen::add) tylko lokalnie, świeży Set, bez parallel.
    }

    // =================================================================================================
    // 8. limit, skip, STRONICOWANIE, TOP-N
    // =================================================================================================

    /**
     * 8. {@code limit(n)} bierze n pierwszych, {@code skip(n)} pomija n pierwszych. Razem dają stronę wyników:
     * {@code skip((numer - 1) * rozmiar).limit(rozmiar)}. Top-N = sorted + limit (w TEJ kolejności).
     */
    static void limitSkipPaging() {
        section("8. limit, skip, stronicowanie, top-N");

        List<Integer> numbers = SampleData.numbers();
        show("limit(3)", numbers.stream().limit(3).toList());
        // WYNIK: limit(3) → [5, 3, 8]
        show("skip(10)", numbers.stream().skip(10).toList());
        // WYNIK: skip(10) → [4, 8]
        show("skip(100) — za daleko", numbers.stream().skip(100).toList());
        // WYNIK: skip(100) — za daleko → []
        expectThrows("limit(-1)", () -> numbers.stream().limit(-1).toList());
        // WYNIK: ✔ limit(-1) → rzucono IllegalArgumentException: -1

        // Stronicowanie: 14 produktów, po 5 na stronę → strony 1, 2, 3 (ostatnia niepełna).
        List<String> names = SampleData.products().stream().map(Product::name).toList();
        show("strona 1", page(names, 1, 5));
        // WYNIK: strona 1 → [Laptop Pro 14, Smartfon X, Słuchawki BT, Monitor 27 cali, Kawa ziarnista 1kg]
        show("strona 3", page(names, 3, 5));
        // WYNIK: strona 3 → [Kurtka zimowa, T-shirt bawełniany, Ekspres do kawy, Lampka biurkowa]
        show("strona 4", page(names, 4, 5));
        // WYNIK: strona 4 → []
        show("liczba stron", (names.size() + 5 - 1) / 5);        // dzielenie „w górę” dla liczb całkowitych
        // WYNIK: liczba stron → 3
        expectThrows("strona 0", () -> page(names, 0, 5));
        // WYNIK: ✔ strona 0 → rzucono IllegalArgumentException: numer strony liczymy od 1, a jest: 0

        // Top-N: najpierw sorted, potem limit.
        List<Employee> employees = SampleData.employees();
        show("top 3 pensje", employees.stream()
                .sorted(Comparator.comparingInt(Employee::salary).reversed())
                .limit(3)
                .toList());
        // WYNIK: top 3 pensje → [Michał Lewandowski (IT, 17200 zł), Anna Nowak (IT, 14500 zł), Ewa Woźniak (IT, 12100 zł)]

        // PUŁAPKA: limit PRZED sorted = „weź 3 pierwsze z listy i je posortuj” — to nie są 3 najlepsze!
        show("ŹLE: limit przed sorted", employees.stream()
                .limit(3)
                .sorted(Comparator.comparingInt(Employee::salary).reversed())
                .toList());
        // WYNIK: ŹLE: limit przed sorted → [Anna Nowak (IT, 14500 zł), Piotr Kowalski (IT, 9800 zł), Katarzyna Wiśniewska (HR, 7200 zł)]

        // Kolejność skip/limit też ma znaczenie:
        show("skip(2).limit(3)", numbers.stream().skip(2).limit(3).toList());
        // WYNIK: skip(2).limit(3) → [8, 1, 9]
        show("limit(3).skip(2)", numbers.stream().limit(3).skip(2).toList());
        // WYNIK: limit(3).skip(2) → [8]
    }

    /**
     * Zwraca stronę {@code number} (liczoną od 1) o rozmiarze {@code size}. Pusta lista, gdy strona jest za daleko.
     * Rzutowanie na long chroni przed przepełnieniem int przy mnożeniu dużych liczb.
     */
    static <T> List<T> page(List<T> list, int number, int size) {
        if (number < 1) {
            throw new IllegalArgumentException("numer strony liczymy od 1, a jest: " + number);
        }
        if (size < 1) {
            throw new IllegalArgumentException("rozmiar strony musi być dodatni, a jest: " + size);
        }
        return list.stream()
                .skip((long) (number - 1) * size)
                .limit(size)
                .toList();
    }

    // =================================================================================================
    // 9. takeWhile / dropWhile (Java 9+) vs filter
    // =================================================================================================

    /**
     * 9. {@code takeWhile} (Java 9+) bierze elementy od początku, dopóki warunek jest spełniony — przy PIERWSZYM
     * „nie” kończy. {@code dropWhile} (Java 9+) odwrotnie. Ma to sens tylko na danych UPORZĄDKOWANYCH.
     */
    static void takeWhileDropWhile() {
        section("9. takeWhile / dropWhile (Java 9+) vs filter");

        List<Integer> numbers = SampleData.numbers();   // [5, 3, 8, 1, 9, 2, 7, 3, 10, 6, 4, 8]

        // PUŁAPKA: na nieposortowanych danych takeWhile to NIE jest filter — zatrzymuje się na 8.
        show("takeWhile(n < 8)", numbers.stream().takeWhile(n -> n < 8).toList());
        // WYNIK: takeWhile(n < 8) → [5, 3]
        show("filter(n < 8)", numbers.stream().filter(n -> n < 8).toList());
        // WYNIK: filter(n < 8) → [5, 3, 1, 2, 7, 3, 6, 4]
        show("dropWhile(n < 8)", numbers.stream().dropWhile(n -> n < 8).toList());
        // WYNIK: dropWhile(n < 8) → [8, 1, 9, 2, 7, 3, 10, 6, 4, 8]

        // Na posortowanych danych takeWhile daje to samo co filter, ale kończy wcześniej (nie sprawdza reszty).
        show("sorted().takeWhile(n < 8)", numbers.stream().sorted().takeWhile(n -> n < 8).toList());
        // WYNIK: sorted().takeWhile(n < 8) → [1, 2, 3, 3, 4, 5, 6, 7]

        // Przykład z domeny: zamówienia są ułożone po dacie → „wszystko przed 1 marca” i „od 1 marca”.
        List<Order> orders = SampleData.orders();
        show("przed 1 marca", orders.stream()
                .takeWhile(order -> order.date().isBefore(MARCH_1))   // isBefore = czy przed
                .map(Order::id)
                .toList());
        // WYNIK: przed 1 marca → [ZAM-001, ZAM-002, ZAM-003, ZAM-004, ZAM-005]
        show("od 1 marca", orders.stream()
                .dropWhile(order -> order.date().isBefore(MARCH_1))
                .map(Order::id)
                .toList());
        // WYNIK: od 1 marca → [ZAM-006, ZAM-007, ZAM-008, ZAM-009, ZAM-010]

        // takeWhile zatrzymuje NIESKOŃCZONY strumień. filter by tego nie zrobił (liczyłby w nieskończoność).
        show("potęgi 2 mniejsze od 100", Stream.iterate(1, n -> n * 2).takeWhile(n -> n < 100).toList());
        // WYNIK: potęgi 2 mniejsze od 100 → [1, 2, 4, 8, 16, 32, 64]

        // Produkty tańsze niż 100 zł — po posortowaniu po cenie takeWhile jest poprawny i szybki.
        BigDecimal hundred = new BigDecimal("100");
        show("tanie (po sortowaniu)", SampleData.products().stream()
                .sorted(Comparator.comparing(Product::price))
                .takeWhile(p -> p.price().compareTo(hundred) < 0)
                .map(Product::name)
                .toList());
        // WYNIK: tanie (po sortowaniu) → [Czekolada gorzka, Oliwa z oliwek, T-shirt bawełniany, Kawa ziarnista 1kg, Czysty kod, Wzorce projektowe]

        // DOBRA PRAKTYKA: takeWhile/dropWhile tylko gdy dane są uporządkowane wg warunku (posortowane, po dacie,
        // „do pierwszej pustej linii”). W każdym innym przypadku użyj filter.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • sorted()                          → porządek naturalny (Comparable); String = kody Unicode!
     *   • sorted(Collator.getInstance(PL))  → polski porządek alfabetyczny (Ł po L, Ś po S, bez względu na wielkość)
     *   • Comparator.comparing(k) / comparingInt(k) / comparingDouble(k) → porównaj po kluczu
     *   • .reversed() → odwraca CAŁY łańcuch zbudowany do tego miejsca; odwróć tylko jedno kryterium:
     *       comparing(A).thenComparing(Comparator.comparingInt(B).reversed())
     *   • .thenComparing(k) / thenComparing(k, cmp) → kryterium przy remisie; dodaj „rozstrzygające” na końcu
     *   • null w kluczu → comparing(k, Comparator.nullsLast(Comparator.naturalOrder()))
     *   • BigDecimal → comparing(Product::price) (compareTo; 129.0 i 129.00 to remis)
     *   • distinct() → equals/hashCode, zostaje PIERWSZE; klasa bez equals → nic nie usunie
     *   • unikalne wg klucza → toMap(k, v, (a, b) -> a, LinkedHashMap::new) | TreeSet(comparing(k)) | filter(seen::add)
     *   • top-N → sorted(...).limit(N)  (NIE limit przed sorted!)
     *   • strona → skip((long) (nr - 1) * rozmiar).limit(rozmiar)
     *   • takeWhile/dropWhile (Java 9+) → tnie przy PIERWSZYM „nie”; sens tylko na danych uporządkowanych
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego sorted() stawia „Zakopane” przed „elbląg”, a „Łódź” na samym końcu? Jak to naprawić?
     *   2. Co wypisze:  System.out.println(Stream.of(5, 1, 4, 1, 5).distinct().sorted().toList());  ?
     *   3. Co wypisze:  System.out.println(Stream.of(1, 5, 2, 6, 3).takeWhile(n -> n < 5).toList());  ?
     *   4. ZNAJDŹ BŁĄD (chcemy 3 najlepiej zarabiających):
     *        employees.stream().limit(3).sorted(comparingInt(Employee::salary).reversed()).toList();
     *   5. ZNAJDŹ BŁĄD (chcemy: działy rosnąco, w dziale pensje malejąco):
     *        comparing(Employee::department).thenComparingInt(Employee::salary).reversed()
     *   6. Co się stanie przy sorted(comparing(Customer::email)), gdy któryś e-mail to null? Jak to naprawić?
     *   7. Jakie są wady „distinct wg klucza” przez filter(seen::add)?
     *   8. Czy distinct() usunie duplikaty obiektów zwykłej klasy bez equals i hashCode? Dlaczego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        List<Product> products = SampleData.products();
        Check.equal("ćw. 1: 3 najtańsze dostępne", List.of("Czekolada gorzka", "T-shirt bawełniany", "Kawa ziarnista 1kg"),
                () -> exercise1(products, 3));
        Check.equal("ćw. 1: 1 najtańszy dostępny", List.of("Czekolada gorzka"), () -> exercise1(products, 1));
        Check.equal("ćw. 2: rok ↓, imię ↑", EXPECTED_STUDENTS, () -> exercise2(SampleData.students()));
        Check.equal("ćw. 3: strona 1 (po polsku)", EXPECTED_PL_PAGE_1, () -> exercise3(products, 1, 4));
        Check.equal("ćw. 3: strona 3 z 5 (po polsku)", EXPECTED_PL_PAGE_3, () -> exercise3(products, 3, 5));
        Check.equal("ćw. 4: najnowsze zamówienie klienta", EXPECTED_LATEST, () -> exercise4(SampleData.orders()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec, 3)", List.of("Czekolada gorzka", "T-shirt bawełniany", "Kawa ziarnista 1kg"),
                () -> solution1(products, 3));
        Check.equal("ćw. 1 (wzorzec, 1)", List.of("Czekolada gorzka"), () -> solution1(products, 1));
        Check.equal("ćw. 2 (wzorzec)", EXPECTED_STUDENTS, () -> solution2(SampleData.students()));
        Check.equal("ćw. 3 (wzorzec, strona 1)", EXPECTED_PL_PAGE_1, () -> solution3(products, 1, 4));
        Check.equal("ćw. 3 (wzorzec, strona 3)", EXPECTED_PL_PAGE_3, () -> solution3(products, 3, 5));
        Check.equal("ćw. 4 (wzorzec)", EXPECTED_LATEST, () -> solution4(SampleData.orders()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    private static final List<String> EXPECTED_STUDENTS =
            List.of("Darek", "Filip", "Bartek", "Ela", "Gosia", "Ala", "Celina", "Henryk");

    private static final List<String> EXPECTED_PL_PAGE_1 =
            List.of("Czekolada gorzka", "Czysty kod", "Ekspres do kawy", "Java. Podstawy");

    private static final List<String> EXPECTED_PL_PAGE_3 =
            List.of("Słuchawki BT", "Smartfon X", "T-shirt bawełniany", "Wzorce projektowe");

    private static final List<String> EXPECTED_LATEST =
            List.of("ZAM-010", "ZAM-009", "ZAM-008", "ZAM-007", "ZAM-006", "ZAM-004");

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ na jeden strumień. Kod zwraca nazwy {@code n} najtańszych produktów,
     * które są na stanie:
     * <pre>{@code
     * List<Product> copy = new ArrayList<>(products);
     * copy.removeIf(p -> !p.inStock());
     * copy.sort(Comparator.comparing(Product::price));
     * List<String> result = new ArrayList<>();
     * for (int i = 0; i < Math.min(n, copy.size()); i++) {
     *     result.add(copy.get(i).name());
     * }
     * return result;
     * }</pre>
     * Podpowiedź: filter → sorted(comparing) → limit → map → toList. Uważaj na kolejność sorted i limit.
     */
    static List<String> exercise1(List<Product> products, int n) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (średnie): Zwróć imiona studentów posortowane po roku studiów MALEJĄCO,
     * a przy tym samym roku — po imieniu ROSNĄCO.
     * Podpowiedź: {@code comparingInt(Student::year).reversed().thenComparing(Student::name)}.
     * Sprawdź, co wyjdzie, gdy dasz reversed() na samym końcu — i dlaczego to błąd.
     */
    static List<String> exercise2(List<Student> students) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): Zwróć stronę {@code number} (od 1, po {@code size} pozycji) z nazwami produktów
     * posortowanymi wg POLSKIEGO alfabetu (Collator pl-PL). Uwaga: „Słuchawki” mają być przed „Smartfon”,
     * bo w polskim alfabecie ł jest przed m.
     * Podpowiedź: {@code sorted(Collator.getInstance(PL))} po map(Product::name), potem skip i limit
     * (możesz użyć metody page z tej lekcji).
     */
    static List<String> exercise3(List<Product> products, int number, int size) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): Dla każdego klienta znajdź jego NAJNOWSZE zamówienie (najpóźniejsza data)
     * i zwróć numery tych zamówień posortowane od najnowszego.
     * Podpowiedź: posortuj zamówienia po dacie malejąco, a potem zrób „unikalne wg klucza” (klucz = id klienta),
     * które zostawia PIERWSZE wystąpienie — czyli właśnie najnowsze. Kolejność po sortowaniu zostanie zachowana.
     */
    static List<String> exercise4(List<Order> orders) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(List<Product> products, int n) {
        return products.stream()
                .filter(Product::inStock)
                .sorted(Comparator.comparing(Product::price))
                .limit(n)
                .map(Product::name)
                .toList();
    }

    static List<String> solution2(List<Student> students) {
        return students.stream()
                .sorted(Comparator.comparingInt(Student::year).reversed()
                        .thenComparing(Student::name))
                .map(Student::name)
                .toList();
    }

    static List<String> solution3(List<Product> products, int number, int size) {
        List<String> sorted = products.stream()
                .map(Product::name)
                .sorted(Collator.getInstance(PL))
                .toList();
        return page(sorted, number, size);
    }

    static List<String> solution4(List<Order> orders) {
        return new ArrayList<>(orders.stream()
                .sorted(Comparator.comparing(Order::date).reversed())
                .collect(Collectors.toMap(o -> o.customer().id(), Order::id, (first, second) -> first, LinkedHashMap::new))
                .values());
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. String porównuje kody Unicode: wielkie A–Z mają kody 65–90, małe a–z 97–122, a polskie Ł, Ś, Ż ponad 300.
     *      Naprawa: sorted(Collator.getInstance(Locale.forLanguageTag("pl-PL"))).
     *   2. [1, 4, 5] — distinct zostawia 5, 1, 4 (pierwsze wystąpienia), sorted je układa.
     *   3. [1] — takeWhile kończy przy PIERWSZYM elemencie, który nie spełnia warunku (5). Dalsze 2 i 3 nie wejdą.
     *   4. limit(3) jest PRZED sorted → bierzemy 3 pierwsze z listy (Anna, Piotr, Katarzyna) i dopiero je sortujemy.
     *      Poprawnie: sorted(...).limit(3).
     *   5. reversed() na końcu odwraca CAŁY komparator — także działy. Poprawnie:
     *      comparing(Employee::department).thenComparing(Comparator.comparingInt(Employee::salary).reversed())
     *   6. NullPointerException — comparing wywołuje compareTo na null. Naprawa:
     *      comparing(Customer::email, Comparator.nullsLast(Comparator.naturalOrder())) (albo nullsFirst).
     *   7. Lambda ma stan (efekt uboczny): drugi przebieg z tym samym Set daje pusty wynik, HashSet nie jest
     *      bezpieczny wątkowo (parallel → losowe wyniki), a czytelnik łatwo przeoczy, że filter coś zmienia.
     *   8. Nie. distinct używa equals/hashCode; bez nich obowiązuje equals z Object, czyli porównanie adresów
     *      (to ten sam obiekt?). Dwa różne obiekty o tych samych polach są „różne”. Rekordy mają equals z automatu.
     */
    // </editor-fold>
}
