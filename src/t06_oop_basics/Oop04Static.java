package t06_oop_basics;

import helpers.Check;
import java.util.Locale;
import static java.lang.Math.max;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: static — składowe należące do KLASY, a nie do obiektu
 *        (static = statyczny, instance = egzemplarz, constant = stała, utility class = klasa narzędziowa)
 *
 * W SKRÓCIE:
 *   Zwykłe pole istnieje w każdym obiekcie osobno. Pole static istnieje RAZ — jest wspólne dla wszystkich
 *   obiektów klasy. Metoda static nie działa na żadnym obiekcie (nie ma this), więc może korzystać tylko
 *   z parametrów i innych składowych static. Tak budujemy stałe, liczniki i klasy narzędziowe (Math).
 *
 * ANALOGIA:
 *   Klasa szkolna. Każdy uczeń ma własny zeszyt (pole instancji). Na ścianie wisi JEDNA tablica
 *   (pole static) — gdy ktoś na niej coś napisze, widzą to wszyscy. Woźny (metoda static) może zmienić
 *   godzinę na zegarze w klasie, ale nie zajrzy do zeszytu konkretnego ucznia, bo nie wie którego.
 *
 * JAK TO DZIAŁA:
 *   class Robot {
 *       static int created;   ← 1 sztuka na całą klasę (w pamięci klasy)
 *       int id;               ← osobna sztuka w KAŻDYM obiekcie
 *   }
 *       Robot.created ──▶ [ 3 ]           (wspólne)
 *       r1 ──▶ { id=1 }   r2 ──▶ { id=2 }   r3 ──▶ { id=3 }
 *
 *   Wywołanie:  Robot.created, Math.max(3, 7)  — przez nazwę KLASY, bez new.
 *   static { ... } — blok uruchamiany RAZ, gdy klasa jest używana po raz pierwszy.
 *
 * SŁÓWKA:
 *   static = statyczny (należy do klasy); instance = egzemplarz; constant = stała; final = ostateczny
 *   (nie do zmiany); utility class = klasa narzędziowa; static import = import statyczny;
 *   counter = licznik; shared = współdzielony; initializer = inicjalizator
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop02Constructors (bloki inicjalizacyjne instancji),
 *             t06_oop_basics/Oop07NestedClasses (static nested kontra inner class),
 *             t26_jvm/Jvm02ClassLoadingInit (kiedy dokładnie JVM inicjalizuje klasę),
 *             t22_design_patterns/Patterns04Singleton (jeden obiekt na całą aplikację)
 * </pre>
 */
public class Oop04Static {

    public static void main(String[] args) {    // main też jest static — JVM wywołuje ją BEZ tworzenia obiektu
        title("Oop04 — static: pola, metody, stałe");

        sharedCounter();            // shared counter = wspólny licznik
        staticVsInstanceMethods();  // static vs instance methods = metody statyczne kontra instancji
        constants();                // constants = stałe
        staticInitBlock();          // static init block = statyczny blok inicjalizacyjny
        utilityClass();             // utility class = klasa narzędziowa
        staticImport();             // static import = import statyczny
        sharedStatePitfall();       // shared state pitfall = pułapka współdzielonego stanu
        whenToUseStatic();          // when to use static = kiedy używać static
        exercises();                // exercises = ćwiczenia
    }

    // Klasy przykładowe są statycznymi klasami zagnieżdżonymi, by lekcja była jednym plikiem
    // (wyjaśnienie w t06_oop_basics/Oop01ClassesObjects). W tej lekcji wreszcie widać, co znaczy static.

    // =================================================================================================
    // 1. POLE STATYCZNE — WSPÓLNY LICZNIK
    // =================================================================================================

    static class Robot {                // robot = robot
        static int created;             // created = utworzono (ile robotów) — JEDNO pole na całą klasę
        final int id;                   // id = numer — osobny w każdym obiekcie
        final String name;

        Robot(String name) {
            created++;                  // zwiększamy WSPÓLNY licznik
            this.id = created;          // i zapamiętujemy jego stan jako własny numer
            this.name = name;
        }

