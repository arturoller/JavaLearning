package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Customer;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Product;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Kolektory podstawowe — collect(Collectors.xxx)
 *        (collector = kolektor, „zbieracz”; collect = zbierz)
 *
 * W SKRÓCIE:
 *   collect(...) to operacja końcowa, która ZBIERA elementy strumienia w coś nowego: listę, zbiór, napis,
 *   liczbę, statystyki. Przepis na to „zbieranie” to Collector. Klasa Collectors ma gotowe przepisy:
 *   toList, toSet, toCollection, joining, counting, summingInt, averagingInt, minBy, maxBy, summarizingInt.
 *
 * ANALOGIA: Pakowanie rzeczy przy przeprowadzce.
 *   supplier    (dostawca)   = ktoś przynosi PUSTY karton,
 *   accumulator (akumulator) = wkładasz do kartonu rzeczy jedna po drugiej,
 *   combiner    (łącznik)    = pakowało kilka osób naraz? Zsypujecie dwa kartony w jeden,
 *   finisher    (wykończenie)= na koniec zaklejasz karton taśmą albo naklejasz etykietę.
 *   Collector to po prostu przepis zawierający te cztery kroki.
 *
 * JAK TO DZIAŁA:
 *   A pojemnik = supplier.get();                  ← np. new ArrayList, new StringBuilder, licznik
 *   dla każdego elementu t:  accumulator(pojemnik, t);
 *   (równolegle: pojemnik = combiner(pojemnik1, pojemnik2))
 *   R wynik = finisher(pojemnik);                  ← np. unmodifiableList, toString
 *
 *   kolektor                     | wynik
 *   toList() / toSet()           | {@code List<T> / Set<T>}
 *   toCollection(TreeSet::new)   | dokładnie ta kolekcja, którą wskażesz
 *   joining(", ", "[", "]")      | String (tylko dla tekstów!)
 *   counting()                   | Long  (nie int!)
 *   summingInt / averagingInt    | Integer / Double (pusty → 0 i 0.0)
 *   minBy / maxBy                | {@code Optional<T>}
 *   summarizingInt               | IntSummaryStatistics
 *
 * SŁÓWKA:
 *   collect = zbierz; collector = kolektor (zbieracz); supplier = dostawca; accumulator = akumulator (gromadzący);
 *   combiner = łącznik; finisher = wykańczacz (krok końcowy); joining = łączenie (napisów); counting = liczenie;
 *   summing = sumowanie; averaging = uśrednianie; summarizing = podsumowywanie; unmodifiable = niemodyfikowalny;
 *   downstream = kolektor podrzędny („w dół strumienia”)
 *
 * ZOBACZ TEŻ: t16_streams/Streams06TerminalOps (toList vs Collectors.toList vs toUnmodifiableList),
 *   t16_streams/Streams08PrimitiveStreams (sum/average/summaryStatistics na IntStream),
 *   t16_streams/Streams10CollectorsToMap (toMap), t16_streams/Streams11GroupingBy (kolektory jako downstream),
 *   t12_collections/Collections04Sets (HashSet vs TreeSet)
 * </pre>
 */
public class Streams09CollectorsBasic {

