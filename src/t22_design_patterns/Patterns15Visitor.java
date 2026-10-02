package t22_design_patterns;

import helpers.Check;
import helpers.TempDir;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wzorzec Visitor — nowe operacje na stałej hierarchii klas
 *        (visitor = wizytator, "odwiedzający"; accept = przyjmij; visit = odwiedź; double dispatch = podwójna dyspozycja)
 *
 * W SKRÓCIE:
 *   Masz stałą hierarchię klas (węzły drzewa wyrażenia: liczba, zmienna, dodawanie, mnożenie) i chcesz do niej
 *   dopisywać KOLEJNE OPERACJE (policz, wypisz, uprość, wyeksportuj) bez dotykania klas węzłów. Każdą operację
 *   zapisujesz w jednym miejscu — w klasie wizytatora — a węzeł tylko "wpuszcza" wizytatora do siebie.
 *
 * ANALOGIA: inspektor odwiedzający mieszkania w bloku. Mieszkania (węzły) się nie zmieniają, a inspektor może
 *   być różny: elektryk, kominiarz, administrator. Każdy wchodzi do KAŻDEGO mieszkania i robi swoje, a mieszkanie
 *   tylko mówi "proszę wejść" (accept). Nowy rodzaj inspektora to nowa osoba, a nie remont wszystkich mieszkań.
 *
 * JAK TO DZIAŁA:
 *   Role z książki GoF (Gang of Four):
 *     Element (element)             — interfejs węzła z metodą accept(visitor)
 *     ConcreteElement               — konkretny węzeł (Num, Var, Add, Mul); accept woła visitor.visitXxx(this)
 *     Visitor (wizytator)           — interfejs z JEDNĄ metodą visit na każdy rodzaj węzła
 *     ConcreteVisitor               — jedna operacja (Eval, Print, Count...) zapisana w jednej klasie
 *
 *   Podwójna dyspozycja (double dispatch) — dwa wybory w czasie działania programu:
 *
 *     expr.accept(visitor)            1. wybór metody accept — po TYPIE WĘZŁA (zwykły polimorfizm)
 *        └▶ Add.accept(v)               2. w środku wiemy, że this jest typu Add, więc wołamy v.visitAdd(this)
 *              └▶ v.visitAdd(add)          3. wybór implementacji visitAdd — po TYPIE WIZYTATORA (polimorfizm)
 *
 *   Wyrażenie przykładowe w lekcji:  1 + 2 * x   czyli   Add(Num 1, Mul(Num 2, Var x)).
 *
 *   PROBLEM WYRAŻENIA (expression problem): wizytator ułatwia dodawanie OPERACJI, a utrudnia dodawanie TYPÓW
 *   węzłów (trzeba zmienić interfejs wizytatora i wszystkich wizytatorów). Zwykłe metody w klasach — odwrotnie.
 *
 * SŁÓWKA: visitor = wizytator; accept = przyjmij; visit = odwiedź; node = węzeł; expression = wyrażenie; evaluate = obliczyć;
 *   dispatch = wybór metody do wywołania; overload = przeciążenie; traversal = przechodzenie po drzewie;
 *   accumulate = gromadzić; simplify = uprościć; tree = drzewo
 *
 * ZOBACZ TEŻ: t22_design_patterns/Patterns12Composite (drzewo, po którym chodzi wizytator),
 *   t18_io_files/Io07WalkingDirectories (FileVisitor — wizytator z JDK),
 *   t19_annotations_reflection/Annotations07Processors (ElementVisitor w procesorach adnotacji),
 *   t22_design_patterns/Patterns13State (stany jako rekordy sealed z danymi)
 * </pre>
 */
public class Patterns15Visitor {

    public static void main(String[] args) {
        title("Patterns15 — Visitor: wizytator i podsumowanie wzorców");

        problemOperationsEverywhere();   // problem operations everywhere = problem: operacje wszędzie
        classicVisitor();                // classic visitor = klasyczny wizytator
        doubleDispatchExplained();       // double dispatch explained = podwójna dyspozycja wyjaśniona
        accumulatingAndTransforming();   // accumulating and transforming = gromadzenie wyników i przekształcanie drzewa
        expressionProblem();             // expression problem = problem wyrażenia
        modernAlternative();             // modern alternative = nowoczesna alternatywa (sealed + rekordy)
        visitorInTheJdk();               // visitor in the jdk = wizytator w JDK (FileVisitor)
        testabilityAndStatePitfall();    // testability and state pitfall = testowalność i pułapka stanu
        pitfallsAndWhenToUse();          // pitfalls and when to use = pułapki i kiedy stosować
        allPatternsSummary();            // all patterns summary = podsumowanie wszystkich 15 wzorców
        exercises();                     // exercises = ćwiczenia
    }

    // =================================================================================================
    // Hierarchia wyrażeń (Element + ConcreteElement) i interfejs wizytatora
    // =================================================================================================

    /** ELEMENT — węzeł drzewa wyrażenia. Jedyna metoda: wpuść wizytatora. */
    interface Expr {  // expr = wyrażenie (expression)
        <R> R accept(ExprVisitor<R> visitor);  // <R> = typ wyniku, który wybiera wizytator (generyczna metoda)
    }

    /** VISITOR — po jednej metodzie na rodzaj węzła; R to typ wyniku operacji (Integer, String, Expr...). */
    interface ExprVisitor<R> {
        R visitNum(Num num);  // visit = odwiedź
        R visitVar(Var variable);
        R visitAdd(Add add);
        R visitMul(Mul mul);
    }

    /** Liczba całkowita (liść). */
    record Num(int value) implements Expr {  // num = liczba; value = wartość
        @Override public <R> R accept(ExprVisitor<R> visitor) { return visitor.visitNum(this); }
    }

    /** Zmienna o nazwie, np. x (liść). */
    record Var(String name) implements Expr {  // variable = zmienna
        @Override public <R> R accept(ExprVisitor<R> visitor) { return visitor.visitVar(this); }
    }

    /** Dodawanie (węzeł wewnętrzny). */
    record Add(Expr left, Expr right) implements Expr {  // left = lewy, right = prawy
        @Override public <R> R accept(ExprVisitor<R> visitor) { return visitor.visitAdd(this); }
    }

    /** Mnożenie (węzeł wewnętrzny). */
    record Mul(Expr left, Expr right) implements Expr {  // mul = mnożenie (multiplication)
        @Override public <R> R accept(ExprVisitor<R> visitor) { return visitor.visitMul(this); }
    }

    // Krótkie fabryki — skracają zapis drzewa w przykładach (static factory methods, Patterns03Factory).
    static Expr num(int value) { return new Num(value); }
    static Expr variable(String name) { return new Var(name); }
    static Expr add(Expr left, Expr right) { return new Add(left, right); }
    static Expr mul(Expr left, Expr right) { return new Mul(left, right); }

