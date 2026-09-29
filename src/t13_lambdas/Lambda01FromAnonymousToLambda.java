package t13_lambdas;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Employee;
import helpers.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Od klasy anonimowej do lambdy — jak przekazać metodzie ZACHOWANIE
 *        (from anonymous to lambda = od klasy anonimowej do lambdy; lambda = funkcja anonimowa, bez nazwy)
 *
 * W SKRÓCIE:
 *   Czasem metoda potrzebuje nie tylko DANYCH, ale też ZACHOWANIA: „według czego posortować?”,
 *   „które elementy zostawić?”, „co zrobić z każdym elementem?”. W Javie zachowanie przekazujemy jako
 *   OBIEKT, który implementuje interfejs z jedną metodą. Kiedyś trzeba było w tym celu napisać całą klasę
 *   (nazwaną albo anonimową). Od Javy 8 wystarczy lambda — krótki zapis: parametry, strzałka, wynik.
 *
 * ANALOGIA: karteczka dla kuriera.
 *   Kurier (metoda) zna całą procedurę doręczenia, ale nie wie, co zrobić, gdy nikogo nie ma w domu.
 *   • Klasa nazwana — zatrudniasz pracownika na etat, z teczką osobową, tylko po to, żeby przekazał
 *     kurierowi jedno zdanie.
 *   • Klasa anonimowa — pracownik tymczasowy: bez teczki, ale formularz nadal ma trzy strony.
 *   • Lambda — przyklejasz na paczce karteczkę „zostaw u sąsiada”. Kurier sam zdecyduje, KIEDY z niej skorzystać.
 *
 * JAK TO DZIAŁA:
 *   Składnia lambdy:
 *     {@code (parametry) -> wyrażenie}                       ← wynik to wartość wyrażenia
 *     {@code (parametry) -> { instrukcje; return wynik; }}   ← ciało blokowe: klamry, średniki, return
 *
 *     {@code (Product p) -> p.inStock()}   ← pełny zapis: typ parametru, strzałka, wynik
 *     {@code p -> p.inStock()}             ← typ wydedukuje kompilator; przy JEDNYM parametrze nawiasy są zbędne
 *
 *   Lambdę można napisać tylko tam, gdzie Java oczekuje INTERFEJSU FUNKCYJNEGO — interfejsu z dokładnie
 *   jedną metodą abstrakcyjną (Lambda02FunctionalInterfaces). Kompilator patrzy na to miejsce (typ docelowy,
 *   target type), sprawdza, jaki interfejs jest tam wymagany, i z niego bierze typy parametrów oraz typ wyniku.
 *   Lambda = ciało tej jednej metody. Nic więcej.
 *
 * SŁÓWKA:
 *   anonymous = anonimowy (bez nazwy); behaviour = zachowanie; filter = filtr, filtruj; accept = przyjmij, przepuść;
 *   arrow = strzałka; expression = wyrażenie; body = ciało (metody, lambdy); block = blok (kod w klamrach);
 *   target type = typ docelowy; infer = wydedukować (typ); comparator = komparator (obiekt porównujący);
 *   compare = porównaj; runnable = „do uruchomienia”; run = uruchom; forEach = dla każdego;
 *   removeIf = usuń, jeśli; replaceAll = zamień wszystkie; sort = sortuj; cheap = tani; in stock = na stanie.
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop07NestedClasses (klasy zagnieżdżone i anonimowe),
 *             t07_inheritance_polymorphism/Inherit04Interfaces (interfejsy),
 *             t12_collections/Collections07ComparableComparator (Comparator),
 *             Lambda02FunctionalInterfaces (czym dokładnie jest interfejs funkcyjny).
 * </pre>
 */
public class Lambda01FromAnonymousToLambda {

