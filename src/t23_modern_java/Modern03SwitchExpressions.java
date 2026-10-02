package t23_modern_java;

import helpers.Check;
import helpers.model.OrderStatus;
import java.util.List;
import java.util.Locale;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wyrażenia switch — strzałki, yield, wyczerpywalność (Java 14, JEP 361)
 *        (switch expression = wyrażenie switch; arrow = strzałka; exhaustive = wyczerpujący)
 *
 * W SKRÓCIE:
 *   Stary switch był instrukcją z „przechodzeniem dalej” (fall-through) i wymagał break. Od Javy 14 switch może być
 *   WYRAŻENIEM (zwraca wartość), zapisywanym ze strzałką {@code case X -> wartość}, bez break i bez przechodzenia dalej.
 *   Dla enumów kompilator sprawdza, czy obsłużyłeś wszystkie stałe.
 *
 * ANALOGIA:
 *   Stary switch to rząd drzwi na korytarzu bez ścian: wchodzisz do pokoju nr 3 i jeśli nie zamkniesz za sobą
 *   drzwi (break), idziesz dalej do pokoju 4, 5 i 6. Nowy switch to automat z biletami: wciskasz przycisk, dostajesz
 *   DOKŁADNIE jeden bilet i koniec. A „wyczerpywalność” to inspektor, który sprawdza, czy na automacie
 *   jest przycisk dla każdego rodzaju biletu.
 *
 * JAK TO DZIAŁA:
 *   1. Strzałka {@code case A, B -> ...} — kilka etykiet po przecinku, po wykonaniu jednej gałęzi switch się kończy.
 *   2. Switch jako wyrażenie: {@code var x = switch (v) { case 1 -> "a"; default -> "b"; };} — średnik na końcu!
 *   3. Gałąź może być: wyrażeniem, blokiem {...} (wartość zwracasz przez yield) albo throw.
 *   4. Wyrażenie MUSI zwrócić wartość dla każdego możliwego argumentu: dla enuma wystarczy wymienić wszystkie stałe,
 *      dla int/String potrzebny jest default.
 *   5. Obie składnie (strzałka i dwukropek) działają zarówno w instrukcji, jak i w wyrażeniu — ale
 *      NIE wolno ich mieszać w jednym switchu. Strzałka nie ma przechodzenia dalej; dwukropek ma.
 *
 *   Historia: podgląd w Javie 12 (JEP 325) i 13 (JEP 354), wersja ostateczna w Javie 14 (JEP 361).
 *   Wzorce w switch: podgląd w Javie 17–20, wersja ostateczna w Javie 21 (JEP 441).
 *
 * SŁÓWKA:
 *   switch = przełącznik; case = przypadek; default = domyślnie (gdy nic nie pasuje); yield = „zwróć z bloku”
 *   (dosłownie: oddaj, wydaj plon); fall-through = przechodzenie do kolejnych gałęzi; break = przerwij;
 *   label = etykieta; exhaustive = wyczerpujący (pokrywa wszystkie możliwości); guard = strażnik.
 *
 * ZOBACZ TEŻ: t02_controlflow/Control02Switch (klasyczny switch), t08_enums/Enums01Basics (enumy),
 *   t23_modern_java/Modern05RecordsSealedPatterns (klasy zapieczętowane i wzorce),
 *   t23_modern_java/Modern07WhatsNextJava21 (switch ze wzorcami w Javie 21)
 * </pre>
 */
public class Modern03SwitchExpressions {

    /** Pomocniczy enum do pokazania wyczerpywalności (level = poziom). */
    enum Level {
        LOW, MEDIUM, HIGH
    }

    public static void main(String[] args) {
        title("Modern03 — wyrażenia switch");

        oldSwitchPitfall();          // old switch pitfall = pułapka starego switcha
        arrowStatement();            // arrow statement = instrukcja ze strzałką
        switchAsExpression();        // switch as expression = switch jako wyrażenie
        exhaustiveEnums();           // exhaustive enums = wyczerpywalność dla enumów
        supportedTypes();            // supported types = obsługiwane typy
        nullInSwitch();              // null in switch = null w switchu
        resultTypePitfall();         // result type pitfall = pułapka typu wyniku
        beforeAfterRewrites();       // before/after rewrites = przepisywanie przed i po
        syntaxPitfalls();            // syntax pitfalls = pułapki składni
        patternsPreview();           // patterns preview = wzorce (zapowiedź)
        exercises();                 // exercises = ćwiczenia
    }

    /** Pomocnicza: prosta nazwa klasy z czasu wykonania. */
    static String typeOf(Object value) {   // type of = typ czegoś
        return value.getClass().getSimpleName();
    }

    // =================================================================================================
    // 1. PUŁAPKA STAREGO SWITCHA
    // =================================================================================================

