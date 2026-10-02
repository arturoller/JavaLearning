package t18_io_files;

import helpers.Check;
import helpers.TempDir;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Czytanie plików tekstowych — readString, readAllLines, lines, BufferedReader, Scanner
 *        (read = czytaj; string = napis; line = linia, wiersz; lazy = leniwy; reader = czytelnik, czytnik)
 *
 * W SKRÓCIE:
 *   Plik tekstowy to bajty, które czytamy jako znaki w wybranym kodowaniu. Java ma kilka sposobów:
 *   cały plik naraz (readString, readAllLines), linia po linii jako strumień (Files.lines)
 *   albo pętla z BufferedReader. Wybór zależy od rozmiaru pliku i od tego, co z nim robimy.
 *
 * ANALOGIA: książka w bibliotece.
 *   readString = ksero całej książki na biurko (wygodnie, ale zajmuje całe biurko).
 *   readAllLines = ksero książki pocięte na kartki-linie.
 *   Files.lines / BufferedReader = czytasz stronę po stronie, a przeczytane odkładasz (zajmujesz mało miejsca).
 *   Kodowanie to język, w którym napisano książkę: czytając polską książkę „po angielsku” dostaniesz bzdury.
 *
 * JAK TO DZIAŁA:
 *   metoda                         co zwraca            pamięć         kiedy
 *   ----------------------------   ------------------   ------------   ---------------------------------
 *   Files.readString(p)            String (cały plik)   cały plik      mały plik, potrzebny cały tekst
 *   Files.readAllLines(p)          List z liniami       cały plik      mały plik, praca na liście
 *   Files.lines(p)                 Stream linii         jedna linia    duży plik; MUSI być zamknięty
 *   Files.newBufferedReader(p)     BufferedReader       jedna linia    duży plik, własna pętla, numery linii
 *   new Scanner(p, UTF_8)          tokeny i linie       mało           krótkie dane „liczby i słowa”; wolny
 *
 *   Wszystkie metody Files.* czytające tekst bez podanego kodowania używają UTF-8, ale zawsze podawaj je jawnie
 *   w kodzie, który ma być przenośny. Wyjątek IOException jest SPRAWDZANY, bo plik może zniknąć lub być uszkodzony.
 *
 * SŁÓWKA:
 *   readString = wczytaj napis; readAllLines = wczytaj wszystkie linie; lines = linie; charset = zestaw znaków (kodowanie);
 *   token = fragment tekstu oddzielony odstępami; Scanner = skaner; malformed = źle sformowany; input = wejście.
 *
 * ZOBACZ TEŻ: t18_io_files/Io01PathFiles (ścieżki i pliki), t18_io_files/Io03WritingText (zapis),
 *   t18_io_files/Io12Charsets (kodowania), t16_streams/Streams01Intro (strumienie)
 * </pre>
 */
public class Io02ReadingText {

    /** Tekst próbny. Zapisujemy go z jawnym "\n", więc plik wygląda tak samo w Windows i na Linuksie. */
    private static final String TEXT =
            "Ala ma kota\nZażółć gęślą jaźń\n\nJava 17 jest świetna\nkot, pies i kot\n";

    public static void main(String[] args) throws IOException {
        title("Io02 — czytanie plików tekstowych");

        Path dir = TempDir.create("io02"); // create = utwórz
        try {
            readString(dir);              // readString = wczytaj napis
            readAllLines(dir);            // readAllLines = wczytaj wszystkie linie
            lazyLines(dir);               // lazy lines = leniwe linie (Files.lines)
            bufferedReaderLoop(dir);      // buffered reader loop = pętla z BufferedReader
            countingAndSearching(dir);    // counting and searching = liczenie i szukanie
            whichToChoose(dir);           // which to choose = co wybrać
            wrongCharset(dir);            // wrong charset = złe kodowanie
            scannerBriefly(dir);          // scanner briefly = Scanner w skrócie
            exercises();                  // exercises = ćwiczenia
        } finally {
            TempDir.deleteRecursively(dir);
        }
    }

    // =================================================================================================
    // Pomocnicze
    // =================================================================================================

    /** slash = ukośnik. Windows pisze "\" w ścieżkach, my zawsze "/", żeby wydruk był taki sam wszędzie. */
    private static String slash(Path p) {
        return p.toString().replace('\\', '/');
    }

    @FunctionalInterface
    private interface ThrowingIo {
        void run() throws IOException;
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }

    /**
     * ioFails = „IO zawodzi”. Oczekuje wyjątku IO i wypisuje jego NAZWĘ (nie komunikat: komunikaty IO zawierają
     * pełną ścieżkę i tekst zależny od systemu, więc różniłyby się w Windows i na Linuksie).
     */
    private static void ioFails(String label, ThrowingIo action) {
        try {
            action.run();
            System.out.println("✘ " + label + " → NIE rzucono wyjątku (a spodziewaliśmy się go)");
        } catch (IOException e) {
            String file = "";
            if (e instanceof FileSystemException fse && fse.getFile() != null) { // instanceof ze wzorcem (Java 16+)
                file = " (plik: " + Path.of(fse.getFile()).getFileName() + ")";
            }
            System.out.println("✔ " + label + " → rzucono " + e.getClass().getSimpleName() + file);
        }
    }

