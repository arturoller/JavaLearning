package t19_annotations_reflection;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Product;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Refleksja — zaglądanie do budowy klas w czasie działania programu
 *        (reflection = refleksja, „odbicie w lustrze”: program ogląda sam siebie)
 *
 * W SKRÓCIE:
 *   Każda klasa ma w JVM swój obiekt Class. Przez niego możesz w czasie działania programu poznać pola, metody,
 *   konstruktory i adnotacje, wywołać metodę po nazwie, odczytać pole prywatne albo utworzyć obiekt — bez pisania
 *   tego w kodzie wprost. Tak działają Spring, JUnit, Jackson i Hibernate. Ty w codziennym kodzie raczej nie.
 *
 * ANALOGIA: rentgen.
 *   Zwykle widzisz obiekt „z zewnątrz” — przez jego publiczne metody (jak człowieka w ubraniu). Refleksja to rentgen:
 *   widzisz wszystkie kości, także prywatne. Świetne narzędzie dla lekarza (frameworka), ale nie do codziennej
 *   rozmowy — i każde prześwietlenie kosztuje.
 *
 * JAK TO DZIAŁA:
 *   Class        ← obiekt opisujący klasę:  Point.class / p.getClass() / Class.forName("pakiet.Zewn$Wewn")
 *    ├─ Field         getDeclaredFields()        → get(obiekt), set(obiekt, wartość)
 *    ├─ Method        getDeclaredMethods()       → invoke(obiekt, argumenty...)
 *    ├─ Constructor   getDeclaredConstructors()  → newInstance(argumenty...)
 *    └─ RecordComponent (rekordy, Java 16+), getEnumConstants() (enumy)
 *   getDeclaredX = wszystko zadeklarowane W TEJ klasie (także prywatne), bez odziedziczonych;
 *   getX         = tylko publiczne, ale razem z odziedziczonymi.
 *
 * SŁÓWKA:
 *   reflection = refleksja; declared = zadeklarowany; field = pole; method = metoda; constructor = konstruktor;
 *   invoke = wywołaj; instance = instancja (obiekt); accessible = dostępny; modifier = modyfikator;
 *   invocation target = cel wywołania; inaccessible = niedostępny; bridge method = metoda mostkowa;
 *   synthetic = syntetyczny (wygenerowany przez kompilator); nest mate = współlokator (klasa z tego samego „gniazda”).
 *
 * ZOBACZ TEŻ: t19_annotations_reflection/Annotations02Custom (adnotacje czytane refleksją),
 *             t11_generics/Generics06ErasureLimits (wymazywanie, metody mostkowe), t09_records/Records01Basics,
 *             t08_enums/Enums01Basics, t26_jvm/Jvm02ClassLoadingInit (ładowanie klas).
 * </pre>
 */
public class Annotations03ReflectionBasics {

    // ---------------------------------------------------------------------------------------------
    // Typy do oglądania
    // ---------------------------------------------------------------------------------------------

    /** Shape = kształt. */
    interface Shape {
        double area();                       // area = pole powierzchni
    }

    /** Point = punkt. Ma pola różnego rodzaju, kilka konstruktorów i metod — idealny „pacjent” do prześwietlenia. */
    static class Point implements Shape, Comparable<Point> {
        public static final Point ORIGIN = new Point(0, 0);   // origin = początek układu
        private final int x;
        private final int y;
        protected String label = "P";                          // label = etykieta

        public Point() {
            this(0, 0);
        }

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        private Point(String label) {
            this(0, 0);
            this.label = label;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        /** scaled = przeskalowany; factor = współczynnik. Rzuca wyjątek dla factor ≤ 0. */
        public Point scaled(int factor) {
            if (factor <= 0) {
                throw new IllegalArgumentException("współczynnik musi być > 0, jest: " + factor);
            }
            return new Point(x * factor, y * factor);
        }

        public static Point of(int x, int y) {
            return new Point(x, y);
        }

        private String secret() {
            return "tajne: " + label;
        }

        @Override
        public double area() {
            return 0.0;
        }

        @Override
        public int compareTo(Point other) {
            return Integer.compare(x * x + y * y, other.x * other.x + other.y * other.y);
        }

        @Override
        public String toString() {
            return "Point(" + x + ", " + y + ")";
        }
    }

    /** Account = konto. Konstruktor pilnuje, by saldo nie było ujemne. Refleksja potrafi to ominąć. */
    static final class Account {
        private final String owner;            // owner = właściciel
        private int balance;                   // balance = saldo

