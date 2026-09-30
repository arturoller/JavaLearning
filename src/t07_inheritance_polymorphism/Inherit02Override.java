package t07_inheritance_polymorphism;

import helpers.Check;

import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Nadpisywanie metod — @Override, reguły, klasyczne pułapki
 *        (override = nadpisanie; overload = przeciążenie; hiding = ukrycie)
 *
 * W SKRÓCIE:
 *   Podklasa może dać metodzie odziedziczonej po klasie bazowej NOWE ciało — to nadpisanie (override).
 *   @Override to adnotacja, która każe kompilatorowi SPRAWDZIĆ, że naprawdę nadpisujesz istniejącą
 *   metodę (a nie np. tworzysz nową przez literówkę). Obowiązują ścisłe reguły: ta sama sygnatura,
 *   zwracany typ co najwyżej "węższy" (kowariancja), dostęp co najmniej taki sam, wyjątki co najwyżej węższe.
 *
 * ANALOGIA: aktor grający tę samą rolę w remake'u filmu.
 *   Scenariusz (sygnatura metody) zostaje ten sam, ale aktor (implementacja) może zagrać scenę inaczej.
 *   Nie może jednak nagle zmienić języka filmu (sygnatury) ani dodać scen, których scenariusz nie przewiduje.
 *
 * JAK TO DZIAŁA:
 *   class Vehicle { String describe() { return "Pojazd"; } }
 *   class Car extends Vehicle {
 *       @Override
 *       String describe() { return super.describe() + " (samochód)"; }   ← ta sama sygnatura + super.metoda()
 *   }
 *   Vehicle v = new Car(...);
 *   v.describe()  → wywołuje wersję Car (dynamic dispatch — wybór w czasie DZIAŁANIA, nie kompilacji)
 *
 * SŁÓWKA:
 *   override = nadpisanie (ta sama sygnatura, nowe ciało); overload = przeciążenie (inna sygnatura,
 *   inna metoda); hiding = ukrycie (dotyczy pól i metod statycznych — nie jest polimorficzne);
 *   covariant return type = kowariantny zwracany typ (węższy w podklasie); dynamic dispatch = wybór
 *   metody w czasie działania programu; static dispatch = wybór w czasie kompilacji.
 *
 * ZOBACZ TEŻ: t07_inheritance_polymorphism/Inherit01Basics (extends, konstruktory),
 *             t07_inheritance_polymorphism/Inherit05Polymorphism (pełny obraz dynamic dispatch),
 *             t06_oop_basics/Oop05ObjectMethods (equals/hashCode — poprawne nadpisanie Object.equals).
 * </pre>
 */
public class Inherit02Override {

    // ---------------------------------------------------------------------------------------------
    // Klasy przykładowe jako statyczne klasy zagnieżdżone — lekcja ma być samodzielna (patrz Inherit01).
    // ---------------------------------------------------------------------------------------------

    /** Vehicle = pojazd. Klasa bazowa z metodą final, metodą statyczną i zwykłą metodą do nadpisania. */
    static class Vehicle {
        protected final String name;

        Vehicle(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Vehicle[" + name + "]";
        }

        String describe() {
            return "Pojazd: " + name;
        }

        final void honk() {                 // final = nikt nie może nadpisać (patrz sekcja 5)
            System.out.println(name + " trąbi: Pi!");
        }

        static void serviceInfo() {         // statyczne metody NIE są polimorficzne (patrz sekcja 6)
            System.out.println("Serwis (Vehicle): co 15 000 km");
        }

        Vehicle copy() {                    // zwracany typ Vehicle — w Car zawężony do Car (kowariancja)
            return new Vehicle(name);
        }
    }

    /** Car is-a Vehicle. Nadpisuje describe()/toString()/copy(), a serviceInfo() tylko UKRYWA (hiding). */
    static class Car extends Vehicle {
        Car(String name) {
            super(name);
        }

        @Override
        public String toString() {
            return "Car[" + name + "]";
        }

        @Override
        String describe() {
            return super.describe() + " (samochód)";   // super.describe() = wersja z Vehicle, nie kopiujemy jej
        }

        static void serviceInfo() {         // BRAK @Override: metody statyczne nie da się nadpisać, tylko ukryć
            System.out.println("Serwis (Car): co 10 000 km");
        }

