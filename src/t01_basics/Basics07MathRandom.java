package t01_basics;

import helpers.Check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Klasa Math i liczby losowe (Random, ThreadLocalRandom)
 *        (math = matematyka; random = losowy; seed = ziarno)
 *
 * W SKRÓCIE:
 *   Math to „kalkulator naukowy” Javy: wartość bezwzględna, potęgi, pierwiastki, zaokrąglenia, min/max.
 *   Liczby losowe dają klasy Random (z ziarnem — powtarzalne wyniki) i ThreadLocalRandom (szybkie, do wielu wątków).
 *   Najczęstsze błędy: pow zwraca double, zakres losowania „o jeden za mało” i ucięcie zamiast zaokrąglenia.
 *
 * ANALOGIA: kostka do gry z nagraniem.
 *   Random bez ziarna to prawdziwa kostka — za każdym razem inne rzuty. Random z ziarnem (seed) to NAGRANIE
 *   rzutów: odtworzysz je 100 razy i zawsze zobaczysz te same liczby. Świetne do testów i do tej lekcji.
 *
 * JAK TO DZIAŁA:
 *   Math.abs(-5) = 5          Math.max(3, 7) = 7        Math.pow(2, 10) = 1024.0 (double!)
 *   Math.sqrt(9) = 3.0        Math.round(2.5) = 3       Math.floor(2.7) = 2.0     Math.ceil(2.1) = 3.0
 *   random.nextInt(6)         → liczba od 0 do 5 (górna granica NIE wchodzi)
 *   random.nextInt(6) + 1     → od 1 do 6 (kostka)
 *   random.nextInt(max - min + 1) + min   → od min do max włącznie
 *
 * SŁÓWKA:
 *   abs (absolute) = wartość bezwzględna; pow (power) = potęga; sqrt (square root) = pierwiastek kwadratowy;
 *   floor = podłoga (zaokrąglenie w dół); ceil (ceiling) = sufit (w górę); round = zaokrąglij; bound = granica;
 *   origin = początek (zakresu); seed = ziarno; shuffle = przetasuj; thread local = lokalny dla wątku.
 *
 * ZOBACZ TEŻ: t01_basics/Basics05Casting (Math.round a rzutowanie), t01_basics/Basics08FloatingPoint (dokładność double),
 *             t15_numbers/Numbers05IntegerTricks (floorDiv, floorMod), t16_streams/Streams02Creation (random.ints()).
 * </pre>
 */
public class Basics07MathRandom {

