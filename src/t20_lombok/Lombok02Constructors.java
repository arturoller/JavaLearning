package t20_lombok;

import helpers.Check;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.stream.Collectors;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Lombok — konstruktory: @NoArgsConstructor, @AllArgsConstructor, @RequiredArgsConstructor, @NonNull
 *        (required = wymagany; factory = fabryka; injection = wstrzykiwanie)
 *
 * W SKRÓCIE:
 *   Trzy adnotacje generują trzy różne konstruktory w zależności od tego, KTÓRE pola mają się w nich
 *   znaleźć: {@code @NoArgsConstructor} (zero argumentów), {@code @AllArgsConstructor} (wszystkie pola,
 *   w KOLEJNOŚCI deklaracji) i {@code @RequiredArgsConstructor} (tylko pola {@code final} oraz
 *   {@code @NonNull} bez wartości początkowej). {@code @NonNull} na polu dodatkowo wstawia sprawdzenie
 *   „czy nie null” na starcie konstruktora (i settera).
 *
 * ANALOGIA: formularz rejestracyjny.
 *   {@code @RequiredArgsConstructor} to formularz z gwiazdkami przy polach obowiązkowych — reszta jest
 *   opcjonalna i ma wartość domyślną. {@code @AllArgsConstructor} to formularz, w którym MUSISZ wypełnić
 *   każde pole, w ustalonej kolejności — a jeśli ktoś przestawi pytania miejscami (pola w kodzie), Twoje
 *   stare odpowiedzi (wywołania konstruktora) trafią w złe rubryki.
 *
 * JAK TO DZIAŁA:
 *   class Product {
 *       private final String sku;      ← final → wymagane
 *       @NonNull private String name;  ← @NonNull, bez inicjalizatora → wymagane
 *       private double price;          ← zwykłe pole → NIE jest wymagane (pomijane przez @RequiredArgsConstructor)
 *   }
 *   @RequiredArgsConstructor generuje:  Product(String sku, String name) { ...sprawdzenie name... }
 *
 * SŁÓWKA:
 *   required = wymagany; factory method = metoda fabrykująca; staticName = nazwa statycznej fabryki;
 *   dependency injection = wstrzykiwanie zależności; constructor injection = wstrzykiwanie przez
 *   konstruktor; positional (parameter) = pozycyjny (parametr wg miejsca, nie nazwy); signature = sygnatura.
 *
 * ZOBACZ TEŻ: t09_records/Records02Constructors (konstruktor kompaktowy rekordu — też waliduje/normalizuje),
 *             t06_oop_basics/Oop02Constructors (konstruktory ręcznie, this(...) łańcuchowanie),
 *             t19_annotations_reflection/Annotations03ReflectionBasics (refleksja użyta do dowodu niżej).
 * </pre>
 */
public class Lombok02Constructors {

    // ---------------------------------------------------------------------------------------------
    // Klasy używane w lekcji
    // ---------------------------------------------------------------------------------------------

    /** Point = punkt. Pokazuje RAZEM @NoArgsConstructor (zero argumentów) i @AllArgsConstructor (wszystkie pola). */
    @Getter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    static class Point {
        private int x;
        private int y;
    }

    /**
     * Product = produkt. {@code sku} jest final → wymagane. {@code name} ma {@code @NonNull} → też
     * wymagane (i sprawdzane w locie). {@code price} to zwykłe pole → NIE wchodzi do konstruktora
     * wygenerowanego przez {@code @RequiredArgsConstructor} (dostaje domyślną wartość typu, tu 0.0).
     */
    @Getter
    @ToString
    @RequiredArgsConstructor
    static class Product {
        private final String sku;
        @NonNull
        private String name;
        private double price;
    }

    /**
     * Money = kwota z walutą. {@code staticName = "of"} ukrywa zwykły konstruktor (robi go
     * {@code private}) i dodaje publiczną statyczną fabrykę {@code Money.of(amount, currency)}.
     */
    @Getter
    @ToString
    @AllArgsConstructor(staticName = "of")
    static class Money {
        private final int amount;
        private final String currency;
    }

    /** Notifier = powiadamiacz. Funkcyjny interfejs „zależności” do wstrzyknięcia w OrderService niżej. */
    interface Notifier {
        String notify(String message);
    }

