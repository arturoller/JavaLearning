package t07_inheritance_polymorphism;

import helpers.Check;

import java.util.ArrayList;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Interfejsy — kontrakt zachowania, metody default/static/private
 *        (interface = interfejs; contract = kontrakt; default method = metoda domyślna)
 *
 * W SKRÓCIE:
 *   Interfejs opisuje CO klasa potrafi zrobić (kontrakt), bez mówienia JAK (poza metodami default/static
 *   od Javy 8+). Klasa może implementować (implements) WIELE interfejsów naraz — w przeciwieństwie do
 *   pojedynczego dziedziczenia klas (Inherit01). Dzięki temu niepowiązane klasy mogą dzielić wspólne zachowanie.
 *
 * ANALOGIA: gniazdko elektryczne.
 *   Interfejs to kształt gniazdka (kontrakt: "wsadzisz wtyczkę, dostaniesz prąd"). Nie obchodzi cię, czy
 *   za ścianą jest elektrownia węglowa czy panele słoneczne (implementacja) — liczy się, że pasuje wtyczka.
 *   Jedno urządzenie (klasa) może mieć wtyczkę I złącze USB I gniazdo HDMI naraz — wiele kontraktów.
 *
 * JAK TO DZIAŁA:
 *   interface Flyable { void fly(); }                          ← metoda abstrakcyjna (bez ciała)
 *   interface Swimmable { default void swim() { ... } }         ← metoda domyślna (Java 8+, z ciałem)
 *   class Duck implements Flyable, Swimmable { void fly() {...} }   ← implements WIELU interfejsów naraz
 *
 * SŁÓWKA:
 *   interface = interfejs; implements = implementuje (podpisuje kontrakt); default method = metoda
 *   domyślna (ma ciało, można nadpisać); static method (w interfejsie) = metoda narzędziowa powiązana
 *   z interfejsem, nie z obiektem; private method (w interfejsie) = pomocnicza, tylko do użytku wewnątrz
 *   interfejsu (Java 9+); diamond conflict = konflikt diamentu (dwie metody default o tej samej sygnaturze);
 *   marker interface = interfejs znacznikowy (bez metod, tylko "etykieta typu").
 *
 * ZOBACZ TEŻ: t07_inheritance_polymorphism/Inherit03AbstractClasses (klasa abstrakcyjna kontra interfejs),
 *             t13_lambdas/Lambda02FunctionalInterfaces (interfejs funkcyjny — jedna metoda abstrakcyjna),
 *             t12_collections/Collections07ComparableComparator (Comparable w pełnym rozdziale o kolekcjach).
 * </pre>
 */
public class Inherit04Interfaces {

    // ---------------------------------------------------------------------------------------------
    // Klasy/interfejsy przykładowe jako statyczne składowe zagnieżdżone — lekcja ma być samodzielna.
    // ---------------------------------------------------------------------------------------------

    /** Flyable = potrafi latać. Czysty kontrakt: tylko metoda abstrakcyjna. */
    interface Flyable {
        void fly();   // brak ciała — implementująca klasa MUSI je dostarczyć
    }

    /** Swimmable = potrafi pływać. Ma metodę domyślną (default) — implementująca klasa MOŻE, ale nie musi, nadpisać. */
    interface Swimmable {
        default void swim() {
            System.out.println("    (domyślnie) pluska się w wodzie");
        }
    }

    /** Duck implementuje DWA interfejsy naraz — czego klasa (pojedyncze extends) nigdy by nie mogła zrobić. */
    static class Duck implements Flyable, Swimmable {
        private final String name;

        Duck(String name) {
            this.name = name;
        }

        @Override
        public void fly() {
            System.out.println("    " + name + " leci");
        }
        // swim() NIE jest nadpisane — Duck używa wersji domyślnej ze Swimmable
    }

