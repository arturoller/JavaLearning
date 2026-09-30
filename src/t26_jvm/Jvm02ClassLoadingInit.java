package t26_jvm;

import helpers.Check;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Ładowanie i inicjalizacja klas — kiedy JVM „budzi” klasę
 *        (class loading = ładowanie klas; initialization = inicjalizacja; class loader = ładowacz klas)
 *
 * W SKRÓCIE:
 *   Klasa nie jest gotowa od startu programu. JVM wczytuje ją, gdy jest potrzebna, a pola static i bloki static
 *   wykonuje dopiero przy PIERWSZYM AKTYWNYM UŻYCIU — i tylko raz. Ta wiedza tłumaczy „tajemnicze” zera i null-e
 *   w polach statycznych, błędy ExceptionInInitializerError / NoClassDefFoundError i leniwy singleton bez synchronized.
 *
 * ANALOGIA: wynajem mieszkania.
 *   Ładowanie = odbierasz klucze i plan mieszkania. Łączenie = inspekcja (czy ściany stoją — weryfikacja),
 *   wstawienie pustych szafek (przygotowanie: pola static = 0/null/false) i spisanie liczników (rozwiązanie
 *   odwołań). Inicjalizacja = urządzenie mieszkania według instrukcji (bloki static) — robi się ją RAZ, w dniu,
 *   w którym ktoś naprawdę się wprowadza, a nie w dniu podpisania umowy.
 *
 * JAK TO DZIAŁA:
 *   Cykl życia klasy (specyfikacja JVM, rozdział 5; tak działa HotSpot w Javie 17):
 *   etap                      | co się dzieje
 *   1. ładowanie (loading)    | class loader znajduje bajty klasy (plik .class, JAR, moduł JDK); powstaje obiekt Class
 *   2. łączenie (linking):    |
 *      a) weryfikacja         | weryfikator bajtkodu sprawdza poprawność kodu (typy, skoki, stos operandów)
 *      b) przygotowanie       | pola static dostają wartości DOMYŚLNE: 0, 0.0, false, null
 *      c) rozwiązanie         | nazwy symboliczne (np. „java/util/List”) → prawdziwe odnośniki; HotSpot robi
 *                             | to leniwie — przy pierwszym wykonaniu danej instrukcji
 *   3. inicjalizacja          | metoda {@code <clinit>}: inicjalizatory pól static i bloki static w kolejności
 *                             | z tekstu źródłowego; najpierw nadklasa; dokładnie RAZ na klasę
 *   4. używanie, 5. usunięcie | klasa może zniknąć (unloading) tylko razem ze swoim class loaderem
 *
 *   Inicjalizuje klasę T (aktywne użycie):          NIE inicjalizuje T:
 *     • new T()                                       • T.class, zmienna typu T, instanceof T
 *     • wywołanie metody static z T                   • new T[10] (tablica elementów typu T)
 *     • odczyt/zapis pola static T (nie-stałej)       • odczyt stałej czasu kompilacji (static final int X = 5)
 *     • Class.forName("pakiet.T"), refleksja          • Class.forName("pakiet.T", false, loader)
 *     • inicjalizacja podklasy T (nadklasa pierwsza)  • pole static nadklasy czytane przez podklasę → tylko nadklasa
 *
 * SŁÓWKA:
 *   loading = ładowanie; linking = łączenie; verification = weryfikacja; preparation = przygotowanie;
 *   resolution = rozwiązanie (odwołań); initializer = inicjalizator; trace = ślad; lazy = leniwy; eager = zachłanny;
 *   holder = przechowalnia; bootstrap = rozruch; platform = platforma; parent = rodzic; delegation = delegowanie;
 *   unloading = usunięcie klasy z pamięci; binary name = nazwa binarna.
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop04Static (pola i bloki static od strony składni),
 *             t07_inheritance_polymorphism/Inherit01Basics (konstruktory w hierarchii klas),
 *             t21_concurrency/Concurrency08ThreadSafetyPatterns (bezpieczna publikacja, singleton),
 *             t26_jvm/Jvm01Memory (metaspace — gdzie leżą metadane klas).
 * </pre>
 */
public class Jvm02ClassLoadingInit {

    public static void main(String[] args) throws ClassNotFoundException {
        title("Jvm02 — ładowanie i inicjalizacja klas");

        lazyInitialization();      // lazy initialization = leniwa inicjalizacja
        initializationOrder();     // initialization order = kolejność inicjalizacji
        compileTimeConstants();    // compile-time constants = stałe czasu kompilacji
        classForName();            // class for name = klasa po nazwie
        orderPitfalls();           // order pitfalls = pułapki kolejności
        failedInitialization();    // failed initialization = nieudana inicjalizacja
        holderIdiom();             // holder idiom = idiom przechowalni (leniwy singleton)
        classLoaders();            // class loaders = ładowacze klas
        exercises();               // exercises = ćwiczenia
    }

    /** TRACE = ślad: zdarzenia zapisywane przez bloki static i konstruktory klas pokazowych (w kolejności wykonania). */
    private static final List<String> TRACE = new ArrayList<>();

