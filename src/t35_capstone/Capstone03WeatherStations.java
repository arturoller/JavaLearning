package t35_capstone;

import helpers.Check;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Projekt 3 — stacje pogodowe przetwarzane współbieżnie
 *        (weather station = stacja pogodowa; measurement = pomiar; concurrent = współbieżny)
 *
 * W SKRÓCIE:
 *   Kilka symulowanych stacji wysyła pomiary do wspólnej kolejki (producent–konsument), pula wątków je
 *   przetwarza, statystyki zbieramy bezpiecznie dla wątków, a alarmy rozsyła obserwator. Potem liczymy to
 *   samo strumieniem równoległym i przez CompletableFuture — i udowadniamy, że wynik jest zawsze ten sam.
 *
 * ANALOGIA:
 *   Sortownia listów. Listonosze (producenci) wrzucają listy do jednego kosza (kolejka), kilku pracowników
 *   (konsumenci) wyjmuje je i zapisuje w zeszycie zbiorczym (statystyki). Gdy listonosze skończą, kierownik
 *   wrzuca do kosza po jednej kartce „KONIEC” dla każdego pracownika (pigułka z trucizną) — kto ją wyjmie,
 *   idzie do domu. Kto wyjmie który list — nie wiadomo, ale suma w zeszycie musi się zgadzać.
 *
 * JAK TO DZIAŁA:
 *   stacja WAW ─┐                       ┌─► konsument 1 ─┐
 *   stacja KRK ─┼─► BlockingQueue ──────┼─► konsument 2 ─┼─► ConcurrentHashMap.merge(stacja, Stats)
 *   stacja ZAK ─┘   (+ PoisonPill × N)  └─► konsument N ─┘        └─► AlertDetector ─► słuchacze (observer)
 *   Wykorzystane działy kursu:
 *   • wątki, pule, Future                          → t21_concurrency/Concurrency04Executors
 *   • BlockingQueue, ConcurrentHashMap, LongAdder  → t21_concurrency/Concurrency06ConcurrentCollections
 *   • niezmienność jako bezpieczeństwo wątków      → t21_concurrency/Concurrency08ThreadSafetyPatterns
 *   • CompletableFuture                            → t21_concurrency/Concurrency05CompletableFuture
 *   • strumienie równoległe                        → t16_streams/Streams18Parallel
 *   • obserwator                                   → t22_design_patterns/Patterns06Observer
 *   • Random z ziarnem, liczby całkowite zamiast double → t01_basics/Basics08FloatingPoint
 *   • typy zamknięte i enum implementujący interfejs   → t07_inheritance_polymorphism/Inherit07SealedClasses
 *
 * SŁÓWKA:
 *   producer = producent; consumer = konsument; queue = kolejka; poison pill = pigułka z trucizną (znacznik
 *   końca); worker = pracownik (wątek roboczy); merge = scal; alert = alarm; listener = słuchacz;
 *   threshold = próg; seed = ziarno; tenths = dziesiąte części; baseline = wynik odniesienia.
 *
 * ZOBACZ TEŻ: t21_concurrency/Concurrency02RaceConditions (co się psuje bez synchronizacji),
 *             t21_concurrency/Concurrency10MemoryModel (widoczność zmian między wątkami),
 *             t16_streams/Streams17SideEffectsPitfalls (efekty uboczne w strumieniach)
 * </pre>
 */
public class Capstone03WeatherStations {

    static final Locale PL = Locale.forLanguageTag("pl-PL");                       // forLanguageTag = z tagu języka
    static final int PER_STATION = 60;                                              // per station = na stację

    public static void main(String[] args) throws InterruptedException {
        title("Capstone03 — stacje pogodowe");

        requirements();          // requirements = wymagania
        domainModel();           // domain model = model dziedziny
        simulatedStations();     // simulated stations = symulowane stacje
        sequentialBaseline();    // sequential baseline = wynik odniesienia liczony po kolei
        producerConsumer();      // producer–consumer = producent–konsument
        threadSafeStatistics();  // thread-safe statistics = statystyki bezpieczne dla wątków
        alertsWithObserver();    // alerts with observer = alarmy przez obserwatora
        parallelAlternatives();  // parallel alternatives = równoległe alternatywy
        finalReport();           // final report = raport końcowy
        behaviourTests();        // behaviour tests = testy zachowania
        whatNext();              // what next = co dalej
        exercises();             // exercises = ćwiczenia
    }

    // =================================================================================================
    // MODEL DZIEDZINY — same niezmienne wartości (można je bez obaw przekazywać między wątkami)
    // =================================================================================================

    /** Stacja: id, miasto, ziarno generatora i temperatura bazowa w dziesiątych częściach stopnia. */
    record Station(String id, String city, long seed, int baseTenths) {
        Station {
            Objects.requireNonNull(id, "id");                                       // requireNonNull = wymagaj nie-null
            Objects.requireNonNull(city, "miasto");
        }
    }

    /** Pomiar. Temperatura jako int w dziesiątych częściach stopnia: 215 = 21,5 °C. */
    record Measurement(String stationId, int sequence, int temperatureTenths, int humidity) {
        Measurement {
            Objects.requireNonNull(stationId, "id stacji");
            if (temperatureTenths < -600 || temperatureTenths > 600) {
                throw new IllegalArgumentException("temperatura poza zakresem czujnika: " + temperatureTenths);
            }
            if (humidity < 0 || humidity > 100) {
                throw new IllegalArgumentException("wilgotność poza 0–100: " + humidity);
            }
        }
    }

    /**
     * Statystyki stacji jako NIEZMIENNY rekord. combine (połącz) jest łączne i przemienne, więc kolejność
     * łączenia — czyli to, który wątek był pierwszy — nie wpływa na wynik.
     */
    record Stats(long count, long sumTenths, int minTenths, int maxTenths) {
        static final Stats EMPTY = new Stats(0, 0, Integer.MAX_VALUE, Integer.MIN_VALUE);

