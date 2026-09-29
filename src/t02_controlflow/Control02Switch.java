package t02_controlflow;

import helpers.Check;

import java.util.Locale;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: switch — wybór jednej z wielu ścieżek
 *        (switch = przełącznik; case = przypadek; default = domyślnie; break = przerwij; yield = oddaj wartość)
 *
 * W SKRÓCIE:
 *   switch porównuje JEDNĄ wartość z listą stałych (case) i skacze do pasującej.
 *   Klasyczny switch (z dwukropkiem) wymaga break, inaczej „przelatuje” do następnych case'ów.
 *   Switch jako wyrażenie (Java 14+) ze strzałką {@code ->} zwraca wartość, nie przelatuje
 *   i dla enumów pilnuje, żeby żadna stała nie została pominięta.
 *
 * ANALOGIA: winda. Wciskasz numer piętra (wartość), winda jedzie prosto na to piętro (case).
 *   Stara winda (klasyczny switch) bez hamulca (break) jedzie dalej przez kolejne piętra.
 *   Nowa winda (switch ze strzałką) zatrzymuje się dokładnie na wybranym piętrze.
 *
 * JAK TO DZIAŁA:
 *   klasyczny (instrukcja):              nowoczesny (wyrażenie, Java 14+):
 *   switch (x) {                         String s = switch (x) {
 *       case 1:                              case 1 -> "jeden";
 *           s = "jeden";                     case 2, 3 -> "dwa lub trzy";
 *           break;       ← konieczne!        default -> "inne";
 *       case 2:                          };                    ← średnik, bo to wyrażenie
 *       case 3:          ← grupowanie
 *           s = "dwa lub trzy";
 *           break;
 *       default:
 *           s = "inne";
 *   }
 *   Dozwolone typy w Java 17: int, short, byte, char (i ich klasy opakowujące), String, enum.
 *   NIE: long, float, double, boolean.
 *
 * SŁÓWKA:
 *   fall-through = przelatywanie (do kolejnego case); label = etykieta (case 1:); exhaustive = wyczerpujący
 *   (obejmuje wszystkie możliwości); expression = wyrażenie (ma wartość); statement = instrukcja (nie ma wartości);
 *   case-sensitive = rozróżnia wielkość liter; arrow = strzałka
 *
 * ZOBACZ TEŻ: t02_controlflow/Control01IfElse (if / else if dla przedziałów i złożonych warunków),
 *   t08_enums/Enums01Basics (typy wyliczeniowe enum), t10_exceptions/Exceptions01Basics (NullPointerException)
 * </pre>
 */
public class Control02Switch {

    /** Mały typ wyliczeniowy (enum = lista nazwanych stałych). Pełne omówienie: t08_enums/Enums01Basics. */
    enum Day { MON, TUE, WED, THU, FRI, SAT, SUN } // Day = dzień tygodnia

