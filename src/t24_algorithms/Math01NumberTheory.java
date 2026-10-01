package t24_algorithms;

import helpers.Check;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Teoria liczb — podzielność, NWD, NWW, liczby pierwsze, sito Eratostenesa
 *        (number theory = teoria liczb; divisibility = podzielność; GCD = NWD (największy wspólny
 *        dzielnik); LCM = NWW (najmniejsza wspólna wielokrotność); prime = liczba pierwsza)
 *
 * W SKRÓCIE:
 *   Teoria liczb bada własności liczb całkowitych: kto kogo dzieli, jaki jest wspólny dzielnik dwóch
 *   liczb, jak rozpoznać liczbę pierwszą. To podstawa kryptografii (RSA), funkcji haszujących i
 *   mnóstwa klasycznych pytań rekrutacyjnych.
 *
 * ANALOGIA: NWD dwóch listewek (48 cm i 18 cm) to długość najdłuższej "miarki", którą zmierzysz OBA
 *   odcinki bez reszty — algorytm Euklidesa to powtarzane odkładanie krótszej od dłuższej do zera.
 *
 * JAK TO DZIAŁA:
 *   Euklides:            NWD(a, b) = NWD(b, a mod b), dopóki b != 0; wtedy NWD = a.
 *   NWW przez NWD:       NWW(a, b) = (a / NWD(a, b)) * b   — dzielimy PRZED mnożeniem (overflow!)
 *   Test pierwszości:    dzielniki sprawdzamy tylko do √n (dzielniki występują w parach d i n/d)
 *   Sito Eratostenesa:   zamiast testować KAŻDĄ liczbę osobno, przekreślamy wielokrotności kolejnych
 *                        liczb pierwszych — raz, dla całego zakresu
 *   Rozkład:             n = p1^e1 * p2^e2 * ...  →  liczba dzielników = (e1+1)*(e2+1)*...
 *
 * SŁÓWKA:
 *   divisor = dzielnik; multiple = wielokrotność; GCD = NWD; LCM = NWW; prime = liczba pierwsza;
 *   composite = liczba złożona; coprime = względnie pierwsze (NWD = 1); trial division = dzielenie
 *   próbne; sieve = sito; factorization = rozkład na czynniki pierwsze; exponent = wykładnik; perfect
 *   number = liczba doskonała; complexity = złożoność; recursion = rekurencja; overflow = przepełnienie.
 *
 * ZOBACZ TEŻ: t24_algorithms/Algorithms01Complexity (złożoność), t15_numbers/Numbers03BigInteger (duże
 *             liczby), t15_numbers/Numbers05IntegerTricks (przepełnienie), t24_algorithms/
 *             Math02ModularChecksums (kolejna lekcja), t03_arrays/Arrays04Algorithms (algorytmy na tablicach).
 * </pre>
 */
public class Math01NumberTheory {

    public static void main(String[] args) {
        title("Math01 — teoria liczb: podzielność, NWD, NWW, liczby pierwsze");

        divisibilityBasics();          // divisibility basics = podstawy podzielności
        gcdEuclidIterative();          // GCD Euclid iterative = NWD Euklidesa iteracyjnie
        gcdEuclidRecursive();          // GCD Euclid recursive = NWD Euklidesa rekurencyjnie
        lcmViaGcd();                   // LCM via GCD = NWW przez NWD
        primalityTrialDivision();      // primality trial division = pierwszość dzieleniem próbnym
        sieveOfEratosthenes();         // sieve of Eratosthenes = sito Eratostenesa
        primeFactorization();          // prime factorization = rozkład na czynniki pierwsze
        numberOfDivisors();            // number of divisors = liczba dzielników
        complexityComparison();        // complexity comparison = porównanie złożoności
        exercises();                   // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PODZIELNOŚĆ — PODSTAWY
    // =================================================================================================

    /**
     * 1. Liczba {@code a} jest podzielna przez {@code b}, gdy {@code a % b == 0}. Dzielniki liczby n
     * znajdziemy, sprawdzając KAŻDĄ liczbę od 1 do n — to działa, ale jest wolne (O(n)) dla dużych n.
     */
    static void divisibilityBasics() {
        section("1. Podzielność — podstawy");

        show("36 podzielne przez 4?", 36 % 4 == 0);
        show("36 podzielne przez 5?", 36 % 5 == 0);
        // WYNIK: 36 podzielne przez 4? → true
        // WYNIK: 36 podzielne przez 5? → false

        List<Integer> divisorsOf36 = divisorsBrute(36);
        show("dzielniki liczby 36 (metoda naiwna, O(n))", divisorsOf36);
        // WYNIK: dzielniki liczby 36 (metoda naiwna, O(n)) → [1, 2, 3, 4, 6, 9, 12, 18, 36]

        // PUŁAPKA: pętla "for (int i = 1; i <= n; i++)" sprawdza n liczb — dla n = 1 000 000 000 to miliard
        //   dzieleń. W sekcji 8 poznasz wersję O(√n), czyli tu ok. 31 623 sprawdzeń zamiast miliarda.
    }

    /** divisorsBrute = wszystkie dzielniki n metodą naiwną (sprawdź każdą liczbę od 1 do n). */
    static List<Integer> divisorsBrute(int n) {
        List<Integer> result = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            if (n % i == 0) result.add(i);
        }
        return result;
    }

