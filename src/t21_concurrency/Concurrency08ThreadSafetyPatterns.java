package t21_concurrency;

import helpers.Check;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wzorce bezpieczeństwa wątkowego — jak projektować klasy, żeby wyścigi w ogóle nie powstawały
 *        (thread safety = bezpieczeństwo wątkowe; pattern = wzorzec)
 *
 * W SKRÓCIE:
 *   Najlepszy wyścig to taki, który nie może wystąpić. Zamiast łatać kod blokadami, wybieramy strategię:
 *   1) nie zmieniaj (niezmienność), 2) nie współdziel (zamknięcie w wątku), 3) a dopiero gdy trzeba —
 *   synchronizuj (blokady, zmienne atomowe, kolekcje współbieżne). Do tego bezpieczne leniwe tworzenie,
 *   bezpieczna publikacja obiektów i bezstanowe serwisy (jak singletony w Springu).
 *
 * ANALOGIA:
 *   Tablica ogłoszeń wydrukowana i zalaminowana (niezmienna) — może ją czytać tłum, nikt nic nie zepsuje.
 *   Własny zeszyt każdego ucznia (zamknięcie w wątku) — nikt inny do niego nie pisze.
 *   Wspólna tablica kredowa (stan współdzielony) — potrzebny dyżurny, który pilnuje, kto pisze (blokada).
 *
 * JAK TO DZIAŁA:
 *   kolejność wyboru strategii:
 *   1. niezmienność      record, pola final, kopie obronne (List.copyOf)     → zero synchronizacji
 *   2. zamknięcie        zmienne lokalne, ThreadLocal (z remove!)             → brak współdzielenia
 *   3. bezstanowość      serwis bez pól z danymi żądania                       → nic do współdzielenia
 *   4. gotowe narzędzia  AtomicXxx, ConcurrentHashMap, BlockingQueue           → atomowość w bibliotece
 *   5. synchronizacja    synchronized / ReentrantLock — wszystkie dostępy pod TĄ SAMĄ blokadą
 *
 * SŁÓWKA:
 *   confinement = zamknięcie (ograniczenie do jednego wątku); stateless = bezstanowy; defensive copy = kopia
 *   obronna; lazy initialization = leniwa inicjalizacja; holder = posiadacz (klasa-pojemnik); safe publication =
 *   bezpieczna publikacja; escape = ucieczka (referencji); invariant = niezmiennik; guarded by = chroniony przez;
 *   leak = wyciek; request = żądanie; bean = ziarno (obiekt zarządzany przez Springa)
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop06Immutability (niezmienność), t21_concurrency/Concurrency02RaceConditions
 *   (wyścigi), t21_concurrency/Concurrency10MemoryModel (happens-before, final, volatile),
 *   t22_design_patterns/Patterns04Singleton (singleton jako wzorzec), t17_datetime/DateTime03Formatting
 * </pre>
 */
public class Concurrency08ThreadSafetyPatterns {

    public static void main(String[] args) {
        title("Concurrency08 — wzorce bezpieczeństwa wątkowego");

        immutability();          // immutability = niezmienność
        confinement();           // confinement = zamknięcie w wątku
        threadLocalLeak();       // thread local leak = wyciek ThreadLocal
        guardedInvariants();     // guarded invariants = niezmienniki chronione blokadą
        lazyInitialization();    // lazy initialization = leniwa inicjalizacja
        safePublication();       // safe publication = bezpieczna publikacja
        statelessServices();     // stateless services = bezstanowe serwisy
        formatters();            // formatters = formatery dat
        documentAndReview();     // document and review = dokumentowanie i przegląd kodu
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI
    // =================================================================================================

    static ExecutorService namedPool(String prefix, int threads) {
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory factory = r -> new Thread(r, prefix + "-" + counter.incrementAndGet());
        return Executors.newFixedThreadPool(threads, factory);
    }

    static void shutdownAndAwait(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    static void runAll(String prefix, Runnable... tasks) {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < tasks.length; i++) {
            threads.add(new Thread(tasks[i], prefix + "-" + (i + 1)));
        }
        threads.forEach(Thread::start);
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("przekroczono czas oczekiwania");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("przerwano", e);
        }
    }

