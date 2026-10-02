package t23_modern_java;

import helpers.Check;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Rekordy, instanceof ze wzorcem i klasy zapieczętowane (Java 16–17)
 *        (record = rekord; pattern = wzorzec; sealed = zapieczętowany; permits = zezwala)
 *
 * W SKRÓCIE:
 *   Trzy nowości tworzą razem „algebraiczne typy danych” w Javie: REKORDY (Java 16, JEP 395) opisują dane,
 *   {@code instanceof} ze wzorcem (Java 16, JEP 394) pozwala sprawdzić typ i od razu go użyć bez rzutowania,
 *   a KLASY ZAPIECZĘTOWANE (Java 17, JEP 409) zamykają zbiór dozwolonych podtypów. Razem opisujesz domenę
 *   jako zamkniętą listę wariantów — i kompilator wie, jakie to warianty.
 *
 * ANALOGIA:
 *   Menu w restauracji: danie dnia to jedna z trzech pozycji (zupa, makaron, ryba) — i tylko te trzy. Rekord to
 *   karta pojedynczego dania (nazwa, cena). Zapieczętowanie to zdanie „nic poza tym menu nie serwujemy”,
 *   więc kelner (kod) może przygotować się na każdy wariant. Wzorzec instanceof to kelner, który patrzy na talerz
 *   i od razu wie, co to jest, bez zaglądania do notatnika.
 *
 * JAK TO DZIAŁA:
 *   1. Rekord: {@code record Point(int x, int y) { }} — kompilator tworzy pola final, konstruktor, akcesory
 *      {@code x()} i {@code y()}, equals, hashCode i toString. Jest płytko niezmienny.
 *   2. {@code if (o instanceof String s && s.length() > 3)} — jeśli o jest napisem, zmienna wzorca s jest już
 *      typu String; widoczna tylko tam, gdzie kompilator ma pewność, że wzorzec pasuje („zasięg przepływowy”).
 *   3. {@code sealed interface Shape permits Circle, Square} — tylko wymienione typy mogą implementować Shape.
 *      Każdy musi być final, sealed albo non-sealed (rekordy i enumy są domyślnie final).
 *   4. Gdy wszystkie warianty są znane, kod obsługujący je „wyczerpująco” jest bezpieczny: nowy wariant to
 *      sygnał do przejrzenia każdego miejsca użycia. W Javie 17 sprawdzasz to ręcznie (łańcuch if-else
 *      z końcowym throw); od Javy 21 robi to kompilator w switchu ze wzorcami (JEP 441, rekordy: JEP 440).
 *
 *   Historia: rekordy — podgląd 14 i 15, ostatecznie 16; instanceof ze wzorcem — podgląd 14 i 15, ostatecznie 16;
 *   zapieczętowane — podgląd 15 i 16, ostatecznie 17; wzorce rekordów — podgląd 19 i 20, ostatecznie 21.
 *
 * SŁÓWKA:
 *   record = rekord; component = składowa rekordu; compact constructor = konstruktor kompaktowy;
 *   pattern matching = dopasowanie wzorca; binding variable = zmienna wzorca; sealed = zapieczętowany;
 *   permits = zezwala; non-sealed = niezapieczętowany (otwarty); exhaustive = wyczerpujący;
 *   variant = wariant; algebraic data type = algebraiczny typ danych (suma wariantów).
 *
 * ZOBACZ TEŻ: t09_records/Records01Basics (rekordy w głąb), t07_inheritance_polymorphism/Inherit07SealedClasses
 *   (zapieczętowane klasy), t22_design_patterns/Patterns15Visitor (wzorzec Visitor — zastępowany przez sealed + switch),
 *   t23_modern_java/Modern03SwitchExpressions (switch), t23_modern_java/Modern07WhatsNextJava21 (wzorce w Javie 21)
 * </pre>
 */
public class Modern05RecordsSealedPatterns {

