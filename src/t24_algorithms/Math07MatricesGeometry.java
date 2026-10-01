package t24_algorithms;

import helpers.Check;

import java.util.Arrays;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Macierze i geometria obliczeniowa — tablice 2D jako liczby, punkty i wielokąty
 *        (matrix = macierz; determinant = wyznacznik; polygon = wielokąt; shoelace = "sznurowadło")
 *
 * W SKRÓCIE:
 *   Macierz to prostokątna tabela liczb — w Javie najwygodniej jako {@code int[][]}. Dodawanie, mnożenie,
 *   transpozycja i wyznacznik to podstawowe operacje pod grafiką, uczeniem maszynowym i rozwiązywaniem
 *   układów równań. Druga połowa lekcji to geometria obliczeniowa na płaskich punktach: odległość, pole
 *   wielokąta, "czy punkt jest w środku?" — klasyka gier, map i systemów GIS.
 *
 * ANALOGIA: macierz to arkusz kalkulacyjny bez nagłówków — same liczby w wierszach i kolumnach.
 *   Mnożenie macierzy to jakby każdy wiersz jednego arkusza "witał się" ze każdą kolumną drugiego
 *   i obie strony liczyły wspólny iloczyn skalarny. Punkt w wielokącie to pytanie "czy stoję w pokoju,
 *   czy na zewnątrz?", sprawdzane strzelaniem promieniem i liczeniem, ile razy przebija ściany.
 *
 * JAK TO DZIAŁA:
 *   dodawanie A+B        wymiary MUSZĄ się zgadzać, dodajemy pole po polu          O(wiersze·kolumny)
 *   mnożenie A×B         kolumny A muszą = wiersze B; wynik: wiersze A × kolumny B O(wiersze_A·kolumny_B·kolumny_A)
 *   transpozycja         wiersze ↔ kolumny: wynik[j][i] = a[i][j]
 *   wyznacznik 2×2       ad − bc
 *   wyznacznik 3×3       reguła Sarrusa: suma 3 iloczynów "w dół" minus suma 3 "w górę"
 *   odległość punktów    twierdzenie Pitagorasa: √((x2−x1)² + (y2−y1)²)
 *   pole wielokąta       wzór Gaussa (shoelace): ½|Σ (x_i·y_{i+1} − x_{i+1}·y_i)|
 *   punkt w wielokącie   ray casting: strzel promieniem w prawo, policz przecięcia z krawędziami — nieparzyste = w środku
 *   orientacja 3 punktów znak iloczynu wektorowego (ABxAC): dodatni = przeciwnie do wskazówek, ujemny = zgodnie, 0 = współliniowe
 *
 * SŁÓWKA:
 *   matrix = macierz; row/column = wiersz/kolumna; transpose = transpozycja; determinant = wyznacznik;
 *   identity matrix = macierz jednostkowa; dimension = wymiar; point = punkt; distance = odległość;
 *   polygon = wielokąt; shoelace formula = wzór Gaussa (dosłownie: wzór "sznurowadła"); ray casting =
 *   rzucanie promienia; orientation = orientacja (skrętność); cross product = iloczyn wektorowy;
 *   clockwise/counterclockwise = zgodnie/przeciwnie do wskazówek zegara; collinear = współliniowe (na jednej prostej).
 *
 * ZOBACZ TEŻ: t03_arrays/Arrays02MultiDim (tablice 2D, deepToString), t09_records/Records01Basics (rekordy —
 *             tu record Point), t24_algorithms/Math06Statistics (statystyka opisowa), t24_algorithms/Math08BigNumbers
 *             (gdy wyznaczniki/mnożenia przepełniają int — BigInteger).
 * </pre>
 */
public class Math07MatricesGeometry {

