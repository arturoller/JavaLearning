package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Customer;
import helpers.model.Employee;
import helpers.model.Order;
import helpers.model.OrderLine;
import helpers.model.OrderStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: flatMap — przekształć i spłaszcz strumień
 *        (flat = płaski; map = przekształć (mapuj); flatMap = przekształć i spłaszcz)
 *
 * W SKRÓCIE:
 *   map zamienia 1 element na DOKŁADNIE 1 element. flatMap zamienia 1 element na 0, 1 albo wiele
 *   elementów i skleja wszystkie te małe strumienie w jeden płaski strumień.
 *   Używasz go, gdy element „ma w sobie” kolekcję: zamówienie → pozycje, zdanie → słowa, pracownik → umiejętności.
 *
 * ANALOGIA: Masz 4 pudełka z klockami (lista list).
 *   map     = „przyklej na każde pudełko karteczkę” — dalej masz 4 pudełka.
 *   flatMap = „otwórz każde pudełko i wysyp klocki na jedną kupkę” — masz jedną kupkę klocków,
 *             a puste pudełko po prostu nic nie dokłada.
 *
 * JAK TO DZIAŁA:
 *   element ──(funkcja)──▶ mały Stream (0..n elementów) ──▶ jego elementy lecą do JEDNEGO wspólnego strumienia
 *
 *     [[1, 2], [3], [], [4, 5, 6]]
 *        │      │    │      │          flatMap(List::stream)
 *        ▼      ▼    ▼      ▼
 *      1, 2     3  (nic)  4, 5, 6  ──▶  [1, 2, 3, 4, 5, 6]
 *
 *   • Funkcja podana do flatMap MUSI zwrócić Stream (nie listę!). Dla listy piszesz {@code List::stream}.
 *   • Tak jak map, flatMap jest operacją POŚREDNIĄ i leniwą — bez operacji końcowej nic się nie dzieje.
 *   • Kolejność zostaje zachowana: najpierw wszystkie elementy 1. pudełka, potem 2. itd.
 *
 * SŁÓWKA:
 *   flat = płaski; flatten = spłaszczyć; nested = zagnieżdżony; inner = wewnętrzny; line = pozycja (zamówienia);
 *   skill = umiejętność; owner = właściciel; cartesian product = iloczyn kartezjański (każdy z każdym);
 *   downstream = dalej w dół (strumienia); sink = ujście, odbiornik; multi = wiele; split = podziel
 *
 * ZOBACZ TEŻ: t16_streams/Streams03FilterMap (map — przekształcenie 1 → 1),
 *   t14_optional/Optional02Transform (flatMap w Optional — ta sama idea „bez podwójnego pudełka”),
 *   t16_streams/Streams08PrimitiveStreams (IntStream — więcej o flatMapToInt),
 *   t16_streams/Streams14OptionalInStreams (Optional w strumieniach)
 * </pre>
 */
public class Streams04FlatMap {

    /** Polskie ustawienia regionalne — do toLowerCase (zamień na małe litery) w przewidywalny sposób. */
    private static final Locale PL = Locale.forLanguageTag("pl-PL");

