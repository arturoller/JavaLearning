package t31_jdk_toolbox;

import helpers.Check;
import java.math.BigDecimal;
import java.text.Bidi;
import java.text.ChoiceFormat;
import java.text.CollationKey;
import java.text.Collator;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.ResourceBundle;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Internacjonalizacja (i18n) — język, liczby, daty, komunikaty, liczba mnoga, sortowanie
 *        (internationalization = umiędzynarodowienie; locale = ustawienia regionalne;
 *         resource bundle = zestaw zasobów tekstowych; collator = porównywacz tekstów)
 *
 * W SKRÓCIE:
 *   Ten sam program może mówić do użytkownika po polsku, niemiecku i angielsku, a liczby,
 *   daty i waluty zapisywać tak, jak ludzie w danym kraju są do tego przyzwyczajeni.
 *   Klucz do wszystkiego to obiekt Locale: komunikaty wybierasz z pakietu zasobów,
 *   a liczby i daty formatujesz formatterami z JEDNYM, JAWNIE podanym Locale.
 *
 * ANALOGIA:
 *   Locale to "paszport językowy" użytkownika. Recepcjonista hotelowy (Twój program) patrzy
 *   w paszport i wita gościa po jego polsku, wystawia rachunek w jego walucie i zapisuje datę
 *   po jego zwyczaju. Gdy nie ma słowa w języku gościa, sięga po język ogólny (angielski).
 *
 * JAK TO DZIAŁA:
 *   Locale = język [+ kraj]:       pl, pl-PL, de-AT, en-US   (znacznik języka: "pl-PL")
 *   Liczby:   1234567,891 → pl: "1 234 567,891" | en-US: "1,234,567.891" | de-DE: "1.234.567,891"
 *   Waluta:   pl-PL: "1 234,50 zł" | en-US: "$1,234.50" | de-DE: "1.234,50 €"
 *   Daty:     pl: "15 marca 2026" | en-US: "March 15, 2026" | de-DE: "15. März 2026"
 *   Pakiet zasobów: ten sam klucz, różne teksty. Łańcuch szukania dla pl-PL:
 *       Messages_pl_PL  →  Messages_pl  →  Messages (bazowy)
 *   Wiadomość z parametrami:  MessageFormat "Masz {0} plików" + argumenty.
 *   Liczba mnoga w polskim ma TRZY formy (1 plik, 2 pliki, 5 plików) — wymaga własnej reguły.
 *
 * SŁÓWKA:
 *   locale = ustawienia regionalne; language = język; country = kraj; bundle = pakiet;
 *   fallback = wybór zapasowy; placeholder = miejsce na argument; plural = liczba mnoga;
 *   collation = reguły porządkowania; normalize = sprowadź do postaci znormalizowanej;
 *   right-to-left (RTL) = pisane od prawej do lewej.
 *
 * ZOBACZ TEŻ: t17_datetime/DateTime06FormatterAdvanced (formatery dat),
 *   t15_numbers/Numbers01BigDecimal (pieniądze jako BigDecimal),
 *   t12_collections/Collections07ComparableComparator (komparatory),
 *   t18_io_files/Io12Charsets (kodowanie znaków), t04_strings/Strings06CharUnicode (Unicode)
 * </pre>
 */
public class Toolbox03I18n {

    /** Polskie ustawienia regionalne — jawnie, żeby wynik nie zależał od komputera. */
    private static final Locale PL = Locale.forLanguageTag("pl-PL");

