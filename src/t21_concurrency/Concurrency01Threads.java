package t21_concurrency;

import helpers.Check;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Wątki — podstawy
 *        (thread = wątek, process = proces, start = uruchom, join = dołącz/poczekaj na koniec,
 *         interrupt = przerwij, daemon = demon, state = stan)
 *
 * W SKRÓCIE:
 *   Wątek to niezależna „ścieżka wykonania” wewnątrz jednego programu. Dzięki wątkom program może robić kilka rzeczy
 *   naraz: czekać na sieć lub dysk i w tym czasie liczyć coś innego, albo wykorzystać kilka rdzeni procesora.
 *   W tej lekcji: tworzenie i uruchamianie wątków, czekanie na ich koniec, stany wątku, demony i przerywanie.
 *
 * ANALOGIA:
 *   Proces to restauracja: ma własną kuchnię, magazyn i kasę (własną pamięć). Wątki to kucharze w tej kuchni:
 *   każdy wykonuje swój przepis (własny stos wywołań), ale wszyscy korzystają z tych samych lodówek i blatów
 *   (wspólna sterta — heap). Więcej kucharzy = szybciej, ale gdy dwóch sięga po ten sam garnek, robi się bałagan
 *   (o tym Concurrency02RaceConditions).
 *
 * JAK TO DZIAŁA:
 *   • Proces = uruchomiony program z WŁASNĄ pamięcią (system operacyjny izoluje procesy od siebie).
 *     Wątek = jednostka wykonania WEWNĄTRZ procesu. Wątki jednego procesu dzielą stertę (obiekty), ale każdy ma
 *     własny stos (zmienne lokalne, parametry, kolejne wywołania metod).
 *   • JVM startuje z wątkiem „main”, który wykonuje metodę main(). Sami tworzymy kolejne:
 *       Thread t = new Thread(() -> praca(), "pracownik-1");   // zadanie (Runnable) + nazwa
 *       t.start();   // NOWY wątek zaczyna wykonywać run()
 *       t.join();    // bieżący wątek czeka, aż t się skończy
 *   • Po co współbieżność?
 *       1) czekanie na wejście/wyjście (IO): wątek czekający na bazę danych nie zużywa procesora — inny może liczyć;
 *       2) wiele rdzeni: obliczenia podzielone na wątki mogą biec naprawdę równolegle;
 *       3) responsywność: serwer obsługuje wiele żądań naraz, okno programu nie „zamarza”.
 *   • Cykl życia (Thread.State):
 *       NEW ──start()──▶ RUNNABLE ──koniec run()──▶ TERMINATED
 *                          │  ▲
 *          sleep/join/wait │  │ obudzenie
 *                          ▼  │
 *            WAITING / TIMED_WAITING / BLOCKED (czeka na monitor synchronized)
 *   • RUNNABLE znaczy „może działać” — wątek wykonuje się albo czeka w kolejce na przydział procesora.
 *     Java nie rozróżnia tych dwóch sytuacji.
 *
 * SŁÓWKA:
 *   thread = wątek; process = proces; runnable = dający się uruchomić (zadanie bez wyniku); start = uruchom;
 *   run = wykonaj; join = dołącz (czekaj na koniec); sleep = śpij; interrupt = przerwij; daemon = demon (wątek tła);
 *   state = stan; alive = żywy; priority = priorytet; handler = obsługa (procedura obsługi); busy waiting = aktywne
 *   czekanie; stack = stos; heap = sterta; current = bieżący.
 *
 * ZOBACZ TEŻ: t21_concurrency/Concurrency02RaceConditions (co psuje wspólna pamięć),
 *             t21_concurrency/Concurrency04Executors (pule wątków zamiast ręcznego new Thread),
 *             t21_concurrency/Concurrency10MemoryModel (dlaczego join() gwarantuje widoczność wyników),
 *             t13_lambdas/Lambda02FunctionalInterfaces (Runnable to interfejs funkcyjny),
 *             t10_exceptions/Exceptions02CheckedUnchecked (InterruptedException jest sprawdzany).
 * </pre>
 */
public class Concurrency01Threads {

