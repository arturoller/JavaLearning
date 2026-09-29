package t01_basics;

import helpers.Check;

import java.math.BigDecimal;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Pułapki liczb zmiennoprzecinkowych (double, float)
 *        (floating point = zmiennoprzecinkowy; precision = precyzja; epsilon = bardzo mała tolerancja błędu)
 *
 * W SKRÓCIE:
 *   double przechowuje liczby w systemie dwójkowym i większości ułamków dziesiętnych (0.1, 0.2...) NIE da się
 *   tak zapisać dokładnie. Dlatego 0.1 + 0.2 to 0.30000000000000004, a suma dziesięciu 0.1 to 0.9999999999999999.
 *   Takie liczby porównuj z tolerancją (epsilon), a pieniądze licz na BigDecimal.
 *
 * ANALOGIA: 1/3 na kartce.
 *   Ułamka 1/3 nie zapiszesz dokładnie w systemie dziesiętnym: 0.3333... i gdzieś musisz uciąć. Komputer ma ten sam
 *   problem z 1/10 w systemie dwójkowym — 0.1 to dla niego „nieskończony ułamek”, który zostaje ucięty.
 *
 * JAK TO DZIAŁA:
 *   0.1 + 0.2 == 0.3                        → false!
 *   Math.abs((0.1 + 0.2) - 0.3) < 1e-9       → true   (porównanie z tolerancją)
 *   1.0 / 0 = Infinity;  0.0 / 0 = NaN (Not a Number = nie-liczba);  NaN == NaN → false!
 *   Double.MIN_VALUE = 4.9E-324 — najmniejsza DODATNIA liczba, a nie najbardziej ujemna!
 *
 * SŁÓWKA:
 *   floating point = zmiennoprzecinkowy; precision = precyzja; rounding error = błąd zaokrąglenia; epsilon = tolerancja;
 *   infinity = nieskończoność; NaN (Not a Number) = nie-liczba; nearly equal = prawie równe; sentinel = wartość startowa
 *   „strażnik” (np. do szukania maksimum); accumulate = kumulować się, narastać.
 *
 * ZOBACZ TEŻ: t01_basics/Basics02PrimitiveTypes (float vs double), t01_basics/Basics07MathRandom (zaokrąglanie),
 *             t15_numbers/Numbers01BigDecimal (dokładne liczby dziesiętne), t15_numbers/Numbers02MoneyValueObject (pieniądze).
 * </pre>
 */
public class Basics08FloatingPoint {

    /** EPSILON = tolerancja przy porównywaniu double. 1e-9 = 0,000000001. */
    private static final double EPSILON = 1e-9;

