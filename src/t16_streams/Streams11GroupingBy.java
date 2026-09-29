package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Customer;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Order;
import helpers.model.OrderStatus;
import helpers.model.Product;
import helpers.model.Student;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static java.util.stream.Collectors.averagingDouble;
import static java.util.stream.Collectors.averagingInt;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.maxBy;
import static java.util.stream.Collectors.minBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Collectors.groupingBy — grupowanie elementów strumienia
 *        (grouping by = grupowanie według; downstream = kolektor podrzędny)
 *
 * W SKRÓCIE:
 *   groupingBy(klucz) dzieli elementy na grupy o tym samym kluczu i zwraca mapę {@code Map<K, List<V>>}.
 *   Drugi (lub trzeci) argument — downstream — mówi, co zrobić z KAŻDĄ grupą: policzyć, zsumować,
 *   uśrednić, wyciągnąć nazwy, znaleźć największy element albo… pogrupować jeszcze raz.
 *
 * ANALOGIA: Sortownia listów na poczcie. Każdy list (element) trafia do przegródki z kodem pocztowym
 *   (klucz). Przegródki powstają dopiero, gdy przyjdzie pierwszy list z danym kodem — pustych nie ma.
 *   Na koniec możesz: oddać całe przegródki (toList), policzyć listy w każdej (counting),
 *   zważyć je (summingInt), albo w każdej przegródce posortować jeszcze po ulicach (groupingBy w groupingBy).
 *
 * JAK TO DZIAŁA:
 *   groupingBy(klucz)                              = groupingBy(klucz, HashMap::new, toList())
 *   groupingBy(klucz, downstream)                  → HashMap, każda grupa przetworzona przez downstream
 *   groupingBy(klucz, TreeMap::new, downstream)    → wybrana mapa (TreeMap / EnumMap / LinkedHashMap)
 *
 *   dla każdego elementu e:  k = klucz(e);  jeśli brak grupy k → utwórz;  dorzuć e do grupy k (downstream)
 *
 *   downstream                         | wartość w mapie
 *   toList() / toSet()                 | {@code List<V> / Set<V>}
 *   counting()                         | Long
 *   summingInt(f) / averagingInt(f)    | Integer / Double
 *   mapping(f, toList())               | {@code List<f(V)>}
 *   mapping(f, joining(", "))          | String
 *   maxBy(c) / minBy(c)                | {@code Optional<V>}  (zapach! → Streams13)
 *   reducing(zero, f, op)              | wynik redukcji (np. BigDecimal)
 *   groupingBy(...)                    | kolejna mapa (grupowanie wielopoziomowe)
 *
 * SŁÓWKA:
 *   group = grupa; classifier = klasyfikator (funkcja dająca klucz); downstream = kolektor podrzędny;
 *   mapping = przekształcanie (mapowanie) w grupie; reducing = redukowanie; multi-level = wielopoziomowy;
 *   composite key = klucz złożony; compute if absent = oblicz, jeśli brak; year month = rok i miesiąc;
 *   prefill = wstępne wypełnienie
 *
 * ZOBACZ TEŻ: t16_streams/Streams09CollectorsBasic (counting, summingInt, averagingInt, maxBy),
 *   t16_streams/Streams10CollectorsToMap (gdy klucz ma dokładnie JEDNĄ wartość),
 *   t16_streams/Streams12PartitioningBy (dwie grupy: true / false),
 *   t16_streams/Streams13AdvancedCollectors (collectingAndThen, filtering, flatMapping, teeing),
 *   t12_collections/Collections05Maps (computeIfAbsent, TreeMap, EnumMap)
 * </pre>
 */
public class Streams11GroupingBy {

