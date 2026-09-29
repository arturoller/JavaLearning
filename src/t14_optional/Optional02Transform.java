package t14_optional;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Customer;
import helpers.model.Product;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Optional — przekształcanie wartości bez wyjmowania jej z pudełka
 *        (transform = przekształcać; map = przekształć (mapuj); flatMap = przekształć i spłaszcz; filter = filtruj)
 *
 * W SKRÓCIE:
 *   Nie musisz wyjmować wartości z Optional, żeby coś z nią zrobić. Możesz ją PRZEKSZTAŁCAĆ w środku pudełka:
 *   map zmienia wartość, flatMap łączy metody, które same zwracają Optional, filter odrzuca niepasujące wartości.
 *   Puste pudełko przechodzi przez cały łańcuch bez błędu, a decyzję „co, gdy pusto?” podejmujesz raz — na końcu.
 *
 * ANALOGIA: taśma w sortowni paczek.
 *   • Paczka jedzie przez kolejne stanowiska: jedno przepakowuje towar (map), drugie wyjmuje paczkę
 *     z większego kartonu (flatMap), trzecie odrzuca paczki, które nie spełniają warunku (filter).
 *   • Pusta paczka też jedzie po taśmie, ale każde stanowisko ją po prostu przepuszcza. Nikt nie sięga
 *     do środka, więc nikt nie „łapie powietrza” (NullPointerException).
 *   • Na końcu taśmy stoi jeden pracownik, który decyduje, co zrobić z pustą paczką (orElse, orElseThrow...).
 *
 * JAK TO DZIAŁA:
 *   Każda metoda zwraca NOWY Optional (stary się nie zmienia):
 *                     pudełko PEŁNE [x]                           pudełko PUSTE
 *     map(f)       →  Optional.ofNullable(f(x))                →  puste (f się NIE wykona)
 *     flatMap(f)   →  f(x) — f sama zwraca Optional            →  puste (f się NIE wykona)
 *     filter(p)    →  [x], gdy p(x) prawdziwe; inaczej puste   →  puste (p się NIE wykona)
 *     or(s)        →  [x] (s się NIE wykona)                   →  s.get() — inny Optional (Java 9+)
 *     stream()     →  strumień z 1 elementem                   →  pusty strumień (Java 9+)
 *   Typowy łańcuch:
 *     findCustomerById(id) → flatMap(findEmail) → map(domena) → map(wielkie litery) → orElse("...")
 *
 * SŁÓWKA:
 *   transform = przekształcać; map = przekształć (mapuj); flat = płaski; flatMap = przekształć i spłaszcz;
 *   filter = filtruj; or = albo (inny Optional); stream = strumień; chain = łańcuch; nested = zagnieżdżony;
 *   domain = domena (część adresu po @); upper case = wielkie litery; region = województwo; backup = zapasowy;
 *   trace = ślad; step = krok; equals = równa się; hash code = skrót (liczba wyliczona z obiektu).
 *
 * ZOBACZ TEŻ: t14_optional/Optional01Basics (tworzenie Optional i wyjmowanie wartości),
 *             t14_optional/Optional03BestPractices (gdzie używać Optional, a gdzie nie),
 *             t13_lambdas/Lambda04MethodReferences (zapis Customer::findEmail),
 *             t16_streams/Streams04FlatMap (flatMap w strumieniach), t16_streams/Streams14OptionalInStreams.
 * </pre>
 */
public class Optional02Transform {

