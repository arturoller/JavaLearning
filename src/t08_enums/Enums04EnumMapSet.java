package t08_enums;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Department;
import helpers.model.Employee;
import helpers.model.Order;
import helpers.model.OrderStatus;
import helpers.model.Product;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: EnumSet i EnumMap — kolekcje dla enumów; maszyna stanów
 *        (EnumSet = zbiór stałych enuma; EnumMap = mapa z kluczami-enumami; state machine = maszyna stanów)
 *
 * W SKRÓCIE:
 *   Gdy elementy zbioru albo klucze mapy to stałe enuma — użyj EnumSet / EnumMap zamiast HashSet / HashMap.
 *   Są szybsze i zajmują mniej pamięci (w środku to tablica / ciąg bitów indeksowany ordinal()), a przede wszystkim
 *   ZAWSZE iterują w kolejności deklaracji stałych — wydruk jest przewidywalny.
 *   Połączenie EnumMap + EnumSet świetnie opisuje MASZYNĘ STANÓW: „z jakiego stanu do jakich wolno przejść”.
 *
 * ANALOGIA: szafka z przegródkami.
 *   HashMap to worek, do którego wrzucasz karteczki — kolejność wyjmowania jest przypadkowa. EnumMap to szafka
 *   z przegródkami podpisanymi kolejnymi stałymi: każda stała ma swoją przegródkę, zawsze w tym samym miejscu.
 *
 * JAK TO DZIAŁA:
 *   Set{@code <DayOfWeek>} weekend = EnumSet.of(SATURDAY, SUNDAY);
 *   Set{@code <DayOfWeek>} work    = EnumSet.complementOf(weekend);      ← dopełnienie: wszystkie POZA weekendem
 *   Map{@code <Department, Integer>} m = new EnumMap<>(Department.class);  ← trzeba podać klasę enuma
 *   Maszyna stanów: {@code Map<OrderStatus, Set<OrderStatus>>} ALLOWED — dla każdego stanu zbiór dozwolonych następnych.
 *
 * SŁÓWKA:
 *   of = z (podanych elementów); range = zakres; all of = wszystkie z; none of = żaden z (pusty zbiór);
 *   complement of = dopełnienie; copy of = kopia; state = stan; transition = przejście; allowed = dozwolony;
 *   move to = przejdź do; can = czy można; traffic light = sygnalizacja świetlna; next = następny.
 *
 * ZOBACZ TEŻ: t08_enums/Enums01Basics (values, ordinal), t12_collections/Collections04Sets (zbiory),
 *             t12_collections/Collections05Maps (mapy), t16_streams/Streams11GroupingBy (groupingBy do EnumMap),
 *             t22_design_patterns/Patterns01Strategy (zachowanie zależne od stanu).
 * </pre>
 */
public class Enums04EnumMapSet {

