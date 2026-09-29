package t17_datetime;

import helpers.Check;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: LocalDate, LocalTime, LocalDateTime — data i czas bez strefy czasowej
 *        (local = lokalny, „jak na kalendarzu na ścianie”; date = data; time = godzina)
 *
 * W SKRÓCIE:
 *   LocalDate to sama data (2025-03-14), LocalTime sama godzina (08:30), LocalDateTime jedno i drugie. Wszystkie są
 *   NIEZMIENNE: plusDays(1) nie zmienia obiektu, tylko zwraca nowy. Miesiące liczone są od 1 (marzec = 3), dni tygodnia
 *   i miesiące to enumy (DayOfWeek, Month). Tworzysz je przez of(...) i parse("2025-03-14"), a „teraz” przez now() —
 *   najlepiej z podanym zegarem (Clock), żeby kod dało się testować.
 *
 * ANALOGIA: kartka z kalendarza i zegar na ścianie.
 *   LocalDate to kartka z kalendarza — nie wiadomo, w jakim mieście wisi. LocalTime to wskazówki zegara. LocalDateTime
 *   to zdjęcie obu naraz. Żeby wiedzieć, KIEDY to było na świecie, potrzeba jeszcze strefy (DateTime04ZonesInstant).
 *
 * JAK TO DZIAŁA:
 *   LocalDate d = LocalDate.of(2025, 3, 14);          ← rok, miesiąc (1–12!), dzień
 *   d.getDayOfWeek()      → FRIDAY                     d.plusDays(20) → 2025-04-03 (NOWY obiekt)
 *   d.withDayOfMonth(1)   → 2025-03-01                 d.isBefore(LocalDate.of(2025, 4, 1)) → true
 *   LocalDateTime dt = d.atTime(8, 30);               → 2025-03-14T08:30
 *
 * SŁÓWKA:
 *   local = lokalny; date = data; time = godzina; of = z (podanych wartości); parse = odczytaj z tekstu; now = teraz;
 *   plus/minus = dodaj/odejmij; with = z (zmienionym polem); leap year = rok przestępny; day of week = dzień tygodnia;
 *   is before/after = czy przed/po; adjuster = „dopasowywacz” (reguła zmiany daty); clock = zegar; fixed = ustalony.
 *
 * ZOBACZ TEŻ: t17_datetime/DateTime02PeriodDuration (różnice dat), t17_datetime/DateTime03Formatting (formatowanie),
 *             t08_enums/Enums04EnumMapSet (DayOfWeek jako enum), t06_oop_basics/Oop06Immutability (niezmienność).
 * </pre>
 */
public class DateTime01LocalDateTime {