    /** io = opakowuje IOException w UncheckedIOException, żeby pasowało do Supplier w Check.equal. */
    private static <T> Supplier<T> io(IoSupplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    // =================================================================================================
    // 1. Files.readString
    // =================================================================================================

    /**
     * 1. Files.readString (Java 11+) wczytuje CAŁY plik do jednego napisu. Najprostsze, gdy plik jest mały.
     */
    static void readString(Path base) throws IOException {
        section("1. Files.readString — cały plik w jednym napisie");

        Path file = base.resolve("notatki.txt");
        Files.writeString(file, TEXT); // writeString = zapisz napis (Java 11+); szczegóły w Io03WritingText

        String all = Files.readString(file, StandardCharsets.UTF_8); // StandardCharsets.UTF_8 = kodowanie UTF-8
        show("liczba znaków", all.length()); // length = długość
        // WYNIK: liczba znaków → 68
        show("rozmiar pliku w bajtach", Files.size(file)); // size = rozmiar
        // WYNIK: rozmiar pliku w bajtach → 78
        note("Bajtów jest więcej niż znaków: w UTF-8 polskie litery (ą, ę, ł, ż...) zajmują po 2 bajty.");
        // WYNIK: ℹ Bajtów jest więcej niż znaków: w UTF-8 polskie litery (ą, ę, ł, ż...) zajmują po 2 bajty.
        show("zaczyna się od", all.substring(0, 11)); // substring = podnapis
        // WYNIK: zaczyna się od → Ala ma kota
        show("kończy się znakiem nowej linii", all.endsWith("\n")); // endsWith = kończy się na
        // WYNIK: kończy się znakiem nowej linii → true

        // PUŁAPKA: readString czyta wszystko do pamięci. Plik wielkości gigabajtów da OutOfMemoryError
        //   (a napis w Javie nie może mieć więcej niż ok. 2 miliardów znaków). Dlaczego to groźne: błąd
        //   wychodzi dopiero na produkcji, gdy ktoś poda duży plik. Dla dużych plików użyj Files.lines.
        // PUŁAPKA: brak pliku to NoSuchFileException (podklasa IOException), nie pusty napis:
        ioFails("readString nieistniejącego pliku", () -> Files.readString(base.resolve("nie-ma.txt"), StandardCharsets.UTF_8));
        // WYNIK: ✔ readString nieistniejącego pliku → rzucono NoSuchFileException (plik: nie-ma.txt)

        // DOBRA PRAKTYKA: podawaj kodowanie jawnie, choć Files.readString bez niego też czyta UTF-8. Dlaczego: kod
        //   mówi wprost, czego oczekuje, a zmiana kodowania to jedno miejsce.
    }

    // =================================================================================================
    // 2. Files.readAllLines
    // =================================================================================================

    /**
     * 2. Files.readAllLines wczytuje cały plik jako listę linii. Znaki końca linii (\n, \r\n i \r) są ucinane,
     * więc plik z Windows (\r\n) i z Linuksa (\n) dają tę samą listę.
     */
    static void readAllLines(Path base) throws IOException {
        section("2. Files.readAllLines — lista linii");

        Path file = base.resolve("notatki.txt");
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8); // List<String> = lista napisów
        show("liczba linii", lines.size()); // size = rozmiar (liczba elementów)
        // WYNIK: liczba linii → 5
        show("linia 2", lines.get(1)); // get = pobierz element (numeracja od zera)
        // WYNIK: linia 2 → Zażółć gęślą jaźń
        show("linia 3 (pusta)", "[" + lines.get(2) + "]");
        // WYNIK: linia 3 (pusta) → []
        show("ostatnia", lines.get(lines.size() - 1));
        // WYNIK: ostatnia → kot, pies i kot
        note("Końcowy \\n nie tworzy dodatkowej, pustej linii na końcu listy.");
        // WYNIK: ℹ Końcowy \n nie tworzy dodatkowej, pustej linii na końcu listy.

        // Różne końce linii: Windows pisze "\r\n" (CR+LF), Linux "\n". Plik zapisany w Windows, czytany na Linuksie
        // (albo odwrotnie), nie może Cię zaskoczyć:
        Path windowsStyle = base.resolve("windows.txt");
        Files.writeString(windowsStyle, "a\r\nb\r\n\r\nc");
        show("readAllLines", Files.readAllLines(windowsStyle, StandardCharsets.UTF_8));
        // WYNIK: readAllLines → [a, b, , c]
        String raw = Files.readString(windowsStyle, StandardCharsets.UTF_8);
        show("readString: długość", raw.length());
        // WYNIK: readString: długość → 9
        show("readString: zawiera \\r", raw.contains("\r")); // contains = zawiera
        // WYNIK: readString: zawiera \r → true
        // PUŁAPKA: readString zostawia znaki końca linii w napisie. Porównanie z "a\nb" po wczytaniu pliku
        //   z Windows się nie powiedzie. Dzieląc napis samemu, rób to przez raw.lines() (Java 11+; rozpoznaje
        //   \n, \r\n i \r) albo split("\\R") — nigdy split("\n").
        show("raw.lines().toList()", raw.lines().toList()); // lines = linie napisu; toList = do listy (Java 16+)
        // WYNIK: raw.lines().toList() → [a, b, , c]

    }

