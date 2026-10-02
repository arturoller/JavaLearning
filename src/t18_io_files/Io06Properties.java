package t18_io_files;

import helpers.Check;
import helpers.TempDir;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.io.BufferedReader;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Properties — pliki konfiguracyjne klucz=wartość
 *        (properties = właściwości; key = klucz; value = wartość; default = wartość domyślna;
 *         load = wczytaj; store = zapisz; override = nadpisanie)
 *
 * W SKRÓCIE:
 *   java.util.Properties to mapa napisów (klucz → wartość), którą umie wczytać plik .properties i zapisać go z powrotem.
 *   To najprostszy format konfiguracji w Javie; w Springu to plik application.properties. Wszystkie wartości są
 *   napisami — liczby, true/false i czasy musisz sam zamienić na właściwy typ i sprawdzić.
 *
 * ANALOGIA: plik .properties to kartka z ustawieniami na lodówce: „wifi.haslo = ..., godzina.budzika = 6:30”.
 *   Każda linia to jedno ustawienie, a wszystko jest napisane długopisem (tekst) — to Ty musisz zrozumieć, że
 *   „6:30” to godzina, a „tak” to zgoda. Kartki można nakładać: ogólna (domyślna) + Twoja osobista, która ma pierwszeństwo.
 *
 * JAK TO DZIAŁA:
 *   Linia pliku:      klucz=wartość     klucz: wartość     klucz wartość      (trzy dozwolone separatory)
 *   Komentarz:        linia zaczynająca się od # albo !
 *   Ciąg dalszy:      ukośnik wsteczny na końcu linii łączy ją z następną
 *   Ucieczki:         \n \t \\ oraz zapis znaku jako ukośnik + u + cztery cyfry szesnastkowe
 *   Kodowanie:        load(Reader) — takie, jakie wybierzesz (używaj UTF-8);
 *                     load(InputStream) — ZAWSZE ISO-8859-1 (stary standard; polskie litery się psują)
 *
 *   Warstwy konfiguracji (od najsłabszej):  wartości domyślne → plik użytkownika → parametr -D / zmienna środowiskowa
 *   Uwaga: w tym pliku sekwencji ukośnik+u nie wpisujemy wprost w kodzie (kompilator zamieniłby je na znak jeszcze
 *   przed analizą); budujemy je z ukośnika (stała BS) i tekstu.
 *
 * SŁÓWKA:
 *   getProperty = pobierz właściwość; setProperty = ustaw właściwość; stringPropertyNames = nazwy kluczy (napisy);
 *   defaults = wartości domyślne; layer = warstwa; required = wymagany; missing = brakujący; range = zakres;
 *   continuation = ciąg dalszy; prefix = przedrostek; timeout = limit czasu; strict = ścisły.
 *
 * ZOBACZ TEŻ: t18_io_files/Io02ReadingText (czytanie plików tekstowych), t18_io_files/Io12Charsets (kodowania),
 *   t17_datetime/DateTime01LocalDateTime (czas), t18_io_files/Io05JsonManual (inny format konfiguracji)
 * </pre>
 */
public class Io06Properties {

    public static void main(String[] args) throws IOException {
        // throws IOException = „rzuca IOException” — main przekazuje wyjątek wyżej
        title("Io06 — Properties: konfiguracja klucz=wartość");

        Path dir = TempDir.create("io06"); // create = utwórz; wszystko znika w finally
        try {
            fileFormat();                    // file format = format pliku
            specialCharacters();             // special characters = znaki specjalne
            readerVersusStream(dir.resolve("s3")); // Reader versus Stream = Reader kontra strumień bajtów
            hashtableTraps();                // Hashtable traps = pułapki po Hashtable
            typedValues();                   // typed values = wartości z typem
            storing();                       // storing = zapisywanie
            layers(dir.resolve("s7"));       // layers = warstwy
            typedConfig();                   // typed config = konfiguracja z typami
            exercises();                     // exercises = ćwiczenia
        } finally {
            TempDir.deleteRecursively(dir);
        }
    }

    // =================================================================================================
    // Pomocnicze
    // =================================================================================================

    /** BS = ukośnik wsteczny (backslash). W kodzie piszemy go jako "\\". */
    private static final String BS = "\\";

    /**
     * rel = ścieżka względem katalogu bazowego, zawsze z ukośnikiem "/".
     * Po co? Katalog tymczasowy ma losową nazwę (innego nie wypisujemy), a Windows pisze ścieżki z "\"
     * (np. "a\b.txt"), Linux z "/" (np. "a/b.txt"). Zamiana na "/" sprawia, że wydruk jest taki sam wszędzie.
     */
    private static String rel(Path base, Path p) {
        return base.relativize(p).toString().replace('\\', '/');
    }

