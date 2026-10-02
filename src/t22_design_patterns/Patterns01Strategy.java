package t22_design_patterns;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Product;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToIntFunction;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Strategy (strategia) — wymienny algorytm za wspólnym interfejsem
 *        (strategy = strategia, czyli sposób postępowania; policy = reguła, polityka; context = kontekst)
 *
 * W SKRÓCIE:
 *   Gdy ten sam krok programu można wykonać na kilka sposobów (inny rabat, inny koszt dostawy, inne
 *   sortowanie), każdy sposób zamykamy w osobnym obiekcie o wspólnym interfejsie. Kod, który z nich
 *   korzysta, nie wie, KTÓRY sposób dostał — a nowy sposób dodajesz bez zmiany starego kodu.
 *
 * ANALOGIA: nawigacja w samochodzie. Cel podróży ten sam, ale wybierasz strategię: najszybciej,
 *   najkrócej, bez autostrad. Nawigacja (kontekst) nie zmienia się wcale — podmieniasz tylko
 *   sposób liczenia trasy. Nowa opcja „trasa widokowa” nie wymaga przepisywania całej nawigacji.
 *
 * JAK TO DZIAŁA:
 *   Role z książki „Gang of Four” (GoF, czyli „banda czworga” — autorzy klasycznego katalogu wzorców):
 *
 *     ┌────────────┐   ma pole    ┌──────────────────────┐
 *     │  Checkout  │─────────────▶│  «interface»         │
 *     │ (Context = │              │  DiscountPolicy      │
 *     │  kontekst) │              │ (Strategy = strategia)│
 *     └────────────┘              └──────────────────────┘
 *                                    △          △          △
 *                          NoDiscount   PercentDiscount   ThresholdDiscount
 *                          (ConcreteStrategy = konkretna strategia)
 *
 *   1. Interfejs strategii: jedna metoda, np. {@code int discount(Cart cart)}.
 *   2. Konkretne strategie: każda implementuje ją po swojemu (klasa, rekord, lambda, stała enum).
 *   3. Kontekst trzyma strategię w polu i deleguje do niej pracę (delegate = przekaż dalej).
 *   4. Ktoś ZEWNĘTRZNY wybiera strategię (konfiguracja, klient, kontener DI) — kontekst jej nie wybiera.
 *
 *   Gdzie już to znasz: Comparator (porównywanie), ThreadPoolExecutor.AbortPolicy (co zrobić, gdy
 *   kolejka jest pełna), w Springu — wstrzykiwane implementacje interfejsu.
 *
 * SŁÓWKA:
 *   strategy = strategia; policy = polityka, reguła; discount = rabat; cart = koszyk; checkout = kasa
 *   (finalizacja zakupów); shipping = dostawa; context = kontekst; delegate = przekazać pracę innemu
 *   obiektowi; registry = rejestr; runtime = czas działania programu; fake = atrapa (prosty zastępnik
 *   do testu); stateless = bez stanu; open for extension = otwarty na rozszerzanie.
 *
 * ZOBACZ TEŻ: t13_lambdas/Lambda02FunctionalInterfaces (interfejs z jedną metodą),
 *   t12_collections/Collections07ComparableComparator (Comparator), t08_enums/Enums03ConstantBodies
 *   (enum z zachowaniem), t27_clean_code_pitfalls/CleanCode02Solid (zasada otwarte/zamknięte),
 *   t22_design_patterns/Patterns08DependencyInjection (kto dostarcza strategię),
 *   t22_design_patterns/Patterns07Decorator (strategia opakowująca strategię)
 * </pre>
 */
public class Patterns01Strategy {

