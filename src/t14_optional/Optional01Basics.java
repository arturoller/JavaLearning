package t14_optional;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Customer;
import helpers.model.Student;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Optional — podstawy. Pudełko na wartość, której może nie być
 *        (optional = opcjonalny, nieobowiązkowy; basics = podstawy)
 *
 * W SKRÓCIE:
 *   Optional to obiekt-opakowanie, w którym jest JEDNA wartość albo NIC. Metoda, która czasem nie ma
 *   czego zwrócić (np. klient bez e-maila), zamiast null zwraca Optional. Już sam typ mówi wtedy
 *   wywołującemu: „uważaj, wyniku może nie być — zdecyduj, co wtedy zrobić”.
 *
 * ANALOGIA: paczka z paczkomatu.
 *   • Paczka może zawierać towar albo być pusta — ale SAMA PACZKA zawsze istnieje (Optional nigdy nie jest null).
 *   • Zanim użyjesz towaru, zaglądasz do środka (isPresent) albo od razu mówisz, co zrobisz, gdy paczka będzie
 *     pusta: „weź towar ALBO zamiennik” (orElse), „ALBO zamów nowy” (orElseGet), „ALBO złóż reklamację” (orElseThrow).
 *   • null to sytuacja, w której kurier w ogóle nie przyniósł paczki, a Ty i tak sięgasz po towar —
 *     ręka trafia w powietrze (NullPointerException).
 *
 * JAK TO DZIAŁA:
 *   {@code Optional<T>} to zwykła klasa generyczna z jednym polem „value”:
 *     Optional.of("x")         → pudełko z wartością "x"         (toString: Optional[x])
 *     Optional.empty()         → puste pudełko                   (toString: Optional.empty)
 *     Optional.ofNullable(v)   → gdy v == null → puste, w przeciwnym razie → z wartością
 *   Wyjmowanie wartości zawsze wymaga decyzji, co zrobić z pustym pudełkiem:
 *     orElse(domyślna)         → wartość albo domyślna (domyślna jest liczona ZAWSZE)
 *     orElseGet(() -> ...)     → wartość albo wynik lambdy (lambda uruchamia się TYLKO dla pustego)
 *     orElseThrow(...)         → wartość albo wyjątek
 *     ifPresent(akcja)         → wykonaj akcję tylko wtedy, gdy wartość jest
 *   Optional jest NIEZMIENNY (immutable): żadna metoda nie zmienia pudełka, najwyżej zwraca nowe.
 *
 * SŁÓWKA:
 *   optional = opcjonalny; present = obecny; empty = pusty; of = z (wartości); nullable = mogący być null;
 *   get = pobierz; orElse = albo (w przeciwnym razie); orElseGet = albo pobierz; orElseThrow = albo rzuć;
 *   supplier = dostawca; if present = jeśli obecny; or else = w przeciwnym razie; no value present = brak wartości;
 *   as double / as int = jako double / jako int; average = średnia; grade = ocena; phone book = książka telefoniczna.
 *
 * ZOBACZ TEŻ: Optional02Transform (map, flatMap, filter — praca z wartością bez wyjmowania jej),
 *             Optional03BestPractices (kiedy Optional, a kiedy nie), t10_exceptions/Exceptions01Basics
 *             (NullPointerException i inne wyjątki), t16_streams/Streams14OptionalInStreams (Optional w streamach).
 * </pre>
 */
public class Optional01Basics {

    /** defaultCalls = liczba wywołań (metody liczącej) wartości domyślnej. Licznik do sekcji 6. */
    private static int defaultCalls = 0;

