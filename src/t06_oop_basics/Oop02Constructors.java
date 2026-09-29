package t06_oop_basics;

import helpers.Check;
import java.util.Arrays;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Konstruktory — jak obiekt dostaje stan startowy
 *        (constructor = konstruktor, default = domyślny, overloading = przeciążanie, chaining = łańcuch wywołań)
 *
 * W SKRÓCIE:
 *   Konstruktor to specjalny blok kodu uruchamiany przy new. Ma nazwę klasy i nie ma typu zwracanego.
 *   Jego zadanie: ustawić pola tak, żeby obiekt od pierwszej chwili był POPRAWNY (np. pies ma imię).
 *   Konstruktory można przeciążać i łączyć przez this(...), a złe dane odrzucać wyjątkiem.
 *
 * ANALOGIA:
 *   Konstruktor to montaż mebla w fabryce. Klient dostaje gotową szafę z drzwiami i półkami — nie pudło
 *   luźnych desek. Jeśli brakuje zawiasów (złe dane), fabryka nie wypuszcza szafy wcale (wyjątek),
 *   zamiast wysłać klientowi mebel, który rozpadnie się przy pierwszym otwarciu.
 *
 * JAK TO DZIAŁA:
 *   new Book("Czysty kod", 464)
 *     1. JVM rezerwuje pamięć, pola dostają wartości domyślne (null, 0, false),
 *     2. uruchamiają się inicjalizatory pól i bloki inicjalizacyjne { ... } (w kolejności z pliku),
 *     3. wykonuje się ciało konstruktora (this.title = title; ...),
 *     4. new zwraca referencję do gotowego obiektu.
 *
 *   Reguły:
 *     • brak jakiegokolwiek konstruktora → kompilator dopisuje pusty konstruktor bezparametrowy,
 *     • zadeklarujesz choć jeden → tego „darmowego” już nie ma,
 *     • this(...) = wywołaj inny konstruktor tej klasy; musi być PIERWSZĄ instrukcją.
 *
 * SŁÓWKA:
 *   constructor = konstruktor; default constructor = konstruktor domyślny; parameter = parametr;
 *   overloading = przeciążanie; chaining = łańcuch (łączenie) wywołań; shadowing = zasłanianie;
 *   initializer = inicjalizator; block = blok; copy = kopia; validation = walidacja (sprawdzanie danych);
 *   IllegalArgumentException = wyjątek „niepoprawny argument”
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop01ClassesObjects (obiekty i wartości domyślne),
 *             t05_methods/Methods02Overloading (przeciążanie metod — te same zasady),
 *             t06_oop_basics/Oop03Encapsulation (pilnowanie poprawności także PO utworzeniu obiektu),
 *             t09_records/Records02Constructors (konstruktory rekordów)
 * </pre>
 */
public class Oop02Constructors {

    public static void main(String[] args) {
        title("Oop02 — konstruktory");

        defaultConstructor();       // default constructor = konstruktor domyślny
        parameterizedConstructor(); // parameterized constructor = konstruktor z parametrami
        noArgDisappears();          // no-arg disappears = bezparametrowy znika
        overloadedConstructors();   // overloaded constructors = przeciążone konstruktory
        thisChaining();             // this chaining = łańcuch this(...)
        shadowing();                // shadowing = zasłanianie nazw
        validation();               // validation = walidacja
        initializationOrder();      // initialization order = kolejność inicjalizacji
        copyConstructor();          // copy constructor = konstruktor kopiujący
        exercises();                // exercises = ćwiczenia
    }

    // Klasy przykładowe są statycznymi klasami zagnieżdżonymi, by lekcja była jednym plikiem
    // (w projekcie każda byłaby w osobnym pliku) — wyjaśnienie w t06_oop_basics/Oop01ClassesObjects.

    // =================================================================================================
    // 1. KONSTRUKTOR DOMYŚLNY
    // =================================================================================================

    static class Lamp {                 // lamp = lampa
        boolean on;                     // on = włączona (domyślnie false)
        String color = "biały";         // color = kolor; inicjalizator pola — wartość startowa inna niż null
        // Brak konstruktora → kompilator po cichu dopisuje:  Lamp() { }
    }

