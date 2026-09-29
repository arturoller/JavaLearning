package t06_oop_basics;

import helpers.Check;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Klasy zagnieżdżone — static nested, inner, lokalne i anonimowe
 *        (nested = zagnieżdżona, inner = wewnętrzna, local = lokalna, anonymous = anonimowa)
 *
 * W SKRÓCIE:
 *   Klasę można zadeklarować WEWNĄTRZ innej klasy, a nawet wewnątrz metody. Robimy to, gdy klasa
 *   pomocnicza ma sens tylko razem z klasą zewnętrzną. Są cztery rodzaje, różnią się tym, co „widzą”
 *   i jak się je tworzy. Domyślny wybór: static nested.
 *
 * ANALOGIA:
 *   Firma (klasa zewnętrzna). static nested = firma podwykonawcza pod tym samym adresem: działa
 *   samodzielnie. inner = pracownik: zawsze należy do KONKRETNEJ firmy i ma dostęp do jej szaf.
 *   lokalna = zespół powołany na jedno spotkanie. anonimowa = jednorazowy zastępca bez nazwiska.
 *
 * JAK TO DZIAŁA:
 *   class Car {
 *       static class Engine { }     ← static nested: NIE potrzebuje obiektu Car     new Car.Engine()
 *       class Dashboard { }         ← inner: należy do obiektu Car (Car.this)       car.new Dashboard()
 *       void drive() {
 *           class Log { }           ← lokalna: istnieje tylko w tej metodzie        new Log()
 *           Runnable r = new Runnable() { ... };   ← anonimowa: klasa + obiekt naraz
 *       }
 *   }
 *   Wszystkie widzą składowe private klasy zewnętrznej (i odwrotnie).
 *
 * SŁÓWKA:
 *   nested = zagnieżdżony; inner = wewnętrzny; outer = zewnętrzny; enclosing instance = obiekt
 *   otaczający; local = lokalny; anonymous = anonimowy; effectively final = efektywnie finalny
 *   (nigdy nie zmieniany); comparator = porównywacz; builder = budowniczy
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop04Static (co znaczy static),
 *             t07_inheritance_polymorphism/Inherit04Interfaces (interfejsy, które implementują klasy anonimowe),
 *             t13_lambdas/Lambda01FromAnonymousToLambda (od klasy anonimowej do lambdy),
 *             t22_design_patterns/Patterns02Builder (Builder jako static nested class)
 * </pre>
 */
public class Oop07NestedClasses {

    public static void main(String[] args) {
        title("Oop07 — klasy zagnieżdżone");

        staticNested();             // static nested = zagnieżdżona statyczna
        innerClass();               // inner class = klasa wewnętrzna
        outerThis();                // outer this = Outer.this (obiekt zewnętrzny)
        localClass();               // local class = klasa lokalna
        anonymousClass();           // anonymous class = klasa anonimowa
        anonymousComparator();      // anonymous comparator = anonimowy Comparator
        hiddenReferencePitfall();   // hidden reference pitfall = pułapka ukrytej referencji
        whenToUseWhich();           // when to use which = kiedy której używać
        exercises();                // exercises = ćwiczenia
    }

    // Uwaga: CAŁA ta lekcja to klasy zagnieżdżone w klasie Oop07NestedClasses — w innych lekcjach
    // robimy tak tylko po to, żeby mieć jeden plik; tu to jest właśnie temat.

    // =================================================================================================
    // 1. STATIC NESTED CLASS — SAMODZIELNA KLASA „POD ADRESEM” INNEJ
    // =================================================================================================

    /** Samochód z zagnieżdżonym silnikiem (static) i deską rozdzielczą (inner). */
    static class Car {                          // Car = samochód
        private static final int MAX_HP = 500;  // private static — widzą je też klasy zagnieżdżone
        private final String model;
        private int speed;                      // speed = prędkość

        Car(String model) {
            this.model = model;
        }

        void accelerate(int delta) {            // accelerate = przyspiesz; delta = przyrost
            speed += delta;
        }

