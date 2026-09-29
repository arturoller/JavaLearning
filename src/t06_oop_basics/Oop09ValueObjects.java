package t06_oop_basics;

import helpers.Check;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Obiekty-wartości (value objects) — małe, niezmienne klasy zamiast „gołych” String i double
 *        (value object = obiekt-wartość, entity = encja, primitive obsession = obsesja typów prostych)
 *
 * W SKRÓCIE:
 *   Zamiast wszędzie przekazywać String email albo double temperature, tworzymy małe klasy Email,
 *   Temperature, Range. Sprawdzają poprawność RAZ (w fabryce/konstruktorze), są niezmienne i równe
 *   po wartości. Kompilator pilnuje wtedy, żeby nikt nie podał temperatury tam, gdzie oczekiwano e-maila.
 *
 * ANALOGIA:
 *   Banknot 50 zł to wartość: nie obchodzi Cię, KTÓRY to egzemplarz — dwa banknoty 50 zł są „tym samym”.
 *   Pracownik to encja: ma numer identyfikatora i nawet po zmianie nazwiska to wciąż ta sama osoba.
 *   A karteczka z napisem „50” (double) nie mówi, czy to złotówki, stopnie, czy kilogramy.
 *
 * JAK TO DZIAŁA:
 *   final class Email {
 *       private final String value;            ← niezmienne pole
 *       private Email(String value) { ... }    ← prywatny konstruktor
 *       static Email of(String raw) {          ← fabryka: normalizuje + waliduje
 *           ... strip().toLowerCase() ...  if (niepoprawny) throw ...
 *       }
 *       equals / hashCode / toString po value  ← równość PO WARTOŚCI
 *   }
 *   ENCJA:   równa, gdy to samo id (Customer #7 po zmianie nazwiska = ten sam klient)
 *   WARTOŚĆ: równa, gdy te same dane (Email "jan@x.pl" = Email "jan@x.pl")
 *
 * SŁÓWKA:
 *   value object = obiekt-wartość; entity = encja (obiekt z tożsamością); identity = tożsamość;
 *   primitive obsession = obsesja typów prostych; factory method = metoda fabrykująca; normalize =
 *   znormalizować (sprowadzić do jednej postaci); invariant = niezmiennik (warunek zawsze prawdziwy)
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop05ObjectMethods (equals i hashCode),
 *             t06_oop_basics/Oop06Immutability (niezmienność),
 *             t09_records/Records02Constructors (rekord z walidacją — krócej),
 *             t15_numbers/Numbers02MoneyValueObject (Money — najważniejszy obiekt-wartość w biznesie)
 * </pre>
 */
public class Oop09ValueObjects {

    public static void main(String[] args) {
        title("Oop09 — obiekty-wartości (value objects)");

        primitiveObsession();       // primitive obsession = obsesja typów prostych
        entityVsValue();            // entity vs value = encja kontra wartość
        emailValueObject();         // email value object = e-mail jako obiekt-wartość
        staticFactories();          // static factories = statyczne metody fabrykujące
        temperatureConversions();   // temperature conversions = przeliczenia temperatury
        rangeInvariant();           // range invariant = niezmiennik przedziału
        valueObjectsInUse();        // value objects in use = obiekty-wartości w użyciu
        recordsComparison();        // records comparison = porównanie z rekordami
        exercises();                // exercises = ćwiczenia
    }

    // Klasy przykładowe są zagnieżdżone (static nested — Oop07), żeby lekcja była w jednym pliku.

    // =================================================================================================
    // 1. PROBLEM: „OBSESJA TYPÓW PROSTYCH”
    // =================================================================================================

    /** Linia faktury z dwoma parametrami typu String — łatwo je zamienić miejscami. */
    static String invoiceLine(String customerName, String email) {   // invoice line = linia faktury
        return "Faktura dla " + customerName + " <" + email + ">";
    }

    /**
     * 1. Gdy wszystko jest String/double, kompilator nie odróżni imienia od e-maila ani stopni Celsjusza
     * od Fahrenheita. Błędne dane (zła wielkość liter, spacje, brak @) przechodzą bez słowa.
     */
    static void primitiveObsession() {
        section("1. Problem: obsesja typów prostych");

        show("poprawne wywołanie", invoiceLine("Jan Kowalski", "jan@example.com"));
        // WYNIK: poprawne wywołanie → Faktura dla Jan Kowalski <jan@example.com>
        show("zamienione argumenty (kompiluje się!)", invoiceLine("jan@example.com", "Jan Kowalski"));
        // WYNIK: zamienione argumenty (kompiluje się!) → Faktura dla jan@example.com <Jan Kowalski>

        String typed = "Jan@Example.com ";              // typed = wpisany przez użytkownika
        show("\"Jan@Example.com \" równe \"jan@example.com\"?", typed.equals("jan@example.com"));
        // WYNIK: "Jan@Example.com " równe "jan@example.com"? → false

        double temperature = 100;                       // 100 czego? °C — woda wrze; °F — ciepły dzień
        show("double temperature", temperature);
        // WYNIK: double temperature → 100.0
        note("sama liczba nie mówi, w jakiej jest jednostce");
        // WYNIK: ℹ sama liczba nie mówi, w jakiej jest jednostce

        // PUŁAPKA: te same sprawdzenia (trim, toLowerCase, czy jest @) trzeba by kopiować w KAŻDYM
        // miejscu, gdzie pojawia się e-mail. Wystarczy raz zapomnieć — i w bazie są „dwa różne” e-maile.
        // DOBRA PRAKTYKA: pojęcie z dziedziny (e-mail, kwota, temperatura, przedział) → własny mały typ.
    }

    // =================================================================================================
    // 2. ENCJA KONTRA WARTOŚĆ
    // =================================================================================================

    /** Encja: tożsamość = id. Nazwisko może się zmienić, a to wciąż ten sam klient. */
    static final class CustomerEntity {         // customer entity = klient (encja)
        private final long id;
        private String name;                    // encja często JEST zmienna — ale id nigdy

        CustomerEntity(long id, String name) {
            this.id = id;
            this.name = name;
        }

        void rename(String newName) {           // rename = zmień nazwę
            this.name = newName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return id == ((CustomerEntity) o).id;          // TYLKO id
        }

        @Override
        public int hashCode() {
            return Long.hashCode(id);                      // też TYLKO id — zgodnie z equals
        }

        @Override
        public String toString() {
            return "#" + id + " " + name;
        }
    }

    /**
     * 2. Encję rozpoznajemy po identyfikatorze (id), wartość — po wszystkich danych. Klient #7 po ślubie
     * zmienia nazwisko, ale to ten sam klient. Dwa e-maile „jan@x.pl” to ta sama wartość.
     */
    static void entityVsValue() {
        section("2. Encja kontra wartość");

        CustomerEntity before = new CustomerEntity(7, "Anna Nowak");      // np. wczytana wczoraj
        CustomerEntity after = new CustomerEntity(7, "Anna Kowalska");    // ta sama osoba, wczytana dziś
        show("before", before);
        // WYNIK: before → #7 Anna Nowak
        show("after", after);
        // WYNIK: after → #7 Anna Kowalska
        show("before.equals(after) — to samo id", before.equals(after));
        // WYNIK: before.equals(after) — to samo id → true
        before.rename("Anna Kowalska");
        show("po rename", before);
        // WYNIK: po rename → #7 Anna Kowalska

        //                 ENCJA                         WARTOŚĆ (value object)
        //   równość       po id                         po wszystkich polach
        //   zmienność     często zmienna                ZAWSZE niezmienna
        //   przykłady     Klient, Zamówienie, Konto     Email, Kwota, Temperatura, Przedział, Adres
        //   „zmiana”      setter / metoda na obiekcie   nowy obiekt (with..., plus...)
        // DOBRA PRAKTYKA: encja SKŁADA SIĘ z wartości — Customer ma pole Email, a nie String email.
    }

    // =================================================================================================
    // 3. EMAIL — WALIDACJA I NORMALIZACJA W JEDNYM MIEJSCU
    // =================================================================================================

    /** E-mail: znormalizowany (bez spacji, małe litery) i sprawdzony — inaczej obiekt nie powstanie. */
    static final class Email {
        private final String value;

        private Email(String value) {           // private — tworzymy tylko przez Email.of(...)
            this.value = value;
        }

        static Email of(String raw) {           // of = „z” (utwórz z tekstu)
            if (raw == null) {
                throw new IllegalArgumentException("e-mail jest null");
            }
            String normalized = raw.strip().toLowerCase(Locale.ROOT);   // strip (Java 11+) = usuń białe znaki z brzegów
            int at = normalized.indexOf('@');
            boolean valid = at > 0                                // coś przed @
                    && at == normalized.lastIndexOf('@')          // dokładnie jedna @
                    && at < normalized.length() - 1               // coś po @
                    && !normalized.contains(" ");                 // bez spacji w środku
            if (!valid) {
                throw new IllegalArgumentException("niepoprawny e-mail: " + raw);
            }
            return new Email(normalized);
        }

        String domain() {                       // domain = domena (część po @)
            return value.substring(value.indexOf('@') + 1);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return value.equals(((Email) o).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * 3. Cała wiedza „co to jest poprawny e-mail” siedzi w JEDNEJ metodzie Email.of. Kto ma w ręku obiekt
     * Email, ma gwarancję, że jest poprawny i znormalizowany — nie musi niczego sprawdzać ponownie.
     */
    static void emailValueObject() {
        section("3. Email — walidacja i normalizacja");

        Email typed = Email.of("  Jan@Example.COM ");
        show("Email.of(\"  Jan@Example.COM \")", typed);
        // WYNIK: Email.of("  Jan@Example.COM ") → jan@example.com
        show("domena", typed.domain());
        // WYNIK: domena → example.com
        show("równy Email.of(\"jan@example.com\")?", typed.equals(Email.of("jan@example.com")));
        // WYNIK: równy Email.of("jan@example.com")? → true

        expectThrows("Email.of(\"jan.example.com\")", () -> Email.of("jan.example.com"));
        // WYNIK: ✔ Email.of("jan.example.com") → rzucono IllegalArgumentException: niepoprawny e-mail: jan.example.com
        expectThrows("Email.of(\"a@b@c.pl\")", () -> Email.of("a@b@c.pl"));
        // WYNIK: ✔ Email.of("a@b@c.pl") → rzucono IllegalArgumentException: niepoprawny e-mail: a@b@c.pl
        // new Email("x")   → tu działa (ta sama klasa najwyższego poziomu), ale z innego pliku:
        //                    błąd kompilacji: Email(String) has private access

        // PUŁAPKA: prawdziwa walidacja e-maili jest bardzo trudna (standard RFC 5322). Nasza jest celowo
        // uproszczona — w projekcie użyj sprawdzonej biblioteki, ale ZAMKNIJ ją w Email.of.
        // DOBRA PRAKTYKA: normalizuj PRZED walidacją i przed zapisaniem — wtedy equals/hashCode działają
        // na jednej, „kanonicznej” postaci.
    }

    // =================================================================================================
    // 4. STATYCZNE METODY FABRYKUJĄCE (of, ofCelsius...) + PRYWATNY KONSTRUKTOR
    // =================================================================================================

    /** Temperatura przechowywana DOKŁADNIE: w setnych częściach stopnia Celsjusza (long). */
    static final class Temperature {
        private static final long ABSOLUTE_ZERO = -27315;  // -273,15 °C w setnych stopnia
        private final long centiCelsius;                   // centi = setna część

        private Temperature(long centiCelsius) {
            if (centiCelsius < ABSOLUTE_ZERO) {
                throw new IllegalArgumentException("poniżej zera absolutnego: " + format(centiCelsius / 100.0));
            }
            this.centiCelsius = centiCelsius;
        }

        static Temperature ofCelsius(double celsius) {      // of Celsius = ze stopni Celsjusza
            return new Temperature(Math.round(celsius * 100));   // Math.round = zaokrąglij do najbliższej całkowitej
        }

        static Temperature ofFahrenheit(double fahrenheit) { // of Fahrenheit = ze stopni Fahrenheita
            return ofCelsius((fahrenheit - 32) * 5 / 9);
        }

        double celsius() {
            return centiCelsius / 100.0;
        }

        double fahrenheit() {
            return celsius() * 9 / 5 + 32;
        }

        boolean isFreezing() {                  // is freezing = czy mróz (≤ 0 °C)
            return centiCelsius <= 0;
        }

        private static String format(double celsius) {
            return String.format(Locale.ROOT, "%.2f °C", celsius);   // Locale.ROOT = kropka dziesiętna zawsze
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return centiCelsius == ((Temperature) o).centiCelsius;   // porównujemy long — dokładnie
        }

        @Override
        public int hashCode() {
            return Long.hashCode(centiCelsius);
        }

        @Override
        public String toString() {
            return format(celsius());
        }
    }

    /**
     * 4. Konstruktor ma zawsze nazwę klasy, więc dwa konstruktory {@code Temperature(double)} (jeden dla °C,
     * drugi dla °F) są niemożliwe. Metody fabrykujące mają NAZWY, które mówią, co znaczy liczba.
     */
    static void staticFactories() {
        section("4. Statyczne metody fabrykujące");

        Temperature boiling = Temperature.ofCelsius(100);          // boiling = wrzenie
        Temperature warmDay = Temperature.ofFahrenheit(100);       // warm day = ciepły dzień
        show("ofCelsius(100)", boiling);
        // WYNIK: ofCelsius(100) → 100.00 °C
        show("ofFahrenheit(100)", warmDay);
        // WYNIK: ofFahrenheit(100) → 37.78 °C
        show("ofCelsius(-5).isFreezing()", Temperature.ofCelsius(-5).isFreezing());
        // WYNIK: ofCelsius(-5).isFreezing() → true

        expectThrows("Temperature.ofCelsius(-300)", () -> Temperature.ofCelsius(-300));
        // WYNIK: ✔ Temperature.ofCelsius(-300) → rzucono IllegalArgumentException: poniżej zera absolutnego: -300.00 °C

        // Dwa konstruktory o tych samych typach parametrów:
        //   Temperature(double celsius) { ... }
        //   Temperature(double fahrenheit) { ... }  → błąd kompilacji: constructor Temperature(double) is already defined
        //
        // ZALETY fabryk statycznych:
        //   • nazwa mówi, co znaczy argument (ofCelsius, ofFahrenheit, Email.of, List.of)
        //   • mogą normalizować i walidować PRZED utworzeniem obiektu
        //   • mogą zwrócić istniejący obiekt zamiast nowego (np. Integer.valueOf dla małych liczb)
        // DOBRA PRAKTYKA: private konstruktor + static of(...) = jedyna droga do obiektu wiedzie przez
        // sprawdzenia. Tak robi JDK: LocalDate.of, List.of, Optional.of, BigDecimal.valueOf.
    }

    // =================================================================================================
    // 5. PRZELICZENIA I PUŁAPKA double
    // =================================================================================================

    /**
     * 5. Obiekt-wartość sam wie, jak się przeliczać. Przechowuje wartość w dokładnej postaci (long setnych
     * stopnia), więc equals nie cierpi przez błędy zaokrągleń double (t01_basics/Basics08FloatingPoint).
     */
    static void temperatureConversions() {
        section("5. Przeliczenia i pułapka double");

        Temperature body = Temperature.ofCelsius(36.6);            // body = ciało (temperatura ciała)
        show("36.6 °C w Fahrenheitach", String.format(Locale.ROOT, "%.2f °F", body.fahrenheit()));
        // WYNIK: 36.6 °C w Fahrenheitach → 97.88 °F

        double rawRise = 0.1 + 0.2;                                 // wzrost o 0,1 °C i o 0,2 °C na „gołym” double
        show("0.1 + 0.2 na double", rawRise);
        // WYNIK: 0.1 + 0.2 na double → 0.30000000000000004
        show("równe 0.3?", rawRise == 0.3);
        // WYNIK: równe 0.3? → false
        show("ofCelsius(0.1 + 0.2).equals(ofCelsius(0.3))",
                Temperature.ofCelsius(0.1 + 0.2).equals(Temperature.ofCelsius(0.3)));
        // WYNIK: ofCelsius(0.1 + 0.2).equals(ofCelsius(0.3)) → true

        Temperature fromF = Temperature.ofFahrenheit(98.6);         // przeliczenie (98.6 - 32) * 5 / 9
        show("ofFahrenheit(98.6)", fromF);
        // WYNIK: ofFahrenheit(98.6) → 37.00 °C
        show("ofFahrenheit(98.6).equals(ofCelsius(37))", fromF.equals(Temperature.ofCelsius(37)));
        // WYNIK: ofFahrenheit(98.6).equals(ofCelsius(37)) → true

        // PUŁAPKA: gdyby pole było double i equals porównywało double, 0.1 + 0.2 i 0.3 byłyby RÓŻNYMI
        // temperaturami, a HashSet trzymałby „duplikaty”. Dlatego w fabryce zaokrąglamy do setnych.
        // DOBRA PRAKTYKA: w obiekcie-wartości przechowuj liczby w dokładnej postaci: long (grosze, setne
        // stopnia, gramy) albo BigDecimal (t15_numbers/Numbers01BigDecimal).
    }

    // =================================================================================================
    // 6. RANGE — NIEZMIENNIK from ≤ to
    // =================================================================================================

    /** Przedział liczb całkowitych [from..to], obustronnie domknięty. Niezmiennik: from ≤ to. */
    static final class Range {                  // range = przedział
        private final int from;
        private final int to;

        private Range(int from, int to) {
            if (from > to) {                    // niezmiennik sprawdzany w KONSTRUKTORZE → każda droga
                throw new IllegalArgumentException("from > to: " + from + " > " + to);
            }
            this.from = from;
            this.to = to;
        }

        static Range of(int from, int to) {
            return new Range(from, to);
        }

        int from() {
            return from;
        }

        int to() {
            return to;
        }

        int length() {                          // length = długość (ile liczb w przedziale)
            return to - from + 1;
        }

        boolean contains(int value) {           // contains = zawiera
            return value >= from && value <= to;
        }

        Range shiftBy(int delta) {              // shift by = przesuń o
            return new Range(from + delta, to + delta);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Range other = (Range) o;
            return from == other.from && to == other.to;
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }

        @Override
        public String toString() {
            return "[" + from + ".." + to + "]";
        }
    }

    /**
     * 6. Niezmiennik (invariant) to warunek, który jest prawdziwy przez całe życie obiektu. Sprawdzamy go
     * w konstruktorze, a że obiekt jest niezmienny — nikt go potem nie złamie.
     */
    static void rangeInvariant() {
        section("6. Range — niezmiennik from ≤ to");

        Range workHours = Range.of(8, 16);                  // work hours = godziny pracy
        show("workHours", workHours);
        // WYNIK: workHours → [8..16]
        show("długość", workHours.length());
        // WYNIK: długość → 9
        show("contains(12), contains(17)", workHours.contains(12) + ", " + workHours.contains(17));
        // WYNIK: contains(12), contains(17) → true, false
        show("shiftBy(2) — nowy obiekt", workHours.shiftBy(2));
        // WYNIK: shiftBy(2) — nowy obiekt → [10..18]
        show("workHours bez zmian", workHours);
        // WYNIK: workHours bez zmian → [8..16]
        show("Range.of(8, 16).equals(workHours)", Range.of(8, 16).equals(workHours));
        // WYNIK: Range.of(8, 16).equals(workHours) → true

        expectThrows("Range.of(10, 5)", () -> Range.of(10, 5));
        // WYNIK: ✔ Range.of(10, 5) → rzucono IllegalArgumentException: from > to: 10 > 5

        // PUŁAPKA: gdyby Range miał settery setFrom/setTo, niezmiennik dałoby się złamać w dowolnej chwili
        // (setFrom(100) przy to = 16). Niezmienność + walidacja w konstruktorze = gwarancja na zawsze.
    }

    // =================================================================================================
    // 7. OBIEKTY-WARTOŚCI W UŻYCIU: PARAMETRY METOD I HashSet
    // =================================================================================================

    /** Ta sama faktura co w sekcji 1 — ale e-mail ma własny typ. */
    static String invoiceLineSafe(String customerName, Email email) {   // safe = bezpieczna
        return "Faktura dla " + customerName + " <" + email + ">";
    }

    /**
     * 7. PRZED/PO: z typem Email zamiana argumentów to błąd KOMPILACJI, a nie błąd na produkcji.
     * Dzięki equals/hashCode po wartości obiekty-wartości działają poprawnie w HashSet.
     */
    static void valueObjectsInUse() {
        section("7. Obiekty-wartości w użyciu");

        // PRZED: invoiceLine(String, String) — zamiana miejscami się kompilowała (sekcja 1).
        // PO:
        show("invoiceLineSafe", invoiceLineSafe("Jan Kowalski", Email.of("Jan@Example.com")));
        // WYNIK: invoiceLineSafe → Faktura dla Jan Kowalski <jan@example.com>
        // invoiceLineSafe(Email.of("jan@example.com"), "Jan Kowalski")
        //     → błąd kompilacji: incompatible types (String nie jest Email i odwrotnie)

        Set<Email> subscribers = new HashSet<>();           // subscribers = subskrybenci (HashSet — Oop05, t12)
        subscribers.add(Email.of("Ola@Example.com"));
        subscribers.add(Email.of("  ola@example.com"));     // ten sam e-mail w innej postaci
        subscribers.add(Email.of("jan@example.com"));
        show("liczba subskrybentów", subscribers.size());
        // WYNIK: liczba subskrybentów → 2

        // DOBRA PRAKTYKA: obiekty-wartości „rozlewają się” po kodzie: gdy metoda przyjmuje Email, nie musi
        // już sprawdzać poprawności — typ jest dowodem, że sprawdzenie się odbyło.
    }

    // =================================================================================================
    // 8. PORÓWNANIE Z REKORDAMI (zapowiedź t09)
    // =================================================================================================

    /** Ten sam Range jako rekord (Java 16+): equals, hashCode, toString i gettery generuje kompilator. */
    record RangeRecord(int from, int to) {      // record = rekord (t09_records/Records01Basics)
        RangeRecord {                           // konstruktor kompaktowy — miejsce na walidację (t09_records/Records02Constructors)
            if (from > to) {
                throw new IllegalArgumentException("from > to: " + from + " > " + to);
            }
        }

        static RangeRecord of(int from, int to) {
            return new RangeRecord(from, to);
        }

        int length() {
            return to - from + 1;
        }
    }

    /**
     * 8. Klasa Range to ponad 60 linii; ten sam rekord — kilkanaście. Rekord jest final, ma pola
     * private final, equals/hashCode/toString po wartości. Walidację dopisujemy w konstruktorze kompaktowym.
     */
    static void recordsComparison() {
        section("8. Porównanie z rekordami");

        RangeRecord r = RangeRecord.of(3, 7);
        show("rekord", r);
        // WYNIK: rekord → RangeRecord[from=3, to=7]
        show("r.from(), r.length()", r.from() + ", " + r.length());
        // WYNIK: r.from(), r.length() → 3, 5
        show("equals po wartości", r.equals(new RangeRecord(3, 7)));
        // WYNIK: equals po wartości → true
        expectThrows("new RangeRecord(9, 1)", () -> new RangeRecord(9, 1));
        // WYNIK: ✔ new RangeRecord(9, 1) → rzucono IllegalArgumentException: from > to: 9 > 1

        // RÓŻNICE:
        //   • konstruktor rekordu jest zawsze dostępny (nie da się go ukryć jak private Range(...)),
        //     więc fabryka of(...) to tylko wygoda — walidacja MUSI być w konstruktorze kompaktowym;
        //   • rekord nie zrobi za Ciebie normalizacji ani kopii obronnych list (to wciąż Twoja praca).
        // DOBRA PRAKTYKA: od Java 16 obiekty-wartości pisz zwykle jako rekordy; klasę „ręczną” wybieraj,
        // gdy potrzebujesz ukryć konstruktor albo przechowywać dane inaczej, niż je pokazujesz (Temperature).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Obiekt-wartość: niezmienny, równy po wartości, sam pilnuje poprawności (Email, Temperature, Range).
     *   • Encja: równa po id, zwykle zmienna (Customer, Order). Encja składa się z wartości.
     *   • private konstruktor + static of(...) / ofCelsius(...) — nazwane fabryki, walidacja, normalizacja.
     *   • Niezmiennik (np. from ≤ to) sprawdzaj w konstruktorze — obejmie każdą drogę tworzenia.
     *   • Liczby przechowuj dokładnie (long setnych, groszy; BigDecimal), nie jako surowy double.
     *   • Własny typ zamiast String/double = kompilator łapie zamienione argumenty.
     *   • Rekord (Java 16+) = obiekt-wartość w kilku linijkach; walidacja w konstruktorze kompaktowym.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się równość encji od równości obiektu-wartości? Podaj przykład każdego.
     *   2. Co wypisze:  System.out.println(Email.of(" Ala@X.PL").equals(Email.of("ala@x.pl")));
     *   3. Dlaczego Temperature ma ofCelsius/ofFahrenheit zamiast dwóch konstruktorów?
     *   4. ZNAJDŹ BŁĄD:  static Range of(int from, int to) { return new Range(from, to); }
     *                    private Range(int from, int to) { this.from = from; this.to = to; }
     *                    Range shiftBy(int d) { return new Range(from + d, to + d); }
     *                    (walidacja from ≤ to jest tylko w innej metodzie create(...))
     *   5. Co wypisze:  System.out.println(Range.of(8, 16).shiftBy(-8).length());
     *   6. Dlaczego Temperature przechowuje long setnych stopnia zamiast double?
     *   7. Kiedy zamiast rekordu napiszesz zwykłą klasę-wartość?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: ten sam e-mail", true, () -> exercise1("Jan@X.pl ", "jan@x.pl"));
        Check.equal("ćw. 1: różne e-maile", false, () -> exercise1("jan@x.pl", "ola@x.pl"));
        Check.throwsException("ćw. 1: niepoprawny e-mail", IllegalArgumentException.class,
                () -> exercise1("bez-malpy", "bez-malpy"));
        Check.equal("ćw. 2: nakładają się", true,
                () -> exercise2(Range.of(1, 5), Range.of(5, 9)) && exercise2(Range.of(3, 4), Range.of(1, 10)));
        Check.equal("ćw. 2: rozłączne", false,
                () -> exercise2(Range.of(1, 4), Range.of(5, 9)) || exercise2(Range.of(7, 9), Range.of(1, 6)));
        Check.equal("ćw. 3: normalizacja", "00-950", () -> PostalCode.of(" 00-950 ").toString());
        Check.equal("ćw. 3: równość i hashCode", true, () -> PostalCode.of("31-000").equals(PostalCode.of("31-000 "))
                && PostalCode.of("31-000").hashCode() == PostalCode.of(" 31-000").hashCode());
        Check.throwsException("ćw. 3: zły format", IllegalArgumentException.class, () -> PostalCode.of("00950"));
        Check.equal("ćw. 4: 1,5 kg + 250 g", "1750 g", () -> Weight.ofKilograms(1.5).plus(Weight.ofGrams(250)).toString());
        Check.equal("ćw. 4: oryginał bez zmian", "100 g", () -> {
            Weight w = Weight.ofGrams(100);
            w.plus(Weight.ofGrams(50));
            return w.toString();
        });
        Check.equal("ćw. 4: 0,5 kg i 500 g w HashSet", 1,
                () -> distinctCount(Weight.ofKilograms(0.5), Weight.ofGrams(500)));
        Check.throwsException("ćw. 4: waga ujemna", IllegalArgumentException.class, () -> Weight.ofGrams(-5));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec): ten sam e-mail", true, () -> solution1("Jan@X.pl ", "jan@x.pl"));
        Check.equal("ćw. 1 (wzorzec): różne e-maile", false, () -> solution1("jan@x.pl", "ola@x.pl"));
        Check.throwsException("ćw. 1 (wzorzec): niepoprawny e-mail", IllegalArgumentException.class,
                () -> solution1("bez-malpy", "bez-malpy"));
        Check.equal("ćw. 2 (wzorzec): nakładają się", true,
                () -> solution2(Range.of(1, 5), Range.of(5, 9)) && solution2(Range.of(3, 4), Range.of(1, 10)));
        Check.equal("ćw. 2 (wzorzec): rozłączne", false,
                () -> solution2(Range.of(1, 4), Range.of(5, 9)) || solution2(Range.of(7, 9), Range.of(1, 6)));
        Check.equal("ćw. 3 (wzorzec): normalizacja", "00-950", () -> PostalCodeSolution.of(" 00-950 ").toString());
        Check.equal("ćw. 3 (wzorzec): równość i hashCode", true,
                () -> PostalCodeSolution.of("31-000").equals(PostalCodeSolution.of("31-000 "))
                        && PostalCodeSolution.of("31-000").hashCode() == PostalCodeSolution.of(" 31-000").hashCode());
        Check.throwsException("ćw. 3 (wzorzec): zły format", IllegalArgumentException.class,
                () -> PostalCodeSolution.of("00950"));
        Check.equal("ćw. 4 (wzorzec): 1,5 kg + 250 g", "1750 g",
                () -> WeightSolution.ofKilograms(1.5).plus(WeightSolution.ofGrams(250)).toString());
        Check.equal("ćw. 4 (wzorzec): oryginał bez zmian", "100 g", () -> {
            WeightSolution w = WeightSolution.ofGrams(100);
            w.plus(WeightSolution.ofGrams(50));
            return w.toString();
        });
        Check.equal("ćw. 4 (wzorzec): 0,5 kg i 500 g w HashSet", 1,
                () -> distinctCount(WeightSolution.ofKilograms(0.5), WeightSolution.ofGrams(500)));
        Check.throwsException("ćw. 4 (wzorzec): waga ujemna", IllegalArgumentException.class,
                () -> WeightSolution.ofGrams(-5));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 12 OK, ✘ 0 BŁĄD
    }

    /** Liczy różne obiekty według ICH equals/hashCode (do testów ćwiczenia 4). */
    static int distinctCount(Object... items) {
        Set<Object> set = new HashSet<>();
        for (Object item : items) {
            set.add(item);
        }
        return set.size();
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ porównywanie e-maili na obiekt-wartość. Stary sposób (kopiowany
     * w wielu miejscach, bez walidacji — „bez-malpy” uznałby za poprawny e-mail):
     * <pre>{@code
     * return a.trim().toLowerCase().equals(b.trim().toLowerCase());
     * }</pre>
     * Nowy sposób: utwórz dwa obiekty Email i porównaj je przez equals. Niepoprawny e-mail ma rzucić
     * IllegalArgumentException (zrobi to za Ciebie Email.of).
     * Podpowiedź: jedna linijka.
     */
    static boolean exercise1(String a, String b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć true, gdy przedziały a i b mają choć jedną wspólną liczbę
     * ([1..5] i [5..9] → true, [1..4] i [5..9] → false). Przedziały są obustronnie domknięte.
     * Podpowiedź: nakładają się, gdy a zaczyna się nie później niż kończy b ORAZ b zaczyna się
     * nie później niż kończy a — użyj from() i to().
     */
    static boolean exercise2(Range a, Range b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): dokończ obiekt-wartość PostalCode (polski kod pocztowy, np. "00-950").
     * of(text) ma usunąć spacje z brzegów (strip), sprawdzić format „2 cyfry, myślnik, 3 cyfry”
     * (inaczej IllegalArgumentException) i zwrócić nowy obiekt. Dopisz equals, hashCode i toString
     * (toString zwraca sam kod).
     * Podpowiedź: {@code text.strip().matches("\\d{2}-\\d{3}")} (regex: t04_strings/Strings05Regex);
     * equals/hashCode po polu value — jak w klasie Email.
     */
    static final class PostalCode {             // postal code = kod pocztowy
        private final String value;

        private PostalCode(String value) {
            this.value = value;
        }

        static PostalCode of(String text) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }

        // TODO: dopisz equals, hashCode i toString
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): dokończ obiekt-wartość Weight (waga), przechowywany DOKŁADNIE w gramach
     * (long). Fabryki: ofGrams(long) i ofKilograms(double) (zaokrąglij przez Math.round(kg * 1000)).
     * plus(other) zwraca NOWĄ wagę (suma). toString → "1750 g". Waga ujemna → IllegalArgumentException
     * (konstruktor już to sprawdza). Dopisz equals i hashCode po gramach.
     * Podpowiedź: wzoruj się na Temperature (sekcja 4) — Long.hashCode(grams).
     */
    static final class Weight {                 // weight = waga
        private final long grams;               // grams = gramy

        private Weight(long grams) {
            if (grams < 0) {
                throw new IllegalArgumentException("waga ujemna: " + grams + " g");
            }
            this.grams = grams;
        }

        static Weight ofGrams(long grams) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }

        static Weight ofKilograms(double kilograms) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }

        Weight plus(Weight other) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }

        // TODO: dopisz equals, hashCode i toString
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(String a, String b) {
        return Email.of(a).equals(Email.of(b));
    }

    static boolean solution2(Range a, Range b) {
        return a.from() <= b.to() && b.from() <= a.to();
    }

    static final class PostalCodeSolution {
        private final String value;

        private PostalCodeSolution(String value) {
            this.value = value;
        }

        static PostalCodeSolution of(String text) {
            String normalized = text.strip();
            if (!normalized.matches("\\d{2}-\\d{3}")) {     // matches = pasuje do wzorca (regex)
                throw new IllegalArgumentException("niepoprawny kod pocztowy: " + text);
            }
            return new PostalCodeSolution(normalized);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return value.equals(((PostalCodeSolution) o).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return value;
        }
    }

    static final class WeightSolution {
        private final long grams;

        private WeightSolution(long grams) {
            if (grams < 0) {
                throw new IllegalArgumentException("waga ujemna: " + grams + " g");
            }
            this.grams = grams;
        }

        static WeightSolution ofGrams(long grams) {
            return new WeightSolution(grams);
        }

        static WeightSolution ofKilograms(double kilograms) {
            return new WeightSolution(Math.round(kilograms * 1000));   // 0.1 * 1000 = 100.00000000000001 → 100
        }

        WeightSolution plus(WeightSolution other) {
            return new WeightSolution(grams + other.grams);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return grams == ((WeightSolution) o).grams;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(grams);
        }

        @Override
        public String toString() {
            return grams + " g";
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Encja: równa, gdy ma to samo id (Customer #7 po zmianie nazwiska to wciąż #7). Obiekt-wartość:
     *      równy, gdy ma te same dane (Email "jan@x.pl" = Email "jan@x.pl"; Range [1..5] = [1..5]).
     *   2. true — Email.of usuwa spacje z brzegów i zamienia litery na małe, więc oba obiekty mają
     *      wartość "ala@x.pl".
     *   3. Konstruktory mają tę samą nazwę (nazwę klasy), więc dwa konstruktory (double) są niemożliwe.
     *      Fabryki mają różne nazwy, które mówią, co znaczy liczba.
     *   4. Walidacja nie jest w konstruktorze, więc of(...) i shiftBy(...) mogą utworzyć Range z from > to.
     *      Niezmiennik trzeba sprawdzać w (prywatnym) konstruktorze — przez niego przechodzi każda droga.
     *   5. 9 — [0..8] ma dziewięć liczb.
     *   6. Bo rachunki na double dają błędy zaokrągleń (0.1 + 0.2 = 0.30000000000000004), a wtedy equals
     *      i hashCode uznałyby tę samą temperaturę za różne. long setnych porównuje się dokładnie.
     *   7. Gdy trzeba ukryć konstruktor (tylko fabryki), przechowywać dane w innej postaci niż pokazywane
     *      (Temperature: long setnych, a na zewnątrz double), albo gdy musisz pisać pod Javę starszą niż 16.
     */
    // </editor-fold>
}
