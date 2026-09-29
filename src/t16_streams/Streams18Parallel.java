package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Customer;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Strumienie równoległe — parallelStream() i parallel()
 *        (parallel = równoległy; sequential = sekwencyjny, po kolei)
 *
 * W SKRÓCIE:
 *   Równoległy stream dzieli dane na kawałki i liczy je na kilku rdzeniach procesora naraz.
 *   Czasem przyspiesza, często NIC nie daje, a przy współdzielonym stanie daje BŁĘDNE wyniki.
 *   Kolejność pracy: najpierw poprawny kod sekwencyjny, potem pomiar, dopiero potem ewentualnie parallel().
 *
 * ANALOGIA: Liczenie głosów w wyborach. Jedna komisja może liczyć wszystkie karty po kolei (sekwencyjnie).
 *   Można też podzielić stos kart między 4 komisje: każda liczy swoją część, a na końcu ktoś sumuje
 *   4 wyniki (równolegle). Przy 20 kartach sam podział i sumowanie trwają dłużej niż liczenie.
 *   Przy milionie kart — opłaca się. Ale jeśli 4 komisje dopisują wyniki do JEDNEJ wspólnej kartki
 *   naraz, to część zapisów zginie. To właśnie wyścig wątków (race condition).
 *
 * JAK TO DZIAŁA:
 *   1. Źródło jest dzielone na coraz mniejsze kawałki (Spliterator.trySplit = spróbuj podzielić).
 *   2. Kawałki trafiają do wspólnej puli wątków ForkJoinPool.commonPool() (fork = rozwidlenie zadania).
 *   3. Każdy wątek przepuszcza SWÓJ kawałek przez ten sam pipeline (filter, map, ...).
 *   4. Wyniki częściowe są łączone (join = złączenie) — np. combiner w reduce/collect.
 *
 *      [1..8] ─podział─► [1..4] [5..8] ─podział─► [1,2] [3,4] [5,6] [7,8]
 *      wątki A, B, C, D liczą sumy kawałków:          3     7    11    15
 *      łączenie:                                   3+7 = 10    11+15 = 26   →   10 + 26 = 36
 *
 *   Wynik jest poprawny TYLKO gdy: brak wspólnego stanu, operacja łączna (asocjacyjna),
 *   a wartość początkowa (tożsamość) jest neutralna.
 *
 * SŁÓWKA:
 *   parallel = równoległy; sequential = sekwencyjny; thread = wątek; pool = pula; common = wspólny;
 *   fork = rozwidlić; join = złączyć; split = podzielić; ordered = uporządkowany; unordered = nieuporządkowany;
 *   race condition = wyścig wątków; thread-safe = bezpieczny wątkowo; overhead = narzut (koszt dodatkowy);
 *   concurrent = współbieżny; benchmark = pomiar wydajności; identity = element neutralny (tożsamość).
 *
 * ZOBACZ TEŻ: t16_streams/Streams07Reduce (tożsamość i łączność w reduce),
 *   t16_streams/Streams17SideEffectsPitfalls (efekty uboczne w lambdach),
 *   t21_concurrency/Concurrency02RaceConditions (wyścigi wątków dokładnie),
 *   t21_concurrency/Concurrency06ConcurrentCollections (kolekcje współbieżne).
 * </pre>
 */
public class Streams18Parallel {

    /** Stała BigDecimal poza lambdą — nie tworzymy jej przy każdym elemencie. */
    private static final BigDecimal LIMIT_100 = new BigDecimal("100");