    public static void main(String[] args) {
        title("Streams09 — kolektory podstawowe: collect(Collectors.xxx)");

        collectorIdea();          // collector idea = idea kolektora
        toListSetCollection();    // to list / set / collection = do listy / zbioru / kolekcji
        unmodifiable();           // unmodifiable = niemodyfikowalne
        joining();                // joining = łączenie napisów
        countingAndSumming();     // counting and summing = liczenie i sumowanie
        averaging();              // averaging = uśrednianie
        minByMaxBy();             // min by / max by = najmniejszy / największy według
        summarizing();            // summarizing = podsumowanie (statystyki)
        streamOpsVsCollectors();  // stream ops vs collectors = operacje strumienia kontra kolektory
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. COLLECT I IDEA KOLEKTORA
    // =================================================================================================

    /**
     * 1. collect(...) z trzema funkcjami podanymi ręcznie, a potem własny Collector.of(...) z czterema.
     * Nie będziesz tego pisać na co dzień — ale zrozumiesz, co robią gotowe Collectors.xxx().
     */
    static void collectorIdea() {
        section("1. collect() i idea kolektora");

        List<String> words = SampleData.words();
        show("słowa", words);
        // WYNIK: słowa → [java, stream, lambda, kolekcja, java, mapa, lista, stream, java, optional, rekord, enum]

        // collect(dostawca, akumulator, łącznik) — trzy kroki „przeprowadzki” podane ręcznie
        ArrayList<String> longWords = words.stream()
                .filter(w -> w.length() > 5)
                .collect(ArrayList::new,       // supplier = dostawca: przynieś pusty karton
                        ArrayList::add,         // accumulator = akumulator: włóż jeden element
                        ArrayList::addAll);     // combiner = łącznik: zsyp dwa kartony (używany przy równoległości)
        show("collect(ArrayList::new, add, addAll)", longWords);
        // WYNIK: collect(ArrayList::new, add, addAll) → [stream, lambda, kolekcja, stream, optional, rekord]

        // Collector.of = własny kolektor z czwartym krokiem: finisher (zaklej karton taśmą)
        Collector<String, List<String>, List<String>> sealedList = Collector.of(
                ArrayList::new,                                           // supplier
                List::add,                                                // accumulator
                (left, right) -> { left.addAll(right); return left; },   // combiner
                Collections::unmodifiableList);                           // finisher = zrób listę tylko do odczytu
        List<String> startsWithL = words.stream().filter(w -> w.startsWith("l")).collect(sealedList);
        show("własny kolektor", startsWithL);
        // WYNIK: własny kolektor → [lambda, lista]
        expectThrows("add do „zaklejonej” listy", () -> startsWithL.add("lody"));
        // WYNIK: ✔ add do „zaklejonej” listy → rzucono UnsupportedOperationException: (brak komunikatu)

        // Dobra wiadomość: gotowe Collectors.xxx() robią dokładnie to samo za Ciebie.
        List<String> same = words.stream().filter(w -> w.startsWith("l")).collect(Collectors.toList());
        show("Collectors.toList() — ten sam efekt", same.equals(startsWithL));
        // WYNIK: Collectors.toList() — ten sam efekt → true

        // PUŁAPKA: combiner nie jest „do ozdoby”. W strumieniu sekwencyjnym zwykle nie jest wołany, więc błąd
        // w nim (np. zwracanie tylko left bez addAll) ujawni się dopiero po .parallel() — t16_streams/Streams18Parallel.
    }

    // =================================================================================================
    // 2. TOLIST, TOSET, TOCOLLECTION
    // =================================================================================================

    /**
     * 2. toList() i toSet() nie obiecują KONKRETNEJ klasy. Gdy potrzebujesz konkretnej kolekcji
     * (TreeSet, ArrayList, ArrayDeque) — toCollection(Konstruktor::new).
     */
    static void toListSetCollection() {
        section("2. toList, toSet, toCollection");

        List<String> words = SampleData.words();

        List<String> list = words.stream().collect(Collectors.toList());    // Collectors.toList = do listy
        show("Collectors.toList()", list);
        // WYNIK: Collectors.toList() → [java, stream, lambda, kolekcja, java, mapa, lista, stream, java, optional, rekord, enum]

        Set<String> set = words.stream().collect(Collectors.toSet());      // toSet = do zbioru (bez duplikatów)
        show("toSet(): ile unikalnych", set.size());
        // WYNIK: toSet(): ile unikalnych → 9
        show("toSet(): zawiera \"mapa\"?", set.contains("mapa"));        // contains = zawiera
        // WYNIK: toSet(): zawiera "mapa"? → true
        // PUŁAPKA: kolejność elementów w toSet() (dziś HashSet) jest NIEOKREŚLONA — nie wypisujemy go,
        // bo wyglądałby na „losowo pomieszany”. Chcesz porządek? Wskaż kolekcję sam:

        TreeSet<String> sorted = words.stream()
                .collect(Collectors.toCollection(TreeSet::new));   // toCollection = do wskazanej kolekcji
        show("toCollection(TreeSet::new)", sorted);
        // WYNIK: toCollection(TreeSet::new) → [enum, java, kolekcja, lambda, lista, mapa, optional, rekord, stream]
        show("sorted.first()", sorted.first());                    // first = pierwszy (metoda TreeSet)
        // WYNIK: sorted.first() → enum
        // Uwaga: TreeSet sortuje wg Unicode — polskie litery (ą, ł, ś) trafią na koniec. Collator → Streams05.

        // PUŁAPKA: Collectors.toList() w dokumentacji NIE gwarantuje typu ani modyfikowalności.
        // Dziś to ArrayList, ale jeśli Twój kod MUSI dostać ArrayList (bo potem dodaje elementy) — powiedz to jawnie:
        ArrayList<String> editable = words.stream().limit(2).collect(Collectors.toCollection(ArrayList::new));
        editable.add("nowe");
        show("toCollection(ArrayList::new) + add", editable);
        // WYNIK: toCollection(ArrayList::new) + add → [java, stream, nowe]

        ArrayDeque<String> queue = words.stream().limit(3)
                .collect(Collectors.toCollection(ArrayDeque::new));   // ArrayDeque = kolejka dwustronna
        show("toCollection(ArrayDeque::new)", queue);
        // WYNIK: toCollection(ArrayDeque::new) → [java, stream, lambda]
        show("pollFirst()", queue.pollFirst());   // pollFirst = zdejmij pierwszy
        // WYNIK: pollFirst() → java
        show("peekLast()", queue.peekLast());     // peekLast = podejrzyj ostatni (bez zdejmowania)
        // WYNIK: peekLast() → lambda

        // DOBRA PRAKTYKA: potrzebujesz tylko listy do odczytu → stream.toList() (Java 16+).
        // Potrzebujesz konkretnej kolekcji (sortowanej, kolejki, modyfikowalnej) → toCollection(X::new).
    }

    // =================================================================================================
    // 3. TOUNMODIFIABLELIST / TOUNMODIFIABLESET
    // =================================================================================================

    /**
     * 3. Kolekcje tylko do odczytu prosto ze strumienia (Java 10+). Krótkie przypomnienie ze Streams06
     * i dwie różnice, o które łatwo się potknąć: null i duplikaty.
     */
    static void unmodifiable() {
        section("3. toUnmodifiableList / toUnmodifiableSet (Java 10+)");

        // Przypomnienie (Streams06):
        //   stream.toList()               (Java 16+) → tylko do odczytu, null DOZWOLONY
        //   Collectors.toList()                      → w praktyce ArrayList, można zmieniać
        //   Collectors.toUnmodifiableList() (Java 10+) → tylko do odczytu, null ZABRONIONY
        List<String> frozen = SampleData.words().stream().limit(2)
                .collect(Collectors.toUnmodifiableList());   // toUnmodifiableList = do listy niemodyfikowalnej
        show("toUnmodifiableList()", frozen);
        // WYNIK: toUnmodifiableList() → [java, stream]
        expectThrows("add do toUnmodifiableList", () -> frozen.add("x"));
        // WYNIK: ✔ add do toUnmodifiableList → rzucono UnsupportedOperationException: (brak komunikatu)

        expectThrows("null w toUnmodifiableList", () -> Stream.of("a", null).collect(Collectors.toUnmodifiableList()));
        // WYNIK: ✔ null w toUnmodifiableList → rzucono NullPointerException: (brak komunikatu)
        show("null w stream.toList()", Stream.of("a", null).toList());
        // WYNIK: null w stream.toList() → [a, null]

        // toUnmodifiableSet (Java 10+) po cichu USUWA duplikaty...
        Set<String> frozenSet = SampleData.words().stream().collect(Collectors.toUnmodifiableSet());
        show("toUnmodifiableSet(): rozmiar", frozenSet.size());
        // WYNIK: toUnmodifiableSet(): rozmiar → 9
        // PUŁAPKA: ...a Set.of (Java 9+) z duplikatem RZUCA wyjątek. Podobne nazwy, inne zachowanie!
        expectThrows("Set.of z duplikatem", () -> Set.of("java", "java"));
        // WYNIK: ✔ Set.of z duplikatem → rzucono IllegalArgumentException: duplicate element: java

        // DOBRA PRAKTYKA: zwracasz wynik z metody publicznej? Niech będzie niemodyfikowalny (toList() albo
        // toUnmodifiableList()), żeby nikt z zewnątrz nie popsuł Ci danych. Więcej: t12_collections/Collections08ImmutableUnmodifiable.
    }

    // =================================================================================================
    // 4. JOINING — TRZY WARIANTY
    // =================================================================================================

    /**
     * 4. joining() skleja TEKSTY w jeden String: bez separatora, z separatorem, z separatorem
     * i nawiasami (prefiks + sufiks). Działa tylko na strumieniu tekstów (CharSequence).
     */
    static void joining() {
        section("4. joining — sklejanie napisów");

        List<String> books = SampleData.products().stream()
                .filter(p -> p.category() == Category.KSIAZKI)
                .map(Product::name)                  // najpierw Product → String!
                .toList();

        show("joining()", books.stream().collect(Collectors.joining()));
        // WYNIK: joining() → Czysty kodJava. PodstawyWzorce projektowe
        show("joining(\", \")", books.stream().collect(Collectors.joining(", ")));   // separator = separator (przecinek)
        // WYNIK: joining(", ") → Czysty kod, Java. Podstawy, Wzorce projektowe
        show("joining(\", \", \"Książki: [\", \"]\")",
                books.stream().collect(Collectors.joining(", ", "Książki: [", "]")));  // prefix = przedrostek; suffix = przyrostek
        // WYNIK: joining(", ", "Książki: [", "]") → Książki: [Czysty kod, Java. Podstawy, Wzorce projektowe]

        // Pusty strumień: prefiks i sufiks i tak są dodane.
        show("pusty z nawiasami", Stream.<String>empty().collect(Collectors.joining(", ", "[", "]")));
        // WYNIK: pusty z nawiasami → []

        // PUŁAPKA: joining działa tylko na tekstach. To się nie skompiluje:
        //     String s = SampleData.products().stream().collect(Collectors.joining(", "));
        //     → error: no suitable method found ... Product cannot be converted to CharSequence
        // Rozwiązanie: najpierw map(Product::name) albo map(String::valueOf) (użyje toString()).
        show("liczby przez map(String::valueOf)", Stream.of(1, 2, 3)
                .map(String::valueOf)                // valueOf = wartość jako tekst
                .collect(Collectors.joining(" + ")));
        // WYNIK: liczby przez map(String::valueOf) → 1 + 2 + 3

        // DOBRA PRAKTYKA: masz JUŻ gotową listę Stringów? String.join(", ", lista) jest krótsze niż strumień.
        show("String.join", String.join(" | ", books));   // join = połącz
        // WYNIK: String.join → Czysty kod | Java. Podstawy | Wzorce projektowe
    }

    // =================================================================================================
    // 5. COUNTING I SUMMING
    // =================================================================================================

    /**
     * 5. counting() zwraca Long (obiekt!), summingInt → Integer, summingLong → Long, summingDouble → Double.
     * Pusty strumień → 0, nie wyjątek.
     */
    static void countingAndSumming() {
        section("5. counting, summingInt / summingLong / summingDouble");

        List<Employee> employees = SampleData.employees();
        List<Product> products = SampleData.products();

        Long count = employees.stream().collect(Collectors.counting());   // counting = liczenie
        show("counting()", count);
        // WYNIK: counting() → 10
        // PUŁAPKA: counting() daje Long, więc to się NIE skompiluje:
        //     int n = employees.stream().collect(Collectors.counting());
        //     → error: incompatible types: inferred type does not conform to upper bound(s)  (Long ≠ int)
        // Poprawnie: long n = ... albo po prostu employees.stream().count().

        Integer salaries = employees.stream().collect(Collectors.summingInt(Employee::salary));   // summingInt = sumuj int
        show("summingInt(Employee::salary)", salaries);
        // WYNIK: summingInt(Employee::salary) → 106400

        Long stock = products.stream().collect(Collectors.summingLong(Product::stock));           // summingLong = sumuj long
        show("summingLong(Product::stock)", stock);
        // WYNIK: summingLong(Product::stock) → 595

        Double pricesAsDouble = products.stream()
                .collect(Collectors.summingDouble(p -> p.price().doubleValue()));                // summingDouble = sumuj double
        show("summingDouble(cena jako double)", pricesAsDouble);
        // WYNIK: summingDouble(cena jako double) → 13106.36
        // PUŁAPKA: że tym razem wyszło „ładnie”, to szczęście — double nie przechowuje dokładnie groszy
        // (0.1 + 0.2 = 0.30000000000000004). Pieniądze sumuj w BigDecimal: t16_streams/Streams15BigDecimalMoney.

        Integer emptySum = Stream.<Employee>empty().collect(Collectors.summingInt(Employee::salary));
        show("summingInt pustego", emptySum);
        // WYNIK: summingInt pustego → 0
    }

    // =================================================================================================
    // 6. AVERAGING — PUSTO → 0.0 (A NIE OPTIONAL)
    // =================================================================================================

    /**
     * 6. averagingInt / averagingDouble zwracają Double. Dla pustego strumienia → 0.0 (bez ostrzeżenia!),
     * podczas gdy IntStream.average() zwraca OptionalDouble.empty.
     */
    static void averaging() {
        section("6. averagingInt / averagingDouble");

        List<Employee> employees = SampleData.employees();

        Double avgSalary = employees.stream().collect(Collectors.averagingInt(Employee::salary));   // averagingInt = średnia z int
        show("averagingInt(Employee::salary)", avgSalary);
        // WYNIK: averagingInt(Employee::salary) → 10640.0

        Double avgGrades = SampleData.students().stream()
                .filter(s -> !s.grades().isEmpty())                                   // Henryk nie ma ocen — pomijamy
                .collect(Collectors.averagingDouble(s -> s.averageGrade().orElseThrow())); // averagingDouble = średnia z double
        show("średnia średnich studentów", String.format(Locale.ROOT, "%.2f", avgGrades));
        // WYNIK: średnia średnich studentów → 3.86

        // PUŁAPKA: pusty strumień. Kolektor mówi „0.0”, IntStream mówi „nie ma średniej”.
        Double logisticsCollector = employees.stream()
                .filter(e -> e.department() == Department.LOGISTYKA)
                .collect(Collectors.averagingInt(Employee::salary));
        show("LOGISTYKA: averagingInt", logisticsCollector);
        // WYNIK: LOGISTYKA: averagingInt → 0.0    ← wygląda jak „średnio zarabiają 0 zł”!
        OptionalDouble logisticsStream = employees.stream()
                .filter(e -> e.department() == Department.LOGISTYKA)
                .mapToInt(Employee::salary)
                .average();
        show("LOGISTYKA: mapToInt().average()", logisticsStream);
        // WYNIK: LOGISTYKA: mapToInt().average() → OptionalDouble.empty

        // DOBRA PRAKTYKA: jeśli „brak danych” to co innego niż „zero” — użyj IntStream.average().
        // averagingInt jest wygodny głównie jako downstream w groupingBy (tam grupy nigdy nie są puste — Streams11).
    }

    // =================================================================================================
    // 7. MINBY / MAXBY — WYNIK W OPTIONAL
    // =================================================================================================

    /**
     * 7. minBy / maxBy zwracają {@code Optional<T>}, bo strumień może być pusty. Przy remisie wygrywa
     * PIERWSZY napotkany element.
     */
    static void minByMaxBy() {
        section("7. minBy / maxBy");

        List<Employee> employees = SampleData.employees();

        Optional<Employee> richest = employees.stream()
                .collect(Collectors.maxBy(Comparator.comparingInt(Employee::salary)));   // maxBy = największy według
        show("maxBy(pensja)", richest);
        // WYNIK: maxBy(pensja) → Optional[Michał Lewandowski (IT, 17200 zł)]

        Optional<Employee> youngest = employees.stream()
                .collect(Collectors.minBy(Comparator.comparingInt(Employee::age)));      // minBy = najmniejszy według
        show("minBy(wiek)", youngest.map(Employee::name).orElse("nikt"));
        // WYNIK: minBy(wiek) → Piotr Kowalski

        // Remis: Piotr Kowalski i Paweł Dąbrowski zarabiają po 9800. Wygrywa pierwszy na liście.
        Optional<Employee> richestBelow10k = employees.stream()
                .filter(e -> e.salary() < 10_000)
                .collect(Collectors.maxBy(Comparator.comparingInt(Employee::salary)));
        show("maxBy wśród < 10 000", richestBelow10k);
        // WYNIK: maxBy wśród < 10 000 → Optional[Piotr Kowalski (IT, 9800 zł)]

        Optional<Employee> nobody = Stream.<Employee>empty()
                .collect(Collectors.maxBy(Comparator.comparingInt(Employee::salary)));
        show("maxBy pustego", nobody);
        // WYNIK: maxBy pustego → Optional.empty

        // DOBRA PRAKTYKA: na „najwyższym poziomie” pisz po prostu stream.max(comparator) — to samo, krócej.
        // maxBy/minBy przydają się jako downstream: „najlepiej zarabiający W KAŻDYM dziale” (Streams11).
    }

    // =================================================================================================
    // 8. SUMMARIZINGINT — STATYSTYKI JAKO KOLEKTOR
    // =================================================================================================

    /**
     * 8. summarizingInt to kolektorowa wersja mapToInt(...).summaryStatistics() ze Streams08.
     */
    static void summarizing() {
        section("8. summarizingInt");

        IntSummaryStatistics ages = SampleData.employees().stream()
                .collect(Collectors.summarizingInt(Employee::age));   // summarizingInt = podsumuj int (statystyki)
        show("liczba", ages.getCount());
        // WYNIK: liczba → 10
        show("najmłodszy / najstarszy", ages.getMin() + " / " + ages.getMax());
        // WYNIK: najmłodszy / najstarszy → 27 / 50
        show("średni wiek", ages.getAverage());
        // WYNIK: średni wiek → 37.6

        IntSummaryStatistics same = SampleData.employees().stream().mapToInt(Employee::age).summaryStatistics();
        show("to samo co mapToInt().summaryStatistics()?", same.getSum() == ages.getSum());
        // WYNIK: to samo co mapToInt().summaryStatistics()? → true
        // PUŁAPKA: toString() statystyk zależy od ustawień regionalnych — patrz Streams08, sekcja 5.
    }

    // =================================================================================================
    // 9. OPERACJE STRUMIENIA KONTRA KOLEKTORY
    // =================================================================================================

    /**
     * 9. Prawie każdy prosty kolektor ma krótszy odpowiednik w samym strumieniu. Kolektory naprawdę
     * błyszczą dopiero jako downstream — w groupingBy (zapowiedź Streams11).
     */
    static void streamOpsVsCollectors() {
        section("9. Operacje strumienia vs kolektory");

        //   kolektor                          | krótszy odpowiednik
        //   collect(counting())               | count()
        //   collect(summingInt(f))            | mapToInt(f).sum()
        //   collect(averagingInt(f))          | mapToInt(f).average()      (uwaga: Optional!)
        //   collect(maxBy(c)) / minBy(c)      | max(c) / min(c)
        //   collect(summarizingInt(f))        | mapToInt(f).summaryStatistics()
        //   collect(toList())                 | toList()                   (Java 16+)
        List<Employee> employees = SampleData.employees();
        show("count() == counting()", employees.stream().count() == employees.stream().collect(Collectors.counting()));
        // WYNIK: count() == counting() → true

        // Zapowiedź Streams11: groupingBy dzieli elementy na grupy, a KOLEKTOR PODRZĘDNY (downstream)
        // mówi, co zrobić z każdą grupą. Tu counting() i summingInt() naprawdę się przydają!
        // TreeMap::new — bo klucze to enum, a HashMap z kluczami enum wypisuje się w losowej kolejności.
        Map<Department, Long> peoplePerDepartment = employees.stream()
                .collect(Collectors.groupingBy(Employee::department, TreeMap::new, Collectors.counting()));
        show("liczba osób w działach", peoplePerDepartment);
        // WYNIK: liczba osób w działach → {IT=4, HR=1, SPRZEDAZ=2, KSIEGOWOSC=1, MARKETING=2}

        Map<Department, Integer> salaryPerDepartment = employees.stream()
                .collect(Collectors.groupingBy(Employee::department, TreeMap::new, Collectors.summingInt(Employee::salary)));
        show("suma pensji w działach", salaryPerDepartment);
        // WYNIK: suma pensji w działach → {IT=53600, HR=7200, SPRZEDAZ=20100, KSIEGOWOSC=8100, MARKETING=17400}

        // DOBRA PRAKTYKA: na najwyższym poziomie wybieraj operacje strumienia (count, sum, max, toList) —
        // są krótsze i działają na typach prostych. Kolektory zostaw na „wnętrze” groupingBy / partitioningBy.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • collect(Collector) — Collector = supplier (pusty pojemnik) + accumulator (włóż) + combiner (połącz)
     *     + finisher (wykończ). Własny: Collector.of(...); zwykle wystarczą gotowe Collectors.xxx().
     *   • toList() / toSet() — bez gwarancji typu; toCollection(TreeSet::new / ArrayList::new / ArrayDeque::new) —
     *     dokładnie ta kolekcja. Kolejność w toSet() jest nieokreślona.
     *   • toUnmodifiableList / Set (Java 10+) — tylko do odczytu, bez null; toUnmodifiableSet usuwa duplikaty,
     *     a Set.of z duplikatem rzuca IllegalArgumentException.
     *   • joining() / joining(sep) / joining(sep, prefiks, sufiks) — tylko dla tekstów (najpierw map).
     *   • counting() → Long; summingInt → Integer; summingDouble → Double; pusty → 0.
     *   • averagingInt → Double, pusty → 0.0 (!) — a IntStream.average() → OptionalDouble.empty.
     *   • minBy / maxBy → Optional; przy remisie wygrywa pierwszy.
     *   • summarizingInt = mapToInt(...).summaryStatistics() w wersji kolektora.
     *   • Na górze strumienia: count / sum / max / toList. Kolektory — jako downstream w groupingBy (Streams11).
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie cztery funkcje tworzą Collector i co robi każda z nich?
     *   2. Co wypisze:  System.out.println(Stream.of("a", "b", "c").collect(Collectors.joining("-", "<", ">")));  ?
     *   3. Co wypisze:  System.out.println(Stream.<Integer>empty().collect(Collectors.averagingInt(i -> i)));  ?
     *   4. ZNAJDŹ BŁĄD:  int n = employees.stream().collect(Collectors.counting());
     *   5. ZNAJDŹ BŁĄD:  String s = products.stream().collect(Collectors.joining(", "));
     *   6. Czym różni się collect(Collectors.toSet()) od collect(Collectors.toCollection(TreeSet::new))?
     *   7. Co wypisze:  System.out.println(Stream.of("b", "a", "b").collect(Collectors.toUnmodifiableSet()).size());
     *      a co zrobi:  Set.of("b", "a", "b")  ?
     *   8. Kiedy napiszesz collect(Collectors.counting()), a kiedy count()?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: nazwy produktów DOM przez \" | \"", "Ekspres do kawy | Lampka biurkowa",
                () -> exercise1(SampleData.products()));
        Check.equal("ćw. 2: liczba klientów VIP", 2L, () -> exercise2(SampleData.customers()));
        Check.equal("ćw. 3: posortowane miasta bez powtórzeń", expectedCities(),
                () -> exercise3(SampleData.customers()));
        Check.equal("ćw. 4: średnia pensja w MARKETING", 8700.0,
                () -> exercise4(SampleData.employees(), Department.MARKETING));
        Check.equal("ćw. 5a: raport działu IT", "IT: 4 os., suma 53600, średnio 13400.00, od 9800 do 17200",
                () -> exercise5(SampleData.employees(), Department.IT));
        Check.equal("ćw. 5b: raport działu LOGISTYKA", "LOGISTYKA: brak pracowników",
                () -> exercise5(SampleData.employees(), Department.LOGISTYKA));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "Ekspres do kawy | Lampka biurkowa", () -> solution1(SampleData.products()));
        Check.equal("ćw. 2 (wzorzec)", 2L, () -> solution2(SampleData.customers()));
        Check.equal("ćw. 3 (wzorzec)", expectedCities(), () -> solution3(SampleData.customers()));
        Check.equal("ćw. 4 (wzorzec)", 8700.0, () -> solution4(SampleData.employees(), Department.MARKETING));
        Check.equal("ćw. 5a (wzorzec)", "IT: 4 os., suma 53600, średnio 13400.00, od 9800 do 17200",
                () -> solution5(SampleData.employees(), Department.IT));
        Check.equal("ćw. 5b (wzorzec)", "LOGISTYKA: brak pracowników",
                () -> solution5(SampleData.employees(), Department.LOGISTYKA));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    private static TreeSet<String> expectedCities() {
        return new TreeSet<>(List.of("Gdańsk", "Kraków", "Poznań", "Warszawa", "Wrocław"));
    }

