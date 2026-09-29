package t05_methods;

import helpers.Check;

import java.util.Locale;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Przeciążanie metod — ta sama nazwa, różne parametry
 *        (overload = przeciążyć; overloading = przeciążanie; resolution = rozstrzygnięcie, wybór)
 *
 * W SKRÓCIE:
 *   W jednej klasie może być kilka metod o TEJ SAMEJ nazwie, jeśli różnią się listą parametrów (liczbą albo typami).
 *   Kompilator wybiera wersję na podstawie argumentów. Typ zwracany NIE wystarcza do rozróżnienia.
 *   Kolejność wyboru: dokładne dopasowanie → poszerzenie typu (int → long) → opakowanie (int → Integer) → varargs.
 *
 * ANALOGIA: przycisk „zapłać” w sklepie.
 *   Jeden przycisk, ale kasjer robi co innego zależnie od tego, co podasz: kartę, gotówkę albo telefon. Nazwa czynności
 *   ta sama („zapłać”), a sposób wykonania zależy od „argumentu”.
 *
 * JAK TO DZIAŁA:
 *   static int    sum(int a, int b)           ← sum(2, 3)
 *   static int    sum(int a, int b, int c)     ← sum(2, 3, 4)
 *   static double sum(double a, double b)     ← sum(2.5, 1.0)
 *   Sygnatura (signature) = nazwa + typy parametrów. Dwie metody w klasie nie mogą mieć identycznej sygnatury.
 *
 * SŁÓWKA:
 *   overload = przeciążyć; signature = sygnatura; exact match = dokładne dopasowanie; widening = poszerzanie typu;
 *   boxing = opakowanie (int → Integer); varargs = zmienna liczba argumentów; ambiguous = niejednoznaczny;
 *   most specific = najbardziej konkretny; delegate = przekazać (pracę innej metodzie).
 *
 * ZOBACZ TEŻ: t05_methods/Methods01Basics (budowa metody), t01_basics/Basics05Casting (poszerzanie typów),
 *             t01_basics/Basics06Wrappers (boxing), t03_arrays/Arrays05Varargs (varargs),
 *             t07_inheritance_polymorphism/Inherit02Override (nadpisywanie — to co innego niż przeciążanie!).
 * </pre>
 */
public class Methods02Overloading {

