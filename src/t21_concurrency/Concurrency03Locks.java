package t21_concurrency;

import helpers.Check;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Blokady — synchronized, ReentrantLock, Condition, ReadWriteLock, StampedLock, wait/notify, zakleszczenie
 *        (lock = blokada / zamek, reentrant = wielowejściowy, condition = warunek, deadlock = zakleszczenie)
 *
 * W SKRÓCIE:
 *   synchronized wystarcza w większości przypadków. Jawne blokady z pakietu java.util.concurrent.locks dają więcej:
 *   próbę bez czekania (tryLock), czekanie z limitem, przerywalne czekanie, kilka warunków (Condition), osobne
 *   blokady dla czytelników i pisarzy. Ceną jest obowiązek ręcznego unlock() w finally.
 *   Najgroźniejszy błąd blokad to zakleszczenie: wątki czekają na siebie nawzajem w nieskończoność.
 *
 * ANALOGIA:
 *   Blokada to klucz do łazienki na stacji benzynowej: kto ma klucz, ten wchodzi, reszta czeka w kolejce.
 *   tryLock = „zajrzę, czy klucz wisi; jak nie, idę dalej”. Condition = poczekalnia z tabliczką „wołamy, gdy
 *   zwolni się miejsce”. Zakleszczenie = dwie osoby w wąskim korytarzu, każda czeka, aż druga się cofnie.
 *
 * JAK TO DZIAŁA:
 *   • synchronized (blokada wbudowana) i ReentrantLock są WIELOWEJŚCIOWE: wątek, który już ma blokadę, może wejść
 *     ponownie (licznik wejść rośnie; blokada zwalnia się, gdy licznik spadnie do zera).
 *   • Wzorzec ReentrantLock — ZAWSZE tak:
 *         lock.lock();
 *         try { ...sekcja krytyczna... } finally { lock.unlock(); }
 *   • Porównanie:
 *         potrzeba                         synchronized        ReentrantLock
 *         zwykłe wykluczanie               tak                 tak
 *         próba bez czekania / z limitem   nie                 tryLock() / tryLock(czas)
 *         przerywalne czekanie             nie                 lockInterruptibly()
 *         kilka kolejek oczekujących       nie (jedna)         newCondition() wiele razy
 *         uczciwość (kolejność FIFO)       nie                 new ReentrantLock(true)
 *         automatyczne zwolnienie          tak (wyjście)       NIE — unlock() w finally
 *   • Zakleszczenie wymaga 4 warunków naraz (Coffman): wzajemne wykluczanie, trzymanie i czekanie, brak wywłaszczenia,
 *     cykl oczekiwania. Usuń jeden (najłatwiej: cykl — stała kolejność brania blokad) i zakleszczenia nie ma.
 *
 * SŁÓWKA:
 *   lock = blokada (zamknij); unlock = odblokuj; reentrant = wielowejściowy; try lock = spróbuj zablokować;
 *   interruptibly = w sposób przerywalny; fair = uczciwy; condition = warunek; await = czekaj; signal = zasygnalizuj;
 *   read/write lock = blokada odczytu/zapisu; stamped = ze stemplem (znacznikiem); optimistic = optymistyczny;
 *   validate = sprawdź poprawność; wait = czekaj; notify = powiadom; monitor = monitor (blokada obiektu);
 *   deadlock = zakleszczenie; livelock = uwięzienie w ruchu; starvation = zagłodzenie; hold count = liczba wejść.
 *
 * ZOBACZ TEŻ: t21_concurrency/Concurrency02RaceConditions (po co blokady, synchronized),
 *             t21_concurrency/Concurrency06ConcurrentCollections (BlockingQueue — gotowy bufor z tej lekcji),
 *             t21_concurrency/Concurrency07Synchronizers (CountDownLatch, Semaphore, CyclicBarrier),
 *             t21_concurrency/Concurrency10MemoryModel (blokada daje też widoczność — happens-before).
 * </pre>
 */
public class Concurrency03Locks {