    public static void main(String[] args) {
        title("Enums04 — EnumSet, EnumMap, maszyna stanów");

        enumSetBasics();        // enum set basics = EnumSet: podstawy
        enumSetOperations();    // enum set operations = operacje na EnumSet
        enumMapBasics();        // enum map basics = EnumMap: podstawy
        enumMapFromStream();    // enum map from stream = EnumMap ze streamu
        nullPitfall();          // null pitfall = pułapka z null
        stateMachine();         // state machine = maszyna stanów
        stateInsideEnum();      // state inside enum = stan w samym enumie
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. EnumSet — TWORZENIE
    // =================================================================================================

    /**
     * 1. EnumSet nie ma konstruktora — tworzy się go metodami statycznymi. DayOfWeek (dzień tygodnia) to enum z JDK
     * (java.time, t17_datetime): MONDAY ... SUNDAY.
     */
    static void enumSetBasics() {
        section("1. EnumSet: of, range, allOf, noneOf, complementOf");

        Set<DayOfWeek> weekend = EnumSet.of(DayOfWeek.SUNDAY, DayOfWeek.SATURDAY);   // podane w „złej” kolejności
        show("of(SUNDAY, SATURDAY)", weekend);
        // WYNIK: of(SUNDAY, SATURDAY) → [SATURDAY, SUNDAY]    ← zawsze kolejność deklaracji, nie dodawania

        show("range(MONDAY, WEDNESDAY)", EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
        // WYNIK: range(MONDAY, WEDNESDAY) → [MONDAY, TUESDAY, WEDNESDAY]

        show("complementOf(weekend)", EnumSet.complementOf(EnumSet.copyOf(weekend)));
        // WYNIK: complementOf(weekend) → [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY]

        show("allOf(Department).size()", EnumSet.allOf(Department.class).size());
        show("noneOf(Department)", EnumSet.noneOf(Department.class));
        // WYNIK: allOf(Department).size() → 6
        // WYNIK: noneOf(Department) → []

        // DOBRA PRAKTYKA: zbiór stałych enuma = EnumSet. HashSet też zadziała, ale kolejność przy wypisywaniu
        //   i iterowaniu jest wtedy nieprzewidywalna (zależy od hashCode, który dla enuma zmienia się między uruchomieniami).
    }

    // =================================================================================================
    // 2. EnumSet — OPERACJE ZBIOROWE
    // =================================================================================================

    /** 2. EnumSet to zwykły Set: contains, add, remove, containsAll, retainAll — tylko szybciej (operacje na bitach). */
    static void enumSetOperations() {
        section("2. EnumSet: operacje na zbiorach");

        Set<DayOfWeek> gymDays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY);
        Set<DayOfWeek> workDays = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

        show("siłownia w sobotę?", gymDays.contains(DayOfWeek.SATURDAY));
        // WYNIK: siłownia w sobotę? → true

        Set<DayOfWeek> gymOnWorkDays = EnumSet.copyOf(gymDays);   // kopia — nie psujemy oryginału
        gymOnWorkDays.retainAll(workDays);                        // retainAll = zostaw tylko wspólne (część wspólna)
        show("siłownia w dni robocze", gymOnWorkDays);
        // WYNIK: siłownia w dni robocze → [MONDAY, WEDNESDAY]

        Set<DayOfWeek> freeWorkDays = EnumSet.copyOf(workDays);
        freeWorkDays.removeAll(gymDays);                          // removeAll = usuń wszystkie podane (różnica)
        show("dni robocze bez siłowni", freeWorkDays);
        // WYNIK: dni robocze bez siłowni → [TUESDAY, THURSDAY, FRIDAY]

        // PUŁAPKA: EnumSet.copyOf(kolekcja) rzuca IllegalArgumentException dla PUSTEJ zwykłej kolekcji (nie zna typu
        //   enuma). Dla pustego zbioru użyj EnumSet.noneOf(X.class).
    }

    // =================================================================================================
    // 3. EnumMap — PODSTAWY
    // =================================================================================================

    /**
     * 3. EnumMap: klucze to stałe jednego enuma. Konstruktor wymaga klasy enuma (Department.class).
     * Wypisuje się i iteruje w kolejności deklaracji stałych — jak EnumSet.
     */
    static void enumMapBasics() {
        section("3. EnumMap: liczenie pracowników w działach");

        Map<Department, Integer> headcount = new EnumMap<>(Department.class);   // headcount = liczba pracowników
        for (Department d : Department.values()) {
            headcount.put(d, 0);                     // najpierw zera — żeby w wyniku był też dział bez ludzi
        }
        for (Employee e : SampleData.employees()) {
            headcount.merge(e.department(), 1, Integer::sum);   // merge = scal: dodaj 1 do dotychczasowej wartości
        }
        show("liczba pracowników", headcount);
        // WYNIK: liczba pracowników → {IT=4, HR=1, SPRZEDAZ=2, KSIEGOWOSC=1, MARKETING=2, LOGISTYKA=0}

        // DOBRA PRAKTYKA: klucze-enumy = EnumMap. Kolejność wydruku stała (deklaracja), a nie „losowa” jak w HashMap.
    }

    // =================================================================================================
    // 4. EnumMap ZE STREAMU
    // =================================================================================================

    /**
     * 4. groupingBy domyślnie tworzy HashMap. Trzecim argumentem (fabryka mapy) można zażądać EnumMap:
     * groupingBy(klucz, () -> new EnumMap<>(X.class), kolektor) — t16_streams/Streams11GroupingBy.
     */
    static void enumMapFromStream() {
        section("4. groupingBy do EnumMap");

        Map<Category, Long> productsPerCategory = SampleData.products().stream()
                .collect(Collectors.groupingBy(Product::category,
                        () -> new EnumMap<>(Category.class),
                        Collectors.counting()));
        show("produkty w kategoriach", productsPerCategory);
        // WYNIK: produkty w kategoriach → {ELEKTRONIKA=4, SPOZYWCZE=3, KSIAZKI=3, ODZIEZ=2, DOM=2}

        Map<OrderStatus, Long> ordersPerStatus = SampleData.orders().stream()
                .collect(Collectors.groupingBy(Order::status,
                        () -> new EnumMap<>(OrderStatus.class),
                        Collectors.counting()));
        show("zamówienia według statusu", ordersPerStatus);
        // WYNIK: zamówienia według statusu → {NOWE=2, OPLACONE=2, WYSLANE=2, DOSTARCZONE=3, ANULOWANE=1}
        // Uwaga: groupingBy tworzy tylko klucze, które wystąpiły — status bez zamówień nie pojawi się wcale.
    }

    // =================================================================================================
    // 5. PUŁAPKA: null
    // =================================================================================================

    /**
     * thrownType = typ rzuconego wyjątku. Wypisujemy tylko typ, bo treść komunikatu NPE z wnętrza JDK zależy
     * od wersji i sposobu kompilacji samego JDK.
     */
    static String thrownType(Runnable action) {
        try {
            action.run();
            return "brak wyjątku";
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName();
        }
    }

    /** 5. EnumSet i EnumMap nie przyjmują null jako elementu/klucza — to NullPointerException. */
    static void nullPitfall() {
        section("5. Pułapka: null w EnumSet i EnumMap");

        Map<Department, Integer> map = new EnumMap<>(Department.class);
        show("EnumMap.put(null, 1)", thrownType(() -> map.put(null, 1)));
        // WYNIK: EnumMap.put(null, 1) → NullPointerException

        Set<Department> set = EnumSet.noneOf(Department.class);
        show("EnumSet.add(null)", thrownType(() -> set.add(null)));
        // WYNIK: EnumSet.add(null) → NullPointerException

        show("map.get(null) (odczyt nie rzuca)", map.get(null));
        // WYNIK: map.get(null) (odczyt nie rzuca) → null
    }

    // =================================================================================================
    // 6. MASZYNA STANÓW: EnumMap + EnumSet
    // =================================================================================================

    /**
     * ALLOWED = dozwolone przejścia. Dla każdego stanu — zbiór stanów, do których wolno przejść.
     * Całe „prawo” zamówienia w jednym miejscu, czytelne jak tabelka.
     */
    static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(OrderStatus.NOWE, EnumSet.of(OrderStatus.OPLACONE, OrderStatus.ANULOWANE));
        ALLOWED.put(OrderStatus.OPLACONE, EnumSet.of(OrderStatus.WYSLANE, OrderStatus.ANULOWANE));
        ALLOWED.put(OrderStatus.WYSLANE, EnumSet.of(OrderStatus.DOSTARCZONE, OrderStatus.ANULOWANE));
        ALLOWED.put(OrderStatus.DOSTARCZONE, EnumSet.noneOf(OrderStatus.class));   // stan końcowy — nigdzie dalej
        ALLOWED.put(OrderStatus.ANULOWANE, EnumSet.noneOf(OrderStatus.class));     // stan końcowy
    }