    /** Wyrażenie przykładowe: 1 + 2 * x. */
    static Expr sample() {
        return add(num(1), mul(num(2), variable("x")));
    }

    // ----- Wizytatory (ConcreteVisitor): po jednej klasie na operację -----

    /** Operacja 1: obliczenie wartości. R = Integer. Zmienne bierze z mapy (environment = środowisko wartości). */
    static class EvalVisitor implements ExprVisitor<Integer> {  // eval = oblicz (evaluate)
        private final Map<String, Integer> environment;  // environment = środowisko: nazwa zmiennej → wartość

        EvalVisitor(Map<String, Integer> environment) {
            this.environment = environment;
        }

        @Override public Integer visitNum(Num num) { return num.value(); }

        @Override
        public Integer visitVar(Var variable) {
            Integer value = environment.get(variable.name());
            if (value == null) {
                throw new IllegalStateException("nieznana zmienna: " + variable.name());
            }
            return value;
        }

        @Override public Integer visitAdd(Add add) { return add.left().accept(this) + add.right().accept(this); }
        @Override public Integer visitMul(Mul mul) { return mul.left().accept(this) * mul.right().accept(this); }
    }

    /** Operacja 2: zapis tekstowy z pełnymi nawiasami. R = String. */
    static class PrintVisitor implements ExprVisitor<String> {  // print = wypisz
        @Override public String visitNum(Num num) { return String.valueOf(num.value()); }
        @Override public String visitVar(Var variable) { return variable.name(); }
        @Override public String visitAdd(Add add) { return "(" + add.left().accept(this) + " + " + add.right().accept(this) + ")"; }
        @Override public String visitMul(Mul mul) { return "(" + mul.left().accept(this) + " * " + mul.right().accept(this) + ")"; }
    }

    /** Operacja 3: liczba węzłów w drzewie. R = Integer. */
    static class CountVisitor implements ExprVisitor<Integer> {  // count = policz
        @Override public Integer visitNum(Num num) { return 1; }
        @Override public Integer visitVar(Var variable) { return 1; }
        @Override public Integer visitAdd(Add add) { return 1 + add.left().accept(this) + add.right().accept(this); }
        @Override public Integer visitMul(Mul mul) { return 1 + mul.left().accept(this) + mul.right().accept(this); }
    }

    /** Skrót: wypisz wyrażenie. */
    static String print(Expr expr) {
        return expr.accept(new PrintVisitor());
    }

    // =================================================================================================
    // 1. PROBLEM: operacje rozsiane albo łańcuch instanceof
    // =================================================================================================

    /** Dodatkowy typ węzła (negacja), którego "stary" kod nie zna. */
    record Neg(Expr inner) implements Expr {  // neg = negacja (minus przed wyrażeniem); inner = wnętrze
        @Override public <R> R accept(ExprVisitor<R> visitor) {
            throw new UnsupportedOperationException("wizytator nie ma metody visitNeg");
        }
    }

    /** Operacja "na piechotę": łańcuch instanceof po typach węzłów (bez wzorca). */
    static int evalOld(Expr expr, Map<String, Integer> env) {  // old = stary
        if (expr instanceof Num num) {
            return num.value();
        } else if (expr instanceof Var variable) {
            return env.get(variable.name());
        } else if (expr instanceof Add add) {
            return evalOld(add.left(), env) + evalOld(add.right(), env);
        } else if (expr instanceof Mul mul) {
            return evalOld(mul.left(), env) * evalOld(mul.right(), env);
        }
        throw new IllegalStateException("nieznany węzeł: " + expr.getClass().getSimpleName());  // getSimpleName = krótka nazwa klasy
    }

    /**
     * 1. Są dwie "naiwne" drogi. (a) Każda operacja jako metoda w KAŻDEJ klasie węzła: nowa operacja (np. zapis RPN)
     * = edycja wszystkich klas, a klasy puchną od niepowiązanych metod. (b) Operacja jako funkcja z łańcuchem
     * instanceof (tutaj): nowy TYP węzła kompiluje się bez problemu, a błąd wychodzi dopiero w czasie działania — i to
     * w każdej operacji, w której zapomniano go obsłużyć.
     */
    static void problemOperationsEverywhere() {
        section("1. Problem: instanceof w każdej operacji");

        Map<String, Integer> env = Map.of("x", 5);  // Map.of = mapa niezmienna (Java 9+)
        show("1 + 2 * x dla x = 5", evalOld(sample(), env));
        // WYNIK: 1 + 2 * x dla x = 5 → 11

        Expr withNeg = add(num(1), new Neg(variable("x")));  // nowy typ węzła pojawił się w drzewie
        expectThrows("evalOld z Neg", () -> evalOld(withNeg, env));
        // WYNIK: ✔ evalOld z Neg → rzucono IllegalStateException: nieznany węzeł: Neg

        // PUŁAPKA: kompilator nie ostrzegł, że dodanie typu Neg wymaga zmian w evalOld, printOld, countOld... —
        // każdą z nich musisz znaleźć sam i błąd zobaczysz dopiero na produkcji.
        // DOBRA PRAKTYKA: zbierz "co robimy z węzłem" w jednym interfejsie (wizytator) — wtedy nowy typ węzła
        // wymusza zmianę interfejsu i kompilator pokaże KAŻDE miejsce, które trzeba uzupełnić.
    }

    // =================================================================================================
    // 2. KLASYCZNY WIZYTATOR
    // =================================================================================================

    /**
     * 2. Klasyczny wizytator w akcji. Zauważ: klasy Num/Var/Add/Mul nie wiedzą nic o obliczaniu, wypisywaniu ani
     * liczeniu. Wiedzą tylko jedno: "wpuść wizytatora i powiedz mu, kim jestem" (visitXxx(this)).
     */
    static void classicVisitor() {
        section("2. Klasyczny Visitor: accept + visitXxx");

        Expr expr = sample();
        show("zapis", expr.accept(new PrintVisitor()));  // accept = wpuść wizytatora
        // WYNIK: zapis → (1 + (2 * x))
        show("wartość dla x = 5", expr.accept(new EvalVisitor(Map.of("x", 5))));
        // WYNIK: wartość dla x = 5 → 11
        show("wartość dla x = 10", expr.accept(new EvalVisitor(Map.of("x", 10))));
        // WYNIK: wartość dla x = 10 → 21
        show("liczba węzłów", expr.accept(new CountVisitor()));
        // WYNIK: liczba węzłów → 5
        show("drzewo jako rekord", expr);
        // WYNIK: drzewo jako rekord → Add[left=Num[value=1], right=Mul[left=Num[value=2], right=Var[name=x]]]
        expectThrows("brak zmiennej", () -> expr.accept(new EvalVisitor(Map.of())));
        // WYNIK: ✔ brak zmiennej → rzucono IllegalStateException: nieznana zmienna: x

        // DOBRA PRAKTYKA: typ wyniku jako parametr generyczny R pozwala jednemu interfejsowi obsłużyć operacje,
        // które zwracają Integer, String albo nowe drzewo (sekcja 4).
        // PUŁAPKA: "boilerplate" (powtarzalny kod): jedna metoda accept w każdym węźle + po jednej metodzie visit
        // na węzeł w każdym wizytatorze. To cena za oddzielenie operacji od danych.
    }

