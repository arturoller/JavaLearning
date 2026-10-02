package t23_modern_java;

import helpers.Check;
import helpers.TempDir;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Małe perełki API wydanie po wydaniu (Java 9–17)
 *        (API = interfejs programistyczny, czyli klasy i metody biblioteki; addition = dodatek)
 *
 * W SKRÓCIE:
 *   Poza dużymi nowościami języka każde wydanie Javy dodaje drobne metody, które codziennie oszczędzają kilka linii
 *   i kilka błędów: List.of, isBlank, Files.readString, Stream.toList, HexFormat... Ta lekcja to przegląd
 *   według wydań — każda perełka z jednozdaniowym PRZED/PO i najważniejszą pułapką — oraz tabela na końcu.
 *
 * ANALOGIA:
 *   Szwajcarski scyzoryk w kolejnych edycjach: nowy model dostaje kolejne małe narzędzie — otwieracz, lupę,
 *   wykałaczkę. Żadne z nich nie zmienia scyzoryka, ale po przyzwyczajeniu nie wyobrażasz sobie pracy bez nich.
 *   Kto zna tylko wersję z 2014 roku, wciąż otwiera puszki nożem.
 *
 * JAK TO DZIAŁA:
 *   Wydanie  Najważniejsze dodatki w tej lekcji
 *   Java 9   List/Set/Map.of, Optional.or/ifPresentOrElse/stream, Stream.takeWhile/dropWhile/iterate/ofNullable,
 *            prywatne metody w interfejsach, try-with-resources na zmiennej, transferTo, ProcessHandle
 *   Java 10  List.copyOf, Collectors.toUnmodifiable*, Optional.orElseThrow()
 *   Java 11  String: isBlank/strip/lines/repeat; Files.readString/writeString; Predicate.not; Optional.isEmpty;
 *            Collection.toArray(IntFunction); HttpClient
 *   Java 12  Collectors.teeing, String.indent/transform, CompactNumberFormat
 *   Java 14  pomocne komunikaty NullPointerException (JEP 358)
 *   Java 16  Stream.toList, Stream.mapMulti
 *   Java 17  RandomGenerator (JEP 356), HexFormat
 *
 * SŁÓWKA:
 *   factory method = metoda fabryczna; unmodifiable = niemodyfikowalny; copy = kopia; blank = pusty lub same białe znaki;
 *   strip = obetnij; repeat = powtórz; compact = zwięzły; helpful = pomocny; random = losowy; seed = ziarno;
 *   generator = generator; hex = szesnastkowy; sink = „zlew” (miejsce, do którego wrzucasz elementy).
 *
 * ZOBACZ TEŻ: t12_collections/Collections08ImmutableUnmodifiable (niezmienne kolekcje),
 *   t14_optional/Optional02Transform (Optional), t16_streams/Streams02Creation (tworzenie strumieni),
 *   t04_strings/Strings02Methods (napisy), t18_io_files/Io02ReadingText (odczyt plików),
 *   t28_networking_http/Http02HttpClient (HttpClient), t23_modern_java/Modern01Java8 (poprzednia lekcja)
 * </pre>
 */
public class Modern06ApiAdditions {

    static String nullField;   // null field = pole o wartości null (do demonstracji pomocnych komunikatów NPE)

    record Person(String name) { }

    /** Interfejs z metodą prywatną (private = prywatna) — Java 9. */
    interface Greeter {
        default String hello(String name) {                        // hello = witaj
            return decorate("Cześć " + name);
        }

        default String bye(String name) {                          // bye = do widzenia
            return decorate("Pa " + name);
        }

        private String decorate(String text) {                     // decorate = ozdób; widoczna tylko w interfejsie
            return "*" + text + "*";
        }
    }

    public static void main(String[] args) {
        title("Modern06 — małe perełki API");

        java9Collections();          // Java 9 collections = kolekcje z Javy 9
        java9OptionalStreams();      // Java 9 optional and streams = Optional i strumienie z Javy 9
        java9Misc();                 // Java 9 misc = różne dodatki z Javy 9
        java10();                    // Java 10
        java11Strings();             // Java 11 strings = napisy z Javy 11
        java11Other();               // Java 11 other = inne dodatki z Javy 11
        java12();                    // Java 12
        java14HelpfulNpe();          // Java 14 helpful NPE = pomocne komunikaty NPE
        java16();                    // Java 16
        java17();                    // Java 17
        summaryTable();              // summary table = tabela podsumowująca
        exercises();                 // exercises = ćwiczenia
    }

    /** Pomocnicza: pokazuje niewidoczne znaki jako tekst. */
    static String escape(String text) {
        return text.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r");
    }

    // =================================================================================================
    // 1. JAVA 9: FABRYKI KOLEKCJI
    // =================================================================================================

