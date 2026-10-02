package t19_annotations_reflection;

import helpers.Check;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Adnotacje wbudowane — czym jest adnotacja i kto ją czyta
 *        (annotation = adnotacja; built-in = wbudowany; metadata = metadane, czyli „dane o danych”)
 *
 * W SKRÓCIE:
 *   Adnotacja to etykieta przyklejona do klasy, metody, pola lub parametru, np. {@code @Override}. Sama NIC nie robi —
 *   nie zmienia ani jednej instrukcji programu. Działa dopiero wtedy, gdy ktoś ją PRZECZYTA: kompilator (javac),
 *   narzędzie uruchamiane przy kompilacji albo framework w czasie działania programu (przez refleksję).
 *
 * ANALOGIA: naklejka „OSTROŻNIE, SZKŁO” na paczce.
 *   Od naklejki paczka nie staje się mocniejsza. Coś się zmienia dopiero wtedy, gdy kurier ją przeczyta i odłoży
 *   paczkę delikatnie. Kurier, który nie czyta naklejek = adnotacja, której nikt nie czyta = zero efektu.
 *
 * JAK TO DZIAŁA:
 *   składnia: {@code @Nazwa} albo {@code @Nazwa(element = wartość, ...)} tuż przed deklaracją.
 *
 *   KTO CZYTA?                  KIEDY?                  PRZYKŁADY
 *   kompilator javac            podczas kompilacji      Override, Deprecated, SuppressWarnings, FunctionalInterface
 *   procesor adnotacji          podczas kompilacji      Lombok, MapStruct (Annotations07Processors)
 *   framework przez refleksję   w czasie działania      Spring, JUnit, walidacja (Annotations04, Annotations05)
 *
 *   Jak długo adnotacja „żyje”, decyduje meta-adnotacja Retention (retention = przechowywanie):
 *   SOURCE (tylko w kodzie źródłowym) → CLASS (także w pliku .class) → RUNTIME (widoczna dla refleksji).
 *
 * SŁÓWKA:
 *   annotation = adnotacja; override = nadpisz; deprecated = przestarzały (wycofywany); since = od (wersji);
 *   for removal = do usunięcia; suppress warnings = wycisz ostrzeżenia; unchecked = niesprawdzony; raw type = typ surowy;
 *   functional interface = interfejs funkcyjny; safe varargs = bezpieczne zmienne argumenty; retention = przechowywanie;
 *   target = cel; documented = dokumentowany; inherited = dziedziczony; repeatable = powtarzalny;
 *   meta-annotation = meta-adnotacja (adnotacja nałożona na inną adnotację); heap pollution = zanieczyszczenie sterty.
 *
 * ZOBACZ TEŻ: t07_inheritance_polymorphism/Inherit02Override (nadpisywanie), t13_lambdas/Lambda02FunctionalInterfaces,
 *             t11_generics/Generics06ErasureLimits (typy surowe, wymazywanie typów),
 *             t19_annotations_reflection/Annotations02Custom (własne adnotacje).
 * </pre>
 */
public class Annotations01BuiltIn {

    // ---------------------------------------------------------------------------------------------
    // Typy pomocnicze
    // ---------------------------------------------------------------------------------------------

    /** BadPoint = zły punkt: equals(BadPoint) to PRZECIĄŻENIE (overload), a nie nadpisanie equals(Object). */
    static final class BadPoint {
        private final int x;
        private final int y;

        BadPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        // Brak @Override — kompilator milczy, a ta metoda NIE nadpisuje Object.equals(Object).
        public boolean equals(BadPoint other) {
            return other != null && x == other.x && y == other.y;
        }
    }

    /** GoodPoint = dobry punkt: equals(Object) i hashCode z @Override — kompilator sprawdza, że naprawdę nadpisujemy. */
    static final class GoodPoint {
        private final int x;
        private final int y;

        GoodPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof GoodPoint p && x == p.x && y == p.y;   // wzorzec w instanceof (Java 16+)
        }