    /**
     * Stary switch z zapomnianym break. Adnotacja pomija ostrzeżenie kompilatora o przechodzeniu dalej —
     * normalnie -Xlint:fallthrough by je zgłosił, bo to jeden z najczęstszych błędów w Javie.
     */
    @SuppressWarnings("fallthrough")
    static String oldBuggyName(int day) {   // old buggy name = stara błędna nazwa
        String name = "inny";
        switch (day) {
            case 1:
                name = "poniedziałek";       // brak break → wykona się też następna gałąź!
            case 2:
                name = "wtorek";             // brak break → i ta też
            case 3:
                name = "środa";
                break;
            default:
                name = "inny dzień";
        }
        return name;
    }

    /**
     * 1. Stary switch (instrukcja z dwukropkami) „spada” przez kolejne gałęzie, dopóki nie napotka break.
     * Czasem jest to zamierzone, ale częściej to błąd, który kompiluje się i działa — tylko źle.
     */
    static void oldSwitchPitfall() {
        section("1. Pułapka starego switcha: brak break");

        show("oldBuggyName(1)", oldBuggyName(1));
        // WYNIK: oldBuggyName(1) → środa
        show("oldBuggyName(2)", oldBuggyName(2));
        // WYNIK: oldBuggyName(2) → środa
        show("oldBuggyName(3)", oldBuggyName(3));
        // WYNIK: oldBuggyName(3) → środa
        show("oldBuggyName(9)", oldBuggyName(9));
        // WYNIK: oldBuggyName(9) → inny dzień

        // PUŁAPKA: dla dnia 1 oczekiwałeś „poniedziałek”, a dostałeś „środa”. Wykonały się trzy przypisania pod rząd.
        //   Kompilator milczy (bez -Xlint), program nie rzuca wyjątku, wynik po prostu jest zły.
        // Dodatkowa pułapka starej składni: wszystkie gałęzie dzielą JEDEN zasięg zmiennych, więc deklaracja
        //   w jednym case jest widoczna (choć nieprzypisana) w następnym.
        // DOBRA PRAKTYKA: w nowym kodzie używaj strzałek — nie ma przechodzenia dalej, więc nie ma tego błędu.
        //   Starą składnię zostaw tam, gdzie fall-through jest celowy (i dopisz komentarz „// celowo”).
    }

    // =================================================================================================
    // 2. INSTRUKCJA ZE STRZAŁKĄ
    // =================================================================================================

    /**
     * 2. Strzałka w instrukcji: koniec z break i przechodzeniem dalej. Kilka etykiet łączysz przecinkami.
     * Po strzałce może stać wyrażenie (np. wywołanie metody), blok {...} albo throw.
     */
    static void arrowStatement() {
        section("2. Instrukcja ze strzałką i wiele etykiet");

        StringBuilder log = new StringBuilder();    // log = dziennik
        for (int day = 1; day <= 7; day++) {
            switch (day) {
                case 1, 2, 3, 4, 5 -> log.append("R");           // R = dzień roboczy
                case 6, 7 -> {                                   // blok, bo robimy dwie rzeczy
                    log.append("[");
                    log.append("W");                             // W = weekend
                    log.append("]");
                }
                default -> throw new IllegalStateException("niemożliwe: " + day);
            }
        }
        show("tydzień", log);
        // WYNIK: tydzień → RRRRR[W][W]

        // PRZED (stary zapis wielu etykiet): trzeba puste case'y jeden pod drugim:
        //   case 1: case 2: case 3: case 4: case 5: log.append("R"); break;
        // PO: case 1, 2, 3, 4, 5 -> log.append("R");

        // DOBRA PRAKTYKA: w switchu ze strzałką zawsze dodawaj default z throw, gdy wartości spoza listy oznaczają
        //   błąd programisty. Cicha ścieżka „nic się nie stało” ukrywa błędy.
    }

    // =================================================================================================
    // 3. SWITCH JAKO WYRAŻENIE
    // =================================================================================================

    static String quarterName(int month) {   // quarter name = nazwa kwartału
        return switch (month) {
            case 1, 2, 3 -> "I kwartał";
            case 4, 5, 6 -> "II kwartał";
            case 7, 8, 9 -> "III kwartał";
            case 10, 11, 12 -> "IV kwartał";
            default -> throw new IllegalArgumentException("miesiąc spoza 1-12: " + month);
        };   // ← średnik: to jest wyrażenie, więc kończymy je jak przypisanie
    }

    static int daysInMonth(int month, int year) {   // days in month = dni w miesiącu
        return switch (month) {
            case 4, 6, 9, 11 -> 30;
            case 2 -> {
                // blok: może mieć wiele instrukcji; wartość oddajemy przez yield
                boolean leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;   // leap = przestępny
                yield leap ? 29 : 28;
            }
            default -> 31;
        };
    }

    static int oldStyleYield(String word) {   // old style yield = yield w starej składni
        // Wyrażenie switch ze starym dwukropkiem: zamiast break dajemy yield.
        return switch (word) {
            case "jeden":
                yield 1;
            case "dwa":
                yield 2;
            default:
                yield 0;
        };
    }

