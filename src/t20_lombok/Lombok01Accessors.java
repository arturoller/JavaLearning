package t20_lombok;

import helpers.Check;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Lombok — gettery, settery, toString, equals/hashCode bez pisania ich ręcznie
 *        (accessor = metoda dostępowa; boilerplate = kod szablonowy/powtarzalny)
 *
 * W SKRÓCIE:
 *   Lombok to PROCESOR ADNOTACJI (annotation processor, patrz t19_annotations_reflection/Annotations07Processors):
 *   podczas kompilacji czyta adnotacje takie jak {@code @Getter} czy {@code @ToString} i DOPISUJE do klasy
 *   gotowy kod (do bajtkodu, nie do pliku .java). Mniej linii w repozytorium — ale trzeba znać efekt na pamięć,
 *   bo w kodzie źródłowym tego kodu po prostu NIE WIDAĆ.
 *
 * ANALOGIA: automat do przyszywania guzików.
 *   Zamiast ręcznie przyszywać każdy guzik (pisać getter po getterze), zaznaczasz tkaninę szpilkami
 *   (adnotacjami) i wrzucasz do automatu (kompilatora z Lombokiem). Automat przyszywa guziki dokładnie
 *   tam, gdzie wskazują szpilki — szybciej, ale musisz wiedzieć, jak wygląda gotowy szew, żeby go
 *   poprawnie ocenić i naprawić, gdy coś pójdzie nie tak.
 *
 * JAK TO DZIAŁA:
 *   1. javac uruchamia zarejestrowane procesory adnotacji (Lombok rejestruje się przez META-INF/services —
 *      patrz t19_annotations_reflection/Annotations07Processors).
 *   2. Lombok NIE generuje osobnego pliku .java — modyfikuje drzewo składniowe (AST) kompilatora wprost,
 *      korzystając z jego wewnętrznych (niepublicznych) klas. Dlatego działa inaczej niż zwykły procesor
 *      adnotacji (więcej o tej różnicy w Lombok04Other).
 *   3. Wynikowy .class zawiera metody tak, jakby ktoś je napisał ręcznie — widać je w refleksji i w IDE
 *      (podgląd źródła „po Lomboku”: akcja delombok, patrz Lombok04Other).
 *
 * SŁÓWKA:
 *   accessor = metoda dostępowa (getter/setter); boilerplate = kod szablonowy; getter/setter = metoda
 *   odczytująca/zapisująca pole; AccessLevel = poziom dostępu; exclude = wyklucz; callSuper = uwzględnij
 *   klasę bazową; field = pole; inherited = odziedziczony.
 *
 * ZOBACZ TEŻ: t09_records/Records01Basics (rekordy jako alternatywa dla Lomboka — patrz Lombok03DataValueBuilder),
 *             t06_oop_basics/Oop06Immutability (ręczne equals/hashCode i niezmienność),
 *             t19_annotations_reflection/Annotations03ReflectionBasics (refleksja użyta tu jako dowód).
 * </pre>
 */
public class Lombok01Accessors {

    // ---------------------------------------------------------------------------------------------
    // Klasy używane w lekcji (zagnieżdżone, statyczne — tak jak w poprzednich tematach: realny projekt
    // trzymałby każdą w osobnym pliku; tu wszystko musi się zmieścić w jednej lekcji).
    // ---------------------------------------------------------------------------------------------

