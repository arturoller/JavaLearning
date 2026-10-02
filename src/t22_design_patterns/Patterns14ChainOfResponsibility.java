package t22_design_patterns;

import helpers.Check;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wzorzec Chain of Responsibility — łańcuch odpowiedzialności
 *        (chain = łańcuch; responsibility = odpowiedzialność; handler = obsługujący; request = żądanie)
 *
 * W SKRÓCIE:
 *   Żądanie (zgłoszenie, zamówienie do sprawdzenia) przechodzi przez uporządkowaną listę obsługujących.
 *   Każde ogniwo samo decyduje: "obsługuję to" (i ewentualnie kończę) albo "przekazuję dalej". Nadawca nie wie,
 *   kto ostatecznie zajmie się żądaniem — zna tylko początek łańcucha.
 *
 * ANALOGIA: infolinia z poziomami wsparcia. Najpierw odbiera konsultant pierwszej linii; jeśli sprawa jest
 *   trudniejsza, przełącza do drugiej linii, potem do kierownika. Klient nie musi wiedzieć, kto z nich rozwiąże
 *   problem, a firma może dodać kolejną linię bez zmiany numeru telefonu.
 *
 * JAK TO DZIAŁA:
 *   Role z książki GoF (Gang of Four):
 *     Handler (obsługujący)     — wspólny interfejs ogniwa: handle(request); zwykle trzyma referencję do następnego
 *     ConcreteHandler           — konkretne ogniwo: obsługuje albo przekazuje dalej
 *     Client (klient)           — wysyła żądanie do PIERWSZEGO ogniwa
 *
 *     Client ──żądanie──▶ [Ogniwo 1] ──nie moje──▶ [Ogniwo 2] ──nie moje──▶ [Ogniwo 3] ──▶ (koniec: nikt?)
 *                              │                       │                        │
 *                           obsługuję               obsługuję                obsługuję
 *
 *   Dwa rodzaje semantyki (sekcja 3):
 *     "pierwszy wygrywa" (first wins) — pierwsze pasujące ogniwo obsługuje i łańcuch się kończy (eskalacja, reguły rabatów),
 *     "wszyscy uczestniczą" (all participate) — każde ogniwo coś robi i przekazuje dalej, chyba że zatrzyma żądanie
 *        (walidacja, filtry serwletów, logowanie).
 *
 * SŁÓWKA: chain = łańcuch; handler = obsługujący; next = następny; escalation = eskalacja; filter = filtr;
 *   middleware = warstwa pośrednia; validation = walidacja (sprawdzanie poprawności); unhandled = nieobsłużone;
 *   short-circuit = skrót (przerwanie przed końcem); priority = priorytet
 *
 * ZOBACZ TEŻ: t22_design_patterns/Patterns05TemplateMethod (handle() jest metodą szablonową),
 *   t22_design_patterns/Patterns07Decorator (też opakowuje następny obiekt, ale ZAWSZE go woła),
 *   t13_lambdas/Lambda04MethodReferences (Function/Predicate jako ogniwa), t14_optional/Optional01Basics,
 *   t22_design_patterns/Patterns09Command (żądanie jako obiekt)
 * </pre>
 */
public class Patterns14ChainOfResponsibility {

    public static void main(String[] args) {
        title("Patterns14 — Chain of Responsibility: łańcuch odpowiedzialności");

        problemOneBigMethod();         // problem one big method = problem: jedna wielka metoda
        classicChain();                // classic chain = klasyczny łańcuch (eskalacja zgłoszeń)
        stopOrContinue();              // stop or continue = zatrzymać czy kontynuować
        middlewareStyle();             // middleware style = styl filtrów (ogniwo samo woła następne)
        functionalChains();            // functional chains = łańcuchy funkcyjne
        endOfTheChain();               // end of the chain = koniec łańcucha
        orderMatters();                // order matters = kolejność ma znaczenie
        chainsInTheJdkAndSpring();     // chains in the jdk and spring = łańcuchy w JDK i Springu
        testability();                 // testability = testowalność
        pitfallsAndWhenToUse();        // pitfalls and when to use = pułapki i kiedy stosować
        exercises();                   // exercises = ćwiczenia
    }

    // =================================================================================================
    // Wspólne typy lekcji
    // =================================================================================================

    /** Zgłoszenie do obsługi technicznej: tytuł i waga 1 (drobna) do 5 (awaria krytyczna). */
    record Ticket(String title, int severity) { }  // ticket = zgłoszenie; severity = waga

    /** Odpowiedź: kto obsłużył i co zrobił. */
    record Response(String handledBy, String text) { }  // handled by = obsłużone przez

    // =================================================================================================
    // 1. PROBLEM: jedna wielka metoda
    // =================================================================================================

    /** Obsługa zgłoszeń "na twardo": kto za co odpowiada — zaszyte w jednej metodzie. */
    static String handleOld(Ticket ticket) {  // old = stary
        if (ticket.severity() <= 1) {
            return "Pierwsza linia: " + ticket.title();
        } else if (ticket.severity() <= 3) {
            return "Druga linia: " + ticket.title();
        } else if (ticket.severity() <= 4) {
            return "Kierownik: " + ticket.title();
        }
        return "BRAK ODPOWIEDZI";  // magiczny napis zamiast typu wyniku
    }

    /** Nocna zmiana nie ma drugiej linii — trzeba skopiować CAŁĄ metodę i usunąć jedną gałąź. */
    static String handleOldNight(Ticket ticket) {  // night = noc
        if (ticket.severity() <= 1) {
            return "Pierwsza linia: " + ticket.title();
        } else if (ticket.severity() <= 4) {
            return "Kierownik: " + ticket.title();
        }
        return "BRAK ODPOWIEDZI";
    }

