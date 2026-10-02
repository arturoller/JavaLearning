package t22_design_patterns;

import helpers.Check;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wzorzec State — zachowanie zależne od stanu obiektu
 *        (state = stan; context = kontekst, czyli obiekt, który ma stan; transition = przejście)
 *
 * W SKRÓCIE:
 *   Zamówienie jest NOWE, potem OPŁACONE, WYSŁANE, DOSTARCZONE (albo ANULOWANE). W każdym stanie wolno co innego:
 *   zapłacić można tylko nowe, wysłać tylko opłacone. Zamiast pisać ten sam "switch po statusie" w każdej metodzie,
 *   wydzielasz KAŻDY STAN do osobnego obiektu, który sam wie, co wolno i dokąd przejść.
 *
 * ANALOGIA: automat z napojami albo sygnalizacja świetlna. Ten sam przycisk (moneta, timer) robi co innego
 *   zależnie od tego, w jakim stanie jest automat: "czeka na monetę", "ma monetę", "wydaje napój". Nikt nie
 *   sprawdza w kółko długiej listy warunków — stan zna swoje zasady.
 *
 * JAK TO DZIAŁA:
 *   Role z książki GoF (Gang of Four):
 *     Context (kontekst) — obiekt ze stanem (Order); trzyma pole "aktualny stan" i DELEGUJE do niego wywołania
 *     State (stan)       — interfejs z operacjami (pay, ship, deliver, cancel)
 *     ConcreteState      — jedna klasa na stan; implementuje, co wolno w tym stanie, i zwraca stan następny
 *
 *   Przejścia (diagram stanów) w tej lekcji:
 *
 *     NOWE ──zapłać──▶ OPŁACONE ──wyślij──▶ WYSŁANE ──dostarcz──▶ DOSTARCZONE
 *       │                  │
 *       └──────anuluj──────┴──────anuluj──▶ ANULOWANE
 *
 *   Wywołanie niedozwolone (np. wyślij z NOWE) kończy się wyjątkiem IllegalStateException — zamiast cichego
 *   przeskoczenia w błędny stan.
 *
 *   Warianty w tej lekcji: klasy (sekcja 2), enum z metodami (4), tabela przejść (5), rekordy sealed z danymi (6).
 *
 * SŁÓWKA: state = stan; transition = przejście; action = akcja; context = kontekst; illegal = niedozwolony;
 *   lifecycle = cykl życia; state machine = maszyna stanów; table-driven = sterowane tabelą; guard = strażnik (warunek)
 *
 * ZOBACZ TEŻ: t22_design_patterns/Patterns01Strategy (ten sam kształt, inny cel — sekcja 7),
 *   t08_enums/Enums03ConstantBodies (enum z metodami per stała), t08_enums/Enums04EnumMapSet (EnumMap i EnumSet),
 *   t22_design_patterns/Patterns15Visitor (sealed + rekordy)
 * </pre>
 */
public class Patterns13State {

