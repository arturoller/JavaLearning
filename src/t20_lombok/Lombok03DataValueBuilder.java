package t20_lombok;

import helpers.Check;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import lombok.Value;
import lombok.With;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Lombok — @Data, @Value, @Builder: gotowe klasy danych, niezmienność, płynne tworzenie obiektów
 *        (bundle = zestaw/pakiet adnotacji; fluent = płynny; immutable = niezmienny; default = domyślny)
 *
 * W SKRÓCIE:
 *   {@code @Data} to SKRÓT na @Getter + @Setter + @ToString + @EqualsAndHashCode + konstruktor
 *   wymaganych pól — wygodny dla klas ZMIENNYCH, ale niebezpieczny tam, gdzie obiekt może zmienić się
 *   po użyciu w kolekcji opartej na hashu. {@code @Value} to odpowiednik dla klas NIEZMIENNYCH (jak
 *   rekord, ale z biblioteki, nie z języka). {@code @Builder} daje płynne, nazwane tworzenie obiektów
 *   — zwłaszcza przydatne przy wielu polach tego samego typu (patrz pułapka w Lombok02Constructors, sekcja 6).
 *
 * ANALOGIA: gotowy zestaw mebli kontra zestaw "zrób to sam".
 *   @Data to gotowy, tani zestaw mebli — szybko złożony, ale jeśli przestawisz go (zmienisz pole),
 *   traci oznaczenie, pod którym go kupiłeś (hashCode). @Value to zestaw mebli PRZYKRĘCONY do podłogi —
 *   raz złożony, już się nie rusza. @Builder to zamawianie mebli na wymiar: podajesz nazwane opcje
 *   (kolor, rozmiar, dodatki) w dowolnej kolejności, a stolarz (builder) składa gotowy produkt na końcu.
 *
 * JAK TO DZIAŁA:
 *   @Builder class Pizza {
 *       private final String size;
 *       @Singular private final List<String> toppings;   ← builder dostaje topping(x), toppings(coll), clearToppings()
 *   }
 *   Pizza.builder().size("duża").topping("ser").topping("salami").build();
 *
 * SŁÓWKA:
 *   bundle = zestaw (kilka adnotacji naraz); fluent API = płynne API (wywołania łańcuchowo, każde zwraca
 *   "this"/budowniczego); immutable = niezmienny; default = domyślny; singular = liczba pojedyncza;
 *   wither = metoda "with..." zwracająca nowy obiekt ze zmienionym jednym polem.
 *
 * ZOBACZ TEŻ: t09_records/Records01Basics, t09_records/Records02Constructors (rekordy — wbudowana
 *             alternatywa dla @Value), t06_oop_basics/Oop06Immutability (dlaczego niezmienność pomaga),
 *             Lombok02Constructors (pułapka z kolejnością pól — jedno z uzasadnień @Builder).
 * </pre>
 */
public class Lombok03DataValueBuilder {

    // ---------------------------------------------------------------------------------------------
    // Klasy używane w lekcji
    // ---------------------------------------------------------------------------------------------

    /**
     * MutableTag = etykieta, ZMIENNA. {@code @Data} = @Getter + @Setter + @ToString + @EqualsAndHashCode
     * + konstruktor wymaganych pól (tu: żadne pole nie jest final/@NonNull, więc wychodzi konstruktor
     * bezargumentowy).
     */
    @Data
    static class MutableTag {
        private String name;
    }

    /** ImmutablePoint = punkt NIEZMIENNY. @Value robi pola private final automatycznie, klasę final, generuje same gettery. */
    @Value
    @With
    static class ImmutablePoint {
        double x;
        double y;
    }

    /** PointR = ten sam pomysł co ImmutablePoint, ale jako REKORD — do porównania w sekcji 4. */
    record PointR(double x, double y) {
    }

    /**
     * Pizza = przykład {@code @Builder}. {@code size} i {@code toppings} są wymagane logicznie (brak
     * sensownej wartości domyślnej), {@code spicy} ma jawny domyślny stan dzięki {@code @Builder.Default}.
     * {@code @Singular} na liście dodatków daje builderowi metody do dodawania PO JEDNYM elemencie.
     */
    @Getter
    @ToString
    @Builder(toBuilder = true)
    static class Pizza {
        private final String size;
        @Builder.Default
        private final boolean spicy = false;
        @Singular
        private final List<String> toppings;
    }

