package t24_algorithms;

import helpers.Check;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Łamigłówki w stylu Project Euler — brute force, potem sprytniej
 *        (brute force = siłowe/brutalne przeszukanie wszystkich możliwości)
 *
 * W SKRÓCIE:
 *   Project Euler (projecteuler.net) to zbiór kilkuset zadań matematyczno-programistycznych — każde ma
 *   jedną liczbową odpowiedź. Tu rozwiązujemy kilka klasycznych: najpierw NAIWNIE (sprawdź wszystko),
 *   potem SPRYTNIEJ (wzór albo lepszy algorytm) — i porównujemy złożoność obu podejść.
 *
 * ANALOGIA: brute force to szukanie kluczy, przeszukując KAŻDĄ szufladę w domu po kolei — zawsze znajdzie,
 *   ale wolno. Podejście "sprytniejsze" to przypomnienie sobie, gdzie się je zwykle odkłada — szybciej,
 *   ale wymaga zrozumienia problemu, nie tylko mocy obliczeniowej.
 *
 * JAK TO DZIAŁA:
 *   Każde zadanie: 1) wersja brute force (łatwa do napisania, O(n) lub gorzej), 2) wersja sprytniejsza
 *   (wzór matematyczny albo lepszy algorytm), 3) porównanie wyniku i złożoności.
 *
 * SŁÓWKA:
 *   brute force = siłowe przeszukanie; sequence = ciąg; chain = łańcuch; happy number = liczba szczęśliwa;
 *   Collatz conjecture = hipoteza Collatza; move = ruch; arithmetic series = ciąg arytmetyczny.
 *
 * ZOBACZ TEŻ: t24_algorithms/Math09InterviewClassics (Fibonacci, isPrime), t24_algorithms/Math01NumberTheory
 *             (sito Eratostenesa, podzielność), t03_arrays/Arrays04Algorithms (siłowe algorytmy na tablicach).
 * </pre>
 */
public class Math10PuzzlesEuler {