    /** HUNDRED = sto. Stała BigDecimal tworzona RAZ — nie w każdej lambdzie od nowa. */
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public static void main(String[] args) {
        title("Lambda01 — od klasy anonimowej do lambdy");

        duplicatedLoops();         // duplicated loops = powielone pętle (problem)
        namedClass();              // named class = klasa nazwana (krok 1)
        anonymousClass();          // anonymous class = klasa anonimowa (krok 2)
        lambdaExpression();        // lambda expression = wyrażenie lambda (krok 3)
        syntaxVariants();          // syntax variants = warianty składni
        targetTyping();            // target typing = typ docelowy (skąd kompilator zna typy)
        comparatorAndRunnable();   // comparator and runnable = Comparator i Runnable
        firstRealUses();           // first real uses = pierwsze prawdziwe zastosowania
        whatLambdaCannotDo();      // what lambda cannot do = czego lambda nie zastąpi
        exercises();               // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PROBLEM: POWIELONE PĘTLE
    // =================================================================================================

    /**
     * 1. Dwie metody, które różnią się tylko WARUNKIEM w if.
     * <p>
     * Zobacz niżej cheapProductNames i inStockProductNames — pętla, lista wynikowa, add, return: wszystko
     * identyczne. Inny jest tylko jeden warunek. Każde nowe pytanie („z kategorii X?”, „droższe niż Y?”)
     * to kolejna kopia pętli.
     */
    static void duplicatedLoops() {
        section("1. Problem: dwie metody różnią się tylko warunkiem");

        List<Product> products = SampleData.products();
        show("tańsze niż 100 zł", cheapProductNames(products));
        show("na stanie (liczba)", inStockProductNames(products).size());
        // WYNIK: tańsze niż 100 zł → [Kawa ziarnista 1kg, Czekolada gorzka, Oliwa z oliwek, Czysty kod, Wzorce projektowe, T-shirt bawełniany]
        // WYNIK: na stanie (liczba) → 12

        // To łamie zasadę DRY (Don't Repeat Yourself = nie powtarzaj się): błąd w pętli trzeba by poprawiać
        // w każdej kopii osobno. Chcielibyśmy napisać pętlę RAZ, a warunek podać jako PARAMETR.
        // Tylko jak przekazać do metody „kawałek kodu”? Parametr to przecież wartość: liczba, napis, obiekt...
        // Odpowiedź Javy: opakuj kod w OBIEKT, który ma metodę z tym kodem. Sekcje 2–4 pokazują trzy sposoby.
        note("Pętla jest ta sama — różni się tylko warunek. Warunek to ZACHOWANIE, które chcemy przekazać.");
        // WYNIK: ℹ Pętla jest ta sama — różni się tylko warunek. Warunek to ZACHOWANIE, które chcemy przekazać.
    }

    /** cheapProductNames = nazwy tanich produktów (cena poniżej 100 zł). compareTo = porównaj z (t15_numbers). */
    private static List<String> cheapProductNames(List<Product> products) {
        List<String> result = new ArrayList<>();                 // result = wynik
        for (Product p : products) {
            if (p.price().compareTo(HUNDRED) < 0) {              // ← JEDYNA różnica
                result.add(p.name());
            }
        }
        return result;
    }

    /** inStockProductNames = nazwy produktów na stanie. */
    private static List<String> inStockProductNames(List<Product> products) {
        List<String> result = new ArrayList<>();
        for (Product p : products) {
            if (p.inStock()) {                                   // ← JEDYNA różnica
                result.add(p.name());
            }
        }
        return result;
    }

    // =================================================================================================
    // 2. KROK 1: KLASA NAZWANA
    // =================================================================================================

    /**
     * ProductFilter = filtr produktów. NASZ interfejs z jedną metodą: „czy przepuścić ten produkt?”.
     * To „gniazdko”, do którego podłączymy różne warunki.
     */
    interface ProductFilter {
        boolean accept(Product product);   // accept = przyjmij, przepuść
    }

    /**
     * filterNames = przefiltruj i zwróć nazwy. Pętla napisana RAZ — warunek przychodzi z zewnątrz
     * jako obiekt ProductFilter. Metoda nie wie, JAKI to warunek — po prostu woła accept.
     */
    static List<String> filterNames(List<Product> products, ProductFilter filter) {
        List<String> result = new ArrayList<>();
        for (Product p : products) {
            if (filter.accept(p)) {            // wywołujemy „zachowanie”, które ktoś nam przekazał
                result.add(p.name());
            }
        }
        return result;
    }

    /** CheapFilter = filtr tanich produktów. Zwykła klasa (nazwana) implementująca interfejs. */
    static class CheapFilter implements ProductFilter {
        @Override
        public boolean accept(Product product) {
            return product.price().compareTo(HUNDRED) < 0;
        }
    }

    /**
     * 2. Krok 1: warunek w osobnej klasie. Metoda filterNames dostaje OBIEKT tej klasy.
     */
    static void namedClass() {
        section("2. Krok 1: klasa nazwana implementująca interfejs");

        ProductFilter cheap = new CheapFilter();                  // cheap = tani
        show("tanie (klasa CheapFilter)", filterNames(SampleData.products(), cheap));
        // WYNIK: tanie (klasa CheapFilter) → [Kawa ziarnista 1kg, Czekolada gorzka, Oliwa z oliwek, Czysty kod, Wzorce projektowe, T-shirt bawełniany]

        // Plus: pętla jest tylko raz (w filterNames).
        // Minus: na KAŻDY warunek potrzebna osobna klasa (5+ linii), zwykle użyta w jednym miejscu,
        // a jej kod leży daleko od miejsca użycia — czytając filterNames(..., cheap), musisz szukać, co robi CheapFilter.
    }

    // =================================================================================================
    // 3. KROK 2: KLASA ANONIMOWA
    // =================================================================================================

    /**
     * 3. Krok 2: klasa anonimowa — klasa bez nazwy, zdefiniowana i utworzona w jednym miejscu.
     * Zapis {@code new Interfejs() { ... }} znaczy: „utwórz obiekt klasy bez nazwy, która implementuje Interfejs”.
     */
    static void anonymousClass() {
        section("3. Krok 2: klasa anonimowa — w miejscu użycia");

        List<String> books = filterNames(SampleData.products(), new ProductFilter() {
            @Override
            public boolean accept(Product product) {
                return product.category() == Category.KSIAZKI;    // enumy porównujemy przez ==
            }
        });
        show("książki (klasa anonimowa)", books);
        // WYNIK: książki (klasa anonimowa) → [Czysty kod, Java. Podstawy, Wzorce projektowe]

        // Lepiej: warunek jest TAM, gdzie go używamy. Ale policz linie: z sześciu tylko JEDNA niesie treść
        // (return ...). Reszta to „ceremonia”: new ProductFilter(), @Override, public boolean accept(Product product),
        // klamry. Kompilator i tak to wszystko wie — interfejs ma przecież tylko jedną metodę.
    }

    // =================================================================================================
    // 4. KROK 3: LAMBDA
    // =================================================================================================

    /**
     * 4. Krok 3: lambda — zostawiamy tylko parametr i wynik.
     * <p>
     * Jak z klasy anonimowej zrobić lambdę? Skreśl wszystko, co kompilator już wie:
     * <pre>{@code
     *   new ProductFilter() {                           ← skreśl: typ wynika z parametru metody filterNames
     *       @Override                                    ← skreśl: metoda jest jedna, nie ma w czym się pomylić
     *       public boolean accept(Product product) {     ← skreśl nazwę metody i typy — są w interfejsie
     *           return product.category() == KSIAZKI;    ← zostaw parametr i wynik
     *       }
     *   }
     *   ZOSTAJE:   product -> product.category() == KSIAZKI
     * }</pre>
     */
    static void lambdaExpression() {
        section("4. Krok 3: lambda — tylko parametr, strzałka i wynik");

        List<Product> products = SampleData.products();

        // Ten sam warunek co w sekcji 3 — jedna linijka zamiast sześciu:
        show("książki (lambda)", filterNames(products, p -> p.category() == Category.KSIAZKI));
        // WYNIK: książki (lambda) → [Czysty kod, Java. Podstawy, Wzorce projektowe]

        // Jedna metoda filterNames, dowolnie wiele warunków — każdy w miejscu użycia:
        show("ponad 100 sztuk", filterNames(products, p -> p.stock() > 100));
        show("brak na stanie", filterNames(products, p -> !p.inStock()));
        // WYNIK: ponad 100 sztuk → [Kawa ziarnista 1kg, Czekolada gorzka]
        // WYNIK: brak na stanie → [Smartfon X, Oliwa z oliwek]

        // Lambdę można też zapisać w zmiennej i nazwać — nazwa dokumentuje warunek:
        BigDecimal thousand = new BigDecimal("1000");             // stała poza lambdą — tworzona raz
        ProductFilter expensive = p -> p.price().compareTo(thousand) > 0;   // expensive = drogi
        show("drogie (ponad 1000 zł)", filterNames(products, expensive));
        // WYNIK: drogie (ponad 1000 zł) → [Laptop Pro 14, Smartfon X, Monitor 27 cali, Ekspres do kawy]

        // Jak czytać  p -> p.stock() > 100 :  „dla produktu p zwróć: czy stan p jest większy niż 100”.
        // Po lewej od strzałki -> są PARAMETRY, po prawej WYNIK. Nazwa p jest dowolna (jak nazwa parametru metody).
        // DOBRA PRAKTYKA: w krótkich lambdach wystarczy jedna litera (p — produkt, e — pracownik, w — słowo).
        //   W dłuższych użyj pełnej nazwy (product), żeby kod czytał się jak zdanie.

        // Lambda NIE jest wykonywana w chwili zapisania. To „przepis” przekazany do filterNames —
        // wykona go dopiero filterNames, raz dla każdego produktu (tu 14 razy).
    }

    // =================================================================================================
    // 5. WARIANTY SKŁADNI
    // =================================================================================================

    /** Greeting = powitanie. Metoda bez parametrów, zwraca napis. text = tekst. */
    interface Greeting {
        String text();
    }

    /** IntCalculator = kalkulator liczb całkowitych: dwa int na wejściu, int na wyjściu. calculate = oblicz. */
    interface IntCalculator {
        int calculate(int a, int b);
    }

    /**
     * 5. Wszystkie odmiany zapisu lambdy — od zera do wielu parametrów, z wyrażeniem albo z blokiem.
     */
    static void syntaxVariants() {
        section("5. Warianty składni lambdy");

        // a) ZERO parametrów — puste nawiasy są OBOWIĄZKOWE:
        Greeting hello = () -> "Dzień dobry";
        show("() -> wyrażenie", hello.text());
        // WYNIK: () -> wyrażenie → Dzień dobry

        // b) JEDEN parametr — nawiasy można pominąć (obie wersje są poprawne):
        ProductFilter available1 = p -> p.inStock();
        ProductFilter available2 = (p) -> p.inStock();
        Product laptop = SampleData.products().get(0);
        show("p -> ... oraz (p) -> ...", available1.accept(laptop) + " / " + available2.accept(laptop));
        // WYNIK: p -> ... oraz (p) -> ... → true / true

        // c) KILKA parametrów — nawiasy obowiązkowe, parametry po przecinku:
        IntCalculator add = (a, b) -> a + b;
        show("(a, b) -> a + b dla 2 i 3", add.calculate(2, 3));
        // WYNIK: (a, b) -> a + b dla 2 i 3 → 5

        // d) Jawne typy parametrów — wtedy WSZYSTKIE parametry muszą mieć typ:
        IntCalculator max = (int a, int b) -> a > b ? a : b;     // operator trójargumentowy: warunek ? tak : nie
        show("(int a, int b) -> ... dla 7 i 4", max.calculate(7, 4));
        // WYNIK: (int a, int b) -> ... dla 7 i 4 → 7
        // PUŁAPKA: mieszanie nie przejdzie:  (int a, b) -> ...  →  błąd kompilacji:
        //   „invalid lambda parameter declaration (cannot mix implicitly-typed and explicitly-typed parameters)”
        //   = „niepoprawna deklaracja parametrów lambdy (nie można mieszać parametrów z typem i bez typu)”.

        // e) var zamiast typu (Java 11+) — przydaje się rzadko (np. gdy chcesz dodać adnotację do parametru):
        IntCalculator multiply = (var a, var b) -> a * b;
        show("(var a, var b) -> a * b dla 6 i 7", multiply.calculate(6, 7));
        // WYNIK: (var a, var b) -> a * b dla 6 i 7 → 42

        // f) CIAŁO BLOKOWE — gdy potrzebujesz kilku instrukcji: klamry, średniki i return (jak w metodzie):
        IntCalculator sumOfSquares = (a, b) -> {                  // sum of squares = suma kwadratów
            int x = a * a;
            int y = b * b;
            return x + y;
        };
        show("blok z return dla 3 i 4", sumOfSquares.calculate(3, 4));
        // WYNIK: blok z return dla 3 i 4 → 25

        // g) Lambda, która nic nie zwraca (void) — Runnable z java.lang: run() nie ma parametrów ani wyniku.
        Runnable printer = () -> System.out.println("   lambda typu void: tylko wypisuje");
        printer.run();
        // WYNIK: lambda typu void: tylko wypisuje

        // PUŁAPKA: w ciele blokowym brak return (albo średnika) to błąd kompilacji:
        //   (a, b) -> { a + b; }        →  „not a statement” (= „to nie jest instrukcja”) — brakuje return
        //   (a, b) -> { return a + b }  →  „';' expected” (= „oczekiwano średnika”)
        // PUŁAPKA: w wersji z wyrażeniem NIE piszemy return:  (a, b) -> return a + b;  →  „illegal start of expression”
        //   (= „niedozwolony początek wyrażenia”).
        // Zasada: bez klamer — samo wyrażenie; z klamrami — pełne instrukcje, jak w zwykłej metodzie.
    }

    // =================================================================================================
    // 6. TYP DOCELOWY — SKĄD KOMPILATOR ZNA TYPY
    // =================================================================================================

    /** TextJoiner = łącznik napisów: dwa String na wejściu, String na wyjściu. join = połącz. */
    interface TextJoiner {
        String join(String a, String b);
    }

    /**
     * 6. Lambda sama w sobie NIE MA typu. Typ nadaje jej MIEJSCE, w którym stoi (target typing = nadawanie
     * typu przez cel): zmienna, parametr metody, wartość zwracana, rzutowanie.
     */
    static void targetTyping() {
        section("6. Typ docelowy — ten sam tekst lambdy, różne znaczenia");

        // Identyczny tekst  (a, b) -> a + b  — ale inny interfejs, więc inne typy i inne działanie:
        IntCalculator numbers = (a, b) -> a + b;                  // a, b to int  → dodawanie
        TextJoiner texts = (a, b) -> a + b;                       // a, b to String → sklejanie napisów
        show("IntCalculator: 2 + 3", numbers.calculate(2, 3));
        show("TextJoiner: \"2\" + \"3\"", texts.join("2", "3"));
        // WYNIK: IntCalculator: 2 + 3 → 5
        // WYNIK: TextJoiner: "2" + "3" → 23

        // Skąd kompilator wie? Patrzy na typ zmiennej po lewej: IntCalculator ma metodę calculate(int, int),
        // więc a i b MUSZĄ być int. To samo z parametrem metody: filterNames(..., ProductFilter filter)
        // — dlatego w  filterNames(products, p -> ...)  p jest typu Product, choć nigdzie tego nie napisaliśmy.

        // Bez typu docelowego lambda NIE MA SENSU:
        //   Object o = () -> {};   →  błąd: „incompatible types: Object is not a functional interface”
        //                             (= „niezgodne typy: Object nie jest interfejsem funkcyjnym”)
        //   Kompilator nie wie, jaki interfejs ma zrealizować ta lambda. Rozwiązanie — rzutowanie, które podaje typ:
        Object task = (Runnable) () -> System.out.println("   uruchomiono Runnable schowany w zmiennej Object");
        show("task instanceof Runnable", task instanceof Runnable);
        // WYNIK: task instanceof Runnable → true
        ((Runnable) task).run();
        // WYNIK: uruchomiono Runnable schowany w zmiennej Object
        // (W praktyce rzutowania lambd prawie nie ma. Wrócimy do niego przy przeciążonych metodach — Lambda08Pitfalls.
        //  Tam też, dlaczego  var f = x -> x * 2;  się nie kompiluje.)
    }

    // =================================================================================================
    // 7. COMPARATOR I RUNNABLE
    // =================================================================================================

    /**
     * 7. Dwa interfejsy funkcyjne znane sprzed Javy 8, które od razu zyskały na lambdach:
     * {@code Comparator<T>} (compare = porównaj: dwa obiekty → liczba ujemna / 0 / dodatnia)
     * i Runnable (run = uruchom: nic na wejściu, nic na wyjściu).
     */
    static void comparatorAndRunnable() {
        section("7. Comparator i Runnable — klasa anonimowa kontra lambda");

        List<Employee> employees = new ArrayList<>(SampleData.employees());   // kopia, bo SampleData = List.of

        // PRZED: klasa anonimowa — sortuj po wieku rosnąco
        employees.sort(new Comparator<Employee>() {
            @Override
            public int compare(Employee a, Employee b) {
                return Integer.compare(a.age(), b.age());         // Integer.compare = porównaj dwie liczby int
            }
        });
        show("najmłodsza trójka (klasa anonimowa)", employees.subList(0, 3));
        // WYNIK: najmłodsza trójka (klasa anonimowa) → [Piotr Kowalski (IT, 9800 zł), Agnieszka Zielińska (MARKETING, 7600 zł), Ewa Woźniak (IT, 12100 zł)]

        // PO: lambda — sortuj po pensji MALEJĄCO (zamieniamy a i b miejscami)
        employees.sort((a, b) -> Integer.compare(b.salary(), a.salary()));
        show("najlepiej zarabiająca trójka (lambda)", employees.subList(0, 3));
        // WYNIK: najlepiej zarabiająca trójka (lambda) → [Michał Lewandowski (IT, 17200 zł), Anna Nowak (IT, 14500 zł), Ewa Woźniak (IT, 12100 zł)]

        // PUŁAPKA: kusi zapis  (a, b) -> a.age() - b.age().  Dla wieku działa, ale dla dużych liczb odejmowanie
        //   może PRZEPEŁNIĆ int (wynik zmieni znak) i sortowanie się rozsypie. Integer.compare jest zawsze bezpieczne.
        // DOBRA PRAKTYKA: jeszcze czytelniej:  Comparator.comparingInt(Employee::age)  — Lambda05Composition.

        // Runnable: „kod do uruchomienia później”. Samo utworzenie lambdy NICZEGO nie wykonuje:
        Runnable reminder = () -> System.out.println("   Przypomnienie: zrób kopię zapasową!");   // reminder = przypomnienie
        note("Lambda utworzona — ale jeszcze nic się nie wypisało.");
        // WYNIK: ℹ Lambda utworzona — ale jeszcze nic się nie wypisało.
        reminder.run();                                            // dopiero teraz
        reminder.run();                                            // i jeszcze raz — ten sam kod, drugie wykonanie
        // WYNIK: Przypomnienie: zrób kopię zapasową!
        // WYNIK: Przypomnienie: zrób kopię zapasową!
        // Runnable przekazuje się np. wątkom:  new Thread(reminder).start()  — t21_concurrency/Concurrency01Threads.
    }

    // =================================================================================================
    // 8. PIERWSZE PRAWDZIWE ZASTOSOWANIA: sort, forEach, removeIf, replaceAll
    // =================================================================================================

    /**
     * 8. Metody kolekcji, które przyjmują lambdę. Wszystkie ZMIENIAJĄ listę, na której je wołasz
     * (w przeciwieństwie do streamów z t16_streams, które tworzą nowy wynik).
     */
    static void firstRealUses() {
        section("8. Pierwsze prawdziwe zastosowania: sort, forEach, removeIf, replaceAll");

        List<String> animals = new ArrayList<>(List.of("kot", "Ala", "pies", "zebra", "Ćma", "osa"));   // animals = zwierzęta

        // forEach = dla każdego — wykonaj lambdę dla każdego elementu (println w lambdzie to efekt uboczny — tu celowo):
        animals.subList(0, 2).forEach(a -> System.out.println("   • " + a));
        // WYNIK: • kot
        // WYNIK: • Ala

        // sort = posortuj — z lambdą porównującą (Comparator):
        animals.sort((a, b) -> a.compareTo(b));                    // compareTo = porządek naturalny napisów
        show("sort — porządek naturalny", animals);
        // WYNIK: sort — porządek naturalny → [Ala, kot, osa, pies, zebra, Ćma]
        // PUŁAPKA: „Ćma” jest NA KOŃCU. compareTo porównuje KODY znaków Unicode: wielkie litery łacińskie są przed
        //   małymi, a polskie litery (Ć, Ł, Ś...) — za wszystkimi literami łacińskimi. To nie jest polski alfabet!
        //   Do sortowania po polsku służy Collator (t12_collections/Collections07ComparableComparator).

        animals.sort((a, b) -> a.length() - b.length());           // po długości (krótkie napisy — odejmowanie jest bezpieczne)
        show("sort — po długości", animals);
        // WYNIK: sort — po długości → [Ala, kot, osa, Ćma, pies, zebra]
        // List.sort jest STABILNE: słowa o tej samej długości zachowują poprzednią kolejność (Ala, kot, osa, Ćma).

        // removeIf = usuń, jeśli — usuwa elementy, dla których lambda zwróci true:
        boolean removed = animals.removeIf(a -> a.length() > 3);   // zwraca true, jeśli coś usunięto
        show("removeIf (dłuższe niż 3 litery)", animals + ", coś usunięto? " + removed);
        // WYNIK: removeIf (dłuższe niż 3 litery) → [Ala, kot, osa, Ćma], coś usunięto? true

        // replaceAll = zamień wszystkie — każdy element zamienia na wynik lambdy:
        animals.replaceAll(a -> a.toUpperCase());                  // toUpperCase = na wielkie litery
        show("replaceAll (wielkie litery)", animals);
        // WYNIK: replaceAll (wielkie litery) → [ALA, KOT, OSA, ĆMA]

        // Map.forEach — lambda z DWOMA parametrami: klucz i wartość (TreeMap = mapa posortowana po kluczach):
        Map<String, Integer> stock = new TreeMap<>(Map.of("kawa", 120, "czekolada", 300, "oliwa", 0));
        stock.forEach((name, qty) -> System.out.println("   " + name + ": " + qty + " szt."));   // qty (quantity) = ilość
        // WYNIK: czekolada: 300 szt.
        // WYNIK: kawa: 120 szt.
        // WYNIK: oliwa: 0 szt.

        // PUŁAPKA: sort, removeIf i replaceAll ZMIENIAJĄ listę. Na liście niemodyfikowalnej (List.of, SampleData)
        //   rzucą wyjątek — dlatego wyżej zawsze robimy kopię: new ArrayList<>(...).
        expectThrows("removeIf na liście z SampleData (List.of)", () -> SampleData.words().removeIf(w -> w.length() > 4));
        // WYNIK: ✔ removeIf na liście z SampleData (List.of) → rzucono UnsupportedOperationException: (brak komunikatu)
        // DOBRA PRAKTYKA: forEach z lambdą nie jest „lepszą pętlą for”. Do zwykłego przejścia po liście pętla
        //   for-each jest równie dobra (i pozwala na break). forEach błyszczy, gdy akcję dostajesz z zewnątrz jako parametr.
    }

    // =================================================================================================
    // 9. CZEGO LAMBDA NIE ZASTĄPI
    // =================================================================================================

    /** Lifecycle = cykl życia. Interfejs z DWIEMA metodami abstrakcyjnymi — lambda tu nie pasuje. */
    interface Lifecycle {
        void start();   // start = uruchom

        void stop();    // stop = zatrzymaj
    }

    /**
     * 9. Lambda to skrót TYLKO dla jednej metody abstrakcyjnej, bez pól i bez własnej tożsamości.
     * Tam, gdzie tego za mało, nadal potrzebujesz klasy (anonimowej albo nazwanej).
     */
    static void whatLambdaCannotDo() {
        section("9. Czego lambda NIE zastąpi");

        // a) Interfejs z DWIEMA metodami abstrakcyjnymi. Która z nich miałaby być ciałem lambdy?
        //    Lifecycle l = () -> {};  →  błąd: „incompatible types: Lifecycle is not a functional interface
        //    multiple non-overriding abstract methods found in interface Lifecycle”
        //    (= „Lifecycle nie jest interfejsem funkcyjnym — znaleziono kilka metod abstrakcyjnych”).
        //    Klasa anonimowa radzi sobie bez problemu:
        Lifecycle engine = new Lifecycle() {                        // engine = silnik
            @Override
            public void start() {
                System.out.println("   silnik: start");
            }

            @Override
            public void stop() {
                System.out.println("   silnik: stop");
            }
        };
        engine.start();
        engine.stop();
        // WYNIK: silnik: start
        // WYNIK: silnik: stop

        // b) STAN (pola). Klasa anonimowa może mieć własne pole — np. licznik sprawdzeń. Lambda NIE MA pól.
        ProductFilter countingFilter = new ProductFilter() {        // counting = liczący
            private int checked = 0;                                // checked = sprawdzono (pole klasy anonimowej)

            @Override
            public boolean accept(Product product) {
                checked++;
                return product.category() == Category.DOM;
            }

            @Override
            public String toString() {
                return "sprawdzono produktów: " + checked;
            }
        };
        show("kategoria DOM", filterNames(SampleData.products(), countingFilter));
        show("stan filtra", countingFilter);
        // WYNIK: kategoria DOM → [Ekspres do kawy, Lampka biurkowa]
        // WYNIK: stan filtra → sprawdzono produktów: 14

        // c) Klasa ABSTRAKCYJNA — nawet z jedną metodą abstrakcyjną lambda jej nie zaimplementuje
        //    (lambda działa tylko z interfejsami — szczegóły: Lambda02FunctionalInterfaces, sekcja 7).
        // d) this — w klasie anonimowej this to obiekt klasy anonimowej; w lambdzie this to obiekt OTACZAJĄCY
        //    (lambda nie tworzy własnego „ja”). Szczegóły i przykład: Lambda06ClosuresScope, sekcja 8.

        // DOBRA PRAKTYKA: wybór jest prosty:
        //   • interfejs z jedną metodą abstrakcyjną i krótki kod bez stanu → lambda,
        //   • kilka metod, własne pola albo kod dłuższy niż kilka linii → klasa (anonimowa lub nazwana).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Zachowanie przekazujemy jako OBIEKT implementujący interfejs. Ewolucja zapisu:
     *       klasa nazwana → klasa anonimowa (new I() { ... }) → lambda (p -> ...).
     *   • Składnia:  () -> wyr    x -> wyr    (x) -> wyr    (a, b) -> wyr    (int a, int b) -> wyr
     *                (var a, var b) -> wyr (Java 11+)       (a, b) -> { instrukcje; return wynik; }
     *   • Bez klamer: samo wyrażenie, BEZ return. Z klamrami: pełne instrukcje, średniki i return (gdy jest wynik).
     *   • Typy parametrów: wszystkie jawne albo żaden. Jeden parametr bez typu → nawiasy można pominąć.
     *   • Lambda nie ma własnego typu — nadaje go MIEJSCE (typ docelowy): zmienna, parametr, return, rzutowanie.
     *   • Lambda tylko dla interfejsu z JEDNĄ metodą abstrakcyjną. Dwie metody, pola, klasa abstrakcyjna → klasa.
     *   • Utworzenie lambdy nic nie wykonuje; kod ruszy dopiero przy wywołaniu metody interfejsu (run, accept...).
     *   • list.sort(cmp), list.forEach(akcja), list.removeIf(warunek), list.replaceAll(zamiana), map.forEach((k, v) -> ...).
     *   • sort/removeIf/replaceAll ZMIENIAJĄ listę — na List.of rzucą UnsupportedOperationException.
     *   • Comparator: Integer.compare(a, b) zamiast a - b (przepełnienie!). compareTo napisów ≠ polski alfabet.
     *
     * PYTANIA KONTROLNE:
     *   1. Co znaczy „przekazać zachowanie jako parametr”? Podaj przykład z tej lekcji.
     *   2. Kiedy nawiasy wokół parametrów lambdy są obowiązkowe, a kiedy można je pominąć?
     *   3. Co wypisze ten kod?
     *          Runnable r = () -> System.out.println("A");
     *          System.out.println("B");
     *          r.run();
     *          r.run();
     *   4. ZNAJDŹ BŁĄD:
     *          Comparator<String> byLength = (a, b) -> { a.length() - b.length(); };
     *   5. ZNAJDŹ BŁĄD (kod się kompiluje, ale wybucha):
     *          List<Integer> list = List.of(5, 12, 7);
     *          list.removeIf(n -> n > 10);
     *   6. Skąd kompilator wie, jakiego typu są a i b w  words.sort((a, b) -> a.compareTo(b)),  skoro nigdzie
     *      tego nie napisaliśmy?
     *   7. Podaj dwie sytuacje, w których lambda NIE zastąpi klasy anonimowej.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /**
     * Uruchamia ćwiczenia: najpierw TWOJE rozwiązania (✘ dopóki nie uzupełnisz), potem wzorcowe (✔).
     */
    static void exercises() {
        List<String> expected2 = List.of("Michał Lewandowski", "Anna Nowak", "Ewa Woźniak", "Krzysztof Szymański",
                "Piotr Kowalski", "Paweł Dąbrowski", "Tomasz Wójcik", "Magdalena Kamińska", "Agnieszka Zielińska",
                "Katarzyna Wiśniewska");
        List<String> expected3 = List.of("Laptop Pro 14", "Słuchawki BT", "Monitor 27 cali", "Kawa ziarnista 1kg", "Kurtka zimowa");
        List<String> expected4 = List.of("kolekcja", "optional", "lambda", "rekord", "stream", "stream", "lista");

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: removeIf — zostaw liczby parzyste", List.of(8, 2, 10, 6, 4, 8), () -> exercise1(SampleData.numbers()));
        Check.equal("ćw. 2: klasa anonimowa → lambda (pensje malejąco)", expected2, () -> exercise2(SampleData.employees()));
        Check.equal("ćw. 3: wartość zapasu powyżej 5000 zł", expected3, () -> exercise3(SampleData.products()));
        Check.equal("ćw. 4: długie słowa, od najdłuższych, potem alfabetycznie", expected4, () -> exercise4(SampleData.words()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of(8, 2, 10, 6, 4, 8), () -> solution1(SampleData.numbers()));
        Check.equal("ćw. 2 (wzorzec)", expected2, () -> solution2(SampleData.employees()));
        Check.equal("ćw. 3 (wzorzec)", expected3, () -> solution3(SampleData.products()));
        Check.equal("ćw. 4 (wzorzec)", expected4, () -> solution4(SampleData.words()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć NOWĄ listę z samymi liczbami parzystymi (kolejność jak w źródle).
     * Podpowiedź: zrób kopię {@code new ArrayList<>(numbers)} i wywołaj na niej removeIf z lambdą,
     * która zwraca true dla liczb NIEparzystych ({@code n % 2 != 0}).
     */
    static List<Integer> exercise1(List<Integer> numbers) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (łatwe): PRZEPISZ KLASĘ ANONIMOWĄ NA LAMBDĘ. Poniższy kod sortuje kopię listy pracowników
     * po pensji MALEJĄCO i zwraca ich imiona i nazwiska. Napisz to samo, ale z lambdą zamiast klasy anonimowej.
     * <pre>{@code
     *   List<Employee> copy = new ArrayList<>(employees);
     *   copy.sort(new Comparator<Employee>() {
     *       @Override
     *       public int compare(Employee a, Employee b) {
     *           return Integer.compare(b.salary(), a.salary());
     *       }
     *   });
     *   List<String> names = new ArrayList<>();
     *   for (Employee e : copy) {
     *       names.add(e.name());
     *   }
     *   return names;
     * }</pre>
     * Podpowiedź: z klasy anonimowej zostają tylko parametry (a, b) i wyrażenie po return.
     * Piotr i Paweł zarabiają tyle samo (9800) — sort jest stabilny, więc Piotr (wcześniej na liście) zostanie przed Pawłem.
     */
    static List<String> exercise2(List<Employee> employees) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć nazwy produktów, których wartość zapasu (metoda stockValue() = cena × liczba sztuk)
     * jest WIĘKSZA niż 5000 zł. Użyj metody filterNames z tej lekcji i lambdy.
     * Podpowiedź: stała {@code new BigDecimal("5000")} POZA lambdą; porównanie {@code compareTo(...) > 0}.
     */
    static List<String> exercise3(List<Product> products) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): z kopii listy słów usuń słowa krótsze niż 5 liter, a pozostałe posortuj:
     * najpierw od NAJDŁUŻSZYCH, a przy tej samej długości — alfabetycznie (compareTo). Zwróć listę.
     * Podpowiedź: removeIf, a potem sort z lambdą o ciele BLOKOWYM: najpierw porównaj długości
     * (malejąco: {@code Integer.compare(b.length(), a.length())}); jeśli wynik nie jest 0 — zwróć go,
     * w przeciwnym razie zwróć {@code a.compareTo(b)}.
     */
    static List<String> exercise4(List<String> words) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<Integer> solution1(List<Integer> numbers) {
        List<Integer> copy = new ArrayList<>(numbers);            // kopia — oryginał (List.of) jest niemodyfikowalny
        copy.removeIf(n -> n % 2 != 0);                           // usuń nieparzyste = zostaw parzyste
        return copy;
    }

    static List<String> solution2(List<Employee> employees) {
        List<Employee> copy = new ArrayList<>(employees);
        copy.sort((a, b) -> Integer.compare(b.salary(), a.salary()));   // b przed a = malejąco
        List<String> names = new ArrayList<>();
        for (Employee e : copy) {                                  // zwykła pętla jest tu w pełni w porządku
            names.add(e.name());
        }
        return names;
    }

    static List<String> solution3(List<Product> products) {
        BigDecimal limit = new BigDecimal("5000");                // limit = granica; tworzona RAZ
        return filterNames(products, p -> p.stockValue().compareTo(limit) > 0);
    }

    static List<String> solution4(List<String> words) {
        List<String> copy = new ArrayList<>(words);
        copy.removeIf(w -> w.length() < 5);
        copy.sort((a, b) -> {
            int byLength = Integer.compare(b.length(), a.length());   // by length = według długości (malejąco)
            if (byLength != 0) {
                return byLength;
            }
            return a.compareTo(b);                                     // remis → alfabetycznie
        });
        return copy;
        // Krócej (Lambda05Composition):
        //   copy.sort(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Metoda dostaje nie tylko dane, ale też kod do wykonania (np. warunek) — opakowany w obiekt interfejsu.
     *      Przykład: filterNames(products, p -> p.stock() > 100) — pętla jest w metodzie, warunek przychodzi z zewnątrz.
     *   2. Obowiązkowe: przy zerze parametrów  ()  i przy dwóch lub więcej  (a, b),  a także przy jawnym typie
     *      (Product p)  lub  var.  Pominąć można tylko przy JEDNYM parametrze bez typu:  p -> ...
     *   3. B, A, A — utworzenie lambdy niczego nie wypisuje; kod rusza dopiero przy każdym run().
     *   4. Ciało blokowe wymaga instrukcji i return. Samo wyrażenie w klamrach to „not a statement”. Poprawnie:
     *      (a, b) -> a.length() - b.length()   albo   (a, b) -> { return a.length() - b.length(); }
     *      (a jeszcze lepiej Integer.compare(a.length(), b.length()) — bez ryzyka przepełnienia).
     *   5. List.of tworzy listę NIEMODYFIKOWALNĄ — removeIf rzuci UnsupportedOperationException.
     *      Trzeba najpierw zrobić kopię: new ArrayList<>(List.of(5, 12, 7)).
     *   6. Z typu docelowego: words to List<String>, więc sort oczekuje Comparator<String> (dokładniej
     *      Comparator<? super String>), a metoda compare(String, String) wyznacza typy a i b.
     *   7. Np. interfejs z dwiema metodami abstrakcyjnymi; potrzebne pole (stan) w obiekcie; klasa abstrakcyjna
     *      zamiast interfejsu; gdy this ma oznaczać sam obiekt implementujący.
     */
    // </editor-fold>
}