    /**
     * 1. Kto obsługuje które zgłoszenie — to wiedza zaszyta w jednej metodzie. Nowy poziom (dyrektor) = edycja metody;
     * inna konfiguracja (nocna zmiana) = kopia całej metody. Dwie kopie rozjadą się przy pierwszej zmianie progu.
     */
    static void problemOneBigMethod() {
        section("1. Problem: jedna wielka metoda i jej kopie");

        Ticket ticket = new Ticket("drukarka nie działa", 3);
        show("dzień", handleOld(ticket));
        // WYNIK: dzień → Druga linia: drukarka nie działa
        show("noc", handleOldNight(ticket));
        // WYNIK: noc → Kierownik: drukarka nie działa
        show("awaria krytyczna", handleOld(new Ticket("serwer leży", 5)));
        // WYNIK: awaria krytyczna → BRAK ODPOWIEDZI

        // PUŁAPKA: zgłoszenie o wadze 3 trafiło w dzień do drugiej linii, a w nocy — do kierownika. To akurat zamierzone,
        // ale progi (1 i 4) są teraz w DWÓCH miejscach i trzeba je zmieniać razem. Do tego wynik "BRAK ODPOWIEDZI" to
        // zwykły napis, który ktoś może pomylić z prawdziwą odpowiedzią.
        // DOBRA PRAKTYKA: wydziel każde ogniwo ("kto i kiedy obsługuje") do osobnego obiektu i składaj z nich łańcuch.
    }

    // =================================================================================================
    // 2. KLASYCZNY ŁAŃCUCH: eskalacja zgłoszeń
    // =================================================================================================

    /**
     * HANDLER (obsługujący) — klasa bazowa ogniwa. {@code handle} jest metodą szablonową (Patterns05TemplateMethod):
     * sprawdź, czy to moje (canHandle); jeśli tak — obsłuż; jeśli nie — przekaż następnemu; jeśli brak następnego — pusty wynik.
     */
    abstract static class SupportHandler {  // support = wsparcie techniczne
        private SupportHandler next;  // next = następne ogniwo

        /** Ustawia następne ogniwo i ZWRACA je, więc można pisać {@code a.linkTo(b).linkTo(c)}. */
        SupportHandler linkTo(SupportHandler nextHandler) {  // link to = połącz z
            this.next = nextHandler;
            return nextHandler;
        }

        final Optional<Response> handle(Ticket ticket) {  // final: nikt nie zmieni zasady "obsłuż albo przekaż"
            if (canHandle(ticket)) {
                return Optional.of(new Response(name(), process(ticket)));
            }
            return next == null ? Optional.empty() : next.handle(ticket);  // Optional.empty = brak wyniku
        }

        abstract boolean canHandle(Ticket ticket);  // can handle = czy potrafi obsłużyć
        abstract String name();                     // name = nazwa ogniwa
        abstract String process(Ticket ticket);     // process = obsłuż
    }

    /** ConcreteHandler 1: drobne sprawy (waga do 1). */
    static class FirstLine extends SupportHandler {
        @Override boolean canHandle(Ticket ticket) { return ticket.severity() <= 1; }
        @Override String name() { return "Pierwsza linia"; }
        @Override String process(Ticket ticket) { return "podpowiedź z FAQ do: " + ticket.title(); }  // FAQ = często zadawane pytania
    }

    /** ConcreteHandler 2: sprawy średnie (waga do 3). */
    static class SecondLine extends SupportHandler {
        @Override boolean canHandle(Ticket ticket) { return ticket.severity() <= 3; }
        @Override String name() { return "Druga linia"; }
        @Override String process(Ticket ticket) { return "diagnoza zdalna: " + ticket.title(); }
    }

    /** ConcreteHandler 3: poważne sprawy (waga do 4). */
    static class Manager extends SupportHandler {
        @Override boolean canHandle(Ticket ticket) { return ticket.severity() <= 4; }
        @Override String name() { return "Kierownik"; }
        @Override String process(Ticket ticket) { return "decyzja kierownika: " + ticket.title(); }
    }

    /** Buduje łańcuch: pierwsza → druga → kierownik. Zwraca POCZĄTEK łańcucha. */
    static SupportHandler supportChain() {
        SupportHandler first = new FirstLine();
        first.linkTo(new SecondLine()).linkTo(new Manager());  // linkTo zwraca dodane ogniwo, więc łączymy dalej
        return first;
    }

    /**
     * 2. Klasyczny łańcuch krok po kroku. Klient zna tylko początek łańcucha, a ogniwa znają tylko swojego następcę.
     * Zmiana konfiguracji (nocna zmiana bez drugiej linii) to inne POŁĄCZENIE ogniw, bez kopiowania logiki.
     */
    static void classicChain() {
        section("2. Klasyczny łańcuch: eskalacja zgłoszeń");

        SupportHandler chain = supportChain();
        for (int severity : new int[] {1, 3, 4}) {
            Optional<Response> response = chain.handle(new Ticket("problem", severity));
            show("waga " + severity, response.map(Response::handledBy).orElse("nikt"));  // map/orElse = przekształć/lub domyślna
            // WYNIK: waga 1 → Pierwsza linia
            // WYNIK: waga 3 → Druga linia
            // WYNIK: waga 4 → Kierownik
        }

        // Nocna zmiana: ta sama klasa FirstLine i Manager, tylko inne połączenie — bez nowej metody i bez kopii.
        SupportHandler night = new FirstLine();
        night.linkTo(new Manager());
        show("noc, waga 3", night.handle(new Ticket("problem", 3)).map(Response::handledBy).orElse("nikt"));
        // WYNIK: noc, waga 3 → Kierownik

        show("odpowiedź w całości", chain.handle(new Ticket("drukarka", 2)).orElseThrow());  // orElseThrow() = pobierz lub rzuć (Java 10+)
        // WYNIK: odpowiedź w całości → Response[handledBy=Druga linia, text=diagnoza zdalna: drukarka]
        // DOBRA PRAKTYKA: ogniwo zwraca WYNIK (Optional), a nie void — wtedy widać, czy ktoś obsłużył. Ukryty efekt
        // uboczny ("wysłałem maila i tyle") trudno przetestować.
    }