    /** Wartość, którą da się policzyć, ale obliczenie może rzucić IOException. */
    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }

    /** io = opakowuje IOException w UncheckedIOException, żeby pasowało do Supplier w Check.equal. */
    private static <T> java.util.function.Supplier<T> io(IoSupplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    /** fromText = wczytaj Properties z napisu (StringReader nie ma kodowania, więc nic się nie psuje). */
    private static Properties fromText(String text) {
        Properties p = new Properties();
        try (StringReader reader = new StringReader(text)) {
            p.load(reader); // load = wczytaj; rzuca IOException, której StringReader nigdy nie wywoła
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return p;
    }

    /** lines = linie napisu zapisane jawnym znakiem "\n" (nie separatorem systemu), więc tekst jest taki sam wszędzie. */
    private static String lines(String... lines) {
        return String.join("\n", lines) + "\n";
    }

    /** sorted = klucze=wartości w kolejności alfabetycznej (Properties nie gwarantuje żadnej kolejności). */
    private static String sorted(Properties p) {
        Map<String, String> map = new TreeMap<>();
        for (String key : p.stringPropertyNames()) { // stringPropertyNames = nazwy kluczy (razem z domyślnymi)
            map.put(key, p.getProperty(key));
        }
        return map.toString();
    }

    // =================================================================================================
    // 1. FORMAT PLIKU
    // =================================================================================================

    /**
     * 1. Trzy separatory (=, :, spacja), komentarze (# i !), białe znaki wokół klucza obcinane, puste wartości.
     * Najważniejsza niespodzianka: spacje na KOŃCU wartości zostają — to część wartości.
     */
    static void fileFormat() {
        section("1. Format pliku");

        Properties p = fromText(lines(
                "# komentarz zaczyna się od #",
                "! albo od wykrzyknika",
                "",
                "a=jeden",
                "b: dwa",
                "c trzy",
                "   d   =   cztery",
                "pusty=",
                "sam-klucz",
                "ogon=Ala   ",
                "dwa.slowa=ala ma kota",
                "powtorzony=pierwszy",
                "powtorzony=drugi"));

        show("a (znak =)", p.getProperty("a")); // getProperty = pobierz wartość klucza
        // WYNIK: a (znak =) → jeden
        show("b (dwukropek)", p.getProperty("b"));
        // WYNIK: b (dwukropek) → dwa
        show("c (spacja)", p.getProperty("c"));
        // WYNIK: c (spacja) → trzy
        show("d (spacje wokół klucza i na początku wartości)", p.getProperty("d"));
        // WYNIK: d (spacje wokół klucza i na początku wartości) → cztery
        show("pusty= daje pusty napis, nie null", "[" + p.getProperty("pusty") + "]");
        // WYNIK: pusty= daje pusty napis, nie null → []
        show("sam-klucz też daje pusty napis", "[" + p.getProperty("sam-klucz") + "]");
        // WYNIK: sam-klucz też daje pusty napis → []
        show("ogon: spacje na końcu zostają, długość", p.getProperty("ogon").length());
        // WYNIK: ogon: spacje na końcu zostają, długość → 6
        show("wartość ze spacjami w środku", p.getProperty("dwa.slowa"));
        // WYNIK: wartość ze spacjami w środku → ala ma kota
        show("powtórzony klucz: wygrywa ostatni", p.getProperty("powtorzony"));
        // WYNIK: powtórzony klucz: wygrywa ostatni → drugi
        show("liczba kluczy (komentarze nie liczą się)", p.size());
        // WYNIK: liczba kluczy (komentarze nie liczą się) → 9

        // PUŁAPKA: spacje na końcu wartości ("ogon=Ala   ") zostają. Takiej spacji nie widać w edytorze, a potem
        //   "Ala   " nie jest równe "Ala". Obcinaj wartości przy odczycie (strip) albo pilnuj edytora.
        // PUŁAPKA: literówka w kluczu NIE jest błędem: getProperty("poort") po prostu zwróci null.
        //   Dlatego przy odczycie podawaj wartość domyślną albo sprawdzaj, czy klucz jest wymagany (sekcja 5).

        // Konwencja nazw: małe litery i kropki jako „przestrzenie nazw”: db.url, db.user, app.name.
        // To tylko zwyczaj — Properties jest płaską mapą, kropki nic nie znaczą (ćw. 3 pokazuje, jak wybrać klucze z przedrostkiem).

        // DOBRA PRAKTYKA: używaj jednego separatora (=) i komentarzy nad kluczami. Dlaczego: zapis "klucz wartość" bez
        //   separatora jest poprawny, ale łatwo się pomylić, gdy wartość też zawiera spacje.
    }

    // =================================================================================================
    // 2. ZNAKI SPECJALNE
    // =================================================================================================

    /**
     * 2. Ukośnik wsteczny ma w pliku .properties znaczenie specjalne: łączy linie, zapisuje \n, \t i znaki Unicode.
     * Przez to ścieżki Windows (C:\temp) są pułapką — trzeba je pisać z podwójnym ukośnikiem albo z "/".
     */
    static void specialCharacters() {
        section("2. Znaki specjalne i ciąg dalszy linii");

        // Ciąg dalszy: ukośnik na końcu linii łączy ją z następną, a spacje na początku następnej są pomijane.
        Properties p = fromText(lines(
                "lista=pierwszy, \\",
                "       drugi, \\",
                "       trzeci",
                "tekst=wiersz1\\nwiersz2\\tkoniec",
                "sciezka=C:\\\\dane\\\\raport.txt",
                "urodziny=2000-01-01"));
        show("po złączeniu linii", p.getProperty("lista"));
        // WYNIK: po złączeniu linii → pierwszy, drugi, trzeci
        show("n w wartości daje nową linię", p.getProperty("tekst").contains("\n"));
        // WYNIK: n w wartości daje nową linię → true
        show("podwójny ukośnik daje jeden", p.getProperty("sciezka"));
        // WYNIK: podwójny ukośnik daje jeden → C:\dane\raport.txt

        // PUŁAPKA: (1) ścieżka Windows zapisana „normalnie”. Ukośnik + t to tabulator, ukośnik + n to nowa linia!
        Properties bad = fromText("katalog=C:" + BS + "temp" + BS + "new");
        String value = bad.getProperty("katalog");
        show("C:\\temp\\new po wczytaniu", value.replace("\t", "<TAB>").replace("\n", "<LF>"));
        // WYNIK: C:\temp\new po wczytaniu → C:<TAB>emp<LF>ew

        // PUŁAPKA: (2) ukośnik + u to początek znaku Unicode. "C:", ukośnik, "users" jest błędem, bo "sers" to nie cztery cyfry hex.
        try {
            fromText("katalog=C:" + BS + "users" + BS + "ola");
            System.out.println("✘ nie rzucono wyjątku");
        } catch (IllegalArgumentException e) {
            System.out.println("✔ ścieżka z ukośnikiem przed u → rzucono " + e.getClass().getSimpleName());
            // WYNIK: ✔ ścieżka z ukośnikiem przed u → rzucono IllegalArgumentException
        }

        // DOBRA PRAKTYKA: w plikach .properties pisz ścieżki z ukośnikiem "/" (działa też w Windows: C:/temp/new),
        //   albo z podwójnym ukośnikiem. Dlaczego: pojedynczy ukośnik zmienia znaczenie znaków po nim.

        // Znak zapisany ucieczką: ukośnik + u + cztery cyfry hex (np. litera ł to ukośnik, u i 0142):
        Properties unicode = fromText("miasto=x" + BS + "u0142" + BS + "u00f3" + "dz");
        show("zapis unicode w pliku", unicode.getProperty("miasto"));
        // WYNIK: zapis unicode w pliku → xłódz

        // Spacja na początku wartości jest pomijana; żeby ją zachować, trzeba ją poprzedzić ukośnikiem.
        // Dwukropek, znak równości i # w WARTOŚCI nie wymagają ucieczek przy odczycie ("url=http://x:80/a" działa),
        // ale wymagają ich w KLUCZU (bo kończą klucz), a store (sekcja 6) i tak zapisuje je z ucieczką.
        show("dwukropek w wartości", fromText("url=http://localhost:8080/app").getProperty("url"));
        // WYNIK: dwukropek w wartości → http://localhost:8080/app
    }

    // =================================================================================================
    // 3. READER KONTRA STRUMIEŃ BAJTÓW
    // =================================================================================================

    /**
     * 3. load(Reader) pozwala wybrać kodowanie (UTF-8). load(InputStream) zakłada ISO-8859-1 — to dziedzictwo czasów,
     * gdy pliki .properties miały być czysto ASCII, a polskie litery zapisywano ucieczkami. Od Javy 9 pakiety
     * zasobów (ResourceBundle) czytają UTF-8, ale Properties.load(InputStream) NIE zmieniło się.
     */
    static void readerVersusStream(Path base) throws IOException {
        section("3. load(Reader) kontra load(InputStream)");
        Files.createDirectory(base);

        Path file = base.resolve("app.properties");
        Files.writeString(file, "nazwa=zażółć\nmiasto=Łódź\n", StandardCharsets.UTF_8); // writeString = zapisz napis (Java 11+)

        // DOBRZE: Reader z jawnym UTF-8.
        Properties good = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { // newBufferedReader = nowy czytnik
            good.load(reader);
        }
        show("Reader + UTF-8", good.getProperty("nazwa") + " / " + good.getProperty("miasto"));
        // WYNIK: Reader + UTF-8 → zażółć / Łódź

        // ŹLE: strumień bajtów — Java czyta każdy bajt jako osobny znak Latin-1.
        Properties bad = new Properties();
        try (var in = Files.newInputStream(file)) { // newInputStream = nowy strumień bajtów
            bad.load(in);
        }
        String broken = bad.getProperty("nazwa");
        show("InputStream: początek zepsutego napisu", broken.substring(0, 4));
        // WYNIK: InputStream: początek zepsutego napisu → zaÅ¼
        show("InputStream: długość (było 6)", broken.length());
        // WYNIK: InputStream: długość (było 6) → 10
        // Polskie litery mają w UTF-8 po 2 bajty, a Latin-1 czyta każdy bajt jako osobny znak: stąd 4 litery × 2 + "za" = 10.
        // To jest „mojibake” (krzaki z niezgodnego kodowania) — więcej w t18_io_files/Io12Charsets.

        // Naprawa po fakcie: oddaj bajty tak, jak je odczytano (Latin-1) i odczytaj je poprawnie jako UTF-8.
        String repaired = new String(broken.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        show("naprawione", repaired);
        // WYNIK: naprawione → zażółć

        // Dawny sposób zapisu polskich liter w .properties: ucieczki Unicode. store(OutputStream) robi to sam:
        Properties polish = new Properties();
        polish.setProperty("nazwa", "zażółć");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); // w pamięci zamiast pliku
        polish.store(bytes, null); // pierwszy argument: strumień bajtów; drugi: komentarz (null = brak)
        List<String> written = new ArrayList<>();
        for (String line : bytes.toString(StandardCharsets.ISO_8859_1) /* toString(Charset): Java 10+ */.split("\\R")) { // \R = dowolny koniec linii
            if (!line.startsWith("#")) { // pomijamy linię z datą (zmienia się przy każdym uruchomieniu)
                written.add(line);
            }
        }
        show("store(OutputStream) zapisuje samo ASCII", written);
        // WYNIK: store(OutputStream) zapisuje samo ASCII → [nazwa=za\u017C\u00F3\u0142\u0107]
        Properties again = new Properties();
        again.load(new ByteArrayInputStream(bytes.toByteArray())); // ByteArrayInputStream = strumień z tablicy bajtów
        show("load(InputStream) rozumie te ucieczki", again.getProperty("nazwa"));
        // WYNIK: load(InputStream) rozumie te ucieczki → zażółć

        // PUŁAPKA: Properties.load(InputStream) NIE używa UTF-8. Plik zapisany w UTF-8 (IntelliJ i Notatnik tak robią)
        //   wczytany strumieniem bajtów daje krzaki. To jedno z najczęstszych źródeł „dziwnych znaków” w konfiguracji.
        // PUŁAPKA: load(Reader) bez podania kodowania (new FileReader(plik)) użyje kodowania SYSTEMU (w Javie 17 na
        //   polskim Windows: windows-1250; w Javie 18 i nowszych: UTF-8, JEP 400). Zawsze podawaj UTF-8 jawnie.
        // DOBRA PRAKTYKA: Files.newBufferedReader(plik, UTF_8) + load(reader). Dlaczego: kodowanie wybierasz Ty, a nie
        //   przypadek (system, wersja Javy), więc plik czyta się tak samo na każdym komputerze.
        show("plik: " + rel(base, file), Files.exists(file));
        // WYNIK: plik: app.properties → true
    }

    // =================================================================================================
    // 4. PUŁAPKI PO HASHTABLE
    // =================================================================================================

    /**
     * 4. Properties dziedziczy po Hashtable (stara mapa Object → Object), więc „z urzędu” przyjmuje dowolne obiekty
     * (put), choć sensowne są tylko napisy (setProperty). Kolejność kluczy jest nieokreślona.
     */
    static void hashtableTraps() {
        section("4. Pułapki po Hashtable");

        Properties p = new Properties();
        p.setProperty("port", "8080"); // setProperty = ustaw właściwość (tylko napisy)
        p.put("limit", 5);             // put = „wstaw” z klasy Hashtable: przyjmie Integer!
        p.setProperty("tryb", "prod");

        show("getProperty(\"port\")", p.getProperty("port"));
        // WYNIK: getProperty("port") → 8080
        show("getProperty(\"limit\") — wstawiono Integer", String.valueOf(p.getProperty("limit")));
        // WYNIK: getProperty("limit") — wstawiono Integer → null
        show("get(\"limit\") — to samo, ale przez Hashtable", p.get("limit"));
        // WYNIK: get("limit") — to samo, ale przez Hashtable → 5
        // getProperty widzi TYLKO wartości typu String; Integer wstawiony przez put jest dla niego niewidoczny.
        // Dodatkowo store() rzuciłoby ClassCastException na takiej wartości.

        // Kolejność nie jest gwarantowana → do wydruku i testów sortuj nazwy kluczy:
        show("klucze posortowane", new TreeSet<>(p.stringPropertyNames()));
        // WYNIK: klucze posortowane → [port, tryb]
        // stringPropertyNames zwraca tylko klucze, których klucz i wartość są napisami (stąd brak "limit").
        show("size() liczy też wpisy z put", p.size());
        // WYNIK: size() liczy też wpisy z put → 3

        // PUŁAPKA: p.put("limit", 5) „działa”, a getProperty zwraca null. Zawsze używaj setProperty (napisy).
        // PUŁAPKA: Properties to stara klasa (po Hashtable, bezpieczna wątkowo, ale tylko napisy); nowy kod, który tylko
        //   czyta, zwykle zamienia je od razu na niezmienny rekord z konfiguracją (sekcja 8).
        // DOBRA PRAKTYKA: nie wypisuj Properties przez println(p) ani nie porównuj zrzutów — kolejność jest przypadkowa.
        //   Dlaczego: ten sam program na innej wersji Javy lub komputerze wypisze klucze w innym porządku.
    }

    // =================================================================================================
    // 5. WARTOŚCI Z TYPEM
    // =================================================================================================

    /** Odczyt liczby całkowitej z zakresem; błąd mówi, którego klucza dotyczy. */
    static int intValue(Properties p, String key, int min, int max) {
        String raw = p.getProperty(key);
        if (raw == null) {
            throw new IllegalArgumentException("brak klucza '" + key + "'");
        }
        int value;
        try {
            value = Integer.parseInt(raw.strip()); // parseInt = zamień napis na int
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("klucz '" + key + "': oczekiwano liczby całkowitej, jest '" + raw + "'", e);
        }
        if (value < min || value > max) {
            throw new IllegalArgumentException("klucz '" + key + "': wartość " + value + " poza zakresem " + min + ".." + max);
        }
        return value;
    }

    /** Ścisły odczyt wartości logicznej: tylko true lub false (wielkość liter bez znaczenia). */
    static boolean strictBoolean(String key, String raw) {
        String text = raw.strip();
        if (text.equalsIgnoreCase("true")) { // equalsIgnoreCase = równe, ignorując wielkość liter
            return true;
        }
        if (text.equalsIgnoreCase("false")) {
            return false;
        }
        throw new IllegalArgumentException("klucz '" + key + "': oczekiwano true lub false, jest '" + raw + "'");
    }

    /**
     * 5. Wszystko w Properties to napis. Zamiana na liczbę, wartość logiczną czy czas wymaga walidacji.
     * Zasada: błąd w konfiguracji ma wskazać KLUCZ i oczekiwany format, a nie rzucić gołe NumberFormatException.
     */
    static void typedValues() {
        section("5. Wartości z typem i walidacja");

        Properties p = fromText(lines("port=8080", "zle=abc", "duzy=70000", "debug=tak", "timeout=PT30S", "zly.czas=30s"));

        show("getProperty z wartością domyślną", p.getProperty("nie.ma", "domyslna")); // drugi argument = wartość domyślna
        // WYNIK: getProperty z wartością domyślną → domyslna
        show("port", intValue(p, "port", 1, 65535));
        // WYNIK: port → 8080
        expectThrows("liczba z tekstu", () -> intValue(p, "zle", 1, 65535));
        // WYNIK: ✔ liczba z tekstu → rzucono IllegalArgumentException: klucz 'zle': oczekiwano liczby całkowitej, jest 'abc'
        expectThrows("poza zakresem", () -> intValue(p, "duzy", 1, 65535));
        // WYNIK: ✔ poza zakresem → rzucono IllegalArgumentException: klucz 'duzy': wartość 70000 poza zakresem 1..65535
        expectThrows("brak klucza", () -> intValue(p, "nie.ma", 1, 65535));
        // WYNIK: ✔ brak klucza → rzucono IllegalArgumentException: brak klucza 'nie.ma'

        // PUŁAPKA: Boolean.parseBoolean zwraca false dla KAŻDEGO napisu innego niż "true" — także dla "tak" i literówek.
        show("Boolean.parseBoolean(\"tak\")", Boolean.parseBoolean(p.getProperty("debug")));
        // WYNIK: Boolean.parseBoolean("tak") → false
        show("Boolean.parseBoolean(\"ture\")", Boolean.parseBoolean("ture"));
        // WYNIK: Boolean.parseBoolean("ture") → false
        expectThrows("ścisły odczyt boolean", () -> strictBoolean("debug", p.getProperty("debug")));
        // WYNIK: ✔ ścisły odczyt boolean → rzucono IllegalArgumentException: klucz 'debug': oczekiwano true lub false, jest 'tak'
        show("ścisły odczyt: TRUE", strictBoolean("debug", " TRUE "));
        // WYNIK: ścisły odczyt: TRUE → true

        // Czas: Duration.parse czyta format ISO-8601 (PT30S = 30 sekund, PT5M = 5 minut, P1D = doba).
        show("Duration.parse(\"PT30S\")", Duration.parse(p.getProperty("timeout")).toSeconds()); // toSeconds = w sekundach (Java 9+)
        // WYNIK: Duration.parse("PT30S") → 30
        try {
            Duration.parse(p.getProperty("zly.czas"));
        } catch (DateTimeParseException e) {
            System.out.println("✔ Duration.parse(\"30s\") → rzucono " + e.getClass().getSimpleName());
            // WYNIK: ✔ Duration.parse("30s") → rzucono DateTimeParseException
        }

        // Lista: napis dzielimy przecinkiem i obcinamy spacje.
        List<String> hosts = Arrays.stream("a.example.com, b.example.com ,, c.example.com".split(","))
                .map(String::strip) // strip = obetnij białe znaki (Java 11+)
                .filter(s -> !s.isEmpty())
                .toList(); // toList = zbierz do niezmiennej listy (Java 16+)
        show("lista z napisu", hosts);
        // WYNIK: lista z napisu → [a.example.com, b.example.com, c.example.com]

        // DOBRA PRAKTYKA: wartość domyślna dla ustawień opcjonalnych, błąd dla wymaganych — i zawsze z nazwą klucza
        //   w komunikacie. Dlaczego: „NumberFormatException: For input string "abc"” nie mówi, który klucz jest zły.
    }

    // =================================================================================================
    // 6. ZAPIS: store
    // =================================================================================================

    /**
     * 6. store(Writer, komentarz) zapisuje: linię komentarza, linię z datą, potem wpisy w NIEokreślonej kolejności.
     * Wszystkie końce linii to separator systemu (Windows: dwa znaki), więc przy wypisywaniu normalizujemy je do "\n",
     * pomijamy linię z datą (zmienia się za każdym razem) i sortujemy wpisy.
     */
    static void storing() throws IOException {
        section("6. Zapis: store");

        Properties p = new Properties();
        p.setProperty("url", "http://localhost:8080/app");
        p.setProperty("nazwa", "Zażółć gęślą");
        p.setProperty("klucz ze spacją", "a=b");

        StringWriter out = new StringWriter(); // StringWriter = zapis do napisu w pamięci
        p.store(out, "Konfiguracja");           // store = zapisz do Writera; drugi argument to komentarz

        List<String> all = out.toString().replace("\r\n", "\n").lines().toList(); // lines = linie napisu (Java 11+)
        show("pierwsza linia (komentarz)", all.get(0));
        // WYNIK: pierwsza linia (komentarz) → #Konfiguracja
        show("druga linia to data (zaczyna się od #)", all.get(1).startsWith("#"));
        // WYNIK: druga linia to data (zaczyna się od #) → true
        List<String> entries = new ArrayList<>(all.subList(2, all.size()));
        entries.sort(null); // null = naturalna kolejność napisów (alfabetyczna wg kodów znaków)
        showEach("wpisy po posortowaniu", entries);
        // WYNIK: wpisy po posortowaniu (liczba elementów: 3):
        // WYNIK: • klucz\ ze\ spacją=a\=b
        // WYNIK: • nazwa=Zażółć gęślą
        // WYNIK: • url=http\://localhost\:8080/app

        // Co zaszło: store wstawia ukośniki przed znakami, które mają znaczenie w formacie: spacja w kluczu, '=', ':'.
        // Zapis wygląda inaczej niż ręczny, ale po wczytaniu daje DOKŁADNIE to samo:
        Properties back = fromText(out.toString());
        show("wczytane z powrotem == oryginał", back.equals(p));
        // WYNIK: wczytane z powrotem == oryginał → true

        // PUŁAPKA: linia z datą zmienia się przy każdym zapisie, więc plik zapisany store() za każdym razem inny —
        //   nie nadaje się do repozytorium ani do porównań. Gdy zależy Ci na stałym pliku, napisz własną pętlę po
        //   posortowanych kluczach (i bez daty). Od Javy 21 (Java 21+) da się ustawić stałą datę (właściwość
        //   systemowa java.properties.date), ale w Javie 17 jej nie ma.
        // PUŁAPKA: store(Writer) zapisuje w kodowaniu Writera, store(OutputStream) — w ISO-8859-1 z ucieczkami (sekcja 3).
        // PUŁAPKA: store gubi KOMENTARZE z pliku wczytanego wcześniej — load ich nie zachowuje. Pliku edytowanego
        //   ręcznie nie nadpisuj przez load → store.

        // DOBRA PRAKTYKA: zapisuj do pliku przez Files.newBufferedWriter(plik, UTF_8) i p.store(writer, komentarz).
        //   Dlaczego: jawne UTF-8 i try-with-resources (zamknięcie) — jak w t18_io_files/Io03WritingText.
    }

    // =================================================================================================
    // 7. WARSTWY KONFIGURACJI
    // =================================================================================================

    /** Złóż konfigurację: wartości domyślne, na to plik użytkownika, na to parametr systemowy io06.port. */
    private static Properties layered(Properties defaults, Path userFile) throws IOException {
        Properties user = new Properties(defaults); // defaults = zapasowe wartości, gdy klucza nie ma w user
        if (Files.exists(userFile)) {
            try (BufferedReader reader = Files.newBufferedReader(userFile, StandardCharsets.UTF_8)) {
                user.load(reader);
            }
        }
        String override = System.getProperty("io06.port"); // getProperty z System = parametr -Dio06.port=...
        if (override != null) {
            user.setProperty("port", override);
        }
        return user;
    }

    /**
     * 7. Typowe zasady: ogólne ustawienia domyślne, plik użytkownika je nadpisuje, a parametr uruchomienia
     * (-Dnazwa=wartość) ma pierwszeństwo przed plikiem. Tak działa też Spring (zobacz niżej).
     */
    static void layers(Path base) throws IOException {
        section("7. Warstwy: domyślne → plik → parametr");
        Files.createDirectory(base);

        Properties defaults = fromText(lines("port=8080", "tryb=prod", "nazwa=Sklep"));
        Path userFile = base.resolve("user.properties");
        Files.writeString(userFile, lines("port=9090", "debug=true"), StandardCharsets.UTF_8);

        Properties config = layered(defaults, userFile);
        show("port (plik nadpisał domyślną)", config.getProperty("port"));
        // WYNIK: port (plik nadpisał domyślną) → 9090
        show("tryb (zostaje z domyślnych)", config.getProperty("tryb"));
        // WYNIK: tryb (zostaje z domyślnych) → prod
        show("debug (tylko w pliku)", config.getProperty("debug"));
        // WYNIK: debug (tylko w pliku) → true
        show("wszystkie klucze", sorted(config));
        // WYNIK: wszystkie klucze → {debug=true, nazwa=Sklep, port=9090, tryb=prod}

        // PUŁAPKA: domyślne wartości są „za” mapą: size(), keySet(), entrySet(), containsKey i get() ich NIE widzą,
        //   a getProperty i stringPropertyNames — tak.
        show("size() (bez domyślnych)", config.size());
        // WYNIK: size() (bez domyślnych) → 2
        show("get(\"tryb\") (bez domyślnych)", String.valueOf(config.get("tryb")));
        // WYNIK: get("tryb") (bez domyślnych) → null
        show("getProperty(\"tryb\")", config.getProperty("tryb"));
        // WYNIK: getProperty("tryb") → prod

        // Trzecia warstwa: parametr systemowy. Ustawiamy go na chwilę i ZAWSZE czyścimy (finally),
        // żeby nie zostawić śladu dla reszty programu.
        System.setProperty("io06.port", "7777"); // setProperty = ustaw parametr JVM (jak -Dio06.port=7777)
        try {
            show("port po -Dio06.port", layered(defaults, userFile).getProperty("port"));
            // WYNIK: port po -Dio06.port → 7777
        } finally {
            System.clearProperty("io06.port"); // clearProperty = usuń parametr
        }
        show("port po wyczyszczeniu parametru", layered(defaults, userFile).getProperty("port"));
        // WYNIK: port po wyczyszczeniu parametru → 9090

        // Brak pliku użytkownika to zwykły przypadek (pierwsze uruchomienie), a nie błąd:
        show("bez pliku użytkownika", layered(defaults, base.resolve("nie-ma.properties")).getProperty("port"));
        // WYNIK: bez pliku użytkownika → 8080

        // W Springu: application.properties (plik), application-dev.properties (profil dev), zmienne środowiskowe
        // (SERVER_PORT) i parametry (--server.port=9000) — w tej kolejności rosnącej ważności; to samo „warstwowanie”.
        // Pliki zasobów w JAR czyta się przez getResourceAsStream (SpringLearning pokazuje to bez ręcznego ładowania).

        // DOBRA PRAKTYKA: sekrety (hasła, klucze API) NIE należą do pliku .properties w repozytorium — czytaj je ze zmiennych
        //   środowiskowych lub z menedżera sekretów. Dlaczego: repozytorium zostaje na zawsze w historii gita.
        // PUŁAPKA: System.setProperty zmienia stan całego programu (efekt uboczny). Ustawiaj tylko na chwilę w testach
        //   i czyść w finally — inaczej inny test zobaczy zmienioną wartość.
    }

    // =================================================================================================
    // 8. KONFIGURACJA Z TYPAMI
    // =================================================================================================

    /** AppConfig = niezmienny rekord z ustawieniami aplikacji (typy zamiast napisów). */
    private record AppConfig(String name, int port, boolean debug, Duration timeout, List<String> hosts) {

        /** from = utwórz z Properties; zbiera WSZYSTKIE błędy naraz, żeby użytkownik poprawił je za jednym razem. */
        static AppConfig from(Properties p) {
            List<String> errors = new ArrayList<>();
            String name = p.getProperty("app.name");
            if (name == null || name.isBlank()) { // isBlank = pusty lub same spacje (Java 11+)
                errors.add("klucz 'app.name': wartość wymagana");
                name = "";
            }
            int port = 8080;
            try {
                port = intValue(p, "app.port", 1, 65535);
            } catch (IllegalArgumentException e) {
                if (p.getProperty("app.port") != null) { // brak klucza = zostaje domyślny 8080
                    errors.add(e.getMessage());
                }
            }
            boolean debug = false;
            if (p.getProperty("app.debug") != null) {
                try {
                    debug = strictBoolean("app.debug", p.getProperty("app.debug"));
                } catch (IllegalArgumentException e) {
                    errors.add(e.getMessage());
                }
            }
            Duration timeout = Duration.ofSeconds(30);
            if (p.getProperty("app.timeout") != null) {
                try {
                    timeout = Duration.parse(p.getProperty("app.timeout").strip());
                } catch (DateTimeParseException e) {
                    errors.add("klucz 'app.timeout': oczekiwano czasu ISO, np. PT30S, jest '" + p.getProperty("app.timeout") + "'");
                }
            }
            List<String> hosts = Arrays.stream(p.getProperty("app.hosts", "").split(","))
                    .map(String::strip).filter(s -> !s.isEmpty()).toList();
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException(String.join("; ", errors));
            }
            return new AppConfig(name, port, debug, timeout, hosts);
        }
    }

    /**
     * 8. Zamiast wołać getProperty po całym programie, zamieniamy Properties RAZ na niezmienny rekord. Reszta kodu
     * dostaje typy (int, boolean, Duration), a błędy konfiguracji wychodzą przy starcie, z nazwami kluczy.
     * Spring robi to samo adnotacją @ConfigurationProperties: wiąże klucze app.* z polami klasy lub rekordu.
     */
    static void typedConfig() {
        section("8. Konfiguracja z typami (rekord)");

        Properties good = fromText(lines(
                "app.name = Sklep zażółć",
                "app.port = 9090",
                "app.debug = true",
                "app.timeout = PT45S",
                "app.hosts = a.example.com, b.example.com"));
        show("poprawna konfiguracja", AppConfig.from(good));
        // WYNIK: poprawna konfiguracja → AppConfig[name=Sklep zażółć, port=9090, debug=true, timeout=PT45S, hosts=[a.example.com, b.example.com]]

        Properties minimal = fromText("app.name=Minimalny");
        show("same domyślne", AppConfig.from(minimal));
        // WYNIK: same domyślne → AppConfig[name=Minimalny, port=8080, debug=false, timeout=PT30S, hosts=[]]

        Properties bad = fromText(lines("app.port=abc", "app.debug=tak", "app.timeout=30s"));
        try {
            AppConfig.from(bad);
        } catch (IllegalArgumentException e) {
            for (String part : e.getMessage().split("; ")) {
                System.out.println("błąd konfiguracji → " + part);
            }
            // WYNIK: błąd konfiguracji → klucz 'app.name': wartość wymagana
            // WYNIK: błąd konfiguracji → klucz 'app.port': oczekiwano liczby całkowitej, jest 'abc'
            // WYNIK: błąd konfiguracji → klucz 'app.debug': oczekiwano true lub false, jest 'tak'
            // WYNIK: błąd konfiguracji → klucz 'app.timeout': oczekiwano czasu ISO, np. PT30S, jest '30s'
        }

        // Spring Boot: application.properties zawiera np. server.port=8080, spring.application.name=sklep,
        // a rekord @ConfigurationProperties("app") z polami name, port, debug, timeout dostaje te wartości
        // automatycznie (łącznie ze zamianą "45s" na Duration) — nie piszesz ręcznie tej klasy z sekcji 5.
        // W projekcie, który tylko używa Springa, dostajesz to za darmo, ale zasada zostaje ta sama: konfiguracja
        // to teksty, które trzeba zamienić na typy i sprawdzić — najlepiej raz, przy starcie.

        // DOBRA PRAKTYKA: waliduj całą konfigurację przy starcie i zgłoś wszystkie błędy naraz. Dlaczego: aplikacja,
        //   która pada po godzinie pracy, bo ktoś źle wpisał timeout, jest gorsza od takiej, która nie wstaje w ogóle.
        // DOBRA PRAKTYKA: rekord (niezmienny) zamiast Properties przekazywanego po programie. Dlaczego: nikt go nie
        //   zmieni przypadkiem, a literówka w kluczu wychodzi raz, w jednym miejscu.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Properties = mapa String → String; plik: klucz=wartość (też : i spacja), komentarze # i !.
     *   • Spacje na końcu wartości ZOSTAJĄ; brak klucza = null (getProperty(klucz, domyślna) ratuje).
     *   • Ukośnik wsteczny w wartości: łączy linie, \n, \t, \\ i znaki Unicode — ścieżki Windows pisz z "/" lub "\\".
     *   • load(Reader) + UTF-8 (jawnie!); load(InputStream) = ISO-8859-1 → polskie litery się psują.
     *   • Hashtable: put(String, Integer) „działa”, ale getProperty zwraca null; używaj setProperty. Kolejność kluczy
     *     przypadkowa → sortuj (stringPropertyNames do TreeSet).
     *   • Wartości zamieniaj na typy z walidacją i nazwą klucza w błędzie; Boolean.parseBoolean("tak") to false.
     *   • store: linia z datą, nieokreślona kolejność, znaki specjalne z ukośnikiem; nie nadaje się do porównań.
     *   • Warstwy: new Properties(defaults); size()/get() nie widzą domyślnych, getProperty tak.
     *   • Spring: application.properties, profile, @ConfigurationProperties — to samo, tylko za Ciebie.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego do wczytania pliku .properties z polskimi literami używamy load(Reader) z UTF-8, a nie load(InputStream)?
     *   2. Co jest wartością klucza "ogon" w linii  ogon=Ala   (trzy spacje na końcu)?
     *   3. Co wypisze:  Properties p = new Properties(); p.put("n", 5); System.out.println(p.getProperty("n"));  ?
     *   4. Co wypisze:  System.out.println(Boolean.parseBoolean("tak"));  ?
     *   5. ZNAJDŹ BŁĄD:  katalog=C:\temp\new   w pliku .properties — co naprawdę zostanie wczytane i jak to poprawić?
     *   6. ZNAJDŹ BŁĄD:  Properties p = new Properties(defaults); ... System.out.println(p.size());  — oczekujemy liczby
     *      wszystkich ustawień (razem z domyślnymi). Czemu wynik jest za mały i której metody użyć?
     *   7. Dlaczego nie opłaca się porównywać dwóch plików zapisanych przez store()?
     *   8. Jak w Springu nazywa się mechanizm, który zamienia klucze app.* na pola rekordu z typami?
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
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** reference = czy sprawdzamy rozwiązania wzorcowe (true) czy zaślepki ucznia (false). */
    private static void runExercises(boolean reference) {
        Path dir = TempDir.create("io06cw");
        try {
            Check.equal("ćw. 1: port z wartością domyślną", "9090|8080",
                    () -> (reference ? solution1("port=9090") : exercise1("port=9090")) + "|"
                            + (reference ? solution1("nazwa=x") : exercise1("nazwa=x")));

            Check.equal("ćw. 2: ścisły boolean", "true|false|błąd",
                    () -> bool2(reference, "TRUE") + "|" + bool2(reference, "false") + "|" + bool2(reference, "tak"));

            Check.equal("ćw. 3: podzbiór kluczy z przedrostkiem", Map.of("url", "x", "user", "sa"),
                    () -> reference
                            ? solution3(fromText("db.url=x\ndb.user=sa\napp.name=Z"), "db.")
                            : exercise3(fromText("db.url=x\ndb.user=sa\napp.name=Z"), "db."));

            Check.equal("ćw. 4: warstwy z plikiem UTF-8", "debug=true;nazwa=Zażółć;port=9090",
                    io(() -> {
                        Path user = dir.resolve("user.properties");
                        Files.writeString(user, "port=9090\ndebug=true\n", StandardCharsets.UTF_8);
                        Properties defaults = fromText("port=8080\nnazwa=Zażółć\n");
                        return reference ? solution4(defaults, user) : exercise4(defaults, user);
                    }));
        } finally {
            TempDir.deleteRecursively(dir);
        }
        Check.summary();
    }

    /** Pomoc dla ćw. 2: wynik jako "true"/"false", a IllegalArgumentException jako "błąd". */
    private static String bool2(boolean reference, String raw) {
        try {
            return String.valueOf(reference ? solution2(raw) : exercise2(raw));
        } catch (IllegalArgumentException e) {
            return "błąd";
        }
    }

    /**
     * ĆWICZENIE 1 (łatwe): wczytaj Properties z podanego tekstu (użyj fromText) i zwróć wartość klucza "port" jako int;
     * gdy klucza nie ma, zwróć 8080.
     * Podpowiedź: getProperty(klucz, "8080") i Integer.parseInt.
     */
    static int exercise1(String text) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ luźne Boolean.parseBoolean na ścisłe sprawdzanie. Stary kod:
     * <pre>{@code
     * return Boolean.parseBoolean(raw);   // "tak" daje false bez żadnego ostrzeżenia
     * }</pre>
     * Nowa wersja: "true" i "false" (bez względu na wielkość liter i spacje wokół) dają odpowiednią wartość,
     * każdy inny tekst rzuca IllegalArgumentException.
     * Podpowiedź: strip, equalsIgnoreCase (albo użyj gotowej metody strictBoolean z tej lekcji).
     */
    static boolean exercise2(String raw) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć mapę kluczy zaczynających się od przedrostka (prefix), z OBCIĘTYM przedrostkiem.
     * Dla db.url=x, db.user=sa, app.name=Z i prefiksu "db." wynik to mapa {url=x, user=sa}.
     * Podpowiedź: stringPropertyNames(), startsWith, substring(prefix.length()).
     */
    static Map<String, String> exercise3(Properties p, String prefix) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): złóż konfigurację z wartości domyślnych (defaults) i pliku użytkownika (UTF-8) —
     * plik nadpisuje domyślne — i zwróć wszystkie klucze jako "k=v;k=v" w kolejności alfabetycznej kluczy.
     * Podpowiedź: new Properties(defaults), Files.newBufferedReader z UTF_8, load, stringPropertyNames do TreeSet.
     */
    static String exercise4(Properties defaults, Path userFile) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(String text) {
        return Integer.parseInt(fromText(text).getProperty("port", "8080"));
    }

    static boolean solution2(String raw) {
        return strictBoolean("wartość", raw);
    }

    static Map<String, String> solution3(Properties p, String prefix) {
        Map<String, String> result = new TreeMap<>();
        for (String key : p.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                result.put(key.substring(prefix.length()), p.getProperty(key));
            }
        }
        return result;
    }

    static String solution4(Properties defaults, Path userFile) throws IOException {
        Properties config = new Properties(defaults);
        try (BufferedReader reader = Files.newBufferedReader(userFile, StandardCharsets.UTF_8)) {
            config.load(reader);
        }
        List<String> parts = new ArrayList<>();
        for (String key : new TreeSet<>(config.stringPropertyNames())) {
            parts.add(key + "=" + config.getProperty(key));
        }
        return String.join(";", parts);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. load(InputStream) zawsze czyta ISO-8859-1, więc każdy bajt UTF-8 staje się osobnym (złym) znakiem —
     *      polskie litery zamieniają się w krzaki. load(Reader) z UTF-8 pozwala sam wybrać kodowanie.
     *   2. "Ala   " — z trzema spacjami (6 znaków). Spacje na końcu wartości są jej częścią.
     *   3. null — put wstawił Integer, a getProperty widzi tylko wartości typu String.
     *   4. false — parseBoolean zwraca true tylko dla "true" (bez względu na wielkość liter); "tak" daje false.
     *   5. Ukośnik + t to tabulator, a ukośnik + n to nowa linia, więc wczytane zostanie "C:<TAB>emp<LF>ew". Popraw:
     *      katalog=C:/temp/new  albo  katalog=C:\\temp\\new.
     *   6. size() liczy tylko własne wpisy, bez wartości domyślnych. Użyj stringPropertyNames().size().
     *   7. Bo store() za każdym razem wstawia linię z bieżącą datą, a kolejność wpisów jest nieokreślona.
     *   8. @ConfigurationProperties (wiązanie właściwości z konfiguracją, ang. configuration properties binding).
     */
    // </editor-fold>
}
