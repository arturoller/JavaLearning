package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Order;
import helpers.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Zaawansowane kolektory — collectingAndThen, filtering, flatMapping, teeing, Collector.of
 *        (advanced collectors = zaawansowane kolektory; collector = kolektor, „przepis na zbieranie”)
 *
 * W SKRÓCIE:
 *   Kolektor mówi strumieniowi, JAK zebrać elementy w jeden wynik. Znasz już toList, toMap,
 *   groupingBy i partitioningBy. Tu dochodzą nowe „klocki”: poprawka wyniku na końcu, filtr
 *   i flatMap WEWNĄTRZ grupy, dwa kolektory naraz oraz własny kolektor napisany od zera.
 *
 * ANALOGIA: Kolektor to pakowanie zakupów przy kasie.
 *   supplier = weź puste pudełko; accumulator = włóż do niego jeden produkt;
 *   combiner = zsyp dwa pudełka w jedno (gdy pakuje kilku kasjerów naraz);
 *   finisher = zaklej pudełko i przyklej etykietę. Tyle — każdy kolektor to te 4 czynności.
 *
 * JAK TO DZIAŁA:
 *   {@code Collector<T, A, R>}:  T = typ elementu, A = typ „pudełka” (akumulatora), R = typ wyniku.
 *     supplier()     → A              raz na start (w parallel: raz na każdy kawałek danych)
 *     accumulator()  → (A, T) → void  raz dla KAŻDEGO elementu
 *     combiner()     → (A, A) → A     tylko w parallel — skleja wyniki kawałków
 *     finisher()     → A → R          raz na końcu (albo wcale, gdy cecha IDENTITY_FINISH)
 *
 *   Kolektory do składania (downstream = kolektor podrzędny, działa WEWNĄTRZ grupy):
 *     collectingAndThen(k, f)   — zbierz kolektorem k, potem przekształć wynik funkcją f
 *     filtering(p, k)    (9+)   — do k trafiają tylko elementy spełniające p; grupa ZOSTAJE
 *     flatMapping(f, k)  (9+)   — element → strumień, spłaszcz i wrzuć do k
 *     teeing(k1, k2, m)  (12+)  — każdy element idzie do k1 i k2; m łączy oba wyniki
 *     reducing(id, f, op)       — przekształć funkcją f, zwiń operacją op, startując od id
 *
 * SŁÓWKA:
 *   collect = zbierz; collector = kolektor; downstream = podrzędny (dalej w dół potoku);
 *   collecting and then = zbierz, a potem; filtering = filtrowanie; flat mapping = mapowanie ze spłaszczaniem;
 *   tee = trójnik (rozgałęzienie rury w kształcie litery T); merger = łącznik wyników;
 *   supplier = dostawca; accumulator = akumulator (zbieracz); combiner = sklejacz; finisher = wykańczacz;
 *   characteristics = cechy; identity finish = wykończenie „bez zmian”; unordered = bez kolejności;
 *   concurrent = współbieżny; heap = kopiec (kolejka priorytetowa)
 *
 * ZOBACZ TEŻ: t16_streams/Streams11GroupingBy (downstream: counting, mapping, maxBy, reducing),
 *   t16_streams/Streams12PartitioningBy (podział true/false), t16_streams/Streams14OptionalInStreams
 *   (Optional w mapach z maxBy), t16_streams/Streams18Parallel (kiedy naprawdę działa combiner)
 * </pre>
 */
public class Streams13AdvancedCollectors {

