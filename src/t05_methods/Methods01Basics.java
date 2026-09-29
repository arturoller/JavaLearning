package t05_methods;

import helpers.Check;

import java.util.Locale;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Metody — budowa, parametry, zwracanie wyniku
 *        (method = metoda; parameter = parametr; return = zwróć; void = nic, pustka)
 *
 * W SKRÓCIE:
 *   Metoda to nazwany kawałek kodu: dostaje dane (parametry), wykonuje pracę i może oddać wynik (return).
 *   Zamiast pisać ten sam kod w pięciu miejscach, piszesz go raz w metodzie i wywołujesz po nazwie.
 *   Metoda bez wyniku ma typ void. Parametry są KOPIAMI argumentów (t01_basics/Basics09PassByValue).
 *
 * ANALOGIA: przepis w książce kucharskiej.
 *   „Ciasto drożdżowe” to nazwa przepisu (metody). Składniki to parametry (mąka, cukier), a upieczone ciasto to wynik
 *   (return). W innych przepisach piszesz tylko „zrób ciasto drożdżowe (wg strony 12)” — zamiast przepisywać go od nowa.
 *
 * JAK TO DZIAŁA:
 *   static   double      grossPrice  (double net, int vatRate)   {  return net * (1 + vatRate / 100.0);  }
 *   │        │           │            │                              │
 *   │        typ wyniku  nazwa        parametry (typ + nazwa)        ciało; return oddaje wynik i KOŃCZY metodę
 *   static = metoda klasy (wołamy ją bez tworzenia obiektu — obiekty: t06_oop_basics)
 *
 *   Wywołanie:  double price = grossPrice(100.0, 23);   ← argumenty trafiają do parametrów w tej samej kolejności
 *
 * SŁÓWKA:
 *   method = metoda; call / invoke = wywołać; parameter = parametr (w definicji); argument = argument (przy wywołaniu);
 *   return type = typ zwracany; void = nic (brak wyniku); body = ciało (metody); signature = sygnatura (nazwa + parametry);
 *   call stack = stos wywołań; side effect = efekt uboczny; gross / net = brutto / netto.
 *
 * ZOBACZ TEŻ: t05_methods/Methods02Overloading (ta sama nazwa, różne parametry), t05_methods/Methods04GoodPractices
 *             (jak pisać dobre metody), t01_basics/Basics09PassByValue (parametry to kopie), t06_oop_basics/Oop04Static.
 * </pre>
 */
public class Methods01Basics {