    /**
     * 3. Switch jako wyrażenie zwraca wartość. Mniej zmiennych pomocniczych (nie ma „String result; ... result = ...”),
     * a kompilator pilnuje, żeby każda gałąź coś zwróciła (albo rzuciła wyjątek).
     */
    static void switchAsExpression() {
        section("3. Switch jako wyrażenie i yield");

        show("miesiąc 5", quarterName(5));
        // WYNIK: miesiąc 5 → II kwartał
        show("miesiąc 12", quarterName(12));
        // WYNIK: miesiąc 12 → IV kwartał
        expectThrows("miesiąc 13", () -> quarterName(13));
        // WYNIK: ✔ miesiąc 13 → rzucono IllegalArgumentException: miesiąc spoza 1-12: 13

        show("luty 2024 (przestępny)", daysInMonth(2, 2024));
        // WYNIK: luty 2024 (przestępny) → 29
        show("luty 2100 (nie jest przestępny)", daysInMonth(2, 2100));
        // WYNIK: luty 2100 (nie jest przestępny) → 28
        show("kwiecień", daysInMonth(4, 2026));
        // WYNIK: kwiecień → 30
        show("styczeń", daysInMonth(1, 2026));
        // WYNIK: styczeń → 31
        show("yield w starej składni", oldStyleYield("dwa") + oldStyleYield("nic"));
        // WYNIK: yield w starej składni → 2

        // PRZED: zmienna pomocnicza, break w każdej gałęzi, łatwo zapomnieć o którejś.
        //   String name;
        //   switch (m) { case 1: name = "I"; break; case 2: name = "II"; break; default: name = "?"; }
        // PO: String name = switch (m) { case 1 -> "I"; case 2 -> "II"; default -> "?"; };

        // yield jest „słowem kontekstowym”: znaczy coś tylko wewnątrz bloku wyrażenia switch (stąd kolor w IDE).
        // PUŁAPKA: przy strzałce w wyrażeniu NIE piszesz yield dla pojedynczego wyrażenia (case 1 -> 5;),
        //   yield jest potrzebne dopiero w bloku {...} albo przy dwukropku. Brak yield w bloku = błąd kompilacji.
        // DOBRA PRAKTYKA: jeśli blok robi się długi (ponad kilka linii), wynieś go do osobnej metody:
        //   case 2 -> februaryDays(year);  — switch ma zostać czytelną „tabelą decyzji”.
    }

    // =================================================================================================
    // 4. WYCZERPYWALNOŚĆ DLA ENUMÓW
    // =================================================================================================

    /** Switch-wyrażenie po enumie BEZ default: kompilator wymusza obsługę wszystkich stałych. */
    static String statusMessage(OrderStatus status) {   // status message = komunikat o statusie
        return switch (status) {
            case NOWE -> "czeka na płatność";
            case OPLACONE -> "do spakowania";
            case WYSLANE -> "w drodze";
            case DOSTARCZONE -> "u klienta";
            case ANULOWANE -> "anulowane";
        };
    }

    static String advice(Level level) {   // advice = rada
        return switch (level) {
            case LOW -> "spokojnie";
            case MEDIUM -> "uważaj";
            case HIGH -> "alarm";
        };
    }

    /**
     * 4. Wyczerpywalność. Wyrażenie switch po enumie z wymienionymi wszystkimi stałymi nie potrzebuje default.
     * To zaleta, nie wada: gdy dopiszesz nową stałą do enuma, każdy taki switch przestanie się kompilować
     * i kompilator pokaże Ci dokładnie miejsca do poprawienia. Z default błąd zostałby ukryty.
     */
    static void exhaustiveEnums() {
        section("4. Wyczerpywalność: enum bez default");

        for (OrderStatus status : OrderStatus.values()) {
            show(status.name(), statusMessage(status));
        }
        // WYNIK: NOWE → czeka na płatność
        // WYNIK: OPLACONE → do spakowania
        // WYNIK: WYSLANE → w drodze
        // WYNIK: DOSTARCZONE → u klienta
        // WYNIK: ANULOWANE → anulowane
        show("advice(HIGH)", advice(Level.HIGH));
        // WYNIK: advice(HIGH) → alarm

        // Co się stanie po dopisaniu stałej, np. Level.CRITICAL?
        //   • switch-WYRAŻENIE bez default: błąd kompilacji („wyrażenie switch nie obejmuje wszystkich możliwych wartości”).
        //   • switch-INSTRUKCJA ze strzałką bez default: kompiluje się i po cichu ignoruje nową stałą (w Javie 17;
        //     od Javy 21 instrukcja switch ze wzorcami lub null też musi być wyczerpująca).
        // Pokażmy to drugie: instrukcja po enumie, w której zabrakło stałych.
        StringBuilder log = new StringBuilder();
        for (Level level : Level.values()) {
            switch (level) {                                          // instrukcja, nie wyrażenie
                case LOW -> log.append("niski;");
                case MEDIUM -> log.append("średni;");
                // HIGH pominięty — kompilator nie protestuje, po prostu nic się nie dzieje
            }
        }
        show("instrukcja bez wszystkich stałych", log);
        // WYNIK: instrukcja bez wszystkich stałych → niski;średni;

        // Jeśli enum zmieni się już po skompilowaniu switcha (inny plik .jar, bez ponownej kompilacji),
        // wyrażenie bez default rzuci wyjątek w czasie wykonania (w Javie 17: IncompatibleClassChangeError,
        // od Javy 21: MatchException) zamiast po cichu zwrócić coś złego.
        // PUŁAPKA: dodanie „na wszelki wypadek” default -> ... do switcha po enumie wyłącza tę ochronę.
        // DOBRA PRAKTYKA: w switchu po enumie zostaw default tylko wtedy, gdy NAPRAWDĘ chcesz obsłużyć „wszystkie
        //   pozostałe” tak samo. Jeśli każda stała ma własne znaczenie — wymień je wszystkie i pozwól kompilatorowi pilnować.
    }