    /**
     * OrderService = serwis zamówień. Styl znany ze Springa: pola {@code final} + {@code @RequiredArgsConstructor}
     * = gotowy konstruktor do WSTRZYKIWANIA ZALEŻNOŚCI (constructor injection). Od Spring 4.3 klasa z
     * JEDNYM konstruktorem nie potrzebuje nawet adnotacji {@code @Autowired} — kontener sam go znajdzie i użyje.
     */
    @RequiredArgsConstructor
    static class OrderService {
        private final Notifier notifier;

        String placeOrder(String item) {
            return notifier.notify("Zamówiono: " + item);
        }
    }

    /** OrderOriginal = zamówienie, kolejność pól: klient, potem produkt. */
    @Getter
    @ToString
    @AllArgsConstructor
    static class OrderOriginal {
        private final String customerName;
        private final String productName;
    }

    /**
     * OrderReordered = TA SAMA klasa logicznie, ale ktoś „dla porządku” przestawił pola alfabetycznie.
     * Kolejność parametrów {@code @AllArgsConstructor} zawsze idzie za kolejnością DEKLARACJI pól —
     * więc wywołanie z tymi samymi argumentami co dla OrderOriginal da inny wynik. Patrz sekcja 6.
     */
    @Getter
    @ToString
    @AllArgsConstructor
    static class OrderReordered {
        private final String productName;   // UWAGA: productName jest teraz PIERWSZE
        private final String customerName;
    }

