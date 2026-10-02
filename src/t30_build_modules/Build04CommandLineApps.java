package t30_build_modules;

import helpers.Check;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Aplikacje konsolowe — argumenty, kody wyjścia, stdin/stdout/stderr
 *        (CLI = command-line interface = interfejs wiersza poleceń)
 *
 * W SKRÓCIE:
 *   Program konsolowy dostaje argumenty w String[] args, czyta dane ze standardowego wejścia (stdin),
 *   wyniki pisze na stdout, błędy na stderr, a na koniec oddaje systemowi KOD WYJŚCIA (0 = sukces).
 *   Budujemy małe narzędzie „suma” i testujemy je bez konsoli — przez metodę run(args, in, out, err).
 *
 * ANALOGIA:
 *   Okienko na poczcie. Argumenty to to, co mówisz urzędnikowi („polecony, priorytet”), stdin to paczka,
 *   którą podajesz, stdout to pokwitowanie, stderr to uwagi urzędnika („brak kodu pocztowego!”),
 *   a kod wyjścia to pieczątka: „przyjęto” albo „odrzucono — powód nr 2”.
 *
 * JAK TO DZIAŁA:
 *   java -jar suma.jar -v --separator=" plus " -- 1 2 -3
 *        args = ["-v", "--separator= plus ", "--", "1", "2", "-3"]   (cudzysłowy zdejmuje POWŁOKA, nie Java)
 *   main(args) → run(args, System.in, System.out, System.err) → int kod → System.exit(kod)
 *   Konwencja kodów: 0 = OK, 1 = błąd działania (np. złe dane), 2 = złe użycie (zła opcja, brak argumentów).
 *
 * SŁÓWKA:
 *   argument = argument; option = opcja; flag = flaga (opcja bez wartości); positional = pozycyjny;
 *   usage = sposób użycia; exit code = kod wyjścia; standard input/output/error = standardowe wejście/wyjście/błędy;
 *   pipe = potok; redirect = przekierowanie; parse = przetwarzać (rozbierać tekst na części); verbose = szczegółowy.
 *
 * ZOBACZ TEŻ: t30_build_modules/Build05ProcessesEnv (uruchamianie innych programów), t18_io_files/Io02ReadingText
 *             (BufferedReader), t30_build_modules/Build02JarClasspath (java -jar), t34_toward_spring/Spring01IocContainer.
 * </pre>
 */
public class Build04CommandLineApps {

    static final String JAVA = Path.of(System.getProperty("java.home"), "bin", "java").toString();

    public static void main(String[] args) throws Exception {
        // Tryby dla procesów potomnych (sekcje 5–7): ta sama klasa uruchomiona w osobnym JVM jako narzędzie.
        if (args.length > 0 && args[0].equals("--suma")) {
            System.exit(SumTool.run(Arrays.copyOfRange(args, 1, args.length), System.in, System.out, System.err));
        }
        if (args.length > 0 && args[0].equals("--konsola")) {
            System.out.println("System.console() == null → " + (System.console() == null));
            return;
        }

        title("Build04 — aplikacje konsolowe");

        mainArgs();                // main args = argumenty metody main
        parsingByHand();           // parsing by hand = ręczne przetwarzanie argumentów
        sumToolInProcess();        // sum tool in process = narzędzie „suma” w tym samym procesie
        errorsAndUsage();          // errors and usage = błędy i tekst pomocy
        exitCodesInChildJvm();     // exit codes in child JVM = prawdziwe kody wyjścia
        standardStreams();         // standard streams = standardowe strumienie
        consoleMayBeNull();        // console may be null = konsola może być null
        librariesAndSpring();      // libraries and Spring = biblioteki i Spring Boot
        exercises();               // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. main(String[] args)
    // =================================================================================================

    static String describe(String[] args) {
        return "liczba=" + args.length + (args.length > 0 ? ", args[0]=" + args[0] : "");
    }

    /**
     * 1. JVM woła public static void main(String[] args). Tablica NIGDY nie jest null — bez argumentów ma długość 0.
     * args[0] to PIERWSZY argument (nie nazwa programu, jak w C czy Pythonie).
     */
    static void mainArgs() {
        section("1. main(String[] args)");

        show("java app.Main", describe(new String[]{}));
        // WYNIK: java app.Main → liczba=0
        show("java app.Main Jan Kowalski", describe(new String[]{"Jan", "Kowalski"}));
        // WYNIK: java app.Main Jan Kowalski → liczba=2, args[0]=Jan
        show("java app.Main \"Jan Kowalski\"", describe(new String[]{"Jan Kowalski"}));
        // WYNIK: java app.Main "Jan Kowalski" → liczba=1, args[0]=Jan Kowalski

        // Kto dzieli linię na argumenty? POWŁOKA (bash, cmd, PowerShell) — po spacjach, z uwzględnieniem cudzysłowów.
        // Java dostaje gotową tablicę. Dlatego "Jan Kowalski" w cudzysłowie to JEDEN argument.
        // PUŁAPKA: args[0] bez sprawdzenia długości → ArrayIndexOutOfBoundsException, gdy ktoś uruchomi program bez
        // argumentów. Dlaczego: tablica ma wtedy długość 0 — zawsze sprawdzaj args.length i pokaż sposób użycia.
        expectThrows("args[0] przy pustej tablicy", () -> describeFirst(new String[]{}));
        // WYNIK: ✔ args[0] przy pustej tablicy → rzucono ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0
        // IntelliJ: argumenty ustawiasz w Run → Edit Configurations… → pole „Program arguments”.
    }

    static String describeFirst(String[] args) {
        return args[0];
    }

    // =================================================================================================
    // 2. RĘCZNE PRZETWARZANIE ARGUMENTÓW
    // =================================================================================================

    /** Wynik przetwarzania: flagi (-v), opcje z wartością (--nazwa=wartość) i argumenty pozycyjne. */
    record ParsedArgs(Set<String> flags, Map<String, String> options, List<String> positional) {
    }

    /** Wyjątek „złe użycie programu” — kończy się kodem 2 i tekstem pomocy. */
    static class UsageException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        UsageException(String message) {
            super(message);
        }
    }

