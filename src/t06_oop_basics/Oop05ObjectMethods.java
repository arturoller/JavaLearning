package t06_oop_basics;

import helpers.Check;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: toString, equals, hashCode — trzy metody, które KAŻDA klasa dostaje od klasy Object
 *        (override = nadpisać, equals = równa się, hash code = kod skrótu, contract = kontrakt/umowa)
 *
 * W SKRÓCIE:
 *   Każda klasa w Javie ma „w genach” metody z klasy Object. Domyślnie toString zwraca nazwę klasy
 *   i dziwny numer, a equals działa jak == (czy to TEN SAM obiekt?). Gdy obiekty mają być równe
 *   po ZAWARTOŚCI (dwa punkty (1, 2) to „ten sam punkt”), nadpisujemy equals i ZAWSZE razem z nim hashCode.
 *
 * ANALOGIA:
 *   Dwa banknoty 100 zł. == pyta: „czy to TEN SAM kawałek papieru?” (nie — mają różne numery seryjne).
 *   equals pyta: „czy są tyle samo warte?” (tak). hashCode to numer szuflady w kasie: banknoty o tej
 *   samej wartości MUSZĄ trafić do tej samej szuflady, inaczej kasjer ich nie znajdzie.
 *
 * JAK TO DZIAŁA:
 *   class Object {                       ← „przodek” każdej klasy (dziedziczenie: t07)
 *       String  toString()  → "pakiet.Klasa@1b6d3586"  (nazwa + hashCode szesnastkowo)
 *       boolean equals(Object o) → this == o          (czy TEN SAM obiekt)
 *       int     hashCode()  → liczba związana z obiektem (inna przy każdym uruchomieniu)
 *   }
 *   Nadpisujemy (override) je, gdy klasa opisuje WARTOŚĆ: Point, Money, Email...
 *
 *   KONTRAKT equals:                         KONTRAKT hashCode:
 *     x.equals(x) → true   (zwrotność)         x.equals(y) → x.hashCode() == y.hashCode()  (MUSI!)
 *     x.equals(y) == y.equals(x) (symetria)    równe hashCode NIE oznacza równych obiektów (kolizja)
 *     x=y i y=z → x=z   (przechodniość)        bez zmian pól → zawsze ta sama liczba
 *     bez zmian pól → ten sam wynik (spójność)
 *     x.equals(null) → false
 *
 * SŁÓWKA:
 *   override = nadpisać; equals = równa się; hash code = kod skrótu (liczba „odcisk palca”);
 *   contract = kontrakt (umowa); reflexive = zwrotny; symmetric = symetryczny; transitive = przechodni;
 *   consistent = spójny; collision = kolizja; bucket = kubełek (szuflada); null-safe = odporny na null
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop06Immutability (niezmienne obiekty = bezpieczne klucze),
 *             t06_oop_basics/Oop09ValueObjects (klasy-wartości z equals/hashCode),
 *             t09_records/Records01Basics (rekord generuje te trzy metody sam),
 *             t12_collections/Collections04Sets (HashSet dokładnie),
 *             t12_collections/Collections11HashingInternals (jak HashMap używa hashCode w środku)
 * </pre>
 */
public class Oop05ObjectMethods {

    public static void main(String[] args) {
        title("Oop05 — toString, equals, hashCode");

        defaultToString();          // default toString = domyślny toString
        overrideAnnotation();       // override annotation = adnotacja @Override
        referenceVsValue();         // reference vs value = referencja kontra wartość
        equalsStepByStep();         // equals step by step = equals krok po kroku
        hashCodeContract();         // hashCode contract = kontrakt hashCode
        nullSafeHelpers();          // null-safe helpers = pomocnicy odporni na null
        getClassVsInstanceof();     // getClass vs instanceof = getClass kontra instanceof
        missingHashCodePitfall();   // missing hashCode pitfall = pułapka braku hashCode
        mutableKeyPitfall();        // mutable key pitfall = pułapka zmiennego klucza
        exercises();                // exercises = ćwiczenia
    }

    // Klasy przykładowe są zagnieżdżone (static nested — szczegóły w Oop07), żeby cała lekcja była
    // w jednym pliku. W prawdziwym projekcie każda z nich leżałaby w osobnym pliku .java.

    // =================================================================================================
    // 1. DOMYŚLNY toString I JEGO NADPISANIE
    // =================================================================================================

    /** Pies BEZ własnego toString — dostaje wersję z Object. */
    static class Dog {                          // Dog = pies
        private final String name;              // name = imię

        Dog(String name) {
            this.name = name;
        }
    }

    /** Kot Z własnym toString. */
    static class Cat {                          // Cat = kot
        private final String name;
        private final int age;                  // age = wiek

        Cat(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override                               // Override = nadpisuję (metodę odziedziczoną po Object)
        public String toString() {              // toString = na tekst
            return "Cat{name='" + name + "', age=" + age + "}";
        }
    }

