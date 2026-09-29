package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Tworzenie streamów — skąd wziąć strumień
 *        (creation = tworzenie; create = utworzyć)
 *
 * W SKRÓCIE:
 *   Każdy potok zaczyna się od ŹRÓDŁA. Najczęściej to kolekcja (lista.stream()), ale stream można zrobić też
 *   z tablicy, z pojedynczych wartości, z zakresu liczb, z funkcji wytwarzającej kolejne elementy, z napisu i z pliku.
 *
 * ANALOGIA:
 *   Taśma produkcyjna może być zasilana z różnych miejsc: z magazynu (kolekcja), z ciężarówki z kilkoma paczkami
 *   (Stream.of), z maszyny, która sama produkuje kolejne sztuki (iterate/generate), albo z przenośnika
 *   z innej hali (plik).
 *
 * JAK TO DZIAŁA:
 *   Najważniejsze źródła:
 *   kolekcja.stream()             — lista, zbiór, kolejka; dla mapy: map.entrySet()/keySet()/values().stream()
 *   Stream.of(a, b, c)            — kilka podanych wartości
 *   Arrays.stream(tablica)        — tablica (dla int[] daje IntStream)
 *   IntStream.range(a, b)         — liczby od a do b-1;   rangeClosed(a, b) — od a do b
 *   Stream.iterate(start, f)      — start, f(start), f(f(start))... (nieskończony! trzeba ograniczyć)
 *   Stream.iterate(start, war, f) — jak pętla for: dopóki warunek spełniony (Java 9+)
 *   Stream.generate(supplier)     — każdy element z „dostawcy” (nieskończony! trzeba ograniczyć)
 *   "tekst".chars() / lines()     — kody znaków / linie napisu (lines: Java 11+)
 *   Files.lines(ścieżka)          — linie pliku (ZAMYKAJ w try-with-resources!)
 *   Stream.concat(a, b)           — sklejenie dwóch strumieni
 *
 * SŁÓWKA:
 *   of = z (utwórz z podanych); empty = pusty; nullable = mogący być null; range = zakres; closed = domknięty;
 *   iterate = iterować (liczyć kolejne wartości krok po kroku); generate = generuj, wytwarzaj; supplier = dostawca;
 *   seed = ziarno (wartość startowa); limit = ogranicz; chars = znaki; lines = linie; split = podziel;
 *   builder = budowniczy; build = zbuduj; concat = sklej; boxed = opakowany ({@code IntStream → Stream<Integer>});
 *   entry = wpis (para klucz–wartość); key = klucz; value = wartość; skip = pomiń.
 *
 * ZOBACZ TEŻ: Streams08PrimitiveStreams (IntStream dokładnie), Streams16Laziness (strumienie nieskończone),
 *             t18_io_files/Io02ReadingText (czytanie plików).
 * </pre>
 */
public class Streams02Creation {

    // throws IOException (rzuca IOException = wyjątek wejścia/wyjścia) — metoda fromFile() pracuje na plikach,
    // a operacje na plikach mogą rzucić ten wyjątek sprawdzany. W lekcji przepuszczamy go przez main.
    // W prawdziwej aplikacji obsłuż go sensownie (t10_exceptions).
    public static void main(String[] args) throws IOException {
        title("Streams02 — tworzenie streamów");

        fromCollections();      // from collections = z kolekcji
        fromValues();           // from values = z wartości
        fromArrays();           // from arrays = z tablic
        fromRanges();           // from ranges = z zakresów liczb
        fromIterate();          // from iterate = z iteracji (krok po kroku)
        fromGenerate();         // from generate = z generatora
        fromStrings();          // from strings = z napisów
        fromBuilderAndConcat(); // from builder and concat = z budowniczego i ze sklejenia
        fromFile();             // from file = z pliku
        exercises();
    }

    // =================================================================================================
    // 1. Z KOLEKCJI
    // =================================================================================================

