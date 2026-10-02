package t19_annotations_reflection;

import helpers.Check;
import helpers.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.annotation.Retention;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Procesory adnotacji — kod, który czyta adnotacje PODCZAS KOMPILACJI i generuje nowe pliki
 *        (annotation processor = procesor adnotacji; compile time = czas kompilacji; generate = generuj)
 *
 * W SKRÓCIE:
 *   Refleksja czyta adnotacje w czasie DZIAŁANIA programu. Procesor adnotacji robi to wcześniej: javac uruchamia go
 *   w trakcie kompilacji i przekazuje mu opis kodu źródłowego (elementy: klasy, pola, metody). Procesor może zgłosić
 *   błąd kompilacji (Messager) albo wygenerować NOWY plik źródłowy (Filer), który javac od razu skompiluje.
 *   Tak działają MapStruct, AutoValue, Dagger i generator metamodelu JPA. Lombok idzie dalej — i to jest ryzykowne.
 *
 * ANALOGIA: korektor w drukarni.
 *   Refleksja to czytelnik, który czyta gotową książkę. Procesor to korektor, który dostaje rękopis PRZED drukiem:
 *   może odesłać go z uwagami (błąd kompilacji) albo dołożyć gotowy aneks (wygenerowany plik). Nie wolno mu jednak
 *   przepisywać cudzych rozdziałów (Filer tworzy tylko nowe pliki).
 *
 * JAK TO DZIAŁA:
 *   {@code @SupportedAnnotationTypes("demo.ToStringGen")}   ← które adnotacje mnie interesują
 *   class ToStringProcessor extends AbstractProcessor
 *       getSupportedSourceVersion() → SourceVersion.latestSupported()
 *       process(adnotacje, runda):
 *           dla elementów z adnotacją: sprawdź → Messager (ERROR/WARNING/NOTE) albo Filer (nowy plik .java)
 *   javac: runda 1 (Twój kod) → nowe pliki? → runda 2 (wygenerowany kod) → ... → brak nowych plików → koniec
 *
 * SŁÓWKA:
 *   processor = procesor; round = runda; element = element (fragment kodu: klasa, pole, metoda); messager = posłaniec
 *   (zgłasza komunikaty); filer = „pisarz plików”; diagnostic = diagnostyka (komunikat kompilatora); kind = rodzaj;
 *   source version = wersja języka; supported = obsługiwany; claim = przejąć (adnotację); syntax tree = drzewo
 *   składni; service = usługa; class loader = ładowacz klas.
 *
 * ZOBACZ TEŻ: t19_annotations_reflection/Annotations02Custom (retencja SOURCE), t20_lombok/Lombok01Accessors,
 *             t19_annotations_reflection/Annotations03ReflectionBasics (refleksja), t18_io_files/Io01PathFiles.
 * </pre>
 */
public class Annotations07Processors {

    // ---------------------------------------------------------------------------------------------
    // Kod źródłowy, który skompilujemy W TRAKCIE działania lekcji (bloki tekstu — Java 15+)
    // ---------------------------------------------------------------------------------------------

    /** ToStringGen = „wygeneruj opis”. Retencja SOURCE: procesor czyta ją z kodu źródłowego, potem jest zbędna. */
    static final String ANNOTATION_SOURCE = """
            package demo;

            import java.lang.annotation.*;

            @Retention(RetentionPolicy.SOURCE)
            @Target(ElementType.TYPE)
            public @interface ToStringGen {
            }
            """;

    static final String PERSON_SOURCE = """
            package demo;

            @ToStringGen
            public class Person {
                String name;
                int age;

                public Person(String name, int age) {
                    this.name = name;
                    this.age = age;
                }
            }
            """;

    static final String BROKEN_SOURCE = """
            package demo;

            @ToStringGen
            public interface Shape {
            }

            @ToStringGen
            class Secret {
                private String password;
            }
            """;

