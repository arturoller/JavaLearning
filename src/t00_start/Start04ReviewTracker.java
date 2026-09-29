package t00_start;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Dziennik powtórek — co mam dziś powtórzyć?
 *        (review = powtórka; tracker = rejestr, „śledzik” postępów)
 *
 * W SKRÓCIE:
 *   Wpisujesz tutaj, KTÓRĄ lekcję i KIEDY przerobiłeś. Po uruchomieniu program pokaże, które lekcje
 *   masz dziś do powtórki (po 1, 3, 7, 14 i 30 dniach), a kiedy wypada następna powtórka pozostałych.
 *
 * ANALOGIA:
 *   To jak kalendarz przypomnień u dentysty: nie musisz pamiętać terminów — kalendarz powie Ci, kiedy przyjść.
 *
 * JAK TO DZIAŁA:
 *   1. Po przerobieniu lekcji dopisz linię do listy LEARNED (niżej), np.:
 *          {@code learned("t16_streams.Streams01Intro", "2026-09-28"),}
 *      Data w formacie rok-miesiąc-dzień (ISO), taka jak na wydruku LocalDate.
 *   2. Uruchamiaj ten plik codziennie (albo co kilka dni) — zobaczysz listę „DZIŚ” i najbliższe terminy.
 *   3. Dopóki lista LEARNED jest pusta, program pokazuje PRZYKŁAD na wymyślonych danych.
 *
 * SŁÓWKA:
 *   learned = nauczony, przerobiony; due = należny, wypadający (termin); today = dziś; overdue = zaległy;
 *   next = następny; between = pomiędzy; until = do (daty); parse = przetwórz (tekst na datę).
 *
 * ZOBACZ TEŻ: Start03LearningPath (dlaczego powtórki w odstępach działają).
 * </pre>
 */
public class Start04ReviewTracker {

    /** Learned = przerobiona lekcja: nazwa lekcji + data nauki. RECORD — zwięzła klasa na dane (t09_records). */
    record Learned(String lesson, LocalDate date) {
    }

    /** learned = „przerobione”. Skrót do tworzenia wpisu; {@code LocalDate.parse} zamienia tekst "2026-09-28" na datę. */
    static Learned learned(String lesson, String isoDate) {
        return new Learned(lesson, LocalDate.parse(isoDate));
    }

    /**
     * LEARNED = TWOJE przerobione lekcje. Dopisuj tu kolejne linie.
     * PUŁAPKA: wpisy oddziela przecinek, ale po OSTATNIM wpisie przecinka NIE MA (inaczej błąd kompilacji).
     * Żeby włączyć przykładowe wpisy, usuń // na początku linii.
     */
    private static final List<Learned> LEARNED = List.of(
            // learned("t00_start.Start01HowToUse", "2026-09-28"),
            // learned("t16_streams.Streams01Intro", "2026-09-28")
    );

    public static void main(String[] args) {
        title("Start04 — dziennik powtórek");

        LocalDate today = LocalDate.now();                     // now = teraz (dzisiejsza data z zegara komputera)
        List<Learned> entries = LEARNED;
        if (entries.isEmpty()) {
            note("Lista LEARNED jest pusta — pokazuję PRZYKŁAD na wymyślonych danych (względem dzisiejszej daty).");
            entries = List.of(
                    new Learned("przykład: lekcja sprzed 1 dnia", today.minusDays(1)),     // minusDays = odejmij dni
                    new Learned("przykład: lekcja sprzed 5 dni", today.minusDays(5)),
                    new Learned("przykład: lekcja sprzed 14 dni", today.minusDays(14)),
                    new Learned("przykład: lekcja sprzed 45 dni", today.minusDays(45))
            );
        }
        showToday(entries, today);       // show today = pokaż (co na) dziś
        showUpcoming(entries, today);    // show upcoming = pokaż nadchodzące
    }

    /** 1. Lekcje, których termin powtórki wypada DZIŚ. */
    static void showToday(List<Learned> entries, LocalDate today) {
        section("1. Do powtórki DZIŚ (" + today + ")");
        List<String> dueToday = new ArrayList<>();
        for (Learned entry : entries) {
            // ChronoUnit.DAYS.between(a, b) = ile dni minęło od a do b
            long daysAgo = ChronoUnit.DAYS.between(entry.date(), today);
            for (int offset : Start03LearningPath.REVIEW_OFFSETS) {
                if (daysAgo == offset) {
                    dueToday.add(entry.lesson() + "  (powtórka po " + offset + (offset == 1 ? " dniu)" : " dniach)"));
                }
            }
        }
        if (dueToday.isEmpty()) {
            note("Dziś nie ma zaplanowanych powtórek.");
        } else {
            showEach("do powtórki", dueToday);
        }
    }

    /** 2. Kiedy wypada NASTĘPNA powtórka każdej lekcji (albo czy cykl powtórek już się zakończył). */
    static void showUpcoming(List<Learned> entries, LocalDate today) {
        section("2. Następne powtórki");
        for (Learned entry : entries) {
            LocalDate next = null;
            for (int offset : Start03LearningPath.REVIEW_OFFSETS) {
                LocalDate due = entry.date().plusDays(offset);
                if (due.isAfter(today)) {                        // isAfter = czy jest po (dacie)
                    next = due;
                    break;                                       // pierwsza przyszła powtórka — kończymy szukanie
                }
            }
            if (next == null) {
                show(entry.lesson(), "cykl powtórek zakończony (minęło ponad 30 dni) — powtarzaj raz na kilka miesięcy");
            } else {
                String dayName = next.getDayOfWeek().getDisplayName(TextStyle.FULL, Start03LearningPath.POLISH);
                show(entry.lesson(), next + " (" + dayName + ")");
            }
        }
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Po lekcji dopisz: learned("pakiet.Klasa", "RRRR-MM-DD"),
     *   • Uruchom plik — sekcja 1 pokaże, co powtórzyć dziś, sekcja 2 — najbliższe terminy.
     *
     * PYTANIA KONTROLNE:
     *   1. W jakim formacie wpisujesz datę w learned(...)?
     *   2. Co program pokazuje, gdy lista LEARNED jest pusta?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. ISO: rok-miesiąc-dzień, np. "2026-09-28" (tak parsuje LocalDate.parse).
     *   2. Przykład na wymyślonych lekcjach, liczony względem dzisiejszej daty.
     */
    // </editor-fold>
}