    // =================================================================================================
    // 2. NWD (NAJWIĘKSZY WSPÓLNY DZIELNIK) — ALGORYTM EUKLIDESA ITERACYJNIE
    // =================================================================================================

    /**
     * 2. Algorytm Euklidesa: {@code NWD(a, b) = NWD(b, a mod b)}, dopóki {@code b != 0} — wtedy NWD to a.
     * Jeden z najstarszych znanych algorytmów (ok. 300 r. p.n.e.). PYTANIE REKRUTACYJNE: "zaimplementuj
     * NWD bez używania metod bibliotecznych" to klasyk wśród pytań wstępnych dla programistów.
     */
    static void gcdEuclidIterative() {
        section("2. NWD — algorytm Euklidesa iteracyjnie");

        long a = 48;
        long b = 18;
        int step = 1;
        while (b != 0) {
            long remainder = a % b;
            show("krok " + step + ": " + a + " mod " + b, remainder);
            a = b;
            b = remainder;
            step++;
        }
        show("NWD(48, 18)", a);
        // WYNIK: krok 1: 48 mod 18 → 12
        // WYNIK: krok 2: 18 mod 12 → 6
        // WYNIK: krok 3: 12 mod 6 → 0
        // WYNIK: NWD(48, 18) → 6

        show("gcdIterative(48, 18)", gcdIterative(48, 18));
        show("gcdIterative(17, 5)", gcdIterative(17, 5));             // 17 i 5 są WZGLĘDNIE PIERWSZE (coprime)
        show("gcdIterative(0, 7)", gcdIterative(0, 7));                // NWD(0, n) = n
        show("gcdIterative(7, 0)", gcdIterative(7, 0));
        // WYNIK: gcdIterative(48, 18) → 6
        // WYNIK: gcdIterative(17, 5) → 1
        // WYNIK: gcdIterative(0, 7) → 7
        // WYNIK: gcdIterative(7, 0) → 7

        // DOBRA PRAKTYKA: w kodzie produkcyjnym użyj BigInteger.gcd (t15_numbers/Numbers03BigInteger) —
        //   tu uczymy się MECHANIZMU.
    }