    public static void main(String[] args) {
        title("Basics07 — Math i liczby losowe");

        mathBasics();           // math basics = podstawy Math
        rounding();             // rounding = zaokrąglanie
        randomWithSeed();       // random with seed = losowanie z ziarnem
        randomRanges();         // random ranges = zakresy losowania
        randomWithoutSeed();    // random without seed = losowanie bez ziarna
        shuffle();              // shuffle = tasowanie
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PODSTAWY MATH
    // =================================================================================================

    /** 1. Metody klasy Math są statyczne — wołasz je przez nazwę klasy: Math.abs(...), bez tworzenia obiektu. */
    static void mathBasics() {
        section("1. Podstawowe metody Math");

        show("Math.abs(-7)", Math.abs(-7));
        show("Math.max(3, 9)", Math.max(3, 9));
        show("Math.min(3, 9)", Math.min(3, 9));
        show("Math.pow(2, 10)", Math.pow(2, 10));
        show("Math.sqrt(81)", Math.sqrt(81));
        // WYNIK: Math.abs(-7) → 7
        // WYNIK: Math.max(3, 9) → 9
        // WYNIK: Math.min(3, 9) → 3
        // WYNIK: Math.pow(2, 10) → 1024.0    ← pow ZAWSZE zwraca double
        // WYNIK: Math.sqrt(81) → 9.0

        // PUŁAPKA: int x = Math.pow(2, 3);  → błąd kompilacji (double do int). Potrzebne rzutowanie: (int) Math.pow(2, 3).
        //   Do potęg dwójki na liczbach całkowitych szybsze jest przesunięcie bitowe: 1 << 10 (Basics04Operators).
        // Przydatne stałe: Math.PI (3.141592653589793), Math.E (2.718281828459045).
        show("pole koła o promieniu 2", Math.PI * Math.pow(2, 2));
        // WYNIK: pole koła o promieniu 2 → 12.566370614359172
    }

    // =================================================================================================
    // 2. ZAOKRĄGLANIE
    // =================================================================================================

    /** 2. round — do najbliższej całkowitej; floor — zawsze w dół; ceil — zawsze w górę. Ważne przy liczbach ujemnych! */
    static void rounding() {
        section("2. round / floor / ceil");

        show("round(2.5) / floor(2.5) / ceil(2.5)", Math.round(2.5) + " / " + Math.floor(2.5) + " / " + Math.ceil(2.5));
        show("round(-2.5) / floor(-2.5) / ceil(-2.5)", Math.round(-2.5) + " / " + Math.floor(-2.5) + " / " + Math.ceil(-2.5));
        // WYNIK: round(2.5) / floor(2.5) / ceil(2.5) → 3 / 2.0 / 3.0
        // WYNIK: round(-2.5) / floor(-2.5) / ceil(-2.5) → -2 / -3.0 / -2.0    ← floor „w dół” to w stronę minus nieskończoności

        // Zaokrąglenie do 2 miejsc po przecinku (do wyświetlania):
        double value = 3.14159;
        show("3.14159 do 2 miejsc", Math.round(value * 100) / 100.0);
        // WYNIK: 3.14159 do 2 miejsc → 3.14

        // PUŁAPKA: ten trik nie jest idealny: 1.005 * 100 = 100.49999999999999 (przybliżenie double), więc zamiast
        //   oczekiwanego 1.01 wyjdzie 1.0.
        show("1.005 * 100", 1.005 * 100);
        show("1.005 do 2 miejsc (pułapka)", Math.round(1.005 * 100) / 100.0);
        // WYNIK: 1.005 * 100 → 100.49999999999999
        // WYNIK: 1.005 do 2 miejsc (pułapka) → 1.0    ← a „szkolnie” powinno być 1.01
        //   Do pieniędzy i dokładnych zaokrągleń: BigDecimal + RoundingMode (t15_numbers/Numbers01BigDecimal).
    }

    // =================================================================================================
    // 3. RANDOM Z ZIARNEM — POWTARZALNE „LOSOWANIE”
    // =================================================================================================

    /**
     * 3. Random z ziarnem (seed) daje ZAWSZE ten sam ciąg liczb. To nie jest „prawdziwa” losowość, tylko
     * liczby pseudolosowe — wyliczane wzorem z ziarna. Idealne do testów i do powtarzalnych przykładów.
     */
    static void randomWithSeed() {
        section("3. Random z ziarnem — ten sam ciąg przy każdym uruchomieniu");

        Random first = new Random(42);
        Random second = new Random(42);
        show("pierwszy Random(42)", first.nextInt(100) + ", " + first.nextInt(100) + ", " + first.nextInt(100));
        show("drugi Random(42)", second.nextInt(100) + ", " + second.nextInt(100) + ", " + second.nextInt(100));
        // WYNIK: pierwszy Random(42) → 30, 63, 48
        // WYNIK: drugi Random(42) → 30, 63, 48    ← identyczne!

        Random r = new Random(7);
        show("nextDouble (0..1)", r.nextDouble() < 1.0);    // nextDouble = następna liczba z przedziału [0, 1)
        show("nextBoolean", r.nextBoolean());                // nextBoolean = losowe true/false
        // WYNIK: nextDouble (0..1) → true
        // WYNIK: nextBoolean → true
    }

    // =================================================================================================
    // 4. ZAKRESY LOSOWANIA
    // =================================================================================================

    /**
     * 4. nextInt(bound) losuje z [0, bound) — bound (granica) NIE wchodzi. Żeby losować od min do max WŁĄCZNIE:
     * {@code nextInt(max - min + 1) + min}. Od Javy 17 jest też {@code nextInt(origin, bound)} — ale bound znowu wyłączone.
     */
    static void randomRanges() {
        section("4. Zakresy: nextInt(bound), kostka, [min, max]");

        Random dice = new Random(42);
        List<Integer> rolls = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            rolls.add(dice.nextInt(6) + 1);          // 0..5 + 1 → 1..6
        }
        show("10 rzutów kostką (ziarno 42)", rolls);
        // WYNIK: 10 rzutów kostką (ziarno 42) → [3, 4, 1, 3, 1, 2, 6, 3, 2, 6]

        // Sprawdzamy na 10 000 losowaniach, że zakres [5, 10] jest „szczelny”: widać 5 i 10, nic poza.
        Random r = new Random(1);
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < 10_000; i++) {
            int value = r.nextInt(10 - 5 + 1) + 5;   // [5, 10]
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        show("najmniejsza / największa wylosowana", min + " / " + max);
        // WYNIK: najmniejsza / największa wylosowana → 5 / 10

        // PUŁAPKA: nextInt(10) + 1 losuje 1..10, ale nextInt(10) samo — 0..9 (dziesiątki NIGDY nie będzie).
        //   Klasyczny błąd „o jeden” (off-by-one). Zawsze sprawdź granice: najmniejszą i największą możliwą wartość.
    }

