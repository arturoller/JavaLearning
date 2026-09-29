package t13_lambdas;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Funkcje wyższego rzędu — funkcje, które przyjmują albo zwracają inne funkcje
 *        (higher-order function = funkcja wyższego rzędu)
 *
 * W SKRÓCIE:
 *   Skoro lambda jest obiektem, metoda może ją PRZYJĄĆ (jak list.sort(komparator)) albo ZWRÓCIĆ (jak fabryka
 *   rabatów percentDiscount(20)). Takie metody i funkcje nazywamy funkcjami wyższego rzędu. Dzięki nim piszesz
 *   ogólny „szkielet” raz, a zmienną część (strategię, warunek, krok) wstawiasz z zewnątrz. Poznasz też:
 *   currying (funkcja zwracająca funkcję zamiast dwóch parametrów naraz), leniwe liczenie przez Supplier,
 *   tablicę poleceń (mapa nazwa → funkcja) i memoizację (zapamiętywanie wyników).
 *
 * ANALOGIA: robot kuchenny z wymiennymi końcówkami.
 *   Robot (funkcja wyższego rzędu) zna ogólną pracę: kręć, trzymaj, pilnuj czasu. Co dokładnie robi — ubija, kroi,
 *   ugniata — zależy od końcówki (przekazanej lambdy). A fabryka końcówek (funkcja zwracająca funkcję) na zamówienie
 *   „tarka o oczkach 5 mm” wytwarza nową końcówkę z zadanym parametrem.
 *
 * JAK TO DZIAŁA:
 *   Przyjmuje funkcję:   {@code static int applyTwice(IntUnaryOperator f, int x) { return f.applyAsInt(f.applyAsInt(x)); }}
 *   Zwraca funkcję:      {@code static IntUnaryOperator multiplier(int n) { return x -> x * n; }}
 *   Zwrócona lambda PAMIĘTA parametr n (domknięcie — Lambda06ClosuresScope). multiplier(3) i multiplier(10)
 *   to dwie różne funkcje z tego samego „przepisu”.
 *
 * SŁÓWKA:
 *   higher-order = wyższego rzędu; multiplier = mnożnik; discount = rabat; percent = procent; currying = rozwijanie
 *   funkcji (od nazwiska matematyka Haskella Curry'ego); partial application = częściowe zastosowanie; strategy = strategia;
 *   checkout = kasa (podsumowanie zakupu); lazy = leniwy; eager = gorliwy; expensive = kosztowny; command = polecenie;
 *   operation = działanie; memoize = zapamiętuj wyniki; cache = pamięć podręczna; retry = ponów; attempt = próba.
 *
 * ZOBACZ TEŻ: Lambda05Composition (andThen, compose — też funkcje wyższego rzędu), t22_design_patterns/Patterns01Strategy
 *             (wzorzec strategii), t22_design_patterns/Patterns09Command (wzorzec polecenia), t14_optional (orElseGet — leniwość),
 *             t15_numbers/Numbers01BigDecimal (BigDecimal i zaokrąglanie).
 * </pre>
 */
public class Lambda07HigherOrderFunctions {

    /** HUNDRED = sto — do liczenia procentów. */
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** expensiveCalls = licznik wywołań „kosztownej” metody (sekcja 5). */
    private static int expensiveCalls = 0;

    /** naiveFibCalls = licznik wywołań naiwnego Fibonacciego (sekcja 7). */
    private static long naiveFibCalls = 0;

    public static void main(String[] args) {
        title("Lambda07 — funkcje wyższego rzędu");

        functionsAsParameters();      // functions as parameters = funkcje jako parametry
        functionsReturningFunctions();// functions returning functions = funkcje zwracające funkcje
        currying();                   // currying = rozwijanie funkcji
        strategyAsLambda();           // strategy as lambda = strategia jako lambda
        lazyEvaluation();             // lazy evaluation = leniwe obliczanie (Supplier)
        commandTable();               // command table = tablica poleceń (mapa funkcji)
        memoization();                // memoization = memoizacja (zapamiętywanie wyników)
        returningPredicates();        // returning predicates = metody zwracające predykaty
        retryWrapper();               // retry wrapper = opakowanie „ponów przy błędzie”
        exercises();                  // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. FUNKCJE JAKO PARAMETRY
    // =================================================================================================

    /** applyTwice = zastosuj dwa razy. Przyjmuje FUNKCJĘ i wartość — funkcja wyższego rzędu. */
    static <T> T applyTwice(UnaryOperator<T> f, T value) {
        return f.apply(f.apply(value));
    }

    /**
     * 1. Funkcja wyższego rzędu to taka, która przyjmuje funkcję jako parametr (albo zwraca funkcję — sekcja 2).
     * Znasz je od dawna: list.sort(Comparator), list.removeIf(Predicate), map.computeIfAbsent(klucz, Function).
     */
    static void functionsAsParameters() {
        section("1. Funkcje jako parametry — applyTwice");

        show("applyTwice(x -> x * 3, 2)", applyTwice(x -> x * 3, 2));
        show("applyTwice(s -> s + \"!\", \"hej\")", applyTwice(s -> s + "!", "hej"));
        show("applyTwice(String::strip, \"  ok \")", "[" + applyTwice(String::strip, "  ok ") + "]");
        // WYNIK: applyTwice(x -> x * 3, 2) → 18    ← (2 * 3) * 3
        // WYNIK: applyTwice(s -> s + "!", "hej") → hej!!
        // WYNIK: applyTwice(String::strip, "  ok ") → [ok]    ← drugie strip nic już nie zmienia

        // applyTwice nie wie, CO robi f — zna tylko schemat „zastosuj dwa razy”. Szkielet napisany raz,
        // zachowanie wstawiane z zewnątrz. To jest sedno funkcji wyższego rzędu.
    }

    // =================================================================================================
    // 2. FUNKCJE ZWRACAJĄCE FUNKCJE (FABRYKI)
    // =================================================================================================

    /** multiplier = mnożnik. Fabryka funkcji: dla n zwraca funkcję „pomnóż przez n”. */
    static IntUnaryOperator multiplier(int n) {
        return x -> x * n;                          // lambda pamięta n (domknięcie)
    }

    /**
     * percentDiscount = rabat procentowy. Dla percent = 20 zwraca funkcję: cena → cena × 0.80, zaokrąglona do groszy.
     * divide = podziel; setScale(2, HALF_UP) = 2 miejsca po przecinku, zaokrąglanie „szkolne” (t15_numbers).
     */
    static UnaryOperator<BigDecimal> percentDiscount(int percent) {
        if (percent < 0 || percent > 100) {         // walidacja RAZ, przy tworzeniu funkcji — a nie przy każdym użyciu
            throw new IllegalArgumentException("Rabat musi być w zakresie 0–100%: " + percent);
        }
        BigDecimal factor = BigDecimal.valueOf(100 - percent).divide(HUNDRED);   // np. 80 / 100 = 0.8 (factor = mnożnik)
        return price -> price.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 2. Metoda może ZWRACAĆ lambdę. Parametry metody „zamrażają się” w zwróconej funkcji — dostajesz fabrykę
     * gotowych, skonfigurowanych funkcji.
     */
    static void functionsReturningFunctions() {
        section("2. Funkcje zwracające funkcje — fabryki");

        IntUnaryOperator triple = multiplier(3);                     // triple = potrój
        IntUnaryOperator tenTimes = multiplier(10);
        show("triple(7) / tenTimes(7)", triple.applyAsInt(7) + " / " + tenTimes.applyAsInt(7));
        // WYNIK: triple(7) / tenTimes(7) → 21 / 70

        UnaryOperator<BigDecimal> minus20 = percentDiscount(20);
        UnaryOperator<BigDecimal> minus15 = percentDiscount(15);
        show("-20% od 129.00", minus20.apply(new BigDecimal("129.00")));
        show("-15% od 49.99", minus15.apply(new BigDecimal("49.99")));
        // WYNIK: -20% od 129.00 → 103.20
        // WYNIK: -15% od 49.99 → 42.49    ← 42.4915 zaokrąglone do groszy

        expectThrows("percentDiscount(120)", () -> percentDiscount(120));
        // WYNIK: ✔ percentDiscount(120) → rzucono IllegalArgumentException: Rabat musi być w zakresie 0–100%: 120
        // DOBRA PRAKTYKA: w fabryce sprawdź parametry i przygotuj stałe (factor) RAZ — zwrócona lambda zostaje prosta
        //   i szybka, a błędna konfiguracja wychodzi od razu, a nie przy pierwszym kliencie.
    }

    // =================================================================================================
    // 3. CURRYING — FUNKCJA ZWRACAJĄCA FUNKCJĘ ZAMIAST DWÓCH PARAMETRÓW
    // =================================================================================================

    /** curry = rozwiń. Zamienia funkcję dwuargumentową (a, b) → r na łańcuch a → (b → r). */
    static <A, B, R> Function<A, Function<B, R>> curry(BiFunction<A, B, R> f) {
        return a -> b -> f.apply(a, b);
    }

    /**
     * 3. Currying: zamiast funkcji dwóch argumentów {@code (a, b) -> a + b} piszemy funkcję jednego argumentu,
     * która zwraca KOLEJNĄ funkcję jednego argumentu: {@code a -> b -> a + b}. Czytaj od prawej: {@code a -> (b -> a + b)}.
     * Zaleta: można podać tylko pierwszy argument i dostać „wstępnie skonfigurowaną” funkcję (partial application).
     */
    static void currying() {
        section("3. Currying — a -> b -> wynik");

        Function<Integer, Function<Integer, Integer>> add = a -> b -> a + b;
        show("add.apply(3).apply(4)", add.apply(3).apply(4));
        // WYNIK: add.apply(3).apply(4) → 7

        Function<Integer, Integer> addFive = add.apply(5);           // częściowe zastosowanie: a = 5 „zamrożone”
        show("addFive.apply(10)", addFive.apply(10));
        // WYNIK: addFive.apply(10) → 15

        // Praktyczniejszy przykład: szablon powitania — najpierw powitanie, potem imię.
        Function<String, Function<String, String>> greet = greeting -> name -> greeting + ", " + name + "!";
        Function<String, String> formal = greet.apply("Szanowny Panie");
        Function<String, String> casual = greet.apply("Siema");
        show("formal / casual", formal.apply("Kowalski") + " | " + casual.apply("Piotrek"));
        // WYNIK: formal / casual → Szanowny Panie, Kowalski! | Siema, Piotrek!

        // Dowolną BiFunction można „rozwinąć” metodą curry:
        Function<Integer, Function<Integer, Integer>> power = curry((base, exp) -> (int) Math.pow(base, exp));   // pow = potęga
        Function<Integer, Integer> powersOfTwo = power.apply(2);     // 2^x
        show("potęgi dwójki 2^1..2^4", powersOfTwo.apply(1) + " " + powersOfTwo.apply(2) + " "
                + powersOfTwo.apply(3) + " " + powersOfTwo.apply(4));
        // WYNIK: potęgi dwójki 2^1..2^4 → 2 4 8 16

        // PUŁAPKA: typ Function<Integer, Function<Integer, Integer>> szybko staje się nieczytelny.
        //   W Javie currying stosuj oszczędnie — zwykle czytelniejsza jest metoda-fabryka z nazwą (sekcja 2).
    }

    // =================================================================================================
    // 4. STRATEGIA PRZEKAZANA JAKO LAMBDA
    // =================================================================================================

    /** checkout = kasa. Sumuje ceny i stosuje przekazaną strategię promocji (promotion = promocja). */
    static BigDecimal checkout(List<BigDecimal> prices, UnaryOperator<BigDecimal> promotion) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal price : prices) {
            sum = sum.add(price);
        }
        return promotion.apply(sum);
    }

    /**
     * 4. Wzorzec Strategia (t22_design_patterns/Patterns01Strategy) w wersji z lambdą: zamiast hierarchii klas
     * PromotionStrategy, NoPromotion, PercentPromotion... przekazujesz po prostu funkcję.
     */
    static void strategyAsLambda() {
        section("4. Strategia jako lambda — jedna kasa, różne promocje");

        List<BigDecimal> cart = List.of(new BigDecimal("64.99"), new BigDecimal("79.00"), new BigDecimal("129.00"));
        BigDecimal fifty = new BigDecimal("50");

        show("bez promocji", checkout(cart, UnaryOperator.identity()));
        show("-10%", checkout(cart, percentDiscount(10)));
        show("-50 zł (nie mniej niż 0)", checkout(cart, total -> total.subtract(fifty).max(BigDecimal.ZERO)));
        // WYNIK: bez promocji → 272.99
        // WYNIK: -10% → 245.69
        // WYNIK: -50 zł (nie mniej niż 0) → 222.99

        // Strategie można też trzymać w mapie i wybierać po nazwie (np. z kodu promocyjnego wpisanego przez klienta):
        Map<String, UnaryOperator<BigDecimal>> promoCodes = new LinkedHashMap<>();
        promoCodes.put("BRAK", UnaryOperator.identity());
        promoCodes.put("LATO20", percentDiscount(20));
        show("kod LATO20", checkout(cart, promoCodes.get("LATO20")));
        // WYNIK: kod LATO20 → 218.39
        // DOBRA PRAKTYKA: lambda jako strategia jest świetna, gdy strategia to JEDNA operacja bez stanu.
        //   Gdy ma kilka metod, konfigurację i nazwę do wyświetlenia — lepsza klasa albo rekord (Lambda02FunctionalInterfaces).
    }

    // =================================================================================================
    // 5. LENIWOŚĆ: SUPPLIER ODKŁADA KOSZTOWNE OBLICZENIA
    // =================================================================================================

    /** buildReport = zbuduj raport. „Kosztowna” operacja — liczymy, ile razy naprawdę się wykonała. */
    static String buildReport() {
        expensiveCalls++;
        return "raport: " + SampleData.products().size() + " produktów";
    }

    /** debugEager = debug gorliwy. Dostaje GOTOWY napis — policzony przed wywołaniem, nawet gdy nie będzie wypisany. */
    static void debugEager(boolean enabled, String message) {
        if (enabled) {
            System.out.println("   DEBUG: " + message);
        }
    }

    /** debugLazy = debug leniwy. Dostaje PRZEPIS na napis (Supplier) — wykona go tylko, gdy trzeba. */
    static void debugLazy(boolean enabled, Supplier<String> message) {
        if (enabled) {
            System.out.println("   DEBUG: " + message.get());
        }
    }

    /**
     * 5. Argumenty metody są obliczane PRZED jej wywołaniem (gorliwie, eager). Jeśli argument jest drogi, a metoda
     * może go nie użyć — marnujemy pracę. Supplier odwraca kolejność: przekazujesz „przepis”, a metoda sama decyduje,
     * czy go wykonać (leniwie, lazy).
     */
    static void lazyEvaluation() {
        section("5. Leniwość — Supplier liczy tylko wtedy, gdy trzeba");

        expensiveCalls = 0;
        debugEager(false, buildReport());                            // debug wyłączony, a raport i tak zbudowany!
        show("gorliwie, debug wyłączony — wywołań buildReport", expensiveCalls);
        // WYNIK: gorliwie, debug wyłączony — wywołań buildReport → 1

        expensiveCalls = 0;
        debugLazy(false, Lambda07HigherOrderFunctions::buildReport); // przekazujemy PRZEPIS, nie wynik
        show("leniwie, debug wyłączony — wywołań buildReport", expensiveCalls);
        // WYNIK: leniwie, debug wyłączony — wywołań buildReport → 0

        debugLazy(true, Lambda07HigherOrderFunctions::buildReport);  // debug włączony — teraz przepis się wykona
        show("leniwie, debug włączony — wywołań buildReport", expensiveCalls);
        // WYNIK: DEBUG: raport: 14 produktów
        // WYNIK: leniwie, debug włączony — wywołań buildReport → 1

        // Biblioteka Javy używa tego wzorca w wielu miejscach, np. komunikat wyjątku budowany tylko przy błędzie:
        expectThrows("Objects.requireNonNull(null, Supplier)",
                () -> Objects.requireNonNull(null, () -> "Brak klienta — zbudowano komunikat dopiero teraz"));
        // WYNIK: ✔ Objects.requireNonNull(null, Supplier) → rzucono NullPointerException: Brak klienta — zbudowano komunikat dopiero teraz
        // Inne przykłady: Optional.orElseGet(Supplier) (t14_optional), Map.computeIfAbsent, Logger.fine(Supplier) w java.util.logging.
    }

    // =================================================================================================
    // 6. TABLICA POLECEŃ: Map<String, BinaryOperator<Integer>>
    // =================================================================================================

    /** OPERATIONS = działania. Mapa: symbol → funkcja. Dodanie działania = jedna linia, bez zmiany calculate. */
    private static final Map<String, BinaryOperator<Integer>> OPERATIONS = createOperations();

    /** createOperations = utwórz działania. LinkedHashMap — pamięta kolejność dodawania. */
    private static Map<String, BinaryOperator<Integer>> createOperations() {
        Map<String, BinaryOperator<Integer>> ops = new LinkedHashMap<>();
        ops.put("+", Integer::sum);                                  // sum = suma (metoda statyczna Integer)
        ops.put("-", (a, b) -> a - b);
        ops.put("*", (a, b) -> a * b);
        ops.put("/", (a, b) -> a / b);                               // dzielenie całkowite
        ops.put("max", Math::max);
        return ops;
    }

    /** calculate = oblicz wyrażenie w postaci "liczba działanie liczba", np. "7 * 6". */
    static int calculate(String expression) {
        String[] parts = expression.split(" ");                     // ["7", "*", "6"]
        BinaryOperator<Integer> operation = OPERATIONS.get(parts[1]);
        if (operation == null) {
            throw new IllegalArgumentException("Nieznane działanie: " + parts[1]);
        }
        return operation.apply(Integer.parseInt(parts[0]), Integer.parseInt(parts[2]));
    }

    /**
     * 6. Mapa „nazwa → funkcja” zastępuje długi switch albo łańcuch if-else. Każde działanie to osobny wpis;
     * nowe działanie dodajesz jednym put, a kod calculate się nie zmienia (zasada otwarte-zamknięte, t27_clean_code_pitfalls).
     */
    static void commandTable() {
        section("6. Tablica poleceń — mapa symbol → funkcja (mini-kalkulator)");

        show("dostępne działania", OPERATIONS.keySet());
        // WYNIK: dostępne działania → [+, -, *, /, max]
        for (String expression : List.of("7 * 6", "20 - 30", "17 / 5", "3 max 9")) {
            System.out.println("   " + expression + " = " + calculate(expression));
        }
        // WYNIK: 7 * 6 = 42
        // WYNIK: 20 - 30 = -10
        // WYNIK: 17 / 5 = 3    ← dzielenie całkowite obcina część ułamkową
        // WYNIK: 3 max 9 = 9

        expectThrows("calculate(\"2 ^ 3\")", () -> calculate("2 ^ 3"));
        expectThrows("calculate(\"8 / 0\")", () -> calculate("8 / 0"));
        // WYNIK: ✔ calculate("2 ^ 3") → rzucono IllegalArgumentException: Nieznane działanie: ^
        // WYNIK: ✔ calculate("8 / 0") → rzucono ArithmeticException: / by zero
        // Ta sama technika: menu programu konsolowego (numer opcji → Runnable), obsługa poleceń tekstowych,
        // reguły walidacji (nazwa pola → Predicate). Wzorzec Polecenie: t22_design_patterns/Patterns09Command.
    }

    // =================================================================================================
    // 7. MEMOIZACJA — ZAPAMIĘTYWANIE WYNIKÓW
    // =================================================================================================

    /**
     * memoize = zapamiętuj. Przyjmuje funkcję i ZWRACA jej wersję z pamięcią podręczną (cache):
     * dla argumentu, który już był, oddaje zapamiętany wynik zamiast liczyć od nowa.
     * Mapa cache jest przechwycona przez lambdę i prywatna dla niej — nikt inny jej nie widzi.
     */
    static <T, R> Function<T, R> memoize(Function<T, R> slowFunction) {
        Map<T, R> cache = new HashMap<>();
        return argument -> cache.computeIfAbsent(argument, slowFunction);
    }

    /** naiveFib = naiwny Fibonacci: fib(n) = fib(n-1) + fib(n-2). Liczy te same wartości wiele razy. */
    static long naiveFib(int n) {
        naiveFibCalls++;
        return n < 2 ? n : naiveFib(n - 1) + naiveFib(n - 2);
    }

    /**
     * memoFib = Fibonacci z pamięcią. Rekurencja + ręczne get/put (NIE computeIfAbsent — patrz PUŁAPKA w sekcji 7).
     */
    static long memoFib(int n, Map<Integer, Long> memo) {
        if (n < 2) {
            return n;
        }
        Long cached = memo.get(n);                                  // cached = zapamiętany
        if (cached != null) {
            return cached;
        }
        long result = memoFib(n - 1, memo) + memoFib(n - 2, memo);
        memo.put(n, result);
        return result;
    }

    /**
     * 7. Memoizacja: gdy funkcja jest kosztowna i CZYSTA (ten sam argument → zawsze ten sam wynik, bez efektów
     * ubocznych), można zapamiętywać jej wyniki. Funkcja wyższego rzędu memoize dodaje to do DOWOLNEJ funkcji.
     */
    static void memoization() {
        section("7. Memoizacja — memoize(funkcja) i Map.computeIfAbsent");

        AtomicInteger realCalls = new AtomicInteger();              // prywatny licznik tylko do pomiaru
        Function<Integer, Integer> slowSquare = x -> {              // slow square = wolny kwadrat
            realCalls.incrementAndGet();
            return x * x;
        };
        Function<Integer, Integer> fastSquare = memoize(slowSquare);

        List<Integer> results = new ArrayList<>();
        for (int x : List.of(4, 4, 5, 4, 5)) {
            results.add(fastSquare.apply(x));
        }
        show("wyniki / prawdziwe obliczenia", results + " / " + realCalls.get());
        // WYNIK: wyniki / prawdziwe obliczenia → [16, 16, 25, 16, 25] / 2    ← 5 wywołań, ale liczone tylko dla 4 i 5

        // Fibonacci: naiwna rekurencja liczy te same wartości wykładniczo wiele razy.
        naiveFibCalls = 0;
        long fib25 = naiveFib(25);
        show("naiwnie fib(25) / wywołań", fib25 + " / " + naiveFibCalls);
        // WYNIK: naiwnie fib(25) / wywołań → 75025 / 242785

        Map<Integer, Long> memo = new HashMap<>();
        show("z pamięcią fib(50) / zapamiętanych wyników", memoFib(50, memo) + " / " + memo.size());
        // WYNIK: z pamięcią fib(50) / zapamiętanych wyników → 12586269025 / 49
        // Naiwnie fib(50) wymagałby ok. 40 miliardów wywołań (minuty pracy); z pamięcią — 49 obliczeń.

        // PUŁAPKA: rekurencyjne computeIfAbsent na tej samej HashMap (funkcja w środku znowu woła computeIfAbsent,
        //   więc zmienia mapę w trakcie jej zmieniania). Od Javy 9 HashMap to wykrywa i rzuca wyjątek:
        Map<Integer, Long> badMemo = new HashMap<>();
        expectThrows("rekurencyjne computeIfAbsent", () -> recursiveFibWithComputeIfAbsent(30, badMemo));
        // WYNIK: ✔ rekurencyjne computeIfAbsent → rzucono ConcurrentModificationException: (brak komunikatu)
        // (concurrent modification = „równoczesna modyfikacja”.) Rozwiązanie: ręczne get/put jak w memoFib.
        // DOBRA PRAKTYKA: memoizuj tylko funkcje CZYSTE. Pamiętaj, że cache rośnie bez końca — w prawdziwym systemie
        //   potrzebny jest limit rozmiaru albo czas życia wpisów (biblioteki typu Caffeine).
    }

    /** Błędna wersja — tylko do pokazania wyjątku ConcurrentModificationException. */
    static long recursiveFibWithComputeIfAbsent(int n, Map<Integer, Long> memo) {
        if (n < 2) {
            return n;
        }
        return memo.computeIfAbsent(n, k -> recursiveFibWithComputeIfAbsent(k - 1, memo)
                + recursiveFibWithComputeIfAbsent(k - 2, memo));
    }

    // =================================================================================================
    // 8. METODY ZWRACAJĄCE PREDYKATY — MINI-JĘZYK WARUNKÓW
    // =================================================================================================

    /** priceBelow = cena poniżej. Fabryka predykatów: kwotę zamieniamy na BigDecimal RAZ, przy tworzeniu. */
    static Predicate<Product> priceBelow(String amount) {
        BigDecimal limit = new BigDecimal(amount);
        return p -> p.price().compareTo(limit) < 0;
    }

    /** inCategory = w kategorii. */
    static Predicate<Product> inCategory(Category category) {
        return p -> p.category() == category;
    }

    /** nameContains = nazwa zawiera (bez względu na wielkość liter). */
    static Predicate<Product> nameContains(String fragment) {
        String lower = fragment.toLowerCase(Locale.ROOT);          // Locale.ROOT — wynik niezależny od języka systemu
        return p -> p.name().toLowerCase(Locale.ROOT).contains(lower);
    }

    /** productNames = nazwy produktów spełniających warunek. */
    static List<String> productNames(Predicate<Product> condition) {
        List<String> result = new ArrayList<>();
        for (Product p : SampleData.products()) {
            if (condition.test(p)) {
                result.add(p.name());
            }
        }
        return result;
    }

    /**
     * 8. Metody zwracające predykaty dają NAZWANE, sparametryzowane warunki. Połączone przez and/or czytają się
     * jak zdanie — to taki mini-język (DSL = domain-specific language, język dziedzinowy).
     */
    static void returningPredicates() {
        section("8. Metody zwracające predykaty — warunki z nazwą i parametrem");

        show("książki poniżej 100 zł", productNames(inCategory(Category.KSIAZKI).and(priceBelow("100"))));
        show("nazwa zawiera „kaw”", productNames(nameContains("kaw")));
        show("elektronika LUB poniżej 10 zł", productNames(inCategory(Category.ELEKTRONIKA).or(priceBelow("10"))));
        // WYNIK: książki poniżej 100 zł → [Czysty kod, Wzorce projektowe]
        // WYNIK: nazwa zawiera „kaw” → [Kawa ziarnista 1kg, Ekspres do kawy]
        // WYNIK: elektronika LUB poniżej 10 zł → [Laptop Pro 14, Smartfon X, Słuchawki BT, Monitor 27 cali, Czekolada gorzka]

        // Porównaj z lambdą „w miejscu”:  p -> p.category() == Category.KSIAZKI && p.price().compareTo(new BigDecimal("100")) < 0
        // Wersja z fabrykami jest krótsza, czytelniejsza, a każdy warunek można osobno przetestować i użyć ponownie.
    }

    // =================================================================================================
    // 9. OPAKOWANIE „PONÓW PRZY BŁĘDZIE” (RETRY)
    // =================================================================================================

    /**
     * withRetry = z ponawianiem. Wywołuje action; gdy rzuci RuntimeException — próbuje ponownie, najwyżej maxAttempts razy.
     * Po ostatniej nieudanej próbie rzuca ostatni wyjątek dalej.
     */
    static <T> T withRetry(int maxAttempts, Supplier<T> action) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                System.out.println("   próba " + attempt + ": " + e.getMessage());
                lastError = e;
            }
        }
        throw lastError;
    }

    /**
     * 9. Funkcja wyższego rzędu może „opakować” dowolną operację dodatkowym zachowaniem: ponawianiem, pomiarem czasu,
     * logowaniem, obsługą wyjątków. Operację przekazujemy jako Supplier.
     */
    static void retryWrapper() {
        section("9. Opakowanie operacji: withRetry(próby, Supplier)");

        AtomicInteger attempts = new AtomicInteger();               // licznik prób — symuluje niestabilny serwer
        Supplier<String> flakyServer = () -> {                      // flaky = zawodny, niestabilny
            if (attempts.incrementAndGet() < 3) {
                throw new IllegalStateException("Serwer nie odpowiada (próba " + attempts.get() + ")");
            }
            return "dane z serwera";
        };

        show("wynik po ponowieniach", withRetry(5, flakyServer));
        // WYNIK: próba 1: Serwer nie odpowiada (próba 1)
        // WYNIK: próba 2: Serwer nie odpowiada (próba 2)
        // WYNIK: wynik po ponowieniach → dane z serwera

        attempts.set(0);                                            // set = ustaw — od nowa
        expectThrows("withRetry(2, ...) — za mało prób", () -> withRetry(2, flakyServer));
        // WYNIK: próba 1: Serwer nie odpowiada (próba 1)
        // WYNIK: próba 2: Serwer nie odpowiada (próba 2)
        // WYNIK: ✔ withRetry(2, ...) — za mało prób → rzucono IllegalStateException: Serwer nie odpowiada (próba 2)
        // DOBRA PRAKTYKA: w prawdziwym kodzie między próbami czeka się coraz dłużej (backoff) i ponawia tylko błędy
        //   przejściowe (np. brak połączenia), a nie np. błędne dane — tych ponowienie nie naprawi.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Funkcja wyższego rzędu = przyjmuje funkcję (sort, removeIf, applyTwice, checkout) lub zwraca funkcję (fabryka).
     *   • Fabryka funkcji:  static IntUnaryOperator multiplier(int n) { return x -> x * n; }  — parametr „zamrożony” w lambdzie.
     *     Walidację i stałe (np. BigDecimal) przygotuj w fabryce RAZ, poza zwracaną lambdą.
     *   • Currying:  Function<A, Function<B, R>> f = a -> b -> ...;   f.apply(a).apply(b);   f.apply(a) = częściowe zastosowanie.
     *   • Strategia jako lambda: checkout(koszyk, UnaryOperator<BigDecimal> promocja); strategie w mapie po nazwie.
     *   • Leniwość: Supplier<T> zamiast T, gdy wartość jest droga i może nie być potrzebna (logi, komunikaty, orElseGet).
     *   • Tablica poleceń: Map<String, BinaryOperator<Integer>> zamiast switcha; nieznany klucz → get zwraca null → wyjątek.
     *   • Memoizacja: memoize(f) = f z cache (computeIfAbsent). Tylko dla funkcji czystych. Rekurencyjne computeIfAbsent
     *     na HashMap → ConcurrentModificationException (Java 9+) — użyj get/put.
     *   • Metody zwracające Predicate = nazwane warunki z parametrem: inCategory(KSIAZKI).and(priceBelow("100")).
     *   • Opakowania (retry, logowanie, pomiar czasu) = funkcja wyższego rzędu przyjmująca Supplier/Runnable.
     *
     * PYTANIA KONTROLNE:
     *   1. Co to jest funkcja wyższego rzędu? Podaj po jednym przykładzie z biblioteki Javy dla „przyjmuje” i „zwraca”.
     *   2. Co wypisze ten kod?
     *          Function<Integer, Function<Integer, Integer>> f = a -> b -> a * 10 + b;
     *          Function<Integer, Integer> g = f.apply(4);
     *          System.out.println(g.apply(2) + " " + f.apply(1).apply(9));
     *   3. Ile razy wykona się buildReport() w każdym wierszu, gdy enabled = false?
     *          debugEager(false, buildReport());
     *          debugLazy(false, () -> buildReport());
     *   4. ZNAJDŹ BŁĄD (fabryka rabatów jest wolna i tworzy mnóstwo obiektów):
     *          static UnaryOperator<BigDecimal> discount(int percent) {
     *              return price -> price.multiply(new BigDecimal(100 - percent).divide(new BigDecimal("100")));
     *          }
     *   5. ZNAJDŹ BŁĄD:
     *          static long fib(int n) {
     *              return n < 2 ? n : memo.computeIfAbsent(n, k -> fib(k - 1) + fib(k - 2));   // memo to HashMap
     *          }
     *   6. Kiedy memoizacja jest bezpieczna, a kiedy da złe wyniki?
     *   7. Jakie są zalety mapy „nazwa → funkcja” w porównaniu ze switchem?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        List<String> expected2 = List.of("Anna Nowak", "Michał Lewandowski", "Ewa Woźniak");

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: fabryka funkcji liniowej 2x + 3 dla x = 10", 23, () -> exercise1(2, 3).applyAsInt(10));
        Check.equal("ćw. 2: predykat „dział + minimalna pensja”", expected2,
                () -> employeeNames(exercise2(Department.IT, 12_000)));
        Check.equal("ćw. 3: currying — etykieta cenowa", "5.00 zł | 12.50 EUR", () -> {
            Function<BigDecimal, String> inZloty = exercise3().apply("zł");
            return inZloty.apply(new BigDecimal("5")) + " | " + exercise3().apply("EUR").apply(new BigDecimal("12.5"));
        });
        Check.equal("ćw. 4: leniwa wartość zapasowa", "jest, zapas, wywołań zapasu: 1", () -> {
            AtomicInteger calls = new AtomicInteger();
            Supplier<String> fallback = () -> {
                calls.incrementAndGet();
                return "zapas";
            };
            return exercise4("jest", fallback) + ", " + exercise4(null, fallback) + ", wywołań zapasu: " + calls.get();
        });
        Check.equal("ćw. 5: memoize", "2 obliczenia, wyniki [16, 16, 25, 16]", () -> {
            AtomicInteger calls = new AtomicInteger();
            Function<Integer, Integer> fast = exercise5(x -> {
                calls.incrementAndGet();
                return x * x;
            });
            List<Integer> results = List.of(fast.apply(4), fast.apply(4), fast.apply(5), fast.apply(4));
            return calls.get() + " obliczenia, wyniki " + results;
        });
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 23, () -> solution1(2, 3).applyAsInt(10));
        Check.equal("ćw. 2 (wzorzec)", expected2, () -> employeeNames(solution2(Department.IT, 12_000)));
        Check.equal("ćw. 3 (wzorzec)", "5.00 zł | 12.50 EUR", () -> {
            Function<BigDecimal, String> inZloty = solution3().apply("zł");
            return inZloty.apply(new BigDecimal("5")) + " | " + solution3().apply("EUR").apply(new BigDecimal("12.5"));
        });
        Check.equal("ćw. 4 (wzorzec)", "jest, zapas, wywołań zapasu: 1", () -> {
            AtomicInteger calls = new AtomicInteger();
            Supplier<String> fallback = () -> {
                calls.incrementAndGet();
                return "zapas";
            };
            return solution4("jest", fallback) + ", " + solution4(null, fallback) + ", wywołań zapasu: " + calls.get();
        });
        Check.equal("ćw. 5 (wzorzec)", "2 obliczenia, wyniki [16, 16, 25, 16]", () -> {
            AtomicInteger calls = new AtomicInteger();
            Function<Integer, Integer> fast = solution5(x -> {
                calls.incrementAndGet();
                return x * x;
            });
            List<Integer> results = List.of(fast.apply(4), fast.apply(4), fast.apply(5), fast.apply(4));
            return calls.get() + " obliczenia, wyniki " + results;
        });
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /** employeeNames = imiona i nazwiska pracowników spełniających warunek (pomocnicza do ćwiczenia 2). */
    static List<String> employeeNames(Predicate<Employee> condition) {
        List<String> result = new ArrayList<>();
        for (Employee e : SampleData.employees()) {
            if (condition.test(e)) {
                result.add(e.name());
            }
        }
        return result;
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć funkcję liniową x → a·x + b. Dla a = 2, b = 3 i x = 10 wynik to 23.
     * Podpowiedź: jak multiplier z sekcji 2 — lambda użyje parametrów a i b.
     */
    static IntUnaryOperator exercise1(int a, int b) {
        // TODO: twoje rozwiązanie
        return x -> 0;
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć predykat „pracownik z działu department, zarabiający CO NAJMNIEJ minSalary”.
     * Dla IT i 12 000 → Anna Nowak, Michał Lewandowski, Ewa Woźniak.
     * Podpowiedź: jak fabryki predykatów z sekcji 8 (inCategory, priceBelow). Enumy porównuj przez ==.
     */
    static Predicate<Employee> exercise2(Department department, int minSalary) {
        // TODO: twoje rozwiązanie
        return e -> false;
    }

    /**
     * ĆWICZENIE 3 (średnie): currying. Zwróć funkcję, która najpierw przyjmuje walutę (np. "zł"), a potem kwotę,
     * i zwraca etykietę z kwotą zaokrągloną do 2 miejsc: "zł", 5 → "5.00 zł"; "EUR", 12.5 → "12.50 EUR".
     * Podpowiedź: {@code currency -> amount -> ...}; {@code amount.setScale(2, RoundingMode.HALF_UP) + " " + currency}.
     */
    static Function<String, Function<BigDecimal, String>> exercise3() {
        // TODO: twoje rozwiązanie
        return currency -> amount -> "";
    }

    /**
     * ĆWICZENIE 4 (średnie): leniwa wartość zapasowa. Zwróć value, jeśli nie jest null; w przeciwnym razie wynik
     * fallback.get(). Zapas NIE może być liczony, gdy value istnieje (sprawdzenie liczy wywołania!).
     * Podpowiedź: tak działa Optional.orElseGet (t14_optional). Wywołaj get() tylko w gałęzi „value == null”.
     */
    static <T> T exercise4(T value, Supplier<T> fallback) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): napisz memoize — zwróć funkcję, która dla każdego argumentu woła slowFunction
     * TYLKO RAZ, a przy kolejnych wywołaniach z tym samym argumentem oddaje zapamiętany wynik.
     * Podpowiedź: mapa utworzona WEWNĄTRZ metody (każda zwrócona funkcja ma własną pamięć) i
     * {@code cache.computeIfAbsent(argument, slowFunction)} w zwracanej lambdzie. Nie używaj pola static!
     */
    static <T, R> Function<T, R> exercise5(Function<T, R> slowFunction) {
        // TODO: twoje rozwiązanie
        return slowFunction;                  // na razie bez pamięci — liczy za każdym razem
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static IntUnaryOperator solution1(int a, int b) {
        return x -> a * x + b;
    }

    static Predicate<Employee> solution2(Department department, int minSalary) {
        return e -> e.department() == department && e.salary() >= minSalary;
    }

    static Function<String, Function<BigDecimal, String>> solution3() {
        return currency -> amount -> amount.setScale(2, RoundingMode.HALF_UP) + " " + currency;
    }

    static <T> T solution4(T value, Supplier<T> fallback) {
        if (value != null) {
            return value;                     // zapas w ogóle nie jest liczony
        }
        return fallback.get();
    }

    static <T, R> Function<T, R> solution5(Function<T, R> slowFunction) {
        Map<T, R> cache = new HashMap<>();    // prywatna pamięć tej jednej zwróconej funkcji
        return argument -> cache.computeIfAbsent(argument, slowFunction);
        // Uwaga: computeIfAbsent nie zapamięta wyniku null (dla null policzy od nowa za każdym razem).
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Funkcja (metoda), która przyjmuje funkcję jako parametr albo zwraca funkcję. Przyjmuje: list.sort(Comparator),
     *      removeIf(Predicate), computeIfAbsent(klucz, Function). Zwraca: Comparator.comparing(...), Predicate.not(...),
     *      Function.identity(), f.andThen(g).
     *   2. „42 19” — g = b -> 4 * 10 + b, więc g.apply(2) = 42; f.apply(1).apply(9) = 10 + 9 = 19.
     *   3. Pierwszy wiersz: 1 raz (argument liczony przed wywołaniem metody — gorliwie). Drugi: 0 razy (Supplier nigdy
     *      nie zostanie wywołany, bo enabled = false).
     *   4. BigDecimal (100 - percent) i "100" są tworzone i dzielone przy KAŻDYM wywołaniu zwróconej lambdy. Współczynnik
     *      trzeba policzyć RAZ w fabryce, przed return (jak percentDiscount w sekcji 2). Warto też sprawdzić zakres percent.
     *   5. Rekurencyjne computeIfAbsent na tej samej HashMap — funkcja przekazana do computeIfAbsent sama zmienia mapę
     *      → ConcurrentModificationException (Java 9+). Rozwiązanie: ręczne get / put (memoFib w sekcji 7).
     *   6. Bezpieczna dla funkcji CZYSTYCH (wynik zależy tylko od argumentu, brak efektów ubocznych). Złe wyniki, gdy
     *      funkcja zależy od czasu, bazy danych, pól, które się zmieniają — cache zwróci nieaktualny wynik.
     *   7. Dodanie działania = jeden wpis w mapie, bez zmiany kodu, który z niej korzysta; listę dostępnych działań
     *      można wypisać (keySet); działania można wczytać/zbudować dynamicznie; każde działanie to osobny, testowalny klocek.
     */
    // </editor-fold>
}