    public static void main(String[] args) {
        title("Patterns13 — State: cykl życia zamówienia");

        problemSwitchEverywhere();      // problem switch everywhere = problem: switch w każdej metodzie
        classicState();                 // classic state = klasyczny wzorzec: klasa na stan
        behaviourPerState();            // behaviour per state = zachowanie zależne od stanu
        enumStateMachine();             // enum state machine = maszyna stanów w enumie
        tableDrivenTransitions();       // table driven transitions = przejścia z tabeli
        sealedRecordStates();           // sealed record states = stany jako rekordy z danymi
        stateVersusStrategy();          // state versus strategy = stan a strategia
        testingTransitions();           // testing transitions = testowanie przejść
        stateInTheJdk();                // state in the jdk = stan w JDK i Springu
        pitfallsAndWhenToUse();         // pitfalls and when to use = pułapki i kiedy stosować
        exercises();                    // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PROBLEM: switch po statusie w każdej metodzie
    // =================================================================================================

    /** Status w wersji "bez wzorca": zwykły enum (typ wyliczeniowy). */
    enum Status { NEW, PAID, SHIPPED, DELIVERED, CANCELLED }  // new = nowe; paid = opłacone; shipped = wysłane; delivered = dostarczone; cancelled = anulowane

    /** Zamówienie "po staremu": każda metoda sama sprawdza status. */
    static class NaiveOrder {  // naive = naiwny
        Status status = Status.NEW;  // status = stan zamówienia

        void pay() {  // pay = zapłać
            switch (status) {
                case NEW -> status = Status.PAID;
                default -> throw new IllegalStateException("nie można zapłacić w stanie " + status);
            }
        }

        void ship() {  // ship = wyślij
            // BŁĄD: sprawdzamy tylko NEW; o ANULOWANYM i DOSTARCZONYM autor po prostu zapomniał.
            if (status == Status.NEW) {
                throw new IllegalStateException("najpierw zapłać");
            }
            status = Status.SHIPPED;
        }

        void cancel() {  // cancel = anuluj
            if (status == Status.NEW || status == Status.PAID) {
                status = Status.CANCELLED;
            } else {
                throw new IllegalStateException("nie można anulować w stanie " + status);
            }
        }

        boolean canEdit() {  // canEdit = czy można edytować
            return switch (status) {  // switch jako wyrażenie (Java 14+)
                case NEW -> true;
                case PAID, SHIPPED, DELIVERED, CANCELLED -> false;
            };
        }
    }

    /**
     * 1. Reguły cyklu życia są rozsiane po wielu metodach (pay, ship, cancel, canEdit, ...), a każda ma własny
     * kawałek "co wolno w jakim stanie". Nowy stan (np. ZWROT) to edycja KAŻDEJ metody, a zapomniany przypadek
     * kończy się cichym błędem. Tu: anulowane zamówienie da się wysłać.
     */
    static void problemSwitchEverywhere() {
        section("1. Problem: reguły stanu rozsiane po metodach");

        NaiveOrder order = new NaiveOrder();
        order.cancel();
        show("stan po cancel()", order.status);
        // WYNIK: stan po cancel() → CANCELLED
        order.ship();  // ANULOWANE zamówienie wysłane — nikt nie rzucił wyjątku!
        show("stan po ship() na anulowanym", order.status);
        // WYNIK: stan po ship() na anulowanym → SHIPPED

        // PUŁAPKA: gdy każda metoda ma własne sprawdzanie statusu, wystarczy JEDNO zapomniane sprawdzenie, żeby
        // powstało przejście, które w ogóle nie powinno istnieć. Switch bez gałęzi default ostrzeże Cię tylko
        // w wyrażeniu switch (Java 14+); w if-ach nikt Cię nie ostrzeże.
        // DOBRA PRAKTYKA: reguły przejść trzymaj w JEDNYM miejscu (jedna klasa stanu, jeden enum, jedna tabela).
        show("canEdit() dla NEW", new NaiveOrder().canEdit());
        // WYNIK: canEdit() dla NEW → true
    }

    // =================================================================================================
    // 2. KLASYCZNY WZORZEC STATE
    // =================================================================================================

    /**
     * STATE (stan) — interfejs. Domyślnie KAŻDA akcja jest niedozwolona (rzuca wyjątek); konkretne stany
     * nadpisują tylko to, co w nich wolno. Dzięki temu nowa akcja nie zmusza do dopisywania "rzucania" w każdym stanie.
     */
    interface OrderState {
        String name();  // name = nazwa stanu do wydruku

        default OrderState pay() { throw illegal("zapłacić"); }       // zwraca STAN NASTĘPNY
        default OrderState ship() { throw illegal("wysłać"); }
        default OrderState deliver() { throw illegal("dostarczyć"); }
        default OrderState cancel() { throw illegal("anulować"); }
        default boolean isEditable() { return false; }                // editable = edytowalny

        private IllegalStateException illegal(String action) {  // metoda prywatna w interfejsie (Java 9+)
            return new IllegalStateException("nie można " + action + " w stanie " + name());
        }
    }

    // Konkretne stany jako rekordy bez pól (rekord = krótki zapis klasy bez danych) — każdy zna swoje reguły.
    record NewState() implements OrderState {
        public String name() { return "NOWE"; }
        public OrderState pay() { return new PaidState(); }
        public OrderState cancel() { return new CancelledState(); }
        public boolean isEditable() { return true; }
    }

    record PaidState() implements OrderState {
        public String name() { return "OPŁACONE"; }
        public OrderState ship() { return new ShippedState(); }
        public OrderState cancel() { return new CancelledState(); }
    }

    record ShippedState() implements OrderState {
        public String name() { return "WYSŁANE"; }
        public OrderState deliver() { return new DeliveredState(); }
    }

    record DeliveredState() implements OrderState {
        public String name() { return "DOSTARCZONE"; }  // stan końcowy: żadnych przejść
    }

    record CancelledState() implements OrderState {
        public String name() { return "ANULOWANE"; }    // stan końcowy: żadnych przejść
    }

    /** CONTEXT (kontekst): trzyma aktualny stan i deleguje do niego; zmienia stan na zwrócony przez stan. */
    static class Order {
        private OrderState state = new NewState();  // state = aktualny stan
        private final List<String> history = new ArrayList<>(List.of("NOWE"));  // history = historia stanów

        void pay() { moveTo(state.pay()); }
        void ship() { moveTo(state.ship()); }
        void deliver() { moveTo(state.deliver()); }
        void cancel() { moveTo(state.cancel()); }

        boolean isEditable() { return state.isEditable(); }
        String stateName() { return state.name(); }
        List<String> history() { return List.copyOf(history); }

        private void moveTo(OrderState next) {  // moveTo = przejdź do
            state = next;
            history.add(next.name());
        }

        BigDecimal cancellationFee(BigDecimal total) {  // cancellation fee = opłata za anulowanie
            return cancellationFeeOf(state, total);
        }
    }

    /**
     * 2. Klasyczny State krok po kroku. Order nie ma ani jednego switch po stanie: wywołuje {@code state.pay()}
     * i zapisuje zwrócony stan. Zasady są w klasach stanów — dodanie stanu to nowy rekord i jedna zmiana
     * w tym stanie, z którego do niego prowadzi przejście.
     */
    static void classicState() {
        section("2. Klasyczny State: klasa na stan, stan zwraca następny stan");

        Order order = new Order();
        show("na starcie", order.stateName());
        // WYNIK: na starcie → NOWE
        order.pay();
        order.ship();
        order.deliver();
        show("historia", order.history());
        // WYNIK: historia → [NOWE, OPŁACONE, WYSŁANE, DOSTARCZONE]

        Order cancelled = new Order();
        cancelled.cancel();
        expectThrows("anulowane → ship()", cancelled::ship);  // cancelled::ship = referencja do metody
        // WYNIK: ✔ anulowane → ship() → rzucono IllegalStateException: nie można wysłać w stanie ANULOWANE
        expectThrows("dostarczone → cancel()", order::cancel);
        // WYNIK: ✔ dostarczone → cancel() → rzucono IllegalStateException: nie można anulować w stanie DOSTARCZONE
        show("stan nadal", order.stateName());
        // WYNIK: stan nadal → DOSTARCZONE

        // DOBRA PRAKTYKA: niedozwolone przejście = wyjątek z komunikatem "co + w jakim stanie". Wtedy błąd widać
        // od razu przy jego popełnieniu, a nie dopiero, gdy ktoś dostanie paczkę z anulowanego zamówienia.
        // Stany bez danych są niezmienne — nowy rekord przy każdym przejściu jest tani i bezpieczny.
    }

    // =================================================================================================
    // 3. ZACHOWANIE ZALEŻNE OD STANU
    // =================================================================================================

    /** Opłata za anulowanie zależy od stanu: dla NOWE 0, dla OPŁACONE 5% (zasada szkolna), inne stany nie anulują. */
    static BigDecimal cancellationFeeOf(OrderState state, BigDecimal total) {  // fee = opłata
        if (state instanceof NewState) {
            return BigDecimal.ZERO;
        }
        if (state instanceof PaidState) {
            return total.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);  // HALF_UP = w górę od połowy
        }
        throw new IllegalStateException("nie można anulować w stanie " + state.name());
    }

