package t24_algorithms;

import helpers.Check;

import java.util.Random;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Metody numeryczne — Newton, bisekcja, całkowanie, Monte Carlo
 *        (numerical method = metoda numeryczna; iteration = iteracja; convergence = zbieżność;
 *        tolerance/epsilon = tolerancja/epsilon; bisection = bisekcja (połowienie))
 *
 * W SKRÓCIE:
 *   Niektórych równań nie da się rozwiązać "wzorem" — szukamy wyniku PRZYBLIŻONEGO, iteracyjnie,
 *   zatrzymując się, gdy jesteśmy "wystarczająco blisko" (epsilon). Te same idee liczą pierwiastki
 *   (Newton, bisekcja), pola pod wykresem (całkowanie numeryczne) i nawet π (Monte Carlo).
 *
 * ANALOGIA: szukanie zgubionego klucza po ciemku po omacku — nie trafiasz od razu w idealne miejsce,
 *   tylko zbliżasz się krok po kroku, aż "wystarczająco blisko" (ręka dotyka klucza). Nie potrzebujesz
 *   wzoru matematycznego opisującego dokładnie, gdzie leży klucz — potrzebujesz TYLKO reguły "idź bliżej".
 *
 * JAK TO DZIAŁA:
 *   Newton:        x_{n+1} = x_n - f(x_n)/f'(x_n)   (dla √a: x_{n+1} = (x_n + a/x_n) / 2) — zbieżność SZYBKA
 *   Bisekcja:      jeśli f(low) i f(high) mają PRZECIWNE znaki, pierwiastek jest między nimi — połów
 *                  przedział, zatrzymaj gdy (high - low) < epsilon — zbieżność WOLNIEJSZA, ale PEWNA
 *   Trapezy:       pole pod krzywą ≈ suma trapezów między sąsiednimi punktami siatki
 *   Simpson:       pole pod krzywą ≈ suma PARABOL przez trójki punktów — dokładne dla wielomianów stopnia ≤ 3
 *   Monte Carlo:   losuj punkty, licz UDZIAŁ trafień w obszar — błąd maleje jak 1/√n (WOLNO!)
 *
 * SŁÓWKA:
 *   numerical method = metoda numeryczna; iteration = iteracja (jeden obrót pętli przybliżania);
 *   convergence = zbieżność (przybliżanie się do wyniku); tolerance / epsilon = tolerancja / epsilon
 *   (najmniejsza akceptowalna różnica); bisection = bisekcja (metoda połowienia przedziału); integration
 *   = całkowanie; trapezoid rule = metoda trapezów; Simpson's rule = metoda Simpsona; accumulated error
 *   = błąd skumulowany (narastający); seed = ziarno (generatora liczb losowych).
 *
 * ZOBACZ TEŻ: t01_basics/Basics08FloatingPoint (dlaczego double nie jest dokładny), t15_numbers/
 *             Numbers01BigDecimal (gdy dokładność jest krytyczna — pieniądze), t24_algorithms/
 *             Math04NumberSystems (poprzednia lekcja), t21_concurrency (Random i determinizm —
 *             tu używamy go do Monte Carlo, zawsze z ustalonym ziarnem).
 * </pre>
 */
public class Math05NumericalMethods {

    public static void main(String[] args) {
        title("Math05 — metody numeryczne: Newton, bisekcja, całkowanie, Monte Carlo");

        doubleEqualityPitfall();       // double equality pitfall = pułapka porównywania double
        newtonSqrt2();                 // Newton sqrt 2 = metoda Newtona dla pierwiastka z 2
        bisectionCubic();              // bisection cubic = bisekcja dla równania sześciennego
        trapezoidIntegration();        // trapezoid integration = całkowanie metodą trapezów
        simpsonIntegration();          // Simpson integration = całkowanie metodą Simpsona
        monteCarloPiDemo();            // Monte Carlo pi demo = demonstracja Monte Carlo dla π
        accumulatedErrorDemo();        // accumulated error demo = demonstracja błędu skumulowanego
        exercises();                   // exercises = ćwiczenia
    }

    /** roundTo = zaokrąglij x do podanej liczby miejsc po przecinku (do stabilnego porównywania w WYNIK/Check). */
    static double roundTo(double x, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(x * factor) / factor;
    }