    public static void main(String[] args) {
        title("Control02 — switch: klasyczny i nowoczesny (Java 14+)");

        classicSwitch();        // classic switch = klasyczny switch
        fallThrough();          // fall through = przelatywanie przez case'y
        charAndString();        // char and string = switch na znakach i tekstach
        switchOnEnum();         // switch on enum = switch na typie wyliczeniowym
        switchExpression();     // switch expression = switch jako wyrażenie
        yieldInBlock();         // yield in block = yield w bloku
        exhaustiveness();       // exhaustiveness = wyczerpanie wszystkich przypadków
        pitfalls();             // pitfalls = pułapki
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. KLASYCZNY SWITCH Z BREAK
    // =================================================================================================

    /**
     * 1. Klasyczny switch: case z dwukropkiem, break kończy switch, default łapie resztę.
     */
    static void classicSwitch() {
        section("1. Klasyczny switch z break");

        int dayNumber = 3;  // day number = numer dnia
        String dayName;     // day name = nazwa dnia
        switch (dayNumber) {
            case 1:
                dayName = "poniedziałek";
                break;
            case 2:
                dayName = "wtorek";
                break;
            case 3:
                dayName = "środa";
                break;
            case 4:
                dayName = "czwartek";
                break;
            case 5:
                dayName = "piątek";
                break;
            default:
                dayName = "weekend albo błąd";
                break;
        }
        show("dzień nr 3", dayName);
        // WYNIK: dzień nr 3 → środa

        // W metodzie zamiast break można użyć return — wychodzi on z całej metody, więc i ze switcha.
        show("dayNameByReturn(5)", dayNameByReturn(5)); // day name by return = nazwa dnia przez return
        // WYNIK: dayNameByReturn(5) → piątek
        show("dayNameByReturn(9)", dayNameByReturn(9));
        // WYNIK: dayNameByReturn(9) → nieznany dzień

        // Jak to się wykonuje? Java wylicza wartość w nawiasie RAZ, skacze do pasującej etykiety case
        // i wykonuje instrukcje W DÓŁ, aż trafi na break (albo return, albo koniec switcha).
        // Gdy nic nie pasuje — skacze do default. Bez default (i bez dopasowania) nic się nie dzieje.

        // DOBRA PRAKTYKA: default zawsze na końcu. Nawet gdy „nie powinien się zdarzyć” — obsłuż go
        // (komunikat, wartość specjalna), zamiast udawać, że nie istnieje.
    }

    /** Klasyczny switch z return zamiast break. */
    static String dayNameByReturn(int dayNumber) {
        switch (dayNumber) {
            case 1:
                return "poniedziałek";
            case 2:
                return "wtorek";
            case 3:
                return "środa";
            case 4:
                return "czwartek";
            case 5:
                return "piątek";
            default:
                return "nieznany dzień";
        }
    }

    // =================================================================================================
    // 2. FALL-THROUGH: ZAMIERZONY I PRZYPADKOWY
    // =================================================================================================

    /**
     * 2. Bez break wykonanie „przelatuje” do następnego case. Czasem to zamierzone (grupowanie),
     * częściej — błąd (zapomniany break).
     */
    static void fallThrough() {
        section("2. Fall-through — grupowanie i zapomniany break");

        // Zamierzone: kilka etykiet case bez instrukcji między nimi = „którakolwiek z nich”.
        show("dayKindClassic(2)", dayKindClassic(2)); // day kind = rodzaj dnia
        // WYNIK: dayKindClassic(2) → dzień roboczy
        show("dayKindClassic(7)", dayKindClassic(7));
        // WYNIK: dayKindClassic(7) → weekend
        show("dayKindClassic(0)", dayKindClassic(0));
        // WYNIK: dayKindClassic(0) → błąd

        // PUŁAPKA: zapomniany break. Rozmiar S powinien kosztować 10 zł, a kosztuje...
        show("cena S (bez break)", priceForgottenBreak('S')); // price = cena
        // WYNIK: cena S (bez break) → 20
        show("cena M (bez break)", priceForgottenBreak('M'));
        // WYNIK: cena M (bez break) → 20
        // Dla 'S': price = 10, brak break → price = 15, brak break → price = 20, break. Ostatnie przypisanie wygrywa.
        // Kompilator z opcją -Xlint:fallthrough ostrzega: „possible fall-through into case”
        // (możliwe przelatywanie do case). Tu celowo wyciszyliśmy to ostrzeżenie adnotacją @SuppressWarnings.

        show("cena S (z break)", priceWithBreak('S'));
        // WYNIK: cena S (z break) → 10

        // DOBRA PRAKTYKA: gdy fall-through jest naprawdę zamierzony (i między case'ami SĄ instrukcje),
        // napisz komentarz „// fall through” — inaczej każdy czytelnik uzna to za błąd.
        // Jeszcze lepiej: użyj switcha ze strzałką (sekcja 5) — tam fall-through w ogóle nie istnieje.
    }

    /** Zamierzone grupowanie: case 1..5 mają wspólny kod. */
    static String dayKindClassic(int day) {
        String kind; // kind = rodzaj
        switch (day) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                kind = "dzień roboczy";
                break;
            case 6:
            case 7:
                kind = "weekend";
                break;
            default:
                kind = "błąd";
        }
        return kind;
    }

    /** BŁĄD celowy: brak break — wykonanie przelatuje w dół. */
    @SuppressWarnings("fallthrough") // SuppressWarnings = wycisz ostrzeżenia; tu: celowa demonstracja błędu
    static int priceForgottenBreak(char size) {
        int price = 0;
        switch (size) {
            case 'S':
                price = 10;
            case 'M':
                price = 15;
            case 'L':
                price = 20;
                break;
            default:
                price = -1;
        }
        return price;
    }