    /**
     * 3. Stan to nie tylko "czy przejście wolno" — to też zachowanie: co pokazać, ile policzyć, czy pole jest
     * edytowalne. Tę wiedzę pakujemy do stanu, zamiast pytać o status w interfejsie użytkownika i serwisach.
     */
    static void behaviourPerState() {
        section("3. Zachowanie zależne od stanu, nie tylko przejścia");

        BigDecimal total = new BigDecimal("200.00");
        Order order = new Order();
        show("edytowalne (NOWE)", order.isEditable());
        // WYNIK: edytowalne (NOWE) → true
        show("opłata za anulowanie (NOWE)", order.cancellationFee(total));
        // WYNIK: opłata za anulowanie (NOWE) → 0
        order.pay();
        show("edytowalne (OPŁACONE)", order.isEditable());
        // WYNIK: edytowalne (OPŁACONE) → false
        show("opłata za anulowanie (OPŁACONE)", order.cancellationFee(total));
        // WYNIK: opłata za anulowanie (OPŁACONE) → 10.00
        order.ship();
        expectThrows("opłata (WYSŁANE)", () -> order.cancellationFee(total));
        // WYNIK: ✔ opłata (WYSŁANE) → rzucono IllegalStateException: nie można anulować w stanie WYSŁANE

        // PUŁAPKA: ten sam napis "stan" bywa używany do trzech różnych rzeczy: (1) "w jakim jestem etapie",
        // (2) "co mi wolno", (3) "ile to kosztuje w tym etapie". Wzorzec State porządkuje (2) i (3) — a (1) to
        // po prostu wskazanie na aktualny obiekt stanu.
        // (cancellationFeeOf używa instanceof dla prostoty; w czystej wersji opłata byłaby kolejną metodą
        // w interfejsie OrderState — wtedy nowy stan MUSI ją obsłużyć albo odziedziczyć wyjątek.)
    }

    // =================================================================================================
    // 4. MASZYNA STANÓW W ENUMIE
    // =================================================================================================

    /**
     * Stany bez danych to naturalni kandydaci na enum. Każda stała ma ciało z nadpisanymi metodami
     * (t08_enums/Enums03ConstantBodies). Stałe są jedynymi instancjami (singletony), więc nie tworzymy
     * obiektów przy przejściach.
     */
    enum Stage {  // stage = etap
        NEW {
            @Override Stage pay() { return PAID; }
            @Override Stage cancel() { return CANCELLED; }
        },
        PAID {
            @Override Stage ship() { return SHIPPED; }
            @Override Stage cancel() { return CANCELLED; }
        },
        SHIPPED {
            @Override Stage deliver() { return DELIVERED; }
        },
        DELIVERED,
        CANCELLED;

        // Domyślnie wszystko niedozwolone; stałe nadpisują to, co im wolno.
        Stage pay() { throw illegal("pay"); }
        Stage ship() { throw illegal("ship"); }
        Stage deliver() { throw illegal("deliver"); }
        Stage cancel() { throw illegal("cancel"); }

        private IllegalStateException illegal(String action) {
            return new IllegalStateException("akcja " + action + " niedozwolona w etapie " + this);
        }
    }

    /**
     * 4. Wersja z enumem jest najkrótsza dla prostych maszyn stanów: bez osobnych klas i bez pola w kontekście.
     * Kontekst trzyma po prostu wartość {@code Stage}. Kompilator pilnuje, że każda stała istnieje, a EnumMap/EnumSet
     * (t08_enums/Enums04EnumMapSet) dają szybkie mapy po stanach.
     */
    static void enumStateMachine() {
        section("4. Maszyna stanów w enumie");

        Stage stage = Stage.NEW;
        stage = stage.pay();
        show("po pay()", stage);
        // WYNIK: po pay() → PAID
        stage = stage.ship();
        show("po ship()", stage);
        // WYNIK: po ship() → SHIPPED

        Stage current = stage;  // zmienna efektywnie finalna do lambdy
        expectThrows("SHIPPED.pay()", current::pay);
        // WYNIK: ✔ SHIPPED.pay() → rzucono IllegalStateException: akcja pay niedozwolona w etapie SHIPPED
        expectThrows("DELIVERED.cancel()", () -> Stage.DELIVERED.cancel());
        // WYNIK: ✔ DELIVERED.cancel() → rzucono IllegalStateException: akcja cancel niedozwolona w etapie DELIVERED

        // KIEDY enum, a kiedy klasy: enum — stany bez własnych danych, jedna płaska lista, reguły krótkie.
        // Klasy/rekordy — stan niesie dane (numer przesyłki, kod autoryzacji), logika każdego stanu jest duża,
        // albo stanów jest wiele i każdy zasługuje na osobny plik/testy.
        // PUŁAPKA: enum ze stałymi z ciałami, który urósł do tysiąca linii, jest gorszy niż osobne klasy —
        // trudno go czytać i testować po kawałku.
    }

    // =================================================================================================
    // 5. PRZEJŚCIA STEROWANE TABELĄ
    // =================================================================================================