    public static void main(String[] args) {
        title("Lombok03 — @Data, @Value, @Builder");

        dataAnnotation();       // data annotation = adnotacja @Data
        dataHashSetDanger();    // data hashset danger = niebezpieczeństwo @Data w HashSet
        valueAnnotation();      // value annotation = adnotacja @Value
        valueVsRecord();        // value vs record = @Value kontra rekord
        builderSingular();      // builder singular = budowniczy i kolekcje
        builderDefaultToBuilder(); // builder default to builder = domyślne wartości i toBuilder
        withAnnotation();       // with annotation = adnotacja @With
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. @Data — co łączy
    // =================================================================================================

    /** 1. @Data to "pakiet" pięciu adnotacji naraz — wygodny domyślny wybór dla prostych, zmiennych klas danych. */
    static void dataAnnotation() {
        section("1. @Data — co łączy");

        MutableTag tag = new MutableTag();
        tag.setName("ważne");
        show("tag", tag);
        show("metody MutableTag (posortowane)", methodNames(MutableTag.class));
        // WYNIK: tag → Lombok03DataValueBuilder.MutableTag(name=ważne)
        // WYNIK: metody MutableTag (posortowane) → canEqual, equals, getName, hashCode, setName, toString

        // @Data = @Getter (wszystkie pola) + @Setter (wszystkie pola NIE-final) + @ToString +
        //   @EqualsAndHashCode + @RequiredArgsConstructor (konstruktor z pól final/@NonNull — tu: brak
        //   takich pól, więc konstruktor bezargumentowy).

        // DOBRA PRAKTYKA: @Data to dobry PUNKT STARTOWY dla prostego, zmiennego DTO (obiektu przenoszącego
        //   dane) — ale "domyślny wybór" nie znaczy "zawsze właściwy". Sekcja 2 pokazuje, kiedy szkodzi.
    }

    // =================================================================================================
    // 2. PUŁAPKA: @Data w kolekcji opartej na hashu
    // =================================================================================================

    /**
     * 2. equals/hashCode generowane przez @Data liczą się z WSZYSTKICH pól — w tym zmiennych. Jeśli
     * obiekt trafi do HashSet/HashMap, a potem jego pole się zmieni, hashCode się zmienia, a obiekt
     * "gubi się" (jest fizycznie w zbiorze, ale contains() już go nie znajduje).
     */
    static void dataHashSetDanger() {
        section("2. Pułapka: @Data + HashSet — obiekt \"gubi się\" po zmianie pola");

        MutableTag tag = new MutableTag();
        tag.setName("pierwotna-nazwa");
        Set<MutableTag> tags = new HashSet<>();
        tags.add(tag);
        show("contains(tag) zaraz po dodaniu", tags.contains(tag));
        // WYNIK: contains(tag) zaraz po dodaniu → true

        tag.setName("zmieniona-nazwa");
        show("contains(tag) po zmianie name", tags.contains(tag));
        show("rozmiar zbioru (obiekt nadal fizycznie w środku!)", tags.size());
        // WYNIK: contains(tag) po zmianie name → false
        // WYNIK: rozmiar zbioru (obiekt nadal fizycznie w środku!) → 1

        // JAK TO DZIAŁA: HashSet wkłada obiekt do "przegródki" (kubełka) wyliczonej z hashCode() W
        //   MOMENCIE DODANIA. Zmiana pola zmienia hashCode, ale obiekt zostaje w STAREJ przegródce.
        //   contains() liczy NOWY hashCode i szuka w nowej (złej) przegródce — nie znajduje.

        // PUŁAPKA: ten sam problem dotyczy encji JPA/Hibernate oznaczonych @Data — equals/hashCode
        //   liczone ze WSZYSTKICH pól (albo z pola id, zanim baza nada mu wartość — przed zapisem id
        //   bywa null) prowadzi do identycznych niespodzianek w Set<Encja>/Map<Encja,...>. DLACZEGO to
        //   ważne: typowa rada dla encji to @EqualsAndHashCode(onlyExplicitlyIncluded = true) na STAŁYM,
        //   biznesowym kluczu (nigdy na wszystkich polach) — albo samo @Getter/@Setter bez equals/hashCode
        //   z Lomboka, pisane ręcznie z rozwagą.

        // DOBRA PRAKTYKA: dla kluczy w HashSet/HashMap używaj obiektów NIEZMIENNYCH (@Value, rekord) —
        //   wtedy hashCode nie może się zmienić, bo pola nie mogą się zmienić.
    }

    // =================================================================================================
    // 3. @Value — niezmienność
    // =================================================================================================

    /** 3. @Value = pola private final automatycznie, klasa final, same gettery (bez setterów), equals/hashCode/toString. */
    static void valueAnnotation() {
        section("3. @Value — niezmienność");

        ImmutablePoint p = new ImmutablePoint(2.0, 3.0);
        show("punkt", p);
        show("p.getX()", p.getX());
        // WYNIK: punkt → Lombok03DataValueBuilder.ImmutablePoint(x=2.0, y=3.0)
        // WYNIK: p.getX() → 2.0

        show("klasa jest final", Modifier.isFinal(ImmutablePoint.class.getModifiers()));
        show("metody ImmutablePoint (posortowane)", methodNames(ImmutablePoint.class));
        // WYNIK: klasa jest final → true
        // WYNIK: metody ImmutablePoint (posortowane) → equals, getX, getY, hashCode, toString, withX, withY

        // CIEKAWOSTKA: tym razem na liście NIE MA metody canEqual (w przeciwieństwie do PersonLombok
        //   w Lombok01Accessors, sekcja 1). canEqual chroni symetrię equals w HIERARCHII klas — skoro
        //   ImmutablePoint jest final (nikt go nie rozszerzy), Lombok wie, że ta ochrona jest zbędna,
        //   i jej nie generuje.

        // Lombok generuje (w uproszczeniu):
        //   public final class ImmutablePoint {
        //       private final double x;
        //       private final double y;
        //       public ImmutablePoint(double x, double y) { this.x = x; this.y = y; }
        //       public double getX() { return x; }     // bez setterów!
        //       public double getY() { return y; }
        //       // + equals, hashCode, toString (bez canEqual — klasa jest final)
        //   }

        // DOBRA PRAKTYKA: @Value na klasę = "ta klasa jest wartością, nie encją" — jedna adnotacja
        //   zamiast pamiętania o final na każdym polu, braku setterów i ręcznego equals/hashCode
        //   (patrz t06_oop_basics/Oop06Immutability, gdzie robiliśmy to wszystko ręcznie).
    }

    // =================================================================================================
    // 4. @Value kontra rekord
    // =================================================================================================

    /**
     * 4. Rekord (Java 16+, patrz t09_records) robi niemal to samo co @Value — ale jako WBUDOWANA
     * funkcja języka, bez zależności od biblioteki. Obie formy tego samego punktu — do porównania.
     */
    static void valueVsRecord() {
        section("4. @Value kontra rekord — ten sam punkt, dwa zapisy");

        PointR r = new PointR(2.0, 3.0);
        show("rekord", r);
        show("r.x()", r.x());
        // WYNIK: rekord → PointR[x=2.0, y=3.0]
        // WYNIK: r.x() → 2.0

        // TABELA (ŚCIĄGA):
        //   @Value (Lombok)                              | record (Java 16+, wbudowany)
        //   ---------------------------------------------------------------------------------------
        //   zależność od biblioteki Lombok                | nic dodatkowego — część języka
        //   może "extends" zwykłą klasę                   | niejawnie "extends Record" — nie może nic innego
        //   gettery w stylu JavaBeans: getX(), getY()      | akcesory bez "get": x(), y()
        //   łatwo dołożyć @Builder, @With, @Singular       | buildery/withery trzeba pisać ręcznie (t09_records)
        //   walidacja: własny konstruktor + @Value         | konstruktor kompaktowy wbudowany (Records02Constructors)
        //
        // KIEDY REKORD JEST LEPSZY: gdy nie potrzebujesz dziedziczenia, buildera ani @Singular — rekord
        //   jest prostszy (0 zależności, czytelniejszy w stack trace, wspiera "instanceof" z dekonstrukcją
        //   — Java 21+). KIEDY @Value MOŻE BYĆ LEPSZY: gdy chcesz @Builder/@With "za darmo" albo gdy
        //   klasa musi dziedziczyć po istniejącej klasie bazowej (rekordy tego nie potrafią).
    }

    // =================================================================================================
    // 5. @Builder i @Singular
    // =================================================================================================

    /** 5. @Builder daje płynne, NAZWANE tworzenie obiektu. @Singular na kolekcji dodaje metodę "dodaj jeden element". */
    static void builderSingular() {
        section("5. @Builder i @Singular — płynne tworzenie, kolekcje");

        Pizza margherita = Pizza.builder()
                .size("duża")
                .topping("ser")
                .topping("pomidory")
                .build();
        show("margherita", margherita);
        // WYNIK: margherita → Lombok03DataValueBuilder.Pizza(size=duża, spicy=false, toppings=[ser, pomidory])

        Pizza fourCheese = Pizza.builder()
                .size("średnia")
                .toppings(List.of("mozzarella", "gorgonzola", "parmezan", "ementaler"))
                .build();
        show("fourCheese (toppings naraz przez listę)", fourCheese);
        // WYNIK: fourCheese (toppings naraz przez listę) → Lombok03DataValueBuilder.Pizza(size=średnia, spicy=false, toppings=[mozzarella, gorgonzola, parmezan, ementaler])

        // Lombok generuje dla pola "toppings" (fragment klasy PizzaBuilder):
        //   public PizzaBuilder topping(String topping) { this.toppings.add(topping); return this; }
        //   public PizzaBuilder toppings(Collection<? extends String> toppings) { this.toppings.addAll(toppings); return this; }
        //   public PizzaBuilder clearToppings() { this.toppings.clear(); return this; }
        // build() kopiuje zebrane elementy do NIEMODYFIKOWALNEJ listy (podobnie jak List.copyOf) —
        //   nawet jeśli nikt nie wywoła topping(...) ani toppings(...), pole dostanie PUSTĄ listę, nigdy null.

        // DOBRA PRAKTYKA: @Builder (zwłaszcza z @Singular) rozwiązuje pułapkę z Lombok02Constructors
        //   (sekcja 6) — wywołania są NAZWANE, więc przestawienie kolejności pól w kodzie źródłowym nie
        //   psuje już istniejących wywołań buildera.
    }

    // =================================================================================================
    // 6. @Builder.Default i toBuilder
    // =================================================================================================

    /**
     * 6. Pole z inicjalizatorem w klasie z @Builder BEZ @Builder.Default to częsta pułapka — Lombok
     * OSTRZEGA i (gdyby to zignorować) inicjalizator jest całkowicie POMIJANY przez builder.
     * {@code toBuilder = true} daje metodę do stworzenia nowego obiektu na bazie istniejącego.
     */
    static void builderDefaultToBuilder() {
        section("6. @Builder.Default — pułapka, i toBuilder — wygoda");

        Pizza bezPikantnosci = Pizza.builder().size("mała").topping("szynka").build();
        show("spicy, gdy nie podano (powinno użyć @Builder.Default)", bezPikantnosci.isSpicy());
        // WYNIK: spicy, gdy nie podano (powinno użyć @Builder.Default) → false

        Pizza pikantna = Pizza.builder().size("mała").spicy(true).topping("chorizo").build();
        show("spicy, gdy jawnie ustawione", pikantna.isSpicy());
        // WYNIK: spicy, gdy jawnie ustawione → true

        // PUŁAPKA: pokazana tylko w komentarzu — celowo NIE kompilujemy tej wersji.
        //   @Builder
        //   class Zly {
        //       private String currency = "PLN";   // inicjalizator BEZ @Builder.Default
        //   }
        //   Zly.builder().build().getCurrency();   // DAJE null, NIE "PLN"!
        // Lombok podczas kompilacji takiego kodu ostrzega: "@Builder will ignore the initializing
        //   expression entirely. If you want the initializing expression to serve as default, add
        //   @Builder.Default." Dla "spicy" (boolean) pułapka jest ZAKAMUFLOWANA, bo domyślna wartość
        //   typu (false) przypadkiem zgadza się z zamierzonym domyślnym stanem — dla String/obiektu
        //   wynikiem jest zaskakujący null.

        Pizza zOliwkami = margherita().toBuilder()
                .topping("oliwki")
                .build();
        show("margherita (oryginał)", margherita());
        show("zOliwkami (toBuilder + dodatkowy topping)", zOliwkami);
        // WYNIK: margherita (oryginał) → Lombok03DataValueBuilder.Pizza(size=duża, spicy=false, toppings=[ser, pomidory])
        // WYNIK: zOliwkami (toBuilder + dodatkowy topping) → Lombok03DataValueBuilder.Pizza(size=duża, spicy=false, toppings=[ser, pomidory, oliwki])

        // DOBRA PRAKTYKA: toBuilder = true to wygodny sposób na "zmianę" niezmiennego obiektu o WIELE
        //   pól naraz — bez pisania dziesiątek metod withX dla każdego pola osobno (porównaj z @With
        //   w sekcji 7, które pasuje lepiej przy niewielkiej liczbie pól).
    }

    private static Pizza margherita() {
        return Pizza.builder().size("duża").topping("ser").topping("pomidory").build();
    }

    // =================================================================================================
    // 7. @With — withery na niezmiennych obiektach
    // =================================================================================================

    /** 7. @With generuje "withX" dla KAŻDEGO pola — zwraca NOWY obiekt z jednym zmienionym polem, oryginał bez zmian. */
    static void withAnnotation() {
        section("7. @With — withery");

        ImmutablePoint original = new ImmutablePoint(1.0, 1.0);
        ImmutablePoint przesuniety = original.withX(5.0);
        show("original", original);
        show("original.withX(5.0)", przesuniety);
        // WYNIK: original → Lombok03DataValueBuilder.ImmutablePoint(x=1.0, y=1.0)
        // WYNIK: original.withX(5.0) → Lombok03DataValueBuilder.ImmutablePoint(x=5.0, y=1.0)

        // Lombok generuje:
        //   public ImmutablePoint withX(double x) {
        //       return this.x == x ? this : new ImmutablePoint(x, this.y);
        //   }
        // (zauważ optymalizację: jeśli wartość się nie zmienia, zwracany jest TEN SAM obiekt, bez
        //   tworzenia nowego — bezpieczne, bo obiekt jest niezmienny).

        // DOBRA PRAKTYKA: @With dobrze pasuje do małej liczby pól i częstych "pojedynczych" zmian;
        //   przy wielu polach i złożonych modyfikacjach naraz lepiej sprawdza się toBuilder (sekcja 6).
    }

    // ---------------------------------------------------------------------------------------------
    // Pomocnik: posortowana lista nazw metod zadeklarowanych w klasie (dowód refleksją, patrz
    // t19_annotations_reflection/Annotations03ReflectionBasics).
    // ---------------------------------------------------------------------------------------------

    private static String methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .map(Method::getName)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @Data = @Getter + @Setter(pola nie-final) + @ToString + @EqualsAndHashCode + konstruktor
     *     wymaganych pól. Dla klas ZMIENNYCH; NIGDY jako klucz w HashSet/HashMap po zmianie pola.
     *   • @Value = @Getter + final na wszystkich polach + klasa final + @ToString + @EqualsAndHashCode
     *     + konstruktor wszystkich pól, BEZ setterów. Dla klas NIEZMIENNYCH — alternatywa: rekord.
     *   • @Builder: nazwane, płynne tworzenie; @Singular na kolekcji → dodawanie po jednym elemencie +
     *     bezpieczna pusta lista domyślnie; @Builder.Default dla pól z inicjalizatorem (inaczej: null/
     *     ostrzeżenie); toBuilder = true → kopiowanie z modyfikacją wielu pól naraz.
     *   • @With: po jednym getterze "withX" na pole — nowy obiekt, oryginał bez zmian.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego obiekt @Data "gubi się" w HashSet po zmianie pola, mimo że fizycznie tam jest?
     *   2. Co wypisze:  System.out.println(new ImmutablePoint(1, 2).getClass().getSuperclass());  ?
     *      (podpowiedź: @Value nie zmienia hierarchii klas — to zwykła klasa, nie rekord)
     *   3. ZNAJDŹ BŁĄD:  @Builder class Cfg { private String env = "prod"; }  — Cfg.builder().build().getEnv()
     *      ma zwrócić "prod", ale zwraca null. Co poprawić?
     *   4. Czym różni się getter rekordu od gettera wygenerowanego przez @Value?
     *   5. Co wypisze:  Pizza.builder().size("mała").build().getToppings().size();  (bez żadnego toppingu)?
     *   6. Dlaczego original.withX(5.0) nie zmienia obiektu original?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: pizza przez builder", "Lombok03DataValueBuilder.Pizza(size=mała, spicy=false, toppings=[ser])",
                () -> exercise1("mała", "ser"));
        Check.equal("ćw. 2: dwie etykiety o tej samej nazwie są równe", true, () -> exercise2("x", "x"));
        Check.equal("ćw. 3 (PRZEPISZ): pizza przez builder zamiast konstruktora", "Lombok03DataValueBuilder.Pizza(size=duża, spicy=true, toppings=[salami, papryka])",
                () -> exercise3());
        Check.equal("ćw. 4: toBuilder z dodatkowym toppingiem", 3, () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "Lombok03DataValueBuilder.Pizza(size=mała, spicy=false, toppings=[ser])", () -> solution1("mała", "ser"));
        Check.equal("ćw. 2 (wzorzec)", true, () -> solution2("x", "x"));
        Check.equal("ćw. 3 (wzorzec)", "Lombok03DataValueBuilder.Pizza(size=duża, spicy=true, toppings=[salami, papryka])", () -> solution3());
        Check.equal("ćw. 4 (wzorzec)", 3, () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** ĆWICZENIE 1 (łatwe): zbuduj Pizza przez builder z podanym rozmiarem i JEDNYM toppingiem, zwróć toString(). */
    static String exercise1(String size, String topping) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /** ĆWICZENIE 2 (łatwe): zwróć, czy dwa MutableTag o podanych nazwach są sobie równe (equals). */
    static boolean exercise2(String name1, String name2) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ): niewygodny, stary sposób budowania pizzy wymagał pamiętania
     * WSZYSTKICH pól i ich kolejności (konstruktor pakietowy wygenerowany przez @Builder):
     * <pre>{@code
     *     new Pizza("duża", true, List.of("salami", "papryka"));   // łatwo pomylić kolejność/pola
     * }</pre>
     * Przepisz to na budowanie przez {@code Pizza.builder()} (rozmiar "duża", ostra, dodatki "salami"
     * i "papryka") i zwróć {@code toString()}.
     */
    static String exercise3() {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zbuduj pizzę z dwoma toppingami ("ser", "pomidory"), potem przez
     * toBuilder() dodaj trzeci topping ("bazylia") i zwróć LICZBĘ toppingów w nowej pizzy.
     * Podpowiedź: pizza.toBuilder().topping("bazylia").build().getToppings().size().
     */
    static int exercise4() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String size, String topping) {
        return Pizza.builder().size(size).topping(topping).build().toString();
    }