    public static void main(String[] args) throws InterruptedException {
        title("Concurrency01 — wątki: tworzenie, join, stany, demony, przerywanie");

        processAndMainThread();      // process and main thread = proces i wątek główny
        creatingThreads();           // creating threads = tworzenie wątków
        startVersusRun();            // start versus run = start kontra run
        joiningThreads();            // joining threads = czekanie na koniec wątków
        threadStates();              // thread states = stany wątku
        daemonThreads();             // daemon threads = wątki demony
        interruptingSleep();         // interrupting sleep = przerywanie uśpienia
        cooperativeCancellation();   // cooperative cancellation = anulowanie za zgodą (kooperacyjne)
        uncaughtExceptions();        // uncaught exceptions = nieprzechwycone wyjątki
        costAndHints();              // cost and hints = koszt wątków i „podpowiedzi” dla systemu
        exercises();                 // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PROCES A WĄTEK
    // =================================================================================================

    /**
     * 1. Każdy program w Javie ma od początku co najmniej jeden wątek: „main”. Odczytujemy jego właściwości przez
     * {@code Thread.currentThread()} — statyczną metodę zwracającą wątek, który właśnie wykonuje ten kod.
     */
    static void processAndMainThread() {
        section("1. Proces a wątek — wątek główny");

        Thread current = Thread.currentThread();   // currentThread = bieżący wątek
        show("nazwa bieżącego wątku", current.getName());   // getName = pobierz nazwę
        // WYNIK: nazwa bieżącego wątku → main
        show("czy to demon?", current.isDaemon());          // isDaemon = czy jest demonem
        // WYNIK: czy to demon? → false
        show("priorytet", current.getPriority());           // getPriority = pobierz priorytet
        // WYNIK: priorytet → 5
        show("stan wątku, który sam się pyta", current.getState());   // getState = pobierz stan
        // WYNIK: stan wątku, który sam się pyta → RUNNABLE

        // availableProcessors = dostępne procesory (rdzenie logiczne widziane przez JVM).
        // Liczba zależy od maszyny — wypisujemy tylko pewny fakt.
        show("liczba rdzeni > 0", Runtime.getRuntime().availableProcessors() > 0);
        // WYNIK: liczba rdzeni > 0 → true

        // Proces a wątek w liczbach (typowe wartości, nie gwarancje):
        //   • proces: osobna przestrzeń adresowa; komunikacja między procesami przez sieć, pliki, potoki — drogo;
        //   • wątek: wspólna sterta z innymi wątkami procesu; komunikacja przez wspólne obiekty — tanio,
        //     ale niebezpiecznie (wyścigi, Concurrency02RaceConditions).
        // DOBRA PRAKTYKA: zanim dodasz wątki, zapytaj „na co mój program czeka?”. Jeśli czeka na IO (sieć, dysk, baza),
        // wątki pomogą nawet na jednym rdzeniu. Jeśli liczy, pomogą najwyżej tyle razy, ile jest rdzeni — dlaczego:
        // dwa wątki liczące na jednym rdzeniu tylko się przeplatają, a przełączanie samo kosztuje czas.
    }

    // =================================================================================================
    // 2. TWORZENIE WĄTKÓW
    // =================================================================================================

    /**
     * 2. Trzy sposoby podania zadania: obiekt {@code Runnable}, lambda (Runnable to interfejs funkcyjny z jedną
     * metodą {@code void run()}) i podklasa {@code Thread}. Wyniki zapisujemy do tablicy, a wypisujemy dopiero po
     * {@code join()} — wtedy kolejność wydruku jest pewna.
     */
    static void creatingThreads() throws InterruptedException {
        section("2. Tworzenie wątków — Runnable, lambda, podklasa Thread");

        String[] results = new String[3];   // results = wyniki; każdy wątek pisze TYLKO do swojej komórki

        // a) klasa anonimowa implementująca Runnable (styl sprzed Javy 8)
        Runnable task = new Runnable() {    // task = zadanie
            @Override
            public void run() {
                results[0] = "anonimowy Runnable w wątku " + Thread.currentThread().getName();
            }
        };
        Thread first = new Thread(task, "pracownik-1");     // konstruktor (zadanie, nazwa)

        // b) lambda — najczęstszy zapis
        Thread second = new Thread(() -> results[1] = "lambda w wątku " + Thread.currentThread().getName(),
                "pracownik-2");

        // c) podklasa Thread z nadpisanym run() — działa, ale miesza „co robić” z „jak uruchomić”
        Thread third = new GreetingThread(results);

        first.start();    // start = uruchom (tworzy NOWY wątek systemowy)
        second.start();
        third.start();
        first.join();     // join = czekaj na koniec wątku
        second.join();
        third.join();

        for (String r : results) {
            show("wynik", r);
        }
        // WYNIK: wynik → anonimowy Runnable w wątku pracownik-1
        // WYNIK: wynik → lambda w wątku pracownik-2
        // WYNIK: wynik → podklasa Thread w wątku pracownik-3

        // Kolejność WYKONANIA trzech wątków jest nieznana (mogły się wykonać w dowolnej kolejności, nawet naraz).
        // Kolejność WYDRUKU jest pewna, bo drukuje tylko main, po join(), z tablicy w ustalonym porządku.
        // DOBRA PRAKTYKA: zawsze nadawaj wątkom własne nazwy — dlaczego: domyślne nazwy „Thread-0”, „Thread-1”
        // zależą od tego, ile wątków utworzono wcześniej, a w logach i zrzutach wątków nic nie mówią.
        // DOBRA PRAKTYKA: wybieraj Runnable/lambdę zamiast dziedziczenia po Thread — dlaczego: zadanie można potem
        // przekazać do puli wątków (Concurrency04Executors) bez przepisywania, a klasa może dziedziczyć po czymś innym.

        // PUŁAPKA: wątku nie da się uruchomić drugi raz — dlaczego: obiekt Thread opisuje jeden przebieg wątku
        // systemowego; po zakończeniu jest TERMINATED na zawsze. Nowy przebieg = nowy obiekt Thread.
        expectThrows("drugie start() na tym samym wątku", first::start);
        // WYNIK: ✔ drugie start() na tym samym wątku → rzucono IllegalThreadStateException: (brak komunikatu)
    }

    /** Podklasa Thread — tylko do pokazania, że tak też można. */
    static class GreetingThread extends Thread {   // greeting = powitanie
        private final String[] results;

        GreetingThread(String[] results) {
            super("pracownik-3");   // nazwa wątku przez konstruktor klasy bazowej
            this.results = results;
        }

        @Override
        public void run() {
            results[2] = "podklasa Thread w wątku " + getName();
        }
    }

    // =================================================================================================
    // 3. START KONTRA RUN
    // =================================================================================================

    /**
     * 3. Najczęstszy błąd początkujących: wywołanie {@code run()} zamiast {@code start()}. Metoda {@code run()} to
     * zwykła metoda — wykona się w BIEŻĄCYM wątku, synchronicznie. Żaden nowy wątek nie powstaje.
     */
    static void startVersusRun() throws InterruptedException {
        section("3. start() kontra run()");

        String[] who = new String[1];   // who = kto
        Thread worker = new Thread(() -> who[0] = Thread.currentThread().getName(), "pracownik-run");

        worker.run();   // PUŁAPKA: zwykłe wywołanie metody — kod biegnie w wątku main
        show("po run() zadanie wykonał wątek", who[0]);
        // WYNIK: po run() zadanie wykonał wątek → main
        show("stan obiektu Thread po run()", worker.getState());
        // WYNIK: stan obiektu Thread po run() → NEW

        worker.start(); // dopiero start() tworzy nowy wątek, który wywoła run()
        worker.join();
        show("po start() zadanie wykonał wątek", who[0]);
        // WYNIK: po start() zadanie wykonał wątek → pracownik-run
        show("stan po zakończeniu", worker.getState());
        // WYNIK: stan po zakończeniu → TERMINATED

        // PUŁAPKA: kod z run() zamiast start() DZIAŁA i daje poprawny wynik — tylko wolniej i bez współbieżności.
        // Dlatego ten błąd długo pozostaje niezauważony. Dlaczego tak jest: start() prosi JVM o nowy wątek systemowy
        // i to ON woła run(); bezpośrednie run() pomija cały ten mechanizm.
    }

    // =================================================================================================
    // 4. JOIN I JOIN(TIMEOUT)
    // =================================================================================================

    /**
     * 4. {@code join()} wstrzymuje bieżący wątek do końca wskazanego wątku. {@code join(ms)} czeka najwyżej podany
     * czas i NIE mówi, czy wątek się skończył — trzeba sprawdzić {@code isAlive()}.
     */
    static void joiningThreads() throws InterruptedException {
        section("4. join() i join(limit czasu)");

        // Sto wątków, każdy liczy kwadrat swojego numeru do własnej komórki tablicy.
        long[] squares = new long[100];   // squares = kwadraty
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < squares.length; i++) {
            int index = i;   // zmienna efektywnie finalna — lambda może jej użyć (t13_lambdas/Lambda06ClosuresScope)
            Thread t = new Thread(() -> squares[index] = (long) (index + 1) * (index + 1), "kwadrat-" + (i + 1));
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join();   // czekamy na KAŻDY wątek; kolejność czekania nie ma znaczenia dla wyniku
        }
        show("suma kwadratów 1..100", java.util.Arrays.stream(squares).sum());
        // WYNIK: suma kwadratów 1..100 → 338350

        // Dlaczego wolno czytać tablicę bez synchronizacji? Bo join() tworzy relację happens-before („dzieje się
        // wcześniej”): wszystko, co wątek zapisał przed końcem, jest WIDOCZNE dla wątku, który z join() wrócił.
        // Bez join() main mógłby zobaczyć stare zera (szczegóły: Concurrency10MemoryModel).

        // join(limit): wątek czeka na zatrzask (CountDownLatch = zatrzask odliczający, Concurrency07Synchronizers),
        // który zwolnimy dopiero później — więc join(100) na pewno wróci przed końcem wątku.
        CountDownLatch gate = new CountDownLatch(1);   // gate = bramka
        Thread slow = new Thread(() -> awaitQuietly(gate), "powolny");   // slow = powolny
        slow.start();
        slow.join(100);   // czekaj najwyżej 100 ms
        show("po join(100) wątek wciąż żyje?", slow.isAlive());   // isAlive = czy żyje
        // WYNIK: po join(100) wątek wciąż żyje? → true
        gate.countDown();   // countDown = odlicz w dół — otwieramy bramkę
        slow.join();        // teraz już bez limitu — wiemy, że skończy
        show("po zwolnieniu i join() żyje?", slow.isAlive());
        // WYNIK: po zwolnieniu i join() żyje? → false

        // PUŁAPKA: join(ms) nie rzuca wyjątku po upływie czasu — po prostu wraca. Kto nie sprawdzi isAlive(),
        // ten uzna niedokończoną pracę za gotową. Dlaczego tak zaprojektowano: limit to „nie czekaj dłużej niż”,
        // a decyzja, co zrobić z niedokończonym wątkiem, należy do wołającego.
    }

