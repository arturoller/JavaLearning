package t06_oop_basics;

import helpers.Check;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Kopiowanie obiektów — kopia płytka i głęboka, konstruktor kopiujący, clone()
 *        (copy = kopia; shallow copy = kopia płytka; deep copy = kopia głęboka;
 *        copy constructor = konstruktor kopiujący; clone = sklonuj)
 *
 * W SKRÓCIE:
 *   Przypisanie {@code b = a} NIE kopiuje obiektu — tworzy drugą referencję (alias) do tego samego obiektu.
 *   Prawdziwa kopia to nowy obiekt. Kopia płytka przepisuje pola (więc obiekty-pola są wspólne),
 *   kopia głęboka kopiuje też obiekty, na które wskazują pola. Najlepsze narzędzia: konstruktor kopiujący
 *   i statyczna fabryka copyOf. Metoda clone() jest zepsuta w projekcie — znasz ją, żeby ją rozumieć.
 *
 * ANALOGIA: obiekt to kartka z adresem klucza do szafki (referencja). Kserokopia kartki (kopia płytka)
 *   daje drugą kartkę z TYM SAMYM adresem — obie otwierają tę samą szafkę. Kopia głęboka to zrobienie
 *   dorobionego klucza i NOWEJ szafki z kopią zawartości: zmiany w jednej szafce nie dotyczą drugiej.
 *
 * JAK TO DZIAŁA:
 *   Oryginał                 Kopia płytka               Kopia głęboka
 *   Person ──► Address       Person ──► Address         Person ──► Address
 *                              ▲                                    Person' ──► Address' (nowy)
 *                  Person' ────┘ (ten sam Address)
 *
 *   Zasada: kopia płytka kopiuje WARTOŚCI pól. Dla typów prostych (int) to wartość, a dla referencji
 *   to ADRES — czyli obiekt wskazywany przez pole pozostaje wspólny.
 *   Typy niezmienne (String, Integer, LocalDate, rekord z niezmiennymi polami) można dzielić bezpiecznie —
 *   nikt ich nie zmieni, więc nie trzeba ich kopiować. Kopiować trzeba tylko obiekty ZMIENNE.
 *
 *   Sposoby kopiowania:  1. konstruktor kopiujący / fabryka copyOf (zalecane),
 *                        2. clone() + Cloneable (przestarzały styl; wyjątek: tablice),
 *                        3. metody "with" na typach niezmiennych (rekordy),
 *                        4. serializacja (tylko wzmianka — t18_io_files/Io09Serialization).
 *
 * SŁÓWKA: copy = kopia; shallow = płytki; deep = głęboki; alias = drugie imię (druga referencja);
 *   clone = sklonuj; Cloneable = "klonowalny" (interfejs znacznikowy); copy constructor = konstruktor kopiujący;
 *   factory = fabryka; view = widok; defensive copy = kopia obronna; wither = metoda "with" (zwraca zmienioną kopię).
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop05ObjectMethods (equals/hashCode — równość po kopiowaniu),
 *   t06_oop_basics/Oop06Immutability (kopie obronne), t12_collections/Collections08ImmutableUnmodifiable (widok i kopia),
 *   t18_io_files/Io09Serialization (głęboka kopia przez serializację)
 * </pre>
 */
public class Oop10Copying {

    public static void main(String[] args) {
        title("Oop10 — kopiowanie obiektów");

        aliasVsCopy();            // alias vs copy = alias a kopia
        shallowVsDeep();          // shallow vs deep = płytka a głęboka
        copyConstructorAndFactory(); // copy constructor and factory = konstruktor kopiujący i fabryka
        copyAndInheritance();     // copy and inheritance = kopiowanie a dziedziczenie
        arraysCopy();             // arrays copy = kopiowanie tablic
        collectionsCopy();        // collections copy = kopiowanie kolekcji
        cloneBasics();            // clone basics = podstawy clone()
        cloneIsBroken();          // clone is broken = clone() jest zepsute
        recordsAndWithers();      // records and withers = rekordy i metody "with"
        identityVsEquality();     // identity vs equality = tożsamość a równość
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // Klasy pomocnicze (w prawdziwym projekcie — każda w osobnym pliku)
    // =================================================================================================

    /** Punkt — prosta klasa zmienna (mutable): pola można zmieniać. */
    static class Point {
        int x;
        int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "Point(" + x + ", " + y + ")";
        }
    }

    /** Adres — klasa zmienna. To ona jest "obiektem w polu", który sprawia kłopoty przy kopiowaniu. */
    static class Address {
        String city;   // city = miasto
        String street; // street = ulica

        Address(String city, String street) {
            this.city = city;
            this.street = street;
        }

        /** Konstruktor kopiujący: wszystkie pola to String (niezmienny), więc wystarczy je przepisać. */
        Address(Address other) {
            this(other.city, other.street);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Address other = (Address) o;
            return Objects.equals(city, other.city) && Objects.equals(street, other.street);
        }

        @Override
        public int hashCode() {
            return Objects.hash(city, street);
        }

        @Override
        public String toString() {
            return city + ", " + street;
        }
    }

    /** Osoba: imię (String — niezmienny) i adres (obiekt ZMIENNY). */
    static class Person {
        final String name;
        Address address;

        Person(String name, Address address) {
            this.name = name;
            this.address = address;
        }

        /** Konstruktor kopiujący GŁĘBOKI: adres kopiujemy, imię można dzielić (String jest niezmienny). */
        Person(Person other) {
            this(other.name, new Address(other.address));
        }

        /** Statyczna fabryka — to samo co konstruktor kopiujący, ale z czytelną nazwą. */
        static Person copyOf(Person other) {
            return new Person(other);
        }

        /** Kopia PŁYTKA: ten sam obiekt Address w obu osobach. Pokazujemy ją tylko jako antywzorzec. */
        static Person shallowCopyOf(Person other) {
            return new Person(other.name, other.address);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person other = (Person) o;
            return name.equals(other.name) && Objects.equals(address, other.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, address);
        }

        @Override
        public String toString() {
            return name + " (" + address + ")";
        }
    }

    // =================================================================================================
    // 1. ALIAS A KOPIA
    // =================================================================================================

    /** Pomocnicza metoda: zmienia obiekt przekazany przez referencję (zmiana widoczna u wołającego). */
    static void moveRight(Point p) {
        p.x += 10; // p to KOPIA REFERENCJI, ale wskazuje ten sam obiekt
    }

