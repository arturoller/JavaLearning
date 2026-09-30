package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Order;
import helpers.model.OrderLine;
import helpers.model.OrderStatus;
import helpers.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: reduce — składanie strumienia w jeden wynik
 *        (reduce = zredukuj, złóż; identity = element neutralny; accumulator = akumulator;
 *         combiner = łącznik wyników częściowych)
 *
 * W SKRÓCIE:
 *   reduce bierze wszystkie elementy i „składa” je parami w JEDEN wynik: sumę, iloczyn, maksimum,
 *   najdłuższe słowo, łączną kwotę zamówień. To uogólnienie pętli z akumulatorem (int sum = 0; sum += x).
 *
 * ANALOGIA: Liczenie pieniędzy z portfela. Zaczynasz od 0 zł w głowie (identity). Bierzesz monetę
 *   po monecie i dodajesz do tego, co masz w głowie (accumulator). Gdy liczycie we dwoje (parallel),
 *   każdy liczy swoją kupkę od 0, a na końcu dodajecie swoje wyniki (combiner).
 *
 * JAK TO DZIAŁA:
 *   reduce(0, (acc, n) -> acc + n) na [5, 3, 8, 1]:
 *     acc = 0                ← identity (element neutralny)
 *     acc = 0 + 5   = 5
 *     acc = 5 + 3   = 8
 *     acc = 8 + 8   = 16
 *     acc = 16 + 1  = 17     ← wynik
 *
 *   Trzy odmiany:
 *     reduce(identity, accumulator)            → T            (pusty strumień → identity)
 *     reduce(accumulator)                      → Optional     (pusty strumień → Optional.empty)
 *     reduce(identity, accumulator, combiner)  → U            (wynik innego typu niż elementy)
 *
 *   Dwa warunki poprawności (inaczej parallel da ZŁY wynik):
 *     • identity jest NEUTRALNE:  op(identity, x) == x     (0 dla +, 1 dla *, "" dla sklejania)
 *     • operacja jest ŁĄCZNA:     (a op b) op c == a op (b op c)   (+, *, max — tak; -, /, średnia — NIE)
 *
 * SŁÓWKA:
 *   fold = składać (zwijać); identity = tożsamość, element neutralny; accumulator = akumulator („zbieracz”);
 *   combiner = łącznik; associative = łączny; neutral = neutralny; partial result = wynik częściowy;
 *   sum = suma; product = iloczyn; join = połącz, sklej
 *
 * ZOBACZ TEŻ: t16_streams/Streams06TerminalOps (inne operacje końcowe), t15_numbers/Numbers01BigDecimal
 *   (BigDecimal: add, compareTo), t16_streams/Streams08PrimitiveStreams (sum, average bez reduce),
 *   t16_streams/Streams09CollectorsBasic (collect i joining), t16_streams/Streams18Parallel (strumienie równoległe)
 * </pre>
 */
public class Streams07Reduce {