    /** Poprawnie: break po każdym case. */
    static int priceWithBreak(char size) {
        int price;
        switch (size) {
            case 'S':
                price = 10;
                break;
            case 'M':
                price = 15;
                break;
            case 'L':
                price = 20;
                break;
            default:
                price = -1;
        }
        return price;
    }

    // =================================================================================================
    // 3. SWITCH NA CHAR I STRING
    // =================================================================================================

    /**
     * 3. switch działa na znakach (char) i tekstach (String). Dla String porównanie jest jak equals:
     * rozróżnia wielkość liter.
     */
    static void charAndString() {
        section("3. switch na char i String");

        char answer = 't'; // answer = odpowiedź
        switch (answer) {
            case 'T':
            case 't':
                System.out.println("Odpowiedź: tak");
                break;
            case 'N':
            case 'n':
                System.out.println("Odpowiedź: nie");
                break;
            default:
                System.out.println("Nie rozumiem: " + answer);
        }
        // WYNIK: Odpowiedź: tak

        show("command(\"start\")", command("start")); // command = polecenie
        // WYNIK: command("start") → Uruchamiam
        show("command(\"stop\")", command("stop"));
        // WYNIK: command("stop") → Zatrzymuję

        // PUŁAPKA: switch na String rozróżnia wielkość liter (działa jak equals, a nie equalsIgnoreCase).
        show("command(\"START\")", command("START"));
        // WYNIK: command("START") → Nieznane polecenie: START
        // Rozwiązanie: zamień tekst na małe litery PRZED switchem. Locale.ROOT = reguły neutralne językowo
        // (w niektórych językach, np. tureckim, „I” zmienia się w inną literę niż „i”).
        show("command(\"START\" małymi literami)", command("START".toLowerCase(Locale.ROOT))); // to lower case = na małe litery
        // WYNIK: command("START" małymi literami) → Uruchamiam

        // Typy, na których NIE zrobisz switcha: long, float, double, boolean — błąd kompilacji.
        // Dla przedziałów liczbowych (np. temperatura 20–25) używaj if / else if — case przyjmuje tylko STAŁE.
    }

    /** Klasyczny switch na String. */
    static String command(String cmd) {
        switch (cmd) {
            case "start":
                return "Uruchamiam";
            case "stop":
                return "Zatrzymuję";
            default:
                return "Nieznane polecenie: " + cmd;
        }
    }

    // =================================================================================================
    // 4. SWITCH NA ENUM
    // =================================================================================================

    /**
     * 4. switch na enum: w etykietach piszemy SAMĄ nazwę stałej (SAT), bez nazwy typu (Day.SAT).
     */
    static void switchOnEnum() {
        section("4. switch na enum");

        Day today = Day.FRI; // today = dziś
        switch (today) {
            case SAT:
            case SUN:
                System.out.println("Weekend — śpimy dłużej");
                break;
            case FRI:
                System.out.println("Piątek — jeszcze tylko dziś!");
                break;
            default:
                System.out.println("Zwykły dzień roboczy");
        }
        // WYNIK: Piątek — jeszcze tylko dziś!

        // PUŁAPKA: w Java 17 „case Day.SAT:” to błąd kompilacji — etykieta musi być samą nazwą stałej.
        // (Java 21+ pozwala już na pełną nazwę Day.SAT w etykiecie).

        // Każda stała enuma ma numer kolejny (ordinal = liczba porządkowa) liczony od 0:
        show("Day.FRI.ordinal()", today.ordinal());
        // WYNIK: Day.FRI.ordinal() → 4
    }

    // =================================================================================================
    // 5. SWITCH JAKO WYRAŻENIE (JAVA 14+)
    // =================================================================================================

