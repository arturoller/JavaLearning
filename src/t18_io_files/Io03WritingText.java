package t18_io_files;

import helpers.Check;
import helpers.TempDir;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Zapisywanie plików tekstowych — writeString, write, BufferedWriter, PrintWriter, flush i close
 *        (write = zapisz; writer = piszący, zapisywacz; append = dopisz; flush = opróżnij bufor; close = zamknij)
 *
 * W SKRÓCIE:
 *   Zapis jest groźniejszy od odczytu: łatwo nadpisać dobry plik, zgubić dane, które „siedzą w buforze”,
 *   albo zostawić plik w połowie zapisany. Ta lekcja pokazuje opcje otwarcia pliku, klasy piszące
 *   i wzorzec bezpiecznego zapisu: najpierw plik tymczasowy, potem atomowe przeniesienie.
 *
 * ANALOGIA: pisanie listu.
 *   Zapisujesz zdania w brudnopisie (bufor). Dopiero wrzucenie listu do skrzynki (flush/close) wysyła go do adresata.
 *   Zgaś światło przed wrzuceniem listu — i nic nie dotrze. Opcje otwarcia to decyzja: czy piszesz na nowej kartce,
 *   dopisujesz do starej, czy zamazujesz starą (TRUNCATE).
 *
 * JAK TO DZIAŁA:
 *   Files.writeString(p, tekst)                 → CREATE + TRUNCATE_EXISTING + WRITE (domyślnie!)
 *   opcja (StandardOpenOption)   znaczenie
 *   -------------------------    ---------------------------------------------------------------
 *   CREATE                       utwórz plik, jeśli go nie ma
 *   CREATE_NEW                   utwórz; jeśli plik JUŻ JEST → FileAlreadyExistsException
 *   TRUNCATE_EXISTING            skasuj dotychczasową zawartość (długość 0) przed zapisem
 *   APPEND                       dopisuj na końcu
 *   WRITE                        otwórz do zapisu
 *   Podanie JAKICHKOLWIEK opcji wyłącza domyślny zestaw: sama APPEND na nieistniejącym pliku → NoSuchFileException.
 *   Zwykle piszemy więc CREATE, APPEND razem.
 *
 *   program → BufferedWriter (bufor w pamięci) → kodowanie → strumień bajtów → dysk
 *   Dane trafiają na dysk dopiero po flush() lub close() (close robi flush i zwalnia plik).
 *
 * SŁÓWKA:
 *   truncate = obetnij, skasuj zawartość; atomic = niepodzielny; buffer = bufor (pamięć pośrednia);
 *   StringWriter = „piszący do napisu”; separator = separator (znak oddzielający linie); crash = awaria.
 *
 * ZOBACZ TEŻ: t18_io_files/Io02ReadingText (odczyt), t18_io_files/Io01PathFiles (move, ATOMIC_MOVE),
 *   t10_exceptions/Exceptions04TryWithResources (zamykanie zasobów), t18_io_files/Io04Csv (zapis raportu CSV)
 * </pre>
 */
public class Io03WritingText {

    public static void main(String[] args) throws IOException {
        title("Io03 — zapisywanie plików tekstowych");

        Path dir = TempDir.create("io03"); // create = utwórz
        try {
            writeStringOptions(dir);        // writeString options = opcje writeString
            writingInLoopPitfall(dir);      // writing in loop pitfall = pułapka zapisu w pętli
            filesWriteLines(dir);           // Files.write lines = Files.write z liniami
            bufferedWriter(dir);            // buffered writer = zapisywacz z buforem
            printWriter(dir);               // print writer = zapisywacz z printf
            flushAndClose(dir);             // flush and close = opróżnij i zamknij
            stringWriter();                 // string writer = zapis do napisu w pamięci
            atomicSave(dir);                // atomic save = zapis atomowy (bezpieczny)
            exercises();                    // exercises = ćwiczenia
        } finally {
            TempDir.deleteRecursively(dir);
        }
    }

    // =================================================================================================
    // Pomocnicze
    // =================================================================================================

    /** read = wczytaj plik jako napis (skrót, żeby wydruki były krótkie). */
    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
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
     * ioFails = „IO zawodzi”. Oczekuje wyjątku IO i wypisuje jego NAZWĘ oraz nazwę pliku (nie komunikat: komunikaty
     * IO zawierają pełną ścieżkę i tekst zależny od systemu, więc różniłyby się w Windows i na Linuksie).
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
    // 1. Files.writeString I OPCJE OTWARCIA
    // =================================================================================================