        static Stats of(Measurement m) {
            int t = m.temperatureTenths();
            return new Stats(1, t, t, t);
        }

        Stats combine(Stats other) {
            return new Stats(count + other.count, sumTenths + other.sumTenths,
                    Math.min(minTenths, other.minTenths), Math.max(maxTenths, other.maxTenths));
        }

        double average() {
            return count == 0 ? 0.0 : sumTenths / 10.0 / count;
        }
    }

    enum AlertKind { UPAŁ, MRÓZ }

    record Alert(String stationId, int sequence, AlertKind kind, int temperatureTenths) {
        @Override
        public String toString() {
            return stationId + " #" + sequence + " " + kind + " " + celsius(temperatureTenths);
        }
    }

    /** Progi alarmów (granica wyłączona: równo 31,0 °C to jeszcze nie upał). */
    record Thresholds(int hotAboveTenths, int frostBelowTenths) {
        static final Thresholds DEFAULT = new Thresholds(310, -110);
    }

    /** Element kolejki: odczyt albo znacznik końca. Typ zamknięty — trzeciej możliwości nie ma. */
    sealed interface QueueItem permits Reading, PoisonPill { }
    record Reading(Measurement measurement) implements QueueItem { }
    enum PoisonPill implements QueueItem { INSTANCE }                               // enum = jedyny egzemplarz

    static String celsius(int tenths) {
        return String.format(PL, "%.1f°C", tenths / 10.0);
    }

    static List<Station> stations() {
        return List.of(
                new Station("WAW", "Warszawa", 11L, 150),
                new Station("KRK", "Kraków", 22L, 220),
                new Station("GDN", "Gdańsk", 33L, 120),
                new Station("ZAK", "Zakopane", 44L, -20));
    }

    // =================================================================================================
    // LOGIKA — czyste funkcje i obiekty bez stanu globalnego
    // =================================================================================================

    /** Generator: własny Random z ziarnem stacji → te same dane przy każdym uruchomieniu i w każdym wątku. */
    static List<Measurement> generate(Station station, int count) {
        Random random = new Random(station.seed());
        List<Measurement> result = new ArrayList<>(count);
        for (int seq = 1; seq <= count; seq++) {
            int temperature = station.baseTenths() + random.nextInt(201) - 100;     // nextInt(201) = 0..200 → ±10 °C
            int humidity = 30 + random.nextInt(71);
            result.add(new Measurement(station.id(), seq, temperature, humidity));
        }
        return List.copyOf(result);                                                 // copyOf = niezmienna kopia (Java 10+)
    }

    static Stats statsOf(List<Measurement> measurements) {
        Stats stats = Stats.EMPTY;
        for (Measurement m : measurements) {
            stats = stats.combine(Stats.of(m));
        }
        return stats;
    }

    /** Wynik odniesienia: zwykła pętla w jednym wątku. Każda wersja współbieżna musi dać to samo. */
    static Map<String, Stats> sequential(List<Station> stations, int perStation) {
        Map<String, Stats> result = new TreeMap<>();
        for (Station station : stations) {
            result.put(station.id(), statsOf(generate(station, perStation)));
        }
        return result;
    }

    /** Obserwator: słuchacz dostaje alarm. Wołany z WIELU wątków — implementacja musi to znieść. */
    @FunctionalInterface
    interface AlertListener {
        void onAlert(Alert alert);                                                  // onAlert = przy alarmie
    }

    /** AlertDetector = wykrywacz alarmów (podmiot obserwatora). Lista słuchaczy niezmienna od konstrukcji. */
    static final class AlertDetector {
        private final Thresholds thresholds;
        private final List<AlertListener> listeners;

        AlertDetector(Thresholds thresholds, List<AlertListener> listeners) {
            this.thresholds = thresholds;
            this.listeners = List.copyOf(listeners);
        }

        void check(Measurement m) {
            int t = m.temperatureTenths();
            if (t > thresholds.hotAboveTenths()) {
                publish(new Alert(m.stationId(), m.sequence(), AlertKind.UPAŁ, t));
            } else if (t < thresholds.frostBelowTenths()) {
                publish(new Alert(m.stationId(), m.sequence(), AlertKind.MRÓZ, t));
            }
        }

        private void publish(Alert alert) {
            listeners.forEach(listener -> listener.onAlert(alert));
        }
    }

    /** Słuchacz zbierający alarmy: kolejka bezpieczna dla wątków, a na zewnątrz posortowana kopia. */
    static final class CollectingListener implements AlertListener {
        private final ConcurrentLinkedQueue<Alert> alerts = new ConcurrentLinkedQueue<>();

        @Override
        public void onAlert(Alert alert) {
            alerts.add(alert);
        }

        List<Alert> sorted() {
            return alerts.stream()
                    .sorted(Comparator.comparing(Alert::stationId).thenComparingInt(Alert::sequence))
                    .toList();                                                      // toList (Java 16+)
        }
    }

    record PipelineResult(Map<String, Stats> stats, long processed) { }

    /** Potok producent–konsument. Każde wywołanie run tworzy WŁASNĄ kolejkę, mapę i pulę — brak stanu współdzielonego. */
    static final class WeatherPipeline {
        private final int consumers;
        private final AlertDetector detector;

        WeatherPipeline(int consumers, AlertDetector detector) {
            if (consumers < 1) {
                throw new IllegalArgumentException("potrzebny co najmniej 1 konsument");
            }
            this.consumers = consumers;
            this.detector = detector;
        }

