package t24_algorithms;

import helpers.Check;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Bardzo duże i bardzo dokładne liczby — BigInteger i BigDecimal bez granic
 *        (arbitrary precision = dowolna precyzja; overflow = przepełnienie)
 *
 * W SKRÓCIE:
 *   long ma stały rozmiar (64 bity) — mieści liczby do ok. 9,2 trylionów. Silnia 21! już go przebija.
 *   BigInteger i BigDecimal nie mają górnego limitu (poza pamięcią komputera) — liczą dokładnie, kosztem
 *   szybkości. Tej lekcji używamy, gdy liczby urastają za duże dla long/double, albo gdy potrzebujemy
 *   dokładności co do ostatniej cyfry (finanse, kryptografia, matematyka symboliczna).
 *
 * ANALOGIA: long to kalkulator kieszonkowy z wyświetlaczem na 19 cyfr — poza tym zakresem miga błąd albo,
 *   gorzej, cicho pokazuje złą liczbę (przepełnienie). BigInteger to kartka papieru: rośnie tak długo,
 *   jak trzeba, licząc "w słupku" tak jak w szkole — wolniej, ale zawsze dokładnie.
 *
 * JAK TO DZIAŁA:
 *   long       64 bity, zakres do ok. 9 223 372 036 854 775 807               — stały rozmiar, bardzo szybki
 *   BigInteger tablica cyfr w pamięci, rośnie wraz z wartością                 — wolniejszy, bez górnego limitu
 *   BigDecimal BigInteger (cyfry) + skala (gdzie postawić przecinek)          — dokładne ułamki dziesiętne
 *   Obie klasy są NIEZMIENNE (immutable) — każda operacja (add, multiply...) zwraca NOWY obiekt,
 *     oryginał zostaje bez zmian (dokładnie jak String).
 *
 * SŁÓWKA:
 *   arbitrary precision = dowolna precyzja; immutable = niezmienny; scale = skala (liczba cyfr po przecinku);
 *   precision = precyzja (liczba cyfr znaczących); rounding mode = tryb zaokrąglania; probable prime =
 *   liczba prawdopodobnie pierwsza; certainty = pewność (parametr testu pierwszości); modular exponentiation
 *   = potęgowanie modularne; non-terminating = nieskończony (nieokresowy rozwinięcie dziesiętne).
 *
 * ZOBACZ TEŻ: t15_numbers/Numbers01BigDecimal (podstawy BigDecimal, RoundingMode), t15_numbers/Numbers05IntegerTricks
 *             (przepełnienie long, Math.*Exact), t24_algorithms/Math09InterviewClassics (silnia, przepełnienie int/long),
 *             t24_algorithms/Math02ModularChecksums (modulo i potęgowanie modularne w praktyce — sumy kontrolne).
 * </pre>
 */
public class Math08BigNumbers {

