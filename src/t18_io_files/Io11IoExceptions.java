package t18_io_files;

import helpers.Check;
import helpers.TempDir;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wyjątki wejścia-wyjścia — co może pójść nie tak z plikiem i jak to obsłużyć
 *        (exception = wyjątek; IOException = wyjątek wejścia-wyjścia; suppressed = stłumiony, dopisany do głównego;
 *         retry = ponowienie próby; swallow = „połknąć” wyjątek, czyli zignorować)
 *
 * W SKRÓCIE:
 *   Operacje na plikach zawodzą z powodów spoza programu: pliku nie ma, ktoś go usunął, dysk jest pełny, bajty mają
 *   złe kodowanie. Dlatego IOException jest wyjątkiem SPRAWDZANYM: kompilator zmusza do decyzji. W tej lekcji
 *   poznajesz rodzinę tych wyjątków, uczysz się je odróżniać i obsługiwać we właściwym miejscu: nisko — rzucasz,
 *   wysoko — jeden komunikat dla użytkownika i wpis do logu.
 *
 * ANALOGIA: awaria w kuchni restauracji.
 *   Kucharz (niski poziom) nie tłumaczy gościom, że zabrakło mąki — woła kierownika sali (rzuca wyjątek). Kierownik
 *   dopisuje kontekst („zamówienie 17, stolik 4”) i przekazuje wyżej. Dopiero szef sali (najwyższy poziom) mówi
 *   gościowi jedno uprzejme zdanie, a w książce zapisuje, co się stało. Kto udaje, że nic się nie stało
 *   (pusty catch), ten serwuje gościom danie bez mąki.
 *
 * JAK TO DZIAŁA:
 *   Throwable
 *    └ Exception
 *       └ IOException  (sprawdzany)
 *          ├ FileSystemException  (wyjątki NIO.2, mają getFile())
 *          │   ├ NoSuchFileException, FileAlreadyExistsException, DirectoryNotEmptyException,
 *          │   ├ NotDirectoryException, AccessDeniedException ...
 *          ├ FileNotFoundException (stare java.io: FileReader, FileInputStream)
 *          ├ EOFException (koniec danych w środku odczytu: DataInputStream.readInt)
 *          └ CharacterCodingException → MalformedInputException (bajty niezgodne z kodowaniem)
 *   RuntimeException
 *    └ UncheckedIOException  (opakowuje IOException tam, gdzie sprawdzany wyjątek się nie mieści: lambdy, strumienie)
 *
 *   Reguły:
 *     • catch od NAJBARDZIEJ szczegółowego do ogólnego (NoSuchFileException przed FileSystemException przed IOException),
 *     • try-with-resources zamyka zasoby w kolejności ODWROTNEJ do otwarcia, a wyjątki z close() dopisuje jako stłumione,
 *     • nie sprawdzaj istnienia pliku przed otwarciem (luka czasowa, TOCTOU): po prostu spróbuj i obsłuż wyjątek,
 *     • nigdy pusty catch.
 *
 * SŁÓWKA:
 *   cause = przyczyna; suppressed = stłumiony; checked = sprawdzany; unchecked = niesprawdzany; transient error = błąd
 *   przejściowy (chwilowy); permanent = trwały; wrap = opakować; unwrap = rozpakować; TOCTOU = „time of check to
 *   time of use”, czyli luka między sprawdzeniem a użyciem; resource = zasób; close = zamknij.
 *
 * ZOBACZ TEŻ: t10_exceptions/Exceptions06ChainingWrapping (przyczyny i opakowywanie), t18_io_files/Io01PathFiles
 *   (pierwsze wyjątki NIO.2), t18_io_files/Io02ReadingText (MalformedInputException przy czytaniu),
 *   t18_io_files/Io10SimpleLogger (logowanie wyjątków), t18_io_files/Io12Charsets (kodowanie znaków)
 * </pre>
 */
public class Io11IoExceptions {

    public static void main(String[] args) throws IOException {
        // throws IOException = „rzuca IOException”: main przekazuje wyjątki IO wyżej
        title("Io11 — Wyjątki wejścia-wyjścia");

        // Pliki lekcji powstają w katalogu tymczasowym i znikają w finally (blok finally = zawsze się wykona).
        Path dir = TempDir.create("io11"); // create = utwórz
        try {
            hierarchy();                                   // hierarchy = hierarchia
            nio2Exceptions(dir.resolve("s2"));             // NIO.2 exceptions = wyjątki NIO.2
            oldVersusNew(dir.resolve("s3"));               // old versus new = stary kontra nowy
            encodingAndEof(dir.resolve("s4"));             // encoding and EOF = kodowanie i koniec danych
            uncheckedWrapping(dir.resolve("s5"));          // unchecked wrapping = opakowanie w wyjątek niesprawdzany
            closingOrder();                                // closing order = kolejność zamykania
            suppressedExceptions();                        // suppressed exceptions = wyjątki stłumione
            whereToCatch(dir.resolve("s8"));               // where to catch = gdzie łapać
            retryDemo();                                   // retry = ponawianie
            exercises(dir.resolve("cw"));                  // exercises = ćwiczenia
        } finally {
            TempDir.deleteRecursively(dir);
        }
    }

    // =================================================================================================
    // Pomocnicze
    // =================================================================================================

    /**
     * rel = ścieżka względem katalogu bazowego, zawsze z ukośnikiem "/".
     * Po co? Katalog tymczasowy ma losową nazwę (innego nie wypisujemy), a Windows pisze ścieżki z "\"
     * (np. "a\b.txt"), Linux z "/". Zamiana na "/" sprawia, że wydruk jest taki sam wszędzie.
     */
    private static String rel(Path base, Path p) {
        return base.relativize(p).toString().replace('\\', '/');
    }

    /** Akcja, która może rzucić IOException (zwykły Runnable nie może). */
    @FunctionalInterface
    private interface ThrowingIo {
        void run() throws IOException;
    }

    /** Wartość, której obliczenie może rzucić IOException. */
    @FunctionalInterface
    interface IoSupplier<T> {
        T get() throws IOException;
    }

    /** Wartość, której obliczenie może rzucić dowolny wyjątek (używana w ćwiczeniach). */
    @FunctionalInterface
    private interface Risky<T> {
        T get() throws Exception;
    }