    /** gcdIterative = NWD dwóch liczb, algorytm Euklidesa (pętla). */
    static long gcdIterative(long a, long b) {
        while (b != 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return Math.abs(a);
    }

    // =================================================================================================
    // 3. NWD REKURENCYJNIE — DLACZEGO TO DZIAŁA
    // =================================================================================================

    /**
     * 3. Ta sama idea bez pętli: {@code NWD(a, b) = a}, gdy {@code b == 0}; inaczej
     * {@code NWD(b, a mod b)}. DOWÓD: jeśli d dzieli a i b, to dzieli też (a mod b), bo
     * {@code a mod b = a - k*b} dla pewnego całkowitego k (różnica dwóch wielokrotności d jest
     * wielokrotnością d). Więc pary (a,b) i (b, a mod b) mają te same wspólne dzielniki — ten sam NWD.
     */
    static void gcdEuclidRecursive() {
        section("3. NWD rekurencyjnie — ta sama idea, bez pętli");

        show("gcdRecursive(48, 18)", gcdRecursive(48, 18));
        show("gcdRecursive(1071, 462)", gcdRecursive(1071, 462));
        // WYNIK: gcdRecursive(48, 18) → 6
        // WYNIK: gcdRecursive(1071, 462) → 21

        // PUŁAPKA: Java NIE optymalizuje rekurencji ogonowej (brak TCO) — każde wywołanie to nowa ramka
        //   na stosie. Dla NWD głębokość rośnie bardzo wolno (rzędu log mniejszej liczby), więc
        //   StackOverflowError tu praktycznie nie grozi, ale w ogólności trzeba o tym pamiętać.
        // DOBRA PRAKTYKA: wersja iteracyjna (sekcja 2) jest równie czytelna i nie zużywa stosu —
        //   dla NWD wybieraj ją w prawdziwym kodzie; rekurencja jest tu dydaktyczna.
    }

    /** gcdRecursive = NWD dwóch liczb, algorytm Euklidesa (rekurencja). */
    static long gcdRecursive(long a, long b) {
        if (b == 0) return a;
        return gcdRecursive(b, a % b);
    }

    // =================================================================================================
    // 4. NWW (NAJMNIEJSZA WSPÓLNA WIELOKROTNOŚĆ) PRZEZ NWD
    // =================================================================================================

    /**
     * 4. {@code NWW(a, b) = a * b / NWD(a, b)}, ale mnożenie {@code a * b} PIERWSZE może przepełnić typ,
     * zanim zdążymy podzielić. Bezpieczna kolejność: {@code (a / NWD(a, b)) * b} — dzielenie
     * {@code a / NWD(a, b)} jest zawsze CAŁKOWITE (bez reszty), więc nic nie tracimy, a wynik pośredni
     * jest mniejszy.
     */
    static void lcmViaGcd() {
        section("4. NWW przez NWD — kolejność dzielenia i mnożenia ma znaczenie");

        show("NWW(4, 6)", lcmSafe(4, 6));
        show("NWW(21, 6)", lcmSafe(21, 6));
        show("NWW(1, 5)", lcmSafe(1, 5));
        // WYNIK: NWW(4, 6) → 12
        // WYNIK: NWW(21, 6) → 42
        // WYNIK: NWW(1, 5) → 5

        int x = 1_000_000;
        int y = 999_999;                                              // x i y to liczby KOLEJNE → NWD = 1
        long gcdXy = gcdIterative(x, y);
        show("NWD(1000000, 999999)", gcdXy);
        // WYNIK: NWD(1000000, 999999) → 1

        long lcmWrongOrder = (long) (x * y) / gcdXy;                   // ŹLE: x * y liczone na int, PRZEPEŁNIA
        long lcmRightOrder = (long) x / gcdXy * y;                     // DOBRZE: dzielimy najpierw
        show("(x * y) / NWD   (źle — x*y na int)", lcmWrongOrder);
        show("(x / NWD) * y   (dobrze)", lcmRightOrder);
        // WYNIK: (x * y) / NWD   (źle — x*y na int) → -728379968    ← przepełnienie int, wynik bezwartościowy
        // WYNIK: (x / NWD) * y   (dobrze) → 999999000000

        // DOBRA PRAKTYKA: zawsze (a / NWD(a, b)) * b, z typem long gdy liczby mogą być duże — to ten sam
        //   problem co "long x = int * int" w t15_numbers/Numbers05IntegerTricks (sekcja 2 tamtej lekcji).
    }

    /** lcmSafe = NWW dwóch liczb, bezpieczna kolejność dzielenia i mnożenia. */
    static long lcmSafe(long a, long b) {
        long gcd = gcdIterative(a, b);
        return (a / gcd) * b;
    }

    // =================================================================================================
    // 5. TEST PIERWSZOŚCI — DZIELENIE PRÓBNE DO √n
    // =================================================================================================

    /**
     * 5. Aby sprawdzić, czy n jest liczbą pierwszą, wystarczy sprawdzić dzielniki od 2 do √n. Gdyby n
     * miało dzielnik d większy niż √n, to n/d byłoby dzielnikiem MNIEJSZYM niż √n — więc znaleźlibyśmy go
     * wcześniej. PYTANIE REKRUTACYJNE: "napisz funkcję isPrime" to jedno z najpopularniejszych pytań
     * wstępnych na rozmowach kwalifikacyjnych.
     */
    static void primalityTrialDivision() {
        section("5. Test pierwszości — dzielenie próbne do √n");

        int[] candidates = {1, 2, 17, 91, 97, 100};
        for (int n : candidates) {
            show("isPrimeTrial(" + n + ")", isPrimeTrial(n));
        }
        // WYNIK: isPrimeTrial(1) → false
        // WYNIK: isPrimeTrial(2) → true
        // WYNIK: isPrimeTrial(17) → true
        // WYNIK: isPrimeTrial(91) → false
        // WYNIK: isPrimeTrial(97) → true
        // WYNIK: isPrimeTrial(100) → false

        // PUŁAPKA: 91 = 7 × 13 "wygląda" losowo, ale NIE jest pierwsza — trzeba dojść dzielnikiem aż do 9
        //   (√91 ≈ 9,54). Liczby mniejsze od 2 (0, 1, ujemne) NIE są ani pierwsze, ani złożone — osobny
        //   przypadek do obsłużenia na początku funkcji.
    }

    /** isPrimeTrial = test pierwszości dzieleniem próbnym do √n. */
    static boolean isPrimeTrial(long n) {
        if (n < 2) return false;                                      // 0, 1 i liczby ujemne nie są pierwsze
        if (n == 2) return true;
        if (n % 2 == 0) return false;
        for (long i = 3; i * i <= n; i += 2) {                         // tylko nieparzyste dzielniki próbne
            if (n % i == 0) return false;
        }
        return true;
    }

    // =================================================================================================
    // 6. SITO ERATOSTENESA — WSZYSTKIE LICZBY PIERWSZE DO LIMITU
    // =================================================================================================

    /**
     * 6. Sito Eratostenesa: zamiast testować każdą liczbę OSOBNO (dzielenie próbne, O(√n) na liczbę),
     * przekreślamy WIELOKROTNOŚCI kolejnych liczb pierwszych jako złożone. Dla znalezienia wszystkich
     * liczb pierwszych do n daje to O(n log log n) łącznie — znacznie szybciej niż n razy dzielenie próbne.
     */
    static void sieveOfEratosthenes() {
        section("6. Sito Eratostenesa — wszystkie liczby pierwsze do 100");

        List<Integer> primes = primesUpTo(100);
        show("liczby pierwsze ≤ 100", primes);
        show("ile liczb pierwszych ≤ 100", primes.size());
        // WYNIK: liczby pierwsze ≤ 100 → [2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97]
        // WYNIK: ile liczb pierwszych ≤ 100 → 25

        // Wewnętrzna pętla zaczyna przekreślanie od i*i — mniejsze wielokrotności (2*i, 3*i, ...) już
        //   przekreśliły wcześniej MNIEJSZE liczby pierwsze. DOBRA PRAKTYKA: sito buduj RAZ i pytaj
        //   wielokrotnie — dla POJEDYNCZEGO sprawdzenia wystarczy isPrimeTrial (sekcja 5).
    }

    /** primesUpTo = wszystkie liczby pierwsze w przedziale [2, limit] (sito Eratostenesa). */
    static List<Integer> primesUpTo(int limit) {
        if (limit < 2) return List.of();
        boolean[] composite = new boolean[limit + 1];                  // composite = złożona (nie pierwsza)
        for (int i = 2; (long) i * i <= limit; i++) {
            if (!composite[i]) {
                for (int multiple = i * i; multiple <= limit; multiple += i) {
                    composite[multiple] = true;
                }
            }
        }
        List<Integer> primes = new ArrayList<>();
        for (int i = 2; i <= limit; i++) {
            if (!composite[i]) primes.add(i);
        }
        return primes;
    }

    // =================================================================================================
    // 7. ROZKŁAD NA CZYNNIKI PIERWSZE
    // =================================================================================================

    /**
     * 7. Rozkład na czynniki pierwsze: dzielimy n kolejno przez 2, 3, 4, ... dopóki się da — każdy udany
     * podział to jeden czynnik. To, co zostanie na końcu większe od 1, jest OSTATNIM czynnikiem pierwszym
     * (bo gdyby było złożone, jego najmniejszy dzielnik zostałby znaleziony wcześniej).
     */
    static void primeFactorization() {
        section("7. Rozkład na czynniki pierwsze");

        Map<Integer, Integer> factors360 = primeFactorsOf(360);
        show("360 jako mapa (baza → wykładnik)", factors360);
        show("360 jako napis", factorizationToString(factors360));
        // WYNIK: 360 jako mapa (baza → wykładnik) → {2=3, 3=2, 5=1}
        // WYNIK: 360 jako napis → 2^3 × 3^2 × 5

        show("97 jako mapa (liczba pierwsza)", primeFactorsOf(97));
        show("97 jako napis", factorizationToString(primeFactorsOf(97)));
        show("1 jako mapa (brak czynników)", primeFactorsOf(1));
        show("1 jako napis", factorizationToString(primeFactorsOf(1)));
        // WYNIK: 97 jako mapa (liczba pierwsza) → {97=1}
        // WYNIK: 97 jako napis → 97
        // WYNIK: 1 jako mapa (brak czynników) → {}
        // WYNIK: 1 jako napis → (brak — liczba 1 nie ma czynników pierwszych)

        // DOBRA PRAKTYKA: TreeMap daje DETERMINISTYCZNĄ kolejność kluczy (rosnąco) — czytelny wynik
        //   "od najmniejszego czynnika", bez polegania na przypadkowej stabilności HashMap dla Integer.
    }

    /** primeFactorsOf = rozkład n na czynniki pierwsze jako mapa baza → wykładnik (posortowana). */
    static Map<Integer, Integer> primeFactorsOf(int n) {
        Map<Integer, Integer> factors = new TreeMap<>();                // TreeMap = mapa posortowana po kluczu
        int remaining = n;
        for (int factor = 2; (long) factor * factor <= remaining; factor++) {
            while (remaining % factor == 0) {
                factors.merge(factor, 1, Integer::sum);                 // merge = dodaj albo zsumuj istniejącą wartość
                remaining /= factor;
            }
        }
        if (remaining > 1) {
            factors.merge(remaining, 1, Integer::sum);
        }
        return factors;
    }

    /** factorizationToString = rozkład jako czytelny napis, np. "2^3 × 3^2 × 5". */
    static String factorizationToString(Map<Integer, Integer> factors) {
        if (factors.isEmpty()) return "(brak — liczba 1 nie ma czynników pierwszych)";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : factors.entrySet()) {
            if (sb.length() > 0) sb.append(" × ");
            sb.append(entry.getKey());
            if (entry.getValue() > 1) sb.append('^').append(entry.getValue());
        }
        return sb.toString();
    }

