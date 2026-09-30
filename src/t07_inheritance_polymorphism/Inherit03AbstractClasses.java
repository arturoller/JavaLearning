package t07_inheritance_polymorphism;

import helpers.Check;

import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Klasy i metody abstrakcyjne — wspólny szkielet, różne szczegóły
 *        (abstract class = klasa abstrakcyjna; abstract method = metoda abstrakcyjna)
 *
 * W SKRÓCIE:
 *   Klasa abstrakcyjna to "niedokończony przepis" — ma pełne pola i metody KONKRETNE (z ciałem),
 *   ale też metody ABSTRAKCYJNE (bez ciała), które każda podklasa MUSI dopisać. Nie da się utworzyć
 *   obiektu klasy abstrakcyjnej wprost (new) — ma sens dopiero jako baza dla konkretnych podklas.
 *
 * ANALOGIA: formularz urzędowy z rubrykami "wypełnia urzędnik".
 *   Część formularza jest już wydrukowana i taka sama dla każdego (metody konkretne). Inne rubryki są
 *   puste i MUSZĄ zostać wypełnione, zanim formularz stanie się ważny (metody abstrakcyjne) — ale sposób
 *   ich wypełnienia zależy od urzędu (podklasy).
 *
 * JAK TO DZIAŁA:
 *   abstract class Shape {
 *       abstract double area();                    ← BEZ ciała — każda podklasa MUSI ją dopisać
 *       String describe() { return "Pole: " + area(); }   ← KONKRETNA, wspólna dla wszystkich kształtów
 *   }
 *   class Circle extends Shape { double area() { return Math.PI * r * r; } }
 *   new Shape();     // BŁĄD KOMPILACJI: Shape is abstract; cannot be instantiated
 *   new Circle(...); // OK — Circle dostarczyło WSZYSTKIE metody abstrakcyjne
 *
 * SŁÓWKA:
 *   abstract = abstrakcyjny (niedokończony, wymaga dopełnienia); concrete class = klasa konkretna
 *   (da się z niej robić obiekty); partial implementation = częściowa implementacja; template method =
 *   metoda szablonowa (szkielet algorytmu w klasie bazowej, szczegóły w podklasach).
 *
 * ZOBACZ TEŻ: t07_inheritance_polymorphism/Inherit02Override (reguły nadpisywania metod),
 *             t07_inheritance_polymorphism/Inherit04Interfaces (abstrakcyjna klasa kontra interfejs),
 *             t22_design_patterns/Patterns05TemplateMethod (metoda szablonowa — pełny wzorzec).
 * </pre>
 */
public class Inherit03AbstractClasses {

    // ---------------------------------------------------------------------------------------------
    // Klasy przykładowe jako statyczne klasy zagnieżdżone — lekcja ma być samodzielna (patrz Inherit01).
    // ---------------------------------------------------------------------------------------------

    /** Shape = kształt. Klasa abstrakcyjna: ma konstruktor, pole i metodę konkretną, ale area() jest abstrakcyjne. */
    abstract static class Shape {
        protected final String label;   // label = etykieta — wspólne dla wszystkich kształtów

        Shape(String label) {           // klasa abstrakcyjna MOŻE mieć konstruktor — wołają go podklasy przez super()
            this.label = label;
        }

        /** area = pole powierzchni. Każdy kształt liczy je inaczej — brak wspólnego wzoru. */
        abstract double area();

        /** perimeter = obwód. Też abstrakcyjna — z tego samego powodu co area(). */
        abstract double perimeter();

        /** describe = opisz. KONKRETNA: gotowa dla wszystkich podklas, korzysta z ich area()/perimeter(). */
        String describe() {
            return label + ": pole=" + round2(area()) + ", obwód=" + round2(perimeter());
        }

        /** Pomocnicza metoda konkretna — zaokrąglenie do 2 miejsc, wspólne dla wszystkich kształtów. */
        static double round2(double value) {
            return Math.round(value * 100.0) / 100.0;
        }
    }

    /** Circle is-a Shape. Dostarcza WSZYSTKIE metody abstrakcyjne — dlatego można z niej robić obiekty. */
    static class Circle extends Shape {
        private final double r;   // r = promień (radius)

        Circle(double r) {
            super("Koło");
            this.r = r;
        }