    public static void main(String[] args) {
        title("Streams07 — reduce: składanie w jeden wynik");

        foldingIdea();              // folding idea = idea składania
        reduceWithIdentity();       // reduce with identity = reduce z elementem neutralnym
        reduceWithoutIdentity();    // reduce without identity = reduce bez elementu neutralnego → Optional
        identityMustBeNeutral();    // identity must be neutral = identity musi być neutralne
        associativity();            // associativity = łączność operacji
        threeArgReduce();           // three-arg reduce = reduce z trzema argumentami (zmiana typu)
        bigDecimalSum();            // BigDecimal sum = sumowanie kwot BigDecimal
        reduceVsAlternatives();     // reduce vs alternatives = reduce vs sum()/max()/collect
        stringConcatPitfall();      // string concat pitfall = pułapka sklejania tekstów przez reduce
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. IDEA SKŁADANIA
    // =================================================================================================

    /**
     * 1. reduce to pętla z akumulatorem zapisana jako jedna operacja. Śledzimy każdy krok.
     */
    static void foldingIdea() {
        section("1. Idea: składanie elementów parami w jeden wynik");

        // PRZED: pętla z akumulatorem.
        int loopSum = 0;
        for (int n : List.of(5, 3, 8, 1)) {        // List.of (Java 9+) = niemodyfikowalna lista
            loopSum += n;
        }
        show("pętla", loopSum);
        // WYNIK: pętla → 17

        // PO: reduce(identity, accumulator). println w lambdzie to efekt uboczny — tylko do pokazu kroków.
        int traced = Stream.of(5, 3, 8, 1).reduce(0, (acc, n) -> {   // acc = akumulator (dotychczasowy wynik)
            int next = acc + n;
            System.out.println("   acc=" + acc + " + n=" + n + " → " + next);
            return next;
        });
        // WYNIK: acc=0 + n=5 → 5
        // WYNIK: acc=5 + n=3 → 8
        // WYNIK: acc=8 + n=8 → 16
        // WYNIK: acc=16 + n=1 → 17
        show("reduce", traced);
        // WYNIK: reduce → 17

        // Akumulator dostaje DWA argumenty: (wynik do tej pory, kolejny element) i zwraca NOWY wynik.
        // Nie zmienia niczego „na boku” — tylko zwraca wartość. Dzięki temu reduce działa też równolegle.
    }

    // =================================================================================================
    // 2. reduce(identity, accumulator) — SUMA, ILOCZYN, MAX, SKLEJANIE
    // =================================================================================================

    /**
     * 2. Wersja z elementem neutralnym zwraca zwykłą wartość (nie Optional) — dla pustego strumienia daje identity.
     */
    static void reduceWithIdentity() {
        section("2. reduce(identity, accumulator) — suma, iloczyn, max, sklejanie");

        List<Integer> numbers = SampleData.numbers();   // [5, 3, 8, 1, 9, 2, 7, 3, 10, 6, 4, 8]

        show("suma: reduce(0, Integer::sum)", numbers.stream().reduce(0, Integer::sum));      // Integer::sum = (a, b) -> a + b
        // WYNIK: suma: reduce(0, Integer::sum) → 66
        show("iloczyn 1..5: reduce(1, a * b)", Stream.of(1, 2, 3, 4, 5).reduce(1, (a, b) -> a * b));
        // WYNIK: iloczyn 1..5: reduce(1, a * b) → 120
        show("max: reduce(MIN_VALUE, Math::max)", numbers.stream().reduce(Integer.MIN_VALUE, Math::max));
        // WYNIK: max: reduce(MIN_VALUE, Math::max) → 10
        show("sklejanie: reduce(\"\", String::concat)", Stream.of("a", "b", "c").reduce("", String::concat)); // concat = sklej
        // WYNIK: sklejanie: reduce("", String::concat) → abc

        // Pusty strumień → dostajesz identity.
        show("pusty: reduce(0, Integer::sum)", Stream.<Integer>empty().reduce(0, Integer::sum));
        // WYNIK: pusty: reduce(0, Integer::sum) → 0

        // PUŁAPKA: iloczyn szybko przepełnia int. 13! = 6 227 020 800 > Integer.MAX_VALUE (2 147 483 647).
        show("13! jako int (ŹLE)", IntStream.rangeClosed(1, 13).reduce(1, (a, b) -> a * b));
        // WYNIK: 13! jako int (ŹLE) → 1932053504    ← przepełnienie, bez żadnego wyjątku
        show("13! jako long", LongStream.rangeClosed(1, 13).reduce(1L, (a, b) -> a * b));
        // WYNIK: 13! jako long → 6227020800
        // DOBRA PRAKTYKA: iloczyny licz w long; gdy i long nie starczy — BigInteger albo Math.multiplyExact (rzuca wyjątek).
    }

    // =================================================================================================
    // 3. reduce(accumulator) BEZ IDENTITY → Optional
    // =================================================================================================

    /**
     * 3. Bez elementu neutralnego reduce nie wie, co zwrócić dla pustego strumienia — więc zwraca {@code Optional}.
     * Pierwszy element staje się początkowym akumulatorem.
     */
    static void reduceWithoutIdentity() {
        section("3. reduce(accumulator) bez identity → Optional");

        Optional<Integer> sum = SampleData.numbers().stream().reduce(Integer::sum);
        show("reduce(Integer::sum)", sum);
        // WYNIK: reduce(Integer::sum) → Optional[66]
        show("pusty strumień", Stream.<Integer>empty().reduce(Integer::sum));
        // WYNIK: pusty strumień → Optional.empty

        // Najdłuższe słowo. Przy remisie (>=) zostaje WCZEŚNIEJSZE: „kolekcja” i „optional” mają po 8 liter.
        Optional<String> longest = SampleData.words().stream()
                .reduce((a, b) -> a.length() >= b.length() ? a : b);
        show("najdłuższe słowo", longest);
        // WYNIK: najdłuższe słowo → Optional[kolekcja]

        // Tu nie ma sensownego identity — dla max nie istnieje „najmniejszy możliwy String”, więc Optional to uczciwa odpowiedź.
        // DOBRA PRAKTYKA: dla min/max czytelniej jest napisać max(Comparator.comparingInt(String::length)).
        show("to samo przez max(...)", SampleData.words().stream().max(Comparator.comparingInt(String::length)));
        // WYNIK: to samo przez max(...) → Optional[kolekcja]
    }

    // =================================================================================================
    // 4. IDENTITY MUSI BYĆ NEUTRALNE
    // =================================================================================================

    /**
     * 4. identity nie jest „wartością startową, od której zaczynam liczyć”. To element NEUTRALNY:
     * {@code op(identity, x) == x}. W parallel identity jest używane w KAŻDYM kawałku osobno.
     */
    static void identityMustBeNeutral() {
        section("4. identity musi być neutralne (0 dla +, 1 dla *)");

        List<Integer> numbers = SampleData.numbers();

        // PUŁAPKA: „chcę suma + 10”, więc piszę reduce(10, ...). Sekwencyjnie wychodzi 76...
        show("reduce(10, Integer::sum) sekwencyjnie", numbers.stream().reduce(10, Integer::sum));
        // WYNIK: reduce(10, Integer::sum) sekwencyjnie → 76

        // ...ale strumień równoległy dzieli dane na kawałki i KAŻDY kawałek zaczyna od identity = 10.
        // Symulacja dla 2 kawałków (prawdziwy parallel robi to samo, tylko kawałków bywa więcej):
        int left = numbers.subList(0, 6).stream().reduce(10, Integer::sum);    // 10 + 5+3+8+1+9+2
        int right = numbers.subList(6, 12).stream().reduce(10, Integer::sum);  // 10 + 7+3+10+6+4+8
        show("kawałek lewy", left);
        // WYNIK: kawałek lewy → 38
        show("kawałek prawy", right);
        // WYNIK: kawałek prawy → 48
        show("combiner: 38 + 48", left + right);
        // WYNIK: combiner: 38 + 48 → 86    ← a nie 76! Dziesiątka doliczona dwa razy.

        System.out.println("   parallel naprawdę: " + numbers.parallelStream().reduce(10, Integer::sum));
        // (wynik zależy od uruchomienia i liczby rdzeni — zwykle więcej niż 76)

        // DOBRA PRAKTYKA: identity = element neutralny; „dodatek” dolicz POZA reduce.
        show("reduce(0, Integer::sum) + 10", numbers.stream().reduce(0, Integer::sum) + 10);
        // WYNIK: reduce(0, Integer::sum) + 10 → 76
        // Elementy neutralne: suma → 0; iloczyn → 1; max → Integer.MIN_VALUE; min → Integer.MAX_VALUE;
        // sklejanie tekstu → ""; „i” (AND) → true; „lub” (OR) → false.
    }

    // =================================================================================================
    // 5. ŁĄCZNOŚĆ — PUŁAPKA Z ODEJMOWANIEM I ŚREDNIĄ
    // =================================================================================================

    /**
     * 5. Operacja w reduce musi być łączna: {@code (a op b) op c == a op (b op c)}. Odejmowanie, dzielenie
     * i „średnia z dwóch” NIE są łączne — sekwencyjnie coś wyjdzie, ale parallel da inny wynik.
     */
    static void associativity() {
        section("5. Łączność — odejmowanie i średnia to pułapki");

        // Sekwencyjnie: ((((0 - 1) - 2) - 3) - 4) = -10
        show("[1, 2, 3, 4] reduce(0, a - b)", Stream.of(1, 2, 3, 4).reduce(0, (a, b) -> a - b));
        // WYNIK: [1, 2, 3, 4] reduce(0, a - b) → -10

        // Symulacja parallel na 2 kawałki: [1, 2] i [3, 4], potem łączenie tą samą operacją.
        int chunk1 = Stream.of(1, 2).reduce(0, (a, b) -> a - b);   // 0 - 1 - 2 = -3
        int chunk2 = Stream.of(3, 4).reduce(0, (a, b) -> a - b);   // 0 - 3 - 4 = -7
        show("kawałki: -3 i -7, łączenie: -3 - (-7)", chunk1 - chunk2);
        // WYNIK: kawałki: -3 i -7, łączenie: -3 - (-7) → 4    ← zupełnie inny wynik niż -10

        // PUŁAPKA: „średnia przez reduce” — (a + b) / 2 parami to NIE jest średnia wszystkich.
        Optional<Double> fakeAverage = Stream.of(2.0, 4.0, 9.0).reduce((a, b) -> (a + b) / 2);
        show("reduce((a + b) / 2)", fakeAverage);
        // WYNIK: reduce((a + b) / 2) → Optional[6.0]    ← ((2 + 4) / 2 + 9) / 2 = 6.0
        show("prawdziwa średnia", Stream.of(2.0, 4.0, 9.0).mapToDouble(Double::doubleValue).average());
        // WYNIK: prawdziwa średnia → OptionalDouble[5.0]

        // Test łączności „na kartce”: czy (a op b) op c == a op (b op c)?
        //   +, *, max, min, sklejanie tekstu, BigDecimal::add → TAK   (można w reduce)
        //   -, /, średnia z dwóch                            → NIE   (nie wolno w reduce)
        // DOBRA PRAKTYKA: odejmowanie zamień na dodawanie liczb przeciwnych: 100 - suma(wydatki) zamiast reduce(100, a - b).
    }

    // =================================================================================================
    // 6. reduce Z TRZEMA ARGUMENTAMI — ZMIANA TYPU; PO CO combiner
    // =================================================================================================

    /**
     * 6. {@code reduce(identity, accumulator, combiner)} — gdy wynik ma INNY typ niż elementy
     * (np. słowa → suma długości). combiner łączy wyniki częściowe i jest używany tylko w parallel.
     */
    static void threeArgReduce() {
        section("6. reduce(identity, accumulator, combiner) — zmiana typu");

        List<String> words = SampleData.words();

        // accumulator: (Integer suma, String słowo) → Integer;   combiner: (Integer, Integer) → Integer
        int totalLength = words.stream().reduce(
                0,                                        // identity: suma długości na start
                (sum, word) -> sum + word.length(),       // accumulator: dolicz długość słowa
                Integer::sum);                            // combiner: połącz dwie sumy częściowe
        show("suma długości słów", totalLength);
        // WYNIK: suma długości słów → 65

        // W strumieniu sekwencyjnym combiner NIE jest wywoływany (nie ma czego łączyć).
        int sequential = words.stream().reduce(0,
                (sum, word) -> sum + word.length(),
                (a, b) -> {
                    System.out.println("   combiner wywołany!");
                    return a + b;
                });
        show("sekwencyjnie (i ani jednego „combiner wywołany!”)", sequential);
        // WYNIK: sekwencyjnie (i ani jednego „combiner wywołany!”) → 65

        // W parallel combiner łączy sumy z kawałków. Liczymy wywołania bezpiecznym wątkowo licznikiem.
        AtomicInteger combinerCalls = new AtomicInteger();   // AtomicInteger = licznik bezpieczny dla wielu wątków
        int parallel = words.parallelStream().reduce(0,
                (sum, word) -> sum + word.length(),
                (a, b) -> {
                    combinerCalls.incrementAndGet();          // incrementAndGet = zwiększ o 1 i zwróć
                    return a + b;
                });
        show("parallel: wynik", parallel);
        // WYNIK: parallel: wynik → 65
        show("parallel: combiner był wywołany?", combinerCalls.get() > 0);
        // WYNIK: parallel: combiner był wywołany? → true
        // Ile razy dokładnie — (wynik zależy od uruchomienia i liczby rdzeni).

        // PUŁAPKA: combiner niezgodny z akumulatorem (np. (a, b) -> a * b) „działa” sekwencyjnie, bo nie jest wołany,
        // i psuje wynik dopiero w parallel. Combiner musi robić to samo „łączenie” co akumulator.
        // DOBRA PRAKTYKA: zamiast 3-argumentowego reduce zwykle wystarczy map + prostsza operacja:
        show("mapToInt(String::length).sum()", words.stream().mapToInt(String::length).sum());
        // WYNIK: mapToInt(String::length).sum() → 65
    }

    // =================================================================================================
    // 7. BigDecimal — reduce(BigDecimal.ZERO, BigDecimal::add)
    // =================================================================================================

    /**
     * 7. Dla pieniędzy (BigDecimal) nie ma {@code sum()} — używamy {@code reduce(BigDecimal.ZERO, BigDecimal::add)}.
     * ZERO jest neutralne, a dodawanie łączne, więc działa też w parallel.
     */
    static void bigDecimalSum() {
        section("7. BigDecimal: reduce(BigDecimal.ZERO, BigDecimal::add)");

        List<Product> products = SampleData.products();
        BigDecimal priceSum = products.stream()
                .map(Product::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);          // add = dodaj (zwraca NOWY BigDecimal)
        show("suma cen katalogowych", priceSum);
        // WYNIK: suma cen katalogowych → 13106.36

        BigDecimal stockValue = products.stream()
                .map(Product::stockValue)                // stockValue = cena × liczba sztuk
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        show("wartość magazynu", stockValue);
        // WYNIK: wartość magazynu → 80759.43

        List<Order> orders = SampleData.orders();
        BigDecimal allOrders = orders.stream().map(Order::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        show("suma wszystkich zamówień", allOrders);
        // WYNIK: suma wszystkich zamówień → 15948.96
        BigDecimal notCancelled = orders.stream()
                .filter(o -> o.status() != OrderStatus.ANULOWANE)
                .map(Order::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        show("bez anulowanych", notCancelled);
        // WYNIK: bez anulowanych → 12949.96

        // To samo „od dołu”: wszystkie pozycje (flatMap) → kwota pozycji → suma. Wynik musi się zgadzać.
        BigDecimal fromLines = orders.stream()
                .flatMap(o -> o.lines().stream())
                .map(OrderLine::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        show("z pozycji == z zamówień?", fromLines.compareTo(allOrders) == 0);  // compareTo, nie equals!
        // WYNIK: z pozycji == z zamówień? → true

        // PUŁAPKA: pieniądze w double tracą dokładność. BigDecimal liczy dokładnie.
        show("double: 0.1 + 0.2 + 0.3", Stream.of(0.1, 0.2, 0.3).reduce(0.0, Double::sum));
        // WYNIK: double: 0.1 + 0.2 + 0.3 → 0.6000000000000001
        show("BigDecimal: 0.1 + 0.2 + 0.3", Stream.of("0.1", "0.2", "0.3").map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add));
        // WYNIK: BigDecimal: 0.1 + 0.2 + 0.3 → 0.6

        // DOBRA PRAKTYKA: BigDecimal porównuj przez compareTo (0.6 i 0.60 mają różną skalę, więc equals da false).
        show("0.6 equals 0.60?", new BigDecimal("0.6").equals(new BigDecimal("0.60")));
        // WYNIK: 0.6 equals 0.60? → false
        show("0.6 compareTo 0.60 == 0?", new BigDecimal("0.6").compareTo(new BigDecimal("0.60")) == 0);
        // WYNIK: 0.6 compareTo 0.60 == 0? → true
    }

    // =================================================================================================
    // 8. reduce vs sum()/max() vs collect
    // =================================================================================================

    /**
     * 8. reduce jest uniwersalny, ale często jest lepsze narzędzie: {@code sum()/max()} na strumieniach
     * liczbowych (czytelniej, bez pakowania) i {@code collect} do zbierania w kontener (lista, mapa, tekst).
     */
    static void reduceVsAlternatives() {
        section("8. reduce vs sum()/max() vs collect");

        List<Integer> numbers = SampleData.numbers();

        // Suma: trzy zapisy, ten sam wynik. Najczytelniej: mapToInt(...).sum().
        show("reduce(0, Integer::sum)", numbers.stream().reduce(0, Integer::sum));
        // WYNIK: reduce(0, Integer::sum) → 66
        show("mapToInt(...).sum()", numbers.stream().mapToInt(Integer::intValue).sum());
        // WYNIK: mapToInt(...).sum() → 66

        // Max: reduce(Integer::max) vs max(Comparator) vs mapToInt().max().
        show("reduce(Integer::max)", numbers.stream().reduce(Integer::max));
        // WYNIK: reduce(Integer::max) → Optional[10]
        show("max(naturalOrder())", numbers.stream().max(Comparator.naturalOrder()));
        // WYNIK: max(naturalOrder()) → Optional[10]

        // PUŁAPKA: reduce do ZMIENNEGO kontenera (ArrayList). identity ma być niezmienne i neutralne,
        // a tu je zmieniamy — „element neutralny” po operacji nie jest już pusty!
        List<Integer> identity = new ArrayList<>();
        List<Integer> result = Stream.of(1, 2, 3).reduce(identity,
                (list, n) -> {
                    list.add(n);                          // ← zmienia listę identity (efekt uboczny)
                    return list;
                },
                (a, b) -> {
                    a.addAll(b);
                    return a;
                });
        show("wynik", result);
        // WYNIK: wynik → [1, 2, 3]
        show("identity po reduce (miało być puste!)", identity);
        // WYNIK: identity po reduce (miało być puste!) → [1, 2, 3]
        show("wynik i identity to ten sam obiekt?", result == identity);
        // WYNIK: wynik i identity to ten sam obiekt? → true
        // W parallel wszystkie wątki dopisywałyby do TEJ SAMEJ ArrayList → zgubione elementy albo wyjątek.

        // DOBRA PRAKTYKA: zbieranie do kontenera → collect / toList (osobny kontener dla każdego wątku).
        show("toList()", Stream.of(1, 2, 3).toList());   // toList (Java 16+) = do listy
        // WYNIK: toList() → [1, 2, 3]
        // Zasada: reduce → wartości niezmienne (liczby, BigDecimal, String, rekordy); collect → kontenery zmienne.
    }

    // =================================================================================================
    // 9. SKLEJANIE TEKSTÓW PRZEZ reduce — O(n²); ZAPOWIEDŹ joining
    // =================================================================================================

    /**
     * 9. {@code reduce("", (a, b) -> a + b)} działa, ale każdy krok kopiuje CAŁY dotychczasowy tekst — O(n²).
     * Do tego trudno o separator. {@code Collectors.joining} używa StringBuilder i robi to dobrze.
     */
    static void stringConcatPitfall() {
        section("9. Sklejanie tekstów: reduce to O(n²) — użyj joining");

        List<String> distinctWords = SampleData.words().stream().distinct().toList();

        // PUŁAPKA: separator — identity "" + ", " daje przecinek NA POCZĄTKU.
        String withSeparator = distinctWords.stream().reduce("", (a, b) -> a + ", " + b);
        show("reduce z separatorem", withSeparator);
        // WYNIK: reduce z separatorem → , java, stream, lambda, kolekcja, mapa, lista, optional, rekord, enum

        // PUŁAPKA: wydajność — każdy krok a + b tworzy NOWY String i kopiuje wszystko, co było.
        //   10 000 słów po 5 liter → ok. 250 milionów skopiowanych znaków (n² / 2 × 5),
        //   a StringBuilder w joining kopiuje każdy znak raz → ok. 50 tysięcy.

        // DOBRA PRAKTYKA: Collectors.joining(separator[, prefiks, sufiks]) — t16_streams/Streams09CollectorsBasic.
        show("joining(\", \")", distinctWords.stream().collect(Collectors.joining(", ")));
        // WYNIK: joining(", ") → java, stream, lambda, kolekcja, mapa, lista, optional, rekord, enum
        show("joining(\", \", \"[\", \"]\")", distinctWords.stream().limit(3).collect(Collectors.joining(", ", "[", "]")));
        // WYNIK: joining(", ", "[", "]") → [java, stream, lambda]
        // Bez strumienia: String.join(", ", lista) — też StringBuilder w środku.
        show("String.join", String.join(" | ", distinctWords.subList(0, 3)));
        // WYNIK: String.join → java | stream | lambda
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • reduce(identity, (acc, x) -> ...)        → wynik typu T; pusty strumień → identity
     *   • reduce((a, b) -> ...)                    → Optional; pusty strumień → Optional.empty
     *   • reduce(identity, accumulator, combiner)  → wynik typu U ≠ T; combiner łączy wyniki częściowe (parallel)
     *   • identity NEUTRALNE: + → 0, * → 1, max → MIN_VALUE, min → MAX_VALUE, tekst → "", AND → true, OR → false
     *   • operacja ŁĄCZNA: +, *, max, min, concat, BigDecimal::add — TAK;  -, /, (a + b) / 2 — NIE
     *   • „+10 do sumy” → reduce(0, Integer::sum) + 10, NIGDY reduce(10, ...)
     *   • iloczyn → long / BigInteger (13! nie mieści się w int)
     *   • pieniądze → map(X::kwota).reduce(BigDecimal.ZERO, BigDecimal::add); porównanie → compareTo
     *   • liczby → mapToInt(...).sum() / max() / average() czytelniej niż reduce
     *   • zbieranie do listy/mapy → collect / toList; NIGDY reduce z zmiennym ArrayList jako identity
     *   • sklejanie tekstu → Collectors.joining(", ") (StringBuilder), nie reduce("", a + b) (O(n²))
     *
     * PYTANIA KONTROLNE:
     *   1. Co to jest element neutralny (identity)? Podaj go dla: sumy, iloczynu, max, sklejania tekstów.
     *   2. Co wypisze:  System.out.println(Stream.of(1, 2, 3).reduce(10, Integer::sum));  ?  Czy w parallel też?
     *   3. Co wypisze:  System.out.println(Stream.<Integer>empty().reduce(Integer::sum));  ?
     *   4. ZNAJDŹ BŁĄD:  int balance = expenses.parallelStream().reduce(1000, (a, b) -> a - b);
     *   5. ZNAJDŹ BŁĄD:  String csv = names.stream().reduce("", (a, b) -> a + ";" + b);
     *   6. Po co jest trzeci argument (combiner) w reduce? Kiedy jest wywoływany?
     *   7. Dlaczego kwoty sumujemy przez reduce(BigDecimal.ZERO, BigDecimal::add), a nie mapToDouble(...).sum()?
     *   8. ZNAJDŹ BŁĄD:  List<Integer> all = stream.reduce(new ArrayList<>(), (l, x) -> { l.add(x); return l; }, (a, b) -> { a.addAll(b); return a; });
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Statystyka tekstów do ćwiczenia 4 (rekord, Java 16+): ile tekstów i łączna liczba znaków. */
    record TextStats(int count, int totalLength) { }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        List<Product> products = SampleData.products();
        List<Order> orders = SampleData.orders();
        List<String> words = SampleData.words();
        Check.equal("ćw. 1: suma sztuk (wszystkie)", 595, () -> exercise1(products));
        Check.equal("ćw. 1: suma sztuk (pierwsze 3)", 32, () -> exercise1(products.subList(0, 3)));
        Check.equal("ćw. 2: najdłuższe słowo", Optional.of("kolekcja"), () -> exercise2(words));
        Check.equal("ćw. 2: remis → pierwsze", Optional.of("ab"), () -> exercise2(List.of("ab", "cd")));
        Check.equal("ćw. 2: pusta lista", Optional.empty(), () -> exercise2(List.of()));
        Check.equal("ćw. 3: klient 1 bez anulowanych", new BigDecimal("6669.79"), () -> exercise3(orders, 1L));
        Check.equal("ćw. 3: klient 4 bez anulowanych", new BigDecimal("4755.98"), () -> exercise3(orders, 4L));
        Check.equal("ćw. 4: słowa (sekwencyjnie)", new TextStats(12, 65), () -> exercise4(words.stream()));
        Check.equal("ćw. 4: słowa (parallel)", new TextStats(12, 65), () -> exercise4(words.parallelStream()));
        Check.equal("ćw. 4: zdania", new TextStats(4, 112), () -> exercise4(SampleData.sentences().stream()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec, wszystkie)", 595, () -> solution1(products));
        Check.equal("ćw. 1 (wzorzec, pierwsze 3)", 32, () -> solution1(products.subList(0, 3)));
        Check.equal("ćw. 2 (wzorzec)", Optional.of("kolekcja"), () -> solution2(words));
        Check.equal("ćw. 2 (wzorzec, remis)", Optional.of("ab"), () -> solution2(List.of("ab", "cd")));
        Check.equal("ćw. 2 (wzorzec, pusta)", Optional.empty(), () -> solution2(List.of()));
        Check.equal("ćw. 3 (wzorzec, klient 1)", new BigDecimal("6669.79"), () -> solution3(orders, 1L));
        Check.equal("ćw. 3 (wzorzec, klient 4)", new BigDecimal("4755.98"), () -> solution3(orders, 4L));
        Check.equal("ćw. 4 (wzorzec, sekwencyjnie)", new TextStats(12, 65), () -> solution4(words.stream()));
        Check.equal("ćw. 4 (wzorzec, parallel)", new TextStats(12, 65), () -> solution4(words.parallelStream()));
        Check.equal("ćw. 4 (wzorzec, zdania)", new TextStats(4, 112), () -> solution4(SampleData.sentences().stream()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 10 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ pętlę na map + reduce (z elementem neutralnym):
     * <pre>{@code
     * int total = 0;
     * for (Product p : products) {
     *     total += p.stock();
     * }
     * return total;
     * }</pre>
     * Podpowiedź: {@code map(Product::stock).reduce(0, Integer::sum)}. Potem pomyśl, jak zapisać to przez mapToInt.
     */
    static int exercise1(List<Product> products) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): Zwróć najdłuższe słowo przez reduce BEZ identity. Przy remisie wygrywa słowo
     * WCZEŚNIEJSZE na liście. Dla pustej listy — Optional.empty().
     * Podpowiedź: {@code reduce((a, b) -> ...)}; zastanów się, czy w warunku użyć {@code >} czy {@code >=}.
     * Usuń linię z throw.
     */
    static Optional<String> exercise2(List<String> words) {
        // TODO: twoje rozwiązanie (usuń throw)
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): Policz łączną kwotę (BigDecimal) wszystkich NIEANULOWANYCH zamówień
     * klienta o podanym id.
     * Podpowiedź: filter po {@code o.customer().id()} i statusie → {@code map(Order::total)} →
     * {@code reduce(BigDecimal.ZERO, BigDecimal::add)}.
     */
    static BigDecimal exercise3(List<Order> orders, long customerId) {
        // TODO: twoje rozwiązanie
        return BigDecimal.ZERO;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): Jednym reduce z TRZEMA argumentami policz dla strumienia tekstów rekord
     * {@code TextStats(count, totalLength)}: ile jest tekstów i ile mają łącznie znaków.
     * Rozwiązanie musi działać także na strumieniu równoległym (test „parallel”).
     * Podpowiedź: identity = {@code new TextStats(0, 0)}; accumulator: (stats, text) → nowy TextStats
     * z count + 1 i totalLength + text.length(); combiner: (s1, s2) → TextStats z sumą obu pól.
     */
    static TextStats exercise4(Stream<String> texts) {
        // TODO: twoje rozwiązanie
        return new TextStats(0, 0);
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(List<Product> products) {
        return products.stream()
                .map(Product::stock)
                .reduce(0, Integer::sum);
        // Czytelniej: products.stream().mapToInt(Product::stock).sum();
    }

    static Optional<String> solution2(List<String> words) {
        return words.stream()
                .reduce((a, b) -> a.length() >= b.length() ? a : b);   // >= → przy remisie zostaje a (wcześniejsze)
    }

    static BigDecimal solution3(List<Order> orders, long customerId) {
        return orders.stream()
                .filter(o -> o.customer().id() == customerId)
                .filter(o -> o.status() != OrderStatus.ANULOWANE)
                .map(Order::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static TextStats solution4(Stream<String> texts) {
        return texts.reduce(
                new TextStats(0, 0),
                (stats, text) -> new TextStats(stats.count() + 1, stats.totalLength() + text.length()),
                (s1, s2) -> new TextStats(s1.count() + s2.count(), s1.totalLength() + s2.totalLength()));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Wartość, która nic nie zmienia w operacji: op(identity, x) == x. Suma → 0, iloczyn → 1,
     *      max → Integer.MIN_VALUE, sklejanie tekstów → "".
     *   2. 16 (10 + 1 + 2 + 3). W parallel może wyjść więcej (np. 26 albo 36), bo 10 trafia do KAŻDEGO kawałka.
     *      10 nie jest neutralne dla dodawania. Poprawnie: reduce(0, Integer::sum) + 10.
     *   3. Optional.empty — bez identity pusty strumień nie ma wyniku.
     *   4. Dwa błędy: 1000 nie jest neutralne i odejmowanie nie jest łączne → w parallel wynik losowy.
     *      Poprawnie: int balance = 1000 - expenses.stream().mapToInt(Integer::intValue).sum();
     *   5. Wynik zaczyna się od „;” (identity "" + ";" + pierwsze imię), a sklejanie przez reduce to O(n²).
     *      Poprawnie: names.stream().collect(Collectors.joining(";"))  albo  String.join(";", names).
     *   6. combiner łączy wyniki częściowe z różnych kawałków (wątków) w strumieniu równoległym. Jest potrzebny,
     *      gdy wynik ma inny typ niż elementy. W strumieniu sekwencyjnym nie jest wywoływany.
     *   7. double jest niedokładny (0.1 + 0.2 + 0.3 = 0.6000000000000001), a w pieniądzach liczy się każdy grosz.
     *      BigDecimal liczy dokładnie; ZERO jest neutralne, a add łączne — więc reduce jest poprawny także w parallel.
     *   8. identity (ArrayList) jest zmieniane przez akumulator — przestaje być „puste”, a w parallel wszystkie
     *      wątki piszą do jednej listy (błędy, zgubione elementy). Poprawnie: stream.toList() albo collect(...).
     */
    // </editor-fold>
}