    /** Czy tekst to liczba ujemna (np. "-5"), a nie opcja? */
    static boolean isNegativeNumber(String arg) {
        return arg.matches("-\\d+");
    }

    /**
     * Zasady: "--" kończy opcje (wszystko dalej jest pozycyjne); "--nazwa=wartość" to opcja; "-x" i "--nazwa" bez "="
     * to flagi; liczba ujemna NIE jest opcją; reszta to argumenty pozycyjne (w kolejności).
     */
    static ParsedArgs parse(String[] args) {
        Set<String> flags = new TreeSet<>();
        Map<String, String> options = new TreeMap<>();
        List<String> positional = new ArrayList<>();
        boolean optionsEnded = false;                           // options ended = koniec opcji
        for (String arg : args) {
            if (optionsEnded || !arg.startsWith("-") || isNegativeNumber(arg) || arg.equals("-")) {
                positional.add(arg);                            // "-" samo w sobie zwyczajowo znaczy „czytaj stdin”
            } else if (arg.equals("--")) {
                optionsEnded = true;
            } else if (arg.startsWith("--") && arg.contains("=")) {
                int eq = arg.indexOf('=');
                options.put(arg.substring(2, eq), arg.substring(eq + 1));
            } else {
                flags.add(arg);
            }
        }
        return new ParsedArgs(flags, options, List.copyOf(positional));
    }

    /**
     * 2. Najprostsze konwencje (styl GNU/POSIX): krótkie flagi "-v", długie "--verbose", opcje z wartością
     * "--name=Jan", argumenty pozycyjne i separator "--", po którym nic nie jest opcją.
     */
    static void parsingByHand() {
        section("2. Ręczne przetwarzanie argumentów");

        ParsedArgs a = parse(new String[]{"-v", "--name=Jan", "plik.txt", "--force", "raport.csv"});
        show("flagi", a.flags());
        // WYNIK: flagi → [--force, -v]
        show("opcje", a.options());
        // WYNIK: opcje → {name=Jan}
        show("pozycyjne", a.positional());
        // WYNIK: pozycyjne → [plik.txt, raport.csv]

        ParsedArgs b = parse(new String[]{"--sep=;", "--", "-v", "--name=X"});
        show("po \"--\" wszystko jest pozycyjne", b.positional());
        // WYNIK: po "--" wszystko jest pozycyjne → [-v, --name=X]
        show("a opcje sprzed \"--\"", b.options());
        // WYNIK: a opcje sprzed "--" → {sep=;}

        ParsedArgs c = parse(new String[]{"10", "-5", "-x"});
        show("liczba ujemna to nie opcja", c.positional() + " / flagi " + c.flags());
        // WYNIK: liczba ujemna to nie opcja → [10, -5] / flagi [-x]

        // PUŁAPKA: plik o nazwie zaczynającej się od "-" (np. "-raport.txt") wygląda jak opcja.
        // Dlaczego: parser nie zgadnie intencji — użytkownik pisze wtedy: program -- -raport.txt.
        // PUŁAPKA: opcja „--name Jan” (wartość jako osobny argument) to INNA konwencja niż „--name=Jan” — nasz prosty
        // parser jej nie obsługuje (Jan byłby pozycyjny). Biblioteki (sekcja 8) obsługują obie.
        // DOBRA PRAKTYKA: zawsze obsłuż -h/--help — to pierwsze, co wpisze nowy użytkownik.
    }