    /**
     * 1. Klasa bez konstruktora i tak da się utworzyć przez {@code new Lamp()} — kompilator dodał pusty
     * konstruktor bezparametrowy (default constructor).
     */
    static void defaultConstructor() {
        section("1. Konstruktor domyślny — dopisuje go kompilator");

        Lamp lamp = new Lamp();         // wywołanie niewidocznego konstruktora Lamp()
        show("lamp.on", lamp.on);
        // WYNIK: lamp.on → false
        show("lamp.color", lamp.color);
        // WYNIK: lamp.color → biały

        // JAK TO DZIAŁA: konstruktor domyślny nic nie robi — pola mają wartości domyślne albo te
        // z inicjalizatorów (= "biały"). To wystarcza tylko dla najprostszych klas.
    }

    // =================================================================================================
    // 2. KONSTRUKTOR Z PARAMETRAMI
    // =================================================================================================

    static class Book {                 // book = książka
        String title;                   // title = tytuł
        int pages;                      // pages = strony (liczba stron)

        Book(String title, int pages) { // konstruktor: nazwa klasy, BRAK typu zwracanego (nawet void)
            this.title = title;         // this.title = pole obiektu, title = parametr
            this.pages = pages;
        }

        Book(Book other) {              // konstruktor kopiujący (sekcja 9); other = inna (książka)
            this(other.title, other.pages);
        }

        String describe() {             // describe = opisz
            return title + ", " + pages + " str.";
        }
    }

    /**
     * 2. Konstruktor z parametrami ustawia stan w JEDNYM kroku — nie da się zapomnieć o żadnym polu.
     */
    static void parameterizedConstructor() {
        section("2. Konstruktor z parametrami");

        // PRZED (Oop01): obiekt „pół-gotowy”, pola ustawiane ręcznie — łatwo o jednym zapomnieć.
        //     Book b = new Book();  b.title = "Czysty kod";  // ...a pages? zostaje 0
        // PO: wszystko podane przy tworzeniu.
        Book cleanCode = new Book("Czysty kod", 464);
        show("cleanCode", cleanCode.describe());
        // WYNIK: cleanCode → Czysty kod, 464 str.

        // PUŁAPKA: konstruktor z typem zwracanym to już NIE konstruktor, tylko zwykła metoda!
        //     void Book(String title, int pages) { ... }   // kompiluje się, ale new Book(...) jej nie wywoła
        // DOBRA PRAKTYKA: konstruktor ma tylko nazwę klasy — bez void, bez return z wartością.
    }

    // =================================================================================================
    // 3. BEZPARAMETROWY ZNIKA
    // =================================================================================================

    /**
     * 3. Gdy zadeklarujesz dowolny konstruktor, kompilator przestaje dopisywać ten domyślny.
     */
    static void noArgDisappears() {
        section("3. Konstruktor bezparametrowy znika");

        // Klasa Book ma konstruktory (String, int) i (Book). Dlatego:
        //     Book empty = new Book();
        //     // błąd kompilacji: constructor Book in class Book cannot be applied to given types
        //     //   required: String,int   found: no arguments
        // To zaleta, nie wada: klasa sama decyduje, jakich danych WYMAGA. Pusta książka bez tytułu
        // nie powstanie przez przypadek.
        note("new Book() się nie kompiluje — Book wymaga tytułu i liczby stron");
        // WYNIK: ℹ new Book() się nie kompiluje — Book wymaga tytułu i liczby stron

        // Jeśli pusty konstruktor jest naprawdę potrzebny (np. dla bibliotek typu JSON),
        // trzeba go dopisać samemu:  Book() { this("bez tytułu", 0); }
    }

    // =================================================================================================
    // 4. PRZECIĄŻANIE KONSTRUKTORÓW
    // =================================================================================================

    /** Bilet — trzy konstruktory, ale z powtórzonym kodem (wersja PRZED). */
    static class TicketCopyPaste {      // ticket = bilet; copy-paste = kopiuj-wklej
        String route;                   // route = trasa
        int price;                      // price = cena
        boolean discount;               // discount = zniżka

        TicketCopyPaste(String route) {
            this.route = route;
            this.price = 50;
            this.discount = false;
        }