        String describe() {
            return "#" + id + " " + name;
        }
    }

    /**
     * 1. Pole static żyje w klasie, nie w obiekcie. Każdy konstruktor zwiększa TEN SAM licznik.
     */
    static void sharedCounter() {
        section("1. Pole static — wspólny licznik obiektów");

        show("Robot.created na starcie", Robot.created);   // dostęp przez nazwę KLASY — obiekt niepotrzebny
        // WYNIK: Robot.created na starcie → 0

        Robot r1 = new Robot("Odkurzacz");
        Robot r2 = new Robot("Kosiarka");
        Robot r3 = new Robot("Ramię");
        show("r1", r1.describe());
        // WYNIK: r1 → #1 Odkurzacz
        show("r2", r2.describe());
        // WYNIK: r2 → #2 Kosiarka
        show("r3", r3.describe());
        // WYNIK: r3 → #3 Ramię
        show("Robot.created", Robot.created);
        // WYNIK: Robot.created → 3

        // JAK TO DZIAŁA: id każdego robota jest inne (pole instancji), created — jedno dla wszystkich.
        // PUŁAPKA: da się napisać r1.created — kompiluje się (z ostrzeżeniem [static]), ale wprowadza w błąd,
        // bo wygląda jak pole obiektu. Zawsze pisz Robot.created.
    }

    // =================================================================================================
    // 2. METODY STATYCZNE KONTRA METODY INSTANCJI
    // =================================================================================================

    static class Temperature {          // temperature = temperatura
        final double celsius;           // celsius = stopnie Celsjusza

        Temperature(double celsius) {
            this.celsius = celsius;
        }

        double toFahrenheit() {         // metoda INSTANCJI — korzysta z pola „tego” obiektu
            return celsius * 9 / 5 + 32;
        }

        static double celsiusToFahrenheit(double c) {   // metoda STATYCZNA — tylko parametry
            return c * 9 / 5 + 32;
        }
    }

    /**
     * 2. Metoda instancji potrzebuje obiektu (ma this). Metoda static — nie: dostaje wszystko w parametrach.
     */
    static void staticVsInstanceMethods() {
        section("2. Metoda static kontra metoda instancji");

        Temperature boiling = new Temperature(100);         // boiling = wrzenie
        show("boiling.toFahrenheit()", boiling.toFahrenheit());
        // WYNIK: boiling.toFahrenheit() → 212.0
        show("Temperature.celsiusToFahrenheit(25)", Temperature.celsiusToFahrenheit(25));
        // WYNIK: Temperature.celsiusToFahrenheit(25) → 77.0

        // PUŁAPKA: metoda static nie widzi pól instancji ani this:
        //     static double broken() { return celsius * 9 / 5 + 32; }
        //     // błąd kompilacji: non-static variable celsius cannot be referenced from a static context
        //     static void alsoBroken() { System.out.println(this); }
        //     // błąd kompilacji: non-static variable this cannot be referenced from a static context
        // Dlaczego? Wywołanie Temperature.celsiusToFahrenheit(25) nie wskazuje ŻADNEGO obiektu —
        // nie ma „tego” celsius, który można by odczytać.
        // W drugą stronę działa: metoda instancji może używać pól i metod static.

        // Dlatego w lekcjach wszystkie metody wywoływane z main są static: main jest static,
        // więc bez tworzenia obiektu może wołać tylko inne metody static.
    }

    // =================================================================================================
    // 3. STAŁE: STATIC FINAL
    // =================================================================================================

    static class Car {                  // car = samochód
        static final int MAX_PASSENGERS = 5;        // stała: static (jedna) + final (niezmienna)
        static final String DEFAULT_COLOR = "czarny";

        int passengers;                 // passengers = pasażerowie

        boolean canTakeMore() {         // can take more = czy zmieści więcej
            return passengers < MAX_PASSENGERS;     // zamiast „magicznej liczby” 5
        }
    }