    // =================================================================================================
    // 3. PODWÓJNA DYSPOZYCJA
    // =================================================================================================

    /** Przeciążone metody — kompilator wybiera ją po TYPIE ZMIENNEJ (statycznym), nie po typie obiektu w środku. */
    static class OverloadDemo {  // overload = przeciążenie (wiele metod o tej samej nazwie)
        String describe(Expr expr) { return "to jest Expr"; }
        String describe(Num num) { return "to jest Num"; }
    }

    /**
     * 3. Dlaczego nie wystarczą przeciążone metody {@code describe(Num)}, {@code describe(Add)}? Bo Java wybiera
     * przeciążenie w CZASIE KOMPILACJI po typie zmiennej. Polimorfizm w czasie działania (wirtualne metody) wybiera
     * tylko po typie ODBIORCY wywołania (obiektu przed kropką). Wizytator robi to w dwóch krokach: accept (po typie
     * węzła), potem visitXxx (po typie wizytatora) — razem to "podwójna dyspozycja".
     */
    static void doubleDispatchExplained() {
        section("3. Podwójna dyspozycja: dlaczego accept(this)");

        OverloadDemo demo = new OverloadDemo();
        Expr node = num(42);  // w środku Num, ale zmienna ma typ Expr
        show("describe(node)", demo.describe(node));
        // WYNIK: describe(node) → to jest Expr
        show("describe((Num) node)", demo.describe((Num) node));  // rzutowanie zmienia typ statyczny
        // WYNIK: describe((Num) node) → to jest Num

        // To samo przez accept: węzeł sam wie, że jest Num, i woła właściwą metodę wizytatora.
        ExprVisitor<String> describer = new ExprVisitor<>() {  // klasa anonimowa implementująca wizytatora
            @Override public String visitNum(Num num) { return "to jest Num"; }
            @Override public String visitVar(Var variable) { return "to jest Var"; }
            @Override public String visitAdd(Add add) { return "to jest Add"; }
            @Override public String visitMul(Mul mul) { return "to jest Mul"; }
        };
        show("node.accept(describer)", node.accept(describer));
        // WYNIK: node.accept(describer) → to jest Num
        show("sample().accept(describer)", sample().accept(describer));
        // WYNIK: sample().accept(describer) → to jest Add

        // PUŁAPKA: dopisanie kolejnego przeciążenia describe(Add) NIE pomoże, gdy zmienna ma typ Expr —
        // kompilator nadal wybierze describe(Expr). Dlatego wizytator ma OSOBNE nazwy visitNum, visitAdd... i wywołuje
        // je z wnętrza węzła, gdzie typ this jest znany dokładnie.
    }

    // =================================================================================================
    // 4. GROMADZENIE WYNIKÓW I PRZEKSZTAŁCANIE DRZEWA
    // =================================================================================================

    /** Wizytator "z pamięcią": zbiera nazwy zmiennych do zbioru posortowanego (TreeSet — drukowanie jest powtarzalne). */
    static class VariablesCollector implements ExprVisitor<Void> {  // collector = zbieracz; Void = "brak wyniku" (zawsze null)
        private final Set<String> names = new TreeSet<>();  // TreeSet = zbiór posortowany alfabetycznie

        Set<String> names() { return Collections.unmodifiableSet(names); }  // unmodifiableSet = widok tylko do odczytu

        @Override public Void visitNum(Num num) { return null; }
        @Override public Void visitVar(Var variable) { names.add(variable.name()); return null; }
        @Override public Void visitAdd(Add add) { add.left().accept(this); add.right().accept(this); return null; }
        @Override public Void visitMul(Mul mul) { mul.left().accept(this); mul.right().accept(this); return null; }
    }

    /** Wizytator-przekształcacz: R = Expr, zwraca NOWE drzewo (stare się nie zmienia). Upraszcza stałe: 1+2 → 3, x*1 → x, 0+x → x. */
    static class SimplifyVisitor implements ExprVisitor<Expr> {  // simplify = uprość
        @Override public Expr visitNum(Num num) { return num; }
        @Override public Expr visitVar(Var variable) { return variable; }

        @Override
        public Expr visitAdd(Add add) {
            Expr left = add.left().accept(this);    // najpierw upraszczamy dzieci (rekurencja przez accept)
            Expr right = add.right().accept(this);
            if (left instanceof Num l && right instanceof Num r) {
                return num(l.value() + r.value());   // stała + stała = stała
            }
            if (left instanceof Num l && l.value() == 0) {
                return right;                        // 0 + x = x
            }
            if (right instanceof Num r && r.value() == 0) {
                return left;                         // x + 0 = x
            }
            return add(left, right);
        }

        @Override
        public Expr visitMul(Mul mul) {
            Expr left = mul.left().accept(this);
            Expr right = mul.right().accept(this);
            if (left instanceof Num l && right instanceof Num r) {
                return num(l.value() * r.value());
            }
            if (left instanceof Num l && l.value() == 1) {
                return right;                        // 1 * x = x
            }
            if (right instanceof Num r && r.value() == 1) {
                return left;                         // x * 1 = x
            }
            return mul(left, right);
        }
    }

    /**
     * 4. Wizytator nie musi tylko liczyć liczby. Może (a) gromadzić dane w polu (zbieranie zmiennych), (b) zwracać wynik
     * dla każdego węzła i składać go z wyników dzieci (Eval, Print), (c) budować NOWE drzewo (uproszczenie). W (c)
     * R = Expr — te same rekordy, które są niezmienne, więc stare drzewo zostaje nietknięte.
     */
    static void accumulatingAndTransforming() {
        section("4. Wizytator zbiera dane i przekształca drzewo");

        Expr expr = add(mul(variable("y"), num(1)), mul(add(num(1), num(2)), variable("x")));  // (y * 1) + ((1 + 2) * x)

        VariablesCollector collector = new VariablesCollector();
        expr.accept(collector);  // wynik zwracany jest nieważny (Void), liczy się stan zbieracza
        show("zmienne (posortowane)", collector.names());
        // WYNIK: zmienne (posortowane) → [x, y]

        Expr simplified = expr.accept(new SimplifyVisitor());
        show("przed", print(expr));
        // WYNIK: przed → ((y * 1) + ((1 + 2) * x))
        show("po uproszczeniu", print(simplified));
        // WYNIK: po uproszczeniu → (y + (3 * x))
        show("oryginał bez zmian", print(expr));
        // WYNIK: oryginał bez zmian → ((y * 1) + ((1 + 2) * x))

        // DOBRA PRAKTYKA: wizytator zwracający wynik (R) jest łatwiejszy do przetestowania i bezpieczniejszy niż
        // wizytator z polem; używaj pól tylko do prawdziwego gromadzenia (sekcja 8: pułapka ponownego użycia).
        // PUŁAPKA: uproszczenie tylko jednego przejścia nie zawsze jest pełne (np. wynik reguły tworzy nową okazję
        // do uproszczenia wyżej) — tu nie występuje, bo najpierw upraszczamy dzieci, a dopiero potem rodzica.
    }

