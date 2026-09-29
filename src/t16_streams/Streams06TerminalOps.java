package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Order;
import helpers.model.Product;
import helpers.model.Student;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Operacje końcowe (terminal operations) — co można „wyjąć” ze strumienia
 *        (terminal = końcowy; forEach = dla każdego; count = policz; find = znajdź; match = pasuje)
 *
 * W SKRÓCIE:
 *   Operacja końcowa uruchamia cały potok i daje WYNIK: liczbę, Optional, boolean, tablicę, listę
 *   albo efekt uboczny (np. wypisanie). Po niej strumień jest ZUŻYTY — drugi raz go nie użyjesz.
 *
 * ANALOGIA: Linia produkcyjna w fabryce. Operacje pośrednie (filter, map, sorted) to stanowiska na taśmie.
 *   Operacja końcowa to wyłącznik „START” + to, co robisz z wynikiem: pakujesz do kartonu (toList),
 *   liczysz sztuki (count), szukasz pierwszej wadliwej (findFirst) albo sprawdzasz, czy wszystkie są OK
 *   (allMatch). Bez wciśnięcia START taśma stoi.
 *
 * JAK TO DZIAŁA:
 *   operacja końcowa            │ zwraca                     │ krótkie spięcie*
 *   ────────────────────────────┼────────────────────────────┼─────────────────
 *   forEach / forEachOrdered    │ nic (void)                 │ nie
 *   count                       │ long                       │ nie
 *   min / max (komparator)      │ Optional (pusty, gdy brak) │ nie
 *   findFirst / findAny         │ Optional                   │ TAK
 *   anyMatch / allMatch / noneMatch │ boolean                │ TAK
 *   toArray                     │ tablica                    │ nie
 *   toList (Java 16+)           │ lista niemodyfikowalna     │ nie
 *   collect / reduce            │ dowolny wynik              │ nie   (osobne lekcje)
 *   iterator                    │ Iterator                   │ pobiera leniwie, na żądanie
 *   * krótkie spięcie (short-circuit) = może skończyć, zanim przejrzy wszystkie elementy.
 *
 * SŁÓWKA:
 *   terminal = końcowy; ordered = uporządkowany; first = pierwszy; any = dowolny; all = wszystkie;
 *   none = żaden; match = pasować; array = tablica; generator = twórca (tu: tablicy o danym rozmiarze);
 *   unmodifiable = niemodyfikowalny; iterator = iterator (wskaźnik „następny element”); short-circuit = krótkie spięcie
 *
 * ZOBACZ TEŻ: t16_streams/Streams01Intro (lenistwo: bez operacji końcowej nic się nie dzieje),
 *   t14_optional/Optional01Basics (co zrobić z Optional z min/max/findFirst),
 *   t12_collections/Collections08ImmutableUnmodifiable (listy niemodyfikowalne),
 *   t16_streams/Streams07Reduce (reduce), t16_streams/Streams09CollectorsBasic (collect)
 * </pre>
 */
public class Streams06TerminalOps {

    /** Stała poza lambdą: 1000 zł jako BigDecimal. */
    private static final BigDecimal THOUSAND = new BigDecimal("1000");

