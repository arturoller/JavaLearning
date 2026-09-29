package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Product;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Leniwość strumieni — przepis, pionowe przetwarzanie, bariery, skróty, nieskończoność
 *        (laziness = leniwość; lazy = leniwy — „nie robi niczego, dopóki nie musi”)
 *
 * W SKRÓCIE:
 *   Operacje pośrednie (filter, map, sorted...) tylko DOPISUJĄ kroki do przepisu. Praca rusza dopiero
 *   przy operacji końcowej — i to element po elemencie, pionowo przez cały potok. Dzięki temu strumień
 *   potrafi skończyć wcześniej (findFirst, limit), a nawet obsłużyć nieskończone źródło.
 *
 * ANALOGIA: Kuchnia w restauracji. Przepis (potok) leży w szufladzie — nic się nie gotuje.
 *   Gotowanie zaczyna się, gdy przyjdzie zamówienie (operacja końcowa). Kucharz robi JEDEN talerz
 *   od początku do końca (pionowo), a gdy gość zamówił tylko 2 porcje (limit), nie gotuje trzeciej.
 *   Wyjątek: „ułóż talerze od najmniejszego” (sorted) — tu trzeba najpierw zrobić WSZYSTKIE.
 *
 * JAK TO DZIAŁA:
 *   operacja                 rodzaj                 co robi z przepływem
 *   filter, map, peek        bezstanowa             element przechodzi dalej od razu (pionowo)
 *   distinct                 stanowa (pamięta)      przepuszcza od razu, ale pamięta widziane
 *   sorted                   stanowa — BARIERA      musi zobaczyć WSZYSTKO, zanim wypuści pierwszy
 *   limit, takeWhile         skracająca             po osiągnięciu celu przestaje pobierać
 *   findFirst, anyMatch...   końcowa skracająca     kończy, gdy wynik jest znany
 *   count, toList, forEach   końcowa                uruchamia cały przepis
 *
 * SŁÓWKA:
 *   lazy = leniwy; eager = gorliwy (liczy od razu); pipeline = potok; intermediate = pośrednia;
 *   terminal = końcowa; stateless = bezstanowa; stateful = stanowa; barrier = bariera;
 *   short-circuit = skrót (zakończenie przed czasem); infinite = nieskończony; iterate = powtarzaj krok;
 *   generate = generuj; take while = bierz, dopóki; supplier = dostawca; consumed = zużyty; trace = ślad
 *
 * ZOBACZ TEŻ: t16_streams/Streams01Intro (pierwsze spotkanie z leniwością), t16_streams/Streams02Creation
 *   (iterate, generate), t16_streams/Streams05SortDistinctLimit (sorted, limit, takeWhile),
 *   t16_streams/Streams17SideEffectsPitfalls (dlaczego println w lambdzie to efekt uboczny),
 *   t13_lambdas/Lambda03JavaUtilFunction (Supplier)
 * </pre>
 */
public class Streams16Laziness {