    public static void main(String[] args) {
        title("Patterns01 — Strategy (strategia): wymienny algorytm");

        problemSwitch();          // problem switch = problem z instrukcją switch
        classicStrategy();        // classic strategy = klasyczna strategia (interfejs + klasy)
        chooseAtRuntime();        // choose at runtime = wybór w czasie działania programu
        lambdasAsStrategies();    // lambdas as strategies = lambdy jako strategie
        enumWithLambda();         // enum with lambda = enum z lambdą w polu
        comparatorInJdk();        // comparator in JDK = Comparator w bibliotece standardowej
        diAndTests();             // DI and tests = wstrzykiwanie zależności i testy
        pitfalls();               // pitfalls = pułapki
        whenToUse();              // when to use = kiedy używać
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // WSPÓLNE TYPY LEKCJI (domena: sklep — rabaty, dostawa, sortowanie produktów)
    // =================================================================================================

    /** Koszyk klienta: kwota towarów w zł (amount = kwota) i waga paczki w kg (weight = waga). */
    record Cart(int amount, int weightKg) {
    }

    /**
     * Interfejs strategii rabatu. {@code @FunctionalInterface} (functional = funkcyjny) to adnotacja:
     * kompilator dopilnuje, że interfejs ma dokładnie jedną metodę abstrakcyjną — wtedy
     * można go zapisać lambdą (t13_lambdas/Lambda02FunctionalInterfaces).
     */
    @FunctionalInterface
    interface DiscountPolicy {
        /** Zwraca rabat w zł (nie procent!) dla danego koszyka. */
        int discount(Cart cart);
    }

    // =================================================================================================
    // 1. PROBLEM: switch rośnie z każdym nowym wymaganiem
    // =================================================================================================

    /** Wersja BEZ wzorca: jeden typ klienta = jedna gałąź switch (switch expression = wyrażenie switch, Java 14+). */
    static int legacyDiscount(String type, Cart cart) {
        return switch (type) {
            case "REGULAR" -> 0;
            case "VIP" -> cart.amount() * 10 / 100;
            case "STUDENT" -> cart.amount() * 5 / 100;
            case "BLACK_FRIDAY" -> cart.amount() * 25 / 100;
            default -> throw new IllegalArgumentException("Nieznany typ rabatu: " + type);
        };
    }

    /** Ta sama rzecz dla dostawy: drugi switch na te same typy klientów — i drugi punkt do zmiany. */
    static int legacyShipping(String type, Cart cart) {
        return switch (type) {
            case "REGULAR", "STUDENT" -> 15 + cart.weightKg();
            case "VIP" -> 0;
            case "BLACK_FRIDAY" -> 20;
            default -> throw new IllegalArgumentException("Nieznany typ dostawy: " + type);
        };
    }

    /**
     * 1. PROBLEM: switch po napisie. Działa, dopóki wymagań jest mało. Potem biznes prosi:
     * „dodajmy rabat FIRST_ORDER dla pierwszego zamówienia”. Co musisz zrobić?
     */
    static void problemSwitch() {
        section("1. Problem: switch, który rośnie");

        Cart cart = new Cart(200, 4);
        show("VIP, koszyk 200 zł → rabat", legacyDiscount("VIP", cart));
        // WYNIK: VIP, koszyk 200 zł → rabat → 20
        show("BLACK_FRIDAY, koszyk 200 zł → rabat", legacyDiscount("BLACK_FRIDAY", cart));
        // WYNIK: BLACK_FRIDAY, koszyk 200 zł → rabat → 50
        show("STUDENT, dostawa", legacyShipping("STUDENT", cart));
        // WYNIK: STUDENT, dostawa → 19
        expectThrows("nieznany typ PARTNER", () -> legacyDiscount("PARTNER", cart));
        // WYNIK: ✔ nieznany typ PARTNER → rzucono IllegalArgumentException: Nieznany typ rabatu: PARTNER

        // CO BOLI W TYM KODZIE (a w prawdziwym sklepie takich switchy jest więcej niż dwa):
        //   1. Nowy typ klienta = edycja KAŻDEGO switcha (rabat, dostawa, faktura, raport...).
        //      Łatwo zapomnieć o jednym — kompilator nie ostrzeże, bo wejściem jest napis (String).
        //   2. Każda zmiana dotyka kodu, który już działał, więc trzeba testować wszystko od nowa
        //      (łamie zasadę „otwarte na rozszerzanie, zamknięte na modyfikację” — OCP, CleanCode02Solid).
        //   3. Nie da się przetestować jednej reguły osobno — zawsze przechodzisz przez cały switch.
        //   4. Nie da się dodać reguły „z zewnątrz” (np. z konfiguracji albo z innego modułu).
        //   5. Metoda puchnie: reguły z warunkami (próg kwoty, data promocji) zamieniają gałąź w kilkanaście linii.

        // PUŁAPKA: switch po napisie nie daje żadnej kontroli czasu kompilacji — literówka "VIPP"
        // wyjdzie dopiero w działającym programie, jako IllegalArgumentException (illegal argument =
        // niedozwolony argument). Strategia z interfejsem przenosi tę kontrolę do kompilatora.
    }

    // =================================================================================================
    // 2. KLASYCZNA STRATEGIA: interfejs + klasy
    // =================================================================================================

    /** Brak rabatu. record = rekord (t09_records/Records01Basics) — krótki zapis klasy bez zmiennych pól. */
    record NoDiscount() implements DiscountPolicy {
        @Override // Override = przesłoń (metodę z interfejsu)
        public int discount(Cart cart) {
            return 0;
        }
    }

    /** Rabat procentowy: percent = procent. Strategia ma własną konfigurację (pole percent). */
    record PercentDiscount(int percent) implements DiscountPolicy {
        @Override
        public int discount(Cart cart) {
            return cart.amount() * percent / 100;
        }
    }

    /** Kupon: threshold = próg kwoty; amountOff = ile zł taniej. Działa tylko od progu wzwyż. */
    record ThresholdDiscount(int threshold, int amountOff) implements DiscountPolicy {
        @Override
        public int discount(Cart cart) {
            return cart.amount() >= threshold ? amountOff : 0;
        }
    }

    /**
     * Kontekst (Context): zna TYLKO interfejs DiscountPolicy. Nie wie, czy to procent, kupon czy coś,
     * co ktoś doda za rok. final = pole ustawione raz w konstruktorze i niezmienne.
     */
    static final class Checkout {
        private final DiscountPolicy policy;

        Checkout(DiscountPolicy policy) {
            this.policy = policy;
        }

        /** toPay = do zapłaty. Delegacja: liczenie rabatu oddajemy strategii. */
        int toPay(Cart cart) {
            return cart.amount() - policy.discount(cart);
        }
    }

    /**
     * 2. Ta sama kasa, trzy różne zachowania. Klasa Checkout nie zmieniła się ani o linijkę,
     * a dołożyliśmy trzy strategie — to właśnie „otwarte na rozszerzanie, zamknięte na modyfikację”.
     */
    static void classicStrategy() {
        section("2. Klasyczna strategia: interfejs + konkretne klasy");

        Cart cart = new Cart(200, 4);
        Checkout noDiscount = new Checkout(new NoDiscount());
        Checkout vip = new Checkout(new PercentDiscount(10));
        Checkout voucher = new Checkout(new ThresholdDiscount(150, 30));   // voucher = kupon

        show("do zapłaty (bez rabatu)", noDiscount.toPay(cart));
        // WYNIK: do zapłaty (bez rabatu) → 200
        show("do zapłaty (VIP, 10%)", vip.toPay(cart));
        // WYNIK: do zapłaty (VIP, 10%) → 180
        show("do zapłaty (kupon 30 zł od 150 zł)", voucher.toPay(cart));
        // WYNIK: do zapłaty (kupon 30 zł od 150 zł) → 170
        show("kupon przy koszyku za 100 zł", voucher.toPay(new Cart(100, 1)));
        // WYNIK: kupon przy koszyku za 100 zł → 100
        show("obiekt strategii (toString rekordu)", new PercentDiscount(10));
        // WYNIK: obiekt strategii (toString rekordu) → PercentDiscount[percent=10]

        // DOBRA PRAKTYKA: strategia ma jedną odpowiedzialność i jest małą, osobną klasą. Dzięki temu
        // każdą testujesz osobno, bez kasy, bazy danych i reszty sklepu.
        // DOBRA PRAKTYKA: konfigurację strategii (procent, próg) trzymaj w jej polach, a nie w kontekście —
        // wtedy kontekst nie musi wiedzieć, jakie parametry mają poszczególne strategie.
    }

    // =================================================================================================
    // 3. WYBÓR STRATEGII W CZASIE DZIAŁANIA
    // =================================================================================================

    /** Rejestr: nazwa z konfiguracji → strategia. TreeMap = mapa posortowana po kluczach (stały wydruk). */
    static Map<String, DiscountPolicy> policyRegistry() {
        Map<String, DiscountPolicy> registry = new TreeMap<>();
        registry.put("REGULAR", new NoDiscount());
        registry.put("VIP", new PercentDiscount(10));
        registry.put("STUDENT", new PercentDiscount(5));
        registry.put("BLACK_FRIDAY", new PercentDiscount(25));
        return registry;
    }

    /** Jedyne miejsce, w którym „nazwa” zamienia się w strategię; brak nazwy = czytelny błąd. */
    static DiscountPolicy policyFromConfig(Map<String, DiscountPolicy> registry, String name) {
        DiscountPolicy policy = registry.get(name);
        if (policy == null) {
            throw new IllegalArgumentException(
                    "Nieznana polityka rabatowa: " + name + " (dostępne: " + registry.keySet() + ")");
        }
        return policy;
    }

    /**
     * 3. Wybór strategii z napisu (np. z pliku konfiguracyjnego albo parametru uruchomienia).
     * Zamiast switch w wielu miejscach mamy JEDEN rejestr (registry) w jednym miejscu.
     */
    static void chooseAtRuntime() {
        section("3. Wybór strategii w czasie działania (rejestr nazw)");

        Map<String, DiscountPolicy> registry = policyRegistry();
        Cart cart = new Cart(200, 4);
        show("dostępne polityki", registry.keySet());
        // WYNIK: dostępne polityki → [BLACK_FRIDAY, REGULAR, STUDENT, VIP]

        String configured = "VIP";   // configured = skonfigurowana; tu na sztywno, normalnie z pliku
        Checkout checkout = new Checkout(policyFromConfig(registry, configured));
        show("konfiguracja VIP → do zapłaty", checkout.toPay(cart));
        // WYNIK: konfiguracja VIP → do zapłaty → 180

        expectThrows("konfiguracja PARTNER", () -> policyFromConfig(registry, "PARTNER"));
        // WYNIK: ✔ konfiguracja PARTNER → rzucono IllegalArgumentException: Nieznana polityka rabatowa: PARTNER (dostępne: [BLACK_FRIDAY, REGULAR, STUDENT, VIP])

        // Nowa strategia „z zewnątrz” — Checkout i istniejące strategie pozostają nietknięte:
        registry.put("FIRST_ORDER", new ThresholdDiscount(100, 25));
        show("po dodaniu FIRST_ORDER", registry.keySet());
        // WYNIK: po dodaniu FIRST_ORDER → [BLACK_FRIDAY, FIRST_ORDER, REGULAR, STUDENT, VIP]
        show("FIRST_ORDER → do zapłaty", new Checkout(policyFromConfig(registry, "FIRST_ORDER")).toPay(cart));
        // WYNIK: FIRST_ORDER → do zapłaty → 175

        // DOBRA PRAKTYKA: wybór strategii (kto ją wybiera) odseparuj od jej użycia. Kontekst dostaje
        // gotową strategię — ten sam pomysł rozwija t22_design_patterns/Patterns08DependencyInjection.
        // PUŁAPKA: mapa wypełniana w wielu miejscach programu = ten sam bałagan co switch, tylko
        // ukryty. Rejestr buduj w jednym miejscu (tu: policyRegistry) i rzucaj błąd dla nieznanej nazwy,
        // zamiast po cichu zwracać „brak rabatu” — cichy błąd w pieniądzach trudno później wykryć.
    }

    // =================================================================================================
    // 4. NOWOCZESNA WERSJA: lambdy i referencje do metod
    // =================================================================================================

    /** Zwykła metoda statyczna — może posłużyć jako strategia przez referencję do metody. */
    static int bulkDiscount(Cart cart) {   // bulk = hurt, duża partia
        return cart.weightKg() >= 20 ? 50 : 0;
    }

    /** Strategia opakowująca inną strategię: ogranicza rabat z góry (cap = sufit). Funkcja zwracająca funkcję. */
    static DiscountPolicy capped(DiscountPolicy inner, int maxDiscount) {
        return cart -> Math.min(inner.discount(cart), maxDiscount);   // min = mniejsza z dwóch liczb
    }

    /**
     * 4. Interfejs z jedną metodą to dla Javy po prostu „funkcja”. Zamiast osobnej klasy z jedną linią
     * logiki piszemy lambdę (t13_lambdas/Lambda01FromAnonymousToLambda) albo referencję do metody
     * (t13_lambdas/Lambda04MethodReferences).
     */
    static void lambdasAsStrategies() {
        section("4. Lambdy i referencje do metod jako strategie");

        DiscountPolicy tenPercent = cart -> cart.amount() / 10;       // lambda zamiast klasy
        DiscountPolicy bulk = Patterns01Strategy::bulkDiscount;       // referencja do metody statycznej
        Cart heavy = new Cart(300, 25);

        show("10% z 300 zł", tenPercent.discount(heavy));
        // WYNIK: 10% z 300 zł → 30
        show("rabat hurtowy (waga 25 kg)", bulk.discount(heavy));
        // WYNIK: rabat hurtowy (waga 25 kg) → 50
        show("10% ograniczone do 20 zł", capped(tenPercent, 20).discount(heavy));
        // WYNIK: 10% ograniczone do 20 zł → 20
        show("10% ograniczone do 20 zł (koszyk 100 zł)", capped(tenPercent, 20).discount(new Cart(100, 1)));
        // WYNIK: 10% ograniczone do 20 zł (koszyk 100 zł) → 10

        // Lambdę można też trzymać w mapie zamiast rejestru obiektów:
        Map<String, DiscountPolicy> byName = new TreeMap<>();
        byName.put("TEN_PERCENT", tenPercent);
        byName.put("BULK", bulk);
        byName.put("NONE", cart -> 0);
        show("rejestr z lambdami", byName.keySet());
        // WYNIK: rejestr z lambdami → [BULK, NONE, TEN_PERCENT]

        // KLASA CZY LAMBDA?
        //   lambda  — gdy strategia to jedna krótka reguła i nie potrzebuje nazwy ani własnego stanu;
        //   klasa/rekord — gdy strategia ma nazwę z domeny („rabat dla seniora”), konfigurację w polach,
        //     kilka metod (np. discount() i description()), własne testy albo czytelny toString().
        // PUŁAPKA: lambda nie ma sensownego toString() — wydruk to coś w rodzaju
        // Patterns01Strategy$$Lambda$14/0x00000008000c1840@1b2c3d (zależy od uruchomienia). Do logów i
        // komunikatów dla użytkownika potrzebujesz nazwy: dodaj ją osobno albo użyj rekordu.
    }

    // =================================================================================================
    // 5. ENUM Z LAMBDĄ W POLU — zamknięty zbiór strategii
    // =================================================================================================

    /**
     * Strategia kosztu dostawy trzymana w stałej enum. ToIntFunction (to int function = funkcja
     * zwracająca int) to gotowy interfejs z JDK: metoda applyAsInt = zastosuj i zwróć int.
     */
    enum ShippingMethod {
        COURIER("Kurier", cart -> 15 + cart.weightKg()),                       // 15 zł + 1 zł za kg
        PARCEL_LOCKER("Paczkomat", cart -> cart.weightKg() <= 25 ? 10 : 25),   // locker = skrytka
        PICKUP("Odbiór osobisty", cart -> 0);

        private final String label;                 // label = etykieta (nazwa dla klienta)
        private final ToIntFunction<Cart> price;    // price = cena; strategia siedzi w polu

        ShippingMethod(String label, ToIntFunction<Cart> price) {
            this.label = label;
            this.price = price;
        }

        int cost(Cart cart) {                       // cost = koszt
            return price.applyAsInt(cart);
        }

        String label() {
            return label;
        }
    }

    /**
     * 5. Gdy zbiór strategii jest ZAMKNIĘTY i znany z góry (trzy sposoby dostawy), enum jest wygodny:
     * kompilator zna wszystkie stałe, valueOf zamienia nazwę na stałą, values() wylicza wszystkie.
     */
    static void enumWithLambda() {
        section("5. Enum z lambdą: zamknięty zbiór strategii");

        Cart parcel = new Cart(120, 8);
        for (ShippingMethod method : ShippingMethod.values()) {
            show("dostawa: " + method.label(), method.cost(parcel));
        }
        // WYNIK: dostawa: Kurier → 23
        // WYNIK: dostawa: Paczkomat → 10
        // WYNIK: dostawa: Odbiór osobisty → 0
        show("paczka 30 kg w paczkomacie", ShippingMethod.PARCEL_LOCKER.cost(new Cart(120, 30)));
        // WYNIK: paczka 30 kg w paczkomacie → 25
        show("valueOf(\"PICKUP\")", ShippingMethod.valueOf("PICKUP"));
        // WYNIK: valueOf("PICKUP") → PICKUP
        expectThrows("valueOf(\"DRONE\")", () -> ShippingMethod.valueOf("DRONE"));
        // WYNIK: ✔ valueOf("DRONE") → rzucono IllegalArgumentException: No enum constant t22_design_patterns.Patterns01Strategy.ShippingMethod.DRONE

        // ENUM CZY INTERFEJS?
        //   enum      — zbiór zamknięty, każda wartość to „typ” z nazwą, wspólnie z switch i EnumMap
        //               (t08_enums/Enums03ConstantBodies, Enums04EnumMapSet); nikt spoza klasy nie doda stałej.
        //   interfejs — zbiór otwarty: nowa strategia może pojawić się w innym module, w innej bibliotece
        //               albo w teście, bez dotykania istniejącego kodu.
        // DOBRA PRAKTYKA: zacznij od enum, gdy wariantów jest kilka i są częścią Twojego modelu;
        // przejdź na interfejs, gdy ktoś z zewnątrz ma mieć prawo dodać własny wariant.
    }

    // =================================================================================================
    // 6. COMPARATOR — strategia, którą już znasz z JDK
    // =================================================================================================

    /** Metoda przyjmuje STRATEGIĘ SORTOWANIA jako argument (order = kolejność) i zwraca n pierwszych nazw. */
    static List<String> topNames(List<Product> products, Comparator<Product> order, int n) {
        List<Product> copy = new ArrayList<>(products);   // kopia, żeby nie ruszać oryginału
        copy.sort(order);                                 // sort(Comparator) deleguje porównywanie do strategii
        return copy.stream().limit(n).map(Product::name).toList();   // toList = do listy (Java 16+)
    }

    /**
     * 6. {@code Comparator} to interfejs strategii: algorytm sortowania (kontekst: {@code List.sort})
     * jest zawsze ten sam, a zmienia się tylko sposób porównywania dwóch elementów.
     */
    static void comparatorInJdk() {
        section("6. Comparator = strategia porównywania");

        List<Product> products = SampleData.products();
        Comparator<Product> byPrice = Comparator.comparing(Product::price);   // comparing = porównuj według

        show("3 najtańsze", topNames(products, byPrice, 3));
        // WYNIK: 3 najtańsze → [Czekolada gorzka, Oliwa z oliwek, T-shirt bawełniany]
        show("3 najdroższe", topNames(products, byPrice.reversed(), 3));   // reversed = odwrócony
        // WYNIK: 3 najdroższe → [Laptop Pro 14, Smartfon X, Ekspres do kawy]
        show("3 pierwsze alfabetycznie", topNames(products, Comparator.comparing(Product::name), 3));
        // WYNIK: 3 pierwsze alfabetycznie → [Czekolada gorzka, Czysty kod, Ekspres do kawy]

        // Zmiana strategii NIE zmienia algorytmu sortowania. Wyniki remisowe (stock = stan magazynu 0, 0):
        Comparator<Product> byStock = Comparator.comparingInt(Product::stock);   // stock = zapas
        show("według zapasu (remis: kolejność wejściowa)", topNames(products, byStock, 3));
        // WYNIK: według zapasu (remis: kolejność wejściowa) → [Smartfon X, Oliwa z oliwek, Ekspres do kawy]
        show("według zapasu, potem nazwy", topNames(products, byStock.thenComparing(Product::name), 3));
        // WYNIK: według zapasu, potem nazwy → [Oliwa z oliwek, Smartfon X, Ekspres do kawy]

        // Gotowe strategie w JDK:
        List<String> fruit = new ArrayList<>(List.of("ananas", "Banan", "cytryna"));
        fruit.sort(null);    // null = porządek naturalny (Comparable): wielkie litery PRZED małymi
        show("porządek naturalny", fruit);
        // WYNIK: porządek naturalny → [Banan, ananas, cytryna]
        fruit.sort(String.CASE_INSENSITIVE_ORDER);   // case insensitive = bez względu na wielkość liter
        show("CASE_INSENSITIVE_ORDER", fruit);
        // WYNIK: CASE_INSENSITIVE_ORDER → [ananas, Banan, cytryna]

        // PUŁAPKA: sortowanie polskich napisów przez porządek naturalny porównuje kody Unicode, więc
        // „Łódź” trafia za „Zakopane” (Ł ma kod większy niż Z). Dla alfabetu polskiego potrzebna jest strategia
        // oparta o Collator (java.text.Collator, t04_strings) — kolejny dowód, że zamiana strategii wystarcza.
        // INNE STRATEGIE, które już znasz: ThreadPoolExecutor.AbortPolicy / CallerRunsPolicy (co zrobić z zadaniem,
        // gdy kolejka jest pełna), Collectors (strategia zbierania wyniku strumienia), a w Springu np.
        // PasswordEncoder — interfejs z wieloma wymiennymi algorytmami haszowania haseł.
    }

    // =================================================================================================
    // 7. STRATEGIA + WSTRZYKIWANIE ZALEŻNOŚCI + TESTY
    // =================================================================================================

    /** Paragon: goods = towary, discount = rabat, shipping = dostawa, total = suma. */
    record Receipt(int goods, int discount, int shipping, int total) {
    }

    /**
     * Usługa dostaje strategie w KONSTRUKTORZE (constructor injection = wstrzykiwanie przez konstruktor).
     * Sama niczego nie tworzy przez new — to zapowiedź wzorca z Patterns08DependencyInjection.
     */
    static final class OrderService {
        private final DiscountPolicy discountPolicy;
        private final ShippingMethod shippingMethod;

        OrderService(DiscountPolicy discountPolicy, ShippingMethod shippingMethod) {
            this.discountPolicy = discountPolicy;
            this.shippingMethod = shippingMethod;
        }

        Receipt receipt(Cart cart) {
            int discount = discountPolicy.discount(cart);
            int shipping = shippingMethod.cost(cart);
            return new Receipt(cart.amount(), discount, shipping, cart.amount() - discount + shipping);
        }
    }

    /**
     * 7. Strategia ułatwia testy: w teście podstawiasz prostą atrapę (fake = udawana wersja), która
     * zwraca stałą wartość — wynik nie zależy od reguł biznesowych, więc test jest krótki i stabilny.
     */
    static void diAndTests() {
        section("7. Strategia, wstrzykiwanie i testowanie");

        // „Produkcja”: prawdziwe reguły z rejestru.
        OrderService production = new OrderService(policyFromConfig(policyRegistry(), "VIP"), ShippingMethod.COURIER);
        show("paragon (VIP + kurier, 200 zł, 4 kg)", production.receipt(new Cart(200, 4)));
        // WYNIK: paragon (VIP + kurier, 200 zł, 4 kg) → Receipt[goods=200, discount=20, shipping=19, total=199]

        // „Test”: stały rabat 10 zł i odbiór osobisty — sprawdzamy TYLKO sposób składania paragonu.
        OrderService underTest = new OrderService(cart -> 10, ShippingMethod.PICKUP);   // under test = testowany
        Receipt expected = new Receipt(100, 10, 0, 90);
        show("test: paragon zgodny z oczekiwanym", underTest.receipt(new Cart(100, 3)).equals(expected));
        // WYNIK: test: paragon zgodny z oczekiwanym → true

        // W Springu to samo robi kontener: wstrzykuje implementację interfejsu do konstruktora, a gdy
        // implementacji jest kilka, potrafi wstrzyknąć wszystkie naraz jako listę albo mapę
        // (nazwa beana → implementacja). Reguły wyboru: SpringLearning, a tu t34_toward_spring/Spring01IocContainer.
        // DOBRA PRAKTYKA: usługa zależy od INTERFEJSU strategii (DIP — zasada odwrócenia zależności,
        // CleanCode02Solid), nigdy od konkretnej klasy. Wtedy zamiana strategii w teście to jedna linia.
    }

    // =================================================================================================
    // 8. PUŁAPKI
    // =================================================================================================

    /** Strategia ze stanem: kupon jednorazowy zapamiętuje, że został użyty. */
    static final class OneTimeCoupon implements DiscountPolicy {
        private boolean used;   // used = użyty; zmienne pole — to jest „stan”

        @Override
        public int discount(Cart cart) {
            if (used) {
                return 0;
            }
            used = true;       // efekt uboczny (side effect): wywołanie zmienia obiekt
            return 50;
        }
    }

    /** Interfejs z JEDNĄ implementacją na zawsze — zbędny ceremoniał (ceremony = obrzęd, nadmiar formalności). */
    interface TaxCalculator {
        int tax(int amount);   // tax = podatek
    }

    static final class PolishTaxCalculator implements TaxCalculator {
        @Override
        public int tax(int amount) {
            return amount * 23 / 100;
        }
    }

    /** To samo bez wzorca — zwykła metoda. */
    static int tax(int amount) {
        return amount * 23 / 100;
    }

    /**
     * 8. Typowe błędy: wzorzec „na zapas” (YAGNI — „you aren't gonna need it” = nie będzie ci to potrzebne)
     * oraz strategie ze stanem.
     */
    static void pitfalls() {
        section("8. Pułapki: jedna implementacja na zawsze i strategie ze stanem");

        // PUŁAPKA: interfejs + klasa tylko po to, żeby „był wzorzec Strategy”. Dopóki istnieje jedna
        // implementacja i nikt nie planuje drugiej, zwykła metoda robi to samo, a kod jest krótszy.
        TaxCalculator calculator = new PolishTaxCalculator();
        show("z interfejsem (jedyna implementacja)", calculator.tax(200));
        // WYNIK: z interfejsem (jedyna implementacja) → 46
        show("zwykła metoda", tax(200));
        // WYNIK: zwykła metoda → 46
        // DOBRA PRAKTYKA: wprowadź strategię DOPIERO, gdy pojawi się drugi wariant (albo pierwszy test,
        // który potrzebuje atrapy). Przejście od metody do interfejsu to krótka, bezpieczna refaktoryzacja
        // (refactoring = przebudowa kodu bez zmiany zachowania) — nie musisz robić jej z wyprzedzeniem.

        // PUŁAPKA: strategia ze stanem, współdzielona między klientów.
        DiscountPolicy coupon = new OneTimeCoupon();
        Cart cart = new Cart(200, 4);
        show("pierwszy klient", coupon.discount(cart));
        // WYNIK: pierwszy klient → 50
        show("drugi klient (ta sama strategia!)", coupon.discount(cart));
        // WYNIK: drugi klient (ta sama strategia!) → 0
        // Wynik zależy od KOLEJNOŚCI wywołań, a nie tylko od koszyka. W aplikacji z wieloma wątkami doszedłby
        // jeszcze wyścig (race condition) na polu used (t21_concurrency/Concurrency02RaceConditions).
        // DOBRA PRAKTYKA: strategia powinna być funkcją „argumenty → wynik”, bez ukrytego stanu. Jeśli stan
        // jest naprawdę potrzebny (kupon jednorazowy), trzymaj go w osobnym obiekcie (np. w bazie)
        // i przekaż strategii jako argument albo zależność, żeby było jasne, kto go zmienia.

        // PUŁAPKA: kontekst, który „zagląda” do wnętrza strategii (rzutowanie instanceof na konkretną klasę).
        // To wraca do switch po typie — cały sens wzorca znika. Jeśli kontekst potrzebuje więcej informacji,
        // dodaj ją do interfejsu albo do argumentów metody.
    }

    // =================================================================================================
    // 9. KIEDY UŻYWAĆ, KIEDY NIE
    // =================================================================================================

    /**
     * 9. Podsumowanie decyzji.
     * <pre>
     *   UŻYJ STRATEGII, GDY:                              NIE UŻYWAJ, GDY:
     *   • jest co najmniej 2–3 warianty algorytmu         • wariant jest jeden i nie widać drugiego
     *   • warianty zmieniają się niezależnie od klienta   • różnica to jedna prosta instrukcja if
     *   • wybór ma zapaść w czasie działania programu     • zbiór wariantów jest zamknięty i mały
     *     (konfiguracja, ustawienia użytkownika)            — wystarczy enum ze switch (wyrażeniem)
     *   • chcesz testować każdy wariant osobno            • strategia musiałaby znać wnętrze kontekstu
     *   • kod z długim switch/if-else na typie rośnie
     * </pre>
     */
    static void whenToUse() {
        section("9. Kiedy używać, a kiedy nie");

        note("Strategia = wymienny algorytm za interfejsem; kontekst nie wie, którego używa.");
        // WYNIK:    ℹ Strategia = wymienny algorytm za interfejsem; kontekst nie wie, którego używa.
        note("Zacznij prosto (metoda, if); wprowadź strategię, gdy pojawi się drugi wariant.");
        // WYNIK:    ℹ Zacznij prosto (metoda, if); wprowadź strategię, gdy pojawi się drugi wariant.
        note("Lambda dla krótkich reguł, rekord/klasa dla reguł z nazwą i konfiguracją.");
        // WYNIK:    ℹ Lambda dla krótkich reguł, rekord/klasa dla reguł z nazwą i konfiguracją.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Strategy (strategia) = rodzina wymiennych algorytmów za jednym interfejsem; kontekst deleguje pracę.
     *   • Role: Context (kontekst), Strategy (interfejs), ConcreteStrategy (konkretna strategia).
     *   • Zastępuje rosnący switch/if-else po typie; nowy wariant = nowa klasa/lambda, kontekst bez zmian.
     *   • Nowoczesna Java: interfejs funkcyjny + lambda/referencja do metody; ToIntFunction i inne z JDK.
     *   • Enum z lambdą w polu = zamknięty zbiór strategii; interfejs = zbiór otwarty.
     *   • Wybór strategii (rejestr nazw, konfiguracja) trzymaj w jednym miejscu; nieznana nazwa = wyjątek.
     *   • W JDK: Comparator, RejectedExecutionHandler, Collectors; w Springu: wstrzykiwane implementacje.
     *   • Testy: podstaw atrapę (lambda zwracająca stałą wartość).
     *   • Pułapki: strategia na zapas (YAGNI), współdzielony stan, rzutowanie na konkretną strategię.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie trzy role występują we wzorcu Strategy i która z nich NIE wybiera strategii?
     *   2. Dlaczego rosnący switch po napisie łamie zasadę otwarte/zamknięte?
     *   3. Co wypisze:  System.out.println(new PercentDiscount(10).discount(new Cart(250, 1)));  ?
     *   4. Co wypisze:  System.out.println(ShippingMethod.PARCEL_LOCKER.cost(new Cart(100, 30)));  ?
     *   5. ZNAJDŹ BŁĄD: strategia rabatu ma pole  private int usedCount;  zwiększane przy każdym wywołaniu
     *      discount(), a jedna instancja jest wspólna dla wszystkich klientów sklepu. Co jest nie tak?
     *   6. ZNAJDŹ BŁĄD: interfejs  PriceFormatter  ma jedną implementację  DefaultPriceFormatter  i nikt
     *      nie planuje drugiej. Jaki problem projektowy tu widać?
     *   7. Kiedy wybierzesz enum z lambdą w polu, a kiedy interfejs z osobnymi klasami?
     *   8. Który obiekt z JDK, który już znasz, jest strategią porównywania?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        Cart cart200 = new Cart(200, 4);
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: percent(10) dla 200 zł", 20, () -> exercise1(10).discount(cart200));
        Check.equal("ćw. 1: percent(0) dla 200 zł", 0, () -> exercise1(0).discount(cart200));
        Check.equal("ćw. 2: bonus GOLD, 200 zł", 10, () -> exercise2("GOLD", cart200));
        Check.equal("ćw. 2: bonus PLATINUM, 200 zł", 20, () -> exercise2("PLATINUM", cart200));
        Check.equal("ćw. 2: bonus NONE (nieznany typ)", 0, () -> exercise2("NONE", cart200));
        Check.equal("ćw. 3: najlepszy z (10%, 15 zł)", 20, () -> exercise3(List.of(exercise1(10), c -> 15)).discount(cart200));
        Check.equal("ćw. 3: pusta lista strategii", 0, () -> exercise3(List.of()).discount(cart200));
        Check.equal("ćw. 4: najtańsze", List.of("Czekolada gorzka", "Oliwa z oliwek", "T-shirt bawełniany"),
                () -> exercise4(SampleData.products(), "price-asc"));
        Check.equal("ćw. 4: najdroższe", List.of("Laptop Pro 14", "Smartfon X", "Ekspres do kawy"),
                () -> exercise4(SampleData.products(), "price-desc"));
        Check.equal("ćw. 4: według nazwy", List.of("Czekolada gorzka", "Czysty kod", "Ekspres do kawy"),
                () -> exercise4(SampleData.products(), "name"));
        Check.throwsException("ćw. 4: nieznany tryb", IllegalArgumentException.class,
                () -> exercise4(SampleData.products(), "random"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 20, () -> solution1(10).discount(cart200));
        Check.equal("ćw. 1 (wzorzec): 0%", 0, () -> solution1(0).discount(cart200));
        Check.equal("ćw. 2 (wzorzec): GOLD", 10, () -> solution2("GOLD", cart200));
        Check.equal("ćw. 2 (wzorzec): PLATINUM", 20, () -> solution2("PLATINUM", cart200));
        Check.equal("ćw. 2 (wzorzec): NONE", 0, () -> solution2("NONE", cart200));
        Check.equal("ćw. 3 (wzorzec)", 20, () -> solution3(List.of(solution1(10), c -> 15)).discount(cart200));
        Check.equal("ćw. 3 (wzorzec): pusta lista", 0, () -> solution3(List.of()).discount(cart200));
        Check.equal("ćw. 4 (wzorzec): najtańsze", List.of("Czekolada gorzka", "Oliwa z oliwek", "T-shirt bawełniany"),
                () -> solution4(SampleData.products(), "price-asc"));
        Check.equal("ćw. 4 (wzorzec): najdroższe", List.of("Laptop Pro 14", "Smartfon X", "Ekspres do kawy"),
                () -> solution4(SampleData.products(), "price-desc"));
        Check.equal("ćw. 4 (wzorzec): według nazwy", List.of("Czekolada gorzka", "Czysty kod", "Ekspres do kawy"),
                () -> solution4(SampleData.products(), "name"));
        Check.throwsException("ćw. 4 (wzorzec): nieznany tryb", IllegalArgumentException.class,
                () -> solution4(SampleData.products(), "random"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 11 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zaimplementuj fabrykę strategii rabatu procentowego — zwraca lambdę, która dla
     * koszyka daje {@code amount * p / 100} zł rabatu (dzielenie całkowitoliczbowe).
     * Podpowiedź: interfejs DiscountPolicy jest funkcyjny, więc wystarczy {@code return cart -> ...;}.
     */
    static DiscountPolicy exercise1(int p) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ switch na rejestr strategii. Stary kod:
     * <pre>{@code
     * static int bonus(String type, Cart cart) {
     *     switch (type) {
     *         case "SILVER":   return cart.amount() * 2 / 100;
     *         case "GOLD":     return cart.amount() * 5 / 100;
     *         case "PLATINUM": return cart.amount() * 10 / 100;
     *         default:         return 0;
     *     }
     * }
     * }</pre>
     * Nowy kod: zbuduj mapę nazwa → strategia (użyj {@code exercise1}) i zwróć wynik strategii z mapy;
     * dla nieznanego typu rabat ma wynosić 0 (bez rzucania wyjątku).
     * Podpowiedź: {@code map.getOrDefault(type, cart -> 0)} — getOrDefault = pobierz albo wartość domyślna.
     */
    static int exercise2(String type, Cart cart) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zaimplementuj strategię „najlepsza z kilku” — dla koszyka zwraca największy
     * rabat spośród strategii z listy; dla pustej listy 0.
     * Podpowiedź: {@code policies.stream().mapToInt(p -> p.discount(cart)).max().orElse(0)} — max zwraca
     * OptionalInt (t14_optional/Optional01Basics), orElse = albo wartość domyślna.
     */
    static DiscountPolicy exercise3(List<DiscountPolicy> policies) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć nazwy trzech pierwszych produktów w kolejności wskazanej trybem:
     * "price-asc" (od najtańszego), "price-desc" (od najdroższego), "name" (alfabetycznie, porządek
     * naturalny napisów). Inny tryb → IllegalArgumentException. Zrób to przez mapę trybów na Comparator
     * i użyj {@code topNames} — bez łańcucha if/else.
     * Podpowiedź: {@code Map.of("price-asc", Comparator.comparing(Product::price), ...)} (Map.of = Java 9+).
     */
    static List<String> exercise4(List<Product> products, String mode) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static DiscountPolicy solution1(int p) {
        return cart -> cart.amount() * p / 100;
    }

    static int solution2(String type, Cart cart) {
        Map<String, DiscountPolicy> bonuses = Map.of(
                "SILVER", solution1(2),
                "GOLD", solution1(5),
                "PLATINUM", solution1(10));
        return bonuses.getOrDefault(type, c -> 0).discount(cart);
    }

    static DiscountPolicy solution3(List<DiscountPolicy> policies) {
        return cart -> policies.stream().mapToInt(p -> p.discount(cart)).max().orElse(0);
    }

    static List<String> solution4(List<Product> products, String mode) {
        Map<String, Comparator<Product>> orders = Map.of(
                "price-asc", Comparator.comparing(Product::price),
                "price-desc", Comparator.comparing(Product::price).reversed(),
                "name", Comparator.comparing(Product::name));
        Comparator<Product> order = orders.get(mode);
        if (order == null) {
            throw new IllegalArgumentException("Nieznany tryb sortowania: " + mode);
        }
        return topNames(products, order, 3);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Kontekst (Context), interfejs strategii (Strategy) i konkretne strategie (ConcreteStrategy).
     *      Strategii NIE wybiera kontekst — wybiera ją ktoś z zewnątrz (konfiguracja, klient, kontener DI).
     *   2. Każdy nowy typ wymaga edycji istniejącej metody (modyfikacji działającego kodu), a nie tylko
     *      dodania nowego kodu. Strategia pozwala rozszerzać program przez DODANIE klasy lub lambdy.
     *   3. 25 (250 * 10 / 100).
     *   4. 25 (waga 30 kg przekracza 25 kg, więc paczkomat kosztuje 25 zł).
     *   5. Wspólny stan: wynik zależy od liczby wcześniejszych wywołań, a nie tylko od koszyka. Rabat staje
     *      się nieprzewidywalny, a przy wielu wątkach dochodzi wyścig na polu (niezsynchronizowany licznik).
     *      Strategia powinna być bezstanowa albo dostawać stan jawnie.
     *   6. Wzorzec „na zapas” (YAGNI): interfejs z jedną implementacją to dodatkowy kod bez korzyści.
     *      Zwykła klasa albo metoda wystarczy; interfejs dodasz, gdy pojawi się drugi wariant lub atrapa do testu.
     *   7. Enum — gdy zbiór strategii jest zamknięty i znany z góry; interfejs — gdy ktoś z zewnątrz
     *      (inny moduł, test) ma móc dodać własną strategię.
     *   8. Comparator (używany przez List.sort, Collections.sort, TreeMap, Stream.sorted).
     */
    // </editor-fold>
}
