package t03_arrays;

import helpers.Check;

import java.util.Arrays;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Tablice wielowymiarowe — tablice tablic
 *        (multidimensional = wielowymiarowy; row = wiersz; column = kolumna; jagged = postrzępiony)
 *
 * W SKRÓCIE:
 *   int[][] to „tablica, której elementami są tablice int[]”. Pierwszy indeks wybiera wiersz,
 *   drugi — kolumnę w tym wierszu. Wiersze mogą mieć różne długości (tablica postrzępiona).
 *   Do wypisania całości służy Arrays.deepToString.
 *
 * ANALOGIA: sala kinowa. grid[rząd][miejsce]. Najpierw wybierasz rząd, potem miejsce w tym rzędzie.
 *   W nietypowej sali (amfiteatr) rzędy mogą mieć różną liczbę miejsc — to tablica postrzępiona.
 *
 * JAK TO DZIAŁA:
 *   int[][] g = {{1, 2, 3}, {4, 5, 6}};
 *
 *   g ──► ┌─────┐
 *         │  ●──┼──► [1, 2, 3]     g[0]       g.length    = 2 (liczba wierszy)
 *         │  ●──┼──► [4, 5, 6]     g[1]       g[1].length = 3 (liczba kolumn w wierszu 1)
 *         └─────┘
 *   g[1][2] → najpierw g[1] (wiersz [4, 5, 6]), potem jego element [2] → 6.
 *   Każdy wiersz to OSOBNY obiekt tablicy — dlatego wiersze mogą mieć różną długość, a nawet być null.
 *
 * SŁÓWKA:
 *   grid = siatka; matrix = macierz; transpose = transpozycja (zamiana wierszy z kolumnami);
 *   board = plansza; winner = zwycięzca; deep = głęboki (deepToString — „wypisz także tablice w środku”)
 *
 * ZOBACZ TEŻ: t03_arrays/Arrays01Basics (tablice jednowymiarowe), t02_controlflow/Control04BreakContinueLabels
 *   (przerywanie pętli zagnieżdżonych), t03_arrays/Arrays03Utility (clone i płytka kopia tablic 2D)
 * </pre>
 */
public class Arrays02MultiDim {

