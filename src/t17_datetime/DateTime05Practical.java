package t17_datetime;

import helpers.Check;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Data i czas w praktyce — dni robocze, terminy płatności, harmonogramy, YearMonth, kolizje terminów, kalendarz
 *        (working day = dzień roboczy; deadline = termin; schedule = harmonogram; overlap = nakładanie się)
 *
 * W SKRÓCIE:
 *   Typowe zadania biznesowe: „termin płatności = 14 dni roboczych”, „wypłata 10-go, a gdy wypada w weekend —
 *   w piątek wcześniej”, „czy dwa spotkania na siebie nachodzą”, „ile zamówień w każdym miesiącu”. Wszystkie
 *   rozwiązuje się kilkoma klockami: pętla po dniach (datesUntil, plusDays), getDayOfWeek, zbiór świąt,
 *   YearMonth (rok + miesiąc) i reguła nakładania się przedziałów: startA {@code <} koniecB i startB {@code <} koniecA.
 *
 * ANALOGIA: kalendarz w biurze księgowej.
 *   Księgowa ma kalendarz z zaznaczonymi weekendami i świętami (Set{@code <LocalDate>}), liczy „14 dni roboczych” palcem
 *   po kratkach (pętla plusDays) i zbiera faktury do teczek z nazwami miesięcy (YearMonth → lista).
 *
 * JAK TO DZIAŁA:
 *   start.datesUntil(end)          → Stream{@code <LocalDate>} kolejnych dni [start, end)  (Java 9+)
 *   YearMonth.of(2025, 3)          → 2025-03;  ym.atDay(10), ym.atEndOfMonth(), YearMonth.from(date)
 *   nakładanie: a.start().isBefore(b.end()) {@code &&} b.start().isBefore(a.end())
 *
 * SŁÓWKA:
 *   working day = dzień roboczy; holiday = święto; deadline = termin; due date = termin płatności; payday = dzień wypłaty;
 *   schedule = harmonogram; year-month = rok i miesiąc; dates until = daty aż do; overlap = nakładać się;
 *   meeting = spotkanie; slot = przedział (w kalendarzu); calendar = kalendarz; text style = styl tekstu (pełny/skrót).
 *
 * ZOBACZ TEŻ: t17_datetime/DateTime01LocalDateTime … DateTime04ZonesInstant, t16_streams/Streams11GroupingBy (grupowanie),
 *             t35_capstone/Capstone01Library (terminy zwrotów książek).
 * </pre>
 */
public class DateTime05Practical {

