package t14_optional;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Customer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Optional — dobre praktyki. Gdzie używać, a gdzie NIE
 *        (best practices = dobre praktyki; code smell = „zapach kodu”, sygnał, że coś jest nie tak)
 *
 * W SKRÓCIE:
 *   Optional zaprojektowano do jednego zadania: jako TYP ZWRACANY metody, która może nie mieć wyniku.
 *   Nie wkładaj go do pól, parametrów ani kolekcji. Metoda zwracająca Optional nigdy nie zwraca null,
 *   a metoda zwracająca listę zwraca pustą listę zamiast Optional albo null.
 *
 * ANALOGIA: pudełko z paczkomatu jest świetne do ODBIERANIA przesyłek.
 *   • Nikt jednak nie trzyma w pudełku z paczkomatu swoich rzeczy w szafie (pole klasy),
 *     nie wysyła komuś pustego pudełka „na wszelki wypadek” (parametr), nie pakuje pudełek do pudełek
 *     (Optional w kolekcji) i nie nosi każdej kartki w osobnym kartonie (Optional zamiast zwykłego if).
 *   • Najgorsze, co może się zdarzyć: kurier zamiast pudełka (nawet pustego) nie przynosi NIC
 *     (metoda zwracająca Optional zwraca null) — wtedy całe zaufanie do paczkomatu przepada.
 *
 * JAK TO DZIAŁA: zasady w pigułce
 *   GDZIE                         TAK / NIE        ZAMIAST TEGO
 *   typ zwracany „może nie być”   TAK              —
 *   typ zwracany listy/mapy       NIE              pusta kolekcja: List.of(), new ArrayList()
 *   pole klasy                    NIE              pole może być null + getter zwraca Optional.ofNullable(pole)
 *   parametr metody               NIE              przeciążenie metody albo parametr, który może być null
 *   element kolekcji              NIE              wrzucaj tylko obecne wartości
 *   return null w metodzie        NIGDY            return Optional.empty()
 *   isPresent() + get()           rzadko           map / orElse / orElseGet / ifPresent / orElseThrow
 *
 * SŁÓWKA:
 *   best practice = dobra praktyka; repository = repozytorium (magazyn obiektów); find by id = znajdź po id;
 *   get by id = pobierz po id (musi być); field = pole; parameter = parametr; overload = przeciążenie;
 *   legacy = stary kod (odziedziczony); smell = zapach; allocation = przydział pamięci (tworzenie obiektu);
 *   nickname = pseudonim; display name = nazwa wyświetlana; greet = przywitaj; serializable = dający się zapisać.
 *
 * ZOBACZ TEŻ: t14_optional/Optional01Basics (orElse, orElseGet, orElseThrow),
 *             t14_optional/Optional02Transform (map, flatMap, filter, or),
 *             t11_generics/Generics07Repository (repozytorium generyczne),
 *             t10_exceptions/Exceptions07BestPractices (kiedy rzucać wyjątek, a kiedy zwrócić „brak”).
 * </pre>
 */
public class Optional03BestPractices {