    public static void main(String[] args) {
        title("Math10 — łamigłówki w stylu Project Euler");

        wstep();                          // wstep = introduction
        wielokrotnosciBruteForce();        // wielokrotnosci brute force = multiples brute force
        wielokrotnosciWzorem();             // wielokrotnosci wzorem = multiples by formula
        parzysteFibonacci();                 // parzyste fibonacci = even Fibonacci
        najwiekszyCzynnikPierwszy();          // najwiekszy czynnik pierwszy = largest prime factor
        liczbySzczesliwe();                    // liczby szczesliwe = happy numbers
        lancuchCollatza();                      // lancuch collatza = Collatz chain
        wiezeHanoi();                            // wieze hanoi = Tower of Hanoi
        exercises();                              // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. WSTĘP: CZYM SĄ ZAGADKI TYPU PROJECT EULER
    // =================================================================================================

    /**
     * 1. Project Euler to seria zadań: od prostej arytmetyki po teorię liczb i kombinatorykę. Reguła tej
     * lekcji: najpierw rozwiązanie OCZYWISTE (pętla sprawdzająca wszystko), potem — jeśli się da — wzór
     * albo algorytm, który omija zbędną pracę.
     */
    static void wstep() {
        section("1. Wstęp: czym są zagadki typu Project Euler");

        note("projecteuler.net — kilkaset zadań matematyczno-programistycznych, każde z jedną liczbą jako odpowiedzią.");
        note("Strategia: 1) brute force (zawsze działa, czasem wolno), 2) wzór/algorytm (szybciej, trudniej wymyślić).");

        // DOBRA PRAKTYKA: na rozmowie kwalifikacyjnej ZACZNIJ od brute force (działające rozwiązanie
        //   jest lepsze niż brak rozwiązania), a potem zaproponuj optymalizację — tak jak w tej lekcji.
    }

    // =================================================================================================
    // 2. SUMA WIELOKROTNOŚCI 3 LUB 5 PONIŻEJ 1000 — BRUTE FORCE
    // =================================================================================================

    /**
     * 2. PYTANIE REKRUTACYJNE (Project Euler #1): zsumuj wszystkie wielokrotności 3 lub 5 mniejsze od N.
     * Brute force: przejdź przez każdą liczbę 1..N−1 i sprawdź podzielność. Złożoność O(N).
     */
    static void wielokrotnosciBruteForce() {
        section("2. Suma wielokrotności 3 lub 5 poniżej 1000 — brute force");

        show("suma wielokrotności 3 lub 5 poniżej 10 (z treści zadania: 3+5+6+9)", sumMultiplesOf3Or5BruteForce(10));
        show("suma wielokrotności 3 lub 5 poniżej 1000", sumMultiplesOf3Or5BruteForce(1000));
        // WYNIK: suma wielokrotności 3 lub 5 poniżej 10 (z treści zadania: 3+5+6+9) → 23
        // WYNIK: suma wielokrotności 3 lub 5 poniżej 1000 → 233168

        note("233168 to oficjalna odpowiedź Project Euler #1 — dobry sposób, żeby sprawdzić, czy kod działa poprawnie.");
    }

    // =================================================================================================
    // 3. TA SAMA SUMA — WZÓR Z CIĄGIEM ARYTMETYCZNYM (SZYBCIEJ)
    // =================================================================================================

    /**
     * 3. Suma wielokrotności k poniżej N to suma ciągu arytmetycznego: k + 2k + ... + m·k = k·m(m+1)/2,
     * gdzie m = (N−1)/k. Licząc osobno dla 3 i dla 5, policzylibyśmy wielokrotności 15 PODWÓJNIE (bo 15
     * jest wielokrotnością obu) — trzeba je odjąć raz (zasada włączeń-wyłączeń). Złożoność O(1).
     */
    static void wielokrotnosciWzorem() {
        section("3. Ta sama suma — wzór z ciągiem arytmetycznym (szybciej)");

        show("wzorem: poniżej 10", sumMultiplesOf3Or5Formula(10));
        show("wzorem: poniżej 1000", sumMultiplesOf3Or5Formula(1000));
        show("wzorem: poniżej 1000000 (brute force by tu długo czekał)", sumMultiplesOf3Or5Formula(1_000_000));
        // WYNIK: wzorem: poniżej 10 → 23
        // WYNIK: wzorem: poniżej 1000 → 233168
        // WYNIK: wzorem: poniżej 1000000 (brute force by tu długo czekał) → 233333166668

        Check.isTrue("brute force i wzór dają to samo dla 1000", sumMultiplesOf3Or5BruteForce(1000) == sumMultiplesOf3Or5Formula(1000));
        // WYNIK: ✔ OK    brute force i wzór dają to samo dla 1000

        // DOBRA PRAKTYKA: O(1) kontra O(N) — dla N=1000 różnicy nie widać, ale dla N=10^12 brute force
        //   nie skończyłby się w rozsądnym czasie, a wzór liczy to natychmiast.
    }

    // =================================================================================================
    // 4. SUMA PARZYSTYCH LICZB FIBONACCIEGO ≤ 4 000 000
    // =================================================================================================

    /**
     * 4. PYTANIE REKRUTACYJNE (Project Euler #2): zsumuj PARZYSTE wyrazy ciągu Fibonacciego nie większe
     * niż limit. Generujemy ciąg iteracyjnie (bez pamiętania całej listy — tylko dwie ostatnie wartości)
     * i sumujemy parzyste w locie. Złożoność O(log(limit)) kroków — Fibonacci rośnie wykładniczo.
     */
    static void parzysteFibonacci() {
        section("4. Suma parzystych liczb Fibonacciego ≤ 4 000 000");

        show("suma parzystych Fibonacciego ≤ 4 000 000", sumEvenFibonacciUpTo(4_000_000));
        // WYNIK: suma parzystych Fibonacciego ≤ 4 000 000 → 4613732

        note("Co trzeci wyraz Fibonacciego jest parzysty (1,1,2,3,5,8,13,21,34,...) — regularny wzorzec, nie przypadek.");
    }

    // =================================================================================================
    // 5. NAJWIĘKSZY CZYNNIK PIERWSZY LICZBY 600851475143
    // =================================================================================================

    /**
     * 5. PYTANIE REKRUTACYJNE (Project Euler #3): rozkładamy n na czynniki pierwsze, dzieląc przez kolejne
     * liczby 2, 3, 4, ... — gdy {@code factor} dzieli n, dzielimy n przez niego TAK DŁUGO, jak się da,
     * zanim przejdziemy do następnego czynnika (to gwarantuje, że znalezione czynniki są pierwsze).
     * Pętla idzie tylko do √n (reszta, jeśli zostanie {@code > 1}, to ostatni, największy czynnik pierwszy).
     */
    static void najwiekszyCzynnikPierwszy() {
        section("5. Największy czynnik pierwszy liczby 600851475143");

        show("13195 = 5 × 7 × 13 × 29, największy czynnik", largestPrimeFactor(13195));
        show("największy czynnik pierwszy 600851475143", largestPrimeFactor(600_851_475_143L));
        // WYNIK: 13195 = 5 × 7 × 13 × 29, największy czynnik → 29
        // WYNIK: największy czynnik pierwszy 600851475143 → 6857

        // PUŁAPKA: 600851475143 nie mieści się w int (> 2^31) — trzeba liczyć na long od samego początku,
        //   w tym parametr i zmienne pętli, inaczej dzielenie dałoby błędne wyniki.
    }

    // =================================================================================================
    // 6. LICZBY SZCZĘŚLIWE (HAPPY NUMBERS) ≤ 50
    // =================================================================================================

    /**
     * 6. Liczba szczęśliwa: sumuj KWADRATY jej cyfr, powtarzaj — jeśli w końcu dojdziesz do 1, jest
     * szczęśliwa. Jeśli wpadniesz w pętlę (np. 4→16→37→58→89→145→42→20→4→...), nie jest. Wykrywamy pętlę
     * zbiorem odwiedzonych liczb: gdy trafimy na liczbę, którą już widzieliśmy, a nie jest to 1 — NIESZCZĘŚLIWA.
     */
    static void liczbySzczesliwe() {
        section("6. Liczby szczęśliwe (happy numbers) ≤ 50");

        List<Integer> happyNumbers = new ArrayList<>();
        for (int n = 1; n <= 50; n++) {
            if (isHappy(n)) {
                happyNumbers.add(n);
            }
        }
        show("liczby szczęśliwe ≤ 50", happyNumbers);
        // WYNIK: liczby szczęśliwe ≤ 50 → [1, 7, 10, 13, 19, 23, 28, 31, 32, 44, 49]

        show("isHappy(4) [wpada w pętlę 4→16→...→4]", isHappy(4));
        // WYNIK: isHappy(4) [wpada w pętlę 4→16→...→4] → false
    }

    // =================================================================================================
    // 7. NAJDŁUŻSZY ŁAŃCUCH COLLATZA PONIŻEJ 10 000
    // =================================================================================================

    /**
     * 7. PYTANIE REKRUTACYJNE (Project Euler #14, hipoteza Collatza): z dowolnej liczby n: jeśli parzysta →
     * n/2, jeśli nieparzysta → 3n+1; powtarzaj, aż dojdziesz do 1. Hipoteza mówi, że ZAWSZE się to uda
     * (nieudowodnione!). Szukamy liczby startowej poniżej 10 000 dającej NAJDŁUŻSZY łańcuch — brute force,
     * bo nie ma znanego wzoru.
     */
    static void lancuchCollatza() {
        section("7. Najdłuższy łańcuch Collatza poniżej 10 000");

        show("collatzLength(6)", collatzLength(6));
        // WYNIK: collatzLength(6) → 9

        long[] longest = collatzLongestChainUnder(10_000);
        show("liczba startowa z najdłuższym łańcuchem", longest[0]);
        show("długość tego łańcucha", longest[1]);
        // WYNIK: liczba startowa z najdłuższym łańcuchem → 6171
        // WYNIK: długość tego łańcucha → 262

        // PUŁAPKA: liczby w trakcie łańcucha (3n+1) mogą chwilowo URosnąć znacznie powyżej liczby startowej —
        //   dla liczb bliskich Long.MAX_VALUE mogłoby to przepełnić long (tu, dla n < 10 000, nie grozi).
    }

    // =================================================================================================
    // 8. WIEŻE HANOI: RUCHY DLA n = 3 I WZÓR 2^n − 1
    // =================================================================================================

    /**
     * 8. PYTANIE REKRUTACYJNE: przełóż n krążków z A na C (przez B), nigdy nie kładąc większego na mniejszy.
     * Rekurencja: przełóż n−1 z A na B, przełóż ostatni z A na C, przełóż n−1 z B na C. Liczba ruchów
     * rośnie wykładniczo: T(n) = 2·T(n−1) + 1, co daje wzór zamknięty 2^n − 1.
     */
    static void wiezeHanoi() {
        section("8. Wieże Hanoi: ruchy dla n = 3 i wzór 2^n − 1");

        showEach("ruchy dla n = 3", hanoiMoves(3, 'A', 'B', 'C'));
        show("liczba ruchów dla n = 3 (ze wzoru 2^3 − 1)", hanoiMovesCount(3));
        // WYNIK: ruchy dla n = 3 (liczba elementów: 7):
        // WYNIK:    • A -> C
        // WYNIK:    • A -> B
        // WYNIK:    • C -> B
        // WYNIK:    • A -> C
        // WYNIK:    • B -> A
        // WYNIK:    • B -> C
        // WYNIK:    • A -> C
        // WYNIK: liczba ruchów dla n = 3 (ze wzoru 2^3 − 1) → 7

        show("liczba ruchów dla n = 63 (= Long.MAX_VALUE!)", hanoiMovesCount(63));
        // WYNIK: liczba ruchów dla n = 63 (= Long.MAX_VALUE!) → 9223372036854775807

        show("liczba ruchów dla n = 64 — PUŁAPKA przepełnienia", hanoiMovesCount(64));
        // WYNIK: liczba ruchów dla n = 64 — PUŁAPKA przepełnienia → 0

        // PUŁAPKA: 2^64 − 1 (legenda Wieży Brahmy) jest WIĘKSZE niż Long.MAX_VALUE (2^63 − 1) — już samo to
        //   czyni je niereprezentowalnym w long. Co gorsza, {@code <<} dla long liczy przesunięcie MODULO 64,
        //   więc {@code 1L << 64} to to samo co {@code 1L << 0}, czyli 1 — stąd mylący wynik 0, a nie wyjątek.
        //   Poprawnie trzeba policzyć to przez BigInteger (t24_algorithms/Math08BigNumbers).
    }

    // =================================================================================================
    // FUNKCJE POMOCNICZE
    // =================================================================================================

    static long sumMultiplesOf3Or5BruteForce(int limit) {
        long sum = 0;
        for (int i = 1; i < limit; i++) {
            if (i % 3 == 0 || i % 5 == 0) {
                sum += i;
            }
        }
        return sum;
    }

    /** sumOfMultiplesBelow = suma wielokrotności k mniejszych od limit, wzorem na ciąg arytmetyczny. */
    private static long sumOfMultiplesBelow(long limit, long k) {
        long m = (limit - 1) / k;
        return k * m * (m + 1) / 2;
    }

    static long sumMultiplesOf3Or5Formula(long limit) {
        return sumOfMultiplesBelow(limit, 3) + sumOfMultiplesBelow(limit, 5) - sumOfMultiplesBelow(limit, 15);
    }

    static long sumEvenFibonacciUpTo(long limit) {
        long a = 1;
        long b = 2;
        long sum = 0;
        while (a <= limit) {
            if (a % 2 == 0) {
                sum += a;
            }
            long next = a + b;
            a = b;
            b = next;
        }
        return sum;
    }

    static long largestPrimeFactor(long n) {
        long largest = 1;
        for (long factor = 2; factor * factor <= n; factor++) {
            while (n % factor == 0) {
                largest = factor;
                n /= factor;
            }
        }
        if (n > 1) { largest = n; }                                  // reszta > 1 to ostatni, największy czynnik pierwszy
        return largest;
    }

    private static int sumOfSquareDigits(int n) {
        int sum = 0;
        while (n > 0) {
            int digit = n % 10;
            sum += digit * digit;
            n /= 10;
        }
        return sum;
    }

    static boolean isHappy(int n) {
        Set<Integer> seen = new HashSet<>();
        while (n != 1 && seen.add(n)) {
            n = sumOfSquareDigits(n);
        }
        return n == 1;
    }

    static int collatzLength(long n) {
        int length = 1;
        while (n != 1) {
            n = (n % 2 == 0) ? n / 2 : 3 * n + 1;
            length++;
        }
        return length;
    }

    /** collatzLongestChainUnder = [liczba startowa, długość] najdłuższego łańcucha, dla startu poniżej limitu. */
    static long[] collatzLongestChainUnder(int limit) {
        long bestStart = 1;
        int bestLength = 1;
        for (long start = 1; start < limit; start++) {
            int length = collatzLength(start);
            if (length > bestLength) {
                bestLength = length;
                bestStart = start;
            }
        }
        return new long[]{bestStart, bestLength};
    }

    static List<String> hanoiMoves(int n, char from, char aux, char to) {
        List<String> moves = new ArrayList<>();
        hanoiHelper(n, from, aux, to, moves);
        return moves;
    }

    private static void hanoiHelper(int n, char from, char aux, char to, List<String> moves) {
        if (n == 0) { return; }
        hanoiHelper(n - 1, from, to, aux, moves);
        moves.add(from + " -> " + to);
        hanoiHelper(n - 1, aux, from, to, moves);
    }

    /** hanoiMovesCount = minimalna liczba ruchów dla n krążków: 2^n − 1 (wzór zamknięty, bez rekurencji). */
    static long hanoiMovesCount(int n) {
        return (1L << n) - 1;
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   WIELOKROTNOŚCI 3/5  brute force O(N) kontra wzór k·m(m+1)/2 (ciąg arytm.) O(1); odejmij wielokrotności 15!
     *   PARZYSTE FIBONACCI  generuj iteracyjnie (dwie zmienne), sumuj parzyste w locie — O(log limit) kroków
     *   NAJWIĘKSZY CZYNNIK  dziel przez kolejne liczby od 2, każdą "do oporu"; pętla do √n; uważaj na long dla dużych n
     *   HAPPY NUMBER        sumuj kwadraty cyfr, wykrywaj pętlę zbiorem odwiedzonych (nie licznikiem kroków!)
     *   COLLATZ              n parzyste → n/2, nieparzyste → 3n+1; brute force po wszystkich startach < limit
     *   WIEŻE HANOI          T(n) = 2·T(n−1) + 1  →  2^n − 1 ruchów; rekurencja: n−1 z A na B, 1 z A na C, n−1 z B na C
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego licząc sumę wielokrotności 3 ORAZ 5 wzorem, trzeba OD JĄĆ sumę wielokrotności 15?
     *   2. Co wypisze:  System.out.println(sumMultiplesOf3Or5BruteForce(1));  ? (brak liczb poniżej 1)
     *   3. ZNAJDŹ BŁĄD:  int largest = largestPrimeFactor(600851475143L);  (600851475143 to prawidłowy long, ale...)
     *   4. Dlaczego wykrywanie pętli w isHappy używa zbioru (Set) odwiedzonych liczb, a nie np. licznika "zrób 100 kroków"?
     *   5. Co wypisze:  System.out.println(hanoiMovesCount(1));  ? (jeden krążek)
     *   6. ZNAJDŹ BŁĄD:  for (long start = 1; start < limit; start++) collatzLength(start);  w pętli BEZ
     *      zapamiętywania wyniku — czego tu brakuje, żeby znaleźć NAJDŁUŻSZY łańcuch, a nie tylko ostatni policzony?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: suma wielokrotności 3 lub 5 poniżej 10", 23L, () -> exercise1(10));
        Check.equal("ćw. 2: największy czynnik pierwszy 13195", 29L, () -> exercise2(13195));
        Check.equal("ćw. 3a: isHappy(7)", true, () -> exercise3(7));
        Check.equal("ćw. 3b: isHappy(4)", false, () -> exercise3(4));
        Check.equal("ćw. 4: collatzLength(6)", 9, () -> exercise4(6));
        Check.equal("ćw. 5a: hanoiMovesCount(4)", 15L, () -> exercise5(4));
        Check.equal("ćw. 5b: hanoiMovesCount(10)", 1023L, () -> exercise5(10));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 23L, () -> solution1(10));
        Check.equal("ćw. 2 (wzorzec)", 29L, () -> solution2(13195));
        Check.equal("ćw. 3a (wzorzec)", true, () -> solution3(7));
        Check.equal("ćw. 3b (wzorzec)", false, () -> solution3(4));
        Check.equal("ćw. 4 (wzorzec)", 9, () -> solution4(6));
        Check.equal("ćw. 5a (wzorzec)", 15L, () -> solution5(4));
        Check.equal("ćw. 5b (wzorzec)", 1023L, () -> solution5(10));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 7 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): suma wielokrotności 3 lub 5 poniżej {@code limit} (dowolnym sposobem). */
    static long exercise1(int limit) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /** ĆWICZENIE 2 (łatwe): największy czynnik pierwszy liczby {@code n}. Podpowiedź: dziel przez kolejne liczby od 2. */
    static long exercise2(long n) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /** ĆWICZENIE 3 (średnie): czy {@code n} jest liczbą szczęśliwą? Podpowiedź: sumuj kwadraty cyfr, wykrywaj pętlę zbiorem. */
    static boolean exercise3(int n) {
        // Pusty stub przypadkiem "zaliczyłby" jeden z testów — wyjątek pokazuje ✘, dopóki go nie zrobisz.
        throw new UnsupportedOperationException("TODO");
    }

