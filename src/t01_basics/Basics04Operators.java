package t01_basics;

import helpers.Check;

import java.util.ArrayList;
import java.util.List;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Operatory — arytmetyczne, przypisania, porównania, logiczne, trójargumentowy, bitowe
 *        (operator = znak działania, np. + - * /; operand = wartość, na której działa operator)
 *
 * W SKRÓCIE:
 *   Operatory liczą (+ - * / %), porównują (== != < >), łączą warunki (&& || !) i skracają zapis (+= ++).
 *   Najwięcej błędów robi się na: dzieleniu liczb całkowitych (7 / 2 = 3!), różnicy i++ / ++i, kolejności
 *   działań oraz na tym, że && nie sprawdza prawej strony, gdy lewa jest fałszywa.
 *
 * ANALOGIA: kalkulator i bramki.
 *   Operatory arytmetyczne to przyciski kalkulatora. Operatory logiczne to bramki na lotnisku: przy && (i) wystarczy,
 *   że PIERWSZA bramka Cię zatrzyma — do drugiej już nie dojdziesz. Przy || (lub) wystarczy, że pierwsza przepuści.
 *
 * JAK TO DZIAŁA:
 *   Arytmetyczne:   +  -  *  /  %(reszta)        int / int → int (ułamek ucięty!)
 *   Przypisania:    =  +=  -=  *=  /=  %=         x += 5  to skrót od  x = x + 5 (z ukrytym rzutowaniem!)
 *   Zwiększanie:    ++  --                        i++ (użyj, potem zwiększ)  vs  ++i (zwiększ, potem użyj)
 *   Porównania:     ==  !=  <  >  <=  >=          wynik: boolean
 *   Logiczne:       &&  ||  !                     && i || „na skróty” (short-circuit)
 *   Trójargumentowy: warunek ? gdyTak : gdyNie
 *   Bitowe:         &  |  ^  ~  <<  >>  >>>       działają na pojedynczych bitach
 *   Kolejność: najpierw * / %, potem + -, potem porównania, potem &&, potem ||, na końcu = (w razie wątpliwości — nawiasy).
 *
 * SŁÓWKA:
 *   operator = operator; operand = argument operatora; remainder = reszta (z dzielenia); increment = zwiększenie;
 *   decrement = zmniejszenie; compound assignment = przypisanie złożone (+=); short-circuit = skrócone obliczanie;
 *   ternary = trójargumentowy; precedence = pierwszeństwo (kolejność działań); bitwise = bitowy; shift = przesunięcie.
 *
 * ZOBACZ TEŻ: t01_basics/Basics05Casting (rzutowanie, przepełnienie), t01_basics/Basics08FloatingPoint (double),
 *             t15_numbers/Numbers05IntegerTricks (floorMod, sztuczki bitowe), t04_strings/Strings01Basics (== na napisach).
 * </pre>
 */
public class Basics04Operators {

    /** checkCalls = liczba wywołań metody check — do pokazania „skróconego” obliczania && i ||. */
    private static int checkCalls = 0;