    /** trace = zapisz ślad; traced = zapisz i zwróć (wygodne w inicjalizatorach pól). */
    static void trace(String event) {
        TRACE.add(event);
    }

    static String traced(String event) {
        trace(event);
        return event;
    }

    /** takeTrace = zabierz ślad: zwraca kopię zdarzeń (copyOf, Java 10+) i czyści listę przed kolejnym pokazem. */
    static List<String> takeTrace() {
        List<String> copy = List.copyOf(TRACE);
        TRACE.clear();
        return copy;
    }

    // =================================================================================================
    // 1. LENIWA INICJALIZACJA — CO JĄ WYZWALA
    // =================================================================================================

    /** Lazy = leniwa: melduje w śladzie inicjalizację i każdy konstruktor. Item = element (utworzymy tylko tablicę). */
    static final class Lazy {
        static { trace("Lazy: blok static"); }
        Lazy() { trace("Lazy: konstruktor"); }
    }

    static class Item {
        static { trace("Item: inicjalizacja"); }
    }

    /** Tool = narzędzie: klasa z metodą static; twice = podwój. */
    static class Tool {
        static { trace("Tool: inicjalizacja"); }
        static int twice(int number) { return 2 * number; }
    }

    /** Parent = rodzic: deklaruje pole static; Child = dziecko: tylko je dziedziczy. */
    static class Parent {
        static String parentName = traced("Parent: inicjalizacja");
    }

    static class Child extends Parent {
        static { trace("Child: inicjalizacja"); }
    }

    /**
     * 1. Literał klasy (Lazy.class), tablica, instanceof — klasa co najwyżej się ŁADUJE. Bloki static wykonuje
     * dopiero aktywne użycie (new, metoda static, pole static) — raz na cały program.
     */
    static void lazyInitialization() {
        section("1. Leniwa inicjalizacja — co ją wyzwala");

        takeTrace();
        Class<?> type = Lazy.class;                 // type = typ; obiekt Class istnieje, klasa jest załadowana
        Item[] items = new Item[3];                 // items = elementy; tablica referencji — ani jednego obiektu Item
        Object something = "tekst";                // something = coś
        show("Lazy.class.getSimpleName()", type.getSimpleName());   // getSimpleName = pobierz krótką nazwę
        show("new Item[3] → length", items.length);
        show("\"tekst\" instanceof Item", something instanceof Item);
        show("ślad", takeTrace());
        // WYNIK: Lazy.class.getSimpleName() → Lazy
        // WYNIK: new Item[3] → length → 3
        // WYNIK: "tekst" instanceof Item → false
        // WYNIK: ślad → []

        new Lazy();
        show("ślad po pierwszym new Lazy()", takeTrace());
        new Lazy();
        show("ślad po drugim new Lazy()", takeTrace());
        show("Tool.twice(21)", Tool.twice(21));
        show("ślad", takeTrace());
        // WYNIK: ślad po pierwszym new Lazy() → [Lazy: blok static, Lazy: konstruktor]
        // WYNIK: ślad po drugim new Lazy() → [Lazy: konstruktor]
        // WYNIK: Tool.twice(21) → 42
        // WYNIK: ślad → [Tool: inicjalizacja]

        show("Child.parentName", Child.parentName);   // pole zadeklarowane w Parent!
        show("ślad", takeTrace());
        new Child();
        show("ślad po new Child()", takeTrace());
        // WYNIK: Child.parentName → Parent: inicjalizacja
        // WYNIK: ślad → [Parent: inicjalizacja]
        // WYNIK: ślad po new Child() → [Child: inicjalizacja]
        // Odczyt Child.parentName inicjalizuje tylko klasę, która DEKLARUJE pole (Parent). Child startuje przy new.

        show("nazwa binarna (getName)", type.getName());           // getName = pobierz (pełną) nazwę
        // WYNIK: nazwa binarna (getName) → t26_jvm.Jvm02ClassLoadingInit$Lazy
        // Klasa zagnieżdżona to osobna klasa z osobnym plikiem (Jvm02ClassLoadingInit$Lazy.class) i własnym
        // momentem inicjalizacji — inicjalizacja klasy zewnętrznej jej nie obejmuje.

        // PUŁAPKA: „klasa jest w projekcie, więc jej blok static już się wykonał” — nie. JVM inicjalizuje leniwie,
        //   żeby start był szybki. Blok static może wykonać się późno (przy pierwszym żądaniu klienta) albo wcale —
        //   np. rejestracja „w bloku static podklasy” nie zadziała, gdy kod czyta tylko pola static nadklasy.
        // Podgląd: java -Xlog:class+load=info ... (albo -verbose:class) wypisuje każdą ładowaną klasę i jej źródło;
        //   „shared objects file” = archiwum CDS (współdzielenie danych klas, domyślnie od Javy 12, szybszy start).
    }

    // =================================================================================================
    // 2. KOLEJNOŚĆ INICJALIZACJI — TEKST ŹRÓDŁOWY, NADKLASA PIERWSZA
    // =================================================================================================

    /** Base = baza (nadklasa). Każdy element melduje się w śladzie. */
    static class Base {
        static String baseStatic = traced("Base: pole static");
        static { trace("Base: blok static"); }
        String baseInstance = traced("Base: pole instancji");
        { trace("Base: blok instancji"); }
        Base() { trace("Base: konstruktor"); }
    }

