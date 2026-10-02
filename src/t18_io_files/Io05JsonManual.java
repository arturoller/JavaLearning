package t18_io_files;

import helpers.Check;
import helpers.TempDir;
import helpers.SampleData;
import helpers.model.Product;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: JSON „na piechotę” — własny zapis i własny odczyt
 *        (JSON = JavaScript Object Notation, tekstowy format danych; parser = analizator tekstu;
 *         escape = ucieczka, czyli zapis znaku specjalnego za pomocą ukośnika wstecznego)
 *
 * W SKRÓCIE:
 *   JSON to tekstowy format wymiany danych: obiekty (klucz: wartość), tablice, napisy, liczby, true/false/null.
 *   JDK 17 nie ma wbudowanego parsera JSON, więc w prawdziwych projektach używa się biblioteki (Jackson, Gson).
 *   Tu piszemy maleńki zapis i odczyt sami — żeby zrozumieć, co biblioteka robi za Ciebie i skąd biorą się błędy.
 *
 * ANALOGIA: JSON to list w kopercie, a parser to sekretarka, która go czyta.
 *   Zasady pisowni listu są sztywne: cudzysłów, przecinek, dwukropek. Sekretarka nie zgaduje — gdy brakuje
 *   dwukropka, odkłada list i mówi: „w 17. znaku brakuje dwukropka”. Dokładnie taki komunikat zbudujemy.
 *
 * JAK TO DZIAŁA:
 *   Sześć rodzajów wartości w JSON       Odpowiednik w Javie (w naszym parserze)
 *   -----------------------------------  ---------------------------------------
 *   obiekt   {"a": 1}                    Map z napisami jako kluczami (LinkedHashMap — zachowuje kolejność)
 *   tablica  [1, 2, 3]                   List (ArrayList)
 *   napis    "tekst"                     String
 *   liczba   12, -3.5, 1e3               BigDecimal (bez utraty dokładności)
 *   logiczna true / false                Boolean
 *   null     null                        null
 *
 *   Zapis (obiekty Javy → tekst):   rekurencja po Map / List / wartościach, napisy z ucieczkami.
 *   Odczyt (tekst → obiekty Javy):  parser rekurencyjny: patrzy na znak i wie, co czytać:
 *        '{' → obiekt,  '[' → tablica,  '"' → napis,  cyfra lub '-' → liczba,  true / false / null → słowo.
 *   Surowy JSON jest tekstem — czytamy go zawsze z jawnym kodowaniem UTF-8 (zobacz t18_io_files/Io02ReadingText).
 *
 *   Czego JSON NIE dopuszcza (częste błędy): przecinek na końcu, apostrofy zamiast cudzysłowów, komentarze,
 *   klucze bez cudzysłowów, NaN i Infinity.
 *
 *   Ucieczki w napisach: \" (cudzysłów), \\ (ukośnik), \n (nowa linia), \t (tabulator), a każdy znak
 *   można zapisać jako: ukośnik wsteczny, litera u i cztery cyfry szesnastkowe (np. litera ł to u0142).
 *   Uwaga: w tym pliku takich sekwencji nie wpisujemy w kodzie wprost — kompilator zamieniłby je na znak
 *   jeszcze przed analizą kodu; budujemy je z ukośnika (stała BS) i tekstu.
 *
 * SŁÓWKA:
 *   object = obiekt; array = tablica; string = napis; number = liczba; boolean = logiczna; parse = analizuj tekst;
 *   writer = piszący; pretty = ładnie (z wcięciami); compact = zwięźle; indent = wcięcie; escape = ucieczka;
 *   key = klucz; value = wartość; position = pozycja; round trip = tam i z powrotem.
 *
 * ZOBACZ TEŻ: t18_io_files/Io02ReadingText (czytanie tekstu z pliku), t18_io_files/Io04Csv (inny format tekstowy),
 *   t15_numbers/Numbers01BigDecimal (dlaczego liczby jako BigDecimal), t12_collections/Collections05Maps (mapy),
 *   t18_io_files/Io12Charsets (kodowania)
 * </pre>
 */
public class Io05JsonManual {

    public static void main(String[] args) throws IOException {
        // throws IOException = „rzuca IOException” — main przekazuje wyjątek wyżej
        title("Io05 — JSON ręcznie: zapis i odczyt");

        Path dir = TempDir.create("io05"); // create = utwórz; wszystko znika w finally
        try {
            jsonTypes();                    // JSON types = rodzaje wartości JSON
            writingValues();                // writing values = zapisywanie wartości
            escapingStrings();              // escaping strings = ucieczki w napisach
            prettyAndOrder();               // pretty and order = ładny zapis i kolejność kluczy
            readingValues();                // reading values = odczyt wartości
            readingEscapes();               // reading escapes = odczyt ucieczek
            parseErrors();                  // parse errors = błędy analizy
            numbersPitfall();               // numbers pitfall = pułapka liczb
            roundTrip(dir.resolve("s9"));   // round trip = plik → obiekty → plik
            exercises();                    // exercises = ćwiczenia
        } finally {
            TempDir.deleteRecursively(dir);
        }
    }

    // =================================================================================================
    // Pomocnicze: wspólne dla wszystkich lekcji o plikach
    // =================================================================================================

    /**
     * rel = ścieżka względem katalogu bazowego, zawsze z ukośnikiem "/".
     * Po co? Katalog tymczasowy ma losową nazwę (innego nie wypisujemy), a Windows pisze ścieżki z "\"
     * (np. "a\b.txt"), Linux z "/" (np. "a/b.txt"). Zamiana na "/" sprawia, że wydruk jest taki sam wszędzie.
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
     * ioFails = „IO zawodzi”. Wykonuje akcję i oczekuje wyjątku IO. Wypisuje nazwę klasy wyjątku i nazwę pliku,
     * a NIE komunikat (getMessage): komunikaty IO zawierają pełną ścieżkę (losowy katalog tymczasowy)
     * oraz tekst zależny od systemu i języka, więc wydruk różniłby się na Windows i na Linuksie.
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
    // NARZĘDZIA LEKCJI: ukośnik, skrót do pisania JSON-a w kodzie, JsonWriter i JsonParser
    // =================================================================================================

    /** BS = ukośnik wsteczny (backslash). Jeden znak: w kodzie piszemy go jako "\\". */
    private static final String BS = "\\";

    /**
     * j = skrót do zapisu JSON-a w kodzie Javy. W JSON-ie pełno cudzysłowów, a w napisie Javy każdy trzeba
     * poprzedzić ukośnikiem. Dlatego w tej lekcji piszemy w JSON-owych tekstach APOSTROF zamiast cudzysłowu
     * oraz znak @ zamiast ukośnika wstecznego; metoda j zamienia je z powrotem. Przykład:
     * j("{'a': 'x@ny'}") to napis {"a": "x\ny"} (z prawdziwym ukośnikiem i cudzysłowami).
     * Gdy chcemy prawdziwy apostrof w JSON-ie (żeby pokazać błąd), podajemy napis bez j.
     */
    private static String j(String text) {
        return text.replace('\'', '"').replace("@", BS);
    }

    /** Błąd analizy JSON-a. Komunikat zaczyna się od pozycji: "pozycja 17: oczekiwano ':'". */
    static final class JsonException extends RuntimeException {
        private static final long serialVersionUID = 1L; // wymagane przez -Xlint:all: wyjątki są Serializable

        JsonException(String message) {
            super(message);
        }
    }