        Account(String owner, int balance) {
            if (balance < 0) {
                throw new IllegalArgumentException("saldo nie może być ujemne: " + balance);
            }
            this.owner = owner;
            this.balance = balance;
        }

        @Override
        public String toString() {
            return "Account[" + owner + ", saldo=" + balance + "]";
        }
    }

    /** Money = kwota (rekord). Pola rekordu są naprawdę niezmienne — nawet dla refleksji. */
    record Money(int amount) {
    }

    /** Level = poziom (prosty enum). */
    enum Level { LOW, HIGH }

    /** Op = operacja: stałe enuma z własnym ciałem (każda to anonimowa podklasa!). */
    enum Op {
        PLUS {
            @Override
            int apply(int a, int b) {
                return a + b;
            }
        };

        abstract int apply(int a, int b);
    }

    public static void main(String[] args) throws Exception {
        title("Annotations03 — podstawy refleksji");

        classObjects();          // class objects = obiekty Class
        fields();                // fields = pola
        methodsAndConstructors();// methods and constructors = metody i konstruktory
        invokingMethods();       // invoking methods = wywoływanie metod
        privateAccess();         // private access = dostęp do prywatnych składowych
        creatingInstances();     // creating instances = tworzenie obiektów
        recordsAndEnums();       // records and enums = rekordy i enumy
        costAndDangers();        // cost and dangers = koszt i zagrożenia
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. OBIEKT Class
    // =================================================================================================

    /** 1. Trzy drogi do obiektu Class. Każda klasa ma w JVM (dla danego ładowacza klas) dokładnie jeden taki obiekt. */
    static void classObjects() throws ClassNotFoundException {
        section("1. Obiekt Class — trzy sposoby");

        Class<Point> byLiteral = Point.class;                     // literał klasy — znany w czasie kompilacji
        Class<?> byInstance = new Point(3, 4).getClass();         // getClass = pobierz klasę obiektu
        // forName = po nazwie (np. z pliku konfiguracyjnego). Klasa zagnieżdżona: Zewnętrzna$Wewnętrzna!
        Class<?> byName = Class.forName("t19_annotations_reflection.Annotations03ReflectionBasics$Point");
        show("ten sam obiekt Class?", byLiteral == byInstance && byInstance == byName);
        // WYNIK: ten sam obiekt Class? → true

        show("getName()", byLiteral.getName());                   // nazwa binarna (z $)
        show("getSimpleName()", byLiteral.getSimpleName());       // prosta nazwa
        show("getCanonicalName()", byLiteral.getCanonicalName()); // nazwa „jak w kodzie” (z kropkami)
        // WYNIK: getName() → t19_annotations_reflection.Annotations03ReflectionBasics$Point
        // WYNIK: getSimpleName() → Point
        // WYNIK: getCanonicalName() → t19_annotations_reflection.Annotations03ReflectionBasics.Point

        show("getSuperclass()", byLiteral.getSuperclass().getSimpleName());   // nadklasa
        show("getInterfaces()", Arrays.stream(byLiteral.getInterfaces()).map(Class::getSimpleName).toList());
        show("String[].class.getName()", String[].class.getName());
        // WYNIK: getSuperclass() → Object
        // WYNIK: getInterfaces() → [Shape, Comparable]    ← kolejność jak w „implements”
        // WYNIK: String[].class.getName() → [Ljava.lang.String;    ← tak JVM nazywa tablice

        expectThrows("Class.forName z kropką zamiast $",
                () -> Class.forName("t19_annotations_reflection.Annotations03ReflectionBasics.Point"));
        // WYNIK: ✔ Class.forName z kropką zamiast $ → rzucono ClassNotFoundException: t19_annotations_reflection.Annotations03ReflectionBasics.Point

        // PUŁAPKA: Class.forName potrzebuje nazwy BINARNEJ: pełny pakiet + „$” przed klasą zagnieżdżoną. Kropka
        //   (nazwa kanoniczna) nie działa. Dodatkowo forName INICJALIZUJE klasę (uruchamia bloki static).
    }

    // =================================================================================================
    // 2. POLA
    // =================================================================================================

    /** 2. getDeclaredFields: wszystkie pola tej klasy. Kolejność NIE jest gwarantowana — zawsze sortuj. */
    static void fields() {
        section("2. Pola: getDeclaredFields, getFields, Modifier");

        Field[] declared = Point.class.getDeclaredFields();
        Arrays.sort(declared, Comparator.comparing(Field::getName));
        for (Field f : declared) {
            // Modifier.toString = zamień liczbę z bitami modyfikatorów na tekst
            System.out.printf(Locale.ROOT, "   %-7s %-7s %s%n", f.getName(), f.getType().getSimpleName(),
                    Modifier.toString(f.getModifiers()));
        }
        // WYNIK:    ORIGIN  Point   public static final
        // WYNIK:    label   String  protected
        // WYNIK:    x       int     private final
        // WYNIK:    y       int     private final

        show("getFields() (publiczne + odziedziczone)",
                Arrays.stream(Point.class.getFields()).map(Field::getName).sorted().toList());
        // WYNIK: getFields() (publiczne + odziedziczone) → [ORIGIN]

        // Modyfikatory to bity w jednej liczbie int: private = 2, static = 8, final = 16, ...
        int mods = declared[2].getModifiers();   // pole x (po sortowaniu: ORIGIN, label, x, y)
        show("x: getModifiers()", mods);
        show("x: isPrivate / isFinal / isStatic",
                Modifier.isPrivate(mods) + " / " + Modifier.isFinal(mods) + " / " + Modifier.isStatic(mods));
        // WYNIK: x: getModifiers() → 18    ← 2 (private) + 16 (final)
        // WYNIK: x: isPrivate / isFinal / isStatic → true / true / false

        // PUŁAPKA: zakładanie kolejności z getDeclaredFields/Methods — dokumentacja mówi, że NIE jest określona.
    }

    // =================================================================================================
    // 3. METODY I KONSTRUKTORY
    // =================================================================================================

    /** signature = sygnatura metody w czytelnej postaci: nazwa(typy): typZwracany. */
    static String signature(Method m) {
        String params = Arrays.stream(m.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));   // joining = połącz napisy
        return m.getName() + "(" + params + "): " + m.getReturnType().getSimpleName();
    }