    /** Derived = pochodna (podklasa). Dwa bloki static przedzielone polem — kolejność jak w tekście. */
    static class Derived extends Base {
        static { trace("Derived: blok static nr 1"); }
        static String derivedStatic = traced("Derived: pole static");
        static { trace("Derived: blok static nr 2"); }
        String derivedInstance = traced("Derived: pole instancji");

        Derived() {
            super();                                // niejawnie i tak byłoby pierwsze
            trace("Derived: konstruktor");
        }
    }

    /**
     * 2. Inicjalizacja klasy: najpierw nadklasa, potem podklasa; w obrębie klasy pola static i bloki static
     * w kolejności z tekstu. Tworzenie obiektu: konstruktor nadklasy (z jej polami i blokami instancji), potem
     * pola i bloki instancji podklasy, na końcu reszta konstruktora podklasy.
     */
    static void initializationOrder() {
        section("2. Kolejność inicjalizacji — tekst źródłowy, nadklasa pierwsza");

        takeTrace();
        new Derived();
        showEach("ślad pierwszego new Derived()", takeTrace());
        // WYNIK: ślad pierwszego new Derived() (liczba elementów: 10):
        // WYNIK:    • Base: pole static
        // WYNIK:    • Base: blok static
        // WYNIK:    • Derived: blok static nr 1
        // WYNIK:    • Derived: pole static
        // WYNIK:    • Derived: blok static nr 2
        // WYNIK:    • Base: pole instancji
        // WYNIK:    • Base: blok instancji
        // WYNIK:    • Base: konstruktor
        // WYNIK:    • Derived: pole instancji
        // WYNIK:    • Derived: konstruktor

        new Derived();
        show("ślad drugiego new Derived() — ile wpisów", takeTrace().size());
        // WYNIK: ślad drugiego new Derived() — ile wpisów → 5
        // Drugi obiekt: już tylko 5 wpisów „instancji” (pola, blok, konstruktory) — część static była jednorazowa.

        // Skąd ta kolejność? javac skleja wszystkie inicjalizatory pól static i bloki static (w kolejności z tekstu)
        // w jedną ukrytą metodę <clinit> (class init = inicjalizacja klasy). Inicjalizatory pól instancji
        // i bloki instancji wkleja do KAŻDEGO konstruktora — zaraz po wywołaniu super(...).
        // PUŁAPKA: przestawienie bloku static nad pole (albo pod nie) zmienia działanie programu, bo kolejność
        //   w tekście JEST kolejnością wykonania. Porządkując kod „dla urody”, łatwo niechcący zmienić logikę.
        // DOBRA PRAKTYKA: jeden blok static na klasę (albo żaden) i proste inicjalizatory — wtedy kolejność
        //   widać od razu i nie trzeba jej odtwarzać z pamięci.
    }

    // =================================================================================================
    // 3. STAŁE CZASU KOMPILACJI — WKLEJANE PRZEZ KOMPILATOR
    // =================================================================================================

    /** Config = konfiguracja: dwie stałe czasu kompilacji i jedna wartość liczona w czasie działania. */
    static class Config {
        static final int LIMIT = 5;                                  // static final + typ prosty + stałe wyrażenie
        static final String LABEL = "sklep";                         // LABEL = etykieta; String z literałem = stała
        static final int COMPUTED_LIMIT = Integer.parseInt("5");     // computed = wyliczony; wynik metody → NIE stała
        static { trace("Config: inicjalizacja"); }
    }

    /**
     * 3. Stała czasu kompilacji (constant variable = zmienna stała): pole final typu prostego albo String,
     * zainicjalizowane wyrażeniem stałym. javac WKLEJA jej wartość w miejsce użycia, więc odczyt w ogóle
     * nie dotyka klasy Config. Pole liczone w czasie działania wymaga inicjalizacji klasy.
     */
    static void compileTimeConstants() {
        section("3. Stałe czasu kompilacji — wklejane przez kompilator");

        takeTrace();
        show("Config.LIMIT", Config.LIMIT);
        show("Config.LABEL", Config.LABEL);
        show("ślad po odczycie stałych", takeTrace());
        // WYNIK: Config.LIMIT → 5
        // WYNIK: Config.LABEL → sklep
        // WYNIK: ślad po odczycie stałych → []

        show("Config.COMPUTED_LIMIT", Config.COMPUTED_LIMIT);
        show("ślad po odczycie COMPUTED_LIMIT", takeTrace());
        // WYNIK: Config.COMPUTED_LIMIT → 5
        // WYNIK: ślad po odczycie COMPUTED_LIMIT → [Config: inicjalizacja]

        // Stałe: static final int X = 5; static final long MS = 60L * 1000; static final String S = "a" + "b". NIE-stałe:
        //   static final Integer X = 5 (typ opakowujący), static final int X = compute(), List.of(...), pole bez final.
        // PUŁAPKA: wartość stałej jest skopiowana do KAŻDEJ klasy, która jej używa. Gdy biblioteka zmieni
        //   public static final int LIMIT = 5 na 10, a Twój kod nie zostanie przekompilowany, nadal będzie
        //   używał 5 — bo „5” siedzi w Twoim pliku .class. Stałe publiczne zmieniaj tylko z pełną rekompilacją.
        // DOBRA PRAKTYKA: public static final tylko dla wartości naprawdę stałych (liczba dni tygodnia, nazwa
        //   nagłówka). Wartość, która może się zmieniać między wersjami, udostępnij metodą — metoda nie jest wklejana.
    }