    /**
     * ToMap = „na mapę”. Nasz zapis nie zna rekordów (do tego trzeba refleksji — jej uczy dopiero późniejszy
     * dział, a biblioteki jak Jackson właśnie jej używają). Rekord, który chcemy zapisać, sam podaje swoje pola.
     */
    interface ToMap {
        Map<String, Object> toMap(); // toMap = zamień na mapę
    }

    /**
     * JsonWriter = piszący JSON. Zamienia Map, List, rekordy z toMap, napisy, liczby, Boolean i null na tekst.
     * Trzy tryby: zwięzły (compact), z wcięciami (pretty) i „tylko ASCII” (każdy znak powyżej 126 jako ucieczka).
     */
    static final class JsonWriter {
        private final boolean pretty;
        private final boolean asciiOnly;

        private JsonWriter(boolean pretty, boolean asciiOnly) {
            this.pretty = pretty;
            this.asciiOnly = asciiOnly;
        }

        static String compact(Object value) { return new JsonWriter(false, false).write(value); }

        static String pretty(Object value) { return new JsonWriter(true, false).write(value); }

        static String asciiOnly(Object value) { return new JsonWriter(false, true).write(value); }

        private String write(Object value) {
            StringBuilder sb = new StringBuilder();
            writeValue(sb, value, pretty ? 0 : -1); // poziom -1 = bez wcięć
            return sb.toString();
        }

        private void writeValue(StringBuilder sb, Object v, int level) {
            if (v == null) {
                sb.append("null");
            } else if (v instanceof String s) {
                writeString(sb, s);
            } else if (v instanceof Boolean b) {
                sb.append(b.booleanValue());
            } else if (v instanceof BigDecimal bd) {
                sb.append(bd.toPlainString()); // toPlainString = bez notacji naukowej (1E+3 byłoby błędem dla ludzi)
            } else if (v instanceof Double || v instanceof Float) {
                if (!Double.isFinite(((Number) v).doubleValue())) { // isFinite = skończona (nie NaN, nie nieskończoność)
                    throw new IllegalArgumentException("JSON nie ma zapisu dla NaN ani Infinity");
                }
                sb.append(v);
            } else if (v instanceof Number n) {
                sb.append(n); // Integer, Long itd.
            } else if (v instanceof ToMap t) {
                writeValue(sb, t.toMap(), level);
            } else if (v instanceof Map<?, ?> m) {
                writeObject(sb, m, level);
            } else if (v instanceof Collection<?> c) {
                writeArray(sb, c, level);
            } else {
                throw new IllegalArgumentException("nie umiem zapisać typu " + v.getClass().getSimpleName());
            }
        }

        private void writeObject(StringBuilder sb, Map<?, ?> map, int level) {
            if (map.isEmpty()) {
                sb.append("{}");
                return;
            }
            int inner = level < 0 ? -1 : level + 1; // inner = poziom wewnętrzny
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                newLine(sb, inner);
                if (!(e.getKey() instanceof String key)) {
                    throw new IllegalArgumentException("klucz obiektu JSON musi być napisem");
                }
                writeString(sb, key);
                sb.append(pretty ? ": " : ":");
                writeValue(sb, e.getValue(), inner);
            }
            newLine(sb, level);
            sb.append('}');
        }

