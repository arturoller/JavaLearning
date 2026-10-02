package t18_io_files;

import helpers.Check;
import helpers.SampleData;
import helpers.TempDir;
import helpers.model.Category;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: CSV — czytanie i zapis danych tabelarycznych w plikach tekstowych
 *        (CSV = Comma-Separated Values, wartości rozdzielone przecinkami; field = pole; record/row = wiersz;
 *         header = nagłówek; validation = sprawdzanie poprawności)
 *
 * W SKRÓCIE:
 *   CSV to tekst, w którym każda linia jest wierszem tabeli, a pola oddziela przecinek. Pozornie trywialne: "split po przecinku".
 *   W praktyce psują to puste pola na końcu, błędne dane, przecinki w tekście i polski Excel (średnik, przecinek dziesiętny).
 *   Lekcja buduje czytnik, który NIE przewraca się na złej linii, tylko zbiera błędy z numerami linii.
 *
 * ANALOGIA: arkusz z kratek przepisany do zeszytu.
 *   Każda linia zeszytu to wiersz, kreski pionowe (przecinki) to granice kolumn. Gdy w komórce jest sam przecinek
 *   („Kawa, ziarnista”), trzeba ją ująć w cudzysłów — inaczej czytelnik pomyśli, że to dwie kolumny.
 *
 * JAK TO DZIAŁA:
 *   data,sku,produkt,kategoria,ilosc,cena        ← nagłówek (pierwsza linia)
 *   2026-05-04,KSI-001,Czysty kod,KSIAZKI,2,79.00
 *
 *   1) czytaj linia po linii (BufferedReader, numer linii od 1)
 *   2) line.split(",", -1)  — minus jeden: NIE obcinaj pustych pól na końcu
 *   3) zamień pola na typy (LocalDate, int, BigDecimal, enum) i SPRAWDŹ je
 *   4) zły wiersz → zapisz "linia N: powód" i idź dalej (nie przerywaj całego pliku)
 *
 *   Prawdziwy CSV (RFC 4180): pole z przecinkiem, cudzysłowem lub nową linią ujmujemy w cudzysłów ("..."),
 *   a cudzysłów w środku podwajamy (""). Zwykłe split(",") tego nie rozumie.
 *
 * SŁÓWKA:
 *   split = podziel; header = nagłówek; delimiter/separator = znak rozdzielający; quote = cudzysłów / ująć w cudzysłów;
 *   escape = ucieczka (znak specjalny); validation = walidacja; trailing = końcowy; row = wiersz.
 *
 * ZOBACZ TEŻ: t18_io_files/Io02ReadingText (BufferedReader), t18_io_files/Io03WritingText (zapis),
 *   t15_numbers/Numbers01BigDecimal (pieniądze), t17_datetime/DateTime01LocalDateTime (daty),
 *   t16_streams/Streams11GroupingBy (grupowanie)
 * </pre>
 */
public class Io04Csv {

    /** Oczekiwany nagłówek pliku sprzedaży. */
    private static final String HEADER = "data,sku,produkt,kategoria,ilosc,cena";

    /**
     * Jeden poprawnie wczytany wiersz sprzedaży. record = rekord (Java 16+): niezmienna klasa danych.
     * date = data; sku = kod produktu; name = nazwa; category = kategoria; quantity = ilość; price = cena jednostkowa.
     */
    record SaleRow(LocalDate date, String sku, String name, Category category, int quantity, BigDecimal price) {
        /** total = wartość wiersza: cena razy ilość. */
        BigDecimal total() {
            return price.multiply(BigDecimal.valueOf(quantity)); // multiply = pomnóż; valueOf = z liczby
        }
    }

    /** Wynik wczytania pliku: poprawne wiersze oraz opisy błędów (errors = błędy). */
    record ParseResult(List<SaleRow> rows, List<String> errors) {
    }

    public static void main(String[] args) throws IOException {
        title("Io04 — CSV: wczytywanie, walidacja, raport");

        Path dir = TempDir.create("io04"); // create = utwórz
        try {
            splitPitfall();                         // split pitfall = pułapka funkcji split
            parsingOneRow();                        // parsing one row = parsowanie jednego wiersza
            Path file = dir.resolve("sprzedaz.csv");
            ParseResult result = readingWholeFile(file);  // reading whole file = czytanie całego pliku
            summaryPerCategory(result.rows());      // summary per category = podsumowanie wg kategorii
            reportRoundTrip(dir, result.rows());    // report round trip = raport: zapis i ponowny odczyt
            quotedFields();                         // quoted fields = pola w cudzysłowie
            polishExcel();                          // Polish Excel = polski Excel
            exercises();                            // exercises = ćwiczenia
        } finally {
            TempDir.deleteRecursively(dir);
        }
    }

    // =================================================================================================
    // Pomocnicze: parser jednego wiersza, czytnik pliku i prosty parser z cudzysłowami
    // =================================================================================================