    // =================================================================================================
    // 5. OBSŁUGIWANE TYPY
    // =================================================================================================

    static String describeCommand(String command) {   // describe command = opisz polecenie
        return switch (command.toLowerCase(Locale.ROOT)) {   // normalizujemy wielkość liter, switch ją rozróżnia
            case "start", "run" -> "uruchamiam";
            case "stop" -> "zatrzymuję";
            default -> "nieznane polecenie: " + command;
        };
    }

    static String letterKind(char letter) {   // letter kind = rodzaj litery
        return switch (letter) {
            case 'a', 'e', 'i', 'o', 'u', 'y', 'ą', 'ę', 'ó' -> "samogłoska";
            case ' ' -> "spacja";
            default -> "inna";
        };
    }

    /**
     * 5. Typy, po których można robić switch: byte, short, char, int (i ich klasy opakowujące),
     * String oraz enum. Etykiety muszą być stałymi czasu kompilacji i nie mogą się powtarzać.
     */
    static void supportedTypes() {
        section("5. Po czym można robić switch");

        show("String: \"START\"", describeCommand("START"));
        // WYNIK: String: "START" → uruchamiam
        show("String: \"stop\"", describeCommand("stop"));
        // WYNIK: String: "stop" → zatrzymuję
        show("String: \"jump\"", describeCommand("jump"));
        // WYNIK: String: "jump" → nieznane polecenie: jump
        show("char: 'ó'", letterKind('ó'));
        // WYNIK: char: 'ó' → samogłoska
        show("char: 'k'", letterKind('k'));
        // WYNIK: char: 'k' → inna

        // Niedozwolone w Javie 17 (błędy kompilacji): switch po long, float, double, boolean:
        //   long big = 5L; switch (big) { ... }          // „long” nie jest obsługiwanym typem
        // Dla boolean użyj zwykłego if albo wyrażenia warunkowego; dla long — rzutuj świadomie lub użyj if-else.
        // Duplikat etykiety (case 1, case 1) i etykieta niebędąca stałą (case zmienna) też są błędami.

        // PUŁAPKA: switch po String rozróżnia wielkość liter — „START” nie pasuje do „start”. Dlatego wyżej
        //   normalizujemy wejście: toLowerCase(Locale.ROOT) (jawny Locale, bo w tureckim „I” zmienia się w „ı”).
        // DOBRA PRAKTYKA: jeśli zbiór wartości jest zamknięty i znany, zamień napisy na enum — wtedy kompilator
        //   sprawdza wyczerpywalność, a literówka w case nie przejdzie.
    }

    // =================================================================================================
    // 6. NULL W SWITCHU
    // =================================================================================================

    /**
     * 6. W Javie 17 switch po wartości null ZAWSZE rzuca NullPointerException — dla String, enumów i obiektów
     * opakowujących. Powód: switch wywołuje na wartości metodę (hashCode, ordinal albo intValue), a na null to niemożliwe.
     * Dopiero Java 21 pozwala napisać {@code case null ->} jako zwykłą gałąź.
     */
    static void nullInSwitch() {
        section("6. null w switchu");

        String command = null;
        try {
            String result = switch (command) {                       // result = wynik
                case "start" -> "ruszamy";
                default -> "domyślnie";
            };
            show("nie dojdzie tutaj", result);
        } catch (NullPointerException e) {
            show("switch po null", e.getClass().getSimpleName());
            // WYNIK: switch po null → NullPointerException
        }

        Level level = null;
        try {
            switch (level) {
                case LOW -> note("niski");
                default -> note("inny");
            }
        } catch (NullPointerException e) {
            show("enum null", e.getClass().getSimpleName());
            // WYNIK: enum null → NullPointerException
        }
        // (Treść komunikatu tego wyjątku zależy od opcji kompilacji -g, więc jej tu nie drukujemy.)

        // Dziś bezpieczny wzorzec: sprawdź null przed switchem.
        String safe = command == null ? "brak polecenia" : describeCommand(command);   // safe = bezpiecznie
        show("sprawdzenie przed switchem", safe);
        // WYNIK: sprawdzenie przed switchem → brak polecenia

        // (Java 21+) case null jest dozwolone i można je połączyć z default:
        //   String result = switch (command) {
        //       case null -> "brak polecenia";
        //       case "start" -> "ruszamy";
        //       default -> "domyślnie";
        //   };
        // PUŁAPKA: switch po enumie pochodzącym z zewnątrz (z formularza, bazy, JSON-a) może dostać null.
        // DOBRA PRAKTYKA: waliduj wejście na granicy systemu i nie przepuszczaj null w głąb programu.
    }