    /** Akcje, które mogą zmienić etap zamówienia. */
    enum Action { PAY, SHIP, DELIVER, CANCEL }  // pay = zapłać; ship = wyślij; deliver = dostarcz; cancel = anuluj

    /** Tabela przejść: etap → (akcja → następny etap). EnumMap iteruje zawsze w kolejności stałych enuma. */
    static final Map<Stage, Map<Action, Stage>> TRANSITIONS = buildTransitions();  // transitions = przejścia

    static Map<Stage, Map<Action, Stage>> buildTransitions() {
        Map<Stage, Map<Action, Stage>> table = new EnumMap<>(Stage.class);
        for (Stage stage : Stage.values()) {
            table.put(stage, new EnumMap<>(Action.class));  // puste wiersze dla wszystkich etapów (też końcowych)
        }
        table.get(Stage.NEW).put(Action.PAY, Stage.PAID);
        table.get(Stage.NEW).put(Action.CANCEL, Stage.CANCELLED);
        table.get(Stage.PAID).put(Action.SHIP, Stage.SHIPPED);
        table.get(Stage.PAID).put(Action.CANCEL, Stage.CANCELLED);
        table.get(Stage.SHIPPED).put(Action.DELIVER, Stage.DELIVERED);
        return table;
    }

    /** Silnik: jedna metoda dla wszystkich przejść — reguły to DANE w tabeli, nie kod. */
    static Stage fire(Stage from, Action action) {  // fire = odpal (wykonaj akcję)
        Stage next = TRANSITIONS.get(from).get(action);
        if (next == null) {
            throw new IllegalStateException("z " + from + " nie da się wykonać " + action
                    + " (dozwolone: " + TRANSITIONS.get(from).keySet() + ")");  // keySet = zbiór kluczy
        }
        return next;
    }

    /**
     * 5. Maszyna sterowana tabelą: reguły to dane. Dzięki temu możesz je wypisać, zwalidować (np. czy z każdego
     * stanu niekońcowego da się dojść do końcowego), załadować z pliku konfiguracji albo narysować diagram.
     * Cena: logika "specjalnych przypadków" (np. opłata zależna od stanu) nie mieści się w tabeli — potrzebny
     * dodatkowy kod (w klasach stanów albo w osobnym strażniku, ang. guard).
     */
    static void tableDrivenTransitions() {
        section("5. Tabela przejść: reguły jako dane");

        show("NEW + PAY", fire(Stage.NEW, Action.PAY));
        // WYNIK: NEW + PAY → PAID
        expectThrows("NEW + SHIP", () -> fire(Stage.NEW, Action.SHIP));
        // WYNIK: ✔ NEW + SHIP → rzucono IllegalStateException: z NEW nie da się wykonać SHIP (dozwolone: [PAY, CANCEL])
        expectThrows("DELIVERED + PAY", () -> fire(Stage.DELIVERED, Action.PAY));
        // WYNIK: ✔ DELIVERED + PAY → rzucono IllegalStateException: z DELIVERED nie da się wykonać PAY (dozwolone: [])

        for (Stage stage : Stage.values()) {
            show("dozwolone z " + stage, TRANSITIONS.get(stage).keySet());
            // WYNIK: dozwolone z NEW → [PAY, CANCEL]
            // WYNIK: dozwolone z PAID → [SHIP, CANCEL]
            // WYNIK: dozwolone z SHIPPED → [DELIVER]
            // WYNIK: dozwolone z DELIVERED → []
            // WYNIK: dozwolone z CANCELLED → []
        }

        // DOBRA PRAKTYKA: wypisanie dozwolonych akcji w komunikacie błędu oszczędza czas przy diagnozie.
        // PUŁAPKA: tabela w kodzie bywa niespójna z dokumentacją/diagramem; test (sekcja 8) powinien ją pilnować.
    }

    // =================================================================================================
    // 6. STANY Z DANYMI: sealed + rekordy
    // =================================================================================================

    /**
     * Zapieczętowany interfejs: cała lista etapów jest znana kompilatorowi. KAŻDY etap to rekord z dokładnie tymi
     * danymi, które mają sens w tym etapie — numer przesyłki istnieje tylko w WYSŁANE, a nie jako pole "null".
     */
    sealed interface Phase permits Placed, Paid, Shipped, Delivered, Cancelled { }  // phase = faza; sealed = zapieczętowany

    record Placed() implements Phase { }                                   // placed = złożone
    record Paid(String paymentId) implements Phase { }                     // payment id = numer płatności
    record Shipped(String paymentId, String trackingNumber) implements Phase { }  // tracking number = numer przesyłki
    record Delivered(String trackingNumber, LocalDate date) implements Phase { }  // date = data (java.time.LocalDate)
    record Cancelled(String reason) implements Phase { }                   // reason = powód

    /** Przejście "zapłać": tylko ze stanu Placed; wynik niesie numer płatności. */
    static Phase pay(Phase phase, String paymentId) {
        if (phase instanceof Placed) {  // instanceof (Java 16+ pozwala też od razu nazwać zmienną)
            return new Paid(paymentId);
        }
        throw new IllegalStateException("zapłacić można tylko złożone, a jest: " + phase);
    }

    /** Przejście "wyślij": tylko z Paid; bierze numer płatności z poprzedniego stanu. */
    static Phase ship(Phase phase, String trackingNumber) {
        if (phase instanceof Paid paid) {  // paid = zmienna wzorca: ma już typ Paid
            return new Shipped(paid.paymentId(), trackingNumber);
        }
        throw new IllegalStateException("wysłać można tylko opłacone, a jest: " + phase);
    }