    /**
     * 1. Przypisanie referencji nie kopiuje obiektu. Typy proste (int) kopiują się przy przypisaniu
     * naprawdę, ale zmienna obiektowa trzyma tylko adres.
     */
    static void aliasVsCopy() {
        section("1. Alias (druga referencja) a prawdziwa kopia");

        int n = 5;
        int m = n; // typ prosty: kopiuje się WARTOŚĆ
        m++;
        show("n po zmianie m", n);
        // WYNIK: n po zmianie m → 5

        Point a = new Point(1, 2);
        Point alias = a;                    // alias = drugie imię: TA SAMA kartka z adresem
        Point copy = new Point(a.x, a.y);   // copy = kopia: NOWY obiekt o tych samych wartościach
        alias.x = 100;

        show("a po zmianie przez alias", a);
        // WYNIK: a po zmianie przez alias → Point(100, 2)
        show("copy po zmianie aliasu", copy);
        // WYNIK: copy po zmianie aliasu → Point(1, 2)
        show("a == alias (ten sam obiekt?)", a == alias);
        // WYNIK: a == alias (ten sam obiekt?) → true
        show("a == copy (ten sam obiekt?)", a == copy);
        // WYNIK: a == copy (ten sam obiekt?) → false

        moveRight(copy); // moveRight = przesuń w prawo
        show("copy po moveRight", copy);
        // WYNIK: copy po moveRight → Point(11, 2)

        // PUŁAPKA: metoda dostaje kopię REFERENCJI, nie kopię obiektu. Zmieni obiekt wołającego — to bywa
        // zaskoczeniem. Dlaczego: Java zawsze przekazuje argumenty przez wartość, a wartością zmiennej
        // obiektowej jest adres (zobacz t05_methods/Methods01Basics).

        // DOBRA PRAKTYKA: gdy metoda ma zmieniać obiekt "u siebie" — najpierw zrób kopię i zmieniaj kopię.
        // Dlaczego: wołający nie traci danych, a kod jest przewidywalny.
    }

    // =================================================================================================
    // 2. KOPIA PŁYTKA A GŁĘBOKA
    // =================================================================================================

    /**
     * 2. Ta sama osoba skopiowana płytko i głęboko. Zmieniamy adres w kopii i patrzymy na oryginał.
     */
    static void shallowVsDeep() {
        section("2. Kopia płytka a kopia głęboka");

        Person original = new Person("Ala", new Address("Warszawa", "Długa"));

        Person shallow = Person.shallowCopyOf(original); // shallowCopyOf = kopia płytka z
        shallow.address.city = "Kraków";                 // zmieniamy WSPÓLNY obiekt Address
        show("oryginał po zmianie kopii płytkiej", original);
        // WYNIK: oryginał po zmianie kopii płytkiej → Ala (Kraków, Długa)
        show("kopia płytka", shallow);
        // WYNIK: kopia płytka → Ala (Kraków, Długa)

        // Przywracamy oryginał i robimy kopię GŁĘBOKĄ.
        original.address.city = "Warszawa";
        Person deep = Person.copyOf(original); // deep = głęboka; copyOf = kopia z
        deep.address.city = "Gdańsk";
        show("oryginał po zmianie kopii głębokiej", original);
        // WYNIK: oryginał po zmianie kopii głębokiej → Ala (Warszawa, Długa)
        show("kopia głęboka", deep);
        // WYNIK: kopia głęboka → Ala (Gdańsk, Długa)

        // Ważny niuans: PODMIANA referencji w kopii płytkiej nie rusza oryginału. Wspólny jest tylko obiekt
        // w środku — dopóki go zmieniasz. Gdy wskażesz nowy obiekt, kopia przestaje być powiązana.
        Person shallow2 = Person.shallowCopyOf(original);
        shallow2.address = new Address("Łódź", "Piotrkowska"); // nowa referencja w kopii
        show("oryginał po podmianie referencji w kopii", original);
        // WYNIK: oryginał po podmianie referencji w kopii → Ala (Warszawa, Długa)

        // PUŁAPKA: kopia płytka wygląda na kopię (inny obiekt, te same dane), a po cichu współdzieli
        // obiekty zmienne. Błąd ujawnia się dopiero przy zmianie — często daleko od miejsca kopiowania.
        // Dlaczego: pole przechowuje referencję, a kopiowanie pola przepisuje referencję, nie obiekt.

        // DOBRA PRAKTYKA: dla każdego pola zapytaj: "czy ten typ jest niezmienny?". Jeśli tak (String,
        // Integer, LocalDate) — dziel referencję. Jeśli zmienny (Address, lista, tablica) — skopiuj go.
        // Dlaczego: kopia ma być niezależna dokładnie tam, gdzie zmiana jest możliwa.

        // Poziomy głębokości: kopia głęboka kopiuje całe drzewo obiektów zmiennych. Jeśli Address
        // miałby pole typu zmiennego, jego konstruktor kopiujący też musiałby je skopiować (rekurencja).
    }

    // =================================================================================================
    // 3. KONSTRUKTOR KOPIUJĄCY I STATYCZNA FABRYKA
    // =================================================================================================

    /** Koszyk z listą produktów: pokazuje konstruktor kopiujący z polem typu kolekcja. */
    static class Basket {
        final String owner;                   // owner = właściciel
        final List<String> items = new ArrayList<>(); // items = elementy (ArrayList = lista, t12_collections)

        Basket(String owner) {
            this.owner = Objects.requireNonNull(owner); // requireNonNull = wymagaj, żeby nie było null
        }

        /** Konstruktor kopiujący: pola final są zwykłymi polami, więc działa też z final. */
        Basket(Basket other) {
            this(other.owner);           // przechodzi przez walidację konstruktora głównego
            this.items.addAll(other.items); // addAll = dodaj wszystkie; teksty są niezmienne, więc kopia wystarczy
        }

        /** Statyczna fabryka — nazwa mówi, co się dzieje, i można zwrócić np. podtyp. */
        static Basket copyOf(Basket other) {
            return new Basket(other);
        }

        @Override
        public String toString() {
            return owner + items;
        }
    }