    public static void main(String[] args) {
        title("Methods02 — przeciążanie metod");

        sameNameDifferentParams();  // same name, different params = ta sama nazwa, różne parametry
        differentCount();           // different count = różna liczba parametrów
        resolutionOrder();          // resolution order = kolejność wyboru
        nullArgument();             // null argument = null jako argument
        overloadsInJdk();           // overloads in JDK = przeciążenia w bibliotece Javy
        delegation();               // delegation = przekazywanie pracy
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. TA SAMA NAZWA, RÓŻNE TYPY PARAMETRÓW
    // =================================================================================================

    /** info = informacja. Trzy wersje — kompilator wybiera po TYPIE argumentu. */
    static String info(int value) {
        return "int: " + value;
    }

    static String info(double value) {
        return "double: " + value;
    }

    static String info(String value) {
        return "String: " + value;
    }

    /** 1. Kompilator patrzy na typy argumentów i wybiera pasującą wersję — w czasie KOMPILACJI, nie działania. */
    static void sameNameDifferentParams() {
        section("1. Ta sama nazwa, różne typy parametrów");

        show("info(5)", info(5));
        show("info(5.0)", info(5.0));
        show("info(\"pięć\")", info("pięć"));
        // WYNIK: info(5) → int: 5
        // WYNIK: info(5.0) → double: 5.0
        // WYNIK: info("pięć") → String: pięć

        // PUŁAPKA: sam typ zwracany NIE rozróżnia metod:  static int x()  i  static String x()  w jednej klasie
        //   to błąd kompilacji („method x() is already defined”).
    }

    // =================================================================================================
    // 2. RÓŻNA LICZBA PARAMETRÓW
    // =================================================================================================

    /** sum = suma. Wersje z dwoma i trzema parametrami. */
    static int sum(int a, int b) {
        return a + b;
    }

    static int sum(int a, int b, int c) {
        return a + b + c;
    }

    /** 2. Metody mogą się różnić liczbą parametrów. */
    static void differentCount() {
        section("2. Różna liczba parametrów");

        show("sum(2, 3)", sum(2, 3));
        show("sum(2, 3, 4)", sum(2, 3, 4));
        // WYNIK: sum(2, 3) → 5
        // WYNIK: sum(2, 3, 4) → 9
    }

    // =================================================================================================
    // 3. KOLEJNOŚĆ WYBORU: dokładne → poszerzenie → opakowanie → varargs
    // =================================================================================================

    // exact = dokładny: wersje dla int i long
    static String exact(int x) {
        return "int";
    }

    static String exact(long x) {
        return "long";
    }

    // pick = wybierz: poszerzenie (long) kontra opakowanie (Integer)
    static String pick(long x) {
        return "long (poszerzenie)";
    }

    static String pick(Integer x) {
        return "Integer (opakowanie)";
    }

    // choose = wybierz: opakowanie do Object kontra varargs
    static String choose(Object x) {
        return "Object (opakowanie)";
    }

    static String choose(int... xs) {
        return "int... (varargs)";
    }

    // code = kod: tylko wersja z int — char zostanie poszerzony do int
    static int code(int x) {
        return x;
    }

    /**
     * 3. Gdy nie ma dokładnego dopasowania, Java próbuje po kolei: poszerzenie typu prostego (int → long → double),
     * potem opakowanie (int → Integer → Object), a na samym końcu varargs. Wygrywa PIERWSZY etap, który coś znajdzie.
     */
    static void resolutionOrder() {
        section("3. Jak Java wybiera wersję: dokładne → poszerzenie → opakowanie → varargs");

        show("exact(5) — jest wersja int", exact(5));
        show("exact(5L) — jest wersja long", exact(5L));
        // WYNIK: exact(5) — jest wersja int → int
        // WYNIK: exact(5L) — jest wersja long → long

        show("pick(5) — long czy Integer?", pick(5));
        // WYNIK: pick(5) — long czy Integer? → long (poszerzenie)    ← poszerzenie wygrywa z opakowaniem

        show("choose(5) — Object czy varargs?", choose(5));
        // WYNIK: choose(5) — Object czy varargs? → Object (opakowanie)    ← varargs jest zawsze ostatnią deską ratunku

        show("code('A') — char do int", code('A'));
        // WYNIK: code('A') — char do int → 65

        // PUŁAPKA: niejednoznaczność = błąd kompilacji. Mając  f(int a, long b)  i  f(long a, int b),  wywołanie f(1, 1)
        //   pasuje do obu tak samo dobrze → „reference to f is ambiguous” (odwołanie jest niejednoznaczne).
    }

    // =================================================================================================
    // 4. null JAKO ARGUMENT
    // =================================================================================================

    static String nullable(Object value) {
        return "Object";
    }

    static String nullable(String value) {
        return "String";
    }

    /**
     * 4. null pasuje do każdego typu obiektowego. Java wybiera wtedy NAJBARDZIEJ KONKRETNY typ: String jest „węższy”
     * niż Object (każdy String jest Objectem), więc wygrywa wersja ze String.
     */
    static void nullArgument() {
        section("4. null jako argument — wygrywa najbardziej konkretny typ");

        show("nullable(null)", nullable(null));
        // WYNIK: nullable(null) → String

        // PUŁAPKA: gdyby były wersje  g(String)  i  g(Integer),  wywołanie g(null) byłoby niejednoznaczne (błąd kompilacji),
        //   bo żaden z tych typów nie jest „węższy” od drugiego. Rozwiązanie: rzutowanie  g((String) null).
    }

    // =================================================================================================
    // 5. PRZECIĄŻENIA W BIBLIOTECE JAVY
    // =================================================================================================

    /**
     * 5. Biblioteka Javy pełna jest przeciążeń: println ma wersje dla int, double, char, String, Object...,
     * Math.abs — dla int, long, float, double. Uwaga na Math.round: dla float zwraca int, dla double — long.
     */
    static void overloadsInJdk() {
        section("5. Przeciążenia w JDK: println, Math.abs, Math.round");

        show("Math.abs(-5)", Math.abs(-5));
        show("Math.abs(-5.5)", Math.abs(-5.5));
        // WYNIK: Math.abs(-5) → 5
        // WYNIK: Math.abs(-5.5) → 5.5

        Object fromFloat = Math.round(2.5f);          // wynik opakowany w obiekt, żeby sprawdzić jego klasę
        Object fromDouble = Math.round(2.5);
        show("typ wyniku Math.round(2.5f)", fromFloat.getClass().getSimpleName());
        show("typ wyniku Math.round(2.5)", fromDouble.getClass().getSimpleName());
        // WYNIK: typ wyniku Math.round(2.5f) → Integer    ← wersja dla float zwraca int
        // WYNIK: typ wyniku Math.round(2.5) → Long    ← wersja dla double zwraca long
    }

    // =================================================================================================
    // 6. PRZEKAZYWANIE PRACY (DELEGACJA)
    // =================================================================================================

    /** greet = przywitaj. Krótsza wersja przekazuje pracę pełnej — logika jest w JEDNYM miejscu. */
    static String greet(String name) {
        return greet(name, "Cześć");                  // wartość domyślna dla powitania
    }

    static String greet(String name, String greeting) {
        return greeting + ", " + name + "!";
    }

    /**
     * 6. Java nie ma parametrów domyślnych (jak np. Python). Zastępuje się je przeciążeniem: krótsza wersja woła
     * dłuższą, podając wartość domyślną.
     */
    static void delegation() {
        section("6. Parametr „domyślny” przez przeciążenie");

        show("greet(\"Ala\")", greet("Ala"));
        show("greet(\"Ala\", \"Dzień dobry\")", greet("Ala", "Dzień dobry"));
        // WYNIK: greet("Ala") → Cześć, Ala!
        // WYNIK: greet("Ala", "Dzień dobry") → Dzień dobry, Ala!

        // DOBRA PRAKTYKA: wersje przeciążone powinny robić TO SAMO (różnić się tylko danymi wejściowymi) i dzielić
        //   jedną implementację. Jeśli robią coś innego — nadaj im różne nazwy (np. findByName, findById).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Przeciążenie = ta sama nazwa, inna lista parametrów (liczba albo typy). Typ zwracany się nie liczy.
     *   • Wybór w czasie KOMPILACJI: dokładne → poszerzenie (int→long) → opakowanie (int→Integer) → varargs.
     *   • null → najbardziej konkretny typ; gdy dwa typy są „równorzędne” → niejednoznaczność (błąd kompilacji).
     *   • Math.round(float) → int, Math.round(double) → long.
     *   • „Parametry domyślne”: krótsza wersja woła dłuższą.
     *   • Przeciążenie (overload) ≠ nadpisanie (override, t07_inheritance_polymorphism).
     *
     * PYTANIA KONTROLNE:
     *   1. Czy mogą istnieć obok siebie  int count()  i  long count()?  Dlaczego?
     *   2. Mając  m(long)  i  m(Integer),  którą wersję wybierze  m(10)?
     *   3. Mając  m(Object)  i  m(int...),  którą wersję wybierze  m(10)?
     *   4. ZNAJDŹ BŁĄD:  static void f(int a, long b) {}  static void f(long a, int b) {}  ...  f(1, 1);
     *   5. Co wypisze:  System.out.println(Math.round(7.5f) + Math.round(7.5));  ?
     *   6. Jak w Javie zrobić odpowiednik parametru z wartością domyślną?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: pole koła r = 1", Math.PI, () -> exercise1(1.0));
        Check.equal("ćw. 1b: pole prostokąta 2 × 3", 6.0, () -> exercise1(2.0, 3.0));
        Check.equal("ćw. 2a: opis 5", "liczba całkowita: 5", () -> exercise2(5));
        Check.equal("ćw. 2b: opis 2.5", "liczba z ułamkiem: 2.5", () -> exercise2(2.5));
        Check.equal("ćw. 2c: opis \"abc\"", "napis: abc (3 znaki)", () -> exercise2("abc"));
        Check.equal("ćw. 3a: cena z domyślnym VAT 23%", "123.00 zł", () -> exercise3(100.0));
        Check.equal("ćw. 3b: cena z VAT 8%", "108.00 zł", () -> exercise3(100.0, 8));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", Math.PI, () -> solution1(1.0));
        Check.equal("ćw. 1b (wzorzec)", 6.0, () -> solution1(2.0, 3.0));
        Check.equal("ćw. 2a (wzorzec)", "liczba całkowita: 5", () -> solution2(5));
        Check.equal("ćw. 2b (wzorzec)", "liczba z ułamkiem: 2.5", () -> solution2(2.5));
        Check.equal("ćw. 2c (wzorzec)", "napis: abc (3 znaki)", () -> solution2("abc"));
        Check.equal("ćw. 3a (wzorzec)", "123.00 zł", () -> solution3(100.0));
        Check.equal("ćw. 3b (wzorzec)", "108.00 zł", () -> solution3(100.0, 8));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 7 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): dwie wersje exercise1 — pole KOŁA z promienia (π·r²) i pole PROSTOKĄTA z boków (a·b).
     * Podpowiedź: {@code Math.PI * r * r} oraz {@code a * b}.
     */
    static double exercise1(double radius) {
        // TODO: twoje rozwiązanie (pole koła)
        return 0;
    }

    static double exercise1(double a, double b) {
        // TODO: twoje rozwiązanie (pole prostokąta)
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): trzy wersje exercise2 opisujące argument:
     * int → "liczba całkowita: 5", double → "liczba z ułamkiem: 2.5", String → "napis: abc (3 znaki)".
     * Podpowiedź: długość napisu to {@code text.length()}.
     */
    static String exercise2(int value) {
        // TODO: twoje rozwiązanie
        return "";
    }

    static String exercise2(double value) {
        // TODO: twoje rozwiązanie
        return "";
    }

    static String exercise2(String text) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): cena brutto jako napis "123.00 zł". Wersja z jednym parametrem używa domyślnego VAT 23%
     * i PRZEKAZUJE pracę wersji z dwoma parametrami (sekcja 6).
     * Podpowiedź: {@code String.format(Locale.ROOT, "%.2f zł", net * (1 + vat / 100.0))}.
     */
    static String exercise3(double net) {
        // TODO: twoje rozwiązanie (wywołaj wersję z dwoma parametrami)
        return "";
    }