    /** 3. Metody i konstruktory — też sortujemy. Uwaga na metody SYNTETYCZNE, dodane przez kompilator. */
    static void methodsAndConstructors() {
        section("3. Metody i konstruktory");

        Method[] all = Point.class.getDeclaredMethods();
        List<String> visible = Arrays.stream(all)
                .filter(m -> !m.isSynthetic())                    // isSynthetic = czy wygenerowana przez kompilator
                .map(Annotations03ReflectionBasics::signature)
                .sorted()
                .toList();
        show("liczba getDeclaredMethods()", all.length);
        showEach("metody z kodu (bez syntetycznych)", visible);
        // WYNIK: liczba getDeclaredMethods() → 9
        // WYNIK: metody z kodu (bez syntetycznych) (liczba elementów: 8):
        // WYNIK:    • area(): double
        // WYNIK:    • compareTo(Point): int
        // WYNIK:    • getX(): int
        // WYNIK:    • getY(): int
        // WYNIK:    • of(int, int): Point
        // WYNIK:    • scaled(int): Point
        // WYNIK:    • secret(): String
        // WYNIK:    • toString(): String

        List<String> synthetic = Arrays.stream(all).filter(Method::isSynthetic).map(Annotations03ReflectionBasics::signature).toList();
        show("metody syntetyczne", synthetic);
        // WYNIK: metody syntetyczne → [compareTo(Object): int]    ← metoda mostkowa (bridge) dla Comparable<Point>

        List<String> constructors = Arrays.stream(Point.class.getDeclaredConstructors())
                .map(c ->(Modifier.toString(c.getModifiers()) + " Point(" + Arrays.stream(c.getParameterTypes())
                        .map(Class::getSimpleName).collect(Collectors.joining(", ")) + ")").strip())
                .sorted()
                .toList();
        showEach("konstruktory", constructors);
        // WYNIK: konstruktory (liczba elementów: 3):
        // WYNIK:    • private Point(String)
        // WYNIK:    • public Point()
        // WYNIK:    • public Point(int, int)

        // PUŁAPKA: przez wymazywanie typów (t11_generics) kompilator dodaje mostek compareTo(Object) → compareTo(Point).
        //   Kod szukający lub liczący metody musi pomijać isSynthetic(), inaczej zobaczy tę samą metodę dwa razy.
    }

    // =================================================================================================
    // 4. WYWOŁYWANIE METOD
    // =================================================================================================

