package t04_strings;

import helpers.Check;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Formatter;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Ściąga z java.util.Formatter — wszystkie konwersje, flagi, szerokość, precyzja, daty
 *        (formatter = formatujący; conversion = konwersja, czyli "typ" w specyfikatorze; flag = flaga)
 *
 * W SKRÓCIE:
 *   String.format, formatted, printf i klasa Formatter używają tego samego mini-języka specyfikatorów
 *   (np. %-8s, %,.2f, %tF). Strings04Formatting uczy podstaw. Tu jest PEŁNA ŚCIĄGA: każda konwersja, każda flaga,
 *   znaczenie szerokości i precyzji dla każdego typu, indeksy argumentów, daty, wyjątki — jako drukowane tabele.
 *
 * ANALOGIA: specyfikator to formularz z rubrykami. Wpisujesz kolejno: który argument (numer),
 *   jak wypełnić (flagi), na ile miejsc (szerokość), ile znaków po przecinku (precyzja), a na końcu typ danych.
 *   Zły typ w rubryce (np. liczba całkowita do rubryki "ułamek") urzędnik odrzuca — wyjątkiem.
 *
 * JAK TO DZIAŁA:
 *   Składnia:   %[indeks$][flagi][szerokość][.precyzja]konwersja     (daty: %[indeks$][flagi][szerokość]t + litera)
 *
 *   konwersja  typ argumentu           przykład wyniku
 *   %s %S      dowolny (toString)      abc  ABC
 *   %b %B      boolean                 true
 *   %h %H      hashCode szesnastkowo   17862
 *   %c %C      znak                    x
 *   %d         liczba całkowita        -42
 *   %x %X %o   szesnastkowo / ósemkowo ff  FF  10
 *   %f         ułamek dziesiętny       1.500000
 *   %e %E      notacja naukowa         1.234568e+04
 *   %g %G      f albo e (zależnie od wielkości)
 *   %a %A      szesnastkowy ułamek     0x1.0p0
 *   %tY ...    data i czas             2026
 *   %n  %%     nowy wiersz platformy; znak procenta
 *
 * SŁÓWKA: width = szerokość; precision = precyzja; padding = dopełnianie (wyrównywanie spacjami); argument index =
 *   numer argumentu; grouping separator = separator tysięcy; truncate = obciąć; locale = ustawienia regionalne.
 *
 * ZOBACZ TEŻ: t04_strings/Strings04Formatting (podstawy), t04_strings/Strings08RegexAdvanced (regex zamiast formatów),
 *   t15_numbers/Numbers04FormattingParsing (NumberFormat, DecimalFormat), t15_numbers/Numbers01BigDecimal,
 *   t17_datetime/DateTime03Formatting (DateTimeFormatter), t31_jdk_toolbox/Toolbox03I18n (MessageFormat, plural)
 * </pre>
 */
public class Strings09FormatterCheatsheet {

    // Stała data do wszystkich przykładów: niedziela, 15 marca 2026, 14:05:09.123456789
    private static final LocalDateTime MOMENT = LocalDateTime.of(2026, 3, 15, 14, 5, 9, 123_456_789);
    private static final Locale POLISH = Locale.forLanguageTag("pl-PL"); // forLanguageTag = z oznaczenia języka