    public static void main(String[] args) {
        title("Modern05 — rekordy, wzorce, klasy zapieczętowane");

        recordsRecap();                // records recap = przypomnienie rekordów
        instanceofPattern();           // instanceof pattern = instanceof ze wzorcem
        patternScope();                // pattern scope = zasięg zmiennej wzorca
        sealedSyntax();                // sealed syntax = składnia zapieczętowanych typów
        domainModelling();             // domain modelling = modelowanie domeny
        exhaustiveHandling();          // exhaustive handling = obsługa wyczerpująca
        algebraicResult();             // algebraic result = wynik jako typ algebraiczny
        whenSealedHelps();             // when sealed helps = kiedy sealed pomaga
        java21Preview();               // Java 21 preview = zapowiedź Javy 21
        exercises();                   // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. REKORDY — PRZYPOMNIENIE
    // =================================================================================================

    /** Rekord z konstruktorem kompaktowym (walidacja) i metodą pomocniczą. */
    record Point(int x, int y) {                                   // point = punkt
        Point {                                                    // konstruktor kompaktowy: bez listy parametrów
            if (x < 0 || y < 0) {
                throw new IllegalArgumentException("współrzędne muszą być nieujemne: " + x + ", " + y);
            }
        }

        double distanceTo(Point other) {                           // distance to = odległość do
            return Math.hypot(x - other.x, y - other.y);           // hypot = przeciwprostokątna
        }

        static Point origin() {                                    // origin = początek układu
            return new Point(0, 0);
        }
    }

    /** Rekord bez ochrony: lista z zewnątrz jest przechowywana wprost — płytka niezmienność. */
    record NaiveTeam(String name, List<String> members) { }        // naive = naiwny; members = członkowie

    /** Rekord z obroną: konstruktor kompaktowy robi niezmienną kopię listy. */
    record SafeTeam(String name, List<String> members) {
        SafeTeam {
            members = List.copyOf(members);                        // copy of = kopia z
        }
    }

    /**
     * 1. Rekordy — krótkie przypomnienie (pełny kurs: t09_records). Rekord to nośnik danych: kilka linii zamiast
     * kilkudziesięciu. Pamiętaj tylko o dwóch rzeczach: rekord jest PŁYTKO niezmienny, a konstruktor kompaktowy
     * jest właściwym miejscem na walidację.
     */
    static void recordsRecap() {
        section("1. Rekordy (Java 16+) — przypomnienie");

        Point p = new Point(3, 4);
        show("toString", p);
        // WYNIK: toString → Point[x=3, y=4]
        show("akcesor x()", p.x());
        // WYNIK: akcesor x() → 3
        show("equals — wartości", p.equals(new Point(3, 4)));
        // WYNIK: equals — wartości → true
        show("odległość do początku", p.distanceTo(Point.origin()));
        // WYNIK: odległość do początku → 5.0
        expectThrows("walidacja w konstruktorze", () -> new Point(-1, 2));
        // WYNIK: ✔ walidacja w konstruktorze → rzucono IllegalArgumentException: współrzędne muszą być nieujemne: -1, 2

        // PUŁAPKA: „niezmienny” znaczy tylko, że pól nie da się przypisać ponownie. Jeśli składowa jest zmienną
        //   kolekcją, rekord można zmienić „od środka” — albo z zewnątrz, trzymając referencję do tej samej listy.
        List<String> source = new ArrayList<>(List.of("Ala"));      // source = źródło
        NaiveTeam naive = new NaiveTeam("A", source);
        SafeTeam safe = new SafeTeam("A", source);
        source.add("Ola");
        show("NaiveTeam po zmianie źródła", naive.members());
        // WYNIK: NaiveTeam po zmianie źródła → [Ala, Ola]
        show("SafeTeam po zmianie źródła", safe.members());
        // WYNIK: SafeTeam po zmianie źródła → [Ala]

        // Rekord może być też zadeklarowany lokalnie, wewnątrz metody (Java 16+) — świetny na wynik pośredni:
        record Pair(String key, int value) { }                     // pair = para
        show("lokalny rekord", new Pair("a", 1));
        // WYNIK: lokalny rekord → Pair[key=a, value=1]

        // Rekord nie może dziedziczyć po klasie (już „dziedziczy” po java.lang.Record), nie ma pól instancji
        // poza składowymi, ale może implementować interfejsy, mieć metody i pola statyczne.
        // DOBRA PRAKTYKA: w konstruktorze kompaktowym kopiuj kolekcje (List.copyOf) i waliduj dane — rekord ma być
        //   wartością, która jest poprawna od chwili utworzenia.
    }

    // =================================================================================================
    // 2. INSTANCEOF ZE WZORCEM
    // =================================================================================================

    /** Klasa pokazująca idiom equals z instanceof ze wzorcem. */
    static final class Meters {                                    // meters = metry
        private final int value;                                   // value = wartość

        Meters(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object other) {                      // other = inny
            return other instanceof Meters m && value == m.value;  // sprawdzenie typu + rzutowanie w jednym
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);                        // hash code = kod mieszający
        }
    }

    /**
     * 2. Wzorzec w instanceof: sprawdzenie typu i deklaracja zmiennej w jednym wyrażeniu. Koniec z powtarzaniem
     * nazwy typu trzy razy (sprawdź → zrzutuj → użyj) — a więc i z błędem „zrzutowałem na zły typ”.
     */
    static void instanceofPattern() {
        section("2. instanceof ze wzorcem (Java 16+)");

        Object value = "Kowalski";                                 // Object = „dowolny obiekt”

        // PRZED: trzy kroki, nazwa typu powtórzona dwa razy.
        String before = "nie napis";
        if (value instanceof String) {
            String text = (String) value;                          // rzutowanie ręczne
            before = "napis o długości " + text.length();
        }
        show("PRZED", before);
        // WYNIK: PRZED → napis o długości 8

        // PO: zmienna wzorca s od razu ma typ String.
        String after = "nie napis";
        if (value instanceof String s) {
            after = "napis o długości " + s.length();
        }
        show("PO", after);
        // WYNIK: PO → napis o długości 8

        // Idiom equals: jedna linia zamiast trzech.
        show("Meters(5).equals(Meters(5))", new Meters(5).equals(new Meters(5)));
        // WYNIK: Meters(5).equals(Meters(5)) → true
        show("Meters(5).equals(\"5\")", new Meters(5).equals("5"));
        // WYNIK: Meters(5).equals("5") → false
        show("Meters(5).equals(null)", new Meters(5).equals(null));
        // WYNIK: Meters(5).equals(null) → false

        // Wzorzec nie pasuje do null: (null instanceof String s) jest false — dlatego equals jest bezpieczne.
        Object nothing = null;                                     // nothing = nic
        show("null instanceof String s", nothing instanceof String s);
        // WYNIK: null instanceof String s → false

        // PUŁAPKA: w Javie 16 i 17 wzorzec, który zawsze pasuje, to błąd kompilacji — np. dla zmiennej typu String
        //   zapis „text instanceof String s” jest odrzucany („wyrażenie jest podtypem typu wzorca”). Od Javy 21 jest to dozwolone.
        // PUŁAPKA: nie można użyć typu generycznego z konkretnym parametrem, którego nie da się sprawdzić w czasie
        //   wykonania: o instanceof List<String> to błąd; poprawnie List<?> (parametry typu są usuwane — zobacz t11_generics).
        // DOBRA PRAKTYKA: piszesz „instanceof T t” zamiast „(T) obj” — kompilator pilnuje, żeby użycie t było możliwe
        //   tylko tam, gdzie wzorzec na pewno pasuje.
    }