        TicketCopyPaste(String route, int price) {
            this.route = route;
            this.price = price;
            this.discount = false;
        }

        TicketCopyPaste(String route, int price, boolean discount) {
            this.route = route;
            this.price = price;
            this.discount = discount;
        }

        String describe() {
            return route + " " + price + " zł" + (discount ? " (ulga)" : "");
        }
    }

    /**
     * 4. Kilka konstruktorów o różnych listach parametrów = przeciążanie (jak przy metodach).
     * Kompilator wybiera wersję po liczbie i typach argumentów.
     */
    static void overloadedConstructors() {
        section("4. Przeciążanie konstruktorów");

        show("1 argument", new TicketCopyPaste("Kraków-Warszawa").describe());
        // WYNIK: 1 argument → Kraków-Warszawa 50 zł
        show("2 argumenty", new TicketCopyPaste("Kraków-Gdańsk", 120).describe());
        // WYNIK: 2 argumenty → Kraków-Gdańsk 120 zł
        show("3 argumenty", new TicketCopyPaste("Poznań-Łódź", 60, true).describe());
        // WYNIK: 3 argumenty → Poznań-Łódź 60 zł (ulga)

        // PUŁAPKA: przypisania pól powtórzone w trzech miejscach. Dodasz pole „class” (klasa wagonu)
        // → musisz pamiętać o trzech konstruktorach. Zapomnisz o jednym → pole zostanie null.
        // Rozwiązanie: łańcuch this(...) — następna sekcja.
    }

    // =================================================================================================
    // 5. ŁAŃCUCH THIS(...)
    // =================================================================================================

    /** Pizza — krótsze konstruktory delegują do jednego „głównego”. */
    static class Pizza {
        String name;                    // name = nazwa
        String size;                    // size = rozmiar
        int extraCheese;                // extra cheese = dodatkowy ser (porcje)

        Pizza() {
            this("Margherita");         // this(...) = wywołaj INNY konstruktor tej klasy
        }

        Pizza(String name) {
            this(name, "średnia");
        }

        Pizza(String name, String size) {
            this(name, size, 0);
        }

        Pizza(String name, String size, int extraCheese) {  // główny konstruktor — cała logika w JEDNYM miejscu
            this.name = name;
            this.size = size;
            this.extraCheese = extraCheese;
        }

        String describe() {
            return name + " (" + size + ", ser x" + extraCheese + ")";
        }
    }

    /**
     * 5. Krótsze konstruktory podają wartości domyślne i przekazują pracę dalej przez {@code this(...)}.
     */
    static void thisChaining() {
        section("5. Łańcuch this(...) — jeden główny konstruktor");

        show("new Pizza()", new Pizza().describe());
        // WYNIK: new Pizza() → Margherita (średnia, ser x0)
        show("new Pizza(\"Hawajska\")", new Pizza("Hawajska").describe());
        // WYNIK: new Pizza("Hawajska") → Hawajska (średnia, ser x0)
        show("new Pizza(\"Diavola\", \"duża\", 2)", new Pizza("Diavola", "duża", 2).describe());
        // WYNIK: new Pizza("Diavola", "duża", 2) → Diavola (duża, ser x2)

        // JAK TO DZIAŁA: Pizza() → Pizza("Margherita") → Pizza("Margherita", "średnia")
        //                → Pizza("Margherita", "średnia", 0) — tu dzieje się przypisanie pól.

        // PUŁAPKA: this(...) musi być PIERWSZĄ instrukcją konstruktora:
        //     Pizza(String name) {
        //         System.out.println("tworzę");
        //         this(name, "średnia");      // błąd kompilacji: call to this must be first statement
        //     }
        // DOBRA PRAKTYKA: krótkie konstruktory „w dół” do najpełniejszego; walidację i przypisania
        // trzymaj tylko w tym jednym.
    }

    // =================================================================================================
    // 6. ZASŁANIANIE: THIS.NAME = NAME
    // =================================================================================================

    static class BuggyUser {            // buggy user = użytkownik z błędem
        String name;

        BuggyUser(String name) {
            name = name;                // PUŁAPKA: parametr przypisany... sam do siebie! Pole zostaje null.
        }
    }

    static class User {                 // user = użytkownik
        String name;

