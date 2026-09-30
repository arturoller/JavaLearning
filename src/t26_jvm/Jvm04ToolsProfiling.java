package t26_jvm;

import helpers.Check;
import helpers.Sleep;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Narzędzia JDK i profilowanie — jak zajrzeć do działającej JVM
 *        (profiling = profilowanie, czyli mierzenie, gdzie program traci czas i pamięć; tool = narzędzie)
 *
 * W SKRÓCIE:
 *   JDK ma w katalogu bin gotowe narzędzia diagnostyczne: listę procesów (jps), zdalne komendy (jcmd), zrzuty wątków
 *   i sterty, statystyki GC oraz rejestrator zdarzeń JFR. Część tych danych odczytasz też z kodu przez MXBeany.
 *   Zasada: najpierw MIERZ, potem zgaduj i poprawiaj.
 *
 * ANALOGIA: warsztat samochodowy.
 *   jps = spis aut na placu; jcmd = komputer diagnostyczny podpinany do gniazda OBD; zrzut wątków = zdjęcie, kto
 *   w tej chwili co robi; zrzut sterty = rozebranie auta na części i zważenie ich; JFR = rejestrator jazdy, który
 *   nagrywa wszystko po trochu, tanio i bez zatrzymywania auta.
 *
 * JAK TO DZIAŁA:
 *   narzędzie    | do czego                                            | przykład
 *   jps          | lista procesów Javy: PID i klasa główna             | jps -l
 *   jcmd         | komendy wysyłane do działającej JVM (zalecane)      | jcmd PID help   /   jcmd PID VM.flags
 *   jstack       | zrzut wątków (thread dump) — to samo co Thread.print | jstack PID
 *   jmap         | histogram obiektów, zrzut sterty (heap dump)        | jmap -histo:live PID
 *   jstat        | statystyki GC co N ms (zajętość Eden/Old, liczby GC) | jstat -gcutil PID 1000
 *   jconsole     | okienkowy podgląd JMX (wątki, pamięć, MXBeany)       | jconsole
 *   VisualVM     | okienkowy monitor i profiler; od JDK 9 osobne pobranie | visualvm
 *   JFR + jfr    | Java Flight Recorder: nagranie zdarzeń, narzędzie jfr | jfr summary nagranie.jfr
 *   JMC          | JDK Mission Control: analiza nagrań JFR; osobne pobranie | jmc
 *   Narzędzia wiersza poleceń działają lokalnie, na procesach tego samego użytkownika; zdalnie łączy się przez JMX.
 *
 * SŁÓWKA:
 *   dump = zrzut; thread dump = zrzut wątków; heap dump = zrzut sterty; histogram = zestawienie liczności;
 *   flight recorder = rejestrator lotu; sample = próbka; hot = gorący (często wykonywany); warm-up = rozgrzewka;
 *   tier = poziom; deoptimization = deoptymalizacja; deadlock = zakleszczenie; bean = ziarno (tu: obiekt zarządzania);
 *   uptime = czas działania; retained size = rozmiar zatrzymany (ile pamięci zwolni usunięcie obiektu).
 *
 * ZOBACZ TEŻ: t26_jvm/Jvm01Memory (obszary pamięci), t26_jvm/Jvm03GarbageCollection (logi GC, ścieżka do korzenia),
 *             t21_concurrency/Concurrency03Locks (blokady i zakleszczenia od strony kodu).
 * </pre>
 */
public class Jvm04ToolsProfiling {

