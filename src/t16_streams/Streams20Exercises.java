package t16_streams;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Customer;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Order;
import helpers.model.OrderLine;
import helpers.model.OrderStatus;
import helpers.model.Product;
import helpers.model.Student;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Streams20 — duży zestaw ćwiczeń z całego rozdziału o streamach
 *        (exercise = ćwiczenie; boss = „szef” — zadanie końcowe łączące wiele tematów)
 *
 * W SKRÓCIE:
 *   23 zadania od łatwych do trudnych: tworzenie, filter/map/flatMap, sortowanie, operacje końcowe,
 *   reduce, liczby, kolektory, Optional, BigDecimal, lenistwo i efekty uboczne.
 *   Są trzy zadania „PRZEPISZ PĘTLĘ NA STREAM” i trzy zadania BOSS na koniec.
 *
 * ANALOGIA: Trening na siłowni po kursie techniki. Znasz już każde ćwiczenie osobno,
 *   teraz robisz pełny obwód: po kolei, coraz ciężej, a na końcu „seria bossa”.
 *   Mięśnie (i pamięć) rosną od powtórzeń, nie od oglądania filmów.
 *
 * JAK TO DZIAŁA:
 *   1. Uruchom plik — każde zadanie pokazuje ✘ (jeszcze nie zrobione).
 *   2. Znajdź metodę exerciseN, przeczytaj treść i podpowiedź, zastąp TODO swoim kodem.
 *   3. Uruchom ponownie — ✔ oznacza poprawny wynik. Na dole jest PODSUMOWANIE.
 *   4. Dopiero potem porównaj z solutionN w zwiniętym bloku (styl, krótsze wersje).
 *
 *   Mapa zadań → lekcje:
 *     A (1–5)   tworzenie, filter, map, sorted, distinct, flatMap  → Streams02–Streams05
 *     B (6–10)  anyMatch/allMatch, sum, reduce, average, count      → Streams06–Streams08
 *     C (11–15) joining, toMap, groupingBy, partitioningBy, teeing  → Streams09–Streams13
 *     D (16–18) Optional, BigDecimal, max z komparatorem            → Streams14–Streams15
 *     E (19–20) lenistwo (limit), naprawa efektów ubocznych         → Streams16–Streams17
 *     F (21–23) BOSS — wszystko naraz
 *
 * SŁÓWKA:
 *   exercise = ćwiczenie; solution = rozwiązanie; hint = podpowiedź; stub = zaślepka (metoda do uzupełnienia);
 *   expected = oczekiwany; actual = rzeczywisty; boss = zadanie końcowe; report = raport.
 *
 * ZOBACZ TEŻ: t16_streams/Streams19Recipes (gotowe przepisy — ściąga do tych zadań),
 *   t16_streams/Streams13AdvancedCollectors (teeing, filtering, flatMapping),
 *   t16_streams/Streams15BigDecimalMoney (pieniądze), t16_streams/Streams17SideEffectsPitfalls (pułapki).
 * </pre>
 */
public class Streams20Exercises {

    /** Stałe poza lambdami (BigDecimal i daty tworzymy raz). */
    private static final BigDecimal LOW_PRICE = new BigDecimal("100");
    private static final LocalDate YEAR_2020 = LocalDate.of(2020, 1, 1);

