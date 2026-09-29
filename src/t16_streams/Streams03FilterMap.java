package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Customer;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: filter i map — dwie najczęściej używane operacje na streamach
 *        (filter = filtruj, odsiej; map = przekształć każdy element)
 *
 * W SKRÓCIE:
 *   filter(warunek) zostawia tylko elementy, dla których warunek zwraca true — liczba elementów może się ZMNIEJSZYĆ,
 *   ale ich typ się nie zmienia. map(funkcja) zamienia KAŻDY element na inny — liczba elementów zostaje ta sama,
 *   ale typ może się zmienić (np. Product → String).
 *
 * ANALOGIA:
 *   filter = sito: przepuszcza tylko to, co pasuje (np. tylko jabłka bez plamek).
 *   map = maszyna, która każdą sztukę zamienia na coś innego (jabłko → sok z jabłka). Ile wejdzie, tyle wyjdzie.
 *
 * JAK TO DZIAŁA:
 *   filter przyjmuje {@code Predicate<T>} — funkcję T → boolean (predykat = warunek, „czy spełnia?”).
 *   map przyjmuje {@code Function<T, R>} — funkcję T → R (z elementu typu T robi element typu R).
 *
 *     {@code Stream<Product> ─ filter(Product::inStock) ─▶ Stream<Product>}  (mniej elementów, ten sam typ)
 *     {@code Stream<Product> ─ map(Product::name) ───────▶ Stream<String>}   (tyle samo elementów, inny typ)
 *
 * SŁÓWKA:
 *   filter = filtruj; map = przekształć (mapuj); predicate = predykat (warunek); function = funkcja;
 *   test = sprawdź (metoda predykatu); apply = zastosuj (metoda funkcji); and / or / negate / not = i / lub / zaprzecz / nie;
 *   mapToInt / mapToDouble = przekształć na int / na double; mapToObj = przekształć na obiekt; peek = podejrzyj;
 *   nonNull = nie jest null; cheap = tani; first name = imię; low stock = mały zapas.
 *
 * ZOBACZ TEŻ: Streams04FlatMap (gdy jeden element ma dać WIELE elementów), t13_lambdas/Lambda03JavaUtilFunction
 *             (Predicate, Function i inne interfejsy funkcyjne), t13_lambdas/Lambda05Composition (and/or/negate).
 * </pre>
 */
public class Streams03FilterMap {

    /** HUNDRED = sto. Stała BigDecimal tworzona RAZ — nie w każdej lambdzie od nowa. */
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public static void main(String[] args) {
        title("Streams03 — filter i map");

        filterBasics();          // filter basics = podstawy filtrowania
        predicatesAsVariables(); // predicates as variables = predykaty jako zmienne
        mapBasics();             // map basics = podstawy przekształcania
        mapChaining();           // map chaining = łańcuch przekształceń
        mapToPrimitives();       // map to primitives = przekształcanie na typy proste
        orderMatters();          // order matters = kolejność ma znaczenie
        peekForDebugging();      // peek for debugging = peek do debugowania
        nullsInStreams();        // nulls in streams = null w strumieniach
        realWorldExamples();     // real world examples = przykłady z życia
        exercises();
    }

    // =================================================================================================
    // 1. FILTER — PODSTAWY
    // =================================================================================================