        @Override
        Car copy() {                        // kowariantny zwracany typ: Car jest PODTYPEM Vehicle — dozwolone
            return new Car(name);
        }
    }

    /** Money = pieniądze w groszach. Świadomie "zepsuty" przykład — patrz sekcja 4. */
    static class Money {
        private final int grosze;

        Money(int grosze) {
            this.grosze = grosze;
        }

        // PUŁAPKA: brak @Override to sygnał ostrzegawczy — ta metoda NIE nadpisuje Object.equals(Object),
        // bo ma INNY parametr (Money, nie Object). To PRZECIĄŻENIE (overload), zobacz sekcję 4.
        boolean equals(Money other) {
            return other != null && this.grosze == other.grosze;
        }
    }

    /** ParentBox/ChildBox — pola NIE są polimorficzne (patrz sekcja 7: cieniowanie/field hiding). */
    static class ParentBox {
        String label = "Parent";

        String getLabel() {                 // metoda JEST polimorficzna — dla kontrastu z polem label
            return label;
        }
    }

    static class ChildBox extends ParentBox {
        String label = "Child";             // PUŁAPKA: to UKRYWA (przesłania) ParentBox.label, nie nadpisuje go

        @Override
        String getLabel() {
            return label;                   // tu "label" odnosi się do ChildBox.label (najbliższe w zasięgu)
        }
    }

    /** ParentInit/ChildInit — klasyczna pułapka: nadpisywalna metoda wołana z konstruktora (sekcja 8). */
    static class ParentInit {
        ParentInit() {
            System.out.println("    ParentInit(): wołam init() -> " + init());
        }

        int init() {                        // metoda PRZEZNACZONA do nadpisania
            return 0;
        }
    }

    static class ChildInit extends ParentInit {
        private int value = 42;             // NIE final: final+literał to "stała" — kompilator wstawiłby 42
                                             // wszędzie, gdzie czytamy "value" (patrz PUŁAPKA niżej), i cała
                                             // demonstracja by zniknęła. Inicjalizator uruchamia się PO super()!

        @Override
        int init() {
            return value;                   // w chwili wywołania z ParentInit() to pole ma jeszcze wartość 0
        }

        int getValue() {
            return value;
        }
    }