    /**
     * 3. {@code static final} = stała: jedna kopia i nie da się jej zmienić. Nazwy stałych piszemy
     * WIELKIMI_LITERAMI_Z_PODKREŚLNIKAMI.
     */
    static void constants() {
        section("3. Stałe — static final");

        show("Car.MAX_PASSENGERS", Car.MAX_PASSENGERS);
        // WYNIK: Car.MAX_PASSENGERS → 5
        show("Car.DEFAULT_COLOR", Car.DEFAULT_COLOR);
        // WYNIK: Car.DEFAULT_COLOR → czarny
        show("Integer.MAX_VALUE", Integer.MAX_VALUE);           // stałe z JDK
        // WYNIK: Integer.MAX_VALUE → 2147483647
        show("Math.PI", Math.PI);
        // WYNIK: Math.PI → 3.141592653589793

        Car car = new Car();
        car.passengers = 5;
        show("car.canTakeMore()", car.canTakeMore());
        // WYNIK: car.canTakeMore() → false

        //     Car.MAX_PASSENGERS = 7;   // błąd kompilacji: cannot assign a value to final variable MAX_PASSENGERS
        // DOBRA PRAKTYKA: zamiast „magicznych liczb” (if (passengers < 5)) używaj nazwanej stałej —
        // kod mówi, CO oznacza liczba, a zmiana wymaga poprawki w jednym miejscu.
    }

    // =================================================================================================
    // 4. STATYCZNY BLOK INICJALIZACYJNY
    // =================================================================================================

    static class Config {               // config = konfiguracja
        static final int VERSION = 3;               // stała CZASU KOMPILACJI (literał) — wklejana w miejsce użycia
        static final String APP_NAME;               // app name = nazwa aplikacji — ustawiana w bloku static

        static {                        // static { } — uruchamia się RAZ, przy pierwszym użyciu klasy
            note("static { } klasy Config — start (tylko raz)");
            APP_NAME = "Sklep v" + VERSION;
        }
    }

    /**
     * 4. Blok {@code static { ... }} przygotowuje dane klasy. JVM uruchamia go raz, gdy klasa jest
     * pierwszy raz naprawdę potrzebna (leniwie).
     */
    static void staticInitBlock() {
        section("4. Blok static { } — ślad inicjalizacji klasy");

        show("Config.VERSION", Config.VERSION);
        // WYNIK: Config.VERSION → 3
        // Ciekawostka: odczyt VERSION NIE uruchomił bloku static — kompilator wkleił tu po prostu 3.

        show("Config.APP_NAME (1. odczyt)", Config.APP_NAME);
        // WYNIK: ℹ static { } klasy Config — start (tylko raz)
        // WYNIK: Config.APP_NAME (1. odczyt) → Sklep v3
        show("Config.APP_NAME (2. odczyt)", Config.APP_NAME);
        // WYNIK: Config.APP_NAME (2. odczyt) → Sklep v3

        // JAK TO DZIAŁA: pierwszy odczyt APP_NAME wymaga zainicjalizowanej klasy → JVM uruchamia
        // static { } (stąd linia ℹ PRZED wynikiem), a drugi odczyt już nie — klasa jest gotowa.
        // Kolejność: inicjalizatory pól static i bloki static wykonują się z góry na dół, RAZ na klasę
        // (bloki instancji z Oop02 — raz na KAŻDY obiekt). Szczegóły: t26_jvm/Jvm02ClassLoadingInit.
        // PUŁAPKA: wyjątek w static { } psuje klasę na dobre (ExceptionInInitializerError) — trzymaj tam proste rzeczy.
    }

    // =================================================================================================
    // 5. KLASA NARZĘDZIOWA
    // =================================================================================================

    static final class TextUtils {      // text utils = narzędzia tekstowe; final = nie da się po niej dziedziczyć
        private TextUtils() {           // prywatny konstruktor: nie twórz obiektów tej klasy
            throw new UnsupportedOperationException("klasa narzędziowa — nie twórz obiektów");
        }

