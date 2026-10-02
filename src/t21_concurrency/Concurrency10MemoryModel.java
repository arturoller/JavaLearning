package t21_concurrency;

import helpers.Check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Model pamięci Javy (JMM) — widoczność, zmiana kolejności, atomowość i relacja happens-before
 *        (memory model = model pamięci; visibility = widoczność; reordering = zmiana kolejności;
 *         happens-before = „dzieje się przed”)
 *
 * W SKRÓCIE:
 *   Wątek NIE ma gwarancji, że zobaczy zapis wykonany przez inny wątek — ani kiedy, ani w jakiej kolejności.
 *   Gwarancję daje dopiero relacja happens-before, którą tworzą: synchronized, volatile, start/join wątku,
 *   przekazanie zadania do executora, klasy z java.util.concurrent i pola final (po konstruktorze).
 *   JMM to rozdział 17 specyfikacji języka (JLS 17.4–17.7).
 *
 * ANALOGIA:
 *   Każdy wątek pracuje przy biurku z własnymi notatkami (rejestry, pamięć podręczna procesora, optymalizacje
 *   kompilatora). Wspólna tablica w korytarzu to pamięć główna. Nikt nie obiecuje, że Twoja zmiana na tablicy
 *   trafi do notatek kolegi. volatile i synchronized to umówiony rytuał: „zapisałem i ogłosiłem” → „przeczytałem
 *   ogłoszenie, więc odświeżam notatki”. Bez rytuału kolega może patrzeć w stare notatki w nieskończoność.
 *
 * JAK TO DZIAŁA:
 *   Trzy OSOBNE problemy:
 *     atomowość   — czy operacja wykona się w całości (count++ to odczyt + dodanie + zapis → nie jest atomowe)
 *     widoczność  — czy inny wątek W OGÓLE zobaczy nową wartość
 *     kolejność   — czy inny wątek zobaczy zapisy w kolejności z kodu (kompilator, JIT i procesor mogą je
 *                   przestawiać, jeśli JEDEN wątek nie zauważy różnicy)
 *   Reguły happens-before (A hb B → efekty A są widoczne dla B, a B „widzi” je we właściwej kolejności):
 *     • kolejność programu: wcześniejsza akcja wątku hb późniejsza akcja TEGO SAMEGO wątku
 *     • monitor: zwolnienie blokady m hb każde późniejsze zajęcie m
 *     • volatile: zapis pola volatile hb każdy późniejszy odczyt TEGO pola
 *     • Thread.start(): wywołanie start() hb każda akcja uruchomionego wątku
 *     • Thread.join(): każda akcja wątku hb udany powrót z join() na tym wątku
 *     • executor: akcje przed submit()/execute() hb wykonanie zadania; akcje zadania hb powrót z Future.get()
 *     • przechodniość: A hb B i B hb C → A hb C
 *     • pola final: wartości ustawione w konstruktorze widać po jego zakończeniu (o ile this nie uciekło)
 *
 * SŁÓWKA:
 *   memory model = model pamięci; visibility = widoczność; reordering = zmiana kolejności; atomicity = atomowość;
 *   happens-before = dzieje się przed; hoisting = wyciągnięcie (odczytu przed pętlę); tearing = rozerwanie
 *   (zapisu na połówki); false sharing = fałszywe współdzielenie; cache line = linia pamięci podręcznej;
 *   double-checked locking = blokada z podwójnym sprawdzeniem; spin = kręcić się (aktywne czekanie)
 *
 * ZOBACZ TEŻ: t21_concurrency/Concurrency02RaceConditions (atomowość, AtomicInteger),
 *   t21_concurrency/Concurrency03Locks (synchronized, blokady), t21_concurrency/Concurrency08ThreadSafetyPatterns
 *   (bezpieczna publikacja, holder), t21_concurrency/Concurrency01Threads (start, join, interrupt)
 * </pre>
 */
public class Concurrency10MemoryModel {