    // =================================================================================================
    // 5. STANY WĄTKU
    // =================================================================================================

    /**
     * 5. Stany odczytujemy w momentach, które WYMUSZAMY: przed startem (NEW), gdy wątek na pewno czeka
     * (pętla czekająca na stan), po join() (TERMINATED). Odczyt „w przypadkowym momencie” dałby losowy wynik.
     */
    static void threadStates() throws InterruptedException {
        section("5. Stany wątku (Thread.State)");

        List<String> log = new ArrayList<>();   // log = dziennik zdarzeń (pisze do niego tylko main)

        // TIMED_WAITING: wątek śpi z limitem czasu (sleep(ms), join(ms), wait(ms), await z limitem).
        Thread sleeper = new Thread(() -> {     // sleeper = śpioch
            try {
                Thread.sleep(10_000);   // sleep = śpij; 10 s — zaraz go przerwiemy, nie będziemy czekać
            } catch (InterruptedException e) {
                // Przerwanie to tu oczekiwany sposób zakończenia; nic więcej nie robimy (wątek i tak się kończy).
            }
        }, "śpioch");
        log.add("przed start(): " + sleeper.getState());
        sleeper.start();
        waitForState(sleeper, Thread.State.TIMED_WAITING);   // czekamy, aż wątek NA PEWNO śpi
        log.add("podczas sleep(10 s): " + sleeper.getState());
        sleeper.interrupt();   // budzimy go wyjątkiem InterruptedException (sekcja 7)
        sleeper.join();
        log.add("po join(): " + sleeper.getState());

        // WAITING: czekanie BEZ limitu (join(), wait(), await() bez limitu, LockSupport.park()).
        CountDownLatch gate = new CountDownLatch(1);
        Thread waiter = new Thread(() -> awaitQuietly(gate), "czekający");   // waiter = czekający
        waiter.start();
        waitForState(waiter, Thread.State.WAITING);
        log.add("podczas await(): " + waiter.getState());
        gate.countDown();
        waiter.join();

        // BLOCKED: wątek chce wejść do bloku synchronized, ale monitor trzyma ktoś inny (tu: main).
        Object monitor = new Object();   // monitor = obiekt, którego blokadę bierze synchronized
        Thread blocked;
        synchronized (monitor) {
            blocked = new Thread(() -> {
                synchronized (monitor) {
                    // pusto — chodzi tylko o wejście
                }
            }, "zablokowany");
            blocked.start();
            waitForState(blocked, Thread.State.BLOCKED);
            log.add("czeka na synchronized: " + blocked.getState());
        }   // tu main oddaje monitor — wątek „zablokowany” wchodzi i kończy
        blocked.join();

        showEach("stany", log);
        // WYNIK: stany (liczba elementów: 5):
        // WYNIK:    • przed start(): NEW
        // WYNIK:    • podczas sleep(10 s): TIMED_WAITING
        // WYNIK:    • po join(): TERMINATED
        // WYNIK:    • podczas await(): WAITING
        // WYNIK:    • czeka na synchronized: BLOCKED

        // PUŁAPKA: getState() to migawka — w chwili, gdy ją czytasz, stan mógł się już zmienić. Dlaczego: inne wątki
        // biegną dalej. Nadaje się do diagnostyki i nauki, NIE do sterowania programem („jeśli WAITING, to…”).
        // Do koordynacji służą join(), zatrzaski i blokady (Concurrency03Locks, Concurrency07Synchronizers).
    }