        static String capitalize(String s) {        // capitalize = zacznij wielką literą
            if (s == null || s.isEmpty()) {
                return s;
            }
            return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
        }

        static boolean isPalindrome(String s) {     // is palindrome = czy palindrom
            String clean = s.replace(" ", "").toLowerCase(Locale.ROOT);
            return new StringBuilder(clean).reverse().toString().equals(clean);
        }
    }

    /**
     * 5. Klasa narzędziowa = zbiór metod static bez stanu (jak Math, Arrays). Prywatny konstruktor
     * blokuje bezsensowne {@code new TextUtils()}.
     */
    static void utilityClass() {
        section("5. Klasa narzędziowa — same metody static");

        show("capitalize(\"warszawa\")", TextUtils.capitalize("warszawa"));
        // WYNIK: capitalize("warszawa") → Warszawa
        show("isPalindrome(\"Kobyła ma mały bok\")", TextUtils.isPalindrome("Kobyła ma mały bok"));
        // WYNIK: isPalindrome("Kobyła ma mały bok") → true
        show("Math.abs(-7)", Math.abs(-7));                 // abs = wartość bezwzględna — też klasa narzędziowa
        // WYNIK: Math.abs(-7) → 7

        // W osobnym pliku new TextUtils() to błąd kompilacji (TextUtils() has private access in TextUtils).
        // Tu klasa jest zagnieżdżona, więc kompilator na to pozwala — i wtedy działa „druga linia obrony”:
        expectThrows("new TextUtils()", () -> new TextUtils());
        // WYNIK: ✔ new TextUtils() → rzucono UnsupportedOperationException: klasa narzędziowa — nie twórz obiektów

        // DOBRA PRAKTYKA: klasa narzędziowa = final + private konstruktor + same metody static,
        // bez pól zmiennych. Przykłady z JDK: Math, Arrays, Objects, Collections.
    }

    // =================================================================================================
    // 6. IMPORT STATYCZNY
    // =================================================================================================

    /**
     * 6. {@code import static java.lang.Math.max;} pozwala pisać {@code max(3, 7)} zamiast
     * {@code Math.max(3, 7)}. Z tego samego powodu w lekcjach piszemy show(...) zamiast Console.show(...).
     */
    static void staticImport() {
        section("6. Import statyczny");

        show("max(3, 7) — po imporcie statycznym", max(3, 7));
        // WYNIK: max(3, 7) — po imporcie statycznym → 7
        show("Math.max(3, 7) — pełny zapis", Math.max(3, 7));
        // WYNIK: Math.max(3, 7) — pełny zapis → 7

        // Na górze pliku: import static java.lang.Math.max;
        //                 import static helpers.Console.*;   ← gwiazdka = wszystkie składowe static
        // PUŁAPKA: przy wielu importach statycznych z gwiazdką czytelnik nie wie, SKĄD jest metoda.
        // DOBRA PRAKTYKA: import static dla kilku często używanych, jednoznacznych nazw (asercje w testach,
        // stałe, pomocnicy jak show). Więcej o importach: t06_oop_basics/Oop08PackagesAccess.
    }

    // =================================================================================================
    // 7. PUŁAPKA: WSPÓŁDZIELONY STAN STATYCZNY
    // =================================================================================================

    static class Cart {                 // cart = koszyk — wersja Z BŁĘDEM
        static int itemCount;           // BŁĄD PROJEKTOWY: licznik wspólny dla WSZYSTKICH koszyków
        final String owner;

        Cart(String owner) {
            this.owner = owner;
        }

        void add(String item) {         // add = dodaj; item = pozycja
            itemCount++;
        }

        String describe() {
            return owner + ": " + itemCount + " szt.";
        }
    }

