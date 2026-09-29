package t09_records;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Employee;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Rekordy dla zaawansowanych — generyczne, lokalne, z interfejsem, jako klucze map, z instanceof ze wzorcem
 *        (generic = generyczny, z parametrem typu; local = lokalny; pattern matching = dopasowanie do wzorca)
 *
 * W SKRÓCIE:
 *   Rekord to pełnoprawny typ: może mieć parametry typu (Pair{@code <A, B>}), może być zadeklarowany LOKALNIE w metodzie
 *   (np. na pośredni wynik w streamie), może implementować interfejsy (Comparable, własne) i świetnie nadaje się
 *   na klucz mapy złożony z kilku wartości. Razem z interfejsami sealed (t07) i instanceof ze wzorcem (Java 16+)
 *   tworzy wygodny sposób opisywania danych „jedno z kilku” (kształt: koło ALBO prostokąt).
 *
 * ANALOGIA: pudełka z przegródkami.
 *   Pair to pudełko na dwie rzeczy dowolnego rodzaju. Rekord lokalny to jednorazowe pudełko, którego używasz tylko
 *   przy jednym zadaniu i nie trzymasz w szafie (poza metodą nikt go nie widzi).
 *
 * JAK TO DZIAŁA:
 *   record Pair{@code <A, B>}(A first, B second) {}           ← rekord generyczny
 *   void m() { record Row(String name, int n) {} ... }       ← rekord lokalny (tylko w tej metodzie)
 *   record Version(int major, int minor) implements Comparable{@code <Version>} { ... }
 *   if (shape instanceof Circle c) { c.radius() ... }        ← sprawdzenie typu + zmienna w jednym (Java 16+)
 *
 * SŁÓWKA:
 *   generic = generyczny; local = lokalny; pair = para; first = pierwszy; second = drugi; version = wersja;
 *   major = główny; minor = poboczny; cell = komórka; row = wiersz; column = kolumna; shape = kształt; circle = koło;
 *   rectangle = prostokąt; area = pole powierzchni; sealed = zapieczętowany; permits = zezwala; mask = zamaskuj.
 *
 * ZOBACZ TEŻ: t09_records/Records02Constructors (konstruktory), t11_generics/Generics02Classes (klasy generyczne),
 *             t07_inheritance_polymorphism/Inherit07SealedClasses (sealed), t12_collections/Collections07ComparableComparator,
 *             t23_modern_java/Modern05RecordsSealedPatterns (rekordy + sealed + wzorce, zapowiedź Javy 21).
 * </pre>
 */
public class Records03Advanced {

    /** Pair = para dwóch wartości dowolnych typów. A i B to parametry typu (t11_generics). */
    record Pair<A, B>(A first, B second) {
    }

    /**
     * Version = wersja programu, np. 1.10. Implementuje Comparable (porównywalny), żeby dało się ją sortować
     * „po liczbach”, a nie „po tekście”. Nadpisany toString daje zwięzły zapis "1.10".
     */
    record Version(int major, int minor) implements Comparable<Version> {

        private static final Comparator<Version> ORDER =
                Comparator.comparingInt(Version::major).thenComparingInt(Version::minor);

        /** parse = odczytaj z tekstu. "1.10" → Version[1, 10]. Kropka w regex to „dowolny znak”, więc \\. */
        static Version parse(String text) {
            String[] parts = text.split("\\.");
            return new Version(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }

        @Override
        public int compareTo(Version other) {       // compareTo = porównaj z
            return ORDER.compare(this, other);
        }

        @Override
        public String toString() {
            return major + "." + minor;
        }
    }

    /** Cell = komórka planszy (wiersz, kolumna). Idealny klucz mapy: equals/hashCode po wartościach za darmo. */
    record Cell(int row, int col) {
    }

    /** Shape = kształt. sealed (Java 17+) = tylko wymienione typy mogą go implementować (t07_inheritance_polymorphism). */
    sealed interface Shape permits Circle, Rectangle {
    }

    /** Circle = koło o promieniu radius. */
    record Circle(double radius) implements Shape {
    }

    /** Rectangle = prostokąt o bokach width × height. */
    record Rectangle(double width, double height) implements Shape {
    }

    /** Credentials = dane logowania. Nadpisany toString ukrywa hasło — ważne, bo toString trafia do logów. */
    record Credentials(String login, String password) {
        @Override
        public String toString() {
            return "Credentials[login=" + login + ", password=***]";
        }
    }

