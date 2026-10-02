package t17_datetime;

import helpers.Check;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: DateTimeFormatter dla zaawansowanych — gotowe formaty ISO, formaty lokalne, Builder, parsowanie
 *        (formatter = formatujący; builder = budowniczy; resolver = rozstrzygacz; parse = odczytaj z tekstu)
 *
 * W SKRÓCIE:
 *   DateTime03Formatting pokazała wzorce ofPattern. Tu schodzimy poziom niżej: gotowe formatery ISO i lokalne
 *   (ofLocalizedDate...), własne formatery składane klockami (DateTimeFormatterBuilder), przyjmowanie kilku formatów
 *   wejściowych, tryby rozstrzygania (ResolverStyle), parsowanie do TemporalAccessor, ręczne formatowanie
 *   Duration i Period oraz współpraca ze starymi klasami Date i SimpleDateFormat.
 *
 * ANALOGIA:
 *   ofPattern to gotowa foremka do ciastek: jeden kształt, szybko. DateTimeFormatterBuilder to klocki LEGO:
 *   sam układasz kolejne elementy (liczba, tekst, znak, część opcjonalna). Rozstrzyganie (resolver) to
 *   kontrola na lotnisku: sprawdza, czy z odczytanych kawałków (rok, miesiąc, dzień) da się złożyć prawdziwą datę
 *   — i decyduje, jak surowo ją potraktować (STRICT = bardzo, SMART = rozsądnie, LENIENT = przymyka oko).
 *
 * JAK TO DZIAŁA:
 *   1. FORMATOWANIE: obiekt (LocalDate, ZonedDateTime...) → formatter pyta go o pola (rok, miesiąc, ...) → tekst.
 *      Brak pola (np. godziny w LocalDate) = wyjątek UnsupportedTemporalTypeException.
 *   2. PARSOWANIE ma DWA kroki:
 *        tekst → (parse) → surowe pola {rok=2026, miesiąc=3, dzień=8} → (resolve) → LocalDate / LocalTime ...
 *      Krok drugi robi ResolverStyle:
 *        STRICT  — każde pole musi być w zakresie i data musi istnieć (30 lutego = błąd),
 *        SMART   — domyślny w ofPattern: dzień 29–31 jest przycinany do końca miesiąca (30 lutego = 28 lutego),
 *        LENIENT — nadmiar się przelewa (30 lutego = 2 marca, miesiąc 13 = styczeń następnego roku).
 *   3. Formatery są NIEZMIENNE i bezpieczne wątkowo — można je trzymać w static final.
 *
 *   Które formatery do czego:
 *     ISO_*                — wymiana danych (JSON, bazy, protokoły); ten sam zapis na całym świecie,
 *     ofLocalized*         — to, co widzi człowiek; zależy od Locale (kraju/języka),
 *     ofPattern            — własny, stały układ (raporty, pliki),
 *     DateTimeFormatterBuilder — gdy wzorzec nie wystarcza (opcje, wartości domyślne, własne nazwy).
 *
 * SŁÓWKA:
 *   predefined = gotowy; localized = lokalny (zależny od języka/kraju); style = styl; builder = budowniczy;
 *   optional = opcjonalny; default = domyślny; sign = znak (+/-); case insensitive = bez względu na wielkość liter;
 *   resolver = rozstrzygacz; strict = ścisły; smart = rozsądny; lenient = pobłażliwy; query = zapytanie;
 *   legacy = dziedzictwo (stary kod); thread-safe = bezpieczny wątkowo; standalone = samodzielny (mianownik).
 *
 * ZOBACZ TEŻ: t17_datetime/DateTime03Formatting (podstawy wzorców), t17_datetime/DateTime04ZonesInstant (strefy),
 *             t15_numbers/Numbers04FormattingParsing (liczby i waluty), t31_jdk_toolbox/Toolbox03I18n (tłumaczenia)
 * </pre>
 */
public class DateTime06FormatterAdvanced {

    // Stałe lekcji — wszystkie wyniki są przez to powtarzalne (żadnego now() ani domyślnej strefy/Locale).
    private static final Locale PL = Locale.forLanguageTag("pl-PL");  // locale = ustawienia językowe i krajowe
    private static final Locale US = Locale.forLanguageTag("en-US");
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");  // zone = strefa
    private static final LocalDateTime DT = LocalDateTime.of(2026, 3, 8, 14, 5, 9);  // niedziela
    private static final ZonedDateTime ZDT = DT.atZone(WARSAW);  // 14:05:09 w Warszawie = +01:00 (zima)

    public static void main(String[] args) {
        title("DateTime06 — DateTimeFormatter dla zaawansowanych");

        isoFormatters();          // isoFormatters = formatery ISO
        localizedFormats();       // localizedFormats = formaty lokalne
        localizedPitfalls();      // localizedPitfalls = pułapki formatów lokalnych
        builderBasics();          // builderBasics = podstawy Buildera
        monthNames();             // monthNames = nazwy miesięcy
        caseAndManyFormats();     // caseAndManyFormats = wielkość liter i wiele formatów
        resolverStyles();         // resolverStyles = tryby rozstrzygania
        parsedAndQueries();       // parsedAndQueries = wynik parsowania i zapytania
        durationAndPeriod();      // durationAndPeriod = Duration i Period
        threadSafetyAndLegacy();  // threadSafetyAndLegacy = bezpieczeństwo wątkowe i stary kod
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI
    // =================================================================================================

    /**
     * Formaty lokalne mogą zawierać twarde spacje: U+00A0 (spacja niełamliwa) i U+202F (wąska spacja niełamliwa).
     * Wyglądają jak zwykła spacja, ale {@code equals} z tekstem ze zwykłą spacją da false. Zamieniamy je na ' '.
     * Znaki budujemy rzutowaniem (char), żeby w kodzie nie było niewidocznych znaków.
     */
    static String clean(String s) {
        return s.replace((char) 0x00A0, ' ').replace((char) 0x202F, ' ');
    }

    /** Pokazuje wynik albo nazwę wyjątku (bez komunikatu — niektóre komunikaty zależą od kolejności w HashMap). */
    static void tryIt(String label, Supplier<Object> action) {
        try {
            show(label, action.get());
        } catch (DateTimeException e) {   // DateTimeParseException i UnsupportedTemporalTypeException to jego dzieci
            show(label, "wyjątek " + e.getClass().getSimpleName());
        }
    }

    // =================================================================================================
    // 1. GOTOWE FORMATERY ISO
    // =================================================================================================

    /**
     * 1. Klasa DateTimeFormatter ma gotowe stałe (static final) dla standardowych zapisów. Nie trzeba pisać wzorca,
     * nie można go też źle napisać. Zapis ISO 8601 (2026-03-08T14:05:09) jest bezpieczny do wymiany danych,
     * bo nie zależy od języka ani kraju.
     */
    static void isoFormatters() {
        section("1. Gotowe formatery ISO (DateTimeFormatter.ISO_...)");

        show("ISO_LOCAL_DATE", DateTimeFormatter.ISO_LOCAL_DATE.format(DT));
        // WYNIK: ISO_LOCAL_DATE → 2026-03-08
        show("ISO_LOCAL_TIME", DateTimeFormatter.ISO_LOCAL_TIME.format(DT));
        // WYNIK: ISO_LOCAL_TIME → 14:05:09
        show("ISO_LOCAL_DATE_TIME", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(DT));
        // WYNIK: ISO_LOCAL_DATE_TIME → 2026-03-08T14:05:09
        // Wersje ze strefą potrzebują ZonedDateTime (albo OffsetDateTime) — LocalDateTime nie zna przesunięcia.
        show("ISO_OFFSET_DATE_TIME", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZDT));
        // WYNIK: ISO_OFFSET_DATE_TIME → 2026-03-08T14:05:09+01:00
        show("ISO_ZONED_DATE_TIME", DateTimeFormatter.ISO_ZONED_DATE_TIME.format(ZDT));
        // WYNIK: ISO_ZONED_DATE_TIME → 2026-03-08T14:05:09+01:00[Europe/Warsaw]
        // ISO_DATE_TIME = „jak się da”: część ze strefą jest opcjonalna, więc przyjmie obie wersje.
        show("ISO_DATE_TIME (ze strefą)", DateTimeFormatter.ISO_DATE_TIME.format(ZDT));
        // WYNIK: ISO_DATE_TIME (ze strefą) → 2026-03-08T14:05:09+01:00[Europe/Warsaw]
        show("ISO_DATE_TIME (bez strefy)", DateTimeFormatter.ISO_DATE_TIME.format(DT));
        // WYNIK: ISO_DATE_TIME (bez strefy) → 2026-03-08T14:05:09
        // ISO_INSTANT zawsze pisze czas UTC (litera Z = „Zulu” = UTC), bo to zapis punktu na osi czasu.
        show("ISO_INSTANT", DateTimeFormatter.ISO_INSTANT.format(ZDT));
        // WYNIK: ISO_INSTANT → 2026-03-08T13:05:09Z
        show("BASIC_ISO_DATE", DateTimeFormatter.BASIC_ISO_DATE.format(DT));
        // WYNIK: BASIC_ISO_DATE → 20260308
        show("BASIC_ISO_DATE (ze strefą)", DateTimeFormatter.BASIC_ISO_DATE.format(ZDT));
        // WYNIK: BASIC_ISO_DATE (ze strefą) → 20260308+0100
        show("ISO_WEEK_DATE", DateTimeFormatter.ISO_WEEK_DATE.format(DT));
        // WYNIK: ISO_WEEK_DATE → 2026-W10-7
        show("ISO_ORDINAL_DATE", DateTimeFormatter.ISO_ORDINAL_DATE.format(DT));
        // WYNIK: ISO_ORDINAL_DATE → 2026-067
        // RFC_1123_DATE_TIME = zapis z nagłówków HTTP i e-maili. Nazwy dni i miesięcy są ZAWSZE angielskie.
        show("RFC_1123_DATE_TIME", DateTimeFormatter.RFC_1123_DATE_TIME.format(ZDT));
        // WYNIK: RFC_1123_DATE_TIME → Sun, 8 Mar 2026 14:05:09 +0100
        show("RFC_1123 → odczyt", ZonedDateTime.parse("Sun, 8 Mar 2026 14:05:09 +0100",
                DateTimeFormatter.RFC_1123_DATE_TIME));
        // WYNIK: RFC_1123 → odczyt → 2026-03-08T14:05:09+01:00

        // To samo robi toString() klas java.time — dlatego LocalDate.parse("2026-03-08") działa bez formatera.
        show("LocalDateTime.toString()", DT);
        // WYNIK: LocalDateTime.toString() → 2026-03-08T14:05:09

        // PUŁAPKA: ISO_INSTANT pyta o „sekundy od epoki” — LocalDateTime ich nie ma, bo nie zna strefy.
        // Dlaczego: bez strefy nie wiadomo, o którą chwilę na osi czasu chodzi.
        expectThrows("ISO_INSTANT na LocalDateTime", () -> DateTimeFormatter.ISO_INSTANT.format(DT));
        // WYNIK: ✔ ISO_INSTANT na LocalDateTime → rzucono UnsupportedTemporalTypeException: Unsupported field: InstantSeconds
        expectThrows("ISO_OFFSET_DATE_TIME na LocalDateTime",
                () -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(DT));
        // WYNIK: ✔ ISO_OFFSET_DATE_TIME na LocalDateTime → rzucono UnsupportedTemporalTypeException: Unsupported field: OffsetSeconds

        // DOBRA PRAKTYKA: do plików, API i baz używaj formaterów ISO (stałych), a nie formatów „dla ludzi”.
        // Dlaczego: format dla ludzi zależy od Locale i wersji JDK — dane zapisane dziś mogą się nie dać
        // odczytać jutro; ISO jest stałe i zrozumiałe dla każdego systemu.
    }