    /**
     * 1. Files.writeString (Java 11+) zapisuje napis jednym wywołaniem. Co się stanie z istniejącym plikiem,
     * decydują opcje otwarcia (StandardOpenOption).
     */
    static void writeStringOptions(Path base) throws IOException {
        section("1. Files.writeString i opcje otwarcia");

        Path file = base.resolve("notatka.txt");

        Files.writeString(file, "pierwsza\n"); // writeString = zapisz napis (Java 11+); domyślnie UTF-8
        show("po pierwszym zapisie", read(file).strip()); // strip = obetnij białe znaki z brzegów (Java 11+)
        // WYNIK: po pierwszym zapisie → pierwsza

        // DOMYŚLNIE plik jest NADPISYWANY (TRUNCATE_EXISTING): stara zawartość znika bez ostrzeżenia.
        Files.writeString(file, "druga\n");
        show("po drugim zapisie (domyślnie)", read(file).strip());
        // WYNIK: po drugim zapisie (domyślnie) → druga
        // PUŁAPKA: przypadkowe nadpisanie. Dwa wywołania writeString pod rząd NIE dopisują, tylko zastępują.
        //   Nie ma pytania „czy na pewno?” ani kosza. Dlatego świadomie wybieraj opcję (poniżej).

        // APPEND = dopisz na końcu (CREATE, żeby plik powstał, gdy go nie ma).
        Files.writeString(file, "trzecia\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        show("po dopisaniu (APPEND)", read(file).lines().toList()); // lines = linie napisu (Java 11+); toList (Java 16+)
        // WYNIK: po dopisaniu (APPEND) → [druga, trzecia]

        // CREATE_NEW = „utwórz tylko nowy”: ochrona przed nadpisaniem cudzego pliku.
        ioFails("CREATE_NEW na istniejącym pliku", () -> Files.writeString(file, "x", StandardOpenOption.CREATE_NEW));
        // WYNIK: ✔ CREATE_NEW na istniejącym pliku → rzucono FileAlreadyExistsException (plik: notatka.txt)
        show("plik nietknięty", read(file).lines().toList());
        // WYNIK: plik nietknięty → [druga, trzecia]
        Files.writeString(base.resolve("nowy.txt"), "ok\n", StandardOpenOption.CREATE_NEW);
        note("CREATE_NEW na nieistniejącym pliku działa jak zwykłe utworzenie.");
        // WYNIK: ℹ CREATE_NEW na nieistniejącym pliku działa jak zwykłe utworzenie.

        // Sama APPEND (bez CREATE) na nieistniejącym pliku: wyjątek, bo podanie opcji wyłącza domyślny zestaw.
        ioFails("APPEND bez CREATE, brak pliku", () -> Files.writeString(base.resolve("brak.txt"), "x", StandardOpenOption.APPEND));
        // WYNIK: ✔ APPEND bez CREATE, brak pliku → rzucono NoSuchFileException (plik: brak.txt)

        // Brak katalogu nadrzędnego to też NoSuchFileException — writeString nie tworzy katalogów.
        ioFails("zapis do nieistniejącego katalogu", () -> Files.writeString(base.resolve("nie-ma/x.txt"), "x"));
        // WYNIK: ✔ zapis do nieistniejącego katalogu → rzucono NoSuchFileException (plik: x.txt)
        // Rozwiązanie: Files.createDirectories(plik.getParent()) przed zapisem (Io01PathFiles).

        // PUŁAPKA: opcje bez TRUNCATE_EXISTING NIE skracają pliku — nowy tekst nadpisuje tylko początek:
        Path partial = base.resolve("czesciowo.txt");
        Files.writeString(partial, "abcdef");
        Files.writeString(partial, "XY", StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        show("WRITE + CREATE bez TRUNCATE_EXISTING", read(partial));
        // WYNIK: WRITE + CREATE bez TRUNCATE_EXISTING → XYcdef

        // DOBRA PRAKTYKA: w kodzie, który nie powinien niszczyć istniejących plików, dawaj CREATE_NEW.
        //   Dlaczego: błąd „plik już istnieje” jest głośny, a cicha utrata danych — nie.
        // DOBRA PRAKTYKA: kodowanie podawaj jawnie: Files.writeString(p, tekst, StandardCharsets.UTF_8, opcje...).
        //   Wersja bez kodowania też zapisuje UTF-8, ale metody ze starego java.io (FileWriter, PrintWriter(String)...)
        //   bez kodowania używają kodowania domyślnego systemu: w Javie 17 na polskim Windows to windows-1250.
    }

    // =================================================================================================
    // 2. PUŁAPKA: writeString W PĘTLI
    // =================================================================================================

    /**
     * 2. Każde wywołanie writeString otwiera plik od nowa i (domyślnie) go czyści. W pętli zostaje tylko ostatni wpis.
     */
    static void writingInLoopPitfall(Path base) throws IOException {
        section("2. Pułapka: zapis w pętli");

        List<String> names = List.of("Ala", "Bartek", "Celina");

        Path wrong = base.resolve("zle.txt");
        for (String name : names) {
            Files.writeString(wrong, name + "\n"); // ZŁE: każdy obrót pętli czyści plik
        }
        show("ZŁE: writeString w pętli", read(wrong).lines().toList());
        // WYNIK: ZŁE: writeString w pętli → [Celina]

        // Poprawka 1: APPEND. Działa, ale plik jest otwierany i zamykany przy każdym wpisie (wolno przy tysiącach linii).
        Path appended = base.resolve("append.txt");
        for (String name : names) {
            Files.writeString(appended, name + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        show("APPEND w pętli", read(appended).lines().toList());
        // WYNIK: APPEND w pętli → [Ala, Bartek, Celina]

        // Poprawka 2 (najlepsza do zapisu wielu linii naraz): jeden BufferedWriter na cały zapis.
        Path buffered = base.resolve("bufor.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(buffered, StandardCharsets.UTF_8)) {
            for (String name : names) {
                writer.write(name);
                writer.write('\n'); // jawny "\n": plik wygląda tak samo wszędzie (patrz sekcja 4 o newLine)
            }
        }
        show("jeden BufferedWriter", read(buffered).lines().toList());
        // WYNIK: jeden BufferedWriter → [Ala, Bartek, Celina]

        // Poprawka 3: zbuduj cały tekst i zapisz raz: String.join("\n", names) + "\n" albo StringBuilder.
        Path joined = base.resolve("razem.txt");
        Files.writeString(joined, String.join("\n", names) + "\n"); // join = sklej z separatorem
        show("zapis jednym writeString", read(joined).lines().toList());
        // WYNIK: zapis jednym writeString → [Ala, Bartek, Celina]
        // DOBRA PRAKTYKA: dużo linii → BufferedWriter. Mało danych naraz → jedno writeString. Dopisywanie do logu → APPEND.
    }

    // =================================================================================================
    // 3. Files.write(path, lines)
    // =================================================================================================

    /**
     * 3. Files.write z listą linii dodaje po każdej linii separator linii SYSTEMU (Windows: "\r\n", Linux: "\n").
     * Wygodne, ale rozmiar pliku zależy od systemu.
     */
    static void filesWriteLines(Path base) throws IOException {
        section("3. Files.write z listą linii");

        Path file = base.resolve("linie.txt");
        List<String> lines = List.of("zażółć", "gęślą", "jaźń");

        Files.write(file, lines, StandardCharsets.UTF_8); // write = zapisz; po każdej linii separator systemowy
        show("odczyt readAllLines", Files.readAllLines(file, StandardCharsets.UTF_8));
        // WYNIK: odczyt readAllLines → [zażółć, gęślą, jaźń]
        show("liczba linii", Files.readAllLines(file, StandardCharsets.UTF_8).size());
        // WYNIK: liczba linii → 3

        // Rozmiar pliku NIE jest tu wypisany celowo: w Windows każda linia kończy się dwoma bajtami (\r\n), na Linuksie
        // jednym (\n), więc ten sam kod daje pliki o różnym rozmiarze. Odczyt przez readAllLines tego nie widzi.
        // Gdy plik ma być IDENTYCZNY wszędzie (np. do porównania sum kontrolnych, do gita), pisz "\n" ręcznie:
        Path exact = base.resolve("dokladny.txt");
        Files.writeString(exact, String.join("\n", lines) + "\n");
        show("rozmiar przy jawnym \\n (w bajtach)", Files.size(exact));
        // WYNIK: rozmiar przy jawnym \n (w bajtach) → 27
        // Skąd 27: "zażółć" to 10 bajtów w UTF-8 (4 polskie litery po 2 bajty + 2 litery ASCII), "gęślą" to 8, "jaźń" to 6,
        // do tego trzy znaki "\n" po jednym bajcie: 10 + 8 + 6 + 3 = 27. W Windows po Files.write(lines) byłoby 30.
        show("zażółć w bajtach UTF-8", "zażółć".getBytes(StandardCharsets.UTF_8).length); // getBytes = zamień na bajty
        // WYNIK: zażółć w bajtach UTF-8 → 10

        // Przeciążenie Files.write(path, byte[]) zapisuje surowe bajty — zobacz Io08BinaryStreams. Opcje działają
        // tak samo jak w writeString: Files.write(p, lines, UTF_8, CREATE, APPEND) dopisze linie.

        // PUŁAPKA: Files.write(path, lines) przyjmie każdy Iterable, także Set — a w HashSet kolejność linii jest przypadkowa.
    }

    // =================================================================================================
    // 4. BufferedWriter
    // =================================================================================================

    /**
     * 4. BufferedWriter zbiera tekst w buforze i zapisuje go na dysk większymi porcjami. Wołanie write wiele razy
     * jest tanie. newLine() wstawia separator linii systemu.
     */
    static void bufferedWriter(Path base) throws IOException {
        section("4. BufferedWriter");

        Path file = base.resolve("raport.txt");

        // Files.newBufferedWriter(ścieżka, kodowanie, opcje...) — odpowiednik newBufferedReader z lekcji Io02.
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) { // BufferedWriter = zapisywacz z buforem
            writer.write("Raport sprzedaży"); // write = zapisz napis
            writer.newLine();                 // newLine = nowa linia (separator SYSTEMU)
            for (int i = 1; i <= 3; i++) {
                writer.write("pozycja " + i + ": zażółć");
                writer.newLine();
            }
            writer.write('#');                // write(char) = zapisz jeden znak
            writer.append(" koniec");         // append = dopisz (zwraca writera, można łańcuchowo)
        } // try-with-resources woła close(): opróżnia bufor i zamyka plik, nawet gdy poleci wyjątek
        showEach("zawartość", Files.readAllLines(file, StandardCharsets.UTF_8));
        // WYNIK: zawartość (liczba elementów: 5):
        // WYNIK: • Raport sprzedaży
        // WYNIK: • pozycja 1: zażółć
        // WYNIK: • pozycja 2: zażółć
        // WYNIK: • pozycja 3: zażółć
        // WYNIK: • # koniec

        // Dlaczego newLine(), a nie write("\n")? newLine() wstawia separator właściwy dla systemu: w Windows "\r\n",
        // na Linuksie "\n". Edytory Windows (np. stary Notatnik) lepiej znoszą "\r\n". Za to plik różni się rozmiarem
        // między systemami. Wybór: plik dla ludzi i narzędzi systemowych → newLine(); plik, który ma być identyczny
        // wszędzie (testy, git, sumy kontrolne, protokoły) → write("\n").

        // Dopisywanie: opcje podajemy po kodowaniu — newBufferedWriter(file, UTF_8, CREATE, APPEND).

        // PUŁAPKA: new FileWriter(plik) / new FileWriter("nazwa") bez kodowania pisze w kodowaniu domyślnym systemu
        //   (w Javie 17 na polskim Windows: windows-1250). Plik czytany potem jako UTF-8 ma krzaczki. Wersja z jawnym
        //   kodowaniem to FileWriter(plik, UTF_8) (Java 11+), a najprościej Files.newBufferedWriter(path, UTF_8).
        // PUŁAPKA: domyślne opcje newBufferedWriter to CREATE + TRUNCATE_EXISTING — plik jest czyszczony przy otwarciu,
        //   zanim cokolwiek zapiszesz. Nawet pusty try nadpisze plik pustym.
        // DOBRA PRAKTYKA: BufferedWriter zawsze w try-with-resources. Dlaczego: close() wypycha bufor na dysk —
        //   bez niego ostatnie dane mogą zniknąć (sekcja 6), a w Windows plik pozostanie zablokowany.
    }

    // =================================================================================================
    // 5. PrintWriter
    // =================================================================================================

    /**
     * 5. PrintWriter ma wygodne print, println i printf (formatowanie jak String.format). Dla pliku opakowujemy
     * BufferedWriter. Haczyk: PrintWriter NIE rzuca IOException — połyka ją.
     */
    static void printWriter(Path base) throws IOException {
        section("5. PrintWriter i printf");

        Path file = base.resolve("tabela.txt");

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) { // PrintWriter = zapisywacz z print/printf
            out.printf(Locale.ROOT, "%-8s|%8.2f\n", "kawa", 64.99); // printf = formatuj i zapisz; Locale.ROOT = bez ustawień językowych
            out.printf(Locale.ROOT, "%-8s|%8.2f\n", "czekolada", 7.5);
            out.print("razem");   // print = zapisz bez końca linii
            out.print('\n');
            out.println("koniec"); // println = zapisz i dodaj separator SYSTEMU (w Windows \r\n)
        }
        showEach("tabela", Files.readAllLines(file, StandardCharsets.UTF_8));
        // WYNIK: tabela (liczba elementów: 4):
        // WYNIK: • kawa    |   64.99
        // WYNIK: • czekolada|    7.50
        // WYNIK: • razem
        // WYNIK: • koniec

        // Locale w printf: bez niego użyte byłyby ustawienia językowe komputera. Polskie: "64,99" (przecinek),
        // angielskie: "64.99" (kropka). Plik z danymi dla programów (CSV, JSON) powinien mieć stałą kropkę: Locale.ROOT.
        show("pl-PL", String.format(Locale.forLanguageTag("pl-PL"), "%.2f", 3.5));
        // WYNIK: pl-PL → 3,50
        show("ROOT ", String.format(Locale.ROOT, "%.2f", 3.5));
        // WYNIK: ROOT  → 3.50
        // W formacie piszemy "\n" zamiast %n: %n daje separator systemu (w Windows \r\n), więc rozmiar pliku by się różnił.

        // PUŁAPKA: PrintWriter nie ma throws IOException. Błędy zapisu (dysk pełny, zamknięty strumień) są po cichu
        //   zapamiętywane w fladze i odczytasz je tylko przez checkError(). Przykład: pisanie po zamknięciu.
        PrintWriter closed = new PrintWriter(Files.newBufferedWriter(base.resolve("zamkniety.txt"), StandardCharsets.UTF_8));
        closed.close();
        closed.println("to nie trafi do pliku"); // żaden wyjątek!
        show("checkError() po zapisie do zamkniętego", closed.checkError()); // checkError = sprawdź błąd (true = był błąd)
        // WYNIK: checkError() po zapisie do zamkniętego → true

        // DOBRA PRAKTYKA: do pisania plików, w których błąd zapisu jest ważny (dane użytkownika), używaj BufferedWriter
        //   (rzuca IOException). PrintWriter zostaw do raportów i podglądu, i po zapisie wołaj checkError().
        //   Dlaczego: cicha utrata danych jest gorsza niż głośny wyjątek.
    }

    // =================================================================================================
    // 6. flush KONTRA close
    // =================================================================================================

    /**
     * 6. Bufor zatrzymuje dane w pamięci. flush() wypycha je do pliku, ale zostawia plik otwarty. close() robi flush
     * i zwalnia plik. Bez któregokolwiek z nich dane mogą nigdy nie dotrzeć na dysk.
     */
    static void flushAndClose(Path base) throws IOException {
        section("6. flush kontra close — dane w buforze");

        Path file = base.resolve("bufor-demo.txt");

        // Demonstracja BŁĘDU: zapis bez flush i bez close. Zamykamy dopiero na końcu, żeby nie zostawić otwartego pliku.
        BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
        try {
            writer.write("zginie");
            show("rozmiar pliku po write (bez flush/close)", Files.size(file));
            // WYNIK: rozmiar pliku po write (bez flush/close) → 0
            note("Dane siedzą w buforze w pamięci — plik na dysku jest wciąż pusty.");
            // WYNIK: ℹ Dane siedzą w buforze w pamięci — plik na dysku jest wciąż pusty.

            writer.flush(); // flush = opróżnij bufor do pliku (plik zostaje otwarty)
            show("rozmiar po flush", Files.size(file));
            // WYNIK: rozmiar po flush → 6
            writer.write(" i więcej");
            show("rozmiar po kolejnym write (bez flush)", Files.size(file));
            // WYNIK: rozmiar po kolejnym write (bez flush) → 6
        } finally {
            writer.close(); // close = flush + zamknięcie pliku. W prawdziwym kodzie robi to try-with-resources.
        }
        show("po close", read(file));
        // WYNIK: po close → zginie i więcej
        show("rozmiar po close", Files.size(file));
        // WYNIK: rozmiar po close → 16

        // Dlaczego „zginie”? Gdyby program zakończył się albo poleciał wyjątek BEZ close(), zawartość bufora przepadłaby:
        // Java nie opróżnia buforów BufferedWriter automatycznie przy końcu programu ani przy sprzątaniu pamięci (GC).
        // Plik zostałby pusty albo ucięty w losowym miejscu — a błąd zauważyłbyś dopiero przy odczycie.

        // close() robi flush, więc to właśnie close() może rzucić IOException o błędzie zapisu (np. dysk pełny): dane
        // zapisują się dopiero w tym momencie! Dlatego try-with-resources z close() NIE wolno łykać wyjątku po cichu.
        // Wielokrotne close() jest bezpieczne (kolejne wywołania nic nie robią).

        // flush() po każdej linii spowalnia zapis. Używaj go, gdy dane muszą być od razu widoczne dla innego programu
        // (typowo logi, Io10SimpleLogger), a nie „na wszelki wypadek”.

        // PUŁAPKA: flush() NIE zamyka pliku. W Windows otwartego pliku często nie da się usunąć ani przenieść.
        // DOBRA PRAKTYKA: try-with-resources zamiast ręcznego flush/close. Dlaczego: zamyka się zawsze, także po wyjątku.
    }

    // =================================================================================================
    // 7. StringWriter — tekst w pamięci
    // =================================================================================================

    /**
     * 7. StringWriter zbiera tekst w pamięci (nie dotyka dysku). Dobry do zbudowania całego raportu, a potem
     * zapisania go jednym ruchem — albo do testów kodu, który przyjmuje Writer.
     */
    static void stringWriter() throws IOException {
        section("7. StringWriter — zapis do napisu w pamięci");

        StringWriter buffer = new StringWriter(); // StringWriter = zapisywacz do napisu
        try (PrintWriter out = new PrintWriter(buffer)) {
            out.printf(Locale.ROOT, "%s: %.1f\n", "średnia", 4.25);
            out.print("koniec\n");
        }
        String text = buffer.toString(); // toString = pobierz zebrany tekst
        show("zebrany tekst", text.lines().toList());
        // WYNIK: zebrany tekst → [średnia: 4.3, koniec]
        // (printf zaokrągla w górę przy remisie, HALF_UP: 4.25 → 4.3. Dla pieniędzy używaj BigDecimal, nie double.)
        show("długość w znakach", text.length());
        // WYNIK: długość w znakach → 20

        // Kod, który pisze do Writera, napisze i do pliku, i do StringWriter — w testach sprawdzasz wtedy napis zamiast plików.
        // printStackTrace(PrintWriter) pozwala „złapać” ślad wyjątku do napisu (np. do logu).
        // StringWriter nie trzyma żadnego zasobu systemowego: jego close() nic nie robi (ale deklaruje IOException).
    }

    // =================================================================================================
    // 8. ZAPIS ATOMOWY: plik tymczasowy + przeniesienie
    // =================================================================================================

    /** Zapisuje nowy plik przez plik tymczasowy; po awarii w trakcie zapisu stary plik zostaje nietknięty. */
    private static void saveAtomically(Path target, String content, boolean crashHalfway) throws IOException {
        // 1) Plik tymczasowy w TYM SAMYM katalogu: przenoszenie atomowe działa tylko w obrębie jednego dysku.
        Path tmp = Files.createTempFile(target.getParent(), "zapis-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                writer.write(content.substring(0, content.length() / 2));
                if (crashHalfway) {
                    throw new IllegalStateException("awaria w połowie zapisu"); // symulacja awarii
                }
                writer.write(content.substring(content.length() / 2));
            }
            // 2) Cały nowy plik gotowy — dopiero teraz podmieniamy stary jednym, niepodzielnym ruchem.
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) { // system plików nie umie ruchu atomowego
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp); // po sukcesie plik tmp już nie istnieje (przeniesiony); po awarii sprzątamy
        }
    }