    // =================================================================================================
    // 3. ZATRZYMAĆ CZY KONTYNUOWAĆ: walidacja zamówienia
    // =================================================================================================

    /** Żądanie do sprawdzenia: dane zamówienia przed zapisaniem. */
    record OrderRequest(String email, int quantity, BigDecimal amount, String country) { }  // amount = kwota; country = kraj

    /** Reguła walidacji (jedno ogniwo): zwraca komunikat błędu albo Optional.empty(), gdy wszystko w porządku. */
    interface Rule {
        Optional<String> check(OrderRequest request);  // check = sprawdź
    }

    private static final Set<String> SUPPORTED_COUNTRIES = Set.of("PL", "DE");  // supported = obsługiwane; Set.of (Java 9+)

    /** Lista reguł = ogniwa łańcucha (zamiast powiązanych obiektów używamy zwykłej listy). */
    static List<Rule> validationRules() {
        return List.of(
                request -> request.email().contains("@") ? Optional.empty() : Optional.of("e-mail bez znaku @"),
                request -> request.quantity() >= 1 && request.quantity() <= 100
                        ? Optional.empty() : Optional.of("ilość poza zakresem 1..100: " + request.quantity()),
                request -> request.amount().signum() > 0 ? Optional.empty() : Optional.of("kwota musi być dodatnia"),  // signum = znak liczby
                request -> SUPPORTED_COUNTRIES.contains(request.country())
                        ? Optional.empty() : Optional.of("kraj nieobsługiwany: " + request.country()));
    }

    /** Semantyka "pierwszy błąd kończy": łańcuch zatrzymuje się na pierwszym ogniwie, które zgłosi problem. */
    static Optional<String> firstError(List<Rule> rules, OrderRequest request) {
        for (Rule rule : rules) {
            Optional<String> error = rule.check(request);
            if (error.isPresent()) {
                return error;  // stop: dalsze reguły nie są wołane
            }
        }
        return Optional.empty();
    }

    /** Semantyka "wszyscy uczestniczą": każda reguła jest wołana, błędy zbieramy. */
    static List<String> allErrors(List<Rule> rules, OrderRequest request) {
        return rules.stream()
                .map(rule -> rule.check(request))
                .flatMap(Optional::stream)  // Optional.stream (Java 9+): pusty → 0 elementów, pełny → 1 element
                .toList();                  // toList = do listy (Java 16+)
    }

    /**
     * 3. Ten sam zestaw ogniw można zinterpretować na dwa sposoby. Gdy sprawdzamy WSZYSTKO, użytkownik dostaje pełną listę
     * błędów naraz (formularz). Gdy przerywamy na pierwszym — oszczędzamy pracę (np. drogie sprawdzenie w bazie danych
     * robimy dopiero po tanich).
     */
    static void stopOrContinue() {
        section("3. Zatrzymać czy kontynuować: walidacja zamówienia");

        List<Rule> rules = validationRules();
        OrderRequest good = new OrderRequest("jan@example.com", 2, new BigDecimal("99.90"), "PL");
        OrderRequest bad = new OrderRequest("jan.example.com", 0, new BigDecimal("-5"), "PL");
        OrderRequest foreign = new OrderRequest("ola@example.com", 3, BigDecimal.TEN, "US");

        show("poprawne: pierwszy błąd", firstError(rules, good));
        // WYNIK: poprawne: pierwszy błąd → Optional.empty
        show("złe: pierwszy błąd", firstError(rules, bad));
        // WYNIK: złe: pierwszy błąd → Optional[e-mail bez znaku @]
        showEach("złe: wszystkie błędy", allErrors(rules, bad));
        // WYNIK: złe: wszystkie błędy (liczba elementów: 3):
        // WYNIK: • e-mail bez znaku @
        // WYNIK: • ilość poza zakresem 1..100: 0
        // WYNIK: • kwota musi być dodatnia
        show("obce: wszystkie błędy", allErrors(rules, foreign));
        // WYNIK: obce: wszystkie błędy → [kraj nieobsługiwany: US]

        // PUŁAPKA: pomylenie semantyk. Jeśli dla formularza użyjesz "pierwszy błąd kończy", użytkownik poprawia
        // jedno pole, odsyła i dostaje następny błąd — poprawiając formularz pięć razy zamiast raz.
        // DOBRA PRAKTYKA: reguły niezależne od siebie zbieraj wszystkie; reguły zależne ("najpierw sprawdź, czy
        // klient istnieje, dopiero potem jego limit") ustaw w kolejności i przerywaj na pierwszym błędzie.
    }

    // =================================================================================================
    // 4. STYL FILTRÓW: ogniwo samo woła następne
    // =================================================================================================

    /** Następne ogniwo widziane jako funkcja: przyjmuje żądanie (tekst), zwraca odpowiedź (tekst). */
    interface Next {
        String call(String request);  // call = wywołaj
    }

    /** Ogniwo-filtr (middleware): dostaje żądanie ORAZ następne ogniwo i samo decyduje, czy je wywołać. */
    interface Middleware {
        String handle(String request, Next next);
    }

    /** Składa listę ogniw w jedną funkcję. Od końca: ostatnie ogniwo owija "właściwą" obsługę, przedostatnie — to, itd. */
    static Next chain(List<Middleware> middlewares, Next last) {  // last = ostatni, właściwy odbiorca
        Next current = last;
        for (int i = middlewares.size() - 1; i >= 0; i--) {
            Middleware middleware = middlewares.get(i);
            Next following = current;  // zmienna efektywnie finalna dla lambdy (following = następne)
            current = request -> middleware.handle(request, following);
        }
        return current;
    }