    public static void main(String[] args) {
        title("Streams06 — operacje końcowe");

        terminalBasics();        // terminal basics = podstawy operacji końcowych
        forEachVsOrdered();      // forEach vs forEachOrdered = dla każdego vs dla każdego po kolei
        countDemo();             // count demo = liczenie
        minMaxDemo();            // min/max demo = najmniejszy / największy
        findFirstFindAny();      // findFirst/findAny = znajdź pierwszy / dowolny
        matchDemo();             // match demo = anyMatch / allMatch / noneMatch
        toArrayDemo();           // toArray demo = do tablicy
        toListVariants();        // toList variants = odmiany „do listy”
        iteratorDemo();          // iterator demo = iterator (krótko)
        reduceCollectPreview();  // reduce/collect preview = zapowiedź reduce i collect
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. CO TO JEST OPERACJA KOŃCOWA
    // =================================================================================================

    /**
     * 1. Operacja końcowa uruchamia potok. Jest tylko JEDNA na strumień — potem strumień jest zużyty.
     */
    static void terminalBasics() {
        section("1. Operacja końcowa: uruchamia potok i zużywa strumień");

        // Bez operacji końcowej lambda w filter NIE wykona się ani razu (lenistwo — t16_streams/Streams01Intro).
        Stream<Integer> pipeline = SampleData.numbers().stream()
                .filter(n -> {
                    System.out.println("   filtruję " + n);   // efekt uboczny — tylko do pokazu
                    return n > 8;
                });
        note("potok zbudowany, ale nic się nie wypisało — brak operacji końcowej");
        // WYNIK: ℹ potok zbudowany, ale nic się nie wypisało — brak operacji końcowej

        long bigOnes = pipeline.count();   // count = policz → dopiero TERAZ lambdy ruszają
        // WYNIK: filtruję 5
        // WYNIK: filtruję 3
        // WYNIK: filtruję 8
        // WYNIK: filtruję 1
        // WYNIK: filtruję 9
        // WYNIK: filtruję 2
        // WYNIK: filtruję 7
        // WYNIK: filtruję 3
        // WYNIK: filtruję 10
        // WYNIK: filtruję 6
        // WYNIK: filtruję 4
        // WYNIK: filtruję 8
        show("liczby większe od 8", bigOnes);
        // WYNIK: liczby większe od 8 → 2

        // PUŁAPKA: druga operacja końcowa na tym samym strumieniu → IllegalStateException.
        expectThrows("druga operacja końcowa", () -> pipeline.toList());  // toList (Java 16+) = do listy
        // WYNIK: ✔ druga operacja końcowa → rzucono IllegalStateException: stream has already been operated upon or closed

        // DOBRA PRAKTYKA: nie trzymaj strumienia w zmiennej. Potrzebujesz dwóch wyników? Utwórz strumień dwa razy
        // (list.stream() jest tanie) albo zbierz dane do listy i licz z niej.
    }

    // =================================================================================================
    // 2. forEach vs forEachOrdered
    // =================================================================================================

    /**
     * 2. {@code forEach} wykonuje akcję dla każdego elementu, ale w strumieniu równoległym w DOWOLNEJ kolejności.
     * {@code forEachOrdered} zawsze trzyma kolejność strumienia. Obie służą do efektów ubocznych na końcu potoku.
     */
    static void forEachVsOrdered() {
        section("2. forEach vs forEachOrdered");

        List<Integer> small = List.of(1, 2, 3, 4, 5, 6, 7, 8);  // List.of (Java 9+) = niemodyfikowalna lista

        System.out.print("   forEach (sekwencyjnie):  ");
        small.stream().forEach(n -> System.out.print(n + " "));
        System.out.println();
        // WYNIK: forEach (sekwencyjnie):  1 2 3 4 5 6 7 8

        // parallel = równolegle (kilka wątków naraz). Kolejność wypisania zależy od tego, który wątek będzie pierwszy.
        System.out.print("   forEach (parallel):      ");
        small.parallelStream().forEach(n -> System.out.print(n + " "));  // parallelStream = strumień równoległy
        System.out.println();
        // (wynik zależy od uruchomienia)

        System.out.print("   forEachOrdered (parallel): ");
        small.parallelStream().forEachOrdered(n -> System.out.print(n + " "));
        System.out.println();
        // WYNIK: forEachOrdered (parallel): 1 2 3 4 5 6 7 8

        // PUŁAPKA: antywzorzec — forEach do „zbierania” wyników w zewnętrznej liście.
        // PRZED:
        List<String> collected = new ArrayList<>();
        SampleData.products().stream()
                .filter(p -> p.category() == Category.KSIAZKI)
                .forEach(p -> collected.add(p.name()));   // efekt uboczny; w parallel ArrayList by się posypała
        show("forEach + add (źle)", collected);
        // WYNIK: forEach + add (źle) → [Czysty kod, Java. Podstawy, Wzorce projektowe]
        // PO: niech strumień sam zbuduje listę.
        List<String> books = SampleData.products().stream()
                .filter(p -> p.category() == Category.KSIAZKI)
                .map(Product::name)
                .toList();                                 // toList (Java 16+) = do (niemodyfikowalnej) listy
        show("map + toList (dobrze)", books);
        // WYNIK: map + toList (dobrze) → [Czysty kod, Java. Podstawy, Wzorce projektowe]

        // DOBRA PRAKTYKA: forEach tylko na sam koniec do „wyjścia na zewnątrz” (wypisz, zapisz, wyślij).
        // Na liście wystarczy list.forEach(...) — bez .stream().
    }

    // =================================================================================================
    // 3. count — ZAWSZE long
    // =================================================================================================

    /**
     * 3. {@code count()} zwraca {@code long}. Od Java 9 może w ogóle NIE przechodzić elementów,
     * jeśli rozmiar jest znany z góry — wtedy peek/map się nie wykonają.
     */
    static void countDemo() {
        section("3. count() — wynik typu long");

        long inStock = SampleData.products().stream().filter(Product::inStock).count();
        show("produkty na stanie", inStock);
        // WYNIK: produkty na stanie → 12

        // PUŁAPKA: int n = stream.count();  → błąd kompilacji (long nie mieści się „bez pytania” w int).
        // Jeśli naprawdę potrzebujesz int: Math.toIntExact (rzuci wyjątek zamiast po cichu obciąć liczbę).
        int asInt = Math.toIntExact(inStock);  // toIntExact = zamień na int dokładnie (albo wyjątek)
        show("jako int", asInt);
        // WYNIK: jako int → 12

        // PUŁAPKA: count() na liście bez filtra zna rozmiar od razu — peek się NIE wykona (Java 9+).
        long noPeek = SampleData.numbers().stream()
                .peek(n -> System.out.println("   peek " + n))   // peek = podejrzyj
                .count();
        show("count() bez filtra", noPeek);
        // WYNIK: count() bez filtra → 12    ← i ani jednej linii „peek”
        // Wniosek: nigdy nie wkładaj ważnej logiki do peek ani map „dla efektu ubocznego”.
    }

    // =================================================================================================
    // 4. min / max Z KOMPARATOREM → Optional
    // =================================================================================================

    /**
     * 4. {@code min(cmp)} i {@code max(cmp)} zwracają {@code Optional}, bo strumień może być pusty.
     */
    static void minMaxDemo() {
        section("4. min / max z komparatorem → Optional");

        List<Product> products = SampleData.products();

        Optional<Product> mostExpensive = products.stream().max(Comparator.comparing(Product::price));
        show("najdroższy", mostExpensive);
        // WYNIK: najdroższy → Optional[Laptop Pro 14 (5499.99 zł)]
        show("najtańszy", products.stream().min(Comparator.comparing(Product::price)));
        // WYNIK: najtańszy → Optional[Czekolada gorzka (7.49 zł)]

        // Pusty strumień → Optional.empty. W dziale LOGISTYKA nie ma pracowników.
        Optional<Employee> topLogistics = SampleData.employees().stream()
                .filter(e -> e.department() == Department.LOGISTYKA)
                .max(Comparator.comparingInt(Employee::salary));
        show("najlepiej zarabiający w LOGISTYKA", topLogistics);
        // WYNIK: najlepiej zarabiający w LOGISTYKA → Optional.empty
        show("...z orElse", topLogistics.map(Employee::name).orElse("brak pracowników")); // orElse = albo (wartość domyślna)
        // WYNIK: ...z orElse → brak pracowników

        // PUŁAPKA: .get() na pustym Optional → NoSuchElementException. Wolisz wyjątek? orElseThrow() (Java 10+) mówi to wprost.
        expectThrows("max(...).get() na pustym", () -> topLogistics.get());
        // WYNIK: ✔ max(...).get() na pustym → rzucono NoSuchElementException: No value present

        // Najstarszy pracownik — max po wieku, od razu zamieniony na tekst.
        String oldest = SampleData.employees().stream()
                .max(Comparator.comparingInt(Employee::age))   // age = wiek
                .map(Employee::name)
                .orElseThrow();                                  // orElseThrow (Java 10+) = albo rzuć wyjątek
        show("najstarszy", oldest);
        // WYNIK: najstarszy → Krzysztof Szymański
        // DOBRA PRAKTYKA: przy możliwym remisie (Magdalena i Michał mają po 45 lat) nie zgaduj, kto „wygra” —
        // dodaj kryterium rozstrzygające: comparingInt(Employee::age).thenComparing(Employee::name)
        // (t16_streams/Streams05SortDistinctLimit).

        // Na IntStream min/max nie potrzebują komparatora i zwracają OptionalInt.
        show("max z IntStream", SampleData.numbers().stream().mapToInt(Integer::intValue).max());
        // WYNIK: max z IntStream → OptionalInt[10]
    }

    // =================================================================================================
    // 5. findFirst / findAny
    // =================================================================================================

    /**
     * 5. {@code findFirst()} zwraca pierwszy element (w kolejności strumienia), {@code findAny()} — dowolny.
     * Obie kończą przy pierwszym trafieniu (krótkie spięcie).
     */
    static void findFirstFindAny() {
        section("5. findFirst / findAny — krótkie spięcie");

        Optional<Product> firstExpensive = SampleData.products().stream()
                .filter(p -> p.price().compareTo(THOUSAND) > 0)
                .findFirst();
        show("pierwszy droższy niż 1000 zł", firstExpensive);
        // WYNIK: pierwszy droższy niż 1000 zł → Optional[Laptop Pro 14 (5499.99 zł)]

        // Krótkie spięcie: patrzymy tylko do pierwszego trafienia.
        Optional<Integer> firstAbove7 = SampleData.numbers().stream()
                .peek(n -> System.out.println("   sprawdzam " + n))
                .filter(n -> n > 7)
                .findFirst();
        // WYNIK: sprawdzam 5
        // WYNIK: sprawdzam 3
        // WYNIK: sprawdzam 8
        show("pierwsza > 7", firstAbove7);
        // WYNIK: pierwsza > 7 → Optional[8]

        // findAny: „daj którykolwiek”. W parallel może zwrócić inny element przy każdym uruchomieniu — za to szybciej.
        Optional<Integer> anyAbove7 = SampleData.numbers().parallelStream().filter(n -> n > 7).findAny();
        show("findAny coś znalazł?", anyAbove7.isPresent());   // isPresent = czy jest wartość
        // WYNIK: findAny coś znalazł? → true
        // Sama wartość: (wynik zależy od uruchomienia) — 8, 9, 10 albo znów 8.

        show("brak trafienia", SampleData.numbers().stream().filter(n -> n > 100).findFirst());
        // WYNIK: brak trafienia → Optional.empty

        // DOBRA PRAKTYKA: kolejność ma znaczenie (np. „pierwsze zamówienie”)? → findFirst.
        // Chcesz tylko wiedzieć, CZY coś jest? → anyMatch (następna sekcja), nie findAny().isPresent().
    }

    // =================================================================================================
    // 6. anyMatch / allMatch / noneMatch — TAKŻE NA PUSTYM STRUMIENIU
    // =================================================================================================

    /**
     * 6. Trzy pytania „tak/nie”: czy KTÓRYŚ pasuje, czy WSZYSTKIE pasują, czy ŻADEN nie pasuje.
     * Na pustym strumieniu: anyMatch = false, allMatch = true, noneMatch = true.
     */
    static void matchDemo() {
        section("6. anyMatch / allMatch / noneMatch");

        List<Product> products = SampleData.products();
        show("anyMatch: jakiś niedostępny?", products.stream().anyMatch(p -> !p.inStock()));
        // WYNIK: anyMatch: jakiś niedostępny? → true
        show("allMatch: wszystkie dostępne?", products.stream().allMatch(Product::inStock));
        // WYNIK: allMatch: wszystkie dostępne? → false
        show("noneMatch: żaden nie ma ceny ≤ 0?", products.stream().noneMatch(p -> p.price().signum() <= 0)); // signum = znak liczby
        // WYNIK: noneMatch: żaden nie ma ceny ≤ 0? → true

        // Krótkie spięcie: allMatch kończy przy PIERWSZYM „nie”.
        boolean allBelow8 = SampleData.numbers().stream()
                .peek(n -> System.out.println("   sprawdzam " + n))
                .allMatch(n -> n < 8);
        // WYNIK: sprawdzam 5
        // WYNIK: sprawdzam 3
        // WYNIK: sprawdzam 8
        show("wszystkie < 8?", allBelow8);
        // WYNIK: wszystkie < 8? → false

        // Pusty strumień. allMatch pyta: „czy da się pokazać element, który NIE pasuje?”. Nie ma elementów → nie da się → true.
        // Do zapamiętania: „Wszystkie moje ferrari są czerwone” — nie mam żadnego, więc nikt mi nie pokaże niebieskiego.
        show("pusty: anyMatch", Stream.<Integer>empty().anyMatch(n -> n > 100));
        // WYNIK: pusty: anyMatch → false
        show("pusty: allMatch", Stream.<Integer>empty().allMatch(n -> n > 100));
        // WYNIK: pusty: allMatch → true
        show("pusty: noneMatch", Stream.<Integer>empty().noneMatch(n -> n > 100));
        // WYNIK: pusty: noneMatch → true

        // PUŁAPKA: z życia wzięta — „kto zdał wszystko (każda ocena ≥ 3)?” — Henryk nie ma ŻADNEJ oceny, a allMatch mówi „tak”.
        List<Student> students = SampleData.students();
        show("allMatch(ocena ≥ 3)", students.stream()
                .filter(s -> s.grades().stream().allMatch(g -> g >= 3))  // grades = oceny
                .map(Student::name)
                .toList());
        // WYNIK: allMatch(ocena ≥ 3) → [Ala, Celina, Ela, Filip, Gosia, Henryk]    ← Henryk „zdał” bez ocen!
        show("+ warunek: ma jakieś oceny", students.stream()
                .filter(s -> !s.grades().isEmpty() && s.grades().stream().allMatch(g -> g >= 3))
                .map(Student::name)
                .toList());
        // WYNIK: + warunek: ma jakieś oceny → [Ala, Celina, Ela, Filip, Gosia]

        // DOBRA PRAKTYKA: zamiast filter(...).count() > 0 pisz anyMatch(...) — czytelniej i kończy przy 1. trafieniu.
        // noneMatch(x) to to samo co !anyMatch(x) — wybierz to, co brzmi naturalniej.
    }

    // =================================================================================================
    // 7. toArray
    // =================================================================================================

    /**
     * 7. {@code toArray()} daje {@code Object[]}. Żeby dostać {@code String[]}, podaj „generator” tablicy:
     * {@code toArray(String[]::new)} — czyli funkcję {@code rozmiar -> new String[rozmiar]}.
     */
    static void toArrayDemo() {
        section("7. toArray() i toArray(String[]::new)");

        Object[] objects = SampleData.words().stream().distinct().limit(4).toArray();
        show("toArray() → Object[]", objects);
        // WYNIK: toArray() → Object[] → [java, stream, lambda, kolekcja]

        String[] strings = SampleData.words().stream().distinct().limit(4).toArray(String[]::new); // String[]::new = size -> new String[size]
        show("toArray(String[]::new) → String[]", strings);
        // WYNIK: toArray(String[]::new) → String[] → [java, stream, lambda, kolekcja]
        show("długość tablicy", strings.length);
        // WYNIK: długość tablicy → 4

        // PUŁAPKA: rzutowanie Object[] na String[] NIE działa, nawet jeśli w środku są same Stringi.
        expectThrows("(String[]) stream.toArray()", () -> {
            String[] wrong = (String[]) SampleData.words().stream().toArray();
            System.out.println(wrong.length);
        });
        // WYNIK: ✔ (String[]) stream.toArray() → rzucono ClassCastException: class [Ljava.lang.Object; cannot be cast to class [Ljava.lang.String; ([Ljava.lang.Object; and [Ljava.lang.String; are in module java.base of loader 'bootstrap')

        // Strumień liczb prymitywnych → tablica prymitywów (bez generatora).
        int[] ints = SampleData.numbers().stream().mapToInt(Integer::intValue).filter(n -> n > 7).toArray();
        show("int[] liczb > 7", Arrays.toString(ints));        // Arrays.toString = tablica jako tekst
        // WYNIK: int[] liczb > 7 → [8, 9, 10, 8]

        // DOBRA PRAKTYKA: w nowym kodzie wolimy listy; tablica tylko gdy wymaga jej stare API (np. metoda z String...).
    }

    // =================================================================================================
    // 8. toList() vs collect(Collectors.toList()) vs Collectors.toUnmodifiableList()
    // =================================================================================================

    /**
     * 8. Trzy sposoby na listę. Różnią się tym, czy wynik można zmieniać i czy przyjmą null:
     * {@code toList()} (Java 16+), {@code collect(Collectors.toList())}, {@code collect(Collectors.toUnmodifiableList())} (Java 10+).
     */
    static void toListVariants() {
        section("8. toList() vs Collectors.toList() vs toUnmodifiableList()");

        // (1) toList() (Java 16+): niemodyfikowalna, null DOZWOLONY.
        List<String> viaToList = Stream.of("a", null, "c").toList();
        show("toList() z null", viaToList);
        // WYNIK: toList() z null → [a, null, c]
        expectThrows("toList().add(...)", () -> viaToList.add("d"));
        // WYNIK: ✔ toList().add(...) → rzucono UnsupportedOperationException: (brak komunikatu)

        // (2) collect(Collectors.toList()): dziś ArrayList — można zmieniać. ALE specyfikacja tego nie obiecuje!
        List<String> viaCollector = Stream.of("a", null, "c").collect(Collectors.toList());
        viaCollector.add("d");
        show("Collectors.toList() + add", viaCollector);
        // WYNIK: Collectors.toList() + add → [a, null, c, d]

        // (3) Collectors.toUnmodifiableList() (Java 10+): niemodyfikowalna I bez null (jak List.of).
        List<String> viaUnmodifiable = Stream.of("a", "c").collect(Collectors.toUnmodifiableList());
        show("toUnmodifiableList()", viaUnmodifiable);
        // WYNIK: toUnmodifiableList() → [a, c]
        expectThrows("toUnmodifiableList() z null", () -> Stream.of("a", null, "c").collect(Collectors.toUnmodifiableList()));
        // WYNIK: ✔ toUnmodifiableList() z null → rzucono NullPointerException: (brak komunikatu)
        expectThrows("toUnmodifiableList().add(...)", () -> viaUnmodifiable.add("d"));
        // WYNIK: ✔ toUnmodifiableList().add(...) → rzucono UnsupportedOperationException: (brak komunikatu)

        // Potrzebujesz listy, którą będziesz zmieniać? Powiedz to wprost:
        List<String> mutable = Stream.of("a", "c").collect(Collectors.toCollection(ArrayList::new)); // toCollection = do wskazanej kolekcji
        mutable.add("d");
        show("toCollection(ArrayList::new) + add", mutable);
        // WYNIK: toCollection(ArrayList::new) + add → [a, c, d]

        // DOBRA PRAKTYKA: w Java 17 domyślnie toList(). Zmienna lista → toCollection(ArrayList::new) albo new ArrayList<>(...).
        // Pełne porównanie w ŚCIĄGA na końcu lekcji.
    }

    // =================================================================================================
    // 9. iterator() — KRÓTKO
    // =================================================================================================

    /**
     * 9. {@code iterator()} pozwala pobierać elementy strumienia ręcznie, jeden po drugim — i leniwie
     * (następny element jest liczony dopiero przy {@code next()}).
     */
    static void iteratorDemo() {
        section("9. iterator() — ręczne pobieranie elementów");

        Iterator<String> it = SampleData.words().stream()
                .distinct()
                .map(String::toUpperCase)
                .iterator();                       // iterator = „kursor” po elementach
        show("next() #1", it.next());              // next = następny
        // WYNIK: next() #1 → JAVA
        show("next() #2", it.next());
        // WYNIK: next() #2 → STREAM
        show("hasNext()", it.hasNext());           // hasNext = czy jest następny
        // WYNIK: hasNext() → true

        // Stream NIE jest Iterable, więc „for (String w : stream)” się nie skompiluje.
        // Sztuczka (rzadko potrzebna): for (String w : (Iterable<String>) stream::iterator) { ... }
        // DOBRA PRAKTYKA: iterator() tylko do współpracy ze starym API albo gdy naprawdę sterujesz pobieraniem ręcznie.
    }

    // =================================================================================================
    // 10. ZAPOWIEDŹ: reduce I collect
    // =================================================================================================

    /**
     * 10. Dwie najmocniejsze operacje końcowe mają własne lekcje. Tu tylko smak:
     * {@code reduce} składa elementy w JEDEN wynik, {@code collect} zbiera je do kontenera (lista, tekst, mapa).
     */
    static void reduceCollectPreview() {
        section("10. Zapowiedź: reduce i collect");

        // reduce: 0 + 5 + 3 + 8 + ... — szczegóły w t16_streams/Streams07Reduce.
        int sum = SampleData.numbers().stream().reduce(0, Integer::sum);   // reduce = zredukuj (złóż w jeden wynik)
        show("reduce(0, Integer::sum)", sum);
        // WYNIK: reduce(0, Integer::sum) → 66

        // collect(joining): sklejanie tekstów — t16_streams/Streams09CollectorsBasic.
        String joined = SampleData.words().stream().distinct().limit(3).collect(Collectors.joining(", "));
        show("collect(joining(\", \"))", joined);
        // WYNIK: collect(joining(", ")) → java, stream, lambda

        // collect(groupingBy): liczność w każdej kategorii — t16_streams/Streams11GroupingBy.
        // EnumMap trzyma kolejność enuma, więc wynik jest powtarzalny.
        show("groupingBy + counting", SampleData.products().stream()
                .collect(Collectors.groupingBy(Product::category, () -> new EnumMap<>(Category.class), Collectors.counting())));
        // WYNIK: groupingBy + counting → {ELEKTRONIKA=4, SPOZYWCZE=3, KSIAZKI=3, ODZIEZ=2, DOM=2}
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • operacja końcowa uruchamia potok; JEDNA na strumień; potem „stream has already been operated upon or closed”
     *   • forEach → efekt uboczny na końcu (w parallel dowolna kolejność); forEachOrdered → zawsze po kolei
     *   • count() → long (Math.toIntExact, gdy musisz mieć int); może pominąć peek/map, gdy zna rozmiar (Java 9+)
     *   • min/max(Comparator) → Optional; pusty strumień → Optional.empty; nie rób .get() bez sprawdzenia
     *   • findFirst → pierwszy wg kolejności; findAny → dowolny (szybszy w parallel)
     *   • anyMatch / allMatch / noneMatch → boolean, krótkie spięcie; pusty: false / true / true
     *   • toArray() → Object[];  toArray(String[]::new) → String[];  (String[]) toArray() → ClassCastException
     *   ┌────────────────────────────────────┬────────────────┬──────────┬──────────────────────────┐
     *   │ sposób                             │ można zmieniać?│ null?    │ uwagi                    │
     *   ├────────────────────────────────────┼────────────────┼──────────┼──────────────────────────┤
     *   │ toList()  (Java 16+)               │ NIE            │ TAK      │ domyślny wybór w Java 17 │
     *   │ collect(Collectors.toList())       │ dziś tak*      │ TAK      │ *spec. nie gwarantuje    │
     *   │ collect(toUnmodifiableList()) (10+)│ NIE            │ NIE (NPE)│ jak List.of              │
     *   │ collect(toCollection(ArrayList::new))│ TAK          │ TAK      │ gdy MUSISZ zmieniać      │
     *   └────────────────────────────────────┴────────────────┴──────────┴──────────────────────────┘
     *   • iterator() → ręczne, leniwe pobieranie; Stream nie jest Iterable
     *   • reduce → jeden wynik (Streams07); collect → kontener: lista, tekst, mapa (Streams09–13)
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się operacja pośrednia od końcowej? Ile operacji końcowych może mieć jeden strumień?
     *   2. Co wypisze:  System.out.println(Stream.<Integer>empty().allMatch(n -> n > 100));  ?
     *   3. Co wypisze:  System.out.println(Stream.of(3, 1, 2).max(Comparator.naturalOrder()));  ?
     *   4. ZNAJDŹ BŁĄD:  int available = products.stream().filter(Product::inStock).count();
     *   5. ZNAJDŹ BŁĄD:  List<String> names = products.stream().map(Product::name).toList();  names.add("Nowy");
     *   6. ZNAJDŹ BŁĄD:  String[] arr = (String[]) words.stream().toArray();
     *   7. Kiedy findAny zamiast findFirst? A kiedy anyMatch zamiast findAny().isPresent()?
     *   8. Dlaczego „kto zdał wszystko” przez allMatch może pokazać studenta bez żadnej oceny?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        List<Product> products = SampleData.products();
        List<Employee> employees = SampleData.employees();
        List<Order> orders = SampleData.orders();
        List<Order> reversedOrders = reversed(orders);
        Check.equal("ćw. 1: ELEKTRONIKA ma braki?", true, () -> exercise1(products, Category.ELEKTRONIKA));
        Check.equal("ćw. 1: KSIAZKI mają braki?", false, () -> exercise1(products, Category.KSIAZKI));
        Check.equal("ćw. 2: najstarszy w IT", "Michał Lewandowski", () -> exercise2(employees, Department.IT));
        Check.equal("ćw. 2: najstarszy w LOGISTYKA", "brak", () -> exercise2(employees, Department.LOGISTYKA));
        Check.equal("ćw. 3: jest 5 i nie ma 2", List.of("Ala", "Celina", "Ela", "Gosia"), () -> exercise3(SampleData.students()));
        Check.equal("ćw. 4: zamówienia po dacie?", true, () -> exercise4(orders));
        Check.equal("ćw. 4: odwrócone po dacie?", false, () -> exercise4(reversedOrders));
        Check.equal("ćw. 4: pusta lista po dacie?", true, () -> exercise4(List.of()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec, ELEKTRONIKA)", true, () -> solution1(products, Category.ELEKTRONIKA));
        Check.equal("ćw. 1 (wzorzec, KSIAZKI)", false, () -> solution1(products, Category.KSIAZKI));
        Check.equal("ćw. 2 (wzorzec, IT)", "Michał Lewandowski", () -> solution2(employees, Department.IT));
        Check.equal("ćw. 2 (wzorzec, LOGISTYKA)", "brak", () -> solution2(employees, Department.LOGISTYKA));
        Check.equal("ćw. 3 (wzorzec)", List.of("Ala", "Celina", "Ela", "Gosia"), () -> solution3(SampleData.students()));
        Check.equal("ćw. 4 (wzorzec, po dacie)", true, () -> solution4(orders));
        Check.equal("ćw. 4 (wzorzec, odwrócone)", false, () -> solution4(reversedOrders));
        Check.equal("ćw. 4 (wzorzec, pusta)", true, () -> solution4(List.of()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 8 OK, ✘ 0 BŁĄD
    }

    /** Pomocnicza: kopia listy w odwrotnej kolejności (do testów ćwiczenia 4). */
    private static <T> List<T> reversed(List<T> list) {
        List<T> copy = new ArrayList<>(list);
        Collections.reverse(copy);   // Collections.reverse = odwróć kolejność (w miejscu)
        return copy;
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ na strumień. Metoda sprawdza, czy w danej kategorii jest choć jeden
     * produkt, którego NIE MA na stanie:
     * <pre>{@code
     * for (Product p : products) {
     *     if (p.category() == category && !p.inStock()) {
     *         return true;
     *     }
     * }
     * return false;
     * }</pre>
     * Podpowiedź: jedna operacja końcowa z krótkim spięciem — anyMatch. Usuń linię z throw.
     */
    static boolean exercise1(List<Product> products, Category category) {
        // TODO: twoje rozwiązanie (usuń throw)
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): Zwróć imię i nazwisko NAJSTARSZEGO pracownika w danym dziale
     * albo tekst "brak", gdy w dziale nikt nie pracuje.
     * Podpowiedź: filter → max(Comparator.comparingInt(Employee::age)) → map(Employee::name) → orElse("brak").
     */
    static String exercise2(List<Employee> employees, Department department) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 3 (średnie): Zwróć imiona studentów, którzy mają CO NAJMNIEJ JEDNĄ piątkę
     * i ANI JEDNEJ dwójki.
     * Podpowiedź: w filter połącz dwa pytania o {@code s.grades().stream()}: anyMatch(g -> g == 5) i noneMatch(g -> g == 2).
     */
    static List<String> exercise3(List<Student> students) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): Sprawdź, czy zamówienia są ułożone po dacie NIEMALEJĄCO
     * (każde kolejne ma datę taką samą albo późniejszą). Pusta lista też jest „ułożona”.
     * Podpowiedź: porównuj sąsiadów po indeksach: {@code IntStream.range(1, orders.size())} i allMatch,
     * w którym element i nie może mieć daty WCZEŚNIEJSZEJ niż element i - 1 (isBefore).
     * Zastanów się, dlaczego dla pustej listy wynik true wychodzi „sam”. Usuń linię z throw.
     */
    static boolean exercise4(List<Order> orders) {
        // TODO: twoje rozwiązanie (usuń throw)
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(List<Product> products, Category category) {
        return products.stream()
                .anyMatch(p -> p.category() == category && !p.inStock());
    }

    static String solution2(List<Employee> employees, Department department) {
        return employees.stream()
                .filter(e -> e.department() == department)
                .max(Comparator.comparingInt(Employee::age))
                .map(Employee::name)
                .orElse("brak");
    }

    static List<String> solution3(List<Student> students) {
        return students.stream()
                .filter(s -> s.grades().stream().anyMatch(g -> g == 5))
                .filter(s -> s.grades().stream().noneMatch(g -> g == 2))
                .map(Student::name)
                .toList();
    }

    static boolean solution4(List<Order> orders) {
        // Pusta lista → range(1, 0) jest pusty → allMatch zwraca true. Lista 1-elementowa → range(1, 1) też pusty → true.
        return IntStream.range(1, orders.size())
                .allMatch(i -> !orders.get(i).date().isBefore(orders.get(i - 1).date()));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Pośrednia (filter, map, sorted...) zwraca nowy Stream i jest leniwa. Końcowa (count, toList, forEach...)
     *      uruchamia cały potok i zwraca wynik (albo nic). Jedna — po niej strumień jest zużyty.
     *   2. true — w pustym strumieniu nie ma elementu, który by NIE pasował.
     *   3. Optional[3] — max zawsze zwraca Optional, bo strumień mógłby być pusty.
     *   4. count() zwraca long → błąd kompilacji. Poprawnie: long available = ...; (albo Math.toIntExact(...)).
     *   5. toList() daje listę niemodyfikowalną → add rzuci UnsupportedOperationException.
     *      Potrzebujesz zmieniać: collect(Collectors.toCollection(ArrayList::new)) albo new ArrayList<>(lista).
     *   6. toArray() tworzy Object[], a Object[] to nie String[] → ClassCastException. Poprawnie: toArray(String[]::new).
     *   7. findAny: gdy kolejność nie ma znaczenia (zwłaszcza w parallel — szybciej). Gdy chcesz tylko odpowiedzi
     *      tak/nie — anyMatch: krócej i czytelniej niż findAny().isPresent().
     *   8. Bo allMatch na pustej liście ocen zwraca true („brak kontrprzykładu”). Dodaj warunek !grades().isEmpty().
     */
    // </editor-fold>
}