    /**
     * 1. filter zostawia elementy, dla których lambda zwraca true. Lambda w filter MUSI zwracać boolean.
     */
    static void filterBasics() {
        section("1. filter — zostaw tylko pasujące");

        // numbers() = [5, 3, 8, 1, 9, 2, 7, 3, 10, 6, 4, 8]
        List<Integer> even = SampleData.numbers().stream()
                .filter(n -> n % 2 == 0)             // % = reszta z dzielenia; liczba parzysta ma resztę 0
                .toList();
        show("parzyste", even);
        // WYNIK: parzyste → [8, 2, 10, 6, 4, 8]    ← kolejność jak w źródle, duplikaty zostają

        long available = SampleData.products().stream()
                .filter(Product::inStock)            // referencja do metody zwracającej boolean — idealna do filter
                .count();
        show("produkty na stanie", available);
        // WYNIK: produkty na stanie → 12

        // Kilka warunków: jeden filter z && albo kilka filtrów po kolei — wynik ten sam.
        // compareTo = porównaj z: wynik < 0 znaczy „mniejsze”, 0 „równe”, > 0 „większe”.
        List<String> cheapBooks1 = SampleData.products().stream()
                .filter(p -> p.category() == Category.KSIAZKI && p.price().compareTo(HUNDRED) < 0)
                .map(Product::name)
                .toList();
        List<String> cheapBooks2 = SampleData.products().stream()
                .filter(p -> p.category() == Category.KSIAZKI)
                .filter(p -> p.price().compareTo(HUNDRED) < 0)
                .map(Product::name)
                .toList();
        show("tanie książki (&&)", cheapBooks1);
        show("tanie książki (2× filter)", cheapBooks2);
        // WYNIK: tanie książki (&&) → [Czysty kod, Wzorce projektowe]
        // WYNIK: tanie książki (2× filter) → [Czysty kod, Wzorce projektowe]

        // DOBRA PRAKTYKA: kilka krótkich filtrów czyta się łatwiej niż jeden długi warunek z wieloma &&.
        //   Wydajność jest praktycznie taka sama (dzięki przetwarzaniu pionowemu — Streams01Intro, sekcja 6).

        // PUŁAPKA: BigDecimal porównujemy przez compareTo, NIE przez < > ani equals.
        //   a.compareTo(b) < 0 → a < b;   a.compareTo(b) == 0 → a == b (wartością);   a.compareTo(b) > 0 → a > b.
        //   equals porównuje też skalę: new BigDecimal("2.0").equals(new BigDecimal("2.00")) → false! (t15_numbers)
    }

    // =================================================================================================
    // 2. PREDYKATY JAKO ZMIENNE
    // =================================================================================================

    /**
     * 2. Warunek (Predicate) można zapisać w zmiennej, nazwać i wielokrotnie używać.
     * Predykaty łączy się metodami: and (i), or (lub), negate (zaprzeczenie), a {@code Predicate.not(...)}
     * (Java 11+) zaprzecza referencję do metody.
     */
    static void predicatesAsVariables() {
        section("2. Predykaty jako zmienne i ich łączenie (and / or / negate / not)");

        // Predicate<Product> = „warunek dla produktu”. W środku ma metodę test(Product) → boolean.
        Predicate<Product> isCheap = p -> p.price().compareTo(HUNDRED) < 0;     // is cheap = czy tani
        Predicate<Product> isHome = p -> p.category() == Category.DOM;           // is home = czy z kategorii Dom

        show("isCheap.test(pierwszy produkt)", isCheap.test(SampleData.products().get(0)));   // test = sprawdź
        // WYNIK: isCheap.test(pierwszy produkt) → false    ← Laptop za 5499.99 zł

        show("tanie", names(SampleData.products().stream().filter(isCheap)));
        // WYNIK: tanie → [Kawa ziarnista 1kg, Czekolada gorzka, Oliwa z oliwek, Czysty kod, Wzorce projektowe, T-shirt bawełniany]

        show("tanie I dostępne", names(SampleData.products().stream().filter(isCheap.and(Product::inStock))));
        // WYNIK: tanie I dostępne → [Kawa ziarnista 1kg, Czekolada gorzka, Czysty kod, Wzorce projektowe, T-shirt bawełniany]

        show("tanie LUB z kategorii Dom (sztuk)", SampleData.products().stream().filter(isCheap.or(isHome)).count());
        // WYNIK: tanie LUB z kategorii Dom (sztuk) → 8

        show("NIE tanie — negate (sztuk)", SampleData.products().stream().filter(isCheap.negate()).count());
        // WYNIK: NIE tanie — negate (sztuk) → 8

        // Predicate.not (Java 11+) — zaprzeczenie referencji do metody (bo  !Product::inStock  się NIE skompiluje)
        show("niedostępne (Predicate.not)", names(SampleData.products().stream().filter(Predicate.not(Product::inStock))));
        // WYNIK: niedostępne (Predicate.not) → [Smartfon X, Oliwa z oliwek]

        // Klasyczne użycie Predicate.not: wyrzuć puste napisy (isBlank, Java 11+ = czy pusty albo same spacje)
        List<String> raw = List.of("a", " ", "", "b");
        show("bez pustych", raw.stream().filter(Predicate.not(String::isBlank)).toList());
        // WYNIK: bez pustych → [a, b]

        // PUŁAPKA: kolejność łączenia ma znaczenie, bo metody wykonują się od lewej:
        //   isCheap.or(isHome).and(Product::inStock)  =  (tani LUB dom) I dostępny
        //   isCheap.or(isHome.and(Product::inStock))  =  tani LUB (dom I dostępny)
        // DOBRA PRAKTYKA: nazwany predykat (isCheap, isHome) dokumentuje sam siebie — kod czyta się jak zdanie:
        //   filter(isCheap.and(Product::inStock))  =  „odfiltruj tanie i dostępne”.
    }