    /**
     * 1. Każda kolekcja (List, Set, Queue...) ma metodę stream().
     * Mapa NIE ma stream() — bo mapa to nie kolekcja elementów, tylko par. Robimy stream z jednego z trzech „widoków”:
     * entrySet() (pary), keySet() (klucze), values() (wartości).
     */
    static void fromCollections() {
        section("1. Z kolekcji: list/set .stream(), mapa przez entrySet/keySet/values");

        List<String> list = List.of("jabłko", "banan", "gruszka");
        show("z listy", list.stream().map(String::length).toList());   // długość każdego słowa
        // WYNIK: z listy → [6, 5, 7]

        // TreeSet = zbiór POSORTOWANY. Używamy go celowo, żeby kolejność była przewidywalna.
        // PUŁAPKA: HashSet i Set.of NIE gwarantują kolejności — przy Set.of kolejność może być inna przy każdym
        // uruchomieniu programu! Nie zakładaj żadnej kolejności elementów zbioru, chyba że to TreeSet/LinkedHashSet.
        Set<String> set = new TreeSet<>(List.of("ser", "chleb", "masło"));
        show("ze zbioru (TreeSet)", set.stream().toList());
        // WYNIK: ze zbioru (TreeSet) → [chleb, masło, ser]

        // TreeMap = mapa posortowana po kluczach (znów: dla przewidywalnej kolejności).
        // Map.of = utwórz niemodyfikowalną mapę z par klucz, wartość (kolejność — jak w Set.of — nieokreślona).
        Map<String, Integer> prices = new TreeMap<>(Map.of("chleb", 5, "masło", 9, "ser", 12));

        // entrySet() — strumień WPISÓW (Map.Entry): każdy wpis ma getKey() (pobierz klucz) i getValue() (pobierz wartość)
        List<String> entries = prices.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue() + " zł")
                .toList();
        show("z mapy — entrySet", entries);
        // WYNIK: z mapy — entrySet → [chleb=5 zł, masło=9 zł, ser=12 zł]