    public static void main(String[] args) {
        title("Lombok02 — konstruktory");

        noArgsAllArgs();         // no args all args = bez argumentów / wszystkie argumenty
        requiredArgs();          // required args = wymagane argumenty
        nonNullCheck();          // non null check = sprawdzenie @NonNull
        staticFactory();         // static factory = statyczna fabryka
        constructorInjection();  // constructor injection = wstrzykiwanie przez konstruktor
        reorderingPitfall();     // reordering pitfall = pułapka zmiany kolejności
        reflectionProof();       // reflection proof = dowód refleksją
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. @NoArgsConstructor i @AllArgsConstructor
    // =================================================================================================

    /** 1. Najprostsza para: konstruktor bez argumentów i konstruktor ze WSZYSTKIMI polami po kolei. */
    static void noArgsAllArgs() {
        section("1. @NoArgsConstructor i @AllArgsConstructor");

        show("new Point()", new Point());
        // WYNIK: new Point() → Lombok02Constructors.Point(x=0, y=0)

        show("new Point(3, 4)", new Point(3, 4));
        // WYNIK: new Point(3, 4) → Lombok02Constructors.Point(x=3, y=4)

        // Lombok generuje:
        //   public Point() { }
        //   public Point(int x, int y) { this.x = x; this.y = y; }

        // DOBRA PRAKTYKA: @NoArgsConstructor bywa potrzebny technicznie (np. frameworki JSON/ORM tworzą
        //   obiekt konstruktorem bez argumentów, a potem ustawiają pola przez settery/refleksję) — ale
        //   taki obiekt przez chwilę istnieje w niekompletnym stanie, więc to kompromis, nie ideał.
    }

    // =================================================================================================
    // 2. @RequiredArgsConstructor — tylko final i @NonNull
    // =================================================================================================

    /** 2. Do konstruktora trafiają TYLKO pola final (bez inicjalizatora) i @NonNull (bez inicjalizatora). */
    static void requiredArgs() {
        section("2. @RequiredArgsConstructor — tylko pola wymagane");

        Product p = new Product("ELE-001", "Laptop Pro 14");
        show("produkt (price domyślnie)", p);
        show("p.getPrice()", p.getPrice());
        // WYNIK: produkt (price domyślnie) → Lombok02Constructors.Product(sku=ELE-001, name=Laptop Pro 14, price=0.0)
        // WYNIK: p.getPrice() → 0.0

        // Lombok generuje:
        //   public Product(String sku, String name) {
        //       if (name == null) {
        //           throw new NullPointerException("name is marked non-null but is null");
        //       }
        //       this.sku = sku;
        //       this.name = name;
        //       // price NIE jest tu przypisywane — zostaje przy domyślnej wartości typu (0.0)
        //   }

        // PUŁAPKA: "p.setPrice(5499.99)" by się NIE skompilowało — Product nie ma @Setter, tylko
        //   @Getter. @RequiredArgsConstructor pomija "price" w konstruktorze, ale nie dodaje mu
        //   żadnego innego sposobu ustawienia — za ustawianie pól odpowiada osobna adnotacja (@Setter).
    }

    // =================================================================================================
    // 3. @NonNull — sprawdzenie w locie
    // =================================================================================================

    /** 3. @NonNull wstawia na początku konstruktora (i settera) sprawdzenie "czy nie null" — fail fast. */
    static void nonNullCheck() {
        section("3. @NonNull — sprawdzenie null w konstruktorze");

        expectThrows("new Product(\"ELE-002\", null)", () -> new Product("ELE-002", null));
        // WYNIK: ✔ new Product("ELE-002", null) → rzucono NullPointerException: name is marked non-null but is null

        // DOBRA PRAKTYKA: komunikat "pole is marked non-null but is null" od razu wskazuje, KTÓRE pole
        //   zawiodło — to dużo czytelniejsze niż gołe NullPointerException gdzieś głęboko w kodzie,
        //   który dostał null i zorientował się o tym dopiero przy pierwszym użyciu pola.
    }

    // =================================================================================================
    // 4. staticName — fabryka statyczna zamiast konstruktora
    // =================================================================================================

    /** 4. staticName = "of" chowa konstruktor (robi go private) i daje fabrykę o czytelnej nazwie. */
    static void staticFactory() {
        section("4. staticName — fabryka statyczna");

        Money price = Money.of(150, "PLN");
        show("Money.of(150, \"PLN\")", price);
        // WYNIK: Money.of(150, "PLN") → Lombok02Constructors.Money(amount=150, currency=PLN)

        // Lombok generuje:
        //   private Money(int amount, String currency) { this.amount = amount; this.currency = currency; }
        //   public static Money of(int amount, String currency) { return new Money(amount, currency); }

        // PUŁAPKA: "new Money(150, \"PLN\")" poza klasą Money już się NIE skompiluje (konstruktor jest
        //   private) — błąd kompilacji: "Money(...) has private access". Trzeba użyć Money.of(...).

        // DOBRA PRAKTYKA: staticName = "of" pasuje do stylu statycznych fabryk znanego z List.of,
        //   Optional.of czy fabryk w rekordach (patrz t09_records/Records02Constructors) — czytelne
        //   wywołanie i miejsce na dodatkową logikę w przyszłości bez zmiany API.
    }

    // =================================================================================================
    // 5. Wstrzykiwanie zależności przez konstruktor (styl Spring)
    // =================================================================================================

    /** 5. @RequiredArgsConstructor na polach final = dokładnie taki konstruktor, jakiego chce Spring. */
    static void constructorInjection() {
        section("5. Wstrzykiwanie przez konstruktor (styl Spring)");

        Notifier logujacy = message -> "[LOG] " + message;
        OrderService service = new OrderService(logujacy);
        show("service.placeOrder(\"Laptop\")", service.placeOrder("Laptop"));
        // WYNIK: service.placeOrder("Laptop") → [LOG] Zamówiono: Laptop

        // W Springu wyglądałoby to tak (sam kod identyczny, dochodzi tylko adnotacja @Service na klasie):
        //   @Service
        //   @RequiredArgsConstructor
        //   class OrderService {
        //       private final Notifier notifier;   // Spring wstrzyknie tu swoją implementację (bean)
        //       ...
        //   }
        // Kontener Springa sam wywoła wygenerowany konstruktor, podając zarejestrowany bean Notifier —
        //   dokładnie tak samo jak my ręcznie wyżej.

        // DOBRA PRAKTYKA: pola final wymuszają, że KAŻDA zależność jest podana przy tworzeniu obiektu —
        //   nie da się stworzyć "połowicznie gotowego" serwisu ani podmienić zależności po fakcie.
    }

    // =================================================================================================
    // 6. PUŁAPKA: zmiana kolejności pól cicho zmienia sygnaturę
    // =================================================================================================

    /**
     * 6. @AllArgsConstructor układa parametry WG KOLEJNOŚCI DEKLARACJI pól. Gdy dwa pola mają ten sam
     * typ (tu: String), przestawienie ich w kodzie źródłowym NIE jest błędem kompilacji — ale stare
     * wywołania zaczynają po cichu przypisywać wartości do złych pól.
     */
    static void reorderingPitfall() {
        section("6. Pułapka: przestawienie pól zmienia sygnaturę konstruktora");

        OrderOriginal original = new OrderOriginal("Jan Kowalski", "Laptop Pro 14");
        show("OrderOriginal(\"Jan Kowalski\", \"Laptop Pro 14\")", original);
        // WYNIK: OrderOriginal("Jan Kowalski", "Laptop Pro 14") → Lombok02Constructors.OrderOriginal(customerName=Jan Kowalski, productName=Laptop Pro 14)

        OrderReordered reordered = new OrderReordered("Jan Kowalski", "Laptop Pro 14");
        show("OrderReordered(\"Jan Kowalski\", \"Laptop Pro 14\") — TE SAME argumenty", reordered);
        // WYNIK: OrderReordered("Jan Kowalski", "Laptop Pro 14") — TE SAME argumenty → Lombok02Constructors.OrderReordered(productName=Jan Kowalski, customerName=Laptop Pro 14)

        // PUŁAPKA: w OrderReordered pola productName i customerName zostały zamienione miejscami w
        //   kodzie źródłowym (kosmetyczna, "porządkująca" zmiana). To samo wywołanie konstruktora, co
        //   dla OrderOriginal, teraz wstawia "Jan Kowalski" (imię klienta) do productName! Kompilator
        //   milczy — oba pola to String, więc typy się zgadzają. DLACZEGO to groźne: błąd ujawnia się
        //   dopiero w DANYCH (złe wartości w bazie, złe powiadomienia), nie przy kompilacji ani testach
        //   kompilujących się bez ostrzeżeń.

        // DOBRA PRAKTYKA: im więcej pól tego samego typu, tym większe ryzyko. Lekarstwa: (1) fabryka
        //   staticName="of" + nazwane zmienne pośrednie w miejscu wywołania, (2) @Builder (patrz
        //   Lombok03DataValueBuilder) — wywołania nazwane po polu, kolejność bez znaczenia,
        //   (3) rekord/klasa z jawnym konstruktorem kanonicznym, gdzie zmiana kolejności jest widoczna
        //   w jednym miejscu i celowa, nie przypadkowa.
    }

    // =================================================================================================
    // 7. DOWÓD REFLEKSJĄ: sygnatury wygenerowanych konstruktorów
    // =================================================================================================

    /** 7. Refleksja potwierdza liczbę parametrów każdego wygenerowanego konstruktora. */
    static void reflectionProof() {
        section("7. Dowód refleksją: liczba parametrów konstruktorów");

        show("konstruktory Point (liczby parametrów, posortowane)", constructorParamCounts(Point.class));
        // WYNIK: konstruktory Point (liczby parametrów, posortowane) → 0, 2

        show("konstruktory Product (liczby parametrów)", constructorParamCounts(Product.class));
        // WYNIK: konstruktory Product (liczby parametrów) → 2

        show("konstruktor Money jest prywatny", !java.lang.reflect.Modifier.isPublic(Money.class.getDeclaredConstructors()[0].getModifiers()));
        // WYNIK: konstruktor Money jest prywatny → true
    }

    private static String constructorParamCounts(Class<?> type) {
        return Arrays.stream(type.getDeclaredConstructors())
                .map(Constructor::getParameterCount)
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @NoArgsConstructor — zero argumentów (wszystkie pola dostają wartość domyślną typu).
     *   • @AllArgsConstructor — wszystkie pola, w kolejności DEKLARACJI (nie alfabetycznej, nie wg ważności).
     *   • @RequiredArgsConstructor — tylko pola final (bez inicjalizatora) i @NonNull (bez inicjalizatora).
     *   • @NonNull na polu → sprawdzenie "X is marked non-null but is null" w konstruktorze i w setterze.
     *   • staticName = "..." → konstruktor staje się private, dochodzi publiczna statyczna fabryka o tej nazwie.
     *   • Pola final + @RequiredArgsConstructor = gotowy konstruktor do wstrzykiwania zależności (Spring).
     *   • PUŁAPKA: przestawienie pól tego samego typu zmienia sygnaturę @AllArgsConstructor bez błędu
     *     kompilacji — lekarstwo: @Builder albo staticName z nazwanymi zmiennymi.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie pola trafiają do konstruktora wygenerowanego przez @RequiredArgsConstructor?
     *   2. Co wypisze:  System.out.println(new Point());  (Point ma @NoArgsConstructor i @AllArgsConstructor)?
     *   3. ZNAJDŹ BŁĄD:  @AllArgsConstructor(staticName = "of") class Money {...}  — ktoś w innej
     *      klasie pisze "new Money(10, \"PLN\")". Co się stanie i jak to naprawić?
     *   4. Dlaczego @NonNull jest lepsze niż zwykłe "if (x == null) throw ..." powtarzane w każdym konstruktorze?
     *   5. Co wypisze (OrderReordered ma pola w kolejności productName, customerName):
     *      new OrderReordered("Ala", "Rower").getCustomerName()  ?
     *   6. Dlaczego pole "price" (bez final, bez @NonNull) w Product NIE pojawia się w konstruktorze
     *      wygenerowanym przez @RequiredArgsConstructor?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: suma współrzędnych", 7, () -> exercise1(3, 4));
        Check.equal("ćw. 2: domyślna cena nowego produktu", 0.0, () -> exercise2("KSI-001", "Czysty kod"));
        Check.equal("ćw. 3 (PRZEPISZ): Money przez fabrykę", "Lombok02Constructors.Money(amount=25, currency=PLN)", () -> exercise3(25, "PLN"));
        Check.equal("ćw. 4: serwis z własnym powiadamiaczem", "WYSŁANO: Zamówiono: Kurtka zimowa", () -> exercise4("Kurtka zimowa"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 7, () -> solution1(3, 4));
        Check.equal("ćw. 2 (wzorzec)", 0.0, () -> solution2("KSI-001", "Czysty kod"));
        Check.equal("ćw. 3 (wzorzec)", "Lombok02Constructors.Money(amount=25, currency=PLN)", () -> solution3(25, "PLN"));
        Check.equal("ćw. 4 (wzorzec)", "WYSŁANO: Zamówiono: Kurtka zimowa", () -> solution4("Kurtka zimowa"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): zbuduj Point(x, y) konstruktorem all-args i zwróć sumę x + y. */
    static int exercise1(int x, int y) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 2 (łatwe): zbuduj Product tylko z wymaganymi polami (sku, name) i zwróć jego cenę
     * (powinna być domyślną wartością typu double).
     */
    static double exercise2(String sku, String name) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ): stary, błędny kod próbował tworzyć kwotę tak:
     * <pre>{@code
     *     Money m = new Money(amount, currency);   // BŁĄD KOMPILACJI: konstruktor jest private!
     * }</pre>
     * Przepisz to na poprawne wywołanie przez statyczną fabrykę i zwróć {@code m.toString()}.
     * Podpowiedź: Money.of(amount, currency).
     */
    static String exercise3(int amount, String currency) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): utwórz OrderService z własną implementacją Notifier (lambda), która
     * zwraca "WYSŁANO: " + otrzymana wiadomość. Wywołaj placeOrder(item) i zwróć wynik.
     * Podpowiedź: Notifier to interfejs funkcyjny — lambda (String message) -> "WYSŁANO: " + message.
     */
    static String exercise4(String item) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int x, int y) {
        Point p = new Point(x, y);
        return p.getX() + p.getY();
    }

    static double solution2(String sku, String name) {
        Product p = new Product(sku, name);
        return p.getPrice();
    }

    static String solution3(int amount, String currency) {
        Money m = Money.of(amount, currency);
        return m.toString();
    }

    static String solution4(String item) {
        Notifier wysylajacy = message -> "WYSŁANO: " + message;
        OrderService service = new OrderService(wysylajacy);
        return service.placeOrder(item);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Tylko pola final (bez inicjalizatora przy deklaracji) oraz pola @NonNull bez inicjalizatora —
     *      zwykłe pola (bez final, bez @NonNull) są pomijane.
     *   2. "Lombok02Constructors.Point(x=0, y=0)" — konstruktor bez argumentów zostawia pola int przy
     *      domyślnej wartości 0.
     *   3. Kod się NIE skompiluje ("Money(...) has private access") — staticName="of" chowa konstruktor.
     *      Naprawa: użyć Money.of(10, "PLN") zamiast new Money(10, "PLN").
     *   4. @NonNull daje JEDNO miejsce prawdy (adnotację przy polu) zamiast powtarzania tego samego
     *      "if (x == null) throw ..." w każdym konstruktorze/setterze — mniej kodu, spójny komunikat,
     *      trudniej zapomnieć o sprawdzeniu w nowym konstruktorze.
     *   5. "Rower" — w OrderReordered pierwsze pole to productName, więc "Ala" trafia do productName,
     *      a "Rower" do customerName; getCustomerName() zwraca więc "Rower".
     *   6. @RequiredArgsConstructor bierze pod uwagę tylko pola final i @NonNull bez inicjalizatora —
     *      "price" nie ma żadnej z tych cech, więc dostaje zwykłą domyślną wartość typu (0.0), a nie
     *      wartość z konstruktora.
     */
    // </editor-fold>
}