    /** names = nazwy. Pomocnicza metoda: zamienia strumień produktów na listę ich nazw (żeby nie powtarzać kodu). */
    private static List<String> names(Stream<Product> productStream) {
        return productStream.map(Product::name).toList();
    }

    // =================================================================================================
    // 3. MAP — PODSTAWY
    // =================================================================================================

    /**
     * 3. map zamienia KAŻDY element na nowy. Liczba elementów się nie zmienia, typ — może.
     */
    static void mapBasics() {
        section("3. map — przekształć każdy element");

        // String → Integer (długość słowa)
        show("długości słów", Stream.of("java", "stream", "lambda").map(String::length).toList());
        // WYNIK: długości słów → [4, 6, 6]

        // Product → String (opis)
        List<String> descriptions = SampleData.products().stream()
                .filter(p -> p.category() == Category.KSIAZKI)
                .map(p -> p.name() + " — " + p.price() + " zł")
                .toList();
        show("opisy książek", descriptions);
        // WYNIK: opisy książek → [Czysty kod — 79.00 zł, Java. Podstawy — 129.00 zł, Wzorce projektowe — 99.00 zł]

        // Product → BigDecimal (cena brutto = netto × 1.23, zaokrąglona do groszy)
        // multiply = pomnóż; setScale(2, RoundingMode.HALF_UP) = ustaw 2 miejsca po przecinku, zaokrąglając
        // „szkolnie” (od 5 w górę). Szczegóły: t15_numbers.
        BigDecimal vat = new BigDecimal("1.23");
        List<BigDecimal> gross = SampleData.products().stream()
                .filter(p -> p.category() == Category.DOM)
                .map(Product::price)                                              // Product → BigDecimal (netto)
                .map(net -> net.multiply(vat).setScale(2, RoundingMode.HALF_UP))  // BigDecimal → BigDecimal (brutto)
                .toList();
        show("ceny brutto (Dom)", gross);
        // WYNIK: ceny brutto (Dom) → [2335.77, 158.67]

        // PUŁAPKA: map NIE służy do „zrób coś z elementem” (np. wypisz, zapisz do bazy). Do tego jest forEach.
        //   map ma ZWRÓCIĆ nowy element. Jeśli lambda w map nic nie zwraca (void) — kod się nie skompiluje.
    }

    // =================================================================================================
    // 4. ŁAŃCUCH MAP
    // =================================================================================================

