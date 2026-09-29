package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Customer;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Order;
import helpers.model.OrderLine;
import helpers.model.OrderStatus;
import helpers.model.Product;
import helpers.model.Student;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Przepisy (recipes) — 39 gotowych rozwiązań typowych zadań na streamach
 *        (recipe = przepis; cookbook = książka kucharska)
 *
 * W SKRÓCIE:
 *   Znasz już wszystkie klocki: filter, map, flatMap, sorted, reduce, kolektory, Optional, BigDecimal.
 *   Tu składamy z nich krótkie „przepisy” na zadania, które w pracy wracają co tydzień.
 *   Każdy przepis ma numer (R1, R2, ...) — łatwo go znaleźć w wydruku i w kodzie.
 *
 * ANALOGIA: Książka kucharska. Znasz składniki (mąka, jajka, mleko = filter, map, groupingBy),
 *   ale naleśniki wychodzą dopiero wtedy, gdy znasz PRZEPIS: co, w jakiej kolejności i ile.
 *   Po kilku razach nie zaglądasz już do książki — przepis masz w głowie.
 *
 * JAK TO DZIAŁA:
 *   Jak dobrać przepis do zadania:
 *   „daj mi N najlepszych”            → sorted(comparator.reversed()) + limit(N)
 *   „ile czego jest”                  → groupingBy(klucz, TreeMap::new, counting())
 *   „średnia / suma w grupie”         → groupingBy(klucz, averagingInt(...) / reducing(ZERO, ..., add))
 *   „największy w grupie”             → groupingBy(klucz, collectingAndThen(maxBy(...), Optional → wartość))
 *   „czego brakuje / kto nie ma”      → zbiór (Set) obecnych kluczy + filter(!set.contains(...))
 *   „po indeksie” (paczki, zip)       → IntStream.range(0, n).mapToObj(i -> ...)
 *   „wynik zależy od poprzedniego”    → zwykła pętla (np. bieżąca suma) — stream tu nie błyszczy
 *
 * SŁÓWKA:
 *   recipe = przepis; top = najlepsze; duplicate = duplikat; missing = brakujący; batch = paczka;
 *   zip = „suwak” (łączenie dwóch list parami); running total = suma bieżąca (narastająca);
 *   frequency = częstość; palindrome = palindrom; revenue = przychód; tenure = staż pracy.
 *
 * ZOBACZ TEŻ: t16_streams/Streams11GroupingBy (grupowanie), t16_streams/Streams13AdvancedCollectors
 *   (collectingAndThen, flatMapping), t16_streams/Streams15BigDecimalMoney (pieniądze),
 *   t16_streams/Streams20Exercises (duży zestaw ćwiczeń), t17_datetime/DateTime01LocalDateTime (daty).
 * </pre>
 */
public class Streams19Recipes {