    /**
     * 4. W wersji z sekcji 2 ogniwo "obsługuje albo przekazuje". W stylu filtrów (jak {@code Filter.doFilter} w serwletach)
     * ogniwo dostaje referencję do następnego i może: (a) coś zrobić PRZED, wywołać następne i coś zrobić PO,
     * (b) przerwać łańcuch (nie wołać następnego i zwrócić własną odpowiedź). To elastyczniejsze, bo ogniwo
     * widzi też odpowiedź idącą z powrotem.
     */
    static void middlewareStyle() {
        section("4. Styl filtrów: ogniwo samo woła następne");

        List<String> log = new ArrayList<>();  // log = dziennik zdarzeń (do pokazania kolejności)

        Middleware logging = (request, next) -> {  // logging = logowanie: przed i po
            log.add("→ " + request);
            String response = next.call(request);
            log.add("← " + response);
            return response;
        };
        Middleware auth = (request, next) -> {  // auth = autoryzacja (sprawdzenie uprawnień)
            if (!request.startsWith("user:")) {
                log.add("auth: odmowa");
                return "401 brak dostępu";  // przerywamy: next.call NIE jest wołane
            }
            log.add("auth: ok");
            return next.call(request);
        };
        Next core = request -> "200 OK dla " + request;  // core = właściwa obsługa

        Next pipeline = chain(List.of(logging, auth), core);  // pipeline = potok (rurociąg)

        show("żądanie z użytkownikiem", pipeline.call("user:ola"));
        // WYNIK: żądanie z użytkownikiem → 200 OK dla user:ola
        show("żądanie anonimowe", pipeline.call("anon"));
        // WYNIK: żądanie anonimowe → 401 brak dostępu
        showEach("dziennik", log);
        // WYNIK: dziennik (liczba elementów: 6):
        // WYNIK: • → user:ola
        // WYNIK: • auth: ok
        // WYNIK: • ← 200 OK dla user:ola
        // WYNIK: • → anon
        // WYNIK: • auth: odmowa
        // WYNIK: • ← 401 brak dostępu

        // DOBRA PRAKTYKA: kolejność ma znaczenie — logowanie przed autoryzacją zapisze też odrzucone żądania,
        // a po niej tylko dopuszczone. Wybierz świadomie.
        // W prawdziwych aplikacjach tak działa filtr serwletowy: chain.doFilter(request, response) oznacza
        // "przekaż dalej"; niewywołanie go przerywa łańcuch (sekcja 8).
    }

    // =================================================================================================
    // 5. ŁAŃCUCHY FUNKCYJNE
    // =================================================================================================

    private static final BigDecimal COUPON_FACTOR = new BigDecimal("0.90");    // factor = współczynnik: -10%
    private static final BigDecimal LOYALTY_FACTOR = new BigDecimal("0.95");   // loyalty = lojalność: -5%
    private static final BigDecimal BIG_ORDER_LIMIT = new BigDecimal("1000");  // big order = duże zamówienie
    private static final BigDecimal BIG_ORDER_BONUS = new BigDecimal("20");    // bonus = zniżka kwotowa

    /** Kupujący do reguł rabatowych. */
    record Buyer(String name, boolean vip, String coupon) { }  // buyer = kupujący; coupon = kupon

    /**
     * 5. Gdy ogniwa to małe funkcje, łańcuch to ich składanie. (a) "Wszyscy uczestniczą" = potok funkcji:
     * {@code andThen} składa funkcje jedna po drugiej. (b) "Pierwszy wygrywa" = strumień reguł i {@code findFirst}.
     * Zero klas i zero pól {@code next}.
     */
    static void functionalChains() {
        section("5. Łańcuchy funkcyjne: andThen i findFirst");

        // (a) potok: każda funkcja przekształca cenę i oddaje następnej
        UnaryOperator<BigDecimal> coupon = price -> price.multiply(COUPON_FACTOR);          // UnaryOperator = funkcja T → T
        UnaryOperator<BigDecimal> loyalty = price -> price.multiply(LOYALTY_FACTOR);
        UnaryOperator<BigDecimal> bigOrder = price -> price.compareTo(BIG_ORDER_LIMIT) >= 0
                ? price.subtract(BIG_ORDER_BONUS) : price;

        Function<BigDecimal, BigDecimal> pipeline = Function.identity();  // identity = funkcja neutralna (zwraca to, co dostała)
        for (UnaryOperator<BigDecimal> step : List.of(coupon, loyalty, bigOrder)) {
            pipeline = pipeline.andThen(step);  // andThen = "a potem"
        }
        pipeline = pipeline.andThen(price -> price.setScale(2, RoundingMode.HALF_UP));  // na końcu zaokrąglamy do groszy

        show("cena 1200.00 po potoku", pipeline.apply(new BigDecimal("1200.00")));
        // WYNIK: cena 1200.00 po potoku → 1006.00
        show("cena 100.00 po potoku", pipeline.apply(new BigDecimal("100.00")));
        // WYNIK: cena 100.00 po potoku → 85.50

        // (b) pierwszy wygrywa: lista reguł zwracających Optional, bierzemy pierwszy niepusty wynik
        List<Function<Buyer, Optional<Integer>>> discountRules = List.of(
                buyer -> buyer.vip() ? Optional.of(15) : Optional.empty(),
                buyer -> "WIOSNA".equals(buyer.coupon()) ? Optional.of(10) : Optional.empty());

        show("VIP", firstDiscount(discountRules, new Buyer("Ala", true, null)) + "%");
        // WYNIK: VIP → 15%
        show("kupon", firstDiscount(discountRules, new Buyer("Bartek", false, "WIOSNA")) + "%");
        // WYNIK: kupon → 10%
        show("nic", firstDiscount(discountRules, new Buyer("Celina", false, null)) + "%");
        // WYNIK: nic → 0%

        // PUŁAPKA: operacje na BigDecimal są niezmienne — {@code price.multiply(...)} zwraca NOWY obiekt. Pamiętaj
        // przypisać wynik (tu robi to andThen). Zaokrąglaj RAZ, na końcu — zaokrąglanie po każdym kroku daje inne grosze.
    }

