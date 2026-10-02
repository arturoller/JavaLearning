package t22_design_patterns;

import helpers.Check;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Singleton (singleton, „jedynak”) — dokładnie jedna instancja klasy
 *        (singleton = jedyny egzemplarz; instance = instancja, egzemplarz; eager = gorliwy, od razu;
 *         lazy = leniwy, dopiero gdy potrzebny; holder = „właściciel”, klasa pomocnicza trzymająca obiekt)
 *
 * W SKRÓCIE:
 *   Singleton gwarantuje, że istnieje tylko jeden obiekt danej klasy, i daje do niego globalny dostęp.
 *   Jest najbardziej znanym — i najbardziej nadużywanym — wzorcem. W tej lekcji poznasz cztery
 *   poprawne sposoby jego zapisu w Javie, dwa sposoby jego „złamania” oraz powody, dla których
 *   doświadczeni programiści unikają go na rzecz wstrzykiwania zależności.
 *
 * ANALOGIA: prezydent państwa albo centralny rejestr gruntów. Jest tylko jeden, wszyscy wiedzą, jak
 *   się do niego odwołać („prezydent RP”), a nie da się „założyć drugiego”. Ale też: jeśli prezydent
 *   zmieni zdanie, zmienia je dla wszystkich naraz, a urzędnik, który z niego korzysta, nie napisze
 *   tego w swoim CV — ukryta zależność.
 *
 * JAK TO DZIAŁA:
 *   Trzy składniki każdego singletona:
 *     1. prywatny konstruktor (nikt z zewnątrz nie zrobi {@code new});
 *     2. statyczne pole z jedyną instancją;
 *     3. publiczna metoda statyczna {@code getInstance()} zwracająca ją.
 *
 *   Sposoby zapisu:
 *     eager (gorliwy)         — instancja w polu {@code static final}; powstaje przy ładowaniu klasy
 *     lazy holder (leniwy)    — instancja w zagnieżdżonej klasie Holder; powstaje przy pierwszym getInstance()
 *     enum                    — jedna stała enum; odporna na serializację i refleksję
 *     double-checked locking  — leniwy z blokadą i volatile; trudny, rzadko potrzebny
 *
 *   Każda klasa jest inicjalizowana przez JVM tylko raz (w obrębie jednego ładowacza klas), a inicjalizacja
 *   jest bezpieczna dla wątków — na tym opiera się większość technik (t26_jvm/Jvm02ClassLoadingInit).
 *
 * SŁÓWKA:
 *   singleton = jedyny egzemplarz; instance = instancja; eager = gorliwy; lazy = leniwy; holder = nośnik;
 *   thread-safe = bezpieczny dla wielu wątków; global state = stan globalny; hidden dependency = ukryta
 *   zależność; reflection = refleksja; serialization = serializacja (zamiana obiektu na bajty);
 *   lock = blokada; volatile = „ulotna” (zmienna zawsze czytana z pamięci głównej); container = kontener.
 *
 * ZOBACZ TEŻ: t21_concurrency/Concurrency10MemoryModel (volatile i double-checked locking),
 *   t21_concurrency/Concurrency08ThreadSafetyPatterns (bezpieczeństwo wątkowe),
 *   t19_annotations_reflection/Annotations03ReflectionBasics (refleksja), t18_io_files/Io09Serialization (serializacja),
 *   t26_jvm/Jvm02ClassLoadingInit (kiedy klasa jest inicjalizowana), t08_enums/Enums01Basics,
 *   t25_testing/Testing03TestableDesign (testowalny projekt), t22_design_patterns/Patterns08DependencyInjection,
 *   t34_toward_spring/Spring01IocContainer (kontener i zasięg singleton)
 * </pre>
 */
public class Patterns04Singleton {