    public static void main(String[] args) {
        title("Inherit02 — nadpisywanie metod: @Override i pułapki");

        overrideBasics();                  // override basics = podstawy nadpisywania
        overrideRules();                   // override rules = reguły nadpisywania
        superCall();                       // super call = wywołanie przez super
        overrideVsOverload();              // override vs overload = nadpisanie kontra przeciążenie
        finalMethods();                    // final methods = metody final
        staticHiding();                    // static hiding = ukrywanie metod statycznych
        fieldHiding();                     // field hiding = cieniowanie pól
        constructorOverridablePitfall();   // constructor overridable pitfall = pułapka konstruktora
        exercises();                       // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. @Override — PO CO JEST TA ADNOTACJA
    // =================================================================================================

    /** 1. println(obiekt) woła toString() polimorficznie — wybiera wersję Car, nie Vehicle. */
    static void overrideBasics() {
        section("1. @Override — po co jest ta adnotacja");

        Car c = new Car("Toyota Corolla");
        System.out.println(c);      // println woła c.toString() - polimorficznie wybiera wersję Car
        // WYNIK: Car[Toyota Corolla]

        // PUŁAPKA: literówka w nazwie metody, bez @Override.
        //   class Car extends Vehicle {
        //       public String toStrnig() { return "..."; }   // literówka! To zupełnie NOWA metoda
        //   }                                                 // (nie nadpisuje Vehicle.toString())
        //   Bez @Override kompilator MILCZY — kod się kompiluje, ale println nadal woła oryginalny
        //   toString() z Vehicle, bo toStrnig() to inna metoda o innej nazwie. Z @Override kompilator
        //   natychmiast zgłasza błąd: "method does not override a method from its superclass".
        // DOBRA PRAKTYKA: zawsze dodawaj @Override przy każdym zamierzonym nadpisaniu — to darmowa
        //   siatka bezpieczeństwa, która wyłapuje literówki i pomyłki w sygnaturze od razu przy kompilacji.
    }

    // =================================================================================================
    // 2. REGUŁY NADPISYWANIA
    // =================================================================================================

    /**
     * 2. Nadpisanie musi mieć TĘ SAMĄ sygnaturę (nazwa + parametry). Zwracany typ może być węższy
     * (kowariancja), dostęp może być tylko taki sam lub szerszy, a wyjątki kontrolowane (checked) —
     * takie same albo węższe (nigdy szersze).
     */
    static void overrideRules() {
        section("2. Reguły nadpisywania: sygnatura, zwracany typ, dostęp, wyjątki");

        Vehicle v = new Car("Skoda Octavia");
        Vehicle kopia = v.copy();
        show("kopia.getClass().getSimpleName()", kopia.getClass().getSimpleName());
        show("kopia instanceof Car", kopia instanceof Car);
        // WYNIK: kopia.getClass().getSimpleName() → Car
        // WYNIK: kopia instanceof Car → true

        note("Car.copy() deklaruje zwracany typ Car (nie Vehicle) — to KOWARIANTNY zwracany typ: "
                + "dozwolony, bo Car JEST Vehicle (węższy typ w podklasie).");
        // WYNIK: ℹ Car.copy() deklaruje zwracany typ Car (nie Vehicle) — to KOWARIANTNY zwracany typ: dozwolony, bo Car JEST Vehicle (węższy typ w podklasie).

        // PUŁAPKA: dostęp w nadpisaniu nie może być węższy.
        //   class Vehicle { protected void service() { ... } }
        //   class Car extends Vehicle {
        //       @Override
        //       private void service() { ... }   // BŁĄD: attempting to assign weaker access privileges
        //   }
        // PUŁAPKA: wyjątki kontrolowane w nadpisaniu nie mogą być szersze.
        //   class Vehicle { void start() throws java.io.IOException { ... } }
        //   class Car extends Vehicle {
        //       @Override
        //       void start() throws Exception { ... }   // BŁĄD: Exception jest SZERSZY niż IOException
        //   }                                            // (zawężenie do java.io.FileNotFoundException byłoby OK)
        // DOBRA PRAKTYKA: nadpisanie może być "łagodniejsze" (szerszy dostęp, węższy wyjątek, węższy
        //   zwracany typ), ale nigdy "surowsze" — inaczej kod wołający przez typ bazowy by się zepsuł.
    }

    // =================================================================================================
    // 3. super.metoda() — WYWOŁANIE WERSJI Z KLASY BAZOWEJ
    // =================================================================================================

    /** 3. super.describe() pozwala Car ROZSZERZYĆ opis z Vehicle, zamiast go kopiować od nowa. */
    static void superCall() {
        section("3. super.metoda() — wywołanie wersji z klasy bazowej");

        Car c = new Car("Fiat Panda");
        show("c.describe()", c.describe());
        // WYNIK: c.describe() → Pojazd: Fiat Panda (samochód)

        // DOBRA PRAKTYKA: super.metoda() pozwala ROZSZERZYĆ zachowanie klasy bazowej zamiast kopiować
        //   jej kod do podklasy — jedno miejsce prawdy, mniej duplikacji.
    }

    // =================================================================================================
    // 4. NADPISYWANIE KONTRA PRZECIĄŻANIE: PUŁAPKA equals(Foo)
    // =================================================================================================

    /**
     * 4. Money.equals(Money) NIE nadpisuje Object.equals(Object) — ma inny parametr, więc to
     * przeciążenie (overload). Java wybiera metodę do wywołania na podstawie DEKLAROWANEGO typu
     * argumentu w czasie KOMPILACJI, nie typu w czasie działania.
     */
    static void overrideVsOverload() {
        section("4. Nadpisywanie kontra przeciążanie: pułapka equals(Foo)");

        Money a = new Money(500);
        Money b = new Money(500);
        show("a.equals(b) — argument zadeklarowany jako Money (przeciążenie Money)", a.equals(b));

        Object bAsObject = b;
        show("a.equals(bAsObject) — argument zadeklarowany jako Object (Object.equals!)", a.equals(bAsObject));
        // WYNIK: a.equals(b) — argument zadeklarowany jako Money (przeciążenie Money) → true
        // WYNIK: a.equals(bAsObject) — argument zadeklarowany jako Object (Object.equals!) → false

        // PUŁAPKA: obie linie wołają "a.equals(...)" na TYM SAMYM obiekcie b, a mimo to dają różny wynik!
        //   equals(Money other) nie ma @Override (bo nie mogłoby go mieć — nie nadpisuje niczego). Gdyby
        //   ktoś dopisał @Override nad equals(Money other), kompilator OD RAZU zgłosiłby błąd: to nie jest
        //   nadpisanie Object.equals(Object) — czyli literówka/pomyłka w sygnaturze wyszłaby na jaw.
        // DOBRA PRAKTYKA: żeby porównywać obiekty niezależnie od zadeklarowanego typu zmiennej, trzeba
        //   POPRAWNIE nadpisać equals(Object) (z @Override) — patrz t06_oop_basics/Oop05ObjectMethods
        //   oraz ćwiczenie 3 tej lekcji.
    }

    // =================================================================================================
    // 5. METODY final — NIE DA SIĘ ICH NADPISAĆ
    // =================================================================================================

    /** 5. honk() jest final w Vehicle — każda podklasa (nawet Car) używa DOKŁADNIE tej samej wersji. */
    static void finalMethods() {
        section("5. Metody final — nie da się ich nadpisać");

        Car c = new Car("Opel Astra");
        c.honk();      // Car nie ma WŁASNEJ wersji honk() — używa tej z Vehicle, bo jest final
        // WYNIK: Opel Astra trąbi: Pi!

        // class Car extends Vehicle {
        //     @Override
        //     void honk() { ... }   // BŁĄD KOMPILACJI: honk() cannot override final method Vehicle.honk()
        // }

        // DOBRA PRAKTYKA: final na metodzie gwarantuje, że KAŻDY obiekt tej klasy i jej podklas zachowuje
        //   się identycznie — przydatne, gdy zachowanie metody jest częścią kontraktu, którego nie wolno łamać.
    }

    // =================================================================================================
    // 6. METODY STATYCZNE SĄ UKRYWANE, NIE NADPISYWANE
    // =================================================================================================

    /**
     * 6. serviceInfo() istnieje w Vehicle i osobno w Car — to DWIE NIEZALEŻNE metody statyczne (ukrycie,
     * hiding), nie jedna nadpisana. Wybór, którą wywołać, zależy WYŁĄCZNIE od nazwy klasy przy wywołaniu.
     */
    static void staticHiding() {
        section("6. Metody statyczne są UKRYWANE (hiding), nie nadpisywane");

        Vehicle.serviceInfo();      // wołanie przez nazwę klasy Vehicle -> zawsze wersja Vehicle
        Car.serviceInfo();          // wołanie przez nazwę klasy Car -> zawsze wersja Car
        // WYNIK: Serwis (Vehicle): co 15 000 km
        // WYNIK: Serwis (Car): co 10 000 km

        Vehicle v = new Car("Mazda 3");
        // v.serviceInfo();   // DZIAŁAŁOBY, ale -Xlint:all ostrzega [static]: "static method should be
        //                     // qualified by type name, Vehicle, rather than by an expression" — dlatego
        //                     // wołamy WYŁĄCZNIE przez nazwę klasy: Vehicle.serviceInfo() / Car.serviceInfo().
        show("v (zmienna typu Vehicle) faktycznie trzyma obiekt Car", v instanceof Car);
        // WYNIK: v (zmienna typu Vehicle) faktycznie trzyma obiekt Car → true

        note("Gdyby serviceInfo() dało się wywołać przez v, wybór wersji i tak zależałby od DEKLAROWANEGO "
                + "typu zmiennej v (Vehicle) w czasie KOMPILACJI, a nie od typu obiektu w czasie działania.");
        // WYNIK: ℹ Gdyby serviceInfo() dało się wywołać przez v, wybór wersji i tak zależałby od DEKLAROWANEGO typu zmiennej v (Vehicle) w czasie KOMPILACJI, a nie od typu obiektu w czasie działania.

        // PUŁAPKA: metody statyczne NIE korzystają z dynamic dispatch (polimorfizmu) — wybór metody
        //   dzieje się w czasie KOMPILACJI, na podstawie zadeklarowanego typu. Dlatego mówimy "ukrycie"
        //   (hiding), a nie "nadpisanie" (override). @Override nad statyczną metodą w ogóle się nie skompiluje.
    }

    // =================================================================================================
    // 7. POLA NIE SĄ POLIMORFICZNE — CIENIOWANIE (FIELD HIDING)
    // =================================================================================================

    /**
     * 7. ChildBox.label UKRYWA (przesłania) ParentBox.label (obiekt ma NAPRAWDĘ dwa pola o tej samej nazwie).
     * Dostęp do pola przez zmienną typu ParentBox zawsze widzi pole ParentBox — niezależnie od
     * rzeczywistego typu obiektu. Dostęp przez METODĘ jest polimorficzny, jak zwykle.
     */
    static void fieldHiding() {
        section("7. Pola NIE są polimorficzne — cieniowanie (field hiding)");

        ParentBox p = new ChildBox();
        show("p.label (dostęp do POLA — decyduje typ DEKLAROWANY zmiennej p)", p.label);
        show("p.getLabel() (dostęp przez METODĘ — decyduje RZECZYWISTY typ obiektu)", p.getLabel());
        // WYNIK: p.label (dostęp do POLA — decyduje typ DEKLAROWANY zmiennej p) → Parent
        // WYNIK: p.getLabel() (dostęp przez METODĘ — decyduje RZECZYWISTY typ obiektu) → Child

        // PUŁAPKA: ChildBox.label nie NADPISUJE ParentBox.label — UKRYWA je (field hiding = ukrywanie pola). Obiekt ma
        //   dwa oddzielne pola o tej samej nazwie: jedno "widoczne" przez zmienną typu ParentBox, drugie
        //   przez zmienną typu ChildBox. To, które pole widzisz, zależy WYŁĄCZNIE od typu ZMIENNEJ,
        //   przez którą sięgasz — nie od typu obiektu w pamięci (w przeciwieństwie do metod!).
        // DOBRA PRAKTYKA: unikaj deklarowania pola o tej samej nazwie w podklasie — to źródło trudnych do
        //   znalezienia błędów. Jeśli podklasa potrzebuje innej wartości, ustaw ją w konstruktorze zamiast
        //   deklarować nowe pole.
    }

    // =================================================================================================
    // 8. PUŁAPKA: WYWOŁANIE NADPISYWALNEJ METODY Z KONSTRUKTORA
    // =================================================================================================

    /**
     * 8. Klasyczna pułapka: konstruktor ParentInit() woła init() polimorficznie (dostaje wersję z
     * ChildInit), ale pole ChildInit.value JESZCZE nie jest zainicjalizowane — inicjalizatory pól
     * podklasy uruchamiają się dopiero PO zakończeniu konstruktora klasy bazowej.
     */
    static void constructorOverridablePitfall() {
        section("8. PUŁAPKA: wywołanie nadpisywalnej metody z konstruktora");

        note("Tworzymy ChildInit — konstruktor ParentInit() woła init(), ale pole 'value' z ChildInit "
                + "jeszcze nie jest ustawione (inicjalizator pola uruchamia się PO super()):");
        ChildInit ci = new ChildInit();
        show("po PEŁNYM zbudowaniu obiektu: ci.getValue()", ci.getValue());
        // WYNIK: ℹ Tworzymy ChildInit — konstruktor ParentInit() woła init(), ale pole 'value' z ChildInit jeszcze nie jest ustawione (inicjalizator pola uruchamia się PO super()):
        // WYNIK:     ParentInit(): wołam init() -> 0
        // WYNIK: po PEŁNYM zbudowaniu obiektu: ci.getValue() → 42

        // PUŁAPKA: init() jest wywołane POLIMORFICZNIE (dostajemy wersję z ChildInit), ale w chwili tego
        //   wywołania ChildInit.value ma jeszcze wartość DOMYŚLNĄ (0 dla int) — zostanie ustawione na 42
        //   dopiero PO zakończeniu konstruktora ParentInit(). Obiekt "jeszcze nie istnieje w całości".
        //   (Gdyby value było `final int value = 42;`, kompilator uznałby je za stałą — bo final + stały
        //   literał to tzw. "constant variable" — i wstawiłby 42 wszędzie, gdzie odczytujemy "value",
        //   jeszcze zanim program w ogóle ruszy. To inny mechanizm, więc dla czytelności demonstracji
        //   celowo NIE oznaczamy tego pola jako final.)
        // DOBRA PRAKTYKA: nie wołaj z konstruktora metody, którą podklasy mogą nadpisać (chyba że jest
        //   final albo private — wtedy nie ma polimorfizmu, więc pułapka znika). Jeśli musisz, jasno to
        //   udokumentuj albo przenieś taką logikę poza konstruktor (np. do osobnej metody init wywoływanej ręcznie).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @Override każe kompilatorowi sprawdzić, że metoda NAPRAWDĘ nadpisuje coś z klasy bazowej
     *     (wyłapuje literówki w nazwie/sygnaturze).
     *   • Reguły nadpisania: ta sama sygnatura; zwracany typ może być węższy (kowariancja); dostęp
     *     może być tylko szerszy lub taki sam; wyjątki kontrolowane — tylko takie same albo węższe.
     *   • super.metoda() woła wersję z klasy bazowej — pozwala ROZSZERZYĆ zachowanie zamiast je kopiować.
     *   • equals(Foo) bez @Override to PRZECIĄŻENIE, nie nadpisanie Object.equals(Object) — klasyczna pułapka.
     *   • Metody final nie da się nadpisać.
     *   • Metody statyczne są UKRYWANE (hiding), nie nadpisywane — wybór w czasie kompilacji, po typie zmiennej.
     *   • Pola NIE są polimorficzne — cieniowanie (hiding) pól też działa po typie zmiennej, nie obiektu.
     *   • Wołanie nadpisywalnej metody z konstruktora klasy bazowej może zobaczyć NIEZAINICJALIZOWANE pola podklasy.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się nadpisywanie (override) od przeciążania (overload)?
     *   2. Co wypisze:  Vehicle v = new Car("X"); System.out.println(v.copy().getClass().getSimpleName());  ?
     *   3. ZNAJDŹ BŁĄD (koncepcyjny): dlaczego `boolean equals(Money other)` bez @Override nie jest
     *      nadpisaniem Object.equals, mimo że nazywa się tak samo?
     *   4. Dlaczego nie można nadpisać metody final?
     *   5. Co wypisze (w tej kolejności):  Vehicle.serviceInfo(); Car.serviceInfo();  ?
     *   6. Czym różni się "ukrycie" (hiding) metody statycznej/pola od nadpisania (override) metody instancyjnej?
     *   7. ZNAJDŹ BŁĄD:  class Car extends Vehicle { @Override void start() throws Exception {...} }
     *      (wiedząc, że Vehicle.start() deklaruje `throws java.io.IOException`)
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");

        Vehicle rower = new Vehicle("Rower");
        Car corolla = new Car("Toyota Corolla");
        List<Vehicle> vehicles = List.of(rower, corolla);
        Money m500a = new Money(500);
        Money m500b = new Money(500);
        Money m700 = new Money(700);

        Check.equal("ćw. 1: opis Car przez zmienną typu Vehicle", "Pojazd: Toyota Corolla (samochód)",
                () -> describeVehicle(corolla));
        Check.equal("ćw. 2: suma długości nazw (Rower + Toyota Corolla)", 19, () -> totalNameLength(vehicles));
        Check.equal("ćw. 3a: fixedEquals dla tej samej kwoty (przez Object)", true, () -> fixedEquals(m500a, m500b));
        Check.equal("ćw. 3b: fixedEquals dla różnych kwot", false, () -> fixedEquals(m500a, m700));
        Check.equal("ćw. 4: raport pojazdów", "Pojazd: Rower, Pojazd: Toyota Corolla (samochód)",
                () -> vehicleReport(vehicles));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "Pojazd: Toyota Corolla (samochód)", () -> solution1(corolla));
        Check.equal("ćw. 2 (wzorzec)", 19, () -> solution2(vehicles));
        Check.equal("ćw. 3a (wzorzec)", true, () -> solution3(m500a, m500b));
        Check.equal("ćw. 3b (wzorzec)", false, () -> solution3(m500a, m700));
        Check.equal("ćw. 4 (wzorzec)", "Pojazd: Rower, Pojazd: Toyota Corolla (samochód)", () -> solution4(vehicles));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć v.describe() — parametr ma zadeklarowany typ Vehicle, ale gdy dostaniesz
     * obiekt Car, wywołanie i tak jest polimorficzne (dynamic dispatch).
     */
    static String describeVehicle(Vehicle v) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /** ĆWICZENIE 2 (łatwe): zsumuj długości pól name wszystkich pojazdów z listy (v.name.length()). */
    static int totalNameLength(List<Vehicle> vehicles) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ): sekcja 4 pokazała pułapkę — equals(Money other) to PRZECIĄŻENIE,
     * nie nadpisanie, więc porównanie "przez Object" go pomija:
     * <pre>{@code
     * // PRZED (pułapka): equals(Money other) NIE nadpisuje Object.equals(Object)
     * boolean equals(Money other) { return this.grosze == other.grosze; }
     * }</pre>
     * PO: napisz poprawną metodę fixedEquals(Object a, Object b), która porówna DWA dowolne obiekty pod
     * kątem "czy oba są Money o tej samej kwocie" — działa niezależnie od zadeklarowanego typu zmiennych.
     * Podpowiedź: instanceof z wzorcem (Java 16+): `a instanceof Money ma && b instanceof Money mb`.
     */
    static boolean fixedEquals(Object a, Object b) {
        // stub boolean: neutralny "return false" mógłby przypadkiem zdać test, więc zaczynamy od wyjątku
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): dla każdego pojazdu z listy zbuduj opis metodą describe() (polimorficznie
     * — Car doda "(samochód)", zwykły Vehicle nie). Połącz opisy przez ", ".
     * Podpowiedź: zbuduj listę Stringów pętlą for-each, potem String.join(", ", lista).
     */
    static String vehicleReport(List<Vehicle> vehicles) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(Vehicle v) {
        return v.describe();
    }