    // =================================================================================================
    // 3. ZASIĘG ZMIENNEJ WZORCA
    // =================================================================================================

    static String lengthKind(Object value) {                       // length kind = rodzaj długości
        // && : zmienna s widoczna po prawej stronie, bo gdy dojdziemy do s.length(), wzorzec już pasuje
        if (value instanceof String s && s.length() > 5) {
            return "długi tekst: " + s;
        }
        // ! : jeśli to NIE napis, kończymy — dalej s jest widoczne w całej reszcie metody
        if (!(value instanceof String s)) {
            return "to nie tekst";
        }
        return "krótki tekst: " + s;                               // tu s jest już widoczne i przypisane
    }

    /**
     * 3. Zasięg przepływowy (flow scoping): zmienna wzorca jest widoczna dokładnie tam, gdzie kompilator potrafi
     * udowodnić, że wzorzec pasuje. To nie jest „zwykły” zasięg blokowy.
     */
    static void patternScope() {
        section("3. Zasięg zmiennej wzorca");

        show("lengthKind(\"Rzeczpospolita\")", lengthKind("Rzeczpospolita"));
        // WYNIK: lengthKind("Rzeczpospolita") → długi tekst: Rzeczpospolita
        show("lengthKind(\"Ala\")", lengthKind("Ala"));
        // WYNIK: lengthKind("Ala") → krótki tekst: Ala
        show("lengthKind(42)", lengthKind(42));
        // WYNIK: lengthKind(42) → to nie tekst

        // Reguły (mniej więcej):
        //   • a instanceof T t && warunek(t)     — t widoczne w „warunek” i w gałęzi „then”
        //   • !(a instanceof T t) → return/throw — t widoczne PO ifie (gałąź kończy metodę)
        //   • a instanceof T t || warunek(t)     — BŁĄD KOMPILACJI: przy || wzorzec nie musiał pasować
        //   • zmienna wzorca nie może mieć nazwy już użytej w tym zasięgu (np. parametru metody)
        // Zmienna wzorca nie jest final, można ją przypisać ponownie — ale tak nie rób.

        // Przykład z pętlą: wczytuj, dopóki NIE jest liczbą (wzorzec widoczny po pętli).
        Object[] inputs = {"x", "y", 7, "z"};                      // inputs = dane wejściowe
        int index = 0;
        while (!(inputs[index] instanceof Integer found)) {        // found = znaleziona
            index++;
        }
        show("pierwsza liczba na pozycji", index + " → " + (found + 1));
        // WYNIK: pierwsza liczba na pozycji → 2 → 8

        // PUŁAPKA: gdy zmienna wzorca ma tę samą nazwę co pole klasy, „zasłania” ją (przesłanianie) w zasięgu
        //   wzorca — łatwo wtedy odczytać złą wartość. Nazywaj zmienne wzorców krótko i inaczej niż pola.
        // DOBRA PRAKTYKA: wczesny return przy braku dopasowania („guard clause” = klauzula strażnicza) daje płaski,
        //   czytelny kod bez zagnieżdżeń: najpierw odrzuć złe przypadki, potem pracuj na dopasowanej zmiennej.
    }

    // =================================================================================================
    // 4. KLASY ZAPIECZĘTOWANE — SKŁADNIA
    // =================================================================================================

    /** Zapieczętowany interfejs: tylko wymienione typy mogą go implementować. */
    sealed interface Shape permits Circle, Square, Rectangle {     // shape = kształt
    }

    record Circle(double radius) implements Shape { }              // circle = koło; radius = promień

    record Square(double side) implements Shape { }                // square = kwadrat; side = bok

    record Rectangle(double width, double height) implements Shape { }   // rectangle = prostokąt

    /** Zapieczętowana klasa abstrakcyjna z różnymi rodzajami podklas. */
    abstract static sealed class Vehicle permits Car, Truck, Bicycle { }   // vehicle = pojazd

    static final class Car extends Vehicle { }                     // final: koniec dziedziczenia

    static non-sealed class Truck extends Vehicle { }              // non-sealed: dziedziczenie znowu otwarte

    static final class Bicycle extends Vehicle { }                 // bicycle = rower

    static class Tipper extends Truck { }                          // tipper = wywrotka; wolno, bo Truck jest non-sealed

    /** Bez permits: gdy podtypy są w TYM SAMYM pliku, kompilator sam ustala listę. */
    sealed interface Light { }                                     // light = światło

    record Red() implements Light { }

    record Green() implements Light { }