    // =================================================================================================
    // 2. FORMATY LOKALNE
    // =================================================================================================

    /**
     * 2. ofLocalizedDate / ofLocalizedTime / ofLocalizedDateTime dobierają układ do kraju i języka (Locale)
     * oraz do stylu FormatStyle: SHORT (krótki), MEDIUM (średni), LONG (długi), FULL (pełny).
     * Układ i nazwy pochodzą z bazy CLDR wbudowanej w JDK — nie piszesz ich sam.
     */
    static void localizedFormats() {
        section("2. Formaty lokalne — ofLocalizedDate/Time/DateTime");

        // withLocale = z ustawieniami językowymi. Bez niego formatter użyłby domyślnego Locale komputera
        // (a ono jest inne u ucznia, na serwerze i w IntelliJ) — wynik nie byłby powtarzalny.
        List<String> dateLines = new ArrayList<>();
        for (FormatStyle style : FormatStyle.values()) {
            for (Locale loc : List.of(PL, US)) {
                String text = DateTimeFormatter.ofLocalizedDate(style).withLocale(loc).format(DT);
                dateLines.add(style + " " + loc.toLanguageTag() + " = " + clean(text));
            }
        }
        showEach("data", dateLines);
        // WYNIK: data (liczba elementów: 8):
        // WYNIK:    • FULL pl-PL = niedziela, 8 marca 2026
        // WYNIK:    • FULL en-US = Sunday, March 8, 2026
        // WYNIK:    • LONG pl-PL = 8 marca 2026
        // WYNIK:    • LONG en-US = March 8, 2026
        // WYNIK:    • MEDIUM pl-PL = 8 mar 2026
        // WYNIK:    • MEDIUM en-US = Mar 8, 2026
        // WYNIK:    • SHORT pl-PL = 08.03.2026
        // WYNIK:    • SHORT en-US = 3/8/26

        // Czas: LONG i FULL dopisują nazwę strefy, więc dajemy ZonedDateTime (zob. sekcja 3).
        List<String> timeLines = new ArrayList<>();
        for (FormatStyle style : FormatStyle.values()) {
            for (Locale loc : List.of(PL, US)) {
                String text = DateTimeFormatter.ofLocalizedTime(style).withLocale(loc).format(ZDT);
                timeLines.add(style + " " + loc.toLanguageTag() + " = " + clean(text));
            }
        }
        showEach("czas", timeLines);
        // WYNIK: czas (liczba elementów: 8):
        // WYNIK:    • FULL pl-PL = 14:05:09 czas środkowoeuropejski standardowy
        // WYNIK:    • FULL en-US = 2:05:09 PM Central European Standard Time
        // WYNIK:    • LONG pl-PL = 14:05:09 CET
        // WYNIK:    • LONG en-US = 2:05:09 PM CET
        // WYNIK:    • MEDIUM pl-PL = 14:05:09
        // WYNIK:    • MEDIUM en-US = 2:05:09 PM
        // WYNIK:    • SHORT pl-PL = 14:05
        // WYNIK:    • SHORT en-US = 2:05 PM

        // Data i czas razem: jeden styl dla obu części albo dwa osobne (data, czas).
        for (Locale loc : List.of(PL, US)) {
            show("DateTime MEDIUM " + loc.toLanguageTag(),
                    clean(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(loc).format(ZDT)));
            show("DateTime (LONG, SHORT) " + loc.toLanguageTag(),
                    clean(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)
                            .withLocale(loc).format(ZDT)));
        }
        // WYNIK: DateTime MEDIUM pl-PL → 8 mar 2026, 14:05:09
        // WYNIK: DateTime (LONG, SHORT) pl-PL → 8 marca 2026, 14:05
        // WYNIK: DateTime MEDIUM en-US → Mar 8, 2026, 2:05:09 PM
        // WYNIK: DateTime (LONG, SHORT) en-US → March 8, 2026, 2:05 PM