    /** Stałe poza lambdami: BigDecimal i daty tworzymy raz. */
    private static final BigDecimal LOW_PRICE = new BigDecimal("100");
    private static final LocalDate HIRED_BEFORE = LocalDate.of(2015, 1, 1);
    private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 1, 1);

    public static void main(String[] args) {
        title("Streams19 — przepisy: gotowe rozwiązania na co dzień");

        listsAndSelection();   // lists and selection = listy i wybieranie
        countingAndStats();    // counting and stats = liczenie i statystyki
        grouping();            // grouping = grupowanie
        strings();             // strings = napisy
        money();               // money = pieniądze
        dates();               // dates = daty
        relations();           // relations = relacje między danymi
        exercises();           // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. LISTY I WYBIERANIE
    // =================================================================================================

    /**
     * 1. Wybieranie fragmentów listy: najlepsze N, ostatnie N, duplikaty, braki, paczki, zip.
     */
    static void listsAndSelection() {
        section("1. Listy i wybieranie");
        List<Product> products = SampleData.products();

        // R1. Top 3 najdroższe produkty. sorted = posortuj; reversed = odwrócony; limit = ogranicz
        List<String> top3 = products.stream()
                .sorted(Comparator.comparing(Product::price).reversed())
                .limit(3)
                .map(Product::name)
                .toList(); // toList (Java 16+) = do listy
        show("R1 top 3 najdroższe", top3);
        // WYNIK: R1 top 3 najdroższe → [Laptop Pro 14, Smartfon X, Ekspres do kawy]

        // R2. Ostatnie 3 elementy. skip = pomiń; Math.max chroni przed ujemną liczbą przy krótkiej liście.
        List<Integer> numbers = SampleData.numbers();
        List<Integer> last3 = numbers.stream().skip(Math.max(0, numbers.size() - 3)).toList();
        show("R2 ostatnie 3 liczby", last3);
        // WYNIK: R2 ostatnie 3 liczby → [6, 4, 8]

        // R3. Duplikaty: policz wystąpienia i zostaw te > 1. groupingBy = grupuj; counting = policz
        List<String> duplicates = SampleData.words().stream()
                .collect(Collectors.groupingBy(w -> w, TreeMap::new, Collectors.counting()))
                .entrySet().stream()                 // entrySet = zbiór par klucz-wartość
                .filter(e -> e.getValue() > 1)       // getValue = pobierz wartość
                .map(Map.Entry::getKey)              // getKey = pobierz klucz
                .toList();
        show("R3 słowa powtórzone", duplicates);
        // WYNIK: R3 słowa powtórzone → [java, stream]

        // R4. Brakujące numery 1..10. Najpierw Set (szybkie contains = zawiera), potem przejdź zakres.
        List<Integer> tickets = List.of(1, 2, 4, 7, 8, 10); // List.of (Java 9+) = lista niezmienna
        Set<Integer> present = new HashSet<>(tickets);
        List<Integer> missing = IntStream.rangeClosed(1, 10) // rangeClosed = zakres z końcem włącznie
                .filter(i -> !present.contains(i))
                .boxed()                                     // boxed = opakuj int w Integer
                .toList();
        show("R4 brakujące numery 1..10", missing);
        // WYNIK: R4 brakujące numery 1..10 → [3, 5, 6, 9]

        // R5. Paczki po 3 (batches). Liczymy numery paczek, a każda paczka to subList (fragment listy).
        List<Integer> oneToTen = IntStream.rangeClosed(1, 10).boxed().toList();
        int size = 3;
        List<List<Integer>> batches = IntStream.range(0, (oneToTen.size() + size - 1) / size)
                .mapToObj(i -> oneToTen.subList(i * size, Math.min((i + 1) * size, oneToTen.size())))
                .toList();
        show("R5 paczki po 3", batches);
        // WYNIK: R5 paczki po 3 → [[1, 2, 3], [4, 5, 6], [7, 8, 9], [10]]

        // R6. Zip — łączenie dwóch list parami po indeksie (do krótszej z nich).
        List<String> names = List.of("Ala", "Bartek", "Celina");
        List<Integer> scores = List.of(90, 75, 88, 60);
        List<String> zipped = IntStream.range(0, Math.min(names.size(), scores.size()))
                .mapToObj(i -> names.get(i) + "=" + scores.get(i))
                .toList();
        show("R6 zip imion z punktami", zipped);
        // WYNIK: R6 zip imion z punktami → [Ala=90, Bartek=75, Celina=88]

        // R7. Niski stan magazynu (< 5), od najmniejszego. comparingInt = porównaj po int
        List<String> lowStock = products.stream()
                .filter(p -> p.stock() < 5)
                .sorted(Comparator.comparingInt(Product::stock))
                .map(p -> p.name() + ":" + p.stock())
                .toList();
        show("R7 niski stan", lowStock);
        // WYNIK: R7 niski stan → [Smartfon X:0, Oliwa z oliwek:0, Ekspres do kawy:2, Wzorce projektowe:3, Monitor 27 cali:4]

        // R8. Unikalne umiejętności, posortowane. flatMap = spłaszcz; distinct = bez powtórzeń
        List<String> skills = SampleData.employees().stream()
                .flatMap(e -> e.skills().stream())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER) // CASE_INSENSITIVE_ORDER = bez rozróżniania wielkości liter
                .toList();
        show("R8 umiejętności", skills);
        // WYNIK: R8 umiejętności → [AWS, Canva, CRM, Docker, Excel, Google Ads, Java, Kotlin, Negocjacje, Python, Rekrutacja, SAP, SEO, Spring, SQL]
        // PUŁAPKA: samo sorted() porównuje kody Unicode: wielkie litery < małe, więc „CRM” < „Canva”
        // i „SQL” < „Spring”. Dla polskich liter (ą, ł, ś) potrzebny jest Collator (porządek alfabetyczny
        // danego języka), np. Collator.getInstance(Locale.forLanguageTag("pl-PL")) — sortowanie: t16_streams/Streams05SortDistinctLimit.
    }

    // =================================================================================================
    // 2. LICZENIE I STATYSTYKI
    // =================================================================================================

    /**
     * 2. Liczby: druga najwyższa pensja, najczęstsze słowo, statystyki w jednym przejściu, suma bieżąca.
     */
    static void countingAndStats() {
        section("2. Liczenie i statystyki");
        List<Employee> employees = SampleData.employees();

        // R9. Druga najwyższa RÓŻNA pensja: distinct → malejąco → pomiń 1 → pierwszy.
        int secondSalary = employees.stream()
                .map(Employee::salary)
                .distinct()
                .sorted(Comparator.reverseOrder())   // reverseOrder = odwrotna kolejność naturalna
                .skip(1)
                .findFirst()                         // findFirst = znajdź pierwszy
                .orElseThrow();                      // orElseThrow() (Java 10+) = albo rzuć wyjątek
        show("R9 druga najwyższa pensja", secondSalary);
        // WYNIK: R9 druga najwyższa pensja → 14500
        // PUŁAPKA: bez distinct() przy pensjach [5000, 5000, 4000] „druga” wyszłaby 5000 (ta sama co pierwsza).
        List<Integer> tied = List.of(5000, 5000, 4000);
        show("R9 bez distinct", tied.stream().sorted(Comparator.reverseOrder()).skip(1).findFirst().orElseThrow());
        // WYNIK: R9 bez distinct → 5000
        show("R9 z distinct", tied.stream().distinct().sorted(Comparator.reverseOrder()).skip(1).findFirst().orElseThrow());
        // WYNIK: R9 z distinct → 4000

        // R10. Najczęstsze słowo; przy remisie — alfabetycznie pierwsze. Komparator: liczba malejąco, potem słowo.
        Map<String, Long> wordCounts = SampleData.words().stream()
                .collect(Collectors.groupingBy(w -> w, TreeMap::new, Collectors.counting()));
        Comparator<Map.Entry<String, Long>> byCountThenWord = Map.Entry.<String, Long>comparingByValue()
                .reversed()
                .thenComparing(Map.Entry.comparingByKey()); // thenComparing = następnie porównaj po
        String mostFrequent = wordCounts.entrySet().stream().min(byCountThenWord).map(Map.Entry::getKey).orElse("(brak)");
        show("R10 najczęstsze słowo", mostFrequent);
        // WYNIK: R10 najczęstsze słowo → java
        List<String> top3Words = wordCounts.entrySet().stream()
                .sorted(byCountThenWord)
                .limit(3)
                .map(e -> e.getKey() + "=" + e.getValue())
                .toList();
        show("R10 top 3 słowa (remisy alfabetycznie)", top3Words);
        // WYNIK: R10 top 3 słowa (remisy alfabetycznie) → [java=3, stream=2, enum=1]

        // R11. Statystyki w JEDNYM przejściu. summaryStatistics = statystyki zbiorcze (min, max, średnia, ...)
        IntSummaryStatistics stats = employees.stream().mapToInt(Employee::salary).summaryStatistics();
        show("R11 pensje", String.format(Locale.ROOT, "min=%d, max=%d, średnia=%.1f, liczba=%d, suma=%d",
                stats.getMin(), stats.getMax(), stats.getAverage(), stats.getCount(), stats.getSum()));
        // WYNIK: R11 pensje → min=7200, max=17200, średnia=10640.0, liczba=10, suma=106400
        // PUŁAPKA: stats.toString() formatuje średnią według domyślnego Locale (u nas przecinek) — dlatego format(ROOT).

        // R12. Suma bieżąca (running total). Wersja stream: dla każdej pozycji i sumujemy pierwsze i liczb.
        List<Integer> firstSix = SampleData.numbers().subList(0, 6);
        List<Integer> runningStream = IntStream.rangeClosed(1, firstSix.size())
                .mapToObj(i -> firstSix.subList(0, i).stream().mapToInt(Integer::intValue).sum())
                .toList();
        show("R12 suma bieżąca (stream)", runningStream);
        // WYNIK: R12 suma bieżąca (stream) → [5, 8, 16, 17, 26, 28]
        // Wersja pętla: jedno przejście, zero kombinowania. Tu PĘTLA JEST LEPSZA.
        List<Integer> runningLoop = new ArrayList<>();
        int acc = 0;
        for (int x : firstSix) {
            acc += x;
            runningLoop.add(acc);
        }
        show("R12 suma bieżąca (pętla)", runningLoop);
        // WYNIK: R12 suma bieżąca (pętla) → [5, 8, 16, 17, 26, 28]
        // DOBRA PRAKTYKA: gdy wynik zależy od POPRZEDNIEGO elementu, stream daje O(n²) albo efekt uboczny.
        // Pętla jest wtedy czytelniejsza i szybsza (dla tablic jest też Arrays.parallelPrefix).

        // R13. Procent produktów dostępnych. count = policz; %% w formacie = znak procentu
        List<Product> products = SampleData.products();
        long available = products.stream().filter(Product::inStock).count();
        show("R13 dostępne produkty", String.format(Locale.ROOT, "%d z %d (%.1f%%)",
                available, products.size(), 100.0 * available / products.size()));
        // WYNIK: R13 dostępne produkty → 12 z 14 (85.7%)
    }

    // =================================================================================================
    // 3. GRUPOWANIE
    // =================================================================================================

    /**
     * 3. Grupy: średnia, maksimum w grupie, podział na dwie części, lista → mapa po kluczu.
     * Klucze-enumy → {@code TreeMap::new}, żeby kolejność wydruku była stała.
     */
    static void grouping() {
        section("3. Grupowanie");
        List<Employee> employees = SampleData.employees();

        // R14. Średnia pensja w dziale. averagingInt = średnia z int (zawsze double)
        Map<Department, Double> avgSalary = employees.stream()
                .collect(Collectors.groupingBy(Employee::department, TreeMap::new,
                        Collectors.averagingInt(Employee::salary)));
        show("R14 średnia pensja w dziale", avgSalary);
        // WYNIK: R14 średnia pensja w dziale → {IT=13400.0, HR=7200.0, SPRZEDAZ=10050.0, KSIEGOWOSC=8100.0, MARKETING=8700.0}

        // R15. Najstarszy w dziale. maxBy daje Optional → collectingAndThen = zbierz, a potem przekształć.
        Map<Department, String> oldest = employees.stream()
                .collect(Collectors.groupingBy(Employee::department, TreeMap::new,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparingInt(Employee::age)), // maxBy = największy według
                                opt -> opt.map(Employee::name).orElse("?"))));
        show("R15 najstarszy w dziale", oldest);
        // WYNIK: R15 najstarszy w dziale → {IT=Michał Lewandowski, HR=Katarzyna Wiśniewska, SPRZEDAZ=Krzysztof Szymański, KSIEGOWOSC=Magdalena Kamińska, MARKETING=Paweł Dąbrowski}

        // R16. Podział według ceny. partitioningBy = podziel na dwie grupy: true / false (obie zawsze są).
        Map<Boolean, Long> cheapVsExpensive = SampleData.products().stream()
                .collect(Collectors.partitioningBy(p -> p.price().compareTo(LOW_PRICE) < 0, Collectors.counting()));
        show("R16 tańsze niż 100 zł? (liczba)", cheapVsExpensive);
        // WYNIK: R16 tańsze niż 100 zł? (liczba) → {false=8, true=6}

        // R17. Lista → mapa po sku. toMap = do mapy; Function.identity() = „sam element”;
        // (a, b) -> a = przy duplikacie zostaw pierwszy; TreeMap::new = posortowane klucze.
        Map<String, Product> bySku = SampleData.products().stream()
                .collect(Collectors.toMap(Product::sku, Function.identity(), (a, b) -> a, TreeMap::new));
        show("R17 mapa po sku: rozmiar", bySku.size());
        // WYNIK: R17 mapa po sku: rozmiar → 14
        show("R17 bySku.get(\"KSI-002\")", bySku.get("KSI-002"));
        // WYNIK: R17 bySku.get("KSI-002") → Java. Podstawy (129.00 zł)
        // PUŁAPKA: toMap bez funkcji łączącej rzuca IllegalStateException przy powtórzonym kluczu.
    }

    // =================================================================================================
    // 4. NAPISY
    // =================================================================================================

    /**
     * 4. Napisy: łączenie z „i”, spłaszczanie zdań, częstość znaków, palindromy, odwracanie.
     */
    static void strings() {
        section("4. Napisy");

        // R18. Łączenie imion: „Ala, Ola i Ela” (przecinki, a przed ostatnim „ i ”).
        show("R18 trzy imiona", joinPolish(List.of("Ala", "Ola", "Ela")));
        // WYNIK: R18 trzy imiona → Ala, Ola i Ela
        show("R18 jedno imię", joinPolish(List.of("Ala")));
        // WYNIK: R18 jedno imię → Ala

        // R19. Spłaszcz zdania na słowa. split = podziel; "\\s+" = jeden lub więcej białych znaków
        List<String> allWords = SampleData.sentences().stream()
                .flatMap(s -> Arrays.stream(s.split("\\s+")))
                .toList();
        show("R19 liczba słów we wszystkich zdaniach", allWords.size());
        // WYNIK: R19 liczba słów we wszystkich zdaniach → 16
        show("R19 pierwsze 5 słów", allWords.subList(0, 5));
        // WYNIK: R19 pierwsze 5 słów → [Java, jest, językiem, obiektowym, Stream]

        // R20. Liczba słów w każdym zdaniu.
        List<Integer> wordsPerSentence = SampleData.sentences().stream()
                .map(s -> s.split("\\s+").length)
                .toList();
        show("R20 słowa w zdaniach", wordsPerSentence);
        // WYNIK: R20 słowa w zdaniach → [4, 5, 4, 3]

        // R21. Częstość znaków. chars = strumień kodów znaków (IntStream); mapToObj = zamień na obiekty
        Map<Character, Long> charFrequency = "abrakadabra".chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.groupingBy(c -> c, TreeMap::new, Collectors.counting()));
        show("R21 częstość liter w „abrakadabra”", charFrequency);
        // WYNIK: R21 częstość liter w „abrakadabra” → {a=5, b=2, d=1, k=1, r=2}

        // R22. Palindromy (czytane wspak tak samo), bez rozróżniania wielkości liter.
        List<String> candidates = List.of("kajak", "java", "oko", "radar", "stream", "Anna");
        List<String> palindromes = candidates.stream().filter(Streams19Recipes::isPalindrome).toList();
        show("R22 palindromy", palindromes);
        // WYNIK: R22 palindromy → [kajak, oko, radar, Anna]

        // R23. Odwróć kolejność słów — indeksy od końca.
        String[] parts = "Stream to nie jest kolekcja".split(" ");
        String reversedWords = IntStream.range(0, parts.length)
                .mapToObj(i -> parts[parts.length - 1 - i])
                .collect(Collectors.joining(" ")); // joining = połącz napisy
        show("R23 słowa od końca", reversedWords);
        // WYNIK: R23 słowa od końca → kolekcja jest nie to Stream

        // R24. Najdłuższe słowo na każdą pierwszą literę; przy remisie długości — alfabetycznie pierwsze.
        Map<Character, String> longestByLetter = SampleData.words().stream()
                .distinct()
                .collect(Collectors.groupingBy(w -> w.charAt(0), TreeMap::new,   // charAt = znak na pozycji
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparingInt(String::length)
                                        .thenComparing(Comparator.reverseOrder())),
                                opt -> opt.orElse(""))));
        show("R24 najdłuższe słowo na literę", longestByLetter);
        // WYNIK: R24 najdłuższe słowo na literę → {e=enum, j=java, k=kolekcja, l=lambda, m=mapa, o=optional, r=rekord, s=stream}
    }

    /** Łączy imiona przecinkami, a ostatnie doklejamy przez „ i ”. Działa też dla 0 i 1 imienia. */
    static String joinPolish(List<String> names) {
        if (names.size() <= 1) {
            return String.join("", names);
        }
        String allButLast = names.stream().limit(names.size() - 1).collect(Collectors.joining(", "));
        return allButLast + " i " + names.get(names.size() - 1);
    }

    /** Palindrom: po zamianie na małe litery (Locale.ROOT) napis czytany wspak jest taki sam. */
    static boolean isPalindrome(String word) {
        String lower = word.toLowerCase(Locale.ROOT);
        return new StringBuilder(lower).reverse().toString().equals(lower); // reverse = odwróć
    }

    // =================================================================================================
    // 5. PIENIĄDZE (BigDecimal)
    // =================================================================================================

    /**
     * 5. Pieniądze: sumy na klienta i status, najlepszy klient, wartość magazynu, średnia z zaokrągleniem.
     * Zawsze {@code reduce(BigDecimal.ZERO, ..., BigDecimal::add)} i porównania przez {@code compareTo}.
     */
    static void money() {
        section("5. Pieniądze (BigDecimal)");
        List<Order> orders = SampleData.orders();

        // R25. Suma zamówień na klienta (bez anulowanych). reducing = redukuj w grupie (start, mapuj, połącz)
        Map<String, BigDecimal> totalPerCustomer = orders.stream()
                .filter(o -> o.status() != OrderStatus.ANULOWANE)
                .collect(Collectors.groupingBy(o -> o.customer().name(), TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Order::total, BigDecimal::add)));
        showEach("R25 suma na klienta", totalPerCustomer);
        // WYNIK: R25 suma na klienta (liczba kluczy: 5):
        // WYNIK:    • Jan Kowalski → 6669.79
        // WYNIK:    • Marek Król → 295.45
        // WYNIK:    • Maria Nowak → 619.77
        // WYNIK:    • Ola Pawlak → 608.97
        // WYNIK:    • Zofia Krawczyk → 4755.98

        // R26. Najlepszy klient = największa suma. comparingByValue = porównaj po wartości (BigDecimal.compareTo)
        Map.Entry<String, BigDecimal> best = totalPerCustomer.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();
        show("R26 najlepszy klient", best.getKey() + " (" + best.getValue() + " zł)");
        // WYNIK: R26 najlepszy klient → Jan Kowalski (6669.79 zł)

        // R27. Przychód według statusu — EnumMap trzyma kolejność deklaracji enuma.
        Map<OrderStatus, BigDecimal> revenuePerStatus = orders.stream()
                .collect(Collectors.groupingBy(Order::status, () -> new EnumMap<>(OrderStatus.class),
                        Collectors.reducing(BigDecimal.ZERO, Order::total, BigDecimal::add)));
        show("R27 przychód wg statusu", revenuePerStatus);
        // WYNIK: R27 przychód wg statusu → {NOWE=958.87, OPLACONE=2191.98, WYSLANE=602.45, DOSTARCZONE=9196.66, ANULOWANE=2999.00}

        // R28. Wartość magazynu = suma (cena × stan). stockValue = wartość zapasu
        List<Product> products = SampleData.products();
        BigDecimal stockValue = products.stream()
                .map(Product::stockValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        show("R28 wartość magazynu", stockValue + " zł");
        // WYNIK: R28 wartość magazynu → 80759.43 zł

        // R29. Średnia cena z zaokrągleniem. divide = podziel (skala 2, HALF_UP = zaokrąglenie „szkolne”)
        BigDecimal sumOfPrices = products.stream().map(Product::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgPrice = sumOfPrices.divide(BigDecimal.valueOf(products.size()), 2, RoundingMode.HALF_UP);
        show("R29 średnia cena", avgPrice + " zł");
        // WYNIK: R29 średnia cena → 936.17 zł
        // PUŁAPKA: divide bez skali i trybu zaokrąglenia rzuca ArithmeticException, gdy wynik ma nieskończone rozwinięcie.
    }

    // =================================================================================================
    // 6. DATY
    // =================================================================================================

    /**
     * 6. Daty: grupowanie po miesiącu ({@code YearMonth}), filtr „przed datą”, staż, odstęp w dniach.
     * Zawsze stała data odniesienia — nigdy {@code now()} w wyniku, który ma być powtarzalny.
     */
    static void dates() {
        section("6. Daty");
        List<Order> orders = SampleData.orders();
        List<Employee> employees = SampleData.employees();

        // R30. Zamówienia według miesiąca. YearMonth.from = rok-miesiąc z daty; mapping = przekształć w grupie
        Map<YearMonth, List<String>> byMonth = orders.stream()
                .collect(Collectors.groupingBy(o -> YearMonth.from(o.date()), TreeMap::new,
                        Collectors.mapping(Order::id, Collectors.toList())));
        showEach("R30 zamówienia wg miesiąca", byMonth);
        // WYNIK: R30 zamówienia wg miesiąca (liczba kluczy: 4):
        // WYNIK:    • 2026-01 → [ZAM-001, ZAM-002]
        // WYNIK:    • 2026-02 → [ZAM-003, ZAM-004, ZAM-005]
        // WYNIK:    • 2026-03 → [ZAM-006, ZAM-007, ZAM-008, ZAM-009]
        // WYNIK:    • 2026-04 → [ZAM-010]

        // R31. Zatrudnieni przed 2015-01-01, od najdawniej zatrudnionego. isBefore = jest przed
        List<String> veterans = employees.stream()
                .filter(e -> e.hireDate().isBefore(HIRED_BEFORE))
                .sorted(Comparator.comparing(Employee::hireDate))
                .map(Employee::name)
                .toList();
        show("R31 zatrudnieni przed 2015", veterans);
        // WYNIK: R31 zatrudnieni przed 2015 → [Krzysztof Szymański, Magdalena Kamińska, Michał Lewandowski]

        // R32. Najdłuższy staż (w pełnych latach) na dzień 2026-01-01. ChronoUnit.YEARS.between = ile lat między
        Employee longest = employees.stream().min(Comparator.comparing(Employee::hireDate)).orElseThrow();
        long years = ChronoUnit.YEARS.between(longest.hireDate(), REFERENCE_DATE);
        show("R32 najdłuższy staż", longest.name() + " (" + years + " lat)");
        // WYNIK: R32 najdłuższy staż → Krzysztof Szymański (15 lat)

        // R33. Ile dni między pierwszym a ostatnim zamówieniem? min/max na datach (LocalDate jest Comparable).
        LocalDate firstDate = orders.stream().map(Order::date).min(Comparator.naturalOrder()).orElseThrow();
        LocalDate lastDate = orders.stream().map(Order::date).max(Comparator.naturalOrder()).orElseThrow();
        show("R33 zakres zamówień", firstDate + " … " + lastDate + " = "
                + ChronoUnit.DAYS.between(firstDate, lastDate) + " dni");
        // WYNIK: R33 zakres zamówień → 2026-01-05 … 2026-04-02 = 87 dni
    }

    // =================================================================================================
    // 7. RELACJE MIĘDZY DANYMI
    // =================================================================================================

    /**
     * 7. Dane z dwóch list: kto nie ma zamówień, który dział jest pusty, bestseller, kto kupił książki.
     * Wzorzec: najpierw zbuduj {@code Set} kluczy z jednej listy, potem filtruj drugą (szybko: O(n + m)).
     */
    static void relations() {
        section("7. Relacje między danymi");
        List<Order> orders = SampleData.orders();

        // R34. Klienci bez zamówień. toSet = do zbioru
        Set<Long> customersWithOrders = orders.stream().map(o -> o.customer().id()).collect(Collectors.toSet());
        List<String> withoutOrders = SampleData.customers().stream()
                .filter(c -> !customersWithOrders.contains(c.id()))
                .map(Customer::name)
                .toList();
        show("R34 klienci bez zamówień", withoutOrders);
        // WYNIK: R34 klienci bez zamówień → [Ewa Lis]
        // PUŁAPKA: orders.stream().noneMatch(...) WEWNĄTRZ filtra klientów działa, ale to pętla w pętli (n × m).

        // R35. Działy bez pracowników. EnumSet.noneOf = pusty zbiór enumów; values() = wszystkie stałe enuma
        Set<Department> usedDepartments = SampleData.employees().stream()
                .map(Employee::department)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Department.class)));
        List<Department> emptyDepartments = Arrays.stream(Department.values())
                .filter(d -> !usedDepartments.contains(d))
                .toList();
        show("R35 działy bez pracowników", emptyDepartments);
        // WYNIK: R35 działy bez pracowników → [LOGISTYKA]

        // R36. Studenci bez ocen. isEmpty = czy pusta
        List<String> noGrades = SampleData.students().stream()
                .filter(s -> s.grades().isEmpty())
                .map(Student::name)
                .toList();
        show("R36 studenci bez ocen", noGrades);
        // WYNIK: R36 studenci bez ocen → [Henryk]

        // R37. Bestseller = produkt z największą liczbą zamówionych sztuk. summingInt = zsumuj int
        Map<String, Integer> quantityPerProduct = orders.stream()
                .flatMap(o -> o.lines().stream())
                .collect(Collectors.groupingBy(line -> line.product().name(), TreeMap::new,
                        Collectors.summingInt(OrderLine::quantity)));
        Map.Entry<String, Integer> bestseller = quantityPerProduct.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();
        show("R37 bestseller", bestseller.getKey() + " (" + bestseller.getValue() + " szt.)");
        // WYNIK: R37 bestseller → Czekolada gorzka (15 szt.)

        // R38. Klienci, którzy kupili choć jedną książkę. anyMatch = czy którykolwiek pasuje
        List<String> bookBuyers = orders.stream()
                .filter(o -> o.lines().stream().anyMatch(line -> line.product().category() == Category.KSIAZKI))
                .map(o -> o.customer().name())
                .distinct()
                .sorted()
                .toList();
        show("R38 kupili książki", bookBuyers);
        // WYNIK: R38 kupili książki → [Jan Kowalski, Marek Król]

        // R39. Ile zamówień ma każdy klient (także ZERO!). Zaczynamy od KLIENTÓW, nie od zamówień.
        Map<Long, Long> ordersPerId = orders.stream()
                .collect(Collectors.groupingBy(o -> o.customer().id(), Collectors.counting()));
        List<String> ordersCount = SampleData.customers().stream()
                .map(c -> c.name() + "=" + ordersPerId.getOrDefault(c.id(), 0L)) // getOrDefault = pobierz albo domyślna
                .toList();
        show("R39 zamówienia klientów", ordersCount);
        // WYNIK: R39 zamówienia klientów → [Jan Kowalski=3, Maria Nowak=2, Adam Mazur=1, Zofia Krawczyk=2, Ola Pawlak=1, Marek Król=1, Ewa Lis=0]
        // DOBRA PRAKTYKA: grupując zamówienia, NIE zobaczysz klientów bez zamówień — dlatego iterujemy po klientach.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • top N: sorted(comparing(...).reversed()).limit(N);  ostatnie N: skip(max(0, size - N))
     *   • druga najwyższa RÓŻNA: distinct().sorted(reverseOrder()).skip(1).findFirst()
     *   • najczęstszy z remisami: groupingBy(counting) → entrySet → min(liczba malejąco, klucz rosnąco)
     *   • braki / „kto nie ma”: Set obecnych kluczy + filter(!set.contains(x))  — nie pętla w pętli
     *   • paczki i zip: IntStream.range(0, n).mapToObj(i -> ...)
     *   • średnia w grupie: averagingInt; maks w grupie: collectingAndThen(maxBy(...), opt -> ...)
     *   • pieniądze: reducing(ZERO, Order::total, BigDecimal::add); divide(x, 2, HALF_UP)
     *   • daty: YearMonth.from(date), isBefore, ChronoUnit.YEARS/DAYS.between — stała data odniesienia
     *   • klucze-enumy: TreeMap::new albo () -> new EnumMap<>(X.class); łańcuchy: TreeMap::new
     *   • wynik zależy od poprzedniego elementu (suma bieżąca) → zwykła pętla
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego w R9 (druga najwyższa pensja) potrzebne jest distinct()?
     *   2. Co wypisze:
     *        System.out.println(Stream.of("b", "a", "c", "a")
     *              .collect(Collectors.groupingBy(s -> s, TreeMap::new, Collectors.counting())));
     *   3. ZNAJDŹ BŁĄD (suma zamówień na klienta):
     *        orders.stream().collect(Collectors.toMap(o -> o.customer().name(), Order::total));
     *   4. Co wypisze:  System.out.println(Stream.of("Ola", "Ala", "ela").sorted().toList());  ?
     *   5. ZNAJDŹ BŁĄD (suma bieżąca):
     *        int[] sum = {0};
     *        List<Integer> running = numbers.parallelStream().map(x -> sum[0] += x).toList();
     *   6. Dlaczego w R34 budujemy najpierw Set identyfikatorów, zamiast dla każdego klienta
     *      przeszukiwać wszystkie zamówienia?
     *   7. Dlaczego R39 zaczyna od listy klientów, a nie od listy zamówień?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: niedostępne produkty", List.of("Oliwa z oliwek", "Smartfon X"),
                () -> exercise1(SampleData.products()));
        Check.equal("ćw. 2: zamówienia wg miasta", expectedOrdersPerCity(), () -> exercise2(SampleData.orders()));
        Check.equal("ćw. 3: średnia ocen w mieście", expectedAvgPerCity(), () -> exercise3(SampleData.students()));
        Check.equal("ćw. 4: najdroższy produkt VIP-a", expectedVipTopProduct(), () -> exercise4(SampleData.orders()));
        Check.equal("ćw. 5: przychód miesięczny", expectedMonthlyRevenue(), () -> exercise5(SampleData.orders()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("Oliwa z oliwek", "Smartfon X"), () -> solution1(SampleData.products()));
        Check.equal("ćw. 2 (wzorzec)", expectedOrdersPerCity(), () -> solution2(SampleData.orders()));
        Check.equal("ćw. 3 (wzorzec)", expectedAvgPerCity(), () -> solution3(SampleData.students()));
        Check.equal("ćw. 4 (wzorzec)", expectedVipTopProduct(), () -> solution4(SampleData.orders()));
        Check.equal("ćw. 5 (wzorzec)", expectedMonthlyRevenue(), () -> solution5(SampleData.orders()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    static Map<String, Long> expectedOrdersPerCity() {
        Map<String, Long> expected = new TreeMap<>();
        expected.put("Gdańsk", 1L);
        expected.put("Kraków", 3L);
        expected.put("Poznań", 1L);
        expected.put("Warszawa", 5L);
        return expected;
    }

    static Map<String, Double> expectedAvgPerCity() {
        Map<String, Double> expected = new TreeMap<>();
        expected.put("Gdańsk", 4.75);
        expected.put("Kraków", 3.75);
        expected.put("Poznań", 3.5);
        expected.put("Warszawa", 3.75);
        return expected;
    }

    static Map<String, String> expectedVipTopProduct() {
        Map<String, String> expected = new TreeMap<>();
        expected.put("Jan Kowalski", "Laptop Pro 14");
        expected.put("Zofia Krawczyk", "Ekspres do kawy");
        return expected;
    }

    static Map<YearMonth, BigDecimal> expectedMonthlyRevenue() {
        Map<YearMonth, BigDecimal> expected = new TreeMap<>();
        expected.put(YearMonth.of(2026, 1), new BigDecimal("6469.66"));
        expected.put(YearMonth.of(2026, 2), new BigDecimal("2335.98"));
        expected.put(YearMonth.of(2026, 3), new BigDecimal("3981.32"));
        expected.put(YearMonth.of(2026, 4), new BigDecimal("163.00"));
        return expected;
    }

    /**
     * ĆWICZENIE 1 (łatwe): nazwy produktów niedostępnych (stan 0), posortowane alfabetycznie.
     * Podpowiedź: filter(p -> !p.inStock()) → map(Product::name) → sorted() → toList().
     */
    static List<String> exercise1(List<Product> products) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ pętlę na stream — liczba zamówień według miasta klienta:
     * <pre>{@code
     * Map<String, Long> result = new TreeMap<>();
     * for (Order o : orders) {
     *     String city = o.customer().city();
     *     result.put(city, result.getOrDefault(city, 0L) + 1);
     * }
     * return result;
     * }</pre>
     * Podpowiedź: groupingBy(o -> o.customer().city(), TreeMap::new, counting()).
     */
    static Map<String, Long> exercise2(List<Order> orders) {
        // TODO: twoje rozwiązanie
        return new TreeMap<>();
    }

    /**
     * ĆWICZENIE 3 (średnie): średnia WSZYSTKICH ocen studentów z danego miasta (TreeMap: miasto → średnia).
     * Student bez ocen nie zmienia średniej miasta.
     * Podpowiedź: groupingBy(Student::city, TreeMap::new, flatMapping(s -> s.grades().stream(), averagingInt(...)))
     * — flatMapping (Java 9+) spłaszcza listy ocen wewnątrz grupy.
     */
    static Map<String, Double> exercise3(List<Student> students) {
        // TODO: twoje rozwiązanie
        return new TreeMap<>();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): dla każdego klienta VIP podaj nazwę najdroższego produktu, jaki kiedykolwiek
     * zamówił (TreeMap: imię i nazwisko → nazwa produktu).
     * Podpowiedź: filter(VIP) → groupingBy(nazwa klienta, TreeMap::new, flatMapping(o -> linie → produkty,
     * collectingAndThen(maxBy(comparing(Product::price)), opt -> opt.map(Product::name).orElse("?")))).
     */
    static Map<String, String> exercise4(List<Order> orders) {
        // TODO: twoje rozwiązanie
        return new TreeMap<>();
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): przychód w każdym miesiącu (YearMonth → BigDecimal), BEZ zamówień anulowanych.
     * Łączy R25 (pieniądze) i R30 (miesiące).
     * Podpowiedź: filter(status != ANULOWANE) → groupingBy(YearMonth.from(date), TreeMap::new,
     * reducing(BigDecimal.ZERO, Order::total, BigDecimal::add)).
     */
    static Map<YearMonth, BigDecimal> exercise5(List<Order> orders) {
        // TODO: twoje rozwiązanie
        return new TreeMap<>();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(List<Product> products) {
        return products.stream()
                .filter(p -> !p.inStock())
                .map(Product::name)
                .sorted()
                .toList();
    }

    static Map<String, Long> solution2(List<Order> orders) {
        return orders.stream()
                .collect(Collectors.groupingBy(o -> o.customer().city(), TreeMap::new, Collectors.counting()));
    }

    static Map<String, Double> solution3(List<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::city, TreeMap::new,
                        Collectors.flatMapping(s -> s.grades().stream(),
                                Collectors.averagingInt(Integer::intValue))));
    }

    static Map<String, String> solution4(List<Order> orders) {
        return orders.stream()
                .filter(o -> o.customer().vip())
                .collect(Collectors.groupingBy(o -> o.customer().name(), TreeMap::new,
                        Collectors.flatMapping(o -> o.lines().stream().map(OrderLine::product),
                                Collectors.collectingAndThen(
                                        Collectors.maxBy(Comparator.comparing(Product::price)),
                                        opt -> opt.map(Product::name).orElse("?")))));
    }

    static Map<YearMonth, BigDecimal> solution5(List<Order> orders) {
        return orders.stream()
                .filter(o -> o.status() != OrderStatus.ANULOWANE)
                .collect(Collectors.groupingBy(o -> YearMonth.from(o.date()), TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Order::total, BigDecimal::add)));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo najwyższa pensja może się powtarzać. Bez distinct() skip(1) pominie tylko JEDNĄ kopię
     *      i „druga” będzie równa pierwszej (np. [5000, 5000, 4000] → 5000 zamiast 4000).
     *   2. {a=2, b=1, c=1} — TreeMap sortuje klucze, counting zwraca Long.
     *   3. Klient z kilkoma zamówieniami → powtórzony klucz → IllegalStateException (Duplicate key).
     *      Poprawnie: toMap(o -> o.customer().name(), Order::total, BigDecimal::add)
     *      albo groupingBy(..., reducing(BigDecimal.ZERO, Order::total, BigDecimal::add)).
     *   4. [Ala, Ola, ela] — kolejność Unicode: wielkie litery są przed małymi.
     *      Bez rozróżniania wielkości: sorted(String.CASE_INSENSITIVE_ORDER) → [Ala, ela, Ola].
     *   5. Efekt uboczny na wspólnej tablicy + parallelStream → wyścig wątków, sumy „skaczą”.
     *      Nawet sekwencyjnie to zły styl. Suma bieżąca → zwykła pętla (R12).
     *   6. Set.contains działa w czasie stałym, więc całość to O(n + m). Przeszukiwanie zamówień dla
     *      każdego klienta to O(n × m) — przy 10 000 klientów i 1 000 000 zamówień to katastrofa.
     *   7. Grupowanie zamówień tworzy klucze TYLKO dla klientów, którzy coś zamówili. Ewa Lis
     *      by zniknęła. Iterując po klientach i używając getOrDefault(id, 0L), widzimy też zera.
     */
    // </editor-fold>
}
