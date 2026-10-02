package t22_design_patterns;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Order;
import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Dependency Injection (wstrzykiwanie zależności) — „nie twórz, tylko dostań”
 *        (dependency = zależność, to czego klasa potrzebuje do pracy; injection = wstrzyknięcie, podanie z zewnątrz;
 *         container = kontener; wiring = okablowanie, łączenie obiektów; scope = zasięg życia obiektu)
 *
 * W SKRÓCIE:
 *   Klasa nie tworzy swoich zależności przez {@code new} i nie szuka ich sama — dostaje je (zwykle w konstruktorze)
 *   jako INTERFEJSY. Dzięki temu można podmienić prawdziwą wysyłkę maili na atrapę w teście, a prawdziwy zegar
 *   na zegar „zatrzymany”. Składanie obiektów dzieje się w jednym miejscu: w korzeniu kompozycji.
 *
 * ANALOGIA: kucharz w restauracji. Zły kucharz sam jedzie na targ po warzywa, sam kupuje piec i sam zatrudnia
 *   kelnera — nie da się go sprawdzić bez prawdziwego targu. Dobry kucharz dostaje składniki i sprzęt od
 *   kierownika sali: na egzaminie kierownik może dać mu atrapy i sprawdzić, co ugotuje. Kierownik to korzeń
 *   kompozycji (composition root): wie, kto czego używa, i podaje to wszystkim.
 *
 * JAK TO DZIAŁA:
 *   PRZED:   class OrderService { MailSender mail = new SmtpMailSender(); }        // twarde powiązanie
 *   PO:      class OrderService { OrderService(MailSender mail) { this.mail = mail; } }   // dostaje z zewnątrz
 *
 *      main (korzeń kompozycji)                  OrderService zna tylko interfejsy
 *      +-----------------------+                 +-----------------+
 *      | new SmtpMailSender()  |-- wstrzykuje -->| MailSender      |   implementacje: SmtpMailSender (produkcja),
 *      | new OrderService(...) |                 | OrderRepository |   RecordingMailSender (test), InMemory... (test)
 *      +-----------------------+                 +-----------------+
 *
 *   Trzy sposoby wstrzykiwania: konstruktor (najlepszy), setter (opcjonalne zależności), pole (najgorsze).
 *   Trzy pojęcia, które się mylą:
 *     DIP (Dependency Inversion Principle) — ZASADA projektowa: moduły wysokiego poziomu zależą od abstrakcji;
 *     DI (Dependency Injection) — TECHNIKA: podawanie zależności z zewnątrz;
 *     IoC (Inversion of Control, odwrócenie sterowania) — OGÓLNA IDEA: to framework woła nasz kod, a nie odwrotnie.
 *
 * SŁÓWKA:
 *   dependency = zależność; inject = wstrzyknij; constructor = konstruktor; setter = metoda ustawiająca;
 *   fake = atrapa (prosta, działająca podróbka); stub = zaślepka ze stałą odpowiedzią; mock = obiekt
 *   weryfikujący wywołania; scope = zasięg; singleton = jedna instancja; prototype = nowa instancja za każdym razem;
 *   service locator = lokalizator usług; clock = zegar; wire = połącz (okablować).
 *
 * ZOBACZ TEŻ: t27_clean_code_pitfalls/CleanCode02Solid (D = Dependency Inversion),
 *   t22_design_patterns/Patterns01Strategy (strategia wstrzyknięta jako zależność),
 *   t22_design_patterns/Patterns04Singleton (singleton ręczny kontra singleton z kontenera),
 *   t19_annotations_reflection/Annotations05MiniFramework (kontener oparty na refleksji),
 *   t34_toward_spring/Spring01IocContainer (pełny kontener), t32_junit_mockito/JUnit04Mockito (atrapy)
 * </pre>
 */
public class Patterns08DependencyInjection {

