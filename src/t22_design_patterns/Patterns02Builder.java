package t22_design_patterns;

import helpers.Check;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Builder (budowniczy) — składanie złożonego obiektu krok po kroku
 *        (builder = budowniczy, ten, kto składa; fluent = płynny, „zdanie złożone z kropek”;
 *         telescoping constructors = konstruktory teleskopowe, jeden wydłużony o parametr względem drugiego)
 *
 * W SKRÓCIE:
 *   Gdy obiekt ma wiele parametrów (część obowiązkowa, część opcjonalna), konstruktor z ośmioma
 *   argumentami jest nieczytelny. Builder pozwala podać je po nazwie, w dowolnej kolejności,
 *   a na końcu jednym wywołaniem build() sprawdzić całość i wydać gotowy, niezmienny obiekt.
 *   Java nie ma argumentów nazwanych — builder jest ich zamiennikiem.
 *
 * ANALOGIA: zamówienie kanapki przy ladzie. Nie wykrzykujesz ośmiu parametrów w ustalonej
 *   kolejności („tak, nie, tak, nie, nie, dwa, bez cebuli”). Mówisz: „bułka razowa, szynka, ser,
 *   bez sosu” — i dopiero gdy powiesz „to wszystko”, kanapka powstaje. Builder to ta rozmowa,
 *   a build() to „to wszystko”.
 *
 * JAK TO DZIAŁA:
 *   Role (GoF): Builder (budowniczy) zbiera parametry, Product (produkt) to gotowy obiekt.
 *
 *     klient ──▶ Pizza.builder(LARGE) ──▶ .thinCrust(true) ──▶ .topping("ser") ──▶ .build() ──▶ Pizza
 *                (parametry wymagane)       każda metoda zwraca this (ten sam builder)    walidacja
 *
 *   Wersja z książki GoF miała jeszcze „dyrektora” (Director), który znał przepisy; w Javie prawie
 *   zawsze występuje wersja uproszczona: builder jako zagnieżdżona klasa statyczna produktu
 *   (Effective Java, pozycja 2).
 *
 *   Kolejność rozwiązań: konstruktor teleskopowy → JavaBeans (settery) → Builder.
 *
 * SŁÓWKA:
 *   builder = budowniczy; build = zbuduj; fluent = płynny (łańcuch wywołań); required = wymagany;
 *   optional = opcjonalny; default = domyślny; telescoping = teleskopowy; setter = metoda ustawiająca
 *   pole; validate = sprawdzić poprawność; step = krok; toBuilder = „z powrotem do budowniczego”
 *   (kopia do modyfikacji); crust = ciasto (spód pizzy); topping = dodatek.
 *
 * ZOBACZ TEŻ: t09_records/Records02Constructors (walidacja w konstruktorze rekordu),
 *   t06_oop_basics/Oop06Immutability (obiekty niezmienne), t20_lombok/Lombok03DataValueBuilder (generowany builder),
 *   t06_oop_basics/Oop07NestedClasses (klasa zagnieżdżona), t27_clean_code_pitfalls/CleanCode01Principles,
 *   t22_design_patterns/Patterns03Factory (inne sposoby tworzenia obiektów)
 * </pre>
 */
public class Patterns02Builder {

