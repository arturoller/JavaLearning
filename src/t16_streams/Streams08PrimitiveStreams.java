package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Product;
import helpers.model.Student;

import java.util.Arrays;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Strumienie prymitywne — IntStream, LongStream, DoubleStream
 *        (primitive stream = strumień prymitywny, czyli strumień „gołych” liczb int / long / double)
 *
 * W SKRÓCIE:
 *   {@code Stream<Integer>} przechowuje OBIEKTY Integer (liczby „w pudełkach”). IntStream przechowuje zwykłe int-y.
 *   Dlatego IntStream zużywa mniej pamięci, jest szybszy i ma gotowe metody: sum(), average(), min(), max(),
 *   summaryStatistics(). Liczysz coś na liczbach? Prawie zawsze przejdź na strumień prymitywny.
 *
 * ANALOGIA: Monety do przeliczenia. {@code Stream<Integer>} to monety, z których każda leży w osobnym pudełku
 *   z kokardką (obiekt Integer) — żeby je zsumować, musisz otworzyć każde pudełko (rozpakowanie).
 *   IntStream to monety luzem wsypane do liczarki: liczarka od razu podaje sumę, średnią, najmniejszą
 *   i największą monetę. Pudełka zakładasz z powrotem dopiero wtedy, gdy naprawdę musisz (boxed()).
 *
 * JAK TO DZIAŁA:
 *   Stream obiektów  ── mapToInt(Product::stock) ──→  IntStream  ── sum() ──→  int
 *   IntStream        ── boxed() ──────────────────→  {@code Stream<Integer>}
 *   IntStream        ── mapToObj(i → "Pokój " + i) →  {@code Stream<String>}
 *
 *   typ strumienia | sum()   | average()      | min() / max()   | count()
 *   IntStream      | int     | OptionalDouble | OptionalInt     | long
 *   LongStream     | long    | OptionalDouble | OptionalLong    | long
 *   DoubleStream   | double  | OptionalDouble | OptionalDouble  | long
 *
 *   Pakowanie (boxing)       = int → Integer (nowy obiekt na stercie, jeśli liczba jest spoza -128..127).
 *   Rozpakowanie (unboxing)  = Integer → int. W {@code Stream<Integer>} dzieje się to przy KAŻDYM elemencie.
 *
 * SŁÓWKA:
 *   primitive = prymitywny (typ prosty); boxing = pakowanie; unboxing = rozpakowanie; range = zakres;
 *   rangeClosed = zakres domknięty (razem z końcem); boxed = opakowany; summary statistics = statystyki zbiorcze;
 *   average = średnia; overflow = przepełnienie; as = jako; iterate = powtarzaj (iteruj); chars = znaki;
 *   empty = pusty; count = policz
 *
 * ZOBACZ TEŻ: t16_streams/Streams03FilterMap (pierwsze mapToInt), t16_streams/Streams07Reduce (sum jako reduce),
 *   t15_numbers/Numbers05IntegerTricks (przepełnienie int), t16_streams/Streams09CollectorsBasic (summingInt,
 *   averagingInt), t16_streams/Streams15BigDecimalMoney (pieniądze liczymy w BigDecimal, nie w double)
 * </pre>
 */
public class Streams08PrimitiveStreams {

