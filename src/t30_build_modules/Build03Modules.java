package t30_build_modules;

import helpers.Check;
import helpers.TempDir;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import javax.lang.model.SourceVersion;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Moduły JPMS (Java 9+) — module-info.java, ścieżka modułów, silna enkapsulacja
 *        (JPMS = Java Platform Module System = system modułów platformy Java)
 *
 * W SKRÓCIE:
 *   Moduł to JAR z plikiem module-info.class, który mówi: „potrzebuję TYCH modułów” (requires)
 *   i „pokazuję światu TYLKO te pakiety” (exports). Reszta jest ukryta — nawet klasy public.
 *   Sam JDK od Javy 9 jest podzielony na moduły (java.base, java.sql, java.xml...).
 *
 * ANALOGIA:
 *   Biurowiec z recepcją. Każda firma (moduł) ma na drzwiach listę: „przyjmujemy interesantów w pokojach
 *   101 i 102” (exports). Do pozostałych pokojów nie wejdziesz, choć drzwi nie mają zamka (public).
 *   Żeby w ogóle odwiedzić firmę, musisz mieć ją wpisaną na swojej przepustce (requires).
 *
 * JAK TO DZIAŁA:
 *   module com.example.app {                 → nazwa modułu (jak pakiet: odwrócona domena)
 *       requires com.example.greeter;        → czytam (reads) ten moduł
 *       uses com.example.greeter.api.Greeter;→ szukam implementacji przez ServiceLoader
 *   }
 *   javac --module-source-path src -d mods -m com.example.app,com.example.greeter
 *   java  --module-path mods -m com.example.app/com.example.app.Main
 *   Dostęp do klasy = (moduł CZYTA moduł docelowy) ORAZ (pakiet jest EKSPORTOWANY) ORAZ (klasa jest public).
 *
 * SŁÓWKA:
 *   module = moduł; requires = wymaga; exports = eksportuje (udostępnia); opens = otwiera (dla refleksji);
 *   transitive = przechodni; uses = używa; provides ... with = dostarcza ... za pomocą; module path = ścieżka modułów;
 *   unnamed module = moduł nienazwany; automatic module = moduł automatyczny; split package = rozdzielony pakiet;
 *   service loader = ładowacz usług; encapsulation = enkapsulacja (ukrywanie szczegółów).
 *
 * ZOBACZ TEŻ: t30_build_modules/Build02JarClasspath (ścieżka klas), t19_annotations_reflection/Annotations03ReflectionBasics
 *             (setAccessible i jego granice), t27_clean_code_pitfalls/CleanCode03Architecture (granice modułów w architekturze).
 * </pre>
 */
public class Build03Modules {

    static final String JAVA = Path.of(System.getProperty("java.home"), "bin", "java").toString();

    public static void main(String[] args) throws Exception {
        title("Build03 — moduły JPMS");

        modulesInJdk();                                    // modules in JDK = moduły w samym JDK
        moduleInfoAnatomy();                               // module-info anatomy = budowa module-info
        Path work = TempDir.create("build03");
        try {
            Path src = writeModuleSources(work);           // write module sources = zapisz źródła modułów
            Path mods = compileAndInspect(work, src);      // compile and inspect = skompiluj i obejrzyj
            runOnModulePath(mods);                         // run on module path = uruchom na ścieżce modułów
            encapsulationCompileError(work, src);          // encapsulation compile error = błąd kompilacji enkapsulacji
            reflectionIntoJdk();                           // reflection into JDK = refleksja we wnętrzu JDK
            serviceLoaderOnClasspath(work);                // service loader on classpath = ServiceLoader na ścieżce klas
            automaticModulesAndSplitPackages(work);        // automatic modules and split packages
            jdepsAndJlink(work);                           // jdeps and jlink = analiza zależności i własne środowisko
        } finally {
            TempDir.deleteRecursively(work);
        }
        exercises();                                       // exercises = ćwiczenia
    }

    // =================================================================================================
    // NARZĘDZIA POMOCNICZE (jak w Build02)
    // =================================================================================================

    record Result(int exitCode, String output) {
        /** Linie bez „Picked up ...” — JVM wypisuje je, gdy ustawiono JAVA_TOOL_OPTIONS; to nie jest wynik programu. */
        List<String> lines() {
            return output.lines().filter(l -> !l.startsWith("Picked up ") && !l.startsWith("NOTE: Picked up ")).toList();
        }
    }

    static Result tool(String name, String... args) {
        ToolProvider provider = ToolProvider.findFirst(name).orElseThrow();   // findFirst = znajdź pierwszy
        StringWriter text = new StringWriter();
        PrintWriter writer = new PrintWriter(text);
        int exit = provider.run(writer, writer, args);
        writer.flush();
        return new Result(exit, text.toString().replace("\r\n", "\n"));
    }

    static Result java(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(JAVA,
                "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readAll(process.getInputStream()));
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("proces potomny nie skończył się w 10 s");
        }
        return new Result(process.exitValue(), output.join().replace("\r\n", "\n"));
    }