    // =================================================================================================
    // 8. LICZBA DZIELNIKÓW LICZBY
    // =================================================================================================

    /**
     * 8. Liczbę dzielników n odczytamy wprost z rozkładu: jeśli {@code n = p1^e1 * p2^e2 * ...}, to liczba
     * dzielników to {@code (e1+1) * (e2+1) * ...} — każdy dzielnik wybiera NIEZALEŻNIE wykładnik od 0 do
     * ei dla każdego pi. Alternatywnie: licz dzielniki w PARACH (i oraz n/i) tylko do √n — O(√n).
     */
    static void numberOfDivisors() {
        section("8. Liczba dzielników liczby");

        Map<Integer, Integer> factors360 = primeFactorsOf(360);
        long fromFactorization = numberOfDivisorsFromFactors(factors360);
        long fromSqrtCount = countDivisorsSqrt(360);
        show("liczba dzielników 360 (z rozkładu, wzór)", fromFactorization);
        show("liczba dzielników 360 (licząc parami do √n)", fromSqrtCount);
        show("obie metody dają ten sam wynik?", fromFactorization == fromSqrtCount);
        // WYNIK: liczba dzielników 360 (z rozkładu, wzór) → 24
        // WYNIK: liczba dzielników 360 (licząc parami do √n) → 24
        // WYNIK: obie metody dają ten sam wynik? → true

        show("liczba dzielników 97 (liczba pierwsza)", countDivisorsSqrt(97));
        show("liczba dzielników 1", countDivisorsSqrt(1));
        // WYNIK: liczba dzielników 97 (liczba pierwsza) → 2
        // WYNIK: liczba dzielników 1 → 1
    }