        private void writeArray(StringBuilder sb, Collection<?> items, int level) {
            if (items.isEmpty()) {
                sb.append("[]");
                return;
            }
            int inner = level < 0 ? -1 : level + 1;
            sb.append('[');
            boolean first = true;
            for (Object item : items) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                newLine(sb, inner);
                writeValue(sb, item, inner);
            }
            newLine(sb, level);
            sb.append(']');
        }

        /** Nowa linia i wcięcie (po dwie spacje na poziom); w trybie zwięzłym (poziom -1) nic nie robi. */
        private static void newLine(StringBuilder sb, int level) {
            if (level >= 0) {
                sb.append('\n').append("  ".repeat(level)); // repeat = powtórz (Java 11+)
            }
        }

        private void writeString(StringBuilder sb, String s) {
            sb.append('"');
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) { // switch ze strzałkami i kilkoma etykietami: Java 14+
                    case '"' -> sb.append(BS).append('"');
                    case '\\' -> sb.append(BS).append(BS);
                    case '\n' -> sb.append(BS).append('n');
                    case '\r' -> sb.append(BS).append('r');
                    case '\t' -> sb.append(BS).append('t');
                    case '\b' -> sb.append(BS).append('b');
                    case '\f' -> sb.append(BS).append('f');
                    default -> {
                        if (c < 0x20 || (asciiOnly && c > 126)) { // znaki sterujące MUSZĄ mieć ucieczkę
                            sb.append(BS).append('u').append(String.format("%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                    }
                }
            }
            sb.append('"');
        }
    }

    /**
     * JsonParser = analizator JSON (parser rekurencyjny: metoda value wywołuje object lub array, a te znowu value).
     * Pole pos = numer znaku, na którym stoimy. Każdy błąd zawiera pozycję (liczoną od 0).
     */
    static final class JsonParser {
        private static final Pattern NUMBER = Pattern.compile("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?");
        private final String s;
        private int pos;

        private JsonParser(String s) {
            this.s = s;
        }

        static Object parse(String text) {
            JsonParser p = new JsonParser(text);
            Object result = p.value();
            p.skipSpaces();
            if (p.pos < text.length()) {
                throw p.error("po wartości są jeszcze jakieś znaki");
            }
            return result;
        }

        private Object value() {
            skipSpaces();
            if (pos >= s.length()) {
                throw error("nieoczekiwany koniec tekstu");
            }
            char c = s.charAt(pos);
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (s.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
            if (s.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
            if (s.startsWith("null", pos)) { pos += 4; return null; }
            if (c == '-' || (c >= '0' && c <= '9')) return number();
            throw error("nieoczekiwany znak '" + c + "'");
        }

        private Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // pomiń '{'
            skipSpaces();
            if (peek() == '}') { pos++; return map; }
            while (true) {
                skipSpaces();
                if (peek() != '"') throw error("oczekiwano klucza w cudzysłowie");
                String key = string();
                skipSpaces();
                expect(':');
                map.put(key, value()); // powtórzony klucz: ostatni wygrywa (standard zaleca klucze unikalne)
                skipSpaces();
                if (peek() == ',') { pos++; continue; }
                expect('}');
                return map;
            }
        }

        private List<Object> array() {
            List<Object> list = new ArrayList<>();
            pos++; // pomiń '['
            skipSpaces();
            if (peek() == ']') { pos++; return list; }
            while (true) {
                list.add(value());
                skipSpaces();
                if (peek() == ',') { pos++; continue; }
                expect(']');
                return list;
            }
        }

        private String string() {
            StringBuilder sb = new StringBuilder();
            pos++; // pomiń otwierający cudzysłów
            while (true) {
                if (pos >= s.length()) throw error("niezamknięty napis");
                char c = s.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c < 0x20) { pos--; throw error("znak sterujący w napisie (użyj ucieczki)"); }
                if (c != '\\') { sb.append(c); continue; }
                if (pos >= s.length()) throw error("niezamknięty napis");
                char e = s.charAt(pos++);
                switch (e) {
                    case '"', '\\', '/' -> sb.append(e);
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> sb.append(hex4());
                    default -> { pos--; throw error("nieznana sekwencja ucieczki"); }
                }
            }
        }

        private char hex4() { // cztery cyfry szesnastkowe po literze u
            if (pos + 4 > s.length()) throw error("za mało cyfr w sekwencji unicode");
            int code = 0;
            for (int i = 0; i < 4; i++) {
                int digit = Character.digit(s.charAt(pos + i), 16);
                if (digit < 0) throw error("to nie jest cyfra szesnastkowa");
                code = code * 16 + digit;
            }
            pos += 4;
            return (char) code;
        }

        private BigDecimal number() {
            Matcher m = NUMBER.matcher(s);
            m.region(pos, s.length()); // szukaj od bieżącej pozycji
            if (!m.lookingAt()) throw error("błędna liczba");
            pos = m.end();
            return new BigDecimal(m.group());
        }

        private void skipSpaces() {
            while (pos < s.length() && " \t\n\r".indexOf(s.charAt(pos)) >= 0) pos++;
        }

        private char peek() { // peek = zerknij; na końcu tekstu zwraca znak o kodzie 0
            return pos < s.length() ? s.charAt(pos) : '\0';
        }

        private void expect(char c) { // expect = oczekuj
            if (peek() != c) throw error("oczekiwano '" + c + "'");
            pos++;
        }

        private JsonException error(String message) {
            return new JsonException("pozycja " + pos + ": " + message);
        }
    }

    /** asMap = traktuj wartość jako obiekt JSON. Wzorzec instanceof zamiast rzutowania: bez ostrzeżeń kompilatora. */
    private static Map<?, ?> asMap(Object value) {
        if (value instanceof Map<?, ?> m) {
            return m;
        }
        throw new IllegalArgumentException("oczekiwano obiektu JSON, jest: " + typeName(value));
    }

    /** asList = traktuj wartość jako tablicę JSON. */
    private static List<?> asList(Object value) {
        if (value instanceof List<?> l) {
            return l;
        }
        throw new IllegalArgumentException("oczekiwano tablicy JSON, jest: " + typeName(value));
    }

    private static String typeName(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    // =================================================================================================
    // 1. RODZAJE WARTOŚCI JSON
    // =================================================================================================

    /**
     * 1. JSON zna tylko sześć rodzajów wartości. Parser (sekcja 5) zamienia je na zwykłe typy Javy.
     * Tu używamy go jako czarnej skrzynki, żeby zobaczyć, co z czego wychodzi.
     */
    static void jsonTypes() {
        section("1. Rodzaje wartości JSON");

        Object parsed = JsonParser.parse(j("['tekst', 12.5, true, false, null, {'k': 1}, [1]]")); // parse = analizuj
        List<String> types = new ArrayList<>();
        for (Object item : asList(parsed)) {
            types.add(typeName(item));
        }
        show("typy w Javie", types);
        // WYNIK: typy w Javie → [String, BigDecimal, Boolean, Boolean, null, LinkedHashMap, ArrayList]

        note("Liczba w JSON ma jeden rodzaj (bez podziału na int i double) — my czytamy ją jako BigDecimal.");
        // WYNIK: ℹ Liczba w JSON ma jeden rodzaj (bez podziału na int i double) — my czytamy ją jako BigDecimal.

        // Dlaczego JDK nie ma parsera JSON? JSON zrobił się powszechny długo po powstaniu podstawowych bibliotek Javy,
        // a gotowe biblioteki (Jackson, Gson, także Jakarta JSON-P) dobrze rozwiązały temat poza JDK.
        // W prawdziwych projektach NIE piszesz własnego parsera: używasz Jacksona.
        //   Jackson:  new ObjectMapper().readValue(tekst, Produkt.class)  — od razu do Twojej klasy lub rekordu,
        //             new ObjectMapper().writeValueAsString(produkt)      — z obiektu do tekstu.
        // Spring Boot używa Jacksona w kontrolerach REST (SpringLearning) — gdy metoda zwraca rekord, Spring sam
        // zamienia go na JSON. Dlatego warto wiedzieć, co dzieje się w środku: ucieczki, liczby, kolejność kluczy.

        // DOBRA PRAKTYKA: w kodzie produkcyjnym używaj sprawdzonej biblioteki, a własny mały parser traktuj jako
        //   naukę. Dlaczego: biblioteka obsługuje setki przypadków brzegowych (głębokie zagnieżdżenie, ogromne
        //   liczby, błędne kodowanie) i jest testowana przez tysiące użytkowników.
    }

    // =================================================================================================
    // 2. ZAPISYWANIE WARTOŚCI
    // =================================================================================================

    /**
     * 2. JsonWriter przechodzi rekurencyjnie po strukturze: Map → obiekt, List → tablica, reszta → wartość.
     * Tryb zwięzły (compact) nie wstawia żadnych zbędnych spacji — taki JSON jest najmniejszy.
     */
    static void writingValues() {
        section("2. Zapisywanie wartości");

        Map<String, Object> person = new LinkedHashMap<>(); // LinkedHashMap = mapa z zachowaną kolejnością wstawiania
        person.put("imie", "Zofia");
        person.put("wiek", 31);
        person.put("vip", true);
        person.put("adres", null);
        person.put("oceny", List.of(5, 4.5, 3));
        person.put("kontakt", Map.of("email", "zofia@example.com")); // jednoelementowa Map.of — kolejność nie ma znaczenia

        show("compact", JsonWriter.compact(person));
        // WYNIK: compact → {"imie":"Zofia","wiek":31,"vip":true,"adres":null,"oceny":[5,4.5,3],"kontakt":{"email":"zofia@example.com"}}

        show("sam napis", JsonWriter.compact("cześć"));
        // WYNIK: sam napis → "cześć"
        show("samo null", JsonWriter.compact(null));
        // WYNIK: samo null → null
        show("pusta tablica i pusty obiekt", JsonWriter.compact(List.of(List.of(), Map.of())));
        // WYNIK: pusta tablica i pusty obiekt → [[],{}]

        // JSON dopuszcza dowolną wartość na samej górze (nie tylko obiekt) — tak mówi nowszy standard (RFC 8259).

        // BigDecimal zapisujemy przez toPlainString: toString dałby dla niektórych wartości notację naukową.
        show("BigDecimal 1E+3 przez toString", new BigDecimal("1E+3").toString());
        // WYNIK: BigDecimal 1E+3 przez toString → 1E+3
        show("BigDecimal 1E+3 przez writer", JsonWriter.compact(new BigDecimal("1E+3")));
        // WYNIK: BigDecimal 1E+3 przez writer → 1000
        show("cena 19.90 zachowuje zera", JsonWriter.compact(new BigDecimal("19.90")));
        // WYNIK: cena 19.90 zachowuje zera → 19.90

        // PUŁAPKA: NaN i nieskończoność nie istnieją w JSON. Writer odmawia, zamiast wypisać niepoprawny tekst.
        expectThrows("zapis NaN", () -> JsonWriter.compact(Double.NaN));
        // WYNIK: ✔ zapis NaN → rzucono IllegalArgumentException: JSON nie ma zapisu dla NaN ani Infinity

        // PUŁAPKA: klucze obiektu JSON to ZAWSZE napisy. Mapa z kluczami innego typu nie ma sensu — odmawiamy.
        Map<Integer, String> badKeys = Map.of(1, "a");
        expectThrows("zapis mapy z kluczem Integer", () -> JsonWriter.compact(badKeys));
        // WYNIK: ✔ zapis mapy z kluczem Integer → rzucono IllegalArgumentException: klucz obiektu JSON musi być napisem

        // Typ, którego writer nie zna, to błąd, a nie ciche toString():
        expectThrows("zapis nieznanego typu", () -> JsonWriter.compact(new StringBuilder("x")));
        // WYNIK: ✔ zapis nieznanego typu → rzucono IllegalArgumentException: nie umiem zapisać typu StringBuilder
    }

    // =================================================================================================
    // 3. UCIECZKI W NAPISACH
    // =================================================================================================

    /**
     * 3. W napisie JSON cudzysłów, ukośnik i znaki sterujące (kody poniżej 32, np. nowa linia) MUSZĄ mieć ucieczkę.
     * Bez tego tekst przestaje być poprawnym JSON-em, a w skrajnym przypadku daje furtkę do ataku (wstrzyknięcie).
     */
    static void escapingStrings() {
        section("3. Ucieczki w napisach");

        String text = "Ala powiedziała: \"cześć\"\nścieżka C:\\temp\t(koniec)";
        // W Javie: cudzysłowy, nowa linia, ukośnik i tabulator — wszystko już „po ucieczkach Javy”.
        String json = JsonWriter.compact(text);
        show("napis w JSON", json);
        // WYNIK: napis w JSON → "Ala powiedziała: \"cześć\"\nścieżka C:\\temp\t(koniec)"

        // Znak sterujący bez nazwy (np. kod 1) dostaje zapis: ukośnik, litera u i cztery cyfry szesnastkowe:
        String control = "a" + (char) 1 + "b";
        show("znak o kodzie 1", JsonWriter.compact(control));
        // WYNIK: znak o kodzie 1 → "a\u0001b"

        // Polskie litery wolno zapisać wprost (JSON w pliku to UTF-8) — tak robi zwykły tryb:
        show("zwykły tryb", JsonWriter.compact("zażółć"));
        // WYNIK: zwykły tryb → "zażółć"
        // Tryb „tylko ASCII” zamienia je na ucieczki. Przydaje się, gdy nie ufamy kodowaniu po drodze
        // (stare systemy, e-mail). Każdy znak powyżej 126 → cztery cyfry szesnastkowe jego kodu.
        show("tryb tylko ASCII", JsonWriter.asciiOnly("zażółć"));
        // WYNIK: tryb tylko ASCII → "za\u017c\u00f3\u0142\u0107"

        // Znak spoza podstawowego zakresu (np. emotikona) to w Javie DWA znaki char (para zastępcza, ang. surrogate
        // pair) — JSON zapisuje go więc jako dwie ucieczki pod rząd; to jest poprawne i standardowe.
        String emoji = new String(Character.toChars(0x1F600)); // toChars = zamień kod znaku na tablicę char
        show("emotikona: liczba char", emoji.length());
        // WYNIK: emotikona: liczba char → 2
        show("emotikona w trybie ASCII", JsonWriter.asciiOnly(emoji));
        // WYNIK: emotikona w trybie ASCII → "\ud83d\ude00"

        // PRZED: sklejanie JSON-a ręcznie, bez ucieczek — pęka na pierwszym cudzysłowie w danych.
        String name = "Jan \"Kowal\"";
        String broken = "{\"imie\":\"" + name + "\"}";
        // PO: writer sam dba o ucieczki.
        Map<String, Object> good = new LinkedHashMap<>();
        good.put("imie", name);
        show("PRZED (sklejanie)", broken);
        // WYNIK: PRZED (sklejanie) → {"imie":"Jan "Kowal""}
        show("PO (writer)", JsonWriter.compact(good));
        // WYNIK: PO (writer) → {"imie":"Jan \"Kowal\""}
        expectThrows("odczyt sklejonego tekstu", () -> JsonParser.parse(broken));
        // WYNIK: ✔ odczyt sklejonego tekstu → rzucono JsonException: pozycja 14: oczekiwano '}'

        // PUŁAPKA: ręczne sklejanie JSON-a z danych użytkownika to dziura bezpieczeństwa (wstrzyknięcie JSON-a:
        //   użytkownik wpisuje imię z cudzysłowem i dopisuje własne pola). Zawsze serializuj przez writer/bibliotekę.
        // DOBRA PRAKTYKA: nigdy nie sklejaj JSON-a ręcznie z danych spoza programu. Dlaczego: pojedynczy cudzysłów
        //   lub ukośnik w danych psuje cały dokument, a złośliwy tekst może podmienić inne pola.
    }

    // =================================================================================================
    // 4. ŁADNY ZAPIS I KOLEJNOŚĆ KLUCZY
    // =================================================================================================

    /** Rekord zapisywany do JSON: sam podaje swoje pola (toMap). */
    private record Address(String city, String street) implements ToMap {
        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("miasto", city);
            map.put("ulica", street);
            return map;
        }
    }

    /**
     * 4. Tryb pretty wstawia nową linię i wcięcia — dla ludzi (pliki konfiguracyjne, podgląd). Kolejność kluczy
     * w obiekcie JSON nie ma znaczenia dla maszyn, ale ma dla ludzi i dla porównywania plików (diff w git).
     */
    static void prettyAndOrder() {
        section("4. Ładny zapis i kolejność kluczy");

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("nazwa", "Biuro");
        doc.put("adres", new Address("Łódź", "Piotrkowska 1")); // rekord przez ToMap
        doc.put("pokoje", List.of(101, 102));
        doc.put("puste", List.of());

        String pretty = JsonWriter.pretty(doc);
        for (String line : pretty.split("\n")) {
            System.out.println(line);
        }
        // WYNIK: {
        // WYNIK:   "nazwa": "Biuro",
        // WYNIK:   "adres": {
        // WYNIK:     "miasto": "Łódź",
        // WYNIK:     "ulica": "Piotrkowska 1"
        // WYNIK:   },
        // WYNIK:   "pokoje": [
        // WYNIK:     101,
        // WYNIK:     102
        // WYNIK:   ],
        // WYNIK:   "puste": []
        // WYNIK: }

        // Zapisaliśmy sami znak "\n" (a nie System.lineSeparator()), więc plik wygląda tak samo w Windows i Linuksie.

        // Kolejność kluczy zależy od mapy, którą podasz:
        //   LinkedHashMap — kolejność wstawiania (zwykle to chcemy: pola w kolejności z klasy),
        //   TreeMap       — alfabetycznie (stały, powtarzalny wynik — dobre do testów i porównań),
        //   HashMap       — kolejność przypadkowa i niegwarantowana (NIE używaj do zapisu JSON-a, jeśli ważny jest wydruk).
        Map<String, Object> sorted = new TreeMap<>(); // TreeMap = mapa posortowana po kluczach
        sorted.put("zeta", 1);
        sorted.put("alfa", 2);
        sorted.put("miodek", 3);
        show("TreeMap", JsonWriter.compact(sorted));
        // WYNIK: TreeMap → {"alfa":2,"miodek":3,"zeta":1}

        Map<String, Object> insertion = new LinkedHashMap<>();
        insertion.put("zeta", 1);
        insertion.put("alfa", 2);
        insertion.put("miodek", 3);
        show("LinkedHashMap", JsonWriter.compact(insertion));
        // WYNIK: LinkedHashMap → {"zeta":1,"alfa":2,"miodek":3}

        // Po stronie odczytu też: nasz parser używa LinkedHashMap, więc „odczyt i zapis” nie przestawia pól.

        // DOBRA PRAKTYKA: do plików, które trafiają do repozytorium, używaj stałej kolejności kluczy (TreeMap lub
        //   zawsze ta sama kolejność z kodu). Dlaczego: inaczej każdy zapis może przestawić pola i w git widać zmianę
        //   całego pliku, choć dane są te same.
        // PUŁAPKA: JSON w trybie pretty jest większy (wcięcia) — do przesyłania przez sieć używaj trybu zwięzłego.
    }

    // =================================================================================================
    // 5. ODCZYT WARTOŚCI
    // =================================================================================================

    private static final String SAMPLE = """
            {
              "imie": "Zofia",
              "wiek": 31,
              "vip": true,
              "adres": null,
              "oceny": [5, 4.5, 3],
              "kontakt": {"email": "zofia@example.com"}
            }
            """; // blok tekstowy (Java 15+): wielolinijkowy napis bez ucieczek przy cudzysłowach

    /**
     * 5. Parser to jedna klasa (JsonParser, wyżej): metoda value patrzy na pierwszy znak i wybiera, co czytać;
     * object i array wołają value dla każdego elementu — stąd „rekurencyjny”. Wynik to zwykłe Map, List, String,
     * BigDecimal, Boolean i null.
     */
    static void readingValues() {
        section("5. Odczyt wartości");

        Map<?, ?> person = asMap(JsonParser.parse(SAMPLE));
        show("typ korzenia", person.getClass().getSimpleName());
        // WYNIK: typ korzenia → LinkedHashMap
        show("imie", person.get("imie")); // get = pobierz wartość po kluczu
        // WYNIK: imie → Zofia
        show("wiek (typ)", typeName(person.get("wiek")));
        // WYNIK: wiek (typ) → BigDecimal
        show("wiek", person.get("wiek"));
        // WYNIK: wiek → 31
        show("vip", person.get("vip"));
        // WYNIK: vip → true
        show("adres", String.valueOf(person.get("adres")));
        // WYNIK: adres → null
        show("klucz adres istnieje", person.containsKey("adres")); // containsKey = czy jest taki klucz
        // WYNIK: klucz adres istnieje → true
        show("klucz telefon istnieje", person.containsKey("telefon"));
        // WYNIK: klucz telefon istnieje → false
        show("oceny", person.get("oceny"));
        // WYNIK: oceny → [5, 4.5, 3]
        show("e-mail z obiektu w obiekcie", asMap(person.get("kontakt")).get("email"));
        // WYNIK: e-mail z obiektu w obiekcie → zofia@example.com

        // Kolejność pól zachowana:
        show("klucze po kolei", person.keySet());
        // WYNIK: klucze po kolei → [imie, wiek, vip, adres, oceny, kontakt]

        // PUŁAPKA: null z JSON-a i brak klucza to dla map to samo (get zwraca null w obu przypadkach). Gdy różnica
        //   ma znaczenie, pytaj containsKey (jak wyżej: "adres" jest, ale ma wartość null).
        // PUŁAPKA: wynik parsera to Object — trzeba sprawdzić typ przed użyciem. Stąd pomocnicze asMap/asList:
        //   zamiast rzutowania (które daje ostrzeżenie „unchecked”) używamy instanceof ze wzorcem.
        expectThrows("tablica potraktowana jak obiekt", () -> asMap(person.get("oceny")));
        // WYNIK: ✔ tablica potraktowana jak obiekt → rzucono IllegalArgumentException: oczekiwano obiektu JSON, jest: ArrayList

        // Spacje i nowe linie między elementami są dowolne (przecinek i dwukropek nie muszą mieć spacji):
        show("tekst bez spacji", asList(JsonParser.parse("[1,2,{\"a\":[]}]")));
        // WYNIK: tekst bez spacji → [1, 2, {a=[]}]

        // DOBRA PRAKTYKA: waliduj strukturę po odczycie (czy jest pole, czy ma właściwy typ) i zgłaszaj błąd
        //   z nazwą pola. Dlaczego: JSON jest „luźny” — plik od kogoś innego może mieć brakujące lub dziwne pola,
        //   a błąd w środku programu (ClassCastException, NullPointerException) nic nie mówi o przyczynie.
    }

    // =================================================================================================
    // 6. ODCZYT UCIECZEK
    // =================================================================================================

    /**
     * 6. Parser zamienia ucieczki z powrotem na znaki: n na nową linię, ukośnik z cudzysłowem na cudzysłów, itd.
     * Sekwencja: ukośnik, litera u i cztery cyfry szesnastkowe zamienia się na jeden znak char.
     */
    static void readingEscapes() {
        section("6. Odczyt ucieczek");

        // W zapisie testowym (j): @ = ukośnik wsteczny, apostrof = cudzysłów JSON.
        List<?> list = asList(JsonParser.parse(j(
                "['Zo@u0142ta', 'a@nb', '@'q@'', 'C:@@dir', '@/', '@ud83d@ude00', '@u00F3']")));
        show("sekwencja unicode (litera ł)", list.get(0));
        // WYNIK: sekwencja unicode (litera ł) → Zołta
        show("\\n daje nową linię (długość)", list.get(1).toString().length());
        // WYNIK: \n daje nową linię (długość) → 3
        show("\\n — czy zawiera znak nowej linii", list.get(1).toString().contains("\n"));
        // WYNIK: \n — czy zawiera znak nowej linii → true
        show("cudzysłów w środku", list.get(2));
        // WYNIK: cudzysłów w środku → "q"
        show("ukośnik", list.get(3));
        // WYNIK: ukośnik → C:\dir
        show("ucieczka przed ukośnikiem prawym", list.get(4));
        // WYNIK: ucieczka przed ukośnikiem prawym → /
        show("para zastępcza: długość w char", list.get(5).toString().length());
        // WYNIK: para zastępcza: długość w char → 2
        show("para zastępcza: liczba znaków Unicode", list.get(5).toString().codePointCount(0, 2));
        // WYNIK: para zastępcza: liczba znaków Unicode → 1
        show("wielkie litery w cyfrach hex", list.get(6));
        // WYNIK: wielkie litery w cyfrach hex → ó

        // Ukośnik przed ukośnikiem to jeden ukośnik: dlatego ścieżka C:\dir w JSON wygląda jak "C:\\dir".
        // Ukośnik wsteczny przed "/" jest dozwolony (bywa spotykany w JSON-ie osadzanym w HTML), ale nie jest wymagany.

        // PUŁAPKA: w NIEZAMKNIĘTYM napisie albo ze znakiem sterującym parser kończy błędem — zwykła nowa linia
        //   w środku napisu JSON jest niedozwolona (musi być ucieczka n).
        expectThrows("surowa nowa linia w napisie", () -> JsonParser.parse("\"a\nb\""));
        // WYNIK: ✔ surowa nowa linia w napisie → rzucono JsonException: pozycja 2: znak sterujący w napisie (użyj ucieczki)
        expectThrows("nieznana ucieczka", () -> JsonParser.parse(j("'a@qb'")));
        // WYNIK: ✔ nieznana ucieczka → rzucono JsonException: pozycja 3: nieznana sekwencja ucieczki
        expectThrows("za krótka sekwencja unicode", () -> JsonParser.parse(j("'@u12'")));
        // WYNIK: ✔ za krótka sekwencja unicode → rzucono JsonException: pozycja 3: za mało cyfr w sekwencji unicode
        expectThrows("niepoprawna cyfra szesnastkowa", () -> JsonParser.parse(j("'@u00zz'")));
        // WYNIK: ✔ niepoprawna cyfra szesnastkowa → rzucono JsonException: pozycja 3: to nie jest cyfra szesnastkowa
    }

    // =================================================================================================
    // 7. BŁĘDY ANALIZY Z POZYCJĄ
    // =================================================================================================

    /**
     * 7. Dobry parser mówi, GDZIE jest błąd i CO jest nie tak. Pozycja to numer znaku (od 0) — łatwo ją odnaleźć
     * w krótkim tekście, a w dużym pliku dodaje się numer linii (zadanie dla chętnych).
     */
    static void parseErrors() {
        section("7. Błędy analizy z pozycją");

        // Przecinek na końcu — dozwolony w wielu językach (JavaScript, Java w tablicach), ale NIE w JSON:
        expectThrows("przecinek na końcu obiektu", () -> JsonParser.parse("{\"a\": 1,}"));
        // WYNIK: ✔ przecinek na końcu obiektu → rzucono JsonException: pozycja 8: oczekiwano klucza w cudzysłowie
        expectThrows("przecinek na końcu tablicy", () -> JsonParser.parse("[1, 2,]"));
        // WYNIK: ✔ przecinek na końcu tablicy → rzucono JsonException: pozycja 6: nieoczekiwany znak ']'

        // Apostrofy zamiast cudzysłowów — dozwolone w JavaScript, w JSON nie:
        expectThrows("apostrofy", () -> JsonParser.parse("{'a': 'b'}"));
        // WYNIK: ✔ apostrofy → rzucono JsonException: pozycja 1: oczekiwano klucza w cudzysłowie

        // Klucz bez cudzysłowów:
        expectThrows("klucz bez cudzysłowów", () -> JsonParser.parse("{a: 1}"));
        // WYNIK: ✔ klucz bez cudzysłowów → rzucono JsonException: pozycja 1: oczekiwano klucza w cudzysłowie

        // Brak dwukropka:
        expectThrows("brak dwukropka", () -> JsonParser.parse("{\"a\" 1}"));
        // WYNIK: ✔ brak dwukropka → rzucono JsonException: pozycja 5: oczekiwano ':'

        // Komentarze nie istnieją w JSON (dlatego pliki konfiguracyjne w JSON nie mogą nic objaśniać):
        expectThrows("komentarz", () -> JsonParser.parse("// konfiguracja\n{}"));
        // WYNIK: ✔ komentarz → rzucono JsonException: pozycja 0: nieoczekiwany znak '/'

        // Niezamknięty napis, brak zamknięcia obiektu, śmieci po wartości:
        expectThrows("niezamknięty napis", () -> JsonParser.parse("{\"a\": \"tekst"));
        // WYNIK: ✔ niezamknięty napis → rzucono JsonException: pozycja 12: niezamknięty napis
        expectThrows("brak nawiasu zamykającego", () -> JsonParser.parse("[1, 2"));
        // WYNIK: ✔ brak nawiasu zamykającego → rzucono JsonException: pozycja 5: oczekiwano ']'
        expectThrows("śmieci po wartości", () -> JsonParser.parse("{} {}"));
        // WYNIK: ✔ śmieci po wartości → rzucono JsonException: pozycja 3: po wartości są jeszcze jakieś znaki
        expectThrows("pusty tekst", () -> JsonParser.parse("   "));
        // WYNIK: ✔ pusty tekst → rzucono JsonException: pozycja 3: nieoczekiwany koniec tekstu
        expectThrows("NaN w tekście", () -> JsonParser.parse("[NaN]"));
        // WYNIK: ✔ NaN w tekście → rzucono JsonException: pozycja 1: nieoczekiwany znak 'N'
        expectThrows("liczba z plusem", () -> JsonParser.parse("[+1]"));
        // WYNIK: ✔ liczba z plusem → rzucono JsonException: pozycja 1: nieoczekiwany znak '+'
        expectThrows("sama minus", () -> JsonParser.parse("[-]"));
        // WYNIK: ✔ sama minus → rzucono JsonException: pozycja 1: błędna liczba

        // Wyjątek jest NIESPRAWDZANY (RuntimeException): błędny JSON to zwykle błąd danych, a nie awaria dysku.
        // Odczyt z pliku może rzucić IOException (sprawdzany), a analiza tekstu — JsonException. Dwa osobne problemy.

        // DOBRA PRAKTYKA: w komunikacie o błędzie podawaj POZYCJĘ i CO oczekiwano. Dlaczego: „błąd w JSON” nie mówi nic,
        //   a „pozycja 17: oczekiwano ':'” pozwala znaleźć przecinek lub brakujący znak w kilka sekund.
        // DOBRA PRAKTYKA: ogranicz głębokość zagnieżdżenia i rozmiar wejścia, gdy czytasz JSON z internetu: parser
        //   rekurencyjny przy tysiącach zagnieżdżonych nawiasów skończy się StackOverflowError.
    }

    // =================================================================================================
    // 8. PUŁAPKA LICZB
    // =================================================================================================

    /**
     * 8. Standard JSON nie mówi, ile cyfr ma liczba. Wiele parserów (m.in. w JavaScript) czyta każdą liczbę jako double
     * i traci dokładność. My czytamy jako BigDecimal — dokładnie to, co jest w tekście.
     */
    static void numbersPitfall() {
        section("8. Pułapka liczb");

        double sum = Double.parseDouble("0.1") + Double.parseDouble("0.2"); // parseDouble = czytaj jako double
        show("0.1 + 0.2 jako double", sum);
        // WYNIK: 0.1 + 0.2 jako double → 0.30000000000000004
        BigDecimal exact = asBig(JsonParser.parse("0.1")).add(asBig(JsonParser.parse("0.2")));
        show("0.1 + 0.2 jako BigDecimal", exact);
        // WYNIK: 0.1 + 0.2 jako BigDecimal → 0.3

        BigDecimal big = asBig(JsonParser.parse("12345678901234567890"));
        show("duża liczba jako BigDecimal", big);
        // WYNIK: duża liczba jako BigDecimal → 12345678901234567890
        show("ta sama liczba jako double", big.doubleValue());
        // WYNIK: ta sama liczba jako double → 1.2345678901234567E19

        BigDecimal price = asBig(JsonParser.parse("19.90"));
        show("19.90 zachowuje skalę", price);
        // WYNIK: 19.90 zachowuje skalę → 19.90
        show("skala (liczba cyfr po przecinku)", price.scale()); // scale = skala
        // WYNIK: skala (liczba cyfr po przecinku) → 2

        BigDecimal sci = asBig(JsonParser.parse("1e3"));
        show("1e3 przez toPlainString", sci.toPlainString());
        // WYNIK: 1e3 przez toPlainString → 1000
        show("1e3 jako int (intValueExact)", sci.intValueExact()); // intValueExact = int albo wyjątek
        // WYNIK: 1e3 jako int (intValueExact) → 1000

        // Liczba całkowita wymaga sprawdzenia, że to naprawdę całkowita (intValueExact rzuca ArithmeticException):
        expectThrows("wiek 31.5 jako int", () -> asBig(JsonParser.parse("31.5")).intValueExact());
        // WYNIK: ✔ wiek 31.5 jako int → rzucono ArithmeticException: Rounding necessary

        // Zera wiodące są w JSON zabronione (01 to nie liczba), wartości jak 1. i .5 też:
        expectThrows("liczba z zerem wiodącym", () -> JsonParser.parse("[01]"));
        // WYNIK: ✔ liczba z zerem wiodącym → rzucono JsonException: pozycja 2: oczekiwano ']'

        // PUŁAPKA: pieniądze w double to klasyczny błąd (t15_numbers/Numbers01BigDecimal). W JSON zapisz cenę jako
        //   liczbę z dokładnie dwoma miejscami albo jako napis "19.90" — i czytaj do BigDecimal, nie do double.
        // DOBRA PRAKTYKA: porównuj BigDecimal przez compareTo, nie equals (equals porównuje też skalę: 19.9 ≠ 19.90).
        show("19.9 equals 19.90", new BigDecimal("19.9").equals(new BigDecimal("19.90")));
        // WYNIK: 19.9 equals 19.90 → false
        show("19.9 compareTo 19.90", new BigDecimal("19.9").compareTo(new BigDecimal("19.90")));
        // WYNIK: 19.9 compareTo 19.90 → 0
    }

    /** asBig = traktuj wartość jako liczbę. */
    private static BigDecimal asBig(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        throw new IllegalArgumentException("oczekiwano liczby JSON, jest: " + typeName(value));
    }

    // =================================================================================================
    // 9. PLIK → OBIEKTY → PLIK
    // =================================================================================================

    /**
     * ProductRow = „wiersz produktu”: własny rekord z polami, które chcemy zapisać do JSON-a i odczytać z niego.
     * Ma wbudowane tłumaczenie do mapy (toMap) i z mapy (fromMap) z walidacją — wszystko ręcznie, bez refleksji.
     */
    private record ProductRow(String sku, String name, String category, BigDecimal price, int stock) implements ToMap {

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sku", sku);
            map.put("name", name);
            map.put("category", category);
            map.put("price", price);
            map.put("stock", stock);
            return map;
        }

        static ProductRow fromMap(Map<?, ?> map) { // fromMap = z mapy
            return new ProductRow(
                    text(map, "sku"), text(map, "name"), text(map, "category"),
                    number(map, "price"), number(map, "stock").intValueExact());
        }

        private static String text(Map<?, ?> map, String key) {
            if (map.get(key) instanceof String s) {
                return s;
            }
            throw new IllegalArgumentException("pole '" + key + "' musi być napisem");
        }

        private static BigDecimal number(Map<?, ?> map, String key) {
            if (map.get(key) instanceof BigDecimal bd) {
                return bd;
            }
            throw new IllegalArgumentException("pole '" + key + "' musi być liczbą");
        }
    }

    /**
     * 9. Tam i z powrotem (round trip): obiekty → JSON w pliku → obiekty. Jeśli odczyt daje to samo, co zapisaliśmy,
     * zapis i odczyt do siebie pasują. Plik zapisujemy z jawnym UTF-8 i własnym znakiem "\n".
     */
    static void roundTrip(Path base) throws IOException {
        section("9. Plik → obiekty → plik");
        Files.createDirectory(base);

        List<ProductRow> original = new ArrayList<>();
        for (Product p : SampleData.products().subList(0, 3)) { // subList = fragment listy (indeksy 0..2)
            original.add(new ProductRow(p.sku(), p.name(), p.category().name(), p.price(), p.stock()));
        }

        Path file = base.resolve("produkty.json");
        Files.writeString(file, JsonWriter.pretty(original) + "\n", StandardCharsets.UTF_8); // writeString = zapisz napis (Java 11+)
        show("plik", rel(base, file));
        // WYNIK: plik → produkty.json

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8); // readAllLines = wczytaj wszystkie linie
        show("liczba linii w pliku", lines.size());
        // WYNIK: liczba linii w pliku → 23
        show("linia 4", lines.get(3).strip());
        // WYNIK: linia 4 → "name": "Laptop Pro 14",
        show("linia 18", lines.get(17).strip());
        // WYNIK: linia 18 → "name": "Słuchawki BT",

        // Odczyt: plik → tekst → obiekty → rekordy.
        String content = Files.readString(file, StandardCharsets.UTF_8);
        List<ProductRow> back = new ArrayList<>();
        for (Object item : asList(JsonParser.parse(content))) {
            back.add(ProductRow.fromMap(asMap(item)));
        }
        show("po odczycie: liczba rekordów", back.size());
        // WYNIK: po odczycie: liczba rekordów → 3
        show("po odczycie: rekord 2", back.get(1));
        // WYNIK: po odczycie: rekord 2 → ProductRow[sku=ELE-002, name=Smartfon X, category=ELEKTRONIKA, price=2999.00, stock=0]
        show("rekordy takie same jak przed zapisem", back.equals(original));
        // WYNIK: rekordy takie same jak przed zapisem → true

        // Zapis ponowny i porównanie plików: powinny być identyczne (stała kolejność kluczy, stały format).
        Path file2 = base.resolve("produkty-2.json");
        Files.writeString(file2, JsonWriter.pretty(back) + "\n", StandardCharsets.UTF_8);
        show("drugi zapis identyczny z pierwszym", Files.readString(file2, StandardCharsets.UTF_8).equals(content));
        // WYNIK: drugi zapis identyczny z pierwszym → true

        // Walidacja przy odczycie: brakujące pole lub zły typ dają czytelny błąd z nazwą pola.
        expectThrows("brak pola price", () -> ProductRow.fromMap(asMap(JsonParser.parse(j("{'sku': 'X', 'name': 'N', 'category': 'K', 'stock': 1}")))));
        // WYNIK: ✔ brak pola price → rzucono IllegalArgumentException: pole 'price' musi być liczbą
        expectThrows("stock jako napis", () -> ProductRow.fromMap(asMap(JsonParser.parse(j("{'sku': 'X', 'name': 'N', 'category': 'K', 'price': 1, 'stock': '2'}")))));
        // WYNIK: ✔ stock jako napis → rzucono IllegalArgumentException: pole 'stock' musi być liczbą

        // Błędy IO to osobna sprawa: brak pliku to IOException (NoSuchFileException), nie JsonException.
        ioFails("odczyt brakującego pliku", () -> Files.readString(base.resolve("brak.json"), StandardCharsets.UTF_8));
        // WYNIK: ✔ odczyt brakującego pliku → rzucono NoSuchFileException (plik: brak.json)

        // PUŁAPKA: Files.readString / writeString bez podanego kodowania używają UTF-8 (to akurat prawda od Javy 11),
        //   ale Reader/Writer/String.getBytes bez kodowania używają kodowania SYSTEMU (w Javie 17 na polskim Windows:
        //   windows-1250). Dlatego w całym kursie podajemy StandardCharsets.UTF_8 jawnie.
        // DOBRA PRAKTYKA: zapisuj najpierw do pliku tymczasowego i dopiero potem przenoś go na miejsce
        //   (t18_io_files/Io03WritingText) — odczyt pliku, który jest właśnie zapisywany, widzi wtedy albo całość, albo nic.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • JSON: obiekt {"k": w}, tablica [..], napis, liczba, true/false, null. Tylko cudzysłowy, bez komentarzy,
     *     bez przecinka na końcu, bez NaN/Infinity.
     *   • JDK nie ma parsera JSON → Jackson/Gson (Spring Boot używa Jacksona).
     *   • Zapis: rekurencja po Map/List; napisy z ucieczkami: \" \\ \n \t i ukośnik+u+4 cyfry dla znaków sterujących;
     *     BigDecimal przez toPlainString; kolejność kluczy: LinkedHashMap (wstawiania) lub TreeMap (alfabetyczna).
     *   • Odczyt: parser rekurencyjny → Map, List, String, BigDecimal, Boolean, null; błąd z pozycją.
     *   • Liczby czytaj do BigDecimal (0.1 + 0.2 w double to 0.30000000000000004).
     *   • Nie sklejaj JSON-a ręcznie z danych użytkownika (ucieczki, wstrzyknięcie).
     *   • Plik: UTF-8 jawnie; IOException (plik) i JsonException (treść) to dwa różne błędy.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego w prawdziwym projekcie nie piszemy własnego parsera JSON?
     *   2. Po co ucieczki w napisie JSON i które znaki muszą je mieć?
     *   3. Co wypisze:  System.out.println(0.1 + 0.2);  oraz  System.out.println(new BigDecimal("0.1").add(new BigDecimal("0.2")));  ?
     *   4. Co wypisze:  Object x = JsonParser.parse("{\"a\": [1, 2]}");  System.out.println(((Map<?, ?>) x).get("a"));  ?
     *   5. ZNAJDŹ BŁĄD:  {"a": 1, "b": 2,}   — dlaczego parser JSON to odrzuci?
     *   6. ZNAJDŹ BŁĄD:  {'imie': 'Ola'}   — co trzeba zmienić, żeby to był poprawny JSON?
     *   7. Dlaczego w JSON zapisujemy cenę jako liczbę czytaną do BigDecimal, a nie do double?
     *   8. Co wypisze:  JsonWriter.compact(Map.of("k", "a\"b"))  (zapis w JSON, z cudzysłowem w danych)?
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

    /** Każde uruchomienie dostaje własny, świeży katalog; reference = czy sprawdzamy rozwiązania wzorcowe. */
    private static void runExercises(boolean reference) {
        Path dir = TempDir.create("io05cw");
        try {
            String quoted = "Ala \"ma\" kota\nC:\\temp";
            Check.equal("ćw. 1: napis jako JSON", j("\"Ala @\"ma@\" kota@nC:@@temp\""),
                    () -> reference ? solution1(quoted) : exercise1(quoted));

            String shop = j("{'sklep': {'produkty': [{'nazwa': 'A', 'cena': 10}, {'nazwa': 'B', 'cena': 5.5}]}}");
            Check.equal("ćw. 2: suma cen z drzewa JSON", new BigDecimal("15.5"),
                    () -> reference ? solution2(shop) : exercise2(shop));

            Check.equal("ćw. 3: PRZEPISZ sklejanie na writer", j("{'imie':'Jan @'Kowal@'','wiek':30}"),
                    () -> reference ? solution3("Jan \"Kowal\"", 30) : exercise3("Jan \"Kowal\"", 30));

            Check.equal("ćw. 4: konfiguracja z pliku", "localhost:8080 debug=true|błąd: port musi być liczbą|błąd: port poza zakresem",
                    io(() -> {
                        Path a = dir.resolve("a.json");
                        Path b = dir.resolve("b.json");
                        Path c = dir.resolve("c.json");
                        Files.writeString(a, j("{'host': 'localhost', 'port': 8080, 'debug': true}"), StandardCharsets.UTF_8);
                        Files.writeString(b, j("{'host': 'x', 'port': '80', 'debug': false}"), StandardCharsets.UTF_8);
                        Files.writeString(c, j("{'host': 'x', 'port': 70000, 'debug': false}"), StandardCharsets.UTF_8);
                        return (reference ? solution4(a) : exercise4(a)) + "|"
                                + (reference ? solution4(b) : exercise4(b)) + "|"
                                + (reference ? solution4(c) : exercise4(c));
                    }));
        } finally {
            TempDir.deleteRecursively(dir);
        }
        Check.summary();
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć podany tekst jako napis JSON (z cudzysłowami na początku i końcu): zamień ukośnik
     * na dwa ukośniki, cudzysłów na ukośnik+cudzysłów, a nową linię na ukośnik+n.
     * Podpowiedź: najpierw ukośniki (replace), dopiero potem cudzysłowy — inaczej podwoisz ukośniki dodane przy cudzysłowach.
     */
    static String exercise1(String text) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): z tekstu JSON w postaci {"sklep": {"produkty": [{"nazwa": .., "cena": ..}, ..]}} policz sumę
     * wszystkich cen. Użyj JsonParser.parse oraz pomocniczych asMap, asList, asBig.
     * Podpowiedź: asMap(root).get("sklep") → asMap → get("produkty") → asList → pętla for po elementach.
     */
    static BigDecimal exercise2(String json) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ sklejanie napisów na JsonWriter. Stary kod (psuje się, gdy imię ma cudzysłów):
     * <pre>{@code
     * return "{\"imie\":\"" + name + "\",\"wiek\":" + age + "}";
     * }</pre>
     * Nowa wersja ma zwrócić zwięzły JSON z polami "imie" i "wiek" (w tej kolejności).
     * Podpowiedź: LinkedHashMap, dwa razy put, JsonWriter.compact.
     */
    static String exercise3(String name, int age) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): wczytaj z pliku JSON z polami host, port, debug i zwróć "host:port debug=wartość",
     * np. "localhost:8080 debug=true". Gdy port nie jest liczbą, zwróć "błąd: port musi być liczbą"; gdy jest poza
     * zakresem 1..65535, zwróć "błąd: port poza zakresem".
     * Podpowiedź: Files.readString z UTF-8, JsonParser.parse, instanceof BigDecimal, intValueExact.
     */
    static String exercise4(Path file) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String text) {
        return "\"" + text.replace(BS, BS + BS).replace("\"", BS + "\"").replace("\n", BS + "n") + "\"";
    }

    static BigDecimal solution2(String json) {
        BigDecimal sum = BigDecimal.ZERO;
        Map<?, ?> shop = asMap(asMap(JsonParser.parse(json)).get("sklep"));
        for (Object item : asList(shop.get("produkty"))) {
            sum = sum.add(asBig(asMap(item).get("cena")));
        }
        return sum;
    }

    static String solution3(String name, int age) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("imie", name);
        map.put("wiek", age);
        return JsonWriter.compact(map);
    }

    static String solution4(Path file) throws IOException {
        Map<?, ?> config = asMap(JsonParser.parse(Files.readString(file, StandardCharsets.UTF_8)));
        if (!(config.get("port") instanceof BigDecimal port)) {
            return "błąd: port musi być liczbą";
        }
        int number = port.intValueExact();
        if (number < 1 || number > 65535) {
            return "błąd: port poza zakresem";
        }
        return config.get("host") + ":" + number + " debug=" + config.get("debug");
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo biblioteka (Jackson, Gson) obsługuje mnóstwo przypadków brzegowych, jest szybka, przetestowana i od
     *      razu zamienia JSON na Twoje klasy i rekordy. Własny parser pisze się tylko po to, żeby zrozumieć format.
     *   2. Żeby tekst był jednoznaczny: cudzysłów kończyłby napis, ukośnik zaczyna ucieczkę. Ucieczkę MUSZĄ mieć:
     *      cudzysłów, ukośnik i wszystkie znaki sterujące (kod poniżej 32: nowa linia, tabulator itd.).
     *   3. 0.30000000000000004 (double nie ma dokładnego 0.1 ani 0.2) oraz 0.3 (BigDecimal liczy dokładnie z tekstu).
     *   4. [1, 2]  — to lista dwóch BigDecimal (toString listy: elementy w nawiasach, po przecinku).
     *   5. Przecinek po ostatnim elemencie ("2,}") jest w JSON zabroniony — parser zgłosi błąd na zamykającym nawiasie.
     *   6. Klucze i napisy muszą być w cudzysłowach, nie w apostrofach: {"imie": "Ola"}.
     *   7. Bo double nie przechowuje dokładnie ułamków dziesiętnych (np. 0.1) i traci cyfry w bardzo dużych liczbach;
     *      BigDecimal zachowuje dokładnie to, co jest w tekście (także skalę: 19.90).
     *   8. {"k":"a\"b"}  — cudzysłów w danych dostaje ucieczkę (ukośnik przed cudzysłowem).
     */
    // </editor-fold>
}