    /**
     * 8. Zapis „wprost” do docelowego pliku jest groźny: gdy program padnie w połowie, plik zostaje ucięty
     * (a stara wersja już przepadła przez TRUNCATE). Bezpieczny wzorzec: zapisz do pliku tymczasowego obok, a
     * po udanym zapisie przenieś go na miejsce docelowe (move z ATOMIC_MOVE).
     */
    static void atomicSave(Path base) throws IOException {
        section("8. Bezpieczny zapis: plik tymczasowy + move");

        Path config = base.resolve("konfiguracja.txt");

        // ZŁE: zapis wprost do docelowego pliku, awaria w połowie.
        Files.writeString(config, "wersja 1: komplet danych\n");
        try (BufferedWriter writer = Files.newBufferedWriter(config, StandardCharsets.UTF_8)) { // tu plik zostaje WYCZYSZCZONY
            writer.write("wersja 2: pol");
            writer.flush();
            throw new IllegalStateException("awaria w połowie zapisu");
        } catch (IllegalStateException e) {
            show("ZŁE: po awarii plik zawiera", read(config));
            // WYNIK: ZŁE: po awarii plik zawiera → wersja 2: pol
            note("Stara wersja przepadła, nowa jest ucięta — nie mamy żadnej poprawnej.");
            // WYNIK: ℹ Stara wersja przepadła, nowa jest ucięta — nie mamy żadnej poprawnej.
        }

        // DOBRZE: ten sam scenariusz przez saveAtomically.
        Files.writeString(config, "wersja 1: komplet danych\n");
        try {
            saveAtomically(config, "wersja 2: komplet nowych danych\n", true);
        } catch (IllegalStateException e) {
            show("DOBRZE: po awarii plik zawiera", read(config).strip());
            // WYNIK: DOBRZE: po awarii plik zawiera → wersja 1: komplet danych
        }
        try (Stream<Path> files = Files.list(base)) { // list = lista plików katalogu (Io07WalkingDirectories)
            show("zostały pliki zapis-*.tmp po awarii", files.anyMatch(p -> p.getFileName().toString().startsWith("zapis-")));
            // WYNIK: zostały pliki zapis-*.tmp po awarii → false
        }

        saveAtomically(config, "wersja 2: komplet nowych danych\n", false);
        show("po udanym zapisie plik zawiera", read(config).strip());
        // WYNIK: po udanym zapisie plik zawiera → wersja 2: komplet nowych danych

        // Dlaczego to działa? Przeniesienie w obrębie jednego dysku to jedna, niepodzielna operacja systemu: czytelnik
        // zobaczy albo CAŁĄ starą wersję, albo CAŁĄ nową — nigdy połowę. Przed przeniesieniem stary plik leży nietknięty.
        // Plik tymczasowy jest w tym samym katalogu, bo przeniesienie między dyskami to kopiowanie (nie atomowe):
        // Java zgłosi AtomicMoveNotSupportedException, a my wracamy do zwykłego move z REPLACE_EXISTING.
        // Dla pełnej odporności na nagły zanik prądu potrzebne jest jeszcze FileChannel.force (wymuszenie zapisu na dysk).

        // DOBRA PRAKTYKA: dane, których nie wolno stracić (konfiguracja, zapisany stan), zapisuj przez plik tymczasowy
        //   + atomowy move. Dlaczego: awaria w trakcie zapisu zostawia starą, poprawną wersję.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • writeString/write domyślnie NADPISUJĄ (CREATE + TRUNCATE_EXISTING); APPEND dopisuje (z CREATE), CREATE_NEW
     *     nie pozwala nadpisać (FileAlreadyExistsException).
     *   • Podanie opcji wyłącza domyślny zestaw: sama APPEND na nieistniejącym pliku = NoSuchFileException.
     *   • writeString w pętli zostawia tylko ostatni wpis — użyj jednego BufferedWriter (albo APPEND, wolniej).
     *   • Files.write(lines) i newLine()/println dają separator SYSTEMU (Windows \r\n); chcesz identycznie — pisz "\n".
     *   • PrintWriter: wygodny printf, ale połyka IOException (checkError); w printf podawaj Locale.ROOT.
     *   • BufferedWriter trzyma dane w buforze: flush opróżnia, close = flush + zwolnienie pliku. Zawsze try-with-resources.
     *   • Bezpieczny zapis: plik tymczasowy w tym samym katalogu + Files.move(ATOMIC_MOVE) + sprzątanie w finally.
     *   • Kodowanie podawaj jawnie (UTF_8); FileWriter i PrintWriter(String) bez niego używają kodowania domyślnego.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie, gdy dwa razy z rzędu wywołasz Files.writeString(p, "a") i Files.writeString(p, "b")?
     *   2. Czym różni się flush od close? Który z nich musi być wywołany, żeby plik się zwolnił?
     *   3. Co wypisze:  Files.writeString(p, "abcdef");  Files.writeString(p, "XY", WRITE, CREATE);
     *      System.out.println(Files.readString(p));  ?
     *   4. Co wypisze:  Files.writeString(p, "x", APPEND);  gdy plik p nie istnieje (tylko ta jedna opcja)?
     *   5. ZNAJDŹ BŁĄD:  for (String s : lista) { Files.writeString(p, s + "\n"); }  — chcemy mieć wszystkie linie.
     *   6. ZNAJDŹ BŁĄD:  BufferedWriter w = Files.newBufferedWriter(p, UTF_8);  w.write("dane");  — i koniec metody.
     *   7. Dlaczego zapis atomowy tworzy plik tymczasowy w TYM SAMYM katalogu, a nie w katalogu systemowym?
     *   8. Dlaczego PrintWriter jest ryzykowny do zapisu ważnych danych i co robi checkError()?
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

    /** Każde uruchomienie ma własny katalog tymczasowy; reference = czy sprawdzamy rozwiązania wzorcowe. */
    private static void runExercises(boolean reference) {
        Path dir = TempDir.create("io03cw");
        try {
            Path log = dir.resolve("dziennik.txt");
            Check.equal("ćw. 1: dopisz linię", "pierwsza\ndruga\ntrzecia\n", io(() -> {
                Files.writeString(log, "pierwsza\ndruga\n");
                if (reference) {
                    solution1(log);
                } else {
                    exercise1(log);
                }
                return read(log);
            }));

            Path polish = dir.resolve("polskie.txt");
            Check.equal("ćw. 2: zapis linii przez BufferedWriter", "zażółć\ngęślą\n", io(() -> {
                if (reference) {
                    solution2(polish, List.of("zażółć", "gęślą"));
                } else {
                    exercise2(polish, List.of("zażółć", "gęślą"));
                }
                return read(polish).replace("\r\n", "\n");
            }));

            Path unique = dir.resolve("unikalny.txt");
            Check.equal("ćw. 3: CREATE_NEW", "zapisano|już istnieje|pierwszy", io(() -> {
                String first = reference ? solution3(unique, "pierwszy") : exercise3(unique, "pierwszy");
                String second = reference ? solution3(unique, "drugi") : exercise3(unique, "drugi");
                return first + "|" + second + "|" + read(unique);
            }));

            Path report = dir.resolve("raport.txt");
            Map<String, Integer> data = new java.util.LinkedHashMap<>();
            data.put("a", 1);
            data.put("b", 2);
            Check.equal("ćw. 4: raport (PRZEPISZ)", List.of("Raport", "a=1", "b=2"), io(() -> {
                if (reference) {
                    solution4(report, data);
                } else {
                    exercise4(report, data);
                }
                return Files.readAllLines(report, StandardCharsets.UTF_8);
            }));

            Path safeDir = dir.resolve("bezpieczny");
            Files.createDirectory(safeDir);
            Path safe = safeDir.resolve("dane.txt");
            Check.equal("ćw. 5: zapis atomowy", "nowa treść|1", io(() -> {
                Files.writeString(safe, "stara treść");
                if (reference) {
                    solution5(safe, "nowa treść");
                } else {
                    exercise5(safe, "nowa treść");
                }
                try (Stream<Path> files = Files.list(safeDir)) {
                    return read(safe) + "|" + files.count();
                }
            }));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            TempDir.deleteRecursively(dir);
        }
        Check.summary();
    }

    /**
     * ĆWICZENIE 1 (łatwe): dopisz do istniejącego pliku jedną linię "trzecia\n" (bez kasowania tego, co jest).
     * Podpowiedź: Files.writeString z opcjami CREATE i APPEND.
     */
    static void exercise1(Path file) throws IOException {
        // TODO: twoje rozwiązanie
    }

    /**
     * ĆWICZENIE 2 (łatwe): zapisz podane linie do pliku (UTF-8), każdą zakończoną znakiem nowej linii.
     * Użyj BufferedWriter z Files.newBufferedWriter i try-with-resources.
     * Podpowiedź: write(linia) i newLine() — test odczytuje plik niezależnie od separatora systemu.
     */
    static void exercise2(Path file, List<String> lines) throws IOException {
        // TODO: twoje rozwiązanie
    }

    /**
     * ĆWICZENIE 3 (średnie): zapisz tekst do pliku tylko wtedy, gdy plik jeszcze nie istnieje. Zwróć "zapisano"
     * albo "już istnieje" (gdy plik był) — istniejącego pliku nie zmieniaj.
     * Podpowiedź: opcja CREATE_NEW i złapanie FileAlreadyExistsException (bez wstępnego exists — luka czasowa).
     */
    static String exercise3(Path file, String text) throws IOException {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 4 (średnie): PRZEPISZ ze starego stylu. Stary kod (kodowanie domyślne, ręczne zamykanie):
     * <pre>{@code
     * FileWriter fw = new FileWriter(path);
     * PrintWriter pw = new PrintWriter(fw);
     * pw.println("Raport");
     * for (Map.Entry<String, Integer> e : data.entrySet()) {
     *     pw.println(e.getKey() + "=" + e.getValue());
     * }
     * pw.close();     // niezamknięty po wyjątku, błędy zapisu połknięte
     * }</pre>
     * Nowa wersja: BufferedWriter z UTF-8 i try-with-resources; plik ma mieć linie: Raport, a=1, b=2.
     * Podpowiedź: newLine() po każdej linii.
     */
    static void exercise4(Path file, Map<String, Integer> data) throws IOException {
        // TODO: twoje rozwiązanie
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): zapisz tekst do pliku target w sposób bezpieczny: najpierw do pliku tymczasowego
     * w tym samym katalogu, potem Files.move z ATOMIC_MOVE. Po zakończeniu w katalogu ma zostać tylko plik target.
     * Podpowiedź: Files.createTempFile(target.getParent(), "tmp-", ".tmp"); sprzątanie w finally (deleteIfExists).
     */
    static void exercise5(Path target, String text) throws IOException {
        // TODO: twoje rozwiązanie
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static void solution1(Path file) throws IOException {
        Files.writeString(file, "trzecia\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    static void solution2(Path file, List<String> lines) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    static String solution3(Path file, String text) throws IOException {
        try {
            Files.writeString(file, text, StandardOpenOption.CREATE_NEW);
            return "zapisano";
        } catch (FileAlreadyExistsException e) {
            return "już istnieje";
        }
    }

    static void solution4(Path file, Map<String, Integer> data) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("Raport");
            writer.newLine();
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue());
                writer.newLine();
            }
        }
    }