    /** numberOfDivisorsFromFactors = liczba dzielników ze wzoru (e1+1)*(e2+1)*... */
    static long numberOfDivisorsFromFactors(Map<Integer, Integer> factors) {
        long count = 1;
        for (int exponent : factors.values()) {
            count *= (exponent + 1);
        }
        return count;
    }

    /** countDivisorsSqrt = liczba dzielników n licząc PARAMI (i oraz n/i) tylko do √n — O(√n). */
    static long countDivisorsSqrt(long n) {
        long count = 0;
        for (long i = 1; i * i <= n; i++) {
            if (n % i == 0) {
                count++;
                if (i != n / i) count++;                               // i i n/i to RÓŻNE dzielniki (poza pierwiastkiem)
            }
        }
        return count;
    }

    // =================================================================================================
    // 9. PORÓWNANIE ZŁOŻONOŚCI: DZIELENIE PRÓBNE KONTRA SITO
    // =================================================================================================

    /**
     * 9. Testowanie pierwszości KAŻDEJ liczby osobno (dzielenie próbne, O(√n) na liczbę) kontra zbudowanie
     * sita RAZ (O(n log log n) łącznie) i odczytywanie gotowej odpowiedzi. Liczymy WYKONANE OPERACJE
     * (dzielenia / oznaczenia), nie czas — czas zależy od komputera, liczba operacji nie.
     */
    static void complexityComparison() {
        section("9. Porównanie złożoności: dzielenie próbne kontra sito");

        int limit = 10_000;
        long trialOps = totalTrialDivisionSteps(limit);
        long sieveOps = sieveMarkOperations(limit);
        show("limit", limit);
        show("łączna liczba sprawdzeń (dzielenie próbne każdej liczby OSOBNO)", trialOps);
        show("łączna liczba oznaczeń w sicie (RAZ dla całego zakresu)", sieveOps);
        // WYNIK: limit → 10000
        // WYNIK: łączna liczba sprawdzeń (dzielenie próbne każdej liczby OSOBNO) → 117527
        // WYNIK: łączna liczba oznaczeń w sicie (RAZ dla całego zakresu) → 16981

        // Dla JEDNEGO zapytania dzielenie próbne wystarczy; dla WIELU (np. wszystkie liczby pierwsze do
        //   miliona) sito jest znacznie efektywniejsze — dokładne klasy złożoności są podsumowane niżej.
    }

