package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Customer;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Order;
import helpers.model.Product;
import helpers.model.Student;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static java.util.stream.Collectors.averagingInt;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Collectors.partitioningBy — podział na dwie grupy: true / false
 *        (partition = podział, przegroda; predicate = warunek, predykat)
 *
 * W SKRÓCIE:
 *   partitioningBy(warunek) dzieli elementy na DWIE grupy: spełniające warunek (true) i niespełniające (false).
 *   Wynik to {@code Map<Boolean, List<V>>}, który ZAWSZE ma oba klucze — nawet gdy któraś grupa jest pusta.
 *   Tak jak w groupingBy, drugi argument (downstream) mówi, co zrobić z każdą grupą.
 *
 * ANALOGIA: Bramka na lotnisku. Każdy pasażer (element) przechodzi przez JEDNĄ bramkę: zapiszczy (true)
 *   albo nie (false). Na końcu zawsze są dwa koszyki — „do kontroli” i „wolni” — nawet jeśli jeden jest pusty.
 *   Nikt nie przechodzi przez bramkę dwa razy (jedno przejście po danych), a nie trzeba dwóch bramek.
 *
 * JAK TO DZIAŁA:
 *   partitioningBy(warunek)               = partitioningBy(warunek, toList())
 *   partitioningBy(warunek, downstream)   → {false = downstream(nie spełniają), true = downstream(spełniają)}
 *
 *                        | partitioningBy(warunek)    | groupingBy(warunek)
 *   klucze               | ZAWSZE false i true        | tylko te, które wystąpiły
 *   get(true) gdy pusto  | [] / 0 / 0.0               | null
 *   rodzaj mapy          | specjalna, tylko odczyt    | HashMap (albo wybrana)
 *   liczba grup          | dokładnie 2                | dowolna
 *
 * SŁÓWKA:
 *   partition = podział; predicate = warunek (predykat); negate = zaprzecz; in stock = na stanie;
 *   passed / failed = zaliczył / nie zaliczył; expensive / cheap = drogi / tani; one pass = jedno przejście;
 *   nested = zagnieżdżony; outcome = wynik (rezultat)
 *
 * ZOBACZ TEŻ: t16_streams/Streams11GroupingBy (downstream, grupowanie wielopoziomowe),
 *   t16_streams/Streams09CollectorsBasic (counting, averagingInt), t13_lambdas/Lambda05Composition
 *   (Predicate.negate / and / or), t15_numbers/Numbers01BigDecimal (compareTo zamiast equals),
 *   t16_streams/Streams13AdvancedCollectors (teeing — dwa wyniki w jednym przejściu)
 * </pre>
 */
public class Streams12PartitioningBy {

    /** Próg „drogiego” produktu — stała BigDecimal utworzona RAZ, poza lambdą. */
    private static final BigDecimal EXPENSIVE_FROM = new BigDecimal("1000.00");

    /** Wynik studenta, gdy dwie grupy (true / false) to za mało. */
    enum Outcome { ZALICZONE, NIEZALICZONE, BRAK_OCEN }

