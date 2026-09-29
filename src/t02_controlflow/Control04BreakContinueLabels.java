package t02_controlflow;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Department;
import helpers.model.Employee;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: break, continue i etykiety — sterowanie wnętrzem pętli
 *        (break = przerwij; continue = kontynuuj, czyli „pomiń resztę tego obrotu”; label = etykieta)
 *
 * W SKRÓCIE:
 *   break natychmiast kończy najbliższą pętlę (albo switch). continue przerywa tylko bieżący obrót
 *   i przechodzi do następnego. W pętlach zagnieżdżonych etykieta (np. search:) pozwala przerwać
 *   lub kontynuować pętlę ZEWNĘTRZNĄ. Często czytelniej jest jednak wydzielić metodę i zrobić return.
 *
 * ANALOGIA: sprawdzanie klasówek.
 *   continue = „ta kartka jest pusta — odkładam ją i biorę następną”,
 *   break    = „znalazłem ściągę — przestaję sprawdzać ten stos”,
 *   break z etykietą = „przestaję sprawdzać WSZYSTKIE stosy, idę do dyrektora”.
 *
 * JAK TO DZIAŁA:
 *   for (...) {
 *       if (a) { continue; }   ← skok do następnego obrotu (w for: najpierw update, potem warunek)
 *       if (b) { break; }      ← skok ZA pętlę
 *       ...
 *   }
 *   ← tutaj ląduje break
 *
 *   outer:                     ← etykieta = nazwa + dwukropek, tuż przed pętlą
 *   for (...) {
 *       for (...) {
 *           if (x) { continue outer; }   ← następny obrót pętli ZEWNĘTRZNEJ
 *           if (y) { break outer; }      ← koniec pętli ZEWNĘTRZNEJ (a więc i wewnętrznej)
 *       }
 *   }
 *   Bez etykiety break i continue dotyczą zawsze NAJBLIŻSZEJ (najbardziej wewnętrznej) pętli.
 *
 * SŁÓWKA:
 *   nested loops = pętle zagnieżdżone; outer = zewnętrzna; inner = wewnętrzna; flag = flaga;
 *   found = znaleziono; target = cel (szukana wartość); row = wiersz; column (col) = kolumna; skip = pomiń
 *
 * ZOBACZ TEŻ: t02_controlflow/Control03Loops (pętle), t02_controlflow/Control05LoopPatterns (szukanie pierwszego
 *   pasującego elementu), t03_arrays/Arrays02MultiDim (tablice dwuwymiarowe)
 * </pre>
 */
public class Control04BreakContinueLabels {