    // =================================================================================================
    // 6. WĄTKI DEMONY
    // =================================================================================================

    /**
     * 6. JVM kończy pracę, gdy skończą się wszystkie wątki NIE-demony. Wątki demony (tła) są przerywane razem
     * z końcem JVM — bez wykonania bloków finally. Demonem trzeba zrobić wątek PRZED {@code start()}.
     */
    static void daemonThreads() {
        section("6. Wątki demony (tła)");

        // Ten wątek śpi „w nieskończoność”. Gdyby nie był demonem, program NIGDY by się nie zakończył.
        Thread daemon = new Thread(() -> {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();   // przywrócenie flagi (sekcja 7)
            }
        }, "demon-zegar");
        show("nowy wątek dziedziczy status demona po main", daemon.isDaemon());
        // WYNIK: nowy wątek dziedziczy status demona po main → false
        daemon.setDaemon(true);   // setDaemon = ustaw jako demona — TYLKO przed start()
        daemon.start();
        show("po setDaemon(true) i start()", daemon.isDaemon());
        // WYNIK: po setDaemon(true) i start() → true

        // PUŁAPKA: zmiana statusu działającego wątku jest zabroniona — dlaczego: JVM musi wiedzieć od początku,
        // czy ma na ten wątek czekać przy zamykaniu.
        expectThrows("setDaemon(false) po start()", () -> daemon.setDaemon(false));
        // WYNIK: ✔ setDaemon(false) po start() → rzucono IllegalThreadStateException: (brak komunikatu)