        User(String name) {
            this.name = name;           // this.name = POLE, name = PARAMETR
        }
    }

    /**
     * 6. Parametr o tej samej nazwie co pole zasłania pole (shadowing). Bez {@code this.} piszesz do
     * parametru, a pole zostaje puste.
     */
    static void shadowing() {
        section("6. Zasłanianie nazw — this.name = name");

        show("BuggyUser.name", new BuggyUser("Ala").name);
        // WYNIK: BuggyUser.name → null
        show("User.name", new User("Ala").name);
        // WYNIK: User.name → Ala

        // JAK TO DZIAŁA: wewnątrz konstruktora najbliższa deklaracja wygrywa — „name” to parametr.
        // Pole jest nadal dostępne, ale tylko jako this.name.
        // DOBRA PRAKTYKA: używaj tej samej nazwy dla parametru i pola (to konwencja w Javie),
        // ale ZAWSZE z this. po lewej stronie. IntelliJ podkreśla „name = name” jako podejrzane.
    }

    // =================================================================================================
    // 7. WALIDACJA W KONSTRUKTORZE
    // =================================================================================================

    static class Account {              // account = konto
        String owner;                   // owner = właściciel
        int balance;                    // balance = saldo

        Account(String owner, int balance) {
            if (owner == null || owner.isBlank()) {             // isBlank = czy pusty/same spacje (Java 11+)
                throw new IllegalArgumentException("właściciel nie może być pusty");
            }
            if (balance < 0) {
                throw new IllegalArgumentException("saldo startowe nie może być ujemne: " + balance);
            }
            this.owner = owner.strip();                         // strip = obetnij białe znaki (Java 11+)
            this.balance = balance;
        }
    }

    /**
     * 7. Konstruktor to pierwsza linia obrony: złe dane → wyjątek, obiekt w ogóle nie powstaje.
     */
    static void validation() {
        section("7. Walidacja w konstruktorze");

        Account ok = new Account("  Jan Kowalski ", 100);
        show("poprawne konto", ok.owner + ", saldo " + ok.balance);
        // WYNIK: poprawne konto → Jan Kowalski, saldo 100

        expectThrows("pusty właściciel", () -> new Account("   ", 100));
        // WYNIK: ✔ pusty właściciel → rzucono IllegalArgumentException: właściciel nie może być pusty
        expectThrows("ujemne saldo", () -> new Account("Ola", -50));
        // WYNIK: ✔ ujemne saldo → rzucono IllegalArgumentException: saldo startowe nie może być ujemne: -50
        expectThrows("null jako właściciel", () -> new Account(null, 0));
        // WYNIK: ✔ null jako właściciel → rzucono IllegalArgumentException: właściciel nie może być pusty

        // JAK TO DZIAŁA: throw przerywa konstruktor. new nie zwraca referencji, więc NIKT nie dostanie
        // „zepsutego” obiektu. Szczegóły o wyjątkach: t10_exceptions/Exceptions01Basics.
        // DOBRA PRAKTYKA: komunikat wyjątku mówi CO jest nie tak i jaka była wartość (-50).
        // DOBRA PRAKTYKA: sprawdzaj w konstruktorze wszystko, co musi być prawdą przez całe życie obiektu.
    }

    // =================================================================================================
    // 8. KOLEJNOŚĆ INICJALIZACJI
    // =================================================================================================

    /** Klasa, która „melduje” każdy krok swojego tworzenia. */
    static class Trace {                // trace = ślad
        int a = log("1. inicjalizator pola a");

        {                               // blok inicjalizacyjny instancji (instance initializer block)
            log("2. blok inicjalizacyjny { ... }");
        }

        int b = log("3. inicjalizator pola b");

        Trace() {
            log("4. ciało konstruktora Trace()");
        }

        Trace(String label) {           // label = etykieta
            this();                     // najpierw cały „normalny” proces z Trace()
            log("5. ciało konstruktora Trace(String): " + label);
        }

        static int log(String step) {   // log = zapisz w dzienniku; step = krok
            note(step);
            return 1;
        }
    }