    /** Pierwszy rabat z reguł, które cokolwiek zwróciły; brak = 0. */
    static int firstDiscount(List<Function<Buyer, Optional<Integer>>> rules, Buyer buyer) {
        return rules.stream()
                .map(rule -> rule.apply(buyer))
                .flatMap(Optional::stream)
                .findFirst()  // findFirst = znajdź pierwszy (strumień jest leniwy: dalsze reguły nie są liczone)
                .orElse(0);
    }

    // =================================================================================================
    // 6. KONIEC ŁAŃCUCHA: nikt nie obsłużył
    // =================================================================================================

    /** Ostatnie ogniwo "łap wszystko": nie ma zgłoszenia, którego nie obsłuży. */
    static class Director extends SupportHandler {  // director = dyrektor
        @Override boolean canHandle(Ticket ticket) { return true; }
        @Override String name() { return "Dyrektor"; }
        @Override String process(Ticket ticket) { return "plan awaryjny dla: " + ticket.title(); }
    }

    /**
     * 6. Co, jeśli żadne ogniwo nie pasuje? W naszym łańcuchu zgłoszenie o wadze 5 "wypada" poza koniec. Są trzy
     * rozsądne wybory: (1) zwrócić wynik opcjonalny i kazać klientowi zdecydować (Optional), (2) rzucić wyjątek,
     * (3) dodać ogniwo domyślne ("łap wszystko") na końcu.
     */
    static void endOfTheChain() {
        section("6. Koniec łańcucha: nikt nie obsłużył");

        SupportHandler chain = supportChain();
        Ticket critical = new Ticket("serwer leży", 5);

        show("bez ogniwa domyślnego", chain.handle(critical));
        // WYNIK: bez ogniwa domyślnego → Optional.empty
        expectThrows("orElseThrow", () -> chain.handle(critical).orElseThrow(
                () -> new IllegalStateException("nikt nie obsłużył zgłoszenia: " + critical.title())));  // orElseThrow(dostawca) = pobierz lub rzuć własny wyjątek
        // WYNIK: ✔ orElseThrow → rzucono IllegalStateException: nikt nie obsłużył zgłoszenia: serwer leży

        SupportHandler withDefault = new FirstLine();
        withDefault.linkTo(new SecondLine()).linkTo(new Manager()).linkTo(new Director());  // dyrektor na końcu
        show("z ogniwem domyślnym", withDefault.handle(critical).map(Response::handledBy).orElse("nikt"));
        // WYNIK: z ogniwem domyślnym → Dyrektor

        // PUŁAPKA: łańcuch, który po cichu gubi żądania (zwraca null albo "nic"), to klasyczny błąd tego wzorca —
        // zgłoszenie znika, a nikt o tym nie wie.
        // DOBRA PRAKTYKA: zawsze zdecyduj, co dzieje się na końcu łańcucha, i zapisz to w nazwie lub dokumentacji
        // (ogniwo domyślne, wyjątek albo jawny wynik opcjonalny).
    }

    // =================================================================================================
    // 7. KOLEJNOŚĆ MA ZNACZENIE
    // =================================================================================================

    /** Reguła rabatowa z priorytetem: niższa liczba = sprawdzana wcześniej (jak {@code @Order} w Springu). */
    record PrioritizedRule(int priority, String name, Function<Buyer, Optional<Integer>> rule) { }  // priority = priorytet

    /**
     * 7. W semantyce "pierwszy wygrywa" kolejność ogniw zmienia WYNIK. Kupujący jest VIP-em i ma kupon: jeśli
     * reguła kuponu stoi przed regułą VIP, dostaje 10%, w odwrotnej kolejności 15%. Kolejność trzeba więc ustalić świadomie
     * i najlepiej zapisać jawnie (priorytety), zamiast polegać na przypadkowej kolejności w kodzie.
     */
    static void orderMatters() {
        section("7. Kolejność ogniw zmienia wynik");

        Function<Buyer, Optional<Integer>> vipRule = buyer -> buyer.vip() ? Optional.of(15) : Optional.empty();
        Function<Buyer, Optional<Integer>> couponRule = buyer -> buyer.coupon() != null ? Optional.of(10) : Optional.empty();
        Buyer buyer = new Buyer("Dorota", true, "WIOSNA");

        show("kupon przed VIP", firstDiscount(List.of(couponRule, vipRule), buyer) + "%");
        // WYNIK: kupon przed VIP → 10%
        show("VIP przed kuponem", firstDiscount(List.of(vipRule, couponRule), buyer) + "%");
        // WYNIK: VIP przed kuponem → 15%

        // Jawne priorytety: lista nieuporządkowana + sortowanie po priorytecie daje przewidywalną kolejność.
        List<PrioritizedRule> rules = new ArrayList<>(List.of(
                new PrioritizedRule(20, "kupon", couponRule),
                new PrioritizedRule(10, "VIP", vipRule)));
        rules.sort(Comparator.comparingInt(PrioritizedRule::priority));  // comparingInt = porównaj po liczbie int
        show("kolejność po priorytetach", rules.stream().map(PrioritizedRule::name).toList());
        // WYNIK: kolejność po priorytetach → [VIP, kupon]
        show("wynik po priorytetach", firstDiscount(rules.stream().map(PrioritizedRule::rule).toList(), buyer) + "%");
        // WYNIK: wynik po priorytetach → 15%

        // PUŁAPKA: ukryta zależność kolejności. Ktoś "porządkuje" listę alfabetycznie albo dodaje regułę na końcu —
        // i zmienia wyniki. DOBRA PRAKTYKA: test, który pilnuje kolejności (sekcja 9), i priorytety w danych.
        // W walidacji: tanie sprawdzenia (format) PRZED drogimi (zapytanie do bazy).
    }

    // =================================================================================================
    // 8. ŁAŃCUCHY W JDK I W SPRINGU
    // =================================================================================================