    static void solution5(Path target, String text) throws IOException {
        Path tmp = Files.createTempFile(target.getParent(), "tmp-", ".tmp");
        try {
            Files.writeString(tmp, text);
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Plik będzie zawierał tylko "b": domyślne opcje to CREATE + TRUNCATE_EXISTING, więc drugi zapis kasuje
     *      to, co zapisał pierwszy. Żeby dopisać, trzeba APPEND.
     *   2. flush wypycha dane z bufora do pliku, ale zostawia plik otwarty; close robi flush i zwalnia plik.
     *      Zwolnić plik może tylko close (flush nie wystarcza).
     *   3. XYcdef — bez TRUNCATE_EXISTING nowy tekst nadpisuje tylko początek, reszta starej zawartości zostaje.
     *   4. NoSuchFileException — podanie opcji wyłącza domyślny zestaw (z CREATE), więc plik nie zostanie utworzony.
     *   5. Każdy obrót pętli czyści plik (domyślnie TRUNCATE_EXISTING) — zostanie tylko ostatnia linia. Użyj jednego
     *      BufferedWriter albo Files.write(p, lista), albo APPEND (wolniej).
     *   6. Writer nie jest zamknięty ani opróżniony: dane z bufora nie trafią do pliku (plik zostanie pusty), a w
     *      Windows plik pozostanie zablokowany. Trzeba try-with-resources.
     *   7. Atomowe przeniesienie działa tylko w obrębie jednego systemu plików. Katalog systemowy bywa na innym
     *      dysku, a wtedy przeniesienie to kopiowanie i usuwanie (nie atomowe, AtomicMoveNotSupportedException).
     *   8. PrintWriter nie rzuca IOException — błędy zapisu zapamiętuje po cichu w fladze. checkError() opróżnia
     *      bufor i zwraca true, jeśli kiedykolwiek wystąpił błąd. Bez jego sprawdzenia nie wiesz, że dane przepadły.
     */
    // </editor-fold>
}