    /** HOLIDAYS_2025 = polskie święta ustawowe (dni wolne) w 2025 roku — w prawdziwym programie z konfiguracji/bazy. */
    static final Set<LocalDate> HOLIDAYS_2025 = Set.of(
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 6), LocalDate.of(2025, 4, 20), LocalDate.of(2025, 4, 21),
            LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 3), LocalDate.of(2025, 6, 8), LocalDate.of(2025, 6, 19),
            LocalDate.of(2025, 8, 15), LocalDate.of(2025, 11, 1), LocalDate.of(2025, 11, 11),
            LocalDate.of(2025, 12, 25), LocalDate.of(2025, 12, 26));

    /** isWorkingDay = czy dzień roboczy: nie sobota, nie niedziela, nie święto. */
    static boolean isWorkingDay(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY && !HOLIDAYS_2025.contains(date);
    }

    /** addWorkingDays = dodaj n dni roboczych (liczone od następnego dnia). */
    static LocalDate addWorkingDays(LocalDate start, int n) {
        LocalDate date = start;
        int added = 0;
        while (added < n) {
            date = date.plusDays(1);
            if (isWorkingDay(date)) {
                added++;
            }
        }
        return date;
    }

    /** Meeting = spotkanie od start do end (end wyłącznie — spotkanie 9:00–10:00 nie koliduje z 10:00–11:00). */
    record Meeting(String title, LocalDateTime start, LocalDateTime end) {
        Meeting {
            if (!start.isBefore(end)) {
                throw new IllegalArgumentException("Początek musi być przed końcem: " + start + " / " + end);
            }
        }

        boolean overlaps(Meeting other) {
            return start.isBefore(other.end) && other.start.isBefore(end);
        }
    }

    public static void main(String[] args) {
        title("DateTime05 — data i czas w praktyce");

        workingDays();          // working days = dni robocze
        paymentDeadline();      // payment deadline = termin płatności
        datesUntil();           // dates until = strumień dni
        yearMonth();            // year-month = rok i miesiąc
        meetingOverlap();       // meeting overlap = kolizja spotkań
        groupByMonth();         // group by month = grupowanie po miesiącu
        printCalendar();        // print calendar = wydruk kalendarza
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. DNI ROBOCZE
    // =================================================================================================

    /** 1. Dzień roboczy = nie weekend i nie święto. Święta najlepiej trzymać w zbiorze (szybkie contains). */
    static void workingDays() {
        section("1. Czy to dzień roboczy?");

        for (LocalDate d : List.of(LocalDate.of(2025, 4, 18), LocalDate.of(2025, 4, 19), LocalDate.of(2025, 4, 21))) {
            show(d + " (" + d.getDayOfWeek() + ")", isWorkingDay(d));
        }
        // WYNIK: 2025-04-18 (FRIDAY) → true
        // WYNIK: 2025-04-19 (SATURDAY) → false
        // WYNIK: 2025-04-21 (MONDAY) → false    ← Poniedziałek Wielkanocny

        // PUŁAPKA: święta ruchome (Wielkanoc, Boże Ciało) co roku wypadają w inny dzień — nie wpisuj ich „na sztywno”
        //   na lata; wylicz albo wczytaj z konfiguracji.
    }

    // =================================================================================================
    // 2. TERMIN PŁATNOŚCI
    // =================================================================================================

    /** 2. „14 dni roboczych od wystawienia” — pętla dzień po dniu, liczymy tylko dni robocze. */
    static void paymentDeadline() {
        section("2. Termin: N dni roboczych");

        LocalDate issued = LocalDate.of(2025, 4, 14);           // poniedziałek przed Wielkanocą
        show("+14 dni kalendarzowych", issued.plusDays(14));
        show("+14 dni roboczych", addWorkingDays(issued, 14));
        // WYNIK: +14 dni kalendarzowych → 2025-04-28
        // WYNIK: +14 dni roboczych → 2025-05-06    ← weekendy, Poniedziałek Wielkanocny, 1 i 3 maja pominięte
    }

    // =================================================================================================
    // 3. datesUntil — STRUMIEŃ DNI
    // =================================================================================================

    /** 3. datesUntil (Java 9+) daje Stream kolejnych dat [start, end) — wygodne do liczenia i filtrowania. */
    static void datesUntil() {
        section("3. datesUntil: dni jako strumień");

        LocalDate start = LocalDate.of(2025, 5, 1);
        LocalDate end = LocalDate.of(2025, 6, 1);
        long workingInMay = start.datesUntil(end).filter(DateTime05Practical::isWorkingDay).count();
        List<Integer> mondays = start.datesUntil(end)
                .filter(d -> d.getDayOfWeek() == DayOfWeek.MONDAY)
                .map(LocalDate::getDayOfMonth)
                .collect(Collectors.toList());
        show("dni robocze w maju 2025", workingInMay);
        show("poniedziałki w maju", mondays);
        // WYNIK: dni robocze w maju 2025 → 21    ← 22 dni pn–pt minus święto 1 maja (3 maja to sobota)
        // WYNIK: poniedziałki w maju → [5, 12, 19, 26]
    }

    // =================================================================================================
    // 4. YearMonth
    // =================================================================================================

    /** payday = dzień wypłaty: 10. dzień miesiąca, a gdy to weekend — ostatni piątek przed nim. */
    static LocalDate payday(YearMonth month) {
        LocalDate day = month.atDay(10);
        while (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) {
            day = day.minusDays(1);
        }
        return day;
    }

    /** 4. YearMonth = rok + miesiąc (bez dnia): idealny na „faktury za marzec”, „wypłata za maj”. */
    static void yearMonth() {
        section("4. YearMonth i harmonogram wypłat");

        YearMonth march = YearMonth.of(2025, 3);
        show("YearMonth", march);
        show("długość / ostatni dzień", march.lengthOfMonth() + " / " + march.atEndOfMonth());
        // WYNIK: YearMonth → 2025-03
        // WYNIK: długość / ostatni dzień → 31 / 2025-03-31

        List<String> schedule = new ArrayList<>();
        for (YearMonth ym = YearMonth.of(2025, 4); !ym.isAfter(YearMonth.of(2025, 6)); ym = ym.plusMonths(1)) {
            LocalDate p = payday(ym);
            schedule.add(p + " " + p.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("pl-PL")));
        }
        show("wypłaty kwiecień–czerwiec", schedule);
        // WYNIK: wypłaty kwiecień–czerwiec → [2025-04-10 czw., 2025-05-09 pt., 2025-06-10 wt.]    ← 10.05 to sobota
    }

    // =================================================================================================
    // 5. KOLIZJA SPOTKAŃ
    // =================================================================================================

    /** 5. Dwa przedziały [a1, a2) i [b1, b2) nachodzą na siebie ⇔ a1 {@code <} b2 i b1 {@code <} a2. Jedna linijka zamiast 4 przypadków. */
    static void meetingOverlap() {
        section("5. Czy spotkania nachodzą na siebie?");

        LocalDateTime day = LocalDateTime.of(2025, 3, 14, 0, 0);
        Meeting standup = new Meeting("stand-up", day.withHour(9), day.withHour(9).plusMinutes(15));
        Meeting review = new Meeting("przegląd", day.withHour(9).plusMinutes(10), day.withHour(10));
        Meeting lunch = new Meeting("lunch", day.withHour(10), day.withHour(11));
        show("stand-up × przegląd", standup.overlaps(review));
        show("przegląd × lunch (styk o 10:00)", review.overlaps(lunch));
        // WYNIK: stand-up × przegląd → true
        // WYNIK: przegląd × lunch (styk o 10:00) → false    ← koniec wyłącznie: 10:00 już wolne

        expectThrows("spotkanie odwrotne", () -> new Meeting("błąd", day.withHour(12), day.withHour(11)));
        // WYNIK: ✔ spotkanie odwrotne → rzucono IllegalArgumentException: Początek musi być przed końcem: 2025-03-14T12:00 / 2025-03-14T11:00
    }

    // =================================================================================================
    // 6. GRUPOWANIE PO MIESIĄCU
    // =================================================================================================

    /** 6. YearMonth.from(data) jako klucz grupowania; TreeMap sortuje miesiące chronologicznie. */
    static void groupByMonth() {
        section("6. Zamówienia według miesiąca");

        List<LocalDate> orders = List.of(LocalDate.of(2025, 3, 2), LocalDate.of(2025, 1, 15), LocalDate.of(2025, 3, 30),
                LocalDate.of(2025, 2, 1), LocalDate.of(2025, 3, 5));
        Map<YearMonth, Long> perMonth = orders.stream()
                .collect(Collectors.groupingBy(YearMonth::from, TreeMap::new, Collectors.counting()));
        show("zamówienia w miesiącach", perMonth);
        // WYNIK: zamówienia w miesiącach → {2025-01=1, 2025-02=1, 2025-03=3}
    }

    // =================================================================================================
    // 7. WYDRUK KALENDARZA
    // =================================================================================================

    /** 7. Siatka miesiąca od poniedziałku: puste pola przed 1. dniem = (dzień tygodnia 1. dnia − 1). */
    static void printCalendar() {
        section("7. Kalendarz na marzec 2025");

        YearMonth ym = YearMonth.of(2025, 3);
        System.out.println("   " + ym.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("pl-PL"))));
        System.out.println("   Pn Wt Śr Cz Pt So Nd");
        StringBuilder line = new StringBuilder("   " + "   ".repeat(ym.atDay(1).getDayOfWeek().getValue() - 1));
        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            line.append(String.format("%2d ", day));
            if (ym.atDay(day).getDayOfWeek() == DayOfWeek.SUNDAY) {
                System.out.println(line.toString().stripTrailing());
                line = new StringBuilder("   ");
            }
        }
        if (!line.toString().isBlank()) {
            System.out.println(line.toString().stripTrailing());
        }
        // WYNIK:    marzec 2025
        // WYNIK:    Pn Wt Śr Cz Pt So Nd
        // WYNIK:                    1  2
        // WYNIK:     3  4  5  6  7  8  9
        // WYNIK:    10 11 12 13 14 15 16
        // WYNIK:    17 18 19 20 21 22 23
        // WYNIK:    24 25 26 27 28 29 30
        // WYNIK:    31
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Dzień roboczy: getDayOfWeek() != SATURDAY/SUNDAY i !holidays.contains(date) (Set<LocalDate>).
     *   • N dni roboczych: pętla plusDays(1), licz tylko robocze. Święta ruchome — wyliczaj/wczytuj, nie wpisuj na lata.
     *   • start.datesUntil(end) — Stream dni [start, end) (Java 9+): filter, count, map.
     *   • YearMonth: of(2025, 3), atDay(10), atEndOfMonth(), lengthOfMonth(), plusMonths, YearMonth.from(date).
     *   • Nakładanie przedziałów [a1, a2) i [b1, b2): a1 < b2 && b1 < a2 (koniec wyłącznie).
     *   • Grupowanie po miesiącu: groupingBy(YearMonth::from, TreeMap::new, counting()).
     *   • Nazwy dni: getDayOfWeek().getDisplayName(TextStyle.SHORT/FULL, locale).
     *
     * PYTANIA KONTROLNE:
     *   1. Jak sprawdzić, czy dwa przedziały czasu nachodzą na siebie? Dlaczego koniec „wyłącznie”?
     *   2. Co wypisze:  System.out.println(YearMonth.of(2024, 2).atEndOfMonth());  ?
     *   3. ZNAJDŹ BŁĄD:  LocalDate due = issued.plusDays(14);   // „14 dni roboczych na zapłatę”
     *   4. Co zwraca LocalDate.of(2025, 5, 1).datesUntil(LocalDate.of(2025, 5, 4)).count()?
     *   5. Dlaczego święta trzymamy w Set, a nie w List?
     *   6. Po co YearMonth, skoro jest LocalDate?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: dni robocze w kwietniu 2025", 21L,
                () -> exercise1(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 5, 1)));
        Check.equal("ćw. 2: ostatni dzień roboczy miesiąca", "2025-05-30,2025-08-29",
                () -> exercise2(YearMonth.of(2025, 5)) + "," + exercise2(YearMonth.of(2025, 8)));
        Check.equal("ćw. 3: ile spotkań koliduje z nowym", 2L, () -> {
            LocalDateTime d = LocalDateTime.of(2025, 3, 14, 0, 0);
            List<Meeting> calendar = List.of(new Meeting("a", d.withHour(8), d.withHour(9)),
                    new Meeting("b", d.withHour(9), d.withHour(11)), new Meeting("c", d.withHour(12), d.withHour(13)),
                    new Meeting("d", d.withHour(10).plusMinutes(30), d.withHour(12).plusMinutes(30)));
            return exercise3(calendar, new Meeting("nowe", d.withHour(10), d.withHour(12)));
        });
        Check.equal("ćw. 4: miesiące z największą liczbą zamówień", YearMonth.of(2025, 3),
                () -> exercise4(List.of(LocalDate.of(2025, 3, 2), LocalDate.of(2025, 1, 15), LocalDate.of(2025, 3, 30))));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 21L, () -> solution1(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 5, 1)));
        Check.equal("ćw. 2 (wzorzec)", "2025-05-30,2025-08-29",
                () -> solution2(YearMonth.of(2025, 5)) + "," + solution2(YearMonth.of(2025, 8)));
        Check.equal("ćw. 3 (wzorzec)", 2L, () -> {
            LocalDateTime d = LocalDateTime.of(2025, 3, 14, 0, 0);
            List<Meeting> calendar = List.of(new Meeting("a", d.withHour(8), d.withHour(9)),
                    new Meeting("b", d.withHour(9), d.withHour(11)), new Meeting("c", d.withHour(12), d.withHour(13)),
                    new Meeting("d", d.withHour(10).plusMinutes(30), d.withHour(12).plusMinutes(30)));
            return solution3(calendar, new Meeting("nowe", d.withHour(10), d.withHour(12)));
        });
        Check.equal("ćw. 4 (wzorzec)", YearMonth.of(2025, 3),
                () -> solution4(List.of(LocalDate.of(2025, 3, 2), LocalDate.of(2025, 1, 15), LocalDate.of(2025, 3, 30))));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): policz dni robocze w przedziale [from, to). Podpowiedź: datesUntil + filter + count. */
    static long exercise1(LocalDate from, LocalDate to) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /** ĆWICZENIE 2 (średnie): ostatni dzień roboczy miesiąca (od atEndOfMonth() cofaj się, aż isWorkingDay). */
    static LocalDate exercise2(YearMonth month) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /** ĆWICZENIE 3 (średnie): ile spotkań z kalendarza koliduje z nowym spotkaniem? Podpowiedź: overlaps + count. */
    static long exercise3(List<Meeting> calendar, Meeting candidate) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): miesiąc (YearMonth) z największą liczbą zamówień. Podpowiedź: groupingBy(YearMonth::from,
     * counting()), potem entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey().
     */
    static YearMonth exercise4(List<LocalDate> orders) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static long solution1(LocalDate from, LocalDate to) {
        return from.datesUntil(to).filter(DateTime05Practical::isWorkingDay).count();
    }

    static LocalDate solution2(YearMonth month) {
        LocalDate day = month.atEndOfMonth();
        while (!isWorkingDay(day)) {
            day = day.minusDays(1);
        }
        return day;
    }

    static long solution3(List<Meeting> calendar, Meeting candidate) {
        return calendar.stream().filter(m -> m.overlaps(candidate)).count();
    }

    static YearMonth solution4(List<LocalDate> orders) {
        return orders.stream()
                .collect(Collectors.groupingBy(YearMonth::from, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow()
                .getKey();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. a.start < b.end && b.start < a.end. Koniec „wyłącznie” sprawia, że spotkania „na styk” (9–10 i 10–11)
     *      nie kolidują, a długość przedziału to po prostu end − start.
     *   2. „2024-02-29”.
     *   3. plusDays(14) liczy dni KALENDARZOWE — weekendy i święta się wliczają. Trzeba addWorkingDays(issued, 14).
     *   4. 3 — dni 1, 2 i 3 maja (koniec wyłącznie).
     *   5. Set.contains jest szybkie (haszowanie) i nie ma duplikatów; List.contains przegląda całą listę.
     *   6. YearMonth wyraża „miesiąc w roku” bez udawania dnia (np. 1.) — czytelne klucze grupowania, okresy
     *      rozliczeniowe, daty ważności kart (MM/rr), metody atDay/atEndOfMonth.
     */
    // </editor-fold>
}