    /** Robot implementuje Flyable, ale nadpisuje... nic z Swimmable, bo nawet go nie implementuje. */
    static class Robot implements Flyable {
        @Override
        public void fly() {
            System.out.println("    Dron wzbija się w powietrze");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Stałe w interfejsie, metody static i private (Java 9+)
    // ---------------------------------------------------------------------------------------------

    /** PhysicsConstants = stałe fizyczne. Pola w interfejsie są NIEJAWNIE public static final. */
    interface PhysicsConstants {
        double GRAVITY = 9.81;   // to samo co: public static final double GRAVITY = 9.81;
    }

    /** MathHelper = pomocnik matematyczny. Pokazuje metodę static i private (Java 9+) w interfejsie. */
    interface MathHelper {
        static int square(int x) {          // metoda static (Java 8+) — wołana przez NAZWĘ interfejsu
            return validated(x) * validated(x);
        }

        static int cube(int x) {
            return validated(x) * validated(x) * validated(x);
        }

        private static int validated(int x) {   // metoda private (Java 9+) — WSPÓLNY kod dla square/cube,
            if (x < 0) {                          // ale niewidoczna spoza interfejsu (szczegół implementacji)
                throw new IllegalArgumentException("x musi być nieujemne: " + x);
            }
            return x;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Konflikt diamentu: dwie metody default o tej samej sygnaturze
    // ---------------------------------------------------------------------------------------------

    interface Greeter1 {
        default String greet() {
            return "Cześć od Greeter1";
        }
    }

    interface Greeter2 {
        default String greet() {
            return "Cześć od Greeter2";
        }
    }

    /**
     * BilingualGreeter implementuje DWA interfejsy z KONFLIKTUJĄCYMI metodami default o tej samej
     * sygnaturze — Java NIE zgaduje, którą wybrać. Trzeba jawnie nadpisać greet() i samemu zdecydować
     * (tu: wybieramy Greeter1.super.greet(), moglibyśmy równie dobrze złączyć obie wersje).
     */
    static class BilingualGreeter implements Greeter1, Greeter2 {
        @Override
        public String greet() {
            return Greeter1.super.greet();   // jawny wybór: "X.super.metoda()" — inaczej błąd kompilacji
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Comparable — przykład z JDK: interfejs implementowany przez WŁASNĄ klasę, użyty przez List.sort
    // ---------------------------------------------------------------------------------------------

    /** Player = gracz. Implementuje Comparable<Player>, więc obiekty da się sortować "naturalnie" po punktach. */
    static class Player implements Comparable<Player> {
        private final String name;
        private final int score;

        Player(String name, int score) {
            this.name = name;
            this.score = score;
        }

        @Override
        public int compareTo(Player other) {
            return Integer.compare(this.score, other.score);   // rosnąco po wyniku
        }

        @Override
        public String toString() {
            return name + "(" + score + ")";
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Interfejs znacznikowy (marker interface) — bez żadnych metod
    // ---------------------------------------------------------------------------------------------

    /** Auditable = podlega audytowi. Marker interface: nie ma metod — sama przynależność coś oznacza. */
    interface Auditable {
    }

    static class Payment implements Auditable {
        final String id;

        Payment(String id) {
            this.id = id;
        }
    }

    public static void main(String[] args) {
        title("Inherit04 — interfejsy: kontrakt, default/static/private, diament, Comparable");

        contractBasics();        // contract basics = podstawy kontraktu
        multipleInterfaces();    // multiple interfaces = wiele interfejsów naraz
        constantsAndStatic();    // constants and static = stałe i metody statyczne
        diamondConflict();       // diamond conflict = konflikt diamentu
        comparableExample();     // Comparable example = przykład z JDK
        functionalInterfacePreview(); // functional interface preview = zapowiedź interfejsu funkcyjnego
        markerInterface();       // marker interface = interfejs znacznikowy
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. INTERFEJS JAKO KONTRAKT
    // =================================================================================================

    /** 1. Flyable nie mówi NIC o tym, JAK się lata — każda implementacja robi to po swojemu. */
    static void contractBasics() {
        section("1. Interfejs jako kontrakt: CO, nie JAK");

        Flyable duck = new Duck("Kaczor Donald");
        Flyable robot = new Robot();
        duck.fly();
        robot.fly();
        // WYNIK:     Kaczor Donald leci
        // WYNIK:     Dron wzbija się w powietrze

        show("duck instanceof Flyable", duck instanceof Flyable);
        // WYNIK: duck instanceof Flyable → true

        // DOBRA PRAKTYKA: kod, który tylko WOŁA fly(), niech przyjmuje typ Flyable, nie konkretną klasę
        //   (Duck/Robot) — wtedy zadziała z KAŻDĄ obecną i przyszłą implementacją.
    }

    // =================================================================================================
    // 2. JEDNA KLASA, WIELE INTERFEJSÓW
    // =================================================================================================

    /** 2. Duck implementuje Flyable I Swimmable naraz — klasa mogłaby też "extends" jedną klasę bazową do tego. */
    static void multipleInterfaces() {
        section("2. Jedna klasa implementuje wiele interfejsów naraz");

        Duck duck = new Duck("Kaczor");
        duck.fly();
        duck.swim();      // Duck NIE nadpisało swim() — używa wersji domyślnej ze Swimmable
        // WYNIK:     Kaczor leci
        // WYNIK:     (domyślnie) pluska się w wodzie

        show("duck instanceof Flyable", duck instanceof Flyable);
        show("duck instanceof Swimmable", duck instanceof Swimmable);
        // WYNIK: duck instanceof Flyable → true
        // WYNIK: duck instanceof Swimmable → true

        // class Hybryda extends Duck, Robot { }
        //   // BŁĄD KOMPILACJI: nadal działa reguła pojedynczego dziedziczenia KLAS (Inherit01) — ale
        //   // "implements Flyable, Swimmable, Auditable" dowolnej liczby interfejsów jest jak najbardziej OK.

        // DOBRA PRAKTYKA: interfejsy pozwalają "dokładać role" (potrafi latać, potrafi pływać) bez
        //   ograniczeń jednej klasy bazowej — to duża przewaga nad samym dziedziczeniem klas.
    }

    // =================================================================================================
    // 3. STAŁE, METODY static I private W INTERFEJSIE
    // =================================================================================================

    /**
     * 3. Pola w interfejsie są NIEJAWNIE public static final (stałe). Metody static (8+) należą do
     * interfejsu, nie do obiektu — wołamy je przez nazwę interfejsu. Metody private (9+) to szczegóły
     * implementacji, dzielone między metodami static/default tego samego interfejsu, niewidoczne na zewnątrz.
     */
    static void constantsAndStatic() {
        section("3. Stałe i metody static/private w interfejsie");

        show("PhysicsConstants.GRAVITY", PhysicsConstants.GRAVITY);
        show("MathHelper.square(5)", MathHelper.square(5));
        show("MathHelper.cube(3)", MathHelper.cube(3));
        // WYNIK: PhysicsConstants.GRAVITY → 9.81
        // WYNIK: MathHelper.square(5) → 25
        // WYNIK: MathHelper.cube(3) → 27

        expectThrows("MathHelper.square(-1) — walidacja we wspólnej metodzie private", () -> MathHelper.square(-1));
        // WYNIK: ✔ MathHelper.square(-1) — walidacja we wspólnej metodzie private → rzucono IllegalArgumentException: x musi być nieujemne: -1

        // MathHelper.validated(5);
        //   // BŁĄD KOMPILACJI: validated() jest private — widoczna TYLKO wewnątrz MathHelper,
        //   // niedostępna nawet dla klas implementujących ten interfejs.

        // DOBRA PRAKTYKA: metody private w interfejsie (Java 9+) pozwalają uniknąć duplikacji między
        //   kilkoma metodami default/static, bez wystawiania tych szczegółów na zewnątrz.
    }

    // =================================================================================================
    // 4. KONFLIKT DIAMENTU: DWIE METODY default O TEJ SAMEJ SYGNATURZE
    // =================================================================================================

    /**
     * 4. Gdy klasa implementuje dwa interfejsy z METODAMI DEFAULT o identycznej sygnaturze, Java
     * NIE wybiera żadnej automatycznie — wymaga jawnego nadpisania i wskazania (X.super.metoda()),
     * którą wersję chcemy (albo napisania zupełnie nowej logiki).
     */
    static void diamondConflict() {
        section("4. Konflikt diamentu: dwie metody default, ta sama sygnatura");

        // static class Zly implements Greeter1, Greeter2 { }
        //   // BŁĄD KOMPILACJI: class Zly inherits unrelated defaults for greet() from types Greeter1 and Greeter2
        //   // Java nie zgaduje, którą wersję wybrać — MUSISZ nadpisać greet() jawnie (patrz BilingualGreeter).

        BilingualGreeter g = new BilingualGreeter();
        show("g.greet()", g.greet());
        // WYNIK: g.greet() → Cześć od Greeter1

        // DOBRA PRAKTYKA: unikaj sytuacji, w których dwa niepowiązane interfejsy dają tę samą metodę
        //   default o różnym znaczeniu — jeśli się zdarzy, rozstrzygnij jawnie i UZASADNIJ wybór komentarzem.
    }

    // =================================================================================================
    // 5. Comparable — PRZYKŁAD Z JDK
    // =================================================================================================

    /** 5. Player implementuje Comparable<Player> — dzięki temu List.sort(null) użyje compareTo() automatycznie. */
    static void comparableExample() {
        section("5. Comparable — przykład interfejsu z samego JDK");

        List<Player> players = new ArrayList<>(List.of(
                new Player("Ala", 42), new Player("Bartek", 17), new Player("Celina", 99)));
        players.sort(null);   // null = "użyj naturalnego porządku", czyli compareTo() z Comparable
        show("gracze posortowani po wyniku (rosnąco)", players);
        // WYNIK: gracze posortowani po wyniku (rosnąco) → [Bartek(17), Ala(42), Celina(99)]

        // DOBRA PRAKTYKA: Comparable to KLASYCZNY przykład interfejsu — cała biblioteka standardowa
        //   (sortowanie, TreeMap/TreeSet) współpracuje z KAŻDĄ klasą, która "podpisze" ten kontrakt,
        //   bez potrzeby dziedziczenia po jakiejkolwiek konkretnej klasie. Pełny rozdział o porównywaniu
        //   i sortowaniu: t12_collections/Collections07ComparableComparator.
    }

    // =================================================================================================
    // 6. ZAPOWIEDŹ: INTERFEJS FUNKCYJNY
    // =================================================================================================

    /**
     * 6. Interfejs z DOKŁADNIE JEDNĄ metodą abstrakcyjną (jak Flyable) to "interfejs funkcyjny" — można
     * go zaimplementować krótko, przez lambdę, zamiast pisać całą klasę. Pełny rozdział: t13_lambdas.
     */
    static void functionalInterfacePreview() {
        section("6. Zapowiedź: interfejs funkcyjny i lambda (pełny rozdział: t13_lambdas)");

        Flyable lambdaFlyer = () -> System.out.println("    Latający dywan leci (lambda, bez żadnej klasy!)");
        lambdaFlyer.fly();
        // WYNIK:     Latający dywan leci (lambda, bez żadnej klasy!)

        note("Flyable ma DOKŁADNIE jedną metodę abstrakcyjną (fly()) — dlatego lambda '() -> ...' pasuje "
                + "do niego bez pisania osobnej klasy. Szczegóły: t13_lambdas/Lambda02FunctionalInterfaces.");
        // WYNIK: ℹ Flyable ma DOKŁADNIE jedną metodę abstrakcyjną (fly()) — dlatego lambda '() -> ...' pasuje do niego bez pisania osobnej klasy. Szczegóły: t13_lambdas/Lambda02FunctionalInterfaces.
    }

    // =================================================================================================
    // 7. INTERFEJS ZNACZNIKOWY (MARKER INTERFACE)
    // =================================================================================================

    /** 7. Auditable nie ma metod — sama przynależność do niego ("Payment JEST Auditable") niesie informację. */
    static void markerInterface() {
        section("7. Interfejs znacznikowy (marker interface)");

        Payment p = new Payment("PAY-001");
        show("p instanceof Auditable", p instanceof Auditable);
        // WYNIK: p instanceof Auditable → true

        note("Marker interface nie deklaruje ŻADNEJ metody — kod, który sprawdza 'obj instanceof Auditable', "
                + "używa go jako ETYKIETY typu (np. 'ten obiekt trzeba zalogować do audytu').");
        // WYNIK: ℹ Marker interface nie deklaruje ŻADNEJ metody — kod, który sprawdza 'obj instanceof Auditable', używa go jako ETYKIETY typu (np. 'ten obiekt trzeba zalogować do audytu').

        // DOBRA PRAKTYKA: znane przykłady z JDK to Serializable i Cloneable. Dziś do znakowania metadanych
        //   częściej używa się adnotacji (t19_annotations_reflection) — marker interface wciąż bywa
        //   przydatny, gdy chcesz wymusić sprawdzenie typu w czasie KOMPILACJI, nie tylko w czasie działania.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • interface = kontrakt (CO klasa potrafi), implements = klasa PODPISUJE kontrakt.
     *   • Klasa może implementować WIELE interfejsów naraz (w przeciwieństwie do jednej klasy bazowej).
     *   • Pola w interfejsie są niejawnie public static final (stałe).
     *   • Metody default (8+) mają ciało — implementująca klasa może je nadpisać, ale nie musi.
     *   • Metody static (8+) należą do interfejsu, wołane przez jego nazwę; private (9+) to ukryte
     *     szczegóły wspólne dla kilku metod default/static tego samego interfejsu.
     *   • Konflikt diamentu (dwie metody default o tej samej sygnaturze) wymaga jawnego nadpisania
     *     i wyboru przez Interfejs.super.metoda().
     *   • Comparable to klasyczny przykład interfejsu z JDK — pozwala sortować własne klasy.
     *   • Interfejs z jedną metodą abstrakcyjną = interfejs funkcyjny (można go zastąpić lambdą, t13_lambdas).
     *   • Marker interface (bez metod) to etykieta typu sprawdzana przez instanceof.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się interfejs od klasy abstrakcyjnej pod kątem liczby "rodziców"?
     *   2. Co wypisze:  System.out.println(new Duck("X") instanceof Swimmable);  ?
     *   3. ZNAJDŹ BŁĄD:
     *          class Zly implements Greeter1, Greeter2 { }
     *          // (Greeter1 i Greeter2 mają obie metodę default String greet())
     *   4. Dlaczego pola w interfejsie są zawsze i tak public static final, nawet bez tych słów kluczowych?
     *   5. Po co Player implementuje interfejs Comparable, skoro dałoby się po prostu napisać osobną
     *      metodę sortującą, która porównuje pole score?
     *   6. Do czego służy metoda private w interfejsie (Java 9+) i dlaczego nie da się jej wywołać z zewnątrz?
     *   7. Czym różni się marker interface od zwykłego interfejsu z metodami?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");

        List<Flyable> flyers = List.of(new Duck("Kaczor"), new Robot(), new Duck("Kaczka"));
        List<Player> players = new ArrayList<>(List.of(
                new Player("Ala", 42), new Player("Bartek", 17), new Player("Celina", 99)));

        Check.equal("ćw. 1: liczba latających obiektów, które też pływają", 2, () -> countSwimmers(flyers));
        Check.equal("ćw. 2: suma kwadratów 2..4 (MathHelper.square)", 29, () -> sumOfSquares(2, 4));
        Check.equal("ćw. 3: gracz z najwyższym wynikiem (Comparable)", "Celina(99)", () -> topPlayer(players));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 2, () -> solution1(flyers));
        Check.equal("ćw. 2 (wzorzec)", 29, () -> solution2(2, 4));
        Check.equal("ćw. 3 (wzorzec)", "Celina(99)", () -> solution3(players));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz, ile obiektów z listy Flyable jest JEDNOCZEŚNIE Swimmable.
     * Podpowiedź: pętla for-each + `element instanceof Swimmable`.
     */
    static int countSwimmers(List<Flyable> flyers) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (łatwe): zsumuj MathHelper.square(x) dla x od from do to (włącznie).
     * Przykład: from=2, to=4 → square(2)+square(3)+square(4) = 4+9+16 = 29.
     */
    static int sumOfSquares(int from, int to) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć toString() gracza z NAJWYŻSZYM wynikiem, korzystając z compareTo()
     * (interfejsu Comparable), a nie z porównywania pól "na piechotę".
     * Podpowiedź: pętla for-each, `if (p.compareTo(best) > 0) best = p;`.
     */
    static String topPlayer(List<Player> players) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(List<Flyable> flyers) {
        int count = 0;
        for (Flyable f : flyers) {
            if (f instanceof Swimmable) {
                count++;
            }
        }
        return count;
    }

    static int solution2(int from, int to) {
        int sum = 0;
        for (int x = from; x <= to; x++) {
            sum += MathHelper.square(x);
        }
        return sum;
    }

    static String solution3(List<Player> players) {
        Player best = players.get(0);
        for (Player p : players) {
            if (p.compareTo(best) > 0) {
                best = p;
            }
        }
        return best.toString();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Klasa może mieć tylko JEDNĄ klasę bazową (extends), ale implementować DOWOLNIE WIELE
     *      interfejsów (implements) naraz — interfejsy nie podlegają ograniczeniu pojedynczego dziedziczenia.
     *   2. "true" — Duck implements Swimmable (nawet nie nadpisując swim(), nadal JEST Swimmable).
     *   3. Zly dziedziczy DWIE niezgodne metody default greet() (z Greeter1 i z Greeter2) i nie mówi,
     *      której użyć — błąd kompilacji: "class Zly inherits unrelated defaults for greet() from types
     *      Greeter1 and Greeter2". Trzeba nadpisać greet() jawnie, np. wołając Greeter1.super.greet().
     *   4. Bo interfejs opisuje kontrakt, a nie stan obiektu — pole w interfejsie MUSI być tą samą stałą
     *      wartością dla każdej implementującej klasy, więc Java wymusza public static final niejawnie.
     *   5. Comparable pozwala dowolnemu miejscu w JDK (List.sort(null), Collections.sort, TreeSet/TreeMap)
     *      sortować Twoje obiekty BEZ pisania osobnej metody porównującej za każdym razem — jedna metoda
     *      compareTo() obsługuje wszystkie te miejsca naraz.
     *   6. Metoda private w interfejsie to wspólny kod pomocniczy dla kilku metod default/static TEGO
     *      SAMEGO interfejsu (np. wspólna walidacja) — nie jest częścią kontraktu, więc nie jest widoczna
     *      na zewnątrz ani dla klas implementujących.
     *   7. Marker interface nie deklaruje żadnej metody — samo "bycie" tym typem (sprawdzane przez
     *      instanceof) niesie informację. Zwykły interfejs wymusza dostarczenie konkretnego zachowania.
     */
    // </editor-fold>
}