    public static void main(String[] args) {
        title("Streams16 — leniwość strumieni");

        recipeNotCooking();        // recipe, not cooking = przepis, a nie gotowanie
        verticalProcessing();      // vertical processing = przetwarzanie pionowe
        barriers();                // barriers = bariery
        shortCircuit();            // short-circuit = skróty (zakończenie przed czasem)
        infiniteStreams();         // infinite streams = strumienie nieskończone
        limitEarlyOrLate();        // limit early or late = limit wcześnie czy późno
        supplierForDefaults();     // supplier for defaults = dostawca wartości domyślnych
        iterateWithPredicate();    // iterate with predicate = iterate z warunkiem
        consumedStream();          // consumed stream = zużyty strumień
        peekSkippedByCount();      // peek skipped by count = peek pominięty przez count
        exercises();               // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PRZEPIS, A NIE GOTOWANIE
    // =================================================================================================

    /**
     * 1. Budowanie potoku nic nie liczy. Dziennik (log) pokazuje, że lambdy w filter i map ruszają
     * dopiero przy operacji końcowej. (Dopisywanie do listy w lambdzie = efekt uboczny — tylko do pokazu!)
     */
    static void recipeNotCooking() {
        section("1. Przepis, a nie gotowanie");

        List<String> animals = List.of("kot", "pies", "mysz");
        List<String> log = new ArrayList<>();

        Stream<String> recipe = animals.stream()
                .filter(a -> {
                    log.add("filter " + a);
                    return a.length() > 3;
                })
                .map(a -> {
                    log.add("map " + a);
                    return a.toUpperCase(Locale.ROOT);   // toUpperCase = na wielkie litery
                });
        show("dziennik po ZBUDOWANIU potoku", log);
        // WYNIK: dziennik po ZBUDOWANIU potoku → []

        List<String> result = recipe.toList();   // operacja końcowa = „zamówienie przyszło”
        show("wynik", result);
        // WYNIK: wynik → [PIES, MYSZ]
        show("dziennik po toList()", log);
        // WYNIK: dziennik po toList() → [filter kot, filter pies, map pies, filter mysz, map mysz]

        // Zauważ kolejność: NIE „trzy razy filter, potem dwa razy map”, tylko przeplatanie — sekcja 2.
        // DOBRA PRAKTYKA: potok bez operacji końcowej to martwy kod — IDE często to podkreśla.
    }

    // =================================================================================================
    // 2. PRZETWARZANIE PIONOWE (operacje bezstanowe)
    // =================================================================================================

    /**
     * 2. Każdy element przechodzi CAŁY potok (filter → map → forEach), zanim ruszy następny.
     * Pętle z listami pośrednimi działają „poziomo”: najpierw wszystko przez filtr, potem wszystko przez map.
     */
    static void verticalProcessing() {
        section("2. Przetwarzanie pionowe (filter, map, peek)");

        List<String> animals = List.of("kot", "pies", "mysz");

        // PRZED: „poziomo” — pętla za pętlą, z listą pośrednią.
        List<String> horizontal = new ArrayList<>();
        List<String> filtered = new ArrayList<>();
        for (String a : animals) {
            horizontal.add("filter " + a);
            if (a.length() > 3) {
                filtered.add(a);
            }
        }
        for (String a : filtered) {
            horizontal.add("map " + a);
        }
        showEach("pętle (poziomo)", horizontal);
        // WYNIK: pętle (poziomo) (liczba elementów: 5):
        // WYNIK: • filter kot
        // WYNIK: • filter pies
        // WYNIK: • filter mysz
        // WYNIK: • map pies
        // WYNIK: • map mysz

        // PO: strumień — pionowo. forEach = dla każdego (operacja końcowa).
        List<String> vertical = new ArrayList<>();
        animals.stream()
                .filter(a -> {
                    vertical.add("filter " + a);
                    return a.length() > 3;
                })
                .map(a -> {
                    vertical.add("map " + a);
                    return a.toUpperCase(Locale.ROOT);
                })
                .forEach(a -> vertical.add("forEach " + a));
        showEach("strumień (pionowo)", vertical);
        // WYNIK: strumień (pionowo) (liczba elementów: 7):
        // WYNIK: • filter kot
        // WYNIK: • filter pies
        // WYNIK: • map pies
        // WYNIK: • forEach PIES
        // WYNIK: • filter mysz
        // WYNIK: • map mysz
        // WYNIK: • forEach MYSZ

        // DLACZEGO to ważne? Brak list pośrednich (mniej pamięci) i możliwość zakończenia w połowie
        // (skróty, sekcja 4). „kot” odpadł na filtrze i nigdy nie dotarł do map.
    }

    // =================================================================================================
    // 3. BARIERY: sorted (i dlaczego distinct nią nie jest)
    // =================================================================================================

    /**
     * 3. sorted musi zobaczyć WSZYSTKIE elementy, zanim wypuści pierwszy (najmniejszy może być na końcu).
     * To bariera: przed nią wszystko, dopiero potem dalej. distinct jest stanowy, ale w strumieniu
     * sekwencyjnym przepuszcza elementy od razu — pamięta tylko, co już widział.
     */
    static void barriers() {
        section("3. Bariery: sorted kontra distinct");

        List<String> log = new ArrayList<>();
        List<String> sorted = Stream.of("pies", "kot", "mysz", "lis")
                .peek(a -> log.add("przed sorted: " + a))   // peek = podejrzyj (bez zmiany elementu)
                .sorted()
                .peek(a -> log.add("po sorted: " + a))
                .toList();
        show("wynik sorted", sorted);
        // WYNIK: wynik sorted → [kot, lis, mysz, pies]
        showEach("ślad sorted", log);
        // WYNIK: ślad sorted (liczba elementów: 8):
        // WYNIK: • przed sorted: pies
        // WYNIK: • przed sorted: kot
        // WYNIK: • przed sorted: mysz
        // WYNIK: • przed sorted: lis
        // WYNIK: • po sorted: kot
        // WYNIK: • po sorted: lis
        // WYNIK: • po sorted: mysz
        // WYNIK: • po sorted: pies

        // sorted + limit: bariera i tak ZJADA wszystko ze źródła — limit oszczędza tylko to, co za nią.
        AtomicInteger before = new AtomicInteger();   // AtomicInteger = licznik, który można zmieniać w lambdzie
        AtomicInteger after = new AtomicInteger();
        List<Integer> twoSmallest = SampleData.numbers().stream()
                .peek(n -> before.incrementAndGet())
                .sorted()
                .peek(n -> after.incrementAndGet())
                .limit(2)
                .toList();
        show("sorted().limit(2)", twoSmallest + ", przed sorted: " + before.get() + ", po sorted: " + after.get());
        // WYNIK: sorted().limit(2) → [1, 2], przed sorted: 12, po sorted: 2

        // distinct: stanowy, ale NIE bariera (w strumieniu sekwencyjnym) — duplikat po prostu odpada.
        List<String> distinctLog = new ArrayList<>();
        List<String> distinct = Stream.of("a", "b", "a", "c")
                .peek(s -> distinctLog.add("przed distinct: " + s))
                .distinct()
                .peek(s -> distinctLog.add("po distinct: " + s))
                .toList();
        show("wynik distinct", distinct);
        // WYNIK: wynik distinct → [a, b, c]
        showEach("ślad distinct", distinctLog);
        // WYNIK: ślad distinct (liczba elementów: 7):
        // WYNIK: • przed distinct: a
        // WYNIK: • po distinct: a
        // WYNIK: • przed distinct: b
        // WYNIK: • po distinct: b
        // WYNIK: • przed distinct: a
        // WYNIK: • przed distinct: c
        // WYNIK: • po distinct: c

        // PUŁAPKA: distinct PAMIĘTA wszystkie widziane elementy — na ogromnym strumieniu zje dużo pamięci.
        // W parallel (Streams18) distinct z zachowaniem kolejności zachowuje się jak bariera.
    }

    // =================================================================================================
    // 4. SKRÓTY: findFirst, anyMatch, limit, takeWhile
    // =================================================================================================

    /**
     * 4. Operacje skracające kończą pracę, gdy wynik jest już znany. Licznik pokazuje, ile elementów
     * ze źródła naprawdę pobrano. SampleData.numbers() = [5, 3, 8, 1, 9, 2, 7, 3, 10, 6, 4, 8].
     */
    static void shortCircuit() {
        section("4. Skróty: findFirst, anyMatch, limit, takeWhile");

        List<Integer> numbers = SampleData.numbers();
        AtomicInteger read = new AtomicInteger();

        Optional<Integer> firstBig = watched(numbers, read).filter(n -> n > 7).findFirst();
        show("findFirst(n > 7)", firstBig + ", pobrano: " + read.get() + " z 12");
        // WYNIK: findFirst(n > 7) → Optional[8], pobrano: 3 z 12

        boolean hasOne = watched(numbers, read).anyMatch(n -> n == 1);   // anyMatch = czy jakikolwiek pasuje
        show("anyMatch(n == 1)", hasOne + ", pobrano: " + read.get() + " z 12");
        // WYNIK: anyMatch(n == 1) → true, pobrano: 4 z 12

        boolean allAboveTwo = watched(numbers, read).allMatch(n -> n > 2);   // allMatch = czy wszystkie pasują
        show("allMatch(n > 2)", allAboveTwo + ", pobrano: " + read.get() + " z 12");
        // WYNIK: allMatch(n > 2) → false, pobrano: 4 z 12

        boolean noneHuge = watched(numbers, read).noneMatch(n -> n > 100);   // noneMatch = czy żaden nie pasuje
        show("noneMatch(n > 100)", noneHuge + ", pobrano: " + read.get() + " z 12");
        // WYNIK: noneMatch(n > 100) → true, pobrano: 12 z 12
        // noneMatch musiał obejrzeć wszystko — żeby powiedzieć „żaden”, trzeba sprawdzić każdego.

        List<Integer> threeEven = watched(numbers, read).filter(n -> n % 2 == 0).limit(3).toList();
        show("filter(parzyste).limit(3)", threeEven + ", pobrano: " + read.get() + " z 12");
        // WYNIK: filter(parzyste).limit(3) → [8, 2, 10], pobrano: 9 z 12

        List<Integer> prefix = watched(numbers, read).takeWhile(n -> n < 9).toList();   // takeWhile (Java 9+)
        show("takeWhile(n < 9)", prefix + ", pobrano: " + read.get() + " z 12");
        // WYNIK: takeWhile(n < 9) → [5, 3, 8, 1], pobrano: 5 z 12

        // Dla porównania: count() po filtrze nie ma skrótu — musi policzyć wszystko.
        long bigCount = watched(numbers, read).filter(n -> n > 7).count();
        show("filter(n > 7).count()", bigCount + ", pobrano: " + read.get() + " z 12");
        // WYNIK: filter(n > 7).count() → 4, pobrano: 12 z 12

        // PUŁAPKA: takeWhile ≠ filter. takeWhile kończy przy PIERWSZYM niepasującym (9) — dalsze 2, 7, 3
        // też są mniejsze od 9, ale już ich nie zobaczy. Ma sens głównie dla danych posortowanych.
    }

    /** Strumień z licznikiem pobranych elementów (licznik zerowany na starcie). Tylko do pokazu! */
    static Stream<Integer> watched(List<Integer> source, AtomicInteger counter) {
        counter.set(0);
        return source.stream().peek(n -> counter.incrementAndGet());
    }

    // =================================================================================================
    // 5. STRUMIENIE NIESKOŃCZONE + limit / takeWhile
    // =================================================================================================

    /**
     * 5. {@code Stream.iterate(start, krok)} i {@code Stream.generate(dostawca)} nie mają końca.
     * Działają tylko dzięki leniwości — o końcu decyduje operacja skracająca (limit, takeWhile, findFirst).
     */
    static void infiniteStreams() {
        section("5. Strumienie nieskończone + limit / takeWhile");

        // iterate = powtarzaj krok: 1, 2, 4, 8, ... bez końca. limit(10) mówi: „wystarczy 10”.
        List<Integer> powers = Stream.iterate(1, n -> n * 2).limit(10).toList();
        show("10 potęg dwójki", powers);
        // WYNIK: 10 potęg dwójki → [1, 2, 4, 8, 16, 32, 64, 128, 256, 512]

        // takeWhile — koniec wyznacza WARUNEK, a nie liczba sztuk.
        List<Integer> below1000 = Stream.iterate(1, n -> n * 2).takeWhile(n -> n < 1000).toList();
        show("potęgi dwójki < 1000", below1000);
        // WYNIK: potęgi dwójki < 1000 → [1, 2, 4, 8, 16, 32, 64, 128, 256, 512]

        // generate = generuj z dostawcy. Random ZE ZIARNEM (seed = 42) → zawsze te same „losowe” liczby.
        Random random = new Random(42);
        List<Integer> dice = Stream.generate(() -> random.nextInt(6) + 1).limit(5).toList();
        show("5 rzutów kostką (seed 42)", dice);
        // WYNIK: 5 rzutów kostką (seed 42) → [3, 4, 1, 3, 1]

        // PUŁAPKI (NIE uruchamiamy — program by się zawiesił):
        //   Stream.iterate(1, n -> n + 1).filter(n -> n < 5).toList();
        //       filter nie wie, że większe liczby nie przejdą — szuka w nieskończoność. Użyj takeWhile.
        //   Stream.iterate(1, n -> n + 1).filter(n -> n < 5).limit(5).toList();
        //       są tylko 4 pasujące (1..4), a limit czeka na piąty — w nieskończoność.
        //   Stream.iterate(1, n -> n + 1).sorted().limit(3).toList();
        //       sorted to bariera — chce zobaczyć WSZYSTKO z nieskończonego źródła (brak pamięci).
        // DOBRA PRAKTYKA: przy nieskończonym źródle od razu pisz ogranicznik (limit/takeWhile) i sprawdź,
        // czy przed nim nie stoi bariera (sorted).
    }

    // =================================================================================================
    // 6. limit WCZEŚNIE CZY PÓŹNO
    // =================================================================================================

    /**
     * 6. Miejsce limit zmienia (a) WYNIK — bo filtruje co innego, (b) KOSZT — gdy w potoku jest bariera.
     */
    static void limitEarlyOrLate() {
        section("6. limit wcześnie czy późno");

        List<Integer> numbers = SampleData.numbers();

        // (a) Znaczenie: „3 pierwsze parzyste” to nie to samo co „parzyste spośród 3 pierwszych”.
        show("filter(parzyste).limit(3)", numbers.stream().filter(n -> n % 2 == 0).limit(3).toList());
        // WYNIK: filter(parzyste).limit(3) → [8, 2, 10]
        show("limit(3).filter(parzyste)", numbers.stream().limit(3).filter(n -> n % 2 == 0).toList());
        // WYNIK: limit(3).filter(parzyste) → [8]

        // (b) Koszt: „droga” operacja (np. zapytanie do bazy) PRZED barierą wykona się dla wszystkich.
        AtomicInteger calls = new AtomicInteger();
        List<Integer> late = numbers.stream()
                .map(n -> {
                    calls.incrementAndGet();
                    return n * 10;
                })
                .sorted()
                .limit(3)
                .toList();
        show("map → sorted → limit(3)", late + ", drogich wywołań: " + calls.get());
        // WYNIK: map → sorted → limit(3) → [10, 20, 30], drogich wywołań: 12

        calls.set(0);
        List<Integer> early = numbers.stream()
                .sorted()
                .limit(3)
                .map(n -> {
                    calls.incrementAndGet();
                    return n * 10;
                })
                .toList();
        show("sorted → limit(3) → map", early + ", drogich wywołań: " + calls.get());
        // WYNIK: sorted → limit(3) → map → [10, 20, 30], drogich wywołań: 3

        // Bez bariery leniwość sama pilnuje kosztu: map przed limit wywoła się tylko 3 razy.
        calls.set(0);
        List<Integer> noBarrier = numbers.stream()
                .map(n -> {
                    calls.incrementAndGet();
                    return n * 10;
                })
                .limit(3)
                .toList();
        show("map → limit(3) (bez bariery)", noBarrier + ", drogich wywołań: " + calls.get());
        // WYNIK: map → limit(3) (bez bariery) → [50, 30, 80], drogich wywołań: 3

        // DOBRA PRAKTYKA: filtruj i ograniczaj jak najwcześniej, drogie map przenoś ZA sorted/limit —
        // ale tylko wtedy, gdy nie zmienia to wyniku (tu n * 10 zachowuje kolejność, więc wolno).
    }

    // =================================================================================================
    // 7. Supplier DLA KOSZTOWNYCH WARTOŚCI DOMYŚLNYCH
    // =================================================================================================

    /**
     * 7. Argument metody Java oblicza ZAWSZE przed jej wywołaniem. {@code orElse(load())} woła load()
     * nawet wtedy, gdy wartość jest. {@code orElseGet(() -> load())} przekazuje przepis (Supplier) —
     * wołany tylko w razie potrzeby. To ta sama leniwość, co w strumieniach.
     */
    static void supplierForDefaults() {
        section("7. Supplier dla kosztownych wartości domyślnych");

        List<Product> products = SampleData.products();
        AtomicInteger loads = new AtomicInteger();

        // Wartość JEST (ELE-001 istnieje), a mimo to orElse „ładuje” domyślną.
        String a = products.stream()
                .filter(p -> p.sku().equals("ELE-001"))
                .findFirst()
                .map(Product::name)
                .orElse(loadDefaultName(loads));
        show("orElse", a + ", ładowań domyślnej: " + loads.get());
        // WYNIK: orElse → Laptop Pro 14, ładowań domyślnej: 1

        loads.set(0);
        String b = products.stream()
                .filter(p -> p.sku().equals("ELE-001"))
                .findFirst()
                .map(Product::name)
                .orElseGet(() -> loadDefaultName(loads));   // orElseGet = albo pobierz (z dostawcy)
        show("orElseGet", b + ", ładowań domyślnej: " + loads.get());
        // WYNIK: orElseGet → Laptop Pro 14, ładowań domyślnej: 0

        loads.set(0);
        String c = products.stream()
                .filter(p -> p.sku().equals("XYZ-999"))
                .findFirst()
                .map(Product::name)
                .orElseGet(() -> loadDefaultName(loads));
        show("orElseGet (brak produktu)", c + ", ładowań domyślnej: " + loads.get());
        // WYNIK: orElseGet (brak produktu) → Produkt domyślny, ładowań domyślnej: 1

        // Ten sam pomysł we własnym kodzie: komunikat do logu jako Supplier<String>.
        // Kosztowny raport budujemy TYLKO, gdy debug jest włączony.
        AtomicInteger reports = new AtomicInteger();
        debug(false, () -> buildReport(products, reports));
        show("raporty zbudowane (debug wyłączony)", reports.get());
        // WYNIK: raporty zbudowane (debug wyłączony) → 0
        debug(true, () -> buildReport(products, reports));
        // WYNIK: ℹ DEBUG: 14 produktów, 12 na stanie
        show("raporty zbudowane (debug włączony)", reports.get());
        // WYNIK: raporty zbudowane (debug włączony) → 1

        // DOBRA PRAKTYKA: stała albo gotowa zmienna → orElse("brak"). Wywołanie metody, new, zapytanie → orElseGet.
    }

    /** Udaje „drogie” wczytanie wartości domyślnej (np. z bazy danych) i liczy wywołania. */
    static String loadDefaultName(AtomicInteger loads) {
        loads.incrementAndGet();
        return "Produkt domyślny";
    }

    /** Wypisuje komunikat tylko przy włączonym debug. Supplier = przepis na tekst, liczony na żądanie. */
    static void debug(boolean enabled, Supplier<String> message) {
        if (enabled) {
            note("DEBUG: " + message.get());
        }
    }

    static String buildReport(List<Product> products, AtomicInteger reports) {
        reports.incrementAndGet();
        long inStock = products.stream().filter(Product::inStock).count();
        return products.size() + " produktów, " + inStock + " na stanie";
    }

    // =================================================================================================
    // 8. iterate Z WARUNKIEM (Java 9+)
    // =================================================================================================

    /**
     * 8. {@code Stream.iterate(start, warunek, krok)} (Java 9+) to strumieniowy odpowiednik pętli
     * {@code for (start; warunek; krok)}. Strumień jest skończony — nie potrzeba limit ani takeWhile.
     */
    static void iterateWithPredicate() {
        section("8. iterate z warunkiem (Java 9+)");

        // PRZED: for (int n = 1; n <= 100; n *= 3) { ... }
        List<Integer> loop = new ArrayList<>();
        for (int n = 1; n <= 100; n *= 3) {
            loop.add(n);
        }
        show("pętla for", loop);
        // WYNIK: pętla for → [1, 3, 9, 27, 81]

        // PO: te same trzy części — start, warunek, krok.
        List<Integer> stream = Stream.iterate(1, n -> n <= 100, n -> n * 3).toList();
        show("iterate(1, n <= 100, n * 3)", stream);
        // WYNIK: iterate(1, n <= 100, n * 3) → [1, 3, 9, 27, 81]

        // Daty: poniedziałki stycznia 2026 (5 stycznia to poniedziałek). plusWeeks = dodaj tygodnie.
        // Więcej o datach: t17_datetime/DateTime01LocalDateTime.
        List<LocalDate> mondays = Stream.iterate(LocalDate.of(2026, 1, 5),
                        d -> d.getMonthValue() == 1,
                        d -> d.plusWeeks(1))
                .toList();
        show("poniedziałki w styczniu 2026", mondays);
        // WYNIK: poniedziałki w styczniu 2026 → [2026-01-05, 2026-01-12, 2026-01-19, 2026-01-26]

        // Fibonacci: stan to para liczb [a, b]; krok: [b, a + b]. Bierzemy pierwszą z pary.
        List<Long> fibonacci = Stream.iterate(new long[]{0, 1}, f -> new long[]{f[1], f[0] + f[1]})
                .limit(10)
                .map(f -> f[0])
                .toList();
        show("10 liczb Fibonacciego", fibonacci);
        // WYNIK: 10 liczb Fibonacciego → [0, 1, 1, 2, 3, 5, 8, 13, 21, 34]

        // PUŁAPKA: warunek jest sprawdzany PRZED pierwszym elementem (jak w for) — zły start = pusty strumień.
        show("iterate(200, n <= 100, ...)", Stream.iterate(200, n -> n <= 100, n -> n * 3).toList());
        // WYNIK: iterate(200, n <= 100, ...) → []
    }

    // =================================================================================================
    // 9. ZUŻYTY STRUMIEŃ — powtórka
    // =================================================================================================

    /**
     * 9. Strumień to jednorazowy przepływ, nie kolekcja. Po operacji końcowej jest zużyty.
     * Chcesz „ten sam strumień” wiele razy? Przechowuj przepis: {@code Supplier<Stream<T>>}.
     */
    static void consumedStream() {
        section("9. Zużyty strumień — powtórka");

        List<String> words = SampleData.words();
        Stream<String> longWords = words.stream().filter(w -> w.length() > 4);

        show("pierwsze użycie: count()", longWords.count());
        // WYNIK: pierwsze użycie: count() → 7

        expectThrows("drugie użycie tego samego strumienia", () -> longWords.toList());
        // WYNIK: ✔ drugie użycie tego samego strumienia → rzucono IllegalStateException: stream has already been operated upon or closed

        // Rozwiązanie: Supplier (dostawca) — przepis, który przy każdym get() buduje NOWY strumień.
        Supplier<Stream<String>> longWordsRecipe = () -> words.stream().filter(w -> w.length() > 4);
        show("przepis → count()", longWordsRecipe.get().count());
        // WYNIK: przepis → count() → 7
        show("przepis → distinct().toList()", longWordsRecipe.get().distinct().toList());
        // WYNIK: przepis → distinct().toList() → [stream, lambda, kolekcja, lista, optional, rekord]

        // DOBRA PRAKTYKA: nie trzymaj strumieni w polach ani zmiennych „na później”. Trzymaj kolekcję
        // (źródło) albo Supplier i twórz strumień tuż przed użyciem.
    }

    // =================================================================================================
    // 10. peek POMIJANY PRZEZ count — powtórka
    // =================================================================================================

    /**
     * 10. Od Javy 9 {@code count()} potrafi policzyć elementy BEZ przepuszczania ich przez potok, jeśli
     * zna rozmiar z góry (lista + tylko map/peek). Wtedy lambdy w peek i map w ogóle się nie wykonują.
     */
    static void peekSkippedByCount() {
        section("10. peek pominięty przez count — powtórka");

        List<String> letters = List.of("a", "b", "c");
        AtomicInteger peeks = new AtomicInteger();

        long sized = letters.stream().peek(s -> peeks.incrementAndGet()).count();
        show("lista → peek → count", sized + ", wywołań peek: " + peeks.get());
        // WYNIK: lista → peek → count → 3, wywołań peek: 0

        peeks.set(0);
        long mapped = letters.stream()
                .map(s -> {
                    peeks.incrementAndGet();
                    return s.toUpperCase(Locale.ROOT);
                })
                .count();
        show("lista → map → count", mapped + ", wywołań map: " + peeks.get());
        // WYNIK: lista → map → count → 3, wywołań map: 0

        // filter zmienia (potencjalnie) liczbę elementów → rozmiar nieznany → trzeba przejść potok.
        peeks.set(0);
        long filtered = letters.stream().peek(s -> peeks.incrementAndGet()).filter(s -> true).count();
        show("lista → peek → filter → count", filtered + ", wywołań peek: " + peeks.get());
        // WYNIK: lista → peek → filter → count → 3, wywołań peek: 3

        // PUŁAPKA: jeśli Twój kod „coś robi” w peek/map (zapis, licznik, wysyłka maila), a potok kończy
        // count(), to może się to po prostu NIE wydarzyć. Efekty uboczne → forEach, nie peek/map (Streams17).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • operacje pośrednie tylko budują przepis; nic się nie dzieje bez operacji końcowej
     *   • PIONOWO (filter, map, peek, flatMap): każdy element przechodzi CAŁY potok, zanim ruszy następny
     *
     *       el.1 ──► filter ──► map ──► wynik
     *       el.2 ──► filter ✗                      (odpada — map go nie zobaczy)
     *       el.3 ──► filter ──► map ──► wynik
     *
     *   • BARIERA (sorted): zbiera WSZYSTKO, dopiero potem wypuszcza dalej
     *
     *       el.1 ──► filter ──┐
     *       el.2 ──► filter ──┼──► [ sorted czeka na komplet ] ──► el.A ──► map ──► wynik
     *       el.3 ──► filter ──┘                                  ──► el.B ──► map ──► wynik
     *
     *   • distinct: stanowy (pamięta widziane), ale w sekwencyjnym przepuszcza od razu
     *   • SKRÓTY: findFirst, findAny, anyMatch, allMatch, noneMatch, limit, takeWhile — kończą, gdy wynik znany
     *     (noneMatch/allMatch „true” muszą obejrzeć wszystko)
     *   • nieskończone: iterate(start, krok), generate(dostawca) + limit / takeWhile; bez sorted przed ogranicznikiem!
     *   • iterate(start, warunek, krok) (9+) = pętla for w strumieniu (skończony)
     *   • limit: miejsce zmienia WYNIK (filter→limit ≠ limit→filter) i KOSZT (drogie map za sorted/limit)
     *   • orElse(x) liczy x zawsze; orElseGet(() -> x) — tylko gdy trzeba; kosztowne rzeczy → Supplier
     *   • strumień jest jednorazowy → IllegalStateException; wielokrotnie: Supplier<Stream<T>>
     *   • count() na znanym rozmiarze pomija peek/map (9+) — nie polegaj na efektach ubocznych
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  Stream.of(1, 2, 3).peek(System.out::println).map(n -> n * 2);  ?
     *   2. Co wypisze:  Stream.of("b", "a", "c").peek(System.out::print).sorted()
     *          .forEach(s -> System.out.print(s.toUpperCase()));  ?
     *   3. Co wypisze:  Stream.of(5, 1, 7, 2).filter(n -> { System.out.print(n + " "); return n > 4; }).findFirst();  ?
     *   4. ZNAJDŹ BŁĄD:  List<Integer> small = Stream.iterate(1, n -> n + 1).filter(n -> n < 5).toList();
     *   5. Czym różni się orElse(loadFromDb()) od orElseGet(() -> loadFromDb())? Kiedy to ma znaczenie?
     *   6. ZNAJDŹ BŁĄD:  Stream<String> s = words.stream(); long c = s.count(); List<String> l = s.toList();
     *   7. Dlaczego List.of(1, 2, 3).stream().peek(x -> licznik++).count() może nie zmienić licznika?
     *   8. Czym różni się filter(parzyste).limit(3) od limit(3).filter(parzyste)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        List<Integer> powers = List.of(1, 2, 4, 8, 16, 32, 64, 128, 256, 512);
        List<String> invoices = List.of("FV/2026/0001", "FV/2026/0002", "FV/2026/0003");
        List<Integer> collatz6 = List.of(6, 3, 10, 5, 16, 8, 4, 2, 1);

        Check.equal("ćw. 1: potęgi dwójki < 1000", powers, () -> exercise1());
        Check.equal("ćw. 2: 3 numery faktur", invoices, () -> exercise2(3));
        Check.equal("ćw. 2: dwunasty numer", "FV/2026/0012", () -> exercise2(12).get(11));
        Check.equal("ćw. 3: suma do dziesiątki", 38, () -> exercise3(SampleData.numbers()));
        Check.equal("ćw. 3: dziesiątka na starcie", 0, () -> exercise3(List.of(10, 1, 2)));
        Check.equal("ćw. 4: Collatz(6)", collatz6, () -> exercise4(6));
        Check.equal("ćw. 4: Collatz(1)", List.of(1), () -> exercise4(1));
        Check.equal("ćw. 4: długość Collatz(27)", 112, () -> exercise4(27).size());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", powers, () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", invoices, () -> solution2(3));
        Check.equal("ćw. 2 dwunasty (wzorzec)", "FV/2026/0012", () -> solution2(12).get(11));
        Check.equal("ćw. 3 (wzorzec)", 38, () -> solution3(SampleData.numbers()));
        Check.equal("ćw. 3 dziesiątka na starcie (wzorzec)", 0, () -> solution3(List.of(10, 1, 2)));
        Check.equal("ćw. 4 Collatz(6) (wzorzec)", collatz6, () -> solution4(6));
        Check.equal("ćw. 4 Collatz(1) (wzorzec)", List.of(1), () -> solution4(1));
        Check.equal("ćw. 4 długość Collatz(27) (wzorzec)", 112, () -> solution4(27).size());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 8 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć wszystkie potęgi dwójki mniejsze niż 1000, używając
     * {@code Stream.iterate} z TRZEMA argumentami (Java 9+) — bez limit i bez takeWhile.
     * Podpowiedź: start 1, warunek „mniejsze niż 1000”, krok „razy 2”.
     */
    static List<Integer> exercise1() {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (średnie): z NIESKOŃCZONEGO strumienia kolejnych liczb (1, 2, 3...) zrób {@code count}
     * pierwszych numerów faktur w formacie "FV/2026/0001" (4 cyfry z zerami z przodu).
     * Podpowiedź: {@code Stream.iterate(1, n -> n + 1)} → map z {@code String.format(Locale.ROOT, "FV/2026/%04d", n)}
     * → limit(count) → toList().
     */
    static List<String> exercise2(int count) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ pętlę z break na strumień (bez break, bez zmiennych zewnętrznych).
     * <pre>{@code
     * int sum = 0;
     * for (int n : numbers) {
     *     if (n == 10) {
     *         break;
     *     }
     *     sum += n;
     * }
     * return sum;
     * }</pre>
     * Podpowiedź: {@code takeWhile(n -> n != 10)} → mapToInt(Integer::intValue) → sum().
     */
    static int exercise3(List<Integer> numbers) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): ciąg Collatza od liczby {@code start}: parzystą dziel przez 2,
     * nieparzystą zamień na 3n + 1; kończ na 1 (jedynka MA być w wyniku). 6 → [6, 3, 10, 5, 16, 8, 4, 2, 1].
     * Podpowiedź: {@code Stream.iterate(start, n -> n != 1, n -> ...)} zatrzyma się PRZED jedynką —
     * dołącz ją przez {@code Stream.concat(..., Stream.of(1))}. Dla start = 1 wynik to [1].
     */
    static List<Integer> exercise4(int start) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<Integer> solution1() {
        return Stream.iterate(1, n -> n < 1000, n -> n * 2).toList();
    }

    static List<String> solution2(int count) {
        return Stream.iterate(1, n -> n + 1)
                .map(n -> String.format(Locale.ROOT, "FV/2026/%04d", n))
                .limit(count)
                .toList();
    }

    static int solution3(List<Integer> numbers) {
        return numbers.stream()
                .takeWhile(n -> n != 10)
                .mapToInt(Integer::intValue)
                .sum();
    }

    static List<Integer> solution4(int start) {
        Stream<Integer> beforeOne = Stream.iterate(start, n -> n != 1, n -> n % 2 == 0 ? n / 2 : 3 * n + 1);
        return Stream.concat(beforeOne, Stream.of(1)).toList();   // concat = połącz dwa strumienie
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Nic — brak operacji końcowej, więc peek ani map się nie wykonają.
     *   2. bacABC  — sorted to bariera: najpierw peek widzi wszystkie (b, a, c), potem forEach
     *      dostaje posortowane (A, B, C).
     *   3. 5   — pierwszy element spełnia warunek, findFirst kończy; 1, 7, 2 nie są nawet sprawdzane.
     *   4. Program się zawiesza: iterate jest nieskończony, a filter nie wie, że większe liczby nie przejdą.
     *      Poprawnie: .takeWhile(n -> n < 5) albo Stream.iterate(1, n -> n < 5, n -> n + 1).
     *   5. orElse(loadFromDb()) wywołuje loadFromDb() ZAWSZE (argument liczony przed wywołaniem),
     *      orElseGet tylko, gdy Optional jest pusty. Ma znaczenie przy drogich lub mających efekty ubocznych
     *      wartościach domyślnych (baza, plik, sieć, new dużego obiektu).
     *   6. s.toList() na zużytym strumieniu → IllegalStateException. Utwórz nowy strumień (words.stream())
     *      albo użyj Supplier<Stream<String>>.
     *   7. Od Javy 9 count() na źródle o znanym rozmiarze (bez filter/flatMap) zwraca rozmiar bez
     *      przechodzenia potoku — peek w ogóle się nie wykona.
     *   8. filter→limit: 3 pierwsze PARZYSTE z całej listy ([8, 2, 10]). limit→filter: parzyste spośród
     *      3 PIERWSZYCH elementów ([8]). Kolejność operacji zmienia znaczenie.
     */
    // </editor-fold>
}