    /** Wzorzec „jeden lub więcej białych znaków” — do dzielenia zdań na słowa. */
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public static void main(String[] args) {
        title("Streams04 — flatMap: przekształć i spłaszcz");

        nestedListProblem();     // nested list problem = problem listy list
        ordersToLines();         // orders to lines = zamówienia → pozycje
        sentencesToWords();      // sentences to words = zdania → słowa
        employeeSkills();        // employee skills = umiejętności pracowników
        keepParentContext();     // keep parent context = zachowaj informację o „rodzicu”
        optionalStream();        // optional stream = Optional jako mini-strumień 0..1
        flatMapToIntChars();     // flatMapToInt chars = spłaszcz do IntStream (znaki)
        mapMultiDemo();          // mapMulti demo = pokaz mapMulti (Java 16+)
        cartesianProduct();      // cartesian product = iloczyn kartezjański (każdy z każdym)
        nullCollectionPitfall(); // null collection pitfall = pułapka: kolekcja równa null
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PROBLEM: LISTA LIST
    // =================================================================================================

    /**
     * 1. Lista list. {@code map} daje „strumień pudełek” ({@code Stream<List<Integer>>} albo
     * {@code Stream<Stream<Integer>>}), a my chcemy płaskich liczb. Rozwiązanie: {@code flatMap}.
     */
    static void nestedListProblem() {
        section("1. Problem: lista list — map nie spłaszcza");

        List<List<Integer>> nested = List.of(List.of(1, 2), List.of(3), List.of(), List.of(4, 5, 6));   // List.of (Java 9+) = niemodyfikowalna lista
        show("lista list", nested);
        // WYNIK: lista list → [[1, 2], [3], [], [4, 5, 6]]

        // map: 1 element → 1 element. Z 4 list robi 4 rozmiary — dalej 4 elementy.
        List<Integer> sizes = nested.stream().map(List::size).toList(); // size = rozmiar; toList (Java 16+) = do listy
        show("map(List::size)", sizes);
        // WYNIK: map(List::size) → [2, 1, 0, 3]

        // PUŁAPKA: map(List::stream) NIE spłaszcza. Dostajesz Stream<Stream<Integer>> — strumień strumieni.
        Stream<Stream<Integer>> streamOfStreams = nested.stream().map(List::stream);
        show("map(List::stream).count()", streamOfStreams.count()); // count = policz
        // WYNIK: map(List::stream).count() → 4    ← 4 „pudełka”, a nie 6 liczb

        // flatMap: każda lista → mały strumień, a wszystkie małe strumienie sklejone w jeden.
        List<Integer> flat = nested.stream().flatMap(List::stream).toList();
        show("flatMap(List::stream)", flat);
        // WYNIK: flatMap(List::stream) → [1, 2, 3, 4, 5, 6]
        show("flatMap(List::stream).count()", nested.stream().flatMap(List::stream).count());
        // WYNIK: flatMap(List::stream).count() → 6    ← pusta lista dołożyła 0 elementów

        // PRZED (pętla): zewnętrzna pętla + addAll (albo druga, wewnętrzna pętla).
        List<Integer> flatLoop = new ArrayList<>();
        for (List<Integer> inner : nested) {   // inner = wewnętrzna (lista)
            flatLoop.addAll(inner);            // addAll = dodaj wszystkie
        }
        show("pętla + addAll", flatLoop);
        // WYNIK: pętla + addAll → [1, 2, 3, 4, 5, 6]
        // PO (stream): nested.stream().flatMap(List::stream).toList() — bez ręcznej listy-akumulatora.

        // Śledzimy przepływ: flatMap otwiera jedno pudełko, wysyła WSZYSTKIE jego elementy
        // dalej rurą i dopiero wtedy otwiera następne. println w peek to efekt uboczny — tylko do pokazu!
        List<Integer> evens = nested.stream()
                .peek(list -> System.out.println("   otwieram pudełko " + list)) // peek = podejrzyj
                .flatMap(List::stream)
                .peek(n -> System.out.println("      dalej płynie " + n))
                .filter(n -> n % 2 == 0)
                .toList();
        // WYNIK: otwieram pudełko [1, 2]
        // WYNIK: dalej płynie 1
        // WYNIK: dalej płynie 2
        // WYNIK: otwieram pudełko [3]
        // WYNIK: dalej płynie 3
        // WYNIK: otwieram pudełko []
        // WYNIK: otwieram pudełko [4, 5, 6]
        // WYNIK: dalej płynie 4
        // WYNIK: dalej płynie 5
        // WYNIK: dalej płynie 6
        show("parzyste po spłaszczeniu", evens);
        // WYNIK: parzyste po spłaszczeniu → [2, 4, 6]
    }

    // =================================================================================================
    // 2. ZAMÓWIENIA → POZYCJE
    // =================================================================================================

    /**
     * 2. Najczęstszy przypadek w praktyce: obiekt „rodzic” ma listę „dzieci”
     * (zamówienie → pozycje). flatMap pozwala liczyć po wszystkich dzieciach naraz.
     */
    static void ordersToLines() {
        section("2. Zamówienia → pozycje zamówień");

        List<Order> orders = SampleData.orders();
        show("liczba zamówień", orders.size());
        // WYNIK: liczba zamówień → 10

        long lineCount = orders.stream()
                .flatMap(order -> order.lines().stream())   // Order → Stream<OrderLine>
                .count();
        show("liczba wszystkich pozycji", lineCount);
        // WYNIK: liczba wszystkich pozycji → 19

        // To samo w dwóch krokach z referencjami do metod: najpierw map (Order → List), potem flatMap (List → elementy).
        int totalQuantity = orders.stream()
                .map(Order::lines)                  // lines = pozycje; tu mamy Stream<List<OrderLine>>
                .flatMap(List::stream)              // a tu już Stream<OrderLine>
                .mapToInt(OrderLine::quantity)      // quantity = ilość; mapToInt = przekształć na int
                .sum();                             // sum = suma
        show("łączna liczba sztuk", totalQuantity);
        // WYNIK: łączna liczba sztuk → 41

        // Filtr PRZED flatMap działa na zamówieniach, map PO flatMap działa na pozycjach.
        List<String> delivered = orders.stream()
                .filter(order -> order.status() == OrderStatus.DOSTARCZONE)
                .flatMap(order -> order.lines().stream())
                .map(line -> line.product().name())
                .toList();
        showEach("produkty z zamówień DOSTARCZONYCH", delivered);
        // WYNIK: produkty z zamówień DOSTARCZONYCH (liczba elementów: 6):
        // WYNIK: • Laptop Pro 14
        // WYNIK: • Słuchawki BT
        // WYNIK: • Kawa ziarnista 1kg
        // WYNIK: • Czekolada gorzka
        // WYNIK: • Monitor 27 cali
        // WYNIK: • Lampka biurkowa

        // Ile sztuk kawy (SPO-001) sprzedano, nie licząc anulowanych zamówień?
        int coffee = orders.stream()
                .filter(order -> order.status() != OrderStatus.ANULOWANE)
                .flatMap(order -> order.lines().stream())
                .filter(line -> line.product().sku().equals("SPO-001"))
                .mapToInt(OrderLine::quantity)
                .sum();
        show("sprzedana kawa (szt.)", coffee);
        // WYNIK: sprzedana kawa (szt.) → 5

        // DOBRA PRAKTYKA: filtruj jak najwcześniej. Jeśli warunek dotyczy ZAMÓWIENIA (status),
        // postaw filter przed flatMap — nie rozpakowujesz pozycji, których i tak nie potrzebujesz.
    }

    // =================================================================================================
    // 3. ZDANIA → SŁOWA
    // =================================================================================================

    /**
     * 3. Zdanie → słowa: jeden tekst daje wiele elementów. Do dzielenia używamy
     * {@code Arrays.stream(s.split(" "))} albo {@code Pattern.splitAsStream}.
     */
    static void sentencesToWords() {
        section("3. Zdania → słowa → unikalne, małymi literami");

        List<String> sentences = SampleData.sentences();
        showEach("zdania", sentences);
        // WYNIK: zdania (liczba elementów: 4):
        // WYNIK: • Java jest językiem obiektowym
        // WYNIK: • Stream to nie jest kolekcja
        // WYNIK: • Lambda to anonimowa funkcja
        // WYNIK: • Kolekcja przechowuje elementy

        List<String> words = sentences.stream()
                .flatMap(sentence -> Arrays.stream(sentence.split(" "))) // split = podziel; Arrays.stream = tablica → strumień
                .toList();
        show("liczba słów", words.size());
        // WYNIK: liczba słów → 16

        List<String> distinctLower = sentences.stream()
                .flatMap(WHITESPACE::splitAsStream)          // splitAsStream = podziel od razu na strumień
                .map(word -> word.toLowerCase(PL))           // toLowerCase = zamień na małe litery
                .distinct()                                  // distinct = bez powtórzeń
                .toList();
        show("unikalne słowa", distinctLower);
        // WYNIK: unikalne słowa → [java, jest, językiem, obiektowym, stream, to, nie, kolekcja, lambda, anonimowa, funkcja, przechowuje, elementy]
        show("ile unikalnych", distinctLower.size());
        // WYNIK: ile unikalnych → 13

        // PUŁAPKA: kolejność operacji ma znaczenie. Najpierw distinct, potem toLowerCase →
        // „Kolekcja” i „kolekcja” są dla distinct RÓŻNE, więc po zmianie na małe litery mamy duplikat.
        List<String> wrongOrder = sentences.stream()
                .flatMap(WHITESPACE::splitAsStream)
                .distinct()
                .map(word -> word.toLowerCase(PL))
                .toList();
        show("distinct PRZED toLowerCase", wrongOrder.size());
        // WYNIK: distinct PRZED toLowerCase → 14    ← „kolekcja” występuje 2 razy

        // PUŁAPKA: split(" ") przy podwójnej spacji tworzy PUSTE słowo. Wzorzec \\s+ („1 lub więcej spacji”) tego nie robi.
        show("split(\" \")", "Ala  ma kota".split(" "));
        // WYNIK: split(" ") → [Ala, , ma, kota]
        show("split(\"\\\\s+\")", "Ala  ma kota".split("\\s+"));
        // WYNIK: split("\\s+") → [Ala, ma, kota]
    }

    // =================================================================================================
    // 4. PRACOWNICY → UMIEJĘTNOŚCI
    // =================================================================================================

    /**
     * 4. Każdy pracownik ma listę umiejętności. Chcemy jedną listę wszystkich umiejętności w firmie:
     * bez powtórzeń i posortowaną.
     */
    static void employeeSkills() {
        section("4. Pracownicy → umiejętności (distinct, sorted)");

        List<Employee> employees = SampleData.employees();

        long allSkills = employees.stream()
                .flatMap(employee -> employee.skills().stream())   // skills = umiejętności
                .count();
        show("wszystkie umiejętności (z powtórzeniami)", allSkills);
        // WYNIK: wszystkie umiejętności (z powtórzeniami) → 26

        List<String> distinctSkills = employees.stream()
                .map(Employee::skills)
                .flatMap(List::stream)
                .distinct()
                .toList();
        show("bez powtórzeń (kolejność wystąpienia)", distinctSkills);
        // WYNIK: bez powtórzeń (kolejność wystąpienia) → [Java, Spring, SQL, Docker, Rekrutacja, Excel, Negocjacje, CRM, SAP, Kotlin, AWS, SEO, Canva, Python, Google Ads]

        List<String> sortedSkills = employees.stream()
                .flatMap(employee -> employee.skills().stream())
                .distinct()
                .sorted()                                          // sorted = posortowane (naturalnie)
                .toList();
        show("bez powtórzeń, posortowane", sortedSkills);
        // WYNIK: bez powtórzeń, posortowane → [AWS, CRM, Canva, Docker, Excel, Google Ads, Java, Kotlin, Negocjacje, Python, Rekrutacja, SAP, SEO, SQL, Spring]

        // PUŁAPKA: „CRM” przed „Canva”, „SQL” przed „Spring”? sorted() porównuje kody Unicode znak po znaku,
        // a WIELKIE litery mają mniejsze kody niż małe ('R' = 82, 'a' = 97). Więcej: t16_streams/Streams05SortDistinctLimit.
        show("'R' < 'a' ?", 'R' < 'a');
        // WYNIK: 'R' < 'a' ? → true
    }

    // =================================================================================================
    // 5. ZACHOWAJ „RODZICA”
    // =================================================================================================

    /** Para: umiejętność + imię i nazwisko jej właściciela (pomocniczy rekord — Java 16+ — tylko dla tej lekcji). */
    record SkillOwner(String skill, String owner) { }

    /**
     * 5. Po flatMap „rodzic” znika — w kolejnym kroku masz już tylko dzieci. Jeśli rodzic jest potrzebny,
     * zrób {@code map} WEWNĄTRZ lambdy flatMap i połącz dziecko z rodzicem (np. w rekordzie albo tekście).
     */
    static void keepParentContext() {
        section("5. Zachowaj informację o „rodzicu” (pracownik, zamówienie)");

        List<Employee> employees = SampleData.employees();

        // PUŁAPKA: po .flatMap(e -> e.skills().stream()) następny krok widzi tylko String — zmiennej e już nie ma.
        // Rozwiązanie: map wewnątrz flatMap, gdzie e jest jeszcze widoczne.
        List<SkillOwner> pairs = employees.stream()
                .flatMap(e -> e.skills().stream().map(skill -> new SkillOwner(skill, e.name())))
                .toList();
        show("liczba par", pairs.size());
        // WYNIK: liczba par → 26
        showEach("pierwsze 3 pary", pairs.subList(0, 3)); // subList = fragment listy
        // WYNIK: pierwsze 3 pary (liczba elementów: 3):
        // WYNIK: • SkillOwner[skill=Java, owner=Anna Nowak]
        // WYNIK: • SkillOwner[skill=Spring, owner=Anna Nowak]
        // WYNIK: • SkillOwner[skill=SQL, owner=Anna Nowak]

        List<String> sqlPeople = pairs.stream()
                .filter(pair -> pair.skill().equals("SQL"))
                .map(SkillOwner::owner)
                .toList();
        show("kto zna SQL (przez pary)", sqlPeople);
        // WYNIK: kto zna SQL (przez pary) → [Anna Nowak, Michał Lewandowski, Ewa Woźniak]

        // DOBRA PRAKTYKA: nie używaj flatMap „na siłę”. Gdy pytasz tylko „KTO zna SQL”, wystarczy filter + contains.
        List<String> sqlSimple = employees.stream()
                .filter(e -> e.skills().contains("SQL"))       // contains = zawiera
                .map(Employee::name)
                .toList();
        show("kto zna SQL (prościej)", sqlSimple);
        // WYNIK: kto zna SQL (prościej) → [Anna Nowak, Michał Lewandowski, Ewa Woźniak]

        // To samo z zamówieniami: każda pozycja „podpisana” numerem swojego zamówienia.
        List<String> signedLines = SampleData.orders().stream()
                .flatMap(order -> order.lines().stream().map(line -> order.id() + ": " + line))
                .limit(4)                                       // limit = najwyżej 4 elementy
                .toList();
        showEach("pozycje z numerem zamówienia", signedLines);
        // WYNIK: pozycje z numerem zamówienia (liczba elementów: 4):
        // WYNIK: • ZAM-001: Laptop Pro 14 x1
        // WYNIK: • ZAM-001: Słuchawki BT x2
        // WYNIK: • ZAM-002: Kawa ziarnista 1kg x3
        // WYNIK: • ZAM-002: Czekolada gorzka x10
    }

    // =================================================================================================
    // 6. OPTIONAL JAKO STRUMIEŃ 0..1
    // =================================================================================================

    /**
     * 6. {@code Optional.stream()} (Java 9+) zamienia Optional w strumień 0 albo 1 elementu.
     * Razem z flatMap: „weź wartości, które są, a puste pomiń”.
     */
    static void optionalStream() {
        section("6. flatMap(Optional::stream) — wyciągnij tylko istniejące wartości");

        List<Customer> customers = SampleData.customers();

        List<Optional<String>> maybeEmails = customers.stream()
                .map(Customer::findEmail)            // findEmail = znajdź e-mail → Optional<String>
                .toList();
        showEach("map(Customer::findEmail)", maybeEmails);
        // WYNIK: map(Customer::findEmail) (liczba elementów: 7):
        // WYNIK: • Optional[jan@example.com]
        // WYNIK: • Optional.empty
        // WYNIK: • Optional[adam.mazur@example.com]
        // WYNIK: • Optional[zofia@example.com]
        // WYNIK: • Optional.empty
        // WYNIK: • Optional[marek@example.com]
        // WYNIK: • Optional[ewa.lis@example.com]

        // PRZED (Java 8): filter(isPresent) + map(get) — dwa kroki, łatwo zapomnieć o pierwszym.
        List<String> java8Style = customers.stream()
                .map(Customer::findEmail)
                .filter(Optional::isPresent)         // isPresent = czy jest wartość
                .map(Optional::get)                  // get = pobierz (bezpieczne TYLKO po sprawdzeniu)
                .toList();
        show("Java 8: filter + get", java8Style);
        // WYNIK: Java 8: filter + get → [jan@example.com, adam.mazur@example.com, zofia@example.com, marek@example.com, ewa.lis@example.com]

        // PO (Java 9+): Optional::stream daje 0 albo 1 element, a flatMap skleja wyniki.
        List<String> emails = customers.stream()
                .map(Customer::findEmail)
                .flatMap(Optional::stream)           // Optional.stream (Java 9+) = Optional → strumień 0..1
                .toList();
        show("Java 9+: flatMap(Optional::stream)", emails);
        // WYNIK: Java 9+: flatMap(Optional::stream) → [jan@example.com, adam.mazur@example.com, zofia@example.com, marek@example.com, ewa.lis@example.com]

        // Gdy pole może być null (a nie Optional): Stream.ofNullable (Java 9+) = null → pusty strumień, wartość → 1 element.
        List<String> viaOfNullable = customers.stream()
                .flatMap(customer -> Stream.ofNullable(customer.email()))
                .toList();
        show("Stream.ofNullable daje to samo?", viaOfNullable.equals(emails));
        // WYNIK: Stream.ofNullable daje to samo? → true

        // PUŁAPKA: map(Optional::get) bez sprawdzenia — wybuchnie na pierwszym pustym Optional (Maria Nowak).
        expectThrows("map(Optional::get) bez filtra", () -> customers.stream()
                .map(Customer::findEmail)
                .map(Optional::get)
                .toList());
        // WYNIK: ✔ map(Optional::get) bez filtra → rzucono NoSuchElementException: No value present
    }

    // =================================================================================================
    // 7. flatMapToInt — SŁOWA → ZNAKI
    // =================================================================================================

    /**
     * 7. {@code String.chars()} zwraca {@code IntStream} (strumień liczb int), a nie {@code Stream<Character>}.
     * Dlatego do spłaszczania słów na znaki używamy {@code flatMapToInt}.
     */
    static void flatMapToIntChars() {
        section("7. flatMapToInt — słowa → znaki");

        List<String> words = SampleData.words();
        show("słowa", words);
        // WYNIK: słowa → [java, stream, lambda, kolekcja, java, mapa, lista, stream, java, optional, rekord, enum]

        // PUŁAPKA: chars() daje KODY znaków (liczby), nie litery.
        show("\"java\".chars()", "java".chars().boxed().toList()); // chars = znaki; boxed = opakuj (int → Integer)
        // WYNIK: "java".chars() → [106, 97, 118, 97]

        // flatMap(String::chars) się NIE skompiluje — IntStream to nie Stream<...>. Stąd flatMapToInt.
        long letterCount = words.stream()
                .flatMapToInt(String::chars)         // flatMapToInt = przekształć i spłaszcz do IntStream
                .count();
        show("liczba liter we wszystkich słowach", letterCount);
        // WYNIK: liczba liter we wszystkich słowach → 65
        show("kontrola: suma długości", words.stream().mapToInt(String::length).sum());
        // WYNIK: kontrola: suma długości → 65

        long aCount = words.stream()
                .flatMapToInt(String::chars)
                .filter(c -> c == 'a')               // porównujemy int z char — 'a' to kod 97
                .count();
        show("ile razy litera 'a'", aCount);
        // WYNIK: ile razy litera 'a' → 15

        String distinctLetters = words.stream()
                .flatMapToInt(String::chars)
                .distinct()
                .sorted()
                .mapToObj(c -> String.valueOf((char) c)) // mapToObj = przekształć na obiekt; (char) = rzutowanie int → char
                .collect(Collectors.joining());          // joining = sklej teksty (więcej: t16_streams/Streams09CollectorsBasic)
        show("użyte litery (bez powtórzeń)", distinctLetters);
        // WYNIK: użyte litery (bez powtórzeń) → abcdeijklmnoprstuv

        // DOBRA PRAKTYKA: chars() działa na 16-bitowych jednostkach UTF-16. Polskie litery (ą, ę, ł...) są OK,
        // ale emoji zajmuje 2 jednostki — wtedy użyj codePoints() (punkty kodowe Unicode).
        show("\"żółw\".chars().count()", "żółw".chars().count());
        // WYNIK: "żółw".chars().count() → 4
    }

    // =================================================================================================
    // 8. mapMulti (Java 16+) — KRÓTKO
    // =================================================================================================

    /**
     * 8. {@code mapMulti} (Java 16+) robi to samo co flatMap, ale zamiast zwracać strumień,
     * „wrzucasz” elementy do odbiornika ({@code downstream.accept(x)}) — 0, 1 albo wiele razy.
     */
    static void mapMultiDemo() {
        section("8. mapMulti (Java 16+) — flatMap bez tworzenia małych strumieni");

        // Liczba n → n kopii liczby n.
        List<Integer> repeated = Stream.of(1, 2, 3)
                .<Integer>mapMulti((n, downstream) -> {  // downstream = odbiornik „dalej w dół”
                    for (int i = 0; i < n; i++) {
                        downstream.accept(n);             // accept = przyjmij (wyślij dalej)
                    }
                })
                .toList();
        show("1, 2, 3 → n kopii n", repeated);
        // WYNIK: 1, 2, 3 → n kopii n → [1, 2, 2, 3, 3, 3]

        // Przykład z domeny: pozycje tylko z NIE-anulowanych zamówień — filtr i spłaszczenie w jednym kroku.
        List<OrderLine> activeLines = SampleData.orders().stream()
                .<OrderLine>mapMulti((order, sink) -> {   // sink = ujście, odbiornik
                    if (order.status() != OrderStatus.ANULOWANE) {
                        order.lines().forEach(sink);      // forEach(sink) = wyślij każdą pozycję dalej
                    }
                })
                .toList();
        show("pozycje z nieanulowanych zamówień", activeLines.size());
        // WYNIK: pozycje z nieanulowanych zamówień → 18

        // PUŁAPKA: bez „świadka typu” <OrderLine> przed mapMulti kompilator nie zgadnie typu wyniku
        // i dostaniesz Stream<Object> → błąd przy przypisaniu do List<OrderLine>.
        // DOBRA PRAKTYKA: domyślnie pisz flatMap (czytelniejszy). mapMulti opłaca się, gdy z jednego elementu
        // powstaje zwykle 0–2 elementy albo logika jest imperatywna (if, pętla) — nie tworzysz wtedy małych strumieni.
    }

    // =================================================================================================
    // 9. ILOCZYN KARTEZJAŃSKI — KAŻDY Z KAŻDYM
    // =================================================================================================

    /**
     * 9. Zagnieżdżony strumień wewnątrz flatMap = odpowiednik pętli w pętli.
     * Przykład: wszystkie warianty koszulki (rozmiar × kolor).
     */
    static void cartesianProduct() {
        section("9. Iloczyn kartezjański — każdy z każdym (pętla w pętli)");

        List<String> sizes = List.of("S", "M", "L");
        List<String> colours = List.of("czarny", "biały");   // colours = kolory

        // PRZED: pętla w pętli
        List<String> loopVariants = new ArrayList<>();
        for (String size : sizes) {
            for (String colour : colours) {
                loopVariants.add(size + "-" + colour);
            }
        }
        show("pętla w pętli", loopVariants);
        // WYNIK: pętla w pętli → [S-czarny, S-biały, M-czarny, M-biały, L-czarny, L-biały]

        // PO: zewnętrzna pętla = flatMap, wewnętrzna pętla = strumień + map w środku.
        List<String> variants = sizes.stream()
                .flatMap(size -> colours.stream().map(colour -> size + "-" + colour))
                .toList();
        show("flatMap + map", variants);
        // WYNIK: flatMap + map → [S-czarny, S-biały, M-czarny, M-biały, L-czarny, L-biały]
        show("liczba wariantów = 3 × 2", variants.size());
        // WYNIK: liczba wariantów = 3 × 2 → 6

        // Pary liczb (a < b) z zakresu 1..6, których suma to 7 — wewnętrzny zakres zaczyna się od a + 1.
        List<String> pairsTo7 = IntStream.rangeClosed(1, 6)  // rangeClosed = zakres z końcem włącznie
                .boxed()
                .flatMap(a -> IntStream.rangeClosed(a + 1, 6)
                        .filter(b -> a + b == 7)
                        .mapToObj(b -> a + "+" + b))
                .toList();
        show("pary o sumie 7", pairsTo7);
        // WYNIK: pary o sumie 7 → [1+6, 2+5, 3+4]

        // PUŁAPKA: wewnętrzny strumień trzeba tworzyć OD NOWA dla każdego elementu (colours.stream() w lambdzie).
        // Jeden wspólny strumień zostanie zużyty przy pierwszym rozmiarze.
        Stream<String> sharedColours = colours.stream();
        expectThrows("wspólny wewnętrzny strumień", () -> sizes.stream()
                .flatMap(size -> sharedColours.map(colour -> size + "-" + colour))
                .toList());
        // WYNIK: ✔ wspólny wewnętrzny strumień → rzucono IllegalStateException: stream has already been operated upon or closed

        // PUŁAPKA: rozmiar wyniku to iloczyn rozmiarów: 1000 × 1000 = 1 000 000 elementów. Uważaj na duże dane.
    }

    // =================================================================================================
    // 10. PUŁAPKA: KOLEKCJA RÓWNA NULL
    // =================================================================================================

    /** Koszyk ze „starego” kodu: ktoś zwraca null zamiast pustej listy (tak NIE robimy). */
    record Basket(String owner, List<String> items) { }

    /**
     * 10. Jeśli lista wewnątrz obiektu może być null, to {@code b.items().stream()} rzuci NullPointerException.
     * Naprawa: {@code Stream.ofNullable} albo warunek — a najlepiej: nigdy nie zwracaj null zamiast kolekcji.
     */
    static void nullCollectionPitfall() {
        section("10. PUŁAPKA: kolekcja równa null w flatMap");

        List<Basket> baskets = List.of(
                new Basket("Ala", List.of("chleb", "mleko")),
                new Basket("Bartek", null),                   // ← null zamiast List.of()
                new Basket("Celina", List.of("kawa")));

        // PUŁAPKA: wywołanie .stream() na null → NPE. Komunikat (Java 14+) mówi dokładnie, co było null.
        expectThrows("flatMap z listą null", () -> baskets.stream()
                .flatMap(basket -> basket.items().stream())
                .toList());
        // WYNIK: ✔ flatMap z listą null → rzucono NullPointerException: Cannot invoke "java.util.List.stream()" because the return value of "t16_streams.Streams04FlatMap$Basket.items()" is null

        // Naprawa 1: warunek — null zamieniamy na pusty strumień (Stream.empty = pusty strumień).
        List<String> fix1 = baskets.stream()
                .flatMap(basket -> basket.items() == null ? Stream.empty() : basket.items().stream())
                .toList();
        show("naprawa 1: warunek", fix1);
        // WYNIK: naprawa 1: warunek → [chleb, mleko, kawa]

        // Naprawa 2: Stream.ofNullable (Java 9+) daje 0 albo 1 listę, a drugi flatMap ją rozpakowuje.
        List<String> fix2 = baskets.stream()
                .flatMap(basket -> Stream.ofNullable(basket.items()))
                .flatMap(List::stream)
                .toList();
        show("naprawa 2: ofNullable", fix2);
        // WYNIK: naprawa 2: ofNullable → [chleb, mleko, kawa]

        // CIEKAWOSTKA: gdy SAMA funkcja w flatMap zwróci null (zamiast strumienia), flatMap potraktuje to
        // jak pusty strumień. Nie polegaj na tym — zwracaj Stream.empty(), bo czytelnik kodu tego nie wie.
        long quirk = Stream.of("a", "b").flatMap(s -> s.equals("a") ? Stream.of(s) : null).count();
        show("funkcja zwróciła null dla \"b\"", quirk);
        // WYNIK: funkcja zwróciła null dla "b" → 1

        // DOBRA PRAKTYKA: metoda zwracająca kolekcję NIGDY nie zwraca null — zwraca List.of().
        // W rekordzie zrób to w konstruktorze kompaktowym:  items = items == null ? List.of() : List.copyOf(items);
        // Rekordy z helpers.model (Order, Employee...) już tak robią, dlatego tam flatMap jest bezpieczny.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   ┌──────────────────────┬──────────────────────────────┬──────────────────────────────────────┐
     *   │                      │ map                          │ flatMap                              │
     *   ├──────────────────────┼──────────────────────────────┼──────────────────────────────────────┤
     *   │ z 1 elementu powstaje│ dokładnie 1                  │ 0, 1 albo wiele                      │
     *   │ funkcja zwraca       │ dowolny obiekt R             │ Stream<R>   (np. List::stream)       │
     *   │ na liście list daje  │ Stream<List<T>>              │ Stream<T>   (spłaszczone)            │
     *   │ przykład             │ Order → total()              │ Order → jego pozycje (lines)         │
     *   │ wersje prymitywne    │ mapToInt / Long / Double     │ flatMapToInt / Long / Double         │
     *   │ w Optional           │ Optional.map                 │ Optional.flatMap                     │
     *   └──────────────────────┴──────────────────────────────┴──────────────────────────────────────┘
     *   • lista list → .flatMap(List::stream);  rodzic z listą → .flatMap(o -> o.lines().stream())
     *   • potrzebujesz rodzica po spłaszczeniu? → map WEWNĄTRZ flatMap: o.lines().stream().map(l -> o.id() + l)
     *   • pomiń puste Optional → .flatMap(Optional::stream) (Java 9+); pole może być null → Stream.ofNullable (Java 9+)
     *   • String → znaki → .flatMapToInt(String::chars) (to są kody int!)
     *   • pętla w pętli → flatMap + map w środku; wewnętrzny strumień twórz od nowa w lambdzie
     *   • mapMulti (Java 16+) → .<Typ>mapMulti((x, sink) -> ...) — gdy logika jest imperatywna
     *   • lista może być null → NPE; naprawa: Stream.ofNullable / warunek; najlepiej nigdy nie zwracaj null
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się map od flatMap? Ile elementów może powstać z jednego elementu w każdym z nich?
     *   2. Co wypisze:  System.out.println(Stream.of(List.of(1, 2), List.of(), List.of(3)).map(List::size).toList());  ?
     *   3. Co wypisze:  System.out.println(Stream.of(List.of(1, 2), List.of(), List.of(3)).flatMap(List::stream).count());  ?
     *   4. ZNAJDŹ BŁĄD:  long n = words.stream().flatMap(String::chars).count();
     *   5. ZNAJDŹ BŁĄD:  List<String> e = customers.stream().map(Customer::findEmail).map(Optional::get).toList();
     *   6. Po .flatMap(o -> o.lines().stream()) chcesz wypisać numer zamówienia przy każdej pozycji. Jak?
     *   7. Dlaczego przy mapMulti często trzeba napisać .<OrderLine>mapMulti(...)?
     *   8. Lista w obiekcie może być null. Podaj dwa sposoby bezpiecznego spłaszczenia i jeden sposób „u źródła”.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        List<Order> orders = SampleData.orders();
        Check.equal("ćw. 1: SKU klienta 1", List.of("ELE-001", "ELE-003", "KSI-001", "KSI-002", "KSI-003", "SPO-003", "KSI-001"),
                () -> exercise1(orders, 1L));
        Check.equal("ćw. 1: SKU klienta 2", List.of("SPO-001", "SPO-002", "ELE-003"), () -> exercise1(orders, 2L));
        Check.equal("ćw. 2: sztuki ELEKTRONIKA bez anulowanych", 6, () -> exercise2(orders, Category.ELEKTRONIKA));
        Check.equal("ćw. 2: sztuki SPOZYWCZE bez anulowanych", 22, () -> exercise2(orders, Category.SPOZYWCZE));
        Check.equal("ćw. 3: e-maile klientów z zamówień", EXPECTED_EMAILS, () -> exercise3(orders));
        Check.equal("ćw. 4: pary z tego samego działu", EXPECTED_PAIRS, () -> exercise4(SampleData.employees()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec, klient 1)", List.of("ELE-001", "ELE-003", "KSI-001", "KSI-002", "KSI-003", "SPO-003", "KSI-001"),
                () -> solution1(orders, 1L));
        Check.equal("ćw. 1 (wzorzec, klient 2)", List.of("SPO-001", "SPO-002", "ELE-003"), () -> solution1(orders, 2L));
        Check.equal("ćw. 2 (wzorzec, ELEKTRONIKA)", 6, () -> solution2(orders, Category.ELEKTRONIKA));
        Check.equal("ćw. 2 (wzorzec, SPOZYWCZE)", 22, () -> solution2(orders, Category.SPOZYWCZE));
        Check.equal("ćw. 3 (wzorzec)", EXPECTED_EMAILS, () -> solution3(orders));
        Check.equal("ćw. 4 (wzorzec)", EXPECTED_PAIRS, () -> solution4(SampleData.employees()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    private static final List<String> EXPECTED_EMAILS =
            List.of("jan@example.com", "adam.mazur@example.com", "zofia@example.com", "marek@example.com");

    private static final List<String> EXPECTED_PAIRS = List.of(
            "Anna Nowak + Piotr Kowalski",
            "Anna Nowak + Michał Lewandowski",
            "Anna Nowak + Ewa Woźniak",
            "Piotr Kowalski + Michał Lewandowski",
            "Piotr Kowalski + Ewa Woźniak",
            "Tomasz Wójcik + Krzysztof Szymański",
            "Michał Lewandowski + Ewa Woźniak",
            "Agnieszka Zielińska + Paweł Dąbrowski");

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ na strumień. Kod z pętlami zbiera SKU wszystkich pozycji
     * ze wszystkich zamówień danego klienta (z powtórzeniami, w kolejności zamówień):
     * <pre>{@code
     * List<String> result = new ArrayList<>();
     * for (Order order : orders) {
     *     if (order.customer().id() == customerId) {
     *         for (OrderLine line : order.lines()) {
     *             result.add(line.product().sku());
     *         }
     *     }
     * }
     * return result;
     * }</pre>
     * Podpowiedź: zewnętrzna pętla + if = stream + filter; wewnętrzna pętla = flatMap; add = map + toList.
     */
    static List<String> exercise1(List<Order> orders, long customerId) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (średnie): Policz, ile SZTUK produktów z danej kategorii sprzedano we wszystkich
     * zamówieniach, które NIE są anulowane (status ANULOWANE pomijamy).
     * Podpowiedź: filter na zamówieniach → flatMap na pozycje → filter po {@code line.product().category()}
     * → {@code mapToInt(OrderLine::quantity).sum()}.
     */
    static int exercise2(List<Order> orders, Category category) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): Zwróć e-maile klientów, którzy złożyli zamówienia — bez powtórzeń,
     * w kolejności pierwszego zamówienia. Klientów bez e-maila pomiń.
     * Podpowiedź: {@code map(Order::customer)} → {@code map(Customer::findEmail)} →
     * {@code flatMap(Optional::stream)} → distinct.
     */
    static List<String> exercise3(List<Order> orders) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): Zwróć wszystkie PARY pracowników z tego samego działu w formacie
     * „Imię Nazwisko + Imię Nazwisko”. Każda para tylko raz, bez pary „sam ze sobą”. Kolejność: jak w
     * podwójnej pętli po indeksach {@code i < j} (i = pierwszy pracownik, j = drugi, zawsze dalej na liście).
     * Podpowiedź: {@code IntStream.range(0, n).boxed().flatMap(i -> IntStream.range(i + 1, n)...)},
     * w środku filter „ten sam dział” i {@code mapToObj} na tekst.
     */
    static List<String> exercise4(List<Employee> employees) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(List<Order> orders, long customerId) {
        return orders.stream()
                .filter(order -> order.customer().id() == customerId)
                .flatMap(order -> order.lines().stream())
                .map(line -> line.product().sku())
                .toList();
    }

    static int solution2(List<Order> orders, Category category) {
        return orders.stream()
                .filter(order -> order.status() != OrderStatus.ANULOWANE)
                .flatMap(order -> order.lines().stream())
                .filter(line -> line.product().category() == category)
                .mapToInt(OrderLine::quantity)
                .sum();
    }

    static List<String> solution3(List<Order> orders) {
        return orders.stream()
                .map(Order::customer)
                .map(Customer::findEmail)
                .flatMap(Optional::stream)
                .distinct()
                .toList();
    }

    static List<String> solution4(List<Employee> employees) {
        int n = employees.size();
        return IntStream.range(0, n)                              // range = zakres [0, n)
                .boxed()
                .flatMap(i -> IntStream.range(i + 1, n)           // j zaczyna się od i + 1 → każda para raz
                        .filter(j -> employees.get(i).department() == employees.get(j).department())
                        .mapToObj(j -> employees.get(i).name() + " + " + employees.get(j).name()))
                .toList();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. map: 1 element → dokładnie 1 wynik. flatMap: 1 element → strumień 0..n elementów, a wszystkie
     *      te strumienie są sklejane w jeden płaski strumień.
     *   2. [2, 0, 1] — map daje rozmiar każdej listy: [1, 2] → 2, [] → 0, [3] → 1. Nadal 3 elementy.
     *   3. 3 — spłaszczone liczby to 1, 2, 3; pusta lista nic nie dokłada.
     *   4. Nie skompiluje się: String::chars zwraca IntStream, a flatMap chce Stream. Użyj flatMapToInt(String::chars).
     *   5. Optional::get na pustym Optional (Maria Nowak, Ola Pawlak) → NoSuchElementException.
     *      Poprawnie: .map(Customer::findEmail).flatMap(Optional::stream).toList().
     *   6. map wewnątrz flatMap, gdzie zmienna order jest jeszcze widoczna:
     *      .flatMap(o -> o.lines().stream().map(line -> o.id() + ": " + line))
     *   7. Lambda w mapMulti nic nie zwraca, więc kompilator nie ma skąd wziąć typu wyniku → wyszedłby Stream<Object>.
     *      Świadek typu <OrderLine> mówi wprost, jakie elementy wrzucamy do odbiornika.
     *   8. (a) .flatMap(b -> b.items() == null ? Stream.empty() : b.items().stream())
     *      (b) .flatMap(b -> Stream.ofNullable(b.items())).flatMap(List::stream)   (Java 9+)
     *      „u źródła”: metoda/rekord nigdy nie zwraca null — zwraca List.of() (np. w konstruktorze kompaktowym).
     */
    // </editor-fold>
}