    /**
     * 3. Konstruktor kopiujący (przyjmuje obiekt własnego typu) i statyczna fabryka copyOf to preferowany
     * sposób kopiowania: używa zwykłego konstruktora, więc działa z polami final i walidacją.
     */
    static void copyConstructorAndFactory() {
        section("3. Konstruktor kopiujący i fabryka copyOf");

        Basket original = new Basket("Ola");
        original.items.add("chleb");
        original.items.add("masło");

        Basket viaConstructor = new Basket(original); // konstruktor kopiujący
        Basket viaFactory = Basket.copyOf(original);  // fabryka
        viaConstructor.items.add("ser");
        viaFactory.items.clear();

        show("oryginał", original);
        // WYNIK: oryginał → Ola[chleb, masło]
        show("kopia z konstruktora", viaConstructor);
        // WYNIK: kopia z konstruktora → Ola[chleb, masło, ser]
        show("kopia z fabryki", viaFactory);
        // WYNIK: kopia z fabryki → Ola[]

        // Konstruktor kopiujący przechodzi przez walidację zwykłego konstruktora (requireNonNull).
        // Dlatego nie da się przez niego utworzyć obiektu o niedozwolonym stanie.
        note("konstruktor kopiujący = zwykły konstruktor → pola final i walidacja działają");
        // WYNIK: ℹ konstruktor kopiujący = zwykły konstruktor → pola final i walidacja działają

        // DOBRA PRAKTYKA: dla własnych klas zmiennych pisz konstruktor kopiujący albo fabrykę copyOf.
        // Dlaczego: jest prosty, jawny, działa z final, nie rzuca wyjątków sprawdzanych i nie wymaga
        // żadnego interfejsu — w przeciwieństwie do clone().

        // PUŁAPKA: łatwo zapomnieć o nowym polu. Dodajesz pole do klasy, a konstruktor kopiujący go nie
        // przepisuje — kopia ma wartość domyślną (null/0). Dlaczego: kompilator nie sprawdza kompletności
        // kopii. Pomaga test: skopiuj, porównaj equals (Oop05ObjectMethods) i zmień kopię.
    }

    // =================================================================================================
    // 4. KOPIOWANIE A DZIEDZICZENIE
    // =================================================================================================

    static class Animal {
        String name; // name = imię

        Animal(String name) {
            this.name = name;
        }

        Animal(Animal other) {
            this.name = other.name;
        }

        /** Wirtualna "kopia" — każda podklasa nadpisuje ją i zwraca własny typ. */
        Animal copy() {
            return new Animal(this); // copy = kopia
        }

        String describe() { // describe = opisz
            return getClass().getSimpleName() + " " + name; // getSimpleName = pobierz prostą nazwę
        }
    }

    static class Dog extends Animal {
        int tricks; // tricks = sztuczki

        Dog(String name, int tricks) {
            super(name);
            this.tricks = tricks;
        }

        Dog(Dog other) {
            super(other); // kopiujemy część odziedziczoną
            this.tricks = other.tricks;
        }

        @Override
        Dog copy() { // zwrot węższego typu (Dog zamiast Animal) = zwracany typ kowariantny (covariant return)
            return new Dog(this);
        }

        @Override
        String describe() {
            return super.describe() + " (sztuczki: " + tricks + ")";
        }
    }

    /**
     * 4. Konstruktor kopiujący zna tylko typ ze zmiennej. Gdy zmienna ma typ nadrzędny, a obiekt jest
     * podtypem, "kopia" traci podtyp. Rozwiązanie: wirtualna metoda copy() nadpisywana w podklasach.
     */
    static void copyAndInheritance() {
        section("4. Kopiowanie a dziedziczenie (podtyp ginie)");

        Animal pet = new Dog("Burek", 3); // typ zmiennej: Animal, obiekt: Dog

        Animal wrong = new Animal(pet);   // wybiera się konstruktor Animal(Animal) — wiemy tylko, że to zwierzę
        show("kopia przez new Animal(pet)", wrong.describe());
        // WYNIK: kopia przez new Animal(pet) → Animal Burek

        Animal right = pet.copy();        // wirtualne wywołanie: wykona się Dog.copy()
        show("kopia przez pet.copy()", right.describe());
        // WYNIK: kopia przez pet.copy() → Dog Burek (sztuczki: 3)

        Dog dog = new Dog("Reks", 1);
        Dog dogCopy = dog.copy(); // dzięki typowi kowariantnemu NIE trzeba rzutowania (Dog, nie Animal)
        dogCopy.tricks = 7;
        show("oryginalny pies", dog.describe());
        // WYNIK: oryginalny pies → Dog Reks (sztuczki: 1)
        show("kopia psa", dogCopy.describe());
        // WYNIK: kopia psa → Dog Reks (sztuczki: 7)

        // PUŁAPKA: new Animal(zmiennaTypuAnimal) zawsze tworzy czyste Animal, nawet gdy w środku jest
        // Dog. Dlaczego: konstruktor nie jest wirtualny — wybiera go kompilator na podstawie typu zmiennej.

        // DOBRA PRAKTYKA: w hierarchii klas daj klasie bazowej wirtualną metodę copy() i nadpisz ją
        // w każdej podklasie (z węższym typem zwracanym). Dlaczego: wtedy kopia zachowuje prawdziwy typ obiektu.
        // Pamiętaj: jeśli podklasa zapomni nadpisać copy(), kopia będzie typu nadrzędnego — to ten sam problem,
        // który ma clone(). Prostsze rozwiązanie: klasy końcowe (final) albo rekordy zamiast hierarchii.
    }

    // =================================================================================================
    // 5. KOPIOWANIE TABLIC
    // =================================================================================================

    /**
     * 5. Tablice są obiektami. clone() na tablicy jest publiczne i działa dobrze (kopia płytka), bez
     * rzutowania. Dla tablic typów prostych płytka kopia jest równocześnie pełna.
     */
    static void arraysCopy() {
        section("5. Kopiowanie tablic");

        int[] numbers = {1, 2, 3};
        int[] alias = numbers;
        int[] cloned = numbers.clone();             // clone = sklonuj; zwraca int[] (bez rzutowania)
        int[] copied = Arrays.copyOf(numbers, 5);   // copyOf = kopia z; nowa długość 5 (dopełnia zerami)
        alias[0] = 99;
        show("numbers (po zmianie przez alias)", Arrays.toString(numbers)); // toString = na tekst
        // WYNIK: numbers (po zmianie przez alias) → [99, 2, 3]
        show("cloned (niezależna kopia)", Arrays.toString(cloned));
        // WYNIK: cloned (niezależna kopia) → [1, 2, 3]
        show("copied (copyOf, dłuższa)", Arrays.toString(copied));
        // WYNIK: copied (copyOf, dłuższa) → [1, 2, 3, 0, 0]

        // Tablica obiektów: clone i copyOf kopiują PŁYTKO — kopiują referencje, nie same obiekty.
        StringBuilder[] builders = {new StringBuilder("a"), new StringBuilder("b")};
        StringBuilder[] builderClone = builders.clone();
        builderClone[0].append("!");            // append = dopisz; zmieniamy wspólny obiekt
        builderClone[1] = new StringBuilder("z"); // podmiana referencji w kopii — oryginał nietknięty
        show("builders (oryginał)", Arrays.toString(builders));
        // WYNIK: builders (oryginał) → [a!, b]
        show("builderClone", Arrays.toString(builderClone));
        // WYNIK: builderClone → [a!, z]

        // Tablica dwuwymiarowa to tablica tablic: clone kopiuje tylko zewnętrzną tablicę.
        int[][] matrix = {{1, 2}, {3, 4}};
        int[][] shallowMatrix = matrix.clone();
        shallowMatrix[0][0] = 100;                 // wiersz [0] jest wspólny!
        show("matrix po zmianie kopii płytkiej", Arrays.deepToString(matrix)); // deepToString = na tekst (głęboko)
        // WYNIK: matrix po zmianie kopii płytkiej → [[100, 2], [3, 4]]

        // Kopia głęboka 2D — kopiujemy każdy wiersz.
        int[][] matrix2 = {{1, 2}, {3, 4}};
        int[][] deepMatrix = new int[matrix2.length][];
        for (int i = 0; i < matrix2.length; i++) {
            deepMatrix[i] = matrix2[i].clone(); // każdy wiersz to int[] — jego clone() jest pełną kopią
        }
        deepMatrix[0][0] = 100;
        show("matrix2 po zmianie kopii głębokiej", Arrays.deepToString(matrix2));
        // WYNIK: matrix2 po zmianie kopii głębokiej → [[1, 2], [3, 4]]
        show("deepMatrix", Arrays.deepToString(deepMatrix));
        // WYNIK: deepMatrix → [[100, 2], [3, 4]]

        // Wariant ze strumieniem (t16_streams): Arrays.stream(matrix2).map(int[]::clone).toArray(int[][]::new)

        // PUŁAPKA: Arrays.copyOf, Arrays.copyOfRange, System.arraycopy i clone() są płytkie. Nie ma
        // gotowej metody "Arrays.deepCopy". Dlaczego: biblioteka nie wie, jak kopiować Twoje obiekty
        // (równie dobrze mogą być niekopiowalne) — kopię głęboką piszesz sam pętlą.

        // DOBRA PRAKTYKA: kopię tablicy typów prostych rób przez clone() albo Arrays.copyOf; tablice 2D
        // i tablice obiektów zmiennych kopiuj pętlą. Dlaczego: unikasz współdzielonych wierszy i obiektów.
    }

