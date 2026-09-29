package t17_datetime;

import helpers.Check;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Strefy czasowe i Instant — ZoneId, ZonedDateTime, OffsetDateTime, zmiana czasu letni/zimowy
 *        (zone = strefa; instant = chwila, punkt na osi czasu; offset = przesunięcie względem UTC; DST = czas letni)
 *
 * W SKRÓCIE:
 *   LocalDateTime „10:00” nie mówi, KIEDY to było — 10:00 w Warszawie i 10:00 w Tokio to różne chwile. Instant to
 *   jednoznaczny punkt na osi czasu (liczony w UTC). ZonedDateTime = data i godzina + strefa (Europe/Warsaw) —
 *   z regułami zmiany czasu. Tę samą chwilę pokazujemy w innej strefie przez withZoneSameInstant. Doba przy zmianie
 *   czasu ma 23 albo 25 godzin — dlatego plusDays(1) ≠ plusHours(24).
 *
 * ANALOGIA: transmisja meczu na żywo.
 *   Gol pada w JEDNEJ chwili (Instant). Kibic w Warszawie widzi na zegarku 21:00, w Nowym Jorku 15:00, w Tokio 5:00
 *   rano (ZonedDateTime w różnych strefach). Każdy ma inną godzinę na zegarku, ale to ten sam gol.
 *
 * JAK TO DZIAŁA:
 *   ZonedDateTime waw = ZonedDateTime.of(LocalDateTime.of(2025, 3, 14, 10, 0), ZoneId.of("Europe/Warsaw"));
 *   waw.withZoneSameInstant(ZoneId.of("Asia/Tokyo"))   → 2025-03-14T18:00+09:00[Asia/Tokyo]  (ta sama chwila)
 *   waw.toInstant()                                     → 2025-03-14T09:00:00Z                (UTC)
 *   Strefy podawaj jako "Kontynent/Miasto" — zawierają historię i reguły zmiany czasu.
 *
 * SŁÓWKA:
 *   zone = strefa; zone id = identyfikator strefy; instant = chwila; offset = przesunięcie; UTC = uniwersalny czas
 *   koordynowany; epoch = epoka (początek liczenia: 1970-01-01T00:00Z); same instant = ta sama chwila; same local =
 *   ta sama godzina na zegarku; DST (daylight saving time) = czas letni; gap = luka (godzina, która nie istnieje);
 *   overlap = nakładka (godzina, która jest dwa razy); flight = lot; arrival = przylot.
 *
 * ZOBACZ TEŻ: t17_datetime/DateTime01LocalDateTime (Clock), t17_datetime/DateTime02PeriodDuration (Duration),
 *             t17_datetime/DateTime05Practical (praktyka), t18_io_files/Io10SimpleLogger (znacznik czasu w logach).
 * </pre>
 */
public class DateTime04ZonesInstant {

    static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