    /**
     * 7. Zmienne pole static to „globalna zmienna”. Wszystkie obiekty ją współdzielą — często wbrew intencji.
     */
    static void sharedStatePitfall() {
        section("7. PUŁAPKA — zmienny stan static jest wspólny");

        Cart anna = new Cart("Anna");
        Cart piotr = new Cart("Piotr");
        anna.add("kawa");
        anna.add("czekolada");
        piotr.add("książka");

        show("koszyk Anny", anna.describe());
        // WYNIK: koszyk Anny → Anna: 3 szt.
        show("koszyk Piotra", piotr.describe());
        // WYNIK: koszyk Piotra → Piotr: 3 szt.

        // PUŁAPKA: Anna ma 2 rzeczy, Piotr 1, a oba koszyki pokazują 3 — bo licznik jest jeden.
        // Poprawka: int itemCount BEZ static (pole instancji). Ćwiczenie 3 każe to przepisać.
        // Inne skutki zmiennego static: stan „przecieka” między testami (trzeba go resetować)
        // i jest dzielony między wątkami (t21_concurrency/Concurrency02RaceConditions).
        // DOBRA PRAKTYKA: static final dla stałych — tak; zmienne pola static — tylko świadomie i rzadko.
    }

    // =================================================================================================
    // 8. KIEDY STATIC, A KIEDY NIE
    // =================================================================================================