    public static void main(String[] args) {
        title("Optional03 — dobre praktyki: gdzie używać Optional, a gdzie nie");

        returnTypeOnly();           // return type only = tylko jako typ zwracany
        neverReturnNull();          // never return null = nigdy nie zwracaj null
        collectionsNotOptional();   // collections not optional = kolekcje zamiast Optional
        notAsField();               // not as field = nie jako pole
        notAsParameter();           // not as parameter = nie jako parametr
        isPresentGetRefactor();     // isPresent + get refactor = przeróbka isPresent + get
        ofVsOfNullable();           // of vs ofNullable = of kontra ofNullable
        orElseNullSmell();          // orElse(null) smell = zapach orElse(null)
        costAndOveruse();           // cost and overuse = koszt i nadużywanie
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. OPTIONAL TO TYP ZWRACANY — REPOZYTORIUM W PAMIĘCI
    // =================================================================================================

    /**
     * 1. Twórcy Javy (Brian Goetz) opisali Optional jako „ograniczony mechanizm dla TYPÓW ZWRACANYCH
     * metod bibliotecznych, gdy trzeba jasno pokazać, że wyniku może nie być”.
     * <p>
     * Klasyczny przykład: repozytorium (magazyn obiektów). Konwencja nazw, którą spotkasz w wielu bibliotekach:
     * <ul>
     *   <li>{@code findById} — „szukam, mogę nie znaleźć” → zwraca Optional,</li>
     *   <li>{@code getById} — „na pewno jest” → zwraca obiekt albo rzuca wyjątek.</li>
     * </ul>
     */
    static void returnTypeOnly() {
        section("1. Optional to typ ZWRACANY — repozytorium w pamięci");

        CustomerRepository repo = new CustomerRepository(SampleData.customers());   // repository = repozytorium
        show("findById(4)", repo.findById(4));                        // find by id = znajdź po id
        show("findById(42)", repo.findById(42));
        // WYNIK: findById(4) → Optional[Zofia Krawczyk (Warszawa, VIP)]
        // WYNIK: findById(42) → Optional.empty

        show("getById(4)", repo.getById(4));                          // get by id = pobierz po id
        expectThrows("getById(42)", () -> repo.getById(42));
        // WYNIK: getById(4) → Zofia Krawczyk (Warszawa, VIP)
        // WYNIK: ✔ getById(42) → rzucono NoSuchElementException: Brak klienta o id 42

        // Kiedy która? findById — gdy brak wyniku to NORMALNA sytuacja (użytkownik wpisał zły numer).
        // getById — gdy brak to BŁĄD (id pochodzi z zamówienia, więc klient MUSI istnieć).
        // DOBRA PRAKTYKA: getById buduje się z findById:  findById(id).orElseThrow(() -> new ...).
        //   Jedna metoda szuka, druga tylko decyduje, co zrobić z pustym pudełkiem.
    }

    /**
     * CustomerRepository = repozytorium klientów trzymane w pamięci (w mapie id → klient).
     * Pokazuje trzy dobre typy zwracane: Optional, obiekt-albo-wyjątek oraz lista (pusta, gdy nic nie ma).
     */
    static final class CustomerRepository {
        private final Map<Long, Customer> byId = new LinkedHashMap<>();   // byId = według id

        CustomerRepository(List<Customer> customers) {
            for (Customer c : customers) {
                byId.put(c.id(), c);
            }
        }

        /** Może nie znaleźć → Optional. Map.get zwraca null, więc ofNullable. */
        Optional<Customer> findById(long id) {
            return Optional.ofNullable(byId.get(id));
        }

        /** Musi znaleźć → obiekt albo wyjątek z czytelnym komunikatem. */
        Customer getById(long id) {
            return findById(id).orElseThrow(() -> new NoSuchElementException("Brak klienta o id " + id));
        }

        /** Wielu wyników → lista. Brak wyników → PUSTA lista, nigdy null ani Optional. */
        List<Customer> findByCity(String city) {
            List<Customer> result = new ArrayList<>();
            for (Customer c : byId.values()) {
                if (c.city().equals(city)) {
                    result.add(c);
                }
            }
            return result;
        }
    }

    // =================================================================================================
    // 2. NIGDY NIE ZWRACAJ null ZAMIAST Optional
    // =================================================================================================

    /**
     * 2. Kto widzi typ zwracany {@code Optional<Customer>}, ten ufa, że dostanie pudełko — pełne albo puste.
     * Nie sprawdza już null. Metoda, która mimo to zwraca null, łamie tę umowę i wywołuje NullPointerException
     * w miejscu, w którym nikt się go nie spodziewa.
     */
    static void neverReturnNull() {
        section("2. Nigdy nie zwracaj null zamiast Optional");

        show("findByIdBroken(1)", findByIdBroken(1));
        // WYNIK: findByIdBroken(1) → Optional[Jan Kowalski (Warszawa, VIP)]

        expectThrows("findByIdBroken(42).isPresent()", () -> findByIdBroken(42).isPresent());
        // WYNIK: ✔ findByIdBroken(42).isPresent() → rzucono NullPointerException: Cannot invoke "java.util.Optional.isPresent()" because the return value of "t14_optional.Optional03BestPractices.findByIdBroken(long)" is null
        // Tłumaczenie: „nie można wywołać isPresent(), bo wartość zwrócona przez findByIdBroken(long) jest null”.
        // (Tak szczegółowe komunikaty NPE to helpful NPE — Java 14+.)

        // PUŁAPKA: to najgorszy możliwy błąd z Optional — łączy wady obu światów. Wywołujący musiałby
        //   sprawdzać i null, i isPresent(). Kompilator Ci tu NIE pomoże: return null kompiluje się bez ostrzeżenia.
        // DOBRA PRAKTYKA: w metodzie zwracającej Optional każda ścieżka kończy się
        //   return Optional.of(...) / Optional.ofNullable(...) / Optional.empty().
    }

    /** findByIdBroken = ZEPSUTA wersja wyszukiwania: dla braku wyniku zwraca null. NIE RÓB TAK! */
    static Optional<Customer> findByIdBroken(long id) {
        for (Customer c : SampleData.customers()) {
            if (c.id() == id) {
                return Optional.of(c);
            }
        }
        return null;                                                  // BŁĄD: powinno być Optional.empty()
    }

    // =================================================================================================
    // 3. KOLEKCJE: PUSTA LISTA ZAMIAST Optional<List> I null
    // =================================================================================================

    /**
     * 3. Lista ma już WBUDOWANY sposób na „nic nie ma” — jest pusta. Opakowanie jej w Optional dodaje
     * drugi, zbędny sposób. Podobnie Optional jako element listy: lepiej po prostu nie wkładać braków.
     */
    static void collectionsNotOptional() {
        section("3. Kolekcje: pusta lista zamiast Optional<List> i null");

        CustomerRepository repo = new CustomerRepository(SampleData.customers());
        show("findByCity(\"Kraków\")", repo.findByCity("Kraków"));
        show("findByCity(\"Łódź\")", repo.findByCity("Łódź"));
        // WYNIK: findByCity("Kraków") → [Maria Nowak (Kraków), Marek Król (Kraków)]
        // WYNIK: findByCity("Łódź") → []

        // Pusta lista nie wymaga ŻADNEGO sprawdzania — pętla po prostu wykona się 0 razy:
        int count = 0;
        for (Customer c : repo.findByCity("Łódź")) {
            count++;
        }
        show("obrotów pętli dla Łodzi", count);
        // WYNIK: obrotów pętli dla Łodzi → 0

        // PUŁAPKA: Optional<List<Customer>> ma DWA różne „nic”, a wywołujący musi obsłużyć oba:
        Optional<List<Customer>> nothing1 = Optional.empty();
        Optional<List<Customer>> nothing2 = Optional.of(List.of());
        show("Optional.empty()", nothing1);
        show("Optional.of(List.of())", nothing2);
        // WYNIK: Optional.empty() → Optional.empty
        // WYNIK: Optional.of(List.of()) → Optional[[]]    ← co to znaczy? „brak listy” czy „pusta lista”?

        // PUŁAPKA: lista Optionali — każdy odczyt wymaga rozpakowania, a wydruk jest nieczytelny.
        List<Optional<String>> badEmails = new ArrayList<>();
        List<String> goodEmails = new ArrayList<>();
        for (Customer c : SampleData.customers().subList(0, 3)) {
            badEmails.add(c.findEmail());                             // ŹLE: pudełka w liście
            c.findEmail().ifPresent(goodEmails::add);                 // DOBRZE: tylko obecne wartości
        }
        show("List<Optional<String>>", badEmails);
        show("List<String>", goodEmails);
        // WYNIK: List<Optional<String>> → [Optional[jan@example.com], Optional.empty, Optional[adam.mazur@example.com]]
        // WYNIK: List<String> → [jan@example.com, adam.mazur@example.com]

        // DOBRA PRAKTYKA: metoda zwracająca kolekcję zwraca pustą kolekcję (List.of(), new ArrayList<>()),
        //   nigdy null i nigdy Optional<List<...>>. To samo dotyczy Map i Set.
    }

    // =================================================================================================
    // 4. NIE JAKO POLE KLASY
    // =================================================================================================

    /**
     * 4. Pole typu Optional to zły pomysł:
     * <ul>
     *   <li>Optional NIE jest Serializable — obiektu z takim polem nie zapiszesz standardową serializacją,</li>
     *   <li>samo pole może być null — wtedy masz TRZY stany: null, puste pudełko, pełne pudełko,</li>
     *   <li>każdy obiekt nosi dodatkowe pudełko w pamięci; wiele bibliotek (np. do JSON i baz danych) źle je obsługuje.</li>
     * </ul>
     * Wzorzec: pole może być null (to wewnętrzna sprawa klasy), a getter zwraca {@code Optional.ofNullable(pole)}.
     * Tak zbudowany jest rekord Customer z danych przykładowych: pole email + metoda findEmail().
     */
    static void notAsField() {
        section("4. Nie jako pole klasy — pole może być null, getter zwraca Optional");

        show("czy Optional jest Serializable?", Serializable.class.isAssignableFrom(Optional.class));
        // WYNIK: czy Optional jest Serializable? → false
        // (isAssignableFrom = czy da się przypisać z; tu: czy Optional jest rodzajem Serializable)

        UserProfile withNick = new UserProfile("jkowalski", "Janek");
        UserProfile withoutNick = new UserProfile("mnowak", null);   // brak pseudonimu to normalna sytuacja
        show("nickname() — jkowalski", withNick.nickname());
        show("nickname() — mnowak", withoutNick.nickname());
        show("displayName() — jkowalski", withNick.displayName());
        show("displayName() — mnowak", withoutNick.displayName());
        // WYNIK: nickname() — jkowalski → Optional[Janek]
        // WYNIK: nickname() — mnowak → Optional.empty
        // WYNIK: displayName() — jkowalski → Janek
        // WYNIK: displayName() — mnowak → mnowak    ← gdy brak pseudonimu, pokazujemy login

        // DOBRA PRAKTYKA: null jest dozwolony WEWNĄTRZ klasy (prywatne pole), ale nie „wycieka” na zewnątrz —
        //   świat widzi tylko Optional zwracany przez getter.
    }

    /**
     * UserProfile = profil użytkownika. login jest obowiązkowy, nickname (pseudonim) — nie.
     * Pole nickname może być null; na zewnątrz udostępniamy je jako Optional.
     */
    static final class UserProfile {
        private final String login;
        private final String nickname;                                // może być null — i to jest OK dla POLA

        UserProfile(String login, String nickname) {
            this.login = Objects.requireNonNull(login, "login");      // requireNonNull = wymagaj nie-null
            this.nickname = nickname;
        }

        /** Getter zwraca Optional — dopiero tu powstaje pudełko. */
        Optional<String> nickname() {
            return Optional.ofNullable(nickname);
        }

        /** displayName = nazwa wyświetlana: pseudonim, a gdy go brak — login. */
        String displayName() {
            return nickname().orElse(login);
        }
    }

    // =================================================================================================
    // 5. NIE JAKO PARAMETR METODY
    // =================================================================================================

    /**
     * 5. Parametr typu Optional zmusza KAŻDEGO wywołującego do pakowania wartości w pudełko,
     * a i tak nie chroni przed null — ktoś może przekazać null zamiast pudełka.
     * Lepsze są dwie przeciążone metody (overload): z parametrem i bez niego.
     */
    static void notAsParameter() {
        section("5. Nie jako parametr metody — lepsze przeciążenie");

        show("greetBad(Optional.of(\"Anna\"))", greetBad(Optional.of("Anna")));
        show("greetBad(Optional.empty())", greetBad(Optional.empty()));
        // WYNIK: greetBad(Optional.of("Anna")) → Dzień dobry, Anna!
        // WYNIK: greetBad(Optional.empty()) → Dzień dobry, Gościu!

        expectThrows("greetBad(null)", () -> greetBad(null));
        // WYNIK: ✔ greetBad(null) → rzucono NullPointerException: Cannot invoke "java.util.Optional.orElse(Object)" because "name" is null

        // PO: dwie proste metody. Wywołanie jest krótsze i nie trzeba nic pakować.
        show("greet(\"Anna\")", greet("Anna"));
        show("greet()", greet());
        // WYNIK: greet("Anna") → Dzień dobry, Anna!
        // WYNIK: greet() → Dzień dobry, Gościu!

        // DOBRA PRAKTYKA: gdy przeciążenie nie pasuje (np. parametrów opcjonalnych jest wiele),
        //   przyjmij zwykły parametr, który może być null, i opisz to w dokumentacji — albo użyj wzorca
        //   Builder (t22_design_patterns/Patterns02Builder).
    }

    /** greetBad = przywitaj (ZŁA wersja): parametr typu Optional. */
    static String greetBad(Optional<String> name) {
        return "Dzień dobry, " + name.orElse("Gościu") + "!";
    }

    /** greet = przywitaj po imieniu. */
    static String greet(String name) {
        return "Dzień dobry, " + name + "!";
    }

    /** greet() = przywitaj gościa (przeciążenie bez parametru). */
    static String greet() {
        return greet("Gościu");
    }

    // =================================================================================================
    // 6. isPresent() + get() → map / orElse / ifPresent (PRZED / PO)
    // =================================================================================================

    /**
     * 6. Para {@code if (opt.isPresent()) { ... opt.get() ... }} to „null-check w przebraniu”.
     * Prawie zawsze da się ją zastąpić jedną metodą Optional:
     * <pre>{@code
     *   PRZED                                                  PO
     *   opt.isPresent() ? opt.get() : "brak"                   opt.orElse("brak")
     *   opt.isPresent() ? opt.get().length() : 0               opt.map(String::length).orElse(0)
     *   if (opt.isPresent()) { send(opt.get()); }              opt.ifPresent(e -> send(e))
     *   if (!opt.isPresent()) { throw ...; } x = opt.get();    x = opt.orElseThrow(() -> ...)
     * }</pre>
     */
    static void isPresentGetRefactor() {
        section("6. isPresent() + get() → map / orElse / ifPresent (PRZED / PO)");

        for (Customer c : SampleData.customers().subList(0, 2)) {     // Jan i Maria
            Optional<String> email = c.findEmail();

            // PRZED:
            String textBefore;
            if (email.isPresent()) {
                textBefore = email.get() + " (" + email.get().length() + " znaków)";
            } else {
                textBefore = "brak";
            }
            // PO:
            String textAfter = email.map(e -> e + " (" + e.length() + " znaków)").orElse("brak");

            show(c.name() + " PRZED", textBefore);
            show(c.name() + " PO", textAfter);
        }
        // WYNIK: Jan Kowalski PRZED → jan@example.com (15 znaków)
        // WYNIK: Jan Kowalski PO → jan@example.com (15 znaków)
        // WYNIK: Maria Nowak PRZED → brak
        // WYNIK: Maria Nowak PO → brak

        // Dlaczego PO jest lepsze? Nie ma get(), więc nie da się go wywołać „przez zapomnienie” poza if-em.
        // Nie ma zmiennej, którą trzeba zainicjować w dwóch gałęziach. Czyta się jak zdanie.

        // Kiedy isPresent() jest w porządku? Gdy potrzebujesz TYLKO odpowiedzi tak/nie:
        show("czy Maria ma e-mail?", SampleData.customers().get(1).findEmail().isPresent());
        // WYNIK: czy Maria ma e-mail? → false
    }

    // =================================================================================================
    // 7. of CZY ofNullable — KTO GWARANTUJE, ŻE NIE MA null?
    // =================================================================================================

    /**
     * 7. {@code Optional.of} — gdy TY gwarantujesz, że wartość nie jest null (sam ją utworzyłeś albo sprawdziłeś).
     * {@code Optional.ofNullable} — na granicy ze „starym” kodem, który może zwrócić null (Map.get, pole rekordu,
     * stara biblioteka). Używanie ofNullable „na wszelki wypadek” wszędzie ukrywa błędy.
     */
    static void ofVsOfNullable() {
        section("7. of czy ofNullable — kto gwarantuje, że nie ma null?");

        Customer maria = SampleData.customers().get(1);               // pole email = null

        expectThrows("Optional.of(maria.email())", () -> Optional.of(maria.email()));
        show("Optional.ofNullable(maria.email())", Optional.ofNullable(maria.email()));
        // WYNIK: ✔ Optional.of(maria.email()) → rzucono NullPointerException: (brak komunikatu)
        // WYNIK: Optional.ofNullable(maria.email()) → Optional.empty

        // PUŁAPKA: ofNullable „na wszelki wypadek” zamienia BŁĄD w cichy brak wartości.
        Customer lostCustomer = findCustomerOrNullBuggy();            // błąd: powinien być klient, a jest null
        String city = Optional.ofNullable(lostCustomer).map(Customer::city).orElse("?");
        show("miasto klienta z błędnego kodu", city);
        // WYNIK: miasto klienta z błędnego kodu → ?    ← błąd ukryty — nikt się nie dowie, że klienta zgubiono

        expectThrows("Optional.of(lostCustomer)", () -> Optional.of(lostCustomer));
        // WYNIK: ✔ Optional.of(lostCustomer) → rzucono NullPointerException: (brak komunikatu)
        // of zgłosił problem od razu, w miejscu jego powstania (fail fast = zawiedź szybko).

        // DOBRA PRAKTYKA: of — wartości, które „na pewno są” (ma być wyjątek, jeśli nie są);
        //   ofNullable — wyniki starego API, gdzie null to UMÓWIONY sygnał braku (Map.get, pole mogące być null).
    }

    /** findCustomerOrNullBuggy = udaje zepsuty kod, który gubi klienta i zwraca null. */
    private static Customer findCustomerOrNullBuggy() {
        return null;
    }

    // =================================================================================================
    // 8. orElse(null) — ZAPACH KODU
    // =================================================================================================

    /**
     * 8. {@code orElse(null)} wyjmuje wartość z pudełka i... z powrotem wpuszcza do kodu null.
     * Wszystkie zalety Optional znikają. Jedyne rozsądne użycie: przekazanie wartości do STAREGO API,
     * które wymaga null jako „braku” (i tylko w tym jednym miejscu).
     */
    static void orElseNullSmell() {
        section("8. orElse(null) — zapach kodu (code smell)");

        Customer maria = SampleData.customers().get(1);

        String email = maria.findEmail().orElse(null);               // wracamy do świata null...
        expectThrows("email.length() po orElse(null)", () -> email.length());
        // WYNIK: ✔ email.length() po orElse(null) → rzucono NullPointerException: Cannot invoke "String.length()" because "email" is null

        // Akceptowalne: granica ze starym kodem. legacySend przyjmuje null jako „bez kopii do wiadomości”.
        legacySend("biuro@sklep.pl", maria.findEmail().orElse(null));
        legacySend("biuro@sklep.pl", SampleData.customers().get(0).findEmail().orElse(null));
        // WYNIK: wysyłam do biuro@sklep.pl (DW: brak)
        // WYNIK: wysyłam do biuro@sklep.pl (DW: jan@example.com)

        // DOBRA PRAKTYKA: widzisz orElse(null)? Zapytaj: „czy ta wartość trafia prosto do starego API?”
        //   Jeśli nie — przerób kod na map / ifPresent / orElse(sensowna wartość).
    }

    /**
     * legacySend = wyślij (stara metoda). DW = „do wiadomości” (kopia, ang. cc). null w cc oznacza brak kopii —
     * tak projektowano API przed Javą 8.
     */
    private static void legacySend(String to, String cc) {
        System.out.println("   wysyłam do " + to + " (DW: " + (cc == null ? "brak" : cc) + ")");
    }

    // =================================================================================================
    // 9. KOSZT I NADUŻYWANIE: Optional TO TEŻ OBIEKT
    // =================================================================================================

    /**
     * 9. Każde {@code Optional.of(x)} tworzy nowy obiekt w pamięci (allocation = przydział pamięci).
     * Zwykle to nieistotne, ale w pętli wykonywanej miliony razy — już tak. Jedynie {@code Optional.empty()}
     * jest wspólnym obiektem (singleton). Dla liczb są lżejsze OptionalInt/OptionalLong/OptionalDouble.
     * <p>
     * Druga sprawa: Optional nie jest zamiennikiem KAŻDEGO if-a. Tworzenie pudełka tylko po to, żeby
     * od razu wyjąć z niego wartość, jest dłuższe i wolniejsze niż zwykłe sprawdzenie.
     */
    static void costAndOveruse() {
        section("9. Koszt i nadużywanie — Optional to też obiekt");

        Optional<String> e1 = Optional.empty();
        Optional<String> e2 = Optional.empty();
        show("Optional.of(\"a\") == Optional.of(\"a\")", Optional.of("a") == Optional.of("a"));
        show("Optional.empty() == Optional.empty()", e1 == e2);
        // WYNIK: Optional.of("a") == Optional.of("a") → false    ← dwa nowe obiekty
        // WYNIK: Optional.empty() == Optional.empty() → true    ← jeden wspólny obiekt

        // Trzy sposoby na „wartość albo domyślna”, gdy zmienna lokalna może być null:
        String nick = null;
        String v1 = nick != null ? nick : "anonim";                              // zwykły operator ?:
        String v2 = Optional.ofNullable(nick).orElse("anonim");                  // pudełko na chwilę — przesada
        String v3 = Objects.requireNonNullElse(nick, "anonim");                  // (Java 9+) — krótko i jasno
        show("?: / Optional / requireNonNullElse", v1 + " / " + v2 + " / " + v3);
        // WYNIK: ?: / Optional / requireNonNullElse → anonim / anonim / anonim
        // (requireNonNullElse = wymagaj nie-null, a w przeciwnym razie weź drugą wartość)

        // DOBRA PRAKTYKA: Optional.ofNullable(x).orElse(y) wewnątrz jednej metody to przesada — wystarczy
        //   Objects.requireNonNullElse(x, y). Optional ma sens, gdy wartość „wychodzi” z metody jako wynik.
        // DOBRA PRAKTYKA: dla liczb zwracaj OptionalInt / OptionalDouble zamiast Optional<Integer> —
        //   bez pakowania liczby w obiekt Integer (boxing).
        note("Optional ma pomagać czytelnikowi kodu. Gdy nie pomaga — zwykły if jest lepszy.");
        // WYNIK: ℹ Optional ma pomagać czytelnikowi kodu. Gdy nie pomaga — zwykły if jest lepszy.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA: lista kontrolna przy code review (przeglądzie kodu)
     *   ☐ Optional tylko jako TYP ZWRACANY metody „może nie być wyniku” (findXxx).
     *   ☐ Metoda zwracająca Optional NIGDY nie zwraca null → Optional.empty().
     *   ☐ Kolekcje: pusta lista/mapa/zbiór zamiast Optional<List> i zamiast null.
     *   ☐ Nie w polach (nie jest Serializable, 3 stany) → pole może być null, getter zwraca Optional.ofNullable.
     *   ☐ Nie w parametrach → przeciążenie metody (albo parametr mogący być null / Builder).
     *   ☐ Nie w kolekcjach (List<Optional<T>>) → dodawaj tylko obecne wartości.
     *   ☐ isPresent() + get() → orElse / orElseGet / map / ifPresent / orElseThrow.
     *   ☐ of — gdy wartość na pewno jest (fail fast); ofNullable — gdy null to umówiony „brak” (stare API).
     *   ☐ orElse(null) tylko na granicy ze starym API.
     *   ☐ findById → Optional; getById → obiekt albo wyjątek (zbudowany na findById + orElseThrow).
     *   ☐ Liczby → OptionalInt / OptionalLong / OptionalDouble; proste „albo” w metodzie → requireNonNullElse.
     *
     * PYTANIA KONTROLNE:
     *   1. Podaj trzy powody, dla których Optional nie powinien być polem klasy.
     *   2. ZNAJDŹ BŁĄD:
     *          Optional<Customer> findByEmail(String email) {
     *              for (Customer c : customers) { if (email.equals(c.email())) return Optional.of(c); }
     *              return null;
     *          }
     *   3. ZNAJDŹ BŁĄD:  Optional<List<Order>> findOrders(Customer customer)  — co jest nie tak z tym typem?
     *   4. ZNAJDŹ BŁĄD:
     *          void send(String to, Optional<String> cc) { ... cc.orElse("") ... }
     *          send("a@b.pl", null);
     *   5. Co wypisze:
     *          String s = Optional.<String>empty().orElse(null);
     *          System.out.println(s + "!");
     *   6. Co wypisze:  System.out.println(repo.findByCity("Łódź").size());  (repozytorium z sekcji 1)  ?
     *   7. Kiedy Optional.of jest lepszy od Optional.ofNullable, choć „bardziej niebezpieczny”?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Uruchamia ćwiczenia: najpierw TWOJE rozwiązania (✘ dopóki nie uzupełnisz), potem wzorcowe (✔). */
    static void exercises() {
        List<Customer> customers = SampleData.customers();
        Customer jan = customers.get(0);
        Customer maria = customers.get(1);
        CustomerRepository repo = new CustomerRepository(customers);

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: kontakt Jana", "Wyślij na: jan@example.com", () -> exercise1(jan));
        Check.equal("ćw. 1b: kontakt Marii", "Zadzwoń do: Maria Nowak", () -> exercise1(maria));
        Check.equal("ćw. 2a: VIP w Warszawie", Optional.of(jan), () -> exercise2(customers, "Warszawa"));
        Check.equal("ćw. 2b: VIP w Krakowie (brak)", Optional.empty(), () -> exercise2(customers, "Kraków"));
        Check.equal("ćw. 3a: e-maile z Warszawy", List.of("jan@example.com", "zofia@example.com"),
                () -> exercise3(customers, "Warszawa"));
        Check.equal("ćw. 3b: e-maile z Krakowa", List.of("marek@example.com"), () -> exercise3(customers, "Kraków"));
        Check.equal("ćw. 3c: e-maile z Łodzi", List.of(), () -> exercise3(customers, "Łódź"));
        Check.equal("ćw. 4a: etykieta klienta 1", "Jan Kowalski <jan@example.com>", () -> exercise4(repo, 1));
        Check.equal("ćw. 4b: etykieta klienta 5", "Ola Pawlak <brak e-maila>", () -> exercise4(repo, 5));
        Check.throwsException("ćw. 4c: klient 42 → wyjątek", NoSuchElementException.class, () -> exercise4(repo, 42));
        Check.equal("ćw. 5a: pierwszy e-mail z [5, 2, 3]", Optional.of("adam.mazur@example.com"),
                () -> exercise5(repo, List.of(5L, 2L, 3L)));
        Check.equal("ćw. 5b: pierwszy e-mail z [5, 2]", Optional.empty(), () -> exercise5(repo, List.of(5L, 2L)));
        Check.equal("ćw. 5c: pierwszy e-mail z [42, 4, 1]", Optional.of("zofia@example.com"),
                () -> exercise5(repo, List.of(42L, 4L, 1L)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", "Wyślij na: jan@example.com", () -> solution1(jan));
        Check.equal("ćw. 1b (wzorzec)", "Zadzwoń do: Maria Nowak", () -> solution1(maria));
        Check.equal("ćw. 2a (wzorzec)", Optional.of(jan), () -> solution2(customers, "Warszawa"));
        Check.equal("ćw. 2b (wzorzec)", Optional.empty(), () -> solution2(customers, "Kraków"));
        Check.equal("ćw. 3a (wzorzec)", List.of("jan@example.com", "zofia@example.com"),
                () -> solution3(customers, "Warszawa"));
        Check.equal("ćw. 3b (wzorzec)", List.of("marek@example.com"), () -> solution3(customers, "Kraków"));
        Check.equal("ćw. 3c (wzorzec)", List.of(), () -> solution3(customers, "Łódź"));
        Check.equal("ćw. 4a (wzorzec)", "Jan Kowalski <jan@example.com>", () -> solution4(repo, 1));
        Check.equal("ćw. 4b (wzorzec)", "Ola Pawlak <brak e-maila>", () -> solution4(repo, 5));
        Check.throwsException("ćw. 4c (wzorzec)", NoSuchElementException.class, () -> solution4(repo, 42));
        Check.equal("ćw. 5a (wzorzec)", Optional.of("adam.mazur@example.com"), () -> solution5(repo, List.of(5L, 2L, 3L)));
        Check.equal("ćw. 5b (wzorzec)", Optional.empty(), () -> solution5(repo, List.of(5L, 2L)));
        Check.equal("ćw. 5c (wzorzec)", Optional.of("zofia@example.com"), () -> solution5(repo, List.of(42L, 4L, 1L)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 13 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ kod z isPresent() + get() na jedno wyrażenie bez get():
     * <pre>{@code
     * Optional<String> email = customer.findEmail();
     * if (email.isPresent()) {
     *     return "Wyślij na: " + email.get();
     * }
     * return "Zadzwoń do: " + customer.name();
     * }</pre>
     * Podpowiedź: {@code map(e -> "Wyślij na: " + e)}, a na końcu orElse(...).
     */
    static String exercise1(Customer customer) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 2 (łatwe, ZNAJDŹ I NAPRAW): metoda ma zwrócić pierwszego klienta VIP z podanego miasta.
     * Działa dla Warszawy, ale dla Krakowa łamie zasadę z sekcji 2. Popraw jedną linijkę.
     * Podpowiedź: co powinna zwrócić metoda z typem Optional, gdy nic nie znalazła?
     */
    static Optional<Customer> exercise2(List<Customer> customers, String city) {
        for (Customer c : customers) {
            if (c.city().equals(city) && c.vip()) {
                return Optional.of(c);
            }
        }
        return null;                                                  // TODO: popraw
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć listę e-maili klientów z podanego miasta (pomijając tych bez e-maila).
     * Gdy nikogo nie ma — PUSTA lista (nie null, nie Optional). Kolejność jak na liście klientów.
     * Podpowiedź: new ArrayList, pętla, warunek na city(), a w środku findEmail().ifPresent(lista::add).
     */
    static List<String> exercise3(List<Customer> customers, String city) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 4 (średnie, repozytorium): zwróć etykietę klienta o podanym id w postaci
     * {@code "Jan Kowalski <jan@example.com>"}; gdy klient nie ma e-maila: {@code "Ola Pawlak <brak e-maila>"}.
     * Gdy klienta nie ma — NoSuchElementException (brak klienta to tu BŁĄD).
     * Podpowiedź: repo.getById(id) (sam rzuci wyjątek), potem findEmail().orElse("brak e-maila").
     */
    static String exercise4(CustomerRepository repo, long id) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): klient podał listę id osób kontaktowych w kolejności ważności.
     * Zwróć e-mail PIERWSZEJ osoby, która istnieje w repozytorium i ma e-mail. Gdy nikt — pusty Optional.
     * Podpowiedź: pętla po ids; dla każdego id: repo.findById(id).flatMap(Customer::findEmail).
     * Wersja dla ambitnych: zmienna {@code Optional<String> result = Optional.empty()} i w pętli
     * {@code result = result.or(() -> ...)} (or — Java 9+).
     */
    static Optional<String> exercise5(CustomerRepository repo, List<Long> ids) {
        // TODO: twoje rozwiązanie
        return Optional.empty();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(Customer customer) {
        return customer.findEmail()
                .map(e -> "Wyślij na: " + e)
                .orElse("Zadzwoń do: " + customer.name());           // tani napis → orElse wystarczy
    }

    static Optional<Customer> solution2(List<Customer> customers, String city) {
        for (Customer c : customers) {
            if (c.city().equals(city) && c.vip()) {
                return Optional.of(c);
            }
        }
        return Optional.empty();                                      // nigdy null!
    }

    static List<String> solution3(List<Customer> customers, String city) {
        List<String> result = new ArrayList<>();
        for (Customer c : customers) {
            if (c.city().equals(city)) {
                c.findEmail().ifPresent(result::add);
            }
        }
        return result;                                                // pusta lista, gdy nikogo nie ma
    }

    static String solution4(CustomerRepository repo, long id) {
        Customer customer = repo.getById(id);                         // brak → NoSuchElementException
        return customer.name() + " <" + customer.findEmail().orElse("brak e-maila") + ">";
    }

    static Optional<String> solution5(CustomerRepository repo, List<Long> ids) {
        Optional<String> result = Optional.empty();
        for (long id : ids) {
            result = result.or(() -> repo.findById(id).flatMap(Customer::findEmail));   // leniwie: tylko gdy pusto
        }
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. (a) Optional nie jest Serializable; (b) samo pole może być null → trzy stany zamiast dwóch;
     *      (c) dodatkowy obiekt w każdej instancji, a biblioteki (JSON, bazy danych) często go nie rozumieją.
     *      Zamiast tego: zwykłe pole (może być null) + getter zwracający Optional.ofNullable(pole).
     *   2. return null w metodzie zwracającej Optional. Wywołujący dostanie NullPointerException przy pierwszym
     *      .map/.isPresent. Poprawnie: return Optional.empty();
     *   3. Lista ma już swój „brak” — pustą listę. Optional<List> daje dwa różne „nic” (Optional.empty
     *      i Optional[[]]). Poprawnie: List<Order> findOrders(Customer customer), zwracająca pustą listę.
     *   4. Parametr typu Optional: każdy musi pakować wartość, a null i tak przejdzie — cc.orElse("") rzuci
     *      NullPointerException. Poprawnie: przeciążenia send(to) i send(to, cc).
     *   5. „null!” — orElse(null) zwraca null, a sklejanie napisu z null daje tekst "null".
     *   6. „0” — findByCity zwraca pustą listę, nie null, więc size() działa bez sprawdzania.
     *   7. Gdy null oznaczałby BŁĄD w programie. of rzuci wyjątek od razu w miejscu powstania problemu
     *      (fail fast), a ofNullable ukryłby błąd jako „zwykły brak wartości”.
     */
    // </editor-fold>
}