    public static void main(String[] args) {
        title("Patterns02 — Builder (budowniczy)");

        telescopingProblem();   // telescoping problem = problem konstruktorów teleskopowych
        javaBeansProblem();     // JavaBeans problem = problem półgotowych obiektów z setterami
        classicBuilder();       // classic builder = klasyczny builder
        validationInBuild();    // validation in build = walidacja w build()
        recordWithBuilder();    // record with builder = rekord z builderem i toBuilder
        stepBuilder();          // step builder = builder krokowy (kolejność wymuszona przez kompilator)
        builderInJdk();         // builder in JDK = buildery w bibliotece standardowej
        testDataBuilder();      // test data builder = builder danych testowych
        lombokRecap();          // Lombok recap = powtórka: @Builder z Lomboka
        pitfalls();             // pitfalls = pułapki i kiedy używać
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // WSPÓLNE TYPY: domena „zamawiamy pizzę”
    // =================================================================================================

    /** Rozmiar pizzy. label = etykieta; basePrice = cena podstawowa w zł (bez dodatków). */
    enum Size {
        SMALL("mała", 20), MEDIUM("średnia", 28), LARGE("duża", 36);

        private final String label;
        private final int basePrice;

        Size(String label, int basePrice) {
            this.label = label;
            this.basePrice = basePrice;
        }

        String label() {
            return label;
        }

        int basePrice() {
            return basePrice;
        }
    }

    /** Cena jednej pizzy: cena podstawowa plus 4 zł za każdy dodatek. */
    static int unitPrice(Size size, int toppingCount) {   // unit price = cena jednostkowa; count = liczba
        return size.basePrice() + 4 * toppingCount;
    }

    // =================================================================================================
    // 1. PROBLEM: konstruktory teleskopowe i „pułapka boolean”
    // =================================================================================================

    /**
     * Wersja BEZ wzorca. Kolejne konstruktory „wydłużają się” o jeden parametr i wołają dłuższy
     * (this(...) = wywołaj inny konstruktor tej samej klasy).
     */
    static final class TelescopicPizza {
        private final Size size;
        private final boolean thinCrust;     // cienkie ciasto
        private final boolean extraCheese;   // dodatkowy ser
        private final boolean mushrooms;     // grzyby
        private final boolean pepperoni;     // salami pepperoni
        private final boolean olives;        // oliwki
        private final int quantity;          // liczba sztuk
        private final String note;           // uwaga dla kuchni

        TelescopicPizza(Size size) {
            this(size, false);
        }

        TelescopicPizza(Size size, boolean thinCrust) {
            this(size, thinCrust, false, false, false, false, 1, "");
        }

        TelescopicPizza(Size size, boolean thinCrust, boolean extraCheese, boolean mushrooms,
                        boolean pepperoni, boolean olives) {
            this(size, thinCrust, extraCheese, mushrooms, pepperoni, olives, 1, "");
        }

        TelescopicPizza(Size size, boolean thinCrust, boolean extraCheese, boolean mushrooms,
                        boolean pepperoni, boolean olives, int quantity, String note) {
            this.size = size;
            this.thinCrust = thinCrust;
            this.extraCheese = extraCheese;
            this.mushrooms = mushrooms;
            this.pepperoni = pepperoni;
            this.olives = olives;
            this.quantity = quantity;
            this.note = note;
        }

        String describe() {   // describe = opisz
            List<String> parts = new ArrayList<>();
            parts.add(size.label());
            if (thinCrust) parts.add("cienkie ciasto");
            if (extraCheese) parts.add("extra ser");
            if (mushrooms) parts.add("grzyby");
            if (pepperoni) parts.add("pepperoni");
            if (olives) parts.add("oliwki");
            parts.add("x" + quantity);
            if (!note.isEmpty()) parts.add("uwaga: " + note);
            return String.join(", ", parts);
        }
    }

    /**
     * 1. PROBLEM: przy konstruktorze z ośmioma parametrami wywołanie
     * {@code new TelescopicPizza(Size.LARGE, true, false, true, false, false, 2, "bez cebuli")}
     * jest nieczytelne: co znaczy trzecie {@code false}? Trzeba zajrzeć do definicji konstruktora.
     */
    static void telescopingProblem() {
        section("1. Problem: konstruktory teleskopowe i pułapka boolean");

        TelescopicPizza a = new TelescopicPizza(Size.LARGE, true, false, true, false, false, 2, "bez cebuli");
        show("zamówienie A", a.describe());
        // WYNIK: zamówienie A → duża, cienkie ciasto, grzyby, x2, uwaga: bez cebuli

        // Zamieniamy miejscami dwa sąsiednie boolean (grzyby <-> pepperoni). Kompiluje się bez słowa ostrzeżenia:
        TelescopicPizza b = new TelescopicPizza(Size.LARGE, true, false, false, true, false, 2, "bez cebuli");
        show("zamówienie B (zamienione dwa boolean)", b.describe());
        // WYNIK: zamówienie B (zamienione dwa boolean) → duża, cienkie ciasto, pepperoni, x2, uwaga: bez cebuli

        // PUŁAPKA: „boolean trap” (pułapka boolean) — kilka parametrów tego samego typu obok siebie. Kompilator
        // nie wychwyci zamiany, bo true i false pasują wszędzie. Błąd wychodzi dopiero u klienta, który dostał
        // pizzę z pepperoni zamiast z grzybami. To samo dotyczy kilku liczb int albo kilku String z rzędu.
        // PUŁAPKA: liczba konstruktorów rośnie z kombinacjami („tylko rozmiar i ilość”, „rozmiar i uwaga”...).
        // Przy n opcjonalnych parametrach potrzebowałbyś nawet 2^n konstruktorów, żeby obsłużyć każdą kombinację.
        // Dodanie dziewiątego parametru wymusza zmianę wszystkich wywołań najdłuższego konstruktora.
    }

    // =================================================================================================
    // 2. PROBLEM: JavaBeans — settery i półgotowy obiekt
    // =================================================================================================

    /** Wzorzec „JavaBeans”: pusty konstruktor + settery (setter = metoda ustawiająca jedno pole). */
    static final class PizzaBean {
        private Size size;
        private int quantity;       // domyślnie 0!
        private int toppingCount;

        void setSize(Size size) {
            this.size = size;
        }

        void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        void setToppingCount(int toppingCount) {
            this.toppingCount = toppingCount;
        }

        int total() {
            return unitPrice(size, toppingCount) * quantity;
        }
    }

    /** Przechowalnia zamówień „przyjętych do realizacji”: trzyma REFERENCJE do obiektów, nie kopie. */
    static final class Kitchen {   // kitchen = kuchnia
        private final List<PizzaBean> accepted = new ArrayList<>();   // accepted = przyjęte

        void accept(PizzaBean pizza) {
            accepted.add(pizza);
        }

        int totalToCook() {
            return accepted.stream().mapToInt(PizzaBean::total).sum();
        }
    }

    /**
     * 2. JavaBeans rozwiązuje czytelność (setX jest nazwane), ale tworzy dwa nowe problemy:
     * obiekt przez chwilę istnieje NIEDOKOŃCZONY, a potem jest ZMIENNY dla każdego, kto ma referencję.
     */
    static void javaBeansProblem() {
        section("2. Problem: JavaBeans — półgotowy i zmienny obiekt");

        PizzaBean bean = new PizzaBean();
        bean.setSize(Size.MEDIUM);
        bean.setToppingCount(2);
        // Zapomnieliśmy o setQuantity — nic nas nie ostrzegło. Obiekt jest „półgotowy”:
        show("suma półgotowego zamówienia (ilość = 0)", bean.total());
        // WYNIK: suma półgotowego zamówienia (ilość = 0) → 0

        bean.setQuantity(1);
        Kitchen kitchen = new Kitchen();
        kitchen.accept(bean);
        show("kuchnia ma do zrobienia (zł)", kitchen.totalToCook());
        // WYNIK: kuchnia ma do zrobienia (zł) → 36

        // Ktoś z zewnątrz nadal trzyma referencję i „poprawia” zamówienie już przyjęte do realizacji:
        bean.setQuantity(5);
        show("po cichej zmianie z zewnątrz", kitchen.totalToCook());
        // WYNIK: po cichej zmianie z zewnątrz → 180

        // PUŁAPKA: obiekt zmienny (mutable) po przekazaniu dalej nie jest już Twój. Nie można go
        // bezpiecznie współdzielić między wątkami ani używać jako klucza w mapie (t06_oop_basics/Oop06Immutability).
        // PUŁAPKA: reguły łączące kilka pól („ilość od 1 do 10”, „najwyżej 4 dodatki”) nie mają gdzie się wykonać —
        // każdy setter widzi tylko jedno pole, a obiekt jest „gotowy” w nieznanym momencie.
        // DOBRA PRAKTYKA: budowanie (zmienne, krok po kroku) oddziel od produktu (niezmienny, zawsze kompletny).
        // Builder robi dokładnie to.
    }

    // =================================================================================================
    // 3. KLASYCZNY BUILDER: zagnieżdżona klasa statyczna
    // =================================================================================================

    /** Produkt: niezmienny (wszystkie pola final), konstruktor prywatny — jedyna droga to builder. */
    static final class Pizza {
        private final Size size;
        private final boolean thinCrust;
        private final List<String> toppings;
        private final int quantity;
        private final String note;

        private Pizza(Builder b) {
            // 1) kopiujemy pola z buildera (listę — przez List.copyOf, żeby builder nie dzielił jej z produktem)
            this.size = b.size;
            this.thinCrust = b.thinCrust;
            this.toppings = List.copyOf(b.toppings);
            this.quantity = b.quantity;
            this.note = b.note;
            // 2) dopiero potem sprawdzamy reguły — NA POLACH PRODUKTU, nie buildera
            if (quantity < 1 || quantity > 10) {
                throw new IllegalStateException("Liczba pizz musi być od 1 do 10, a jest: " + quantity);
            }
            if (toppings.size() > 4) {
                throw new IllegalStateException("Maksymalnie 4 dodatki, podano: " + toppings.size());
            }
        }

        static Builder builder(Size size) {   // fabryka buildera; parametr wymagany podajemy od razu
            return new Builder(size);
        }

        Size size() {
            return size;
        }

        List<String> toppings() {
            return toppings;
        }

        int quantity() {
            return quantity;
        }

        int total() {
            return unitPrice(size, toppings.size()) * quantity;
        }

        @Override
        public String toString() {
            return size.label() + (thinCrust ? ", cienkie ciasto" : "") + ", " + toppings + ", x" + quantity
                    + (note.isEmpty() ? "" : ", uwaga: " + note) + ", razem " + total() + " zł";
        }

        /** Builder: zbiera wartości, każda metoda ustawiająca zwraca this (to jest „fluent interface”). */
        static final class Builder {
            private final Size size;                              // wymagany — tylko przez konstruktor
            private boolean thinCrust;                            // opcjonalny: domyślnie false
            private final List<String> toppings = new ArrayList<>();
            private int quantity = 1;                             // opcjonalny: wartość domyślna 1
            private String note = "";

            private Builder(Size size) {
                this.size = Objects.requireNonNull(size, "rozmiar jest wymagany");   // requireNonNull = wymagaj nie-null
            }

            Builder thinCrust(boolean thinCrust) {
                this.thinCrust = thinCrust;
                return this;
            }

            Builder topping(String name) {
                this.toppings.add(name);
                return this;
            }

            Builder quantity(int quantity) {
                this.quantity = quantity;
                return this;
            }

            Builder note(String note) {
                this.note = note;
                return this;
            }

            Pizza build() {
                return new Pizza(this);
            }
        }
    }

    /**
     * 3. To samo zamówienie co w sekcji 1, ale każdy parametr ma nazwę. Kod czyta się jak zdanie,
     * kolejność wywołań jest dowolna, a pominięte parametry mają wartości domyślne.
     */
    static void classicBuilder() {
        section("3. Klasyczny builder: zagnieżdżona klasa statyczna");

        Pizza pizza = Pizza.builder(Size.LARGE)
                .thinCrust(true)
                .topping("grzyby")
                .topping("papryka")
                .quantity(2)
                .note("bez cebuli")
                .build();
        show("pizza", pizza);
        // WYNIK: pizza → duża, cienkie ciasto, [grzyby, papryka], x2, uwaga: bez cebuli, razem 88 zł

        // Tylko to, co potrzebne — reszta domyślna:
        show("najprostsza pizza", Pizza.builder(Size.SMALL).build());
        // WYNIK: najprostsza pizza → mała, [], x1, razem 20 zł

        // Ten sam builder można ponownie rozbudować i zbudować DRUGI, niezależny obiekt:
        Pizza.Builder base = Pizza.builder(Size.MEDIUM).topping("ser");
        Pizza one = base.build();
        Pizza two = base.topping("szynka").build();
        show("pierwsza (zbudowana wcześniej)", one.toppings());
        // WYNIK: pierwsza (zbudowana wcześniej) → [ser]
        show("druga", two.toppings());
        // WYNIK: druga → [ser, szynka]

        // DOBRA PRAKTYKA: produkt jest niezmienny, a konstruktor prywatny — nie da się go obejść ani zepsuć.
        // DOBRA PRAKTYKA: parametry WYMAGANE podawaj w konstruktorze buildera (tu: rozmiar), a opcjonalne w metodach.
        // Wtedy zapomnieć o wymaganym parametrze się nie da — kod się nie skompiluje.
        // DOBRA PRAKTYKA: kolekcje z buildera kopiuj (List.copyOf) przy budowie produktu. Gdyby produkt
        // trzymał tę samą listę co builder, kolejne wywołanie topping(...) po zbudowaniu zmieniłoby
        // gotową pizzę „za plecami” — wynik „pierwsza” powyżej pokazuje, że tak się nie dzieje.
    }

    // =================================================================================================
    // 4. WALIDACJA W build()
    // =================================================================================================

    /**
     * 4. Wszystkie reguły sprawdzamy w jednym miejscu, na końcu, gdy znamy komplet pól: „wszystko albo nic”.
     * Nie powstaje obiekt, który łamie reguły. Typowe wyjątki: IllegalStateException (illegal state =
     * niedozwolony stan) lub IllegalArgumentException (niedozwolony argument).
     */
    static void validationInBuild() {
        section("4. Walidacja w build()");

        expectThrows("zero pizz", () -> Pizza.builder(Size.SMALL).quantity(0).build());
        // WYNIK: ✔ zero pizz → rzucono IllegalStateException: Liczba pizz musi być od 1 do 10, a jest: 0
        expectThrows("jedenaście pizz", () -> Pizza.builder(Size.SMALL).quantity(11).build());
        // WYNIK: ✔ jedenaście pizz → rzucono IllegalStateException: Liczba pizz musi być od 1 do 10, a jest: 11
        expectThrows("pięć dodatków", () -> Pizza.builder(Size.LARGE)
                .topping("a").topping("b").topping("c").topping("d").topping("e").build());
        // WYNIK: ✔ pięć dodatków → rzucono IllegalStateException: Maksymalnie 4 dodatki, podano: 5
        expectThrows("brak rozmiaru (null)", () -> Pizza.builder(null));
        // WYNIK: ✔ brak rozmiaru (null) → rzucono NullPointerException: rozmiar jest wymagany

        show("granica: 10 pizz i 4 dodatki", Pizza.builder(Size.SMALL).quantity(10)
                .topping("a").topping("b").topping("c").topping("d").build().total());
        // WYNIK: granica: 10 pizz i 4 dodatki → 360

        // DOBRA PRAKTYKA: waliduj w build() (albo w konstruktorze produktu), nie w poszczególnych metodach
        // ustawiających — tylko tam widać wszystkie pola naraz, więc da się sprawdzić reguły łączące pola.
        // DOBRA PRAKTYKA: komunikat błędu zawiera NAZWĘ reguły i FAKTYCZNĄ wartość („a jest: 0”) — wtedy
        // nie trzeba uruchamiać debuggera, żeby zrozumieć problem.
        // PUŁAPKA: sprawdzenie pól buildera, a potem utworzenie produktu z innych danych (np. po zmianie
        // przez drugi wątek) to klasyczny błąd typu „sprawdzono co innego niż użyto”. Najbezpieczniej: najpierw
        // skopiuj pola do produktu, potem sprawdź pola PRODUKTU (tak robi konstruktor Pizza powyżej).
    }

    // =================================================================================================
    // 5. REKORD + BUILDER, toBuilder
    // =================================================================================================

    /**
     * Rekord z walidacją w konstruktorze kanonicznym (compact constructor = skrócony zapis, t09_records/Records02Constructors)
     * i builderem. Rekord jest niezmienny „z urzędu”, ale nie ma ani wartości domyślnych, ani nazwanych argumentów.
     */
    record Mail(String to, String subject, String body, List<String> cc, boolean urgent) {

        Mail {   // compact constructor: ciało wykonuje się przed przypisaniem pól
            Objects.requireNonNull(to, "adresat jest wymagany");
            if (to.isBlank()) {   // isBlank = pusty lub same spacje (Java 11+)
                throw new IllegalArgumentException("Adres nie może być pusty");
            }
            Objects.requireNonNull(subject, "temat jest wymagany");
            cc = List.copyOf(cc);   // kopia obronna: rekord niezmienny także wewnątrz
        }

        static Builder builder(String to, String subject) {
            return new Builder(to, subject);
        }

        /** toBuilder = „zamień z powrotem na budowniczego”: kopia wartości do zmiany kilku pól. */
        Builder toBuilder() {
            Builder builder = new Builder(to, subject).body(body).urgent(urgent);
            cc.forEach(builder::cc);   // forEach = dla każdego; builder::cc = referencja do metody
            return builder;
        }

        static final class Builder {
            private final String to;
            private String subject;
            private String body = "";
            private final List<String> cc = new ArrayList<>();
            private boolean urgent;

            private Builder(String to, String subject) {
                this.to = to;
                this.subject = subject;
            }

            Builder subject(String subject) {
                this.subject = subject;
                return this;
            }

            Builder body(String body) {
                this.body = body;
                return this;
            }

            Builder cc(String address) {
                this.cc.add(address);
                return this;
            }

            Builder urgent(boolean urgent) {
                this.urgent = urgent;
                return this;
            }

            Mail build() {
                return new Mail(to, subject, body, cc, urgent);
            }
        }
    }

    /**
     * 5. Builder + rekord: walidacja jest w rekordzie, więc działa także przy bezpośrednim {@code new Mail(...)}.
     * Zmiana pojedynczego pola obiektu niezmiennego = skopiuj przez toBuilder() i zbuduj nowy.
     */
    static void recordWithBuilder() {
        section("5. Rekord + builder, toBuilder()");

        Mail mail = Mail.builder("anna@example.com", "Faktura 17").body("Dzień dobry").cc("ksiegowosc@example.com").build();
        show("mail", mail);
        // WYNIK: mail → Mail[to=anna@example.com, subject=Faktura 17, body=Dzień dobry, cc=[ksiegowosc@example.com], urgent=false]

        // Zmiana jednego pola: oryginał pozostaje bez zmian, powstaje NOWY obiekt.
        Mail urgentCopy = mail.toBuilder().urgent(true).subject("PILNE: Faktura 17").build();
        show("kopia z toBuilder()", urgentCopy);
        // WYNIK: kopia z toBuilder() → Mail[to=anna@example.com, subject=PILNE: Faktura 17, body=Dzień dobry, cc=[ksiegowosc@example.com], urgent=true]
        show("oryginał nietknięty", mail.urgent());
        // WYNIK: oryginał nietknięty → false

        expectThrows("pusty adres", () -> Mail.builder("  ", "x").build());
        // WYNIK: ✔ pusty adres → rzucono IllegalArgumentException: Adres nie może być pusty
        expectThrows("bezpośredni konstruktor też waliduje", () -> new Mail(null, "x", "", List.of(), false));
        // WYNIK: ✔ bezpośredni konstruktor też waliduje → rzucono NullPointerException: adresat jest wymagany

        // Alternatywa dla pojedynczych zmian: „wither” (with = z; metoda zwracająca kopię z jednym zmienionym polem):
        //   Mail withSubject(String s) { return new Mail(to, s, body, cc, urgent); }
        // DOBRA PRAKTYKA: przy 2–3 polach wither jest prostszy; przy 5+ polach toBuilder() jest krótszy
        // i nie wymaga kolejnej metody za każdym razem, gdy dodasz pole.
        // PUŁAPKA: kopia obronna listy (List.copyOf) jest konieczna. Rekord daje niezmienność pól, ale NIE
        // niezmienność obiektów, na które pola wskazują — zmienna lista w rekordzie pozostałaby zmienna.
    }

    // =================================================================================================
    // 6. BUILDER KROKOWY (step builder)
    // =================================================================================================

    /** Każdy krok to osobny interfejs, który udostępnia TYLKO następną dozwoloną metodę. */
    interface NeedsTo {
        NeedsSubject to(String address);
    }

    interface NeedsSubject {
        NeedsBody subject(String subject);
    }

    interface NeedsBody {
        Ready body(String body);
    }

    /** Ostatni krok: wymagane parametry podane — można dodawać opcjonalne albo zbudować. */
    interface Ready {
        Ready cc(String address);

        Ready urgent();

        Mail build();
    }

    /** Jedna klasa implementuje wszystkie kroki; z zewnątrz widać zawsze tylko bieżący interfejs. */
    static final class MailSteps implements NeedsTo, NeedsSubject, NeedsBody, Ready {
        private String to;
        private String subject;
        private String body;
        private final List<String> cc = new ArrayList<>();
        private boolean urgent;

        private MailSteps() {
        }

        static NeedsTo mail() {
            return new MailSteps();
        }

        @Override
        public NeedsSubject to(String address) {
            this.to = address;
            return this;
        }

        @Override
        public NeedsBody subject(String subject) {
            this.subject = subject;
            return this;
        }

        @Override
        public Ready body(String body) {
            this.body = body;
            return this;
        }

        @Override
        public Ready cc(String address) {
            cc.add(address);
            return this;
        }

        @Override
        public Ready urgent() {
            this.urgent = true;
            return this;
        }

        @Override
        public Mail build() {
            return new Mail(to, subject, body, cc, urgent);
        }
    }

    /**
     * 6. Step builder: kompilator wymusza KOLEJNOŚĆ i KOMPLETNOŚĆ parametrów wymaganych.
     * Zapisu {@code MailSteps.mail().subject("x")} nie da się skompilować (błąd kompilacji: cannot find symbol —
     * interfejs NeedsTo nie ma metody subject).
     */
    static void stepBuilder() {
        section("6. Builder krokowy (step builder)");

        Mail mail = MailSteps.mail()
                .to("jan@example.com")
                .subject("Zamówienie")
                .body("Potwierdzenie w załączniku")
                .cc("szef@example.com")
                .urgent()
                .build();
        show("mail z kroków", mail);
        // WYNIK: mail z kroków → Mail[to=jan@example.com, subject=Zamówienie, body=Potwierdzenie w załączniku, cc=[szef@example.com], urgent=true]

        // Cena: dużo kodu pomocniczego (interfejs na każdy krok). Dlatego step builder opłaca się tylko tam,
        // gdzie parametrów wymaganych jest kilka, a błąd „zapomniałem” jest kosztowny.
        // Zwykły builder z wymaganymi parametrami w konstruktorze (sekcja 3) wystarcza w 90% przypadków.
        // PUŁAPKA: step builder zamraża kolejność kroków; jeśli klient chce podać temat przed adresatem,
        // musi się dostosować — to ograniczenie jest ceną bezpieczeństwa.
    }

    // =================================================================================================
    // 7. BUILDERY W JDK I W SPRINGU
    // =================================================================================================

    /**
     * 7. Buildery, które już znasz lub zaraz poznasz: StringBuilder (t04_strings), Stream.builder(),
     * HttpRequest.newBuilder() (Java 11+, pakiet java.net.http — t28_networking_http). Budowanie żądania
     * NIE wysyła niczego przez sieć — to tylko zebranie parametrów.
     */
    static void builderInJdk() {
        section("7. Buildery w JDK");

        StringBuilder sb = new StringBuilder()
                .append("Zamówienie ").append(7).append(": ").append(3).append(" szt.");   // append = dołącz
        show("StringBuilder", sb.toString());
        // WYNIK: StringBuilder → Zamówienie 7: 3 szt.

        Stream.Builder<String> streamBuilder = Stream.builder();   // Stream.builder = budowniczy strumienia
        streamBuilder.add("a").add("b").add("c");
        show("Stream.builder", streamBuilder.build().toList());   // toList = Java 16+
        // WYNIK: Stream.builder → [a, b, c]

        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:8080/orders"))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        show("żądanie HTTP", request.method() + " " + request.uri());
        // WYNIK: żądanie HTTP → GET http://localhost:8080/orders
        show("nagłówek Accept", request.headers().firstValue("Accept").orElse("brak"));
        // WYNIK: nagłówek Accept → application/json

        // W Springu: UriComponentsBuilder (składanie adresów URL), ResponseEntity.ok().header(...).body(...),
        // MockMvcRequestBuilders (testy), RestClient/WebClient.builder() — wszystko to ten sam wzorzec.
        // Inne w JDK: ProcessBuilder (uruchamianie programów), Locale.Builder
        // oraz Stream.Builder, który pokazaliśmy wyżej.
        // PUŁAPKA: StringBuilder i Stream.Builder są ZMIENNE i nie są bezpieczne dla wielu wątków; produkt
        // (String, Stream) powstaje dopiero po toString()/build().
    }

    // =================================================================================================
    // 8. TESTOWALNOŚĆ: builder danych testowych
    // =================================================================================================

    /** Prosta reguła do przetestowania: pilne maile idą do kolejki PILNE, reszta do ZWYKŁA. */
    static String queueFor(Mail mail) {   // queue = kolejka
        return mail.urgent() ? "PILNE" : "ZWYKŁA";
    }

    /** „Builder danych testowych”: poprawny obiekt domyślny — test zmienia tylko to, co go interesuje. */
    static Mail.Builder aMail() {
        return Mail.builder("test@example.com", "Temat testowy");
    }

    /**
     * 8. W testach builder z sensownymi wartościami domyślnymi to wielka oszczędność: test mówi tylko o tym,
     * co jest istotne („to mail PILNY”), a nie powtarza w każdym teście sześciu parametrów.
     */
    static void testDataBuilder() {
        section("8. Testowalność: builder danych testowych");

        show("test: pilny mail → kolejka", queueFor(aMail().urgent(true).build()));
        // WYNIK: test: pilny mail → kolejka → PILNE
        show("test: domyślny mail → kolejka", queueFor(aMail().build()));
        // WYNIK: test: domyślny mail → kolejka → ZWYKŁA

        // DOBRA PRAKTYKA: w testach nazwij pomocnika aMail()/anOrder() — wtedy test czyta się jak opis:
        //   queueFor(aMail().urgent(true).build())  ==  „dla pilnego maila…”.
        // Wzorzec nazywa się „Test Data Builder” (builder danych testowych); wariant z gotowymi obiektami
        // to „Object Mother” (matka obiektów). Szersze omówienie: t25_testing/Testing03TestableDesign.
    }

    // =================================================================================================
    // 9. LOMBOK — POWTÓRKA
    // =================================================================================================

    /**
     * 9. Pisanie buildera ręcznie to dużo powtarzalnego kodu. Lombok generuje go z adnotacji
     * (t20_lombok/Lombok03DataValueBuilder). W tym kursie lekcje poza t20 nie używają Lomboka,
     * więc przykład kodu jest w komentarzu wewnątrz metody.
     */
    static void lombokRecap() {
        section("9. Lombok @Builder — powtórka");

        // Tak wyglądałaby klasa z Lomboka (kod tylko w komentarzu):
        //   @Builder(toBuilder = true)
        //   public class Pizza {
        //       private final Size size;
        //       @Builder.Default private final int quantity = 1;
        //       @Singular private final List<String> toppings;
        //   }
        //   Pizza p = Pizza.builder().size(Size.LARGE).topping("ser").topping("szynka").build();

        note("@Builder generuje klasę Builder; toBuilder = true dodaje toBuilder().");
        // WYNIK:    ℹ @Builder generuje klasę Builder; toBuilder = true dodaje toBuilder().
        note("@Builder.Default = wartość domyślna; @Singular = dodawanie elementów po jednym.");
        // WYNIK:    ℹ @Builder.Default = wartość domyślna; @Singular = dodawanie elementów po jednym.

        // Co Lombok daje: mniej kodu, nowe pole = jedna linijka. Czego NIE daje: wymagane parametry w konstruktorze
        // buildera (domyślnie każde pole jest opcjonalne, a brak wartości to null/0 — sprawdza je dopiero
        // adnotacja @NonNull w czasie działania), ani kroków wymuszanych przez kompilator.
        // Rekord + ręczny builder (sekcja 5) jest dobrym wyborem, gdy chcesz uniknąć Lomboka.
    }

    // =================================================================================================
    // 10. PUŁAPKI I KIEDY UŻYWAĆ
    // =================================================================================================

    /**
     * 10. Najczęstsze błędy i tabela decyzji.
     * <pre>
     *   UŻYJ BUILDERA, GDY:                                NIE UŻYWAJ, GDY:
     *   • parametrów jest 4 lub więcej                     • są 2–3 parametry — wystarczy konstruktor/rekord
     *   • wiele z nich jest opcjonalnych                   • wszystkie parametry są wymagane i różnych typów
     *   • kilka parametrów ma ten sam typ (boolean trap)   • obiekt jest prosty i zmienny z natury
     *   • obiekt ma być niezmienny, ale składany etapami   • wystarczy statyczna metoda fabrykująca (Patterns03)
     *   • reguły łączą kilka pól (walidacja w build())
     * </pre>
     */
    static void pitfalls() {
        section("10. Pułapki i kiedy używać");

        // PUŁAPKA: builder dla dwóch pól. Kod  Point.builder().x(1).y(2).build()  jest dłuższy i mniej
        // czytelny niż  new Point(1, 2)  albo rekord Point(int x, int y). Wzorzec ma być lekarstwem
        // na ból, a nie ozdobą (YAGNI — nie dodawaj „na zapas”).
        // PUŁAPKA: builder nie wymusza kompletności. Jeśli wymagany parametr trafi do metody opcjonalnej,
        // pominięcie go wyjdzie dopiero w build() (w czasie działania). Wymagane daj do konstruktora buildera.
        // PUŁAPKA: builder jest zmienny i NIE jest bezpieczny dla wielu wątków — nie współdziel jednego
        // buildera między wątkami; niech każdy ma własny.
        // PUŁAPKA: zapomniana walidacja w build() = „niby niezmienny” obiekt o niepoprawnych danych.
        // PUŁAPKA: kopiowanie referencji do zmiennej kolekcji z buildera do produktu (bez List.copyOf).

        note("Builder zastępuje argumenty nazwane, których Java nie ma.");
        // WYNIK:    ℹ Builder zastępuje argumenty nazwane, których Java nie ma.
        note("Wymagane parametry do konstruktora buildera, opcjonalne do metod, reguły do build().");
        // WYNIK:    ℹ Wymagane parametry do konstruktora buildera, opcjonalne do metod, reguły do build().
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Builder (budowniczy) = składanie obiektu krok po kroku; build() waliduje i zwraca produkt.
     *   • Rozwiązuje: konstruktory teleskopowe, pułapkę boolean, półgotowe obiekty z setterami.
     *   • Struktura: produkt niezmienny + prywatny konstruktor + zagnieżdżona klasa statyczna Builder.
     *   • Metody buildera zwracają this (fluent); wymagane parametry w konstruktorze buildera.
     *   • Waliduj w build()/konstruktorze produktu, na polach PRODUKTU; kolekcje kopiuj (List.copyOf).
     *   • Rekord + builder: walidacja w compact constructor, toBuilder() do zmian pojedynczych pól.
     *   • Step builder: osobny interfejs na każdy krok — kompilator pilnuje kolejności (dużo kodu).
     *   • W JDK: StringBuilder, Stream.builder(), HttpRequest.newBuilder(), ProcessBuilder.
     *   • Lombok: @Builder (+ toBuilder, @Builder.Default, @Singular) generuje kod za Ciebie.
     *   • W testach: builder danych testowych z domyślnymi wartościami (aMail()).
     *   • Nie używaj dla 2–3 parametrów (YAGNI); builder jest zmienny i nie dla wielu wątków.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie trzy problemy ma konstruktor z ośmioma parametrami, w tym kilkoma typu boolean?
     *   2. Dlaczego obiekt zbudowany przez settery (JavaBeans) może być „półgotowy”?
     *   3. Dlaczego parametry wymagane najlepiej podawać w konstruktorze buildera?
     *   4. Co wypisze:  Pizza.Builder b = Pizza.builder(Size.MEDIUM).topping("ser");
     *                   Pizza p1 = b.build();  b.topping("szynka");
     *                   System.out.println(p1.toppings());  ?
     *   5. Co wypisze:  System.out.println(Pizza.builder(Size.SMALL).quantity(3).topping("ser").build().total());  ?
     *   6. ZNAJDŹ BŁĄD: konstruktor produktu robi  this.toppings = b.toppings;  (bez kopiowania listy).
     *      Co się stanie po zbudowaniu pizzy i dalszej pracy z tym samym builderem?
     *   7. ZNAJDŹ BŁĄD: metoda build() najpierw sprawdza pola BUILDERA, a potem tworzy produkt z pól buildera.
     *      Dlaczego to ryzykowne i jak to poprawić?
     *   8. Który z buildera StringBuilder, HttpRequest.newBuilder(), Stream.builder() wysyła coś przez sieć?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Rekord do ćwiczenia 4: zniżka. Walidacja w konstruktorze kanonicznym. */
    record Discount(String code, int percent, int minAmount, boolean stackable) {
        Discount {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("Kod zniżki jest wymagany");
            }
            if (percent < 1 || percent > 90) {
                throw new IllegalArgumentException("Procent musi być od 1 do 90, a jest: " + percent);
            }
        }
    }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: suma pizzy", 108, () -> exercise1().total());
        Check.equal("ćw. 1: dodatki", List.of("ser", "szynka"), () -> exercise1().toppings());
        Check.equal("ćw. 2: suma pizzy", 28, () -> exercise2().total());
        Check.equal("ćw. 2: dodatki", List.of("extra ser", "pepperoni"), () -> exercise2().toppings());
        Mail base = Mail.builder("a@example.com", "Temat").cc("b@example.com").build();
        Check.equal("ćw. 3: pilny", true, () -> exercise3(base).urgent());
        Check.equal("ćw. 3: cc", List.of("b@example.com", "szef@example.com"), () -> exercise3(base).cc());
        Check.equal("ćw. 3: oryginał bez zmian", false, () -> {
            exercise3(base);
            return base.urgent();
        });
        Check.equal("ćw. 4: zniżka", new Discount("WIOSNA", 15, 100, true),
                () -> new Exercise4Builder("WIOSNA").percent(15).minAmount(100).stackable(true).build());
        Check.equal("ćw. 4: wartości domyślne", new Discount("STALA", 10, 0, false),
                () -> new Exercise4Builder("STALA").percent(10).build());
        Check.throwsException("ćw. 4: procent 0", IllegalArgumentException.class,
                () -> new Exercise4Builder("BAD").percent(0).build());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec): suma", 108, () -> solution1().total());
        Check.equal("ćw. 1 (wzorzec): dodatki", List.of("ser", "szynka"), () -> solution1().toppings());
        Check.equal("ćw. 2 (wzorzec): suma", 28, () -> solution2().total());
        Check.equal("ćw. 2 (wzorzec): dodatki", List.of("extra ser", "pepperoni"), () -> solution2().toppings());
        Check.equal("ćw. 3 (wzorzec): pilny", true, () -> solution3(base).urgent());
        Check.equal("ćw. 3 (wzorzec): cc", List.of("b@example.com", "szef@example.com"), () -> solution3(base).cc());
        Check.equal("ćw. 3 (wzorzec): oryginał bez zmian", false, () -> {
            solution3(base);
            return base.urgent();
        });
        Check.equal("ćw. 4 (wzorzec): zniżka", new Discount("WIOSNA", 15, 100, true),
                () -> new Solution4Builder("WIOSNA").percent(15).minAmount(100).stackable(true).build());
        Check.equal("ćw. 4 (wzorzec): wartości domyślne", new Discount("STALA", 10, 0, false),
                () -> new Solution4Builder("STALA").percent(10).build());
        Check.throwsException("ćw. 4 (wzorzec): procent 0", IllegalArgumentException.class,
                () -> new Solution4Builder("BAD").percent(0).build());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 10 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zbuduj builderem średnią pizzę z dodatkami „ser” i „szynka”, w ilości 3 sztuk.
     * Podpowiedź: {@code Pizza.builder(Size.MEDIUM).topping(...)...build()}. Cena jednej: 28 + 2*4 = 36 zł.
     */
    static Pizza exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ wywołanie konstruktora teleskopowego na builder (jedna mała pizza).
     * Stary kod:
     * <pre>{@code
     * new TelescopicPizza(Size.SMALL, false, true, false, true, false, 1, "")
     * }</pre>
     * Znaczenie: cienkie ciasto = nie, extra ser = tak, grzyby = nie, pepperoni = tak, oliwki = nie.
     * Dodatki nazwij „extra ser” i „pepperoni” (w tej kolejności).
     * Podpowiedź: liczba sztuk 1 jest wartością domyślną, więc nie musisz jej podawać.
     */
    static Pizza exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): użyj {@code toBuilder()} i zwróć kopię maila, która jest PILNA i ma dodatkowego
     * odbiorcę kopii „szef@example.com” (po istniejących). Oryginał ma pozostać bez zmian.
     * Podpowiedź: {@code mail.toBuilder().urgent(true).cc(...).build()}.
     */
    static Mail exercise3(Mail mail) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): dokończ builder rekordu {@code Discount}. Kod jest wymagany (konstruktor
     * buildera), a procent, próg kwoty (minAmount, domyślnie 0) i łączenie zniżek (stackable, domyślnie false)
     * — opcjonalne. build() ma zwrócić Discount; walidację robi już rekord.
     * Podpowiedź: wzoruj się na Mail.Builder; każda metoda ustawiająca zwraca this. Pole percent
     * nie ma wartości domyślnej — zostaw 0, rekord sam odrzuci niepoprawną wartość.
     */
    static final class Exercise4Builder {
        Exercise4Builder(String code) {
            // TODO: zapamiętaj kod
            throw new UnsupportedOperationException("TODO");
        }

        Exercise4Builder percent(int percent) {
            throw new UnsupportedOperationException("TODO");
        }

        Exercise4Builder minAmount(int minAmount) {
            throw new UnsupportedOperationException("TODO");
        }

        Exercise4Builder stackable(boolean stackable) {
            throw new UnsupportedOperationException("TODO");
        }

        Discount build() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Pizza solution1() {
        return Pizza.builder(Size.MEDIUM).topping("ser").topping("szynka").quantity(3).build();
    }

    static Pizza solution2() {
        return Pizza.builder(Size.SMALL).topping("extra ser").topping("pepperoni").build();
    }

    static Mail solution3(Mail mail) {
        return mail.toBuilder().urgent(true).cc("szef@example.com").build();
    }

    static final class Solution4Builder {
        private final String code;
        private int percent;
        private int minAmount;
        private boolean stackable;

        Solution4Builder(String code) {
            this.code = code;
        }

        Solution4Builder percent(int percent) {
            this.percent = percent;
            return this;
        }

        Solution4Builder minAmount(int minAmount) {
            this.minAmount = minAmount;
            return this;
        }

        Solution4Builder stackable(boolean stackable) {
            this.stackable = stackable;
            return this;
        }

        Discount build() {
            return new Discount(code, percent, minAmount, stackable);
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Nieczytelne wywołanie (nie wiadomo, co znaczy trzecie false), łatwa zamiana sąsiednich parametrów
     *      tego samego typu (kompilator nie zauważy) oraz mnożenie się konstruktorów przy opcjonalnych parametrach.
     *   2. Obiekt powstaje pusty i jest uzupełniany setterami — między nimi istnieje w stanie niekompletnym,
     *      a nic nie wymusza wywołania wszystkich setterów. Dodatkowo jest zmienny dla każdego, kto trzyma referencję.
     *   3. Wtedy kompilator pilnuje, że wymagany parametr zawsze jest podany; zapomnieć o nim się nie da.
     *   4. [ser] — produkt kopiuje listę (List.copyOf), więc późniejsze topping("szynka") jej nie zmienia.
     *   5. 72 — mała pizza kosztuje 20 zł, jeden dodatek +4 zł = 24 zł; razy 3 sztuki = 72.
     *   6. Produkt i builder dzielą tę samą listę: dalsze topping(...) zmienia „gotową” pizzę, może też
     *      ominąć walidację (np. limit czterech dodatków) i zepsuć niezmienność. Trzeba List.copyOf.
     *   7. Między sprawdzeniem a użyciem builder może się zmienić (np. przez inny wątek) i do produktu
     *      trafią dane, których nie sprawdzono. Poprawka: najpierw skopiuj pola do produktu, potem waliduj pola produktu.
     *   8. Żaden. HttpRequest.newBuilder()...build() tylko tworzy opis żądania; wysyła je dopiero HttpClient.
     */
    // </editor-fold>
}