    // =================================================================================================
    // 5. LOSOWANIE BEZ ZIARNA
    // =================================================================================================

    /**
     * 5. W prawdziwym programie (gra, losowanie nagród) zwykle NIE chcesz powtarzalności — wtedy bez ziarna.
     * Math.random() zwraca double z [0, 1). ThreadLocalRandom.current() to szybki generator do programów
     * wielowątkowych (t21_concurrency) — ma od razu nextInt(od, do).
     */
    static void randomWithoutSeed() {
        section("5. Bez ziarna: Math.random(), ThreadLocalRandom");

        double any = Math.random();
        int dice = ThreadLocalRandom.current().nextInt(1, 7);     // 1..6 (7 NIE wchodzi)
        show("Math.random()", any);
        show("ThreadLocalRandom 1..6", dice);
        // (wynik zależy od uruchomienia — dlatego sprawdzamy tylko, czy mieści się w zakresie:)
        show("czy w zakresie", any >= 0 && any < 1 && dice >= 1 && dice <= 6);
        // WYNIK: czy w zakresie → true

        // DOBRA PRAKTYKA: do haseł, tokenów i szyfrowania NIE używaj Random ani Math.random — są przewidywalne.
        //   Do bezpieczeństwa służy java.security.SecureRandom.
    }

    // =================================================================================================
    // 6. TASOWANIE
    // =================================================================================================