        // DOBRA PRAKTYKA: dla użytkownika pokazuj daty formatem lokalnym (ofLocalized...) z JEGO Locale,
        // a nie własnym wzorcem „dd/MM/yyyy”. Dlaczego: Amerykanin czyta 03/08 jako 8 marca, Polak jako 3 sierpnia
        // — format lokalny pilnuje kolejności części i separatorów za Ciebie.
    }

    // =================================================================================================
    // 3. PUŁAPKI FORMATÓW LOKALNYCH
    // =================================================================================================

    /**
     * 3. Dwie pułapki: (a) formaty LONG/FULL czasu wymagają strefy, (b) tekst może zawierać twarde spacje,
     * a dokładne brzmienie zależy od wersji danych CLDR (inny JDK = inny tekst).
     */
    static void localizedPitfalls() {
        section("3. Pułapki formatów lokalnych");

        // PUŁAPKA: LONG i FULL pokazują nazwę strefy („CET”), a LocalTime/LocalDateTime strefy nie mają.
        // Dlaczego: formatter nie zgaduje strefy — wolałby rzucić wyjątek, niż pokazać nieprawdziwą godzinę.
        expectThrows("ofLocalizedTime(LONG) na LocalTime",
                () -> DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG).withLocale(PL).format(LocalTime.of(14, 5)));
        // WYNIK: ✔ ofLocalizedTime(LONG) na LocalTime → rzucono DateTimeException: Unable to extract ZoneId from temporal 14:05 with chronology ISO
        // Rozwiązania: dać ZonedDateTime, albo wskazać strefę w formatterze (withZone), albo użyć stylu SHORT/MEDIUM.
        DateTimeFormatter longTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG).withLocale(PL);
        show("withZone(WARSAW) na Instant", clean(longTime.withZone(WARSAW).format(ZDT.toInstant())));
        // WYNIK: withZone(WARSAW) na Instant → 14:05:09 CET
        show("SHORT na LocalTime", clean(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(PL).format(LocalTime.of(14, 5))));
        // WYNIK: SHORT na LocalTime → 14:05

        // PUŁAPKA: ofLocalizedDate nie przyjmie obiektu bez daty.
        expectThrows("ofLocalizedDate na LocalTime",
                () -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(PL).format(LocalTime.of(14, 5)));
        // WYNIK: ✔ ofLocalizedDate na LocalTime → rzucono UnsupportedTemporalTypeException: Unsupported field: DayOfMonth

        // PUŁAPKA: twarde spacje. W nowszych JDK (od 20) angielski czas „2:05 PM” ma przed PM znak U+202F,
        // a polskie liczby używają U+00A0 jako separatora tysięcy. Porównanie z tekstem pisanym ręcznie
        // ze zwykłą spacją daje false, choć w konsoli wygląda tak samo.
        String sample = "2:05 PM";
        String withNarrow = "2:05" + (char) 0x202F + "PM";
        show("equals bez czyszczenia", sample.equals(withNarrow));
        // WYNIK: equals bez czyszczenia → false
        show("equals po clean()", sample.equals(clean(withNarrow)));
        // WYNIK: equals po clean() → true

        // PUŁAPKA: nie parsuj tekstu pokazanego użytkownikowi ani nie zapisuj go w bazie. Układ lokalny
        // zmienia się między wersjami JDK (zmiana danych CLDR), więc stary zapis przestaje się parsować.
        // DOBRA PRAKTYKA: w testach i logach podawaj Locale jawnie, a dane trzymaj w ISO.
    }

    // =================================================================================================
    // 4. DateTimeFormatterBuilder
    // =================================================================================================

    /**
     * 4. DateTimeFormatterBuilder składa formatter z klocków: appendPattern (wzorzec), appendLiteral (stały znak),
     * appendValue (liczba z szerokością i znakiem), appendFraction (ułamek sekundy), optionalStart/End (część
     * opcjonalna), parseDefaulting (wartość domyślna przy odczycie). Na końcu toFormatter(Locale).
     */
    static void builderBasics() {
        section("4. DateTimeFormatterBuilder — opcje, wartości domyślne, szerokości");

        // Część opcjonalna: [ ... ] we wzorcu albo optionalStart()/optionalEnd() w Builderze.
        // parseDefaulting = „jeśli przy odczycie nie znaleziono tego pola, wstaw tę wartość”.
        DateTimeFormatter dateOrDateTime = new DateTimeFormatterBuilder()
                .appendPattern("uuuu-MM-dd")
                .optionalStart().appendPattern(" HH:mm").optionalEnd()   // optional = opcjonalny; start/end = początek/koniec
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)             // brak godziny → 0
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)          // brak minut → 0
                .toFormatter(PL);

        show("parse samej daty", LocalDateTime.parse("2026-03-08", dateOrDateTime));
        // WYNIK: parse samej daty → 2026-03-08T00:00
        show("parse daty z godziną", LocalDateTime.parse("2026-03-08 09:30", dateOrDateTime));
        // WYNIK: parse daty z godziną → 2026-03-08T09:30
        // Przy FORMATOWANIU część opcjonalna pojawia się tylko, gdy obiekt ma wszystkie jej pola.
        show("format LocalDateTime", dateOrDateTime.format(DT));
        // WYNIK: format LocalDateTime → 2026-03-08 14:05
        show("format LocalDate", dateOrDateTime.format(DT.toLocalDate()));
        // WYNIK: format LocalDate → 2026-03-08
        // To samo krócej: nawiasy kwadratowe we wzorcu.
        DateTimeFormatter shorter = DateTimeFormatter.ofPattern("uuuu-MM-dd[ HH:mm]", PL);
        show("wzorzec z [ ]", shorter.format(DT));
        // WYNIK: wzorzec z [ ] → 2026-03-08 14:05

        // PUŁAPKA: bez parseDefaulting parsowanie samej daty do LocalDateTime kończy się wyjątkiem.
        // Dlaczego: LocalDateTime potrzebuje godziny, a tekst jej nie zawiera — nikt nie zgaduje „północy” za Ciebie.
        DateTimeFormatter noDefaults = DateTimeFormatter.ofPattern("uuuu-MM-dd[ HH:mm]", PL);
        tryIt("LocalDateTime.parse bez defaultów", () -> LocalDateTime.parse("2026-03-08", noDefaults));
        // WYNIK: LocalDateTime.parse bez defaultów → wyjątek DateTimeParseException
        // Rozwiązanie bez Buildera: LocalDate.parse(...).atStartOfDay() (atStartOfDay = o początku dnia).

        // appendValue(pole, minSzerokość, maxSzerokość, SignStyle): liczba z dopełnieniem zerami i regułą znaku.
        // EXCEEDS_PAD = znak + tylko wtedy, gdy liczba jest dłuższa niż minimalna szerokość (rok 12345 → +12345).
        DateTimeFormatter yearExceedsPad = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendLiteral('/')                                      // literal = stały znak
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)               // stała szerokość 2: 03
                .appendLiteral('/')
                .appendValue(ChronoField.DAY_OF_MONTH, 2)
                .toFormatter(Locale.ROOT);
        DateTimeFormatter yearNormal = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4, 10, SignStyle.NORMAL)  // NORMAL = znak tylko dla liczb ujemnych
                .toFormatter(Locale.ROOT);
        show("EXCEEDS_PAD, rok 2026", yearExceedsPad.format(LocalDate.of(2026, 3, 8)));
        // WYNIK: EXCEEDS_PAD, rok 2026 → 2026/03/08
        show("EXCEEDS_PAD, rok 12345", yearExceedsPad.format(LocalDate.of(12345, 1, 2)));
        // WYNIK: EXCEEDS_PAD, rok 12345 → +12345/01/02
        show("NORMAL, rok 12345", yearNormal.format(LocalDate.of(12345, 1, 2)));
        // WYNIK: NORMAL, rok 12345 → 12345

        // appendFraction(pole, minCyfr, maxCyfr, kropka?): ułamek sekundy. minCyfr = 0 → zera na końcu są obcinane.
        DateTimeFormatter withFraction = new DateTimeFormatterBuilder()
                .appendPattern("HH:mm:ss")
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 3, true)  // fraction = ułamek
                .toFormatter(Locale.ROOT);
        show("ułamek .120", withFraction.format(LocalTime.of(14, 5, 9, 120_000_000)));
        // WYNIK: ułamek .120 → 14:05:09.12
        show("ułamek .000", withFraction.format(LocalTime.of(14, 5, 9)));
        // WYNIK: ułamek .000 → 14:05:09

        // DOBRA PRAKTYKA: formater złożony Builderem trzymaj w static final i nadaj mu nazwę mówiącą o roli
        // (DATE_OR_DATETIME). Dlaczego: budowanie jest droższe od użycia, a nazwa dokumentuje przyjęty format.
    }

    // =================================================================================================
    // 5. NAZWY MIESIĘCY
    // =================================================================================================

    /** Mianownik („styczeń”) — do własnej mapy miesięcy w nazwie kolumny, nagłówku tabeli itp. */
    private static final String[] MONTHS_NOMINATIVE = {"styczeń", "luty", "marzec", "kwiecień", "maj", "czerwiec",
            "lipiec", "sierpień", "wrzesień", "październik", "listopad", "grudzień"};
    /** Dopełniacz („stycznia”) — pasuje po numerze dnia: „8 marca”. */
    private static final String[] MONTHS_GENITIVE = {"stycznia", "lutego", "marca", "kwietnia", "maja", "czerwca",
            "lipca", "sierpnia", "września", "października", "listopada", "grudnia"};

    static Map<Long, String> monthMap(String[] names) {
        Map<Long, String> map = new HashMap<>();   // klucz = numer miesiąca 1–12 (jako Long, tego wymaga appendText)
        for (int i = 0; i < names.length; i++) {
            map.put((long) (i + 1), names[i]);
        }
        return map;
    }

    /**
     * 5. W polszczyźnie miesiąc ma dwie formy: MIANOWNIK (samodzielny: „marzec”) i DOPEŁNIACZ (po numerze dnia:
     * „8 marca”). Wzorzec LLLL daje mianownik (L = standalone, „samodzielna” forma), a MMMM — formę
     * odmienioną, czyli dopełniacz (M = format, „w zdaniu”). W angielskim obie formy są takie same, dlatego
     * pułapka wychodzi dopiero po polsku.
     */
    static void monthNames() {
        section("5. Nazwy miesięcy — LLLL czy MMMM, własna mapa");

        show("d MMMM uuuu", DateTimeFormatter.ofPattern("d MMMM uuuu", PL).format(DT));
        // WYNIK: d MMMM uuuu → 8 marca 2026
        show("d LLLL uuuu (źle!)", DateTimeFormatter.ofPattern("d LLLL uuuu", PL).format(DT));
        // WYNIK: d LLLL uuuu (źle!) → 8 marzec 2026
        show("LLLL uuuu (nagłówek)", DateTimeFormatter.ofPattern("LLLL uuuu", PL).format(DT));
        // WYNIK: LLLL uuuu (nagłówek) → marzec 2026
        show("MMMM uuuu (źle!)", DateTimeFormatter.ofPattern("MMMM uuuu", PL).format(DT));
        // WYNIK: MMMM uuuu (źle!) → marca 2026
        show("MMM i LLL (skrót)", DateTimeFormatter.ofPattern("d MMM, LLL", PL).format(DT));
        // WYNIK: MMM i LLL (skrót) → 8 mar, mar
        // Zasada: M... po numerze dnia („8 marca”), L... gdy miesiąc stoi sam („marzec 2026”).

        // To samo w API enumów: TextStyle.FULL = forma odmieniona, FULL_STANDALONE = samodzielna.
        // Month.getDisplayName(style, locale) = podaj nazwę wyświetlaną.
        show("Month FULL", Month.MARCH.getDisplayName(TextStyle.FULL, PL));
        // WYNIK: Month FULL → marca
        show("Month FULL_STANDALONE", Month.MARCH.getDisplayName(TextStyle.FULL_STANDALONE, PL));
        // WYNIK: Month FULL_STANDALONE → marzec
        show("angielski: FULL = STANDALONE", Month.MARCH.getDisplayName(TextStyle.FULL, US)
                + " = " + Month.MARCH.getDisplayName(TextStyle.FULL_STANDALONE, US));
        // WYNIK: angielski: FULL = STANDALONE → March = March

        // PUŁAPKA: TextStyle.FULL w Month/DayOfWeek.getDisplayName po polsku to forma ODMIENIONA („marca”),
        // a nie mianownik. Lista miesięcy do rozwijanego menu zbudowana przez FULL będzie więc ZŁA
        // („stycznia, lutego...”). Do list i nagłówków używaj FULL_STANDALONE.

        // Własne nazwy: appendText(pole, Map<Long, String>) — formatowanie i parsowanie używa Twojej mapy.
        // Dlaczego warto: pełna kontrola, brak zależności od danych CLDR konkretnego JDK.
        DateTimeFormatter genitiveDate = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.DAY_OF_MONTH)
                .appendLiteral(' ')
                .appendText(ChronoField.MONTH_OF_YEAR, monthMap(MONTHS_GENITIVE))   // text = tekst
                .appendLiteral(' ')
                .appendValue(ChronoField.YEAR, 4)
                .toFormatter(PL);
        DateTimeFormatter nominativeMonth = new DateTimeFormatterBuilder()
                .appendText(ChronoField.MONTH_OF_YEAR, monthMap(MONTHS_NOMINATIVE))
                .appendLiteral(' ')
                .appendValue(ChronoField.YEAR, 4)
                .toFormatter(PL);
        show("własna mapa (dopełniacz)", genitiveDate.format(DT));
        // WYNIK: własna mapa (dopełniacz) → 8 marca 2026
        show("własna mapa (mianownik)", nominativeMonth.format(DT));
        // WYNIK: własna mapa (mianownik) → marzec 2026
        show("odczyt przez własną mapę", LocalDate.parse("31 grudnia 2026", genitiveDate));
        // WYNIK: odczyt przez własną mapę → 2026-12-31
        // Odczyt nie rozumie nazw spoza mapy: mianownik w formatterze dopełniaczowym to błąd.
        tryIt("odczyt „31 grudzień 2026”", () -> LocalDate.parse("31 grudzień 2026", genitiveDate));
        // WYNIK: odczyt „31 grudzień 2026” → wyjątek DateTimeParseException
    }

    // =================================================================================================
    // 6. WIELKOŚĆ LITER I WIELE FORMATÓW WEJŚCIOWYCH
    // =================================================================================================

    private static DateTimeFormatter strict(String pattern) {
        return DateTimeFormatter.ofPattern(pattern, PL).withResolverStyle(ResolverStyle.STRICT);
    }

    /** Czytnik niewrażliwy na wielkość liter (np. „8 MARCA 2026”, „8 Marca 2026”). */
    private static final DateTimeFormatter POLISH_LONG_INSENSITIVE = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()                    // case insensitive = bez względu na wielkość liter
            .appendPattern("d MMMM uuuu")
            .toFormatter(PL)
            .withResolverStyle(ResolverStyle.STRICT);

    /** Lista formatów wejściowych — sprawdzane po kolei, wygrywa pierwszy, który pasuje. */
    private static final List<DateTimeFormatter> INPUT_FORMATS = List.of(   // List.of = Java 9+
            DateTimeFormatter.ISO_LOCAL_DATE,           // 2026-03-08 (ISO jest STRICT od urodzenia)
            strict("dd.MM.uuuu"),                       // 08.03.2026
            strict("d/M/uuuu"),                         // 8/3/2026
            POLISH_LONG_INSENSITIVE);                   // 8 marca 2026

    /**
     * Zwraca datę z pierwszego formatu, który pasuje; Optional.empty() = nic nie pasuje.
     * Optional = opcjonalny (może nie mieć wartości) — zob. t14_optional/Optional01Basics.
     */
    static Optional<LocalDate> parseAny(String text) {
        for (DateTimeFormatter format : INPUT_FORMATS) {
            try {
                return Optional.of(LocalDate.parse(text, format));
            } catch (DateTimeParseException e) {
                // ten format nie pasuje — próbujemy następny (wyjątek jako sterowanie ma koszt, ale tu to rzadki przypadek)
            }
        }
        return Optional.empty();
    }

    /**
     * 6. Dwie rzeczy z życia: ludzie piszą „MARCA” wielkimi literami, a pliki od różnych dostawców mają różne
     * daty. Odpowiedzi: parseCaseInsensitive w Builderze oraz lista formatów próbowanych po kolei
     * (albo jeden formatter z częściami opcjonalnymi).
     */
    static void caseAndManyFormats() {
        section("6. Wielkość liter i wiele formatów wejściowych");

        DateTimeFormatter plain = DateTimeFormatter.ofPattern("d MMMM uuuu", PL);
        expectThrows("odczyt „8 MARCA 2026” zwykłym", () -> LocalDate.parse("8 MARCA 2026", plain));
        // WYNIK: ✔ odczyt „8 MARCA 2026” zwykłym → rzucono DateTimeParseException: Text '8 MARCA 2026' could not be parsed at index 2
        show("z parseCaseInsensitive", LocalDate.parse("8 MARCA 2026", POLISH_LONG_INSENSITIVE));
        // WYNIK: z parseCaseInsensitive → 2026-03-08
        show("z parseCaseInsensitive, 2", LocalDate.parse("8 Marca 2026", POLISH_LONG_INSENSITIVE));
        // WYNIK: z parseCaseInsensitive, 2 → 2026-03-08

        // Metoda parseAny próbuje czterech formatów po kolei.
        for (String text : List.of("2026-03-08", "08.03.2026", "8/3/2026", "8 MARCA 2026", "31.02.2026", "jutro")) {
            show("parseAny(" + text + ")", parseAny(text));
        }
        // WYNIK: parseAny(2026-03-08) → Optional[2026-03-08]
        // WYNIK: parseAny(08.03.2026) → Optional[2026-03-08]
        // WYNIK: parseAny(8/3/2026) → Optional[2026-03-08]
        // WYNIK: parseAny(8 MARCA 2026) → Optional[2026-03-08]
        // WYNIK: parseAny(31.02.2026) → Optional.empty
        // WYNIK: parseAny(jutro) → Optional.empty

        // Alternatywa: JEDEN formatter z częściami opcjonalnymi. Każda część [..] jest próbowana po kolei;
        // nieudana jest wycofywana. Zaleta: brak wyjątków jako sterowania. Wada: trudniej o dobry komunikat błędu.
        DateTimeFormatter optionalParts = DateTimeFormatter.ofPattern("[uuuu-MM-dd][dd.MM.uuuu]", PL)
                .withResolverStyle(ResolverStyle.STRICT);
        show("części opcjonalne (ISO)", LocalDate.parse("2026-03-08", optionalParts));
        // WYNIK: części opcjonalne (ISO) → 2026-03-08
        show("części opcjonalne (polski)", LocalDate.parse("08.03.2026", optionalParts));
        // WYNIK: części opcjonalne (polski) → 2026-03-08

        // PUŁAPKA: kolejność ma znaczenie przy niejednoznacznych zapisach. „3/8/2026” to 3 sierpnia (d/M/uuuu)
        // albo 8 marca (M/d/uuuu)? Żaden parser tego nie rozstrzygnie — musisz znać źródło danych.
        // PUŁAPKA: formaty z listy muszą być STRICT. Domyślny SMART przyjąłby „31.02.2026” jako 28.02.2026
        // i błąd w danych przeszedłby niezauważony (zob. sekcja 7).
        // DOBRA PRAKTYKA: lista dozwolonych formatów ma być krótka, jawna i pokryta testami; „zgadywanie”
        // formatu na podstawie treści to źródło cichych pomyłek.
    }

    // =================================================================================================
    // 7. RESOLVERSTYLE
    // =================================================================================================

    /** Odczyt jednym formatterem; błąd zamienia na krótki napis. */
    private static String attempt(DateTimeFormatter formatter, String text) {
        try {
            return LocalDate.parse(text, formatter).toString();
        } catch (DateTimeParseException e) {
            return "błąd";
        }
    }

    /**
     * 7. ResolverStyle decyduje, co zrobić z odczytanymi polami. Ten sam tekst, ten sam wzorzec — trzy różne
     * wyniki. Uwaga: ofPattern jest domyślnie SMART, a gotowe formatery ISO_* są STRICT.
     */
    static void resolverStyles() {
        section("7. ResolverStyle — STRICT, SMART, LENIENT");

        DateTimeFormatter smart = DateTimeFormatter.ofPattern("uuuu-MM-dd", PL);   // domyślnie SMART
        DateTimeFormatter strict = smart.withResolverStyle(ResolverStyle.STRICT);
        DateTimeFormatter lenient = smart.withResolverStyle(ResolverStyle.LENIENT);

        for (String text : List.of("2026-02-28", "2026-02-30", "2026-02-32", "2026-13-01")) {
            show(text, "SMART: " + attempt(smart, text) + " | STRICT: " + attempt(strict, text)
                    + " | LENIENT: " + attempt(lenient, text));
        }
        // WYNIK: 2026-02-28 → SMART: 2026-02-28 | STRICT: 2026-02-28 | LENIENT: 2026-02-28
        // WYNIK: 2026-02-30 → SMART: 2026-02-28 | STRICT: błąd | LENIENT: 2026-03-02
        // WYNIK: 2026-02-32 → SMART: błąd | STRICT: błąd | LENIENT: 2026-03-04
        // WYNIK: 2026-13-01 → SMART: błąd | STRICT: błąd | LENIENT: 2027-01-01
        // Wnioski:
        //  - 30 lutego: SMART przycina do końca miesiąca (28.02), STRICT odrzuca, LENIENT liczy dalej (2 marca).
        //  - 32 lutego: poza zakresem dni 1–31, więc SMART też odrzuca; LENIENT przelewa nadmiar na marzec.
        //  - miesiąc 13: SMART i STRICT odrzucają, LENIENT przechodzi do stycznia następnego roku.

        // PUŁAPKA: LocalDate.parse("2026-02-30") (bez formatera) JEST ścisły, bo używa ISO_LOCAL_DATE, a
        // DateTimeFormatter.ofPattern("uuuu-MM-dd") jest SMART — i „naprawia” datę po cichu. Dwa sposoby
        // zapisania tego samego wzorca zachowują się inaczej.
        expectThrows("LocalDate.parse(\"2026-02-30\")", () -> LocalDate.parse("2026-02-30"));
        // WYNIK: ✔ LocalDate.parse("2026-02-30") → rzucono DateTimeParseException: Text '2026-02-30' could not be parsed: Invalid date 'FEBRUARY 30'
        show("ofPattern(uuuu-MM-dd) SMART", LocalDate.parse("2026-02-30", smart));
        // WYNIK: ofPattern(uuuu-MM-dd) SMART → 2026-02-28

        // DOBRA PRAKTYKA: dane od użytkownika i z plików czytaj w trybie STRICT. LENIENT zostaw do świadomych
        // obliczeń (np. „dzień 400 roku”). Dlaczego: błędna data ma być zauważona, a nie cicho przesunięta.

        // uuuu a yyyy. Litera u = rok liczony od ery matematycznej (…, -1, 0, 1, 2026), litera y = „rok ery”
        // (rok n.e. albo p.n.e.: 1 p.n.e. to yyyy = 1 z erą BCE). Aby z „roku ery” złożyć datę, trzeba znać erę
        // (pole G). SMART po cichu zakłada naszą erę, STRICT nie zakłada niczego.
        DateTimeFormatter yyyyStrict = DateTimeFormatter.ofPattern("yyyy-MM-dd", PL)
                .withResolverStyle(ResolverStyle.STRICT);
        tryIt("yyyy + STRICT: odczyt", () -> LocalDate.parse("2026-03-08", yyyyStrict));
        // WYNIK: yyyy + STRICT: odczyt → wyjątek DateTimeParseException
        show("yyyy + STRICT: formatowanie", yyyyStrict.format(DT));
        // WYNIK: yyyy + STRICT: formatowanie → 2026-03-08
        // Rozwiązanie 1: uuuu. Rozwiązanie 2: dopisać erę (G) — wtedy tekst musi ją zawierać (po angielsku „AD”).
        DateTimeFormatter yyyyWithEra = DateTimeFormatter.ofPattern("yyyy-MM-dd G", US)
                .withResolverStyle(ResolverStyle.STRICT);
        show("yyyy G + STRICT", LocalDate.parse("2026-03-08 AD", yyyyWithEra));
        // WYNIK: yyyy G + STRICT → 2026-03-08
        // Różnica widoczna już przy formatowaniu roku 0 (czyli 1 p.n.e.):
        show("rok 0: uuuu / yyyy", DateTimeFormatter.ofPattern("uuuu", PL).format(LocalDate.of(0, 1, 1))
                + " / " + DateTimeFormatter.ofPattern("yyyy", PL).format(LocalDate.of(0, 1, 1)));
        // WYNIK: rok 0: uuuu / yyyy → 0000 / 0001
        // Inna znana pułapka (DateTime03Formatting): YYYY to rok „tygodniowy”, a nie kalendarzowy — wokół
        // 31 grudnia potrafi pokazać następny rok. Do zwykłych dat zawsze uuuu (lub yyyy z erą).
    }

    // =================================================================================================
    // 8. PARSOWANIE DO TemporalAccessor I ZAPYTANIA
    // =================================================================================================

    /**
     * 8. formatter.parse(tekst) zwraca TemporalAccessor („dostęp do pól czasu”) — to surowy wynik, z którego
     * można zrobić różne typy: LocalDate.from(ta), LocalTime.from(ta), ZonedDateTime.from(ta)...
     * Albo użyć zapytania (TemporalQuery): LocalDate::from jest takim zapytaniem (wzorzec Strategia).
     * Dzięki temu NIE musimy z góry wiedzieć, ile informacji jest w tekście.
     */
    static void parsedAndQueries() {
        section("8. Parsowanie do TemporalAccessor i zapytania (query)");

        TemporalAccessor full = DateTimeFormatter.ISO_DATE_TIME.parse("2026-03-08T14:05:09+01:00[Europe/Warsaw]");
        show("isSupported(HOUR_OF_DAY)", full.isSupported(ChronoField.HOUR_OF_DAY));   // isSupported = czy obsługuje
        // WYNIK: isSupported(HOUR_OF_DAY) → true
        show("get(HOUR_OF_DAY)", full.get(ChronoField.HOUR_OF_DAY));
        // WYNIK: get(HOUR_OF_DAY) → 14
        show("LocalDate.from", LocalDate.from(full));
        // WYNIK: LocalDate.from → 2026-03-08
        show("LocalTime.from", LocalTime.from(full));
        // WYNIK: LocalTime.from → 14:05:09
        show("ZonedDateTime.from", ZonedDateTime.from(full));
        // WYNIK: ZonedDateTime.from → 2026-03-08T14:05:09+01:00[Europe/Warsaw]
        show("Instant.from", Instant.from(full));
        // WYNIK: Instant.from → 2026-03-08T13:05:09Z
        show("query(zone)", full.query(TemporalQueries.zone()));       // zone = strefa
        // WYNIK: query(zone) → Europe/Warsaw
        show("query(offset)", full.query(TemporalQueries.offset()));   // offset = przesunięcie względem UTC
        // WYNIK: query(offset) → +01:00

        // Ta sama składnia z jednym wywołaniem: parse(tekst, zapytanie).
        show("parse(tekst, LocalDate::from)",
                DateTimeFormatter.ISO_DATE_TIME.parse("2026-03-08T14:05:09", LocalDate::from));
        // WYNIK: parse(tekst, LocalDate::from) → 2026-03-08

        // Tekst tylko z datą: godzina niedostępna — próba złożenia LocalTime kończy się wyjątkiem.
        TemporalAccessor onlyDate = DateTimeFormatter.ISO_LOCAL_DATE.parse("2026-03-08");
        show("data: isSupported(HOUR_OF_DAY)", onlyDate.isSupported(ChronoField.HOUR_OF_DAY));
        // WYNIK: data: isSupported(HOUR_OF_DAY) → false
        tryIt("data: LocalTime.from", () -> LocalTime.from(onlyDate));
        // WYNIK: data: LocalTime.from → wyjątek DateTimeException
        tryIt("data: ZonedDateTime.from", () -> ZonedDateTime.from(onlyDate));
        // WYNIK: data: ZonedDateTime.from → wyjątek DateTimeException
        // DOBRA PRAKTYKA: najpierw pytaj isSupported / query, potem składaj typ — zamiast łapać wyjątki.

        // Własne zapytanie to zwykła lambda: TemporalQuery<T> ma jedną metodę queryFrom(TemporalAccessor).
        TemporalQuery<Boolean> hasTime = t -> t.isSupported(ChronoField.HOUR_OF_DAY);
        DateTimeFormatter dateOrDateTime = DateTimeFormatter.ofPattern("uuuu-MM-dd[ HH:mm]", PL)
                .withResolverStyle(ResolverStyle.STRICT);
        for (String text : List.of("2026-03-08", "2026-03-08 09:30")) {
            TemporalAccessor parsed = dateOrDateTime.parse(text);
            Object result = parsed.query(hasTime) ? LocalDateTime.from(parsed) : LocalDate.from(parsed);
            show("data czy data+godzina? " + text, result.getClass().getSimpleName() + " " + result);
        }
        // WYNIK: data czy data+godzina? 2026-03-08 → LocalDate 2026-03-08
        // WYNIK: data czy data+godzina? 2026-03-08 09:30 → LocalDateTime 2026-03-08T09:30
        // Zastosowanie: jedno pole „termin” w pliku może mieć datę albo datę z godziną — typ wybieramy po treści.
    }

    // =================================================================================================
    // 9. Duration I Period RĘCZNIE
    // =================================================================================================

    /**
     * Polska liczba mnoga: 1 rok, 2–4 lata, 5–21 lat, 22–24 lata, 25 lat... Reguła: forma „few” dla końcówek
     * 2–4 z wyjątkiem 12–14.
     */
    static String plural(long n, String one, String few, String many) {   // plural = liczba mnoga
        long mod10 = n % 10;
        long mod100 = n % 100;
        if (n == 1) {
            return one;
        }
        if (mod10 >= 2 && mod10 <= 4 && !(mod100 >= 12 && mod100 <= 14)) {
            return few;
        }
        return many;
    }

    /** Duration jako „26 godz. 5 min 9 s” (godziny liczone łącznie, bez dni). Zera pomijamy. */
    static String formatDuration(Duration d) {
        if (d.isZero()) {
            return "0 s";
        }
        // Znak obsługujemy osobno: dla ujemnych Duration części (godziny, minuty) są ujemne — wyszłoby „-1 godz. -30 min”.
        String sign = d.isNegative() ? "-" : "";
        Duration abs = d.abs();
        List<String> parts = new ArrayList<>();
        if (abs.toHours() > 0) {
            parts.add(abs.toHours() + " godz.");
        }
        if (abs.toMinutesPart() > 0) {   // toMinutesPart = część minutowa 0–59 (Java 9+)
            parts.add(abs.toMinutesPart() + " min");
        }
        if (abs.toSecondsPart() > 0) {   // toSecondsPart = część sekundowa 0–59 (Java 9+)
            parts.add(abs.toSecondsPart() + " s");
        }
        return sign + String.join(" ", parts);
    }

    /** Period jako „2 lata 3 miesiące 22 dni”; zera pomijamy. */
    static String formatPeriod(Period p) {
        if (p.isZero()) {
            return "0 dni";
        }
        List<String> parts = new ArrayList<>();
        if (p.getYears() != 0) {
            parts.add(p.getYears() + " " + plural(p.getYears(), "rok", "lata", "lat"));
        }
        if (p.getMonths() != 0) {
            parts.add(p.getMonths() + " " + plural(p.getMonths(), "miesiąc", "miesiące", "miesięcy"));
        }
        if (p.getDays() != 0) {
            parts.add(p.getDays() + " " + plural(p.getDays(), "dzień", "dni", "dni"));
        }
        return String.join(" ", parts);
    }

    /**
     * 9. Duration i Period nie mają formattera — mają tylko toString w stylu ISO („PT26H5M9S”, „P2Y1M21D”).
     * Dla człowieka formatujemy je sami z części. Pamiętaj o dwóch pułapkach: godziny to ŁĄCZNA liczba (nie
     * 0–23), a minuty i sekundy mają osobne metody „…Part” (część).
     */
    static void durationAndPeriod() {
        section("9. Duration i Period — formatowanie ręczne");

        Duration d = Duration.ofHours(26).plusMinutes(5).plusSeconds(9);
        show("Duration.toString()", d);
        // WYNIK: Duration.toString() → PT26H5M9S
        show("formatDuration", formatDuration(d));
        // WYNIK: formatDuration → 26 godz. 5 min 9 s
        show("zegar HH:mm:ss", String.format(Locale.ROOT, "%02d:%02d:%02d",
                d.toHours(), d.toMinutesPart(), d.toSecondsPart()));   // String.format = formatuj tekst
        // WYNIK: zegar HH:mm:ss → 26:05:09

        // toMinutes() = ŁĄCZNIE minut, toMinutesPart() = tylko reszta po wyjęciu pełnych godzin.
        Duration m125 = Duration.ofMinutes(125);
        show("125 min: toMinutes / toMinutesPart", m125.toMinutes() + " / " + m125.toMinutesPart());
        // WYNIK: 125 min: toMinutes / toMinutesPart → 125 / 5
        show("125 min: toHours", m125.toHours());
        // WYNIK: 125 min: toHours → 2
        show("formatDuration(125 min)", formatDuration(m125));
        // WYNIK: formatDuration(125 min) → 2 godz. 5 min

        // PUŁAPKA: ujemny Duration. toHours() i toMinutesPart() oba są ujemne („-1” i „-30”). Naiwne sklejanie
        // da „-1 godz. -30 min”. Dlatego formatDuration używa abs() i dopisuje znak raz, na początku.
        Duration minus90 = Duration.ofMinutes(-90);
        show("-90 min: toHours / toMinutesPart", minus90.toHours() + " / " + minus90.toMinutesPart());
        // WYNIK: -90 min: toHours / toMinutesPart → -1 / -30
        show("formatDuration(-90 min)", formatDuration(minus90));
        // WYNIK: formatDuration(-90 min) → -1 godz. 30 min

        // Period: lata + miesiące + dni, bez godzin. Period.between liczy „kalendarzowo”.
        Period between = Period.between(LocalDate.of(2024, 1, 15), LocalDate.of(2026, 3, 8));
        show("Period.toString()", between);
        // WYNIK: Period.toString() → P2Y1M21D
        show("formatPeriod", formatPeriod(between));
        // WYNIK: formatPeriod → 2 lata 1 miesiąc 21 dni
        for (Period p : List.of(Period.of(1, 0, 0), Period.of(2, 3, 22), Period.of(5, 12, 21), Period.ZERO)) {
            show("formatPeriod(" + p + ")", formatPeriod(p));
        }
        // WYNIK: formatPeriod(P1Y) → 1 rok
        // WYNIK: formatPeriod(P2Y3M22D) → 2 lata 3 miesiące 22 dni
        // WYNIK: formatPeriod(P5Y12M21D) → 5 lat 12 miesięcy 21 dni
        // WYNIK: formatPeriod(P0D) → 0 dni
        // PUŁAPKA: Period.of(5, 12, 21) NIE normalizuje się do „6 lat” — to, co wpiszesz, to dostajesz.
        // Wywołaj p.normalized() (normalized = znormalizowany), by 12 miesięcy zamienić na rok.
        show("Period.of(5,12,21).normalized()", formatPeriod(Period.of(5, 12, 21).normalized()));
        // WYNIK: Period.of(5,12,21).normalized() → 6 lat 21 dni
        // DOBRA PRAKTYKA: dla biblioteki używanej po polsku zrób JEDNĄ metodę plural() — odmiana „1 dzień,
        // 2 dni, 5 dni” to klasyczne źródło błędów; po angielsku wystarczy dopisać „s”.
    }

    // =================================================================================================
    // 10. BEZPIECZEŃSTWO WĄTKOWE I STARY KOD
    // =================================================================================================

    /** Współdzielony formatter — bezpieczny, bo niezmienny (static final to jego naturalne miejsce). */
    private static final DateTimeFormatter SHARED = DateTimeFormatter.ofPattern("dd.MM.uuuu", PL);

    /**
     * 10. DateTimeFormatter jest niezmienny, więc wiele wątków może go używać naraz. Stary SimpleDateFormat
     * trzyma w środku zmienny kalendarz, więc współdzielony między wątkami psuje wyniki (czasem rzuca wyjątek,
     * czasem po cichu daje złą datę). Na końcu: most do klas Date/Calendar.
     */
    static void threadSafetyAndLegacy() {
        section("10. Bezpieczeństwo wątkowe i współpraca ze starym kodem");

        List<LocalDate> dates = IntStream.range(0, 2000)
                .mapToObj(i -> LocalDate.of(2026, 1, 1).plusDays(i))
                .toList();                                     // toList = do listy niezmiennej (Java 16+)
        List<String> sequential = dates.stream().map(SHARED::format).toList();
        List<String> parallel = dates.parallelStream().map(SHARED::format).toList();   // parallel = równolegle
        show("równolegle = po kolei", sequential.equals(parallel));
        // WYNIK: równolegle = po kolei → true
        show("pierwszy i ostatni", sequential.get(0) + " ... " + sequential.get(sequential.size() - 1));
        // WYNIK: pierwszy i ostatni → 01.01.2026 ... 23.06.2031
        // PUŁAPKA: ten sam test z jednym współdzielonym SimpleDateFormat dawałby losowe wyniki, więc go NIE
        // uruchamiamy. Dlaczego: format() zapisuje pola w jednym, wspólnym obiekcie Calendar — dwa wątki
        // nadpisują sobie nawzajem rok i dzień. Stare rozwiązania (ThreadLocal, synchronized) są zbędne.

        // Most: Instant ↔ java.util.Date (Date to w gruncie rzeczy „milisekundy od 1970-01-01T00:00Z”).
        Instant instant = ZDT.toInstant();
        Date date = Date.from(instant);                        // Date.from = utwórz Date z Instant
        show("date.getTime() [ms od epoki]", date.getTime());
        // WYNIK: date.getTime() [ms od epoki] → 1772975109000
        show("date.toInstant()", date.toInstant());
        // WYNIK: date.toInstant() → 2026-03-08T13:05:09Z
        // Date nie ma strefy, więc przy zamianie na LocalDate/LocalDateTime strefę podajemy SAMI.
        show("Date → LocalDate", date.toInstant().atZone(WARSAW).toLocalDate());
        // WYNIK: Date → LocalDate → 2026-03-08
        // PUŁAPKA: date.toString() i new SimpleDateFormat(...) bez setTimeZone używają domyślnej strefy
        // komputera — ten sam kod daje inną godzinę na serwerze w UTC niż u Ciebie. Dlatego strefa jawnie:
        SimpleDateFormat old = new SimpleDateFormat("yyyy-MM-dd HH:mm", PL);
        old.setTimeZone(TimeZone.getTimeZone(WARSAW));         // TimeZone.getTimeZone(ZoneId) — most do starej strefy
        show("SimpleDateFormat z setTimeZone", old.format(date));
        // WYNIK: SimpleDateFormat z setTimeZone → 2026-03-08 14:05
        // Calendar: GregorianCalendar.from(ZonedDateTime) i calendar.toZonedDateTime().
        GregorianCalendar calendar = GregorianCalendar.from(ZDT);
        show("Calendar → ZonedDateTime", calendar.toZonedDateTime());
        // WYNIK: Calendar → ZonedDateTime → 2026-03-08T14:05:09+01:00[Europe/Warsaw]
        // Bazy danych (JDBC, zob. t29_jdbc_databases): java.sql.Date.valueOf(LocalDate) i
        // java.sql.Timestamp.from(Instant) oraz metody toLocalDate()/toInstant() w drugą stronę.
        // DOBRA PRAKTYKA: stare klasy tylko na granicy systemu (biblioteka, sterownik); w środku zawsze java.time.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • ISO_* = stałe zapisy do wymiany danych (ISO_LOCAL_DATE, ISO_DATE_TIME, ISO_INSTANT = UTC z literą Z,
     *     RFC_1123 = angielskie nazwy, BASIC_ISO_DATE = 20260308). ISO formatery są STRICT.
     *   • ofLocalizedDate/Time/DateTime(FormatStyle) + withLocale(...) = zapis dla człowieka; zawsze podaj Locale.
     *     LONG/FULL dla czasu wymagają strefy (ZonedDateTime albo withZone).
     *   • Wyniki lokalne mogą mieć U+00A0 / U+202F — przed porównaniem lub wydrukiem zamień na zwykłą spację.
     *     Układ zależy od wersji JDK (CLDR): nie parsuj i nie zapisuj tego, co widzi użytkownik.
     *   • Polski miesiąc: MMMM = „marca” (po numerze dnia), LLLL = „marzec” (samodzielnie).
     *     TextStyle.FULL = odmieniona, FULL_STANDALONE = mianownik.
     *   • DateTimeFormatterBuilder: appendPattern, appendLiteral, appendValue(pole, min, max, SignStyle),
     *     appendFraction, appendText(pole, mapa), optionalStart/End, parseDefaulting, parseCaseInsensitive,
     *     toFormatter(Locale). Wynik trzymaj w static final.
     *   • Wiele formatów wejściowych: lista formatterów próbowanych po kolei (wszystkie STRICT) albo jeden
     *     formatter z częściami [opcjonalnymi]. Niejednoznaczne zapisy (3/8) wymagają znajomości źródła.
     *   • ResolverStyle: STRICT (30 lutego = błąd), SMART (domyślny w ofPattern: 30 lutego = 28 lutego,
     *     32 lutego = błąd), LENIENT (przelewa nadmiar). Dane z zewnątrz czytaj STRICT.
     *   • uuuu = rok (…, 0, 1, 2026); yyyy = rok ery, z STRICT wymaga pola G. W zwykłym kodzie: uuuu.
     *   • parse(tekst) → TemporalAccessor; potem LocalDate.from(ta), query(TemporalQueries.zone()),
     *     isSupported(pole); parse(tekst, LocalDate::from) w jednym kroku.
     *   • Duration i Period nie mają formattera: Duration → toHours(), toMinutesPart(), toSecondsPart() (Java 9+),
     *     uważaj na znak; Period → getYears/getMonths/getDays, normalized(); polska liczba mnoga własną metodą.
     *   • DateTimeFormatter jest niezmienny = bezpieczny wątkowo; SimpleDateFormat nie jest.
     *   • Most do starego kodu: Date.from(instant), date.toInstant() (strefę dodajesz sam przez atZone).
     *
     * PYTANIA KONTROLNE:
     *   1. Kiedy użyjesz ISO_LOCAL_DATE, a kiedy ofLocalizedDate(FormatStyle.MEDIUM)? Dlaczego nie zamienić ich miejscami?
     *   2. Co wypisze:  System.out.println(DateTimeFormatter.ofPattern("d LLLL uuuu", PL).format(LocalDate.of(2026, 3, 8)));
     *      (PL to Locale polski)
     *   3. ZNAJDŹ BŁĄD:  DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG).withLocale(PL).format(LocalTime.of(14, 5));
     *      — kod się kompiluje, a po uruchomieniu rzuca wyjątek. Dlaczego? Podaj dwa sposoby naprawy.
     *   4. Co wypisze:  LocalDate.parse("2026-02-30", DateTimeFormatter.ofPattern("uuuu-MM-dd"));
     *      a co:         LocalDate.parse("2026-02-30");  ?
     *   5. ZNAJDŹ BŁĄD:  DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd").withResolverStyle(ResolverStyle.STRICT);
     *      LocalDate.parse("2026-03-08", f);   — co się stanie i jak to naprawić na dwa sposoby?
     *   6. Co wypisze:  Duration d = Duration.ofMinutes(125);  System.out.println(d.toMinutesPart() + " " + d.toMinutes());
     *   7. Czemu wolno trzymać DateTimeFormatter w polu static final i używać z wielu wątków, a SimpleDateFormat nie?
     *   8. Jak przyjąć daty „2026-03-08” i „08.03.2026” w jednym polu wejściowym, nie przepuszczając „31.02.2026”?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        runChecks(false);

        section("ĆWICZENIA — rozwiązania wzorcowe");
        runChecks(true);
        // WYNIK: PODSUMOWANIE: ✔ 14 OK, ✘ 0 BŁĄD
    }

    /** Te same sprawdzenia dla Twoich rozwiązań (reference = false) i dla wzorcowych (true). */
    private static void runChecks(boolean reference) {
        LocalDate march8 = LocalDate.of(2026, 3, 8);
        LocalDate dec31 = LocalDate.of(2026, 12, 31);
        Check.equal("ćw. 1: 8 marca 2026", "8 marca 2026",
                () -> reference ? solution1(march8) : exercise1(march8));
        Check.equal("ćw. 1: 31 grudnia 2026", "31 grudnia 2026",
                () -> reference ? solution1(dec31) : exercise1(dec31));

        Check.equal("ćw. 2: ISO", march8, () -> reference ? solution2("2026-03-08") : exercise2("2026-03-08"));
        Check.equal("ćw. 2: kropki", march8, () -> reference ? solution2("08.03.2026") : exercise2("08.03.2026"));
        Check.equal("ćw. 2: słownie", march8,
                () -> reference ? solution2("8 MARCA 2026") : exercise2("8 MARCA 2026"));
        Check.throwsException("ćw. 2: 31.02.2026 odrzucone", IllegalArgumentException.class,
                () -> { if (reference) solution2("31.02.2026"); else exercise2("31.02.2026"); });
        Check.equal("ćw. 2: ISO 29 lutego 2024 (rok przestępny)", LocalDate.of(2024, 2, 29),
                () -> reference ? solution2("2024-02-29") : exercise2("2024-02-29"));

        Check.equal("ćw. 3: poprawna data i godzina", Instant.parse("2026-03-08T13:05:00Z"),
                () -> reference ? solution3("08.03.2026 14:05") : exercise3("08.03.2026 14:05"));
        Check.throwsException("ćw. 3: 31.02.2026 odrzucone", DateTimeParseException.class,
                () -> { if (reference) solution3("31.02.2026 10:00"); else exercise3("31.02.2026 10:00"); });

        Check.equal("ćw. 4: 26:03:04", "26:03:04",
                () -> reference ? solution4(Duration.ofSeconds(26 * 3600 + 3 * 60 + 4)) : exercise4(Duration.ofSeconds(26 * 3600 + 3 * 60 + 4)));
        Check.equal("ćw. 4: 59 s", "00:00:59",
                () -> reference ? solution4(Duration.ofSeconds(59)) : exercise4(Duration.ofSeconds(59)));
        Check.equal("ćw. 4: 125 min", "02:05:00",
                () -> reference ? solution4(Duration.ofMinutes(125)) : exercise4(Duration.ofMinutes(125)));

        Check.equal("ćw. 5: format rzymski", "8 III 2026",
                () -> (reference ? solution5() : exercise5()).format(march8));
        Check.equal("ćw. 5: odczyt rzymski", dec31,
                () -> LocalDate.parse("31 XII 2026", reference ? solution5() : exercise5()));
        Check.summary();
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć datę zapisaną po polsku słownie, np. „8 marca 2026” (dzień bez zera,
     * miesiąc w formie odmienionej, rok czterocyfrowy).
     * Podpowiedź: wzorzec z MMMM (nie LLLL!) i jawny Locale PL.
     */
    static String exercise1(LocalDate date) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): napisz parser dat przyjmujący trzy zapisy: „2026-03-08”, „08.03.2026”
     * oraz „8 marca 2026” (także wielkimi literami, np. „8 MARCA 2026”). Nieistniejącą datę (31.02.2026) lub
     * śmieci odrzuć wyjątkiem IllegalArgumentException.
     * Podpowiedź: lista formatterów STRICT i parseCaseInsensitive; używaj uuuu, nie yyyy.
     */
    static LocalDate exercise2(String text) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ stary kod na java.time. Zwróć chwilę (Instant) dla tekstu w formacie
     * dd.MM.uuuu HH:mm interpretowanego w strefie Europe/Warsaw; nieistniejąca data ma rzucić
     * DateTimeParseException.
     * <pre>{@code
     * // STARY sposób: strefa domyślna komputera, tryb pobłażliwy (lenient), obiekt niebezpieczny wątkowo
     * SimpleDateFormat f = new SimpleDateFormat("dd.MM.yyyy HH:mm");
     * Date d = f.parse(text);              // 31.02.2026 po cichu staje się 3.03.2026!
     * Instant result = d.toInstant();
     * }</pre>
     * Podpowiedź: formatter STRICT → LocalDateTime.parse → atZone(WARSAW) → toInstant().
     */
    static Instant exercise3(String text) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (średnie): zapisz Duration jako zegar HH:mm:ss, gdzie HH to ŁĄCZNA liczba godzin
     * (26 godzin 3 minuty 4 sekundy = „26:03:04”), z zerami wiodącymi (59 s = „00:00:59”).
     * Podpowiedź: String.format("%02d:%02d:%02d", ...) oraz toHours(), toMinutesPart(), toSecondsPart().
     */
    static String exercise4(Duration duration) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): zbuduj DateTimeFormatterem Builder formatter dat z miesiącem zapisanym cyfrą
     * rzymską: „8 III 2026” (dzień bez zera, I–XII, rok czterocyfrowy), działający w obie strony
     * (format i parse).
     * Podpowiedź: appendValue(DAY_OF_MONTH), appendText(MONTH_OF_YEAR, mapa Long → String), appendValue(YEAR, 4).
     */
    static DateTimeFormatter exercise5() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    private static final DateTimeFormatter SOLUTION1 = DateTimeFormatter.ofPattern("d MMMM uuuu", PL);

    static String solution1(LocalDate date) {
        return SOLUTION1.format(date);
    }

    private static final List<DateTimeFormatter> SOLUTION2_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            strict("dd.MM.uuuu"),
            POLISH_LONG_INSENSITIVE);

    static LocalDate solution2(String text) {
        for (DateTimeFormatter format : SOLUTION2_FORMATS) {
            try {
                return LocalDate.parse(text, format);
            } catch (DateTimeParseException e) {
                // próbujemy następny format
            }
        }
        throw new IllegalArgumentException("Nie rozpoznano daty: " + text);
    }

    private static final DateTimeFormatter SOLUTION3 = DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm", PL)
            .withResolverStyle(ResolverStyle.STRICT);

    static Instant solution3(String text) {
        return LocalDateTime.parse(text, SOLUTION3).atZone(WARSAW).toInstant();
    }

    static String solution4(Duration duration) {
        return String.format(Locale.ROOT, "%02d:%02d:%02d",
                duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    static DateTimeFormatter solution5() {
        String[] roman = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};
        return new DateTimeFormatterBuilder()
                .appendValue(ChronoField.DAY_OF_MONTH)
                .appendLiteral(' ')
                .appendText(ChronoField.MONTH_OF_YEAR, monthMap(roman))
                .appendLiteral(' ')
                .appendValue(ChronoField.YEAR, 4)
                .toFormatter(PL)
                .withResolverStyle(ResolverStyle.STRICT);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. ISO_LOCAL_DATE do wymiany danych (pliki, API, baza) — zapis stały, ten sam wszędzie. ofLocalizedDate(MEDIUM)
     *      do tego, co czyta człowiek — układ i nazwy dopasowane do jego kraju. Zamiana miejscami: ISO na ekranie
     *      jest mało naturalne, a format lokalny w pliku zmienia się między Locale i wersjami JDK (nie da się go
     *      bezpiecznie odczytać po latach).
     *   2. Wypisze „8 marzec 2026” — LLLL to forma samodzielna (mianownik), a po numerze dnia powinno być MMMM
     *      („8 marca 2026”).
     *   3. LONG dla czasu pokazuje nazwę strefy, a LocalTime strefy nie ma — format() rzuca DateTimeException
     *      (nie da się wyciągnąć strefy). Naprawa: (a) przekazać ZonedDateTime, (b) dodać .withZone(WARSAW) i
     *      formatować Instant/ZonedDateTime, (c) użyć stylu SHORT lub MEDIUM.
     *   4. Pierwsze zwróci 2026-02-28 (ofPattern jest SMART i przycina dzień do końca miesiąca). Drugie rzuci
     *      DateTimeParseException (ISO_LOCAL_DATE jest STRICT: „Invalid date 'FEBRUARY 30'”).
     *   5. Rzuci DateTimeParseException: yyyy to rok ERY, a STRICT nie zakłada ery, więc nie da się złożyć daty.
     *      Naprawa: (a) uuuu zamiast yyyy, (b) dopisać pole ery G we wzorcu i w tekście („2026-03-08 AD”).
     *   6. „5 125” — toMinutesPart() = 5 (reszta po 2 pełnych godzinach), toMinutes() = 125 (łącznie).
     *   7. Bo jest niezmienny — po utworzeniu nic w nim się nie zmienia, więc wątki nie mogą sobie przeszkadzać.
     *      SimpleDateFormat przechowuje w środku zmienny Calendar, który format() i parse() nadpisują.
     *   8. Lista formatterów STRICT (ISO_LOCAL_DATE i ofPattern("dd.MM.uuuu").withResolverStyle(STRICT)) próbowana po
     *      kolei albo jeden formatter ze wzorcem „[uuuu-MM-dd][dd.MM.uuuu]” w trybie STRICT. STRICT odrzuci 31.02.2026;
     *      domyślny SMART po cichu zamieniłby ją na 28.02.2026.
     */
    // </editor-fold>
}