    /** canMove = czy można przejść ze stanu from do stanu to. */
    static boolean canMove(OrderStatus from, OrderStatus to) {
        return ALLOWED.get(from).contains(to);
    }

    /** Parcel = przesyłka. Pilnuje, żeby jej stan zmieniał się tylko zgodnie z ALLOWED. */
    static final class Parcel {
        private OrderStatus status = OrderStatus.NOWE;

        /** moveTo = przejdź do. Niedozwolone przejście → IllegalStateException (niedozwolony stan obiektu). */
        void moveTo(OrderStatus next) {
            if (!canMove(status, next)) {
                throw new IllegalStateException("Nie można przejść z " + status + " do " + next);
            }
            status = next;
        }

        OrderStatus getStatus() {
            return status;
        }
    }

    /** 6. Maszyna stanów: obiekt ma stan, a przejścia między stanami są dozwolone tylko według tabeli. */
    static void stateMachine() {
        section("6. Maszyna stanów zamówienia");

        for (Map.Entry<OrderStatus, Set<OrderStatus>> e : ALLOWED.entrySet()) {
            System.out.println(e.getKey() + " → " + e.getValue());
        }
        // WYNIK: NOWE → [OPLACONE, ANULOWANE]
        // WYNIK: OPLACONE → [WYSLANE, ANULOWANE]
        // WYNIK: WYSLANE → [DOSTARCZONE, ANULOWANE]
        // WYNIK: DOSTARCZONE → []
        // WYNIK: ANULOWANE → []

        Parcel parcel = new Parcel();
        parcel.moveTo(OrderStatus.OPLACONE);
        parcel.moveTo(OrderStatus.WYSLANE);
        show("stan przesyłki", parcel.getStatus());
        // WYNIK: stan przesyłki → WYSLANE

        expectThrows("WYSLANE → NOWE", () -> parcel.moveTo(OrderStatus.NOWE));
        // WYNIK: ✔ WYSLANE → NOWE → rzucono IllegalStateException: Nie można przejść z WYSLANE do NOWE

        parcel.moveTo(OrderStatus.DOSTARCZONE);
        expectThrows("DOSTARCZONE → ANULOWANE", () -> parcel.moveTo(OrderStatus.ANULOWANE));
        // WYNIK: ✔ DOSTARCZONE → ANULOWANE → rzucono IllegalStateException: Nie można przejść z DOSTARCZONE do ANULOWANE

        // DOBRA PRAKTYKA: stan zmieniaj TYLKO przez metodę, która sprawdza regułę (moveTo), nigdy przez setter
        //   setStatus(...) dostępny dla wszystkich. Wtedy obiekt nie może trafić w „niemożliwy” stan.
        // PUŁAPKA: zapomniany stan w tabeli ALLOWED → ALLOWED.get(stan) zwróci null i canMove rzuci NPE. Sprawdzaj
        //   kompletność tabeli (ćwiczenie 4) albo trzymaj przejścia w samym enumie (sekcja 7).
    }