        /** static nested: nie ma obiektu Car, więc nie widzi model ani speed. */
        static class Engine {                   // Engine = silnik
            private final int horsePower;       // horse power = konie mechaniczne

            Engine(int horsePower) {
                if (horsePower > MAX_HP) {      // pole STATIC klasy zewnętrznej — OK, nawet private
                    throw new IllegalArgumentException("za mocny silnik: " + horsePower);
                }
                this.horsePower = horsePower;
            }

            String describe() {                 // describe = opisz
                return horsePower + " KM (max " + MAX_HP + ")";
                // return model;  → błąd kompilacji: non-static variable model cannot be referenced
                //                  from a static context (który samochód? — nie wiadomo)
            }
        }

        /** inner: każdy obiekt Dashboard należy do konkretnego samochodu. */
        class Dashboard {                       // Dashboard = deska rozdzielcza
            String display() {                  // display = wyświetl
                return model + ": " + speed + " km/h";   // widzi pola SWOJEGO samochodu
            }
        }
    }

    /**
     * 1. Klasa zagnieżdżona statyczna to zwykła klasa, tylko „zameldowana” w innej. Tworzymy ją przez
     * {@code new Car.Engine(...)} — bez żadnego samochodu. Widzi składowe static klasy zewnętrznej.
     */
    static void staticNested() {
        section("1. static nested — samodzielna klasa w klasie");

        Car.Engine engine = new Car.Engine(150);        // pełna nazwa: Zewnętrzna.Zagnieżdżona
        show("silnik", engine.describe());
        // WYNIK: silnik → 150 KM (max 500)
        show("prywatne pole horsePower czytane tutaj", engine.horsePower);
        // WYNIK: prywatne pole horsePower czytane tutaj → 150
        note("private działa na poziomie całej klasy najwyższego poziomu — tu widać wszystko w Oop07NestedClasses");
        // WYNIK: ℹ private działa na poziomie całej klasy najwyższego poziomu — tu widać wszystko w Oop07NestedClasses

        expectThrows("new Car.Engine(900)", () -> new Car.Engine(900));
        // WYNIK: ✔ new Car.Engine(900) → rzucono IllegalArgumentException: za mocny silnik: 900

        // DOBRA PRAKTYKA: static nested to DOMYŚLNY wybór dla klasy pomocniczej (Builder, węzeł listy,
        // wpis mapy — np. Map.Entry z JDK). Przyjrzyj się nazwie: Map.Entry = klasa Entry w Map.
    }

    // =================================================================================================
    // 2. INNER CLASS — KLASA, KTÓRA NALEŻY DO OBIEKTU
    // =================================================================================================

    /**
     * 2. Klasa wewnętrzna (bez static) ma ukrytą referencję do obiektu zewnętrznego — dlatego widzi jego
     * pola (także private) i zawsze aktualny stan. Z zewnątrz tworzymy ją przez {@code car.new Dashboard()}.
     */
    static void innerClass() {
        section("2. inner class — należy do konkretnego obiektu");

        Car fiat = new Car("Fiat");
        fiat.accelerate(50);
        Car.Dashboard dash = fiat.new Dashboard();      // składnia: obiektZewnętrzny.new Wewnętrzna()
        show("deska Fiata", dash.display());
        // WYNIK: deska Fiata → Fiat: 50 km/h

        fiat.accelerate(30);                            // zmieniamy stan AUTA, nie deski
        show("ta sama deska po przyspieszeniu", dash.display());
        // WYNIK: ta sama deska po przyspieszeniu → Fiat: 80 km/h

        Car opel = new Car("Opel");
        show("deska Opla", opel.new Dashboard().display());
        // WYNIK: deska Opla → Opel: 0 km/h

        //   dash ──▶ Dashboard{ [ukryte] Car.this ──▶ Car{ model="Fiat", speed=80 } }
        //
        // PUŁAPKA: bez obiektu zewnętrznego nie ma deski:
        //   Car.Dashboard d = new Car.Dashboard();   → błąd kompilacji: an enclosing instance that
        //                                              contains Car.Dashboard is required
        // DOBRA PRAKTYKA: używaj inner, gdy obiekt pomocniczy MUSI działać na danych konkretnego
        // obiektu zewnętrznego (klasyczny przykład: iterator po kolekcji — t12).
    }