    // =================================================================================================
    // 7. TYP WYNIKU
    // =================================================================================================

    /**
     * 7. Typ wyrażenia switch zależy od kontekstu. Jeśli jest przypisane do zmiennej o znanym typie (albo
     * przekazane do metody), każda gałąź jest dopasowywana do tego typu osobno. Jeśli nie ma typu docelowego
     * (var) — gałęzie są łączone jak w operatorze {@code ?:}, a liczby poddawane promocji numerycznej.
     */
    static void resultTypePitfall() {
        section("7. Typ wyniku: var kontra jawny typ");

        int selector = Integer.parseInt("1");                        // selector = selektor (wartość wybierająca)

        var standalone = switch (selector) {                         // standalone = samodzielne (bez typu docelowego)
            case 1 -> 1;                                             // int
            default -> 2.5;                                          // double
        };
        Object withTarget = switch (selector) {                      // with target = z typem docelowym
            case 1 -> 1;
            default -> 2.5;
        };
        show("var → wartość i typ", standalone + " (" + typeOf(standalone) + ")");
        // WYNIK: var → wartość i typ → 1.0 (Double)
        show("Object → wartość i typ", withTarget + " (" + typeOf(withTarget) + ")");
        // WYNIK: Object → wartość i typ → 1 (Integer)

        // PUŁAPKA: ta sama gałąź „case 1 -> 1” zwróciła raz 1 (Integer), raz 1.0 (Double). Przy var typ
        //   wspólny dla int i double to double, więc int został po cichu rozszerzony. To zachowanie jest takie
        //   samo jak w operatorze trójargumentowym: true ? 1 : 2.5 daje 1.0.
        // DOBRA PRAKTYKA: nie mieszaj w jednym switchu liczb różnych typów. Jeśli musisz, zapisz jawnie typ
        //   zmiennej (double result = switch ...) — wtedy wiadomo, czego się spodziewać.
    }

    // =================================================================================================
    // 8. PRZEPISYWANIE: PRZED I PO
    // =================================================================================================

    // PRZED: łańcuch if-else.
    static int gradeBefore(int points) {   // grade before = ocena (przed)
        if (points >= 90) {
            return 5;
        } else if (points >= 70) {
            return 4;
        } else if (points >= 60) {
            return 3;
        } else {
            return 2;
        }
    }

    // PO: switch po dziesiątkach punktów (points / 10) z wieloma etykietami.
    static int gradeAfter(int points) {    // grade after = ocena (po)
        if (points < 0 || points > 100) {
            throw new IllegalArgumentException("punkty spoza 0-100: " + points);
        }
        return switch (points / 10) {      // 0..10 po dzieleniu całkowitym
            case 10, 9 -> 5;
            case 8, 7 -> 4;
            case 6 -> 3;
            default -> 2;                  // 0..5
        };
    }

    static String httpCategory(int status) {   // http category = kategoria odpowiedzi HTTP
        return switch (status / 100) {
            case 1 -> "informacyjna";
            case 2 -> "sukces";
            case 3 -> "przekierowanie";
            case 4 -> "błąd klienta";
            case 5 -> "błąd serwera";
            default -> "nieznana";
        };
    }

    /**
     * 8. Typowe przepisania. Switch na zakres wartości zwykle rozwiązujemy dzieleniem (points / 10), bo etykiety
     * muszą być stałymi, a nie warunkami. Gdy warunki są złożone (porównania, koniunkcje), pozostaje if-else —
     * switch nie jest lepszy od if we wszystkich przypadkach.
     */
    static void beforeAfterRewrites() {
        section("8. Przepisywanie: if-else → switch");

        List<Integer> samples = List.of(100, 95, 75, 65, 40);        // samples = próbki
        StringBuilder before = new StringBuilder();
        StringBuilder after = new StringBuilder();
        for (int points : samples) {
            before.append(gradeBefore(points));
            after.append(gradeAfter(points));
        }
        show("oceny if-else", before);
        // WYNIK: oceny if-else → 55432
        show("oceny switch", after);
        // WYNIK: oceny switch → 55432
        show("HTTP 404", httpCategory(404));
        // WYNIK: HTTP 404 → błąd klienta
        show("HTTP 503", httpCategory(503));
        // WYNIK: HTTP 503 → błąd serwera
        show("HTTP 204", httpCategory(204));
        // WYNIK: HTTP 204 → sukces

        // PUŁAPKA: (points / 10) dla points = 100 daje 10, a dla 89 daje 8 — łatwo pomylić się o jeden w granicach
        //   przedziałów. Dlatego wyżej sprawdzamy zakres 0–100 na początku, a testy robimy na wartościach brzegowych
        //   (59, 60, 69, 70, 89, 90, 100).
        // DOBRA PRAKTYKA: switch wybierz, gdy masz tabelę „wartość → wynik” z wieloma przypadkami. Dla 2–3 gałęzi
        //   albo zakresów z porównaniami czytelniejszy bywa if-else.
    }