    /**
     * ioFails = „IO zawodzi”. Wykonuje akcję i oczekuje wyjątku IO. Wypisuje nazwę klasy wyjątku i nazwę pliku,
     * a NIE komunikat (getMessage): komunikaty IO zawierają pełną ścieżkę (losowy katalog tymczasowy)
     * oraz tekst zależny od systemu i języka (np. polski Windows pisze inaczej niż angielski Linux).
     * Dlatego w tej lekcji wypisujemy TYPY wyjątków, a nie ich komunikaty.
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

    /** risky = „ryzykowne”: opakowuje wyjątki w IllegalStateException, żeby pasowało do Supplier w Check.equal. */
    private static <T> Supplier<T> risky(Risky<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new IllegalStateException(e.getClass().getSimpleName(), e);
            }
        };
    }

    /** Łańcuch KLAS NADRZĘDNYCH podanej klasy, od najbliższej w górę, aż do Exception (bez niej samej, bez Throwable i Object). */
    private static String chain(Class<?> type) {
        List<String> names = new ArrayList<>();
        for (Class<?> c = type.getSuperclass(); c != Exception.class; c = c.getSuperclass()) {
            names.add(c.getSimpleName());
        }
        return String.join(" → ", names); // join = połącz
    }

    // =================================================================================================
    // 1. HIERARCHIA
    // =================================================================================================

    /**
     * 1. Wszystkie wyjątki plikowe to IOException (sprawdzany). Wyjątki NIO.2 mają wspólnego rodzica FileSystemException,
     * który zna nazwę pliku (getFile). Hierarchię odczytujemy z samych klas (getSuperclass = klasa nadrzędna).
     */
    static void hierarchy() {
        section("1. Hierarchia wyjątków IO");

        show("NoSuchFileException", chain(NoSuchFileException.class));
        // WYNIK: NoSuchFileException → FileSystemException → IOException
        show("FileAlreadyExistsException", chain(FileAlreadyExistsException.class));
        // WYNIK: FileAlreadyExistsException → FileSystemException → IOException
        show("DirectoryNotEmptyException", chain(DirectoryNotEmptyException.class));
        // WYNIK: DirectoryNotEmptyException → FileSystemException → IOException
        show("NotDirectoryException", chain(NotDirectoryException.class));
        // WYNIK: NotDirectoryException → FileSystemException → IOException
        show("AccessDeniedException", chain(AccessDeniedException.class));
        // WYNIK: AccessDeniedException → FileSystemException → IOException
        show("FileNotFoundException (stare java.io)", chain(FileNotFoundException.class));
        // WYNIK: FileNotFoundException (stare java.io) → IOException
        show("EOFException", chain(EOFException.class));
        // WYNIK: EOFException → IOException
        show("MalformedInputException", chain(java.nio.charset.MalformedInputException.class));
        // WYNIK: MalformedInputException → CharacterCodingException → IOException
        show("UncheckedIOException", chain(UncheckedIOException.class));
        // WYNIK: UncheckedIOException → RuntimeException

        // Dlaczego IOException jest SPRAWDZANY? Dysk, sieć i uprawnienia leżą poza kontrolą programu: nawet idealny
        // kod może dostać „brak pliku”. Kompilator wymusza więc świadomą decyzję: obsłuż (try/catch) albo przekaż
        // (throws). Dzięki temu nikt nie „zapomni”, że czytanie pliku bywa zawodne.
        // Kosztem jest wygoda (lambdy, strumienie — sekcja 5), dlatego istnieje UncheckedIOException.

        // DOBRA PRAKTYKA: łap najwęższy wyjątek, jaki umiesz sensownie obsłużyć (NoSuchFileException: „użyj wartości
        //   domyślnej”), a resztę (IOException) przekaż wyżej. Dlaczego: szeroki catch (Exception e) połyka także
        //   błędy programisty (NullPointerException), których nie wolno ukrywać.
        // PUŁAPKA: FileNotFoundException NIE jest FileSystemException. Kod, który łapie tylko FileSystemException,
        //   nie złapie błędu ze starego new FileInputStream(...). Łap wspólnego rodzica (IOException) albo oba typy.
    }

    // =================================================================================================
    // 2. WYJĄTKI NIO.2 W AKCJI
    // =================================================================================================

    /** kind = rodzaj: zamienia wyjątek na krótki opis. Kolejność catch: od szczegółowych do ogólnych. */
    private static String kind(ThrowingIo action) {
        try {
            action.run();
            return "brak wyjątku";
        } catch (NoSuchFileException e) {
            return "brak pliku";
        } catch (FileAlreadyExistsException e) {
            return "już istnieje";
        } catch (DirectoryNotEmptyException e) {
            return "katalog niepusty";
        } catch (NotDirectoryException e) {
            return "to nie katalog";
        } catch (FileSystemException e) {
            return "inny błąd systemu plików";
        } catch (IOException e) {
            return "inny błąd IO";
        }
        // Gdyby catch (IOException e) stał PRZED catch (NoSuchFileException e), kompilator zgłosiłby błąd:
        // wyjątek został już przechwycony przez szerszy catch (nieosiągalny kod).
    }

    /**
     * 2. Typowe sytuacje i wyjątki, jakie wywołują. Każdą rozpoznajemy po TYPIE, więc można zareagować inaczej
     * na brak pliku, inaczej na „już istnieje”.
     */
    static void nio2Exceptions(Path base) throws IOException {
        section("2. Wyjątki NIO.2 w akcji i kolejność catch");
        Files.createDirectory(base);

        Path file = base.resolve("dane.txt");
        Files.writeString(file, "treść\n", StandardCharsets.UTF_8); // writeString = zapisz napis (Java 11+)
        Path folder = base.resolve("folder");
        Files.createDirectory(folder);
        Files.writeString(folder.resolve("wewnatrz.txt"), "x", StandardCharsets.UTF_8);

        show("usunięcie brakującego pliku", kind(() -> Files.delete(base.resolve("brak.txt"))));
        // WYNIK: usunięcie brakującego pliku → brak pliku
        show("createFile na istniejącym", kind(() -> Files.createFile(file)));
        // WYNIK: createFile na istniejącym → już istnieje
        show("delete niepustego katalogu", kind(() -> Files.delete(folder)));
        // WYNIK: delete niepustego katalogu → katalog niepusty
        show("lista zawartości zwykłego pliku", kind(() -> Files.list(file).close()));
        // WYNIK: lista zawartości zwykłego pliku → to nie katalog
        show("move na istniejący cel bez REPLACE_EXISTING", kind(() -> Files.move(file, folder.resolve("wewnatrz.txt"))));
        // WYNIK: move na istniejący cel bez REPLACE_EXISTING → już istnieje
        show("poprawna operacja", kind(() -> Files.size(file)));
        // WYNIK: poprawna operacja → brak wyjątku
        ioFails("odczyt brakującego pliku", () -> Files.readString(base.resolve("brak.txt"), StandardCharsets.UTF_8));
        // WYNIK: ✔ odczyt brakującego pliku → rzucono NoSuchFileException (plik: brak.txt)

        // Czego tu NIE pokazujemy (bo zależy od systemu i uprawnień): AccessDeniedException — brak uprawnień do pliku;
        // w Windows bywa też zgłaszana, gdy plik jest otwarty przez inny program (albo jako ogólny FileSystemException
        // z opisem „proces nie może uzyskać dostępu do pliku”). Zachowanie zależy od systemu — dlatego ostatni
        // catch (FileSystemException) i catch (IOException) są potrzebne nawet, gdy znasz konkretne przypadki.

        // PUŁAPKA: w jednym multi-catch nie połączysz wyjątków spokrewnionych: catch (NoSuchFileException |
        //   FileSystemException e) to błąd kompilacji („alternatywy w multi-catch są ze sobą w relacji
        //   dziedziczenia”). Połączysz za to rodzeństwo: NoSuchFileException | AccessDeniedException.
        // DOBRA PRAKTYKA: getFile() z FileSystemException wskazuje, którego pliku dotyczy błąd (przy operacjach
        //   dwuplikowych, np. move, jest też getOtherFile()). Dlaczego: komunikat dla użytkownika powinien mieć
        //   nazwę pliku, a nie samo „błąd zapisu”.
    }

    // =================================================================================================
    // 3. FILENOTFOUNDEXCEPTION A NOSUCHFILEEXCEPTION
    // =================================================================================================

    /**
     * 3. Dwa światy: stare java.io (FileReader, FileInputStream) rzuca FileNotFoundException, nowe NIO.2 (Files...)
     * rzuca NoSuchFileException. Stary komunikat zawiera tekst systemowy w języku systemu, dlatego w lekcji
     * wypisujemy tylko typy.
     */
    static void oldVersusNew(Path base) throws IOException {
        section("3. FileNotFoundException a NoSuchFileException");
        Files.createDirectory(base);
        Path missing = base.resolve("nie-ma.txt");

        // FileReader(File, Charset) = czytnik pliku z jawnym kodowaniem (Java 11+); dawniej tylko wersja bez kodowania.
        ioFails("new FileReader (stare java.io)", () -> new FileReader(missing.toFile(), StandardCharsets.UTF_8).close());
        // WYNIK: ✔ new FileReader (stare java.io) → rzucono FileNotFoundException
        ioFails("Files.newBufferedReader (NIO.2)", () -> Files.newBufferedReader(missing, StandardCharsets.UTF_8).close());
        // WYNIK: ✔ Files.newBufferedReader (NIO.2) → rzucono NoSuchFileException (plik: nie-ma.txt)

        try {
            new FileReader(missing.toFile(), StandardCharsets.UTF_8).close();
        } catch (FileNotFoundException e) {
            show("stary komunikat zawiera nazwę pliku", e.getMessage().contains("nie-ma.txt"));
            // WYNIK: stary komunikat zawiera nazwę pliku → true
            Object asObject = e; // (Object), bo kompilator nie pozwala sprawdzać pokrewieństwa niepowiązanych klas
            show("FileNotFoundException to FileSystemException", asObject instanceof FileSystemException);
            // WYNIK: FileNotFoundException to FileSystemException → false
        }

        // Komunikat starego wyjątku wygląda np. tak: "ścieżka (No such file or directory)" na angielskim Linuksie,
        // a na polskim Windowsie ma w nawiasie tekst po polsku (mniej więcej „Nie można odnaleźć określonego pliku”).
        // Tekst w nawiasie pochodzi z systemu operacyjnego, więc zależy od systemu i jego języka. Program, który
        // PORÓWNUJE ten tekst (np. getMessage().contains("No such file")), działa tylko na angielskim systemie!
        // Nowy NoSuchFileException jest rozpoznawany po TYPIE, a nie po tekście — i to jest właściwa droga.

        // Stary FileNotFoundException opisuje różne sytuacje jednym typem: brak pliku, ale też „to jest katalog”
        // albo brak uprawnień (różnie w różnych systemach). Nie da się ich odróżnić po typie, tylko po tekście.
        // NIO.2 ma osobne typy: NoSuchFileException, AccessDeniedException, NotDirectoryException...

        // DOBRA PRAKTYKA: w nowym kodzie używaj Files.newBufferedReader / Files.newInputStream zamiast
        //   new FileReader / new FileInputStream. Dlaczego: precyzyjne wyjątki, jawne kodowanie i spójne API.
        // PUŁAPKA: new FileReader(String) bez kodowania (Java 17) czyta w domyślnym kodowaniu systemu (w polskim Windows
        //   windows-1250, w Linuksie zwykle UTF-8) — ten sam plik daje inny tekst. Od Javy 18 domyślne jest UTF-8
        //   (JEP 400). Zawsze podawaj kodowanie jawnie (więcej: t18_io_files/Io12Charsets).
    }

    // =================================================================================================
    // 4. KODOWANIE I KONIEC DANYCH
    // =================================================================================================

    /**
     * 4. MalformedInputException (zła zawartość względem kodowania) i EOFException (dane urwały się w środku wartości).
     * Obie to IOException, ale mówią o czymś innym niż „brak pliku”.
     */
    static void encodingAndEof(Path base) throws IOException {
        section("4. MalformedInputException i EOFException");
        Files.createDirectory(base);

        Charset windows1250 = Charset.forName("windows-1250"); // forName = znajdź kodowanie po nazwie
        Path legacy = base.resolve("stary.txt");
        Files.write(legacy, "zażółć gęślą jaźń".getBytes(windows1250)); // bajty w kodowaniu windows-1250, nie UTF-8

        ioFails("readString jako UTF-8", () -> Files.readString(legacy, StandardCharsets.UTF_8));
        // WYNIK: ✔ readString jako UTF-8 → rzucono MalformedInputException
        ioFails("readAllLines jako UTF-8", () -> Files.readAllLines(legacy, StandardCharsets.UTF_8));
        // WYNIK: ✔ readAllLines jako UTF-8 → rzucono MalformedInputException

        try {
            Files.readString(legacy, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) { // CharacterCodingException = wspólny rodzic (błąd kodowania znaków)
            System.out.println("✔ złapano jako CharacterCodingException (rodzic) → " + e.getClass().getSimpleName());
            // WYNIK: ✔ złapano jako CharacterCodingException (rodzic) → MalformedInputException
        }

        show("ten sam plik z właściwym kodowaniem", Files.readString(legacy, windows1250));
        // WYNIK: ten sam plik z właściwym kodowaniem → zażółć gęślą jaźń

        byte[] raw = Files.readAllBytes(legacy);
        show("new String(bajty, UTF_8) zawiera znak zastępczy", new String(raw, StandardCharsets.UTF_8).indexOf(0xFFFD) >= 0);
        // WYNIK: new String(bajty, UTF_8) zawiera znak zastępczy → true
        note("Files.readString zgłasza błąd; new String i InputStreamReader po cichu podstawiają znak zastępczy.");
        // WYNIK: ℹ Files.readString zgłasza błąd; new String i InputStreamReader po cichu podstawiają znak zastępczy.

        // EOFException: DataInputStream czyta liczby o stałym rozmiarze; gdy brakuje bajtów, rzuca wyjątek.
        byte[] twoBytes = {0, 1}; // readInt potrzebuje 4 bajtów
        ioFails("readInt na dwóch bajtach", () -> {
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(twoBytes))) {
                in.readInt(); // readInt = odczytaj liczbę int (4 bajty)
            }
        });
        // WYNIK: ✔ readInt na dwóch bajtach → rzucono EOFException
        try (ByteArrayInputStream plain = new ByteArrayInputStream(new byte[0])) {
            show("read() na pustym strumieniu zwraca", plain.read()); // zwykłe read() NIE rzuca wyjątku, tylko -1
            // WYNIK: read() na pustym strumieniu zwraca → -1
        }

        // PUŁAPKA: MalformedInputException najczęściej znaczy „plik nie jest w UTF-8” (np. zapisany w windows-1250
        //   przez stary program). Nie „naprawiaj” tego pustym catch — ustal prawdziwe kodowanie pliku.
        // PUŁAPKA: ciche podstawianie znaków (new String, InputStreamReader) jest gorsze niż wyjątek: dane psują się
        //   bez śladu. Gdy chcesz wykryć błędy, czytaj przez Files.readString/readAllLines/newBufferedReader.
        // DOBRA PRAKTYKA: odróżniaj „koniec danych” od „urwane dane”: read() zwraca -1 (normalny koniec), a
        //   readInt/readFully rzucają EOFException (plik jest uszkodzony lub ucięty).
    }

    // =================================================================================================
    // 5. UNCHECKEDIOEXCEPTION
    // =================================================================================================

    /**
     * Wczytuje pliki, których nazwy są w pliku listy. W lambdzie nie wolno rzucić IOException (sprawdzanego),
     * więc opakowujemy go w UncheckedIOException.
     */
    private static List<String> readAllListed(Path base, Path listFile) throws IOException {
        try (Stream<String> names = Files.lines(listFile, StandardCharsets.UTF_8)) { // lines = linie (leniwy strumień)
            return names.map(name -> {
                try {
                    return Files.readString(base.resolve(name), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new UncheckedIOException(e); // opakowanie: lambda może rzucić tylko wyjątek niesprawdzany
                }
            }).toList(); // toList = do listy niezmiennej (Java 16+)
        } catch (UncheckedIOException e) {
            throw e.getCause(); // rozpakowanie: na granicy metody wracamy do zwykłego IOException
        }
    }

    /**
     * 5. Interfejsy funkcyjne (Function, Supplier...) nie deklarują throws IOException. W lambdzie wołającej Files.*
     * łapiemy IOException i opakowujemy go w UncheckedIOException; potem, gdzie możemy, rozpakowujemy przez getCause().
     */
    static void uncheckedWrapping(Path base) throws IOException {
        section("5. UncheckedIOException w lambdach i strumieniach");
        Files.createDirectory(base);
        Files.writeString(base.resolve("a.txt"), "alfa", StandardCharsets.UTF_8);
        Files.writeString(base.resolve("b.txt"), "beta", StandardCharsets.UTF_8);
        Path list = base.resolve("lista.txt");
        Files.writeString(list, "a.txt\nb.txt\n", StandardCharsets.UTF_8);

        show("wszystkie pliki z listy", readAllListed(base, list));
        // WYNIK: wszystkie pliki z listy → [alfa, beta]

        Files.writeString(list, "a.txt\nc.txt\nb.txt\n", StandardCharsets.UTF_8); // c.txt nie istnieje
        ioFails("lista z brakującym plikiem", () -> readAllListed(base, list));
        // WYNIK: ✔ lista z brakującym plikiem → rzucono NoSuchFileException (plik: c.txt)
        note("W środku poleciał UncheckedIOException, a metoda oddała na zewnątrz oryginalny NoSuchFileException.");
        // WYNIK: ℹ W środku poleciał UncheckedIOException, a metoda oddała na zewnątrz oryginalny NoSuchFileException.

        // Files.lines jest LENIWE: plik otwiera od razu, ale linie czyta i dekoduje dopiero przy przetwarzaniu strumienia.
        // Dlatego błąd kodowania wychodzi później i w innej postaci: jako UncheckedIOException.
        Path legacy = base.resolve("stary.txt");
        Files.write(legacy, "zażółć".getBytes(Charset.forName("windows-1250")));
        try (Stream<String> lines = Files.lines(legacy, StandardCharsets.UTF_8)) {
            lines.forEach(line -> { }); // forEach = dla każdego (tu: nic nie robimy, wystarczy przeczytać)
        } catch (UncheckedIOException e) {
            System.out.println("✔ Files.lines + zły kodowanie → UncheckedIOException, przyczyna: "
                    + e.getCause().getClass().getSimpleName());
            // WYNIK: ✔ Files.lines + zły kodowanie → UncheckedIOException, przyczyna: MalformedInputException
        }

        // Podobnie: Files.list/walk mogą rzucić UncheckedIOException w trakcie iteracji (np. katalog zniknął w trakcie),
        // a DirectoryStream (pętla for-each) — DirectoryIteratorException (też niesprawdzany, przyczyna w getCause()).

        // PUŁAPKA: UncheckedIOException „wymazuje” obowiązek obsługi z oczu kompilatora. Jeśli nikt go nie złapie,
        //   program padnie ze stosem. Rozpakuj go na granicy swojego kodu (jak w readAllListed) albo obsłuż na szczycie.
        // DOBRA PRAKTYKA: opakowuj w UncheckedIOException WYŁĄCZNIE tam, gdzie kompilator nie pozwala inaczej
        //   (lambdy), i zawsze z przyczyną (new UncheckedIOException(e)). Dlaczego: bez przyczyny tracisz informację,
        //   który plik i dlaczego zawiódł.
        // PRZED: names.map(n -> Files.readString(Path.of(n)))  — błąd kompilacji: IOException jest sprawdzany.
    }

    // =================================================================================================
    // 6. KOLEJNOŚĆ ZAMYKANIA ZASOBÓW
    // =================================================================================================

    /** Zasób, który zapisuje do wspólnego dziennika każdą swoją czynność. close() może rzucić wyjątek. */
    static final class Res implements AutoCloseable {
        private final String name;
        private final List<String> trace; // trace = ślad (dziennik czynności)
        private final boolean failOnClose;

        Res(String name, List<String> trace, boolean failOnOpen, boolean failOnClose) throws IOException {
            this.name = name;
            this.trace = trace;
            this.failOnClose = failOnClose;
            if (failOnOpen) {
                trace.add("błąd otwierania " + name);
                throw new IOException("nie można otworzyć " + name);
            }
            trace.add("otwarto " + name);
        }

        void use() {
            trace.add("używam " + name);
        }

        @Override
        public void close() throws IOException {
            trace.add("zamykam " + name);
            if (failOnClose) {
                throw new IOException("błąd zamykania " + name);
            }
        }
    }

    /**
     * 6. W try-with-resources zasoby zamykają się w kolejności ODWROTNEJ do otwarcia (jak zdejmowanie naczyń ze
     * stosu). Jeśli otwarcie kolejnego się nie uda, wcześniej otwarte zasoby i tak zostaną zamknięte.
     */
    static void closingOrder() {
        section("6. try-with-resources: kolejność zamykania");

        List<String> trace = new ArrayList<>();
        try (Res a = new Res("A", trace, false, false);
             Res b = new Res("B", trace, false, false);
             Res c = new Res("C", trace, false, false)) {
            a.use();
            b.use();
            c.use();
        } catch (IOException e) {
            trace.add("nieoczekiwany wyjątek");
        }
        show("trzy zasoby", trace);
        // WYNIK: trzy zasoby → [otwarto A, otwarto B, otwarto C, używam A, używam B, używam C, zamykam C, zamykam B, zamykam A]

        trace.clear();
        try (Res a = new Res("A", trace, false, false);
             Res b = new Res("B", trace, true, false); // otwarcie B zawodzi
             Res c = new Res("C", trace, false, false)) {
            a.use();
            b.use();
            c.use();
        } catch (IOException e) {
            trace.add("złapano: " + e.getMessage());
        }
        show("otwarcie drugiego zawodzi", trace);
        // WYNIK: otwarcie drugiego zawodzi → [otwarto A, błąd otwierania B, zamykam A, złapano: nie można otworzyć B]
        note("A zostało zamknięte, mimo że B się nie otworzyło; C nawet nie próbowano otworzyć. Blok catch działa PO zamknięciu zasobów.");
        // WYNIK: ℹ A zostało zamknięte, mimo że B się nie otworzyło; C nawet nie próbowano otworzyć. Blok catch działa PO zamknięciu zasobów.

        // Typowy przykład z życia: new BufferedReader(new InputStreamReader(Files.newInputStream(p), UTF_8)) — zamknięcie
        // zewnętrznego (BufferedReader) zamyka też wewnętrzne. W try podaj WSZYSTKIE zasoby osobno, gdy każdy
        // jest ważny, a przy dekoratorach wystarczy zamknąć ten zewnętrzny.

        // PUŁAPKA: zasób zadeklarowany w nagłówku try nie jest widoczny w catch ani w finally (jest już zamknięty).
        // DOBRA PRAKTYKA: zawsze try-with-resources zamiast ręcznego finally { x.close(); }. Dlaczego: sam zamyka
        //   w dobrej kolejności, nie zapomni o żadnym zasobie i nie gubi wyjątków (sekcja 7).
    }

    // =================================================================================================
    // 7. WYJĄTKI STŁUMIONE
    // =================================================================================================

    private static List<String> suppressedMessages(Throwable e) {
        List<String> result = new ArrayList<>();
        for (Throwable t : e.getSuppressed()) { // getSuppressed = pobierz stłumione
            result.add(t.getMessage());
        }
        return result;
    }

    /**
     * 7. Co, gdy zawiedzie i praca, i zamykanie? Stary try/finally gubi pierwszy wyjątek (zostaje ten z finally).
     * try-with-resources zachowuje GŁÓWNY wyjątek (z ciała), a wyjątki z close() dopisuje jako stłumione.
     */
    static void suppressedExceptions() throws IOException {
        section("7. Wyjątki stłumione (suppressed)");

        List<String> trace = new ArrayList<>();

        // PRZED: ręczne finally — wyjątek z close() przykrywa wyjątek z pracy.
        Res old = new Res("X", trace, false, true);
        try {
            try {
                old.use();
                throw new IOException("błąd głównej pracy");
            } finally {
                old.close(); // rzuca „błąd zamykania X” i ten wyjątek zastępuje poprzedni
            }
        } catch (IOException e) {
            show("PRZED (finally): widoczny wyjątek", e.getMessage());
            // WYNIK: PRZED (finally): widoczny wyjątek → błąd zamykania X
            show("PRZED (finally): stłumione", suppressedMessages(e));
            // WYNIK: PRZED (finally): stłumione → []
            note("Prawdziwa przyczyna problemu („błąd głównej pracy”) przepadła bez śladu.");
            // WYNIK: ℹ Prawdziwa przyczyna problemu („błąd głównej pracy”) przepadła bez śladu.
        }

        // PO: try-with-resources.
        try (Res r = new Res("X", trace, false, true)) {
            r.use();
            throw new IOException("błąd głównej pracy");
        } catch (IOException e) {
            show("PO (try-with-resources): widoczny wyjątek", e.getMessage());
            // WYNIK: PO (try-with-resources): widoczny wyjątek → błąd głównej pracy
            show("PO: stłumione", suppressedMessages(e));
            // WYNIK: PO: stłumione → [błąd zamykania X]
        }

        // Dwa zasoby, oba zawodzą przy zamykaniu — zamykane w odwrotnej kolejności (B, potem A):
        try (Res a = new Res("A", trace, false, true);
             Res b = new Res("B", trace, false, true)) {
            a.use();
            b.use();
            throw new IOException("błąd głównej pracy");
        } catch (IOException e) {
            show("dwa zasoby: stłumione", suppressedMessages(e));
            // WYNIK: dwa zasoby: stłumione → [błąd zamykania B, błąd zamykania A]
        }

        // Gdy ciało NIE rzuca, a close() tak — wyjątek z close() staje się głównym:
        try (Res r = new Res("Y", trace, false, true)) {
            r.use();
        } catch (IOException e) {
            show("tylko close() zawodzi", e.getMessage());
            // WYNIK: tylko close() zawodzi → błąd zamykania Y
            show("liczba stłumionych", e.getSuppressed().length);
            // WYNIK: liczba stłumionych → 0
        }

        // addSuppressed(Throwable) pozwala dopisać stłumiony wyjątek ręcznie (robi to try-with-resources i nasze
        // ponawianie w sekcji 9).

        // DOBRA PRAKTYKA: zawsze try-with-resources dla zasobów (pliki, strumienie, połączenia). Dlaczego: nie gubisz
        //   głównego wyjątku. Przy wypisywaniu błędu pokaż też getSuppressed() albo cały stos — logger (Io10SimpleLogger)
        //   i printStackTrace wypisują je pod nagłówkiem „Suppressed:”.
        // PUŁAPKA: wyjątek z finally przykrywa wyjątek z try. Nie rzucaj wyjątków z bloku finally; sprzątanie
        //   (close) rób w try-with-resources.
    }

    // =================================================================================================
    // 8. GDZIE ŁAPAĆ
    // =================================================================================================

    /** Nasz wyjątek warstwy konfiguracji: dopisuje kontekst („jaki plik”) do przyczyny technicznej. */
    static class ConfigException extends Exception {
        private static final long serialVersionUID = 1L;

        ConfigException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Warstwa niska: tylko czyta i rzuca IOException dalej (throws), niczego nie ukrywa. */
    private static String readText(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    /** Warstwa środkowa: zna kontekst (to jest PORT), więc tłumaczy wyjątki techniczne na zrozumiałe i dodaje przyczynę. */
    private static int loadPort(Path file) throws ConfigException {
        try {
            return Integer.parseInt(readText(file).trim()); // trim = obetnij białe znaki
        } catch (NoSuchFileException e) {
            throw new ConfigException("brak pliku konfiguracji " + file.getFileName(), e);
        } catch (IOException e) {
            throw new ConfigException("nie można odczytać konfiguracji " + file.getFileName(), e);
        } catch (NumberFormatException e) {
            throw new ConfigException("port w pliku " + file.getFileName() + " nie jest liczbą", e);
        }
    }

    /** Szczyt: JEDEN komunikat dla użytkownika i jeden wpis do dziennika (tu: lista). Nie rzuca dalej. */
    private static String runApplication(Path file, List<String> journal) {
        try {
            return "Port: " + loadPort(file);
        } catch (ConfigException e) {
            journal.add(e.getMessage() + " | przyczyna: " + e.getCause().getClass().getSimpleName());
            return "Nie udało się uruchomić: " + e.getMessage();
        }
    }

    /** Źle: połyka wyjątek. Wołający nie wie, że plik nie istniał — dostaje pusty tekst. */
    private static String readSwallowing(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // pusty catch: NIE RÓB TEGO
        }
        return "";
    }

    /** Dobrze: wyjątek, który umiemy sensownie obsłużyć (brak pliku = wartość domyślna), a resztę przekazujemy dalej. */
    private static String readOrDefault(Path file, String fallback) throws IOException {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            return fallback; // fallback = wartość zapasowa; to świadoma decyzja, nie połknięcie błędu
        }
    }

    /**
     * 8. Warstwy: nisko rzucamy (throws), w środku tłumaczymy i dodajemy kontekst, na szczycie raz informujemy
     * użytkownika i zapisujemy w dzienniku. Nigdy pustego catch. Nigdy exists() przed otwarciem (TOCTOU).
     */
    static void whereToCatch(Path base) throws IOException {
        section("8. Gdzie łapać wyjątki");
        Files.createDirectory(base);

        Path portFile = base.resolve("port.cfg");
        List<String> journal = new ArrayList<>(); // journal = dziennik (zamiast prawdziwego loggera)

        Files.writeString(portFile, "8080\n", StandardCharsets.UTF_8);
        show("poprawny plik", runApplication(portFile, journal));
        // WYNIK: poprawny plik → Port: 8080
        show("brak pliku", runApplication(base.resolve("brak.cfg"), journal));
        // WYNIK: brak pliku → Nie udało się uruchomić: brak pliku konfiguracji brak.cfg
        Files.writeString(portFile, "osiem\n", StandardCharsets.UTF_8);
        show("zła zawartość", runApplication(portFile, journal));
        // WYNIK: zła zawartość → Nie udało się uruchomić: port w pliku port.cfg nie jest liczbą
        showEach("dziennik (dla administratora, z przyczyną techniczną)", journal);
        // WYNIK: dziennik (dla administratora, z przyczyną techniczną) (liczba elementów: 2):
        // WYNIK: • brak pliku konfiguracji brak.cfg | przyczyna: NoSuchFileException
        // WYNIK: • port w pliku port.cfg nie jest liczbą | przyczyna: NumberFormatException

        // Stos wywołań (printStackTrace) kierujemy do StringWriter, NIE na System.err. Pierwsza linia to
        // klasa i komunikat; w kolejnych są numery linii kodu, więc ich nie wypisujemy.
        try {
            loadPort(base.resolve("brak.cfg"));
        } catch (ConfigException e) {
            StringWriter text = new StringWriter(); // StringWriter = pisanie do tekstu w pamięci
            e.printStackTrace(new PrintWriter(text));
            List<String> traceLines = text.toString().lines().toList(); // lines = linie (Java 11+)
            show("pierwsza linia stosu", traceLines.get(0).replace("t18_io_files.Io11IoExceptions$", ""));
            // WYNIK: pierwsza linia stosu → ConfigException: brak pliku konfiguracji brak.cfg
            show("jest sekcja Caused by", traceLines.stream().anyMatch(l -> l.startsWith("Caused by: ")));
            // WYNIK: jest sekcja Caused by → true
        }

        // PRZED: pusty catch.
        Path ghost = base.resolve("duch.txt");
        show("PRZED (połknięty błąd) — wynik", "'" + readSwallowing(ghost) + "'");
        // WYNIK: PRZED (połknięty błąd) — wynik → ''
        note("Wołający myśli, że plik jest pusty. Prawdziwa przyczyna (brak pliku) przepadła.");
        // WYNIK: ℹ Wołający myśli, że plik jest pusty. Prawdziwa przyczyna (brak pliku) przepadła.
        // PO: obsłużony tylko NoSuchFileException i to z jasną decyzją.
        show("PO (wartość domyślna dla braku pliku)", readOrDefault(ghost, "(brak pliku)"));
        // WYNIK: PO (wartość domyślna dla braku pliku) → (brak pliku)

        // TOCTOU („sprawdzenie, potem użycie”): między exists() a otwarciem inny proces może usunąć plik.
        Path queue = base.resolve("kolejka.txt");
        Files.writeString(queue, "zadanie", StandardCharsets.UTF_8);
        if (Files.exists(queue)) { // 1. sprawdzenie: plik jest...
            Files.delete(queue);    // ...tu „wchodzi” inny proces i go usuwa (symulujemy to sami)...
            ioFails("odczyt po sprawdzeniu exists()", () -> readText(queue)); // 2. użycie: za późno
        }
        // WYNIK: ✔ odczyt po sprawdzeniu exists() → rzucono NoSuchFileException (plik: kolejka.txt)
        // Wniosek: exists() nie daje gwarancji na przyszłość. Jedyna pewna odpowiedź to wynik samej operacji.
        // Zamiast "if (exists) { czytaj }" pisz "try { czytaj } catch (NoSuchFileException e) { ... }" (jak readOrDefault).

        // DOBRA PRAKTYKA: wyjątek logujesz w JEDNYM miejscu (szczyt), niżej dodajesz kontekst i rzucasz z przyczyną
        //   (new ConfigException(opis, e)). Dlaczego: inaczej ten sam błąd pojawia się w logu kilka razy, a bez
        //   przyczyny nie wiadomo, co naprawdę się stało.
        // DOBRA PRAKTYKA: komunikat dla użytkownika mówi CO zrobić albo co zawiodło („brak pliku port.cfg”), bez
        //   nazw klas wyjątków i ścieżek wewnętrznych. Szczegóły techniczne idą do logu (t18_io_files/Io10SimpleLogger).
        // PUŁAPKA: catch (Exception e) lub catch (Throwable t) „na wszelki wypadek” łapie też błędy programisty
        //   (NullPointerException) i zawieszenie wątku (InterruptedException). Łap konkretne typy.
    }

    // =================================================================================================
    // 9. PONAWIANIE
    // =================================================================================================

    /** Operacja zawodna: pierwsze failFirst wywołań rzuca IOException, potem działa (symulacja błędu przejściowego). */
    static final class Flaky {
        int calls; // calls = liczba wywołań
        private final int failFirst;

        Flaky(int failFirst) {
            this.failFirst = failFirst;
        }

        String get() throws IOException {
            calls++;
            if (calls <= failFirst) {
                throw new IOException("chwilowy błąd " + calls);
            }
            return "dane po " + calls + " próbach";
        }
    }

    /**
     * Ponawia operację do {@code attempts} razy. Błędy trwałe (brak pliku, brak uprawnień) rzuca od razu — ponawianie
     * nic nie da. Gdy wszystkie próby zawiodą, rzuca OSTATNI wyjątek z wcześniejszymi jako stłumionymi.
     */
    static <T> T retry(int attempts, IoSupplier<T> action) throws IOException {
        if (attempts < 1) {
            throw new IllegalArgumentException("liczba prób musi być dodatnia: " + attempts);
        }
        List<IOException> failures = new ArrayList<>();
        for (int i = 1; i <= attempts; i++) {
            try {
                return action.get();
            } catch (NoSuchFileException | AccessDeniedException e) {
                throw e; // trwały błąd: nie ponawiamy
            } catch (IOException e) {
                failures.add(e);
                // W prawdziwym kodzie tu jest krótka pauza, rosnąca z każdą próbą (exponential backoff) — pomijamy
                // ją w lekcji, żeby działała szybko i powtarzalnie.
            }
        }
        IOException last = failures.get(failures.size() - 1);
        for (int i = 0; i < failures.size() - 1; i++) {
            last.addSuppressed(failures.get(i)); // addSuppressed = dopisz jako stłumiony
        }
        throw last;
    }

    /**
     * 9. Błędy przejściowe (chwilowa awaria sieci, plik zajęty przez antywirus lub indeksowanie) czasem mijają same.
     * Ponawianie z limitem prób pomaga, ale tylko przy błędach przejściowych i operacjach bezpiecznych do powtórzenia.
     */
    static void retryDemo() throws IOException {
        section("9. Ponawianie z limitem prób");

        Flaky twoFailures = new Flaky(2);
        show("2 błędy, limit 3 próby", retry(3, twoFailures::get));
        // WYNIK: 2 błędy, limit 3 próby → dane po 3 próbach

        Flaky alwaysFailing = new Flaky(10);
        try {
            retry(3, alwaysFailing::get);
        } catch (IOException e) {
            show("po wyczerpaniu prób — wyjątek", e.getMessage());
            // WYNIK: po wyczerpaniu prób — wyjątek → chwilowy błąd 3
            show("stłumione (poprzednie próby)", suppressedMessages(e));
            // WYNIK: stłumione (poprzednie próby) → [chwilowy błąd 1, chwilowy błąd 2]
            show("liczba wywołań", alwaysFailing.calls);
            // WYNIK: liczba wywołań → 3
        }

        int[] permanentCalls = {0}; // tablica jednoelementowa: lambda może zmieniać jej zawartość
        ioFails("brak pliku — bez ponawiania", () -> retry(5, () -> {
            permanentCalls[0]++;
            return Files.readString(Path.of("na-pewno-nie-ma-takiego-pliku.txt"), StandardCharsets.UTF_8);
        }));
        // WYNIK: ✔ brak pliku — bez ponawiania → rzucono NoSuchFileException (plik: na-pewno-nie-ma-takiego-pliku.txt)
        show("liczba wywołań przy trwałym błędzie", permanentCalls[0]);
        // WYNIK: liczba wywołań przy trwałym błędzie → 1

        // PUŁAPKA: ponawianie bez limitu (while (true)) potrafi zawiesić program na zawsze i obciążyć awarię
        //   (setki zapytań na sekundę do systemu, który się właśnie podnosi). Zawsze limit prób i pauzy.
        // PUŁAPKA: ponawiaj tylko operacje, które można bezpiecznie powtórzyć (odczyt, zapis „ustaw wartość”). Zapis
        //   „dodaj do salda” wykonany drugi raz po niepewnym błędzie podwoi kwotę.
        // DOBRA PRAKTYKA: ponawiaj tylko błędy przejściowe, a trwałe (brak pliku, zła zawartość) zgłaszaj od razu.
        //   Dlaczego: ponawianie błędu, który nie minie, tylko opóźnia informację o problemie.
        // W praktyce: biblioteki do ponawiania (np. Spring Retry, Resilience4j) — z backoffem i losowym rozrzutem.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • IOException (sprawdzany) → FileSystemException (NIO.2: NoSuchFile, FileAlreadyExists, DirectoryNotEmpty,
     *     NotDirectory, AccessDenied) | FileNotFoundException (stare java.io) | EOFException | CharacterCodingException.
     *   • FileNotFoundException NIE jest FileSystemException; jego komunikat zależy od systemu i języka — rozpoznawaj
     *     błędy po TYPIE, nie po tekście.
     *   • catch: od najwęższego do najszerszego; rodzeństwo można łączyć przez |, rodzica z dzieckiem nie.
     *   • MalformedInputException = bajty niezgodne z kodowaniem; EOFException = dane urwane; read() zwraca -1.
     *   • W lambdach: UncheckedIOException (zawsze z przyczyną), rozpakowanie przez getCause().
     *   • try-with-resources: zamyka w ODWROTNEJ kolejności, zachowuje główny wyjątek, close() → getSuppressed().
     *   • Nisko throws, w środku kontekst + przyczyna, na szczycie jeden komunikat i log. Nigdy pusty catch.
     *   • Zamiast exists() przed otwarciem — spróbuj i złap NoSuchFileException (TOCTOU).
     *   • Ponawianie: limit prób, tylko błędy przejściowe, operacje bezpieczne do powtórzenia, pauzy.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego IOException jest wyjątkiem sprawdzanym, a UncheckedIOException — nie? Po co ten drugi?
     *   2. Co wypisze:  try (Res a = new Res("A"); Res b = new Res("B")) { System.out.println("ciało"); }
     *                   — przy założeniu, że konstruktor i close() każdego zasobu wypisują „otwarto X” i „zamykam X”.
     *   3. ZNAJDŹ BŁĄD:  try { Files.readString(p, UTF_8); }
     *                    catch (IOException e) { ... }
     *                    catch (NoSuchFileException e) { ... }
     *   4. Dlaczego porównywanie e.getMessage().contains("No such file") jest złym pomysłem? Czego użyć zamiast tego?
     *   5. ZNAJDŹ BŁĄD:  if (Files.exists(p)) { tekst = Files.readString(p, UTF_8); } else { tekst = ""; }
     *      — czemu to kod z luką i jak go poprawić?
     *   6. Co wypisze:  try (Res r = new Res("X")) { throw new IOException("A"); }  // close() rzuca IOException("B")
     *                   catch (IOException e) { System.out.println(e.getMessage() + " " + e.getSuppressed().length); }  ?
     *   7. Czy ponawianie warto stosować przy NoSuchFileException? A przy chwilowym błędzie połączenia? Dlaczego?
     *   8. Co poszło źle:  catch (IOException e) { }  w metodzie wczytującej konfigurację?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises(Path base) throws IOException {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Files.createDirectory(base);

        Path okFile = base.resolve("ok.txt");
        Files.writeString(okFile, "zażółć\ngęślą jaźń\n", StandardCharsets.UTF_8);
        Path badFile = base.resolve("zle.txt");
        Files.write(badFile, "zażółć gęślą jaźń".getBytes(Charset.forName("windows-1250")));
        Path missing = base.resolve("brak.txt");

        Check.equal("ćw. 1: klasyfikacja plików", List.of("OK", "BRAK PLIKU", "ZŁE KODOWANIE"),
                risky(() -> List.of(exercise1(okFile), exercise1(missing), exercise1(badFile))));
        Check.equal("ćw. 2: readIfExists", List.of(Optional.of("zażółć\ngęślą jaźń\n"), Optional.empty()),
                risky(() -> List.of(exercise2(okFile), exercise2(missing))));
        Check.equal("ćw. 3: liczba znaków", List.of(16, -1), risky(() -> List.of(exercise3(okFile), exercise3(badFile))));
        Check.equal("ćw. 4: ponawianie", List.of("dane po 3 próbach", "porażka po 2 próbach, stłumione: 1", "wywołań: 1"),
                risky(() -> retryReport(false)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("OK", "BRAK PLIKU", "ZŁE KODOWANIE"),
                risky(() -> List.of(solution1(okFile), solution1(missing), solution1(badFile))));
        Check.equal("ćw. 2 (wzorzec)", List.of(Optional.of("zażółć\ngęślą jaźń\n"), Optional.empty()),
                risky(() -> List.of(solution2(okFile), solution2(missing))));
        Check.equal("ćw. 3 (wzorzec)", List.of(16, -1), risky(() -> List.of(solution3(okFile), solution3(badFile))));
        Check.equal("ćw. 4 (wzorzec)", List.of("dane po 3 próbach", "porażka po 2 próbach, stłumione: 1", "wywołań: 1"),
                risky(() -> retryReport(true)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** Uruchamia ćwiczenie 4 (albo wzorzec) w trzech sytuacjach i opisuje wyniki napisami. */
    private static List<String> retryReport(boolean reference) throws IOException {
        List<String> report = new ArrayList<>();

        Flaky recovers = new Flaky(2);
        report.add(reference ? solution4(3, recovers::get) : exercise4(3, recovers::get));

        Flaky failing = new Flaky(5);
        try {
            if (reference) {
                solution4(2, failing::get);
            } else {
                exercise4(2, failing::get);
            }
            report.add("brak wyjątku?");
        } catch (IOException e) {
            report.add("porażka po " + failing.calls + " próbach, stłumione: " + e.getSuppressed().length);
        }

        int[] calls = {0};
        IoSupplier<String> permanent = () -> {
            calls[0]++;
            throw new FileAlreadyExistsException("x"); // FileSystemException = błąd trwały w tym ćwiczeniu
        };
        try {
            if (reference) {
                solution4(3, permanent);
            } else {
                exercise4(3, permanent);
            }
        } catch (FileSystemException e) {
            report.add("wywołań: " + calls[0]);
        }
        return report;
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć "OK", gdy plik da się wczytać jako UTF-8; "BRAK PLIKU", gdy go nie ma
     * (NoSuchFileException); "ZŁE KODOWANIE", gdy bajty nie są poprawnym UTF-8 (CharacterCodingException);
     * każdy inny błąd IO przekaż dalej (throws).
     * Podpowiedź: Files.readString(file, UTF_8) i trzy bloki catch w dobrej kolejności.
     */
    static String exercise1(Path file) throws IOException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ funkcję z luką czasową (TOCTOU) na wersję „spróbuj i złap”:
     * <pre>{@code
     * // PRZED:
     * static String readIfExists(Path file) throws IOException {
     *     if (Files.exists(file)) {
     *         return Files.readString(file, StandardCharsets.UTF_8);
     *     }
     *     return null;
     * }
     * // PO: zwraca Optional<String> (pusty, gdy pliku brak), bez wywołania exists()
     * }</pre>
     * Podpowiedź: try { return Optional.of(...); } catch (NoSuchFileException e) { return Optional.empty(); }
     */
    static Optional<String> exercise2(Path file) throws IOException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): policz łączną liczbę znaków we wszystkich liniach pliku (suma długości linii, bez znaków
     * nowej linii), czytając przez Files.lines(file, UTF_8). Gdy plik ma złe kodowanie, zwróć -1 (strumień zgłosi
     * UncheckedIOException z przyczyną MalformedInputException). Strumień zamknij (try-with-resources).
     * Podpowiedź: lines.mapToInt(String::length).sum(); złap UncheckedIOException i sprawdź getCause().
     */
    static int exercise3(Path file) throws IOException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): napisz retry, który wykona operację najwyżej {@code attempts} razy.
     * Każdy IOException, który NIE jest FileSystemException, uznaj za przejściowy i ponów próbę; FileSystemException
     * (trwały błąd) rzuć od razu. Gdy wszystkie próby zawiodą, rzuć OSTATNI wyjątek, a poprzednie dopisz mu jako stłumione.
     * Podpowiedź: lista porażek, catch (FileSystemException e) { throw e; } przed catch (IOException e),
     * a na końcu addSuppressed na ostatnim wyjątku.
     */
    static <T> T exercise4(int attempts, IoSupplier<T> action) throws IOException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(Path file) throws IOException {
        try {
            Files.readString(file, StandardCharsets.UTF_8);
            return "OK";
        } catch (NoSuchFileException e) {
            return "BRAK PLIKU";
        } catch (CharacterCodingException e) {
            return "ZŁE KODOWANIE";
        }
    }

    static Optional<String> solution2(Path file) throws IOException {
        try {
            return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        }
    }

    static int solution3(Path file) throws IOException {
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines.mapToInt(String::length).sum();
        } catch (UncheckedIOException e) {
            if (e.getCause() instanceof CharacterCodingException) {
                return -1;
            }
            throw e.getCause();
        }
    }

    static <T> T solution4(int attempts, IoSupplier<T> action) throws IOException {
        List<IOException> failures = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            try {
                return action.get();
            } catch (FileSystemException e) {
                throw e;
            } catch (IOException e) {
                failures.add(e);
            }
        }
        IOException last = failures.get(failures.size() - 1);
        for (int i = 0; i < failures.size() - 1; i++) {
            last.addSuppressed(failures.get(i));
        }
        throw last;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. IOException jest sprawdzany, bo operacje na plikach zawodzą z powodów poza kontrolą programu (dysk, sieć,
     *      uprawnienia), więc kompilator wymusza decyzję. UncheckedIOException (RuntimeException) służy do przeniesienia
     *      takiego błędu przez miejsca, gdzie wyjątku sprawdzanego nie wolno zadeklarować: lambdy i strumienie.
     *   2. otwarto A, otwarto B, ciało, zamykam B, zamykam A — zamykanie w odwrotnej kolejności do otwarcia.
     *   3. Szerszy catch (IOException) stoi PRZED węższym (NoSuchFileException), więc drugi jest nieosiągalny:
     *      błąd kompilacji. Poprawnie: najpierw NoSuchFileException, potem IOException.
     *   4. Tekst komunikatu pochodzi z systemu operacyjnego i zależy od jego języka (polski Windows pisze po polsku),
     *      więc taki kod działa tylko na części systemów. Należy rozpoznawać błąd po TYPIE wyjątku
     *      (NoSuchFileException) — w NIO.2 jest osobny typ dla każdej sytuacji.
     *   5. To luka czasowa TOCTOU: między exists() a readString inny proces może usunąć plik, a wtedy i tak poleci
     *      NoSuchFileException. Poprawka: try { tekst = Files.readString(p, UTF_8); } catch (NoSuchFileException e)
     *      { tekst = ""; } — jedna operacja, jedno miejsce obsługi.
     *   6. "A 1" — główny wyjątek (z ciała) ma komunikat A, a wyjątek z close() („B”) jest dopisany jako stłumiony.
     *   7. Przy NoSuchFileException nie: brak pliku jest błędem trwałym, ponowienie niczego nie zmieni. Przy chwilowym
     *      błędzie połączenia tak (z limitem prób i pauzami), bo takie błędy często znikają same.
     *   8. Pusty catch połyka wyjątek: program działa dalej z błędnymi (pustymi) danymi i nikt nie wie, że plik
     *      konfiguracji nie został wczytany. Trzeba albo obsłużyć konkretny przypadek świadomie (np. wartość
     *      domyślna dla NoSuchFileException), albo przekazać wyjątek wyżej (throws / opakowanie z przyczyną).
     */
    // </editor-fold>
}