    public static void main(String[] args) {
        title("Strings09 — ściąga z Formatter");

        generalConversions(); // general conversions = konwersje ogólne
        integers();           // integers = liczby całkowite
        floatingPoint();      // floating point = liczby zmiennoprzecinkowe
        flags();              // flags = flagi
        widthAndPrecision();  // width and precision = szerokość i precyzja
        argumentIndex();      // argument index = numer argumentu
        dateTime();           // date and time = data i czas
        bigDecimalVsDouble(); // BigDecimal kontra double
        whichTool();          // which tool = które narzędzie
        exceptions();         // exceptions = wyjątki
        exercises();          // exercises = ćwiczenia
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI
    // =================================================================================================

    /** Zamienia twarde spacje (U+00A0 i U+202F) na zwykłe — polski separator tysięcy to właśnie taka spacja. */
    static String plain(String text) {
        return text.replace((char) 0x00A0, ' ').replace((char) 0x202F, ' ');
    }

    /** Wiersz tabeli: specyfikator → [wynik] (nawiasy kwadratowe pokazują spacje na brzegach). Locale.ROOT. */
    static void demo(String spec, Object... args) {
        show(spec, "[" + plain(String.format(Locale.ROOT, spec, args)) + "]");
    }

    /** To samo, ale po polsku (pl-PL). */
    static void demoPl(String spec, Object... args) {
        show(spec + "  (pl-PL)", "[" + plain(String.format(POLISH, spec, args)) + "]");
    }

    /** Pokazuje, że specyfikator jest błędny: wypisuje nazwę wyjątku i komunikat. */
    static void bad(String spec, Object... args) {
        expectThrows(spec, () -> String.format(Locale.ROOT, spec, args));
    }

    /** Zamienia znaki nowego wiersza na widoczny napis \n. */
    static String visible(Object value) {
        return String.valueOf(value).replace("\n", "\\n");
    }

    // =================================================================================================
    // 1. KONWERSJE OGÓLNE: %s %S %b %h %c %n %%
    // =================================================================================================

    /**
     * 1. Konwersje, które przyjmują prawie wszystko: {@code %s} (tekst, woła toString), {@code %b} (logiczna),
     * {@code %h} (hashCode w zapisie szesnastkowym), {@code %c} (znak). Wersja z DUŻĄ literą zamienia wynik na wielkie litery.
     */
    static void generalConversions() {
        section("1. Konwersje ogólne: %s %S %b %h %c %n %%");

        demo("%s", "abc");
        demo("%S", "żółw");
        demo("%s", 1.0E10);
        demo("%s", (Object) null);
        // WYNIK: %s → [abc]
        // WYNIK: %S → [ŻÓŁW]
        // WYNIK: %s → [1.0E10]
        // WYNIK: %s → [null]
        // %s woła String.valueOf(argument), więc null daje napis "null" (bez wyjątku), a double — zapis jak Double.toString.
        // %S (duże S) zamienia na wielkie litery według Locale — po turecku "i" daje "İ" (z kropką)!
        show("%S po turecku (Locale tr-TR) dla \"i\"", String.format(Locale.forLanguageTag("tr-TR"), "%S", "i"));
        // WYNIK: %S po turecku (Locale tr-TR) dla "i" → İ

        demo("%b", true);
        demo("%b", (Object) null);
        demo("%b", "false");
        demo("%b", 0);
        // WYNIK: %b → [true]
        // WYNIK: %b → [false]
        // WYNIK: %b → [true]
        // WYNIK: %b → [true]
        // PUŁAPKA: %b to NIE jest konwersja napisu na boolean. null daje false, a KAŻDA inna wartość niż Boolean
        // (nawet napis "false" i liczba 0) daje true. Jedynie prawdziwy Boolean decyduje o wyniku.

        demo("%h", "abc");
        demo("%h", (Object) null);
        // WYNIK: %h → [17862]
        // WYNIK: %h → [null]
        // %h = Integer.toHexString(argument.hashCode()); dla "abc" hashCode to 96354, czyli szesnastkowo 17862.
        // Przydatne do diagnostyki (podobny skrót widać w domyślnym toString obiektu: Klasa@1b6d3586).

        demo("%c", 'x');
        demo("%c", 380);
        demo("%C", 'ż');
        // WYNIK: %c → [x]
        // WYNIK: %c → [ż]
        // WYNIK: %C → [Ż]
        // %c przyjmuje char albo liczbę całkowitą (punkt kodowy Unicode: 380 to U+017C, litera ż).

        demo("%%");
        demo("%5%");
        demo("%-5%|");
        // WYNIK: %% → [%]
        // WYNIK: %5% → [    %]
        // WYNIK: %-5%| → [%    |]
        // %% to dosłowny znak procenta. DOBRA PRAKTYKA: pojedynczy % w tekście formatu (np. "100%") to błąd składni
        // (wyjątek, patrz sekcja 10) — zawsze pisz %%.

        // %n = separator wiersza PLATFORMY: "\r\n" w Windows, "\n" w Linuksie i macOS. \n zawsze jest jednym znakiem.
        show("%n równa się System.lineSeparator()", String.format("a%nb").equals("a" + System.lineSeparator() + "b"));
        // WYNIK: %n równa się System.lineSeparator() → true
        show("długość \"a\\nb\"", "a\nb".length());
        // WYNIK: długość "a\nb" → 3
        // DOBRA PRAKTYKA: do plików tekstowych i protokołów (HTTP, CSV) używaj \n (lub jawnego \r\n) — wynik nie
        // zależy od systemu. %n jest dobre tam, gdzie tekst ma wyglądać "po systemowemu" (wydruk w konsoli, notatnik).
        // W tej lekcji w przykładach drukowanych używamy \n, bo wynik musi być taki sam na każdym komputerze.
    }

    // =================================================================================================
    // 2. LICZBY CAŁKOWITE: %d %x %X %o
    // =================================================================================================

    /**
     * 2. {@code %d} (dziesiętnie), {@code %x}/{@code %X} (szesnastkowo), {@code %o} (ósemkowo) przyjmują
     * byte, short, int, long i BigInteger — a NIE double ani char.
     */
    static void integers() {
        section("2. Liczby całkowite: %d %x %X %o");

        demo("%d", -42);
        demo("%d", 123456789012L);
        demo("%d", new BigInteger("123456789012345678901234567890"));
        demo("%d", (Object) null);
        // WYNIK: %d → [-42]
        // WYNIK: %d → [123456789012]
        // WYNIK: %d → [123456789012345678901234567890]
        // WYNIK: %d → [null]
        demo("%x", 255);
        demo("%X", 255);
        demo("%o", 8);
        demo("%08X", 255);
        demo("%#x", 255);
        demo("%#o", 8);
        // WYNIK: %x → [ff]
        // WYNIK: %X → [FF]
        // WYNIK: %o → [10]
        // WYNIK: %08X → [000000FF]
        // WYNIK: %#x → [0xff]
        // WYNIK: %#o → [010]
        // Ujemne liczby w %x/%o pokazują zapis uzupełnieniowy (dopełnienie do dwóch) w szerokości TYPU argumentu:
        demo("%x", -1);
        demo("%x", (byte) -1);
        demo("%x", -255L);
        demo("%x", new BigInteger("-255"));
        // WYNIK: %x → [ffffffff]
        // WYNIK: %x → [ff]
        // WYNIK: %x → [ffffffffffffff01]
        // WYNIK: %x → [-ff]
        // int -1 to osiem znaków f, byte -1 to "ff", long to szesnaście znaków. BigInteger nie ma stałej szerokości,
        // więc pisze znak minus: -ff.

        // PUŁAPKA: nie ma konwersji dwójkowej (%b to boolean!). Zapis binarny robi Integer.toBinaryString,
        // a uzupełnienie zerami: format %8s + replace.
        show("5 binarnie na 8 miejsc", String.format("%8s", Integer.toBinaryString(5)).replace(' ', '0'));
        // WYNIK: 5 binarnie na 8 miejsc → 00000101
        // Dla bajtu Formatter zna typ Byte, więc (byte) -1 daje FF (a nie FFFFFFFF, jak po rozszerzeniu do int).
        demo("%02X", (byte) -1);
        // WYNIK: %02X → [FF]
        // Błędne typy rzucają wyjątki: "%d" z liczbą 1.5 lub znakiem 'a' (konwersja %d nie przyjmuje double ani char).
        bad("%d", 1.5);
        bad("%d", 'a');
        // WYNIK: ✔ %d → rzucono IllegalFormatConversionException: d != java.lang.Double
        // WYNIK: ✔ %d → rzucono IllegalFormatConversionException: d != java.lang.Character
    }

    // =================================================================================================
    // 3. LICZBY ZMIENNOPRZECINKOWE: %f %e %E %g %a
    // =================================================================================================

    /**
     * 3. Dla double/float/BigDecimal: {@code %f} (zwykły zapis), {@code %e} (naukowy), {@code %g} (wybiera
     * sam), {@code %a} (szesnastkowy). Domyślna precyzja to 6 cyfr po przecinku (dla %g: 6 cyfr znaczących).
     */
    static void floatingPoint() {
        section("3. Ułamki: %f %e %E %g %a");

        double value = 12345.6789;
        demo("%f", value);
        demo("%e", value);
        demo("%E", value);
        demo("%g", value);
        demo("%a", value);
        // WYNIK: %f → [12345.678900]
        // WYNIK: %e → [1.234568e+04]
        // WYNIK: %E → [1.234568E+04]
        // WYNIK: %g → [12345.7]
        // WYNIK: %a → [0x1.81cd6e631f8a1p13]
        // %f i %e: precyzja = liczba cyfr PO kropce. %g: precyzja = liczba cyfr ZNACZĄCYCH (w sumie).
        // %g wybiera zapis naukowy, gdy zaokrąglona liczba < 0,0001 albo >= 10 do potęgi precyzji; zer na końcu NIE obcina.
        demo("%g", 0.0001234);
        demo("%g", 0.00001234);
        demo("%g", 1234567.0);
        demo("%.3g", 1234567.0);
        // WYNIK: %g → [0.000123400]
        // WYNIK: %g → [1.23400e-05]
        // WYNIK: %g → [1.23457e+06]
        // WYNIK: %.3g → [1.23e+06]
        demo("%a", 1.0);
        demo("%a", 0.5);
        // WYNIK: %a → [0x1.0p0]
        // WYNIK: %a → [0x1.0p-1]
        // %a = zapis binarny "wprost": 0x1.0p-1 znaczy 1,0 * 2 do potęgi -1. Używany do debugowania dokładnej wartości.

        // Zaokrąglanie: ZAWSZE "połowa w górę" (HALF_UP), a nie bankierskie jak w C czy w DecimalFormat.
        demo("%.0f", 2.5);
        demo("%.0f", 3.5);
        demo("%.1f", 0.25);
        // WYNIK: %.0f → [3]
        // WYNIK: %.0f → [4]
        // WYNIK: %.1f → [0.3]
        demo("%f", Double.NaN);
        demo("%f", Double.POSITIVE_INFINITY);
        demo("%10.2f", Double.NaN);
        // WYNIK: %f → [NaN]
        // WYNIK: %f → [Infinity]
        // WYNIK: %10.2f → [       NaN]
        // NaN i Infinity nie dostają zer ani zer wiodących — flaga 0 jest wtedy ignorowana, a szerokość dopełnia spacjami.

        // PUŁAPKA: %f NIE przyjmuje liczb całkowitych (wyjątek), a %d nie przyjmuje double. Nie ma automatycznej konwersji.
        bad("%f", 1);
        // WYNIK: ✔ %f → rzucono IllegalFormatConversionException: f != java.lang.Integer
        // DOBRA PRAKTYKA: w %.2f nie liczysz pieniędzy — formatowanie to WYŚWIETLANIE. Pieniądze licz w BigDecimal (sekcja 8).
    }

    // =================================================================================================
    // 4. FLAGI
    // =================================================================================================

    /**
     * 4. Flagi stoją po {@code %} (i po opcjonalnym {@code indeks$}), a przed szerokością. Można je łączyć,
     * ale nie wszystkie ze wszystkimi.
     * <pre>{@code
     * -  wyrównaj do lewej (wymaga szerokości)     0  dopełnij zerami (wymaga szerokości)
     * +  zawsze znak + lub -                       spacja: miejsce na znak (spacja zamiast +)
     * ,  separator tysięcy (według Locale)         (  ujemne w nawiasach
     * #  forma alternatywna: 0x, 0, kropka dziesiętna
     * }</pre>
     */
    static void flags() {
        section("4. Flagi: - 0 + spacja , ( #");

        demo("%6d|", 42);
        demo("%-6d|", 42);
        demo("%06d|", 42);
        demo("%+d", 5);
        demo("% d", 5);
        demo("%(d", -5);
        demo("%,d", 1234567);
        demo("%#x", 255);
        demo("%#.0f", 3.0);
        // WYNIK: %6d| → [    42|]
        // WYNIK: %-6d| → [42    |]
        // WYNIK: %06d| → [000042|]
        // WYNIK: %+d → [+5]
        // WYNIK: % d → [ 5]
        // WYNIK: %(d → [(5)]
        // WYNIK: %,d → [1,234,567]
        // WYNIK: %#x → [0xff]
        // WYNIK: %#.0f → [3.]
        // Kombinacje: znak i tysiące razem; ujemne w nawiasach z tysiącami i precyzją.
        demo("%+,d", 1234);
        demo("%(,.2f", -1234.5);
        demo("% ,d", 1234);
        demo("%,d", -1234);
        demo("%010.3f", -3.14159);
        // WYNIK: %+,d → [+1,234]
        // WYNIK: %(,.2f → [(1,234.50)]
        // WYNIK: % ,d → [ 1,234]
        // WYNIK: %,d → [-1,234]
        // WYNIK: %010.3f → [-00003.142]
        // Flaga , działa tylko dla %d, %f i %g. Dla %x, %o, %e i %s to wyjątek (patrz sekcja 10).

        // --- separator tysięcy zależy od Locale ---
        demo("%,d", 1234567);
        demoPl("%,d", 1234567);
        demoPl("%,.2f", 1234567.891);
        // WYNIK: %,d → [1,234,567]
        // WYNIK: %,d  (pl-PL) → [1 234 567]
        // WYNIK: %,.2f  (pl-PL) → [1 234 567,89]
        // PUŁAPKA: polski separator tysięcy to NIE zwykła spacja, tylko twarda spacja U+00A0 (w innych wersjach Javy
        // bywa też U+202F, wąska twarda spacja). Gołym okiem nie do odróżnienia, ale equals("1 234") da false
        // i porównanie w teście lub parsowanie pliku się wysypie. Dlatego w tej lekcji przed wydrukiem robimy
        // plain(...): replace(U+00A0, ' ') oraz replace(U+202F, ' ').
        // DOBRA PRAKTYKA: do plików, JSON-a i komunikacji między programami formatuj z Locale.ROOT (bez separatorów,
        // z kropką); lokalny format zostaw wyłącznie na ekran dla człowieka.
    }

    // =================================================================================================
    // 5. SZEROKOŚĆ I PRECYZJA
    // =================================================================================================

    /**
     * 5. Szerokość to MINIMALNA liczba znaków (krótszy wynik jest dopełniany spacjami, dłuższy NIE jest obcinany).
     * Precyzja znaczy co innego dla każdej konwersji:
     * <pre>{@code
     * %s %S %b %h  precyzja OBCINA wynik do n znaków
     * %f %e        n cyfr po kropce            %g   n cyfr znaczących w sumie
     * %d %x %o %c  precyzja NIE jest dozwolona (wyjątek)
     * }</pre>
     */
    static void widthAndPrecision() {
        section("5. Szerokość i precyzja");

        demo("%8s|", "abc");
        demo("%-8s|", "abc");
        demo("%.3s|", "abcdef");
        demo("%8.3s|", "abcdef");
        demo("%-8.3s|", "abcdef");
        // WYNIK: %8s| → [     abc|]
        // WYNIK: %-8s| → [abc     |]
        // WYNIK: %.3s| → [abc|]
        // WYNIK: %8.3s| → [     abc|]
        // WYNIK: %-8.3s| → [abc     |]
        // PUŁAPKA: precyzja obcina też wtedy, gdy się jej nie spodziewasz: null jest zamieniany na napis "null", więc
        // %.2f z null daje "nu" (bez wyjątku!), a %.2b z true daje "tr".
        demo("%.2f", (Object) null);
        demo("%.2b", true);
        // WYNIK: %.2f → [nu]
        // WYNIK: %.2b → [tr]
        demo("%2d", 12345);
        demo("%5d|", 42);
        demo("%8x|", 255);
        // WYNIK: %2d → [12345]
        // WYNIK: %5d| → [   42|]
        // WYNIK: %8x| → [      ff|]
        // Za wąska szerokość nic nie obcina: %2d dla 12345 daje pełne 12345.
        demo("%8.2f|", 3.14159);
        demo("%.0f", 3.14159);
        demo("%.10f", 0.5);
        demo("%.2e", 12345.6789);
        demo("%.3g", 12345.6789);
        // WYNIK: %8.2f| → [    3.14|]
        // WYNIK: %.0f → [3]
        // WYNIK: %.10f → [0.5000000000]
        // WYNIK: %.2e → [1.23e+04]
        // WYNIK: %.3g → [1.23e+04]

        // Szerokość znana dopiero w czasie działania: sklejasz tekst formatu (gwiazdka z języka C NIE istnieje).
        int width = 12;
        String dynamic = "%-" + width + "s|%" + width + ".2f|";
        demo(dynamic, "Kawa", 64.99);
        // WYNIK: %-12s|%12.2f| → [Kawa        |       64.99|]
        bad("%*d", 5, 42);
        // WYNIK: ✔ %*d → rzucono UnknownFormatConversionException: Conversion = '*'
        // DOBRA PRAKTYKA: szerokość kolumny licz z danych (najdłuższy napis), a nie na oko — patrz ćwiczenie 4.
        // Uwaga: szerokość liczy się w jednostkach char, więc emoji i znaki złożone mogą rozjechać tabelę.
    }

    // =================================================================================================
    // 6. NUMER ARGUMENTU
    // =================================================================================================

    /**
     * 6. {@code %2$s} bierze argument numer 2 (numeracja od 1). {@code %<s} bierze TEN SAM argument co poprzedni
     * specyfikator. Zwykłe {@code %s} (bez numeru) mają własny licznik, niezależny od numerów jawnych.
     */
    static void argumentIndex() {
        section("6. Numer argumentu: %1$s i %<s");

        demo("%2$s %1$s", "świat", "Witaj");
        demo("%1$s i jeszcze raz %1$s", "echo");
        demo("%s %<s %s", "a", "b");
        // WYNIK: %2$s %1$s → [Witaj świat]
        // WYNIK: %1$s i jeszcze raz %1$s → [echo i jeszcze raz echo]
        // WYNIK: %s %<s %s → [a a b]
        // %s %<s %s: pierwsze %s = argument 1, %<s = ten sam, drugie %s = argument 2 (zwykły licznik idzie dalej).
        demo("%1$s %s %s", "a", "b");
        demo("%2$s %s %s", "a", "b");
        // WYNIK: %1$s %s %s → [a a b]
        // WYNIK: %2$s %s %s → [b a b]
        // PUŁAPKA: zwykłe %s i numerowane %2$s mieszają się nieintuicyjnie: zwykłe liczą się od początku, ignorując
        // numerowane. W drugim przykładzie wynik to "b a b". DOBRA PRAKTYKA: w jednym tekście używaj jednego stylu.

        // Po co to? (1) Powtórzenie wartości bez podawania jej dwa razy. (2) Tłumaczenie: szyk zdania zależy od języka,
        // więc napis z tłumaczenia może zmienić kolejność argumentów bez zmiany kodu.
        String polishText = "%1$s kupił %2$d szt. (%1$s, VIP)";
        String englishText = "%2$d items bought by %1$s";
        demo(polishText, "Jan", 3);
        demo(englishText, "Jan", 3);
        // WYNIK: %1$s kupił %2$d szt. (%1$s, VIP) → [Jan kupił 3 szt. (Jan, VIP)]
        // WYNIK: %2$d items bought by %1$s → [3 items bought by Jan]

        // Nadmiarowe argumenty są po cichu ignorowane (bez wyjątku), brakujące — to wyjątek.
        demo("%s", "a", "b");
        bad("%s %s", "a");
        bad("%3$s", "a");
        // WYNIK: %s → [a]
        // WYNIK: ✔ %s %s → rzucono MissingFormatArgumentException: Format specifier '%s'
        // WYNIK: ✔ %3$s → rzucono MissingFormatArgumentException: Format specifier '%3$s'
        // PUŁAPKA: nadmiar to najczęściej literówka (zgubiony specyfikator), a Java milczy. W kodzie testuj teksty formatu.
    }

    // =================================================================================================
    // 7. DATA I CZAS: %t
    // =================================================================================================

    /**
     * 7. Konwersje daty/czasu mają dwuznakowy zapis: {@code %t} + litera ({@code %tY}), a wersja z dużym {@code %T}
     * zamienia wynik na wielkie litery. Działają z klasami java.time (LocalDateTime, LocalDate, ZonedDateTime...),
     * a także ze starymi Date, Calendar i liczbą long. Argument MUSI zawierać potrzebne pole.
     */
    static void dateTime() {
        section("7. Data i czas: %tY %tm %td %tH %tM %tS %tB %tA");

        // Data: Y rok (4 cyfry), y rok (2), C wiek, m miesiąc 01-12, d dzień 01-31, e dzień bez zera, j dzień roku
        for (String code : List.of("Y", "y", "C", "m", "d", "e", "j")) {
            demo("%t" + code, MOMENT);
        }
        // WYNIK: %tY → [2026]
        // WYNIK: %ty → [26]
        // WYNIK: %tC → [20]
        // WYNIK: %tm → [03]
        // WYNIK: %td → [15]
        // WYNIK: %te → [15]
        // WYNIK: %tj → [074]
        // Czas: H godzina 00-23, I godzina 01-12, k/l to samo bez zera wiodącego, M minuta, S sekunda, L milisekundy, N nanosekundy
        for (String code : List.of("H", "I", "k", "l", "M", "S", "L", "N")) {
            demo("%t" + code, MOMENT);
        }
        // WYNIK: %tH → [14]
        // WYNIK: %tI → [02]
        // WYNIK: %tk → [14]
        // WYNIK: %tl → [2]
        // WYNIK: %tM → [05]
        // WYNIK: %tS → [09]
        // WYNIK: %tL → [123]
        // WYNIK: %tN → [123456789]
        // Nazwy (zależą od Locale!): B miesiąc, b skrót miesiąca, A dzień tygodnia, a skrót dnia, p am/pm
        for (String code : List.of("B", "b", "A", "a", "p")) {
            demo("%t" + code, MOMENT);
        }
        // WYNIK: %tB → [Mar]
        // WYNIK: %tb → [Mar]
        // WYNIK: %tA → [Sun]
        // WYNIK: %ta → [Sun]
        // WYNIK: %tp → [pm]
        demoPl("%tB", MOMENT);
        demoPl("%tA", MOMENT);
        demoPl("%tb", MOMENT);
        demoPl("%ta", MOMENT);
        // WYNIK: %tB  (pl-PL) → [marca]
        // WYNIK: %tA  (pl-PL) → [niedziela]
        // WYNIK: %tb  (pl-PL) → [mar]
        // WYNIK: %ta  (pl-PL) → [niedz.]
        // PUŁAPKA: %tB po polsku daje "marca" (dopełniacz, jak w dacie "15 marca"), a nie "marzec". Do nazwy miesiąca
        // w mianowniku użyj DateTimeFormatter z wzorcem LLLL (t17_datetime/DateTime03Formatting). Dla Locale.ROOT
        // nazwy są angielskie (Mar, Sun), a %tp daje małe "pm" — dla "PM" użyj %Tp.
        demo("%1$Tp %1$TB %1$TA", MOMENT);
        // WYNIK: %1$Tp %1$TB %1$TA → [PM MAR SUN]

        // Gotowe skróty: F = ISO (RRRR-MM-DD), T = HH:MM:SS, R = HH:MM, r = hh:MM:SS AM/PM, D = MM/DD/RR (amerykański!)
        for (String code : List.of("F", "T", "R", "r", "D")) {
            demo("%t" + code, MOMENT);
        }
        // WYNIK: %tF → [2026-03-15]
        // WYNIK: %tT → [14:05:09]
        // WYNIK: %tR → [14:05]
        // WYNIK: %tr → [02:05:09 PM]
        // WYNIK: %tD → [03/15/26]
        // Ta sama data kilka razy: numer argumentu + %t
        demo("%1$td.%1$tm.%1$tY o %1$tH:%1$tM", MOMENT);
        // WYNIK: %1$td.%1$tm.%1$tY o %1$tH:%1$tM → [15.03.2026 o 14:05]

        // Strefa czasowa: potrzebny ZonedDateTime (LocalDateTime nie ma strefy). Z strefą działają też %tZ, %tz, %tc, %ts.
        ZonedDateTime warsaw = ZonedDateTime.of(MOMENT, ZoneId.of("Europe/Warsaw"));
        demo("%tZ", warsaw);
        demo("%tz", warsaw);
        demo("%tc", warsaw);
        demo("%ts", warsaw);
        // WYNIK: %tZ → [CET]
        // WYNIK: %tz → [+0100]
        // WYNIK: %tc → [Sun Mar 15 14:05:09 CET 2026]
        // WYNIK: %ts → [1773579909]
        // %tZ = skrót strefy, %tz = przesunięcie względem UTC, %tc = pełna data, %ts = sekundy od 1970 (epoka).
        // PUŁAPKA: brakujące pole to wyjątek, nie puste miejsce. Każdy typ obsługuje tylko "swoje" konwersje.
        bad("%tZ", MOMENT);
        bad("%tH", LocalDate.of(2026, 3, 15));
        bad("%tY", Instant.EPOCH);
        // WYNIK: ✔ %tZ → rzucono IllegalFormatConversionException: Z != java.time.LocalDateTime
        // WYNIK: ✔ %tH → rzucono IllegalFormatConversionException: H != java.time.LocalDate
        // WYNIK: ✔ %tY → rzucono IllegalFormatConversionException: Y != java.time.Instant
        // Instant to tylko punkt na osi czasu (bez kalendarza) — rok i godzinę da dopiero atZone(strefa).
        // DOBRA PRAKTYKA: do logów i prostych komunikatów %t jest wygodne, ale do dłuższych formatów wybierz
        // DateTimeFormatter: jest niezmienny, ma nazwane wzorce i dokładniej obsługuje polskie nazwy.
    }

    // =================================================================================================
    // 8. BIGDECIMAL KONTRA DOUBLE
    // =================================================================================================

    /**
     * 8. {@code %f} z double formatuje KRÓTKI zapis dziesiętny liczby (taki, jaki daje Double.toString), a nie jej
     * dokładną wartość binarną; z BigDecimal formatuje wartość dokładną. To ważna różnica przy zaokrąglaniu.
     */
    static void bigDecimalVsDouble() {
        section("8. BigDecimal kontra double w %f");

        demo("%.20f", 0.1);
        demo("%.20f", new BigDecimal(0.1));
        // WYNIK: %.20f → [0.10000000000000000000]
        // WYNIK: %.20f → [0.10000000000000000555]
        // double 0.1 w komputerze to naprawdę 0.1000000000000000055511151231257827... Formatter dla double bierze krótką
        // postać "0.1" i dopisuje zera — UKRYWA błąd binarny. new BigDecimal(0.1) pokazuje prawdziwą wartość tego double'a.
        demo("%.20f", 0.1 + 0.2);
        // WYNIK: %.20f → [0.30000000000000004000]
        // PUŁAPKA: 0.1 + 0.2 to w istocie 0.3000000000000000444..., ale %f pokazuje 0.30000000000000004 (krótką postać)
        // z dopisanymi zerami. Stąd złudzenie, że "format wszystko naprawia". To tylko wygładza wyświetlanie.

        // Zaokrąglanie do 2 miejsc: ten sam napis "1.005", trzy różne drogi.
        demo("%.2f", 1.005);
        demo("%.2f", new BigDecimal(1.005));
        demo("%.2f", new BigDecimal("1.005"));
        // WYNIK: %.2f → [1.01]
        // WYNIK: %.2f → [1.00]
        // WYNIK: %.2f → [1.01]
        // double 1.005: krótka postać "1.005", zaokrąglona HALF_UP → 1.01 (choć prawdziwy double to 1.00499999999999989...).
        // new BigDecimal(1.005): dokładna wartość binarna 1.00499999999999989... → 1.00.
        // new BigDecimal("1.005"): dokładnie 1.005 z napisu → HALF_UP → 1.01.
        // DOBRA PRAKTYKA: pieniądze trzymaj w BigDecimal zbudowanym z NAPISU (nigdy z double), a zaokrąglaj jawnie:
        // setScale(2, RoundingMode.HALF_UP). Formatowanie służy tylko do wyświetlenia (t15_numbers/Numbers01BigDecimal).

        demo("%s", new BigDecimal("1E+3"));
        demo("%f", new BigDecimal("1E+3"));
        demo("%s|%.1f", new BigDecimal("2.50"), new BigDecimal("2.50"));
        demo("%,.2f", new BigDecimal("1234567.891"));
        // WYNIK: %s → [1E+3]
        // WYNIK: %f → [1000.000000]
        // WYNIK: %s|%.1f → [2.50|2.5]
        // WYNIK: %,.2f → [1,234,567.89]
        // %s z BigDecimal to toString() — może dać zapis naukowy (1E+3). Do ekranu użyj %f z jawną precyzją.
        // PUŁAPKA: %.1f z 2.50 zaokrągla do 2.5 TYLKO w wyświetlaniu — wartość obiektu nie zmieniła się.
    }

    // =================================================================================================
    // 9. KTÓRE NARZĘDZIE? Formatter, String.format, formatted, printf, MessageFormat, DecimalFormat
    // =================================================================================================

    /**
     * 9. Wszystkie narzędzia z rodziny printf to w gruncie rzeczy {@code Formatter}. Różnią się tym, GDZIE
     * wynik ląduje i jakiego Locale użyją, gdy go nie podasz.
     * <pre>{@code
     * String.format(Locale, fmt, args)  -> String                   (zalecane: jawny Locale)
     * "fmt".formatted(args)             -> String (Java 15+)        (Locale domyślny, bez możliwości zmiany)
     * System.out.printf(Locale, ...)    -> wypisuje na PrintStream  (Locale domyślny, gdy go pominiesz)
     * new Formatter(sb, Locale)         -> dopisuje do StringBuilder (wiele fragmentów bez pośrednich Stringów)
     * MessageFormat                     -> "Mam {0} plików"; numery zamiast typów; osobna składnia
     * DecimalFormat / NumberFormat      -> wzorce liczb "#,##0.00"; w liczbach inne zaokrąglanie (HALF_EVEN)
     * }</pre>
     */
    static void whichTool() {
        section("9. String.format, formatted, printf, Formatter, MessageFormat, DecimalFormat");

        // --- Formatter dopisujący do StringBuilder ---
        StringBuilder sb = new StringBuilder();
        try (Formatter formatter = new Formatter(sb, Locale.ROOT)) { // try-with-resources zamyka Formatter
            formatter.format("%-6s|%6.2f\n", "Kawa", 64.99);
            formatter.format("%-6s|%6.2f\n", "Cukier", 4.5);
            formatter.format("%-6s|%6.2f", "Sól", 2.0);
        }
        show("Formatter do StringBuilder", visible(sb));
        // WYNIK: Formatter do StringBuilder → Kawa  | 64.99\nCukier|  4.50\nSól   |  2.00
        // Formatter pozwala złożyć wiele wierszy w jednym buforze, bez tworzenia pośrednich Stringów.
        // Zamknięcie Formattera nie zamyka StringBuildera (nie ma czego zamykać) — sb dalej jest używalny.

        // --- String.format a formatted ---
        show("String.format  vs  formatted (Java 15+)", String.format(Locale.ROOT, "%03d", 7) + " / " + "%03d".formatted(7));
        // WYNIK: String.format  vs  formatted (Java 15+) → 007 / 007
        // formatted to ten sam mechanizm w krótszej postaci: "tekst %d".formatted(x). Ale bez parametru Locale.

        // --- Locale domyślny: ten sam kod, inny wynik ---
        // Domyślny Locale zależy od komputera (Windows po polsku: pl-PL). Ustawiamy go tymczasowo, żeby pokazać skutek.
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, POLISH);
            show("domyślny pl-PL: String.format(\"%,.2f\")", plain(String.format("%,.2f", 1234.5)));
            // WYNIK: domyślny pl-PL: String.format("%,.2f") → 1 234,50
            show("domyślny pl-PL: \"%,.2f\".formatted", plain("%,.2f".formatted(1234.5)));
            // WYNIK: domyślny pl-PL: "%,.2f".formatted → 1 234,50
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            show("domyślny en-US: String.format(\"%,.2f\")", plain(String.format("%,.2f", 1234.5)));
            // WYNIK: domyślny en-US: String.format("%,.2f") → 1,234.50
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old); // zawsze przywracamy
        }
        // PUŁAPKA: program z String.format("%.2f", x) bez Locale na polskim komputerze wypisze przecinek, a na serwerze
        // CI lub w kontenerze (często en/ROOT) kropkę. Testy raz przechodzą, raz nie, a plik CSV ma przecinki w liczbach.
        // DOBRA PRAKTYKA: zawsze podawaj Locale jawnie (Locale.ROOT dla maszyn, pl-PL dla ludzi).

        // --- printf = format + wypisanie ---
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        stream.printf(Locale.ROOT, "%s=%.1f", "pi", Math.PI);
        stream.flush();
        show("printf (do własnego strumienia)", bytes.toString(StandardCharsets.UTF_8));
        // WYNIK: printf (do własnego strumienia) → pi=3.1

        // --- MessageFormat: {0}, {1}, ... zamiast typów ---
        MessageFormat plainMessage = new MessageFormat("Mam {0} zł i {1,number,#.##} m", Locale.ROOT);
        show("MessageFormat z Locale.ROOT", plainMessage.format(new Object[] {1234567, 3.14159}));
        // WYNIK: MessageFormat z Locale.ROOT → Mam 1,234,567 zł i 3.14 m
        show("MessageFormat z pl-PL", plain(new MessageFormat("Mam {0} zł", POLISH).format(new Object[] {1234567})));
        // WYNIK: MessageFormat z pl-PL → Mam 1 234 567 zł
        show("MessageFormat: apostrof", MessageFormat.format("To nie '{0}' tylko {0}", "x"));
        // WYNIK: MessageFormat: apostrof → To nie {0} tylko x
        show("MessageFormat: podwójny apostrof", MessageFormat.format("It''s {0}", "ok"));
        // WYNIK: MessageFormat: podwójny apostrof → It's ok
        // PUŁAPKA: w MessageFormat pojedynczy apostrof zaczyna cytat (tekst w środku NIE jest zamieniany), a literalny
        // apostrof trzeba podwoić ''. Statyczne MessageFormat.format używa Locale domyślnego i formatuje liczby
        // (1234567 → "1 234 567"), czego nie widać w Formatterze (tam %d nie dodaje separatora bez flagi ,).
        show("MessageFormat choice (liczba mnoga)", new MessageFormat(
                "Mam {0,choice,0#brak plików|1#jeden plik|1<{0} plików}", Locale.ROOT).format(new Object[] {5}));
        // WYNIK: MessageFormat choice (liczba mnoga) → Mam 5 plików
        // Pełne reguły liczby mnogiej (po polsku: 1 plik, 2 pliki, 5 plików) i tłumaczenia: t31_jdk_toolbox/Toolbox03I18n.

        // --- DecimalFormat: wzorce liczb i zaokrąglanie HALF_EVEN ---
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT); // symbols = symbole (kropka, przecinek)
        show("DecimalFormat #,##0.00", new DecimalFormat("#,##0.00", symbols).format(1234.5));
        // WYNIK: DecimalFormat #,##0.00 → 1,234.50
        show("DecimalFormat \"0\" dla 2.5 i 3.5",
                new DecimalFormat("0", symbols).format(2.5) + " i " + new DecimalFormat("0", symbols).format(3.5));
        // WYNIK: DecimalFormat "0" dla 2.5 i 3.5 → 2 i 4
        demo("%.0f", 2.5);
        // WYNIK: %.0f → [3]
        // PUŁAPKA: DecimalFormat zaokrągla domyślnie HALF_EVEN (2.5 → 2, 3.5 → 4), a Formatter HALF_UP (2.5 → 3).
        // DecimalFormat i NumberFormat NIE są bezpieczne wątkowo (mają stan) — String.format jest.
        // Dla parsowania tekstu na liczbę (Formatter tego nie umie) użyj NumberFormat (t15_numbers/Numbers04FormattingParsing).
        //
        // Kiedy co:  zwykły tekst z liczbami → String.format/formatted;  wiele wierszy do bufora → Formatter;
        //            komunikaty tłumaczone i liczba mnoga → MessageFormat/ResourceBundle;  wzorzec waluty/procentu
        //            lub PARSOWANIE → NumberFormat/DecimalFormat;  data → DateTimeFormatter.
    }

    // =================================================================================================
    // 10. WYJĄTKI: RODZINA IllegalFormatException
    // =================================================================================================

    /**
     * 10. Tekst formatu to zwykły napis — kompilator go nie sprawdza. Błędy wychodzą dopiero w czasie działania jako
     * podklasy {@code IllegalFormatException} (a ta dziedziczy po IllegalArgumentException).
     */
    static void exceptions() {
        section("10. Rodzina IllegalFormatException");

        bad("%s %s", "a");                 // zabrakło argumentu
        bad("%d", 1.5);                    // zły typ argumentu
        bad("%q", 1);                      // nieznana konwersja
        bad("%.2d", 5);                    // precyzja niedozwolona dla %d
        bad("%-d", 5);                     // flaga - bez szerokości
        bad("%-05d", 5);                   // flagi - i 0 razem
        bad("%--5d", 5);                   // powtórzona flaga
        bad("%,s", "a");                   // flaga , niedozwolona dla %s
        bad("%,x", 255);                   // flaga , niedozwolona dla %x
        bad("%#d", 5);                     // flaga # niedozwolona dla %d
        bad("%0$s", "a");                  // numer argumentu 0 (numeracja od 1)
        bad("100%", 5);                    // zbłąkany procent na końcu
        // WYNIK: ✔ %s %s → rzucono MissingFormatArgumentException: Format specifier '%s'
        // WYNIK: ✔ %d → rzucono IllegalFormatConversionException: d != java.lang.Double
        // WYNIK: ✔ %q → rzucono UnknownFormatConversionException: Conversion = 'q'
        // WYNIK: ✔ %.2d → rzucono IllegalFormatPrecisionException: 2
        // WYNIK: ✔ %-d → rzucono MissingFormatWidthException: %-d
        // WYNIK: ✔ %-05d → rzucono IllegalFormatFlagsException: Flags = '-0'
        // WYNIK: ✔ %--5d → rzucono DuplicateFormatFlagsException: Flags = '-'
        // WYNIK: ✔ %,s → rzucono FormatFlagsConversionMismatchException: Conversion = s, Flags = ,
        // WYNIK: ✔ %,x → rzucono FormatFlagsConversionMismatchException: Conversion = x, Flags = ,
        // WYNIK: ✔ %#d → rzucono FormatFlagsConversionMismatchException: Conversion = d, Flags = #
        // WYNIK: ✔ %0$s → rzucono IllegalFormatArgumentIndexException: Illegal format argument index = 0
        // WYNIK: ✔ 100% → rzucono UnknownFormatConversionException: Conversion = '%'
        // Nazwy mówią, co poszło źle: MissingFormatArgumentException (brak argumentu), IllegalFormatConversionException
        // (typ), UnknownFormatConversionException (nieznana litera), IllegalFormatPrecisionException (precyzja),
        // MissingFormatWidthException (brak szerokości), IllegalFormatFlagsException (zła kombinacja flag),
        // DuplicateFormatFlagsException (powtórzona flaga), FormatFlagsConversionMismatchException (flaga nie pasuje
        // do konwersji), IllegalFormatArgumentIndexException (zły numer argumentu, Java 16+).

        // Wszystkie to IllegalFormatException, więc można je złapać jednym catch:
        try {
            String.format("%d", "5");
        } catch (IllegalFormatException e) {
            show("złapane jako IllegalFormatException", e.getClass().getSimpleName());
            // WYNIK: złapane jako IllegalFormatException → IllegalFormatConversionException
            show("a to także IllegalArgumentException", e instanceof IllegalArgumentException);
            // WYNIK: a to także IllegalArgumentException → true
        }

        // PUŁAPKA: wyjątek formatu bywa ukryty w rzadko wykonywanej ścieżce (np. w obsłudze błędu) — i wtedy PRZYSŁANIA
        // oryginalny błąd: w catch budujesz komunikat z literówką, a na wierzch wychodzi IllegalFormatException.
        try {
            try {
                throw new IllegalStateException("oryginalny błąd");
            } catch (IllegalStateException e) {
                String message = String.format("Błąd: %s (%s)", e.getMessage()); // zabrakło drugiego argumentu
                show("komunikat", message);
            }
        } catch (IllegalFormatException e) {
            show("zamiast oryginalnego błędu poleciało", e.getClass().getSimpleName());
            // WYNIK: zamiast oryginalnego błędu poleciało → MissingFormatArgumentException
        }
        // DOBRA PRAKTYKA: napisz test (lub choćby jedno wywołanie), które wykona KAŻDY tekst formatu. W kodzie
        // diagnostycznym wolisz prostą konkatenację lub logger z {} (nie rzuca wyjątku) niż zawiłe String.format.
        // null pasuje do każdej konwersji (nie jest błędem typu): "%d" z null daje "null".
        demo("%d", (Object) null);
        // WYNIK: %d → [null]
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • składnia: %[indeks$][flagi][szerokość][.precyzja]konwersja; wielka litera konwersji = wynik WIELKIMI literami
     *   • %s (toString, null → "null"), %b (null → false, cokolwiek innego niż Boolean → true), %c (znak lub kod),
     *     %h (hashCode szesnastkowo), %d %x %o (całkowite; BEZ binarnych), %f %e %g %a (ułamki), %n (wiersz platformy), %%
     *   • flagi: - (do lewej), 0 (zera), + (znak), spacja, , (tysiące wg Locale), ( (nawiasy), # (0x, 0, kropka)
     *   • szerokość = minimum (nie obcina); precyzja: %s obcina, %f/%e = cyfry po kropce, %g = cyfry znaczące, %d/%x/%c: błąd
     *   • %f zaokrągla HALF_UP; double → krótki zapis dziesiętny, BigDecimal → wartość dokładna; %f nie przyjmuje int
     *   • %2$s numer argumentu, %<s ten sam co poprzedni; za dużo argumentów = cisza, za mało = wyjątek
     *   • daty: %tY %tm %td %tH %tM %tS, %tF %tT %tR, %tB %tA zależą od Locale (po polsku: "marca"); %tZ wymaga strefy
     *   • String.format(Locale.ROOT, ...) dla maszyn; formatted i printf używają Locale domyślnego (pułapka)
     *   • polski separator tysięcy to twarda spacja U+00A0 (lub U+202F) — przed porównaniem zamień na zwykłą
     *   • MessageFormat: apostrof ' to cytat ('' = apostrof); DecimalFormat: HALF_EVEN; DecimalFormat nie jest bezpieczny wątkowo
     *   • \n zawsze jeden znak, %n zależy od systemu; wyjątki: IllegalFormatException i podklasy (czas działania!)
     *
     * PYTANIA KONTROLNE:
     *   1. Rozbierz specyfikator %-12.3s: co znaczy każda część?
     *   2. Co wypisze:  System.out.println(String.format(Locale.ROOT, "%08.3f", -3.14159));  ?
     *   3. Co wypisze:  System.out.println(String.format("%2$s %1$s %s", "a", "b"));  ?
     *   4. ZNAJDŹ BŁĄD:  String.format("%d", 3.0);   — co rzuci i jak naprawić?
     *   5. Co wypisze:  System.out.println(String.format(Locale.ROOT, "%.3s|%5.1f|%b", "abcdef", 2.25, "false"));  ?
     *   6. Dlaczego String.format("%,d", 1234567) na polskim Windows daje tekst, który nie jest równy "1 234 567"?
     *   7. Dlaczego %.2f dla double 1.005 daje 1.01, a dla new BigDecimal(1.005) daje 1.00?
     *   8. ZNAJDŹ BŁĄD:  String s = String.format("Rabat: 15%", rabat);   i  String.format("%s %s", name);
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: wiersz paragonu", "Kawa        2 x    64.99", () -> exercise1("Kawa", 2, 64.99));
        Check.equal("ćw. 2: kwota po polsku", List.of("1 234,50 zł", "0,07 zł", "-12,35 zł"),
                () -> List.of(exercise2(new BigDecimal("1234.5")), exercise2(new BigDecimal("0.07")),
                        exercise2(new BigDecimal("-12.345"))));
        Check.equal("ćw. 3: bajty szesnastkowo", "0A:FF:00", () -> exercise3(new byte[] {10, -1, 0}));
        Check.equal("ćw. 4: tabela", EXPECTED_TABLE,
                () -> exercise4(List.of("Kawa", "Czekolada gorzka", "Lampka"), List.of(64.99, 7.49, 129.0)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "Kawa        2 x    64.99", () -> solution1("Kawa", 2, 64.99));
        Check.equal("ćw. 2 (wzorzec)", List.of("1 234,50 zł", "0,07 zł", "-12,35 zł"),
                () -> List.of(solution2(new BigDecimal("1234.5")), solution2(new BigDecimal("0.07")),
                        solution2(new BigDecimal("-12.345"))));
        Check.equal("ćw. 3 (wzorzec)", "0A:FF:00", () -> solution3(new byte[] {10, -1, 0}));
        Check.equal("ćw. 4 (wzorzec)", EXPECTED_TABLE,
                () -> solution4(List.of("Kawa", "Czekolada gorzka", "Lampka"), List.of(64.99, 7.49, 129.0)));
        Check.summary();
        // WYNIK: ✔ OK    ćw. 1 (wzorzec)
        // WYNIK: ✔ OK    ćw. 2 (wzorzec)
        // WYNIK: ✔ OK    ćw. 3 (wzorzec)
        // WYNIK: ✔ OK    ćw. 4 (wzorzec)
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    private static final String EXPECTED_TABLE = String.join("\n",
            "Kawa             |     64.99",
            "Czekolada gorzka |      7.49",
            "Lampka           |    129.00",
            "SUMA             |    201.48");

    /**
     * ĆWICZENIE 1 (łatwe): zbuduj wiersz paragonu: nazwa wyrównana do LEWEJ w 10 znakach, ilość w 3 znakach,
     * tekst " x ", cena w 8 znakach z dwoma miejscami po kropce. Użyj Locale.ROOT.
     * Przykład: ("Kawa", 2, 64.99) → "Kawa        2 x    64.99".
     * Podpowiedź: String.format(Locale.ROOT, "%-10s%3d x %8.2f", ...).
     */
    static String exercise1(String name, int quantity, double price) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ na formatowanie. Kwota po polsku: separator tysięcy, przecinek dziesiętny,
     * dwa miejsca, " zł" na końcu, ZWYKŁE spacje (nie twarde). Zaokrąglenie HALF_UP (to robi %f dla BigDecimal).
     * <pre>{@code
     * // PRZED:
     * String text = amount.setScale(2, RoundingMode.HALF_UP).toString().replace('.', ',') + " zł";
     * // (brak separatora tysięcy: "1234,50 zł" — zły wynik)
     * }</pre>
     * Podpowiedź: String.format(pl-PL, "%,.2f zł", ...) i plain(...) na wyniku.
     */
    static String exercise2(BigDecimal amount) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zamień tablicę bajtów na zapis szesnastkowy rozdzielony dwukropkami, każdy bajt
     * dwoma WIELKIMI znakami: {10, -1, 0} → "0A:FF:00".
     * Podpowiedź: %02X z pojedynczym bajtem (typ Byte pisze 2 znaki także dla ujemnych); sklej przez StringJoiner
     * albo String.join(":", ...).
     */
    static String exercise3(byte[] data) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): tabela z dopasowaną szerokością. Kolumna nazw ma szerokość najdłuższej nazwy
     * potem " | " i kwota w 9 znakach z 2 miejscami po kropce; ostatni wiersz "SUMA" z sumą kwot. Wiersze
     * rozdziel znakiem \n (bez znaku na końcu). Użyj Locale.ROOT.
     * Podpowiedź: szerokość policz strumieniem (mapToInt(String::length).max()), format sklej z "%-" + width + "s".
     */
    static String exercise4(List<String> names, List<Double> amounts) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String name, int quantity, double price) {
        return String.format(Locale.ROOT, "%-10s%3d x %8.2f", name, quantity, price);
    }

    static String solution2(BigDecimal amount) {
        return plain(String.format(POLISH, "%,.2f zł", amount));
    }

    static String solution3(byte[] data) {
        List<String> parts = new java.util.ArrayList<>();
        for (byte b : data) {
            parts.add(String.format(Locale.ROOT, "%02X", b));
        }
        return String.join(":", parts);
    }

    static String solution4(List<String> names, List<Double> amounts) {
        int width = names.stream().mapToInt(String::length).max().orElse(0);
        String rowFormat = "%-" + width + "s | %9.2f";
        List<String> rows = new java.util.ArrayList<>();
        double sum = 0;
        for (int i = 0; i < names.size(); i++) {
            rows.add(String.format(Locale.ROOT, rowFormat, names.get(i), amounts.get(i)));
            sum += amounts.get(i);
        }
        rows.add(String.format(Locale.ROOT, rowFormat, "SUMA", sum));
        return String.join("\n", rows);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. % — początek; - — wyrównanie do lewej; 12 — szerokość minimalna 12 znaków; .3 — precyzja: dla %s obcina tekst
     *      do 3 znaków; s — konwersja tekstowa. Efekt: 3 znaki tekstu, dopełnione spacjami do 12, tekst przy lewej krawędzi.
     *   2. -003.142 — trzy miejsca po kropce, szerokość 8 uzupełniona zerami po znaku minus.
     *   3. "b a a" — %2$s = b, %1$s = a, a zwykłe %s ma własny licznik i bierze PIERWSZY argument (a).
     *   4. IllegalFormatConversionException (d != java.lang.Double). Napraw: %.0f / %f dla double albo (int) 3.0 dla %d.
     *   5. abc|  2.3|true — precyzja .3 obcina napis do "abc"; 2.25 → HALF_UP daje 2.3; %b z napisem "false" daje true
     *      (każda wartość inna niż Boolean i null to true).
     *   6. Polski separator tysięcy to twarda spacja U+00A0 (lub U+202F), a nie zwykła spacja (U+0020).
     *      Napraw: replace twardej spacji na zwykłą albo format z Locale.ROOT (jeśli wynik jest dla programu).
     *   7. double 1.005 formatuje się z krótkiego zapisu "1.005" i HALF_UP daje 1.01; BigDecimal(1.005) trzyma
     *      dokładną wartość binarną 1.00499999999999989..., więc zaokrągla w dół do 1.00.
     *   8. "Rabat: 15%" — % na końcu to niepełny specyfikator (wyjątek): pisz 15%%. Drugi tekst: brakuje argumentu
     *      dla drugiego %s (MissingFormatArgumentException); podaj dwa argumenty albo usuń jeden %s.
     */
    // </editor-fold>
}