    // =================================================================================================
    // 9. PUŁAPKI SKŁADNI
    // =================================================================================================

    /**
     * 9. Pułapki składni. Kilka reguł, o które kompilator się potknie — lepiej znać je z góry.
     */
    static void syntaxPitfalls() {
        section("9. Pułapki składni");

        // 1) Nie wolno mieszać strzałki i dwukropka w jednym switchu:
        //      switch (x) { case 1 -> a(); case 2: b(); }   // błąd kompilacji
        // 2) Z wyrażenia switch nie wolno „uciec” instrukcjami return, break ani continue — wyrażenie MUSI
        //    zakończyć się wartością (albo wyjątkiem):
        //      int v = switch (x) { case 1 -> { return 5; } default -> 0; };   // błąd kompilacji
        //    Dozwolone jest tylko throw (wyjątek) i yield.
        // 3) Wyrażenie switch bez default i bez wymienienia wszystkich stałych enuma nie kompiluje się
        //    („nie obejmuje wszystkich możliwych wartości”).
        // 4) Strzałka bez bloku wymaga WYRAŻENIA, throw albo bloku. Samo „case 1 -> int y = 3;” to błąd.
        // 5) Switch-wyrażenie zakończ średnikiem; switch-instrukcję — nie.

        // Za to zasięgi zmiennych w gałęziach z blokiem są osobne (inaczej niż w starym switchu):
        StringBuilder log = new StringBuilder();
        for (int i = 1; i <= 2; i++) {
            switch (i) {
                case 1 -> {
                    int temp = 10;                                   // temp = tymczasowa
                    log.append("A").append(temp);
                }
                case 2 -> {
                    int temp = 20;                                   // ta sama nazwa — to inny zasięg, bez konfliktu
                    log.append("B").append(temp);
                }
                default -> throw new IllegalStateException();
            }
        }
        show("osobne zasięgi", log);
        // WYNIK: osobne zasięgi → A10B20

        // Wyrażenie switch może też stać wprost jako argument metody:
        show("switch jako argument", switch (selectorForDemo()) {
            case 1 -> "jeden";
            default -> "inny";
        });
        // WYNIK: switch jako argument → jeden

        // PUŁAPKA: w wyrażeniu switch po String lub int ZAWSZE potrzebujesz default — nie ma
        //   „wszystkich liczb całkowitych” ani „wszystkich napisów”, które dałoby się wymienić.
        // DOBRA PRAKTYKA: preferuj stałą formę: strzałka + wyrażenie w jednej linii. Blok używaj tylko wtedy,
        //   gdy gałąź naprawdę wymaga kilku kroków.
    }

    private static int selectorForDemo() {   // selector for demo = selektor do demonstracji
        return 1;
    }

    // =================================================================================================
    // 10. WZORCE W SWITCH (ZAPOWIEDŹ)
    // =================================================================================================

    /** Dziś, w Javie 17: łańcuch instanceof (instanceof ze wzorcem — Java 16). */
    static String describeToday(Object value) {   // describe today = opisz dzisiaj
        if (value == null) {
            return "brak";
        } else if (value instanceof Integer number && number > 100) {
            return "duża liczba " + number;
        } else if (value instanceof Integer number) {
            return "liczba " + number;
        } else if (value instanceof String text) {
            return "tekst " + text;
        } else {
            return "coś innego";
        }
    }