    public static void main(String[] args) {
        title("DateTime01 — LocalDate, LocalTime, LocalDateTime");

        oldApiProblems();       // old API problems = problemy starego API
        creatingDates();        // creating dates = tworzenie dat
        readingFields();        // reading fields = odczyt pól
        times();                // times = godziny
        immutableArithmetic();  // immutable arithmetic = niezmienna arytmetyka
        adjusters();            // adjusters = reguły dopasowania
        comparing();            // comparing = porównywanie
        clockForTesting();      // clock for testing = zegar do testów
        pitfalls();             // pitfalls = pułapki
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. DLACZEGO NOWE API
    // =================================================================================================

    /** 1. Stare java.util.Date i Calendar miały wady, które java.time naprawia. Starego API nie używaj w nowym kodzie. */
    static void oldApiProblems() {
        section("1. Stare API (Date, Calendar) kontra java.time");

        note("Date/Calendar: zmienne (ktoś może zmienić datę „pod Tobą”), miesiące od 0 (styczeń = 0), mylące nazwy");
        note("java.time: niezmienne, miesiące od 1, osobne klasy na datę, godzinę, chwilę i strefę");
        // WYNIK:    ℹ Date/Calendar: zmienne (ktoś może zmienić datę „pod Tobą”), miesiące od 0 (styczeń = 0), mylące nazwy
        // WYNIK:    ℹ java.time: niezmienne, miesiące od 1, osobne klasy na datę, godzinę, chwilę i strefę
        // Spotkasz jeszcze Date w starych bibliotekach — konwersja: date.toInstant() i Date.from(instant) (DateTime04).
    }

    // =================================================================================================
    // 2. TWORZENIE
    // =================================================================================================

    /** 2. of(...) z liczb albo enumu Month; parse(...) z tekstu w formacie ISO (rrrr-mm-dd). */
    static void creatingDates() {
        section("2. of i parse");

        LocalDate a = LocalDate.of(2025, 3, 14);
        LocalDate b = LocalDate.of(2025, Month.MARCH, 14);        // enum czytelniejszy niż liczba
        LocalDate c = LocalDate.parse("2025-03-14");              // ISO-8601: rok-miesiąc-dzień
        show("of(2025, 3, 14)", a);
        show("wszystkie równe?", a.equals(b) && b.equals(c));
        // WYNIK: of(2025, 3, 14) → 2025-03-14
        // WYNIK: wszystkie równe? → true

        LocalDateTime meeting = LocalDateTime.of(2025, 3, 14, 9, 30);
        show("LocalDateTime.of", meeting);
        show("parse z T", LocalDateTime.parse("2025-03-14T17:05:30"));
        // WYNIK: LocalDateTime.of → 2025-03-14T09:30
        // WYNIK: parse z T → 2025-03-14T17:05:30    ← „T” oddziela datę od godziny (standard ISO)
    }

    // =================================================================================================
    // 3. ODCZYT PÓL
    // =================================================================================================

    /** 3. Gettery zwracają liczby albo enumy: getMonth() → Month.MARCH, getDayOfWeek() → DayOfWeek.FRIDAY. */
    static void readingFields() {
        section("3. Odczyt: rok, miesiąc, dzień tygodnia, rok przestępny");

        LocalDate d = LocalDate.of(2024, 2, 29);
        show("getYear()", d.getYear());
        show("getMonth() / getMonthValue()", d.getMonth() + " / " + d.getMonthValue());
        show("getDayOfWeek()", d.getDayOfWeek());
        show("getDayOfYear()", d.getDayOfYear());
        show("lengthOfMonth()", d.lengthOfMonth());
        show("isLeapYear()", d.isLeapYear());
        // WYNIK: getYear() → 2024
        // WYNIK: getMonth() / getMonthValue() → FEBRUARY / 2
        // WYNIK: getDayOfWeek() → THURSDAY
        // WYNIK: getDayOfYear() → 60
        // WYNIK: lengthOfMonth() → 29
        // WYNIK: isLeapYear() → true
    }

    // =================================================================================================
    // 4. GODZINY
    // =================================================================================================

    /** 4. LocalTime „kręci się” w kółko: 23:30 + 45 minut = 00:15 (bez informacji o zmianie dnia!). */
    static void times() {
        section("4. LocalTime");

        LocalTime late = LocalTime.of(23, 30);
        show("23:30 + 45 min", late.plusMinutes(45));
        // WYNIK: 23:30 + 45 min → 00:15    ← LocalTime nie wie, że to już następny dzień

        LocalDateTime lateDt = LocalDateTime.of(2025, 12, 31, 23, 30);
        show("31.12 23:30 + 45 min", lateDt.plusMinutes(45));
        // WYNIK: 31.12 23:30 + 45 min → 2026-01-01T00:15    ← LocalDateTime przechodzi przez północ i rok

        show("LocalTime.parse(\"08:05\").getMinute()", LocalTime.parse("08:05").getMinute());
        show("MIDNIGHT / NOON", LocalTime.MIDNIGHT + " / " + LocalTime.NOON);
        // WYNIK: LocalTime.parse("08:05").getMinute() → 5
        // WYNIK: MIDNIGHT / NOON → 00:00 / 12:00
    }

    // =================================================================================================
    // 5. NIEZMIENNA ARYTMETYKA
    // =================================================================================================

    /** 5. plus/minus/with zwracają NOWY obiekt. Oryginał się nie zmienia — wynik trzeba przypisać. */
    static void immutableArithmetic() {
        section("5. plus, minus, with — zawsze nowy obiekt");

        LocalDate start = LocalDate.of(2025, 3, 14);
        start.plusDays(10);                                    // PUŁAPKA: wynik zignorowany — start bez zmian
        show("po zignorowanym plusDays", start);
        // WYNIK: po zignorowanym plusDays → 2025-03-14

        LocalDate later = start.plusDays(20).plusMonths(1).minusYears(1);   // łańcuch wywołań
        show("plusDays(20).plusMonths(1).minusYears(1)", later);
        show("withDayOfMonth(1)", start.withDayOfMonth(1));
        show("withMonth(12)", start.withMonth(12));
        // WYNIK: plusDays(20).plusMonths(1).minusYears(1) → 2024-05-03
        // WYNIK: withDayOfMonth(1) → 2025-03-01
        // WYNIK: withMonth(12) → 2025-12-14

        // DOBRA PRAKTYKA: IntelliJ ostrzega „Result of LocalDate.plusDays() is ignored” — traktuj to jak błąd.
    }

    // =================================================================================================
    // 6. TemporalAdjusters — „ostatni dzień miesiąca”, „następny poniedziałek”
    // =================================================================================================

    /** 6. with(TemporalAdjusters...) — gotowe reguły, których nie trzeba liczyć ręcznie. */
    static void adjusters() {
        section("6. TemporalAdjusters");

        LocalDate d = LocalDate.of(2025, 3, 14);               // piątek
        show("ostatni dzień miesiąca", d.with(TemporalAdjusters.lastDayOfMonth()));
        show("pierwszy dzień następnego miesiąca", d.with(TemporalAdjusters.firstDayOfNextMonth()));
        show("następny poniedziałek", d.with(TemporalAdjusters.next(DayOfWeek.MONDAY)));
        show("ten lub następny piątek", d.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)));
        show("ostatni piątek miesiąca", d.with(TemporalAdjusters.lastInMonth(DayOfWeek.FRIDAY)));
        // WYNIK: ostatni dzień miesiąca → 2025-03-31
        // WYNIK: pierwszy dzień następnego miesiąca → 2025-04-01
        // WYNIK: następny poniedziałek → 2025-03-17
        // WYNIK: ten lub następny piątek → 2025-03-14
        // WYNIK: ostatni piątek miesiąca → 2025-03-28
    }

    // =================================================================================================
    // 7. PORÓWNYWANIE
    // =================================================================================================

    /** 7. isBefore/isAfter/isEqual czytają się jak zdanie; daty są Comparable, więc sortują się naturalnie. */
    static void comparing() {
        section("7. Porównywanie i sortowanie");

        LocalDate deadline = LocalDate.of(2025, 6, 30);
        LocalDate submitted = LocalDate.of(2025, 7, 2);
        show("oddane po terminie?", submitted.isAfter(deadline));
        show("oddane przed terminem?", submitted.isBefore(deadline));
        // WYNIK: oddane po terminie? → true
        // WYNIK: oddane przed terminem? → false

        List<LocalDate> dates = new ArrayList<>(List.of(
                LocalDate.of(2025, 12, 1), LocalDate.of(2024, 5, 20), LocalDate.of(2025, 1, 9)));
        dates.sort(null);                                     // porządek naturalny = chronologiczny
        show("posortowane", dates);
        // WYNIK: posortowane → [2024-05-20, 2025-01-09, 2025-12-01]

        // PUŁAPKA: daty porównuj isBefore/isAfter/equals — nigdy przez porównanie tekstów w formacie "dd.MM.yyyy"
        //   ("02.01.2025" < "15.12.2024" jako tekst!).
    }

    // =================================================================================================
    // 8. now() I ZEGAR DO TESTÓW
    // =================================================================================================

    /**
     * isExpired = czy termin minął. Przyjmuje Clock (zegar) zamiast wołać LocalDate.now() — w programie dajemy
     * Clock.systemDefaultZone(), w teście zegar ustawiony na konkretny dzień.
     */
    static boolean isExpired(LocalDate expiryDate, Clock clock) {
        return LocalDate.now(clock).isAfter(expiryDate);
    }

    /**
     * 8. LocalDate.now() zwraca co dzień co innego — wyniki programu (i testów) się zmieniają. Rozwiązanie: Clock.
     * Clock.fixed(...) to zegar „zamrożony” w podanej chwili.
     */
    static void clockForTesting() {
        section("8. now() i Clock — testowalny „teraz”");

        // LocalDate.now()  → dzisiejsza data (wynik zależy od uruchomienia — dlatego nie pokazujemy go w WYNIK)
        Clock fixed = Clock.fixed(Instant.parse("2025-03-14T10:00:00Z"), ZoneId.of("Europe/Warsaw"));
        show("LocalDate.now(fixed)", LocalDate.now(fixed));
        show("LocalTime.now(fixed)", LocalTime.now(fixed));
        // WYNIK: LocalDate.now(fixed) → 2025-03-14
        // WYNIK: LocalTime.now(fixed) → 11:00    ← 10:00 UTC to 11:00 w Warszawie zimą (UTC+1)

        show("mleko ważne do 13.03 — przeterminowane?", isExpired(LocalDate.of(2025, 3, 13), fixed));
        show("mleko ważne do 14.03 — przeterminowane?", isExpired(LocalDate.of(2025, 3, 14), fixed));
        // WYNIK: mleko ważne do 13.03 — przeterminowane? → true
        // WYNIK: mleko ważne do 14.03 — przeterminowane? → false

        // DOBRA PRAKTYKA: kod zależny od „teraz” niech przyjmuje Clock (albo gotową datę) jako parametr — t25_testing.
    }

    // =================================================================================================
    // 9. PUŁAPKI
    // =================================================================================================

    /** 9. Dodawanie miesięcy „przycina” dzień do końca miesiąca; nieistniejąca data to wyjątek. */
    static void pitfalls() {
        section("9. Pułapki: koniec miesiąca, nieistniejąca data");

        LocalDate jan31 = LocalDate.of(2025, 1, 31);
        show("31.01 + 1 miesiąc", jan31.plusMonths(1));
        show("31.01 + 1 miesiąc + 1 miesiąc", jan31.plusMonths(1).plusMonths(1));
        show("31.01 + 2 miesiące", jan31.plusMonths(2));
        // WYNIK: 31.01 + 1 miesiąc → 2025-02-28    ← luty nie ma 31 dni — przycięte do ostatniego
        // WYNIK: 31.01 + 1 miesiąc + 1 miesiąc → 2025-03-28    ← przycięcie „zostaje”
        // WYNIK: 31.01 + 2 miesiące → 2025-03-31    ← inny wynik niż dwa razy po 1!

        expectThrows("LocalDate.of(2025, 2, 29)", () -> LocalDate.of(2025, 2, 29));
        // WYNIK: ✔ LocalDate.of(2025, 2, 29) → rzucono DateTimeException: Invalid date 'February 29' as '2025' is not a leap year

        expectThrows("LocalDate.of(2025, 13, 1)", () -> LocalDate.of(2025, 13, 1));
        // WYNIK: ✔ LocalDate.of(2025, 13, 1) → rzucono DateTimeException: Invalid value for MonthOfYear (valid values 1 - 12): 13
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • LocalDate (data), LocalTime (godzina), LocalDateTime (oba) — bez strefy; niezmienne.
     *   • Tworzenie: of(2025, 3, 14) / of(2025, Month.MARCH, 14), parse("2025-03-14"), parse("2025-03-14T09:30").
     *   • Odczyt: getYear, getMonth (enum), getMonthValue (1–12), getDayOfWeek (enum), lengthOfMonth, isLeapYear.
     *   • plus/minus/with → NOWY obiekt (przypisz wynik!); TemporalAdjusters: lastDayOfMonth, next(MONDAY)...
     *   • isBefore/isAfter/equals; daty są Comparable (sort chronologiczny).
     *   • now(clock) zamiast now() — testowalność; Clock.fixed w testach.
     *   • 31.01 + 1 miesiąc = 28.02; nieistniejąca data → DateTimeException.
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  LocalDate d = LocalDate.of(2025, 5, 10); d.plusDays(5); System.out.println(d);  ?
     *   2. Co wypisze:  System.out.println(LocalDate.of(2024, 3, 31).minusMonths(1));  ?
     *   3. ZNAJDŹ BŁĄD:  if (LocalDate.now().toString().compareTo("2025-6-1") > 0) { ... }
     *   4. Czym różni się LocalTime.of(23, 0).plusHours(2) od LocalDateTime.of(2025, 1, 1, 23, 0).plusHours(2)?
     *   5. Dlaczego metoda sprawdzająca termin powinna przyjmować Clock albo datę, a nie wołać LocalDate.now()?
     *   6. Jak znaleźć ostatni dzień bieżącego miesiąca dla daty d?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: weekend?", "false,true,true", () -> exercise1(LocalDate.of(2025, 3, 14)) + ","
                + exercise1(LocalDate.of(2025, 3, 15)) + "," + exercise1(LocalDate.of(2025, 3, 16)));
        Check.equal("ćw. 2: ostatni dzień miesiąca", LocalDate.of(2024, 2, 29), () -> exercise2(LocalDate.of(2024, 2, 10)));
        Check.equal("ćw. 3: następny dzień roboczy", "2025-03-17,2025-03-18",
                () -> exercise3(LocalDate.of(2025, 3, 14)) + "," + exercise3(LocalDate.of(2025, 3, 17)));
        Check.equal("ćw. 4: piątki 13-go w 2026", 3, () -> exercise4(2026));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "false,true,true", () -> solution1(LocalDate.of(2025, 3, 14)) + ","
                + solution1(LocalDate.of(2025, 3, 15)) + "," + solution1(LocalDate.of(2025, 3, 16)));
        Check.equal("ćw. 2 (wzorzec)", LocalDate.of(2024, 2, 29), () -> solution2(LocalDate.of(2024, 2, 10)));
        Check.equal("ćw. 3 (wzorzec)", "2025-03-17,2025-03-18",
                () -> solution3(LocalDate.of(2025, 3, 14)) + "," + solution3(LocalDate.of(2025, 3, 17)));
        Check.equal("ćw. 4 (wzorzec)", 3, () -> solution4(2026));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): czy data wypada w sobotę lub niedzielę? Podpowiedź: getDayOfWeek() i == (enum). */
    static boolean exercise1(LocalDate date) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** ĆWICZENIE 2 (łatwe): ostatni dzień miesiąca, w którym leży data. Dwa sposoby: adjuster albo withDayOfMonth(lengthOfMonth()). */
    static LocalDate exercise2(LocalDate date) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 3 (średnie): najbliższy dzień roboczy PO podanej dacie (pomijaj sobotę i niedzielę; święta pomijamy).
     * Piątek 14.03 → poniedziałek 17.03; poniedziałek 17.03 → wtorek 18.03. Podpowiedź: pętla plusDays(1).
     */
    static LocalDate exercise3(LocalDate date) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /** ĆWICZENIE 4 (trudniejsze): ile razy w danym roku 13. dzień miesiąca wypada w piątek? Podpowiedź: pętla po 12 miesiącach. */
    static int exercise4(int year) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    static LocalDate solution2(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    static LocalDate solution3(LocalDate date) {
        LocalDate next = date.plusDays(1);
        while (solution1(next)) {
            next = next.plusDays(1);
        }
        return next;
    }

    static int solution4(int year) {
        int count = 0;
        for (int month = 1; month <= 12; month++) {
            if (LocalDate.of(year, month, 13).getDayOfWeek() == DayOfWeek.FRIDAY) {
                count++;
            }
        }
        return count;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. „2025-05-10” — plusDays zwraca nowy obiekt, a wynik został zignorowany.
     *   2. „2024-02-29” — marzec ma 31 dni, luty 2024 ma 29 → przycięcie do ostatniego dnia lutego.
     *   3. Porównanie TEKSTÓW (i to w niepełnym formacie "2025-6-1") zamiast dat. Poprawnie:
     *      LocalDate.now(clock).isAfter(LocalDate.of(2025, 6, 1)).
     *   4. LocalTime da 01:00 (bez informacji o zmianie dnia), LocalDateTime da 2025-01-02T01:00.
     *   5. now() zmienia się z dnia na dzień — wynik metody i testów zależy od daty uruchomienia. Z Clock/parametrem
     *      można sprawdzić dowolny scenariusz (np. ostatni dzień roku) w każdej chwili.
     *   6. d.with(TemporalAdjusters.lastDayOfMonth()) albo d.withDayOfMonth(d.lengthOfMonth()).
     */
    // </editor-fold>
}