    public static void main(String[] args) {
        title("Streams08 — strumienie prymitywne: IntStream, LongStream, DoubleStream");

        whyPrimitive();          // why primitive = dlaczego prymitywne
        creation();              // creation = tworzenie
        terminalMath();          // terminal math = obliczenia na końcu strumienia
        averageIsOptional();     // average is optional = średnia bywa pusta
        summaryStatistics();     // summary statistics = statystyki zbiorcze
        backToObjects();         // back to objects = powrót do obiektów
        conversions();           // conversions = konwersje (zamiany typów)
        iterateAndGenerate();    // iterate and generate = iteruj i generuj
        overflow();              // overflow = przepełnienie
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. DLACZEGO STRUMIENIE PRYMITYWNE
    // =================================================================================================

    /**
     * 1. Po co osobne IntStream / LongStream / DoubleStream? Bo pakowanie (boxing) kosztuje,
     * a {@code Stream<Integer>} nie ma metody sum() — nie wie, że trzyma liczby.
     */
    static void whyPrimitive() {
        section("1. Dlaczego strumienie prymitywne?");

        List<Integer> numbers = SampleData.numbers();   // elementy to OBIEKTY Integer, nie int-y
        show("liczby", numbers);
        // WYNIK: liczby → [5, 3, 8, 1, 9, 2, 7, 3, 10, 6, 4, 8]

        // PRZED: Stream<Integer> — sumowanie przez reduce (przypomnienie z Streams07).
        // Każdy krok: rozpakuj dwa Integer → dodaj → zapakuj wynik w nowy Integer.
        int sumBoxed = numbers.stream().reduce(0, Integer::sum);   // reduce = zredukuj (zwiń do jednej wartości)
        show("suma przez reduce", sumBoxed);
        // WYNIK: suma przez reduce → 66

        // PO: IntStream — jedno rozpakowanie na element, dalej już czyste int-y i gotowe sum().
        int sumPrimitive = numbers.stream()
                .mapToInt(Integer::intValue)   // mapToInt = przekształć (mapuj) na int; intValue = wartość int
                .sum();                        // sum = suma
        show("suma przez mapToInt(...).sum()", sumPrimitive);
        // WYNIK: suma przez mapToInt(...).sum() → 66

        // PUŁAPKA: Stream<Integer> NIE MA metody sum(). To się nie skompiluje:
        //     int sum = numbers.stream().sum();
        //     → error: cannot find symbol   symbol: method sum()   location: interface Stream<Integer>
        // Dlaczego? Stream<T> działa dla DOWOLNEGO T: String, Product, Customer... Stringów nie da się zsumować,
        // więc Stream<T> nie może mieć sum(). IntStream WIE, że ma liczby — dlatego ma sum(), average(), max().

        // Koszt pakowania w słowach (bez pomiarów czasu, bo zależą od komputera):
        //   • Integer to obiekt: nagłówek + pole int ≈ 16 bajtów zamiast 4 bajtów „gołego” int-a,
        //   • każdy Integer spoza zakresu -128..127 to NOWY obiekt → więcej pracy dla odśmiecacza (garbage collector),
        //   • wartości w pudełkach są rozrzucone po pamięci → procesor czyta je wolniej niż ciągłą tablicę int[],
        //   • Integer może być null, a int nie — rozpakowanie null kończy się wyjątkiem:
        expectThrows("rozpakowanie null w mapToInt", () -> Stream.of(1, null, 3).mapToInt(i -> i).sum());
        // WYNIK: ✔ rozpakowanie null w mapToInt → rzucono NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because "i" is null

        note("Stream<Integer> = liczby w pudełkach. IntStream = liczby luzem. Do liczenia → IntStream.");
        // WYNIK: ℹ Stream<Integer> = liczby w pudełkach. IntStream = liczby luzem. Do liczenia → IntStream.

        // DOBRA PRAKTYKA: liczysz sumę, średnią, min albo max? Najpierw mapToInt / mapToLong / mapToDouble.
    }

    // =================================================================================================
    // 2. TWORZENIE
    // =================================================================================================

    /**
     * 2. Skąd wziąć strumień prymitywny: range / rangeClosed, of, Arrays.stream, chars,
     * mapToInt / mapToLong / mapToDouble oraz flatMapToInt.
     */
    static void creation() {
        section("2. Tworzenie strumieni prymitywnych");

        // range(od, do) — „do” NIE wchodzi do wyniku (jak w pętli: for (int i = 1; i < 5; i++))
        show("IntStream.range(1, 5)", IntStream.range(1, 5).toArray());          // toArray = do tablicy
        // WYNIK: IntStream.range(1, 5) → [1, 2, 3, 4]
        show("IntStream.rangeClosed(1, 5)", IntStream.rangeClosed(1, 5).toArray());
        // WYNIK: IntStream.rangeClosed(1, 5) → [1, 2, 3, 4, 5]
        show("IntStream.range(5, 1)", IntStream.range(5, 1).toArray());          // start ≥ koniec → pusto, bez wyjątku
        // WYNIK: IntStream.range(5, 1) → []

        show("IntStream.of(3, 1, 2)", IntStream.of(3, 1, 2).toArray());            // of = z (podanych wartości)
        // WYNIK: IntStream.of(3, 1, 2) → [3, 1, 2]

        int[] temperatures = {-3, 0, 4, 7};
        show("Arrays.stream(int[]).sum()", Arrays.stream(temperatures).sum());    // Arrays.stream = strumień z tablicy
        // WYNIK: Arrays.stream(int[]).sum() → 8

        // chars() — String jako IntStream KODÓW znaków (liczby, nie litery!)
        show("\"kot\".chars()", "kot".chars().toArray());
        // WYNIK: "kot".chars() → [107, 111, 116]
        show("\"żółw\".chars()", "żółw".chars().toArray());
        // WYNIK: "żółw".chars() → [380, 243, 322, 119]

        // PUŁAPKA: chars() liczy jednostki UTF-16, a nie „znaki, które widzisz”. Emoji zajmuje dwie jednostki.
        String smile = "😀";   // jedno emoji (uśmiechnięta buźka) zapisane dwiema jednostkami UTF-16
        show("emoji: chars().count()", smile.chars().count());
        // WYNIK: emoji: chars().count() → 2
        show("emoji: codePoints().count()", smile.codePoints().count());   // codePoints = punkty kodowe (pełne znaki)
        // WYNIK: emoji: codePoints().count() → 1

        // mapToInt / mapToLong / mapToDouble — z obiektów do liczb (najczęstsza droga w praktyce)
        List<Product> products = SampleData.products();
        show("suma sztuk w magazynie", products.stream().mapToInt(Product::stock).sum());
        // WYNIK: suma sztuk w magazynie → 595

        List<Employee> employees = SampleData.employees();
        show("mapToLong: suma pensji", employees.stream().mapToLong(Employee::salary).sum());
        // WYNIK: mapToLong: suma pensji → 106400
        show("mapToDouble: pensje w tys.", employees.stream()
                .mapToDouble(e -> e.salary() / 1000.0)   // mapToDouble = przekształć na double
                .limit(3)
                .toArray());
        // WYNIK: mapToDouble: pensje w tys. → [14.5, 9.8, 7.2]

        // flatMapToInt = spłaszcz do IntStream (jak flatMap ze Streams04, ale od razu na liczby)
        long allLetters = SampleData.words().stream()
                .flatMapToInt(String::chars)   // każde słowo → jego znaki; wszystko w jednym IntStream
                .count();                       // count = policz (zwraca long)
        show("liczba liter we wszystkich słowach", allLetters);
        // WYNIK: liczba liter we wszystkich słowach → 65
    }

    // =================================================================================================
    // 3. SUM / AVERAGE / MIN / MAX / COUNT
    // =================================================================================================

    /**
     * 3. Gotowe operacje końcowe. Zwróć uwagę na TYPY wyników: sum() → int, count() → long,
     * average() → OptionalDouble, min()/max() → OptionalInt.
     */
    static void terminalMath() {
        section("3. sum, average, min, max, count");

        List<Employee> employees = SampleData.employees();

        // Strumień da się zużyć tylko raz (Streams01), więc każda operacja dostaje NOWY strumień.
        show("sum()", employees.stream().mapToInt(Employee::salary).sum());
        // WYNIK: sum() → 106400
        show("average()", employees.stream().mapToInt(Employee::salary).average());   // average = średnia
        // WYNIK: average() → OptionalDouble[10640.0]
        show("min()", employees.stream().mapToInt(Employee::salary).min());           // min = najmniejsza
        // WYNIK: min() → OptionalInt[7200]
        show("max()", employees.stream().mapToInt(Employee::salary).max());           // max = największa
        // WYNIK: max() → OptionalInt[17200]
        show("count()", employees.stream().mapToInt(Employee::salary).count());
        // WYNIK: count() → 10

        // OptionalInt to „Optional dla int”: getAsInt(), orElse(...), isPresent(), ifPresent(...)
        OptionalInt youngest = employees.stream().mapToInt(Employee::age).min();
        show("najmłodszy ma lat", youngest.getAsInt());   // getAsInt = pobierz jako int (tu wiemy, że lista nie jest pusta)
        // WYNIK: najmłodszy ma lat → 27

        // PUŁAPKA: dzielenie sumy przez liczbę elementów na int-ach obcina część ułamkową.
        List<Integer> numbers = SampleData.numbers();
        int sum = numbers.stream().mapToInt(Integer::intValue).sum();
        show("sum / size (int / int)", sum / numbers.size());
        // WYNIK: sum / size (int / int) → 5
        show("average()", numbers.stream().mapToInt(Integer::intValue).average().orElse(0.0));
        // WYNIK: average() → 5.5

        // PUŁAPKA: IntStream „gubi” obiekt. max() powie ILE, ale nie KTO.
        // Chcesz osobę z najwyższą pensją? Zostań przy Stream<Employee> i użyj max(Comparator) (Streams06).
        show("kto zarabia najwięcej", employees.stream()
                .max(Comparator.comparingInt(Employee::salary))   // comparingInt = porównuj po int
                .orElseThrow());                                             // orElseThrow (Java 10+) = albo rzuć wyjątek
        // WYNIK: kto zarabia najwięcej → Michał Lewandowski (IT, 17200 zł)
    }

    // =================================================================================================
    // 4. AVERAGE() ZWRACA OPTIONALDOUBLE — BO BYWA PUSTO
    // =================================================================================================

    /**
     * 4. Średnia z pustego zbioru nie istnieje (0 / 0), dlatego average() zwraca OptionalDouble.
     * Za to sum() pustego strumienia to po prostu 0.
     */
    static void averageIsOptional() {
        section("4. average() → OptionalDouble (pusty!)");

        List<Employee> employees = SampleData.employees();

        OptionalDouble logistics = employees.stream()
                .filter(e -> e.department() == Department.LOGISTYKA)   // w LOGISTYKA nie ma nikogo
                .mapToInt(Employee::salary)
                .average();
        show("średnia pensja w LOGISTYKA", logistics);
        // WYNIK: średnia pensja w LOGISTYKA → OptionalDouble.empty
        show("isPresent()", logistics.isPresent());   // isPresent = czy jest obecna (wartość)
        // WYNIK: isPresent() → false

        // PUŁAPKA: getAsDouble() na pustym OptionalDouble rzuca wyjątek — tak samo jak get() w Optional.
        expectThrows("getAsDouble() na pustym", logistics::getAsDouble);   // getAsDouble = pobierz jako double
        // WYNIK: ✔ getAsDouble() na pustym → rzucono NoSuchElementException: No value present

        show("orElse(0.0)", logistics.orElse(0.0));   // orElse = albo (wartość domyślna)
        // WYNIK: orElse(0.0) → 0.0

        // sum() nie potrzebuje Optional: suma niczego to 0 (element neutralny dodawania — Streams07).
        show("IntStream.empty().sum()", IntStream.empty().sum());   // empty = pusty
        // WYNIK: IntStream.empty().sum() → 0
        show("IntStream.empty().max()", IntStream.empty().max());
        // WYNIK: IntStream.empty().max() → OptionalInt.empty
        show("IntStream.empty().count()", IntStream.empty().count());
        // WYNIK: IntStream.empty().count() → 0

        // DOBRA PRAKTYKA: zastanów się, co ZNACZY brak danych. Czasem 0.0 jest OK (raport),
        // czasem lepiej pokazać „brak danych” (np. ifPresentOrElse z Java 9+ — t14_optional/Optional02Transform).
        logistics.ifPresentOrElse(                       // ifPresentOrElse (Java 9+) = jeśli jest, to…, w przeciwnym razie…
                avg -> note("średnia: " + avg),
                () -> note("LOGISTYKA: brak danych do średniej"));
        // WYNIK: ℹ LOGISTYKA: brak danych do średniej
    }

    // =================================================================================================
    // 5. SUMMARYSTATISTICS — WSZYSTKO W JEDNYM PRZEJŚCIU
    // =================================================================================================

    /**
     * 5. summaryStatistics() liczy count, sum, min, max i average w JEDNYM przejściu po danych.
     * Zamiast pięciu strumieni — jeden.
     */
    static void summaryStatistics() {
        section("5. summaryStatistics() — pięć wyników naraz");

        IntSummaryStatistics stats = SampleData.employees().stream()   // IntSummaryStatistics = statystyki zbiorcze int
                .mapToInt(Employee::salary)
                .summaryStatistics();

        show("getCount()", stats.getCount());       // getCount = pobierz liczbę elementów (long)
        // WYNIK: getCount() → 10
        show("getSum()", stats.getSum());           // getSum = pobierz sumę (long! nie int)
        // WYNIK: getSum() → 106400
        show("getMin()", stats.getMin());           // getMin = pobierz minimum
        // WYNIK: getMin() → 7200
        show("getMax()", stats.getMax());           // getMax = pobierz maksimum
        // WYNIK: getMax() → 17200
        show("getAverage()", stats.getAverage());   // getAverage = pobierz średnią (double, bez Optional)
        // WYNIK: getAverage() → 10640.0

        // PUŁAPKA: toString() statystyk formatuje średnią wg DOMYŚLNYCH ustawień regionalnych komputera
        // (na polskim Windowsie „10640,000000”, na angielskim „10640.000000”). Do raportów używaj getterów
        // i String.format(Locale.ROOT, ...).
        show("toString()", stats);
        // (wynik zależy od ustawień regionalnych komputera)
        show("własny format", String.format(Locale.ROOT, "średnio %.2f zł (od %d do %d)",
                stats.getAverage(), stats.getMin(), stats.getMax()));
        // WYNIK: własny format → średnio 10640.00 zł (od 7200 do 17200)

        // PUŁAPKA: statystyki PUSTEGO strumienia nie rzucają wyjątku, tylko zwracają „dziwne” wartości.
        IntSummaryStatistics none = IntStream.empty().summaryStatistics();
        show("pusty: getMin()", none.getMin());
        // WYNIK: pusty: getMin() → 2147483647    ← Integer.MAX_VALUE, a nie „brak”!
        show("pusty: getMax()", none.getMax());
        // WYNIK: pusty: getMax() → -2147483648    ← Integer.MIN_VALUE
        show("pusty: getAverage()", none.getAverage());
        // WYNIK: pusty: getAverage() → 0.0
        // DOBRA PRAKTYKA: zanim pokażesz min/max ze statystyk, sprawdź getCount() > 0.
    }

    // =================================================================================================
    // 6. POWRÓT DO OBIEKTÓW: BOXED / MAPTOOBJ
    // =================================================================================================

    /**
     * 6. Z IntStream wracamy do świata obiektów przez boxed() (int → Integer) albo mapToObj(...)
     * (int → dowolny obiekt). IntStream nie ma toList() — najpierw trzeba „zapakować”.
     */
    static void backToObjects() {
        section("6. boxed() i mapToObj()");

        List<Integer> squares = IntStream.rangeClosed(1, 5)
                .map(i -> i * i)   // map na IntStream: int → int
                .boxed()           // boxed = zapakuj: IntStream → Stream<Integer>
                .toList();         // toList (Java 16+) = do listy
        show("kwadraty", squares);
        // WYNIK: kwadraty → [1, 4, 9, 16, 25]

        List<String> rooms = IntStream.rangeClosed(1, 3)
                .mapToObj(i -> "Pokój " + i)   // mapToObj = przekształć na obiekt
                .toList();
        show("mapToObj", rooms);
        // WYNIK: mapToObj → [Pokój 1, Pokój 2, Pokój 3]

        List<Character> letters = "stream".chars()
                .mapToObj(c -> (char) c)   // kod znaku (int) → char → Character (zapakowany)
                .toList();
        show("litery słowa", letters);
        // WYNIK: litery słowa → [s, t, r, e, a, m]

        // Praktyczny trik: range po INDEKSACH, gdy potrzebny numer elementu.
        List<String> words = List.of("kawa", "herbata", "sok");
        List<String> menu = IntStream.range(0, words.size())
                .mapToObj(i -> (i + 1) + ". " + words.get(i))
                .toList();
        show("menu z numerami", menu);
        // WYNIK: menu z numerami → [1. kawa, 2. herbata, 3. sok]

        // PUŁAPKA: IntStream NIE MA toList() ani collect(Collectors.toList()). Nie skompiluje się:
        //     IntStream.range(0, 3).toList();                        → cannot find symbol: method toList()
        //     IntStream.range(0, 3).collect(Collectors.toList());    → collect w IntStream wymaga 3 argumentów
        // Rozwiązanie: .boxed().toList()  albo  .toArray() (int[]).

        // PUŁAPKA: Stream.of(int[]) NIE rozbija tablicy int[] na elementy — dostajesz Stream<int[]> z JEDNYM elementem.
        int[] array = {1, 2, 3};
        show("Stream.of(int[]).count()", Stream.of(array).count());
        // WYNIK: Stream.of(int[]).count() → 1    ← jeden element: cała tablica
        show("Arrays.stream(int[]).count()", Arrays.stream(array).count());
        // WYNIK: Arrays.stream(int[]).count() → 3
        show("IntStream.of(int[]).count()", IntStream.of(array).count());
        // WYNIK: IntStream.of(int[]).count() → 3
    }

    // =================================================================================================
    // 7. KONWERSJE: ASLONGSTREAM / ASDOUBLESTREAM
    // =================================================================================================

    /**
     * 7. asLongStream() i asDoubleStream() poszerzają typ (int → long, int → double) — bez utraty danych.
     * W drugą stronę (double → int) trzeba jawnie rzutować i część ułamkowa przepada.
     */
    static void conversions() {
        section("7. asLongStream(), asDoubleStream()");

        // Silnia 20! nie mieści się w int, ale mieści się w long.
        long factorialLong = IntStream.rangeClosed(1, 20)
                .asLongStream()                 // asLongStream = jako LongStream (int → long)
                .reduce(1, (a, b) -> a * b);
        show("20! na long", factorialLong);
        // WYNIK: 20! na long → 2432902008176640000

        int factorialInt = IntStream.rangeClosed(1, 20).reduce(1, (a, b) -> a * b);
        show("20! na int", factorialInt);
        // WYNIK: 20! na int → -2102132736    ← przepełnienie, bzdura bez żadnego ostrzeżenia

        // asDoubleStream — gdy dalej chcemy dzielić „po ludzku”, a nie całkowicie
        show("połówki (asDoubleStream)", IntStream.rangeClosed(1, 3)
                .asDoubleStream()               // asDoubleStream = jako DoubleStream (int → double)
                .map(x -> x / 2)
                .toArray());
        // WYNIK: połówki (asDoubleStream) → [0.5, 1.0, 1.5]
        show("połówki (int / 2)", IntStream.rangeClosed(1, 3).map(x -> x / 2).toArray());
        // WYNIK: połówki (int / 2) → [0, 1, 1]    ← dzielenie całkowite

        // double → int: mapToInt z rzutowaniem (int) OBCINA część ułamkową (nie zaokrągla!)
        show("(int) 2.9, (int) -2.9", Stream.of(2.9, -2.9).mapToInt(d -> (int) d.doubleValue()).toArray());
        // WYNIK: (int) 2.9, (int) -2.9 → [2, -2]
        show("Math.round", Stream.of(2.9, -2.9).mapToLong(Math::round).toArray());   // round = zaokrąglij
        // WYNIK: Math.round → [3, -3]

        // DOBRA PRAKTYKA: DoubleStream nie nadaje się do pieniędzy (błędy zaokrągleń) — tam BigDecimal
        // i reduce(BigDecimal.ZERO, BigDecimal::add) — t16_streams/Streams15BigDecimalMoney.
    }

    // =================================================================================================
    // 8. ITERATE I GENERATE
    // =================================================================================================

    /**
     * 8. IntStream.iterate i IntStream.generate — ciągi liczb bez tablicy i bez pętli.
     * Dwuargumentowe iterate jest nieskończone, więc potrzebuje limit().
     */
    static void iterateAndGenerate() {
        section("8. IntStream.iterate / generate");

        show("potęgi dwójki", IntStream.iterate(1, i -> i * 2)   // iterate(start, następny)
                .limit(10)                                        // limit = ogranicz (BEZ tego — nieskończoność!)
                .toArray());
        // WYNIK: potęgi dwójki → [1, 2, 4, 8, 16, 32, 64, 128, 256, 512]

        // Trzyargumentowe iterate (Java 9+) = pętla for: start; warunek; krok. Warunek sprawdzany PRZED elementem.
        show("iterate(1, i <= 100, i * 3)", IntStream.iterate(1, i -> i <= 100, i -> i * 3).toArray());
        // WYNIK: iterate(1, i <= 100, i * 3) → [1, 3, 9, 27, 81]

        show("generate(() -> 7).limit(3)", IntStream.generate(() -> 7).limit(3).toArray());   // generate = wytwarzaj
        // WYNIK: generate(() -> 7).limit(3) → [7, 7, 7]

        // PRZED: pętla — suma liczb parzystych 1..10
        int evenSumLoop = 0;
        for (int i = 1; i <= 10; i++) {
            if (i % 2 == 0) {
                evenSumLoop += i;
            }
        }
        // PO: IntStream — to samo, bez zmiennej, którą trzeba zmieniać
        int evenSumStream = IntStream.rangeClosed(1, 10).filter(i -> i % 2 == 0).sum();
        show("suma parzystych (pętla, stream)", evenSumLoop + ", " + evenSumStream);
        // WYNIK: suma parzystych (pętla, stream) → 30, 30

        // PUŁAPKA: IntStream.iterate(1, i -> i * 2).sum() — bez limit() NIGDY się nie skończy
        // (liczby się przepełniają i kręcą w kółko, a strumień wciąż jest nieskończony). Nie uruchamiamy tego.
        // PUŁAPKA: filter nie zatrzymuje nieskończonego strumienia — zatrzymuje go dopiero limit / takeWhile (Java 9+).
        show("takeWhile(i < 50)", IntStream.iterate(1, i -> i * 2)
                .takeWhile(i -> i < 50)          // takeWhile (Java 9+) = bierz, dopóki warunek jest prawdziwy
                .toArray());
        // WYNIK: takeWhile(i < 50) → [1, 2, 4, 8, 16, 32]
    }

    // =================================================================================================
    // 9. PRZEPEŁNIENIE SUMY → MAPTOLONG
    // =================================================================================================

    /**
     * 9. IntStream.sum() zwraca int i po cichu się przepełnia. Duże sumy licz na long (mapToLong)
     * albo pilnuj ich przez Math::addExact.
     */
    static void overflow() {
        section("9. Przepełnienie sumy → mapToLong");

        List<Employee> employees = SampleData.employees();

        // Fundusz płac w GROSZACH na 20 lat: pensja × 100 gr × 12 miesięcy × 20 lat = pensja × 24 000.
        // Pojedyncza wartość mieści się w int (max 17200 × 24000 = 412 800 000), ale SUMA już nie.
        int wrong = employees.stream().mapToInt(e -> e.salary() * 24_000).sum();
        show("int: suma", wrong);
        // WYNIK: int: suma → -1741367296    ← ujemny fundusz płac? Przepełnienie!

        long right = employees.stream().mapToLong(e -> e.salary() * 24_000L).sum();   // 24_000L → mnożenie na long
        show("long: suma", right);
        // WYNIK: long: suma → 2553600000

        // Ciekawostka: statystyki i average() liczą sumę wewnętrznie na long — tu się nie przepełniają.
        IntSummaryStatistics stats = employees.stream().mapToInt(e -> e.salary() * 24_000).summaryStatistics();
        show("summaryStatistics().getSum()", stats.getSum());
        // WYNIK: summaryStatistics().getSum() → 2553600000
        show("average()", employees.stream().mapToInt(e -> e.salary() * 24_000).average());
        // WYNIK: average() → OptionalDouble[2.5536E8]

        // Chcesz, żeby przepełnienie było GŁOŚNE? Math.addExact rzuca wyjątek zamiast zwracać bzdurę.
        expectThrows("reduce(0, Math::addExact)", () -> employees.stream()
                .mapToInt(e -> e.salary() * 24_000)
                .reduce(0, Math::addExact));    // addExact = dodaj dokładnie (albo rzuć wyjątek)
        // WYNIK: ✔ reduce(0, Math::addExact) → rzucono ArithmeticException: integer overflow

        // PUŁAPKA: mapToLong(e -> e.salary() * 24_000) — mnożenie dzieje się jeszcze na INT (oba argumenty to int),
        // a dopiero wynik jest zamieniany na long. Przy większych liczbach przepełni się już pojedynczy element.
        // Dlatego piszemy 24_000L (literał long) — wtedy całe mnożenie jest na long.
        // DOBRA PRAKTYKA: sumujesz dużo dużych liczb (grosze, bajty, milisekundy)? Od razu mapToLong.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • IntStream / LongStream / DoubleStream = liczby bez pudełek → szybciej, mniej pamięci, gotowa matematyka.
     *   • Tworzenie: range(a, b) (bez b), rangeClosed(a, b) (z b), of(...), Arrays.stream(int[]), "tekst".chars(),
     *     mapToInt / mapToLong / mapToDouble, flatMapToInt, iterate, generate.
     *   • sum() → int/long/double (pusty = 0); count() → long; average() → OptionalDouble (pusty = empty!);
     *     min()/max() → OptionalInt / OptionalLong / OptionalDouble.
     *   • summaryStatistics() = count, sum (long), min, max, average w jednym przejściu; pusty → min = MAX_VALUE.
     *   • Powrót do obiektów: boxed() (int → Integer), mapToObj(...) (int → cokolwiek). IntStream nie ma toList().
     *   • asLongStream() / asDoubleStream() poszerzają typ; (int) obcina, Math.round zaokrągla.
     *   • sum() na int przepełnia się po cichu → mapToLong(... * 24_000L) albo reduce(0, Math::addExact).
     *   • Stream.of(int[]) = jeden element (tablica)! Do int[] używaj Arrays.stream albo IntStream.of.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego Stream<Integer> nie ma metody sum(), a IntStream ma?
     *   2. Co wypisze:  System.out.println(IntStream.range(1, 4).sum());  ?
     *   3. Co wypisze:  System.out.println(IntStream.empty().average());  ?
     *   4. ZNAJDŹ BŁĄD:  List<Integer> list = List.of(1, 2, 3);  int total = list.stream().sum();
     *   5. ZNAJDŹ BŁĄD:  long payroll = employees.stream().mapToInt(e -> e.salary() * 1_000_000).sum();
     *   6. Czym różni się boxed() od mapToObj(...)? Kiedy użyjesz którego?
     *   7. Co wypisze:  System.out.println(Stream.of(new int[]{1, 2, 3}).count());  ?
     *   8. Co zwraca IntStream.empty().summaryStatistics().getMin() i dlaczego to niebezpieczne?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: suma sztuk w kategorii ELEKTRONIKA", 36, () -> exercise1(SampleData.products()));
        Check.equal("ćw. 2: średni wiek w dziale IT", 34.25, () -> exercise2(SampleData.employees()));
        Check.equal("ćw. 3: suma kwadratów wielokrotności 3 do 10", 126, () -> exercise3(10));
        Check.equal("ćw. 4: liczba liter 'a' w zdaniach", 10L, () -> exercise4(SampleData.sentences()));
        Check.equal("ćw. 5: średnia wszystkich ocen", "3.86", () -> exercise5(SampleData.students()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 36, () -> solution1(SampleData.products()));
        Check.equal("ćw. 2 (wzorzec)", 34.25, () -> solution2(SampleData.employees()));
        Check.equal("ćw. 3 (wzorzec)", 126, () -> solution3(10));
        Check.equal("ćw. 4 (wzorzec)", 10L, () -> solution4(SampleData.sentences()));
        Check.equal("ćw. 5 (wzorzec)", "3.86", () -> solution5(SampleData.students()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz, ile sztuk (stock) leży w magazynie w kategorii ELEKTRONIKA.
     * Podpowiedź: filter po category() → mapToInt(Product::stock) → sum().
     */
    static int exercise1(List<Product> products) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (łatwe): średni wiek pracowników działu IT. Gdy nikogo nie ma — zwróć 0.0.
     * Podpowiedź: mapToInt(Employee::age).average() zwraca OptionalDouble → orElse(0.0).
     */
    static double exercise2(List<Employee> employees) {
        // TODO: twoje rozwiązanie
        return 0.0;
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ pętlę na IntStream — bez zmiennej sum i bez if.
     * <pre>{@code
     * int sum = 0;
     * for (int i = 1; i <= n; i++) {
     *     if (i % 3 == 0) {
     *         sum += i * i;
     *     }
     * }
     * return sum;
     * }</pre>
     * Podpowiedź: IntStream.rangeClosed(1, n) → filter → map(i -> i * i) → sum().
     */
    static int exercise3(int n) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (średnie): policz wszystkie litery 'a' (małe i wielkie) we wszystkich zdaniach.
     * Podpowiedź: flatMapToInt(String::chars) → map(Character::toLowerCase) → filter(c -> c == 'a') → count().
     */
    static long exercise4(List<String> sentences) {
        // TODO: twoje rozwiązanie
        return 0L;
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): średnia WSZYSTKICH ocen wszystkich studentów razem (każda ocena liczy się
     * tak samo), sformatowana do 2 miejsc po kropce: String.format(Locale.ROOT, "%.2f", ...).
     * Student bez ocen (Henryk) niczego nie dokłada. Gdy nie ma żadnych ocen — "0.00".
     * Podpowiedź: flatMapToInt(s -> s.grades().stream().mapToInt(Integer::intValue)) → average() → orElse(0.0).
     */
    static String exercise5(List<Student> students) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(List<Product> products) {
        return products.stream()
                .filter(p -> p.category() == Category.ELEKTRONIKA)
                .mapToInt(Product::stock)
                .sum();
    }

    static double solution2(List<Employee> employees) {
        return employees.stream()
                .filter(e -> e.department() == Department.IT)
                .mapToInt(Employee::age)
                .average()
                .orElse(0.0);
    }

    static int solution3(int n) {
        return IntStream.rangeClosed(1, n)
                .filter(i -> i % 3 == 0)
                .map(i -> i * i)
                .sum();
    }

    static long solution4(List<String> sentences) {
        return sentences.stream()
                .flatMapToInt(String::chars)
                .map(Character::toLowerCase)   // wersja Character.toLowerCase(int) — działa na kodach znaków
                .filter(c -> c == 'a')
                .count();
    }

    static String solution5(List<Student> students) {
        double average = students.stream()
                .flatMapToInt(s -> s.grades().stream().mapToInt(Integer::intValue))
                .average()
                .orElse(0.0);
        return String.format(Locale.ROOT, "%.2f", average);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Stream<T> jest ogólny — T może być Stringiem albo Productem, których nie da się dodawać. IntStream wie,
     *      że trzyma int-y, więc może mieć sum(), average(), min(), max() i summaryStatistics().
     *   2. 6  (1 + 2 + 3; range nie bierze końca 4).
     *   3. OptionalDouble.empty  — średnia z niczego nie istnieje, dlatego wynik jest „opcjonalny”.
     *   4. Stream<Integer> nie ma sum() → błąd kompilacji. Poprawnie: list.stream().mapToInt(Integer::intValue).sum().
     *   5. Dwa błędy: mnożenie e.salary() * 1_000_000 przepełnia int już dla pojedynczej pensji (> 2 147 483 647),
     *      a sum() na IntStream przepełnia się przy sumowaniu. Poprawnie: mapToLong(e -> e.salary() * 1_000_000L).sum().
     *   6. boxed() zamienia int → Integer (ta sama liczba, tylko w pudełku). mapToObj(f) zamienia int → dowolny obiekt
     *      (String, Product...). Do listy liczb: boxed().toList(); do napisów/obiektów: mapToObj(...).
     *   7. 1 — Stream.of(int[]) traktuje całą tablicę jako JEDEN element (Stream<int[]>). Użyj Arrays.stream / IntStream.of.
     *   8. Integer.MAX_VALUE (2147483647). Nie ma wyjątku ani Optional, więc łatwo pokazać bzdurę jako „minimum”.
     *      Najpierw sprawdź getCount() > 0.
     */
    // </editor-fold>
}