    /** totalTrialDivisionSteps = suma kroków dzielenia próbnego dla każdej liczby 2..limit osobno. */
    static int totalTrialDivisionSteps(int limit) {
        int totalSteps = 0;
        for (int n = 2; n <= limit; n++) {
            totalSteps += trialDivisionSteps(n);
        }
        return totalSteps;
    }

    /** trialDivisionSteps = ile sprawdzeń "n % i == 0" wykonała pętla dla pojedynczego n. */
    static int trialDivisionSteps(int n) {
        int steps = 0;
        for (int i = 2; (long) i * i <= n; i++) {
            steps++;
            if (n % i == 0) break;
        }
        return steps;
    }

    /** sieveMarkOperations = ile razy sito oznaczyło liczbę jako złożoną, budując zakres [2, limit] RAZ. */
    static long sieveMarkOperations(int limit) {
        boolean[] composite = new boolean[limit + 1];
        long marks = 0;
        for (int i = 2; (long) i * i <= limit; i++) {
            if (!composite[i]) {
                for (int multiple = i * i; multiple <= limit; multiple += i) {
                    composite[multiple] = true;
                    marks++;
                }
            }
        }
        return marks;
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   PODZIELNOŚĆ:    a % b == 0  (dzielniki naiwnie: O(n), parami do √n: O(√n))
     *   NWD (Euklides):  NWD(a, b) = NWD(b, a mod b), aż b == 0 → NWD = a              [O(log min(a,b))]
     *   NWW:             (a / NWD(a, b)) * b  — NIGDY (a * b) / NWD(a, b) dla dużych liczb (przepełnienie)
     *   PIERWSZOŚĆ:      dzielniki próbne tylko do √n                                  [O(√n) na liczbę]
     *   SITO:            przekreśl wielokrotności kolejnych liczb pierwszych, start od i*i [O(n log log n)]
     *   ROZKŁAD:         n = p1^e1 * p2^e2 * ...  (dziel kolejno przez 2, 3, 4, ...)    [O(√n)]
     *   DZIELNIKI:       liczba dzielników = (e1+1) * (e2+1) * ...
     *   KIEDY CO:        jedno zapytanie → dzielenie próbne; wiele zapytań / cały zakres → sito
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego do testu pierwszości liczby n wystarczy sprawdzić dzielniki do √n, a nie do n?
     *   2. Co wypisze:  System.out.println(gcdIterative(0, 7) + " " + gcdIterative(7, 0));  ?
     *   3. ZNAJDŹ BŁĄD: "static boolean isPrime(int n) { for (int i = 2; i < n; i++) if (n % i == 0)
     *      return false; return true; }" — sprawdź dla n = 1 i n = 2, i oceń złożoność.
     *   4. Dlaczego NWW liczymy jako (a / NWD(a, b)) * b, a nie (a * b) / NWD(a, b)? Co może pójść źle
     *      w drugiej wersji dla dużych liczb typu int?
     *   5. Co wypisze:  System.out.println(360 % 7 == 0);  ?
     *   6. Dlaczego wewnętrzna pętla sita Eratostenesa dla liczby i może zaczynać przekreślanie od i*i,
     *      a nie od 2*i — i dlaczego to NIE jest błąd, tylko optymalizacja?
     *   7. ZNAJDŹ BŁĄD: ktoś twierdzi, że liczba 1 jest pierwsza, bo "dzieli się tylko przez siebie i
     *      przez 1". Co jest nie tak w tym rozumowaniu (podpowiedź: definicja liczby pierwszej mówi
     *      o DOKŁADNIE dwóch różnych dzielnikach)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Uruchamia ćwiczenia: najpierw TWOJE rozwiązania (✘ dopóki nie uzupełnisz), potem wzorcowe (✔). */
    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: isPrime(97)", true, () -> exercise1(97));
        Check.equal("ćw. 1b: isPrime(91)", false, () -> exercise1(91));
        Check.equal("ćw. 1c: isPrime(1)", false, () -> exercise1(1));
        Check.equal("ćw. 1d: isPrime(2)", true, () -> exercise1(2));
        Check.equal("ćw. 2a: NWW(4, 6)", 12L, () -> exercise2(4, 6));
        Check.equal("ćw. 2b: NWW(21, 6)", 42L, () -> exercise2(21, 6));
        Check.equal("ćw. 3a: dzielniki 36 (szybko)", 9, () -> exercise3(36));
        Check.equal("ćw. 3b: dzielniki 97 (liczba pierwsza)", 2, () -> exercise3(97));
        Check.equal("ćw. 3c: dzielniki 1", 1, () -> exercise3(1));
        Check.equal("ćw. 4a: suma właść. dzielników 6 (doskonała)", 6, () -> exercise4(6));
        Check.equal("ćw. 4b: suma właść. dzielników 28 (doskonała)", 28, () -> exercise4(28));
        Check.equal("ćw. 4c: suma właść. dzielników 10", 8, () -> exercise4(10));
        Check.equal("ćw. 5a: rozkład 360", new TreeMap<>(Map.of(2, 3, 3, 2, 5, 1)), () -> exercise5(360));
        Check.equal("ćw. 5b: rozkład 97", new TreeMap<>(Map.of(97, 1)), () -> exercise5(97));
        Check.equal("ćw. 5c: rozkład 1 (pusta mapa)", new TreeMap<Integer, Integer>(), () -> exercise5(1));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", true, () -> solution1(97));
        Check.equal("ćw. 1b (wzorzec)", false, () -> solution1(91));
        Check.equal("ćw. 1c (wzorzec)", false, () -> solution1(1));
        Check.equal("ćw. 1d (wzorzec)", true, () -> solution1(2));
        Check.equal("ćw. 2a (wzorzec)", 12L, () -> solution2(4, 6));
        Check.equal("ćw. 2b (wzorzec)", 42L, () -> solution2(21, 6));
        Check.equal("ćw. 3a (wzorzec)", 9, () -> solution3(36));
        Check.equal("ćw. 3b (wzorzec)", 2, () -> solution3(97));
        Check.equal("ćw. 3c (wzorzec)", 1, () -> solution3(1));
        Check.equal("ćw. 4a (wzorzec)", 6, () -> solution4(6));
        Check.equal("ćw. 4b (wzorzec)", 28, () -> solution4(28));
        Check.equal("ćw. 4c (wzorzec)", 8, () -> solution4(10));
        Check.equal("ćw. 5a (wzorzec)", new TreeMap<>(Map.of(2, 3, 3, 2, 5, 1)), () -> solution5(360));
        Check.equal("ćw. 5b (wzorzec)", new TreeMap<>(Map.of(97, 1)), () -> solution5(97));
        Check.equal("ćw. 5c (wzorzec)", new TreeMap<Integer, Integer>(), () -> solution5(1));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 15 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): sprawdź, czy n jest liczbą pierwszą (dzielenie próbne do √n) — napisz
     * samodzielnie, bez wywoływania isPrimeTrial z sekcji 5. Liczby mniejsze od 2 nie są pierwsze.
     * Podpowiedź: {@code for (int i = 2; i * i <= n; i++) { if (n % i == 0) return false; }}.
     */
    static boolean exercise1(int n) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): policz NWW dwóch liczb bezpiecznie (bez przepełnienia) — możesz użyć
     * gcdIterative z sekcji 2.
     * Podpowiedź: {@code (a / NWD(a, b)) * b}.
     */
    static long exercise2(int a, int b) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 3 (średnie) — PRZEPISZ wersję naiwną na szybszą:
     * <pre>{@code
     * static int countDivisorsSlow(int n) {              // O(n) — sprawdza KAŻDĄ liczbę od 1 do n
     *     int count = 0;
     *     for (int i = 1; i <= n; i++) {
     *         if (n % i == 0) count++;
     *     }
     *     return count;
     * }
     * }</pre>
     * Napisz wersję O(√n): licz dzielniki PARAMI (i oraz n/i), uważając na przypadek i == n/i (pierwiastek).
     * Podpowiedź: pętla tylko do {@code i * i <= n}; zobacz countDivisorsSqrt w sekcji 8.
     */
    static int exercise3(int n) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 4 (średnie): policz sumę WŁAŚCIWYCH dzielników n (czyli wszystkich dzielników poza
     * samym n). Liczba jest "doskonała" (perfect number), gdy ta suma równa się n — więcej o tym
     * w Math09InterviewClassics.
     * Podpowiedź: {@code for (int i = 1; i < n; i++) if (n % i == 0) sum += i;} — dla liczb z tego
     * ćwiczenia prosta pętla do n wystarczy.
     */
    static int exercise4(int n) {
        // TODO: twoje rozwiązanie
        return -1;
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): rozłóż n na czynniki pierwsze i zwróć mapę baza → wykładnik
     * (posortowaną — użyj TreeMap). Dla n == 1 zwróć pustą mapę.
     * Podpowiedź: dziel kolejno przez i = 2, 3, 4, ..., dopóki {@code i * i <= pozostała część}; to,
     * co zostanie większe od 1, jest ostatnim czynnikiem.
     */
    static Map<Integer, Integer> exercise5(int n) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(int n) {
        if (n < 2) return false;
        for (int i = 2; (long) i * i <= n; i++) {
            if (n % i == 0) return false;
        }
        return true;
    }

    static long solution2(int a, int b) {
        return lcmSafe(a, b);
    }

    static int solution3(int n) {
        int count = 0;
        for (int i = 1; (long) i * i <= n; i++) {
            if (n % i == 0) {
                count++;
                if (i != n / i) count++;
            }
        }
        return count;
    }

    static int solution4(int n) {
        int sum = 0;
        for (int i = 1; i < n; i++) {
            if (n % i == 0) sum += i;
        }
        return sum;
    }

    static Map<Integer, Integer> solution5(int n) {
        return primeFactorsOf(n);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Dzielniki liczby n zawsze występują w PARACH d i n/d. Gdyby n miało dzielnik d > √n, to
     *      n/d < √n byłoby drugim dzielnikiem z tej pary — więc znaleźlibyśmy go wcześniej, sprawdzając
     *      tylko do √n. Sprawdzanie dalej nic nowego by nie wniosło.
     *   2. "7 7" — NWD(0, n) = n (bo n dzieli i 0, i n, i jest największym takim dzielnikiem), a
     *      NWD(n, 0) zwraca od razu a = n, bo pętla w ogóle się nie wykonuje (b == 0 na starcie).
     *   3. Dla n = 1: pętla w ogóle się nie wykona (i = 2, warunek 2 < 1 fałszywy), metoda zwróci true —
     *      błędnie, bo 1 NIE jest liczbą pierwszą (brakuje warunku n < 2 → false). Dla n = 2 metoda
     *      działa poprawnie (zwróci true), ale złożoność to O(n) zamiast O(√n) — dla dużych liczb
     *      pierwszych dzieli przez WSZYSTKIE liczby do n-1, a wystarczyłoby do √n.
     *   4. (a * b) najpierw mnoży obie liczby — dla liczb typu int wynik może łatwo przekroczyć zakres
     *      int (przepełnienie, zob. t15_numbers/Numbers05IntegerTricks) ZANIM zdążymy podzielić przez
     *      NWD. (a / NWD(a, b)) * b dzieli najpierw (dokładnie, bez reszty), więc wynik pośredni jest
     *      mniejszy i rzadziej przepełnia.
     *   5. "false" — 360 / 7 = 51 reszty 3, więc 360 % 7 == 3, a nie 0.
     *   6. Każda wielokrotność i mniejsza niż i*i (czyli 2*i, 3*i, ..., (i-1)*i) ma czynnik mniejszy niż i
     *      (np. 3*i ma czynnik 3), więc została już przekreślona, gdy przetwarzaliśmy TEN mniejszy
     *      czynnik. Zaczynanie od i*i nie psuje poprawności — tylko pomija oznaczenia, które i tak już
     *      są zrobione.
     *   7. Definicja liczby pierwszej wymaga DOKŁADNIE dwóch różnych dzielników: 1 i samej siebie. Liczba
     *      1 ma tylko JEDEN dzielnik (samą siebie, bo 1 == 1), więc nie spełnia definicji — nie jest ani
     *      pierwsza, ani złożona, to osobna kategoria.
     */
    // </editor-fold>
}