        // Wątek „demon-zegar” zostaje uśpiony do końca programu — i to jest w porządku: JVM go nie zatrzyma.
        // Przykłady demonów w JVM: „Finalizer” i „Reference Handler” (pomocnicze wątki sprzątania pamięci), wątki kompilatora JIT.
        // PUŁAPKA: nie rób demonem wątku, który zapisuje pliki lub wysyła dane — dlaczego: przy końcu JVM demon
        // zostaje przerwany w dowolnym miejscu, a jego bloki finally się nie wykonają (plik może zostać ucięty).
        // DOBRA PRAKTYKA: demon = zadania pomocnicze, które można bezpiecznie przerwać (odświeżanie pamięci
        // podręcznej, monitorowanie). Ważną pracę kończ jawnie: join() albo zamknięcie puli (Concurrency04Executors).
    }

    // =================================================================================================
    // 7. PRZERYWANIE UŚPIENIA — INTERRUPT
    // =================================================================================================

    /**
     * 7. {@code interrupt()} NIE zabija wątku. Ustawia tylko flagę „poproszono cię o przerwanie”. Metody blokujące
     * ({@code sleep}, {@code join}, {@code wait}, {@code await}) reagują rzuceniem {@code InterruptedException}
     * i przy tym CZYSZCZĄ flagę.
     */
    static void interruptingSleep() throws InterruptedException {
        section("7. interrupt() — przerywanie uśpionego wątku");

        String[] report = new String[3];   // report = raport (pisze wątek, czyta main po join)
        Thread sleeper = new Thread(() -> {
            try {
                Thread.sleep(10_000);
                report[0] = "obudził się sam (to się nie zdarzy)";
            } catch (InterruptedException e) {
                report[0] = "złapano " + e.getClass().getSimpleName() + ": " + e.getMessage();
                // isInterrupted = czy jest przerwany (odczyt flagi bez jej zmiany)
                report[1] = "flaga zaraz po złapaniu: " + Thread.currentThread().isInterrupted();
                Thread.currentThread().interrupt();   // DOBRA PRAKTYKA: przywróć flagę (patrz niżej)
                report[2] = "flaga po przywróceniu: " + Thread.currentThread().isInterrupted();
            }
        }, "śpioch-2");
        sleeper.start();
        // Nie musimy czekać, aż zaśnie: jeśli interrupt() przyjdzie wcześniej, sleep() rzuci wyjątek od razu,
        // bo zobaczy już ustawioną flagę. Wynik jest taki sam w obu przypadkach.
        sleeper.interrupt();
        sleeper.join();
        for (String line : report) {
            show("śpioch-2", line);
        }
        // WYNIK: śpioch-2 → złapano InterruptedException: sleep interrupted
        // WYNIK: śpioch-2 → flaga zaraz po złapaniu: false
        // WYNIK: śpioch-2 → flaga po przywróceniu: true

        // DOBRA PRAKTYKA: w catch (InterruptedException e) zrób jedno z dwóch:
        //   1) przekaż wyjątek dalej (dopisz throws InterruptedException do metody), albo
        //   2) przywróć flagę: Thread.currentThread().interrupt();
        // Dlaczego: rzucenie wyjątku wyczyściło flagę. Jeśli ją „połkniesz” pustym catch, kod wyżej (pętla, pula
        // wątków) nie dowie się, że poproszono o zatrzymanie — i będzie pracował dalej.
        // PUŁAPKA: statyczne Thread.interrupted() (interrupted = przerwany) ODCZYTUJE i CZYŚCI flagę bieżącego wątku,
        // a metoda obiektu isInterrupted() tylko odczytuje. Łatwo pomylić — nazwy są prawie takie same.
        Thread.currentThread().interrupt();                           // ustawiamy flagę wątkowi main
        boolean firstRead = Thread.interrupted();                      // odczyt + czyszczenie
        boolean secondRead = Thread.interrupted();                     // flaga już wyczyszczona
        show("Thread.interrupted() dwa razy", firstRead + ", " + secondRead);
        // WYNIK: Thread.interrupted() dwa razy → true, false
    }

    // =================================================================================================
    // 8. ANULOWANIE KOOPERACYJNE; DLACZEGO NIE stop()
    // =================================================================================================

    /**
     * 8. Wątek zatrzymuje się SAM, gdy zauważy prośbę o przerwanie: sprawdza flagę w pętli i obsługuje
     * {@code InterruptedException} z metod blokujących. To jedyny bezpieczny sposób zatrzymania wątku.
     */
    static void cooperativeCancellation() throws InterruptedException {
        section("8. Anulowanie kooperacyjne (wątek kończy się sam)");

        long[] processed = new long[1];        // processed = przetworzono (liczba partii)
        String[] howEnded = new String[1];     // how ended = jak się zakończył
        Thread worker = new Thread(() -> {
            // Pętla pracy: przed każdą partią sprawdza flagę. Kod „sprząta” na końcu — zawsze się wykona.
            while (!Thread.currentThread().isInterrupted()) {
                processed[0]++;   // „przetworzenie partii”
                try {
                    Thread.sleep(5);   // np. czekanie na kolejną partię danych
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();   // przywracamy flagę → warunek pętli ją zobaczy
                }
            }
            howEnded[0] = "zauważył przerwanie i posprzątał";
        }, "przetwarzacz");
        worker.start();
        worker.interrupt();   // prośba: „skończ, kiedy będziesz w bezpiecznym miejscu”
        worker.join(5_000);   // hojny limit — wątek kończy się zwykle od razu
        show("czy wątek się zakończył", !worker.isAlive());
        // WYNIK: czy wątek się zakończył → true
        show("jak", howEnded[0]);
        // WYNIK: jak → zauważył przerwanie i posprzątał
        // Liczba przetworzonych partii zależy od tego, kiedy wątek zdążył wystartować (mogło być 0):
        show("przetworzono partii ≥ 0", processed[0] >= 0);
        // WYNIK: przetworzono partii ≥ 0 → true

        // Dlaczego NIE Thread.stop() / suspend() / resume():
        //   • stop() przerywał wątek w DOWOLNYM miejscu (rzucał ThreadDeath) i zwalniał wszystkie jego blokady —
        //     obiekty zostawały w połowie modyfikacji (np. pieniądze zdjęte z konta A, niedodane do B), a inne wątki
        //     od razu je widziały. Przestarzałe od Javy 1.2; od Javy 20 stop() rzuca UnsupportedOperationException.
        //   • suspend() zamrażał wątek RAZEM z trzymanymi blokadami → łatwe zakleszczenie (deadlock); resume()
        //     istniał tylko jako para do suspend(). Oba są przestarzałe i przeznaczone do usunięcia (forRemoval).
        // DOBRA PRAKTYKA: zatrzymanie = interrupt() + wątek, który sprawdza flagę i kończy się sam w spójnym stanie.
        // To samo robi pula wątków przy shutdownNow() i Future.cancel(true) (Concurrency04Executors).
    }

    // =================================================================================================
    // 9. WYJĄTKI W WĄTKACH — UncaughtExceptionHandler
    // =================================================================================================

    /**
     * 9. Wyjątek, który „ucieka” z {@code run()}, kończy TYLKO ten wątek — main nic o nim nie wie. Domyślnie JVM
     * wypisuje stos wywołań na System.err. Własną reakcję ustawiamy przez {@code UncaughtExceptionHandler}.
     */
    static void uncaughtExceptions() throws InterruptedException {
        section("9. Wyjątki w wątkach — UncaughtExceptionHandler");

        List<String> errors = new ArrayList<>();   // errors = błędy (dopisuje procedura obsługi, czyta main po join)
        Thread failing = new Thread(() -> {        // failing = psujący się
            throw new IllegalStateException("brak pliku konfiguracji");
        }, "wczytywacz");
        // setUncaughtExceptionHandler = ustaw obsługę nieprzechwyconych wyjątków (dla tego jednego wątku).
        // Procedura dostaje wątek i wyjątek; wykonuje się W UMIERAJĄCYM wątku, przed jego zakończeniem.
        failing.setUncaughtExceptionHandler((thread, ex) ->
                errors.add(thread.getName() + " padł: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()));
        failing.start();
        failing.join();   // join wraca dopiero, gdy wątek skończył — razem z procedurą obsługi

        show("zgłoszony błąd", errors.get(0));
        // WYNIK: zgłoszony błąd → wczytywacz padł: IllegalStateException: brak pliku konfiguracji
        show("stan po wyjątku", failing.getState());
        // WYNIK: stan po wyjątku → TERMINATED

        // PUŁAPKA: try/catch WOKÓŁ start() nie złapie wyjątku z wątku — dlaczego: start() wraca od razu, a wyjątek
        // poleci później, na zupełnie innym stosie wywołań. Wyjątek łapiemy WEWNĄTRZ run() albo procedurą obsługi.
        String caughtAroundStart = "nic";
        try {
            Thread t = new Thread(() -> {
                try {
                    throw new IllegalArgumentException("zły format");
                } catch (IllegalArgumentException e) {
                    // DOBRA PRAKTYKA: obsłuż (zaloguj) wyjątek wewnątrz zadania — dlaczego: tylko tu znasz kontekst.
                }
            }, "parser");
            t.start();
            t.join();
        } catch (RuntimeException e) {
            caughtAroundStart = e.getMessage();
        }
        show("try/catch wokół start() złapał", caughtAroundStart);
        // WYNIK: try/catch wokół start() złapał → nic

        // Thread.setDefaultUncaughtExceptionHandler(...) ustawia obsługę dla WSZYSTKICH wątków bez własnej —
        // przydatne w aplikacji (np. zapis do logu), ale to ustawienie globalne, więc nie zmieniamy go w lekcji.
        // W pulach wątków wyjątki zachowują się inaczej: submit() chowa je w Future (Concurrency04Executors).
    }

    // =================================================================================================
    // 10. AKTYWNE CZEKANIE, PRIORYTETY, KOSZT WĄTKÓW
    // =================================================================================================

    /**
     * 10. Trzy uwagi praktyczne: nie czekaj w pustej pętli, nie licz na priorytety, nie twórz tysięcy wątków.
     */
    static void costAndHints() throws InterruptedException {
        section("10. Aktywne czekanie, priorytety, koszt wątków");

        // a) Aktywne czekanie (busy waiting):  while (!gotowe) { }  — wątek kręci się w pętli i zjada 100% rdzenia,
        //    nic nie robiąc. Dodatkowo bez volatile może NIGDY nie zobaczyć zmiany flagi (Concurrency10MemoryModel).
        //    Lepiej: zablokować się na czymś, co obudzi wątek — join(), CountDownLatch.await(), kolejka blokująca.
        //    Thread.onSpinWait() (Java 9+, on spin wait = podczas kręcenia się) to tylko podpowiedź dla procesora
        //    w KRÓTKICH pętlach — nie lekarstwo na długie czekanie.
        CountDownLatch ready = new CountDownLatch(1);   // ready = gotowe
        String[] message = new String[1];
        Thread producer = new Thread(() -> {             // producer = producent
            message[0] = "dane gotowe";
            ready.countDown();   // sygnał „gotowe” — budzi czekających, bez kręcenia się w pętli
        }, "producent");
        producer.start();
        ready.await();   // main śpi (nie zużywa procesora), dopóki producent nie da sygnału
        // await() też daje happens-before: zapis message[0] przed countDown() jest widoczny po await().
        show("main po await()", message[0]);
        // WYNIK: main po await() → dane gotowe
        producer.join();

        // b) Priorytety: liczby od 1 do 10. To tylko PODPOWIEDŹ dla systemu operacyjnego — Java nie gwarantuje,
        //    że wątek o wyższym priorytecie dostanie więcej czasu (Windows i Linux traktują je zupełnie inaczej).
        show("MIN / NORM / MAX", Thread.MIN_PRIORITY + " / " + Thread.NORM_PRIORITY + " / " + Thread.MAX_PRIORITY);
        // WYNIK: MIN / NORM / MAX → 1 / 5 / 10
        Thread important = new Thread(() -> { }, "ważny");   // important = ważny
        important.setPriority(Thread.MAX_PRIORITY);           // setPriority = ustaw priorytet
        show("priorytet wątku „ważny”", important.getPriority());
        // WYNIK: priorytet wątku „ważny” → 10
        // PUŁAPKA: poprawność programu nie może zależeć od priorytetów — dlaczego: na innej maszynie lub systemie
        // podpowiedź może zostać zignorowana. Kolejność wymuszaj narzędziami synchronizacji.

        // c) Koszt: każdy wątek platformowy (zwykły, systemowy) ma własny stos — typowo rezerwacja rzędu 1 MB
        //    pamięci wirtualnej (ustawienie -Xss; zależy od systemu), a utworzenie i przełączanie wątków kosztuje czas
        //    jądra systemu. Tysiące wątków = dużo pamięci i ciągłe przełączanie. Dlatego w praktyce NIE piszemy
        //    new Thread(...) dla każdego zadania, tylko używamy PULI wątków, która używa wątków wielokrotnie
        //    (Concurrency04Executors). Serwer WWW (np. Tomcat pod Springiem) ma pulę rzędu 200 wątków na żądania HTTP.
        //    (Java 21+) Wątki wirtualne (virtual threads) są bardzo lekkie i zmieniają ten rachunek — w Javie 17 ich nie ma.
        int[] counter = new int[1];
        List<Thread> many = IntStream.rangeClosed(1, 50)
                .mapToObj(i -> new Thread(() -> { }, "krótki-" + i))
                .toList();   // toList (Java 16+) = do listy niezmiennej
        for (Thread t : many) {
            t.start();
        }
        for (Thread t : many) {
            t.join();
            counter[0]++;
        }
        show("utworzono i zakończono krótkich wątków", counter[0]);
        // WYNIK: utworzono i zakończono krótkich wątków → 50
        // 50 wątków, z których każdy nic nie robi — tworzenie kosztowało więcej niż sama praca. Pula by to naprawiła.
    }

    // =================================================================================================
    // NARZĘDZIA LEKCJI
    // =================================================================================================

    /** Czeka (w pętli z krótkim spaniem) aż wątek osiągnie stan; awaryjnie przerywa po 10 s. */
    static void waitForState(Thread thread, Thread.State expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;   // deadline = termin ostateczny
        while (thread.getState() != expected) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("wątek " + thread.getName() + " nie osiągnął stanu " + expected);
            }
            Thread.sleep(1);   // tu krótkie spanie jest w porządku: tylko obserwujemy, nie synchronizujemy danych
        }
    }

    /** await() na zatrzasku bez rzucania wyjątku sprawdzanego (przywraca flagę przerwania). */
    static void awaitQuietly(CountDownLatch latch) {   // await quietly = czekaj po cichu
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • new Thread(runnable, "nazwa") → start() uruchamia NOWY wątek; run() to zwykła metoda w bieżącym wątku.
     *   • Obiekt Thread uruchomisz tylko raz (drugie start() → IllegalThreadStateException).
     *   • join() czeka na koniec i gwarantuje widoczność zapisów wątku; join(ms) może wrócić wcześniej → isAlive().
     *   • Stany: NEW → RUNNABLE → (BLOCKED / WAITING / TIMED_WAITING) → TERMINATED; getState() to tylko migawka.
     *   • Demon: setDaemon(true) PRZED start(); JVM na niego nie czeka; finally w demonie może się nie wykonać.
     *   • interrupt() = prośba (flaga). sleep/join/wait/await rzucają InterruptedException i czyszczą flagę.
     *   • W catch (InterruptedException): rzuć dalej albo Thread.currentThread().interrupt().
     *   • Thread.interrupted() czyta i CZYŚCI flagę; isInterrupted() tylko czyta.
     *   • Nigdy stop()/suspend()/resume() — zostawiają obiekty w niespójnym stanie lub zakleszczają.
     *   • Wyjątek z run() kończy tylko ten wątek → łap w zadaniu albo UncaughtExceptionHandler.
     *   • Nie czekaj w pustej pętli; nie polegaj na priorytetach; zamiast tysięcy wątków — pula (Concurrency04).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się proces od wątku? Co wątki jednego procesu współdzielą, a czego nie?
     *   2. Co wypisze?
     *        Thread t = new Thread(() -> System.out.println(Thread.currentThread().getName()), "A");
     *        t.run();
     *        t.start(); t.join();
     *   3. ZNAJDŹ BŁĄD:
     *        try { Thread.sleep(1000); } catch (InterruptedException e) { }
     *      (metoda działa w pętli w wątku z puli, którą zamykamy przez shutdownNow())
     *   4. Co wypisze?
     *        Thread.currentThread().interrupt();
     *        System.out.println(Thread.currentThread().isInterrupted() + " " + Thread.interrupted()
     *                + " " + Thread.interrupted());
     *   5. Dlaczego program z wątkiem demonem, który śpi w nieskończoność, kończy się normalnie, a z takim samym
     *      zwykłym wątkiem — nie?
     *   6. ZNAJDŹ BŁĄD:
     *        Thread t = new Thread(task);
     *        t.start();
     *        t.setDaemon(true);
     *   7. Wątek wykonał join(500) na innym wątku i wrócił. Czy ten drugi wątek na pewno się skończył?
     *   8. Dlaczego Thread.stop() jest niebezpieczne, a interrupt() — nie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        int[] oneToThousand = IntStream.rangeClosed(1, 1000).toArray();
        Check.equal("ćw. 1: suma 1..1000 liczona przez 4 wątki", 500500L, () -> exercise1(oneToThousand, 4));
        Check.equal("ćw. 2: nazwy wątków, które wykonały zadania", List.of("w-1", "w-2", "w-3"), () -> exercise2(3));
        Check.equal("ćw. 3: flaga przerwania po złapaniu i po przywróceniu", List.of(false, true), Concurrency01Threads::exercise3);
        Check.equal("ćw. 4: stany wątku czekającego na zatrzask",
                List.of(Thread.State.NEW, Thread.State.WAITING, Thread.State.TERMINATED), Concurrency01Threads::exercise4);
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", 500500L, () -> solution1(oneToThousand, 4));
        Check.equal("ćw. 1 (wzorzec, 7 wątków — nierówny podział)", 500500L, () -> solution1(oneToThousand, 7));
        Check.equal("ćw. 2 (wzorzec)", List.of("w-1", "w-2", "w-3"), () -> solution2(3));
        Check.equal("ćw. 3 (wzorzec)", List.of(false, true), Concurrency01Threads::solution3);
        Check.equal("ćw. 4 (wzorzec)",
                List.of(Thread.State.NEW, Thread.State.WAITING, Thread.State.TERMINATED), Concurrency01Threads::solution4);
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): policz sumę tablicy {@code numbers} przy pomocy {@code threadCount} wątków. Podziel tablicę
     * na kawałki; wątek nr i sumuje swój kawałek do komórki {@code partial[i]} tablicy {@code long[]}. Po
     * {@code join()} wszystkich wątków zsumuj kawałki.
     * Podpowiedź: kawałek wątku i to indeksy od {@code i * n / threadCount} (włącznie) do
     * {@code (i + 1) * n / threadCount} (wyłącznie) — taki wzór obsłuży też nierówny podział.
     */
    static long exercise1(int[] numbers, int threadCount) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ — poniższy kod „uruchamia wątki”, ale każde zadanie wykonuje main:
     * <pre>{@code
     * String[] names = new String[n];
     * for (int i = 0; i < n; i++) {
     *     int idx = i;
     *     new Thread(() -> names[idx] = Thread.currentThread().getName(), "w-" + (i + 1)).run();
     * }
     * return List.of(names);   // zwraca [main, main, main]
     * }</pre>
     * Popraw tak, by zadania naprawdę biegły w wątkach „w-1”…„w-n” i metoda zwróciła ich nazwy po kolei.
     * Podpowiedź: start() zamiast run(), zapamiętaj wątki na liście i zrób join() każdego PRZED odczytem tablicy.
     */
    static List<String> exercise2(int n) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): uruchom wątek, który śpi 10 s; przerwij go z main. W catch wątek ma zapisać stan flagi
     * przerwania zaraz po złapaniu wyjątku, przywrócić flagę i zapisać ją jeszcze raz. Zwróć listę dwóch wartości
     * logicznych [flaga po złapaniu, flaga po przywróceniu].
     * Podpowiedź: tablica {@code boolean[2]} jako „skrzynka” na wyniki; odczytaj ją po {@code join()}.
     */
    static List<Boolean> exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): utwórz wątek, który czeka na {@code CountDownLatch(1)} (bez limitu czasu). Zbierz
     * w liście jego stany: przed startem, w trakcie czekania (zaczekaj, aż NA PEWNO czeka) i po zakończeniu
     * (po zwolnieniu zatrzasku i join()). Zwróć tę listę.
     * Podpowiedź: „na pewno czeka” = pętla {@code while (t.getState() != Thread.State.WAITING) Thread.sleep(1);}
     * Pamiętaj, że wątek musi się zakończyć — inaczej zostanie w tle na zawsze (nie jest demonem!).
     */
    static List<Thread.State> exercise4() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static long solution1(int[] numbers, int threadCount) {
        long[] partial = new long[threadCount];   // partial = częściowe sumy
        List<Thread> threads = new ArrayList<>();
        int n = numbers.length;
        for (int i = 0; i < threadCount; i++) {
            int index = i;
            int from = i * n / threadCount;
            int to = (i + 1) * n / threadCount;
            Thread t = new Thread(() -> {
                long sum = 0;
                for (int k = from; k < to; k++) {
                    sum += numbers[k];
                }
                partial[index] = sum;   // każdy wątek pisze tylko do SWOJEJ komórki — brak wyścigu
            }, "sumator-" + (i + 1));
            threads.add(t);
            t.start();
        }
        try {
            for (Thread t : threads) {
                t.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("przerwano liczenie", e);
        }
        long total = 0;
        for (long p : partial) {
            total += p;
        }
        return total;
    }

    static List<String> solution2(int n) {
        String[] names = new String[n];
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int idx = i;
            Thread t = new Thread(() -> names[idx] = Thread.currentThread().getName(), "w-" + (i + 1));
            threads.add(t);
            t.start();
        }
        try {
            for (Thread t : threads) {
                t.join();   // bez join() tablica mogłaby być jeszcze niewypełniona (albo niewidoczna)
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return List.of(names);
    }

    static List<Boolean> solution3() {
        boolean[] flags = new boolean[2];
        Thread sleeper = new Thread(() -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                flags[0] = Thread.currentThread().isInterrupted();
                Thread.currentThread().interrupt();
                flags[1] = Thread.currentThread().isInterrupted();
            }
        }, "śpioch-ćw");
        sleeper.start();
        sleeper.interrupt();
        try {
            sleeper.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return List.of(flags[0], flags[1]);
    }

    static List<Thread.State> solution4() {
        List<Thread.State> states = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        Thread waiter = new Thread(() -> awaitQuietly(latch), "czekający-ćw");
        try {
            states.add(waiter.getState());
            waiter.start();
            waitForState(waiter, Thread.State.WAITING);
            states.add(waiter.getState());
            latch.countDown();
            waiter.join();
            states.add(waiter.getState());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } finally {
            latch.countDown();   // na wypadek błędu: zwolnij wątek, żeby nie wisiał w tle
        }
        return states;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Proces ma własną, izolowaną pamięć; wątek działa wewnątrz procesu. Wątki jednego procesu współdzielą stertę
     *      (obiekty, pola statyczne), ale każdy ma własny stos (zmienne lokalne, parametry, łańcuch wywołań).
     *   2. Najpierw „main” (run() to zwykłe wywołanie w wątku main), potem „A” (start() uruchomił nowy wątek o nazwie A).
     *   3. Pusty catch połyka przerwanie: rzucenie InterruptedException wyczyściło flagę, więc pętla i pula nie wiedzą,
     *      że poproszono o zatrzymanie — wątek pracuje dalej, a shutdownNow() „nie działa”. Poprawka: w catch
     *      Thread.currentThread().interrupt(); (i wyjście z pętli) albo przekazanie wyjątku dalej (throws).
     *   4. „true true false” — isInterrupted() tylko czyta (true), pierwsze Thread.interrupted() czyta true i czyści,
     *      drugie widzi już false.
     *   5. JVM kończy pracę, gdy skończą się wszystkie wątki NIE-demony; na demony nie czeka. Zwykły śpiący wątek
     *      jest nie-demonem, więc JVM czeka na niego w nieskończoność.
     *   6. setDaemon() po start() rzuca IllegalThreadStateException — status demona ustawia się przed start().
     *   7. Nie. join(500) wraca po zakończeniu wątku ALBO po upływie 500 ms — trzeba sprawdzić isAlive().
     *   8. stop() przerywał wątek w dowolnym miejscu i zwalniał jego blokady, zostawiając obiekty w połowie zmiany
     *      (widoczne dla innych wątków). interrupt() tylko ustawia flagę; wątek sam decyduje, kiedy bezpiecznie skończyć.
     */
    // </editor-fold>
}