    /**
     * 1. Java 9 (JEP 269): List.of, Set.of, Map.of, Map.ofEntries — krótkie, NIEZMIENNE kolekcje. Poprzednio
     * trzeba było pisać Collections.unmodifiableList(new ArrayList<>(Arrays.asList(...))).
     */
    static void java9Collections() {
        section("1. Java 9: List.of, Set.of, Map.of");

        // PRZED: trzy kroki, żeby dostać listę, której nie da się zmienić.
        List<String> before = Collections.unmodifiableList(new ArrayList<>(Arrays.asList("a", "b", "c")));
        // PO (Java 9+): jedno wywołanie.
        List<String> after = List.of("a", "b", "c");
        show("PRZED == PO", before.equals(after));
        // WYNIK: PRZED == PO → true

        Set<String> set = Set.of("x", "y");
        Map<String, Integer> map = Map.of("jeden", 1, "dwa", 2);
        Map<String, Integer> entries = Map.ofEntries(Map.entry("a", 1), Map.entry("b", 2));   // entry = wpis
        show("Set.of: rozmiar i zawartość", set.size() + ", zawiera x: " + set.contains("x"));
        // WYNIK: Set.of: rozmiar i zawartość → 2, zawiera x: true
        show("Map.of: wartość dla klucza dwa", map.get("dwa"));
        // WYNIK: Map.of: wartość dla klucza dwa → 2
        show("Map.ofEntries: rozmiar", entries.size());
        // WYNIK: Map.ofEntries: rozmiar → 2

        // PUŁAPKA: kolekcje są niezmienne — każda próba zmiany to UnsupportedOperationException.
        expectThrows("dodanie do List.of", () -> after.add("d"));
        // WYNIK: ✔ dodanie do List.of → rzucono UnsupportedOperationException: (brak komunikatu)

        // PUŁAPKA: null jest zabroniony — także w pytaniu: contains(null) rzuca NullPointerException
        //   (zwykła ArrayList odpowiedziałaby po prostu false).
        expectThrows("List.of z null", () -> List.of("a", null));
        // WYNIK: ✔ List.of z null → rzucono NullPointerException: (brak komunikatu)
        expectThrows("List.of(...).contains(null)", () -> after.contains(null));
        // WYNIK: ✔ List.of(...).contains(null) → rzucono NullPointerException: (brak komunikatu)

        // PUŁAPKA: Set.of i Map.of nie tolerują duplikatów (rzucają wyjątek), a kolejność elementów jest
        //   nieokreślona i może zmienić się między uruchomieniami programu — dlatego jej nie drukujemy.
        expectThrows("Set.of z duplikatem", () -> Set.of("a", "a"));
        // WYNIK: ✔ Set.of z duplikatem → rzucono IllegalArgumentException: duplicate element: a
        expectThrows("Map.of z duplikatem klucza", () -> Map.of("a", 1, "a", 2));
        // WYNIK: ✔ Map.of z duplikatem klucza → rzucono IllegalArgumentException: duplicate key: a

        // Dla porównania: Arrays.asList ma stały rozmiar, ale elementy można zamieniać i dopuszcza null.
        List<String> fixedSize = Arrays.asList("a", "b");           // fixed size = stały rozmiar
        fixedSize.set(0, "z");                                      // set = ustaw
        show("Arrays.asList po set", fixedSize);
        // WYNIK: Arrays.asList po set → [z, b]

        // DOBRA PRAKTYKA: List.of do stałych i danych testowych; gdy dane mają być zmieniane — new ArrayList<>(List.of(...)).
        //   Map.of obsługuje do 10 par; dla większej liczby użyj Map.ofEntries. Szczegóły: t12_collections/Collections08ImmutableUnmodifiable.
    }

    // =================================================================================================
    // 2. JAVA 9: OPTIONAL I STRUMIENIE
    // =================================================================================================

    /**
     * 2. Java 9: Optional.or, ifPresentOrElse, stream oraz Stream.takeWhile, dropWhile, iterate z warunkiem, ofNullable.
     */
    static void java9OptionalStreams() {
        section("2. Java 9: Optional i Stream");

        // Optional.or (Java 9+) = „albo”: jeśli pusty, spróbuj innego Optional (wołane leniwie).
        Optional<String> primary = Optional.empty();                // primary = główny
        Optional<String> backup = Optional.of("zapasowa");          // backup = zapasowy
        show("or", primary.or(() -> backup));
        // WYNIK: or → Optional[zapasowa]

        // ifPresentOrElse (Java 9+) = „jeśli obecny, to … w przeciwnym razie …” (zastępuje if/else z isPresent).
        StringBuilder log = new StringBuilder();                    // log = dziennik
        Optional.of("A").ifPresentOrElse(v -> log.append("jest ").append(v), () -> log.append("brak"));
        Optional.empty().ifPresentOrElse(v -> log.append("jest"), () -> log.append("; brak"));
        show("ifPresentOrElse", log);
        // WYNIK: ifPresentOrElse → jest A; brak

        // Optional.stream (Java 9+): Optional jako strumień 0 lub 1 elementu — przydatne we flatMap.
        List<Optional<String>> maybe = List.of(Optional.of("a"), Optional.empty(), Optional.of("c"));
        show("flatMap(Optional::stream)", maybe.stream().flatMap(Optional::stream).toList());   // toList = Java 16+
        // WYNIK: flatMap(Optional::stream) → [a, c]

        // Stream.takeWhile / dropWhile (Java 9+) = „bierz dopóki” / „pomijaj dopóki”.
        List<Integer> numbers = List.of(1, 2, 3, 4, 1, 2);
        show("takeWhile(n < 4)", numbers.stream().takeWhile(n -> n < 4).toList());
        // WYNIK: takeWhile(n < 4) → [1, 2, 3]
        show("dropWhile(n < 4)", numbers.stream().dropWhile(n -> n < 4).toList());
        // WYNIK: dropWhile(n < 4) → [4, 1, 2]
        show("filter(n < 4) — dla porównania", numbers.stream().filter(n -> n < 4).toList());
        // WYNIK: filter(n < 4) — dla porównania → [1, 2, 3, 1, 2]

        // Stream.iterate z trzema argumentami (Java 9+): początek, warunek dalszego działania, następny element.
        // PRZED: Stream.iterate(1, n -> n * 2).limit(7) — musisz znać liczbę elementów, a nie warunek.
        show("iterate(1, n <= 100, n * 2)", Stream.iterate(1, n -> n <= 100, n -> n * 2).toList());
        // WYNIK: iterate(1, n <= 100, n * 2) → [1, 2, 4, 8, 16, 32, 64]

        // Stream.ofNullable (Java 9+): strumień pusty dla null, jednoelementowy w przeciwnym razie.
        String maybeNull = null;
        show("ofNullable(null).count()", Stream.ofNullable(maybeNull).count());
        // WYNIK: ofNullable(null).count() → 0

        // PUŁAPKA: takeWhile i filter to NIE to samo. takeWhile ZATRZYMUJE się na pierwszym elemencie, który nie
        //   spełnia warunku (reszty nie sprawdza), a filter przegląda wszystkie. Wynik dla niepustych danych bywa inny
        //   (wyżej: dwie dodatkowe liczby 1 i 2 po czwórce). takeWhile ma sens dla danych uporządkowanych.
        // PUŁAPKA: iterate z dwoma argumentami jest nieskończony — bez limit/takeWhile program się „zawiesi”.
        // DOBRA PRAKTYKA: dla Optional wybieraj ifPresentOrElse zamiast isPresent()+get(); Optional.or zamiast if/else
        //   z kilkoma źródłami wartości.
    }

