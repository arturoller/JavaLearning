package t30_build_modules;

import helpers.Check;
import helpers.TempDir;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Procesy potomne, zmienne środowiskowe i właściwości systemowe
 *        (process = proces, environment variable = zmienna środowiskowa, system property = właściwość systemowa)
 *
 * W SKRÓCIE:
 *   ProcessBuilder (budowniczy procesu) uruchamia inny program: podajesz listę [program, argumenty...],
 *   katalog roboczy i zmienne środowiskowe, a potem czytasz jego wyjście i kod wyjścia.
 *   System.getenv czyta zmienne systemu operacyjnego, System.getProperty — właściwości JVM (np. z -Dklucz=wartość).
 *
 * ANALOGIA:
 *   Zlecenie dla podwykonawcy. Dajesz mu instrukcję (polecenie i argumenty), adres warsztatu (katalog roboczy)
 *   i teczkę z dokumentami (kopia zmiennych środowiskowych). Musisz odbierać jego raporty na bieżąco
 *   (czytać wyjście), bo gdy skrzynka na raporty się zapełni, podwykonawca stanie i będzie czekał.
 *   Na końcu dostajesz ocenę: kod wyjścia.
 *
 * JAK TO DZIAŁA:
 *   ProcessBuilder pb = new ProcessBuilder(List.of(java, "-cp", cp, "Main"));
 *   pb.directory(katalog); pb.environment().put("KLUCZ", "wartość"); pb.redirectErrorStream(true);
 *   Process p = pb.start();               → dziecko działa RÓWNOLEGLE z nami
 *   p.getInputStream()                    → jego stdout (dla nas to wejście!)
 *   p.waitFor(10, SECONDS)                → czekaj z limitem czasu; potem p.exitValue()
 *   p.destroy() / p.destroyForcibly()     → poproś o zakończenie / zabij
 *
 * SŁÓWKA:
 *   child process = proces potomny; parent = rodzic; working directory = katalog roboczy; redirect = przekierowanie;
 *   inherit = dziedziczyć; deadlock = zakleszczenie; destroy = zniszczyć; timeout = limit czasu;
 *   handle = uchwyt; injection = wstrzyknięcie (atak); shell = powłoka; token = kawałek tekstu.
 *
 * ZOBACZ TEŻ: t30_build_modules/Build04CommandLineApps (strona dziecka: args, kody wyjścia),
 *             t21_concurrency/Concurrency05CompletableFuture (onExit, czytanie w tle), t18_io_files/Io06Properties,
 *             t31_jdk_toolbox/Toolbox02HashingSecurity (bezpieczeństwo).
 * </pre>
 */
public class Build05ProcessesEnv {