    // =================================================================================================
    // 5. PROBLEM WYRAŻENIA
    // =================================================================================================

    /** Nowa operacja dopisana BEZ zmian w klasach węzłów: zapis w odwrotnej notacji polskiej (RPN — operator na końcu). */
    static class PostfixVisitor implements ExprVisitor<String> {  // postfix = zapis przyrostkowy
        @Override public String visitNum(Num num) { return String.valueOf(num.value()); }
        @Override public String visitVar(Var variable) { return variable.name(); }
        @Override public String visitAdd(Add add) { return add.left().accept(this) + " " + add.right().accept(this) + " +"; }
        @Override public String visitMul(Mul mul) { return mul.left().accept(this) + " " + mul.right().accept(this) + " *"; }
    }

    /**
     * 5. Problem wyrażenia (expression problem): masz hierarchię typów i zbiór operacji — chcesz łatwo dodawać i jedno, i
     * drugie, ale żadne podejście nie daje obu naraz:
     * <pre>
     *                          | nowa OPERACJA              | nowy TYP węzła
     *   Wizytator              | ŁATWO: nowa klasa          | TRUDNO: zmiana interfejsu + wszystkich wizytatorów
     *   metody w klasach       | TRUDNO: edycja wszystkich  | ŁATWO: nowa klasa
     * </pre>
     * Wizytator wybieraj, gdy hierarchia typów jest STAŁA, a operacji przybywa (kompilatory, parsery, eksporty).
     */
    static void expressionProblem() {
        section("5. Problem wyrażenia: łatwo dodać operację, trudno typ");

        Expr expr = sample();
        show("RPN", expr.accept(new PostfixVisitor()));  // nowa operacja: zero zmian w Num/Var/Add/Mul
        // WYNIK: RPN → 1 2 x * +
        show("zapis zwykły", print(expr));
        // WYNIK: zapis zwykły → (1 + (2 * x))

        // Nowy typ węzła (np. Neg) wymusza: dopisanie visitNeg do interfejsu ExprVisitor, więc KAŻDY wizytator przestaje
        // się kompilować, dopóki nie dostanie tej metody. To zaleta (kompilator pokazuje wszystkie miejsca) i wada
        // (praca przy każdej nowej klasie węzła).
        // PUŁAPKA: pokusa "załatwienia" tego domyślną metodą default visitNeg(...) { return null; } w interfejsie.
        // To po cichu wyłącza sprawdzanie kompilatora — wizytator zapomni o Neg i nikt tego nie zauważy.
        note("hierarchia stała, operacji przybywa → Visitor; typów przybywa → zwykły polimorfizm");
        // WYNIK: ℹ hierarchia stała, operacji przybywa → Visitor; typów przybywa → zwykły polimorfizm
    }

    // =================================================================================================
    // 6. NOWOCZESNA ALTERNATYWA: sealed + rekordy
    // =================================================================================================

    /** Zapieczętowana hierarchia (Java 17): kompilator zna WSZYSTKIE dopuszczone typy. */
    sealed interface Node permits Lit, Ref, Plus, Times { }  // node = węzeł; sealed = zapieczętowany

    record Lit(int value) implements Node { }                        // lit = literał (stała liczba)
    record Ref(String name) implements Node { }                      // ref = odwołanie do zmiennej (reference)
    record Plus(Node left, Node right) implements Node { }           // plus = dodawanie
    record Times(Node left, Node right) implements Node { }          // times = mnożenie

    /** Operacja jako zwykła metoda statyczna z instanceof (Java 16+): bez accept, bez wizytatora. */
    static int evalModern(Node node, Map<String, Integer> env) {
        if (node instanceof Lit lit) {
            return lit.value();
        }
        if (node instanceof Ref ref) {
            return env.get(ref.name());
        }
        if (node instanceof Plus plus) {
            return evalModern(plus.left(), env) + evalModern(plus.right(), env);
        }
        if (node instanceof Times times) {
            return evalModern(times.left(), env) * evalModern(times.right(), env);
        }
        throw new IllegalStateException("nieznany węzeł");  // w Javie 17 kompilator nie sprawdza kompletności łańcucha if
        // (Java 21+): switch ze wzorcami jest sprawdzany pod kątem kompletności przy sealed — bez gałęzi default:
        //   return switch (node) {
        //       case Lit lit -> lit.value();
        //       case Ref ref -> env.get(ref.name());
        //       case Plus plus -> evalModern(plus.left(), env) + evalModern(plus.right(), env);
        //       case Times times -> evalModern(times.left(), env) * evalModern(times.right(), env);
        //   };   // brak jednej gałęzi = błąd kompilacji
    }

    /** Zapis tekstowy tą samą techniką. */
    static String showModern(Node node) {
        if (node instanceof Lit lit) {
            return String.valueOf(lit.value());
        }
        if (node instanceof Ref ref) {
            return ref.name();
        }
        if (node instanceof Plus plus) {
            return "(" + showModern(plus.left()) + " + " + showModern(plus.right()) + ")";
        }
        if (node instanceof Times times) {
            return "(" + showModern(times.left()) + " * " + showModern(times.right()) + ")";
        }
        throw new IllegalStateException("nieznany węzeł");
    }