    // =================================================================================================
    // 3. NARZĘDZIE „SUMA” — TESTOWANE BEZ KONSOLI
    // =================================================================================================

    /**
     * Narzędzie: suma [-v|--verbose] [--separator=TEKST] [--] liczba... — bez liczb czyta je ze stdin (po jednej w linii).
     * Cała logika jest w run(...) z JAWNYMI strumieniami → testy nie potrzebują prawdziwej konsoli ani System.exit.
     */
    static final class SumTool {

        static final String USAGE = "użycie: suma [-v|--verbose] [--separator=TEKST] [--] liczba...";

        private SumTool() {
        }

        static int run(String[] args, InputStream in, PrintStream out, PrintStream err) {
            try {
                ParsedArgs parsed = parse(args);
                if (parsed.flags().contains("-h") || parsed.flags().contains("--help")) {
                    out.println(USAGE);                         // pomoc na ŻYCZENIE → stdout i kod 0
                    return 0;
                }
                Set<String> unknownFlags = new TreeSet<>(parsed.flags());
                unknownFlags.removeAll(Set.of("-v", "--verbose"));
                Set<String> unknownOptions = new TreeSet<>(parsed.options().keySet());
                unknownOptions.remove("separator");
                if (!unknownFlags.isEmpty() || !unknownOptions.isEmpty()) {
                    unknownFlags.addAll(unknownOptions.stream().map(o -> "--" + o).toList());
                    throw new UsageException("nieznana opcja: " + String.join(", ", unknownFlags));
                }
                List<String> numbers = parsed.positional().isEmpty() ? readLines(in) : parsed.positional();
                if (numbers.isEmpty()) {
                    throw new UsageException("brak liczb (ani w argumentach, ani na wejściu)");
                }
                long sum = 0;
                for (String n : numbers) {
                    try {
                        sum += Long.parseLong(n.strip());
                    } catch (NumberFormatException e) {
                        err.println("błąd: to nie jest liczba: " + n);   // złe DANE → kod 1
                        return 1;
                    }
                }
                boolean verbose = parsed.flags().contains("-v") || parsed.flags().contains("--verbose");
                String separator = parsed.options().getOrDefault("separator", " + ");
                out.println(verbose ? String.join(separator, numbers) + " = " + sum : String.valueOf(sum));
                return 0;
            } catch (UsageException e) {
                err.println("błąd: " + e.getMessage());          // złe UŻYCIE → komunikat + pomoc na stderr, kod 2
                err.println(USAGE);
                return 2;
            }
        }