    public static void main(String[] args) {
        title("Arrays02 — tablice dwuwymiarowe");

        createAndPrint();       // create and print = tworzenie i wypisywanie
        initializerAndAccess(); // initializer and access = inicjalizator i dostęp
        nestedLoops();          // nested loops = pętle zagnieżdżone
        rowsAreArrays();        // rows are arrays = wiersze to zwykłe tablice
        jaggedArrays();         // jagged arrays = tablice postrzępione
        nullRowsPitfall();      // null rows pitfall = pułapka wierszy null
        rowAndColumnSums();     // row and column sums = sumy wierszy i kolumn
        transpose();            // transpose = transpozycja
        ticTacToe();            // tic tac toe = kółko i krzyżyk
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. TWORZENIE I WYPISYWANIE
    // =================================================================================================

    /**
     * 1. new int[3][4] tworzy 3 wiersze po 4 kolumny, wypełnione zerami.
     * Arrays.toString pokazuje tylko adresy wierszy — potrzebny jest Arrays.deepToString.
     */
    static void createAndPrint() {
        section("1. new int[3][4] i Arrays.deepToString");

        int[][] grid = new int[3][4]; // grid = siatka: 3 wiersze, 4 kolumny
        show("grid.length (liczba wierszy)", grid.length);
        // WYNIK: grid.length (liczba wierszy) → 3
        show("grid[0].length (kolumny w wierszu 0)", grid[0].length);
        // WYNIK: grid[0].length (kolumny w wierszu 0) → 4
        show("Arrays.deepToString(grid)", Arrays.deepToString(grid)); // deep to string = wypisz „w głąb”
        // WYNIK: Arrays.deepToString(grid) → [[0, 0, 0, 0], [0, 0, 0, 0], [0, 0, 0, 0]]

        // PUŁAPKA: Arrays.toString wypisuje tylko JEDEN poziom — elementami są tablice, więc widać ich adresy.
        String shallow = Arrays.toString(grid); // shallow = płytki
        show("Arrays.toString(grid) zaczyna się od \"[[I@\"", shallow.startsWith("[[I@"));
        // WYNIK: Arrays.toString(grid) zaczyna się od "[[I@" → true
        // Pełny tekst to np. [[I@6d06d69c, [I@7852e922, ...] — adresy zmieniają się przy każdym uruchomieniu.

        // DOBRA PRAKTYKA: 1D → Arrays.toString, 2D i więcej → Arrays.deepToString.
    }

    // =================================================================================================
    // 2. INICJALIZATOR I DOSTĘP DO ELEMENTÓW
    // =================================================================================================

    /**
     * 2. Inicjalizator: klamry w klamrach, każdy wewnętrzny nawias to jeden wiersz.
     */
    static void initializerAndAccess() {
        section("2. Inicjalizator {{...}, {...}} i dostęp [wiersz][kolumna]");

        int[][] seats = {        // seats = miejsca w kinie: 1 = zajęte, 0 = wolne
                {1, 0, 0, 1},
                {1, 1, 0, 0},
                {0, 0, 0, 1}
        };
        show("seats[1][2]", seats[1][2]);
        // WYNIK: seats[1][2] → 0
        seats[1][2] = 1;         // rezerwacja: rząd 1, miejsce 2
        show("rząd 1 po rezerwacji", Arrays.toString(seats[1]));
        // WYNIK: rząd 1 po rezerwacji → [1, 1, 1, 0]

        int lastRow = seats.length - 1;             // last row = ostatni wiersz
        int lastCol = seats[lastRow].length - 1;    // last col = ostatnia kolumna TEGO wiersza
        show("ostatnie miejsce ostatniego rzędu", seats[lastRow][lastCol]);
        // WYNIK: ostatnie miejsce ostatniego rzędu → 1

        // PUŁAPKA: kolejność indeksów. seats[kolumna][wiersz] to częsty błąd — tu wyrzuciłby wyjątek:
        expectThrows("seats[3][1] (zamienione indeksy)", () -> System.out.println(seats[3][1]));
        // WYNIK: ✔ seats[3][1] (zamienione indeksy) → rzucono ArrayIndexOutOfBoundsException: Index 3 out of bounds for length 3
        // DOBRA PRAKTYKA: nazywaj liczniki r / c (row / col), a nie i / j — łatwiej zauważyć zamianę.
    }

    // =================================================================================================
    // 3. PĘTLE ZAGNIEŻDŻONE
    // =================================================================================================

    /**
     * 3. Zewnętrzna pętla idzie po wierszach, wewnętrzna po kolumnach danego wiersza.
     * Warunek wewnętrznej pętli to r-ty wiersz: c {@code <} grid[r].length.
     */
    static void nestedLoops() {
        section("3. Pętle zagnieżdżone: wiersze i kolumny");

        int[][] seats = {
                {1, 0, 0, 1},
                {1, 1, 1, 0},
                {0, 0, 0, 1}
        };
        for (int r = 0; r < seats.length; r++) {
            System.out.print("rząd " + (r + 1) + ": ");
            for (int c = 0; c < seats[r].length; c++) {
                System.out.print(seats[r][c] == 1 ? "X " : ". ");
            }
            System.out.println();
        }
        // WYNIK: rząd 1: X . . X
        // WYNIK: rząd 2: X X X .
        // WYNIK: rząd 3: . . . X

        // for-each: zewnętrzna zmienna to cały wiersz (int[]), wewnętrzna — pojedyncza liczba.
        int free = 0; // free = wolne
        for (int[] row : seats) {        // row = wiersz
            for (int seat : row) {       // seat = miejsce
                if (seat == 0) {
                    free++;
                }
            }
        }
        show("wolnych miejsc", free);
        // WYNIK: wolnych miejsc → 6

        // DOBRA PRAKTYKA: warunek wewnętrznej pętli pisz jako c < seats[r].length (długość TEGO wiersza),
        // a nie seats[0].length — wtedy kod zadziała także dla wierszy różnej długości.
    }

    // =================================================================================================
    // 4. WIERSZ TO ZWYKŁA TABLICA
    // =================================================================================================

    /**
     * 4. grid[r] to referencja do tablicy-wiersza. Można ją zapamiętać, podmienić albo zamienić z innym wierszem.
     */
    static void rowsAreArrays() {
        section("4. Wiersz to zwykła tablica (referencja)");

        int[][] m = {{1, 2}, {3, 4}, {5, 6}}; // m = macierz
        int[] firstRow = m[0];                // first row = pierwszy wiersz — ALIAS, nie kopia
        firstRow[0] = 100;
        show("m po firstRow[0] = 100", Arrays.deepToString(m));
        // WYNIK: m po firstRow[0] = 100 → [[100, 2], [3, 4], [5, 6]]

        // Zamiana wierszy = zamiana trzech referencji (elementy nie są kopiowane — szybko!).
        int[] tmp = m[0]; // tmp = temporary = tymczasowa
        m[0] = m[2];
        m[2] = tmp;
        show("po zamianie wierszy 0 i 2", Arrays.deepToString(m));
        // WYNIK: po zamianie wierszy 0 i 2 → [[5, 6], [3, 4], [100, 2]]

        m[1] = new int[] {7, 8, 9};   // wiersz może zostać podmieniony na dłuższy
        show("po podmianie wiersza 1", Arrays.deepToString(m));
        // WYNIK: po podmianie wiersza 1 → [[5, 6], [7, 8, 9], [100, 2]]
    }

    // =================================================================================================
    // 5. TABLICE POSTRZĘPIONE (JAGGED)
    // =================================================================================================

    /**
     * 5. new int[5][] tworzy tylko „kręgosłup” z 5 pustymi miejscami na wiersze. Każdy wiersz
     * tworzymy osobno — i każdy może mieć inną długość.
     */
    static void jaggedArrays() {
        section("5. Tablice postrzępione: trójkąt Pascala");

        int[][] pascal = new int[5][];            // pascal = trójkąt Pascala; wiersze na razie null
        for (int r = 0; r < pascal.length; r++) {
            pascal[r] = new int[r + 1];           // wiersz r ma r + 1 elementów
            pascal[r][0] = 1;
            pascal[r][r] = 1;
            for (int c = 1; c < r; c++) {
                pascal[r][c] = pascal[r - 1][c - 1] + pascal[r - 1][c]; // suma dwóch liczb „nad” nami
            }
        }
        for (int[] row : pascal) {
            System.out.println(Arrays.toString(row));
        }
        // WYNIK: [1]
        // WYNIK: [1, 1]
        // WYNIK: [1, 2, 1]
        // WYNIK: [1, 3, 3, 1]
        // WYNIK: [1, 4, 6, 4, 1]

        int[][] weeks = {{1, 2, 3}, {4}, {}}; // weeks = tygodnie; postrzępiona od razu z inicjalizatora
        show("długości wierszy weeks", weeks[0].length + ", " + weeks[1].length + ", " + weeks[2].length);
        // WYNIK: długości wierszy weeks → 3, 1, 0
    }

    // =================================================================================================
    // 6. PUŁAPKA: WIERSZE NULL
    // =================================================================================================

    /**
     * 6. Po new int[3][] wiersze jeszcze nie istnieją (są null). Zapis do rows[0][0] kończy się
     * NullPointerException.
     */
    static void nullRowsPitfall() {
        section("6. PUŁAPKA: new int[3][] — wiersze są null");

        int[][] rows = new int[3][]; // rows = wiersze
        show("rows", Arrays.deepToString(rows));
        // WYNIK: rows → [null, null, null]
        expectThrows("rows[0][0] = 5", () -> rows[0][0] = 5);
        // WYNIK: ✔ rows[0][0] = 5 → rzucono NullPointerException: Cannot store to int array because "rows[0]" is null
        expectThrows("rows[1].length", () -> System.out.println(rows[1].length));
        // WYNIK: ✔ rows[1].length → rzucono NullPointerException: Cannot read the array length because "rows[1]" is null

        rows[0] = new int[2];   // najpierw utwórz wiersz...
        rows[0][0] = 5;         // ...potem wpisuj
        show("rows po utworzeniu wiersza 0", Arrays.deepToString(rows));
        // WYNIK: rows po utworzeniu wiersza 0 → [[5, 0], null, null]

        // DOBRA PRAKTYKA: gdy wszystkie wiersze mają mieć tę samą długość, podaj OBA wymiary: new int[3][4].
        // Pusty drugi wymiar tylko wtedy, gdy zaraz w pętli tworzysz wiersze różnej długości.
    }

    // =================================================================================================
    // 7. SUMY WIERSZY I KOLUMN
    // =================================================================================================

    /**
     * 7. Suma wiersza: jedna pętla po kolumnach. Sumy kolumn: osobna tablica akumulatorów,
     * po jednym na każdą kolumnę.
     */
    static void rowAndColumnSums() {
        section("7. Sumy wierszy i kolumn");

        int[][] sales = {    // sales = sprzedaż; wiersze = sklepy, kolumny = miesiące (styczeń, luty, marzec)
                {120, 80, 150},
                {90, 110, 70},
                {60, 40, 100}
        };
        int[] columnSums = new int[sales[0].length]; // column sums = sumy kolumn (po jednej na miesiąc)
        int total = 0;
        for (int r = 0; r < sales.length; r++) {
            int rowSum = 0; // row sum = suma wiersza — zerowana dla KAŻDEGO wiersza
            for (int c = 0; c < sales[r].length; c++) {
                rowSum += sales[r][c];
                columnSums[c] += sales[r][c];
            }
            total += rowSum;
            System.out.println("sklep " + r + ": " + rowSum);
        }
        // WYNIK: sklep 0: 350
        // WYNIK: sklep 1: 270
        // WYNIK: sklep 2: 200
        show("sumy miesięcy (kolumn)", Arrays.toString(columnSums));
        // WYNIK: sumy miesięcy (kolumn) → [270, 230, 320]
        show("razem", total);
        // WYNIK: razem → 820

        // PUŁAPKA: „int rowSum = 0;” PRZED zewnętrzną pętlą → sumy kolejnych wierszy by się kumulowały
        // (350, 620, 820 zamiast 350, 270, 200).
    }

    // =================================================================================================
    // 8. TRANSPOZYCJA
    // =================================================================================================

    /**
     * 8. Transpozycja zamienia wiersze z kolumnami: element [r][c] trafia na [c][r].
     * Macierz 2 x 3 staje się macierzą 3 x 2.
     */
    static void transpose() {
        section("8. Transpozycja: wiersze ↔ kolumny");

        int[][] a = {
                {1, 2, 3},
                {4, 5, 6}
        };
        int[][] t = new int[a[0].length][a.length]; // t = transposed = transponowana; 3 wiersze, 2 kolumny
        for (int r = 0; r < a.length; r++) {
            for (int c = 0; c < a[r].length; c++) {
                t[c][r] = a[r][c];
            }
        }
        show("a", Arrays.deepToString(a));
        // WYNIK: a → [[1, 2, 3], [4, 5, 6]]
        show("transpozycja a", Arrays.deepToString(t));
        // WYNIK: transpozycja a → [[1, 4], [2, 5], [3, 6]]

        // PUŁAPKA: wymiary nowej tablicy są ZAMIENIONE: new int[liczbaKolumn][liczbaWierszy].
        // new int[a.length][a[0].length] (2 x 3) → wyjątek ArrayIndexOutOfBoundsException przy t[2][...].
    }

    // =================================================================================================
    // 9. PLANSZA DO GRY: KÓŁKO I KRZYŻYK
    // =================================================================================================

    /**
     * 9. char[][] jako plansza 3 x 3. Sprawdzamy zwycięzcę: wiersze, kolumny i dwie przekątne.
     */
    static void ticTacToe() {
        section("9. char[][]: kółko i krzyżyk");

        char[][] board = {       // board = plansza
                {'X', 'O', 'X'},
                {'O', 'X', 'O'},
                {'O', ' ', 'X'}
        };
        printBoard(board);       // print board = wypisz planszę
        // WYNIK: X|O|X
        // WYNIK: O|X|O
        // WYNIK: O| |X
        show("zwycięzca", winner(board)); // winner = zwycięzca
        // WYNIK: zwycięzca → X

        board[2][2] = 'O';       // zmieniamy jedno pole — przekątna X przerwana
        board[2][1] = 'X';
        show("zwycięzca po zmianie", winner(board));
        // WYNIK: zwycięzca po zmianie → -

        char[][] columnWin = {   // column win = wygrana w kolumnie
                {'O', 'X', ' '},
                {'O', 'X', ' '},
                {' ', 'X', 'O'}
        };
        show("zwycięzca (kolumna 1)", winner(columnWin));
        // WYNIK: zwycięzca (kolumna 1) → X
    }

    /** Wypisuje planszę, oddzielając pola znakiem |. */
    static void printBoard(char[][] board) {
        for (char[] row : board) {
            StringBuilder line = new StringBuilder(); // StringBuilder: t02_controlflow/Control05LoopPatterns
            for (int c = 0; c < row.length; c++) {
                if (c > 0) {
                    line.append('|');
                }
                line.append(row[c]);
            }
            System.out.println(line);
        }
    }

    /** Zwraca 'X' albo 'O', gdy ktoś ma trzy w linii; '-' gdy nikt. Puste pole to spacja. */
    static char winner(char[][] b) {
        for (int i = 0; i < 3; i++) {
            if (b[i][0] != ' ' && b[i][0] == b[i][1] && b[i][1] == b[i][2]) {   // wiersz i
                return b[i][0];
            }
            if (b[0][i] != ' ' && b[0][i] == b[1][i] && b[1][i] == b[2][i]) {   // kolumna i
                return b[0][i];
            }
        }
        boolean mainDiagonal = b[0][0] == b[1][1] && b[1][1] == b[2][2];      // main diagonal = główna przekątna
        boolean antiDiagonal = b[0][2] == b[1][1] && b[1][1] == b[2][0];      // anti diagonal = druga przekątna
        if (b[1][1] != ' ' && (mainDiagonal || antiDiagonal)) {
            return b[1][1];
        }
        return '-';
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • int[][] g = new int[wiersze][kolumny];  albo  int[][] g = {{1, 2}, {3, 4}};
     *   • g.length = liczba wierszy; g[r].length = liczba kolumn w wierszu r; element: g[r][c].
     *   • Wypisywanie: Arrays.deepToString(g). Arrays.toString(g) pokaże tylko adresy wierszy.
     *   • Pętle: for (int r ...) { for (int c = 0; c < g[r].length; c++) { ... } }  albo  for (int[] row : g).
     *   • Wiersz to osobna tablica: można go zapamiętać (alias), podmienić, zamienić z innym.
     *   • Tablica postrzępiona: new int[n][] + w pętli g[r] = new int[dlugosc]. Do tego czasu wiersze są null!
     *   • Sumy kolumn: tablica akumulatorów int[liczbaKolumn], columnSums[c] += g[r][c].
     *   • Transpozycja: t = new int[kolumny][wiersze]; t[c][r] = a[r][c].
     *
     * PYTANIA KONTROLNE:
     *   1. Co zwraca g.length, a co g[0].length dla int[][] g = new int[4][7]?
     *   2. Co wypisze:  int[][] g = {{1, 2}, {3, 4, 5}};  System.out.println(g[1][2] + g[0][1]);  ?
     *   3. ZNAJDŹ BŁĄD:  int[][] g = new int[2][];  g[0][0] = 1;
     *   4. Co wypisze:  System.out.println(Arrays.deepToString(new int[2][2]));  ?
     *   5. ZNAJDŹ BŁĄD (tablica postrzępiona):  for (int r = 0; r < g.length; r++) { for (int c = 0; c < g[0].length; c++) { suma += g[r][c]; } }
     *   6. Dlaczego zamiana dwóch wierszy tablicy 2D jest szybka, niezależnie od ich długości?
     *   7. Jakie wymiary ma transpozycja tablicy 4 x 2?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: suma wszystkich elementów", 21,
                () -> exercise1(new int[][] {{1, 2}, {3}, {4, 5, 6}}));
        Check.equal("ćw. 2: maksimum każdego wiersza", "[9, -2, 7]",
                () -> Arrays.toString(exercise2(new int[][] {{3, 9, 1}, {-4, -2}, {7}})));
        Check.equal("ćw. 3: liczba ujemnych (for-each)", 3,
                () -> exercise3(new int[][] {{-1, 2}, {-3, -4, 5}}));
        Check.equal("ćw. 4: saper — liczby sąsiednich min", "[[-1, 1, 0], [2, 2, 1], [1, -1, 1]]",
                () -> Arrays.deepToString(exercise4(new char[][] {{'*', '.', '.'}, {'.', '.', '.'}, {'.', '*', '.'}})));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 21,
                () -> solution1(new int[][] {{1, 2}, {3}, {4, 5, 6}}));
        Check.equal("ćw. 2 (wzorzec)", "[9, -2, 7]",
                () -> Arrays.toString(solution2(new int[][] {{3, 9, 1}, {-4, -2}, {7}})));
        Check.equal("ćw. 3 (wzorzec)", 3,
                () -> solution3(new int[][] {{-1, 2}, {-3, -4, 5}}));
        Check.equal("ćw. 4 (wzorzec)", "[[-1, 1, 0], [2, 2, 1], [1, -1, 1]]",
                () -> Arrays.deepToString(solution4(new char[][] {{'*', '.', '.'}, {'.', '.', '.'}, {'.', '*', '.'}})));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zsumuj wszystkie elementy tablicy 2D (wiersze mogą mieć różne długości).
     * Podpowiedź: dwie pętle for-each: for (int[] row : grid) { for (int x : row) { ... } }.
     */
    static int exercise1(int[][] grid) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć tablicę, w której element r to największa liczba z wiersza r.
     * Zakładamy, że żaden wiersz nie jest pusty. Uwaga na wiersze z samymi ujemnymi liczbami!
     * Podpowiedź: int[] result = new int[grid.length]; maksimum wiersza startuje od grid[r][0].
     */
    static int[] exercise2(int[][] grid) {
        // TODO: twoje rozwiązanie
        return new int[0];
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ na dwie pętle for-each (bez indeksów r i c), wynik bez zmian.
     * <pre>{@code
     * int count = 0;
     * for (int r = 0; r < grid.length; r++) {
     *     for (int c = 0; c < grid[r].length; c++) {
     *         if (grid[r][c] < 0) {
     *             count++;
     *         }
     *     }
     * }
     * return count;
     * }</pre>
     * Podpowiedź: zewnętrzna zmienna pętli ma typ int[], wewnętrzna — int.
     */
    static int exercise3(int[][] grid) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): saper. Dla pola z miną ('*') wpisz -1, dla pozostałych — liczbę min
     * wśród 8 sąsiadów (poziomo, pionowo i na ukos). Wynik to nowa tablica int[][] tych samych wymiarów.
     * Podpowiedź: dla pola (r, c) dwie pętle dr i dc od -1 do 1; pomiń dr == 0 i dc == 0 (samo pole)
     * oraz sąsiadów poza planszą (nr mniejsze od 0 albo nr równe field.length itd.).
     */
    static int[][] exercise4(char[][] field) {
        // TODO: twoje rozwiązanie
        return new int[0][0];
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int[][] grid) {
        int sum = 0;
        for (int[] row : grid) {
            for (int x : row) {
                sum += x;
            }
        }
        return sum;
    }