    public static void main(String[] args) throws InterruptedException {
        title("Jvm04 — narzędzia JDK i profilowanie");

        thisProcess();          // this process = ten proces
        runtimeInfo();          // runtime info = informacje o środowisku uruchomieniowym
        threadsAndDumps();      // threads and dumps = wątki i zrzuty wątków
        deadlockDetection();    // deadlock detection = wykrywanie zakleszczeń
        memoryDiagnostics();    // memory diagnostics = diagnostyka pamięci
        jitCompilation();       // JIT compilation = kompilacja w locie (just-in-time)
        hotLoopDemo();          // hot loop demo = pokaz gorącej pętli
        slowOrLeaking();        // slow or leaking = wolno działa albo cieknie
        exercises();            // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. TEN PROCES — PID I KOMENDY jcmd
    // =================================================================================================

    /**
     * 1. Każde narzędzie zaczyna od PID (process id = identyfikator procesu). Znajdziesz go przez jps albo samo
     * jcmd bez argumentów — a z kodu przez ProcessHandle (Java 9+).
     */
    static void thisProcess() {
        section("1. Ten proces — PID i komendy jcmd");

        long pid = ProcessHandle.current().pid();     // ProcessHandle = uchwyt procesu; current = bieżący
        show("ProcessHandle.current().pid() > 0", pid > 0);
        // WYNIK: ProcessHandle.current().pid() > 0 → true

        note("komendy dla tego procesu: jcmd " + pid + " VM.flags   |   jcmd " + pid + " Thread.print");
        // (wynik zależy od uruchomienia) — PID jest inny przy każdym starcie

        // Najważniejsze komendy jcmd (lista dla konkretnej JVM: jcmd PID help):
        //   VM.version, VM.flags, VM.command_line   — wersja, flagi, pełna linia uruchomienia
        //   VM.system_properties, VM.uptime          — właściwości systemowe, czas działania
        //   GC.heap_info, GC.class_histogram          — stan sterty, liczba obiektów każdej klasy
        //   GC.heap_dump C:\tmp\heap.hprof            — zrzut sterty do pliku (podaj pełną ścieżkę)
        //   Thread.print                              — zrzut wątków (jak jstack)
        //   JFR.start, JFR.dump, JFR.stop             — nagrywanie Flight Recorderem
        //   VM.native_memory summary                  — pamięć natywna (wymaga -XX:NativeMemoryTracking=summary)
        // PUŁAPKA: jcmd i jps zwykle „nie widzą” procesu uruchomionego przez innego użytkownika systemu (np. usługi
        //   działającej na osobnym koncie) — bo dołączają się (attach) przez lokalny mechanizm dostępny dla
        //   właściciela procesu. Uruchom je jako ten sam użytkownik, zamiast szukać błędu w aplikacji.
        // DOBRA PRAKTYKA: zamiast jstack/jmap/jinfo używaj jcmd — Oracle zaleca je jako jedno, nowsze narzędzie
        //   z pełną listą komend (help), więc nie musisz pamiętać wielu programów i ich flag.
    }

    // =================================================================================================
    // 2. INFORMACJE O JVM Z KODU — MXBeany
    // =================================================================================================

    /**
     * 2. ManagementFactory (fabryka obiektów zarządzania) daje MXBeany — obiekty z danymi o JVM. Te same dane
     * pokazują jconsole i VisualVM (przez JMX). Wartości zależą od maszyny, więc wypisujemy tylko pewne fakty.
     */
    static void runtimeInfo() {
        section("2. Informacje o JVM z kodu — MXBeany");

        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();   // obiekt zarządzania środowiskiem uruchomieniowym
        show("getVmName() zawiera \"VM\"", runtime.getVmName().contains("VM"));   // VM name = nazwa maszyny wirtualnej
        show("Runtime.version().feature() >= 17", Runtime.version().feature() >= 17);   // feature (Java 10+) = numer wersji
        show("getUptime() >= 0", runtime.getUptime() >= 0);                            // uptime w ms od startu JVM
        show("załadowane klasy > 0", ManagementFactory.getClassLoadingMXBean().getLoadedClassCount() > 0);   // loaded class count = liczba załadowanych klas
        show("procesory >= 1", ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors() >= 1);   // operating system = system operacyjny
        // WYNIK: getVmName() zawiera "VM" → true
        // WYNIK: Runtime.version().feature() >= 17 → true
        // WYNIK: getUptime() >= 0 → true
        // WYNIK: załadowane klasy > 0 → true
        // WYNIK: procesory >= 1 → true

        note("VM: " + runtime.getVmName() + ", wersja " + Runtime.version());
        note("argumenty JVM: " + runtime.getInputArguments());   // getInputArguments = flagi podane przy starcie
        // (wynik zależy od uruchomienia) — np. „OpenJDK 64-Bit Server VM, wersja 17.0.16+8” i lista flag z IDE

        // PUŁAPKA: kod, który „wykrywa wersję” przez parsowanie System.getProperty("java.version"), psuje się na
        //   formatach typu "1.8.0_392" kontra "17.0.16" — Runtime.version() zwraca gotowe liczby (feature, update).
        // DOBRA PRAKTYKA: przy starcie aplikacji zaloguj wersję JVM i jej argumenty (jak wyżej). Gdy przyjdzie
        //   zgłoszenie „u klienta działa wolno”, od razu wiesz, na czym i z jakimi flagami to działało.
    }

    // =================================================================================================
    // 3. WĄTKI I ZRZUT WĄTKÓW
    // =================================================================================================

    /** awaitQuietly = czekaj „po cichu”: await na zatrzasku, przerwanie tylko przywraca flagę. */
    static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();                                // await = czekaj, aż licznik spadnie do zera
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** waitForState = poczekaj na stan: sprawdza stan wątku co 1 ms, maksymalnie 5 sekund. */
    static Thread.State waitForState(Thread thread, Thread.State expected) {
        for (int attempt = 0; attempt < 5_000 && thread.getState() != expected; attempt++) {
            Sleep.ms(1);
        }
        return thread.getState();
    }

    /**
     * 3. Zrzut wątków (jcmd PID Thread.print albo jstack PID) to lista wszystkich wątków z ich stanem i stosem
     * wywołań. Te same informacje daje ThreadMXBean — pokazujemy wątek, który czeka na zatrzasku.
     */
    static void threadsAndDumps() throws InterruptedException {
        section("3. Wątki i zrzut wątków");

        ThreadMXBean threads = ManagementFactory.getThreadMXBean();     // obiekt zarządzania wątkami
        show("getThreadCount() > 0", threads.getThreadCount() > 0);    // thread count = liczba wątków
        show("findDeadlockedThreads() == null (brak zakleszczeń)", threads.findDeadlockedThreads() == null);   // znajdź zakleszczone wątki
        // WYNIK: getThreadCount() > 0 → true
        // WYNIK: findDeadlockedThreads() == null (brak zakleszczeń) → true

        CountDownLatch orderArrived = new CountDownLatch(1);   // order arrived = zamówienie przyszło; zatrzask z licznikiem
        Thread waiter = new Thread(() -> awaitQuietly(orderArrived), "czekający-na-zamówienie");   // waiter = czekający
        waiter.setDaemon(true);                                 // daemon = wątek w tle, nie blokuje końca programu
        waiter.start();
        waitForState(waiter, Thread.State.WAITING);
        ThreadInfo info = threads.getThreadInfo(waiter.getId()); // ThreadInfo = informacja o wątku (jak w zrzucie)
        show("wątek i stan", info.getThreadName() + " → " + info.getThreadState());
        // WYNIK: wątek i stan → czekający-na-zamówienie → WAITING

        orderArrived.countDown();                               // countDown = odlicz w dół (tu: do zera)
        waiter.join();                                          // join = poczekaj na zakończenie wątku
        show("stan po countDown() i join()", waiter.getState());
        // WYNIK: stan po countDown() i join() → TERMINATED

        // Ten sam wątek w zrzucie z jcmd PID Thread.print wyglądałby mniej więcej tak (skrócone; adresy, czasy
        // i numery zależą od uruchomienia):
        //   "czekający-na-zamówienie" #15 daemon prio=5 ... waiting on condition
        //      java.lang.Thread.State: WAITING (parking)
        //           at jdk.internal.misc.Unsafe.park(java.base@17.../Native Method)
        //           - parking to wait for  <0x...> (a java.util.concurrent.CountDownLatch$Sync)
        //           at java.util.concurrent.CountDownLatch.await(...)
        // Stany: RUNNABLE (liczy albo czeka na I/O!), BLOCKED (czeka na synchronized), WAITING / TIMED_WAITING
        //   (await, join, sleep, pula wątków bez zadań), NEW, TERMINATED.
        // PUŁAPKA: RUNNABLE nie znaczy „zużywa CPU” — wątek czytający z gniazda sieciowego też jest RUNNABLE.
        //   Dlatego jeden zrzut niewiele mówi: rób 3–5 zrzutów co kilka sekund i porównuj.
        // DOBRA PRAKTYKA: nadawaj wątkom i pulom czytelne nazwy (jak "czekający-na-zamówienie"). W zrzucie
        //   z 300 wątkami nazwa "pool-7-thread-12" nic nie mówi, a "import-faktur-3" — od razu wiadomo, czyj to wątek.
    }

    // =================================================================================================
    // 4. WYKRYWANIE ZAKLESZCZEŃ
    // =================================================================================================

    /** lockInOrder = zablokuj w kolejności: bierze pierwszą blokadę, czeka na drugi wątek, potem sięga po drugą. */
    static void lockInOrder(Object firstLock, Object secondLock, CountDownLatch bothHoldFirst) {
        synchronized (firstLock) {
            bothHoldFirst.countDown();
            awaitQuietly(bothHoldFirst);           // oba wątki trzymają już swoją pierwszą blokadę
            synchronized (secondLock) {
                note("to się nie wypisze — zakleszczenie");
            }
        }
    }

    /**
     * 4. CELOWE zakleszczenie: dwa wątki biorą dwie blokady w odwrotnej kolejności. JVM potrafi je wykryć —
     * zrzut wątków kończy się wtedy sekcją „Found one Java-level deadlock”, a z kodu: findDeadlockedThreads().
     */
    static void deadlockDetection() {
        section("4. Wykrywanie zakleszczeń");

        Object stockLock = new Object();           // stock = magazyn
        Object paymentLock = new Object();         // payment = płatność
        CountDownLatch bothHoldFirst = new CountDownLatch(2);   // both hold first = oba trzymają pierwszą blokadę
        Thread first = new Thread(() -> lockInOrder(stockLock, paymentLock, bothHoldFirst), "zakleszczony-1");
        Thread second = new Thread(() -> lockInOrder(paymentLock, stockLock, bothHoldFirst), "zakleszczony-2");
        first.setDaemon(true);
        second.setDaemon(true);
        first.start();
        second.start();

        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        long[] deadlocked = null;                  // deadlocked = zakleszczone (identyfikatory wątków)
        for (int attempt = 0; attempt < 500 && deadlocked == null; attempt++) {
            Sleep.ms(10);
            deadlocked = threads.findDeadlockedThreads();   // null = brak zakleszczeń
        }
        if (deadlocked == null) {
            note("nie wykryto zakleszczenia w 5 s (nie powinno się zdarzyć)");
            return;
        }
        TreeSet<String> names = new TreeSet<>();
        for (ThreadInfo info : threads.getThreadInfo(deadlocked)) {
            names.add(info.getThreadName());
        }
        ThreadInfo firstInfo = threads.getThreadInfo(first.getId());
        show("zakleszczone wątki", names);
        show("stan zakleszczony-1", firstInfo.getThreadState());
        show("zakleszczony-1 czeka na blokadę wątku", firstInfo.getLockOwnerName());   // lock owner = właściciel blokady
        // WYNIK: zakleszczone wątki → [zakleszczony-1, zakleszczony-2]
        // WYNIK: stan zakleszczony-1 → BLOCKED
        // WYNIK: zakleszczony-1 czeka na blokadę wątku → zakleszczony-2

        // Te dwa wątki zostaną zablokowane do końca programu — są daemon, więc nie przeszkodzą mu się zakończyć.
        // W prawdziwej aplikacji to „zawieszenie”: CPU spada do zera, żądania wiszą. Pomaga restart i poprawka kodu.
        // PUŁAPKA: zakleszczenie zwykle nie wychodzi w testach, bo wymaga złego wyczucia czasu — a pod obciążeniem
        //   na produkcji pojawia się raz na kilka dni. Tu wymusiliśmy je zatrzaskiem, żeby było powtarzalne.
        // DOBRA PRAKTYKA: zawsze bierz blokady w TEJ SAMEJ, ustalonej kolejności (np. wg numeru konta) albo używaj
        //   tryLock z limitem czasu (t21_concurrency). Gdy aplikacja „wisi”, pierwszy krok to zrzut wątków.
    }

    // =================================================================================================
    // 5. DIAGNOSTYKA PAMIĘCI — STERTA, HISTOGRAM, ZRZUT
    // =================================================================================================

    /**
     * 5. Z kodu: MemoryMXBean pokazuje zajętość sterty. Z zewnątrz: jcmd GC.heap_info, GC.class_histogram
     * i zrzut sterty (heap dump) do analizy w VisualVM albo Eclipse MAT.
     */
    static void memoryDiagnostics() {
        section("5. Diagnostyka pamięci — sterta, histogram, zrzut");

        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();   // MemoryUsage = zużycie pamięci
        show("used <= committed", heap.getUsed() <= heap.getCommitted());   // used = używane; committed = zarezerwowane
        show("committed <= max (lub max nieokreślony = -1)", heap.getMax() == -1 || heap.getCommitted() <= heap.getMax());
        // WYNIK: used <= committed → true
        // WYNIK: committed <= max (lub max nieokreślony = -1) → true
        note(String.format(Locale.ROOT, "sterta: używane %d MB, zarezerwowane %d MB",
                heap.getUsed() >> 20, heap.getCommitted() >> 20));
        // (wynik zależy od uruchomienia)

        // Histogram (jcmd PID GC.class_histogram) — ile obiektów każdej klasy i ile bajtów zajmują:
        //    num     #instances         #bytes  class name (module)
        //      1:         52000        4160000  [B (java.base@17...)          ← [B = byte[] (wnętrze String)
        //      2:         48000        1152000  java.lang.String (java.base@17...)
        // Kilka histogramów w odstępach czasu pokazuje, KTÓRA klasa rośnie (ćwiczenie 2).
        // Zrzut sterty: jcmd PID GC.heap_dump C:\tmp\heap.hprof  albo automatycznie przy awarii:
        //   java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=C:\tmp ...
        // Analiza (VisualVM, Eclipse MAT): drzewo dominatorów (dominator tree) i rozmiar zatrzymany (retained size)
        //   wskazują obiekty, które trzymają najwięcej pamięci; „ścieżka do korzenia GC” mówi, KTO je trzyma (Jvm03).
        // Pamięć procesu rośnie, a sterta nie? → -XX:NativeMemoryTracking=summary + jcmd PID VM.native_memory summary.
        // PUŁAPKA: zrzut sterty zatrzymuje aplikację na czas zapisu (przy dużej stercie — sekundy) i ma rozmiar
        //   zbliżony do zajętej sterty. Zawiera też WSZYSTKIE dane z pamięci: hasła, tokeny, dane osobowe.
        // DOBRA PRAKTYKA: włącz -XX:+HeapDumpOnOutOfMemoryError na produkcji (dowód zbierze się sam w chwili awarii),
        //   ale traktuj plik .hprof jak dane poufne — nie wysyłaj go mailem ani nie wrzucaj do zgłoszenia.
    }

    // =================================================================================================
    // 6. KOMPILACJA JIT — INTERPRETER → C1 → C2
    // =================================================================================================

    /**
     * 6. HotSpot najpierw INTERPRETUJE bajtkod. Metody i pętle wykonywane często („gorące”) kompiluje do kodu
     * maszynowego: najpierw szybko kompilatorem C1, a najgorętsze — mocno optymalizującym C2.
     */
    static void jitCompilation() {
        section("6. Kompilacja JIT — interpreter → C1 → C2");

        note("kompilator JIT: " + ManagementFactory.getCompilationMXBean().getName());
        // (wynik zależy od uruchomienia) — w HotSpot 17 zwykle „HotSpot 64-Bit Tiered Compilers”

        // Kompilacja warstwowa (tiered compilation, domyślna od Javy 8) — poziomy:
        //   0 = interpreter (zbiera statystyki: ile wywołań, które gałęzie, jakie typy)
        //   1–3 = C1 (szybka kompilacja; poziom 3 dodatkowo zbiera profil)
        //   4 = C2 (wolniejsza kompilacja, ale najszybszy kod — korzysta z zebranego profilu)
        // Optymalizacje C2: wklejanie małych metod (inlining), analiza ucieczki (escape analysis — Jvm01),
        //   rozwijanie pętli, usuwanie martwego kodu i zbędnych sprawdzeń zakresu tablic.
        // Podgląd: java -XX:+PrintCompilation ... — fragment z prawdziwego uruchomienia na JDK 17 (liczby u Ciebie
        //   będą inne):
        //       411 1298 %     3       Probe::hot @ 4 (29 bytes)
        //       411 1299       3       Probe::hot (29 bytes)
        //       487 1300 %     4       Probe::hot @ 4 (29 bytes)
        //       489 1298 %     3       Probe::hot @ 4 (29 bytes)   made not entrant
        //   kolumny: ms od startu | numer kompilacji | znaczniki | poziom | metoda | rozmiar bajtkodu.
        //   % = OSR (on-stack replacement = podmiana w trakcie działania): pętla skompilowana, gdy jeszcze się
        //   kręci; „@ 4” to miejsce w bajtkodzie. „made not entrant” = ta wersja nie przyjmuje nowych wywołań —
        //   zastąpiła ją lepsza (poziom 4) albo założenie optymalizacji przestało być prawdziwe (deoptymalizacja).
        // Flagi do eksperymentów: -Xint (tylko interpreter — dużo wolniej), -XX:TieredStopAtLevel=1 (tylko C1 —
        //   szybszy start, wolniejszy szczyt), -XX:ReservedCodeCacheSize (miejsce na skompilowany kod, Jvm01).
        // PUŁAPKA: kod działa wolno w pierwszych sekundach po starcie (rozgrzewka: interpreter + kompilacja).
        //   Pomiar „na zimno” mówi o starcie aplikacji, a nie o jej prędkości w stałej pracy.
        // DOBRA PRAKTYKA: nie optymalizuj „pod JIT” na ślepo (ręczne rozwijanie pętli itp.) — C2 robi to lepiej.
        //   Pisz prosty, czytelny kod z małymi metodami: takie JIT najłatwiej wkleja i optymalizuje.
    }

    // =================================================================================================
    // 7. GORĄCA PĘTLA — CO ZOBACZĄ NARZĘDZIA
    // =================================================================================================

    /** mix = wymieszaj: mała metoda wołana miliony razy — idealny kandydat do wklejenia (inlining) przez JIT. */
    static long mix(int value) {
        return (value * 31L) ^ (value >>> 3);
    }

    /** hotLoop = gorąca pętla: suma kontrolna z iterations wywołań mix. */
    static long hotLoop(int iterations) {
        long checksum = 0;
        for (int i = 0; i < iterations; i++) {
            checksum += mix(i);
        }
        return checksum;
    }

    /**
     * 7. Pięć rund tej samej pracy (łącznie 10 mln wywołań, poniżej sekundy). Wynik jest zawsze ten sam, ale CZAS
     * rund zależy od maszyny i od tego, czy JIT zdążył już skompilować hotLoop — dlatego czasów nie traktujemy jako pewnego wyniku.
     */
    static void hotLoopDemo() {
        section("7. Gorąca pętla — co zobaczą narzędzia");

        List<Long> checksums = new ArrayList<>();        // checksums = sumy kontrolne
        List<String> timings = new ArrayList<>();        // timings = czasy rund
        for (int round = 1; round <= 5; round++) {
            long start = System.nanoTime();              // nanoTime = zegar do mierzenia odcinków czasu
            checksums.add(hotLoop(2_000_000));
            long micros = (System.nanoTime() - start) / 1_000;
            timings.add("runda " + round + ": " + micros + " µs");
        }
        show("suma kontrolna", checksums.get(0));
        show("wszystkie rundy dały ten sam wynik", checksums.stream().distinct().count() == 1);   // distinct = bez powtórzeń
        // WYNIK: suma kontrolna → 62000521755456
        // WYNIK: wszystkie rundy dały ten sam wynik → true
        note(String.join(", ", timings));
        // (wynik zależy od uruchomienia) — zwykle pierwsza runda jest wyraźnie wolniejsza (rozgrzewka JIT)

        // Jak to podejrzeć:
        //   • JIT:  java -XX:+PrintCompilation -cp KATALOG_KLAS t26_jvm.Jvm04ToolsProfiling | findstr Jvm04
        //           (Linux/macOS: | grep Jvm04) → zobaczysz hotLoop i mix na poziomach 3 i 4, pętlę z % (OSR).
        //   • Porównanie: to samo z -Xint — rundy trwają wielokrotnie dłużej (sam interpreter).
        //   • JFR:  java -XX:StartFlightRecording=duration=30s,filename=hot.jfr ...  a potem
        //           jfr summary hot.jfr   albo   jfr print --events jdk.ExecutionSample hot.jfr
        //           (próbki stosów: które metody najczęściej były na szczycie) — graficznie w JMC.
        //   • Zrzut wątków w trakcie: zwiększ liczbę rund do kilku tysięcy i wywołaj jcmd PID Thread.print —
        //           wątek main będzie RUNNABLE z hotLoop/mix na szczycie stosu.
        // PUŁAPKA: taki ręczny pomiar to NIE jest wiarygodny benchmark: rozgrzewka, kompilacja w tle, GC i usunięcie
        //   „niepotrzebnego” kodu przez JIT (gdyby wynik nie był używany) potrafią zmienić czas wielokrotnie.
        // DOBRA PRAKTYKA: do mikro-pomiarów używaj JMH (Java Microbenchmark Harness) — robi rozgrzewkę, powtórzenia
        //   i statystykę. Do szukania wąskich gardeł w całej aplikacji — profilera (JFR, VisualVM, async-profiler).
    }

    // =================================================================================================
    // 8. APLIKACJA ZWALNIA ALBO CIEKNIE — OD CZEGO ZACZĄĆ
    // =================================================================================================

    /** heapTrend = trend sterty: czy zajętość PO kolejnych pełnych zbiórkach stale rośnie (wzór wycieku). */
    static String heapTrend(List<Integer> heapAfterFullGcMb) {
        for (int i = 1; i < heapAfterFullGcMb.size(); i++) {
            if (heapAfterFullGcMb.get(i) <= heapAfterFullGcMb.get(i - 1)) {
                return "stabilnie → raczej brak wycieku";
            }
        }
        return "stale rośnie → podejrzenie wycieku";
    }

    /**
     * 8. Diagnoza to lista kontrolna, a nie zgadywanie. Przy wyciekach patrz na „dolną linię piły”: zajętość
     * sterty tuż PO zbiórkach. Jeśli ona rośnie z każdą zbiórką — coś trzyma obiekty (Jvm01, Jvm03).
     */
    static void slowOrLeaking() {
        section("8. Aplikacja zwalnia albo cieknie — od czego zacząć");

        show("po pełnych GC [120, 180, 240, 310] MB", heapTrend(List.of(120, 180, 240, 310)));
        show("po pełnych GC [120, 118, 125, 119] MB", heapTrend(List.of(120, 118, 125, 119)));
        // WYNIK: po pełnych GC [120, 180, 240, 310] MB → stale rośnie → podejrzenie wycieku
        // WYNIK: po pełnych GC [120, 118, 125, 119] MB → stabilnie → raczej brak wycieku

        // WOLNO DZIAŁA:
        //   1. CPU wysokie? → profil CPU: JFR (jdk.ExecutionSample) albo VisualVM/async-profiler; kilka zrzutów
        //      wątków — te same metody na szczycie stosu to gorący kod.
        //   2. CPU niskie, a wolno? → wątki CZEKAJĄ: zrzut wątków — BLOCKED (blokady), WAITING/TIMED_WAITING
        //      (pula połączeń do bazy, zdalne wywołania, kolejki), zakleszczenie (sekcja 4).
        //   3. GC? → -Xlog:gc albo jstat -gcutil PID 1000: długie/częste pauzy, Pause Full, rosnące kolumny FGC/GCT.
        //   4. Dopiero z tą wiedzą — kod: algorytm (t24_algorithms), zapytania do bazy, zbędne kopiowanie danych.
        // CIEKNIE PAMIĘĆ:
        //   1. Trend sterty po pełnych GC (jak wyżej; dane z logów GC albo z jstat).
        //   2. Kilka histogramów (jcmd PID GC.class_histogram) w odstępach — która klasa rośnie (ćwiczenie 2).
        //   3. Zrzut sterty → MAT/VisualVM: dominatory i ścieżka do korzenia GC — kto trzyma te obiekty.
        //   4. Rośnie metaspace → wyciek class loaderów (Jvm02); rośnie proces, a nie sterta → NMT, wątki, bufory.
        // PUŁAPKA: „dołóżmy -Xmx, to przestanie padać” — przy wycieku tylko opóźnia OutOfMemoryError i wydłuża
        //   pauzy GC (więcej sterty do przejrzenia). Najpierw znajdź, kto trzyma obiekty.
        // DOBRA PRAKTYKA: zbieraj dane, zanim zrestartujesz aplikację (zrzut wątków, histogram, logi GC) — restart
        //   usuwa objawy i dowody; kolejna szansa na diagnozę pojawi się dopiero przy następnej awarii.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • jps -l → PID; jcmd PID help → lista komend; jcmd zastępuje jstack/jmap/jinfo.
     *   • Wątki: jcmd PID Thread.print (jstack); zakleszczenie → „Found one Java-level deadlock”; rób kilka zrzutów.
     *   • Pamięć: GC.heap_info, GC.class_histogram, GC.heap_dump plik.hprof; -XX:+HeapDumpOnOutOfMemoryError.
     *   • GC na żywo: jstat -gcutil PID 1000; logi: -Xlog:gc (Jvm03).
     *   • JFR: -XX:StartFlightRecording=duration=30s,filename=x.jfr albo jcmd PID JFR.start; jfr summary / jfr print.
     *   • JIT: interpreter (0) → C1 (1–3) → C2 (4); -XX:+PrintCompilation: % = OSR, „made not entrant” = wycofana wersja.
     *   • Z kodu: ManagementFactory.get...MXBean(): Runtime, Thread, Memory, ClassLoading, Compilation, GarbageCollector.
     *   • ProcessHandle.current().pid() (Java 9+), Runtime.version().feature() (Java 10+).
     *   • Benchmark → JMH; szukanie wąskich gardeł → profiler; nigdy pojedynczy pomiar „na zimno”.
     *
     * PYTANIA KONTROLNE:
     *   1. Którym narzędziem znajdziesz PID aplikacji, a którą komendą jcmd zrobisz zrzut wątków?
     *   2. Co oznacza znak % w wyjściu -XX:+PrintCompilation, a co dopisek „made not entrant”?
     *   3. Co wypisze na JDK 17:  System.out.println(Runtime.version().feature() >= 17);  ?
     *   4. ZNAJDŹ BŁĄD:  long t = System.currentTimeMillis(); compute(); System.out.println(System.currentTimeMillis() - t);
     *      — wynik 0, więc wniosek: „compute() jest błyskawiczne”.
     *   5. Aplikacja „wisi”, CPU prawie 0%. Co zrobisz najpierw i czego będziesz szukać?
     *   6. ZNAJDŹ BŁĄD:  po awarii zespół wysyła plik heap.hprof z produkcji mailem do zewnętrznej firmy.
     *   7. Po czym w danych GC odróżnisz wyciek pamięci od zwykłej „piły” (rośnie — spada po GC)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static final List<String> HISTOGRAM_BEFORE = List.of(
            " num     #instances         #bytes  class name (module)",
            "-------------------------------------------------------",
            "   1:         52000        4160000  [B (java.base@17.0.16)",
            "   2:         48000        1152000  java.lang.String (java.base@17.0.16)",
            "   3:          1200          38400  com.shop.Session",
            "   4:           900          28800  com.shop.Order");

    static final List<String> HISTOGRAM_AFTER = List.of(
            " num     #instances         #bytes  class name (module)",
            "-------------------------------------------------------",
            "   1:         55000        4400000  [B (java.base@17.0.16)",
            "   2:         49500        1188000  java.lang.String (java.base@17.0.16)",
            "   3:          9200         294400  com.shop.Session",
            "   4:           950          30400  com.shop.Order");

    static void exercises() {
        String[] needs = "flagi sterta wątki histogram".split(" ");    // needs = potrzeby; teksty z czasu działania!
        List<String> expectedCommands = List.of("jcmd 4242 VM.flags", "jcmd 4242 GC.heap_info",
                "jcmd 4242 Thread.print", "jcmd 4242 GC.class_histogram");
        Map<String, String> waitsFor = Map.of("worker-1", "worker-2", "worker-2", "worker-3",
                "worker-3", "worker-1", "http-7", "worker-2");       // waits for = czeka na (blokadę) wątku
        Map<String, String> noCycle = Map.of("a", "b", "b", "c");
        List<List<String>> expectedCycles = List.of(List.of("worker-1", "worker-2", "worker-3"), List.of());

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        // toList() (Java 16+) = zbierz strumień do niemodyfikowalnej listy
        Check.equal("ćw. 1: komendy jcmd", expectedCommands, () -> Arrays.stream(needs).map(n -> exercise1(4242, n)).toList());
        Check.equal("ćw. 2: klasa, która rośnie", "com.shop.Session (+8000)",
                () -> exercise2(HISTOGRAM_BEFORE, HISTOGRAM_AFTER));
        Check.equal("ćw. 3: cykl zakleszczenia", expectedCycles,
                () -> List.of(exercise3(waitsFor), exercise3(noCycle)));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", expectedCommands, () -> Arrays.stream(needs).map(n -> solution1(4242, n)).toList());
        Check.equal("ćw. 2 (wzorzec)", "com.shop.Session (+8000)", () -> solution2(HISTOGRAM_BEFORE, HISTOGRAM_AFTER));
        Check.equal("ćw. 3 (wzorzec)", expectedCycles, () -> List.of(solution3(waitsFor), solution3(noCycle)));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 3 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ wybór komendy jcmd na switch expression (Java 14+).
     * Stary sposób (obecny kod) ma dwa problemy: rozwlekłość i porównanie napisów przez == (Jvm01, sekcja 5) —
     * dla tekstów z czasu działania (tu: ze split) == zwraca false, więc wszystko kończy się na "nieznane".
     * <pre>{@code
     * if (need == "flagi") { return "jcmd " + pid + " VM.flags"; } else if (need == "sterta") { ... } ...
     * }</pre>
     * Nowy sposób: {@code return "jcmd " + pid + " " + switch (need) { case "flagi" -> "VM.flags"; ... };}
     * Mapowanie: flagi → VM.flags, sterta → GC.heap_info, wątki → Thread.print, histogram → GC.class_histogram,
     * inne → "nieznane" (default). Podpowiedź: switch na String porównuje przez equals — tak jak trzeba.
     */
    static String exercise1(long pid, String need) {
        // TODO: przepisz na switch expression (IDE słusznie podkreśla == na napisach — to celowy błąd do naprawy)
        if (need == "flagi") {
            return "jcmd " + pid + " VM.flags";
        } else if (need == "sterta") {
            return "jcmd " + pid + " GC.heap_info";
        } else if (need == "wątki") {
            return "jcmd " + pid + " Thread.print";
        } else if (need == "histogram") {
            return "jcmd " + pid + " GC.class_histogram";
        }
        return "nieznane";
    }

    /**
     * ĆWICZENIE 2 (średnie): porównaj dwa histogramy obiektów (w formacie jcmd GC.class_histogram) i zwróć klasę,
     * której liczba obiektów (#instances) wzrosła najbardziej, w formacie "nazwa.klasy (+przyrost)".
     * Dla danych testowych: "com.shop.Session (+8000)" — podejrzany numer 1 przy szukaniu wycieku.
     * Podpowiedź: pomiń dwie linie nagłówka; linia.trim().split("\\s+") da [numer:, liczba, bajty, klasa, ...].
     * Zbuduj {@code Map<String, Long>} klasa → liczba dla „przed”, potem przejdź po „po” i szukaj największej różnicy.
     */
    static String exercise2(List<String> before, List<String> after) {
        // TODO: twoje rozwiązanie
        return "";
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): tak działa findDeadlockedThreads. Dostajesz mapę „wątek → wątek, który trzyma
     * blokadę, na którą ten czeka”. Znajdź cykl i zwróć go od wątku o alfabetycznie najmniejszej nazwie
     * (np. [worker-1, worker-2, worker-3]); gdy cyklu nie ma — pustą listę. http-7 też czeka, ale nie jest w cyklu.
     * Podpowiedź: dla każdego wątku start (w kolejności alfabetycznej — TreeSet z kluczy) idź po mapie, zapisując
     * odwiedzone; jeśli wrócisz do start — to cykl; jeśli trafisz na null albo na inny odwiedzony wątek — to nie on.
     */
    static List<String> exercise3(Map<String, String> waitsFor) {
        // TODO: twoje rozwiązanie
        return List.of();
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(long pid, String need) {
        return "jcmd " + pid + " " + switch (need) {
            case "flagi" -> "VM.flags";
            case "sterta" -> "GC.heap_info";
            case "wątki" -> "Thread.print";
            case "histogram" -> "GC.class_histogram";
            default -> "nieznane";
        };
    }

    /** parseHistogram = przeczytaj histogram: klasa → liczba obiektów (pomija nagłówek). */
    private static Map<String, Long> parseHistogram(List<String> lines) {
        Map<String, Long> instances = new HashMap<>();
        for (String line : lines.subList(2, lines.size())) {
            String[] columns = line.trim().split("\\s+");
            instances.put(columns[3], Long.parseLong(columns[1]));
        }
        return instances;
    }

    static String solution2(List<String> before, List<String> after) {
        Map<String, Long> old = parseHistogram(before);
        String winner = "";
        long biggestGrowth = Long.MIN_VALUE;
        for (Map.Entry<String, Long> entry : parseHistogram(after).entrySet()) {
            long growth = entry.getValue() - old.getOrDefault(entry.getKey(), 0L);
            if (growth > biggestGrowth) {
                biggestGrowth = growth;
                winner = entry.getKey();
            }
        }
        return winner + " (+" + biggestGrowth + ")";
    }

    static List<String> solution3(Map<String, String> waitsFor) {
        for (String start : new TreeSet<>(waitsFor.keySet())) {
            List<String> path = new ArrayList<>();
            String current = start;
            while (current != null && !path.contains(current)) {
                path.add(current);
                current = waitsFor.get(current);
            }
            if (start.equals(current)) {
                return path;                         // wróciliśmy do startu → cykl
            }
        }
        return List.of();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. PID: jps -l (albo samo jcmd bez argumentów). Zrzut wątków: jcmd PID Thread.print (odpowiednik jstack PID).
     *   2. % = kompilacja OSR — pętla skompilowana i podmieniona w trakcie działania metody. „made not entrant” =
     *      ta skompilowana wersja nie przyjmuje nowych wywołań: zastąpiła ją lepsza albo nastąpiła deoptymalizacja.
     *   3. true — na JDK 17 feature() zwraca 17.
     *   4. currentTimeMillis ma słabą rozdzielczość i służy do „godziny”, nie do mierzenia odcinków (do tego nanoTime).
     *      Pojedynczy pomiar „na zimno” pomija rozgrzewkę JIT, a JIT może w ogóle usunąć obliczenia, których wyniku
     *      nikt nie używa. Wiarygodny pomiar: JMH (rozgrzewka, powtórzenia, statystyka).
     *   5. Kilka zrzutów wątków (jcmd PID Thread.print) co kilka sekund. Szukasz: „Found one Java-level deadlock”,
     *      wielu wątków BLOCKED na tej samej blokadzie, wątków WAITING na pulę połączeń / zdalne wywołanie.
     *   6. Zrzut sterty zawiera wszystkie dane z pamięci (hasła, tokeny, dane osobowe klientów). Należy go chronić
     *      jak bazę produkcyjną: analizować na zaufanej maszynie, nie wysyłać otwartym kanałem.
     *   7. W „pile” zajętość PO zbiórce wraca do podobnego poziomu. Przy wycieku dolna linia (po pełnych GC) rośnie
     *      z każdą zbiórką, pauzy się wydłużają, aż do OutOfMemoryError.
     */
    // </editor-fold>
}
