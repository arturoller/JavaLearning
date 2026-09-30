package t07_inheritance_polymorphism;

import helpers.Check;

import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Polimorfizm — typ zadeklarowany kontra rzeczywisty, upcasting i downcasting
 *        (polymorphism = polimorfizm; declared type = typ zadeklarowany; runtime type = typ w czasie działania)
 *
 * W SKRÓCIE:
 *   Zmienna ma typ ZADEKLAROWANY (co widać w kodzie) i obiekt, na który wskazuje, ma typ RZECZYWISTY
 *   (co naprawdę zostało utworzone przez new). Wywołanie metody NADPISANEJ wybiera wersję na podstawie
 *   typu RZECZYWISTEGO (dynamic dispatch) — to jest właśnie polimorfizm: "jeden kod, wiele zachowań".
 *
 * ANALOGIA: pilot uniwersalny.
 *   Pilot (zmienna typu Shape) ma te same przyciski niezależnie od tego, czy sterujesz telewizorem czy
 *   odtwarzaczem (rzeczywisty typ obiektu). Wciskasz "OK" (wołasz metodę) i KAŻDE urządzenie reaguje
 *   PO SWOJEMU — pilot nie musi wiedzieć, jakie to dokładnie urządzenie.
 *
 * JAK TO DZIAŁA:
 *   Shape s = new Circle(2.0);      ← upcasting: NIEJAWNY, zawsze bezpieczny (Circle JEST Shape)
 *   s.area();                       ← wybiera wersję Circle w czasie DZIAŁANIA (dynamic dispatch)
 *   if (s instanceof Circle c) {    ← downcasting: JAWNY, wymaga sprawdzenia (nie każdy Shape to Circle)
 *       ...
 *   }
 *
 * SŁÓWKA:
 *   declared type = typ zadeklarowany (co widać w kodzie zmiennej); runtime type = typ rzeczywisty
 *   (co utworzono przez new); upcasting = rzutowanie w górę (do typu bazowego, niejawne); downcasting =
 *   rzutowanie w dół (do typu pochodnego, jawne, może się nie udać); dynamic dispatch = wybór metody
 *   w czasie działania; ClassCastException = wyjątek złego rzutowania.
 *
 * ZOBACZ TEŻ: t07_inheritance_polymorphism/Inherit03AbstractClasses (Shape/Circle/Rectangle — ta sama hierarchia),
 *             t07_inheritance_polymorphism/Inherit02Override (mechanizm nadpisywania pod spodem),
 *             t10_exceptions/Exceptions01Basics (obsługa wyjątków, w tym ClassCastException).
 * </pre>
 */
public class Inherit05Polymorphism {

    // ---------------------------------------------------------------------------------------------
    // Klasy przykładowe jako statyczne klasy zagnieżdżone — lekcja ma być samodzielna (patrz Inherit01).
    // Ta sama hierarchia Shape/Circle/Rectangle co w Inherit03 — celowo skopiowana, bo lekcje kompilują
    // się osobno i nie mogą korzystać z klas z innych plików tego rozdziału.
    // ---------------------------------------------------------------------------------------------

    abstract static class Shape {
        protected final String label;

        Shape(String label) {
            this.label = label;
        }

        abstract double area();

        String describe() {
            return label + ": pole=" + round2(area());
        }

        static double round2(double value) {
            return Math.round(value * 100.0) / 100.0;
        }
    }

    static class Circle extends Shape {
        final double r;

        Circle(double r) {
            super("Koło");
            this.r = r;
        }

        @Override
        double area() {
            return Math.PI * r * r;
        }
    }

    static class Rectangle extends Shape {
        final double width;
        final double height;

        Rectangle(double width, double height) {
            super("Prostokąt");
            this.width = width;
            this.height = height;
        }

        @Override
        double area() {
            return width * height;
        }

        /** Metoda TYLKO w Rectangle — nie ma jej w Shape, więc dostęp do niej wymaga downcastingu. */
        boolean isSquare() {
            return Math.abs(width - height) < 1e-9;
        }
    }