    /**
     * 1. Domyślny toString z Object zwraca „nazwaKlasy@liczba” — liczba zmienia się między uruchomieniami
     * i nic nie mówi o zawartości. Nadpisujemy go, żeby println i konkatenacja pokazywały dane obiektu.
     */
    static void defaultToString() {
        section("1. Domyślny toString i jego nadpisanie");

        Dog rex = new Dog("Reks");
        String raw = rex.toString();
        // System.out.println(rex);   → np. t06_oop_basics.Oop05ObjectMethods$Dog@1b6d3586
        //                              liczba po @ jest INNA przy każdym uruchomieniu — dlatego jej nie drukujemy
        show("część przed @", raw.substring(0, raw.indexOf('@')));   // substring = fragment; indexOf = pozycja
        // WYNIK: część przed @ → t06_oop_basics.Oop05ObjectMethods$Dog
        show("prosta nazwa klasy", rex.getClass().getSimpleName());  // getClass = pobierz klasę; getSimpleName = prosta nazwa
        // WYNIK: prosta nazwa klasy → Dog
        note("$ w nazwie oznacza klasę zagnieżdżoną: Dog siedzi wewnątrz Oop05ObjectMethods");
        // WYNIK: ℹ $ w nazwie oznacza klasę zagnieżdżoną: Dog siedzi wewnątrz Oop05ObjectMethods

        Cat mruczek = new Cat("Mruczek", 3);
        show("kot", mruczek);                           // show sam woła toString()
        // WYNIK: kot → Cat{name='Mruczek', age=3}
        System.out.println(mruczek);                    // println(Object) też woła toString()
        // WYNIK: Cat{name='Mruczek', age=3}
        String text = "Mój kot: " + mruczek;           // konkatenacja (+) też woła toString()
        System.out.println(text);
        // WYNIK: Mój kot: Cat{name='Mruczek', age=3}

        Cat nobody = null;                              // nobody = nikt
        System.out.println("Kot sąsiada: " + nobody);   // null w konkatenacji → tekst "null", bez wyjątku
        // WYNIK: Kot sąsiada: null

        // PUŁAPKA: jawne nobody.toString() to już NullPointerException — na null nie wywołasz żadnej metody.
        expectThrows("toString() na null", () -> describe(nobody));
        // WYNIK: ✔ toString() na null → rzucono NullPointerException: Cannot invoke "t06_oop_basics.Oop05ObjectMethods$Cat.toString()" because "cat" is null

        // DOBRA PRAKTYKA: toString służy do logów i debugowania. Pokaż w nim najważniejsze pola,
        // ale NIGDY haseł ani numerów kart. IntelliJ wygeneruje go za Ciebie: Alt+Insert → toString().
    }

    /** Pomocnik: wywołuje toString na przekazanym kocie (nazwa parametru trafia do komunikatu NPE). */
    static String describe(Cat cat) {           // describe = opisz
        return cat.toString();
    }

    // =================================================================================================
    // 2. ADNOTACJA @Override — STRAŻNIK LITERÓWEK
    // =================================================================================================

    /** Papuga z literówką w nazwie metody — i bez @Override, więc kompilator milczy. */
    static class Parrot {                       // Parrot = papuga
        private final String word = "Kra!";     // word = słowo

        // Literówka: "tostring" z małym s. Bez @Override kompilator uznaje to za NOWĄ, osobną metodę.
        public String tostring() {
            return "Papuga mówi " + word;
        }
    }

    /**
     * 2. {@code @Override} mówi kompilatorowi: „ta metoda MA nadpisywać metodę rodzica”. Jeśli nic nie nadpisuje
     * (literówka, zły typ parametru), dostajemy błąd kompilacji zamiast cichego błędu w działaniu.
     */
    static void overrideAnnotation() {
        section("2. @Override — strażnik literówek");

        Parrot polly = new Parrot();
        show("nasza metoda tostring()", polly.tostring());
        // WYNIK: nasza metoda tostring() → Papuga mówi Kra!
        show("czy toString() użył naszego tekstu?", polly.toString().startsWith("Papuga"));   // startsWith = zaczyna się od
        // WYNIK: czy toString() użył naszego tekstu? → false

        // Gdyby nad tostring() stało @Override:
        //     @Override
        //     public String tostring() { ... }   → błąd kompilacji: metoda niczego nie nadpisuje
        //                                          (method does not override ... a method from a supertype)
        //
        // PUŁAPKA: nadpisywana metoda nie może mieć „węższego” dostępu niż w Object:
        //     @Override String toString() { ... }  → błąd kompilacji: attempting to assign weaker access
        //                                             privileges; was public (brak słowa public)

        // DOBRA PRAKTYKA: pisz @Override nad KAŻDĄ nadpisywaną metodą. Kosztuje jedną linijkę, a łapie
        // literówki i złe typy parametrów już podczas kompilacji.
    }

    // =================================================================================================
    // 3. == KONTRA equals — REFERENCJA KONTRA WARTOŚĆ
    // =================================================================================================

    /** Moneta BEZ equals — porównanie działa jak ==. */
    static class Coin {                         // Coin = moneta
        private final int value;                // value = wartość

        Coin(int value) {
            this.value = value;
        }
    }