    /**
     * 4. Method.invoke(obiekt, argumenty...). Wynik zawsze jako Object (typy proste opakowane). Wyjątek rzucony
     * PRZEZ metodę przychodzi opakowany w InvocationTargetException — prawdziwy błąd siedzi w getCause().
     */
    static void invokingMethods() throws ReflectiveOperationException {
        section("4. Method.invoke i InvocationTargetException");

        Point p = new Point(3, 4);
        Method getX = Point.class.getMethod("getX");           // getMethod = pobierz publiczną metodę
        Method of = Point.class.getMethod("of", int.class, int.class);
        show("getX.invoke(p)", getX.invoke(p));                // int wraca opakowany jako Integer
        show("of.invoke(null, 1, 2) — statyczna", of.invoke(null, 1, 2));   // dla static obiekt = null
        // WYNIK: getX.invoke(p) → 3
        // WYNIK: of.invoke(null, 1, 2) — statyczna → Point(1, 2)

        Method scaled = Point.class.getMethod("scaled", int.class);
        expectThrows("scaled.invoke(p, 0)", () -> scaled.invoke(p, 0));
        // WYNIK: ✔ scaled.invoke(p, 0) → rzucono InvocationTargetException: (brak komunikatu)
        try {
            scaled.invoke(p, 0);
        } catch (InvocationTargetException e) {
            show("e.getCause()", e.getCause());                 // getCause = przyczyna — prawdziwy wyjątek
        }
        // WYNIK: e.getCause() → java.lang.IllegalArgumentException: współczynnik musi być > 0, jest: 0

        expectThrows("zły typ argumentu", () -> scaled.invoke(p, "dwa"));
        expectThrows("zła liczba argumentów", () -> scaled.invoke(p));
        // WYNIK: ✔ zły typ argumentu → rzucono IllegalArgumentException: argument type mismatch
        // WYNIK: ✔ zła liczba argumentów → rzucono IllegalArgumentException: wrong number of arguments

        // PUŁAPKA: logowanie samego e.getMessage() z InvocationTargetException daje „null” — informacja o błędzie
        //   ginie. Zawsze rozpakuj: e.getCause() (i najczęściej rzuć ją dalej lub opakuj, jak w t10_exceptions).
        // DOBRA PRAKTYKA: odróżniaj IllegalArgumentException z samego invoke (źle WYWOŁAŁEŚ: typy, liczba
        //   argumentów) od InvocationTargetException (metoda wykonała się i SAMA rzuciła wyjątek).
    }

    // =================================================================================================
    // 5. POLA PRYWATNE, setAccessible I MODUŁY
    // =================================================================================================

    /**
     * 5. Odczyt i zapis pól prywatnych. Klasy zagnieżdżone w tej samej klasie zewnętrznej to „współlokatorzy”
     * (nest mates, Java 11+) — mają do siebie dostęp nawet przez refleksję. Obca klasa potrzebuje setAccessible(true),
     * a klasy JDK są dodatkowo chronione przez moduły (Java 9+).
     */
    static void privateAccess() throws ReflectiveOperationException {
        section("5. Pola prywatne: setAccessible i moduły");

        Account account = new Account("Ala", 100);
        Field balance = Account.class.getDeclaredField("balance");
        show("balance.get(account) — współlokator", balance.get(account));
        // WYNIK: balance.get(account) — współlokator → 100

        balance.setAccessible(true);                // setAccessible = ustaw dostępność (wyłącz kontrolę dostępu)
        balance.set(account, -500);                 // OMINĘLIŚMY walidację z konstruktora (saldo ≥ 0)!
        show("konto po „włamaniu”", account);
        // WYNIK: konto po „włamaniu” → Account[Ala, saldo=-500]

        Point p = new Point(3, 4);
        Field x = Point.class.getDeclaredField("x");
        x.setAccessible(true);
        x.set(p, 99);                               // pole private FINAL w zwykłej klasie — da się zmienić!
        show("Point po zmianie pola final", p);
        // WYNIK: Point po zmianie pola final → Point(99, 4)

        Field amount = Money.class.getDeclaredField("amount");
        amount.setAccessible(true);
        Money money = new Money(5);
        expectThrows("zmiana pola rekordu", () -> amount.set(money, 99));
        // WYNIK: ✔ zmiana pola rekordu → rzucono IllegalAccessException: Can not set final int field t19_annotations_reflection.Annotations03ReflectionBasics$Money.amount to java.lang.Integer

        // Klasa z JDK, nie współlokator: bez setAccessible — IllegalAccessException.
        List<String> list = new ArrayList<>(List.of("a", "b"));
        Field size = ArrayList.class.getDeclaredField("size");
        expectThrows("size.get(list)", () -> size.get(list));
        // WYNIK: ✔ size.get(list) → rzucono IllegalAccessException: class t19_annotations_reflection.Annotations03ReflectionBasics cannot access a member of class java.util.ArrayList (in module java.base) with modifiers "private"
        show("size.trySetAccessible()", size.trySetAccessible());   // trySetAccessible = spróbuj (Java 9+): false zamiast wyjątku
        // WYNIK: size.trySetAccessible() → false    ← moduł java.base nie otwiera pakietu java.util

        Field value = String.class.getDeclaredField("value");
        expectThrows("value.setAccessible(true) na String", () -> value.setAccessible(true));
        // (wynik zależy od uruchomienia) — komunikat kończy się numerem modułu, np. „... to unnamed module @1b6d3586”
        try {
            value.setAccessible(true);
        } catch (InaccessibleObjectException e) {   // InaccessibleObjectException = obiekt niedostępny (Java 9+)
            String message = e.getMessage();
            show("stała część komunikatu", message.substring(0, message.indexOf(" to unnamed module")));
        }
        // WYNIK: stała część komunikatu → Unable to make field private final byte[] java.lang.String.value accessible: module java.base does not "opens java.lang"

        // PUŁAPKA: setAccessible(true) działa na Twoich klasach, ale na wnętrzu JDK rzuca InaccessibleObjectException
        //   (od Java 16 domyślnie; w Java 17 nie da się już tego wyłączyć opcją --illegal-access). Stare biblioteki,
        //   które „grzebały” w JDK, wymagają opcji JVM --add-opens. Pól rekordów i pól static final nie zmienisz
        //   nawet po setAccessible — set rzuci IllegalAccessException.
        // DOBRA PRAKTYKA: refleksja na polach prywatnych łamie hermetyzację (encapsulation) — walidacja z konstruktora
        //   po prostu nie zadziałała. To narzędzie dla frameworków (np. wstrzykiwanie pól), nie dla logiki biznesowej.
    }