    /**
     * 6. Rekordy i zapieczętowany interfejs dają krótszą alternatywę: operacje to zwykłe metody, bez accept i bez
     * interfejsu wizytatora. Porównanie:
     * <pre>
     *                        | Visitor (klasyczny)            | sealed + instanceof / switch
     *   boilerplate          | duży (accept + visit)          | mały
     *   nowa operacja        | nowa klasa                     | nowa metoda statyczna
     *   nowy typ węzła       | błąd kompilacji we wszystkich  | Java 17: runtime; Java 21: błąd kompilacji
     *   podwójna dyspozycja  | tak                            | nie (niepotrzebna)
     * </pre>
     * Wizytator zostaje dobrym wyborem, gdy: kod ma działać w starszej Javie bez sealed, hierarchia jest w innej
     * bibliotece (nie możesz jej zapieczętować), albo potrzebujesz kontroli nad przechodzeniem po drzewie.
     */
    static void modernAlternative() {
        section("6. Nowocześnie: sealed + rekordy + instanceof");

        Node tree = new Plus(new Lit(1), new Times(new Lit(2), new Ref("x")));
        show("zapis", showModern(tree));
        // WYNIK: zapis → (1 + (2 * x))
        show("wartość dla x = 5", evalModern(tree, Map.of("x", 5)));
        // WYNIK: wartość dla x = 5 → 11
        show("wartość dla x = 7", evalModern(tree, Map.of("x", 7)));
        // WYNIK: wartość dla x = 7 → 15

        // DOBRA PRAKTYKA: dla własnych, zamkniętych hierarchii w Javie 17+ zacznij od sealed + rekordów; sięgnij po
        // klasyczny wizytator, gdy brakuje Ci jego zalet (kompletność sprawdzana kompilatorem w Javie 17,
        // hierarchia spoza Twojego projektu, wielu "odwiedzających" z własnym stanem).
        // PUŁAPKA: w Javie 17 ostatni "throw" w łańcuchu instanceof jest ręcznym zabezpieczeniem. Nowy rekord
        // dopuszczony w permits skompiluje się bez ostrzeżenia, a błąd pokaże się dopiero w czasie działania.
    }

    // =================================================================================================
    // 7. WIZYTATOR W JDK: FileVisitor
    // =================================================================================================