    // =================================================================================================
    // 6. KOPIOWANIE KOLEKCJI
    // =================================================================================================

    /**
     * 6. Kolekcję kopiujesz konstruktorem {@code new ArrayList<>(x)} (zmienna kopia płytka) albo
     * {@code List.copyOf(x)} (niezmienna kopia płytka). Widok z {@code Collections.unmodifiableList}
     * to NIE kopia — pokazuje to, co dzieje się w oryginale.
     */
    static void collectionsCopy() {
        section("6. Kopiowanie kolekcji: kopia kontra widok");

        List<String> source = new ArrayList<>(List.of("a", "b")); // source = źródło; List.of = lista z (Java 9+)
        List<String> mutableCopy = new ArrayList<>(source);       // zmienna kopia płytka
        List<String> immutableCopy = List.copyOf(source);         // List.copyOf = lista-kopia (Java 10+), niezmienna
        List<String> view = Collections.unmodifiableList(source); // unmodifiableList = lista tylko do odczytu (WIDOK)
        source.add("c");

        show("source", source);
        // WYNIK: source → [a, b, c]
        show("mutableCopy (new ArrayList<>(source))", mutableCopy);
        // WYNIK: mutableCopy (new ArrayList<>(source)) → [a, b]
        show("immutableCopy (List.copyOf)", immutableCopy);
        // WYNIK: immutableCopy (List.copyOf) → [a, b]
        show("view (unmodifiableList)", view);
        // WYNIK: view (unmodifiableList) → [a, b, c]    ← widok "widzi" zmianę źródła

        expectThrows("dodanie do widoku", () -> view.add("x"));
        // WYNIK: ✔ dodanie do widoku → rzucono UnsupportedOperationException: (brak komunikatu)
        expectThrows("dodanie do List.copyOf", () -> immutableCopy.add("x"));
        // WYNIK: ✔ dodanie do List.copyOf → rzucono UnsupportedOperationException: (brak komunikatu)

        // Kopia kolekcji jest płytka: elementy są wspólne. Dla elementów zmiennych — kopia głęboka.
        List<Address> addresses = new ArrayList<>();
        addresses.add(new Address("Poznań", "Półwiejska"));
        List<Address> shallowList = new ArrayList<>(addresses);
        shallowList.get(0).city = "Szczecin";      // get = pobierz; zmieniamy wspólny element
        show("addresses po zmianie przez kopię płytką", addresses);
        // WYNIK: addresses po zmianie przez kopię płytką → [Szczecin, Półwiejska]

        List<Address> deepList = new ArrayList<>();
        for (Address a : addresses) {
            deepList.add(new Address(a)); // kopiujemy każdy element
        }
        deepList.get(0).city = "Lublin";
        show("addresses po zmianie przez kopię głęboką", addresses);
        // WYNIK: addresses po zmianie przez kopię głęboką → [Szczecin, Półwiejska]
        show("deepList", deepList);
        // WYNIK: deepList → [Lublin, Półwiejska]

        // Zagnieżdżona kolekcja: lista list. Kopia zewnętrzna dzieli wewnętrzne listy.
        List<List<Integer>> grid = new ArrayList<>(); // grid = siatka
        grid.add(new ArrayList<>(List.of(1, 2)));
        grid.add(new ArrayList<>(List.of(3)));

        List<List<Integer>> shallowGrid = new ArrayList<>(grid);
        shallowGrid.get(0).add(99);                   // wewnętrzna lista wspólna
        show("grid po zmianie kopii płytkiej", grid);
        // WYNIK: grid po zmianie kopii płytkiej → [[1, 2, 99], [3]]

        List<List<Integer>> deepGrid = new ArrayList<>();
        for (List<Integer> row : grid) {
            deepGrid.add(new ArrayList<>(row));       // kopia każdej wewnętrznej listy (Integer jest niezmienny)
        }
        deepGrid.get(1).add(100);
        show("grid po zmianie kopii głębokiej", grid);
        // WYNIK: grid po zmianie kopii głębokiej → [[1, 2, 99], [3]]
        show("deepGrid", deepGrid);
        // WYNIK: deepGrid → [[1, 2, 99], [3, 100]]

        // PUŁAPKA: Collections.unmodifiableList(x) tylko ZAMYKA DOSTĘP do zapisu przez ten widok. Kto ma
        // oryginalną listę, nadal ją zmienia — a widok to odzwierciedla. Dlaczego: widok trzyma referencję
        // do oryginału, nic nie kopiuje. Przy polu klasy zwracanym przez getter to niebezpieczna "niezmienność".
        // Także List.copyOf(x) wyrzuci NullPointerException, gdy lista zawiera null.

        // DOBRA PRAKTYKA: jeśli klasa ma być niezależna od listy przekazanej z zewnątrz — zrób kopię
        // (List.copyOf lub new ArrayList<>(x)) w konstruktorze, a getter zwracaj jako widok lub kopię.
        // Dlaczego: to kopia obronna (Oop06Immutability); sam widok nie chroni przed zmianami u źródła.
        // Więcej o różnicy: t12_collections/Collections08ImmutableUnmodifiable.
    }