    public static void main(String[] args) {
        title("Streams18 — strumienie równoległe (parallel streams)");

        parallelBasics();        // parallel basics = podstawy równoległości
        forkJoinUnderTheHood();  // fork/join under the hood = fork/join pod maską
        orderingRules();         // ordering rules = zasady kolejności
        splittingSources();      // splitting sources = dzielenie źródeł
        raceCondition();         // race condition = wyścig wątków
        reduceIdentity();        // reduce identity = tożsamość w reduce
        concurrentCollectors();  // concurrent collectors = kolektory współbieżne
        measureFirst();          // measure first = najpierw zmierz
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PIERWSZY RÓWNOLEGŁY STREAM
    // =================================================================================================

    /**
     * 1. Dwa sposoby: {@code kolekcja.parallelStream()} albo {@code stream.parallel()}.
     * Wynik sumy jest taki sam — zmienia się tylko SPOSÓB liczenia.
     */
    static void parallelBasics() {
        section("1. parallelStream() i parallel() — pierwszy równoległy stream");

        List<Integer> numbers = SampleData.numbers();

        // stream() = zwykły, sekwencyjny stream; mapToInt = przekształć (mapuj) na int; sum = suma
        int sequentialSum = numbers.stream().mapToInt(Integer::intValue).sum();
        // parallelStream() = równoległy stream prosto z kolekcji
        int parallelSum = numbers.parallelStream().mapToInt(Integer::intValue).sum();
        // parallel() = przełącz pipeline na tryb równoległy
        int parallelSum2 = numbers.stream().parallel().mapToInt(Integer::intValue).sum();

        show("suma sekwencyjnie", sequentialSum);
        // WYNIK: suma sekwencyjnie → 66
        show("suma parallelStream()", parallelSum);
        // WYNIK: suma parallelStream() → 66
        show("suma stream().parallel()", parallelSum2);
        // WYNIK: suma stream().parallel() → 66

        // isParallel() = czy jest równoległy
        show("stream().isParallel()", numbers.stream().isParallel());
        // WYNIK: stream().isParallel() → false
        show("parallelStream().isParallel()", numbers.parallelStream().isParallel());
        // WYNIK: parallelStream().isParallel() → true

        // PUŁAPKA: parallel() i sequential() NIE działają „od tego miejsca w dół”.
        // To jedna flaga dla CAŁEGO pipeline'u. Wygrywa ostatnie wywołanie.
        boolean mixed = numbers.stream()
                .parallel()
                .filter(n -> n > 3)
                .sequential()            // sequential = sekwencyjny — nadpisuje wcześniejsze parallel()
                .map(n -> n * 2)
                .isParallel();
        show("parallel() ... sequential() → isParallel()", mixed);
        // WYNIK: parallel() ... sequential() → isParallel() → false

        // toList() (Java 16+) = do listy. Kolejność elementów jest ZACHOWANA także równolegle,
        // bo źródło (List) jest uporządkowane (ordered) — stream sam składa kawałki we właściwej kolejności.
        List<Integer> times10 = numbers.parallelStream().map(n -> n * 10).toList();
        show("równolegle map(n * 10).toList()", times10);
        // WYNIK: równolegle map(n * 10).toList() → [50, 30, 80, 10, 90, 20, 70, 30, 100, 60, 40, 80]

        // DOBRA PRAKTYKA: parallel() to jedno słowo, ale zmienia model wykonania.
        // Dodawaj je świadomie, po pomiarze (sekcja 8), a nie „na wszelki wypadek”.
    }

    // =================================================================================================
    // 2. POD MASKĄ: FORK/JOIN I WSPÓLNA PULA
    // =================================================================================================

    /**
     * 2. Równoległe streamy używają wspólnej puli {@code ForkJoinPool.commonPool()}.
     * Liczba wątków zależy od komputera, więc te wartości nie mają WYNIK.
     */
    static void forkJoinUnderTheHood() {
        section("2. Pod maską: fork/join i wspólna pula wątków");

        // availableProcessors = dostępne procesory (rdzenie logiczne)
        int cores = Runtime.getRuntime().availableProcessors();
        // ForkJoinPool = pula „rozwidlaj i łącz”; commonPool = wspólna pula; getParallelism = poziom równoległości
        int workers = ForkJoinPool.commonPool().getParallelism();
        show("rdzenie procesora", cores);
        show("wątki robocze wspólnej puli", workers);
        // (wynik zależy od uruchomienia)  ← zwykle: wątki robocze = rdzenie - 1
        note("wątek main też pomaga liczyć, więc zwykle pracuje tyle wątków, ile jest rdzeni");

        // Które wątki pracowały? Thread.currentThread().getName() = nazwa bieżącego wątku.
        // Zbieramy nazwy do TreeSet (posortowany zbiór bez duplikatów).
        TreeSet<String> threadNames = IntStream.rangeClosed(1, 20_000)
                .parallel()
                .mapToObj(i -> Thread.currentThread().getName())
                .collect(Collectors.toCollection(TreeSet::new)); // toCollection = do kolekcji
        show("ile różnych wątków pracowało", threadNames.size());
        show("przykładowa nazwa wątku", threadNames.first());
        // (wynik zależy od uruchomienia)  ← np. 8 i ForkJoinPool.commonPool-worker-1

        // Wspólna pula jest JEDNA na całą aplikację (całą maszynę wirtualną JVM).
        // PUŁAPKA: jeśli jeden równoległy stream zablokuje wątki (np. czeka na sieć), to
        // WSZYSTKIE inne równoległe streamy w programie czekają razem z nim.

        // Sztuczka spotykana w praktyce: uruchomić stream wewnątrz WŁASNEJ puli.
        // submit = zgłoś zadanie; get = poczekaj na wynik; shutdown = zamknij pulę.
        // Uwaga: to szczegół implementacji, nie gwarancja z dokumentacji — traktuj ostrożnie.
        ForkJoinPool ownPool = new ForkJoinPool(2);
        try {
            long sum = ownPool.submit(() -> LongStream.rangeClosed(1, 1_000).parallel().sum()).get();
            show("suma 1..1000 we własnej puli (2 wątki)", sum);
            // WYNIK: suma 1..1000 we własnej puli (2 wątki) → 500500
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // przywróć flagę przerwania
        } catch (ExecutionException e) {
            show("błąd w zadaniu", e.getCause());
        } finally {
            ownPool.shutdown();
        }
    }

    // =================================================================================================
    // 3. KOLEJNOŚĆ: forEach vs forEachOrdered, findAny vs findFirst
    // =================================================================================================

    /**
     * 3. Równolegle {@code forEach} i {@code findAny} nie trzymają kolejności.
     * {@code forEachOrdered}, {@code findFirst} i {@code toList()} — trzymają (kosztem wydajności).
     */
    static void orderingRules() {
        section("3. Kolejność: forEach vs forEachOrdered, findAny vs findFirst");

        List<Integer> oneToTen = IntStream.rangeClosed(1, 10).boxed().toList(); // boxed = opakuj w Integer

        // forEach = dla każdego — równolegle wykonuje akcję w DOWOLNEJ kolejności.
        // synchronizedList = lista synchronizowana (bezpieczna wątkowo). Dopisywanie = efekt uboczny, tylko demo!
        List<Integer> seen = Collections.synchronizedList(new ArrayList<>());
        oneToTen.parallelStream().forEach(seen::add);
        show("forEach (równolegle)", seen);
        // (wynik zależy od uruchomienia)  ← np. [7, 6, 3, 2, 9, 10, 1, 5, 4, 8]

        // forEachOrdered = dla każdego w kolejności źródła (wątki czekają na siebie → wolniej)
        List<Integer> seenOrdered = Collections.synchronizedList(new ArrayList<>());
        oneToTen.parallelStream().forEachOrdered(seenOrdered::add);
        show("forEachOrdered (równolegle)", seenOrdered);
        // WYNIK: forEachOrdered (równolegle) → [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

        // findFirst = znajdź pierwszy (w kolejności źródła) — zawsze ten sam wynik
        int first = oneToTen.parallelStream().filter(n -> n % 3 == 0).findFirst().orElseThrow(); // orElseThrow() (Java 10+) = albo rzuć
        show("findFirst (podzielna przez 3)", first);
        // WYNIK: findFirst (podzielna przez 3) → 3

        // findAny = znajdź dowolny — ten, który któryś wątek znalazł najszybciej
        int any = oneToTen.parallelStream().filter(n -> n % 3 == 0).findAny().orElseThrow();
        show("findAny (podzielna przez 3)", any);
        // (wynik zależy od uruchomienia)  ← 3, 6 albo 9
        show("czy findAny zwrócił liczbę podzielną przez 3", any % 3 == 0);
        // WYNIK: czy findAny zwrócił liczbę podzielną przez 3 → true

        // unordered() = zrezygnuj z kolejności. Pozwala szybciej wykonać distinct()/limit() równolegle.
        // Wynik trafia do TreeSet, więc wydruk i tak jest posortowany (deterministyczny).
        TreeSet<Integer> distinctRemainders = SampleData.numbers().parallelStream()
                .unordered()
                .map(n -> n % 4)
                .distinct()
                .collect(Collectors.toCollection(TreeSet::new));
        show("reszty z dzielenia przez 4 (unordered + distinct)", distinctRemainders);
        // WYNIK: reszty z dzielenia przez 4 (unordered + distinct) → [0, 1, 2, 3]

        // DOBRA PRAKTYKA: potrzebujesz kolejności → toList()/collect/forEachOrdered/findFirst.
        // Nie potrzebujesz → findAny, unordered() — pozwalasz strumieniowi pracować szybciej.
    }

    // =================================================================================================
    // 4. ŹRÓDŁA, KTÓRE DOBRZE (I ŹLE) SIĘ DZIELĄ
    // =================================================================================================

    /**
     * 4. Równoległość zaczyna się od podziału źródła. {@code ArrayList} dzieli się na pół w chwilę,
     * {@code LinkedList} trzeba przejść element po elemencie, a {@code Stream.iterate} nie zna swojego rozmiaru.
     */
    static void splittingSources() {
        section("4. Źródła, które dobrze (i źle) się dzielą");

        List<Integer> arrayList = new ArrayList<>(IntStream.range(0, 100).boxed().toList());
        List<Integer> linkedList = new LinkedList<>(arrayList);

        // spliterator = „rozdzielacz” — obiekt, który umie oddać połowę swoich elementów innemu wątkowi
        show("ArrayList (100)       → pierwszy podział", describeSplit(arrayList.spliterator()));
        // WYNIK: ArrayList (100)       → pierwszy podział → 50 | 50
        show("IntStream.range (100) → pierwszy podział", describeSplit(IntStream.range(0, 100).spliterator()));
        // WYNIK: IntStream.range (100) → pierwszy podział → 50 | 50
        show("LinkedList (100)      → pierwszy podział", describeSplit(linkedList.spliterator()));
        // WYNIK: LinkedList (100)      → pierwszy podział → 100 | 0
        show("Stream.iterate        → pierwszy podział", describeSplit(Stream.iterate(0, n -> n + 1).spliterator()));
        // WYNIK: Stream.iterate        → pierwszy podział → 1024 | nieznany rozmiar

        // JAK CZYTAĆ: „lewa | prawa” = ile elementów oddano do nowego kawałka | ile zostało.
        //   ArrayList, tablica, range → równe połówki od razu (znają rozmiar, mają dostęp po indeksie).
        //   LinkedList → musi przejść węzły po kolei i kopiuje je do tablicy; 100 elementów = cały kawałek.
        //   iterate → każdy element zależy od poprzedniego; dzieli się „paczkami” po 1024, bez znajomości końca.
        //
        //   DOBRZE się dzielą:   ArrayList, tablice (Arrays.stream), IntStream.range/rangeClosed
        //   ŚREDNIO:             HashSet, TreeSet, HashMap.keySet()
        //   ŹLE:                 LinkedList, Stream.iterate, Stream.generate, BufferedReader.lines()
        //
        // PUŁAPKA: operacje zależne od kolejności są drogie równolegle na uporządkowanym streamie:
        // limit(), skip(), findFirst(), sorted(), distinct(). Każdy wątek musi wiedzieć, „który jest który”.
        // PUŁAPKA: Stream<Integer> to pudełkowanie (boxing) każdej liczby — dla liczb wybieraj IntStream/LongStream.
    }

    /** Wykonuje jeden podział i opisuje go jako „oddane | pozostałe”. */
    static String describeSplit(Spliterator<?> spliterator) {
        Spliterator<?> prefix = spliterator.trySplit(); // trySplit = spróbuj podzielić (null = nie da się)
        if (prefix == null) {
            return "nie da się podzielić";
        }
        long rest = spliterator.estimateSize();         // estimateSize = szacowany rozmiar
        String restText = rest == Long.MAX_VALUE ? "nieznany rozmiar" : String.valueOf(rest);
        return prefix.estimateSize() + " | " + restText;
    }

    // =================================================================================================
    // 5. WYŚCIG WĄTKÓW: WSPÓLNA LISTA I LICZNIK
    // =================================================================================================

    /**
     * 5. Wiele wątków modyfikuje ten sam obiekt bez synchronizacji → gubione zapisy albo wyjątek.
     * Wypisujemy wynik złej wersji bez WYNIK (zmienia się), a dobrej — z WYNIK.
     */
    static void raceCondition() {
        section("5. Wyścig wątków: wspólna lista i licznik");
        int n = 10_000;

        // ŹLE: ArrayList NIE jest bezpieczna wątkowo (thread-safe). Dwa wątki mogą zapisać ten sam indeks.
        List<Integer> unsafeList = new ArrayList<>();
        String badListResult;
        try {
            IntStream.range(0, n).parallel().boxed().forEach(unsafeList::add); // efekt uboczny z wielu wątków!
            badListResult = "rozmiar " + unsafeList.size();
        } catch (RuntimeException e) {
            badListResult = "rzucono " + e.getClass().getSimpleName();
        }
        show("ŹLE  ArrayList + forEach(add)", badListResult + " (oczekiwano " + n + ")");
        // (wynik zależy od uruchomienia)  ← np. rozmiar 8712 albo rzucono ArrayIndexOutOfBoundsException

        // ŹLE: licznik w tablicy. counter[0]++ to TRZY kroki: odczyt, +1, zapis. Wątki się przeplatają.
        int[] counter = {0};
        IntStream.range(0, n).parallel().forEach(i -> counter[0]++);
        show("ŹLE  int[] licznik++", counter[0]);
        // (wynik zależy od uruchomienia)  ← czasem 10000, czasem mniej — i właśnie to jest groźne
        note("nawet jeśli wyszło 10000 — to przypadek, nie dowód poprawności");

        // DOBRZE: niech stream sam zbierze wynik — każdy wątek ma własny pojemnik, potem są łączone.
        List<Integer> safeList = IntStream.range(0, n).parallel().boxed().toList();
        show("DOBRZE toList() → rozmiar", safeList.size());
        // WYNIK: DOBRZE toList() → rozmiar → 10000
        show("DOBRZE toList() → pierwsze 5", safeList.subList(0, 5));
        // WYNIK: DOBRZE toList() → pierwsze 5 → [0, 1, 2, 3, 4]

        // DOBRZE: count() = policz — wbudowana redukcja
        long count = IntStream.range(0, n).parallel().filter(i -> i % 2 == 0).count();
        show("DOBRZE count() liczb parzystych", count);
        // WYNIK: DOBRZE count() liczb parzystych → 5000

        // ZNOŚNIE: AtomicInteger = liczba całkowita z atomowymi operacjami; incrementAndGet = zwiększ i pobierz.
        // Działa poprawnie, ale wątki „przepychają się” na jednej zmiennej — zwykle wolniej niż count()/sum().
        AtomicInteger atomic = new AtomicInteger();
        IntStream.range(0, n).parallel().forEach(i -> atomic.incrementAndGet());
        show("ZNOŚNIE AtomicInteger", atomic.get());
        // WYNIK: ZNOŚNIE AtomicInteger → 10000

        // DOBRA PRAKTYKA: w równoległym streamie ZERO zapisów do zmiennych spoza lambdy.
        // Wynik ma powstać z collect/reduce/sum/count. Więcej: t21_concurrency/Concurrency02RaceConditions.
    }

    // =================================================================================================
    // 6. REDUCE: TOŻSAMOŚĆ NEUTRALNA I OPERACJA ŁĄCZNA
    // =================================================================================================

    /**
     * 6. Równolegle {@code reduce(identity, op)} używa {@code identity} w KAŻDYM kawałku.
     * Dlatego identity musi być neutralne (0 dla +, 1 dla *, "" dla sklejania), a op — łączna.
     */
    static void reduceIdentity() {
        section("6. reduce: tożsamość musi być neutralna, operacja łączna");

        List<Integer> numbers = SampleData.numbers(); // suma = 66

        // ŹLE: 10 to nie jest element neutralny dodawania
        int sequentialWrong = numbers.stream().reduce(10, Integer::sum);
        show("sekwencyjnie reduce(10, sum)", sequentialWrong);
        // WYNIK: sekwencyjnie reduce(10, sum) → 76
        int parallelWrong = numbers.parallelStream().reduce(10, Integer::sum);
        show("równolegle   reduce(10, sum)", parallelWrong);
        // (wynik zależy od uruchomienia)  ← np. 186: „+10” doliczone w każdym z 12 kawałków
        note("sekwencyjnie „działało”, więc błąd długo pozostaje niezauważony");

        // DOBRZE: tożsamość neutralna (0), a stałą dodajemy raz, na końcu
        int parallelGood = numbers.parallelStream().reduce(0, Integer::sum) + 10;
        show("równolegle   reduce(0, sum) + 10", parallelGood);
        // WYNIK: równolegle   reduce(0, sum) + 10 → 76

        // ŹLE: odejmowanie NIE jest łączne: (a - b) - c  ≠  a - (b - c)
        int minusSequential = numbers.stream().reduce(0, (a, b) -> a - b);
        show("sekwencyjnie reduce(0, a - b)", minusSequential);
        // WYNIK: sekwencyjnie reduce(0, a - b) → -66
        int minusParallel = numbers.parallelStream().reduce(0, (a, b) -> a - b);
        show("równolegle   reduce(0, a - b)", minusParallel);
        // (wynik zależy od uruchomienia)  ← kawałki są łączone w innym „nawiasowaniu”

        // DOBRZE: sklejanie napisów jest łączne, a "" jest neutralne → kolejność zachowana.
        // (Choć do sklejania lepszy jest Collectors.joining — nie tworzy tylu obiektów String.)
        List<String> words = List.of("a", "b", "c", "d", "e", "f"); // List.of (Java 9+) = lista niezmienna
        show("równolegle reduce(\"\", concat)", words.parallelStream().reduce("", String::concat));
        // WYNIK: równolegle reduce("", concat) → abcdef
        show("równolegle joining(\"-\")", words.parallelStream().collect(Collectors.joining("-")));
        // WYNIK: równolegle joining("-") → a-b-c-d-e-f

        // Trzyargumentowy reduce: identity, accumulator (akumulator), combiner (łącznik kawałków).
        // Równolegle combiner NAPRAWDĘ jest wywoływany — musi pasować do akumulatora.
        int totalStock = SampleData.products().parallelStream()
                .reduce(0, (acc, p) -> acc + p.stock(), Integer::sum);
        show("suma stanów magazynowych (3-arg reduce)", totalStock);
        // WYNIK: suma stanów magazynowych (3-arg reduce) → 595

        // PUŁAPKA: reduce z nieneutralną tożsamością to błąd, który „wychodzi” dopiero po dodaniu parallel().
        // ZOBACZ TEŻ: t16_streams/Streams07Reduce.
    }

    // =================================================================================================
    // 7. KOLEKTORY WSPÓŁBIEŻNE
    // =================================================================================================

    /**
     * 7. {@code groupingBy} działa równolegle poprawnie (każdy wątek ma swoją mapę, potem scalanie).
     * {@code groupingByConcurrent} i {@code toConcurrentMap} piszą do JEDNEJ mapy współbieżnej — bez scalania.
     */
    static void concurrentCollectors() {
        section("7. toConcurrentMap i groupingByConcurrent");

        // groupingByConcurrent = grupuj współbieżnie; counting = policz. Wynik: ConcurrentMap (mapa współbieżna).
        ConcurrentMap<Department, Long> perDepartment = SampleData.employees().parallelStream()
                .collect(Collectors.groupingByConcurrent(Employee::department, Collectors.counting()));
        // PUŁAPKA: ConcurrentHashMap z kluczem-enumem ma kolejność zależną od hashCode (inna w każdym uruchomieniu).
        // Do wydruku kopiujemy do TreeMap — enumy sortują się w kolejności deklaracji.
        show("pracownicy w działach", new TreeMap<>(perDepartment));
        // WYNIK: pracownicy w działach → {IT=4, HR=1, SPRZEDAZ=2, KSIEGOWOSC=1, MARKETING=2}

        // toConcurrentMap = do mapy współbieżnej (klucz, wartość, funkcja łącząca duplikaty)
        ConcurrentMap<String, Integer> customersPerCity = SampleData.customers().parallelStream()
                .collect(Collectors.toConcurrentMap(Customer::city, c -> 1, Integer::sum));
        show("klienci w miastach", new TreeMap<>(customersPerCity));
        // WYNIK: klienci w miastach → {Gdańsk=1, Kraków=2, Poznań=1, Warszawa=2, Wrocław=1}

        // Zwykły groupingBy + toList() równolegle: kolejność elementów w listach ZACHOWANA (scalanie w kolejności).
        Map<Department, List<String>> namesPerDept = SampleData.employees().parallelStream()
                .collect(Collectors.groupingBy(Employee::department, TreeMap::new,
                        Collectors.mapping(Employee::name, Collectors.toList())));
        show("IT (groupingBy, równolegle)", namesPerDept.get(Department.IT));
        // WYNIK: IT (groupingBy, równolegle) → [Anna Nowak, Piotr Kowalski, Michał Lewandowski, Ewa Woźniak]

        // PUŁAPKA: groupingByConcurrent + toList() → kolejność w listach DOWOLNA (wątki dopisują naraz).
        // Jeśli kolejność ma znaczenie — sortuj albo użyj zwykłego groupingBy.
        // DOBRA PRAKTYKA: kolektory współbieżne mają sens przy DUŻYCH danych, gdy kolejność nie jest ważna.
        // Przy 10 pracownikach żaden kolektor nie jest „szybszy” — narzut dominuje.
    }

    // =================================================================================================
    // 8. KIEDY PARALLEL POMAGA, KIEDY SZKODZI — MIERZ
    // =================================================================================================

    /**
     * 8. Prosty pomiar czasu {@code System.nanoTime()} — tylko orientacyjnie. Poważne pomiary: JMH.
     * Czasy są różne przy każdym uruchomieniu, więc bez WYNIK; drukujemy też fakt stały: czy wyniki są równe.
     */
    static void measureFirst() {
        section("8. Kiedy parallel pomaga, kiedy szkodzi — mierz!");

        long[] data = LongStream.rangeClosed(1, 3_000_000).toArray();

        long t0 = System.nanoTime(); // nanoTime = czas w nanosekundach (tylko do mierzenia odstępów)
        long sequential = Arrays.stream(data).map(x -> x * x % 7).sum();
        long t1 = System.nanoTime();
        long parallel = Arrays.stream(data).parallel().map(x -> x * x % 7).sum();
        long t2 = System.nanoTime();

        show("wyniki równe", sequential == parallel);
        // WYNIK: wyniki równe → true
        show("czas sekwencyjnie", millis(t1 - t0));
        show("czas równolegle", millis(t2 - t1));
        // (wynik zależy od uruchomienia)  ← np. 14.2 ms vs 5.1 ms (a przy pierwszym uruchomieniu bywa odwrotnie!)

        // Mała kolekcja: 12 liczb. Podział + wątki + łączenie kosztują więcej niż samo dodawanie.
        long t3 = System.nanoTime();
        int small = SampleData.numbers().parallelStream().mapToInt(Integer::intValue).sum();
        long t4 = System.nanoTime();
        show("12 liczb równolegle: suma", small);
        // WYNIK: 12 liczb równolegle: suma → 66
        show("12 liczb równolegle: czas", millis(t4 - t3));
        // (wynik zależy od uruchomienia)

        // JAK OCENIĆ „NA OKO” (reguła kciuka, model N×Q):
        //   N = liczba elementów, Q = koszt pracy na jeden element.
        //   N × Q małe (np. 1000 prostych dodawań)  → parallel zwykle WOLNIEJSZY.
        //   N × Q duże (miliony elementów albo ciężkie obliczenia na element) → parallel może pomóc.
        //
        //   POMAGA, gdy:  dużo danych · praca obliczeniowa (CPU) · źródło dobrze się dzieli ·
        //                 brak wspólnego stanu · kolejność nieważna · liczby prymitywne (LongStream)
        //   SZKODZI, gdy: mało danych · I/O (plik, sieć, baza — wątki czekają zamiast liczyć) ·
        //                 LinkedList/iterate · synchronized/Atomic w lambdzie · limit/sorted/findFirst ·
        //                 serwer, gdzie wiele żądań naraz już zajmuje wszystkie rdzenie
        //
        // PUŁAPKA: operacje I/O w parallel() blokują wątki WSPÓLNEJ puli. Do równoległego I/O używaj
        // własnego ExecutorService lub CompletableFuture (t21_concurrency/Concurrency04Executors,
        // t21_concurrency/Concurrency05CompletableFuture).
        //
        // DOBRA PRAKTYKA: mierz narzędziem JMH (Java Microbenchmark Harness = uprząż do mikropomiarów).
        // JMH robi rozgrzewkę (JIT = kompilator w locie optymalizuje kod po kilku przebiegach),
        // powtarza pomiary i liczy statystyki. Nasz nanoTime to tylko wskazówka, nie dowód.
    }

    /** Zamienia nanosekundy na tekst „12.3 ms” z kropką (Locale.ROOT = bez polskiego przecinka). */
    static String millis(long nanos) {
        return String.format(Locale.ROOT, "%.1f ms", nanos / 1_000_000.0);
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • kolekcja.parallelStream()  albo  stream.parallel()  — flaga dla CAŁEGO pipeline'u, wygrywa ostatnia
     *   • pod spodem: podział źródła (Spliterator) + ForkJoinPool.commonPool() + łączenie wyników
     *   • kolejność: toList()/collect/forEachOrdered/findFirst — zachowują; forEach/findAny — nie
     *   • dobre źródła: ArrayList, tablice, IntStream.range; złe: LinkedList, iterate, generate, lines()
     *   • ZERO zapisów do wspólnych zmiennych/list → wynik tylko z collect/reduce/sum/count
     *   • reduce: tożsamość neutralna (0, 1, ""), operacja łączna (+, *, max, concat — NIE minus)
     *   • groupingByConcurrent/toConcurrentMap: jedna mapa współbieżna, bez kolejności; drukuj przez TreeMap
     *   • I/O nie w parallel() — blokuje wspólną pulę
     *   • najpierw poprawnie i sekwencyjnie → zmierz (JMH) → dopiero wtedy parallel()
     *
     * PYTANIA KONTROLNE:
     *   1. Co zrobi pipeline  list.stream().parallel().map(...).sequential().filter(...).toList() ?
     *      Czy map wykona się równolegle?
     *   2. Co wypisze:  System.out.println(List.of(1, 2, 3, 4).parallelStream().map(x -> x * 10).toList());  ?
     *   3. Co wypisze:  System.out.println(Stream.of(1, 2, 3).parallel().reduce(100, Integer::sum));  ?
     *   4. ZNAJDŹ BŁĄD:
     *        Map<String, Integer> counts = new HashMap<>();
     *        words.parallelStream().forEach(w -> counts.merge(w, 1, Integer::sum));
     *   5. ZNAJDŹ BŁĄD (programista chce wypisać zamówienia od najstarszego):
     *        orders.parallelStream().sorted(Comparator.comparing(Order::date)).forEach(System.out::println);
     *   6. Dlaczego LinkedList słabo nadaje się na źródło równoległego streamu?
     *   7. Dlaczego nie należy pobierać stron WWW w parallelStream()?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: suma stanów równolegle", 595, () -> exercise1(SampleData.products()));
        Check.equal("ćw. 2: tanie produkty (kolejność źródła)", cheapNames(), () -> exercise2(SampleData.products()));
        Check.equal("ćw. 3: pierwszy z IT z pensją > 10000", "Anna Nowak", () -> exercise3(SampleData.employees()));
        Check.equal("ćw. 4: suma + premia równolegle", 166, () -> exercise4(SampleData.numbers(), 100));
        Check.equal("ćw. 5: stan magazynu w kategoriach", expectedStockPerCategory(),
                () -> exercise5(SampleData.products()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 595, () -> solution1(SampleData.products()));
        Check.equal("ćw. 2 (wzorzec)", cheapNames(), () -> solution2(SampleData.products()));
        Check.equal("ćw. 3 (wzorzec)", "Anna Nowak", () -> solution3(SampleData.employees()));
        Check.equal("ćw. 4 (wzorzec)", 166, () -> solution4(SampleData.numbers(), 100));
        Check.equal("ćw. 5 (wzorzec)", expectedStockPerCategory(), () -> solution5(SampleData.products()));
        Check.summary();
        // WYNIK: ✔ OK    ćw. 1 (wzorzec)
        // WYNIK: ✔ OK    ćw. 2 (wzorzec)
        // WYNIK: ✔ OK    ćw. 3 (wzorzec)
        // WYNIK: ✔ OK    ćw. 4 (wzorzec)
        // WYNIK: ✔ OK    ćw. 5 (wzorzec)
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /** Oczekiwany wynik ćw. 2 — nazwy produktów tańszych niż 100 zł w kolejności z SampleData. */
    static List<String> cheapNames() {
        return List.of("Kawa ziarnista 1kg", "Czekolada gorzka", "Oliwa z oliwek",
                "Czysty kod", "Wzorce projektowe", "T-shirt bawełniany");
    }

    /** Oczekiwany wynik ćw. 5 — EnumMap trzyma kolejność deklaracji enuma. */
    static Map<Category, Integer> expectedStockPerCategory() {
        Map<Category, Integer> expected = new EnumMap<>(Category.class);
        expected.put(Category.ELEKTRONIKA, 36);
        expected.put(Category.SPOZYWCZE, 420);
        expected.put(Category.KSIAZKI, 27);
        expected.put(Category.ODZIEZ, 92);
        expected.put(Category.DOM, 20);
        return expected;
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz sumę stanów magazynowych (stock) wszystkich produktów,
     * używając {@code parallelStream()}.
     * Podpowiedź: parallelStream() → mapToInt(Product::stock) → sum().
     */
    static int exercise1(List<Product> products) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ błędny kod na bezpieczny — wynik ma mieć kolejność źródła:
     * <pre>{@code
     * List<String> result = new ArrayList<>();
     * products.parallelStream()
     *         .filter(p -> p.price().compareTo(LIMIT_100) < 0)
     *         .forEach(p -> result.add(p.name()));   // wyścig wątków + losowa kolejność!
     * return result;
     * }</pre>
     * Podpowiedź: zamiast forEach + add użyj map(Product::name) i toList() — zachowuje kolejność.
     */
    static List<String> exercise2(List<Product> products) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): równolegle znajdź PIERWSZEGO (w kolejności listy) pracownika działu IT
     * z pensją większą niż 10000. Zwróć jego imię i nazwisko.
     * Podpowiedź: findAny() mógłby zwrócić innego pracownika — potrzebujesz findFirst().
     */
    static String exercise3(List<Employee> employees) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): równolegle policz sumę liczb powiększoną JEDEN raz o premię (bonus).
     * Kod {@code numbers.parallelStream().reduce(bonus, Integer::sum)} jest błędny — dlaczego?
     * Podpowiedź: tożsamość w reduce musi być neutralna (0); premię dodaj poza reduce.
     */
    static int exercise4(List<Integer> numbers, int bonus) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): równolegle policz sumę stanów magazynowych w każdej kategorii.
     * Zwróć mapę posortowaną według kategorii (TreeMap albo EnumMap).
     * Podpowiedź: Collectors.toConcurrentMap(Product::category, Product::stock, Integer::sum),
     * a potem skopiuj wynik do new EnumMap(mapa) albo new TreeMap(mapa) — ConcurrentMap nie trzyma kolejności.
     */
    static Map<Category, Integer> exercise5(List<Product> products) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(List<Product> products) {
        return products.parallelStream()
                .mapToInt(Product::stock)
                .sum();
    }

    static List<String> solution2(List<Product> products) {
        return products.parallelStream()
                .filter(p -> p.price().compareTo(LIMIT_100) < 0) // compareTo, nie equals (BigDecimal)
                .map(Product::name)
                .toList();                                        // kolejność źródła zachowana
    }

    static String solution3(List<Employee> employees) {
        return employees.parallelStream()
                .filter(e -> e.department() == Department.IT)
                .filter(e -> e.salary() > 10_000)
                .map(Employee::name)
                .findFirst()                                      // NIE findAny — tu liczy się kolejność
                .orElseThrow();
    }

    static int solution4(List<Integer> numbers, int bonus) {
        return numbers.parallelStream().reduce(0, Integer::sum) + bonus;
        // albo: numbers.parallelStream().mapToInt(Integer::intValue).sum() + bonus
    }

    static Map<Category, Integer> solution5(List<Product> products) {
        ConcurrentMap<Category, Integer> concurrent = products.parallelStream()
                .collect(Collectors.toConcurrentMap(Product::category, Product::stock, Integer::sum));
        return new EnumMap<>(concurrent);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Cały pipeline będzie SEKWENCYJNY — parallel()/sequential() ustawiają jedną flagę dla całego
     *      streamu, a wygrywa ostatnie wywołanie. map też wykona się sekwencyjnie.
     *   2. [10, 20, 30, 40] — zawsze. toList() zachowuje kolejność źródła także równolegle.
     *   3. Nie wiadomo z góry. Sekwencyjnie byłoby 106, ale równolegle 100 trafia do KAŻDEGO kawałka,
     *      więc wychodzi np. 306. Tożsamość 100 nie jest neutralna. Poprawnie: reduce(0, Integer::sum) + 100.
     *   4. HashMap nie jest bezpieczna wątkowo — wiele wątków wywołuje merge naraz: zgubione wpisy,
     *      a nawet uszkodzona mapa. Poprawnie: words.parallelStream().collect(
     *      Collectors.groupingBy(w -> w, Collectors.counting()))  (albo groupingByConcurrent).
     *   5. forEach w parallel ignoruje kolejność — sorted() nic nie da, wydruk będzie pomieszany.
     *      Poprawnie: forEachOrdered(...) albo po prostu stream() zamiast parallelStream().
     *   6. Nie da się jej szybko podzielić na pół: nie ma dostępu po indeksie, trzeba przejść węzły
     *      po kolei (a to robi jeden wątek). Podział kosztuje tyle, co samo przetwarzanie.
     *   7. Pobieranie stron to I/O: wątki głównie CZEKAJĄ na sieć. Blokują przy tym wspólną pulę
     *      ForkJoinPool.commonPool(), z której korzystają wszystkie równoległe streamy w aplikacji.
     *      Do I/O lepszy jest własny ExecutorService lub CompletableFuture z własną pulą.
     */
    // </editor-fold>
}