    /**
     * 4. Składnia zapieczętowanych typów. Zasady: (1) każdy dozwolony podtyp musi bezpośrednio dziedziczyć po typie
     * zapieczętowanym, (2) musi być final, sealed albo non-sealed (rekordy i enumy są domyślnie final),
     * (3) podtypy muszą być w tym samym module albo — gdy nie ma modułów — w tym samym pakiecie.
     */
    static void sealedSyntax() {
        section("4. Klasy zapieczętowane (Java 17+) — składnia");

        // Class.isSealed() i getPermittedSubclasses() (Java 17+) — „widzą” zapieczętowanie w czasie wykonania.
        show("Shape.isSealed()", Shape.class.isSealed());
        // WYNIK: Shape.isSealed() → true
        show("Shape — dozwolone podtypy", names(Shape.class.getPermittedSubclasses()));
        // WYNIK: Shape — dozwolone podtypy → [Circle, Rectangle, Square]
        show("Vehicle — dozwolone podtypy", names(Vehicle.class.getPermittedSubclasses()));
        // WYNIK: Vehicle — dozwolone podtypy → [Bicycle, Car, Truck]
        show("Light (bez permits)", names(Light.class.getPermittedSubclasses()));
        // WYNIK: Light (bez permits) → [Green, Red]
        show("Car.isSealed()", Car.class.isSealed());
        // WYNIK: Car.isSealed() → false
        show("Tipper extends Truck → Vehicle?", Vehicle.class.isAssignableFrom(Tipper.class));
        // WYNIK: Tipper extends Truck → Vehicle? → true

        // Czego NIE wolno (błędy kompilacji):
        //   • class Plane extends Vehicle { }        — Plane nie jest na liście permits
        //   • static class Bus extends Vehicle {}    — nawet w tym samym pliku, jeśli permits go nie wymienia
        //   • podklasa bez modyfikatora final/sealed/non-sealed
        //   • anonimowa i lokalna podklasa typu zapieczętowanego
        // Dlaczego trzy modyfikatory? Zapieczętowanie jest „zaraźliwe”: autor musi zdecydować o każdym podtypie:
        //   final = tu koniec, sealed = dalsza kontrolowana lista, non-sealed = świadomie otwieram.
        // PUŁAPKA: non-sealed dziurawi gwarancję zamkniętej hierarchii — każdy może dopisać podklasę Truck
        //   (jak Tipper), więc pełnej listy wariantów nie znasz. Używaj tylko, gdy to celowe.
        // DOBRA PRAKTYKA: wariantom zapieczętowanego interfejsu daj rekordy (są final) — krótko i niezmiennie.
    }

    /** Pomocnicza: nazwy klas, posortowane (kolejność z refleksji nie jest dla nas gwarantowana). */
    private static List<String> names(Class<?>[] classes) {
        return Arrays.stream(classes).map(Class::getSimpleName).sorted().toList();   // sorted = posortowane
    }

    // =================================================================================================
    // 5. MODELOWANIE DOMENY
    // =================================================================================================

    /** Metoda płatności jako zamknięta lista wariantów (payment = płatność). */
    sealed interface Payment permits CardPayment, BlikPayment, TransferPayment { }

    record CardPayment(String number) implements Payment { }       // number = numer karty

    record BlikPayment(String code) implements Payment { }         // code = kod

    record TransferPayment(String accountNumber) implements Payment { }   // account number = numer konta

    /** Opłata w groszach (fee = prowizja): karta 2% kwoty, BLIK 0, przelew stała 150 gr. */
    static long fee(Payment payment, long amountCents) {           // amount cents = kwota w groszach
        if (payment instanceof CardPayment) {
            return amountCents * 2 / 100;
        } else if (payment instanceof BlikPayment) {
            return 0;
        } else if (payment instanceof TransferPayment) {
            return 150;
        } else {
            // Do tej gałęzi nie powinno nigdy dojść. Jeśli dojdzie, ktoś dopisał wariant i zapomniał tu.
            throw new IllegalStateException("nieobsłużona metoda płatności: " + payment);
        }
    }

    /** Opis płatności z wzorcem — zmienna wzorca daje dostęp do składowych. */
    static String describe(Payment payment) {
        if (payment instanceof CardPayment card) {
            return "karta ****" + card.number().substring(card.number().length() - 4);
        } else if (payment instanceof BlikPayment blik) {
            return "BLIK " + blik.code();
        } else if (payment instanceof TransferPayment transfer) {
            return "przelew na " + transfer.accountNumber();
        }
        throw new IllegalStateException("nieobsłużona metoda płatności: " + payment);
    }

    /** Drzewo wyrażeń arytmetycznych: każdy węzeł to rekord (expression = wyrażenie). */
    sealed interface Expr permits Num, Add, Mul, Neg { }

    record Num(int value) implements Expr { }                      // num = liczba

    record Add(Expr left, Expr right) implements Expr { }          // add = dodawanie

    record Mul(Expr left, Expr right) implements Expr { }          // mul = mnożenie

    record Neg(Expr operand) implements Expr { }                   // neg = przeciwieństwo (minus)

    static int eval(Expr expr) {                                   // eval = oblicz
        if (expr instanceof Num n) {
            return n.value();
        } else if (expr instanceof Add a) {
            return eval(a.left()) + eval(a.right());
        } else if (expr instanceof Mul m) {
            return eval(m.left()) * eval(m.right());
        } else if (expr instanceof Neg g) {
            return -eval(g.operand());
        }
        throw new IllegalStateException("nieobsłużone wyrażenie: " + expr);
    }

    static String render(Expr expr) {                              // render = zapisz tekstowo
        if (expr instanceof Num n) {
            return String.valueOf(n.value());
        } else if (expr instanceof Add a) {
            return "(" + render(a.left()) + " + " + render(a.right()) + ")";
        } else if (expr instanceof Mul m) {
            return "(" + render(m.left()) + " * " + render(m.right()) + ")";
        } else if (expr instanceof Neg g) {
            return "-" + render(g.operand());
        }
        throw new IllegalStateException("nieobsłużone wyrażenie: " + expr);
    }