    public static void main(String[] args) throws Exception {
        title("Concurrency03 — blokady, warunki i zakleszczenie");

        reentrancy();             // reentrancy = wielowejściowość
        reentrantLockBasics();    // reentrant lock basics = podstawy ReentrantLock
        tryLockDemo();            // try lock demo = próba zablokowania
        interruptibleAndFair();   // interruptible and fair = przerywalne i uczciwe
        conditionBuffer();        // condition buffer = bufor z warunkami
        readWriteLock();          // read write lock = blokada odczytu i zapisu
        stampedLockOptimistic();  // stamped lock optimistic = optymistyczny odczyt StampedLock
        waitNotifyClassic();      // wait notify classic = klasyczne wait/notify
        deadlock();               // deadlock = zakleszczenie
        livelockAndStarvation();  // livelock and starvation = uwięzienie w ruchu i zagłodzenie
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. WIELOWEJŚCIOWOŚĆ BLOKADY WBUDOWANEJ
    // =================================================================================================

    /** Klasa z dwiema metodami synchronized, z których jedna woła drugą. */
    static class Account {   // account = konto
        private int balance; // balance = saldo

        synchronized void deposit(int amount) {   // deposit = wpłać
            balance += amount;
        }

        synchronized void depositTwice(int amount) {   // wpłać dwa razy — woła inną metodę synchronized
            deposit(amount);   // ten sam wątek bierze tę samą blokadę drugi raz — wolno (wielowejściowość)
            deposit(amount);
        }

        synchronized boolean holdsOwnLock() {   // czy trzymam własną blokadę
            return Thread.holdsLock(this);      // holdsLock = czy trzyma blokadę (tylko bieżący wątek)
        }

        synchronized int balance() {
            return balance;
        }
    }

    /**
     * 1. Gdyby blokady nie były wielowejściowe, metoda synchronized wołająca inną metodę synchronized tego samego obiektu
     * zakleszczyłaby się sama ze sobą. W Javie i synchronized, i ReentrantLock pozwalają wejść ponownie.
     */
    static void reentrancy() {
        section("1. Wielowejściowość (reentrancy)");

        Account account = new Account();
        account.depositTwice(50);
        show("saldo po depositTwice(50)", account.balance());
        // WYNIK: saldo po depositTwice(50) → 100
        show("wewnątrz metody synchronized holdsLock(this)", account.holdsOwnLock());
        // WYNIK: wewnątrz metody synchronized holdsLock(this) → true
        show("poza nią holdsLock(account)", Thread.holdsLock(account));
        // WYNIK: poza nią holdsLock(account) → false

        // ReentrantLock liczy wejścia jawnie: getHoldCount = liczba wejść bieżącego wątku.
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        lock.lock();   // drugie wejście tego samego wątku
        show("getHoldCount() po dwóch lock()", lock.getHoldCount());
        // WYNIK: getHoldCount() po dwóch lock() → 2
        lock.unlock();
        show("po jednym unlock() wciąż zablokowana?", lock.isLocked());   // isLocked = czy zablokowana
        // WYNIK: po jednym unlock() wciąż zablokowana? → true
        lock.unlock();
        show("po drugim unlock()", lock.isLocked());
        // WYNIK: po drugim unlock() → false
        // PUŁAPKA: każde lock() wymaga swojego unlock() — dlaczego: blokada zwalnia się dopiero, gdy licznik wejść
        // spadnie do zera. Jedno brakujące unlock() = blokada trzymana na zawsze, inne wątki czekają w nieskończoność.
    }

    // =================================================================================================
    // 2. ReentrantLock — lock() / unlock() w finally
    // =================================================================================================

    /** Licznik chroniony jawną blokadą. */
    static class LockedCounter {   // locked counter = licznik pod blokadą
        private final Lock lock = new ReentrantLock();
        private int count;

        void increment() {
            lock.lock();          // NIE wewnątrz try — jeśli lock() rzuci, nie wolno wołać unlock()
            try {
                count++;
            } finally {
                lock.unlock();    // ZAWSZE w finally — także gdy sekcja krytyczna rzuci wyjątek
            }
        }

        int get() {
            lock.lock();
            try {
                return count;
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 2. ReentrantLock działa jak synchronized (wykluczanie + widoczność), ale blokadę bierzemy i zwalniamy ręcznie.
     */
    static void reentrantLockBasics() throws InterruptedException {
        section("2. ReentrantLock — lock() i unlock() w finally");

        LockedCounter counter = new LockedCounter();
        runInThreads(2, () -> {
            for (int i = 0; i < 100_000; i++) {
                counter.increment();
            }
        });
        show("licznik z 2 wątków", counter.get());
        // WYNIK: licznik z 2 wątków → 200000

        // PUŁAPKA: unlock() wywołane przez wątek, który blokady NIE trzyma, rzuca wyjątek — dlaczego: ReentrantLock
        // ma właściciela; zwolnić może tylko ten wątek, który zablokował.
        ReentrantLock lock = new ReentrantLock();
        expectThrows("unlock() bez lock()", lock::unlock);
        // WYNIK: ✔ unlock() bez lock() → rzucono IllegalMonitorStateException: (brak komunikatu)

        // PUŁAPKA: zapomniany unlock() (brak finally) — wyjątek w sekcji krytycznej zostawia blokadę zajętą na zawsze.
        // Dlaczego synchronized tego problemu nie ma: blokada wbudowana zwalnia się automatycznie przy wyjściu z bloku,
        // także przez wyjątek. DOBRA PRAKTYKA: jeśli nie potrzebujesz tryLock/Condition/uczciwości — wybierz synchronized.
    }

    // =================================================================================================
    // 3. tryLock() i tryLock(czas)
    // =================================================================================================

    /**
     * 3. {@code tryLock()} nie czeka: zwraca {@code true} (masz blokadę) albo {@code false} (zajęta). Wersja z czasem
     * czeka najwyżej podany czas. Dzięki temu wątek może zrobić coś innego, zamiast stać w kolejce.
     */
    static void tryLockDemo() throws InterruptedException {
        section("3. tryLock() — spróbuj, nie czekaj w nieskończoność");

        ReentrantLock lock = new ReentrantLock();
        boolean[] results = new boolean[2];
        lock.lock();   // main trzyma blokadę przez cały czas działania drugiego wątku
        try {
            Thread other = new Thread(() -> {
                results[0] = lock.tryLock();   // od razu false — blokada zajęta
                try {
                    // tryLock(czas, jednostka) — czeka najwyżej 100 ms; TimeUnit = jednostka czasu
                    results[1] = lock.tryLock(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "próbujący");
            other.start();
            other.join();   // main czeka, wciąż trzymając blokadę — oba tryLock na pewno trafiły na zajętą
        } finally {
            lock.unlock();
        }
        show("tryLock() gdy zajęta", results[0]);
        // WYNIK: tryLock() gdy zajęta → false
        show("tryLock(100 ms) gdy zajęta", results[1]);
        // WYNIK: tryLock(100 ms) gdy zajęta → false

        boolean got = lock.tryLock();
        try {
            show("tryLock() gdy wolna", got);
            // WYNIK: tryLock() gdy wolna → true
        } finally {
            if (got) {
                lock.unlock();   // DOBRA PRAKTYKA: zwalniaj TYLKO wtedy, gdy tryLock zwrócił true
            }
        }
        // Wzorzec użycia:
        //   if (lock.tryLock(1, TimeUnit.SECONDS)) {
        //       try { ...praca... } finally { lock.unlock(); }
        //   } else {
        //       ...plan B: komunikat „spróbuj później”, inna kolejność, ponowienie po chwili...
        //   }
        // PUŁAPKA: unlock() po nieudanym tryLock() rzuci IllegalMonitorStateException — dlaczego: wątek nie jest
        // właścicielem blokady. Dlatego unlock() wołamy tylko w gałęzi „udało się”.
    }

    // =================================================================================================
    // 4. lockInterruptibly() I UCZCIWOŚĆ
    // =================================================================================================

    /**
     * 4. {@code lock()} ignoruje przerwania — wątek czeka dalej, aż dostanie blokadę. {@code lockInterruptibly()}
     * przerywa czekanie wyjątkiem {@code InterruptedException}, gdy ktoś wywoła {@code interrupt()}.
     */
    static void interruptibleAndFair() throws InterruptedException {
        section("4. lockInterruptibly() i blokada uczciwa (fair)");

        ReentrantLock lock = new ReentrantLock();
        String[] outcome = new String[1];   // outcome = rezultat
        lock.lock();
        try {
            Thread waiter = new Thread(() -> {
                try {
                    lock.lockInterruptibly();   // czeka, ale da się przerwać
                    try {
                        outcome[0] = "dostał blokadę (to się tu nie zdarzy)";
                    } finally {
                        lock.unlock();
                    }
                } catch (InterruptedException e) {
                    outcome[0] = "przerwano czekanie: " + e.getClass().getSimpleName();
                    Thread.currentThread().interrupt();
                }
            }, "przerywalny");
            waiter.start();
            waitForState(waiter, Thread.State.WAITING);   // czekający na ReentrantLock jest w stanie WAITING
            show("wątek czeka na blokadę", waiter.getState());
            // WYNIK: wątek czeka na blokadę → WAITING
            show("hasQueuedThreads()", lock.hasQueuedThreads());   // has queued threads = czy ktoś czeka w kolejce
            // WYNIK: hasQueuedThreads() → true
            waiter.interrupt();
            waiter.join();
        } finally {
            lock.unlock();
        }
        show("rezultat", outcome[0]);
        // WYNIK: rezultat → przerwano czekanie: InterruptedException
        // DOBRA PRAKTYKA: lockInterruptibly() w zadaniach, które trzeba umieć anulować (shutdownNow, cancel(true)) —
        // dlaczego: wątek zablokowany w lock() lub w wejściu do synchronized NIE reaguje na interrupt().

        // Uczciwość (fairness): new ReentrantLock(true) przydziela blokadę najdłużej czekającemu wątkowi.
        ReentrantLock fair = new ReentrantLock(true);
        show("new ReentrantLock(true).isFair()", fair.isFair());   // isFair = czy uczciwa
        // WYNIK: new ReentrantLock(true).isFair() → true
        show("new ReentrantLock().isFair()", lock.isFair());
        // WYNIK: new ReentrantLock().isFair() → false
        // Domyślna blokada jest nieuczciwa: wątek, który właśnie przyszedł, może „wyprzedzić” czekających.
        // To zwykle DUŻO szybsze (mniej przełączeń wątków). Uczciwa blokada zmniejsza ryzyko zagłodzenia, ale
        // kosztuje przepustowość. Uwaga z dokumentacji: tryLock() bez czasu NIE respektuje uczciwości — weźmie
        // wolną blokadę nawet wtedy, gdy inni czekają.
    }

    // =================================================================================================
    // 5. CONDITION — BUFOR OGRANICZONY (PRODUCENT–KONSUMENT)
    // =================================================================================================

    /** Bufor o stałej pojemności: put czeka, gdy pełny; take czeka, gdy pusty. */
    static class BoundedBuffer<T> {   // bounded buffer = bufor ograniczony
        private final Deque<T> items = new ArrayDeque<>();   // items = elementy
        private final int capacity;                            // capacity = pojemność
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();    // not full = niepełny (tu czekają producenci)
        private final Condition notEmpty = lock.newCondition();   // not empty = niepusty (tu czekają konsumenci)

        BoundedBuffer(int capacity) {
            this.capacity = capacity;
        }

        void put(T item) throws InterruptedException {
            lock.lock();
            try {
                while (items.size() == capacity) {   // WHILE, nie if — patrz PUŁAPKA w sekcji
                    notFull.await();                 // await = czekaj: ZWALNIA blokadę i usypia wątek
                }
                items.addLast(item);
                notEmpty.signal();                   // signal = zasygnalizuj: obudź JEDNEGO czekającego konsumenta
            } finally {
                lock.unlock();
            }
        }

        T take() throws InterruptedException {
            lock.lock();
            try {
                while (items.isEmpty()) {
                    notEmpty.await();
                }
                T item = items.removeFirst();
                notFull.signal();
                return item;
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 5. {@code Condition} (warunek) to „poczekalnia” związana z blokadą. {@code await()} atomowo zwalnia blokadę
     * i usypia wątek; po obudzeniu wątek ponownie zdobywa blokadę, zanim wróci z {@code await()}.
     */
    static void conditionBuffer() throws InterruptedException {
        section("5. Condition — bufor producent–konsument");

        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(2);   // mały bufor: producent będzie musiał czekać
        List<Integer> consumed = new ArrayList<>();               // consumed = skonsumowane (pisze tylko konsument)
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    buffer.put(i);
                }
                buffer.put(-1);   // „pigułka trucizny” (poison pill): sygnał końca dla konsumenta
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producent");
        Thread consumer = new Thread(() -> {
            try {
                for (int item = buffer.take(); item != -1; item = buffer.take()) {
                    consumed.add(item * 10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "konsument");
        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        show("konsument odebrał (×10)", consumed);
        // WYNIK: konsument odebrał (×10) → [10, 20, 30, 40, 50, 60, 70, 80, 90, 100]
        // Kolejność jest pewna: jeden producent wkłada po kolei, bufor jest kolejką FIFO, jeden konsument wyjmuje.
        // Ile razy producent czekał na miejsce — zależy od uruchomienia, więc tego nie wypisujemy.

        // PUŁAPKA: if zamiast while przy await() — dlaczego źle: (1) dokumentacja Condition dopuszcza „fałszywe
        // obudzenia” (spurious wakeups) bez żadnego signal(); (2) między signal() a ponownym zdobyciem blokady inny wątek
        // mógł już zabrać element. Po obudzeniu warunek trzeba sprawdzić jeszcze raz — stąd pętla while.
        // DOBRA PRAKTYKA: w prawdziwym kodzie użyj gotowej ArrayBlockingQueue (Concurrency06ConcurrentCollections) —
        // jest zbudowana dokładnie tak jak ten bufor, ale przetestowana. Własny bufor piszemy tu, by zrozumieć mechanizm.
    }

    // =================================================================================================
    // 6. ReadWriteLock — WIELU CZYTELNIKÓW, JEDEN PISARZ
    // =================================================================================================

    /**
     * 6. {@code ReentrantReadWriteLock}: blokadę odczytu może trzymać naraz WIELE wątków, blokadę zapisu — tylko jeden
     * i tylko wtedy, gdy nikt nie czyta. Opłaca się, gdy odczytów jest dużo więcej niż zapisów (np. konfiguracja,
     * pamięć podręczna słownika).
     */
    static void readWriteLock() throws InterruptedException {
        section("6. ReadWriteLock — wielu czytelników, jeden pisarz");

        ReadWriteLock rw = new ReentrantReadWriteLock();
        boolean[] results = new boolean[2];
        rw.readLock().lock();   // readLock = blokada odczytu — main „czyta”
        try {
            Thread reader = new Thread(() -> {     // reader = czytelnik
                boolean got = rw.readLock().tryLock();
                results[0] = got;
                if (got) {
                    rw.readLock().unlock();
                }
            }, "czytelnik");
            Thread writer = new Thread(() -> {     // writer = pisarz
                boolean got = rw.writeLock().tryLock();   // writeLock = blokada zapisu
                results[1] = got;
                if (got) {
                    rw.writeLock().unlock();
                }
            }, "pisarz");
            reader.start();
            reader.join();
            writer.start();
            writer.join();
        } finally {
            rw.readLock().unlock();
        }
        show("drugi czytelnik wchodzi, gdy ktoś czyta", results[0]);
        // WYNIK: drugi czytelnik wchodzi, gdy ktoś czyta → true
        show("pisarz wchodzi, gdy ktoś czyta", results[1]);
        // WYNIK: pisarz wchodzi, gdy ktoś czyta → false

        // Typowe użycie — słownik z rzadkimi zmianami:
        PriceList prices = new PriceList();
        prices.update("KSI-001", 79);
        runInThreads(4, () -> prices.get("KSI-001"));   // czytelnicy nie blokują się nawzajem
        show("cena po odczytach z 4 wątków", prices.get("KSI-001"));
        // WYNIK: cena po odczytach z 4 wątków → 79

        // PUŁAPKA: blokady odczytu NIE da się „podnieść” do blokady zapisu (najpierw readLock, potem writeLock
        // w tym samym wątku) — dlaczego: writeLock czeka, aż wszyscy czytelnicy wyjdą, łącznie z nami → wątek
        // zakleszcza się sam ze sobą. Odwrotnie (zapis → odczyt, „obniżenie”) jest dozwolone.
        // DOBRA PRAKTYKA: przy małej liczbie odczytów lub bardzo krótkich sekcjach zwykła blokada bywa szybsza —
        // ReadWriteLock ma większy narzut. Mierz, zanim wybierzesz.
    }

    /** Cennik chroniony blokadą odczytu/zapisu. */
    static class PriceList {   // price list = cennik
        private final java.util.Map<String, Integer> prices = new java.util.HashMap<>();
        private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();

        Integer get(String sku) {
            rw.readLock().lock();
            try {
                return prices.get(sku);
            } finally {
                rw.readLock().unlock();
            }
        }

        void update(String sku, int price) {   // update = zaktualizuj
            rw.writeLock().lock();
            try {
                prices.put(sku, price);
            } finally {
                rw.writeLock().unlock();
            }
        }
    }

    // =================================================================================================
    // 7. StampedLock — ODCZYT OPTYMISTYCZNY (KRÓTKO)
    // =================================================================================================

    /** Punkt chroniony StampedLock (przykład w duchu dokumentacji JDK). */
    static class Point {   // point = punkt
        private double x;
        private double y;
        private final StampedLock sl = new StampedLock();

        void move(double dx, double dy) {   // move = przesuń
            long stamp = sl.writeLock();    // stamp = stempel (znacznik wersji)
            try {
                x += dx;
                y += dy;
            } finally {
                sl.unlockWrite(stamp);
            }
        }

        /** Odczyt optymistyczny: bez blokowania; jeśli w międzyczasie był zapis — powtórz z prawdziwą blokadą. */
        double sumOfCoordinates(boolean[] optimisticWorked) {   // sum of coordinates = suma współrzędnych
            long stamp = sl.tryOptimisticRead();   // try optimistic read = spróbuj odczytu optymistycznego
            double cx = x;
            double cy = y;
            if (!sl.validate(stamp)) {             // validate = sprawdź, czy od stempla nikt nie pisał
                optimisticWorked[0] = false;
                stamp = sl.readLock();             // plan B: zwykła blokada odczytu
                try {
                    cx = x;
                    cy = y;
                } finally {
                    sl.unlockRead(stamp);
                }
            } else {
                optimisticWorked[0] = true;
            }
            return cx + cy;
        }

        /** Rozdzielona wersja do demonstracji: stempel → (zapis z zewnątrz) → validate. */
        long beginOptimistic() {
            return sl.tryOptimisticRead();
        }

        boolean stillValid(long stamp) {
            return sl.validate(stamp);
        }
    }

    /**
     * 7. {@code StampedLock} (Java 8+) ma tryb odczytu optymistycznego: odczytujemy BEZ blokady i dopiero potem
     * sprawdzamy, czy nikt w tym czasie nie pisał. Bardzo szybkie, gdy zapisy są rzadkie.
     */
    static void stampedLockOptimistic() throws InterruptedException {
        section("7. StampedLock — odczyt optymistyczny");

        Point point = new Point();
        point.move(3, 4);
        boolean[] optimistic = new boolean[1];
        show("suma współrzędnych", point.sumOfCoordinates(optimistic));
        // WYNIK: suma współrzędnych → 7.0
        show("odczyt optymistyczny się udał (nikt nie pisał)", optimistic[0]);
        // WYNIK: odczyt optymistyczny się udał (nikt nie pisał) → true

        // Wymuszamy zapis POMIĘDZY pobraniem stempla a sprawdzeniem:
        long stamp = point.beginOptimistic();
        Thread mover = new Thread(() -> point.move(1, 1), "przesuwacz");
        mover.start();
        mover.join();
        show("validate po zapisie innego wątku", point.stillValid(stamp));
        // WYNIK: validate po zapisie innego wątku → false
        // Po false trzeba odczytać jeszcze raz pod prawdziwą blokadą (jak w sumOfCoordinates).

        // PUŁAPKA: StampedLock NIE jest wielowejściowy — dlaczego: nie ma pojęcia właściciela; ponowne writeLock()
        // w tym samym wątku zawiesi go na zawsze. Nie ma też Condition. Używaj tylko w małych, prostych klasach.
        // PUŁAPKA: w trybie optymistycznym odczytane wartości mogą być niespójne (x z przed zapisu, y po) — wolno ich
        // użyć dopiero PO udanym validate(); nie wołaj na nich metod, które mogą się wysypać na złych danych.
    }

    // =================================================================================================
    // 8. wait() / notify() — KLASYKA
    // =================================================================================================

    /** Skrzynka na jedną wiadomość — klasyczne wait/notifyAll. */
    static class Mailbox {   // mailbox = skrzynka pocztowa
        private String message;   // null = pusto

        synchronized void put(String text) throws InterruptedException {
            while (message != null) {   // zajęte — czekamy (w pętli!)
                wait();                 // wait = czekaj: zwalnia monitor this i usypia
            }
            message = text;
            notifyAll();                // notifyAll = powiadom wszystkich czekających na monitorze this
        }

        synchronized String take() throws InterruptedException {
            while (message == null) {
                wait();
            }
            String text = message;
            message = null;
            notifyAll();
            return text;
        }
    }

    /**
     * 8. Przed java.util.concurrent koordynację robiło się metodami klasy {@code Object}: {@code wait()},
     * {@code notify()}, {@code notifyAll()}. Zasady: wołaj je TYLKO trzymając monitor obiektu (w synchronized)
     * i zawsze czekaj w pętli while.
     */
    static void waitNotifyClassic() throws InterruptedException {
        section("8. wait() i notify() — klasyczna koordynacja");

        Mailbox box = new Mailbox();
        List<String> received = new ArrayList<>();   // received = odebrane
        Thread sender = new Thread(() -> {
            try {
                for (String s : List.of("raz", "dwa", "trzy")) {
                    box.put(s);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "nadawca");
        Thread receiver = new Thread(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    received.add(box.take());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "odbiorca");
        sender.start();
        receiver.start();
        sender.join();
        receiver.join();
        show("odebrane wiadomości", received);
        // WYNIK: odebrane wiadomości → [raz, dwa, trzy]

        // PUŁAPKA: wait()/notify() bez trzymania monitora tego obiektu → IllegalMonitorStateException — dlaczego:
        // wait() musi atomowo zwolnić monitor i zasnąć, a nie można zwolnić czegoś, czego się nie trzyma.
        Object lock = new Object();
        expectThrows("lock.wait() poza synchronized", () -> lock.wait());
        // WYNIK: ✔ lock.wait() poza synchronized → rzucono IllegalMonitorStateException: current thread is not owner
        expectThrows("lock.notify() poza synchronized", lock::notify);
        // WYNIK: ✔ lock.notify() poza synchronized → rzucono IllegalMonitorStateException: current thread is not owner

        // PUŁAPKA: notify() zamiast notifyAll() — notify budzi JEDEN dowolny wątek. Gdy na tym samym monitorze czekają
        // i producenci, i konsumenci, można obudzić „niewłaściwego” (np. producenta, gdy bufor wciąż pełny); ten
        // zaśnie z powrotem, a właściwy nie zostanie obudzony nigdy. Dlaczego Condition jest lepszy: osobne
        // poczekalnie notFull/notEmpty, więc signal() budzi właściwą grupę.
        // DOBRA PRAKTYKA: w nowym kodzie nie używaj wait/notify — wybierz BlockingQueue, CountDownLatch, Semaphore
        // (Concurrency06, Concurrency07). wait/notify trzeba znać, bo pojawia się w starym kodzie i na rozmowach.
    }

    // =================================================================================================
    // 9. ZAKLESZCZENIE — WYKRYCIE, KOLEJNOŚĆ BLOKAD, tryLock Z LIMITEM
    // =================================================================================================

    /**
     * 9. Dwa wątki biorą dwie blokady w ODWROTNEJ kolejności. Bariera gwarantuje, że oba zdążą wziąć pierwszą —
     * więc zakleszczenie jest pewne. Wątki są demonami: wykrywamy zakleszczenie i je porzucamy, a JVM i tak się zakończy.
     */
    static void deadlock() throws InterruptedException {
        section("9. Zakleszczenie (deadlock)");

        // a) Prawdziwe zakleszczenie dwóch wątków-demonów, wykryte przez ThreadMXBean (zarządzanie wątkami JVM).
        Object forkA = new Object();   // fork = widelec (problem ucztujących filozofów)
        Object forkB = new Object();
        CyclicBarrier bothHaveFirst = new CyclicBarrier(2);   // both have first = obaj mają pierwszą blokadę
        Thread t1 = new Thread(() -> {
            synchronized (forkA) {
                awaitBarrier(bothHaveFirst);
                synchronized (forkB) {   // czeka na B, które trzyma t2 — na zawsze
                    note("nigdy się nie wykona");
                }
            }
        }, "filozof-1");
        Thread t2 = new Thread(() -> {
            synchronized (forkB) {
                awaitBarrier(bothHaveFirst);
                synchronized (forkA) {   // czeka na A, które trzyma t1 — na zawsze
                    note("nigdy się nie wykona");
                }
            }
        }, "filozof-2");
        t1.setDaemon(true);   // demony: JVM na nie nie czeka, więc program się zakończy mimo zakleszczenia
        t2.setDaemon(true);
        t1.start();
        t2.start();

        List<String> deadlocked = findDeadlockedThreadNames();   // nazwy posortowane
        show("wykryto zakleszczone wątki", deadlocked);
        // WYNIK: wykryto zakleszczone wątki → [filozof-1, filozof-2]
        show("stany", t1.getState() + ", " + t2.getState());
        // WYNIK: stany → BLOCKED, BLOCKED
        // Tych wątków NIE da się już uratować: interrupt() nie działa na czekanie przy synchronized. Zostają do końca JVM.

        // b) NAPRAWA 1: stała kolejność brania blokad — usuwamy cykl oczekiwania. Obaj biorą najpierw A, potem B.
        Object lockA = new Object();
        Object lockB = new Object();
        int[] meals = new int[1];   // meals = posiłki
        runInThreads(2, () -> {
            for (int i = 0; i < 10_000; i++) {
                synchronized (lockA) {        // ZAWSZE najpierw A…
                    synchronized (lockB) {    // …potem B
                        meals[0]++;
                    }
                }
            }
        });
        show("stała kolejność blokad — zjedzono posiłków", meals[0]);
        // WYNIK: stała kolejność blokad — zjedzono posiłków → 20000

        // c) NAPRAWA 2: tryLock z limitem — wątek, który nie dostanie drugiej blokady, wycofuje się i zwalnia pierwszą.
        //    Druga bariera trzyma pierwsze blokady do chwili, gdy obaj skończą próbę — więc obie próby NA PEWNO trafią
        //    na zajętą blokadę (inaczej wynik zależałby od tego, kto pierwszy upłynie limit).
        ReentrantLock first = new ReentrantLock();
        ReentrantLock second = new ReentrantLock();
        CyclicBarrier haveFirst = new CyclicBarrier(2);
        CyclicBarrier triedSecond = new CyclicBarrier(2);   // tried second = obaj spróbowali drugiej
        String[] report = new String[2];
        runInThreadsIndexed(2, index -> {
            ReentrantLock mine = index == 0 ? first : second;      // mine = moja
            ReentrantLock other = index == 0 ? second : first;     // other = cudza
            mine.lock();
            try {
                awaitBarrier(haveFirst);
                boolean got = false;
                try {
                    got = other.tryLock(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                report[index] = "wątek " + (index + 1) + (got ? " wziął obie blokady" : " nie dostał drugiej w 100 ms → wycofuje się");
                awaitBarrier(triedSecond);
                if (got) {
                    other.unlock();
                }
            } finally {
                mine.unlock();   // wycofanie = zwolnienie tego, co mamy; można spróbować ponownie po losowej przerwie
            }
        });
        show("tryLock", report[0]);
        // WYNIK: tryLock → wątek 1 nie dostał drugiej w 100 ms → wycofuje się
        show("tryLock", report[1]);
        // WYNIK: tryLock → wątek 2 nie dostał drugiej w 100 ms → wycofuje się
        // Wątki nie wiszą — program idzie dalej. W prawdziwym kodzie po wycofaniu ponawia się próbę po LOSOWEJ przerwie.

        // PUŁAPKA: zakleszczenie nie daje wyjątku ani komunikatu — program po prostu „staje”. Dlaczego trudno je
        // znaleźć: zdarza się tylko przy niefortunnym przeplocie, np. raz na tydzień pod obciążeniem.
        // Diagnoza w praktyce: zrzut wątków — jstack <pid> albo Ctrl+Break (Windows) w konsoli z programem; JVM
        // sama wypisze sekcję „Found one Java-level deadlock”. W IntelliJ: przycisk aparatu (Dump Threads) w debugerze.
        // DOBRA PRAKTYKA: (1) ustal globalną kolejność blokad (np. po id konta) i trzymaj się jej; (2) nie wołaj cudzego
        // kodu trzymając blokadę; (3) trzymaj blokady krótko; (4) tam, gdzie się da, tryLock z limitem.
    }

    // =================================================================================================
    // 10. LIVELOCK I ZAGŁODZENIE (OPIS)
    // =================================================================================================

    /**
     * 10. Dwa pokrewne problemy, które opisujemy bez uruchamiania (ich przebieg z natury zależy od czasu).
     */
    static void livelockAndStarvation() {
        section("10. Livelock i zagłodzenie (opis)");

        // Livelock (uwięzienie w ruchu): wątki NIE są zablokowane — ciągle coś robią, ale żaden nie posuwa się naprzód.
        //   Przykład: dwa wątki z naprawy 2 wycofują się i ponawiają próbę po DOKŁADNIE tej samej przerwie — za każdym
        //   razem znowu biorą „swoją” blokadę i znowu się wycofują. Jak dwie osoby w korytarzu, które jednocześnie
        //   ustępują sobie w tę samą stronę. Lekarstwo: losowa przerwa przed ponowieniem (Random z różnym czasem).
        // Zagłodzenie (starvation): wątek bardzo długo (lub nigdy) nie dostaje zasobu, bo inni go stale wyprzedzają.
        //   Przykład: nieuczciwa blokada pod dużym obciążeniem; wątki czytające w ReadWriteLock, które nie dają dojść
        //   pisarzowi; długie sekcje krytyczne. Lekarstwo: blokada uczciwa (fair), krótsze sekcje, ograniczenie liczby
        //   wątków walczących o zasób.
        note("deadlock: wszyscy czekają; livelock: wszyscy się ruszają, nikt nie postępuje; starvation: jeden wiecznie czeka");
        // WYNIK: ℹ deadlock: wszyscy czekają; livelock: wszyscy się ruszają, nikt nie postępuje; starvation: jeden wiecznie czeka

        // Związek ze Springiem: transakcje bazodanowe też biorą blokady (na wierszach tabel). Dwie transakcje
        // aktualizujące te same wiersze w odwrotnej kolejności = zakleszczenie w bazie danych; baza wykryje je
        // i wycofa jedną z transakcji wyjątkiem. Ta sama zasada leczy: stała kolejność aktualizacji.
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI
    // =================================================================================================

    /** Zwraca posortowane nazwy wątków w zakleszczeniu; czeka na nie najwyżej 10 s. */
    static List<String> findDeadlockedThreadNames() throws InterruptedException {
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();   // MXBean = obiekt zarządzania JVM
        long deadline = System.currentTimeMillis() + 10_000;
        long[] ids = mx.findDeadlockedThreads();   // find deadlocked threads = znajdź zakleszczone wątki (null = brak)
        while (ids == null) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("nie wykryto zakleszczenia");
            }
            Thread.sleep(10);
            ids = mx.findDeadlockedThreads();
        }
        List<String> names = new ArrayList<>();
        for (ThreadInfo info : mx.getThreadInfo(ids)) {   // ThreadInfo = informacje o wątku
            names.add(info.getThreadName());
        }
        names.sort(null);   // sort(null) = sortuj naturalnie (alfabetycznie)
        return names;
    }

    @FunctionalInterface
    interface IndexedTask {
        void run(int index);
    }

    static void runInThreads(int n, Runnable task) throws InterruptedException {
        runInThreadsIndexed(n, index -> task.run());
    }

    /** Uruchamia {@code n} wątków z numerem 0..n-1 i czeka na wszystkie. */
    static void runInThreadsIndexed(int n, IndexedTask task) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int index = i;
            Thread t = new Thread(() -> task.run(index), "lekcja-" + (i + 1));
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
    }

    static void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (BrokenBarrierException e) {
            throw new IllegalStateException(e);
        }
    }

    static void waitForState(Thread thread, Thread.State expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (thread.getState() != expected) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("wątek " + thread.getName() + " nie osiągnął stanu " + expected);
            }
            Thread.sleep(1);
        }
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • synchronized i ReentrantLock są wielowejściowe; każde lock() wymaga swojego unlock().
     *   • Wzorzec: lock.lock(); try { ... } finally { lock.unlock(); }  — lock() PRZED try.
     *   • tryLock() — bez czekania; tryLock(czas) — z limitem; unlock() tylko gdy zwróciły true.
     *   • lockInterruptibly() reaguje na interrupt(); lock() i synchronized — nie.
     *   • new ReentrantLock(true) = uczciwa (FIFO), wolniejsza; domyślna = nieuczciwa, szybsza.
     *   • Condition: await() zwalnia blokadę i czeka; signal()/signalAll() budzą; czekaj ZAWSZE w pętli while.
     *   • ReadWriteLock: wielu czytelników naraz albo jeden pisarz; nie podnoś odczytu do zapisu.
     *   • StampedLock: odczyt optymistyczny + validate(); NIE jest wielowejściowy.
     *   • wait/notify tylko w synchronized na tym samym obiekcie (inaczej IllegalMonitorStateException), w pętli while;
     *     notifyAll zamiast notify. W nowym kodzie — gotowe narzędzia java.util.concurrent.
     *   • Zakleszczenie: 4 warunki; lekarstwo — stała kolejność blokad, tryLock z limitem, krótkie sekcje.
     *   • Diagnoza: jstack / zrzut wątków / ThreadMXBean.findDeadlockedThreads().
     *
     * PYTANIA KONTROLNE:
     *   1. Co by się stało, gdyby blokady w Javie NIE były wielowejściowe, a metoda synchronized wołała inną
     *      metodę synchronized tego samego obiektu?
     *   2. ZNAJDŹ BŁĄD:
     *        lock.lock();
     *        process(order);      // może rzucić wyjątek
     *        lock.unlock();
     *   3. ZNAJDŹ BŁĄD:
     *        synchronized (this) { if (queue.isEmpty()) wait(); return queue.poll(); }
     *   4. Co wypisze?
     *        ReentrantLock l = new ReentrantLock();
     *        l.lock(); l.lock(); l.unlock();
     *        System.out.println(l.isLocked() + " " + l.getHoldCount());
     *   5. Co wypisze?  new Object().notify();   (wyjątek? jaki?)
     *   6. Wymień 4 warunki zakleszczenia i powiedz, który najłatwiej usunąć.
     *   7. Kiedy ReadWriteLock ma sens, a kiedy jest zbędny?
     *   8. Czym różni się livelock od deadlocka?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: licznik na ReentrantLock, 3 wątki × 20 000", 60_000, () -> exercise1(3, 20_000));
        Check.equal("ćw. 2: tryLock gdy zajęta, potem gdy wolna", List.of(false, true), Concurrency03Locks::exercise2);
        Check.equal("ćw. 3: suma 1..100 przez bufor o pojemności 3", 5050, () -> exercise3(100, 3));
        Check.equal("ćw. 4: salda po przelewach w obie strony (bez zakleszczenia)", List.of(500, 500), () -> exercise4(4, 2_000));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 60_000, () -> solution1(3, 20_000));
        Check.equal("ćw. 2 (wzorzec)", List.of(false, true), Concurrency03Locks::solution2);
        Check.equal("ćw. 3 (wzorzec)", 5050, () -> solution3(100, 3));
        Check.equal("ćw. 4 (wzorzec)", List.of(500, 500), () -> solution4(4, 2_000));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): {@code threads} wątków zwiększa wspólny licznik po {@code perThread} razy, chroniony przez
     * {@code ReentrantLock}. Zwróć wynik.
     * Podpowiedź: {@code lock.lock(); try { counter[0]++; } finally { lock.unlock(); }} — lock() przed try.
     */
    static int exercise1(int threads, int perThread) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): utwórz {@code ReentrantLock}. Zablokuj ją w main, a w innym wątku wywołaj {@code tryLock()}
     * i zapamiętaj wynik. Po zwolnieniu blokady przez main wywołaj {@code tryLock()} w kolejnym wątku. Zwróć listę
     * [wynik pierwszej próby, wynik drugiej próby]. Nie zostaw zajętej blokady!
     * Podpowiedź: wątek, któremu tryLock się udał, sam musi zrobić unlock(). Wyniki czytaj po join().
     */
    static List<Boolean> exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): użyj klasy {@code BoundedBuffer} z sekcji 5. Producent wkłada liczby 1..n, konsument
     * odbiera dokładnie n liczb i je sumuje. Zwróć sumę (odczytaną po join() obu wątków).
     * Podpowiedź: konsument może po prostu wykonać n razy {@code take()} — pigułka trucizny nie jest tu potrzebna.
     */
    static int exercise3(int n, int capacity) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): PRZEPISZ przelew, który może się zakleszczyć, na wersję ze stałą kolejnością blokad:
     * <pre>{@code
     * void transfer(Konto from, Konto to, int amount) {
     *     from.lock.lock();            // wątek 1: from = A, to = B
     *     try {                        // wątek 2: from = B, to = A   → zakleszczenie!
     *         to.lock.lock();
     *         try { from.balance -= amount; to.balance += amount; } finally { to.lock.unlock(); }
     *     } finally { from.lock.unlock(); }
     * }
     * }</pre>
     * Dwa konta (id 0 i 1) po 500 zł. Wątki o parzystym numerze robią {@code rounds} przelewów 1 zł z 0 na 1, a o
     * nieparzystym — z 1 na 0. Zwróć [saldo konta 0, saldo konta 1].
     * Podpowiedź: blokuj zawsze najpierw konto o MNIEJSZYM id, niezależnie od kierunku przelewu. Uwaga: błędna wersja
     * może się naprawdę zakleszczyć i program „stanie” — wtedy przerwij go w IntelliJ (czerwony kwadrat Stop).
     */
    static List<Integer> exercise4(int threads, int rounds) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static int solution1(int threads, int perThread) {
        ReentrantLock lock = new ReentrantLock();
        int[] counter = new int[1];
        try {
            runInThreads(threads, () -> {
                for (int i = 0; i < perThread; i++) {
                    lock.lock();
                    try {
                        counter[0]++;
                    } finally {
                        lock.unlock();
                    }
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return counter[0];
    }

    static List<Boolean> solution2() {
        ReentrantLock lock = new ReentrantLock();
        boolean[] results = new boolean[2];
        try {
            lock.lock();
            try {
                Thread first = new Thread(() -> results[0] = tryAndRelease(lock), "próba-1");
                first.start();
                first.join();
            } finally {
                lock.unlock();
            }
            Thread second = new Thread(() -> results[1] = tryAndRelease(lock), "próba-2");
            second.start();
            second.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return List.of(results[0], results[1]);
    }

    static boolean tryAndRelease(ReentrantLock lock) {   // spróbuj i zwolnij
        boolean got = lock.tryLock();
        if (got) {
            lock.unlock();
        }
        return got;
    }

    static int solution3(int n, int capacity) {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(capacity);
        int[] sum = new int[1];
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= n; i++) {
                    buffer.put(i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producent-ćw");
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < n; i++) {
                    sum[0] += buffer.take();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "konsument-ćw");
        producer.start();
        consumer.start();
        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return sum[0];
    }

    /** Konto z własną blokadą — do ćwiczenia 4. */
    static final class LockedAccount {
        final int id;
        final ReentrantLock lock = new ReentrantLock();
        int balance;

        LockedAccount(int id, int balance) {
            this.id = id;
            this.balance = balance;
        }
    }

    static void orderedTransfer(LockedAccount from, LockedAccount to, int amount) {   // ordered = uporządkowany
        LockedAccount firstLock = from.id < to.id ? from : to;    // zawsze mniejsze id najpierw
        LockedAccount secondLock = from.id < to.id ? to : from;
        firstLock.lock.lock();
        try {
            secondLock.lock.lock();
            try {
                from.balance -= amount;
                to.balance += amount;
            } finally {
                secondLock.lock.unlock();
            }
        } finally {
            firstLock.lock.unlock();
        }
    }

    static List<Integer> solution4(int threads, int rounds) {
        LockedAccount a = new LockedAccount(0, 500);
        LockedAccount b = new LockedAccount(1, 500);
        try {
            runInThreadsIndexed(threads, index -> {
                for (int r = 0; r < rounds; r++) {
                    if (index % 2 == 0) {
                        orderedTransfer(a, b, 1);
                    } else {
                        orderedTransfer(b, a, 1);
                    }
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return List.of(a.balance, b.balance);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Wątek zakleszczyłby się sam ze sobą: druga metoda czekałaby na blokadę, którą trzyma ten sam wątek.
     *      Wielowejściowość pozwala wejść ponownie (licznik wejść).
     *   2. Brak try/finally: gdy process() rzuci wyjątek, unlock() się nie wykona i blokada zostanie zajęta na zawsze.
     *      Poprawnie: lock.lock(); try { process(order); } finally { lock.unlock(); }
     *   3. if zamiast while: po obudzeniu (także fałszywym) kolejka może być pusta — poll() zwróci null.
     *      Poprawnie: while (queue.isEmpty()) wait();
     *   4. „true 1” — po dwóch lock() i jednym unlock() licznik wejść wynosi 1, blokada wciąż zajęta.
     *   5. Rzuci IllegalMonitorStateException (komunikat w JDK 17: „current thread is not owner”) — notify() wymaga
     *      trzymania monitora tego obiektu (bloku synchronized na nim).
     *   6. Wzajemne wykluczanie, trzymanie i czekanie (hold and wait), brak wywłaszczenia, cykl oczekiwania.
     *      Najłatwiej usunąć cykl: wszystkie wątki biorą blokady w tej samej, ustalonej kolejności.
     *   7. Gdy odczytów jest dużo więcej niż zapisów, a sekcje odczytu nie są trywialnie krótkie (np. cennik,
     *      konfiguracja). Przy częstych zapisach lub bardzo krótkich sekcjach zwykła blokada bywa szybsza.
     *   8. W deadlocku wątki stoją zablokowane i czekają na siebie. W livelocku wątki nie są zablokowane — ciągle
     *      reagują na siebie (np. wycofują się i ponawiają), ale żaden nie robi postępu.
     */
    // </editor-fold>
}