    /** Przejście "dostarcz": tylko z Shipped; data przychodzi z zewnątrz (nie z zegara — wynik jest powtarzalny). */
    static Phase deliver(Phase phase, LocalDate date) {
        if (phase instanceof Shipped shipped) {
            return new Delivered(shipped.trackingNumber(), date);
        }
        throw new IllegalStateException("dostarczyć można tylko wysłane, a jest: " + phase);
    }

    /** Przejście "anuluj": z Placed albo Paid. */
    static Phase cancel(Phase phase, String reason) {
        if (phase instanceof Placed || phase instanceof Paid) {
            return new Cancelled(reason);
        }
        throw new IllegalStateException("anulować można tylko złożone lub opłacone, a jest: " + phase);
    }

    /**
     * 6. Rekordy-stany są niezmienne: przejście ZWRACA nowy stan. W nowym stanie są tylko dane, które do niego
     * pasują — nie trzeba pól "może null". Kompilator zna listę stanów, więc po przejściu na switch ze wzorcami
     * (Java 21+) dostaniesz błąd, gdy zapomnisz obsłużyć nowy rekord.
     */
    static void sealedRecordStates() {
        section("6. Stany z danymi: sealed + rekordy");

        Phase phase = new Placed();
        phase = pay(phase, "PAY-77");
        show("po pay", phase);
        // WYNIK: po pay → Paid[paymentId=PAY-77]
        phase = ship(phase, "TRK-9");
        show("po ship", phase);
        // WYNIK: po ship → Shipped[paymentId=PAY-77, trackingNumber=TRK-9]
        phase = deliver(phase, LocalDate.of(2026, 3, 1));
        show("po deliver", phase);
        // WYNIK: po deliver → Delivered[trackingNumber=TRK-9, date=2026-03-01]

        Phase paid = pay(new Placed(), "PAY-1");
        expectThrows("pay na opłaconym", () -> pay(paid, "PAY-2"));
        // WYNIK: ✔ pay na opłaconym → rzucono IllegalStateException: zapłacić można tylko złożone, a jest: Paid[paymentId=PAY-1]
        expectThrows("cancel po dostarczeniu", () -> cancel(new Delivered("TRK-1", LocalDate.of(2026, 1, 1)), "za późno"));
        // WYNIK: ✔ cancel po dostarczeniu → rzucono IllegalStateException: anulować można tylko złożone lub opłacone, a jest: Delivered[trackingNumber=TRK-1, date=2026-01-01]
        show("anulowanie opłaconego", cancel(paid, "klient zrezygnował"));
        // WYNIK: anulowanie opłaconego → Cancelled[reason=klient zrezygnował]

        // DOBRA PRAKTYKA: "niemożliwy stan niemożliwy do zapisania" — jeśli numer przesyłki ma sens tylko po wysłaniu,
        // niech istnieje wyłącznie w rekordzie Shipped. Wtedy nie da się mieć "dostarczonego bez numeru".
        // (Java 21+) switch ze wzorcami zastąpi łańcuch instanceof:
        //   return switch (phase) { case Placed p -> ...; case Paid p -> ...; ... };   // bez default — kompilator sprawdza komplet
    }

    // =================================================================================================
    // 7. STATE A STRATEGY
    // =================================================================================================

    /**
     * 7. Strategy (Patterns01Strategy) i State mają TEN SAM kształt: kontekst deleguje do wymiennego obiektu.
     * Różni je cel i to, KTO zmienia obiekt:
     * <pre>
     *   cecha                  | Strategy                         | State
     *   kto zmienia obiekt     | klient (z zewnątrz, świadomie)   | sam stan/kontekst (po zdarzeniu)
     *   czy obiekty znają się  | nie, są niezależne               | tak, stan wie, dokąd przejść
     *   po co                  | wybór algorytmu                  | zachowanie zależne od etapu życia
     *   typowy przykład        | Comparator, sposób dostawy       | zamówienie, połączenie, automat
     * </pre>
     */
    static void stateVersusStrategy() {
        section("7. State a Strategy: kto zmienia obiekt");

        // Strategia: WYWOŁUJĄCY wybiera algorytm — kontekst (lista) tylko go używa.
        List<String> words = new ArrayList<>(List.of("herbata", "kot", "pies"));
        words.sort((a, b) -> Integer.compare(a.length(), b.length()));  // strategia: porównanie po długości
        show("strategia (sort po długości)", words);
        // WYNIK: strategia (sort po długości) → [kot, pies, herbata]

        // Stan: nikt z zewnątrz nie "wstrzykuje" stanu — zmienia go zdarzenie, a zachowanie przełącza się samo.
        Order order = new Order();
        show("edytowalne przed pay()", order.isEditable());
        // WYNIK: edytowalne przed pay() → true
        order.pay();
        show("edytowalne po pay()", order.isEditable());
        // WYNIK: edytowalne po pay() → false

        // PUŁAPKA: nazywanie czegoś "strategią", gdy obiekty przełączają się nawzajem, albo "stanem", gdy decyduje
        // klient — myli czytelników. Pytanie kontrolne: kto i kiedy zamienia obiekt? Klient = strategia; zdarzenie = stan.
    }

    // =================================================================================================
    // 8. TESTOWANIE PRZEJŚĆ
    // =================================================================================================

    /** Ten sam wynik policzony drugą drogą — przez metody enuma, nie przez tabelę. */
    static Stage viaEnum(Stage stage, Action action) {  // via = przez
        return switch (action) {  // switch bez default na enumie: kompilator sprawdza, czy obsłużono wszystkie stałe
            case PAY -> stage.pay();
            case SHIP -> stage.ship();
            case DELIVER -> stage.deliver();
            case CANCEL -> stage.cancel();
        };
    }