    /**
     * 5. Switch expression (Java 14+) = switch, który ZWRACA wartość. Strzałka {@code ->} nie przelatuje,
     * kilka etykiet oddzielamy przecinkiem, całość kończy średnik.
     */
    static void switchExpression() {
        section("5. Switch jako wyrażenie (Java 14+)");

        Day day = Day.SAT;
        String kind = switch (day) {
            case MON, TUE, WED, THU, FRI -> "dzień roboczy";
            case SAT, SUN -> "weekend";
        };  // ← średnik! To jest przypisanie „String kind = ...;”
        show("rodzaj dnia SAT", kind);
        // WYNIK: rodzaj dnia SAT → weekend

        // PRZED: dayKindClassic (sekcja 2) — 18 linii, break-i, zmienna bez wartości początkowej.
        // PO: dayKindArrow — 5 linii, zero break, zero ryzyka zapomnienia.
        show("dayKindArrow(3)", dayKindArrow(3));
        // WYNIK: dayKindArrow(3) → dzień roboczy
        show("dayKindArrow(6)", dayKindArrow(6));
        // WYNIK: dayKindArrow(6) → weekend

        // Strzałka działa też w zwykłej INSTRUKCJI switch (bez zwracania wartości):
        switch (day) {
            case SAT, SUN -> System.out.println("Budzik wyłączony");
            default -> System.out.println("Budzik o 6:30");
        }
        // WYNIK: Budzik wyłączony

        // Wyrażenie switch można wstawić wszędzie, gdzie pasuje wartość — np. prosto do println:
        int month = 2; // month = miesiąc
        System.out.println("Kwartał miesiąca 2: " + switch (month) {
            case 1, 2, 3 -> 1;
            case 4, 5, 6 -> 2;
            case 7, 8, 9 -> 3;
            default -> 4;
        });
        // WYNIK: Kwartał miesiąca 2: 1

        // DOBRA PRAKTYKA: w nowym kodzie (Java 14+) wybieraj switch ze strzałką. Klasyczny switch musisz
        // umieć czytać, bo jest w milionach linii starszego kodu.
    }

    /** PO: switch expression zamiast klasycznego switcha z break. */
    static String dayKindArrow(int day) {
        return switch (day) {
            case 1, 2, 3, 4, 5 -> "dzień roboczy";
            case 6, 7 -> "weekend";
            default -> "błąd";
        };
    }

    // =================================================================================================
    // 6. YIELD — WARTOŚĆ Z BLOKU
    // =================================================================================================

    /**
     * 6. Gdy po strzałce potrzebujesz kilku instrukcji, użyj bloku w klamrach i zwróć wartość przez yield.
     * return tu nie zadziała — return wychodzi z METODY, a yield tylko ze switcha.
     */
    static void yieldInBlock() {
        section("6. yield — wartość z bloku { }");

        // Linia „(weekend: ...)” pojawia się PRZED wynikiem, bo najpierw liczy się argument metody show.
        show("bilet SAT, 15 lat", ticketPrice(Day.SAT, 15)); // ticket price = cena biletu
        // WYNIK: (weekend: cena bazowa 30)
        // WYNIK: bilet SAT, 15 lat → 15
        show("bilet SUN, 40 lat", ticketPrice(Day.SUN, 40));
        // WYNIK: (weekend: cena bazowa 30)
        // WYNIK: bilet SUN, 40 lat → 30
        show("bilet TUE, 40 lat", ticketPrice(Day.TUE, 40));
        // WYNIK: bilet TUE, 40 lat → 20

        // yield działa też w switchu-wyrażeniu z dwukropkami (rzadko spotykane):
        //   int x = switch (n) { case 1: yield 10; default: yield 0; };
        // Tu każda etykieta MUSI zakończyć się yield (albo rzuceniem wyjątku).
        int bonus = switch (Day.MON) { // bonus = premia
            case MON:
                yield 5;
            default:
                yield 0;
        };
        show("premia w poniedziałek", bonus);
        // WYNIK: premia w poniedziałek → 5
    }

    /** Blok z kilkoma instrukcjami i yield. */
    static int ticketPrice(Day day, int age) {
        return switch (day) {
            case SAT, SUN -> {
                int base = 30;                          // base = cena bazowa
                System.out.println("   (weekend: cena bazowa " + base + ")");
                yield age < 18 ? base / 2 : base;       // yield = oddaj wartość z tego bloku
            }
            default -> age < 18 ? 10 : 20;
        };
    }

    // =================================================================================================
    // 7. WYCZERPYWALNOŚĆ (EXHAUSTIVENESS)
    // =================================================================================================