    /**
     * 5. Modelowanie domeny: zamiast klasy z polem „typ” i kilkoma polami zerowanymi (null), mamy osobny rekord
     * dla każdego wariantu — każdy ma dokładnie te dane, których potrzebuje. Niemożliwe stany są niereprezentowalne.
     */
    static void domainModelling() {
        section("5. Modelowanie domeny: płatności i drzewo wyrażeń");

        List<Payment> payments = List.of(
                new CardPayment("1234567812345678"), new BlikPayment("123456"), new TransferPayment("PL61 1090"));
        for (Payment payment : payments) {
            show(describe(payment), fee(payment, 20_000) + " gr prowizji od 200 zł");
        }
        // WYNIK: karta ****5678 → 400 gr prowizji od 200 zł
        // WYNIK: BLIK 123456 → 0 gr prowizji od 200 zł
        // WYNIK: przelew na PL61 1090 → 150 gr prowizji od 200 zł

        // 2 + 3 * 4
        Expr expr = new Add(new Num(2), new Mul(new Num(3), new Num(4)));
        show("drzewo (toString rekordów)", expr);
        // WYNIK: drzewo (toString rekordów) → Add[left=Num[value=2], right=Mul[left=Num[value=3], right=Num[value=4]]]
        show("zapis", render(expr));
        // WYNIK: zapis → (2 + (3 * 4))
        show("wartość", eval(expr));
        // WYNIK: wartość → 14
        show("z minusem", render(new Neg(expr)) + " = " + eval(new Neg(expr)));
        // WYNIK: z minusem → -(2 + (3 * 4)) = -14

        // PRZED (bez sealed): klasa Payment z polem „type” i polami cardNumber/blikCode/accountNumber,
        //   z których wypełnione jest tylko jedno — reszta null. Łatwo o NullPointerException i o stan niemożliwy
        //   (np. type = BLIK, a kod pusty). PO: rekord na wariant, bez pól opcjonalnych.
        // DOBRA PRAKTYKA: wariantom daj nazwy z domeny (CardPayment, BlikPayment), a nie technicznego „Type1”.
        //   Rekord rekurencyjny (Add zawiera Expr) pozwala opisać drzewa i grafy bez dodatkowych klas.
    }

    // =================================================================================================
    // 6. OBSŁUGA WYCZERPUJĄCA W JAVIE 17
    // =================================================================================================

    /** Celowo niepełna obsługa: zapomniano o Rectangle (perimeter = obwód). */
    static double perimeterIncomplete(Shape shape) {
        if (shape instanceof Circle c) {
            return 2 * Math.PI * c.radius();
        } else if (shape instanceof Square s) {
            return 4 * s.side();
        } else {
            throw new IllegalStateException("nieobsłużony kształt: " + shape);
        }
    }

    /**
     * 6. W Javie 17 kompilator nie sprawdza wyczerpywalności łańcucha if-else po typach — to robi dopiero switch
     * ze wzorcami w Javie 21. Dlatego ostatnia gałąź musi rzucać wyjątek, a nie zwracać „wartość domyślną”:
     * wtedy dopisanie wariantu zakończy się głośnym błędem w pierwszym teście, a nie cichym złym wynikiem.
     */
    static void exhaustiveHandling() {
        section("6. Obsługa wyczerpująca w Javie 17");

        show("obwód kwadratu 2", perimeterIncomplete(new Square(2)));
        // WYNIK: obwód kwadratu 2 → 8.0
        expectThrows("zapomniany Rectangle", () -> perimeterIncomplete(new Rectangle(2, 3)));
        // WYNIK: ✔ zapomniany Rectangle → rzucono IllegalStateException: nieobsłużony kształt: Rectangle[width=2.0, height=3.0]

        // PUŁAPKA: ostatnie „else return 0;” ukryłoby brakujący wariant — program działałby dalej z błędnymi danymi.
        // PUŁAPKA: w Javie 17 NIE ma ostrzeżenia, że łańcuch if-else nie obejmuje wszystkich wariantów.
        //   Pomagają: testy po jednym przypadku na wariant, przeszukanie projektu po nazwie typu zapieczętowanego
        //   po każdej zmianie i (po aktualizacji do Javy 21) switch bez default.
        // DOBRA PRAKTYKA: końcowy throw z treścią zawierającą obiekt (jak wyżej) — diagnoza z komunikatu błędu
        //   od razu wskaże brakujący wariant.
    }

    // =================================================================================================
    // 7. TYP ALGEBRAICZNY: WYNIK ALBO BŁĄD
    // =================================================================================================

    /** Wynik operacji, która może się nie udać: albo Ok z wartością, albo Failure z powodem. */
    sealed interface Result<T> permits Ok, Failure { }             // result = wynik

    record Ok<T>(T value) implements Result<T> { }                 // ok = udało się

    record Failure<T>(String reason) implements Result<T> { }      // failure = porażka; reason = powód

    static Result<Integer> parseAge(String text) {                 // parse age = zamień tekst na wiek
        try {
            int age = Integer.parseInt(text.trim());
            if (age < 0 || age > 150) {
                return new Failure<>("wiek poza zakresem: " + age);
            }
            return new Ok<>(age);
        } catch (NumberFormatException e) {
            return new Failure<>("to nie liczba: " + text);
        }
    }

    static String render(Result<Integer> result) {                 // przeciążenie render dla wyniku
        if (result instanceof Ok<Integer> ok) {
            return "wiek = " + ok.value();
        } else if (result instanceof Failure<Integer> failure) {
            return "błąd: " + failure.reason();
        }
        throw new IllegalStateException("nieobsłużony wynik: " + result);
    }