    // =================================================================================================
    // 3. Files.lines — leniwy strumień
    // =================================================================================================

    /**
     * 3. Files.lines zwraca Stream linii. Jest LENIWY: czyta plik w miarę potrzeby, więc pasuje do dużych plików.
     * Ale trzyma otwarty plik, więc MUSI być zamknięty — najlepiej przez try-with-resources.
     */
    static void lazyLines(Path base) throws IOException {
        section("3. Files.lines — leniwy strumień, który trzeba zamknąć");

        Path file = base.resolve("notatki.txt");

        // Strumień jest zasobem (AutoCloseable), więc try-with-resources zamknie go, nawet gdy poleci wyjątek.
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) { // Stream = strumień (t16)
            List<String> withKot = lines
                    .filter(line -> line.contains("kot")) // filter = zostaw tylko pasujące
                    .toList();
            show("linie z \"kot\"", withKot);
            // WYNIK: linie z "kot" → [Ala ma kota, kot, pies i kot]
        }

        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            long nonBlank = lines.filter(line -> !line.isBlank()).count(); // isBlank = pusty lub same spacje (Java 11+)
            show("niepuste linie", nonBlank);
            // WYNIK: niepuste linie → 4
        }

        // Leniwość: limit(1) przerywa czytanie po pierwszej linii — reszta pliku nie jest nawet wczytywana.
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            show("pierwsza linia", lines.findFirst().orElse("(brak)")); // findFirst = pierwszy; orElse = albo
            // WYNIK: pierwsza linia → Ala ma kota
        }

        // Numery linii: strumień ich nie zna — użyj pętli z BufferedReader (następna sekcja).

        // PUŁAPKA: strumień niezamknięty = otwarty plik. Na Linuksie „tylko” wycieka uchwyt (po tysiącach pętli
        //   kończą się deskryptory), a w Windows otwarty plik często blokuje usunięcie lub przeniesienie (jego albo całego katalogu).
        //   Zły kod:    long n = Files.lines(p).count();            // nikt nie zamyka strumienia
        //   Dobry kod:  try (Stream<String> s = Files.lines(p)) { n = s.count(); }
        // PUŁAPKA: błąd IO w trakcie przetwarzania strumienia (np. zły bajt w UTF-8) NIE jest wyjątkiem sprawdzanym
        //   — leci UncheckedIOException (niesprawdzany opakowany IOException; t10_exceptions/Exceptions06ChainingWrapping),
        //   bo operacje strumienia nie mogą deklarować throws IOException. Przyczynę znajdziesz w getCause().
        //   Przykład na końcu lekcji (sekcja 7).

    }

    // =================================================================================================
    // 4. BufferedReader i pętla readLine
    // =================================================================================================

    /**
     * 4. Klasyczna pętla: czytaj linię, dopóki readLine nie zwróci null. Daje pełną kontrolę (numery linii,
     * przerwanie w środku) i nie trzyma całego pliku w pamięci.
     */
    static void bufferedReaderLoop(Path base) throws IOException {
        section("4. BufferedReader i pętla readLine");

        Path file = base.resolve("notatki.txt");

        // PRZED (stary styl): ręczne zamykanie w finally, bez podanego kodowania — dwa błędy naraz.
        //   BufferedReader reader = null;
        //   try {
        //       reader = new BufferedReader(new FileReader("notatki.txt"));   // kodowanie domyślne systemu!
        //       String line;
        //       while ((line = reader.readLine()) != null) { ... }
        //   } finally {
        //       if (reader != null) { reader.close(); }                       // close też może rzucić wyjątek
        //   }
        //
        // PO (nowy styl): newBufferedReader z kodowaniem + try-with-resources (zamknie sam, nawet po wyjątku).
        List<String> numbered = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { // BufferedReader = czytnik z buforem
            String line;
            int lineNo = 0; // lineNo = numer linii
            while ((line = reader.readLine()) != null) { // readLine = wczytaj linię; null = koniec pliku
                lineNo++;
                if (!line.isEmpty()) { // isEmpty = jest pusty
                    numbered.add(lineNo + ": " + line);
                }
            }
        }
        showEach("niepuste linie z numerami", numbered); // showEach = pokaż każdy element
        // WYNIK: niepuste linie z numerami (liczba elementów: 4):
        // WYNIK: • 1: Ala ma kota
        // WYNIK: • 2: Zażółć gęślą jaźń
        // WYNIK: • 4: Java 17 jest świetna
        // WYNIK: • 5: kot, pies i kot

        // Bufor: BufferedReader wczytuje dane z dysku większymi porcjami (domyślnie 8192 znaki), a readLine oddaje z
        // nich kolejne linie — nie pytamy systemu o każdy znak (to wolne).
        // readLine NIE zwraca znaku końca linii. Pustą linię w środku pliku zwraca jako "" (nie null!);
        // null oznacza dopiero koniec pliku.

        // PUŁAPKA: new FileReader(plik) i new InputStreamReader(strumień) BEZ kodowania używają kodowania domyślnego
        //   systemu: w Javie 17 na polskim Windows to windows-1250, na Linuksie zwykle UTF-8. Plik UTF-8 z „ł”
        //   przeczytasz więc raz dobrze, raz jako krzaczki. Java 18+ domyślnie używa UTF-8 (JEP 400), ale kod ma
        //   działać na Javie 17 — podawaj kodowanie. Wersja z kodowaniem to FileReader(plik, UTF_8) (Java 11+).
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile(), StandardCharsets.UTF_8))) {
            show("FileReader z kodowaniem — pierwsza linia", reader.readLine());
            // WYNIK: FileReader z kodowaniem — pierwsza linia → Ala ma kota
        }

        // DOBRA PRAKTYKA: Files.newBufferedReader(path, UTF_8) zamiast new BufferedReader(new FileReader(...)):
        //   krócej, kodowanie jawne, a brak pliku daje NoSuchFileException z nazwą pliku (a nie FileNotFoundException).
    }

    // =================================================================================================
    // 5. LICZENIE I SZUKANIE
    // =================================================================================================

    /**
     * 5. Typowe zadania: policz linie i słowa, znajdź linie z frazą, zlicz częstość słów. Duży plik (20 000 linii)
     * czytamy bez wczytywania go w całości.
     */
    static void countingAndSearching(Path base) throws IOException {
        section("5. Liczenie i szukanie");

        Path file = base.resolve("notatki.txt");

        long lineCount;
        long wordCount = 0; // wordCount = liczba słów
        Map<String, Integer> frequency = new TreeMap<>(); // TreeMap = mapa posortowana po kluczach; frequency = częstość
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            lineCount = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                // split = podziel; \\P{L}+ = jeden lub więcej znaków, które NIE są literami (spacje, przecinki, cyfry)
                for (String word : line.toLowerCase(Locale.ROOT).split("\\P{L}+")) { // toLowerCase = małe litery
                    if (!word.isEmpty()) { // przy pustej linii albo separatorze na początku split daje ""
                        wordCount++;
                        frequency.merge(word, 1, Integer::sum); // merge = dodaj lub połącz z istniejącą wartością
                    }
                }
            }
        }
        show("linie", lineCount);
        // WYNIK: linie → 5
        show("słowa (same litery)", wordCount);
        // WYNIK: słowa (same litery) → 13
        show("częstość \"kot\"", frequency.get("kot")); // get = pobierz wartość dla klucza
        // WYNIK: częstość "kot" → 2
        show("częstość \"java\"", frequency.get("java"));
        // WYNIK: częstość "java" → 1
        // Dlaczego split("\\P{L}+"), a nie split(" ")? Przecinki i kropki zostałyby przyklejone do słów ("kot," ≠ "kot"),
        // a podwójna spacja dałaby puste „słowo”. \p{L} to dowolna litera Unicode (także ą, ę, ł), \P{L} — nie-litera.
        // To nie to samo co [a-z]: ten zakres nie zna polskich liter.

        // Szukanie linii z numerem (Files.lines nie zna numerów, więc czytamy listę):
        List<String> all = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> found = new ArrayList<>(); // found = znalezione
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).contains("Java")) {
                found.add("linia " + (i + 1) + ": " + all.get(i)); // numeracja linii dla ludzi zaczyna się od 1
            }
        }
        show("linie z \"Java\"", found);
        // WYNIK: linie z "Java" → [linia 4: Java 17 jest świetna]

        // Duży plik: 20 000 linii "linia 1" ... "linia 20000". Ani readString, ani readAllLines nie są tu potrzebne.
        Path big = base.resolve("duzy.txt");
        StringBuilder sb = new StringBuilder(); // StringBuilder = budowniczy napisu (szybsze sklejanie)
        for (int i = 1; i <= 20_000; i++) {
            sb.append("linia ").append(i).append('\n'); // append = dopisz
        }
        Files.writeString(big, sb.toString());

        long sum = 0;
        long lines = 0;
        try (BufferedReader reader = Files.newBufferedReader(big, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) { // w pamięci jest naraz jedna linia (plus bufor)
                lines++;
                sum += Long.parseLong(line.substring("linia ".length())); // parseLong = zamień napis na liczbę long
            }
        }
        show("duży plik: linie", lines);
        // WYNIK: duży plik: linie → 20000
        show("duży plik: suma numerów", sum);
        // WYNIK: duży plik: suma numerów → 200010000
        // DOBRA PRAKTYKA: przetwarzając plik „w locie” trzymaj w pamięci tylko wynik (licznik, mapę), nie wszystkie
        //   linie. Dlaczego: zużycie pamięci nie zależy wtedy od rozmiaru pliku.
    }

    // =================================================================================================
    // 6. CO WYBRAĆ
    // =================================================================================================

    /**
     * 6. Trzy sposoby, ten sam wynik, różne koszty. Zasada: mały plik = wygoda, duży plik = strumieniowo.
     */
    static void whichToChoose(Path base) throws IOException {
        section("6. Co wybrać? Pamięć kontra wygoda");

        Path big = base.resolve("duzy.txt");

        long a = Files.readString(big, StandardCharsets.UTF_8).lines().count();
        long b = Files.readAllLines(big, StandardCharsets.UTF_8).size();
        long c;
        try (Stream<String> stream = Files.lines(big, StandardCharsets.UTF_8)) {
            c = stream.count();
        }
        show("readString + lines()", a);
        // WYNIK: readString + lines() → 20000
        show("readAllLines().size()", b);
        // WYNIK: readAllLines().size() → 20000
        show("Files.lines().count()", c);
        // WYNIK: Files.lines().count() → 20000

        // Reguła kciuka:
        //   • plik konfiguracyjny, szablon, mały CSV (do kilku MB)  → readString / readAllLines (wygoda),
        //   • log, eksport z bazy, nieznany rozmiar                  → Files.lines albo BufferedReader (stała pamięć),
        //   • potrzebne numery linii, przerwanie w środku, parsowanie → BufferedReader + readLine.

        // Zasoby z classpath (pliki w jar, np. application.properties) nie są zwykłymi plikami na dysku, więc Path.of ich
        // nie znajdzie: używa się Klasa.class.getResourceAsStream("/nazwa") (resource = zasób) i InputStreamReader z UTF_8.
        // Spring robi to za Ciebie (ClassPathResource, @Value) — zobacz kurs SpringLearning.
    }

    // =================================================================================================
    // 7. ZŁE KODOWANIE
    // =================================================================================================

    /**
     * 7. Plik zapisany w innym kodowaniu niż to, którym go czytamy. Polskie litery w windows-1250 i ISO-8859-2
     * to bajty, które w UTF-8 są niepoprawne — Java to zauważa i rzuca MalformedInputException (zły bajt).
     */
    static void wrongCharset(Path base) throws IOException {
        section("7. Złe kodowanie — MalformedInputException");

        Charset windows1250 = Charset.forName("windows-1250"); // Charset.forName = kodowanie o podanej nazwie
        Path legacy = base.resolve("stary.txt"); // legacy = pozostałość po starszych czasach
        Files.write(legacy, "zażółć\nkot\n".getBytes(windows1250)); // getBytes = zamień napis na bajty w danym kodowaniu

        ioFails("readString(UTF_8) pliku windows-1250", () -> Files.readString(legacy, StandardCharsets.UTF_8));
        // WYNIK: ✔ readString(UTF_8) pliku windows-1250 → rzucono MalformedInputException
        ioFails("readAllLines(UTF_8) pliku windows-1250", () -> Files.readAllLines(legacy, StandardCharsets.UTF_8));
        // WYNIK: ✔ readAllLines(UTF_8) pliku windows-1250 → rzucono MalformedInputException
        ioFails("newBufferedReader(UTF_8) i readLine", () -> {
            try (BufferedReader reader = Files.newBufferedReader(legacy, StandardCharsets.UTF_8)) {
                reader.readLine();
            }
        });
        // WYNIK: ✔ newBufferedReader(UTF_8) i readLine → rzucono MalformedInputException

        // Files.lines: błąd wychodzi dopiero podczas pracy strumienia, jako niesprawdzany UncheckedIOException.
        try (Stream<String> lines = Files.lines(legacy, StandardCharsets.UTF_8)) {
            lines.count();
        } catch (UncheckedIOException e) { // Unchecked = niesprawdzany
            System.out.println("✔ Files.lines → rzucono " + e.getClass().getSimpleName()
                    + ", przyczyna: " + e.getCause().getClass().getSimpleName()); // getCause = pobierz przyczynę
            // WYNIK: ✔ Files.lines → rzucono UncheckedIOException, przyczyna: MalformedInputException
        }

        // Rozwiązanie: znasz kodowanie pliku — podaj je.
        show("windows-1250 czytany jako windows-1250", Files.readString(legacy, windows1250).lines().findFirst().orElse(""));
        // WYNIK: windows-1250 czytany jako windows-1250 → zażółć

        // Gorzej, gdy źle dobrane kodowanie NIE rzuca błędu. ISO-8859-1 przyjmuje KAŻDY bajt, więc UTF-8 przeczytane
        // jako ISO-8859-1 „działa”, tylko daje krzaczki (mojibake): każda polska litera to dwa dziwne znaki.
        Path utf8 = base.resolve("utf8.txt");
        Files.writeString(utf8, "zażółć", StandardCharsets.UTF_8);
        String garbled = Files.readString(utf8, StandardCharsets.ISO_8859_1); // garbled = zniekształcony
        show("UTF-8 czytany jako ISO-8859-1: znaków zamiast 6", garbled.length());
        // WYNIK: UTF-8 czytany jako ISO-8859-1: znaków zamiast 6 → 10
        // (10 = 2 litery ASCII + 4 polskie litery po 2 bajty: każdy bajt stał się jednym znakiem.)

        // PUŁAPKA: nie wszystkie klasy zgłaszają zły bajt! FileReader, InputStreamReader i Scanner po cichu zastępują
        //   niepoprawny bajt znakiem zastępczym U+FFFD (czarny romb z pytajnikiem) i czytają dalej. Błąd
        //   wychodzi dopiero u użytkownika. Files.readString / readAllLines / newBufferedReader / lines są
        //   „głośne” i dlatego bezpieczniejsze. Szczegóły o kodowaniach: t18_io_files/Io12Charsets.
        try (Scanner scanner = new Scanner(legacy, StandardCharsets.UTF_8)) { // Scanner(Path, Charset) — Java 10+
            String line = scanner.nextLine();
            show("Scanner: czy w linii jest znak zastępczy U+FFFD", line.indexOf(0xFFFD) >= 0); // indexOf = pozycja znaku (-1 = brak)
            // WYNIK: Scanner: czy w linii jest znak zastępczy U+FFFD → true
        }
        // DOBRA PRAKTYKA: gdy pliki przychodzą z zewnątrz (eksport z Excela, stary system), ustal ich kodowanie
        //   z góry i zapisz je w dokumentacji. Polskie Windows i Excel często dają windows-1250 albo UTF-8 z BOM.
    }

    // =================================================================================================
    // 8. SCANNER
    // =================================================================================================

    /**
     * 8. Scanner dzieli tekst na tokeny (fragmenty oddzielone odstępami) i zamienia je na liczby. Dobry do
     * krótkich danych (ćwiczenia, zadania z liczbami), zły do dużych plików i do poważnych programów.
     */
    static void scannerBriefly(Path base) throws IOException {
        section("8. Scanner — tylko do prostych rzeczy");

        Path numbers = base.resolve("liczby.txt");
        Files.writeString(numbers, "10 20\n30 abc\n40\n");

        long sum = 0;
        String stoppedAt;
        try (Scanner scanner = new Scanner(numbers, StandardCharsets.UTF_8)) { // Scanner = skaner (czytnik tokenów)
            while (scanner.hasNextInt()) { // hasNextInt = czy następny token jest liczbą całkowitą
                sum += scanner.nextInt(); // nextInt = wczytaj liczbę
            }
            stoppedAt = scanner.next(); // next = wczytaj następny token (napis)
            show("ioException() po odczycie", scanner.ioException()); // ioException = ostatni połknięty wyjątek IO
            // WYNIK: ioException() po odczycie → null
        }
        show("suma liczb do pierwszego nie-liczbowego tokenu", sum);
        // WYNIK: suma liczb do pierwszego nie-liczbowego tokenu → 60
        show("zatrzymał się na", stoppedAt);
        // WYNIK: zatrzymał się na → abc

        // PUŁAPKA: nextInt() NIE zjada końca linii. Następne nextLine() zwraca więc resztę bieżącej linii — pustą.
        Path mixed = base.resolve("mieszany.txt");
        Files.writeString(mixed, "10\nkot\n");
        try (Scanner scanner = new Scanner(mixed, StandardCharsets.UTF_8)) {
            int number = scanner.nextInt();
            String rest = scanner.nextLine(); // rest = reszta
            String next = scanner.nextLine();
            show("nextInt", number);
            // WYNIK: nextInt → 10
            show("nextLine zaraz po nextInt", "[" + rest + "]");
            // WYNIK: nextLine zaraz po nextInt → []
            show("następne nextLine", next);
            // WYNIK: następne nextLine → kot
        }

        // PUŁAPKA: liczby zmiennoprzecinkowe czyta według Locale (ustawień językowych). W polskim "3,14", w angielskim
        //   "3.14". Program działa u Ciebie, a na serwerze z innym językiem nie. Ustaw język jawnie.
        Path decimals = base.resolve("ulamki.txt");
        Files.writeString(decimals, "3,14 2.5\n");
        try (Scanner scanner = new Scanner(decimals, StandardCharsets.UTF_8)) {
            scanner.useLocale(Locale.forLanguageTag("pl-PL")); // useLocale = użyj ustawień językowych
            double first = scanner.nextDouble(); // nextDouble = wczytaj liczbę zmiennoprzecinkową
            show("pl-PL: \"3,14\" daje", first);
            // WYNIK: pl-PL: "3,14" daje → 3.14
            show("pl-PL: czy \"2.5\" jest liczbą", scanner.hasNextDouble());
            // WYNIK: pl-PL: czy "2.5" jest liczbą → false
        }
        try (Scanner scanner = new Scanner(decimals, StandardCharsets.UTF_8)) {
            scanner.useLocale(Locale.ROOT); // ROOT = neutralne ustawienia (kropka dziesiętna)
            show("ROOT: czy \"3,14\" jest liczbą", scanner.hasNextDouble());
            // WYNIK: ROOT: czy "3,14" jest liczbą → false
        }

        // Kiedy NIE używać Scannera:
        //   • duże pliki: Scanner opiera się na wyrażeniach regularnych i jest wielokrotnie wolniejszy od BufferedReader,
        //   • błędy IO: Scanner nie rzuca IOException z odczytu — połyka je. Po przedwczesnym końcu danych hasNext() zwraca
        //     po prostu false i nie wiesz, czy plik się skończył, czy dysk zawiódł (ślad jest tylko w ioException()),
        //   • kodowanie: złe bajty są po cichu zamieniane na U+FFFD (patrz sekcja 7).
        // Do prostych liczb i słów z krótkiego pliku, albo wejścia z klawiatury (new Scanner(System.in)), jest w porządku.

        // DOBRA PRAKTYKA: czytając plik z danymi użyj BufferedReader + split + parsowanie (tak jak w Io04Csv).
        //   Dlaczego: zgłasza błędy, jest szybki i nie zależy od Locale.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • readString (Java 11+) = cały plik w napisie; readAllLines = lista linii; oba trzymają CAŁY plik w pamięci.
     *   • Files.lines = leniwy strumień linii, który trzeba zamknąć (try-with-resources); błąd IO w środku
     *     to UncheckedIOException.
     *   • BufferedReader + readLine: null = koniec pliku; "" = pusta linia; daje numery linii i pełną kontrolę.
     *   • readAllLines i lines() ucinają \n, \r\n i \r; readString zostawia je w napisie.
     *   • Duży plik: licz „w locie” (licznik, mapa częstości), nie trzymaj linii w pamięci.
     *   • Kodowanie podawaj ZAWSZE jawnie (UTF_8); FileReader/Scanner/InputStreamReader bez kodowania biorą domyślne.
     *   • Zły bajt: Files.* rzuca MalformedInputException (lines: UncheckedIOException), Scanner i FileReader
     *     zamieniają go po cichu na U+FFFD.
     *   • Scanner: nextInt nie zjada końca linii; liczby wg Locale; wolny; połyka IOException (ioException()).
     *
     * PYTANIA KONTROLNE:
     *   1. Którą metodę wybierzesz dla pliku 3 GB, w którym szukasz linii ze słowem „ERROR”, i dlaczego?
     *   2. Co zwraca readLine na końcu pliku, a co dla pustej linii w środku?
     *   3. Co wypisze:  Files.writeString(p, "a\nb\n");  System.out.println(Files.readAllLines(p).size());  ?
     *   4. Co wypisze:  Files.writeString(p, "a\r\nb");  System.out.println(Files.readString(p).length());  ?
     *   5. ZNAJDŹ BŁĄD:  long n = Files.lines(Path.of("log.txt")).count();  — co jest nie tak z zasobem?
     *   6. ZNAJDŹ BŁĄD:  Scanner s = new Scanner(System.in);  int x = s.nextInt();  String name = s.nextLine();
     *      — wpisano „5” Enter „Ola” Enter. Co dostanie name?
     *   7. Dlaczego plik windows-1250 z polskimi literami czytany przez Files.readString(p, UTF_8) rzuca wyjątek,
     *      a przez Scannera nie? Które zachowanie jest lepsze?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        runExercises(false);

        section("ĆWICZENIA — rozwiązania wzorcowe");
        runExercises(true);
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /** Każde uruchomienie ma własny, świeży katalog tymczasowy; reference = czy sprawdzamy rozwiązania wzorcowe. */
    private static void runExercises(boolean reference) {
        Path dir = TempDir.create("io02cw");
        try {
            Path text = dir.resolve("tekst.txt");
            Path legacy = dir.resolve("stary.txt");
            Files.writeString(text, "Ala ma kota\n\nJava 17 jest świetna\nKot i pies\nkot, kot, kot\n");
            Files.write(legacy, "zażółć gęślą jaźń\nkot\n".getBytes(Charset.forName("windows-1250")));

            Check.equal("ćw. 1: liczba linii", 5L, io(() -> reference ? solution1(text) : exercise1(text)));
            Check.equal("ćw. 2: najdłuższa linia", "Java 17 jest świetna", io(() -> reference ? solution2(text) : exercise2(text)));
            Check.equal("ćw. 3: niepuste linie (PRZEPISZ)", 4, io(() -> reference ? solution3(text) : exercise3(text)));
            Check.equal("ćw. 4: grep z numerami", List.of("1: Ala ma kota", "4: Kot i pies", "5: kot, kot, kot"),
                    io(() -> reference ? solution4(text, "kot") : exercise4(text, "kot")));
            Check.equal("ćw. 5: plik windows-1250", "zażółć gęślą jaźń", io(() -> reference ? solution5(legacy) : exercise5(legacy)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            TempDir.deleteRecursively(dir);
        }
        Check.summary();
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć liczbę linii pliku (puste linie też się liczą).
     * Podpowiedź: Files.readAllLines(path, UTF_8).size() — plik jest mały.
     */
    static long exercise1(Path file) throws IOException {
        // TODO: twoje rozwiązanie
        return -1L;
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć najdłuższą linię pliku (pierwszą, gdy kilka ma tę samą długość).
     * Użyj Files.lines w try-with-resources.
     * Podpowiedź: stream.max(Comparator.comparingInt(String::length)) — Optional, więc orElse("").
     */
    static String exercise2(Path file) throws IOException {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ ze starego stylu na nowy. Stary kod liczy niepuste linie (bez spacji):
     * <pre>{@code
     * FileReader fr = new FileReader(path);          // kodowanie domyślne systemu!
     * BufferedReader br = new BufferedReader(fr);
     * int count = 0;
     * String line;
     * while ((line = br.readLine()) != null) {
     *     if (!line.isBlank()) { count++; }
     * }
     * br.close();                                   // nie zamknie się, gdy wyżej poleci wyjątek
     * return count;
     * }</pre>
     * Nowa wersja: Files.newBufferedReader z UTF_8 i try-with-resources. Zwróć int.
     */
    static int exercise3(Path file) throws IOException {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 4 (średnie): zwróć linie zawierające słowo word (BEZ względu na wielkość liter) w postaci
     * "numer: linia", numeracja od 1, np. "1: Ala ma kota".
     * Podpowiedź: BufferedReader + licznik; porównuj line.toLowerCase(Locale.ROOT).contains(word.toLowerCase(Locale.ROOT)).
     */
    static List<String> exercise4(Path file, String word) throws IOException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): plik został zapisany w kodowaniu windows-1250 (jak starsze programy w Windows).
     * Wczytaj go poprawnie i zwróć PIERWSZĄ linię (z polskimi literami).
     * Podpowiedź: Charset.forName("windows-1250") i Files.readAllLines(path, charset).
     */
    static String exercise5(Path file) throws IOException {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static long solution1(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8).size();
    }

    static String solution2(Path file) throws IOException {
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines.max(java.util.Comparator.comparingInt(String::length)).orElse("");
        }
    }

    static int solution3(Path file) throws IOException {
        int count = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    count++;
                }
            }
        }
        return count;
    }

    static List<String> solution4(Path file, String word) throws IOException {
        List<String> result = new ArrayList<>();
        String needle = word.toLowerCase(Locale.ROOT);
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.toLowerCase(Locale.ROOT).contains(needle)) {
                    result.add(lineNo + ": " + line);
                }
            }
        }
        return result;
    }

    static String solution5(Path file) throws IOException {
        return Files.readAllLines(file, Charset.forName("windows-1250")).get(0);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Files.lines (albo BufferedReader + readLine) w try-with-resources: czyta linia po linii i trzyma w pamięci
     *      jedną linię. readString/readAllLines wczytałyby 3 GB do pamięci (OutOfMemoryError).
     *   2. Na końcu pliku readLine zwraca null; dla pustej linii w środku zwraca "" (pusty napis).
     *   3. 2 — końcowy "\n" nie tworzy dodatkowej, pustej linii (lista to [a, b]).
     *   4. 4 — napis to a, \r, \n, b; readString zostawia \r\n (w odróżnieniu od readAllLines).
     *   5. Strumień z Files.lines nie jest zamknięty: wyciek uchwytu pliku, a w Windows blokada pliku. Trzeba
     *      try-with-resources (i podać UTF_8; kompilator wymusi też obsługę IOException).
     *   6. Pusty napis: nextInt zostawia w wejściu koniec linii po „5”, więc nextLine zwraca resztę tej linii — "".
     *      Poprawka: dodatkowe scanner.nextLine() po nextInt albo czytanie wszystkiego przez nextLine i Integer.parseInt.
     *   7. Bajty windows-1250 (np. ż = BF) nie są poprawnym UTF-8. Files.readString zgłasza to wyjątkiem
     *      MalformedInputException, a Scanner po cichu zamienia je na U+FFFD. Lepszy jest wyjątek: błąd widać od razu,
     *      zamiast cicho zepsutych danych.
     */
    // </editor-fold>
}