        @Override
        public int hashCode() {
            return 31 * x + y;
        }
    }

    static final BigDecimal VAT_RATE = new BigDecimal("1.23");
    static final BigDecimal NINETY_PERCENT = new BigDecimal("0.90");
    static final BigDecimal SHIPPING = new BigDecimal("15.00");

    /** oldPrice = stara cena brutto liczona na int (gubi grosze). Zastąpiona przez grossPrice(BigDecimal). */
    @Deprecated(since = "2.0", forRemoval = true)
    static int oldPrice(int net) {
        return net * 123 / 100;   // dzielenie całkowite obcina grosze
    }

    /** grossPrice = cena brutto (net = netto) — nowa, poprawna wersja na BigDecimal. */
    static BigDecimal grossPrice(BigDecimal net) {
        return net.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);   // HALF_UP = zaokrąglaj „szkolnie”
    }

    /** PriceRule = reguła ceny: interfejs funkcyjny — dokładnie JEDNA metoda abstrakcyjna. */
    @FunctionalInterface
    interface PriceRule {
        BigDecimal apply(BigDecimal price);              // apply = zastosuj; jedyna metoda abstrakcyjna

        default PriceRule andThen(PriceRule next) {      // andThen = a potem; metoda domyślna się nie liczy
            return price -> next.apply(apply(price));
        }

        @Override
        boolean equals(Object other);                    // publiczna metoda z Object też się nie liczy
    }

    /** listOf = lista z podanych elementów. Tylko CZYTA tablicę varargs — obietnica @SafeVarargs jest prawdziwa. */
    @SafeVarargs
    static <T> List<T> listOf(T... items) {
        List<T> result = new ArrayList<>();
        for (T item : items) {
            result.add(item);
        }
        return result;
    }

    /** toArray = zwróć tablicę varargs. KŁAMIE: tablica „ucieka” z metody, więc obietnica @SafeVarargs jest fałszywa. */
    @SafeVarargs
    @SuppressWarnings("varargs")   // celowo: javac ostrzega, że tablica wychodzi z metody — wyciszamy, by pokazać skutek
    static <T> T[] toArray(T... items) {
        return items;
    }

    /** pickTwo = wybierz dwa. T jest wymazane (erasure), więc toArray dostaje nową tablicę Object[], nie String[]. */
    static <T> T[] pickTwo(T a, T b) {
        return toArray(a, b);
    }

    public static void main(String[] args) throws Exception {
        title("Annotations01 — adnotacje wbudowane i meta-adnotacje");

        whatIsAnnotation();      // what is annotation = czym jest adnotacja
        overrideCheck();         // override check = sprawdzenie nadpisania
        deprecatedApi();         // deprecated API = przestarzałe API
        suppressWarnings();      // suppress warnings = wycisz ostrzeżenia
        functionalInterface();   // functional interface = interfejs funkcyjny
        safeVarargs();           // safe varargs = bezpieczne zmienne argumenty
        metaAnnotations();       // meta-annotations = meta-adnotacje
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. ADNOTACJA TO METADANE
    // =================================================================================================

    /**
     * 1. Adnotacja nie zmienia działania kodu. Żeby coś się stało, ktoś musi ją odczytać. Tu „czytelnikiem” jest
     * refleksja (szczegóły w Annotations03ReflectionBasics) — ale widzi ona tylko adnotacje z retencją RUNTIME.
     */
    static void whatIsAnnotation() throws NoSuchMethodException {
        section("1. Adnotacja to metadane — sama nic nie robi");

        // oldPrice ma @Deprecated, a liczy dokładnie tak samo, jakby adnotacji nie było.
        show("oldPrice(99)", oldPrice(99));
        // WYNIK: oldPrice(99) → 121

        // getDeclaredMethod = pobierz zadeklarowaną metodę (nazwa + typy parametrów)
        Method oldPriceMethod = Annotations01BuiltIn.class.getDeclaredMethod("oldPrice", int.class);
        // isAnnotationPresent = czy adnotacja jest obecna
        show("oldPrice ma @Deprecated?", oldPriceMethod.isAnnotationPresent(Deprecated.class));
        // WYNIK: oldPrice ma @Deprecated? → true

        Method equalsMethod = GoodPoint.class.getDeclaredMethod("equals", Object.class);
        // getAnnotations = pobierz adnotacje widoczne w czasie działania
        show("adnotacje na GoodPoint.equals", Arrays.toString(equalsMethod.getAnnotations()));
        // WYNIK: adnotacje na GoodPoint.equals → []    ← @Override (SOURCE) kompilator sprawdził i wyrzucił

        show("Runnable ma @FunctionalInterface?", Runnable.class.isAnnotationPresent(FunctionalInterface.class));
        // WYNIK: Runnable ma @FunctionalInterface? → true

        // PUŁAPKA: „dodałem adnotację, a nic się nie dzieje” — bo nikt jej nie czyta. Adnotacja jest jak naklejka:
        //   potrzebny jest kod (kompilator, procesor, framework), który ją znajdzie i na nią zareaguje.
    }

    // =================================================================================================
    // 2. @Override
    // =================================================================================================

    /**
     * 2. @Override mówi kompilatorowi: „ta metoda MA nadpisywać metodę z nadklasy lub interfejsu”. Jeśli nie nadpisuje
     * (literówka w nazwie, zły typ parametru) — błąd kompilacji. Bez adnotacji powstaje po cichu nowa metoda.
     */
    static void overrideCheck() {
        section("2. @Override — kompilator pilnuje nadpisania");

        BadPoint a = new BadPoint(1, 2);
        BadPoint b = new BadPoint(1, 2);
        Object bAsObject = b;
        show("PRZED: a.equals(b)", a.equals(b));                       // wybrane przeciążenie equals(BadPoint)
        show("PRZED: a.equals((Object) b)", a.equals(bAsObject));      // wybrane Object.equals(Object) → porównanie referencji
        show("PRZED: List.of(a).contains(b)", List.of(a).contains(b)); // contains = zawiera; woła equals(Object)
        // WYNIK: PRZED: a.equals(b) → true
        // WYNIK: PRZED: a.equals((Object) b) → false
        // WYNIK: PRZED: List.of(a).contains(b) → false    ← kolekcje „nie widzą” equals(BadPoint)

        GoodPoint g1 = new GoodPoint(1, 2);
        GoodPoint g2 = new GoodPoint(1, 2);
        show("PO: List.of(g1).contains(g2)", List.of(g1).contains(g2));
        // WYNIK: PO: List.of(g1).contains(g2) → true

        // Gdyby nad equals(BadPoint other) stało @Override, kompilator od razu by zaprotestował:
        //   błąd kompilacji: method does not override or implement a method from a supertype
        //   (metoda nie nadpisuje ani nie implementuje metody z typu nadrzędnego)

        // PUŁAPKA: equals(MojaKlasa) zamiast equals(Object) to klasyczny błąd — kod się kompiluje, testy „na piechotę”
        //   (a.equals(b)) przechodzą, a HashSet, HashMap i List.contains działają źle, bo wołają equals(Object).
        // DOBRA PRAKTYKA: pisz @Override ZAWSZE przy nadpisywaniu (także przy implementacji metod interfejsu) —
        //   to darmowa kontrola kompilatora, która łapie literówki i złe typy parametrów.
    }

    // =================================================================================================
    // 3. @Deprecated
    // =================================================================================================

    /**
     * 3. @Deprecated(since = "wersja", forRemoval = true/false) oznacza API, którego nie należy już używać.
     * Kompilator ostrzega każdego, kto go używa: [deprecation] albo mocniej [removal], gdy forRemoval = true.
     */
    static void deprecatedApi() throws NoSuchMethodException {
        section("3. @Deprecated(since, forRemoval) — API do wycofania");

        // Stara metoda gubi grosze, nowa liczy dokładnie — dlatego starą wycofujemy.
        show("oldPrice(99)", oldPrice(99));
        show("grossPrice(99)", grossPrice(new BigDecimal("99")));
        // WYNIK: oldPrice(99) → 121
        // WYNIK: grossPrice(99) → 121.77

        // @Deprecated ma retencję RUNTIME, więc since i forRemoval da się odczytać także refleksją.
        Deprecated own = Annotations01BuiltIn.class.getDeclaredMethod("oldPrice", int.class)
                .getAnnotation(Deprecated.class);       // getAnnotation = pobierz adnotację danego typu (albo null)
        show("oldPrice: since", own.since());
        show("oldPrice: forRemoval", own.forRemoval());
        // WYNIK: oldPrice: since → 2.0
        // WYNIK: oldPrice: forRemoval → true

        // JDK też oznacza swoje stare API. getMethod/getConstructor = pobierz publiczną metodę/konstruktor.
        show("Character.isSpace", Character.class.getMethod("isSpace", char.class).getAnnotation(Deprecated.class));
        show("new Integer(int)", Integer.class.getConstructor(int.class).getAnnotation(Deprecated.class));
        // WYNIK: Character.isSpace → @java.lang.Deprecated(forRemoval=false, since="1.1")
        // WYNIK: new Integer(int) → @java.lang.Deprecated(forRemoval=true, since="9")

        // Co pokaże kompilator (-Xlint:all), gdy ktoś UŻYJE takiego API:
        //   ostrzeżenie [deprecation] isSpace(char) in Character has been deprecated
        //   ostrzeżenie [removal] Integer(int) in Integer has been deprecated and marked for removal
        // Wyjątek z JLS: użycie w tej samej klasie zewnętrznej, w której jest deklaracja, NIE daje ostrzeżenia —
        // dlatego wywołanie oldPrice(99) powyżej kompiluje się bez ostrzeżeń.

        // DOBRA PRAKTYKA: wycofując metodę, napisz w jej Javadoc (znacznik {@code @deprecated}), CZEGO użyć zamiast niej,
        //   i podaj since — wtedy użytkownik wie, od kiedy i dokąd ma przejść.
        // PUŁAPKA: forRemoval = true to zapowiedź usunięcia w przyszłej wersji. Kod, który z tego korzysta, kiedyś
        //   przestanie się kompilować — dlatego [removal] traktuj jak „zadanie do zrobienia teraz”, a nie szum.
    }

    // =================================================================================================
    // 4. @SuppressWarnings
    // =================================================================================================

    /** addRaw = dodaj przez typ surowy (raw type): kompilator nie sprawdza typu wkładanego elementu. */
    @SuppressWarnings({"rawtypes", "unchecked"})   // celowo: pokazujemy, jak typ surowy psuje bezpieczeństwo typów
    static void addRaw(List list, Object value) {
        list.add(value);   // bez wyciszenia: ostrzeżenie [unchecked] unchecked call to add(E) as a member of the raw type
    }

    /**
     * 4. @SuppressWarnings("nazwa") wycisza ostrzeżenie kompilatora w zasięgu jednej deklaracji (klasy, metody, pola,
     * zmiennej lokalnej). Najczęstsze nazwy: unchecked, rawtypes, deprecation, removal, varargs, serial.
     */
    static void suppressWarnings() {
        section("4. @SuppressWarnings — wyciszanie ostrzeżeń (jak najwężej!)");

        // 4a. Wyciszone ostrzeżenie = nikt nas nie ostrzegł, że typ surowy przemyca Integer do List<String>.
        List<String> names = new ArrayList<>(List.of("Ala"));
        addRaw(names, 42);
        show("names (to miała być List<String>)", names);
        // WYNIK: names (to miała być List<String>) → [Ala, 42]
        expectThrows("String second = names.get(1)", () -> {
            String second = names.get(1);   // kompilator wstawił tu niewidoczne rzutowanie (String)
            show("tu nie dojdziemy", second);
        });
        // WYNIK: ✔ String second = names.get(1) → rzucono ClassCastException: class java.lang.Integer cannot be cast to class java.lang.String (java.lang.Integer and java.lang.String are in module java.base of loader 'bootstrap')
        // PUŁAPKA: wyjątek wybucha DALEKO od błędu (addRaw), w niewinnym get(1). To zanieczyszczenie sterty
        //   (heap pollution) — dlatego ostrzeżeń unchecked/rawtypes nie wolno wyciszać „dla świętego spokoju”.

        // 4b. Uzasadnione wyciszenie: NAJWĘŻSZY zasięg (jedna zmienna) + komentarz DLACZEGO to bezpieczne.
        Map<String, Object> cache = new HashMap<>();
        cache.put("names", List.of("Ola", "Ula"));
        @SuppressWarnings("unchecked")   // pod "names" wkładamy wyłącznie List<String> (linia wyżej) — sprawdziliśmy sami
        List<String> cached = (List<String>) cache.get("names");
        show("cached.get(0)", cached.get(0));
        // WYNIK: cached.get(0) → Ola

        // 4c. Świadome użycie przestarzałego API (np. porównanie starej i nowej metody) — też jedna zmienna.
        @SuppressWarnings("deprecation")   // celowo: porównujemy przestarzałe isSpace z następcą isWhitespace
        boolean oldWay = Character.isSpace(' ');
        boolean newWay = Character.isWhitespace(' ');   // isWhitespace = czy biały znak
        @SuppressWarnings("removal")       // celowo: konstruktor Integer(int) jest przeznaczony do usunięcia
        Integer oldBox = new Integer(42);
        Integer newBox = Integer.valueOf(42);           // valueOf = wartość z (używa pamięci podręcznej -128..127)
        show("isSpace / isWhitespace", oldWay + " / " + newWay);
        show("new Integer(42).equals(Integer.valueOf(42))", oldBox.equals(newBox));
        // WYNIK: isSpace / isWhitespace → true / true
        // WYNIK: new Integer(42).equals(Integer.valueOf(42)) → true

        // PUŁAPKA: javac po cichu ignoruje nieznane nazwy — @SuppressWarnings("uncheked") (literówka) niczego nie wyciszy
        //   i nie da błędu. Ostrzeżenie dalej się pojawi, a Ty będziesz się dziwić, czemu adnotacja „nie działa”.
        // DOBRA PRAKTYKA: wyciszaj na zmiennej lokalnej albo jednej metodzie, NIGDY na całej klasie — inaczej ukryjesz
        //   także przyszłe, prawdziwe problemy. Zawsze dopisz komentarz, dlaczego to bezpieczne.
    }

    // =================================================================================================
    // 5. @FunctionalInterface
    // =================================================================================================

    /**
     * 5. Interfejs funkcyjny ma dokładnie jedną metodę abstrakcyjną — tylko taki można zastąpić lambdą.
     * {@code @FunctionalInterface} nie jest do tego potrzebna, ale sprawia, że kompilator PILNUJE tej zasady.
     */
    static void functionalInterface() {
        section("5. @FunctionalInterface — dokładnie jedna metoda abstrakcyjna");

        PriceRule discount = price -> price.multiply(NINETY_PERCENT).setScale(2, RoundingMode.HALF_UP);
        PriceRule shipping = price -> price.add(SHIPPING);
        show("rabat 10% i wysyłka od 100.00", discount.andThen(shipping).apply(new BigDecimal("100.00")));
        // WYNIK: rabat 10% i wysyłka od 100.00 → 105.00

        // Comparator ma DWIE metody abstrakcyjne: compare i equals. equals pochodzi z Object, więc się nie liczy.
        List<String> abstractMethods = Arrays.stream(Comparator.class.getMethods())   // getMethods = metody publiczne
                .filter(m -> Modifier.isAbstract(m.getModifiers()))                   // isAbstract = czy abstrakcyjna
                .map(Method::getName)
                .sorted()
                .toList();                                                            // toList (Java 16+)
        show("metody abstrakcyjne Comparator", abstractMethods);
        show("Comparator ma @FunctionalInterface?", Comparator.class.isAnnotationPresent(FunctionalInterface.class));
        // WYNIK: metody abstrakcyjne Comparator → [compare, equals]
        // WYNIK: Comparator ma @FunctionalInterface? → true

        // Druga metoda abstrakcyjna w interfejsie z @FunctionalInterface:
        //   @FunctionalInterface interface Two { void a(); void b(); }
        //   błąd kompilacji: Unexpected @FunctionalInterface annotation — Two is not a functional interface,
        //   multiple non-overriding abstract methods found in interface Two

        // PUŁAPKA: bez tej adnotacji ktoś dopisze drugą metodę abstrakcyjną i błąd pojawi się DALEKO — przy każdej
        //   lambdzie, która używała interfejsu. Z adnotacją błąd jest w jednym miejscu: w samym interfejsie.
        // DOBRA PRAKTYKA: oznaczaj @FunctionalInterface każdy własny interfejs, który ma być używany z lambdami.
    }

    // =================================================================================================
    // 6. @SafeVarargs
    // =================================================================================================

    /**
     * 6. Varargs z typem generycznym (T...) to w środku tablica, a tablice i generyki źle się dogadują (wymazywanie).
     * Kompilator ostrzega: [unchecked] Possible heap pollution. {@code @SafeVarargs} to OBIETNICA autora metody:
     * „nie robię z tą tablicą nic groźnego” — i wycisza ostrzeżenie u wszystkich wywołujących.
     */
    static void safeVarargs() {
        section("6. @SafeVarargs — obietnica przy generycznych varargs");

        List<List<Integer>> nested = listOf(List.of(1), List.of(2, 3));
        show("listOf(List.of(1), List.of(2, 3))", nested);
        // WYNIK: listOf(List.of(1), List.of(2, 3)) → [[1], [2, 3]]
        // Bez @SafeVarargs przy wywołaniu: [unchecked] unchecked generic array creation for varargs parameter

        // toArray złamało obietnicę: tablica Object[] (bo T wymazane w pickTwo) trafia do zmiennej String[].
        expectThrows("String[] pair = pickTwo(\"a\", \"b\")", () -> {
            String[] pair = pickTwo("a", "b");
            show("tu nie dojdziemy", Arrays.toString(pair));
        });
        // WYNIK: ✔ String[] pair = pickTwo("a", "b") → rzucono ClassCastException: class [Ljava.lang.Object; cannot be cast to class [Ljava.lang.String; ([Ljava.lang.Object; and [Ljava.lang.String; are in module java.base of loader 'bootstrap')

        // Gdzie wolno postawić @SafeVarargs: metody static, metody final, metody private (Java 9+) i konstruktory.
        // Na zwykłej metodzie instancyjnej:
        //   błąd kompilacji: Invalid SafeVarargs annotation. Instance method ... is neither final nor private.
        // Dlaczego? Taką metodę podklasa może nadpisać i złamać obietnicę, a adnotacja „wisiałaby” na niej dalej.

        // PUŁAPKA: @SafeVarargs to Twoja obietnica, kompilator jej nie sprawdza (najwyżej ostrzeże [varargs], gdy tablica
        //   wychodzi z metody). Fałszywa obietnica = ClassCastException w zupełnie innym miejscu programu.
        // DOBRA PRAKTYKA: dawaj @SafeVarargs tylko metodom, które tablicę varargs wyłącznie CZYTAJĄ
        //   (pętla po elementach) — nie zwracają jej, nie zapisują do niej i nie przekazują dalej.
    }

    // =================================================================================================
    // 7. META-ADNOTACJE
    // =================================================================================================

    /**
     * 7. Meta-adnotacja to adnotacja nałożona na DEKLARACJĘ innej adnotacji. Mówi, jak ta adnotacja się zachowuje.
     * Odczytamy je z samych adnotacji wbudowanych — one też są zwykłymi typami z pakietu java.lang.
     */
    static void metaAnnotations() {
        section("7. Meta-adnotacje — adnotacje na adnotacjach");

        List<Class<? extends Annotation>> builtIns = List.of(Override.class, Deprecated.class,
                SuppressWarnings.class, FunctionalInterface.class, SafeVarargs.class);
        System.out.printf(Locale.ROOT, "   %-20s %-8s %-11s %s%n", "adnotacja", "retencja", "dokumentow.", "cel (Target)");
        for (Class<? extends Annotation> type : builtIns) {
            Retention retention = type.getAnnotation(Retention.class);
            Target target = type.getAnnotation(Target.class);
            System.out.printf(Locale.ROOT, "   %-20s %-8s %-11s %s%n", "@" + type.getSimpleName(), retention.value(),
                    type.isAnnotationPresent(Documented.class), Arrays.toString(target.value()));
        }
        // WYNIK:    adnotacja            retencja dokumentow. cel (Target)
        // WYNIK:    @Override            SOURCE   false       [METHOD]
        // WYNIK:    @Deprecated          RUNTIME  true        [CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, PACKAGE, MODULE, PARAMETER, TYPE]
        // WYNIK:    @SuppressWarnings    SOURCE   false       [TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, MODULE]
        // WYNIK:    @FunctionalInterface RUNTIME  true        [TYPE]
        // WYNIK:    @SafeVarargs         RUNTIME  true        [CONSTRUCTOR, METHOD]

        // Retention sama jest adnotacją. Jej cel to ANNOTATION_TYPE — wolno ją kleić tylko na adnotacje.
        show("cel @Retention", Arrays.toString(Retention.class.getAnnotation(Target.class).value()));
        // WYNIK: cel @Retention → [ANNOTATION_TYPE]

        // Pięć meta-adnotacji z java.lang.annotation (szczegóły i przykłady w Annotations02Custom):
        //   Retention  (przechowywanie) — SOURCE / CLASS (domyślnie!) / RUNTIME: jak długo adnotacja żyje
        //   Target     (cel)            — gdzie wolno ją przykleić: TYPE, FIELD, METHOD, PARAMETER, ...
        //   Documented (dokumentowany)  — pokaż adnotację w dokumentacji Javadoc elementu
        //   Inherited  (dziedziczony)   — adnotacja na klasie przechodzi na podklasy (tylko klasy, nie interfejsy)
        //   Repeatable (powtarzalny)    — tę samą adnotację można dać kilka razy na jednej deklaracji

        // PUŁAPKA: brak Retention oznacza CLASS, a nie RUNTIME. Adnotacja jest wtedy w pliku .class, ale refleksja
        //   jej NIE widzi — to najczęstszy powód, dla którego „mój framework nie widzi mojej adnotacji”.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Adnotacja = metadane. Sama nic nie robi; czyta ją kompilator, procesor albo framework (refleksja).
     *   • @Override — kompilator sprawdza, że metoda naprawdę nadpisuje. Pisz zawsze (equals(Object)!).
     *   • @Deprecated(since = "x", forRemoval = true) — ostrzeżenie [deprecation] lub mocniejsze [removal].
     *   • @SuppressWarnings("unchecked" | "rawtypes" | "deprecation" | "removal" | "varargs") — najwęższy zasięg
     *     (zmienna lokalna, jedna metoda) + komentarz DLACZEGO. Literówki w nazwie javac po cichu ignoruje.
     *   • @FunctionalInterface — kompilator pilnuje JEDNEJ metody abstrakcyjnej (default, static i metody z Object
     *     się nie liczą).
     *   • @SafeVarargs — obietnica „tylko czytam tablicę varargs”; tylko static / final / private / konstruktor.
     *   • Meta-adnotacje: Retention (domyślnie CLASS!), Target, Documented, Inherited, Repeatable.
     *
     * PYTANIA KONTROLNE:
     *   1. Czy adnotacja sama zmienia działanie metody? Kto może ją przeczytać?
     *   2. Co wypisze:  System.out.println(Override.class.getAnnotation(Retention.class).value());  ?
     *   3. ZNAJDŹ BŁĄD:  class Point { ... public boolean equals(Point other) { ... } }
     *      — dlaczego List.of(p1).contains(p2) zwraca false, choć p1.equals(p2) zwraca true?
     *   4. Czym różni się @Deprecated(forRemoval = true) od zwykłego @Deprecated z punktu widzenia kompilatora?
     *   5. ZNAJDŹ BŁĄD:  @SuppressWarnings("unchecked") nad całą klasą Repository z 30 metodami.
     *   6. Co wypisze kompilator dla:  @FunctionalInterface interface Task { void run(); String toString(); }  ?
     *   7. Dlaczego @SafeVarargs nie można postawić na zwykłej (nie final, nie private) metodzie instancyjnej?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** NoRetention = adnotacja BEZ meta-adnotacji Retention (do ćwiczenia 1). */
    @interface NoRetention {
    }

    /** LegacyApi = stare API z częścią metod wycofanych (do ćwiczenia 2). */
    static final class LegacyApi {
        @Deprecated
        static String oldFormat(int value) {
            return "#" + value;
        }

        @Deprecated(since = "3.0", forRemoval = true)
        static int oldTotal(int a, int b) {
            return a + b;
        }

        static String format(int value) {
            return String.format(Locale.ROOT, "%05d", value);
        }

        static int total(int a, int b) {
            return Math.addExact(a, b);   // addExact = dodaj dokładnie (wyjątek przy przepełnieniu)
        }
    }

    /** TwoAbstract = interfejs z dwiema metodami abstrakcyjnymi — NIE jest funkcyjny (do ćwiczenia 4). */
    interface TwoAbstract {
        void first();

        void second();

        @Override
        String toString();
    }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: retencja @Override", "SOURCE", () -> exercise1(Override.class));
        Check.equal("ćw. 1b: retencja @Deprecated", "RUNTIME", () -> exercise1(Deprecated.class));
        Check.equal("ćw. 1c: retencja bez Retention", "CLASS", () -> exercise1(NoRetention.class));
        Check.equal("ćw. 2: wycofane metody LegacyApi", List.of("oldFormat", "oldTotal"), () -> exercise2(LegacyApi.class));
        Check.equal("ćw. 3a: suma z tekstu", 42, () -> exercise3("10 20  12"));
        Check.equal("ćw. 3b: spacje na brzegach", 5, () -> exercise3("  5 "));
        Check.equal("ćw. 4a: Comparator", List.of("compare"), () -> exercise4(Comparator.class));
        Check.equal("ćw. 4b: TwoAbstract", List.of("first", "second"), () -> exercise4(TwoAbstract.class));
        Check.equal("ćw. 4c: PriceRule", List.of("apply"), () -> exercise4(PriceRule.class));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", "SOURCE", () -> solution1(Override.class));
        Check.equal("ćw. 1b (wzorzec)", "RUNTIME", () -> solution1(Deprecated.class));
        Check.equal("ćw. 1c (wzorzec)", "CLASS", () -> solution1(NoRetention.class));
        Check.equal("ćw. 2 (wzorzec)", List.of("oldFormat", "oldTotal"), () -> solution2(LegacyApi.class));
        Check.equal("ćw. 3a (wzorzec)", 42, () -> solution3("10 20  12"));
        Check.equal("ćw. 3b (wzorzec)", 5, () -> solution3("  5 "));
        Check.equal("ćw. 4a (wzorzec)", List.of("compare"), () -> solution4(Comparator.class));
        Check.equal("ćw. 4b (wzorzec)", List.of("first", "second"), () -> solution4(TwoAbstract.class));
        Check.equal("ćw. 4c (wzorzec)", List.of("apply"), () -> solution4(PriceRule.class));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 9 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć nazwę retencji adnotacji type: "SOURCE", "CLASS" albo "RUNTIME".
     * Podpowiedź: type.getAnnotation(Retention.class); gdy wynik to null — obowiązuje wartość domyślna (jaka?).
     */
    static String exercise1(Class<? extends Annotation> type) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć posortowane nazwy metod ZADEKLAROWANYCH w klasie type, które mają @Deprecated.
     * Podpowiedź: Arrays.stream(type.getDeclaredMethods()), filter z isAnnotationPresent, map(Method::getName), sorted.
     */
    static List<String> exercise2(Class<?> type) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ bez przestarzałego API (strumienie z t16_streams). Stary kod:
     * <pre>{@code
     * int sum = 0;
     * for (String token : text.split(" ")) {
     *     if (!token.isEmpty() && !Character.isSpace(token.charAt(0))) {
     *         sum += new Integer(token).intValue();
     *     }
     * }
     * return sum;
     * }</pre>
     * Podpowiedź: text.strip().split("\\s+") (strip — Java 11+), potem mapToInt(Integer::parseInt).sum().
     */
    static int exercise3(String text) {
        // TODO: twoje rozwiązanie
        return 0;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć posortowane nazwy metod abstrakcyjnych interfejsu iface, które LICZĄ SIĘ
     * do „funkcyjności” — czyli bez publicznych metod z Object (equals, hashCode, toString).
     * Podpowiedź: iface.getMethods() + Modifier.isAbstract; metodę z Object rozpoznasz próbą
     * Object.class.getMethod(m.getName(), m.getParameterTypes()) — NoSuchMethodException = „nie z Object”.
     */
    static List<String> exercise4(Class<?> iface) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(Class<? extends Annotation> type) {
        Retention retention = type.getAnnotation(Retention.class);
        return retention == null ? "CLASS" : retention.value().name();
    }

    static List<String> solution2(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Deprecated.class))
                .map(Method::getName)
                .sorted()
                .toList();
    }

    static int solution3(String text) {
        return Arrays.stream(text.strip().split("\\s+"))
                .mapToInt(Integer::parseInt)
                .sum();
    }

    static List<String> solution4(Class<?> iface) {
        return Arrays.stream(iface.getMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .filter(m -> !isPublicObjectMethod(m))
                .map(Method::getName)
                .sorted()
                .toList();
    }

    static boolean isPublicObjectMethod(Method m) {
        try {
            Object.class.getMethod(m.getName(), m.getParameterTypes());
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Nie. Adnotacja to tylko metadane. Czyta ją kompilator (np. @Override), procesor adnotacji podczas kompilacji
     *      (np. Lombok) albo kod w czasie działania przez refleksję (np. Spring) — i dopiero ten czytelnik coś robi.
     *   2. SOURCE — @Override jest potrzebna tylko kompilatorowi i nie trafia do pliku .class.
     *   3. equals(Point) to przeciążenie, a nie nadpisanie. Kolekcje wołają equals(Object), które dalej porównuje
     *      referencje. Z @Override kompilator zgłosiłby błąd; poprawnie: public boolean equals(Object o) (+ hashCode).
     *   4. Zwykłe @Deprecated daje ostrzeżenie [deprecation], forRemoval = true daje mocniejsze [removal] (zapowiedź
     *      usunięcia). Wycisza się je osobno: @SuppressWarnings("deprecation") / @SuppressWarnings("removal").
     *   5. Za szeroki zasięg: wyciszenie obejmie też przyszłe, prawdziwe problemy w 30 metodach. Wyciszaj na jednej
     *      zmiennej lub metodzie, z komentarzem dlaczego to bezpieczne.
     *   6. Nic — skompiluje się bez błędu. toString() jest publiczną metodą z Object, więc się nie liczy; zostaje
     *      jedna metoda abstrakcyjna run().
     *   7. Bo zwykłą metodę instancyjną podklasa może nadpisać i zrobić z tablicą coś groźnego — obietnica przestałaby
     *      być prawdziwa. Metod static, final, private i konstruktorów nadpisać się nie da.
     */
    // </editor-fold>
}