    public static void main(String[] args) throws ParseException {
        title("Toolbox03 — internacjonalizacja (i18n)");

        localeBasics();       // locale basics = podstawy Locale
        numbersAndCurrency(); // numbers and currency = liczby i waluty
        datesAndTimes();      // dates and times = daty i godziny
        resourceBundles();    // resource bundles = pakiety zasobów
        messageFormat();      // message format = format komunikatów
        polishPlurals();      // Polish plurals = polska liczba mnoga
        sortingWithCollator(); // sorting with collator = sortowanie z Collator
        unicodeText();        // Unicode text = tekst w Unicode
        wholeMessage();       // whole message = cały komunikat razem
        exercises();          // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. LOCALE
    // =================================================================================================

    /**
     * 1. {@code Locale} (ustawienia regionalne) to język i opcjonalnie kraj. Tworzymy go ze znacznika
     * języka ({@code Locale.forLanguageTag("pl-PL")}). Zwróć uwagę na myślnik: to nie podkreślnik.
     */
    static void localeBasics() {
        section("1. Locale: język, kraj, znacznik");

        show("getLanguage (język)", PL.getLanguage());
        // WYNIK: getLanguage (język) → pl
        show("getCountry (kraj)", PL.getCountry());
        // WYNIK: getCountry (kraj) → PL
        show("toLanguageTag (znacznik)", PL.toLanguageTag());
        // WYNIK: toLanguageTag (znacznik) → pl-PL
        show("toString (stary zapis)", PL);
        // WYNIK: toString (stary zapis) → pl_PL
        show("nazwa po polsku", PL.getDisplayName(PL));
        // WYNIK: nazwa po polsku → polski (Polska)
        show("nazwa po angielsku", PL.getDisplayName(Locale.ENGLISH));
        // WYNIK: nazwa po angielsku → Polish (Poland)
        show("kraj de-AT po polsku", Locale.forLanguageTag("de-AT").getDisplayCountry(PL));
        // WYNIK: kraj de-AT po polsku → Austria
        show("sam język: kraj pusty", "'" + Locale.forLanguageTag("pl").getCountry() + "'");
        // WYNIK: sam język: kraj pusty → ''
        show("stała Locale.US", Locale.US.toLanguageTag());
        // WYNIK: stała Locale.US → en-US
        // Locale.ROOT to "brak języka" — neutralny zapis, bez zwyczajów żadnego kraju.
        // Używamy go do zapisu przeznaczonego dla maszyn (pliki, protokoły, klucze).
        show("Locale.ROOT, liczba z kropką", String.format(Locale.ROOT, "%.2f", 1234.5));
        // WYNIK: Locale.ROOT, liczba z kropką → 1234.50
        show("Locale.GERMANY, ta sama liczba", String.format(Locale.GERMANY, "%.2f", 1234.5));
        // WYNIK: Locale.GERMANY, ta sama liczba → 1234,50
        show("pl-PL z grupowaniem", plain(String.format(PL, "%,.2f", 1234567.891)));
        // WYNIK: pl-PL z grupowaniem → 1 234 567,89

        // PUŁAPKA: znacznik z podkreślnikiem — forLanguageTag("pl_PL") — nie jest błędem,
        // tylko daje PUSTE Locale. Dlaczego: znacznik języka (standard BCP 47) używa myślnika;
        // podkreślnik to stary zapis z toString(). Metoda nie rzuca wyjątku, więc pomyłka cicho
        // zamienia Twoje ustawienia w "brak języka".
        Locale wrong = Locale.forLanguageTag("pl_PL");
        show("forLanguageTag(\"pl_PL\") → język", "'" + wrong.getLanguage() + "'");
        // WYNIK: forLanguageTag("pl_PL") → język → ''

        // PUŁAPKA: metody BEZ Locale używają ustawienia domyślnego komputera.
        // Dlaczego: String.format("%.2f", x), toUpperCase(), NumberFormat.getInstance() i
        // MessageFormat.format(...) bez Locale dadzą inny wynik na komputerze z polskim,
        // a inny z angielskim systemem: "1234,50" albo "1234.50". Program, który działa u Ciebie,
        // zepsuje plik CSV czy JSON na serwerze z innymi ustawieniami. Zawsze podawaj Locale.

        // Bardzo znana pułapka: tureckie "i". W języku tureckim wielka litera od "i" to "İ" (z kropką),
        // a mała od "I" to "ı" (bez kropki). toUpperCase()/toLowerCase() bez Locale używają
        // języka komputera, więc na tureckim systemie "FILE".toLowerCase() daje "fıle".
        Locale tr = Locale.forLanguageTag("tr-TR");
        show("\"title\".toUpperCase(tr)", "title".toUpperCase(tr));
        // WYNIK: "title".toUpperCase(tr) → TİTLE
        show("\"FILE\".toLowerCase(tr)", "FILE".toLowerCase(tr));
        // WYNIK: "FILE".toLowerCase(tr) → fıle
        show("\"FILE\".toLowerCase(tr) równa się \"file\"", "FILE".toLowerCase(tr).equals("file"));
        // WYNIK: "FILE".toLowerCase(tr) równa się "file" → false
        show("\"FILE\".toLowerCase(Locale.ROOT)", "FILE".toLowerCase(Locale.ROOT));
        // WYNIK: "FILE".toLowerCase(Locale.ROOT) → file

        // DOBRA PRAKTYKA: dla nazw poleceń, kluczy, rozszerzeń plików, nagłówków i innych
        // "tekstów dla maszyn" — toLowerCase(Locale.ROOT) i String.format(Locale.ROOT, ...).
        // Dla tekstów DLA LUDZI — Locale użytkownika (z żądania, profilu, ustawień aplikacji).
        // Dlaczego: to rozdziela dwa światy i wynik nie zależy od ustawień komputera.

        // Uwaga o nowszych JDK: od Java 19 konstruktor new Locale("pl", "PL") jest przestarzały
        // (deprecated) na rzecz Locale.of("pl", "PL"). W JDK 17 oba istnieją; forLanguageTag
        // działa wszędzie i dlatego używamy go w kursie.
    }

    // =================================================================================================
    // 2. LICZBY I WALUTY
    // =================================================================================================

    /**
     * 2. {@code NumberFormat} (format liczb) zapisuje i odczytuje liczby, waluty i procenty
     * według zwyczajów danego kraju. Polska grupuje tysiące twardą spacją i ma przecinek dziesiętny.
     */
    static void numbersAndCurrency() throws ParseException {
        section("2. NumberFormat: liczby, waluty, procenty");

        Locale us = Locale.US;
        Locale de = Locale.GERMANY;
        double number = 1234567.891;
        show("liczba pl-PL", plain(NumberFormat.getInstance(PL).format(number)));
        // WYNIK: liczba pl-PL → 1 234 567,891
        show("liczba en-US", NumberFormat.getInstance(us).format(number));
        // WYNIK: liczba en-US → 1,234,567.891
        show("liczba de-DE", NumberFormat.getInstance(de).format(number));
        // WYNIK: liczba de-DE → 1.234.567,891
        show("waluta pl-PL", plain(NumberFormat.getCurrencyInstance(PL).format(1234.5)));
        // WYNIK: waluta pl-PL → 1 234,50 zł
        show("waluta en-US", NumberFormat.getCurrencyInstance(us).format(1234.5));
        // WYNIK: waluta en-US → $1,234.50
        show("waluta de-DE", plain(NumberFormat.getCurrencyInstance(de).format(1234.5)));
        // WYNIK: waluta de-DE → 1.234,50 €
        show("procent pl-PL", plain(NumberFormat.getPercentInstance(PL).format(0.256)));
        // WYNIK: procent pl-PL → 26%
        show("procent de-DE (spacja przed %)", plain(NumberFormat.getPercentInstance(de).format(0.256)));
        // WYNIK: procent de-DE (spacja przed %) → 26 %

        // PUŁAPKA: polski format zawiera TWARDE SPACJE (znaki U+00A0 lub U+202F), a nie zwykłe.
        // Dlaczego: twarda spacja nie pozwala zawinąć liczby w środku ("1 | 234"), więc wygląda
        // dobrze na ekranie. Ale: porównanie z tekstem pisanym ręcznie ("1 234,50 zł" ze zwykłą
        // spacją) zawiedzie, a w logu lub CSV znaki wyglądają jak spacje i dają zagadkowe błędy.
        // Dlatego w tej lekcji przed drukiem zamieniamy je na zwykłą spację (metoda plain).
        String raw = NumberFormat.getInstance(PL).format(1234.5);
        show("zawiera twardą spację", raw.indexOf(0x00A0) >= 0 || raw.indexOf(0x202F) >= 0);
        // WYNIK: zawiera twardą spację → true
        show("po plain() zwykła spacja", plain(raw));
        // WYNIK: po plain() zwykła spacja → 1 234,5

        // PUŁAPKA: odczyt tekstu liczby musi używać TEGO SAMEGO Locale i znaków jak formatowanie.
        // Dlaczego: parse zatrzymuje się na pierwszym nieznanym znaku i NIE zgłasza błędu,
        // jeśli udało mu się przeczytać początek — zwykła spacja nie jest separatorem tysięcy.
        NumberFormat parser = NumberFormat.getInstance(PL);
        show("parse z twardą spacją", parser.parse("1" + (char) 0x00A0 + "234,5"));
        // WYNIK: parse z twardą spacją → 1234.5
        show("parse ze zwykłą spacją (zatrzymał się na spacji)", parser.parse("1 234,5"));
        // WYNIK: parse ze zwykłą spacją (zatrzymał się na spacji) → 1
        // Dane dla maszyn (JSON, CSV wymieniane między systemami) zapisuj bez Locale-specyficznych
        // znaków: kropka dziesiętna, bez separatora tysięcy (String.format(Locale.ROOT, ...)).

        // PUŁAPKA: NumberFormat jest ZMIENNY i NIE jest bezpieczny wątkowo.
        // Dlaczego: setMaximumFractionDigits itp. zmieniają obiekt, a format() używa wewnętrznego
        // bufora. Trzymany w polu statycznym i używany z wielu wątków da pomieszane wyniki.
        // Twórz nowy obiekt na użycie albo użyj DateTimeFormatter dla dat (ten jest niezmienny).

        // Zaokrąglanie: NumberFormat domyślnie zaokrągla "do parzystej" (HALF_EVEN).
        NumberFormat integers = NumberFormat.getIntegerInstance(PL);
        show("2.5 → liczba całkowita", integers.format(2.5));
        // WYNIK: 2.5 → liczba całkowita → 2
        show("3.5 → liczba całkowita", integers.format(3.5));
        // WYNIK: 3.5 → liczba całkowita → 4
        // Dla pieniędzy użyj BigDecimal i jawnego RoundingMode (t15_numbers/Numbers01BigDecimal).

        // WALUTA: Currency (waluta) zna kod ISO, symbol i liczbę miejsc po przecinku.
        Currency pln = Currency.getInstance("PLN");
        show("kod waluty", pln.getCurrencyCode());
        // WYNIK: kod waluty → PLN
        show("symbol w polskim", pln.getSymbol(PL));
        // WYNIK: symbol w polskim → zł
        show("symbol w angielskim (USA)", pln.getSymbol(us));
        // WYNIK: symbol w angielskim (USA) → PLN
        show("nazwa po polsku", pln.getDisplayName(PL));
        // WYNIK: nazwa po polsku → złoty polski
        show("domyślne miejsca po przecinku", pln.getDefaultFractionDigits());
        // WYNIK: domyślne miejsca po przecinku → 2

        // PUŁAPKA: getCurrencyInstance(PL) formatuje w walucie KRAJU (PLN), niezależnie od tego,
        // czy kwota jest w złotych. Dlaczego: Locale wybiera zwyczaje zapisu, a walutę bierze
        // z kraju. Kwota 100 EUR pokazana polskiemu użytkownikowi bez setCurrency wyglądałaby
        // jak "100,00 zł" — to poważny błąd. Walutę kwoty ustaw jawnie.
        NumberFormat euro = NumberFormat.getCurrencyInstance(PL);
        show("100 EUR bez setCurrency", plain(euro.format(100)));
        // WYNIK: 100 EUR bez setCurrency → 100,00 zł
        euro.setCurrency(Currency.getInstance("EUR"));
        show("100 EUR z setCurrency", plain(euro.format(100)));
        // WYNIK: 100 EUR z setCurrency → 100,00 €

        // DOBRA PRAKTYKA: kwotę trzymaj jako BigDecimal wraz z kodem waluty (np. rekord Money),
        // a w warstwie wyświetlania formatuj ją z Locale użytkownika i JEGO walutą kwoty.
        BigDecimal price = new BigDecimal("1234.50");
        show("BigDecimal w formacie waluty", plain(NumberFormat.getCurrencyInstance(PL).format(price)));
        // WYNIK: BigDecimal w formacie waluty → 1 234,50 zł
    }

    // =================================================================================================
    // 3. DATY I GODZINY
    // =================================================================================================

    /**
     * 3. {@code DateTimeFormatter.ofLocalizedDate(styl)} wybiera zapis daty zgodny z krajem.
     * Nazwy miesięcy i dni też są tłumaczone (t17_datetime/DateTime06FormatterAdvanced).
     */
    static void datesAndTimes() {
        section("3. Daty i godziny według regionu");

        LocalDate date = LocalDate.of(2026, 3, 15); // niedziela
        for (Locale locale : List.of(PL, Locale.US, Locale.GERMANY)) {
            String longText = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(date);
            String shortText = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale).format(date);
            show(locale.toLanguageTag() + " LONG / SHORT", longText + " / " + shortText);
        }
        // WYNIK: pl-PL LONG / SHORT → 15 marca 2026 / 15.03.2026
        // WYNIK: en-US LONG / SHORT → March 15, 2026 / 3/15/26
        // WYNIK: de-DE LONG / SHORT → 15. März 2026 / 15.03.26
        show("pl-PL FULL", DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(PL).format(date));
        // WYNIK: pl-PL FULL → niedziela, 15 marca 2026
        LocalTime time = LocalTime.of(14, 5);
        show("godzina pl-PL", plain(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(PL).format(time)));
        // WYNIK: godzina pl-PL → 14:05
        show("godzina en-US", plain(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.US).format(time)));
        // WYNIK: godzina en-US → 2:05 PM