    /**
     * 7. Switch-wyrażenie MUSI obsłużyć każdą możliwą wartość. Dla enuma wystarczy wymienić wszystkie
     * stałe — default nie jest wtedy potrzebny, a kompilator pilnuje nas przy zmianach.
     */
    static void exhaustiveness() {
        section("7. Wyczerpywalność — kompilator sprawdza, czy obsłużono wszystko");

        show("isWorkday(WED)", isWorkday(Day.WED)); // is workday = czy dzień roboczy
        // WYNIK: isWorkday(WED) → true
        show("isWorkday(SUN)", isWorkday(Day.SUN));
        // WYNIK: isWorkday(SUN) → false

        // isWorkday nie ma default. Gdyby ktoś dopisał do enuma nową stałą, np. HOLIDAY (święto),
        // metoda PRZESTANIE SIĘ KOMPILOWAĆ: „the switch expression does not cover all possible input values”
        // (wyrażenie switch nie obejmuje wszystkich możliwych wartości). To ZALETA: kompilator pokaże
        // każde miejsce, które trzeba poprawić.
        // Gdyby był default, nowa stała po cichu wpadłaby do default — i mógłby to być błąd.

        // Dla int / String nie da się wymienić wszystkich wartości, więc default jest OBOWIĄZKOWY:
        //   String s = switch (n) { case 1 -> "jeden"; };   // błąd kompilacji: brak default
        // Instrukcja switch (bez zwracania wartości) na enumie w Java 17 nie musi być wyczerpująca.

        // DOBRA PRAKTYKA: dla enumów w switch-wyrażeniu wypisz wszystkie stałe i NIE dodawaj default.
    }

    /** Wyczerpujący switch na enum — bez default. */
    static boolean isWorkday(Day day) {
        return switch (day) {
            case MON, TUE, WED, THU, FRI -> true;
            case SAT, SUN -> false;
        };
    }

    // =================================================================================================
    // 8. PUŁAPKI: NULL, JAVA 21
    // =================================================================================================

    /**
     * 8. switch na null rzuca NullPointerException — zarówno dla String, jak i dla enum.
     * Obsługa null w samym switchu (case null) to dopiero Java 21.
     */
    static void pitfalls() {
        section("8. Pułapki: null w switchu, nowości z Java 21");

        // PUŁAPKA: switch na null NIE trafia do default — od razu rzuca wyjątek.
        String nothing = null; // nothing = nic
        expectThrows("switch na String równym null", () -> command(nothing));
        // WYNIK: ✔ switch na String równym null → rzucono NullPointerException: Cannot invoke "String.hashCode()" because "<local1>" is null
        // Dlaczego hashCode? Switch na String pod spodem najpierw liczy „skrót” tekstu (hashCode),
        // a dopiero potem porównuje equals. Na null nie da się wywołać żadnej metody.

        Day noDay = null; // no day = brak dnia
        expectThrows("switch na enum równym null", () -> isWorkday(noDay));
        // WYNIK: ✔ switch na enum równym null → rzucono NullPointerException: Cannot invoke "t02_controlflow.Control02Switch$Day.ordinal()" because "day" is null

        // DOBRA PRAKTYKA: sprawdź null PRZED switchem (klauzula strażnika z Control01):
        show("safeCommand(null)", safeCommand(nothing)); // safe command = bezpieczne polecenie
        // WYNIK: safeCommand(null) → Brak polecenia

        // Java 21+ (tylko dla ciekawości — w Java 17 się NIE skompiluje):
        //   switch (obj) {
        //       case null -> "brak";                       ← obsługa null wprost w switchu
        //       case Integer i when i > 0 -> "dodatnia";   ← pattern matching (dopasowanie wzorca)
        //       case String s -> "tekst: " + s;
        //       default -> "coś innego";
        //   }
    }