    public static void main(String[] args) {
        title("Optional01 — podstawy: pudełko na wartość, której może nie być");

        nullProblem();              // null problem = problem z null
        whatIsOptional();           // what is optional = czym jest Optional
        creatingOptionals();        // creating optionals = tworzenie Optionali
        checkingPresence();         // checking presence = sprawdzanie obecności wartości
        getAndOrElse();             // get and orElse = pobierz oraz „albo wartość domyślna”
        orElseVsOrElseGet();        // orElse vs orElseGet = orElse kontra orElseGet (leniwość)
        orElseThrowVariants();      // orElseThrow variants = odmiany orElseThrow
        ifPresentVariants();        // ifPresent variants = odmiany ifPresent
        primitiveOptionals();       // primitive optionals = Optionale dla typów prostych
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PROBLEM Z NULL
    // =================================================================================================

    /**
     * 1. Skąd w ogóle wziął się Optional? Z kłopotów z null.
     * <p>
     * null znaczy „ta zmienna nie wskazuje na żaden obiekt”. Kłopot w tym, że NIC w typie tego nie zdradza:
     * metoda {@code String get(Object key)} może zwrócić napis albo null, a kompilator pozwala od razu
     * wywołać na wyniku {@code .length()}. Błąd wychodzi dopiero w czasie działania programu.
     * <p>
     * Tony Hoare, który w 1965 roku dodał null do języka ALGOL W, nazwał go po latach
     * „moim błędem za miliard dolarów” (billion-dollar mistake) — tyle mniej więcej kosztowały świat
     * awarie spowodowane przez NullPointerException i podobne błędy.
     */
    static void nullProblem() {
        section("1. Problem z null — „błąd za miliard dolarów”");

        // LinkedHashMap = mapa pamiętająca kolejność wstawiania; phoneBook = książka telefoniczna (imię → numer)
        Map<String, String> phoneBook = new LinkedHashMap<>();
        phoneBook.put("Anna", "600-100-200");                     // put = włóż parę klucz → wartość
        phoneBook.put("Piotr", "601-300-400");

        String annaPhone = phoneBook.get("Anna");                 // get = pobierz wartość spod klucza
        show("telefon Anny", annaPhone);
        // WYNIK: telefon Anny → 600-100-200

        String olaPhone = phoneBook.get("Ola");                   // klucza "Ola" nie ma → get po cichu zwraca null
        show("telefon Oli", olaPhone);
        // WYNIK: telefon Oli → null

        expectThrows("długość numeru Oli", () -> olaPhone.length());      // length = długość napisu
        // WYNIK: ✔ długość numeru Oli → rzucono NullPointerException: Cannot invoke "String.length()" because "olaPhone" is null
        // Tłumaczenie: „nie można wywołać String.length(), bo olaPhone jest null”.

        // PUŁAPKA: kompilator NIE ostrzegł nas ani słowem. Typ String nie mówi „może być null” —
        //   trzeba o tym pamiętać albo przeczytać dokumentację. Programiści zapominają, stąd tyle błędów.

        // To samo z Optional: typ Optional<String> MÓWI, że wartości może nie być.
        Optional<String> olaPhoneBox = Optional.ofNullable(phoneBook.get("Ola"));  // ofNullable — sekcja 3
        show("telefon Oli jako Optional", olaPhoneBox);
        // WYNIK: telefon Oli jako Optional → Optional.empty

        // olaPhoneBox.length()  — NIE skompiluje się: Optional nie ma metody length(). Kompilator zmusza nas,
        // żebyśmy najpierw zdecydowali, co zrobić, gdy pudełko jest puste:
        show("numer Oli albo komunikat", olaPhoneBox.orElse("brak numeru"));
        // WYNIK: numer Oli albo komunikat → brak numeru

        note("Optional przenosi problem „a co, jeśli nic nie ma?” z czasu działania do czasu pisania kodu.");
        // WYNIK: ℹ Optional przenosi problem „a co, jeśli nic nie ma?” z czasu działania do czasu pisania kodu.
    }

    // =================================================================================================
    // 2. CZYM JEST OPTIONAL
    // =================================================================================================

    /**
     * 2. Optional to pudełko na 0 albo 1 wartość.
     * <p>
     * W danych przykładowych klient ma metodę {@code findEmail()}, która zwraca {@code Optional<String>}:
     * pełne pudełko, gdy e-mail jest, i puste, gdy go nie ma (Maria Nowak i Ola Pawlak nie mają e-maila).
     * Przedrostek find (znajdź) w nazwie metody to konwencja: „szukam, ale mogę nie znaleźć”.
     */
    static void whatIsOptional() {
        section("2. Czym jest Optional — pudełko na 0 albo 1 wartość");

        for (Customer c : SampleData.customers()) {                    // customers = klienci
            show(c.name(), c.findEmail());                              // findEmail = znajdź e-mail
        }
        // WYNIK: Jan Kowalski → Optional[jan@example.com]
        // WYNIK: Maria Nowak → Optional.empty
        // WYNIK: Adam Mazur → Optional[adam.mazur@example.com]
        // WYNIK: Zofia Krawczyk → Optional[zofia@example.com]
        // WYNIK: Ola Pawlak → Optional.empty
        // WYNIK: Marek Król → Optional[marek@example.com]
        // WYNIK: Ewa Lis → Optional[ewa.lis@example.com]

        // Zapamiętaj dwie postaci toString:  Optional[wartość]  i  Optional.empty.
        // Gdy na wydruku widzisz „Optional[...]”, a spodziewałeś się samej wartości — zapomniałeś ją wyjąć.

        // Ważne cechy Optional:
        //   • sam Optional NIGDY nie powinien być null — ma być pudełkiem, pełnym albo pustym,
        //   • w środku nigdy nie ma null (null „zamienia się” w puste pudełko),
        //   • jest niezmienny (immutable) — raz utworzonego pudełka nie da się napełnić ani opróżnić,
        //   • jest generyczny: Optional<String>, Optional<Customer>, Optional<BigDecimal>...
        Optional<Customer> firstVip = Optional.of(SampleData.customers().get(0));   // Optional z klientem
        show("pudełko z klientem", firstVip);
        // WYNIK: pudełko z klientem → Optional[Jan Kowalski (Warszawa, VIP)]
    }

    // =================================================================================================
    // 3. TWORZENIE: of, ofNullable, empty
    // =================================================================================================

    /**
     * 3. Trzy sposoby tworzenia Optional.
     * <ul>
     *   <li>{@code Optional.of(v)} — „na pewno mam wartość”. Dla null rzuca NullPointerException OD RAZU.</li>
     *   <li>{@code Optional.ofNullable(v)} — „wartość może być null”. null daje puste pudełko.</li>
     *   <li>{@code Optional.empty()} — „na pewno nie mam wartości”.</li>
     * </ul>
     */
    static void creatingOptionals() {
        section("3. Tworzenie: of, ofNullable, empty");

        Optional<String> coffee = Optional.of("kawa");                  // of = z (podanej wartości)
        Optional<String> nothing = Optional.empty();                    // empty = pusty
        show("Optional.of(\"kawa\")", coffee);
        show("Optional.empty()", nothing);
        // WYNIK: Optional.of("kawa") → Optional[kawa]
        // WYNIK: Optional.empty() → Optional.empty

        String fromDatabase = null;                                     // np. wynik starej metody, która zwraca null
        String fromForm = "Kraków";                                     // np. pole wypełnione w formularzu
        show("ofNullable(null)", Optional.ofNullable(fromDatabase));    // ofNullable = z wartości, która może być null
        show("ofNullable(\"Kraków\")", Optional.ofNullable(fromForm));
        // WYNIK: ofNullable(null) → Optional.empty
        // WYNIK: ofNullable("Kraków") → Optional[Kraków]

        expectThrows("Optional.of(null)", () -> Optional.of(fromDatabase));
        // WYNIK: ✔ Optional.of(null) → rzucono NullPointerException: (brak komunikatu)

        // Po co of, skoro ofNullable jest „bezpieczniejsze”? Bo of to DEKLARACJA: „tu null jest niemożliwy”.
        // Jeśli jednak się pojawi, to znaczy, że gdzieś jest błąd — i of zgłosi go natychmiast, w miejscu
        // powstania (zasada fail fast = zawiedź szybko), a nie trzy metody dalej.
        // DOBRA PRAKTYKA: of — gdy wartość na pewno istnieje; ofNullable — gdy null jest dozwolony
        //   (np. wynik Map.get albo starego API); empty — gdy wiesz, że wyniku nie ma.

        // Puste pudełko jest JEDNO na cały program (singleton) — empty() nie tworzy nowego obiektu.
        // Dwie zmienne tego samego typu Optional<String> — bez tego kompilator nie pozwoli porównać
        // Optional<Object> (sam empty() bez podpowiedzi typu) z Optional<String> („incomparable types”).
        Optional<String> emptyBox = Optional.empty();
        Optional<String> fromNull = Optional.ofNullable(fromDatabase);
        show("empty() == ofNullable(null)?", emptyBox == fromNull);
        // WYNIK: empty() == ofNullable(null)? → true
        // Nie opieraj jednak kodu na == — Optionale porównuje się przez equals (Optional02Transform, sekcja 9).
    }

    // =================================================================================================
    // 4. SPRAWDZANIE: isPresent, isEmpty
    // =================================================================================================

    /**
     * 4. isPresent() — czy w pudełku jest wartość; isEmpty() (Java 11+) — czy pudełko jest puste.
     * Obie zwracają boolean i są swoim zaprzeczeniem.
     */
    static void checkingPresence() {
        section("4. Sprawdzanie: isPresent i isEmpty (Java 11+)");

        List<Customer> customers = SampleData.customers();
        for (Customer c : customers.subList(0, 3)) {                    // subList(0, 3) = pierwsi trzej klienci
            Optional<String> email = c.findEmail();
            System.out.println("   " + c.name() + ": isPresent = " + email.isPresent()   // is present = czy obecny
                    + ", isEmpty = " + email.isEmpty());                                 // is empty = czy pusty
        }
        // WYNIK: Jan Kowalski: isPresent = true, isEmpty = false
        // WYNIK: Maria Nowak: isPresent = false, isEmpty = true
        // WYNIK: Adam Mazur: isPresent = true, isEmpty = false

        // Przykład sensownego użycia: POLICZ klientów bez e-maila (sama informacja „jest/nie ma”, bez wartości).
        int withoutEmail = 0;
        for (Customer c : customers) {
            if (c.findEmail().isEmpty()) {                              // czytelniej niż !c.findEmail().isPresent()
                withoutEmail++;
            }
        }
        show("klienci bez e-maila", withoutEmail);
        // WYNIK: klienci bez e-maila → 2

        // UWAGA: isPresent/isEmpty są w porządku, gdy interesuje Cię TYLKO odpowiedź tak/nie.
        // PUŁAPKA: para  if (opt.isPresent()) { ... opt.get() ... }  to zwykle „null-check w przebraniu” —
        //   działa, ale traci zalety Optional. Lepsze zamienniki: orElse, ifPresent, map (Optional03BestPractices).
    }

    // =================================================================================================
    // 5. WYJMOWANIE WARTOŚCI: get i orElse
    // =================================================================================================

    /**
     * 5. get() zwraca wartość albo rzuca NoSuchElementException (no such element = nie ma takiego elementu).
     * orElse(domyślna) zwraca wartość albo podaną wartość domyślną — nigdy nie rzuca.
     */
    static void getAndOrElse() {
        section("5. Wyjmowanie wartości: pułapka get() i bezpieczne orElse");

        Customer jan = SampleData.customers().get(0);                  // ma e-mail
        Customer maria = SampleData.customers().get(1);                // NIE ma e-maila

        show("e-mail Jana przez get()", jan.findEmail().get());
        // WYNIK: e-mail Jana przez get() → jan@example.com

        expectThrows("get() na pustym Optional", () -> maria.findEmail().get());
        // WYNIK: ✔ get() na pustym Optional → rzucono NoSuchElementException: No value present
        // Tłumaczenie komunikatu: „brak wartości”.

        // PUŁAPKA: get() bez sprawdzenia to ten sam błąd co wywołanie metody na null — tylko wyjątek ma inną
        //   nazwę. Zamieniliśmy NullPointerException na NoSuchElementException i nic nie zyskaliśmy.
        //   Dlatego w Javie 10 dodano orElseThrow() — robi to samo co get(), ale nazwa ostrzega (sekcja 7).

        // orElse = „albo”: wartość z pudełka, a gdy pudełko puste — wartość domyślna.
        show("e-mail Jana albo domyślny", jan.findEmail().orElse("(brak e-maila)"));
        show("e-mail Marii albo domyślny", maria.findEmail().orElse("(brak e-maila)"));
        // WYNIK: e-mail Jana albo domyślny → jan@example.com
        // WYNIK: e-mail Marii albo domyślny → (brak e-maila)

        // Wartością domyślną może być cokolwiek tego samego typu, np. adres ogólny firmy:
        String recipient = maria.findEmail().orElse("biuro@sklep.pl");
        show("do kogo wysłać fakturę Marii", recipient);
        // WYNIK: do kogo wysłać fakturę Marii → biuro@sklep.pl
    }

    // =================================================================================================
    // 6. orElse KONTRA orElseGet — LENIWOŚĆ
    // =================================================================================================

    /**
     * 6. Najważniejsza różnica w tej lekcji: argument orElse jest obliczany ZAWSZE — także wtedy,
     * gdy pudełko jest pełne i wartość domyślna do niczego się nie przyda.
     * orElseGet przyjmuje {@code Supplier} (dostawcę — lambdę bez argumentów) i uruchamia go TYLKO dla pustego pudełka.
     * To tzw. leniwe obliczanie (lazy = leniwy): „policz dopiero wtedy, gdy naprawdę trzeba”.
     * <p>
     * Liczymy wywołania w polu statycznym defaultCalls. Lambda może zmieniać POLA (także statyczne);
     * zakaz zmiany dotyczy tylko zmiennych LOKALNYCH (t13_lambdas/Lambda06ClosuresScope).
     */
    static void orElseVsOrElseGet() {
        section("6. orElse kontra orElseGet — kiedy liczy się wartość domyślna?");

        Customer jan = SampleData.customers().get(0);                  // ma e-mail
        Customer maria = SampleData.customers().get(1);                // nie ma

        defaultCalls = 0;
        String r1 = jan.findEmail().orElse(loadDefaultEmail());
        show("orElse — Jan (pudełko PEŁNE)", r1);
        show("   wywołań loadDefaultEmail", defaultCalls);
        // WYNIK: ⚙ loadDefaultEmail: pytam bazę danych o adres domyślny...
        // WYNIK: orElse — Jan (pudełko PEŁNE) → jan@example.com
        // WYNIK: wywołań loadDefaultEmail → 1    ← praca wykonana na darmo!

        defaultCalls = 0;
        String r2 = jan.findEmail().orElseGet(() -> loadDefaultEmail());
        show("orElseGet — Jan (pudełko PEŁNE)", r2);
        show("   wywołań loadDefaultEmail", defaultCalls);
        // WYNIK: orElseGet — Jan (pudełko PEŁNE) → jan@example.com
        // WYNIK: wywołań loadDefaultEmail → 0    ← lambda w ogóle się nie uruchomiła

        defaultCalls = 0;
        String r3 = maria.findEmail().orElseGet(Optional01Basics::loadDefaultEmail);   // referencja do metody
        show("orElseGet — Maria (pudełko PUSTE)", r3);
        show("   wywołań loadDefaultEmail", defaultCalls);
        // WYNIK: ⚙ loadDefaultEmail: pytam bazę danych o adres domyślny...
        // WYNIK: orElseGet — Maria (pudełko PUSTE) → kontakt@sklep.pl
        // WYNIK: wywołań loadDefaultEmail → 1    ← tym razem wartość domyślna była potrzebna

        // Dlaczego orElse liczy zawsze? To zwykła metoda Javy: ZANIM zostanie wywołana, Java oblicza
        // wszystkie jej argumenty. orElse(loadDefaultEmail()) — najpierw wykona się loadDefaultEmail(),
        // dopiero potem orElse dostanie gotowy wynik. Do orElseGet przekazujemy przepis (lambdę), a nie wynik.

        // PUŁAPKA: orElse(createNewAccount()) — jeśli metoda ma efekt uboczny (zapis do bazy, wysłanie maila),
        //   wykona się ZAWSZE, nawet gdy wartość była w pudełku. Takie błędy trudno zauważyć.
        // DOBRA PRAKTYKA: orElse — dla gotowych, tanich wartości (stała, literał, zmienna);
        //   orElseGet — gdy wartość domyślną trzeba OBLICZYĆ (metoda, new, zapytanie do bazy).
    }

    /**
     * loadDefaultEmail = wczytaj domyślny e-mail. Udaje „kosztowną” operację (np. zapytanie do bazy):
     * wypisuje komunikat i zwiększa licznik. Wypisywanie to efekt uboczny — tu celowo, żeby go zobaczyć.
     */
    private static String loadDefaultEmail() {
        defaultCalls++;
        System.out.println("   ⚙ loadDefaultEmail: pytam bazę danych o adres domyślny...");
        return "kontakt@sklep.pl";
    }

    // =================================================================================================
    // 7. orElseThrow() I orElseThrow(supplier)
    // =================================================================================================

    /**
     * 7. orElseThrow — „daj wartość albo rzuć wyjątek”.
     * <ul>
     *   <li>{@code orElseThrow()} (Java 10+) — rzuca NoSuchElementException. Działa jak get(), ale nazwa uczciwie
     *       mówi, co się stanie przy pustym pudełku. Używaj jej zamiast get().</li>
     *   <li>{@code orElseThrow(() -> new TwójWyjątek(...))} (Java 8+) — rzuca wyjątek, który sam wybierzesz,
     *       z czytelnym komunikatem. Wyjątek jest tworzony TYLKO dla pustego pudełka (to też Supplier).</li>
     * </ul>
     * Kiedy rzucać, a kiedy podać wartość domyślną? Gdy brak wartości to BŁĄD (np. zamówienie bez klienta) —
     * rzucaj. Gdy brak wartości to normalna sytuacja (klient bez e-maila) — orElse / orElseGet / ifPresent.
     */
    static void orElseThrowVariants() {
        section("7. orElseThrow() (Java 10+) i orElseThrow(supplier)");

        Customer jan = SampleData.customers().get(0);
        Customer maria = SampleData.customers().get(1);

        show("orElseThrow() — Jan", jan.findEmail().orElseThrow());
        // WYNIK: orElseThrow() — Jan → jan@example.com

        expectThrows("orElseThrow() — Maria", () -> maria.findEmail().orElseThrow());
        // WYNIK: ✔ orElseThrow() — Maria → rzucono NoSuchElementException: No value present

        expectThrows("orElseThrow(supplier) — Maria", () -> maria.findEmail()
                .orElseThrow(() -> new IllegalStateException("Klient " + maria.name() + " nie ma e-maila")));
        // WYNIK: ✔ orElseThrow(supplier) — Maria → rzucono IllegalStateException: Klient Maria Nowak nie ma e-maila
        // (IllegalState = niedozwolony stan). Taki komunikat od razu mówi, KTO i CZEGO nie ma — łatwo znaleźć błąd.

        // orElseThrow(supplier) działa także z wyjątkami SPRAWDZANYMI (checked). Metoda musi je wtedy
        // zadeklarować w throws — zobacz requireEmail niżej. Sygnatura w JDK:
        //   <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X
        expectThrows("requireEmail(Maria) — wyjątek sprawdzany", () -> requireEmail(maria));
        // WYNIK: ✔ requireEmail(Maria) — wyjątek sprawdzany → rzucono MissingEmailException: Brak e-maila: Maria Nowak

        // DOBRA PRAKTYKA: zamiast get() pisz orElseThrow() — gdy wiesz, że wartość MUSI być,
        //   albo orElseThrow(() -> new ...(komunikat)) — gdy chcesz dokładnie opisać błąd.
    }

    /**
     * MissingEmailException = wyjątek „brak e-maila”. Dziedziczy po Exception, więc jest SPRAWDZANY (checked):
     * kompilator wymaga try/catch albo throws (t10_exceptions/Exceptions02CheckedUnchecked).
     */
    static class MissingEmailException extends Exception {
        // serialVersionUID = numer wersji do serializacji. Każdy wyjątek jest Serializable, a kompilator
        // z włączonymi ostrzeżeniami prosi o to pole (t18_io_files/Io09Serialization).
        private static final long serialVersionUID = 1L;

        MissingEmailException(String message) {
            super(message);                                            // message = komunikat
        }
    }

    /** requireEmail = wymagaj e-maila. Zwraca e-mail albo rzuca wyjątek sprawdzany MissingEmailException. */
    static String requireEmail(Customer customer) throws MissingEmailException {
        return customer.findEmail()
                .orElseThrow(() -> new MissingEmailException("Brak e-maila: " + customer.name()));
    }

    // =================================================================================================
    // 8. ifPresent I ifPresentOrElse
    // =================================================================================================

    /**
     * 8. Czasem nie potrzebujesz wartości „na zewnątrz” — chcesz tylko COŚ ZROBIĆ, jeśli ona jest.
     * <ul>
     *   <li>{@code ifPresent(akcja)} — wykonaj akcję (Consumer) z wartością; dla pustego pudełka nic nie rób.</li>
     *   <li>{@code ifPresentOrElse(akcja, akcjaGdyPusty)} (Java 9+) — dwie gałęzie jak if/else.</li>
     * </ul>
     * Consumer (konsument) = lambda, która coś przyjmuje i nic nie zwraca; Runnable = lambda bez argumentów i wyniku.
     */
    static void ifPresentVariants() {
        section("8. ifPresent i ifPresentOrElse (Java 9+) — zrób coś, gdy wartość jest");

        Customer jan = SampleData.customers().get(0);
        Customer maria = SampleData.customers().get(1);

        jan.findEmail().ifPresent(email -> System.out.println("   Jan: wysyłam potwierdzenie na " + email));
        maria.findEmail().ifPresent(email -> System.out.println("   Maria: wysyłam potwierdzenie na " + email));
        // WYNIK: Jan: wysyłam potwierdzenie na jan@example.com
        // Dla Marii nie wypisało się NIC — pudełko puste, akcja pominięta.

        for (Customer c : SampleData.customers()) {
            c.findEmail().ifPresentOrElse(
                    email -> System.out.println("   newsletter → " + email),         // gdy e-mail jest
                    () -> System.out.println("   brak e-maila → dzwonimy: " + c.name()));  // gdy go nie ma
        }
        // WYNIK: newsletter → jan@example.com
        // WYNIK: brak e-maila → dzwonimy: Maria Nowak
        // WYNIK: newsletter → adam.mazur@example.com
        // WYNIK: newsletter → zofia@example.com
        // WYNIK: brak e-maila → dzwonimy: Ola Pawlak
        // WYNIK: newsletter → marek@example.com
        // WYNIK: newsletter → ewa.lis@example.com

        // Referencja do metody też działa: dodaj każdy znaleziony e-mail do listy.
        List<String> emails = new ArrayList<>();
        for (Customer c : SampleData.customers()) {
            c.findEmail().ifPresent(emails::add);          // emails::add = e -> emails.add(e)
        }
        show("zebrane e-maile", emails.size() + " " + emails);
        // WYNIK: zebrane e-maile → 5 [jan@example.com, adam.mazur@example.com, zofia@example.com, marek@example.com, ewa.lis@example.com]

        // DOBRA PRAKTYKA: ifPresent zamiast  if (opt.isPresent()) { zrób(opt.get()); }  — krócej i bez get().
    }

    // =================================================================================================
    // 9. OptionalInt I OptionalDouble
    // =================================================================================================

    /**
     * 9. Dla liczb prostych są osobne klasy: OptionalInt, OptionalLong, OptionalDouble.
     * Nie opakowują liczby w obiekt Integer/Double (boxing = pakowanie), więc są lżejsze.
     * Wartość wyjmuje się przez getAsInt() / getAsDouble() (get as int = pobierz jako int), a domyślną daje orElse.
     * <p>
     * Student ma metodę {@code averageGrade()} zwracającą OptionalDouble: Henryk nie ma ocen, więc średniej NIE MA.
     * <p>
     * UWAGA: te klasy są uboższe od {@code Optional<T>} — nie mają map, filter ani flatMap.
     * Zwracają je np. operacje na strumieniach liczb: max(), min(), average() (t16_streams/Streams08PrimitiveStreams).
     */
    static void primitiveOptionals() {
        section("9. OptionalInt i OptionalDouble — wersje dla liczb");

        Student ala = SampleData.students().get(0);                    // oceny [5, 4, 5, 3]
        Student henryk = SampleData.students().get(7);                 // oceny [] — brak ocen

        OptionalDouble alaAverage = ala.averageGrade();                // averageGrade = średnia ocen
        show("średnia Ali", alaAverage);
        show("średnia Henryka", henryk.averageGrade());
        // WYNIK: średnia Ali → OptionalDouble[4.25]
        // WYNIK: średnia Henryka → OptionalDouble.empty

        show("getAsDouble — Ala", alaAverage.getAsDouble());
        // WYNIK: getAsDouble — Ala → 4.25
        expectThrows("getAsDouble — Henryk", () -> henryk.averageGrade().getAsDouble());
        // WYNIK: ✔ getAsDouble — Henryk → rzucono NoSuchElementException: No value present

        // PUŁAPKA: orElse(0.0) dla średniej ocen robi z Henryka najgorszego studenta —
        //   jego „0.0” wygląda gorzej niż 2.5 Darka, który ma same dwójki i trójki. A Henryk po prostu nie ma ocen!
        show("Henryk z orElse(0.0)", henryk.averageGrade().orElse(0.0));
        // WYNIK: Henryk z orElse(0.0) → 0.0    ← zero ≠ brak wartości

        // Lepiej: o wyświetleniu braku decyduje ten, kto pokazuje wynik.
        for (Student s : SampleData.students()) {
            OptionalDouble avg = s.averageGrade();
            String text = avg.isPresent()
                    ? String.format(Locale.ROOT, "%.2f", avg.getAsDouble())   // Locale.ROOT = kropka jako separator
                    : "brak ocen";
            System.out.println("   " + s.name() + ": " + text);
        }
        // WYNIK: Ala: 4.25
        // WYNIK: Bartek: 3.00
        // WYNIK: Celina: 4.75
        // WYNIK: Darek: 2.50
        // WYNIK: Ela: 4.50
        // WYNIK: Filip: 3.50
        // WYNIK: Gosia: 4.50
        // WYNIK: Henryk: brak ocen
        // (Tu isPresent + getAsDouble jest w porządku: OptionalDouble nie ma map, więc nie ma lepszej drogi.)

        // OptionalInt — tworzymy go sami w metodzie bestGrade (najlepsza ocena):
        show("najlepsza ocena Ali", bestGrade(ala));
        show("najlepsza ocena Henryka", bestGrade(henryk));
        show("najlepsza ocena Henryka albo -1", bestGrade(henryk).orElse(-1));
        // WYNIK: najlepsza ocena Ali → OptionalInt[5]
        // WYNIK: najlepsza ocena Henryka → OptionalInt.empty
        // WYNIK: najlepsza ocena Henryka albo -1 → -1
        // DOBRA PRAKTYKA: -1 jako „brak” to stara konwencja (jak indexOf). Zwracając OptionalInt, nie musisz jej
        //   znać ani pamiętać — typ sam mówi, że wyniku może nie być.
    }

    /** bestGrade = najlepsza ocena. Pętla szuka maksimum; dla pustej listy zwraca OptionalInt.empty(). */
    static OptionalInt bestGrade(Student student) {
        if (student.grades().isEmpty()) {
            return OptionalInt.empty();
        }
        int best = Integer.MIN_VALUE;                                 // MIN_VALUE = najmniejszy możliwy int
        for (int grade : student.grades()) {
            best = Math.max(best, grade);
        }
        return OptionalInt.of(best);
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   TWORZENIE:    Optional.of(v)  — v na pewno nie null (null → NullPointerException od razu)
     *                 Optional.ofNullable(v) — null → puste pudełko;   Optional.empty() — puste pudełko
     *   SPRAWDZANIE:  isPresent() / isEmpty() (Java 11+) — tylko gdy potrzebujesz odpowiedzi tak/nie
     *   WYJMOWANIE:   orElse(x)            — wartość albo x (x liczone ZAWSZE)
     *                 orElseGet(() -> x)   — wartość albo x (x liczone TYLKO dla pustego) — do obliczeń
     *                 orElseThrow()        — wartość albo NoSuchElementException (Java 10+, zamiast get())
     *                 orElseThrow(() -> new XxxException("..."))  — wartość albo Twój wyjątek
     *                 get()                — jak orElseThrow(), ale nazwa nie ostrzega — unikaj
     *   AKCJE:        ifPresent(v -> ...)  /  ifPresentOrElse(v -> ..., () -> ...) (Java 9+)
     *   LICZBY:       OptionalInt / OptionalLong / OptionalDouble — getAsInt(), getAsDouble(), orElse(...);
     *                 bez map/filter; zero ≠ brak wartości!
     *   toString:     Optional[wartość]  /  Optional.empty  /  OptionalDouble[4.25]  /  OptionalInt.empty
     *
     * PYTANIA KONTROLNE:
     *   1. Jaką informację niesie typ zwracany Optional<String>, której nie niesie zwykły String?
     *   2. Czym różni się Optional.of(v) od Optional.ofNullable(v)? Co się stanie przy Optional.of(null)?
     *   3. Co wypisze ten kod?
     *          static String fallback() { System.out.print("F "); return "x"; }
     *          System.out.println(Optional.of("a").orElse(fallback()));
     *          System.out.println(Optional.of("a").orElseGet(() -> fallback()));
     *   4. ZNAJDŹ BŁĄD:
     *          Optional<String> email = customer.findEmail();
     *          if (email != null) {
     *              send(email.get());
     *          }
     *   5. Kiedy użyjesz orElseThrow(), a kiedy orElseThrow(() -> new ...)?
     *   6. Dlaczego averageGrade() zwraca OptionalDouble, a nie po prostu 0.0 dla studenta bez ocen?
     *   7. Co wypisze:  System.out.println(Optional.ofNullable(null) + " " + Optional.of(""));  ?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /**
     * Uruchamia ćwiczenia: najpierw TWOJE rozwiązania (✘ dopóki nie uzupełnisz), potem wzorcowe (✔).
     * Check porównuje Optionale przez equals: {@code Optional.of("x").equals(Optional.of("x"))} daje true.
     */
    static void exercises() {
        Customer jan = SampleData.customers().get(0);
        Customer maria = SampleData.customers().get(1);
        Student ala = SampleData.students().get(0);
        Student henryk = SampleData.students().get(7);
        Map<String, String> phones = new LinkedHashMap<>();
        phones.put("Anna", "600-100-200");
        phones.put("Piotr", "601-300-400");
        List<String> expected4 = List.of("jan@example.com", "adam.mazur@example.com", "zofia@example.com",
                "marek@example.com", "ewa.lis@example.com");

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: e-mail Jana albo komunikat", "jan@example.com", () -> exercise1(jan));
        Check.equal("ćw. 1b: e-mail Marii albo komunikat", "brak e-maila", () -> exercise1(maria));
        Check.equal("ćw. 2a: telefon Piotra jako Optional", Optional.of("601-300-400"), () -> exercise2(phones, "Piotr"));
        Check.equal("ćw. 2b: telefon Oli jako Optional", Optional.empty(), () -> exercise2(phones, "Ola"));
        Check.equal("ćw. 3a: średnia Ali jako tekst", "Ala: 4.25", () -> exercise3(ala));
        Check.equal("ćw. 3b: średnia Henryka jako tekst", "Henryk: brak ocen", () -> exercise3(henryk));
        Check.equal("ćw. 4: pętla + ifPresent — wszystkie e-maile", expected4, () -> exercise4(SampleData.customers()));
        Check.equal("ćw. 5a: e-mail albo wyjątek — Jan", "jan@example.com", () -> exercise5(jan));
        Check.equal("ćw. 5b: e-mail albo wyjątek — Maria", "IllegalStateException: Brak e-maila: Maria Nowak",
                () -> messageOf(() -> exercise5(maria)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", "jan@example.com", () -> solution1(jan));
        Check.equal("ćw. 1b (wzorzec)", "brak e-maila", () -> solution1(maria));
        Check.equal("ćw. 2a (wzorzec)", Optional.of("601-300-400"), () -> solution2(phones, "Piotr"));
        Check.equal("ćw. 2b (wzorzec)", Optional.empty(), () -> solution2(phones, "Ola"));
        Check.equal("ćw. 3a (wzorzec)", "Ala: 4.25", () -> solution3(ala));
        Check.equal("ćw. 3b (wzorzec)", "Henryk: brak ocen", () -> solution3(henryk));
        Check.equal("ćw. 4 (wzorzec)", expected4, () -> solution4(SampleData.customers()));
        Check.equal("ćw. 5a (wzorzec)", "jan@example.com", () -> solution5(jan));
        Check.equal("ćw. 5b (wzorzec)", "IllegalStateException: Brak e-maila: Maria Nowak",
                () -> messageOf(() -> solution5(maria)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 9 OK, ✘ 0 BŁĄD
    }

    /**
     * messageOf = komunikat z (wyjątku). Uruchamia kod i zwraca „TypWyjątku: komunikat”,
     * a gdy wyjątku nie było — „brak wyjątku”. Pomocnik do sprawdzania ćwiczenia 5.
     */
    private static String messageOf(Supplier<?> code) {
        try {
            code.get();
            return "brak wyjątku";
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć e-mail klienta, a gdy go nie ma — napis "brak e-maila".
     * Podpowiedź: customer.findEmail() → orElse(...). Jedna linijka.
     */
    static String exercise1(Customer customer) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć numer telefonu osoby z mapy jako Optional — pusty, gdy osoby nie ma w mapie.
     * Podpowiedź: Map.get zwraca null dla brakującego klucza. Który sposób tworzenia Optional zamienia null
     * w puste pudełko, a który rzuciłby wyjątek?
     */
    static Optional<String> exercise2(Map<String, String> phones, String name) {
        // TODO: twoje rozwiązanie
        return Optional.empty();
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć napis „Imię: średnia” ze średnią sformatowaną do 2 miejsc po kropce
     * (np. "Ala: 4.25"), a dla studenta bez ocen „Imię: brak ocen”.
     * Podpowiedź: averageGrade() zwraca OptionalDouble — sprawdź isPresent(), wartość weź przez getAsDouble().
     * Formatowanie: {@code String.format(Locale.ROOT, "%.2f", liczba)} (Locale.ROOT daje kropkę, a nie przecinek).
     */
    static String exercise3(Student student) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 4 (średnie, łączy Optional z kolekcjami i referencjami do metod): zwróć listę e-maili
     * wszystkich klientów, pomijając tych bez e-maila. Kolejność jak na liście klientów.
     * Podpowiedź: nowa ArrayList, pętla for-each po klientach, a w środku findEmail().ifPresent(...).
     * Spróbuj użyć referencji do metody {@code lista::add} zamiast lambdy.
     */
    static List<String> exercise4(List<Customer> customers) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): zwróć e-mail klienta. Gdy go nie ma, rzuć IllegalStateException
     * z komunikatem „Brak e-maila: Imię Nazwisko” (np. "Brak e-maila: Maria Nowak").
     * Podpowiedź: orElseThrow z lambdą tworzącą wyjątek: {@code () -> new IllegalStateException(...)}.
     * Wyjątek ma powstać TYLKO wtedy, gdy e-maila nie ma (dlatego lambda, a nie gotowy obiekt).
     */
    static String exercise5(Customer customer) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(Customer customer) {
        return customer.findEmail().orElse("brak e-maila");      // "brak e-maila" to gotowy literał → orElse wystarczy
    }

    static Optional<String> solution2(Map<String, String> phones, String name) {
        return Optional.ofNullable(phones.get(name));            // of(...) rzuciłby NullPointerException dla "Ola"
    }

    static String solution3(Student student) {
        OptionalDouble average = student.averageGrade();
        if (average.isPresent()) {                               // OptionalDouble nie ma map — isPresent jest OK
            return student.name() + ": " + String.format(Locale.ROOT, "%.2f", average.getAsDouble());
        }
        return student.name() + ": brak ocen";
    }

    static List<String> solution4(List<Customer> customers) {
        List<String> result = new ArrayList<>();
        for (Customer c : customers) {
            c.findEmail().ifPresent(result::add);                // dodaj tylko, gdy e-mail jest
        }
        return result;
    }

    static String solution5(Customer customer) {
        return customer.findEmail()
                .orElseThrow(() -> new IllegalStateException("Brak e-maila: " + customer.name()));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Że wyniku może NIE BYĆ. Kompilator zmusza do obsługi pustego przypadku (Optional nie ma metod Stringa),
     *      a zwykły String może być null bez żadnego ostrzeżenia.
     *   2. of zakłada, że wartość nie jest null — dla null od razu rzuca NullPointerException (fail fast).
     *      ofNullable dla null zwraca puste pudełko (Optional.empty).
     *   3. „F a”, a w drugiej linii samo „a”. Argument orElse jest liczony zawsze (więc fallback() wypisał „F ”),
     *      lambda w orElseGet uruchamia się tylko dla pustego pudełka — tu pudełko jest pełne.
     *   4. findEmail() nigdy nie zwraca null, więc warunek email != null jest ZAWSZE prawdziwy, a get() dla
     *      pustego pudełka rzuci NoSuchElementException. Poprawnie: customer.findEmail().ifPresent(e -> send(e));
     *   5. orElseThrow() — gdy wartość MUSI być i brak to błąd programisty (wystarczy NoSuchElementException).
     *      orElseThrow(supplier) — gdy chcesz konkretny typ wyjątku i komunikat opisujący sytuację
     *      (np. IllegalStateException("Klient 7 nie ma e-maila")) albo wyjątek sprawdzany.
     *   6. Bo 0.0 to prawdziwa (bardzo zła) średnia, a student bez ocen średniej w ogóle NIE MA. Zero myliłoby się
     *      z wynikiem, a o wyświetleniu braku („brak ocen”, „—”) powinien decydować ten, kto pokazuje wynik.
     *   7. „Optional.empty Optional[]” — ofNullable(null) daje puste pudełko, a pusty napis "" to normalna wartość
     *      (nie null!), więc of("") tworzy pełne pudełko z pustym napisem.
     */
    // </editor-fold>
}