    public static void main(String[] args) {
        title("Streams12 — Collectors.partitioningBy: podział true / false");

        basics();                  // basics = podstawy
        alwaysBothKeys();          // always both keys = zawsze oba klucze
        downstream();              // downstream = kolektor podrzędny
        studentsPassedFailed();    // students passed / failed = studenci zaliczeni / niezaliczeni
        expensiveCheap();          // expensive / cheap = drogie / tanie
        onePassVsTwoFilters();     // one pass vs two filters = jedno przejście kontra dwa filtry
        nestedPartitioning();      // nested partitioning = podział zagnieżdżony
        exercises();               // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PODSTAWY: MAP<BOOLEAN, LIST<V>>
    // =================================================================================================

    /**
     * 1. partitioningBy(warunek) — dwie listy: false (nie spełniają) i true (spełniają).
     * Kolejność kluczy w wydruku zawsze ta sama: najpierw false, potem true.
     */
    static void basics() {
        section("1. partitioningBy → Map<Boolean, List<V>>");

        List<Product> products = SampleData.products();

        Map<Boolean, List<Product>> byStock = products.stream()
                .collect(partitioningBy(Product::inStock));   // partitioningBy = podziel wg warunku; inStock = na stanie
        show("false (brak na stanie)", byStock.get(false));
        // WYNIK: false (brak na stanie) → [Smartfon X (2999.00 zł), Oliwa z oliwek (42.00 zł)]
        show("true (na stanie): ile", byStock.get(true).size());
        // WYNIK: true (na stanie): ile → 12
        show("klucze", byStock.keySet());
        // WYNIK: klucze → [false, true]

        // PUŁAPKA: mapa z partitioningBy jest TYLKO DO ODCZYTU (specjalna klasa, a nie HashMap).
        expectThrows("put do wyniku partitioningBy", () -> byStock.put(true, List.of()));
        // WYNIK: ✔ put do wyniku partitioningBy → rzucono UnsupportedOperationException: (brak komunikatu)

        // DOBRA PRAKTYKA: nazwij zmienne tak, by było wiadomo, co znaczy true: inStock.get(true), a nie map.get(true).
        List<Product> available = byStock.get(true);
        List<Product> soldOut = byStock.get(false);
        show("dostępne / wyprzedane", available.size() + " / " + soldOut.size());
        // WYNIK: dostępne / wyprzedane → 12 / 2
    }

    // =================================================================================================
    // 2. ZAWSZE OBA KLUCZE — RÓŻNICA WZGLĘDEM GROUPINGBY
    // =================================================================================================

    /**
     * 2. groupingBy z warunkiem jako kluczem wygląda podobnie, ale tworzy TYLKO te grupy, które wystąpiły.
     * partitioningBy ma zawsze false i true — nie trzeba się bać null.
     */
    static void alwaysBothKeys() {
        section("2. Zawsze oba klucze (vs groupingBy)");

        List<Product> products = SampleData.products();
        Predicate<Product> hugeStock = p -> p.stock() > 1000;   // Predicate = warunek; żaden produkt go nie spełnia

        Map<Boolean, Long> partitioned = products.stream().collect(partitioningBy(hugeStock, counting()));
        show("partitioningBy", partitioned);
        // WYNIK: partitioningBy → {false=14, true=0}

        Map<Boolean, Long> grouped = products.stream().collect(groupingBy(hugeStock::test, counting()));   // test = sprawdź
        show("groupingBy", grouped);
        // WYNIK: groupingBy → {false=14}

        show("partitioned.get(true)", partitioned.get(true));
        // WYNIK: partitioned.get(true) → 0
        show("grouped.get(true)", grouped.get(true));
        // WYNIK: grouped.get(true) → null    ← przy rozpakowaniu do long: NullPointerException

        Map<Boolean, List<String>> hugeNames = products.stream()
                .collect(partitioningBy(hugeStock, mapping(Product::name, toList())));
        show("partitioningBy + mapping: get(true)", hugeNames.get(true));
        // WYNIK: partitioningBy + mapping: get(true) → []

        // DOBRA PRAKTYKA: podział „tak / nie” → partitioningBy. Kod, który potem czyta get(true) i get(false),
        // nie musi sprawdzać null ani używać getOrDefault.
    }

    // =================================================================================================
    // 3. DOWNSTREAM: COUNTING, MAPPING, AVERAGINGINT
    // =================================================================================================

    /**
     * 3. Te same kolektory podrzędne co w groupingBy. Uwaga na averagingInt: pusta strona podziału
     * też istnieje i dostaje mylące 0.0.
     */
    static void downstream() {
        section("3. Downstream: counting, mapping, averagingInt");

        List<Employee> employees = SampleData.employees();
        Predicate<Employee> wellPaid = e -> e.salary() >= 10_000;   // „dobrze zarabia” = co najmniej 10 000 zł

        Map<Boolean, Long> howMany = employees.stream().collect(partitioningBy(wellPaid, counting()));
        show("ile osób (< 10 000 / ≥ 10 000)", howMany);
        // WYNIK: ile osób (< 10 000 / ≥ 10 000) → {false=6, true=4}

        Map<Boolean, String> names = employees.stream()
                .collect(partitioningBy(wellPaid, mapping(Employee::name, joining(", "))));
        showEach("kto", names);
        // WYNIK: kto (liczba kluczy: 2):
        // WYNIK: • false → Piotr Kowalski, Katarzyna Wiśniewska, Tomasz Wójcik, Magdalena Kamińska, Agnieszka Zielińska, Paweł Dąbrowski
        // WYNIK: • true → Anna Nowak, Michał Lewandowski, Krzysztof Szymański, Ewa Woźniak

        Map<Boolean, Double> avgAge = employees.stream().collect(partitioningBy(wellPaid, averagingInt(Employee::age)));
        show("średni wiek", avgAge);
        // WYNIK: średni wiek → {false=36.0, true=40.0}

        // PUŁAPKA: w partitioningBy obie strony istnieją ZAWSZE — także pusta. averagingInt pustej strony = 0.0,
        // co wygląda jak prawdziwa średnia. (W groupingBy pustych grup nie ma, więc tam ten problem nie występuje.)
        Map<Boolean, Double> avgStock = SampleData.products().stream()
                .collect(partitioningBy(p -> p.stock() > 1000, averagingInt(Product::stock)));
        show("średni stan (> 1000 szt. / reszta)", avgStock);
        // WYNIK: średni stan (> 1000 szt. / reszta) → {false=42.5, true=0.0}    ← true=0.0, choć takich produktów NIE MA
        // DOBRA PRAKTYKA: pokazujesz średnie? Pokaż obok liczebność (counting) albo sprawdź, czy strona nie jest pusta.
    }

    // =================================================================================================
    // 4. STUDENCI: ZALICZENI / NIEZALICZENI — CO Z HENRYKIEM?
    // =================================================================================================

    /**
     * 4. averageGrade() zwraca OptionalDouble — Henryk nie ma ocen. Zanim podzielisz, MUSISZ zdecydować,
     * gdzie trafia ktoś „bez danych”. Czasem uczciwiej jest mieć trzy grupy zamiast dwóch.
     */
    static void studentsPassedFailed() {
        section("4. Studenci: zaliczeni / niezaliczeni (i Henryk bez ocen)");

        List<Student> students = SampleData.students();

        // PUŁAPKA: getAsDouble() dla Henryka (brak ocen) rzuca wyjątek — i wywraca cały podział.
        expectThrows("getAsDouble() w warunku", () -> students.stream()
                .collect(partitioningBy(s -> s.averageGrade().getAsDouble() >= 3.0)));
        // WYNIK: ✔ getAsDouble() w warunku → rzucono NoSuchElementException: No value present

        // DECYZJA: brak ocen = NIE zaliczył (nie ma czego zaliczyć). Zapisujemy to jawnie przez orElse(0.0).
        Map<Boolean, List<String>> passed = students.stream()
                .collect(partitioningBy(s -> s.averageGrade().orElse(0.0) >= 3.0,   // orElse = albo (wartość zastępcza)
                        mapping(Student::name, toList())));
        show("zaliczeni (średnia ≥ 3.0)", passed);
        // WYNIK: zaliczeni (średnia ≥ 3.0) → {false=[Darek, Henryk], true=[Ala, Bartek, Celina, Ela, Filip, Gosia]}
        // Uwaga: Bartek ma średnią dokładnie 3.0 — przez „≥” zalicza. Granice warunków zawsze sprawdzaj osobno.

        // INNA DECYZJA: studentów bez ocen w ogóle nie oceniamy — odfiltruj ich PRZED podziałem.
        Map<Boolean, List<String>> onlyGraded = students.stream()
                .filter(s -> !s.grades().isEmpty())
                .collect(partitioningBy(s -> s.averageGrade().orElseThrow() >= 3.0, mapping(Student::name, toList())));
        show("tylko z ocenami", onlyGraded);
        // WYNIK: tylko z ocenami → {false=[Darek], true=[Ala, Bartek, Celina, Ela, Filip, Gosia]}

        // Dwie grupy to za mało? Wtedy groupingBy z własnym enumem (Outcome) i EnumMap — trzy uczciwe grupy.
        Map<Outcome, List<String>> threeWay = students.stream()
                .collect(groupingBy(Streams12PartitioningBy::outcomeOf,
                        () -> new EnumMap<>(Outcome.class),
                        mapping(Student::name, toList())));
        show("trzy grupy", threeWay);
        // WYNIK: trzy grupy → {ZALICZONE=[Ala, Bartek, Celina, Ela, Filip, Gosia], NIEZALICZONE=[Darek], BRAK_OCEN=[Henryk]}

        // DOBRA PRAKTYKA: partitioningBy tylko wtedy, gdy odpowiedź NAPRAWDĘ brzmi „tak” albo „nie”.
        // Pojawia się „nie wiadomo” / „brak danych”? To trzecia grupa → groupingBy z enumem.
    }

    /** Klasyfikacja studenta do jednej z trzech grup (używana w sekcji 4). */
    private static Outcome outcomeOf(Student s) {
        OptionalDouble average = s.averageGrade();
        if (average.isEmpty()) {                 // isEmpty (Java 11+) = czy pusty
            return Outcome.BRAK_OCEN;
        }
        return average.getAsDouble() >= 3.0 ? Outcome.ZALICZONE : Outcome.NIEZALICZONE;
    }

    // =================================================================================================
    // 5. DROGIE / TANIE — STAŁA BIGDECIMAL
    // =================================================================================================

    /**
     * 5. Warunek na pieniądzach: porównanie BigDecimal przez compareTo, a próg jako stała poza lambdą
     * (EXPENSIVE_FROM na górze klasy).
     */
    static void expensiveCheap() {
        section("5. Drogie / tanie (stała BigDecimal)");

        List<Product> products = SampleData.products();

        Map<Boolean, List<String>> expensive = products.stream()
                .collect(partitioningBy(p -> p.price().compareTo(EXPENSIVE_FROM) >= 0,   // compareTo = porównaj (≥ 0 → „co najmniej”)
                        mapping(Product::name, toList())));
        showEach("drogie (≥ 1000 zł)?", expensive);
        // WYNIK: drogie (≥ 1000 zł)? (liczba kluczy: 2):
        // WYNIK: • false → [Słuchawki BT, Kawa ziarnista 1kg, Czekolada gorzka, Oliwa z oliwek, Czysty kod, Java. Podstawy, Wzorce projektowe, Kurtka zimowa, T-shirt bawełniany, Lampka biurkowa]
        // WYNIK: • true → [Laptop Pro 14, Smartfon X, Monitor 27 cali, Ekspres do kawy]

        // Wartość magazynu po obu stronach: stockValue() = cena × sztuki; sumujemy BigDecimal przez reducing.
        Map<Boolean, BigDecimal> stockValue = products.stream()
                .collect(partitioningBy(p -> p.price().compareTo(EXPENSIVE_FROM) >= 0,
                        reducing(BigDecimal.ZERO, Product::stockValue, BigDecimal::add)));
        show("wartość magazynu (tanie / drogie)", stockValue);
        // WYNIK: wartość magazynu (tanie / drogie) → {false=33265.50, true=47493.93}

        // PUŁAPKA: equals w BigDecimal porównuje też SKALĘ (liczbę miejsc po przecinku).
        show("1000.00 equals 1000", EXPENSIVE_FROM.equals(new BigDecimal("1000")));
        // WYNIK: 1000.00 equals 1000 → false
        show("1000.00 compareTo 1000", EXPENSIVE_FROM.compareTo(new BigDecimal("1000")));
        // WYNIK: 1000.00 compareTo 1000 → 0    ← 0 znaczy „równe co do wartości”
        // DOBRA PRAKTYKA: próg jako static final — nie twórz new BigDecimal("1000.00") w lambdzie dla KAŻDEGO elementu.
    }

    // =================================================================================================
    // 6. JEDNO PRZEJŚCIE KONTRA DWA FILTRY
    // =================================================================================================

    /**
     * 6. Dwa filtry (warunek i jego zaprzeczenie) = dwa przejścia po danych i dwa miejsca z tym samym
     * warunkiem. partitioningBy = jedno przejście, jeden warunek.
     */
    static void onePassVsTwoFilters() {
        section("6. partitioningBy (jedno przejście) vs dwa filtry");

        List<Product> products = SampleData.products();

        // Licznik wywołań warunku. To EFEKT UBOCZNY w lambdzie — tylko do demonstracji, nie rób tak w kodzie produkcyjnym!
        AtomicInteger calls = new AtomicInteger();   // AtomicInteger = licznik int bezpieczny wątkowo
        Predicate<Product> inStockCounted = p -> {
            calls.incrementAndGet();                  // incrementAndGet = zwiększ i pobierz
            return p.inStock();
        };

        // PRZED: dwa filtry
        List<Product> yes = products.stream().filter(inStockCounted).toList();
        List<Product> no = products.stream().filter(inStockCounted.negate()).toList();   // negate = zaprzecz warunek
        show("dwa filtry: wywołań warunku", calls.get());
        // WYNIK: dwa filtry: wywołań warunku → 28

        // PO: jedno przejście
        calls.set(0);                                 // set = ustaw (zerujemy licznik)
        Map<Boolean, List<Product>> once = products.stream().collect(partitioningBy(inStockCounted));
        show("partitioningBy: wywołań warunku", calls.get());
        // WYNIK: partitioningBy: wywołań warunku → 14
        show("te same wyniki?", once.get(true).equals(yes) && once.get(false).equals(no));
        // WYNIK: te same wyniki? → true

        // Dlaczego to ważne:
        //   • dane czytane tylko raz (plik, zapytanie do bazy, Iterator) — drugi filtr nie ma już czego czytać,
        //   • kosztowny warunek (np. zapytanie sieciowe) liczony 2× zamiast 1×,
        //   • dwa miejsca z warunkiem → ktoś zmieni jedno, zapomni o drugim i element „wypadnie” z obu list
        //     albo trafi do obu.
        // DOBRA PRAKTYKA: potrzebujesz TYLKO jednej strony? Wtedy zwykły filter — partitioningBy byłby na wyrost.
    }

    // =================================================================================================
    // 7. PODZIAŁ W GROUPINGBY (I ODWROTNIE)
    // =================================================================================================

    /**
     * 7. partitioningBy może być downstreamem groupingBy (w każdym dziale: ilu dobrze zarabia) —
     * i odwrotnie (osobno dla dostępnych i wyprzedanych: ile w każdej kategorii).
     */
    static void nestedPartitioning() {
        section("7. partitioningBy w groupingBy (i odwrotnie)");

        Map<Department, Map<Boolean, Long>> wellPaidPerDept = SampleData.employees().stream()
                .collect(groupingBy(Employee::department, TreeMap::new,
                        partitioningBy(e -> e.salary() >= 10_000, counting())));
        showEach("dział → {< 10 000, ≥ 10 000}", wellPaidPerDept);
        // WYNIK: dział → {< 10 000, ≥ 10 000} (liczba kluczy: 5):
        // WYNIK: • IT → {false=1, true=3}
        // WYNIK: • HR → {false=1, true=0}
        // WYNIK: • SPRZEDAZ → {false=1, true=1}
        // WYNIK: • KSIEGOWOSC → {false=1, true=0}
        // WYNIK: • MARKETING → {false=2, true=0}
        // Zauważ: każdy dział ma OBA klucze — także tam, gdzie nikt nie zarabia ≥ 10 000 (true=0).

        show("IT: ilu dobrze zarabia", wellPaidPerDept.get(Department.IT).get(true));
        // WYNIK: IT: ilu dobrze zarabia → 3

        Map<Boolean, Map<Category, Long>> stockThenCategory = SampleData.products().stream()
                .collect(partitioningBy(Product::inStock,
                        groupingBy(Product::category, TreeMap::new, counting())));
        showEach("na stanie? → {kategoria → ile}", stockThenCategory);
        // WYNIK: na stanie? → {kategoria → ile} (liczba kluczy: 2):
        // WYNIK: • false → {ELEKTRONIKA=1, SPOZYWCZE=1}
        // WYNIK: • true → {ELEKTRONIKA=3, SPOZYWCZE=2, KSIAZKI=3, ODZIEZ=2, DOM=2}

        // PUŁAPKA: wewnętrzne groupingBy (drugi przykład) NIE tworzy pustych kategorii — po stronie false
        // nie ma KSIAZKI, ODZIEZ ani DOM. Tylko partitioningBy gwarantuje komplet kluczy (patrz Streams11, sekcja 10).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • partitioningBy(warunek) → Map<Boolean, List<V>>; partitioningBy(warunek, downstream) → Map<Boolean, D>.
     *   • ZAWSZE oba klucze false i true (pusta strona: [] / 0 / 0.0). groupingBy(warunek) — tylko wystąpione.
     *   • Wynik jest tylko do odczytu; wydruk zawsze w kolejności {false=..., true=...}.
     *   • Downstream jak w groupingBy: counting(), mapping(f, toList()/joining()), averagingInt, reducing(...).
     *   • averagingInt pustej strony = 0.0 — mylące! Pokazuj też liczebność.
     *   • OptionalDouble w warunku: zdecyduj o „braku danych” (orElse, filtr przed podziałem albo trzecia grupa).
     *   • Więcej niż „tak / nie” → groupingBy z enumem i EnumMap.
     *   • Pieniądze: compareTo z progiem static final BigDecimal, nie equals.
     *   • Jedno przejście zamiast dwóch filtrów: mniej pracy, jeden warunek, działa na danych czytanych raz.
     *   • Zagnieżdżanie: groupingBy(k, partitioningBy(w, counting())) i partitioningBy(w, groupingBy(k, ...)).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się wynik partitioningBy(warunek) od groupingBy(warunek), gdy żaden element nie spełnia warunku?
     *   2. Co wypisze:  System.out.println(Stream.of(1, 3, 5).collect(Collectors.partitioningBy(n -> n % 2 == 0)));  ?
     *   3. Co wypisze:  System.out.println(Stream.of(1, 3, 5).collect(Collectors.groupingBy(n -> n % 2 == 0)));  ?
     *   4. ZNAJDŹ BŁĄD:  students.stream().collect(partitioningBy(s -> s.averageGrade().getAsDouble() >= 3.0));
     *   5. ZNAJDŹ BŁĄD:  products.stream().collect(partitioningBy(p -> p.price().equals(new BigDecimal("129"))));
     *                    // „ma znaleźć produkty za 129 zł”, a strona true jest pusta
     *   6. Podaj trzy powody, dla których partitioningBy jest lepszy od dwóch filtrów (warunek i jego negacja).
     *   7. Kiedy zamiast partitioningBy wybierzesz groupingBy z własnym enumem?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: klienci VIP / zwykli", twoSides(5L, 2L), () -> exercise1(SampleData.customers()));
        Check.equal("ćw. 2: słowa dłuższe niż 5 liter", expectedLongWords(), () -> exercise2(SampleData.words()));
        Check.equal("ćw. 3: zamówienia zakończone / w toku", expectedFinalOrders(), () -> exercise3(SampleData.orders()));
        Check.equal("ćw. 4: wartość zamówień VIP / zwykłych", expectedVipTotals(), () -> exercise4(SampleData.orders()));
        Check.equal("ćw. 5: miasto → wyróżnieni (średnia ≥ 4.0)", expectedHonoursByCity(),
                () -> exercise5(SampleData.students()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", twoSides(5L, 2L), () -> solution1(SampleData.customers()));
        Check.equal("ćw. 2 (wzorzec)", expectedLongWords(), () -> solution2(SampleData.words()));
        Check.equal("ćw. 3 (wzorzec)", expectedFinalOrders(), () -> solution3(SampleData.orders()));
        Check.equal("ćw. 4 (wzorzec)", expectedVipTotals(), () -> solution4(SampleData.orders()));
        Check.equal("ćw. 5 (wzorzec)", expectedHonoursByCity(), () -> solution5(SampleData.students()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /** Pomocnik do oczekiwanych wyników: mapa {false=..., true=...} w stałej kolejności. */
    private static <V> Map<Boolean, V> twoSides(V whenFalse, V whenTrue) {
        Map<Boolean, V> map = new TreeMap<>();
        map.put(false, whenFalse);
        map.put(true, whenTrue);
        return map;
    }

    private static Map<Boolean, List<String>> expectedLongWords() {
        return twoSides(List.of("java", "java", "mapa", "lista", "java", "enum"),
                List.of("stream", "lambda", "kolekcja", "stream", "optional", "rekord"));
    }

    private static Map<Boolean, List<String>> expectedFinalOrders() {
        return twoSides(List.of("ZAM-003", "ZAM-005", "ZAM-006", "ZAM-008", "ZAM-009", "ZAM-010"),
                List.of("ZAM-001", "ZAM-002", "ZAM-004", "ZAM-007"));
    }

    private static Map<Boolean, BigDecimal> expectedVipTotals() {
        return twoSides(new BigDecimal("4523.19"), new BigDecimal("11425.77"));
    }

    private static Map<String, Map<Boolean, List<String>>> expectedHonoursByCity() {
        Map<String, Map<Boolean, List<String>>> expected = new TreeMap<>();
        expected.put("Gdańsk", twoSides(List.of("Henryk"), List.of("Celina")));
        expected.put("Kraków", twoSides(List.of("Bartek"), List.of("Ela")));
        expected.put("Poznań", twoSides(List.of("Filip"), List.of()));
        expected.put("Warszawa", twoSides(List.of("Darek"), List.of("Ala", "Gosia")));
        return expected;
    }

    /**
     * ĆWICZENIE 1 (łatwe): podziel klientów na VIP (true) i zwykłych (false) i policz każdą stronę.
     * Podpowiedź: partitioningBy(Customer::vip, counting()).
     */
    static Map<Boolean, Long> exercise1(List<Customer> customers) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 2 (łatwe): podziel słowa na dłuższe niż 5 liter (true) i pozostałe (false), zachowaj powtórzenia
     * i kolejność.
     * Podpowiedź: partitioningBy(w -> w.length() > 5) — domyślny downstream to toList().
     */
    static Map<Boolean, List<String>> exercise2(List<String> words) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): numery zamówień podzielone na zakończone (status.isFinal() → true) i w toku.
     * Podpowiedź: partitioningBy(o -> o.status().isFinal(), mapping(Order::id, toList())).
     */
    static Map<Boolean, List<String>> exercise3(List<Order> orders) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 4 (średnie): PRZEPISZ dwie pętle na JEDNO przejście z partitioningBy.
     * <pre>{@code
     * BigDecimal vip = BigDecimal.ZERO;
     * for (Order o : orders) {
     *     if (o.customer().vip()) { vip = vip.add(o.total()); }
     * }
     * BigDecimal regular = BigDecimal.ZERO;
     * for (Order o : orders) {
     *     if (!o.customer().vip()) { regular = regular.add(o.total()); }
     * }
     * return Map.of(true, vip, false, regular);
     * }</pre>
     * Podpowiedź: partitioningBy(o -> o.customer().vip(), reducing(BigDecimal.ZERO, Order::total, BigDecimal::add)).
     */
    static Map<Boolean, BigDecimal> exercise4(List<Order> orders) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): dla każdego miasta (posortowane) podziel IMIONA studentów na wyróżnionych
     * (średnia ≥ 4.0 → true) i pozostałych (false). Student bez ocen trafia do false.
     * Oczekiwane np. Poznań → {false=[Filip], true=[]} — pusta strona też musi być!
     * Podpowiedź: groupingBy(Student::city, TreeMap::new, partitioningBy(warunek, mapping(Student::name, toList()))),
     * warunek: s.averageGrade().orElse(0.0) >= 4.0.
     */
    static Map<String, Map<Boolean, List<String>>> exercise5(List<Student> students) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Map<Boolean, Long> solution1(List<Customer> customers) {
        return customers.stream()
                .collect(partitioningBy(Customer::vip, counting()));
    }

    static Map<Boolean, List<String>> solution2(List<String> words) {
        return words.stream()
                .collect(partitioningBy(w -> w.length() > 5));
    }

    static Map<Boolean, List<String>> solution3(List<Order> orders) {
        return orders.stream()
                .collect(partitioningBy(o -> o.status().isFinal(), mapping(Order::id, toList())));
    }

    static Map<Boolean, BigDecimal> solution4(List<Order> orders) {
        return orders.stream()
                .collect(partitioningBy(o -> o.customer().vip(),
                        reducing(BigDecimal.ZERO, Order::total, BigDecimal::add)));
    }

    static Map<String, Map<Boolean, List<String>>> solution5(List<Student> students) {
        return students.stream()
                .collect(groupingBy(Student::city, TreeMap::new,
                        partitioningBy(s -> s.averageGrade().orElse(0.0) >= 4.0,
                                mapping(Student::name, toList()))));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. partitioningBy ma oba klucze: {false=[...wszystkie...], true=[]}. groupingBy ma tylko {false=[...]},
     *      a get(true) zwraca null.
     *   2. {false=[1, 3, 5], true=[]}
     *   3. {false=[1, 3, 5]}  — groupingBy nie tworzy pustej grupy true.
     *   4. Henryk nie ma ocen → averageGrade() jest pusty → getAsDouble() rzuca NoSuchElementException i cały collect
     *      się wywraca. Trzeba zdecydować: orElse(0.0) (brak ocen = nie zaliczył), odfiltrować takich studentów
     *      albo trzecia grupa (groupingBy z enumem).
     *   5. equals w BigDecimal porównuje też skalę: 129.00 nie equals 129 → strona true pusta. Poprawnie:
     *      p.price().compareTo(PRICE) == 0, a PRICE jako stała static final (nie new BigDecimal w lambdzie).
     *   6. (a) jedno przejście po danych zamiast dwóch — ważne przy danych czytanych raz (plik, Iterator);
     *      (b) warunek liczony raz na element (kosztowny warunek!); (c) jeden warunek w jednym miejscu —
     *      nie da się „rozjechać” warunku i jego zaprzeczenia.
     *   7. Gdy odpowiedzi są więcej niż dwie, np. zaliczone / niezaliczone / brak ocen, albo gdy „nie wiadomo”
     *      ma być osobną grupą. Wtedy groupingBy(klasyfikator, () -> new EnumMap<>(X.class), ...).
     */
    // </editor-fold>
}