    /** Klasyfikacja wyjątku przez kolejne bloki catch — to też łańcuch: pierwszy pasujący blok obsługuje. */
    static String classify(Runnable action) {  // classify = sklasyfikuj
        try {
            action.run();
            return "bez błędu";
        } catch (NumberFormatException e) {            // najwęższy wyjątek
            return "zły format liczby";
        } catch (IllegalArgumentException e) {         // NumberFormatException jest jego podklasą
            return "zły argument";
        } catch (RuntimeException e) {                 // najszerszy
            return "inny błąd wykonania";
        }
    }

    /**
     * 8. Gdzie spotkasz ten wzorzec: bloki {@code catch} (pierwszy pasujący wygrywa; odwrócenie kolejności szerszy-węższy
     * kompilator odrzuca jako błąd). W serwerach WWW: filtry serwletów ({@code Filter.doFilter(request, response, chain)}
     * woła {@code chain.doFilter}, żeby kontynuować). W Springu: {@code HandlerInterceptor.preHandle} zwraca
     * {@code boolean} — {@code false} przerywa łańcuch; Spring Security to łańcuch filtrów bezpieczeństwa
     * (uwierzytelnianie, autoryzacja, CSRF...) — patrz SpringLearning. W bibliotekach logowania: rejestratory (loggery)
     * mają "rodziców", a komunikat wędruje w górę hierarchii do kolejnych obsługujących (handlerów).
     */
    static void chainsInTheJdkAndSpring() {
        section("8. Łańcuchy w JDK i w Springu");

        show("parseInt(\"x\")", classify(() -> Integer.parseInt("x")));
        // WYNIK: parseInt("x") → zły format liczby
        show("new ArrayList(-1)", classify(() -> new ArrayList<String>(-1)));
        // WYNIK: new ArrayList(-1) → zły argument
        show("List.of().get(0)", classify(() -> List.of().get(0)));
        // WYNIK: List.of().get(0) → inny błąd wykonania
        show("poprawny kod", classify(() -> { }));
        // WYNIK: poprawny kod → bez błędu

        // PUŁAPKA: w bloku catch kolejność ma znaczenie w OBIE strony: węższy przed szerszym działa, odwrotnie —
        // błąd kompilacji "exception has already been caught" (wyjątek już przechwycony).
        // Po stronie Springa pamiętaj: interceptor, który zwraca false, ale nie wysyła żadnej odpowiedzi, zostawia
        // klienta bez odpowiedzi — ta sama pułapka "żądanie znika" co w sekcji 6.
        note("catch, filtr serwletu, interceptor Springa: ten sam wzorzec w trzech przebraniach");
        // WYNIK: ℹ catch, filtr serwletu, interceptor Springa: ten sam wzorzec w trzech przebraniach
    }

    // =================================================================================================
    // 9. TESTOWALNOŚĆ
    // =================================================================================================

    /** Atrapa ogniwa do testu: zapisuje, że została zapytana, i odpowiada zgodnie z konfiguracją. */
    static class Recording extends SupportHandler {  // recording = nagrywające
        private final String id;
        private final boolean accepts;  // accepts = przyjmuje (obsługuje) wszystko
        private final List<String> calls;  // calls = wywołania

        Recording(String id, boolean accepts, List<String> calls) {
            this.id = id;
            this.accepts = accepts;
            this.calls = calls;
        }

        @Override boolean canHandle(Ticket ticket) { calls.add(id); return accepts; }
        @Override String name() { return id; }
        @Override String process(Ticket ticket) { return "obsłużone przez " + id; }
    }