    // =================================================================================================
    // 4. Class.forName KONTRA .class
    // =================================================================================================

    /** ForNameTarget = cel wyszukiwania po nazwie. */
    static class ForNameTarget {
        static { trace("ForNameTarget: inicjalizacja"); }
    }

    /** NotYet = jeszcze nie: załadujemy ją bez inicjalizacji, a zainicjalizujemy później; touch = dotknij. */
    static class NotYet {
        static { trace("NotYet: inicjalizacja"); }
        static void touch() { trace("NotYet: touch()"); }
    }

    /**
     * 4. Class.forName(nazwa) ładuje I INICJALIZUJE klasę (używa class loadera klasy wywołującej).
     * Wersja trzyargumentowa z false tylko ładuje. X.class — tylko ładuje.
     */
    static void classForName() throws ClassNotFoundException {
        section("4. Class.forName kontra .class");

        String outer = Jvm02ClassLoadingInit.class.getName();          // outer = zewnętrzna: "t26_jvm.Jvm02ClassLoadingInit"
        takeTrace();
        Class<?> found = Class.forName(outer + "$ForNameTarget");     // found = znaleziona; forName = po nazwie
        show("ślad po Class.forName(nazwa)", takeTrace());
        show("ten sam obiekt co ForNameTarget.class", found == ForNameTarget.class);
        // WYNIK: ślad po Class.forName(nazwa) → [ForNameTarget: inicjalizacja]
        // WYNIK: ten sam obiekt co ForNameTarget.class → true

        ClassLoader loader = Jvm02ClassLoadingInit.class.getClassLoader();   // getClassLoader = pobierz loader
        Class.forName(outer + "$NotYet", false, loader);                     // false = NIE inicjalizuj
        show("ślad po Class.forName(nazwa, false, loader)", takeTrace());
        NotYet.touch();
        show("ślad po NotYet.touch()", takeTrace());
        // WYNIK: ślad po Class.forName(nazwa, false, loader) → []
        // WYNIK: ślad po NotYet.touch() → [NotYet: inicjalizacja, NotYet: touch()]

        // PUŁAPKA: klasa zagnieżdżona ma w nazwie binarnej znak $, a nie kropkę — kropka oznacza pakiet.
        expectThrows("Class.forName z kropką przed nazwą klasy zagnieżdżonej",
                () -> Class.forName(outer + ".NotYet"));
        // WYNIK: ✔ Class.forName z kropką przed nazwą klasy zagnieżdżonej → rzucono ClassNotFoundException: t26_jvm.Jvm02ClassLoadingInit.NotYet

        // Stary kod JDBC: Class.forName("com.mysql.jdbc.Driver") — tylko po to, żeby blok static sterownika go
        // zarejestrował. Od JDBC 4.0 (Java 6) sterowniki z JAR-a wykrywa ServiceLoader (ładowacz usług) — zbędne.
        // DOBRA PRAKTYKA: nazwy klas czytane z konfiguracji ładuj przez forName już przy STARCIE aplikacji —
        //   literówka albo brak JAR-a (ClassNotFoundException, wyjątek sprawdzany) wyjdzie od razu, a nie u klienta.
    }

    // =================================================================================================
    // 5. PUŁAPKI KOLEJNOŚCI — WARTOŚCI DOMYŚLNE WIDOCZNE „ZA WCZEŚNIE”
    // =================================================================================================

    /** Counters = liczniki: singleton tworzony w PIERWSZEJ linii inicjalizacji, zanim reszta pól dostanie wartości. */
    static class Counters {
        static final Counters INSTANCE = new Counters();   // konstruktor działa, gdy pola niżej mają jeszcze 0
        static int created;                                // created = utworzone; bez inicjalizatora → zostaje wynik konstruktora
        static int createdWithZero = 0;                    // with zero = z zerem: „= 0” wykona się PO konstruktorze i skasuje wynik

        Counters() {
            created++;
            createdWithZero++;
        }
    }

