package t21_concurrency;

import helpers.Check;
import helpers.SampleData;
import helpers.model.Category;
import helpers.model.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Kolekcje współbieżne — ConcurrentHashMap, CopyOnWriteArrayList, BlockingQueue i spółka
 *        (concurrent = współbieżny; collection = kolekcja; queue = kolejka; blocking = blokujący)
 *
 * W SKRÓCIE:
 *   Zwykłe ArrayList i HashMap nie są bezpieczne wątkowo. Pakiet java.util.concurrent daje kolekcje,
 *   które same dbają o poprawność przy wielu wątkach i mają ATOMOWE operacje złożone
 *   (merge, computeIfAbsent, putIfAbsent), kolejki blokujące do wzorca producent–konsument
 *   i iteratory, które nie rzucają ConcurrentModificationException.
 *
 * ANALOGIA:
 *   synchronizedList to sklep z jedną kasą i ochroniarzem przy drzwiach: wpuszcza jedną osobę naraz,
 *   ale jeśli chcesz „sprawdzić, czy jest chleb, i go kupić”, ktoś może wejść między sprawdzeniem
 *   a zakupem. ConcurrentHashMap to sklep z wieloma kasami i operacją „kup, jeśli jest” wykonywaną
 *   w jednym ruchu. BlockingQueue to taśma na poczcie: nadawca kładzie paczki, odbiorca zdejmuje;
 *   gdy taśma pełna — nadawca czeka, gdy pusta — czeka odbiorca.
 *
 * JAK TO DZIAŁA:
 *   kolekcja                     | odczyt           | zapis              | iterator
 *   -----------------------------+------------------+--------------------+-------------------------
 *   Collections.synchronizedList | blokada na całość| blokada na całość  | fail-fast, ręczny synchronized
 *   ConcurrentHashMap            | bez blokady      | blokada 1 kubełka  | słabo spójny, bez CME
 *   CopyOnWriteArrayList         | bez blokady      | KOPIA całej tablicy| migawka (snapshot)
 *   ArrayBlockingQueue           | blokada          | blokada, limit     | słabo spójny
 *   ConcurrentLinkedQueue        | bez blokady (CAS)| bez blokady (CAS)  | słabo spójny
 *   ConcurrentSkipListMap        | bez blokady      | bez blokady (CAS)  | słabo spójny, posortowany
 *
 *   „Bezpieczna wątkowo” kolekcja gwarantuje poprawność POJEDYNCZEJ operacji. Ciąg dwóch operacji
 *   (get, a potem put) nadal może przeplatać się z innymi wątkami — wtedy potrzebna jest jedna
 *   operacja atomowa (merge, compute) albo własna blokada.
 *
 * SŁÓWKA:
 *   concurrent = współbieżny; thread-safe = bezpieczny wątkowo; compound action = operacja złożona;
 *   weakly consistent = słabo spójny; snapshot = migawka; copy-on-write = kopiuj przy zapisie;
 *   producer = producent; consumer = konsument; poison pill = „trująca pigułka” (znacznik końca);
 *   skip list = lista z przeskokami; bounded = ograniczony (pojemnością); contention = rywalizacja wątków
 *
 * ZOBACZ TEŻ: t12_collections/Collections05Maps (zwykłe mapy, merge i compute),
 *   t21_concurrency/Concurrency02RaceConditions (wyścig, check-then-act),
 *   t21_concurrency/Concurrency04Executors (pule wątków użyte w tej lekcji),
 *   t21_concurrency/Concurrency07Synchronizers (CountDownLatch, CyclicBarrier)
 * </pre>
 */
public class Concurrency06ConcurrentCollections {

