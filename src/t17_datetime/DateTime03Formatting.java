package t17_datetime;

import helpers.Check;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Optional;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: DateTimeFormatter — formatowanie i parsowanie dat: wzorce, polskie nazwy, tryb ścisły, pułapki
 *        (format = zamień na tekst; parse = odczytaj z tekstu; pattern = wzorzec; locale = ustawienia regionalne)
 *
 * W SKRÓCIE:
 *   toString() daty daje format ISO (2025-03-14) — dobry do plików i API, ale nie dla ludzi. DateTimeFormatter.ofPattern
 *   ("dd.MM.yyyy") opisuje własny format; ta sama reguła służy do format (data → tekst) i parse (tekst → data).
 *   Nazwy dni i miesięcy zależą od Locale — podawaj je jawnie. Uwaga na litery: MM (miesiąc) ≠ mm (minuty),
 *   yyyy (rok) ≠ YYYY (rok „tygodniowy”), HH (0–23) ≠ hh (1–12). Formatter jest niezmienny — trzymaj go w stałej.
 *
 * ANALOGIA: szablon do wypełniania formularza.
 *   Wzorzec "dd.MM.yyyy" to szablon z kratkami: dwie na dzień, kropka, dwie na miesiąc, kropka, cztery na rok.
 *   Tym samym szablonem wypełniasz formularz (format) i odczytujesz cudzy (parse) — jeśli ktoś wpisał inaczej, błąd.
 *
 * JAK TO DZIAŁA:
 *   DateTimeFormatter PL = DateTimeFormatter.ofPattern("dd.MM.yyyy");
 *   LocalDate.of(2025, 3, 14).format(PL)      → "14.03.2025"
 *   LocalDate.parse("14.03.2025", PL)         → 2025-03-14
 *   ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("pl-PL"))   → "piątek, 14 marca 2025"
 *
 * SŁÓWKA:
 *   formatter = formater (obiekt formatujący); pattern = wzorzec; locale = ustawienia regionalne (język + kraj);
 *   format = sformatuj; parse = odczytaj; resolver style = sposób rozstrzygania (SMART — „sprytny”, STRICT — ścisły);
 *   week-based year = rok liczony tygodniami; ISO = międzynarodowy standard zapisu dat (ISO-8601); constant = stała.
 *
 * ZOBACZ TEŻ: t17_datetime/DateTime01LocalDateTime, t04_strings/Strings04Formatting (Locale i String.format),
 *             t15_numbers/Numbers04FormattingParsing (formatowanie liczb), t10_exceptions/Exceptions07BestPractices.
 * </pre>
 */
public class DateTime03Formatting {

    /** Formatery trzymamy w STAŁYCH: są niezmienne i bezpieczne wątkowo — jeden obiekt dla całego programu. */
    static final Locale PL = Locale.forLanguageTag("pl-PL");
    static final DateTimeFormatter PL_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    static final DateTimeFormatter PL_DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    static final DateTimeFormatter PL_LONG = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", PL);

    public static void main(String[] args) {
        title("DateTime03 — formatowanie i parsowanie dat");

        isoVsCustom();          // ISO vs custom = ISO kontra własny format
        patternLetters();       // pattern letters = litery wzorca
        localeNames();          // locale names = nazwy zależne od języka
        parsing();              // parsing = parsowanie
        strictParsing();        // strict parsing = ścisłe parsowanie
        letterPitfalls();       // letter pitfalls = pułapki liter
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. ISO KONTRA WŁASNY FORMAT
    // =================================================================================================

    /** 1. toString() = ISO-8601 (do maszyn). Dla ludzi — własny wzorzec. */
    static void isoVsCustom() {
        section("1. toString (ISO) kontra ofPattern");

        LocalDateTime dt = LocalDateTime.of(2025, 3, 14, 9, 5);
        show("toString()", dt);
        show("format(PL_DATE_TIME)", dt.format(PL_DATE_TIME));
        show("BASIC_ISO_DATE", dt.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE));
        // WYNIK: toString() → 2025-03-14T09:05
        // WYNIK: format(PL_DATE_TIME) → 14.03.2025 09:05
        // WYNIK: BASIC_ISO_DATE → 20250314

        // DOBRA PRAKTYKA: w plikach, bazach i API zapisuj daty w ISO (2025-03-14) — sortują się jako tekst i nie ma
        //   wątpliwości, co jest dniem, a co miesiącem. Format „ludzki” tylko przy wyświetlaniu.
    }