    static final String JAVA = Path.of(System.getProperty("java.home"), "bin", "java").toString();

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].startsWith("--dziecko-")) {
            childMode(args[0]);                                // tryby procesu potomnego (ta sama klasa w innym JVM)
            return;
        }
        title("Build05 — procesy, środowisko, właściwości");

        Path work = TempDir.create("build05");
        try {
            processBuilderBasics(work);        // process builder basics = podstawy ProcessBuilder
            redirects(work);                   // redirects = przekierowania
            deadlockPitfall();                 // deadlock pitfall = pułapka zakleszczenia
            destroyAndHandles();               // destroy and handles = przerywanie i uchwyty procesów
            inheritIo();                       // inherit IO = dziedziczenie wejścia/wyjścia
        } finally {
            TempDir.deleteRecursively(work);
        }
        envVsProperties();                     // env vs properties = zmienne środowiskowe a właściwości
        runtimeExecPitfalls();                 // Runtime.exec pitfalls = pułapki Runtime.exec
        shellAndSecurity();                    // shell and security = powłoka i bezpieczeństwo
        exercises();                           // exercises = ćwiczenia
    }

    // =================================================================================================
    // TRYBY DZIECKA I NARZĘDZIA POMOCNICZE
    // =================================================================================================

    static void childMode(String mode) throws InterruptedException {
        switch (mode) {
            case "--dziecko-env" -> {
                System.out.println("KURS_LEKCJA=" + System.getenv("KURS_LEKCJA"));
                System.out.println("kurs.tryb=" + System.getProperty("kurs.tryb"));
                System.out.println("znacznik.txt w katalogu roboczym: " + Files.exists(Path.of("znacznik.txt")));
                System.err.println("to poszło na stderr");
                System.exit(3);
            }
            case "--dziecko-duzo" -> {
                for (int i = 1; i <= 20_000; i++) {
                    System.out.println("wynik " + i);
                }
                for (int i = 1; i <= 5_000; i++) {
                    System.err.println("uwaga " + i);
                }
            }
            case "--dziecko-spij" -> Thread.sleep(60_000);
            case "--dziecko-ascii" -> System.out.println("[dziecko] pisze prosto na konsole rodzica");
            default -> System.exit(2);
        }
    }

    /** Polecenie uruchamiające TĘ klasę w osobnym JVM w podanym trybie. */
    static List<String> childCommand(String mode, String... jvmOptions) throws Exception {
        List<String> command = new ArrayList<>(List.of(JAVA, "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8",
                "-Dsun.stderr.encoding=UTF-8"));
        command.addAll(List.of(jvmOptions));
        Path own = Path.of(Build05ProcessesEnv.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        command.addAll(List.of("-cp", own + File.pathSeparator + System.getProperty("java.class.path"),
                Build05ProcessesEnv.class.getName(), mode));
        return command;
    }

    /** Linie bez „Picked up ...” — JVM dopisuje je na stderr, gdy ustawiono JAVA_TOOL_OPTIONS; to nie wynik programu. */
    static List<String> withoutPickedUp(List<String> lines) {
        return lines.stream().filter(l -> !l.startsWith("Picked up ") && !l.startsWith("NOTE: Picked up ")).toList();
    }

    // =================================================================================================
    // 1. ProcessBuilder: POLECENIE, KATALOG, ŚRODOWISKO, KOD WYJŚCIA
    // =================================================================================================

    /**
     * 1. Uruchamiamy dziecko z: dodatkową zmienną środowiskową, właściwością -D, własnym katalogiem roboczym.
     * Dziecko wypisuje, co widzi, i kończy się kodem 3. Czytamy CAŁE wyjście, dopiero potem czekamy na koniec.
     */
    static void processBuilderBasics(Path work) throws Exception {
        section("1. ProcessBuilder: polecenie, katalog, środowisko");

        Path dir = Files.createDirectories(work.resolve("warsztat"));
        Files.writeString(dir.resolve("znacznik.txt"), "jestem\n");

        ProcessBuilder pb = new ProcessBuilder(childCommand("--dziecko-env", "-Dkurs.tryb=test"));
        pb.directory(dir.toFile());                            // directory = katalog roboczy dziecka
        Map<String, String> env = pb.environment();            // KOPIA środowiska rodzica — zmiany dotyczą tylko dziecka
        env.put("KURS_LEKCJA", "Build05");
        pb.redirectErrorStream(true);                          // stderr dziecka doklejony do jego stdout
        show("rodzic widzi KURS_LEKCJA", System.getenv("KURS_LEKCJA") != null);
        // WYNIK: rodzic widzi KURS_LEKCJA → false

        Process process = pb.start();                          // start = uruchom
        List<String> lines;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            lines = withoutPickedUp(reader.lines().toList());  // czytamy do końca (EOF = dziecko zamknęło wyjście)
        }
        boolean finished = process.waitFor(10, TimeUnit.SECONDS);   // waitFor = czekaj (z limitem)
        if (!finished) {
            process.destroyForcibly();
        }
        showEach("wyjście dziecka", lines);
        // WYNIK: wyjście dziecka (liczba elementów: 4):
        // WYNIK: • KURS_LEKCJA=Build05
        // WYNIK: • kurs.tryb=test
        // WYNIK: • znacznik.txt w katalogu roboczym: true
        // WYNIK: • to poszło na stderr
        show("zakończył się w limicie", finished);
        // WYNIK: zakończył się w limicie → true
        show("kod wyjścia", process.exitValue());
        // WYNIK: kod wyjścia → 3

        // Zasady:
        //   • polecenie to LISTA: program + każdy argument osobno — bez cudzysłowów i bez powłoki,
        //   • program podawaj pełną ścieżką albo licz się z wyszukiwaniem w PATH (java.home/bin/java = pewniak),
        //   • środowisko dziecka = kopia środowiska rodzica + Twoje zmiany; rodzic się nie zmienia,
        //   • env.remove("X") usuwa zmienną tylko u dziecka; env.clear() to ryzyko — np. na Windows bez SystemRoot
        //     wiele programów nie wystartuje.
        // PUŁAPKA: process.exitValue() przed zakończeniem dziecka → IllegalThreadStateException.
        // Dlaczego: kodu jeszcze nie ma — najpierw waitFor.
    }

    // =================================================================================================
    // 2. PRZEKIEROWANIA: DO PLIKU, PRZECHWYCENIE, DZIEDZICZENIE
    // =================================================================================================

    /**
     * 2. Wyjście dziecka można: przechwycić (PIPE, domyślnie — czytasz strumień), przekierować do pliku
     * (Redirect.to / appendTo) albo odziedziczyć (INHERIT — dziecko pisze na Twoją konsolę).
     */
    static void redirects(Path work) throws Exception {
        section("2. Przekierowania: do pliku");

        Path out = work.resolve("wyniki.txt");
        Path err = work.resolve("uwagi.txt");
        Process process = new ProcessBuilder(childCommand("--dziecko-duzo"))
                .redirectOutput(ProcessBuilder.Redirect.to(out.toFile()))      // stdout → plik (nadpisz)
                .redirectError(ProcessBuilder.Redirect.to(err.toFile()))       // stderr → inny plik
                .redirectInput(ProcessBuilder.Redirect.from(nullInput(work)))  // stdin z pustego pliku
                .start();
        show("zakończył się w limicie", process.waitFor(10, TimeUnit.SECONDS));
        // WYNIK: zakończył się w limicie → true
        try (Stream<String> outLines = Files.lines(out); Stream<String> errLines = Files.lines(err)) {
            show("linie w wyniki.txt", outLines.count());
            // WYNIK: linie w wyniki.txt → 20000
            show("linie w uwagi.txt (bez Picked up)", errLines.filter(l -> !l.contains("Picked up ")).count());
            // WYNIK: linie w uwagi.txt (bez Picked up) → 5000
        }
        // Przy przekierowaniu do plików NIE MA ryzyka zakleszczenia (sekcja 3): system pisze prosto do pliku,
        // a my tylko czekamy. To najprostsza droga, gdy wyjście jest duże, a nie musisz go analizować na bieżąco.
        // Inne opcje: Redirect.appendTo(plik) — dopisz; Redirect.DISCARD (Java 9+) — wyrzuć; Redirect.INHERIT — konsola.
    }

    /** Pusty plik jako stdin dziecka (dziecko nie czeka wtedy na dane z klawiatury). */
    static File nullInput(Path work) throws IOException {
        return Files.writeString(work.resolve("pusty.txt"), "").toFile();
    }

    // =================================================================================================
    // 3. PUŁAPKA: ZAKLESZCZENIE PRZY NIECZYTANYM WYJŚCIU
    // =================================================================================================

    /**
     * 3. Między rodzicem a dzieckiem są potoki o ograniczonym buforze (zwykle kilka–kilkadziesiąt KB, zależnie
     * od systemu). Jeśli dziecko pisze dużo, a rodzic nie czyta, bufor się zapełnia i dziecko STAJE na println.
     */
    static void deadlockPitfall() {
        section("3. Pułapka: zakleszczenie (deadlock)");

        // ŹLE (nie uruchamiamy — zawiesiłoby lekcję):
        //   Process p = new ProcessBuilder(cmd).start();      // stdout i stderr = osobne potoki
        //   p.waitFor();                                       // czekamy na koniec...
        //   String out = new String(p.getInputStream().readAllBytes());   // ...a czytać zaczynamy dopiero potem
        // Dziecko z sekcji 2 wypisuje ~200 KB — zapełnia bufor, czeka, aż ktoś przeczyta. Rodzic czeka, aż dziecko
        // skończy. Nikt nie ustąpi = zakleszczenie (deadlock). Bez limitu czasu program wisi w nieskończoność.
        //
        // PUŁAPKA: czytanie TYLKO stdout, gdy stderr jest osobnym potokiem — dziecko może zablokować się na stderr.
        // Dlaczego: każdy potok ma własny bufor.
        // DOBRA PRAKTYKA: wybierz jedno z poniższych:
        //   • redirectErrorStream(true) i czytaj stdout do końca PRZED waitFor (sekcja 1),
        //   • przekieruj do plików albo Redirect.DISCARD (sekcja 2),
        //   • czytaj stdout i stderr równolegle (np. dwa CompletableFuture.supplyAsync — tak robi Build04),
        //   • zawsze waitFor(limit, jednostka) + destroyForcibly po przekroczeniu limitu.
        List<String> rules = List.of("czytaj wyjście na bieżąco", "albo przekieruj je do pliku", "zawsze z limitem czasu");
        show("zasady", rules);
        // WYNIK: zasady → [czytaj wyjście na bieżąco, albo przekieruj je do pliku, zawsze z limitem czasu]
    }

    // =================================================================================================
    // 4. destroy, destroyForcibly I ProcessHandle
    // =================================================================================================

    /**
     * 4. Dziecko, które się zawiesiło, trzeba zakończyć. destroy() prosi o zakończenie (na Linuksie sygnał SIGTERM),
     * destroyForcibly() zabija (SIGKILL). ProcessHandle (Java 9+) daje pid, rodzica, dzieci i informacje o procesie.
     */
    static void destroyAndHandles() throws Exception {
        section("4. Przerywanie procesu i ProcessHandle");

        ProcessHandle me = ProcessHandle.current();            // current = bieżący proces
        show("mój pid > 0", me.pid() > 0);
        // WYNIK: mój pid > 0 → true

        Process sleeper = new ProcessBuilder(childCommand("--dziecko-spij"))
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)   // nic nie czytamy — więc wyrzucamy (bez zakleszczenia)
                .start();
        show("skończył się w 300 ms", sleeper.waitFor(300, TimeUnit.MILLISECONDS));
        // WYNIK: skończył się w 300 ms → false
        show("żyje", sleeper.isAlive());
        // WYNIK: żyje → true
        show("rodzic dziecka to ja", sleeper.toHandle().parent().map(ProcessHandle::pid).orElse(-1L) == me.pid());
        // WYNIK: rodzic dziecka to ja → true
        show("jest na liście moich dzieci", me.children().anyMatch(h -> h.pid() == sleeper.pid()));
        // WYNIK: jest na liście moich dzieci → true

        sleeper.destroy();                                     // poproś o zakończenie
        if (!sleeper.waitFor(5, TimeUnit.SECONDS)) {
            sleeper.destroyForcibly().waitFor(5, TimeUnit.SECONDS);   // nie posłuchał → zabij
        }
        show("żyje po destroy", sleeper.isAlive());
        // WYNIK: żyje po destroy → false
        show("kod wyjścia różny od 0", sleeper.exitValue() != 0);
        // WYNIK: kod wyjścia różny od 0 → true
        // Samego kodu nie wypisujemy: Linux daje 143 (128 + 15 = SIGTERM), po destroyForcibly 137 (128 + 9),
        // a Windows zwykle 1 — tam destroy() i tak kończy proces siłą (supportsNormalTermination() == false).

        String command = me.info().command().orElse("(brak)"); // info = informacje (pola Optional — mogą być puste)
        note("pełna ścieżka mojego programu znana: " + !command.equals("(brak)"));
        // (wynik zależy od uruchomienia)
        // info() zależy od systemu i uprawnień: command(), arguments(), startInstant(), user() — każde może być puste.
        // onExit() (Java 9+) zwraca CompletableFuture, które kończy się razem z procesem — można reagować bez blokowania.
    }

    // =================================================================================================
    // 5. inheritIO: DZIECKO PISZE NA NASZĄ KONSOLĘ
    // =================================================================================================

    /**
     * 5. inheritIO() = dziecko używa NASZEGO stdin/stdout/stderr. Najprostsze, gdy tylko chcesz pokazać
     * użytkownikowi wyjście narzędzia, ale wtedy nie możesz go przeanalizować w kodzie.
     */
    static void inheritIo() throws Exception {
        section("5. inheritIO: dziecko pisze prosto na konsolę");

        System.out.flush();                                    // nasze bufory najpierw — inaczej kolejność linii się pomiesza
        Process process = new ProcessBuilder(childCommand("--dziecko-ascii")).inheritIO().start();
        show("kod wyjścia", process.waitFor(10, TimeUnit.SECONDS) ? process.exitValue() : -1);
        // WYNIK: [dziecko] pisze prosto na konsole rodzica    ← ta linia pochodzi z procesu potomnego
        // WYNIK: kod wyjścia → 0
        // Dziecko pisze tylko znaki ASCII: jego kodowanie konsoli może się różnić od naszego (polskie litery
        // mogłyby się zepsuć — szczególnie w konsoli Windows).
        // PUŁAPKA: inheritIO w aplikacji serwerowej — wyjście dziecka miesza się z logami serwera i nie da się
        // go powiązać z żądaniem. Dlaczego: obaj piszą do tego samego strumienia, bez znaczników.
    }

    // =================================================================================================
    // 6. System.getenv vs System.getProperty
    // =================================================================================================

    /**
     * 6. Zmienne środowiskowe należą do SYSTEMU (ustawia je powłoka, system, kontener), są tylko do odczytu.
     * Właściwości systemowe należą do JVM: część ustawia sama JVM (java.home, user.dir), część Ty (-Dklucz=wartość).
     */
    static void envVsProperties() {
        section("6. System.getenv a System.getProperty");

        show("PATH ustawione", System.getenv("PATH") != null);
        // WYNIK: PATH ustawione → true
        // Wartości NIE wypisujemy — jest inna na każdym komputerze (i bywa długa). Na Windows zmienna nazywa się
        // "Path", ale getenv na Windows nie rozróżnia wielkości liter, więc getenv("PATH") ją znajdzie.
        expectThrows("System.getenv().put(...)", () -> System.getenv().put("X", "1"));
        // WYNIK: ✔ System.getenv().put(...) → rzucono UnsupportedOperationException: (brak komunikatu)
        // Środowiska bieżącego procesu Java NIE zmieni — można tylko podać inne środowisko DZIECKU (sekcja 1).

        System.setProperty("kurs.lekcja", "Build05");          // set property = ustaw właściwość
        show("kurs.lekcja", System.getProperty("kurs.lekcja"));
        // WYNIK: kurs.lekcja → Build05
        show("kurs.brak z wartością domyślną", System.getProperty("kurs.brak", "domyślna"));
        // WYNIK: kurs.brak z wartością domyślną → domyślna
        show("user.dir ustawione", System.getProperty("user.dir") != null);
        // WYNIK: user.dir ustawione → true
        show("wersja Javy (feature)", Runtime.version().feature());   // feature = główny numer wersji (Java 10+)
        // WYNIK: wersja Javy (feature) → 17

        // PUŁAPKA: Boolean.getBoolean("true") NIE zamienia tekstu na boolean — szuka WŁAŚCIWOŚCI SYSTEMOWEJ o nazwie "true"!
        show("Boolean.getBoolean(\"true\")", Boolean.getBoolean("true"));
        // WYNIK: Boolean.getBoolean("true") → false
        System.setProperty("kurs.debug", "true");
        show("Boolean.getBoolean(\"kurs.debug\")", Boolean.getBoolean("kurs.debug"));
        // WYNIK: Boolean.getBoolean("kurs.debug") → true
        show("Integer.getInteger(\"kurs.port\", 8080)", Integer.getInteger("kurs.port", 8080));
        // WYNIK: Integer.getInteger("kurs.port", 8080) → 8080
        // Do zamiany tekstu: Boolean.parseBoolean("true"), Integer.parseInt("8080"). Nazwy getBoolean/getInteger
        // to historyczny wypadek przy pracy w API — łatwo się pomylić.

        // Kiedy co:
        //   zmienne środowiskowe — konfiguracja z zewnątrz (hasła w kontenerze, DATABASE_URL, JAVA_HOME),
        //   -D — przełączniki JVM i aplikacji (-Dfile.encoding=UTF-8, -Dspring.profiles.active=dev).
        // Spring Boot czyta jedno i drugie: właściwość spring.datasource.url można podać też jako zmienną
        // SPRING_DATASOURCE_URL (relaxed binding = luźne dopasowanie nazw).
        // PUŁAPKA: wypisywanie wszystkich zmiennych środowiskowych do logów — często zawierają hasła i klucze API.
        // Dlaczego: logi czyta więcej osób i systemów niż powinno znać sekrety.
    }

    // =================================================================================================
    // 7. PUŁAPKI Runtime.exec(String)
    // =================================================================================================

    /**
     * 7. Runtime.getRuntime().exec(String) to stary sposób. Dzieli tekst po białych znakach (StringTokenizer)
     * i NIE rozumie cudzysłowów. Od Javy 18 ta wersja jest oznaczona jako przestarzała (deprecated).
     */
    static void runtimeExecPitfalls() {
        section("7. Pułapki Runtime.exec(String)");

        String line = "java -cp \"C:\\Program Files\\app\" app.Main";
        List<String> tokens = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(line); // tak samo dzieli Runtime.exec(String)
        while (tokenizer.hasMoreTokens()) {
            tokens.add(tokenizer.nextToken());
        }
        showEach("Runtime.exec widzi argumenty", tokens);
        // WYNIK: Runtime.exec widzi argumenty (liczba elementów: 5):
        // WYNIK: • java
        // WYNIK: • -cp
        // WYNIK: • "C:\Program
        // WYNIK: • Files\app"
        // WYNIK: • app.Main
        // Ścieżka ze spacją rozpadła się na dwa argumenty, a cudzysłowy zostały w środku tekstu.

        List<String> correct = List.of("java", "-cp", "C:\\Program Files\\app", "app.Main");
        show("ProcessBuilder z listą — argumentów", correct.size());
        // WYNIK: ProcessBuilder z listą — argumentów → 4
        // DOBRA PRAKTYKA: ProcessBuilder z listą (albo Runtime.exec(String[])) — każdy argument to osobny element,
        // spacje w środku nie przeszkadzają. Dodatkowo ProcessBuilder daje katalog, środowisko i przekierowania.
        // PUŁAPKA: Runtime.exec("dir") albo exec("ls | wc -l") — dir to polecenie WBUDOWANE w cmd.exe (nie ma pliku
        // dir.exe), a „|” rozumie tylko powłoka. Dlaczego: exec uruchamia program bezpośrednio, bez powłoki.
    }

    // =================================================================================================
    // 8. POWŁOKA JEST ZALEŻNA OD SYSTEMU, A POLECENIA Z DANYCH UŻYTKOWNIKA — NIEBEZPIECZNE
    // =================================================================================================

    /**
     * 8. Gdy naprawdę potrzebujesz powłoki (potoki, gwiazdki), polecenie jest różne dla systemów:
     * Windows: cmd /c ..., Linux/macOS: sh -c "...". Lekcja uruchamia tylko java, żeby działać wszędzie tak samo.
     */
    static void shellAndSecurity() {
        section("8. Powłoka i bezpieczeństwo");

        boolean windows = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).startsWith("windows");
        List<String> listing = windows ? List.of("cmd", "/c", "dir") : List.of("sh", "-c", "ls -l | wc -l");
        note("polecenie powłoki dla tego systemu ma elementów: " + listing.size());
        // (wynik zależy od uruchomienia)

        // ATAK: wstrzyknięcie polecenia (command injection). Nazwa pliku od użytkownika wklejona do tekstu dla powłoki:
        String userInput = "raport.txt; echo drugie-polecenie";
        String shellLine = "cat " + userInput;                 // sh -c "cat raport.txt; echo drugie-polecenie" → DWA polecenia!
        show("powłoka zobaczy średnik", shellLine.contains(";"));
        // WYNIK: powłoka zobaczy średnik → true
        // W liście argumentów BEZ powłoki ten sam tekst to po prostu dziwna nazwa pliku (cat jej nie znajdzie):
        show("argument bez powłoki", List.of("cat", userInput));
        // WYNIK: argument bez powłoki → [cat, raport.txt; echo drugie-polecenie]

        show("nazwa 'raport-2026.txt' bezpieczna", isSafeFileName("raport-2026.txt"));
        // WYNIK: nazwa 'raport-2026.txt' bezpieczna → true
        show("nazwa '../../inny-katalog/plik' bezpieczna", isSafeFileName("../../inny-katalog/plik"));
        // WYNIK: nazwa '../../inny-katalog/plik' bezpieczna → false
        show("nazwa '-rf' bezpieczna", isSafeFileName("-rf"));
        // WYNIK: nazwa '-rf' bezpieczna → false
        // DOBRA PRAKTYKA: od najlepszej: 1) zrób to w Javie, bez procesu (Files.list zamiast ls); 2) lista argumentów
        // bez powłoki; 3) BIAŁA LISTA dozwolonych programów i wzorzec dozwolonych znaków; nigdy „czarna lista” znaków.
        // Dlaczego: nazwa zaczynająca się od "-" może zostać wzięta za opcję programu, a "../" wyjść poza katalog —
        // nawet bez powłoki.
    }

    /** Biała lista znaków: litery, cyfry, kropka, myślnik, podkreślnik; bez "-" na początku i bez "..". */
    static boolean isSafeFileName(String name) {
        return name.matches("[A-Za-z0-9._-]+") && !name.startsWith("-") && !name.contains("..");
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • new ProcessBuilder(List.of(program, arg1, arg2)) — lista, bez cudzysłowów, bez powłoki
     *   • directory(...), environment().put(...) (kopia — tylko dla dziecka), redirectErrorStream(true)
     *   • wyjście: PIPE (czytaj!), Redirect.to/appendTo(plik), DISCARD, INHERIT / inheritIO()
     *   • czytaj wyjście do końca PRZED waitFor albo przekieruj do pliku — inaczej zakleszczenie
     *   • waitFor(limit, jednostka) → false = nie zdążył → destroy() / destroyForcibly(); exitValue() dopiero po końcu
     *   • ProcessHandle.current().pid(), toHandle().parent(), children(), info() (pola Optional), onExit()
     *   • System.getenv — zmienne systemu (tylko odczyt; na Windows bez rozróżniania wielkości liter)
     *   • System.getProperty — właściwości JVM, -Dklucz=wartość; getProperty(klucz, domyślna)
     *   • Boolean.getBoolean / Integer.getInteger czytają WŁAŚCIWOŚCI, nie zamieniają tekstu
     *   • Runtime.exec(String) dzieli po spacjach i nie zna cudzysłowów (deprecated od Javy 18)
     *   • powłoka: cmd /c (Windows), sh -c (Linux/macOS); nigdy nie składaj polecenia z danych użytkownika
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego program, który woła p.waitFor() przed przeczytaniem wyjścia dziecka, czasem wisi w nieskończoność?
     *   2. Co wypisze:  System.out.println(Boolean.getBoolean("true"));  ?
     *   3. ZNAJDŹ BŁĄD:
     *          ProcessBuilder pb = new ProcessBuilder("java -version");
     *          pb.start();
     *   4. Rodzic robi pb.environment().put("TRYB", "test") i uruchamia dziecko. Co zwróci System.getenv("TRYB")
     *      w rodzicu, a co w dziecku?
     *   5. Co wypisze:  System.out.println(System.getProperty("kurs.nieistniejaca", "brak"));  ?
     *   6. ZNAJDŹ BŁĄD (serwer WWW):
     *          String file = request.getParameter("plik");
     *          new ProcessBuilder("sh", "-c", "cat /dane/" + file).start();
     *   7. Czym różni się destroy() od destroyForcibly()? Dlaczego po destroy() warto jeszcze raz poczekać?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Map<String, String> props = Map.of("app.port", "9090");
        Map<String, String> env = Map.of("APP_PORT", "7070", "APP_MODE", "prod");
        Check.equal("ćw. 1: -D wygrywa", "9090", () -> exercise1("app.port", "APP_PORT", props, env, "8080"));
        Check.equal("ćw. 1: potem środowisko", "prod", () -> exercise1("app.mode", "APP_MODE", props, env, "dev"));
        Check.equal("ćw. 1: na końcu domyślna", "INFO", () -> exercise1("app.log", "APP_LOG", props, env, "INFO"));
        Check.equal("ćw. 2: podział po spacjach", List.of("java", "-cp", "out", "app.Main"), () -> exercise2("  java  -cp out\tapp.Main "));
        Check.equal("ćw. 3: znaczenie kodów", List.of("sukces", "złe użycie", "nie znaleziono polecenia", "zabity sygnałem 9", "błąd (kod 42)"),
                () -> List.of(0, 2, 127, 137, 42).stream().map(Build05ProcessesEnv::exercise3).toList());
        Check.equal("ćw. 4: bezpieczne polecenie", List.of("jar", "--list", "--file", "app-1.0.jar"), () -> exercise4("jar", "app-1.0.jar"));
        Check.throwsException("ćw. 4: program spoza listy", IllegalArgumentException.class, () -> exercise4("rm", "app.jar"));
        Check.throwsException("ćw. 4: zła nazwa pliku", IllegalArgumentException.class, () -> exercise4("jar", "a.jar; echo drugie-polecenie"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec): -D", "9090", () -> solution1("app.port", "APP_PORT", props, env, "8080"));
        Check.equal("ćw. 1 (wzorzec): środowisko", "prod", () -> solution1("app.mode", "APP_MODE", props, env, "dev"));
        Check.equal("ćw. 1 (wzorzec): domyślna", "INFO", () -> solution1("app.log", "APP_LOG", props, env, "INFO"));
        Check.equal("ćw. 2 (wzorzec)", List.of("java", "-cp", "out", "app.Main"), () -> solution2("  java  -cp out\tapp.Main "));
        Check.equal("ćw. 3 (wzorzec)", List.of("sukces", "złe użycie", "nie znaleziono polecenia", "zabity sygnałem 9", "błąd (kod 42)"),
                () -> List.of(0, 2, 127, 137, 42).stream().map(Build05ProcessesEnv::solution3).toList());
        Check.equal("ćw. 4 (wzorzec)", List.of("jar", "--list", "--file", "app-1.0.jar"), () -> solution4("jar", "app-1.0.jar"));
        Check.throwsException("ćw. 4 (wzorzec): program", IllegalArgumentException.class, () -> solution4("rm", "app.jar"));
        Check.throwsException("ćw. 4 (wzorzec): plik", IllegalArgumentException.class, () -> solution4("jar", "a.jar; echo drugie-polecenie"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 8 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): popularny wzorzec konfiguracji — weź właściwość systemową (props, jak z -D), a gdy jej nie ma,
     * zmienną środowiskową (env), a gdy i jej nie ma — wartość domyślną.
     * Podpowiedź: props.get(...) != null ? ... ; albo Optional.ofNullable(...).or(...) (Java 9+).
     */
    static String exercise1(String property, String envName, Map<String, String> props, Map<String, String> env,
                            String defaultValue) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): PRZEPISZ pętlę z StringTokenizer (sekcja 7) na jedną linię ze strip() i split.
     * Wynik ma być taki sam: kawałki rozdzielone dowolnymi białymi znakami, bez pustych elementów.
     * Stara wersja:
     * <pre>{@code
     * List<String> tokens = new ArrayList<>();
     * StringTokenizer t = new StringTokenizer(line);
     * while (t.hasMoreTokens()) {
     *     tokens.add(t.nextToken());
     * }
     * return tokens;
     * }</pre>
     * Podpowiedź: line.strip().split("\\s+") — strip usuwa spacje z brzegów, inaczej pierwszy element byłby pusty.
     */
    static List<String> exercise2(String line) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): opisz kod wyjścia wyrażeniem switch (Java 14+): 0 → "sukces", 1 → "błąd",
     * 2 → "złe użycie", 126 → "nie można wykonać", 127 → "nie znaleziono polecenia", 129..192 → "zabity sygnałem N"
     * (N = kod − 128), inne → "błąd (kod X)".
     * Podpowiedź: case z wieloma etykietami nie przyjmuje zakresów — zakres sprawdź w default (if albo operator ?:).
     */
    static String exercise3(int exitCode) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zbuduj BEZPIECZNE polecenie [program, "--list", "--file", plik]. Program musi być
     * z białej listy {"jar"} (Set), a nazwa pliku przejść isSafeFileName i kończyć się ".jar"; inaczej
     * IllegalArgumentException z opisem. Żadnej powłoki!
     * Podpowiedź: Set.of("jar").contains(program); List.of(...) na końcu.
     */
    static List<String> exercise4(String program, String fileName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String property, String envName, Map<String, String> props, Map<String, String> env,
                            String defaultValue) {
        String fromProperty = props.get(property);
        if (fromProperty != null) {
            return fromProperty;
        }
        return env.getOrDefault(envName, defaultValue);
    }

    static List<String> solution2(String line) {
        return List.of(line.strip().split("\\s+"));
    }

    static String solution3(int exitCode) {
        return switch (exitCode) {
            case 0 -> "sukces";
            case 1 -> "błąd";
            case 2 -> "złe użycie";
            case 126 -> "nie można wykonać";
            case 127 -> "nie znaleziono polecenia";
            default -> exitCode > 128 && exitCode <= 192 ? "zabity sygnałem " + (exitCode - 128) : "błąd (kod " + exitCode + ")";
        };
    }

    static List<String> solution4(String program, String fileName) {
        if (!Set.of("jar").contains(program)) {
            throw new IllegalArgumentException("program spoza białej listy: " + program);
        }
        if (!isSafeFileName(fileName) || !fileName.endsWith(".jar")) {
            throw new IllegalArgumentException("niedozwolona nazwa pliku: " + fileName);
        }
        return List.of(program, "--list", "--file", fileName);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Dziecko pisze do potoku o ograniczonym buforze. Gdy bufor się zapełni, dziecko czeka, aż rodzic przeczyta,
     *      a rodzic czeka (waitFor), aż dziecko skończy — zakleszczenie. Przy małym wyjściu problem się nie pojawia,
     *      dlatego błąd wychodzi „czasem”.
     *   2. false — Boolean.getBoolean szuka właściwości systemowej o nazwie "true", której nie ma.
     *   3. Konstruktor z jednym tekstem traktuje CAŁY tekst jako nazwę programu ("java -version" ze spacją) →
     *      IOException (nie znaleziono programu). Poprawnie: new ProcessBuilder("java", "-version") albo lista.
     *      Do tego brak czytania wyjścia, waitFor i sprawdzenia kodu.
     *   4. W rodzicu null (environment() to kopia dla dziecka), w dziecku "test".
     *   5. brak
     *   6. Wstrzyknięcie polecenia: plik = "x; echo drugie-polecenie" wykona drugie polecenie, a "../../inny-katalog/plik" wyjdzie poza
     *      katalog. Poprawnie: bez powłoki i bez procesu — Files.readString(base.resolve(file).normalize()) ze sprawdzeniem,
     *      że wynik leży w katalogu /dane i że nazwa pasuje do białej listy.
     *   7. destroy() prosi o zakończenie (Linux: SIGTERM — program może posprzątać), destroyForcibly() zabija od razu
     *      (SIGKILL). Oba tylko WYSYŁAJĄ żądanie — proces kończy się chwilę później, więc trzeba waitFor (z limitem)
     *      i dopiero wtedy sprawdzać exitValue()/isAlive().
     */
    // </editor-fold>
}