        /** Niepuste linie ze strumienia (UTF-8 podany jawnie). */
        static List<String> readLines(InputStream in) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                return reader.lines().map(String::strip).filter(l -> !l.isEmpty()).toList();
            } catch (UncheckedIOException e) {
                throw new IllegalStateException("nie da się czytać wejścia", e);
            }
        }
    }

    /** Wynik uruchomienia narzędzia w teście: kod, stdout i stderr (linie rozdzielone " | "). */
    record Run(int exit, String out, String err) {
        @Override
        public String toString() {
            return "kod=" + exit + " out=[" + out + "] err=[" + err + "]";
        }
    }

    /** Uruchamia SumTool w TYM procesie z podanym stdin; wyjścia zbiera do pamięci. */
    static Run sum(String stdin, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = SumTool.run(args, new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Run(code, joinLines(out), joinLines(err));
    }

    static String joinLines(ByteArrayOutputStream bytes) {
        return bytes.toString(StandardCharsets.UTF_8).lines().collect(Collectors.joining(" | "));
    }

    /**
     * 3. Logika narzędzia NIE woła System.exit ani System.out — dostaje strumienie jako parametry i zwraca kod.
     * Dzięki temu testujemy je zwykłym wywołaniem metody, deterministycznie i szybko.
     */
    static void sumToolInProcess() {
        section("3. Narzędzie „suma” testowane bez konsoli");

        show("suma 1 2 3", sum("", "1", "2", "3"));
        // WYNIK: suma 1 2 3 → kod=0 out=[6] err=[]
        show("suma -v 1 2 3", sum("", "-v", "1", "2", "3"));
        // WYNIK: suma -v 1 2 3 → kod=0 out=[1 + 2 + 3 = 6] err=[]
        show("suma --verbose --separator=\" plus \" 4 -1", sum("", "--verbose", "--separator= plus ", "4", "-1"));
        // WYNIK: suma --verbose --separator=" plus " 4 -1 → kod=0 out=[4 plus -1 = 3] err=[]
        show("suma (liczby ze stdin)", sum("10\n\n20\n12\n"));
        // WYNIK: suma (liczby ze stdin) → kod=0 out=[42] err=[]
        show("suma --help", sum("", "--help"));
        // WYNIK: suma --help → kod=0 out=[użycie: suma [-v|--verbose] [--separator=TEKST] [--] liczba...] err=[]

        // DOBRA PRAKTYKA: main tylko „spina” program ze światem: System.exit(run(args, System.in, System.out, System.err)).
        // Dlaczego: System.exit w środku logiki zabija cały JVM — także test, który ją wywołał (w JUnit: koniec
        // całego przebiegu testów), i nie da się tego kodu użyć ponownie w innym programie.
    }

    // =================================================================================================
    // 4. BŁĘDY I TEKST POMOCY
    // =================================================================================================

    /**
     * 4. Dobre narzędzie przy błędzie mówi CO jest źle, JAK użyć poprawnie i zwraca niezerowy kod.
     * Błędy idą na stderr — dzięki temu suma 1 2 > wynik.txt zapisze do pliku tylko wynik.
     */
    static void errorsAndUsage() {
        section("4. Błędy i tekst pomocy");

        show("suma --bogus 1", sum("", "--bogus", "1"));
        // WYNIK: suma --bogus 1 → kod=2 out=[] err=[błąd: nieznana opcja: --bogus | użycie: suma [-v|--verbose] [--separator=TEKST] [--] liczba...]
        show("suma --kolor=red 1", sum("", "--kolor=red", "1"));
        // WYNIK: suma --kolor=red 1 → kod=2 out=[] err=[błąd: nieznana opcja: --kolor | użycie: suma [-v|--verbose] [--separator=TEKST] [--] liczba...]
        show("suma 1 dwa", sum("", "1", "dwa"));
        // WYNIK: suma 1 dwa → kod=1 out=[] err=[błąd: to nie jest liczba: dwa]
        show("suma (pusty stdin)", sum(""));
        // WYNIK: suma (pusty stdin) → kod=2 out=[] err=[błąd: brak liczb (ani w argumentach, ani na wejściu) | użycie: suma [-v|--verbose] [--separator=TEKST] [--] liczba...]

        // Konwencje kodów wyjścia (Unix, przyjęte też w świecie Javy):
        //   0 — sukces; 1 — ogólny błąd działania; 2 — złe użycie (opcje, argumenty);
        //   126/127 — powłoka: plik nie jest wykonywalny / polecenia nie znaleziono;
        //   128+N — proces zabity sygnałem N (130 = Ctrl+C, 137 = kill -9).
        // PUŁAPKA: System.exit(-1) albo System.exit(300) — na Linuksie/macOS kod jest obcinany do 0–255
        // (-1 → 255, 300 → 44). Dlaczego: system przechowuje tylko najniższy bajt. Trzymaj się 0–125.
        // PUŁAPKA: komunikat błędu na stdout → użytkownik przekierował wynik do pliku i nie widzi, że coś poszło źle,
        // a skrypt czytający wynik dostaje śmieci. Dlaczego: stdout jest dla DANYCH, stderr dla KOMUNIKATÓW.
    }

    // =================================================================================================
    // 5. PRAWDZIWE KODY WYJŚCIA — PROCES POTOMNY
    // =================================================================================================

    /** Ścieżka klas dla dziecka: katalog/JAR z klasami tej lekcji + ścieżka klas bieżącego JVM. */
    static String childClasspath() throws Exception {
        Path own = Path.of(Build04CommandLineApps.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        return own + File.pathSeparator + System.getProperty("java.class.path");
    }

    /** Uruchamia TĘ klasę w osobnym JVM z podanymi argumentami i tekstem na stdin. */
    static Run child(String stdin, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of(JAVA, "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8",
                "-Dsun.stderr.encoding=UTF-8", "-cp", childClasspath(), Build04CommandLineApps.class.getName()));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).start();   // stdout i stderr OSOBNO (bez redirectErrorStream)
        CompletableFuture<String> out = CompletableFuture.supplyAsync(() -> read(process.getInputStream()));
        CompletableFuture<String> err = CompletableFuture.supplyAsync(() -> read(process.getErrorStream()));
        try (OutputStream toChild = process.getOutputStream()) {  // stdin dziecka = NASZ strumień wyjściowy
            toChild.write(stdin.getBytes(StandardCharsets.UTF_8));
        }                                                        // zamknięcie = koniec danych (EOF) dla dziecka
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("proces potomny nie skończył się w 10 s");
        }
        return new Run(process.exitValue(), out.join(), err.join());
    }

    /** Linie bez „Picked up ...” (JVM dopisuje je na stderr, gdy ustawiono JAVA_TOOL_OPTIONS) złączone " | ". */
    static String read(InputStream in) {
        try (in) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(l -> !l.startsWith("Picked up ") && !l.startsWith("NOTE: Picked up "))
                    .collect(Collectors.joining(" | "));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 5. Teraz to samo narzędzie w OSOBNYM JVM: main woła System.exit(kod), a rodzic odczytuje exitValue().
     * Tak samo kod wyjścia widzi powłoka i serwer CI (niezerowy kod = czerwony krok budowania).
     */
    static void exitCodesInChildJvm() throws Exception {
        section("5. Prawdziwe kody wyjścia (osobny JVM)");

        show("java ... --suma 1 2 3", child("", "--suma", "1", "2", "3"));
        // WYNIK: java ... --suma 1 2 3 → kod=0 out=[6] err=[]
        show("java ... --suma --bogus", child("", "--suma", "--bogus"));
        // WYNIK: java ... --suma --bogus → kod=2 out=[] err=[błąd: nieznana opcja: --bogus | użycie: suma [-v|--verbose] [--separator=TEKST] [--] liczba...]

        // Jak sprawdzić kod wyjścia ostatniego polecenia:
        //   bash:        echo $?
        //   PowerShell:  $LASTEXITCODE
        //   cmd.exe:     echo %ERRORLEVEL%
        // W skryptach: java -jar suma.jar 1 2 && echo OK   (&& = „wykonaj dalej tylko przy kodzie 0”; w PowerShell 5.1
        // operatora && nie ma — sprawdź $LASTEXITCODE w if).
        // PUŁAPKA: System.exit nie wykonuje bloków finally (wykonuje tylko shutdown hooks = zadania zamykania).
        // Dlaczego: JVM kończy pracę od razu — dlatego zasoby zamykaj PRZED wywołaniem exit.
        // Gdy main kończy się normalnie, kod = 0; nieobsłużony wyjątek w main → kod 1 (Build02, sekcja 7).
    }

    // =================================================================================================
    // 6. STDIN, STDOUT, STDERR I POTOKI
    // =================================================================================================

    /**
     * 6. Trzy standardowe strumienie: stdin (System.in), stdout (System.out), stderr (System.err). Powłoka może
     * każdy z nich przekierować do pliku albo połączyć programy potokiem „|”.
     */
    static void standardStreams() throws Exception {
        section("6. stdin, stdout, stderr i potoki");

        // W teście stdin to ByteArrayInputStream — nie trzeba niczego wpisywać z klawiatury:
        show("stdin z ByteArrayInputStream", sum(" 7\n8 \n", "-v"));
        // WYNIK: stdin z ByteArrayInputStream → kod=0 out=[7 + 8 = 15] err=[]

        // Dziecko czyta to, co mu wyślemy przez process.getOutputStream() — to dokładnie potok „|”:
        show("echo 10 20 | java ... --suma", child("10\n20\n", "--suma"));
        // WYNIK: echo 10 20 | java ... --suma → kod=0 out=[30] err=[]

        // Polecenia w powłoce:
        //   java -jar suma.jar 1 2 > wynik.txt          — stdout do pliku (błędy dalej na ekranie)
        //   java -jar suma.jar 1 x 2> bledy.txt         — stderr do pliku
        //   java -jar suma.jar 1 2 > wszystko.txt 2>&1  — oba do jednego pliku
        //   cat liczby.txt | java -jar suma.jar          — Linux/macOS (Windows cmd: type liczby.txt | ...)
        //   Get-Content liczby.txt | java -jar suma.jar  — PowerShell
        // PUŁAPKA: w Windows PowerShell 5.1 tekst wysyłany potokiem do programu (np. java) jest kodowany według zmiennej
        // $OutputEncoding, domyślnie US-ASCII → polskie litery docierają jako "?". Dlaczego: to ustawienie starej
        // wersji PowerShell; pomaga $OutputEncoding = [System.Text.Encoding]::UTF8 (albo PowerShell 7).
        // DOBRA PRAKTYKA: czytając System.in, podawaj kodowanie jawnie (InputStreamReader(in, UTF_8)) i czytaj do końca
        // strumienia (EOF): w konsoli EOF to Ctrl+D (Linux/macOS) albo Ctrl+Z i Enter (Windows).
    }

    // =================================================================================================
    // 7. System.console() MOŻE BYĆ null
    // =================================================================================================

    /**
     * 7. System.console() daje dostęp do terminala (np. readPassword — hasło bez wyświetlania znaków). W Javie 17
     * zwraca null, gdy wejście lub wyjście nie jest terminalem: w IntelliJ, w testach, przy przekierowaniu i potoku.
     */
    static void consoleMayBeNull() throws Exception {
        section("7. System.console() może być null");

        show("dziecko z przekierowanym wejściem i wyjściem", child("", "--konsola"));
        // WYNIK: dziecko z przekierowanym wejściem i wyjściem → kod=0 out=[System.console() == null → true] err=[]
        // W TYM procesie wynik zależy od sposobu uruchomienia (terminal czy IDE), więc go nie wypisujemy:
        boolean hereNull = System.console() == null;
        note("w bieżącym procesie console == null: " + hereNull);
        // (wynik zależy od uruchomienia)

        // PUŁAPKA: System.console().readLine() bez sprawdzenia null → NullPointerException w IDE i w CI.
        // Dlaczego: tam nie ma terminala. Rozwiązanie: jeśli console == null, czytaj z System.in
        // (BufferedReader) i uprzedź, że hasło będzie widoczne. W nowszych wersjach (Java 22+) console() może zwracać
        // obiekt także bez terminala — wtedy sprawdza się dodatkowo isTerminal().
        // char[] password = console.readPassword("Hasło: ");  — tablica znaków, którą po użyciu wyzerujesz
        // (Arrays.fill(password, ' ')), czego nie da się zrobić z niezmiennym String.
    }

    // =================================================================================================
    // 8. BIBLIOTEKI I SPRING BOOT
    // =================================================================================================

    /** Coś, co dostaje argumenty po starcie aplikacji — jak CommandLineRunner w Spring Boot. */
    @FunctionalInterface
    interface Runner {
        void run(String... args);
    }

    /**
     * 8. Ręczny parser jest dobry do nauki i małych narzędzi. W prawdziwych projektach: picocli, JCommander albo
     * Apache Commons CLI — generują pomoc, sprawdzają typy, obsługują podkomendy. W Spring Boot argumenty dostaje
     * CommandLineRunner albo ApplicationRunner.
     */
    static void librariesAndSpring() {
        section("8. Biblioteki i Spring Boot");

        // picocli (przykład w komentarzu — biblioteka nie jest zależnością kursu):
        //   @Command(name = "suma", mixinStandardHelpOptions = true)      // sama doda --help i --version
        //   class Suma implements Callable<Integer> {
        //       @Option(names = {"-v", "--verbose"}) boolean verbose;
        //       @Parameters long[] numbers;
        //       public Integer call() { ... return 0; }
        //   }
        //   main: System.exit(new CommandLine(new Suma()).execute(args));
        //
        // Spring Boot: każdy bean typu CommandLineRunner jest wołany po starcie kontekstu z argumentami programu:
        //   @Bean CommandLineRunner seed(Repo repo) { return args -> repo.save(...); }
        // ApplicationRunner dostaje ApplicationArguments z gotowym podziałem: getOptionNames() dla --nazwa=wartość
        // i getNonOptionArgs() dla pozycyjnych. Kod wyjścia: SpringApplication.exit(kontekst) + ExitCodeGenerator.
        List<String> log = new ArrayList<>();
        List<Runner> runners = List.of(
                args -> log.add("seed: " + args.length + " arg."),
                args -> log.add("raport: " + String.join(",", args)));
        String[] programArgs = {"--profil=dev", "start"};
        runners.forEach(r -> r.run(programArgs));                 // tak „po starcie” robi Spring
        show("runnery wywołane po starcie", log);
        // WYNIK: runnery wywołane po starcie → [seed: 2 arg., raport: --profil=dev,start]
        // DOBRA PRAKTYKA: CommandLineRunner służy do jednorazowych zadań przy starcie (dane testowe, migracje,
        // narzędzia wsadowe), a nie do ciężkiej logiki — dlaczego: wydłuża start aplikacji i utrudnia testy.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • args nigdy nie jest null; args[0] = pierwszy argument; cudzysłowy obsługuje powłoka
     *   • konwencje: -v (flaga), --name=wartość (opcja), argumenty pozycyjne, "--" = koniec opcji, -h/--help
     *   • liczby ujemne ("-5") i pliki zaczynające się od "-" to pułapki parsera
     *   • logika w run(args, in, out, err) → int; main = System.exit(run(...)); testy bez konsoli i bez exit
     *   • kody: 0 OK, 1 błąd działania, 2 złe użycie; 128+N = sygnał; zakres 0–255 na Unix
     *   • stdout = dane, stderr = komunikaty i błędy; > plik, 2> plik, 2>&1, potok |
     *   • System.in czytaj z jawnym kodowaniem; EOF: Ctrl+D (Linux) / Ctrl+Z Enter (Windows)
     *   • System.console() bywa null (IDE, potok) — sprawdzaj przed użyciem
     *   • System.exit nie wykonuje finally
     *   • gotowce: picocli, JCommander, Commons CLI; Spring Boot: CommandLineRunner / ApplicationRunner
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego narzędzie ma metodę run(args, in, out, err) zamiast pisać od razu do System.out i wołać System.exit?
     *   2. Co wypisze:  show("x", parse(new String[]{"--", "--help"}).positional());  ?
     *   3. Co wypisze:  show("x", sum("", "-v", "5", "-2"));  ?
     *   4. ZNAJDŹ BŁĄD:
     *          public static void main(String[] args) {
     *              String file = args[0];
     *              if (args.length == 0) { System.err.println("podaj plik"); System.exit(2); }
     *          }
     *   5. Program zapisuje komunikat „Nie znaleziono pliku!” przez System.out.println i kończy się kodem 0.
     *      Co jest nie tak (dwie rzeczy)?
     *   6. Jaki kod wyjścia zobaczy powłoka Linuksa po System.exit(256)?
     *   7. Dlaczego System.console() zwraca null po uruchomieniu w IntelliJ?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        String[] sample = {"-v", "--name=Ola", "a.txt", "--", "--name=Ignoruj", "-3"};
        Check.equal("ćw. 1: wartość opcji", Optional.of("Ola"), () -> exercise1(sample, "name"));
        Check.equal("ćw. 1: brak opcji", Optional.empty(), () -> exercise1(sample, "kolor"));
        Check.equal("ćw. 2: pozycyjne", List.of("a.txt", "--name=Ignoruj", "-3"), () -> exercise2(sample));
        Check.equal("ćw. 3: suma linii", 60L, () -> exercise3(new ByteArrayInputStream(" 10\n\n20 \n30\n".getBytes(StandardCharsets.UTF_8))));
        Check.equal("ćw. 4: powtorz --razy=2 hej", "kod=0 out=[hej | hej] err=[]", () -> repeat(Build04CommandLineApps::exercise4, "--razy=2", "hej"));
        Check.equal("ćw. 4: powtorz (brak tekstu)", "kod=2 out=[] err=[błąd: brak tekstu | użycie: powtorz [--razy=N] tekst]",
                () -> repeat(Build04CommandLineApps::exercise4));
        Check.equal("ćw. 4: powtorz --razy=0 x", "kod=2 out=[] err=[błąd: --razy musi być liczbą dodatnią | użycie: powtorz [--razy=N] tekst]",
                () -> repeat(Build04CommandLineApps::exercise4, "--razy=0", "x"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", Optional.of("Ola"), () -> solution1(sample, "name"));
        Check.equal("ćw. 1 (wzorzec): brak", Optional.empty(), () -> solution1(sample, "kolor"));
        Check.equal("ćw. 2 (wzorzec)", List.of("a.txt", "--name=Ignoruj", "-3"), () -> solution2(sample));
        Check.equal("ćw. 3 (wzorzec)", 60L, () -> solution3(new ByteArrayInputStream(" 10\n\n20 \n30\n".getBytes(StandardCharsets.UTF_8))));
        Check.equal("ćw. 4 (wzorzec)", "kod=0 out=[hej | hej] err=[]", () -> repeat(Build04CommandLineApps::solution4, "--razy=2", "hej"));
        Check.equal("ćw. 4 (wzorzec): brak tekstu", "kod=2 out=[] err=[błąd: brak tekstu | użycie: powtorz [--razy=N] tekst]",
                () -> repeat(Build04CommandLineApps::solution4));
        Check.equal("ćw. 4 (wzorzec): --razy=0", "kod=2 out=[] err=[błąd: --razy musi być liczbą dodatnią | użycie: powtorz [--razy=N] tekst]",
                () -> repeat(Build04CommandLineApps::solution4, "--razy=0", "x"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 7 OK, ✘ 0 BŁĄD
    }

    /** Sygnatura narzędzia do ćwiczenia 4. */
    @FunctionalInterface
    interface Tool {
        int run(String[] args, PrintStream out, PrintStream err);
    }

    /** Uruchamia narzędzie z ćwiczenia 4 i zwraca opis wyniku (jak Run.toString()). */
    static String repeat(Tool tool, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = tool.run(args, new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Run(code, joinLines(out), joinLines(err)).toString();
    }

    static final String REPEAT_USAGE = "użycie: powtorz [--razy=N] tekst";

    /**
     * ĆWICZENIE 1 (łatwe): zwróć wartość opcji --nazwa=wartość (przed ewentualnym "--") albo Optional.empty().
     * Podpowiedź: pętla, break na "--", startsWith("--" + name + "=").
     */
    static Optional<String> exercise1(String[] args, String name) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć argumenty pozycyjne: przed "--" pomiń wszystko, co zaczyna się od "-"
     * (ale NIE liczby ujemne), po "--" bierz wszystko.
     * Podpowiedź: flaga boolean optionsEnded i metoda isNegativeNumber.
     */
    static List<String> exercise2(String[] args) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ starą pętlę na strumień linii (t16_streams). Zsumuj liczby z wejścia
     * (jedna w linii, spacje dookoła, puste linie pomijamy). Stara wersja:
     * <pre>{@code
     * BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
     * long sum = 0;
     * String line;
     * while ((line = reader.readLine()) != null) {
     *     if (!line.trim().isEmpty()) {
     *         sum += Long.parseLong(line.trim());
     *     }
     * }
     * return sum;
     * }</pre>
     * Podpowiedź: reader.lines().map(String::strip).filter(...).mapToLong(Long::parseLong).sum().
     */
    static long exercise3(InputStream in) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): napisz narzędzie „powtorz [--razy=N] tekst”: wypisuje tekst N razy (domyślnie 1),
     * każdy w osobnej linii na out, i zwraca 0. Błędy (na err, potem linia REPEAT_USAGE, kod 2):
     * brak tekstu → "błąd: brak tekstu"; --razy nie jest liczbą dodatnią → "błąd: --razy musi być liczbą dodatnią";
     * inna opcja → "błąd: nieznana opcja: X". Kilka słów tekstu łącz spacją.
     * Podpowiedź: użyj parse(args) z sekcji 2 i UsageException.
     */
    static int exercise4(String[] args, PrintStream out, PrintStream err) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static Optional<String> solution1(String[] args, String name) {
        for (String arg : args) {
            if (arg.equals("--")) {
                break;
            }
            if (arg.startsWith("--" + name + "=")) {
                return Optional.of(arg.substring(name.length() + 3));
            }
        }
        return Optional.empty();
    }

    static List<String> solution2(String[] args) {
        List<String> result = new ArrayList<>();
        boolean optionsEnded = false;
        for (String arg : args) {
            if (!optionsEnded && arg.equals("--")) {
                optionsEnded = true;
            } else if (optionsEnded || !arg.startsWith("-") || isNegativeNumber(arg)) {
                result.add(arg);
            }
        }
        return result;
    }

    static long solution3(InputStream in) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        return reader.lines().map(String::strip).filter(l -> !l.isEmpty()).mapToLong(Long::parseLong).sum();
    }

    static int solution4(String[] args, PrintStream out, PrintStream err) {
        try {
            ParsedArgs parsed = parse(args);
            if (!parsed.flags().isEmpty()) {
                throw new UsageException("nieznana opcja: " + parsed.flags().iterator().next());
            }
            for (String option : parsed.options().keySet()) {
                if (!option.equals("razy")) {
                    throw new UsageException("nieznana opcja: --" + option);
                }
            }
            int times;
            try {
                times = Integer.parseInt(parsed.options().getOrDefault("razy", "1"));
            } catch (NumberFormatException e) {
                times = 0;
            }
            if (times <= 0) {
                throw new UsageException("--razy musi być liczbą dodatnią");
            }
            if (parsed.positional().isEmpty()) {
                throw new UsageException("brak tekstu");
            }
            String text = String.join(" ", parsed.positional());
            for (int i = 0; i < times; i++) {
                out.println(text);
            }
            return 0;
        } catch (UsageException e) {
            err.println("błąd: " + e.getMessage());
            err.println(REPEAT_USAGE);
            return 2;
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo wtedy da się ją testować zwykłym wywołaniem (strumienie w pamięci, kod jako wartość zwracana),
     *      a System.exit nie zabije testów ani programu, który użyje narzędzia jako biblioteki.
     *   2. x → [--help]   (po "--" wszystko jest pozycyjne)
     *   3. x → kod=0 out=[5 + -2 = 3] err=[]
     *   4. args[0] jest czytane PRZED sprawdzeniem długości — bez argumentów poleci ArrayIndexOutOfBoundsException,
     *      zanim program wypisze pomoc. Sprawdzenie args.length musi być pierwsze.
     *   5. Komunikat o błędzie powinien iść na stderr (System.err), a program powinien zakończyć się niezerowym
     *      kodem (np. 1) — inaczej skrypt/CI uzna, że wszystko się udało.
     *   6. 0 — Linux przechowuje tylko najniższy bajt kodu (256 mod 256 = 0). Dlatego kody trzymaj w 0–125.
     *   7. W Javie 17 System.console() zwraca obiekt tylko wtedy, gdy JVM jest połączony z prawdziwym terminalem;
     *      IntelliJ przekierowuje wejście i wyjście programu do własnego okna, więc terminala nie ma.
     */
    // </editor-fold>
}