    /**
     * 5. Przygotowanie ustawia pola static na 0/null/false, a inicjalizacja nadpisuje je po kolei. Kod, który
     * wykona się „w środku” tej kolejności, zobaczy wartości domyślne.
     */
    static void orderPitfalls() {
        section("5. Pułapki kolejności — wartości domyślne widoczne za wcześnie");

        show("Counters.created", Counters.created);
        show("Counters.createdWithZero", Counters.createdWithZero);
        // WYNIK: Counters.created → 1
        // WYNIK: Counters.createdWithZero → 0
        // Podobnie: static int first = readSecond(); static int second = 10; — readSecond() zobaczy 0. Zapis wprost
        // first = second; się nie skompiluje (niedozwolone odwołanie w przód), ale przez metodę javac tego nie wykryje.

        // PUŁAPKA: singleton tworzony w pierwszej linii widzi resztę pól static z wartościami domyślnymi, a późniejszy
        //   inicjalizator „= 0” po cichu kasuje to, co konstruktor zdążył ustawić. Kod wygląda poprawnie, wynik nie.
        // To samo dzieje się z obiektami: metoda nadpisana w podklasie, wywołana z konstruktora nadklasy, widzi pola
        //   podklasy jako null/0 — ich inicjalizatory wykonają się dopiero po powrocie z super().
        // DOBRA PRAKTYKA: w konstruktorach wołaj tylko metody private, static albo final. Singleton trzymaj
        //   na KOŃCU pól static albo użyj idiomu holder (sekcja 7) — wtedy problem kolejności znika.
    }

    // =================================================================================================
    // 6. BŁĄD W BLOKU STATIC — ExceptionInInitializerError, POTEM NoClassDefFoundError
    // =================================================================================================

    /** Broken = zepsuta: inicjalizacja pola static rzuca wyjątek (np. brak pliku konfiguracji); read port = odczytaj port. */
    static class Broken {
        static final int PORT = readPort();
        static int readPort() { throw new IllegalStateException("brak pliku konfiguracji"); }
    }

    /**
     * 6. Wyjątek z inicjalizacji klasy JVM opakowuje w ExceptionInInitializerError. Klasa zostaje oznaczona jako
     * błędna i KAŻDA następna próba użycia (w tym samym class loaderze) kończy się NoClassDefFoundError.
     */
    static void failedInitialization() {
        section("6. Błąd w bloku static — ExceptionInInitializerError, potem NoClassDefFoundError");

        try {
            show("Broken.PORT", Broken.PORT);
        } catch (ExceptionInInitializerError error) {
            show("1. próba → błąd", error.getClass().getSimpleName());
            show("przyczyna (getCause)", error.getCause());
        }
        // WYNIK: 1. próba → błąd → ExceptionInInitializerError
        // WYNIK: przyczyna (getCause) → java.lang.IllegalStateException: brak pliku konfiguracji

        expectThrows("2. próba: Broken.PORT", () -> show("Broken.PORT", Broken.PORT));
        // WYNIK: ✔ 2. próba: Broken.PORT → rzucono NoClassDefFoundError: Could not initialize class t26_jvm.Jvm02ClassLoadingInit$Broken

        // Klasa NIE spróbuje zainicjalizować się ponownie, nawet gdy plik się pojawi — pomoże tylko restart programu.
        // PUŁAPKA: w logach widać zwykle dziesiątki NoClassDefFoundError „Could not initialize class”, które sugerują
        //   brak pliku .class. A plik jest! Przyczyna siedzi w PIERWSZYM błędzie (ExceptionInInitializerError z cause).
        // Nie mylić: ClassNotFoundException = forName/loadClass nie znalazł klasy o danej nazwie (sekcja 4);
        //   NoClassDefFoundError = klasa była przy kompilacji, a w czasie działania jej brak albo jej inicjalizacja padła.
        // DOBRA PRAKTYKA: w blokach static nie rób rzeczy, które mogą się nie udać (pliki, sieć, baza).
        //   Przenieś je do zwykłej metody lub konstruktora, gdzie wyjątek da się obsłużyć i ponowić.
    }

    // =================================================================================================
    // 7. IDIOM HOLDER — LENIWY I BEZPIECZNY WĄTKOWO SINGLETON
    // =================================================================================================

    /** HeavyService = ciężka usługa: kosztowna w tworzeniu, więc tworzymy ją dopiero, gdy ktoś jej użyje. */
    static final class HeavyService {
        static { trace("HeavyService: inicjalizacja klasy"); }
        private HeavyService() { trace("HeavyService: konstruktor (drogie!)"); }
        static String version() { return "2.1"; }                     // version = wersja

        static HeavyService getInstance() {   // get instance = pobierz instancję
            return Holder.INSTANCE;           // pierwsze użycie Holder → inicjalizacja Holder → new HeavyService()
        }

        /** Holder = przechowalnia: osobna klasa, więc jej inicjalizacja startuje dopiero przy getInstance(). */
        private static final class Holder {
            static final HeavyService INSTANCE = new HeavyService();
        }
    }

