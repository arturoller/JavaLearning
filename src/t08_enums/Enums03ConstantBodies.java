package t08_enums;

import helpers.Check;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.IntBinaryOperator;
import java.util.stream.Collectors;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Własne zachowanie każdej stałej, enum z lambdą, enum implementujący interfejs, enum jako singleton
 *        (constant body = ciało stałej; abstract method = metoda abstrakcyjna; interface = interfejs; singleton = jedynak)
 *
 * W SKRÓCIE:
 *   Stałe enuma mogą się różnić nie tylko DANYMI (Enums02), ale też ZACHOWANIEM. Są trzy sposoby:
 *   (1) metoda abstrakcyjna w enumie + osobne ciało { ... } przy każdej stałej,
 *   (2) pole z lambdą przekazaną w konstruktorze (krócej, Java 8+),
 *   (3) enum implementuje interfejs — wtedy stałe enuma i zwykłe klasy/lambdy są wymienne.
 *   Zamiast switch(this) w każdej metodzie — kompilator wymusza implementację przy KAŻDEJ nowej stałej.
 *
 * ANALOGIA: przyciski kalkulatora.
 *   Każdy przycisk (+, -, ×, ÷) ma inną nalepkę (dane) i robi co innego (zachowanie). Nie ma jednego „centralnego
 *   pokrętła” sprawdzającego, który przycisk naciśnięto — każdy przycisk sam wie, co ma zrobić.
 *
 * JAK TO DZIAŁA:
 *   enum Operation {
 *       PLUS("+")  { int apply(int a, int b) { return a + b; } },   ← ciało stałej = anonimowa podklasa enuma
 *       MINUS("-") { int apply(int a, int b) { return a - b; } };
 *       abstract int apply(int a, int b);                          ← każda stała MUSI ją zaimplementować
 *   }
 *   Operation.PLUS.apply(2, 3) → 5
 *
 * SŁÓWKA:
 *   constant body = ciało stałej; abstract = abstrakcyjny; apply = zastosuj; operation = działanie; symbol = znak;
 *   operator = operator (funkcja dwóch argumentów); rule = reguła; discount = zniżka; instance = egzemplarz;
 *   declaring class = klasa deklarująca; anonymous class = klasa anonimowa; strategy = strategia.
 *
 * ZOBACZ TEŻ: t08_enums/Enums02FieldsMethods (pola i metody), t13_lambdas/Lambda03JavaUtilFunction (IntBinaryOperator),
 *             t07_inheritance_polymorphism/Inherit04Interfaces (interfejsy), t22_design_patterns/Patterns01Strategy (strategia),
 *             t22_design_patterns/Patterns04Singleton (singleton).
 * </pre>
 */
public class Enums03ConstantBodies {

    // ---------------------------------------------------------------------------------------------
    // Sposób 1: metoda abstrakcyjna + ciało przy każdej stałej
    // ---------------------------------------------------------------------------------------------

    /** Operation = działanie arytmetyczne. Każda stała ma własną implementację apply. */
    enum Operation {
        PLUS("+") {
            @Override
            int apply(int a, int b) {
                return a + b;
            }
        },
        MINUS("-") {
            @Override
            int apply(int a, int b) {
                return a - b;
            }
        },
        TIMES("*") {
            @Override
            int apply(int a, int b) {
                return a * b;
            }
        },
        DIVIDE("/") {
            @Override
            int apply(int a, int b) {
                return a / b;           // dzielenie całkowite; b == 0 → ArithmeticException
            }
        };

        private final String symbol;    // symbol = znak działania

        Operation(String symbol) {
            this.symbol = symbol;
        }

        String getSymbol() {
            return symbol;
        }

        /** apply = zastosuj. abstract = każda stała MUSI dostarczyć własną wersję (inaczej błąd kompilacji). */
        abstract int apply(int a, int b);
    }

    // ---------------------------------------------------------------------------------------------
    // Sposób 1 — PRZED: jedna metoda ze switch(this)
    // ---------------------------------------------------------------------------------------------

    /** OperationWithSwitch = działanie ze switchem. Działa, ale logika każdej stałej jest daleko od jej deklaracji. */
    enum OperationWithSwitch {
        PLUS, MINUS;