    static int[] solution2(int[][] grid) {
        int[] result = new int[grid.length];
        for (int r = 0; r < grid.length; r++) {
            int max = grid[r][0];
            for (int c = 1; c < grid[r].length; c++) {
                max = Math.max(max, grid[r][c]);
            }
            result[r] = max;
        }
        return result;
    }

    static int solution3(int[][] grid) {
        int count = 0;
        for (int[] row : grid) {
            for (int x : row) {
                if (x < 0) {
                    count++;
                }
            }
        }
        return count;
    }

    static int[][] solution4(char[][] field) {
        int[][] result = new int[field.length][];
        for (int r = 0; r < field.length; r++) {
            result[r] = new int[field[r].length];
            for (int c = 0; c < field[r].length; c++) {
                if (field[r][c] == '*') {
                    result[r][c] = -1;
                    continue;
                }
                int mines = 0; // mines = miny
                for (int dr = -1; dr <= 1; dr++) {          // dr = delta row = przesunięcie wiersza
                    for (int dc = -1; dc <= 1; dc++) {      // dc = delta column = przesunięcie kolumny
                        int nr = r + dr;                    // nr = neighbour row = wiersz sąsiada
                        int nc = c + dc;                    // nc = neighbour column = kolumna sąsiada
                        if ((dr == 0 && dc == 0) || nr < 0 || nr >= field.length || nc < 0 || nc >= field[nr].length) {
                            continue;
                        }
                        if (field[nr][nc] == '*') {
                            mines++;
                        }
                    }
                }
                result[r][c] = mines;
            }
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. g.length = 4 (wiersze), g[0].length = 7 (kolumny).
     *   2. 7 — g[1][2] to 5, g[0][1] to 2.
     *   3. new int[2][] nie tworzy wierszy — g[0] jest null → NullPointerException. Najpierw g[0] = new int[...].
     *   4. [[0, 0], [0, 0]]
     *   5. Warunek c < g[0].length używa długości PIERWSZEGO wiersza. Dla dłuższych wierszy część elementów
     *      zostanie pominięta, dla krótszych — wyjątek. Poprawnie: c < g[r].length.
     *   6. Bo zamieniamy tylko referencje (adresy) wierszy — trzy przypisania, bez kopiowania elementów.
     *   7. 2 x 4 (dwa wiersze, cztery kolumny).
     */
    // </editor-fold>
}