    public static void main(String[] args) {
        title("DateTime04 — strefy czasowe i Instant");

        instantBasics();        // instant basics = podstawy Instant
        zonedDateTime();        // zoned date time = data i czas ze strefą
        sameInstantVsSameLocal(); // same instant vs same local = ta sama chwila kontra ta sama godzina
        daylightSaving();       // daylight saving = zmiana czasu
        offsetDateTime();       // offset date time = data i czas z przesunięciem
        legacyDate();           // legacy Date = stara klasa Date
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. INSTANT
    // =================================================================================================

    /** 1. Instant = liczba sekund (i nanosekund) od 1970-01-01T00:00:00Z. Zawsze w UTC — „Z” na końcu = Zulu = UTC. */
    static void instantBasics() {
        section("1. Instant — punkt na osi czasu");

        show("ofEpochSecond(0)", Instant.ofEpochSecond(0));
        show("ofEpochMilli(1 000 000 000 000)", Instant.ofEpochMilli(1_000_000_000_000L));
        // WYNIK: ofEpochSecond(0) → 1970-01-01T00:00:00Z
        // WYNIK: ofEpochMilli(1 000 000 000 000) → 2001-09-09T01:46:40Z

        Instant start = Instant.parse("2025-03-14T10:00:00Z");
        Instant end = start.plusSeconds(90);
        show("Duration.between", Duration.between(start, end));
        // WYNIK: Duration.between → PT1M30S

        // Instant.now() — bieżąca chwila (wynik zależy od uruchomienia). Do mierzenia czasu wykonania kodu lepszy
        //   System.nanoTime() (nie „skacze” przy synchronizacji zegara systemowego).
        // DOBRA PRAKTYKA: w bazie, logach i API zapisuj chwile jako Instant/UTC; strefę dokładaj dopiero przy wyświetlaniu.
    }

    // =================================================================================================
    // 2. ZonedDateTime
    // =================================================================================================

    /** 2. Ta sama chwila w trzech strefach — withZoneSameInstant przelicza godzinę na zegarku. */
    static void zonedDateTime() {
        section("2. ZonedDateTime: jedna chwila, trzy zegarki");

        ZonedDateTime meeting = ZonedDateTime.of(LocalDateTime.of(2025, 3, 14, 10, 0), WARSAW);
        show("Warszawa", meeting);
        show("Nowy Jork", meeting.withZoneSameInstant(NEW_YORK));
        show("Tokio", meeting.withZoneSameInstant(TOKYO));
        show("jako Instant (UTC)", meeting.toInstant());
        // WYNIK: Warszawa → 2025-03-14T10:00+01:00[Europe/Warsaw]
        // WYNIK: Nowy Jork → 2025-03-14T05:00-04:00[America/New_York]    ← w USA czas letni zaczął się już 9 marca
        // WYNIK: Tokio → 2025-03-14T18:00+09:00[Asia/Tokyo]
        // WYNIK: jako Instant (UTC) → 2025-03-14T09:00:00Z

        // PUŁAPKA: nie używaj stałych przesunięć ("+01:00") zamiast stref — Polska ma +01:00 zimą i +02:00 latem.
        //   ZoneId.of("Europe/Warsaw") zna te reguły.
    }

    // =================================================================================================
    // 3. withZoneSameInstant KONTRA withZoneSameLocal
    // =================================================================================================

    /** 3. SameInstant: ta sama chwila, inna godzina. SameLocal: ta sama godzina na zegarku, INNA chwila. */
    static void sameInstantVsSameLocal() {
        section("3. withZoneSameInstant kontra withZoneSameLocal");

        ZonedDateTime waw = ZonedDateTime.of(LocalDateTime.of(2025, 3, 14, 10, 0), WARSAW);
        ZonedDateTime sameInstant = waw.withZoneSameInstant(NEW_YORK);
        ZonedDateTime sameLocal = waw.withZoneSameLocal(NEW_YORK);
        show("sameInstant", sameInstant.toLocalTime() + ", ta sama chwila? " + sameInstant.toInstant().equals(waw.toInstant()));
        show("sameLocal", sameLocal.toLocalTime() + ", ta sama chwila? " + sameLocal.toInstant().equals(waw.toInstant()));
        // WYNIK: sameInstant → 05:00, ta sama chwila? true
        // WYNIK: sameLocal → 10:00, ta sama chwila? false

        // DOBRA PRAKTYKA: „o której to będzie u nich?” → withZoneSameInstant. withZoneSameLocal tylko wtedy, gdy
        //   godzina ma być „lokalna wszędzie” (np. sklepy w każdym mieście otwierają o 9:00 czasu miejscowego).
    }

    // =================================================================================================
    // 4. ZMIANA CZASU
    // =================================================================================================

    /**
     * 4. W Polsce 30.03.2025 o 2:00 zegary przestawiono na 3:00 — godzina 2:30 NIE ISTNIAŁA (luka). ZonedDateTime
     * przesuwa ją do przodu. Doba 29→30 marca miała 23 godziny.
     */
    static void daylightSaving() {
        section("4. Zmiana czasu: luka i doba 23-godzinna");

        ZonedDateTime inGap = ZonedDateTime.of(LocalDateTime.of(2025, 3, 30, 2, 30), WARSAW);
        show("30.03 02:30 (nie istnieje)", inGap);
        // WYNIK: 30.03 02:30 (nie istnieje) → 2025-03-30T03:30+02:00[Europe/Warsaw]

        ZonedDateTime saturdayNoon = ZonedDateTime.of(LocalDateTime.of(2025, 3, 29, 12, 0), WARSAW);
        show("plusDays(1)", saturdayNoon.plusDays(1));
        show("plusHours(24)", saturdayNoon.plusHours(24));
        show("doba trwała", Duration.between(saturdayNoon, saturdayNoon.plusDays(1)));
        // WYNIK: plusDays(1) → 2025-03-30T12:00+02:00[Europe/Warsaw]    ← „ta sama godzina jutro”
        // WYNIK: plusHours(24) → 2025-03-30T13:00+02:00[Europe/Warsaw]    ← dokładnie 24 h później
        // WYNIK: doba trwała → PT23H

        // PUŁAPKA: przypomnienie „codziennie o 12:00” licz przez plusDays(1), a „co 24 godziny” przez plusHours(24).
        //   W październiku (koniec czasu letniego) jest odwrotnie: godzina 2:30 występuje DWA razy, doba ma 25 h.
    }

    // =================================================================================================
    // 5. OffsetDateTime
    // =================================================================================================

    /** 5. OffsetDateTime = data i godzina + STAŁE przesunięcie (bez reguł strefy). Typowy w API i bazach danych. */
    static void offsetDateTime() {
        section("5. OffsetDateTime — stałe przesunięcie");

        OffsetDateTime odt = OffsetDateTime.of(2025, 7, 1, 10, 0, 0, 0, ZoneOffset.ofHours(2));
        show("OffsetDateTime", odt);
        show("w UTC", odt.withOffsetSameInstant(ZoneOffset.UTC));
        show("parse z API", OffsetDateTime.parse("2025-07-01T08:00:00Z").toInstant().equals(odt.toInstant()));
        // WYNIK: OffsetDateTime → 2025-07-01T10:00+02:00
        // WYNIK: w UTC → 2025-07-01T08:00Z
        // WYNIK: parse z API → true
    }

    // =================================================================================================
    // 6. STARA KLASA Date
    // =================================================================================================

    /**
     * 6. Stare biblioteki używają java.util.Date. Konwersja przez Instant: Date.from(instant), date.toInstant().
     * Nie wypisujemy date.toString() — wynik zależy od strefy komputera.
     */
    static void legacyDate() {
        section("6. Konwersja z/do java.util.Date");

        Instant instant = Instant.parse("2025-03-14T09:00:00Z");
        Date legacy = Date.from(instant);                     // z nowego na stary
        Instant back = legacy.toInstant();                    // ze starego na nowy
        show("ta sama chwila po konwersji", back.equals(instant));
        show("getTime() == toEpochMilli()", legacy.getTime() == instant.toEpochMilli());
        // WYNIK: ta sama chwila po konwersji → true
        // WYNIK: getTime() == toEpochMilli() → true
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Instant — chwila w UTC („Z”); ofEpochSecond/ofEpochMilli/parse; do zapisu i logów.
     *   • ZoneId.of("Europe/Warsaw") — strefa z regułami zmiany czasu (nie stałe "+01:00").
     *   • ZonedDateTime.of(localDateTime, zone); withZoneSameInstant — ta sama chwila w innej strefie;
     *     withZoneSameLocal — ta sama godzina na zegarku (inna chwila).
     *   • Zmiana czasu: luka (godzina nie istnieje → przesunięcie do przodu), doba 23/25 h; plusDays(1) ≠ plusHours(24).
     *   • OffsetDateTime — stałe przesunięcie (API, bazy); Date ↔ Instant: Date.from(i), d.toInstant().
     *   • Przechowuj w UTC, wyświetlaj w strefie użytkownika.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się LocalDateTime od Instant?
     *   2. Co wypisze:  System.out.println(Instant.ofEpochSecond(60));  ?
     *   3. ZNAJDŹ BŁĄD:  ZoneOffset poland = ZoneOffset.ofHours(1);   // „strefa Polski” używana przez cały rok
     *   4. Ile godzin trwała doba od 25.10.2025 12:00 do 26.10.2025 12:00 w Warszawie?
     *   5. Kiedy użyjesz withZoneSameInstant, a kiedy withZoneSameLocal?
     *   6. Dlaczego nie warto wypisywać new Date().toString() w teście?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: Warszawa → Tokio", LocalDateTime.of(2025, 7, 1, 17, 0),
                () -> exercise1(LocalDateTime.of(2025, 7, 1, 10, 0), WARSAW, TOKYO));
        Check.equal("ćw. 2: długość doby 26.10.2025", 25L, () -> exercise2(
                ZonedDateTime.of(LocalDateTime.of(2025, 10, 25, 12, 0), WARSAW),
                ZonedDateTime.of(LocalDateTime.of(2025, 10, 26, 12, 0), WARSAW)));
        Check.equal("ćw. 3: epoch millis → ISO", "2001-09-09T01:46:40Z", () -> exercise3(1_000_000_000_000L));
        Check.equal("ćw. 4: przylot do Nowego Jorku", LocalDateTime.of(2025, 7, 1, 13, 55),
                () -> exercise4(LocalDateTime.of(2025, 7, 1, 10, 15), Duration.ofHours(9).plusMinutes(40)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", LocalDateTime.of(2025, 7, 1, 17, 0),
                () -> solution1(LocalDateTime.of(2025, 7, 1, 10, 0), WARSAW, TOKYO));
        Check.equal("ćw. 2 (wzorzec)", 25L, () -> solution2(
                ZonedDateTime.of(LocalDateTime.of(2025, 10, 25, 12, 0), WARSAW),
                ZonedDateTime.of(LocalDateTime.of(2025, 10, 26, 12, 0), WARSAW)));
        Check.equal("ćw. 3 (wzorzec)", "2001-09-09T01:46:40Z", () -> solution3(1_000_000_000_000L));
        Check.equal("ćw. 4 (wzorzec)", LocalDateTime.of(2025, 7, 1, 13, 55),
                () -> solution4(LocalDateTime.of(2025, 7, 1, 10, 15), Duration.ofHours(9).plusMinutes(40)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): godzina localDateTime w strefie from — jaka to godzina na zegarku w strefie to?
     * Podpowiedź: atZone(from).withZoneSameInstant(to).toLocalDateTime().
     */
    static LocalDateTime exercise1(LocalDateTime localDateTime, ZoneId from, ZoneId to) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /** ĆWICZENIE 2 (łatwe): ile PEŁNYCH godzin minęło między dwiema chwilami? Podpowiedź: Duration.between(...).toHours(). */
    static long exercise2(ZonedDateTime start, ZonedDateTime end) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /** ĆWICZENIE 3 (łatwe): zamień milisekundy od epoki na tekst ISO chwili (Instant.toString()). */
    static String exercise3(long epochMillis) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): samolot startuje z Warszawy o departure (czas warszawski) i leci flightTime.
     * Zwróć czas przylotu na zegarku w Nowym Jorku (LocalDateTime). Podpowiedź: atZone(WARSAW).plus(flightTime)
     * .withZoneSameInstant(NEW_YORK).toLocalDateTime().
     */
    static LocalDateTime exercise4(LocalDateTime departure, Duration flightTime) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static LocalDateTime solution1(LocalDateTime localDateTime, ZoneId from, ZoneId to) {
        return localDateTime.atZone(from).withZoneSameInstant(to).toLocalDateTime();
    }

    static long solution2(ZonedDateTime start, ZonedDateTime end) {
        return Duration.between(start, end).toHours();
    }

    static String solution3(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).toString();
    }

    static LocalDateTime solution4(LocalDateTime departure, Duration flightTime) {
        return departure.atZone(WARSAW)
                .plus(flightTime)
                .withZoneSameInstant(NEW_YORK)
                .toLocalDateTime();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. LocalDateTime to „godzina na zegarku” bez strefy — nie wskazuje jednoznacznej chwili. Instant to konkretny
     *      punkt na osi czasu (UTC), taki sam dla całego świata.
     *   2. „1970-01-01T00:01:00Z”.
     *   3. Stałe przesunięcie +01:00 jest poprawne tylko zimą; latem Polska ma +02:00. Trzeba ZoneId.of("Europe/Warsaw").
     *   4. 25 godzin — 26.10.2025 cofnięto zegary z 3:00 na 2:00.
     *   5. withZoneSameInstant — „ta sama chwila, jaka to godzina tam?” (spotkanie online, lot). withZoneSameLocal —
     *      „ta sama godzina na zegarku, gdziekolwiek” (np. godzina otwarcia sklepów w różnych miastach).
     *   6. Date.toString() używa strefy komputera — ten sam test da inny wynik na innym komputerze lub serwerze.
     */
    // </editor-fold>
}