        int apply(int a, int b) {
            return switch (this) {
                case PLUS -> a + b;
                case MINUS -> a - b;
            };
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Sposób 2: pole z lambdą (Java 8+)
    // ---------------------------------------------------------------------------------------------

    /**
     * CompactOperation = zwięzłe działanie. Zachowanie przekazane jako lambda do konstruktora.
     * IntBinaryOperator = operator na dwóch intach: (a, b) → int (t13_lambdas/Lambda03JavaUtilFunction).
     */
    enum CompactOperation {
        PLUS("+", (a, b) -> a + b),
        MINUS("-", (a, b) -> a - b),
        MAX("max", Math::max);                   // referencja do metody też pasuje

        private final String symbol;
        private final IntBinaryOperator function; // function = funkcja (zachowanie tej stałej)

        CompactOperation(String symbol, IntBinaryOperator function) {
            this.symbol = symbol;
            this.function = function;
        }

        int apply(int a, int b) {
            return function.applyAsInt(a, b);     // applyAsInt = zastosuj i zwróć int
        }

        String getSymbol() {
            return symbol;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Sposób 3: enum implementujący interfejs
    // ---------------------------------------------------------------------------------------------

    /** PriceRule = reguła cenowa. Interfejs funkcyjny: z ceny w groszach robi nową cenę w groszach. */
    interface PriceRule {
        int apply(int priceGrosze);
    }

    /** StandardDiscount = standardowa zniżka. Gotowe reguły jako stałe enuma — wszystkie są też PriceRule. */
    enum StandardDiscount implements PriceRule {
        NONE(0), STUDENT(20), SENIOR(30);

        private final int percent;    // percent = procent zniżki

        StandardDiscount(int percent) {
            this.percent = percent;
        }

        @Override
        public int apply(int priceGrosze) {           // metoda interfejsu jest public — tu też musi być public
            return priceGrosze * (100 - percent) / 100;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Enum jako singleton
    // ---------------------------------------------------------------------------------------------

    /**
     * IdGenerator = generator identyfikatorów. Enum z jedną stałą INSTANCE to najprostszy, bezpieczny singleton
     * (jedyny egzemplarz w programie): Java gwarantuje dokładnie jeden obiekt, także przy serializacji i refleksji.
     * Uwaga: to wyjątek od zasady „pola enuma final” — singleton z definicji trzyma stan. Wersja wielowątkowa
     * wymagałaby AtomicInteger (t21_concurrency/Concurrency02RaceConditions).
     */
    enum IdGenerator {
        INSTANCE;

        private int next = 1;

        int nextId() {
            return next++;
        }
    }

    public static void main(String[] args) {
        title("Enums03 — zachowanie stałych, lambdy, interfejsy, singleton");

        constantBodies();          // constant bodies = ciała stałych
        beforeAfter();             // before/after = przed/po
        lambdaField();             // lambda field = pole z lambdą
        enumImplementsInterface(); // enum implements interface = enum implementuje interfejs
        getClassPitfall();         // getClass pitfall = pułapka getClass
        enumSingleton();           // enum singleton = enum jako singleton
        exercises();               // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. CIAŁA STAŁYCH
    // =================================================================================================

    /** 1. Każda stała liczy po swojemu. Pętla po values() nie musi wiedzieć, które działanie wywołuje — polimorfizm. */
    static void constantBodies() {
        section("1. Metoda abstrakcyjna + ciało każdej stałej");

        for (Operation op : Operation.values()) {
            System.out.println("12 " + op.getSymbol() + " 4 = " + op.apply(12, 4));
        }
        // WYNIK: 12 + 4 = 16
        // WYNIK: 12 - 4 = 8
        // WYNIK: 12 * 4 = 48
        // WYNIK: 12 / 4 = 3

        expectThrows("DIVIDE przez 0", () -> Operation.DIVIDE.apply(1, 0));
        // WYNIK: ✔ DIVIDE przez 0 → rzucono ArithmeticException: / by zero
    }

    // =================================================================================================
    // 2. PRZED / PO
    // =================================================================================================

    /**
     * 2. PRZED: switch(this) w metodzie. PO: ciało przy stałej. Oba dają ten sam wynik — różnica w utrzymaniu kodu:
     * dopisując nową stałą w wersji PO, kompilator od razu każe napisać jej apply (metoda abstrakcyjna).
     */
    static void beforeAfter() {
        section("2. PRZED/PO: switch(this) kontra ciało stałej");

        show("PRZED: MINUS 10, 3", OperationWithSwitch.MINUS.apply(10, 3));
        show("PO:    MINUS 10, 3", Operation.MINUS.apply(10, 3));
        // WYNIK: PRZED: MINUS 10, 3 → 7
        // WYNIK: PO:    MINUS 10, 3 → 7

        // DOBRA PRAKTYKA: gdy zachowanie zależy WYŁĄCZNIE od stałej — trzymaj je przy stałej (ciało albo lambda).
        //   Wyrażenie switch bez default (jak w PRZED) też jest bezpieczne — kompilator zgłosi brak nowej stałej.
        // PUŁAPKA: stary switch (instrukcja z case ...: i default) NIE przypomni o nowej stałej — po cichu wpadnie w default.
    }

    // =================================================================================================
    // 3. POLE Z LAMBDĄ
    // =================================================================================================

    /** 3. Zamiast ciała { ... } przy każdej stałej — lambda w konstruktorze. Krócej, gdy zachowanie to jedno wyrażenie. */
    static void lambdaField() {
        section("3. Pole z lambdą (Java 8+)");

        for (CompactOperation op : CompactOperation.values()) {
            System.out.println(op.getSymbol() + "(7, 9) = " + op.apply(7, 9));
        }
        // WYNIK: +(7, 9) = 16
        // WYNIK: -(7, 9) = -2
        // WYNIK: max(7, 9) = 9

        // DOBRA PRAKTYKA: lambda — gdy zachowanie jest krótkie. Ciało stałej — gdy jest dłuższe lub ma kilka metod.
    }

    // =================================================================================================
    // 4. ENUM IMPLEMENTUJĄCY INTERFEJS
    // =================================================================================================

    /** applyAll = zastosuj wszystkie reguły po kolei. Nie wie i nie musi wiedzieć, czy reguła to stała enuma, czy lambda. */
    static int applyAll(int priceGrosze, List<PriceRule> rules) {
        int result = priceGrosze;
        for (PriceRule rule : rules) {
            result = rule.apply(result);
        }
        return result;
    }

    /**
     * 4. Enum zamknięty na nowe stałe (nie da się dopisać stałej spoza pliku), ale interfejs jest OTWARTY:
     * obok gotowych stałych można podać własną regułę jako lambdę lub klasę.
     */
    static void enumImplementsInterface() {
        section("4. Enum implementujący interfejs");

        show("STUDENT z 100,00 zł", StandardDiscount.STUDENT.apply(10_000));
        // WYNIK: STUDENT z 100,00 zł → 8000

        PriceRule minus5zl = price -> Math.max(0, price - 500);   // własna reguła: 5 zł mniej, ale nie poniżej zera
        List<PriceRule> rules = List.of(StandardDiscount.SENIOR, minus5zl);
        show("SENIOR, potem -5 zł z 100,00 zł", applyAll(10_000, rules));
        // WYNIK: SENIOR, potem -5 zł z 100,00 zł → 6500

        // DOBRA PRAKTYKA: metody przyjmujące regułę niech przyjmują INTERFEJS (PriceRule), nie enum (StandardDiscount) —
        //   wtedy działają i z gotowymi stałymi, i z regułami dopisanymi później (zasada otwarte-zamknięte, t07).
    }

    // =================================================================================================
    // 5. PUŁAPKA: getClass() stałej z ciałem
    // =================================================================================================

    /**
     * 5. Stała z własnym ciałem { ... } to ANONIMOWA PODKLASA enuma. getClass() zwraca tę podklasę, nie typ enuma.
     * getDeclaringClass() (klasa deklarująca) zawsze zwraca właściwy typ enuma.
     */
    static void getClassPitfall() {
        section("5. Pułapka: getClass() kontra getDeclaringClass()");

        show("PLUS.getClass() == Operation.class", Operation.PLUS.getClass() == Operation.class);
        show("PLUS.getDeclaringClass() == Operation.class", Operation.PLUS.getDeclaringClass() == Operation.class);
        show("PLUS instanceof Operation", Operation.PLUS instanceof Operation);
        // WYNIK: PLUS.getClass() == Operation.class → false    ← to klasa anonimowa (np. Operation$1)
        // WYNIK: PLUS.getDeclaringClass() == Operation.class → true
        // WYNIK: PLUS instanceof Operation → true

        show("stała BEZ ciała: MAX.getClass() == CompactOperation.class",
                CompactOperation.MAX.getClass() == CompactOperation.class);
        // WYNIK: stała BEZ ciała: MAX.getClass() == CompactOperation.class → true

        // DOBRA PRAKTYKA: typ enuma ze stałej odczytuj przez getDeclaringClass() — działa dla stałych z ciałem i bez.
    }

    // =================================================================================================
    // 6. ENUM JAKO SINGLETON
    // =================================================================================================

    /** 6. INSTANCE istnieje raz — każde użycie to ten sam obiekt i ten sam licznik. */
    static void enumSingleton() {
        section("6. Enum jako singleton");

        IdGenerator a = IdGenerator.INSTANCE;
        IdGenerator b = IdGenerator.INSTANCE;
        show("ten sam obiekt?", a == b);
        show("kolejne id", a.nextId() + ", " + b.nextId() + ", " + a.nextId());
        // WYNIK: ten sam obiekt? → true
        // WYNIK: kolejne id → 1, 2, 3    ← a i b to ten sam obiekt, więc licznik jest wspólny

        // PUŁAPKA: singleton to stan globalny — trudniej go testować (licznik „pamięta” poprzednie testy).
        //   Używaj oszczędnie; częściej lepiej przekazać obiekt w konstruktorze (t22_design_patterns/Patterns08DependencyInjection).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • abstract metoda w enumie + ciało { ... } przy każdej stałej → każda stała zachowuje się inaczej;
     *     nowa stała bez implementacji = błąd kompilacji.
     *   • Krótsza wersja: pole z lambdą przekazaną w konstruktorze (IntBinaryOperator, Function, Predicate...).
     *   • enum X implements Interfejs — stałe są wymienne z lambdami/klasami; metody przyjmują interfejs.
     *   • Stała z ciałem to anonimowa podklasa: getClass() ≠ typ enuma; używaj getDeclaringClass().
     *   • enum { INSTANCE; } — najprostszy singleton (jeden egzemplarz gwarantowany przez Javę).
     *   • Stary switch z default nie przypomni o nowej stałej; wyrażenie switch bez default — przypomni.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się enum ze switch(this) w metodzie od enuma z metodą abstrakcyjną i ciałami stałych?
     *   2. Co wypisze:  System.out.println(Operation.TIMES.apply(Operation.PLUS.apply(1, 2), 5));  ?
     *   3. ZNAJDŹ BŁĄD:
     *          enum Shape { CIRCLE { double area(double r) { return Math.PI * r * r; } }, SQUARE;
     *                       abstract double area(double x); }
     *   4. Dlaczego metoda applyAll przyjmuje {@code List<PriceRule>}, a nie {@code List<StandardDiscount>}?
     *   5. Co wypisze:  System.out.println(Operation.PLUS.getClass() == Operation.MINUS.getClass());  ?
     *   6. Dlaczego enum z jedną stałą INSTANCE jest dobrym singletonem?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        List<Operation> ops = List.of(Operation.PLUS, Operation.TIMES, Operation.MINUS);
        List<Integer> values = List.of(5, 3, 4);
        Check.equal("ćw. 1: ((10 + 5) * 3) - 4", 41, () -> exercise1(10, ops, values));
        Check.equal("ćw. 2a: działanie o znaku *", Optional.of(Operation.TIMES), () -> exercise2("*"));
        Check.equal("ćw. 2b: działanie o znaku %", Optional.empty(), () -> exercise2("%"));
        Check.equal("ćw. 3: STUDENT, potem -5 zł z 100,00 zł", 7500, () -> exercise3(10_000));
        Check.equal("ćw. 4: tabelka działań dla 12 i 4", "+=16, -=8, *=48, /=3", () -> exercise4(12, 4));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 41, () -> solution1(10, ops, values));
        Check.equal("ćw. 2a (wzorzec)", Optional.of(Operation.TIMES), () -> solution2("*"));
        Check.equal("ćw. 2b (wzorzec)", Optional.empty(), () -> solution2("%"));
        Check.equal("ćw. 3 (wzorzec)", 7500, () -> solution3(10_000));
        Check.equal("ćw. 4 (wzorzec)", "+=16, -=8, *=48, /=3", () -> solution4(12, 4));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zacznij od start i po kolei stosuj ops.get(i) z values.get(i).
     * Przykład: start 10, [PLUS, TIMES, MINUS], [5, 3, 4] → ((10 + 5) * 3) - 4 = 41.
     * Podpowiedź: pętla for (int i = 0; ...) i result = ops.get(i).apply(result, values.get(i)).
     */
    static int exercise1(int start, List<Operation> ops, List<Integer> values) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /** ĆWICZENIE 2 (średnie): znajdź działanie po znaku (getSymbol); gdy brak — Optional.empty(). */
    static Optional<Operation> exercise2(String symbol) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 3 (średnie): zastosuj do ceny najpierw zniżkę STUDENT, potem WŁASNĄ regułę-lambdę „minus 5 zł”
     * (500 groszy). Użyj applyAll i List.of(...). Wynik dla 10 000 groszy: 8000 - 500 = 7500.
     */
    static int exercise3(int priceGrosze) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze, łączy z t16_streams): dla każdego działania zbuduj tekst "znak=wynik" dla a i b,
     * połącz przez ", " w kolejności deklaracji. Dla 12 i 4: "+=16, -=8, *=48, /=3".
     * Podpowiedź: Arrays.stream(Operation.values()).map(op -> ...).collect(Collectors.joining(", "))
     */
    static String exercise4(int a, int b) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int start, List<Operation> ops, List<Integer> values) {
        int result = start;
        for (int i = 0; i < ops.size(); i++) {
            result = ops.get(i).apply(result, values.get(i));
        }
        return result;
    }

    static Optional<Operation> solution2(String symbol) {
        return Arrays.stream(Operation.values())
                .filter(op -> op.getSymbol().equals(symbol))
                .findFirst();
    }

    static int solution3(int priceGrosze) {
        PriceRule minus5zl = price -> Math.max(0, price - 500);
        return applyAll(priceGrosze, List.of(StandardDiscount.STUDENT, minus5zl));
    }

    static String solution4(int a, int b) {
        return Arrays.stream(Operation.values())
                .map(op -> op.getSymbol() + "=" + op.apply(a, b))
                .collect(Collectors.joining(", "));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Wynik ten sam. Ze switch(this) logika wszystkich stałych jest w jednej metodzie, daleko od deklaracji stałych.
     *      Z metodą abstrakcyjną każda stała ma logikę przy sobie, a nowa stała bez implementacji to błąd kompilacji.
     *   2. „15” — PLUS.apply(1, 2) = 3, potem TIMES.apply(3, 5) = 15.
     *   3. SQUARE nie ma ciała, a metoda area jest abstrakcyjna — błąd kompilacji. Trzeba dopisać
     *      SQUARE { double area(double a) { return a * a; } } (albo zrobić area nieabstrakcyjną z domyślną wersją).
     *   4. Żeby przyjmowała także reguły spoza enuma (lambdy, inne klasy). Enum jest zamknięty, interfejs — otwarty.
     *   5. „false” — każda stała z ciałem to OSOBNA klasa anonimowa (Operation$1, Operation$2...).
     *   6. Java gwarantuje, że stała enuma istnieje dokładnie raz — nie da się jej utworzyć przez new, refleksję
     *      ani przez deserializację. Do tego jest leniwie i bezpiecznie inicjalizowana przy ładowaniu klasy.
     */
    // </editor-fold>
}
