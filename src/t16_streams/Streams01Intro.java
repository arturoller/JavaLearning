package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Stream — wprowadzenie. Czym jest strumień i dlaczego warto go używać
 *        (stream = strumień; intro/introduction = wprowadzenie)
 *
 * W SKRÓCIE:
 *   Stream to „taśma”, po której przejeżdżają elementy kolekcji. Po drodze możesz je filtrować,
 *   przekształcać i sortować, a na końcu zebrać wynik (np. do nowej listy).
 *   Stream NIE przechowuje danych i NIE zmienia oryginalnej kolekcji — tworzy nowy wynik.
 *
 * ANALOGIA: taśma produkcyjna w fabryce.
 *   • ŹRÓDŁO (source) — magazyn, z którego surowce trafiają na taśmę (lista, tablica, plik...).
 *   • OPERACJE POŚREDNIE (intermediate operations) — stanowiska przy taśmie: jedno odrzuca wadliwe sztuki (filter),
 *     drugie przerabia każdą sztukę na inny produkt (map), trzecie układa w kolejności (sorted).
 *   • OPERACJA KOŃCOWA (terminal operation) — pakowanie na końcu taśmy (toList, count, collect...).
 *   • Taśma rusza dopiero, gdy na końcu stoi pakowacz (LENIWOŚĆ — bez operacji końcowej nic się nie dzieje).
 *   • Raz przejechana partia nie wraca — żeby przetworzyć dane jeszcze raz, trzeba uruchomić taśmę od nowa
 *     (JEDNORAZOWOŚĆ — streamu nie da się użyć dwa razy).
 *
 * JAK TO DZIAŁA:
 *   Budowa każdego potoku (pipeline = potok):
 *
 *     lista.stream()            ← 1. ŹRÓDŁO: zrób strumień z kolekcji
 *          .filter(...)         ← 2. operacja pośrednia (zwraca nowy strumień) — może ich być dowolnie dużo
 *          .map(...)            ← 2. operacja pośrednia
 *          .toList();           ← 3. OPERACJA KOŃCOWA: uruchamia cały potok i daje wynik (już nie strumień!)
 *
 *   Pętla to podejście IMPERATYWNE (rozkazujące) — mówisz komputerowi JAK: „utwórz listę, dla każdego elementu
 *   sprawdź warunek, jeśli spełniony — dodaj, potem posortuj...”.
 *   Stream to podejście DEKLARATYWNE (opisowe) — mówisz CO chcesz: „odfiltruj dostępne, weź nazwy, posortuj,
 *   zbierz do listy”. Kod ze streamami czyta się jak zdanie opisujące wynik.
 *
 * SŁÓWKA:
 *   stream = strumień; pipeline = potok; source = źródło; intermediate = pośredni; terminal = końcowy;
 *   filter = filtruj (zostaw pasujące); map = przekształć (zamień każdy element na inny); sorted = posortowany;
 *   toList = do listy; count = policz; lazy = leniwy; consumed = zużyty; imperative = imperatywny (rozkazujący);
 *   declarative = deklaratywny (opisowy); in stock = na stanie (w magazynie); stateless = bezstanowy.
 *
 * ZOBACZ TEŻ: Streams02Creation (skąd wziąć stream), Streams16Laziness (leniwość dokładnie),
 *             t13_lambdas (lambdy i referencje do metod, na których opierają się streamy).
 * </pre>
 */
public class Streams01Intro {