    // =================================================================================================
    // 3. JAVA 9: RÓŻNE
    // =================================================================================================

    /**
     * 3. Java 9: prywatne metody w interfejsach, try-with-resources na zmiennej, InputStream.transferTo, ProcessHandle.
     */
    static void java9Misc() {
        section("3. Java 9: interfejsy, try-with-resources, transferTo, ProcessHandle");

        // Metoda prywatna w interfejsie (Java 9+): wspólny kod metod default bez wystawiania go na zewnątrz.
        Greeter greeter = new Greeter() { };                        // anonimowa implementacja bez dodatków
        show("hello", greeter.hello("Ola"));
        // WYNIK: hello → *Cześć Ola*
        show("bye", greeter.bye("Ola"));
        // WYNIK: bye → *Pa Ola*

        // try-with-resources na zmiennej (Java 9+). PRZED: trzeba było deklarować nową zmienną w nagłówku try.
        //   try (BufferedReader copy = reader) { ... }       — dodatkowa zmienna tylko po to, żeby ją zamknąć
        // PO: wystarczy nazwa istniejącej zmiennej, o ile jest finalna lub efektywnie finalna.
        BufferedReader reader = new BufferedReader(new StringReader("linia1\nlinia2"));
        try (reader) {
            show("odczyt w try (reader)", reader.readLine());
            // WYNIK: odczyt w try (reader) → linia1
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        expectThrows("odczyt po zamknięciu", () -> reader.readLine());
        // WYNIK: ✔ odczyt po zamknięciu → rzucono IOException: Stream closed

        // InputStream.transferTo (Java 9+): kopiuje cały strumień wejściowy do wyjściowego (zwraca liczbę bajtów).
        // PRZED: pętla z buforem byte[] i read/write.
        byte[] source = "Cześć".getBytes(java.nio.charset.StandardCharsets.UTF_8);   // source = źródło
        ByteArrayOutputStream target = new ByteArrayOutputStream();   // target = cel
        try {
            long copied = new ByteArrayInputStream(source).transferTo(target);   // transfer to = przenieś do
            show("transferTo: skopiowane bajty", copied + " (równe źródłu: " + Arrays.equals(source, target.toByteArray()) + ")");
            // WYNIK: transferTo: skopiowane bajty → 7 (równe źródłu: true)
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // ProcessHandle (Java 9+, JEP 102): informacje o procesach systemu operacyjnego, w tym o własnym.
        // PRZED: zgadywanie z nazwy JVM („12345@host”) albo kod natywny.
        show("własny proces ma dodatni numer PID", ProcessHandle.current().pid() > 0);   // pid = numer procesu
        // WYNIK: własny proces ma dodatni numer PID → true
        show("czy proces żyje", ProcessHandle.current().isAlive());
        // WYNIK: czy proces żyje → true
        // (Sam numer PID jest inny przy każdym uruchomieniu, więc go nie drukujemy.)

        // PUŁAPKA: try (reader) zamyka zasób po wyjściu z bloku — dalsze użycie kończy się wyjątkiem „Stream closed”,
        //   jak widać wyżej. Nie przekazuj zasobu dalej, jeśli wcześniej wprowadziłeś go do try.
        // DOBRA PRAKTYKA: transferTo zamiast własnej pętli kopiowania — mniej kodu, mniej błędów z buforem.
    }

    // =================================================================================================
    // 4. JAVA 10
    // =================================================================================================

    /**
     * 4. Java 10: List.copyOf (kopia niezmienna), Collectors.toUnmodifiable*, Optional.orElseThrow() bez argumentów.
     */
    static void java10() {
        section("4. Java 10: copyOf, toUnmodifiable*, orElseThrow()");

        // Collections.unmodifiableList to tylko WIDOK na oryginał — zmiany oryginału są widoczne przez widok.
        // List.copyOf (Java 10+) robi prawdziwą, niezmienną KOPIĘ.
        List<String> source = new ArrayList<>(List.of("a", "b"));   // source = źródło
        List<String> view = Collections.unmodifiableList(source);   // view = widok
        List<String> copy = List.copyOf(source);                    // copy = kopia
        source.add("c");
        show("widok po zmianie źródła", view);
        // WYNIK: widok po zmianie źródła → [a, b, c]
        show("kopia po zmianie źródła", copy);
        // WYNIK: kopia po zmianie źródła → [a, b]

        // Collectors.toUnmodifiableList / Set / Map (Java 10+) = „zbierz do niemodyfikowalnej …”.
        List<Integer> squares = Stream.of(1, 2, 3).map(n -> n * n).collect(Collectors.toUnmodifiableList());   // squares = kwadraty
        show("toUnmodifiableList", squares);
        // WYNIK: toUnmodifiableList → [1, 4, 9]
        expectThrows("zmiana wyniku toUnmodifiableList", () -> squares.add(16));
        // WYNIK: ✔ zmiana wyniku toUnmodifiableList → rzucono UnsupportedOperationException: (brak komunikatu)
        Map<String, Integer> lengths = Stream.of("kot", "pies").collect(Collectors.toUnmodifiableMap(s -> s, String::length));
        show("toUnmodifiableMap: długość słowa pies", lengths.get("pies"));
        // WYNIK: toUnmodifiableMap: długość słowa pies → 4

        // Optional.orElseThrow() bez argumentów (Java 10+): to samo co get(), ale z nazwą mówiącą, że może rzucić wyjątek.
        // PRZED: get() — IDE ostrzega, bo „get” brzmi niewinnie, a rzuca NoSuchElementException.
        expectThrows("orElseThrow() na pustym", () -> Optional.empty().orElseThrow());
        // WYNIK: ✔ orElseThrow() na pustym → rzucono NoSuchElementException: No value present

        // PUŁAPKA: List.copyOf robi kopię PŁYTKĄ — elementy (obiekty zmienne) nie są kopiowane. Jeśli lista zawiera
        //   zmienne obiekty, można je zmienić przez kopię. Ponadto copyOf zabrania null (jak List.of).
        // PUŁAPKA: Collectors.toUnmodifiableMap rzuca wyjątek przy zduplikowanym kluczu (jak toMap) i nie dopuszcza null.
        // DOBRA PRAKTYKA: w konstruktorze rekordu lub klasy „niezmiennej” przechowuj List.copyOf(parametr) —
        //   obrona przed późniejszymi zmianami z zewnątrz (zob. Modern05RecordsSealedPatterns, SafeTeam).
        // Uwaga: List.copyOf(lista) dla listy już niezmiennej zwykle zwraca tę samą instancję (to szczegół implementacji).
    }

    // =================================================================================================
    // 5. JAVA 11: NAPISY
    // =================================================================================================

    /**
     * 5. Java 11: String.isBlank, strip (stripLeading, stripTrailing), lines, repeat.
     */
    static void java11Strings() {
        section("5. Java 11: isBlank, strip, lines, repeat");

        // isBlank (Java 11+) = „pusty lub same białe znaki”. PRZED: s.trim().isEmpty() — tworzy nowy napis.
        show("\"\".isBlank()", "".isBlank());
        // WYNIK: "".isBlank() → true
        show("\"   \".isBlank()", "   ".isBlank());
        // WYNIK: "   ".isBlank() → true
        show("\" a \".isBlank()", " a ".isBlank());
        // WYNIK: " a ".isBlank() → false

        // strip (Java 11+) = „obetnij” białe znaki Unicode; trim obcina tylko znaki o kodzie do spacji (U+0020).
        String emSpace = String.valueOf((char) 0x2003);             // em space = długa spacja (szeroka)
        String withEm = "x" + emSpace;                              // with em = z długą spacją
        show("trim() — długość po obcięciu", withEm.trim().length());
        // WYNIK: trim() — długość po obcięciu → 2
        show("strip() — długość po obcięciu", withEm.strip().length());
        // WYNIK: strip() — długość po obcięciu → 1
        show("stripLeading / stripTrailing", "[" + "  a  ".stripLeading() + "] [" + "  a  ".stripTrailing() + "]");
        // WYNIK: stripLeading / stripTrailing → [a  ] [  a]

        // Twarda spacja (non-breaking space, U+00A0) NIE jest białym znakiem według Javy — przetrwa i trim, i strip.
        String nbsp = "x" + (char) 0xA0;                            // nbsp = twarda spacja
        show("strip() i twarda spacja", nbsp.strip().length());
        // WYNIK: strip() i twarda spacja → 2

        // lines (Java 11+) = strumień linii; rozpoznaje \n, \r i \r\n i nie tworzy pustej linii na końcu.
        // PRZED: split("\n") — nie obsłuży \r\n i potrafi zgubić puste linie na końcu.
        show("lines", "a\nb\r\nc".lines().toList());
        // WYNIK: lines → [a, b, c]

        // repeat (Java 11+) = „powtórz n razy”. PRZED: pętla z StringBuilder albo String.join("", Collections.nCopies(...)).
        show("repeat(3)", "ab".repeat(3));
        // WYNIK: repeat(3) → ababab
        show("linia z myślników", "-".repeat(10));
        // WYNIK: linia z myślników → ----------
        expectThrows("repeat(-1)", () -> "x".repeat(-1));
        // WYNIK: ✔ repeat(-1) → rzucono IllegalArgumentException: count is negative: -1

        // PUŁAPKA: pusta linia bez treści vs. null: isBlank na null rzuca NullPointerException — sprawdź null wcześniej.
        // PUŁAPKA: twarde spacje z kopiowanych tekstów (Word, strony WWW) przeżywają strip() — trzeba je zamienić
        //   ręcznie (replace((char) 0xA0, ' ')).
        // DOBRA PRAKTYKA: dla danych od użytkownika używaj strip() zamiast trim(), a isBlank() zamiast trim().isEmpty().
    }

    // =================================================================================================
    // 6. JAVA 11: POZOSTAŁE
    // =================================================================================================

    /**
     * 6. Java 11: Files.readString / writeString, Path.of, Predicate.not, Optional.isEmpty, toArray(IntFunction),
     * HttpClient (tylko opis — bez sieci).
     */
    static void java11Other() {
        section("6. Java 11: Files, Predicate.not, isEmpty, toArray, HttpClient");

        // Files.writeString / readString (Java 11+) — cały plik tekstowy jednym wywołaniem, domyślnie w UTF-8.
        // PRZED: new String(Files.readAllBytes(path), StandardCharsets.UTF_8) i pętle z BufferedReader.
        Path dir = TempDir.create("modern06");                      // dir = katalog tymczasowy
        try {
            Path file = dir.resolve("notatka.txt");                 // resolve = dołącz ścieżkę; file = plik
            Files.writeString(file, "Cześć\nświecie\n");            // write string = zapisz napis
            String back = Files.readString(file);                   // read string = odczytaj napis; back = z powrotem
            show("readString", escape(back));
            // WYNIK: readString → Cześć\nświecie\n
            try {
                Files.readString(dir.resolve("brak.txt"));
            } catch (NoSuchFileException e) {                       // no such file = brak takiego pliku
                show("odczyt brakującego pliku", e.getClass().getSimpleName());
                // WYNIK: odczyt brakującego pliku → NoSuchFileException
                // (Komunikat wyjątku zawiera ścieżkę do katalogu tymczasowego, więc go nie drukujemy.)
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            TempDir.deleteRecursively(dir);                         // sprzątanie
        }

        // Path.of (Java 11+) zastępuje Paths.get (Paths.get nadal działa).
        show("Path.of(\"a\", \"b\").getNameCount()", Path.of("a", "b").getNameCount());   // name count = liczba elementów
        // WYNIK: Path.of("a", "b").getNameCount() → 2

        // Predicate.not (Java 11+) = „negacja warunku” — pozwala użyć referencji do metody z negacją.
        // PRZED: filter(s -> !s.isBlank())
        List<String> words = List.of("kot", " ", "", "pies");
        show("filter(Predicate.not(String::isBlank))", words.stream().filter(Predicate.not(String::isBlank)).toList());
        // WYNIK: filter(Predicate.not(String::isBlank)) → [kot, pies]

        // Optional.isEmpty (Java 11+) = przeciwieństwo isPresent. PRZED: !optional.isPresent()
        show("Optional.empty().isEmpty()", Optional.empty().isEmpty());
        // WYNIK: Optional.empty().isEmpty() → true

        // Collection.toArray(IntFunction) (Java 11+): tablica właściwego typu bez „new String[0]”.
        // PRZED: list.toArray(new String[0])
        String[] array = List.of("a", "b").toArray(String[]::new);  // array = tablica
        show("toArray(String[]::new)", Arrays.toString(array));
        // WYNIK: toArray(String[]::new) → [a, b]

        // HttpClient (Java 11+, wcześniej tylko w module inkubacyjnym od Javy 9): nowoczesny klient HTTP z obsługą
        // HTTP/2 i trybu asynchronicznego. Zastępuje HttpURLConnection. Przykład (bez uruchamiania — sieć jest poza tą lekcją):
        //   HttpClient client = HttpClient.newHttpClient();
        //   HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:8080/ping")).build();
        //   String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        // Szczegóły: t28_networking_http/Http02HttpClient.
        // Java 11 pozwala też uruchomić jeden plik źródłowy bez osobnej kompilacji: java Program.java (JEP 330) —
        // tak działa weryfikator tego kursu (tools/Verify.java).

        // PUŁAPKA: Files.readString wczytuje CAŁY plik do pamięci — dla dużych plików (setki MB) użyj Files.lines
        //   albo BufferedReader (zob. t18_io_files/Io02ReadingText).
        // PUŁAPKA: jeśli plik nie jest poprawnym UTF-8, readString rzuca MalformedInputException zamiast po cichu zgubić znaki.
        // DOBRA PRAKTYKA: Predicate.not zamiast lambdy z wykrzyknikiem — kod czyta się „po polsku”: filtruj tylko niepuste.
    }

    // =================================================================================================
    // 7. JAVA 12
    // =================================================================================================

    /**
     * 7. Java 12: Collectors.teeing, String.indent i transform, CompactNumberFormat.
     */
    static void java12() {
        section("7. Java 12: teeing, indent, transform, zwięzłe liczby");

        // teeing (Java 12+) = „trójnik”: jeden przebieg po strumieniu, dwa kolektory, wynik łączony funkcją.
        // PRZED: dwa osobne przebiegi (sum + count) albo własna klasa akumulatora.
        List<Integer> data = List.of(3, 7, 1, 9);                   // data = dane
        String sumAndCount = data.stream().collect(Collectors.teeing(
                Collectors.summingInt(n -> n), Collectors.counting(), (sum, count) -> sum + "/" + count));
        show("teeing: suma i liczba", sumAndCount);
        // WYNIK: teeing: suma i liczba → 20/4
        String range = data.stream().collect(Collectors.teeing(
                Collectors.minBy(Integer::compare), Collectors.maxBy(Integer::compare),
                (min, max) -> min.orElseThrow() + ".." + max.orElseThrow()));
        show("teeing: najmniejsza i największa", range);
        // WYNIK: teeing: najmniejsza i największa → 1..9

        // String.indent (Java 12+) = „wetnij wcięcie”; dodatnia liczba dodaje spacje, ujemna usuwa; normalizuje końce linii
        // i ZAWSZE dodaje \n na końcu.
        show("indent(2)", escape("a\nb".indent(2)));
        // WYNIK: indent(2) →   a\n  b\n
        // (Przed „a” i „b” stoją po dwie spacje; na końcu doszedł znak nowej linii.)
        show("indent(-2)", escape("    a\n  b".indent(-2)));
        // WYNIK: indent(-2) →   a\nb\n

        // String.transform (Java 12+): zastosuj funkcję do napisu i zwróć jej wynik — łańcuchy bez zmiennych pośrednich.
        String shout = "  hej ".transform(String::strip).transform(s -> s.toUpperCase(Locale.ROOT) + "!");   // shout = krzyk
        show("transform", shout);
        // WYNIK: transform → HEJ!

        // CompactNumberFormat (Java 12+, NumberFormat.getCompactNumberInstance) = „1,5 mln”, „2K”: skracanie liczb.
        // Zawsze podawaj Locale jawnie.
        NumberFormat shortUs = NumberFormat.getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);   // short = krótki
        show("US SHORT: 1234 / 1500000", shortUs.format(1234) + " / " + shortUs.format(1_500_000));
        // WYNIK: US SHORT: 1234 / 1500000 → 1K / 2M
        shortUs.setMaximumFractionDigits(1);                        // maximum fraction digits = maks. cyfr po przecinku
        show("po setMaximumFractionDigits(1)", shortUs.format(1234) + " / " + shortUs.format(1_500_000));
        // WYNIK: po setMaximumFractionDigits(1) → 1.2K / 1.5M
        NumberFormat longUs = NumberFormat.getCompactNumberInstance(Locale.US, NumberFormat.Style.LONG);   // long = długi
        show("US LONG: 2000000", longUs.format(2_000_000));
        // WYNIK: US LONG: 2000000 → 2 million
        NumberFormat shortPl = NumberFormat.getCompactNumberInstance(Locale.forLanguageTag("pl-PL"), NumberFormat.Style.SHORT);
        String millions = shortPl.format(2_000_000).replace((char) 0xA0, ' ').replace((char) 0x202F, ' ');
        String thousands = shortPl.format(5000).replace((char) 0xA0, ' ').replace((char) 0x202F, ' ');
        show("pl-PL SHORT: 2000000 / 5000", millions + " / " + thousands);
        // WYNIK: pl-PL SHORT: 2000000 / 5000 → 2 mln / 5 tys.

        // PUŁAPKA: domyślne zaokrąglenie to HALF_EVEN („bankierskie”), a liczba cyfr po przecinku domyślnie 0 — dlatego
        //   1 500 000 pokazuje się jako „2M”, a nie „1,5M”. Ustaw setMaximumFractionDigits, jeśli potrzebujesz ułamków.
        // PUŁAPKA: polski format zawiera twarde spacje (U+00A0) — przed porównaniem lub wypisaniem zamień je na zwykłe
        //   (jak wyżej), bo różne systemy i czcionki wyświetlają je różnie.
        // DOBRA PRAKTYKA: teeing zamiast dwóch przebiegów po tym samym strumieniu (strumień można zużyć tylko raz!).
    }

    // =================================================================================================
    // 8. JAVA 14: POMOCNE KOMUNIKATY NPE
    // =================================================================================================

    /**
     * 8. Java 14 (JEP 358): komunikat NullPointerException mówi DOKŁADNIE, co było nullem. Włączone domyślnie od
     * Javy 15 (w Javie 14 trzeba było dodać -XX:+ShowCodeDetailsInExceptionMessages). Do Javy 13 był tylko
     * „java.lang.NullPointerException” bez szczegółów — szukanie źródła w łańcuchu a.b().c() było zgadywaniem.
     */
    static void java14HelpfulNpe() {
        section("8. Java 14: pomocne komunikaty NullPointerException");

        try {
            nullField.length();
        } catch (NullPointerException e) {
            show("pole statyczne", e.getMessage());
            // WYNIK: pole statyczne → Cannot invoke "String.length()" because "t23_modern_java.Modern06ApiAdditions.nullField" is null
        }
        try {
            new Person(null).name().length();
        } catch (NullPointerException e) {
            show("wynik metody", e.getMessage());
            // WYNIK: wynik metody → Cannot invoke "String.length()" because the return value of "t23_modern_java.Modern06ApiAdditions$Person.name()" is null
        }
        Map<String, String> map = new HashMap<>();
        try {
            map.get("brak").trim();
        } catch (NullPointerException e) {
            show("wynik Map.get", e.getMessage());
            // WYNIK: wynik Map.get → Cannot invoke "String.trim()" because the return value of "java.util.Map.get(Object)" is null
        }

        // Komunikat składa się z dwóch części: CO próbowaliśmy zrobić („Cannot invoke ...” = nie można wywołać)
        // i DLACZEGO się nie udało („because ... is null” = ponieważ ... ma wartość null). Komunikaty są po angielsku
        // (tak generuje je JVM), więc warto umieć je czytać: invoke = wywołać, because = ponieważ, return value = wartość zwrócona.

        // PUŁAPKA: dla zmiennych LOKALNYCH nazwa pojawia się w komunikacie tylko wtedy, gdy kod skompilowano z opcją -g
        //   (informacje debugowania); inaczej widzisz „<local1>” zamiast nazwy. Dlatego w tej lekcji nie drukujemy
        //   komunikatów o zmiennych lokalnych — wynik zależałby od sposobu kompilacji.
        // PUŁAPKA: nie parsuj tego tekstu w kodzie i nie traktuj go jako stałego interfejsu — format może się zmienić.
        //   Komunikat może też ujawnić nazwy pól i metod, więc nie wysyłaj go użytkownikowi końcowemu (tylko do logów).
        // DOBRA PRAKTYKA: czytaj komunikat do końca zanim zaczniesz debugować — zwykle od razu wskazuje null.
        //   Zamiast łapać NPE, zapobiegaj: Objects.requireNonNull(x, "x nie może być null") i Optional dla wyników.
    }

    // =================================================================================================
    // 9. JAVA 16
    // =================================================================================================

    /**
     * 9. Java 16: Stream.toList i Stream.mapMulti.
     */
    static void java16() {
        section("9. Java 16: Stream.toList i mapMulti");

        // Stream.toList (Java 16+) zamiast collect(Collectors.toList()): krócej i zwraca listę NIEMODYFIKOWALNĄ.
        List<String> names = Stream.of("Ala", "Ola").toList();
        expectThrows("zmiana wyniku toList()", () -> names.add("Ewa"));
        // WYNIK: ✔ zmiana wyniku toList() → rzucono UnsupportedOperationException: (brak komunikatu)
        // Collectors.toList() zwraca zwykle zmienną ArrayList — ale specyfikacja tego nie gwarantuje:
        List<String> mutable = Stream.of("Ala", "Ola").collect(Collectors.toList());   // mutable = zmienna (w praktyce)
        mutable.add("Ewa");
        show("Collectors.toList() po dodaniu", mutable);
        // WYNIK: Collectors.toList() po dodaniu → [Ala, Ola, Ewa]

        // PUŁAPKA: w odróżnieniu od List.of, toList() DOPUSZCZA null jako element (a List.of rzuca wyjątek).
        show("toList() z null", Stream.of("a", null).toList());
        // WYNIK: toList() z null → [a, null]

        // mapMulti (Java 16+) = „mapuj na wiele”: dla każdego elementu możesz wywołać „sink” 0, 1 lub wiele razy.
        // PRZED: flatMap z Stream.of(...) / Stream.empty() — tworzy mały strumień na każdy element.
        List<Integer> expanded = Stream.of(1, 2, 3).<Integer>mapMulti((n, sink) -> {      // expanded = rozwinięte
            if (n % 2 == 1) {                                       // tylko liczby nieparzyste
                sink.accept(n);                                     // accept = przyjmij
                sink.accept(n * 10);
            }
        }).toList();
        show("mapMulti", expanded);
        // WYNIK: mapMulti → [1, 10, 3, 30]

        // PUŁAPKA: przy mapMulti kompilator często nie potrafi wywnioskować typu wynikowego z lambdy — trzeba go podać
        //   jawnie: stream.<Integer>mapMulti(...) (jak wyżej).
        // DOBRA PRAKTYKA: toList() jako domyślny sposób kończenia strumienia, gdy nie potrzebujesz zmieniać listy;
        //   mapMulti — gdy z jednego elementu powstaje niewiele wyników; flatMap — gdy masz już gotową kolekcję.
    }

    // =================================================================================================
    // 10. JAVA 17
    // =================================================================================================

    /**
     * 10. Java 17: RandomGenerator (JEP 356) — wspólny interfejs generatorów liczb losowych, i HexFormat.
     */
    static void java17() {
        section("10. Java 17: RandomGenerator i HexFormat");

        // RandomGenerator (Java 17+, pakiet java.util.random) = „generator losowy”: jeden interfejs dla Random,
        // SplittableRandom i nowych algorytmów. Nazwany algorytm tworzysz przez fabrykę, a ziarno (seed) daje powtarzalność.
        // PRZED: tylko java.util.Random (jeden, dość słaby algorytm) lub ThreadLocalRandom.
        RandomGenerator seeded = RandomGeneratorFactory.of("L64X128MixRandom").create(42L);   // seeded = z ziarnem
        show("L64X128MixRandom(42): trzy liczby", seeded.nextInt(1000) + " " + seeded.nextInt(1000) + " " + seeded.nextInt(1000));
        // WYNIK: L64X128MixRandom(42): trzy liczby → 398 901 980
        RandomGenerator legacy = new Random(42);                    // legacy = stary (Random implementuje RandomGenerator)
        show("Random(42): nextInt(100)", legacy.nextInt(100));
        // WYNIK: Random(42): nextInt(100) → 30
        // Tworząc generator z tym samym ziarnem dostajesz tę samą sekwencję — w testach to podstawa powtarzalności.

        // HexFormat (Java 17+) = zapis szesnastkowy bajtów. PRZED: String.format("%02x", b) w pętli.
        byte[] bytes = {1, (byte) 255, 16};
        show("formatHex", HexFormat.of().formatHex(bytes));
        // WYNIK: formatHex → 01ff10
        show("z separatorem, wielkie litery", HexFormat.ofDelimiter(":").withUpperCase().formatHex(bytes));
        // WYNIK: z separatorem, wielkie litery → 01:FF:10
        show("parseHex", Arrays.toString(HexFormat.of().parseHex("0aff")));   // parse = zamień tekst na wartość
        // WYNIK: parseHex → [10, -1]
        show("toHexDigits(bajt)", HexFormat.of().toHexDigits((byte) 10));
        // WYNIK: toHexDigits(bajt) → 0a

        // PUŁAPKA: Integer.toHexString((byte) -1) daje „ffffffff”, bo bajt ze znakiem jest rozszerzany do int.
        //   Stary kod musiał pisać Integer.toHexString(b & 0xFF). HexFormat robi to poprawnie.
        show("Integer.toHexString((byte) -1)", Integer.toHexString((byte) -1));
        // WYNIK: Integer.toHexString((byte) -1) → ffffffff

        // PUŁAPKA: generator bez ziarna (RandomGenerator.getDefault(), new Random()) daje inne liczby przy każdym uruchomieniu.
        //   Nie wpisuj takich wartości do WYNIK ani do testów jednostkowych — zawsze ustaw ziarno.
        // DOBRA PRAKTYKA: metody, które potrzebują losowości, przyjmują RandomGenerator jako parametr —
        //   w produkcji dostają generator bez ziarna, w teście z ziarnem.
    }

    // =================================================================================================
    // 11. TABELA
    // =================================================================================================

    /**
     * 11. Tabela podsumowująca: wydanie → perełki. Wiersze drukujemy z listy, żeby tabela była „żywa” i sprawdzana
     * razem z resztą lekcji.
     */
    static void summaryTable() {
        section("11. Tabela: co w którym wydaniu");

        List<String[]> rows = List.of(                              // rows = wiersze
                new String[]{"Java 9", "List.of, Optional.or, takeWhile, private w interfejsie, transferTo"},
                new String[]{"Java 10", "List.copyOf, toUnmodifiableList, orElseThrow()"},
                new String[]{"Java 11", "isBlank, strip, lines, repeat, readString, Predicate.not"},
                new String[]{"Java 12", "teeing, indent, transform, CompactNumberFormat"},
                new String[]{"Java 14", "pomocne komunikaty NPE"},
                new String[]{"Java 16", "Stream.toList, mapMulti"},
                new String[]{"Java 17", "RandomGenerator, HexFormat"});
        for (String[] row : rows) {
            show(row[0], row[1]);
        }
        // WYNIK: Java 9 → List.of, Optional.or, takeWhile, private w interfejsie, transferTo
        // WYNIK: Java 10 → List.copyOf, toUnmodifiableList, orElseThrow()
        // WYNIK: Java 11 → isBlank, strip, lines, repeat, readString, Predicate.not
        // WYNIK: Java 12 → teeing, indent, transform, CompactNumberFormat
        // WYNIK: Java 14 → pomocne komunikaty NPE
        // WYNIK: Java 16 → Stream.toList, mapMulti
        // WYNIK: Java 17 → RandomGenerator, HexFormat

        // DOBRA PRAKTYKA: przy przejściu na nowszy JDK przeczytaj sekcję „API additions” w notatkach wydania —
        //   to najtańszy sposób, by dowiedzieć się o metodach, które zastępują Twoje własne narzędzia pomocnicze.
        note("Przy każdej nowej metodzie sprawdź w dokumentacji znacznik „Since” (od wersji) — czy Twój JDK ją ma.");
        // WYNIK: ℹ Przy każdej nowej metodzie sprawdź w dokumentacji znacznik „Since” (od wersji) — czy Twój JDK ją ma.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Java 9: List/Set/Map.of (niezmienne, bez null, Set/Map bez duplikatów); Optional.or / ifPresentOrElse / stream;
     *     takeWhile / dropWhile / iterate(3) / ofNullable; private metody w interfejsach; try (zmienna); transferTo.
     *   • Java 10: List.copyOf (kopia, nie widok); Collectors.toUnmodifiable*; Optional.orElseThrow().
     *   • Java 11: isBlank / strip / lines / repeat; Files.readString / writeString; Predicate.not; Optional.isEmpty;
     *     toArray(String[]::new); HttpClient; java Plik.java.
     *   • Java 12: Collectors.teeing; String.indent / transform; zwięzłe liczby (jawny Locale).
     *   • Java 14: pomocne NPE (domyślnie od 15); zmienne lokalne bez -g to <localN>.
     *   • Java 16: Stream.toList (niemodyfikowalna, dopuszcza null); mapMulti (podaj typ jawnie).
     *   • Java 17: RandomGenerator (ziarno = powtarzalność); HexFormat (bez rozszerzania znaku).
     *   • Zawsze: trim obcina tylko znaki <= spacja, strip — białe znaki Unicode (ale nie twardą spację U+00A0).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się Collections.unmodifiableList(x) od List.copyOf(x)?
     *   2. Co wypisze:  List.of(1, 2, 3, 4, 1).stream().takeWhile(n -> n < 3).toList()  ?
     *   3. ZNAJDŹ BŁĄD:  List<String> list = List.of("a", "b");  list.add("c");
     *   4. Co wypisze:  System.out.println("ab".repeat(2) + " ".isBlank());  ?
     *   5. Dlaczego Integer.toHexString((byte) -1) zwraca "ffffffff", a nie "ff"?
     *   6. ZNAJDŹ BŁĄD:  Optional<String> o = Optional.empty();  String s = o.orElseThrow(() -> "brak");
     *   7. Do czego służy ziarno (seed) generatora losowego i jak wpływa na testy?
     *   8. Czym różni się Stream.toList() od collect(Collectors.toList())?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: czyszczenie imion", List.of("Ala", "Ola"),
                () -> exercise1(List.of(" Ala ", "", "  ", "Ola")));
        Check.equal("ćw. 2: suma i liczba (teeing)", "suma=10, liczba=4", () -> exercise2(List.of(1, 2, 3, 4)));
        Check.equal("ćw. 3: takeWhile i dropWhile", List.of(List.of(1, 2, 3), List.of(-1, 4, 5)),
                () -> exercise3(List.of(1, 2, 3, -1, 4, 5)));
        Check.equal("ćw. 4: adres MAC", List.of("ff:0a", "brak"),
                () -> List.of(exercise4(Optional.of(new byte[]{(byte) 255, 10})), exercise4(Optional.empty())));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("Ala", "Ola"),
                () -> solution1(List.of(" Ala ", "", "  ", "Ola")));
        Check.equal("ćw. 2 (wzorzec)", "suma=10, liczba=4", () -> solution2(List.of(1, 2, 3, 4)));
        Check.equal("ćw. 3 (wzorzec)", List.of(List.of(1, 2, 3), List.of(-1, 4, 5)),
                () -> solution3(List.of(1, 2, 3, -1, 4, 5)));
        Check.equal("ćw. 4 (wzorzec)", List.of("ff:0a", "brak"),
                () -> List.of(solution4(Optional.of(new byte[]{(byte) 255, 10})), solution4(Optional.empty())));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ na nowe API. Z listy napisów usuń puste i składające się z samych białych znaków,
     * obetnij pozostałe i zwróć niemodyfikowalną listę.
     * <pre>{@code
     * // PRZED:
     * List<String> result = new ArrayList<>();
     * for (String s : names) { if (!s.trim().isEmpty()) { result.add(s.trim()); } }
     * return Collections.unmodifiableList(result);
     * }</pre>
     * Podpowiedź: Predicate.not(String::isBlank), String::strip, toList().
     */
    static List<String> exercise1(List<String> names) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): policz w JEDNYM przebiegu sumę i liczbę elementów i zwróć tekst „suma=S, liczba=N”.
     * Podpowiedź: Collectors.teeing(summingInt, counting, (suma, liczba) -> ...).
     */
    static String exercise2(List<Integer> numbers) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć dwie listy: pierwszą z elementów do pierwszej liczby ujemnej (bez niej),
     * drugą od tej liczby ujemnej do końca. Dla [1, 2, 3, -1, 4, 5] wynik to [[1, 2, 3], [-1, 4, 5]].
     * Podpowiedź: takeWhile(n -> n >= 0) i dropWhile(n -> n >= 0).
     */
    static List<List<Integer>> exercise3(List<Integer> numbers) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): z opcjonalnej tablicy bajtów zbuduj tekst szesnastkowy z dwukropkami, małymi literami
     * (np. „ff:0a”), a dla pustego Optional zwróć „brak”.
     * Podpowiedź: Optional.map + HexFormat.ofDelimiter(":").formatHex(bajty) + orElse.
     */
    static String exercise4(Optional<byte[]> bytes) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(List<String> names) {
        return names.stream().filter(Predicate.not(String::isBlank)).map(String::strip).toList();
    }

    static String solution2(List<Integer> numbers) {
        return numbers.stream().collect(Collectors.teeing(
                Collectors.summingInt(n -> n), Collectors.counting(),
                (sum, count) -> "suma=" + sum + ", liczba=" + count));
    }

    static List<List<Integer>> solution3(List<Integer> numbers) {
        List<Integer> before = numbers.stream().takeWhile(n -> n >= 0).toList();   // before = przed
        List<Integer> after = numbers.stream().dropWhile(n -> n >= 0).toList();    // after = po
        return List.of(before, after);
    }

    static String solution4(Optional<byte[]> bytes) {
        return bytes.map(b -> HexFormat.ofDelimiter(":").formatHex(b)).orElse("brak");
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. unmodifiableList to widok: zmiany oryginału są przez niego widoczne (tylko nie da się zmieniać przez widok).
     *      List.copyOf to niezależna, niezmienna kopia — późniejsze zmiany oryginału jej nie dotyczą.
     *   2. [1, 2] (takeWhile zatrzymuje się na pierwszej trójce i nie wraca do jedynki na końcu).
     *   3. List.of daje listę niezmienną — add rzuca UnsupportedOperationException. Poprawka: new ArrayList<>(List.of("a", "b")).
     *   4. ababtrue („ab” powtórzone dwa razy to „abab”, a napis złożony z samej spacji jest „blank”, więc
     *      isBlank() zwraca true; plus skleja tekst z wartością logiczną).
     *   5. Bajt ze znakiem (-1) jest rozszerzany do int (-1 = 0xFFFFFFFF) przed zamianą na tekst. Trzeba maskować: b & 0xFF,
     *      albo użyć HexFormat.
     *   6. orElseThrow przyjmuje Supplier wyjątku, a „brak” to String, nie wyjątek — błąd kompilacji.
     *      Poprawka: orElseThrow(() -> new NoSuchElementException("brak")).
     *   7. Ziarno ustala początek sekwencji: to samo ziarno daje te same liczby. W testach daje to powtarzalność;
     *      bez ziarna test mógłby raz przechodzić, a raz nie.
     *   8. toList() zwraca listę niemodyfikowalną i dopuszcza null; Collectors.toList() zwraca zwykle zmienną
     *      ArrayList (specyfikacja tego nie gwarantuje).
     */
    // </editor-fold>
}