    public static void main(String[] args) {
        title("Math08 — bardzo duże i bardzo dokładne liczby");

        kiedyLongNieWystarcza();     // kiedy long nie wystarcza = when long is not enough
        silniaBigInteger();           // silnia BigInteger = BigInteger factorial
        fibonacciBigInteger();         // fibonacci BigInteger = BigInteger Fibonacci
        operacjeBigInteger();           // operacje BigInteger = BigInteger operations
        pierwiastekBigDecimal();         // pierwiastek BigDecimal = BigDecimal sqrt
        jedenTrzeciZMathContext();        // 1/3 z MathContext = 1/3 with MathContext
        pulapkaDzieleniaBezKontekstu();    // pulapka dzielenia bez kontekstu = division pitfall without context
        niezmiennoscBigInteger();           // niezmiennosc BigInteger = BigInteger immutability
        exercises();                         // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. KIEDY long NIE WYSTARCZA: 21!
    // =================================================================================================

    /**
     * 1. 20! mieści się w long. 21! już NIE — Math.multiplyExact to wykrywa i rzuca wyjątek (zamiast cicho
     * zawinąć wynik, jak zwykły operator {@code *}, patrz t15_numbers/Numbers05IntegerTricks).
     */
    static void kiedyLongNieWystarcza() {
        section("1. Kiedy long nie wystarcza: 21!");

        long factorial20 = 1;
        for (int i = 2; i <= 20; i++) {
            factorial20 = Math.multiplyExact(factorial20, i);
        }
        show("20! (long, mieści się)", factorial20);
        show("Long.MAX_VALUE", Long.MAX_VALUE);
        // WYNIK: 20! (long, mieści się) → 2432902008176640000
        // WYNIK: Long.MAX_VALUE → 9223372036854775807

        long finalFactorial20 = factorial20;
        expectThrows("20! * 21 jako long (Math.multiplyExact)", () -> Math.multiplyExact(finalFactorial20, 21L));
        // WYNIK: ✔ 20! * 21 jako long (Math.multiplyExact) → rzucono ArithmeticException: long overflow

        long wrongFactorial21 = factorial20 * 21;
        show("20! * 21 zwykłym operatorem * (ŹLE, cicho przepełnione)", wrongFactorial21);
        // WYNIK: 20! * 21 zwykłym operatorem * (ŹLE, cicho przepełnione) → -4249290049419214848

        // PUŁAPKA: zwykłe * nie ostrzega — dostajesz liczbę UJEMNĄ zamiast "21! jest za duże". Dlatego
        //   od tej lekcji dla silni, kombinatoryki i podobnych szybko rosnących wartości używamy BigInteger.
    }

    // =================================================================================================
    // 2. BigInteger: SILNIA 50!
    // =================================================================================================

    /**
     * 2. BigInteger.multiply zwraca nowy obiekt o tylu cyfrach, ile potrzeba — nie ma górnego limitu
     * poza pamięcią. Licząc silnię w pętli O(n) mnożeń, same mnożenia stają się coraz droższe (liczby
     * mają coraz więcej cyfr), więc realny koszt rośnie szybciej niż liniowo.
     */
    static void silniaBigInteger() {
        section("2. BigInteger: silnia 50!");

        BigInteger f10 = factorial(10);
        BigInteger f50 = factorial(50);
        show("10!", f10);
        show("50!", f50);
        show("liczba cyfr w 50!", f50.toString().length());
        // WYNIK: 10! → 3628800
        // WYNIK: 50! → 30414093201713378043612608166064768844377641568960512000000000000
        // WYNIK: liczba cyfr w 50! → 65

        note("50! ma 65 cyfr — żaden prymitywny typ Javy by tego nie pomieścił.");
    }

    // =================================================================================================
    // 3. BigInteger: CIĄG FIBONACCIEGO — FIBONACCI(100)
    // =================================================================================================

    /**
     * 3. Ciąg Fibonacciego rośnie wykładniczo (złoty podział), więc setna wyraz jest ogromny. Liczymy
     * iteracyjnie (dwie zmienne "poprzednia/bieżąca"), O(n) mnożeń... a tu nawet samych dodawań — BigInteger.add
     * jest tańsze niż multiply, ale dla ogromnych n i tak dominuje rosnąca długość liczb.
     */
    static void fibonacciBigInteger() {
        section("3. BigInteger: ciąg Fibonacciego — Fibonacci(100)");

        for (int n : new int[]{10, 50, 100}) {
            show("fibonacci(" + n + ")", fibonacci(n));
        }
        // WYNIK: fibonacci(10) → 55
        // WYNIK: fibonacci(50) → 12586269025
        // WYNIK: fibonacci(100) → 354224848179261915075

        note("Fibonacci(50) wciąż mieści się w long (< 9,2 tryliona), ale Fibonacci(100) już nie.");
    }

    // =================================================================================================
    // 4. BigInteger.pow, mod, gcd, isProbablePrime
    // =================================================================================================

    /**
     * 4. {@code pow} to szybkie potęgowanie (nie naiwna pętla mnożeń — JDK używa algorytmu "square and
     * multiply", O(log wykładnika) mnożeń dużych liczb). {@code mod} zawsze zwraca wynik NIEUJEMNY
     * (w przeciwieństwie do {@code remainder}, które ma znak dzielnej — podobnie jak zwykłe % dla int/long).
     * {@code gcd} liczy największy wspólny dzielnik algorytmem Euklidesa. {@code isProbablePrime(certainty)}
     * testuje pierwszość probabilistycznie — im wyższa "certainty", tym mniejsza szansa błędu (praktycznie zerowa).
     */
    static void operacjeBigInteger() {
        section("4. BigInteger.pow, mod, gcd, isProbablePrime");

        BigInteger two = BigInteger.valueOf(2);
        show("2^100 (pow)", two.pow(100));
        // WYNIK: 2^100 (pow) → 1267650600228229401496703205376

        show("50! mod 97", factorial(50).mod(BigInteger.valueOf(97)));
        // WYNIK: 50! mod 97 → 65

        show("gcd(48, 180)", BigInteger.valueOf(48).gcd(BigInteger.valueOf(180)));
        show("gcd(17, 5)", BigInteger.valueOf(17).gcd(BigInteger.valueOf(5)));
        // WYNIK: gcd(48, 180) → 12
        // WYNIK: gcd(17, 5) → 1

        BigInteger mersenne31 = two.pow(31).subtract(BigInteger.ONE);                  // 2^31 − 1
        show("2^31 - 1 (liczba Mersenne'a)", mersenne31);
        show("czy 2^31 - 1 jest prawdopodobnie pierwsza?", mersenne31.isProbablePrime(50));
        show("czy 97 jest prawdopodobnie pierwsza?", BigInteger.valueOf(97).isProbablePrime(50));
        show("czy 100 jest prawdopodobnie pierwsza?", BigInteger.valueOf(100).isProbablePrime(50));
        // WYNIK: 2^31 - 1 (liczba Mersenne'a) → 2147483647
        // WYNIK: czy 2^31 - 1 jest prawdopodobnie pierwsza? → true
        // WYNIK: czy 97 jest prawdopodobnie pierwsza? → true
        // WYNIK: czy 100 jest prawdopodobnie pierwsza? → false

        // DOBRA PRAKTYKA: "probable prime" to NIE jest matematyczny dowód — przy certainty=50 szansa
        //   pomyłki jest astronomicznie mała (rzędu 1/2^50), więc w praktyce (np. generowanie kluczy RSA)
        //   jest to w pełni akceptowalne i dużo szybsze niż próba dzielenia przez wszystkie liczby.
    }

    // =================================================================================================
    // 5. BigDecimal.sqrt(MathContext) DO 30 CYFR (Java 9+)
    // =================================================================================================

    /**
     * 5. {@code BigDecimal.sqrt(MathContext)} (Java 9+) liczy pierwiastek z zadaną precyzją — bez tego
     * BigDecimal w ogóle nie ma metody sqrt (pierwiastek z 2 jest niewymierny — nieskończenie wiele cyfr,
     * trzeba jawnie powiedzieć, gdzie przyciąć).
     */
    static void pierwiastekBigDecimal() {
        section("5. BigDecimal.sqrt(MathContext) do 30 cyfr (Java 9+)");

        MathContext mc30 = new MathContext(30);                      // 30 cyfr znaczących (precision)
        BigDecimal sqrt2 = BigDecimal.valueOf(2).sqrt(mc30);
        show("sqrt(2) z precyzją 30 cyfr", sqrt2);
        // WYNIK: sqrt(2) z precyzją 30 cyfr → 1.41421356237309504880168872421

        MathContext mc10 = new MathContext(10);
        show("sqrt(2) z precyzją 10 cyfr", BigDecimal.valueOf(2).sqrt(mc10));
        // WYNIK: sqrt(2) z precyzją 10 cyfr → 1.414213562

        note("Im więcej cyfr precyzji (MathContext), tym dłuższe, ale dokładniejsze przybliżenie.");
    }

    // =================================================================================================
    // 6. 1/3 Z MathContext I RoundingMode
    // =================================================================================================

    /**
     * 6. 1/3 w zapisie dziesiętnym to 0,(3) — nieskończony, okresowy ułamek. Bez podania precyzji albo
     * skali BigDecimal nie wie, gdzie przerwać, i rzuca wyjątek (sekcja 7). Z MathContext podajemy liczbę
     * cyfr znaczących ORAZ tryb zaokrąglania ostatniej z nich.
     */
    static void jedenTrzeciZMathContext() {
        section("6. 1/3 z MathContext i RoundingMode");

        BigDecimal one = BigDecimal.ONE;
        BigDecimal three = BigDecimal.valueOf(3);
        show("1/3, MathContext(5, HALF_UP)", one.divide(three, new MathContext(5, RoundingMode.HALF_UP)));
        show("1/3, MathContext(10, HALF_UP)", one.divide(three, new MathContext(10, RoundingMode.HALF_UP)));
        // WYNIK: 1/3, MathContext(5, HALF_UP) → 0.33333
        // WYNIK: 1/3, MathContext(10, HALF_UP) → 0.3333333333

        // Wariant z jawną SKALĄ (liczba miejsc po przecinku) zamiast precyzji (liczby cyfr znaczących):
        show("1/3, scale=4, HALF_UP", one.divide(three, 4, RoundingMode.HALF_UP));
        // WYNIK: 1/3, scale=4, HALF_UP → 0.3333

        // DOBRA PRAKTYKA: MathContext ustala PRECYZJĘ (ile cyfr ZNACZĄCYCH), divide(scale, rounding) ustala
        //   SKALĘ (ile miejsc PO PRZECINKU) — dla pieniędzy zwykle chcesz skali (grosze), nie precyzji.
    }

    // =================================================================================================
    // 7. PUŁAPKA: DZIELENIE BigDecimal BEZ MathContext → ArithmeticException
    // =================================================================================================

    /**
     * 7. {@code divide} bez podania skali ani MathContext próbuje policzyć wynik DOKŁADNIE. Gdy to
     * niemożliwe (ułamek nieokresowy w zapisie dziesiętnym), rzuca ArithmeticException zamiast zwrócić
     * obcięty wynik — BigDecimal woli głośno się wywrócić niż po cichu skłamać.
     */
    static void pulapkaDzieleniaBezKontekstu() {
        section("7. PUŁAPKA: dzielenie BigDecimal bez MathContext → ArithmeticException");

        BigDecimal one = BigDecimal.ONE;
        BigDecimal three = BigDecimal.valueOf(3);
        expectThrows("1 / 3 bez skali ani MathContext", () -> one.divide(three));
        // WYNIK: ✔ 1 / 3 bez skali ani MathContext → rzucono ArithmeticException: Non-terminating decimal expansion; no exact representable decimal result.

        show("4 / 2 bez skali (działa — wynik dokładny)", BigDecimal.valueOf(4).divide(BigDecimal.valueOf(2)));
        // WYNIK: 4 / 2 bez skali (działa — wynik dokładny) → 2

        // PUŁAPKA: ten sam kod (divide bez argumentów) działa dla 4/2, a wywraca się dla 1/3 — błąd
        //   ujawnia się dopiero na KONKRETNYCH danych w produkcji, nie na etapie kompilacji ani code review.
        // DOBRA PRAKTYKA: w kodzie na produkcję ZAWSZE podawaj skalę/precyzję i RoundingMode jawnie
        //   (sekcja 6) — nigdy nie licz na to, że dzielenie "akurat wyjdzie równo".
    }

    // =================================================================================================
    // 8. NIEZMIENNOŚĆ BigInteger — PUŁAPKA Z NIEPRZYPISANYM WYNIKIEM
    // =================================================================================================

    /**
     * 8. BigInteger (podobnie jak String i BigDecimal) jest NIEZMIENNY — add/multiply/pow nie modyfikują
     * obiektu, na którym są wywołane, tylko zwracają NOWY. Zapomnienie o przypisaniu wyniku to cichy błąd:
     * kod się kompiluje i działa, tylko nic nie robi.
     */
    static void niezmiennoscBigInteger() {
        section("8. Niezmienność BigInteger — pułapka z nieprzypisanym wynikiem");

        BigInteger a = BigInteger.TEN;
        a.add(BigInteger.ONE);                                        // wynik NIGDZIE nie trafia — zgubiony!
        show("a po a.add(ONE) bez przypisania", a);
        // WYNIK: a po a.add(ONE) bez przypisania → 10

        a = a.add(BigInteger.ONE);                                    // poprawnie: przypisujemy wynik z powrotem
        show("a po a = a.add(ONE)", a);
        // WYNIK: a po a = a.add(ONE) → 11

        // PUŁAPKA: a.add(BigInteger.ONE); wygląda jak "zwiększ a o 1" (tak jak a++ na int), ale to zupełnie
        //   inny świat — add zwraca NOWY obiekt, a stary zostaje bez zmian, jeśli wyniku nie przechwycisz.
        // DOBRA PRAKTYKA: traktuj BigInteger/BigDecimal jak String — każda "modyfikująca" metoda to tak
        //   naprawdę fabryka nowego obiektu; zawsze pisz {@code x = x.operacja(...)}.
    }

    // =================================================================================================
    // FUNKCJE POMOCNICZE
    // =================================================================================================

    /** factorial = silnia n! jako BigInteger (iteracyjnie, O(n) mnożeń o rosnącej liczbie cyfr). */
    static BigInteger factorial(int n) {
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }

    /** fibonacci = n-ty wyraz ciągu Fibonacciego (fib(0)=0, fib(1)=1), iteracyjnie jako BigInteger. */
    static BigInteger fibonacci(int n) {
        BigInteger previous = BigInteger.ZERO;
        BigInteger current = BigInteger.ONE;
        for (int i = 0; i < n; i++) {
            BigInteger next = previous.add(current);
            previous = current;
            current = next;
        }
        return previous;
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   long               max ≈ 9,22 · 10^18; 20! mieści się, 21! już nie (Math.multiplyExact to wykrywa)
     *   BigInteger         dowolna wielkość całkowita; multiply/add/pow/mod/gcd/isProbablePrime(certainty)
     *   BigDecimal         dowolna precyzja dziesiętna; sqrt(MathContext) (Java 9+)
     *   MathContext(n, rm) n cyfr ZNACZĄCYCH + tryb zaokrąglenia ostatniej
     *   divide(scale, rm)  n miejsc PO PRZECINKU (skala) + tryb zaokrąglenia — wybór dla pieniędzy
     *   divide() bez nic   ArithmeticException, gdy wynik nie jest skończonym ułamkiem dziesiętnym
     *   NIEZMIENNOŚĆ       a.add(x); bez przypisania NIC nie zmienia — zawsze a = a.add(x);
     *   mod vs remainder   mod zawsze ≥ 0; remainder ma znak dzielnej (jak zwykłe %)
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego 20! mieści się w long, a 21! już nie?
     *   2. Co wypisze:
     *          BigInteger x = BigInteger.valueOf(5);
     *          x.multiply(BigInteger.TEN);
     *          System.out.println(x);
     *   3. ZNAJDŹ BŁĄD:
     *          BigDecimal wynik = BigDecimal.ONE.divide(BigDecimal.valueOf(7));
     *   4. Dlaczego isProbablePrime nazywa się "probable" (prawdopodobna), a nie po prostu isPrime?
     *   5. Co wypisze:  System.out.println(BigInteger.valueOf(-7).mod(BigInteger.valueOf(3)));  ? (mod, nie remainder!)
     *   6. ZNAJDŹ BŁĄD:  MathContext mc = new MathContext(3); // chcemy 3 miejsca PO PRZECINKU
     *                      System.out.println(BigDecimal.valueOf(1).divide(BigDecimal.valueOf(3), mc));
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: silnia 10!", BigInteger.valueOf(3_628_800L), () -> exercise1(10));
        Check.equal("ćw. 1b: silnia 15!", new BigInteger("1307674368000"), () -> exercise1(15));
        Check.equal("ćw. 2a: NWD(48, 180)", BigInteger.valueOf(12), () -> exercise2(BigInteger.valueOf(48), BigInteger.valueOf(180)));
        Check.equal("ćw. 2b: NWD(17, 5)", BigInteger.ONE, () -> exercise2(BigInteger.valueOf(17), BigInteger.valueOf(5)));
        Check.equal("ćw. 3a: czy 97 pierwsza?", true, () -> exercise3(BigInteger.valueOf(97)));
        Check.equal("ćw. 3b: czy 100 pierwsza?", false, () -> exercise3(BigInteger.valueOf(100)));
        Check.equal("ćw. 4: 10/3 bezpiecznie, skala 4, HALF_UP", new BigDecimal("3.3333"),
                () -> exercise4(BigDecimal.valueOf(10), BigDecimal.valueOf(3), 4));
        Check.equal("ćw. 5: 1/8 bezpiecznie, skala 2, HALF_UP", new BigDecimal("0.13"),
                () -> exercise4(BigDecimal.ONE, BigDecimal.valueOf(8), 2));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", BigInteger.valueOf(3_628_800L), () -> solution1(10));
        Check.equal("ćw. 1b (wzorzec)", new BigInteger("1307674368000"), () -> solution1(15));
        Check.equal("ćw. 2a (wzorzec)", BigInteger.valueOf(12), () -> solution2(BigInteger.valueOf(48), BigInteger.valueOf(180)));
        Check.equal("ćw. 2b (wzorzec)", BigInteger.ONE, () -> solution2(BigInteger.valueOf(17), BigInteger.valueOf(5)));
        Check.equal("ćw. 3a (wzorzec)", true, () -> solution3(BigInteger.valueOf(97)));
        Check.equal("ćw. 3b (wzorzec)", false, () -> solution3(BigInteger.valueOf(100)));
        Check.equal("ćw. 4 (wzorzec)", new BigDecimal("3.3333"),
                () -> solution4(BigDecimal.valueOf(10), BigDecimal.valueOf(3), 4));
        Check.equal("ćw. 5 (wzorzec)", new BigDecimal("0.13"),
                () -> solution4(BigDecimal.ONE, BigDecimal.valueOf(8), 2));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 8 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz silnię {@code n!} jako BigInteger.
     * Podpowiedź: pętla od 2 do n, mnożenie przez BigInteger.valueOf(i).
     */
    static BigInteger exercise1(int n) {
        // TODO: twoje rozwiązanie
        return BigInteger.valueOf(-1);
    }

    /**
     * ĆWICZENIE 2 (łatwe): policz NWD (gcd) dwóch BigInteger algorytmem Euklidesa, BEZ wbudowanej metody gcd().
     * Podpowiedź: dopóki b != ZERO: (a, b) = (b, a mod b); wynik to a.
     */
    static BigInteger exercise2(BigInteger a, BigInteger b) {
        // TODO: twoje rozwiązanie
        return BigInteger.valueOf(-1);
    }

    /**
     * ĆWICZENIE 3 (średnie): sprawdź, czy {@code n} jest prawdopodobnie pierwsza (certainty = 50).
     * Podpowiedź: {@code n.isProbablePrime(50)}.
     */
    static boolean exercise3(BigInteger n) {
        // Pusty stub zwracałby zawsze to samo i przypadkiem "zaliczyłby" jeden z testów —
        // dlatego tu wyjątek, żeby ćwiczenie jawnie pokazywało ✘ dopóki go nie zrobisz.
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): podziel {@code a / b} bezpiecznie — z zadaną skalą i zaokrągleniem HALF_UP
     * (tak, by nigdy nie rzuciło ArithmeticException tak jak w sekcji 7).
     * Podpowiedź: {@code a.divide(b, scale, RoundingMode.HALF_UP)}.
     */
    static BigDecimal exercise4(BigDecimal a, BigDecimal b, int scale) {
        // TODO: twoje rozwiązanie
        return BigDecimal.valueOf(-1);
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static BigInteger solution1(int n) {
        return factorial(n);
    }

    static BigInteger solution2(BigInteger a, BigInteger b) {
        while (!b.equals(BigInteger.ZERO)) {
            BigInteger temp = b;
            b = a.mod(b);
            a = temp;
        }
        return a;
    }

    static boolean solution3(BigInteger n) {
        return n.isProbablePrime(50);
    }

    static BigDecimal solution4(BigDecimal a, BigDecimal b, int scale) {
        return a.divide(b, scale, RoundingMode.HALF_UP);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 20! ≈ 2,43 · 10^18, a Long.MAX_VALUE ≈ 9,22 · 10^18 — mieści się. 21! = 20! × 21 ≈ 5,1 · 10^19,
     *      czyli ponad pięć razy więcej niż long potrafi pomieścić.
     *   2. 5 — multiply zwraca NOWY BigInteger, nie modyfikuje x. Bez przypisania wynik (50) jest zgubiony.
     *   3. 1/7 = 0,142857142857... (okres) — nieskończone rozwinięcie dziesiętne, divide() bez skali/MathContext
     *      rzuci ArithmeticException. Trzeba podać skalę i RoundingMode albo MathContext.
     *   4. Test jest PROBABILISTYCZNY (np. Miller-Rabin) — dla bardzo dużych liczb sprawdzanie WSZYSTKICH
     *      dzielników byłoby zbyt wolne. isProbablePrime daje wynik z astronomicznie małą szansą pomyłki,
     *      ale formalnie nie jest dowodem matematycznym — stąd "probable", nie "is".
     *   5. 2 — mod() ZAWSZE zwraca wynik nieujemny (w przedziale 0..dzielnik−1), w przeciwieństwie do
     *      remainder() czy zwykłego %, które miałyby tu znak dzielnej (-7 → wynik ujemny).
     *   6. new MathContext(3) ustala PRECYZJĘ — 3 cyfry ZNACZĄCE (np. 0.333), a nie 3 miejsca po przecinku.
     *      Dla "3 miejsc po przecinku" trzeba divide(BigDecimal.valueOf(3), 3, RoundingMode...) — skala, nie MathContext.
     */
    // </editor-fold>
}