        // WZORCE z Locale: nazwy miesięcy i dni zależą od języka.
        show("EEEE, d MMMM yyyy (pl)", DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", PL).format(date));
        // WYNIK: EEEE, d MMMM yyyy (pl) → niedziela, 15 marca 2026
        show("EEEE, d MMMM yyyy (en)", DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.US).format(date));
        // WYNIK: EEEE, d MMMM yyyy (en) → Sunday, 15 March 2026
        // W polskim miesiąc ma dwie formy: w dacie "15 marca" (dopełniacz) i samodzielnie "marzec".
        // Wzorzec MMMM daje formę z daty, a LLLL — formę samodzielną (standalone).
        show("MMMM w dacie", DateTimeFormatter.ofPattern("d MMMM", PL).format(date));
        // WYNIK: MMMM w dacie → 15 marca
        show("LLLL samodzielnie", DateTimeFormatter.ofPattern("LLLL yyyy", PL).format(date));
        // WYNIK: LLLL samodzielnie → marzec 2026
        show("Month.getDisplayName(FULL)", Month.MARCH.getDisplayName(TextStyle.FULL, PL));
        // WYNIK: Month.getDisplayName(FULL) → marca
        show("Month.getDisplayName(FULL_STANDALONE)", Month.MARCH.getDisplayName(TextStyle.FULL_STANDALONE, PL));
        // WYNIK: Month.getDisplayName(FULL_STANDALONE) → marzec
        show("DayOfWeek.getDisplayName", DayOfWeek.SUNDAY.getDisplayName(TextStyle.FULL, PL));
        // WYNIK: DayOfWeek.getDisplayName → niedziela
        // PUŁAPKA: złożenie daty ręcznie z nazwy miesiąca ("d " + nazwa + " yyyy") w polskim daje
        // "15 marzec 2026", bo nazwa samodzielna to nie ta forma co w dacie. Użyj wzorca z
        // MMMM albo gotowego stylu LONG.