    /**
     * PersonHandwritten = osoba, wersja RĘCZNA (PRZED). Gettery, settery, toString, equals, hashCode —
     * wszystko napisane samodzielnie. Ok. 40 linii kodu na trzy pola. To właśnie ten kod Lombok generuje
     * za nas (patrz PersonLombok niżej).
     */
    static class PersonHandwritten {
        private String firstName;
        private String lastName;
        private int age;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public String toString() {
            return "PersonHandwritten(firstName=" + firstName + ", lastName=" + lastName + ", age=" + age + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PersonHandwritten other)) {   // instanceof z wzorcem (Java 16+) — patrz t07
                return false;
            }
            return age == other.age
                    && Objects.equals(firstName, other.firstName)
                    && Objects.equals(lastName, other.lastName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(firstName, lastName, age);
        }
    }

    /**
     * PersonLombok = ta sama osoba — PO. {@code @Getter + @Setter + @ToString + @EqualsAndHashCode} na
     * poziomie KLASY = te same metody co w PersonHandwritten, ale bez pisania ich. Adnotacja na klasie
     * działa tak, jakby postawić ją przy każdym polu z osobna.
     */
    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    static class PersonLombok {
        private String firstName;
        private String lastName;
        private int age;
    }

    /**
     * Customer = klient. Pokazuje {@code AccessLevel}: {@code id} jest final (Lombok NIGDY nie generuje
     * dla pola final settera — przypisanie final pola po konstrukcji to błąd kompilacji), a
     * {@code internalNotes} jest jawnie wyłączone adnotacją {@code @Setter(AccessLevel.NONE)} mimo
     * klasowego {@code @Setter} (adnotacja na POLU zawsze wygrywa z adnotacją na KLASIE).
     */
    @Getter
    @Setter
    static class Customer {
        private final long id;
        private String email;
        @Setter(AccessLevel.NONE)
        private String internalNotes;

        Customer(long id, String email, String internalNotes) {
            this.id = id;
            this.email = email;
            this.internalNotes = internalNotes;
        }
    }

    /**
     * Flags = pola logiczne. {@code active} to primityw {@code boolean}, {@code vip} to opakowanie
     * {@code Boolean}. Sprawdzamy refleksją (sekcja 4), czy Lombok nazywa oba gettery tak samo
     * ({@code isX}), czy inaczej.
     */
    @Getter
    static class Flags {
        private boolean active;
        private Boolean vip;

        Flags(boolean active, Boolean vip) {
            this.active = active;
            this.vip = vip;
        }
    }

    /**
     * Document = dokument. {@code secret} wykluczony z toString ({@code @ToString.Exclude}) — tak
     * wyklucza się np. hasła, tokeny czy inne dane, których nie wolno wypisywać w logach.
     */
    @ToString
    static class Document {
        private final String id;
        private final String title;
        @ToString.Exclude
        private final String secret;

        Document(String id, String title, String secret) {
            this.id = id;
            this.title = title;
            this.secret = secret;
        }
    }

    /**
     * Point2D = punkt na płaszczyźnie. {@code includeFieldNames = false} → krótszy zapis
     * „Point2D(x, y)” zamiast „Point2D(x=.., y=..)”.
     */
    @ToString(includeFieldNames = false)
    static class Point2D {
        private final double x;
        private final double y;

        Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    /** Vehicle = pojazd, klasa bazowa. Zwykłe {@code @EqualsAndHashCode} — brak nadklasy, więc nie ma czego dołączać. */
    @Getter
    @ToString
    @EqualsAndHashCode
    static class Vehicle {
        private final String brand;

        Vehicle(String brand) {
            this.brand = brand;
        }
    }

    /**
     * Car = samochód, PODKLASA Vehicle. {@code callSuper = true} w {@code @ToString}/{@code @EqualsAndHashCode}
     * dolicza pola z klasy bazowej (tu: {@code brand}) do porównania i tekstu. Bez tego dwa samochody
     * różnych marek, ale z tą samą liczbą drzwi, byłyby sobie „równe” — patrz sekcja 7.
     */
    @Getter
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    static class Car extends Vehicle {
        private final int doors;

        Car(String brand, int doors) {
            super(brand);
            this.doors = doors;
        }
    }

    public static void main(String[] args) {
        title("Lombok01 — gettery, settery, toString, equals/hashCode");

        czymJestLombok();        // czym jest Lombok = what Lombok is
        gettersSetters();        // getters setters = gettery i settery
        accessLevel();           // access level = poziom dostępu
        booleanGetters();        // boolean getters = gettery dla pól logicznych
        toStringCustomization(); // toString customization = dostosowanie toString
        equalsHashCode();        // equals hashCode = równość i kod skrótu
        callSuperPitfall();      // call super pitfall = pułapka z klasą bazową
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. CZYM JEST LOMBOK — PRZED I PO
    // =================================================================================================

    /**
     * 1. PRZED/PO: PersonHandwritten (~40 linii) kontra PersonLombok (3 pola + 4 adnotacje = 8 linii).
     * Obie klasy mają IDENTYCZNY zestaw publicznych metod — dowód refleksją niżej.
     */
    static void czymJestLombok() {
        section("1. Czym jest Lombok — PRZED (ręcznie) i PO (adnotacje)");

        PersonHandwritten h = new PersonHandwritten();
        h.setFirstName("Jan");
        h.setLastName("Kowalski");
        h.setAge(30);
        show("PersonHandwritten (ręcznie)", h);
        // WYNIK: PersonHandwritten (ręcznie) → PersonHandwritten(firstName=Jan, lastName=Kowalski, age=30)

        PersonLombok l = new PersonLombok();
        l.setFirstName("Jan");
        l.setLastName("Kowalski");
        l.setAge(30);
        show("PersonLombok (Lombok)", l);
        // WYNIK: PersonLombok (Lombok) → Lombok01Accessors.PersonLombok(firstName=Jan, lastName=Kowalski, age=30)

        show("metody PersonLombok (posortowane)", methodNames(PersonLombok.class));
        // WYNIK: metody PersonLombok (posortowane) → canEqual, equals, getAge, getFirstName, getLastName, hashCode, setAge, setFirstName, setLastName, toString

        // CIEKAWOSTKA: nazwa klasy w toString to "Lombok01Accessors.PersonLombok", nie samo "PersonLombok".
        //   Dla klasy ZAGNIEŻDŻONEJ Lombok wstawia do tekstu literał ze zbudowaną w czasie KOMPILACJI
        //   nazwą „Zewnętrzna.Wewnętrzna” (nie woła getClass().getSimpleName() w czasie działania) —
        //   dzięki temu działa identycznie niezależnie od tego, jak załadowano klasę.

        // DOBRA PRAKTYKA: zanim dodasz adnotację, sprawdź (jak tutaj refleksją), co dokładnie generuje —
        //   nie zgaduj. W IDE (IntelliJ) wtyczka Lombok jest wbudowana, ale trzeba w Ustawieniach
        //   włączyć przetwarzanie adnotacji (annotation processing), inaczej IDE pokaże błędy
        //   „cannot find symbol” dla metod, które Lombok dopiero wygeneruje podczas kompilacji.
    }

    // =================================================================================================
    // 2. @Getter / @Setter
    // =================================================================================================

    /**
     * 2. {@code @Getter} i {@code @Setter} można postawić na POLU (jedna metoda) albo na KLASIE
     * (wszystkie pola na raz — pomija pola {@code static} i {@code final} tam, gdzie setter nie ma sensu).
     */
    static void gettersSetters() {
        section("2. @Getter i @Setter — na polu i na klasie");

        Customer c = new Customer(1L, "jan@example.com", "VIP od 2020");
        show("c.getId()", c.getId());
        show("c.getEmail()", c.getEmail());
        // WYNIK: c.getId() → 1
        // WYNIK: c.getEmail() → jan@example.com

        c.setEmail("jan.kowalski@example.com");
        show("po setEmail", c.getEmail());
        // WYNIK: po setEmail → jan.kowalski@example.com

        // Lombok generuje (dla pola „email” z @Setter):
        //   public void setEmail(String email) { this.email = email; }
        // a dla pola „id” (@Getter, bez settera):
        //   public long getId() { return id; }

        // PUŁAPKA: @Getter/@Setter na klasie NIE tworzy settera dla pól final (id) — próba przypisania
        //   final pola po konstrukcji to błąd kompilacji, więc Lombok po prostu go pomija (bez ostrzeżenia).
    }

    // =================================================================================================
    // 3. AccessLevel — poziom dostępu generowanych metod
    // =================================================================================================

    /**
     * 3. {@code AccessLevel.NONE} na POLU wyłącza generowanie konkretnej metody mimo adnotacji na klasie —
     * adnotacja na polu zawsze ma pierwszeństwo przed adnotacją na klasie.
     */
    static void accessLevel() {
        section("3. AccessLevel — poziom dostępu");

        show("metody Customer (posortowane)", methodNames(Customer.class));
        // WYNIK: metody Customer (posortowane) → getEmail, getId, getInternalNotes, setEmail

        boolean hasSetInternalNotes = Arrays.stream(Customer.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("setInternalNotes"));
        show("czy istnieje setInternalNotes", hasSetInternalNotes);
        // WYNIK: czy istnieje setInternalNotes → false

        // Lombok generuje (fragment, dla internalNotes z @Setter(AccessLevel.NONE)):
        //   // (celowo NIC — AccessLevel.NONE = "nie generuj tej metody")
        // Inne poziomy: PUBLIC (domyślny), PROTECTED, PACKAGE (pakietowy, bez słowa kluczowego),
        //   PRIVATE, MODULE (Java 9+ moduły).

        // DOBRA PRAKTYKA: AccessLevel.NONE na polu to czytelny sposób powiedzenia „to pole tylko do
        //   odczytu z zewnątrz, zmienia się tylko wewnątrz klasy” — bez pisania całego gettera ręcznie.
    }

    // =================================================================================================
    // 4. GETTERY DLA boolean / Boolean
    // =================================================================================================

    /**
     * 4. JavaBeans: getter logicznego pola zwykle nazywa się {@code isX}, nie {@code getX}. Sprawdzamy
     * refleksją, czy Lombok stosuje tę konwencję identycznie dla primitywu {@code boolean} i dla
     * opakowania {@code Boolean}.
     */
    static void booleanGetters() {
        section("4. Gettery dla pól logicznych — boolean kontra Boolean");

        show("metody Flags (posortowane)", methodNames(Flags.class));
        // WYNIK: metody Flags (posortowane) → getVip, isActive

        Flags f = new Flags(true, false);
        show("f.isActive()", f.isActive());
        show("f.getVip()", f.getVip());
        // WYNIK: f.isActive() → true
        // WYNIK: f.getVip() → false

        // JAK TO DZIAŁA: dla primitywu „boolean” Lombok stosuje konwencję JavaBeans „isX” (tak jak dla
        //   ręcznie pisanych klas). Dla opakowania „Boolean” pole MOŻE być null (trzy stany: true/false/
        //   null) — to już nie jest czysto logiczna wartość w sensie JavaBeans, więc Lombok generuje
        //   zwykły getter „getX”, tak jak dla każdego innego typu obiektowego.

        // PUŁAPKA: jeśli pole boolean nazywa się już „isReady”, Lombok NIE doda drugiego „is” — getter
        //   to po prostu „isReady()” (bez podwojenia). Warto nazywać pola logiczne bez przedrostka „is”
        //   (np. „active”, nie „isActive”) i zostawić przedrostek getterowi.
    }

    // =================================================================================================
    // 5. @ToString — exclude, includeFieldNames
    // =================================================================================================

    /** 5. {@code @ToString.Exclude} na polu usuwa je z wygenerowanego tekstu — przydatne dla haseł, tokenów, dużych danych. */
    static void toStringCustomization() {
        section("5. @ToString — wykluczanie pól, nazwy pól");

        Document d = new Document("DOC-1", "Umowa", "tajne-haslo-123");
        show("dokument", d);
        // WYNIK: dokument → Lombok01Accessors.Document(id=DOC-1, title=Umowa)

        show("punkt", new Point2D(3.5, -2.0));
        // WYNIK: punkt → Lombok01Accessors.Point2D(3.5, -2.0)

        // Lombok generuje (dla Document, pole secret z @ToString.Exclude):
        //   public String toString() {
        //       return "Lombok01Accessors.Document(id=" + this.id + ", title=" + this.title + ")";   // secret pominięty
        //   }
        // Domyślnie (bez wykluczeń) toString odwołuje się BEZPOŚREDNIO do pól (this.pole), nie przez
        //   gettery — ma to znaczenie, gdy getter jest nadpisany i robi coś więcej niż zwraca pole.

        // DOBRA PRAKTYKA: zawsze wykluczaj z toString dane wrażliwe (hasła, tokeny, numery kart) —
        //   toString trafia do logów częściej, niż się wydaje (np. przez domyślne logowanie wyjątków).
    }

    // =================================================================================================
    // 6. @EqualsAndHashCode — exclude
    // =================================================================================================

    /**
     * 6. {@code @EqualsAndHashCode} generuje parę equals/hashCode zgodną z kontraktem (patrz
     * t06_oop_basics/Oop06Immutability) — plus metodę {@code canEqual}, która chroni symetrię equals
     * przy dziedziczeniu (sprawdzana w sekcji 7).
     */
    static void equalsHashCode() {
        section("6. @EqualsAndHashCode");

        PersonLombok a = new PersonLombok();
        a.setFirstName("Ala");
        a.setLastName("Nowak");
        a.setAge(25);
        PersonLombok b = new PersonLombok();
        b.setFirstName("Ala");
        b.setLastName("Nowak");
        b.setAge(25);

        show("a.equals(b)", a.equals(b));
        show("a.hashCode() == b.hashCode()", a.hashCode() == b.hashCode());
        // WYNIK: a.equals(b) → true
        // WYNIK: a.hashCode() == b.hashCode() → true

        b.setAge(26);
        show("po zmianie wieku b: a.equals(b)", a.equals(b));
        // WYNIK: po zmianie wieku b: a.equals(b) → false

        // Lombok generuje w uproszczeniu (porównanie pole po polu, bez getterów):
        //   public boolean equals(Object o) {
        //       if (o == this) return true;
        //       if (!(o instanceof PersonLombok other)) return false;
        //       if (!other.canEqual(this)) return false;
        //       return age == other.age
        //               && Objects.equals(firstName, other.firstName)
        //               && Objects.equals(lastName, other.lastName);
        //   }
        //   public int hashCode() {
        //       int PRIME = 59, result = 1;
        //       result = result * PRIME + age;
        //       result = result * PRIME + (firstName == null ? 43 : firstName.hashCode());
        //       result = result * PRIME + (lastName == null ? 43 : lastName.hashCode());
        //       return result;
        //   }
        //   protected boolean canEqual(Object other) { return other instanceof PersonLombok; }

        // PUŁAPKA: @EqualsAndHashCode (podobnie jak ręczne equals/hashCode) powinno używać TYCH SAMYCH
        //   pól co do identyczności obiektu logicznie należą — pole czysto pomocnicze (np. cache,
        //   znacznik czasu ostatniego dostępu) wyklucz adnotacją @EqualsAndHashCode.Exclude na polu,
        //   inaczej dwa „te same” obiekty przestaną być sobie równe po samym odczycie z cache.
    }

    // =================================================================================================
    // 7. callSuper — PUŁAPKA w podklasach
    // =================================================================================================

    /**
     * 7. Gdy klasa DZIEDZICZY, domyślne {@code callSuper = false} ignoruje pola klasy bazowej.
     * Ustawienie {@code callSuper = true} na Car dolicza pole {@code brand} z Vehicle.
     */
    static void callSuperPitfall() {
        section("7. callSuper — pułapka przy dziedziczeniu");

        Car vwGolf = new Car("Volkswagen", 5);
        Car toyotaCorolla = new Car("Toyota", 5);
        show("ten sam liczba drzwi, inna marka: equals", vwGolf.equals(toyotaCorolla));
        // WYNIK: ten sam liczba drzwi, inna marka: equals → false

        show("samochód (callSuper=true)", vwGolf);
        // WYNIK: samochód (callSuper=true) → Lombok01Accessors.Car(super=Lombok01Accessors.Vehicle(brand=Volkswagen), doors=5)

        // PUŁAPKA: gdyby Car miał tylko „@EqualsAndHashCode” (bez callSuper=true), Lombok podczas
        //   kompilacji wypisałby OSTRZEŻENIE: „Generating equals/hashCode implementation but without a
        //   call to superclass, even though this class does not extend java.lang.Object” — i (co gorsza,
        //   gdyby zignorować ostrzeżenie) dwa samochody różnych marek z tą samą liczbą drzwi byłyby sobie
        //   „równe”, bo pole brand z Vehicle w ogóle nie brałoby udziału w porównaniu. DLACZEGO to ważne:
        //   equals ma porównywać CAŁĄ tożsamość logiczną obiektu — pominięcie pól z klasy bazowej łamie
        //   tę zasadę po cichu, bez błędu kompilacji.

        // DOBRA PRAKTYKA: w podklasie zawsze jawnie zdecyduj — callSuper = true (klasa bazowa ma
        //   znaczące pola) albo callSuper = false z komentarzem DLACZEGO (np. klasa bazowa to tylko
        //   techniczny szkielet bez własnych danych). Nigdy nie zostawiaj domyślnej wartości „w ciemno”
        //   w hierarchii klas — Lombok i tak o tym przypomni ostrzeżeniem.
    }

    // ---------------------------------------------------------------------------------------------
    // Pomocnik: posortowana lista nazw metod zadeklarowanych w klasie (dowód refleksją, patrz
    // t19_annotations_reflection/Annotations03ReflectionBasics — kolejność z getDeclaredMethods()
    // NIE jest gwarantowana, dlatego zawsze sortujemy przed wypisaniem).
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
     *   • @Getter/@Setter — na polu (jedna metoda) albo na klasie (wszystkie pola; final/static pomijane).
     *   • AccessLevel.{PUBLIC,PROTECTED,PACKAGE,PRIVATE,MODULE,NONE} — NONE = nie generuj tej metody;
     *     adnotacja na polu wygrywa z adnotacją na klasie.
     *   • boolean pole → getter isX; Boolean (opakowanie) pole → getter getX (może być null).
     *   • @ToString(exclude=..., includeFieldNames=..., callSuper=...) lub @ToString.Exclude na polu.
     *   • @EqualsAndHashCode(exclude=..., callSuper=...) lub @EqualsAndHashCode.Exclude na polu; generuje
     *     też canEqual() — chroni symetrię equals przy dziedziczeniu.
     *   • W podklasie zawsze ustaw callSuper jawnie — inaczej Lombok ostrzega, a pola bazowe znikają
     *     z porównania/tekstu po cichu.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego @Getter/@Setter na klasie nie tworzy settera dla pola final?
     *   2. Co wypisze:  Flags fl = new Flags(false, null); System.out.println(fl.getVip());  ?
     *   3. Czym różni się getter dla „boolean active” od gettera dla „Boolean vip”?
     *   4. ZNAJDŹ BŁĄD:  @Setter(AccessLevel.NONE) private String email;  — a w main ktoś woła
     *      „customer.setEmail(...)” i oczekuje, że to się skompiluje. Co się stanie i dlaczego?
     *   5. Co wypisze (przy domyślnym callSuper=false w Car):
     *      new Car("VW", 5).equals(new Car("Toyota", 5))  — gdyby Car NIE miał callSuper=true?
     *   6. Po co @EqualsAndHashCode generuje dodatkową metodę canEqual, skoro i tak sprawdzamy instanceof?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: PersonLombok przez settery", "Ewa Zielińska (41 lat)", () -> exercise1("Ewa", "Zielińska", 41));
        Check.equal("ćw. 2: czy klient jest w pełni opisany", true, () -> exercise2(new Customer(2L, "a@b.pl", "stały klient")));
        Check.equal("ćw. 3 (PRZEPISZ): z PersonHandwritten na PersonLombok", "Kamil Górski(32)", () -> exercise3("Kamil", "Górski", 32));
        Check.equal("ćw. 4: liczba getterów Flags (get*/is*)", 2, () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "Ewa Zielińska (41 lat)", () -> solution1("Ewa", "Zielińska", 41));
        Check.equal("ćw. 2 (wzorzec)", true, () -> solution2(new Customer(2L, "a@b.pl", "stały klient")));
        Check.equal("ćw. 3 (wzorzec)", "Kamil Górski(32)", () -> solution3("Kamil", "Górski", 32));
        Check.equal("ćw. 4 (wzorzec)", 2, () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zbuduj PersonLombok przez settery (firstName, lastName, age) i zwróć tekst
     * "Imię Nazwisko (wiek lat)", np. "Ewa Zielińska (41 lat)".
     * Podpowiedź: użyj setFirstName/setLastName/setAge, potem getterów do złożenia tekstu.
     */
    static String exercise1(String firstName, String lastName, int age) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć true, jeśli klient ma niepuste getEmail() I niepuste getInternalNotes().
     * Podpowiedź: String.isBlank() (Java 11+) — pokazano już w t14_optional/Optional03BestPractices.
     */
    static boolean exercise2(Customer customer) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ): poniższy kod budował opis osoby na starym, ręcznie pisanym
     * PersonHandwritten:
     * <pre>{@code
     *     PersonHandwritten h = new PersonHandwritten();
     *     h.setFirstName(firstName); h.setLastName(lastName); h.setAge(age);
     *     String opis = h.getFirstName() + " " + h.getLastName() + "(" + h.getAge() + ")";
     * }</pre>
     * Przepisz go tak, by korzystał z PersonLombok (ten sam wynik, np. "Kamil Górski(32)").
     * Podpowiedź: API PersonLombok jest identyczne jak PersonHandwritten — zmienia się tylko typ.
     */
    static String exercise3(String firstName, String lastName, int age) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): refleksją policz, ile metod zadeklarowanych w Flags.class zaczyna się
     * od "get" lub "is" (gettery). Podpowiedź: Flags.class.getDeclaredMethods(), Method::getName,
     * startsWith("get") || startsWith("is").
     */
    static int exercise4() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String firstName, String lastName, int age) {
        PersonLombok p = new PersonLombok();
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setAge(age);
        return p.getFirstName() + " " + p.getLastName() + " (" + p.getAge() + " lat)";
    }

    static boolean solution2(Customer customer) {
        return !customer.getEmail().isBlank() && !customer.getInternalNotes().isBlank();
    }

    static String solution3(String firstName, String lastName, int age) {
        PersonLombok h = new PersonLombok();
        h.setFirstName(firstName);
        h.setLastName(lastName);
        h.setAge(age);
        return h.getFirstName() + " " + h.getLastName() + "(" + h.getAge() + ")";
    }

    static int solution4() {
        return (int) Arrays.stream(Flags.class.getDeclaredMethods())
                .map(Method::getName)
                .filter(n -> n.startsWith("get") || n.startsWith("is"))
                .count();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Pole final można przypisać tylko raz (przy tworzeniu obiektu) — setter próbujący przypisać
     *      je ponownie byłby błędem kompilacji, więc Lombok go po prostu pomija.
     *   2. "null" — Boolean to opakowanie, może przechowywać null, a getter zwraca pole bez zmian.
     *   3. isActive() (primityw boolean, konwencja JavaBeans "is") kontra getVip() (Boolean — zwykły
     *      getter "get", bo pole może być null, czyli nie jest to już czysto logiczna wartość).
     *   4. Kod się NIE skompiluje — "cannot find symbol: method setEmail" — AccessLevel.NONE oznacza,
     *      że ta metoda w ogóle nie istnieje w wygenerowanym bajtkodzie, więc nie ma jej jak wywołać.
     *   5. "true" — bez callSuper=true pole brand z Vehicle nie bierze udziału w equals, więc oba
     *      samochody (ta sama liczba drzwi, inna marka) wyszłyby sobie "równe".
     *   6. canEqual chroni SYMETRIĘ equals przy dziedziczeniu: bez niej obiekt klasy bazowej mógłby
     *      wyjść "równy" obiektowi podklasy (bo instanceof w jedną stronę przechodzi), a to łamie
     *      kontrakt equals (patrz t06_oop_basics/Oop06Immutability — x.equals(y) musi dawać to samo co
     *      y.equals(x)).
     */
    // </editor-fold>
}