    public static void main(String[] args) {
        title("Records03 — rekordy dla zaawansowanych");

        genericRecord();        // generic record = rekord generyczny
        localRecord();          // local record = rekord lokalny
        comparableRecord();     // comparable record = rekord porównywalny
        recordAsMapKey();       // record as map key = rekord jako klucz mapy
        sealedAndInstanceof();  // sealed and instanceof = sealed i instanceof ze wzorcem
        hidingData();           // hiding data = ukrywanie danych w toString
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. REKORD GENERYCZNY
    // =================================================================================================

    /** 1. Pair{@code <A, B>} — jedna definicja, dowolne typy. Przydatne, gdy metoda ma zwrócić dwie wartości naraz. */
    static void genericRecord() {
        section("1. Rekord generyczny Pair<A, B>");

        Pair<String, Integer> nameAge = new Pair<>("Ala", 30);
        show("para", nameAge);
        show("second() + 1", nameAge.second() + 1);
        // WYNIK: para → Pair[first=Ala, second=30]
        // WYNIK: second() + 1 → 31    ← second() zwraca Integer — kompilator zna typ, rzutowanie niepotrzebne

        Pair<Integer, Integer> mm = minMax(List.of(7, 2, 9, 4));
        show("minMax(7, 2, 9, 4)", mm.first() + ".." + mm.second());
        // WYNIK: minMax(7, 2, 9, 4) → 2..9

        // DOBRA PRAKTYKA: Pair jest wygodny lokalnie, ale w publicznym API lepszy jest rekord z ZNACZĄCĄ nazwą,
        //   np. record MinMax(int min, int max) — first()/second() nic nie mówią czytelnikowi.
    }

    /** minMax = najmniejsza i największa wartość naraz — dwie wartości zwrócone w jednym rekordzie. */
    static Pair<Integer, Integer> minMax(List<Integer> numbers) {
        int min = numbers.get(0);
        int max = numbers.get(0);
        for (int n : numbers) {
            min = Math.min(min, n);
            max = Math.max(max, n);
        }
        return new Pair<>(min, max);
    }

    // =================================================================================================
    // 2. REKORD LOKALNY
    // =================================================================================================

    /**
     * 2. Rekord zadeklarowany w metodzie (Java 16+) — widoczny tylko tu. Świetny na pośrednie wyniki w streamie:
     * zamiast Object[] albo Map.Entry z niejasnymi getKey/getValue masz nazwane pola.
     */
    static void localRecord() {
        section("2. Rekord lokalny w metodzie");

        record NameSalary(String name, int salary) {     // lokalny: poza localRecord() nie istnieje
        }

        List<NameSalary> top3 = SampleData.employees().stream()
                .map(e -> new NameSalary(e.name(), e.salary()))
                .sorted(Comparator.comparingInt(NameSalary::salary).reversed())
                .limit(3)
                .collect(Collectors.toList());
        for (NameSalary ns : top3) {
            System.out.println(ns.name() + ": " + ns.salary());
        }
        // WYNIK: Michał Lewandowski: 17200
        // WYNIK: Anna Nowak: 14500
        // WYNIK: Ewa Woźniak: 12100

        // Rekordy (także lokalne i zagnieżdżone) są niejawnie static — nie widzą pól instancji klasy zewnętrznej.
    }

    // =================================================================================================
    // 3. REKORD Z INTERFEJSEM: Comparable
    // =================================================================================================

    /** 3. Sortowanie wersji jako tekstu daje „1.10” przed „1.2”. Rekord Comparable sortuje liczbowo. */
    static void comparableRecord() {
        section("3. Rekord implementujący Comparable");

        List<String> texts = new ArrayList<>(List.of("1.10", "1.2", "2.0", "1.9"));
        texts.sort(null);                                 // null = porządek naturalny (dla String: znak po znaku)
        show("jako tekst", texts);
        // WYNIK: jako tekst → [1.10, 1.2, 1.9, 2.0]    ← źle: '1' < '2', więc "1.10" przed "1.2"

        List<Version> versions = new ArrayList<>();
        for (String t : List.of("1.10", "1.2", "2.0", "1.9")) {
            versions.add(Version.parse(t));
        }
        versions.sort(null);                              // porządek naturalny = compareTo z rekordu
        show("jako Version", versions);
        // WYNIK: jako Version → [1.2, 1.9, 1.10, 2.0]

        // PUŁAPKA: gdy nadpisujesz compareTo, dbaj o zgodność z equals (compareTo == 0 ⇔ equals). Tu jest zgodne,
        //   bo porównujemy te same składniki, z których record buduje equals.
    }

    // =================================================================================================
    // 4. REKORD JAKO KLUCZ MAPY
    // =================================================================================================

    /** 4. Klucz złożony z kilku wartości — rekord zamiast sklejania napisów "1:2" czy map zagnieżdżonych. */
    static void recordAsMapKey() {
        section("4. Rekord jako klucz mapy");

        Map<Cell, Character> board = new HashMap<>();     // plansza kółko-krzyżyk: tylko zajęte pola
        board.put(new Cell(0, 0), 'X');
        board.put(new Cell(1, 1), 'O');
        board.put(new Cell(0, 0), 'O');                  // ten sam klucz (równy po wartościach) — nadpisanie

        show("board.get(new Cell(0, 0))", board.get(new Cell(0, 0)));
        show("zajęte pola", board.size());
        show("pole (2, 2) wolne?", !board.containsKey(new Cell(2, 2)));
        // WYNIK: board.get(new Cell(0, 0)) → O
        // WYNIK: zajęte pola → 2
        // WYNIK: pole (2, 2) wolne? → true

        // DOBRA PRAKTYKA: klucz mapy musi być niezmienny — gdyby zmienił się po włożeniu, zmieniłby się hashCode
        //   i mapa by go „zgubiła”. Rekordy (z niezmiennymi składnikami) spełniają to automatycznie.
    }

    // =================================================================================================
    // 5. sealed + instanceof ZE WZORCEM
    // =================================================================================================

    /** area = pole powierzchni. instanceof ze wzorcem (Java 16+): sprawdza typ i od razu tworzy zmienną c / r. */
    static double area(Shape shape) {
        if (shape instanceof Circle c) {
            return Math.PI * c.radius() * c.radius();
        } else if (shape instanceof Rectangle r) {
            return r.width() * r.height();
        }
        throw new IllegalArgumentException("Nieznany kształt: " + shape);
    }

    /**
     * 5. Interfejs sealed + rekordy = zamknięta lista wariantów danych. Java 21 pozwala pisać
     * switch (shape) { case Circle c -> ...; case Rectangle r -> ...; } — bez default, bo kompilator zna wszystkie warianty,
     * a nawet rozbierać rekord: case Circle(double radius) -> ... (record patterns, t23_modern_java).
     */
    static void sealedAndInstanceof() {
        section("5. sealed + rekordy + instanceof ze wzorcem");

        List<Shape> shapes = List.of(new Circle(1), new Rectangle(2, 3));
        for (Shape s : shapes) {
            System.out.println(String.format(java.util.Locale.ROOT, "%s → pole %.2f", s, area(s)));
        }
        // WYNIK: Circle[radius=1.0] → pole 3.14
        // WYNIK: Rectangle[width=2.0, height=3.0] → pole 6.00
    }

    // =================================================================================================
    // 6. UKRYWANIE DANYCH W toString
    // =================================================================================================

    /** 6. Domyślny toString rekordu wypisuje WSZYSTKO — także hasła i numery kart. Nadpisz go dla danych wrażliwych. */
    static void hidingData() {
        section("6. Dane wrażliwe: nadpisany toString");

        Credentials c = new Credentials("jan", "Tajne123!");
        show("toString", c);
        show("password() nadal działa", c.password().length());
        // WYNIK: toString → Credentials[login=jan, password=***]
        // WYNIK: password() nadal działa → 9

        // PUŁAPKA: hasło w toString trafia do logów, wyjątków i debuggera. Rekord z danymi wrażliwymi = nadpisany toString.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • record Pair<A, B>(A first, B second) {} — rekord generyczny; w API lepsza nazwa znacząca (MinMax, NameAge).
     *   • Rekord lokalny w metodzie (Java 16+) — na pośrednie wyniki (np. w streamie); niejawnie static.
     *   • Rekord może implementować interfejsy: Comparable (sortowanie), własne (Shape).
     *   • Rekord = dobry klucz mapy: equals/hashCode po wartościach, niezmienny.
     *   • sealed interface + rekordy = zamknięta lista wariantów; instanceof Typ zmienna (Java 16+).
     *   • Dane wrażliwe → nadpisz toString (domyślny wypisuje wszystkie składniki).
     *
     * PYTANIA KONTROLNE:
     *   1. Po co rekord lokalny, skoro można użyć Map.Entry albo tablicy Object[]?
     *   2. Co wypisze:  System.out.println(new Pair<>("x", List.of(1, 2)));  ?
     *   3. ZNAJDŹ BŁĄD:  List<String> v = new ArrayList<>(List.of("1.10", "1.9")); v.sort(null);
     *                    System.out.println("najnowsza: " + v.get(v.size() - 1));   // oczekiwano 1.10
     *   4. Dlaczego klucz mapy powinien być niezmienny?
     *   5. Co daje połączenie sealed interface z rekordami?
     *   6. Co wypisze:  Map<Cell, String> m = new HashMap<>(); m.put(new Cell(1, 2), "a"); System.out.println(m.get(new Cell(1, 2)));  ?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: zamiana w parze", new Pair<>(5, "pięć"), () -> exercise1(new Pair<>("pięć", 5)));
        Check.equal("ćw. 2: najnowsza wersja", "1.12", () -> exercise2(List.of("1.9", "1.12", "0.99", "1.2")));
        Check.equal("ćw. 3: suma pól prostokątów", 26.0,
                () -> exercise3(List.of(new Rectangle(2, 3), new Circle(5), new Rectangle(4, 5))));
        Check.equal("ćw. 4: liczba pracowników z pensją ≥ 10000 w działach", "{IT=3, SPRZEDAZ=1}",
                () -> exercise4(SampleData.employees()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", new Pair<>(5, "pięć"), () -> solution1(new Pair<>("pięć", 5)));
        Check.equal("ćw. 2 (wzorzec)", "1.12", () -> solution2(List.of("1.9", "1.12", "0.99", "1.2")));
        Check.equal("ćw. 3 (wzorzec)", 26.0,
                () -> solution3(List.of(new Rectangle(2, 3), new Circle(5), new Rectangle(4, 5))));
        Check.equal("ćw. 4 (wzorzec)", "{IT=3, SPRZEDAZ=1}", () -> solution4(SampleData.employees()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): zwróć nową parę z zamienionymi elementami: (a, b) → (b, a). */
    static <A, B> Pair<B, A> exercise1(Pair<A, B> pair) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć NAJNOWSZĄ wersję z listy tekstów jako tekst (np. "1.12").
     * Podpowiedź: Version.parse dla każdego, potem największa według compareTo (pętla albo Collections.max).
     */
    static String exercise2(List<String> texts) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 3 (średnie): zsumuj pola powierzchni TYLKO prostokątów (koła pomiń).
     * Podpowiedź: if (s instanceof Rectangle r) sum += r.width() * r.height();
     */
    static double exercise3(List<Shape> shapes) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze, łączy z t16_streams): policz pracowników z pensją ≥ 10000 w każdym dziale.
     * Zwróć toString mapy posortowanej po nazwie działu (TreeMap), np. "{IT=3, SPRZEDAZ=1}".
     * Podpowiedź: filter + groupingBy(e -> e.department().name(), TreeMap::new, counting()) — klucz jako String
     * sortuje się alfabetycznie. (Możesz użyć lokalnego rekordu, ale nie musisz.)
     */
    static String exercise4(List<Employee> employees) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static <A, B> Pair<B, A> solution1(Pair<A, B> pair) {
        return new Pair<>(pair.second(), pair.first());
    }

    static String solution2(List<String> texts) {
        Version newest = null;
        for (String t : texts) {
            Version v = Version.parse(t);
            if (newest == null || v.compareTo(newest) > 0) {
                newest = v;
            }
        }
        return String.valueOf(newest);
    }

    static double solution3(List<Shape> shapes) {
        double sum = 0;
        for (Shape s : shapes) {
            if (s instanceof Rectangle r) {
                sum += r.width() * r.height();
            }
        }
        return sum;
    }

    static String solution4(List<Employee> employees) {
        return employees.stream()
                .filter(e -> e.salary() >= 10_000)
                .collect(Collectors.groupingBy(e -> e.department().name(), java.util.TreeMap::new, Collectors.counting()))
                .toString();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Rekord ma NAZWANE pola z typami (name(), salary()) — kod czyta się jak zdanie, a kompilator pilnuje typów.
     *      Map.Entry ma nic niemówiące getKey/getValue, a Object[] wymaga rzutowania i indeksów.
     *   2. „Pair[first=x, second=[1, 2]]”.
     *   3. Sortowanie tekstów porównuje znak po znaku: "1.10" < "1.9" (bo '1' < '9'), więc wypisze „najnowsza: 1.9”.
     *      Trzeba sortować liczbowo — np. rekordem Version implements Comparable.
     *   4. HashMap szuka klucza po hashCode. Gdy klucz zmieni się po włożeniu, jego hashCode się zmienia
     *      i mapa szuka w złym miejscu — wpis „ginie”, choć nadal zajmuje pamięć.
     *   5. Zamkniętą, znaną kompilatorowi listę wariantów danych (np. Circle albo Rectangle). W Javie 21 switch
     *      po takim typie nie wymaga default i kompilator ostrzeże o nieobsłużonym wariancie.
     *   6. „a” — nowy Cell(1, 2) jest równy (equals/hashCode) temu użytemu przy put.
     */
    // </editor-fold>
}