    /**
     * 4. Kilka wywołań map pod rząd — każde robi jeden mały krok. Czytelniej niż jedna duża lambda.
     */
    static void mapChaining() {
        section("4. Łańcuch map — kilka małych kroków");

        List<String> firstNames = SampleData.employees().stream()
                .map(Employee::name)                 // Employee → "Anna Nowak"
                .map(name -> name.split(" ")[0])     // "Anna Nowak" → "Anna"   (split = podziel; [0] = pierwszy kawałek)
                .map(String::toUpperCase)            // "Anna" → "ANNA"
                .toList();
        show("imiona", firstNames);
        // WYNIK: imiona → [ANNA, PIOTR, KATARZYNA, TOMASZ, MAGDALENA, MICHAŁ, AGNIESZKA, KRZYSZTOF, EWA, PAWEŁ]

        // DOBRA PRAKTYKA: jedna operacja = jedna myśl. Łatwiej czytać, łatwiej zmienić jeden krok,
        //   a wydajność jest praktycznie taka sama jak przy jednej wielkiej lambdzie.
    }

    // =================================================================================================
    // 5. MAP NA TYPY PROSTE: mapToInt / mapToDouble / mapToObj
    // =================================================================================================

    /**
     * 5. mapToInt / mapToLong / mapToDouble zamieniają {@code Stream<T>} na strumień liczb prostych (IntStream...).
     * Taki strumień ma od razu sum(), average(), max(), min() — i nie opakowuje liczb w obiekty (szybciej).
     * mapToObj robi odwrotnie: z IntStream robi {@code Stream<T>}.
     */
    static void mapToPrimitives() {
        section("5. mapToInt / mapToDouble / mapToObj — obiekty ↔ liczby proste");

        int totalStock = SampleData.products().stream()
                .mapToInt(Product::stock)            // Stream<Product> → IntStream
                .sum();                              // sum = suma (istnieje tylko na strumieniach liczbowych!)
        show("łączny stan magazynu", totalStock + " szt.");
        // WYNIK: łączny stan magazynu → 595 szt.

        show("średnia pensja", SampleData.employees().stream().mapToInt(Employee::salary).average());
        show("najwyższa pensja", SampleData.employees().stream().mapToInt(Employee::salary).max());
        // WYNIK: średnia pensja → OptionalDouble[10640.0]
        // WYNIK: najwyższa pensja → OptionalInt[17200]

        // mapToDouble — np. cena jako double (doubleValue = wartość jako double):
        show("najwyższa cena (double)", SampleData.products().stream().mapToDouble(p -> p.price().doubleValue()).max());
        // WYNIK: najwyższa cena (double) → OptionalDouble[5499.99]
        // PUŁAPKA: double to PRZYBLIŻENIE. Do porównań „na oko” wystarczy, ale sum i średnich kwot pieniędzy
        //   NIE licz na double — tylko na BigDecimal (Streams15BigDecimalMoney, t15_numbers).

        // Odwrotnie: liczby → obiekty
        show("numery pokoi", IntStream.rangeClosed(1, 3).mapToObj(i -> "Pokój " + i).toList());
        // WYNIK: numery pokoi → [Pokój 1, Pokój 2, Pokój 3]

        // PUŁAPKA: zwykły Stream<Integer> NIE ma metody sum()!  list.stream().sum()  się nie skompiluje.
        //   Najpierw mapToInt(Integer::intValue), potem sum(). Więcej: Streams08PrimitiveStreams.
    }

    // =================================================================================================
    // 6. KOLEJNOŚĆ OPERACJI MA ZNACZENIE (DLA WYDAJNOŚCI)
    // =================================================================================================

