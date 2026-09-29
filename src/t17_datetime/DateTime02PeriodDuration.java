package t17_datetime;

import helpers.Check;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Period, Duration, ChronoUnit — ile czasu minęło, ile zostało
 *        (period = okres (lata, miesiące, dni); duration = czas trwania (godziny, minuty, sekundy); unit = jednostka)
 *
 * W SKRÓCIE:
 *   Period mierzy odstęp KALENDARZOWY: „2 lata, 3 miesiące i 5 dni” — do dat (wiek, staż, okres umowy).
 *   Duration mierzy odstęp „ZEGAROWY”: „1 godzina 30 minut” — do godzin i chwil (czas pracy, długość filmu).
 *   ChronoUnit.X.between(a, b) daje CAŁKOWITĄ liczbę jednostek: ile dni, ile tygodni, ile minut.
 *   Pułapka: Period.getDays() to tylko „reszta dni” — nie łączna liczba dni!
 *
 * ANALOGIA: urodziny kontra stoper.
 *   Period to pytanie „ile masz lat?” — liczysz w latach i miesiącach kalendarza (lata mają różną długość, i dobrze).
 *   Duration to stoper — liczy równe sekundy, nie interesują go miesiące.
 *
 * JAK TO DZIAŁA:
 *   Period.between(LocalDate.of(2000, 5, 20), LocalDate.of(2025, 3, 14))  → P24Y9M22D (24 lata, 9 mies., 22 dni)
 *   Duration.between(LocalTime.of(8, 0), LocalTime.of(16, 30))           → PT8H30M
 *   ChronoUnit.DAYS.between(a, b)                                        → łączna liczba dni (long)
 *   Zapis ISO: P = period, T oddziela część czasową: PT1H30M = 1 h 30 min.
 *
 * SŁÓWKA:
 *   period = okres; duration = czas trwania; between = pomiędzy; unit = jednostka; chrono = czasowy (od gr. chronos);
 *   part = część (toHoursPart = część godzinowa); normalized = znormalizowany (12 miesięcy → 1 rok); age = wiek;
 *   until = do (aż do); unsupported = nieobsługiwany; worked = przepracowany; interval = przedział.
 *
 * ZOBACZ TEŻ: t17_datetime/DateTime01LocalDateTime (daty), t17_datetime/DateTime04ZonesInstant (Duration i strefy),
 *             t17_datetime/DateTime05Practical (terminy, dni robocze), t04_strings/Strings04Formatting (format liczb).
 * </pre>
 */
public class DateTime02PeriodDuration {