    /**
     * 3. == porównuje REFERENCJE (czy dwie strzałki wskazują ten sam obiekt). Domyślne equals z Object
     * robi dokładnie to samo — dopóki go nie nadpiszemy, dwa „identyczne” obiekty są dla Javy różne.
     */
    static void referenceVsValue() {
        section("3. == kontra equals");

        Coin a = new Coin(5);
        Coin b = new Coin(5);
        Coin c = a;
        //   STOS (zmienne)          STERTA (obiekty)
        //   a ──────────────┬────▶ Coin{value=5}   ← obiekt nr 1
        //   c ──────────────┘
        //   b ────────────────────▶ Coin{value=5}   ← obiekt nr 2 (osobny, choć „taki sam”)
        show("a == b  (dwa osobne obiekty)", a == b);
        // WYNIK: a == b  (dwa osobne obiekty) → false
        show("a == c  (ta sama referencja)", a == c);
        // WYNIK: a == c  (ta sama referencja) → true
        show("a.equals(b)  (equals z Object)", a.equals(b));
        // WYNIK: a.equals(b)  (equals z Object) → false

        // String ma equals nadpisane przez twórców Javy — dlatego porównujemy teksty przez equals:
        String s1 = new String("java");
        show("new String(\"java\") == \"java\"", s1 == "java");
        // WYNIK: new String("java") == "java" → false
        show("new String(\"java\").equals(\"java\")", s1.equals("java"));
        // WYNIK: new String("java").equals("java") → true

        // DOBRA PRAKTYKA: == tylko dla prymitywów (int, char...), enumów i pytania „czy to TEN SAM obiekt?”.
        // Do porównania ZAWARTOŚCI obiektów zawsze equals.
    }

    // =================================================================================================
    // 4. equals KROK PO KROKU
    // =================================================================================================

    /** Wzorcowy punkt: equals + hashCode + toString. Będzie używany w dalszych sekcjach. */
    static class Point {                        // Point = punkt
        private final int x;
        private final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {       // parametr MUSI być typu Object (nie Point!)
            // Jedna instrukcja po if, więc bez klamer — dokładnie tak generuje to IntelliJ.
            if (this == o) return true;         // KROK 1: ten sam obiekt → na pewno równe (i szybko)
            if (o == null || getClass() != o.getClass()) return false;   // KROK 2: null albo inna klasa
            Point other = (Point) o;            // KROK 3: rzutowanie — po kroku 2 bezpieczne
            return x == other.x && y == other.y; // KROK 4: porównaj pola, które tworzą „wartość”
        }