    /**
     * ĆWICZENIE 1 (łatwe): sklej nazwy produktów z kategorii DOM separatorem " | ".
     * Podpowiedź: filter → map(Product::name) → collect(Collectors.joining(" | ")).
     */
    static String exercise1(List<Product> products) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (łatwe): policz klientów VIP kolektorem counting() (zwróć long).
     * Podpowiedź: filter(Customer::vip) → collect(Collectors.counting()); Long rozpakuje się do long.
     */
    static long exercise2(List<Customer> customers) {
        // TODO: twoje rozwiązanie
        return 0L;
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć miasta klientów bez powtórzeń, posortowane alfabetycznie, jako TreeSet.
     * Podpowiedź: map(Customer::city) → collect(Collectors.toCollection(TreeSet::new)).
     */
    static TreeSet<String> exercise3(List<Customer> customers) {
        // TODO: twoje rozwiązanie
        return new TreeSet<>();
    }

    /**
     * ĆWICZENIE 4 (średnie): PRZEPISZ pętlę na jeden strumień z kolektorem averagingInt.
     * <pre>{@code
     * int sum = 0;
     * int count = 0;
     * for (Employee e : employees) {
     *     if (e.department() == department) {
     *         sum += e.salary();
     *         count++;
     *     }
     * }
     * return count == 0 ? 0.0 : (double) sum / count;
     * }</pre>
     * Podpowiedź: averagingInt dla pustej grupy sam zwróci 0.0 — dokładnie jak ta pętla.
     */
    static double exercise4(List<Employee> employees, Department department) {
        // TODO: twoje rozwiązanie
        return 0.0;
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): raport działu w JEDNYM przejściu po danych, np.
     * "IT: 4 os., suma 53600, średnio 13400.00, od 9800 do 17200". Gdy w dziale nikogo nie ma:
     * "LOGISTYKA: brak pracowników" (nie pokazuj min = 2147483647!).
     * Podpowiedź: filter → collect(Collectors.summarizingInt(Employee::salary)); sprawdź getCount() == 0;
     * średnią formatuj String.format(Locale.ROOT, "%.2f", ...).
     */
    static String exercise5(List<Employee> employees, Department department) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(List<Product> products) {
        return products.stream()
                .filter(p -> p.category() == Category.DOM)
                .map(Product::name)
                .collect(Collectors.joining(" | "));
    }

    static long solution2(List<Customer> customers) {
        return customers.stream()
                .filter(Customer::vip)
                .collect(Collectors.counting());
    }

    static TreeSet<String> solution3(List<Customer> customers) {
        return customers.stream()
                .map(Customer::city)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    static double solution4(List<Employee> employees, Department department) {
        return employees.stream()
                .filter(e -> e.department() == department)
                .collect(Collectors.averagingInt(Employee::salary));
    }

    static String solution5(List<Employee> employees, Department department) {
        IntSummaryStatistics stats = employees.stream()
                .filter(e -> e.department() == department)
                .collect(Collectors.summarizingInt(Employee::salary));
        if (stats.getCount() == 0) {
            return department + ": brak pracowników";
        }
        return String.format(Locale.ROOT, "%s: %d os., suma %d, średnio %.2f, od %d do %d",
                department, stats.getCount(), stats.getSum(), stats.getAverage(), stats.getMin(), stats.getMax());
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. supplier — tworzy pusty pojemnik; accumulator — wkłada jeden element do pojemnika; combiner — łączy
     *      dwa pojemniki (gdy pracowało kilka wątków); finisher — zamienia pojemnik na wynik końcowy
     *      (np. robi listę niemodyfikowalną albo String ze StringBuildera).
     *   2. <a-b-c>
     *   3. 0.0 — averagingInt dla pustego strumienia zwraca 0.0, a nie Optional (w przeciwieństwie do IntStream.average()).
     *   4. counting() zwraca Long, którego nie da się przypisać do int → błąd kompilacji. Użyj long n = ...
     *      albo po prostu employees.stream().count().
     *   5. joining działa tylko na strumieniu tekstów (CharSequence), a tu jest Stream<Product> → błąd kompilacji.
     *      Poprawnie: products.stream().map(Product::name).collect(Collectors.joining(", ")).
     *   6. toSet() daje „jakiś” Set (dziś HashSet) o nieokreślonej kolejności; toCollection(TreeSet::new) daje
     *      na pewno TreeSet — posortowany i z metodami first(), last(), headSet()...
     *   7. 2 — toUnmodifiableSet po cichu usuwa duplikaty. Set.of("b", "a", "b") rzuca IllegalArgumentException
     *      (duplicate element: b).
     *   8. Na najwyższym poziomie — count() (krócej, zwraca long). counting() — jako downstream,
     *      np. groupingBy(Employee::department, counting()).
     */
    // </editor-fold>
}