    public static void main(String[] args) {
        title("DateTime02 — Period, Duration, ChronoUnit");

        periodBetween();        // period between = okres pomiędzy
        periodArithmetic();     // period arithmetic = arytmetyka okresów
        durationBasics();       // duration basics = podstawy Duration
        chronoUnitBetween();    // ChronoUnit between = ChronoUnit pomiędzy
        getDaysPitfall();       // getDays pitfall = pułapka getDays
        wrongTypePitfall();     // wrong type pitfall = pułapka złego typu
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. Period.between — WIEK, STAŻ
    // =================================================================================================

    /** 1. Period.between(od, do) — wynik w latach, miesiącach i dniach, jak liczy człowiek z kalendarzem. */
    static void periodBetween() {
        section("1. Period.between: wiek");

        LocalDate birth = LocalDate.of(2000, 5, 20);
        LocalDate today = LocalDate.of(2025, 3, 14);
        Period age = Period.between(birth, today);
        show("Period", age);
        show("lata / miesiące / dni", age.getYears() + " / " + age.getMonths() + " / " + age.getDays());
        // WYNIK: Period → P24Y9M22D
        // WYNIK: lata / miesiące / dni → 24 / 9 / 22

        show("wiek w latach", Period.between(birth, LocalDate.of(2025, 5, 20)).getYears());
        // WYNIK: wiek w latach → 25    ← w dniu urodzin rok jest już „pełny”
    }

    // =================================================================================================
    // 2. ARYTMETYKA OKRESÓW
    // =================================================================================================

    /** 2. Period.of..., plus do daty, normalized() — 14 miesięcy to 1 rok i 2 miesiące. */
    static void periodArithmetic() {
        section("2. Tworzenie i dodawanie okresów");

        Period contract = Period.ofMonths(14);
        show("ofMonths(14)", contract);
        show("normalized()", contract.normalized());
        // WYNIK: ofMonths(14) → P14M
        // WYNIK: normalized() → P1Y2M

        LocalDate start = LocalDate.of(2025, 3, 1);
        show("koniec umowy", start.plus(contract).minusDays(1));
        show("start + P1Y2M3D", start.plus(Period.of(1, 2, 3)));
        // WYNIK: koniec umowy → 2026-04-30
        // WYNIK: start + P1Y2M3D → 2026-05-04
    }

    // =================================================================================================
    // 3. Duration — GODZINY I MINUTY
    // =================================================================================================

    /** 3. Duration mierzy czas „zegarowy”. toMinutes() — łącznie; toHoursPart()/toMinutesPart() (Java 9+) — części. */
    static void durationBasics() {
        section("3. Duration: czas pracy");

        Duration work = Duration.between(LocalTime.of(8, 15), LocalTime.of(16, 45));
        show("Duration", work);
        show("toMinutes() — łącznie", work.toMinutes());
        show("toHoursPart() h toMinutesPart() min", work.toHoursPart() + " h " + work.toMinutesPart() + " min");
        // WYNIK: Duration → PT8H30M
        // WYNIK: toMinutes() — łącznie → 510
        // WYNIK: toHoursPart() h toMinutesPart() min → 8 h 30 min

        Duration film = Duration.ofMinutes(135);
        show("ofMinutes(135)", film);
        show("film + 20 min reklam", film.plusMinutes(20));
        show("seans od 19:40 kończy się o", LocalTime.of(19, 40).plus(film.plusMinutes(20)));
        // WYNIK: ofMinutes(135) → PT2H15M
        // WYNIK: film + 20 min reklam → PT2H35M
        // WYNIK: seans od 19:40 kończy się o → 22:15

        Duration overnight = Duration.between(LocalDateTime.of(2025, 3, 14, 22, 0), LocalDateTime.of(2025, 3, 15, 6, 30));
        show("nocna zmiana", overnight);
        // WYNIK: nocna zmiana → PT8H30M    ← przez północ trzeba użyć LocalDateTime (z datą)
    }

    // =================================================================================================
    // 4. ChronoUnit.between — ŁĄCZNA LICZBA JEDNOSTEK
    // =================================================================================================

    /** 4. ChronoUnit.DAYS/WEEKS/MONTHS/HOURS.between — ile PEŁNYCH jednostek mieści się między datami. */
    static void chronoUnitBetween() {
        section("4. ChronoUnit.X.between");

        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 3, 14);
        show("DAYS.between", ChronoUnit.DAYS.between(from, to));
        show("WEEKS.between", ChronoUnit.WEEKS.between(from, to));
        show("MONTHS.between", ChronoUnit.MONTHS.between(from, to));
        show("odwrotnie (to → from)", ChronoUnit.DAYS.between(to, from));
        // WYNIK: DAYS.between → 72
        // WYNIK: WEEKS.between → 10
        // WYNIK: MONTHS.between → 2
        // WYNIK: odwrotnie (to → from) → -72    ← wynik ze znakiem: data „od” późniejsza → ujemny

        show("from.until(to, DAYS)", from.until(to, ChronoUnit.DAYS));
        // WYNIK: from.until(to, DAYS) → 72    ← to samo, zapisane „od strony” daty
    }

    // =================================================================================================
    // 5. PUŁAPKA: Period.getDays()
    // =================================================================================================

    /** 5. Period.getDays() to NIE łączna liczba dni — tylko „reszta” po odjęciu lat i miesięcy. */
    static void getDaysPitfall() {
        section("5. Pułapka: Period.getDays() to nie „ile dni”");

        LocalDate a = LocalDate.of(2025, 1, 10);
        LocalDate b = LocalDate.of(2025, 3, 15);
        show("Period.between(a, b)", Period.between(a, b));
        show("Period.getDays()", Period.between(a, b).getDays());
        show("ChronoUnit.DAYS.between", ChronoUnit.DAYS.between(a, b));
        // WYNIK: Period.between(a, b) → P2M5D
        // WYNIK: Period.getDays() → 5    ← tylko „5 dni” z „2 miesięcy i 5 dni”
        // WYNIK: ChronoUnit.DAYS.between → 64    ← łączna liczba dni

        // DOBRA PRAKTYKA: „ile dni do…” → ChronoUnit.DAYS.between; „ile masz lat” → Period.between(...).getYears().
    }

    // =================================================================================================
    // 6. PUŁAPKA: Duration z datami
    // =================================================================================================

    /** 6. Duration potrzebuje GODZIN — LocalDate ich nie ma, więc Duration.between(dataA, dataB) rzuca wyjątek. */
    static void wrongTypePitfall() {
        section("6. Pułapka: Duration.between na LocalDate");

        expectThrows("Duration.between(LocalDate, LocalDate)",
                () -> Duration.between(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2)));
        // WYNIK: ✔ Duration.between(LocalDate, LocalDate) → rzucono UnsupportedTemporalTypeException: Unsupported unit: Seconds