    /** Stała BigDecimal poza lambdą — tworzona raz, a nie przy każdym elemencie. */
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public static void main(String[] args) {
        title("Streams13 — zaawansowane kolektory");

        recap();                     // recap = powtórka
        collectingAndThenDemo();     // collecting and then = zbierz, a potem
        filteringDemo();             // filtering = filtrowanie
        flatMappingDemo();           // flat mapping = mapowanie ze spłaszczaniem
        teeingDemo();                // teeing = rozgałęzienie (trójnik „T”)
        reducingDemo();              // reducing = redukowanie (zwijanie)
        customCollectorAnatomy();    // custom collector anatomy = budowa własnego kolektora
        practicalCustomCollector();  // practical = praktyczny
        characteristicsDemo();       // characteristics = cechy
        exercises();                 // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. POWTÓRKA — groupingBy + kolektor podrzędny
    // =================================================================================================

    /**
     * 1. Szybka powtórka z Streams11. Wszystko, co dziś poznasz, wkładasz w to samo miejsce,
     * w które wkładałeś {@code counting()} albo {@code mapping(...)} — jako trzeci argument groupingBy.
     */
    static void recap() {
        section("1. Powtórka — groupingBy + kolektor podrzędny");

        // groupingBy = grupuj według; TreeMap::new = fabryka mapy posortowanej; counting = liczenie.
        // TreeMap dla kluczy enum → kolejność deklaracji (IT, HR, SPRZEDAZ...) — zawsze taka sama.
        Map<Department, Long> countByDept = SampleData.employees().stream()
                .collect(Collectors.groupingBy(Employee::department, TreeMap::new, Collectors.counting()));
        show("liczba osób w dziale", countByDept);
        // WYNIK: liczba osób w dziale → {IT=4, HR=1, SPRZEDAZ=2, KSIEGOWOSC=1, MARKETING=2}

        note("LOGISTYKA nie ma pracowników → nie ma jej w mapie (grupa powstaje przy 1. elemencie)");
        // WYNIK: ℹ LOGISTYKA nie ma pracowników → nie ma jej w mapie (grupa powstaje przy 1. elemencie)

        // Schemat na całą lekcję:  groupingBy( KLUCZ , FABRYKA_MAPY , KOLEKTOR_PODRZĘDNY )
        //   — w miejsce KOLEKTOR_PODRZĘDNY wkładasz filtering, flatMapping, teeing, własny kolektor...
    }

    // =================================================================================================
    // 2. collectingAndThen — zbierz, a potem popraw wynik
    // =================================================================================================

    /**
     * 2. {@code collectingAndThen(kolektor, funkcja)} najpierw zbiera, a potem JEDEN raz przekształca
     * gotowy wynik. Najważniejsze zastosowanie: rozpakowanie Optional z maxBy wewnątrz groupingBy.
     */
    static void collectingAndThenDemo() {
        section("2. collectingAndThen — zbierz, a potem popraw wynik");

        // PRZED (styl Java 8): zbierz do zwykłej listy, potem „zamroź” ją.
        // unmodifiableList = lista niemodyfikowalna (tylko do odczytu)
        List<String> cheapNames = SampleData.products().stream()
                .filter(p -> p.price().compareTo(HUNDRED) < 0)   // compareTo = porównaj (dla BigDecimal)
                .map(Product::name)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        show("tanie produkty (< 100 zł)", cheapNames);
        // WYNIK: tanie produkty (< 100 zł) → [Kawa ziarnista 1kg, Czekolada gorzka, Oliwa z oliwek, Czysty kod, Wzorce projektowe, T-shirt bawełniany]

        expectThrows("dodanie do listy niemodyfikowalnej", () -> cheapNames.add("Guma do żucia"));
        // WYNIK: ✔ dodanie do listy niemodyfikowalnej → rzucono UnsupportedOperationException: (brak komunikatu)

        // PO: na końcu potoku wystarczy .toList() (Java 16+). collectingAndThen jest potrzebny tam,
        // gdzie NIE MA „końca potoku” — czyli wewnątrz groupingBy/partitioningBy.

        // Najważniejszy przykład: maxBy zwraca Optional, więc mapa ma „opakowane” wartości.
        // maxBy = największy według; comparing = porównuj według
        Map<Category, Optional<Product>> wrapped = SampleData.products().stream()
                .collect(Collectors.groupingBy(Product::category, TreeMap::new,
                        Collectors.maxBy(Comparator.comparing(Product::price))));
        show("najdroższy w DOM (z Optional)", wrapped.get(Category.DOM));
        // WYNIK: najdroższy w DOM (z Optional) → Optional[Ekspres do kawy (1899.00 zł)]

        Map<Category, Product> mostExpensive = SampleData.products().stream()
                .collect(Collectors.groupingBy(Product::category, TreeMap::new,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(Product::price)),
                                Optional::get)));   // get = pobierz — TUTAJ bezpieczne, patrz niżej
        showEach("najdroższy produkt w kategorii", mostExpensive);
        // WYNIK: najdroższy produkt w kategorii (liczba kluczy: 5):
        // WYNIK: • ELEKTRONIKA → Laptop Pro 14 (5499.99 zł)
        // WYNIK: • SPOZYWCZE → Kawa ziarnista 1kg (64.99 zł)
        // WYNIK: • KSIAZKI → Java. Podstawy (129.00 zł)
        // WYNIK: • ODZIEZ → Kurtka zimowa (459.00 zł)
        // WYNIK: • DOM → Ekspres do kawy (1899.00 zł)

        // DLACZEGO get() jest tu bezpieczne? groupingBy tworzy grupę dopiero wtedy, gdy trafi do niej
        // PIERWSZY element. Pustych grup nie ma → maxBy w każdej grupie widzi ≥ 1 element → Optional
        // nigdy nie jest pusty. To jedyne miejsce, gdzie „gołe” Optional::get jest w porządku.
        // PUŁAPKA: ta gwarancja znika, gdy dołożysz filtering (sekcja 3) — tam grupa MOŻE być pusta!