    /**
     * 10. Zapowiedź: wzorce w switch (Java 21, JEP 441). W Javie 17 ten sam efekt dajemy łańcuchem instanceof.
     * Wersja Java 21 jest krótsza, sprawdzana pod kątem wyczerpywalności i obsługuje null.
     */
    static void patternsPreview() {
        section("10. Zapowiedź: wzorce w switch (Java 21+)");

        show("describeToday(500)", describeToday(500));
        // WYNIK: describeToday(500) → duża liczba 500
        show("describeToday(7)", describeToday(7));
        // WYNIK: describeToday(7) → liczba 7
        show("describeToday(\"hej\")", describeToday("hej"));
        // WYNIK: describeToday("hej") → tekst hej
        show("describeToday(null)", describeToday(null));
        // WYNIK: describeToday(null) → brak

        // (Java 21+) — tak samo, ale ze switchem; NIE kompiluje się w Javie 17:
        //   static String describe(Object value) {
        //       return switch (value) {
        //           case null -> "brak";
        //           case Integer number when number > 100 -> "duża liczba " + number;   // when = strażnik
        //           case Integer number -> "liczba " + number;
        //           case String text -> "tekst " + text;
        //           default -> "coś innego";
        //       };
        //   }
        // (Java 21+) wzorce rekordów i zapieczętowane hierarchie — bez default, bo kompilator zna wszystkie warianty:
        //   switch (shape) {
        //       case Circle c -> Math.PI * c.radius() * c.radius();
        //       case Square s -> s.side() * s.side();
        //   }
        // Historia: wzorce w switch miały podgląd w Javie 17 (JEP 406), 18, 19, 20; wersja ostateczna: Java 21 (JEP 441).
        // Zobacz Modern05RecordsSealedPatterns (hierarchie zapieczętowane) i Modern07WhatsNextJava21.
        // DOBRA PRAKTYKA: nie włączaj funkcji podglądowych (--enable-preview) w kodzie produkcyjnym —
        //   mogą się zmienić lub zniknąć w kolejnym wydaniu.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Switch-wyrażenie (Java 14, JEP 361): var x = switch (v) { case A, B -> ...; default -> ...; };
     *   • Strzałka: bez break, bez przechodzenia dalej. Dwukropek: stara składnia z fall-through. Nie mieszaj.
     *   • Blok w gałęzi → wartość przez yield. Dozwolone w wyrażeniu: wyrażenie, blok z yield, throw.
     *   • Z wyrażenia nie wolno wyjść przez return, break, continue.
     *   • Enum: wymień wszystkie stałe, a default pomiń — kompilator wykryje brakującą stałą.
     *   • Obsługiwane typy: byte, short, char, int (i opakowania), String, enum. Nie: long, float, double, boolean.
     *   • null w switchu rzuca NullPointerException (case null dopiero w Javie 21).
     *   • Bez typu docelowego (var) gałęzie liczbowe są promowane do wspólnego typu (1 → 1.0).
     *   • Wzorce w switch: Java 21 (JEP 441); w Javie 17 użyj łańcucha instanceof.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się strzałka od dwukropka w switchu? Które ma „przechodzenie dalej”?
     *   2. Kiedy w wyrażeniu switch nie potrzeba default?
     *   3. Co wypisze:  int x = 2;  String s = switch (x) { case 1 -> "a"; case 2 -> "b"; default -> "c"; };
     *      System.out.println(s);  ?
     *   4. Co wypisze (stara składnia):  int n = 1;  String r = "";  switch (n) { case 1: r += "A"; case 2: r += "B"; break;
     *      case 3: r += "C"; }  System.out.println(r);  ?
     *   5. ZNAJDŹ BŁĄD:  int v = switch (x) { case 1 -> { System.out.println("raz"); } default -> 0; };
     *   6. Co wypisze:  int k = 1;  var r = switch (k) { case 1 -> 1; default -> 2.5; };  System.out.println(r);  ?
     *   7. Dlaczego dodanie default do switcha po enumie może ukryć błąd po rozbudowie enuma?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: pory roku", List.of("zima", "wiosna", "lato", "jesień"),
                () -> List.of(exercise1(1), exercise1(4), exercise1(7), exercise1(10)));
        Check.throwsException("ćw. 1: miesiąc 13", IllegalArgumentException.class, () -> exercise1(13));
        Check.equal("ćw. 2: można anulować", List.of(true, true, false, false, false),
                () -> List.of(exercise2(OrderStatus.NOWE), exercise2(OrderStatus.OPLACONE),
                        exercise2(OrderStatus.WYSLANE), exercise2(OrderStatus.DOSTARCZONE),
                        exercise2(OrderStatus.ANULOWANE)));
        Check.equal("ćw. 3: kalkulator", List.of(5, 20, 4),
                () -> List.of(exercise3("+", 2, 3), exercise3("*", 4, 5), exercise3("/", 9, 2)));
        Check.throwsException("ćw. 3: nieznany operator", IllegalArgumentException.class, () -> exercise3("^", 1, 2));
        Check.equal("ćw. 4: cena biletu", List.of(10, 12, 18, 30),
                () -> List.of(exercise4("dziecko", 5), exercise4("ulgowy", 70), exercise4("ulgowy", 20),
                        exercise4("normalny", 30)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("zima", "wiosna", "lato", "jesień"),
                () -> List.of(solution1(1), solution1(4), solution1(7), solution1(10)));
        Check.throwsException("ćw. 1 (wzorzec): miesiąc 13", IllegalArgumentException.class, () -> solution1(13));
        Check.equal("ćw. 2 (wzorzec)", List.of(true, true, false, false, false),
                () -> List.of(solution2(OrderStatus.NOWE), solution2(OrderStatus.OPLACONE),
                        solution2(OrderStatus.WYSLANE), solution2(OrderStatus.DOSTARCZONE),
                        solution2(OrderStatus.ANULOWANE)));
        Check.equal("ćw. 3 (wzorzec)", List.of(5, 20, 4),
                () -> List.of(solution3("+", 2, 3), solution3("*", 4, 5), solution3("/", 9, 2)));
        Check.throwsException("ćw. 3 (wzorzec): nieznany operator", IllegalArgumentException.class,
                () -> solution3("^", 1, 2));
        Check.equal("ćw. 4 (wzorzec)", List.of(10, 12, 18, 30),
                () -> List.of(solution4("dziecko", 5), solution4("ulgowy", 70), solution4("ulgowy", 20),
                        solution4("normalny", 30)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ na wyrażenie switch. Zwróć porę roku dla numeru miesiąca (12, 1, 2 → zima;
     * 3–5 → wiosna; 6–8 → lato; 9–11 → jesień), a dla innych wartości rzuć IllegalArgumentException.
     * <pre>{@code
     * // PRZED:
     * String season;
     * if (month == 12 || month == 1 || month == 2) { season = "zima"; }
     * else if (month >= 3 && month <= 5) { season = "wiosna"; }
     * // ... i tak dalej
     * }</pre>
     * Podpowiedź: case 12, 1, 2 -> "zima"; default -> throw new IllegalArgumentException(...).
     */
    static String exercise1(int month) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć true, jeśli zamówienie można jeszcze anulować (NOWE lub OPLACONE).
     * Użyj wyrażenia switch BEZ default — niech kompilator pilnuje wszystkich stałych.
     * Podpowiedź: case NOWE, OPLACONE -> true; potem wymień pozostałe stałe.
     */
    static boolean exercise2(OrderStatus status) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): kalkulator. Dla operatora "+", "-", "*", "/" zwróć wynik działania na a i b
     * (dzielenie całkowite); dla innego operatora rzuć IllegalArgumentException("nieznany operator: ...").
     * Podpowiedź: switch po String zwracający int; operator zapisany jako tekst.
     */
    static int exercise3(String operator, int a, int b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): cena biletu. "dziecko" → 10; "normalny" → 30; "ulgowy" → 12 dla osób od 65 lat,
     * w przeciwnym razie 18; inny rodzaj → IllegalArgumentException. Użyj bloku z yield w gałęzi "ulgowy".
     * Podpowiedź: case "ulgowy" -> { if (age >= 65) { yield 12; } yield 18; }
     */
    static int exercise4(String type, int age) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(int month) {
        return switch (month) {
            case 12, 1, 2 -> "zima";
            case 3, 4, 5 -> "wiosna";
            case 6, 7, 8 -> "lato";
            case 9, 10, 11 -> "jesień";
            default -> throw new IllegalArgumentException("zły miesiąc: " + month);
        };
    }

    static boolean solution2(OrderStatus status) {
        return switch (status) {
            case NOWE, OPLACONE -> true;
            case WYSLANE, DOSTARCZONE, ANULOWANE -> false;
        };
    }

    static int solution3(String operator, int a, int b) {
        return switch (operator) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> a / b;
            default -> throw new IllegalArgumentException("nieznany operator: " + operator);
        };
    }

    static int solution4(String type, int age) {
        return switch (type) {
            case "dziecko" -> 10;
            case "normalny" -> 30;
            case "ulgowy" -> {
                if (age >= 65) {
                    yield 12;
                }
                yield 18;
            }
            default -> throw new IllegalArgumentException("nieznany bilet: " + type);
        };
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Strzałka (case X ->) wykonuje tylko swoją gałąź i kończy switch. Dwukropek (case X:) ma przechodzenie
     *      dalej — bez break wykonują się kolejne gałęzie. Mieszanie obu w jednym switchu jest zabronione.
     *   2. Gdy argument jest enumem i wymieniono wszystkie jego stałe (albo gdy typ jest zamknięty, np. zapieczętowany
     *      w Javie 21). Dla int i String default jest zawsze potrzebny.
     *   3. b
     *   4. AB (n = 1: dodaje A, nie ma break, wpada do case 2 i dodaje B, tam jest break).
     *   5. Gałąź z blokiem w wyrażeniu musi zwrócić wartość przez yield (albo rzucić wyjątek); ten blok kończy się
     *      bez yield — błąd kompilacji. Poprawka: { System.out.println("raz"); yield 5; }
     *   6. 1.0 (bez typu docelowego int i double są promowane do double).
     *   7. Z default switch kompiluje się nawet po dopisaniu nowej stałej, więc nowa stała trafia po cichu do gałęzi
     *      domyślnej. Bez default kompilator zgłosi błąd w każdym takim switchu i wskaże miejsca do poprawienia.
     */
    // </editor-fold>
}