    public static void main(String[] args) {
        title("Control04 — break, continue i etykiety");

        breakBasics();          // break basics = podstawy break
        continueBasics();       // continue basics = podstawy continue
        breakInsideSwitch();    // break inside switch = break wewnątrz switcha
        nestedLoops();          // nested loops = pętle zagnieżdżone
        labeledBreakContinue(); // labeled break continue = break i continue z etykietą
        flagInsteadOfLabel();   // flag instead of label = flaga zamiast etykiety
        methodWithReturn();     // method with return = metoda z return
        continueInWhilePitfall(); // continue in while pitfall = pułapka continue w while
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. BREAK — PRZERWIJ PĘTLĘ
    // =================================================================================================

    /**
     * 1. break kończy pętlę natychmiast — reszta ciała i kolejne obroty są pomijane.
     */
    static void breakBasics() {
        section("1. break — koniec pętli");

        int[] temperatures = {12, 15, -3, 18, -7}; // temperatures = temperatury (tablica: t03_arrays)
        for (int i = 0; i < temperatures.length; i++) {
            if (temperatures[i] < 0) {
                System.out.println("pierwszy mróz: dzień " + i + ", " + temperatures[i] + "°C");
                break;
            }
            System.out.println("dzień " + i + ": " + temperatures[i] + "°C — bez mrozu");
        }
        // WYNIK: dzień 0: 12°C — bez mrozu
        // WYNIK: dzień 1: 15°C — bez mrozu
        // WYNIK: pierwszy mróz: dzień 2, -3°C
        // Dni 3 i 4 w ogóle nie zostały sprawdzone — break zakończył pętlę.

        // Zakupy z budżetem: kupujemy po kolei, dopóki starcza pieniędzy.
        int[] prices = {40, 25, 60, 30}; // prices = ceny
        int budget = 100;  // budget = budżet
        int spent = 0;     // spent = wydano
        int bought = 0;    // bought = kupiono (sztuk)
        for (int price : prices) {
            if (spent + price > budget) {
                break;     // na ten produkt już nie starczy — kończymy zakupy
            }
            spent += price;
            bought++;
        }
        System.out.println("kupiono " + bought + " produkty za " + spent + " zł");
        // WYNIK: kupiono 2 produkty za 65 zł

        // DOBRA PRAKTYKA: break jest dobry, gdy „szukamy do skutku” — dalsze obroty nic by nie zmieniły.
        // Jeśli pętla ma kilka break-ów w różnych miejscach, trudno ją zrozumieć — rozważ osobną metodę.
    }

    // =================================================================================================
    // 2. CONTINUE — POMIŃ RESZTĘ OBROTU
    // =================================================================================================

    /**
     * 2. continue przerywa tylko bieżący obrót. W pętli for skacze do aktualizacji (i++), potem do warunku.
     */
    static void continueBasics() {
        section("2. continue — pomiń ten obrót");

        for (int i = 1; i <= 10; i++) {
            if (i % 3 == 0) {
                continue;   // wielokrotności 3 pomijamy
            }
            System.out.print(i + " ");
        }
        System.out.println();
        // WYNIK: 1 2 4 5 7 8 10

        // continue jako „strażnik w pętli” (jak klauzula strażnika z Control01): odrzuć i idź dalej.
        int itSalaries = 0; // it salaries = suma pensji działu IT
        for (Employee e : SampleData.employees()) { // e = employee = pracownik
            if (e.department() != Department.IT) {
                continue;
            }
            itSalaries += e.salary(); // salary = pensja
        }
        show("suma pensji IT", itSalaries);
        // WYNIK: suma pensji IT → 53600

        // PRZED (bez continue): cała logika wcięta w if.
        //   for (Employee e : lista) {
        //       if (e.department() == Department.IT) {
        //           ... wiele linii ...
        //       }
        //   }
        // PO (z continue): odrzucenie na początku, główna logika bez dodatkowego wcięcia.
        // Przy jednej linii w środku oba style są w porządku. Przy wielu — continue jest czytelniejsze.
    }

    // =================================================================================================
    // 3. BREAK WEWNĄTRZ SWITCHA W PĘTLI
    // =================================================================================================

    /**
     * 3. W klasycznym switchu break kończy SWITCH, a nie pętlę, w której ten switch stoi.
     */
    static void breakInsideSwitch() {
        section("3. PUŁAPKA: break w switchu nie kończy pętli");

        String[] commands = {"dodaj", "dodaj", "stop", "dodaj"}; // commands = polecenia
        int items = 0; // items = liczba elementów (produktów w koszyku)
        for (String cmd : commands) {
            switch (cmd) {
                case "dodaj":
                    items++;
                    break;
                case "stop":
                    System.out.println("stop! (ale break kończy tylko switch...)");
                    break;   // ← kończy SWITCH, pętla leci dalej!
                default:
                    break;
            }
        }
        // WYNIK: stop! (ale break kończy tylko switch...)
        show("produkty po „stop” (błąd)", items);
        // WYNIK: produkty po „stop” (błąd) → 3
        // Czwarte „dodaj” się wykonało, chociaż wcześniej było „stop”.

        // Rozwiązanie: etykieta na pętli i break z etykietą.
        items = 0;
        commandLoop:  // command loop = pętla poleceń (etykieta)
        for (String cmd : commands) {
            switch (cmd) {
                case "dodaj" -> items++;
                case "stop" -> {
                    break commandLoop;   // kończy PĘTLĘ oznaczoną etykietą
                }
                default -> System.out.println("nieznane: " + cmd);
            }
        }
        show("produkty po „stop” (poprawnie)", items);
        // WYNIK: produkty po „stop” (poprawnie) → 2
        // Inna droga: zamiast etykiety — metoda i return (sekcja 7).
    }

    // =================================================================================================
    // 4. PĘTLE ZAGNIEŻDŻONE — BREAK DZIAŁA NA NAJBLIŻSZĄ
    // =================================================================================================

    /**
     * 4. W pętlach zagnieżdżonych zwykły break przerywa tylko pętlę WEWNĘTRZNĄ.
     * Zewnętrzna działa dalej i uruchamia wewnętrzną od nowa.
     */
    static void nestedLoops() {
        section("4. Pętle zagnieżdżone — break przerywa tylko wewnętrzną");

        for (int row = 1; row <= 3; row++) {       // row = wiersz
            for (int col = 1; col <= 3; col++) {   // col = kolumna
                if (col == 2) {
                    break;                         // wychodzi z pętli po col, NIE z pętli po row
                }
                System.out.print("(" + row + "," + col + ") ");
            }
        }
        System.out.println();
        // WYNIK: (1,1) (2,1) (3,1)

        for (int row = 1; row <= 3; row++) {
            for (int col = 1; col <= 3; col++) {
                if (col == 2) {
                    continue;                      // pomija tylko (row, 2)
                }
                System.out.print("(" + row + "," + col + ") ");
            }
        }
        System.out.println();
        // WYNIK: (1,1) (1,3) (2,1) (2,3) (3,1) (3,3)
    }

    // =================================================================================================
    // 5. ETYKIETY: BREAK I CONTINUE DLA PĘTLI ZEWNĘTRZNEJ
    // =================================================================================================

    /**
     * 5. Etykieta (nazwa z dwukropkiem przed pętlą) pozwala przerwać lub kontynuować pętlę zewnętrzną.
     * Tablica dwuwymiarowa to „tablica wierszy”: grid[r][c] = element w wierszu r i kolumnie c.
     */
    static void labeledBreakContinue() {
        section("5. break i continue z etykietą");

        int[][] grid = {      // grid = siatka; 3 wiersze po 3 liczby (szczegóły: t03_arrays/Arrays02MultiDim)
                {4, 8, 15},
                {16, 23, 42},
                {7, 99, 3}
        };

        // Szukamy 23 BEZ etykiety — break przerywa tylko wewnętrzną pętlę:
        int checks = 0; // checks = liczba sprawdzeń
        for (int r = 0; r < grid.length; r++) {             // grid.length = liczba wierszy
            for (int c = 0; c < grid[r].length; c++) {      // grid[r].length = liczba kolumn w wierszu r
                checks++;
                if (grid[r][c] == 23) {
                    break;
                }
            }
        }
        show("sprawdzeń bez etykiety", checks);
        // WYNIK: sprawdzeń bez etykiety → 8
        // Po znalezieniu 23 zewnętrzna pętla przeszła jeszcze cały wiersz 2 — niepotrzebna praca.

        // To samo Z etykietą — koniec obu pętli naraz:
        checks = 0;
        int foundRow = -1; // found row = znaleziony wiersz; -1 = „nie znaleziono”
        int foundCol = -1; // found col = znaleziona kolumna
        search:            // search = szukanie (etykieta pętli zewnętrznej)
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                checks++;
                if (grid[r][c] == 23) {
                    foundRow = r;
                    foundCol = c;
                    break search;
                }
            }
        }
        show("23 znalezione w [wiersz, kolumna]", "[" + foundRow + ", " + foundCol + "]");
        // WYNIK: 23 znalezione w [wiersz, kolumna] → [1, 1]
        show("sprawdzeń z etykietą", checks);
        // WYNIK: sprawdzeń z etykietą → 5