    public static void main(String[] args) {
        title("Concurrency10 — model pamięci Javy");

        threeProblems();          // three problems = trzy problemy
        happensBeforeRules();     // happens-before rules = reguły „dzieje się przed”
        volatileStopFlag();       // volatile stop flag = flaga zatrzymania volatile
        volatileIsNotAtomic();    // volatile is not atomic = volatile to nie atomowość
        safePiggyback();          // safe piggyback = bezpieczne „doczepienie” zapisów do volatile
        doubleCheckedLocking();   // double-checked locking = blokada z podwójnym sprawdzeniem
        longTearingAndFalseSharing(); // long tearing and false sharing = rozerwanie long i fałszywe współdzielenie
        guaranteesTable();        // guarantees table = tabela gwarancji
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI
    // =================================================================================================

    /** Uruchamia zadania w nazwanych wątkach i czeka (join) na wszystkie — join daje happens-before. */
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

    /** Aktywne czekanie z limitem czasu — nigdy nie wisi. Zwraca true, gdy warunek się spełnił. */
    static boolean spinUntil(BooleanSupplier condition, long timeoutMs) {   // BooleanSupplier = dostawca wartości logicznej
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);   // deadline = termin
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() - deadline > 0) {
                return false;
            }
            Thread.onSpinWait();                        // onSpinWait = podpowiedź dla CPU „kręcę się” (Java 9+)
        }
        return true;
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

    // =================================================================================================
    // 1. TRZY OSOBNE PROBLEMY
    // =================================================================================================

    /** Zwykłe (nie-volatile) pole dzielone przez wątki — do pokazywania, CO daje happens-before. */
    static final class Shared {                         // shared = współdzielony
        int value;                                      // zwykłe pole: bez volatile, bez synchronized
        int copy;
        boolean ready;
    }

    /**
     * 1. Atomowość, widoczność i kolejność to trzy różne rzeczy. Narzędzia rozwiązują różne ich zestawy:
     * AtomicInteger — wszystkie trzy dla jednej zmiennej; volatile — widoczność i kolejność, ale NIE
     * atomowość złożonych operacji; synchronized — wszystkie trzy dla kodu w bloku (gdy WSZYSCY używają tej samej blokady).
     */
    static void threeProblems() {
        section("1. Atomowość, widoczność, kolejność — trzy różne problemy");

        // Atomowość: count++ to TRZY kroki (odczyt, +1, zapis). Bajtkod w przybliżeniu:
        //     getfield count  →  iconst_1  →  iadd  →  putfield count
        // Dwa wątki mogą odczytać tę samą wartość i jeden przyrost zginie (t21_concurrency/Concurrency02RaceConditions).
        //
        // Widoczność: wątek A zapisuje stop = true, wątek B czyta stop w pętli. Bez happens-before JMM pozwala,
        // by B NIGDY nie zobaczył zmiany (sekcja 3).
        //
        // Kolejność: wątek A wykonuje   data = 42;  ready = true;   (oba pola zwykłe)
        // Wątek B może zobaczyć ready == true i data == 0 — kompilator/JIT/procesor wolno zamienić niezależne zapisy,
        // bo wątek A tego nie zauważy. Klasyczny przykład (x = y = 0 na starcie):
        //     wątek 1:  x = 1;  r1 = y;          wątek 2:  y = 1;  r2 = x;
        // Wynik r1 == 0 i r2 == 0 jest DOZWOLONY przez JMM (i bywa obserwowany na prawdziwym sprzęcie),
        // choć „intuicyjnie” któryś zapis musiał być pierwszy. Nie uruchamiamy tego — wynik zależy od uruchomienia.
        List<String> problems = List.of(
                "atomowość — czy operacja wykona się w całości",
                "widoczność — czy inny wątek zobaczy zapis",
                "kolejność — czy zobaczy zapisy w kolejności z kodu");
        showEach("trzy problemy", problems);
        // WYNIK: trzy problemy (liczba elementów: 3):
        // WYNIK: • atomowość — czy operacja wykona się w całości
        // WYNIK: • widoczność — czy inny wątek zobaczy zapis
        // WYNIK: • kolejność — czy zobaczy zapisy w kolejności z kodu

        // PUŁAPKA: „na moim komputerze działa” nic nie dowodzi. Dlaczego: x86 ma dość silny model pamięci
        // i wiele błędów się tam nie ujawnia, a na ARM (telefony, Apple M, serwery Graviton) wychodzą częściej.
        // Także JIT optymalizuje dopiero „gorący” kod, więc błąd może wyjść po godzinie działania produkcji.
        // DOBRA PRAKTYKA: rozumuj regułami happens-before, nie „sprzętem”. Dlaczego: tylko one są gwarancją specyfikacji.
    }

    // =================================================================================================
    // 2. REGUŁY HAPPENS-BEFORE W PRAKTYCE
    // =================================================================================================

    /**
     * 2. Każde demo używa ZWYKŁYCH pól (bez volatile) — a mimo to wynik jest gwarantowany, bo istnieje
     * łańcuch happens-before: start(), join(), submit()/get(), synchronized.
     */
    static void happensBeforeRules() {
        section("2. Reguły happens-before na zwykłych polach");

        // a) Thread.start(): zapis PRZED start() jest widoczny w nowym wątku.
        //    b) Thread.join(): zapis W wątku jest widoczny po powrocie z join().
        Shared s = new Shared();
        s.value = 42;                                   // przed start()
        runAll("hb", () -> s.copy = s.value + 1);       // wątek widzi 42 (start), main widzi copy (join)
        show("start/join: wątek zobaczył 42, main widzi copy", s.copy);
        // WYNIK: start/join: wątek zobaczył 42, main widzi copy → 43

        // c) executor: zapis przed submit() widoczny w zadaniu; wynik zadania widoczny po Future.get().
        Shared e = new Shared();
        ExecutorService pool = Executors.newSingleThreadExecutor(r -> new Thread(r, "hb-pula"));
        try {
            e.value = 7;                                // przed submit()
            Future<?> f = pool.submit(() -> {
                e.copy = e.value * 6;                   // zadanie widzi 7
            });
            f.get(5, TimeUnit.SECONDS);                 // po get() widzimy copy
            show("submit/get: copy", e.copy);
            // WYNIK: submit/get: copy → 42
        } catch (Exception ex) {
            show("błąd", ex);
        } finally {
            shutdownAndAwait(pool);
        }

        // d) monitor: zapis pod blokadą → odczyt pod TĄ SAMĄ blokadą (po jej zwolnieniu przez piszącego).
        Shared m = new Shared();
        Object lock = new Object();
        runAll("monitor",
                () -> {
                    synchronized (lock) {
                        m.value = 99;
                        m.ready = true;
                    }                                   // zwolnienie blokady
                },
                () -> {
                    boolean seen = spinUntil(() -> {
                        synchronized (lock) {           // zajęcie TEJ SAMEJ blokady
                            return m.ready;
                        }
                    }, 5000);
                    synchronized (lock) {
                        m.copy = seen ? m.value : -1;
                    }
                });
        show("synchronized: czytelnik zobaczył", m.copy);
        // WYNIK: synchronized: czytelnik zobaczył → 99

        // PUŁAPKA: synchronized tylko po stronie piszącego NIE wystarczy. Dlaczego: reguła wiąże zwolnienie
        // blokady z jej ZAJĘCIEM; czytelnik bez synchronized nie „zajmuje” niczego, więc nie ma happens-before.
        // DOBRA PRAKTYKA: w kodzie biznesowym korzystaj z gotowych narzędzi (executor, Future, kolekcje współbieżne,
        // CountDownLatch) — ich Javadoc opisuje „Memory consistency effects”, czyli właśnie takie gwarancje.
    }

    // =================================================================================================
    // 3. FLAGA ZATRZYMANIA — VOLATILE
    // =================================================================================================

    /** Pracownik z flagą volatile — POPRAWNA wersja. */
    static final class Worker implements Runnable {     // worker = pracownik
        private volatile boolean stop;                  // volatile = ulotny: każdy odczyt widzi ostatni zapis
        private long iterations;                        // czyta go main dopiero po join()

        @Override
        public void run() {
            while (!stop) {
                iterations++;
            }
        }

        void requestStop() {                            // request stop = poproś o zatrzymanie
            stop = true;
        }
    }

    /**
     * 3. Wątek kręci się w pętli, dopóki flaga stop jest false. Z volatile zatrzymanie jest gwarantowane.
     * Wersję BEZ volatile pokazujemy tylko w komentarzu — mogłaby nigdy się nie skończyć.
     */
    static void volatileStopFlag() {
        section("3. Flaga zatrzymania: volatile (wersja bez volatile tylko w komentarzu)");

        Worker worker = new Worker();
        Thread t = new Thread(worker, "pracownik");
        t.setDaemon(true);                              // zabezpieczenie: demon nie zatrzyma JVM
        t.start();
        spinUntil(() -> t.getState() == Thread.State.RUNNABLE, 2000);   // niech pętla ruszy
        worker.requestStop();
        try {
            t.join(5000);                               // join z limitem — nigdy nie wisimy
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        show("pracownik zatrzymał się (volatile)", !t.isAlive());
        // WYNIK: pracownik zatrzymał się (volatile) → true
        // Ile iteracji zdążył wykonać (pole iterations, bezpieczne do odczytu po join) — (wynik zależy od uruchomienia).

        // PUŁAPKA: ta sama klasa BEZ volatile (NIE uruchamiamy!):
        //     private boolean stop;                       // zwykłe pole
        //     public void run() { while (!stop) { iterations++; } }
        // JIT (kompilator C2 w HotSpot) widzi, że w pętli nic nie zapisuje stop i nie ma w niej żadnej
        // synchronizacji, więc może „wyciągnąć” odczyt przed pętlę (hoisting):
        //     if (!stop) { while (true) { iterations++; } }
        // JMM na to POZWALA: bez happens-before wątek nie musi nigdy zobaczyć zapisu z main.
        // Skutek: program może nigdy się nie zakończyć. Często tak się dzieje dopiero, gdy pętla się „rozgrzeje”.
        // PUŁAPKA: dodanie System.out.println w pętli często „naprawia” błąd — bo println używa synchronized
        // i przeszkadza optymalizacji. To przypadek, nie gwarancja: JMM nadal jej nie daje.
        // DOBRA PRAKTYKA: flaga zatrzymania = volatile boolean albo AtomicBoolean; dla wątków blokujących się
        // (sleep, wait, take) — interrupt() (t21_concurrency/Concurrency01Threads).
    }

    // =================================================================================================
    // 4. VOLATILE TO NIE ATOMOWOŚĆ
    // =================================================================================================

    static volatile int volatileCounter;                // licznik volatile (do demonstracji wyścigu)

    /**
     * 4. volatile daje widoczność i kolejność, ale count++ na polu volatile to nadal trzy kroki.
     * Wypisujemy tylko fakty pewne; liczba zgubionych przyrostów zależy od uruchomienia.
     */
    static void volatileIsNotAtomic() {
        section("4. volatile nie czyni count++ atomowym");

        volatileCounter = 0;
        Runnable inc = () -> {
            for (int i = 0; i < 100_000; i++) {
                volatileCounter++;                      // odczyt volatile, +1, zapis volatile — 3 kroki
            }
        };
        runAll("volatile", inc, inc);
        show("volatile count++: wynik ≤ 200000", volatileCounter <= 200_000);
        // WYNIK: volatile count++: wynik ≤ 200000 → true
        // Typowo wychodzi mniej niż 200000 (np. 130 000–190 000) — (wynik zależy od uruchomienia), czasem równo.

        AtomicInteger atomic = new AtomicInteger();
        Runnable atomicInc = () -> {
            for (int i = 0; i < 100_000; i++) {
                atomic.incrementAndGet();               // jedna niepodzielna operacja (CAS w pętli albo instrukcja CPU)
            }
        };
        runAll("atomic", atomicInc, atomicInc);
        show("AtomicInteger: wynik", atomic.get());
        // WYNIK: AtomicInteger: wynik → 200000

        // PUŁAPKA: kompilator javac tego NIE zgłosi — ostrzeże dopiero IntelliJ („Non-atomic operation on volatile
        // field” = nieatomowa operacja na polu volatile) albo analiza statyczna. Dlaczego: składniowo kod jest poprawny.
        // DOBRA PRAKTYKA: volatile dla flag i referencji zapisywanych przez JEDEN wątek (albo zapisów niezależnych od
        // poprzedniej wartości). Liczniki → AtomicInteger/LongAdder; kilka pól naraz → synchronized.
    }

    // =================================================================================================
    // 5. BEZPIECZNA PUBLIKACJA PRZEZ VOLATILE („DOCZEPIANIE”)
    // =================================================================================================

    /** Dane zwykłe + flaga volatile: zapisy PRZED zapisem flagi są widoczne PO odczycie flagi. */
    static final class Message {                        // message = wiadomość
        int a;                                          // zwykłe pola
        int b;
        String text;
        volatile boolean published;                     // published = opublikowana
    }

    /**
     * 5. Przechodniość happens-before: zapis a, b, text (kolejność programu) hb zapis published (volatile)
     * hb odczyt published == true hb odczyt a, b, text. Czytelnik, który zobaczył true, MUSI zobaczyć dane.
     */
    static void safePiggyback() {
        section("5. Zapis danych, potem flaga volatile — gwarantowana kolejność");

        Message msg = new Message();
        String[] result = new String[1];
        runAll("publikacja",
                () -> {
                    msg.a = 20;
                    msg.b = 22;
                    msg.text = "odpowiedź";
                    msg.published = true;               // zapis volatile jako OSTATNI
                },
                () -> {
                    boolean seen = spinUntil(() -> msg.published, 5000);   // odczyt volatile jako PIERWSZY
                    result[0] = seen ? msg.text + " = " + (msg.a + msg.b) : "nie doczekano się";
                });
        show("czytelnik zobaczył", result[0]);
        // WYNIK: czytelnik zobaczył → odpowiedź = 42

        // PUŁAPKA: kolejność ma znaczenie. Gdyby piszący ustawił published = true PRZED msg.a = 20, czytelnik mógłby
        // zobaczyć published == true i a == 0. Dlaczego: gwarancja obejmuje tylko zapisy WYKONANE PRZED zapisem volatile.
        // PUŁAPKA: po publikacji dane nie mogą się już zmieniać (albo ich zmiany wymagają kolejnej synchronizacji).
        // DOBRA PRAKTYKA: zamiast ręcznej flagi — przekaż obiekt przez BlockingQueue, CompletableFuture lub
        // AtomicReference. Dlaczego: robią to samo, a intencja jest czytelna.
    }

    // =================================================================================================
    // 6. DOUBLE-CHECKED LOCKING
    // =================================================================================================

    static final AtomicInteger CREATED = new AtomicInteger();

    /** Drogi obiekt z ZWYKŁYMI polami — przy złej publikacji można by je zobaczyć niezainicjalizowane. */
    static final class Connection {                     // connection = połączenie
        String url;
        int timeoutMs;

        Connection() {
            CREATED.incrementAndGet();
            url = "jdbc:h2:mem:kurs";
            timeoutMs = 3000;
        }
    }

    /** POPRAWNE double-checked locking — pole MUSI być volatile. */
    static final class ConnectionHolder {
        private static volatile Connection instance;    // volatile — kluczowe!

        static Connection get() {
            Connection local = instance;                // jeden odczyt volatile na szybkiej ścieżce
            if (local == null) {                        // 1. sprawdzenie (bez blokady)
                synchronized (ConnectionHolder.class) {
                    local = instance;
                    if (local == null) {                // 2. sprawdzenie (pod blokadą)
                        local = new Connection();
                        instance = local;               // zapis volatile PO zbudowaniu obiektu
                    }
                }
            }
            return local;
        }
    }

    /**
     * 6. Double-checked locking (DCL): sprawdź bez blokady, a dopiero gdy null — zablokuj i sprawdź znowu.
     * Bez volatile ten wzorzec jest BŁĘDNY. Uruchamiamy tylko wersję poprawną.
     */
    static void doubleCheckedLocking() {
        section("6. Double-checked locking — tylko z volatile");

        CREATED.set(0);
        Connection[] seen = new Connection[4];
        Runnable[] tasks = new Runnable[4];
        for (int i = 0; i < 4; i++) {
            int idx = i;
            tasks[i] = () -> {
                for (int k = 0; k < 1000; k++) {
                    seen[idx] = ConnectionHolder.get();
                }
            };
        }
        runAll("dcl", tasks);
        show("utworzonych połączeń", CREATED.get());
        // WYNIK: utworzonych połączeń → 1
        show("wszystkie wątki mają ten sam obiekt", Arrays.stream(seen).allMatch(c -> c == seen[0]));
        // WYNIK: wszystkie wątki mają ten sam obiekt → true
        show("pola zainicjalizowane", seen[0].url + ", " + seen[0].timeoutMs + " ms");
        // WYNIK: pola zainicjalizowane → jdbc:h2:mem:kurs, 3000 ms

        // PUŁAPKA: wersja BEZ volatile (klasyczny błąd sprzed Javy 5):
        //     private static Connection instance;             // zwykłe pole
        //     if (instance == null) { synchronized (X.class) { if (instance == null) instance = new Connection(); } }
        //     return instance;
        // „instance = new Connection()” to: przydziel pamięć, wykonaj konstruktor, zapisz referencję. Bez volatile
        // zapis referencji może stać się widoczny dla innego wątku PRZED zapisami pól z konstruktora. Drugi wątek
        // przy 1. sprawdzeniu (bez blokady!) zobaczy instance != null i użyje obiektu z url == null, timeoutMs == 0.
        // Z volatile (JMM od Javy 5, JSR-133) zapis volatile następuje po konstruktorze i daje happens-before.
        // DOBRA PRAKTYKA: zamiast DCL użyj idiomu holder albo enum (t21_concurrency/Concurrency08ThreadSafetyPatterns).
        // Dlaczego: prostsze, bez volatile i bez ryzyka pomyłki. DCL ma sens głównie dla pól instancji.
    }

    // =================================================================================================
    // 7. ROZERWANIE LONG/DOUBLE I FAŁSZYWE WSPÓŁDZIELENIE
    // =================================================================================================

    static volatile long volatileLong;                  // volatile long — zapis i odczyt ZAWSZE atomowe

    /**
     * 7. Dwie ciekawostki z pogranicza: JLS 17.7 pozwala dzielić zapis zwykłego long/double na dwie połówki
     * 32-bitowe; fałszywe współdzielenie spowalnia (ale nie psuje) programy.
     */
    static void longTearingAndFalseSharing() {
        section("7. Rozerwanie long/double (JLS 17.7) i fałszywe współdzielenie");

        // JLS 17.7: zapis do NIE-volatile pola long lub double może być potraktowany jak DWA osobne zapisy po 32 bity.
        // Inny wątek może wtedy odczytać „połowę starej i połowę nowej” wartości (tearing = rozerwanie).
        // Zapisy i odczyty volatile long/double są zawsze atomowe. Zapisy i odczyty referencji oraz int, short,
        // byte, char, boolean, float — też zawsze atomowe.
        // Na 64-bitowych JVM HotSpot zwykłe long/double w praktyce zapisują się w całości, ale specyfikacja tego
        // NIE gwarantuje (zachęca tylko implementacje, by unikały dzielenia).
        long pattern = 0x1111_1111_2222_2222L;          // górna i dolna połówka różne — łatwo byłoby zauważyć rozerwanie
        volatileLong = pattern;
        show("volatile long odczytany w całości", volatileLong == pattern);
        // WYNIK: volatile long odczytany w całości → true
        show("górna połówka (hex)", Long.toHexString(volatileLong >>> 32));   // toHexString = jako tekst szesnastkowy
        // WYNIK: górna połówka (hex) → 11111111
        show("dolna połówka (hex)", Long.toHexString(volatileLong & 0xFFFF_FFFFL));
        // WYNIK: dolna połówka (hex) → 22222222

        // Fałszywe współdzielenie (false sharing): procesor przenosi pamięć między rdzeniami całymi liniami
        // (zwykle 64 bajty). Dwa NIEZALEŻNE pola w tej samej linii, zmieniane przez dwa rdzenie, powodują ciągłe
        // „przerzucanie” linii między rdzeniami — program działa POPRAWNIE, ale wolniej. JDK broni się wewnętrzną
        // adnotacją @Contended (np. komórki LongAdder), która rozsuwa pola do osobnych linii.
        LongAdder adder = new LongAdder();
        runAll("adder",
                () -> IntStream.range(0, 50_000).forEach(i -> adder.increment()),
                () -> IntStream.range(0, 50_000).forEach(i -> adder.increment()));
        show("LongAdder (komórki rozsunięte przez @Contended) — suma", adder.sum());
        // WYNIK: LongAdder (komórki rozsunięte przez @Contended) — suma → 100000
        // PUŁAPKA: nie optymalizuj „pod linie pamięci” na ślepo. Dlaczego: efekt widać tylko w pomiarach (JMH)
        // bardzo gorącego kodu; w zwykłej aplikacji to mikrooptymalizacja bez znaczenia.
    }

    // =================================================================================================
    // 8. TABELA: CO GWARANTUJE CO
    // =================================================================================================

    /**
     * 8. Podsumowanie narzędzi w trzech wymiarach: atomowość / widoczność / kolejność.
     */
    static void guaranteesTable() {
        section("8. Co gwarantuje co");

        // narzędzie                     | atomowość                 | widoczność | kolejność (hb)
        // ------------------------------+---------------------------+------------+----------------
        // zwykłe pole                   | tylko pojedynczy odczyt/zapis (bez long/double) | NIE | NIE
        // volatile                      | pojedynczy odczyt/zapis (też long/double), NIE count++ | TAK | TAK
        // synchronized / Lock           | cały blok (dla tej samej blokady) | TAK | TAK
        // AtomicXxx                     | operacje złożone (incrementAndGet, CAS) | TAK | TAK
        // final (po konstruktorze)      | — (pole się nie zmienia)  | TAK        | TAK (dla wartości z konstruktora)
        // start / join / executor / Future / latch | —              | TAK        | TAK
        List<String> rows = List.of(
                "zwykłe pole → atomowość: pojedynczy zapis (poza long/double), widoczność: NIE, kolejność: NIE",
                "volatile → atomowość: pojedynczy zapis/odczyt, widoczność: TAK, kolejność: TAK",
                "synchronized → atomowość: cały blok, widoczność: TAK, kolejność: TAK",
                "AtomicInteger → atomowość: operacje złożone, widoczność: TAK, kolejność: TAK",
                "final → wartość z konstruktora widoczna dla wszystkich (bez ucieczki this)",
                "start/join/submit/get/latch → widoczność i kolejność między wątkami");
        showEach("gwarancje", rows);
        // WYNIK: gwarancje (liczba elementów: 6):
        // WYNIK: • zwykłe pole → atomowość: pojedynczy zapis (poza long/double), widoczność: NIE, kolejność: NIE
        // WYNIK: • volatile → atomowość: pojedynczy zapis/odczyt, widoczność: TAK, kolejność: TAK
        // WYNIK: • synchronized → atomowość: cały blok, widoczność: TAK, kolejność: TAK
        // WYNIK: • AtomicInteger → atomowość: operacje złożone, widoczność: TAK, kolejność: TAK
        // WYNIK: • final → wartość z konstruktora widoczna dla wszystkich (bez ucieczki this)
        // WYNIK: • start/join/submit/get/latch → widoczność i kolejność między wątkami

        // DOBRA PRAKTYKA: jeśli nie potrafisz wskazać krawędzi happens-before między zapisem a odczytem, to masz
        // wyścig danych (data race) — nawet jeśli testy przechodzą. Dlaczego: obietnicę „program zachowuje się tak,
        // jakby wątki wykonywały się po kolei” (sekwencyjna spójność) JMM daje tylko programom bez wyścigów danych
        // (DRF = data-race-free = wolny od wyścigów danych).
        // W Springu: kontener tworzy singletony przy starcie, a jego rejestr jest synchronizowany; pola wstrzykiwane przez
        // konstruktor oznaczaj final — wtedy bezpieczeństwo daje także semantyka pól final.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Trzy problemy: atomowość, widoczność, kolejność — różne narzędzia rozwiązują różne ich zestawy.
     *   • Bez happens-before wątek może NIGDY nie zobaczyć zapisu innego wątku albo zobaczyć go w innej kolejności.
     *   • hb tworzą: kolejność programu, unlock→lock tej samej blokady, zapis→odczyt tego samego volatile,
     *     start(), join(), submit()→zadanie→Future.get(), narzędzia j.u.c., przechodniość; pola final po konstruktorze.
     *   • Flaga zatrzymania: volatile boolean / AtomicBoolean. Bez volatile JIT może wyciągnąć odczyt przed pętlę.
     *   • volatile ≠ atomowość: volatile count++ gubi przyrosty → AtomicInteger / LongAdder.
     *   • Publikacja: najpierw dane, NA KOŃCU zapis volatile; czytelnik NAJPIERW czyta volatile, potem dane.
     *   • DCL działa tylko z volatile; prościej: holder albo enum.
     *   • JLS 17.7: zwykły long/double może się rozerwać; volatile long/double i referencje — nigdy.
     *   • False sharing: problem wydajności, nie poprawności.
     *   • „U mnie działa” ≠ poprawne: x86 i nierozgrzany JIT ukrywają błędy.
     *
     * PYTANIA KONTROLNE:
     *   1. Wymień trzy osobne problemy współbieżności, których dotyczy model pamięci.
     *   2. ZNAJDŹ BŁĄD:
     *        class Task implements Runnable { private boolean running = true;
     *            public void run() { while (running) { work(); } }
     *            void stop() { running = false; } }
     *   3. Co wypisze (pewny fakt): pole volatile int c = 0; dwa wątki po 1000 razy c++; po join:
     *        System.out.println(c <= 2000);
     *   4. Dlaczego double-checked locking bez volatile jest błędny?
     *   5. Wątek A: data = 5; flag = true; (flag volatile). Wątek B: if (flag) print(data). Co może wypisać B?
     *      A co, gdyby w A kolejność była odwrotna (flag = true; data = 5;)?
     *   6. Co wypisze:
     *        int[] box = {0};
     *        Thread t = new Thread(() -> box[0] = 10);
     *        t.start(); t.join();
     *        System.out.println(box[0]);
     *   7. Czy zapis zwykłego pola long jest atomowy według JLS? A volatile long?
     *   8. Czy wystarczy, że TYLKO metoda zapisująca jest synchronized, a odczyt nie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        int[] values = IntStream.range(0, 20_000).map(i -> (i * 7919) % 10_007).toArray();
        int expectedMax = Arrays.stream(values).max().orElseThrow();   // orElseThrow = albo rzuć (Java 10+)
        Check.equal("ćw. 1: pracownik z flagą zatrzymuje się", true, () -> exercise1());
        Check.equal("ćw. 2: DCL z volatile — 8 wątków, utworzono obiektów", 1, () -> exercise2(8));
        Check.equal("ćw. 3: publikacja przez volatile — suma danych", 60, () -> exercise3(10, 20, 30));
        Check.equal("ćw. 4: maksimum pętlą compareAndSet (4 wątki)", expectedMax, () -> exercise4(values, 4));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", true, () -> solution1());
        Check.equal("ćw. 2 (wzorzec)", 1, () -> solution2(8));
        Check.equal("ćw. 3 (wzorzec)", 60, () -> solution3(10, 20, 30));
        Check.equal("ćw. 4 (wzorzec)", expectedMax, () -> solution4(values, 4));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): napisz pracownika, który kręci się w pętli, dopóki flaga nie zostanie ustawiona,
     * uruchom go w wątku-demonie, ustaw flagę z main i poczekaj join(5000). Zwróć true, jeśli wątek się
     * zakończył (!isAlive()).
     * Podpowiedź: flaga jako AtomicBoolean (zmienna lokalna, efektywnie finalna) albo pole volatile w klasie
     * zagnieżdżonej — NIE zwykły boolean.
     */
    static boolean exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ błędne leniwe tworzenie na poprawne double-checked locking z volatile
     * (albo holder). {@code threads} wątków po 1000 razy pobiera obiekt; zwróć, ile obiektów UTWORZONO.
     * <pre>{@code
     * static Service instance;                         // brak volatile, brak blokady
     * static Service get() {
     *     if (instance == null) instance = new Service();   // wyścig + zła publikacja
     *     return instance;
     * }
     * }</pre>
     * Podpowiedź: licznik utworzeń w konstruktorze (AtomicInteger); klasa z polem static volatile i metodą get()
     * jak ConnectionHolder. Uwaga: pole static — wyzeruj je (albo użyj nowej klasy), jeśli wołasz wielokrotnie.
     */
    static int exercise2(int threads) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): wątek-pisarz zapisuje trzy ZWYKŁE pola (a, b, c), a na końcu ustawia flagę volatile.
     * Wątek-czytelnik czeka (spinUntil z limitem) na flagę i liczy a + b + c. Zwróć sumę widzianą przez czytelnika.
     * Podpowiedź: klasa z polami int a, b, c i volatile boolean ready (jak Message); wynik czytelnika przekaż do
     * main przez tablicę/pole i odczytaj PO join() (dlaczego to bezpieczne?).
     */
    static int exercise3(int a, int b, int c) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): znajdź maksimum tablicy w {@code threads} wątkach (każdy przegląda swój kawałek)
     * i aktualizuj WSPÓLNE AtomicInteger max własną pętlą compareAndSet (bez accumulateAndGet i bez blokad).
     * Podpowiedź: int cur; do { cur = max.get(); if (v ≤ cur) break; } while (!max.compareAndSet(cur, v));
     * Start: new AtomicInteger(Integer.MIN_VALUE).
     */
    static int exercise4(int[] values, int threads) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1() {
        Worker worker = new Worker();                   // pole stop jest volatile
        Thread t = new Thread(worker, "cw1");
        t.setDaemon(true);
        t.start();
        worker.requestStop();
        try {
            t.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return !t.isAlive();
    }

    static final AtomicInteger SERVICES_CREATED = new AtomicInteger();

    static final class Service {
        Service() {
            SERVICES_CREATED.incrementAndGet();
        }
    }

    static final class ServiceHolder {
        private static volatile Service instance;

        static Service get() {
            Service local = instance;
            if (local == null) {
                synchronized (ServiceHolder.class) {
                    local = instance;
                    if (local == null) {
                        local = new Service();
                        instance = local;
                    }
                }
            }
            return local;
        }

        static void resetForDemo() {                    // tylko na potrzeby wielokrotnego wywołania ćwiczenia
            synchronized (ServiceHolder.class) {
                instance = null;
            }
        }
    }

    static int solution2(int threads) {
        ServiceHolder.resetForDemo();
        SERVICES_CREATED.set(0);
        Runnable[] tasks = new Runnable[threads];
        for (int i = 0; i < threads; i++) {
            tasks[i] = () -> {
                for (int k = 0; k < 1000; k++) {
                    ServiceHolder.get();
                }
            };
        }
        runAll("cw2", tasks);
        return SERVICES_CREATED.get();
    }

    static final class Triple {
        int a;
        int b;
        int c;
        volatile boolean ready;
    }

    static int solution3(int a, int b, int c) {
        Triple t = new Triple();
        int[] result = {-1};
        runAll("cw3",
                () -> {
                    t.a = a;
                    t.b = b;
                    t.c = c;
                    t.ready = true;
                },
                () -> {
                    if (spinUntil(() -> t.ready, 5000)) {
                        result[0] = t.a + t.b + t.c;
                    }
                });
        return result[0];                               // bezpieczne: zapis czytelnika hb powrót z join()
    }

    static int solution4(int[] values, int threads) {
        AtomicInteger max = new AtomicInteger(Integer.MIN_VALUE);
        Runnable[] tasks = new Runnable[threads];
        int n = values.length;
        for (int i = 0; i < threads; i++) {
            int from = i * n / threads;
            int to = (i + 1) * n / threads;
            tasks[i] = () -> {
                for (int k = from; k < to; k++) {
                    int v = values[k];
                    int cur;
                    do {
                        cur = max.get();
                        if (v <= cur) {
                            break;
                        }
                    } while (!max.compareAndSet(cur, v));   // compareAndSet = porównaj i ustaw
                }
            };
        }
        runAll("cw4", tasks);
        return max.get();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Atomowość (czy operacja wykona się w całości), widoczność (czy inny wątek zobaczy zapis) i kolejność
     *      (czy zobaczy zapisy w kolejności z kodu).
     *   2. Pole running nie jest volatile — wątek wykonujący run() może nigdy nie zobaczyć running = false (JIT może
     *      wyciągnąć odczyt przed pętlę). Poprawnie: private volatile boolean running = true; (albo AtomicBoolean).
     *   3. true — volatile nie chroni c++ przed zgubieniem przyrostów, ale wynik nie przekroczy 2000.
     *      Dokładna wartość zależy od uruchomienia.
     *   4. Zapis referencji do pola może stać się widoczny przed zapisami pól z konstruktora; drugi wątek przy
     *      pierwszym sprawdzeniu (bez blokady) zobaczy instance != null i użyje niedokończonego obiektu.
     *      volatile zakazuje takiego przestawienia i daje happens-before.
     *   5. Przy kolejności „data = 5; flag = true;” B wypisze 5 albo nic (jeśli jeszcze nie widzi flag == true).
     *      Przy odwrotnej kolejności B może zobaczyć flag == true i wypisać 0 (albo 5) — zapis data jest po zapisie
     *      volatile, więc nie jest nim objęty.
     *   6. 10 — zapis w wątku hb powrót z join(), więc main na pewno zobaczy 10.
     *   7. Zwykły long/double — nie (JLS 17.7 pozwala na dwa zapisy po 32 bity); volatile long/double — tak, zawsze.
     *   8. Nie. happens-before powstaje między zwolnieniem i ZAJĘCIEM tej samej blokady. Czytelnik bez synchronized
     *      nie ma gwarancji widoczności ani spójnego odczytu kilku pól.
     */
    // </editor-fold>
}