    /**
     * 8. Maszynę stanów testuje się wyczerpująco: stanów jest kilka, akcji kilka, więc można sprawdzić WSZYSTKIE
     * pary (stan, akcja). Tu dodatkowo porównujemy dwie implementacje (tabelę i metody enuma) — test wyłapie
     * rozjazd reguł zanim zrobi to klient.
     */
    static void testingTransitions() {
        section("8. Testowanie: wszystkie pary (stan, akcja)");

        int legal = 0;
        int illegal = 0;
        boolean consistent = true;  // consistent = zgodne
        for (Stage stage : Stage.values()) {
            for (Action action : Action.values()) {
                Stage viaTable = null;
                Stage viaMethods = null;
                try {
                    viaTable = fire(stage, action);
                } catch (IllegalStateException e) {
                    // brak przejścia w tabeli — zapisano null
                }
                try {
                    viaMethods = viaEnum(stage, action);
                } catch (IllegalStateException e) {
                    // brak przejścia w metodach enuma — zapisano null
                }
                if (viaTable != viaMethods) {
                    consistent = false;
                }
                if (viaTable == null) {
                    illegal++;
                } else {
                    legal++;
                }
            }
        }
        show("dozwolone przejścia", legal + " z " + (legal + illegal));
        // WYNIK: dozwolone przejścia → 5 z 20
        show("tabela zgodna z enumem", consistent);
        // WYNIK: tabela zgodna z enumem → true

        Check.equal("NEW + CANCEL → CANCELLED", Stage.CANCELLED, () -> fire(Stage.NEW, Action.CANCEL));
        Check.equal("PAID + SHIP → SHIPPED", Stage.SHIPPED, () -> fire(Stage.PAID, Action.SHIP));
        Check.throwsException("CANCELLED + SHIP", IllegalStateException.class, () -> fire(Stage.CANCELLED, Action.SHIP));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD

        // DOBRA PRAKTYKA: test "wszystkie niedozwolone pary rzucają wyjątek" jest ważniejszy niż test dozwolonych —
        // to on wykryłby błąd z sekcji 1 (wysyłka anulowanego zamówienia).
    }

    // =================================================================================================
    // 9. STAN W JDK I W SPRINGU
    // =================================================================================================

    /**
     * 9. W JDK: {@code Thread.State} to enum z etapami życia wątku (NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING,
     * TERMINATED) — cykl życia wątku jest właśnie maszyną stanów (t21_concurrency/Concurrency01Threads).
     * W Springu: osobny projekt Spring Statemachine udostępnia gotową maszynę stanów z konfiguracją przejść.
     * Zwykle jednak w aplikacjach status (np. zamówienia) trzyma się w bazie jako enum, a reguły przejść w
     * serwisie — to właśnie wersja z tabelą albo enumem z tej lekcji.
     */
    static void stateInTheJdk() {
        section("9. Stan w JDK i w Springu");

        show("Thread.State.values()", Arrays.toString(Thread.State.values()));
        // WYNIK: Thread.State.values() → [NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED]
        Thread notStarted = new Thread(() -> { });  // tworzymy wątek, ale NIE uruchamiamy go (start nie jest wołany)
        show("stan nowego wątku", notStarted.getState());  // getState = pobierz stan
        // WYNIK: stan nowego wątku → NEW

        // DOBRA PRAKTYKA: zapisuj w bazie NAZWĘ stanu (enum jako tekst), nie numer porządkowy (ordinal) — dodanie
        // stałej w środku enuma zmieniłoby sens wszystkich starych rekordów.
        // PUŁAPKA: kolumna "status" jako dowolny tekst bez ograniczeń w bazie — literówka daje stan, którego kod nie zna.
    }

    // =================================================================================================
    // 10. PUŁAPKI I KIEDY STOSOWAĆ
    // =================================================================================================