    /** Pobiera wyniki Future w kolejności zgłoszenia — deterministyczna kolejność wydruku. */
    static <T> List<T> getAll(List<Future<T>> futures) {
        List<T> results = new ArrayList<>();
        try {
            for (Future<T> f : futures) {
                results.add(f.get(10, TimeUnit.SECONDS));
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return results;
    }

    // =================================================================================================
    // 1. NIEZMIENNOŚĆ
    // =================================================================================================

    /** Niezmienny koszyk: record + kopia obronna listy. Można go dać dowolnej liczbie wątków. */
    @ThreadSafe
    record Basket(String owner, List<String> items) {    // basket = koszyk; owner = właściciel; items = pozycje
        Basket {
            items = List.copyOf(items);                   // copyOf = skopiuj — kopia obronna, niezmienna
        }

        Basket with(String item) {                        // with = z (zwraca NOWY koszyk)
            List<String> copy = new ArrayList<>(items);
            copy.add(item);
            return new Basket(owner, copy);
        }
    }

    /** Record bez kopii obronnej — płytko niezmienny, ale lista w środku nadal zmienna. */
    record LeakyBasket(String owner, List<String> items) { }   // leaky = przeciekający

    /**
     * 1. Obiekt niezmienny jest bezpieczny wątkowo „za darmo”: nie ma czego chronić. Warunki: pola final,
     * brak setterów, kopie obronne zmiennych argumentów, this nie ucieka z konstruktora.
     */
    static void immutability() {
        section("1. Niezmienność — record, final, kopie obronne");

        List<String> source = new ArrayList<>(List.of("kawa", "chleb"));
        Basket basket = new Basket("ola", source);
        LeakyBasket leaky = new LeakyBasket("ola", source);
        source.add("wino");                               // ktoś zmienia listę po utworzeniu
        show("Basket (kopia obronna)", basket.items());
        // WYNIK: Basket (kopia obronna) → [kawa, chleb]
        show("LeakyBasket (bez kopii)", leaky.items());
        // WYNIK: LeakyBasket (bez kopii) → [kawa, chleb, wino]

        // „Zmiana” niezmiennego obiektu = nowy obiekt. Stary nadal istnieje i nadal jest poprawny.
        Basket bigger = basket.with("masło");
        show("nowy koszyk", bigger.items());
        // WYNIK: nowy koszyk → [kawa, chleb, masło]
        show("stary koszyk bez zmian", basket.items());
        // WYNIK: stary koszyk bez zmian → [kawa, chleb]
        expectThrows("basket.items().add(...)", () -> basket.items().add("x"));
        // WYNIK: ✔ basket.items().add(...) → rzucono UnsupportedOperationException: (brak komunikatu)

        // PUŁAPKA: record jest tylko PŁYTKO niezmienny. Pole final List może wskazywać na zmienną ArrayList,
        // a wtedy inny wątek widzi ją w trakcie zmian (LeakyBasket). Dlaczego: final chroni referencję, nie zawartość.
        // DOBRA PRAKTYKA: w konstruktorze kompaktowym (compact constructor) rób List.copyOf / Map.copyOf (Java 10+).
        // Dlaczego: kopia jest niezmienna i odcina obiekt od listy, którą ktoś trzyma na zewnątrz.
    }

    // =================================================================================================
    // 2. ZAMKNIĘCIE W WĄTKU (CONFINEMENT)
    // =================================================================================================

    /**
     * 2. Dane, których nie widzi żaden inny wątek, nie potrzebują synchronizacji. Najprostszy przypadek:
     * zmienne lokalne (każdy wątek ma własny stos). Drugi: ThreadLocal — osobna wartość dla każdego wątku.
     */
    static void confinement() {
        section("2. Zamknięcie w wątku — zmienne lokalne i ThreadLocal");

        // Zmienna lokalna: każdy wątek ma WŁASNY StringBuilder — żadnego współdzielenia, zero blokad.
        ConcurrentHashMap<String, String> built = new ConcurrentHashMap<>();
        Runnable builder = () -> {
            StringBuilder sb = new StringBuilder();       // lokalny → zamknięty w tym wątku
            for (int i = 1; i <= 5; i++) {
                sb.append(i);
            }
            built.put(Thread.currentThread().getName(), sb.toString());
        };
        runAll("budowniczy", builder, builder, builder);
        show("każdy wątek zbudował swoje", new TreeMap<>(built));
        // WYNIK: każdy wątek zbudował swoje → {budowniczy-1=12345, budowniczy-2=12345, budowniczy-3=12345}

        // ThreadLocal = zmienna lokalna wątku: jedna zmienna w kodzie, ale osobna wartość w każdym wątku.
        ThreadLocal<StringBuilder> perThread = ThreadLocal.withInitial(StringBuilder::new);   // withInitial = z wartością początkową
        ConcurrentHashMap<String, String> seen = new ConcurrentHashMap<>();
        Runnable appender = () -> {
            String me = Thread.currentThread().getName();
            perThread.get().append(me.charAt(me.length() - 1));    // get = pobierz wartość TEGO wątku
            perThread.get().append("!");
            seen.put(me, perThread.get().toString());
            perThread.remove();                           // remove = usuń — sprzątamy po sobie
        };
        runAll("w", appender, appender);
        show("ThreadLocal — każdy widzi tylko swoje", new TreeMap<>(seen));
        // WYNIK: ThreadLocal — każdy widzi tylko swoje → {w-1=1!, w-2=2!}

        // DOBRA PRAKTYKA: zmienne lokalne to pierwszy wybór. Dlaczego: kompilator i czytelnik od razu widzą,
        // że nikt inny ich nie dotknie. ThreadLocal stosuj oszczędnie (kontekst żądania, transakcji, użytkownika —
        // tak robi Spring Security w SecurityContextHolder i Spring w zarządzaniu transakcjami).
        // PUŁAPKA: zmienna lokalna przestaje być „zamknięta”, gdy referencję do obiektu przekażesz innemu
        // wątkowi (np. do zadania w puli albo do pola). Wtedy to już stan współdzielony.
    }

    // =================================================================================================
    // 3. THREADLOCAL W PULI — WYCIEK
    // =================================================================================================

    static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();   // current user = bieżący użytkownik

    /**
     * 3. W puli wątków wątki są używane WIELOKROTNIE. Wartość ThreadLocal nieusunięta po żądaniu A
     * zostaje w wątku i „przecieka” do żądania B. Pula z 1 wątkiem gwarantuje, że oba żądania trafią
     * do tego samego wątku — dlatego wynik jest deterministyczny.
     */
    static void threadLocalLeak() {
        section("3. PUŁAPKA: ThreadLocal w puli bez remove()");

        ExecutorService pool = namedPool("serwer", 1);
        try {
            // Żądanie A: loguje „ola” i NIE sprząta.
            Future<String> a = pool.submit(() -> {
                CURRENT_USER.set("ola");                  // set = ustaw
                return "A obsłużone dla " + CURRENT_USER.get();
            });
            // Żądanie B: niezalogowane — powinno widzieć null.
            Future<String> b = pool.submit(() -> "B widzi użytkownika: " + CURRENT_USER.get());
            show("żądanie A", a.get(5, TimeUnit.SECONDS));
            // WYNIK: żądanie A → A obsłużone dla ola
            show("żądanie B (bez remove)", b.get(5, TimeUnit.SECONDS));
            // WYNIK: żądanie B (bez remove) → B widzi użytkownika: ola
            note("wyciek danych między żądaniami — B dostało tożsamość A!");
            // WYNIK: ℹ wyciek danych między żądaniami — B dostało tożsamość A!

            // Naprawa: remove() w finally.
            Future<String> c = pool.submit(() -> {
                CURRENT_USER.set("jan");
                try {
                    return "C obsłużone dla " + CURRENT_USER.get();
                } finally {
                    CURRENT_USER.remove();                // ZAWSZE w finally
                }
            });
            Future<String> d = pool.submit(() -> "D widzi użytkownika: " + CURRENT_USER.get());
            show("żądanie C", c.get(5, TimeUnit.SECONDS));
            // WYNIK: żądanie C → C obsłużone dla jan
            show("żądanie D (po remove)", d.get(5, TimeUnit.SECONDS));
            // WYNIK: żądanie D (po remove) → D widzi użytkownika: null
        } catch (Exception e) {
            show("błąd", e);
        } finally {
            shutdownAndAwait(pool);
        }
        // Uwaga: po żądaniu A wątek „serwer-1” wciąż trzymał „ola” aż do C — dlatego B ją widziało.
        // PUŁAPKA: ThreadLocal w puli to też wyciek PAMIĘCI: wartość żyje tak długo jak wątek (czyli często
        // tyle, co aplikacja). Dlaczego: wątki puli nie umierają po zadaniu.
        // DOBRA PRAKTYKA: set(...); try { ... } finally { remove(); } — jak przy blokadach.
        // (Java 21+: ScopedValue jako bezpieczniejsza alternatywa — wartość znika sama po wyjściu z zakresu.)
    }

    // =================================================================================================
    // 4. NIEZMIENNIKI KILKU PÓL — JEDNA BLOKADA
    // =================================================================================================

    /** Dwa konta w jednym obiekcie; niezmiennik: suma zawsze 1000. Wszystkie dostępy pod tym samym monitorem. */
    @ThreadSafe
    static class TwoAccounts {                            // two accounts = dwa konta
        private int checking = 500;                       // @GuardedBy("this") — chronione przez this
        private int savings = 500;                        // @GuardedBy("this")

        synchronized void moveToSavings(int amount) {     // move to savings = przenieś na oszczędności
            checking -= amount;
            savings += amount;
        }

        synchronized void moveToChecking(int amount) {
            savings -= amount;
            checking += amount;
        }

        synchronized int total() {
            return checking + savings;
        }
    }

    /**
     * 4. Gdy niezmiennik obejmuje KILKA pól, pojedyncze zmienne atomowe nie wystarczą — każda jest atomowa
     * osobno, ale między dwiema zmianami ktoś zobaczy stan pośredni. Wtedy: jedna blokada na całą operację.
     */
    static void guardedInvariants() {
        section("4. Niezmiennik kilku pól — jedna blokada na całą operację");

        TwoAccounts accounts = new TwoAccounts();
        AtomicInteger badTotals = new AtomicInteger();    // ile razy ktoś zobaczył sumę ≠ 1000
        Runnable mover = () -> {
            for (int i = 0; i < 10_000; i++) {
                accounts.moveToSavings(7);
                accounts.moveToChecking(7);
            }
        };
        Runnable auditor = () -> {                        // auditor = audytor (czyta sumę)
            for (int i = 0; i < 10_000; i++) {
                if (accounts.total() != 1000) {
                    badTotals.incrementAndGet();
                }
            }
        };
        runAll("bank", mover, mover, auditor);
        show("suma po zakończeniu", accounts.total());
        // WYNIK: suma po zakończeniu → 1000
        show("audytor widział złą sumę (razy)", badTotals.get());
        // WYNIK: audytor widział złą sumę (razy) → 0

        // PUŁAPKA: dwa pola jako AtomicInteger i metody BEZ synchronized: każde odjęcie/dodanie jest atomowe,
        // ale audytor może trafić MIĘDZY nie i zobaczyć 993 (wynik zależy od uruchomienia). Dlaczego: atomowość
        // pojedynczych zmiennych nie składa się w atomowość operacji na dwóch zmiennych.
        // PUŁAPKA: total() bez synchronized też byłby błędem — czytający musi brać TĘ SAMĄ blokadę co piszący,
        // inaczej nie ma gwarancji ani atomowości odczytu obu pól, ani widoczności (Concurrency10).
        // DOBRA PRAKTYKA: zapisz w komentarzu, która blokada chroni które pole (@GuardedBy("this")).
    }

    // =================================================================================================
    // 5. LENIWA INICJALIZACJA
    // =================================================================================================

    static final AtomicInteger CONFIG_CREATED = new AtomicInteger();

    /** Drogi obiekt — liczymy, ile razy powstał. */
    static final class Config {                           // config = konfiguracja
        final String url;

        Config() {
            CONFIG_CREATED.incrementAndGet();
            this.url = "jdbc:h2:mem:kurs";
        }
    }

    /** ŹLE: sprawdź, potem utwórz. Hak afterCheck pozwala wymusić złośliwy przeplot w demie. */
    static final class RacyLazy {                         // racy lazy = leniwy z wyścigiem
        private Config instance;

        Config get(Runnable afterCheck) {
            if (instance == null) {
                afterCheck.run();                         // tutaj drugi wątek może zrobić to samo sprawdzenie
                instance = new Config();
            }
            return instance;
        }
    }

    /** DOBRZE: idiom klasy-posiadacza (holder). JVM inicjalizuje klasę Holder raz, leniwie i bezpiecznie. */
    static final class LazyConfig {
        private LazyConfig() { }

        private static final class Holder {               // ładowana dopiero przy pierwszym get()
            static final Config INSTANCE = new Config();
        }

        static Config get() {
            return Holder.INSTANCE;
        }
    }

    /** DOBRZE: enum z jedną stałą — singleton gwarantowany przez JVM (także przy serializacji i refleksji). */
    enum Registry {                                       // registry = rejestr
        INSTANCE;
        private final ConcurrentHashMap<String, Integer> values = new ConcurrentHashMap<>();

        int register(String key) {
            return values.merge(key, 1, Integer::sum);
        }
    }

    /**
     * 5. Leniwe tworzenie „sprawdź, czy null, potem utwórz” to check-then-act. Wymuszamy przeplot i widzimy
     * DWA obiekty. Potem idiom holder i enum — bez żadnej jawnej synchronizacji.
     */
    static void lazyInitialization() {
        section("5. Leniwa inicjalizacja — wyścig, holder, enum");

        CONFIG_CREATED.set(0);
        RacyLazy racy = new RacyLazy();
        CountDownLatch aChecked = new CountDownLatch(1);
        CountDownLatch bChecked = new CountDownLatch(1);
        runAll("leniwy",
                () -> racy.get(() -> { aChecked.countDown(); await(bChecked); }),
                () -> racy.get(() -> { bChecked.countDown(); await(aChecked); }));
        show("RacyLazy: utworzonych Config", CONFIG_CREATED.get());
        // WYNIK: RacyLazy: utworzonych Config → 2
        // Obie gałęzie zobaczyły null, zanim którakolwiek przypisała pole — więc powstały dwa „singletony”.

        CONFIG_CREATED.set(0);
        Runnable useHolder = () -> {
            for (int i = 0; i < 1000; i++) {
                LazyConfig.get();
            }
        };
        runAll("holder", useHolder, useHolder, useHolder, useHolder);
        show("holder: utworzonych Config", CONFIG_CREATED.get());
        // WYNIK: holder: utworzonych Config → 1
        show("holder: ten sam obiekt", LazyConfig.get() == LazyConfig.get());
        // WYNIK: holder: ten sam obiekt → true
        // Dlaczego działa: JLS 12.4.2 — inicjalizacja klasy odbywa się pod blokadą JVM dokładnie raz, a jej
        // efekty są widoczne dla każdego wątku, który potem użyje klasy. Holder ładuje się dopiero przy get().

        Runnable useEnum = () -> Registry.INSTANCE.register("klik");
        runAll("enum", useEnum, useEnum, useEnum);
        show("enum singleton: kliknięć", Registry.INSTANCE.register("klik") - 1);
        // WYNIK: enum singleton: kliknięć → 3

        // DOBRA PRAKTYKA: najprościej — inicjalizacja „chciwa” (eager = chciwa): static final X = new X().
        // Leniwie: holder albo enum. Dlaczego: zero kodu synchronizacji, gwarancje daje sama JVM.
        // Podwójne sprawdzanie (double-checked locking) działa tylko z volatile — t21_concurrency/Concurrency10MemoryModel.
        // W Springu tym wszystkim zajmuje się kontener: singletony tworzy raz przy starcie.
    }

    // =================================================================================================
    // 6. BEZPIECZNA PUBLIKACJA
    // =================================================================================================

    /** Obiekt z polami final — bezpieczny nawet przy publikacji przez wyścig (JLS 17.5). */
    static final class Point {                            // point = punkt
        final int x;
        final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    /**
     * 6. „Publikacja” = udostępnienie referencji innym wątkom. Bez odpowiedniego mechanizmu inny wątek może
     * zobaczyć referencję do obiektu, ale z polami jeszcze w wartościach domyślnych (0, null).
     */
    static void safePublication() {
        section("6. Bezpieczna publikacja obiektu");

        // Sposoby bezpiecznej publikacji (każdy daje happens-before między zapisem a odczytem):
        //   • inicjalizacja w static (static final Point ORIGIN = new Point(0, 0)),
        //   • zapis do pola volatile albo AtomicReference,
        //   • zapis do pola final innego, poprawnie zbudowanego obiektu,
        //   • zapis i odczyt pod tą samą blokadą,
        //   • włożenie do kolekcji współbieżnej (ConcurrentHashMap, BlockingQueue...) i odczyt z niej.
        AtomicReference<Point> published = new AtomicReference<>();   // AtomicReference = atomowa referencja
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<String> readerSaw = new AtomicReference<>();
        runAll("publikacja",
                () -> {
                    published.set(new Point(3, 4));       // set = zapis o semantyce volatile
                    ready.countDown();
                },
                () -> {
                    await(ready);
                    readerSaw.set(String.valueOf(published.get()));   // get = odczyt o semantyce volatile
                });
        show("czytelnik zobaczył", readerSaw.get());
        // WYNIK: czytelnik zobaczył → (3, 4)

        // Gwarancja pól final (JLS 17.5): jeśli konstruktor się zakończył i this nie uciekło wcześniej, KAŻDY wątek,
        // który zobaczy referencję, zobaczy też poprawne wartości pól final (i obiektów osiągalnych przez nie
        // według stanu z końca konstruktora) — nawet gdy referencję przekazano przez wyścig.
        // Pola NIE-final takiej gwarancji nie mają: przy publikacji przez zwykłe pole bez synchronizacji
        // czytelnik MOŻE zobaczyć 0 zamiast 3. Na HotSpot/x86 zdarza się to rzadko, ale JMM na to pozwala.

        // PUŁAPKA: ucieczka this z konstruktora, np.
        //     EventSource(EventBus bus) { bus.register(this); this.name = "x"; }   // final String name
        // Inny wątek może dostać obiekt przez bus ZANIM konstruktor przypisze name → zobaczy null.
        // Dlaczego: gwarancja final działa tylko od KOŃCA konstruktora. Rejestruj obiekt po jego zbudowaniu
        // (metoda fabryczna: najpierw new, potem bus.register(obj)).
        show("Point z polami final — niezmienny", new Point(1, 2));
        // WYNIK: Point z polami final — niezmienny → (1, 2)
    }

    // =================================================================================================
    // 7. BEZSTANOWE SERWISY (SINGLETONY SPRINGA)
    // =================================================================================================

    /** BŁĄD: serwis-singleton trzyma dane żądania w polu. Hak pause wymusza przeplot w demie. */
    static class StatefulGreetingService {                // stateful = stanowy
        private String currentUser;                       // pole współdzielone przez WSZYSTKIE żądania!

        String greet(String user, Runnable pause) {
            currentUser = user;
            pause.run();                                  // „praca” — np. zapytanie do bazy
            return "Cześć, " + currentUser;
        }
    }

    /** DOBRZE: dane żądania tylko w parametrach i zmiennych lokalnych. */
    @ThreadSafe
    static class StatelessGreetingService {               // stateless = bezstanowy
        String greet(String user, Runnable pause) {
            String name = user;                           // lokalna — zamknięta w wątku żądania
            pause.run();
            return "Cześć, " + name;
        }
    }

    /**
     * 7. W Springu serwis (@Service) jest domyślnie SINGLETONEM: jeden obiekt obsługuje wszystkie żądania,
     * a każde żądanie HTTP działa w innym wątku puli serwera. Pole z danymi żądania = wyścig między klientami.
     */
    static void statelessServices() {
        section("7. Bezstanowe serwisy — pole z danymi żądania to błąd");

        StatefulGreetingService stateful = new StatefulGreetingService();
        CountDownLatch olaSet = new CountDownLatch(1);
        CountDownLatch janSet = new CountDownLatch(1);
        ConcurrentHashMap<String, String> responses = new ConcurrentHashMap<>();   // responses = odpowiedzi
        runAll("http",
                () -> responses.put("ola", stateful.greet("ola", () -> { olaSet.countDown(); await(janSet); })),
                () -> {
                    await(olaSet);                        // jan wchodzi dopiero, gdy Ola już zapisała pole
                    responses.put("jan", stateful.greet("jan", janSet::countDown));
                });
        show("ola dostała (stanowy)", responses.get("ola"));
        // WYNIK: ola dostała (stanowy) → Cześć, jan
        show("jan dostał (stanowy)", responses.get("jan"));
        // WYNIK: jan dostał (stanowy) → Cześć, jan
        // Ola zapisała pole, jan je nadpisał, zanim Ola zbudowała odpowiedź — Ola widzi cudze dane.

        StatelessGreetingService stateless = new StatelessGreetingService();
        CountDownLatch olaSet2 = new CountDownLatch(1);
        CountDownLatch janSet2 = new CountDownLatch(1);
        responses.clear();
        runAll("http-ok",
                () -> responses.put("ola", stateless.greet("ola", () -> { olaSet2.countDown(); await(janSet2); })),
                () -> {
                    await(olaSet2);
                    responses.put("jan", stateless.greet("jan", janSet2::countDown));
                });
        show("ola dostała (bezstanowy)", responses.get("ola"));
        // WYNIK: ola dostała (bezstanowy) → Cześć, ola
        show("jan dostał (bezstanowy)", responses.get("jan"));
        // WYNIK: jan dostał (bezstanowy) → Cześć, jan

        // DOBRA PRAKTYKA: w singletonie trzymaj tylko pola final z zależnościami (repozytoria, klienty HTTP)
        // i niezmienną konfigurację. Dlaczego: to „stan” tylko do odczytu — bezpieczny. Dane żądania — parametry.
        // Liczniki/cache w singletonie → AtomicLong / ConcurrentHashMap (Concurrency06).
    }

    // =================================================================================================
    // 8. SIMPLEDATEFORMAT KONTRA DATETIMEFORMATTER
    // =================================================================================================

    /**
     * 8. Klasyczna pułapka: {@code SimpleDateFormat} trzyma stan roboczy w polach (wewnętrzny Calendar),
     * więc jeden obiekt używany przez wiele wątków daje błędne daty albo wyjątki. {@code DateTimeFormatter}
     * jest niezmienny i bezpieczny wątkowo.
     */
    static void formatters() {
        section("8. SimpleDateFormat (niebezpieczny) kontra DateTimeFormatter (niezmienny)");

        // DateTimeFormatter: JEDNA stała dla całej aplikacji, dzielona przez 4 wątki.
        DateTimeFormatter iso = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT);   // ofPattern = według wzorca
        ExecutorService pool = namedPool("format", 4);
        List<String> formatted;
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int day = 1; day <= 4; day++) {
                LocalDate date = LocalDate.of(2026, 1, day);
                futures.add(pool.submit(() -> iso.format(date)));
            }
            formatted = getAll(futures);                  // kolejność zgłoszenia, nie zakończenia
        } finally {
            shutdownAndAwait(pool);
        }
        show("DateTimeFormatter w 4 wątkach", formatted);
        // WYNIK: DateTimeFormatter w 4 wątkach → [01.01.2026, 02.01.2026, 03.01.2026, 04.01.2026]

        // PUŁAPKA: static final SimpleDateFormat FMT = new SimpleDateFormat(...) używany z wielu wątków.
        // Typowe objawy: zła data (cudzy dzień), NumberFormatException, ArrayIndexOutOfBoundsException —
        // (wynik zależy od uruchomienia), dlatego tego nie uruchamiamy. Dlaczego: format()/parse() zapisują
        // wyniki pośrednie w polach obiektu, które drugi wątek w tym czasie nadpisuje.
        // Stare obejście: osobny obiekt na wątek przez ThreadLocal.
        ThreadLocal<SimpleDateFormat> legacy = ThreadLocal.withInitial(() -> {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));   // jawna strefa — wynik niezależny od komputera
            return f;
        });
        try {
            show("SimpleDateFormat przez ThreadLocal", legacy.get().format(new Date(0L)));
            // WYNIK: SimpleDateFormat przez ThreadLocal → 1970-01-01
        } finally {
            legacy.remove();
        }
        // DOBRA PRAKTYKA: w nowym kodzie java.time + DateTimeFormatter jako static final. Dlaczego: niezmienny
        // obiekt nie wymaga ani ThreadLocal, ani blokad (t17_datetime/DateTime03Formatting).
    }

    // =================================================================================================
    // 9. DOKUMENTOWANIE I PRZEGLĄD KODU
    // =================================================================================================

    /** Własna adnotacja dokumentująca (pomysł z książki „Java Concurrency in Practice”: @ThreadSafe, @GuardedBy). */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface ThreadSafe { }

    /**
     * 9. Bezpieczeństwo wątkowe to część KONTRAKTU klasy — trzeba je opisać. Javadoc JDK robi to zawsze
     * („This class is thread-safe” / „not synchronized”). Tu własna adnotacja odczytana refleksją.
     */
    static void documentAndReview() {
        section("9. Dokumentowanie bezpieczeństwa wątkowego i lista kontrolna");

        List<Class<?>> classes = List.of(Basket.class, LeakyBasket.class, TwoAccounts.class,
                StatefulGreetingService.class, StatelessGreetingService.class);
        List<String> report = new ArrayList<>();
        for (Class<?> c : classes) {
            boolean safe = c.isAnnotationPresent(ThreadSafe.class);   // isAnnotationPresent = czy ma adnotację
            report.add(c.getSimpleName() + (safe ? " — @ThreadSafe" : " — brak deklaracji (zakładaj: NIE)"));
        }
        showEach("deklaracje klas", report);
        // WYNIK: deklaracje klas (liczba elementów: 5):
        // WYNIK: • Basket — @ThreadSafe
        // WYNIK: • LeakyBasket — brak deklaracji (zakładaj: NIE)
        // WYNIK: • TwoAccounts — @ThreadSafe
        // WYNIK: • StatefulGreetingService — brak deklaracji (zakładaj: NIE)
        // WYNIK: • StatelessGreetingService — @ThreadSafe

        List<String> checklist = List.of(
                "Czy obiekt jest współdzielony między wątkami? (singleton, pole static, cache)",
                "Czy da się go zrobić niezmiennym? (final, record, List.copyOf)",
                "Czy dane żądania trzymane są w polach? → przenieś do parametrów",
                "Czy każde pole zmienne ma JEDNĄ blokadę, pod którą czytamy i piszemy?",
                "Czy są operacje złożone (check-then-act, get+put)? → merge/compute lub blokada",
                "Czy ThreadLocal ma remove() w finally?",
                "Czy this nie ucieka z konstruktora?");
        showEach("lista kontrolna przeglądu kodu", checklist);
        // WYNIK: lista kontrolna przeglądu kodu (liczba elementów: 7):
        // WYNIK: • Czy obiekt jest współdzielony między wątkami? (singleton, pole static, cache)
        // WYNIK: • Czy da się go zrobić niezmiennym? (final, record, List.copyOf)
        // WYNIK: • Czy dane żądania trzymane są w polach? → przenieś do parametrów
        // WYNIK: • Czy każde pole zmienne ma JEDNĄ blokadę, pod którą czytamy i piszemy?
        // WYNIK: • Czy są operacje złożone (check-then-act, get+put)? → merge/compute lub blokada
        // WYNIK: • Czy ThreadLocal ma remove() w finally?
        // WYNIK: • Czy this nie ucieka z konstruktora?

        // DOBRA PRAKTYKA: jeśli dokumentacja klasy milczy, zakładaj, że NIE jest bezpieczna wątkowo.
        // Dlaczego: tak jest z większością klas JDK (ArrayList, HashMap, StringBuilder, SimpleDateFormat).
        // PUŁAPKA: adnotacja niczego nie wymusza — to tylko dokumentacja. Kompilator nie sprawdzi, czy klasa
        // oznaczona @ThreadSafe naprawdę taka jest (robią to częściowo narzędzia analizy statycznej).
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Kolejność strategii: niezmienność → zamknięcie w wątku → bezstanowość → gotowe narzędzia
     *     (Atomic, Concurrent*) → własna synchronizacja.
     *   • Niezmienny: pola final, brak setterów, kopie obronne (List.copyOf), this nie ucieka z konstruktora.
     *   • record jest płytko niezmienny — kopiuj listy w konstruktorze kompaktowym.
     *   • Zmienne lokalne są zamknięte w wątku; ThreadLocal = wartość na wątek; w puli ZAWSZE remove() w finally.
     *   • Niezmiennik kilku pól → jedna blokada na całą operację, także przy ODCZYCIE.
     *   • Leniwy singleton: holder (static nested class) albo enum; „if (x == null) x = new X()” to wyścig.
     *   • Bezpieczna publikacja: static init, volatile/Atomic*, pole final, blokada, kolekcja współbieżna.
     *   • Pola final widać poprawnie po zakończeniu konstruktora (JLS 17.5) — o ile this nie uciekło.
     *   • Singleton Springa: tylko zależności i konfiguracja w polach; dane żądania w parametrach.
     *   • SimpleDateFormat — niebezpieczny; DateTimeFormatter — niezmienny, static final.
     *
     * PYTANIA KONTROLNE:
     *   1. Wymień trzy strategie, które usuwają potrzebę synchronizacji.
     *   2. Co wypisze:
     *        List<String> src = new ArrayList<>(List.of("a"));
     *        record R(List<String> l) { }
     *        R r = new R(src); src.add("b");
     *        System.out.println(r.l());
     *   3. ZNAJDŹ BŁĄD (Spring):
     *        @Service class OrderService { private Order current;
     *            void handle(Order o) { current = o; validate(); save(current); } }
     *   4. ZNAJDŹ BŁĄD:
     *        pool.submit(() -> { USER.set(name); process(); });   // USER to ThreadLocal
     *   5. Dlaczego idiom holder jest leniwy i bezpieczny wątkowo bez synchronized?
     *   6. Klasa ma dwa pola AtomicInteger min i max z niezmiennikiem min ≤ max. Czy to wystarczy? Dlaczego?
     *   7. Co gwarantuje JMM dla pól final, a czego nie gwarantuje dla zwykłych pól?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        List<String> requests = Arrays.asList("ola", null, "jan", null);   // asList = jako lista (pozwala na null)
        Check.equal("ćw. 1: kopia obronna w Order", List.of("kawa", "chleb"), () -> {
            List<String> src = new ArrayList<>(List.of("kawa", "chleb"));
            OrderSnapshot order = exercise1("ZAM-1", src);
            src.add("wino");
            return order.items();
        });
        Check.equal("ćw. 2: ThreadLocal w puli bez wycieku", List.of("ola", "anonim", "jan", "anonim"),
                () -> exercise2(requests));
        Check.equal("ćw. 3: bezstanowy serwis cen (4 wątki)", List.of(1000, 900, 750, 500),
                () -> exercise3(1000, List.of(0, 10, 25, 50)));
        Check.equal("ćw. 4: cache — droga funkcja wywołana raz na klucz", 5, () -> exercise4(8, 5));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("kawa", "chleb"), () -> {
            List<String> src = new ArrayList<>(List.of("kawa", "chleb"));
            OrderSnapshot order = solution1("ZAM-1", src);
            src.add("wino");
            return order.items();
        });
        Check.equal("ćw. 2 (wzorzec)", List.of("ola", "anonim", "jan", "anonim"), () -> solution2(requests));
        Check.equal("ćw. 3 (wzorzec)", List.of(1000, 900, 750, 500), () -> solution3(1000, List.of(0, 10, 25, 50)));
        Check.equal("ćw. 4 (wzorzec)", 5, () -> solution4(8, 5));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /** Migawka zamówienia — do ćwiczenia 1. Celowo BEZ kopii w konstruktorze: kopię zrób w exercise1. */
    record OrderSnapshot(String id, List<String> items) { }   // snapshot = migawka

    /**
     * ĆWICZENIE 1 (łatwe): utwórz OrderSnapshot tak, by późniejsza zmiana listy {@code items} nie zmieniała
     * migawki (i by migawki nie dało się zmienić przez items()).
     * Podpowiedź: przekaż do konstruktora List.copyOf(items) — Java 10+.
     */
    static OrderSnapshot exercise1(String id, List<String> items) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): obsłuż żądania w puli z JEDNYM wątkiem (namedPool("cw2", 1)). Żądanie z nazwą
     * ustawia ją w ThreadLocal, żądanie z null — nie ustawia. Każde żądanie zwraca bieżącego użytkownika
     * z ThreadLocal albo „anonim”, gdy brak. Zwróć odpowiedzi w kolejności żądań — bez wycieku między nimi.
     * Podpowiedź: ThreadLocal.withInitial(() -> "anonim") albo sprawdzenie null; remove() w finally.
     */
    static List<String> exercise2(List<String> requests) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ stanowy serwis na bezstanowy i policz ceny po rabatach w puli 4 wątków
     * (wyniki w kolejności rabatów — przez listę Future). Stary kod (wyścig, gdy dzielony przez wątki):
     * <pre>{@code
     * class PriceService {
     *     private int discountPercent;                     // dane żądania w polu!
     *     int price(int base, int discount) {
     *         discountPercent = discount;
     *         return base * (100 - discountPercent) / 100;
     *     }
     * }
     * }</pre>
     * Podpowiedź: metoda tylko na parametrach i zmiennych lokalnych; pool.submit(() -> ...) dla każdego rabatu.
     */
    static List<Integer> exercise3(int basePrice, List<Integer> discounts) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): bezpieczna wątkowo pamięć podręczna (cache). {@code threads} wątków prosi
     * o wartości dla kluczy 1..keys (każdy wątek o wszystkie). „Droga” funkcja (np. k * k) ma się wykonać
     * DOKŁADNIE raz na klucz. Zwróć liczbę wywołań drogiej funkcji.
     * Podpowiedź: ConcurrentHashMap.computeIfAbsent jest atomowe; licznik wywołań jako AtomicInteger
     * zwiększany wewnątrz funkcji. Wersja „if (!cache.containsKey(k)) cache.put(k, compute(k))” policzy za dużo.
     */
    static int exercise4(int threads, int keys) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static OrderSnapshot solution1(String id, List<String> items) {
        return new OrderSnapshot(id, List.copyOf(items));
    }

    static List<String> solution2(List<String> requests) {
        ThreadLocal<String> user = ThreadLocal.withInitial(() -> "anonim");
        ExecutorService pool = namedPool("cw2", 1);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (String name : requests) {
                futures.add(pool.submit(() -> {
                    try {
                        if (name != null) {
                            user.set(name);
                        }
                        return user.get();
                    } finally {
                        user.remove();
                    }
                }));
            }
            return getAll(futures);
        } finally {
            shutdownAndAwait(pool);
        }
    }

    static int discountedPrice(int base, int discount) {   // bezstanowa: tylko parametry
        return base * (100 - discount) / 100;
    }

    static List<Integer> solution3(int basePrice, List<Integer> discounts) {
        ExecutorService pool = namedPool("cw3", 4);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int d : discounts) {
                futures.add(pool.submit(() -> discountedPrice(basePrice, d)));
            }
            return getAll(futures);
        } finally {
            shutdownAndAwait(pool);
        }
    }

    static int solution4(int threads, int keys) {
        ConcurrentHashMap<Integer, Integer> cache = new ConcurrentHashMap<>();
        AtomicInteger calls = new AtomicInteger();
        Runnable[] workers = new Runnable[threads];
        for (int t = 0; t < threads; t++) {
            workers[t] = () -> {
                for (int k = 1; k <= keys; k++) {
                    cache.computeIfAbsent(k, key -> {
                        calls.incrementAndGet();
                        return key * key;
                    });
                }
            };
        }
        runAll("cw4", workers);
        return calls.get();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Niezmienność (nic się nie zmienia), zamknięcie w wątku (nikt inny nie widzi danych: zmienne lokalne,
     *      ThreadLocal) i bezstanowość (obiekt nie ma zmiennego stanu — dane płyną parametrami).
     *   2. [a, b] — record nie kopiuje listy; pole wskazuje na tę samą ArrayList, którą potem zmieniono.
     *   3. Pole current w singletonie jest współdzielone przez wszystkie żądania; równoległe handle() nadpiszą je
     *      sobie nawzajem i zapiszą cudze zamówienie. Poprawnie: przekazywać o jako parametr (validate(o), save(o)).
     *   4. Brak remove() w finally — wątek puli zachowa nazwę użytkownika i następne zadanie ją zobaczy
     *      (wyciek danych i pamięci). Poprawnie: USER.set(name); try { process(); } finally { USER.remove(); }.
     *   5. Klasa Holder jest inicjalizowana dopiero przy pierwszym użyciu (leniwie), a JVM wykonuje inicjalizację
     *      klasy dokładnie raz pod własną blokadą i gwarantuje widoczność jej efektów (JLS 12.4.2).
     *   6. Nie. Każde pole jest atomowe osobno, ale zmiana „min i max razem” to dwie operacje — inny wątek może
     *      zobaczyć (albo wytworzyć) stan min > max. Trzeba jednej blokady na oba pola albo jednego niezmiennego
     *      obiektu Range w AtomicReference (zamiana compareAndSet).
     *   7. Pola final: po zakończeniu konstruktora (bez ucieczki this) każdy wątek, który zobaczy referencję,
     *      zobaczy ich poprawne wartości — nawet przy publikacji przez wyścig. Zwykłe pola: bez happens-before
     *      inny wątek może zobaczyć wartości domyślne (0, null) albo nieaktualne.
     */
    // </editor-fold>
}