    static boolean solution2(String name1, String name2) {
        MutableTag t1 = new MutableTag();
        t1.setName(name1);
        MutableTag t2 = new MutableTag();
        t2.setName(name2);
        return t1.equals(t2);
    }

    static String solution3() {
        return Pizza.builder().size("duża").spicy(true).topping("salami").topping("papryka").build().toString();
    }

    static int solution4() {
        Pizza base = Pizza.builder().size("mała").topping("ser").topping("pomidory").build();
        return base.toBuilder().topping("bazylia").build().getToppings().size();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. HashSet umieszcza obiekt w przegródce wyliczonej z hashCode() w momencie dodania. Zmiana
     *      pola zmienia hashCode, ale obiekt zostaje w starej przegródce — wyszukiwanie liczy nowy
     *      hashCode i szuka w innej (złej) przegródce, więc nie znajduje obiektu, choć fizycznie tam jest.
     *   2. "class java.lang.Object" — @Value to zwykła klasa Java, nie dziedziczy po żadnej specjalnej
     *      klasie bazowej (w przeciwieństwie do rekordu, który niejawnie dziedziczy po java.lang.Record).
     *   3. Brakuje @Builder.Default przy polu "env = \"prod\"" — bez niego builder ignoruje inicjalizator
     *      i pole dostaje null. Poprawka: @Builder.Default private String env = "prod";
     *   4. Getter rekordu nazywa się jak pole, bez prefiksu "get" (np. x()), getter @Value trzyma się
     *      konwencji JavaBeans (getX()).
     *   5. "0" — @Singular bez żadnego wywołania topping(...)/toppings(...) daje PUSTĄ, niemodyfikowalną
     *      listę, nigdy null.
     *   6. withX zwraca NOWY obiekt (ewentualnie "this", jeśli wartość się nie zmienia) — nie modyfikuje
     *      pól istniejącego obiektu, bo ImmutablePoint jest niezmienny (pola final, @Value).
     */
    // </editor-fold>
}