    static int solution2(List<Vehicle> vehicles) {
        int total = 0;
        for (Vehicle v : vehicles) {
            total += v.name.length();
        }
        return total;
    }

    static boolean solution3(Object a, Object b) {
        return a instanceof Money ma && b instanceof Money mb && ma.grosze == mb.grosze;
    }

    static String solution4(List<Vehicle> vehicles) {
        List<String> opisy = new java.util.ArrayList<>();
        for (Vehicle v : vehicles) {
            opisy.add(v.describe());
        }
        return String.join(", ", opisy);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Nadpisanie (override) ma DOKŁADNIE tę samą sygnaturę co metoda w klasie bazowej i podmienia
     *      jej implementację (dynamic dispatch). Przeciążenie (overload) ma INNĄ sygnaturę (inne/inna
     *      liczba parametrów) — to zupełnie inna metoda, wybierana w czasie kompilacji.
     *   2. "Car" — v.copy() jest polimorficzne (wersja z Car, bo v faktycznie trzyma obiekt Car) i zwraca
     *      obiekt typu Car (kowariantny zwracany typ), więc getSimpleName() da "Car".
     *   3. equals(Money other) ma parametr typu Money, a Object.equals ma parametr typu Object — to INNA
     *      sygnatura, więc equals(Money other) jest przeciążeniem, nie nadpisaniem. Dodanie @Override nad
     *      tą metodą od razu ujawniłoby błąd kompilacji "method does not override a method from its superclass".
     *   4. Bo final na metodzie to jawna deklaracja: "to zachowanie jest częścią kontraktu klasy i nikt —
     *      żadna podklasa — nie może go zmienić". Kompilator to wymusza, żeby nikt tego nie złamał przez pomyłkę.
     *   5. "Serwis (Vehicle): co 15 000 km" a potem "Serwis (Car): co 10 000 km" — to dwie NIEZALEŻNE
     *      metody statyczne (ukrycie), wybierane po nazwie klasy użytej w wywołaniu, bez polimorfizmu.
     *   6. Nadpisanie metody instancyjnej jest polimorficzne: wybór wersji zależy od RZECZYWISTEGO typu
     *      obiektu w czasie działania. Ukrycie (pól i metod statycznych) jest rozstrzygane w czasie
     *      KOMPILACJI, na podstawie zadeklarowanego typu zmiennej — nie ma tu "wyboru w locie".
     *   7. Exception jest SZERSZYM wyjątkiem kontrolowanym niż IOException (IOException dziedziczy po
     *      Exception, nie odwrotnie) — nadpisująca metoda może deklarować tylko taki sam typ wyjątku albo
     *      węższy (np. java.io.FileNotFoundException), nigdy szerszy. Błąd: "overridden method does not
     *      throw Exception".
     */
    // </editor-fold>
}