    public static void main(String[] args) {
        title("Math07 — macierze i geometria obliczeniowa");

        macierzJakoTablica();      // macierz jako tablica = matrix as array
        dodawanieMacierzy();        // dodawanie macierzy = matrix addition
        mnozenieMacierzy();         // mnozenie macierzy = matrix multiplication
        transpozycja();              // transpozycja = transpose
        macierzJednostkowa();        // macierz jednostkowa = identity matrix
        wyznacznik();                 // wyznacznik = determinant
        punktyIOdleglosc();           // punkty i odleglosc = points and distance
        polePoligonu();                // pole poligonu = polygon area
        punktWFigurze();                // punkt w figurze = point in shape
        orientacjaPunktow();             // orientacja punktow = orientation of points
        exercises();                      // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. MACIERZ JAKO TABLICA 2D
    // =================================================================================================

    /**
     * 1. W Javie macierz to po prostu {@code int[][]} — tablica tablic. {@code m.length} to liczba wierszy,
     * {@code m[0].length} to liczba kolumn (zakładając macierz PROSTOKĄTNĄ, czyli każdy wiersz tej samej
     * długości — tzw. "jagged array" z różnymi długościami wierszy to co innego, patrz t03_arrays/Arrays02MultiDim).
     */
    static void macierzJakoTablica() {
        section("1. Macierz jako tablica 2D");

        int[][] a = {{1, 2}, {3, 4}};
        show("wiersze", a.length);
        show("kolumny", a[0].length);
        show("macierz A", Arrays.deepToString(a));
        // WYNIK: wiersze → 2
        // WYNIK: kolumny → 2
        // WYNIK: macierz A → [[1, 2], [3, 4]]

        // PUŁAPKA: println(a) wypisałby coś jak "[[I@1b6d3586" — adres tablicy tablic, nie zawartość.
        //   Zawsze używaj Arrays.deepToString dla tablic 2D (Arrays.toString wystarcza tylko dla 1D).
        show("println(a) wyglądałoby jak", a.getClass().getSimpleName() + "@<hash>");
        // WYNIK: println(a) wyglądałoby jak → int[][]@<hash>
    }

    // =================================================================================================
    // 2. DODAWANIE MACIERZY
    // =================================================================================================

    /**
     * 2. Dodawanie macierzy wymaga IDENTYCZNYCH wymiarów — dodajemy odpowiadające sobie pola.
     * Złożoność O(wiersze · kolumny) — jeden przebieg po wszystkich polach.
     */
    static void dodawanieMacierzy() {
        section("2. Dodawanie macierzy");

        int[][] a = {{1, 2}, {3, 4}};
        int[][] b = {{5, 6}, {7, 8}};
        show("A", Arrays.deepToString(a));
        show("B", Arrays.deepToString(b));
        show("A + B", Arrays.deepToString(add(a, b)));
        // WYNIK: A → [[1, 2], [3, 4]]
        // WYNIK: B → [[5, 6], [7, 8]]
        // WYNIK: A + B → [[6, 8], [10, 12]]

        // DOBRA PRAKTYKA: sprawdzaj wymiary PRZED pętlą (a.length == b.length, a[0].length == b[0].length)
        //   i rzucaj czytelny wyjątek — inaczej dostaniesz ArrayIndexOutOfBoundsException w środku pętli,
        //   co niczego nie tłumaczy osobie czytającej stos wywołań.
    }

    // =================================================================================================
    // 3. MNOŻENIE MACIERZY — REGUŁA WYMIARÓW
    // =================================================================================================

    /**
     * 3. Żeby pomnożyć A (m×n) przez B (n×p), liczba KOLUMN A musi równać się liczbie WIERSZY B.
     * Wynik ma wymiar m×p. Pole wyniku [i][j] to iloczyn skalarny wiersza i macierzy A z kolumną j
     * macierzy B. Złożoność O(m·p·n) — dla macierzy kwadratowych n×n to klasyczne O(n³).
     */
    static void mnozenieMacierzy() {
        section("3. Mnożenie macierzy — reguła wymiarów");

        int[][] a = {{1, 2}, {3, 4}};
        int[][] b = {{5, 6}, {7, 8}};
        show("A × B (2×2 razy 2×2)", Arrays.deepToString(multiply(a, b)));
        // WYNIK: A × B (2×2 razy 2×2) → [[19, 22], [43, 50]]

        int[][] c = {{1, 2, 3}, {4, 5, 6}};             // 2×3
        int[][] d = {{7, 8}, {9, 10}, {11, 12}};         // 3×2
        show("C (2×3)", Arrays.deepToString(c));
        show("D (3×2)", Arrays.deepToString(d));
        show("C × D (wynik 2×2)", Arrays.deepToString(multiply(c, d)));
        // WYNIK: C (2×3) → [[1, 2, 3], [4, 5, 6]]
        // WYNIK: D (3×2) → [[7, 8], [9, 10], [11, 12]]
        // WYNIK: C × D (wynik 2×2) → [[58, 64], [139, 154]]

        // D ma 2 kolumny, C ma 2 wiersze — to się ZGADZA, więc D×C jest poprawnym mnożeniem (wynik 3×3):
        show("D × C (wynik 3×3)", Arrays.deepToString(multiply(d, c)));
        // WYNIK: D × C (wynik 3×3) → [[39, 54, 69], [49, 68, 87], [59, 82, 105]]

        expectThrows("C × A? kolumny C (3) != wiersze A (2)", () -> multiply(c, a));
        // WYNIK: ✔ C × A? kolumny C (3) != wiersze A (2) → rzucono IllegalArgumentException: niezgodne wymiary: 3 != 2

        // PUŁAPKA: mnożenie macierzy NIE JEST przemienne — A×B zwykle ≠ B×A, a tu D×C i C×D mają
        //   nawet różne WYMIARY wyniku (2×2 kontra 3×3)! Kolejność ma znaczenie.
    }

    // =================================================================================================
    // 4. TRANSPOZYCJA
    // =================================================================================================

    /**
     * 4. Transpozycja zamienia wiersze z kolumnami: wynik[j][i] = oryginał[i][j]. Macierz m×n staje się n×m.
     * Dwukrotna transpozycja wraca do oryginału. Złożoność O(wiersze · kolumny).
     */
    static void transpozycja() {
        section("4. Transpozycja");

        int[][] c = {{1, 2, 3}, {4, 5, 6}};
        show("C (2×3)", Arrays.deepToString(c));
        show("transpose(C) (3×2)", Arrays.deepToString(transpose(c)));
        show("transpose(transpose(C))", Arrays.deepToString(transpose(transpose(c))));
        // WYNIK: C (2×3) → [[1, 2, 3], [4, 5, 6]]
        // WYNIK: transpose(C) (3×2) → [[1, 4], [2, 5], [3, 6]]
        // WYNIK: transpose(transpose(C)) → [[1, 2, 3], [4, 5, 6]]
    }

    // =================================================================================================
    // 5. MACIERZ JEDNOSTKOWA (IDENTITY)
    // =================================================================================================

    /**
     * 5. Macierz jednostkowa ma jedynki na przekątnej, zera wszędzie indziej. To "1" świata macierzy:
     * A × I == A dla dowolnej macierzy A o zgodnych wymiarach (mnożenie przez nią niczego nie zmienia).
     */
    static void macierzJednostkowa() {
        section("5. Macierz jednostkowa (identity)");

        show("identity(3)", Arrays.deepToString(identity(3)));
        // WYNIK: identity(3) → [[1, 0, 0], [0, 1, 0], [0, 0, 1]]

        int[][] a = {{1, 2}, {3, 4}};
        show("A × identity(2)", Arrays.deepToString(multiply(a, identity(2))));
        // WYNIK: A × identity(2) → [[1, 2], [3, 4]]

        note("A × I == A — identity jest neutralnym elementem mnożenia macierzy, tak jak 1 dla liczb.");
    }

    // =================================================================================================
    // 6. WYZNACZNIK: 2×2 I 3×3 (REGUŁA SARRUSA)
    // =================================================================================================

    /**
     * 6. Wyznacznik (determinant) to pojedyncza liczba opisująca macierz kwadratową — m.in. mówi, czy
     * macierz ma odwrotność (wyznacznik 0 → nie ma). Dla 2×2: {@code ad − bc}. Dla 3×3 reguła Sarrusa:
     * dopisz pierwsze dwie kolumny z prawej, zsumuj 3 iloczyny "w dół" (↘) i odejmij 3 iloczyny "w górę" (↗).
     */
    static void wyznacznik() {
        section("6. Wyznacznik: 2×2 i 3×3 (reguła Sarrusa)");

        int[][] a = {{1, 2}, {3, 4}};
        show("A", Arrays.deepToString(a));
        show("det(A) = 1·4 − 2·3", determinant2x2(a));
        // WYNIK: A → [[1, 2], [3, 4]]
        // WYNIK: det(A) = 1·4 − 2·3 → -2

        int[][] m = {{1, 2, 3}, {4, 5, 6}, {7, 8, 10}};
        show("M", Arrays.deepToString(m));
        show("det(M), reguła Sarrusa", determinant3x3(m));
        // WYNIK: M → [[1, 2, 3], [4, 5, 6], [7, 8, 10]]
        // WYNIK: det(M), reguła Sarrusa → -3

        int[][] singular = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};               // trzeci wiersz = ciąg arytmetyczny
        show("det(macierz osobliwa 1,2,3/4,5,6/7,8,9)", determinant3x3(singular));
        // WYNIK: det(macierz osobliwa 1,2,3/4,5,6/7,8,9) → 0

        // PUŁAPKA: wyznacznik równy 0 oznacza macierz OSOBLIWĄ (singular) — nie da się jej odwrócić,
        //   układ równań, który reprezentuje, nie ma dokładnie jednego rozwiązania.
        // DOBRA PRAKTYKA: dla macierzy większych niż 3×3 reguła Sarrusa już NIE działa — potrzeba eliminacji
        //   Gaussa albo rozwinięcia Laplace'a (poza zakresem tej lekcji).
    }

