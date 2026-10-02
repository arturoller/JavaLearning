package t34_toward_spring;

import helpers.Check;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Kontener IoC od środka — budujemy własny „mini-Spring” w czystej Javie
 *        (IoC = Inversion of Control = odwrócenie sterowania; container = kontener; bean = ziarno, obiekt zarządzany)
 *
 * W SKRÓCIE:
 *   Zamiast samemu pisać {@code new} dla każdej zależności, oddajemy tworzenie i łączenie obiektów kontenerowi.
 *   Kontener czyta adnotacje, wybiera konstruktor przez refleksję, podaje zależności, pilnuje zakresu
 *   (singleton/prototyp), cyklu życia i potrafi owinąć obiekt w proxy (np. transakcja). Tak działa serce Springa.
 *
 * ANALOGIA:
 *   Restauracja. Kucharz (Twoja klasa) nie jeździ rano na targ po warzywa — zamawia je w karcie zapotrzebowania
 *   (parametry konstruktora). Kierownik zaopatrzenia (kontener) sprawdza listę dostawców, kupuje, dowozi
 *   i pilnuje, żeby jeden mikser służył całej kuchni (singleton), a fartuch był nowy dla każdego (prototyp).
 *
 * JAK TO DZIAŁA:
 *   1. Rejestracja: kontener dostaje listę klas z adnotacją @Component (Spring znajduje je skanowaniem).
 *   2. getBean(Typ): szuka klas pasujących do typu (także interfejsu) → 0 = błąd, 1 = OK, 2+ = @Primary/@Qualifier.
 *   3. Tworzenie: wybiera konstruktor (jedyny albo z @Inject), dla każdego parametru rekurencyjnie robi getBean
 *      (albo wstawia wartość z konfiguracji @Value). Stos „w trakcie tworzenia” wykrywa cykle A → B → A.
 *   4. Po konstruktorze: metody @PostConstruct; potem ewentualnie proxy (np. @Transactional).
 *   5. Singleton trafia do pamięci podręcznej (cache); przy close() kontener woła @PreDestroy w odwrotnej kolejności.
 *
 *     rejestracja ─► wybór klasy ─► konstruktor + zależności ─► @PostConstruct ─► proxy ─► cache ─► ... ─► @PreDestroy
 *
 * SŁÓWKA:
 *   component = komponent; inject = wstrzyknij; value = wartość; scope = zakres; prototype = prototyp;
 *   primary = główny; qualifier = kwalifikator (dookreślenie); post construct = po skonstruowaniu;
 *   pre destroy = przed zniszczeniem; proxy = pośrednik; transactional = transakcyjny; wiring = łączenie (okablowanie)
 *
 * ZOBACZ TEŻ: t22_design_patterns/Patterns08DependencyInjection (DI ręcznie), t19_annotations_reflection/Annotations05MiniFramework
 *   (refleksja + adnotacje), t19_annotations_reflection/Annotations06DynamicProxy (Proxy), t34_toward_spring/Spring02Layers (warstwy)
 * </pre>
 */
public class Spring01IocContainer {

    public static void main(String[] args) {
        title("Spring01 — kontener IoC: własny mini-Spring");

        withoutContainer();          // without container = bez kontenera
        ownAnnotations();            // own annotations = własne adnotacje
        miniContainer();             // mini container = mini-kontener
        configValues();              // config values = wartości z konfiguracji
        scopes();                    // scopes = zakresy
        lifecycle();                 // lifecycle = cykl życia
        interfacesAndAmbiguity();    // interfaces and ambiguity = interfejsy i niejednoznaczność
        cycles();                    // cycles = cykle zależności
        miniAop();                   // mini AOP = mini programowanie aspektowe (proxy)
        compareWithSpring();         // compare with Spring = porównanie ze Springiem
        exercises();                 // exercises = ćwiczenia
    }

    // =================================================================================================
    // ADNOTACJE NASZEGO KONTENERA (odpowiedniki adnotacji Springa)
    // =================================================================================================
    // @Retention(RUNTIME) = przechowuj w czasie działania — bez tego refleksja nie zobaczy adnotacji (t19).

    /** Klasa zarządzana przez kontener; {@code value} = jawna nazwa beana. */
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    @interface Component { String value() default ""; }