    /**
     * 8. Metoda static pasuje, gdy wynik zależy TYLKO od parametrów. Gdy zależy od stanu obiektu —
     * metoda instancji. Znane metody static z JDK to często „fabryki” obiektów.
     */
    static void whenToUseStatic() {
        section("8. Kiedy static, a kiedy nie");

        show("Integer.parseInt(\"42\") + 1", Integer.parseInt("42") + 1);  // parse = przetwórz tekst
        // WYNIK: Integer.parseInt("42") + 1 → 43
        show("String.valueOf(3.5)", String.valueOf(3.5));                  // value of = wartość (jako tekst)
        // WYNIK: String.valueOf(3.5) → 3.5
        show("String.join(\"-\", \"a\", \"b\")", String.join("-", "a", "b")); // join = połącz
        // WYNIK: String.join("-", "a", "b") → a-b

        // Tabela decyzji:
        //   wynik zależy tylko od parametrów (max, parseInt)           → static
        //   metoda czyta/zmienia pola obiektu (withdraw, describe)    → instancji
        //   wartość wspólna i niezmienna (MAX_PASSENGERS, PI)          → static final
        //   „utwórz obiekt z…” (valueOf, of)                           → static (metoda fabryczna, Oop09)
        //   wartość różna dla każdego obiektu (imię, saldo)            → pole instancji
        // PUŁAPKA: „wszystko static, bo tak prościej” = programowanie bez obiektów i dużo stanu globalnego.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • static = należy do KLASY (jedna kopia); bez static = każdy obiekt ma swoją
     *   • odwołanie: NazwaKlasy.pole / NazwaKlasy.metoda() — bez new
     *   • metoda static nie ma this i nie widzi pól instancji (błąd: non-static ... from a static context)
     *   • static final = stała, NAZWA_WIELKIMI; zamiast magicznych liczb
     *   • static { } — raz na klasę, przy pierwszym użyciu; stałe-literały nie wywołują inicjalizacji
     *   • klasa narzędziowa: final class + private konstruktor + metody static (Math, Arrays)
     *   • import static java.lang.Math.max; → max(3, 7)
     *   • zmienny stan static = współdzielony przez wszystkich → częste źródło błędów
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się pole static od pola instancji? Podaj przykład każdego.
     *   2. ZNAJDŹ BŁĄD:  class Circle { double r; static double area() { return Math.PI * r * r; } }
     *   3. Co wypisze:  class C { static int n; C() { n++; } }
     *                   new C(); new C(); new C(); System.out.println(C.n);  ?
     *   4. Po co klasie narzędziowej prywatny konstruktor?
     *   5. Ile razy wykona się blok static { } klasy, jeśli utworzysz 100 jej obiektów?
     *   6. ZNAJDŹ BŁĄD:  class Player { static int score; void addPoint() { score++; } }
     *                    — dwóch graczy zdobywa punkty, a wyniki są zawsze równe.
     *   7. Co wypisze:  System.out.println(max(2, -5));  po  import static java.lang.Math.max;  ?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: kolejne numery biletów", "1,2,3", () -> exercise1());
        Check.equal("ćw. 2: inicjały (klasa narzędziowa)", "J.K. A.M.N.",
                () -> Ex2Names.initials("Jan Kowalski") + " " + Ex2Names.initials("anna maria nowak"));
        Check.equal("ćw. 3: koszyki z własnym licznikiem", "a=2, b=1, razem=3", () -> exercise3());
        Check.equal("ćw. 4: generator identyfikatorów", "ZAM-001, ZAM-002, ZAM-003 | po reset: ZAM-001",
                () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "1,2,3", () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", "J.K. A.M.N.",
                () -> Sol2Names.initials("Jan Kowalski") + " " + Sol2Names.initials("anna maria nowak"));
        Check.equal("ćw. 3 (wzorzec)", "a=2, b=1, razem=3", () -> solution3());
        Check.equal("ćw. 4 (wzorzec)", "ZAM-001, ZAM-002, ZAM-003 | po reset: ZAM-001", () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): każdy nowy bilet dostaje kolejny numer: 1, 2, 3... Uzupełnij konstruktor
     * Ex1Ticket, korzystając ze statycznego licznika lastNumber. Oczekiwane: „1,2,3”.
     * Podpowiedź: to samo, co Robot w sekcji 1 — zwiększ licznik klasy i zapisz go w polu obiektu.
     */
    static class Ex1Ticket {            // ticket = bilet
        static int lastNumber;          // last number = ostatni wydany numer (wspólny)
        int number;                     // number = numer TEGO biletu

        Ex1Ticket() {
            // TODO: zwiększ lastNumber i przypisz do number
        }
    }

    static String exercise1() {
        return new Ex1Ticket().number + "," + new Ex1Ticket().number + "," + new Ex1Ticket().number;
    }

    /**
     * ĆWICZENIE 2 (łatwe): klasa narzędziowa Ex2Names z metodą static initials(fullName), która zwraca
     * pierwsze litery słów (wielkie) z kropkami: „Jan Kowalski” → „J.K.”, „anna maria nowak” → „A.M.N.”.
     * Podpowiedź: fullName.split(" "), potem dla każdego słowa charAt(0) i Character.toUpperCase.
     */
    static final class Ex2Names {       // names = imiona/nazwiska
        private Ex2Names() {
        }

        static String initials(String fullName) {  // initials = inicjały; full name = pełne imię i nazwisko
            // TODO
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ koszyk z błędem ze wspólnym licznikiem:
     * <pre>{@code
     * static int itemCount;                      // jeden licznik dla wszystkich koszyków
     * void add() { itemCount++; }
     * int getCount() { return itemCount; }
     * static int getTotal() { return itemCount; }
     * }</pre>
     * Każdy koszyk ma liczyć SWOJE pozycje (getCount), a getTotal() — pozycje we wszystkich koszykach.
     * Oczekiwane: „a=2, b=1, razem=3”.
     * Podpowiedź: potrzebujesz DWÓCH pól: jednego instancji i jednego static.
     */
    static class Ex3Cart {              // cart = koszyk (wersja do poprawienia)
        static int itemCount;

        void add() {
            itemCount++;
        }

        int getCount() {
            return itemCount;
        }

        static int getTotal() {         // total = razem
            return itemCount;
        }
    }

    static String exercise3() {
        Ex3Cart a = new Ex3Cart();
        Ex3Cart b = new Ex3Cart();
        a.add();
        a.add();
        b.add();
        return "a=" + a.getCount() + ", b=" + b.getCount() + ", razem=" + Ex3Cart.getTotal();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): klasa narzędziowa Ex4Ids generuje identyfikatory zamówień:
     * nextId() zwraca kolejno „ZAM-001”, „ZAM-002”..., a reset() zeruje licznik. Użyj stałej PREFIX,
     * prywatnego licznika static i String.format(Locale.ROOT, "%s-%03d", ...).
     * Oczekiwane: „ZAM-001, ZAM-002, ZAM-003 | po reset: ZAM-001”.
     * Podpowiedź: %03d = liczba całkowita dopełniona zerami do 3 cyfr (t04_strings/Strings04Formatting).
     */
    static final class Ex4Ids {         // ids = identyfikatory
        static final String PREFIX = "ZAM";         // prefix = przedrostek

        private Ex4Ids() {
        }

        static String nextId() {        // next id = następny identyfikator
            // TODO
            throw new UnsupportedOperationException("TODO");
        }

        static void reset() {           // reset = wyzeruj
            // TODO
        }
    }

    static String exercise4() {
        Ex4Ids.reset();
        String first = Ex4Ids.nextId() + ", " + Ex4Ids.nextId() + ", " + Ex4Ids.nextId();
        Ex4Ids.reset();
        return first + " | po reset: " + Ex4Ids.nextId();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static class Sol1Ticket {
        static int lastNumber;
        int number;

        Sol1Ticket() {
            lastNumber++;
            number = lastNumber;
        }
    }

    static String solution1() {
        return new Sol1Ticket().number + "," + new Sol1Ticket().number + "," + new Sol1Ticket().number;
    }

    static final class Sol2Names {
        private Sol2Names() {
        }

        static String initials(String fullName) {
            StringBuilder sb = new StringBuilder();
            for (String word : fullName.split(" ")) {
                if (!word.isEmpty()) {
                    sb.append(Character.toUpperCase(word.charAt(0))).append('.');
                }
            }
            return sb.toString();
        }
    }

    static class Sol3Cart {
        static int totalItems;          // WSPÓLNE: wszystkie koszyki razem
        int itemCount;                  // WŁASNE: tylko ten koszyk

        void add() {
            itemCount++;
            totalItems++;
        }

        int getCount() {
            return itemCount;
        }

        static int getTotal() {
            return totalItems;
        }
    }

    static String solution3() {
        Sol3Cart a = new Sol3Cart();
        Sol3Cart b = new Sol3Cart();
        a.add();
        a.add();
        b.add();
        return "a=" + a.getCount() + ", b=" + b.getCount() + ", razem=" + Sol3Cart.getTotal();
    }

    static final class Sol4Ids {
        static final String PREFIX = "ZAM";
        private static int counter;     // private: nikt z zewnątrz nie „przestawi” licznika

        private Sol4Ids() {
        }

        static String nextId() {
            counter++;
            return String.format(Locale.ROOT, "%s-%03d", PREFIX, counter);
        }

        static void reset() {
            counter = 0;
        }
    }

    static String solution4() {
        Sol4Ids.reset();
        String first = Sol4Ids.nextId() + ", " + Sol4Ids.nextId() + ", " + Sol4Ids.nextId();
        Sol4Ids.reset();
        return first + " | po reset: " + Sol4Ids.nextId();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Pole static istnieje raz dla całej klasy (np. licznik utworzonych robotów, stała MAX_PASSENGERS).
     *      Pole instancji istnieje osobno w każdym obiekcie (np. imię robota, saldo konta).
     *   2. Metoda static area() używa pola instancji r — błąd kompilacji (non-static variable r cannot be
     *      referenced from a static context). Poprawka: usuń static z area() albo przekaż r parametrem.
     *   3. 3 — każdy konstruktor zwiększa to samo, wspólne pole n.
     *   4. Żeby nikt nie tworzył bezsensownych obiektów klasy, która ma same metody static. Zwykle dodaje się
     *      też final, a w konstruktorze throw — na wypadek wywołania „od środka” albo przez refleksję.
     *   5. Raz — blok static { } działa raz na klasę, przy pierwszym jej użyciu, a nie przy każdym new.
     *   6. score jest static, więc wszyscy gracze dzielą jeden wynik. Usuń static — każdy gracz dostanie
     *      własne pole score.
     *   7. 2 — max to Math.max, zaimportowane statycznie.
     */
    // </editor-fold>
}