    // =================================================================================================
    // 7. PUNKTY I ODLEGŁOŚĆ
    // =================================================================================================

    /**
     * 7. Punkt na płaszczyźnie reprezentujemy małym rekordem {@code Point(double x, double y)} — niezmienny,
     * z equals/hashCode/toString za darmo (t09_records). Odległość między punktami to zwykłe twierdzenie
     * Pitagorasa zastosowane do różnicy współrzędnych.
     */
    static void punktyIOdleglosc() {
        section("7. Punkty i odległość");

        Point origin = new Point(0, 0);                                   // origin = początek układu
        Point p = new Point(3, 4);
        show("origin", origin);
        show("p", p);
        show("distance(origin, p)", distance(origin, p));
        // WYNIK: origin → Point[x=0.0, y=0.0]
        // WYNIK: p → Point[x=3.0, y=4.0]
        // WYNIK: distance(origin, p) → 5.0

        note("Trójkąt 3-4-5 — klasyczny przykład, gdzie wynik pierwiastka wychodzi całkowity.");
    }

    // =================================================================================================
    // 8. POLE WIELOKĄTA — WZÓR GAUSSA (SHOELACE)
    // =================================================================================================

    /**
     * 8. Wzór Gaussa (shoelace formula — "wzór sznurowadła", bo przy liczeniu na kartce mnożniki krzyżują
     * się jak sznurowadło w bucie) liczy pole DOWOLNEGO wielokąta z samych współrzędnych wierzchołków,
     * podanych w kolejności (po obwodzie). Złożoność O(liczba wierzchołków) — jeden przebieg.
     */
    static void polePoligonu() {
        section("8. Pole wielokąta — wzór Gaussa (shoelace)");

        List<Point> square = List.of(new Point(0, 0), new Point(4, 0), new Point(4, 4), new Point(0, 4));
        show("kwadrat 4×4", polygonArea(square));
        // WYNIK: kwadrat 4×4 → 16.0

        List<Point> triangle = List.of(new Point(0, 0), new Point(4, 0), new Point(0, 3));
        show("trójkąt prostokątny (4, 3)", polygonArea(triangle));
        // WYNIK: trójkąt prostokątny (4, 3) → 6.0

        // PUŁAPKA: kolejność wierzchołków MUSI iść po obwodzie (zgodnie lub przeciwnie do wskazówek zegara) —
        //   losowa kolejność daje bezsensowny wynik (samoprzecinający się "wielokąt").
        // DOBRA PRAKTYKA: wynik bierzemy z Math.abs() — kolejność zgodna ze wskazówkami zegara dałaby
        //   tę samą wartość bezwzględną, ale z minusem (patrz sekcja 10 o orientacji).
    }