    // =================================================================================================
    // 6. TWORZENIE OBIEKTÓW
    // =================================================================================================

    /** 6. getDeclaredConstructor(typy...).newInstance(argumenty...). Tak frameworki tworzą Twoje obiekty. */
    static void creatingInstances() throws ReflectiveOperationException {
        section("6. Tworzenie obiektów: getDeclaredConstructor().newInstance()");

        Point empty = Point.class.getDeclaredConstructor().newInstance();
        Point withArgs = Point.class.getDeclaredConstructor(int.class, int.class).newInstance(7, 8);
        Constructor<Point> privateCtor = Point.class.getDeclaredConstructor(String.class);
        privateCtor.setAccessible(true);            // konstruktor prywatny — frameworki robią dokładnie to
        Point labelled = privateCtor.newInstance("Q");
        show("konstruktor ()", empty);
        show("konstruktor (int, int)", withArgs);
        show("prywatny (String) → secret()", labelled.secret());
        // WYNIK: konstruktor () → Point(0, 0)
        // WYNIK: konstruktor (int, int) → Point(7, 8)
        // WYNIK: prywatny (String) → secret() → tajne: Q

        expectThrows("Account bez konstruktora ()", () -> Account.class.getDeclaredConstructor());
        expectThrows("konstruktor rzuca wyjątek",
                () -> Account.class.getDeclaredConstructor(String.class, int.class).newInstance("Ola", -5));
        // WYNIK: ✔ Account bez konstruktora () → rzucono NoSuchMethodException: t19_annotations_reflection.Annotations03ReflectionBasics$Account.<init>()
        // WYNIK: ✔ konstruktor rzuca wyjątek → rzucono InvocationTargetException: (brak komunikatu)

        // PUŁAPKA: stare Class.newInstance() jest przestarzałe od Java 9 — przepuszczało wyjątki sprawdzane rzucone
        //   przez konstruktor, choć ich nie deklarowało. Używaj getDeclaredConstructor().newInstance().
        // DOBRA PRAKTYKA: dlatego JPA/Hibernate i wiele bibliotek JSON wymagają konstruktora bez argumentów —
        //   to najprostszy sposób, by framework utworzył obiekt, nie znając jego parametrów („<init>()” powyżej).
    }

    // =================================================================================================
    // 7. REKORDY I ENUMY
    // =================================================================================================

