package t18_io_files;

import helpers.Check;
import helpers.TempDir;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Archiwa ZIP i GZIP — pakowanie, rozpakowywanie i bezpieczeństwo
 *        (zip = zamek błyskawiczny, pliki spakowane razem; entry = wpis w archiwum; gzip = pakowanie jednego strumienia;
 *         checksum = suma kontrolna; zip slip = ucieczka z katalogu docelowego; zip bomb = bomba kompresyjna)
 *
 * W SKRÓCIE:
 *   ZIP to jeden plik, w którym leży wiele plików (wpisów), każdy osobno skompresowany. GZIP kompresuje jeden
 *   strumień bajtów (zwykle jeden plik: dane.txt.gz). Java ma to w standardzie, w pakiecie java.util.zip.
 *   Plik .jar, .docx, .xlsx, .odt i .apk to zwykłe archiwa ZIP z własną zawartością.
 *
 * ANALOGIA: ZIP to walizka z przegródkami, GZIP to próżniowy worek na jedną rzecz.
 *   W walizce każda przegródka (wpis) ma etykietę (nazwę) i zawartość. Spis przegródek leży na końcu walizki,
 *   więc można zajrzeć do dowolnej bez wypakowywania reszty (ZipFile). Worek próżniowy (GZIP) ma jedną rzecz
 *   i nie ma etykiet. Uważaj tylko na etykiety od obcych: przegródka „../../ukryte” próbuje cię wyprowadzić
 *   z magazynu (to właśnie zip slip).
 *
 * JAK TO DZIAŁA:
 *   Cztery sposoby pracy z ZIP-em:
 *   ZipOutputStream      →  pisanie: putNextEntry(nazwa), zapis bajtów, closeEntry(); katalog = nazwa kończy się "/"
 *   ZipInputStream       →  czytanie po kolei (strumień): getNextEntry() aż do null; nie da się skoczyć do wpisu
 *   ZipFile              →  czytanie swobodne: getEntry(nazwa), entries(), getInputStream(wpis); działa na pliku
 *   system plików ZIP    →  FileSystems.newFileSystem(plikZip, Map) — archiwum jak katalog: Files.copy, walk...
 *
 *   Nazwy wpisów: zawsze separator "/" (także w ZIP z Windows), Java zapisuje je w UTF-8, więc "zażółć.txt" jest OK.
 *   Wyjątki: IOException (sprawdzany), ZipException (podklasa: zepsute lub błędne archiwum, np. dwa wpisy o tej samej nazwie).
 *
 *   Bezpieczeństwo przy rozpakowywaniu OBCEGO archiwum:
 *   1. zip slip   — nazwa wpisu z "../" wychodzi poza katalog docelowy → sprawdź: cel.resolve(nazwa).normalize()
 *                    musi zaczynać się od katalogu docelowego
 *   2. zip bomb   — mały plik rozpakowuje się do gigabajtów → limit liczby wpisów i łącznego rozmiaru (liczonego
 *                    z faktycznie wczytanych bajtów, nie z nagłówka)
 *
 * SŁÓWKA:
 *   archive = archiwum; entry = wpis; compress = skompresuj; extract = rozpakuj; random access = dostęp swobodny;
 *   checksum = suma kontrolna; CRC = kontrola cykliczna (rodzaj sumy); limit = ograniczenie; target = cel; stored = bez kompresji;
 *   deflated = skompresowany algorytmem deflate; finish = zakończ (bez zamykania strumienia pod spodem).
 *
 * ZOBACZ TEŻ: t18_io_files/Io08BinaryStreams (strumienie bajtów, transferTo), t18_io_files/Io07WalkingDirectories
 *   (przechodzenie po drzewie), t18_io_files/Io12Charsets (kodowanie nazw), t18_io_files/Io11IoExceptions (wyjątki IO),
 *   t10_exceptions/Exceptions04TryWithResources (zamykanie zasobów)
 * </pre>
 */
public class Io13ZipArchives {

    // throws IOException = „rzuca IOException”; main przekazuje wyjątek wyżej
    public static void main(String[] args) throws IOException {
        title("Io13 — archiwa ZIP i GZIP");

        Path dir = TempDir.create("io13"); // create = utwórz
        try {
            zipInMemory();                          // zip in memory = ZIP w pamięci
            zipToFile(dir.resolve("s2"));           // zip to file = ZIP do pliku
            readingWithZipFile(dir.resolve("s3"));  // reading with ZipFile = czytanie przez ZipFile
            zipFileSystem(dir.resolve("s4"));       // zip file system = system plików ZIP
            gzip(dir.resolve("s5"));                // gzip = pakowanie GZIP
            checksums();                            // checksums = sumy kontrolne
            zipSlip(dir.resolve("s7"));             // zip slip = ucieczka z katalogu
            zipBomb();                              // zip bomb = bomba kompresyjna
            reproducibleZip();                      // reproducible = powtarzalny (te same bajty)
            exercises();                            // exercises = ćwiczenia
        } finally {
            TempDir.deleteRecursively(dir);
        }
    }

    // =================================================================================================
    // Pomocnicze
    // =================================================================================================

    /**
     * rel = ścieżka względem katalogu bazowego, zawsze z ukośnikiem "/".
     * Katalog tymczasowy ma losową nazwę, a Windows pisze ścieżki z "\" — zamiana na "/" daje ten sam wydruk wszędzie.
     */
    private static String rel(Path base, Path p) {
        return base.relativize(p).toString().replace('\\', '/');
    }

    /** Akcja, która może rzucić IOException (zwykły Runnable nie może). */
    @FunctionalInterface
    private interface ThrowingIo {
        void run() throws IOException;
    }

