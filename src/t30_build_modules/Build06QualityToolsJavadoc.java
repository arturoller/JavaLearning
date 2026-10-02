package t30_build_modules;

import helpers.Check;
import helpers.TempDir;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Jakość kodu — Javadoc, doclint, ostrzeżenia kompilatora, analiza statyczna, pokrycie, CI
 *        (static analysis = analiza statyczna: sprawdzanie kodu BEZ jego uruchamiania)
 *
 * W SKRÓCIE:
 *   Dokumentację API piszesz w komentarzach Javadoc, a narzędzie javadoc robi z nich strony HTML.
 *   Kompilator (ostrzeżenia -Xlint, doclint) i narzędzia analizy statycznej (Checkstyle, PMD, SpotBugs, Sonar)
 *   znajdują błędy i bałagan, zanim zrobi to klient. Serwer CI uruchamia to wszystko przy każdej zmianie.
 *
 * ANALOGIA:
 *   Wydawnictwo. Autor pisze książkę (kod), korektor poprawia przecinki (Checkstyle, formatowanie),
 *   redaktor merytoryczny łapie bzdury (SpotBugs, Error Prone), a spis treści i indeks robi skład (javadoc).
 *   Drukarnia (CI) nie przyjmie tekstu, który nie przeszedł korekty — to „bramka jakości” (quality gate).
 *
 * JAK TO DZIAŁA:
 *   komentarz „ukośnik + dwie gwiazdki” z opisem i znacznikami (param, return)
 *                          → javadoc -d docs Plik.java → docs/index.html
 *   javac -Xlint:all       → ostrzeżenia (raw types, unchecked, fall-through, deprecation...)
 *   javac -Xdoclint:all    → sprawdza też komentarze Javadoc (brak @param, zły HTML)
 *   javac -Werror          → każde ostrzeżenie staje się błędem (budowanie czerwone)
 *   Maven: wtyczki checkstyle / pmd / spotbugs / jacoco przypięte do fazy verify → ./mvnw verify
 *
 * SŁÓWKA:
 *   documentation comment = komentarz dokumentacyjny; tag = znacznik; warning = ostrzeżenie; lint = „kłaczki”
 *   (drobne usterki w kodzie); quality gate = bramka jakości; coverage = pokrycie; branch = gałąź;
 *   pipeline = potok (ciąg etapów); inspection = inspekcja; bug pattern = wzorzec błędu; code smell = „brzydki zapach” kodu.
 *
 * ZOBACZ TEŻ: t27_clean_code_pitfalls/Pitfalls02CodeReview (przegląd kodu), t32_junit_mockito/JUnit01Basics (testy, które
 *             mierzy JaCoCo), t19_annotations_reflection/Annotations07Processors (javax.tools i diagnostyki),
 *             t30_build_modules/Build01MavenBasics (fazy i wtyczki).
 * </pre>
 */
public class Build06QualityToolsJavadoc {