        PipelineResult run(List<Station> stations, int perStation) throws InterruptedException {
            BlockingQueue<QueueItem> queue = new ArrayBlockingQueue<>(16);          // mała pojemność = naturalne hamowanie
            ConcurrentHashMap<String, Stats> stats = new ConcurrentHashMap<>();
            LongAdder processed = new LongAdder();                                   // LongAdder = szybki licznik wielowątkowy
            ExecutorService pool = Executors.newFixedThreadPool(stations.size() + consumers);
            try {
                List<Future<Void>> producers = new ArrayList<>();
                for (Station station : stations) {
                    producers.add(pool.submit(() -> produce(station, perStation, queue)));
                }
                List<Future<Void>> workers = new ArrayList<>();
                for (int i = 0; i < consumers; i++) {
                    workers.add(pool.submit(() -> consume(queue, stats, processed)));
                }
                awaitAll(producers);                                                 // 1) wszyscy producenci skończyli
                for (int i = 0; i < consumers; i++) {
                    queue.put(PoisonPill.INSTANCE);                                  // 2) jedna pigułka na konsumenta
                }
                awaitAll(workers);                                                   // 3) konsumenci zjedli pigułki
                return new PipelineResult(new TreeMap<>(stats), processed.sum());
            } finally {
                pool.shutdownNow();                                                  // shutdownNow = zatrzymaj (przerwij wątki)
                pool.awaitTermination(5, TimeUnit.SECONDS);
            }
        }

        private static Void produce(Station station, int count, BlockingQueue<QueueItem> queue)
                throws InterruptedException {
            for (Measurement m : generate(station, count)) {
                queue.put(new Reading(m));                                           // put = włóż (czeka, gdy pełna)
            }
            return null;
        }

        private Void consume(BlockingQueue<QueueItem> queue, ConcurrentHashMap<String, Stats> stats,
                             LongAdder processed) throws InterruptedException {
            while (true) {
                QueueItem item = queue.take();                                       // take = wyjmij (czeka, gdy pusta)
                if (item instanceof Reading r) {                                     // instanceof z wzorcem (Java 16+)
                    Measurement m = r.measurement();
                    stats.merge(m.stationId(), Stats.of(m), Stats::combine);         // merge = atomowo dla klucza
                    processed.increment();
                    detector.check(m);
                } else {
                    return null;                                                     // PoisonPill → koniec pracy
                }
            }
        }

        private static void awaitAll(List<Future<Void>> futures) throws InterruptedException {
            for (Future<Void> future : futures) {
                try {
                    future.get();                                                    // get = czekaj na wynik / wyjątek
                } catch (ExecutionException e) {
                    throw new IllegalStateException("zadanie w puli zakończyło się błędem", e.getCause());
                }
            }
        }
    }

    static WeatherPipeline pipeline(int consumers, AlertListener... listeners) {
        return new WeatherPipeline(consumers, new AlertDetector(Thresholds.DEFAULT, List.of(listeners)));
    }

    // =================================================================================================
    // 1. WYMAGANIA I PRZYKŁADY
    // =================================================================================================

    /**
     * 1. Wymagania: 4 stacje po 60 pomiarów, statystyki (liczba, min, max, średnia) dla każdej stacji,
     * alarm „UPAŁ” powyżej 31,0 °C i „MRÓZ” poniżej -11,0 °C, raport posortowany po id stacji.
     * Najważniejsze wymaganie niefunkcjonalne: wynik identyczny przy każdym uruchomieniu.
     */
    static void requirements() {
        section("1. Wymagania i przykłady");

        stations().forEach(s -> show(s.id(), s.city() + ", baza " + celsius(s.baseTenths())));
        // WYNIK: WAW → Warszawa, baza 15,0°C
        // WYNIK: KRK → Kraków, baza 22,0°C
        // WYNIK: GDN → Gdańsk, baza 12,0°C
        // WYNIK: ZAK → Zakopane, baza -2,0°C
        show("progi", "UPAŁ > " + celsius(Thresholds.DEFAULT.hotAboveTenths())
                + ", MRÓZ < " + celsius(Thresholds.DEFAULT.frostBelowTenths()));
        // WYNIK: progi → UPAŁ > 31,0°C, MRÓZ < -11,0°C

        // DOBRA PRAKTYKA: w programie współbieżnym wypisuj tylko to, co od kolejności NIE zależy: sumy, min, max,
        //   liczby. Dlaczego? „Który wątek był pierwszy” zmienia się z uruchomienia na uruchomienie (i z komputera
        //   na komputer), więc taki wydruk nie nadaje się ani do testów, ani do porównań.
    }

    // =================================================================================================
    // 2. MODEL DZIEDZINY
    // =================================================================================================

    /**
     * 2. Same rekordy — niezmienne, więc bezpieczne do przekazywania między wątkami bez synchronizacji.
     * Temperatura w dziesiątych częściach stopnia jako int: sumy są dokładne i nie zależą od kolejności.
     */
    static void domainModel() {
        section("2. Model dziedziny");

        Measurement m = new Measurement("WAW", 1, 215, 60);
        show("pomiar", m);
        // WYNIK: pomiar → Measurement[stationId=WAW, sequence=1, temperatureTenths=215, humidity=60]
        show("jako tekst", celsius(m.temperatureTenths()));
        // WYNIK: jako tekst → 21,5°C
        expectThrows("wilgotność 120", () -> new Measurement("WAW", 2, 100, 120));
        // WYNIK: ✔ wilgotność 120 → rzucono IllegalArgumentException: wilgotność poza 0–100: 120

        Stats a = Stats.of(new Measurement("WAW", 1, 100, 50)).combine(Stats.of(new Measurement("WAW", 2, 300, 50)));
        Stats b = Stats.of(new Measurement("WAW", 2, 300, 50)).combine(Stats.of(new Measurement("WAW", 1, 100, 50)));
        show("a.combine(b) w obu kolejnościach równe?", a.equals(b));
        // WYNIK: a.combine(b) w obu kolejnościach równe? → true
        show("średnia", a.average());
        // WYNIK: średnia → 20.0

        // PUŁAPKA: suma liczb double ZALEŻY od kolejności dodawania (błędy zaokrągleń). Gdyby wątki dodawały
        //   temperatury jako double, ostatnie cyfry wyniku zmieniałyby się między uruchomieniami.
        show("(0.1 + 0.2) + 0.3", (0.1 + 0.2) + 0.3);
        // WYNIK: (0.1 + 0.2) + 0.3 → 0.6000000000000001
        show("0.1 + (0.2 + 0.3)", 0.1 + (0.2 + 0.3));
        // WYNIK: 0.1 + (0.2 + 0.3) → 0.6
    }