    /**
     * Prawdziwy wizytator z JDK: {@code Files.walkFileTree(start, visitor)} chodzi po drzewie katalogów, a wizytator
     * ({@code SimpleFileVisitor}) dostaje wywołania dla plików i katalogów. Wynik wywołania ({@code FileVisitResult})
     * steruje przechodzeniem: CONTINUE (dalej) albo SKIP_SUBTREE (pomiń ten katalog).
     */
    static void walkDemo() throws IOException {  // throws = może rzucić
        Path root = TempDir.create("wizytator");
        try {
            Files.writeString(root.resolve("a.txt"), "a");  // writeString = zapisz tekst (Java 11+)
            Files.writeString(root.resolve("b.txt"), "b");
            Path sub = Files.createDirectory(root.resolve("sub"));
            Files.writeString(sub.resolve("c.txt"), "c");

            List<String> allFiles = new ArrayList<>();
            int[] directories = {0};  // tablica jednoelementowa: licznik modyfikowany w klasie anonimowej
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {  // przed wejściem do katalogu
                    directories[0]++;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {  // dla każdego pliku
                    allFiles.add(file.getFileName().toString());
                    return FileVisitResult.CONTINUE;
                }
            });
            Collections.sort(allFiles);  // kolejność przechodzenia zależy od systemu — sortujemy dla powtarzalności
            show("pliki (posortowane)", allFiles);
            show("odwiedzone katalogi", directories[0]);

            List<String> withoutSub = new ArrayList<>();
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    boolean isSub = dir.getFileName().toString().equals("sub");
                    return isSub ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;  // skip subtree = pomiń poddrzewo
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    withoutSub.add(file.getFileName().toString());
                    return FileVisitResult.CONTINUE;
                }
            });
            Collections.sort(withoutSub);
            show("pliki bez katalogu sub", withoutSub);
        } finally {
            TempDir.deleteRecursively(root);  // sprzątamy po sobie (t18_io_files/Io07WalkingDirectories)
        }
    }

    /**
     * 7. Gdzie jeszcze: {@code javax.lang.model.element.ElementVisitor} w procesorach adnotacji (t19), biblioteki do
     * czytania kodu bajtowego (np. ASM ma {@code ClassVisitor}), a w Springu {@code BeanDefinitionVisitor} przechodzi po
     * definicjach beanów. Wspólna cecha: struktura należy do biblioteki, a Ty dopisujesz własną operację.
     */
    static void visitorInTheJdk() {
        section("7. Wizytator w JDK: Files.walkFileTree");

        try {
            walkDemo();
            // WYNIK: pliki (posortowane) → [a.txt, b.txt, c.txt]
            // WYNIK: odwiedzone katalogi → 2
            // WYNIK: pliki bez katalogu sub → [a.txt, b.txt]
        } catch (IOException e) {
            throw new UncheckedIOException(e);  // unchecked = niekontrolowany
        }

        // DOBRA PRAKTYKA: zwracanie wartości sterującej (CONTINUE / SKIP_SUBTREE / TERMINATE) pozwala wizytatorowi
        // wpływać na przechodzenie — własne wizytatory też mogą to robić, jeśli przechodzenie to zadanie klasy
        // zarządzającej drzewem (a nie węzłów).
        // PUŁAPKA: kolejność odwiedzania plików w katalogu nie jest gwarantowana — nie polegaj na niej w testach.
    }

    // =================================================================================================
    // 8. TESTOWALNOŚĆ I PUŁAPKA STANU
    // =================================================================================================

    /** Stanowy wizytator: zlicza odwiedzone węzły w polu. Jednorazowy! */
    static class NodeCounter implements ExprVisitor<Void> {  // counter = licznik
        int count;  // count = liczba odwiedzonych węzłów

        @Override public Void visitNum(Num num) { count++; return null; }
        @Override public Void visitVar(Var variable) { count++; return null; }
        @Override public Void visitAdd(Add add) { count++; add.left().accept(this); add.right().accept(this); return null; }
        @Override public Void visitMul(Mul mul) { count++; mul.left().accept(this); mul.right().accept(this); return null; }
    }

    /**
     * 8. Wizytatory testuje się świetnie: każda operacja to osobna klasa, a wejściem jest małe, ręcznie zbudowane
     * drzewo. Pułapka: wizytator z polem (stanem) po pierwszym użyciu "pamięta" poprzednie przejście.
     */
    static void testabilityAndStatePitfall() {
        section("8. Testowalność wizytatora i pułapka stanu");

        Expr expr = sample();
        Check.equal("eval 1 + 2 * x, x = 5", 11, () -> expr.accept(new EvalVisitor(Map.of("x", 5))));
        Check.equal("zapis (1 + (2 * x))", "(1 + (2 * x))", () -> print(expr));
        Check.equal("liczba węzłów", 5, () -> expr.accept(new CountVisitor()));
        Check.equal("RPN", "1 2 x * +", () -> expr.accept(new PostfixVisitor()));
        Check.equal("uproszczenie (1 + 2)", "3", () -> print(add(num(1), num(2)).accept(new SimplifyVisitor())));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD

        NodeCounter reused = new NodeCounter();
        expr.accept(reused);
        show("pierwsze przejście", reused.count);
        // WYNIK: pierwsze przejście → 5
        expr.accept(reused);  // ten sam obiekt użyty ponownie
        show("drugie przejście tym samym obiektem", reused.count);
        // WYNIK: drugie przejście tym samym obiektem → 10
        NodeCounter fresh = new NodeCounter();
        expr.accept(fresh);
        show("świeży wizytator", fresh.count);
        // WYNIK: świeży wizytator → 5

        // PUŁAPKA: stanowy wizytator użyty drugi raz daje 10 zamiast 5 — liczy "od stanu poprzedniego".
        // DOBRA PRAKTYKA: wizytator ze stanem twórz na JEDNO przejście (new przed każdym accept) albo zwracaj wynik
        // (R), a nie gromadź w polu.
    }

    // =================================================================================================
    // 9. PUŁAPKI I KIEDY STOSOWAĆ
    // =================================================================================================

    /**
     * 9. Typowe błędy i decyzja.
     */
    static void pitfallsAndWhenToUse() {
        section("9. Pułapki i kiedy stosować");

        // PUŁAPKA: wizytator na hierarchii, która często zyskuje nowe typy — każdy nowy typ to zmiana interfejsu i
        // wszystkich wizytatorów (sekcja 5).
        // PUŁAPKA: łamanie hermetyzacji: wizytator potrzebuje dostępu do danych węzła (gettery/rekordy), więc dane
        // węzłów muszą być jawne. To cena za operacje "poza klasą".
        // PUŁAPKA: wizytator tam, gdzie wystarcza jedna operacja (YAGNI = "nie będziesz tego potrzebował"): jedna metoda
        // w klasach albo jedna funkcja instanceof jest prostsza niż interfejs, accept i klasa wizytatora.
        // PUŁAPKA: wizytator zwraca void i wszystko robi efektem ubocznym — trudniej testować; woli się R.
        // PUŁAPKA: zapominanie o rekurencji — wizytator węzła wewnętrznego musi sam zawołać accept na dzieciach
        // (lub odpowiada za to osobna klasa przechodząca drzewo).
        note("Visitor = stała hierarchia + przybywające operacje; inaczej wybierz prostsze narzędzie");
        // WYNIK: ℹ Visitor = stała hierarchia + przybywające operacje; inaczej wybierz prostsze narzędzie

        // KIEDY UŻYWAĆ / KIEDY NIE:
        //   używaj:  stabilna hierarchia (drzewo składniowe, model dokumentu, struktura plików) i wiele niezwiązanych
        //            operacji na niej (eksport, walidacja, obliczenia, optymalizacje); chcesz każdą operację mieć w jednym miejscu.
        //   nie używaj: hierarchia często się zmienia; jest jedna lub dwie operacje; w Javie 17+ wystarczy sealed + rekordy
        //            + instanceof (a od Javy 21 switch ze wzorcami z kontrolą kompletności).
    }

    // =================================================================================================
    // 10. PODSUMOWANIE WSZYSTKICH 15 WZORCÓW
    // =================================================================================================

    /** Karta wzorca: nazwa, problem, który rozwiązuje, przykład z JDK i ze Springu. */
    record PatternCard(String name, String problem, String jdk, String spring) { }  // card = karta; problem = problem

    /** Dane tabeli: wszystkie 15 wzorców tego działu (w kolejności lekcji). */
    static List<PatternCard> cards() {
        return List.of(
                new PatternCard("01 Strategy (strategia)", "wiele wymiennych algorytmów", "Comparator", "bean wstrzyknięty przez interfejs"),
                new PatternCard("02 Builder (budowniczy)", "obiekt z wieloma parametrami", "StringBuilder, HttpRequest.newBuilder()", "UriComponentsBuilder"),
                new PatternCard("03 Factory (fabryka)", "tworzenie obiektów bez new u klienta", "List.of, Integer.valueOf", "BeanFactory"),
                new PatternCard("04 Singleton (jedynak)", "dokładnie jedna instancja", "Runtime.getRuntime()", "bean o zasięgu singleton (to NIE GoF)"),
                new PatternCard("05 Template Method (metoda szablonowa)", "stały szkielet, zmienne kroki", "AbstractList", "JdbcTemplate"),
                new PatternCard("06 Observer (obserwator)", "powiadamianie o zdarzeniach", "PropertyChangeListener", "ApplicationEvent, @EventListener"),
                new PatternCard("07 Decorator (dekorator)", "dodawanie zachowania przez opakowanie", "BufferedInputStream, Collections.unmodifiableList", "proxy (np. transakcje)"),
                new PatternCard("08 Dependency Injection (wstrzykiwanie zależności)", "zależności podawane z zewnątrz", "konstruktor z interfejsem", "kontener IoC, @Autowired"),
                new PatternCard("09 Command (polecenie)", "operacja jako obiekt (undo, kolejka)", "Runnable, Callable", "zadania w TaskExecutor"),
                new PatternCard("10 Facade (fasada)", "prosty interfejs do złożonego podsystemu", "java.nio.file.Files", "warstwa serwisów"),
                new PatternCard("11 Adapter (adapter)", "niezgodne interfejsy", "Arrays.asList, InputStreamReader", "HandlerAdapter"),
                new PatternCard("12 Composite (kompozyt)", "drzewo: całość jak część", "java.awt.Container", "CompositeCacheManager"),
                new PatternCard("13 State (stan)", "zachowanie zależne od stanu", "Thread.State", "Spring Statemachine"),
                new PatternCard("14 Chain of Responsibility (łańcuch odpowiedzialności)", "żądanie przez kolejne ogniwa", "bloki catch, Filter w serwletach", "HandlerInterceptor, Spring Security"),
                new PatternCard("15 Visitor (wizytator)", "nowe operacje na stałej hierarchii", "FileVisitor", "BeanDefinitionVisitor"));
    }

    /**
     * 10. Tabela: problem → wzorzec → gdzie spotkasz w JDK i Springu. Wzorce dzielą się z grubsza na: TWORZĄCE (Builder,
     * Factory, Singleton), STRUKTURALNE (Decorator, Facade, Adapter, Composite) i BEHAWIORALNE, czyli opisujące
     * zachowanie (Strategy, Template Method, Observer, Command, State, Chain of Responsibility, Visitor). Dependency
     * Injection to raczej technika łączenia obiektów, bliska zasadzie odwrócenia zależności (DIP, CleanCode02Solid).
     */
    static void allPatternsSummary() {
        section("10. Podsumowanie: 15 wzorców (problem → wzorzec → JDK / Spring)");

        List<String> lines = new ArrayList<>();
        for (PatternCard card : cards()) {
            lines.add(card.name() + ": " + card.problem() + " | JDK: " + card.jdk() + " | Spring: " + card.spring());
        }
        showEach("tabela wzorców", lines);
        // WYNIK: tabela wzorców (liczba elementów: 15):
        // WYNIK: • 01 Strategy (strategia): wiele wymiennych algorytmów | JDK: Comparator | Spring: bean wstrzyknięty przez interfejs
        // WYNIK: • 02 Builder (budowniczy): obiekt z wieloma parametrami | JDK: StringBuilder, HttpRequest.newBuilder() | Spring: UriComponentsBuilder
        // WYNIK: • 03 Factory (fabryka): tworzenie obiektów bez new u klienta | JDK: List.of, Integer.valueOf | Spring: BeanFactory
        // WYNIK: • 04 Singleton (jedynak): dokładnie jedna instancja | JDK: Runtime.getRuntime() | Spring: bean o zasięgu singleton (to NIE GoF)
        // WYNIK: • 05 Template Method (metoda szablonowa): stały szkielet, zmienne kroki | JDK: AbstractList | Spring: JdbcTemplate
        // WYNIK: • 06 Observer (obserwator): powiadamianie o zdarzeniach | JDK: PropertyChangeListener | Spring: ApplicationEvent, @EventListener
        // WYNIK: • 07 Decorator (dekorator): dodawanie zachowania przez opakowanie | JDK: BufferedInputStream, Collections.unmodifiableList | Spring: proxy (np. transakcje)
        // WYNIK: • 08 Dependency Injection (wstrzykiwanie zależności): zależności podawane z zewnątrz | JDK: konstruktor z interfejsem | Spring: kontener IoC, @Autowired
        // WYNIK: • 09 Command (polecenie): operacja jako obiekt (undo, kolejka) | JDK: Runnable, Callable | Spring: zadania w TaskExecutor
        // WYNIK: • 10 Facade (fasada): prosty interfejs do złożonego podsystemu | JDK: java.nio.file.Files | Spring: warstwa serwisów
        // WYNIK: • 11 Adapter (adapter): niezgodne interfejsy | JDK: Arrays.asList, InputStreamReader | Spring: HandlerAdapter
        // WYNIK: • 12 Composite (kompozyt): drzewo: całość jak część | JDK: java.awt.Container | Spring: CompositeCacheManager
        // WYNIK: • 13 State (stan): zachowanie zależne od stanu | JDK: Thread.State | Spring: Spring Statemachine
        // WYNIK: • 14 Chain of Responsibility (łańcuch odpowiedzialności): żądanie przez kolejne ogniwa | JDK: bloki catch, Filter w serwletach | Spring: HandlerInterceptor, Spring Security
        // WYNIK: • 15 Visitor (wizytator): nowe operacje na stałej hierarchii | JDK: FileVisitor | Spring: BeanDefinitionVisitor

        show("liczba kart", cards().size());
        // WYNIK: liczba kart → 15

        // DOBRA PRAKTYKA: wzorce to NAZWY dla powtarzających się rozwiązań — ułatwiają rozmowę ("tu dałbym dekorator")
        // i czytanie cudzego kodu. Nie są celem samym w sobie. Zacznij od problemu: jeśli bolą Cię rosnące switch-e,
        // sięgnij po Strategy/State; gdy zależności utrudniają testy — DI; gdy cudze API nie pasuje — Adapter.
        // PUŁAPKA: "wzorcozy" (patternitis) — wciskanie wzorców tam, gdzie wystarczy if, to klasyczna choroba początkujących.
        // Dobra kolejność nauki: najpierw prosty kod, potem refaktoryzacja do wzorca, gdy pojawi się realny ból.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Visitor = operacje na stałej hierarchii zapisane w osobnych klasach (wizytatorach); węzły tylko robią accept(this).
     *   • Role: Element (accept), ConcreteElement, Visitor (visitXxx na każdy typ), ConcreteVisitor (jedna operacja).
     *   • Podwójna dyspozycja: accept wybiera po typie węzła, visitXxx — po typie wizytatora; przeciążenia tego nie dają.
     *   • R jako typ wyniku: Integer (oblicz), String (wypisz), Expr (nowe drzewo); stan w polu tylko do gromadzenia.
     *   • Problem wyrażenia: Visitor — łatwo nowa operacja, trudno nowy typ; metody w klasach — odwrotnie.
     *   • Nowocześnie: sealed + rekordy + instanceof (Java 16+); switch ze wzorcami z kontrolą kompletności (Java 21+).
     *   • Stanowego wizytatora twórz na jedno przejście; po ponownym użyciu liczy dalej od starego stanu.
     *   • JDK i Spring: FileVisitor/SimpleFileVisitor, ElementVisitor, BeanDefinitionVisitor.
     *   • 15 wzorców: tworzące (Builder, Factory, Singleton), strukturalne (Decorator, Facade, Adapter, Composite),
     *     behawioralne (Strategy, Template Method, Observer, Command, State, Chain of Responsibility, Visitor) oraz DI.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego węzeł woła visitor.visitAdd(this), a nie jedną wspólną metodę visit(Expr)?
     *   2. Co to jest "podwójna dyspozycja" i które dwa wybory w niej występują?
     *   3. Co wypisze:  Expr node = num(42); System.out.println(new OverloadDemo().describe(node));  ?
     *   4. Co wypisze:  NodeCounter c = new NodeCounter(); sample().accept(c); sample().accept(c); System.out.println(c.count);  ?
     *   5. ZNAJDŹ BŁĄD:  w interfejsie wizytatora dodano default R visitNeg(Neg n) { return null; } "żeby nic się nie
     *      zepsuło". Co tracimy?
     *   6. ZNAJDŹ BŁĄD:  visitAdd w wizytatorze zwraca "(" + "+" + ")" bez wołania accept na lewym i prawym dziecku.
     *      Co zobaczy użytkownik?
     *   7. Kiedy w Javie 17 wybierzesz sealed + rekordy + instanceof zamiast klasycznego wizytatora?
     *   8. Podaj dla trzech wzorców z tabeli (sekcja 10) jedną ich zaletę i jedną wadę.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: liczba literałów", 2, () -> exercise1(sample()));
        Check.equal("ćw. 2: zmienne bez powtórzeń", List.of("x", "y"), () -> exercise2(mul(variable("x"), add(variable("y"), variable("x")))));
        Check.equal("ćw. 3: głębokość 1 + 2 * x", 3, () -> exercise3(sample()));
        Check.equal("ćw. 3: głębokość x", 1, () -> exercise3(variable("x")));
        Check.equal("ćw. 4: x * 0 → 0", "y", () -> exercise4(add(variable("y"), mul(variable("x"), num(0)))));
        Check.equal("ćw. 4: (1 + 2) * x", "(3 * x)", () -> exercise4(mul(add(num(1), num(2)), variable("x"))));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 2, () -> solution1(sample()));
        Check.equal("ćw. 2 (wzorzec)", List.of("x", "y"), () -> solution2(mul(variable("x"), add(variable("y"), variable("x")))));
        Check.equal("ćw. 3 (wzorzec)", 3, () -> solution3(sample()));
        Check.equal("ćw. 3b (wzorzec)", 1, () -> solution3(variable("x")));
        Check.equal("ćw. 4 (wzorzec)", "y", () -> solution4(add(variable("y"), mul(variable("x"), num(0)))));
        Check.equal("ćw. 4b (wzorzec)", "(3 * x)", () -> solution4(mul(add(num(1), num(2)), variable("x"))));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz LITERAŁY (węzły Num) w wyrażeniu za pomocą własnego wizytatora ExprVisitor.
     * Dla 1 + 2 * x wynik to 2.
     * Podpowiedź: wizytator zwracający Integer — Num daje 1, Var 0, a Add i Mul sumę wyników dzieci.
     */
    static int exercise1(Expr expr) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć nazwy zmiennych w kolejności pierwszego wystąpienia (od lewej), bez powtórzeń.
     * Dla x * (y + x) wynik to [x, y].
     * Podpowiedź: wizytator zbierający do LinkedHashSet (zachowuje kolejność wstawiania), na końcu new ArrayList<>(zbiór).
     */
    static List<String> exercise2(Expr expr) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ): stary kod liczył głębokość drzewa łańcuchem instanceof:
     * <pre>{@code
     * static int depthOld(Expr e) {
     *     if (e instanceof Add a) { return 1 + Math.max(depthOld(a.left()), depthOld(a.right())); }
     *     if (e instanceof Mul m) { return 1 + Math.max(depthOld(m.left()), depthOld(m.right())); }
     *     return 1;   // Num i Var
     * }
     * }</pre>
     * Przepisz na wizytator (głębokość liścia = 1, węzła wewnętrznego = 1 + większa z głębokości dzieci).
     * Podpowiedź: ExprVisitor z R = Integer; dzieci odwiedzasz przez child.accept(this).
     */
    static int exercise3(Expr expr) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): rozszerz uproszczenia z SimplifyVisitor o regułę zerowania: x * 0 = 0 oraz 0 * x = 0.
     * Zwróć uproszczone wyrażenie jako tekst (print). Dla y + x * 0 wynik to "y" (najpierw x*0 → 0, potem y + 0 → y).
     * Podpowiedź: napisz własny wizytator (możesz skopiować SimplifyVisitor) albo odziedzicz po nim i nadpisz visitMul:
     * najpierw uprość dzieci, sprawdź zero, w przeciwnym razie zawołaj super.visitMul(mul).
     */
    static String exercise4(Expr expr) {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(Expr expr) {
        return expr.accept(new ExprVisitor<Integer>() {
            @Override public Integer visitNum(Num num) { return 1; }
            @Override public Integer visitVar(Var variable) { return 0; }
            @Override public Integer visitAdd(Add add) { return add.left().accept(this) + add.right().accept(this); }
            @Override public Integer visitMul(Mul mul) { return mul.left().accept(this) + mul.right().accept(this); }
        });
    }

    static List<String> solution2(Expr expr) {
        Set<String> names = new LinkedHashSet<>();  // LinkedHashSet = zbiór zachowujący kolejność wstawiania
        expr.accept(new ExprVisitor<Void>() {
            @Override public Void visitNum(Num num) { return null; }
            @Override public Void visitVar(Var variable) { names.add(variable.name()); return null; }
            @Override public Void visitAdd(Add add) { add.left().accept(this); add.right().accept(this); return null; }
            @Override public Void visitMul(Mul mul) { mul.left().accept(this); mul.right().accept(this); return null; }
        });
        return new ArrayList<>(names);
    }

    static int solution3(Expr expr) {
        return expr.accept(new ExprVisitor<Integer>() {
            @Override public Integer visitNum(Num num) { return 1; }
            @Override public Integer visitVar(Var variable) { return 1; }
            @Override public Integer visitAdd(Add add) { return 1 + Math.max(add.left().accept(this), add.right().accept(this)); }
            @Override public Integer visitMul(Mul mul) { return 1 + Math.max(mul.left().accept(this), mul.right().accept(this)); }
        });
    }

    /** Uproszczenie z dodatkową regułą zerowania: dziedziczymy po SimplifyVisitor i nadpisujemy tylko visitMul. */
    static class ZeroAwareSimplifier extends SimplifyVisitor {  // zero aware = świadomy zera
        @Override
        public Expr visitMul(Mul mul) {
            Expr left = mul.left().accept(this);
            Expr right = mul.right().accept(this);
            if ((left instanceof Num l && l.value() == 0) || (right instanceof Num r && r.value() == 0)) {
                return num(0);
            }
            return super.visitMul(new Mul(left, right));  // reszta reguł (stałe, mnożenie przez 1) — z klasy bazowej
        }
    }

    static String solution4(Expr expr) {
        return print(expr.accept(new ZeroAwareSimplifier()));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo wizytator ma osobną metodę na każdy typ węzła, a wybór metody nie może zależeć od typu zmiennej
     *      (przeciążenie wybiera kompilator po typie statycznym). Wewnątrz Add typ this jest znany dokładnie, więc
     *      wywołanie visitAdd(this) trafia we właściwą metodę.
     *   2. Dwa wybory w czasie działania: (1) accept — po typie węzła (polimorfizm), (2) visitXxx — po typie wizytatora
     *      (polimorfizm). Razem dają wybór kodu zależny od pary (typ węzła, typ operacji).
     *   3. "to jest Expr" — przeciążenie wybierane jest po typie zmiennej (Expr), a nie po obiekcie w środku (Num).
     *   4. 10 — wizytator ma pole count i użyty drugi raz zaczyna od 5 (sample() ma 5 węzłów: Add, Num, Mul, Num, Var).
     *   5. Tracimy sprawdzanie kompilatora: wizytatory, które zapomniały obsłużyć Neg, nadal się kompilują, a dla Neg
     *      zwrócą null/NPE w czasie działania. Wymuszanie obsługi nowego typu to właśnie zaleta interfejsu bez default.
     *   6. Wyrażenie zostanie zapisane bez operandów, np. "(+)" — rekurencja po dzieciach musi zostać wywołana ręcznie
     *      (child.accept(this)), wizytator nie robi tego sam.
     *   7. Gdy hierarchia jest Twoja i zamknięta, wystarczają krótkie metody statyczne, a nie potrzebujesz
     *      podwójnej dyspozycji ani zgodności ze starszą Javą; chcesz uniknąć boilerplate (accept i visitXxx).
     *   8. Przykład: Strategy — zaleta: wymienne algorytmy bez if-ów; wada: wiele klas dla prostych różnic.
     *      Singleton — zaleta: jedna współdzielona instancja; wada: globalny stan utrudnia testy.
     *      Observer — zaleta: luźne powiązanie nadawcy i odbiorców; wada: trudne do prześledzenia, wycieki pamięci
     *      przy zapomnianych słuchaczach. (Inne wzorce — analogicznie.)
     */
    // </editor-fold>
}