    public static void main(String[] args) throws Exception {
        title("Build06 — jakość kodu: Javadoc, lint, analiza, pokrycie, CI");

        javadocTags();                         // javadoc tags = znaczniki Javadoc
        Path work = TempDir.create("build06");
        try {
            generateDocs(work);                // generate docs = generowanie dokumentacji
            doclintInJavac(work);              // doclint in javac = doclint w kompilatorze
            lintAsQualityGate(work);           // lint as quality gate = ostrzeżenia jako bramka jakości
        } finally {
            TempDir.deleteRecursively(work);
        }
        staticAnalysisTools();                 // static analysis tools = narzędzia analizy statycznej
        formattingAndInspections();            // formatting and inspections = formatowanie i inspekcje IDE
        codeCoverage();                        // code coverage = pokrycie kodu testami
        ciPipeline();                          // CI pipeline = potok ciągłej integracji
        exercises();                           // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. KOMENTARZE JAVADOC I ICH ZNACZNIKI
    // =================================================================================================

    /** Przykładowa klasa z porządnym Javadoc (oprócz ostatniej metody — celowo bez kompletu znaczników). */
    static final String CALCULATOR = """
            package pl.kurs;

            /**
             * Prosty kalkulator liczb całkowitych.
             * Zobacz też {@link java.lang.Math}.
             *
             * @since 1.0
             */
            public class Calculator {

                /**
                 * Dzieli {@code a} przez {@code b} (część ułamkowa jest odcinana).
                 *
                 * @param a dzielna
                 * @param b dzielnik
                 * @return iloraz całkowity
                 * @throws ArithmeticException gdy {@code b == 0}
                 * @see Math#floorDiv(int, int)
                 */
                public int divide(int a, int b) {
                    return a / b;
                }

                /**
                 * Dodaje dwie liczby.
                 * @param a pierwsza liczba
                 */
                public int add(int a, int b) {
                    return a + b;
                }
            }
            """;

    /**
     * 1. Komentarz Javadoc zaczyna się od DWÓCH gwiazdek i stoi tuż przed klasą, metodą lub polem. Pierwsze zdanie
     * to streszczenie (trafia do tabel podsumowań), potem opis, a na końcu znaczniki blokowe.
     */
    static void javadocTags() {
        section("1. Komentarze Javadoc i znaczniki");

        show("znaczniki blokowe w przykładzie", findAll(CALCULATOR, "(?m)^\\s*\\*\\s*(@\\w+)"));
        // WYNIK: znaczniki blokowe w przykładzie → [@param, @return, @see, @since, @throws]
        show("znaczniki wewnątrzliniowe", findAll(CALCULATOR, "\\{@(\\w+)"));
        // WYNIK: znaczniki wewnątrzliniowe → [code, link]

        // Znaczniki blokowe (każdy na początku własnej linii komentarza):
        //   @param nazwa opis    — parametr (po jednym na każdy parametr, w kolejności),
        //   @return opis         — co zwraca metoda (nie dla void),
        //   @throws Typ opis     — kiedy rzuca wyjątek (szczególnie ważne dla wyjątków niesprawdzanych),
        //   @see Klasa#metoda    — „zobacz też”, @since 1.0 — od której wersji, @deprecated — nie używaj (i czym zastąpić).
        // Znaczniki wewnątrzliniowe (w środku zdania, w klamrach):
        //   {@code a < b}        — kod czcionką o stałej szerokości; znaki < > & NIE są wtedy traktowane jak HTML,
        //   {@link Klasa#metoda} — klikalny odnośnik (javadoc sprawdzi, czy cel istnieje!).
        // PUŁAPKA: "@return wynik" w środku zdania nie jest znacznikiem — znacznik blokowy działa tylko na początku linii.
        // Dlaczego: javadoc traktuje wszystko po pierwszym znaczniku blokowym jako sekcję znaczników, a wcześniej — opis.
        // PUŁAPKA: "a < b" w Javadoc bez {@code} → javadoc widzi początek znacznika HTML i zgłasza błąd.
        // DOBRA PRAKTYKA: dokumentuj publiczne API: CO robi metoda, warunki brzegowe (null? pusta lista?), wyjątki —
        // nie opisuj JAK (to widać w kodzie). Dlaczego: Javadoc czyta ktoś, kto NIE ogląda Twojej implementacji.
        // IntelliJ: wpisz /** i Enter nad metodą → szkielet z @param/@return; Ctrl+Q (Windows) = podgląd dokumentacji.
    }

    /** Unikalne dopasowania pierwszej grupy wyrażenia, posortowane. */
    static List<String> findAll(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        TreeSet<String> found = new TreeSet<>();
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return List.copyOf(found);
    }

    // =================================================================================================
    // 2. GENEROWANIE DOKUMENTACJI: javadoc
    // =================================================================================================

    /** Uruchamia narzędzie JDK w tym procesie (java.util.spi.ToolProvider), wyjście zbiera do tekstu. */
    static String[] runTool(String name, String... args) {
        java.util.spi.ToolProvider tool = java.util.spi.ToolProvider.findFirst(name).orElseThrow();
        StringWriter text = new StringWriter();
        PrintWriter writer = new PrintWriter(text);
        int exit = tool.run(writer, writer, args);
        writer.flush();
        return new String[]{String.valueOf(exit), text.toString().replace("\r\n", "\n")};
    }

    /** Linie ostrzeżeń/błędów bez ścieżki katalogu: „Calculator.java:27: warning: ...”. */
    static List<String> diagnosticsWithoutPaths(String output) {
        return output.lines()
                .filter(l -> l.contains(": warning: ") || l.contains(": error: "))
                .map(l -> l.substring(Math.max(l.lastIndexOf('/'), l.lastIndexOf('\\')) + 1))
                .toList();
    }

    /**
     * 2. javadoc czyta źródła (nie klasy!) i generuje stronę HTML: index.html, strona dla każdej klasy, wyszukiwarka.
     * Przy okazji uruchamia doclint i ostrzega o brakujących znacznikach.
     */
    static void generateDocs(Path work) throws IOException {
        section("2. Generowanie dokumentacji: javadoc");

        Path source = work.resolve("src/pl/kurs/Calculator.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, CALCULATOR);
        Path docs = work.resolve("docs");
        String[] result = runTool("javadoc", "-d", docs.toString(), "-quiet", "-encoding", "UTF-8",
                "-docencoding", "UTF-8", "-Xdoclint:all", source.toString());
        show("javadoc — kod wyjścia", result[0]);
        // WYNIK: javadoc — kod wyjścia → 0
        show("powstał index.html", Files.exists(docs.resolve("index.html")));
        // WYNIK: powstał index.html → true
        show("strona klasy pl/kurs/Calculator.html", Files.exists(docs.resolve("pl/kurs/Calculator.html")));
        // WYNIK: strona klasy pl/kurs/Calculator.html → true
        List<String> warnings = diagnosticsWithoutPaths(result[1]);
        showEach("ostrzeżenia doclint", warnings);
        // WYNIK: ostrzeżenia doclint (liczba elementów: 2):
        // WYNIK: • Calculator.java:28: warning: no @param for b
        // WYNIK: • Calculator.java:28: warning: no @return
        // Ostrzeżenia NIE przerywają generowania (kod 0). Błędy (np. niezamknięty znacznik HTML) — przerywają.

        // Opcje: -d katalog wynikowy, -quiet bez komunikatów postępu, -Xdoclint:all,-missing — sprawdzaj wszystko
        // oprócz brakujących komentarzy, -sourcepath src -subpackages pl.kurs — cała paczka pakietów naraz,
        // -link https://docs.oracle.com/en/java/javase/17/docs/api/ — odnośniki do dokumentacji JDK.
        // Maven: ./mvnw javadoc:javadoc (strona w target/site/apidocs) albo javadoc:jar (JAR z dokumentacją —
        // wymagany przy publikacji w Maven Central). IntelliJ: Tools → Generate JavaDoc…
    }

    // =================================================================================================
    // 3. DOCLINT W KOMPILATORZE + javax.tools
    // =================================================================================================

    /** Źródło z tekstu (bez pliku na dysku) — jak w t19_annotations_reflection/Annotations07Processors. */
    static final class StringSource extends SimpleJavaFileObject {
        private final String code;

        StringSource(String path, String code) {
            super(URI.create("string:///" + path), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    /** Wynik kompilacji: czy się udało + diagnostyki jako „RODZAJ linia N: kod — komunikat”, posortowane po linii. */
    record Compilation(boolean success, List<String> diagnostics) {
    }

    static Compilation compile(Path out, String path, String code, String... options) {
        JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();   // kompilator z JDK
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>(); // zbieracz diagnostyk
        List<String> allOptions = new ArrayList<>(List.of(options));
        allOptions.addAll(List.of("-d", out.toString()));
        boolean success = compiler.getTask(null, null, collector, allOptions, null,
                List.of(new StringSource(path, code))).call();
        List<String> lines = collector.getDiagnostics().stream()
                .sorted(Comparator.comparingLong(Diagnostic<? extends JavaFileObject>::getLineNumber))
                .map(d -> d.getKind() + " linia " + d.getLineNumber() + ": " + d.getCode() + " — "
                        + d.getMessage(Locale.ROOT).lines().findFirst().orElse(""))
                .toList();
        return new Compilation(success, lines);
    }

    /**
     * 3. Ten sam doclint działa w javac: -Xdoclint:all. Diagnostyki zbieramy przez DiagnosticCollector — dostajemy
     * rodzaj, numer linii i komunikat bez parsowania tekstu. Komunikaty javac są po angielsku (getMessage(Locale.ROOT)).
     */
    static void doclintInJavac(Path work) {
        section("3. doclint w javac (-Xdoclint)");

        String badDocs = """
                package demo;

                /** Konto bankowe. */
                public class Account {
                    /** Saldo w groszach. */
                    private long balance;

                    /**
                     * Wpłaca kwotę.
                     * @param amount kwota w groszach, musi być {@code > 0}
                     */
                    public void deposit(long amount) { balance += amount; }

                    public long balance() { return balance; }

                    /** Wypłaca. Rzuca wyjątek, gdy a < saldo. */
                    public void withdraw(long amount) { balance -= amount; }
                }
                """;
        Compilation result = compile(work.resolve("doclint-out"), "demo/Account.java", badDocs, "-Xdoclint:all");
        show("kompilacja udana", result.success());
        // WYNIK: kompilacja udana → false
        showEach("diagnostyki", result.diagnostics());
        // WYNIK: diagnostyki (liczba elementów: 3):
        // WYNIK: • WARNING linia 14: compiler.warn.proc.messager — no comment
        // WYNIK: • ERROR linia 16: compiler.err.proc.messager — malformed HTML
        // WYNIK: • WARNING linia 17: compiler.warn.proc.messager — no @param for amount
        // Tłumaczenie: „brak komentarza” (metoda balance), „źle zbudowany HTML”, „brak @param dla amount”.
        // Znak '<' w zwykłym tekście Javadoc to BŁĄD — dlatego kompilacja się nie udała. Lekarstwo: {@code a < saldo}.
    }

    // =================================================================================================
    // 4. OSTRZEŻENIA -Xlint JAKO BRAMKA JAKOŚCI
    // =================================================================================================

    static final String LINT_SOURCE = """
            package demo;

            import java.util.ArrayList;
            import java.util.List;

            public class Legacy implements java.io.Serializable {
                @SuppressWarnings("rawtypes") List allowed;
                List names = new ArrayList();
                void add() { names.add("x"); }
                int grade(int points) {
                    int result = 0;
                    switch (points) {
                        case 1: result = 1;
                        case 2: result = 2; break;
                        default: result = 3;
                    }
                    return result;
                }
                Integer boxed() { return new Integer(5); }
                Object cast(Object o) { return (String) (String) o; }
            }
            """;

    /**
     * 4. -Xlint:all włącza wszystkie kategorie ostrzeżeń javac. Każde z nich to potencjalny błąd albo przestarzały kod.
     * Weryfikator tego kursu kompiluje lekcje właśnie z --release 17 -g -Xlint:all i traktuje każde ostrzeżenie jako
     * problem — lekcja z ostrzeżeniem nie jest „gotowa”.
     */
    static void lintAsQualityGate(Path work) {
        section("4. Ostrzeżenia -Xlint jako bramka jakości");

        Compilation lint = compile(work.resolve("lint-out"), "demo/Legacy.java", LINT_SOURCE, "-Xlint:all");
        show("kompilacja udana (same ostrzeżenia)", lint.success());
        // WYNIK: kompilacja udana (same ostrzeżenia) → true
        showEach("ostrzeżenia", lint.diagnostics());
        // WYNIK: ostrzeżenia (liczba elementów: 7):
        // WYNIK: • WARNING linia 6: compiler.warn.missing.SVUID — serializable class demo.Legacy has no definition of serialVersionUID
        // WYNIK: • WARNING linia 8: compiler.warn.raw.class.use — found raw type: java.util.List
        // WYNIK: • WARNING linia 8: compiler.warn.raw.class.use — found raw type: java.util.ArrayList
        // WYNIK: • MANDATORY_WARNING linia 9: compiler.warn.unchecked.call.mbr.of.raw.type — unchecked call to add(E) as a member of the raw type java.util.List
        // WYNIK: • WARNING linia 14: compiler.warn.possible.fall-through.into.case — possible fall-through into case
        // WYNIK: • MANDATORY_WARNING linia 19: compiler.warn.has.been.deprecated.for.removal — Integer(int) in java.lang.Integer has been deprecated and marked for removal
        // WYNIK: • WARNING linia 20: compiler.warn.redundant.cast — redundant cast to java.lang.String
        // Co znaczą (kategoria -Xlint w nawiasie):
        //   missing.SVUID (serial) — klasa Serializable bez serialVersionUID (t18_io_files/Io09Serialization),
        //   raw.class.use (rawtypes) — typ surowy List zamiast List<String> (t11_generics/Generics01Why),
        //   unchecked — wywołanie bez kontroli typów na typie surowym. MANDATORY_WARNING = ostrzeżenie obowiązkowe:
        //     javac zgłasza je nawet bez -Xlint (unchecked i deprecation jako zbiorczą notkę „Note: ...”, removal w całości),
        //   fall-through — brak break w switch: case 1 „przelatuje” do case 2 (zamiast tego: switch ze strzałkami),
        //   deprecation / removal — przestarzałe API (new Integer → Integer.valueOf),
        //   cast — zbędne rzutowanie.
        // Linia 7 nie ma ostrzeżenia: @SuppressWarnings("rawtypes") je wycisza.
        // DOBRA PRAKTYKA: @SuppressWarnings tylko na najmniejszym możliwym elemencie i z komentarzem DLACZEGO.
        // Dlaczego: wyciszenie całej klasy ukryje też przyszłe, prawdziwe problemy.

        Compilation strict = compile(work.resolve("werror-out"), "demo/Legacy.java", LINT_SOURCE, "-Xlint:all", "-Werror");
        show("z -Werror kompilacja udana", strict.success());
        // WYNIK: z -Werror kompilacja udana → false
        show("ostatnia diagnostyka", strict.diagnostics().stream()
                .filter(d -> d.startsWith("ERROR")).map(d -> d.substring(d.indexOf("compiler."))).findFirst().orElse("(brak)"));
        // WYNIK: ostatnia diagnostyka → compiler.err.warnings.and.werror — warnings found and -Werror specified
        // Maven: <compilerArgs><arg>-Xlint:all</arg><arg>-Werror</arg></compilerArgs> w maven-compiler-plugin
        // (albo <failOnWarning>true</failOnWarning>). Gradle: options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror")).
        // PUŁAPKA: włączenie -Werror w starym projekcie z setkami ostrzeżeń = czerwone budowanie od razu.
        // Dlaczego: najpierw wyczyść ostrzeżenia (albo włączaj kategorie po kolei, np. -Xlint:rawtypes,unchecked).
    }

    // =================================================================================================
    // 5. NARZĘDZIA ANALIZY STATYCZNEJ
    // =================================================================================================

    /**
     * 5. Kompilator sprawdza, czy kod jest POPRAWNĄ Javą. Narzędzia analizy statycznej sprawdzają, czy jest DOBRĄ Javą:
     * styl, typowe błędy, zbyt skomplikowane metody, duplikaty. Uruchamia się je w IDE i w CI.
     */
    static void staticAnalysisTools() {
        section("5. Narzędzia analizy statycznej");

        Map<String, String> tools = new LinkedHashMap<>();
        tools.put("Checkstyle", "styl i konwencje (źródła): nazwy, długość linii, Javadoc, klamry");
        tools.put("PMD", "podejrzane konstrukcje (źródła): pusty catch, nieużywane zmienne, złożoność; CPD = duplikaty");
        tools.put("SpotBugs", "wzorce błędów (bajtkod): null, == na String, zignorowany wynik, equals bez hashCode");
        tools.put("Error Prone", "wzorce błędów jako wtyczka javac — błąd już przy kompilacji");
        tools.put("SonarQube", "serwer: wszystko naraz + historia, pokrycie, bramka jakości (w IDE: SonarLint)");
        showEach("narzędzia", tools);
        // WYNIK: narzędzia (liczba kluczy: 5):
        // WYNIK: • Checkstyle → styl i konwencje (źródła): nazwy, długość linii, Javadoc, klamry
        // WYNIK: • PMD → podejrzane konstrukcje (źródła): pusty catch, nieużywane zmienne, złożoność; CPD = duplikaty
        // WYNIK: • SpotBugs → wzorce błędów (bajtkod): null, == na String, zignorowany wynik, equals bez hashCode
        // WYNIK: • Error Prone → wzorce błędów jako wtyczka javac — błąd już przy kompilacji
        // WYNIK: • SonarQube → serwer: wszystko naraz + historia, pokrycie, bramka jakości (w IDE: SonarLint)

        // Przykłady znalezisk (kod, który kompiluje się bez błędów):
        //   Checkstyle:  int MaxValue = 10;           → nazwa zmiennej powinna być camelCase (maxValue)
        //                if (ok) return;              → brak klamer {} (NeedBraces)
        //                import java.util.*;          → import z gwiazdką (AvoidStarImport)
        //   PMD:         try { ... } catch (IOException e) { }   → pusty catch połyka błąd (EmptyCatchBlock)
        //                private int unused;          → nieużywane pole (UnusedPrivateField)
        //   SpotBugs:    if (name == "admin")         → porównanie String przez == (ES_COMPARING_STRINGS_WITH_EQ)
        //                text.trim();                 → wynik zignorowany, String jest niezmienny (RV_RETURN_VALUE_IGNORED)
        //                equals bez hashCode          → HE_EQUALS_NO_HASHCODE (t12_collections/Collections05Maps)
        //   Error Prone: new IllegalArgumentException("x");  bez throw → DeadException
        //   Sonar:       metoda na 200 linii, złożoność poznawcza 40 → code smell; hasło w kodzie → security hotspot.
        // SonarLint to wtyczka do IntelliJ (obecnie pod nazwą „SonarQube for IDE”) — podkreśla problemy na bieżąco.

        List<String> findings = miniLint(List.of(
                "class Demo {",
                "\tvoid check(String name) {",
                "        if (name == \"admin\") System.out.println(\"witaj\");",
                "        try { run(); } catch (Exception e) { }",
                "    }",
                "}"));
        showEach("mini-analizator (4 reguły)", findings);
        // WYNIK: mini-analizator (4 reguły) (liczba elementów: 4):
        // WYNIK: • linia 2: tabulator zamiast spacji
        // WYNIK: • linia 3: porównanie tekstu przez ==
        // WYNIK: • linia 3: System.out.println (użyj loggera)
        // WYNIK: • linia 4: pusty blok catch
        // Prawdziwe narzędzia nie szukają tekstu wyrażeniami regularnymi — budują drzewo składni (AST) albo czytają
        // bajtkod. Nasz mini-analizator pokazuje tylko IDEĘ: reguła → miejsce → komunikat.
        // PUŁAPKA: włączenie wszystkich reguł naraz → tysiące zgłoszeń i zespół zaczyna je ignorować.
        // Dlaczego: szum zagłusza ważne sygnały. Zacznij od małego zestawu reguł i blokuj tylko NOWE problemy.
    }

    /** Bardzo prosty analizator linii: tabulatory, == z literałem tekstowym, System.out.println, pusty catch. */
    static List<String> miniLint(List<String> lines) {
        List<String> findings = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int number = i + 1;
            if (line.contains("\t")) {
                findings.add("linia " + number + ": tabulator zamiast spacji");
            }
            if (line.matches(".*==\\s*\".*") || line.matches(".*\"\\s*==.*")) {
                findings.add("linia " + number + ": porównanie tekstu przez ==");
            }
            if (line.contains("System.out.println")) {
                findings.add("linia " + number + ": System.out.println (użyj loggera)");
            }
            if (line.matches(".*catch\\s*\\([^)]*\\)\\s*\\{\\s*}.*")) {
                findings.add("linia " + number + ": pusty blok catch");
            }
        }
        return findings;
    }

    // =================================================================================================
    // 6. FORMATOWANIE, KONWENCJE I INSPEKCJE IDE
    // =================================================================================================

    static final String EDITOR_CONFIG = """
            # .editorconfig — wspólne ustawienia edytora dla całego zespołu
            root = true

            [*]
            charset = utf-8
            end_of_line = lf
            insert_final_newline = true

            [*.java]
            indent_style = space
            indent_size = 4
            max_line_length = 120
            """;

    /**
     * 6. Formatowanie nie zmienia działania, ale zmienia czytelność i DIFFY w gicie. Zespół ustala jeden styl
     * i automat go pilnuje — wtedy przegląd kodu dotyczy treści, a nie spacji.
     */
    static void formattingAndInspections() {
        section("6. Formatowanie, konwencje i inspekcje IDE");

        Map<String, String> javaSection = new TreeMap<>();
        boolean inJava = false;
        for (String line : EDITOR_CONFIG.lines().toList()) {
            if (line.startsWith("[")) {
                inJava = line.equals("[*.java]");
            } else if (inJava && line.contains("=")) {
                javaSection.put(line.substring(0, line.indexOf('=')).strip(), line.substring(line.indexOf('=') + 1).strip());
            }
        }
        show("ustawienia [*.java]", javaSection);
        // WYNIK: ustawienia [*.java] → {indent_size=4, indent_style=space, max_line_length=120}

        // Konwencje Javy (Oracle Code Conventions, Google Java Style): klasy PascalCase, metody i zmienne camelCase,
        // stałe UPPER_SNAKE_CASE, pakiety małymi literami, klamra otwierająca w tej samej linii, wcięcie 4 spacje
        // (Google: 2 spacje — ważne, żeby w projekcie był JEDEN styl).
        // IntelliJ:
        //   Ctrl+Alt+L — Reformat Code (formatuj), Ctrl+Alt+O — Optimize Imports (porządkuj importy),
        //   Code → Inspect Code… — pełna analiza projektu (setki inspekcji: nieużywany kod, możliwy NPE, uproszczenia),
        //   Alt+Enter na podkreśleniu — szybka poprawka, Settings → Editor → Code Style — styl (IntelliJ czyta .editorconfig),
        //   „Actions on Save” — formatowanie i porządkowanie importów przy zapisie.
        // Automatyczne formatery w budowaniu: Spotless (Maven/Gradle) z google-java-format albo formatterem Eclipse.
        // DOBRA PRAKTYKA: zmiany formatowania commituj OSOBNO od zmian logiki. Dlaczego: inaczej w diffie nie widać,
        // co naprawdę się zmieniło.
        // PUŁAPKA: end_of_line = lf, a ktoś na Windows ma git z core.autocrlf=true → każdy plik „zmieniony” w całości.
        // Dlaczego: CRLF i LF to różne bajty; ustal końce linii dla repozytorium (.gitattributes / .editorconfig).
    }

    // =================================================================================================
    // 7. POKRYCIE KODU TESTAMI (JaCoCo)
    // =================================================================================================

    /** Ręcznie „zinstrumentowana” metoda: zapisuje, które gałęzie się wykonały (tak działa JaCoCo — na bajtkodzie). */
    static String grade(int points, boolean[] hits) {
        if (points < 0) {
            hits[0] = true;
            throw new IllegalArgumentException("ujemne punkty");
        } else if (points >= 50) {
            hits[1] = true;
            return "zaliczone";
        } else {
            hits[2] = true;
            return "niezaliczone";
        }
    }

    static int coveragePercent(boolean[] hits) {
        int covered = 0;
        for (boolean hit : hits) {
            if (hit) {
                covered++;
            }
        }
        return covered * 100 / hits.length;
    }

    /**
     * 7. Pokrycie mówi, jaka część kodu wykonała się podczas testów: linie (line coverage) i gałęzie if/switch
     * (branch coverage). JaCoCo dołącza się do JVM testów jako agent i zapisuje to automatycznie.
     */
    static void codeCoverage() {
        section("7. Pokrycie kodu testami (JaCoCo)");

        boolean[] onlyHappyPath = new boolean[3];
        grade(80, onlyHappyPath);
        show("test tylko 80 pkt — pokrycie gałęzi %", coveragePercent(onlyHappyPath));
        // WYNIK: test tylko 80 pkt — pokrycie gałęzi % → 33

        boolean[] allPaths = new boolean[3];
        grade(80, allPaths);
        grade(10, allPaths);
        expectThrows("grade(-5)", () -> grade(-5, allPaths));
        // WYNIK: ✔ grade(-5) → rzucono IllegalArgumentException: ujemne punkty
        show("testy 80, 10 i -5 — pokrycie gałęzi %", coveragePercent(allPaths));
        // WYNIK: testy 80, 10 i -5 — pokrycie gałęzi % → 100

        // PUŁAPKA: 100% pokrycia ≠ brak błędów. Granica 50 punktów (49 / 50) nie jest sprawdzona żadnym testem —
        // gdyby ktoś napisał points > 50, pokrycie dalej byłoby 100%. Dlaczego: pokrycie mierzy, co się WYKONAŁO,
        // a nie, co SPRAWDZIŁY asercje. (Narzędzie, które to mierzy: testy mutacyjne, np. PIT.)
        // DOBRA PRAKTYKA: patrz na pokrycie jak na mapę NIEprzetestowanych miejsc, a nie na cel sam w sobie;
        // rozsądny próg dla logiki biznesowej to często 70–80%, ważniejsza jest jakość asercji.
        // JaCoCo w Mavenie: cel prepare-agent (ustawia agenta dla testów), report (raport HTML w target/site/jacoco),
        // check (bramka: np. minimum 80% linii) — fragment w sekcji 8. IntelliJ: Run → Run with Coverage.
    }

    // =================================================================================================
    // 8. POTOK CI I WTYCZKI MAVENA
    // =================================================================================================

    static final String QUALITY_PLUGINS = """
            <build>
              <plugins>
                <plugin>
                  <groupId>org.jacoco</groupId>
                  <artifactId>jacoco-maven-plugin</artifactId>
                  <version>0.8.11</version>
                  <executions>
                    <execution><id>agent</id><goals><goal>prepare-agent</goal></goals></execution>
                    <execution><id>report</id><goals><goal>report</goal></goals></execution>
                  </executions>
                </plugin>
                <plugin>
                  <groupId>org.apache.maven.plugins</groupId>
                  <artifactId>maven-checkstyle-plugin</artifactId>
                  <version>3.3.1</version>
                  <configuration><configLocation>google_checks.xml</configLocation></configuration>
                  <executions><execution><goals><goal>check</goal></goals></execution></executions>
                </plugin>
                <plugin>
                  <groupId>com.github.spotbugs</groupId>
                  <artifactId>spotbugs-maven-plugin</artifactId>
                  <version>4.8.3.0</version>
                  <executions><execution><goals><goal>check</goal></goals></execution></executions>
                </plugin>
              </plugins>
            </build>
            """;

    /**
     * 8. CI (Continuous Integration = ciągła integracja): przy każdym pushu serwer (GitHub Actions, GitLab CI, Jenkins)
     * buduje projekt od zera i uruchamia etapy po kolei. Pierwszy nieudany etap zatrzymuje potok i oznacza zmianę na czerwono.
     */
    static void ciPipeline() {
        section("8. Potok CI i wtyczki Mavena");

        show("wtyczki jakości w pom.xml", findAll(QUALITY_PLUGINS, "<artifactId>([\\w.-]+)</artifactId>"));
        // WYNIK: wtyczki jakości w pom.xml → [jacoco-maven-plugin, maven-checkstyle-plugin, spotbugs-maven-plugin]
        // Wersje w przykładzie są przykładowe — w prawdziwym projekcie sprawdź aktualne w Maven Central.
        // Cele check (checkstyle, spotbugs) i report (jacoco) są domyślnie przypięte do fazy verify, a prepare-agent
        // do initialize — więc wystarczy ./mvnw verify. Bez wpisu w pom.xml można je też wołać jednorazowo,
        // np. ./mvnw checkstyle:check.

        Map<String, Supplier<Boolean>> stages = new LinkedHashMap<>();
        stages.put("kompilacja (-Xlint:all -Werror)", () -> true);
        stages.put("testy (surefire)", () -> true);
        stages.put("analiza (checkstyle, spotbugs)", () -> false);   // np. SpotBugs znalazł == na String
        stages.put("pakowanie (jar)", () -> true);
        show("wynik potoku", runPipeline(stages));
        // WYNIK: wynik potoku → wykonane: [kompilacja (-Xlint:all -Werror), testy (surefire), analiza (checkstyle, spotbugs)] → BŁĄD w: analiza (checkstyle, spotbugs)

        // Minimalny GitHub Actions (.github/workflows/build.yml):
        //   on: [push, pull_request]
        //   jobs:
        //     build:
        //       runs-on: ubuntu-latest
        //       steps:
        //         - uses: actions/checkout@v4
        //         - uses: actions/setup-java@v4
        //           with: { distribution: temurin, java-version: '17', cache: maven }
        //         - run: ./mvnw -B verify          (-B = batch mode = tryb wsadowy, bez kolorów i pytań)
        // DOBRA PRAKTYKA: kolejność od najszybszych etapów (kompilacja, testy jednostkowe) do najwolniejszych
        // (testy integracyjne, analiza) — dlaczego: najczęstsze błędy wychodzą po kilkudziesięciu sekundach, nie po kwadransie.
        // PUŁAPKA: „u mnie działa”, a w CI czerwono — inna wersja JDK, brak pliku poza gitem, zależność od strefy czasowej
        // lub Locale. Dlaczego: CI buduje od zera na czystej maszynie — i właśnie po to istnieje.
    }

    /** Uruchamia etapy po kolei; zatrzymuje się na pierwszym nieudanym. */
    static String runPipeline(Map<String, Supplier<Boolean>> stages) {
        List<String> done = new ArrayList<>();
        for (Map.Entry<String, Supplier<Boolean>> stage : stages.entrySet()) {
            done.add(stage.getKey());
            if (!stage.getValue().get()) {
                return "wykonane: " + done + " → BŁĄD w: " + stage.getKey();
            }
        }
        return "wykonane: " + done + " → SUKCES";
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Javadoc: komentarz z dwiema gwiazdkami przed elementem; pierwsze zdanie = streszczenie
     *   • znaczniki blokowe (na początku linii): @param, @return, @throws, @see, @since, @deprecated
     *   • wewnątrzliniowe: {@code ...} (kod, bez HTML), {@link Klasa#metoda} (sprawdzany odnośnik)
     *   • javadoc -d docs -sourcepath src -subpackages pl.kurs  /  ./mvnw javadoc:javadoc
     *   • doclint: -Xdoclint:all (javac i javadoc); '<' w tekście = błąd; brak @param = ostrzeżenie
     *   • javac -Xlint:all (rawtypes, unchecked, fallthrough, serial, deprecation, removal, cast...) + -Werror = bramka
     *   • @SuppressWarnings("kategoria") — jak najwęziej i z uzasadnieniem
     *   • Checkstyle = styl, PMD = podejrzane konstrukcje + CPD, SpotBugs = błędy w bajtkodzie, Error Prone = javac,
     *     SonarQube/SonarLint = całość + bramka jakości
     *   • .editorconfig + formatter (IntelliJ Ctrl+Alt+L, Spotless) — jeden styl dla całego zespołu
     *   • JaCoCo: pokrycie linii i gałęzi; 100% ≠ brak błędów; prepare-agent + report + check
     *   • CI: przy każdym pushu ./mvnw -B verify; pierwszy czerwony etap zatrzymuje potok
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się znacznik blokowy od wewnątrzliniowego? Podaj po dwa przykłady.
     *   2. ZNAJDŹ BŁĄD:  w komentarzu Javadoc napisano  "Zwraca true, gdy a < b."  — javadoc zgłasza błąd. Dlaczego i jak poprawić?
     *   3. Co wypisze:  show("x", coveragePercent(new boolean[]{true, false, false, true}));  ?
     *   4. Dlaczego kompilacja z -Xlint:all się udaje, a z -Xlint:all -Werror — nie?
     *   5. ZNAJDŹ BŁĄD (SpotBugs zgłasza ostrzeżenie):  String clean = input; clean.trim(); return clean;
     *   6. Co wypisze:  show("x", miniLint(List.of("if (a == \"b\") { }")));  ?
     *   7. Test wywołuje metodę z każdą gałęzią, ale nie ma żadnej asercji. Jakie będzie pokrycie i co ono mówi o jakości?
     *   8. Które narzędzie analizuje bajtkod, a które kod źródłowy: Checkstyle, SpotBugs, PMD?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static final String DOC_FOR_EXERCISE = """
            /**
             * Przelewa pieniądze.
             * @param from konto źródłowe
             * @param amount kwota
             */""";

    static final List<String> DIAGNOSTICS = List.of(
            "compiler.warn.raw.class.use:8", "compiler.warn.redundant.cast:20", "compiler.warn.raw.class.use:8",
            "compiler.warn.missing.SVUID:6", "compiler.warn.raw.class.use:31");

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: brakujące @param", List.of("to", "title"),
                () -> exercise1(List.of("from", "to", "amount", "title"), DOC_FOR_EXERCISE));
        Check.equal("ćw. 2: analizator długości linii", List.of("linia 2: 13 znaków (max 10)", "linia 3: tabulator"),
                () -> exercise2(List.of("int a = 1;", "int bbb = 22;", "\tint c;"), 10));
        Check.equal("ćw. 3: liczba ostrzeżeń wg kodu",
                Map.of("compiler.warn.missing.SVUID", 1L, "compiler.warn.raw.class.use", 3L, "compiler.warn.redundant.cast", 1L),
                () -> exercise3(DIAGNOSTICS));
        Check.equal("ćw. 4: bramka jakości — OK", List.of(), () -> exercise4(0, 85, 0));
        Check.equal("ćw. 4: bramka jakości — 3 powody", List.of("ostrzeżenia kompilatora: 2", "pokrycie 64% < 80%", "błędy SpotBugs: 1"),
                () -> exercise4(2, 64, 1));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", List.of("to", "title"),
                () -> solution1(List.of("from", "to", "amount", "title"), DOC_FOR_EXERCISE));
        Check.equal("ćw. 2 (wzorzec)", List.of("linia 2: 13 znaków (max 10)", "linia 3: tabulator"),
                () -> solution2(List.of("int a = 1;", "int bbb = 22;", "\tint c;"), 10));
        Check.equal("ćw. 3 (wzorzec)",
                Map.of("compiler.warn.missing.SVUID", 1L, "compiler.warn.raw.class.use", 3L, "compiler.warn.redundant.cast", 1L),
                () -> solution3(DIAGNOSTICS));
        Check.equal("ćw. 4 (wzorzec): OK", List.of(), () -> solution4(0, 85, 0));
        Check.equal("ćw. 4 (wzorzec): 3 powody", List.of("ostrzeżenia kompilatora: 2", "pokrycie 64% < 80%", "błędy SpotBugs: 1"),
                () -> solution4(2, 64, 1));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 5 OK, ✘ 0 BŁĄD
    }

    /**
     * ĆWICZENIE 1 (łatwe): które parametry metody NIE mają znacznika param w komentarzu? Zachowaj kolejność parametrów.
     * Podpowiedź: dla każdej nazwy sprawdź, czy komentarz zawiera tekst "@param " + nazwa + " " (spacja na końcu,
     * żeby "to" nie pasowało do "total").
     */
    static List<String> exercise1(List<String> parameters, String javadoc) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): prosty Checkstyle. Dla każdej linii (numeracja od 1) zgłoś: dłuższą niż max →
     * "linia N: X znaków (max M)"; zawierającą tabulator → "linia N: tabulator". Najpierw długość, potem tabulator.
     * Podpowiedź: zwykła pętla for z indeksem; length() liczy też znak tabulacji.
     */
    static List<String> exercise2(List<String> lines, int max) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): PRZEPISZ pętlę na strumień z groupingBy (t16_streams/Streams11GroupingBy). Wpis ma postać
     * "kod:linia"; policz wystąpienia każdego kodu. Stara wersja:
     * <pre>{@code
     * Map<String, Long> counts = new TreeMap<>();
     * for (String d : diagnostics) {
     *     String code = d.substring(0, d.lastIndexOf(':'));
     *     counts.put(code, counts.getOrDefault(code, 0L) + 1);
     * }
     * return counts;
     * }</pre>
     * Podpowiedź: Collectors.groupingBy(klucz, TreeMap::new, Collectors.counting()).
     */
    static Map<String, Long> exercise3(List<String> diagnostics) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): bramka jakości. Zwróć listę powodów odrzucenia (pusta = przepuść), w tej kolejności:
     * warnings > 0 → "ostrzeżenia kompilatora: N"; coveragePercent < 80 → "pokrycie P% < 80%";
     * bugs > 0 → "błędy SpotBugs: B".
     * Podpowiedź: ArrayList + trzy if-y; zwróć List.copyOf, żeby wynik był niezmienny.
     */
    static List<String> exercise4(int warnings, int coveragePercent, int bugs) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static List<String> solution1(List<String> parameters, String javadoc) {
        return parameters.stream().filter(p -> !javadoc.contains("@param " + p + " ")).toList();
    }