    public static void main(String[] args) {
        title("Methods01 — budowa metody");

        callingMethods();           // calling methods = wywoływanie metod
        parametersAndArguments();   // parameters and arguments = parametry i argumenty
        returnStatement();          // return statement = instrukcja return
        voidVersusReturning();      // void versus returning = void kontra zwracanie wyniku
        localScope();               // local scope = zasięg lokalny
        callStack();                // call stack = stos wywołań
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. WYWOŁYWANIE METOD
    // =================================================================================================

    /** grossPrice = cena brutto. Przyjmuje cenę netto i stawkę VAT w procentach, zwraca cenę brutto. */
    static double grossPrice(double net, int vatRate) {
        return net * (1 + vatRate / 100.0);          // 100.0 — żeby nie było dzielenia całkowitego (Basics04Operators)
    }

    /** 1. Metodę wołamy po nazwie, w nawiasach podając argumenty. Wynik można zapisać, wypisać albo od razu użyć dalej. */
    static void callingMethods() {
        section("1. Wywołanie metody i użycie wyniku");

        double price = grossPrice(100.0, 23);                         // zapisz wynik do zmiennej
        show("grossPrice(100.0, 23)", price);
        show("suma dwóch cen brutto", grossPrice(100.0, 23) + grossPrice(50.0, 8));   // użyj wyniku w wyrażeniu
        // WYNIK: grossPrice(100.0, 23) → 123.0
        // WYNIK: suma dwóch cen brutto → 177.0

        // DOBRA PRAKTYKA: gdy stawka VAT się zmieni, poprawiasz JEDNO miejsce — ciało metody. Bez metody szukałbyś wzoru
        //   we wszystkich miejscach programu.
    }

    // =================================================================================================
    // 2. PARAMETRY I ARGUMENTY
    // =================================================================================================

    /** describe = opisz. Kolejność parametrów ma znaczenie — argumenty trafiają do nich po kolei. */
    static String describe(String name, int age) {
        return name + " ma " + age + " lat";
    }

    /** half = połowa. Parametr typu double przyjmie też int (poszerzanie typu — Basics05Casting). */
    static double half(double value) {
        return value / 2;
    }

    /** 2. Parametr = zmienna w definicji metody. Argument = konkretna wartość podana przy wywołaniu. */
    static void parametersAndArguments() {
        section("2. Parametry i argumenty");

        show("describe(\"Ala\", 30)", describe("Ala", 30));
        // WYNIK: describe("Ala", 30) → Ala ma 30 lat

        show("half(7) — int poszerzony do double", half(7));
        // WYNIK: half(7) — int poszerzony do double → 3.5

        // PUŁAPKA: describe(30, "Ala") się nie skompiluje — typy argumentów muszą pasować do parametrów w tej kolejności.
        // PUŁAPKA: gdy dwa parametry mają ten sam typ (np. width, height), kompilator NIE wychwyci zamiany kolejności —
        //   rectangleArea(height, width) policzy „coś”, ale może to być błąd. Nazywaj parametry jednoznacznie.
    }

    // =================================================================================================
    // 3. RETURN
    // =================================================================================================

    /** sign = znak liczby. Kilka instrukcji return — każda natychmiast KOŃCZY metodę. */
    static String sign(int number) {
        if (number > 0) {
            return "dodatnia";
        }
        if (number < 0) {
            return "ujemna";
        }
        return "zero";                 // bez tej linii: błąd kompilacji „missing return statement” (brak return)
    }

    /**
     * 3. return oddaje wynik i kończy metodę — kod po nim w tej gałęzi się nie wykona. Metoda z typem wyniku MUSI
     * zwrócić wartość na KAŻDEJ ścieżce (kompilator to sprawdza).
     */
    static void returnStatement() {
        section("3. return — oddaj wynik i zakończ metodę");

        show("sign(5)", sign(5));
        show("sign(-3)", sign(-3));
        show("sign(0)", sign(0));
        // WYNIK: sign(5) → dodatnia
        // WYNIK: sign(-3) → ujemna
        // WYNIK: sign(0) → zero
    }

    // =================================================================================================
    // 4. VOID KONTRA ZWRACANIE WYNIKU
    // =================================================================================================

    /** printGreeting = wypisz powitanie. void — nic nie zwraca, tylko WYPISUJE (to efekt uboczny). */
    static void printGreeting(String name) {
        System.out.println("   Cześć, " + name + "!");
    }

    /** greeting = powitanie. ZWRACA gotowy napis — co z nim zrobić, decyduje wywołujący. */
    static String greeting(String name) {
        return "Cześć, " + name + "!";
    }

    /**
     * 4. void = metoda nic nie zwraca (np. tylko wypisuje). Metoda zwracająca wynik jest bardziej uniwersalna:
     * wynik można wypisać, zapisać do pliku, wysłać e-mailem albo sprawdzić w teście.
     */
    static void voidVersusReturning() {
        section("4. void kontra zwracanie wyniku");

        printGreeting("Ola");
        // WYNIK: Cześć, Ola!
        String text = greeting("Ola");
        show("długość powitania", text.length());
        // WYNIK: długość powitania → 11

        // String x = printGreeting("Ola");  → błąd kompilacji: void nie ma wartości, której można by użyć.
        // DOBRA PRAKTYKA: metody, które LICZĄ, niech zwracają wynik; wypisywanie zostaw na sam koniec (w main).
        //   Takie metody łatwo sprawdzić (np. Check.equal w ćwiczeniach) i użyć ponownie.
    }

    // =================================================================================================
    // 5. ZASIĘG ZMIENNYCH W METODZIE
    // =================================================================================================

    /** doubleIt = podwój. Zmiana parametru działa na KOPII — wywołujący tego nie zobaczy. */
    static int doubleIt(int value) {
        value = value * 2;             // zmieniamy kopię
        return value;
    }

    /** 5. Parametry i zmienne lokalne istnieją tylko w czasie wykonywania metody. Każde wywołanie ma własne kopie. */
    static void localScope() {
        section("5. Zmienne lokalne i parametry to kopie");

        int original = 21;
        int doubled = doubleIt(original);
        show("original / doubled", original + " / " + doubled);
        // WYNIK: original / doubled → 21 / 42    ← original bez zmian (parametr był kopią)
    }

    // =================================================================================================
    // 6. STOS WYWOŁAŃ
    // =================================================================================================

    /** levelA/B/C = poziom A/B/C. Metody wołające kolejne metody — wypisują, kiedy wchodzą i wychodzą. */
    static void levelA() {
        System.out.println("   → wchodzę do A");
        levelB();
        System.out.println("   ← wychodzę z A");
    }

    static void levelB() {
        System.out.println("      → wchodzę do B");
        levelC();
        System.out.println("      ← wychodzę z B");
    }

    static void levelC() {
        System.out.println("         → jestem w C (najgłębiej)");
    }

    /**
     * 6. Każde wywołanie metody odkłada „ramkę” na stos wywołań (call stack): parametry i zmienne lokalne tej metody.
     * Po return ramka jest zdejmowana i wracamy DOKŁADNIE tam, skąd metodę wywołano. Stack trace wyjątku to właśnie
     * wydruk tego stosu (t10_exceptions/Exceptions01Basics).
     */
    static void callStack() {
        section("6. Stos wywołań — kto kogo woła");

        levelA();
        // WYNIK: → wchodzę do A
        // WYNIK: → wchodzę do B
        // WYNIK: → jestem w C (najgłębiej)
        // WYNIK: ← wychodzę z B
        // WYNIK: ← wychodzę z A
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • static typWyniku nazwa(typ param1, typ param2) { ... return wynik; }
     *   • void = brak wyniku; typ wyniku ≠ void → return na KAŻDEJ ścieżce (inaczej „missing return statement”).
     *   • return kończy metodę natychmiast.
     *   • Argumenty trafiają do parametrów po kolei; typy muszą pasować (int → double przejdzie, odwrotnie nie).
     *   • Parametry i zmienne lokalne to kopie — żyją tylko w czasie wywołania.
     *   • Metody liczące niech ZWRACAJĄ wynik; wypisywanie na końcu.
     *   • Stos wywołań: każde wywołanie to nowa ramka; return wraca tam, skąd wywołano.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się parametr od argumentu?
     *   2. Co wypisze:  static int f(int x) { x = x + 1; return x * 2; }   int a = 3; int b = f(a); System.out.println(a + " " + b);  ?
     *   3. ZNAJDŹ BŁĄD:  static int max(int a, int b) { if (a > b) { return a; } }
     *   4. Dlaczego metoda, która ZWRACA wynik, jest zwykle lepsza od takiej, która go tylko wypisuje?
     *   5. Co się stanie z kodem po  return  w tej samej gałęzi metody?
     *   6. Co się stanie, gdy w wywołaniu describe("Ala", 30) z sekcji 2 zamienisz kolejność: describe(30, "Ala")?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: max(4, 9, 7)", 9, () -> exercise1(4, 9, 7));
        Check.equal("ćw. 2: 25°C w Fahrenheitach", "77.0°F", () -> exercise2(25.0));
        Check.equal("ćw. 3a: ocena za 95 pkt", "celujący", () -> exercise3(95));
        Check.equal("ćw. 3b: ocena za 40 pkt", "niedostateczny", () -> exercise3(40));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 9, () -> solution1(4, 9, 7));
        Check.equal("ćw. 2 (wzorzec)", "77.0°F", () -> solution2(25.0));
        Check.equal("ćw. 3a (wzorzec)", "celujący", () -> solution3(95));
        Check.equal("ćw. 3b (wzorzec)", "niedostateczny", () -> solution3(40));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć największą z trzech liczb.
     * Podpowiedź: {@code Math.max(a, Math.max(b, c))} albo dwa if-y.
     */
    static int exercise1(int a, int b, int c) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): zamień stopnie Celsjusza na Fahrenheita i zwróć napis z jednym miejscem po kropce i „°F”.
     * Wzór: F = C × 9 / 5 + 32. Dla 25.0 → "77.0°F".
     * Podpowiedź: {@code String.format(Locale.ROOT, "%.1f°F", f)}; uważaj: 9 / 5 na liczbach int to 1!
     */
    static String exercise2(double celsius) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zamień punkty (0–100) na ocenę: 90+ „celujący”, 75+ „bardzo dobry”, 60+ „dobry”,
     * 50+ „dostateczny”, poniżej 50 „niedostateczny”.
     * Podpowiedź: łańcuch if z wczesnym return — sprawdzaj od NAJWYŻSZEGO progu (t02_controlflow/Control01IfElse).
     */
    static String exercise3(int points) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int a, int b, int c) {
        return Math.max(a, Math.max(b, c));
    }

    static String solution2(double celsius) {
        double fahrenheit = celsius * 9 / 5 + 32;     // celsius jest double, więc całe działanie jest na double
        return String.format(Locale.ROOT, "%.1f°F", fahrenheit);
    }

    static String solution3(int points) {
        if (points >= 90) {
            return "celujący";
        }
        if (points >= 75) {
            return "bardzo dobry";
        }
        if (points >= 60) {
            return "dobry";
        }
        if (points >= 50) {
            return "dostateczny";
        }
        return "niedostateczny";
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Parametr to zmienna w DEFINICJI metody (int age); argument to konkretna wartość przy WYWOŁANIU (30).
     *   2. „3 8” — a zostaje 3 (parametr był kopią), f zwraca (3 + 1) * 2 = 8.
     *   3. Gdy a <= b, metoda nie zwraca niczego — błąd kompilacji „missing return statement”. Dodaj return b; na końcu.
     *   4. Bo wynik można dalej wykorzystać (zapisać, porównać, przetestować), a wypisanie to tylko jedna z możliwości.
     *   5. Nie wykona się — return natychmiast kończy metodę (kompilator zgłosi „unreachable statement”, jeśli kod
     *      bezpośrednio po return jest nieosiągalny).
     *   6. Nie skompiluje się — pierwszy parametr to String, a podano int (i odwrotnie).
     */
    // </editor-fold>
}