    /**
     * 6. Najpierw filtruj, potem przekształcaj — wtedy map działa na mniejszej liczbie elementów.
     * <p>
     * Wywołania map liczymy licznikiem AtomicInteger (atomowa liczba całkowita — obiekt-licznik).
     * Dlaczego nie zwykły int? Lambda może używać tylko zmiennych lokalnych, których NIGDY nie zmieniamy
     * („effectively final” — t13_lambdas/Lambda06ClosuresScope). Licznik int++ byłby zmianą → błąd kompilacji.
     * AtomicInteger to obiekt: zmienna wskazuje ciągle na ten sam obiekt, zmienia się tylko jego zawartość.
     * UWAGA: zmienianie zewnętrznego licznika z wnętrza streamu to efekt uboczny — tu wyłącznie do demonstracji!
     */
    static void orderMatters() {
        section("6. Kolejność ma znaczenie: filter przed map");

        AtomicInteger mapCalls1 = new AtomicInteger();
        List<String> result1 = SampleData.words().stream()
                .map(w -> {
                    mapCalls1.incrementAndGet();         // incrementAndGet = zwiększ o 1 i zwróć
                    return w.toUpperCase();
                })
                .filter(w -> w.length() > 5)
                .toList();

        AtomicInteger mapCalls2 = new AtomicInteger();
        List<String> result2 = SampleData.words().stream()
                .filter(w -> w.length() > 5)
                .map(w -> {
                    mapCalls2.incrementAndGet();
                    return w.toUpperCase();
                })
                .toList();

        show("map → filter: wywołań map", mapCalls1.get());   // get = pobierz (aktualną wartość licznika)
        show("filter → map: wywołań map", mapCalls2.get());
        show("wyniki takie same?", result1.equals(result2) + " " + result2);
        // WYNIK: map → filter: wywołań map → 12
        // WYNIK: filter → map: wywołań map → 6
        // WYNIK: wyniki takie same? → true [STREAM, LAMBDA, KOLEKCJA, STREAM, OPTIONAL, REKORD]

        // DOBRA PRAKTYKA: filtruj jak najwcześniej — mniej elementów = mniej pracy w dalszych krokach.
        //   (Czasem trzeba najpierw przekształcić, bo warunek dotyczy wyniku map — wtedy kolejność wynika z logiki.)
    }

    // =================================================================================================
    // 7. PEEK — PODGLĄD DO DEBUGOWANIA
    // =================================================================================================

    /**
     * 7. peek(akcja) wykonuje akcję dla każdego elementu i przepuszcza go dalej BEZ zmian.
     * Służy do „podglądania”, co przepływa przez potok (debugowanie). Nie używaj go do logiki programu!
     */
    static void peekForDebugging() {
        section("7. peek — podejrzyj elementy w trakcie");

        List<Integer> result = Stream.of(1, 2, 3, 4)
                .peek(n -> System.out.println("   przed filtrem: " + n))
                .filter(n -> n % 2 == 0)
                .peek(n -> System.out.println("   po filtrze:    " + n))
                .toList();
        show("wynik", result);
        // WYNIK: przed filtrem: 1
        // WYNIK: przed filtrem: 2
        // WYNIK: po filtrze:    2
        // WYNIK: przed filtrem: 3
        // WYNIK: przed filtrem: 4
        // WYNIK: po filtrze:    4
        // WYNIK: wynik → [2, 4]

        // PUŁAPKA: peek może się w ogóle NIE wykonać! Od Javy 9 count() na strumieniu o ZNANYM rozmiarze
        //   nie przechodzi po elementach — po prostu zwraca rozmiar. Rozmiar jest znany, gdy źródło go zna
        //   (lista, tablica), a po drodze są tylko operacje, które go nie zmieniają (peek, map, sorted).
        //   Dopiero filter (albo np. flatMap) sprawia, że rozmiar przestaje być znany.
        AtomicInteger peekCalls = new AtomicInteger();
        long count = List.of("a", "b", "c").stream()
                .peek(s -> peekCalls.incrementAndGet())
                .map(String::toUpperCase)
                .count();
        show("count", count);
        show("ile razy wykonał się peek", peekCalls.get());
        // WYNIK: count → 3
        // WYNIK: ile razy wykonał się peek → 0    ← peek (i map!) zostały pominięte
        // Wniosek: nigdy nie opieraj logiki programu na peek (ani na innych efektach ubocznych w streamach).
    }

    // =================================================================================================
    // 8. NULL W STREAMACH
    // =================================================================================================