    // =================================================================================================
    // 9. PUNKT W PROSTOKĄCIE / PUNKT W WIELOKĄCIE (RAY CASTING)
    // =================================================================================================

    /**
     * 9. Dla prostokąta test jest trywialny — porównanie współrzędnych z zakresem. Dla DOWOLNEGO wielokąta
     * używamy ray castingu: z punktu strzelamy "promieniem" w prawo do nieskończoności i liczymy, ile
     * krawędzi wielokąta przecina. Nieparzysta liczba przecięć = punkt w środku, parzysta = na zewnątrz
     * (klasyczny algorytm PNPOLY). Złożoność O(liczba krawędzi).
     */
    static void punktWFigurze() {
        section("9. Punkt w prostokącie / punkt w wielokącie (ray casting)");

        Point bottomLeft = new Point(0, 0);
        Point topRight = new Point(4, 4);
        show("(2,2) w prostokącie [0,0]-[4,4]?", pointInRectangle(new Point(2, 2), bottomLeft, topRight));
        show("(5,5) w prostokącie [0,0]-[4,4]?", pointInRectangle(new Point(5, 5), bottomLeft, topRight));
        show("(4,4) na brzegu — w prostokącie?", pointInRectangle(new Point(4, 4), bottomLeft, topRight));
        // WYNIK: (2,2) w prostokącie [0,0]-[4,4]? → true
        // WYNIK: (5,5) w prostokącie [0,0]-[4,4]? → false
        // WYNIK: (4,4) na brzegu — w prostokącie? → true

        List<Point> square = List.of(new Point(0, 0), new Point(4, 0), new Point(4, 4), new Point(0, 4));
        show("(2,2) w wielokącie (kwadrat)?", pointInPolygon(new Point(2, 2), square));
        show("(5,5) w wielokącie (kwadrat)?", pointInPolygon(new Point(5, 5), square));
        // WYNIK: (2,2) w wielokącie (kwadrat)? → true
        // WYNIK: (5,5) w wielokącie (kwadrat)? → false

        // PUŁAPKA: pointInRectangle działa TYLKO dla prostokątów ustawionych równolegle do osi (axis-aligned).
        //   Obrócony prostokąt albo dowolny inny kształt wymaga ray castingu albo testu orientacji (sekcja 10).
    }