    /**
     * 7. Algebraiczny typ danych = „suma” wariantów (Ok LUB Failure; tu sealed) złożona z „iloczynów” (każdy
     * wariant to rekord z kilkoma polami). Taki wynik zastępuje zwracanie null albo rzucanie wyjątków dla
     * zwykłych, oczekiwanych błędów — wywołujący MUSI zająć się obiema możliwościami.
     */
    static void algebraicResult() {
        section("7. Wynik albo błąd jako typ zapieczętowany");

        show(" \"42\"", render(parseAge("42")));
        // WYNIK:  "42" → wiek = 42
        show(" \"abc\"", render(parseAge("abc")));
        // WYNIK:  "abc" → błąd: to nie liczba: abc
        show(" \"200\"", render(parseAge("200")));
        // WYNIK:  "200" → błąd: wiek poza zakresem: 200
        show("toString wariantu", parseAge("7"));
        // WYNIK: toString wariantu → Ok[value=7]

        // Typ generyczny + zapieczętowanie działa: Result<T> ma dwa warianty, oba też generyczne.
        // Dla porównania: Optional<T> to „Ok albo nic”, ale bez powodu porażki; wyjątek niesie powód, ale
        // nie jest widoczny w typie wyniku (poza wyjątkami sprawdzanymi).
        // DOBRA PRAKTYKA: używaj Result dla błędów, które są normalnym przebiegiem (zła wartość z formularza);
        //   wyjątki zostaw dla sytuacji wyjątkowych (awaria zasobu, naruszenie założeń programisty).
    }

    // =================================================================================================
    // 8. KIEDY SEALED POMAGA, A KIEDY SZKODZI
    // =================================================================================================

    /**
     * 8. Zapieczętowanie to kontrakt „zbiór wariantów jest ZAMKNIĘTY i znany autorowi”. Pomaga tam, gdzie
     * zbiór naprawdę jest zamknięty; szkodzi tam, gdzie ktoś inny ma prawo dopisywać warianty.
     */
    static void whenSealedHelps() {
        section("8. Kiedy sealed pomaga, a kiedy szkodzi");

        // POMAGA — zamknięty zbiór, którym zarządzasz Ty:
        //   • wyniki operacji (Ok / Failure), zdarzenia domenowe, węzły drzewa składni, stany automatu,
        //   • polecenia protokołu o stałej liście, metody płatności obsługiwane przez Twój system.
        // SZKODZI — zbiór otwarty:
        //   • wtyczki i rozszerzenia dopisywane przez innych (interfejs zwykły albo klasa abstrakcyjna),
        //   • typy z biblioteki publicznej, którą inni rozszerzają — dodanie wariantu psuje kod klientów,
        //     którzy obsługują wszystkie dotychczasowe (po stronie Javy 21: switch przestaje się kompilować).
        // ALTERNATYWY:
        //   • enum — gdy warianty to stałe bez własnych danych lub z takimi samymi polami,
        //   • zwykły interfejs + polimorfizm (metoda w interfejsie) — gdy zachowanie należy do wariantu,
        //     a nie do zewnętrznej funkcji. Dodanie nowego wariantu jest wtedy łatwe, dodanie nowej operacji — trudne.
        //   • sealed + osobne funkcje (wzorce) — odwrotnie: nowa operacja łatwa, nowy wariant wymaga przeglądu.
        // To znany „problem ekspresji” (expression problem): nie da się mieć obu rozszerzeń za darmo.
        note("Sealed: łatwo dodać operację, trudniej wariant. Zwykły interfejs: odwrotnie.");
        // WYNIK: ℹ Sealed: łatwo dodać operację, trudniej wariant. Zwykły interfejs: odwrotnie.

        // PUŁAPKA: zapieczętowany interfejs w publicznym API biblioteki — dodanie wariantu w nowej wersji
        //   łamie kompatybilność wsteczną dla każdego, kto liczył na pełną obsługę.
        // DOBRA PRAKTYKA: zapieczętowuj typy WEWNĘTRZNE dla swojego modułu; w publicznym API rób to tylko wtedy,
        //   gdy lista wariantów jest częścią kontraktu i zamierzasz ją utrzymać.
    }

    // =================================================================================================
    // 9. ZAPOWIEDŹ JAVY 21
    // =================================================================================================