    public static void main(String[] args) {
        title("Streams20 — ćwiczenia z całego rozdziału");

        howToWork();   // how to work = jak pracować
        exercises();   // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. JAK PRACOWAĆ Z TYM PLIKIEM
    // =================================================================================================

    /**
     * 1. Przykład rozwiązanego zadania i szybkie przypomnienie dwóch zasad, które najczęściej psują wyniki.
     */
    static void howToWork() {
        section("1. Jak pracować z tym plikiem");

        // ĆWICZENIE 0 (przykład rozwiązany): imiona klientów VIP.
        List<String> vipNames = SampleData.customers().stream()
                .filter(Customer::vip)           // vip = czy klient VIP
                .map(Customer::name)
                .toList();                       // toList (Java 16+) = do listy (niemodyfikowalnej)
        show("ćw. 0 (przykład): klienci VIP", vipNames);
        // WYNIK: ćw. 0 (przykład): klienci VIP → [Jan Kowalski, Zofia Krawczyk]

        // Check.equal(opis, oczekiwane, rzeczywiste) — porównuje i drukuje ✔ albo ✘.
        Check.equal("ćw. 0 (przykład)", List.of("Jan Kowalski", "Zofia Krawczyk"), vipNames);
        // WYNIK: ✔ OK    ćw. 0 (przykład)
        Check.summary(); // summary = podsumowanie (i wyzerowanie liczników)
        // WYNIK: PODSUMOWANIE: ✔ 1 OK, ✘ 0 BŁĄD

        // PUŁAPKA: stream jest jednorazowy. Zapisany w zmiennej i użyty drugi raz → wyjątek.
        Stream<String> once = SampleData.words().stream();
        show("liczba słów", once.count());
        // WYNIK: liczba słów → 12
        expectThrows("drugie użycie tego samego streamu", once::count);
        // WYNIK: ✔ drugie użycie tego samego streamu → rzucono IllegalStateException: stream has already been operated upon or closed

        // PUŁAPKA: stub rzucający UnsupportedOperationException("TODO") pokaże ✘ z wyjątkiem — to celowe.
        // Zadanie z wynikiem true/false mogłoby „przypadkiem” przejść z pustą zaślepką.

        // DOBRA PRAKTYKA: pisz pipeline krok po kroku. Najpierw samo filter + toList() i show(...),
        // potem dokładaj map/sorted/collect. Łatwiej zobaczyć, który krok psuje wynik.
        // DOBRA PRAKTYKA: każda operacja w osobnej linii, lambdy krótkie, dłuższa logika → metoda z nazwą.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • wybór: filter → map → sorted(comparing(...).thenComparing(...)) → limit → toList()
     *   • spłaszczanie: flatMap(x -> x.lista().stream()); liczby: mapToInt(...).sum()/average()
     *   • pytania tak/nie: anyMatch / allMatch / noneMatch (krótkie spięcie — kończą wcześniej)
     *   • reduce: tożsamość neutralna; BigDecimal: reduce(BigDecimal.ZERO, BigDecimal::add), compareTo
     *   • kolektory: joining, toMap(k, v, merge, TreeMap::new), groupingBy(k, TreeMap::new, downstream),
     *     partitioningBy, filtering / mapping / flatMapping, collectingAndThen, teeing (Java 12+)
     *   • Optional: map / flatMap / orElse — nigdy get() bez sprawdzenia
     *   • klucze-enumy → TreeMap::new albo EnumMap; wynik bez efektów ubocznych (żadnego add w forEach)
     *   • pętla z break → filter + limit / findFirst (stream jest leniwy)
     *
     * PYTANIA KONTROLNE:
     *   1. Co wypisze:  System.out.println(Stream.of(3, 1, 2).peek(System.out::print).count());  ?
     *   2. ZNAJDŹ BŁĄD:
     *        Map<Category, Product> byCategory = products.stream()
     *              .collect(Collectors.toMap(Product::category, p -> p));
     *   3. Co wypisze:  System.out.println(IntStream.rangeClosed(1, 4).reduce(1, (a, b) -> a * b));  ?
     *   4. ZNAJDŹ BŁĄD:
     *        BigDecimal sum = orders.stream().map(Order::total).reduce(BigDecimal.ZERO, BigDecimal::add);
     *        if (sum.equals(new BigDecimal("163"))) { ... }
     *   5. Co wypisze:  System.out.println(Optional.of("x").map(s -> null).isPresent());  ?
     *   6. Czym różni się filter(...) PRZED groupingBy od Collectors.filtering(...) WEWNĄTRZ groupingBy?
     *   7. Kiedy świadomie wybierzesz zwykłą pętlę zamiast streamu?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        List<Product> p = SampleData.products();
        List<Employee> e = SampleData.employees();
        List<Customer> c = SampleData.customers();
        List<Order> o = SampleData.orders();
        List<Student> s = SampleData.students();
        List<String> w = SampleData.words();

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        note("A. Podstawy: tworzenie, filter, map, sorted, flatMap");
        Check.equal("ćw. 1: książki", books(), () -> exercise1(p));
        Check.equal("ćw. 2: słowa unikalne WIELKIMI", upperWords(), () -> exercise2(w));
        Check.equal("ćw. 3: potęgi dwójki", List.of(1, 2, 4, 8, 16), () -> exercise3(5));
        Check.equal("ćw. 4: umiejętności działu IT", itSkills(), () -> exercise4(e));
        Check.equal("ćw. 5: trzech najmłodszych", youngest(), () -> exercise5(e));
        note("B. Operacje końcowe, reduce, liczby");
        Check.equal("ćw. 6: allMatch/anyMatch/noneMatch", List.of(false, true, true), () -> exercise6(s));
        Check.equal("ćw. 7: sztuki tanich produktów", 518, () -> exercise7(p));
        Check.equal("ćw. 8: najdłuższe słowo", "kolekcja", () -> exercise8(w));
        Check.equal("ćw. 9: średnia wszystkich ocen", "3.86", () -> exercise9(s));
        Check.equal("ćw. 10: zamówienia NOWE lub OPLACONE", 4L, () -> exercise10(o));
        note("C. Kolektory");
        Check.equal("ćw. 11: klienci z Krakowa", "Maria Nowak; Marek Król", () -> exercise11(c));
        Check.equal("ćw. 12: stany elektroniki", electronicsStock(), () -> exercise12(p));
        Check.equal("ćw. 13: pensje > 9000 w dziale", richPerDept(), () -> exercise13(e));
        Check.equal("ćw. 14: studenci ze średnią >= 4", goodStudents(), () -> exercise14(s));
        Check.equal("ćw. 15: najtańszy .. najdroższy", "7.49 .. 5499.99", () -> exercise15(p));
        note("D. Optional i BigDecimal");
        Check.equal("ćw. 16: e-mail klienta 3, 2, 99", emails(),
                () -> List.of(exercise16(c, 3), exercise16(c, 2), exercise16(c, 99)));
        Check.equal("ćw. 17: suma DOSTARCZONYCH", new BigDecimal("9196.66"), () -> exercise17(o));
        Check.equal("ćw. 18: najdroższa pozycja zamówienia", "Laptop Pro 14 x1", () -> exercise18(o));
        note("E. Lenistwo i efekty uboczne");
        Check.equal("ćw. 19: pierwsze 3 z zapasem > 10", firstWellStocked(), () -> exercise19(p));
        Check.equal("ćw. 20: słowa według długości", wordsByLength(), () -> exercise20(w));
        note("F. BOSS");
        Check.equal("ćw. 21: raport kategorii", categoryReport(), () -> exercise21(p));
        Check.equal("ćw. 22: najlepszy klient w mieście", bestPerCity(), () -> exercise22(o));
        Check.equal("ćw. 23: top 3 umiejętności weteranów", List.of("Excel=3", "CRM=2", "Java=2"), () -> exercise23(e));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", books(), () -> solution1(p));
        Check.equal("ćw. 2 (wzorzec)", upperWords(), () -> solution2(w));
        Check.equal("ćw. 3 (wzorzec)", List.of(1, 2, 4, 8, 16), () -> solution3(5));
        Check.equal("ćw. 4 (wzorzec)", itSkills(), () -> solution4(e));
        Check.equal("ćw. 5 (wzorzec)", youngest(), () -> solution5(e));
        Check.equal("ćw. 6 (wzorzec)", List.of(false, true, true), () -> solution6(s));
        Check.equal("ćw. 7 (wzorzec)", 518, () -> solution7(p));
        Check.equal("ćw. 8 (wzorzec)", "kolekcja", () -> solution8(w));
        Check.equal("ćw. 9 (wzorzec)", "3.86", () -> solution9(s));
        Check.equal("ćw. 10 (wzorzec)", 4L, () -> solution10(o));
        Check.equal("ćw. 11 (wzorzec)", "Maria Nowak; Marek Król", () -> solution11(c));
        Check.equal("ćw. 12 (wzorzec)", electronicsStock(), () -> solution12(p));
        Check.equal("ćw. 13 (wzorzec)", richPerDept(), () -> solution13(e));
        Check.equal("ćw. 14 (wzorzec)", goodStudents(), () -> solution14(s));
        Check.equal("ćw. 15 (wzorzec)", "7.49 .. 5499.99", () -> solution15(p));
        Check.equal("ćw. 16 (wzorzec)", emails(), () -> List.of(solution16(c, 3), solution16(c, 2), solution16(c, 99)));
        Check.equal("ćw. 17 (wzorzec)", new BigDecimal("9196.66"), () -> solution17(o));
        Check.equal("ćw. 18 (wzorzec)", "Laptop Pro 14 x1", () -> solution18(o));
        Check.equal("ćw. 19 (wzorzec)", firstWellStocked(), () -> solution19(p));
        Check.equal("ćw. 20 (wzorzec)", wordsByLength(), () -> solution20(w));
        Check.equal("ćw. 21 (wzorzec)", categoryReport(), () -> solution21(p));
        Check.equal("ćw. 22 (wzorzec)", bestPerCity(), () -> solution22(o));
        Check.equal("ćw. 23 (wzorzec)", List.of("Excel=3", "CRM=2", "Java=2"), () -> solution23(e));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 23 OK, ✘ 0 BŁĄD
    }

    // ----- oczekiwane wyniki (mapy budujemy przez TreeMap/EnumMap — stała kolejność wydruku) -----

    static List<String> books() {
        return List.of("Czysty kod", "Java. Podstawy", "Wzorce projektowe");
    }

    static List<String> upperWords() {
        return List.of("ENUM", "JAVA", "KOLEKCJA", "LAMBDA", "LISTA", "MAPA", "OPTIONAL", "REKORD", "STREAM");
    }

    static List<String> itSkills() {
        return List.of("AWS", "Docker", "Java", "Kotlin", "Python", "SQL", "Spring");
    }

    static List<String> youngest() {
        return List.of("Piotr Kowalski", "Agnieszka Zielińska", "Ewa Woźniak");
    }

    static Map<String, Integer> electronicsStock() {
        Map<String, Integer> m = new TreeMap<>();
        m.put("ELE-001", 7);
        m.put("ELE-002", 0);
        m.put("ELE-003", 25);
        m.put("ELE-004", 4);
        return m;
    }

    static Map<Department, Long> richPerDept() {
        Map<Department, Long> m = new EnumMap<>(Department.class);
        m.put(Department.IT, 4L);
        m.put(Department.HR, 0L);
        m.put(Department.SPRZEDAZ, 1L);
        m.put(Department.KSIEGOWOSC, 0L);
        m.put(Department.MARKETING, 1L);
        return m;
    }

    static Map<Boolean, List<String>> goodStudents() {
        Map<Boolean, List<String>> m = new TreeMap<>();
        m.put(false, List.of("Bartek", "Darek", "Filip", "Henryk"));
        m.put(true, List.of("Ala", "Celina", "Ela", "Gosia"));
        return m;
    }

    static List<String> emails() {
        return List.of("adam.mazur@example.com", "(brak)", "(brak)");
    }

    static List<String> firstWellStocked() {
        return List.of("Słuchawki BT", "Kawa ziarnista 1kg", "Czekolada gorzka");
    }

    static Map<Integer, Long> wordsByLength() {
        Map<Integer, Long> m = new TreeMap<>();
        m.put(4, 5L);
        m.put(5, 1L);
        m.put(6, 4L);
        m.put(8, 2L);
        return m;
    }

    static List<String> categoryReport() {
        return List.of(
                "Elektronika | produkty: 4 | sztuki: 36 | wartość: 52443.43",
                "Spożywcze | produkty: 3 | sztuki: 420 | wartość: 10045.80",
                "Książki | produkty: 3 | sztuki: 27 | wartość: 2643.00",
                "Odzież | produkty: 2 | sztuki: 92 | wartość: 9507.20",
                "Dom i ogród | produkty: 2 | sztuki: 20 | wartość: 6120.00");
    }

    static Map<String, String> bestPerCity() {
        Map<String, String> m = new TreeMap<>();
        m.put("Kraków", "Maria Nowak (619.77)");
        m.put("Poznań", "Ola Pawlak (608.97)");
        m.put("Warszawa", "Jan Kowalski (6669.79)");
        return m;
    }

    // ----- A. PODSTAWY -----

    /**
     * ĆWICZENIE 1 (łatwe): nazwy produktów z kategorii KSIAZKI, w kolejności z listy.
     * Podpowiedź: filter(p -> p.category() == Category.KSIAZKI) → map(Product::name) → toList().
     */
    static List<String> exercise1(List<Product> products) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (łatwe): słowa bez powtórzeń, posortowane alfabetycznie i zamienione na WIELKIE litery.
     * Podpowiedź: distinct() → sorted() → map(x -> x.toUpperCase(Locale.ROOT)) — zawsze podawaj Locale.
     */
    static List<String> exercise2(List<String> words) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (łatwe): pierwsze {@code count} potęg dwójki: 1, 2, 4, 8, ...
     * Podpowiedź: Stream.iterate(1, x -> x * 2) jest nieskończony — koniecznie limit(count).
     */
    static List<Integer> exercise3(int count) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 4 (średnie): wszystkie umiejętności pracowników działu IT — bez powtórzeń, sorted() naturalnie.
     * Podpowiedź: filter(dział) → flatMap(x -> x.skills().stream()) → distinct() → sorted().
     * Zauważ: „SQL” wypada przed „Spring” (wielka litera Q ma mniejszy kod niż mała p).
     */
    static List<String> exercise4(List<Employee> employees) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 5 (średnie): imiona i nazwiska trzech najmłodszych pracowników (przy równym wieku — alfabetycznie).
     * Podpowiedź: sorted(Comparator.comparingInt(Employee::age).thenComparing(Employee::name)) → limit(3).
     */
    static List<String> exercise5(List<Employee> employees) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // ----- B. OPERACJE KOŃCOWE, REDUCE, LICZBY -----

    /**
     * ĆWICZENIE 6 (łatwe): zwróć listę trzech odpowiedzi [a, b, c]:
     * a) czy WSZYSCY studenci mają jakieś oceny, b) czy JAKIŚ student jest z Poznania,
     * c) czy ŻADEN student nie jest na roku wyższym niż 3.
     * Podpowiedź: allMatch = wszystkie pasują; anyMatch = którykolwiek; noneMatch = żaden.
     */
    static List<Boolean> exercise6(List<Student> students) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 7 (średnie): łączna liczba sztuk (stock) produktów tańszych niż 100 zł.
     * Podpowiedź: filter(p -> p.price().compareTo(LOW_PRICE) < 0) → mapToInt(Product::stock) → sum().
     */
    static int exercise7(List<Product> products) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 8 (średnie): najdłuższe słowo; przy remisie — to, które wystąpiło pierwsze. Użyj reduce.
     * Podpowiedź: reduce((a, b) -> b.length() > a.length() ? b : a) zwraca Optional → orElse("").
     */
    static String exercise8(List<String> words) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 9 (średnie): średnia WSZYSTKICH ocen wszystkich studentów jako tekst z 2 miejscami po kropce.
     * Podpowiedź: flatMapToInt(x -> x.grades().stream().mapToInt(Integer::intValue)) → average() →
     * orElse(0) → String.format(Locale.ROOT, "%.2f", ...).
     */
    static String exercise9(List<Student> students) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 10 (średnie): PRZEPISZ PĘTLĘ NA STREAM:
     * <pre>{@code
     * long count = 0;
     * for (Order o : orders) {
     *     if (o.status() == OrderStatus.NOWE || o.status() == OrderStatus.OPLACONE) {
     *         count++;
     *     }
     * }
     * return count;
     * }</pre>
     * Podpowiedź: filter(...) → count(). count() zwraca long, więc oczekiwany wynik to 4L.
     */
    static long exercise10(List<Order> orders) {
        // TODO: twoje rozwiązanie
        return 0L;
    }

    // ----- C. KOLEKTORY -----

    /**
     * ĆWICZENIE 11 (łatwe): imiona i nazwiska klientów z Krakowa połączone przez "; ".
     * Podpowiedź: Collectors.joining("; ").
     */
    static String exercise11(List<Customer> customers) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 12 (średnie): mapa sku → stan magazynu dla produktów z ELEKTRONIKI, posortowana po sku.
     * Podpowiedź: toMap(Product::sku, Product::stock, (a, b) -> a, TreeMap::new).
     */
    static Map<String, Integer> exercise12(List<Product> products) {
        // TODO: twoje rozwiązanie
        return new TreeMap<>();
    }

    /**
     * ĆWICZENIE 13 (średnie): ilu pracowników w każdym dziale zarabia więcej niż 9000 zł.
     * Działy z wynikiem 0 MAJĄ zostać w mapie (HR=0, KSIEGOWOSC=0). Klucze posortowane (enum).
     * Podpowiedź: groupingBy(Employee::department, TreeMap::new, filtering(x -> ..., counting())).
     * filtering (Java 9+) = filtruj wewnątrz grupy; zwykły filter przed groupingBy usunąłby puste działy.
     */
    static Map<Department, Long> exercise13(List<Employee> employees) {
        // TODO: twoje rozwiązanie
        return new TreeMap<>();
    }

    /**
     * ĆWICZENIE 14 (średnie): podziel studentów na tych ze średnią >= 4.0 (true) i resztę (false) — same imiona.
     * Student bez ocen trafia do false.
     * Podpowiedź: partitioningBy(x -> x.averageGrade().orElse(0) >= 4.0, mapping(Student::name, toList())).
     */
    static Map<Boolean, List<String>> exercise14(List<Student> students) {
        // TODO: twoje rozwiązanie
        return new TreeMap<>();
    }

    /**
     * ĆWICZENIE 15 (trudniejsze): w JEDNYM przejściu znajdź cenę najtańszego i najdroższego produktu,
     * wynik jako tekst „min .. max”.
     * Podpowiedź: Collectors.teeing (Java 12+) = rozdziel na dwa kolektory i połącz wyniki:
     * teeing(minBy(comparing(Product::price)), maxBy(comparing(Product::price)), (min, max) -> ...).
     */
    static String exercise15(List<Product> products) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // ----- D. OPTIONAL I BIGDECIMAL -----

    /**
     * ĆWICZENIE 16 (łatwe): e-mail klienta o podanym id albo "(brak)" — gdy nie ma klienta LUB nie ma e-maila.
     * Podpowiedź: filter(id) → findFirst() → flatMap(Customer::findEmail) → orElse("(brak)").
     * Optional.flatMap, bo findEmail() sam zwraca Optional (inaczej wyszłoby Optional w Optional).
     */
    static String exercise16(List<Customer> customers, long id) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 17 (średnie): suma wartości zamówień o statusie DOSTARCZONE (BigDecimal).
     * Podpowiedź: map(Order::total) → reduce(BigDecimal.ZERO, BigDecimal::add). Nie używaj double!
     */
    static BigDecimal exercise17(List<Order> orders) {
        // TODO: twoje rozwiązanie
        return BigDecimal.ZERO;
    }

    /**
     * ĆWICZENIE 18 (średnie): PRZEPISZ PĘTLĘ NA STREAM — najdroższa pozycja (OrderLine) ze wszystkich zamówień:
     * <pre>{@code
     * OrderLine best = null;
     * for (Order o : orders) {
     *     for (OrderLine line : o.lines()) {
     *         if (best == null || line.total().compareTo(best.total()) > 0) {
     *             best = line;
     *         }
     *     }
     * }
     * return best == null ? "(brak)" : best.toString();
     * }</pre>
     * Podpowiedź: flatMap(o -> o.lines().stream()) → max(Comparator.comparing(OrderLine::total)) → map → orElse.
     */
    static String exercise18(List<Order> orders) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // ----- E. LENISTWO I EFEKTY UBOCZNE -----

    /**
     * ĆWICZENIE 19 (średnie): PRZEPISZ PĘTLĘ NA STREAM — nazwy pierwszych 3 produktów ze stanem > 10:
     * <pre>{@code
     * List<String> result = new ArrayList<>();
     * for (Product p : products) {
     *     if (p.stock() > 10) {
     *         result.add(p.name());
     *         if (result.size() == 3) {
     *             break;
     *         }
     *     }
     * }
     * return result;
     * }</pre>
     * Podpowiedź: break zastępuje limit(3) — stream jest leniwy i przestanie czytać po 3 trafieniach
     * (t16_streams/Streams16Laziness).
     */
    static List<String> exercise19(List<Product> products) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 20 (średnie): NAPRAW kod z efektem ubocznym — liczba słów według długości (klucze rosnąco):
     * <pre>{@code
     * Map<Integer, Long> result = new HashMap<>();
     * words.stream().forEach(x -> result.merge(x.length(), 1L, Long::sum));  // efekt uboczny!
     * return result;
     * }</pre>
     * Podpowiedź: groupingBy(String::length, TreeMap::new, counting()) — wynik powstaje w collect,
     * więc kod zadziała poprawnie także po dodaniu parallel().
     */
    static Map<Integer, Long> exercise20(List<String> words) {
        // TODO: twoje rozwiązanie
        return new TreeMap<>();
    }

    // ----- F. BOSS -----

    /**
     * ĆWICZENIE 21 (trudniejsze, BOSS 1): raport kategorii w kolejności enuma. Dla każdej kategorii jedna linia:
     * {@code "Elektronika | produkty: 4 | sztuki: 36 | wartość: 52443.43"} — nazwa z getDisplayName(),
     * liczba produktów, suma sztuk i suma stockValue() (BigDecimal).
     * Podpowiedź: groupingBy(Product::category, () -> new EnumMap(...), toList()) → entrySet().stream() →
     * map(wpis → tekst). W środku: mapToInt(...).sum() oraz map(Product::stockValue).reduce(ZERO, add).
     */
    static List<String> exercise21(List<Product> products) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 22 (trudniejsze, BOSS 2): najlepszy klient w każdym mieście (TreeMap: miasto → „Imię Nazwisko (suma)”),
     * licząc tylko zamówienia NIE anulowane. Miasto bez takich zamówień (Gdańsk) nie pojawia się w wyniku.
     * Podpowiedź: krok 1: groupingBy(Order::customer, reducing(ZERO, Order::total, BigDecimal::add)).
     * Krok 2: entrySet().stream() → groupingBy(miasto klienta, TreeMap::new,
     * collectingAndThen(maxBy(Map.Entry.comparingByValue()), opt -> tekst)).
     */
    static Map<String, String> exercise22(List<Order> orders) {
        // TODO: twoje rozwiązanie
        return new TreeMap<>();
    }

    /**
     * ĆWICZENIE 23 (trudniejsze, BOSS 3): wśród pracowników zatrudnionych przed 2020-01-01 i zarabiających > 8000
     * znajdź 3 najczęstsze umiejętności w formacie "nazwa=liczba" (liczba malejąco, przy remisie alfabetycznie).
     * Podpowiedź: filter → filter → flatMap(skills) → groupingBy(x -> x, counting()) → entrySet().stream() →
     * sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey())) → limit(3).
     */
    static List<String> exercise23(List<Employee> employees) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(List<Product> products) {
        return products.stream().filter(x -> x.category() == Category.KSIAZKI).map(Product::name).toList();
    }

    static List<String> solution2(List<String> words) {
        return words.stream().distinct().sorted().map(x -> x.toUpperCase(Locale.ROOT)).toList();
    }

    static List<Integer> solution3(int count) {
        return Stream.iterate(1, x -> x * 2).limit(count).toList();
    }

    static List<String> solution4(List<Employee> employees) {
        return employees.stream()
                .filter(x -> x.department() == Department.IT)
                .flatMap(x -> x.skills().stream())
                .distinct()
                .sorted()
                .toList();
    }

    static List<String> solution5(List<Employee> employees) {
        return employees.stream()
                .sorted(Comparator.comparingInt(Employee::age).thenComparing(Employee::name))
                .limit(3)
                .map(Employee::name)
                .toList();
    }

    static List<Boolean> solution6(List<Student> students) {
        boolean allHaveGrades = students.stream().allMatch(x -> !x.grades().isEmpty());
        boolean anyFromPoznan = students.stream().anyMatch(x -> x.city().equals("Poznań"));
        boolean noneAboveThird = students.stream().noneMatch(x -> x.year() > 3);
        return List.of(allHaveGrades, anyFromPoznan, noneAboveThird);
    }

    static int solution7(List<Product> products) {
        return products.stream()
                .filter(x -> x.price().compareTo(LOW_PRICE) < 0)
                .mapToInt(Product::stock)
                .sum();
    }

    static String solution8(List<String> words) {
        return words.stream().reduce((a, b) -> b.length() > a.length() ? b : a).orElse("");
    }

    static String solution9(List<Student> students) {
        double avg = students.stream()
                .flatMapToInt(x -> x.grades().stream().mapToInt(Integer::intValue))
                .average()
                .orElse(0);
        return String.format(Locale.ROOT, "%.2f", avg);
    }

    static long solution10(List<Order> orders) {
        return orders.stream()
                .filter(x -> x.status() == OrderStatus.NOWE || x.status() == OrderStatus.OPLACONE)
                .count();
    }

    static String solution11(List<Customer> customers) {
        return customers.stream()
                .filter(x -> x.city().equals("Kraków"))
                .map(Customer::name)
                .collect(Collectors.joining("; "));
    }

    static Map<String, Integer> solution12(List<Product> products) {
        return products.stream()
                .filter(x -> x.category() == Category.ELEKTRONIKA)
                .collect(Collectors.toMap(Product::sku, Product::stock, (a, b) -> a, TreeMap::new));
    }

    static Map<Department, Long> solution13(List<Employee> employees) {
        return employees.stream()
                .collect(Collectors.groupingBy(Employee::department, TreeMap::new,
                        Collectors.filtering(x -> x.salary() > 9000, Collectors.counting())));
    }

    static Map<Boolean, List<String>> solution14(List<Student> students) {
        return students.stream()
                .collect(Collectors.partitioningBy(x -> x.averageGrade().orElse(0) >= 4.0,
                        Collectors.mapping(Student::name, Collectors.toList())));
    }

    static String solution15(List<Product> products) {
        return products.stream().collect(Collectors.teeing(
                Collectors.minBy(Comparator.comparing(Product::price)),
                Collectors.maxBy(Comparator.comparing(Product::price)),
                (min, max) -> min.orElseThrow().price() + " .. " + max.orElseThrow().price()));
    }

    static String solution16(List<Customer> customers, long id) {
        return customers.stream()
                .filter(x -> x.id() == id)
                .findFirst()
                .flatMap(Customer::findEmail)
                .orElse("(brak)");
    }

    static BigDecimal solution17(List<Order> orders) {
        return orders.stream()
                .filter(x -> x.status() == OrderStatus.DOSTARCZONE)
                .map(Order::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static String solution18(List<Order> orders) {
        return orders.stream()
                .flatMap(x -> x.lines().stream())
                .max(Comparator.comparing(OrderLine::total))
                .map(OrderLine::toString)
                .orElse("(brak)");
    }

    static List<String> solution19(List<Product> products) {
        return products.stream().filter(x -> x.stock() > 10).limit(3).map(Product::name).toList();
    }

    static Map<Integer, Long> solution20(List<String> words) {
        return words.stream().collect(Collectors.groupingBy(String::length, TreeMap::new, Collectors.counting()));
    }

    static List<String> solution21(List<Product> products) {
        Map<Category, List<Product>> byCategory = products.stream()
                .collect(Collectors.groupingBy(Product::category, () -> new EnumMap<>(Category.class),
                        Collectors.toList()));
        return byCategory.entrySet().stream()
                .map(entry -> {
                    List<Product> list = entry.getValue();
                    int units = list.stream().mapToInt(Product::stock).sum();
                    BigDecimal value = list.stream().map(Product::stockValue).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return entry.getKey().getDisplayName() + " | produkty: " + list.size()
                            + " | sztuki: " + units + " | wartość: " + value;
                })
                .toList();
    }

    static Map<String, String> solution22(List<Order> orders) {
        Map<Customer, BigDecimal> perCustomer = orders.stream()
                .filter(x -> x.status() != OrderStatus.ANULOWANE)
                .collect(Collectors.groupingBy(Order::customer,
                        Collectors.reducing(BigDecimal.ZERO, Order::total, BigDecimal::add)));
        return perCustomer.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().city(), TreeMap::new,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Map.Entry.<Customer, BigDecimal>comparingByValue()),
                                opt -> opt.map(en -> en.getKey().name() + " (" + en.getValue() + ")").orElse("?"))));
    }

    static List<String> solution23(List<Employee> employees) {
        Map<String, Long> counts = employees.stream()
                .filter(x -> x.hireDate().isBefore(YEAR_2020))
                .filter(x -> x.salary() > 8000)
                .flatMap(x -> x.skills().stream())
                .collect(Collectors.groupingBy(x -> x, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(3)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toList();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Tylko „3”. Od Java 9 count() na źródle o znanym rozmiarze (Stream.of) nie musi przechodzić
     *      elementów, więc peek w ogóle się nie wykona. Dlatego peek nie służy do logiki (Streams17).
     *   2. Kilka produktów ma tę samą kategorię → IllegalStateException (Duplicate key).
     *      Chcesz listy w grupach → groupingBy(Product::category); chcesz jeden produkt → toMap z funkcją łączącą.
     *   3. 24 — iloczyn 1·2·3·4; tożsamość 1 jest neutralna dla mnożenia.
     *   4. equals w BigDecimal porównuje też skalę: 163.00 (skala 2) nie jest equals 163 (skala 0).
     *      Poprawnie: sum.compareTo(new BigDecimal("163")) == 0.
     *   5. false — map zwracający null daje pusty Optional (a nie wyjątek).
     *   6. filter PRZED groupingBy usuwa elementy, więc grupa bez pasujących elementów znika z mapy.
     *      filtering WEWNĄTRZ tworzy grupę dla każdego klucza i dopiero w niej filtruje → HR=0 zostaje.
     *   7. Gdy wynik zależy od poprzedniego elementu (suma bieżąca), gdy potrzebujesz indeksu i wielu
     *      zmiennych naraz, gdy obsługujesz wyjątki kontrolowane albo gdy pętla jest po prostu czytelniejsza.
     */
    // </editor-fold>
}