        // continue z etykietą: sumujemy wiersze, ale wiersz z błędnym (ujemnym) odczytem pomijamy w całości.
        int[][] readings = { // readings = odczyty
                {5, 7, 9},
                {4, -1, 6},
                {8, 8, 8}
        };
        rows:
        for (int r = 0; r < readings.length; r++) {
            int rowSum = 0; // row sum = suma wiersza
            for (int c = 0; c < readings[r].length; c++) {
                if (readings[r][c] < 0) {
                    System.out.println("wiersz " + r + ": błędny odczyt, pomijam");
                    continue rows;   // następny obrót ZEWNĘTRZNEJ pętli — println poniżej się nie wykona
                }
                rowSum += readings[r][c];
            }
            System.out.println("wiersz " + r + ": suma " + rowSum);
        }
        // WYNIK: wiersz 0: suma 21
        // WYNIK: wiersz 1: błędny odczyt, pomijam
        // WYNIK: wiersz 2: suma 24

        // PUŁAPKA: etykieta musi stać BEZPOŚREDNIO przed pętlą. „break search;” poza tą pętlą to błąd kompilacji.
        // Etykiety to NIE jest „goto” — nie da się skoczyć w dowolne miejsce, tylko wyjść z oznaczonej pętli.
    }

    // =================================================================================================
    // 6. FLAGA ZAMIAST ETYKIETY
    // =================================================================================================

    /**
     * 6. Zamiast etykiety można użyć flagi boolean dopisanej do warunków obu pętli.
     * Działa, ale warunki się wydłużają i łatwo o pomyłkę.
     */
    static void flagInsteadOfLabel() {
        section("6. Flaga boolean zamiast etykiety");

        int[][] grid = {
                {4, 8, 15},
                {16, 23, 42},
                {7, 99, 3}
        };
        boolean found = false; // found = znaleziono (flaga)
        int foundRow = -1;
        int foundCol = -1;
        for (int r = 0; r < grid.length && !found; r++) {
            for (int c = 0; c < grid[r].length && !found; c++) {
                if (grid[r][c] == 99) {
                    found = true;
                    foundRow = r;   // zapisujemy pozycję TERAZ — po wyjściu r i c już nie istnieją
                    foundCol = c;
                }
            }
        }
        show("99 (flaga) w [wiersz, kolumna]", "[" + foundRow + ", " + foundCol + "]");
        // WYNIK: 99 (flaga) w [wiersz, kolumna] → [2, 1]

        // Porównanie:
        //   etykieta — krótko, ale rzadko używana składnia (część osób jej nie zna),
        //   flaga    — zwykła składnia, ale dodatkowa zmienna i „&& !found” w każdym warunku,
        //   metoda + return (sekcja 7) — zwykle NAJCZYTELNIEJSZE.
    }

    // =================================================================================================
    // 7. METODA + RETURN — NAJCZYSTSZE WYJŚCIE Z WIELU PĘTLI
    // =================================================================================================

    /**
     * 7. return kończy całą metodę — a więc wszystkie pętle naraz. Wydzielenie szukania do metody
     * daje nazwę (co robimy) i usuwa etykiety oraz flagi.
     */
    static void methodWithReturn() {
        section("7. DOBRA PRAKTYKA: metoda + return zamiast etykiet");

        int[][] grid = {
                {4, 8, 15},
                {16, 23, 42},
                {7, 99, 3}
        };
        show("findPosition(grid, 42)", findPosition(grid, 42)); // find position = znajdź pozycję
        // WYNIK: findPosition(grid, 42) → [1, 2]
        show("findPosition(grid, 5)", findPosition(grid, 5));
        // WYNIK: findPosition(grid, 5) → brak

        // DOBRA PRAKTYKA: gdy potrzebujesz etykiety, najpierw zapytaj: „czy to nie powinna być osobna metoda?”.
        // Metoda ma nazwę, da się ją przetestować osobno, a return jest zrozumiały dla każdego.
    }

    /** Zwraca pozycję pierwszego wystąpienia target jako tekst „[r, c]” albo „brak”. */
    static String findPosition(int[][] grid, int target) {
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                if (grid[r][c] == target) {
                    return "[" + r + ", " + c + "]";   // koniec metody = koniec obu pętli
                }
            }
        }
        return "brak";
    }

    // =================================================================================================
    // 8. PUŁAPKA: CONTINUE W WHILE POMIJA ZMIANĘ LICZNIKA
    // =================================================================================================

    /**
     * 8. W while nie ma osobnej części „update” — licznik zmieniamy w ciele. continue przed tą zmianą
     * sprawia, że licznik stoi w miejscu, a pętla kręci się w nieskończoność.
     */
    static void continueInWhilePitfall() {
        section("8. PUŁAPKA: continue w while i pominięty i++");

        // Tego NIE uruchamiamy — program by się zawiesił:
        //   int i = 0;
        //   while (i < 10) {
        //       if (i % 2 == 0) {
        //           continue;      ← skok do warunku; i++ poniżej POMINIĘTE, i zostaje 0 na zawsze
        //       }
        //       System.out.print(i + " ");
        //       i++;
        //   }
        // Dla i = 0: 0 % 2 == 0 → continue → warunek 0 < 10 → znowu 0 % 2 == 0 → continue → ... bez końca.

        // Poprawka 1: zmień licznik PRZED continue (tu: na samym początku ciała).
        int i = 0;
        while (i < 10) {
            i++;
            if (i % 2 == 0) {
                continue;
            }
            System.out.print(i + " ");
        }
        System.out.println();
        // WYNIK: 1 3 5 7 9

        // Poprawka 2: użyj for — continue w for skacze do części „update”, więc j++ zawsze się wykona.
        for (int j = 0; j < 10; j++) {
            if (j % 2 == 0) {
                continue;
            }
            System.out.print(j + " ");
        }
        System.out.println();
        // WYNIK: 1 3 5 7 9

        // DOBRA PRAKTYKA: gdy pętla ma licznik, wybierz for. Zmiana licznika w nawiasie for jest odporna
        // na continue i od razu widać, jak pętla się kończy.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • break — kończy najbliższą pętlę (albo klasyczny switch). Kod po pętli wykonuje się dalej.
     *   • continue — kończy bieżący obrót; for: skok do update (i++), while/do-while: skok do warunku.
     *   • W pętlach zagnieżdżonych break/continue bez etykiety dotyczą pętli WEWNĘTRZNEJ.
     *   • Etykieta:  outer: for (...) { for (...) { break outer; / continue outer; } }
     *   • break w klasycznym switchu wewnątrz pętli kończy tylko SWITCH — pętla leci dalej.
     *   • Alternatywy dla etykiet: flaga boolean w warunkach pętli albo (najlepiej) metoda + return.
     *   • continue w while przed zmianą licznika = pętla nieskończona. Licznik → pętla for.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się break od continue?
     *   2. Co wypisze:  for (int i = 0; i < 5; i++) { if (i == 3) { break; } System.out.print(i); }  ?
     *   3. Co wypisze:  for (int i = 0; i < 5; i++) { if (i == 3) { continue; } System.out.print(i); }  ?
     *   4. ZNAJDŹ BŁĄD (pętla się nie kończy):  int n = 0;  while (n < 5) { if (n == 2) { continue; } n++; }
     *   5. Co wypisze:  outer: for (int a = 0; a < 3; a++) { for (int b = 0; b < 3; b++) { if (b == 1) { continue outer; } System.out.print(a + "" + b + " "); } }  ?
     *   6. ZNAJDŹ BŁĄD:  pętla po poleceniach z klasycznym switchem, w którym „case "koniec": break;” ma zakończyć pętlę.
     *   7. Dlaczego metoda z return jest zwykle lepsza niż etykieta?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: pierwsza wielokrotność 7", "14, -1, 21",
                () -> exercise1(10, 30) + ", " + exercise1(1, 6) + ", " + exercise1(21, 25));
        Check.equal("ćw. 2: suma dodatnich do zera", "7, 0, 15",
                () -> exercise2(new int[] {3, -1, 4, 0, 6}) + ", " + exercise2(new int[] {-2, -3})
                        + ", " + exercise2(new int[] {5, 5, 5}));
        Check.equal("ćw. 3: pierwszy wiersz z liczbą ujemną", "1, -1",
                () -> exercise3(new int[][] {{1, 2}, {3, -4}, {-5, 6}}) + ", " + exercise3(new int[][] {{1}, {2}}));
        Check.equal("ćw. 4: para o danej sumie", "0,1 | 2,3 | brak",
                () -> exercise4(new int[] {2, 7, 11, 15}, 9) + " | " + exercise4(new int[] {3, 5, 8, 1}, 9)
                        + " | " + exercise4(new int[] {1, 2}, 10));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "14, -1, 21",
                () -> solution1(10, 30) + ", " + solution1(1, 6) + ", " + solution1(21, 25));
        Check.equal("ćw. 2 (wzorzec)", "7, 0, 15",
                () -> solution2(new int[] {3, -1, 4, 0, 6}) + ", " + solution2(new int[] {-2, -3})
                        + ", " + solution2(new int[] {5, 5, 5}));
        Check.equal("ćw. 3 (wzorzec)", "1, -1",
                () -> solution3(new int[][] {{1, 2}, {3, -4}, {-5, 6}}) + ", " + solution3(new int[][] {{1}, {2}}));
        Check.equal("ćw. 4 (wzorzec)", "0,1 | 2,3 | brak",
                () -> solution4(new int[] {2, 7, 11, 15}, 9) + " | " + solution4(new int[] {3, 5, 8, 1}, 9)
                        + " | " + solution4(new int[] {1, 2}, 10));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć pierwszą liczbę z przedziału od from do to (włącznie) podzielną przez 7.
     * Gdy takiej nie ma — zwróć -1.
     * Podpowiedź: pętla for od from do to; przy pierwszym trafieniu od razu return (albo zapisz wynik i break).
     */
    static int exercise1(int from, int to) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): sumuj liczby z tablicy po kolei. Ujemne pomijaj (continue),
     * a gdy trafisz na 0 — zakończ sumowanie (break). Liczby po zerze się nie liczą.
     * Podpowiedź: for-each po tablicy, dwa if-y na początku ciała, potem sum += n.
     */
    static int exercise2(int[] numbers) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ kod z etykietą na metodę z return (bez etykiety i bez flagi).
     * Metoda zwraca numer pierwszego wiersza, w którym jest liczba ujemna, albo -1.
     * <pre>{@code
     * int result = -1;
     * scan:
     * for (int r = 0; r < grid.length; r++) {
     *     for (int c = 0; c < grid[r].length; c++) {
     *         if (grid[r][c] < 0) {
     *             result = r;
     *             break scan;
     *         }
     *     }
     * }
     * return result;
     * }</pre>
     * Podpowiedź: zamiast „result = r; break scan;” napisz po prostu „return r;”, a na końcu metody „return -1;”.
     */
    static int exercise3(int[][] grid) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): znajdź PIERWSZĄ parę pozycji i, j (i mniejsze od j), dla której
     * numbers[i] + numbers[j] == target. Zwróć tekst "i,j" (np. "0,1") albo "brak".
     * „Pierwsza” = najmniejsze i, a przy tym samym i — najmniejsze j.
     * Podpowiedź: zewnętrzna pętla po i od 0, wewnętrzna po j od i + 1. Wyjście z obu pętli: return
     * (albo etykieta z break — spróbuj obu wersji).
     */
    static String exercise4(int[] numbers, int target) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int from, int to) {
        for (int n = from; n <= to; n++) {
            if (n % 7 == 0) {
                return n;
            }
        }
        return -1;
    }

    static int solution2(int[] numbers) {
        int sum = 0;
        for (int n : numbers) {
            if (n == 0) {
                break;
            }
            if (n < 0) {
                continue;
            }
            sum += n;
        }
        return sum;
    }

    static int solution3(int[][] grid) {
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                if (grid[r][c] < 0) {
                    return r;
                }
            }
        }
        return -1;
    }

    static String solution4(int[] numbers, int target) {
        for (int i = 0; i < numbers.length; i++) {
            for (int j = i + 1; j < numbers.length; j++) {
                if (numbers[i] + numbers[j] == target) {
                    return i + "," + j;
                }
            }
        }
        return "brak";
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. break kończy całą pętlę; continue kończy tylko bieżący obrót i przechodzi do następnego.
     *   2. „012” — przy i == 3 pętla się kończy.
     *   3. „0124” — przy i == 3 obrót jest pomijany, ale pętla działa dalej.
     *   4. Dla n == 2 continue pomija n++, więc n zostaje 2 na zawsze. Zmień n przed continue albo użyj for.
     *   5. „00 10 20 ” — dla każdego a wypisuje się tylko b == 0; przy b == 1 continue outer przechodzi
     *      od razu do następnego a.
     *   6. break w klasycznym switchu kończy tylko switch, pętla czyta kolejne polecenia.
     *      Potrzebna etykieta na pętli (break petla;), flaga albo metoda z return.
     *   7. Metoda ma nazwę mówiącą, co robi, da się ją przetestować osobno, a return rozumie każdy.
     *      Etykiety są rzadko używane i utrudniają czytanie długich pętli.
     */
    // </editor-fold>
}