    // =================================================================================================
    // 3. Outer.this — GDY NAZWY SIĘ ZASŁANIAJĄ
    // =================================================================================================

    /** Dom z pokojami; pole name jest i w domu, i w pokoju. */
    static class House {                        // House = dom
        private final String name;

        House(String name) {
            this.name = name;
        }

        Room room(String roomName) {            // wewnątrz House: new Room(...) znaczy this.new Room(...)
            return new Room(roomName);
        }

        class Room {                            // Room = pokój (inner)
            private final String name;          // to samo imię co House.name — ZASŁANIA je

            Room(String name) {
                this.name = name;               // this = obiekt Room
            }

            String address() {                  // address = adres
                return name + " w domu „" + House.this.name + "”";   // House.this = obiekt zewnętrzny
            }
        }
    }

    /**
     * 3. W klasie wewnętrznej {@code this} to obiekt wewnętrzny, a {@code Zewnętrzna.this} — obiekt
     * zewnętrzny. Przydaje się, gdy pola mają te same nazwy.
     */
    static void outerThis() {
        section("3. Outer.this — obiekt zewnętrzny");

        House house = new House("Pod Lipą");
        House.Room kitchen = house.room("kuchnia");     // kitchen = kuchnia
        show("adres pokoju", kitchen.address());
        // WYNIK: adres pokoju → kuchnia w domu „Pod Lipą”
        show("pokój z house.new Room(...)", house.new Room("salon").address());
        // WYNIK: pokój z house.new Room(...) → salon w domu „Pod Lipą”

        // DOBRA PRAKTYKA: zamiast zasłaniać nazwy, nadaj polom różne imiona (roomName, houseName).
        // Outer.this to „wyjście awaryjne”, gdy zasłonięcie jest nie do uniknięcia.
    }

    // =================================================================================================
    // 4. KLASA LOKALNA — KLASA WEWNĄTRZ METODY
    // =================================================================================================

    /**
     * 4. Klasa lokalna jest widoczna tylko w metodzie, w której ją zadeklarowano. Widzi zmienne lokalne
     * tej metody, ale tylko „efektywnie finalne” — takie, których nikt nie zmienia po przypisaniu.
     */
    static void localClass() {
        section("4. Klasa lokalna — w środku metody");

        int minLength = 3;                              // efektywnie finalna: nigdy jej nie zmieniamy

        class LengthChecker {                           // LengthChecker = sprawdzacz długości (klasa LOKALNA)
            boolean ok(String text) {
                return text.length() >= minLength;      // używa zmiennej lokalnej metody
            }
        }

        LengthChecker checker = new LengthChecker();
        show("ok(\"Al\")", checker.ok("Al"));
        // WYNIK: ok("Al") → false
        show("ok(\"Ala\")", checker.ok("Ala"));
        // WYNIK: ok("Ala") → true
        show("nazwa klasy nadana przez kompilator", checker.getClass().getName());
        // WYNIK: nazwa klasy nadana przez kompilator → t06_oop_basics.Oop07NestedClasses$1LengthChecker

        // PUŁAPKA: gdybyśmy gdziekolwiek zmienili minLength (np. minLength = 5;), dostaniemy
        //   błąd kompilacji: local variables referenced from an inner class must be final or effectively final
        // Dlaczego? Klasa lokalna dostaje KOPIĘ wartości — gdyby zmienna mogła się zmieniać, kopia
        // i oryginał rozjechałyby się. Java zabrania tego od razu.
        //
        // Od Java 16 wewnątrz metody można też deklarować lokalne rekordy, enumy i interfejsy (Java 16+).
        // DOBRA PRAKTYKA: klasy lokalne są rzadkie — używaj ich, gdy pomocnik ma sens tylko w jednej metodzie.
    }

    // =================================================================================================
    // 5. KLASA ANONIMOWA — IMPLEMENTACJA „NA MIEJSCU”
    // =================================================================================================