        @Override
        public int hashCode() {                 // KROK 5: hashCode z TYCH SAMYCH pól co equals
            return Objects.hash(x, y);          // Objects.hash = policz hash z kilku wartości
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    /** Plakietka z BŁĘDNYM equals: parametr typu Badge zamiast Object. */
    static class Badge {                        // Badge = plakietka, identyfikator
        private final String code;              // code = kod

        Badge(String code) {
            this.code = code;
        }

        // BŁĄD: to PRZECIĄŻENIE (overload), a nie nadpisanie — equals(Object) z klasy Object nadal działa jak ==.
        public boolean equals(Badge other) {
            return other != null && code.equals(other.code);
        }
    }

    /**
     * 4. Pięć kroków dobrego equals oraz sprawdzenie wszystkich punktów kontraktu na przykładzie Point.
     * Na końcu klasyczna pułapka: {@code equals(Badge other)} zamiast {@code equals(Object o)}.
     */
    static void equalsStepByStep() {
        section("4. equals krok po kroku");

        Point p1 = new Point(3, 4);
        Point p2 = new Point(3, 4);
        Point p3 = new Point(3, 4);
        show("zwrotność: p1.equals(p1)", p1.equals(p1));
        // WYNIK: zwrotność: p1.equals(p1) → true
        show("symetria: p1.equals(p2), p2.equals(p1)", p1.equals(p2) + ", " + p2.equals(p1));
        // WYNIK: symetria: p1.equals(p2), p2.equals(p1) → true, true
        show("przechodniość: p1=p2 i p2=p3, więc p1.equals(p3)", p1.equals(p3));
        // WYNIK: przechodniość: p1=p2 i p2=p3, więc p1.equals(p3) → true
        show("spójność: trzy wywołania", p1.equals(p2) + " " + p1.equals(p2) + " " + p1.equals(p2));
        // WYNIK: spójność: trzy wywołania → true true true
        show("p1.equals(null)", p1.equals(null));
        // WYNIK: p1.equals(null) → false
        show("p1.equals(\"(3, 4)\")  (inna klasa)", p1.equals("(3, 4)"));
        // WYNIK: p1.equals("(3, 4)")  (inna klasa) → false

        // Jak porównywać pola w KROKU 4:
        //   int, long, char, boolean → ==
        //   double                   → Double.compare(a, b) == 0   (bo NaN i -0.0 — t01_basics/Basics08FloatingPoint)
        //   obiekty (String...)      → Objects.equals(a, b)        (bezpieczne dla null — sekcja 6)
        //   tablice                  → Arrays.equals(a, b)         (t03_arrays/Arrays03Utility)

        // PUŁAPKA: equals(Badge) zamiast equals(Object). Kompilator wybiera metodę po typie ZMIENNEJ.
        Badge b1 = new Badge("X-1");
        Badge b2 = new Badge("X-1");
        Object b2AsObject = b2;                 // ten sam obiekt, ale zmienna typu Object
        show("b1.equals(b2)          — zmienna typu Badge", b1.equals(b2));
        // WYNIK: b1.equals(b2)          — zmienna typu Badge → true
        show("b1.equals(b2AsObject)  — zmienna typu Object", b1.equals(b2AsObject));
        // WYNIK: b1.equals(b2AsObject)  — zmienna typu Object → false
        note("HashSet, HashMap i Check zawsze wołają equals(Object) — nasza metoda jest dla nich niewidzialna");
        // WYNIK: ℹ HashSet, HashMap i Check zawsze wołają equals(Object) — nasza metoda jest dla nich niewidzialna

        // DOBRA PRAKTYKA: generuj equals/hashCode w IDE (Alt+Insert → equals() and hashCode()) i ZAWSZE
        // z @Override — wtedy equals(Badge) dałoby błąd kompilacji. Od Java 16 kroki 2–3 można skrócić:
        //     if (!(o instanceof Point other)) return false;   (Java 16+, pattern matching — t23)
    }

    // =================================================================================================
    // 5. KONTRAKT hashCode
    // =================================================================================================

    /**
     * 5. Równe obiekty MUSZĄ mieć równe hashCode. W drugą stronę nie działa: różne obiekty mogą mieć
     * ten sam hashCode (kolizja) — to legalne, tylko trochę spowalnia HashSet.
     */
    static void hashCodeContract() {
        section("5. Kontrakt hashCode");

        Point p = new Point(3, 4);
        Point q = new Point(3, 4);
        show("p.hashCode()", p.hashCode());
        // WYNIK: p.hashCode() → 1058
        show("q.hashCode()", q.hashCode());
        // WYNIK: q.hashCode() → 1058
        // Objects.hash(3, 4) liczy:  wynik = 1  →  31·1 + 3 = 34  →  31·34 + 4 = 1058

        Point r = new Point(0, 31);
        Point s = new Point(1, 0);
        show("(0, 31).hashCode() i (1, 0).hashCode()", r.hashCode() + " i " + s.hashCode());
        // WYNIK: (0, 31).hashCode() i (1, 0).hashCode() → 992 i 992
        show("(0, 31).equals((1, 0))", r.equals(s));
        // WYNIK: (0, 31).equals((1, 0)) → false
        show("\"Aa\".hashCode() i \"BB\".hashCode()", "Aa".hashCode() + " i " + "BB".hashCode());
        // WYNIK: "Aa".hashCode() i "BB".hashCode() → 2112 i 2112

        // JAK HashSet SZUKA ELEMENTU (podgląd — t12_collections/Collections11HashingInternals):
        //   1) hashCode() → numer kubełka (szuflady)       ← szybko zawęża poszukiwania
        //   2) w tym kubełku: equals() z każdym elementem   ← dokładne sprawdzenie
        //   Różne hashCode → HashSet nawet NIE zawoła equals (szuka w innej szufladzie).
        //
        //   a.equals(b) == true   →  a.hashCode() == b.hashCode()   OBOWIĄZEK
        //   a.hashCode() == b.hashCode()  →  nic nie wiadomo      (kolizja, np. "Aa" i "BB")
        //   a.hashCode() != b.hashCode()  →  na pewno !a.equals(b)

        // PUŁAPKA: "return 42;" w hashCode jest LEGALNE (kontrakt spełniony), ale wszystkie obiekty lądują
        // w jednej szufladzie i HashSet zamienia się w wolną listę.
        // DOBRA PRAKTYKA: hashCode liczymy z DOKŁADNIE tych pól, które porównuje equals — ani jednego więcej.
    }

    // =================================================================================================
    // 6. Objects.equals / Objects.hash — POLA, KTÓRE MOGĄ BYĆ null
    // =================================================================================================

    /** Osoba, której e-mail może być null — equals odporne na null dzięki Objects.equals. */
    static class Person {                       // Person = osoba
        private final String name;
        private final String email;             // email może być null (ktoś go nie podał)

        Person(String name, String email) {
            this.name = name;
            this.email = email;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person other = (Person) o;
            return Objects.equals(name, other.name)          // Objects.equals = czy równe (null-safe)
                    && Objects.equals(email, other.email);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, email);                // null liczy się jako 0 — bez wyjątku
        }

        @Override
        public String toString() {
            return name + " <" + Objects.toString(email, "brak e-maila") + ">";   // Objects.toString = tekst albo zastępnik
        }
    }

    /** Porównanie pola tak, jak robi to naiwne equals: {@code email.equals(otherEmail)}. */
    static boolean naiveCompare(String email, String otherEmail) {   // naive compare = naiwne porównanie
        return email.equals(otherEmail);                              // ← wybuchnie, gdy email == null
    }

