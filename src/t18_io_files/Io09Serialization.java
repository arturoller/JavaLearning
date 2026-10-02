package t18_io_files;

import helpers.Check;
import helpers.TempDir;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Serializacja obiektów — zapis obiektu jako bajtów i odczyt z powrotem
 *        (serialization = serializacja, zamiana obiektu na ciąg bajtów; deserialization = odtworzenie obiektu z bajtów;
 *         Serializable = „da się zserializować”; transient = przejściowe, pomijane przy zapisie)
 *
 * W SKRÓCIE:
 *   Serializacja zamienia graf obiektów (obiekt i wszystko, do czego prowadzą jego pola) na ciąg bajtów, który
 *   można zapisać do pliku albo wysłać przez sieć, a potem odtworzyć. Wystarczy "implements Serializable"
 *   i ObjectOutputStream / ObjectInputStream. To jednak mechanizm pełen pułapek: omija konstruktory,
 *   przywiązuje dane do struktury klasy i jest groźny dla bezpieczeństwa. W nowych projektach wybieramy JSON.
 *
 * ANALOGIA: spakowanie mebli do paczki.
 *   Szafę (obiekt) rozkręcasz na deski i śruby (bajty), wkładasz do kartonu i wysyłasz. Po drugiej stronie
 *   ktoś składa ją według instrukcji (klasa). Gdy instrukcja jest z innej wersji (serialVersionUID), meble
 *   nie pasują. Rzeczy, których nie spakowałeś (transient), po złożeniu po prostu znikają. A skręcanie
 *   cudzej paczki bez sprawdzenia, co jest w środku, to ryzyko: może tam być coś, co zrobi szkodę.
 *
 * JAK TO DZIAŁA:
 *   Zapis:   obiekt → ObjectOutputStream.writeObject → bajty (zaczynają się od magicznej liczby AC ED 00 05)
 *   Odczyt:  bajty → ObjectInputStream.readObject → NOWY obiekt (nie ten sam, tylko równy wartościami)
 *
 *   Co trafia do strumienia                      Co NIE trafia
 *   -------------------------------------------- -----------------------------------------------
 *   pola zwykłe (także prywatne i final)         pola transient (po odczycie: 0 / false / null)
 *   pola obiektów (rekurencyjnie, całe grafy)    pola static (należą do klasy, nie do obiektu)
 *   nazwa klasy i serialVersionUID               pola klas nadrzędnych, które NIE są Serializable
 *
 *   Wyjątki: NotSerializableException (obiekt w grafie nie jest Serializable), InvalidClassException (zła wersja
 *   klasy albo brak konstruktora), StreamCorruptedException (to nie jest strumień obiektów), EOFException (strumień
 *   się urwał), ClassNotFoundException (przy odczycie nie ma takiej klasy). Wszystkie poza ostatnim to IOException:
 *   wyjątek SPRAWDZANY, bo dysk i sieć zawodzą niezależnie od programu. ClassNotFoundException też jest sprawdzany.
 *
 *   Konstruktory przy odczycie:
 *     zwykła klasa Serializable  → konstruktor NIE jest wołany (więc nie działa walidacja z konstruktora!)
 *     pierwsza klasa nadrzędna bez Serializable → wołany jest jej konstruktor BEZPARAMETROWY
 *     rekord (record)            → wołany jest konstruktor kanoniczny (walidacja działa)
 *     enum                       → zapisywana jest tylko nazwa stałej, odczyt daje tę samą stałą (==)
 *
 * SŁÓWKA:
 *   serialVersionUID = identyfikator wersji klasy; transient = przejściowe; writeObject = zapisz obiekt;
 *   readObject = odczytaj obiekt; deep copy = głęboka kopia; shallow copy = płytka kopia; filter = filtr;
 *   allow-list = lista dozwolonych (wszystko inne jest zabronione); gadget chain = łańcuch „klocków” kodu,
 *   które razem wykonują atak; untrusted = niezaufany; payload = ładunek (dane do zserializowania).
 *
 * ZOBACZ TEŻ: t10_exceptions/Exceptions06ChainingWrapping (opakowywanie wyjątków), t18_io_files/Io08BinaryStreams
 *   (bajty i DataOutputStream), t18_io_files/Io05JsonManual (bezpieczniejsza alternatywa: JSON),
 *   t18_io_files/Io11IoExceptions (hierarchia wyjątków IO)
 * </pre>
 */
public class Io09Serialization {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // throws = „rzuca”: main przekazuje wyjątki wyżej
        title("Io09 — Serializacja obiektów");