    // =================================================================================================
    // 7. clone() — PODSTAWY
    // =================================================================================================

    /** Klasa klonowalna "po bożemu": implementuje Cloneable i nadpisuje clone() jako public. */
    static class CloneBasket implements Cloneable { // Cloneable = można klonować (interfejs znacznikowy, bez metod)
        static int constructorCalls = 0; // licznik wywołań konstruktora (constructorCalls = wywołania konstruktora)

        String owner;
        List<String> items = new ArrayList<>();

        CloneBasket(String owner) {
            this.owner = owner;
            constructorCalls++;
        }

        /** Nadpisanie clone(): public (w Object jest protected) i zwracany typ kowariantny: CloneBasket. */
        @Override
        public CloneBasket clone() {
            try {
                return (CloneBasket) super.clone(); // super.clone() = kopiowanie pole po polu, BEZ konstruktora
            } catch (CloneNotSupportedException e) {
                // Nie zdarzy się: klasa implementuje Cloneable. Dlatego jest to błąd programisty (AssertionError).
                throw new AssertionError(e); // AssertionError = błąd założenia
            }
        }
    }

    /** Wersja z poprawną kopią głęboką pola items. */
    static class DeepCloneBasket implements Cloneable {
        String owner;
        List<String> items = new ArrayList<>();

        DeepCloneBasket(String owner) {
            this.owner = owner;
        }