    /** Strażnik null przed switchem. */
    static String safeCommand(String cmd) {
        if (cmd == null) {
            return "Brak polecenia";
        }
        return command(cmd);
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Klasyczny switch: „case X:” + break. Bez break wykonanie przelatuje do następnego case.
     *   • Grupowanie: case 1: case 2: (bez instrukcji między nimi) albo w nowym stylu: case 1, 2 ->
     *   • default łapie wszystko, czego nie obsłużyły case'y. Stawiaj go na końcu.
     *   • Typy: int, short, byte, char, String, enum (+ Integer, Character...). NIE: long, double, boolean.
     *   • String w switchu rozróżnia wielkość liter — ujednolić tekst przed switchem (toLowerCase).
     *   • enum w etykiecie: samo SAT, nie Day.SAT (w Java 17).
     *   • Switch expression (Java 14+): „var = switch (x) { case A -> wartość; ... };” — średnik na końcu.
     *   • Kilka instrukcji po strzałce → blok { ... yield wartość; }. return wychodzi z metody, yield ze switcha.
     *   • Switch-wyrażenie musi być wyczerpujące: dla enuma — wszystkie stałe (bez default), dla int/String — default.
     *   • switch na null → NullPointerException (case null dopiero w Java 21+).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się switch-instrukcja od switch-wyrażenia?
     *   2. Co wypisze:  int n = 1;  switch (n) { case 1: System.out.print("A"); case 2: System.out.print("B"); break; case 3: System.out.print("C"); }  ?
     *   3. ZNAJDŹ BŁĄD:  String s = switch (level) { case 1 -> "niski"; case 2 -> "wysoki"; }   (level to int)
     *   4. ZNAJDŹ BŁĄD:  switch (day) { case Day.MON -> ...; }   (Java 17)
     *   5. Co wypisze:  switch ("Tak") { case "tak" -> System.out.println("1"); default -> System.out.println("2"); }  ?
     *   6. Dlaczego w switch-wyrażeniu na enum lepiej NIE pisać default?
     *   7. Czym różni się yield od return wewnątrz bloku po strzałce?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Rodzaj pojazdu do ćwiczenia 4 (vehicle = pojazd; motorcycle = motocykl; car = samochód; bus = autobus). */
    enum Vehicle { MOTORCYCLE, CAR, BUS }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: nazwy ocen 1, 4, 6, 7", "niedostateczny, dobry, celujący, brak takiej oceny",
                () -> exercise1(1) + ", " + exercise1(4) + ", " + exercise1(6) + ", " + exercise1(7));
        Check.equal("ćw. 2: pory roku 1, 4, 7, 10, 12, 13", "zima, wiosna, lato, jesień, zima, błąd",
                () -> exercise2(1) + ", " + exercise2(4) + ", " + exercise2(7) + ", " + exercise2(10) + ", "
                        + exercise2(12) + ", " + exercise2(13));
        Check.equal("ćw. 3: kalkulator", "10, 4, 21, 2, 1, 0, 0",
                () -> exercise3(7, '+', 3) + ", " + exercise3(7, '-', 3) + ", " + exercise3(7, '*', 3) + ", "
                        + exercise3(7, '/', 3) + ", " + exercise3(7, '%', 3) + ", " + exercise3(7, '/', 0) + ", "
                        + exercise3(7, '?', 3));
        Check.equal("ćw. 4: opłata parkingowa", "6, 5, 14, 20, 50, 0",
                () -> exercise4(Vehicle.MOTORCYCLE, 3) + ", " + exercise4(Vehicle.CAR, 1) + ", "
                        + exercise4(Vehicle.CAR, 4) + ", " + exercise4(Vehicle.BUS, 1) + ", "
                        + exercise4(Vehicle.BUS, 5) + ", " + exercise4(Vehicle.CAR, 0));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "niedostateczny, dobry, celujący, brak takiej oceny",
                () -> solution1(1) + ", " + solution1(4) + ", " + solution1(6) + ", " + solution1(7));
        Check.equal("ćw. 2 (wzorzec)", "zima, wiosna, lato, jesień, zima, błąd",
                () -> solution2(1) + ", " + solution2(4) + ", " + solution2(7) + ", " + solution2(10) + ", "
                        + solution2(12) + ", " + solution2(13));
        Check.equal("ćw. 3 (wzorzec)", "10, 4, 21, 2, 1, 0, 0",
                () -> solution3(7, '+', 3) + ", " + solution3(7, '-', 3) + ", " + solution3(7, '*', 3) + ", "
                        + solution3(7, '/', 3) + ", " + solution3(7, '%', 3) + ", " + solution3(7, '/', 0) + ", "
                        + solution3(7, '?', 3));
        Check.equal("ćw. 4 (wzorzec)", "6, 5, 14, 20, 50, 0",
                () -> solution4(Vehicle.MOTORCYCLE, 3) + ", " + solution4(Vehicle.CAR, 1) + ", "
                        + solution4(Vehicle.CAR, 4) + ", " + solution4(Vehicle.BUS, 1) + ", "
                        + solution4(Vehicle.BUS, 5) + ", " + solution4(Vehicle.CAR, 0));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zamień ocenę szkolną 1–6 na nazwę: niedostateczny, dopuszczający, dostateczny,
     * dobry, bardzo dobry, celujący. Dla innej liczby zwróć "brak takiej oceny".
     * Podpowiedź: return switch (grade) { case 1 -> ...; ... default -> ...; };
     */
    static String exercise1(int grade) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ klasyczny switch na switch-wyrażenie ze strzałkami.
     * <pre>{@code
     * String season;
     * switch (month) {
     *     case 12: case 1: case 2:  season = "zima";   break;
     *     case 3: case 4: case 5:   season = "wiosna"; break;
     *     case 6: case 7: case 8:   season = "lato";   break;
     *     case 9: case 10: case 11: season = "jesień"; break;
     *     default:                  season = "błąd";
     * }
     * return season;
     * }</pre>
     * Podpowiedź: etykiety grupujesz przecinkiem: case 12, 1, 2 -> "zima";
     */
    static String exercise2(int month) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 3 (średnie): prosty kalkulator na liczbach całkowitych. Operator to znak '+', '-', '*', '/' lub '%'.
     * Dla nieznanego operatora zwróć 0. Dla '/' i '%' z b równym 0 też zwróć 0 (zamiast wyjątku ArithmeticException).
     * Podpowiedź: switch-wyrażenie na char; przy '/' i '%' użyj operatora ?: albo bloku z yield.
     */
    static int exercise3(int a, char operator, int b) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): opłata parkingowa w zł za podaną liczbę godzin.
     * MOTORCYCLE: 2 zł za każdą godzinę. CAR: pierwsza godzina 5 zł, każda następna 3 zł.
     * BUS: 10 zł za godzinę, ale minimum 20 zł. Dla hours mniejszego lub równego 0 opłata wynosi 0 (dowolny pojazd).
     * Podpowiedź: najpierw strażnik dla hours, potem switch-wyrażenie na enum BEZ default (wyczerpujący),
     * dla BUS blok z yield i Math.max.
     */
    static int exercise4(Vehicle vehicle, int hours) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(int grade) {
        return switch (grade) {
            case 1 -> "niedostateczny";
            case 2 -> "dopuszczający";
            case 3 -> "dostateczny";
            case 4 -> "dobry";
            case 5 -> "bardzo dobry";
            case 6 -> "celujący";
            default -> "brak takiej oceny";
        };
    }

    static String solution2(int month) {
        return switch (month) {
            case 12, 1, 2 -> "zima";
            case 3, 4, 5 -> "wiosna";
            case 6, 7, 8 -> "lato";
            case 9, 10, 11 -> "jesień";
            default -> "błąd";
        };
    }

    static int solution3(int a, char operator, int b) {
        return switch (operator) {
            case '+' -> a + b;
            case '-' -> a - b;
            case '*' -> a * b;
            case '/' -> b == 0 ? 0 : a / b;
            case '%' -> b == 0 ? 0 : a % b;
            default -> 0;
        };
    }

    static int solution4(Vehicle vehicle, int hours) {
        if (hours <= 0) {
            return 0;
        }
        return switch (vehicle) {
            case MOTORCYCLE -> 2 * hours;
            case CAR -> 5 + 3 * (hours - 1);
            case BUS -> {
                int fee = 10 * hours; // fee = opłata
                yield Math.max(fee, 20);
            }
        };
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Instrukcja tylko coś wykonuje; wyrażenie ma wartość (można ją przypisać, zwrócić, wypisać),
     *      musi być wyczerpujące i kończy się średnikiem, gdy stoi w przypisaniu.
     *   2. „AB” — po case 1 nie ma break, więc wykonanie przelatuje do case 2 i dopiero tam trafia na break.
     *   3. Brak default — dla int nie da się wymienić wszystkich wartości, więc to błąd kompilacji.
     *   4. W Java 17 etykieta enuma musi być samą nazwą stałej: case MON -> ...  (Day.MON dopiero w Java 21+).
     *   5. „2” — switch na String rozróżnia wielkość liter, "Tak" to nie "tak".
     *   6. Bo wtedy kompilator zgłosi błąd, gdy ktoś doda nową stałą do enuma — pokaże miejsca do poprawy.
     *      Z default nowa stała po cichu trafiłaby do domyślnej gałęzi.
     *   7. yield kończy tylko switch i podaje jego wartość; return próbowałby wyjść z całej metody —
     *      wewnątrz switch-wyrażenia to błąd kompilacji.
     */
    // </editor-fold>
}