    // =================================================================================================
    // 2. LITERY WZORCA
    // =================================================================================================

    /** 2. Najważniejsze litery. Liczba powtórzeń zmienia postać: M → 3, MM → 03, MMM → mar, MMMM → marca. */
    static void patternLetters() {
        section("2. Litery wzorca");

        LocalDateTime dt = LocalDateTime.of(2025, 3, 4, 14, 7, 9);
        String[] patterns = {"d.M.yy", "dd.MM.yyyy", "HH:mm:ss", "hh:mm", "d MMM", "yyyy-MM-dd'T'HH:mm"};
        for (String p : patterns) {
            System.out.println(String.format("%-20s → %s", p, dt.format(DateTimeFormatter.ofPattern(p, PL))));
        }
        // WYNIK: d.M.yy               → 4.3.25
        // WYNIK: dd.MM.yyyy           → 04.03.2025
        // WYNIK: HH:mm:ss             → 14:07:09
        // WYNIK: hh:mm                → 02:07
        // WYNIK: d MMM                → 4 mar
        // WYNIK: yyyy-MM-dd'T'HH:mm   → 2025-03-04T14:07

        // Tabela: d/dd dzień · M/MM/MMM/MMMM miesiąc · yy/yyyy rok · E/EEEE dzień tygodnia · HH godzina 0–23 ·
        //   hh godzina 1–12 · mm minuty · ss sekundy · '...' tekst dosłowny (apostrofy).
    }

    // =================================================================================================
    // 3. NAZWY ZALEŻNE OD JĘZYKA
    // =================================================================================================