        show("Duration.ofDays(1)", Duration.ofDays(1));
        // WYNIK: Duration.ofDays(1) → PT24H    ← w Duration „dzień” to zawsze 24 h (a przy zmianie czasu doba ma 23 lub 25 h!)
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Period — lata/miesiące/dni (kalendarz): Period.between(dataA, dataB), getYears(), ofMonths(14).normalized().
     *   • Duration — godziny/minuty/sekundy (zegar): Duration.between(czasA, czasB), toMinutes(), toHoursPart().
     *   • ChronoUnit.DAYS.between(a, b) — łączna liczba dni (ze znakiem); a.until(b, DAYS) — to samo.
     *   • Period.getDays() ≠ łączna liczba dni (to tylko reszta).
     *   • Duration.between(LocalDate, LocalDate) → UnsupportedTemporalTypeException; przez północ → LocalDateTime.
     *   • Zapis ISO: P1Y2M3D, PT8H30M (T = część czasowa).
     *
     * PYTANIA KONTROLNE:
     *   1. Czego użyjesz do policzenia wieku osoby, a czego do czasu trwania meczu?
     *   2. Co wypisze:  System.out.println(Period.between(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 3)).getDays());  ?
     *   3. ZNAJDŹ BŁĄD:  long days = Period.between(start, end).getDays();   // „ile dni trwał urlop”
     *   4. Co wypisze:  System.out.println(Duration.ofMinutes(90));  ?
     *   5. Dlaczego Duration.between(LocalTime.of(22, 0), LocalTime.of(6, 0)) nie da 8 godzin?
     *   6. Co oznacza zapis P1Y6M?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: wiek", "17,18", () -> exercise1(LocalDate.of(2007, 6, 15), LocalDate.of(2025, 6, 14)) + ","
                + exercise1(LocalDate.of(2007, 6, 15), LocalDate.of(2025, 6, 15)));
        Check.equal("ćw. 2: dni do wakacji", 108L, () -> exercise2(LocalDate.of(2025, 3, 14), LocalDate.of(2025, 6, 30)));
        Check.equal("ćw. 3: format czasu", "2 h 05 min", () -> exercise3(Duration.ofMinutes(125)));
        Check.equal("ćw. 4: minuty pracy z przedziałów", 465L, () -> exercise4(List.of("08:00-12:30", "13:00-16:15")));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "17,18", () -> solution1(LocalDate.of(2007, 6, 15), LocalDate.of(2025, 6, 14)) + ","
                + solution1(LocalDate.of(2007, 6, 15), LocalDate.of(2025, 6, 15)));
        Check.equal("ćw. 2 (wzorzec)", 108L, () -> solution2(LocalDate.of(2025, 3, 14), LocalDate.of(2025, 6, 30)));
        Check.equal("ćw. 3 (wzorzec)", "2 h 05 min", () -> solution3(Duration.ofMinutes(125)));
        Check.equal("ćw. 4 (wzorzec)", 465L, () -> solution4(List.of("08:00-12:30", "13:00-16:15")));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): wiek w pełnych latach w dniu date. Podpowiedź: Period.between(...).getYears(). */
    static int exercise1(LocalDate birth, LocalDate date) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** ĆWICZENIE 2 (łatwe): ile dni od from do to (łącznie). Uwaga na pułapkę z sekcji 5! */
    static long exercise2(LocalDate from, LocalDate to) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): sformatuj czas trwania jako "H h MM min" (minuty zawsze dwucyfrowe): 125 min → "2 h 05 min".
     * Podpowiedź: String.format("%d h %02d min", d.toHours(), d.toMinutesPart()).
     */
    static String exercise3(Duration duration) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zsumuj minuty pracy z przedziałów "HH:mm-HH:mm". Podpowiedź: split("-"),
     * LocalTime.parse, Duration.between, Duration.plus, na końcu toMinutes().
     */
    static long exercise4(List<String> intervals) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(LocalDate birth, LocalDate date) {
        return Period.between(birth, date).getYears();
    }

    static long solution2(LocalDate from, LocalDate to) {
        return ChronoUnit.DAYS.between(from, to);
    }

    static String solution3(Duration duration) {
        return String.format("%d h %02d min", duration.toHours(), duration.toMinutesPart());
    }

    static long solution4(List<String> intervals) {
        Duration total = Duration.ZERO;
        for (String interval : intervals) {
            String[] parts = interval.split("-");
            total = total.plus(Duration.between(LocalTime.parse(parts[0]), LocalTime.parse(parts[1])));
        }
        return total.toMinutes();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Wiek — Period.between(dataUrodzenia, dziś).getYears(). Mecz — Duration (godziny, minuty).
     *   2. „2” — okres to P1M2D (1 miesiąc i 2 dni), getDays() zwraca tylko 2.
     *   3. getDays() to tylko reszta dni po latach i miesiącach — urlop 1.07–10.08 dałby 9 zamiast 40.
     *      Poprawnie: ChronoUnit.DAYS.between(start, end) (+ 1, jeśli liczymy oba dni włącznie).
     *   4. „PT1H30M”.
     *   5. LocalTime nie zna daty — 06:00 jest „wcześniej” niż 22:00, więc wynik to -16 godzin (PT-16H).
     *      Przez północ trzeba użyć LocalDateTime z datami.
     *   6. Okres 1 roku i 6 miesięcy.
     */
    // </editor-fold>
}