    /** Wartość, którą da się policzyć, ale obliczenie może rzucić IOException. */
    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }

    /**
     * ioFails = „IO zawodzi”. Oczekuje wyjątku IO i wypisuje jego nazwę klasy, a NIE komunikat: komunikaty
     * zawierają ścieżki i teksty zależne od systemu, więc wydruk różniłby się na Windows i na Linuksie.
     */
    private static void ioFails(String label, ThrowingIo action) {
        try {
            action.run();
            System.out.println("✘ " + label + " → NIE rzucono wyjątku (a spodziewaliśmy się go)");
        } catch (IOException e) {
            System.out.println("✔ " + label + " → rzucono " + e.getClass().getSimpleName());
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

    /** utf8 = tekst na bajty UTF-8 (skrót, żeby kod był krótszy). */
    private static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * zipBytes = buduje ZIP w pamięci z mapy „nazwa wpisu → treść”. Nazwa kończąca się "/" to wpis-katalog (bez treści).
     * Używamy ByteArrayOutputStream (zapis do tablicy bajtów w pamięci), więc nie potrzeba pliku.
     */
    private static byte[] zipBytes(Map<String, String> content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) { // zamknięcie dopisuje spis wpisów na końcu pliku
            for (Map.Entry<String, String> e : content.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey())); // putNextEntry = zacznij nowy wpis
                if (!e.getKey().endsWith("/")) {
                    zip.write(utf8(e.getValue()));
                }
                zip.closeEntry();                           // closeEntry = zakończ wpis
            }
        }
        return bytes.toByteArray();
    }

    /** entryNames = nazwy wpisów z ZIP-a przeczytanego strumieniowo, w kolejności zapisu. */
    private static List<String> entryNames(byte[] zip) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) { // getNextEntry = następny wpis, null = koniec
                names.add(entry.getName());
            }
        }
        return names;
    }

    /** hex = bajty jako liczby szesnastkowe (HexFormat — Java 17+). */
    private static String hex(byte[] bytes, int count) {
        return HexFormat.ofDelimiter(" ").formatHex(Arrays.copyOf(bytes, count));
    }

    // =================================================================================================
    // 1. ZIP W PAMIĘCI
    // =================================================================================================

    /**
     * 1. ZipOutputStream pisze archiwum do dowolnego OutputStream — tu do tablicy w pamięci.
     * Każdy wpis: putNextEntry → bajty → closeEntry. Nazwa zakończona "/" oznacza katalog.
     * ZipInputStream czyta wpisy po kolei: getNextEntry i czytanie bajtów do końca BIEŻĄCEGO wpisu.
     */
    static void zipInMemory() throws IOException {
        section("1. ZIP w pamięci: ZipOutputStream i ZipInputStream");

        Map<String, String> content = new LinkedHashMap<>(); // LinkedHashMap = zachowuje kolejność dodawania
        content.put("readme.txt", "Zażółć gęślą jaźń\n");
        content.put("dok/", "");                  // katalog: nazwa kończy się ukośnikiem
        content.put("dok/raport.txt", "Raport 2026\n");
        byte[] zip = zipBytes(content);

        show("pierwsze 4 bajty pliku ZIP", hex(zip, 4));
        // WYNIK: pierwsze 4 bajty pliku ZIP → 50 4b 03 04
        // 50 4b = litery "PK" (Phil Katz, twórca formatu), 03 04 = nagłówek pierwszego wpisu.
        show("nazwy wpisów (kolejność zapisu)", entryNames(zip));
        // WYNIK: nazwy wpisów (kolejność zapisu) → [readme.txt, dok/, dok/raport.txt]

        // Czytanie treści: po getNextEntry strumień ZipInputStream "jest" bieżącym wpisem — readAllBytes() czyta
        // tylko do końca TEGO wpisu (nie całego archiwum). Metoda read() sygnalizuje koniec wpisu wartością -1.
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (entry.isDirectory()) {                       // isDirectory = czy to katalog
                    show("katalog", entry.getName());
                    continue;
                }
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                show("wpis " + entry.getName(), text.trim() + " (znaków: " + text.length() + ")");
            }
        }
        // WYNIK: wpis readme.txt → Zażółć gęślą jaźń (znaków: 18)
        // WYNIK: katalog → dok/
        // WYNIK: wpis dok/raport.txt → Raport 2026 (znaków: 12)

        // PUŁAPKA: ZipInputStream nie wie z góry, ile bajtów ma wpis. Rozmiar (getSize) jest zwykle nieznany (-1),
        //   dopóki wpisu nie przeczytasz. Do czytania rozmiarów i skakania między wpisami służy ZipFile (sekcja 3).
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry first = in.getNextEntry();
            show("getSize() przed odczytem (strumień)", first == null ? "brak" : String.valueOf(first.getSize()));
            // WYNIK: getSize() przed odczytem (strumień) → -1
        }
        // PUŁAPKA: nie zamykaj strumienia „po każdym wpisie”: close() zamyka całe archiwum.
        //   Zamykasz całość raz (try-with-resources); między wpisami używasz closeEntry() lub po prostu getNextEntry().
        // PUŁAPKA: ZipOutputStream, którego nie zamkniesz (close lub finish), nie ma spisu wpisów na końcu —
        //   taki plik jest uszkodzony (programy zgłaszają „nieoczekiwany koniec archiwum”).
        //   finish() kończy archiwum, ale zostawia strumień pod spodem otwarty; close() robi finish + zamknięcie.
    }

    // =================================================================================================
    // 2. ZIP DO PLIKU
    // =================================================================================================

    /**
     * 2. Do pliku: Files.newOutputStream owinięty w ZipOutputStream. Nazwy wpisów: separator "/", polskie litery
     * OK (UTF-8). Błędy: dwa wpisy o tej samej nazwie → ZipException; wpis STORED (bez kompresji) wymaga
     * wcześniejszego podania rozmiaru i CRC.
     */
    static void zipToFile(Path dir) throws IOException {
        section("2. ZIP do pliku: nazwy, katalogi, metody pakowania");
        Files.createDirectories(dir);

        Path zipPath = dir.resolve("archiwum.zip");
        try (OutputStream out = Files.newOutputStream(zipPath);
             ZipOutputStream zip = new ZipOutputStream(out)) {   // domyślnie nazwy w UTF-8 (od Javy 7)
            zip.setLevel(9);                                      // setLevel = poziom kompresji 0–9 (9 = najmocniejsza)
            put(zip, "dok/", null);
            put(zip, "dok/zażółć.txt", "Treść pliku z polską nazwą\n");
            put(zip, "dok/dane.txt", "x".repeat(2000));           // repeat = powtórz (Java 11+): dobrze się kompresuje
        }
        show("rozmiar pliku zip > 0", Files.size(zipPath) > 0);
        // WYNIK: rozmiar pliku zip > 0 → true
        show("polska nazwa wpisu odczytana", entryNames(Files.readAllBytes(zipPath)).contains("dok/zażółć.txt"));
        // WYNIK: polska nazwa wpisu odczytana → true

        // PUŁAPKA: dwa wpisy o tej samej nazwie — ZipOutputStream zgłasza ZipException (to IOException).
        ioFails("dwa wpisy o tej samej nazwie", () -> {
            try (ZipOutputStream zip = new ZipOutputStream(new ByteArrayOutputStream())) {
                put(zip, "a.txt", "1");
                put(zip, "a.txt", "2");
            }
        });
        // WYNIK: ✔ dwa wpisy o tej samej nazwie → rzucono ZipException

        // Metody pakowania: DEFLATED (domyślna, kompresja) i STORED (bez kompresji, np. pliki już skompresowane: jpg, mp4).
        // STORED wymaga znania z góry rozmiaru i sumy CRC — bez nich ZipException już przy putNextEntry.
        ioFails("STORED bez rozmiaru i CRC", () -> {
            try (ZipOutputStream zip = new ZipOutputStream(new ByteArrayOutputStream())) {
                ZipEntry stored = new ZipEntry("surowy.bin");
                stored.setMethod(ZipEntry.STORED);          // STORED = bez kompresji
                zip.putNextEntry(stored);
                zip.write(new byte[] {1, 2, 3});
                zip.closeEntry();
            }
        });
        // WYNIK: ✔ STORED bez rozmiaru i CRC → rzucono ZipException

        // Nazwa wpisu NIE jest ścieżką systemu plików: zawsze "/", bez litery dysku, bez "\".
        // Budując nazwę z Path na Windows trzeba zamienić separator (tak jak robi rel(...) w tej lekcji).
        // DOBRA PRAKTYKA: nazwy wpisów buduj z ścieżki WZGLĘDNEM do pakowanego katalogu, zawsze z "/".
        //   Nazwy absolutne albo z "../" w archiwum, które TY tworzysz, to prośba o kłopoty u odbiorcy (sekcja 7).
        // Poziom 0–9: setLevel(0) = brak kompresji, 9 = najwolniej i najmniej bajtów. Domyślny (6) zwykle wystarcza.
    }

    /** put = dodaje wpis; content == null albo nazwa z "/" na końcu to katalog. */
    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        if (content != null && !name.endsWith("/")) {
            zip.write(utf8(content));
        }
        zip.closeEntry();
    }

    // =================================================================================================
    // 3. ZIPFILE
    // =================================================================================================

    /**
     * 3. ZipFile czyta spis wpisów z końca pliku, więc potrafi skoczyć do dowolnego wpisu bez czytania reszty
     * (dostęp swobodny). Zna rozmiary i CRC. Trzeba go zamknąć (implementuje AutoCloseable) — na Windows
     * otwarty ZipFile blokuje plik, którego wtedy nie da się usunąć.
     */
    static void readingWithZipFile(Path dir) throws IOException {
        section("3. ZipFile — swobodny dostęp do wpisów");
        Files.createDirectories(dir);
        Path zipPath = dir.resolve("a.zip");
        Map<String, String> content = new LinkedHashMap<>();
        content.put("b.txt", "bbb");
        content.put("a/", "");
        content.put("a/c.txt", "Zażółć gęślą jaźń\n");
        Files.write(zipPath, zipBytes(content));

        try (ZipFile zip = new ZipFile(zipPath.toFile())) { // ZipFile przyjmuje File (nie Path) — patrz uwaga niżej
            show("liczba wpisów (size)", zip.size());
            // WYNIK: liczba wpisów (size) → 3

            // entries() zwraca stary typ Enumeration (wyliczenie). Przerabiamy na listę, żeby posortować nazwy:
            // kolejność jest „jak w pliku”, nie gwarantujemy jej, więc ZAWSZE sortujemy przed wypisaniem.
            Enumeration<? extends ZipEntry> all = zip.entries();
            List<String> names = new ArrayList<>();
            for (ZipEntry e : Collections.list(all)) { // Collections.list = zamień Enumeration na List
                names.add(e.getName());
            }
            Collections.sort(names);
            show("nazwy posortowane", names);
            // WYNIK: nazwy posortowane → [a/, a/c.txt, b.txt]

            ZipEntry entry = zip.getEntry("a/c.txt");  // getEntry = znajdź wpis po nazwie (null, gdy brak)
            show("rozmiar a/c.txt (getSize)", entry.getSize());
            // WYNIK: rozmiar a/c.txt (getSize) → 27
            show("a/c.txt: czy skompresowany (getMethod == DEFLATED)", entry.getMethod() == ZipEntry.DEFLATED);
            // WYNIK: a/c.txt: czy skompresowany (getMethod == DEFLATED) → true
            try (InputStream in = zip.getInputStream(entry)) { // getInputStream = strumień z treścią tego wpisu
                show("treść", new String(in.readAllBytes(), StandardCharsets.UTF_8).trim());
            }
            // WYNIK: treść → Zażółć gęślą jaźń
            show("getEntry(\"nie-ma.txt\")", zip.getEntry("nie-ma.txt"));
            // WYNIK: getEntry("nie-ma.txt") → null
        }
        // Porównanie: ZipInputStream — jeden przebieg, mało pamięci, nie potrzeba pliku (np. dane z sieci).
        //             ZipFile        — wiele odczytów, znane rozmiary i CRC, tylko z pliku na dysku.
        // ZipFile ma konstruktory z File i ze String (oraz wersje z Charset — kodowanie nazw wpisów). Konstruktora z Path
        // nie ma (stąd zipPath.toFile()), ale system plików ZIP z następnej sekcji bierze Path.
        // PUŁAPKA: ZipFile.getInputStream tylko dla JEDNEGO wpisu — nie zamkniesz ZipFile przed końcem czytania.
        // DOBRA PRAKTYKA: wpis-katalog sprawdzaj przez isDirectory(), a nie przez „czy ma rozmiar 0” (pusty plik też ma 0).
    }

    // =================================================================================================
    // 4. SYSTEM PLIKÓW ZIP
    // =================================================================================================

    /**
     * 4. Najwygodniejszy sposób: potraktować archiwum ZIP jak zwykły katalog. FileSystems.newFileSystem(Path, Map)
     * (Java 13+) otwiera plik ZIP jako „system plików”, a potem działają Files.copy, writeString, walk, createDirectories.
     * Klucz "create" = "true" tworzy nowe archiwum. Zamknięcie systemu plików DOPISUJE zmiany do pliku.
     */
    static void zipFileSystem(Path dir) throws IOException {
        section("4. System plików ZIP (FileSystems.newFileSystem)");
        Files.createDirectories(dir);
        Path zipPath = dir.resolve("projekt.zip");
        Path zewnetrzny = dir.resolve("notatka.txt");
        Files.writeString(zewnetrzny, "Notatka z dysku\n", StandardCharsets.UTF_8);

        // Zapis: tworzymy archiwum i wkładamy do niego pliki jak do katalogu.
        // Map.of("create", "true") = ustawienia (env); Map.of = niezmienna mapa (Java 9+).
        try (FileSystem zipFs = FileSystems.newFileSystem(zipPath, Map.of("create", "true"))) {
            Path wZipie = zipFs.getPath("/dok/uwagi.txt");           // getPath = ścieżka WEWNĄTRZ archiwum
            Files.createDirectories(wZipie.getParent());              // jak zwykły katalog
            Files.writeString(wZipie, "Uwagi: zażółć gęślą jaźń\n", StandardCharsets.UTF_8);
            Files.copy(zewnetrzny, zipFs.getPath("/notatka.txt"));    // kopia z dysku do archiwum
        } // ← close() zapisuje archiwum na dysku
        show("plik zip istnieje po zamknięciu", Files.isRegularFile(zipPath));
        // WYNIK: plik zip istnieje po zamknięciu → true

        // Odczyt: otwieramy istniejące archiwum (Map.of() = bez ustawień) i chodzimy po nim jak po katalogu.
        try (FileSystem zipFs = FileSystems.newFileSystem(zipPath, Map.of());
             Stream<Path> walk = Files.walk(zipFs.getPath("/"))) {    // walk też trzeba zamknąć
            List<String> paths = walk.map(Path::toString).sorted().collect(Collectors.toList());
            show("zawartość archiwum", paths);
            // WYNIK: zawartość archiwum → [/, /dok, /dok/uwagi.txt, /notatka.txt]
            show("odczyt /dok/uwagi.txt", Files.readString(zipFs.getPath("/dok/uwagi.txt"), StandardCharsets.UTF_8).trim());
            // WYNIK: odczyt /dok/uwagi.txt → Uwagi: zażółć gęślą jaźń
            show("Files.exists(/brak.txt)", Files.exists(zipFs.getPath("/brak.txt")));
            // WYNIK: Files.exists(/brak.txt) → false

            // Wypakowanie jednego pliku na dysk: zwykłe Files.copy z ścieżki w ZIP do ścieżki na dysku.
            Path wypakowany = dir.resolve("wypakowany.txt");
            Files.copy(zipFs.getPath("/notatka.txt"), wypakowany);
            show("wypakowany plik", Files.readString(wypakowany, StandardCharsets.UTF_8).trim());
            // WYNIK: wypakowany plik → Notatka z dysku
        }
        // Zalety: jeden i ten sam kod (Files.*, Path) dla katalogu na dysku i dla archiwum; nie ma ręcznego
        // putNextEntry/closeEntry. Wady: zmiany są zapisywane przy zamknięciu systemu plików (przy dużych archiwach
        // może to trwać), a przy wielu drobnych zmianach archiwum bywa wolniejsze od ZipOutputStream.
        // Ścieżki wewnątrz archiwum mają ZAWSZE "/" (także na Windows), więc toString() jest tu przenośny.
        // Klucze ustawień: "create" ("true"), "encoding" (kodowanie nazw, domyślnie UTF-8).
        // PUŁAPKA: nie zamknięty system plików ZIP (brak try-with-resources) = zmiany nie trafiają do pliku,
        //   a na Windows plik zostaje zablokowany (nie da się go usunąć ani przenieść).
        // PUŁAPKA: wersja z Map i Path jest dopiero od Javy 13. W starszych przykładach z internetu spotkasz
        //   FileSystems.newFileSystem(URI.create("jar:file:/..."), env) albo newFileSystem(path, (ClassLoader) null) — działają dalej.
    }

    // =================================================================================================
    // 5. GZIP
    // =================================================================================================

    /**
     * 5. GZIP kompresuje jeden strumień (nie ma wpisów ani nazw). Typowy zapis: dane.txt.gz.
     * GZIPOutputStream/GZIPInputStream to dekoratory (nakładki) na zwykłe strumienie — pasują do OutputStream/InputStream.
     * Kilka plików w jednym archiwum .tar.gz to TAR + GZIP; samego TAR-a JDK nie ma.
     */
    static void gzip(Path dir) throws IOException {
        section("5. GZIP — kompresja jednego strumienia");
        Files.createDirectories(dir);

        String tekst = "Zażółć gęślą jaźń. ".repeat(500);          // tekst o powtarzalnej treści
        byte[] surowe = utf8(tekst);
        Path gz = dir.resolve("tekst.txt.gz");

        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(gz))) { // GZIPOutputStream = pakuj strumień
            out.write(surowe);
        }
        byte[] spakowane = Files.readAllBytes(gz);
        show("pierwsze 2 bajty (magiczna liczba GZIP)", hex(spakowane, 2));
        // WYNIK: pierwsze 2 bajty (magiczna liczba GZIP) → 1f 8b
        show("oryginał w bajtach", surowe.length);
        // WYNIK: oryginał w bajtach → 14000
        show("spakowane < oryginał", spakowane.length < surowe.length);
        // WYNIK: spakowane < oryginał → true
        // Dokładnego rozmiaru po kompresji NIE wypisujemy: zależy od wersji biblioteki zlib dołączonej do JDK.

        try (InputStream in = new GZIPInputStream(Files.newInputStream(gz))) {   // GZIPInputStream = rozpakuj strumień
            String odczytane = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            show("po rozpakowaniu równe oryginałowi", odczytane.equals(tekst));
            // WYNIK: po rozpakowaniu równe oryginałowi → true
        }

        // GZIP w pamięci (np. odpowiedź HTTP z nagłówkiem Content-Encoding: gzip):
        ByteArrayOutputStream pamiec = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(pamiec)) {
            out.write(utf8("abc"));
        } // close() dopisuje stopkę GZIP — bez niego pamiec.toByteArray() byłaby niekompletna
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(pamiec.toByteArray()))) {
            show("round trip w pamięci", new String(in.readAllBytes(), StandardCharsets.UTF_8));
            // WYNIK: round trip w pamięci → abc
        }

        // Dane, które nie są GZIP-em, kończą się wyjątkiem już przy otwarciu strumienia (nie przy czytaniu):
        ioFails("rozpakowanie zwykłego tekstu jako GZIP", () -> {
            try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(utf8("to nie jest gzip")))) {
                in.readAllBytes();
            }
        });
        // WYNIK: ✔ rozpakowanie zwykłego tekstu jako GZIP → rzucono ZipException

        // PUŁAPKA: kompresja nie zawsze zmniejsza. Dane już skompresowane (jpg, mp4, zip) albo losowe nie
        //   zmieniają się lub nawet rosną o nagłówek — pakowanie ich drugi raz to strata czasu procesora.
        // PUŁAPKA: GZIP nie przechowuje kodowania tekstu ani nazwy — przy odczycie musisz znać kodowanie
        //   (podajemy StandardCharsets.UTF_8 przy new String, patrz t18_io_files/Io12Charsets).
        // DOBRA PRAKTYKA: GZIP dla jednego strumienia (log, odpowiedź HTTP, plik JSON), ZIP dla wielu plików.
    }

    // =================================================================================================
    // 6. SUMY KONTROLNE
    // =================================================================================================

    /**
     * 6. CRC32 to krótka liczba (32 bity) wyliczona z bajtów — ZIP zapisuje ją przy każdym wpisie i przy rozpakowaniu
     * sprawdza, czy dane się nie zepsuły. Ta sama zawartość daje zawsze ten sam CRC32, więc wartość można wypisać.
     * Uwaga: CRC32 chroni przed uszkodzeniem przypadkowym, NIE przed celowym podrobieniem (do tego służą SHA-256).
     */
    static void checksums() throws IOException {
        section("6. Sumy kontrolne: CRC32");

        CRC32 crc = new CRC32();
        crc.update(utf8("123456789"));                // update = dołóż bajty do sumy
        show("CRC32 tekstu 123456789 (wartość wzorcowa)", Long.toHexString(crc.getValue()));
        // WYNIK: CRC32 tekstu 123456789 (wartość wzorcowa) → cbf43926

        crc.reset();                                  // reset = wyzeruj i zacznij od nowa
        crc.update(utf8("Ala ma kota"));
        show("CRC32 tekstu 'Ala ma kota'", Long.toHexString(crc.getValue()));
        // WYNIK: CRC32 tekstu 'Ala ma kota' → 33b6ab71

        // Jedna zmieniona litera — zupełnie inna suma:
        crc.reset();
        crc.update(utf8("Ala ma kotu"));
        show("CRC32 tekstu 'Ala ma kotu'", Long.toHexString(crc.getValue()));
        // WYNIK: CRC32 tekstu 'Ala ma kotu' → 296c7f0c

        // ZIP zapisał CRC każdego wpisu — ZipFile pokazuje go bez rozpakowywania:
        byte[] zip = zipBytes(Map.of("a.txt", "123456789"));
        Path tmp = TempDir.create("io13crc");
        try {
            Path zipPath = tmp.resolve("crc.zip");
            Files.write(zipPath, zip);
            try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
                ZipEntry entry = zipFile.getEntry("a.txt");
                show("CRC zapisany w ZIP", Long.toHexString(entry.getCrc()));
                // WYNIK: CRC zapisany w ZIP → cbf43926
            }
        } finally {
            TempDir.deleteRecursively(tmp);
        }
        // CRC32 zwraca long, ale mieści się w 32 bitach (liczba bez znaku) — nie rzutuj na int, bo dostaniesz ujemną.
        // Do sprawdzania „czy plik jest taki sam” w ważnych sprawach (pobrane programy, bezpieczeństwo) użyj
        // MessageDigest ("SHA-256"), a nie CRC32. Adler32 też jest w java.util.zip — szybszy, ale słabszy od CRC32.
        // PUŁAPKA: ZipInputStream sprawdza CRC dopiero po przeczytaniu CAŁEGO wpisu — przy uszkodzonym archiwum
        //   dostaniesz ZipException dopiero na końcu czytania, a wcześniejsze bajty wyglądały na dobre.
        //   ZipFile.getInputStream NIE sprawdza CRC wcale: jeśli ci zależy, policz CRC32 sam i porównaj z entry.getCrc().
    }

    // =================================================================================================
    // 7. ZIP SLIP
    // =================================================================================================

    /**
     * 7. Zip slip: archiwum od obcej osoby może zawierać wpis o nazwie "../../evil.txt". Naiwne rozpakowanie
     * (cel.resolve(nazwa)) zapisze plik POZA katalogiem docelowym — nawet w katalogu startowym systemu.
     * Obrona: policz ścieżkę, znormalizuj i sprawdź, czy dalej zaczyna się od katalogu docelowego.
     * (Demonstrujemy samo OBLICZANIE ścieżek — niczego nie zapisujemy poza katalogiem tymczasowym.)
     */
    static void zipSlip(Path base) throws IOException {
        section("7. Zip slip — nazwa wpisu, która ucieka z katalogu");
        Path target = base.resolve("wyjscie"); // katalog docelowy rozpakowania
        Files.createDirectories(target);

        // Ścieżki liczymy, ale NIE zapisujemy: rel(base, ...) pokazuje, gdzie by to trafiło względem katalogu lekcji.
        for (String name : List.of("ok/plik.txt", "../../evil.txt", "a/../../b.txt", "a/../b.txt")) {
            Path naiwny = target.resolve(name).normalize(); // normalize = uprość "a/../b" do "b"
            show("naiwnie " + name, rel(base, naiwny));
        }
        // WYNIK: naiwnie ok/plik.txt → wyjscie/ok/plik.txt
        // WYNIK: naiwnie ../../evil.txt → ../evil.txt
        // WYNIK: naiwnie a/../../b.txt → b.txt
        // WYNIK: naiwnie a/../b.txt → wyjscie/b.txt
        // Dwa wpisy z czterech uciekają z katalogu docelowego "wyjscie": pierwszy ląduje nawet POZA całym katalogiem
        // lekcji ("../evil.txt"), drugi obok katalogu docelowego ("b.txt"). Dwa pozostałe zostają w środku.

        // Ścieżka bezwzględna w nazwie też jest groźna: Path.resolve z argumentem bezwzględnym ZASTĘPUJE całą ścieżkę
        // bazową (target.resolve("/etc/x") daje "/etc/x"). Dlatego sprawdzenie startsWith jest konieczne.

        for (String name : List.of("ok/plik.txt", "../../evil.txt", "a/../../b.txt", "a/../b.txt", "/etc/passwd")) {
            show("bezpieczne? " + name, isSafeEntry(target, name));
        }
        // WYNIK: bezpieczne? ok/plik.txt → true
        // WYNIK: bezpieczne? ../../evil.txt → false
        // WYNIK: bezpieczne? a/../../b.txt → false
        // WYNIK: bezpieczne? a/../b.txt → true
        // WYNIK: bezpieczne? /etc/passwd → false

        // Pełne bezpieczne rozpakowanie: dla KAŻDEGO wpisu sprawdź nazwę, dopiero potem zapisuj.
        // Budujemy „złośliwe” archiwum (nazwa wpisu z "../" jest dozwolona przez ZipOutputStream — format ZIP nie
        // zabrania takich nazw, dlatego odpowiada rozpakowujący).
        Map<String, String> zlosliwe = new LinkedHashMap<>();
        zlosliwe.put("dobry.txt", "ok");
        zlosliwe.put("../../evil.txt", "zły");
        byte[] zip = zipBytes(zlosliwe);

        ioFails("bezpieczne rozpakowanie złośliwego archiwum", () -> extractSafely(zip, target));
        // WYNIK: ✔ bezpieczne rozpakowanie złośliwego archiwum → rzucono ZipException
        show("dobry.txt rozpakowany przed wykryciem", Files.exists(target.resolve("dobry.txt")));
        // WYNIK: dobry.txt rozpakowany przed wykryciem → true
        // (Rozpakowanie przerwano na złym wpisie, ale pierwszy, dobry zdążył się zapisać. Dla pełnej ostrożności
        // sprawdź wszystkie nazwy PRZED zapisem pierwszego pliku albo rozpakuj do osobnego katalogu i usuń go przy błędzie.)

        // PUŁAPKA: sprawdzanie tekstu nazwy ("zawiera ..") jest za słabe — ukośnik wsteczny, kodowanie nazwy,
        //   ścieżki absolutne i dowiązania symboliczne ominą takie filtry. Licz prawdziwą ścieżkę (normalize)
        //   i porównuj z katalogiem docelowym przez startsWith — porównanie po ELEMENTACH ścieżki, nie po tekście.
        // PUŁAPKA: target też normalizuj (target.normalize()), jeśli może zawierać "..".
        // Nazwa z "\" (np. "..\\evil.txt") jest zwykłą nazwą na Linuksie, ale separatorem na Windows —
        //   dlatego na Windows ta sama zła nazwa też zostanie wykryta tą samą metodą (normalize rozumie "\").
        // DOBRA PRAKTYKA: każde rozpakowywanie obcego archiwum = sprawdzenie ścieżki każdego wpisu + limity (sekcja 8).
        //   To znana luka (CVE „Zip Slip”) występowała w setkach bibliotek.
    }

    /** isSafeEntry = czy wpis o tej nazwie po rozpakowaniu zostanie w katalogu docelowym (a nie obok niego). */
    static boolean isSafeEntry(Path target, String entryName) {
        Path normalizedTarget = target.toAbsolutePath().normalize();
        Path resolved = normalizedTarget.resolve(entryName).normalize();
        return resolved.startsWith(normalizedTarget); // startsWith(Path) porównuje po ELEMENTACH ścieżki, nie po tekście
    }

    /** extractSafely = rozpakowuje ZIP z pamięci do katalogu; zła nazwa wpisu → ZipException, bez zapisu poza celem. */
    private static void extractSafely(byte[] zip, Path target) throws IOException {
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (!isSafeEntry(target, entry.getName())) {
                    throw new java.util.zip.ZipException("Podejrzana nazwa wpisu: " + entry.getName());
                }
                Path out = target.resolve(entry.getName()).normalize();
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.write(out, in.readAllBytes());
                }
            }
        }
    }

    // =================================================================================================
    // 8. BOMBA KOMPRESYJNA
    // =================================================================================================

    /**
     * 8. Bomba kompresyjna: bardzo mały plik ZIP/GZIP rozpakowuje się do ogromnej ilości danych (same zera kompresują się
     * ponad 1000 razy) i zapycha pamięć lub dysk. Obrona: limit wpisów i limit łącznej liczby WCZYTANYCH bajtów.
     * Nie ufaj rozmiarowi z nagłówka wpisu — atakujący może go podrobić. Liczysz to, co faktycznie wypływa ze strumienia.
     */
    static void zipBomb() throws IOException {
        section("8. Bomba kompresyjna — limit rozpakowanych bajtów");

        // „Bomba” w skali zabawki: 3 MB zer (kompresja zer jest ekstremalna).
        byte[] zera = new byte[3_000_000];
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("zera.bin"));
            zip.write(zera);
            zip.closeEntry();
        }
        byte[] bomba = bytes.toByteArray();
        show("spakowane < 1% oryginału", bomba.length < zera.length / 100);
        // WYNIK: spakowane < 1% oryginału → true

        // Z limitem 1 MB rozpakowanie przerywamy, zanim rozpakujemy cokolwiek groźnego:
        long limit = 1_000_000;
        ioFails("rozpakowanie z limitem 1 MB", () -> readWithLimit(bomba, limit));
        // WYNIK: ✔ rozpakowanie z limitem 1 MB → rzucono ZipException

        // Z wystarczającym limitem wszystko działa i dostajemy dokładną liczbę bajtów:
        show("rozpakowane bajty przy limicie 5 MB", readWithLimit(bomba, 5_000_000));
        // WYNIK: rozpakowane bajty przy limicie 5 MB → 3000000

        // PUŁAPKA: getSize() zapisane w nagłówku wpisu to tylko deklaracja — można ją zafałszować.
        //   Licz bajty w trakcie czytania (jak w readWithLimit).
        // PUŁAPKA: nie rób readAllBytes() na wpisie z obcego archiwum — ładuje CAŁOŚĆ do pamięci naraz.
        //   Czytaj porcjami (bufor kilku KB) i sumuj.
        // DOBRA PRAKTYKA: limity: maksymalna liczba wpisów, maksymalny rozmiar jednego wpisu, maksymalny rozmiar
        //   całości, a także maksymalny stosunek kompresji (np. więcej niż 100:1 to podejrzane). Dobierz do aplikacji.
    }

    /** readWithLimit = czyta wszystkie wpisy porcjami; po przekroczeniu limitu bajtów rzuca ZipException. Zwraca sumę bajtów. */
    private static long readWithLimit(byte[] zip, long limit) throws IOException {
        long total = 0;
        byte[] buffer = new byte[8192];                      // buffer = bufor na jedną porcję
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            while (in.getNextEntry() != null) {
                int read;                                     // read = ile bajtów faktycznie wczytano (może być mniej niż bufor!)
                while ((read = in.read(buffer)) != -1) {      // -1 = koniec bieżącego wpisu
                    total += read;
                    if (total > limit) {
                        throw new java.util.zip.ZipException("Przekroczono limit rozpakowanych danych: " + limit);
                    }
                }
            }
        }
        return total;
    }

    // =================================================================================================
    // 9. POWTARZALNE ARCHIWA
    // =================================================================================================

    /**
     * 9. Domyślnie wpis dostaje bieżący czas, więc dwa archiwa tej samej treści różnią się bajtami. Ustawiając stały
     * czas (setLastModifiedTime) i stałą kolejność wpisów dostajemy archiwum BAJT W BAJT takie samo —
     * to „powtarzalne budowanie” (reproducible builds), potrzebne np. do porównywania wersji i do cache.
     */
    static void reproducibleZip() throws IOException {
        section("9. Powtarzalne archiwum: stały czas i posortowane wpisy");

        // Kolejność wpisów: weź nazwy, POSORTUJ, dopiero pakuj (listy plików z dysku nie mają gwarantowanej kolejności).
        Map<String, String> files = Map.of("b.txt", "bbb", "a.txt", "aaa", "c/d.txt", "ddd"); // Map.of: kolejność przypadkowa!
        List<String> sorted = new ArrayList<>(files.keySet());
        Collections.sort(sorted);
        show("posortowane nazwy", sorted);
        // WYNIK: posortowane nazwy → [a.txt, b.txt, c/d.txt]

        byte[] first = zipFixedTime(files, sorted);
        byte[] second = zipFixedTime(files, sorted);
        show("dwa archiwa bajt w bajt takie same", Arrays.equals(first, second));
        // WYNIK: dwa archiwa bajt w bajt takie same → true

        // Dlaczego czas jest w ogóle potrzebny? ZIP zapisuje datę i godzinę w formacie DOS (z dokładnością do 2 s,
        // w czasie lokalnym!). Dlatego NIE wypisujemy czasów wpisów: zależą od strefy czasowej komputera.
        // PUŁAPKA: jar/zip budowane „na szybko” zawierają bieżący czas — dwa kolejne buildy różnią się,
        //   choć źródła są te same. Narzędzia buildów (Maven, Gradle) mają opcje powtarzalności (stały znacznik czasu).
        // JAR = ZIP: plik .jar to archiwum ZIP z katalogiem META-INF/ i plikiem MANIFEST.MF (opis: klasa startowa,
        //   wersja). W Javie służy do tego klasa java.util.jar.JarFile (podklasa ZipFile) — t30_build_modules.
    }

    private static byte[] zipFixedTime(Map<String, String> files, List<String> order) throws IOException {
        FileTime fixed = FileTime.from(Instant.parse("2026-01-01T00:00:00Z")); // FileTime = znacznik czasu pliku
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (String name : order) {
                ZipEntry entry = new ZipEntry(name);
                entry.setLastModifiedTime(fixed);     // stały czas zamiast „teraz”
                zip.putNextEntry(entry);
                zip.write(utf8(files.get(name)));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • ZIP = wiele wpisów (ZipEntry) z nazwami "a/b.txt"; katalog = nazwa kończy się "/"; nazwy w UTF-8.
     *   • Pisanie: ZipOutputStream (putNextEntry → write → closeEntry). Czytanie po kolei: ZipInputStream.
     *     Swobodnie: ZipFile (getEntry, entries). Jak katalog: FileSystems.newFileSystem(zip, Map) (Java 13+).
     *   • Zawsze zamykaj (try-with-resources): ZipOutputStream, ZipFile, system plików ZIP, strumienie GZIP.
     *   • GZIP = jeden strumień (GZIPOutputStream/GZIPInputStream), ZIP = wiele plików; .tar.gz = TAR + GZIP.
     *   • CRC32 = tania suma kontrolna wykrywająca uszkodzenia (nie podrobienia).
     *   • Zip slip: wpis "../../x" → sprawdź cel.resolve(nazwa).normalize().startsWith(cel). BEZ wyjątków.
     *   • Zip bomb: limit wpisów i bajtów liczonych w trakcie czytania, nie z nagłówka.
     *   • Rozmiar po kompresji zależy od biblioteki — nie testuj go na równość; testuj „mniejszy niż oryginał”.
     *   • jar, docx, xlsx, apk to ZIP-y.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się ZipInputStream od ZipFile? Kiedy który?
     *   2. Po czym poznać wpis-katalog w archiwum? Jaki separator mają nazwy wpisów na Windows?
     *   3. Co zwróci isSafeEntry(Path.of("/tmp/out"), "a/../b.txt") i isSafeEntry(Path.of("/tmp/out"), "../b.txt")?
     *   4. Co wypisze:  Path.of("/tmp/out2/x.txt").startsWith(Path.of("/tmp/out"))  ?
     *   5. ZNAJDŹ BŁĄD:  Path out = target.resolve(entry.getName()); Files.copy(in, out);  — w pętli po wpisach obcego ZIP-a.
     *   6. ZNAJDŹ BŁĄD:  ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(p));
     *      zip.putNextEntry(new ZipEntry("a.txt")); zip.write(bytes);   // koniec metody, bez close
     *   7. Dlaczego nie ufamy ZipEntry.getSize() przy ochronie przed bombą kompresyjną?
     *   8. Dlaczego CRC32 nie nadaje się do sprawdzenia, czy pobrany plik nie został podmieniony?
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

    /** Każde uruchomienie dostaje własny, świeży katalog; reference = czy sprawdzamy rozwiązania wzorcowe. */
    private static void runExercises(boolean reference) {
        Path dir = TempDir.create("io13cw");
        try {
            Map<String, String> content = new LinkedHashMap<>();
            content.put("readme.txt", "Zażółć\n");
            content.put("dok/", "");
            content.put("dok/a.txt", "AAA\n");
            byte[] zip = zipBytes(content);
            Path zipPath = dir.resolve("cw.zip");
            Files.write(zipPath, zip);

            Check.equal("ćw. 1: nazwy wpisów (posortowane)", "[dok/, dok/a.txt, readme.txt]", io(() ->
                    reference ? solution1(zip).toString() : exercise1(zip).toString()));

            Check.equal("ćw. 2: treść wpisu z ZIP-a", "AAA\n", io(() ->
                    reference ? solution2(zipPath, "dok/a.txt") : exercise2(zipPath, "dok/a.txt")));

            Path gz = dir.resolve("t.gz");
            try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(gz))) {
                out.write(utf8("Zażółć gęślą jaźń"));
            }
            Check.equal("ćw. 3: odczyt pliku .gz", "Zażółć gęślą jaźń", io(() ->
                    reference ? solution3(gz) : exercise3(gz)));

            Path target = dir.resolve("cel");
            Check.equal("ćw. 4: sprawdzanie nazwy wpisu", "true|false|true|false", () ->
                    (reference ? solution4(target, "x/y.txt") : exercise4(target, "x/y.txt")) + "|"
                            + (reference ? solution4(target, "../y.txt") : exercise4(target, "../y.txt")) + "|"
                            + (reference ? solution4(target, "x/../y.txt") : exercise4(target, "x/../y.txt")) + "|"
                            + (reference ? solution4(target, "x/../../y.txt") : exercise4(target, "x/../../y.txt")));

            Path src = dir.resolve("zrodlo");
            Files.createDirectories(src.resolve("dok"));
            Files.writeString(src.resolve("readme.txt"), "R\n", StandardCharsets.UTF_8);
            Files.writeString(src.resolve("dok/b.txt"), "B\n", StandardCharsets.UTF_8);
            Path out = dir.resolve("wynik.zip");
            Check.equal("ćw. 5: pakowanie katalogu", "dok/|dok/b.txt|readme.txt", io(() -> {
                if (reference) {
                    solution5(src, out);
                } else {
                    exercise5(src, out);
                }
                return String.join("|", entryNames(Files.readAllBytes(out)));
            }));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            TempDir.deleteRecursively(dir);
        }
        Check.summary();
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć posortowaną listę nazw wszystkich wpisów ZIP-a podanego jako tablica bajtów.
     * Podpowiedź: ZipInputStream + getNextEntry() w pętli, na końcu Collections.sort.
     */
    static List<String> exercise1(byte[] zip) throws IOException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć treść (UTF-8) jednego wpisu o podanej nazwie z pliku ZIP — bez rozpakowywania reszty.
     * Podpowiedź: ZipFile (zamknij go!), getEntry, getInputStream, readAllBytes.
     */
    static String exercise2(Path zipFile, String entryName) throws IOException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ odczyt pliku GZIP na try-with-resources.
     * <pre>{@code
     * // PRZED: ręczne zamykanie, łatwo o wyciek, gdy read rzuci wyjątek
     * InputStream in = null;
     * try {
     *     in = new GZIPInputStream(Files.newInputStream(gz));
     *     return new String(in.readAllBytes(), StandardCharsets.UTF_8);
     * } finally {
     *     if (in != null) { in.close(); }
     * }
     * }</pre>
     * Podpowiedź: try (InputStream in = new GZIPInputStream(Files.newInputStream(gz))) { ... }.
     */
    static String exercise3(Path gz) throws IOException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (średnie): zip slip. Zwróć true, jeśli wpis o nazwie entryName po rozpakowaniu do katalogu target
     * zostanie WEWNĄTRZ target. Nazwy "x/y.txt" i "x/../y.txt" są bezpieczne; "../y.txt" i "x/../../y.txt" — nie.
     * Podpowiedź: target.toAbsolutePath().normalize().resolve(nazwa).normalize() i startsWith(katalog).
     */
    static boolean exercise4(Path target, String entryName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): spakuj katalog source do pliku zipFile. Nazwy wpisów: względne do source, z "/",
     * posortowane alfabetycznie; katalogi jako wpisy z "/" na końcu (np. "dok/"); sam katalog source pomijamy.
     * Podpowiedź: Files.walk w try-with-resources, wyrzuć source, posortuj po tekście ścieżki z "/" (jak rel w tej lekcji),
     * dla katalogu putNextEntry(nazwa + "/") i closeEntry, dla pliku putNextEntry + Files.copy(plik, zip) + closeEntry.
     */
    static void exercise5(Path source, Path zipFile) throws IOException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(byte[] zip) throws IOException {
        List<String> names = entryNames(zip);
        Collections.sort(names);
        return names;
    }

    static String solution2(Path zipFile, String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                throw new java.util.zip.ZipException("Brak wpisu: " + entryName);
            }
            try (InputStream in = zip.getInputStream(entry)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    static String solution3(Path gz) throws IOException {
        try (InputStream in = new GZIPInputStream(Files.newInputStream(gz))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static boolean solution4(Path target, String entryName) {
        Path dir = target.toAbsolutePath().normalize();
        return dir.resolve(entryName).normalize().startsWith(dir);
    }

    static void solution5(Path source, Path zipFile) throws IOException {
        List<Path> paths;
        try (Stream<Path> walk = Files.walk(source)) {
            paths = walk.filter(p -> !p.equals(source))
                    .sorted((a, b) -> rel(source, a).compareTo(rel(source, b)))
                    .collect(Collectors.toList());
        }
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            for (Path p : paths) {
                String name = rel(source, p);
                if (Files.isDirectory(p)) {
                    zip.putNextEntry(new ZipEntry(name + "/"));
                } else {
                    zip.putNextEntry(new ZipEntry(name));
                    Files.copy(p, zip); // Files.copy(Path, OutputStream) — wpisuje całą zawartość pliku do strumienia
                }
                zip.closeEntry();
            }
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. ZipInputStream czyta archiwum po kolei jako strumień (jeden przebieg, mało pamięci, działa na danych z sieci,
     *      rozmiar wpisu zwykle nieznany). ZipFile czyta z pliku na dysku, zna spis wpisów, rozmiary i CRC i pozwala
     *      skoczyć do dowolnego wpisu. Strumień dla danych „przelotem”, ZipFile gdy trzeba wiele razy sięgać do wpisów.
     *   2. Nazwa kończy się "/" (isDirectory() jest true). Separator nazw wpisów to ZAWSZE "/", także na Windows.
     *   3. true (a/../b.txt to b.txt w środku katalogu) i false (../b.txt wychodzi poza /tmp/out).
     *   4. false: startsWith porównuje po ELEMENTACH ścieżki; "out2" to inny element niż "out". (Gdyby to były napisy,
     *      "/tmp/out2/x.txt".startsWith("/tmp/out") dałoby true — i to jest typowy błąd w filtrach tekstowych.)
     *   5. Brak sprawdzenia, czy nazwa wpisu nie ucieka z katalogu docelowego (zip slip): wpis "../../x" zapisze plik poza
     *      target. Trzeba: resolve(...).normalize() i startsWith(target.normalize()), a przy niepowodzeniu przerwać.
     *   6. Brak zamknięcia (try-with-resources, close albo finish): archiwum nie ma spisu wpisów na końcu i jest uszkodzone,
     *      a plik pozostaje otwarty (na Windows nie da się go usunąć). Brak też closeEntry (close sam to robi).
     *   7. getSize() pochodzi z nagłówka archiwum, który może napisać atakujący. Prawdziwy rozmiar to liczba bajtów,
     *      które faktycznie wypłynęły ze strumienia — ją trzeba liczyć w trakcie czytania.
     *   8. CRC32 wykrywa przypadkowe uszkodzenia, ale atakujący potrafi podrobić zawartość tak, by CRC32 się zgadzał.
     *      Do sprawdzania integralności z myślą o bezpieczeństwie służą funkcje skrótu SHA-256 (MessageDigest).
     */
    // </editor-fold>
}