    /** Prosty interfejs: umowa „umiem kogoś powitać” (interfejsy dokładnie: t07). */
    interface Greeter {                         // Greeter = witacz; interface = interfejs (umowa)
        String greet(String name);              // greet = powitaj
    }

    /**
     * 5. Klasa anonimowa to klasa bez nazwy, zadeklarowana i użyta w jednym wyrażeniu
     * {@code new Interfejs() { ... }}. Powstaje od razu JEDEN obiekt. Idealna na jednorazowe implementacje.
     */
    static void anonymousClass() {
        section("5. Klasa anonimowa — implementacja na miejscu");

        Greeter polite = new Greeter() {                // polite = uprzejmy; „new Interfejs() { ciało }”
            @Override
            public String greet(String name) {
                return "Dzień dobry, " + name + "!";
            }
        };
        show("polite.greet(\"Ola\")", polite.greet("Ola"));
        // WYNIK: polite.greet("Ola") → Dzień dobry, Ola!
        show("nazwa klasy anonimowej", polite.getClass().getName());
        // WYNIK: nazwa klasy anonimowej → t06_oop_basics.Oop07NestedClasses$1
        show("isAnonymousClass()", polite.getClass().isAnonymousClass());   // isAnonymousClass = czy anonimowa
        // WYNIK: isAnonymousClass() → true

        Greeter counting = new Greeter() {              // counting = liczący
            private int calls;                          // klasa anonimowa może mieć WŁASNE pola (stan)

            @Override
            public String greet(String name) {
                calls++;
                return "#" + calls + " Hej, " + name;
            }
        };
        counting.greet("Ala");
        show("drugie wywołanie", counting.greet("Ola"));
        // WYNIK: drugie wywołanie → #2 Hej, Ola

        // PUŁAPKA: w klasie anonimowej nie napiszesz konstruktora (nie ma nazwy!) — dane bierze
        // z efektywnie finalnych zmiennych lokalnych albo z pól z inicjalizatorem (jak calls).
    }

    // =================================================================================================
    // 6. ANONIMOWY Comparator — I ZAPOWIEDŹ LAMBDY
    // =================================================================================================

    /**
     * 6. Najczęstsze klasyczne użycie klasy anonimowej: {@code Comparator} przekazany do sortowania.
     * Od Java 8 ten sam efekt daje krótsza lambda — ale pod spodem idea jest ta sama.
     */
    static void anonymousComparator() {
        section("6. Anonimowy Comparator i zapowiedź lambdy");

        String[] fruits = {"kiwi", "banan", "fig", "jabłko"};   // fruits = owoce
        Arrays.sort(fruits, new Comparator<String>() {          // Comparator = porównywacz (t12_collections/Collections07ComparableComparator)
            @Override
            public int compare(String a, String b) {            // compare = porównaj: wynik <0, 0 albo >0
                return Integer.compare(a.length(), b.length()); // krótsze słowa pierwsze
            }
        });
        show("posortowane po długości", fruits);
        // WYNIK: posortowane po długości → [fig, kiwi, banan, jabłko]

        // To samo lambdą (t13_lambdas/Lambda01FromAnonymousToLambda):
        //   Arrays.sort(fruits, (a, b) -> Integer.compare(a.length(), b.length()));
        Greeter casual = name -> "Cześć, " + name + "!";        // casual = swobodny; lambda = skrót klasy anonimowej
        show("lambda casual.greet(\"Ola\")", casual.greet("Ola"));
        // WYNIK: lambda casual.greet("Ola") → Cześć, Ola!

        // DOBRA PRAKTYKA: interfejs z JEDNĄ metodą → dziś zwykle lambda. Klasa anonimowa zostaje, gdy
        // potrzebujesz pola (stanu), kilku metod albo rozszerzasz klasę, a nie interfejs.
    }

    // =================================================================================================
    // 7. PUŁAPKA: UKRYTA REFERENCJA DO OBIEKTU ZEWNĘTRZNEGO
    // =================================================================================================

    /** Raport z „ciężkimi” danymi; oddaje małe obiekty: inner Summary i static nested Stamp. */
    static class Report {                       // Report = raport
        private final int[] bigData = new int[100_000];   // big data = duże dane (tu tylko dla ilustracji)
        private final String title;