    /**
     * 8. Kolejność: inicjalizatory pól i bloki {@code { ... }} (z góry na dół), potem ciało konstruktora.
     */
    static void initializationOrder() {
        section("8. Kolejność inicjalizacji — ślad");

        new Trace();
        // WYNIK: ℹ 1. inicjalizator pola a
        // WYNIK: ℹ 2. blok inicjalizacyjny { ... }
        // WYNIK: ℹ 3. inicjalizator pola b
        // WYNIK: ℹ 4. ciało konstruktora Trace()

        line();
        new Trace("z etykietą");
        // WYNIK: ℹ 1. inicjalizator pola a
        // WYNIK: ℹ 2. blok inicjalizacyjny { ... }
        // WYNIK: ℹ 3. inicjalizator pola b
        // WYNIK: ℹ 4. ciało konstruktora Trace()
        // WYNIK: ℹ 5. ciało konstruktora Trace(String): z etykietą

        // JAK TO DZIAŁA: inicjalizatory i bloki wykonują się RAZ na obiekt, przed ciałem konstruktora,
        // w kolejności zapisu w pliku. Przy this(...) wykonują się w konstruktorze „na końcu łańcucha”.
        // (Przed nimi jest jeszcze konstruktor klasy nadrzędnej — super(), patrz t07_inheritance_polymorphism/Inherit01Basics.)
        // DOBRA PRAKTYKA: bloki { ... } spotkasz rzadko — prościej zrobić to samo w konstruktorze.
        // Pełny obraz ładowania klas: t26_jvm/Jvm02ClassLoadingInit.
    }

    // =================================================================================================
    // 9. KONSTRUKTOR KOPIUJĄCY
    // =================================================================================================