    // =================================================================================================
    // 7. STAN W SAMYM ENUMIE
    // =================================================================================================

    /** TrafficLight = sygnalizacja świetlna. Każda stała sama wie, jaki stan jest następny (next = następny). */
    enum TrafficLight {
        RED, RED_YELLOW, GREEN, YELLOW;

        TrafficLight next() {
            return switch (this) {
                case RED -> RED_YELLOW;
                case RED_YELLOW -> GREEN;
                case GREEN -> YELLOW;
                case YELLOW -> RED;
            };
        }
    }

    /**
     * 7. Gdy z każdego stanu jest DOKŁADNIE JEDNO przejście (cykl), wystarczy metoda next() w enumie.
     * Gdy przejść jest kilka (jak przy zamówieniu) — tabela EnumMap/EnumSet z sekcji 6 jest czytelniejsza.
     */
    static void stateInsideEnum() {
        section("7. Prosta maszyna stanów w enumie: światła");

        TrafficLight light = TrafficLight.RED;
        StringBuilder cycle = new StringBuilder(light.name());
        for (int i = 0; i < 4; i++) {
            light = light.next();
            cycle.append(" → ").append(light);
        }
        show("cykl", cycle);
        // WYNIK: cykl → RED → RED_YELLOW → GREEN → YELLOW → RED
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • EnumSet: of, range(od, do), allOf(X.class), noneOf(X.class), complementOf(zbiór), copyOf(kolekcja).
     *   • EnumMap: new EnumMap<>(X.class); groupingBy(klucz, () -> new EnumMap<>(X.class), kolektor).
     *   • Obie iterują i wypisują się w kolejności DEKLARACJI stałych — przewidywalnie (HashSet/HashMap nie).
     *   • Szybkie i oszczędne: w środku tablica/bity indeksowane ordinal().
     *   • null jako element/klucz → NullPointerException (odczyt get(null) zwraca null).
     *   • Maszyna stanów: {@code Map<Stan, Set<Stan>>} dozwolonych przejść + metoda moveTo sprawdzająca regułę.
     *   • Jedno przejście z każdego stanu (cykl) → metoda next() w samym enumie.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego do kluczy-enumów lepsza jest EnumMap niż HashMap?
     *   2. Co wypisze:  System.out.println(EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.MONDAY));  ?
     *   3. ZNAJDŹ BŁĄD:
     *          List<Department> none = new ArrayList<>();
     *          Set<Department> set = EnumSet.copyOf(none);
     *   4. Co wypisze:  System.out.println(EnumSet.complementOf(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY)));  ?
     *   5. Dlaczego zmiana stanu zamówienia powinna iść przez metodę moveTo, a nie publiczny setter?
     *   6. Kiedy maszynę stanów wystarczy zapisać jako metodę next() w enumie, a kiedy lepsza jest tabela przejść?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: dni od środy do piątku", EnumSet.of(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                () -> exercise1());
        Check.equal("ćw. 2: suma pensji w działach", "{IT=53600, HR=7200, SPRZEDAZ=20100, KSIEGOWOSC=8100, MARKETING=17400}",
                () -> String.valueOf(exercise2()));
        Check.equal("ćw. 3a: NOWE → OPLACONE → WYSLANE → DOSTARCZONE", true,
                () -> exercise3(List.of(OrderStatus.NOWE, OrderStatus.OPLACONE, OrderStatus.WYSLANE, OrderStatus.DOSTARCZONE)));
        Check.equal("ćw. 3b: NOWE → WYSLANE", false, () -> exercise3(List.of(OrderStatus.NOWE, OrderStatus.WYSLANE)));
        Check.equal("ćw. 4: stany, z których można anulować", EnumSet.of(OrderStatus.NOWE, OrderStatus.OPLACONE, OrderStatus.WYSLANE),
                () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", EnumSet.of(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", "{IT=53600, HR=7200, SPRZEDAZ=20100, KSIEGOWOSC=8100, MARKETING=17400}",
                () -> String.valueOf(solution2()));
        Check.equal("ćw. 3a (wzorzec)", true,
                () -> solution3(List.of(OrderStatus.NOWE, OrderStatus.OPLACONE, OrderStatus.WYSLANE, OrderStatus.DOSTARCZONE)));
        Check.equal("ćw. 3b (wzorzec)", false, () -> solution3(List.of(OrderStatus.NOWE, OrderStatus.WYSLANE)));
        Check.equal("ćw. 4 (wzorzec)", EnumSet.of(OrderStatus.NOWE, OrderStatus.OPLACONE, OrderStatus.WYSLANE), () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): zwróć zbiór dni od środy do piątku włącznie. Podpowiedź: EnumSet.range. */
    static Set<DayOfWeek> exercise1() {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 2 (średnie, łączy z t16_streams): suma pensji (salary) pracowników w każdym dziale, jako EnumMap.
     * Podpowiedź: groupingBy(Employee::department, () -> new EnumMap<>(Department.class), Collectors.summingInt(...)).
     */
    static Map<Department, Integer> exercise2() {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 3 (średnie): sprawdź, czy ścieżka stanów jest dozwolona — każda para sąsiednich stanów
     * (path.get(i), path.get(i + 1)) musi spełniać canMove. Podpowiedź: pętla do path.size() - 1.
     */
    static boolean exercise3(List<OrderStatus> path) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć EnumSet stanów, z których wolno przejść do ANULOWANE (na podstawie ALLOWED).
     * Podpowiedź: EnumSet.noneOf(OrderStatus.class), pętla po ALLOWED.entrySet() i add dla pasujących kluczy.
     */
    static Set<OrderStatus> exercise4() {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Set<DayOfWeek> solution1() {
        return EnumSet.range(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    }

    static Map<Department, Integer> solution2() {
        return SampleData.employees().stream()
                .collect(Collectors.groupingBy(Employee::department,
                        () -> new EnumMap<>(Department.class),
                        Collectors.summingInt(Employee::salary)));
    }

    static boolean solution3(List<OrderStatus> path) {
        for (int i = 0; i < path.size() - 1; i++) {
            if (!canMove(path.get(i), path.get(i + 1))) {
                return false;
            }
        }
        return true;
    }

    static Set<OrderStatus> solution4() {
        Set<OrderStatus> result = EnumSet.noneOf(OrderStatus.class);
        for (Map.Entry<OrderStatus, Set<OrderStatus>> e : ALLOWED.entrySet()) {
            if (e.getValue().contains(OrderStatus.ANULOWANE)) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. EnumMap jest szybsza i oszczędniejsza (tablica indeksowana ordinal()) i zawsze iteruje w kolejności deklaracji
     *      stałych — wydruk jest przewidywalny. HashMap iteruje w kolejności zależnej od hashCode.
     *   2. „[MONDAY, FRIDAY]” — kolejność deklaracji, nie podania.
     *   3. EnumSet.copyOf z pustej zwykłej kolekcji rzuca IllegalArgumentException — nie wie, jakiego enuma użyć.
     *      Poprawnie: EnumSet.noneOf(Department.class).
     *   4. „[SUNDAY]”.
     *   5. Metoda moveTo sprawdza, czy przejście jest dozwolone — obiekt nigdy nie trafi w niemożliwy stan
     *      (np. DOSTARCZONE → NOWE). Publiczny setter pozwala ustawić cokolwiek z dowolnego miejsca.
     *   6. next() wystarczy, gdy z każdego stanu jest dokładnie jedno przejście (np. cykl świateł). Gdy przejść jest
     *      kilka lub zależą od akcji — czytelniejsza jest tabela Map<Stan, Set<Stan>>.
     */
    // </editor-fold>
}