    static List<String> solution2(List<String> lines, int max) {
        List<String> findings = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.length() > max) {
                findings.add("linia " + (i + 1) + ": " + line.length() + " znaków (max " + max + ")");
            }
            if (line.contains("\t")) {
                findings.add("linia " + (i + 1) + ": tabulator");
            }
        }
        return findings;
    }

    static Map<String, Long> solution3(List<String> diagnostics) {
        return diagnostics.stream()
                .collect(Collectors.groupingBy(d -> d.substring(0, d.lastIndexOf(':')), TreeMap::new, Collectors.counting()));
    }

    static List<String> solution4(int warnings, int coveragePercent, int bugs) {
        List<String> reasons = new ArrayList<>();
        if (warnings > 0) {
            reasons.add("ostrzeżenia kompilatora: " + warnings);
        }
        if (coveragePercent < 80) {
            reasons.add("pokrycie " + coveragePercent + "% < 80%");
        }
        if (bugs > 0) {
            reasons.add("błędy SpotBugs: " + bugs);
        }
        return List.copyOf(reasons);
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Blokowy stoi na początku linii i tworzy osobną sekcję dokumentacji (@param, @return, @throws, @see);
     *      wewnątrzliniowy jest w klamrach w środku zdania ({@code ...}, {@link ...}).
     *   2. Javadoc to HTML — '<' rozpoczyna znacznik HTML, więc doclint zgłasza błąd (malformed HTML = źle zbudowany HTML).
     *      Poprawnie: "Zwraca true, gdy {@code a < b}."
     *   3. x → 50   (2 z 4 gałęzi = 50%)
     *   4. Kod ma tylko ostrzeżenia — kompilacja się udaje. -Werror zamienia każde ostrzeżenie w błąd, więc javac
     *      kończy się niepowodzeniem (compiler.err.warnings.and.werror).
     *   5. String jest niezmienny — trim() zwraca NOWY tekst, który tu jest zignorowany (RV_RETURN_VALUE_IGNORED).
     *      Poprawnie: return input.trim();  (albo strip() w Javie 11+).
     *   6. x → [linia 1: porównanie tekstu przez ==]   (puste klamry po if to nie catch — ta reguła nie pasuje)
     *   7. Pokrycie 100%, ale test niczego nie sprawdza — wykryje tylko wyjątki. Pokrycie mierzy wykonanie, nie weryfikację.
     *   8. SpotBugs — bajtkod (pliki .class); Checkstyle i PMD — kod źródłowy (.java).
     */
    // </editor-fold>
}
