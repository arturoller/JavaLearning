package t01_basics;

import helpers.Check;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Rzutowanie typów i przepełnienie
 *        (cast = rzutować, zamienić typ; overflow = przepełnienie; widening = poszerzanie; narrowing = zawężanie)
 *
 * W SKRÓCIE:
 *   Zamiana „mniejszego” typu na „większy” (int → long → double) dzieje się sama i jest bezpieczna — to
 *   poszerzanie (widening). Zamiana w drugą stronę (double → int, long → int) wymaga jawnego rzutowania
 *   (int) i może ZGUBIĆ dane: ułamek przepada, a za duża liczba „przekręca się” (przepełnienie).
 *
 * ANALOGIA: przelewanie wody.
 *   Z kieliszka do wiadra przelejesz bez problemu (poszerzanie). Z wiadra do kieliszka — musisz świadomie
 *   powiedzieć „wiem, co robię” (rzutowanie), i większość wody się wyleje (utrata danych).
 *   Licznik kilometrów w starym aucie po 999999 pokazuje 000000 — to jest przepełnienie.
 *
 * JAK TO DZIAŁA:
 *   byte → short → int → long → float → double      ← w tę stronę: automatycznie (poszerzanie)
 *   double → float → long → int → short → byte      ← w tę stronę: tylko z (typ) — rzutowanie (zawężanie)
 *   (int) 3.99     = 3        ← ułamek UCIĘTY, nie zaokrąglony
 *   (int) 3_000_000_000L = -1294967296   ← za duża liczba: zostają tylko „dolne” 32 bity
 *   byte + byte    = int      ← w działaniach byte/short/char zawsze awansują do int
 *
 * SŁÓWKA:
 *   cast = rzutowanie; implicit = niejawny (automatyczny); explicit = jawny; widening = poszerzanie;
 *   narrowing = zawężanie; overflow = przepełnienie; underflow = niedomiar (przepełnienie w dół);
 *   lossy conversion = konwersja ze stratą; round = zaokrąglij; exact = dokładny; promotion = awans (typu).
 *
 * ZOBACZ TEŻ: t01_basics/Basics02PrimitiveTypes (zakresy typów), t01_basics/Basics04Operators (+= i ukryte rzutowanie),
 *             t01_basics/Basics07MathRandom (Math.round), t15_numbers/Numbers05IntegerTricks (więcej o przepełnieniu).
 * </pre>
 */
public class Basics05Casting {