    /**
     * parseLine = zamień linię CSV na SaleRow. Złe dane → IllegalArgumentException z polskim powodem, który
     * trafi do raportu błędów. Oryginalny wyjątek zostaje jako przyczyna (cause), nic nie ginie
     * (t10_exceptions/Exceptions06ChainingWrapping).
     */
    static SaleRow parseLine(String line) {
        if (line.isBlank()) { // isBlank = pusta lub same spacje (Java 11+)
            throw new IllegalArgumentException("pusta linia");
        }
        String[] fields = line.split(",", -1); // -1 = zachowaj puste pola na końcu
        if (fields.length != 6) {
            throw new IllegalArgumentException("oczekiwano 6 pól, jest " + fields.length);
        }
        LocalDate date;
        try {
            date = LocalDate.parse(fields[0].trim()); // parse = zamień napis na datę (ISO: rok-miesiąc-dzień)
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("niepoprawna data '" + fields[0] + "'", e);
        }
        Category category;
        try {
            category = Category.valueOf(fields[3].trim()); // valueOf = enum o takiej nazwie
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("nieznana kategoria '" + fields[3] + "'", e);
        }
        int quantity;
        try {
            quantity = Integer.parseInt(fields[4].trim()); // parseInt = zamień napis na int
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ilość nie jest liczbą: '" + fields[4] + "'", e);
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("ilość musi być dodatnia: " + quantity);
        }
        BigDecimal price;
        try {
            price = new BigDecimal(fields[5].trim()); // pieniądze jako BigDecimal, nie double
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("cena nie jest liczbą: '" + fields[5] + "'", e);
        }
        if (price.signum() < 0) { // signum = znak liczby (-1, 0, 1)
            throw new IllegalArgumentException("cena nie może być ujemna: " + price);
        }
        return new SaleRow(date, fields[1].trim(), fields[2].trim(), category, quantity, price);
    }