        @Override
        public DeepCloneBasket clone() {
            try {
                DeepCloneBasket copy = (DeepCloneBasket) super.clone();
                copy.items = new ArrayList<>(items); // ręcznie dokopiowujemy pole zmienne
                return copy;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }

    /** Klasa BEZ Cloneable, ale z wywołaniem super.clone() — dla pokazania wyjątku. */
    static class NotCloneable {
        Object tryClone() throws CloneNotSupportedException { // tryClone = spróbuj sklonować
            return super.clone(); // Object.clone() sprawdza, czy klasa implementuje Cloneable
        }
    }

    /**
     * 7. Jak działa Object.clone(): tworzy NOWY obiekt tej samej klasy (bez wywołania konstruktora!)
     * i przepisuje pola jeden po drugim (kopia płytka). Wymaga interfejsu Cloneable, inaczej
     * rzuca CloneNotSupportedException (wyjątek sprawdzany).
     */
    static void cloneBasics() {
        section("7. clone() i Cloneable — jak to działa");

        CloneBasket first = new CloneBasket("Ola");
        first.items.add("jabłko");
        show("wywołania konstruktora po utworzeniu oryginału", CloneBasket.constructorCalls);
        // WYNIK: wywołania konstruktora po utworzeniu oryginału → 1

        CloneBasket second = first.clone(); // bez rzutowania — typ kowariantny
        show("wywołania konstruktora po clone()", CloneBasket.constructorCalls);
        // WYNIK: wywołania konstruktora po clone() → 1    ← clone() NIE wywołuje konstruktora
        show("first == second", first == second);
        // WYNIK: first == second → false
        show("ta sama klasa", first.getClass() == second.getClass());
        // WYNIK: ta sama klasa → true

        // Pola zostały przepisane płytko: lista items jest WSPÓLNA.
        second.items.add("gruszka");
        show("first.items po dodaniu do kopii", first.items);
        // WYNIK: first.items po dodaniu do kopii → [jabłko, gruszka]
        show("lista wspólna (==)", first.items == second.items);
        // WYNIK: lista wspólna (==) → true

        // Poprawna wersja: clone() ręcznie kopiuje pola zmienne.
        DeepCloneBasket d1 = new DeepCloneBasket("Ala");
        d1.items.add("mleko");
        DeepCloneBasket d2 = d1.clone();
        d2.items.add("kawa");
        show("d1.items", d1.items);
        // WYNIK: d1.items → [mleko]
        show("d2.items", d2.items);
        // WYNIK: d2.items → [mleko, kawa]

        // Brak Cloneable → wyjątek sprawdzany CloneNotSupportedException (komunikat to nazwa klasy).
        expectThrows("clone() bez Cloneable", () -> new NotCloneable().tryClone());
        // WYNIK: ✔ clone() bez Cloneable → rzucono CloneNotSupportedException: t06_oop_basics.Oop10Copying$NotCloneable

        // Metoda Object.clone() jest protected: z zewnątrz klasy nie wywołasz new Object().clone().
        // Dlatego klasa musi sama ją nadpisać jako public — i dlatego Cloneable "nic nie mówi" o metodzie.

        // PUŁAPKA: Cloneable jest interfejsem BEZ metod — to tylko znacznik dla Object.clone(). Interfejs
        // nie wymusza public clone(), więc kod ogólny (np. na zmiennej typu Cloneable) nie może jej wywołać.
        // Dlaczego: to wada projektu z pierwszej wersji Javy (1996), której nie da się już naprawić bez łamania zgodności.
    }

    // =================================================================================================
    // 8. DLACZEGO clone() JEST UWAŻANE ZA ZEPSUTE
    // =================================================================================================

    /** Bilet z unikalnym numerem nadawanym w konstruktorze — clone() omija konstruktor, więc kopiuje numer. */
    static class Ticket implements Cloneable { // Ticket = bilet
        private static int nextId = 1;  // nextId = następny numer
        private int id;                 // id = numer

        Ticket() {
            this.id = nextId++;
        }

        /** Konstruktor kopiujący nadaje NOWY numer: kopia to nowy bilet. */
        Ticket(Ticket other) {
            this();
        }

        @Override
        public Ticket clone() {
            try {
                return (Ticket) super.clone(); // numer przepisany — dwa bilety z tym samym numerem
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }

        int id() {
            return id;
        }
    }

    /** Księga z polem final: super.clone() przepisuje referencję, a final nie pozwala jej potem podmienić. */
    static class Ledger implements Cloneable { // Ledger = księga rachunkowa
        final List<String> entries = new ArrayList<>(); // entries = wpisy

        /** Konstruktor kopiujący — z polem final nie ma problemu. */
        Ledger() {
        }

        Ledger(Ledger other) {
            this.entries.addAll(other.entries);
        }

        @Override
        public Ledger clone() {
            try {
                // Chcielibyśmy: copy.entries = new ArrayList<>(entries);  — błąd kompilacji: pola final
                // nie można ponownie przypisać. Zostaje płytka kopia ze wspólną listą.
                return (Ledger) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }

    /**
     * 8. Książka Joshuy Blocha „Effective Java” (po polsku „Java. Efektywne programowanie”, punkt 13) ostrzega przed clone(). Powody: (1) omija konstruktor
     * i jego walidację/niezmienniki, (2) kłóci się z polami final, (3) wyjątek sprawdzany, który
     * nigdy nie wystąpi, (4) Cloneable nie ma metod, (5) kopia głęboka jest ręczna i krucha w podklasach.
     */
    static void cloneIsBroken() {
        section("8. Dlaczego clone() jest uważane za zepsute");

        Ticket t1 = new Ticket();
        Ticket t2 = t1.clone();
        Ticket t3 = new Ticket(t1);
        show("numer t1", t1.id());
        // WYNIK: numer t1 → 1
        show("numer t2 (clone)", t2.id());
        // WYNIK: numer t2 (clone) → 1    ← duplikat! konstruktor nie zadziałał
        show("numer t3 (konstruktor kopiujący)", t3.id());
        // WYNIK: numer t3 (konstruktor kopiujący) → 2

        Ledger ledger = new Ledger();
        ledger.entries.add("wpis 1");
        Ledger cloned = ledger.clone();
        Ledger copied = new Ledger(ledger);
        cloned.entries.add("wpis 2");
        show("księga po clone() i zmianie klona", ledger.entries);
        // WYNIK: księga po clone() i zmianie klona → [wpis 1, wpis 2]    ← wspólna lista
        copied.entries.add("wpis 3");
        show("księga po zmianie konstruktora kopiującego", ledger.entries);
        // WYNIK: księga po zmianie konstruktora kopiującego → [wpis 1, wpis 2]

        // PUŁAPKA: usunięcie final tylko po to, by clone() działało, osłabia klasę — tracisz gwarancję
        // niezmienności pola. Dlaczego: to konstruktor miał ją zapewniać, a clone() go pomija.

        // PUŁAPKA: w podklasach — jeśli klasa bazowa używa super.clone(), a podklasa dodaje pole zmienne i nie
        // nadpisze clone(), kopia dzieli to pole z oryginałem. Gdy klasa bazowa zbuduje klon przez new
        // zamiast super.clone(), podklasa dostaje obiekt złego typu. Kontrakt clone() łatwo złamać.

        // DOBRA PRAKTYKA: nie dodawaj Cloneable do nowych klas. Używaj konstruktora kopiującego lub fabryki
        // copyOf. Wyjątek: tablice (ich clone() jest bezpieczne) i klasy z istniejącym API opartym na clone().
        // Dlaczego: prostszy kod, brak ukrytych pułapek, działa z final i walidacją.
    }

    // =================================================================================================
    // 9. REKORDY I METODY "WITH"
    // =================================================================================================

    /** Faktura jako rekord (Java 16+): niezmienny, z kopią obronną listy w konstruktorze kompaktowym. */
    record Invoice(String number, List<String> items) { // record = rekord; number = numer; items = pozycje
        Invoice { // konstruktor kompaktowy (compact constructor) — bez listy parametrów
            items = List.copyOf(items); // kopia obronna: lista nie zależy od tej, którą przekazał wołający
        }

        /** Metoda "with": nie zmienia faktury, tylko zwraca NOWĄ z dodaną pozycją. */
        Invoice withItem(String item) {
            List<String> more = new ArrayList<>(items);
            more.add(item);
            return new Invoice(number, more);
        }

        Invoice withNumber(String newNumber) { // withNumber = z numerem
            return new Invoice(newNumber, items); // items niezmienne → bezpiecznie dzielimy
        }
    }

    /** Rekord z polem ZMIENNEGO typu: pole jest final, ale obiekt w środku nie. */
    record Place(String name, Address address) { // Place = miejsce
    }

    /**
     * 9. Rekord jest niezmienny płytko: jego pola są final. Jeśli wszystkie komponenty są niezmienne,
     * kopiować nie trzeba — zamiast zmiany robisz nowy rekord (metoda "with"). Gdy komponent jest zmienny,
     * trzeba go skopiować w konstruktorze kompaktowym.
     */
    static void recordsAndWithers() {
        section("9. Rekordy i metody \"with\" zamiast kopiowania");

        List<String> source = new ArrayList<>(List.of("kawa", "herbata"));
        Invoice invoice = new Invoice("FV-1", source);
        source.add("ciastko"); // zmieniamy listę źródłową PO utworzeniu rekordu
        show("faktura po zmianie listy źródłowej", invoice);
        // WYNIK: faktura po zmianie listy źródłowej → Invoice[number=FV-1, items=[kawa, herbata]]

        expectThrows("zmiana listy w rekordzie", () -> invoice.items().add("x"));
        // WYNIK: ✔ zmiana listy w rekordzie → rzucono UnsupportedOperationException: (brak komunikatu)

        Invoice bigger = invoice.withItem("sernik");
        Invoice renumbered = invoice.withNumber("FV-2");
        show("oryginał", invoice);
        // WYNIK: oryginał → Invoice[number=FV-1, items=[kawa, herbata]]
        show("withItem", bigger);
        // WYNIK: withItem → Invoice[number=FV-1, items=[kawa, herbata, sernik]]
        show("withNumber", renumbered);
        // WYNIK: withNumber → Invoice[number=FV-2, items=[kawa, herbata]]

        // Rekord z komponentem zmiennym — pozornie niezmienny.
        Address address = new Address("Opole", "Rynek");
        Place place = new Place("Biuro", address);
        address.city = "Rzeszów";
        show("rekord Place po zmianie adresu", place);
        // WYNIK: rekord Place po zmianie adresu → Place[name=Biuro, address=Rzeszów, Rynek]

        // PUŁAPKA: final na polu rekordu chroni TYLKO referencję, nie zawartość obiektu. Rekord ze zmiennym
        // komponentem (tablica, lista, własna klasa zmienna) można zmienić od środka. Dlaczego: rekord
        // nie kopiuje komponentów automatycznie — robisz to w konstruktorze kompaktowym.

        // DOBRA PRAKTYKA: komponenty rekordu niech będą niezmienne (String, liczby, inne rekordy,
        // List.copyOf). Dlaczego: wtedy kopia jest zbędna — dzielisz obiekt bez ryzyka, a "zmianę"
        // robisz przez nową instancję (metody withX). Zobacz też Oop06Immutability.
    }

    // =================================================================================================
    // 10. TOŻSAMOŚĆ A RÓWNOŚĆ PO SKOPIOWANIU I WYBÓR METODY
    // =================================================================================================

    /**
     * 10. Po kopii głębokiej: {@code ==} daje false (inny obiekt), a {@code equals} true (te same dane),
     * jeśli klasa ma poprawne equals/hashCode (Oop05ObjectMethods). Po kopii płytkiej obiekty wewnątrz
     * są nawet tożsame ({@code ==}).
     */
    static void identityVsEquality() {
        section("10. Tożsamość (==) a równość (equals) po kopiowaniu");

        Person original = new Person("Ala", new Address("Warszawa", "Długa"));
        Person deep = Person.copyOf(original);
        Person shallow = Person.shallowCopyOf(original);

        show("original == deep", original == deep);
        // WYNIK: original == deep → false
        show("original.equals(deep)", original.equals(deep));
        // WYNIK: original.equals(deep) → true
        show("hashCode równe (original, deep)", original.hashCode() == deep.hashCode());
        // WYNIK: hashCode równe (original, deep) → true
        show("adres: original.address == deep.address", original.address == deep.address);
        // WYNIK: adres: original.address == deep.address → false
        show("adres: original.address.equals(deep.address)", original.address.equals(deep.address));
        // WYNIK: adres: original.address.equals(deep.address) → true
        show("adres: original.address == shallow.address", original.address == shallow.address);
        // WYNIK: adres: original.address == shallow.address → true    ← ten sam obiekt

        // Pamiętaj: równość po kopii znika, gdy zmienisz jedną ze stron.
        deep.address.city = "Gdynia";
        show("original.equals(deep) po zmianie kopii", original.equals(deep));
        // WYNIK: original.equals(deep) po zmianie kopii → false

        // Dlaczego == i equals dają różne wyniki: == porównuje adresy w pamięci (czy to ten sam obiekt),
        // equals porównuje zawartość — jeśli klasa go nadpisała. Bez nadpisania equals dziedziczy z Object
        // zachowanie "==" i kopia NIGDY nie byłaby równa oryginałowi.

        // Wybór metody kopiowania — krótka ściągawka.
        note("zwykła klasa → konstruktor kopiujący albo fabryka copyOf");
        // WYNIK: ℹ zwykła klasa → konstruktor kopiujący albo fabryka copyOf
        note("rekord z niezmiennymi polami → kopia niepotrzebna, zmiany przez metody withX");
        // WYNIK: ℹ rekord z niezmiennymi polami → kopia niepotrzebna, zmiany przez metody withX
        note("tablica → clone() (płytko) lub pętla dla głębokiej kopii");
        // WYNIK: ℹ tablica → clone() (płytko) lub pętla dla głębokiej kopii
        note("kolekcja → new ArrayList<>(x) lub List.copyOf(x), przy zmiennych elementach kopiuj elementy");
        // WYNIK: ℹ kolekcja → new ArrayList<>(x) lub List.copyOf(x), przy zmiennych elementach kopiuj elementy
        note("duży graf obiektów → serializacja (Io09Serialization), wolna i wymaga Serializable");
        // WYNIK: ℹ duży graf obiektów → serializacja (Io09Serialization), wolna i wymaga Serializable

        // Serializacja (t18_io_files/Io09Serialization): zapis obiektu do bajtów i odczyt daje kopię
        // głęboką całego grafu — wymaga interfejsu Serializable w KAŻDEJ klasie grafu, jest wolna i podatna
        // na pułapki. Tu tylko wzmianka; w zwykłym kodzie wybierz konstruktor kopiujący.

        // DOBRA PRAKTYKA: po napisaniu kopiowania napisz test: skopiuj, sprawdź equals, zmień kopię
        // i upewnij się, że oryginał się nie zmienił. Dlaczego: to jedyny sposób, by wykryć zapomniane pole
        // albo współdzieloną referencję.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • b = a kopiuje REFERENCJĘ (alias), nie obiekt; == true, każda zmiana jest wspólna
     *   • kopia płytka kopiuje pola (referencje zostają wspólne); kopia głęboka kopiuje też obiekty zmienne
     *   • obiektów niezmiennych (String, Integer, LocalDate, rekord z niezmiennymi polami) nie trzeba kopiować
     *   • preferuj: konstruktor kopiujący albo fabrykę copyOf (działa z final i walidacją)
     *   • w hierarchii: wirtualna metoda copy() z typem kowariantnym, bo new Zwierzę(x) gubi podtyp
     *   • Object.clone(): kopia płytka pole po polu, bez konstruktora, wymaga Cloneable
     *     (inaczej CloneNotSupportedException); override jako public z węższym typem zwracanym
     *   • clone() jest zepsute: omija konstruktor, kłóci się z final, wyjątek sprawdzany,
     *     Cloneable bez metod; nie używaj w nowych klasach (Bloch, „Effective Java”, punkt 13)
     *   • tablice: clone() i Arrays.copyOf są płytkie; int[] jest bezpieczne, int[][] i Object[] — kopiuj pętlą
     *   • kolekcje: new ArrayList<>(x) = zmienna kopia płytka, List.copyOf(x) = niezmienna kopia płytka,
     *     Collections.unmodifiableList(x) = WIDOK (nie kopia)
     *   • rekordy: komponenty niezmienne albo kopia obronna w konstruktorze kompaktowym; zmiana = metoda withX
     *   • po kopii: == false, equals true (przy poprawnym equals); kopia płytka dzieli też == dla pól
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się kopia płytka od głębokiej? Podaj przykład klasy, w której ta różnica ma znaczenie.
     *   2. Co wypisze:  int[][] m = {{1}, {2}}; int[][] c = m.clone(); c[0][0] = 9;
     *                   System.out.println(m[0][0]);  ?
     *   3. ZNAJDŹ BŁĄD:  class Basket { List<String> items = new ArrayList<>();
     *                      Basket(Basket other) { this.items = other.items; } }
     *   4. Podaj trzy powody, dla których clone() uważa się za zepsute.
     *   5. Co wypisze:  List<String> a = new ArrayList<>(List.of("x"));
     *                   List<String> view = Collections.unmodifiableList(a);
     *                   List<String> copy = List.copyOf(a);
     *                   a.add("y");
     *                   System.out.println(view.size() + " " + copy.size());  ?
     *   6. ZNAJDŹ BŁĄD:  record Box(List<String> items) { }   // autor twierdzi: "rekord jest niezmienny"
     *                    List<String> list = new ArrayList<>();  Box box = new Box(list);  list.add("a");
     *   7. Co wypisze:  Animal pet = new Dog("Burek", 3);  Animal copy = new Animal(pet);
     *                   System.out.println(copy instanceof Dog);  ?   (klasy Animal i Dog jak w lekcji)
     *   8. Dlaczego po skopiowaniu obiektu {@code a == copy} jest false, a {@code a.equals(copy)} true?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Zmienna klasa dla ćwiczenia 2. */
    static class Team { // Team = zespół
        String name;
        List<String> members; // members = członkowie

        Team(String name, List<String> members) {
            this.name = name;
            this.members = members;
        }

        @Override
        public String toString() {
            return name + members;
        }
    }

    /** Rekord dla ćwiczenia 4 (bez metod "with" — to Twoje zadanie). */
    record Playlist(String title, List<String> songs) { // Playlist = lista utworów; songs = piosenki
        Playlist {
            songs = List.copyOf(songs);
        }
    }

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: kopia głęboka tablicy 2D", "1 | 99 | 2", () -> checkMatrix(Oop10Copying::exercise1));
        Check.equal("ćw. 2: konstruktor kopiujący zamiast clone()", "Alfa[Ala, Ola] | Beta[Ala, Ola, Ewa]",
                () -> checkTeam(Oop10Copying::exercise2));
        Check.equal("ćw. 3: kopia głęboka mapy list", "{a=[1, 2]} | {a=[1, 2, 3], b=[]}",
                () -> checkMap(Oop10Copying::exercise3));
        Check.equal("ćw. 4: rekord bez słowa 'with'", "3 | Playlist[title=Rock, songs=[A, C]]",
                () -> checkPlaylist(Oop10Copying::exercise4));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "1 | 99 | 2", () -> checkMatrix(Oop10Copying::solution1));
        Check.equal("ćw. 2 (wzorzec)", "Alfa[Ala, Ola] | Beta[Ala, Ola, Ewa]", () -> checkTeam(Oop10Copying::solution2));
        Check.equal("ćw. 3 (wzorzec)", "{a=[1, 2]} | {a=[1, 2, 3], b=[]}", () -> checkMap(Oop10Copying::solution3));
        Check.equal("ćw. 4 (wzorzec)", "3 | Playlist[title=Rock, songs=[A, C]]", () -> checkPlaylist(Oop10Copying::solution4));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    // --- sprawdzarki (testują niezależność kopii; używane dla zaślepek i wzorców) ---

    static String checkMatrix(UnaryOperator<int[][]> copier) { // copier = kopiujący; UnaryOperator = funkcja T → T
        int[][] original = {{1, 2}, {3, 4}};
        int[][] copy = copier.apply(original); // apply = zastosuj
        copy[0][0] = 99;
        return original[0][0] + " | " + copy[0][0] + " | " + copy[1].length;
    }

    static String checkTeam(UnaryOperator<Team> copier) {
        Team original = new Team("Alfa", new ArrayList<>(List.of("Ala", "Ola")));
        Team copy = copier.apply(original);
        copy.members.add("Ewa");
        copy.name = "Beta";
        return original + " | " + copy;
    }

    static String checkMap(UnaryOperator<Map<String, List<Integer>>> copier) {
        Map<String, List<Integer>> original = new LinkedHashMap<>(); // LinkedHashMap = mapa z kolejnością wstawiania
        original.put("a", new ArrayList<>(List.of(1, 2)));
        Map<String, List<Integer>> copy = copier.apply(original);
        copy.get("a").add(3);
        copy.put("b", new ArrayList<>()); // put = wstaw
        return original + " | " + copy;
    }

    static String checkPlaylist(java.util.function.BiFunction<Playlist, String, Playlist> remover) {
        Playlist original = new Playlist("Rock", List.of("A", "B", "C"));
        Playlist without = remover.apply(original, "B"); // without = bez
        return original.songs().size() + " | " + without;
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć GŁĘBOKĄ kopię tablicy dwuwymiarowej (zmiana kopii nie może ruszyć oryginału).
     * Podpowiedź: nowa tablica o tej samej liczbie wierszy, a każdy wiersz skopiuj przez clone().
     */
    static int[][] exercise1(int[][] source) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ — stary kod z clone() zamień na konstruktor kopiujący / fabrykę,
     * która zwraca GŁĘBOKĄ kopię zespołu (osobna lista członków).
     * <pre>{@code
     * // PRZED (płytka kopia, wspólna lista):
     * class Team implements Cloneable {
     *     public Team clone() { try { return (Team) super.clone(); } catch (CloneNotSupportedException e) { throw new AssertionError(e); } }
     * }
     * // PO: static Team exercise2(Team team) { ... new Team(..., ...) ... }
     * }</pre>
     * Podpowiedź: nowy ArrayList z tymi samymi członkami; imię (String) jest niezmienne.
     */
    static Team exercise2(Team team) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć GŁĘBOKĄ kopię mapy "klucz → lista liczb" (nowa mapa i nowe listy).
     * Podpowiedź: pętla po {@code source.entrySet()} (entrySet = zbiór wpisów), do nowej mapy wkładaj
     * {@code new ArrayList<>(entry.getValue())}. Mapy poznasz w t12_collections/Collections05Maps.
     */
    static Map<String, List<Integer>> exercise3(Map<String, List<Integer>> source) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): rekord Playlist nie ma metod "with". Zwróć NOWĄ playlistę bez podanego
     * utworu (oryginał ma pozostać bez zmian).
     * Podpowiedź: skopiuj piosenki do nowego ArrayList, usuń utwór (remove = usuń), zbuduj nowy rekord.
     */
    static Playlist exercise4(Playlist playlist, String song) {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int[][] solution1(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    static Team solution2(Team team) {
        return new Team(team.name, new ArrayList<>(team.members));
    }

    static Map<String, List<Integer>> solution3(Map<String, List<Integer>> source) {
        Map<String, List<Integer>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<Integer>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    static Playlist solution4(Playlist playlist, String song) {
        List<String> songs = new ArrayList<>(playlist.songs());
        songs.remove(song);
        return new Playlist(playlist.title(), songs);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Kopia płytka przepisuje pola, więc obiekty wskazywane przez pola są wspólne; głęboka kopiuje
     *      też te obiekty. Różnica ma znaczenie dla pól zmiennych, np. Person z polem Address:
     *      zmiana adresu w kopii płytkiej zmienia oryginał.
     *   2. 9 — clone() kopiuje tylko zewnętrzną tablicę, wiersze są wspólne.
     *   3. Konstruktor kopiujący przepisuje referencję do listy (alias), więc obie klasy współdzielą listę.
     *      Poprawnie: this.items = new ArrayList<>(other.items);
     *   4. Dowolne trzy: omija konstruktor i walidację; kłóci się z polami final; wymaga obsługi wyjątku
     *      sprawdzanego, który nie wystąpi; Cloneable nie ma metod (clone() jest protected w Object);
     *      kopia głęboka jest ręczna i krucha w podklasach.
     *   5. "2 1" — widok pokazuje zmianę oryginału (2 elementy), List.copyOf jest kopią z chwili tworzenia (1).
     *   6. Rekord chroni tylko referencję (pole final), a lista jest zmienna — po list.add("a") box widzi
     *      zmianę. Poprawka: konstruktor kompaktowy z items = List.copyOf(items).
     *   7. false — new Animal(pet) wybiera konstruktor klasy Animal i tworzy czyste Animal.
     *      Poprawnie: pet.copy() (metoda wirtualna).
     *   8. == porównuje adresy (czy to ten sam obiekt) — kopia to inny obiekt. equals porównuje zawartość
     *      (przy poprawnie nadpisanym equals), a kopia ma te same dane.
     */
    // </editor-fold>
}