    /** 3. EEEE i MMMM dają nazwy w języku z Locale. Polski ma odmianę: „14 marca” (format), „marzec” (samodzielnie, LLLL). */
    static void localeNames() {
        section("3. Nazwy dni i miesięcy: Locale");

        LocalDate d = LocalDate.of(2025, 3, 14);
        show("pl-PL", d.format(PL_LONG));
        show("ENGLISH", d.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)));
        show("LLLL (samodzielnie, pl)", d.format(DateTimeFormatter.ofPattern("LLLL yyyy", PL)));
        // WYNIK: pl-PL → piątek, 14 marca 2025
        // WYNIK: ENGLISH → Friday, 14 March 2025
        // WYNIK: LLLL (samodzielnie, pl) → marzec 2025    ← mianownik, np. w nagłówku kalendarza

        // PUŁAPKA: ofPattern bez Locale używa ustawień KOMPUTERA — ten sam program na innym systemie wypisze
        //   nazwy po angielsku. Przy nazwach (EEEE, MMMM) zawsze podawaj Locale.
    }

    // =================================================================================================
    // 4. PARSOWANIE
    // =================================================================================================

    /** 4. parse(tekst, formatter) — tekst musi DOKŁADNIE pasować do wzorca, inaczej DateTimeParseException. */
    static void parsing() {
        section("4. Parsowanie tekstu");

        show("parse(\"14.03.2025\")", LocalDate.parse("14.03.2025", PL_DATE));
        show("parse(\"14.03.2025 18:45\")", LocalDateTime.parse("14.03.2025 18:45", PL_DATE_TIME));
        // WYNIK: parse("14.03.2025") → 2025-03-14
        // WYNIK: parse("14.03.2025 18:45") → 2025-03-14T18:45

        expectThrows("parse(\"2025-03-14\", PL_DATE)", () -> LocalDate.parse("2025-03-14", PL_DATE));
        // WYNIK: ✔ parse("2025-03-14", PL_DATE) → rzucono DateTimeParseException: Text '2025-03-14' could not be parsed at index 2

        expectThrows("parse(\"4.3.2025\", PL_DATE)", () -> LocalDate.parse("4.3.2025", PL_DATE));
        // WYNIK: ✔ parse("4.3.2025", PL_DATE) → rzucono DateTimeParseException: Text '4.3.2025' could not be parsed at index 0
        // „dd” wymaga DWÓCH cyfr. Wzorzec "d.M.yyyy" przyjmie i „4.3.2025”, i „14.03.2025”.
    }

    // =================================================================================================
    // 5. TRYB ŚCISŁY
    // =================================================================================================

    /**
     * 5. Domyślny tryb SMART „poprawia” niektóre błędne daty: 31.02 → 28.02 (po cichu!). Tryb STRICT odrzuca je.
     * STRICT wymaga litery u (rok) zamiast y (rok ery) — inaczej nawet poprawne daty się nie odczytają.
     */
    static void strictParsing() {
        section("5. SMART kontra STRICT");

        show("SMART: 31.02.2025", LocalDate.parse("31.02.2025", PL_DATE));
        // WYNIK: SMART: 31.02.2025 → 2025-02-28    ← po cichu zmieniona data!

        DateTimeFormatter strict = DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);
        expectThrows("STRICT: 31.02.2025", () -> LocalDate.parse("31.02.2025", strict));
        show("STRICT: 28.02.2025", LocalDate.parse("28.02.2025", strict));
        // WYNIK: ✔ STRICT: 31.02.2025 → rzucono DateTimeParseException: Text '31.02.2025' could not be parsed: Invalid date 'FEBRUARY 31'
        // WYNIK: STRICT: 28.02.2025 → 2025-02-28

        // DOBRA PRAKTYKA: dane od użytkownika (formularze, import) parsuj w trybie STRICT — lepiej odrzucić błąd,
        //   niż zapisać inną datę, niż ktoś wpisał.
    }

    // =================================================================================================
    // 6. PUŁAPKI LITER
    // =================================================================================================

    /** 6. Wielkość liter we wzorcu ma znaczenie. Te pomyłki kompilują się i działają — tylko dają zły wynik. */
    static void letterPitfalls() {
        section("6. Pułapki: mm/MM, YYYY/yyyy, hh/HH");

        LocalDateTime dt = LocalDateTime.of(2024, 12, 30, 15, 45);
        show("dd.mm.yyyy (mm = minuty!)", dt.format(DateTimeFormatter.ofPattern("dd.mm.yyyy")));
        show("dd.MM.YYYY (YYYY = rok tygodniowy!)", dt.format(DateTimeFormatter.ofPattern("dd.MM.YYYY", PL)));
        show("hh:mm (bez AM/PM)", dt.format(DateTimeFormatter.ofPattern("hh:mm")));
        // WYNIK: dd.mm.yyyy (mm = minuty!) → 30.45.2024
        // WYNIK: dd.MM.YYYY (YYYY = rok tygodniowy!) → 30.12.2025    ← 30.12.2024 to już 1. tydzień roku 2025
        // WYNIK: hh:mm (bez AM/PM) → 03:45    ← 15:45 czy 03:45 w nocy? Nie wiadomo

        // PUŁAPKA: YYYY jest podstępne — przez 51 tygodni w roku działa jak yyyy, a psuje się tylko na przełomie roku.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • DateTimeFormatter.ofPattern("dd.MM.yyyy") — format: data.format(f); parse: LocalDate.parse(tekst, f).
     *   • Formatery są niezmienne i bezpieczne wątkowo → static final (w odróżnieniu od starego SimpleDateFormat).
     *   • Litery: dd dzień, MM miesiąc, yyyy rok, HH 0–23, mm minuty, ss sekundy, EEEE dzień tyg., MMMM miesiąc słownie.
     *   • Nazwy: podawaj Locale (Locale.forLanguageTag("pl-PL")); LLLL = mianownik („marzec”).
     *   • Zły tekst → DateTimeParseException (z indeksem miejsca błędu).
     *   • SMART poprawia 31.02 → 28.02; do danych od ludzi: "dd.MM.uuuu" + ResolverStyle.STRICT.
     *   • Pułapki: mm ≠ MM, YYYY ≠ yyyy, hh ≠ HH. Zapis maszynowy: ISO (toString / ISO_LOCAL_DATE).
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  System.out.println(LocalDate.of(2025, 1, 5).format(DateTimeFormatter.ofPattern("d/M/yy")));  ?
     *   2. ZNAJDŹ BŁĄD:  DateTimeFormatter f = DateTimeFormatter.ofPattern("dd-mm-yyyy");
     *   3. Dlaczego formatter warto trzymać w stałej static final?
     *   4. Co się stanie przy LocalDate.parse("2025-3-14") (bez formatera)?
     *   5. Czym różni się tryb SMART od STRICT? Który wybrać dla formularza?
     *   6. Co wypisze wzorzec "EEEE" dla 14.03.2025 z Locale.ENGLISH, a co z pl-PL?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: format polski", "05.01.2025", () -> exercise1(LocalDate.of(2025, 1, 5)));
        Check.equal("ćw. 2: parse z ukośnikami", LocalDate.of(2025, 3, 14), () -> exercise2("14/03/2025"));
        Check.equal("ćw. 3: długi polski format", "sobota, 1 listopada 2025", () -> exercise3(LocalDate.of(2025, 11, 1)));
        Check.equal("ćw. 4a: poprawna data", Optional.of(LocalDate.of(2024, 2, 29)), () -> exercise4("29.02.2024"));
        Check.equal("ćw. 4b: nieistniejąca data", Optional.empty(), () -> exercise4("29.02.2025"));
        Check.equal("ćw. 4c: zły format", Optional.empty(), () -> exercise4("2025-02-10"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "05.01.2025", () -> solution1(LocalDate.of(2025, 1, 5)));
        Check.equal("ćw. 2 (wzorzec)", LocalDate.of(2025, 3, 14), () -> solution2("14/03/2025"));
        Check.equal("ćw. 3 (wzorzec)", "sobota, 1 listopada 2025", () -> solution3(LocalDate.of(2025, 11, 1)));
        Check.equal("ćw. 4a (wzorzec)", Optional.of(LocalDate.of(2024, 2, 29)), () -> solution4("29.02.2024"));
        Check.equal("ćw. 4b (wzorzec)", Optional.empty(), () -> solution4("29.02.2025"));
        Check.equal("ćw. 4c (wzorzec)", Optional.empty(), () -> solution4("2025-02-10"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): sformatuj datę jako "dd.MM.yyyy". */
    static String exercise1(LocalDate date) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /** ĆWICZENIE 2 (łatwe): odczytaj datę zapisaną jako "dd/MM/yyyy". */
    static LocalDate exercise2(String text) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /** ĆWICZENIE 3 (średnie): długi polski format: "sobota, 1 listopada 2025". Podpowiedź: sekcja 3 i Locale. */
    static String exercise3(LocalDate date) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): bezpieczne, ŚCISŁE parsowanie "dd.MM.uuuu": poprawna data → Optional.of(data),
     * nieistniejąca albo zły format → Optional.empty(). Podpowiedź: sekcja 5 + try/catch (DateTimeParseException).
     */
    static Optional<LocalDate> exercise4(String text) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static final DateTimeFormatter SLASHES = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter STRICT_PL = DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);

    static String solution1(LocalDate date) {
        return date.format(PL_DATE);
    }

    static LocalDate solution2(String text) {
        return LocalDate.parse(text, SLASHES);
    }

    static String solution3(LocalDate date) {
        return date.format(PL_LONG);
    }

    static Optional<LocalDate> solution4(String text) {
        try {
            return Optional.of(LocalDate.parse(text, STRICT_PL));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. „5/1/25”.
     *   2. mm to MINUTY, nie miesiąc — dla daty (LocalDate) format rzuci wyjątek (brak minut), a dla LocalDateTime
     *      wypisze minuty w miejscu miesiąca. Poprawnie: "dd-MM-yyyy".
     *   3. Jest niezmienny i bezpieczny wątkowo — tworzenie go za każdym razem to zbędna praca, a stała daje jedno
     *      miejsce definicji formatu w programie.
     *   4. DateTimeParseException — format ISO wymaga dwucyfrowego miesiąca: "2025-03-14".
     *   5. SMART „naprawia” niektóre błędy (31.02 → 28.02), STRICT odrzuca każdą niepoprawną datę (wymaga uuuu).
     *      Dla formularza — STRICT.
     *   6. „Friday” i „piątek”.
     */
    // </editor-fold>
}