    /**
     * 8. Jeśli map zwróci null, następna operacja na tym elemencie może rzucić NullPointerException.
     * Objects = klasa narzędziowa do obiektów; Objects::nonNull = „czy nie jest null”.
     */
    static void nullsInStreams() {
        section("8. null w streamach — Objects::nonNull");

        // Dwóch klientów nie ma e-maila (null)
        List<String> emails = SampleData.customers().stream()
                .map(Customer::email)
                .toList();
        show("e-maile (z null)", emails);
        // WYNIK: e-maile (z null) → [jan@example.com, null, adam.mazur@example.com, zofia@example.com, null, marek@example.com, ewa.lis@example.com]

        expectThrows("toUpperCase na null (referencja do metody)", () -> SampleData.customers().stream()
                .map(Customer::email)
                .map(String::toUpperCase)            // dla null → NullPointerException
                .toList());
        // WYNIK: ✔ toUpperCase na null (referencja do metody) → rzucono NullPointerException: (brak komunikatu)

        expectThrows("toUpperCase na null (lambda)", () -> SampleData.customers().stream()
                .map(Customer::email)
                .map(email -> email.toUpperCase())
                .toList());
        // WYNIK: ✔ toUpperCase na null (lambda) → rzucono NullPointerException: Cannot invoke "String.toUpperCase()" because "email" is null
        // Od Javy 14 NullPointerException ma „pomocny komunikat” (helpful NPE) — mówi, CO było null.
        // Przy referencji do metody (String::toUpperCase) takiego komunikatu brak — to jeden z powodów, żeby przy
        // debugowaniu NPE tymczasowo zamienić referencję na zwykłą lambdę.

        List<String> safe = SampleData.customers().stream()
                .map(Customer::email)
                .filter(Objects::nonNull)            // wyrzuć null-e PRZED dalszymi krokami
                .map(String::toUpperCase)
                .toList();
        show("e-maile bez null", safe);
        // WYNIK: e-maile bez null → [JAN@EXAMPLE.COM, ADAM.MAZUR@EXAMPLE.COM, ZOFIA@EXAMPLE.COM, MAREK@EXAMPLE.COM, EWA.LIS@EXAMPLE.COM]

        // DOBRA PRAKTYKA: jeszcze lepiej, gdy model w ogóle nie zwraca null, tylko Optional:
        //   customers.stream().map(Customer::findEmail).flatMap(Optional::stream)...   (Streams14OptionalInStreams)
    }

    // =================================================================================================
    // 9. PRZYKŁADY Z ŻYCIA
    // =================================================================================================