    /**
     * 9. Każde ogniwo testujesz osobno (podaj zgłoszenie, sprawdź canHandle/process), a cały łańcuch — z atrapami,
     * które zapisują, kto został zapytany. Test sprawdza kolejność i przerwanie, bez żadnej "prawdziwej" obsługi.
     */
    static void testability() {
        section("9. Testowalność: atrapy i zapis wywołań");

        List<String> calls = new ArrayList<>();
        SupportHandler a = new Recording("A", false, calls);
        a.linkTo(new Recording("B", true, calls)).linkTo(new Recording("C", true, calls));

        Optional<Response> response = a.handle(new Ticket("test", 1));
        Check.equal("kto obsłużył", "B", () -> response.map(Response::handledBy).orElse("nikt"));
        Check.equal("kto został zapytany (C nie)", List.of("A", "B"), () -> calls);
        Check.isTrue("pojedyncze ogniwo osobno", new SecondLine().canHandle(new Ticket("x", 3)));
        Check.isTrue("pojedyncze ogniwo osobno (za trudne)", !new SecondLine().canHandle(new Ticket("x", 4)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD

        // DOBRA PRAKTYKA: test, który pilnuje, że łańcuch PRZERYWA się po pierwszym obsługującym (C nie jest pytane),
        // chroni przed cichym zmienieniem semantyki "pierwszy wygrywa" na "wszyscy uczestniczą".
    }

    // =================================================================================================
    // 10. PUŁAPKI I KIEDY STOSOWAĆ
    // =================================================================================================

    /**
     * 10. Typowe błędy i decyzja.
     */
    static void pitfallsAndWhenToUse() {
        section("10. Pułapki i kiedy stosować");

        // PUŁAPKA: żądanie wypada z łańcucha (sekcja 6) — zdecyduj, co dzieje się na końcu.
        // PUŁAPKA: zależność od kolejności (sekcja 7) — jawne priorytety i testy.
        // PUŁAPKA: cykl — jeśli ogniwo A wskazuje B, a B wskazuje A, zgłoszenie krąży bez końca i kończy się
        //   StackOverflowError. Łańcuch buduj w JEDNYM miejscu (fabryka), a nie rozrzucaj linkTo po kodzie.
        // PUŁAPKA: trudna diagnoza ("kto to obsłużył?"). Dodaj nazwę ogniwa do odpowiedzi (Response.handledBy)
        //   albo zapisuj przejścia w dzienniku, jak w sekcji 4.
        // PUŁAPKA: ogniwa zmieniające to samo zmienne żądanie — kolejność zmienia skutki. Wolisz niezmienne żądanie
        //   (rekord) i odpowiedź jako nowy obiekt.
        // PUŁAPKA: wzorzec tam, gdzie wiadomo, kto ma obsłużyć (YAGNI = "nie będziesz tego potrzebował"). Jeśli rodzaj
        //   żądania jednoznacznie wskazuje obsługującego, wystarczy mapa Map<Typ, Obsługa> (strategia).
        note("łańcuch dobry, gdy NIE WIADOMO z góry, kto obsłuży; mapa — gdy wiadomo");
        // WYNIK: ℹ łańcuch dobry, gdy NIE WIADOMO z góry, kto obsłuży; mapa — gdy wiadomo

        // KIEDY UŻYWAĆ / KIEDY NIE:
        //   używaj:  wielu możliwych obsługujących i nie wiadomo z góry który; zestaw ogniw ma być konfigurowalny
        //            i zmienny; kolejne kroki walidacji, filtrowania, rabaty "pierwsza pasująca reguła", eskalacja.
        //   nie używaj: zawsze działa jedno znane ogniwo (zwykłe wywołanie); wszystkie kroki muszą wykonać się zawsze
        //            w ustalonej kolejności bez możliwości przerwania (to zwykły potok albo dekorator); gdy
        //            potrzebujesz gwarancji, że żądanie na pewno zostanie obsłużone (łatwiej bez łańcucha).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Chain of Responsibility: żądanie wędruje przez ogniwa; każde obsługuje je ALBO przekazuje dalej.
     *   • Role: Handler (obsługujący, zna następnego), ConcreteHandler (konkretne ogniwo), Client (zna początek).
     *   • "Pierwszy wygrywa" (eskalacja, rabaty) kontra "wszyscy uczestniczą" (walidacja, filtry) — ustal semantykę.
     *   • Łańcuch można zrobić z powiązanych obiektów (next) albo z listy i pętli — lista jest prostsza i łatwiejsza do zmiany.
     *   • Styl filtrów: ogniwo dostaje następne i samo decyduje, czy je wywołać (przed/po, przerwanie).
     *   • Funkcyjnie: Function.andThen (potok), Stream + Optional.stream + findFirst (pierwsza pasująca reguła).
     *   • Zawsze określ, co się dzieje na końcu łańcucha (ogniwo domyślne, wyjątek, Optional).
     *   • Kolejność ogniw zmienia wynik: jawne priorytety i testy; tanie sprawdzenia przed drogimi.
     *   • W JDK i Springu: bloki catch, filtry serwletów, HandlerInterceptor, Spring Security, loggery.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się łańcuch "pierwszy wygrywa" od "wszyscy uczestniczą"? Podaj po jednym przykładzie.
     *   2. Dlaczego warto, żeby ogniwo zwracało wynik (np. Optional<Response>), a nie tylko "coś robiło"?
     *   3. Co wypisze:  supportChain().handle(new Ticket("x", 2)).map(Response::handledBy).orElse("nikt")  ?
     *   4. Co wypisze:  supportChain().handle(new Ticket("x", 9)).isPresent()  ?
     *   5. ZNAJDŹ BŁĄD:  SupportHandler a = new FirstLine(); SupportHandler b = new SecondLine();
     *      a.linkTo(b); b.linkTo(a);  — zgłoszenie o wadze 5 trafia do a. Co się stanie?
     *   6. ZNAJDŹ BŁĄD:  middleware sprawdzający uprawnienia zwraca "401" w przypadku braku dostępu, ale przed tym
     *      wywołuje next.call(request). Co jest nie tak?
     *   7. Dlaczego kolejność reguł rabatowych ma znaczenie, a kolejność niezależnych reguł walidacji z sekcji 3 (zbieranie
     *      wszystkich błędów) wpływa tylko na kolejność komunikatów?
     *   8. Kiedy zamiast łańcucha lepsza jest zwykła mapa?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: waga 1", "Pierwsza linia", () -> exercise1(1));
        Check.equal("ćw. 1: waga 3", "Druga linia", () -> exercise1(3));
        Check.equal("ćw. 1: waga 5", "nikt", () -> exercise1(5));
        Check.equal("ćw. 2: limit 2 błędy", List.of("e-mail bez znaku @", "ilość poza zakresem 1..100: 0"),
                () -> exercise2(validationRules(), badRequest(), 2));
        Check.equal("ćw. 3: 50 → automat", "automat", () -> exercise3(approvers(), 50));
        Check.equal("ćw. 3: 5000 → kierownik", "kierownik", () -> exercise3(approvers(), 5000));
        Check.equal("ćw. 3: 20000 → brak", "brak", () -> exercise3(approvers(), 20000));
        Check.equal("ćw. 4: A(B(core))", "A(B(core:x))", () -> exercise4(wrappers(), core()).call("x"));
        Check.equal("ćw. 4: przerwanie", "STOP", () -> exercise4(List.of(stopper(), wrappers().get(0)), core()).call("x"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "Pierwsza linia", () -> solution1(1));
        Check.equal("ćw. 1b (wzorzec)", "Druga linia", () -> solution1(3));
        Check.equal("ćw. 1c (wzorzec)", "nikt", () -> solution1(5));
        Check.equal("ćw. 2 (wzorzec)", List.of("e-mail bez znaku @", "ilość poza zakresem 1..100: 0"),
                () -> solution2(validationRules(), badRequest(), 2));
        Check.equal("ćw. 3 (wzorzec)", "automat", () -> solution3(approvers(), 50));
        Check.equal("ćw. 3b (wzorzec)", "kierownik", () -> solution3(approvers(), 5000));
        Check.equal("ćw. 3c (wzorzec)", "brak", () -> solution3(approvers(), 20000));
        Check.equal("ćw. 4 (wzorzec)", "A(B(core:x))", () -> solution4(wrappers(), core()).call("x"));
        Check.equal("ćw. 4b (wzorzec)", "STOP", () -> solution4(List.of(stopper(), wrappers().get(0)), core()).call("x"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 9 OK, ✘ 0 BŁĄD
    }

    /** Dane testowe do ćwiczeń. */
    private static OrderRequest badRequest() {
        return new OrderRequest("jan.example.com", 0, new BigDecimal("-5"), "PL");
    }

    /** Zatwierdzający z limitami kwot (rosnąco): kto zatwierdza zakup do podanej kwoty. */
    record Approver(int limit, String name) { }  // approver = zatwierdzający; limit = górna granica kwoty

    private static List<Approver> approvers() {
        return List.of(new Approver(100, "automat"), new Approver(1000, "pracownik"), new Approver(10_000, "kierownik"));
    }

    private static List<Middleware> wrappers() {
        return List.of(
                (request, next) -> "A(" + next.call(request) + ")",
                (request, next) -> "B(" + next.call(request) + ")");
    }

    private static Middleware stopper() {
        return (request, next) -> "STOP";
    }

    private static Next core() {
        return request -> "core:" + request;
    }

    /**
     * ĆWICZENIE 1 (łatwe): użyj gotowego łańcucha supportChain() i zwróć nazwę ogniwa, które obsłużyło zgłoszenie
     * o podanej wadze; gdy nikt — napis "nikt".
     * Podpowiedź: chain.handle(...) zwraca {@code Optional<Response>}; map(Response::handledBy) i orElse("nikt").
     */
    static String exercise1(int severity) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zbieraj błędy walidacji jak allErrors, ale PRZERWIJ po zebraniu {@code maxErrors} błędów.
     * Podpowiedź: pętla po regułach; po dodaniu błędu sprawdź rozmiar listy i zwróć wcześniej (return).
     */
    static List<String> exercise2(List<Rule> rules, OrderRequest request, int maxErrors) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ): stary kod z zagnieżdżonymi if-ami:
     * <pre>{@code
     * if (amount <= 100) { return "automat"; }
     * else if (amount <= 1000) { return "pracownik"; }
     * else if (amount <= 10000) { return "kierownik"; }
     * else { return "brak"; }
     * }</pre>
     * Przepisz na pętlę po liście zatwierdzających (limity rosnąco): pierwszy, którego limit >= kwota, wygrywa; gdy
     * nikt — "brak". Zatwierdzającego dodasz teraz jedną linią w danych, bez zmiany kodu.
     * Podpowiedź: for (Approver a : approvers) { if (amount <= a.limit()) return a.name(); }
     */
    static String exercise3(List<Approver> approvers, int amount) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): złóż listę Middleware i końcowe Next w jedno Next — ogniwo na pozycji 0 jest
     * zewnętrzne (wołane pierwsze). Dla [A, B] i core wynik dla "x" to "A(B(core:x))"; jeśli ogniwo nie woła
     * next, reszta się nie wykonuje.
     * Podpowiedź: zacznij od ostatniego ogniwa i idź w stronę początku (pętla od końca) albo użyj rekurencji.
     */
    static Next exercise4(List<Middleware> middlewares, Next last) {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(int severity) {
        return supportChain().handle(new Ticket("zgłoszenie", severity)).map(Response::handledBy).orElse("nikt");
    }

    static List<String> solution2(List<Rule> rules, OrderRequest request, int maxErrors) {
        List<String> errors = new ArrayList<>();
        for (Rule rule : rules) {
            rule.check(request).ifPresent(errors::add);  // ifPresent = jeśli jest wartość, wykonaj
            if (errors.size() >= maxErrors) {
                return errors;
            }
        }
        return errors;
    }

    static String solution3(List<Approver> approvers, int amount) {
        for (Approver approver : approvers) {
            if (amount <= approver.limit()) {
                return approver.name();
            }
        }
        return "brak";
    }

    static Next solution4(List<Middleware> middlewares, Next last) {
        return build(middlewares, 0, last);
    }

    /** Rekurencyjnie: ogniwo i-te owija złożone z reszty. */
    private static Next build(List<Middleware> middlewares, int index, Next last) {
        if (index == middlewares.size()) {
            return last;
        }
        Next rest = build(middlewares, index + 1, last);  // rest = reszta łańcucha
        Middleware current = middlewares.get(index);
        return request -> current.handle(request, rest);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. "Pierwszy wygrywa": pierwsze pasujące ogniwo obsługuje i łańcuch się kończy (eskalacja zgłoszeń, rabaty).
     *      "Wszyscy uczestniczą": każde ogniwo coś robi i przekazuje dalej, chyba że przerwie (walidacja, filtry serwletów).
     *   2. Wynik pozwala wywołującemu sprawdzić, czy ktoś obsłużył i jak; efekt uboczny bez wyniku jest trudny do
     *      przetestowania i łatwo go zgubić. Optional dodatkowo zmusza do decyzji "co, jeśli nikt".
     *   3. Druga linia — waga 2 jest za duża dla pierwszej linii (<= 1), a druga obsługuje do 3.
     *   4. false — waga 9 przekracza limit kierownika (<= 4), a w łańcuchu nie ma ogniwa domyślnego.
     *   5. Cykl: a → b → a: FirstLine (waga <= 1) nie bierze wagi 5, SecondLine (<= 3) też nie, więc przekazują
     *      sobie zgłoszenie bez końca i kończy się to StackOverflowError.
     *   6. Wywołanie next.call(request) PRZED odmową uruchamia resztę łańcucha (a więc i właściwą obsługę z jej efektami
     *      ubocznymi) mimo braku uprawnień; zastąpiona zostaje dopiero odpowiedź. Przerwać łańcuch = w ogóle nie wołać next.
     *   7. W "pierwszy wygrywa" wynik zależy od tego, które ogniwo zadziała jako pierwsze; w zbieraniu wszystkich błędów
     *      wszystkie reguły i tak są wywołane, więc zmienia się tylko kolejność komunikatów.
     *   8. Gdy z góry wiadomo, kto obsłuży dany rodzaj żądania (klucz → obsługa) — wtedy Map<Typ, Obsługa> jest prostsza.
     */
    // </editor-fold>
}