    public static void main(String[] args) {
        title("Basics05 — rzutowanie i przepełnienie");

        wideningIsAutomatic();      // widening is automatic = poszerzanie jest automatyczne
        narrowingNeedsCast();       // narrowing needs cast = zawężanie wymaga rzutowania
        roundingVsCasting();        // rounding vs casting = zaokrąglanie kontra rzutowanie
        overflow();                 // overflow = przepełnienie
        overflowBeforeAssignment(); // overflow before assignment = przepełnienie przed przypisaniem
        typePromotion();            // type promotion = awans typów w działaniach
        exactMethods();             // exact methods = metody „dokładne”
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. POSZERZANIE — AUTOMATYCZNE
    // =================================================================================================

    /** 1. Mniejszy typ „mieści się” w większym, więc Java zamienia go sama (niejawnie). */
    static void wideningIsAutomatic() {
        section("1. Poszerzanie (widening) — samo, bez ryzyka");

        int count = 42;
        long bigCount = count;          // int → long: automatycznie
        double asDouble = count;        // int → double: automatycznie
        show("int → long", bigCount);
        show("int → double", asDouble);
        // WYNIK: int → long → 42
        // WYNIK: int → double → 42.0

        char letter = 'A';
        int code = letter;              // char → int: automatycznie (kod znaku)
        show("char → int", code);
        // WYNIK: char → int → 65
    }

    // =================================================================================================
    // 2. ZAWĘŻANIE — TYLKO JAWNIE
    // =================================================================================================

    /** 2. Większy typ do mniejszego — tylko przez jawne rzutowanie (typ). Kompilator wymaga, żebyś „podpisał” ryzyko. */
    static void narrowingNeedsCast() {
        section("2. Zawężanie (narrowing) — tylko z (typ)");

        double price = 19.99;
        int whole = (int) price;        // (int) = rzutowanie na int — ułamek UCIĘTY
        show("(int) 19.99", whole);
        // WYNIK: (int) 19.99 → 19

        show("(int) -7.9", (int) -7.9);
        // WYNIK: (int) -7.9 → -7    ← ucięcie w stronę zera (nie w dół!)

        // Bez rzutowania:  int whole = price;  → błąd kompilacji „possible lossy conversion from double to int”
        //   (możliwa konwersja ze stratą). Kompilator chroni Cię przed przypadkową utratą danych.
        // PUŁAPKA: rzutowanie to NIE zaokrąglanie. (int) 19.99 to 19, a nie 20 — sekcja 3.
    }

    // =================================================================================================
    // 3. ZAOKRĄGLANIE A RZUTOWANIE
    // =================================================================================================

    /** 3. Chcesz zaokrąglić, a nie uciąć? Użyj Math.round (round = zaokrąglij) — „szkolne” zaokrąglanie. */
    static void roundingVsCasting() {
        section("3. Math.round kontra (int)");

        show("(int) 2.5", (int) 2.5);
        show("Math.round(2.5)", Math.round(2.5));
        show("Math.round(2.4)", Math.round(2.4));
        show("Math.round(-2.5)", Math.round(-2.5));
        // WYNIK: (int) 2.5 → 2
        // WYNIK: Math.round(2.5) → 3
        // WYNIK: Math.round(2.4) → 2
        // WYNIK: Math.round(-2.5) → -2    ← połówki zawsze „w górę”, czyli w stronę plus nieskończoności

        // Math.round(double) zwraca long — żeby mieć int: (int) Math.round(x).
        // Zaokrąglanie kwot do groszy: BigDecimal i RoundingMode (t15_numbers/Numbers01BigDecimal).
    }

    // =================================================================================================
    // 4. PRZEPEŁNIENIE
    // =================================================================================================

    /**
     * 4. Gdy wynik nie mieści się w typie, Java NIE zgłasza błędu — liczba „przekręca się” jak licznik kilometrów.
     * Po MAX_VALUE przychodzi MIN_VALUE.
     */
    static void overflow() {
        section("4. Przepełnienie (overflow) — cichy błąd");

        int max = Integer.MAX_VALUE;
        show("MAX_VALUE", max);
        show("MAX_VALUE + 1", max + 1);
        // WYNIK: MAX_VALUE → 2147483647
        // WYNIK: MAX_VALUE + 1 → -2147483648    ← przekręciło się na najmniejszą wartość!

        long tooBig = 3_000_000_000L;
        show("(int) 3_000_000_000L", (int) tooBig);
        // WYNIK: (int) 3_000_000_000L → -1294967296    ← zostały tylko „dolne” 32 bity

        byte small = (byte) 200;
        show("(byte) 200", small);
        // WYNIK: (byte) 200 → -56    ← byte mieści tylko -128..127

        // PUŁAPKA: przepełnienie jest CICHE — żadnego wyjątku, tylko błędny wynik. Liczby, które mogą urosnąć
        //   (sumy, iloczyny, czas w milisekundach, kwoty w groszach), trzymaj w long.
    }

    // =================================================================================================
    // 5. PRZEPEŁNIENIE PRZED PRZYPISANIEM
    // =================================================================================================

    /**
     * 5. Najbardziej podstępny przypadek: wynik trafia do long, ale LICZY SIĘ na int — więc przepełnia się
     * jeszcze przed przypisaniem. Typ zmiennej po lewej stronie nie ma wpływu na obliczenia po prawej.
     */
    static void overflowBeforeAssignment() {
        section("5. Przepełnienie PRZED przypisaniem do long");

        long msPerYearWrong = 365 * 24 * 60 * 60 * 1000;      // wszystko int → przepełnia się, POTEM idzie do long
        long msPerYearGood = 365L * 24 * 60 * 60 * 1000;      // 365L → całe mnożenie na long
        show("365 * 24 * 60 * 60 * 1000", msPerYearWrong);
        show("365L * 24 * 60 * 60 * 1000", msPerYearGood);
        // WYNIK: 365 * 24 * 60 * 60 * 1000 → 1471228928    ← błędne (przepełnienie int)
        // WYNIK: 365L * 24 * 60 * 60 * 1000 → 31536000000

        // DOBRA PRAKTYKA: w długich obliczeniach, które mogą przekroczyć ~2 miliardy, zacznij od literału long (365L)
        //   albo zrzutuj PIERWSZY składnik: (long) days * 24 * ...
    }

    // =================================================================================================
    // 6. AWANS TYPÓW W DZIAŁANIACH
    // =================================================================================================

    /**
     * 6. W działaniach arytmetycznych byte, short i char są najpierw zamieniane na int. Gdy w działaniu jest
     * long albo double — całe działanie liczy się w tym „większym” typie.
     */
    static void typePromotion() {
        section("6. Awans typów: byte + byte = int");

        byte a = 10;
        byte b = 20;
        int sum = a + b;                  // a + b to int — do byte trzeba by rzutować: (byte) (a + b)
        show("byte + byte (jako int)", sum);
        // WYNIK: byte + byte (jako int) → 30
        // byte c = a + b;  → błąd kompilacji „possible lossy conversion from int to byte”.

        show("5 / 2 (int / int)", 5 / 2);
        show("5 / 2.0 (int / double)", 5 / 2.0);
        show("'A' + 2 (char + int)", 'A' + 2);
        // WYNIK: 5 / 2 (int / int) → 2
        // WYNIK: 5 / 2.0 (int / double) → 2.5
        // WYNIK: 'A' + 2 (char + int) → 67
    }

    // =================================================================================================
    // 7. METODY „EXACT” — PRZEPEŁNIENIE ZAMIENIONE NA WYJĄTEK
    // =================================================================================================

    /**
     * 7. Math.addExact, multiplyExact, toIntExact (exact = dokładny) liczą jak zwykłe działania, ale przy
     * przepełnieniu RZUCAJĄ ArithmeticException zamiast po cichu zwrócić błędny wynik.
     */
    static void exactMethods() {
        section("7. Math.addExact / multiplyExact / toIntExact");

        show("addExact(2, 3)", Math.addExact(2, 3));
        // WYNIK: addExact(2, 3) → 5

        expectThrows("addExact(MAX_VALUE, 1)", () -> Math.addExact(Integer.MAX_VALUE, 1));
        // WYNIK: ✔ addExact(MAX_VALUE, 1) → rzucono ArithmeticException: integer overflow
        expectThrows("toIntExact(3_000_000_000L)", () -> Math.toIntExact(3_000_000_000L));
        // WYNIK: ✔ toIntExact(3_000_000_000L) → rzucono ArithmeticException: integer overflow

        // DOBRA PRAKTYKA: tam, gdzie błędny wynik byłby groźny (pieniądze, ilości w magazynie), używaj *Exact —
        //   lepszy głośny wyjątek niż cicho zepsuta liczba (zasada fail fast).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Poszerzanie (int → long → double) — automatyczne, bezpieczne.
     *   • Zawężanie (double → int, long → int) — tylko jawnie: (int) x; ułamek UCINANY w stronę zera.
     *   • Zaokrąglanie: Math.round(x) (zwraca long); połówki w stronę +∞: round(-2.5) = -2.
     *   • Przepełnienie jest CICHE: MAX_VALUE + 1 = MIN_VALUE; (byte) 200 = -56.
     *   • long wynik = 365 * 24 * ... → liczy się na int! Pisz 365L * 24 * ...
     *   • byte/short/char w działaniach → int; int z double → double.
     *   • Math.addExact / multiplyExact / toIntExact → wyjątek zamiast złego wyniku.
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  System.out.println((int) 9.99 + (int) 0.5);  ?
     *   2. Co wypisze:  System.out.println(Integer.MAX_VALUE + 1 == Integer.MIN_VALUE);  ?
     *   3. ZNAJDŹ BŁĄD:  long bytesInTerabyte = 1024 * 1024 * 1024 * 1024;
     *   4. Dlaczego  byte c = a + b;  (a, b typu byte) się nie kompiluje, a  a += b;  już tak?
     *   5. Kiedy warto użyć Math.multiplyExact zamiast zwykłego mnożenia?
     *   6. Co wypisze:  System.out.println(Math.round(7.5) + " " + Math.round(-7.5));  ?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: pełne złote z 49.99", 49, () -> exercise1(49.99));
        Check.equal("ćw. 2: zaokrąglona średnia 4 i 5", 5L, () -> exercise2(4, 5));
        Check.equal("ćw. 3: bajty w terabajcie", 1_099_511_627_776L, () -> exercise3());
        Check.equal("ćw. 4a: bezpieczne mnożenie 1000 × 1000", 1_000_000, () -> exercise4(1000, 1000));
        Check.throwsException("ćw. 4b: 100 000 × 100 000 ma rzucić wyjątek", ArithmeticException.class, () -> exercise4(100_000, 100_000));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 49, () -> solution1(49.99));
        Check.equal("ćw. 2 (wzorzec)", 5L, () -> solution2(4, 5));
        Check.equal("ćw. 3 (wzorzec)", 1_099_511_627_776L, () -> solution3());
        Check.equal("ćw. 4a (wzorzec)", 1_000_000, () -> solution4(1000, 1000));
        Check.throwsException("ćw. 4b (wzorzec)", ArithmeticException.class, () -> solution4(100_000, 100_000));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć liczbę pełnych złotych z ceny (bez groszy): 49.99 → 49.
     * Podpowiedź: rzutowanie (int) ucina ułamek.
     */
    static int exercise1(double price) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć średnią dwóch ocen ZAOKRĄGLONĄ „szkolnie” (4 i 5 → 4.5 → 5).
     * Podpowiedź: uważaj na dzielenie całkowite — najpierw zamień na double, potem Math.round (zwraca long).
     */
    static long exercise2(int a, int b) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć liczbę bajtów w terabajcie: 1024 × 1024 × 1024 × 1024.
     * Podpowiedź: zwykłe 1024 * 1024 * 1024 * 1024 przepełni int — zacznij od literału long.
     */
    static long exercise3() {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): pomnóż a × b tak, żeby przy przepełnieniu int poleciał ArithmeticException,
     * a nie wrócił zły wynik. 1000 × 1000 → 1 000 000; 100 000 × 100 000 → wyjątek.
     * Podpowiedź: Math.multiplyExact.
     */
    static int exercise4(int a, int b) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(double price) {
        return (int) price;
    }

    static long solution2(int a, int b) {
        return Math.round((a + b) / 2.0);        // 2.0 wymusza dzielenie na double
    }

    static long solution3() {
        return 1024L * 1024 * 1024 * 1024;
    }

    static int solution4(int a, int b) {
        return Math.multiplyExact(a, b);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 9 — (int) 9.99 = 9, (int) 0.5 = 0.
     *   2. true — MAX_VALUE + 1 przekręca się na MIN_VALUE.
     *   3. Całe mnożenie liczy się na int i przepełnia się (wynik 0!) jeszcze przed przypisaniem do long.
     *      Poprawnie: 1024L * 1024 * 1024 * 1024.
     *   4. a + b daje int, a int do byte wymaga rzutowania. a += b ma wbudowane ukryte rzutowanie: a = (byte) (a + b).
     *   5. Gdy przepełnienie dałoby groźny, cichy błąd (kwoty, ilości, rozmiary) — wolisz wyjątek niż zły wynik.
     *   6. „8 -7” — połówki zaokrąglane w stronę plus nieskończoności.
     */
    // </editor-fold>
}