    /** readSales = wczytaj plik sprzedaży; złe linie trafiają do listy błędów z numerem linii (od 1, nagłówek = 1). */
    static ParseResult readSales(Path file) throws IOException {
        List<SaleRow> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = reader.readLine(); // pierwsza linia to nagłówek
            if (!HEADER.equals(header)) {
                errors.add("linia 1: zły nagłówek");
                return new ParseResult(rows, errors);
            }
            String line;
            int lineNo = 1; // lineNo = numer linii; nagłówek już przeczytany
            while ((line = reader.readLine()) != null) {
                lineNo++;
                try {
                    rows.add(parseLine(line));
                } catch (IllegalArgumentException e) {
                    errors.add("linia " + lineNo + ": " + e.getMessage()); // zbieramy błąd i idziemy dalej
                }
            }
        }
        return new ParseResult(rows, errors);
    }

    /**
     * splitQuoted = podziel linię CSV z uwzględnieniem cudzysłowów. Mały automat: stan „jestem w cudzysłowie”
     * (inQuotes) zmienia znaczenie separatora. Dwa cudzysłowy pod rząd w środku pola to jeden zwykły cudzysłów.
     * Nie obsługuje pól z nową linią w środku (do tego trzeba czytać z całego strumienia, nie linia po linii).
     */
    static List<String> splitQuoted(String line, char separator) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder(); // current = bieżące pole
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i); // charAt = znak na pozycji
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"'); // "" w środku cudzysłowu = jeden znak "
                        i++;                 // pomijamy drugi cudzysłów
                    } else {
                        inQuotes = false;    // zamykający cudzysłów
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == separator) {
                fields.add(current.toString());
                current.setLength(0); // setLength(0) = wyczyść bufor
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString()); // ostatnie pole (po ostatnim separatorze)
        return fields;
    }

    /** quote = ujmij pole w cudzysłów, gdy to konieczne (separator, cudzysłów, nowa linia); cudzysłowy w środku podwój. */
    static String quote(String field, char separator) {
        boolean needsQuotes = field.indexOf(separator) >= 0 || field.indexOf('"') >= 0
                || field.indexOf('\n') >= 0 || field.indexOf('\r') >= 0; // needsQuotes = wymaga cudzysłowu
        return needsQuotes ? "\"" + field.replace("\"", "\"\"") + "\"" : field;
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
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
    // 1. PUŁAPKA split
    // =================================================================================================

    /**
     * 1. String.split(",") ma ukrytą pułapkę: domyślnie wyrzuca puste pola z KOŃCA. Drugi argument -1 wyłącza to.
     */
    static void splitPitfall() {
        section("1. split(\",\") kontra split(\",\", -1)");

        String line = "a,b,,"; // cztery pola: "a", "b", "" i ""
        show("split(\",\").length", line.split(",").length); // length = długość tablicy
        // WYNIK: split(",").length → 2
        show("split(\",\", -1).length", line.split(",", -1).length);
        // WYNIK: split(",", -1).length → 4
        show("split(\",\", -1)", Arrays.toString(line.split(",", -1))); // Arrays.toString = tablica jako napis
        // WYNIK: split(",", -1) → [a, b, , ]

        // PUŁAPKA: w pliku z kolumnami „imię,nazwisko,telefon” wiersz „Ola,Nowak,” (brak telefonu) po zwykłym split
        //   ma 2 pola zamiast 3 — kod sięgający po fields[2] rzuci ArrayIndexOutOfBoundsException. Z -1 dostajesz
        //   3 pola, a trzecie jest puste, co możesz obsłużyć świadomie.
        // Pusta linia daje jedno pole (""), nie zero pól:
        show("\"\".split(\",\", -1).length", "".split(",", -1).length);
        // WYNIK: "".split(",", -1).length → 1

        // split działa na WYRAŻENIU REGULARNYM: kropka lub kreska pionowa mają znaczenie specjalne. Przecinek i średnik
        // są zwykłymi znakami, ale split(".") albo split("|") zachowają się dziwnie — wtedy użyj Pattern.quote.

        // DOBRA PRAKTYKA: w parserze CSV zawsze split(separator, -1). Dlaczego: liczba pól musi się zgadzać z nagłówkiem
        //   także wtedy, gdy ostatnie kolumny są puste.
    }

    // =================================================================================================
    // 2. PARSOWANIE JEDNEGO WIERSZA
    // =================================================================================================

    /**
     * 2. Zamiana pól tekstowych na typy z walidacją. Każdy błąd ma jasny powód, bo na koniec pokażemy go użytkownikowi.
     */
    static void parsingOneRow() {
        section("2. Jeden wiersz → rekord SaleRow z walidacją");

        SaleRow row = parseLine("2026-05-04,KSI-001,Czysty kod,KSIAZKI,2,79.00");
        show("wiersz", row);
        // WYNIK: wiersz → SaleRow[date=2026-05-04, sku=KSI-001, name=Czysty kod, category=KSIAZKI, quantity=2, price=79.00]
        show("wartość (cena × ilość)", row.total());
        // WYNIK: wartość (cena × ilość) → 158.00

        // Błędne dane: wyjątek niesprawdzany (IllegalArgumentException) z powodem po polsku.
        expectThrows("ilość to tekst", () -> parseLine("2026-05-05,KSI-002,Java. Podstawy,KSIAZKI,dwa,129.00"));
        // WYNIK: ✔ ilość to tekst → rzucono IllegalArgumentException: ilość nie jest liczbą: 'dwa'
        expectThrows("za mało pól", () -> parseLine("2026-05-06,SPO-002,Czekolada gorzka"));
        // WYNIK: ✔ za mało pól → rzucono IllegalArgumentException: oczekiwano 6 pól, jest 3
        expectThrows("pusta linia", () -> parseLine(""));
        // WYNIK: ✔ pusta linia → rzucono IllegalArgumentException: pusta linia
        expectThrows("miesiąc 13", () -> parseLine("2026-13-06,DOM-002,Lampka biurkowa,DOM,1,129.00"));
        // WYNIK: ✔ miesiąc 13 → rzucono IllegalArgumentException: niepoprawna data '2026-13-06'
        expectThrows("nieznana kategoria", () -> parseLine("2026-05-06,X-1,Coś,BRON,1,10.00"));
        // WYNIK: ✔ nieznana kategoria → rzucono IllegalArgumentException: nieznana kategoria 'BRON'
        expectThrows("ujemna ilość", () -> parseLine("2026-05-06,X-1,Coś,DOM,-3,10.00"));
        // WYNIK: ✔ ujemna ilość → rzucono IllegalArgumentException: ilość musi być dodatnia: -3

        // DOBRA PRAKTYKA: waliduj od razu przy wczytaniu (data istnieje, ilość dodatnia, kategoria znana). Dlaczego:
        //   zły wiersz, który przejdzie dalej, psuje sumy i wychodzi dopiero w raporcie — daleko od przyczyny.
        // DOBRA PRAKTYKA: pieniądze jako BigDecimal z napisu (new BigDecimal("79.00")), nigdy double
        //   (t15_numbers/Numbers01BigDecimal). Dlaczego: double nie przechowuje dokładnie 0.1 ani 79.99.
    }

    // =================================================================================================
    // 3. CAŁY PLIK: BŁĘDY Z NUMERAMI LINII
    // =================================================================================================

    /**
     * 3. Zapisujemy dane próbne do pliku i czytamy je linia po linii. Zamiast przerwać na pierwszym błędzie, zbieramy
     * wszystkie błędy z numerami linii. Użytkownik dostaje komplet uwag do poprawienia za jednym razem.
     */
    static ParseResult readingWholeFile(Path file) throws IOException {
        section("3. Cały plik: poprawne wiersze i lista błędów");

        // Zapis jawnym "\n" (a nie Files.write(lines)): plik jest taki sam w Windows i na Linuksie (Io03WritingText).
        Files.writeString(file, String.join("\n", SampleData.salesCsvLines()) + "\n"); // join = sklej z separatorem
        show("linii w pliku (z nagłówkiem)", Files.readAllLines(file, StandardCharsets.UTF_8).size());
        // WYNIK: linii w pliku (z nagłówkiem) → 13

        ParseResult result = readSales(file);
        show("poprawnych wierszy", result.rows().size());
        // WYNIK: poprawnych wierszy → 8
        showEach("błędy", result.errors());
        // WYNIK: błędy (liczba elementów: 4):
        // WYNIK: • linia 6: ilość nie jest liczbą: 'dwa'
        // WYNIK: • linia 7: oczekiwano 6 pól, jest 3
        // WYNIK: • linia 8: pusta linia
        // WYNIK: • linia 9: niepoprawna data '2026-13-06'
        show("pierwszy poprawny", result.rows().get(0));
        // WYNIK: pierwszy poprawny → SaleRow[date=2026-05-04, sku=KSI-001, name=Czysty kod, category=KSIAZKI, quantity=2, price=79.00]

        // Numer linii liczymy tak, jak widzi go edytor: nagłówek to linia 1, więc pierwsza dana to linia 2.
        // Dlaczego nie przerywamy na pierwszym błędzie? Przy pliku z 10 000 wierszy poprawianie po jednym błędzie
        // i ponowne ładowanie to męka. Gdy jednak dane MUSZĄ być w całości poprawne (np. import finansowy),
        // po zebraniu błędów odrzuć cały plik: if (!errors.isEmpty()) throw ...

        // PUŁAPKA: plik z Excela bywa zapisany z BOM (znacznik kodowania na początku: bajty EF BB BF). Pierwsza kolumna
        //   nazywa się wtedy „data” z niewidocznym znakiem BOM (U+FEFF) na początku i porównanie nagłówka zawodzi — patrz Io12Charsets.
        // PUŁAPKA: kodowanie. Excel w Polsce zapisuje „CSV” często w windows-1250, nie UTF-8; czytając jako UTF-8
        //   dostaniesz MalformedInputException (Io02ReadingText). Ustal kodowanie z dostawcą pliku.
        return result;
    }

    // =================================================================================================
    // 4. PODSUMOWANIE WG KATEGORII
    // =================================================================================================

    /**
     * 4. Suma wartości wierszy według kategorii. EnumMap trzyma klucze w kolejności deklaracji enuma — wynik jest
     * stały (HashMap z kluczami-enumami miałby przypadkową kolejność). Kwoty sumujemy jako BigDecimal.
     */
    static void summaryPerCategory(List<SaleRow> rows) {
        section("4. Podsumowanie: wartość sprzedaży wg kategorii");

        Map<Category, BigDecimal> totals = new EnumMap<>(Category.class); // EnumMap = mapa z kluczami-enumami
        for (SaleRow row : rows) {
            totals.merge(row.category(), row.total(), BigDecimal::add); // merge = dodaj lub połącz; add = dodaj
        }
        showEach("wartość wg kategorii", totals);
        // WYNIK: wartość wg kategorii (liczba kluczy: 5):
        // WYNIK: • ELEKTRONIKA → 5849.89
        // WYNIK: • SPOZYWCZE → 269.87
        // WYNIK: • KSIAZKI → 257.00
        // WYNIK: • ODZIEZ → 199.96
        // WYNIK: • DOM → 1899.00

        BigDecimal all = BigDecimal.ZERO;
        for (BigDecimal value : totals.values()) {
            all = all.add(value);
        }
        show("razem", all);
        // WYNIK: razem → 8475.72
        // Niepoprawne linie 6–9 w ogóle nie weszły do rows, więc nie ma ich w sumach.

        // Ten sam wynik w stylu strumieniowym (t16_streams/Streams11GroupingBy), tu w wersji z TreeMap po nazwie:
        Map<String, BigDecimal> byName = new TreeMap<>(); // TreeMap = klucze posortowane alfabetycznie
        rows.forEach(row -> byName.merge(row.category().name(), row.total(), BigDecimal::add)); // name = nazwa enuma
        show("alfabetycznie (TreeMap)", byName);
        // WYNIK: alfabetycznie (TreeMap) → {DOM=1899.00, ELEKTRONIKA=5849.89, KSIAZKI=257.00, ODZIEZ=199.96, SPOZYWCZE=269.87}

        // DOBRA PRAKTYKA: EnumMap, gdy kluczem jest enum (szybka i uporządkowana); TreeMap, gdy chcesz porządek alfabetyczny.
        //   Dlaczego nie HashMap: wypisanie HashMap z kluczami-enumami daje inną kolejność przy każdym uruchomieniu.
        // PUŁAPKA: BigDecimal porównuj przez compareTo (nie equals — 79.0 i 79.00 to dla equals różne liczby).
    }

    // =================================================================================================
    // 5. RAPORT: ZAPIS I PONOWNY ODCZYT
    // =================================================================================================

    /**
     * 5. Zapisujemy raport jako nowy CSV i czytamy go z powrotem. „Okrągła podróż” (round trip) — zapis, odczyt, porównanie —
     * to najprostszy test, że format jest spójny.
     */
    static void reportRoundTrip(Path dir, List<SaleRow> rows) throws IOException {
        section("5. Raport CSV: zapis i ponowne wczytanie");

        Map<Category, BigDecimal> totals = new EnumMap<>(Category.class);
        for (SaleRow row : rows) {
            totals.merge(row.category(), row.total(), BigDecimal::add);
        }

        Path report = dir.resolve("raport.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(report, StandardCharsets.UTF_8)) {
            writer.write("kategoria,suma\n"); // jawny "\n": plik identyczny w Windows i na Linuksie
            for (Map.Entry<Category, BigDecimal> entry : totals.entrySet()) { // entrySet = pary klucz-wartość
                // toPlainString = zapis bez notacji naukowej (bez "E+3"); kropka dziesiętna niezależna od Locale
                writer.write(entry.getKey().name() + "," + entry.getValue().toPlainString() + "\n");
            }
        }
        showEach("zawartość raportu", Files.readAllLines(report, StandardCharsets.UTF_8));
        // WYNIK: zawartość raportu (liczba elementów: 6):
        // WYNIK: • kategoria,suma
        // WYNIK: • ELEKTRONIKA,5849.89
        // WYNIK: • SPOZYWCZE,269.87
        // WYNIK: • KSIAZKI,257.00
        // WYNIK: • ODZIEZ,199.96
        // WYNIK: • DOM,1899.00

        // Wczytanie raportu z powrotem do mapy:
        Map<Category, BigDecimal> reread = new EnumMap<>(Category.class); // reread = wczytane ponownie
        List<String> lines = Files.readAllLines(report, StandardCharsets.UTF_8);
        for (String line : lines.subList(1, lines.size())) { // subList = fragment listy (pomijamy nagłówek)
            String[] fields = line.split(",", -1);
            reread.put(Category.valueOf(fields[0]), new BigDecimal(fields[1]));
        }
        show("po wczytaniu równe oryginałowi", reread.equals(totals)); // equals dla BigDecimal uwzględnia skalę: obie 2 cyfry
        // WYNIK: po wczytaniu równe oryginałowi → true

        // PUŁAPKA: w CSV skala (liczba cyfr po przecinku) jest częścią tekstu. "257.00" i "257.0" to dla BigDecimal.equals
        //   różne liczby. Przy porównywaniu raportów używaj compareTo albo zawsze zapisuj tyle samo miejsc
        //   (setScale(2, RoundingMode.HALF_UP) — t15_numbers/Numbers01BigDecimal).
        // PUŁAPKA: CSV injection (wstrzyknięcie formuły). Pole zaczynające się od =, +, - lub @ Excel uzna za formułę, więc
        //   dane od użytkownika (np. nazwa „=HYPERLINK(...)”) po otwarciu w Excelu mogą wykonać polecenie.
        //   Zabezpieczenie: poprzedź takie pole apostrofem.
    }

    // =================================================================================================
    // 6. PRAWDZIWY CSV: CUDZYSŁOWY
    // =================================================================================================

    /**
     * 6. Gdy w polu jest przecinek („Kawa, ziarnista”), standard CSV każe ująć pole w cudzysłów. Zwykłe split tego nie wie
     * i rozbija jedno pole na dwa.
     */
    static void quotedFields() {
        section("6. Cudzysłowy w CSV — gdzie split zawodzi");

        String line = "1,\"Kawa, ziarnista 1kg\",64.99";

        String[] naive = line.split(",", -1); // naive = naiwny sposób
        show("split: liczba pól (powinno być 3)", naive.length);
        // WYNIK: split: liczba pól (powinno być 3) → 4
        show("split: pola", String.join(" | ", naive));
        // WYNIK: split: pola → 1 | "Kawa |  ziarnista 1kg" | 64.99

        List<String> proper = splitQuoted(line, ','); // proper = poprawny sposób
        show("splitQuoted: liczba pól", proper.size());
        // WYNIK: splitQuoted: liczba pól → 3
        show("splitQuoted: pola", String.join(" | ", proper));
        // WYNIK: splitQuoted: pola → 1 | Kawa, ziarnista 1kg | 64.99

        // Cudzysłów w środku pola podwajamy: pole  Monitor 27" (cala)  zapisujemy jako  "Monitor 27"" (cala)".
        show("cudzysłów w polu", splitQuoted("2,\"Monitor 27\"\" (cala)\",1299.00", ','));
        // WYNIK: cudzysłów w polu → [2, Monitor 27" (cala), 1299.00]
        show("puste pola", splitQuoted("a,,c,", ','));
        // WYNIK: puste pola → [a, , c, ]

        show("quote(zwykłe)", quote("Czysty kod", ','));
        // WYNIK: quote(zwykłe) → Czysty kod
        show("quote(z przecinkiem)", quote("Kawa, ziarnista", ','));
        // WYNIK: quote(z przecinkiem) → "Kawa, ziarnista"
        show("quote(z cudzysłowem)", quote("Monitor 27\"", ','));
        // WYNIK: quote(z cudzysłowem) → "Monitor 27"""

        List<String> original = List.of("Kawa, ziarnista", "Monitor 27\"", "zwykłe", "");
        StringBuilder csvLine = new StringBuilder();
        for (String field : original) {
            if (csvLine.length() > 0) {
                csvLine.append(',');
            }
            csvLine.append(quote(field, ','));
        }
        show("zapisana linia", csvLine);
        // WYNIK: zapisana linia → "Kawa, ziarnista","Monitor 27""",zwykłe,
        show("wczytana równa oryginałowi", splitQuoted(csvLine.toString(), ',').equals(original));
        // WYNIK: wczytana równa oryginałowi → true

        // Granice naszego automatu: pole może też zawierać NOWĄ LINIĘ w cudzysłowie. Wtedy jeden rekord rozciąga się na
        // kilka linii pliku i czytanie readLine() nie wystarczy. Są też inne dziwactwa (BOM, różne końce linii,
        // komentarze, nagłówki w kilku liniach).
        // DOBRA PRAKTYKA: w prawdziwych projektach użyj biblioteki, nie własnego parsera: OpenCSV, Apache Commons CSV albo
        //   Jackson CSV (jackson-dataformat-csv). Dlaczego: obsługują cudzysłowy, nowe linie w polach, różne separatory,
        //   mapowanie na klasy i kodowania — a ich błędy ktoś już poprawił. Własny parser (jak tu) jest dobry do nauki
        //   i do w pełni znanych, prostych danych.
    }

    // =================================================================================================
    // 7. POLSKI EXCEL: ŚREDNIK I PRZECINEK DZIESIĘTNY
    // =================================================================================================

    /**
     * 7. Excel z polskimi ustawieniami eksportuje „CSV” ze ŚREDNIKIEM jako separatorem i PRZECINKIEM dziesiętnym ("79,00").
     * Przecinek jest już zajęty jako część liczby, więc kolumny rozdziela średnik.
     */
    static void polishExcel() {
        section("7. Polski Excel: średnik i przecinek dziesiętny");

        String line = "04.05.2026;KSI-001;Czysty kod;KSIAZKI;2;79,00";
        String[] fields = line.split(";", -1);
        show("liczba pól po średniku", fields.length);
        // WYNIK: liczba pól po średniku → 6

        try {
            new BigDecimal(fields[5]);
        } catch (NumberFormatException e) {
            System.out.println("✔ new BigDecimal(\"79,00\") → rzucono " + e.getClass().getSimpleName());
            // WYNIK: ✔ new BigDecimal("79,00") → rzucono NumberFormatException
        }
        BigDecimal price = new BigDecimal(fields[5].trim().replace(',', '.')); // replace = zamień znak
        show("po zamianie przecinka na kropkę", price);
        // WYNIK: po zamianie przecinka na kropkę → 79.00

        // Data w polskim formacie dd.MM.yyyy: formatter z wzorcem (wzorzec: dd dzień, MM miesiąc, yyyy rok).
        DateTimeFormatter polish = DateTimeFormatter.ofPattern("dd.MM.yyyy"); // ofPattern = z wzorca
        LocalDate date = LocalDate.parse(fields[0], polish);
        show("data", date);
        // WYNIK: data → 2026-05-04

        String out = String.join(";", date.format(polish), "KSI-001", "Czysty kod", "KSIAZKI", "2",
                price.toPlainString().replace('.', ','));
        show("linia w stylu polskim", out);
        // WYNIK: linia w stylu polskim → 04.05.2026;KSI-001;Czysty kod;KSIAZKI;2;79,00

        // PUŁAPKA: ten sam kod z ręczną zamianą przecinka zepsuje liczby z separatorem tysięcy ("1 299,00" — spacja
        //   albo twarda spacja U+00A0, albo "1.299,00" w niektórych krajach). Dla takich danych użyj NumberFormat z
        //   jawnym Locale i uważaj na spacje, albo ustal z dostawcą format bez separatora tysięcy.
        // PUŁAPKA: ten sam plik „CSV” otwarty w Excelu w innych ustawieniach regionalnych (angielskich) pokaże jedną kolumnę:
        //   tam separatorem jest przecinek. Wybór separatora i liczb to umowa — zapisz ją w dokumentacji formatu.
        // DOBRA PRAKTYKA: pliki wymieniane między SYSTEMAMI (API, import) rób w jednym formacie technicznym: przecinek lub
        //   średnik, kropka dziesiętna, data ISO (2026-05-04), kodowanie UTF-8. Format „polski” zostaw dla ludzi i Excela.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • CSV = linie, pola rozdzielone separatorem; pierwsza linia zwykle nagłówek (sprawdź go).
     *   • split(",") obcina puste pola z końca — używaj split(",", -1).
     *   • Waliduj przy wczytaniu (data, liczby, enum, zakresy) i zbieraj błędy "linia N: powód" zamiast przerywać.
     *   • Numery linii dla ludzi od 1; nagłówek to linia 1.
     *   • Pieniądze jako BigDecimal z napisu; sumy przez merge(klucz, wartość, BigDecimal::add); EnumMap/TreeMap dla stałej kolejności.
     *   • Prawdziwy CSV: pole z separatorem, cudzysłowem lub nową linią w cudzysłowie, cudzysłów w środku podwajamy.
     *   • Własny parser tylko do prostych danych; w projekcie OpenCSV, Apache Commons CSV albo Jackson CSV.
     *   • Polski Excel: średnik + przecinek dziesiętny + często windows-1250; dane między systemami: ISO + UTF-8.
     *   • Zapis: jawne "\n" (plik identyczny wszędzie), kropka dziesiętna z toPlainString, uwaga na CSV injection (=, +, -, @).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego "a,b,,".split(",") daje inną długość niż split(",", -1)? Ile pól ma każda wersja?
     *   2. Czemu czytnik zbiera błędy z numerami linii zamiast rzucić wyjątek przy pierwszym?
     *   3. Co wypisze:  System.out.println("1,\"a,b\",3".split(",").length);  ?
     *   4. Co wypisze:  System.out.println("x;;".split(";", -1).length);  ?
     *   5. ZNAJDŹ BŁĄD:  double price = Double.parseDouble(fields[5]);  — pole to "79,00" z polskiego Excela. Co się stanie
     *      i dlaczego double to zły wybór na kwoty?
     *   6. ZNAJDŹ BŁĄD:  Map<Category, BigDecimal> m = new HashMap<>();  ...  System.out.println(m);  — czemu wydruk
     *      bywa za każdym razem inny?
     *   7. Jak w CSV zapisać pole o treści:  Monitor 27", czarny  (jest w nim cudzysłów i przecinek)?
     *   8. Kiedy wystarczy własny split, a kiedy sięgasz po bibliotekę?
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
        Path dir = TempDir.create("io04cw");
        try {
            Check.equal("ćw. 1: liczba pól", "4|1|1", () ->
                    (reference ? solution1("a,b,,") : exercise1("a,b,,")) + "|"
                            + (reference ? solution1("") : exercise1("")) + "|"
                            + (reference ? solution1("x") : exercise1("x")));

            Check.equal("ćw. 2: cena po polsku", List.of(new BigDecimal("79.00"), new BigDecimal("129.50"), new BigDecimal("1299")),
                    () -> {
                        List<BigDecimal> prices = new ArrayList<>();
                        for (String text : List.of("79,00", " 129,5 ", "1299")) {
                            prices.add(reference ? solution2(text) : exercise2(text));
                        }
                        return prices;
                    });

            Map<String, Integer> expected = new TreeMap<>(Map.of("DOM", 1, "ELEKTRONIKA", 2, "KSIAZKI", 2, "ODZIEZ", 1, "SPOZYWCZE", 2));
            Check.equal("ćw. 3: liczba wierszy wg kategorii (PRZEPISZ)", expected,
                    () -> reference ? solution3(SampleData.salesCsvLines()) : exercise3(SampleData.salesCsvLines()));

            Check.equal("ćw. 4: quote", "zwykłe|\"a,b\"|\"on rzekł \"\"hej\"\"\"", () ->
                    (reference ? solution4("zwykłe") : exercise4("zwykłe")) + "|"
                            + (reference ? solution4("a,b") : exercise4("a,b")) + "|"
                            + (reference ? solution4("on rzekł \"hej\"") : exercise4("on rzekł \"hej\"")));

            Path in = dir.resolve("wejscie.csv");
            Path out = dir.resolve("wyjscie.csv");
            Check.equal("ćw. 5: raport po polsku", List.of("kategoria;suma", "ELEKTRONIKA;5849,89", "SPOZYWCZE;269,87",
                    "KSIAZKI;257,00", "ODZIEZ;199,96", "DOM;1899,00"), io(() -> {
                        Files.writeString(in, String.join("\n", SampleData.salesCsvLines()) + "\n");
                        if (reference) {
                            solution5(in, out);
                        } else {
                            exercise5(in, out);
                        }
                        return Files.readAllLines(out, StandardCharsets.UTF_8);
                    }));
        } finally {
            TempDir.deleteRecursively(dir);
        }
        Check.summary();
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć liczbę pól linii CSV rozdzielonej przecinkami, wliczając puste pola na końcu.
     * "a,b,," ma 4 pola, "" ma 1, "x" ma 1.
     * Podpowiedź: split(",", -1).length.
     */
    static int exercise1(String line) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (łatwe): zamień cenę z polskiego Excela na BigDecimal: "79,00" → 79.00, " 129,5 " → 129.50
     * (są spacje wokół), "1299" → 1299 (bez przecinka też ma działać).
     * Podpowiedź: trim, replace(',', '.'), new BigDecimal(napis).
     */
    static BigDecimal exercise2(String text) {
        // TODO: twoje rozwiązanie
        return BigDecimal.ZERO;
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ. Stary kod połyka błędy po cichu i nie wiadomo, ile linii pominął:
     * <pre>{@code
     * for (String line : lines) {
     *     try {
     *         String[] p = line.split(",");
     *         counts.merge(p[3], 1, Integer::sum);
     *     } catch (Exception e) {
     *         // ignorujemy
     *     }
     * }
     * }</pre>
     * Nowa wersja: pomiń nagłówek (pierwszą linię), użyj parseLine z tej lekcji, błędne linie (IllegalArgumentException)
     * pomijaj, a poprawne policz wg NAZWY kategorii (SaleRow.category().name()). Zwróć mapę nazwa → liczba.
     * Podpowiedź: lines.subList(1, lines.size()), TreeMap, merge.
     */
    static Map<String, Integer> exercise3(List<String> lines) {
        // TODO: twoje rozwiązanie
        return Map.of();
    }

    /**
     * ĆWICZENIE 4 (średnie): zapisz pole do CSV: gdy zawiera przecinek lub cudzysłów, ujmij je w cudzysłów i podwój
     * cudzysłowy w środku. "zwykłe" → zwykłe; "a,b" → "a,b" (z cudzysłowami); on rzekł "hej" → "on rzekł ""hej""".
     * Podpowiedź: contains(",") || contains("\""), replace("\"", "\"\"").
     */
    static String exercise4(String field) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): wczytaj plik sprzedaży in (jak w lekcji: nagłówek + dane + błędne linie) i zapisz do out
     * raport w stylu polskim: linia nagłówka "kategoria;suma", potem po jednej linii "KATEGORIA;kwota" z PRZECINKIEM
     * dziesiętnym (np. "KSIAZKI;257,00"), w kolejności deklaracji enuma Category (EnumMap). Zapis z jawnym "\n", UTF-8.
     * Podpowiedź: readSales(in) z tej lekcji, EnumMap + merge, toPlainString().replace('.', ',').
     */
    static void exercise5(Path in, Path out) throws IOException {
        // TODO: twoje rozwiązanie
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(String line) {
        return line.split(",", -1).length;
    }

    static BigDecimal solution2(String text) {
        return new BigDecimal(text.trim().replace(',', '.'));
    }

    static Map<String, Integer> solution3(List<String> lines) {
        Map<String, Integer> counts = new TreeMap<>();
        for (String line : lines.subList(1, lines.size())) {
            try {
                counts.merge(parseLine(line).category().name(), 1, Integer::sum);
            } catch (IllegalArgumentException e) {
                // zły wiersz pomijamy; w prawdziwym programie zapisalibyśmy powód (jak w readSales)
            }
        }
        return counts;
    }

    static String solution4(String field) {
        if (field.contains(",") || field.contains("\"")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    static void solution5(Path in, Path out) throws IOException {
        Map<Category, BigDecimal> totals = new EnumMap<>(Category.class);
        for (SaleRow row : readSales(in).rows()) {
            totals.merge(row.category(), row.total(), BigDecimal::add);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            writer.write("kategoria;suma\n");
            for (Map.Entry<Category, BigDecimal> entry : totals.entrySet()) {
                writer.write(entry.getKey().name() + ";" + entry.getValue().toPlainString().replace('.', ',') + "\n");
            }
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. split(",") bez drugiego argumentu usuwa puste pola z końca, więc "a,b,," daje 2 pola. Z -1 puste pola
     *      zostają: 4 pola ("a", "b", "", "").
     *   2. Bo użytkownik dostaje komplet uwag za jednym razem i może poprawić cały plik naraz; przy 10 000 wierszy
     *      poprawianie po jednym błędzie jest nie do zniesienia. Gdy dane muszą być w całości poprawne, po zebraniu
     *      błędów odrzucamy cały plik.
     *   3. 4 — split(",") rozbija też przecinek w cudzysłowie: 1, "a, b", 3 → cztery kawałki.
     *   4. 3 — pola: "x", "" i "" (z -1 puste pola na końcu zostają).
     *   5. Double.parseDouble("79,00") rzuca NumberFormatException (przecinek zamiast kropki). Trzeba najpierw zamienić
     *      przecinek na kropkę. A double nie przechowuje dokładnie wielu kwot (np. 0.1), więc na pieniądze
     *      używamy BigDecimal z napisu.
     *   6. Kolejność w HashMap z kluczami-enumami zależy od kodów skrótu (identity hash), które zmieniają się między
     *      uruchomieniami. Użyj EnumMap (kolejność deklaracji) albo TreeMap.
     *   7. Całe pole w cudzysłów, a cudzysłów w środku podwojony:  "Monitor 27"", czarny"
     *   8. Własny split: małe, w pełni znane dane bez cudzysłowów (np. własny eksport). Biblioteka (OpenCSV, Apache
     *      Commons CSV, Jackson CSV): dane z zewnątrz, możliwe cudzysłowy, nowe linie w polach, różne kodowania i separatory.
     */
    // </editor-fold>
}