        // Tydzień też zależy od kraju: w Polsce pierwszy dzień tygodnia to poniedziałek, w USA niedziela.
        show("pierwszy dzień tygodnia w Polsce", WeekFields.of(PL).getFirstDayOfWeek());
        // WYNIK: pierwszy dzień tygodnia w Polsce → MONDAY
        show("pierwszy dzień tygodnia w USA", WeekFields.of(Locale.US).getFirstDayOfWeek());
        // WYNIK: pierwszy dzień tygodnia w USA → SUNDAY
        // PUŁAPKA: wzorzec YYYY (duże litery) to "rok tygodniowy", a nie rok kalendarzowy yyyy.
        // Dlaczego: 31 grudnia 2026 w USA należy już do pierwszego tygodnia 2027 roku, więc YYYY
        // pokaże 2027. W Polsce ten dzień należy jeszcze do 2026. Do zwykłych dat używaj yyyy.
        LocalDate newYearsEve = LocalDate.of(2026, 12, 31);
        show("YYYY-MM-dd w USA", DateTimeFormatter.ofPattern("YYYY-MM-dd", Locale.US).format(newYearsEve));
        // WYNIK: YYYY-MM-dd w USA → 2027-12-31
        show("YYYY-MM-dd w Polsce", DateTimeFormatter.ofPattern("YYYY-MM-dd", PL).format(newYearsEve));
        // WYNIK: YYYY-MM-dd w Polsce → 2026-12-31
        show("yyyy-MM-dd (poprawnie)", DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US).format(newYearsEve));
        // WYNIK: yyyy-MM-dd (poprawnie) → 2026-12-31

        // DOBRA PRAKTYKA: do wymiany danych między systemami zawsze ISO-8601 (2026-03-15), a styl
        // lokalny tylko do wyświetlania człowiekowi. Dlaczego: ISO jest jednoznaczny — "3/4/26"
        // to 3 kwietnia dla Polaka i 4 marca dla Amerykanina.
    }

    // =================================================================================================
    // 4. PAKIETY ZASOBÓW (RESOURCE BUNDLE)
    // =================================================================================================

    /**
     * Pakiet bazowy (angielski) — używany, gdy nie ma tekstu w języku użytkownika.
     * W prawdziwym programie takie pakiety to pliki {@code messages.properties},
     * {@code messages_pl.properties} i tak dalej. Tu: klasy, żeby lekcja była jednym plikiem.
     * ListResourceBundle (lista zasobów) zwraca tablicę par {klucz, wartość}.
     * Klasa musi być publiczna i statyczna, bo ResourceBundle tworzy ją przez refleksję.
     */
    public static class Messages extends ListResourceBundle {
        @Override
        protected Object[][] getContents() {
            return new Object[][] {
                {"greeting", "Hello"},
                {"farewell", "Goodbye"},
                {"onlyInBase", "this key exists only in the base bundle"},
                {"postcode", "postcode format depends on the country"},
                {"items.one", "{0} item"},
                {"items.other", "{0} items"},
                {"summary", "{0}, order of {1}: {2} for {3}."},
            };
        }
    }

    /** Pakiet polski: nadpisuje to, co ma polską wersję; reszta pochodzi z bazowego. */
    public static class Messages_pl extends ListResourceBundle {
        @Override
        protected Object[][] getContents() {
            return new Object[][] {
                {"greeting", "Cześć"},
                {"farewell", "Do widzenia"},
                {"items.one", "{0} pozycja"},
                {"items.few", "{0} pozycje"},
                {"items.many", "{0} pozycji"},
                {"summary", "{0}, zamówienie z dnia {1}: {2} na kwotę {3}."},
            };
        }
    }

    /** Pakiet "polski w Polsce": tylko różnice względem pakietu polskiego (tu: zwrot grzecznościowy). */
    public static class Messages_pl_PL extends ListResourceBundle {
        @Override
        protected Object[][] getContents() {
            return new Object[][] {
                {"greeting", "Dzień dobry"},
                {"postcode", "00-000"},
            };
        }
    }

    /** Pakiet niemiecki. */
    public static class Messages_de extends ListResourceBundle {
        @Override
        protected Object[][] getContents() {
            return new Object[][] {
                {"greeting", "Guten Tag"},
                {"farewell", "Auf Wiedersehen"},
                {"items.one", "{0} Position"},
                {"items.other", "{0} Positionen"},
                {"summary", "{0}, Bestellung vom {1}: {2} über {3}."},
            };
        }
    }

    /** Nazwa bazowa pakietu: pełna nazwa klasy z zagnieżdżeniem (znak $). */
    private static final String BASE = Messages.class.getName();

    /**
     * Pobiera pakiet BEZ szukania w języku komputera (no fallback control = kontrola bez wyboru
     * zapasowego). Dzięki temu wynik nie zależy od ustawień systemu, na którym uruchamiasz lekcję.
     */
    static ResourceBundle bundle(Locale locale) {
        ResourceBundle.clearCache(); // clearCache = wyczyść pamięć podręczną pakietów (tylko w demonstracji)
        return ResourceBundle.getBundle(BASE, locale,
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT));
    }

    /**
     * 4. {@code ResourceBundle.getBundle(nazwa, locale)} szuka pakietu od najbardziej szczegółowego
     * ({@code pl_PL}), przez ogólniejszy ({@code pl}), aż do bazowego. Klucz nieobecny w
     * pakiecie szczegółowym jest szukany u "rodziców".
     */
    static void resourceBundles() {
        section("4. ResourceBundle: łańcuch pakietów i klucze zapasowe");

        ResourceBundle plPL = bundle(PL);
        show("pl-PL: greeting (z pakietu pl_PL)", plPL.getString("greeting"));
        // WYNIK: pl-PL: greeting (z pakietu pl_PL) → Dzień dobry
        show("pl-PL: farewell (odziedziczony z pl)", plPL.getString("farewell"));
        // WYNIK: pl-PL: farewell (odziedziczony z pl) → Do widzenia
        show("pl-PL: onlyInBase (odziedziczony z bazowego)", plPL.getString("onlyInBase"));
        // WYNIK: pl-PL: onlyInBase (odziedziczony z bazowego) → this key exists only in the base bundle
        show("pl-PL: postcode", plPL.getString("postcode"));
        // WYNIK: pl-PL: postcode → 00-000
        show("pl-PL: który pakiet naprawdę odpowiedział", "'" + plPL.getLocale() + "'");
        // WYNIK: pl-PL: który pakiet naprawdę odpowiedział → 'pl_PL'

        ResourceBundle pl = bundle(Locale.forLanguageTag("pl"));
        show("pl: greeting", pl.getString("greeting"));
        // WYNIK: pl: greeting → Cześć
        show("pl: postcode (brak w pl → baza)", pl.getString("postcode"));
        // WYNIK: pl: postcode (brak w pl → baza) → postcode format depends on the country
        show("pl: pakiet", "'" + pl.getLocale() + "'");
        // WYNIK: pl: pakiet → 'pl'

        ResourceBundle deAT = bundle(Locale.forLanguageTag("de-AT"));
        show("de-AT: greeting (brak de_AT → de)", deAT.getString("greeting"));
        // WYNIK: de-AT: greeting (brak de_AT → de) → Guten Tag
        ResourceBundle fr = bundle(Locale.FRANCE);
        show("fr-FR: greeting (brak francuskiego → baza)", fr.getString("greeting"));
        // WYNIK: fr-FR: greeting (brak francuskiego → baza) → Hello
        show("fr-FR: pakiet (pusty = bazowy)", "'" + fr.getLocale() + "'");
        // WYNIK: fr-FR: pakiet (pusty = bazowy) → ''

        show("containsKey (zawiera klucz)", plPL.containsKey("onlyInBase") + " / " + plPL.containsKey("brak"));
        // WYNIK: containsKey (zawiera klucz) → true / false

        // BRAKUJĄCY KLUCZ i BRAKUJĄCY PAKIET to wyjątki (MissingResourceException).
        expectThrows("brakujący klucz", () -> plPL.getString("nie.ma.takiego"));
        // WYNIK: ✔ brakujący klucz → rzucono MissingResourceException: Can't find resource for bundle t31_jdk_toolbox.Toolbox03I18n$Messages, key nie.ma.takiego
        expectThrows("brakujący pakiet", () ->
                ResourceBundle.getBundle("nie.ma.takiego.Pakietu", PL,
                        ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT)));
        // WYNIK: ✔ brakujący pakiet → rzucono MissingResourceException: Can't find bundle for base name nie.ma.takiego.Pakietu, locale pl_PL
        // MissingResourceException jest NIESPRAWDZANY (unchecked): kompilator nie wymusi obsługi. Brakujące klucze
        // w ostatniej chwili (u użytkownika) to częsty błąd — dlatego testuj, że KAŻDY język
        // ma komplet kluczy (test porównujący zbiory kluczy pakietów).

        // PUŁAPKA: łańcuch zapasowy ZACZYNA od języka komputera.
        // Dlaczego: getBundle(nazwa, locale) bez specjalnej kontroli, gdy nie znajdzie pakietu dla
        // żądanego Locale, najpierw próbuje pakietu dla Locale.getDefault(), a dopiero potem
        // bazowego. Na komputerze z polskim systemem prośba o francuski zwróci POLSKI pakiet,
        // a na komputerze z niemieckim — niemiecki. Testy przechodzą u Ciebie, a u klienta
        // widać "dziwny" język. Pokazujemy to, ustawiając na chwilę domyślny język na niemiecki.
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            ResourceBundle.clearCache();
            String viaDefault = ResourceBundle.getBundle(BASE, Locale.FRANCE).getString("greeting");
            show("fr-FR przy domyślnym de-DE (zwykłe getBundle)", viaDefault);
            // WYNIK: fr-FR przy domyślnym de-DE (zwykłe getBundle) → Guten Tag
        } finally {
            Locale.setDefault(previous); // ZAWSZE przywracamy ustawienie (finally)
            ResourceBundle.clearCache();
        }
        show("fr-FR z kontrolą bez wyboru zapasowego", bundle(Locale.FRANCE).getString("greeting"));
        // WYNIK: fr-FR z kontrolą bez wyboru zapasowego → Hello

        // W prawdziwych aplikacjach: pliki messages_pl.properties w katalogu zasobów (resources).
        // Od Java 9 pliki .properties czyta się jako UTF-8 (JEP 226) — polskie litery wpisujesz
        // normalnie. W starszych wersjach (Java 8 i wcześniej) był ISO-8859-1 i trzeba było pisać
        // polskie litery jako sekwencje z odwrotnym ukośnikiem i literą u — stare tutoriale nadal o tym wspominają.
        // W Spring Boot ten sam mechanizm obsługuje MessageSource (spring.messages.basename),
        // a język bierze z nagłówka Accept-Language żądania HTTP (LocaleResolver).

        // DOBRA PRAKTYKA: klucze nazywaj hierarchicznie i opisowo (orders.summary, items.one),
        // a nie od tekstu ("Witaj"). Dlaczego: tekst się zmienia (poprawka literówki), a klucz musi
        // zostać taki sam we wszystkich językach.
        // DOBRA PRAKTYKA: nie sklejaj zdań z kawałków ("Masz " + n + " plików") — kolejność słów
        // bywa inna w innym języku. Cały szablon zdania trzymaj w pakiecie (sekcja 5).
    }

    // =================================================================================================
    // 5. MESSAGEFORMAT
    // =================================================================================================

    /**
     * 5. {@code MessageFormat} wstawia argumenty w miejsca {@code {0}}, {@code {1}}... szablonu.
     * Liczby formatuje według Locale. Zawsze twórz go z jawnym Locale:
     * {@code new MessageFormat(szablon, locale)}.
     */
    static void messageFormat() {
        section("5. MessageFormat: argumenty, formaty i apostrofy");

        String pattern = "Witaj, {0}! Masz {1} nowych wiadomości ({2,number,percent} przeczytanych).";
        MessageFormat format = new MessageFormat(pattern, PL); // pattern = szablon
        String text = format.format(new Object[] {"Ala", 1234, 0.5});
        show("szablon z argumentami", plain(text));
        // WYNIK: szablon z argumentami → Witaj, Ala! Masz 1 234 nowych wiadomości (50% przeczytanych).

        // Formaty w nawiasach klamrowych: {indeks,typ,styl}
        show("{0,number,integer}", plain(new MessageFormat("{0,number,integer}", PL).format(new Object[] {1234567.89})));
        // WYNIK: {0,number,integer} → 1 234 568
        show("{0,number,#,##0.00}", plain(new MessageFormat("{0,number,#,##0.00}", PL).format(new Object[] {1234.5})));
        // WYNIK: {0,number,#,##0.00} → 1 234,50
        show("{0,number,currency}", plain(new MessageFormat("{0,number,currency}", PL).format(new Object[] {1234.5})));
        // WYNIK: {0,number,currency} → 1 234,50 zł
        show("tak samo, ale en-US", new MessageFormat("{0,number,#,##0.00}", Locale.US).format(new Object[] {1234.5}));
        // WYNIK: tak samo, ale en-US → 1,234.50

        // PUŁAPKA: liczby w {n} są formatowane jak liczby — także numery, które liczbami nie są.
        // Dlaczego: Integer 1234567 to dla MessageFormat zwykła liczba, więc dostaje separator tysięcy.
        // Numer zamówienia 1234567 wyświetli się jako "1 234 567" i nie da się go skopiować.
        show("numer jako liczba", plain(new MessageFormat("Zamówienie nr {0}", PL).format(new Object[] {1234567})));
        // WYNIK: numer jako liczba → Zamówienie nr 1 234 567
        show("numer jako tekst", new MessageFormat("Zamówienie nr {0}", PL).format(new Object[] {String.valueOf(1234567)}));
        // WYNIK: numer jako tekst → Zamówienie nr 1234567
        show("numer z formatem #", new MessageFormat("Zamówienie nr {0,number,#}", PL).format(new Object[] {1234567}));
        // WYNIK: numer z formatem # → Zamówienie nr 1234567

        // PUŁAPKA: apostrof (') w szablonie zaczyna cytowanie — znaki do końca (albo do następnego
        // apostrofu) są wypisywane dosłownie, więc {0} PRZESTAJE być argumentem.
        // Dlaczego: w MessageFormat apostrof jest znakiem specjalnym. W polskim zdarza się
        // w cudzych nazwach (O'Brien, d'Artagnan) i w skrótach angielskich (it's).
        show("It's {0}", MessageFormat.format("It's {0}", "x"));
        // WYNIK: It's {0} → Its {0}
        show("It''s {0}", MessageFormat.format("It''s {0}", "x"));
        // WYNIK: It''s {0} → It's x
        show("literalne nawiasy: '{'0'}' {0}", MessageFormat.format("'{'0'}' {0}", "x"));
        // WYNIK: literalne nawiasy: '{'0'}' {0} → {0} x
        // Zasada: pojedynczy apostrof zamień na podwójny (''), a literalny nawias klamrowy ujmij
        // w apostrofy ('{'). Gdy argument zawiera apostrof (np. imię O'Brien), nic nie trzeba robić
        // — specjalny jest apostrof w SZABLONIE, nie w argumencie.
        show("argument z apostrofem", MessageFormat.format("Witaj, {0}!", "O'Brien"));
        // WYNIK: argument z apostrofem → Witaj, O'Brien!

        // PUŁAPKA: za mało argumentów — brakujący zostaje w tekście jako {1}; za dużo — są ignorowane.
        // Dlaczego: MessageFormat nie sprawdza zgodności, więc literówka w indeksie widoczna jest
        // dopiero u użytkownika. Pokryj szablony testem.
        show("za mało argumentów", MessageFormat.format("{0} i {1}", "a"));
        // WYNIK: za mało argumentów → a i {1}
        show("za dużo argumentów", MessageFormat.format("{0}", "a", "b", "c"));
        // WYNIK: za dużo argumentów → a

        // PUŁAPKA: MessageFormat zna tylko java.util.Date i liczby, NIE zna java.time (LocalDate).
        // Dlaczego: jest starszy niż java.time. Do dat użyj DateTimeFormatter z Locale, a gotowy
        // tekst wstaw jako argument String (tak robimy w sekcji 8). Daty java.util.Date w
        // MessageFormat używają strefy czasowej komputera — kolejny powód, by ich unikać.

        // DOBRA PRAKTYKA: argumenty przygotuj wcześniej (data → tekst, kwota → tekst) i przekaż jako
        // String. Dlaczego: nad każdym formatem masz jawną kontrolę i szablon pozostaje prosty.
    }

    // =================================================================================================
    // 6. POLSKA LICZBA MNOGA
    // =================================================================================================

    /**
     * 6. W angielskim wystarczą dwie formy ("1 file", "2 files"). W polskim są trzy, a dla
     * liczb ułamkowych czwarta. {@code ChoiceFormat} umie tylko przedziały, więc nie wyrazi reguły
     * "kończy się na 2-4, ale nie na 12-14".
     */
    static void polishPlurals() {
        section("6. Liczba mnoga: ChoiceFormat kontra reguła polska");

        // ChoiceFormat: "granica#tekst|granica#tekst". '#' = "od tej wartości w górę",
        // '<' = "powyżej tej wartości".
        ChoiceFormat choice = new ChoiceFormat("0#brak plików|1#jeden plik|1<więcej niż jeden plik");
        show("ChoiceFormat(0)", choice.format(0));
        // WYNIK: ChoiceFormat(0) → brak plików
        show("ChoiceFormat(1)", choice.format(1));
        // WYNIK: ChoiceFormat(1) → jeden plik
        show("ChoiceFormat(5)", choice.format(5));
        // WYNIK: ChoiceFormat(5) → więcej niż jeden plik

        // W MessageFormat: {0,choice,...}, a w tekście może być zagnieżdżone {0}.
        MessageFormat withChoice = new MessageFormat("{0,choice,0#brak plików|1#jeden plik|1<{0} plików}", PL);
        List<String> wrong = new ArrayList<>();
        for (int n : new int[] {0, 1, 2, 5, 12, 22}) {
            wrong.add(withChoice.format(new Object[] {n}));
        }
        show("ChoiceFormat dla 0, 1, 2, 5, 12, 22", wrong);
        // WYNIK: ChoiceFormat dla 0, 1, 2, 5, 12, 22 → [brak plików, jeden plik, 2 plików, 5 plików, 12 plików, 22 plików]
        // Widać błąd: "2 plików" i "22 plików" — powinno być "2 pliki" i "22 pliki".
        // PUŁAPKA: ChoiceFormat ma jedną regułę "od progu w górę", a polska zależy od OSTATNICH
        // CYFR liczby (n mod 10 i n mod 100). Dla języków z taką regułą potrzebna jest osobna
        // funkcja (poniżej) albo biblioteka z regułami CLDR (np. ICU4J ze składnią plural).

        // REGUŁA POLSKA dla liczb całkowitych:
        //   1                                               → forma 1:  "1 plik"
        //   kończy się na 2, 3, 4, ale NIE na 12, 13, 14    → forma 2:  "2 pliki", "22 pliki"
        //   wszystko inne (0, 5-21, 25..., 101, 112)        → forma 3:  "5 plików", "12 plików"
        // Uwaga: 21 i 101 NIE są formą 1 — "21 plików", "101 plików" (forma 1 tylko dla samego 1).
        // Ułamki (1,5) mają czwartą formę: "1,5 pliku" — tu pomijamy.
        List<String> table = new ArrayList<>();
        for (long n : new long[] {0, 1, 2, 4, 5, 12, 14, 15, 21, 22, 24, 25, 101, 102, 112, 1000}) {
            table.add(n + " " + pluralPl(n, "plik", "pliki", "plików"));
        }
        show("poprawna odmiana", table);
        // WYNIK: poprawna odmiana → [0 plików, 1 plik, 2 pliki, 4 pliki, 5 plików, 12 plików, 14 plików, 15 plików, 21 plików, 22 pliki, 24 pliki, 25 plików, 101 plików, 102 pliki, 112 plików, 1000 plików]

        // Odmiana w pakiecie zasobów: jeden klucz na każdą formę, a regułę wybiera kod.
        ResourceBundle pl = bundle(Locale.forLanguageTag("pl"));
        List<String> items = new ArrayList<>();
        for (long n : new long[] {1, 2, 5, 22}) {
            items.add(itemsText(pl, Locale.forLanguageTag("pl"), n));
        }
        show("pozycje z pakietu (pl)", items);
        // WYNIK: pozycje z pakietu (pl) → [1 pozycja, 2 pozycje, 5 pozycji, 22 pozycje]
        ResourceBundle en = bundle(Locale.US);
        List<String> itemsEn = new ArrayList<>();
        for (long n : new long[] {1, 2, 5}) {
            itemsEn.add(itemsText(en, Locale.US, n));
        }
        show("pozycje z pakietu (en)", itemsEn);
        // WYNIK: pozycje z pakietu (en) → [1 item, 2 items, 5 items]

        // PUŁAPKA: wspólna "uniwersalna" reguła liczby mnogiej nie istnieje.
        // Dlaczego: japoński ma 1 formę, angielski 2, polski 3 (+ ułamki), rosyjski 3, arabski 6.
        // Reguła musi być przypisana do JĘZYKA (Locale), a nie wpisana na stałe w kodzie.
        // Spring MessageSource nie obsługuje liczby mnogiej — używa się kluczy na każdą formę
        // (jak wyżej) albo biblioteki ICU4J ze składnią:
        //   {0, plural, one{# plik} few{# pliki} many{# plików} other{# pliku}}

        // DOBRA PRAKTYKA: zdania typu "Znaleziono N plików" zawsze buduj przez formę mnogą wybraną
        // regułą języka — nigdy "plik(i)" ani "plików: N". Dlaczego: to wygląda nieprofesjonalnie
        // i jest typowym objawem źle przygotowanej i18n.
    }

    /** Wybiera polską formę rzeczownika dla liczby całkowitej (1 plik, 2 pliki, 5 plików). */
    static String pluralPl(long n, String one, String few, String many) {
        return switch (pluralCategory(PL, n)) { // switch jako wyrażenie (Java 14+)
            case "one" -> one;
            case "few" -> few;
            default -> many;
        };
    }

    /**
     * Kategoria liczby mnogiej wg zasad CLDR dla liczb całkowitych. Dla polskiego: one, few, many;
     * dla pozostałych języków uproszczenie: one albo other (tak jest w angielskim i niemieckim).
     */
    static String pluralCategory(Locale locale, long n) {
        if (!"pl".equals(locale.getLanguage())) {
            return n == 1 ? "one" : "other";
        }
        long abs = Math.abs(n); // abs = wartość bezwzględna
        long last = abs % 10;
        long last2 = abs % 100;
        if (abs == 1) {
            return "one";
        }
        if (last >= 2 && last <= 4 && !(last2 >= 12 && last2 <= 14)) {
            return "few";
        }
        return "many";
    }

    /** Tekst "N pozycji" z pakietu: klucz zależy od kategorii (items.one, items.few ...). */
    static String itemsText(ResourceBundle bundle, Locale locale, long n) {
        String key = "items." + pluralCategory(locale, n);
        return new MessageFormat(bundle.getString(key), locale).format(new Object[] {n});
    }

    // =================================================================================================
    // 7. SORTOWANIE: COLLATOR
    // =================================================================================================

    /**
     * 7. Zwykłe porównanie tekstów ({@code String.compareTo}) porównuje kody Unicode, więc litery
     * polskie (Ą, Ć, Ł, Ż...) lądują PO całym alfabecie łacińskim. {@code Collator} sortuje
     * według reguł języka (t12_collections/Collections07ComparableComparator).
     */
    static void sortingWithCollator() {
        section("7. Sortowanie po polsku: Collator");

        List<String> words = new ArrayList<>(List.of("Łódź", "Zebra", "Ącki", "Abba", "Żaba", "Ćma",
                "Cebula", "Zęby", "Źrebak", "Ola", "ola", "Ząb"));

        List<String> natural = new ArrayList<>(words);
        natural.sort(null); // null = kolejność naturalna (compareTo): kody Unicode
        show("String.compareTo (Unicode)", natural);
        // WYNIK: String.compareTo (Unicode) → [Abba, Cebula, Ola, Zebra, Ząb, Zęby, ola, Ącki, Ćma, Łódź, Źrebak, Żaba]

        Collator collator = Collator.getInstance(PL); // collator = porównywacz wg reguł języka
        List<String> polish = new ArrayList<>(words);
        polish.sort(collator); // Collator implementuje Comparator
        show("Collator pl-PL", polish);
        // WYNIK: Collator pl-PL → [Abba, Ącki, Cebula, Ćma, Łódź, ola, Ola, Ząb, Zebra, Zęby, Źrebak, Żaba]
        // PUŁAPKA: sortowanie samym compareTo (lub bazą danych z innym "collation") daje "Łódź"
        // po "Żabie". Dla użytkownika to błąd. Dlaczego: w Unicode litery Ą, Ć, Ę, Ł, Ń, Ó, Ś, Ź, Ż
        // mają kody większe niż Z. Collator wie, że w polskim "ą" idzie zaraz po "a", a "ł" po "l".

        // Siła porównania (strength = jak szczegółowo różnice mają znaczenie):
        //   PRIMARY   — tylko różne litery (a = A),
        //   SECONDARY — także akcenty,   TERTIARY — także wielkość liter (domyślnie).
        Collator primary = Collator.getInstance(PL);
        primary.setStrength(Collator.PRIMARY);
        show("PRIMARY: a ? A", primary.compare("a", "A"));
        // WYNIK: PRIMARY: a ? A → 0
        show("PRIMARY: a ? ą (ą to osobna litera)", Integer.signum(primary.compare("a", "ą")));
        // WYNIK: PRIMARY: a ? ą (ą to osobna litera) → -1
        show("TERTIARY: a ? A (małe przed dużą)", Integer.signum(collator.compare("a", "A")));
        // WYNIK: TERTIARY: a ? A (małe przed dużą) → -1
        show("Collator.equals przy sile PRIMARY", primary.equals("a", "A"));
        // WYNIK: Collator.equals przy sile PRIMARY → true
        // Z siłą PRIMARY sortowanie "bez względu na wielkość liter" jest jednym wywołaniem.
        // To uczciwsza alternatywa dla String.CASE_INSENSITIVE_ORDER, który nie zna reguł języka.

        // Wydajność: porównania Collator są wolne. Przy dużych listach (albo wielokrotnym
        // porównywaniu tych samych tekstów) policz raz CollationKey i porównuj klucze.
        CollationKey keyL = collator.getCollationKey("Łódź");
        CollationKey keyZ = collator.getCollationKey("Żaba");
        show("CollationKey: Łódź < Żaba", keyL.compareTo(keyZ) < 0);
        // WYNIK: CollationKey: Łódź < Żaba → true

        // PUŁAPKA: Collator (podobnie jak NumberFormat) jest zmienny — setStrength zmienia obiekt.
        // getInstance zwraca kopię, więc własnej instancji możesz ustawić siłę bezpiecznie,
        // ale nie współdziel jej między wątkami z różnymi ustawieniami.

        // DOBRA PRAKTYKA: listy wyświetlane użytkownikowi (nazwiska, miasta) sortuj Collatorem z
        // Locale użytkownika; klucze techniczne i identyfikatory — zwykłym porządkiem.
        // Dlaczego: człowiek oczekuje alfabetu swojego języka, maszyna — przewidywalności.
    }

    // =================================================================================================
    // 8. UNICODE: NORMALIZACJA, ASCII, KIERUNEK PISMA
    // =================================================================================================

    /**
     * 8. Ten sam napis może mieć różne zapisy w Unicode. Do porównań i do usuwania ogonków
     * potrzebna jest normalizacja ({@code java.text.Normalizer}).
     */
    static void unicodeText() {
        section("8. Unicode: normalizacja, usuwanie ogonków, pismo od prawej do lewej");

        String composed = "Łódź"; // forma złożona NFC: ó to jeden znak
        String decomposed = Normalizer.normalize(composed, Normalizer.Form.NFD); // NFD = rozłożona
        show("długość NFC", composed.length());
        // WYNIK: długość NFC → 4
        show("długość NFD (ó i ź rozłożone na literę + akcent)", decomposed.length());
        // WYNIK: długość NFD (ó i ź rozłożone na literę + akcent) → 6
        show("czy napisy są równe (equals)", composed.equals(decomposed));
        // WYNIK: czy napisy są równe (equals) → false
        show("równe po sprowadzeniu do NFC", composed.equals(Normalizer.normalize(decomposed, Normalizer.Form.NFC)));
        // WYNIK: równe po sprowadzeniu do NFC → true
        // PUŁAPKA: dwa napisy wyglądające identycznie mogą nie być równe. Dlaczego: litera "ó"
        // to albo jeden znak (U+00F3), albo "o" + łączący akcent (U+0301). Różne systemy
        // (np. nazwy plików na macOS, tekst wklejony z niektórych programów) dają różne formy.
        // Przed porównywaniem i zapisem do bazy normalizuj do NFC.
        // Zauważ: "Ł" (z kreską) NIE rozkłada się na literę i akcent — to samodzielna litera.

        show("bez ogonków (naiwnie)", stripAccentsNaive("Zażółć gęślą jaźń"));
        // WYNIK: bez ogonków (naiwnie) → Zazołc gesla jazn
        show("bez ogonków (poprawnie)", asciiFold("Zażółć gęślą jaźń"));
        // WYNIK: bez ogonków (poprawnie) → Zazolc gesla jazn
        // PUŁAPKA: rozłożenie na NFD i usunięcie akcentów (\p{M}) zostawia "ł" i "Ł".
        // Dlaczego: kreska w "ł" nie jest łączącym akcentem. Dlatego asciiFold dodatkowo zamienia
        // "ł"→"l" i "Ł"→"L". Nie próbuj "wyczyścić" tekstu na zapas — usuwaj ogonki tylko tam, gdzie
        // trzeba (adresy URL, nazwy plików, wyszukiwanie), a użytkownikowi pokazuj oryginał.

        // KIERUNEK PISMA: arabski i hebrajski piszemy od prawej do lewej (RTL). java.text.Bidi
        // (od "bidirectional" = dwukierunkowy) odpowiada, czy tekst wymaga układu RTL.
        String hebrew = new String(new char[] {0x05E9, 0x05DC, 0x05D5, 0x05DD}); // słowo hebrajskie
        show("hebrajski jest RTL", new Bidi(hebrew, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT).isRightToLeft());
        // WYNIK: hebrajski jest RTL → true
        show("polski jest LTR", new Bidi("Zażółć", Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT).isLeftToRight());
        // WYNIK: polski jest LTR → true
        // W internacjonalizowanym interfejsie nie "wstawiaj" na sztywno tekstu obok pola — układ
        // dla RTL jest lustrzany (framework UI zwykle robi to sam).

        // KODOWANIE (powtórka z t18_io_files/Io12Charsets): kod źródłowy, pliki .properties, odpowiedzi
        // HTTP i baza danych — wszędzie UTF-8 i wszędzie jawnie. Konsola Windows bywa kłopotliwa
        // (stare strony kodowe) — to problem wyświetlania, a nie danych.

        // DOBRA PRAKTYKA: wejście od użytkownika normalizuj (NFC) i przycinaj (strip) na granicy
        // systemu, a porównania "bez względu na wielkość" rób z Locale.ROOT lub Collatorem.
    }

    // =================================================================================================
    // 9. CAŁY KOMUNIKAT
    // =================================================================================================

    /**
     * 9. Wszystko razem: pakiet zasobów + liczba mnoga + waluta + data, dla trzech języków.
     * Tak wygląda zwykła warstwa komunikatów aplikacji.
     */
    static void wholeMessage() {
        section("9. Całość: komunikat zamówienia w trzech językach");

        LocalDate orderDate = LocalDate.of(2026, 3, 15);
        BigDecimal total = new BigDecimal("1234.50");
        for (String tag : List.of("pl-PL", "en-US", "de-DE")) {
            Locale locale = Locale.forLanguageTag(tag);
            String line = orderSummary(locale, "Ala", orderDate, 3, total, "PLN");
            show(tag, line);
        }
        // WYNIK: pl-PL → Dzień dobry Ala, zamówienie z dnia 15 marca 2026: 3 pozycje na kwotę 1 234,50 zł.
        // WYNIK: en-US → Hello Ala, order of March 15, 2026: 3 items for PLN1,234.50.
        // WYNIK: de-DE → Guten Tag Ala, Bestellung vom 15. März 2026: 3 Positionen über 1.234,50 PLN.
        // Pojedyncza pozycja i polskie formy mnogie:
        for (int n : new int[] {1, 2, 5}) {
            show("pl-PL, " + n + " szt.", orderSummary(PL, "Jan", orderDate, n, total, "PLN"));
        }
        // WYNIK: pl-PL, 1 szt. → Dzień dobry Jan, zamówienie z dnia 15 marca 2026: 1 pozycja na kwotę 1 234,50 zł.
        // WYNIK: pl-PL, 2 szt. → Dzień dobry Jan, zamówienie z dnia 15 marca 2026: 2 pozycje na kwotę 1 234,50 zł.
        // WYNIK: pl-PL, 5 szt. → Dzień dobry Jan, zamówienie z dnia 15 marca 2026: 5 pozycji na kwotę 1 234,50 zł.

        // DOBRA PRAKTYKA: jedna klasa/metoda zna Locale i formatuje wszystko; reszta programu
        // przekazuje DANE (kwota jako BigDecimal, data jako LocalDate), nie gotowe teksty.
        // Dlaczego: ten sam obiekt możesz wtedy pokazać użytkownikom w różnych językach.
    }

    /** Składa komunikat z pakietu: pozdrowienie, data, liczba pozycji (mnoga) i kwota w walucie. */
    static String orderSummary(Locale locale, String customer, LocalDate date, long items,
                               BigDecimal total, String currencyCode) {
        ResourceBundle bundle = bundle(locale);
        String dateText = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(date);
        NumberFormat money = NumberFormat.getCurrencyInstance(locale);
        money.setCurrency(Currency.getInstance(currencyCode)); // waluta kwoty, nie kraju użytkownika
        String moneyText = plain(money.format(total));
        String itemsText = itemsText(bundle, locale, items);
        String greeting = bundle.getString("greeting") + " " + customer;
        return plain(new MessageFormat(bundle.getString("summary"), locale)
                .format(new Object[] {greeting, dateText, itemsText, moneyText}));
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI
    // =================================================================================================

    /** Zamienia twarde spacje U+00A0 i U+202F na zwykłą spację (żeby WYNIK był czytelny i stały). */
    static String plain(String text) {
        return text.replace((char) 0x00A0, ' ').replace((char) 0x202F, ' ');
    }

    /** Naiwne usuwanie akcentów: NFD i skasowanie znaków łączących (\p{M}); zostawia "ł". */
    static String stripAccentsNaive(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /** Usuwa polskie ogonki, także kreskowane "ł". */
    static String asciiFold(String text) {
        return stripAccentsNaive(text).replace('ł', 'l').replace('Ł', 'L');
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Locale.forLanguageTag("pl-PL") — z MYŚLNIKIEM; "pl_PL" daje puste Locale.
     *   • Zawsze jawny Locale: String.format(Locale.ROOT, ...), toLowerCase(Locale.ROOT).
     *   • Locale.ROOT — dla maszyn; Locale użytkownika — dla ludzi.
     *   • NumberFormat: getInstance / getCurrencyInstance / getPercentInstance; polskie formaty mają
     *     twarde spacje; waluta kwoty ustawiana jawnie (setCurrency); formattery nie są wątkowo bezpieczne.
     *   • Daty: ofLocalizedDate(styl).withLocale(...); MMMM = "marca", LLLL = "marzec"; yyyy a nie YYYY.
     *   • ResourceBundle: łańcuch base ← pl ← pl_PL; brak klucza = MissingResourceException;
     *     getBundle bez kontroli zaczyna od języka komputera.
     *   • MessageFormat(szablon, locale): '' = jeden apostrof; liczby w {n} są formatowane (numery!).
     *   • Liczba mnoga: reguła per język; polski: 1 / kończy się na 2-4 (ale nie 12-14) / reszta.
     *   • Collator.getInstance(locale) do sortowania tekstów dla ludzi; CollationKey przy dużych listach.
     *   • Normalizer NFC/NFD: "ó" może być jednym albo dwoma znakami; "ł" nie rozkłada się.
     *   • Spring: MessageSource + LocaleResolver; plural → klucze na formę lub ICU4J.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego String.format("%.2f", x) bez Locale może dać "12,50" na jednym komputerze
     *      i "12.50" na drugim? Jak temu zapobiec?
     *   2. Co wypisze:  Locale.forLanguageTag("pl_PL").getLanguage().isEmpty()  ?
     *   3. ZNAJDŹ BŁĄD:  if (command.toLowerCase().equals("file")) { ... }  // program działa
     *      na komputerze z tureckim systemem
     *   4. Jak ResourceBundle znajdzie klucz "farewell", jeśli pakiet pl_PL go nie ma, a pakiet pl go ma?
     *   5. Co wypisze:  MessageFormat.format("It's {0}", "x")  ?
     *   6. Co wypisze:  new MessageFormat("nr {0}", Locale.forLanguageTag("pl-PL")).format(new Object[]{1234567})
     *      (po zamianie twardych spacji na zwykłe)?
     *   7. Jaką formę rzeczownika "plik" zastosujesz dla 22, 25 i 112? Dlaczego ChoiceFormat sobie z tym nie radzi?
     *   8. ZNAJDŹ BŁĄD:  String slug = Normalizer.normalize("Łódź", Normalizer.Form.NFD).replaceAll("\\p{M}", "");
     *      — jaki wynik dostaniemy i dlaczego jest niepełny?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: cena 1234,5 zł", "1 234,50 zł", () -> exercise1(1234.5));
        Check.equal("ćw. 1: cena 0,5 zł", "0,50 zł", () -> exercise1(0.5));
        Check.equal("ćw. 2: 1", "one", () -> exercise2(1));
        Check.equal("ćw. 2: 2", "few", () -> exercise2(2));
        Check.equal("ćw. 2: 5", "many", () -> exercise2(5));
        Check.equal("ćw. 2: 12", "many", () -> exercise2(12));
        Check.equal("ćw. 2: 22", "few", () -> exercise2(22));
        Check.equal("ćw. 2: 101", "many", () -> exercise2(101));
        Check.equal("ćw. 2: 0", "many", () -> exercise2(0));
        Check.equal("ćw. 3: sortowanie polskie", List.of("Ala", "Ącki", "Łódź", "ola", "Zebra", "Żaba"),
                () -> exercise3(List.of("Żaba", "Ala", "Łódź", "Zebra", "Ącki", "ola")));
        Check.equal("ćw. 4: slug zdania", "zazolc-gesla-jazn", () -> exercise4("Zażółć gęślą jaźń!"));
        Check.equal("ćw. 4: slug z myślnikiem i spacjami", "lodz-miasto-filmu", () -> exercise4("  Łódź — miasto filmu  "));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1: cena 1234,5 zł", "1 234,50 zł", () -> solution1(1234.5));
        Check.equal("ćw. 1: cena 0,5 zł", "0,50 zł", () -> solution1(0.5));
        Check.equal("ćw. 2: 1", "one", () -> solution2(1));
        Check.equal("ćw. 2: 2", "few", () -> solution2(2));
        Check.equal("ćw. 2: 5", "many", () -> solution2(5));
        Check.equal("ćw. 2: 12", "many", () -> solution2(12));
        Check.equal("ćw. 2: 22", "few", () -> solution2(22));
        Check.equal("ćw. 2: 101", "many", () -> solution2(101));
        Check.equal("ćw. 2: 0", "many", () -> solution2(0));
        Check.equal("ćw. 3: sortowanie polskie", List.of("Ala", "Ącki", "Łódź", "ola", "Zebra", "Żaba"),
                () -> solution3(List.of("Żaba", "Ala", "Łódź", "Zebra", "Ącki", "ola")));
        Check.equal("ćw. 4: slug zdania", "zazolc-gesla-jazn", () -> solution4("Zażółć gęślą jaźń!"));
        Check.equal("ćw. 4: slug z myślnikiem i spacjami", "lodz-miasto-filmu", () -> solution4("  Łódź — miasto filmu  "));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 12 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ formatowanie ceny tak, by nie zależało od komputera.
     * Stary kod (wynik zależy od ustawień systemu, brak separatora tysięcy):
     * <pre>{@code
     * return String.format("%.2f zł", price);
     * }</pre>
     * Nowy kod: kwota w formacie polskim z grupowaniem tysięcy i przecinkiem, a na końcu " zł".
     * Zamień twarde spacje na zwykłe (plain). Podpowiedź: String.format(PL, "%,.2f zł", price).
     */
    static String exercise1(double price) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zaimplementuj polską regułę liczby mnogiej BEZ użycia pluralCategory:
     * zwróć "one" dla 1, "few" dla kończących się na 2-4 (ale nie 12-14), w pozostałych "many".
     * Podpowiedź: n % 10 i n % 100.
     */
    static String exercise2(long n) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć NOWĄ listę posortowaną po polsku (Collator dla PL).
     * Nie zmieniaj listy wejściowej (List.of jest niezmienna!).
     * Podpowiedź: new ArrayList<>(words) i sort(collator).
     */
    static List<String> exercise3(List<String> words) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zrób "slug" do adresu URL: małe litery, bez polskich znaków,
     * ciągi znaków innych niż litery i cyfry zamienione na pojedynczy myślnik, bez myślników
     * na brzegach. Przykład: "Zażółć gęślą jaźń!" → "zazolc-gesla-jazn".
     * Podpowiedź: NFD i \p{M}, pamiętaj o "ł"; replaceAll("[^a-z0-9]+", "-"); toLowerCase(Locale.ROOT).
     */
    static String exercise4(String text) {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(double price) {
        return plain(String.format(PL, "%,.2f zł", price));
    }

    static String solution2(long n) {
        long last = Math.abs(n) % 10;
        long last2 = Math.abs(n) % 100;
        if (n == 1) {
            return "one";
        }
        return (last >= 2 && last <= 4 && (last2 < 12 || last2 > 14)) ? "few" : "many";
    }

    static List<String> solution3(List<String> words) {
        List<String> copy = new ArrayList<>(words);
        copy.sort(Collator.getInstance(PL));
        return copy;
    }

    static String solution4(String text) {
        String ascii = asciiFold(text).toLowerCase(Locale.ROOT);
        String dashed = ascii.replaceAll("[^a-z0-9]+", "-");
        return dashed.replaceAll("^-+|-+$", "");
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bez Locale metoda używa ustawienia domyślnego komputera: polski daje przecinek,
     *      angielski kropkę. Zapobiega temu jawny Locale: String.format(Locale.ROOT, ...)
     *      dla danych maszynowych albo Locale użytkownika dla tekstu dla ludzi.
     *   2. true — znacznik z podkreślnikiem nie jest poprawny (ma być pl-PL), więc powstaje
     *      puste Locale, a metoda nie rzuca wyjątku.
     *   3. Na tureckim systemie toLowerCase() zamienia "I" na "ı" (bez kropki), więc "FILE"
     *      staje się "fıle" i porównanie zawodzi. Poprawnie: toLowerCase(Locale.ROOT).
     *   4. Z pakietu pl (rodzica): łańcuch szukania to pl_PL, potem pl, potem bazowy.
     *   5. Its {0} — apostrof zaczął cytowanie, więc {0} nie jest już argumentem i znika
     *      apostrof. Poprawnie: It''s {0}.
     *   6. nr 1 234 567 — liczba jest formatowana z separatorem tysięcy; numer lepiej podać
     *      jako tekst albo użyć {0,number,#}.
     *   7. 22 pliki (kończy się na 2), 25 plików, 112 plików (12 to wyjątek, jak 12-14).
     *      ChoiceFormat zna tylko progi "od wartości w górę", a polska reguła zależy od ostatnich cyfr.
     *   8. Wynik "Łodz": "Ł" się nie rozkłada (kreska to nie akcent), więc zostaje; trzeba
     *      dodatkowo zamienić ł i Ł ręcznie.
     */
    // </editor-fold>
}