    public static void main(String[] args) {
        title("Optional02 — przekształcanie: map, flatMap, filter, or, stream");

        mapBasics();                // map basics = podstawy map
        mapReturningNull();         // map returning null = map zwracające null
        flatMapBasics();            // flatMap basics = podstawy flatMap
        filterBasics();             // filter basics = podstawy filter
        orAlternative();            // or alternative = or — zapasowy Optional
        streamPreview();            // stream preview = zapowiedź stream()
        chains();                   // chains = łańcuchy wywołań
        wrappingMapGet();           // wrapping Map.get = opakowanie wyniku Map.get
        equalsAndToString();        // equals and toString = porównywanie i wypisywanie
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. map — PRZEKSZTAŁĆ WARTOŚĆ W PUDEŁKU
    // =================================================================================================

    /**
     * 1. {@code map(funkcja)} stosuje funkcję do wartości W ŚRODKU pudełka i zwraca nowe pudełko z wynikiem.
     * Typ zawartości może się zmienić: {@code Optional<String>} po {@code map(String::length)}
     * staje się {@code Optional<Integer>}. Dla pustego pudełka funkcja w ogóle się nie wykonuje.
     */
    static void mapBasics() {
        section("1. map — przekształć wartość, nie wyjmując jej z pudełka");

        Customer jan = SampleData.customers().get(0);                 // ma e-mail
        Customer maria = SampleData.customers().get(1);               // NIE ma e-maila

        Optional<String> janEmail = jan.findEmail();                  // findEmail = znajdź e-mail
        Optional<Integer> janLength = janEmail.map(String::length);   // length = długość napisu
        show("e-mail Jana", janEmail);
        show("map(String::length)", janLength);
        // WYNIK: e-mail Jana → Optional[jan@example.com]
        // WYNIK: map(String::length) → Optional[15]

        show("Maria: map(String::length)", maria.findEmail().map(String::length));
        // WYNIK: Maria: map(String::length) → Optional.empty

        // Wynik map może być dowolnego typu, np. gotowy tekst powitania:
        show("Jan: powitanie", janEmail.map(email -> "Witaj, " + email + "!"));
        // WYNIK: Jan: powitanie → Optional[Witaj, jan@example.com!]
        // Że funkcja z map NIE uruchamia się dla pustego pudełka, zobaczysz na własne oczy w sekcji 7 (ślad).

        // PRZED (wyjmowanie i sprawdzanie — „null-check w przebraniu”):
        int lengthOld;
        if (janEmail.isPresent()) {
            lengthOld = janEmail.get().length();
        } else {
            lengthOld = 0;
        }
        // PO (przekształcenie w pudełku + jedna decyzja na końcu):
        int lengthNew = janEmail.map(String::length).orElse(0);
        show("długość PRZED / PO", lengthOld + " / " + lengthNew);
        // WYNIK: długość PRZED / PO → 15 / 15

        // DOBRA PRAKTYKA: najpierw przekształć (map), a dopiero NA KOŃCU zdecyduj, co z pustym pudełkiem
        //   (orElse, orElseGet, orElseThrow, ifPresent). Tak kod czyta się od góry do dołu jak przepis.
    }

    // =================================================================================================
    // 2. map, KTÓRE ZWRACA null → PUSTE PUDEŁKO
    // =================================================================================================

    /**
     * 2. Co jeśli funkcja w map zwróci null? map opakowuje wynik przez {@code Optional.ofNullable},
     * więc null zamienia się w puste pudełko. Nigdy nie powstanie „pudełko z null w środku”.
     * <p>
     * To wygodne przy starych metodach zwracających null — np. {@code Map.get} dla brakującego klucza.
     */
    static void mapReturningNull() {
        section("2. map zwracające null → puste pudełko");

        // regions = województwa (miasto → województwo). Gdańska celowo nie ma w mapie.
        Map<String, String> regions = new LinkedHashMap<>();
        regions.put("Warszawa", "mazowieckie");
        regions.put("Kraków", "małopolskie");
        regions.put("Poznań", "wielkopolskie");

        for (Customer c : SampleData.customers().subList(0, 3)) {
            Optional<String> region = Optional.of(c)
                    .map(Customer::city)                              // city = miasto
                    .map(regions::get);                               // dla "Gdańsk" get zwraca null
            show("województwo: " + c.name(), region);
        }
        // WYNIK: województwo: Jan Kowalski → Optional[mazowieckie]
        // WYNIK: województwo: Maria Nowak → Optional[małopolskie]
        // WYNIK: województwo: Adam Mazur → Optional.empty    ← null z regions.get zamienił się w puste pudełko

        // Dzięki temu można bezpiecznie dopisać kolejny krok, np. .map(r -> r.toUpperCase(Locale.ROOT))
        // — dla Gdańska się nie wykona, więc nie będzie NullPointerException.

        // PUŁAPKA: null w map jest „połykany” po cichu. Jeśli null oznacza BŁĄD (np. w mapie POWINNO być
        //   każde miasto), nie dowiesz się o nim — dostaniesz tylko puste pudełko. Wtedy lepiej rzucić
        //   wyjątek od razu: .map(city -> requireRegion(city)) albo na końcu orElseThrow(...) z komunikatem.
    }

    // =================================================================================================
    // 3. flatMap — GDY FUNKCJA SAMA ZWRACA Optional
    // =================================================================================================

    /**
     * 3. Metoda {@code findEmail()} już zwraca Optional. Gdy użyjesz jej w map, dostaniesz pudełko w pudełku:
     * {@code Optional<Optional<String>>}. flatMap „spłaszcza” wynik — zostawia tylko jedno pudełko.
     * <p>
     * Reguła: funkcja zwraca zwykłą wartość → map; funkcja zwraca Optional → flatMap.
     */
    static void flatMapBasics() {
        section("3. flatMap — gdy funkcja sama zwraca Optional");

        Optional<Optional<String>> nested = findCustomerById(1).map(Customer::findEmail);
        show("map(Customer::findEmail)", nested);
        // WYNIK: map(Customer::findEmail) → Optional[Optional[jan@example.com]]    ← pudełko w pudełku

        Optional<String> flat = findCustomerById(1).flatMap(Customer::findEmail);
        show("flatMap(Customer::findEmail)", flat);
        // WYNIK: flatMap(Customer::findEmail) → Optional[jan@example.com]

        // Trzy przypadki: klient z e-mailem (1), klient bez e-maila (2), brak klienta (99).
        for (long id : new long[]{1, 2, 99}) {
            System.out.println("   id " + id + ": map → " + findCustomerById(id).map(Customer::findEmail)
                    + " | flatMap → " + findCustomerById(id).flatMap(Customer::findEmail));
        }
        // WYNIK: id 1: map → Optional[Optional[jan@example.com]] | flatMap → Optional[jan@example.com]
        // WYNIK: id 2: map → Optional[Optional.empty] | flatMap → Optional.empty
        // WYNIK: id 99: map → Optional.empty | flatMap → Optional.empty

        // PUŁAPKA: Optional[Optional.empty] to PEŁNE pudełko, w którym leży puste pudełko.
        //   isPresent() zwraca true, a dopiero wewnętrzne get() rzuci wyjątek.
        Optional<Optional<String>> mariaNested = findCustomerById(2).map(Customer::findEmail);
        show("Maria: map(...).isPresent()", mariaNested.isPresent());
        // WYNIK: Maria: map(...).isPresent() → true    ← mylące!
        expectThrows("Maria: get().get()", () -> mariaNested.get().get());
        // WYNIK: ✔ Maria: get().get() → rzucono NoSuchElementException: No value present

        // flatMap z własną metodą: parseNumber zwraca Optional<Integer> (pusty, gdy tekst nie jest liczbą).
        show("\"42\" → flatMap(parseNumber)", Optional.of("42").flatMap(Optional02Transform::parseNumber));
        show("\"abc\" → flatMap(parseNumber)", Optional.of("abc").flatMap(Optional02Transform::parseNumber));
        show("\"42\" → map(parseNumber)", Optional.of("42").map(Optional02Transform::parseNumber));
        // WYNIK: "42" → flatMap(parseNumber) → Optional[42]
        // WYNIK: "abc" → flatMap(parseNumber) → Optional.empty
        // WYNIK: "42" → map(parseNumber) → Optional[Optional[42]]    ← znów pudełko w pudełku

        // DOBRA PRAKTYKA: widzisz Optional<Optional<...>>? Prawie zawsze zamiast map powinno być flatMap.
    }

    /** parseNumber = zamień tekst na liczbę; pusty Optional, gdy parseInt rzuci NumberFormatException. */
    static Optional<Integer> parseNumber(String text) {
        try {
            return Optional.of(Integer.parseInt(text));               // parseInt = przetwórz na int
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    // =================================================================================================
    // 4. filter — ZOSTAW WARTOŚĆ TYLKO, GDY SPEŁNIA WARUNEK
    // =================================================================================================

    /**
     * 4. {@code filter(warunek)} przepuszcza wartość, gdy warunek (Predicate) jest spełniony.
     * W przeciwnym razie zwraca puste pudełko. Dla pustego pudełka warunek się nie wykonuje.
     */
    static void filterBasics() {
        section("4. filter — zostaw wartość tylko, gdy spełnia warunek");

        show("Jan — filter(Customer::vip)", findCustomerById(1).filter(Customer::vip));   // vip = ważny klient
        show("Adam — filter(Customer::vip)", findCustomerById(3).filter(Customer::vip));
        // WYNIK: Jan — filter(Customer::vip) → Optional[Jan Kowalski (Warszawa, VIP)]
        // WYNIK: Adam — filter(Customer::vip) → Optional.empty    ← Adam jest, ale nie jest VIP-em

        // Częste zastosowanie: dane z formularza. null, same spacje i pusty tekst traktujemy jak „brak”.
        String[] inputs = {"  Kraków ", "   ", null};
        for (String raw : inputs) {
            Optional<String> city = cleanInput(raw);
            show("\"" + raw + "\"", city);
        }
        // WYNIK: "  Kraków " → Optional[Kraków]
        // WYNIK: "   " → Optional.empty
        // WYNIK: "null" → Optional.empty

        // Predicate.not(String::isEmpty) (Java 11+) znaczy „NIE jest pusty” — czytelniej niż s -> !s.isEmpty().

        // PUŁAPKA: filter NIE usuwa niczego z oryginału — zwraca nowe pudełko. Wynik trzeba przypisać:
        Optional<Customer> adam = findCustomerById(3);
        adam.filter(Customer::vip);                                   // wynik wyrzucony do kosza!
        show("adam po adam.filter(...) bez przypisania", adam);
        // WYNIK: adam po adam.filter(...) bez przypisania → Optional[Adam Mazur (Gdańsk)]
    }

    /**
     * cleanInput = oczyść dane wejściowe. null albo sam biały tekst → puste pudełko; inaczej tekst bez spacji
     * na brzegach. strip (Java 11+) = usuń białe znaki z początku i końca.
     */
    static Optional<String> cleanInput(String raw) {
        return Optional.ofNullable(raw)                               // null → puste pudełko
                .map(String::strip)
                .filter(Predicate.not(String::isEmpty));              // Predicate.not = zaprzeczenie (Java 11+)
    }

    // =================================================================================================
    // 5. or (Java 9+) — ZAPASOWY Optional
    // =================================================================================================

    /**
     * 5. {@code or(() -> innyOptional)} (Java 9+): gdy pudełko jest puste, weź pudełko z innego źródła.
     * Różnica względem orElse:
     * <ul>
     *   <li>orElse zwraca WARTOŚĆ (String) — łańcuch się kończy,</li>
     *   <li>or zwraca OPTIONAL — łańcuch trwa dalej (można dopisać map, filter, kolejne or...).</li>
     * </ul>
     * Lambda w or jest leniwa jak w orElseGet — uruchamia się tylko dla pustego pudełka.
     */
    static void orAlternative() {
        section("5. or (Java 9+) — zapasowe źródło, gdy pudełko puste");

        // backupEmails = zapasowe e-maile (np. ze starego systemu), klucz to imię i nazwisko.
        Map<String, String> backupEmails = new LinkedHashMap<>();
        backupEmails.put("Maria Nowak", "maria.nowak@poczta.pl");

        List<Customer> all = SampleData.customers();
        for (Customer c : List.of(all.get(0), all.get(1), all.get(4))) {           // Jan, Maria, Ola
            Optional<String> contact = c.findEmail()
                    .or(() -> backupEmail(c, backupEmails));                        // tylko gdy findEmail pusty
            show("kontakt: " + c.name(), contact);
        }
        // WYNIK: kontakt: Jan Kowalski → Optional[jan@example.com]    ← backupEmail się nie wykonał
        // WYNIK: ⚙ backupEmail: szukam w starym systemie dla Maria Nowak
        // WYNIK: kontakt: Maria Nowak → Optional[maria.nowak@poczta.pl]
        // WYNIK: ⚙ backupEmail: szukam w starym systemie dla Ola Pawlak
        // WYNIK: kontakt: Ola Pawlak → Optional.empty    ← oba źródła puste

        // Po or łańcuch trwa dalej — np. wycinamy login (część przed @):
        Customer maria = all.get(1);
        String login = maria.findEmail()
                .or(() -> Optional.ofNullable(backupEmails.get(maria.name())))
                .map(email -> email.substring(0, email.indexOf('@')))   // substring = fragment napisu
                .orElse("(brak loginu)");
        show("login Marii", login);
        // WYNIK: login Marii → maria.nowak

        // DOBRA PRAKTYKA: kilka źródeł po kolei (pamięć podręczna → baza → wartość domyślna):
        //   fromCache(id).or(() -> fromDatabase(id)).orElse(DEFAULT)
    }

    /** backupEmail = e-mail zapasowy. Wypisuje komunikat (efekt uboczny — do pokazu), żeby było widać, kiedy działa. */
    private static Optional<String> backupEmail(Customer customer, Map<String, String> backupEmails) {
        System.out.println("   ⚙ backupEmail: szukam w starym systemie dla " + customer.name());
        return Optional.ofNullable(backupEmails.get(customer.name()));
    }

    // =================================================================================================
    // 6. stream() (Java 9+) — ZAPOWIEDŹ
    // =================================================================================================

    /**
     * 6. {@code stream()} (Java 9+) zamienia Optional w strumień z 0 albo 1 elementem.
     * Sam w sobie mało przydatny — pokaże swoją siłę w strumieniach (t16_streams/Streams14OptionalInStreams).
     */
    static void streamPreview() {
        section("6. stream() (Java 9+) — zapowiedź strumieni");

        Customer jan = SampleData.customers().get(0);
        Customer maria = SampleData.customers().get(1);

        List<String> janList = jan.findEmail().stream().toList();     // toList (Java 16+) = zbierz do listy
        List<String> mariaList = maria.findEmail().stream().toList();
        show("Jan: stream().toList()", janList);
        show("Maria: stream().toList()", mariaList);
        // WYNIK: Jan: stream().toList() → [jan@example.com]
        // WYNIK: Maria: stream().toList() → []

        // Zapowiedź t16: zbierz e-maile wszystkich klientów, pomijając puste pudełka.
        // W Optional01Basics robiliśmy to pętlą z ifPresent — tu jedna „rura” (pipeline):
        List<String> allEmails = SampleData.customers().stream()      // strumień klientów
                .map(Customer::findEmail)                             // strumień Optionali
                .flatMap(Optional::stream)                            // puste znikają, pełne → wartości
                .toList();
        show("wszystkie e-maile", allEmails);
        // WYNIK: wszystkie e-maile → [jan@example.com, adam.mazur@example.com, zofia@example.com, marek@example.com, ewa.lis@example.com]

        note("Szczegóły strumieni: t16_streams/Streams01Intro i t16_streams/Streams14OptionalInStreams.");
        // WYNIK: ℹ Szczegóły strumieni: t16_streams/Streams01Intro i t16_streams/Streams14OptionalInStreams.
    }

    // =================================================================================================
    // 7. ŁAŃCUCHY: klient → e-mail → domena → wielkie litery
    // =================================================================================================

    /**
     * 7. Prawdziwa siła Optional to łańcuchy. Każdy krok może „nie znaleźć” wyniku, a mimo to kod nie ma
     * ani jednego if-a. Porównaj wersję z null (zagnieżdżone if-y) z wersją z Optional.
     */
    static void chains() {
        section("7. Łańcuchy: klient → e-mail → domena → wielkie litery");

        for (long id : new long[]{1, 3, 2, 99}) {
            System.out.println("   id " + id + ": PRZED = " + emailDomainUpperOld(id)
                    + ", PO = " + emailDomainUpper(id));
        }
        // WYNIK: id 1: PRZED = EXAMPLE.COM, PO = EXAMPLE.COM
        // WYNIK: id 3: PRZED = EXAMPLE.COM, PO = EXAMPLE.COM
        // WYNIK: id 2: PRZED = (brak domeny), PO = (brak domeny)
        // WYNIK: id 99: PRZED = (brak domeny), PO = (brak domeny)
        // Wynik ten sam, ale w wersji z Optional nie ma if-ów i nie da się zapomnieć o sprawdzeniu null.

        // Ślad (trace): które kroki łańcucha naprawdę się wykonują? Pierwszy pusty krok zatrzymuje resztę.
        for (long id : new long[]{1, 2, 99}) {
            System.out.println("   --- id " + id);
            show("   wynik", tracedDomain(id));
        }
        // WYNIK: --- id 1
        // WYNIK: krok 1: znaleziono klienta Jan Kowalski
        // WYNIK: krok 2: mam e-mail jan@example.com
        // WYNIK: krok 3: mam domenę example.com
        // WYNIK: wynik → EXAMPLE.COM
        // WYNIK: --- id 2
        // WYNIK: krok 1: znaleziono klienta Maria Nowak
        // WYNIK: wynik → (brak domeny)    ← e-maila brak, kroki 2 i 3 pominięte
        // WYNIK: --- id 99
        // WYNIK: wynik → (brak domeny)    ← klienta brak, żaden krok się nie wykonał

        // PUŁAPKA: toUpperCase() bez Locale używa języka systemu. Na komputerze z językiem tureckim
        //   "i".toUpperCase() daje "İ" (i z kropką). Dlatego piszemy toUpperCase(Locale.ROOT).
    }

    /** emailDomainUpper = domena e-maila klienta wielkimi literami (wersja z Optional). */
    static String emailDomainUpper(long id) {
        return findCustomerById(id)                                   // Optional<Customer>
                .flatMap(Customer::findEmail)                         // Optional<String> — e-mail
                .map(email -> email.substring(email.indexOf('@') + 1)) // Optional<String> — domena
                .map(domain -> domain.toUpperCase(Locale.ROOT))       // Optional<String> — WIELKIE LITERY
                .orElse("(brak domeny)");                             // jedna decyzja na końcu
    }

    /** emailDomainUpperOld = to samo „po staremu”: metody zwracające null i zagnieżdżone if-y. */
    static String emailDomainUpperOld(long id) {
        Customer customer = findCustomerOrNull(id);
        if (customer != null) {
            String email = customer.email();                          // pole e-mail może być null
            if (email != null) {
                String domain = email.substring(email.indexOf('@') + 1);
                return domain.toUpperCase(Locale.ROOT);
            }
        }
        return "(brak domeny)";
    }

    /** tracedDomain = domena ze śladem. println w lambdach to efekt uboczny — tylko do pokazu, jak działa łańcuch. */
    static String tracedDomain(long id) {
        return findCustomerById(id)
                .flatMap(c -> {
                    System.out.println("   krok 1: znaleziono klienta " + c.name());
                    return c.findEmail();
                })
                .map(email -> {
                    System.out.println("   krok 2: mam e-mail " + email);
                    return email.substring(email.indexOf('@') + 1);
                })
                .map(domain -> {
                    System.out.println("   krok 3: mam domenę " + domain);
                    return domain.toUpperCase(Locale.ROOT);
                })
                .orElse("(brak domeny)");
    }

    /** findCustomerById = znajdź klienta po id. Zwykła pętla (strumienie będą w t16). */
    static Optional<Customer> findCustomerById(long id) {
        for (Customer c : SampleData.customers()) {
            if (c.id() == id) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    /** findCustomerOrNull = stary styl: klient albo null (orElse(null) to zapach kodu — Optional03BestPractices). */
    static Customer findCustomerOrNull(long id) {
        return findCustomerById(id).orElse(null);                     // tylko do porównania PRZED/PO
    }

    // =================================================================================================
    // 8. OPAKOWANIE Map.get W ofNullable
    // =================================================================================================

    /**
     * 8. {@code Map.get(klucz)} to klasyczne „stare API”: dla brakującego klucza zwraca null.
     * Owinięcie w {@code Optional.ofNullable(map.get(k))} daje bezpieczne pudełko, na którym można już
     * wywołać map, filter i orElse. Nigdy nie używaj tu {@code Optional.of} — rzuci NullPointerException.
     */
    static void wrappingMapGet() {
        section("8. Opakowanie Map.get w Optional.ofNullable");

        Map<String, Product> bySku = new LinkedHashMap<>();           // bySku = według SKU (kodu produktu)
        for (Product p : SampleData.products()) {
            bySku.put(p.sku(), p);
        }

        show("KSI-002", findProduct(bySku, "KSI-002").map(Product::name));
        show("XXX-999", findProduct(bySku, "XXX-999").map(Product::name));
        // WYNIK: KSI-002 → Optional[Java. Podstawy]
        // WYNIK: XXX-999 → Optional.empty

        // Cały opis ceny w jednym łańcuchu:
        for (String sku : new String[]{"DOM-002", "XXX-999"}) {
            String priceText = findProduct(bySku, sku)
                    .map(p -> p.name() + ": " + p.price() + " zł")
                    .orElse("nie ma produktu " + sku);
            show("cena " + sku, priceText);
        }
        // WYNIK: cena DOM-002 → Lampka biurkowa: 129.00 zł
        // WYNIK: cena XXX-999 → nie ma produktu XXX-999

        expectThrows("Optional.of(bySku.get(\"XXX-999\"))", () -> Optional.of(bySku.get("XXX-999")));
        // WYNIK: ✔ Optional.of(bySku.get("XXX-999")) → rzucono NullPointerException: (brak komunikatu)

        // PUŁAPKA: jeśli mapa przechowuje null JAKO WARTOŚĆ (put("ZAM-002", null)), to get zwraca null i dla
        //   „klucza nie ma”, i dla „klucz jest, wartość null”. Optional tych sytuacji nie odróżni — tylko containsKey.

        // DOBRA PRAKTYKA: nie wkładaj null do map (t12_collections/Collections05Maps). Gdy potrzebujesz tylko
        //   wartości domyślnej, wystarczy map.getOrDefault(k, domyślna). Optional wybierz, gdy chcesz dalej
        //   przekształcać wynik (map, filter) albo zwrócić go z metody.
    }

    /** findProduct = znajdź produkt. Opakowuje wynik Map.get w Optional (null → puste pudełko). */
    static Optional<Product> findProduct(Map<String, Product> bySku, String sku) {
        return Optional.ofNullable(bySku.get(sku));
    }

    // =================================================================================================
    // 9. equals, hashCode I toString
    // =================================================================================================

    /**
     * 9. Dwa Optionale są równe (equals), gdy oba są puste albo oba zawierają równe (equals) wartości.
     * Operator {@code ==} porównuje ADRESY obiektów — do Optional się nie nadaje.
     * hashCode pełnego pudełka to hashCode wartości, pustego — 0.
     */
    static void equalsAndToString() {
        section("9. Porównywanie (equals) i wypisywanie (toString)");

        Optional<String> coffee1 = Optional.of("kawa");
        Optional<String> coffee2 = Optional.of("kawa");
        show("coffee1.equals(coffee2)", coffee1.equals(coffee2));
        show("coffee1 == coffee2", coffee1 == coffee2);
        // WYNIK: coffee1.equals(coffee2) → true
        // WYNIK: coffee1 == coffee2 → false    ← dwa różne obiekty-pudełka

        Optional<String> empty1 = Optional.empty();
        Optional<String> empty2 = Optional.ofNullable(null);
        show("empty().equals(ofNullable(null))", empty1.equals(empty2));
        show("of(\"kawa\").equals(empty())", coffee1.equals(empty1));
        // WYNIK: empty().equals(ofNullable(null)) → true
        // WYNIK: of("kawa").equals(empty()) → false

        // PUŁAPKA: porównanie pudełka z ZAWARTOŚCIĄ. Kompiluje się (equals przyjmuje Object), ale zawsze daje false.
        show("of(\"kawa\").equals(\"kawa\")", coffee1.equals("kawa"));
        // WYNIK: of("kawa").equals("kawa") → false    ← pudełko to nie kawa!

        // Jak dobrze sprawdzić, czy w pudełku jest konkretna wartość?
        show("equals(Optional.of(\"kawa\"))", coffee1.equals(Optional.of("kawa")));
        show("filter(\"kawa\"::equals).isPresent()", coffee1.filter("kawa"::equals).isPresent());
        show("map(\"kawa\"::equals).orElse(false)", empty1.map("kawa"::equals).orElse(false));
        // WYNIK: equals(Optional.of("kawa")) → true
        // WYNIK: filter("kawa"::equals).isPresent() → true
        // WYNIK: map("kawa"::equals).orElse(false) → false    ← dla pustego pudełka

        show("hashCode: of(\"kawa\") / \"kawa\"", coffee1.hashCode() + " / " + "kawa".hashCode());
        show("hashCode: empty()", empty1.hashCode());
        // WYNIK: hashCode: of("kawa") / "kawa" → 3284640 / 3284640
        // WYNIK: hashCode: empty() → 0

        // Postaci toString — naucz się ich rozpoznawać na wydrukach i w debugerze:
        show("Optional.of(\"\")", Optional.of(""));
        show("Optional.of(Optional.empty())", Optional.of(Optional.empty()));
        // WYNIK: Optional.of("") → Optional[]    ← pełne pudełko z pustym napisem
        // WYNIK: Optional.of(Optional.empty()) → Optional[Optional.empty]

        // PUŁAPKA: sklejanie Optional z napisem wypisze „Optional[...]”, a nie samą wartość.
        Customer jan = SampleData.customers().get(0);
        show("źle", "Wysyłam do: " + jan.findEmail());
        show("dobrze", "Wysyłam do: " + jan.findEmail().orElse("?"));
        // WYNIK: źle → Wysyłam do: Optional[jan@example.com]
        // WYNIK: dobrze → Wysyłam do: jan@example.com
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   map(f)          — f zwraca ZWYKŁĄ wartość; null z f → puste pudełko
     *   flatMap(f)      — f zwraca OPTIONAL; brak pudełka w pudełku (Optional<Optional<T>>)
     *   filter(p)       — zostaw wartość, gdy warunek prawdziwy; inaczej puste
     *   or(() -> opt)   — zapasowy Optional (Java 9+), leniwy; orElse zwraca wartość, or — Optional
     *   stream()        — 0 albo 1 element (Java 9+); w strumieniach: flatMap(Optional::stream)
     *   Łańcuch:        find(...).flatMap(...).map(...).filter(...).orElse(...) — jedna decyzja na końcu
     *   Map.get         — zawsze Optional.ofNullable(map.get(k)), nigdy Optional.of(...)
     *   equals          — porównuje zawartość; == porównuje adresy; opt.equals("x") zawsze false
     *   toString        — Optional[x], Optional.empty, Optional[] (pusty napis), Optional[Optional.empty]
     *   Pusty krok zatrzymuje resztę łańcucha — kolejne lambdy się NIE wykonują.
     *
     * PYTANIA KONTROLNE:
     *   1. Kiedy użyjesz map, a kiedy flatMap? Po czym poznasz, że pomyliłeś jedno z drugim?
     *   2. Co wypisze:
     *          System.out.println(Optional.of("abc").map(String::length));
     *          System.out.println(Optional.of("abc").filter(s -> s.startsWith("x")));
     *   3. Co wypisze:  System.out.println(Optional.of("a").map(s -> null));  ?
     *   4. ZNAJDŹ BŁĄD:
     *          Optional<Optional<String>> email = findCustomerById(2).map(Customer::findEmail);
     *          if (email.isPresent()) {
     *              send(email.get().get());
     *          }
     *   5. Czym różni się or(() -> inny) od orElseGet(() -> inny)? Kiedy wybierzesz or?
     *   6. ZNAJDŹ BŁĄD:
     *          if (customer.findEmail() == Optional.of("jan@example.com")) { ... }
     *   7. Co wypisze:  System.out.println("Mail: " + Optional.of("x@y.pl"));  ?
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
        Customer adam = customers.get(2);
        Customer ola = customers.get(4);
        Map<String, Product> bySku = new LinkedHashMap<>();
        for (Product p : SampleData.products()) {
            bySku.put(p.sku(), p);
        }
        Map<String, String> phones = new LinkedHashMap<>();
        phones.put("Maria Nowak", "600-111-222");
        Product cleanCode = SampleData.productBySku("KSI-001");      // productBySku = produkt po SKU

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: długość e-maila Jana", Optional.of(15), () -> exercise1(jan));
        Check.equal("ćw. 1b: długość e-maila Marii", Optional.empty(), () -> exercise1(maria));
        Check.equal("ćw. 2a: KSI-001 dostępny", Optional.of(cleanCode), () -> exercise2(bySku, "KSI-001"));
        Check.equal("ćw. 2b: ELE-002 (brak w magazynie)", Optional.empty(), () -> exercise2(bySku, "ELE-002"));
        Check.equal("ćw. 3a: e-mail klienta 4", Optional.of("zofia@example.com"), () -> exercise3(customers, 4));
        Check.equal("ćw. 3b: e-mail klienta 5 (brak)", Optional.empty(), () -> exercise3(customers, 5));
        Check.equal("ćw. 4a: VIP Jan", "JAN@EXAMPLE.COM", () -> exercise4(jan));
        Check.equal("ćw. 4b: Adam (nie VIP)", "BRAK", () -> exercise4(adam));
        Check.equal("ćw. 4c: null", "BRAK", () -> exercise4(null));
        Check.equal("ćw. 5a: kontakt Jana", "e-mail: jan@example.com", () -> exercise5(jan, phones));
        Check.equal("ćw. 5b: kontakt Marii", "telefon: 600-111-222", () -> exercise5(maria, phones));
        Check.equal("ćw. 5c: kontakt Oli", "brak kontaktu", () -> exercise5(ola, phones));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", Optional.of(15), () -> solution1(jan));
        Check.equal("ćw. 1b (wzorzec)", Optional.empty(), () -> solution1(maria));
        Check.equal("ćw. 2a (wzorzec)", Optional.of(cleanCode), () -> solution2(bySku, "KSI-001"));
        Check.equal("ćw. 2b (wzorzec)", Optional.empty(), () -> solution2(bySku, "ELE-002"));
        Check.equal("ćw. 3a (wzorzec)", Optional.of("zofia@example.com"), () -> solution3(customers, 4));
        Check.equal("ćw. 3b (wzorzec)", Optional.empty(), () -> solution3(customers, 5));
        Check.equal("ćw. 4a (wzorzec)", "JAN@EXAMPLE.COM", () -> solution4(jan));
        Check.equal("ćw. 4b (wzorzec)", "BRAK", () -> solution4(adam));
        Check.equal("ćw. 4c (wzorzec)", "BRAK", () -> solution4(null));
        Check.equal("ćw. 5a (wzorzec)", "e-mail: jan@example.com", () -> solution5(jan, phones));
        Check.equal("ćw. 5b (wzorzec)", "telefon: 600-111-222", () -> solution5(maria, phones));
        Check.equal("ćw. 5c (wzorzec)", "brak kontaktu", () -> solution5(ola, phones));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 12 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć długość e-maila klienta jako Optional (pusty, gdy e-maila nie ma).
     * Podpowiedź: findEmail() i jedno wywołanie map z referencją do metody String::length.
     */
    static Optional<Integer> exercise1(Customer customer) {
        // TODO: twoje rozwiązanie
        return Optional.empty();
    }

    /**
     * ĆWICZENIE 2 (łatwe): znajdź produkt w mapie po SKU, ale zwróć go tylko wtedy, gdy jest w magazynie.
     * Brak klucza w mapie albo stan 0 → pusty Optional.
     * Podpowiedź: Optional.ofNullable(bySku.get(sku)), a potem filter z Product::inStock (in stock = w magazynie).
     */
    static Optional<Product> exercise2(Map<String, Product> bySku, String sku) {
        // TODO: twoje rozwiązanie
        return Optional.empty();
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć e-mail klienta o podanym id. Pusty Optional, gdy klienta nie ma
     * ALBO gdy klient nie ma e-maila.
     * Podpowiedź: najpierw pętlą znajdź klienta i zwróć {@code Optional<Customer>} (pomocnicza metoda albo
     * zmienna), potem flatMap(Customer::findEmail). Dlaczego nie map? Sprawdź typ wyniku.
     */
    static Optional<String> exercise3(List<Customer> customers, long id) {
        // TODO: twoje rozwiązanie
        return Optional.empty();
    }

    /**
     * ĆWICZENIE 4 (średnie): PRZEPISZ stary kod na jeden łańcuch Optional (bez if-ów i bez null-checków):
     * <pre>{@code
     * static String vipEmailUpper(Customer c) {
     *     if (c != null && c.vip()) {
     *         String email = c.email();
     *         if (email != null) {
     *             return email.toUpperCase(Locale.ROOT);
     *         }
     *     }
     *     return "BRAK";
     * }
     * }</pre>
     * Podpowiedź: Optional.ofNullable(c) → filter(...) → flatMap(...) → map(...) → orElse("BRAK").
     */
    static String exercise4(Customer customer) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 5 (trudniejsze, łączy Optional z mapami): zwróć kontakt do klienta:
     * „e-mail: adres”, gdy klient ma e-mail; w przeciwnym razie „telefon: numer”, gdy numer jest w mapie
     * phones (klucz = imię i nazwisko); w przeciwnym razie „brak kontaktu”.
     * Podpowiedź: {@code findEmail().map(e -> "e-mail: " + e)}, potem
     * {@code .or(() -> Optional.ofNullable(phones.get(...)).map(...))} i orElse(...). Metoda or jest z Javy 9.
     */
    static String exercise5(Customer customer, Map<String, String> phones) {
        // TODO: twoje rozwiązanie
        return "";
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Optional<Integer> solution1(Customer customer) {
        return customer.findEmail().map(String::length);
    }

    static Optional<Product> solution2(Map<String, Product> bySku, String sku) {
        return Optional.ofNullable(bySku.get(sku))                   // brak klucza → puste pudełko
                .filter(Product::inStock);                            // stan 0 → puste pudełko
    }

    static Optional<String> solution3(List<Customer> customers, long id) {
        Optional<Customer> found = Optional.empty();
        for (Customer c : customers) {
            if (c.id() == id) {
                found = Optional.of(c);
                break;
            }
        }
        return found.flatMap(Customer::findEmail);                    // map dałby Optional<Optional<String>>
    }

    static String solution4(Customer customer) {
        return Optional.ofNullable(customer)                          // null → puste pudełko
                .filter(Customer::vip)                                // tylko VIP
                .flatMap(Customer::findEmail)                         // findEmail zwraca Optional → flatMap
                .map(email -> email.toUpperCase(Locale.ROOT))
                .orElse("BRAK");
    }

    static String solution5(Customer customer, Map<String, String> phones) {
        return customer.findEmail()
                .map(email -> "e-mail: " + email)
                .or(() -> Optional.ofNullable(phones.get(customer.name())).map(phone -> "telefon: " + phone))
                .orElse("brak kontaktu");
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. map — gdy funkcja zwraca zwykłą wartość (String, Integer...). flatMap — gdy funkcja sama zwraca
     *      Optional (np. findEmail). Pomyłkę zdradza typ Optional<Optional<...>> albo wydruk Optional[Optional[...]].
     *   2. „Optional[3]” i „Optional.empty” — map zamienia "abc" na długość 3, a filter odrzuca "abc",
     *      bo nie zaczyna się od "x".
     *   3. „Optional.empty” — map opakowuje wynik przez ofNullable, więc null z lambdy daje puste pudełko.
     *   4. Klient 2 (Maria) nie ma e-maila: zewnętrzne pudełko jest PEŁNE (w środku leży Optional.empty),
     *      więc isPresent() daje true, a wewnętrzne get() rzuca NoSuchElementException. Poprawnie:
     *      findCustomerById(2).flatMap(Customer::findEmail).ifPresent(e -> send(e));
     *   5. orElseGet zwraca WARTOŚĆ i kończy łańcuch; or zwraca OPTIONAL (zapasowe pudełko), więc łańcuch trwa
     *      dalej. or wybierzesz, gdy zapasowe źródło też może nic nie znaleźć albo chcesz dalej użyć map/filter.
     *   6. == porównuje adresy obiektów, a findEmail() i Optional.of(...) to dwa różne pudełka — warunek jest
     *      zawsze fałszywy. Poprawnie: customer.findEmail().equals(Optional.of("jan@example.com"))
     *      albo customer.findEmail().filter("jan@example.com"::equals).isPresent().
     *   7. „Mail: Optional[x@y.pl]” — sklejanie wywołuje toString pudełka. Trzeba najpierw wyjąć wartość (orElse).
     */
    // </editor-fold>
}