        show("z mapy — keySet", prices.keySet().stream().toList());      // keySet = zbiór kluczy
        show("z mapy — values", prices.values().stream().toList());      // values = wartości
        // WYNIK: z mapy — keySet → [chleb, masło, ser]
        // WYNIK: z mapy — values → [5, 9, 12]
    }

    // =================================================================================================
    // 2. Z PODANYCH WARTOŚCI
    // =================================================================================================

    /**
     * 2. Stream.of(...) — strumień z wartości wypisanych „z ręki”. Stream.empty() — pusty strumień.
     * Stream.ofNullable(x) (Java 9+) — strumień 1-elementowy, albo pusty, gdy x == null.
     */
    static void fromValues() {
        section("2. Z wartości: Stream.of, Stream.empty, Stream.ofNullable");

        show("Stream.of", Stream.of("pon", "wt", "śr").toList());
        // WYNIK: Stream.of → [pon, wt, śr]

        show("Stream.empty().count()", Stream.empty().count());          // empty = pusty
        // WYNIK: Stream.empty().count() → 0

        String missing = null;
        show("ofNullable(null).count()", Stream.ofNullable(missing).count());
        show("ofNullable(\"x\").count()", Stream.ofNullable("x").count());
        // WYNIK: ofNullable(null).count() → 0
        // WYNIK: ofNullable("x").count() → 1
        // Po co? Żeby „opcjonalną” wartość bezpiecznie włączyć do potoku, bez ifów sprawdzających null.

        // PUŁAPKA: Stream.of z tablicą typu prostego (int[]) NIE daje strumienia liczb!
        int[] numbers = {1, 2, 3};
        show("Stream.of(int[]).count()", Stream.of(numbers).count());
        // WYNIK: Stream.of(int[]).count() → 1    ← JEDEN element: cała tablica! (Stream<int[]>)
        show("Arrays.stream(int[]).count()", Arrays.stream(numbers).count());
        // WYNIK: Arrays.stream(int[]).count() → 3    ← tak jest poprawnie (IntStream)
        // Dlaczego? Stream.of(T... values) przyjmuje OBIEKTY. int[] to JEDEN obiekt (tablica), a nie trzy.
        // Z tablicą obiektów (np. String[] albo Integer[]) Stream.of działa „normalnie”.
    }

    // =================================================================================================
    // 3. Z TABLIC
    // =================================================================================================

    /**
     * 3. Arrays.stream(tablica) — strumień z tablicy (Arrays = klasa narzędziowa do tablic).
     * Dla tablic typów prostych (int[], long[], double[]) dostajesz strumień „liczbowy” (IntStream...),
     * który ma od razu sum(), average(), max()...
     */
    static void fromArrays() {
        section("3. Z tablic: Arrays.stream");

        String[] days = {"pon", "wt", "śr", "czw", "pt"};
        show("cała tablica", Arrays.stream(days).toList());
        show("fragment [1, 3)", Arrays.stream(days, 1, 3).toList());    // od indeksu 1 (włącznie) do 3 (bez 3)
        // WYNIK: cała tablica → [pon, wt, śr, czw, pt]
        // WYNIK: fragment [1, 3) → [wt, śr]

        int[] temperatures = {12, 15, 9, 18, 14};
        show("suma", Arrays.stream(temperatures).sum());                 // sum = suma
        show("średnia", Arrays.stream(temperatures).average());          // average = średnia → OptionalDouble
        show("maksimum", Arrays.stream(temperatures).max());             // max = maksimum → OptionalInt
        // WYNIK: suma → 68
        // WYNIK: średnia → OptionalDouble[13.6]
        // WYNIK: maksimum → OptionalInt[18]
        // Dlaczego Optional? Bo z PUSTEJ tablicy nie da się policzyć średniej ani maksimum — wyniku może nie być.
    }

    // =================================================================================================
    // 4. Z ZAKRESÓW LICZB
    // =================================================================================================

    /**
     * 4. {@code IntStream.range(a, b)} — liczby a, a+1, ..., b-1 (bez b!). {@code rangeClosed(a, b)} — z b włącznie.
     * To „streamowy” odpowiednik pętli {@code for (int i = a; i < b; i++)}.
     * <p>
     * boxed() (opakowany) zamienia IntStream na {@code Stream<Integer>} — potrzebne, żeby zebrać liczby do
     * {@code List<Integer>} (lista nie może trzymać typu prostego int). Szczegóły: Streams08PrimitiveStreams.
     */
    static void fromRanges() {
        section("4. Z zakresów: IntStream.range / rangeClosed");

        show("range(1, 5)", IntStream.range(1, 5).boxed().toList());
        show("rangeClosed(1, 5)", IntStream.rangeClosed(1, 5).boxed().toList());
        // WYNIK: range(1, 5) → [1, 2, 3, 4]
        // WYNIK: rangeClosed(1, 5) → [1, 2, 3, 4, 5]

        show("suma 1..100", IntStream.rangeClosed(1, 100).sum());
        // WYNIK: suma 1..100 → 5050

        // Zakres jako „licznik powtórzeń” (mapToObj = zamień każdą liczbę na obiekt)
        show("3 powtórzenia", IntStream.range(0, 3).mapToObj(i -> "próba " + (i + 1)).toList());
        // WYNIK: 3 powtórzenia → [próba 1, próba 2, próba 3]

        // DOBRA PRAKTYKA: range(0, lista.size()) to sposób na strumień INDEKSÓW listy (gdy potrzebujesz pozycji).
    }

    // =================================================================================================
    // 5. Z ITERACJI — Stream.iterate
    // =================================================================================================

    /**
     * 5. {@code Stream.iterate(start, funkcja)} — pierwszy element to start, każdy następny to funkcja(poprzedni).
     * Taki strumień jest NIESKOŃCZONY, więc MUSISZ go ograniczyć (limit).
     * <p>
     * Wersja z 3 argumentami (Java 9+) działa jak pętla for: {@code iterate(start, warunek, krok)}.
     */
    static void fromIterate() {
        section("5. Z iteracji: Stream.iterate");

        // 1, 2, 4, 8, ... — każdy następny = poprzedni * 2; limit(10) = weź tylko 10 pierwszych
        show("potęgi dwójki", Stream.iterate(1, n -> n * 2).limit(10).toList());
        // WYNIK: potęgi dwójki → [1, 2, 4, 8, 16, 32, 64, 128, 256, 512]

        // Wersja „jak pętla for”:  for (int n = 1; n <= 100; n = n * 3)
        show("potęgi trójki ≤ 100", Stream.iterate(1, n -> n <= 100, n -> n * 3).toList());
        // WYNIK: potęgi trójki ≤ 100 → [1, 3, 9, 27, 81]

        // Kolejne daty — co tydzień. LocalDate = data bez godziny; of = utwórz z (rok, miesiąc, dzień);
        // plusWeeks = dodaj tygodnie. Daty są niezmienne, więc każda to NOWY obiekt (t17_datetime).
        show("4 kolejne poniedziałki", Stream.iterate(LocalDate.of(2026, 1, 5), d -> d.plusWeeks(1))
                .limit(4)
                .toList());
        // WYNIK: 4 kolejne poniedziałki → [2026-01-05, 2026-01-12, 2026-01-19, 2026-01-26]

        // PUŁAPKA: Stream.iterate(1, n -> n + 1).forEach(System.out::println);  ← NIGDY SIĘ NIE SKOŃCZY!
        //   Nieskończony strumień bez limit(...) (albo bez warunku w wersji 3-argumentowej) = program, który
        //   kręci się bez końca, a przy zbieraniu do listy (toList) — w końcu pada z braku pamięci (OutOfMemoryError).
        // PUŁAPKA: Stream.generate(() -> 1).distinct().limit(2).toList()  ← też się zawiesi: distinct przepuszcza
        //   tylko NOWE wartości, a drugiej różnej od 1 nigdy nie będzie, więc limit(2) nigdy się nie zapełni.
    }

    // =================================================================================================
    // 6. Z GENERATORA — Stream.generate
    // =================================================================================================

    /**
     * 6. {@code Stream.generate(supplier)} — każdy element pochodzi z wywołania „dostawcy”
     * (Supplier = funkcja bez argumentów, która coś zwraca). Też NIESKOŃCZONY — ograniczamy przez limit.
     * <p>
     * Różnica względem iterate: generate NIE zna poprzedniego elementu (każdy powstaje niezależnie).
     */
    static void fromGenerate() {
        section("6. Z generatora: Stream.generate");

        show("trzy gwiazdki", Stream.generate(() -> "*").limit(3).toList());
        // WYNIK: trzy gwiazdki → [*, *, *]

        // Random = generator liczb pseudolosowych. Z ziarnem (seed) 42 zawsze daje TE SAME „losowe” liczby,
        // dzięki czemu wynik lekcji jest powtarzalny. Bez ziarna (new Random()) wyniki byłyby inne przy każdym uruchomieniu.
        Random random = new Random(42);
        // nextInt(6) = następna losowa liczba z zakresu 0..5, więc +1 daje 1..6 (kostka do gry)
        show("5 rzutów kostką", Stream.generate(() -> random.nextInt(6) + 1).limit(5).toList());
        // WYNIK: 5 rzutów kostką → [3, 4, 1, 3, 1]

        // To samo krócej: Random ma własne metody zwracające strumienie liczb losowych.
        // ints(ile, od, do) — „do” NIE jest włączone, dlatego 7 (daje 1..6)
        Random random2 = new Random(42);
        show("5 rzutów (ints)", random2.ints(5, 1, 7).boxed().toList());
        // WYNIK: 5 rzutów (ints) → [3, 4, 1, 3, 1]
    }

    // =================================================================================================
    // 7. Z NAPISÓW
    // =================================================================================================

    /**
     * 7. Napisy jako źródło: znaki (chars), linie (lines), fragmenty po podziale (split / splitAsStream).
     */
    static void fromStrings() {
        section("7. Z napisów: chars, lines, split");

        // chars() zwraca IntStream z KODAMI znaków (liczby), a nie znaki!
        // Dokładniej: z jednostkami UTF-16. Zwykłe litery to jedna jednostka, ale np. emoji — dwie.
        // Dla tekstu z emoji używa się codePoints() (kody całych znaków).
        show("\"Java\".chars()", "Java".chars().boxed().toList());
        // WYNIK: "Java".chars() → [74, 97, 118, 97]    ← kody Unicode: J=74, a=97, v=118
        // Żeby dostać znaki — rzutujemy kod na char:
        show("jako znaki", "Java".chars().mapToObj(c -> (char) c).toList());
        // WYNIK: jako znaki → [J, a, v, a]

        // lines() (Java 11+) — strumień linii napisu; dzieli po \n, \r\n oraz \r (końce linii z różnych systemów)
        String text = "pierwsza linia\ndruga linia\ntrzecia linia";
        show("lines().count()", text.lines().count());
        // WYNIK: lines().count() → 3

        // split = podziel — zwraca TABLICĘ, więc potem Arrays.stream
        show("split(\" \")", Arrays.stream("Ala ma kota".split(" ")).toList());
        // WYNIK: split(" ") → [Ala, ma, kota]

        // Pattern = skompilowany wzorzec (wyrażenie regularne); compile = skompiluj;
        // splitAsStream = podziel od razu na strumień (bez tablicy pośredniej).
        // Wzorzec "\\s*,\\s*" = przecinek z dowolną liczbą białych znaków (\s) wokół.
        show("splitAsStream", Pattern.compile("\\s*,\\s*").splitAsStream("masło , chleb,ser ,  mleko").toList());
        // WYNIK: splitAsStream → [masło, chleb, ser, mleko]

        // PUŁAPKA: split przyjmuje WYRAŻENIE REGULARNE (regex). split(".") NIE dzieli po kropce — kropka w regex
        //   oznacza „dowolny znak”! Po kropce dzieli się tak: split("\\.")  (t04_strings/Strings05Regex).
    }

    // =================================================================================================
    // 8. BUDOWNICZY I SKLEJANIE
    // =================================================================================================

    /**
     * 8. {@code Stream.builder()} — gdy elementy dodajesz po kolei (np. w ifach), a na końcu chcesz z nich stream.
     * {@code Stream.concat(a, b)} — skleja dwa strumienie w jeden (najpierw elementy a, potem b).
     */
    static void fromBuilderAndConcat() {
        section("8. Stream.builder i Stream.concat");

        show("składniki wypłaty (z premią)", incomeParts(true));
        show("składniki wypłaty (bez premii)", incomeParts(false));
        // WYNIK: składniki wypłaty (z premią) → [pensja, premia, nadgodziny]
        // WYNIK: składniki wypłaty (bez premii) → [pensja, nadgodziny]

        Stream<String> morning = Stream.of("kawa", "kanapka");
        Stream<String> evening = Stream.of("herbata", "zupa");
        show("concat", Stream.concat(morning, evening).toList());
        // WYNIK: concat → [kawa, kanapka, herbata, zupa]
        // Typowe użycie: nagłówek + wiersze danych przy zapisie CSV:  Stream.concat(Stream.of(header), rows)
    }

    /** incomeParts = składniki wypłaty. Pokazuje builder: elementy dodawane warunkowo. */
    private static List<String> incomeParts(boolean withBonus) {
        Stream.Builder<String> builder = Stream.builder();    // Stream.Builder = budowniczy strumienia
        builder.add("pensja");                                // add = dodaj
        if (withBonus) {
            builder.add("premia");
        }
        builder.add("nadgodziny");
        return builder.build().toList();                      // build = zbuduj (gotowy strumień)
    }

    // =================================================================================================
    // 9. Z PLIKU
    // =================================================================================================

    /**
     * 9. {@code Files.lines(ścieżka)} — strumień linii pliku. Czyta LENIWIE, linia po linii
     * (nie wczytuje całego pliku do pamięci).
     * <p>
     * DOBRA PRAKTYKA: strumień z Files.lines trzyma OTWARTY PLIK, więc trzeba go zamknąć — najprościej przez
     * try-with-resources: {@code try (Stream<String> lines = Files.lines(path)) { ... }}.
     * Po wyjściu z bloku try Java sama zamknie plik — nawet jeśli poleci wyjątek.
     * (Strumieni z kolekcji zamykać nie trzeba — one nie trzymają żadnych zasobów systemu.)
     * <p>
     * Path = ścieżka do pliku; Files = klasa narzędziowa do operacji na plikach; write = zapisz.
     */
    static void fromFile() throws IOException {
        section("9. Z pliku: Files.lines (+ try-with-resources)");

        Path dir = TempDir.create("streams02");              // katalog tymczasowy (helpers/TempDir)
        try {
            Path file = dir.resolve("sprzedaz.csv");          // resolve = dołącz nazwę pliku do ścieżki katalogu
            Files.write(file, SampleData.salesCsvLines());    // zapisz linie do pliku (w kodowaniu UTF-8)

            try (Stream<String> lines = Files.lines(file)) {
                show("liczba wszystkich linii", lines.count());
            }
            // WYNIK: liczba wszystkich linii → 13

            try (Stream<String> lines = Files.lines(file)) {
                long dataRows = lines
                        .skip(1)                                  // skip = pomiń (tu: nagłówek)
                        .filter(line -> !line.isBlank())          // isBlank (Java 11+) = czy pusty albo same spacje
                        .count();
                show("niepuste wiersze danych", dataRows);
            }
            // WYNIK: niepuste wiersze danych → 11

            // PUŁAPKA: bez try-with-resources plik zostaje otwarty aż do końca programu (tzw. wyciek zasobu).
            //   Przy wielu plikach program może wyczerpać systemowy limit otwartych plików. Starsze API
            //   (np. FileReader, FileInputStream) dodatkowo blokuje na Windows usunięcie niezamkniętego pliku.
        } finally {                                           // finally = zawsze na końcu (sprzątanie)
            TempDir.deleteRecursively(dir);
        }
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   kolekcja.stream()                     — List/Set/Queue; mapa: entrySet()/keySet()/values().stream()
     *   Stream.of(a, b, c) / Stream.empty()   — z wartości / pusty
     *   Stream.ofNullable(x)                  — 0 albo 1 element, bezpiecznie dla null (Java 9+)
     *   Arrays.stream(tab) / (tab, od, do)    — z tablicy (int[] → IntStream); NIE Stream.of(int[])!
     *   IntStream.range(a, b) / rangeClosed   — [a, b) / [a, b]
     *   Stream.iterate(s, f).limit(n)         — s, f(s), f(f(s))... — zawsze ogranicz!
     *   Stream.iterate(s, warunek, f)         — jak pętla for (Java 9+)
     *   Stream.generate(supplier).limit(n)    — niezależne elementy, np. losowe — zawsze ogranicz!
     *   "txt".chars() / lines()               — kody znaków UTF-16 (IntStream) / linie (Java 11+)
     *   Pattern.compile(regex).splitAsStream  — podział napisu od razu na stream
     *   Stream.builder() / Stream.concat(a,b) — budowanie po kawałku / sklejanie
     *   Files.lines / Files.list / Files.walk / Files.find — trzymają otwarte zasoby: ZAWSZE try-with-resources
     *
     * PYTANIA KONTROLNE:
     *   1. Jak zrobić stream z mapy? Podaj trzy sposoby.
     *   2. Co wypisze:  System.out.println(Stream.of(new int[]{1, 2, 3}).count());  — i dlaczego?
     *   3. Co wypisze:  System.out.println(IntStream.range(1, 4).sum());  ?
     *   4. Czym różni się Stream.iterate od Stream.generate?
     *   5. ZNAJDŹ BŁĄD:  List<Integer> all = Stream.iterate(1, n -> n + 1).filter(n -> n % 2 == 0).toList();
     *   6. Co wypisze:  System.out.println("abc".chars().sum());  (kody: a=97, b=98, c=99)
     *   7. Dlaczego Files.lines trzeba zamykać, a list.stream() nie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        List<Integer> powersOf10 = List.of(1, 10, 100, 1000, 10000, 100000, 1000000);

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: kwadraty liczb 1..5", List.of(1, 4, 9, 16, 25), () -> exercise1());
        Check.equal("ćw. 2: potęgi 10 do miliona", powersOf10, () -> exercise2());
        Check.equal("ćw. 3: liczba małych liter 'a'", 5L, () -> exercise3("Ala ma kota a kot ma Alę"));
        Check.equal("ćw. 4: liczby z „brudnego” CSV", List.of(10, 20, 30), () -> exercise4("  10, 20 ,,30 , "));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of(1, 4, 9, 16, 25), () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", powersOf10, () -> solution2());
        Check.equal("ćw. 3 (wzorzec)", 5L, () -> solution3("Ala ma kota a kot ma Alę"));
        Check.equal("ćw. 4 (wzorzec)", List.of(10, 20, 30), () -> solution4("  10, 20 ,,30 , "));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć listę kwadratów liczb od 1 do 5: [1, 4, 9, 16, 25].
     * Podpowiedź: IntStream.rangeClosed + {@code map(n -> n * n)} + boxed() + toList().
     */
    static List<Integer> exercise1() {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć kolejne potęgi 10, nie większe niż 1 000 000.
     * Podpowiedź: 3-argumentowy Stream.iterate(start, warunek, krok).
     */
    static List<Integer> exercise2() {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): policz, ile razy w tekście występuje MAŁA litera 'a' (wielkie 'A' się nie liczą).
     * Podpowiedź: chars() + {@code filter(c -> c == 'a')} + count(). Porównanie int z 'a' działa, bo char to też liczba (97).
     */
    static long exercise3(String text) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): z „brudnego” napisu CSV zrób listę liczb. Napis ma spacje wokół liczb
     * i puste pola, np. "  10, 20 ,,30 , " ma dać [10, 20, 30].
     * Podpowiedź: podziel po przecinku (split albo splitAsStream) → trim() (usuń spacje z brzegów) →
     * odfiltruj puste (isBlank) → map(Integer::parseInt) → toList().
     * PUŁAPKA: Integer.parseInt(" 20") rzuci NumberFormatException — spacje trzeba usunąć PRZED parsowaniem.
     */
    static List<Integer> exercise4(String csv) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<Integer> solution1() {
        return IntStream.rangeClosed(1, 5)
                .map(n -> n * n)          // map na IntStream zwraca znów IntStream
                .boxed()                  // IntStream → Stream<Integer>
                .toList();
    }

    static List<Integer> solution2() {
        return Stream.iterate(1, n -> n <= 1_000_000, n -> n * 10)   // 1_000_000 — podkreślenia tylko dla czytelności
                .toList();
    }

    static long solution3(String text) {
        return text.chars()
                .filter(c -> c == 'a')
                .count();
    }

    static List<Integer> solution4(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)                  // trim = przytnij (usuń białe znaki z początku i końca)
                .filter(s -> !s.isBlank())          // wyrzuć puste pola
                .map(Integer::parseInt)             // Integer::parseInt = s -> Integer.parseInt(s)
                .toList();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. map.entrySet().stream() (pary), map.keySet().stream() (klucze), map.values().stream() (wartości).
     *   2. 1 — cała tablica int[] jest jednym obiektem; powstaje Stream<int[]>. Dla liczb: Arrays.stream(tab).
     *   3. 6 — range(1, 4) to 1, 2, 3 (bez 4).
     *   4. iterate liczy każdy element z POPRZEDNIEGO (f(poprzedni)); generate tworzy każdy element niezależnie (supplier).
     *   5. Brak ograniczenia — strumień jest nieskończony; filter go nie kończy, więc toList() nigdy się nie skończy
     *      (w końcu OutOfMemoryError). Potrzebny limit(...) albo 3-argumentowy iterate.
     *   6. 294 (97 + 98 + 99) — chars() daje kody znaków, a nie znaki.
     *   7. Files.lines trzyma otwarty plik (zasób systemu); strumień z kolekcji nie trzyma żadnego zasobu.
     */
    // </editor-fold>
}