    public static void main(String[] args) {
        title("Concurrency06 — kolekcje współbieżne");

        synchronizedWrapperLimits();   // synchronized wrapper limits = granice opakowań synchronizowanych
        concurrentHashMapBasics();     // concurrent hash map basics = podstawy ConcurrentHashMap
        getThenPutRace();              // get then put race = wyścig „pobierz, potem wstaw”
        parallelWordCount();           // parallel word count = równoległe liczenie słów
        weaklyConsistentIterators();   // weakly consistent iterators = słabo spójne iteratory
        copyOnWriteList();             // copy-on-write list = lista kopiowana przy zapisie
        blockingQueueMethods();        // blocking queue methods = metody kolejki blokującej
        producerConsumer();            // producer consumer = producent–konsument
        nonBlockingAndSorted();        // non-blocking and sorted = nieblokujące i posortowane
        exercises();                   // exercises = ćwiczenia
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI
    // =================================================================================================

    /** Pula o stałej liczbie wątków z WŁASNYMI nazwami (domyślne nazwy pool-N-thread-M zależą od historii). */
    static ExecutorService namedPool(String prefix, int threads) {
        AtomicInteger counter = new AtomicInteger();                    // counter = licznik
        ThreadFactory factory = r -> new Thread(r, prefix + "-" + counter.incrementAndGet()); // factory = fabryka
        return Executors.newFixedThreadPool(threads, factory);         // newFixedThreadPool = nowa pula o stałym rozmiarze
    }

    /** Zamyka pulę i czeka na koniec zadań — bez tego wątki puli trzymają JVM przy życiu. */
    static void shutdownAndAwait(ExecutorService pool) {
        pool.shutdown();                                                // shutdown = zamknij (nie przyjmuj nowych zadań)
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {         // awaitTermination = czekaj na zakończenie
                pool.shutdownNow();                                     // shutdownNow = zamknij natychmiast
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();                         // przywracamy flagę przerwania
        }
    }

    /** Uruchamia zadania w osobnych, nazwanych wątkach i czeka (join) na wszystkie. */
    static void runAll(String prefix, Runnable... tasks) {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < tasks.length; i++) {
            threads.add(new Thread(tasks[i], prefix + "-" + (i + 1)));
        }
        threads.forEach(Thread::start);
        for (Thread t : threads) {
            try {
                t.join();                                               // join = dołącz (poczekaj na koniec wątku)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /** Czeka na zatrzask z limitem czasu; nigdy nie wisi w nieskończoność. */
    static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("przekroczono czas oczekiwania");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("przerwano oczekiwanie", e);
        }
    }

    // =================================================================================================
    // 1. COLLECTIONS.SYNCHRONIZEDLIST — DLACZEGO TO ZA MAŁO
    // =================================================================================================

    /**
     * 1. {@code Collections.synchronizedList} owija każdą metodę w synchronized. Pojedyncze add jest bezpieczne,
     * ale operacja złożona „sprawdź, potem dodaj” — nie. Pokazujemy przeplot WYMUSZONY zatrzaskami,
     * więc wynik jest zawsze ten sam.
     */
    static void synchronizedWrapperLimits() {
        section("1. Collections.synchronizedList — pojedyncze operacje tak, złożone nie");

        // synchronizedList = lista synchronizowana (opakowanie: każda metoda bierze blokadę na liście)
        List<Integer> numbers = Collections.synchronizedList(new ArrayList<>());
        Runnable add1000 = () -> {
            for (int i = 0; i < 1000; i++) {
                numbers.add(i);
            }
        };
        runAll("dodawacz", add1000, add1000, add1000, add1000);
        show("4 wątki × 1000 add → rozmiar", numbers.size());
        // WYNIK: 4 wątki × 1000 add → rozmiar → 4000
        // Na zwykłej ArrayList ten sam kod gubi elementy albo rzuca ArrayIndexOutOfBoundsException (wynik zależy od uruchomienia).

        // PUŁAPKA: check-then-act (sprawdź, potem działaj) na liście synchronizowanej. Każda metoda z osobna
        // jest atomowa, ale MIĘDZY contains a add drugi wątek może zrobić to samo. Wymuszamy ten przeplot:
        List<String> users = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch aChecked = new CountDownLatch(1);   // A już sprawdził
        CountDownLatch bAdded = new CountDownLatch(1);     // B już dodał
        Runnable threadA = () -> {
            if (!users.contains("ola")) {                  // contains = zawiera → false
                aChecked.countDown();                      // countDown = odlicz w dół
                await(bAdded);                             // czekamy, aż B wstawi „ola”
                users.add("ola");                          // A dodaje, bo wcześniej „widział”, że nie ma
            }
        };
        Runnable threadB = () -> {
            await(aChecked);
            if (!users.contains("ola")) {                  // też false — A jeszcze nie dodał
                users.add("ola");
            }
            bAdded.countDown();
        };
        runAll("rejestracja", threadA, threadB);
        show("lista po wymuszonym przeplocie", users);
        // WYNIK: lista po wymuszonym przeplocie → [ola, ola]

        // Naprawa: cała operacja złożona pod JEDNĄ blokadą — tą samą, której używa opakowanie (sam obiekt listy).
        List<String> safeUsers = Collections.synchronizedList(new ArrayList<>());
        Runnable addIfAbsent = () -> {
            synchronized (safeUsers) {                     // ten sam monitor co wewnątrz opakowania
                if (!safeUsers.contains("ola")) {
                    safeUsers.add("ola");
                }
            }
        };
        runAll("rejestracja-ok", addIfAbsent, addIfAbsent, addIfAbsent);
        show("z synchronized(list) { ... }", safeUsers);
        // WYNIK: z synchronized(list) { ... } → [ola]

        // PUŁAPKA: iteracja po liście synchronizowanej NIE jest chroniona automatycznie. Iterator jest „fail-fast”
        // (szybko zawodzący): wykrywa zmianę listy w trakcie i rzuca ConcurrentModificationException.
        // Pokazujemy to w jednym wątku (deterministycznie) — z drugim wątkiem byłoby tak samo, tylko losowo.
        List<String> names = Collections.synchronizedList(new ArrayList<>(List.of("a", "b", "c")));
        expectThrows("usuwanie w trakcie for-each", () -> {
            for (String n : names) {
                if (n.equals("a")) {
                    names.remove(n);
                }
            }
        });
        // WYNIK: ✔ usuwanie w trakcie for-each → rzucono ConcurrentModificationException: (brak komunikatu)

        // DOBRA PRAKTYKA: jeśli już musisz iterować po synchronizedList przy wielu wątkach, rób to w
        // synchronized (list) { for (...) ... } — tak każe Javadoc. Dlaczego: inaczej inny wątek może zmienić
        // listę między kolejnymi next(). Lepiej jednak sięgnąć po kolekcję współbieżną z dalszych sekcji.
        int total;
        synchronized (numbers) {
            total = 0;
            for (int n : numbers) {
                total += n;
            }
        }
        show("suma liczb (iteracja pod blokadą)", total);
        // WYNIK: suma liczb (iteracja pod blokadą) → 1998000
    }

    // =================================================================================================
    // 2. CONCURRENTHASHMAP — OPERACJE ATOMOWE
    // =================================================================================================

    /**
     * 2. {@code ConcurrentHashMap} — mapa, w której odczyty nie blokują, a zapisy blokują tylko mały fragment.
     * Najważniejsze: metody złożone (putIfAbsent, computeIfAbsent, compute, merge) są ATOMOWE.
     */
    static void concurrentHashMapBasics() {
        section("2. ConcurrentHashMap — putIfAbsent, computeIfAbsent, compute, merge");

        ConcurrentHashMap<String, Integer> stock = new ConcurrentHashMap<>();

        // putIfAbsent = wstaw, jeśli brak; zwraca POPRZEDNIĄ wartość albo null, gdy wstawiono
        show("putIfAbsent(kawa, 10)", stock.putIfAbsent("kawa", 10));
        // WYNIK: putIfAbsent(kawa, 10) → null
        show("putIfAbsent(kawa, 99)", stock.putIfAbsent("kawa", 99));
        // WYNIK: putIfAbsent(kawa, 99) → 10

        // merge = scal: brak klucza → wstaw wartość; jest → połącz funkcją. Idealne do liczników.
        stock.merge("kawa", 5, Integer::sum);
        stock.merge("herbata", 3, Integer::sum);
        show("po merge", new TreeMap<>(stock));
        // WYNIK: po merge → {herbata=3, kawa=15}

        // compute = oblicz nową wartość z klucza i starej wartości (null = brak); zwrócenie null USUWA wpis
        stock.compute("herbata", (key, old) -> old == null ? 1 : old - 3);
        show("compute zwrócił 0 → wpis zostaje", new TreeMap<>(stock));
        // WYNIK: compute zwrócił 0 → wpis zostaje → {herbata=0, kawa=15}
        stock.compute("herbata", (key, old) -> old != null && old == 0 ? null : old);
        show("compute zwrócił null → wpis usunięty", new TreeMap<>(stock));
        // WYNIK: compute zwrócił null → wpis usunięty → {kawa=15}

        // computeIfAbsent = oblicz, jeśli brak — klasyka pamięci podręcznej (cache) i grupowania.
        // Javadoc gwarantuje: funkcja wywoła się najwyżej raz na wywołanie metody, a całość jest atomowa.
        ConcurrentHashMap<Category, List<String>> byCategory = new ConcurrentHashMap<>();
        for (Product p : SampleData.products()) {
            if (p.category() == Category.KSIAZKI) {
                // PUŁAPKA: przy wielu wątkach wartość-lista musi sama być bezpieczna wątkowo! computeIfAbsent
                // chroni tylko WSTAWIENIE listy, a .add() na niej dzieje się już poza blokadą mapy.
                byCategory.computeIfAbsent(p.category(), c -> new CopyOnWriteArrayList<>()).add(p.name());
            }
        }
        show("computeIfAbsent + add", byCategory.get(Category.KSIAZKI));
        // WYNIK: computeIfAbsent + add → [Czysty kod, Java. Podstawy, Wzorce projektowe]

        // PUŁAPKA: ConcurrentHashMap NIE przyjmuje null — ani jako klucz, ani jako wartość. Dlaczego: przy wielu
        // wątkach get(k) == null musi jednoznacznie znaczyć „brak klucza” (HashMap pozwala na null i tam to niejasne).
        expectThrows("put(null, 1)", () -> stock.put(null, 1));
        // WYNIK: ✔ put(null, 1) → rzucono NullPointerException: (brak komunikatu)
        expectThrows("put(\"mleko\", null)", () -> stock.put("mleko", null));
        // WYNIK: ✔ put("mleko", null) → rzucono NullPointerException: (brak komunikatu)

        // DOBRA PRAKTYKA: funkcje w compute/merge/computeIfAbsent mają być KRÓTKIE i bez efektów ubocznych.
        // Dlaczego: w trakcie ich działania inne wątki aktualizujące ten fragment mapy czekają. Javadoc zabrania też
        // modyfikowania TEJ SAMEJ mapy wewnątrz funkcji (może skończyć się wyjątkiem IllegalStateException).
    }

    // =================================================================================================
    // 3. PUŁAPKA: GET, A POTEM PUT — NADAL WYŚCIG
    // =================================================================================================

    /**
     * 3. Kolekcja współbieżna nie naprawia złego użycia. {@code get} i {@code put} są osobno bezpieczne,
     * ale razem tworzą read-modify-write (odczytaj–zmień–zapisz) — klasyczny wyścig.
     */
    static void getThenPutRace() {
        section("3. PUŁAPKA: get, a potem put na ConcurrentHashMap");

        // Wymuszony przeplot: oba wątki NAJPIERW czytają, dopiero potem zapisują.
        ConcurrentHashMap<String, Integer> visits = new ConcurrentHashMap<>();   // visits = odwiedziny
        visits.put("strona", 0);
        CountDownLatch bothRead = new CountDownLatch(2);                          // obaj przeczytali
        Runnable badIncrement = () -> {
            Integer current = visits.get("strona");    // obaj czytają 0
            bothRead.countDown();
            await(bothRead);                           // czekamy, aż drugi też przeczyta
            visits.put("strona", current + 1);         // obaj zapisują 0 + 1
        };
        runAll("odwiedzający", badIncrement, badIncrement);
        show("2 odwiedziny, get+put → licznik", visits.get("strona"));
        // WYNIK: 2 odwiedziny, get+put → licznik → 1
        note("jedna aktualizacja zgubiona (lost update) — mimo ConcurrentHashMap");
        // WYNIK: ℹ jedna aktualizacja zgubiona (lost update) — mimo ConcurrentHashMap

        // Bez wymuszania: 4 wątki × 10 000 razy get+put. Ile zginie — zależy od uruchomienia
        // (często tysiące, czasem 0). Wypisujemy tylko FAKT, który jest pewny.
        ConcurrentHashMap<String, Integer> racy = new ConcurrentHashMap<>();
        racy.put("x", 0);
        Runnable racyLoop = () -> {
            for (int i = 0; i < 10_000; i++) {
                racy.put("x", racy.get("x") + 1);
            }
        };
        runAll("wyścig", racyLoop, racyLoop, racyLoop, racyLoop);
        show("get+put: wynik ≤ 40000", racy.get("x") <= 40_000);
        // WYNIK: get+put: wynik ≤ 40000 → true

        // Naprawa: JEDNA atomowa operacja.
        ConcurrentHashMap<String, Integer> safe = new ConcurrentHashMap<>();
        Runnable mergeLoop = () -> {
            for (int i = 0; i < 10_000; i++) {
                safe.merge("x", 1, Integer::sum);
            }
        };
        runAll("merge", mergeLoop, mergeLoop, mergeLoop, mergeLoop);
        show("merge: wynik", safe.get("x"));
        // WYNIK: merge: wynik → 40000

        // DOBRA PRAKTYKA: przy bardzo gorących licznikach (wiele wątków, ten sam klucz) użyj
        // ConcurrentHashMap<String, LongAdder> i computeIfAbsent(k, x -> new LongAdder()).increment().
        // Dlaczego: LongAdder rozkłada zapisy na kilka komórek, więc wątki mniej ze sobą rywalizują.
        ConcurrentHashMap<String, LongAdder> adders = new ConcurrentHashMap<>();
        Runnable adderLoop = () -> {
            for (int i = 0; i < 10_000; i++) {
                adders.computeIfAbsent("x", k -> new LongAdder()).increment();   // increment = zwiększ o 1
            }
        };
        runAll("adder", adderLoop, adderLoop, adderLoop, adderLoop);
        show("LongAdder: sum()", adders.get("x").sum());
        // WYNIK: LongAdder: sum() → 40000
    }

    // =================================================================================================
    // 4. RÓWNOLEGŁE LICZENIE SŁÓW
    // =================================================================================================

    /**
     * 4. Realny przykład: N zadań liczy słowa z różnych kawałków danych do wspólnej mapy. Wynik jest
     * deterministyczny, bo merge jest atomowe — a kolejność wydruku ustala TreeMap.
     */
    static void parallelWordCount() {
        section("4. Liczenie słów przez 4 zadania do jednej ConcurrentHashMap");

        List<String> words = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            words.addAll(SampleData.words());          // 12 słów × 100 = 1200
        }
        ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();
        ExecutorService pool = namedPool("liczacy", 4);
        try {
            List<Future<?>> futures = new ArrayList<>();
            int chunk = words.size() / 4;              // chunk = kawałek (300 słów)
            for (int t = 0; t < 4; t++) {
                List<String> part = words.subList(t * chunk, (t + 1) * chunk);   // subList = podlista (tylko odczyt tutaj)
                futures.add(pool.submit(() -> part.forEach(w -> counts.merge(w, 1, Integer::sum))));
            }
            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);           // czekamy na każde zadanie (i ujawniamy ewentualny wyjątek)
            }
        } catch (Exception e) {
            show("błąd", e);
        } finally {
            shutdownAndAwait(pool);
        }
        // PUŁAPKA: nie wypisuj samej ConcurrentHashMap — kolejność iteracji jest nieokreślona.
        // TreeMap = mapa posortowana po kluczu → zawsze ta sama kolejność.
        show("liczności (TreeMap)", new TreeMap<>(counts));
        // WYNIK: liczności (TreeMap) → {enum=100, java=300, kolekcja=100, lambda=100, lista=100, mapa=100, optional=100, rekord=100, stream=200}
        show("suma liczności", counts.values().stream().mapToInt(Integer::intValue).sum());
        // WYNIK: suma liczności → 1200

        // DOBRA PRAKTYKA: dzieląc dane między zadania, dawaj każdemu ROZŁĄCZNY kawałek (tu subList) i nie zmieniaj
        // listy źródłowej w trakcie. Dlaczego: wtedy jedynym współdzielonym stanem jest mapa, którą chroni merge.
        // W Springu tak samo: pole-cache w singletonie (@Service) to zwykle ConcurrentHashMap, bo każde żądanie
        // HTTP obsługuje inny wątek puli serwera.
    }

    // =================================================================================================
    // 5. SŁABO SPÓJNE ITERATORY
    // =================================================================================================

    /**
     * 5. Iteratory kolekcji współbieżnych są „weakly consistent” (słabo spójne): nigdy nie rzucają
     * ConcurrentModificationException, pokazują stan z chwili utworzenia LUB późniejszy — bez gwarancji,
     * czy zobaczą zmiany wprowadzone w trakcie.
     */
    static void weaklyConsistentIterators() {
        section("5. Słabo spójne iteratory i size() jako przybliżenie");

        ConcurrentHashMap<String, Integer> prices = new ConcurrentHashMap<>();
        prices.put("a", 1);
        prices.put("b", 2);
        prices.put("c", 3);
        prices.put("d", 4);

        // Usuwanie w trakcie iteracji — w ConcurrentHashMap dozwolone, bez wyjątku.
        for (String key : prices.keySet()) {           // keySet = zbiór kluczy (widok mapy)
            if (prices.get(key) % 2 == 0) {
                prices.remove(key);
            }
        }
        show("po usunięciu parzystych w pętli", new TreeMap<>(prices));
        // WYNIK: po usunięciu parzystych w pętli → {a=1, c=3}

        // Dodawanie w trakcie iteracji — też bez wyjątku, ale CZY pętla zobaczy nowy klucz, nie wiadomo.
        int visited = 0;                               // visited = odwiedzone
        for (String key : prices.keySet()) {
            if (!key.endsWith("-kopia")) {             // endsWith = kończy się na; nowe klucze pomijamy
                prices.putIfAbsent(key + "-kopia", 0);
            }
            visited++;
        }
        show("pętla skończyła się bez wyjątku", visited >= 2);
        // WYNIK: pętla skończyła się bez wyjątku → true
        // Ile razy obróciła się pętla (2? 3? 4?) — zależy od rozmieszczenia kluczy w tablicy; nie polegaj na tym.

        // PUŁAPKA: size(), isEmpty() i containsValue() w trakcie równoległych zmian to tylko PRZYBLIŻENIE.
        // Dlaczego: inne wątki zmieniają mapę w tej samej chwili; Javadoc mówi, że te wyniki mają sens
        // głównie wtedy, gdy mapa nie jest akurat zmieniana. Tu wszystko już stoi, więc liczba jest dokładna.
        show("size() po zakończeniu zmian", prices.size());
        // WYNIK: size() po zakończeniu zmian → 4
        show("mappingCount() (long)", prices.mappingCount());   // mapping count = liczba odwzorowań
        // WYNIK: mappingCount() (long) → 4

        // PUŁAPKA: decyzja „if (map.size() < LIMIT) map.put(...)” przy wielu wątkach to znowu check-then-act —
        // limit zostanie przekroczony. Licz limit atomowo (np. Semaphore z t21_concurrency/Concurrency07Synchronizers).
    }

    // =================================================================================================
    // 6. COPYONWRITEARRAYLIST
    // =================================================================================================

    /**
     * 6. {@code CopyOnWriteArrayList} przy każdym zapisie kopiuje całą tablicę. Odczyt i iteracja nie blokują
     * i iterują po MIGAWCE. Idealna dla list rzadko zmienianych, a często czytanych — np. słuchaczy zdarzeń.
     */
    static void copyOnWriteList() {
        section("6. CopyOnWriteArrayList — migawka przy iteracji");

        List<String> listeners = new CopyOnWriteArrayList<>(List.of("log", "mail", "sms"));  // listeners = słuchacze
        int loops = 0;
        for (String l : listeners) {                   // iterator widzi migawkę z chwili startu pętli
            listeners.add(l + "-2");                   // dopisuje do NOWEJ kopii tablicy
            loops++;
        }
        show("obroty pętli (migawka 3 elementów)", loops);
        // WYNIK: obroty pętli (migawka 3 elementów) → 3
        show("lista po pętli", listeners);
        // WYNIK: lista po pętli → [log, mail, sms, log-2, mail-2, sms-2]

        // Dla porównania zwykła ArrayList — iterator fail-fast rzuca wyjątek.
        List<String> plain = new ArrayList<>(List.of("log", "mail", "sms"));
        expectThrows("ArrayList: add w for-each", () -> {
            for (String l : plain) {
                plain.add(l + "-2");
            }
        });
        // WYNIK: ✔ ArrayList: add w for-each → rzucono ConcurrentModificationException: (brak komunikatu)

        // PUŁAPKA: iterator migawki nie pozwala zmieniać listy przez siebie (remove/set/add).
        Iterator<String> it = listeners.iterator();
        it.next();
        expectThrows("iterator.remove() na CopyOnWriteArrayList", it::remove);
        // WYNIK: ✔ iterator.remove() na CopyOnWriteArrayList → rzucono UnsupportedOperationException: (brak komunikatu)
        listeners.removeIf(s -> s.endsWith("-2"));     // removeIf = usuń, jeśli — jedna operacja na liście, działa
        show("po removeIf", listeners);
        // WYNIK: po removeIf → [log, mail, sms]

        // Równoległe dopisywanie też jest poprawne (każde add pod wewnętrzną blokadą), ale kosztowne.
        List<Integer> events = new CopyOnWriteArrayList<>();
        Runnable add500 = () -> {
            for (int i = 0; i < 500; i++) {
                events.add(i);
            }
        };
        runAll("zdarzenia", add500, add500);
        show("2 wątki × 500 add → rozmiar", events.size());
        // WYNIK: 2 wątki × 500 add → rozmiar → 1000

        // DOBRA PRAKTYKA: CopyOnWriteArrayList tylko gdy zapisów jest MAŁO. Dlaczego: każde add kopiuje całą
        // tablicę — 1000 dodań do listy 1000-elementowej to ok. miliona kopiowanych referencji. Przy częstych
        // zapisach wybierz ConcurrentLinkedQueue albo kolejkę blokującą.
    }

    // =================================================================================================
    // 7. BLOCKINGQUEUE — CZTERY RODZINY METOD
    // =================================================================================================

    /**
     * 7. {@code BlockingQueue} ma cztery warianty każdej operacji: rzuca wyjątek, zwraca wartość specjalną,
     * blokuje bez końca, blokuje z limitem czasu. Pokazujemy je na kolejce o pojemności 2.
     * <pre>
     *            | rzuca wyjątek | wartość specjalna | blokuje | limit czasu
     *   wstaw    | add(e)        | offer(e) → false  | put(e)  | offer(e, czas, jedn.)
     *   pobierz  | remove()      | poll() → null     | take()  | poll(czas, jedn.)
     *   podejrzyj| element()     | peek() → null     |   —     |   —
     * </pre>
     */
    static void blockingQueueMethods() {
        section("7. BlockingQueue — add/offer/put i remove/poll/take");

        // ArrayBlockingQueue = kolejka blokująca na tablicy, ZAWSZE ograniczona pojemnością (capacity = pojemność)
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(2);
        try {
            queue.put("paczka-1");                     // put = włóż (czekaj, gdy pełna) — tu jest miejsce
            show("offer(paczka-2)", queue.offer("paczka-2"));   // offer = zaoferuj (false, gdy pełna)
            // WYNIK: offer(paczka-2) → true
            show("offer(paczka-3) przy pełnej", queue.offer("paczka-3"));
            // WYNIK: offer(paczka-3) przy pełnej → false
            show("offer z limitem 50 ms przy pełnej", queue.offer("paczka-3", 50, TimeUnit.MILLISECONDS));
            // WYNIK: offer z limitem 50 ms przy pełnej → false
            expectThrows("add przy pełnej", () -> queue.add("paczka-3"));
            // WYNIK: ✔ add przy pełnej → rzucono IllegalStateException: Queue full
            show("remainingCapacity()", queue.remainingCapacity());    // remaining capacity = pozostała pojemność
            // WYNIK: remainingCapacity() → 0

            show("take()", queue.take());              // take = weź (czekaj, gdy pusta)
            // WYNIK: take() → paczka-1
            show("poll()", queue.poll());              // poll = pobierz (null, gdy pusta)
            // WYNIK: poll() → paczka-2
            show("poll() przy pustej", queue.poll());
            // WYNIK: poll() przy pustej → null
            show("poll z limitem 50 ms przy pustej", queue.poll(50, TimeUnit.MILLISECONDS));
            // WYNIK: poll z limitem 50 ms przy pustej → null
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // PUŁAPKA: kolejki blokujące nie przyjmują null. Dlaczego: null to „wartość specjalna” z poll() = „pusto”.
        expectThrows("offer(null)", () -> queue.offer(null));
        // WYNIK: ✔ offer(null) → rzucono NullPointerException: (brak komunikatu)

        // PUŁAPKA: new LinkedBlockingQueue<>() bez argumentu ma pojemność Integer.MAX_VALUE — praktycznie
        // NIEOGRANICZONĄ. Gdy producent jest szybszy od konsumenta, kolejka rośnie, aż zabraknie pamięci.
        BlockingQueue<String> unbounded = new LinkedBlockingQueue<>();
        show("LinkedBlockingQueue() — pojemność = MAX_VALUE", unbounded.remainingCapacity() == Integer.MAX_VALUE);
        // WYNIK: LinkedBlockingQueue() — pojemność = MAX_VALUE → true
        // DOBRA PRAKTYKA: podawaj pojemność: new LinkedBlockingQueue<>(1000). Dlaczego: pełna kolejka zatrzymuje
        // producenta (put czeka) — to naturalny „hamulec” (backpressure = przeciwciśnienie).
    }

    // =================================================================================================
    // 8. PRODUCENT–KONSUMENT Z TRUJĄCĄ PIGUŁKĄ
    // =================================================================================================

    static final int POISON = -1;   // POISON = trucizna — znacznik „koniec pracy”

    /**
     * 8. Producent wkłada liczby do kolejki, dwóch konsumentów je pobiera. Koniec sygnalizuje „trująca pigułka”
     * — specjalna wartość, po której konsument kończy pętlę. JEDNA pigułka na KAŻDEGO konsumenta.
     */
    static void producerConsumer() {
        section("8. Producent–konsument z trującą pigułką");

        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);       // mała pojemność → producent czasem czeka
        ConcurrentLinkedQueue<Integer> processed = new ConcurrentLinkedQueue<>();   // processed = przetworzone
        LongAdder sum = new LongAdder();
        int consumers = 2;

        Runnable producer = () -> {                    // producer = producent
            try {
                for (int i = 1; i <= 20; i++) {
                    queue.put(i);                      // czeka, gdy w kolejce już 5 elementów
                }
                for (int c = 0; c < consumers; c++) {
                    queue.put(POISON);                 // po jednej pigułce dla każdego konsumenta
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        Runnable consumer = () -> {                    // consumer = konsument
            try {
                while (true) {
                    int item = queue.take();           // czeka, gdy pusto
                    if (item == POISON) {
                        return;                        // pigułka → koniec tego konsumenta
                    }
                    processed.add(item * 10);
                    sum.add(item * 10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        runAll("taśma", producer, consumer, consumer);

        // Który konsument wziął którą liczbę — zależy od uruchomienia. Wypisujemy więc fakty niezależne od przeplotu.
        show("przetworzonych elementów", processed.size());
        // WYNIK: przetworzonych elementów → 20
        show("suma", sum.sum());
        // WYNIK: suma → 2100
        show("pierwsze 5 po posortowaniu", processed.stream().sorted().limit(5).toList());
        // WYNIK: pierwsze 5 po posortowaniu → [10, 20, 30, 40, 50]
        show("kolejka pusta na końcu", queue.isEmpty());
        // WYNIK: kolejka pusta na końcu → true

        // PUŁAPKA: za mało pigułek = konsument wisi w take() na zawsze, a join() w main razem z nim.
        // Dlaczego: take() czeka bez limitu. W produkcji dodaj zabezpieczenie: poll(czas, jednostka) i obsługę null.
        // PUŁAPKA: nie używaj null jako pigułki — kolejka i tak go odrzuci (sekcja 7).
        // DOBRA PRAKTYKA: zamiast ręcznej pary kolejka + wątki zwykle wystarczy ExecutorService — on ma w środku
        // dokładnie taką kolejkę zadań i wątki-konsumentów (t21_concurrency/Concurrency04Executors).
    }

    // =================================================================================================
    // 9. CONCURRENTLINKEDQUEUE I CONCURRENTSKIPLISTMAP
    // =================================================================================================

    /**
     * 9. Kolekcje nieblokujące (lock-free, oparte na CAS = compare-and-set, porównaj i ustaw):
     * {@code ConcurrentLinkedQueue} (kolejka bez limitu) i {@code ConcurrentSkipListMap} — współbieżny
     * odpowiednik TreeMap, zawsze posortowany.
     */
    static void nonBlockingAndSorted() {
        section("9. ConcurrentLinkedQueue i ConcurrentSkipListMap");

        ConcurrentLinkedQueue<String> log = new ConcurrentLinkedQueue<>();
        Runnable writer = () -> {
            String name = Thread.currentThread().getName();
            for (int i = 0; i < 1000; i++) {
                log.offer(name + ":" + i);             // offer w kolejce bez limitu zawsze zwraca true
            }
        };
        runAll("pisarz", writer, writer, writer);
        show("3 wątki × 1000 offer → rozmiar", log.size());
        // WYNIK: 3 wątki × 1000 offer → rozmiar → 3000
        // PUŁAPKA: size() w ConcurrentLinkedQueue NIE jest O(1) — przechodzi całą kolejkę, a przy równoległych
        // zmianach bywa niedokładne. Dlaczego: brak wspólnego licznika (to by spowalniało zapisy). Zamiast
        // „size() == 0” używaj isEmpty().

        // ConcurrentSkipListMap = współbieżna mapa posortowana (skip list = lista z przeskokami).
        ConcurrentSkipListMap<Integer, String> ranking = new ConcurrentSkipListMap<>();
        Runnable evens = () -> {
            for (int i = 0; i < 100; i += 2) {
                ranking.put(i, "parzysta");
            }
        };
        Runnable odds = () -> {
            for (int i = 1; i < 100; i += 2) {
                ranking.put(i, "nieparzysta");
            }
        };
        runAll("ranking", evens, odds);
        show("rozmiar", ranking.size());
        // WYNIK: rozmiar → 100
        show("firstKey / lastKey", ranking.firstKey() + " / " + ranking.lastKey());   // pierwszy / ostatni klucz
        // WYNIK: firstKey / lastKey → 0 / 99
        show("headMap(3)", ranking.headMap(3));        // headMap = mapa „głowy” (klucze < 3)
        // WYNIK: headMap(3) → {0=parzysta, 1=nieparzysta, 2=parzysta}
        show("ceilingKey(50)", ranking.ceilingKey(50));   // ceiling = sufit: najmniejszy klucz ≥ 50
        // WYNIK: ceilingKey(50) → 50
        // Wydruk ConcurrentSkipListMap jest deterministyczny — kolejność wynika z sortowania, nie z haszy.

        // DOBRA PRAKTYKA: dobieraj kolekcję do wzorca dostępu:
        //   • mapa, wiele wątków                → ConcurrentHashMap (+ merge/compute zamiast get+put)
        //   • mapa posortowana, wiele wątków    → ConcurrentSkipListMap (zbiór: ConcurrentSkipListSet)
        //   • lista czytana często, zmieniana rzadko (słuchacze) → CopyOnWriteArrayList
        //   • przekazywanie pracy między wątkami → BlockingQueue z pojemnością (ArrayBlockingQueue)
        //   • kolejka bez czekania, bez limitu  → ConcurrentLinkedQueue
        //   • Collections.synchronizedXxx      → tylko dla starego kodu; złożone operacje i tak pod synchronized
        // Dlaczego: każda z nich jest zoptymalizowana pod inny wzorzec dostępu — zła kolekcja działa, ale wolno.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Bezpieczna wątkowo kolekcja = bezpieczna POJEDYNCZA operacja. get+put, contains+add to nadal wyścig.
     *   • synchronizedList/Map: każda metoda pod blokadą, iteracja i operacje złożone — ręcznie w synchronized(list).
     *   • ConcurrentHashMap: putIfAbsent, computeIfAbsent, compute, merge są atomowe; brak null (klucz i wartość);
     *     iteratory słabo spójne (bez CME); size() przy zmianach = przybliżenie; wydruk → new TreeMap<>(map).
     *   • Wartość-kolekcja w mapie współbieżnej sama musi być bezpieczna wątkowo.
     *   • CopyOnWriteArrayList: tanie odczyty, drogie zapisy (kopia tablicy), iterator = migawka, bez remove().
     *   • BlockingQueue: add/remove (wyjątek), offer/poll (false/null), put/take (czeka), offer/poll z czasem.
     *   • ArrayBlockingQueue zawsze ograniczona; LinkedBlockingQueue() domyślnie ~nieograniczona — podaj pojemność.
     *   • Producent–konsument: jedna trująca pigułka na każdego konsumenta; null niedozwolony.
     *   • ConcurrentLinkedQueue: nieblokująca, size() to O(n). ConcurrentSkipListMap: posortowana, współbieżna.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego Collections.synchronizedList nie chroni operacji „if (!list.contains(x)) list.add(x)”?
     *   2. Co wypisze (jeden wątek):
     *        Map<String, Integer> m = new ConcurrentHashMap<>();
     *        System.out.println(m.putIfAbsent("a", 1) + " " + m.putIfAbsent("a", 2) + " " + m.get("a"));
     *   3. ZNAJDŹ BŁĄD (licznik odsłon wołany z wielu wątków):
     *        Integer old = hits.get(url);
     *        hits.put(url, old == null ? 1 : old + 1);
     *   4. Dlaczego ConcurrentHashMap nie pozwala na wartości null?
     *   5. Co wypisze:
     *        List<String> l = new CopyOnWriteArrayList<>(List.of("x", "y"));
     *        for (String s : l) { l.add(s); }
     *        System.out.println(l.size());
     *   6. Czym różnią się offer(e), put(e) i add(e) w pełnej ArrayBlockingQueue?
     *   7. ZNAJDŹ BŁĄD: 3 konsumentów, producent na końcu wkłada JEDNĄ trującą pigułkę, main robi join() na wszystkich.
     *   8. Kiedy CopyOnWriteArrayList jest złym wyborem?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Map<String, Integer> expectedWords = new TreeMap<>(Map.of(
                "enum", 1, "java", 3, "kolekcja", 1, "lambda", 1, "lista", 1,
                "mapa", 1, "optional", 1, "rekord", 1, "stream", 2));
        Map<Category, Integer> expectedStock = new TreeMap<>(Map.of(
                Category.ELEKTRONIKA, 36, Category.SPOZYWCZE, 420, Category.KSIAZKI, 27,
                Category.ODZIEZ, 92, Category.DOM, 20));
        Check.equal("ćw. 1: liczenie słów przez 3 zadania", expectedWords, () -> exercise1(SampleData.words(), 3));
        Check.equal("ćw. 2: 4 zadania rejestrują id 0..99 — udanych rejestracji", 100, () -> exercise2(100, 4));
        Check.equal("ćw. 3: suma 1..1000 przez kolejkę i 3 konsumentów", 500500L, () -> exercise3(1000, 3));
        Check.equal("ćw. 4: stan magazynu wg kategorii (3 zadania)", expectedStock, () -> exercise4(SampleData.products(), 3));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", expectedWords, () -> solution1(SampleData.words(), 3));
        Check.equal("ćw. 2 (wzorzec)", 100, () -> solution2(100, 4));
        Check.equal("ćw. 3 (wzorzec)", 500500L, () -> solution3(1000, 3));
        Check.equal("ćw. 4 (wzorzec)", expectedStock, () -> solution4(SampleData.products(), 3));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz wystąpienia słów, dzieląc listę na {@code tasks} kawałków; każdy kawałek
     * przetwarza osobne zadanie w puli (namedPool). Zwróć wynik jako TreeMap (posortowany).
     * Podpowiedź: wspólna ConcurrentHashMap i merge(w, 1, Integer::sum); kawałek i: od i*n/tasks do (i+1)*n/tasks;
     * pamiętaj o Future.get() i shutdownAndAwait w finally.
     */
    static Map<String, Integer> exercise1(List<String> words, int tasks) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ rejestrację z „sprawdź, potem wstaw” na jedną operację atomową.
     * Każde z {@code tasks} zadań próbuje zarejestrować WSZYSTKIE id od 0 do ids-1. Policz, ile prób
     * się udało (powinno być dokładnie ids — każde id raz, niezależnie od przeplotu). Stary, błędny kod:
     * <pre>{@code
     * if (!registry.containsKey(id)) {      // dwa wątki mogą oba zobaczyć false
     *     registry.put(id, name);
     *     successes.incrementAndGet();
     * }
     * }</pre>
     * Podpowiedź: putIfAbsent zwraca null tylko temu wątkowi, który faktycznie wstawił.
     */
    static int exercise2(int ids, int tasks) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): producent wkłada do ArrayBlockingQueue(10) liczby 1..n, a {@code consumers}
     * konsumentów je sumuje. Zwróć sumę (long).
     * Podpowiedź: wątek-producent + wątki-konsumenci (runAll), LongAdder na sumę, na końcu tyle pigułek POISON,
     * ilu konsumentów.
     */
    static long exercise3(int n, int consumers) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): łącząc z t12_collections/Collections05Maps — policz łączny stan magazynu
     * (stock) w każdej kategorii. Produkty podziel między {@code tasks} zadań w puli; każde dopisuje do
     * wspólnej ConcurrentHashMap. Zwróć mapę posortowaną po kategorii (TreeMap — kolejność stałych enuma).
     * Podpowiedź: merge(p.category(), p.stock(), Integer::sum); na końcu new TreeMap<>(mapa).
     */
    static Map<Category, Integer> exercise4(List<Product> products, int tasks) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Map<String, Integer> solution1(List<String> words, int tasks) {
        ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();
        ExecutorService pool = namedPool("cw1", tasks);
        try {
            List<Future<?>> futures = new ArrayList<>();
            int n = words.size();
            for (int i = 0; i < tasks; i++) {
                List<String> part = words.subList(i * n / tasks, (i + 1) * n / tasks);
                futures.add(pool.submit(() -> part.forEach(w -> counts.merge(w, 1, Integer::sum))));
            }
            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            shutdownAndAwait(pool);
        }
        return new TreeMap<>(counts);
    }

    static int solution2(int ids, int tasks) {
        ConcurrentHashMap<Integer, String> registry = new ConcurrentHashMap<>();
        AtomicInteger successes = new AtomicInteger();
        Runnable[] workers = new Runnable[tasks];
        for (int t = 0; t < tasks; t++) {
            String name = "zadanie-" + t;
            workers[t] = () -> {
                for (int id = 0; id < ids; id++) {
                    if (registry.putIfAbsent(id, name) == null) {   // null = to MY wstawiliśmy
                        successes.incrementAndGet();
                    }
                }
            };
        }
        runAll("cw2", workers);
        return successes.get();
    }

    static long solution3(int n, int consumers) {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
        LongAdder sum = new LongAdder();
        Runnable[] tasks = new Runnable[consumers + 1];
        tasks[0] = () -> {
            try {
                for (int i = 1; i <= n; i++) {
                    queue.put(i);
                }
                for (int c = 0; c < consumers; c++) {
                    queue.put(POISON);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        for (int c = 1; c <= consumers; c++) {
            tasks[c] = () -> {
                try {
                    for (int item = queue.take(); item != POISON; item = queue.take()) {
                        sum.add(item);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };
        }
        runAll("cw3", tasks);
        return sum.sum();
    }

    static Map<Category, Integer> solution4(List<Product> products, int tasks) {
        ConcurrentHashMap<Category, Integer> stock = new ConcurrentHashMap<>();
        ExecutorService pool = namedPool("cw4", tasks);
        try {
            List<Future<?>> futures = new ArrayList<>();
            int n = products.size();
            for (int i = 0; i < tasks; i++) {
                List<Product> part = products.subList(i * n / tasks, (i + 1) * n / tasks);
                futures.add(pool.submit(() -> part.forEach(p -> stock.merge(p.category(), p.stock(), Integer::sum))));
            }
            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            shutdownAndAwait(pool);
        }
        return new TreeMap<>(stock);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo to DWIE osobne operacje, każda pod blokadą osobno. Między contains a add blokada jest zwolniona
     *      i inny wątek może wykonać to samo sprawdzenie → duplikat. Trzeba objąć obie synchronized(list).
     *   2. „null 1 1” — pierwsze putIfAbsent wstawia i zwraca poprzednią wartość (null), drugie nic nie zmienia
     *      i zwraca obecną (1).
     *   3. get i put to osobne operacje (read-modify-write) — dwa wątki przeczytają tę samą wartość i jedna odsłona
     *      zginie. Poprawnie: hits.merge(url, 1, Integer::sum).
     *   4. Żeby wynik get(k) == null jednoznacznie znaczył „brak klucza”. Przy wielu wątkach nie da się bezpiecznie
     *      dopytać containsKey (między wywołaniami mapa może się zmienić), więc null jako wartość byłby niejasny.
     *   5. 4 — iterator widzi migawkę z 2 elementów, więc pętla wykona się 2 razy i doda 2 elementy.
     *   6. offer(e) od razu zwraca false, put(e) czeka, aż zwolni się miejsce, add(e) rzuca
     *      IllegalStateException („Queue full”).
     *   7. Pigułkę zje tylko jeden konsument; dwaj pozostali wiszą w take() na zawsze, więc join() nigdy się nie
     *      skończy. Trzeba włożyć 3 pigułki (po jednej na konsumenta) albo używać poll z limitem czasu.
     *   8. Gdy zapisów jest dużo albo lista jest duża — każdy zapis kopiuje całą tablicę (czas i pamięć).
     *      Dobra jest dla małych list czytanych często, a zmienianych rzadko (np. słuchacze zdarzeń).
     */
    // </editor-fold>
}