        // DOBRA PRAKTYKA: alternatywa bez Optional — toMap z BinaryOperator.maxBy (Streams14).
    }

    // =================================================================================================
    // 3. filtering (Java 9+) kontra filter przed groupingBy
    // =================================================================================================

    /**
     * 3. {@code filter} przed grupowaniem wyrzuca elementy ZANIM powstaną grupy, więc grupy bez
     * pasujących elementów znikają. {@code filtering} filtruje WEWNĄTRZ grupy — grupa zostaje (np. z zerem).
     */
    static void filteringDemo() {
        section("3. filtering (Java 9+) kontra filter przed groupingBy");

        // Pytanie z raportu: ilu pracowników w każdym dziale zarabia powyżej 10 000 zł?
        // A) filter PRZED grupowaniem — działy bez takich osób ZNIKAJĄ z raportu.
        Map<Department, Long> filterFirst = SampleData.employees().stream()
                .filter(e -> e.salary() > 10_000)
                .collect(Collectors.groupingBy(Employee::department, TreeMap::new, Collectors.counting()));
        show("filter → groupingBy", filterFirst);
        // WYNIK: filter → groupingBy → {IT=3, SPRZEDAZ=1}

        // B) filtering (Java 9+) WEWNĄTRZ grupy — dział zostaje, nawet jeśli wynik to 0.
        Map<Department, Long> filteringInside = SampleData.employees().stream()
                .collect(Collectors.groupingBy(Employee::department, TreeMap::new,
                        Collectors.filtering(e -> e.salary() > 10_000, Collectors.counting())));
        show("groupingBy(filtering)", filteringInside);
        // WYNIK: groupingBy(filtering) → {IT=3, HR=0, SPRZEDAZ=1, KSIEGOWOSC=0, MARKETING=0}

        // JAK TO ZAPAMIĘTAĆ: filter = bramkarz PRZED wejściem do sali (kto nie wejdzie, nie ma go na liście);
        // filtering = bramkarz przy KAŻDYM stoliku (stolik stoi, najwyżej pusty).

        // PUŁAPKA: filtering + maxBy + Optional::get. Grupa HR istnieje (Katarzyna do niej trafiła),
        // ale po filtrze jest pusta → maxBy daje Optional.empty → get() rzuca wyjątek.
        expectThrows("filtering + maxBy + Optional::get", () -> SampleData.employees().stream()
                .collect(Collectors.groupingBy(Employee::department, TreeMap::new,
                        Collectors.filtering(e -> e.salary() > 10_000,
                                Collectors.collectingAndThen(
                                        Collectors.maxBy(Comparator.comparingInt(Employee::salary)),
                                        Optional::get)))));
        // WYNIK: ✔ filtering + maxBy + Optional::get → rzucono NoSuchElementException: No value present

        // Poprawka: zamiast get() — map + orElse (czytelny zamiennik dla pustej grupy).
        // comparingInt = porównuj według int; orElse = albo (wartość zastępcza)
        Map<Department, String> topEarner = SampleData.employees().stream()
                .collect(Collectors.groupingBy(Employee::department, TreeMap::new,
                        Collectors.filtering(e -> e.salary() > 10_000,
                                Collectors.collectingAndThen(
                                        Collectors.maxBy(Comparator.comparingInt(Employee::salary)),
                                        opt -> opt.map(Employee::name).orElse("(nikt)")))));
        showEach("najlepiej zarabiający powyżej 10 000 zł", topEarner);
        // WYNIK: najlepiej zarabiający powyżej 10 000 zł (liczba kluczy: 5):
        // WYNIK: • IT → Michał Lewandowski
        // WYNIK: • HR → (nikt)
        // WYNIK: • SPRZEDAZ → Krzysztof Szymański
        // WYNIK: • KSIEGOWOSC → (nikt)
        // WYNIK: • MARKETING → (nikt)

        // LOGISTYKA wciąż jej nie ma — nie ma w niej NIKOGO, więc grupa nigdy nie powstała.
        // Chcesz w raporcie WSZYSTKIE działy? Przejdź po Department.values() i dociągnij wartości z mapy.
        // EnumMap = mapa dla kluczy enum (kolejność deklaracji, bardzo szybka); getOrDefault = pobierz albo domyślna
        Map<Department, Long> allDepts = Arrays.stream(Department.values())
                .collect(Collectors.toMap(d -> d, d -> filteringInside.getOrDefault(d, 0L),
                        (a, b) -> a, () -> new EnumMap<>(Department.class)));
        show("wszystkie działy", allDepts);
        // WYNIK: wszystkie działy → {IT=3, HR=0, SPRZEDAZ=1, KSIEGOWOSC=0, MARKETING=0, LOGISTYKA=0}

        // DOBRA PRAKTYKA: raport „dla każdej grupy” → filtering. Interesują Cię tylko grupy z trafieniami → filter.
    }

    // =================================================================================================
    // 4. flatMapping (Java 9+) — umiejętności w dziale
    // =================================================================================================

    /**
     * 4. {@code flatMapping(f, k)} to flatMap działający WEWNĄTRZ grupy. Klucz bierzemy z pracownika,
     * a do kolektora podrzędnego trafiają już pojedyncze umiejętności (a nie listy list).
     */
    static void flatMappingDemo() {
        section("4. flatMapping (Java 9+) — umiejętności w dziale");

        // PRZED: mapping(Employee::skills, ...) → lista LIST (mapping = mapowanie). Działa, ale to „pudełko w pudełku”.
        Map<Department, List<List<String>>> nested = SampleData.employees().stream()
                .collect(Collectors.groupingBy(Employee::department, TreeMap::new,
                        Collectors.mapping(Employee::skills, Collectors.toList())));
        show("SPRZEDAZ przez mapping", nested.get(Department.SPRZEDAZ));
        // WYNIK: SPRZEDAZ przez mapping → [[Negocjacje, Excel, CRM], [Negocjacje, CRM]]

        // Dlaczego nie zwykły flatMap PRZED groupingBy? Bo po spłaszczeniu zostają same słowa
        // ("Java", "SQL"...) i ginie informacja, z jakiego działu pochodzą. flatMapping spłaszcza
        // dopiero w środku grupy — klucz (dział) jest już ustalony.

        // PO: flatMapping + toCollection(TreeSet::new) → bez powtórek i alfabetycznie.
        // toCollection = do kolekcji (podanej fabryką); TreeSet = zbiór posortowany
        Map<Department, Set<String>> skills = SampleData.employees().stream()
                .collect(Collectors.groupingBy(Employee::department, TreeMap::new,
                        Collectors.flatMapping(e -> e.skills().stream(),
                                Collectors.toCollection(TreeSet::new))));
        showEach("umiejętności w dziale", skills);
        // WYNIK: umiejętności w dziale (liczba kluczy: 5):
        // WYNIK: • IT → [AWS, Docker, Java, Kotlin, Python, SQL, Spring]
        // WYNIK: • HR → [Excel, Rekrutacja]
        // WYNIK: • SPRZEDAZ → [CRM, Excel, Negocjacje]
        // WYNIK: • KSIEGOWOSC → [Excel, SAP]
        // WYNIK: • MARKETING → [Canva, Excel, Google Ads, SEO]

        // PUŁAPKA: "SQL" jest przed "Spring", bo TreeSet porównuje kody Unicode: 'Q' (81) < 'p' (112).
        // Wielkie litery idą przed małymi. Dla polskich słów (ą, ę, ł...) użyj Collator (t04_strings).

        // Składanie klocków: flatMapping + collectingAndThen → LICZBA różnych umiejętności w dziale.
        Map<Department, Integer> skillCount = SampleData.employees().stream()
                .collect(Collectors.groupingBy(Employee::department, TreeMap::new,
                        Collectors.flatMapping(e -> e.skills().stream(),
                                Collectors.collectingAndThen(Collectors.toSet(), Set::size))));
        show("liczba różnych umiejętności", skillCount);
        // WYNIK: liczba różnych umiejętności → {IT=7, HR=2, SPRZEDAZ=3, KSIEGOWOSC=2, MARKETING=4}
    }

    // =================================================================================================
    // 5. teeing (Java 12+) — dwa kolektory w jednym przejściu
    // =================================================================================================

    /**
     * 5. {@code teeing(k1, k2, merger)} wysyła KAŻDY element do dwóch kolektorów naraz, a na końcu
     * łączy oba wyniki funkcją merger. Jedno przejście po danych zamiast dwóch.
     */
    static void teeingDemo() {
        section("5. teeing (Java 12+) — dwa kolektory naraz");

        // ANALOGIA: trójnik „T” w hydraulice — woda z jednej rury płynie do dwóch rur jednocześnie.

        // Przykład 1: minimum i maksimum w jednym przejściu. minBy = najmniejszy według.
        String extremes = SampleData.employees().stream()
                .collect(Collectors.teeing(
                        Collectors.minBy(Comparator.comparingInt(Employee::salary)),
                        Collectors.maxBy(Comparator.comparingInt(Employee::salary)),
                        (min, max) -> min.map(Employee::name).orElse("-")
                                + " … " + max.map(Employee::name).orElse("-")));
        show("najniższa … najwyższa pensja", extremes);
        // WYNIK: najniższa … najwyższa pensja → Katarzyna Wiśniewska … Michał Lewandowski

        // Przykład 2: licznik + suma → średnia. summingInt = sumuj int.
        // PRZED: dwa osobne przejścia (count() i sum()) — dwa strumienie, dwa razy te same dane.
        long countOld = SampleData.employees().stream().count();
        int sumOld = SampleData.employees().stream().mapToInt(Employee::salary).sum();
        show("średnia (2 przejścia)", (double) sumOld / countOld);
        // WYNIK: średnia (2 przejścia) → 10640.0

        // PO: jedno przejście. merger dostaje (Long count, Integer sum). doubleValue = wartość double.
        double average = SampleData.employees().stream()
                .collect(Collectors.teeing(
                        Collectors.counting(),
                        Collectors.summingInt(Employee::salary),
                        (count, sum) -> count == 0 ? 0.0 : sum.doubleValue() / count));
        show("średnia (teeing)", average);
        // WYNIK: średnia (teeing) → 10640.0

        // Uczciwie: na średnią jest gotowy averagingInt (Streams09). teeing jest dla sytuacji,
        // gdy gotowca NIE MA — np. „ile zamówień i jakie jest najnowsze”:
        String ordersInfo = SampleData.orders().stream()
                .collect(Collectors.teeing(
                        Collectors.counting(),
                        Collectors.maxBy(Comparator.comparing(Order::date)),
                        (count, newest) -> count + " zamówień, najnowsze: "
                                + newest.map(Order::id).orElse("brak")));
        show("zamówienia", ordersInfo);
        // WYNIK: zamówienia → 10 zamówień, najnowsze: ZAM-010

        // DOBRA PRAKTYKA: merger zawsze musi obsłużyć „pusty strumień” (count == 0, Optional.empty).
    }

    // =================================================================================================
    // 6. reducing z mapperem
    // =================================================================================================

    /**
     * 6. {@code reducing(identity, mapper, op)}: najpierw przekształć (mapper), potem zwiń (op),
     * startując od wartości neutralnej (identity). Przydaje się, gdy nie ma gotowego summingX — np. BigDecimal.
     */
    static void reducingDemo() {
        section("6. reducing z mapperem");

        // Trzy wersje (Streams11):  reducing(op) → Optional;  reducing(id, op);  reducing(id, mapper, op).
        // Integer::sum = suma dwóch liczb
        Map<Category, Integer> stockByCategory = SampleData.products().stream()
                .collect(Collectors.groupingBy(Product::category, TreeMap::new,
                        Collectors.reducing(0, Product::stock, Integer::sum)));
        show("sztuki w kategorii (reducing)", stockByCategory);
        // WYNIK: sztuki w kategorii (reducing) → {ELEKTRONIKA=36, SPOZYWCZE=420, KSIAZKI=27, ODZIEZ=92, DOM=20}

        // Dla int prościej: summingInt(Product::stock). reducing błyszczy, gdy gotowca brak — BigDecimal:
        // stockValue = wartość magazynu (cena × sztuki); ZERO = zero; add = dodaj
        Map<Category, BigDecimal> valueByCategory = SampleData.products().stream()
                .collect(Collectors.groupingBy(Product::category, TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Product::stockValue, BigDecimal::add)));
        show("wartość magazynu w kategorii", valueByCategory);
        // WYNIK: wartość magazynu w kategorii → {ELEKTRONIKA=52443.43, SPOZYWCZE=10045.80, KSIAZKI=2643.00, ODZIEZ=9507.20, DOM=6120.00}

        // PUŁAPKA: identity MUSI być neutralna (0 dla sumy, "" dla najdłuższego tekstu, 1 dla iloczynu).
        // Z identity = 1 każda grupa dostaje „gratis” +1 (a w parallel +1 za każdy kawałek danych!).
        Map<Category, Integer> wrongIdentity = SampleData.products().stream()
                .collect(Collectors.groupingBy(Product::category, TreeMap::new,
                        Collectors.reducing(1, Product::stock, Integer::sum)));
        show("BŁĘDNIE: identity = 1", wrongIdentity);
        // WYNIK: BŁĘDNIE: identity = 1 → {ELEKTRONIKA=37, SPOZYWCZE=421, KSIAZKI=28, ODZIEZ=93, DOM=21}
    }

    // =================================================================================================
    // 7. Collector.of — budowa własnego kolektora
    // =================================================================================================

    /**
     * 7. {@code Collector.of(supplier, accumulator, combiner, finisher)} składa kolektor z 4 funkcji.
     * Każda z nich dopisuje się do dziennika, żebyś zobaczył, kto i kiedy jest wołany.
     */
    static void customCollectorAnatomy() {
        section("7. Collector.of — własny kolektor krok po kroku");

        // Kolektor sklejający teksty w "A | B | C" (bez gotowego joining — żeby zobaczyć środek).
        // Każda z 4 funkcji wpisuje się do dziennika (log = dziennik).
        List<String> names = List.of("Ala", "Ola", "Ela");
        List<String> log = new ArrayList<>();
        String joined = names.stream().collect(pipeJoining(log));
        show("pipeJoining", joined);
        // WYNIK: pipeJoining → Ala | Ola | Ela
        showEach("kolejność wywołań (strumień sekwencyjny)", log);
        // WYNIK: kolejność wywołań (strumień sekwencyjny) (liczba elementów: 5):
        // WYNIK: • supplier → nowy StringBuilder
        // WYNIK: • accumulator ← Ala
        // WYNIK: • accumulator ← Ola
        // WYNIK: • accumulator ← Ela
        // WYNIK: • finisher → String

        // Combinera NIE MA w śladzie — strumień sekwencyjny ma jedno „pudełko”, nie ma czego sklejać.
        // W parallel (Streams18) dane dzielą się na kawałki: każdy dostaje własne pudełko (supplier),
        // a combiner skleja je W KOLEJNOŚCI (lewy + prawy). Lista synchronizowana, bo piszą różne wątki.
        List<String> parallelLog = Collections.synchronizedList(new ArrayList<>());
        String parallelResult = SampleData.words().parallelStream().collect(pipeJoining(parallelLog));
        String sequentialResult = SampleData.words().stream().collect(pipeJoining(new ArrayList<>()));
        show("parallel == sekwencyjnie?", parallelResult.equals(sequentialResult));
        // WYNIK: parallel == sekwencyjnie? → true
        show("czy w parallel wołano combiner?", parallelLog.contains("combiner"));
        // WYNIK: czy w parallel wołano combiner? → true    ← na komputerze wielordzeniowym (na 1 rdzeniu mogłoby być false)
        // Czy Java naprawdę podzieli pracę, zależy od liczby rdzeni procesora i wielkości danych — to decyzja
        // środowiska, nie Twojego kodu. Dlatego collector musi działać poprawnie w OBU przypadkach.

        // PUŁAPKA: „mój strumień nie jest równoległy, więc combiner mogę olać (np. return null)”.
        // Nie rób tego: ktoś kiedyś doda .parallel() i dostanie błędny wynik. Combiner ma być poprawny.
    }

    /** Kolektor: teksty → "A | B | C". Każda z czterech części dopisuje się do dziennika {@code log}. */
    static Collector<String, StringBuilder, String> pipeJoining(List<String> log) {
        return Collector.of(
                () -> {                                          // supplier: nowe, puste pudełko
                    log.add("supplier → nowy StringBuilder");
                    return new StringBuilder();
                },
                (sb, text) -> {                                  // accumulator: dołóż jeden element
                    log.add("accumulator ← " + text);
                    if (sb.length() > 0) {
                        sb.append(" | ");
                    }
                    sb.append(text);
                },
                (left, right) -> {                               // combiner: sklej dwa pudełka (lewe + prawe)
                    log.add("combiner");
                    if (left.length() > 0 && right.length() > 0) {
                        left.append(" | ");
                    }
                    return left.append(right);
                },
                sb -> {                                          // finisher: pudełko → wynik końcowy
                    log.add("finisher → String");
                    return sb.toString();
                });
    }

    // =================================================================================================
    // 8. Praktyczny własny kolektor — TOP N najdroższych
    // =================================================================================================

    /**
     * 8. Własny kolektor ma sens, gdy chcesz go UŻYWAĆ WIELE RAZY, także jako downstream w groupingBy.
     * Przykład: „N najdroższych” trzymające w pamięci tylko N elementów (kopiec = PriorityQueue).
     */
    static void practicalCustomCollector() {
        section("8. Praktyczny kolektor — TOP N najdroższych");

        // PRZED: sorted + limit. Proste i czytelne — ale sortuje WSZYSTKO i nie włożysz tego w groupingBy.
        List<Product> top3Sorted = SampleData.products().stream()
                .sorted(Comparator.comparing(Product::price).reversed())   // reversed = odwrócony
                .limit(3)
                .toList();
        show("top 3 (sorted + limit)", top3Sorted);
        // WYNIK: top 3 (sorted + limit) → [Laptop Pro 14 (5499.99 zł), Smartfon X (2999.00 zł), Ekspres do kawy (1899.00 zł)]

        // PO: własny kolektor topByPrice(n) — i TERAZ da się go włożyć do groupingBy: 2 najdroższe w kategorii.
        Map<Category, List<Product>> top2PerCategory = SampleData.products().stream()
                .collect(Collectors.groupingBy(Product::category, TreeMap::new, topByPrice(2)));
        showEach("2 najdroższe w kategorii", top2PerCategory);
        // WYNIK: 2 najdroższe w kategorii (liczba kluczy: 5):
        // WYNIK: • ELEKTRONIKA → [Laptop Pro 14 (5499.99 zł), Smartfon X (2999.00 zł)]
        // WYNIK: • SPOZYWCZE → [Kawa ziarnista 1kg (64.99 zł), Oliwa z oliwek (42.00 zł)]
        // WYNIK: • KSIAZKI → [Java. Podstawy (129.00 zł), Wzorce projektowe (99.00 zł)]
        // WYNIK: • ODZIEZ → [Kurtka zimowa (459.00 zł), T-shirt bawełniany (49.99 zł)]
        // WYNIK: • DOM → [Ekspres do kawy (1899.00 zł), Lampka biurkowa (129.00 zł)]

        // DOBRA PRAKTYKA: zanim napiszesz Collector.of, sprawdź gotowce i ich złożenia. Własny kolektor =
        // gdy gotowce nie wystarczą albo gdy tę samą logikę powtarzasz w wielu miejscach.
    }

    /**
     * Kolektor „n najdroższych produktów”, malejąco po cenie.
     * Pudełko to kopiec (PriorityQueue), w którym na szczycie leży NAJTAŃSZY z zebranych —
     * gdy produktów jest więcej niż n, wyrzucamy właśnie jego. Zostaje n najdroższych.
     */
    static Collector<Product, ?, List<Product>> topByPrice(int n) {
        Comparator<Product> byPrice = Comparator.comparing(Product::price);
        return Collector.of(
                () -> new PriorityQueue<Product>(byPrice),       // supplier: pusty kopiec
                (heap, p) -> {                                   // accumulator: dodaj, a nadmiar wyrzuć
                    heap.add(p);
                    if (heap.size() > n) {
                        heap.poll();                             // poll = zdejmij ze szczytu (najtańszy)
                    }
                },
                (left, right) -> {                               // combiner: przelej prawy do lewego
                    for (Product p : right) {
                        left.add(p);
                        if (left.size() > n) {
                            left.poll();
                        }
                    }
                    return left;
                },
                heap -> heap.stream()                            // finisher: kopiec → lista malejąco
                        .sorted(byPrice.reversed())
                        .toList());
    }

    // =================================================================================================
    // 9. Characteristics — cechy kolektora (krótko)
    // =================================================================================================

    /**
     * 9. Cechy (characteristics) to podpowiedzi dla strumienia, co wolno mu zoptymalizować.
     * Wypisujemy je przez {@code new TreeSet<>(...)}, żeby kolejność była zawsze ta sama.
     */
    static void characteristicsDemo() {
        section("9. Characteristics — cechy kolektora");

        // IDENTITY_FINISH = „pudełko JEST wynikiem” → strumień pomija finisher.
        // UNORDERED       = kolejność elementów w wyniku nie ma znaczenia (np. Set) → parallel może być szybszy.
        // CONCURRENT      = jedno wspólne, bezpieczne wątkowo pudełko dla wszystkich wątków (Streams18).
        show("toList()", new TreeSet<>(Collectors.toList().characteristics()));
        // WYNIK: toList() → [IDENTITY_FINISH]
        show("toSet()", new TreeSet<>(Collectors.toSet().characteristics()));
        // WYNIK: toSet() → [UNORDERED, IDENTITY_FINISH]
        show("joining()", new TreeSet<>(Collectors.joining().characteristics()));
        // WYNIK: joining() → []
        // Collector.of Z finisherem (jak nasz pipeJoining) nie ma żadnych cech; BEZ finishera
        // sam dodaje IDENTITY_FINISH (pudełko = wynik).

        // PUŁAPKA: kłamstwo w cechach. Deklarujemy IDENTITY_FINISH, choć finisher coś robi (StringBuilder → String).
        // Strumień UFA cechom: pomija finisher i próbuje użyć StringBuildera jako String → ClassCastException.
        Collector<String, StringBuilder, String> liar = Collector.of(
                () -> new StringBuilder(),
                (sb, s) -> sb.append(s),
                (a, b) -> a.append(b),
                sb -> sb.toString(),
                Collector.Characteristics.IDENTITY_FINISH);
        expectThrows("fałszywe IDENTITY_FINISH", () -> {
            String result = Stream.of("a", "b").collect(liar);
            show("nie dojdziemy tutaj", result);
        });
        // WYNIK: ✔ fałszywe IDENTITY_FINISH → rzucono ClassCastException: class java.lang.StringBuilder cannot be cast to class java.lang.String (java.lang.StringBuilder and java.lang.String are in module java.base of loader 'bootstrap')

        // DOBRA PRAKTYKA: podawaj cechy tylko, gdy NA PEWNO są prawdziwe. Brak cech = bezpiecznie (najwyżej wolniej).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • collectingAndThen(k, f) — popraw wynik kolektora k; w groupingBy: maxBy + Optional::get
     *     (bezpieczne, bo grupy nie bywają puste)
     *   • filtering(p, k) (9+) — grupa zostaje, nawet pusta; filter przed groupingBy — grupa znika
     *   • filtering + maxBy + Optional::get = NoSuchElementException → użyj map(...).orElse(...)
     *   • flatMapping(e -> e.lista().stream(), k) (9+) — spłaszcz WEWNĄTRZ grupy (klucz już ustalony)
     *   • teeing(k1, k2, (w1, w2) -> ...) (12+) — dwa wyniki w jednym przejściu; obsłuż pusty strumień
     *   • reducing(identity, mapper, op) — np. BigDecimal: reducing(ZERO, Product::stockValue, BigDecimal::add);
     *     identity MUSI być neutralna (0, "", ZERO)
     *   • Collector.of(supplier, accumulator, combiner[, finisher][, cechy...])
     *       supplier    () → nowe pudełko          accumulator (pudełko, element) → void
     *       combiner    (lewe, prawe) → pudełko   finisher    pudełko → wynik
     *   • combiner działa tylko w parallel, ale MUSI być poprawny (kolejność: lewe + prawe)
     *   • cechy: IDENTITY_FINISH (pomija finisher!), UNORDERED, CONCURRENT — nie kłam w cechach
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się filter(...) przed groupingBy od groupingBy(..., filtering(...))? Kiedy które?
     *   2. Dlaczego collectingAndThen(maxBy(...), Optional::get) jest bezpieczne w zwykłym groupingBy,
     *      a rzuca wyjątek po dołożeniu filtering?
     *   3. Co wypisze:  System.out.println(Stream.of("a", "bb", "cc").collect(Collectors.teeing(
     *          Collectors.counting(), Collectors.joining(), (c, s) -> c + ":" + s)));  ?
     *   4. Kiedy wywoływany jest combiner? Czy w strumieniu sekwencyjnym może być byle jaki?
     *   5. ZNAJDŹ BŁĄD:  groupingBy(Employee::department, reducing(1, Employee::salary, Integer::sum))
     *      — suma pensji w dziale wychodzi „o jeden za dużo”. Dlaczego?
     *   6. ZNAJDŹ BŁĄD:  Collector.of(StringBuilder::new, StringBuilder::append, StringBuilder::append,
     *          StringBuilder::toString, Collector.Characteristics.IDENTITY_FINISH)
     *   7. Co wypisze:  System.out.println(Stream.of(List.of(1, 2), List.of(3))
     *          .collect(Collectors.flatMapping(List::stream, Collectors.counting())));  ?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Map<Category, List<String>> expected2 = expectedCheapPerCategory();
        Map<String, Set<Category>> expected3 = expectedCategoriesPerCustomer();

        Check.equal("ćw. 1: produkty i sztuki (teeing)", "produkty: 14, sztuki: 595",
                () -> exercise1(SampleData.products()));
        Check.equal("ćw. 2: tanie w kategorii (filtering)", expected2, () -> exercise2(SampleData.products()));
        Check.equal("ćw. 3: kategorie klienta (flatMapping)", expected3, () -> exercise3(SampleData.orders()));
        Check.equal("ćw. 4: zakres liczb", "1..10", () -> SampleData.numbers().stream().collect(exercise4()));
        Check.equal("ćw. 4: pusty strumień", "brak", () -> Stream.<Integer>empty().collect(exercise4()));
        Check.equal("ćw. 4: parallel", "1..10", () -> SampleData.numbers().parallelStream().collect(exercise4()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "produkty: 14, sztuki: 595", () -> solution1(SampleData.products()));
        Check.equal("ćw. 2 (wzorzec)", expected2, () -> solution2(SampleData.products()));
        Check.equal("ćw. 3 (wzorzec)", expected3, () -> solution3(SampleData.orders()));
        Check.equal("ćw. 4 (wzorzec)", "1..10", () -> SampleData.numbers().stream().collect(solution4()));
        Check.equal("ćw. 4 pusty (wzorzec)", "brak", () -> Stream.<Integer>empty().collect(solution4()));
        Check.equal("ćw. 4 parallel (wzorzec)", "1..10",
                () -> SampleData.numbers().parallelStream().collect(solution4()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /** Oczekiwany wynik ćw. 2 — EnumMap, żeby kolejność wypisywania była zawsze ta sama. */
    private static Map<Category, List<String>> expectedCheapPerCategory() {
        Map<Category, List<String>> expected = new EnumMap<>(Category.class);
        expected.put(Category.ELEKTRONIKA, List.of());
        expected.put(Category.SPOZYWCZE, List.of("Kawa ziarnista 1kg", "Czekolada gorzka", "Oliwa z oliwek"));
        expected.put(Category.KSIAZKI, List.of("Czysty kod", "Wzorce projektowe"));
        expected.put(Category.ODZIEZ, List.of("T-shirt bawełniany"));
        expected.put(Category.DOM, List.of());
        return expected;
    }

    /** Oczekiwany wynik ćw. 3 — TreeMap + EnumSet, żeby kolejność wypisywania była zawsze ta sama. */
    private static Map<String, Set<Category>> expectedCategoriesPerCustomer() {
        Map<String, Set<Category>> expected = new TreeMap<>();
        expected.put("Adam Mazur", EnumSet.of(Category.ELEKTRONIKA));
        expected.put("Jan Kowalski", EnumSet.of(Category.ELEKTRONIKA, Category.SPOZYWCZE, Category.KSIAZKI));
        expected.put("Marek Król", EnumSet.of(Category.SPOZYWCZE, Category.KSIAZKI));
        expected.put("Maria Nowak", EnumSet.of(Category.ELEKTRONIKA, Category.SPOZYWCZE));
        expected.put("Ola Pawlak", EnumSet.of(Category.ODZIEZ));
        expected.put("Zofia Krawczyk", EnumSet.of(Category.ELEKTRONIKA, Category.SPOZYWCZE, Category.DOM));
        return expected;
    }

    /**
     * ĆWICZENIE 1 (łatwe): w JEDNYM przejściu policz produkty i sumę sztuk w magazynie.
     * Zwróć tekst dokładnie w formacie "produkty: 14, sztuki: 595".
     * Podpowiedź: {@code Collectors.teeing(Collectors.counting(), Collectors.summingInt(Product::stock), (c, s) -> ...)}.
     */
    static String exercise1(List<Product> products) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (średnie): dla KAŻDEJ kategorii (TreeMap) zwróć nazwy produktów tańszych niż 100 zł,
     * w kolejności z listy. Kategorie bez tanich produktów MAJĄ zostać w mapie z pustą listą.
     * Podpowiedź: groupingBy(Product::category, TreeMap::new, filtering(..., mapping(Product::name, toList()))).
     * Do porównania ceny użyj stałej HUNDRED i compareTo.
     */
    static Map<Category, List<String>> exercise2(List<Product> products) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ na strumień. Dla każdego klienta (nazwa, alfabetycznie) zbiór kategorii
     * produktów, które kiedykolwiek zamówił (także w zamówieniach anulowanych).
     * <pre>{@code
     * Map<String, Set<Category>> result = new TreeMap<>();
     * for (Order o : orders) {
     *     Set<Category> set = result.computeIfAbsent(o.customer().name(), k -> new TreeSet<>());
     *     for (OrderLine line : o.lines()) {
     *         set.add(line.product().category());
     *     }
     * }
     * return result;
     * }</pre>
     * Podpowiedź: {@code groupingBy(klucz, TreeMap::new, flatMapping(o -> o.lines().stream().map(...), toCollection(TreeSet::new)))}.
     */
    static Map<String, Set<Category>> exercise3(List<Order> orders) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): napisz własny kolektor przez Collector.of, który zwraca tekst "min..max"
     * (np. "1..10"), a dla pustego strumienia "brak". Musi działać także w parallel (poprawny combiner!).
     * Podpowiedź: pudełko może być tablicą {@code int[]{min, max, licznik}}; start: MAX_VALUE, MIN_VALUE, 0.
     * combiner: min z obu, max z obu, suma liczników. finisher: licznik == 0 ? "brak" : min + ".." + max.
     */
    static Collector<Integer, ?, String> exercise4() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(List<Product> products) {
        return products.stream()
                .collect(Collectors.teeing(
                        Collectors.counting(),
                        Collectors.summingInt(Product::stock),
                        (count, units) -> "produkty: " + count + ", sztuki: " + units));
    }

    static Map<Category, List<String>> solution2(List<Product> products) {
        return products.stream()
                .collect(Collectors.groupingBy(Product::category, TreeMap::new,
                        Collectors.filtering(p -> p.price().compareTo(HUNDRED) < 0,
                                Collectors.mapping(Product::name, Collectors.toList()))));
    }

    static Map<String, Set<Category>> solution3(List<Order> orders) {
        return orders.stream()
                .collect(Collectors.groupingBy(o -> o.customer().name(), TreeMap::new,
                        Collectors.flatMapping(o -> o.lines().stream().map(line -> line.product().category()),
                                Collectors.toCollection(TreeSet::new))));
    }

    static Collector<Integer, ?, String> solution4() {
        return Collector.of(
                () -> new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE, 0},   // [min, max, licznik]
                (box, n) -> {
                    box[0] = Math.min(box[0], n);
                    box[1] = Math.max(box[1], n);
                    box[2]++;
                },
                (left, right) -> {
                    left[0] = Math.min(left[0], right[0]);
                    left[1] = Math.max(left[1], right[1]);
                    left[2] += right[2];
                    return left;
                },
                box -> box[2] == 0 ? "brak" : box[0] + ".." + box[1]);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. filter przed groupingBy odrzuca elementy, zanim powstaną grupy → grupy bez trafień znikają
     *      z mapy. filtering działa wewnątrz grupy → grupa zostaje (pusta lista, 0). Raport „dla każdej
     *      grupy” → filtering; tylko grupy z trafieniami → filter (mniejsza mapa).
     *   2. groupingBy tworzy grupę dopiero przy pierwszym elemencie, więc maxBy zawsze coś widzi →
     *      Optional nie jest pusty. filtering może wyrzucić wszystkie elementy grupy, która już istnieje
     *      → maxBy daje Optional.empty → get() rzuca NoSuchElementException.
     *   3. 3:abbcc  — counting daje 3, joining skleja "a" + "bb" + "cc".
     *   4. Tylko w strumieniu równoległym, do sklejania wyników kawałków (lewy + prawy). Musi być poprawny
     *      zawsze — kod bywa później przełączany na parallel, a kolektor bywa używany w innym miejscu.
     *   5. identity = 1 nie jest neutralne dla dodawania — każda grupa startuje od 1 (w parallel nawet
     *      kilka razy). Poprawnie: reducing(0, Employee::salary, Integer::sum) albo summingInt(Employee::salary).
     *   6. finisher zamienia StringBuilder na String, więc cecha IDENTITY_FINISH jest nieprawdziwa. Strumień
     *      pominie finisher i zwróci StringBuilder tam, gdzie oczekiwano String → ClassCastException.
     *      Poprawka: usuń IDENTITY_FINISH. (Dodatkowo StringBuilder::append jako accumulator skleja bez separatora.)
     *   7. 3  — flatMapping spłaszcza obie listy do elementów 1, 2, 3, a counting je liczy.
     */
    // </editor-fold>
}