    // =================================================================================================
    // 1. DLACZEGO NIE PORÓWNUJEMY double PRZEZ == — TOLERANCJA (EPSILON)
    // =================================================================================================

    /**
     * 1. Większość ułamków dziesiętnych (jak 0.1) nie ma DOKŁADNEGO zapisu binarnego w double — to tak,
     * jakby próbować zapisać 1/3 skończoną liczbą cyfr dziesiętnych. Dlatego liczby zmiennoprzecinkowe
     * porównujemy z TOLERANCJĄ (epsilon): "czy różnica jest mniejsza niż bardzo mała wartość", a nie "czy
     * są identyczne co do bitu".
     */
    static void doubleEqualityPitfall() {
        section("1. Dlaczego nie porównujemy double przez == — tolerancja (epsilon)");

        double sum = 0.1 + 0.2;
        show("0.1 + 0.2", sum);
        show("0.1 + 0.2 == 0.3 ?", sum == 0.3);
        // WYNIK: 0.1 + 0.2 → 0.30000000000000004
        // WYNIK: 0.1 + 0.2 == 0.3 ? → false

        double epsilon = 1e-9;
        show("Math.abs((0.1+0.2) - 0.3) < epsilon ?", Math.abs(sum - 0.3) < epsilon);
        // WYNIK: Math.abs((0.1+0.2) - 0.3) < epsilon ? → true

        // DOBRA PRAKTYKA: cała ta lekcja używa epsilon jako kryterium "wystarczająco blisko" —
        //   w metodzie Newtona, w bisekcji, przy porównywaniu wyniku całkowania z wartością dokładną.
        //   Nigdy nie czekaj na wynik "dokładnie zero" ani "dokładnie równy" dla obliczeń zmiennoprzecinkowych.
    }

    // =================================================================================================
    // 2. METODA NEWTONA DLA √2 (TABELA ITERACJI)
    // =================================================================================================

    /**
     * 2. Metoda Newtona przybliża pierwiastek funkcji f, idąc wzdłuż STYCZNEJ: {@code x_{n+1} = x_n -
     * f(x_n)/f'(x_n)}. Dla f(x) = x² - a (szukamy √a) wzór upraszcza się do {@code (x_n + a/x_n) / 2} —
     * to starożytna metoda babilońska. Zbiega BARDZO szybko (liczba poprawnych cyfr mniej więcej PODWAJA
     * się w każdej iteracji). PYTANIE REKRUTACYJNE: "zaimplementuj pierwiastek kwadratowy bez Math.sqrt"
     * to klasyczne zadanie o metodach numerycznych.
     */
    static void newtonSqrt2() {
        section("2. Metoda Newtona dla √2 (tabela iteracji)");

        double x = 1.0;                                               // punkt startowy
        for (int i = 1; i <= 6; i++) {
            x = newtonSqrtStep(x, 2.0);
            show("iteracja " + i, x);
        }
        // WYNIK: iteracja 1 → 1.5
        // WYNIK: iteracja 2 → 1.4166666666666665
        // WYNIK: iteracja 3 → 1.4142156862745097
        // WYNIK: iteracja 4 → 1.4142135623746899
        // WYNIK: iteracja 5 → 1.414213562373095
        // WYNIK: iteracja 6 → 1.414213562373095

        show("Math.sqrt(2) — do porównania", Math.sqrt(2));
        show("różnica od Math.sqrt(2)", Math.abs(x - Math.sqrt(2)));
        // WYNIK: Math.sqrt(2) — do porównania → 1.4142135623730951
        // WYNIK: różnica od Math.sqrt(2) → 2.220446049250313E-16
    }

    /** newtonSqrtStep = jeden krok metody Newtona dla pierwiastka z a: x_{n+1} = (x_n + a/x_n) / 2. */
    static double newtonSqrtStep(double x, double a) {
        return (x + a / x) / 2.0;
    }

    // =================================================================================================
    // 3. METODA BISEKCJI (POŁOWIENIA) DLA x^3 - x - 2 = 0
    // =================================================================================================