        Report(String title) {
            this.title = title;
        }

        Summary summary() {
            return new Summary();
        }

        Stamp stamp() {
            return new Stamp("Pieczątka: " + title + " (" + bigData.length + " liczb)");
        }

        class Summary {                         // summary = podsumowanie; inner → trzyma Report.this
            String text() {
                return "Podsumowanie: " + title;
            }
        }

        static class Stamp {                    // stamp = pieczątka; static nested → NIE trzyma Report
            private final String text;

            Stamp(String text) {
                this.text = text;
            }

            String text() {
                return text;
            }
        }
    }

    /** Liczy pola dodane przez kompilator (np. ukryte this$0 w klasie wewnętrznej). */
    static int hiddenFields(Class<?> type) {    // hidden fields = ukryte pola
        int count = 0;
        for (Field field : type.getDeclaredFields()) {   // getDeclaredFields = pobierz zadeklarowane pola (refleksja, t19)
            if (field.isSynthetic()) {          // isSynthetic = „sztuczne” — dopisane przez kompilator
                count++;
            }
        }
        return count;
    }

    /**
     * 7. Obiekt klasy wewnętrznej trzyma ukryte pole z referencją do obiektu zewnętrznego. Dopóki żyje
     * mały Summary, garbage collector nie może usunąć dużego Report — to typowy „wyciek pamięci”.
     */
    static void hiddenReferencePitfall() {
        section("7. Pułapka: ukryta referencja do obiektu zewnętrznego");

        Report report = new Report("Sprzedaż 2026");
        Report.Summary summary = report.summary();
        Report.Stamp stamp = report.stamp();
        show("summary.text()", summary.text());
        // WYNIK: summary.text() → Podsumowanie: Sprzedaż 2026
        show("stamp.text()", stamp.text());
        // WYNIK: stamp.text() → Pieczątka: Sprzedaż 2026 (100000 liczb)
        show("ukryte pola w Summary (inner)", hiddenFields(Report.Summary.class));
        // WYNIK: ukryte pola w Summary (inner) → 1
        show("ukryte pola w Stamp (static nested)", hiddenFields(Report.Stamp.class));
        // WYNIK: ukryte pola w Stamp (static nested) → 0

        //   summary ──▶ Summary{ this$0 ──┐ }
        //                                 └──▶ Report{ bigData = [100 000 liczb], title }
        //   stamp ────▶ Stamp{ text }          ↑ Report żyje, dopóki żyje summary!
        //
        // PUŁAPKA: jeśli zapamiętasz gdzieś na długo obiekt inner (np. w liście, w polu static), trzymasz
        // w pamięci CAŁY obiekt zewnętrzny razem z jego danymi. (Java 18+ potrafi pominąć this$0, gdy
        // klasa wewnętrzna w ogóle nie używa obiektu zewnętrznego — tu Summary używa title, więc pole jest.)
        // DOBRA PRAKTYKA: jeśli klasa zagnieżdżona nie potrzebuje Outer.this — dopisz static.
        // IntelliJ podpowiada to komunikatem „Inner class may be static”.
    }

    // =================================================================================================
    // 8. KIEDY KTÓREJ UŻYWAĆ
    // =================================================================================================