    public static void main(String[] args) {
        title("Basics04 — operatory");

        arithmetic();           // arithmetic = arytmetyka
        incrementDecrement();   // increment decrement = zwiększanie i zmniejszanie
        compoundAssignment();   // compound assignment = przypisanie złożone
        comparisons();          // comparisons = porównania
        logicalShortCircuit();  // logical short circuit = logika „na skróty”
        ternary();              // ternary = operator trójargumentowy
        precedence();           // precedence = kolejność działań
        bitwise();              // bitwise = operacje bitowe
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. ARYTMETYKA
    // =================================================================================================

    /** 1. Dzielenie dwóch liczb całkowitych daje liczbę całkowitą — część ułamkowa jest UCINANA (nie zaokrąglana!). */
    static void arithmetic() {
        section("1. Arytmetyka i dzielenie całkowite");

        show("7 / 2", 7 / 2);
        show("7 % 2 (reszta)", 7 % 2);
        show("7.0 / 2", 7.0 / 2);
        show("-7 / 2", -7 / 2);
        show("-7 % 2", -7 % 2);
        // WYNIK: 7 / 2 → 3
        // WYNIK: 7 % 2 (reszta) → 1
        // WYNIK: 7.0 / 2 → 3.5
        // WYNIK: -7 / 2 → -3    ← ucięcie w stronę zera, a nie w dół
        // WYNIK: -7 % 2 → -1    ← reszta ma znak dzielnej

        // PUŁAPKA: średnia z ocen 4 i 5 liczona jako (4 + 5) / 2 da 4, nie 4.5 — bo wszystko jest int.
        //   Poprawnie: (4 + 5) / 2.0  albo  (double) (4 + 5) / 2.
        show("(4 + 5) / 2", (4 + 5) / 2);
        show("(4 + 5) / 2.0", (4 + 5) / 2.0);
        // WYNIK: (4 + 5) / 2 → 4
        // WYNIK: (4 + 5) / 2.0 → 4.5

        expectThrows("10 / 0 na liczbach całkowitych", () -> System.out.println(10 / zero()));
        // WYNIK: ✔ 10 / 0 na liczbach całkowitych → rzucono ArithmeticException: / by zero
        show("10.0 / 0 na double", 10.0 / zero());
        // WYNIK: 10.0 / 0 na double → Infinity    ← double nie rzuca wyjątku (Basics08FloatingPoint)
    }

    /** zero = zero. Metoda zwracająca 0 — żeby kompilator nie „przewidział” dzielenia przez zero w stałych. */
    private static int zero() {
        return 0;
    }

    // =================================================================================================
    // 2. ++ I --
    // =================================================================================================

    /** 2. i++ (postinkrementacja): użyj starej wartości, POTEM zwiększ. ++i (preinkrementacja): zwiększ, POTEM użyj. */
    static void incrementDecrement() {
        section("2. i++ kontra ++i");

        int i = 5;
        int a = i++;          // a dostaje 5, potem i staje się 6
        show("a = i++ → a, i", a + ", " + i);
        int b = ++i;          // i staje się 7, b dostaje 7
        show("b = ++i → b, i", b + ", " + i);
        // WYNIK: a = i++ → a, i → 5, 6
        // WYNIK: b = ++i → b, i → 7, 7

        // DOBRA PRAKTYKA: używaj ++ jako osobnej instrukcji (i++;) albo w pętli for. Wyrażenia typu
        //   x = i++ + ++i  to łamigłówki — poprawne, ale nieczytelne. W prawdziwym kodzie ich unikaj.
    }

    // =================================================================================================
    // 3. PRZYPISANIE ZŁOŻONE (+=, -=, ...)
    // =================================================================================================

    /** 3. x += 5 to skrót od x = x + 5 — ale z UKRYTYM rzutowaniem na typ x (to bywa pułapką). */
    static void compoundAssignment() {
        section("3. +=, -=, *=, /= — i ukryte rzutowanie");

        int total = 100;
        total += 20;          // 120
        total -= 5;           // 115
        total *= 2;           // 230
        total /= 3;           // 76 (dzielenie całkowite)
        show("total po serii", total);
        // WYNIK: total po serii → 76

        int points = 10;
        points += 2.7;        // UKRYTE rzutowanie: (int) (10 + 2.7) = 12 — ułamek przepada BEZ ostrzeżenia!
        show("points += 2.7", points);
        // WYNIK: points += 2.7 → 12
        // PUŁAPKA: points = points + 2.7;  się NIE skompiluje („possible lossy conversion from double to int”),
        //   a points += 2.7; — kompiluje się i po cichu ucina ułamek. Rzutowanie: Basics05Casting.
    }

    // =================================================================================================
    // 4. PORÓWNANIA
    // =================================================================================================

    /** 4. Porównania zwracają boolean. Na liczbach typów prostych działają zgodnie z intuicją. */
    static void comparisons() {
        section("4. Porównania");

        int age = 18;
        show("age >= 18", age >= 18);
        show("age == 20", age == 20);
        show("age != 20", age != 20);
        // WYNIK: age >= 18 → true
        // WYNIK: age == 20 → false
        // WYNIK: age != 20 → true

        // PUŁAPKA: == na OBIEKTACH (np. String, Integer) porównuje, czy to TEN SAM obiekt, a nie treść.
        //   Napisy porównuj przez equals: t04_strings/Strings01Basics; liczby Integer: t01_basics/Basics06Wrappers.
    }

    // =================================================================================================
    // 5. LOGIKA „NA SKRÓTY”
    // =================================================================================================

    /** check = sprawdź. Zwraca podaną wartość i LICZY wywołania — żeby zobaczyć, czy Java w ogóle ją wywołała. */
    private static boolean check(boolean value) {
        checkCalls++;
        return value;
    }

    /**
     * 5. && (i) oraz || (lub) liczą „na skróty”: jeśli wynik jest znany po lewej stronie, prawej NIE wykonują.
     * Pojedyncze & i | liczą ZAWSZE obie strony.
     */
    static void logicalShortCircuit() {
        section("5. &&, ||, ! — obliczanie na skróty");

        checkCalls = 0;
        boolean r1 = false && check(true);        // lewa false → całość false, check się NIE wywoła
        show("false && check(true)", r1 + ", wywołań check: " + checkCalls);
        // WYNIK: false && check(true) → false, wywołań check: 0

        checkCalls = 0;
        boolean r2 = true || check(false);        // lewa true → całość true, check się NIE wywoła
        show("true || check(false)", r2 + ", wywołań check: " + checkCalls);
        // WYNIK: true || check(false) → true, wywołań check: 0

        checkCalls = 0;
        boolean r3 = false & check(true);         // pojedyncze & liczy obie strony
        show("false & check(true)", r3 + ", wywołań check: " + checkCalls);
        // WYNIK: false & check(true) → false, wywołań check: 1

        show("!true", !true);
        // WYNIK: !true → false

        // DOBRA PRAKTYKA: wykorzystuj skróty do zabezpieczeń:  if (text != null && text.length() > 3)
        //   — gdy text jest null, prawa strona się nie wykona, więc nie będzie NullPointerException.
    }

    // =================================================================================================
    // 6. OPERATOR TRÓJARGUMENTOWY
    // =================================================================================================

    /** 6. warunek ? wartośćGdyTak : wartośćGdyNie — krótki if, który ZWRACA wartość. */
    static void ternary() {
        section("6. Operator trójargumentowy ? :");

        int stock = 0;
        String label = stock > 0 ? "dostępny" : "brak w magazynie";
        show("etykieta", label);
        // WYNIK: etykieta → brak w magazynie

        // DOBRA PRAKTYKA: tylko do prostych wyborów. Zagnieżdżone a ? b : c ? d : e są nieczytelne — użyj if/else.
    }

    // =================================================================================================
    // 7. KOLEJNOŚĆ DZIAŁAŃ
    // =================================================================================================

    /** 7. Jak w matematyce: * / % przed + -. Operatory o tym samym priorytecie liczą się od lewej. */
    static void precedence() {
        section("7. Kolejność działań");

        show("2 + 3 * 4", 2 + 3 * 4);
        show("(2 + 3) * 4", (2 + 3) * 4);
        show("10 - 4 - 3", 10 - 4 - 3);
        show("20 / 2 * 5", 20 / 2 * 5);
        // WYNIK: 2 + 3 * 4 → 14
        // WYNIK: (2 + 3) * 4 → 20
        // WYNIK: 10 - 4 - 3 → 3    ← od lewej: (10 - 4) - 3
        // WYNIK: 20 / 2 * 5 → 50    ← od lewej: (20 / 2) * 5, a nie 20 / 10

        // DOBRA PRAKTYKA: gdy nie masz pewności — dodaj nawiasy. Nie kosztują nic, a oszczędzają błędów.
    }

    // =================================================================================================
    // 8. OPERACJE BITOWE (skrót)
    // =================================================================================================

    /**
     * 8. Operatory bitowe działają na pojedynczych bitach liczby. Rzadko potrzebne na co dzień (flagi, sieci,
     * kompresja). Integer.toBinaryString pokazuje zapis dwójkowy. Więcej: t15_numbers/Numbers05IntegerTricks.
     */
    static void bitwise() {
        section("8. Operatory bitowe (w skrócie)");

        show("5 w dwójkowym", Integer.toBinaryString(5));
        show("3 w dwójkowym", Integer.toBinaryString(3));
        show("5 & 3 (i)", 5 & 3);         // 101 & 011 = 001
        show("5 | 3 (lub)", 5 | 3);       // 101 | 011 = 111
        show("5 ^ 3 (xor)", 5 ^ 3);       // 101 ^ 011 = 110
        show("~5 (negacja)", ~5);         // zamienia wszystkie bity → -6
        show("1 << 4 (przesunięcie w lewo)", 1 << 4);        // ×2 cztery razy
        show("-16 >> 2 (ze znakiem)", -16 >> 2);              // ÷4, znak zostaje
        show("-16 >>> 28 (bez znaku)", -16 >>> 28);           // z lewej wchodzą zera
        // WYNIK: 5 w dwójkowym → 101
        // WYNIK: 3 w dwójkowym → 11
        // WYNIK: 5 & 3 (i) → 1
        // WYNIK: 5 | 3 (lub) → 7
        // WYNIK: 5 ^ 3 (xor) → 6
        // WYNIK: ~5 (negacja) → -6
        // WYNIK: 1 << 4 (przesunięcie w lewo) → 16
        // WYNIK: -16 >> 2 (ze znakiem) → -4
        // WYNIK: -16 >>> 28 (bez znaku) → 15

        // PUŁAPKA: & i | na booleanach działają jak && i ||, ale BEZ skrótu (sekcja 5) — łatwo o NullPointerException.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • int / int → int (ucięcie): 7 / 2 = 3; potrzebny ułamek → jeden z operandów double: 7 / 2.0 = 3.5.
     *   • % = reszta (znak jak dzielna): -7 % 2 = -1. Dzielenie int przez 0 → ArithmeticException; double → Infinity.
     *   • i++ = użyj, potem zwiększ; ++i = zwiększ, potem użyj.
     *   • x += y = x = (typ x) (x + y) — ukryte rzutowanie (points += 2.7 ucina ułamek).
     *   • && i || liczą na skróty; & i | liczą obie strony.
     *   • warunek ? a : b — krótki if zwracający wartość.
     *   • Kolejność: * / % → + - → porównania → && → || → =; ten sam priorytet — od lewej. Nawiasy są tanie.
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  System.out.println(9 / 4 + 9 % 4);  ?
     *   2. Co wypisze:  int i = 3; int x = i++ * 2; System.out.println(x + " " + i);  ?
     *   3. ZNAJDŹ BŁĄD:  double average = (3 + 4) / 2;  — dlaczego wynik to 3.0, a nie 3.5?
     *   4. Dlaczego  if (list != null && list.size() > 0)  jest bezpieczne, a  if (list != null & list.size() > 0)  nie?
     *   5. Co wypisze:  System.out.println(1 + 2 + "3" + 4 * 5);  ?
     *   6. Ile wynosi  int p = 10; p += 3.9;  ?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        List<Integer> years = List.of(2024, 1900, 2000, 2026);
        List<Boolean> expected3 = List.of(true, false, true, false);

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: czy 10 jest parzyste", true, () -> exercise1(10));
        Check.equal("ćw. 1b: czy 7 jest parzyste", false, () -> exercise1(7));
        Check.equal("ćw. 2a: strony dla 25 pozycji po 10", 3, () -> exercise2(25, 10));
        Check.equal("ćw. 2b: strony dla 20 pozycji po 10", 2, () -> exercise2(20, 10));
        Check.equal("ćw. 3: lata przestępne", expected3, () -> exercise3(years));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", true, () -> solution1(10));
        Check.equal("ćw. 1b (wzorzec)", false, () -> solution1(7));
        Check.equal("ćw. 2a (wzorzec)", 3, () -> solution2(25, 10));
        Check.equal("ćw. 2b (wzorzec)", 2, () -> solution2(20, 10));
        Check.equal("ćw. 3 (wzorzec)", expected3, () -> solution3(years));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć true, gdy liczba jest parzysta.
     * Podpowiedź: liczba parzysta ma resztę z dzielenia przez 2 równą 0 ({@code n % 2 == 0}).
     */
    static boolean exercise1(int n) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): ile stron potrzeba, żeby wyświetlić {@code items} pozycji po {@code perPage} na stronę?
     * 25 pozycji po 10 → 3 strony; 20 po 10 → 2 strony. Tylko liczby całkowite, bez double.
     * Podpowiedź: zaokrąglenie w górę przy dzieleniu całkowitym: {@code (items + perPage - 1) / perPage}.
     */
    static int exercise2(int items, int perPage) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): dla każdego roku zwróć, czy jest przestępny. Rok jest przestępny, gdy dzieli się
     * przez 4, ALE nie przez 100 — chyba że dzieli się przez 400. (2024 → tak, 1900 → nie, 2000 → tak, 2026 → nie.)
     * Podpowiedź: {@code (y % 4 == 0 && y % 100 != 0) || y % 400 == 0}; wyniki dodawaj w pętli do new ArrayList.
     */
    static List<Boolean> exercise3(List<Integer> years) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(int n) {
        return n % 2 == 0;
    }

    static int solution2(int items, int perPage) {
        return (items + perPage - 1) / perPage;
    }

    static List<Boolean> solution3(List<Integer> years) {
        List<Boolean> result = new ArrayList<>();
        for (int y : years) {
            result.add((y % 4 == 0 && y % 100 != 0) || y % 400 == 0);
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 3 (9 / 4 = 2, 9 % 4 = 1, razem 3).
     *   2. „6 4” — i++ oddaje starą wartość 3 (3 * 2 = 6), a potem i staje się 4.
     *   3. (3 + 4) / 2 liczy się na int (= 3), a dopiero wynik zamienia się na double (3.0). Poprawnie: (3 + 4) / 2.0.
     *   4. && nie sprawdzi list.size(), gdy list jest null; pojedyncze & sprawdzi obie strony → NullPointerException.
     *   5. „3320” — od lewej: 1 + 2 = 3, potem napis "3" + "3" = "33", a 4 * 5 = 20 liczy się najpierw (mnożenie
     *      ma pierwszeństwo), więc doklejamy "20".
     *   6. 13 — ukryte rzutowanie (int) (10 + 3.9) = (int) 13.9 = 13.
     */
    // </editor-fold>
}