    /**
     * 6. Klasa narzędziowa {@code java.util.Objects} ma wersje metod odporne na null:
     * {@code Objects.equals(a, b)}, {@code Objects.hash(...)}, {@code Objects.toString(o, "zastępnik")}.
     */
    static void nullSafeHelpers() {
        section("6. Objects.equals / Objects.hash — pola z null");

        Person ola1 = new Person("Ola", null);
        Person ola2 = new Person("Ola", null);
        show("ola1", ola1);
        // WYNIK: ola1 → Ola <brak e-maila>
        show("ola1.equals(ola2)", ola1.equals(ola2));
        // WYNIK: ola1.equals(ola2) → true
        show("równe hashCode?", ola1.hashCode() == ola2.hashCode());
        // WYNIK: równe hashCode? → true
        show("Objects.equals(null, null)", Objects.equals(null, null));
        // WYNIK: Objects.equals(null, null) → true
        show("Objects.equals(\"a\", null)", Objects.equals("a", null));
        // WYNIK: Objects.equals("a", null) → false

        // PUŁAPKA: gdyby Person.equals miało „return email.equals(other.email);”, to przy e-mailu null wybuchnie.
        expectThrows("naiwne email.equals(...) przy null", () -> naiveCompare(null, null));
        // WYNIK: ✔ naiwne email.equals(...) przy null → rzucono NullPointerException: Cannot invoke "String.equals(Object)" because "email" is null

        // DOBRA PRAKTYKA: w equals porównuj pola-obiekty przez Objects.equals(a, b) — działa zawsze,
        // także gdy któreś (albo oba) są null.
    }

    // =================================================================================================
    // 7. getClass() KONTRA instanceof W equals (krótko)
    // =================================================================================================

    /** Punkt, którego equals używa instanceof (akceptuje też podklasy). */
    static class InstPoint {                    // Inst = od instanceof
        private final int x;
        private final int y;

        InstPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof InstPoint)) return false;   // instanceof = czy jest egzemplarzem (także PODKLASY!)
            InstPoint other = (InstPoint) o;
            return x == other.x && y == other.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    /** Kolorowy punkt — podklasa (extends = rozszerza; dziedziczenie: t07). */
    static class ColorInstPoint extends InstPoint {
        private final String color;             // color = kolor