    public static void main(String[] args) {
        title("Streams11 — Collectors.groupingBy: grupowanie");

        basics();                 // basics = podstawy
        beforeAfter();            // before / after = przed / po
        mapTypeAndOrder();        // map type and order = rodzaj mapy i kolejność
        downstreamNumbers();      // downstream numbers = liczby w grupach
        downstreamMapping();      // downstream mapping = przekształcanie w grupach
        maxByMinBy();             // max by / min by = największy / najmniejszy według
        reducingDownstream();     // reducing downstream = redukcja w grupach
        multiLevel();             // multi level = wielopoziomowe
        derivedAndCompositeKeys();// derived and composite keys = klucze wyliczone i złożone
        emptyGroups();            // empty groups = puste grupy
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PODSTAWY: MAP<K, LIST<V>>
    // =================================================================================================

    /**
     * 1. groupingBy(klucz) — każdemu kluczowi odpowiada LISTA elementów, które go dały.
     * Kolejność w liście = kolejność w strumieniu.
     */
    static void basics() {
        section("1. groupingBy → Map<K, List<V>>");

        List<String> words = SampleData.words();

        Map<Integer, List<String>> byLength = words.stream()
                .collect(groupingBy(String::length));   // groupingBy = grupuj według; klucz: długość słowa
        show("liczba grup", byLength.size());
        // WYNIK: liczba grup → 4
        show("byLength.get(6)", byLength.get(6));
        // WYNIK: byLength.get(6) → [stream, lambda, stream, rekord]
        show("byLength.get(7)", byLength.get(7));
        // WYNIK: byLength.get(7) → null    ← nie ma słowa o długości 7, więc nie ma grupy

        // Całą mapę wypisujemy przez TreeMap — posortowane klucze, zawsze ta sama kolejność.
        Map<Integer, List<String>> sorted = words.stream()
                .collect(groupingBy(String::length, TreeMap::new, toList()));   // toList = Collectors.toList (import statyczny)
        showEach("słowa wg długości", sorted);
        // WYNIK: słowa wg długości (liczba kluczy: 4):
        // WYNIK: • 4 → [java, java, mapa, java, enum]
        // WYNIK: • 5 → [lista]
        // WYNIK: • 6 → [stream, lambda, stream, rekord]
        // WYNIK: • 8 → [kolekcja, optional]

        // DOBRA PRAKTYKA: import statyczny (import static java.util.stream.Collectors.groupingBy itd.)
        // sprawia, że zagnieżdżone kolektory czyta się jak zdanie: groupingBy(dział, counting()).
        // Uwaga na dwa różne toList: stream.toList() (metoda strumienia) i toList() (kolektor jako downstream).
    }

    // =================================================================================================
    // 2. PRZED / PO: PĘTLA Z COMPUTEIFABSENT KONTRA GROUPINGBY
    // =================================================================================================

    /**
     * 2. Tak grupowało się „ręcznie” — i tak samo działa groupingBy w środku.
     */
    static void beforeAfter() {
        section("2. PRZED/PO: computeIfAbsent vs groupingBy");

        List<Employee> employees = SampleData.employees();

        // PRZED: pętla. computeIfAbsent = „daj listę dla klucza, a jak jej nie ma — utwórz”.
        Map<Department, List<String>> before = new TreeMap<>();
        for (Employee e : employees) {
            before.computeIfAbsent(e.department(), d -> new ArrayList<>())   // computeIfAbsent = oblicz, jeśli brak
                    .add(e.name());
        }

        // PO: jedno wyrażenie. mapping(...) zamienia Employee na imię (szczegóły w sekcji 5).
        Map<Department, List<String>> after = employees.stream()
                .collect(groupingBy(Employee::department, TreeMap::new, mapping(Employee::name, toList())));
        showEach("PO: osoby w działach", after);
        // WYNIK: PO: osoby w działach (liczba kluczy: 5):
        // WYNIK: • IT → [Anna Nowak, Piotr Kowalski, Michał Lewandowski, Ewa Woźniak]
        // WYNIK: • HR → [Katarzyna Wiśniewska]
        // WYNIK: • SPRZEDAZ → [Tomasz Wójcik, Krzysztof Szymański]
        // WYNIK: • KSIEGOWOSC → [Magdalena Kamińska]
        // WYNIK: • MARKETING → [Agnieszka Zielińska, Paweł Dąbrowski]
        show("PRZED.equals(PO)", before.equals(after));
        // WYNIK: PRZED.equals(PO) → true

        // PUŁAPKA: w wersji PRZED częsty błąd to map.get(k).add(x) bez sprawdzenia → NullPointerException
        // dla pierwszego elementu grupy. groupingBy nigdy o tym nie zapomni.
    }

    // =================================================================================================
    // 3. RODZAJ MAPY I KOLEJNOŚĆ: HASHMAP / TREEMAP / ENUMMAP / LINKEDHASHMAP
    // =================================================================================================

    /**
     * 3. Domyślnie groupingBy zwraca HashMap. Dla kluczy enum to pułapka: kolejność wydruku potrafi się
     * zmieniać między uruchomieniami. Wybierz mapę świadomie.
     */
    static void mapTypeAndOrder() {
        section("3. Rodzaj mapy i kolejność kluczy");

        List<Order> orders = SampleData.orders();

        Map<OrderStatus, Long> plain = orders.stream().collect(groupingBy(Order::status, counting()));
        show("HashMap: liczba grup", plain.size());
        // WYNIK: HashMap: liczba grup → 5
        show("HashMap: get(DOSTARCZONE)", plain.get(OrderStatus.DOSTARCZONE));
        // WYNIK: HashMap: get(DOSTARCZONE) → 3
        // PUŁAPKA: całej mapy `plain` NIE wypisujemy. hashCode enuma to „hash tożsamości” nadawany przez JVM,
        // inny przy każdym starcie programu → kolejność w HashMap raz jest taka, raz inna.
        // Pobieranie get(...) działa poprawnie — problem dotyczy tylko KOLEJNOŚCI.

        Map<OrderStatus, Long> tree = orders.stream()
                .collect(groupingBy(Order::status, TreeMap::new, counting()));
        show("TreeMap (kolejność stałych enuma)", tree);
        // WYNIK: TreeMap (kolejność stałych enuma) → {NOWE=2, OPLACONE=2, WYSLANE=2, DOSTARCZONE=3, ANULOWANE=1}

        Map<OrderStatus, Long> enumMap = orders.stream()
                .collect(groupingBy(Order::status, () -> new EnumMap<>(OrderStatus.class), counting()));
        show("EnumMap (to samo, szybciej)", enumMap);
        // WYNIK: EnumMap (to samo, szybciej) → {NOWE=2, OPLACONE=2, WYSLANE=2, DOSTARCZONE=3, ANULOWANE=1}

        Map<OrderStatus, Long> linked = orders.stream()
                .collect(groupingBy(Order::status, LinkedHashMap::new, counting()));
        show("LinkedHashMap (kolejność 1. wystąpienia)", linked);
        // WYNIK: LinkedHashMap (kolejność 1. wystąpienia) → {DOSTARCZONE=3, WYSLANE=2, ANULOWANE=1, OPLACONE=2, NOWE=2}

        // Enum jest Comparable: porządek naturalny = kolejność deklaracji stałych (ordinal), nie alfabet.
        // DOBRA PRAKTYKA: klucze enum → EnumMap albo TreeMap::new; klucze String/liczby → TreeMap::new do raportów;
        // „tak jak w danych” → LinkedHashMap::new. Zwykły HashMap tylko wtedy, gdy kolejność naprawdę nie ma znaczenia.
    }

    // =================================================================================================
    // 4. DOWNSTREAM: COUNTING, SUMMINGINT, AVERAGINGINT, AVERAGINGDOUBLE
    // =================================================================================================

    /**
     * 4. Liczby w grupach: ile, ile razem, ile średnio. To te same kolektory co w Streams09,
     * tylko działają osobno w każdej grupie.
     */
    static void downstreamNumbers() {
        section("4. Downstream: counting, summingInt, averagingInt, averagingDouble");

        List<Employee> employees = SampleData.employees();

        Map<Department, Long> headcount = employees.stream()
                .collect(groupingBy(Employee::department, TreeMap::new, counting()));   // counting = policz
        show("liczba osób", headcount);
        // WYNIK: liczba osób → {IT=4, HR=1, SPRZEDAZ=2, KSIEGOWOSC=1, MARKETING=2}

        Map<Department, Integer> payroll = employees.stream()
                .collect(groupingBy(Employee::department, TreeMap::new, summingInt(Employee::salary)));   // summingInt = sumuj int
        show("suma pensji", payroll);
        // WYNIK: suma pensji → {IT=53600, HR=7200, SPRZEDAZ=20100, KSIEGOWOSC=8100, MARKETING=17400}

        Map<Department, Double> avgSalary = employees.stream()
                .collect(groupingBy(Employee::department, TreeMap::new, averagingInt(Employee::salary)));   // averagingInt = uśrednij int
        show("średnia pensja", avgSalary);
        // WYNIK: średnia pensja → {IT=13400.0, HR=7200.0, SPRZEDAZ=10050.0, KSIEGOWOSC=8100.0, MARKETING=8700.0}

        // averagingDouble: średnia ze średnich ocen, osobno dla każdego roku studiów (Henryk bez ocen — pomijamy)
        Map<Integer, Double> avgByYear = SampleData.students().stream()
                .filter(s -> !s.grades().isEmpty())
                .collect(groupingBy(Student::year, TreeMap::new,
                        averagingDouble(s -> s.averageGrade().orElseThrow())));   // averagingDouble = uśrednij double
        show("średnia ocen wg roku", avgByYear);
        // WYNIK: średnia ocen wg roku → {1=4.5, 2=4.0, 3=3.0}

        // Ciekawostka: w Streams09 averagingInt dla PUSTEGO strumienia dawał mylące 0.0. W groupingBy to nie grozi —
        // grupa powstaje dopiero z pierwszym elementem, więc nigdy nie jest pusta.
    }

    // =================================================================================================
    // 5. DOWNSTREAM: MAPPING (TOLIST / TOSET / JOINING) I TOSET
    // =================================================================================================

    /**
     * 5. mapping(f, kolektor) — najpierw przekształć każdy element grupy (np. Employee → imię),
     * potem zbierz kolektorem. Bez mapping w grupach siedzą całe obiekty.
     */
    static void downstreamMapping() {
        section("5. Downstream: mapping(...), toSet()");

        Map<Department, String> namesJoined = SampleData.employees().stream()
                .collect(groupingBy(Employee::department, TreeMap::new,
                        mapping(Employee::name, joining(", "))));   // mapping = przekształć, potem zbierz
        showEach("imiona po przecinku", namesJoined);
        // WYNIK: imiona po przecinku (liczba kluczy: 5):
        // WYNIK: • IT → Anna Nowak, Piotr Kowalski, Michał Lewandowski, Ewa Woźniak
        // WYNIK: • HR → Katarzyna Wiśniewska
        // WYNIK: • SPRZEDAZ → Tomasz Wójcik, Krzysztof Szymański
        // WYNIK: • KSIEGOWOSC → Magdalena Kamińska
        // WYNIK: • MARKETING → Agnieszka Zielińska, Paweł Dąbrowski

        Map<String, List<String>> customersByCity = SampleData.customers().stream()
                .collect(groupingBy(Customer::city, TreeMap::new, mapping(Customer::name, toList())));
        show("klienci wg miasta", customersByCity);
        // WYNIK: klienci wg miasta → {Gdańsk=[Adam Mazur], Kraków=[Maria Nowak, Marek Król], Poznań=[Ola Pawlak], Warszawa=[Jan Kowalski, Zofia Krawczyk], Wrocław=[Ewa Lis]}

        // mapping + toCollection(TreeSet::new): bez powtórzeń I posortowane. Gdyby ktoś miał dwa zamówienia
        // w tym samym statusie, w zbiorze pojawiłby się tylko raz.
        Map<OrderStatus, TreeSet<String>> whoByStatus = SampleData.orders().stream()
                .collect(groupingBy(Order::status, TreeMap::new,
                        mapping(o -> o.customer().name(), toCollection(TreeSet::new))));
        showEach("kto ma zamówienia w danym statusie", whoByStatus);
        // WYNIK: kto ma zamówienia w danym statusie (liczba kluczy: 5):
        // WYNIK: • NOWE → [Maria Nowak, Ola Pawlak]
        // WYNIK: • OPLACONE → [Jan Kowalski, Zofia Krawczyk]
        // WYNIK: • WYSLANE → [Jan Kowalski, Marek Król]
        // WYNIK: • DOSTARCZONE → [Jan Kowalski, Maria Nowak, Zofia Krawczyk]
        // WYNIK: • ANULOWANE → [Adam Mazur]

        // toSet() jako downstream: grupy bez duplikatów (kolejność w środku nieokreślona — nie wypisujemy całych zbiorów)
        Map<Integer, Set<String>> uniqueByLength = SampleData.words().stream()
                .collect(groupingBy(String::length, TreeMap::new, toSet()));   // toSet = do zbioru
        show("unikalne słowa o długości 4: ile", uniqueByLength.get(4).size());
        // WYNIK: unikalne słowa o długości 4: ile → 3    ← java, mapa, enum (java trzy razy, ale w zbiorze raz)

        // PUŁAPKA: chcesz wszystkie UMIEJĘTNOŚCI (listy!) w dziale? mapping(Employee::skills, toList()) da
        // listę list: [[Java, Spring, SQL], [Java, Docker], ...]. Do spłaszczenia służy flatMapping (Java 9+) — Streams13.
    }

    // =================================================================================================
    // 6. MAXBY / MINBY W GRUPACH — OPTIONAL W WARTOŚCIACH
    // =================================================================================================

    /**
     * 6. maxBy/minBy jako downstream dają {@code Map<K, Optional<V>>}. Działa, ale to „zapach kodu”:
     * grupa nigdy nie jest pusta, więc Optional niczego tu nie chroni — tylko przeszkadza.
     */
    static void maxByMinBy() {
        section("6. maxBy / minBy w grupach (Optional w wartościach)");

        List<Employee> employees = SampleData.employees();

        Map<Department, Optional<Employee>> topEarner = employees.stream()
                .collect(groupingBy(Employee::department, TreeMap::new,
                        maxBy(Comparator.comparingInt(Employee::salary))));   // maxBy = największy według
        showEach("najlepiej zarabiający", topEarner);
        // WYNIK: najlepiej zarabiający (liczba kluczy: 5):
        // WYNIK: • IT → Optional[Michał Lewandowski (IT, 17200 zł)]
        // WYNIK: • HR → Optional[Katarzyna Wiśniewska (HR, 7200 zł)]
        // WYNIK: • SPRZEDAZ → Optional[Krzysztof Szymański (SPRZEDAZ, 11200 zł)]
        // WYNIK: • KSIEGOWOSC → Optional[Magdalena Kamińska (KSIEGOWOSC, 8100 zł)]
        // WYNIK: • MARKETING → Optional[Paweł Dąbrowski (MARKETING, 9800 zł)]

        // Każdy czytelnik tej mapy musi „rozpakować” Optional, choć wiadomo, że zawsze coś w nim jest:
        show("topEarner.get(IT)...name()", topEarner.get(Department.IT).map(Employee::name).orElse("?"));
        // WYNIK: topEarner.get(IT)...name() → Michał Lewandowski

        // Lepiej: collectingAndThen (= zbierz, a potem przekształć) — dokładnie w t16_streams/Streams13AdvancedCollectors.
        Map<Department, String> youngest = employees.stream()
                .collect(groupingBy(Employee::department, TreeMap::new,
                        collectingAndThen(                                    // collectingAndThen = zbierz, a potem…
                                minBy(Comparator.comparingInt(Employee::age)), // minBy = najmniejszy według
                                opt -> opt.map(Employee::name).orElseThrow())));
        show("najmłodszy w dziale", youngest);
        // WYNIK: najmłodszy w dziale → {IT=Piotr Kowalski, HR=Katarzyna Wiśniewska, SPRZEDAZ=Tomasz Wójcik, KSIEGOWOSC=Magdalena Kamińska, MARKETING=Agnieszka Zielińska}

        // DOBRA PRAKTYKA: potrzebujesz JEDNEGO elementu na grupę (max/min)? Rozważ też toMap z
        // BinaryOperator.maxBy (t16_streams/Streams10CollectorsToMap, sekcja 3) — tam wartości nie są Optional.
    }

    // =================================================================================================
    // 7. REDUCING — OGÓLNA REDUKCJA W GRUPIE (NP. BIGDECIMAL)
    // =================================================================================================

    /**
     * 7. reducing(zero, f, op) to reduce ze Streams07 w wersji downstream. Najważniejsze zastosowanie:
     * sumowanie BigDecimal w grupach (nie ma „summingBigDecimal”).
     */
    static void reducingDownstream() {
        section("7. reducing — np. suma BigDecimal w grupach");

        // Rozgrzewka: reducing(0, Employee::salary, Integer::sum) robi to samo co summingInt(Employee::salary).
        Map<Department, Integer> payroll = SampleData.employees().stream()
                .collect(groupingBy(Employee::department, TreeMap::new,
                        reducing(0, Employee::salary, Integer::sum)));   // reducing = redukuj (zero, co brać, jak łączyć)
        show("reducing = summingInt", payroll);
        // WYNIK: reducing = summingInt → {IT=53600, HR=7200, SPRZEDAZ=20100, KSIEGOWOSC=8100, MARKETING=17400}

        // Właściwy przykład: wartość zamówień każdego klienta. Pieniądze → BigDecimal (t15_numbers/Numbers01BigDecimal).
        List<Order> orders = SampleData.orders();
        Map<String, BigDecimal> totalPerCustomer = orders.stream()
                .collect(groupingBy(o -> o.customer().name(), TreeMap::new,
                        reducing(BigDecimal.ZERO, Order::total, BigDecimal::add)));
        showEach("suma zamówień klienta", totalPerCustomer);
        // WYNIK: suma zamówień klienta (liczba kluczy: 6):
        // WYNIK: • Adam Mazur → 2999.00
        // WYNIK: • Jan Kowalski → 6669.79
        // WYNIK: • Marek Król → 295.45
        // WYNIK: • Maria Nowak → 619.77
        // WYNIK: • Ola Pawlak → 608.97
        // WYNIK: • Zofia Krawczyk → 4755.98

        // Anulowane zamówienia nie powinny się liczyć. Odfiltruj je PRZED grupowaniem:
        Map<String, BigDecimal> withoutCancelled = orders.stream()
                .filter(o -> o.status() != OrderStatus.ANULOWANE)
                .collect(groupingBy(o -> o.customer().name(), TreeMap::new,
                        reducing(BigDecimal.ZERO, Order::total, BigDecimal::add)));
        show("bez anulowanych: klientów", withoutCancelled.size());
        // WYNIK: bez anulowanych: klientów → 5    ← Adam Mazur zniknął: jego jedyne zamówienie było anulowane (sekcja 10)

        // PUŁAPKA: jednoargumentowe reducing(BigDecimal::add) (bez wartości początkowej) daje w mapie Optional
        // (jak reduce bez identity w Streams07). Z BigDecimal.ZERO na początku dostajesz od razu BigDecimal.
        // DOBRA PRAKTYKA: BigDecimal.ZERO i BigDecimal::add to stałe/metody — nie twórz new BigDecimal("0") w lambdzie.
    }

    // =================================================================================================
    // 8. GRUPOWANIE WIELOPOZIOMOWE
    // =================================================================================================

    /**
     * 8. Downstream może być kolejnym groupingBy. Wynik: mapa map — np. dział → dekada wieku → osoby.
     */
    static void multiLevel() {
        section("8. Grupowanie wielopoziomowe: dział → dekada wieku");

        List<Employee> employees = SampleData.employees();

        // Klucz drugiego poziomu: dekada wieku, np. 34 / 10 * 10 = 30 (dzielenie całkowite obcina resztę).
        Map<Department, Map<Integer, List<String>>> byDeptAndDecade = employees.stream()
                .collect(groupingBy(Employee::department, TreeMap::new,
                        groupingBy(e -> e.age() / 10 * 10, TreeMap::new,
                                mapping(Employee::name, toList()))));
        showEach("dział → {dekada → osoby}", byDeptAndDecade);
        // WYNIK: dział → {dekada → osoby} (liczba kluczy: 5):
        // WYNIK: • IT → {20=[Piotr Kowalski], 30=[Anna Nowak, Ewa Woźniak], 40=[Michał Lewandowski]}
        // WYNIK: • HR → {40=[Katarzyna Wiśniewska]}
        // WYNIK: • SPRZEDAZ → {30=[Tomasz Wójcik], 50=[Krzysztof Szymański]}
        // WYNIK: • KSIEGOWOSC → {40=[Magdalena Kamińska]}
        // WYNIK: • MARKETING → {20=[Agnieszka Zielińska], 30=[Paweł Dąbrowski]}

        show("IT, trzydziestolatkowie", byDeptAndDecade.get(Department.IT).get(30));
        // WYNIK: IT, trzydziestolatkowie → [Anna Nowak, Ewa Woźniak]

        Map<Department, Map<Integer, Long>> countByDeptAndDecade = employees.stream()
                .collect(groupingBy(Employee::department, TreeMap::new,
                        groupingBy(e -> e.age() / 10 * 10, TreeMap::new, counting())));
        show("liczby", countByDeptAndDecade);
        // WYNIK: liczby → {IT={20=1, 30=2, 40=1}, HR={40=1}, SPRZEDAZ={30=1, 50=1}, KSIEGOWOSC={40=1}, MARKETING={20=1, 30=1}}

        // PUŁAPKA: klucz jako tekst, np. "20-29", "30-39", sortuje się ALFABETYCZNIE: "100-109" byłoby przed "20-29".
        // Dlatego tu kluczem jest liczba (dekada), która sortuje się numerycznie.
        // DOBRA PRAKTYKA: więcej niż dwa poziomy zagnieżdżenia robi się nieczytelne — wtedy klucz złożony (sekcja 9).
    }

    // =================================================================================================
    // 9. KLUCZE WYLICZONE I ZŁOŻONE (LOKALNY RECORD)
    // =================================================================================================

    /**
     * 9. Kluczem może być cokolwiek, co wyliczysz z elementu: pierwsza litera, miesiąc z daty,
     * albo kilka pól naraz zamknięte w lokalnym rekordzie (record ma gotowe equals i hashCode).
     */
    static void derivedAndCompositeKeys() {
        section("9. Klucze wyliczone i złożone");

        // Pierwsza litera słowa (bez powtórzeń słów)
        Map<Character, List<String>> byFirstLetter = SampleData.words().stream()
                .distinct()
                .collect(groupingBy(w -> w.charAt(0), TreeMap::new, toList()));   // charAt(0) = pierwszy znak
        show("wg pierwszej litery", byFirstLetter);
        // WYNIK: wg pierwszej litery → {e=[enum], j=[java], k=[kolekcja], l=[lambda, lista], m=[mapa], o=[optional], r=[rekord], s=[stream]}

        // Miesiąc z daty zamówienia: YearMonth.from(LocalDate) — rok i miesiąc, bez dnia (t17_datetime/DateTime01LocalDateTime)
        Map<YearMonth, Long> ordersPerMonth = SampleData.orders().stream()
                .collect(groupingBy(o -> YearMonth.from(o.date()), TreeMap::new, counting()));   // YearMonth = rok-miesiąc
        show("zamówienia wg miesiąca", ordersPerMonth);
        // WYNIK: zamówienia wg miesiąca → {2026-01=2, 2026-02=3, 2026-03=4, 2026-04=1}

        // Klucz złożony: miasto + rok studiów. Lokalny record (Java 16+) = mała klasa-krotka z equals/hashCode/toString.
        record CityYear(String city, int year) { }
        Comparator<CityYear> byCityThenYear = Comparator.comparing(CityYear::city)
                .thenComparingInt(CityYear::year);                                    // thenComparingInt = potem porównaj po int
        Map<CityYear, List<String>> studentsByCityYear = SampleData.students().stream()
                .collect(groupingBy(s -> new CityYear(s.city(), s.year()),
                        () -> new TreeMap<>(byCityThenYear),                          // TreeMap z własnym porządkiem
                        mapping(Student::name, toList())));
        showEach("studenci wg (miasto, rok)", studentsByCityYear);
        // WYNIK: studenci wg (miasto, rok) (liczba kluczy: 6):
        // WYNIK: • CityYear[city=Gdańsk, year=1] → [Celina, Henryk]
        // WYNIK: • CityYear[city=Kraków, year=2] → [Bartek, Ela]
        // WYNIK: • CityYear[city=Poznań, year=3] → [Filip]
        // WYNIK: • CityYear[city=Warszawa, year=1] → [Ala]
        // WYNIK: • CityYear[city=Warszawa, year=2] → [Gosia]
        // WYNIK: • CityYear[city=Warszawa, year=3] → [Darek]

        // PUŁAPKA: klucz sklejony z tekstu, np. s.city() + "-" + s.year(), „działa”, ale łatwo o kolizję
        // (miasto z myślnikiem w nazwie) i trzeba go potem rozcinać. Record jest bezpieczny i czytelny.
        // PUŁAPKA: record z polem enum jako klucz w HashMap = znów losowa kolejność wydruku → TreeMap z komparatorem.
    }

    // =================================================================================================
    // 10. PUSTE GRUPY NIE POWSTAJĄ (LOGISTYKA) + WSTĘPNE WYPEŁNIENIE ENUMMAP
    // =================================================================================================

    /**
     * 10. groupingBy tworzy tylko te grupy, dla których przyszedł choć jeden element. Dział LOGISTYKA
     * nie ma pracowników, więc w mapie go NIE MA — ani z zerem, ani z pustą listą.
     */
    static void emptyGroups() {
        section("10. Puste grupy nie powstają");

        Map<Department, Long> counts = SampleData.employees().stream()
                .collect(groupingBy(Employee::department, () -> new EnumMap<>(Department.class), counting()));
        show("liczba osób", counts);
        // WYNIK: liczba osób → {IT=4, HR=1, SPRZEDAZ=2, KSIEGOWOSC=1, MARKETING=2}
        show("containsKey(LOGISTYKA)", counts.containsKey(Department.LOGISTYKA));   // containsKey = czy zawiera klucz
        // WYNIK: containsKey(LOGISTYKA) → false
        show("get(LOGISTYKA)", counts.get(Department.LOGISTYKA));
        // WYNIK: get(LOGISTYKA) → null
        show("getOrDefault(LOGISTYKA, 0L)", counts.getOrDefault(Department.LOGISTYKA, 0L));   // getOrDefault = pobierz albo domyślna
        // WYNIK: getOrDefault(LOGISTYKA, 0L) → 0

        // PUŁAPKA: rozpakowanie null do long → NullPointerException.
        expectThrows("long n = counts.get(LOGISTYKA)", () -> {
            long n = counts.get(Department.LOGISTYKA);
            note("nie dojdziemy tutaj: " + n);
        });
        // WYNIK: ✔ long n = counts.get(LOGISTYKA) → rzucono NullPointerException: Cannot invoke "java.lang.Long.longValue()" because the return value of "java.util.Map.get(Object)" is null

        // Raport ze WSZYSTKIMI działami (także pustymi): wypełnij EnumMap zerami, potem nadpisz wynikami.
        Map<Department, Long> full = new EnumMap<>(Department.class);
        for (Department d : Department.values()) {   // values = wszystkie stałe enuma
            full.put(d, 0L);
        }
        full.putAll(counts);                          // putAll = wstaw wszystkie (nadpisze zera)
        show("wszystkie działy", full);
        // WYNIK: wszystkie działy → {IT=4, HR=1, SPRZEDAZ=2, KSIEGOWOSC=1, MARKETING=2, LOGISTYKA=0}

        // To samo strumieniem: idziemy po WSZYSTKICH działach, a liczbę bierzemy z mapy (albo 0).
        Map<Department, Long> fullByStream = Arrays.stream(Department.values())
                .collect(toMap(d -> d, d -> counts.getOrDefault(d, 0L), (a, b) -> a,
                        () -> new EnumMap<>(Department.class)));
        show("strumieniem — to samo?", fullByStream.equals(full));
        // WYNIK: strumieniem — to samo? → true

        // DOBRA PRAKTYKA: w raportach „dla każdego X” zaczynaj od listy WSZYSTKICH X (values(), lista klientów),
        // a nie od danych — inaczej puste pozycje po cichu znikną (jak klientka Ewa Lis bez zamówień).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • groupingBy(klucz) → HashMap<K, List<V>>; groupingBy(klucz, downstream); groupingBy(klucz, Mapa::new, downstream).
     *   • Klucze enum: TreeMap::new albo () -> new EnumMap<>(X.class). Nigdy nie wypisuj HashMap z kluczami enum.
     *     LinkedHashMap::new = kolejność pierwszego wystąpienia.
     *   • Downstream: counting() (Long), summingInt, averagingInt/Double, mapping(f, toList()/toSet()/joining()),
     *     toSet(), toCollection(TreeSet::new), maxBy/minBy (Optional!), reducing(zero, f, op), groupingBy (poziom 2).
     *   • BigDecimal w grupach: reducing(BigDecimal.ZERO, Order::total, BigDecimal::add).
     *   • Klucz może być wyliczony (charAt(0), YearMonth.from(date), age / 10 * 10) albo złożony (lokalny record).
     *   • Puste grupy NIE powstają: get → null, getOrDefault(k, 0L), albo wypełnij EnumMap wszystkimi stałymi.
     *   • computeIfAbsent(k, x -> new ArrayList<>()).add(v) — tak grupuje pętla; groupingBy robi to za Ciebie.
     *
     * PYTANIA KONTROLNE:
     *   1. Jaki typ mapy i jakie wartości zwraca groupingBy(f) bez dodatkowych argumentów?
     *   2. Co wypisze:
     *        System.out.println(Stream.of("aa", "b", "cc")
     *                .collect(Collectors.groupingBy(String::length, TreeMap::new, Collectors.counting())));  ?
     *   3. ZNAJDŹ BŁĄD (test porównuje wydruk z tekstem i raz przechodzi, raz nie):
     *        Map<Department, Long> m = employees.stream().collect(groupingBy(Employee::department, counting()));
     *        assertEquals("{IT=4, HR=1, ...}", m.toString());
     *   4. ZNAJDŹ BŁĄD:  long n = counts.get(Department.LOGISTYKA);   // counts z groupingBy(..., counting())
     *   5. Dlaczego groupingBy + maxBy daje Map<K, Optional<V>> i czemu te Optionale nigdy nie są puste?
     *   6. Jak zsumować wartości BigDecimal osobno w każdej grupie?
     *   7. Co wypisze:
     *        System.out.println(Stream.of("ala", "ola", "ada")
     *                .collect(Collectors.groupingBy(w -> w.charAt(0), TreeMap::new, Collectors.counting())));  ?
     *   8. Czym jest downstream? Podaj trzy przykłady.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: liczba produktów w kategoriach", expectedCountPerCategory(),
                () -> exercise1(SampleData.products()));
        Check.equal("ćw. 2: studenci wg miasta", expectedStudentsByCity(), () -> exercise2(SampleData.students()));
        Check.equal("ćw. 3: numery zamówień wg statusu", expectedIdsByStatus(), () -> exercise3(SampleData.orders()));
        Check.equal("ćw. 4: wartość zamówień wg miesiąca", expectedTotalPerMonth(), () -> exercise4(SampleData.orders()));
        Check.equal("ćw. 5: najdroższy produkt w kategorii", expectedMostExpensive(),
                () -> exercise5(SampleData.products()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", expectedCountPerCategory(), () -> solution1(SampleData.products()));
        Check.equal("ćw. 2 (wzorzec)", expectedStudentsByCity(), () -> solution2(SampleData.students()));
        Check.equal("ćw. 3 (wzorzec)", expectedIdsByStatus(), () -> solution3(SampleData.orders()));
        Check.equal("ćw. 4 (wzorzec)", expectedTotalPerMonth(), () -> solution4(SampleData.orders()));
        Check.equal("ćw. 5 (wzorzec)", expectedMostExpensive(), () -> solution5(SampleData.products()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    private static Map<Category, Long> expectedCountPerCategory() {
        return new EnumMap<>(Map.of(Category.ELEKTRONIKA, 4L, Category.SPOZYWCZE, 3L, Category.KSIAZKI, 3L,
                Category.ODZIEZ, 2L, Category.DOM, 2L));
    }

    private static Map<String, List<String>> expectedStudentsByCity() {
        return new TreeMap<>(Map.of("Gdańsk", List.of("Celina", "Henryk"), "Kraków", List.of("Bartek", "Ela"),
                "Poznań", List.of("Filip"), "Warszawa", List.of("Ala", "Darek", "Gosia")));
    }

    private static Map<OrderStatus, List<String>> expectedIdsByStatus() {
        return new EnumMap<>(Map.of(
                OrderStatus.NOWE, List.of("ZAM-006", "ZAM-009"),
                OrderStatus.OPLACONE, List.of("ZAM-005", "ZAM-010"),
                OrderStatus.WYSLANE, List.of("ZAM-003", "ZAM-008"),
                OrderStatus.DOSTARCZONE, List.of("ZAM-001", "ZAM-002", "ZAM-007"),
                OrderStatus.ANULOWANE, List.of("ZAM-004")));
    }

    private static Map<YearMonth, BigDecimal> expectedTotalPerMonth() {
        return new TreeMap<>(Map.of(
                YearMonth.of(2026, 1), new BigDecimal("6469.66"),
                YearMonth.of(2026, 2), new BigDecimal("5334.98"),
                YearMonth.of(2026, 3), new BigDecimal("3981.32"),
                YearMonth.of(2026, 4), new BigDecimal("163.00")));
    }

    private static Map<Category, String> expectedMostExpensive() {
        return new EnumMap<>(Map.of(Category.ELEKTRONIKA, "Laptop Pro 14", Category.SPOZYWCZE, "Kawa ziarnista 1kg",
                Category.KSIAZKI, "Java. Podstawy", Category.ODZIEZ, "Kurtka zimowa", Category.DOM, "Ekspres do kawy"));
    }

    /**
     * ĆWICZENIE 1 (łatwe): ile produktów jest w każdej kategorii? Zwróć mapę w kolejności stałych enuma.
     * Podpowiedź: groupingBy(Product::category, () -> new EnumMap<>(Category.class), counting()).
     */
    static Map<Category, Long> exercise1(List<Product> products) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 2 (łatwe): miasto → lista IMION studentów z tego miasta (w kolejności z danych),
     * miasta posortowane.
     * Podpowiedź: groupingBy(Student::city, TreeMap::new, mapping(Student::name, toList())).
     */
    static Map<String, List<String>> exercise2(List<Student> students) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ pętlę na groupingBy.
     * <pre>{@code
     * Map<OrderStatus, List<String>> result = new EnumMap<>(OrderStatus.class);
     * for (Order o : orders) {
     *     result.computeIfAbsent(o.status(), s -> new ArrayList<>()).add(o.id());
     * }
     * return result;
     * }</pre>
     * Podpowiedź: klucz Order::status, dostawca mapy jak w pętli, downstream mapping(Order::id, toList()).
     */
    static Map<OrderStatus, List<String>> exercise3(List<Order> orders) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 4 (średnie): suma wartości zamówień (BigDecimal) w każdym miesiącu, miesiące posortowane.
     * Podpowiedź: klucz YearMonth.from(o.date()), downstream reducing(BigDecimal.ZERO, Order::total, BigDecimal::add).
     */
    static Map<YearMonth, BigDecimal> exercise4(List<Order> orders) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): kategoria → NAZWA najdroższego produktu. Wartości mają być zwykłymi
     * Stringami, a nie {@code Optional<Product>}.
     * Podpowiedź: groupingBy + collectingAndThen(maxBy(Comparator.comparing(Product::price)),
     * opt -> opt.map(Product::name).orElseThrow()) — collectingAndThen szerzej w Streams13.
     * Alternatywa: toMap z BinaryOperator.maxBy (Streams10), a potem przepisanie wartości na nazwy.
     */
    static Map<Category, String> exercise5(List<Product> products) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Map<Category, Long> solution1(List<Product> products) {
        return products.stream()
                .collect(groupingBy(Product::category, () -> new EnumMap<>(Category.class), counting()));
    }

    static Map<String, List<String>> solution2(List<Student> students) {
        return students.stream()
                .collect(groupingBy(Student::city, TreeMap::new, mapping(Student::name, toList())));
    }

    static Map<OrderStatus, List<String>> solution3(List<Order> orders) {
        return orders.stream()
                .collect(groupingBy(Order::status, () -> new EnumMap<>(OrderStatus.class),
                        mapping(Order::id, toList())));
    }

    static Map<YearMonth, BigDecimal> solution4(List<Order> orders) {
        return orders.stream()
                .collect(groupingBy(o -> YearMonth.from(o.date()), TreeMap::new,
                        reducing(BigDecimal.ZERO, Order::total, BigDecimal::add)));
    }

    static Map<Category, String> solution5(List<Product> products) {
        return products.stream()
                .collect(groupingBy(Product::category, () -> new EnumMap<>(Category.class),
                        collectingAndThen(
                                maxBy(Comparator.comparing(Product::price)),   // BigDecimal porównany przez compareTo
                                opt -> opt.map(Product::name).orElseThrow())));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. HashMap<K, List<V>> — groupingBy(f) to skrót od groupingBy(f, HashMap::new, toList()).
     *      Kolejność kluczy nieokreślona, w listach kolejność jak w strumieniu.
     *   2. {1=1, 2=2}  — jedno słowo o długości 1 ("b"), dwa o długości 2 ("aa", "cc").
     *   3. Domyślna mapa to HashMap, a klucze enum mają hashCode tożsamości — inny przy każdym starcie JVM,
     *      więc kolejność w toString() się zmienia. Naprawa: groupingBy(Employee::department, TreeMap::new, counting())
     *      albo () -> new EnumMap<>(Department.class).
     *   4. W LOGISTYKA nikt nie pracuje, więc grupy nie ma: get zwraca null, a rozpakowanie null do long rzuca
     *      NullPointerException. Poprawnie: counts.getOrDefault(Department.LOGISTYKA, 0L).
     *   5. maxBy jest ogólnym kolektorem i dla pustego strumienia musi coś zwrócić, więc zwraca Optional. W groupingBy
     *      grupa powstaje dopiero z pierwszym elementem, więc Optional zawsze ma wartość — jest tylko balastem.
     *      Naprawa: collectingAndThen(maxBy(...), Optional::orElseThrow) (Streams13) albo toMap + BinaryOperator.maxBy.
     *   6. groupingBy(klucz, reducing(BigDecimal.ZERO, Order::total, BigDecimal::add)).
     *   7. {a=2, o=1}  — "ala" i "ada" zaczynają się na 'a', "ola" na 'o'.
     *   8. Downstream to kolektor, który przetwarza KAŻDĄ grupę osobno: counting(), summingInt(...),
     *      mapping(Employee::name, joining(", ")), maxBy(...), reducing(...), a nawet kolejne groupingBy(...).
     */
    // </editor-fold>
}