    /**
     * 3. Jeśli f(low) i f(high) mają PRZECIWNE znaki, a f jest ciągła, to GDZIEŚ między nimi jest
     * pierwiastek (własność Darboux). Bisekcja powtarza: policz środek, sprawdź znak f(środek), zawęź
     * przedział do tej połowy, która nadal ma przeciwne znaki na końcach. Wolniejsza niż Newton, ale nie
     * wymaga pochodnej i działa, gdy znamy tylko przedział z pierwiastkiem.
     */
    static void bisectionCubic() {
        section("3. Metoda bisekcji dla x³ - x - 2 = 0");

        show("f(1) = 1³ - 1 - 2", cubic(1));
        show("f(2) = 2³ - 2 - 2", cubic(2));
        // WYNIK: f(1) = 1³ - 1 - 2 → -2.0
        // WYNIK: f(2) = 2³ - 2 - 2 → 4.0

        double root = bisection(1, 2, 1e-9);
        show("przybliżony pierwiastek", root);
        show("f(przybliżony pierwiastek)", cubic(root));
        // WYNIK: przybliżony pierwiastek → 1.521379706915468
        // WYNIK: f(przybliżony pierwiastek) → 6.591687196078055E-10

        // PUŁAPKA: bisekcja WYMAGA f(low) i f(high) o przeciwnych znakach na starcie — bez tego nie wiemy,
        //   czy w ogóle jest tam pierwiastek (mogą być dwa, zero, albo funkcja może nie przecinać zera wcale).
    }

    /** cubic = f(x) = x³ - x - 2 (funkcja, której pierwiastka szukamy). */
    static double cubic(double x) {
        return x * x * x - x - 2;
    }