    static String readAll(InputStream in) {
        try (in) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    // =================================================================================================
    // 1. MODUŁY W SAMYM JDK
    // =================================================================================================

    /**
     * 1. Zanim napiszemy własne moduły — obejrzyjmy JDK. Każda klasa należy do jakiegoś modułu. Klasy z classpath
     * (jak ta lekcja) trafiają do jednego wspólnego MODUŁU NIENAZWANEGO.
     */
    static void modulesInJdk() {
        section("1. Moduły w JDK");

        show("String należy do", String.class.getModule().getName());          // getModule (Java 9+)
        // WYNIK: String należy do → java.base
        show("java.sql.Connection należy do", java.sql.Connection.class.getModule().getName());
        // WYNIK: java.sql.Connection należy do → java.sql
        show("ta lekcja jest w module nazwanym", Build03Modules.class.getModule().isNamed());
        // WYNIK: ta lekcja jest w module nazwanym → false

        ModuleDescriptor sql = ModuleLayer.boot().findModule("java.sql").orElseThrow().getDescriptor();
        show("java.sql eksportuje", sql.exports().stream().map(ModuleDescriptor.Exports::source).sorted().toList());
        // WYNIK: java.sql eksportuje → [java.sql, javax.sql]
        show("java.sql wymaga", describeRequires(sql));
        // WYNIK: java.sql wymaga → [mandated java.base, transitive java.logging, transitive java.transaction.xa, transitive java.xml]
        // mandated = narzucony: java.base czyta KAŻDY moduł, nie trzeba go wpisywać.
        // transitive = kto wymaga java.sql, ten automatycznie czyta też java.logging i java.xml (bo typy z nich
        // występują w API java.sql — np. metoda Driver.getParentLogger() zwraca java.util.logging.Logger).
        // Lista modułów Twojego JDK: java --list-modules (kilkadziesiąt pozycji; liczba zależy od wersji i dystrybucji).
    }

    /** "modyfikatory nazwa" posortowane po nazwie; modyfikatory małymi literami. */
    static List<String> describeRequires(ModuleDescriptor descriptor) {
        return descriptor.requires().stream()
                .sorted()                                        // Requires porównuje po nazwie modułu
                .map(r -> (r.modifiers().stream().map(m -> m.name().toLowerCase(Locale.ROOT)).sorted()
                        .collect(Collectors.joining(" ")) + " " + r.name()).strip())
                .toList();
    }

    // =================================================================================================
    // 2. BUDOWA module-info.java
    // =================================================================================================

    /** 2. Słowa kluczowe module-info. To NIE są zastrzeżone słowa w zwykłym kodzie — tylko w tym jednym pliku. */
    static void moduleInfoAnatomy() {
        section("2. Budowa module-info.java");

        Map<String, String> keywords = new LinkedHashMap<>();
        keywords.put("requires M", "czytam moduł M (bez tego nie widzę jego typów)");
        keywords.put("requires transitive M", "i każdy, kto czyta mnie, czyta też M");
        keywords.put("requires static M", "M potrzebny tylko przy kompilacji (opcjonalny w runtime)");
        keywords.put("exports P", "pakiet P widoczny dla innych modułów (kompilacja i runtime)");
        keywords.put("exports P to M", "eksport kwalifikowany — tylko dla modułu M");
        keywords.put("opens P", "pakiet P otwarty dla REFLEKSJI (także pola prywatne)");
        keywords.put("uses S", "szukam implementacji usługi S przez ServiceLoader");
        keywords.put("provides S with I", "dostarczam implementację I usługi S");
        showEach("słowa module-info", keywords);
        // WYNIK: słowa module-info (liczba kluczy: 8):
        // WYNIK: • requires M → czytam moduł M (bez tego nie widzę jego typów)
        // WYNIK: • requires transitive M → i każdy, kto czyta mnie, czyta też M
        // WYNIK: • requires static M → M potrzebny tylko przy kompilacji (opcjonalny w runtime)
        // WYNIK: • exports P → pakiet P widoczny dla innych modułów (kompilacja i runtime)
        // WYNIK: • exports P to M → eksport kwalifikowany — tylko dla modułu M
        // WYNIK: • opens P → pakiet P otwarty dla REFLEKSJI (także pola prywatne)
        // WYNIK: • uses S → szukam implementacji usługi S przez ServiceLoader
        // WYNIK: • provides S with I → dostarczam implementację I usługi S

        // Dodatkowo: open module M { ... } — cały moduł otwarty dla refleksji (wygodne dla frameworków typu Spring/Hibernate,
        // które czytają pola prywatne encji). exports pozwala czytać publiczne API, ale NIE daje refleksji do
        // pól prywatnych — do tego służy opens.
        // PUŁAPKA: exports com.example dotyczy TYLKO pakietu com.example, NIE com.example.api. Dlaczego: w Javie
        // nie ma „podpakietów” — com.example.api to zupełnie osobny pakiet, który trzeba wyeksportować osobno.
        // PUŁAPKA: requires static (np. dla adnotacji Lomboka) — w runtime moduł może być nieobecny; kod, który mimo
        // to sięgnie po jego klasy, dostanie NoClassDefFoundError.
    }

    // =================================================================================================
    // 3. DWA MODUŁY: KOMPILACJA I OGLĄDANIE DESKRYPTORÓW
    // =================================================================================================

    /** Układ katalogów dla --module-source-path: src/NAZWA.MODUŁU/module-info.java + pakiety. */
    static Path writeModuleSources(Path work) throws IOException {
        Path src = work.resolve("src");
        write(src.resolve("com.example.greeter/module-info.java"), """
                module com.example.greeter {
                    exports com.example.greeter.api;
                    provides com.example.greeter.api.Greeter with com.example.greeter.internal.PolishGreeter;
                }
                """);
        write(src.resolve("com.example.greeter/com/example/greeter/api/Greeter.java"), """
                package com.example.greeter.api;

                public interface Greeter {
                    String hello(String name);
                }
                """);
        write(src.resolve("com.example.greeter/com/example/greeter/internal/PolishGreeter.java"), """
                package com.example.greeter.internal;

                import com.example.greeter.api.Greeter;

                public class PolishGreeter implements Greeter {
                    @Override
                    public String hello(String name) {
                        return "Cześć, " + name + "!";
                    }
                }
                """);
        write(src.resolve("com.example.app/module-info.java"), """
                module com.example.app {
                    requires com.example.greeter;
                    uses com.example.greeter.api.Greeter;
                }
                """);
        write(src.resolve("com.example.app/com/example/app/Main.java"), """
                package com.example.app;

                import com.example.greeter.api.Greeter;
                import java.util.ServiceLoader;

                public class Main {
                    public static void main(String[] args) throws Exception {
                        System.out.println("moduł Main: " + Main.class.getModule().getName());
                        Greeter greeter = ServiceLoader.load(Greeter.class).findFirst().orElseThrow();
                        System.out.println(greeter.hello("Jan"));
                        System.out.println("implementacja z modułu: " + greeter.getClass().getModule().getName());
                        try {
                            Class<?> hidden = Class.forName("com.example.greeter.internal.PolishGreeter");
                            hidden.getDeclaredConstructor().newInstance();
                            System.out.println("refleksja: udało się");
                        } catch (IllegalAccessException e) {
                            System.out.println("refleksja: " + e.getClass().getSimpleName());
                        }
                    }
                }
                """);
        return src;
    }

    /**
     * 3. javac w trybie wielomodułowym: --module-source-path wskazuje katalog z podkatalogami nazwanymi jak moduły,
     * -m (--module) wybiera moduły do kompilacji. Wynik: mods/NAZWA.MODUŁU/... z module-info.class w środku.
     */
    static Path compileAndInspect(Path work, Path src) throws IOException {
        section("3. Kompilacja dwóch modułów i ich deskryptory");

        Path mods = work.resolve("mods");
        Result javac = tool("javac", "-encoding", "UTF-8", "-d", mods.toString(),
                "--module-source-path", src.toString(), "-m", "com.example.app,com.example.greeter");
        show("javac — kod wyjścia", javac.exitCode());
        // WYNIK: javac — kod wyjścia → 0
        show("module-info.class w greeter", Files.exists(mods.resolve("com.example.greeter/module-info.class")));
        // WYNIK: module-info.class w greeter → true

        // ModuleFinder (wyszukiwacz modułów) czyta skompilowane module-info.class — tak samo jak JVM przy starcie.
        ModuleFinder finder = ModuleFinder.of(mods.resolve("com.example.greeter"), mods.resolve("com.example.app"));
        ModuleDescriptor greeter = finder.find("com.example.greeter").orElseThrow().descriptor();
        ModuleDescriptor app = finder.find("com.example.app").orElseThrow().descriptor();
        show("greeter: pakiety", greeter.packages().stream().sorted().toList());
        // WYNIK: greeter: pakiety → [com.example.greeter.api, com.example.greeter.internal]
        show("greeter: eksportuje", greeter.exports().stream().map(ModuleDescriptor.Exports::source).sorted().toList());
        // WYNIK: greeter: eksportuje → [com.example.greeter.api]
        show("greeter: dostarcza", greeter.provides().stream()
                .map(p -> p.service() + " → " + p.providers()).sorted().toList());
        // WYNIK: greeter: dostarcza → [com.example.greeter.api.Greeter → [com.example.greeter.internal.PolishGreeter]]
        show("app: wymaga", describeRequires(app));
        // WYNIK: app: wymaga → [com.example.greeter, mandated java.base]
        show("app: używa", app.uses().stream().sorted().toList());
        // WYNIK: app: używa → [com.example.greeter.api.Greeter]

        // Maven: każdy moduł to zwykle osobny projekt (podmoduł w pom.xml rodzica) z src/main/java/module-info.java;
        // maven-compiler-plugin sam wykrywa module-info i kompiluje z --module-path zamiast -classpath.
        // IntelliJ: File → New → Module; IDE podpowiada brakujące requires (Alt+Enter → „Add requires ...”).
        return mods;
    }

    // =================================================================================================
    // 4. URUCHAMIANIE NA ŚCIEŻCE MODUŁÓW
    // =================================================================================================

    /**
     * 4. java --module-path mods -m moduł/klasa. JVM najpierw ROZWIĄZUJE graf modułów (czy wszystkie requires są
     * dostępne, czy nie ma konfliktów) — brakujący moduł to błąd przy STARCIE, a nie w połowie działania.
     */
    static void runOnModulePath(Path mods) throws IOException, InterruptedException {
        section("4. Uruchamianie: java --module-path mods -m ...");

        Result run = java("--module-path", mods.toString(), "-m", "com.example.app/com.example.app.Main");
        show("kod wyjścia", run.exitCode());
        // WYNIK: kod wyjścia → 0
        showEach("wyjście procesu", run.lines());
        // WYNIK: wyjście procesu (liczba elementów: 4):
        // WYNIK: • moduł Main: com.example.app
        // WYNIK: • Cześć, Jan!
        // WYNIK: • implementacja z modułu: com.example.greeter
        // WYNIK: • refleksja: IllegalAccessException

        // Co tu się stało:
        //   • app nie zna klasy PolishGreeter (pakiet internal nie jest eksportowany), a mimo to jej używa —
        //     przez interfejs Greeter i ServiceLoader (uses / provides). To wzorzec „API + ukryta implementacja”.
        //   • Class.forName znalazł klasę, ale utworzenie obiektu refleksją się nie udało: IllegalAccessException,
        //     bo internal nie jest ani eksportowany, ani otwarty (opens). Silna enkapsulacja działa też w runtime.
        // Ścieżka modułów to lista katalogów/JAR-ów (separator jak w classpath: File.pathSeparator); -p to skrót
        // --module-path. Brak modułu → „Module com.example.greeter not found, required by com.example.app” przy starcie.
    }

    // =================================================================================================
    // 5. SILNA ENKAPSULACJA: BŁĄD KOMPILACJI
    // =================================================================================================

    /**
     * 5. Próbujemy zaimportować klasę z NIEEKSPORTOWANEGO pakietu. Na ścieżce klas to by przeszło (klasa jest public),
     * w module — kompilator odmawia.
     */
    static void encapsulationCompileError(Path work, Path src) throws IOException {
        section("5. Silna enkapsulacja: import z nieeksportowanego pakietu");

        Path hack = src.resolve("com.example.app/com/example/app/Hack.java");
        write(hack, """
                package com.example.app;

                import com.example.greeter.internal.PolishGreeter;

                class Hack {
                    Object greeter = new PolishGreeter();
                }
                """);
        Result javac = tool("javac", "-encoding", "UTF-8", "-d", work.resolve("mods-bad").toString(),
                "--module-source-path", src.toString(), "-m", "com.example.app,com.example.greeter");
        Files.delete(hack);
        show("javac — kod wyjścia", javac.exitCode());
        // WYNIK: javac — kod wyjścia → 1
        showEach("komunikat (bez ścieżek)", errorLines(javac.output()));
        // WYNIK: komunikat (bez ścieżek) (liczba elementów: 2):
        // WYNIK: • Hack.java:3: error: package com.example.greeter.internal is not visible
        // WYNIK: • (package com.example.greeter.internal is declared in module com.example.greeter, which does not export it)
        // Komunikaty javac są po angielsku. Tłumaczenie: „pakiet ... nie jest widoczny (jest zadeklarowany w module ...,
        // który go nie eksportuje)”. Wycięliśmy ścieżkę katalogu tymczasowego — zależy od komputera.

        // DOBRA PRAKTYKA: eksportuj MAŁO — tylko pakiety API. Wszystko, czego nie wyeksportujesz, możesz zmieniać
        // bez łamania cudzego kodu. Dlaczego: public przestaje znaczyć „dla całego świata”, znaczy „dla mojego modułu
        // i tych, którym eksportuję pakiet”.
    }

    /** Linie z „error:” (bez ścieżki przed nazwą pliku) i linie wyjaśnień w nawiasie. */
    static List<String> errorLines(String output) {
        List<String> result = new ArrayList<>();
        for (String line : output.lines().toList()) {
            if (line.contains(": error: ")) {
                String fileAndRest = line.substring(line.lastIndexOf(line.contains("\\") ? '\\' : '/') + 1);
                result.add(fileAndRest);
            } else if (line.strip().startsWith("(package")) {
                result.add(line.strip());
            }
        }
        return result;
    }

    // =================================================================================================
    // 6. REFLEKSJA WE WNĘTRZU JDK: InaccessibleObjectException
    // =================================================================================================

    /**
     * 6. java.base nie otwiera swoich pakietów. setAccessible(true) na prywatnym polu klasy JDK kończy się
     * InaccessibleObjectException (Java 9+) — w Javie 8 to działało i wiele starych bibliotek z tego korzystało.
     */
    static void reflectionIntoJdk() throws Exception {
        section("6. Refleksja we wnętrzu JDK");

        Field value = String.class.getDeclaredField("value");   // samo POBRANIE pola jest dozwolone
        show("pole istnieje", value.getName());
        // WYNIK: pole istnieje → value
        try {
            value.setAccessible(true);                           // set accessible = ustaw dostępność
            show("setAccessible się udało", true);
        } catch (RuntimeException e) {
            show("rzucono", e.getClass().getSimpleName());
            show("komunikat wspomina o opens java.lang", e.getMessage().contains("does not \"opens java.lang\""));
        }
        // WYNIK: rzucono → InaccessibleObjectException
        // WYNIK: komunikat wspomina o opens java.lang → true
        // Całego komunikatu nie wypisujemy: kończy się „to unnamed module @6f3b5d16” — liczba (skrót tożsamości)
        // zmienia się przy każdym uruchomieniu.

        show("publiczna metoda przez refleksję", String.class.getMethod("length").invoke("moduły"));
        // WYNIK: publiczna metoda przez refleksję → 6
        // Publiczne API z eksportowanych pakietów działa przez refleksję normalnie.

        // Furtka (tylko awaryjnie, np. stara biblioteka): java --add-opens java.base/java.lang=ALL-UNNAMED ...
        // (otwiera pakiet java.lang dla kodu z classpath). Są też --add-exports i --add-reads.
        // PUŁAPKA: dodawanie --add-opens „aż zadziała” — uzależniasz się od wnętrzności JDK, które mogą zniknąć
        // w następnej wersji. Dlaczego: od Javy 16 (JEP 396) i 17 (JEP 403) JDK domyślnie blokuje taki dostęp
        // i ostrzeżenia zamieniły się w wyjątki. Lepiej zaktualizować bibliotekę.
    }

    // =================================================================================================
    // 7. ServiceLoader NA ŚCIEŻCE KLAS (META-INF/services)
    // =================================================================================================

    /** Usługa (interfejs) i dwie implementacje — dostawcy muszą być public i mieć publiczny konstruktor bez argumentów. */
    public interface Greeting {
        String hello();
    }

    public static class PolishGreeting implements Greeting {
        @Override public String hello() { return "Dzień dobry"; }
    }

    public static class EnglishGreeting implements Greeting {
        @Override public String hello() { return "Good morning"; }
    }

    /**
     * 7. ServiceLoader działa też BEZ modułów: dostawców wypisujesz w pliku META-INF/services/PEŁNA.NAZWA.INTERFEJSU
     * (jedna klasa w linii, # = komentarz). Tak JDBC znajduje sterowniki baz danych, a Spring Boot podobnym
     * mechanizmem ładuje autokonfiguracje.
     */
    static void serviceLoaderOnClasspath(Path work) throws IOException {
        section("7. ServiceLoader na ścieżce klas (META-INF/services)");

        Path services = work.resolve("services");
        write(services.resolve("META-INF/services/" + Greeting.class.getName()), """
                # dostawcy usługi Greeting (kolejność = kolejność ładowania)
                t30_build_modules.Build03Modules$PolishGreeting
                t30_build_modules.Build03Modules$EnglishGreeting
                """);
        show("nazwa pliku usługi", Greeting.class.getName());
        // WYNIK: nazwa pliku usługi → t30_build_modules.Build03Modules$Greeting
        // Klasa zagnieżdżona ma nazwę binarną z „$” — i taką trzeba wpisać w pliku usługi.

        // Ładowacz z dodatkowym katalogiem; rodzic = ładowacz tej lekcji (on zna klasy Greeting).
        try (URLClassLoader loader = new URLClassLoader(new URL[]{services.toUri().toURL()},
                Build03Modules.class.getClassLoader())) {
            List<String> greetings = new ArrayList<>();
            for (Greeting greeting : ServiceLoader.load(Greeting.class, loader)) {   // load = załaduj dostawców
                greetings.add(greeting.getClass().getSimpleName() + ": " + greeting.hello());
            }
            showEach("znalezieni dostawcy", greetings);
            // WYNIK: znalezieni dostawcy (liczba elementów: 2):
            // WYNIK: • PolishGreeting: Dzień dobry
            // WYNIK: • EnglishGreeting: Good morning

            // stream() (Java 9+) daje Provider — można sprawdzić typ BEZ tworzenia obiektu:
            show("typy dostawców", ServiceLoader.load(Greeting.class, loader).stream()
                    .map(p -> p.type().getSimpleName()).toList());
            // WYNIK: typy dostawców → [PolishGreeting, EnglishGreeting]
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // W module zamiast pliku piszesz provides ... with ... (sekcja 3). Moduł nazwany IGNORUJE swój plik
        // META-INF/services — dla niego liczy się tylko provides.
        // PUŁAPKA: literówka w nazwie klasy w pliku usługi → ServiceConfigurationError dopiero przy iterowaniu.
        // Dlaczego: plik to zwykły tekst — kompilator go nie sprawdza (provides w module-info — sprawdza!).
    }

    // =================================================================================================
    // 8. MODUŁY AUTOMATYCZNE, NIENAZWANE I ROZDZIELONE PAKIETY
    // =================================================================================================

    /**
     * 8. Zwykły JAR (bez module-info) położony na ŚCIEŻCE MODUŁÓW staje się modułem automatycznym: nazwę bierze z
     * wpisu Automatic-Module-Name w manifeście albo z nazwy pliku, eksportuje WSZYSTKIE pakiety i czyta wszystkie moduły.
     */
    static void automaticModulesAndSplitPackages(Path work) throws IOException {
        section("8. Moduły automatyczne, nienazwane i rozdzielone pakiety");

        Path classes = work.resolve("plain-classes");
        write(work.resolve("plain-src/dup/Util.java"), "package dup;\npublic class Util { }\n");
        Result javac = tool("javac", "-d", classes.toString(), work.resolve("plain-src/dup/Util.java").toString());
        Path jars = Files.createDirectories(work.resolve("jars"));
        tool("jar", "--create", "--file", jars.resolve("moja-biblioteka-1.2.3.jar").toString(), "-C", classes.toString(), "dup");
        show("javac — kod wyjścia", javac.exitCode());
        // WYNIK: javac — kod wyjścia → 0

        ModuleReference ref = ModuleFinder.of(jars).find("moja.biblioteka").orElseThrow();
        show("nazwa z pliku moja-biblioteka-1.2.3.jar", ref.descriptor().name());
        // WYNIK: nazwa z pliku moja-biblioteka-1.2.3.jar → moja.biblioteka
        show("moduł automatyczny", ref.descriptor().isAutomatic());
        // WYNIK: moduł automatyczny → true
        show("wersja z nazwy pliku", ref.descriptor().rawVersion().orElse("(brak)"));   // raw version = surowa wersja
        // WYNIK: wersja z nazwy pliku → 1.2.3
        // Reguła nazwy: utnij ".jar" i wersję (od pierwszego "-cyfra"), znaki inne niż litery/cyfry → ".".
        // PUŁAPKA: nazwa z pliku jest krucha — zmiana nazwy JAR-a zmienia nazwę modułu i psuje requires u innych.
        // Dlatego porządne biblioteki dodają do manifestu Automatic-Module-Name: com.firma.biblioteka.

        // Rozdzielony pakiet (split package): ten sam pakiet w dwóch modułach. Na classpath „działa” (wygrywa pierwszy,
        // Build02), na ścieżce modułów jest ZABRONIONY.
        Files.copy(jars.resolve("moja-biblioteka-1.2.3.jar"), jars.resolve("inna-biblioteka-2.0.jar"));
        try {
            Configuration.resolve(ModuleFinder.of(jars), List.of(ModuleLayer.boot().configuration()), ModuleFinder.of(),
                    Set.of("moja.biblioteka", "inna.biblioteka"));
            show("rozwiązanie grafu", "OK");
        } catch (RuntimeException e) {
            show("rozwiązanie grafu", e.getClass().getSimpleName());
            show("komunikat dotyczy pakietu dup", e.getMessage().contains("package dup"));
        }
        // WYNIK: rozwiązanie grafu → ResolutionException
        // WYNIK: komunikat dotyczy pakietu dup → true
        // (który moduł komunikat wymieni pierwszy, zależy od kolejności sprawdzania — dlatego nie wypisujemy całości)

        // Trzy rodzaje modułów:
        //   • nazwany (explicit) — ma module-info; czyta tylko to, co requires; eksportuje tylko exports,
        //   • automatyczny — zwykły JAR na ścieżce modułów; eksportuje wszystko, czyta wszystko (most dla starych bibliotek),
        //   • nienazwany (unnamed) — WSZYSTKO z classpath razem; czyta wszystkie moduły, ale moduł nazwany NIE MOŻE
        //     go wymagać (nie ma nazwy do wpisania w requires).
    }

    // =================================================================================================
    // 9. jdeps, jlink I DLACZEGO WIĘKSZOŚĆ APLIKACJI ZOSTAJE NA CLASSPATH
    // =================================================================================================

    /**
     * 9. jdeps (analizator zależności) mówi, których modułów JDK używa Twój kod — przydaje się przy przejściu na
     * moduły i przy jlink, który składa MINIMALNE środowisko uruchomieniowe tylko z potrzebnych modułów.
     */
    static void jdepsAndJlink(Path work) throws IOException {
        section("9. jdeps, jlink i classpath w praktyce");

        write(work.resolve("deps-src/report/Report.java"), """
                package report;

                import java.sql.Connection;
                import java.util.logging.Logger;

                public class Report {
                    private static final Logger LOG = Logger.getLogger("report");
                    private Connection connection;
                }
                """);
        Path out = work.resolve("deps-out");
        tool("javac", "-d", out.toString(), work.resolve("deps-src/report/Report.java").toString());
        Result jdeps = tool("jdeps", "--print-module-deps", out.toString());
        show("jdeps — kod wyjścia", jdeps.exitCode());
        // WYNIK: jdeps — kod wyjścia → 0
        show("potrzebne moduły", List.of(jdeps.output().strip().split(",")));
        // WYNIK: potrzebne moduły → [java.base, java.sql]
        // Kod używa Loggera, a java.logging nie ma na liście — bo java.sql wymaga go PRZECHODNIO (sekcja 1),
        // więc --print-module-deps podaje najkrótszą wystarczającą listę.

        Set<String> summary = new HashSet<>();
        for (String line : tool("jdeps", "-summary", out.toString()).lines()) {   // linie: "deps-out -> java.sql"
            summary.add(line.substring(line.indexOf("->") + 2).strip());
        }
        show("jdeps -summary (posortowane)", summary.stream().sorted().toList());
        // WYNIK: jdeps -summary (posortowane) → [java.base, java.logging, java.sql]
        // -summary pokazuje WSZYSTKIE bezpośrednio użyte moduły (także java.logging). Lewą stronę linii (nazwę katalogu)
        // odcinamy — zależy od ścieżki.

        // jlink (Java 9+) — własny, mały „JRE” z wybranymi modułami:
        //   jlink --module-path mods --add-modules com.example.app --output obraz
        //         --launcher app=com.example.app/com.example.app.Main
        //   obraz/bin/app    (Windows: obraz\bin\app.bat)  — działa bez zainstalowanej Javy.
        // jlink wymaga modułów NAZWANYCH (automatycznych nie przyjmie). Dla aplikacji na classpath:
        //   jlink --add-modules java.base,java.sql --output obraz   (listę modułów da jdeps --print-module-deps).

        // Dlaczego większość aplikacji (także Spring Boot) wciąż działa na classpath:
        //   • wiele bibliotek nie ma module-info albo jest „automatycznych”,
        //   • frameworki intensywnie używają refleksji — w modułach trzeba by otwierać (opens) wiele pakietów,
        //   • gruby JAR Spring Boot ładuje zależności własnym ładowaczem z BOOT-INF/lib (Build02).
        // Moduły i tak pomagają: JDK jest modułowy (stąd InaccessibleObjectException), a modularne biblioteki
        // dają jasne granice API. DOBRA PRAKTYKA: w bibliotece dodaj przynajmniej Automatic-Module-Name.
        note("moduły = jawne zależności + ukryte wnętrze; classpath = wszystko widzi wszystko");
        // WYNIK: ℹ moduły = jawne zależności + ukryte wnętrze; classpath = wszystko widzi wszystko
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • module-info.java w korzeniu źródeł modułu: module nazwa { requires ...; exports ...; }
     *   • requires (czytam), requires transitive (przekazuję dalej), requires static (tylko kompilacja)
     *   • exports P (API), exports P to M (tylko dla M), opens P / open module (refleksja do pól prywatnych)
     *   • uses S + provides S with I → ServiceLoader.load(S.class); na classpath: META-INF/services/PEŁNA.NAZWA.S
     *   • javac --module-source-path src -d mods -m a,b   /   java --module-path mods -m moduł/pakiet.Klasa
     *   • dostęp = czytanie modułu + eksport pakietu + public; brak eksportu → błąd kompilacji „is not visible”
     *   • refleksja do JDK: InaccessibleObjectException; awaryjnie --add-opens java.base/java.lang=ALL-UNNAMED
     *   • moduł automatyczny: zwykły JAR na ścieżce modułów (nazwa z Automatic-Module-Name albo z pliku)
     *   • moduł nienazwany: cały classpath; split package na ścieżce modułów = błąd rozwiązania
     *   • jdeps --print-module-deps → lista modułów dla jlink; jlink → mały obraz środowiska z wybranymi modułami
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się exports od opens? Kiedy frameworkowi nie wystarczy exports?
     *   2. Co wypisze:  System.out.println(String.class.getModule().getName());  ?
     *   3. ZNAJDŹ BŁĄD:  module com.shop { exports com.shop; }  — a inne moduły nie widzą klasy com.shop.api.Order.
     *   4. Moduł A: requires B. Moduł B: requires transitive C. Czy A widzi eksportowane typy C? A gdyby w B było
     *      zwykłe requires C?
     *   5. Co wypisze:  ModuleFinder.of(dir).find("guava").isPresent()  dla dir z plikiem guava-32.1.2-jre.jar?
     *   6. ZNAJDŹ BŁĄD:  w pliku META-INF/services/pl.kurs.Plugin wpisano "pl.kurs.impl.MyPlugin.java".
     *   7. Dlaczego Spring Boot zwykle działa na classpath, a nie na ścieżce modułów?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** Uproszczony opis modułu do ćwiczenia 4. */
    record ModuleSpec(List<String> requires, List<String> requiresTransitive, List<String> exports, List<String> packages) {
    }

    static Map<String, ModuleSpec> exerciseModules() {
        Map<String, ModuleSpec> modules = new LinkedHashMap<>();
        modules.put("app", new ModuleSpec(List.of("web"), List.of(), List.of(), List.of("app.main")));
        modules.put("web", new ModuleSpec(List.of("db"), List.of("json"), List.of("web.api"), List.of("web.api", "web.impl")));
        modules.put("json", new ModuleSpec(List.of(), List.of(), List.of("json.api"), List.of("json.api", "json.impl")));
        modules.put("db", new ModuleSpec(List.of(), List.of(), List.of("db.api"), List.of("db.api")));
        return modules;
    }

    static final String MODULE_INFO = """
            module pl.kurs.sklep {
                requires transitive pl.kurs.model;
                // requires pl.kurs.stare;
                requires java.sql;
                requires static lombok;
                exports pl.kurs.sklep.api;
            }
            """;

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: poprawne nazwy modułów", List.of(true, false, false, false),
                () -> List.of("com.example.app", "moja-biblioteka", "com.example.1app", "pl.kurs.int").stream()
                        .map(Build03Modules::exercise1).toList());
        Check.equal("ćw. 2: nazwa modułu automatycznego", List.of("commons.lang3", "guava", "moja.biblioteka"),
                () -> List.of("commons-lang3-3.12.0.jar", "guava-32.1.2-jre.jar", "moja_biblioteka.jar").stream()
                        .map(Build03Modules::exercise2).toList());
        Check.equal("ćw. 3: wymagane moduły", List.of("java.sql", "lombok", "pl.kurs.model"), () -> exercise3(MODULE_INFO));
        Check.equal("ćw. 4: dostęp", List.of(true, true, false, false, true),
                () -> List.of("web.api", "json.api", "json.impl", "db.api", "app.main").stream()
                        .map(p -> exercise4(exerciseModules(), "app", p)).toList());
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of(true, false, false, false),
                () -> List.of("com.example.app", "moja-biblioteka", "com.example.1app", "pl.kurs.int").stream()
                        .map(Build03Modules::solution1).toList());
        Check.equal("ćw. 2 (wzorzec)", List.of("commons.lang3", "guava", "moja.biblioteka"),
                () -> List.of("commons-lang3-3.12.0.jar", "guava-32.1.2-jre.jar", "moja_biblioteka.jar").stream()
                        .map(Build03Modules::solution2).toList());
        Check.equal("ćw. 3 (wzorzec)", List.of("java.sql", "lombok", "pl.kurs.model"), () -> solution3(MODULE_INFO));
        Check.equal("ćw. 4 (wzorzec)", List.of(true, true, false, false, true),
                () -> List.of("web.api", "json.api", "json.impl", "db.api", "app.main").stream()
                        .map(p -> solution4(exerciseModules(), "app", p)).toList());
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 4 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): czy tekst jest poprawną nazwą modułu? Nazwa = identyfikatory Javy rozdzielone kropkami,
     * bez słów kluczowych (np. "int"), bez myślników, części nie zaczynają się od cyfry.
     * Podpowiedź: javax.lang.model.SourceVersion.isName(...) robi dokładnie to sprawdzenie.
     */
    static boolean exercise1(String name) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): wylicz nazwę modułu automatycznego z nazwy pliku JAR (bez Automatic-Module-Name):
     * 1) utnij ".jar"; 2) utnij wersję: od pierwszego dopasowania wyrażenia "-(\\d+(\\.|$))" do końca;
     * 3) każdy ciąg znaków innych niż litery i cyfry zamień na jedną "."; 4) usuń kropki z początku i końca.
     * Podpowiedź: Pattern/Matcher.find(), replaceAll("[^A-Za-z0-9]+", ".").
     */
    static String exercise2(String jarFileName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, łączy z t16_streams): z tekstu module-info zwróć posortowane nazwy wymaganych modułów,
     * pomijając modyfikatory transitive/static i linie zakomentowane (zaczynające się od //).
     * Podpowiedź: lines(), map(String::strip), filter(startsWith("requires ")), usuń ";" i modyfikatory.
     */
    static List<String> exercise3(String moduleInfo) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): czy kod z modułu from może użyć pakietu pkg? Tak, gdy pakiet należy do from ALBO
     * (moduł-właściciel eksportuje pkg I from czyta właściciela). from czyta: moduły z requires i requiresTransitive,
     * a dodatkowo — rekurencyjnie — moduły, które czytane moduły wymagają przechodnio (requiresTransitive).
     * Podpowiedź: zbiór „czytanych” powiększaj w pętli z kolejką; na końcu znajdź właściciela pakietu.
     */
    static boolean exercise4(Map<String, ModuleSpec> modules, String from, String pkg) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static boolean solution1(String name) {
        return SourceVersion.isName(name);
    }

    static String solution2(String jarFileName) {
        String name = jarFileName.substring(0, jarFileName.length() - ".jar".length());
        java.util.regex.Matcher version = java.util.regex.Pattern.compile("-(\\d+(\\.|$))").matcher(name);
        if (version.find()) {
            name = name.substring(0, version.start());
        }
        return name.replaceAll("[^A-Za-z0-9]+", ".").replaceAll("^\\.|\\.$", "");
    }

    static List<String> solution3(String moduleInfo) {
        return moduleInfo.lines()
                .map(String::strip)
                .filter(l -> l.startsWith("requires "))
                .map(l -> l.replace(";", "").replace("requires ", "").replace("transitive ", "").replace("static ", "").strip())
                .sorted()
                .toList();
    }

    static boolean solution4(Map<String, ModuleSpec> modules, String from, String pkg) {
        if (modules.get(from).packages().contains(pkg)) {
            return true;
        }
        Set<String> reads = new HashSet<>();
        List<String> queue = new ArrayList<>(modules.get(from).requires());
        queue.addAll(modules.get(from).requiresTransitive());
        while (!queue.isEmpty()) {
            String module = queue.remove(0);
            if (reads.add(module)) {
                queue.addAll(modules.get(module).requiresTransitive());   // przechodnie przechodzą dalej
            }
        }
        return modules.entrySet().stream()
                .filter(e -> e.getValue().packages().contains(pkg))
                .anyMatch(e -> reads.contains(e.getKey()) && e.getValue().exports().contains(pkg));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. exports daje dostęp do publicznych typów pakietu (kompilacja i runtime). opens daje dostęp refleksyjny
     *      w runtime, także do prywatnych pól i metod (setAccessible). Frameworki typu Hibernate/Jackson/Spring,
     *      które czytają pola prywatne, potrzebują opens (albo open module).
     *   2. java.base
     *   3. exports dotyczy jednego pakietu — com.shop.api to inny pakiet. Trzeba dodać exports com.shop.api;.
     *   4. Tak — requires transitive C sprawia, że każdy, kto czyta B, czyta też C. Przy zwykłym requires C moduł A
     *      musiałby sam dopisać requires C.
     *   5. true — wersja "-32.1.2-jre" zostaje odcięta (od pierwszego "-cyfra"), zostaje nazwa "guava".
     *   6. W pliku usługi wpisuje się nazwę binarną KLASY (pl.kurs.impl.MyPlugin), bez ".java". Inaczej
     *      ServiceConfigurationError przy iterowaniu ServiceLoadera.
     *   7. Wiele bibliotek nie jest modułami, frameworki korzystają z refleksji (w modułach wymagałoby to wielu opens),
     *      a gruby JAR Spring Boot ma własny ładowacz zależności z BOOT-INF/lib. Classpath jest po prostu prostszy.
     */
    // </editor-fold>
}
