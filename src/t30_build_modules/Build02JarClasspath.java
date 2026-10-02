package t30_build_modules;

import helpers.Check;
import helpers.TempDir;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Kompilacja, ścieżka klas (classpath) i pliki JAR — bez IDE
 *        (classpath = ścieżka klas, JAR = Java ARchive = archiwum Javy, manifest = plik opisu archiwum)
 *
 * W SKRÓCIE:
 *   javac zamienia pliki .java na .class, a JVM szuka klas na ŚCIEŻCE KLAS (lista katalogów i JAR-ów).
 *   JAR to ZIP z klasami, zasobami i manifestem; z wpisem Main-Class uruchomisz go przez java -jar.
 *   Lekcja woła prawdziwe javac i jar (ToolProvider = dostawca narzędzi) i uruchamia osobne JVM-y.
 *
 * ANALOGIA:
 *   Biblioteka miejska. Klasa to książka, pakiet to regał (app/, util/), ścieżka klas to lista budynków,
 *   które bibliotekarz (JVM) przeszukuje PO KOLEI. JAR to karton z książkami i kartką na wierzchu (manifest):
 *   „zacznij czytać od tej książki, a dodatkowe tomy są w kartonie obok”.
 *
 * JAK TO DZIAŁA:
 *   1. javac -d out src/app/Main.java src/util/Greeter.java  → out/app/Main.class, out/util/Greeter.class
 *      (katalogi = pakiety; -d = destination = katalog docelowy).
 *   2. java -cp out app.Main  → JVM ładuje app/Main.class z out, a potem LENIWIE (przy pierwszym użyciu) util/Greeter.class.
 *   3. jar --create --file app.jar --main-class app.Main -C out .  → archiwum z manifestem.
 *   4. java -jar app.jar  → Main-Class i Class-Path biorą się z manifestu (opcja -cp jest wtedy IGNOROWANA).
 *   Separator ścieżki klas: Windows ";"  Linux/macOS ":"  (w Javie: File.pathSeparator).
 *
 * SŁÓWKA:
 *   compile = kompilować; destination = cel; entry = wpis; resource = zasób; class loader = ładowacz klas;
 *   main class = klasa główna; fat/uber jar = gruby JAR (z zależnościami w środku); duplicate = duplikat;
 *   lazy = leniwy (robiony dopiero przy potrzebie); tool provider = dostawca narzędzi.
 *
 * ZOBACZ TEŻ: t30_build_modules/Build01MavenBasics (Maven robi to wszystko za Ciebie), t30_build_modules/Build03Modules
 *             (ścieżka modułów zamiast ścieżki klas), t18_io_files/Io13ZipArchives (JAR to ZIP),
 *             t19_annotations_reflection/Annotations03ReflectionBasics (Class.forName, ładowanie klas).
 * </pre>
 */
public class Build02JarClasspath {

    /** Ścieżka do programu java z TEJ SAMEJ instalacji JDK, która uruchomiła lekcję (działa też na Windows: java.exe). */
    static final String JAVA = Path.of(System.getProperty("java.home"), "bin", "java").toString();

    static final String GREETER = """
            package util;

            public class Greeter {
                public static String greet(String name) {
                    return "Cześć, " + name + "!";
                }
            }
            """;

    static final String MAIN = """
            package app;

            import util.Greeter;

            public class Main {
                public static void main(String[] args) {
                    System.out.println(Greeter.greet(args.length > 0 ? args[0] : "świecie"));
                }
            }
            """;

    public static void main(String[] args) throws Exception {
        title("Build02 — javac, classpath i pliki JAR");

        Path work = TempDir.create("build02");                  // work = katalog roboczy (tymczasowy)
        try {
            Path out = compileWithJavac(work);                 // compile with javac = kompiluj javakiem
            runWithClasspath(out);                             // run with classpath = uruchom ze ścieżką klas
            Path appJar = createAndListJar(work, out);         // create and list jar = utwórz i wylistuj JAR
            runJarWithClassPathInManifest(appJar);             // run jar = uruchom JAR (Class-Path z manifestu)
            resources(out, appJar);                            // resources = zasoby
            duplicateClasses(work);                            // duplicate classes = zduplikowane klasy
            missingClassErrors(out);                           // missing class errors = błędy brakującej klasy
            fatJars();                                         // fat jars = grube JAR-y
        } finally {
            TempDir.deleteRecursively(work);                   // sprzątamy zawsze, także po wyjątku
        }
        exercises();                                           // exercises = ćwiczenia
    }

    // =================================================================================================
    // NARZĘDZIA POMOCNICZE: javac/jar w tym samym procesie i osobny JVM
    // =================================================================================================

    /** Wynik narzędzia albo procesu: kod wyjścia i cały tekst wyjścia (z końcami linii zamienionymi na "\n"). */
    record Result(int exitCode, String output) {