    /** bisection = przybliżony pierwiastek f w [low, high] metodą połowienia, z dokładnością epsilon. */
    static double bisection(double low, double high, double epsilon) {
        double signAtLow = Math.signum(cubic(low));
        while (high - low > epsilon) {
            double mid = (low + high) / 2.0;
            double signAtMid = Math.signum(cubic(mid));
            if (signAtMid == signAtLow) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return (low + high) / 2.0;
    }

    // =================================================================================================
    // 4. CAŁKOWANIE NUMERYCZNE x² NA [0,1]: METODA TRAPEZÓW
    // =================================================================================================

    /**
     * 4. Całka {@code ∫x² dx} na [0,1] ma dokładną wartość 1/3. Metoda trapezów dzieli [0,1] na n
     * równych pasków i przybliża pole pod krzywą w każdym pasku TRAPEZEM (odcinkiem łączącym sąsiednie
     * punkty) zamiast krzywą. Dla funkcji WYPUKŁEJ (jak x²) trapez zawsze leży NAD krzywą, więc wynik
     * nieznacznie PRZESZACOWUJE.
     */
    static void trapezoidIntegration() {
        section("4. Całkowanie numeryczne x² na [0,1]: metoda trapezów");

        for (int n : new int[]{4, 10, 100}) {
            show("trapezoid(n=" + n + ")", roundTo(trapezoid(n), 6));
        }
        show("dokładna wartość całki (1/3)", roundTo(1.0 / 3.0, 6));
        // WYNIK: trapezoid(n=4) → 0.34375
        // WYNIK: trapezoid(n=10) → 0.335
        // WYNIK: trapezoid(n=100) → 0.33335
        // WYNIK: dokładna wartość całki (1/3) → 0.333333
    }

    static double square(double x) {
        return x * x;
    }

    /** trapezoid = przybliżenie ∫x² dx na [0,1], metodą trapezów z n paskami. */
    static double trapezoid(int n) {
        double a = 0.0;
        double b = 1.0;
        double h = (b - a) / n;
        double sum = (square(a) + square(b)) / 2.0;
        for (int i = 1; i < n; i++) {
            sum += square(a + i * h);
        }
        return sum * h;
    }

    // =================================================================================================
    // 5. CAŁKOWANIE NUMERYCZNE: METODA SIMPSONA
    // =================================================================================================

    /**
     * 5. Metoda Simpsona przybliża krzywą PARABOLĄ przez każdą trójkę sąsiednich punktów (zamiast prostą
     * jak trapezy) — wagi 1, 4, 2, 4, 2, ..., 4, 1. Jest DOKŁADNA dla każdego wielomianu stopnia ≤ 3, więc
     * dla x² (stopień 2) daje wynik praktycznie równy dokładnej wartości 1/3, niezależnie od n.
     */
    static void simpsonIntegration() {
        section("5. Całkowanie numeryczne: metoda Simpsona");

        for (int n : new int[]{4, 10}) {
            show("simpson(n=" + n + ")", roundTo(simpson(n), 6));
        }
        show("dokładna wartość całki (1/3)", roundTo(1.0 / 3.0, 6));
        // WYNIK: simpson(n=4) → 0.333333
        // WYNIK: simpson(n=10) → 0.333333
        // WYNIK: dokładna wartość całki (1/3) → 0.333333

        // DOBRA PRAKTYKA: gdy funkcja jest "gładka" (bez ostrych załamań), Simpson zwykle daje dużo
        //   lepszy wynik niż trapezy przy TEJ SAMEJ liczbie punktów — niemal bez dodatkowego kosztu.
    }

    /** simpson = przybliżenie ∫x² dx na [0,1], metodą Simpsona z n paskami (n musi być parzyste). */
    static double simpson(int n) {
        if (n % 2 != 0) throw new IllegalArgumentException("n musi być parzyste");
        double a = 0.0;
        double b = 1.0;
        double h = (b - a) / n;
        double sum = square(a) + square(b);
        for (int i = 1; i < n; i++) {
            double x = a + i * h;
            sum += (i % 2 == 0 ? 2 : 4) * square(x);
        }
        return sum * h / 3.0;
    }

    // =================================================================================================
    // 6. MONTE CARLO π (Random(42), ZBIEŻNOŚĆ)
    // =================================================================================================

    /**
     * 6. Losujemy punkty w kwadracie [-1,1]×[-1,1] (pole 4) i liczymy UDZIAŁ trafiających w koło
     * o promieniu 1 (pole π·1² = π). Stosunek (trafienia / próbki) × 4 to przybliżenie π. Zbieżność jest
     * WOLNA — błąd maleje w tempie {@code 1/√n}, więc żeby zyskać jedną dodatkową cyfrę dokładności,
     * trzeba 100× WIĘCEJ próbek, nie 10×.
     */
    static void monteCarloPiDemo() {
        section("6. Monte Carlo π (Random(42), zbieżność)");

        int[] sampleSizes = {1_000, 100_000, 10_000_000};
        for (int samples : sampleSizes) {
            double pi = monteCarloPi(samples, new Random(42));
            show(samples + " próbek", roundTo(pi, 4));
        }
        show("Math.PI — do porównania", roundTo(Math.PI, 4));
        // WYNIK: 1000 próbek → 3.312
        // WYNIK: 100000 próbek → 3.142
        // WYNIK: 10000000 próbek → 3.1421
        // WYNIK: Math.PI — do porównania → 3.1416

        // Więcej próbek zbliża wynik do π, ale NIE liniowo — stąd metody Monte Carlo opłacają się głównie
        //   tam, gdzie nie ma lepszego (szybciej zbieżnego) sposobu policzenia wyniku.
    }

    /** monteCarloPi = przybliżenie π metodą Monte Carlo (losowe punkty w kwadracie, udział w kole). */
    static double monteCarloPi(int samples, Random random) {
        int insideCircle = 0;
        for (int i = 0; i < samples; i++) {
            double x = random.nextDouble() * 2 - 1;                   // przedział -1..1
            double y = random.nextDouble() * 2 - 1;
            if (x * x + y * y <= 1.0) insideCircle++;
        }
        return 4.0 * insideCircle / samples;
    }

    // =================================================================================================
    // 7. BŁĄD SKUMULOWANY — SUMOWANIE 0.1 WIELE RAZY
    // =================================================================================================

    /**
     * 7. Każde dodawanie 0.1 (liczby, która sama w sobie nie ma dokładnego zapisu binarnego) wprowadza
     * odrobinę błędu zaokrąglenia. Błędy te się KUMULUJĄ — im więcej operacji, tym większa rozbieżność od
     * matematycznie oczekiwanego wyniku.
     */
    static void accumulatedErrorDemo() {
        section("7. Błąd skumulowany — sumowanie 0.1 wiele razy");

        double sum = 0.0;
        for (int i = 0; i < 10; i++) {
            sum += 0.1;
        }
        show("suma 0.1 dziesięć razy", sum);
        show("suma == 1.0 ?", sum == 1.0);
        show("Math.abs(suma - 1.0) < 1e-9 ?", Math.abs(sum - 1.0) < 1e-9);
        // WYNIK: suma 0.1 dziesięć razy → 0.9999999999999999
        // WYNIK: suma == 1.0 ? → false
        // WYNIK: Math.abs(suma - 1.0) < 1e-9 ? → true

        double sumMany = 0.0;
        for (int i = 0; i < 1_000_000; i++) {
            sumMany += 0.1;
        }
        show("suma 0.1 milion razy", sumMany);
        show("oczekiwane dokładnie", 100_000.0);
        show("różnica", Math.abs(sumMany - 100_000.0));
        // WYNIK: suma 0.1 milion razy → 100000.00000133288
        // WYNIK: oczekiwane dokładnie → 100000.0
        // WYNIK: różnica → 1.3328826753422618E-6

        // DOBRA PRAKTYKA: dla sum finansowych używaj BigDecimal albo long w groszach, nigdy sumowania
        //   double w pętli (t15_numbers/Numbers01BigDecimal, t15_numbers/Numbers05IntegerTricks sekcja 10).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   EPSILON:       nigdy a == b dla double — zawsze Math.abs(a - b) < epsilon
     *   NEWTON:        x_{n+1} = x_n - f(x_n)/f'(x_n)   (dla √a: (x_n + a/x_n)/2) — zbieżność SZYBKA
     *   BISEKCJA:      wymaga przeciwnych znaków na końcach przedziału; zbieżność WOLNIEJSZA, ale PEWNA
     *   TRAPEZY:       pole ≈ suma trapezów; dla funkcji wypukłej lekko PRZESZACOWUJE
     *   SIMPSON:       pole ≈ suma parabol; DOKŁADNE dla wielomianów stopnia ≤ 3
     *   MONTE CARLO:   losuj i licz udział trafień; błąd maleje jak 1/√n (100× więcej próbek = 10× dokładniej)
     *   BŁĄD SKUMULOWANY: każda operacja na double dodaje odrobinę błędu — więcej operacji = większy błąd
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego metody numeryczne (Newton, bisekcja) zatrzymują się, gdy różnica jest MNIEJSZA niż
     *      epsilon, zamiast czekać na wynik dokładnie zero?
     *   2. Co wypisze:  System.out.println(0.1 + 0.2 == 0.3);  ?
     *   3. ZNAJDŹ BŁĄD: ktoś zatrzymuje pętlę metody Newtona warunkiem {@code while (x != poprzedniX)} —
     *      dlaczego to ryzykowne (a czasem wręcz nieskończona pętla)?
     *   4. Dlaczego metoda Simpsona dla x² na [0,1] daje wynik PRAKTYCZNIE dokładny (1/3), a metoda
     *      trapezów dla tej samej liczby punktów — tylko przybliżony?
     *   5. Co wypisze (w przybliżeniu):  System.out.println(4.0 * 785 / 1000);  ? (symulacja Monte Carlo:
     *      1000 próbek, 785 trafień w koło)
     *   6. ZNAJDŹ BŁĄD: ktoś zwiększa liczbę próbek Monte Carlo 100 razy, spodziewając się wyniku 100 razy
     *      dokładniejszego. Dlaczego to fałszywe założenie?
     *   7. Dlaczego metoda bisekcji WYMAGA, żeby f(low) i f(high) miały przeciwne znaki na starcie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Uruchamia ćwiczenia: najpierw TWOJE rozwiązania (✘ dopóki nie uzupełnisz), potem wzorcowe (✔). */
    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: √4 metodą Newtona", 2.0, () -> exercise1(4, 10));
        Check.equal("ćw. 1b: √9 metodą Newtona", 3.0, () -> exercise1(9, 10));
        Check.equal("ćw. 1c: √2 metodą Newtona (zaokrąglone)", 1.4142, () -> exercise1(2, 10));
        Check.equal("ćw. 2a: pierwiastek x³-x-2 z [1,2]", 1.5214, () -> exercise2(1, 2, 1e-9));
        Check.equal("ćw. 2b: pierwiastek x³-x-2 z [1.5,1.6]", 1.5214, () -> exercise2(1.5, 1.6, 1e-9));
        Check.equal("ćw. 3a: trapezoid(n=4)", roundTo(trapezoid(4), 6), () -> exercise3(4));
        Check.equal("ćw. 3b: trapezoid(n=10)", roundTo(trapezoid(10), 6), () -> exercise3(10));
        Check.equal("ćw. 3c: trapezoid(n=100)", roundTo(trapezoid(100), 6), () -> exercise3(100));
        Check.equal("ćw. 4a: simpson(n=4) ≈ 1/3", 0.333333, () -> exercise4(4));
        Check.equal("ćw. 4b: simpson(n=10) ≈ 1/3", 0.333333, () -> exercise4(10));
        Check.equal("ćw. 5a: Monte Carlo π, 1000 próbek", roundTo(monteCarloPi(1_000, new Random(42)), 4), () -> exercise5(1_000, new Random(42)));
        Check.equal("ćw. 5b: Monte Carlo π, 100000 próbek", roundTo(monteCarloPi(100_000, new Random(42)), 4), () -> exercise5(100_000, new Random(42)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", 2.0, () -> solution1(4, 10));
        Check.equal("ćw. 1b (wzorzec)", 3.0, () -> solution1(9, 10));
        Check.equal("ćw. 1c (wzorzec)", 1.4142, () -> solution1(2, 10));
        Check.equal("ćw. 2a (wzorzec)", 1.5214, () -> solution2(1, 2, 1e-9));
        Check.equal("ćw. 2b (wzorzec)", 1.5214, () -> solution2(1.5, 1.6, 1e-9));
        Check.equal("ćw. 3a (wzorzec)", roundTo(trapezoid(4), 6), () -> solution3(4));
        Check.equal("ćw. 3b (wzorzec)", roundTo(trapezoid(10), 6), () -> solution3(10));
        Check.equal("ćw. 3c (wzorzec)", roundTo(trapezoid(100), 6), () -> solution3(100));
        Check.equal("ćw. 4a (wzorzec)", 0.333333, () -> solution4(4));
        Check.equal("ćw. 4b (wzorzec)", 0.333333, () -> solution4(10));
        Check.equal("ćw. 5a (wzorzec)", roundTo(monteCarloPi(1_000, new Random(42)), 4), () -> solution5(1_000, new Random(42)));
        Check.equal("ćw. 5b (wzorzec)", roundTo(monteCarloPi(100_000, new Random(42)), 4), () -> solution5(100_000, new Random(42)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 12 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz √a metodą Newtona (iterations kroków), zaokrąglając wynik do 4 miejsc
     * po przecinku (użyj roundTo) — bez wywoływania newtonSqrtStep z sekcji 2.
     * Podpowiedź: {@code x = (x + a/x) / 2}, startując np. od x = a; powtórz iterations razy.
     */
    static double exercise1(double a, int iterations) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 2 (łatwe): znajdź pierwiastek x³-x-2=0 w przedziale [low, high] metodą bisekcji,
     * zaokrąglając wynik do 4 miejsc po przecinku — bez wywoływania bisection z sekcji 3.
     * Podpowiedź: dopóki (high - low) > epsilon, licz środek, sprawdzaj znak cubic(środek) i zawężaj
     * przedział do połowy z przeciwnymi znakami na końcach.
     */
    static double exercise2(double low, double high, double epsilon) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 3 (średnie): policz całkę ∫x² dx na [0,1] metodą trapezów z n paskami, zaokrąglając do
     * 6 miejsc po przecinku — bez wywoływania trapezoid z sekcji 4.
     * Podpowiedź: h = 1/n; suma = (f(0)+f(1))/2 + Σ f(i·h) dla i=1..n-1; wynik = suma × h.
     */
    static double exercise3(int n) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 4 (medium-trudne): policz całkę ∫x² dx na [0,1] metodą Simpsona z n paskami (n parzyste),
     * zaokrąglając do 6 miejsc po przecinku — bez wywoływania simpson z sekcji 5. Sprawdź, że wynik jest
     * (niemal) dokładnie 1/3, niezależnie od n.
     * Podpowiedź: wagi 1, 4, 2, 4, ..., 4, 1; suma × h / 3.
     */
    static double exercise4(int n) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 5 (trudniejsze) — PRZEPISZ: poniższa funkcja tworzy WŁASNY generator losowy w środku,
     * więc każde wywołanie daje INNY wynik i nie da się jej przetestować deterministycznie:
     * <pre>{@code
     * static double piEstimateBad(int samples) {
     *     Random random = new Random();          // ZAWSZE nowe ziarno — brak determinizmu!
     *     int inside = 0;
     *     for (int i = 0; i < samples; i++) {
     *         double x = random.nextDouble() * 2 - 1;
     *         double y = random.nextDouble() * 2 - 1;
     *         if (x * x + y * y <= 1.0) inside++;
     *     }
     *     return 4.0 * inside / samples;
     * }
     * }</pre>
     * Przepisz ją tak, żeby PRZYJMOWAŁA generator Random jako parametr (ten sam pomysł co monteCarloPi
     * w sekcji 6) — dzięki temu wywołujący decyduje o ziarnie i wynik staje się powtarzalny/testowalny.
     * Zaokrąglij wynik do 4 miejsc po przecinku.
     */
    static double exercise5(int samples, Random random) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static double solution1(double a, int iterations) {
        double x = a;
        for (int i = 0; i < iterations; i++) {
            x = (x + a / x) / 2.0;
        }
        return roundTo(x, 4);
    }

    static double solution2(double low, double high, double epsilon) {
        double signAtLow = Math.signum(cubic(low));
        while (high - low > epsilon) {
            double mid = (low + high) / 2.0;
            double signAtMid = Math.signum(cubic(mid));
            if (signAtMid == signAtLow) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return roundTo((low + high) / 2.0, 4);
    }

    static double solution3(int n) {
        return roundTo(trapezoid(n), 6);
    }

    static double solution4(int n) {
        return roundTo(simpson(n), 6);
    }

    static double solution5(int samples, Random random) {
        return roundTo(monteCarloPi(samples, random), 4);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Obliczenia na double prawie nigdy nie trafiają w wartość DOKŁADNIE zero/równą celowi — błędy
     *      zaokrągleń sprawiają, że czekanie na "dokładnie 0" mogłoby się nie skończyć (albo skończyć
     *      przypadkiem po bardzo długim czasie). Epsilon daje jasne, osiągalne kryterium "wystarczająco blisko".
     *   2. "false" — 0.1 i 0.2 nie mają dokładnego zapisu binarnego, ich suma to coś blisko 0.3, ale nie
     *      dokładnie 0.3 (różnica pojawia się na dalekich miejscach po przecinku).
     *   3. Metoda Newtona dla liczb zmiennoprzecinkowych prawie nigdy nie da DWÓCH identycznych bitowo
     *      wyników z rzędu — zazwyczaj oscyluje wokół wyniku o pojedyncze jednostki na ostatnim bicie
     *      (ULP), więc warunek "!=" może nigdy nie być spełniony (pętla nieskończona) albo zatrzymać się
     *      przypadkowo zbyt wcześnie/późno. Właściwe kryterium to różnica mniejsza niż epsilon.
     *   4. Simpson przybliża krzywą PARABOLĄ (wielomianem stopnia 2), a x² SAMO jest parabolą — więc
     *      przybliżenie pokrywa się z funkcją dokładnie (różnica tylko z zaokrągleń double). Trapezy
     *      przybliżają ODCINKIEM (wielomianem stopnia 1), co dla krzywej x² zawsze zostawia lukę.
     *   5. "3.14" — 4.0 * 785 / 1000 = 3.14, blisko π (3.14159...), typowy wynik dla 1000 próbek Monte Carlo.
     *   6. Błąd metody Monte Carlo maleje proporcjonalnie do 1/√n, nie do 1/n. Żeby zmniejszyć błąd 100×,
     *      trzeba 100² = 10 000× więcej próbek — zwiększenie liczby próbek 100× daje tylko ok. 10× mniejszy błąd.
     *   7. Jeśli f(low) i f(high) mają TEN SAM znak, nie wiadomo, czy w ogóle jest tam pierwiastek (może
     *      ich wcale nie być w tym przedziale, albo może być ich parzyście wiele — funkcja "wraca" do tego
     *      samego znaku). Przeciwne znaki na końcach GWARANTUJĄ (dla funkcji ciągłej) co najmniej jedno
     *      przecięcie zera między nimi.
     */
    // </editor-fold>
}