    public static void main(String[] args) {
        title("Patterns08 — Dependency Injection (wstrzykiwanie zależności)");

        hardWired();             // hard-wired = przybite na sztywno (problem)
        constructorInjection();  // constructor injection = wstrzykiwanie przez konstruktor
        setterAndFieldInjection(); // setter and field injection = przez setter i przez pole
        compositionRoot();       // composition root = korzeń kompozycji
        testingWithFakes();      // testing with fakes = testowanie z atrapami
        serviceLocator();        // service locator = lokalizator usług (antywzorzec)
        miniContainer();         // mini container = malutki kontener DI
        springAndDip();          // spring and DIP = co robi Spring i zasada DIP
        pitfalls();              // pitfalls = pułapki
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // Wspólne typy lekcji (domena: potwierdzanie zamówień)
    // =================================================================================================

    /** Zależność 1: wysyłka maili. Interfejs należy do strony serwisu (to ona mówi, czego potrzebuje). */
    interface MailSender {
        void send(String to, String subject); // send = wyślij; to = do kogo; subject = temat
    }

    /** Zależność 2: zapis zamówień. */
    interface OrderRepository {
        void save(Order order); // save = zapisz

        List<Order> findAll(); // findAll = znajdź wszystkie
    }

    /** „Prawdziwy” nadawca: w tej lekcji nie ma serwera SMTP, więc zawsze zgłasza brak połączenia. */
    static final class SmtpMailSender implements MailSender {
        @Override
        public void send(String to, String subject) {
            throw new IllegalStateException("brak połączenia z serwerem SMTP");
        }
    }

    /** Atrapa nadawcy: zapamiętuje, co „wysłano” (recording = nagrywająca). */
    static final class RecordingMailSender implements MailSender {
        final List<String> sent = new ArrayList<>(); // sent = wysłane

        @Override
        public void send(String to, String subject) {
            sent.add(to + ": " + subject);
        }
    }

    /** Atrapa repozytorium: lista w pamięci zamiast bazy danych (in-memory = w pamięci). */
    static final class InMemoryOrderRepository implements OrderRepository {
        private final List<Order> orders = new ArrayList<>();

        @Override
        public void save(Order order) {
            orders.add(order);
        }

        @Override
        public List<Order> findAll() {
            return List.copyOf(orders);
        }
    }

    /** Serwis z wstrzykiwaniem przez konstruktor — zna tylko interfejsy i Clock (zegar z java.time). */
    static final class OrderService {
        static final LocalTime CUTOFF = LocalTime.of(14, 0); // cutoff = godzina graniczna wysyłki „dziś”

        private final OrderRepository repository; // final = pole przypisane raz, serwis jest gotowy po konstruktorze
        private final MailSender mail;
        private final Clock clock;

        OrderService(OrderRepository repository, MailSender mail, Clock clock) {
            this.repository = Objects.requireNonNull(repository, "repository"); // fail fast = zawiedź od razu
            this.mail = Objects.requireNonNull(mail, "mail");
            this.clock = Objects.requireNonNull(clock, "clock");
        }

        /** Zapisuje zamówienie, wysyła potwierdzenie (jeśli klient ma e-mail) i podaje termin wysyłki. */
        String confirm(Order order) { // confirm = potwierdź
            repository.save(order);
            order.customer().findEmail().ifPresent(email -> mail.send(email, "Potwierdzenie " + order.id()));
            boolean sameDay = LocalTime.now(clock).isBefore(CUTOFF); // sameDay = tego samego dnia
            return sameDay ? "WYSYŁKA DZIŚ" : "WYSYŁKA JUTRO";
        }
    }

    /** Zegar „zatrzymany” na wskazanej godzinie (UTC) — wynik w testach jest zawsze taki sam. */
    static Clock clockAt(String time) { // clockAt = zegar o godzinie
        return Clock.fixed(LocalDate.of(2026, 3, 10).atTime(LocalTime.parse(time)).toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC);
    }

    // =================================================================================================
    // 1. PROBLEM — `new` w środku serwisu
    // =================================================================================================

    /** Wersja BEZ wzorca: serwis sam tworzy nadawcę i sam pyta system o godzinę. */
    static final class HardWiredService {
        private final MailSender mail = new SmtpMailSender(); // twarda zależność od konkretnej klasy

        String confirm(Order order) {
            boolean sameDay = LocalTime.now().isBefore(OrderService.CUTOFF); // LocalTime.now() = zegar systemowy
            order.customer().findEmail().ifPresent(email -> mail.send(email, "Potwierdzenie " + order.id()));
            return sameDay ? "WYSYŁKA DZIŚ" : "WYSYŁKA JUTRO";
        }
    }

    /**
     * 1. Problem: żeby przetestować prosty serwis, potrzebujemy prawdziwego serwera SMTP i... odpowiedniej
     * godziny na zegarze komputera. Test, który zależy od świata zewnętrznego, jest wolny, kruchy i często pomijany.
     */
    static void hardWired() {
        section("1. Problem — `new` wewnątrz serwisu");

        Order order = SampleData.orders().get(0); // ZAM-001, klient z adresem e-mail
        HardWiredService service = new HardWiredService();
        expectThrows("test serwisu bez serwera SMTP", () -> service.confirm(order));
        // WYNIK: ✔ test serwisu bez serwera SMTP → rzucono IllegalStateException: brak połączenia z serwerem SMTP

        note("wynik 'dziś/jutro' zależy od godziny uruchomienia testu — nie da się go sprawdzić w stały sposób");
        // WYNIK: ℹ wynik 'dziś/jutro' zależy od godziny uruchomienia testu — nie da się go sprawdzić w stały sposób
        // PUŁAPKA: zależność ukryta w ciele klasy. Z zewnątrz `new HardWiredService()` wygląda niewinnie, a w środku
        //   łączy się z serwerem poczty i odpytuje zegar. Skutki: (1) testu nie da się uruchomić bez infrastruktury,
        //   (2) nie da się sprawdzić gałęzi „po 14:00” i „przed 14:00” w jednym przebiegu,
        //   (3) zmiana dostawcy poczty wymaga edycji serwisu (łamie zasadę otwarte–zamknięte, t27 CleanCode02Solid).
    }

    // =================================================================================================
    // 2. WSTRZYKIWANIE PRZEZ KONSTRUKTOR
    // =================================================================================================

    /**
     * 2. Konstruktor jako jedyna droga do obiektu: wszystkie zależności są wymagane, pola są {@code final},
     * a obiekt po utworzeniu jest od razu gotowy (nie ma stanu „pół-zbudowany”). Pusta zależność wychodzi od razu.
     */
    static void constructorInjection() {
        section("2. Wstrzykiwanie przez konstruktor");

        RecordingMailSender mail = new RecordingMailSender();
        OrderService service = new OrderService(new InMemoryOrderRepository(), mail, clockAt("10:00"));
        Order order = SampleData.orders().get(0);
        show("zamówienie o 10:00", service.confirm(order));
        // WYNIK: zamówienie o 10:00 → WYSYŁKA DZIŚ
        show("wysłane maile", mail.sent);
        // WYNIK: wysłane maile → [jan@example.com: Potwierdzenie ZAM-001]

        expectThrows("brak zależności (null)", () -> new OrderService(null, mail, clockAt("10:00")));
        // WYNIK: ✔ brak zależności (null) → rzucono NullPointerException: repository
        // Objects.requireNonNull(x, "nazwa") rzuca NullPointerException z naszym komunikatem już w konstruktorze —
        // błąd wychodzi w miejscu złożenia programu (fail fast), a nie za godzinę, w środku jakiejś metody.

        // DOBRA PRAKTYKA: konstruktor + pola final + zależności jako interfejsy. Dlaczego: (1) widać wszystkie
        //   zależności w jednym miejscu — długi konstruktor to sygnał, że klasa robi za dużo (zasada jednej
        //   odpowiedzialności), (2) obiekt jest niezmienny po złożeniu, więc bezpieczny dla wielu wątków,
        //   (3) nie da się go użyć „niekompletnego”, (4) test podaje atrapy bez żadnej magii.
    }

    // =================================================================================================
    // 3. SETTER I POLE
    // =================================================================================================

    /** Wstrzykiwanie przez setter: obiekt istnieje, zanim dostanie zależności. */
    static final class SetterInjectedService {
        private MailSender mail; // brak final: musi być zmienne, bo ustawiane później

        void setMail(MailSender mail) { // setMail = ustaw nadawcę
            this.mail = mail;
        }

        void confirm(Order order) {
            mail.send("klient@example.com", "Potwierdzenie " + order.id());
        }
    }

    /**
     * 3. Setter i pole: gorsze niż konstruktor. Setter ma sens tylko dla zależności NIEOBOWIĄZKOWYCH
     * (z rozsądną wartością domyślną). Wstrzykiwanie do pola prywatnego (jak {@code @Autowired} na polu) ukrywa
     * zależności i wymaga refleksji lub frameworka.
     */
    static void setterAndFieldInjection() {
        section("3. Setter i pole — dlaczego gorzej");

        SetterInjectedService service = new SetterInjectedService(); // obiekt „pół-zbudowany”
        try {
            service.confirm(SampleData.orders().get(0));
        } catch (NullPointerException ex) {
            show("zapomniany setter", ex.getClass().getSimpleName());
            // WYNIK: zapomniany setter → NullPointerException
        }
        // PUŁAPKA: kompilator nie pilnuje, czy zadzwoniliśmy setMail. Błąd (NullPointerException) wychodzi dopiero
        //   w czasie działania, w metodzie, która „tylko korzysta” — daleko od miejsca pomyłki. Od Javy 14 komunikat
        //   NPE wskazuje, co było null („because this.mail is null”), ale to tylko ratunek, nie rozwiązanie.
        //   Do tego pole nie może być final, więc obiekt jest zmienny i niebezpieczny w wielu wątkach.

        service.setMail(new RecordingMailSender());
        service.confirm(SampleData.orders().get(0));
        note("po ustawieniu nadawcy działa — ale ktoś musi o tym pamiętać");
        // WYNIK: ℹ po ustawieniu nadawcy działa — ale ktoś musi o tym pamiętać

        // Wstrzykiwanie do pola (field injection), jak w Springu:
        //     @Autowired private MailSender mail;      // brak konstruktora i settera
        //   • klasę można utworzyć tylko przez `new` z pustym polem — test bez frameworka musi sięgać refleksją
        //     do prywatnego pola (albo użyć Mockito @InjectMocks),
        //   • pole nie może być final,
        //   • zależności są niewidoczne w sygnaturze: łatwo urosnąć do 12 pól i nikt tego nie zauważy,
        //   • ukrywa cykle zależności, które konstruktor ujawniłby od razu.
        // DOBRA PRAKTYKA: konstruktor dla zależności obowiązkowych; setter tylko dla opcjonalnych; pole — prawie nigdy.
        //   Dlatego od Springa 4.3 klasa z jednym konstruktorem nie potrzebuje już @Autowired.
    }

    // =================================================================================================
    // 4. KORZEŃ KOMPOZYCJI
    // =================================================================================================

    /**
     * Korzeń kompozycji (composition root): JEDYNE miejsce, w którym tworzymy konkretne klasy i łączymy je
     * w graf obiektów. Reszta kodu widzi tylko interfejsy. Tu wybieramy profil: „prod” lub „test”.
     */
    static OrderService wire(String profile, RecordingMailSender testMail) { // wire = połącz (okablować)
        return switch (profile) { // switch jako wyrażenie (Java 14+)
            case "prod" -> new OrderService(new InMemoryOrderRepository(), new SmtpMailSender(), Clock.systemUTC());
            case "test" -> new OrderService(new InMemoryOrderRepository(), testMail, clockAt("15:30"));
            default -> throw new IllegalArgumentException("nieznany profil: " + profile);
        };
    }

    /**
     * 4. Składanie programu w jednym miejscu: zmiana implementacji (SMTP → atrapa) to zmiana jednej linijki
     * w korzeniu kompozycji, bez dotykania serwisu.
     */
    static void compositionRoot() {
        section("4. Korzeń kompozycji — jedno miejsce na `new`");

        RecordingMailSender testMail = new RecordingMailSender();
        OrderService testService = wire("test", testMail);
        show("profil test, o 15:30", testService.confirm(SampleData.orders().get(0)));
        // WYNIK: profil test, o 15:30 → WYSYŁKA JUTRO
        show("maile w profilu test", testMail.sent);
        // WYNIK: maile w profilu test → [jan@example.com: Potwierdzenie ZAM-001]

        OrderService prodService = wire("prod", testMail);
        expectThrows("profil prod bez serwera SMTP", () -> prodService.confirm(SampleData.orders().get(0)));
        // WYNIK: ✔ profil prod bez serwera SMTP → rzucono IllegalStateException: brak połączenia z serwerem SMTP
        expectThrows("nieznany profil", () -> wire("qa", testMail));
        // WYNIK: ✔ nieznany profil → rzucono IllegalArgumentException: nieznany profil: qa

        // W aplikacji konsolowej korzeniem jest main; w Springu — klasa konfiguracji z metodami @Bean lub skanowanie
        // klas oznaczonych @Component. W aplikacji webowej — start serwera. Zasada ta sama: konkretne klasy
        // sklejamy na brzegu programu, a środek zna tylko interfejsy.
        // Zwróć uwagę, że `new` NIE jest zakazany: value objecty (record, String, List) tworzysz normalnie w kodzie.
        //   Wstrzykujesz to, co ma zachowanie, efekty uboczne albo można je chcieć podmienić (poczta, baza, zegar,
        //   losowanie, system plików, sieć).
        // DOBRA PRAKTYKA: wstrzykuj Clock zamiast wołać LocalDateTime.now() w logice biznesowej — wtedy czas
        //   w testach stoi w miejscu i wynik jest powtarzalny (t17 DateTime).
    }

    // =================================================================================================
    // 5. TESTY Z ATRAPAMI
    // =================================================================================================

    /**
     * 5. Dzięki interfejsom test składa serwis z atrap: wolny serwer poczty to lista w pamięci, a zegar stoi.
     * Atrapy (fake) mają prawdziwą, uproszczoną logikę; stub zwraca stałą odpowiedź; mock sprawdza wywołania.
     */
    static void testingWithFakes() {
        section("5. Testowanie z atrapami — zegar, repozytorium, nagrywający nadawca");

        List<Order> orders = SampleData.orders();
        Order withEmail = orders.get(0);    // ZAM-001, klient Jan Kowalski ma e-mail
        Order withoutEmail = orders.get(1); // ZAM-002, klientka Maria Nowak nie ma e-maila

        RecordingMailSender mail = new RecordingMailSender();
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        OrderService before = new OrderService(repository, mail, clockAt("13:59"));
        OrderService after = new OrderService(repository, mail, clockAt("14:00"));

        Check.equal("13:59 → dziś", "WYSYŁKA DZIŚ", () -> before.confirm(withEmail));
        Check.equal("14:00 → jutro", "WYSYŁKA JUTRO", () -> after.confirm(withEmail));
        Check.equal("maile dla klienta z adresem", 2, () -> mail.sent.size());
        Check.equal("klient bez e-maila — brak maila", 2, () -> {
            before.confirm(withoutEmail);
            return mail.sent.size();
        });
        Check.equal("zapisano 3 zamówienia", 3, () -> repository.findAll().size());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD

        // Ten sam serwis testujemy o 13:59 i 14:00 w jednym przebiegu — przy `LocalTime.now()` w środku byłoby to
        // niemożliwe. To sedno DI: ZMIENNOŚĆ świata (czas, sieć, baza) wchodzi do klasy przez interfejs, więc w teście
        // zastępujemy ją przewidywalną podróbką.
        // Rodzaje „podróbek” (test doubles): fake (działająca uproszczona wersja, np. repozytorium w pamięci),
        //   stub (stała odpowiedź), mock (sprawdza, czy wywołano metodę — t32_junit_mockito/JUnit04Mockito),
        //   spy (prawdziwy obiekt z podglądem wywołań). W tej lekcji RecordingMailSender to ręczny „spy/fake”.
        // DOBRA PRAKTYKA: wolisz fake niż mock tam, gdzie się da — testy sprawdzają wynik, a nie sposób, w jaki
        //   klasa rozmawia ze współpracownikami, więc nie psują się przy refaktoryzacji.
    }

    // =================================================================================================
    // 6. SERVICE LOCATOR
    // =================================================================================================

    /** Globalny rejestr usług (service locator): klasa sama „prosi” o zależność ze statycznej mapy. */
    static final class ServiceLocator {
        private static final Map<Class<?>, Object> SERVICES = new HashMap<>(); // nigdy go nie wypisujemy

        private ServiceLocator() { }

        static <T> void register(Class<T> type, T service) { // register = zarejestruj
            SERVICES.put(type, service);
        }

        static <T> T get(Class<T> type) { // get = pobierz
            Object service = SERVICES.get(type);
            if (service == null) {
                throw new IllegalStateException("brak usługi " + type.getSimpleName());
            }
            return type.cast(service);
        }

        static void clear() { // clear = wyczyść
            SERVICES.clear();
        }
    }

    /** Serwis, który nie ma żadnych zależności w konstruktorze, a używa ich „z powietrza”. */
    static final class LocatorOrderService {
        void confirm(Order order) {
            ServiceLocator.get(MailSender.class).send("jan@example.com", "Potwierdzenie " + order.id());
        }
    }

    /**
     * 6. Service Locator kontra DI. Locator wygląda wygodnie, ale zależności są ukryte, a rejestr jest
     * globalny. Wniosek: w kodzie aplikacji używaj DI; locator zostaw frameworkom i kodowi przejściowemu.
     */
    static void serviceLocator() {
        section("6. Service Locator — antywzorzec w kodzie aplikacji");

        LocatorOrderService service = new LocatorOrderService(); // kompiluje się, wygląda na bezzależnościowy
        Order order = SampleData.orders().get(0);
        expectThrows("locator bez rejestracji", () -> service.confirm(order));
        // WYNIK: ✔ locator bez rejestracji → rzucono IllegalStateException: brak usługi MailSender

        RecordingMailSender mail = new RecordingMailSender();
        try {
            ServiceLocator.register(MailSender.class, mail);
            service.confirm(order);
            show("po rejestracji", mail.sent);
            // WYNIK: po rejestracji → [jan@example.com: Potwierdzenie ZAM-001]
        } finally {
            ServiceLocator.clear(); // sprzątamy globalny stan — inaczej „przecieknie” do następnych testów
        }

        // PUŁAPKA: (1) sygnatura kłamie — konstruktor nic nie mówi o potrzebie MailSender, więc dowiadujesz się
        //   o niej dopiero z wyjątku w czasie działania; (2) rejestr jest globalny i statyczny: dwa testy
        //   wzajemnie nadpisują sobie usługi, a kolejność testów zaczyna mieć znaczenie (jak w singletonie,
        //   t22 Patterns04Singleton); (3) każdą klasę trzeba uzależnić od lokalizatora, więc nie da się jej
        //   użyć bez niego.
        // DI odwraca to: klasa ZADEKLAROWAŁA w konstruktorze, czego potrzebuje, i nie wie, skąd to dostanie.
        // Spring ma ApplicationContext.getBean(...) — to właśnie lokalizator; używany w kodzie aplikacji
        //   (zamiast wstrzykiwania) uznajemy za zapach. Uczciwie: bywa sensowny w kodzie infrastruktury
        //   (np. wtyczki ładowane przez ServiceLoader z JDK) i przy stopniowym porządkowaniu starego kodu.
    }

    // =================================================================================================
    // 7. MINI-KONTENER
    // =================================================================================================

    /** Zasięg obiektu w kontenerze (scope): jedna instancja na kontener albo nowa przy każdym pobraniu. */
    enum Scope { SINGLETON, PROTOTYPE }

    /** Jajko i kura: wzajemna zależność przez konstruktory (nie da się ich utworzyć) — do demonstracji cyklu. */
    record Chicken(Egg egg) { } // Chicken = kura

    record Egg(Chicken chicken) { } // Egg = jajko

    /** Pusty obiekt do pokazania różnicy singleton/prototype. */
    static final class Report { // Report = raport
    }

    /**
     * Najmniejszy możliwy kontener DI: mapa „typ → przepis na utworzenie”, zasięgi i automatyczne łączenie
     * przez konstruktor (autowiring). Refleksji tylko tyle, ile trzeba (t19). Pełny kontener zbudujemy w t34.
     */
    static final class MiniContainer {
        private record Definition(Function<MiniContainer, Object> factory, Scope scope) { } // Definition = definicja

        private final Map<Class<?>, Definition> definitions = new HashMap<>();
        private final Map<Class<?>, Object> singletons = new HashMap<>();
        private final List<Class<?>> creating = new ArrayList<>(); // creating = właśnie tworzone (do wykrywania cykli)

        <T> void register(Class<T> type, Scope scope, Function<MiniContainer, ? extends T> factory) {
            definitions.put(type, new Definition(factory::apply, scope));
        }

        /** Rejestruje klasę, której zależności kontener sam znajdzie po typach parametrów konstruktora. */
        <T> void registerAuto(Class<T> type, Class<? extends T> implementation, Scope scope) {
            register(type, scope, container -> container.construct(implementation));
        }

        <T> T get(Class<T> type) { // get = pobierz instancję
            Definition definition = definitions.get(type);
            if (definition == null) {
                throw new IllegalStateException("brak definicji: " + type.getSimpleName());
            }
            if (definition.scope() == Scope.SINGLETON && singletons.containsKey(type)) {
                return type.cast(singletons.get(type));
            }
            if (creating.contains(type)) {
                List<String> names = new ArrayList<>();
                for (Class<?> c : creating) {
                    names.add(c.getSimpleName());
                }
                names.add(type.getSimpleName());
                throw new IllegalStateException("cykl zależności: " + String.join(" → ", names));
            }
            creating.add(type);
            try {
                Object instance = definition.factory().apply(this);
                if (definition.scope() == Scope.SINGLETON) {
                    singletons.put(type, instance);
                }
                return type.cast(instance);
            } finally {
                creating.remove(creating.size() - 1);
            }
        }

        private <T> T construct(Class<T> implementation) { // construct = skonstruuj
            Constructor<?>[] constructors = implementation.getDeclaredConstructors();
            if (constructors.length != 1) {
                throw new IllegalStateException(implementation.getSimpleName() + ": oczekiwano jednego konstruktora");
            }
            Constructor<?> constructor = constructors[0];
            Object[] arguments = Arrays.stream(constructor.getParameterTypes())
                    .map(parameterType -> get(parameterType)) // rekurencja: zależności zależności
                    .toArray();
            try {
                return implementation.cast(constructor.newInstance(arguments));
            } catch (ReflectiveOperationException ex) { // ReflectiveOperationException = błąd refleksji
                throw new IllegalStateException("nie udało się utworzyć " + implementation.getSimpleName(), ex);
            }
        }
    }

    /**
     * 7. Własny, malutki kontener DI (~60 linii): rejestrujemy przepisy, a kontener sam łączy obiekty.
     * Pokazuje, że „magia” Springa to w gruncie rzeczy mapa przepisów, zasięgi i automatyczne wołanie konstruktorów.
     */
    static void miniContainer() {
        section("7. Mini-kontener DI — przepisy, zasięgi, autowiring");

        MiniContainer container = new MiniContainer();
        container.register(Clock.class, Scope.SINGLETON, c -> clockAt("09:00"));
        container.registerAuto(OrderRepository.class, InMemoryOrderRepository.class, Scope.SINGLETON);
        container.registerAuto(MailSender.class, RecordingMailSender.class, Scope.SINGLETON);
        container.registerAuto(OrderService.class, OrderService.class, Scope.SINGLETON); // konstruktor ma 3 parametry
        container.registerAuto(Report.class, Report.class, Scope.PROTOTYPE);

        OrderService service = container.get(OrderService.class);
        show("zamówienie o 09:00", service.confirm(SampleData.orders().get(0)));
        // WYNIK: zamówienie o 09:00 → WYSYŁKA DZIŚ
        show("singleton: ta sama instancja", container.get(OrderService.class) == service);
        // WYNIK: singleton: ta sama instancja → true
        show("singleton nadawcy współdzielony", container.get(MailSender.class) == container.get(MailSender.class));
        // WYNIK: singleton nadawcy współdzielony → true
        show("prototype: za każdym razem nowa", container.get(Report.class) == container.get(Report.class));
        // WYNIK: prototype: za każdym razem nowa → false

        container.registerAuto(Chicken.class, Chicken.class, Scope.SINGLETON);
        container.registerAuto(Egg.class, Egg.class, Scope.SINGLETON);
        expectThrows("cykl zależności", () -> container.get(Chicken.class));
        // WYNIK: ✔ cykl zależności → rzucono IllegalStateException: cykl zależności: Chicken → Egg → Chicken
        expectThrows("brak definicji", () -> container.get(String.class));
        // WYNIK: ✔ brak definicji → rzucono IllegalStateException: brak definicji: String

        // Co tu zrobiliśmy: (1) register — przepis „jak zrobić T” (lambda), (2) scope — czy obiekt jest jeden
        // (singleton), czy za każdym razem nowy (prototype), (3) registerAuto — kontener czyta parametry
        // konstruktora i rekurencyjnie pobiera je z siebie, (4) wykrywanie cykli (kura i jajko).
        // PUŁAPKA: „singleton w kontenerze” to NIE singleton z GoF (t22 Patterns04Singleton). Tu jedna instancja
        //   na KONTENER: klasa ma zwykły publiczny konstruktor i w innym kontenerze (albo w teście) powstanie
        //   druga. To zaleta — bo nie ma globalnego stanu i testy są niezależne.
        // Czego tu NIE ma, a ma Spring: skanowanie klas (@Component), konfiguracja z adnotacji, cykl życia
        //   (@PostConstruct), proxy (@Transactional), zasięgi webowe (request, session), wybór po nazwie lub
        //   kwalifikatorze, gdy jest kilka implementacji. Zrobimy to w t34_toward_spring/Spring01IocContainer.
    }

    // =================================================================================================
    // 8. SPRING I DIP
    // =================================================================================================

    /**
     * 8. Spring i zasada DIP. Spring to dojrzały kontener DI: w korzeniu kompozycji (konfiguracji) opisujesz
     * komponenty, a wstrzykiwanie robi sam. DIP mówi, GDZIE postawić interfejs.
     */
    static void springAndDip() {
        section("8. Spring i zasada DIP");

        // Spring (SpringLearning) — to samo, co zrobił nasz MiniContainer, tylko pełniej:
        //     @Service                                  // Spring znajdzie klasę i utworzy ją jako singleton
        //     class OrderService {
        //         OrderService(OrderRepository r, MailSender m, Clock c) { ... }   // jeden konstruktor → bez @Autowired
        //     }
        //     @Bean Clock clock() { return Clock.systemUTC(); }     // przepis w klasie konfiguracji
        //   • @Component / @Service / @Repository — „zarejestruj tę klasę w kontenerze”,
        //   • domyślny zasięg beana to singleton (jedna instancja na kontener — nie GoF singleton),
        //   • @Autowired — „wstrzyknij tu”; w nowym kodzie zbędne na jedynym konstruktorze,
        //   • @Primary / @Qualifier — wybór, gdy jest kilka implementacji tego samego interfejsu,
        //   • w testach: @MockBean/@SpringBootTest albo (lepiej) zwykły `new` z atrapami, bez Springa.
        note("Spring = kontener DI + dużo dodatków; wzorzec ten sam");
        // WYNIK: ℹ Spring = kontener DI + dużo dodatków; wzorzec ten sam

        // DIP (zasada „D” z SOLID, t27 CleanCode02Solid): moduły wysokiego poziomu (logika zamówień) nie powinny
        // zależeć od niskiego poziomu (SMTP, SQL); oba zależą od ABSTRAKCJI. Interfejs trzymamy po stronie serwisu:
        //
        //   PRZED (zależność idzie w dół):          PO (strzałka odwrócona):
        //     OrderService --> SmtpMailSender          OrderService --> MailSender <-- SmtpMailSender
        //
        // Dzięki temu logika biznesowa nie importuje klas poczty, a wymiana SMTP na inny kanał nie dotyka serwisu.
        // Uwaga na różnicę: DIP bez DI (interfejs jest, ale serwis nadal robi `new Smtp...`) nic nie daje —
        // potrzebny jest ktoś, kto zależność poda. I odwrotnie: DI bez DIP (wstrzykujesz konkretną klasę)
        // działa, ale nie pozwala na podmianę.
    }

    // =================================================================================================
    // 9. PUŁAPKI
    // =================================================================================================

    /**
     * 9. Pułapki DI: interfejs „na wszelki wypadek”, ogromny konstruktor, przekazywanie kontenera,
     * cykle zależności.
     */
    static void pitfalls() {
        section("9. Pułapki — nadmiar abstrakcji, wielki konstruktor, kontener w serwisie");

        // PUŁAPKA: interfejs dla każdej klasy „bo tak trzeba” (UserService + UserServiceImpl, jedna implementacja
        //   na zawsze). To szum bez korzyści — YAGNI. Interfejs dodaj, gdy są DWIE implementacje (prawdziwa i
        //   testowa) albo granica warstw (np. domena ↔ poczta). Dla zwykłej logiki wystarczy klasa.
        // PUŁAPKA: konstruktor z 8 parametrami. To nie argument za wstrzykiwaniem do pól! To sygnał, że klasa
        //   łamie zasadę jednej odpowiedzialności — podziel ją (np. OrderService na OrderConfirmation i OrderPricing).
        // PUŁAPKA: przekazywanie CAŁEGO kontenera do serwisu (`new OrderService(container)`). To service locator
        //   tylnymi drzwiami: znowu zależności są ukryte. Podawaj konkretne zależności.
        // PUŁAPKA: cykl A → B → A. Konstruktor go ujawnia od razu (nasz kontener rzuca wyjątek, sekcja 7) —
        //   rozwiązanie: wyciągnij wspólną część do trzeciej klasy albo odwróć jedną zależność przez zdarzenie
        //   (t22 Patterns06Observer). Wstrzykiwanie do pól potrafi taki cykl ukryć (Spring Boot od wersji 2.6 domyślnie go zabrania), a to błąd projektowy.
        // PUŁAPKA: wstrzykiwanie rzeczy, które nie powinny być zależnościami: `String`, `int`, `List` (dane).
        //   Dane przekazuj jako parametry metod; wstrzykuj usługi i konfigurację (np. rekord Settings).
        // DOBRA PRAKTYKA: korzeń kompozycji jest jeden, brzydki i nudny — i to jest dobrze: cała „brudna robota
        //   łączenia” jest w jednym miejscu, a reszta kodu jest czysta i testowalna.

        // KIEDY UŻYWAĆ / KIEDY NIE:
        //   | używaj, gdy                                       | nie używaj, gdy                                 |
        //   | klasa dotyka świata zewnętrznego (poczta, baza)   | tworzysz zwykłe dane (record, String, List)     |
        //   | chcesz podmienić implementację (test, profil)     | zależność jest stała i nigdy nie zmieni się     |
        //   | potrzebujesz kontrolować czas / losowość          | to prywatny szczegół, np. StringBuilder w metodzie |
        //   | chcesz widzieć zależności klasy w konstruktorze   | dodajesz kontener do 50-liniowego programu       |
        note("DI = prosty pomysł; kontener to tylko wygoda, nie warunek");
        // WYNIK: ℹ DI = prosty pomysł; kontener to tylko wygoda, nie warunek
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • DI: klasa dostaje zależności z zewnątrz (jako interfejsy), zamiast tworzyć je przez new lub szukać.
     *   • Konstruktor + pola final = najlepszy sposób; setter tylko dla opcjonalnych; pole (@Autowired na polu) — unikaj.
     *   • Korzeń kompozycji: jedno miejsce (main / konfiguracja), w którym składasz graf obiektów.
     *   • Wstrzykuj to, co zmienne lub „brudne”: zegar (Clock), losowość, poczta, baza, sieć, system plików.
     *   • Atrapy w testach: fake (działająca uproszczona), stub (stała odpowiedź), mock (sprawdza wywołania).
     *   • Service Locator = ukryte zależności i globalny stan; DI = zależności widoczne w konstruktorze.
     *   • Kontener: mapa typ → przepis, zasięgi (singleton / prototype), autowiring po typach konstruktora.
     *   • Singleton w Springu to nie GoF singleton: jedna instancja na kontener, zwykły konstruktor.
     *   • DIP (zasada) ≠ DI (technika) ≠ IoC (idea): interfejs po stronie serwisu + ktoś, kto poda implementację.
     *   • Nie rób interfejsu z jedną wieczną implementacją; długi konstruktor = klasa robi za dużo.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego klasa z polem {@code new SmtpMailSender()} jest trudna do testowania? Wymień dwa powody.
     *   2. Czym różnią się DI, DIP i IoC? Jedno zdanie na każde.
     *   3. Dlaczego konstruktor jest lepszy od settera dla zależności obowiązkowych?
     *   4. Co wypisze:
     *        OrderService s = new OrderService(new InMemoryOrderRepository(), new RecordingMailSender(), clockAt("14:00"));
     *        System.out.println(s.confirm(SampleData.orders().get(1)));
     *   5. ZNAJDŹ BŁĄD:  class ReportService { String today() { return LocalDate.now().toString(); } }
     *        — test porównuje wynik ze stałą "2026-03-10" i „czasem” się wywala. Co zmienić?
     *   6. Co wypisze:  MiniContainer c = new MiniContainer();
     *        c.registerAuto(Report.class, Report.class, Scope.PROTOTYPE);
     *        System.out.println(c.get(Report.class) == c.get(Report.class));
     *   7. ZNAJDŹ BŁĄD:  new OrderService(container)  oraz w środku  container.get(MailSender.class).
     *        — jaki antywzorzec to jest i co zrobić?
     *   8. Czym różni się fake od mocka? Który częściej wybierasz i dlaczego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: po 14:00", "WYSYŁKA JUTRO", () -> exercise1());
        Check.equal("ćw. 2: maile bez adresu pomijane", List.of("jan@example.com: Potwierdzenie ZAM-001"),
                () -> exercise2());
        Check.equal("ćw. 3: rano", "Dzień dobry", () -> exercise3(clockAt("10:00")));
        Check.equal("ćw. 3: wieczorem", "Dobry wieczór", () -> exercise3(clockAt("20:00")));
        Check.equal("ćw. 4: kontener", "WYSYŁKA DZIŚ|true", () -> exercise4());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "WYSYŁKA JUTRO", () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", List.of("jan@example.com: Potwierdzenie ZAM-001"), () -> solution2());
        Check.equal("ćw. 3 (wzorzec, rano)", "Dzień dobry", () -> solution3(clockAt("10:00")));
        Check.equal("ćw. 3 (wzorzec, wieczorem)", "Dobry wieczór", () -> solution3(clockAt("20:00")));
        Check.equal("ćw. 4 (wzorzec)", "WYSYŁKA DZIŚ|true", () -> solution4());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): Złóż {@code OrderService} z atrap ({@code InMemoryOrderRepository},
     * {@code RecordingMailSender}) i zegara {@code clockAt("16:00")}, potwierdź pierwsze zamówienie z SampleData
     * i zwróć wynik potwierdzenia.
     * Podpowiedź: {@code SampleData.orders().get(0)}; konstruktor przyjmuje (repozytorium, nadawca, zegar).
     */
    static String exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): Potwierdź pierwsze i drugie zamówienie z SampleData (zegar dowolny) i zwróć listę
     * maili zapamiętanych przez {@code RecordingMailSender}. Klient drugiego zamówienia nie ma e-maila.
     * Podpowiedź: pole {@code sent} atrapy nadawcy zawiera wpisy w postaci {@code "adres: temat"}.
     */
    static List<String> exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, PRZEPISZ): Zamień zależność od zegara systemowego na wstrzyknięty {@code Clock}.
     * Zwróć "Dzień dobry", gdy godzina (wg podanego zegara) jest przed 18:00, w przeciwnym razie "Dobry wieczór".
     * <pre>{@code
     * // PRZED (nietestowalne — zależy od godziny uruchomienia):
     * static String greeting() {
     *     return LocalTime.now().isBefore(LocalTime.of(18, 0)) ? "Dzień dobry" : "Dobry wieczór";
     * }
     * }</pre>
     * Podpowiedź: {@code LocalTime.now(clock)}.
     */
    static String exercise3(Clock clock) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): Użyj {@code MiniContainer}: zarejestruj zegar {@code clockAt("09:00")} (singleton),
     * {@code OrderRepository} i {@code MailSender} (przez registerAuto, singletony) oraz {@code OrderService}
     * (registerAuto, singleton). Pobierz serwis, potwierdź pierwsze zamówienie i zwróć tekst
     * {@code wynik + "|" + (czy dwa pobrania serwisu to ta sama instancja)}.
     * Podpowiedź: wynik dla 09:00 to "WYSYŁKA DZIŚ"; porównanie instancji przez {@code ==}.
     */
    static String exercise4() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1() {
        OrderService service = new OrderService(new InMemoryOrderRepository(), new RecordingMailSender(), clockAt("16:00"));
        return service.confirm(SampleData.orders().get(0));
    }

    static List<String> solution2() {
        RecordingMailSender mail = new RecordingMailSender();
        OrderService service = new OrderService(new InMemoryOrderRepository(), mail, clockAt("10:00"));
        service.confirm(SampleData.orders().get(0));
        service.confirm(SampleData.orders().get(1));
        return mail.sent;
    }

    static String solution3(Clock clock) {
        return LocalTime.now(clock).isBefore(LocalTime.of(18, 0)) ? "Dzień dobry" : "Dobry wieczór";
    }

    static String solution4() {
        MiniContainer container = new MiniContainer();
        container.register(Clock.class, Scope.SINGLETON, c -> clockAt("09:00"));
        container.registerAuto(OrderRepository.class, InMemoryOrderRepository.class, Scope.SINGLETON);
        container.registerAuto(MailSender.class, RecordingMailSender.class, Scope.SINGLETON);
        container.registerAuto(OrderService.class, OrderService.class, Scope.SINGLETON);
        OrderService service = container.get(OrderService.class);
        String result = service.confirm(SampleData.orders().get(0));
        return result + "|" + (container.get(OrderService.class) == service);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Test wymagałby prawdziwego serwera SMTP (wolno, kruche, skutki uboczne), a podmiana zależności
     *      jest niemożliwa bez edycji klasy. Dodatkowo zależność jest ukryta — z zewnątrz nie widać, czego klasa
     *      potrzebuje.
     *   2. DI — technika: zależności podaje się z zewnątrz. DIP — zasada: moduły wysokiego poziomu zależą od
     *      abstrakcji, nie od szczegółów. IoC — idea: to framework/korzeń woła nasz kod i składa obiekty.
     *   3. Konstruktor wymusza zależności (kompilator pilnuje), pola mogą być final, obiekt jest od razu
     *      gotowy i niezmienny; przy setterze można zapomnieć o wywołaniu i dostać NullPointerException później.
     *   4. WYSYŁKA JUTRO — 14:00 nie jest „przed” 14:00 (isBefore jest ostre); klientka bez e-maila: brak maila.
     *   5. Wstrzyknąć Clock (np. Clock.fixed w teście) i użyć LocalDate.now(clock) zamiast LocalDate.now().
     *   6. false — dla zasięgu PROTOTYPE kontener tworzy nową instancję przy każdym pobraniu.
     *   7. Service Locator (kontener przekazany „tylnymi drzwiami”): zależności są ukryte. Wstrzyknąć konkretne
     *      MailSender w konstruktorze.
     *   8. Fake ma prawdziwą, uproszczoną logikę (np. repozytorium w pamięci) i test sprawdza WYNIK; mock
     *      weryfikuje, jakie wywołania nastąpiły. Częściej fake — testy nie psują się przy refaktoryzacji.
     */
    // </editor-fold>
}
