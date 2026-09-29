package t00_start;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Ścieżka nauki i plan powtórek
 *        (learning path = ścieżka nauki; spaced repetition = powtórki w odstępach)
 *
 * W SKRÓCIE:
 *   Kurs ma 29 działów (t00–t28), ułożonych tak, że każdy korzysta tylko z wcześniejszych — idź po kolei.
 *   Po każdej lekcji rób powtórki w rosnących odstępach — to najskuteczniejszy sposób na trwałą wiedzę.
 *
 * ANALOGIA:
 *   Pamięć jest jak ścieżka w trawie: jedno przejście zostawia ślad na chwilę, a kilka przejść w odstępach
 *   wydeptuje trwałą dróżkę. 10 powtórek jednego dnia daje mniej niż 5 powtórek rozłożonych na miesiąc.
 *
 * JAK TO DZIAŁA:
 *   Nauka jednej lekcji (ok. 30–60 minut):
 *   1. PRZECZYTAJ nagłówek lekcji i ŚCIĄGĘ na końcu (2 min) — wiesz, czego szukasz.
 *   2. PRZEJDŹ sekcje: czytaj komentarz → PRZEWIDŹ wynik → uruchom → porównaj z „WYNIK:”.
 *      Przewidywanie PRZED uruchomieniem jest kluczowe — wtedy mózg naprawdę pracuje.
 *   3. ZMIEŃ coś w kodzie (inna liczba, inny warunek) i sprawdź, czy rozumiesz, co się stanie.
 *   4. ODPOWIEDZ na PYTANIA KONTROLNE bez patrzenia w kod — dopiero potem rozwiń odpowiedzi.
 *   5. ROZWIĄŻ ĆWICZENIA. Rozwiązania wzorcowe rozwijaj dopiero po własnej próbie.
 *
 *   Powtórka (5–10 minut) — w dni wyznaczone przez plan (po 1, 3, 7, 14 i 30 dniach):
 *   1. NAJPIERW odpowiedz na PYTANIA KONTROLNE z pamięci (bez ściągi!) — to wysiłek przypominania
 *      sprawia, że wiedza się utrwala.
 *   2. DOPIERO POTEM przeczytaj ŚCIĄGĘ i sprawdź, czego zabrakło.
 *   3. Rozwiąż jedno ćwiczenie od nowa (skasuj swoje stare rozwiązanie).
 *   Jeśli czegoś nie pamiętasz — wróć do tej sekcji lekcji i zacznij odstępy od nowa.
 *   Co i kiedy powtarzać, pokaże Ci Start04ReviewTracker.
 *
 * DOBRA PRAKTYKA:
 *   • Ucz się aktywnie: pisz kod sam, a nie tylko czytaj. Przepisz przykład z pamięci w osobnym pliku.
 *   • Tłumacz na głos (albo „gumowej kaczce”) — jeśli potrafisz wyjaśnić, to rozumiesz.
 *   • Mieszaj tematy przy powtórkach (np. streamy + kolekcje) — mózg uczy się rozróżniać, kiedy czego użyć.
 *
 * SŁÓWKA:
 *   learning path = ścieżka nauki; stage = etap; lesson = lekcja; review = powtórka, przegląd;
 *   schedule = harmonogram; spaced repetition = powtórki w odstępach; rubber duck = gumowa kaczka;
 *   offset = przesunięcie; goal = cel; depends on = zależy od.
 *
 * ZOBACZ TEŻ: Start01HowToUse (budowa lekcji), Start04ReviewTracker (co dziś powtórzyć).
 * </pre>
 */
public class Start03LearningPath {

    /**
     * Stage = etap nauki. To RECORD (rekord) — zwięzła klasa na dane (t09_records).
     * Zdefiniowany wewnątrz klasy (rekord zagnieżdżony), bo potrzebny tylko tutaj.
     * name = nazwa; packages = pakiety; goal = cel; dependsOn = zależy od (co trzeba znać wcześniej).
     */
    record Stage(String name, List<String> packages, String goal, String dependsOn) {
    }