    /** ĆWICZENIE 4 (średnie): długość łańcucha Collatza dla {@code n} (licząc też wyraz startowy i końcowe 1). */
    static int exercise4(long n) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /** ĆWICZENIE 5 (trudniejsze): liczba ruchów Wież Hanoi dla {@code n} krążków, wzorem (bez rekurencji). */
    static long exercise5(int n) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static long solution1(int limit) {
        return sumMultiplesOf3Or5Formula(limit);
    }

    static long solution2(long n) {
        return largestPrimeFactor(n);
    }

    static boolean solution3(int n) {
        return isHappy(n);
    }

    static int solution4(long n) {
        return collatzLength(n);
    }

    static long solution5(int n) {
        return hanoiMovesCount(n);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Każda wielokrotność 15 jest JEDNOCZEŚNIE wielokrotnością 3 i 5, więc licząc sumy osobno,
     *      policzylibyśmy ją DWA razy — trzeba ją odjąć raz (zasada włączeń-wyłączeń dla dwóch zbiorów).
     *   2. 0 — pętla "for (i=1; i<1; ...)" w ogóle się nie wykonuje, bo 1 nie jest mniejsze od 1.
     *   3. int largest = ... przypisuje WYNIK typu long (largestPrimeFactor zwraca long) do zmiennej int —
     *      to błąd kompilacji (niejawne zawężenie), a nawet gdyby zrzutować, 6857 by się zmieściło, ale
     *      dla innych danych wynik mógłby przekroczyć zakres int i cicho się przepełnić.
     *   4. Licznik kroków wymagałby zgadywania "ile to maksymalnie może trwać" — za mało kroków przerwie
     *      analizę przedwcześnie, za dużo zmarnuje czas. Zbiór odwiedzonych wykrywa pętlę PRECYZYJNIE:
     *      gdy liczba się powtarza, to na pewno jest to pętla (ciąg jest deterministyczny).
     *   5. 1 — dla jednego krążka wystarczy jeden ruch (2^1 − 1 = 1), zgodnie ze wzorem.
     *   6. Brakuje PORÓWNANIA z dotychczasowym najlepszym wynikiem i jego ZAPAMIĘTANIA (bestLength,
     *      bestStart) — bez tego pętla policzy wszystkie długości, ale "zapomni" je, gdy tylko przejdzie
     *      do następnej liczby startowej, i na końcu zostanie tylko wynik dla OSTATNIEGO sprawdzonego n.
     */
    // </editor-fold>
}