    public static void main(String[] args) {
        title("Inherit05 — polimorfizm: typ zadeklarowany kontra rzeczywisty");

        declaredVsRuntime();    // declared vs runtime = typ zadeklarowany kontra rzeczywisty
        dynamicDispatch();      // dynamic dispatch = dynamiczny wybór metody
        upcasting();            // upcasting = rzutowanie w górę
        downcasting();          // downcasting = rzutowanie w dół
        classCastException();   // ClassCastException = wyjątek złego rzutowania
        overloadCompileTime();  // overload compile time = przeciążenie wybierane w czasie kompilacji
        instanceofChainVsOverride(); // PRZEPISZ: if/instanceof -> polimorfizm
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. TYP ZADEKLAROWANY KONTRA TYP RZECZYWISTY
    // =================================================================================================

    /** 1. Zmienna "s" jest zadeklarowana jako Shape, ale obiekt pod spodem to Circle — to dwie różne rzeczy. */
    static void declaredVsRuntime() {
        section("1. Typ zadeklarowany (zmiennej) kontra typ rzeczywisty (obiektu)");

        Shape s = new Circle(3.0);
        show("typ zadeklarowany zmiennej s (widać w kodzie)", "Shape");
        show("s.getClass().getSimpleName() (typ rzeczywisty obiektu)", s.getClass().getSimpleName());
        show("s instanceof Circle (obiekt NAPRAWDĘ jest Circle)", s instanceof Circle);
        // WYNIK: typ zadeklarowany zmiennej s (widać w kodzie) → Shape
        // WYNIK: s.getClass().getSimpleName() (typ rzeczywisty obiektu) → Circle
        // WYNIK: s instanceof Circle (obiekt NAPRAWDĘ jest Circle) → true

        // DOBRA PRAKTYKA: kod widzi tylko to, co POZWALA typ zadeklarowany (tu: Shape.area(), Shape.describe()) —
        //   dopóki nie zrobisz downcastingu, metody TYLKO z Circle (jak np. promień "r") są niedostępne wprost.
    }

    // =================================================================================================
    // 2. DYNAMIC DISPATCH: WYBÓR METODY W CZASIE DZIAŁANIA
    // =================================================================================================

    /** 2. area() jest nadpisane w Circle i Rectangle — s.area() zawsze wybiera wersję pasującą do OBIEKTU. */
    static void dynamicDispatch() {
        section("2. Dynamic dispatch: s.area() wybiera wersję w czasie działania");

        List<Shape> shapes = List.of(new Circle(1.0), new Rectangle(2.0, 3.0));
        for (Shape s : shapes) {
            show(s.getClass().getSimpleName() + ".describe()", s.describe());
        }
        // WYNIK: Circle.describe() → Koło: pole=3.14
        // WYNIK: Rectangle.describe() → Prostokąt: pole=6.0

        // DOBRA PRAKTYKA: pętla NIE WIE (i nie musi wiedzieć), jaki dokładnie kształt przetwarza — to
        //   właśnie polimorfizm: jeden kawałek kodu (ta pętla) obsługuje dowolną liczbę różnych podklas,
        //   także tych, które dopiero powstaną w przyszłości.
    }

    // =================================================================================================
    // 3. UPCASTING: NIEJAWNY I ZAWSZE BEZPIECZNY
    // =================================================================================================

    /** 3. "Circle JEST Shape" — przypisanie Circle do zmiennej typu Shape nie wymaga żadnego rzutowania. */
    static void upcasting() {
        section("3. Upcasting — niejawny, zawsze bezpieczny");

        Circle c = new Circle(2.0);
        Shape s = c;                 // upcasting: NIEJAWNY, kompilator wie, że to zawsze się uda
        show("s == c (to WCIĄŻ ten sam obiekt, inny tylko 'widok')", s == c);
        show("s.area() (dalej woła wersję Circle)", Shape.round2(s.area()));
        // WYNIK: s == c (to WCIĄŻ ten sam obiekt, inny tylko 'widok') → true
        // WYNIK: s.area() (dalej woła wersję Circle) → 12.57

        // DOBRA PRAKTYKA: upcasting nie zmienia obiektu — zmienia tylko to, PRZEZ JAKI "typ zmiennej"
        //   na niego patrzymy (czyli jakie metody widać wprost, bez rzutowania).
    }

    // =================================================================================================
    // 4. DOWNCASTING: JAWNY, MOŻE SIĘ NIE UDAĆ
    // =================================================================================================

    /**
     * 4. Żeby dostać się do metody TYLKO z Rectangle (isSquare()), trzeba zrobić downcasting. Od Javy 16
     * robimy to bezpiecznie: instanceof z WZORCEM (pattern matching) — sprawdzenie i rzutowanie w jednym.
     */
    static void downcasting() {
        section("4. Downcasting — jawny, z pattern matching instanceof (Java 16+)");

        Shape s = new Rectangle(4.0, 4.0);

        // PRZED Javą 16 trzeba było robić to w dwóch krokach:
        //   if (s instanceof Rectangle) {
        //       Rectangle r = (Rectangle) s;   // osobne, jawne rzutowanie — łatwo o literówkę/duplikat
        //       ...
        //   }
        // OD Javy 16 (instanceof z wzorcem) — sprawdzenie i rzutowanie w JEDNYM kroku:
        if (s instanceof Rectangle r) {
            show("s to Rectangle — r.isSquare()", r.isSquare());
        }
        // WYNIK: s to Rectangle — r.isSquare() → true

        // DOBRA PRAKTYKA: zawsze SPRAWDZAJ przed rzutowaniem (instanceof) — rzutowanie "na ślepo" bez
        //   sprawdzenia to prosta droga do ClassCastException (patrz sekcja 5).
    }

    // =================================================================================================
    // 5. ClassCastException — RZUTOWANIE BEZ SPRAWDZENIA
    // =================================================================================================

    /** 5. Rzutowanie Circle na Rectangle (dwie NIEZWIĄZANE ze sobą podklasy Shape) zawsze się nie uda. */
    static void classCastException() {
        section("5. PUŁAPKA: ClassCastException przy rzutowaniu bez sprawdzenia");

        Shape s = new Circle(1.0);
        expectThrows("(Rectangle) s — rzutowanie Circle na Rectangle bez sprawdzenia", () -> {
            Rectangle r = (Rectangle) s;   // BRAK instanceof przed rzutowaniem — ryzykowne
            r.isSquare();
        });
        // WYNIK: ✔ (Rectangle) s — rzutowanie Circle na Rectangle bez sprawdzenia → rzucono ClassCastException: class t07_inheritance_polymorphism.Inherit05Polymorphism$Circle cannot be cast to class t07_inheritance_polymorphism.Inherit05Polymorphism$Rectangle (t07_inheritance_polymorphism.Inherit05Polymorphism$Circle and t07_inheritance_polymorphism.Inherit05Polymorphism$Rectangle are in unnamed module of loader 'app')

        // PUŁAPKA: (Rectangle) s kompiluje się bez problemu (Rectangle i Circle mają wspólnego przodka
        //   Shape), ale w czasie DZIAŁANIA JVM sprawdza rzeczywisty typ obiektu i rzuca ClassCastException,
        //   bo obiekt pod s to NAPRAWDĘ Circle, a nie Rectangle.
        // DOBRA PRAKTYKA: instanceof z wzorcem (sekcja 4) eliminuje ten problem CAŁKOWICIE — rzutowanie
        //   dzieje się TYLKO wtedy, gdy sprawdzenie się powiodło.
    }

    // =================================================================================================
    // 6. QUIZ: PRZECIĄŻENIE (KOMPILACJA) KONTRA NADPISANIE (DZIAŁANIE)
    // =================================================================================================

    static void printKind(Shape s) {
        System.out.println("    printKind(Shape): " + s.getClass().getSimpleName());
    }

    static void printKind(Circle c) {
        System.out.println("    printKind(Circle): " + c.getClass().getSimpleName());
    }

    /**
     * 6. Przeciążenie (który printKind wywołać) wybiera kompilator na podstawie TYPU ZADEKLAROWANEGO
     * zmiennej — w przeciwieństwie do nadpisania (area()), które wybiera JVM na podstawie typu obiektu.
     */
    static void overloadCompileTime() {
        section("6. QUIZ: przeciążenie wybrane w czasie kompilacji, nadpisanie — w czasie działania");

        Circle c = new Circle(1.0);
        Shape s = c;                 // ten sam obiekt, inny typ zadeklarowany zmiennej
        printKind(c);                // typ zadeklarowany: Circle -> wybiera printKind(Circle)
        printKind(s);                // typ zadeklarowany: Shape  -> wybiera printKind(Shape) (mimo że obiekt to Circle!)
        // WYNIK:     printKind(Circle): Circle
        // WYNIK:     printKind(Shape): Circle

        note("Ten sam obiekt, dwa różne wywołania printKind — bo przeciążenie (overload) patrzy na typ "
                + "ZADEKLAROWANY zmiennej w czasie KOMPILACJI, a nie na typ obiektu w czasie działania.");
        // WYNIK: ℹ Ten sam obiekt, dwa różne wywołania printKind — bo przeciążenie (overload) patrzy na typ ZADEKLAROWANY zmiennej w czasie KOMPILACJI, a nie na typ obiektu w czasie działania.

        // PUŁAPKA: to częsta pomyłka — "skoro c i s to ten sam obiekt Circle, printKind(s) też powinno
        //   wybrać wersję Circle". NIE: nadpisanie (area()) jest polimorficzne (patrzy na obiekt),
        //   przeciążenie (printKind) NIE JEST — patrzy wyłącznie na zadeklarowany typ zmiennej.
    }

    // =================================================================================================
    // 7. PRZEPISZ: ŁAŃCUCH if/instanceof → POLIMORFIZM (NADPISANA METODA)
    // =================================================================================================

    /**
     * 7. PRZED: żeby dodać nowy kształt, trzeba pamiętać o dopisaniu kolejnego "else if" w KAŻDYM miejscu,
     * które robi coś w zależności od typu. PO: każdy kształt sam wie, jak policzyć swoje pole — kod
     * wołający nie musi w ogóle znać listy wszystkich podklas.
     * <pre>{@code
     * // PRZED: łańcuch if/instanceof — trzeba pamiętać o KAŻDYM typie przy KAŻDEJ takiej metodzie
     * static double areaOld(Shape s) {
     *     if (s instanceof Circle c) {
     *         return Math.PI * c.r * c.r;
     *     } else if (s instanceof Rectangle r) {
     *         return r.width * r.height;
     *     }
     *     throw new IllegalArgumentException("Nieznany kształt: " + s.getClass());   // łatwo zapomnieć!
     * }
     * }</pre>
     */
    static double areaOld(Shape s) {
        if (s instanceof Circle c) {
            return Math.PI * c.r * c.r;
        } else if (s instanceof Rectangle r) {
            return r.width * r.height;
        }
        throw new IllegalArgumentException("Nieznany kształt: " + s.getClass());
    }

    /** PO: jedna linijka, działa dla KAŻDEJ obecnej i przyszłej podklasy Shape — bez zmian w tym kodzie. */
    static double areaNew(Shape s) {
        return s.area();
    }

    static void instanceofChainVsOverride() {
        section("7. PRZEPISZ: łańcuch if/instanceof → polimorfizm (nadpisana metoda)");

        Shape r = new Rectangle(3.0, 5.0);
        show("areaOld(r) — PRZED (łańcuch instanceof)", Shape.round2(areaOld(r)));
        show("areaNew(r) — PO (polimorfizm, s.area())", Shape.round2(areaNew(r)));
        // WYNIK: areaOld(r) — PRZED (łańcuch instanceof) → 15.0
        // WYNIK: areaNew(r) — PO (polimorfizm, s.area()) → 15.0

        // DOBRA PRAKTYKA: gdy widzisz `if (x instanceof A) ... else if (x instanceof B) ...` rozgałęziające
        //   się po TYPIE, a wszystkie klasy A/B mają wspólnego przodka, zwykle lepiej przenieść tę logikę
        //   do NADPISANEJ metody w każdej klasie — kompilator sam upilnuje kompletności (patrz Inherit03,
        //   sekcja 4: brak implementacji metody abstrakcyjnej to błąd kompilacji, brak "case" w if/instanceof — nie).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • typ zadeklarowany (zmiennej) ≠ typ rzeczywisty (obiektu) — Shape s = new Circle(...) to jeden
     *     obiekt widziany przez "węższe okno" (tylko to, co oferuje Shape).
     *   • dynamic dispatch: nadpisana metoda (np. area()) zawsze wybiera wersję pasującą do OBIEKTU,
     *     niezależnie od typu zmiennej — to jest polimorfizm.
     *   • upcasting (w górę, do typu bazowego) jest niejawny i zawsze bezpieczny.
     *   • downcasting (w dół, do typu pochodnego) jest jawny i może się nie udać — używaj instanceof
     *     z wzorcem (Java 16+), nie rzutowania "na ślepo".
     *   • rzutowanie bez sprawdzenia → ClassCastException w czasie działania (kompilacja tego nie wyłapie,
     *     jeśli typy mają wspólnego przodka).
     *   • przeciążenie (overload) jest wybierane w czasie KOMPILACJI po typie ZADEKLAROWANYM — to NIE jest polimorfizm.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się typ zadeklarowany zmiennej od typu rzeczywistego obiektu?
     *   2. Co wypisze:  Shape s = new Rectangle(2, 5); System.out.println(s.describe());  ?
     *   3. ZNAJDŹ BŁĄD:  Shape s = new Circle(1.0); Rectangle r = (Rectangle) s; r.isSquare();
     *   4. Dlaczego upcasting nigdy się nie nie udaje, a downcasting czasem tak?
     *   5. Co wypisze (dwie linie, w tej kolejności):
     *          Circle c = new Circle(1.0); Shape s = c;
     *          printKind(c);
     *          printKind(s);
     *      (patrz przeciążone metody printKind(Shape) / printKind(Circle) z sekcji 6)
     *   6. Dlaczego lepiej zastąpić `if (x instanceof A) ... else if (x instanceof B) ...` nadpisaną metodą?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");

        List<Shape> shapes = List.of(new Circle(2.0), new Rectangle(3.0, 3.0), new Rectangle(2.0, 5.0));

        Check.equal("ćw. 1: suma pól wszystkich kształtów", 31.57, () -> Shape.round2(totalArea(shapes)));
        Check.equal("ćw. 2: liczba kwadratowych prostokątów (downcasting)", 1, () -> countSquares(shapes));
        Check.equal("ćw. 3: bezpieczne pole (Optional-owo bez Optional): pole Circle albo -1", 12.57,
                () -> Shape.round2(safeCircleArea(shapes.get(0))));
        Check.equal("ćw. 3b: safeCircleArea dla Rectangle → -1", -1.0, () -> safeCircleArea(shapes.get(1)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 31.57, () -> Shape.round2(solution1(shapes)));
        Check.equal("ćw. 2 (wzorzec)", 1, () -> solution2(shapes));
        Check.equal("ćw. 3a (wzorzec)", 12.57, () -> Shape.round2(solution3(shapes.get(0))));
        Check.equal("ćw. 3b (wzorzec)", -1.0, () -> solution3(shapes.get(1)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): zsumuj area() wszystkich kształtów z listy (polimorficznie, bez instanceof). */
    static double totalArea(List<Shape> shapes) {
        // TODO: twoje rozwiązanie
        return 0.0;
    }

    /**
     * ĆWICZENIE 2 (średnie): policz, ile obiektów na liście to Rectangle, który JEST kwadratem
     * (isSquare()). Podpowiedź: instanceof z wzorcem — `shape instanceof Rectangle r && r.isSquare()`.
     */
    static int countSquares(List<Shape> shapes) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zwróć pole obiektu, jeśli to Circle; w przeciwnym razie zwróć -1.0
     * (bez rzucania wyjątku!). Podpowiedź: instanceof z wzorcem, bez rzutowania "na ślepo".
     */
    static double safeCircleArea(Shape s) {
        // TODO: twoje rozwiązanie
        return 0.0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static double solution1(List<Shape> shapes) {
        double total = 0.0;
        for (Shape s : shapes) {
            total += s.area();
        }
        return total;
    }

    static int solution2(List<Shape> shapes) {
        int count = 0;
        for (Shape s : shapes) {
            if (s instanceof Rectangle r && r.isSquare()) {
                count++;
            }
        }
        return count;
    }

    static double solution3(Shape s) {
        if (s instanceof Circle c) {
            return c.area();
        }
        return -1.0;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Typ zadeklarowany to to, co widać w kodzie przy deklaracji zmiennej (decyduje, jakie metody
     *      widać wprost, bez rzutowania). Typ rzeczywisty to typ obiektu utworzonego przez "new" (decyduje,
     *      która wersja NADPISANEJ metody zostanie wywołana).
     *   2. "Prostokąt: pole=10.0" — describe() jest nadpisane pośrednio przez area() z Rectangle (2*5=10),
     *      mimo że zmienna s ma zadeklarowany typ Shape.
     *   3. Circle i Rectangle to dwie NIEZWIĄZANE ze sobą podklasy Shape (rodzeństwo, nie linia dziedziczenia)
     *      — rzutowanie (Rectangle) s na obiekcie, który NAPRAWDĘ jest Circle, rzuci ClassCastException
     *      w czasie działania. Trzeba sprawdzić `s instanceof Rectangle r` PRZED użyciem r.
     *   4. Upcasting zawsze się udaje, bo podklasa ZAWSZE ma wszystko, co ma klasa bazowa (is-a w jedną
     *      stronę jest gwarantowane). Downcasting zakłada coś WIĘCEJ, niż gwarantuje typ zadeklarowany —
     *      może się nie udać, jeśli obiekt naprawdę jest innej (siostrzanej) podklasy.
     *   5. "printKind(Circle): Circle" a potem "printKind(Shape): Circle" — to TEN SAM obiekt Circle za
     *      każdym razem (stąd getSimpleName() zawsze zwraca "Circle"), ale WYBÓR przeciążonej metody
     *      (Shape czy Circle w nazwie printKind) zależy od typu ZADEKLAROWANEGO zmiennej użytej w wywołaniu.
     *   6. Bo kompilator PILNUJE kompletności nadpisanych metod (Inherit03: brak implementacji metody
     *      abstrakcyjnej to błąd kompilacji), a łańcucha if/instanceof nikt nie upilnuje — łatwo zapomnieć
     *      dopisać nową gałąź przy nowej podklasie, i kod po cichu wpadnie w gałąź domyślną/wyjątek.
     */
    // </editor-fold>
}