    /** PATH = ścieżka. Etapy w kolejności numerów działów. List.of = utwórz niemodyfikowalną listę z podanych elementów. */
    private static final List<Stage> PATH = List.of(
            new Stage("Etap 1 — Fundamenty",
                    List.of("t01_basics", "t02_controlflow", "t03_arrays", "t04_strings", "t05_methods"),
                    "typy, operatory, pętle, tablice, napisy, metody",
                    "nic — od tego zaczynamy (po t00_start)"),
            new Stage("Etap 2 — Programowanie obiektowe",
                    List.of("t06_oop_basics", "t07_inheritance_polymorphism", "t08_enums", "t09_records"),
                    "klasy, obiekty, dziedziczenie, interfejsy, polimorfizm, enumy, rekordy",
                    "Etap 1"),
            new Stage("Etap 3 — Solidny kod",
                    List.of("t10_exceptions", "t11_generics", "t12_collections"),
                    "wyjątki, typy generyczne, kolekcje (listy, zbiory, mapy, kolejki)",
                    "Etap 2"),
            new Stage("Etap 4 — Przygotowanie do streamów",
                    List.of("t13_lambdas", "t14_optional", "t15_numbers"),
                    "lambdy i interfejsy funkcyjne, Optional, BigDecimal i pieniądze",
                    "Etap 3"),
            new Stage("Etap 5 — STREAMY (największy dział)",
                    List.of("t16_streams"),
                    "przetwarzanie kolekcji potokami: filter, map, sorted, reduce, kolektory, grupowanie",
                    "Etap 4 (lambdy, Optional, BigDecimal) i kolekcje z Etapu 3"),
            new Stage("Etap 6 — Biblioteka standardowa w praktyce",
                    List.of("t17_datetime", "t18_io_files"),
                    "daty i czas, pliki (CSV, JSON, Properties)",
                    "Etap 5"),
            new Stage("Etap 7 — Pod maską kodu",
                    List.of("t19_annotations_reflection", "t20_lombok"),
                    "adnotacje, refleksja, Lombok (co generuje za Ciebie)",
                    "Etap 2 i 6"),
            new Stage("Etap 8 — Współbieżność",
                    List.of("t21_concurrency"),
                    "wątki, synchronizacja, ExecutorService, CompletableFuture",
                    "Etap 4 i 5 (lambdy, streamy)"),
            new Stage("Etap 9 — Projektowanie i jakość",
                    List.of("t22_design_patterns", "t23_modern_java", "t24_algorithms", "t25_testing",
                            "t26_jvm", "t27_clean_code_pitfalls"),
                    "wzorce projektowe, nowości Javy, algorytmy, testowanie, JVM, czysty kod",
                    "Etapy 1–8"),
            new Stage("Etap 10 — Projekty łączące",
                    List.of("t28_capstone"),
                    "małe aplikacje łączące wiele tematów naraz",
                    "wszystko powyżej")
    );

    /** REVIEW_OFFSETS = po ilu dniach od nauki robić kolejne powtórki. */
    static final int[] REVIEW_OFFSETS = {1, 3, 7, 14, 30};

    /** POLISH = polskie ustawienia regionalne — do nazw dni tygodnia po polsku. */
    static final Locale POLISH = Locale.forLanguageTag("pl-PL");   // forLanguageTag = z oznaczenia języka

    public static void main(String[] args) {
        title("Start03 — ścieżka nauki i plan powtórek");

        showPath();                               // show path = pokaż ścieżkę
        showReviewSchedule(LocalDate.now());      // show review schedule = pokaż harmonogram powtórek (od dziś)
    }

    /** 1. Wypisuje etapy nauki w kolejności. */
    static void showPath() {
        section("1. Etapy nauki");
        for (Stage stage : PATH) {                // for-each: „dla każdego etapu w ścieżce”
            System.out.println(stage.name());
            show("   pakiety", String.join(", ", stage.packages()));   // String.join = połącz napisy separatorem
            show("   cel", stage.goal());
            show("   wymaga", stage.dependsOn());
        }
        note("Dział t00_start czytasz na początku; na t16_streams zaplanuj najwięcej czasu.");
    }

    /**
     * 2. Harmonogram powtórek od podanej daty.
     * <p>
     * {@code plusDays(n)} = dodaj n dni. Zwraca NOWĄ datę — LocalDate jest niezmienny (immutable).
     * PUŁAPKA: samo {@code start.plusDays(1);} bez przypisania do zmiennej NIC nie zmienia (t17_datetime).
     * <p>
     * {@code getDayOfWeek()} = pobierz dzień tygodnia (enum DayOfWeek, np. TUESDAY).
     * {@code getDisplayName(TextStyle.FULL, POLISH)} = pobierz nazwę do wyświetlenia: pełną, po polsku („wtorek”).
     */
    static void showReviewSchedule(LocalDate start) {
        section("2. Plan powtórek dla lekcji przerobionej dzisiaj (" + start + ")");
        for (int days : REVIEW_OFFSETS) {
            LocalDate reviewDay = start.plusDays(days);
            String dayName = reviewDay.getDayOfWeek().getDisplayName(TextStyle.FULL, POLISH);
            show("powtórka po " + days + (days == 1 ? " dniu" : " dniach"), reviewDay + " (" + dayName + ")");
        }
        note("Powtórka = PYTANIA KONTROLNE z pamięci → dopiero potem ŚCIĄGA → jedno ćwiczenie od nowa.");
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Kolejność: fundamenty → OOP → wyjątki/generyki/kolekcje → lambdy/Optional/BigDecimal → STREAMY → reszta.
     *   • Lekcja: przewiduj wynik PRZED uruchomieniem, zmieniaj kod, odpowiadaj na pytania bez patrzenia.
     *   • Powtórki po 1, 3, 7, 14, 30 dniach: najpierw pytania z pamięci, potem ściąga.
     *
     * PYTANIA KONTROLNE:
     *   1. Po ilu dniach robisz kolejne powtórki?
     *   2. Dlaczego na powtórce najpierw odpowiadasz na pytania, a dopiero potem czytasz ściągę?
     *   3. Które działy trzeba znać przed streamami?
     *   4. Co wypisze:  System.out.println(LocalDate.of(2026, 1, 31).plusDays(1));  ?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Po 1, 3, 7, 14 i 30 dniach.
     *   2. Bo wysiłek przypominania (bez podpowiedzi) utrwala wiedzę; czytanie ściągi najpierw daje złudzenie, że „wiem”.
     *   3. Kolekcje (t12), generyki (t11), lambdy (t13), Optional (t14), BigDecimal (t15).
     *   4. 2026-02-01 — plusDays sam przechodzi na następny miesiąc.
     */
    // </editor-fold>
}