    /** 7. Rekordy mają składniki (getRecordComponents, Java 16+) w kolejności z nagłówka. Enumy — stałe w kolejności. */
    static void recordsAndEnums() throws ReflectiveOperationException {
        section("7. Rekordy i enumy");

        Product laptop = SampleData.products().get(0);
        show("Product.isRecord()", Product.class.isRecord());
        for (RecordComponent rc : Product.class.getRecordComponents()) {
            show(rc.getName() + " : " + rc.getType().getSimpleName(), rc.getAccessor().invoke(laptop));
        }
        // WYNIK: Product.isRecord() → true
        // WYNIK: sku : String → ELE-001
        // WYNIK: name : String → Laptop Pro 14
        // WYNIK: category : Category → ELEKTRONIKA
        // WYNIK: price : BigDecimal → 5499.99
        // WYNIK: stock : int → 7

        show("Category.getEnumConstants()", Arrays.toString(Category.class.getEnumConstants()));
        show("Enum.valueOf(Category, \"KSIAZKI\")", Enum.valueOf(Category.class, "KSIAZKI"));
        // WYNIK: Category.getEnumConstants() → [ELEKTRONIKA, SPOZYWCZE, KSIAZKI, ODZIEZ, DOM]
        // WYNIK: Enum.valueOf(Category, "KSIAZKI") → KSIAZKI

        show("Op.PLUS.getClass().isEnum()", Op.PLUS.getClass().isEnum());
        show("Op.PLUS.getDeclaringClass().isEnum()", Op.PLUS.getDeclaringClass().isEnum());
        // WYNIK: Op.PLUS.getClass().isEnum() → false    ← stała z ciałem to anonimowa podklasa Op
        // WYNIK: Op.PLUS.getDeclaringClass().isEnum() → true

        Constructor<Level> levelCtor = Level.class.getDeclaredConstructor(String.class, int.class);
        levelCtor.setAccessible(true);
        expectThrows("nowa stała enuma przez refleksję", () -> levelCtor.newInstance("MEDIUM", 2));
        // WYNIK: ✔ nowa stała enuma przez refleksję → rzucono IllegalArgumentException: Cannot reflectively create enum objects

        // PUŁAPKA: przy stałych z własnym ciałem getClass() zwraca anonimową podklasę, a nie typ enuma. Do pytania
        //   „jakiego enuma to stała?” używaj getDeclaringClass().
        // DOBRA PRAKTYKA: Constructor.newInstance odmawia tworzenia stałych enuma — dlatego enum to najpewniejszy
        //   singleton (t22_design_patterns/Patterns04Singleton): nikt nie „dorobi” drugiej instancji refleksją.
    }

    // =================================================================================================
    // 8. KOSZT I ZAGROŻENIA
    // =================================================================================================