    // =================================================================================================
    // 3. SYMULOWANE STACJE
    // =================================================================================================

    /**
     * 3. Każda stacja ma własny {@code Random} z własnym ziarnem. Dzięki temu dane stacji nie zależą od tego,
     * który wątek i kiedy je wygeneruje — w przeciwieństwie do jednego wspólnego Random dla wszystkich.
     */
    static void simulatedStations() {
        section("3. Symulowane stacje");

        Station waw = stations().get(0);
        List<Measurement> first = generate(waw, PER_STATION);
        first.stream().limit(3).forEach(m -> show("WAW #" + m.sequence(), celsius(m.temperatureTenths())
                + ", wilgotność " + m.humidity() + "%"));
        // WYNIK: WAW #1 → 23,0°C, wilgotność 47%
        // WYNIK: WAW #2 → 18,5°C, wilgotność 78%
        // WYNIK: WAW #3 → 11,3°C, wilgotność 51%
        show("drugie generowanie identyczne?", first.equals(generate(waw, PER_STATION)));
        // WYNIK: drugie generowanie identyczne? → true

        // PUŁAPKA: jeden wspólny Random dla wszystkich producentów. Jest bezpieczny dla wątków, ale KOLEJNOŚĆ
        //   pobierania liczb zależy od harmonogramu wątków, więc stacja dostawałaby inne dane przy każdym starcie.

        // DOBRA PRAKTYKA: symulacja bez Thread.sleep. Dlaczego? Uśpienia spowalniają testy i nic nie dowodzą —
        //   współbieżność sprawdzamy przez porównanie wyników, a nie przez „udawanie” czasu.
    }

    // =================================================================================================
    // 4. WYNIK ODNIESIENIA (po kolei, jeden wątek)
    // =================================================================================================

    /**
     * 4. Zanim cokolwiek zrównoleglisz, policz to najprościej. Ten wynik staje się „wzorcem prawdy”
     * dla wszystkich wersji współbieżnych.
     */
    static void sequentialBaseline() {
        section("4. Wynik odniesienia (po kolei)");

        Map<String, Stats> baseline = sequential(stations(), PER_STATION);
        showEach("statystyki", baseline);
        // WYNIK: statystyki (liczba kluczy: 4):
        // WYNIK: • GDN → Stats[count=60, sumTenths=7073, minTenths=21, maxTenths=218]
        // WYNIK: • KRK → Stats[count=60, sumTenths=13810, minTenths=122, maxTenths=319]
        // WYNIK: • WAW → Stats[count=60, sumTenths=9088, minTenths=50, maxTenths=248]
        // WYNIK: • ZAK → Stats[count=60, sumTenths=-1833, minTenths=-120, maxTenths=80]

        // DOBRA PRAKTYKA: TreeMap jako wynik — klucze posortowane, wydruk zawsze w tej samej kolejności.
    }

    // =================================================================================================
    // 5. PRODUCENT–KONSUMENT (BlockingQueue, pigułka z trucizną, ExecutorService)
    // =================================================================================================

    /**
     * 5. Producenci wkładają odczyty do {@code ArrayBlockingQueue}; konsumenci je wyjmują. Kolejność kroków
     * w {@code run}: czekamy na producentów → wkładamy po jednej pigułce na konsumenta → czekamy na konsumentów.
     */
    static void producerConsumer() throws InterruptedException {
        section("5. Producent–konsument");

        Map<String, Stats> baseline = sequential(stations(), PER_STATION);
        PipelineResult result = pipeline(4).run(stations(), PER_STATION);
        show("przetworzonych pomiarów", result.processed());
        // WYNIK: przetworzonych pomiarów → 240
        show("zgodne z wynikiem odniesienia?", result.stats().equals(baseline));
        // WYNIK: zgodne z wynikiem odniesienia? → true

        for (int consumers : new int[] {1, 2, 8}) {
            boolean same = pipeline(consumers).run(stations(), PER_STATION).stats().equals(baseline);
            show("konsumentów: " + consumers + " → zgodne?", same);
        }
        // WYNIK: konsumentów: 1 → zgodne? → true
        // WYNIK: konsumentów: 2 → zgodne? → true
        // WYNIK: konsumentów: 8 → zgodne? → true

        // PUŁAPKA: jedna pigułka dla wielu konsumentów. Pierwszy ją zjada i kończy, reszta czeka na take()
        //   w nieskończoność — program „wisi”. Zasada: tyle pigułek, ilu konsumentów.

        // PUŁAPKA: pigułki włożone, zanim skończą producenci. Konsument mógłby zakończyć pracę, a późniejsze
        //   odczyty zostałyby w kolejce na zawsze. Dlatego najpierw awaitAll(producers), potem pigułki.

        // DOBRA PRAKTYKA: future.get() na KAŻDYM zadaniu. Dlaczego? Wyjątek w zadaniu z submit() nie jest nigdzie
        //   wypisywany — bez get() zginąłby po cichu, a wynik byłby po prostu niepełny.
    }