    /** Wskazuje konstruktor, którego ma użyć kontener (gdy jest ich kilka). */
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.CONSTRUCTOR)
    @interface Inject { }

    /** Wartość z konfiguracji: {@code "${klucz}"} albo {@code "${klucz:domyślna}"}. */
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER)
    @interface Value { String value(); }

    /** Zakres: "singleton" (domyślny) albo "prototype". */
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    @interface Scope { String value(); }

    /** Wybierz tę implementację, gdy pasuje kilka. */
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    @interface Primary { }

    /** Wybierz bean o tej nazwie (na parametrze konstruktora). */
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER)
    @interface Qualifier { String value(); }

    /** Metoda wołana zaraz po utworzeniu i wstrzyknięciu zależności. */
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    @interface PostConstruct { }

    /** Metoda wołana przy zamykaniu kontenera. */
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    @interface PreDestroy { }

    /** Metoda wykonywana „w transakcji” (nasze proxy wypisze BEGIN/COMMIT/ROLLBACK). */
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    @interface Transactional { }

    // =================================================================================================
    // 1. BEZ KONTENERA: PRZED → PO
    // =================================================================================================

    interface Notifier { String send(String to, String text); }   // Notifier = powiadamiacz; send = wyślij

    @Component
    static class EmailNotifier implements Notifier {
        @Override public String send(String to, String text) { return "e-mail do " + to + ": " + text; }
    }

    @Component
    static class SmsNotifier implements Notifier {
        @Override public String send(String to, String text) { return "SMS do " + to + ": " + text; }
    }

    @Component @Primary
    static class PushNotifier implements Notifier {
        @Override public String send(String to, String text) { return "push do " + to + ": " + text; }
    }

    /** PRZED: klasa sama tworzy zależność — „przyspawana” do e-maila. */
    static class HardwiredShop {                                     // hardwired = na sztywno połączony
        private final EmailNotifier notifier = new EmailNotifier();
        String placeOrder(String customer) { return notifier.send(customer, "przyjęto zamówienie"); }
    }

    /** PO: zależność przychodzi z zewnątrz, przez konstruktor (constructor injection = wstrzykiwanie przez konstruktor). */
    static class InjectedShop {
        private final Notifier notifier;
        InjectedShop(Notifier notifier) { this.notifier = notifier; }
        String placeOrder(String customer) { return notifier.send(customer, "przyjęto zamówienie"); }
    }

    /**
     * 1. Odwrócenie sterowania: klasa przestaje sterować tworzeniem swoich zależności. W wersji PO o tym,
     * KTÓRY powiadamiacz dostanie sklep, decyduje kod „na zewnątrz” (composition root = miejsce składania).
     */
    static void withoutContainer() {
        section("1. Bez kontenera: PRZED (new w środku) → PO (wstrzykiwanie przez konstruktor)");

        show("PRZED", new HardwiredShop().placeOrder("Ola"));     // placeOrder = złóż zamówienie
        // WYNIK: PRZED → e-mail do Ola: przyjęto zamówienie

        // Ręczne składanie (composition root): to my decydujemy o implementacji, klasa nic nie wie.
        show("PO, SMS", new InjectedShop(new SmsNotifier()).placeOrder("Ola"));
        // WYNIK: PO, SMS → SMS do Ola: przyjęto zamówienie
        // W teście podajemy atrapę (fake) — bez wysyłania prawdziwych wiadomości:
        show("PO, atrapa", new InjectedShop((to, text) -> "[atrapa] " + to).placeOrder("Ola"));
        // WYNIK: PO, atrapa → [atrapa] Ola

        // PUŁAPKA: w dużej aplikacji ręczne składanie to setki linii new X(new Y(new Z(...))) w złej kolejności.
        // Kontener IoC robi to samo automatycznie — na podstawie typów parametrów i adnotacji.
        // DOBRA PRAKTYKA: zależności przez konstruktor i pola final — obiekt jest od razu kompletny i łatwy do testu.
        // W Springu: @Component na klasie + jeden konstruktor; @Autowired (= wstrzyknij automatycznie) jest
        //            zbędny, gdy konstruktor jest jeden (od Spring 4.3).
    }

    // =================================================================================================
    // 2. WŁASNE ADNOTACJE
    // =================================================================================================

    /**
     * 2. Adnotacja sama nic nie robi — to tylko etykieta. Sens nadaje jej kod, który ją CZYTA przez refleksję.
     * Pokażmy, co kontener „widzi” na klasach (listy z refleksji sortujemy po nazwie — kolejność zwracana
     * przez getDeclaredMethods() nie jest gwarantowana).
     */
    static void ownAnnotations() {
        section("2. Własne adnotacje: etykiety, które czyta kontener");

        for (Class<?> c : List.of(EmailNotifier.class, PushNotifier.class, ShoppingCart.class, AuditLog.class)) {
            show(c.getSimpleName(), describe(c));                    // describe = opisz
        }
        // WYNIK: EmailNotifier → @Component
        // WYNIK: PushNotifier → @Component, @Primary
        // WYNIK: ShoppingCart → @Component, @Scope(prototype)
        // WYNIK: AuditLog → @Component, flush()=@PreDestroy, open()=@PostConstruct

        // isAnnotationPresent = czy adnotacja jest obecna; getAnnotation = pobierz adnotację
        Scope scope = ShoppingCart.class.getAnnotation(Scope.class);
        show("zakres koszyka", scope.value());
        // WYNIK: zakres koszyka → prototype

        // PUŁAPKA: bez @Retention(RetentionPolicy.RUNTIME) adnotacja znika po kompilacji i isAnnotationPresent
        // zwraca false — kontener „nie widzi” komponentu, choć adnotacja stoi w kodzie.
        // W Springu: @Component, @Scope("prototype"), @Primary, @Qualifier("nazwa") — z pakietów
        //            org.springframework.stereotype / org.springframework.context.annotation / ...beans.factory.annotation;
        //            @PostConstruct i @PreDestroy pochodzą z jakarta.annotation (Spring 6 — dawniej javax.annotation).
    }

    /** Lista adnotacji klasy i jej metod, posortowana — do wydruku. */
    static String describe(Class<?> c) {
        List<String> parts = new ArrayList<>();
        if (c.isAnnotationPresent(Component.class)) parts.add("@Component");
        if (c.isAnnotationPresent(Primary.class)) parts.add("@Primary");
        if (c.isAnnotationPresent(Scope.class)) parts.add("@Scope(" + c.getAnnotation(Scope.class).value() + ")");
        Arrays.stream(c.getDeclaredMethods())
                .sorted(Comparator.comparing(Method::getName))
                .forEach(m -> Arrays.stream(m.getAnnotations())
                        .forEach(a -> parts.add(m.getName() + "()=@" + a.annotationType().getSimpleName())));
        return String.join(", ", parts);
    }

    // =================================================================================================
    // 3. MINI-KONTENER
    // =================================================================================================

    interface OrderRepository { void save(String order); List<String> findAll(); }   // repository = repozytorium

    @Component
    static class InMemoryOrderRepository implements OrderRepository {             // in memory = w pamięci
        private final List<String> orders = new ArrayList<>();
        @Override public void save(String order) { orders.add(order); }
        @Override public List<String> findAll() { return List.copyOf(orders); }
    }

    @Component
    static class OrderService {
        private final OrderRepository repository;
        private final Notifier notifier;
        OrderService(OrderRepository repository, @Qualifier("emailNotifier") Notifier notifier) {
            this.repository = repository;
            this.notifier = notifier;
        }
        String placeOrder(String customer, String item) {
            repository.save(customer + ": " + item);
            return notifier.send(customer, "zamówiono " + item);
        }
        int count() { return repository.findAll().size(); }
    }

    /**
     * 3. Kontener dostaje STAŁĄ listę klas. Spring zamiast tego skanuje classpath (classpath scanning =
     * przeszukiwanie ścieżki klas): przegląda pliki .class w pakiecie aplikacji i podpakietach, czyta ich
     * adnotacje (bez ładowania wszystkich klas) i rejestruje te z @Component. My pomijamy ten krok —
     * reszta (wybór konstruktora, rekurencja, cache) jest taka sama.
     */
    static void miniContainer() {
        section("3. Mini-kontener: rejestracja klas i wstrzykiwanie przez konstruktor");

        MiniContainer ctx = new MiniContainer(new Properties(),
                OrderService.class, InMemoryOrderRepository.class, EmailNotifier.class, SmsNotifier.class);

        OrderService service = ctx.getBean(OrderService.class);    // getBean = daj ziarno (obiekt)
        show("wynik", service.placeOrder("Ola", "kawa"));
        // WYNIK: wynik → e-mail do Ola: zamówiono kawa
        ctx.printLog();
        // WYNIK: dziennik kontenera (liczba elementów: 3):
        // WYNIK: • konstruktor: inMemoryOrderRepository
        // WYNIK: • konstruktor: emailNotifier
        // WYNIK: • konstruktor: orderService
        // Kolejność w dzienniku: najpierw zależności (repozytorium, notifier), na końcu OrderService — bo
        // konstruktor OrderService można wywołać dopiero, gdy ma wszystkie argumenty.

        // Drugie getBean nie tworzy nic nowego — singleton z pamięci podręcznej:
        show("ten sam obiekt?", ctx.getBean(OrderService.class) == service);
        // WYNIK: ten sam obiekt? → true
        show("liczba zamówień", ctx.getBean(OrderRepository.class).findAll().size());
        // WYNIK: liczba zamówień → 1

        // PUŁAPKA: klasa bez @Component nie jest beanem — kontener odmawia (Spring: po prostu jej nie znajdzie
        // i zgłosi brak beana przy wstrzykiwaniu).
        expectThrows("rejestracja klasy bez @Component", () -> new MiniContainer(new Properties(), HardwiredShop.class));
        // WYNIK: ✔ rejestracja klasy bez @Component → rzucono BeanException: klasa HardwiredShop nie ma @Component
        // W Springu: ApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);
        //            OrderService s = ctx.getBean(OrderService.class);
        //            a @ComponentScan("pakiet") (w Boot ukryty w @SpringBootApplication) włącza skanowanie.
    }

    // =================================================================================================
    // 4. WARTOŚCI Z KONFIGURACJI (@Value)
    // =================================================================================================

    @Component
    static class ShopInfo {
        final String name;
        final int maxLines;
        final String currency;
        ShopInfo(@Value("${sklep.nazwa}") String name,
                 @Value("${sklep.maks-pozycji}") int maxLines,
                 @Value("${sklep.waluta:PLN}") String currency) {         // currency = waluta
            this.name = name;
            this.maxLines = maxLines;
            this.currency = currency;
        }
        @Override public String toString() { return name + " (max " + maxLines + " pozycji, " + currency + ")"; }
    }

    /**
     * 4. Nie tylko obiekty są zależnościami — także liczby i teksty z konfiguracji. Kontener czyta
     * {@code ${klucz:domyślna}}, szuka klucza w Properties (t18_io_files/Io06Properties) i zamienia tekst na typ
     * parametru (int, boolean, String).
     */
    static void configValues() {
        section("4. Wartości z konfiguracji: @Value(\"${klucz:domyślna}\")");

        Properties props = new Properties();
        props.setProperty("sklep.nazwa", "Sklep u Ani");
        props.setProperty("sklep.maks-pozycji", "20");

        MiniContainer ctx = new MiniContainer(props, ShopInfo.class);
        show("ShopInfo", ctx.getBean(ShopInfo.class));
        // WYNIK: ShopInfo → Sklep u Ani (max 20 pozycji, PLN)
        // Waluty nie ma w props → użyto wartości domyślnej po dwukropku (PLN).

        // PUŁAPKA: literówka w kluczu albo brak właściwości wychodzi dopiero przy tworzeniu beana.
        expectThrows("brak sklep.nazwa", () -> new MiniContainer(new Properties(), ShopInfo.class).getBean(ShopInfo.class));
        // WYNIK: ✔ brak sklep.nazwa → rzucono BeanException: brak właściwości 'sklep.nazwa' (potrzebna w ShopInfo)
        Properties bad = new Properties();
        bad.setProperty("sklep.nazwa", "X");
        bad.setProperty("sklep.maks-pozycji", "dwadzieścia");
        expectThrows("zły typ", () -> new MiniContainer(bad, ShopInfo.class).getBean(ShopInfo.class));
        // WYNIK: ✔ zły typ → rzucono BeanException: 'dwadzieścia' to nie int (w ShopInfo)

        // DOBRA PRAKTYKA: wiele powiązanych kluczy → jeden typowany rekord konfiguracji (Spring04), a nie
        // dziesiątki @Value rozsianych po klasach — łatwiej sprawdzić i przetestować.
        // W Springu: ShopInfo(@Value("${sklep.nazwa}") String name, @Value("${sklep.waluta:PLN}") String currency)
        //            — brak klucza bez domyślnej kończy start aplikacji błędem
        //            „Could not resolve placeholder 'sklep.nazwa'” (nie da się rozwiązać symbolu zastępczego).
    }

    // =================================================================================================
    // 5. ZAKRESY: SINGLETON I PROTOTYP
    // =================================================================================================

    @Component @Scope("prototype")
    static class ShoppingCart {                                      // shopping cart = koszyk
        static int counter;                                          // counter = licznik (ile koszyków powstało)
        final int number = ++counter;
        final List<String> items = new ArrayList<>();
        @Override public String toString() { return "koszyk#" + number + items; }
    }

    @Component
    static class CheckoutDesk {                                      // checkout desk = stanowisko kasowe
        final ShoppingCart cart;                                     // PUŁAPKA: prototyp w singletonie
        final Supplier<ShoppingCart> carts;                          // Supplier = dostawca: nowy koszyk na żądanie
        CheckoutDesk(ShoppingCart cart, Supplier<ShoppingCart> carts) { this.cart = cart; this.carts = carts; }
    }

    /**
     * 5. Singleton (domyślny) = jeden obiekt na cały kontener. Prototyp = nowy obiekt przy każdym getBean.
     * Uwaga: singleton Springa to „jeden na kontener”, a nie wzorzec Singleton z prywatnym konstruktorem.
     */
    static void scopes() {
        section("5. Zakresy: singleton (domyślny) i prototyp");
        ShoppingCart.counter = 0;
        MiniContainer ctx = new MiniContainer(new Properties(), ShoppingCart.class, CheckoutDesk.class);

        ShoppingCart a = ctx.getBean(ShoppingCart.class);
        ShoppingCart b = ctx.getBean(ShoppingCart.class);
        a.items.add("kawa");
        show("a, b", a + " " + b);
        // WYNIK: a, b → koszyk#1[kawa] koszyk#2[]
        show("prototyp: a == b?", a == b);
        // WYNIK: prototyp: a == b? → false
        show("singleton: desk == desk?", ctx.getBean(CheckoutDesk.class) == ctx.getBean(CheckoutDesk.class));
        // WYNIK: singleton: desk == desk? → true

        // PUŁAPKA: prototyp wstrzyknięty do singletona powstaje RAZ — przy tworzeniu singletona — i zostaje na zawsze.
        CheckoutDesk desk = ctx.getBean(CheckoutDesk.class);
        show("koszyk w kasie (2 razy)", desk.cart.number + " i " + desk.cart.number);
        // WYNIK: koszyk w kasie (2 razy) → 3 i 3
        // Rozwiązanie: wstrzyknij DOSTAWCĘ (Supplier) i proś o nowy obiekt, kiedy trzeba.
        show("z dostawcy (2 razy)", desk.carts.get().number + " i " + desk.carts.get().number);
        // WYNIK: z dostawcy (2 razy) → 4 i 5

        // PUŁAPKA: singleton jest współdzielony przez wszystkie wątki/żądania — pola zmienne w nim (np. licznik,
        // lista „bieżący użytkownik”) to proszenie się o kłopoty. Singletony powinny być bezstanowe.
        // W Springu: @Scope("prototype") albo @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE); dostawca =
        //            ObjectProvider<ShoppingCart> (getObject()) albo jakarta.inject.Provider; w aplikacjach web
        //            są też zakresy request i session (= żądanie, sesja).
    }

    // =================================================================================================
    // 6. CYKL ŻYCIA: @PostConstruct, @PreDestroy
    // =================================================================================================

    @Component
    static class AuditLog {                                          // audit log = dziennik audytu
        private final List<String> log;
        AuditLog() { this.log = new ArrayList<>(); }
        @PostConstruct void open() { log.add("otwarty"); }           // open = otwórz
        @PreDestroy void flush() { log.add("zapisany"); }            // flush = opróżnij bufor (zapisz)
    }

    @Component
    static class ReportJob {                                         // report job = zadanie raportu
        private final AuditLog audit;
        ReportJob(AuditLog audit) { this.audit = audit; }
        @PostConstruct void start() { audit.log.add("raport gotowy do pracy"); }
        @PreDestroy void stop() { audit.log.add("raport zatrzymany"); }
    }

    /**
     * 6. Konstruktor powinien tylko przypisać pola. Rzeczy „cięższe” (otwarcie zasobu, wczytanie cache)
     * robi metoda @PostConstruct — wołana, gdy wszystkie zależności są już wstrzyknięte.
     * Przy zamykaniu kontener woła @PreDestroy w ODWROTNEJ kolejności tworzenia (najpierw ten, kto zależy).
     */
    static void lifecycle() {
        section("6. Cykl życia: konstruktor → @PostConstruct → praca → @PreDestroy");

        AuditLog audit;
        try (MiniContainer ctx = new MiniContainer(new Properties(), ReportJob.class, AuditLog.class)) {
            ctx.getBean(ReportJob.class);
            audit = ctx.getBean(AuditLog.class);
            ctx.printLog();
            // WYNIK: dziennik kontenera (liczba elementów: 4):
            // WYNIK: • konstruktor: auditLog
            // WYNIK: • @PostConstruct: auditLog.open()
            // WYNIK: • konstruktor: reportJob
            // WYNIK: • @PostConstruct: reportJob.start()
        } // ← tu close(): @PreDestroy
        show("wpisy audytu", audit.log);
        // WYNIK: dziennik kontenera (liczba elementów: 2):
        // WYNIK: • @PreDestroy: reportJob.stop()
        // WYNIK: • @PreDestroy: auditLog.flush()
        // WYNIK: wpisy audytu → [otwarty, raport gotowy do pracy, raport zatrzymany, zapisany]
        // ReportJob został utworzony po AuditLog (zależy od niego), więc zatrzymano go wcześniej — AuditLog
        // jest jeszcze sprawny, gdy ReportJob.stop() do niego pisze.

        // PUŁAPKA: dla prototypów kontener NIE woła @PreDestroy (nie śledzi ich po wydaniu) — sprzątanie należy
        // do tego, kto wziął obiekt. Tak samo działa Spring.
        // DOBRA PRAKTYKA: kontener zamykaj (try-with-resources / context.close()) — inaczej @PreDestroy się nie wykona.
        // W Springu: @PostConstruct / @PreDestroy (jakarta.annotation) albo @Bean(initMethod = "...",
        //            destroyMethod = "..."); Spring Boot zamyka kontekst przy zamykaniu JVM (shutdown hook).
    }

    // =================================================================================================
    // 7. INTERFEJS → IMPLEMENTACJA, NIEJEDNOZNACZNOŚĆ, @Primary, @Qualifier
    // =================================================================================================

    @Component
    static class AlertService {                                      // alert service = usługa alarmów
        final Notifier notifier;
        AlertService(Notifier notifier) { this.notifier = notifier; }
    }

    /**
     * 7. Prosimy o INTERFEJS, kontener szuka klas, które go implementują: zero → błąd „brak beana”,
     * jedna → sukces, kilka → błąd niejednoznaczności, chyba że jedna ma @Primary albo parametr ma @Qualifier.
     */
    static void interfacesAndAmbiguity() {
        section("7. Interfejs → implementacja; niejednoznaczność; @Primary i @Qualifier");

        MiniContainer one = new MiniContainer(new Properties(), SmsNotifier.class, AlertService.class);
        show("jedna implementacja", one.getBean(AlertService.class).notifier.send("Jan", "awaria"));
        // WYNIK: jedna implementacja → SMS do Jan: awaria

        expectThrows("zero implementacji", () -> new MiniContainer(new Properties(), AlertService.class).getBean(AlertService.class));
        // WYNIK: ✔ zero implementacji → rzucono BeanException: brak beana typu Notifier
        expectThrows("dwie implementacje", () -> new MiniContainer(new Properties(),
                EmailNotifier.class, SmsNotifier.class, AlertService.class).getBean(AlertService.class));
        // WYNIK: ✔ dwie implementacje → rzucono BeanException: 2 beany typu Notifier: [emailNotifier, smsNotifier] — użyj @Primary albo @Qualifier

        MiniContainer three = new MiniContainer(new Properties(),
                EmailNotifier.class, SmsNotifier.class, PushNotifier.class, AlertService.class);
        show("z @Primary", three.getBean(AlertService.class).notifier.send("Jan", "awaria"));
        // WYNIK: z @Primary → push do Jan: awaria
        // OrderService ma @Qualifier("emailNotifier") — wygrywa nazwa, a nie @Primary:
        MiniContainer q = new MiniContainer(new Properties(),
                EmailNotifier.class, PushNotifier.class, InMemoryOrderRepository.class, OrderService.class);
        show("z @Qualifier", q.getBean(OrderService.class).placeOrder("Jan", "lampka"));
        // WYNIK: z @Qualifier → e-mail do Jan: zamówiono lampka

        // PUŁAPKA: nazwa w @Qualifier to zwykły tekst — po zmianie nazwy klasy (EmailNotifier → MailNotifier)
        // domyślna nazwa beana też się zmienia, a kompilator nic nie powie. Błąd wyjdzie dopiero przy starcie.
        // W Springu: NoSuchBeanDefinitionException (brak) i NoUniqueBeanDefinitionException (kilka);
        //            @Primary na klasie, @Qualifier("smsNotifier") na parametrze; można też wstrzyknąć
        //            List<Notifier> (wszystkie implementacje) albo Map<String, Notifier> (nazwa → bean).
    }

    // =================================================================================================
    // 8. CYKLE ZALEŻNOŚCI
    // =================================================================================================

    @Component static class InvoiceService { InvoiceService(CustomerService c) { } }     // invoice = faktura
    @Component static class CustomerService { CustomerService(DiscountService d) { } }   // customer = klient
    @Component static class DiscountService { DiscountService(InvoiceService i) { } }    // discount = rabat

    /**
     * 8. Konstruktor A potrzebuje B, B potrzebuje C, a C potrzebuje A — nikogo nie da się utworzyć jako
     * pierwszego. Kontener trzyma listę „w trakcie tworzenia”; gdy klasa pojawia się na niej drugi raz → cykl.
     */
    static void cycles() {
        section("8. Cykle zależności: A → B → C → A");

        expectThrows("cykl", () -> new MiniContainer(new Properties(),
                InvoiceService.class, CustomerService.class, DiscountService.class).getBean(InvoiceService.class));
        // WYNIK: ✔ cykl → rzucono BeanException: cykl zależności: invoiceService → customerService → discountService → invoiceService
        // Bez wykrywania cykli rekurencja kręciłaby się do StackOverflowError — komunikat z listą klas jest
        // nieporównanie bardziej pomocny.

        // DOBRA PRAKTYKA: cykl to zwykle sygnał złego podziału odpowiedzialności. Wydziel wspólny kawałek do
        // trzeciej klasy (np. DiscountPolicy), od której zależą obie strony, albo użyj zdarzeń (Patterns06Observer).
        // W Springu: cykl w konstruktorach → BeanCurrentlyInCreationException przy starcie; od Spring Boot 2.6
        //            także cykle przez pola/settery są domyślnie zabronione (spring.main.allow-circular-references=false).
    }

    // =================================================================================================
    // 9. MINI AOP: PROXY „TRANSAKCYJNE” I PUŁAPKA SAMOWYWOŁANIA
    // =================================================================================================

    interface PaymentService {                                       // payment = płatność
        String pay(String order, int amount);                        // pay = zapłać; amount = kwota
        String payTwo(String first, String second);
    }

    @Component
    static class PaymentServiceImpl implements PaymentService {      // impl = implementacja
        @Transactional
        @Override public String pay(String order, int amount) {
            if (amount <= 0) throw new IllegalArgumentException("kwota musi być dodatnia");
            return "zapłacono " + order;
        }
        @Override public String payTwo(String first, String second) {
            return pay(first, 10) + " | " + pay(second, 20);       // this.pay(...) — omija proxy!
        }
    }

    /**
     * 9. AOP (aspect-oriented programming = programowanie aspektowe): dodatkowe zachowanie (transakcja, log,
     * bezpieczeństwo) doklejane z zewnątrz. Kontener NIE oddaje oryginału, tylko proxy (Proxy z t19), które
     * przed metodą @Transactional wypisuje BEGIN, a po niej COMMIT albo ROLLBACK.
     */
    static void miniAop() {
        section("9. Mini AOP: proxy transakcyjne i pułapka samowywołania");

        MiniContainer ctx = new MiniContainer(new Properties(), PaymentServiceImpl.class);
        PaymentService payments = ctx.getBean(PaymentService.class);
        show("klasa beana to proxy?", Proxy.isProxyClass(payments.getClass()));
        // WYNIK: klasa beana to proxy? → true
        ctx.printLog();
        // WYNIK: dziennik kontenera (liczba elementów: 2):
        // WYNIK: • konstruktor: paymentServiceImpl
        // WYNIK: • proxy: paymentServiceImpl opakowany (transakcje)

        show("pay", payments.pay("ZAM-1", 100));
        // WYNIK: pay → zapłacono ZAM-1
        ctx.printLog();
        // WYNIK: dziennik kontenera (liczba elementów: 2):
        // WYNIK: • BEGIN pay
        // WYNIK: • COMMIT pay
        expectThrows("pay z kwotą 0", () -> payments.pay("ZAM-2", 0));
        // WYNIK: ✔ pay z kwotą 0 → rzucono IllegalArgumentException: kwota musi być dodatnia
        ctx.printLog();
        // WYNIK: dziennik kontenera (liczba elementów: 2):
        // WYNIK: • BEGIN pay
        // WYNIK: • ROLLBACK pay (kwota musi być dodatnia)

        // PUŁAPKA: samowywołanie (self-invocation) — payTwo nie ma @Transactional, a w środku woła this.pay(...).
        // „this” to ORYGINAŁ, nie proxy — więc żadna transakcja nie startuje!
        show("payTwo", payments.payTwo("ZAM-3", "ZAM-4"));
        // WYNIK: payTwo → zapłacono ZAM-3 | zapłacono ZAM-4
        ctx.printLog();
        // WYNIK: dziennik kontenera (liczba elementów: 0):

        // PUŁAPKA: proxy JDK implementuje tylko INTERFEJSY — o klasę implementacji prosić się nie da.
        expectThrows("getBean(PaymentServiceImpl.class)", () -> ctx.getBean(PaymentServiceImpl.class));
        // WYNIK: ✔ getBean(PaymentServiceImpl.class) → rzucono BeanException: bean 'paymentServiceImpl' nie jest typu PaymentServiceImpl (to proxy JDK z samymi interfejsami)

        // DOBRA PRAKTYKA: metodę transakcyjną wołaj z INNEGO beana (przez proxy) albo przenieś granicę
        // transakcji wyżej (np. całe payTwo z @Transactional).
        // W Springu: @Transactional (org.springframework.transaction.annotation) działa przez proxy — dokładnie
        //            z tą samą pułapką samowywołania. Spring Boot domyślnie robi proxy klas (CGLIB, podklasa),
        //            więc getBean(Klasa) działa, ale samowywołanie nadal omija proxy. Domyślnie ROLLBACK
        //            następuje dla RuntimeException i Error, a dla wyjątków kontrolowanych (checked) — COMMIT.
    }

    // =================================================================================================
    // 10. PORÓWNANIE ZE SPRINGIEM
    // =================================================================================================

    /**
     * 10. Co zbudowaliśmy i co Spring robi lepiej. Kontener Springa ma ten sam szkielet, plus: skanowanie,
     * zachłanne tworzenie singletonów przy starcie (fail-fast = szybka porażka), konfigurację z wielu źródeł,
     * zdarzenia, profile, AOP z wyrażeniami (pointcut = punkt przecięcia), integrację z bazami i web.
     */
    static void compareWithSpring() {
        section("10. Nasz mini-kontener a ApplicationContext Springa");

        Map<String, String> map = new LinkedHashMap<>();
        map.put("new MiniContainer(props, klasy...)", "new AnnotationConfigApplicationContext(AppConfig.class)");
        map.put("stała lista klas", "@ComponentScan / @SpringBootApplication");
        map.put("getBean(Typ)", "context.getBean(Typ) — ale w kodzie: wstrzykiwanie przez konstruktor");
        map.put("tworzenie przy pierwszym getBean", "singletony tworzone zachłannie przy starcie");
        map.put("@Value(\"${k:domyślna}\") z Properties", "@Value + Environment (pliki, zmienne, argumenty)");
        map.put("Proxy JDK dla @Transactional", "Spring AOP: proxy JDK albo CGLIB");
        map.put("close() → @PreDestroy", "context.close() / zamknięcie JVM");
        showEach("mini → Spring", map);
        // WYNIK: mini → Spring (liczba kluczy: 7):
        // WYNIK: • new MiniContainer(props, klasy...) → new AnnotationConfigApplicationContext(AppConfig.class)
        // WYNIK: • stała lista klas → @ComponentScan / @SpringBootApplication
        // WYNIK: • getBean(Typ) → context.getBean(Typ) — ale w kodzie: wstrzykiwanie przez konstruktor
        // WYNIK: • tworzenie przy pierwszym getBean → singletony tworzone zachłannie przy starcie
        // WYNIK: • @Value("${k:domyślna}") z Properties → @Value + Environment (pliki, zmienne, argumenty)
        // WYNIK: • Proxy JDK dla @Transactional → Spring AOP: proxy JDK albo CGLIB
        // WYNIK: • close() → @PreDestroy → context.close() / zamknięcie JVM

        // DOBRA PRAKTYKA: w kodzie aplikacji prawie nigdy nie woła się getBean — to wzorzec Service Locator
        // (= lokalizator usług), który ukrywa zależności. Zależności widać w konstruktorze, kontener je podaje.
        // W Springu: @SpringBootApplication public class App { main → SpringApplication.run(App.class, args); }
        //            run(...) zwraca ConfigurableApplicationContext — ten sam „kontener”, który tu zbudowaliśmy.
    }

    // =================================================================================================
    // MINI-KONTENER — IMPLEMENTACJA (czytaj po sekcjach 3–9)
    // =================================================================================================

    /** Wyjątek kontenera (odpowiednik BeansException w Springu). */
    static final class BeanException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        BeanException(String message) { super(message); }
    }

    /** Nasz kontener IoC. AutoCloseable = można zamknąć w try-with-resources. */
    static final class MiniContainer implements AutoCloseable {
        private final List<Class<?>> registered;                     // registered = zarejestrowane klasy
        private final Properties properties;
        private final Map<Class<?>, Object> singletons = new HashMap<>();   // cache singletonów (nie drukujemy)
        private final List<Class<?>> inCreation = new ArrayList<>(); // in creation = w trakcie tworzenia (stos)
        private final List<Object> destroyOrder = new ArrayList<>(); // kolejność tworzenia (do @PreDestroy)
        private final List<String> log = new ArrayList<>();

        MiniContainer(Properties properties, Class<?>... classes) {
            for (Class<?> c : classes) {
                if (!c.isAnnotationPresent(Component.class)) {
                    throw new BeanException("klasa " + c.getSimpleName() + " nie ma @Component");
                }
            }
            this.registered = List.of(classes);
            this.properties = properties;
        }

        <T> T getBean(Class<T> type) {
            Class<?> impl = resolve(type, null);
            Object bean = obtain(impl);
            if (!type.isInstance(bean)) {                            // isInstance = czy jest instancją
                throw new BeanException("bean '" + beanName(impl) + "' nie jest typu " + type.getSimpleName()
                        + " (to proxy JDK z samymi interfejsami)");
            }
            return type.cast(bean);                                  // cast = rzutuj
        }

        /** Która zarejestrowana klasa pasuje do typu (i ewentualnie nazwy)? */
        private Class<?> resolve(Class<?> type, String qualifier) {
            List<Class<?>> candidates = registered.stream()          // candidates = kandydaci
                    .filter(type::isAssignableFrom)                  // isAssignableFrom = czy da się przypisać z
                    .filter(c -> qualifier == null || beanName(c).equals(qualifier))
                    .sorted(Comparator.comparing(Class::getSimpleName))
                    .collect(Collectors.toList());
            String what = type.getSimpleName() + (qualifier == null ? "" : " o nazwie '" + qualifier + "'");
            if (candidates.isEmpty()) throw new BeanException("brak beana typu " + what);
            if (candidates.size() == 1) return candidates.get(0);
            List<Class<?>> primaries = candidates.stream().filter(c -> c.isAnnotationPresent(Primary.class)).toList();
            if (primaries.size() == 1) return primaries.get(0);
            throw new BeanException(candidates.size() + " beany typu " + what + ": "
                    + candidates.stream().map(Spring01IocContainer::beanName).toList() + " — użyj @Primary albo @Qualifier");
        }

        /** Singleton z cache albo nowy obiekt; tu wykrywamy cykle. */
        private Object obtain(Class<?> impl) {
            boolean prototype = impl.isAnnotationPresent(Scope.class)
                    && impl.getAnnotation(Scope.class).value().equals("prototype");
            if (!prototype && singletons.containsKey(impl)) return singletons.get(impl);
            int index = inCreation.indexOf(impl);
            if (index >= 0) {
                List<String> cycle = new ArrayList<>();
                inCreation.subList(index, inCreation.size()).forEach(c -> cycle.add(beanName(c)));
                cycle.add(beanName(impl));
                throw new BeanException("cykl zależności: " + String.join(" → ", cycle));
            }
            inCreation.add(impl);
            try {
                Object bean = create(impl, prototype);
                if (!prototype) singletons.put(impl, bean);
                return bean;
            } finally {
                inCreation.remove(inCreation.size() - 1);
            }
        }

        private Object create(Class<?> impl, boolean prototype) {
            Constructor<?> ctor = chooseConstructor(impl);
            Parameter[] params = ctor.getParameters();
            Object[] args = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                args[i] = argumentFor(impl, params[i]);
            }
            Object bean;
            try {
                bean = ctor.newInstance(args);                       // newInstance = nowa instancja (woła konstruktor)
            } catch (ReflectiveOperationException e) {
                throw new BeanException("nie udało się utworzyć " + beanName(impl) + ": " + e);
            }
            log.add("konstruktor: " + beanName(impl));
            callAnnotated(bean, PostConstruct.class, "@PostConstruct");
            if (!prototype) destroyOrder.add(bean);
            return maybeProxy(impl, bean);
        }

        /** Jedyny konstruktor albo ten z @Inject. */
        private Constructor<?> chooseConstructor(Class<?> impl) {
            Constructor<?>[] all = impl.getDeclaredConstructors();
            if (all.length == 1) return all[0];
            List<Constructor<?>> marked = Arrays.stream(all).filter(c -> c.isAnnotationPresent(Inject.class)).toList();
            if (marked.size() == 1) return marked.get(0);
            throw new BeanException(impl.getSimpleName() + ": kilka konstruktorów, oznacz jeden @Inject");
        }

        private Object argumentFor(Class<?> impl, Parameter p) {
            Value value = p.getAnnotation(Value.class);
            if (value != null) return convert(placeholder(value.value(), impl), p.getType(), impl);
            if (p.getType() == Supplier.class) {                     // dostawca: getBean odroczone do get()
                Class<?> target = (Class<?>) ((ParameterizedType) p.getParameterizedType()).getActualTypeArguments()[0];
                Supplier<Object> supplier = () -> getBean(target);
                return supplier;
            }
            Qualifier q = p.getAnnotation(Qualifier.class);
            return obtain(resolve(p.getType(), q == null ? null : q.value()));
        }

        /** "${klucz:domyślna}" → wartość z properties. */
        private String placeholder(String expr, Class<?> impl) {
            String inner = expr.substring(2, expr.length() - 1);    // bez "${" i "}"
            int colon = inner.indexOf(':');
            String key = colon < 0 ? inner : inner.substring(0, colon);
            String found = properties.getProperty(key);
            if (found != null) return found;
            if (colon >= 0) return inner.substring(colon + 1);
            throw new BeanException("brak właściwości '" + key + "' (potrzebna w " + impl.getSimpleName() + ")");
        }

        private static Object convert(String text, Class<?> type, Class<?> impl) {
            try {
                if (type == int.class) return Integer.parseInt(text);
                if (type == boolean.class) return Boolean.parseBoolean(text);
                return text;
            } catch (NumberFormatException e) {
                throw new BeanException("'" + text + "' to nie " + type.getSimpleName() + " (w " + impl.getSimpleName() + ")");
            }
        }

        private void callAnnotated(Object bean, Class<? extends java.lang.annotation.Annotation> marker, String label) {
            List<Method> methods = Arrays.stream(bean.getClass().getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(marker))
                    .sorted(Comparator.comparing(Method::getName))
                    .toList();
            for (Method m : methods) {
                log.add(label + ": " + beanName(bean.getClass()) + "." + m.getName() + "()");
                try {
                    m.invoke(bean);                                  // invoke = wywołaj
                } catch (ReflectiveOperationException e) {
                    throw new BeanException(label + " nie powiódł się: " + e);
                }
            }
        }

        /** Jeśli jakaś metoda ma @Transactional — oddaj proxy zamiast oryginału. */
        private Object maybeProxy(Class<?> impl, Object target) {
            boolean tx = Arrays.stream(impl.getDeclaredMethods()).anyMatch(m -> m.isAnnotationPresent(Transactional.class));
            if (!tx) return target;
            log.add("proxy: " + beanName(impl) + " opakowany (transakcje)");
            InvocationHandler handler = (proxy, method, args) -> {
                Method real = impl.getMethod(method.getName(), method.getParameterTypes());
                boolean transactional = real.isAnnotationPresent(Transactional.class);
                if (transactional) log.add("BEGIN " + method.getName());
                try {
                    Object result = method.invoke(target, args);
                    if (transactional) log.add("COMMIT " + method.getName());
                    return result;
                } catch (InvocationTargetException e) {
                    if (transactional) log.add("ROLLBACK " + method.getName() + " (" + e.getCause().getMessage() + ")");
                    throw e.getCause();                              // oddaj oryginalny wyjątek wołającemu
                }
            };
            return Proxy.newProxyInstance(impl.getClassLoader(), impl.getInterfaces(), handler);
        }

        void printLog() {
            showEach("dziennik kontenera", log);
            log.clear();
        }

        @Override public void close() {
            for (int i = destroyOrder.size() - 1; i >= 0; i--) {
                callAnnotated(destroyOrder.get(i), PreDestroy.class, "@PreDestroy");
            }
            printLog();
        }
    }

    /** Domyślna nazwa beana: wartość z @Component albo nazwa klasy z małej litery. */
    static String beanName(Class<?> c) {
        Component component = c.getAnnotation(Component.class);
        if (component != null && !component.value().isEmpty()) return component.value();
        String simple = c.getSimpleName();
        return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • IoC: obiekt nie tworzy swoich zależności — dostaje je (najlepiej przez konstruktor, pola final).
     *   • Kontener: rejestracja (Spring: skanowanie @Component) → wybór klasy po typie → konstruktor przez
     *     refleksję → rekurencyjnie zależności → @PostConstruct → (proxy) → cache singletona.
     *   • Interfejs z 1 implementacją = OK; 0 = „brak beana”; 2+ = @Primary albo @Qualifier("nazwa").
     *   • Domyślna nazwa beana = nazwa klasy z małej litery (EmailNotifier → emailNotifier).
     *   • Singleton = jeden na kontener (bezstanowy!); prototyp = nowy przy każdym pobraniu, bez @PreDestroy.
     *   • Prototyp w singletonie powstaje raz → wstrzykuj dostawcę (ObjectProvider / Provider / Supplier).
     *   • @PreDestroy w odwrotnej kolejności tworzenia; zamykaj kontekst.
     *   • Cykl konstruktorów A → B → A = błąd startu; rozbij odpowiedzialności.
     *   • @Transactional działa przez proxy: wywołanie this.metoda() omija proxy (samowywołanie).
     *
     * PYTANIA KONTROLNE:
     *   1. Co dokładnie „odwraca” odwrócenie sterowania (IoC)? Kto przed, a kto po decyduje o implementacji?
     *   2. Czym różni się singleton Springa od wzorca Singleton z t22_design_patterns/Patterns04Singleton?
     *   3. Co wypisze (nasz kontener z sekcji 5)?
     *        ShoppingCart.counter = 0;
     *        MiniContainer ctx = new MiniContainer(new Properties(), ShoppingCart.class, CheckoutDesk.class);
     *        CheckoutDesk d = ctx.getBean(CheckoutDesk.class);
     *        System.out.println(d.cart.number + " " + ctx.getBean(CheckoutDesk.class).cart.number + " " + d.carts.get().number);
     *   4. ZNAJDŹ BŁĄD: metoda nie działa w transakcji, choć ma @Transactional. Dlaczego?
     *        public void importAll(List<Order> orders) { for (Order o : orders) importOne(o); }
     *        @Transactional public void importOne(Order o) { ... }
     *   5. Kontener ma EmailNotifier i SmsNotifier (bez @Primary). Co się stanie przy wstrzykiwaniu Notifier
     *      do konstruktora bez @Qualifier? Podaj dwa sposoby naprawy.
     *   6. ZNAJDŹ BŁĄD:
     *        @Component class Counter { private int value; int next() { return ++value; } }
     *      Counter jest używany przez wiele żądań HTTP jednocześnie.
     *   7. Dlaczego Spring tworzy singletony przy starcie aplikacji, a nie przy pierwszym użyciu?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        runChecks(false);
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        runChecks(true);
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 9 OK, ✘ 0 BŁĄD
    }

    private static void runChecks(boolean ref) {
        String s = ref ? " (wzorzec)" : "";
        Check.equal("ćw. 1: OrderService" + s, "orderService", () -> ref ? solution1("OrderService", "") : exercise1("OrderService", ""));
        Check.equal("ćw. 1: HTTPClient" + s, "HTTPClient", () -> ref ? solution1("HTTPClient", "") : exercise1("HTTPClient", ""));
        Check.equal("ćw. 1: jawna nazwa" + s, "mailer", () -> ref ? solution1("EmailNotifier", "mailer") : exercise1("EmailNotifier", "mailer"));

        Map<String, List<String>> deps = new LinkedHashMap<>();
        deps.put("service", List.of("repository", "mailer"));
        deps.put("controller", List.of("service"));
        deps.put("repository", List.of("dataSource"));
        deps.put("mailer", List.of());
        deps.put("dataSource", List.of());
        Check.equal("ćw. 2: kolejność tworzenia" + s, List.of("dataSource", "repository", "mailer", "service", "controller"),
                () -> ref ? solution2(deps) : exercise2(deps));

        Map<String, List<String>> cyclic = new LinkedHashMap<>();
        cyclic.put("invoice", List.of("customer"));
        cyclic.put("discount", List.of("invoice"));
        cyclic.put("customer", List.of("discount"));
        cyclic.put("audit", List.of());
        Check.equal("ćw. 3: cykl" + s, "customer → discount → invoice → customer",
                () -> ref ? solution3(cyclic) : exercise3(cyclic));
        Check.equal("ćw. 3: bez cyklu" + s, "", () -> ref ? solution3(deps) : exercise3(deps));

        Map<Class<?>, Object> available = new HashMap<>();
        available.put(Ex4Repo.class, new Ex4Repo());
        available.put(String.class, "sklep");
        Check.equal("ćw. 4: utworzenie" + s, "Ex4Service(repo=jest, name=sklep)",
                () -> String.valueOf(ref ? solution4(Ex4Service.class, available) : exercise4(Ex4Service.class, available)));
        Check.throwsException("ćw. 4: brak zależności" + s, IllegalStateException.class,
                () -> { if (ref) solution4(Ex4Service.class, Map.of()); else exercise4(Ex4Service.class, Map.of()); });

        List<String> log = new ArrayList<>();
        Check.equal("ćw. 5: proxy z dziennikiem" + s, "SMS do Ala: hej | [→ send, ← send]", () -> {
            Notifier n = ref ? solution5(Notifier.class, new SmsNotifier(), log) : exercise5(Notifier.class, new SmsNotifier(), log);
            return n.send("Ala", "hej") + " | " + log;
        });
    }

    static class Ex4Repo { }

    static class Ex4Service {
        final Ex4Repo repo;
        final String name;
        Ex4Service(Ex4Repo repo, String name) { this.repo = repo; this.name = name; }
        @Override public String toString() { return "Ex4Service(repo=" + (repo != null ? "jest" : "brak") + ", name=" + name + ")"; }
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć nazwę beana jak Spring: jeśli {@code explicitName} nie jest pusty — on;
     * w przeciwnym razie nazwa klasy z małej pierwszej litery, ALE gdy dwie pierwsze litery są wielkie
     * (HTTPClient), nazwa zostaje bez zmian (reguła java.beans.Introspector.decapitalize).
     * Podpowiedź: Character.isUpperCase(simpleName.charAt(1)).
     */
    static String exercise1(String simpleName, String explicitName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): mapa „bean → jego zależności”. Zwróć kolejność tworzenia: każdy bean PO swoich
     * zależnościach. Odwiedzaj klucze alfabetycznie, a zależności w podanej kolejności; każdy bean raz.
     * Podpowiedź: DFS (przeszukiwanie w głąb) — rekurencja: najpierw zależności, potem dodaj siebie (jeśli jeszcze nie ma).
     */
    static List<String> exercise2(Map<String, List<String>> deps) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): znajdź pierwszy cykl (klucze startowe alfabetycznie) i zwróć go jak nasz kontener:
     * {@code "a → b → c → a"}; brak cyklu → pusty tekst.
     * Podpowiedź: trzymaj listę „ścieżka” (w trakcie odwiedzania); gdy następny element już na niej jest — to cykl
     * od jego pozycji do końca + on sam.
     */
    static String exercise3(Map<String, List<String>> deps) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): PRZEPISZ ręczne składanie na wersję refleksyjną (to jest serce kontenera).
     * <pre>{@code
     * Ex4Service s = new Ex4Service(new Ex4Repo(), "sklep");     // PRZED
     * Object s = exercise4(Ex4Service.class, available);         // PO
     * }</pre>
     * Weź JEDYNY zadeklarowany konstruktor, dla każdego parametru weź obiekt z mapy po typie parametru.
     * Brak obiektu w mapie albo kilka konstruktorów → IllegalStateException.
     * Podpowiedź: getDeclaredConstructors(), getParameterTypes(), newInstance(args); ReflectiveOperationException
     * zamień na IllegalStateException.
     */
    static Object exercise4(Class<?> type, Map<Class<?>, Object> available) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): zwróć proxy interfejsu {@code iface}, które przed każdą metodą dopisuje do
     * {@code log} tekst {@code "→ nazwa"}, po niej {@code "← nazwa"}, i deleguje do {@code target}.
     * Podpowiedź: Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler), iface.cast(...).
     */
    static <T> T exercise5(Class<T> iface, T target, List<String> log) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String simpleName, String explicitName) {
        if (!explicitName.isEmpty()) return explicitName;
        if (simpleName.length() > 1 && Character.isUpperCase(simpleName.charAt(0)) && Character.isUpperCase(simpleName.charAt(1))) {
            return simpleName;
        }
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    static List<String> solution2(Map<String, List<String>> deps) {
        List<String> order = new ArrayList<>();
        deps.keySet().stream().sorted().forEach(k -> visit(k, deps, order));
        return order;
    }

    private static void visit(String bean, Map<String, List<String>> deps, List<String> order) {
        if (order.contains(bean)) return;
        for (String d : deps.getOrDefault(bean, List.of())) visit(d, deps, order);
        order.add(bean);
    }

    static String solution3(Map<String, List<String>> deps) {
        List<String> done = new ArrayList<>();
        for (String start : deps.keySet().stream().sorted().toList()) {
            String found = findCycle(start, deps, new ArrayList<>(), done);
            if (!found.isEmpty()) return found;
        }
        return "";
    }

    private static String findCycle(String bean, Map<String, List<String>> deps, List<String> path, List<String> done) {
        int i = path.indexOf(bean);
        if (i >= 0) {
            List<String> cycle = new ArrayList<>(path.subList(i, path.size()));
            cycle.add(bean);
            return String.join(" → ", cycle);
        }
        if (done.contains(bean)) return "";
        path.add(bean);
        for (String d : deps.getOrDefault(bean, List.of())) {
            String found = findCycle(d, deps, path, done);
            if (!found.isEmpty()) return found;
        }
        path.remove(path.size() - 1);
        done.add(bean);
        return "";
    }

    static Object solution4(Class<?> type, Map<Class<?>, Object> available) {
        Constructor<?>[] ctors = type.getDeclaredConstructors();
        if (ctors.length != 1) throw new IllegalStateException("oczekiwano jednego konstruktora w " + type.getSimpleName());
        Class<?>[] types = ctors[0].getParameterTypes();
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            args[i] = available.get(types[i]);
            if (args[i] == null) throw new IllegalStateException("brak zależności " + types[i].getSimpleName());
        }
        try {
            return ctors[0].newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static <T> T solution5(Class<T> iface, T target, List<String> log) {
        InvocationHandler handler = (proxy, method, args) -> {
            log.add("→ " + method.getName());
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            } finally {
                log.add("← " + method.getName());
            }
        };
        return iface.cast(Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Odwraca się to, KTO tworzy i łączy obiekty. Przed: klasa sama robi new EmailNotifier() i jest do niego
     *      przyspawana. Po: klasa tylko deklaruje potrzebę (parametr Notifier), a implementację wybiera kod
     *      zewnętrzny — ręczny composition root albo kontener.
     *   2. Wzorzec Singleton wymusza jedną instancję w całej JVM (prywatny konstruktor, pole static) i utrudnia
     *      testy. Singleton Springa to zwykła klasa; „jedna instancja” obowiązuje tylko w danym kontenerze —
     *      w teście możesz utworzyć ją ręcznie przez new.
     *   3. „1 1 2”. Prototyp ShoppingCart wstrzyknięto do singletona CheckoutDesk raz (koszyk nr 1), drugie
     *      getBean zwraca tego samego CheckoutDesk, a dopiero carts.get() tworzy nowy koszyk (nr 2).
     *   4. Samowywołanie: importAll woła this.importOne(o) — z pominięciem proxy, więc @Transactional jest
     *      ignorowane. Naprawa: @Transactional na importAll (jedna transakcja na całość) albo importOne w innym
     *      beanie, wołanym przez wstrzykniętą referencję (proxy).
     *   5. Błąd przy tworzeniu beana: niejednoznaczność (w Springu NoUniqueBeanDefinitionException). Naprawa:
     *      @Primary na jednej implementacji albo @Qualifier("smsNotifier") na parametrze (albo wstrzyknięcie
     *      List<Notifier>, jeśli potrzebne są wszystkie).
     *   6. Singleton ze stanem zmiennym współdzielonym przez wątki: ++value nie jest atomowe → zgubione
     *      zwiększenia (t21_concurrency). Naprawa: AtomicInteger albo — lepiej — bean bezstanowy, a stan w bazie.
     *   7. Fail-fast: błędy konfiguracji (brak beana, niejednoznaczność, cykl, brak właściwości) wychodzą od razu
     *      przy starcie, a nie w środku nocy przy pierwszym żądaniu. Leniwość można włączyć (@Lazy), ale traci
     *      się tę zaletę.
     */
    // </editor-fold>
}