    /** 6. Collections.shuffle (shuffle = przetasuj) miesza listę losowo. Z Random z ziarnem — powtarzalnie. */
    static void shuffle() {
        section("6. Tasowanie listy (Collections.shuffle)");

        List<String> cards = new ArrayList<>(List.of("A", "K", "Q", "J", "10"));
        Collections.shuffle(cards, new Random(42));
        show("potasowane (ziarno 42)", cards);
        // WYNIK: potasowane (ziarno 42) → [K, Q, J, 10, A]
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Math.abs, max, min, pow (zwraca double!), sqrt, PI; round (do najbliższej, zwraca long), floor (w dół), ceil (w górę).
     *   • Zaokrąglenie do 2 miejsc: Math.round(x * 100) / 100.0 — tylko do wyświetlania (pułapka 1.005 → 1.0); pieniądze → BigDecimal.
     *   • new Random(ziarno) — powtarzalne liczby; new Random() / Math.random() / ThreadLocalRandom — różne za każdym razem.
     *   • nextInt(n) → 0..n-1; kostka: nextInt(6) + 1; zakres [min, max]: nextInt(max - min + 1) + min.
     *   • Collections.shuffle(lista, random) — tasowanie.
     *   • Hasła i tokeny: SecureRandom, nie Random.
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  System.out.println(Math.pow(3, 2));  — 9 czy 9.0?
     *   2. Co wypisze:  System.out.println(Math.floor(-1.5) + " " + Math.ceil(-1.5));  ?
     *   3. ZNAJDŹ BŁĄD (losowanie miesiąca 1..12):  int month = random.nextInt(12);
     *   4. Po co podawać ziarno (seed) do Random?
     *   5. Jak wylosować liczbę z przedziału [10, 20] włącznie?
     *   6. Czy do generowania haseł można użyć Math.random()? Dlaczego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: przeciwprostokątna (3, 4)", 5.0, () -> exercise1(3, 4));
        Check.equal("ćw. 2: 3.14159 do 2 miejsc", 3.14, () -> exercise2(3.14159));
        Check.equal("ćw. 3: losowanie [5, 10] — szczelny zakres", true, () -> rangeIsExact((r, a, b) -> exercise3(r, a, b)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 5.0, () -> solution1(3, 4));
        Check.equal("ćw. 2 (wzorzec)", 3.14, () -> solution2(3.14159));
        Check.equal("ćw. 3 (wzorzec)", true, () -> rangeIsExact((r, a, b) -> solution3(r, a, b)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD
    }

    /** RangeDrawer = „losowacz z zakresu” — interfejs funkcyjny do przekazania Twojej metody (lambdy: t13_lambdas). */
    interface RangeDrawer {
        int draw(Random random, int min, int max);
    }

    /** rangeIsExact = czy zakres jest szczelny: 10 000 losowań z [5, 10] trafia w 5 i w 10, i nigdy poza. */
    static boolean rangeIsExact(RangeDrawer drawer) {
        Random random = new Random(3);
        boolean sawMin = false;
        boolean sawMax = false;
        for (int i = 0; i < 10_000; i++) {
            int v = drawer.draw(random, 5, 10);
            if (v < 5 || v > 10) {
                return false;
            }
            sawMin = sawMin || v == 5;
            sawMax = sawMax || v == 10;
        }
        return sawMin && sawMax;
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz długość przeciwprostokątnej trójkąta prostokątnego o przyprostokątnych a i b.
     * Podpowiedź: twierdzenie Pitagorasa — {@code Math.sqrt(a * a + b * b)} (albo Math.pow(a, 2)).
     */
    static double exercise1(double a, double b) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (średnie): zaokrąglij liczbę do 2 miejsc po przecinku (3.14159 → 3.14).
     * Podpowiedź: {@code Math.round(x * 100) / 100.0} — uważaj, żeby dzielić przez 100.0, a nie 100.
     */
    static double exercise2(double x) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): wylosuj liczbę z przedziału [min, max] WŁĄCZNIE, używając podanego obiektu Random.
     * Sprawdzian wylosuje 10 000 liczb z [5, 10] i sprawdzi, że trafiają się i 5, i 10, a nic poza zakresem.
     * Podpowiedź: {@code random.nextInt(max - min + 1) + min}.
     */
    static int exercise3(Random random, int min, int max) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static double solution1(double a, double b) {
        return Math.sqrt(a * a + b * b);
    }

    static double solution2(double x) {
        return Math.round(x * 100) / 100.0;
    }

    static int solution3(Random random, int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 9.0 — Math.pow zawsze zwraca double.
     *   2. „-2.0 -1.0” — floor idzie w dół (w stronę minus nieskończoności), ceil w górę.
     *   3. nextInt(12) daje 0..11 — miesiąc 12 nigdy nie wypadnie, a 0 nie jest miesiącem. Poprawnie: nextInt(12) + 1.
     *   4. Żeby wyniki były powtarzalne — do testów, do odtwarzania błędów i do przykładów w kursie.
     *   5. random.nextInt(20 - 10 + 1) + 10, czyli nextInt(11) + 10 (albo ThreadLocalRandom.current().nextInt(10, 21)).
     *   6. Nie. Math.random i Random są przewidywalne (liczby wyliczane wzorem); do haseł służy SecureRandom.
     */
    // </editor-fold>
}