        // Pliki lekcji powstają w katalogu tymczasowym i znikają w finally (blok finally = zawsze się wykona).
        Path dir = TempDir.create("io09"); // create = utwórz
        try {
            basics();                          // basics = podstawy
            toFile(dir.resolve("s2"));         // to file = do pliku
            transientAndStatic();              // transient and static = pola przejściowe i statyczne
            versionUid();                      // version uid = identyfikator wersji
            notSerializable();                 // not serializable = nie da się zserializować
            superclassWithoutSerializable();   // superclass without Serializable = klasa nadrzędna bez Serializable
            recordsEnumsAndConstructors();     // records, enums and constructors = rekordy, enumy i konstruktory
            customWriteReadObject();           // custom = własny zapis i odczyt
            deepCopy();                        // deep copy = głęboka kopia
            security();                        // security = bezpieczeństwo
            exercises(dir.resolve("cw"));      // exercises = ćwiczenia
        } finally {
            TempDir.deleteRecursively(dir);
        }
    }

    // =================================================================================================
    // Pomocnicze
    // =================================================================================================

    /** Akcja, która może rzucić IOException albo ClassNotFoundException (odczyt obiektu rzuca oba). */
    @FunctionalInterface
    private interface ThrowingIo {
        void run() throws IOException, ClassNotFoundException;
    }

    /** Wartość, której obliczenie może rzucić dowolny wyjątek (sprawdzany też). */
    @FunctionalInterface
    private interface Risky<T> {
        T get() throws Exception;
    }

    /**
     * ioFails = „IO zawodzi”. Wykonuje akcję i oczekuje wyjątku IO. Wypisuje NAZWĘ klasy wyjątku (i nazwę pliku dla
     * wyjątków systemu plików), nie komunikat: komunikaty IO bywają zależne od systemu i zawierają ścieżki.
     */
    private static void ioFails(String label, ThrowingIo action) {
        try {
            action.run();
            System.out.println("✘ " + label + " → NIE rzucono wyjątku (a spodziewaliśmy się go)");
        } catch (IOException | ClassNotFoundException e) {
            String file = "";
            if (e instanceof FileSystemException fse && fse.getFile() != null) { // instanceof ze wzorcem (Java 16+)
                file = " (plik: " + Path.of(fse.getFile()).getFileName() + ")";
            }
            System.out.println("✔ " + label + " → rzucono " + e.getClass().getSimpleName() + file);
        }
    }

    /** risky = „ryzykowne”: opakowuje wyjątki sprawdzane w IllegalStateException, żeby pasowało do Supplier w Check.equal. */
    private static <T> Supplier<T> risky(Risky<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new IllegalStateException(e.getClass().getSimpleName(), e);
            }
        };
    }

    /**
     * toBytes = „na bajty”. Zapis obiektu do tablicy bajtów w pamięci (przydatne w testach i do kopiowania).
     * UWAGA: ObjectOutputStream trzyma część danych we własnym buforze — dopiero close() (albo flush()) przepisuje
     * wszystko do ByteArrayOutputStream. Dlatego toByteArray() wołamy PO zamknięciu strumienia.
     */
    private static byte[] toBytes(Object value) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(); // buffer = bufor
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
            out.writeObject(value); // writeObject = zapisz obiekt
        }
        return buffer.toByteArray();
    }

    /**
     * fromBytes = „z bajtów”. Odczyt obiektu z bajtów. Parametr {@code Class<T> type} służy do bezpiecznego rzutowania
     * (type.cast) — dzięki temu nie ma ostrzeżenia o niesprawdzanym rzutowaniu (unchecked cast).
     */
    private static <T> T fromBytes(byte[] data, Class<T> type) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return type.cast(in.readObject()); // readObject = odczytaj obiekt; cast = rzutuj
        }
    }

    /** saveTo = „zapisz do”: serializacja obiektu do pliku (używana do przygotowania plików w ćwiczeniach). */
    private static void saveTo(Path file, Object value) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(value);
        }
    }

    /**
     * patchInt = „załataj liczbę całkowitą”: nadpisuje 4 bajty (int, kolejność big-endian) w tablicy, licząc od końca.
     * To SYMULACJA spreparowanego lub uszkodzonego strumienia. Bajty wiedzą, co zawierają: pole int na końcu danych
     * obiektu to ostatnie 4 bajty. W prawdziwym programie takich łat nie robisz — robi je atakujący.
     */
    private static byte[] patchInt(byte[] data, int bytesAfterInt, int newValue) {
        byte[] copy = data.clone(); // clone = sklonuj (nie psujemy oryginału)
        int start = copy.length - bytesAfterInt - 4;
        copy[start] = (byte) (newValue >>> 24);
        copy[start + 1] = (byte) (newValue >>> 16);
        copy[start + 2] = (byte) (newValue >>> 8);
        copy[start + 3] = (byte) newValue;
        return copy;
    }

    /** Dziennik zdarzeń (kto kiedy był wołany) — static, bo chcemy go widzieć niezależnie od obiektów. */
    private static final List<String> EVENTS = new ArrayList<>();

    // =================================================================================================
    // Klasy używane w lekcji (deklarujemy je tu, bo lekcje nie importują się nawzajem)
    // =================================================================================================

    /** Zwykła klasa Serializable. Pole password jest transient: po odczycie będzie null. */
    static class Person implements Serializable {
        // serialVersionUID = identyfikator wersji klasy (sekcja 4). Kompilator (-Xlint:all) ostrzega, gdy go brak.
        private static final long serialVersionUID = 1L;
        final String name;
        final int age;
        transient String password; // transient = przejściowe: NIE trafia do strumienia

        Person(String name, int age, String password) {
            this.name = name;
            this.age = age;
            this.password = password;
            EVENTS.add("konstruktor Person(" + name + ")");
        }

        @Override
        public String toString() {
            return name + " (" + age + ")";
        }
    }

    /** Sesja: pokazuje pola transient (z wartościami początkowymi!) i static. */
    static class Session implements Serializable {
        private static final long serialVersionUID = 1L;
        static String serverName = "serwer-A";           // static = należy do klasy
        final String user;
        transient int hits = 5;                          // transient z wartością początkową
        transient List<String> cache = new ArrayList<>(); // transient z obiektem początkowym

        Session(String user) {
            this.user = user;
        }
    }

    /** Klasa BEZ Serializable. */
    static class Address {
        final String city;

        Address(String city) {
            this.city = city;
        }
    }

    /** Klient trzymający adres, który nie jest Serializable — zapis się nie uda. */
    static class Customer implements Serializable {
        private static final long serialVersionUID = 1L;
        final String name;
        final Address address;

        Customer(String name, Address address) {
            this.name = name;
            this.address = address;
        }
    }

    /** Adres poprawiony: tym razem Serializable. */
    static class SerializableAddress implements Serializable {
        private static final long serialVersionUID = 1L;
        final String city;

        SerializableAddress(String city) {
            this.city = city;
        }
    }

    /** Klient z adresem Serializable — zapis działa. */
    static class GoodCustomer implements Serializable {
        private static final long serialVersionUID = 1L;
        final String name;
        final SerializableAddress address;

        GoodCustomer(String name, SerializableAddress address) {
            this.name = name;
            this.address = address;
        }
    }

    /** Klasa nadrzędna BEZ Serializable, za to z konstruktorem bezparametrowym (konstruktor bez argumentów). */
    static class Base {
        int baseValue = -1;

        Base() {
            EVENTS.add("konstruktor Base()");
        }
    }

    /** Klasa potomna: Serializable. Pola Base nie trafią do strumienia. */
    static class Child extends Base implements Serializable {
        private static final long serialVersionUID = 1L;
        int childValue;

        Child(int baseValue, int childValue) {
            this.baseValue = baseValue;
            this.childValue = childValue;
            EVENTS.add("konstruktor Child");
        }
    }

    /** Klasa nadrzędna BEZ konstruktora bezparametrowego i bez Serializable. */
    static class NoDefaultBase {
        final int id;

        NoDefaultBase(int id) {
            this.id = id;
        }
    }

    /** Klasa potomna zapisze się bez błędu, ale NIE da się jej odczytać. */
    static class BrokenChild extends NoDefaultBase implements Serializable {
        private static final long serialVersionUID = 1L;

        BrokenChild(int id) {
            super(id);
        }
    }

    /** Rekord: odczyt idzie przez konstruktor kanoniczny, więc walidacja z kompaktowego konstruktora działa. */
    record Percent(int value) implements Serializable {
        Percent { // kompaktowy konstruktor (compact constructor): walidacja
            if (value < 0 || value > 100) {
                throw new IllegalArgumentException("procent poza zakresem 0..100: " + value);
            }
        }
    }

    /** To samo w zwykłej klasie: walidacja jest w konstruktorze, a odczyt konstruktora NIE woła. */
    static class PercentClass implements Serializable {
        private static final long serialVersionUID = 1L;
        final int value;

        PercentClass(int value) {
            if (value < 0 || value > 100) {
                throw new IllegalArgumentException("procent poza zakresem 0..100: " + value);
            }
            this.value = value;
        }
    }

    /** Enum: serializowany jest tylko napis z nazwą stałej. */
    enum Color { RED, GREEN }

    /** Prostokąt z własnym readObject: sprawdza dane i odtwarza pole wyliczane (transient). */
    static class Rectangle implements Serializable {
        private static final long serialVersionUID = 1L;
        final int width;
        final int height;
        transient int area; // pole wyliczane: po co zajmować bajty, skoro da się policzyć

        Rectangle(int width, int height) {
            this.width = width;
            this.height = height;
            this.area = width * height;
        }

        // Te dwie metody MUSZĄ być private i mieć dokładnie takie sygnatury — JDK woła je przez refleksję.
        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject(); // defaultWriteObject = zapisz domyślnie (zwykłe pola)
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject(); // defaultReadObject = odczytaj domyślnie
            if (width <= 0 || height <= 0) { // walidacja zastępuje tę, której konstruktor nie wykona
                throw new InvalidObjectException("wymiary prostokąta muszą być dodatnie");
            }
            area = width * height; // odtworzenie pola wyliczanego
        }
    }

    /** Zmienny członek zespołu (do głębokiej kopii). */
    static class Member implements Serializable {
        private static final long serialVersionUID = 1L;
        final String name;
        int points; // zmienne pole

        Member(String name, int points) {
            this.name = name;
            this.points = points;
        }
    }

    /** Zespół ze zmienną listą członków. */
    static class Team implements Serializable {
        private static final long serialVersionUID = 1L;
        final String name;
        final List<Member> members;

        Team(String name, List<Member> members) {
            this.name = name;
            this.members = members; // celowo bez kopiowania: pokażemy płytką kopię
        }
    }

    /** Książka do ćwiczeń: zakładka (bookmark) jest transient. */
    static class Book implements Serializable {
        private static final long serialVersionUID = 1L;
        final String title;
        transient int bookmark = 42; // bookmark = zakładka

        Book(String title) {
            this.title = title;
        }
    }

    // =================================================================================================
    // 1. PODSTAWY: ZAPIS DO TABLICY BAJTÓW
    // =================================================================================================

    /**
     * 1. Obiekt → bajty → NOWY obiekt. Odczytany obiekt jest równy wartościami, ale to inna instancja.
     */
    static void basics() throws IOException, ClassNotFoundException {
        section("1. Obiekt → bajty → obiekt");

        Person ala = new Person("Ala", 30, "tajne123");
        byte[] bytes = toBytes(ala);
        show("pierwsze 4 bajty (hex)", HexFormat.of().formatHex(bytes, 0, 4)); // HexFormat = format szesnastkowy (Java 17+)
        // WYNIK: pierwsze 4 bajty (hex) → aced0005
        note("AC ED to „magiczna liczba” strumienia obiektów, 00 05 to wersja formatu.");
        // WYNIK: ℹ AC ED to „magiczna liczba” strumienia obiektów, 00 05 to wersja formatu.
        show("czy są jakieś bajty", bytes.length > 0);
        // WYNIK: czy są jakieś bajty → true

        EVENTS.clear(); // clear = wyczyść
        Person copy = fromBytes(bytes, Person.class);
        show("odczytana osoba", copy);
        // WYNIK: odczytana osoba → Ala (30)
        show("to ten sam obiekt (==)", copy == ala);
        // WYNIK: to ten sam obiekt (==) → false
        show("pola takie same", copy.name.equals(ala.name) && copy.age == ala.age);
        // WYNIK: pola takie same → true
        show("konstruktor Person wołany przy odczycie", EVENTS);
        // WYNIK: konstruktor Person wołany przy odczycie → []

        // PUŁAPKA: odczyt NIE woła konstruktora zwykłej klasy Serializable. Ustawienia, walidacja i wartości
        //   początkowe z konstruktora nie zadziałają — obiekt powstaje „z pominięciem drzwi”, pola wypełnia JDK.
        // DOBRA PRAKTYKA: serializuj małe, proste obiekty danych. Dlaczego: im więcej logiki w konstruktorze
        //   i im bardziej złożony graf, tym więcej niespodzianek przy odczycie.
    }

    // =================================================================================================
    // 2. DO PLIKU I Z PLIKU
    // =================================================================================================

    /**
     * 2. Do pliku zapisujemy tak samo, tylko strumień bajtów jest plikowy. Obiekty czyta się w TEJ SAMEJ kolejności,
     * w jakiej je zapisano. Graf obiektów zachowuje współdzielone referencje.
     */
    static void toFile(Path base) throws IOException, ClassNotFoundException {
        section("2. Do pliku i z pliku");
        Files.createDirectory(base);
        Path file = base.resolve("osoby.bin");

        Person ala = new Person("Ala", 30, "a");
        Person olek = new Person("Olek", 25, "b");
        // Files.newOutputStream = strumień bajtów do pliku (NIO.2), BufferedOutputStream = bufor (mniej wywołań systemu).
        try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            out.writeObject(ala);
            out.writeObject(olek);
            out.writeUTF("koniec"); // ObjectOutputStream umie też zapisać zwykłe wartości (writeInt, writeUTF...)
        }
        show("plik ma zawartość", Files.size(file) > 0);
        // WYNIK: plik ma zawartość → true

        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            Person first = (Person) in.readObject();
            Person second = (Person) in.readObject();
            String end = in.readUTF();
            show("odczytane", first + ", " + second + ", " + end);
            // WYNIK: odczytane → Ala (30), Olek (25), koniec
            ioFails("odczyt ponad koniec", () -> in.readObject());
            // WYNIK: ✔ odczyt ponad koniec → rzucono EOFException
            // EOFException = koniec pliku (end of file): zapisano trzy rzeczy, a próbujemy czytać czwartą.
        }

        // Graf obiektów: dwie pozycje listy wskazują TEN SAM obiekt. Serializacja to zapamiętuje.
        List<Person> list = new ArrayList<>();
        list.add(ala);
        list.add(ala);
        List<?> back = fromBytes(toBytes(list), List.class);
        show("po odczycie: pozycja 0 == pozycja 1", back.get(0) == back.get(1));
        // WYNIK: po odczycie: pozycja 0 == pozycja 1 → true
        show("po odczycie: pozycja 0 == oryginał", back.get(0) == ala);
        // WYNIK: po odczycie: pozycja 0 == oryginał → false
        note("Współdzielenie zostało zachowane (jeden obiekt), ale to już KOPIA, nie oryginał. Cykle też działają.");
        // WYNIK: ℹ Współdzielenie zostało zachowane (jeden obiekt), ale to już KOPIA, nie oryginał. Cykle też działają.

        // Uszkodzone dane:
        byte[] text = "to nie jest strumień obiektów".getBytes(StandardCharsets.UTF_8); // getBytes = pobierz bajty
        ioFails("odczyt zwykłego tekstu", () -> fromBytes(text, Person.class));
        // WYNIK: ✔ odczyt zwykłego tekstu → rzucono StreamCorruptedException
        byte[] whole = toBytes(ala);
        byte[] cut = Arrays.copyOf(whole, 10); // copyOf = skopiuj pierwsze 10 bajtów (urwany plik)
        ioFails("odczyt urwanego strumienia", () -> fromBytes(cut, Person.class));
        // WYNIK: ✔ odczyt urwanego strumienia → rzucono EOFException

        // ClassNotFoundException (sprawdzany!) pojawia się, gdy przy odczycie program nie zna klasy z pliku,
        // np. plik napisał inny program albo klasę przeniesiono do innego pakietu.

        // DOBRA PRAKTYKA: zawsze try-with-resources, a przy pliku dodaj BufferedOutputStream/BufferedInputStream.
        //   Dlaczego: bez zamknięcia nie wszystkie bajty trafią na dysk, a uchwyt pliku zostaje otwarty
        //   (w Windows otwartego pliku nie da się usunąć).
        // DOBRA PRAKTYKA: zapisuj i odczytuj w tej samej kolejności i tego samego typu; rzutuj świadomie
        //   (w tej lekcji robi to type.cast). Dlaczego: strumień nie ma „spisu treści”.
    }

    // =================================================================================================
    // 3. TRANSIENT I STATIC
    // =================================================================================================

    /**
     * 3. transient i static nie trafiają do strumienia. Po odczycie pola transient mają wartość domyślną
     * (0, false, null), a NIE wartość z inicjalizatora, bo konstruktor się nie wykonał.
     */
    static void transientAndStatic() throws IOException, ClassNotFoundException {
        section("3. transient i static");

        Person ala = new Person("Ala", 30, "tajne123");
        Person copy = fromBytes(toBytes(ala), Person.class);
        show("hasło po odczycie", String.valueOf(copy.password));
        // WYNIK: hasło po odczycie → null
        // DOBRA PRAKTYKA: pola, których nie wolno zapisać (hasła, klucze, uchwyty do połączeń, wątki,
        //   pamięć podręczna), oznacz transient. Dlaczego: bajty trafiają do plików i sieci, a hasło w pliku to wyciek.

        Session s = new Session("ola");
        s.hits = 99;
        s.cache.add("x");
        Session.serverName = "serwer-B"; // zmieniamy pole static PO zapisie, a PRZED odczytem
        byte[] bytes = toBytes(s);
        Session.serverName = "serwer-C";
        Session back = fromBytes(bytes, Session.class);
        show("hits po odczycie (zamiast 99 i zamiast 5)", back.hits);
        // WYNIK: hits po odczycie (zamiast 99 i zamiast 5) → 0
        show("cache po odczycie == null", back.cache == null);
        // WYNIK: cache po odczycie == null → true
        show("serverName po odczycie", Session.serverName);
        // WYNIK: serverName po odczycie → serwer-C
        Session.serverName = "serwer-A"; // porządek

        try {
            back.cache.add("y"); // cache = pamięć podręczna
        } catch (NullPointerException e) {
            System.out.println("✔ back.cache.add → rzucono NullPointerException (pole transient jest null)");
            // WYNIK: ✔ back.cache.add → rzucono NullPointerException (pole transient jest null)
        }

        // PUŁAPKA: pole transient z inicjalizatorem ("transient List x = new ArrayList<>()") po odczycie jest null,
        //   bo konstruktor ani inicjalizatory pól się nie wykonały. Skończy się to NullPointerException.
        //   Rozwiązania: odtworzyć pole w readObject (sekcja 8), albo leniwie przy pierwszym użyciu.
        // PUŁAPKA: pola static nie są w strumieniu, bo należą do KLASY. Po odczycie widzisz to, co klasa ma teraz
        //   w tej maszynie wirtualnej (tu: serwer-C), a nie wartość z chwili zapisu.
    }

    // =================================================================================================
    // 4. SERIALVERSIONUID
    // =================================================================================================

    /**
     * 4. serialVersionUID to „numer wersji” klasy zapisany w strumieniu. Przy odczycie JDK porównuje go z numerem
     * klasy w programie. Różne numery → InvalidClassException.
     */
    static void versionUid() {
        section("4. serialVersionUID");

        show("UID klasy Person (deklarowany)", ObjectStreamClass.lookup(Person.class).getSerialVersionUID());
        // WYNIK: UID klasy Person (deklarowany) → 1
        // ObjectStreamClass.lookup = „znajdź opis klasy w serializacji”; zwraca null, gdy klasa nie jest Serializable:
        show("lookup dla Thread == null", ObjectStreamClass.lookup(Thread.class) == null);
        // WYNIK: lookup dla Thread == null → true
        show("UID rekordu Percent (domyślnie)", ObjectStreamClass.lookup(Percent.class).getSerialVersionUID());
        // WYNIK: UID rekordu Percent (domyślnie) → 0
        show("UID enuma Color (zawsze)", ObjectStreamClass.lookup(Color.class).getSerialVersionUID());
        // WYNIK: UID enuma Color (zawsze) → 0

        List<String> names = new ArrayList<>();
        for (ObjectStreamField f : ObjectStreamClass.lookup(Person.class).getFields()) { // getFields = pola w strumieniu
            names.add(f.getName());
        }
        show("pola Person widoczne w strumieniu", names);
        // WYNIK: pola Person widoczne w strumieniu → [age, name]
        note("Pola password (transient) tu nie ma. Kolejność: najpierw typy proste, potem obiekty, alfabetycznie.");
        // WYNIK: ℹ Pola password (transient) tu nie ma. Kolejność: najpierw typy proste, potem obiekty, alfabetycznie.

        // Co się stanie, gdy NIE zadeklarujesz UID? JDK wyliczy go z kształtu klasy (nazwa, pola, metody, modyfikatory).
        // Wtedy każda zmiana klasy — nawet dodanie metody — albo inny kompilator daje INNY numer. Stary plik przestaje
        // się czytać: InvalidClassException ("local class incompatible: stream classdesc serialVersionUID = ...").
        //
        // Reguły praktyczne:
        //   • ZAWSZE deklaruj:  private static final long serialVersionUID = 1L;
        //   • zmiana zgodna (stary plik nadal się odczyta): dodanie pola (w starym pliku dostanie wartość domyślną)
        //     albo usunięcie pola — numeru NIE zmieniasz;
        //   • zmiana niezgodna (zmiana typu pola, zmiana klasy nadrzędnej, usunięcie Serializable): zwiększ numer
        //     (2L) — stare pliki zostaną odrzucone czytelnym wyjątkiem zamiast cichego zepsucia danych;
        //   • zmiana nazwy klasy lub pakietu to ClassNotFoundException przy odczycie starych plików.
        // Rekordy i enumy: UID wynosi 0 i nie jest porównywany (dla rekordów można go zadeklarować, ale odczyt
        // i tak go nie wymaga). Dlatego kompilator nie żąda UID w rekordach i enumach.

        // PUŁAPKA: brak serialVersionUID przy -Xlint:all to ostrzeżenie [serial]. Nie wyciszaj go adnotacją —
        //   dopisz pole. Dlaczego: wyliczony numer zmienia się „sam” i psuje wczytywanie danych po aktualizacji.
        // DOBRA PRAKTYKA: nie używaj serializacji do długotrwałego przechowywania danych. Format jest związany
        //   ze strukturą klas, więc każda ewolucja kodu to ryzyko. Do trwałych danych służą JSON, CSV, baza danych.
    }

    // =================================================================================================
    // 5. NOTSERIALIZABLEEXCEPTION
    // =================================================================================================

    /**
     * 5. Serializable to znacznik (interfejs bez metod). Jeśli KTÓRYKOLWIEK obiekt w grafie go nie ma,
     * zapis kończy się NotSerializableException. Komunikat to nazwa tej klasy, więc łatwo ją znaleźć.
     */
    static void notSerializable() throws IOException, ClassNotFoundException {
        section("5. NotSerializableException");

        expectThrows("zapis klienta z adresem", () -> toBytes(new Customer("Jan", new Address("Łódź"))));
        // WYNIK: ✔ zapis klienta z adresem → rzucono NotSerializableException: t18_io_files.Io09Serialization$Address
        // „$” w nazwie oznacza klasę zagnieżdżoną (Address jest wewnątrz Io09Serialization).
        expectThrows("zapis Optional", () -> toBytes(java.util.Optional.of("x")));
        // WYNIK: ✔ zapis Optional → rzucono NotSerializableException: java.util.Optional
        note("Optional celowo NIE jest Serializable. Podobnie: Thread, Stream, połączenia, Logger, zwykłe lambdy.");
        // WYNIK: ℹ Optional celowo NIE jest Serializable. Podobnie: Thread, Stream, połączenia, Logger, zwykłe lambdy.

        GoodCustomer good = fromBytes(toBytes(new GoodCustomer("Jan", new SerializableAddress("Łódź"))), GoodCustomer.class);
        show("po poprawce", good.name + ", " + good.address.city);
        // WYNIK: po poprawce → Jan, Łódź

        // Trzy sposoby naprawy:
        //   1. klasa pola też implements Serializable (jak SerializableAddress powyżej) — gdy to proste dane,
        //   2. pole oznaczone transient — gdy da się je odtworzyć albo nie jest potrzebne,
        //   3. nie serializować tego obiektu wcale; zapisać same dane (np. rekord z polami) i złożyć obiekt po odczycie.

        // PUŁAPKA: wyjątek wylatuje w trakcie zapisu. Do pliku mogła już trafić część bajtów — plik jest
        //   niekompletny i nie nadaje się do odczytu. Zapisuj najpierw do tablicy bajtów (toBytes) albo do pliku
        //   tymczasowego, a dopiero potem przenieś (zobacz atomowy zapis w t18_io_files/Io03WritingText).
        // PUŁAPKA: pole typu interfejs (np. List) jest w porządku, jeśli KONKRETNA klasa w środku jest Serializable
        //   (ArrayList jest; List.of też; wynik Arrays.asList też). Błąd wychodzi dopiero w czasie działania.
    }

    // =================================================================================================
    // 6. KLASA NADRZĘDNA BEZ SERIALIZABLE
    // =================================================================================================

    /**
     * 6. Pola klasy nadrzędnej, która nie jest Serializable, NIE trafiają do strumienia. Przy odczycie JDK woła jej
     * konstruktor bezparametrowy, a gdy go nie ma — odczyt się nie udaje (zapis się udał!).
     */
    static void superclassWithoutSerializable() throws IOException, ClassNotFoundException {
        section("6. Klasa nadrzędna bez Serializable");

        Child child = new Child(7, 8);
        byte[] bytes = toBytes(child);
        EVENTS.clear();
        Child back = fromBytes(bytes, Child.class);
        show("childValue po odczycie", back.childValue);
        // WYNIK: childValue po odczycie → 8
        show("baseValue po odczycie (było 7)", back.baseValue);
        // WYNIK: baseValue po odczycie (było 7) → -1
        show("wołane konstruktory", EVENTS);
        // WYNIK: wołane konstruktory → [konstruktor Base()]
        note("Wywołał się konstruktor Base(), konstruktor Child — nie. Pole z klasy nadrzędnej wróciło do wartości początkowej.");
        // WYNIK: ℹ Wywołał się konstruktor Base(), konstruktor Child — nie. Pole z klasy nadrzędnej wróciło do wartości początkowej.

        byte[] broken = toBytes(new BrokenChild(5));
        show("zapis BrokenChild się udał", broken.length > 0);
        // WYNIK: zapis BrokenChild się udał → true
        ioFails("odczyt BrokenChild", () -> fromBytes(broken, BrokenChild.class));
        // WYNIK: ✔ odczyt BrokenChild → rzucono InvalidClassException
        // Komunikat tego wyjątku brzmi „... no valid constructor” — brak konstruktora bezparametrowego w NoDefaultBase.

        // PUŁAPKA: błąd widać dopiero przy ODCZYCIE, czyli często po tygodniach, w innym programie. Przetestuj
        //   pełny obieg (zapis i odczyt) w teście jednostkowym — samo „zapisało się” niczego nie dowodzi.
        // DOBRA PRAKTYKA: jeśli klasa nadrzędna ma stan, niech sama będzie Serializable. Dlaczego: wtedy jej pola
        //   zapisują się razem z resztą i nic nie wraca „do ustawień fabrycznych”.
    }

    // =================================================================================================
    // 7. REKORDY, ENUMY I KONSTRUKTORY
    // =================================================================================================

    /**
     * 7. Rekord przy odczycie przechodzi przez konstruktor kanoniczny, więc walidacja działa. Zwykła klasa omija
     * konstruktor, więc spreparowany strumień wprowadza niedozwolone dane. Spreparowanie symulujemy łatą bajtów.
     */
    static void recordsEnumsAndConstructors() throws IOException, ClassNotFoundException {
        section("7. Rekordy, enumy i konstruktory");

        Percent ok = fromBytes(toBytes(new Percent(50)), Percent.class);
        show("rekord po obiegu", ok);
        // WYNIK: rekord po obiegu → Percent[value=50]

        // „Atak”: bierzemy poprawne bajty i podmieniamy ostatnie 4 bajty (pole int) na 250.
        byte[] badRecord = patchInt(toBytes(new Percent(50)), 0, 250);
        ioFails("odczyt rekordu z wartością 250", () -> fromBytes(badRecord, Percent.class));
        // WYNIK: ✔ odczyt rekordu z wartością 250 → rzucono InvalidObjectException
        note("Konstruktor kanoniczny rekordu zaprotestował, a JDK zgłosił to jako InvalidObjectException.");
        // WYNIK: ℹ Konstruktor kanoniczny rekordu zaprotestował, a JDK zgłosił to jako InvalidObjectException.

        byte[] badClass = patchInt(toBytes(new PercentClass(50)), 0, 250);
        PercentClass hacked = fromBytes(badClass, PercentClass.class);
        show("zwykła klasa po spreparowaniu", hacked.value);
        // WYNIK: zwykła klasa po spreparowaniu → 250
        note("Walidacja z konstruktora PercentClass nigdy się nie wykonała: obiekt łamie własną regułę 0..100.");
        // WYNIK: ℹ Walidacja z konstruktora PercentClass nigdy się nie wykonała: obiekt łamie własną regułę 0..100.

        // Enumy: w strumieniu jest tylko nazwa stałej, a odczyt oddaje TĘ SAMĄ stałą — porównanie == działa.
        Color red = fromBytes(toBytes(Color.RED), Color.class);
        show("enum po obiegu == Color.RED", red == Color.RED);
        // WYNIK: enum po obiegu == Color.RED → true
        show("zwykły obiekt po obiegu == oryginał", fromBytes(toBytes("tekst"), String.class) == "tekst");
        // WYNIK: zwykły obiekt po obiegu == oryginał → false

        // Rekordy: metody writeObject/readObject są IGNOROWANE; o wszystkim decydują składowe rekordu.
        // PUŁAPKA: singleton (klasa z jedną instancją) po odczycie ma DRUGĄ instancję. Naprawia to metoda
        //   private Object readResolve() { return INSTANCE; } („rozwiąż odczyt” — podmienia odczytany obiekt).
        //   Najprościej zrobić singleton jako enum — wtedy problem nie istnieje.
        // DOBRA PRAKTYKA: dane do serializacji trzymaj w rekordach. Dlaczego: walidacja w konstruktorze działa
        //   także dla danych z zewnątrz, a rekord nie ma ukrytego stanu.
    }

    // =================================================================================================
    // 8. WŁASNY WRITEOBJECT / READOBJECT
    // =================================================================================================

    /**
     * 8. Gdy trzeba sprawdzić dane po odczycie albo odtworzyć pola, definiujemy prywatne metody writeObject
     * i readObject. Wewnątrz wołamy defaultWriteObject / defaultReadObject, a resztę robimy sami.
     */
    static void customWriteReadObject() throws IOException, ClassNotFoundException {
        section("8. Własny readObject");

        Rectangle r = new Rectangle(3, 4);
        Rectangle back = fromBytes(toBytes(r), Rectangle.class);
        show("wymiary po obiegu", back.width + "x" + back.height);
        // WYNIK: wymiary po obiegu → 3x4
        show("pole wyliczane area (transient) odtworzone", back.area);
        // WYNIK: pole wyliczane area (transient) odtworzone → 12

        // Na końcu danych klasy z własnym writeObject jest jeszcze 1 bajt (znacznik końca bloku), stąd 1.
        // Pola int w strumieniu idą alfabetycznie: height, width — więc szerokość (width) jest ostatnia.
        byte[] hackedBytes = patchInt(toBytes(r), 1, -5);
        ioFails("odczyt prostokąta o szerokości -5", () -> fromBytes(hackedBytes, Rectangle.class));
        // WYNIK: ✔ odczyt prostokąta o szerokości -5 → rzucono InvalidObjectException

        // Inne haki serializacji (tylko nazwy, bez demonstracji):
        //   writeReplace / readResolve — podmiana obiektu przy zapisie / po odczycie (wzorzec „serialization proxy”),
        //   Externalizable — pełna, ręczna kontrola nad formatem (wolniejsza w użyciu, rzadko potrzebna).

        // DOBRA PRAKTYKA: w readObject traktuj dane jak niezaufane: sprawdź zakresy, wartości null, niezmienniki
        //   klasy. Dlaczego: konstruktor się nie wykona, więc to jedyne miejsce na walidację.
        // PUŁAPKA: writeObject i readObject muszą być private, bez wartości zwracanej (void) i z dokładnie takim
        //   parametrem. Przy innej sygnaturze (np. brak private) JDK po cichu ich nie użyje — i nikt nie zgłosi
        //   błędu, a Twoja walidacja po prostu nie działa. Dlatego sprawdź ją testem ze spreparowanym strumieniem.
    }

    // =================================================================================================
    // 9. GŁĘBOKA KOPIA PRZEZ SERIALIZACJĘ
    // =================================================================================================

    /**
     * 9. Zapis do bajtów i odczyt daje głęboką kopię całego grafu. Działa, ale to zwykle zły pomysł:
     * lepszy jest konstruktor kopiujący (albo niezmienne obiekty).
     */
    static void deepCopy() throws IOException, ClassNotFoundException {
        section("9. Głęboka kopia przez serializację");

        Team original = new Team("Alfa", new ArrayList<>(List.of(new Member("Ola", 10), new Member("Ewa", 20))));

        // PŁYTKA kopia (shallow copy): nowy Team, ale te same obiekty Member (i nawet ta sama lista).
        Team shallow = new Team(original.name, original.members);
        shallow.members.get(0).points = 99;
        show("oryginał po zmianie w płytkiej kopii", original.members.get(0).points);
        // WYNIK: oryginał po zmianie w płytkiej kopii → 99

        original.members.get(0).points = 10; // naprawiamy dane

        // GŁĘBOKA kopia (deep copy) przez serializację:
        Team deep = fromBytes(toBytes(original), Team.class);
        deep.members.get(0).points = 77;
        show("oryginał po zmianie w głębokiej kopii", original.members.get(0).points);
        // WYNIK: oryginał po zmianie w głębokiej kopii → 10
        show("głęboka kopia ma własną wartość", deep.members.get(0).points);
        // WYNIK: głęboka kopia ma własną wartość → 77

        // Konstruktor kopiujący (copy constructor) — to samo, jawnie i bez ukrytych kosztów:
        List<Member> copiedMembers = new ArrayList<>();
        for (Member m : original.members) {
            copiedMembers.add(new Member(m.name, m.points));
        }
        Team copied = new Team(original.name, copiedMembers);
        copied.members.get(1).points = 55;
        show("oryginał po zmianie w kopii z konstruktora", original.members.get(1).points);
        // WYNIK: oryginał po zmianie w kopii z konstruktora → 20

        // Dlaczego konstruktor kopiujący jest lepszy niż serializacja:
        //   • szybkość: serializacja tworzy bajty, używa refleksji i jest wielokrotnie wolniejsza,
        //   • wszystkie klasy w grafie muszą być Serializable (a Optional, wątki, połączenia nie są),
        //   • pola transient znikają (kopia jest „okaleczona”), a konstruktory i walidacja się nie wykonują,
        //   • sprawdzane wyjątki (IOException, ClassNotFoundException) tam, gdzie kopiowanie ich nie powinno mieć,
        //   • ukryte sprzężenie: dodanie pola nieserializowalnego psuje „zwykłe kopiowanie” w odległym kodzie.
        // DOBRA PRAKTYKA: preferuj obiekty niezmienne (rekordy, List.copyOf) — wtedy kopiowanie jest zbędne.
        //   Dlaczego: nie da się zmienić współdzielonego obiektu, więc nie trzeba go chronić kopią.
    }

    // =================================================================================================
    // 10. BEZPIECZEŃSTWO
    // =================================================================================================

    /**
     * Odczyt z filtrem: ustawiamy filtr na strumieniu PRZED readObject.
     */
    private static <T> T fromBytesFiltered(byte[] data, Class<T> type, ObjectInputFilter filter)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            in.setObjectInputFilter(filter); // setObjectInputFilter = ustaw filtr (Java 9+)
            return type.cast(in.readObject());
        }
    }

    /**
     * 10. Deserializacja niezaufanych danych to jedna z najgroźniejszych luk. Już samo wywołanie readObject
     * uruchamia kod klas odczytywanych ze strumienia. Obrona: nie deserializować cudzych danych, a gdy trzeba —
     * użyć filtra ObjectInputFilter z listą dozwolonych klas (allow-list).
     */
    static void security() throws IOException, ClassNotFoundException {
        section("10. Bezpieczeństwo i ObjectInputFilter");

        // Bez filtra readObject odtworzy KAŻDĄ klasę, jaką zna program (tu: java.util.logging.Level):
        Object anything = fromBytes(toBytes(Level.INFO), Object.class);
        show("bez filtra odczytano", anything);
        // WYNIK: bez filtra odczytano → INFO

        // Filtr z listą dozwolonych: nasz pakiet (t18_io_files), moduł java.base (String, ArrayList, Integer...),
        // a "!*" odrzuca wszystko inne. Reguły sprawdzane są od lewej do prawej, pierwsza pasująca wygrywa.
        ObjectInputFilter allowList = ObjectInputFilter.Config.createFilter("t18_io_files.*;java.base/*;!*");
        // createFilter = utwórz filtr z opisu tekstowego (Java 9+)

        byte[] personBytes = toBytes(new Person("Ala", 30, "x"));
        show("filtr przepuszcza Person", fromBytesFiltered(personBytes, Person.class, allowList));
        // WYNIK: filtr przepuszcza Person → Ala (30)
        byte[] foreign = toBytes(Level.INFO);
        ioFails("filtr zatrzymuje obcą klasę (Level)", () -> fromBytesFiltered(foreign, Object.class, allowList));
        // WYNIK: ✔ filtr zatrzymuje obcą klasę (Level) → rzucono InvalidClassException
        // Komunikat wyjątku zawiera „filter status: REJECTED” (REJECTED = odrzucono).

        // Filtr może też ograniczać rozmiary: maxarray (długość tablicy), maxdepth (głębokość grafu), maxrefs (liczba
        // referencji), maxbytes (bajty). Chroni przed „bombą”, czyli małym ładunkiem, który puchnie przy odczycie.
        // Limity sprawdzane są przy klasach i tablicach, więc tu pokazujemy tablicę (napisy same filtra nie wołają).
        ObjectInputFilter limit = ObjectInputFilter.Config.createFilter("maxarray=10;*");
        String[] manyTexts = new String[50]; // 50 pozycji, a limit to 10
        Arrays.fill(manyTexts, "x"); // fill = wypełnij
        byte[] big = toBytes(manyTexts);
        ioFails("filtr z limitem tablicy 10", () -> fromBytesFiltered(big, Object.class, limit));
        // WYNIK: ✔ filtr z limitem tablicy 10 → rzucono InvalidClassException

        // Jak wygląda atak (opis): atakujący nie wysyła „swojego” kodu. Układa strumień z obiektów klas, które JUŻ
        // są w programie (biblioteki na classpath!). Ich metody readObject, equals, hashCode czy toString wołane
        // w trakcie odczytu tworzą łańcuch (gadget chain), który na końcu uruchamia np. polecenie systemowe.
        // Kod ataku działa, zanim zdążysz zrobić rzutowanie na swoją klasę i sprawdzić dane.

        // Filtr globalny (Java 9+, JEP 290): ObjectInputFilter.Config.setSerialFilter(...) albo opcja uruchomienia
        //   -Djdk.serialFilter="pakiet.*;!*". W Javie 17 doszły filtry kontekstowe (JEP 415, „filter factory”).

        // PUŁAPKA: rzutowanie na swoją klasę po readObject NIE jest obroną. Atak następuje podczas readObject,
        //   a ClassCastException pojawia się za późno.
        // DOBRA PRAKTYKA: nigdy nie deserializuj danych z sieci, formularzy, plików od użytkownika ani z ciasteczek.
        //   Dlaczego: to zdalne wykonanie kodu na Twoim serwerze. Gdy już musisz — wąska lista dozwolonych klas.
        // DOBRA PRAKTYKA: do wymiany danych używaj formatów, które niosą TYLKO dane: JSON (t18_io_files/Io05JsonManual,
        //   a w praktyce biblioteka Jackson, której używa Spring Boot — SpringLearning), XML, Protocol Buffers.
        //   Dlaczego: parsowanie JSON nie tworzy dowolnych obiektów i nie uruchamia cudzych metod readObject.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Serializable = znacznik; ObjectOutputStream.writeObject / ObjectInputStream.readObject; odczyt daje
     *     NOWY obiekt, a strumień zaczyna się od AC ED 00 05.
     *   • Zawsze: private static final long serialVersionUID = 1L; (brak = ostrzeżenie [serial], a numer wyliczany
     *     „sam” zmienia się przy każdej zmianie klasy → InvalidClassException).
     *   • transient i static NIE trafiają do strumienia; po odczycie transient to 0/false/null (bez wartości
     *     z inicjalizatora).
     *   • Zwykła klasa: konstruktor NIE jest wołany. Klasa nadrzędna bez Serializable: woła się jej konstruktor
     *     bezparametrowy (brak → InvalidClassException przy odczycie). Rekord: woła się konstruktor kanoniczny.
     *     Enum: ta sama stała.
     *   • Obiekt w grafie bez Serializable → NotSerializableException z nazwą klasy (Optional, Thread, Stream...).
     *   • Własne readObject: waliduj dane, odtwarzaj pola wyliczane; defaultReadObject na początku.
     *   • Głęboka kopia przez serializację działa, ale konstruktor kopiujący jest szybszy i prostszy.
     *   • Niezaufane dane → NIE deserializować; w ostateczności ObjectInputFilter z allow-listą.
     *   • Nowe projekty: JSON zamiast serializacji Javy.
     *
     * PYTANIA KONTROLNE:
     *   1. Po co deklarować serialVersionUID i co się stanie, gdy go nie zadeklarujesz, a klasę zmienisz?
     *   2. Co wypisze:  Person p = new Person("Ala", 30, "x");  // password to pole transient
     *                   Person q = fromBytes(toBytes(p), Person.class);
     *                   System.out.println(q.password + " " + (q == p));  ?
     *   3. Czym różni się odczyt rekordu od odczytu zwykłej klasy, jeśli chodzi o konstruktor? Dlaczego to ważne?
     *   4. ZNAJDŹ BŁĄD:  class Konto implements Serializable {
     *                        private static final long serialVersionUID = 1L;
     *                        transient List<String> historia = new ArrayList<>();
     *                        void dodaj(String s) { historia.add(s); }
     *                    }
     *                    // zapis i odczyt przez ObjectOutputStream / ObjectInputStream, potem konto.dodaj("x")
     *   5. Co wypisze:  class A { int x = 1; }  class B extends A implements Serializable { int y = 2; }
     *                   B b = new B(); b.x = 10; b.y = 20;  B c = odczyt(zapis(b));
     *                   System.out.println(c.x + " " + c.y);  ?  (A ma konstruktor bezparametrowy)
     *   6. Dlaczego deserializacja danych z internetu jest groźna? Co jest lepszą obroną niż rzutowanie
     *      wyniku readObject na swoją klasę?
     *   7. ZNAJDŹ BŁĄD:  record Wiek(int v) implements Serializable {}  // „walidacja w rekordzie nie działa przy odczycie”
     *      — co trzeba dopisać, żeby walidacja działała, i czy trzeba coś dopisywać na stronie odczytu?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises(Path base) throws IOException {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Files.createDirectory(base);

        Path bookFile = base.resolve("ksiazka.bin");
        saveTo(bookFile, new Book("Pan Tadeusz"));
        Path listFile = base.resolve("lista.bin"); // tego pliku jeszcze nie ma
        List<Book> books = List.of(new Book("Lalka"), new Book("Quo vadis"));
        Path okFile = base.resolve("dozwolony.bin");
        saveTo(okFile, new Book("Pan Tadeusz"));
        Path foreignFile = base.resolve("obcy.bin");
        saveTo(foreignFile, Level.INFO);

        Check.equal("ćw. 1: tytuł i zakładka", "Pan Tadeusz/0", risky(() -> exercise1(bookFile)));
        Check.equal("ćw. 2: lista książek przez plik", List.of("Lalka/0", "Quo vadis/0"),
                risky(() -> exercise2(listFile, books)));
        Check.equal("ćw. 3: kopia zespołu", "10/99", risky(() -> {
            Team original = new Team("Alfa", new ArrayList<>(List.of(new Member("Ola", 10))));
            Team copy = exercise3(original);
            copy.members.get(0).points = 99;
            return original.members.get(0).points + "/" + copy.members.get(0).points;
        }));
        Check.equal("ćw. 4: dozwolony i obcy plik", List.of("Pan Tadeusz", "ODRZUCONO"),
                risky(() -> List.of(exercise4(okFile), exercise4(foreignFile))));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "Pan Tadeusz/0", risky(() -> solution1(bookFile)));
        Check.equal("ćw. 2 (wzorzec)", List.of("Lalka/0", "Quo vadis/0"), risky(() -> solution2(listFile, books)));
        Check.equal("ćw. 3 (wzorzec)", "10/99", risky(() -> {
            Team original = new Team("Alfa", new ArrayList<>(List.of(new Member("Ola", 10))));
            Team copy = solution3(original);
            copy.members.get(0).points = 99;
            return original.members.get(0).points + "/" + copy.members.get(0).points;
        }));
        Check.equal("ćw. 4 (wzorzec)", List.of("Pan Tadeusz", "ODRZUCONO"),
                risky(() -> List.of(solution4(okFile), solution4(foreignFile))));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): plik zawiera zserializowany obiekt klasy Book (pole title i transient bookmark).
     * Odczytaj go i zwróć napis "tytuł/zakładka", np. "Pan Tadeusz/0". Dlaczego zakładka to 0, a nie 42?
     * Podpowiedź: try-with-resources, new ObjectInputStream(Files.newInputStream(file)), rzutowanie (Book).
     */
    static String exercise1(Path file) throws IOException, ClassNotFoundException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zapisz listę książek do pliku (jako jeden obiekt: new ArrayList<>(books)), odczytaj ją
     * z powrotem i zwróć listę napisów "tytuł/zakładka" w tej samej kolejności.
     * Podpowiedź: odczytany obiekt rzutuj na List z wieloma znakami zapytania, a elementy na Book.
     */
    static List<String> exercise2(Path file, List<Book> books) throws IOException, ClassNotFoundException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ głęboką kopię przez serializację na konstruktor kopiujący (bez żadnych
     * strumieni):
     * <pre>{@code
     * // PRZED:
     * static Team copyTeam(Team original) throws IOException, ClassNotFoundException {
     *     return fromBytes(toBytes(original), Team.class);
     * }
     * // PO: nowy Team z NOWYMI obiektami Member (zmiana kopii nie może dotknąć oryginału)
     * }</pre>
     * Podpowiedź: pętla po original.members i new Member(m.name, m.points); lista wynikowa to nowy ArrayList.
     */
    static Team exercise3(Team original) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): odczytaj obiekt z pliku z filtrem allow-list, który dopuszcza tylko klasy z pakietu
     * t18_io_files i z modułu java.base. Dla Book zwróć jej tytuł, a gdy filtr odrzuci obiekt
     * (InvalidClassException) — napis "ODRZUCONO".
     * Podpowiedź: ObjectInputFilter.Config.createFilter("t18_io_files.*;java.base/*;!*") i setObjectInputFilter
     * ustawiony PRZED readObject; wyjątek złap w catch (InvalidClassException e).
     */
    static String exercise4(Path file) throws IOException, ClassNotFoundException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(Path file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            Book book = (Book) in.readObject();
            return book.title + "/" + book.bookmark;
        }
    }

    static List<String> solution2(Path file, List<Book> books) throws IOException, ClassNotFoundException {
        saveTo(file, new ArrayList<>(books));
        List<String> result = new ArrayList<>();
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            List<?> loaded = (List<?>) in.readObject();
            for (Object o : loaded) {
                Book book = (Book) o;
                result.add(book.title + "/" + book.bookmark);
            }
        }
        return result;
    }

    static Team solution3(Team original) {
        List<Member> members = new ArrayList<>();
        for (Member m : original.members) {
            members.add(new Member(m.name, m.points));
        }
        return new Team(original.name, members);
    }

    static String solution4(Path file) throws IOException, ClassNotFoundException {
        ObjectInputFilter allowList = ObjectInputFilter.Config.createFilter("t18_io_files.*;java.base/*;!*");
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            in.setObjectInputFilter(allowList);
            Object value = in.readObject();
            return ((Book) value).title;
        } catch (java.io.InvalidClassException e) {
            return "ODRZUCONO";
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. serialVersionUID to numer wersji klasy zapisany w strumieniu; przy odczycie JDK porównuje go z numerem
     *      klasy w programie. Bez deklaracji JDK wylicza go z kształtu klasy, więc po każdej zmianie klasy (nawet
     *      dodaniu metody) numer się zmienia (a bywa inny nawet po kompilacji innym kompilatorem) i stare dane dają
     *      InvalidClassException.
     *      Własny numer zmieniamy tylko przy niezgodnej zmianie.
     *   2. "null false" — pole transient nie trafia do strumienia (po odczycie null), a odczyt tworzy nowy obiekt,
     *      więc q == p to false.
     *   3. Rekord: JDK woła konstruktor kanoniczny, więc walidacja z kompaktowego konstruktora działa i spreparowane
     *      dane zostaną odrzucone. Zwykła klasa: konstruktor nie jest wołany, więc walidacja nie działa — trzeba ją
     *      powtórzyć w readObject.
     *   4. Pole historia jest transient z inicjalizatorem: po odczycie jest null (konstruktor ani inicjalizator się
     *      nie wykonały), więc konto.dodaj("x") rzuci NullPointerException. Napraw: odtworzyć listę w readObject
     *      albo nie oznaczać pola jako transient (jeśli historia ma być zapisana).
     *   5. "1 20" — pole x należy do klasy A, która nie jest Serializable, więc nie trafia do strumienia; przy odczycie
     *      wywoła się konstruktor A (x = 1). Pole y jest zapisane i wraca jako 20.
     *   6. Odczyt uruchamia kod klas ze strumienia (metody readObject, hashCode itd.), a atakujący układa je w łańcuch
     *      (gadget chain) wykonujący jego polecenia; dzieje się to ZANIM sprawdzisz typ wyniku, więc rzutowanie nie
     *      pomaga. Lepsza obrona: nie deserializować niezaufanych danych, a w ostateczności ObjectInputFilter
     *      z listą dozwolonych klas; najlepiej użyć JSON.
     *   7. To nie jest błąd, tylko brak walidacji: rekord Wiek nie ma w ogóle walidacji. Trzeba dopisać kompaktowy
     *      konstruktor ze sprawdzeniem zakresu, np. Wiek { if (v < 0) throw new IllegalArgumentException(...); }.
     *      Po stronie odczytu nic się nie dopisuje: JDK sam woła konstruktor kanoniczny.
     */
    // </editor-fold>
}