    /**
     * 9. Typowe zadania biznesowe rozwiązane przez filter + map.
     */
    static void realWorldExamples() {
        section("9. Przykłady z życia");

        // „Lista płac: kto z IT zarabia powyżej 10 000 zł?”
        List<String> itHighEarners = SampleData.employees().stream()
                .filter(e -> e.department() == Department.IT)
                .filter(e -> e.salary() > 10_000)                // 10_000 = 10000 (podkreślenie tylko dla czytelności)
                .map(Employee::name)
                .toList();
        show("IT powyżej 10 000 zł", itHighEarners);
        // WYNIK: IT powyżej 10 000 zł → [Anna Nowak, Michał Lewandowski, Ewa Woźniak]

        // „Do zamówienia u dostawcy: produkty z małym zapasem (1–5 szt.) — z kodem SKU i stanem”
        List<String> lowStock = SampleData.products().stream()
                .filter(p -> p.stock() > 0 && p.stock() <= 5)
                .map(p -> p.sku() + " (" + p.stock() + " szt.)")
                .toList();
        show("mały zapas", lowStock);
        // WYNIK: mały zapas → [ELE-004 (4 szt.), KSI-003 (3 szt.), DOM-001 (2 szt.)]

        // „Adresy do newslettera: e-maile klientów VIP”
        List<String> vipEmails = SampleData.customers().stream()
                .filter(Customer::vip)
                .map(Customer::email)
                .filter(Objects::nonNull)
                .toList();
        show("e-maile VIP", vipEmails);
        // WYNIK: e-maile VIP → [jan@example.com, zofia@example.com]
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   filter(p -> warunek)          — zostaw pasujące; typ bez zmian, elementów ≤
     *   map(x -> nowy)                — zamień każdy; elementów tyle samo, typ może się zmienić
     *   mapToInt / mapToDouble        — na IntStream/DoubleStream → sum(), average(), max()
     *   mapToObj                      — z IntStream z powrotem na Stream<T>
     *   Predicate: and / or / negate  — łączenie warunków (od lewej!);  Predicate.not(Klasa::metoda) — Java 11+
     *   filter(Objects::nonNull)      — wyrzuć null-e, zanim wywołasz na nich metodę
     *   Kolejność: najpierw filter, potem map (mniej pracy).
     *   peek — tylko do podglądu/debugowania; może się nie wykonać (np. przy count() na znanym rozmiarze).
     *   BigDecimal porównuj przez compareTo, nie equals; stałe BigDecimal twórz raz, poza lambdą.
     *
     * PYTANIA KONTROLNE:
     *   1. Co przyjmuje filter, a co map (jaki interfejs funkcyjny, co zwraca lambda)?
     *   2. Która operacja może zmienić liczbę elementów, a która ich typ?
     *   3. Co wypisze:  System.out.println(Stream.of(1, 2, 3).peek(System.out::print).count());  ?
     *   4. Co wypisze:  System.out.println(Stream.of(1, 2, 3).filter(n -> n > 1).peek(System.out::print).count());  ?
     *   5. ZNAJDŹ BŁĄD:  products.stream().map(p -> System.out.println(p.name())).toList();
     *   6. Czym różni się  isCheap.or(isHome).and(inStock)  od  isCheap.or(isHome.and(inStock))?
     *   7. Jak policzyć sumę pól int z listy obiektów streamem?
     *   8. Jak uniknąć NullPointerException, gdy map może zwrócić null?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        List<String> expected4 = List.of("ADAM.MAZUR@EXAMPLE.COM", "MAREK@EXAMPLE.COM", "EWA.LIS@EXAMPLE.COM");
        List<String> expected5 = List.of("Kawa ziarnista 1kg", "Czekolada gorzka", "Czysty kod", "Java. Podstawy",
                "Wzorce projektowe", "T-shirt bawełniany");

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: produkty z kategorii ODZIEZ", List.of("Kurtka zimowa", "T-shirt bawełniany"),
                () -> exercise1(SampleData.products()));
        Check.equal("ćw. 2: kwadraty liczb nieparzystych", List.of(25, 9, 1, 81, 49, 9),
                () -> exercise2(SampleData.numbers()));
        Check.equal("ćw. 3: łączny wiek pracowników działu MARKETING", 65, () -> exercise3(SampleData.employees()));
        Check.equal("ćw. 4: pętla → stream (e-maile spoza Warszawy)", expected4, () -> exercise4(SampleData.customers()));
        Check.equal("ćw. 5: (tanie LUB książki) I dostępne", expected5, () -> exercise5(SampleData.products()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("Kurtka zimowa", "T-shirt bawełniany"), () -> solution1(SampleData.products()));
        Check.equal("ćw. 2 (wzorzec)", List.of(25, 9, 1, 81, 49, 9), () -> solution2(SampleData.numbers()));
        Check.equal("ćw. 3 (wzorzec)", 65, () -> solution3(SampleData.employees()));
        Check.equal("ćw. 4 (wzorzec)", expected4, () -> solution4(SampleData.customers()));
        Check.equal("ćw. 5 (wzorzec)", expected5, () -> solution5(SampleData.products()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć nazwy produktów z kategorii ODZIEZ (Category.ODZIEZ).
     * Podpowiedź: filter z porównaniem kategorii przez ==, potem map(Product::name).
     */
    static List<String> exercise1(List<Product> products) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć kwadraty liczb NIEparzystych, w kolejności ze źródła (duplikaty zostają).
     * Podpowiedź: liczba nieparzysta ma {@code n % 2 != 0}; kwadrat to {@code n * n}.
     */
    static List<Integer> exercise2(List<Integer> numbers) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć łączny wiek (suma pól age) pracowników działu MARKETING.
     * Podpowiedź: filter → mapToInt(Employee::age) → sum().
     */
    static int exercise3(List<Employee> employees) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (średnie): PRZEPISZ PĘTLĘ NA STREAM.
     * <pre>{@code
     *   List<String> result = new ArrayList<>();
     *   for (Customer c : customers) {
     *       if (!c.city().equals("Warszawa") && c.email() != null) {
     *           result.add(c.email().toUpperCase());
     *       }
     *   }
     *   return result;
     * }</pre>
     * Podpowiedź: dwa filtry (miasto, potem Objects::nonNull na e-mailu — najpierw trzeba zamienić klienta na e-mail!),
     * potem map(String::toUpperCase).
     */
    static List<String> exercise4(List<Customer> customers) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): zwróć nazwy produktów, które są (TANIE — cena poniżej 100 zł — LUB są KSIĄŻKAMI)
     * I jednocześnie są DOSTĘPNE (na stanie). Kolejność jak w źródle.
     * Podpowiedź: zbuduj dwa nazwane predykaty (isCheap, isBook) i połącz je or/and.
     * Uważaj na kolejność łączenia (sekcja 2, PUŁAPKA) i porównuj ceny przez compareTo.
     */
    static List<String> exercise5(List<Product> products) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(List<Product> products) {
        return products.stream()
                .filter(p -> p.category() == Category.ODZIEZ)
                .map(Product::name)
                .toList();
    }

    static List<Integer> solution2(List<Integer> numbers) {
        return numbers.stream()
                .filter(n -> n % 2 != 0)             // != 0 zamiast == 1 — działa też dla ujemnych (-3 % 2 == -1!)
                .map(n -> n * n)
                .toList();
    }

    static int solution3(List<Employee> employees) {
        return employees.stream()
                .filter(e -> e.department() == Department.MARKETING)
                .mapToInt(Employee::age)
                .sum();
    }

    static List<String> solution4(List<Customer> customers) {
        return customers.stream()
                .filter(c -> !c.city().equals("Warszawa"))
                .map(Customer::email)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .toList();
    }

    static List<String> solution5(List<Product> products) {
        Predicate<Product> isCheap = p -> p.price().compareTo(HUNDRED) < 0;
        Predicate<Product> isBook = p -> p.category() == Category.KSIAZKI;
        return products.stream()
                .filter(isCheap.or(isBook).and(Product::inStock))   // (tani LUB książka) I dostępny
                .map(Product::name)
                .toList();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. filter przyjmuje Predicate<T> (lambda zwraca boolean); map przyjmuje Function<T, R> (lambda zwraca nowy element).
     *   2. filter może zmienić LICZBĘ elementów (zmniejszyć); map może zmienić TYP elementów (liczba bez zmian).
     *   3. Tylko „3” — rozmiar strumienia z Stream.of jest znany, więc count() pomija peek (Java 9+).
     *   4. „232” — filter sprawia, że rozmiar nie jest znany, więc elementy naprawdę przechodzą przez peek:
     *      peek wypisuje „2” i „3” (print — bez nowej linii), a potem println dopisuje wynik count() = 2.
     *   5. println zwraca void (nic), a map musi ZWRÓCIĆ nowy element → błąd kompilacji. Do wypisywania: forEach.
     *   6. Pierwsze: (tani LUB dom) I dostępny. Drugie: tani LUB (dom I dostępny) — tani niedostępny też przejdzie.
     *   7. lista.stream().mapToInt(Klasa::metoda).sum(), np. employees.stream().mapToInt(Employee::salary).sum()
     *      (:: wskazuje METODĘ — tu akcesor rekordu salary(), a nie pole).
     *   8. filter(Objects::nonNull) przed operacjami na elementach (albo zwracać Optional zamiast null).
     */
    // </editor-fold>
}