        /** Linie wyjścia bez komunikatów „Picked up ...”, które JVM dopisuje, gdy ustawiona jest zmienna JAVA_TOOL_OPTIONS. */
        List<String> lines() {
            return output.lines()
                    .filter(l -> !l.startsWith("Picked up ") && !l.startsWith("NOTE: Picked up "))
                    .toList();                                  // toList (Java 16+) = do niezmiennej listy
        }
    }

    /** Uruchamia narzędzie JDK (javac, jar...) W TYM SAMYM procesie; wyjście trafia do StringWriter, nie na konsolę. */
    static Result tool(String name, String... args) {
        ToolProvider provider = ToolProvider.findFirst(name)   // findFirst = znajdź pierwszy (Optional, Java 9+)
                .orElseThrow(() -> new IllegalStateException("brak narzędzia " + name + " — uruchom lekcję na JDK, nie JRE"));
        StringWriter text = new StringWriter();
        PrintWriter writer = new PrintWriter(text);
        int exit = provider.run(writer, writer, args);          // run(out, err, args) — zwraca kod wyjścia
        writer.flush();
        return new Result(exit, text.toString().replace("\r\n", "\n"));
    }

    /** Uruchamia OSOBNY JVM (proces potomny) i zbiera jego stdout+stderr; po 10 s przerywa go siłą. */
    static Result java(Path dir, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(JAVA,
                "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8"));
        command.addAll(Arrays.asList(args));
        Process process = new ProcessBuilder(command)           // ProcessBuilder = budowniczy procesu (Build05)
                .directory(dir.toFile())                        // katalog roboczy dziecka
                .redirectErrorStream(true)                      // stderr doklejony do stdout
                .start();
        // Czytamy wyjście w tle, ŻEBY dziecko nie zablokowało się na pełnym buforze (szczegóły: Build05).
        CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readAll(process.getInputStream()));
        if (!process.waitFor(10, TimeUnit.SECONDS)) {           // waitFor = czekaj (z limitem czasu)
            process.destroyForcibly();                          // destroy forcibly = zniszcz siłą
            throw new IllegalStateException("proces potomny nie skończył się w 10 s");
        }
        return new Result(process.exitValue(), output.join().replace("\r\n", "\n"));
    }

    static String readAll(InputStream in) {
        try (in) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);   // readAllBytes (Java 9+)
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Ścieżki plików w katalogu, względne, z "/" (także na Windows), posortowane. */
    static List<String> relativeFiles(Path dir, String suffix) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {            // walk = przejdź drzewo katalogów
            return walk.filter(p -> p.toString().endsWith(suffix))
                    .map(p -> dir.relativize(p).toString().replace(File.separatorChar, '/'))
                    .sorted()
                    .toList();
        }
    }

    // =================================================================================================
    // 1. KOMPILACJA: javac -d
    // =================================================================================================

    /**
     * 1. Dwie klasy w dwóch pakietach. Plik źródłowy musi leżeć w katalogu zgodnym z pakietem (dobra praktyka,
     * wymagana przez -sourcepath), a plik .class ZAWSZE ląduje w katalogu pakietu pod -d.
     */
    static Path compileWithJavac(Path work) throws IOException {
        section("1. Kompilacja: javac -d out");

        Path src = work.resolve("src");
        Files.createDirectories(src.resolve("util"));
        Files.createDirectories(src.resolve("app"));
        Files.writeString(src.resolve("util/Greeter.java"), GREETER);   // writeString (Java 11+) — zapis w UTF-8
        Files.writeString(src.resolve("app/Main.java"), MAIN);
        Path out = work.resolve("out");

        Result javac = tool("javac", "-encoding", "UTF-8", "--release", "17", "-d", out.toString(),
                src.resolve("app/Main.java").toString(), src.resolve("util/Greeter.java").toString());
        show("javac — kod wyjścia", javac.exitCode());
        // WYNIK: javac — kod wyjścia → 0
        show("javac — wypisał coś", !javac.output().isBlank());   // isBlank (Java 11+) = czy pusty/same spacje
        // WYNIK: javac — wypisał coś → false
        show("pliki .class", relativeFiles(out, ".class"));
        // WYNIK: pliki .class → [app/Main.class, util/Greeter.class]

        // Opcje javac, które warto znać:
        //   -d out          — katalog wynikowy (bez -d pliki .class lądują OBOK plików .java — bałagan),
        //   -cp / -classpath — gdzie szukać SKOMPILOWANYCH zależności (JAR-ów, katalogów z .class),
        //   -sourcepath src — gdzie szukać ŹRÓDEŁ brakujących klas (wtedy wystarczy podać tylko Main.java),
        //   --release 17    — kompiluj pod Javę 17 (składnia i API), nawet nowszym JDK,
        //   -encoding UTF-8 — kodowanie plików źródłowych.
        // PUŁAPKA: brak -encoding UTF-8 na polskim Windows (JDK 17) → javac czyta pliki jako windows-1250 i "Cześć"
        // zamienia się w krzaki. Dlaczego: przed Javą 18 domyślne kodowanie = kodowanie systemu (od Javy 18: UTF-8).
        // Maven robi to samo przez project.build.sourceEncoding (Build01).
        // IntelliJ: Build → Build Project kompiluje do out/ (albo target/ w projekcie Mavena).
        return out;
    }

    // =================================================================================================
    // 2. URUCHAMIANIE: java -cp
    // =================================================================================================

    /**
     * 2. Uruchamiamy klasę z metodą main w NOWYM procesie JVM. Ścieżka klas mówi, gdzie szukać klas;
     * podajemy PEŁNĄ nazwę klasy (z pakietem), nie ścieżkę pliku.
     */
    static void runWithClasspath(Path out) throws IOException, InterruptedException {
        section("2. Uruchamianie: java -cp out app.Main");

        Result run = java(out.getParent(), "-cp", out.toString(), "app.Main", "Jan");
        show("kod wyjścia", run.exitCode());
        // WYNIK: kod wyjścia → 0
        show("wyjście", run.lines());
        // WYNIK: wyjście → [Cześć, Jan!]

        // Ścieżka klas z wieloma wpisami — ZAWSZE składaj ją przez File.pathSeparator:
        String classpath = String.join(File.pathSeparator, "out", "lib/a.jar", "lib/b.jar");
        show("liczba wpisów", classpath.split(File.pathSeparator).length);
        // WYNIK: liczba wpisów → 3
        // (samego tekstu nie wypisujemy: na Windows to "out;lib/a.jar;lib/b.jar", na Linuksie "out:lib/a.jar:lib/b.jar")
        // Wpis "lib/*" oznacza wszystkie JAR-y w katalogu lib (tylko JAR-y, bez podkatalogów).

        // PUŁAPKA: java -cp out app/Main.class albo java -cp out Main → „Could not find or load main class”.
        // Dlaczego: JVM chce nazwy klasy z pakietem (app.Main), a nie ścieżki pliku.
        // PUŁAPKA: brak -cp → JVM bierze zmienną środowiskową CLASSPATH, a gdy jej nie ma — bieżący katalog ".".
        // Dlaczego to źle: globalna zmienna CLASSPATH zmienia działanie WSZYSTKICH programów Javy — nie ustawiaj jej.
        // Ciekawostka (Java 11+): java Hello.java uruchamia pojedynczy plik źródłowy bez osobnego javac
        // (tak działa weryfikator tego kursu: java tools/Verify.java).
    }

    // =================================================================================================
    // 3. PLIK JAR: jar --create, --list, manifest
    // =================================================================================================

    /**
     * 3. Pakujemy osobno bibliotekę (util.jar) i aplikację (app.jar). Manifest aplikacji mówi: klasa główna to
     * app.Main, a zależność leży obok w util.jar (Class-Path).
     */
    static Path createAndListJar(Path work, Path out) throws IOException {
        section("3. Plik JAR: tworzenie i zawartość");

        Files.writeString(out.resolve("app/config.txt"), "port=8080\n");   // zasób obok klasy (sekcja 5)
        Path dist = Files.createDirectories(work.resolve("dist"));          // dist = distribution = paczka wynikowa
        Path utilJar = dist.resolve("util.jar");
        Path appJar = dist.resolve("app.jar");

        Result util = tool("jar", "--create", "--file", utilJar.toString(), "-C", out.toString(), "util");
        show("jar util.jar — kod wyjścia", util.exitCode());
        // WYNIK: jar util.jar — kod wyjścia → 0

        // Dodatkowe wpisy manifestu podajemy w pliku. Każda linia „Nazwa: wartość”, a plik MUSI kończyć się nową linią
        // (inaczej jar po cichu pominie ostatni wpis!).
        Path manifest = work.resolve("manifest.txt");
        Files.writeString(manifest, "Class-Path: util.jar\n");
        Result app = tool("jar", "--create", "--file", appJar.toString(), "--main-class", "app.Main",
                "--manifest", manifest.toString(), "-C", out.toString(), "app");
        show("jar app.jar — kod wyjścia", app.exitCode());
        // WYNIK: jar app.jar — kod wyjścia → 0

        Result list = tool("jar", "--list", "--file", appJar.toString());
        show("zawartość app.jar", list.lines().stream().sorted().toList());
        // WYNIK: zawartość app.jar → [META-INF/, META-INF/MANIFEST.MF, app/, app/Main.class, app/config.txt]

        try (JarFile jar = new JarFile(appJar.toFile())) {      // JarFile = plik JAR do odczytu (zamykamy!)
            Attributes attributes = jar.getManifest().getMainAttributes();   // main attributes = główne atrybuty
            show("Main-Class", attributes.getValue("Main-Class"));
            // WYNIK: Main-Class → app.Main
            show("Class-Path", attributes.getValue("Class-Path"));
            // WYNIK: Class-Path → util.jar
            show("Manifest-Version", attributes.getValue("Manifest-Version"));
            // WYNIK: Manifest-Version → 1.0
            // Jest też "Created-By" z wersją JDK — nie wypisujemy go, bo zależy od instalacji.
        }
        // Stara (wciąż działająca) składnia: jar cfe app.jar app.Main -C out app   (c = create, f = file, e = entry point).
        // DOBRA PRAKTYKA: długie opcje (--create, --main-class) czyta się bez ściągi — używaj ich w skryptach.
        return appJar;
    }

    // =================================================================================================
    // 4. java -jar I CLASS-PATH Z MANIFESTU
    // =================================================================================================

    /**
     * 4. java -jar app.jar: JVM czyta manifest, bierze klasę z Main-Class, a dodatkowe JAR-y z Class-Path
     * (ścieżki WZGLĘDEM katalogu, w którym leży app.jar, rozdzielone SPACJAMI).
     */
    static void runJarWithClassPathInManifest(Path appJar) throws IOException, InterruptedException {
        section("4. java -jar i Class-Path w manifeście");

        Result run = java(appJar.getParent(), "-jar", appJar.toString(), "Ola");
        show("java -jar app.jar Ola", run.lines());
        // WYNIK: java -jar app.jar Ola → [Cześć, Ola!]

        // PUŁAPKA: java -cp lib/util.jar -jar app.jar — opcja -cp jest przy -jar IGNOROWANA. Dlaczego: przy -jar jedynym
        // źródłem ścieżki klas jest JAR i jego manifest. Potrzebujesz dodatkowych JAR-ów? Class-Path w manifeście
        // albo uruchom bez -jar: java -cp app.jar;lib/* app.Main (Windows) / java -cp "app.jar:lib/*" app.Main (Linux).
        // PUŁAPKA: w Class-Path separatorem jest SPACJA (to lista adresów URL), a nie ";" ani ":".
        // Maven: maven-jar-plugin dopisuje Main-Class i Class-Path, gdy skonfigurujesz <archive><manifest>...;
        // maven-dependency-plugin skopiuje zależności obok (tak jak ten kurs kopiuje je do temp/lib).
    }

    // =================================================================================================
    // 5. ZASOBY W JAR-ACH: getResource i getResourceAsStream
    // =================================================================================================

    /**
     * 5. Zasób (resource) to plik nie-klasowy na ścieżce klas: konfiguracja, szablon, obrazek. Szukamy go przez
     * ładowacz klas, a nie przez Path — bo w JAR-ze nie jest osobnym plikiem na dysku.
     */
    static void resources(Path out, Path appJar) throws Exception {
        section("5. Zasoby: z \"/\" i bez");

        Files.writeString(out.resolve("root.txt"), "korzeń\n");
        // URLClassLoader = ładowacz klas z podanych adresów; rodzic = ładowacz platformy (bez klas tej lekcji).
        try (URLClassLoader loader = new URLClassLoader(new URL[]{out.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            Class<?> main = loader.loadClass("app.Main");      // ładuje klasę, ale jej nie uruchamia
            show("Class.getResource(\"config.txt\")  — względem pakietu app", main.getResource("config.txt") != null);
            // WYNIK: Class.getResource("config.txt")  — względem pakietu app → true
            show("Class.getResource(\"/root.txt\")   — od korzenia", main.getResource("/root.txt") != null);
            // WYNIK: Class.getResource("/root.txt")   — od korzenia → true
            show("Class.getResource(\"root.txt\")    — szuka app/root.txt", main.getResource("root.txt") != null);
            // WYNIK: Class.getResource("root.txt")    — szuka app/root.txt → false
            show("ClassLoader.getResource(\"app/config.txt\")", loader.getResource("app/config.txt") != null);
            // WYNIK: ClassLoader.getResource("app/config.txt") → true
            show("ClassLoader.getResource(\"/app/config.txt\")", loader.getResource("/app/config.txt") != null);
            // WYNIK: ClassLoader.getResource("/app/config.txt") → false
            try (InputStream in = main.getResourceAsStream("config.txt")) {   // get resource as stream = zasób jako strumień
                show("treść config.txt", new String(in.readAllBytes(), StandardCharsets.UTF_8).strip());
                // WYNIK: treść config.txt → port=8080
            }
        }
        // Zasada: Class.getResource — bez "/" względem PAKIETU klasy, z "/" od korzenia ścieżki klas;
        //         ClassLoader.getResource — ZAWSZE od korzenia i BEZ wiodącego "/" (z "/" zwraca null).

        try (URLClassLoader jarLoader = new URLClassLoader(new URL[]{appJar.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            URL url = jarLoader.getResource("app/config.txt");
            show("zasób z JAR-a — protokół URL", url.getProtocol());
            // WYNIK: zasób z JAR-a — protokół URL → jar
            URLConnection connection = url.openConnection();   // openConnection = otwórz połączenie z zasobem
            // setUseCaches(false) = bez pamięci podręcznej. PUŁAPKA: domyślnie JDK trzyma otwarty plik JAR po odczycie
            // przez adres jar: (nawet po zamknięciu ładowacza), a Windows nie pozwala potem usunąć otwartego pliku.
            connection.setUseCaches(false);
            try (InputStream in = connection.getInputStream()) {
                show("treść z JAR-a", new String(in.readAllBytes(), StandardCharsets.UTF_8).strip());
                // WYNIK: treść z JAR-a → port=8080
            }
            // PUŁAPKA: Path.of(url.toURI()) albo new File(url.getPath()) działa w IDE (zasób to zwykły plik w out/),
            // a po spakowaniu do JAR-a — nie. Dlaczego: adres jar:file:...!/app/config.txt nie wskazuje pliku na dysku.
            expectThrows("Path.of(url.toURI()) dla zasobu w JAR-ze", () -> Path.of(url.toURI()));
            // WYNIK: ✔ Path.of(url.toURI()) dla zasobu w JAR-ze → rzucono FileSystemNotFoundException: (brak komunikatu)
        }
        // DOBRA PRAKTYKA: zasoby czytaj ZAWSZE przez getResourceAsStream (w try-with-resources) — działa i w IDE, i w JAR-ze.
        // W Mavenie zasoby kładziesz w src/main/resources — trafiają do korzenia JAR-a, więc czytasz je np.
        // getResourceAsStream("/application.properties"). Spring Boot czyta tak samo swój application.properties.
        // Ładowacze zamknęliśmy (try-with-resources) — na Windows otwarty JAR blokowałby usunięcie katalogu.
    }

    // =================================================================================================
    // 6. KOLEJNOŚĆ NA ŚCIEŻCE KLAS I ZDUPLIKOWANE KLASY
    // =================================================================================================

    /** Kompiluje klasę dup.Version zwracającą podany tekst do osobnego katalogu. */
    static Path compileVersion(Path work, String dirName, String text) throws IOException {
        Path src = Files.createDirectories(work.resolve(dirName + "-src/dup"));
        Files.writeString(src.resolve("Version.java"),
                "package dup;\npublic class Version { public static String text() { return \"" + text + "\"; } }\n");
        Path out = work.resolve(dirName);
        Result javac = tool("javac", "-encoding", "UTF-8", "-d", out.toString(), src.resolve("Version.java").toString());
        if (javac.exitCode() != 0) {
            throw new IllegalStateException(javac.output());
        }
        return out;
    }

    /** Ładuje dup.Version z podanej ścieżki klas (w tej kolejności) i woła text(). */
    static String versionFrom(Path... classpath) throws Exception {
        URL[] urls = new URL[classpath.length];
        for (int i = 0; i < classpath.length; i++) {
            urls[i] = classpath[i].toUri().toURL();
        }
        try (URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
            return (String) loader.loadClass("dup.Version").getMethod("text").invoke(null);   // invoke = wywołaj
        }
    }

    /**
     * 6. Ta sama klasa (ta sama pełna nazwa) w dwóch miejscach ścieżki klas → wygrywa PIERWSZE miejsce,
     * drugie jest po cichu ignorowane. Zwykle zdarza się to przy dwóch wersjach jednej biblioteki.
     */
    static void duplicateClasses(Path work) throws Exception {
        section("6. Kolejność ścieżki klas i duplikaty");

        Path v1 = compileVersion(work, "v1", "wersja 1");
        Path v2 = compileVersion(work, "v2", "wersja 2");
        show("ścieżka v1, v2", versionFrom(v1, v2));
        // WYNIK: ścieżka v1, v2 → wersja 1
        show("ścieżka v2, v1", versionFrom(v2, v1));
        // WYNIK: ścieżka v2, v1 → wersja 2

        // PUŁAPKA: lib/jackson-2.12.jar i lib/jackson-2.16.jar naraz na ścieżce klas → część klas z jednej wersji,
        // część (np. nowe klasy) z drugiej → NoSuchMethodError albo dziwne zachowanie, inne na innym komputerze
        // (kolejność plików w lib/* nie jest gwarantowana!). Dlaczego: JVM nie ostrzega o duplikatach — bierze pierwszy.
        // DOBRA PRAKTYKA: jedna wersja każdej biblioteki — pilnuje tego Maven (Build01, „nearest wins”);
        // ./mvnw dependency:tree pokaże, skąd wzięła się druga wersja.
        // Moduły JPMS (Build03) wykrywają taki konflikt przy starcie: ten sam pakiet w dwóch modułach = błąd.
    }

    // =================================================================================================
    // 7. ClassNotFoundException vs NoClassDefFoundError
    // =================================================================================================

    /**
     * 7. Dwa podobne błędy, dwie różne historie:
     * ClassNotFoundException — WYJĄTEK (checked) przy jawnym ładowaniu po nazwie (Class.forName, loadClass);
     * NoClassDefFoundError — BŁĄD (Error): klasa była przy kompilacji, a w runtime jej zabrakło.
     */
    static void missingClassErrors(Path out) throws Exception {
        section("7. ClassNotFoundException vs NoClassDefFoundError");

        expectThrows("Class.forName(\"util.Nieistniejaca\")", () -> Class.forName("util.Nieistniejaca"));
        // WYNIK: ✔ Class.forName("util.Nieistniejaca") → rzucono ClassNotFoundException: util.Nieistniejaca

        // Teraz symulujemy „zapomniany JAR”: kasujemy util/Greeter.class i uruchamiamy app.Main.
        Files.delete(out.resolve("util/Greeter.class"));
        Result run = java(out.getParent(), "-cp", out.toString(), "app.Main");
        show("kod wyjścia", run.exitCode());
        // WYNIK: kod wyjścia → 1
        show("typ błędu", errorType(run.output()));
        // WYNIK: typ błędu → NoClassDefFoundError
        show("przyczyna to ClassNotFoundException", run.output().contains("Caused by: java.lang.ClassNotFoundException"));
        // WYNIK: przyczyna to ClassNotFoundException → true
        // Pełny wydruk zawiera ślad stosu (stack trace) — wypisujemy tylko typ, bo reszta zależy od wersji JDK.

        // Kod wyjścia 1 = nieobsłużony wyjątek w main. Typowe przyczyny NoClassDefFoundError:
        //   • brak JAR-a zależności przy uruchomieniu (kompilowałeś z -cp lib/*, uruchamiasz bez),
        //   • zależność w zakresie provided, a serwer jej nie dostarcza (Build01),
        //   • wyjątek w bloku static klasy: pierwsza próba = ExceptionInInitializerError, KAŻDA następna =
        //     NoClassDefFoundError „Could not initialize class ...” — szukaj pierwszego błędu w logu!
        // PUŁAPKA: łapanie NoClassDefFoundError w catch (Exception e) — nie zadziała. Dlaczego: to Error, nie Exception.
    }

    /** Wyciąga prostą nazwę typu z linii „Exception in thread "main" java.lang.XyzError: ...”. */
    static String errorType(String output) {
        return output.lines()
                .filter(l -> l.startsWith("Exception in thread"))
                .map(l -> l.replaceFirst("^Exception in thread \"[^\"]*\" ([\\w.$]+).*$", "$1"))
                .map(name -> name.substring(name.lastIndexOf('.') + 1))
                .findFirst()
                .orElse("(brak)");
    }

    // =================================================================================================
    // 8. GRUBE JAR-Y (FAT / UBER JAR) I SPRING BOOT
    // =================================================================================================

    /**
     * 8. Zamiast app.jar + folderu lib można dostarczyć JEDEN plik z wszystkim w środku. Dwa sposoby:
     * „rozpakuj i wymieszaj” (Maven Shade / Assembly) albo „JAR-y w JAR-ze” (Spring Boot).
     */
    static void fatJars() {
        section("8. Grube JAR-y i Spring Boot");

        Map<String, String> bootJar = new LinkedHashMap<>();
        bootJar.put("META-INF/MANIFEST.MF", "Main-Class: JarLauncher (program startowy Spring Boot)");
        bootJar.put("org/springframework/boot/loader/", "klasy ładowacza Spring Boot");
        bootJar.put("BOOT-INF/classes/", "TWOJE klasy i zasoby");
        bootJar.put("BOOT-INF/lib/", "JAR-y zależności (niezmienione, w środku)");
        showEach("układ wykonywalnego JAR-a Spring Boot", bootJar);
        // WYNIK: układ wykonywalnego JAR-a Spring Boot (liczba kluczy: 4):
        // WYNIK: • META-INF/MANIFEST.MF → Main-Class: JarLauncher (program startowy Spring Boot)
        // WYNIK: • org/springframework/boot/loader/ → klasy ładowacza Spring Boot
        // WYNIK: • BOOT-INF/classes/ → TWOJE klasy i zasoby
        // WYNIK: • BOOT-INF/lib/ → JAR-y zależności (niezmienione, w środku)

        // Zwykły JVM nie umie czytać JAR-ów zagnieżdżonych w JAR-ze — dlatego Main-Class wskazuje JarLauncher
        // Spring Boota, a Twoja klasa z main trafia do wpisu Start-Class. Taki JAR buduje cel repackage wtyczki
        // spring-boot-maven-plugin; uruchamiasz go po prostu: java -jar sklep-1.0.0.jar.
        // Maven Shade Plugin (shade = przesłonić) robi inaczej: rozpakowuje wszystkie zależności i pakuje je razem
        // z Twoimi klasami do jednego płaskiego JAR-a.
        // PUŁAPKA: w Shade dwie biblioteki z tym samym plikiem zasobu (np. META-INF/services/...) — przy mieszaniu
        // jeden nadpisze drugi. Dlaczego: w płaskim JAR-ze nie ma dwóch plików o tej samej ścieżce; wtyczka
        // potrzebuje „transformerów”, które łączą takie pliki.
        note("gruby JAR = jeden plik do wdrożenia, ale zwykle kilkadziesiąt MB");
        // WYNIK: ℹ gruby JAR = jeden plik do wdrożenia, ale zwykle kilkadziesiąt MB
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • javac -encoding UTF-8 -d out -cp lib/* pliki.java  → klasy w katalogach pakietów pod out/
     *   • java -cp out pakiet.Klasa argumenty — PEŁNA nazwa klasy, nie ścieżka pliku
     *   • separator ścieżki klas: Windows ";", Linux ":" → File.pathSeparator; "lib/*" = wszystkie JAR-y w lib
     *   • jar --create --file app.jar --main-class app.Main -C out .  /  jar --list --file app.jar
     *   • manifest: Main-Class, Class-Path (spacje, względem położenia JAR-a); plik manifestu kończ nową linią
     *   • java -jar IGNORUJE -cp; ścieżka klas pochodzi z manifestu
     *   • duplikat klasy na ścieżce klas → wygrywa pierwszy wpis, bez ostrzeżenia
     *   • ClassNotFoundException = jawne ładowanie po nazwie; NoClassDefFoundError = była przy kompilacji, brak w runtime
     *   • zasoby: getResourceAsStream; Class: bez "/" = względem pakietu, z "/" = od korzenia; ClassLoader: bez "/"
     *   • zasób w JAR-ze to nie plik — nie rób z niego Path/File
     *   • gruby JAR: Shade (płasko) albo Spring Boot (BOOT-INF/lib + JarLauncher)
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego java -cp out app/Main.class nie działa? Jak powinno wyglądać polecenie?
     *   2. Co wypisze:  System.out.println(main.getResource("/config.txt") != null);  gdy config.txt leży w out/app/?
     *   3. Czym różni się ClassNotFoundException od NoClassDefFoundError? Który z nich złapiesz przez catch (Exception e)?
     *   4. ZNAJDŹ BŁĄD:  java -cp lib/util.jar -jar app.jar   (app.jar nie ma Class-Path) → NoClassDefFoundError. Dlaczego?
     *   5. ZNAJDŹ BŁĄD:  Path config = Path.of(getClass().getResource("/app.properties").toURI());
     *                    — w IntelliJ działa, po zbudowaniu JAR-a rzuca wyjątek.
     *   6. Co wypisze program, gdy na ścieżce klas jest najpierw v2, potem v1 (sekcja 6)?
     *   7. Po co javac opcja -encoding UTF-8, skoro pliki zapisałeś w UTF-8?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: wpis JAR-a dla klasy", "pl/kurs/util/Greeter.class", () -> exercise1("pl.kurs.util.Greeter"));
        Check.equal("ćw. 2: polecenie java", List.of("java", "-cp", "out;lib/a.jar", "app.Main", "Jan"),
                () -> exercise2("java", List.of("out", "lib/a.jar"), ";", "app.Main", List.of("Jan")));
        Check.equal("ćw. 3: manifest", MANIFEST_EXPECTED, () -> exercise3(MANIFEST_TEXT));
        Check.equal("ćw. 4: kto wygrywa", Optional.of("lib/b.jar"), () -> exercise4(CLASSPATH, "com.x.Json"));
        Check.equal("ćw. 4: brak klasy", Optional.empty(), () -> exercise4(CLASSPATH, "com.x.Brak"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "pl/kurs/util/Greeter.class", () -> solution1("pl.kurs.util.Greeter"));
        Check.equal("ćw. 2 (wzorzec)", List.of("java", "-cp", "out;lib/a.jar", "app.Main", "Jan"),
                () -> solution2("java", List.of("out", "lib/a.jar"), ";", "app.Main", List.of("Jan")));
        Check.equal("ćw. 3 (wzorzec)", MANIFEST_EXPECTED, () -> solution3(MANIFEST_TEXT));
        Check.equal("ćw. 4 (wzorzec)", Optional.of("lib/b.jar"), () -> solution4(CLASSPATH, "com.x.Json"));
        Check.equal("ćw. 4 (wzorzec): brak", Optional.empty(), () -> solution4(CLASSPATH, "com.x.Brak"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /** Manifest do ćwiczenia 3: druga linia Class-Path to KONTYNUACJA (zaczyna się od jednej spacji). */
    static final String MANIFEST_TEXT = "Manifest-Version: 1.0\r\nMain-Class: app.Main\r\nClass-Path: lib/a.jar lib/b\r\n .jar\r\n\r\n";

    static final Map<String, String> MANIFEST_EXPECTED = new TreeMap<>(Map.of(
            "Manifest-Version", "1.0", "Main-Class", "app.Main", "Class-Path", "lib/a.jar lib/b.jar"));

    /** Ścieżka klas do ćwiczenia 4: wpis → klasy, które zawiera (w kolejności ścieżki). */
    static final Map<String, List<String>> CLASSPATH = classpathForExercise();

    static Map<String, List<String>> classpathForExercise() {
        Map<String, List<String>> cp = new LinkedHashMap<>();
        cp.put("out", List.of("app.Main"));
        cp.put("lib/b.jar", List.of("com.x.Json", "com.x.Parser"));
        cp.put("lib/a.jar", List.of("com.x.Json"));
        return cp;
    }

    /**
     * ĆWICZENIE 1 (łatwe): zamień pełną nazwę klasy na nazwę wpisu w JAR-ze, np. "app.Main" → "app/Main.class".
     * Podpowiedź: wpisy JAR-a ZAWSZE używają "/" (nawet na Windows) — nie używaj File.separator.
     */
    static String exercise1(String className) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zbuduj listę argumentów polecenia: [java, -cp, ścieżka-klas, klasa, argumenty...].
     * Ścieżkę klas sklej z wpisów podanym separatorem (w prawdziwym kodzie: File.pathSeparator).
     * Podpowiedź: String.join + ArrayList; zwróć List.copyOf(...).
     */
    static List<String> exercise2(String javaExe, List<String> classpath, String separator, String mainClass,
                                  List<String> args) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): odczytaj główną sekcję manifestu do TreeMap. Zasady formatu: linie kończą się "\r\n"
     * albo "\n"; „Nazwa: wartość”; linia zaczynająca się od JEDNEJ spacji to dalszy ciąg poprzedniej wartości
     * (manifest łamie linie dłuższe niż 72 bajty); pusta linia kończy sekcję główną.
     * Podpowiedź: lines(), sprawdzaj startsWith(" "), pamiętaj ostatni klucz.
     */
    static Map<String, String> exercise3(String manifest) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): z której pozycji ścieżki klas JVM weźmie klasę? Mapa zachowuje kolejność ścieżki.
     * Zwróć pierwszy wpis zawierający klasę albo Optional.empty(), gdy klasy nie ma nigdzie.
     * Podpowiedź: entrySet().stream().filter(...).map(Map.Entry::getKey).findFirst().
     */
    static Optional<String> exercise4(Map<String, List<String>> classpath, String className) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String className) {
        return className.replace('.', '/') + ".class";
    }

    static List<String> solution2(String javaExe, List<String> classpath, String separator, String mainClass,
                                  List<String> args) {
        List<String> command = new ArrayList<>(List.of(javaExe, "-cp", String.join(separator, classpath), mainClass));
        command.addAll(args);
        return List.copyOf(command);
    }

    static Map<String, String> solution3(String manifest) {
        Map<String, String> result = new TreeMap<>();
        String lastKey = null;
        for (String line : manifest.lines().toList()) {
            if (line.isEmpty()) {
                break;                                          // koniec sekcji głównej
            }
            if (line.startsWith(" ") && lastKey != null) {
                result.merge(lastKey, line.substring(1), String::concat);   // doklej kontynuację
            } else {
                int colon = line.indexOf(": ");
                lastKey = line.substring(0, colon);
                result.put(lastKey, line.substring(colon + 2));
            }
        }
        return result;
    }

    static Optional<String> solution4(Map<String, List<String>> classpath, String className) {
        return classpath.entrySet().stream()
                .filter(e -> e.getValue().contains(className))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. java oczekuje pełnej nazwy klasy, nie ścieżki pliku. Poprawnie: java -cp out app.Main.
     *   2. false — "/config.txt" szuka w korzeniu ścieżki klas (out/config.txt), a plik leży w out/app/.
     *      Działa main.getResource("config.txt") albo main.getResource("/app/config.txt").
     *   3. ClassNotFoundException — wyjątek sprawdzany (checked) przy jawnym ładowaniu po nazwie (Class.forName, loadClass).
     *      NoClassDefFoundError — Error: klasa była podczas kompilacji, a w runtime nie da się jej załadować.
     *      catch (Exception e) złapie tylko ClassNotFoundException.
     *   4. Przy -jar opcja -cp jest ignorowana — util.jar nie trafia na ścieżkę klas. Dodaj Class-Path: util.jar
     *      do manifestu albo uruchom bez -jar: java -cp app.jar;lib/util.jar app.Main (na Linuksie z ":").
     *   5. W JAR-ze zasób ma adres jar:file:...!/app.properties — to nie plik na dysku, Path.of rzuca
     *      FileSystemNotFoundException. Czytaj przez getResourceAsStream("/app.properties").
     *   6. ścieżka v2, v1 → wersja 2 — wygrywa pierwszy wpis.
     *   7. Bo javac nie wie, jak plik zapisano: w JDK 17 bez -encoding użyje kodowania systemu (na polskim
     *      Windows windows-1250) i zepsuje polskie znaki. Od Javy 18 domyślnie jest UTF-8.
     */
    // </editor-fold>
}