    /** 8. PRZED/PO: refleksja „bo można” kontra zwykłe wywołanie. Refleksja przesuwa błędy z kompilacji do działania. */
    static void costAndDangers() throws ReflectiveOperationException {
        section("8. Koszt i zagrożenia refleksji");

        Point p = new Point(3, 4);
        int before = (Integer) Point.class.getMethod("getX").invoke(p);   // PRZED: nazwa w napisie + rzutowanie
        int after = p.getX();                                             // PO: kompilator wszystko sprawdza
        show("PRZED (refleksja) / PO (wywołanie)", before + " / " + after);
        // WYNIK: PRZED (refleksja) / PO (wywołanie) → 3 / 3

        expectThrows("literówka „getXX” wychodzi dopiero w działaniu", () -> Point.class.getMethod("getXX"));
        // WYNIK: ✔ literówka „getXX” wychodzi dopiero w działaniu → rzucono NoSuchMethodException: t19_annotations_reflection.Annotations03ReflectionBasics$Point.getXX()

        // ZAGROŻENIE                      DLACZEGO
        // brak kontroli kompilatora      nazwy w napisach, typy jako Object → błędy dopiero w działaniu
        // łamanie hermetyzacji           setAccessible omija private i walidację (sekcja 5)
        // wolniej                        kontrola dostępu, opakowywanie argumentów, trudniejsza optymalizacja JIT
        // refaktoryzacja „nie widzi”     zmiana nazwy metody w IDE nie poprawi napisu "getX"
        //
        // GDZIE REFLEKSJA MA SENS: frameworki i narzędzia — Spring (wstrzykiwanie zależności), JUnit (szukanie
        // metod z @Test), Jackson (JSON ↔ obiekt), Hibernate (wiersz ↔ obiekt), walidacja (Annotations04Validator).

        // DOBRA PRAKTYKA: w kodzie aplikacji zamiast refleksji użyj interfejsu, lambdy lub mapy nazwa → akcja
        //   (t13_lambdas). Refleksję zostaw kodowi, który MUSI działać na klasach, których nie zna w czasie kompilacji.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Class: X.class, obj.getClass(), Class.forName("pakiet.Zewn$Wewn") (nazwa binarna, z „$”).
     *   • getDeclaredFields/Methods/Constructors — wszystkie z tej klasy; getFields/Methods — publiczne + odziedziczone.
     *   • Kolejność niegwarantowana → sortuj; pomijaj isSynthetic(); Modifier.toString / isPrivate / isStatic(mods).
     *   • m.invoke(obiekt, args...) (static: obiekt = null); wyjątek metody → InvocationTargetException.getCause().
     *   • f.setAccessible(true) → get/set prywatnych; JDK → InaccessibleObjectException; trySetAccessible() → boolean.
     *   • Tworzenie: getDeclaredConstructor(typy...).newInstance(args...) — nie Class.newInstance().
     *   • Rekordy: getRecordComponents() + getAccessor(); enumy: getEnumConstants(), getDeclaringClass().
     *   • Refleksja = dla frameworków i narzędzi (Spring, JUnit, Jackson). W zwykłym kodzie: interfejsy i lambdy.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie są trzy sposoby uzyskania obiektu Class i kiedy używa się każdego z nich?
     *   2. Co wypisze:  System.out.println(String[].class.getName());  ?
     *   3. ZNAJDŹ BŁĄD:  Class.forName("pl.sklep.Order.Line")  — Line to klasa zagnieżdżona w Order.
     *   4. Co wypisze:  try { m.invoke(obj); } catch (InvocationTargetException e) { System.out.println(e.getMessage()); }
     *      gdy metoda rzuciła new IllegalStateException("x")  ?
     *   5. Dlaczego getDeclaredMethods() klasy implementującej Comparable<Point> zwraca „dwie” metody compareTo?
     *   6. Dlaczego setAccessible(true) na polu String.value rzuca wyjątek, a na polu własnej klasy działa?
     *   7. ZNAJDŹ BŁĄD:  test porównuje tekst złożony z getDeclaredFields() w kolejności zwróconej przez JVM.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** expectedLaptop = oczekiwana mapa składników laptopa (TreeMap = stała kolejność). */
    static Map<String, Object> expectedLaptop() {
        Map<String, Object> expected = new TreeMap<>();
        expected.put("sku", "ELE-001");
        expected.put("name", "Laptop Pro 14");
        expected.put("category", Category.ELEKTRONIKA);
        expected.put("price", new BigDecimal("5499.99"));
        expected.put("stock", 7);
        return expected;
    }

    static void exercises() {
        Product laptop = SampleData.products().get(0);
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: pola Point", List.of("label", "x", "y"), () -> exercise1(Point.class));
        Check.equal("ćw. 1b: pola Account", List.of("balance", "owner"), () -> exercise1(Account.class));
        Check.equal("ćw. 2: składniki laptopa", expectedLaptop(), () -> exercise2(laptop));
        Check.equal("ćw. 3a: toString Point", "Point{label=P, x=3, y=4}", () -> exercise3(new Point(3, 4)));
        Check.equal("ćw. 3b: toString Account", "Account{balance=100, owner=Ala}", () -> exercise3(new Account("Ala", 100)));
        Check.equal("ćw. 4: utwórz i wypełnij", "Point{label=Q, x=5, y=0}",
                () -> solution3(exercise4(Point.class, Map.of("x", 5, "label", "Q"))));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", List.of("label", "x", "y"), () -> solution1(Point.class));
        Check.equal("ćw. 1b (wzorzec)", List.of("balance", "owner"), () -> solution1(Account.class));
        Check.equal("ćw. 2 (wzorzec)", expectedLaptop(), () -> solution2(laptop));
        Check.equal("ćw. 3a (wzorzec)", "Point{label=P, x=3, y=4}", () -> solution3(new Point(3, 4)));
        Check.equal("ćw. 3b (wzorzec)", "Account{balance=100, owner=Ala}", () -> solution3(new Account("Ala", 100)));
        Check.equal("ćw. 4 (wzorzec)", "Point{label=Q, x=5, y=0}",
                () -> solution3(solution4(Point.class, Map.of("x", 5, "label", "Q"))));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć posortowane nazwy pól zadeklarowanych w type, z pominięciem pól static
     * i syntetycznych.
     * Podpowiedź: Modifier.isStatic(f.getModifiers()), f.isSynthetic(), map(Field::getName), sorted().
     */
    static List<String> exercise1(Class<?> type) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 2 (średnie): dla DOWOLNEGO rekordu (t09_records) zwróć mapę (TreeMap) nazwa składnika → wartość.
     * Podpowiedź: record.getClass().getRecordComponents(), rc.getAccessor().invoke(record). Wyjątek
     * ReflectiveOperationException opakuj w IllegalStateException (Check nie przyjmuje wyjątków sprawdzanych).
     */
    static Map<String, Object> exercise2(Record record) {
        // TODO: twoje rozwiązanie
        return new TreeMap<>();
    }

    /**
     * ĆWICZENIE 3 (średnie): „uniwersalny toString” — NazwaProsta{pole=wartość, ...}: pola niestatyczne,
     * posortowane po nazwie, także prywatne.
     * Podpowiedź: setAccessible(true), f.get(obj), StringJoiner(", ", "Nazwa{", "}") albo Collectors.joining.
     */
    static String exercise3(Object obj) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): PRZEPISZ na nowoczesne API i rozbuduj. Stary kod (przestarzały od Java 9):
     * <pre>{@code
     * T instance = type.newInstance();
     * }</pre>
     * Utwórz obiekt konstruktorem bez argumentów (getDeclaredConstructor), a potem dla każdej pary z values
     * ustaw pole o tej nazwie (getDeclaredField + setAccessible + set). Zwróć gotowy obiekt.
     * Podpowiedź: ReflectiveOperationException opakuj w IllegalStateException.
     */
    static <T> T exercise4(Class<T> type, Map<String, Object> values) {
        // TODO: twoje rozwiązanie
        return null;
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .filter(f -> !f.isSynthetic())
                .map(Field::getName)
                .sorted()
                .toList();
    }

    static Map<String, Object> solution2(Record record) {
        Map<String, Object> result = new TreeMap<>();
        try {
            for (RecordComponent rc : record.getClass().getRecordComponents()) {
                result.put(rc.getName(), rc.getAccessor().invoke(record));
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nie można odczytać rekordu", e);
        }
        return result;
    }

    static String solution3(Object obj) {
        Field[] fields = obj.getClass().getDeclaredFields();
        Arrays.sort(fields, Comparator.comparing(Field::getName));
        List<String> parts = new ArrayList<>();
        try {
            for (Field f : fields) {
                if (Modifier.isStatic(f.getModifiers()) || f.isSynthetic()) {
                    continue;
                }
                f.setAccessible(true);
                parts.add(f.getName() + "=" + f.get(obj));
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Brak dostępu do pola", e);
        }
        return obj.getClass().getSimpleName() + "{" + String.join(", ", parts) + "}";
    }

    static <T> T solution4(Class<T> type, Map<String, Object> values) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            T instance = constructor.newInstance();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                Field field = type.getDeclaredField(entry.getKey());
                field.setAccessible(true);
                field.set(instance, entry.getValue());
            }
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nie można utworzyć " + type.getSimpleName(), e);
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. X.class — gdy znasz typ w czasie kompilacji; obj.getClass() — gdy masz obiekt (daje typ RZECZYWISTY,
     *      np. podklasę); Class.forName("...") — gdy nazwa przychodzi z zewnątrz (konfiguracja, plik, wtyczka).
     *   2. [Ljava.lang.String;  — „[” oznacza tablicę, „L...;” typ obiektowy.
     *   3. Trzeba nazwy binarnej: Class.forName("pl.sklep.Order$Line"). Z kropką → ClassNotFoundException.
     *   4. null — InvocationTargetException nie ma własnego komunikatu. Prawdziwy wyjątek: e.getCause().getMessage() → x.
     *   5. Kompilator dodaje syntetyczną metodę mostkową compareTo(Object) (przez wymazywanie typów), która woła
     *      compareTo(Point). Odfiltruj ją przez isSynthetic() (albo isBridge()).
     *   6. Pakiet java.lang w module java.base nie jest „otwarty” (opens) dla Twojego kodu — moduły (Java 9+)
     *      chronią wnętrze JDK. Twoja klasa jest w tym samym, nienazwanym module, więc dostęp jest dozwolony.
     *   7. Kolejność z getDeclaredFields() nie jest gwarantowana — test może przechodzić u Ciebie i padać gdzie indziej.
     *      Posortuj pola po nazwie.
     */
    // </editor-fold>
}