    /**
     * 7. Initialization-on-demand holder (inicjalizacja na żądanie przez przechowalnię): instancja siedzi w polu
     * static klasy zagnieżdżonej. JVM inicjalizuje ją leniwie, dokładnie raz i bezpiecznie wątkowo.
     */
    static void holderIdiom() {
        section("7. Idiom holder — leniwy i bezpieczny wątkowo singleton");

        takeTrace();
        show("HeavyService.version()", HeavyService.version());
        show("ślad", takeTrace());
        // WYNIK: HeavyService.version() → 2.1
        // WYNIK: ślad → [HeavyService: inicjalizacja klasy]

        HeavyService first = HeavyService.getInstance();
        show("ślad po 1. getInstance()", takeTrace());
        HeavyService second = HeavyService.getInstance();
        show("ślad po 2. getInstance()", takeTrace());
        show("first == second", first == second);
        // WYNIK: ślad po 1. getInstance() → [HeavyService: konstruktor (drogie!)]
        // WYNIK: ślad po 2. getInstance() → []
        // WYNIK: first == second → true

        // Dlaczego bezpieczne wątkowo bez synchronized? Specyfikacja (JLS 12.4.2) każe JVM inicjalizować klasę pod
        // blokadą: gdy dwa wątki naraz pierwszy raz użyją Holder, jeden wykona inicjalizację, a drugi POCZEKA
        // i zobaczy gotowy obiekt. Kolejne wywołania getInstance() to zwykły odczyt pola — bez kosztu blokady.
        // Alternatywy: enum z jedną stałą (t08_enums) albo double-checked locking (wymaga volatile, łatwo zepsuć).
        // PUŁAPKA: dwie klasy, których bloki static nawzajem się potrzebują, mogą się ZAKLESZCZYĆ (deadlock), gdy
        //   dwa wątki zaczną je inicjalizować naraz z dwóch stron — bo każdy wątek czeka na blokadę drugiej klasy.
        // DOBRA PRAKTYKA: holder, gdy obiekt jest kosztowny i nie zawsze potrzebny; zwykłe pole static final,
        //   gdy obiekt jest tani — leniwość bez potrzeby to tylko dodatkowa klasa do czytania.
    }

    // =================================================================================================
    // 8. CLASS LOADERY — BOOTSTRAP, PLATFORM, APP
    // =================================================================================================

    /**
     * 8. W Javie 17 są trzy wbudowane class loadery. Bootstrap (w kodzie widoczny jako null) ładuje rdzeń JDK
     * (m.in. moduł java.base), platform — część modułów JDK (np. java.sql), app — Twoje klasy z classpath.
     */
    static void classLoaders() {
        section("8. Class loadery — bootstrap, platform, app");

        show("String.class.getClassLoader()", String.class.getClassLoader());
        ClassLoader app = Jvm02ClassLoadingInit.class.getClassLoader();   // app = loader aplikacji
        show("loader tej lekcji — getName()", app.getName());
        show("jego rodzic (getParent)", app.getParent().getName());
        show("rodzic rodzica", app.getParent().getParent());
        show("loader java.sql.Date", java.sql.Date.class.getClassLoader().getName());
        // WYNIK: String.class.getClassLoader() → null
        // WYNIK: loader tej lekcji — getName() → app
        // WYNIK: jego rodzic (getParent) → platform
        // WYNIK: rodzic rodzica → null
        // WYNIK: loader java.sql.Date → platform
        // Uwaga: „app” widać przy zwykłym uruchomieniu z classpath (IDE, java -cp). Serwery aplikacji czy systemy
        //   wtyczek często ładują kod własnymi loaderami — wtedy nazwa i łańcuch rodziców są inne.

        // Delegowanie do rodzica (parent delegation): loader app, pytany o klasę, najpierw oddaje pytanie w górę
        // (platform → bootstrap), a sam szuka dopiero, gdy wyżej jej nie ma (w Javie 9+ loadery dodatkowo znają
        // podział pakietów na moduły). Efekt: własna klasa java.lang.String na classpath nigdy nie zastąpi prawdziwej.
        // Tożsamość klasy = nazwa + loader, który ją zdefiniował. Ta sama klasa wczytana przez dwa loadery to dla
        //   JVM dwie RÓŻNE klasy — rzutowanie między nimi rzuca ClassCastException „X cannot be cast to X”.
        // Usuwanie klas (unloading): klasa znika z metaspace dopiero wtedy, gdy GC może usunąć jej class loader.
        //   Klasy loaderów bootstrap, platform i app żyją do końca programu.
        // PUŁAPKA: na serwerach aplikacji (ponowne wdrożenie bez restartu) jeden zapomniany odnośnik do starej klasy
        //   (np. wątek, ThreadLocal, rejestr static w JDK) trzyma stary class loader i WSZYSTKIE jego klasy →
        //   metaspace rośnie przy każdym wdrożeniu, aż do OutOfMemoryError: Metaspace.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Cykl: ładowanie → łączenie (weryfikacja, przygotowanie: static = 0/null/false, rozwiązanie) → inicjalizacja.
     *   • Inicjalizacja = <clinit>: pola static + bloki static w kolejności z tekstu; nadklasa pierwsza; RAZ.
     *   • Nowy obiekt: konstruktor nadklasy → pola i bloki instancji podklasy → reszta konstruktora podklasy.
     *   • Inicjalizuje: new, metoda static, pole static (nie-stała), Class.forName(nazwa), refleksja, podklasa.
     *   • Nie inicjalizuje: X.class, tablica X[], instanceof, stała czasu kompilacji, forName(nazwa, false, loader).
     *   • Stałe (static final prymityw/String = wyrażenie stałe) są wklejane → zmiana wymaga rekompilacji użytkowników.
     *   • Wyjątek w static → ExceptionInInitializerError, potem zawsze NoClassDefFoundError „Could not initialize class”.
     *   • Holder: private static final class Holder { static final X INSTANCE = new X(); } — leniwie, raz, bezpiecznie.
     *   • Loadery: bootstrap (null), platform, app; delegowanie do rodzica; klasa = nazwa + loader.
     *
     * PYTANIA KONTROLNE:
     *   1. Co robi etap przygotowania (preparation) i dlaczego czasem widać w polu static 0 zamiast wartości z kodu?
     *   2. Które operacje inicjalizują klasę T: T.class, new T[3], T.run() (static), odczyt static final int X = 5,
     *      odczyt static final Integer Y = 5, Class.forName("pakiet.T")?
     *   3. Co wypisze:  class A { static { System.out.print("A"); } static final int X = 1; static int y = 2; }
     *      System.out.print(A.X); System.out.print(A.y);  ?
     *   4. Co wypisze:  class P { static { System.out.print("P"); } static int v = 1; }
     *      class C extends P { static { System.out.print("C"); } }   System.out.print(C.v);  ?
     *   5. ZNAJDŹ BŁĄD:  Class.forName("com.shop.Outer.Inner")  rzuca ClassNotFoundException, choć klasa istnieje.
     *   6. ZNAJDŹ BŁĄD:  static final Map<String, String> CONFIG = readFile("app.conf");  — w logach setki
     *      NoClassDefFoundError: Could not initialize class. Co się stało i jak to przebudować?
     *   7. Dlaczego String.class.getClassLoader() zwraca null i czy własny java.lang.String na classpath go podmieni?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Puzzle = łamigłówka do ćwiczenia 2: singleton na górze, pola i blok static niżej. */
    static class Puzzle {
        static final Puzzle INSTANCE = new Puzzle();
        static int a;
        static int b = 5;
        static int c;
        static { c++; }

        Puzzle() {
            a++;
            b++;
            c = b;
        }
    }

    static void exercises() {
        List<Integer> puzzleValues = List.of(Puzzle.a, Puzzle.b, Puzzle.c);  // prawdziwe wartości z JVM
        Map<String, String> superclassOf = Map.of("Animal", "Object", "Dog", "Animal",
                "Puppy", "Dog", "Cat", "Animal");                            // superclass of = nadklasa klasy
        List<List<String>> expectedOrders = List.of(List.of("Animal", "Dog", "Puppy", "Cat"), List.of("Animal", "Cat"));
        List<String> expectedSettings = List.of("version()", "--", "tworzę ustawienia", "ta sama instancja: true");

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: leniwe ustawienia", expectedSettings, () -> exercise1());
        Check.equal("ćw. 2: wartości Puzzle.a, b, c", puzzleValues, () -> exercise2());
        Check.equal("ćw. 3: kolejność inicjalizacji", expectedOrders,
                () -> List.of(exercise3(superclassOf, List.of("Puppy", "Cat", "Dog", "Puppy")),
                        exercise3(superclassOf, List.of("Cat"))));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", expectedSettings, () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", puzzleValues, () -> solution2());
        Check.equal("ćw. 3 (wzorzec)", expectedOrders,
                () -> List.of(solution3(superclassOf, List.of("Puppy", "Cat", "Dog", "Puppy")),
                        solution3(superclassOf, List.of("Cat"))));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD
    }