    static String exercise3(double net, int vat) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static double solution1(double radius) {
        return Math.PI * radius * radius;
    }

    static double solution1(double a, double b) {
        return a * b;
    }

    static String solution2(int value) {
        return "liczba całkowita: " + value;
    }

    static String solution2(double value) {
        return "liczba z ułamkiem: " + value;
    }

    static String solution2(String text) {
        return "napis: " + text + " (" + text.length() + " znaki)";
    }

    static String solution3(double net) {
        return solution3(net, 23);
    }

    static String solution3(double net, int vat) {
        return String.format(Locale.ROOT, "%.2f zł", net * (1 + vat / 100.0));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Nie — mają tę samą sygnaturę (nazwa + brak parametrów); typ zwracany nie rozróżnia metod.
     *   2. m(long) — poszerzenie typu prostego ma pierwszeństwo przed opakowaniem.
     *   3. m(Object) — opakowanie (int → Integer → Object) ma pierwszeństwo przed varargs.
     *   4. f(1, 1) pasuje równie dobrze do obu wersji → błąd kompilacji „reference to f is ambiguous”.
     *   5. 16 — Math.round(7.5f) = 8 (int), Math.round(7.5) = 8 (long); 8 + 8 = 16.
     *   6. Przeciążeniem: wersja z mniejszą liczbą parametrów wywołuje pełną wersję, podając wartość domyślną.
     */
    // </editor-fold>
}