    // =================================================================================================
    // 10. ORIENTACJA TRZECH PUNKTÓW (ZNAK ILOCZYNU WEKTOROWEGO)
    // =================================================================================================

    /**
     * 10. Orientacja trójki punktów (a, b, c) mówi, w którą stronę "skręca" droga a→b→c: dodatni iloczyn
     * wektorowy wektorów ab i ac = przeciwnie do wskazówek zegara (counterclockwise), ujemny = zgodnie
     * (clockwise), zero = punkty leżą na jednej prostej (collinear). To budulec wielu algorytmów geometrycznych
     * (otoczka wypukła, test "punkt w trójkącie" z ćwiczenia 5).
     */
    static void orientacjaPunktow() {
        section("10. Orientacja trzech punktów (znak iloczynu wektorowego)");

        Point a = new Point(0, 0);
        show("orientation((0,0),(4,0),(4,4)) — skręt w lewo", orientation(a, new Point(4, 0), new Point(4, 4)));
        show("orientation((0,0),(4,0),(4,-4)) — skręt w prawo", orientation(a, new Point(4, 0), new Point(4, -4)));
        show("orientation((0,0),(2,0),(4,0)) — jedna prosta", orientation(a, new Point(2, 0), new Point(4, 0)));
        // WYNIK: orientation((0,0),(4,0),(4,4)) — skręt w lewo → 1
        // WYNIK: orientation((0,0),(4,0),(4,-4)) — skręt w prawo → -1
        // WYNIK: orientation((0,0),(2,0),(4,0)) — jedna prosta → 0

        note("Test \"punkt w trójkącie\" (ćwiczenie 5): punkt jest W ŚRODKU, gdy ma TĘ SAMĄ orientację");
        note("względem wszystkich trzech boków trójkąta (albo leży dokładnie na którymś z nich).");
    }

    // =================================================================================================
    // FUNKCJE POMOCNICZE — MACIERZE
    // =================================================================================================