    /** ToStringProcessor = procesor, który dla klasy z @ToStringGen generuje klasę XToString z metodą describe. */
    @SupportedAnnotationTypes("demo.ToStringGen")
    static final class ToStringProcessor extends AbstractProcessor {

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latestSupported();      // „obsługuję każdą wersję, którą zna ten javac”
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            Messager messager = processingEnv.getMessager();
            for (TypeElement annotation : annotations) {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    if (element.getKind() != ElementKind.CLASS) {
                        messager.printMessage(Diagnostic.Kind.ERROR, "@ToStringGen można dać tylko na klasę, a "
                                + element.getSimpleName() + " to " + element.getKind(), element);
                        continue;
                    }
                    TypeElement type = (TypeElement) element;
                    List<VariableElement> fields = ElementFilter.fieldsIn(type.getEnclosedElements()).stream()
                            .filter(f -> !f.getModifiers().contains(Modifier.STATIC))
                            .toList();
                    boolean ok = true;
                    for (VariableElement field : fields) {
                        if (field.getModifiers().contains(Modifier.PRIVATE)) {
                            messager.printMessage(Diagnostic.Kind.ERROR, "pole " + field.getSimpleName()
                                    + " jest prywatne — wygenerowany kod go nie odczyta", field);
                            ok = false;
                        }
                    }
                    if (ok) {
                        generate(type, fields.stream().map(f -> f.getSimpleName().toString()).toList());
                    }
                }
            }
            return true;                                 // true = „przejmuję te adnotacje” (inne procesory ich nie dostaną)
        }

        private void generate(TypeElement type, List<String> fieldNames) {
            String pkg = processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString();
            String simpleName = type.getSimpleName().toString();
            try {
                JavaFileObject file = processingEnv.getFiler().createSourceFile(pkg + "." + simpleName + "ToString", type);
                try (Writer writer = file.openWriter()) {
                    writer.write(buildSource(pkg, simpleName, fieldNames));
                }
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                        "wygenerowano " + pkg + "." + simpleName + "ToString");
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "zapis nieudany: " + e.getMessage(), type);
            }
        }
    }

    /** LazyProcessor = „leniwy” procesor: NIE nadpisuje getSupportedSourceVersion (do sekcji 6). */
    @SupportedAnnotationTypes("demo.ToStringGen")
    static final class LazyProcessor extends AbstractProcessor {
        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            return false;
        }
    }

    /** buildSource = zbuduj tekst klasy XToString. Czysta funkcja na napisach — łatwo ją przetestować. */
    static String buildSource(String pkg, String simpleName, List<String> fieldNames) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String name = fieldNames.get(i);
            body.append(i == 0 ? "" : " + \", \"").append(" + \"").append(name).append("=\" + p.").append(name);
        }
        return "package " + pkg + ";\n"
                + "// Wygenerowano automatycznie przez ToStringProcessor — nie edytuj ręcznie\n"
                + "public final class " + simpleName + "ToString {\n"
                + "    private " + simpleName + "ToString() {\n"
                + "    }\n"
                + "    public static String describe(" + simpleName + " p) {\n"
                + "        return \"" + simpleName + "[\"" + body + " + \"]\";\n"
                + "    }\n"
                + "}\n";
    }

    // ---------------------------------------------------------------------------------------------
    // Kompilacja z poziomu programu (javax.tools)
    // ---------------------------------------------------------------------------------------------

    /** Diag = jedna diagnostyka: rodzaj, numer linii (−1 = brak) i komunikat. */
    record Diag(Diagnostic.Kind kind, long line, String message) {
        @Override
        public String toString() {
            return kind + (line == Diagnostic.NOPOS ? "" : " (linia " + line + ")") + ": " + message;
        }
    }

    /** CompileResult = wynik kompilacji: success = czy się udała; diagnostics = posortowane komunikaty. */
    record CompileResult(boolean success, List<Diag> diagnostics) {
    }

    /** writeSource = zapisz plik źródłowy (UTF-8) pod root/relative, tworząc katalogi pakietu. */
    static void writeSource(Path root, String relative, String source) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, source, StandardCharsets.UTF_8);       // writeString (Java 11+)
    }

    /** compile = skompiluj pliki spod root z podanymi procesorami. Klasy → root/classes, wygenerowane → root/generated. */
    static CompileResult compile(Path root, List<? extends Processor> processors, String... files) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();     // null, gdy działamy na samym JRE (bez javac)
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(collector, Locale.ROOT,
                StandardCharsets.UTF_8)) {
            fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT,
                    List.of(Files.createDirectories(root.resolve("classes"))));
            fileManager.setLocationFromPaths(StandardLocation.SOURCE_OUTPUT,
                    List.of(Files.createDirectories(root.resolve("generated"))));
            List<Path> paths = Arrays.stream(files).map(root::resolve).toList();
            List<String> options = processors.isEmpty() ? List.of("-proc:none") : List.of();
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, collector, options, null,
                    fileManager.getJavaFileObjectsFromPaths(paths));
            task.setProcessors(processors);                                // podajemy GOTOWE obiekty procesorów
            boolean success = task.call();
            List<Diag> diagnostics = collector.getDiagnostics().stream()
                    .map(d -> new Diag(d.getKind(), d.getLineNumber(), d.getMessage(Locale.ROOT)))
                    .sorted((a, b) -> a.toString().compareTo(b.toString()))
                    .toList();
            return new CompileResult(success, diagnostics);
        }
    }

    public static void main(String[] args) throws Exception {
        title("Annotations07 — procesory adnotacji");

        whenAnnotationsAreRead();   // when annotations are read = kiedy czytane są adnotacje
        processorAnatomy();         // processor anatomy = budowa procesora
        runningTheProcessor();      // running the processor = uruchomienie procesora
        compileErrors();            // compile errors = błędy kompilacji
        usingGeneratedCode();       // using generated code = użycie wygenerowanego kodu
        forgottenSourceVersion();   // forgotten source version = zapomniana wersja źródeł
        registration();             // registration = rejestracja
        lombokVsGenerators();       // Lombok vs generators = Lombok kontra generatory
        exercises();                // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. KIEDY CZYTANE SĄ ADNOTACJE?
    // =================================================================================================

    /** 1. Refleksja (działanie) kontra procesor (kompilacja). Kompilator jest dostępny także z poziomu programu. */
    static void whenAnnotationsAreRead() {
        section("1. Refleksja czy procesor — kiedy czytamy adnotacje?");

        show("ToolProvider.getSystemJavaCompiler() != null", ToolProvider.getSystemJavaCompiler() != null);
        show("SourceVersion.latestSupported()", SourceVersion.latestSupported());
        // WYNIK: ToolProvider.getSystemJavaCompiler() != null → true    ← działamy na JDK (z javac)
        // WYNIK: SourceVersion.latestSupported() → RELEASE_17    ← na nowszym JDK zobaczysz wyższą wersję

        //                     REFLEKSJA (Annotations03–06)          PROCESOR ADNOTACJI (ta lekcja)
        // kiedy?              w czasie działania programu           w czasie kompilacji (javac)
        // retencja            RUNTIME                               wystarczy SOURCE
        // co widzi?           Class, Field, Method                  TypeElement, VariableElement, ExecutableElement
        // błąd konfiguracji   wyjątek dopiero po uruchomieniu       błąd kompilacji — od razu, w IDE
        // koszt               refleksja przy każdym starcie         zero kosztu w działaniu (kod już wygenerowany)
        // przykłady           Spring, JUnit, Jackson, Hibernate     MapStruct, AutoValue, Dagger, Lombok

        // DOBRA PRAKTYKA: sprawdzaj przy kompilacji, co się da — błąd w IDE jest tańszy niż błąd na produkcji (@Min na String z Annotations04).
    }

    // =================================================================================================
    // 2. BUDOWA PROCESORA
    // =================================================================================================

    /** 2. AbstractProcessor czyta @SupportedAnnotationTypes REFLEKSJĄ — dlatego ta adnotacja ma retencję RUNTIME. */
    static void processorAnatomy() {
        section("2. Budowa procesora: AbstractProcessor, process(), Messager, Filer");

        ToStringProcessor processor = new ToStringProcessor();
        show("getSupportedAnnotationTypes()", processor.getSupportedAnnotationTypes());
        show("getSupportedSourceVersion()", processor.getSupportedSourceVersion());
        show("retencja @SupportedAnnotationTypes", SupportedAnnotationTypes.class.getAnnotation(Retention.class).value());
        // WYNIK: getSupportedAnnotationTypes() → [demo.ToStringGen]
        // WYNIK: getSupportedSourceVersion() → RELEASE_17
        // WYNIK: retencja @SupportedAnnotationTypes → RUNTIME

        // Procesor widzi KOD ŹRÓDŁOWY, a nie klasy (tych jeszcze nie ma!). Odpowiedniki z refleksji:
        //   Class ↔ TypeElement, Field ↔ VariableElement, Method ↔ ExecutableElement,
        //   getDeclaredFields() ↔ ElementFilter.fieldsIn(type.getEnclosedElements()).
        // processingEnv: getMessager() — ERROR/WARNING/NOTE przypięty do elementu (IDE podkreśli linię);
        //   getFiler() — NOWY plik (.java, .class, zasób); getElementUtils() — pomocnicze operacje (np. pakiet klasy).

        // PUŁAPKA: process() jest wołane w KAŻDEJ rundzie, także dla kodu, który sam wygenerowałeś. Procesor musi
        //   być na to gotowy (tu: wygenerowana klasa nie ma @ToStringGen, więc druga runda nic nie robi).
    }

    // =================================================================================================
    // 3. URUCHOMIENIE PROCESORA
    // =================================================================================================

    /** 3. Zapisujemy dwa pliki do katalogu tymczasowego i kompilujemy je z naszym procesorem. */
    static void runningTheProcessor() throws Exception {
        section("3. Uruchomienie: kompilacja z task.setProcessors(...)");

        Path dir = TempDir.create("refl-apt");
        try {
            writeSource(dir, "demo/ToStringGen.java", ANNOTATION_SOURCE);
            writeSource(dir, "demo/Person.java", PERSON_SOURCE);
            CompileResult result = compile(dir, List.of(new ToStringProcessor()),
                    "demo/ToStringGen.java", "demo/Person.java");
            show("kompilacja udana?", result.success());
            showEach("diagnostyki", result.diagnostics());
            // WYNIK: kompilacja udana? → true
            // WYNIK: diagnostyki (liczba elementów: 1):
            // WYNIK:    • NOTE: wygenerowano demo.PersonToString

            Path generated = dir.resolve("generated/demo/PersonToString.java");
            for (String line : Files.readAllLines(generated, StandardCharsets.UTF_8)) {
                System.out.println("   | " + line);
            }
            // WYNIK:    | package demo;
            // WYNIK:    | // Wygenerowano automatycznie przez ToStringProcessor — nie edytuj ręcznie
            // WYNIK:    | public final class PersonToString {
            // WYNIK:    |     private PersonToString() {
            // WYNIK:    |     }
            // WYNIK:    |     public static String describe(Person p) {
            // WYNIK:    |         return "Person[" + "name=" + p.name + ", " + "age=" + p.age + "]";
            // WYNIK:    |     }
            // WYNIK:    | }
            show("klasa PersonToString skompilowana?", Files.exists(dir.resolve("classes/demo/PersonToString.class")));
            // WYNIK: klasa PersonToString skompilowana? → true    ← javac skompilował ją w drugiej rundzie
        } finally {
            TempDir.deleteRecursively(dir);
        }

        // DOBRA PRAKTYKA: generowany plik zaczynaj komentarzem „wygenerowano — nie edytuj”. Ręczne zmiany i tak
        //   znikną przy następnej kompilacji (Maven trzyma takie pliki w target/generated-sources).
    }

    // =================================================================================================
    // 4. BŁĘDY KOMPILACJI Z PROCESORA (Messager)
    // =================================================================================================

    /** 4. Messager.printMessage(ERROR, ...) = prawdziwy błąd kompilacji, z numerem linii — jak każdy inny. */
    static void compileErrors() throws Exception {
        section("4. Messager: błędy kompilacji zgłaszane przez procesor");

        Path dir = TempDir.create("refl-apt");
        try {
            writeSource(dir, "demo/ToStringGen.java", ANNOTATION_SOURCE);
            writeSource(dir, "demo/Shape.java", BROKEN_SOURCE);
            CompileResult result = compile(dir, List.of(new ToStringProcessor()),
                    "demo/ToStringGen.java", "demo/Shape.java");
            show("kompilacja udana?", result.success());
            showEach("diagnostyki", result.diagnostics());
        } finally {
            TempDir.deleteRecursively(dir);
        }
        // WYNIK: kompilacja udana? → false
        // WYNIK: diagnostyki (liczba elementów: 2):
        // WYNIK:    • ERROR (linia 4): @ToStringGen można dać tylko na klasę, a Shape to INTERFACE
        // WYNIK:    • ERROR (linia 9): pole password jest prywatne — wygenerowany kod go nie odczyta

        // PUŁAPKA: printMessage(Kind.ERROR, ...) BEZ elementu też zatrzyma kompilację, ale IDE nie wskaże linii.
        //   Zawsze podawaj element (trzeci argument) — użytkownik od razu zobaczy, gdzie poprawić kod.
    }

    // =================================================================================================
    // 5. UŻYCIE WYGENEROWANEGO KODU
    // =================================================================================================

    /** 5. Ładujemy świeżo skompilowane klasy i wołamy describe — refleksją, bo w czasie kompilacji lekcji ich nie było. */
    static void usingGeneratedCode() throws Exception {
        section("5. Użycie wygenerowanej klasy");

        Path dir = TempDir.create("refl-apt");
        try {
            writeSource(dir, "demo/ToStringGen.java", ANNOTATION_SOURCE);
            writeSource(dir, "demo/Person.java", PERSON_SOURCE);
            compile(dir, List.of(new ToStringProcessor()), "demo/ToStringGen.java", "demo/Person.java");
            URL[] urls = {dir.resolve("classes").toUri().toURL()};
            try (URLClassLoader loader = new URLClassLoader(urls, Annotations07Processors.class.getClassLoader())) {
                Class<?> personClass = loader.loadClass("demo.Person");           // loadClass = załaduj klasę
                Object person = personClass.getConstructor(String.class, int.class).newInstance("Ala", 30);
                Method describe = loader.loadClass("demo.PersonToString").getMethod("describe", personClass);
                show("PersonToString.describe(new Person(\"Ala\", 30))", describe.invoke(null, person));
            }
        } finally {
            TempDir.deleteRecursively(dir);
        }
        // WYNIK: PersonToString.describe(new Person("Ala", 30)) → Person[name=Ala, age=30]

        // W projekcie NIE ma tu refleksji: wołasz PersonToString.describe(person) wprost, z pełną kontrolą kompilatora.
    }

    // =================================================================================================
    // 6. PUŁAPKA: ZAPOMNIANE getSupportedSourceVersion
    // =================================================================================================

    /** 6. Bez nadpisania AbstractProcessor deklaruje RELEASE_6 i javac ostrzega przy każdej kompilacji. */
    static void forgottenSourceVersion() throws Exception {
        section("6. Pułapka: brak getSupportedSourceVersion()");

        show("LazyProcessor: wersja", new LazyProcessor().getSupportedSourceVersion());
        // WYNIK: LazyProcessor: wersja → RELEASE_6
        Path dir = TempDir.create("refl-apt");
        try {
            writeSource(dir, "demo/ToStringGen.java", ANNOTATION_SOURCE);
            writeSource(dir, "demo/Person.java", PERSON_SOURCE);
            CompileResult result = compile(dir, List.of(new LazyProcessor()), "demo/ToStringGen.java", "demo/Person.java");
            showEach("diagnostyki", result.diagnostics());
        } finally {
            TempDir.deleteRecursively(dir);
        }
        // WYNIK: diagnostyki (liczba elementów: 2):
        // WYNIK:    • WARNING: No SupportedSourceVersion annotation found on t19_annotations_reflection.Annotations07Processors$LazyProcessor, returning RELEASE_6.
        // WYNIK:    • WARNING: Supported source version 'RELEASE_6' from annotation processor 't19_annotations_reflection.Annotations07Processors$LazyProcessor' less than -source '17'
        // Pierwsze ostrzeżenie zgłasza sam AbstractProcessor (przez Messager), drugie — javac.

        // PUŁAPKA: bez @SupportedSourceVersion i bez nadpisania metody AbstractProcessor deklaruje RELEASE_6, więc
        //   KAŻDY użytkownik procesora dostaje dwa ostrzeżenia. Zwracaj SourceVersion.latestSupported().
    }

    // =================================================================================================
    // 7. REJESTRACJA PROCESORA
    // =================================================================================================

    /** 7. W demo podajemy obiekt procesora wprost. Normalnie javac sam go znajduje albo wskazujesz go w opcjach. */
    static void registration() {
        section("7. Rejestracja procesora (META-INF/services, -processor, Maven, Gradle)");

        show("plik rejestracji w JAR", "META-INF/services/" + Processor.class.getName());
        show("jego treść (nazwa klasy)", ToStringProcessor.class.getName());
        // WYNIK: plik rejestracji w JAR → META-INF/services/javax.annotation.processing.Processor
        // WYNIK: jego treść (nazwa klasy) → t19_annotations_reflection.Annotations07Processors$ToStringProcessor

        // Sposoby: JAR z plikiem META-INF/services/... (jedna linia = pełna nazwa klasy; javac szuka przez
        //   ServiceLoader); opcje javac -processor i --processor-path; Maven: annotationProcessorPaths w
        //   maven-compiler-plugin; Gradle: konfiguracja annotationProcessor; z programu: task.setProcessors(...).
        // PUŁAPKA: javac tworzy procesor sam (ServiceLoader), więc procesor musi mieć publiczny konstruktor bez
        //   argumentów — w praktyce to publiczna klasa. Nasz niepubliczny ToStringProcessor działa tylko dlatego,
        //   że podajemy gotowy obiekt przez setProcessors.
        // (Java 23+) javac domyślnie nie uruchamia procesorów znalezionych tylko na ścieżce klas — trzeba je wskazać
        //   jawnie (--processor-path / annotationProcessorPaths albo opcja -proc:full).
    }

    // =================================================================================================
    // 8. LOMBOK KONTRA GENERATORY NOWYCH PLIKÓW
    // =================================================================================================

    /** 8. Publiczne API (Filer) pozwala tylko DOKŁADAĆ pliki. Lombok zmienia istniejące klasy — przez wnętrze javac. */
    static void lombokVsGenerators() {
        section("8. Lombok kontra MapStruct/AutoValue/Dagger");

        String row = "   %-10s %-20s %s%n";
        System.out.printf(Locale.ROOT, row, "MapStruct", "nowe pliki (Filer)", "implementacje mapperów obiekt → obiekt");
        System.out.printf(Locale.ROOT, row, "AutoValue", "nowe pliki (Filer)", "klasy wartości z equals/hashCode/toString");
        System.out.printf(Locale.ROOT, row, "Dagger", "nowe pliki (Filer)", "kod wstrzykiwania zależności");
        System.out.printf(Locale.ROOT, row, "Lombok", "zmienia TWOJĄ klasę", "gettery, konstruktory, @Data, @Builder");
        // WYNIK:    MapStruct  nowe pliki (Filer)   implementacje mapperów obiekt → obiekt
        // WYNIK:    AutoValue  nowe pliki (Filer)   klasy wartości z equals/hashCode/toString
        // WYNIK:    Dagger     nowe pliki (Filer)   kod wstrzykiwania zależności
        // WYNIK:    Lombok     zmienia TWOJĄ klasę  gettery, konstruktory, @Data, @Builder

        // Filer pozwala tylko TWORZYĆ pliki (drugie utworzenie tego samego → FilerException); zmienić istniejącej
        // klasy się nie da. Lombok to omija: sięga do wewnętrznych klas javac (com.sun.tools.javac...) i dopisuje
        // metody do DRZEWA SKŁADNI Twojej klasy, zanim javac wygeneruje bajtkod.
        // PUŁAPKA: wewnętrzne API javac nie jest stabilne — nowe JDK często wymagają nowej wersji Lomboka, a IDE wtyczki,
        //   by „widzieć” metody, których nie ma w .java. Wygenerowany plik MapStruct czy AutoValue przeczytasz jak zwykły kod.
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Procesor: extends AbstractProcessor + @SupportedAnnotationTypes("pakiet.Adnotacja").
     *   • ZAWSZE nadpisz getSupportedSourceVersion() → SourceVersion.latestSupported() (inaczej RELEASE_6 + ostrzeżenie).
     *   • process(annotations, roundEnv): roundEnv.getElementsAnnotatedWith(...) → elementy; zwróć true = „moje”.
     *   • Model kodu: TypeElement / VariableElement / ExecutableElement; ElementFilter.fieldsIn/constructorsIn.
     *   • getMessager().printMessage(Kind.ERROR, "...", element) → błąd z linią; getFiler().createSourceFile → nowy plik.
     *   • Z programu: ToolProvider.getSystemJavaCompiler() (null na JRE), getTask, setProcessors, DiagnosticCollector.
     *     Normalnie: META-INF/services/javax.annotation.processing.Processor, -processor, annotationProcessorPaths.
     *   • MapStruct/AutoValue/Dagger generują nowe pliki; Lombok zmienia drzewo składni przez wewnętrzne API javac.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się procesor adnotacji od kodu, który czyta adnotacje refleksją? Jaka retencja wystarczy każdemu?
     *   2. Co wypisze:  System.out.println(new LazyProcessor().getSupportedSourceVersion());  ?
     *   3. ZNAJDŹ BŁĄD:  messager.printMessage(Diagnostic.Kind.ERROR, "pole jest prywatne");  — co zobaczy użytkownik?
     *   4. Dlaczego procesor nie może po prostu dopisać metody toString() do klasy Person?
     *   5. ZNAJDŹ BŁĄD:  ToolProvider.getSystemJavaCompiler().getTask(...)  — program uruchomiony na samym JRE.
     *   6. Jak javac znajduje procesory spakowane w JAR-ze i co musi spełniać klasa procesora?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    /** BOXES_SOURCE = adnotacja i trzy klasy w jednym pliku: Box (brak konstruktora ()), Crate (domyślny), Bag. */
    static final String BOXES_SOURCE = """
            package demo;

            @interface CheckNoArgConstructor { }

            @CheckNoArgConstructor
            class Box { Box(int size) { } }

            @CheckNoArgConstructor
            class Crate { }

            @CheckNoArgConstructor
            class Bag { Bag() { } Bag(String name) { } }
            """;

    static final String GOOD_CALC = "package demo;\npublic class Calc {\n    int twice(int x) { return 2 * x; }\n}\n";
    static final String BAD_CALC = "package demo;\npublic class Calc {\n    int twice(int x) { return \"dwa\" * x; }\n}\n";

    /** noArgScenario = skompiluj Box/Crate/Bag z procesorem i zwróć diagnostyki "RODZAJ: komunikat". */
    static List<String> noArgScenario(Processor processor) {
        Path dir = null;
        try {
            dir = TempDir.create("refl-noarg");
            writeSource(dir, "demo/Boxes.java", BOXES_SOURCE);
            return compile(dir, List.of(processor), "demo/Boxes.java")
                    .diagnostics().stream().map(d -> d.kind() + ": " + d.message()).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (dir != null) {
                TempDir.deleteRecursively(dir);
            }
        }
    }

    static void exercises() {
        String expectedPerson = "return \"Person[name=\" + p.name + \", age=\" + p.age + \"]\";";
        List<String> expectedNoArg = List.of("ERROR: klasa Box musi mieć konstruktor bez argumentów");

        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1a: Person", expectedPerson, () -> exercise1("Person", List.of("name", "age")));
        Check.equal("ćw. 1b: bez pól", "return \"Empty[]\";", () -> exercise1("Empty", List.of()));
        Check.equal("ćw. 2a: poprawny kod", List.of(), () -> exercise2(GOOD_CALC));
        Check.equal("ćw. 2b: błąd w linii 3", List.of("ERROR w linii 3"), () -> exercise2(BAD_CALC));
        Check.equal("ćw. 3: @CheckNoArgConstructor", expectedNoArg, () -> noArgScenario(exercise3()));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1a (wzorzec)", expectedPerson, () -> solution1("Person", List.of("name", "age")));
        Check.equal("ćw. 1b (wzorzec)", "return \"Empty[]\";", () -> solution1("Empty", List.of()));
        Check.equal("ćw. 2a (wzorzec)", List.of(), () -> solution2(GOOD_CALC));
        Check.equal("ćw. 2b (wzorzec)", List.of("ERROR w linii 3"), () -> solution2(BAD_CALC));
        Check.equal("ćw. 3 (wzorzec)", expectedNoArg, () -> noArgScenario(solution3()));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): PRZEPISZ generator tak, by sklejał literały. Dziś buildSource tworzy:
     * <pre>{@code
     * return "Person[" + "name=" + p.name + ", " + "age=" + p.age + "]";
     * }</pre>
     * a ma powstać linia bez zbędnych „+” między napisami:
     * <pre>{@code
     * return "Person[name=" + p.name + ", age=" + p.age + "]";
     * }</pre>
     * Podpowiedź: StringBuilder; przed każdym polem oprócz pierwszego dopisz ", " jeszcze WEWNĄTRZ literału.
     */
    static String exercise1(String simpleName, List<String> fields) {
        // TODO: twoje rozwiązanie
        return null;
    }

    /**
     * ĆWICZENIE 2 (średnie): połącz z t18_io_files. Zapisz source jako demo/Calc.java w katalogu z TempDir.create,
     * skompiluj (compile z pustą listą procesorów) i zwróć posortowaną listę "ERROR w linii N" dla błędów.
     * Podpowiedź: try/finally z TempDir.deleteRecursively; IOException opakuj w UncheckedIOException.
     */
    static List<String> exercise2(String source) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): dokończ procesor NoArgCheckExercise: dla każdej klasy z @CheckNoArgConstructor,
     * która NIE ma konstruktora bez parametrów, zgłoś ERROR "klasa X musi mieć konstruktor bez argumentów"
     * przypięty do tej klasy.
     * Podpowiedź: ElementFilter.constructorsIn(element.getEnclosedElements()), c.getParameters().isEmpty().
     * Klasa bez żadnego konstruktora ma konstruktor domyślny — kompilator dodaje go, zanim uruchomi procesor.
     */
    static Processor exercise3() {
        return new NoArgCheckExercise();
    }

    @SupportedAnnotationTypes("demo.CheckNoArgConstructor")
    static final class NoArgCheckExercise extends AbstractProcessor {
        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latestSupported();
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            // TODO: twoje rozwiązanie
            return false;
        }
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String simpleName, List<String> fields) {
        StringBuilder sb = new StringBuilder("return \"" + simpleName + "[");
        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i);
            sb.append(i == 0 ? "" : ", ").append(field).append("=\" + p.").append(field).append(" + \"");
        }
        return sb.append("]\";").toString();
    }

    static List<String> solution2(String source) {
        Path dir = null;
        try {
            dir = TempDir.create("refl-calc");
            writeSource(dir, "demo/Calc.java", source);
            return compile(dir, List.of(), "demo/Calc.java").diagnostics().stream()
                    .filter(d -> d.kind() == Diagnostic.Kind.ERROR)
                    .map(d -> "ERROR w linii " + d.line())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (dir != null) {
                TempDir.deleteRecursively(dir);
            }
        }
    }

    static Processor solution3() {
        return new NoArgCheckSolution();
    }

    @SupportedAnnotationTypes("demo.CheckNoArgConstructor")
    static final class NoArgCheckSolution extends AbstractProcessor {
        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latestSupported();
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            for (TypeElement annotation : annotations) {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    boolean hasNoArg = ElementFilter.constructorsIn(element.getEnclosedElements()).stream()
                            .anyMatch(c -> c.getParameters().isEmpty());
                    if (!hasNoArg) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                "klasa " + element.getSimpleName() + " musi mieć konstruktor bez argumentów", element);
                    }
                }
            }
            return true;
        }
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Procesor działa podczas KOMPILACJI na modelu kodu źródłowego (elementy), wystarczy mu retencja SOURCE.
     *      Refleksja działa w czasie DZIAŁANIA na klasach i potrzebuje retencji RUNTIME.
     *   2. RELEASE_6 — domyślna wartość AbstractProcessor, gdy nie nadpisano metody ani nie dodano @SupportedSourceVersion.
     *   3. Błąd zatrzyma kompilację, ale bez elementu nie ma pliku ani linii — użytkownik nie wie, które pole poprawić.
     *      Poprawnie: printMessage(Kind.ERROR, "pole " + f.getSimpleName() + " jest prywatne", f).
     *   4. Filer tworzy tylko NOWE pliki; zmiana istniejącej klasy wymaga wewnętrznego API javac (tak robi Lombok).
     *   5. Na samym JRE (bez modułu jdk.compiler) getSystemJavaCompiler() zwraca null → NullPointerException.
     *      Sprawdź null i podaj czytelny komunikat („uruchom na JDK”).
     *   6. Przez ServiceLoader: plik META-INF/services/javax.annotation.processing.Processor w JAR-ze z pełną nazwą
     *      klasy. Klasa procesora musi mieć publiczny konstruktor bez argumentów (w praktyce: publiczna klasa).
     */
    // </editor-fold>
}