    public static void main(String[] args) {
        title("Basics08 — pułapki double");

        notExact();             // not exact = niedokładne
        whyNotExact();          // why not exact = dlaczego niedokładne
        compareWithEpsilon();   // compare with epsilon = porównanie z tolerancją
        errorsAccumulate();     // errors accumulate = błędy się sumują
        specialValues();        // special values = wartości specjalne
        minValuePitfall();      // min value pitfall = pułapka MIN_VALUE
        bigNumbersLosePrecision(); // big numbers lose precision = duże liczby tracą dokładność
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. DOUBLE NIE JEST DOKŁADNY
    // =================================================================================================

    /** 1. Klasyczny przykład, który zaskakuje każdego początkującego. To nie błąd Javy — tak działa każdy język. */
    static void notExact() {
        section("1. 0.1 + 0.2 to nie 0.3");

        show("0.1 + 0.2", 0.1 + 0.2);
        show("0.1 + 0.2 == 0.3", 0.1 + 0.2 == 0.3);
        show("1.1 - 1.0", 1.1 - 1.0);
        // WYNIK: 0.1 + 0.2 → 0.30000000000000004
        // WYNIK: 0.1 + 0.2 == 0.3 → false
        // WYNIK: 1.1 - 1.0 → 0.10000000000000009
    }

    // =================================================================================================
    // 2. DLACZEGO
    // =================================================================================================

    /**
     * 2. new BigDecimal(0.1) pokazuje DOKŁADNIE to, co double przechowuje zamiast 0.1. (Tu celowo używamy
     * konstruktora z double — do obliczeń zawsze twórz BigDecimal z tekstu: new BigDecimal("0.1").)
     */
    static void whyNotExact() {
        section("2. Co naprawdę siedzi w 0.1");

        show("prawdziwa wartość 0.1", new BigDecimal(0.1));
        // WYNIK: prawdziwa wartość 0.1 → 0.1000000000000000055511151231257827021181583404541015625
        // Java wypisuje „0.1”, bo pokazuje najkrótszy zapis, który jednoznacznie wskazuje tę liczbę — ale w pamięci
        //   jest przybliżenie. Liczby typu 0.5, 0.25, 0.125 (połówki, ćwiartki...) są za to zapisane DOKŁADNIE.
        show("0.5 + 0.25 == 0.75", 0.5 + 0.25 == 0.75);
        // WYNIK: 0.5 + 0.25 == 0.75 → true
    }

    // =================================================================================================
    // 3. PORÓWNYWANIE Z TOLERANCJĄ
    // =================================================================================================

    /** 3. Zamiast a == b sprawdzaj, czy różnica jest „prawie zerowa”: {@code Math.abs(a - b) < EPSILON}. */
    static void compareWithEpsilon() {
        section("3. Porównanie z tolerancją (epsilon)");

        double sum = 0.1 + 0.2;
        show("Math.abs(sum - 0.3) < EPSILON", Math.abs(sum - 0.3) < EPSILON);
        // WYNIK: Math.abs(sum - 0.3) < EPSILON → true

        // DOBRA PRAKTYKA: dobierz epsilon do skali danych (1e-9 dla „zwykłych” liczb). Do pieniędzy i wszystkiego,
        //   co musi być DOKŁADNE — BigDecimal, gdzie problemu w ogóle nie ma.
    }

    // =================================================================================================
    // 4. BŁĘDY SIĘ SUMUJĄ
    // =================================================================================================

    /** 4. Każde działanie może dodać mały błąd. W pętli błędy się kumulują. */
    static void errorsAccumulate() {
        section("4. Błędy narastają w pętli");

        double total = 0.0;
        for (int i = 0; i < 10; i++) {
            total += 0.1;
        }
        show("10 × dodaj 0.1", total);
        show("total == 1.0", total == 1.0);
        // WYNIK: 10 × dodaj 0.1 → 0.9999999999999999
        // WYNIK: total == 1.0 → false

        // PUŁAPKA: pętla  for (double x = 0; x != 1.0; x += 0.1)  NIGDY się nie skończy — x przeskoczy 1.0.
        //   Liczniki pętli zawsze jako int; wartość ułamkową wyliczaj z licznika: x = i * 0.1.
    }

    // =================================================================================================
    // 5. WARTOŚCI SPECJALNE: Infinity i NaN
    // =================================================================================================

    /** 5. Dzielenie double przez zero NIE rzuca wyjątku (w odróżnieniu od int) — daje nieskończoność albo NaN. */
    static void specialValues() {
        section("5. Infinity i NaN");

        double zero = 0.0;
        show("1.0 / 0", 1.0 / zero);
        show("-1.0 / 0", -1.0 / zero);
        show("0.0 / 0", 0.0 / zero);
        // WYNIK: 1.0 / 0 → Infinity
        // WYNIK: -1.0 / 0 → -Infinity
        // WYNIK: 0.0 / 0 → NaN

        double nan = 0.0 / zero;
        show("nan == nan", nan == nan);
        show("Double.isNaN(nan)", Double.isNaN(nan));
        // WYNIK: nan == nan → false    ← NaN nie jest równe NICZEMU, nawet sobie
        // WYNIK: Double.isNaN(nan) → true

        // PUŁAPKA: NaN „zaraża” wszystko: NaN + 5 = NaN. Jedno złe dzielenie potrafi zepsuć całą średnią.
        //   Sprawdzaj dzielnik przed dzieleniem albo wynik przez Double.isNaN / Double.isInfinite.
    }

    // =================================================================================================
    // 6. PUŁAPKA: Double.MIN_VALUE
    // =================================================================================================

    /**
     * 6. Integer.MIN_VALUE to najbardziej ujemna liczba int. Ale Double.MIN_VALUE to najmniejsza DODATNIA liczba!
     * Użyta jako „start” przy szukaniu maksimum da zły wynik dla samych liczb ujemnych.
     */
    static void minValuePitfall() {
        section("6. Double.MIN_VALUE jest DODATNIE");

        show("Double.MIN_VALUE", Double.MIN_VALUE);
        show("Double.MIN_VALUE > 0", Double.MIN_VALUE > 0);
        // WYNIK: Double.MIN_VALUE → 4.9E-324
        // WYNIK: Double.MIN_VALUE > 0 → true

        double[] temperatures = {-5.5, -2.0, -9.1};
        double wrongMax = Double.MIN_VALUE;                 // ŹLE: to 0,000...0049, czyli więcej niż każda ujemna!
        double goodMax = Double.NEGATIVE_INFINITY;          // DOBRZE: mniejsze od każdej liczby
        for (double t : temperatures) {
            wrongMax = Math.max(wrongMax, t);
            goodMax = Math.max(goodMax, t);
        }
        show("max od Double.MIN_VALUE", wrongMax);
        show("max od NEGATIVE_INFINITY", goodMax);
        // WYNIK: max od Double.MIN_VALUE → 4.9E-324    ← zły wynik: żadnej takiej temperatury nie było!
        // WYNIK: max od NEGATIVE_INFINITY → -2.0

        // DOBRA PRAKTYKA: jeszcze lepiej zacząć od PIERWSZEGO elementu tablicy (goodMax = temperatures[0]).
    }

    // =================================================================================================
    // 7. DUŻE LICZBY TRACĄ DOKŁADNOŚĆ
    // =================================================================================================

    /** 7. double ma ok. 15–16 cyfr znaczących. Przy bardzo dużych liczbach „mała” zmiana może całkiem zniknąć. */
    static void bigNumbersLosePrecision() {
        section("7. Duże liczby tracą dokładność");

        double big = 1e16;                              // 10 000 000 000 000 000
        show("1e16 + 1 == 1e16", big + 1 == big);
        // WYNIK: 1e16 + 1 == 1e16 → true    ← dodanie 1 „zginęło”

        float f = 16_777_216f;                          // 2^24 — granica dokładności float
        show("16777216f + 1", f + 1);
        // WYNIK: 16777216f + 1 → 1.6777216E7    ← float nawet nie „zauważył” +1

        // DOBRA PRAKTYKA: identyfikatory, liczniki, kwoty w groszach — typy całkowite (long), nigdy double.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • double/float są PRZYBLIŻONE: 0.1 + 0.2 = 0.30000000000000004; 10 × 0.1 = 0.9999999999999999.
     *   • Porównuj z tolerancją: Math.abs(a - b) < 1e-9 — nie przez ==.
     *   • double / 0 → Infinity lub NaN (bez wyjątku); NaN != NaN — sprawdzaj Double.isNaN.
     *   • Double.MIN_VALUE jest DODATNIE (4.9E-324) — do szukania maksimum: NEGATIVE_INFINITY albo pierwszy element.
     *   • double ma ok. 15–16 cyfr: 1e16 + 1 == 1e16.
     *   • Liczniki pętli jako int; pieniądze jako BigDecimal (albo long w groszach).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego 0.1 + 0.2 != 0.3, a 0.5 + 0.25 == 0.75?
     *   2. Co wypisze:  System.out.println(0.1 * 3);  ?
     *   3. ZNAJDŹ BŁĄD:  for (double x = 0.0; x != 1.0; x += 0.1) { ... }
     *   4. Co wypisze:  double d = 0.0 / 0.0; System.out.println(d == d);  ?
     *   5. ZNAJDŹ BŁĄD (szukanie najwyższej temperatury):  double max = Double.MIN_VALUE; for (...) max = Math.max(max, t);
     *   6. Jakim typem liczyć pieniądze i dlaczego nie double?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: 0.1 + 0.2 prawie równe 0.3", true, () -> exercise1(0.1 + 0.2, 0.3));
        Check.equal("ćw. 1b: 0.3 i 0.31 nie są prawie równe", false, () -> exercise1(0.3, 0.31));
        Check.equal("ćw. 2: max z samych ujemnych", -2.0, () -> exercise2(new double[]{-5.5, -2.0, -9.1}));
        Check.equal("ćw. 3: ile razy dodać 0.1, żeby dojść do 1.0", 11, () -> exercise3());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", true, () -> solution1(0.1 + 0.2, 0.3));
        Check.equal("ćw. 1b (wzorzec)", false, () -> solution1(0.3, 0.31));
        Check.equal("ćw. 2 (wzorzec)", -2.0, () -> solution2(new double[]{-5.5, -2.0, -9.1}));
        Check.equal("ćw. 3 (wzorzec)", 11, () -> solution3());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć true, gdy a i b są „prawie równe” (różnica mniejsza niż EPSILON).
     * Podpowiedź: {@code Math.abs(a - b) < EPSILON}.
     */
    static boolean exercise1(double a, double b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć największą liczbę z tablicy (tablica ma co najmniej 1 element; liczby mogą być
     * same ujemne). Podpowiedź: zacznij od {@code values[0]} (albo Double.NEGATIVE_INFINITY), NIE od Double.MIN_VALUE.
     */
    static double exercise2(double[] values) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zaczynając od 0.0, dodawaj 0.1 w pętli, dopóki suma jest MNIEJSZA niż 1.0.
     * Zwróć, ile razy dodałeś. Intuicja mówi 10 — sprawdź, co mówi double (oczekiwany wynik jest w sprawdzianie).
     * Podpowiedź: {@code int count = 0; double sum = 0.0; while (sum < 1.0) { sum += 0.1; count++; }}
     */
    static int exercise3() {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }

    static double solution2(double[] values) {
        double max = values[0];
        for (double v : values) {
            max = Math.max(max, v);
        }
        return max;
    }

    static int solution3() {
        int count = 0;
        double sum = 0.0;
        while (sum < 1.0) {        // po 10 krokach sum = 0.9999999999999999 — nadal < 1.0, więc jest 11. krok
            sum += 0.1;
            count++;
        }
        return count;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 0.1 i 0.2 nie mają dokładnego zapisu dwójkowego (przybliżenia się sumują); 0.5 i 0.25 to potęgi dwójki
     *      (1/2, 1/4), zapisane dokładnie.
     *   2. 0.30000000000000004.
     *   3. x nigdy nie będzie dokładnie 1.0 (0.9999999999999999, potem 1.0999999999999999) — pętla nieskończona.
     *      Licznik jako int: for (int i = 0; i < 10; i++) { double x = i * 0.1; ... }.
     *   4. false — NaN nie jest równe niczemu, nawet sobie.
     *   5. Double.MIN_VALUE jest DODATNIE — dla samych ujemnych temperatur wynikiem będzie 4.9E-324.
     *      Start: pierwszy element albo Double.NEGATIVE_INFINITY.
     *   6. BigDecimal (albo long w groszach) — double nie przechowuje dokładnie kwot typu 0.10 zł, a błędy narastają.
     */
    // </editor-fold>
}