    // =================================================================================================
    // 6. STATYSTYKI BEZPIECZNE DLA WĄTKÓW
    // =================================================================================================

    /**
     * 6. {@code ConcurrentHashMap.merge} wykonuje „odczytaj – połącz – zapisz” atomowo dla danego klucza.
     * W połączeniu z niezmiennym Stats nie potrzebujemy żadnego {@code synchronized}.
     */
    static void threadSafeStatistics() throws InterruptedException {
        section("6. Statystyki bezpieczne dla wątków");

        ConcurrentHashMap<String, Stats> stats = new ConcurrentHashMap<>();
        LongAdder counter = new LongAdder();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<CompletableFuture<Void>> tasks = new ArrayList<>();
            for (Station station : stations()) {
                tasks.add(CompletableFuture.runAsync(() -> generate(station, PER_STATION).forEach(m -> {
                    stats.merge("RAZEM", Stats.of(m), Stats::combine);              // wszyscy piszą do JEDNEGO klucza
                    counter.increment();
                }), pool));                                                          // runAsync = uruchom asynchronicznie
            }
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture<?>[0])).join(); // allOf = wszystkie naraz
        } finally {
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
        show("RAZEM", stats.get("RAZEM"));
        // WYNIK: RAZEM → Stats[count=240, sumTenths=28138, minTenths=-120, maxTenths=319]
        show("licznik LongAdder", counter.sum());
        // WYNIK: licznik LongAdder → 240

        // PUŁAPKA: „sprawdź, potem wstaw” na zwykłej mapie:
        //     Stats old = map.get(key);  map.put(key, old == null ? s : old.combine(s));
        //   Dwa wątki mogą odczytać to samo „old” i jeden zapis przepada (zgubiona aktualizacja). Nawet na
        //   ConcurrentHashMap te DWA wywołania razem nie są atomowe — atomowe jest dopiero merge/compute.

        // DOBRA PRAKTYKA: LongAdder zamiast AtomicLong dla liczników, które wiele wątków zwiększa, a rzadko czyta.
        //   Dlaczego? Rozkłada zapisy na kilka komórek, więc wątki mniej na siebie czekają; sum() je dodaje.
    }

    // =================================================================================================
    // 7. ALARMY PRZEZ OBSERWATORA
    // =================================================================================================

    /**
     * 7. Detektor nie wie, kto słucha: zbieracz alarmów, licznik, w przyszłości SMS. Słuchacze są wołani
     * z wątków konsumentów w dowolnej kolejności, więc przed wydrukiem SORTUJEMY alarmy.
     */
    static void alertsWithObserver() throws InterruptedException {
        section("7. Alarmy przez obserwatora");

        CollectingListener collector = new CollectingListener();
        ConcurrentHashMap<AlertKind, LongAdder> perKind = new ConcurrentHashMap<>();
        AlertListener counting = alert -> perKind.computeIfAbsent(alert.kind(), k -> new LongAdder()).increment();

        pipeline(3, collector, counting).run(stations(), PER_STATION);

        showEach("alarmy (posortowane)", collector.sorted());
        // WYNIK: alarmy (posortowane) (liczba elementów: 7):
        // WYNIK: • KRK #2 UPAŁ 31,6°C
        // WYNIK: • KRK #4 UPAŁ 31,9°C
        // WYNIK: • KRK #36 UPAŁ 31,5°C
        // WYNIK: • KRK #44 UPAŁ 31,7°C
        // WYNIK: • ZAK #14 MRÓZ -11,6°C
        // WYNIK: • ZAK #27 MRÓZ -12,0°C
        // WYNIK: • ZAK #43 MRÓZ -11,4°C
        Map<AlertKind, Long> counts = new EnumMap<>(AlertKind.class);
        perKind.forEach((kind, adder) -> counts.put(kind, adder.sum()));
        show("liczba wg rodzaju", counts);
        // WYNIK: liczba wg rodzaju → {UPAŁ=4, MRÓZ=3}

        // PUŁAPKA: słuchacz z ArrayList.add. Wołany równocześnie z kilku wątków może zgubić elementy albo rzucić
        //   ArrayIndexOutOfBoundsException. Słuchacz w programie współbieżnym musi być bezpieczny dla wątków.

        // PUŁAPKA: wypisywanie alarmu prosto w onAlert (println). Kolejność linii zależałaby od harmonogramu
        //   wątków. Zbieramy, sortujemy, wypisujemy — a ConcurrentHashMap z kluczem-enumem kopiujemy do EnumMap.
    }

    // =================================================================================================
    // 8. RÓWNOLEGŁE ALTERNATYWY: strumień równoległy i CompletableFuture
    // =================================================================================================

    /** Strumień równoległy: dzieli dane na kawałki, liczy osobno i łączy wyniki przez combine. */
    static Map<String, Stats> withParallelStream(List<Station> stations, int perStation) {
        return stations.stream()
                .flatMap(s -> generate(s, perStation).stream())
                .toList()
                .parallelStream()                                                   // parallelStream = strumień równoległy
                .collect(Collectors.groupingBy(Measurement::stationId, TreeMap::new,
                        Collectors.reducing(Stats.EMPTY, Stats::of, Stats::combine)));
    }

    /** CompletableFuture: jedno zadanie na stację na NASZEJ puli, wyniki zebrane po allOf. */
    static Map<String, Stats> withCompletableFutures(List<Station> stations, int perStation)
            throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(stations.size());
        try {
            Map<String, CompletableFuture<Stats>> futures = new TreeMap<>();
            for (Station station : stations) {
                futures.put(station.id(),
                        CompletableFuture.supplyAsync(() -> statsOf(generate(station, perStation)), pool)); // supplyAsync = dostarcz asynchronicznie
            }
            CompletableFuture.allOf(futures.values().toArray(new CompletableFuture<?>[0])).join();
            Map<String, Stats> result = new TreeMap<>();
            futures.forEach((id, future) -> result.put(id, future.join()));          // join = weź wynik (już gotowy)
            return result;
        } finally {
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * 8. Trzy sposoby na to samo. Kolejka daje kontrolę nad przepływem (strumień danych „na żywo”),
     * strumień równoległy jest najkrótszy dla gotowej kolekcji, CompletableFuture pasuje do niezależnych zadań.
     */
    static void parallelAlternatives() throws InterruptedException {
        section("8. Równoległe alternatywy");

        Map<String, Stats> baseline = sequential(stations(), PER_STATION);
        show("strumień równoległy = odniesienie?", withParallelStream(stations(), PER_STATION).equals(baseline));
        // WYNIK: strumień równoległy = odniesienie? → true
        show("CompletableFuture = odniesienie?", withCompletableFutures(stations(), PER_STATION).equals(baseline));
        // WYNIK: CompletableFuture = odniesienie? → true

        // PUŁAPKA: forEach na strumieniu równoległym z dodawaniem do zwykłej mapy lub listy (efekt uboczny).
        //   Wynik byłby niepełny albo program rzuciłby wyjątek. Używaj collect — on sam łączy częściowe wyniki.

        // DOBRA PRAKTYKA: CompletableFuture z WŁASNĄ pulą (drugi argument supplyAsync). Dlaczego? Bez niej zadania
        //   trafiają do wspólnej puli ForkJoinPool.commonPool(), z której korzystają też strumienie równoległe
        //   w całej aplikacji — jedno wolne zadanie spowalnia wszystkich.
    }

    // =================================================================================================
    // 9. RAPORT KOŃCOWY
    // =================================================================================================

    static List<String> renderReport(Map<String, Stats> stats, List<Station> stations) {
        Map<String, String> cities = stations.stream().collect(Collectors.toMap(Station::id, Station::city));
        List<String> lines = new ArrayList<>();
        lines.add(String.format(PL, "%-4s %-9s %4s %8s %8s %8s", "ID", "MIASTO", "N", "MIN", "MAKS", "ŚREDNIA"));
        stats.forEach((id, s) -> lines.add(String.format(PL, "%-4s %-9s %4d %8s %8s %7.1f°C",
                id, cities.get(id), s.count(), celsius(s.minTenths()), celsius(s.maxTenths()), s.average())));
        return lines;
    }

    /**
     * 9. Raport po polsku (przecinek dziesiętny), posortowany po id stacji. Liczymy go z wyniku potoku —
     * a ten jest identyczny przy każdym uruchomieniu, więc i raport jest powtarzalny.
     */
    static void finalReport() throws InterruptedException {
        section("9. Raport końcowy");

        PipelineResult result = pipeline(4).run(stations(), PER_STATION);
        renderReport(result.stats(), stations()).forEach(System.out::println);
        // WYNIK: ID   MIASTO       N      MIN     MAKS  ŚREDNIA
        // WYNIK: GDN  Gdańsk      60    2,1°C   21,8°C    11,8°C
        // WYNIK: KRK  Kraków      60   12,2°C   31,9°C    23,0°C
        // WYNIK: WAW  Warszawa    60    5,0°C   24,8°C    15,1°C
        // WYNIK: ZAK  Zakopane    60  -12,0°C    8,0°C    -3,1°C

        // DOBRA PRAKTYKA: raport to czysta funkcja (Map → List<String>). Dlaczego? Testujesz go bez wątków,
        //   a ta sama funkcja obsłuży wynik z kolejki, strumienia równoległego albo z bazy.
    }

    // =================================================================================================
    // 10. TESTY ZACHOWANIA
    // =================================================================================================

    /** 10. Zachowania i przypadki brzegowe: granica progu, zero pomiarów, jeden konsument, element neutralny. */
    static void behaviourTests() throws InterruptedException {
        section("10. Testy zachowania");

        CollectingListener boundary = new CollectingListener();
        AlertDetector detector = new AlertDetector(Thresholds.DEFAULT, List.of(boundary));
        detector.check(new Measurement("X", 1, 310, 50));                           // równo 31,0 → bez alarmu
        detector.check(new Measurement("X", 2, -110, 50));                          // równo -11,0 → bez alarmu
        detector.check(new Measurement("X", 3, 311, 50));
        Check.equal("granice progów", List.of(new Alert("X", 3, AlertKind.UPAŁ, 311)), boundary::sorted);
        Check.equal("EMPTY jest elementem neutralnym", Stats.of(new Measurement("X", 1, 55, 1)),
                () -> Stats.EMPTY.combine(Stats.of(new Measurement("X", 1, 55, 1))));
        Check.equal("średnia bez pomiarów → 0.0", 0.0, Stats.EMPTY::average);
        Check.equal("0 pomiarów → pusty wynik, brak zawieszenia", Map.of(),
                pipeline(2).run(stations(), 0).stats());
        Check.equal("brak konsumentów → wyjątek", "potrzebny co najmniej 1 konsument", () -> {
            try {
                pipeline(0);
                return "brak wyjątku";
            } catch (IllegalArgumentException e) {
                return e.getMessage();
            }
        });
        Check.equal("1 konsument = odniesienie", sequential(stations(), PER_STATION),
                pipeline(1).run(stations(), PER_STATION).stats());
        Check.summary();
        // WYNIK: ✔ OK    granice progów
        // WYNIK: ✔ OK    EMPTY jest elementem neutralnym
        // WYNIK: ✔ OK    średnia bez pomiarów → 0.0
        // WYNIK: ✔ OK    0 pomiarów → pusty wynik, brak zawieszenia
        // WYNIK: ✔ OK    brak konsumentów → wyjątek
        // WYNIK: ✔ OK    1 konsument = odniesienie
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD

        // DOBRA PRAKTYKA: test „0 pomiarów” chroni przed zawieszeniem. Gdyby konsumenci kończyli się dopiero
        //   po pierwszym odczycie, przy pustych danych program czekałby na take() w nieskończoność.
    }

    // =================================================================================================
    // 11. CO DALEJ
    // =================================================================================================

    /** 11. Ten sam pomysł w większej skali i w Springu. */
    static void whatNext() {
        section("11. Co dalej");

        note("BlockingQueue → broker wiadomości (Kafka, RabbitMQ) i @KafkaListener w Springu");
        // WYNIK: ℹ BlockingQueue → broker wiadomości (Kafka, RabbitMQ) i @KafkaListener w Springu
        note("ExecutorService → ThreadPoolTaskExecutor + @Async; okresowe odczyty → @Scheduled");
        // WYNIK: ℹ ExecutorService → ThreadPoolTaskExecutor + @Async; okresowe odczyty → @Scheduled
        note("AlertListener → ApplicationEventPublisher + @EventListener");
        // WYNIK: ℹ AlertListener → ApplicationEventPublisher + @EventListener
        note("Stats → metryki Micrometer (licznik, wskaźnik) widoczne w Actuatorze");
        // WYNIK: ℹ Stats → metryki Micrometer (licznik, wskaźnik) widoczne w Actuatorze

        // Do powtórki: t21_concurrency/Concurrency04Executors, t21_concurrency/Concurrency06ConcurrentCollections,
        //   t21_concurrency/Concurrency05CompletableFuture, t22_design_patterns/Patterns06Observer.
        // W Javie 21+ wątki wirtualne (Executors.newVirtualThreadPerTaskExecutor()) uproszczą pulę producentów
        //   (Java 21+), ale zasady (kolejka, pigułki, merge) zostają te same.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Najpierw wersja sekwencyjna = wzorzec prawdy; każdą wersję współbieżną porównuj z nią przez equals.
     *   • Dane między wątkami: niezmienne rekordy. Liczby do sum: int/long (np. dziesiąte części), nie double.
     *   • Producent–konsument: BlockingQueue.put/take; koniec = PoisonPill, po jednej na konsumenta,
     *     wkładane dopiero po zakończeniu producentów.
     *   • Agregacja: ConcurrentHashMap.merge(klucz, wartość, combine) — atomowo; liczniki: LongAdder.
     *   • combine łączne i przemienne + element neutralny (EMPTY) → wynik nie zależy od kolejności.
     *   • Każdy Future: get()/join() — inaczej wyjątki giną po cichu. Pula: shutdown w finally.
     *   • Obserwator w wielu wątkach: słuchacze bezpieczni dla wątków; wydruk po sortowaniu.
     *   • Strumień równoległy: collect zamiast forEach z efektem ubocznym. CompletableFuture: własna pula.
     *   • Random z ziarnem NA STACJĘ, bez sleep; wypisuj tylko wyniki niezależne od harmonogramu.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego temperatury są przechowywane jako int w dziesiątych częściach stopnia, a nie jako double?
     *   2. Co wypisze:  System.out.println((0.1 + 0.2) + 0.3 == 0.1 + (0.2 + 0.3));  ?
     *   3. ZNAJDŹ BŁĄD (3 konsumentów):
     *        awaitAll(producers);
     *        queue.put(PoisonPill.INSTANCE);
     *        awaitAll(workers);
     *   4. ZNAJDŹ BŁĄD:  Stats old = stats.get(id);  stats.put(id, old == null ? s : old.combine(s));
     *      (stats to ConcurrentHashMap, kod wołany z wielu wątków)
     *   5. Co wypisze:  System.out.println(Stats.EMPTY.combine(Stats.EMPTY).count());  ?
     *      I dlaczego EMPTY ma min = Integer.MAX_VALUE?
     *   6. Dlaczego alarmy są zbierane i sortowane, a nie wypisywane w onAlert?
     *   7. Czym różni się supplyAsync(zadanie) od supplyAsync(zadanie, pula) i co wybrać w aplikacji serwerowej?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() throws InterruptedException {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        List<Measurement> all = stations().stream().flatMap(s -> generate(s, PER_STATION).stream()).toList();
        List<Measurement> jumps = List.of(new Measurement("T", 1, 100, 50), new Measurement("T", 2, 160, 50),
                new Measurement("T", 3, 150, 50), new Measurement("T", 4, 90, 50), new Measurement("T", 5, 140, 50));
        Check.equal("ćw. 1: pomiary > 20,0°C", expectedWarm(), () -> exercise1(all, 200));
        Check.equal("ćw. 2: nagłe skoki", List.of(2, 4), () -> exercise2(jumps, 50));
        Check.equal("ćw. 3: połączone statystyki", combinedStats(), () -> exercise3(all));
        Check.equal("ćw. 4: maksima przez CompletableFuture", expectedMax(),
                () -> runWithPool(pool -> exercise4(stations(), PER_STATION, pool)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", expectedWarm(), () -> solution1(all, 200));
        Check.equal("ćw. 2 (wzorzec)", List.of(2, 4), () -> solution2(jumps, 50));
        Check.equal("ćw. 3 (wzorzec)", combinedStats(), () -> solution3(all));
        Check.equal("ćw. 4 (wzorzec)", expectedMax(),
                () -> runWithPool(pool -> solution4(stations(), PER_STATION, pool)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    static Map<String, Long> expectedWarm() {
        return new TreeMap<>(Map.of("GDN", 4L, "KRK", 39L, "WAW", 17L));
    }

    static Stats combinedStats() {
        return new Stats(240, 28138, -120, 319);
    }

    static Map<String, Integer> expectedMax() {
        return new TreeMap<>(Map.of("GDN", 218, "KRK", 319, "WAW", 248, "ZAK", 80));
    }

    /** Uruchamia zadanie z pulą 4 wątków i ZAWSZE ją zamyka (także po wyjątku z zaślepki ćwiczenia). */
    static <T> T runWithPool(java.util.function.Function<ExecutorService, T> task) {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            return task.apply(pool);
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * ĆWICZENIE 1 (łatwe): nowa linia raportu — ile pomiarów każdej stacji przekroczyło próg (ściśle większe).
     * Stacje bez takich pomiarów pomiń. Mapa posortowana po id (TreeMap).
     * Podpowiedź: filter(m -> m.temperatureTenths() > próg), groupingBy(stationId, TreeMap::new, counting()).
     */
    static Map<String, Long> exercise1(List<Measurement> measurements, int thresholdTenths) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): nowa reguła alarmu — „nagły skok”. Dla pomiarów JEDNEJ stacji (posortowanych po
     * sequence) zwróć numery sequence tych pomiarów, które różnią się od POPRZEDNIEGO o więcej niż
     * {@code maxJumpTenths} (w dowolną stronę). Przykład: 100, 160, 150, 90, 140 i próg 50 → [2, 4].
     * Podpowiedź: pętla od i = 1, Math.abs(cur - prev) > maxJumpTenths.
     */
    static List<Integer> exercise2(List<Measurement> oneStation, int maxJumpTenths) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): jedne statystyki dla WSZYSTKICH pomiarów, strumieniem RÓWNOLEGŁYM bez efektów
     * ubocznych. Wynik musi być identyczny jak sekwencyjny.
     * Podpowiedź: parallelStream().map(Stats::of).reduce(Stats.EMPTY, Stats::combine) — dlaczego to bezpieczne?
     */
    static Stats exercise3(List<Measurement> measurements) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): maksymalna temperatura (w dziesiątych) każdej stacji, licząc każdą stację
     * w osobnym zadaniu CompletableFuture na podanej puli. Wynik: TreeMap id → maks. Pulę zamyka wywołujący.
     * Podpowiedź: supplyAsync(() -> generate(s, n).stream().mapToInt(Measurement::temperatureTenths).max()
     * .orElseThrow(), pool); potem allOf(...).join() i join() każdego zadania.
     */
    static Map<String, Integer> exercise4(List<Station> stations, int perStation, ExecutorService pool) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Map<String, Long> solution1(List<Measurement> measurements, int thresholdTenths) {
        return measurements.stream()
                .filter(m -> m.temperatureTenths() > thresholdTenths)
                .collect(Collectors.groupingBy(Measurement::stationId, TreeMap::new, Collectors.counting()));
    }

    static List<Integer> solution2(List<Measurement> oneStation, int maxJumpTenths) {
        List<Integer> result = new ArrayList<>();
        for (int i = 1; i < oneStation.size(); i++) {
            int previous = oneStation.get(i - 1).temperatureTenths();
            int current = oneStation.get(i).temperatureTenths();
            if (Math.abs(current - previous) > maxJumpTenths) {
                result.add(oneStation.get(i).sequence());
            }
        }
        return List.copyOf(result);
    }

    static Stats solution3(List<Measurement> measurements) {
        // Bezpieczne, bo Stats jest niezmienny, combine łączne, a EMPTY jest elementem neutralnym.
        return measurements.parallelStream().map(Stats::of).reduce(Stats.EMPTY, Stats::combine);
    }

    static Map<String, Integer> solution4(List<Station> stations, int perStation, ExecutorService pool) {
        Map<String, CompletableFuture<Integer>> futures = new TreeMap<>();
        for (Station s : stations) {
            futures.put(s.id(), CompletableFuture.supplyAsync(() -> generate(s, perStation).stream()
                    .mapToInt(Measurement::temperatureTenths).max().orElseThrow(), pool));
        }
        CompletableFuture.allOf(futures.values().toArray(new CompletableFuture<?>[0])).join();
        Map<String, Integer> result = new TreeMap<>();
        futures.forEach((id, f) -> result.put(id, f.join()));
        return result;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Suma liczb całkowitych jest dokładna i nie zależy od kolejności dodawania. Suma double ma błędy
     *      zaokrągleń zależne od kolejności, a kolejność w programie współbieżnym zmienia się między uruchomieniami
     *      — ostatnie cyfry średniej byłyby niepowtarzalne.
     *   2. false — lewa strona to 0.6000000000000001, prawa 0.6. Dodawanie double nie jest łączne.
     *   3. Jedna pigułka na trzech konsumentów: pierwszy kończy pracę, dwóch pozostałych czeka na take()
     *      w nieskończoność i awaitAll(workers) nigdy się nie kończy. Poprawka: pętla, która wkłada 3 pigułki.
     *   4. get i put to dwie osobne operacje. Dwa wątki mogą odczytać to samo „old” i jeden wynik nadpisze drugi
     *      (zgubiona aktualizacja). Poprawnie: stats.merge(id, s, Stats::combine) — atomowo dla klucza.
     *   5. 0 — liczby się dodają (0 + 0). MAX_VALUE jako min (i MIN_VALUE jako max) to element neutralny:
     *      Math.min(MAX_VALUE, x) = x, więc połączenie z EMPTY nie zmienia żadnej statystyki.
     *   6. onAlert jest wołany z wielu wątków w kolejności zależnej od harmonogramu; wydruk w środku dawałby inną
     *      kolejność linii przy każdym uruchomieniu. Sortowanie po (stacja, sequence) daje stały wynik.
     *   7. Bez puli zadanie trafia do ForkJoinPool.commonPool(), wspólnej dla całej aplikacji (także dla strumieni
     *      równoległych). W aplikacji serwerowej podawaj własną pulę o przemyślanej wielkości i ją zamykaj.
     */
    // </editor-fold>
}