    /** ExerciseSettings = ustawienia do ćwiczenia 1 — WERSJA ZACHŁANNA, do przepisania. */
    static final class ExerciseSettings {
        // TODO: przepisz na idiom holder (ćwiczenie 1)
        private static final ExerciseSettings INSTANCE = new ExerciseSettings();
        private ExerciseSettings() { trace("tworzę ustawienia"); }
        static ExerciseSettings getInstance() { return INSTANCE; }
        static String version() { trace("version()"); return "1.0"; }
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ klasę ExerciseSettings z wersji zachłannej na idiom holder, tak żeby
     * wywołanie version() NIE tworzyło instancji, a tworzyło ją dopiero pierwsze getInstance().
     * Stary sposób (obecny kod):
     * <pre>{@code
     * private static final ExerciseSettings INSTANCE = new ExerciseSettings();   // tworzone już przy version()!
     * static ExerciseSettings getInstance() { return INSTANCE; }
     * }</pre>
     * Nowy sposób: przenieś pole INSTANCE do {@code private static final class Holder} i zwracaj Holder.INSTANCE.
     * Metody exercise1 nie zmieniaj — to scenariusz testu. Oczekiwany ślad:
     * [version(), --, tworzę ustawienia, ta sama instancja: true].
     * Podpowiedź: wzór masz w sekcji 7 (HeavyService).
     */
    static List<String> exercise1() {
        takeTrace();
        ExerciseSettings.version();
        trace("--");
        ExerciseSettings first = ExerciseSettings.getInstance();
        trace("ta sama instancja: " + (first == ExerciseSettings.getInstance()));
        return takeTrace();
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEWIDŹ wartości Puzzle.a, Puzzle.b, Puzzle.c po inicjalizacji klasy Puzzle
     * i zwróć je jako List.of(a, b, c). Test porównuje je z wartościami odczytanymi z JVM.
     * Uwaga: komunikat ✘ pokaże prawdziwe wartości — dlatego NAJPIERW rozpisz przewidywanie na kartce, potem
     * uruchom i sprawdź, w którym kroku się pomyliłeś (to jest właściwa nauka).
     * Podpowiedź: przygotowanie ustawia wszystko na 0, potem idź po kolei liniami klasy — pierwsza woła konstruktor.
     */
    static List<Integer> exercise2() {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zasymuluj JVM. Dostajesz mapę „klasa → jej nadklasa” oraz listę pierwszych użyć
     * klas (w kolejności). Zwróć listę klas w kolejności, w jakiej JVM je ZAINICJALIZUJE. Zasady: każda klasa
     * inicjalizuje się najwyżej raz; przed klasą inicjalizuje się jej nadklasa (rekurencyjnie); "Object" jest już
     * zainicjalizowany i nie trafia do wyniku.
     * Przykład: {Animal→Object, Dog→Animal, Puppy→Dog, Cat→Animal}, użycia [Puppy, Cat, Dog, Puppy] → [Animal, Dog, Puppy, Cat].
     * Podpowiedź: metoda pomocnicza initialize(nazwa, zbiórGotowych, wynik): jeśli nazwa to Object albo jest
     * już w zbiorze — wróć; inaczej najpierw initialize(nadklasa, ...), potem dodaj nazwę do zbioru i wyniku.
     */
    static List<String> exercise3(Map<String, String> superclassOf, List<String> firstUses) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    /** SolutionSettings = ustawienia w wersji z idiomem holder (wzorzec do ćwiczenia 1). */
    static final class SolutionSettings {
        private SolutionSettings() { trace("tworzę ustawienia"); }
        static SolutionSettings getInstance() { return Holder.INSTANCE; }
        static String version() { trace("version()"); return "1.0"; }

        private static final class Holder {
            static final SolutionSettings INSTANCE = new SolutionSettings();
        }
    }

    static List<String> solution1() {
        takeTrace();
        SolutionSettings.version();
        trace("--");
        SolutionSettings first = SolutionSettings.getInstance();
        trace("ta sama instancja: " + (first == SolutionSettings.getInstance()));
        return takeTrace();
    }

    static List<Integer> solution2() {
        // przygotowanie: a = b = c = 0; INSTANCE = new Puzzle(): a = 1, b = 1, c = b = 1;
        // static int a; — bez inicjalizatora, zostaje 1; b = 5 nadpisuje 1; c bez inicjalizatora — 1; blok: c++ → 2.
        return List.of(1, 5, 2);
    }

    static List<String> solution3(Map<String, String> superclassOf, List<String> firstUses) {
        Set<String> initialized = new HashSet<>();
        List<String> order = new ArrayList<>();
        for (String name : firstUses) {
            initializeClass(name, superclassOf, initialized, order);
        }
        return order;
    }

    /** initializeClass = zainicjalizuj klasę: najpierw nadklasa (rekurencja), potem sama klasa — najwyżej raz. */
    private static void initializeClass(String name, Map<String, String> superclassOf,
                                        Set<String> initialized, List<String> order) {
        if (name.equals("Object") || initialized.contains(name)) {
            return;
        }
        initializeClass(superclassOf.get(name), superclassOf, initialized, order);
        initialized.add(name);
        order.add(name);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Przygotowanie przydziela miejsce na pola static i ustawia je na wartości domyślne (0, false, null).
     *      Właściwe wartości z kodu wpisuje dopiero inicjalizacja, po kolei wg tekstu. Kod wykonany „w środku”
     *      (konstruktor singletona z pierwszej linii, metoda czytająca pole zadeklarowane niżej) widzi jeszcze 0.
     *   2. Inicjalizują: T.run(), odczyt static final Integer Y (Integer to nie typ prosty, więc to nie stała),
     *      Class.forName("pakiet.T"). Nie inicjalizują: T.class, new T[3], odczyt stałej X = 5 (wklejona przez javac).
     *   3. "1A2" — A.X to stała (wklejona 1, bez inicjalizacji A); A.y inicjalizuje A (wypisuje "A"), potem 2.
     *   4. "P1" — v jest zadeklarowane w P, więc inicjalizuje się tylko P; blok static C się nie wykona.
     *   5. Nazwa binarna klasy zagnieżdżonej używa $: Class.forName("com.shop.Outer$Inner").
     *   6. readFile rzucił wyjątek w czasie inicjalizacji klasy → za pierwszym razem ExceptionInInitializerError
     *      (z prawdziwą przyczyną w cause), a potem przy KAŻDYM użyciu NoClassDefFoundError — klasa jest oznaczona
     *      jako błędna do końca działania programu. Szukaj w logach pierwszego błędu. Konfigurację wczytuj w zwykłej
     *      metodzie/konstruktorze (z obsługą błędu i możliwością ponowienia), z jasnym komunikatem.
     *   7. String ładuje bootstrap class loader, który w kodzie Javy jest reprezentowany przez null (to część samej
     *      JVM, a nie obiekt ClassLoader). Nie podmieni: delegowanie do rodzica sprawia, że java.lang.String zawsze
     *      pochodzi z java.base, a definiowanie klas w pakietach java.* zwykłymi loaderami jest zabronione.
     */
    // </editor-fold>
}