        ColorInstPoint(int x, int y, String color) {
            super(x, y);                        // super(...) = konstruktor klasy nadrzędnej
            this.color = color;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ColorInstPoint)) return false;
            return super.equals(o) && color.equals(((ColorInstPoint) o).color);
        }
    }

    /** Kolorowy punkt dziedziczący po Point, którego equals używa getClass(). */
    static class ColorPoint extends Point {
        private final String color;

        ColorPoint(int x, int y, String color) {
            super(x, y);
            this.color = color;
        }

        @Override
        public boolean equals(Object o) {
            if (!super.equals(o)) return false; // super.equals sprawdza też getClass() — obie strony równo
            return color.equals(((ColorPoint) o).color);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), color);
        }
    }

    /**
     * 7. instanceof w equals przepuszcza podklasy — i wtedy łatwo złamać SYMETRIĘ. getClass() wymaga
     * identycznej klasy po obu stronach. Dziedziczenie poznasz w t07 — tu tylko skutek.
     */
    static void getClassVsInstanceof() {
        section("7. getClass() kontra instanceof");

        InstPoint p = new InstPoint(1, 2);
        ColorInstPoint cp = new ColorInstPoint(1, 2, "czerwony");
        show("instanceof: p.equals(cp)", p.equals(cp));
        // WYNIK: instanceof: p.equals(cp) → true
        show("instanceof: cp.equals(p)", cp.equals(p));
        // WYNIK: instanceof: cp.equals(p) → false
        note("symetria złamana: p równa się cp, ale cp nie równa się p");
        // WYNIK: ℹ symetria złamana: p równa się cp, ale cp nie równa się p

        Point plain = new Point(1, 2);
        ColorPoint red = new ColorPoint(1, 2, "czerwony");
        show("getClass: plain.equals(red)", plain.equals(red));
        // WYNIK: getClass: plain.equals(red) → false
        show("getClass: red.equals(plain)", red.equals(plain));
        // WYNIK: getClass: red.equals(plain) → false

        // DOBRA PRAKTYKA: w tym kursie używamy getClass(). instanceof jest w porządku, gdy klasa jest
        // final (nie ma podklas — Oop06) — tak robią rekordy (t09_records/Records01Basics).
    }

    // =================================================================================================
    // 8. PUŁAPKA: equals BEZ hashCode → „duplikaty” w HashSet
    // =================================================================================================

    // overrides = nadpisuje. Kompilator z -Xlint ostrzega „equals bez hashCode” — i ma rację! Wyciszamy
    // to ostrzeżenie TYLKO dlatego, że ta klasa jest celowo zepsutym przykładem.
    @SuppressWarnings("overrides")
    static class NoHashPoint {
        private final int x;
        private final int y;

        NoHashPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NoHashPoint other = (NoHashPoint) o;
            return x == other.x && y == other.y;
        }
        // brak hashCode() → zostaje wersja z Object: każdy obiekt ma „swoją” liczbę
    }

    /**
     * 8. HashSet (zbiór bez duplikatów — dokładnie w t12_collections/Collections04Sets) najpierw patrzy
     * na hashCode. Bez nadpisanego hashCode dwa „równe” punkty trafiają do różnych szuflad.
     */
    static void missingHashCodePitfall() {
        section("8. equals bez hashCode → duplikaty w HashSet");

        Set<NoHashPoint> broken = new HashSet<>();   // Set = zbiór; HashSet = zbiór oparty na hashCode
        broken.add(new NoHashPoint(1, 2));           // add = dodaj
        broken.add(new NoHashPoint(1, 2));
        show("equals mówi „równe”", new NoHashPoint(1, 2).equals(new NoHashPoint(1, 2)));
        // WYNIK: equals mówi „równe” → true
        show("rozmiar zbioru (miał być 1!)", broken.size());                     // size = rozmiar
        // WYNIK: rozmiar zbioru (miał być 1!) → 2
        show("contains(new NoHashPoint(1, 2))", broken.contains(new NoHashPoint(1, 2)));   // contains = zawiera
        // WYNIK: contains(new NoHashPoint(1, 2)) → false

        Set<Point> fine = new HashSet<>();
        fine.add(new Point(1, 2));
        fine.add(new Point(1, 2));
        show("Point (equals + hashCode): rozmiar", fine.size());
        // WYNIK: Point (equals + hashCode): rozmiar → 1
        show("Point: contains(new Point(1, 2))", fine.contains(new Point(1, 2)));
        // WYNIK: Point: contains(new Point(1, 2)) → true

        // DOBRA PRAKTYKA: equals i hashCode to PARA — nadpisujesz jedno, nadpisujesz drugie.
    }

    // =================================================================================================
    // 9. PUŁAPKA: ZMIENNE POLE W hashCode → OBIEKT „ZGUBIONY” W ZBIORZE
    // =================================================================================================

    /** Punkt ze setterem — jego hashCode zmienia się razem z polem x. */
    static class MutablePoint {                 // mutable = zmienny
        private int x;
        private final int y;

        MutablePoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void setX(int x) {                      // setX = ustaw x
            this.x = x;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MutablePoint other = (MutablePoint) o;
            return x == other.x && y == other.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    /**
     * 9. HashSet zapamiętuje hashCode w chwili dodania. Gdy potem zmienisz pole użyte w hashCode,
     * obiekt nadal leży w starej szufladzie, a zbiór szuka go w nowej — i nie znajduje.
     */
    static void mutableKeyPitfall() {
        section("9. Zmienne pole w hashCode → zgubiony obiekt");

        Set<MutablePoint> set = new HashSet<>();
        MutablePoint m = new MutablePoint(1, 1);
        set.add(m);
        show("contains(m) przed zmianą", set.contains(m));
        // WYNIK: contains(m) przed zmianą → true

        m.setX(99);                             // zmieniamy pole, z którego liczony jest hashCode
        show("contains(m) po zmianie x", set.contains(m));
        // WYNIK: contains(m) po zmianie x → false
        show("remove(m) — czy usunięto?", set.remove(m));   // remove = usuń
        // WYNIK: remove(m) — czy usunięto? → false
        show("rozmiar zbioru", set.size());
        // WYNIK: rozmiar zbioru → 1

        boolean stillInside = false;            // still inside = nadal w środku
        for (MutablePoint each : set) {         // pętla for-each działa też na zbiorze
            if (each == m) {
                stillInside = true;
            }
        }
        show("ale pętla go znajduje (each == m)", stillInside);
        // WYNIK: ale pętla go znajduje (each == m) → true

        // DOBRA PRAKTYKA: obiekty wkładane do HashSet (i klucze HashMap) powinny być NIEZMIENNE
        // (final pola, brak setterów) — dokładnie o tym jest następna lekcja: Oop06Immutability.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • toString: nadpisz, by println/konkatenacja/logi pokazywały dane; domyślny = Klasa@hash (losowy).
     *   • @Override nad każdą nadpisywaną metodą — łapie literówki (tostring) i złe parametry (equals(Point)).
     *   • == porównuje referencje; equals porównuje zawartość (o ile go nadpisano).
     *   • equals w 5 krokach: this == o → true; null lub inna getClass() → false; rzutuj;
     *     porównaj pola (== dla prymitywów, Objects.equals dla obiektów); hashCode z tych samych pól.
     *   • Kontrakt: zwrotność, symetria, przechodniość, spójność, equals(null) == false.
     *   • Równe obiekty → równe hashCode (MUSI). Równe hashCode → nic nie wiadomo (kolizja).
     *   • Objects.equals / Objects.hash / Objects.toString(o, "zastępnik") — odporne na null.
     *   • equals bez hashCode → duplikaty w HashSet; zmienne pole w hashCode → obiekt zgubiony w zbiorze.
     *   • IntelliJ: Alt+Insert → toString(), equals() and hashCode(). Rekordy (t09) generują je same.
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  System.out.println(new Dog("Reks"));  (Dog bez toString) — i czy wynik
     *      będzie taki sam przy następnym uruchomieniu?
     *   2. Po co pisać @Override, skoro kod bez niego też się kompiluje?
     *   3. ZNAJDŹ BŁĄD:  public boolean equals(Point other) { return x == other.x && y == other.y; }
     *   4. Co wypisze:  Set<NoHashPoint> s = new HashSet<>(); s.add(new NoHashPoint(0, 0));
     *                   s.add(new NoHashPoint(0, 0)); System.out.println(s.size());
     *   5. Dwa obiekty mają ten sam hashCode. Czy equals musi zwrócić true?
     *   6. ZNAJDŹ BŁĄD:  equals porównuje tylko pole id, a hashCode() { return Objects.hash(id, name); }
     *   7. Dlaczego w equals często używa się getClass() zamiast instanceof?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: toString sali", "A-101", () -> new Room("A", 101).toString());
        Check.equal("ćw. 1: te same sale", true, () -> new Room("A", 101).equals(new Room("A", 101)));
        Check.equal("ćw. 1: inny budynek", false, () -> new Room("A", 101).equals(new Room("B", 101)));
        Check.equal("ćw. 1: equals(null)", false, () -> new Room("A", 101).equals(null));
        Check.equal("ćw. 1: równe hashCode", true,
                () -> new Room("A", 101).hashCode() == new Room("A", 101).hashCode());
        Check.equal("ćw. 2: różne punkty", 3, () -> exercise2(samplePoints()));
        Check.equal("ćw. 2: same powtórki", 1,
                () -> exercise2(new Point[]{new Point(5, 5), new Point(5, 5), new Point(5, 5)}));
        Check.equal("ćw. 3: e-mail bez względu na wielkość liter", true,
                () -> new Contact("Jan", "Jan@Example.com").equals(new Contact("Jan K.", "jan@example.com")));
        Check.equal("ćw. 3: spójny hashCode", true,
                () -> new Contact("Jan", "JAN@example.com").hashCode() == new Contact("J.", "jan@EXAMPLE.com").hashCode());
        Check.equal("ćw. 3: HashSet bez duplikatów", 2, () -> distinctCount(
                new Contact("Ala", "ala@example.com"), new Contact("Bob", "bob@example.com"),
                new Contact("Alicja", "ALA@example.com")));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec): toString sali", "A-101", () -> new RoomSolution("A", 101).toString());
        Check.equal("ćw. 1 (wzorzec): te same sale", true,
                () -> new RoomSolution("A", 101).equals(new RoomSolution("A", 101)));
        Check.equal("ćw. 1 (wzorzec): inny budynek", false,
                () -> new RoomSolution("A", 101).equals(new RoomSolution("B", 101)));
        Check.equal("ćw. 1 (wzorzec): equals(null)", false, () -> new RoomSolution("A", 101).equals(null));
        Check.equal("ćw. 1 (wzorzec): równe hashCode", true,
                () -> new RoomSolution("A", 101).hashCode() == new RoomSolution("A", 101).hashCode());
        Check.equal("ćw. 2 (wzorzec): różne punkty", 3, () -> solution2(samplePoints()));
        Check.equal("ćw. 2 (wzorzec): same powtórki", 1,
                () -> solution2(new Point[]{new Point(5, 5), new Point(5, 5), new Point(5, 5)}));
        Check.equal("ćw. 3 (wzorzec): e-mail bez względu na wielkość liter", true,
                () -> new ContactSolution("Jan", "Jan@Example.com").equals(new ContactSolution("Jan K.", "jan@example.com")));
        Check.equal("ćw. 3 (wzorzec): spójny hashCode", true,
                () -> new ContactSolution("Jan", "JAN@example.com").hashCode()
                        == new ContactSolution("J.", "jan@EXAMPLE.com").hashCode());
        Check.equal("ćw. 3 (wzorzec): HashSet bez duplikatów", 2, () -> distinctCount(
                new ContactSolution("Ala", "ala@example.com"), new ContactSolution("Bob", "bob@example.com"),
                new ContactSolution("Alicja", "ALA@example.com")));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 10 OK, ✘ 0 BŁĄD
    }

    /** Dane do ćwiczenia 2: pięć punktów, w tym dwa powtórzone. */
    static Point[] samplePoints() {
        return new Point[]{new Point(1, 2), new Point(3, 4), new Point(1, 2), new Point(0, 0), new Point(3, 4)};
    }

    /** Liczy różne obiekty według ICH equals/hashCode (używane w testach ćwiczenia 3). */
    static int distinctCount(Object... items) {             // distinct count = liczba różnych
        Set<Object> set = new HashSet<>();
        for (Object item : items) {
            set.add(item);
        }
        return set.size();
    }

    /**
     * ĆWICZENIE 1 (łatwe): dokończ trzy metody klasy Room. toString ma zwracać budynek, myślnik i numer
     * ({@code "A-101"}). Dwie sale są równe, gdy mają ten sam budynek (building) i numer (number).
     * Podpowiedź: wzoruj się na klasie Point (5 kroków z sekcji 4); String porównaj przez Objects.equals,
     * int przez ==; hashCode = Objects.hash(building, number).
     */
    static class Room {                         // Room = sala
        private final String building;          // building = budynek
        private final int number;               // number = numer

        Room(String building, int number) {
            this.building = building;
            this.number = number;
        }

        @Override
        public String toString() {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        public boolean equals(Object o) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        public int hashCode() {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ podwójną pętlę na HashSet. Poniższy kod liczy różne punkty,
     * ale ma BŁĄD — porównuje przez == (referencje), więc każdy nowy obiekt uznaje za „inny”:
     * <pre>{@code
     * int distinct = 0;
     * for (int i = 0; i < points.length; i++) {
     *     boolean seenBefore = false;
     *     for (int j = 0; j < i; j++) {
     *         if (points[j] == points[i]) seenBefore = true;
     *     }
     *     if (!seenBefore) distinct++;
     * }
     * return distinct;
     * }</pre>
     * Zwróć liczbę RÓŻNYCH punktów, wkładając je do {@code Set<Point>}.
     * Podpowiedź: Point ma equals i hashCode, więc HashSet sam odrzuci powtórki; wynik to size().
     */
    static int exercise2(Point[] points) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): dokończ klasę Contact. Dwa kontakty są równe, gdy mają ten sam e-mail
     * BEZ WZGLĘDU NA WIELKOŚĆ LITER ("Jan@Example.com" = "jan@example.com"); imię (name) NIE ma znaczenia.
     * hashCode musi być z tym zgodny — równe kontakty MUSZĄ mieć równy hashCode.
     * Podpowiedź: equals → email.equalsIgnoreCase(...); hashCode → email.toLowerCase(Locale.ROOT).hashCode()
     * (Objects.hash(name, email) byłoby BŁĘDEM — dlaczego?).
     */
    static class Contact {                      // Contact = kontakt
        private final String name;
        private final String email;

        Contact(String name, String email) {
            this.name = name;
            this.email = email;
        }

        @Override
        public boolean equals(Object o) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        public int hashCode() {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static class RoomSolution {
        private final String building;
        private final int number;

        RoomSolution(String building, int number) {
            this.building = building;
            this.number = number;
        }

        @Override
        public String toString() {
            return building + "-" + number;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RoomSolution other = (RoomSolution) o;
            return number == other.number && Objects.equals(building, other.building);
        }

        @Override
        public int hashCode() {
            return Objects.hash(building, number);
        }
    }

    static int solution2(Point[] points) {
        Set<Point> unique = new HashSet<>();    // unique = unikalne
        for (Point p : points) {
            unique.add(p);                      // powtórka? equals + hashCode → add nic nie zmienia
        }
        return unique.size();
    }

    static class ContactSolution {
        private final String name;
        private final String email;

        ContactSolution(String name, String email) {
            this.name = name;
            this.email = email;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ContactSolution other = (ContactSolution) o;
            return email.equalsIgnoreCase(other.email);     // equalsIgnoreCase = równe bez względu na wielkość liter
        }

        @Override
        public int hashCode() {
            // Ta sama „normalizacja” co w equals: małe litery. Locale.ROOT — żeby wynik nie zależał od języka
            // systemu (po turecku "I".toLowerCase() to NIE jest "i").
            return email.toLowerCase(Locale.ROOT).hashCode();
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Coś w rodzaju  t06_oop_basics.Oop05ObjectMethods$Dog@1b6d3586  — nazwa klasy, @ i hashCode
     *      szesnastkowo. Liczba po @ zwykle jest INNA przy każdym uruchomieniu (zależy od JVM).
     *   2. Bo bez @Override literówka (tostring) albo zły parametr (equals(Point)) tworzy NOWĄ metodę
     *      zamiast nadpisać starą — kod się kompiluje, ale działa źle. Z @Override to błąd kompilacji.
     *   3. Parametr musi być typu Object. equals(Point) to przeciążenie — HashSet i inne kolekcje wołają
     *      equals(Object), które dalej działa jak ==. Poza tym brak @Override i sprawdzenia null.
     *   4. 2 — NoHashPoint nie ma hashCode, więc dwa „równe” punkty trafiają do różnych kubełków
     *      i HashSet nawet nie woła equals.
     *   5. Nie. Równe hashCode to może być kolizja (np. "Aa" i "BB" → 2112). Obowiązek działa tylko w drugą
     *      stronę: równe obiekty → równe hashCode.
     *   6. Dwa obiekty o tym samym id, a różnym name są równe według equals, ale mają różne hashCode —
     *      kontrakt złamany, HashSet przechowa „duplikaty”. hashCode ma używać tylko pola id.
     *   7. instanceof przepuszcza podklasy; gdy podklasa dołoży pole do equals, łatwo złamać symetrię
     *      (p.equals(cp) == true, ale cp.equals(p) == false). getClass() wymaga identycznej klasy
     *      po obu stronach. Dla klas final (bez podklas) instanceof też jest bezpieczny.
     */
    // </editor-fold>
}