    /**
     * 10. Typowe błędy i decyzja. Najgroźniejszy: eksplozja stanów (ang. state explosion).
     */
    static void pitfallsAndWhenToUse() {
        section("10. Pułapki i kiedy stosować");

        // Eksplozja stanów: jeśli zamówienie ma NIEZALEŻNE wymiary — płatność (3 wartości), wysyłka (3), faktura (2) —
        // to połączenie ich w jeden enum daje 3 * 3 * 2 = 18 stanów (i znacznie więcej przejść). Rozwiązanie: osobne
        // małe maszyny stanów dla każdego wymiaru (trzy pola), zamiast jednej wielkiej.
        int payment = 3;
        int shipping = 3;
        int invoice = 2;
        show("jeden wspólny enum", payment * shipping * invoice + " stanów");
        // WYNIK: jeden wspólny enum → 18 stanów
        show("trzy osobne maszyny", (payment + shipping + invoice) + " stanów");
        // WYNIK: trzy osobne maszyny → 8 stanów

        // PUŁAPKA: współdzielenie stanu zmiennego. Jeśli stany są singletonami (enum, jedna instancja), NIE wolno
        // trzymać w nich danych konkretnego zamówienia — dane trzymaj w kontekście albo w rekordach (sekcja 6).
        // PUŁAPKA: rozproszone przejścia. W wersji "klasa na stan" nikt nie widzi całego diagramu naraz — dopisz
        // diagram w Javadocu (jak w nagłówku) albo użyj tabeli (sekcja 5).
        // PUŁAPKA: wzorzec dla trzech stanów bez własnego zachowania (YAGNI = "nie będziesz tego potrzebował"):
        // zwykły enum i jedna metoda canTransition(from, to) wystarczą.
        // PUŁAPKA: przejścia bez żadnych strażników (guard), np. "wyślij" bez sprawdzenia adresu — warunki
        // biznesowe sprawdzaj PRZED przejściem, a stan niech pilnuje wyłącznie kolejności.
        note("stan = kolejność zdarzeń; reguły biznesowe sprawdzaj przed przejściem");
        // WYNIK: ℹ stan = kolejność zdarzeń; reguły biznesowe sprawdzaj przed przejściem

        // KIEDY UŻYWAĆ / KIEDY NIE:
        //   używaj:  obiekt ma cykl życia z regułami przejść (zamówienie, połączenie, dokument z obiegiem akceptacji);
        //            zachowanie metod zależy od etapu; w kodzie masz kilka switchy po tym samym statusie.
        //   nie używaj: 2–3 stany bez własnego zachowania; stan to zwykła flaga bez reguł przejść; przejścia
        //            zależą głównie od danych (wtedy reguły biznesowe lub silnik reguł), a nie od kolejności zdarzeń.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • State = zachowanie zależne od stanu; kontekst deleguje do obiektu aktualnego stanu, a stan zwraca następny stan.
     *   • Role: Context (kontekst), State (interfejs stanu), ConcreteState (konkretny stan).
     *   • Reguły przejść trzymaj w JEDNYM miejscu; niedozwolone przejście = wyjątek (IllegalStateException).
     *   • Domyślna implementacja "wszystko niedozwolone" w interfejsie + nadpisywanie tego, co wolno w stanie.
     *   • Enum z ciałami stałych: krótko, gdy stany bez danych; klasy/rekordy: gdy stan niesie dane i logikę.
     *   • Tabela przejść (EnumMap): reguły jako dane — łatwo wypisać, zwalidować i przetestować wszystkie pary.
     *   • Sealed + rekordy: każdy etap ma tylko swoje dane; nie ma pól "może null".
     *   • State kontra Strategy: ten sam kształt, inny cel; strategię wybiera klient, stan zmienia się po zdarzeniu.
     *   • Eksplozja stanów: niezależne wymiary = osobne maszyny stanów, nie jeden wielki enum.
     *   • W bazie zapisuj nazwę stanu, nie ordinal.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie role ma wzorzec State i które z nich zna listę dozwolonych przejść?
     *   2. Dlaczego domyślne metody interfejsu OrderState rzucają wyjątek?
     *   3. Co wypisze:  Order o = new Order(); o.pay(); o.cancel(); System.out.println(o.history());  ?
     *   4. Co wypisze:  System.out.println(fire(Stage.PAID, Action.CANCEL));  ?
     *   5. ZNAJDŹ BŁĄD:  void ship() { if (status == Status.NEW) throw new IllegalStateException(); status = Status.SHIPPED; }
     *      Jaki stan zamówienia przejdzie przez to sprawdzenie, choć nie powinien?
     *   6. ZNAJDŹ BŁĄD:  singletonowy stan (enum) zawiera pole String trackingNumber ustawiane w ship(). Co się stanie
     *      przy dwóch zamówieniach?
     *   7. Czym różni się State od Strategy, jeśli oba mają wymienny obiekt w polu kontekstu?
     *   8. Kiedy zwykły enum + metoda canTransition wystarczy zamiast wzorca State?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: dozwolone z NEW", List.of(Action.PAY, Action.CANCEL), () -> exercise1(Stage.NEW));
        Check.equal("ćw. 1: dozwolone z DELIVERED", List.of(), () -> exercise1(Stage.DELIVERED));
        Check.equal("ćw. 2: NEW → DELIVERED", Stage.DELIVERED,
                () -> exercise2(Stage.NEW, List.of(Action.PAY, Action.SHIP, Action.DELIVER)));
        Check.throwsException("ćw. 2: SHIP z NEW", IllegalStateException.class,
                () -> exercise2(Stage.NEW, List.of(Action.SHIP)));
        Check.equal("ćw. 3: Shipped", "wysłane, paczka TRK-9", () -> exercise3(new Shipped("P-1", "TRK-9")));
        Check.equal("ćw. 3: Delivered", "dostarczone 2026-03-01",
                () -> exercise3(new Delivered("TRK-9", LocalDate.of(2026, 3, 1))));
        Check.equal("ćw. 3: Cancelled", "anulowane: brak płatności", () -> exercise3(new Cancelled("brak płatności")));
        Check.equal("ćw. 4: sygnalizacja RED → GREEN", Light.GREEN,
                () -> exercise4(lightTable(), Light.RED, Signal.TIMER));
        Check.throwsException("ćw. 4: brak przejścia", IllegalStateException.class,
                () -> exercise4(TRANSITIONS, Stage.DELIVERED, Action.PAY));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of(Action.PAY, Action.CANCEL), () -> solution1(Stage.NEW));
        Check.equal("ćw. 1b (wzorzec)", List.of(), () -> solution1(Stage.DELIVERED));
        Check.equal("ćw. 2 (wzorzec)", Stage.DELIVERED,
                () -> solution2(Stage.NEW, List.of(Action.PAY, Action.SHIP, Action.DELIVER)));
        Check.throwsException("ćw. 2b (wzorzec)", IllegalStateException.class,
                () -> solution2(Stage.NEW, List.of(Action.SHIP)));
        Check.equal("ćw. 3 (wzorzec)", "wysłane, paczka TRK-9", () -> solution3(new Shipped("P-1", "TRK-9")));
        Check.equal("ćw. 3b (wzorzec)", "dostarczone 2026-03-01",
                () -> solution3(new Delivered("TRK-9", LocalDate.of(2026, 3, 1))));
        Check.equal("ćw. 3c (wzorzec)", "anulowane: brak płatności", () -> solution3(new Cancelled("brak płatności")));
        Check.equal("ćw. 4 (wzorzec)", Light.GREEN, () -> solution4(lightTable(), Light.RED, Signal.TIMER));
        Check.throwsException("ćw. 4b (wzorzec)", IllegalStateException.class,
                () -> solution4(TRANSITIONS, Stage.DELIVERED, Action.PAY));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 9 OK, ✘ 0 BŁĄD
    }

    /** Pomocniczy enum do ćwiczenia 4: prosta sygnalizacja świetlna (light = światło). */
    enum Light { RED, GREEN }

    /** Jedyny sygnał w ćwiczeniu 4. */
    enum Signal { TIMER }  // timer = zegar (odmierzacz czasu)

    static Map<Light, Map<Signal, Light>> lightTable() {
        Map<Light, Map<Signal, Light>> table = new EnumMap<>(Light.class);
        table.put(Light.RED, new EnumMap<>(Map.of(Signal.TIMER, Light.GREEN)));
        table.put(Light.GREEN, new EnumMap<>(Map.of(Signal.TIMER, Light.RED)));
        return table;
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć listę akcji dozwolonych w danym etapie, w kolejności stałych enuma Action.
     * Podpowiedź: TRANSITIONS.get(stage).keySet() — zamień na nową listę (new ArrayList<>(...)).
     */
    static List<Action> exercise1(Stage stage) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zastosuj po kolei listę akcji, zaczynając od danego etapu, i zwróć etap końcowy.
     * Niedozwolona akcja ma przerwać wykonanie wyjątkiem IllegalStateException (wystarczy użyć fire(...)).
     * Podpowiedź: pętla for po akcjach, zmienna current aktualizowana w każdym kroku.
     */
    static Stage exercise2(Stage start, List<Action> actions) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ): stary kod trzymał status w napisie i dane w polach, które bywały null:
     * <pre>{@code
     * if (status.equals("SHIPPED")) { return "wysłane, paczka " + trackingNumber; }       // trackingNumber bywa null
     * if (status.equals("DELIVERED")) { return "dostarczone " + deliveredAt; }            // deliveredAt bywa null
     * if (status.equals("CANCELLED")) { return "anulowane: " + reason; }
     * }</pre>
     * Przepisz na rekordy sealed Phase: Shipped → "wysłane, paczka NUMER", Delivered → "dostarczone DATA",
     * Cancelled → "anulowane: POWÓD", Paid → "opłacone", Placed → "złożone".
     * Podpowiedź: łańcuch instanceof ze zmienną wzorca (np. {@code phase instanceof Shipped s}) i odczyt s.trackingNumber().
     */
    static String exercise3(Phase phase) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): napisz OGÓLNĄ funkcję przejścia dla dowolnej tabeli (dowolne enumy stanów i akcji):
     * zwróć następny stan albo rzuć IllegalStateException, gdy przejścia nie ma. Powinna działać i dla Stage/Action,
     * i dla Light/Signal.
     * Podpowiedź: table.get(from) może dać mapę, a w niej get(action) może dać null — sprawdź oba przypadki.
     */
    static <S extends Enum<S>, A extends Enum<A>> S exercise4(Map<S, Map<A, S>> table, S from, A action) {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<Action> solution1(Stage stage) {
        return new ArrayList<>(TRANSITIONS.get(stage).keySet());
    }

    static Stage solution2(Stage start, List<Action> actions) {
        Stage current = start;
        for (Action action : actions) {
            current = fire(current, action);
        }
        return current;
    }

    static String solution3(Phase phase) {
        if (phase instanceof Shipped shipped) {
            return "wysłane, paczka " + shipped.trackingNumber();
        }
        if (phase instanceof Delivered delivered) {
            return "dostarczone " + delivered.date();
        }
        if (phase instanceof Cancelled cancelled) {
            return "anulowane: " + cancelled.reason();
        }
        if (phase instanceof Paid) {
            return "opłacone";
        }
        return "złożone";  // zostaje tylko Placed (Phase jest sealed)
    }

    static <S extends Enum<S>, A extends Enum<A>> S solution4(Map<S, Map<A, S>> table, S from, A action) {
        Map<A, S> row = table.get(from);
        S next = row == null ? null : row.get(action);
        if (next == null) {
            throw new IllegalStateException("brak przejścia z " + from + " po " + action);
        }
        return next;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Context (kontekst — ma pole ze stanem i deleguje), State (interfejs stanu), ConcreteState (konkretne stany).
     *      Listę dozwolonych przejść zna każdy ConcreteState (ten, w którym jesteśmy).
     *   2. Żeby każda akcja była domyślnie NIEDOZWOLONA: konkretny stan nadpisuje tylko to, co w nim wolno. Dzięki temu
     *      nowy stan nie może przypadkiem "odziedziczyć" zgody na przejście, a nowa akcja nie wymaga pisania
     *      rzucania wyjątku w każdym stanie.
     *   3. [NOWE, OPŁACONE, ANULOWANE] — cancel z OPŁACONE jest dozwolony i dopisuje stan do historii.
     *   4. CANCELLED (z PAID akcja CANCEL prowadzi do CANCELLED).
     *   5. CANCELLED (anulowane), ale też DELIVERED i SHIPPED — sprawdzamy tylko NEW, więc anulowane zamówienie
     *      da się "wysłać", a dostarczone wysłać ponownie. Reguły kolejności trzeba sprawdzić dla każdego stanu, najlepiej w jednym miejscu.
     *   6. Pole jest współdzielone między wszystkimi zamówieniami — drugie ship() nadpisze numer pierwszego. Dane
     *      zamówienia należą do kontekstu albo do rekordu stanu, nie do singletona.
     *   7. Kształt jest ten sam, różni się kto zmienia obiekt i po co: strategię wybiera klient (algorytm), a stan
     *      zmienia się sam po zdarzeniu i wie, dokąd przejść (cykl życia).
     *   8. Gdy stany nie mają własnego zachowania, a jedynie zbiór dozwolonych przejść (2–3 etapy) — wystarczy enum
     *      i tabela albo jedna metoda sprawdzająca.
     */
    // </editor-fold>
}