    /**
     * 9. {@code new Book(other)} tworzy NOWY obiekt z tymi samymi wartościami. Zmiana kopii nie dotyka
     * oryginału (w przeciwieństwie do aliasu {@code Book b = a}).
     */
    static void copyConstructor() {
        section("9. Konstruktor kopiujący");

        Book original = new Book("Java. Podstawy", 800);    // original = oryginał
        Book alias = original;                              // alias — TEN SAM obiekt
        Book copy = new Book(original);                     // copy = kopia — NOWY obiekt

        copy.pages = 900;               // zmiana kopii
        show("original po zmianie kopii", original.describe());
        // WYNIK: original po zmianie kopii → Java. Podstawy, 800 str.
        alias.pages = 1000;             // zmiana przez alias = zmiana oryginału
        show("original po zmianie aliasu", original.describe());
        // WYNIK: original po zmianie aliasu → Java. Podstawy, 1000 str.
        show("copy == original", copy == original);
        // WYNIK: copy == original → false

        // PUŁAPKA: jeśli klasa ma pole-tablicę, „this.songs = other.songs” skopiuje tylko REFERENCJĘ —
        // kopia i oryginał będą dzielić jedną tablicę (tzw. kopia płytka, shallow copy).
        // Trzeba skopiować też tablicę (Arrays.copyOf) — ćwiczenie 4 i t06_oop_basics/Oop06Immutability.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • konstruktor = nazwa klasy, brak typu zwracanego; uruchamia go new
     *   • brak konstruktorów → kompilator dopisuje pusty bezparametrowy; zadeklarujesz własny → znika
     *   • przeciążanie: wiele konstruktorów z różnymi parametrami
     *   • this(...) = wywołaj inny konstruktor; tylko jako PIERWSZA instrukcja
     *   • this.name = name — pole kontra parametr; samo name = name to błąd logiczny (pole zostaje null)
     *   • walidacja w konstruktorze: throw new IllegalArgumentException("...") → obiekt nie powstaje
     *   • kolejność: wartości domyślne → inicjalizatory pól i bloki { } (z góry na dół) → ciało konstruktora
     *   • konstruktor kopiujący: Book(Book other) — nowy obiekt, te same wartości; tablice kopiuj osobno
     *
     * PYTANIA KONTROLNE:
     *   1. Kiedy kompilator dopisuje konstruktor domyślny, a kiedy nie?
     *   2. ZNAJDŹ BŁĄD:  class Car { String brand; Car(String brand) { brand = brand; } }
     *   3. Co wypisze:  class A { int x = 5; { x = x * 2; } A() { x = x + 1; } }
     *                   System.out.println(new A().x);  ?
     *   4. ZNAJDŹ BŁĄD:  Pizza(String name) { System.out.println("tworzę"); this(name, "M"); }
     *   5. Dlaczego walidację robimy w konstruktorze, a nie „kiedyś później”, przy użyciu obiektu?
     *   6. Co wypisze:  Book a = new Book("X", 10); Book b = new Book(a); b.pages = 99;
     *                   System.out.println(a.pages);  ?
     *   7. class Dog { Dog(String name) { } } — czy new Dog() się skompiluje? Dlaczego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: konstruktor osoby", "Ala (25 lat)", () -> new Ex1Person("Ala", 25).describe());
        Check.equal("ćw. 2: łańcuch this(...)", "latte M, cukier 0 | americano L, cukier 0 | mocha S, cukier 2",
                () -> exercise2());
        Check.equal("ćw. 3a: szerokość zakresu", 4, () -> new Ex3Range(1, 5).width());
        Check.throwsException("ćw. 3b: min > max", IllegalArgumentException.class, () -> new Ex3Range(5, 1));
        Check.equal("ćw. 4: głęboka kopia playlisty", "Hity[Z, B] / Kopia[A, B]", () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "Ala (25 lat)", () -> new Sol1Person("Ala", 25).describe());
        Check.equal("ćw. 2 (wzorzec)", "latte M, cukier 0 | americano L, cukier 0 | mocha S, cukier 2",
                () -> solution2());
        Check.equal("ćw. 3a (wzorzec)", 4, () -> new Sol3Range(1, 5).width());
        Check.throwsException("ćw. 3b (wzorzec)", IllegalArgumentException.class, () -> new Sol3Range(5, 1));
        Check.equal("ćw. 4 (wzorzec)", "Hity[Z, B] / Kopia[A, B]", () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): uzupełnij konstruktor Ex1Person(String name, int age), aby ustawiał oba pola.
     * Wtedy {@code new Ex1Person("Ala", 25).describe()} zwróci „Ala (25 lat)”.
     * Podpowiedź: parametry mają te same nazwy co pola — potrzebujesz this.
     */
    static class Ex1Person {            // person = osoba
        String name;
        int age;

        Ex1Person(String name, int age) {
            // TODO: przypisz parametry do pól
        }

        String describe() {
            return name + " (" + age + " lat)";
        }
    }

    /**
     * ĆWICZENIE 2 (średnie): kawa ma główny konstruktor (type, size, sugar). Uzupełnij dwa krótsze tak,
     * by delegowały przez this(...): Ex2Coffee(type) → rozmiar "M", cukier 0; Ex2Coffee(type, size) →
     * cukier 0. Oczekiwane: „latte M, cukier 0 | americano L, cukier 0 | mocha S, cukier 2”.
     * Podpowiedź: jedna linia w każdym konstruktorze, np. this(type, "M").
     */
    static class Ex2Coffee {            // coffee = kawa
        String type;                    // type = rodzaj
        String size;
        int sugar;                      // sugar = cukier (łyżeczki)

        Ex2Coffee(String type) {
            // TODO: this(...)
        }

        Ex2Coffee(String type, String size) {
            // TODO: this(...)
        }

        Ex2Coffee(String type, String size, int sugar) {
            this.type = type;
            this.size = size;
            this.sugar = sugar;
        }

        String describe() {
            return type + " " + size + ", cukier " + sugar;
        }
    }

    static String exercise2() {
        return new Ex2Coffee("latte").describe() + " | " + new Ex2Coffee("americano", "L").describe()
                + " | " + new Ex2Coffee("mocha", "S", 2).describe();
    }

    /**
     * ĆWICZENIE 3 (średnie): konstruktor Ex3Range(min, max) ma przypisać pola, ale gdy {@code min > max} —
     * rzucić IllegalArgumentException. width() zwraca max - min.
     * Podpowiedź: najpierw if + throw, dopiero potem przypisania.
     */
    static class Ex3Range {             // range = zakres
        int min;
        int max;

        Ex3Range(int min, int max) {
            // TODO: walidacja (min > max → wyjątek) i przypisanie pól
        }

        int width() {                   // width = szerokość
            return max - min;
        }
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): uzupełnij konstruktor kopiujący Ex4Playlist(Ex4Playlist other) tak, by
     * kopia miała WŁASNĄ tablicę piosenek (głęboka kopia). Metoda exercise4 zmienia piosenkę w
     * oryginale i nazwę w kopii — nic nie może „przeciec” na drugą stronę.
     * Oczekiwane: „Hity[Z, B] / Kopia[A, B]”.
     * Podpowiedź: Arrays.copyOf(tablica, tablica.length) albo tablica.clone().
     */
    static class Ex4Playlist {          // playlist = lista odtwarzania
        String name;
        String[] songs;                 // songs = piosenki

        Ex4Playlist(String name, String[] songs) {
            this.name = name;
            this.songs = songs;
        }

        Ex4Playlist(Ex4Playlist other) {
            // TODO: skopiuj nazwę i ZAWARTOŚĆ tablicy (nie samą referencję!)
            throw new UnsupportedOperationException("TODO");
        }
    }

    static String exercise4() {
        Ex4Playlist original = new Ex4Playlist("Hity", new String[] {"A", "B"});
        Ex4Playlist copy = new Ex4Playlist(original);
        original.songs[0] = "Z";
        copy.name = "Kopia";
        return original.name + Arrays.toString(original.songs) + " / " + copy.name + Arrays.toString(copy.songs);
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static class Sol1Person {
        String name;
        int age;

        Sol1Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        String describe() {
            return name + " (" + age + " lat)";
        }
    }

    static class Sol2Coffee {
        String type;
        String size;
        int sugar;

        Sol2Coffee(String type) {
            this(type, "M");
        }

        Sol2Coffee(String type, String size) {
            this(type, size, 0);
        }

        Sol2Coffee(String type, String size, int sugar) {
            this.type = type;
            this.size = size;
            this.sugar = sugar;
        }

        String describe() {
            return type + " " + size + ", cukier " + sugar;
        }
    }

    static String solution2() {
        return new Sol2Coffee("latte").describe() + " | " + new Sol2Coffee("americano", "L").describe()
                + " | " + new Sol2Coffee("mocha", "S", 2).describe();
    }

    static class Sol3Range {
        int min;
        int max;

        Sol3Range(int min, int max) {
            if (min > max) {
                throw new IllegalArgumentException("min > max: " + min + " > " + max);
            }
            this.min = min;
            this.max = max;
        }

        int width() {
            return max - min;
        }
    }

    static class Sol4Playlist {
        String name;
        String[] songs;

        Sol4Playlist(String name, String[] songs) {
            this.name = name;
            this.songs = songs;
        }

        Sol4Playlist(Sol4Playlist other) {
            this(other.name, Arrays.copyOf(other.songs, other.songs.length));   // NOWA tablica
        }
    }

    static String solution4() {
        Sol4Playlist original = new Sol4Playlist("Hity", new String[] {"A", "B"});
        Sol4Playlist copy = new Sol4Playlist(original);
        original.songs[0] = "Z";
        copy.name = "Kopia";
        return original.name + Arrays.toString(original.songs) + " / " + copy.name + Arrays.toString(copy.songs);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Tylko wtedy, gdy klasa nie ma ŻADNEGO konstruktora. Jeden zadeklarowany wystarczy, by
     *      domyślny nie powstał.
     *   2. brand = brand przypisuje parametr sam do siebie; pole brand zostaje null. Poprawnie:
     *      this.brand = brand;
     *   3. 11 — najpierw inicjalizator (x = 5), potem blok (x = 10), na końcu konstruktor (x = 11).
     *   4. this(...) musi być pierwszą instrukcją konstruktora — błąd kompilacji. Przenieś println
     *      za wywołanie this(name, "M").
     *   5. Bo wtedy zły obiekt w ogóle nie powstaje. Błąd wychodzi od razu, w miejscu, gdzie podano złe
     *      dane, a nie godzinę później w zupełnie innym miejscu programu.
     *   6. 10 — b to nowy obiekt (kopia), zmiana b.pages nie dotyka a.
     *   7. Nie. Klasa ma konstruktor Dog(String), więc kompilator nie dopisał bezparametrowego.
     *      Błąd: constructor Dog in class Dog cannot be applied to given types.
     */
    // </editor-fold>
}