        @Override
        double area() {
            return Math.PI * r * r;
        }

        @Override
        double perimeter() {
            return 2 * Math.PI * r;
        }
    }

    /** Rectangle is-a Shape. Też konkretna — inne wzory niż Circle, ale ten sam "szkielet" describe(). */
    static class Rectangle extends Shape {
        private final double width;
        private final double height;

        Rectangle(double width, double height) {
            super("Prostokąt");
            this.width = width;
            this.height = height;
        }

        @Override
        double area() {
            return width * height;
        }

        @Override
        double perimeter() {
            return 2 * (width + height);
        }
    }

    public static void main(String[] args) {
        title("Inherit03 — klasy abstrakcyjne: wspólny szkielet, różne szczegóły");

        cannotInstantiate();    // cannot instantiate = nie można utworzyć instancji
        abstractConstructor();  // abstract constructor = konstruktor w klasie abstrakcyjnej
        sharedBehavior();       // shared behavior = wspólne zachowanie (describe())
        mustImplementAll();     // must implement all = trzeba dostarczyć WSZYSTKIE metody abstrakcyjne
        polymorphicLoop();      // polymorphic loop = pętla po List<Shape>
        vsInterface();          // vs interface = klasa abstrakcyjna kontra interfejs (ŚCIĄGA)
        templateMethodPreview(); // template method preview = zapowiedź wzorca "metoda szablonowa"
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. NIE MOŻNA UTWORZYĆ OBIEKTU KLASY ABSTRAKCYJNEJ
    // =================================================================================================

    /** 1. Shape opisuje WSPÓLNY kontrakt, ale sam nie wie, jak policzyć pole — dlatego jest abstrakcyjny. */
    static void cannotInstantiate() {
        section("1. Nie można utworzyć obiektu klasy abstrakcyjnej");

        // Shape s = new Shape("?");
        //   // BŁĄD KOMPILACJI: Shape is abstract; cannot be instantiated
        //   // Gdyby to było dozwolone, s.area() nie miałoby ŻADNEJ sensownej implementacji do wywołania.

        Circle c = new Circle(3.0);
        show("c.getClass().getSuperclass().getSimpleName()", c.getClass().getSuperclass().getSimpleName());
        show("Shape.class jest abstrakcyjny?", java.lang.reflect.Modifier.isAbstract(Shape.class.getModifiers()));
        show("Circle.class jest abstrakcyjny?", java.lang.reflect.Modifier.isAbstract(Circle.class.getModifiers()));
        // WYNIK: c.getClass().getSuperclass().getSimpleName() → Shape
        // WYNIK: Shape.class jest abstrakcyjny? → true
        // WYNIK: Circle.class jest abstrakcyjny? → false

        // DOBRA PRAKTYKA: klasa abstrakcyjna ma sens tylko jako BAZA. Jeśli okazuje się, że nikt nigdy
        //   nie tworzy jej podklas, prawdopodobnie w ogóle nie powinna być klasą (może wystarczy metoda statyczna).
    }

    // =================================================================================================
    // 2. KONSTRUKTOR W KLASIE ABSTRAKCYJNEJ
    // =================================================================================================

    /**
     * 2. Klasa abstrakcyjna MOŻE (i często powinna) mieć konstruktor — inicjalizuje wspólne pola
     * (tu: label). Konstruktor nigdy nie jest wołany przez "new Shape(...)" wprost, tylko przez
     * super(...) z konstruktora konkretnej podklasy (patrz Inherit01Basics — ten sam mechanizm).
     */
    static void abstractConstructor() {
        section("2. Konstruktor w klasie abstrakcyjnej — wołany przez super(...)");

        Circle c = new Circle(2.0);
        Rectangle r = new Rectangle(3.0, 4.0);
        show("c.label (ustawione przez konstruktor Shape)", c.label);
        show("r.label (ustawione przez konstruktor Shape)", r.label);
        // WYNIK: c.label (ustawione przez konstruktor Shape) → Koło
        // WYNIK: r.label (ustawione przez konstruktor Shape) → Prostokąt

        // DOBRA PRAKTYKA: pola wspólne dla wszystkich podklas (jak label) trzymaj i inicjalizuj w klasie
        //   abstrakcyjnej — unikasz powielania tego samego kodu w każdej podklasie.
    }

    // =================================================================================================
    // 3. WSPÓLNE ZACHOWANIE: describe() DZIAŁA DLA KAŻDEGO KSZTAŁTU
    // =================================================================================================

    /**
     * 3. describe() jest napisane RAZ w Shape, ale wewnątrz woła area()/perimeter() — metody, których
     * Shape samo nie ma. To działa dzięki polimorfizmowi: w czasie działania Java wybiera wersję area()
     * pasującą do RZECZYWISTEGO typu obiektu (Circle albo Rectangle).
     */
    static void sharedBehavior() {
        section("3. Wspólne zachowanie: describe() ten sam kod, różne wyniki");

        Circle c = new Circle(3.0);
        Rectangle r = new Rectangle(4.0, 5.0);
        show("c.describe()", c.describe());
        show("r.describe()", r.describe());
        // WYNIK: c.describe() → Koło: pole=28.27, obwód=18.85
        // WYNIK: r.describe() → Prostokąt: pole=20.0, obwód=18.0

        // DOBRA PRAKTYKA: to jest właśnie siła klas abstrakcyjnych — piszesz WSPÓLNĄ logikę raz (describe()),
        //   a każda podklasa dostarcza tylko to, co naprawdę się różni (area(), perimeter()).
    }

    // =================================================================================================
    // 4. TRZEBA DOSTARCZYĆ WSZYSTKIE METODY ABSTRAKCYJNE
    // =================================================================================================

    /**
     * 4. Jeśli podklasa nie zaimplementuje choć jednej metody abstrakcyjnej, sama też musi być
     * zadeklarowana jako abstract — inaczej to błąd kompilacji.
     */
    static void mustImplementAll() {
        section("4. Podklasa musi dostarczyć WSZYSTKIE metody abstrakcyjne");

        // class Triangle extends Shape {
        //     Triangle(double a, double b, double c) { super("Trójkąt"); ... }
        //     double area() { return ...; }
        //     // brak perimeter()!
        // }
        //   // BŁĄD KOMPILACJI: Triangle is not abstract and does not override abstract method
        //   // perimeter() in Shape — trzeba albo dopisać perimeter(), albo oznaczyć Triangle jako abstract.

        note("Jedyne dwa wyjścia dla podklasy: (1) zaimplementować WSZYSTKIE metody abstrakcyjne, albo "
                + "(2) samej zostać abstrakcyjną i przekazać obowiązek dalej.");
        // WYNIK: ℹ Jedyne dwa wyjścia dla podklasy: (1) zaimplementować WSZYSTKIE metody abstrakcyjne, albo (2) samej zostać abstrakcyjną i przekazać obowiązek dalej.

        // PUŁAPKA: łatwo zapomnieć o jednej z kilku metod abstrakcyjnych przy dużej klasie bazowej —
        //   na szczęście to zawsze BŁĄD KOMPILACJI, nie cichy błąd w czasie działania (w przeciwieństwie
        //   do np. interfejsu z metodą default, którą MOŻNA pominąć — patrz Inherit04Interfaces).
    }

    // =================================================================================================
    // 5. PĘTLA PO List<Shape> — POLIMORFICZNIE, BEZ if/instanceof
    // =================================================================================================

    /** 5. Pętla nie musi wiedzieć, czy dany element to Circle czy Rectangle — woła area() polimorficznie. */
    static void polymorphicLoop() {
        section("5. Pętla po List<Shape> — bez if/instanceof");

        List<Shape> shapes = List.of(new Circle(1.0), new Rectangle(2.0, 3.0), new Circle(2.0));
        double totalArea = 0.0;
        for (Shape s : shapes) {
            totalArea += s.area();   // nie wiemy (i nie musimy wiedzieć) jaki to dokładnie kształt
        }
        show("suma pól trzech kształtów", Shape.round2(totalArea));
        // WYNIK: suma pól trzech kształtów → 21.71

        // DOBRA PRAKTYKA: gdy dopiszesz nową podklasę Shape (np. Triangle), ta pętla NIE WYMAGA ŻADNEJ
        //   zmiany — to główna zaleta polimorfizmu nad ręcznym if (kształt instanceof Circle) ... else if ...
        //   (pełny obraz w Inherit05Polymorphism).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA: klasa abstrakcyjna kontra interfejs
     *   ┌──────────────────────────┬───────────────────────────┬───────────────────────────────┐
     *   │                          │ klasa abstrakcyjna         │ interfejs                       │
     *   ├──────────────────────────┼───────────────────────────┼───────────────────────────────┤
     *   │ dziedziczenie/implementy │ tylko JEDNA (extends)      │ WIELE naraz (implements)        │
     *   │ pola instancyjne         │ tak, dowolne                │ tylko stałe (public static final)│
     *   │ konstruktor              │ tak                          │ nie                              │
     *   │ metody konkretne         │ tak, zwykłe                 │ tak: default/static/private (8/9+)│
     *   │ kiedy używać             │ wspólny STAN + częściowa     │ wspólny KONTRAKT zachowania,     │
     *   │                          │ implementacja ("Shape MA     │ możliwy dla niepowiązanych klas  │
     *   │                          │ pole i licznik")             │ ("Comparable", "Runnable")       │
     *   └──────────────────────────┴───────────────────────────┴───────────────────────────────┘
     *   Więcej o interfejsach: Inherit04Interfaces.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego nie można napisać `new Shape("cokolwiek")`?
     *   2. Co wypisze:  System.out.println(new Circle(1.0) instanceof Shape);  ?
     *   3. ZNAJDŹ BŁĄD:
     *          abstract class Shape { abstract double area(); }
     *          class Square extends Shape { double side; }   // brakuje area()!
     *   4. Czy klasa abstrakcyjna może mieć konstruktor? Kto go wywołuje i kiedy?
     *   5. Co wypisze (przybliżenie do 2 miejsc):  System.out.println(new Rectangle(2, 3).describe());  ?
     *   6. Czym różni się metoda abstrakcyjna od zwykłej metody bez ciała w interfejsie (Java 8+)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // 6. KLASA ABSTRAKCYJNA KONTRA INTERFEJS (ŚCIĄGA WYŻEJ) + ZAPOWIEDŹ METODY SZABLONOWEJ
    // =================================================================================================

    static void vsInterface() {
        section("6. Klasa abstrakcyjna kontra interfejs — patrz tabela w ŚCIĄGA");

        note("Skrót: klasa abstrakcyjna daje WSPÓLNY STAN i częściową implementację (jedna klasa bazowa); "
                + "interfejs daje WSPÓLNY KONTRAKT, który mogą podpisać zupełnie niepowiązane klasy (wiele naraz).");
        // WYNIK: ℹ Skrót: klasa abstrakcyjna daje WSPÓLNY STAN i częściową implementację (jedna klasa bazowa); interfejs daje WSPÓLNY KONTRAKT, który mogą podpisać zupełnie niepowiązane klasy (wiele naraz).

        // DOBRA PRAKTYKA: gdy wahasz się między klasą abstrakcyjną a interfejsem, zadaj pytanie: "czy te
        //   klasy MUSZĄ dzielić wspólny stan/pola?" — jeśli tak, klasa abstrakcyjna. Jeśli chodzi tylko
        //   o wspólne zachowanie/kontrakt (i klasy i tak już dziedziczą po czymś innym) — interfejs.
    }

    /**
     * 7. Metoda szablonowa (template method) to WZORZEC PROJEKTOWY: klasa abstrakcyjna definiuje SZKIELET
     * algorytmu (kolejność kroków) w metodzie konkretnej, a podklasy dostarczają tylko brakujące kroki
     * jako metody abstrakcyjne. describe() z tej lekcji to już mini-przykład takiego szkieletu. Pełny
     * wzorzec (z kilkoma krokami i "hookami") pokazuje t22_design_patterns/Patterns05TemplateMethod.
     */
    static void templateMethodPreview() {
        section("7. Zapowiedź: metoda szablonowa (template method)");

        note("describe() to już mini-metoda szablonowa: ustala STAŁY szkielet (\"etykieta: pole=..., obwód=...\"), "
                + "a szczegóły (area(), perimeter()) uzupełnia każda podklasa po swojemu.");
        // WYNIK: ℹ describe() to już mini-metoda szablonowa: ustala STAŁY szkielet ("etykieta: pole=..., obwód=..."), a szczegóły (area(), perimeter()) uzupełnia każda podklasa po swojemu.

        // ZOBACZ TEŻ: t22_design_patterns/Patterns05TemplateMethod — pełny wzorzec z wieloma krokami.
    }

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");

        List<Shape> shapes = List.of(new Circle(2.0), new Rectangle(3.0, 4.0), new Circle(1.0));

        Check.equal("ćw. 1: liczba kształtów o polu > 10", 2, () -> countBigger(shapes, 10.0));
        Check.equal("ćw. 2: opisy wszystkich kształtów", List.of(
                        "Koło: pole=12.57, obwód=12.57",
                        "Prostokąt: pole=12.0, obwód=14.0",
                        "Koło: pole=3.14, obwód=6.28"),
                () -> describeAll(shapes));
        Check.equal("ćw. 3: kształt o największym polu", "Koło: pole=12.57, obwód=12.57", () -> largestByArea(shapes));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 2, () -> solution1(shapes, 10.0));
        Check.equal("ćw. 2 (wzorzec)", List.of(
                        "Koło: pole=12.57, obwód=12.57",
                        "Prostokąt: pole=12.0, obwód=14.0",
                        "Koło: pole=3.14, obwód=6.28"),
                () -> solution2(shapes));
        Check.equal("ćw. 3 (wzorzec)", "Koło: pole=12.57, obwód=12.57", () -> solution3(shapes));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz, ile kształtów z listy ma pole (area()) większe niż minArea.
     * Podpowiedź: pętla for-each + licznik, porównanie s.area() > minArea.
     */
    static int countBigger(List<Shape> shapes, double minArea) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /** ĆWICZENIE 2 (łatwe): zbuduj listę opisów (describe()) wszystkich kształtów, w kolejności z listy. */
    static List<String> describeAll(List<Shape> shapes) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć describe() kształtu o NAJWIĘKSZYM polu. Zakładamy niepustą listę.
     * Podpowiedź: pętla for-each, trzymaj bieżącego "rekordzistę" (typu Shape) w zmiennej lokalnej.
     */
    static String largestByArea(List<Shape> shapes) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(List<Shape> shapes, double minArea) {
        int count = 0;
        for (Shape s : shapes) {
            if (s.area() > minArea) {
                count++;
            }
        }
        return count;
    }

    static List<String> solution2(List<Shape> shapes) {
        List<String> result = new java.util.ArrayList<>();
        for (Shape s : shapes) {
            result.add(s.describe());
        }
        return List.copyOf(result);
    }

    static String solution3(List<Shape> shapes) {
        Shape largest = shapes.get(0);
        for (Shape s : shapes) {
            if (s.area() > largest.area()) {
                largest = s;
            }
        }
        return largest.describe();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo Shape jest abstrakcyjny — ma metody abstrakcyjne (area(), perimeter()) bez ciała. Gdyby
     *      dało się utworzyć obiekt Shape, wywołanie area() nie miałoby czego wykonać.
     *   2. "true" — Circle dziedziczy po Shape (extends), więc każdy Circle JEST też Shape.
     *   3. Square nie implementuje area() (odziedziczonej po Shape jako abstrakcyjnej), a sama nie jest
     *      oznaczona jako abstract — błąd kompilacji: "Square is not abstract and does not override
     *      abstract method area() in Shape". Trzeba dopisać area() albo oznaczyć Square jako abstract.
     *   4. Tak, może i często powinna — inicjalizuje wspólne pola (jak label). Wywołuje go WYŁĄCZNIE
     *      konstruktor konkretnej podklasy, przez super(...), nigdy "new Shape(...)" wprost.
     *   5. "Prostokąt: pole=6.0, obwód=10.0" — area = 2*3 = 6.0, perimeter = 2*(2+3) = 10.0.
     *   6. Metoda abstrakcyjna w klasie MUSI zostać zaimplementowana przez każdą konkretną podklasę —
     *      to błąd kompilacji, jeśli zapomnisz. Metoda default w interfejsie (Java 8+) już MA gotową
     *      implementację — podklasa MOŻE ją nadpisać, ale nie musi (patrz Inherit04Interfaces).
     */
    // </editor-fold>
}