    /** Dziennik tworzenia obiektów: pokazuje KIEDY i ILE razy powstała instancja. Lista bezpieczna dla wątków. */
    static final List<String> CREATION_LOG = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws Exception {
        title("Patterns04 — Singleton (jedynak)");

        whyOneInstance();          // why one instance = po co jedna instancja
        eagerSingleton();          // eager singleton = singleton gorliwy
        lazyHolder();              // lazy holder = leniwy singleton z klasą Holder
        enumSingleton();           // enum singleton = singleton jako enum
        doubleCheckedLocking();    // double-checked locking = podwójnie sprawdzana blokada
        reflectionAttack();        // reflection attack = atak refleksją
        serializationAttack();     // serialization attack = atak serializacją
        whySingletonsAreHated();   // why singletons are hated = za co ich nie lubimy
        singletonByContainer();    // singleton by container = singleton z kontenera DI
        whenToUse();               // when to use = kiedy używać
        exercises();               // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PO CO JEDNA INSTANCJA?
    // =================================================================================================

    /** Zwykła klasa konfiguracji z publicznym konstruktorem — każdy może zrobić własną kopię. */
    static final class PlainConfig {
        private final String currency = "PLN";   // currency = waluta

        String currency() {
            return currency;
        }
    }

    /**
     * 1. Są obiekty, których „dwie sztuki” nie mają sensu: rejestr ustawień aplikacji, pula połączeń,
     * licznik numerów faktur. Dwie kopie mogłyby się rozjechać (dwa liczniki wydałyby ten sam numer faktury).
     */
    static void whyOneInstance() {
        section("1. Po co jedna instancja?");

        PlainConfig a = new PlainConfig();
        PlainConfig b = new PlainConfig();
        show("dwa new → ten sam obiekt?", a == b);   // == porównuje referencje (adresy), nie zawartość
        // WYNIK: dwa new → ten sam obiekt? → false

        // PUŁAPKA: „leniwy singleton na piechotę” — częsty błąd z podręczników:
        //   static Config instance;
        //   static Config getInstance() { if (instance == null) { instance = new Config(); } return instance; }
        // Dwa wątki mogą jednocześnie zobaczyć null i utworzyć DWIE instancje (wyścig, t21_concurrency/Concurrency02RaceConditions).
        // Poniżej cztery wersje, które tego błędu nie mają.
    }

    // =================================================================================================
    // 2. EAGER — gorliwy
    // =================================================================================================

    /** Eager: instancja powstaje przy inicjalizacji klasy (final static = jedno przypisanie, bezpieczne dla wątków). */
    static final class EagerConfig {
        private static final EagerConfig INSTANCE = new EagerConfig();

        private EagerConfig() {   // private = nikt spoza klasy nie zrobi new
            CREATION_LOG.add("EagerConfig");
        }

        static EagerConfig getInstance() {
            return INSTANCE;
        }

        /** Zwykła metoda statyczna — ale JEJ WYWOŁANIE też inicjalizuje klasę, więc tworzy instancję. */
        static String kind() {
            return "eager (gorliwy)";
        }
    }

    /**
     * 2. Najprostsza wersja. Wada: instancja powstaje przy pierwszym użyciu KLASY (nawet metody statycznej
     * niezwiązanej z instancją), niezależnie od tego, czy komuś będzie potrzebna. Jeśli jej utworzenie jest
     * kosztowne (otwarcie pliku, połączenie), wolisz wersję leniwą.
     */
    static void eagerSingleton() {
        section("2. Eager: pole static final");

        CREATION_LOG.clear();   // clear = wyczyść
        show("dziennik przed użyciem klasy", CREATION_LOG);
        // WYNIK: dziennik przed użyciem klasy → []
        show("rodzaj (wywołanie metody statycznej)", EagerConfig.kind());
        // WYNIK: rodzaj (wywołanie metody statycznej) → eager (gorliwy)
        show("dziennik po wywołaniu kind()", CREATION_LOG);
        // WYNIK: dziennik po wywołaniu kind() → [EagerConfig]
        show("dwa getInstance → ten sam obiekt?", EagerConfig.getInstance() == EagerConfig.getInstance());
        // WYNIK: dwa getInstance → ten sam obiekt? → true
        show("dziennik po dwóch getInstance", CREATION_LOG);
        // WYNIK: dziennik po dwóch getInstance → [EagerConfig]

        // DOBRA PRAKTYKA: jeśli już singleton — to najprostszy działający: eager albo enum. Prosty kod nie ma
        // błędów, których nie widać. Leniwość wprowadzaj tylko, gdy utworzenie obiektu naprawdę kosztuje.
    }

    // =================================================================================================
    // 3. LAZY HOLDER
    // =================================================================================================

    /** Lazy holder: instancja leży w osobnej, zagnieżdżonej klasie, która ładuje się dopiero przy getInstance(). */
    static final class LazyConfig {
        private LazyConfig() {
            CREATION_LOG.add("LazyConfig");
        }

        private static final class Holder {   // holder = nośnik
            static final LazyConfig INSTANCE = new LazyConfig();
        }

        static LazyConfig getInstance() {
            return Holder.INSTANCE;   // dopiero tu JVM inicjalizuje Holder, a więc tworzy instancję
        }

        static String kind() {
            return "lazy holder (leniwy)";
        }
    }

    /**
     * 3. Idiom „initialization-on-demand holder” (inicjalizacja na żądanie przez klasę pomocniczą).
     * Leniwy i bezpieczny dla wątków BEZ synchronized i volatile — gwarancję daje JVM, która inicjalizuje klasę
     * dokładnie raz i z blokadą wewnętrzną. To zalecana wersja leniwa dla zwykłych klas.
     */
    static void lazyHolder() {
        section("3. Lazy holder: leniwy bez synchronized");

        CREATION_LOG.clear();
        show("rodzaj (metoda statyczna)", LazyConfig.kind());
        // WYNIK: rodzaj (metoda statyczna) → lazy holder (leniwy)
        show("dziennik po kind()", CREATION_LOG);   // klasa Holder jeszcze nie załadowana
        // WYNIK: dziennik po kind() → []
        LazyConfig first = LazyConfig.getInstance();
        show("dziennik po pierwszym getInstance()", CREATION_LOG);
        // WYNIK: dziennik po pierwszym getInstance() → [LazyConfig]
        show("drugie getInstance → ta sama instancja?", first == LazyConfig.getInstance());
        // WYNIK: drugie getInstance → ta sama instancja? → true
        show("dziennik po drugim getInstance()", CREATION_LOG);
        // WYNIK: dziennik po drugim getInstance() → [LazyConfig]

        // Różnica względem eager: wywołanie kind() NIE stworzyło instancji.
        // DOBRA PRAKTYKA: wybierając wersję leniwą, wybierz holder, a nie ręczne blokady — jest krótszy i trudniej go zepsuć.
    }

    // =================================================================================================
    // 4. ENUM
    // =================================================================================================

    /** Singleton jako enum: jedna stała INSTANCE. Kompilator i JVM pilnują, że druga nie powstanie. */
    enum AppSettings {
        INSTANCE;   // jedyna stała = jedyna instancja

        private final String currency;

        AppSettings() {
            CREATION_LOG.add("AppSettings");
            this.currency = "PLN";
        }

        String currency() {
            return currency;
        }
    }

    /**
     * 4. Rozwiązanie z książki „Effective Java” (Joshua Bloch, pozycja 3): „jednoelementowy enum to najlepszy
     * sposób na singleton”. Chroni przed serializacją i refleksją (sekcje 6 i 7). Ograniczenie: enum nie może
     * dziedziczyć po innej klasie (może implementować interfejsy).
     */
    static void enumSingleton() {
        section("4. Enum: najbezpieczniejszy singleton");

        CREATION_LOG.clear();
        show("dziennik przed użyciem enum", CREATION_LOG);
        // WYNIK: dziennik przed użyciem enum → []
        show("waluta", AppSettings.INSTANCE.currency());
        // WYNIK: waluta → PLN
        show("dziennik po użyciu enum", CREATION_LOG);
        // WYNIK: dziennik po użyciu enum → [AppSettings]
        show("INSTANCE == INSTANCE", AppSettings.INSTANCE == AppSettings.valueOf("INSTANCE"));
        // WYNIK: INSTANCE == INSTANCE → true

        // Enum może mieć stan i metody jak zwykła klasa, może implementować interfejs (np. Notifier), więc w testach
        // da się podmienić inną implementacją tego samego interfejsu — o ile klient zależy od interfejsu.
        // PUŁAPKA: enum nie jest leniwy w sensie „dopiero przy getInstance” — stała powstaje przy pierwszym użyciu klasy
        // enum (jak eager). Zazwyczaj to nie przeszkadza.
    }

    // =================================================================================================
    // 5. DOUBLE-CHECKED LOCKING
    // =================================================================================================

    /** Double-checked locking: dwa sprawdzenia null — jedno bez blokady (szybkie), drugie pod blokadą (poprawne). */
    static final class DclConfig {
        private static volatile DclConfig instance;   // volatile = bez tego inny wątek mógłby zobaczyć niedokończony obiekt

        private DclConfig() {
            CREATION_LOG.add("DclConfig");
        }

        static DclConfig getInstance() {
            DclConfig local = instance;               // 1. odczyt bez blokady (większość wywołań kończy się tutaj)
            if (local == null) {
                synchronized (DclConfig.class) {      // synchronized = tylko jeden wątek naraz
                    local = instance;                 // 2. sprawdzenie PONOWNIE, już pod blokadą
                    if (local == null) {
                        local = new DclConfig();
                        instance = local;
                    }
                }
            }
            return local;
        }
    }

    /**
     * 5. Osiem zadań na czterech wątkach pytają równocześnie o instancję. Pokazujemy tylko fakty, które muszą
     * wyjść zawsze tak samo: instancja jest jedna, a konstruktor wykonał się raz.
     */
    static void doubleCheckedLocking() throws Exception {
        section("5. Double-checked locking (volatile + synchronized)");

        CREATION_LOG.clear();
        ExecutorService pool = Executors.newFixedThreadPool(4);   // pula 4 wątków (t21_concurrency/Concurrency04Executors)
        try {
            List<Callable<DclConfig>> tasks = Collections.nCopies(8, DclConfig::getInstance);   // 8 takich samych zadań
            Set<DclConfig> distinct = new HashSet<>();            // distinct = różne; DclConfig nie nadpisuje equals → identyczność
            for (Future<DclConfig> future : pool.invokeAll(tasks)) {   // invokeAll = uruchom wszystkie i poczekaj
                distinct.add(future.get());                       // get = pobierz wynik zadania
            }
            show("liczba różnych instancji z 8 zadań", distinct.size());
            // WYNIK: liczba różnych instancji z 8 zadań → 1
            show("dziennik tworzenia", CREATION_LOG);
            // WYNIK: dziennik tworzenia → [DclConfig]
        } finally {
            pool.shutdown();   // shutdown = zamknij pulę (zawsze w finally)
        }

        // DLACZEGO volatile: bez niego kompilator/procesor mogą zmienić kolejność zapisów i inny wątek zobaczy
        // referencję do obiektu, którego pola nie są jeszcze ustawione (t21_concurrency/Concurrency10MemoryModel).
        // DLACZEGO dwa sprawdzenia: pierwsze (bez blokady) jest szybkie dla 99,9% wywołań; drugie chroni
        // przed sytuacją, w której dwa wątki równocześnie przeszły pierwsze sprawdzenie.
        // PUŁAPKA: DCL bez volatile jest niepoprawny — działa „prawie zawsze” i psuje się rzadko, na innym procesorze.
        // DOBRA PRAKTYKA: dziś prawie nigdy nie potrzebujesz DCL — masz holder (sekcja 3) i enum (sekcja 4).
        // Jeśli kosztu blokady się nie boisz, wystarczy synchronized na całej metodzie getInstance().
    }

    // =================================================================================================
    // 6. ATAK REFLEKSJĄ
    // =================================================================================================

    /** Singleton, który broni się przed drugą instancją: konstruktor sprawdza, czy instancja już istnieje. */
    static final class GuardedConfig {
        private static final GuardedConfig INSTANCE = new GuardedConfig();

        private GuardedConfig() {
            if (INSTANCE != null) {   // podczas pierwszej inicjalizacji INSTANCE jest jeszcze null
                throw new IllegalStateException("Singleton już istnieje!");
            }
        }

        static GuardedConfig getInstance() {
            return INSTANCE;
        }
    }

    /**
     * 6. Refleksja (reflection, t19_annotations_reflection/Annotations03ReflectionBasics) potrafi wywołać prywatny
     * konstruktor po {@code setAccessible(true)} (set accessible = ustaw jako dostępny).
     */
    static void reflectionAttack() throws Exception {
        section("6. Atak refleksją na prywatny konstruktor");

        Constructor<EagerConfig> constructor = EagerConfig.class.getDeclaredConstructor();   // pobierz prywatny konstruktor
        constructor.setAccessible(true);   // wyłącza kontrolę słowa private
        EagerConfig second = constructor.newInstance();
        show("druga instancja przez refleksję — inna niż getInstance()?", second != EagerConfig.getInstance());
        // WYNIK: druga instancja przez refleksję — inna niż getInstance()? → true

        // Obrona w zwykłej klasie: konstruktor rzuca wyjątek, gdy instancja już istnieje.
        Constructor<GuardedConfig> guarded = GuardedConfig.class.getDeclaredConstructor();
        guarded.setAccessible(true);
        expectThrows("refleksja na klasie z zabezpieczeniem", () -> {
            try {
                guarded.newInstance();
            } catch (InvocationTargetException e) {   // InvocationTargetException = „opakowany wyjątek z konstruktora”
                throw (RuntimeException) e.getCause();
            }
        });
        // WYNIK: ✔ refleksja na klasie z zabezpieczeniem → rzucono IllegalStateException: Singleton już istnieje!

        // Enum: JVM odmawia tworzenia obiektów enum przez refleksję — i to bez żadnego kodu po naszej stronie.
        Constructor<?> enumConstructor = AppSettings.class.getDeclaredConstructors()[0];
        enumConstructor.setAccessible(true);
        expectThrows("refleksja na enum", () -> enumConstructor.newInstance("FAKE", 1));
        // WYNIK: ✔ refleksja na enum → rzucono IllegalArgumentException: Cannot reflectively create enum objects

        // DOBRA PRAKTYKA: nie projektuj bezpieczeństwa wokół „prywatnego konstruktora” — to konwencja, nie
        // zabezpieczenie. Jeśli to ważne (rzadko), użyj enum. W systemie z modułami Java (t30_build_modules) refleksja
        // na klasach z innych modułów bywa dodatkowo blokowana.
    }

    // =================================================================================================
    // 7. ATAK SERIALIZACJĄ
    // =================================================================================================

    /** Serializowalny singleton BEZ ochrony: deserializacja tworzy NOWY obiekt. */
    static final class BrokenSingleton implements Serializable {
        private static final long serialVersionUID = 1L;
        static final BrokenSingleton INSTANCE = new BrokenSingleton();

        private BrokenSingleton() {
        }
    }

    /** Ten sam singleton z metodą readResolve (read resolve = „przy odczycie podstaw to”). */
    static final class FixedSingleton implements Serializable {
        private static final long serialVersionUID = 1L;
        static final FixedSingleton INSTANCE = new FixedSingleton();

        private FixedSingleton() {
        }

        private Object readResolve() {   // JVM woła ją po deserializacji i podmienia wynik
            return INSTANCE;
        }
    }

    /** Zamienia obiekt na bajty i z powrotem (kopia przez serializację, t18_io_files/Io09Serialization). */
    @SuppressWarnings("unchecked")
    static <T> T roundTrip(T object) throws IOException, ClassNotFoundException {   // round trip = tam i z powrotem
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(object);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }

    /**
     * 7. Serializacja zapisuje stan obiektu do bajtów, a odczyt tworzy obiekt od nowa — z pominięciem konstruktora.
     * Dla singletona to oznacza drugą instancję.
     */
    static void serializationAttack() throws Exception {
        section("7. Atak serializacją");

        show("bez readResolve — kopia to ta sama instancja?", roundTrip(BrokenSingleton.INSTANCE) == BrokenSingleton.INSTANCE);
        // WYNIK: bez readResolve — kopia to ta sama instancja? → false
        show("z readResolve — kopia to ta sama instancja?", roundTrip(FixedSingleton.INSTANCE) == FixedSingleton.INSTANCE);
        // WYNIK: z readResolve — kopia to ta sama instancja? → true
        show("enum — kopia to ta sama instancja?", roundTrip(AppSettings.INSTANCE) == AppSettings.INSTANCE);
        // WYNIK: enum — kopia to ta sama instancja? → true

        // Enum jest serializowany tylko jako NAZWA stałej, a przy odczycie JVM wybiera istniejącą stałą — dlatego
        // enum nie wymaga readResolve ani serialVersionUID.
        // DOBRA PRAKTYKA: jeśli singleton ma być Serializable, dodaj readResolve — albo (lepiej) zrób z niego enum
        // lub w ogóle go nie serializuj.
    }

    // =================================================================================================
    // 8. DLACZEGO SINGLETONY SĄ NIELUBIANE
    // =================================================================================================

    /** Singleton ze STANEM (licznik odwiedzin) — globalna zmienna w przebraniu. */
    static final class VisitCounter {
        private static final VisitCounter INSTANCE = new VisitCounter();
        private int visits;

        private VisitCounter() {
        }

        static VisitCounter getInstance() {
            return INSTANCE;
        }

        int increment() {   // increment = zwiększ o jeden
            return ++visits;
        }
    }

    /** „Test 1”: po jednej wizycie licznik powinien pokazać 1. */
    static boolean test1OneVisit() {
        return VisitCounter.getInstance().increment() == 1;
    }

    /** „Test 2”: to samo, ale to inny test — powinien być niezależny od pierwszego. */
    static boolean test2OneVisit() {
        return VisitCounter.getInstance().increment() == 1;
    }

    /** Singleton-sender: w prawdziwym programie wysyłałby maile; tu liczy wywołania. */
    static final class MailerSingleton {
        private static final MailerSingleton INSTANCE = new MailerSingleton();
        private int sent;   // sent = wysłano

        private MailerSingleton() {
        }

        static MailerSingleton getInstance() {
            return INSTANCE;
        }

        void send(String to, String text) {
            sent++;   // (w prawdziwym programie: połączenie z serwerem pocztowym)
        }
    }

    /** Sklep z UKRYTĄ zależnością: sygnatura mówi „placeOrder(String)”, a w środku używa globalnego mailera. */
    static final class ShopWithSingleton {
        void placeOrder(String email) {
            MailerSingleton.getInstance().send(email, "Potwierdzenie zamówienia");
        }
    }

    /** Interfejs mailera — to samo, co robił singleton, ale jako zależność, którą można podmienić. */
    interface Mailer {
        void send(String to, String text);
    }

    /** Atrapa zapamiętująca wiadomości (recording fake = atrapa nagrywająca), do testów. */
    static final class RecordingMailer implements Mailer {
        private final List<String> sent = new ArrayList<>();

        @Override
        public void send(String to, String text) {
            sent.add(to + ": " + text);
        }

        List<String> sent() {
            return sent;
        }
    }

    /** Sklep z zależnością wstrzykniętą przez konstruktor — widoczną w sygnaturze i łatwą do podmiany. */
    static final class ShopWithDi {
        private final Mailer mailer;

        ShopWithDi(Mailer mailer) {
            this.mailer = mailer;
        }

        void placeOrder(String email) {
            mailer.send(email, "Potwierdzenie zamówienia");
        }
    }

    /**
     * 8. Trzy główne zarzuty: (1) stan globalny — każdy może go zmienić; (2) ukryta zależność — nie widać jej w
     * sygnaturze; (3) trudne testowanie — testy wpływają na siebie nawzajem.
     */
    static void whySingletonsAreHated() {
        section("8. Dlaczego singletony są nielubiane");

        // (1) Stan globalny i zanieczyszczanie testów: wynik drugiego testu zależy od tego, czy pierwszy już się wykonał.
        show("test 1 (pierwszy w procesie)", test1OneVisit());
        // WYNIK: test 1 (pierwszy w procesie) → true
        show("test 2 (ten sam proces — stan został po teście 1)", test2OneVisit());
        // WYNIK: test 2 (ten sam proces — stan został po teście 1) → false
        // Uruchomiony osobno, test 2 przeszedłby; po teście 1 — nie. Kolejność testów nie powinna mieć znaczenia!

        // (2) Ukryta zależność: nic w sygnaturze placeOrder(String) nie zdradza, że sklep wysyła maile.
        new ShopWithSingleton().placeOrder("a@example.com");
        show("globalny mailer: liczba wysłanych wiadomości", MailerSingleton.getInstance().sent);
        // WYNIK: globalny mailer: liczba wysłanych wiadomości → 1
        // Test tego sklepu wysłałby „prawdziwy” mail i zostawił ślad w globalnym liczniku.

        // (3) Z wstrzykiwaniem zależności (Patterns08DependencyInjection) test jest izolowany i deterministyczny:
        RecordingMailer fake = new RecordingMailer();
        new ShopWithDi(fake).placeOrder("b@example.com");
        show("atrapa odebrała", fake.sent());
        // WYNIK: atrapa odebrała → [b@example.com: Potwierdzenie zamówienia]

        // DOBRA PRAKTYKA: zamiast „kto chce, niech sięgnie po getInstance()”, przekaż obiekt przez konstruktor.
        // Reguła: jeśli klasa potrzebuje X, niech to będzie widać w jej konstruktorze.
        // PUŁAPKA: singleton z czymś zmiennym w środku (cache, licznik, bieżący użytkownik) to najgorsza wersja:
        // w aplikacji wielowątkowej dochodzi jeszcze wyścig o dane (t21_concurrency/Concurrency02RaceConditions).
    }

    // =================================================================================================
    // 9. SINGLETON Z KONTENERA DI (np. Spring)
    // =================================================================================================

    /**
     * Mini-kontener: dla danej nazwy tworzy obiekt RAZ i oddaje go przy kolejnych prośbach. To jest „singleton
     * zasięgu kontenera” — klasa obiektu jest zwykła, z publicznym konstruktorem.
     */
    static final class MiniContainer {
        private final Map<String, Object> beans = new HashMap<>();   // bean = obiekt zarządzany przez kontener

        @SuppressWarnings("unchecked")
        <T> T singleton(String name, Supplier<T> factory) {
            return (T) beans.computeIfAbsent(name, key -> factory.get());   // computeIfAbsent = oblicz, jeśli brak
        }
    }

    /** Zwykła klasa: publiczny konstruktor, żadnego statycznego pola — da się ją utworzyć w teście przez new. */
    static final class PriceCalculator {
        int gross(int net) {
            return net * 123 / 100;
        }
    }

    /**
     * 9. W Springu domyślny zasięg beana to „singleton”, ale to NIE jest wzorzec GoF: jedna instancja na
     * KONTENER (ApplicationContext) i na definicję beana, a nie na cały proces JVM. Dwa kontenery = dwie instancje;
     * ta sama klasa zdefiniowana jako dwa beany o różnych nazwach = dwie instancje. Klasa nie zna swojej
     * „jedynakowości” — nie ma prywatnego konstruktora ani getInstance().
     */
    static void singletonByContainer() {
        section("9. Singleton z kontenera DI (Spring) ≠ singleton GoF");

        MiniContainer container1 = new MiniContainer();
        MiniContainer container2 = new MiniContainer();
        PriceCalculator a = container1.singleton("calc", PriceCalculator::new);
        PriceCalculator b = container1.singleton("calc", PriceCalculator::new);
        PriceCalculator c = container2.singleton("calc", PriceCalculator::new);
        show("ten sam kontener, ta sama nazwa → ten sam obiekt?", a == b);
        // WYNIK: ten sam kontener, ta sama nazwa → ten sam obiekt? → true
        show("inny kontener → ten sam obiekt?", a == c);
        // WYNIK: inny kontener → ten sam obiekt? → false
        show("test bez kontenera: new PriceCalculator().gross(200)", new PriceCalculator().gross(200));
        // WYNIK: test bez kontenera: new PriceCalculator().gross(200) → 246

        // Zalety: klasa jest zwykła (łatwa do testowania i do podmiany), a „jedyność” to decyzja konfiguracji, nie klasy.
        // W Springu: @Component / @Bean tworzą singletony kontenera (SpringLearning, t34_toward_spring/Spring01IocContainer);
        // inne zasięgi (prototype, request) pozwalają tworzyć obiekt przy każdym pobraniu lub na żądanie HTTP.
        // PUŁAPKA: singleton Springa jest wspólny dla wszystkich wątków obsługujących żądania. Pole zmienne
        // w takim beanie (np. „bieżący użytkownik”) powoduje wyścigi — bean powinien być BEZSTANOWY.
    }

    // =================================================================================================
    // 10. KIEDY SINGLETON JEST OK
    // =================================================================================================

    /**
     * 10. Singleton jest w porządku, gdy jest BEZSTANOWY (nic w nim nie zmienia się w czasie) albo niezmienny.
     * Przykłady w JDK: {@code Runtime.getRuntime()}, {@code Comparator.naturalOrder()}, puste kolekcje z
     * {@code Collections.emptyList()}, stałe enum.
     * <pre>
     *   SINGLETON JEST OK, GDY:                            UNIKAJ, GDY:
     *   • obiekt jest bezstanowy lub niezmienny            • trzyma zmienny stan (licznik, cache, sesja)
     *   • naprawdę nie ma sensu druga instancja            • klasy potrzebują go „po cichu” przez getInstance()
     *   • to np. enum, Comparator, strategia bez pól       • testy muszą go zerować lub podmieniać
     *   • zarządza nim kontener DI                         • chcesz go kiedyś zamienić na wiele instancji
     * </pre>
     */
    static void whenToUse() {
        section("10. Kiedy singleton jest w porządku");

        show("Runtime.getRuntime() == Runtime.getRuntime()", Runtime.getRuntime() == Runtime.getRuntime());
        // WYNIK: Runtime.getRuntime() == Runtime.getRuntime() → true
        note("Bezstanowy singleton jest bezpieczny — nie ma czego zepsuć.");
        // WYNIK:    ℹ Bezstanowy singleton jest bezpieczny — nie ma czego zepsuć.
        note("Dla obiektów ze stanem wstrzykuj zależność zamiast sięgać po getInstance().");
        // WYNIK:    ℹ Dla obiektów ze stanem wstrzykuj zależność zamiast sięgać po getInstance().
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Singleton = prywatny konstruktor + statyczna instancja + dostęp globalny; jedna instancja na klasę/JVM.
     *   • Eager: static final; Lazy holder: instancja w zagnieżdżonej klasie Holder (leniwy, bez blokad);
     *     enum: jedna stała (odporny na serializację i refleksję); DCL: volatile + synchronized (rzadko potrzebny).
     *   • Naiwny „if (instance == null)” bez synchronizacji jest błędny w wielu wątkach.
     *   • Refleksja łamie prywatny konstruktor (setAccessible); enum tego nie dopuszcza
     *     (IllegalArgumentException: Cannot reflectively create enum objects).
     *   • Serializacja tworzy nowy obiekt, chyba że jest readResolve albo to enum.
     *   • Wady: stan globalny, ukryta zależność, trudne testy (kolejność testów ma znaczenie).
     *   • Zamiast tego: wstrzykiwanie zależności; kontener (Spring) zarządza „jedynością” — singleton kontenera
     *     to nie singleton GoF (jedna instancja na kontener i na definicję beana).
     *   • Bezstanowy singleton jest OK; singleton ze zmiennym stanem — raczej nie.
     *
     * PYTANIA KONTROLNE:
     *   1. Z jakich trzech elementów składa się klasyczny singleton?
     *   2. Dlaczego wersja „if (instance == null) instance = new X();” jest niebezpieczna w programie wielowątkowym?
     *   3. Po co volatile w double-checked locking?
     *   4. Co wypisze (dziennik tworzenia po dwóch wywołaniach):
     *        CREATION_LOG.clear();  LazyConfig.getInstance();  LazyConfig.getInstance();  System.out.println(CREATION_LOG);
     *      — uwaga: zakładamy, że LazyConfig nie był jeszcze użyty w tym procesie.
     *   5. Co wypisze:  System.out.println(roundTrip(BrokenSingleton.INSTANCE) == BrokenSingleton.INSTANCE);  ?
     *   6. ZNAJDŹ BŁĄD: singleton ma metodę  void setUser(String u)  i pole  private String user;  a jego instancję
     *      współdzielą wszystkie wątki serwera. Co pójdzie źle?
     *   7. ZNAJDŹ BŁĄD: w Springu ktoś pisze prywatny konstruktor i getInstance() w klasie oznaczonej jako bean
     *      singleton. Czy to potrzebne i co traci?
     *   8. Który zapis singletona jest odporny na serializację i refleksję bez dodatkowego kodu?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: ta sama instancja", true, () -> Exercise1Ids.getInstance() == Exercise1Ids.getInstance());
        Check.equal("ćw. 2: brutto 200", 246, () -> Exercise2Tax.INSTANCE.gross(200));
        Check.equal("ćw. 2: brutto 0", 0, () -> Exercise2Tax.INSTANCE.gross(0));
        Check.equal("ćw. 3: jedno zamówienie", List.of("a@example.com: Potwierdzenie zamówienia"), () -> {
            RecordingMailer fake = new RecordingMailer();
            new Exercise3Shop(fake).placeOrder("a@example.com");
            return fake.sent();
        });
        Check.equal("ćw. 3: dwa zamówienia", 2, () -> {
            RecordingMailer fake = new RecordingMailer();
            Exercise3Shop shop = new Exercise3Shop(fake);
            shop.placeOrder("a@example.com");
            shop.placeOrder("b@example.com");
            return fake.sent().size();
        });
        Check.equal("ćw. 4: jedna instancja i jeden konstruktor", List.of(1, 1), () -> {
            List<Integer> result = runEightTasks(Exercise4Lazy::getInstance);
            result.add(Exercise4Lazy.CREATED.get());
            return result;
        });
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", true, () -> Solution1Ids.getInstance() == Solution1Ids.getInstance());
        Check.equal("ćw. 2 (wzorzec): brutto 200", 246, () -> Solution2Tax.INSTANCE.gross(200));
        Check.equal("ćw. 2 (wzorzec): brutto 0", 0, () -> Solution2Tax.INSTANCE.gross(0));
        Check.equal("ćw. 3 (wzorzec): jedno zamówienie", List.of("a@example.com: Potwierdzenie zamówienia"), () -> {
            RecordingMailer fake = new RecordingMailer();
            new Solution3Shop(fake).placeOrder("a@example.com");
            return fake.sent();
        });
        Check.equal("ćw. 3 (wzorzec): dwa zamówienia", 2, () -> {
            RecordingMailer fake = new RecordingMailer();
            Solution3Shop shop = new Solution3Shop(fake);
            shop.placeOrder("a@example.com");
            shop.placeOrder("b@example.com");
            return fake.sent().size();
        });
        Check.equal("ćw. 4 (wzorzec): jedna instancja i jeden konstruktor", List.of(1, 1), () -> {
            List<Integer> result = runEightTasks(Solution4Lazy::getInstance);
            result.add(Solution4Lazy.CREATED.get());
            return result;
        });
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /**
     * Pomocnik ćwiczenia 4: uruchamia 8 zadań na 4 wątkach i zwraca JEDNOELEMENTOWĄ listę z liczbą
     * różnych instancji (lista zmienna, żeby sprawdzenie mogło dopisać drugi wynik).
     */
    static List<Integer> runEightTasks(Callable<Object> task) {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            Set<Object> distinct = new HashSet<>();
            for (Future<Object> future : pool.invokeAll(Collections.nCopies(8, task))) {
                distinct.add(future.get());
            }
            List<Integer> result = new ArrayList<>();
            result.add(distinct.size());
            return result;
        } catch (InterruptedException | ExecutionException e) {   // opakowujemy w wyjątek niekontrolowany
            throw new IllegalStateException(e);
        } finally {
            pool.shutdown();
        }
    }

    /**
     * ĆWICZENIE 1 (łatwe): zapisz singleton generatora identyfikatorów w wersji „lazy holder”: prywatny
     * konstruktor, zagnieżdżona klasa Holder z polem static final, getInstance() zwraca instancję z Holdera.
     * Podpowiedź: wzoruj się na LazyConfig (sekcja 3). Metodę getInstance() uzupełnij sam.
     */
    static final class Exercise1Ids {
        private Exercise1Ids() {
        }

        static Exercise1Ids getInstance() {
            // TODO: zwróć instancję z klasy Holder (najpierw ją dopisz)
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ klasę-singleton na enum. Stary kod:
     * <pre>{@code
     * public final class TaxSettings {
     *     private static final TaxSettings INSTANCE = new TaxSettings();
     *     private TaxSettings() { }
     *     public static TaxSettings getInstance() { return INSTANCE; }
     *     public int gross(int net) { return net * 123 / 100; }   // brutto = netto + 23% VAT
     * }
     * }</pre>
     * Uzupełnij metodę gross w enumie poniżej (zwraca {@code net * 123 / 100}).
     */
    enum Exercise2Tax {
        INSTANCE;

        int gross(int net) {   // gross = brutto (z podatkiem); net = netto
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 3 (średnie): sklep ma przestać używać singletona. Weź mailera przez konstruktor i w
     * {@code placeOrder(email)} wyślij wiadomość: adresat = email, treść = "Potwierdzenie zamówienia".
     * Podpowiedź: wzoruj się na ShopWithDi z sekcji 8.
     */
    static final class Exercise3Shop {
        private final Mailer mailer;

        Exercise3Shop(Mailer mailer) {
            this.mailer = mailer;
        }

        void placeOrder(String email) {
            // TODO: wyślij potwierdzenie przez mailer
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): double-checked locking. Uzupełnij getInstance() (pole {@code instance} jest
     * już volatile, konstruktor zlicza utworzenia w CREATED). Po 8 zadaniach na 4 wątkach ma powstać
     * dokładnie jedna instancja.
     * Podpowiedź: wzoruj się na DclConfig; sprawdź null, wejdź w synchronized(Exercise4Lazy.class), sprawdź ponownie.
     */
    static final class Exercise4Lazy {
        static final AtomicInteger CREATED = new AtomicInteger();   // liczy wywołania konstruktora
        private static volatile Exercise4Lazy instance;

        private Exercise4Lazy() {
            CREATED.incrementAndGet();
        }

        static Exercise4Lazy getInstance() {
            // TODO: twoje rozwiązanie (DCL)
            throw new UnsupportedOperationException("TODO");
        }
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static final class Solution1Ids {
        private Solution1Ids() {
        }

        private static final class Holder {
            static final Solution1Ids INSTANCE = new Solution1Ids();
        }

        static Solution1Ids getInstance() {
            return Holder.INSTANCE;
        }
    }

    enum Solution2Tax {
        INSTANCE;

        int gross(int net) {
            return net * 123 / 100;
        }
    }

    static final class Solution3Shop {
        private final Mailer mailer;

        Solution3Shop(Mailer mailer) {
            this.mailer = mailer;
        }

        void placeOrder(String email) {
            mailer.send(email, "Potwierdzenie zamówienia");
        }
    }

    static final class Solution4Lazy {
        static final AtomicInteger CREATED = new AtomicInteger();
        private static volatile Solution4Lazy instance;

        private Solution4Lazy() {
            CREATED.incrementAndGet();
        }

        static Solution4Lazy getInstance() {
            Solution4Lazy local = instance;
            if (local == null) {
                synchronized (Solution4Lazy.class) {
                    local = instance;
                    if (local == null) {
                        local = new Solution4Lazy();
                        instance = local;
                    }
                }
            }
            return local;
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Prywatny konstruktor, statyczne pole z jedyną instancją i publiczna metoda statyczna getInstance().
     *   2. Dwa wątki mogą jednocześnie zobaczyć null i oba wykonać new — powstają dwie instancje (wyścig).
     *   3. volatile zapobiega zmianie kolejności zapisów i gwarantuje widoczność: bez niego inny wątek mógłby zobaczyć
     *      niepustą referencję do obiektu, który nie ma jeszcze ustawionych pól.
     *   4. [LazyConfig] — instancja powstaje raz, przy pierwszym getInstance(); drugie wywołanie niczego nie dopisuje.
     *   5. false — deserializacja bez readResolve tworzy nowy obiekt.
     *   6. Pole user jest wspólne dla wszystkich wątków: jedno żądanie nadpisze użytkownika drugiemu (wyścig,
     *      wyciek danych między użytkownikami). Singleton nie powinien trzymać zmiennego stanu żądania.
     *   7. Niepotrzebne — kontener i tak zapewnia jedną instancję. Klasa traci łatwość testowania i
     *      podmiany (nie da się utworzyć jej przez new w teście ani wstrzyknąć atrapy).
     *   8. Enum z jedną stałą.
     */
    // </editor-fold>
}