    public static void main(String[] args) {
        title("Streams01 — wprowadzenie do streamów");

        loopVsStream();            // loop vs stream = pętla kontra strumień
        pipelineAnatomy();         // pipeline anatomy = budowa potoku
        streamDoesNotStoreData();  // stream does not store data = strumień nie przechowuje danych
        sourceIsNotModified();     // source is not modified = źródło nie jest zmieniane
        nothingWithoutTerminal();  // nothing without terminal = nic bez operacji końcowej (leniwość)
        verticalProcessing();      // vertical processing = przetwarzanie pionowe (element po elemencie)
        shortCircuit();            // short circuit = skrócone wykonanie (wcześniejsze zakończenie)
        singleUse();               // single use = jednorazowe użycie
        whenLoopIsBetter();        // when loop is better = kiedy pętla jest lepsza
        exercises();               // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PĘTLA vs STREAM
    // =================================================================================================

    /**
     * 1. To samo zadanie rozwiązane pętlą i streamem.
     * <p>
     * ZADANIE: podaj nazwy produktów z kategorii ELEKTRONIKA, które są na stanie,
     * posortowane i zapisane WIELKIMI LITERAMI.
     */
    static void loopVsStream() {
        section("1. Pętla vs stream — to samo zadanie na dwa sposoby");

        List<Product> products = SampleData.products();

        // ---------- PRZED: pętla (imperatywnie — mówimy JAK) ----------
        List<String> resultLoop = new ArrayList<>();                // ArrayList = lista oparta na tablicy; tu: pusta lista na wynik
        for (Product p : products) {                                // dla każdego produktu...
            if (p.category() == Category.ELEKTRONIKA && p.inStock()) { // ...sprawdź warunek
                resultLoop.add(p.name().toUpperCase());             // ...zamień na WIELKIE litery (toUpperCase) i dodaj
            }
        }
        Collections.sort(resultLoop);                               // Collections.sort = posortuj listę (zmienia ją!)
        show("pętla", resultLoop);
        // WYNIK: pętla → [LAPTOP PRO 14, MONITOR 27 CALI, SŁUCHAWKI BT]

        // ---------- PO: stream (deklaratywnie — mówimy CO) ----------
        List<String> resultStream = products.stream()               // weź produkty...
                .filter(p -> p.category() == Category.ELEKTRONIKA)  // ...tylko elektronikę
                .filter(Product::inStock)                           // ...tylko dostępne
                .map(p -> p.name().toUpperCase())                   // ...zamień na nazwę wielkimi literami
                .sorted()                                           // ...posortuj
                .toList();                                          // ...zbierz do listy
        show("stream", resultStream);
        // WYNIK: stream → [LAPTOP PRO 14, MONITOR 27 CALI, SŁUCHAWKI BT]

        // Jak czytać lambdę  p -> p.category() == Category.ELEKTRONIKA :
        //   „dla produktu p zwróć: czy kategoria p to ELEKTRONIKA”. Po lewej od -> parametr, po prawej wynik.
        // Jak czytać  Product::inStock :
        //   referencja do metody (method reference) — skrót od  p -> p.inStock().  Więcej: t13_lambdas/Lambda04MethodReferences.

        // toList() (Java 16+) — zwraca listę NIEMODYFIKOWALNĄ (sekcja 2 pokazuje, co się stanie przy próbie add).
        // W starszym kodzie zobaczysz zamiast tego  .collect(Collectors.toList())  — działa podobnie,
        // ale zwraca zwykłą, modyfikowalną listę. Porównanie: Streams06TerminalOps i Streams09CollectorsBasic.

        // PUŁAPKA: sorted() sortuje napisy według KODÓW znaków (Unicode), a nie według polskiego alfabetu:
        // wielkie litery są przed małymi, a „Ł”, „Ś” — po wszystkich literach łacińskich. Tu wszystko jest wielkimi
        // literami i zaczyna się od L, M, S, więc wynik wygląda „alfabetycznie”. Sortowanie po polsku: t12_collections.

        // DOBRA PRAKTYKA: w potoku każda operacja w OSOBNEJ linii, kropka na początku linii.
        // Wtedy czytasz go z góry na dół jak przepis: „weź → odfiltruj → przekształć → posortuj → zbierz”.

        note("Oba sposoby dają ten sam wynik. Stream nie potrzebuje listy pomocniczej ani ifów — opisuje wynik.");
    }

    // =================================================================================================
    // 2. BUDOWA POTOKU
    // =================================================================================================

    /**
     * 2. Budowa potoku krok po kroku — z typem każdego etapu.
     * <p>
     * Zwykle piszemy potok „jednym ciągiem”, ale tu rozbijamy go na zmienne, żeby zobaczyć, co zwraca każdy krok.
     * Typy: {@code Stream<Product> → Stream<Product> → Stream<String> → List<String>}.
     * <p>
     * UWAGA: trzymanie streamów w zmiennych robimy TYLKO tutaj, żeby pokazać typy. Na co dzień tego nie rób
     * (dlaczego — sekcja 8).
     */
    static void pipelineAnatomy() {
        section("2. Budowa potoku: źródło → operacje pośrednie → operacja końcowa");

        List<Product> products = SampleData.products();

        // KROK 1 — ŹRÓDŁO. stream() zamienia listę w strumień produktów.
        Stream<Product> source = products.stream();

        // KROK 2 — OPERACJA POŚREDNIA. filter zwraca NOWY strumień z tym samym typem elementów (Product),
        // ale „przepuszcza” tylko te, dla których warunek zwraca true.
        Stream<Product> available = source.filter(Product::inStock);

        // KROK 3 — OPERACJA POŚREDNIA. map zmienia TYP elementów: z Product robi String (nazwę).
        Stream<String> names = available.map(Product::name);

        // KROK 4 — OPERACJA KOŃCOWA. toList uruchamia cały potok i zwraca List<String>. To już NIE jest strumień.
        List<String> result = names.toList();

        show("liczba dostępnych produktów", result.size());       // size = rozmiar (liczba elementów)
        show("pierwsze trzy nazwy", result.subList(0, 3));        // subList = podlista [od, do) — „do” nie wchodzi
        // WYNIK: liczba dostępnych produktów → 12
        // WYNIK: pierwsze trzy nazwy → [Laptop Pro 14, Słuchawki BT, Monitor 27 cali]

        // To samo „jednym ciągiem” — tak piszemy na co dzień:
        List<String> sameResult = products.stream()
                .filter(Product::inStock)
                .map(Product::name)
                .toList();
        show("wynik identyczny?", result.equals(sameResult));      // equals = równy (porównuje zawartość list)
        // WYNIK: wynik identyczny? → true

        // Lista z toList() jest NIEMODYFIKOWALNA:
        expectThrows("dodanie elementu do listy z toList()", () -> result.add("Nowy produkt"));
        // WYNIK: ✔ dodanie elementu do listy z toList() → rzucono UnsupportedOperationException: (brak komunikatu)
        // (UnsupportedOperation = „nieobsługiwana operacja”)

        // ZASADA: operacja pośrednia ZAWSZE zwraca strumień (Stream albo jego wersję liczbową: IntStream, LongStream,
        // DoubleStream — Streams08PrimitiveStreams). Operacja końcowa zwraca COŚ INNEGO (List, long, Optional, Map,
        // albo nic — void) i kończy potok.
    }

    // =================================================================================================
    // 3. STREAM NIE PRZECHOWUJE DANYCH
    // =================================================================================================

    /**
     * 3. Stream to nie kolekcja.
     * <p>
     * Kolekcja (List, Set) to POJEMNIK — trzyma elementy w pamięci, możesz do nich wracać, pobierać po indeksie.
     * Stream to PRZEPŁYW — elementy przez niego przechodzą. Nie ma metody get(indeks) ani size().
     */
    static void streamDoesNotStoreData() {
        section("3. Stream nie przechowuje danych");

        List<String> list = List.of("a", "b", "c");                // List.of = utwórz niemodyfikowalną listę z podanych
        Stream<String> stream = list.stream();

        show("lista wypisana", list);
        // WYNIK: lista wypisana → [a, b, c]

        // Wypisanie streamu NIE pokazuje elementów — tylko nazwę wewnętrznej klasy Javy
        // (getClass = pobierz klasę obiektu; getName = pobierz jej pełną nazwę). Nazwa zależy od wersji JDK.
        show("stream wypisany (jego klasa)", stream.getClass().getName());
        // WYNIK: stream wypisany (jego klasa) → java.util.stream.ReferencePipeline$Head

        // PUŁAPKA: System.out.println(stream) wypisze coś w stylu java.util.stream.ReferencePipeline$Head@1b6d3586,
        // a nie elementy. Żeby zobaczyć elementy — zbierz je: stream.toList(), albo użyj forEach(System.out::println).
        note("Stream to „przepis na przetwarzanie”, a nie pojemnik z danymi.");
        // WYNIK: ℹ Stream to „przepis na przetwarzanie”, a nie pojemnik z danymi.
    }

    // =================================================================================================
    // 4. ŹRÓDŁO NIE JEST ZMIENIANE
    // =================================================================================================

    /**
     * 4. Operacje na streamie tworzą NOWY wynik, a oryginalna kolekcja zostaje nietknięta.
     * Używamy ZMIENIALNEJ listy (ArrayList) — gdybyśmy wzięli List.of, dowód byłby pusty,
     * bo List.of i tak nie da się zmienić.
     */
    static void sourceIsNotModified() {
        section("4. Źródło nie jest zmieniane");

        List<String> names = new ArrayList<>(List.of("anna", "piotr", "ewa"));   // kopia do zmienialnej listy
        List<String> upper = names.stream()
                .map(String::toUpperCase)        // String::toUpperCase = skrót od  s -> s.toUpperCase()
                .toList();

        show("oryginał", names);
        show("nowy wynik", upper);
        // WYNIK: oryginał → [anna, piotr, ewa]
        // WYNIK: nowy wynik → [ANNA, PIOTR, EWA]

        // DOBRA PRAKTYKA: traktuj stream jak funkcję matematyczną — dane wchodzą, nowy wynik wychodzi,
        // nic dookoła się nie zmienia. Nie modyfikuj źródła ani innych zmiennych z wnętrza streamu
        // (to tzw. efekty uboczne — side effects; szczegóły: Streams17SideEffectsPitfalls).
    }

    // =================================================================================================
    // 5. LENIWOŚĆ — BEZ OPERACJI KOŃCOWEJ NIC SIĘ NIE DZIEJE
    // =================================================================================================

    /**
     * 5. Operacje pośrednie są LENIWE (lazy): tylko „zapisują się w przepisie”.
     * Wykonują się dopiero wtedy, gdy wywołasz operację końcową.
     * <p>
     * peek (podejrzyj) — operacja pośrednia, która pozwala „podejrzeć” element w trakcie (tu: wypisać go).
     * Używamy jej wyłącznie do nauki i debugowania. Stream.of = utwórz strumień z podanych wartości.
     */
    static void nothingWithoutTerminal() {
        section("5. Leniwość — bez operacji końcowej nic się nie dzieje");

        Stream<String> pipeline = Stream.of("x", "y", "z")
                .peek(s -> System.out.println("   peek widzi: " + s));

        note("Potok zbudowany, ale nie ma operacji końcowej — powyżej nie wypisało się nic.");
        // WYNIK: ℹ Potok zbudowany, ale nie ma operacji końcowej — powyżej nie wypisało się nic.

        List<String> result = pipeline.toList();                       // dopiero TERAZ taśma rusza
        show("po wywołaniu toList()", result);
        // WYNIK: peek widzi: x
        // WYNIK: peek widzi: y
        // WYNIK: peek widzi: z
        // WYNIK: po wywołaniu toList() → [x, y, z]

        // Dlaczego to ważne? Dzięki leniwości Java może:
        //   • nie wykonywać zbędnej pracy (np. przerwać po znalezieniu pierwszego pasującego elementu — sekcja 7),
        //   • łączyć operacje w jeden przebieg po danych (sekcja 6),
        //   • obsługiwać strumienie nieskończone (Streams02Creation, Streams16Laziness).
    }

    // =================================================================================================
    // 6. PRZETWARZANIE „PIONOWE”
    // =================================================================================================

    /**
     * 6. Kolejność wykonywania: element po elemencie przez CAŁY potok („pionowo”), a nie etap po etapie („poziomo”).
     * <p>
     * Intuicja podpowiada: „najpierw filter przejdzie po wszystkich, potem map po wszystkich”. Tak NIE jest!
     * Pierwszy element przechodzi przez filter, potem (jeśli przeszedł) przez map, i dopiero wtedy na taśmę
     * wchodzi drugi element. Dotyczy to operacji BEZSTANOWYCH (stateless — nie potrzebują znać innych
     * elementów): filter, map, peek.
     * <p>
     * WYJĄTEK: sorted() jest STANOWE (stateful) — żeby posortować, musi najpierw zebrać WSZYSTKIE elementy.
     * Działa jak zapora: wszystko przed nim wykonuje się dla całej partii, dopiero potem reszta potoku.
     * <p>
     * UWAGA: println wewnątrz filter/map to efekt uboczny — robimy to TYLKO, żeby zobaczyć kolejność.
     */
    static void verticalProcessing() {
        section("6. Przetwarzanie pionowe — element po elemencie (i wyjątek: sorted)");

        List<String> result = Stream.of("kot", "pies", "chomik", "rybka")
                .filter(s -> {
                    System.out.println("   filter: " + s);
                    return s.length() > 3;                  // length = długość; przepuść słowa dłuższe niż 3 litery
                })
                .map(s -> {
                    System.out.println("   map:    " + s);
                    return s.toUpperCase();
                })
                .toList();
        show("wynik", result);
        // WYNIK: filter: kot                 ← „kot” odpada na filtrze, do map nie dociera
        // WYNIK: filter: pies
        // WYNIK: map:    pies                ← „pies” przechodzi przez filter i OD RAZU przez map
        // WYNIK: filter: chomik
        // WYNIK: map:    chomik
        // WYNIK: filter: rybka
        // WYNIK: map:    rybka
        // WYNIK: wynik → [PIES, CHOMIK, RYBKA]

        // Ten sam potok z sorted() pośrodku — sorted musi poczekać na WSZYSTKIE elementy:
        List<String> sortedResult = Stream.of("kot", "pies", "chomik", "rybka")
                .filter(s -> {
                    System.out.println("   filter: " + s);
                    return s.length() > 3;
                })
                .sorted()
                .map(s -> {
                    System.out.println("   map:    " + s);
                    return s.toUpperCase();
                })
                .toList();
        show("wynik z sorted", sortedResult);
        // WYNIK: filter: kot
        // WYNIK: filter: pies
        // WYNIK: filter: chomik
        // WYNIK: filter: rybka                ← najpierw WSZYSTKIE przechodzą przez filter (sorted zbiera całość)...
        // WYNIK: map:    chomik               ← ...dopiero potem map, już w posortowanej kolejności
        // WYNIK: map:    pies
        // WYNIK: map:    rybka
        // WYNIK: wynik z sorted → [CHOMIK, PIES, RYBKA]
    }

    // =================================================================================================
    // 7. SHORT-CIRCUIT — WCZEŚNIEJSZE ZAKOŃCZENIE
    // =================================================================================================

    /**
     * 7. Niektóre operacje kończą pracę, gdy tylko znają odpowiedź (short-circuit = skrócone wykonanie).
     * <ul>
     *   <li>operacje KOŃCOWE: findFirst (znajdź pierwszy), findAny, anyMatch, allMatch, noneMatch,</li>
     *   <li>operacje POŚREDNIE: limit (ogranicz), takeWhile (bierz, dopóki) — Streams05SortDistinctLimit.</li>
     * </ul>
     * findFirst zwraca Optional — „pudełko”, które może być puste, gdy nic nie pasuje (t14_optional).
     */
    static void shortCircuit() {
        section("7. Short-circuit — findFirst kończy pracę wcześniej");

        // numbers() = [5, 3, 8, 1, 9, 2, 7, 3, 10, 6, 4, 8]
        Optional<Integer> firstBig = SampleData.numbers().stream()
                .filter(n -> {
                    System.out.println("   sprawdzam: " + n);
                    return n > 6;
                })
                .findFirst();
        show("pierwsza liczba > 6", firstBig);
        // WYNIK: sprawdzam: 5
        // WYNIK: sprawdzam: 3
        // WYNIK: sprawdzam: 8
        // WYNIK: pierwsza liczba > 6 → Optional[8]
        // Pozostałych 9 liczb w ogóle nie sprawdzono!

        note("Pętla też by się zatrzymała (break) — stream robi to automatycznie.");
        // WYNIK: ℹ Pętla też by się zatrzymała (break) — stream robi to automatycznie.
    }

    // =================================================================================================
    // 8. STREAM JEST JEDNORAZOWY
    // =================================================================================================

    /**
     * 8. Stream można użyć tylko raz. Po operacji końcowej jest ZUŻYTY (consumed).
     * Druga operacja na tym samym obiekcie Stream rzuca IllegalStateException (illegal state = niedozwolony stan).
     * <p>
     * PUŁAPKA: „zużyty” ≠ „zamknięty”. Operacja końcowa NIE wywołuje close(). To ważne przy streamach, które
     * trzymają zasób (np. otwarty plik z Files.lines) — te zamykasz sam, w try-with-resources (Streams02Creation).
     */
    static void singleUse() {
        section("8. Stream jest jednorazowy");

        Stream<Product> stream = SampleData.products().stream();
        long count = stream.count();                     // count = policz → zużywa stream; zwraca long
        show("liczba produktów", count);
        // WYNIK: liczba produktów → 14

        expectThrows("drugie użycie tego samego streamu", () -> stream.toList());
        // WYNIK: ✔ drugie użycie tego samego streamu → rzucono IllegalStateException: stream has already been operated upon or closed
        // Tłumaczenie komunikatu: „na strumieniu już wykonano operację albo został zamknięty”.

        // DOBRA PRAKTYKA: NIE zapisuj streamów w zmiennych ani polach „na później”.
        // Trzymaj KOLEKCJĘ i za każdym razem twórz nowy stream: products.stream()...
        List<Product> products = SampleData.products();
        long inStock = products.stream().filter(Product::inStock).count();
        long outOfStock = products.stream().filter(p -> !p.inStock()).count();   // nowy stream z tej samej listy — OK
        show("na stanie / brak", inStock + " / " + outOfStock);
        // WYNIK: na stanie / brak → 12 / 2
    }

    // =================================================================================================
    // 9. KIEDY PĘTLA JEST LEPSZA
    // =================================================================================================

    /**
     * 9. Streamy nie zawsze są najlepszym wyborem. Pętla bywa czytelniejsza, gdy:
     * <ul>
     *   <li>potrzebujesz INDEKSU (pozycji) elementu,</li>
     *   <li>zmieniasz kilka zmiennych naraz albo logika ma dużo warunków i wyjść (break/continue),</li>
     *   <li>w środku są metody rzucające wyjątki sprawdzane (checked) — lambdy ich „nie lubią” (t13_lambdas/Lambda08Pitfalls),</li>
     *   <li>modyfikujesz elementy tablicy „w miejscu”.</li>
     * </ul>
     * ZADANIE: znajdź indeks pierwszego produktu, którego NIE ma na stanie.
     */
    static void whenLoopIsBetter() {
        section("9. Kiedy pętla jest lepsza — potrzebny indeks");

        List<Product> products = SampleData.products();

        // Pętla — prosto i czytelnie:
        int indexLoop = -1;                                   // -1 = „nie znaleziono” (częsta konwencja)
        for (int i = 0; i < products.size(); i++) {
            if (!products.get(i).inStock()) {                 // get(i) = pobierz element o indeksie i
                indexLoop = i;
                break;                                        // break = przerwij pętlę
            }
        }
        show("pętla", indexLoop);
        // WYNIK: pętla → 1                ← Smartfon X, drugi na liście (indeksy liczymy od 0)

        // Stream — da się, ale trzeba „sztucznie” zrobić strumień indeksów 0, 1, 2, ...
        // IntStream = strumień liczb int; range(0, n) = liczby od 0 do n-1 (Streams02Creation).
        int indexStream = IntStream.range(0, products.size())
                .filter(i -> !products.get(i).inStock())
                .findFirst()                                       // zwraca OptionalInt (Optional dla int)
                .orElse(-1);                                       // orElse = albo (gdy nie znaleziono) -1
        show("stream", indexStream);
        // WYNIK: stream → 1

        // DOBRA PRAKTYKA: wybieraj to, co CZYTELNIEJSZE dla danego zadania. Streamy świetnie nadają się do
        // „przetwórz kolekcję w nową kolekcję/wartość”, pętle — do algorytmów z indeksami i złożonym sterowaniem.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Potok = ŹRÓDŁO (list.stream()) → OPERACJE POŚREDNIE (filter, map, sorted...) → OPERACJA KOŃCOWA (toList, count...).
     *   • Operacja pośrednia zwraca strumień; operacja końcowa zwraca wynik (List, long, Optional...) i kończy potok.
     *   • Stream NIE przechowuje danych i NIE zmienia źródła — tworzy nowy wynik.
     *   • toList() (Java 16+) = lista NIEMODYFIKOWALNA; starsze collect(Collectors.toList()) = zwykła lista.
     *   • LENIWOŚĆ: bez operacji końcowej nic się nie wykona.
     *   • Kolejność PIONOWA dla filter/map/peek; sorted() to zapora — zbiera wszystkie elementy przed dalszym krokiem.
     *   • SHORT-CIRCUIT: końcowe findFirst/anyMatch..., pośrednie limit/takeWhile — kończą wcześniej.
     *   • JEDNORAZOWOŚĆ: drugi raz na tym samym Stream → IllegalStateException. Trzymaj kolekcję, nie stream.
     *   • Zużyty ≠ zamknięty: Files.lines zamykaj w try-with-resources.
     *   • Pętla jest lepsza przy indeksach i złożonym sterowaniu.
     *
     * PYTANIA KONTROLNE:
     *   1. Z jakich trzech części składa się każdy potok streamu?
     *   2. Czym różni się operacja pośrednia od końcowej (co zwracają)?
     *   3. Co wypisze ten kod i dlaczego?
     *          Stream.of("a", "b").map(s -> { System.out.println(s); return s; });
     *   4. Co wypisze ostatnia linia?
     *          List<Integer> list = new ArrayList<>(List.of(1, 2));
     *          list.stream().map(x -> x * 10).toList();
     *          System.out.println(list);
     *   5. W jakiej kolejności wypiszą się komunikaty z filter i map dla trzech elementów? A jeśli między nimi
     *      stoi sorted()?
     *   6. ZNAJDŹ BŁĄD:
     *          Stream<Product> s = products.stream();
     *          long all = s.count();
     *          long available = s.filter(Product::inStock).count();
     *   7. Podaj dwie sytuacje, w których pętla jest lepsza od streamu.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /**
     * Uruchamia ćwiczenia: najpierw TWOJE rozwiązania (✘ dopóki nie uzupełnisz), potem wzorcowe (✔).
     * {@code () -> exercise1(...)} to lambda — Check sam ją uruchomi i złapie ewentualny wyjątek.
     */
    static void exercises() {
        List<String> expected1 = List.of("Anna Nowak", "Piotr Kowalski", "Michał Lewandowski", "Ewa Woźniak");
        List<String> expected3 = List.of("EKSPRES DO KAWY", "JAVA. PODSTAWY", "LAPTOP PRO 14", "MONITOR 27 CALI", "WZORCE PROJEKTOWE");

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: pracownicy działu IT", expected1, () -> exercise1(SampleData.employees()));
        // PUŁAPKA: count() zwraca long, więc oczekiwana wartość to 4L (Long), a nie 4 (Integer)!
        Check.equal("ćw. 2: produkty droższe niż 1000 zł", 4L, () -> exercise2(SampleData.products()));
        Check.equal("ćw. 3: pętla przepisana na stream", expected3, () -> exercise3(SampleData.products()));
        Check.equal("ćw. 4a: pierwsze słowo dłuższe niż 6 liter", "kolekcja", () -> exercise4(SampleData.words(), 6));
        Check.equal("ćw. 4b: pierwsze słowo dłuższe niż 10 liter", "brak", () -> exercise4(SampleData.words(), 10));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", expected1, () -> solution1(SampleData.employees()));
        Check.equal("ćw. 2 (wzorzec)", 4L, () -> solution2(SampleData.products()));
        Check.equal("ćw. 3 (wzorzec)", expected3, () -> solution3(SampleData.products()));
        Check.equal("ćw. 4a (wzorzec)", "kolekcja", () -> solution4(SampleData.words(), 6));
        Check.equal("ćw. 4b (wzorzec)", "brak", () -> solution4(SampleData.words(), 10));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć listę imion i nazwisk (name) pracowników z działu IT, w kolejności z listy.
     * Podpowiedź: filter (dział == Department.IT) → map (Employee::name) → toList().
     */
    static List<String> exercise1(List<Employee> employees) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (łatwe): policz produkty, których cena jest większa niż 1000 zł.
     * Podpowiedź: ceny to BigDecimal — porównuj przez compareTo: {@code p.price().compareTo(limit) > 0}
     * (compareTo zwraca liczbę dodatnią, gdy pierwsza wartość jest większa). Na końcu count().
     */
    static long exercise2(List<Product> products) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ PĘTLĘ NA STREAM. Poniższy kod zwraca nazwy produktów, których stan
     * magazynowy jest od 1 do 10 sztuk (włącznie), wielkimi literami, posortowane. Napisz to samo streamem.
     * <pre>{@code
     *   List<String> result = new ArrayList<>();
     *   for (Product p : products) {
     *       if (p.stock() >= 1 && p.stock() <= 10) {
     *           result.add(p.name().toUpperCase());
     *       }
     *   }
     *   Collections.sort(result);
     *   return result;
     * }</pre>
     * Podpowiedź: jeden filter z warunkiem na stock, map, sorted, toList.
     */
    static List<String> exercise3(List<Product> products) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć PIERWSZE słowo dłuższe niż minLength liter; jeśli takiego nie ma — "brak".
     * Podpowiedź: filter → findFirst() (zwraca Optional) → orElse("brak"). Zwróć uwagę, że dzięki
     * short-circuit stream nie sprawdzi słów po znalezionym.
     */
    static String exercise4(List<String> words, int minLength) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(List<Employee> employees) {
        return employees.stream()
                .filter(e -> e.department() == Department.IT)   // enumy porównujemy przez ==
                .map(Employee::name)
                .toList();
    }

    static long solution2(List<Product> products) {
        BigDecimal limit = new BigDecimal("1000");   // tworzymy RAZ, poza lambdą — nie przy każdym elemencie
        return products.stream()
                .filter(p -> p.price().compareTo(limit) > 0)
                .count();
    }

    static List<String> solution3(List<Product> products) {
        return products.stream()
                .filter(p -> p.stock() >= 1 && p.stock() <= 10)
                .map(p -> p.name().toUpperCase())
                .sorted()
                .toList();
    }

    static String solution4(List<String> words, int minLength) {
        return words.stream()
                .filter(w -> w.length() > minLength)
                .findFirst()
                .orElse("brak");
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Źródło (np. list.stream()), operacje pośrednie (filter, map...), operacja końcowa (toList, count...).
     *   2. Pośrednia zwraca nowy strumień (można dalej łączyć); końcowa zwraca wynik innego typu i uruchamia potok.
     *   3. Nic — nie ma operacji końcowej, a operacje pośrednie są leniwe.
     *   4. [1, 2] — stream nie zmienia źródła; toList() utworzył NOWĄ listę [10, 20], której nigdzie nie zapisaliśmy.
     *   5. Bez sorted: pionowo — filter(1), map(1), filter(2), map(2)... (element odrzucony przez filter nie dociera do map).
     *      Z sorted pośrodku: najpierw wszystkie filter, potem wszystkie map (sorted musi zebrać całą partię).
     *   6. Stream s jest zużyty po count() — drugie użycie rzuci IllegalStateException. Trzeba zrobić nowy:
     *      products.stream().filter(Product::inStock).count().
     *   7. Np. gdy potrzebny jest indeks elementu; gdy logika ma wiele warunków, break/continue i zmienia kilka
     *      zmiennych; gdy w środku są metody rzucające wyjątki sprawdzane.
     */
    // </editor-fold>
}