    /**
     * 9. Ta sama logika w Javie 21 (switch ze wzorcami, JEP 441, i wzorce rekordów, JEP 440). W Javie 17 powyższy kod
     * się nie skompiluje — dlatego zostaje w komentarzu.
     */
    static void java21Preview() {
        section("9. Zapowiedź: ta sama logika w Javie 21+");

        // (Java 21+) switch po zapieczętowanym typie — bez default; kompilator wie, że warianty są wyczerpane:
        //   static long fee(Payment payment, long amountCents) {
        //       return switch (payment) {
        //           case CardPayment card -> amountCents * 2 / 100;
        //           case BlikPayment blik -> 0;
        //           case TransferPayment transfer -> 150;
        //       };      // dopisanie nowego wariantu bez gałęzi = błąd kompilacji
        //   }
        // (Java 21+) wzorce rekordów — rozbiór rekordu wprost na składowe, także zagnieżdżony:
        //   static int eval(Expr expr) {
        //       return switch (expr) {
        //           case Num(int value) -> value;
        //           case Add(Expr left, Expr right) -> eval(left) + eval(right);
        //           case Mul(Expr left, Expr right) -> eval(left) * eval(right);
        //           case Neg(Expr operand) -> -eval(operand);
        //       };
        //   }
        //   (nested) case Add(Num(int a), Num(int b)) -> a + b;   // dwa poziomy naraz
        // (Java 21+) strażnik when:  case Num(int value) when value < 0 -> "ujemna";
        //
        // Ta kombinacja (rekordy + sealed + switch ze wzorcami) bywa nazywana „programowaniem zorientowanym na dane”
        // (data-oriented programming) i często zastępuje wzorzec Visitor (zobacz t22_design_patterns/Patterns15Visitor):
        // zamiast metody accept w każdej klasie masz jeden switch w jednym miejscu.
        // Zobacz też Modern07WhatsNextJava21.
        note("W Javie 17: łańcuch if-else + końcowy throw. W Javie 21: switch bez default.");
        // WYNIK: ℹ W Javie 17: łańcuch if-else + końcowy throw. W Javie 21: switch bez default.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Rekord (Java 16): dane + konstruktor, akcesory, equals, hashCode, toString; płytko niezmienny.
     *   • Konstruktor kompaktowy: walidacja i kopiowanie kolekcji (List.copyOf).
     *   • instanceof ze wzorcem (Java 16): o instanceof T t; t widoczne tam, gdzie wzorzec na pewno pasuje.
     *   • && rozszerza zasięg wzorca na prawą stronę, || nie; po if (!(o instanceof T t)) return; — t jest widoczne.
     *   • Wzorzec nie pasuje do null; wzorzec zawsze pasujący jest w Javie 17 błędem (od 21 — dozwolony).
     *   • sealed (Java 17): permits, podtypy final / sealed / non-sealed; rekordy i enumy są final;
     *     ten sam moduł lub pakiet; Class.isSealed(), getPermittedSubclasses().
     *   • Bez permits, gdy podtypy są w tym samym pliku.
     *   • Wyczerpującą obsługę w Javie 17 robisz ręcznie: ostatnie else = throw. Java 21: switch ze wzorcami.
     *   • sealed pomaga przy zamkniętym zbiorze wariantów; przy wtyczkach i otwartym API — szkodzi.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego rekord z polem typu List jest tylko „płytko” niezmienny i jak temu zaradzić?
     *   2. Jaki zasięg ma zmienna s w  if (o instanceof String s && s.isEmpty())  i dlaczego
     *      zapis z || się nie skompiluje?
     *   3. Co wypisze:  Object o = null;  System.out.println(o instanceof String s);  ?
     *   4. ZNAJDŹ BŁĄD:  sealed interface A permits B { }   class B implements A { }
     *   5. Czym różnią się final, sealed i non-sealed w podtypie typu zapieczętowanego?
     *   6. Co wypisze:  record P(int x) { }  System.out.println(new P(1).equals(new P(1)) + " " + new P(7));  ?
     *   7. Dlaczego ostatnia gałąź łańcucha if-else po wariantach powinna rzucać wyjątek, a nie zwracać wartość domyślną?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /**
     * ĆWICZENIE 2 (średnie): uzupełnij rekord Range (zakres liczb całkowitych): konstruktor kompaktowy ma odrzucać
     * zakres, w którym from jest większe od to (IllegalArgumentException), a metoda length() ma zwracać to - from.
     * Podpowiedź: wzoruj się na konstruktorze kompaktowym rekordu Point z sekcji 1; wzorzec w RangeSolution.
     */
    record Range(int from, int to) {
        Range {
            // TODO: odrzuć zakres, w którym from > to (rzuć IllegalArgumentException)
        }

        int length() {                                             // length = długość
            // TODO: zwróć to - from
            throw new UnsupportedOperationException("TODO");
        }
    }

    /** Rekord wzorcowy do ćwiczenia 2. */
    record RangeSolution(int from, int to) {
        RangeSolution {
            if (from > to) {
                throw new IllegalArgumentException("from > to: " + from + " > " + to);
            }
        }

        int length() {
            return to - from;
        }
    }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: opis obiektu", List.of("tekst o długości 3", "liczba 7", "inne"),
                () -> List.of(exercise1("abc"), exercise1(7), exercise1(2.5)));
        Check.throwsException("ćw. 2: zakres 5..1", IllegalArgumentException.class, () -> new Range(5, 1));
        Check.equal("ćw. 2: długość zakresu 2..6", 4, () -> new Range(2, 6).length());
        Check.equal("ćw. 3: obwody", List.of("8.00", "10.00", "6.28"),
                () -> List.of(exercise3(new Square(2)), exercise3(new Rectangle(2, 3)), exercise3(new Circle(1))));
        Check.equal("ćw. 4: uproszczenie 1*7 + 0", new Num(7),
                () -> exercise4(new Add(new Mul(new Num(1), new Num(7)), new Num(0))));
        Check.equal("ćw. 4: uproszczenie (2+0) * 0", new Num(0),
                () -> exercise4(new Mul(new Add(new Num(2), new Num(0)), new Num(0))));
        Check.equal("ćw. 4: bez zmian 2+3", new Add(new Num(2), new Num(3)),
                () -> exercise4(new Add(new Num(2), new Num(3))));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("tekst o długości 3", "liczba 7", "inne"),
                () -> List.of(solution1("abc"), solution1(7), solution1(2.5)));
        Check.throwsException("ćw. 2 (wzorzec): zakres 5..1", IllegalArgumentException.class,
                () -> new RangeSolution(5, 1));
        Check.equal("ćw. 2 (wzorzec): długość 2..6", 4, () -> new RangeSolution(2, 6).length());
        Check.equal("ćw. 3 (wzorzec)", List.of("8.00", "10.00", "6.28"),
                () -> List.of(solution3(new Square(2)), solution3(new Rectangle(2, 3)), solution3(new Circle(1))));
        Check.equal("ćw. 4 (wzorzec): 1*7 + 0", new Num(7),
                () -> solution4(new Add(new Mul(new Num(1), new Num(7)), new Num(0))));
        Check.equal("ćw. 4 (wzorzec): (2+0) * 0", new Num(0),
                () -> solution4(new Mul(new Add(new Num(2), new Num(0)), new Num(0))));
        Check.equal("ćw. 4 (wzorzec): bez zmian 2+3", new Add(new Num(2), new Num(3)),
                () -> solution4(new Add(new Num(2), new Num(3))));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 7 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ na instanceof ze wzorcem. Zwróć „tekst o długości N” dla napisu,
     * „liczba N” dla Integer, a dla reszty „inne”.
     * <pre>{@code
     * // PRZED:
     * if (o instanceof String) { String s = (String) o; return "tekst o długości " + s.length(); }
     * else if (o instanceof Integer) { Integer i = (Integer) o; return "liczba " + i; }
     * else { return "inne"; }
     * }</pre>
     * Podpowiedź: if (o instanceof String s) { ... } else if (o instanceof Integer i) { ... }
     */
    static String exercise1(Object o) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): obwód kształtu jako tekst z dwoma miejscami po przecinku (Locale.ROOT):
     * kwadrat 4 * bok, prostokąt 2 * (szerokość + wysokość), koło 2 * PI * promień. Użyj łańcucha instanceof
     * z końcowym throw dla nieobsłużonego kształtu.
     * Podpowiedź: String.format(Locale.ROOT, "%.2f", wartość).
     */
    static String exercise3(Shape shape) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): uprość drzewo wyrażeń (rekurencyjnie, najpierw dzieci): 0 + x → x, x + 0 → x,
     * 1 * x → x, x * 1 → x, 0 * x → 0, x * 0 → 0 (Num(0)); Neg i Num zostawiasz (Neg z uproszczonym środkiem).
     * Bez składania stałych — Add(Num(2), Num(3)) zostaje bez zmian.
     * Podpowiedź: instanceof Add a → simplify(a.left()), simplify(a.right()), potem sprawdź, czy któraś strona
     * to Num o wartości 0; wzorzec instanceof Num n && n.value() == 0.
     */
    static Expr exercise4(Expr expr) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(Object o) {
        if (o instanceof String s) {
            return "tekst o długości " + s.length();
        } else if (o instanceof Integer i) {
            return "liczba " + i;
        } else {
            return "inne";
        }
    }

    static String solution3(Shape shape) {
        double perimeter;                                          // perimeter = obwód
        if (shape instanceof Square s) {
            perimeter = 4 * s.side();
        } else if (shape instanceof Rectangle r) {
            perimeter = 2 * (r.width() + r.height());
        } else if (shape instanceof Circle c) {
            perimeter = 2 * Math.PI * c.radius();
        } else {
            throw new IllegalStateException("nieobsłużony kształt: " + shape);
        }
        return String.format(Locale.ROOT, "%.2f", perimeter);
    }

    private static boolean isNum(Expr expr, int value) {           // is num = czy to liczba o wartości
        return expr instanceof Num n && n.value() == value;
    }

    static Expr solution4(Expr expr) {
        if (expr instanceof Add a) {
            Expr left = solution4(a.left());
            Expr right = solution4(a.right());
            if (isNum(left, 0)) {
                return right;
            }
            if (isNum(right, 0)) {
                return left;
            }
            return new Add(left, right);
        } else if (expr instanceof Mul m) {
            Expr left = solution4(m.left());
            Expr right = solution4(m.right());
            if (isNum(left, 0) || isNum(right, 0)) {
                return new Num(0);
            }
            if (isNum(left, 1)) {
                return right;
            }
            if (isNum(right, 1)) {
                return left;
            }
            return new Mul(left, right);
        } else if (expr instanceof Neg g) {
            return new Neg(solution4(g.operand()));
        } else if (expr instanceof Num) {
            return expr;
        }
        throw new IllegalStateException("nieobsłużone wyrażenie: " + expr);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Rekord ma pola final, ale lista pod spodem może być zmienna i współdzielona z kodem zewnętrznym.
     *      Zaradź: w konstruktorze kompaktowym przypisz members = List.copyOf(members).
     *   2. s jest widoczne po prawej stronie && i w gałęzi then. Przy || wzorzec nie musiał pasować (lewa strona
     *      mogła być fałszywa), więc s mogłoby być nieprzypisane — kompilator tego zabrania.
     *   3. false (null nie pasuje do żadnego wzorca typu).
     *   4. B musi być final, sealed albo non-sealed — brak modyfikatora to błąd kompilacji.
     *   5. final — koniec dziedziczenia; sealed — dalsza kontrolowana lista podtypów; non-sealed — dziedziczenie
     *      znów otwarte dla każdego.
     *   6. true P[x=7]
     *   7. Zwrócona wartość domyślna ukryłaby brakujący wariant — program liczyłby dalej na błędnych danych.
     *      Wyjątek ujawnia problem w pierwszym teście, a komunikat wskazuje, jaki wariant pominięto.
     */
    // </editor-fold>
}