    /**
     * 8. Podsumowanie wyboru rodzaju klasy zagnieżdżonej — od najczęstszego do najrzadszego.
     */
    static void whenToUseWhich() {
        section("8. Kiedy której używać");

        note("static nested — domyślnie: pomocnik ściśle związany z klasą (Builder, Entry, Node)");
        // WYNIK: ℹ static nested — domyślnie: pomocnik ściśle związany z klasą (Builder, Entry, Node)
        note("inner — gdy obiekt pomocniczy MUSI widzieć stan konkretnego obiektu (iterator)");
        // WYNIK: ℹ inner — gdy obiekt pomocniczy MUSI widzieć stan konkretnego obiektu (iterator)
        note("lokalna — pomocnik potrzebny w jednej metodzie (rzadko)");
        // WYNIK: ℹ lokalna — pomocnik potrzebny w jednej metodzie (rzadko)
        note("anonimowa — jednorazowa implementacja; dla interfejsu z jedną metodą zwykle lambda");
        // WYNIK: ℹ anonimowa — jednorazowa implementacja; dla interfejsu z jedną metodą zwykle lambda

        // RODZAJ         | GDZIE                 | WIDZI OBIEKT ZEWN.? | TWORZENIE              | NAZWA W .class
        // ---------------+-----------------------+---------------------+------------------------+----------------
        // static nested  | w klasie, ze static   | nie                 | new Outer.Nested()     | Outer$Nested
        // inner          | w klasie, bez static  | tak (Outer.this)    | outer.new Inner()      | Outer$Inner
        // lokalna        | w bloku metody        | tak*, + zmienne lok.| new Local() w metodzie | Outer$1Local
        // anonimowa      | w wyrażeniu new       | tak*, + zmienne lok.| od razu jeden obiekt   | Outer$1
        //   * tylko gdy metoda nie jest static; zmienne lokalne muszą być efektywnie finalne
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • static nested: new Outer.Nested(); nie ma Outer.this; widzi static (także private) zewnętrznej.
     *   • inner: outer.new Inner(); widzi pola obiektu zewnętrznego; Outer.this = obiekt zewnętrzny.
     *   • lokalna: w metodzie; widzi efektywnie finalne zmienne lokalne.
     *   • anonimowa: new Interfejs() { ... } — klasa i jeden obiekt naraz; bez konstruktora.
     *   • Klasa zewnętrzna i zagnieżdżone widzą nawzajem swoje private.
     *   • Inner trzyma ukrytą referencję (this$0) → może zatrzymać duży obiekt w pamięci. Nie potrzebujesz
     *     Outer.this? Dopisz static.
     *   • Interfejs z jedną metodą: zamiast klasy anonimowej — lambda (t13).
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  Car c = new Car("Kia"); Car.Dashboard d = c.new Dashboard(); c.accelerate(40);
     *                   System.out.println(d.display());
     *   2. ZNAJDŹ BŁĄD:  Car.Dashboard d = new Car.Dashboard();
     *   3. ZNAJDŹ BŁĄD:  int limit = 3; limit++;
     *                    class Check { boolean ok(int x) { return x < limit; } }
     *   4. Czym różni się static nested class od inner class? Podaj dwie różnice.
     *   5. Co wypisze:  new House("Zielony").room("strych").address()
     *   6. Dlaczego IntelliJ podpowiada „Inner class may be static” i czemu warto go posłuchać?
     *   7. Kiedy wybrać klasę anonimową zamiast lambdy?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        String[] animals = {"lis", "kot", "żubr", "osa", "wilk"};   // animals = zwierzęta

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: powitanie", "Witaj, Ola!", () -> exercise1().greet("Ola"));
        Check.equal("ćw. 1: to klasa anonimowa", true, () -> exercise1().getClass().isAnonymousClass());
        Check.equal("ćw. 2: po długości, potem alfabetycznie", "[kot, lis, osa, wilk, żubr]",
                () -> Arrays.toString(exercise2(animals)));
        Check.equal("ćw. 2: oryginał bez zmian", "[lis, kot, żubr, osa, wilk]", () -> {
            exercise2(animals);
            return Arrays.toString(animals);
        });
        Check.equal("ćw. 3: dwa klikacze, jeden licznik", 3, () -> {
            Tally gate = new Tally();
            Tally.Clicker left = gate.new Clicker();
            Tally.Clicker right = gate.new Clicker();
            left.click();
            right.click();
            right.click();
            return gate.getCount();
        });
        Check.equal("ćw. 3: inny licznik bez zmian", 0, () -> {
            Tally first = new Tally();
            Tally second = new Tally();
            first.new Clicker().click();
            return second.getCount();
        });
        Check.equal("ćw. 4: pełna pizza", "Margherita 32 cm + ser",
                () -> new Pizza.Builder("Margherita").size(32).extraCheese().build().toString());
        Check.equal("ćw. 4: wartości domyślne", "Funghi 30 cm", () -> new Pizza.Builder("Funghi").build().toString());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec): powitanie", "Witaj, Ola!", () -> solution1().greet("Ola"));
        Check.equal("ćw. 1 (wzorzec): to klasa anonimowa", true, () -> solution1().getClass().isAnonymousClass());
        Check.equal("ćw. 2 (wzorzec): po długości, potem alfabetycznie", "[kot, lis, osa, wilk, żubr]",
                () -> Arrays.toString(solution2(animals)));
        Check.equal("ćw. 2 (wzorzec): oryginał bez zmian", "[lis, kot, żubr, osa, wilk]", () -> {
            solution2(animals);
            return Arrays.toString(animals);
        });
        Check.equal("ćw. 3 (wzorzec): dwa klikacze, jeden licznik", 3, () -> {
            TallySolution gate = new TallySolution();
            TallySolution.Clicker left = gate.new Clicker();
            TallySolution.Clicker right = gate.new Clicker();
            left.click();
            right.click();
            right.click();
            return gate.getCount();
        });
        Check.equal("ćw. 3 (wzorzec): inny licznik bez zmian", 0, () -> {
            TallySolution first = new TallySolution();
            TallySolution second = new TallySolution();
            first.new Clicker().click();
            return second.getCount();
        });
        Check.equal("ćw. 4 (wzorzec): pełna pizza", "Margherita 32 cm + ser",
                () -> new PizzaSolution.Builder("Margherita").size(32).extraCheese().build().toString());
        Check.equal("ćw. 4 (wzorzec): wartości domyślne", "Funghi 30 cm",
                () -> new PizzaSolution.Builder("Funghi").build().toString());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 8 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć obiekt Greeter utworzony jako KLASA ANONIMOWA, który dla imienia "Ola"
     * zwraca "Witaj, Ola!".
     * Podpowiedź: {@code return new Greeter() { ... };} — w środku metoda greet z @Override i public.
     */
    static Greeter exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ sortowanie tak, by zamiast osobnej klasy ByLengthThenAlpha
     * (użytej tylko raz) użyć KLASY ANONIMOWEJ w miejscu wywołania. Kolejność: krótsze słowa pierwsze,
     * przy równej długości alfabetycznie. Sortuj KOPIĘ tablicy (oryginał ma zostać bez zmian).
     * <pre>{@code
     * static class ByLengthThenAlpha implements Comparator<String> {
     *     public int compare(String a, String b) {
     *         int byLength = Integer.compare(a.length(), b.length());
     *         return byLength != 0 ? byLength : a.compareTo(b);
     *     }
     * }
     * // ... w metodzie:
     * String[] copy = words.clone();
     * Arrays.sort(copy, new ByLengthThenAlpha());
     * return copy;
     * }</pre>
     * Podpowiedź: {@code Arrays.sort(copy, new Comparator<String>() { ... });} — ciało compare bez zmian.
     * compareTo porównuje kody Unicode; do polskiej kolejności (ą po a) służy Collator (t12).
     */
    static String[] exercise2(String[] words) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): dokończ klasę wewnętrzną Clicker. Każde click() ma zwiększyć licznik count
     * obiektu Tally, do którego klikacz należy. Kilka klikaczy tego samego Tally liczy wspólnie.
     * Podpowiedź: inner class widzi pola obiektu zewnętrznego — wystarczy count++ (albo Tally.this.count++).
     */
    static class Tally {                        // tally = licznik (np. osób wchodzących przez bramkę)
        private int count;                      // count = liczba

        int getCount() {
            return count;
        }

        class Clicker {                         // clicker = klikacz (inner — należy do konkretnego Tally)
            void click() {                      // click = kliknij
                // TODO: twoje rozwiązanie
                throw new UnsupportedOperationException("TODO");
            }
        }
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): dokończ static nested class Pizza.Builder (łączy Oop06 i tę lekcję).
     * size(int) i extraCheese() mają zapamiętać wartość i zwrócić {@code this} (żeby dało się łączyć
     * wywołania), a build() — utworzyć niezmienną Pizza. Domyślny rozmiar to 30 cm, bez dodatkowego sera.
     * Podpowiedź: Builder jest static nested, więc może wywołać PRYWATNY konstruktor Pizza: new Pizza(this).
     */
    static final class Pizza {
        private final String name;
        private final int size;                 // size = rozmiar (cm)
        private final boolean extraCheese;      // extra cheese = dodatkowy ser

        private Pizza(Builder builder) {        // private — pizzę buduje się TYLKO przez Builder
            this.name = builder.name;
            this.size = builder.size;
            this.extraCheese = builder.extraCheese;
        }

        @Override
        public String toString() {
            return name + " " + size + " cm" + (extraCheese ? " + ser" : "");
        }

        static class Builder {                  // Builder = budowniczy
            private final String name;
            private int size = 30;
            private boolean extraCheese;

            Builder(String name) {
                this.name = name;
            }

            Builder size(int size) {
                // TODO: twoje rozwiązanie
                throw new UnsupportedOperationException("TODO");
            }

            Builder extraCheese() {
                // TODO: twoje rozwiązanie
                throw new UnsupportedOperationException("TODO");
            }

            Pizza build() {
                // TODO: twoje rozwiązanie
                throw new UnsupportedOperationException("TODO");
            }
        }
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Greeter solution1() {
        return new Greeter() {
            @Override
            public String greet(String name) {
                return "Witaj, " + name + "!";
            }
        };
    }

    static String[] solution2(String[] words) {
        String[] copy = words.clone();
        Arrays.sort(copy, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                int byLength = Integer.compare(a.length(), b.length());
                return byLength != 0 ? byLength : a.compareTo(b);
            }
        });
        return copy;
    }

    static class TallySolution {
        private int count;

        int getCount() {
            return count;
        }

        class Clicker {
            void click() {
                count++;                        // = TallySolution.this.count++
            }
        }
    }

    static final class PizzaSolution {
        private final String name;
        private final int size;
        private final boolean extraCheese;

        private PizzaSolution(Builder builder) {
            this.name = builder.name;
            this.size = builder.size;
            this.extraCheese = builder.extraCheese;
        }

        @Override
        public String toString() {
            return name + " " + size + " cm" + (extraCheese ? " + ser" : "");
        }

        static class Builder {
            private final String name;
            private int size = 30;
            private boolean extraCheese;

            Builder(String name) {
                this.name = name;
            }

            Builder size(int size) {
                this.size = size;
                return this;                    // zwracamy SIEBIE → można dopisać .extraCheese()...
            }

            Builder extraCheese() {
                this.extraCheese = true;
                return this;
            }

            PizzaSolution build() {
                return new PizzaSolution(this); // prywatny konstruktor — dostępny, bo jesteśmy w środku
            }
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Kia: 40 km/h — deska nie kopiuje prędkości, tylko czyta ją z obiektu Car przy każdym wywołaniu.
     *   2. Dashboard to inner class — potrzebuje obiektu Car: Car.Dashboard d = car.new Dashboard();
     *      (błąd kompilacji: an enclosing instance ... is required).
     *   3. limit nie jest efektywnie finalna (limit++), a klasa lokalna jej używa → błąd kompilacji.
     *      Poprawka: nie zmieniaj limit albo skopiuj ją do nowej zmiennej: int max = limit;
     *   4. static nested nie ma referencji do obiektu zewnętrznego (tworzona przez new Outer.Nested(),
     *      nie widzi pól instancji); inner ma ukryte Outer.this (tworzona przez outer.new Inner(),
     *      widzi pola konkretnego obiektu).
     *   5. strych w domu „Zielony”
     *   6. Bo klasa wewnętrzna, która nie używa Outer.this, i tak trzyma ukrytą referencję — niepotrzebnie
     *      zatrzymuje obiekt zewnętrzny w pamięci i wymaga go do utworzenia. static usuwa oba problemy.
     *   7. Gdy potrzebujesz stanu (pól), kilku metod, albo rozszerzasz klasę (nie interfejs z jedną
     *      metodą). Dla interfejsu z jedną metodą wybierz lambdę.
     */
    // </editor-fold>
}