    /** add = dodaj macierze tych samych wymiarów, pole po polu. */
    static int[][] add(int[][] a, int[][] b) {
        int rows = a.length;
        int cols = a[0].length;
        if (rows != b.length || cols != b[0].length) {
            throw new IllegalArgumentException("niezgodne wymiary macierzy");
        }
        int[][] result = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[i][j] = a[i][j] + b[i][j];
            }
        }
        return result;
    }

    /** multiply = pomnóż macierze A (m×n) i B (n×p) — kolumny A muszą równać się wierszom B. */
    static int[][] multiply(int[][] a, int[][] b) {
        int aRows = a.length;
        int aCols = a[0].length;
        int bRows = b.length;
        int bCols = b[0].length;
        if (aCols != bRows) {
            throw new IllegalArgumentException("niezgodne wymiary: " + aCols + " != " + bRows);
        }
        int[][] result = new int[aRows][bCols];
        for (int i = 0; i < aRows; i++) {
            for (int j = 0; j < bCols; j++) {
                int sum = 0;
                for (int k = 0; k < aCols; k++) {
                    sum += a[i][k] * b[k][j];
                }
                result[i][j] = sum;
            }
        }
        return result;
    }

    /** transpose = zamień wiersze z kolumnami: wynik[j][i] = a[i][j]. */
    static int[][] transpose(int[][] a) {
        int rows = a.length;
        int cols = a[0].length;
        int[][] result = new int[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[j][i] = a[i][j];
            }
        }
        return result;
    }

    /** identity = macierz jednostkowa n×n (jedynki na przekątnej). */
    static int[][] identity(int n) {
        int[][] result = new int[n][n];
        for (int i = 0; i < n; i++) {
            result[i][i] = 1;
        }
        return result;
    }

    /** determinant2x2 = wyznacznik macierzy 2×2: ad − bc. */
    static int determinant2x2(int[][] m) {
        return m[0][0] * m[1][1] - m[0][1] * m[1][0];
    }

    /** determinant3x3 = wyznacznik macierzy 3×3 regułą Sarrusa. */
    static int determinant3x3(int[][] m) {
        int a = m[0][0], b = m[0][1], c = m[0][2];
        int d = m[1][0], e = m[1][1], f = m[1][2];
        int g = m[2][0], h = m[2][1], i = m[2][2];
        return a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
    }

    // =================================================================================================
    // FUNKCJE POMOCNICZE — GEOMETRIA
    // =================================================================================================

    /** Point = punkt na płaszczyźnie. Rekord zagnieżdżony — niezmienny, equals/hashCode/toString gratis. */
    record Point(double x, double y) {
    }

    /** distance = odległość euklidesowa dwóch punktów (twierdzenie Pitagorasa). */
    static double distance(Point a, Point b) {
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** polygonArea = pole wielokąta wzorem Gaussa (shoelace); wierzchołki podane po obwodzie. */
    static double polygonArea(List<Point> polygon) {
        int n = polygon.size();
        double sum = 0;
        for (int i = 0; i < n; i++) {
            Point p1 = polygon.get(i);
            Point p2 = polygon.get((i + 1) % n);
            sum += p1.x() * p2.y() - p2.x() * p1.y();
        }
        return Math.abs(sum) / 2.0;
    }

    /** pointInRectangle = test punktu w prostokącie osiowo-równoległym (brzeg liczy się jako "w środku"). */
    static boolean pointInRectangle(Point p, Point bottomLeft, Point topRight) {
        return p.x() >= bottomLeft.x() && p.x() <= topRight.x()
                && p.y() >= bottomLeft.y() && p.y() <= topRight.y();
    }

    /** pointInPolygon = test punktu w dowolnym wielokącie algorytmem ray casting (PNPOLY). */
    static boolean pointInPolygon(Point p, List<Point> polygon) {
        boolean inside = false;
        int n = polygon.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Point pi = polygon.get(i);
            Point pj = polygon.get(j);
            boolean crosses = (pi.y() > p.y()) != (pj.y() > p.y())
                    && p.x() < (pj.x() - pi.x()) * (p.y() - pi.y()) / (pj.y() - pi.y()) + pi.x();
            if (crosses) {
                inside = !inside;
            }
        }
        return inside;
    }

    /** orientation = znak iloczynu wektorowego ab×ac: 1 = w lewo, -1 = w prawo, 0 = współliniowe. */
    static int orientation(Point a, Point b, Point c) {
        double cross = (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
        if (cross > 0) { return 1; }
        if (cross < 0) { return -1; }
        return 0;
    }

    /** pointInTriangle = test "ta sama strona" dla wszystkich trzech boków trójkąta (brzeg = w środku). */
    static boolean pointInTriangle(Point p, Point a, Point b, Point c) {
        int o1 = orientation(a, b, p);
        int o2 = orientation(b, c, p);
        int o3 = orientation(c, a, p);
        boolean hasNegative = o1 < 0 || o2 < 0 || o3 < 0;
        boolean hasPositive = o1 > 0 || o2 > 0 || o3 > 0;
        return !(hasNegative && hasPositive);
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   DODAWANIE       wymiary identyczne, pole po polu                           O(w·k)
     *   MNOŻENIE A×B    kolumny A == wiersze B; wynik: wiersze A × kolumny B        O(m·p·n), NIE przemienne!
     *   TRANSPOZYCJA    wynik[j][i] = a[i][j]; m×n → n×m
     *   IDENTITY        jedynki na przekątnej; A × I == A
     *   WYZNACZNIK 2×2  ad − bc            WYZNACZNIK 3×3  reguła Sarrusa; = 0 → macierz osobliwa
     *   ODLEGŁOŚĆ       √((x2−x1)² + (y2−y1)²)  — Pitagoras
     *   POLE WIELOKĄTA  shoelace: ½|Σ(x_i·y_{i+1} − x_{i+1}·y_i)|, wierzchołki po obwodzie
     *   PUNKT W PROSTOKĄCIE  porównanie współrzędnych (tylko osiowo-równoległe)
     *   PUNKT W WIELOKĄCIE   ray casting (PNPOLY): nieparzysta liczba przecięć = w środku
     *   ORIENTACJA 3 PKT     znak ab×ac: + w lewo, − w prawo, 0 współliniowe
     *   WYPISYWANIE 2D  zawsze Arrays.deepToString — nigdy println(tablica2D)
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego A × B zwykle nie równa się B × A dla macierzy?
     *   2. Co wypisze:  System.out.println(Arrays.deepToString(new int[][]{{1,2},{3,4}}));  ?
     *   3. ZNAJDŹ BŁĄD:
     *          int[][] a = {{1,2,3}};
     *          int[][] b = {{1,2,3}};
     *          int[][] c = multiply(a, b);   // a i b to obie macierze 1×3
     *   4. Jaki jest geometryczny sens wyznacznika równego 0?
     *   5. Co wypisze:  System.out.println(orientation(new Point(0,0), new Point(5,0), new Point(5,0)));  ?
     *      (trzeci punkt pokrywa się z drugim)
     *   6. ZNAJDŹ BŁĄD: ktoś testuje punkt w obróconym o 45° kwadracie funkcją pointInRectangle — dlaczego
     *      wynik będzie zły niezależnie od tego, jak dobierze bottomLeft i topRight?
     *   7. Dlaczego wzór Gaussa (shoelace) bierze wartość BEZWZGLĘDNĄ z sumy na końcu?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        int[][] ex1A = {{0, 1}, {2, 3}};
        int[][] ex1B = {{4, 5}, {6, 7}};
        int[][] ex2A = {{1, 0}, {2, 5}, {3, 9}};
        int[][] ex3M = {{2, 0}, {0, 3}};
        Point triA = new Point(0, 0);
        Point triB = new Point(3, 4);
        Point triC = new Point(6, 0);
        Point bigA = new Point(0, 0);
        Point bigB = new Point(6, 0);
        Point bigC = new Point(3, 6);

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: dodawanie macierzy", Arrays.deepToString(new int[][]{{4, 6}, {8, 10}}),
                () -> Arrays.deepToString(exercise1(ex1A, ex1B)));
        Check.equal("ćw. 2: transpozycja 3×2", Arrays.deepToString(new int[][]{{1, 2, 3}, {0, 5, 9}}),
                () -> Arrays.deepToString(exercise2(ex2A)));
        Check.equal("ćw. 3: wyznacznik 2×2", 6, () -> exercise3(ex3M));
        Check.equal("ćw. 4: obwód trójkąta (0,0)-(3,4)-(6,0)", 16.0, () -> exercise4(triA, triB, triC));
        Check.equal("ćw. 5a: (3,2) w trójkącie (0,0)-(6,0)-(3,6)?", true,
                () -> exercise5(new Point(3, 2), bigA, bigB, bigC));
        Check.equal("ćw. 5b: (3,7) w trójkącie (0,0)-(6,0)-(3,6)?", false,
                () -> exercise5(new Point(3, 7), bigA, bigB, bigC));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", Arrays.deepToString(new int[][]{{4, 6}, {8, 10}}),
                () -> Arrays.deepToString(solution1(ex1A, ex1B)));
        Check.equal("ćw. 2 (wzorzec)", Arrays.deepToString(new int[][]{{1, 2, 3}, {0, 5, 9}}),
                () -> Arrays.deepToString(solution2(ex2A)));
        Check.equal("ćw. 3 (wzorzec)", 6, () -> solution3(ex3M));
        Check.equal("ćw. 4 (wzorzec)", 16.0, () -> solution4(triA, triB, triC));
        Check.equal("ćw. 5a (wzorzec)", true, () -> solution5(new Point(3, 2), bigA, bigB, bigC));
        Check.equal("ćw. 5b (wzorzec)", false, () -> solution5(new Point(3, 7), bigA, bigB, bigC));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): dodaj dwie macierze tych samych wymiarów.
     * Podpowiedź: pole po polu, {@code result[i][j] = a[i][j] + b[i][j]}.
     */
    static int[][] exercise1(int[][] a, int[][] b) {
        // TODO: twoje rozwiązanie
        return new int[0][0];
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć transpozycję macierzy {@code a} (m×n → n×m).
     * Podpowiedź: {@code result[j][i] = a[i][j]}.
     */
    static int[][] exercise2(int[][] a) {
        // TODO: twoje rozwiązanie
        return new int[0][0];
    }

    /**
     * ĆWICZENIE 3 (średnie): policz wyznacznik macierzy 2×2.
     * Podpowiedź: {@code ad − bc}.
     */
    static int exercise3(int[][] m) {
        // TODO: twoje rozwiązanie
        return Integer.MIN_VALUE;
    }

    /**
     * ĆWICZENIE 4 (średnie): policz obwód trójkąta o wierzchołkach a, b, c (suma trzech odległości).
     * Podpowiedź: {@code distance(a,b) + distance(b,c) + distance(c,a)}.
     */
    static double exercise4(Point a, Point b, Point c) {
        // TODO: twoje rozwiązanie
        return -1.0;
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): sprawdź, czy punkt {@code p} leży w trójkącie a-b-c (brzeg też się liczy).
     * Podpowiedź: policz orientation dla trzech par boków z p i porównaj znaki (sekcja 10, pointInTriangle).
     */
    static boolean exercise5(Point p, Point a, Point b, Point c) {
        // Pusty stub zwracałby zawsze to samo i przypadkiem "zaliczyłby" jeden z testów —
        // dlatego tu wyjątek, żeby ćwiczenie jawnie pokazywało ✘ dopóki go nie zrobisz.
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int[][] solution1(int[][] a, int[][] b) {
        return add(a, b);
    }

    static int[][] solution2(int[][] a) {
        return transpose(a);
    }

    static int solution3(int[][] m) {
        return determinant2x2(m);
    }

    static double solution4(Point a, Point b, Point c) {
        return distance(a, b) + distance(b, c) + distance(c, a);
    }

    static boolean solution5(Point p, Point a, Point b, Point c) {
        return pointInTriangle(p, a, b, c);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Mnożenie macierzy to kombinacja iloczynów skalarnych wierszy jednej i kolumn drugiej — zamiana
     *      kolejności zmienia, które wiersze z którymi kolumnami się "witają", a czasem nawet wymiary wyniku
     *      wychodzą inne (jak D×C kontra C×D w sekcji 3) albo mnożenie w drugą stronę w ogóle nie istnieje.
     *   2. [[1, 2], [3, 4]] — deepToString schodzi rekurencyjnie do wnętrza zagnieżdżonych tablic.
     *   3. a ma wymiar 1×3, b ma wymiar 1×3 — kolumny a (3) != wiersze b (1), więc multiply rzuci
     *      IllegalArgumentException. Żeby to pomnożyć, trzeba by b było wymiaru 3×cokolwiek.
     *   4. Wyznacznik 0 oznacza, że wiersze (albo kolumny) macierzy są LINIOWO ZALEŻNE — np. jeden wiersz to
     *      wielokrotność drugiego, albo leżą na tej samej "płaszczyźnie" o niższym wymiarze. Macierz "zgniata"
     *      przestrzeń zamiast ją przekształcać bez utraty informacji, więc nie da się jej odwrócić.
     *   5. 0 — b i c to ten sam punkt (5,0), więc wektor ac ma długość zero; każdy iloczyn wektorowy z wektorem
     *      zerowym wynosi 0 (formalnie trzy punkty, z których dwa się pokrywają, traktujemy jako współliniowe).
     *   6. pointInRectangle porównuje współrzędne X i Y NIEZALEŻNIE od siebie — zakłada, że boki prostokąta są
     *      równoległe do osi układu. Obrócony kwadrat ma boki pod kątem, więc taki test zawsze da błędny wynik
     *      blisko rogów; potrzebny jest pointInPolygon (ray casting) albo test orientacji względem 4 boków.
     *   7. Suma w formule Gaussa wychodzi DODATNIA dla wierzchołków podanych przeciwnie do wskazówek zegara,
     *      a UJEMNA dla kolejności zgodnej ze wskazówkami (to ten sam znak co orientation w sekcji 10) — pole
     *      fizyczne nie może być ujemne, więc bierzemy wartość bezwzględną niezależnie od kierunku obchodzenia.
     */
    // </editor-fold>
}
