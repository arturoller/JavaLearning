package t30_build_modules;

import helpers.Check;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Maven od podstaw — po co narzędzie budowania i jak czytać pom.xml
 *        (build tool = narzędzie budowania, POM = Project Object Model = model obiektowy projektu)
 *
 * W SKRÓCIE:
 *   Maven pobiera biblioteki (zależności), kompiluje, uruchamia testy i pakuje program do pliku JAR —
 *   zawsze tak samo, na każdym komputerze. Cały opis projektu siedzi w jednym pliku: pom.xml.
 *   Lekcja NIE uruchamia Mavena (potrzebuje sieci i czasu) — rozbieramy jego pomysły na małych modelach w Javie.
 *
 * ANALOGIA:
 *   Przepis kulinarny z listą zakupów. Zamiast mówić kucharzowi „kup coś do sosu”, piszesz:
 *   „pomidory, marka X, puszka 400 g”. Każdy, kto weźmie przepis, ugotuje to samo danie.
 *   pom.xml to taki przepis: dokładne nazwy i wersje bibliotek plus kolejne kroki gotowania (fazy).
 *
 * JAK TO DZIAŁA:
 *   1. Czytasz pom.xml → Maven zna współrzędne projektu i listę zależności.
 *   2. Zależności (także przechodnie) pobiera z Maven Central do lokalnego repozytorium ~/.m2/repository.
 *   3. Polecenie ./mvnw package uruchamia fazy po kolei: validate → compile → test → package.
 *   4. Każdą fazę wykonują cele (goals) wtyczek, np. compiler:compile, surefire:test, jar:jar.
 *   5. Wynik: target/nazwa-wersja.jar.
 *
 * SŁÓWKA:
 *   groupId = identyfikator grupy (organizacji); artifactId = identyfikator artefaktu; version = wersja;
 *   dependency = zależność; scope = zakres; lifecycle = cykl życia; phase = faza; goal = cel;
 *   plugin = wtyczka; repository = repozytorium; wrapper = nakładka (opakowanie); transitive = przechodnia.
 *
 * ZOBACZ TEŻ: t18_io_files/Io14Xml (parser DOM i bezpieczna fabryka), t30_build_modules/Build02JarClasspath (co Maven
 *             robi pod spodem: javac, jar, classpath), t32_junit_mockito/JUnit01Basics (zakres test w praktyce).
 * </pre>
 */
public class Build01MavenBasics {

    public static void main(String[] args) {
        title("Build01 — Maven od podstaw");

        whyBuildTools();          // why build tools = po co narzędzia budowania
        coordinates();            // coordinates = współrzędne
        pomAnatomy();             // pom anatomy = budowa pom.xml
        directoryLayout();        // directory layout = układ katalogów
        lifecycleAndPhases();     // lifecycle and phases = cykl życia i fazy
        dependencyScopes();       // dependency scopes = zakresy zależności
        transitiveConflicts();    // transitive conflicts = konflikty zależności przechodnich
        versionRangesPitfall();   // version ranges pitfall = pułapka zakresów wersji
        wrapperGradleSpring();    // wrapper, Gradle, Spring = nakładka, Gradle i Spring Boot
        exercises();              // exercises = ćwiczenia
    }

    // =================================================================================================
    // 1. PO CO NARZĘDZIA BUDOWANIA
    // =================================================================================================

    /**
     * 1. Bez narzędzia budowania każdy krok robisz ręcznie: szukasz JAR-ów w internecie, wpisujesz długie polecenia javac,
     * pamiętasz kolejność. Maven (albo Gradle) robi to samo powtarzalnie — także na serwerze CI, gdzie nie ma IDE.
     */
    static void whyBuildTools() {
        section("1. Po co narzędzie budowania");

        List<String> manual = List.of(
                "znajdź i pobierz JAR-y bibliotek (i ICH zależności!)",
                "javac -d out -cp lib/a.jar;lib/b.jar ... wszystkie pliki",
                "uruchom testy ręcznie",
                "jar --create ... spakuj wynik");
        showEach("ręcznie", manual);
        // WYNIK: ręcznie (liczba elementów: 4):
        // WYNIK: • znajdź i pobierz JAR-y bibliotek (i ICH zależności!)
        // WYNIK: • javac -d out -cp lib/a.jar;lib/b.jar ... wszystkie pliki
        // WYNIK: • uruchom testy ręcznie
        // WYNIK: • jar --create ... spakuj wynik

        show("z Mavenem", "./mvnw package");
        // WYNIK: z Mavenem → ./mvnw package

        // Trzy główne zyski:
        //   • zarządzanie zależnościami — piszesz współrzędne, Maven pobiera plik i jego zależności przechodnie,
        //   • powtarzalność (repeatable build = powtarzalne budowanie) — ten sam pom.xml = ten sam wynik u każdego,
        //   • konwencja zamiast konfiguracji — standardowe katalogi i fazy, więc każdy projekt Mavena „wygląda znajomo”.
        // IntelliJ: otwierasz pom.xml jako projekt → IDE samo czyta zależności (panel „Maven” po prawej stronie,
        // przycisk „Reload All Maven Projects” = przeładuj projekty Mavena po zmianie pom.xml).

        // DOBRA PRAKTYKA: budowanie, które przechodzi tylko „u mnie w IDE”, to nie budowanie. Sprawdzaj projekt także
        // poleceniem z konsoli (./mvnw verify), bo tak samo zrobi serwer CI — dlaczego: IDE potrafi ukryć brak zależności
        // albo inną wersję Javy.
    }

    // =================================================================================================
    // 2. WSPÓŁRZĘDNE MAVENA
    // =================================================================================================

    /** Współrzędne artefaktu: groupId:artifactId:version. */
    record Coordinates(String groupId, String artifactId, String version) {

        static Coordinates parse(String text) {                 // parse = przetwórz tekst
            String[] parts = text.split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException("oczekiwano g:a:v, jest: " + text);
            }
            return new Coordinates(parts[0], parts[1], parts[2]);
        }

        boolean isSnapshot() {                                  // is snapshot = czy wersja rozwojowa
            return version.endsWith("-SNAPSHOT");
        }

        /** Ścieżka pliku JAR w lokalnym repozytorium (zawsze z "/" — tak zapisujemy ścieżki repozytorium). */
        String repositoryPath() {                               // repository path = ścieżka w repozytorium
            return groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/"
                    + artifactId + "-" + version + ".jar";
        }
    }

    /**
     * 2. Każdy artefakt (plik JAR, POM) ma unikalny adres: groupId (zwykle odwrócona domena), artifactId (nazwa)
     * i version. Wersja z końcówką -SNAPSHOT to wersja ROZWOJOWA — może się zmieniać pod tym samym numerem.
     */
    static void coordinates() {
        section("2. Współrzędne: groupId:artifactId:version");

        Coordinates junit = Coordinates.parse("org.junit.jupiter:junit-jupiter:5.10.2");
        show("groupId", junit.groupId());
        // WYNIK: groupId → org.junit.jupiter
        show("artifactId", junit.artifactId());
        // WYNIK: artifactId → junit-jupiter
        show("ścieżka w ~/.m2/repository", junit.repositoryPath());
        // WYNIK: ścieżka w ~/.m2/repository → org/junit/jupiter/junit-jupiter/5.10.2/junit-jupiter-5.10.2.jar

        Coordinates mine = Coordinates.parse("pl.kurs:sklep:1.0.0-SNAPSHOT");
        show("pl.kurs:sklep:1.0.0-SNAPSHOT to SNAPSHOT", mine.isSnapshot());
        // WYNIK: pl.kurs:sklep:1.0.0-SNAPSHOT to SNAPSHOT → true
        show("junit 5.10.2 to SNAPSHOT", junit.isSnapshot());
        // WYNIK: junit 5.10.2 to SNAPSHOT → false

        expectThrows("brak wersji", () -> Coordinates.parse("pl.kurs:sklep"));
        // WYNIK: ✔ brak wersji → rzucono IllegalArgumentException: oczekiwano g:a:v, jest: pl.kurs:sklep

        // SNAPSHOT vs wydanie (release):
        //   • 1.0.0-SNAPSHOT — „migawka” w trakcie prac; Maven domyślnie raz dziennie sprawdza w zdalnym repozytorium,
        //     czy nie ma nowszej migawki (wymuszenie: ./mvnw -U ...),
        //   • 1.0.0 — wydanie; po opublikowaniu w Maven Central nie da się go już podmienić.
        // PUŁAPKA: zależność od cudzego SNAPSHOT-a w gotowym produkcie — dziś działa, jutro autor wypchnie zmianę
        // i Twoje budowanie da inny wynik. Dlaczego: SNAPSHOT z definicji nie jest niezmienny.
        // Pełne współrzędne mogą mieć też packaging (rodzaj paczki: jar, war, pom) i classifier (klasyfikator,
        // np. sources = źródła): g:a:packaging:classifier:version — na co dzień wystarczą trzy części.
    }

    // =================================================================================================
    // 3. BUDOWA POM.XML
    // =================================================================================================

    static final String POM = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>pl.kurs</groupId>
                <artifactId>sklep</artifactId>
                <version>1.0.0-SNAPSHOT</version>
                <packaging>jar</packaging>

                <properties>
                    <maven.compiler.release>17</maven.compiler.release>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                </properties>

                <dependencies>
                    <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter</artifactId>
                        <version>5.10.2</version>
                        <scope>test</scope>
                    </dependency>
                    <dependency>
                        <groupId>com.h2database</groupId>
                        <artifactId>h2</artifactId>
                        <version>2.2.224</version>
                        <scope>runtime</scope>
                    </dependency>
                    <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-databind</artifactId>
                        <version>2.16.1</version>
                    </dependency>
                    <dependency>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>1.18.30</version>
                        <scope>provided</scope>
                    </dependency>
                </dependencies>

                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>3.11.0</version>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

    /**
     * 3. pom.xml to zwykły XML, więc odczytamy go parserem DOM z JDK (jak w t18_io_files/Io14Xml) i wypiszemy
     * zależności posortowane. Fabrykę parsera konfigurujemy BEZPIECZNIE — XML z zewnątrz może zawierać złośliwe encje.
     */
    static void pomAnatomy() {
        section("3. Budowa pom.xml (odczyt parserem DOM)");

        // Części pom.xml (od góry):
        //   modelVersion  — wersja formatu POM (zawsze 4.0.0),
        //   groupId/artifactId/version/packaging — współrzędne NASZEGO projektu,
        //   properties    — właściwości (zmienne); maven.compiler.release=17 = kompiluj jak javac --release 17,
        //                   project.build.sourceEncoding=UTF-8 = kodowanie plików źródłowych (bez tego ostrzeżenie
        //                   i kodowanie zależne od systemu — na polskim Windows to zepsute „ąę”),
        //   dependencies  — zależności (każda: współrzędne + opcjonalny scope),
        //   build/plugins — wtyczki i ich konfiguracja.
        Document doc = parseSecurely(POM);
        Element root = doc.getDocumentElement();
        show("projekt", childText(root, "groupId") + ":" + childText(root, "artifactId") + ":" + childText(root, "version"));
        // WYNIK: projekt → pl.kurs:sklep:1.0.0-SNAPSHOT

        List<String> deps = dependencies(doc);
        showEach("zależności (posortowane)", deps);
        // WYNIK: zależności (posortowane) (liczba elementów: 4):
        // WYNIK: • com.fasterxml.jackson.core:jackson-databind:2.16.1 [compile]
        // WYNIK: • com.h2database:h2:2.2.224 [runtime]
        // WYNIK: • org.junit.jupiter:junit-jupiter:5.10.2 [test]
        // WYNIK: • org.projectlombok:lombok:1.18.30 [provided]
        // Zależność bez <scope> ma zakres compile — to wartość domyślna, dlatego dopisujemy ją sami.

        // PUŁAPKA: domyślna fabryka DOM przetwarza DOCTYPE i encje zewnętrzne (atak XXE — XML External Entity =
        // zewnętrzna encja XML, może odczytać plik z dysku serwera). Nasza fabryka odrzuca DOCTYPE w ogóle.
        String evil = "<?xml version=\"1.0\"?><!DOCTYPE p [<!ENTITY x SYSTEM \"file:///tajny-plik.txt\">]><p>&x;</p>";
        try {
            parseSecurely(evil);
            show("złośliwy XML odrzucony", false);
        } catch (IllegalStateException e) {
            show("złośliwy XML odrzucony", true);
            show("przyczyna", e.getCause().getClass().getSimpleName());
        }
        // WYNIK: złośliwy XML odrzucony → true
        // WYNIK: przyczyna → SAXParseException
        // Komunikatu wyjątku nie wypisujemy — jego treść zależy od implementacji parsera.
    }

    /** Bezpieczna fabryka DOM: bez DOCTYPE, bez encji zewnętrznych, błędy jako wyjątki (a nie wydruk na stderr). */
    static Document parseSecurely(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();   // factory = fabryka
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // zakaz DOCTYPE
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);   // tryb bezpiecznego przetwarzania
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");         // żadnych zewnętrznych DTD
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");      // ani zewnętrznych schematów
            factory.setXIncludeAware(false);                                    // bez dołączania innych plików
            factory.setExpandEntityReferences(false);                           // nie rozwijaj encji
            DocumentBuilder builder = factory.newDocumentBuilder();              // builder = budowniczy
            // Domyślna obsługa błędów wypisuje „[Fatal Error] ...” na stderr — własna tylko rzuca wyjątek.
            builder.setErrorHandler(new ErrorHandler() {                         // error handler = obsługa błędów
                @Override public void warning(SAXParseException e) throws SAXParseException { throw e; }
                @Override public void error(SAXParseException e) throws SAXParseException { throw e; }
                @Override public void fatalError(SAXParseException e) throws SAXParseException { throw e; }
            });
            return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("nie da się odczytać XML", e);
        }
    }

    /** Tekst BEZPOŚREDNIEGO dziecka o danej nazwie albo null (getElementsByTagName szukałby też głębiej!). */
    static String childText(Element parent, String name) {
        NodeList children = parent.getChildNodes();                              // child nodes = węzły dzieci
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element && element.getTagName().equals(name)) {
                return element.getTextContent().strip();                         // strip (Java 11+) = przytnij
            }
        }
        return null;
    }

    /** Zależności jako "g:a:v [scope]", posortowane alfabetycznie. */
    static List<String> dependencies(Document doc) {
        NodeList nodes = doc.getElementsByTagName("dependency");
        List<String> result = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element dep = (Element) nodes.item(i);
            String scope = childText(dep, "scope");
            result.add(childText(dep, "groupId") + ":" + childText(dep, "artifactId") + ":" + childText(dep, "version")
                    + " [" + (scope == null ? "compile" : scope) + "]");
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }

    // =================================================================================================
    // 4. STANDARDOWY UKŁAD KATALOGÓW
    // =================================================================================================

    /**
     * 4. Konwencja zamiast konfiguracji: Maven zakłada konkretne katalogi. Trzymasz się ich → pom.xml jest krótki.
     */
    static void directoryLayout() {
        section("4. Standardowy układ katalogów");

        List<String> layout = List.of(
                "pom.xml",
                "src/main/java — kod programu",
                "src/main/resources — zasoby trafiające do JAR-a (np. application.properties)",
                "src/test/java — testy (nie trafiają do JAR-a)",
                "src/test/resources — zasoby testów",
                "target/ — WYNIK budowania (klasy, JAR) — nie wrzucaj do gita");
        showEach("układ Mavena", layout);
        // WYNIK: układ Mavena (liczba elementów: 6):
        // WYNIK: • pom.xml
        // WYNIK: • src/main/java — kod programu
        // WYNIK: • src/main/resources — zasoby trafiające do JAR-a (np. application.properties)
        // WYNIK: • src/test/java — testy (nie trafiają do JAR-a)
        // WYNIK: • src/test/resources — zasoby testów
        // WYNIK: • target/ — WYNIK budowania (klasy, JAR) — nie wrzucaj do gita

        // Ten kurs ŚWIADOMIE łamie konwencję: lekcje leżą w src/tNN_temat zamiast src/main/java/tNN_temat,
        // żeby ścieżki były krótkie. W pom.xml kursu wystarcza jedna linia w sekcji build:
        //     <sourceDirectory>src</sourceDirectory>      (sourceDirectory = katalog źródeł)
        // PUŁAPKA: zmieniasz układ, ale zapominasz powiedzieć o tym Mavenowi → ./mvnw compile kończy się sukcesem,
        // a w target/classes nie ma ani jednej klasy. Dlaczego: Maven szukał plików w src/main/java, gdzie nic nie ma.
        // DOBRA PRAKTYKA: w nowych projektach trzymaj się standardowego układu — każdy programista (i każde IDE)
        // od razu wie, gdzie co leży; Spring Boot i jego generator projektów też go zakładają.
        note("ten kurs: sourceDirectory = src (świadome odstępstwo)");
        // WYNIK: ℹ ten kurs: sourceDirectory = src (świadome odstępstwo)
    }

    // =================================================================================================
    // 5. CYKL ŻYCIA, FAZY, WTYCZKI I CELE
    // =================================================================================================

    /** Główne fazy domyślnego cyklu życia (pełna lista ma ponad 20 faz, np. process-resources, test-compile). */
    static final List<String> PHASES = List.of("validate", "compile", "test", "package", "verify", "install", "deploy");

    /** Fazy wykonane po wpisaniu ./mvnw faza — wszystkie od początku cyklu aż do niej włącznie. */
    static List<String> phasesRunFor(String phase) {
        int index = PHASES.indexOf(phase);
        if (index < 0) {
            throw new IllegalArgumentException("nieznana faza: " + phase);
        }
        return PHASES.subList(0, index + 1);   // subList = widok fragmentu listy
    }

    /**
     * 5. Faza to KROK cyklu życia (np. package), a pracę wykonują CELE wtyczek przypięte do faz. Uruchomienie fazy
     * uruchamia wszystkie wcześniejsze — ./mvnw package skompiluje i przetestuje kod przed spakowaniem.
     */
    static void lifecycleAndPhases() {
        section("5. Cykl życia: fazy, wtyczki i cele");

        show("./mvnw package", phasesRunFor("package"));
        // WYNIK: ./mvnw package → [validate, compile, test, package]
        show("./mvnw install", phasesRunFor("install"));
        // WYNIK: ./mvnw install → [validate, compile, test, package, verify, install]
        expectThrows("./mvnw build", () -> phasesRunFor("build"));
        // WYNIK: ✔ ./mvnw build → rzucono IllegalArgumentException: nieznana faza: build

        Map<String, String> bindings = new LinkedHashMap<>();  // bindings = przypięcia (faza → cel wtyczki)
        bindings.put("compile", "compiler:compile (maven-compiler-plugin)");
        bindings.put("test", "surefire:test (maven-surefire-plugin)");
        bindings.put("package", "jar:jar (maven-jar-plugin)");
        bindings.put("install", "install:install (kopiuje JAR do ~/.m2/repository)");
        bindings.put("deploy", "deploy:deploy (wysyła JAR do zdalnego repozytorium)");
        showEach("domyślne przypięcia dla packaging=jar", bindings);
        // WYNIK: domyślne przypięcia dla packaging=jar (liczba kluczy: 5):
        // WYNIK: • compile → compiler:compile (maven-compiler-plugin)
        // WYNIK: • test → surefire:test (maven-surefire-plugin)
        // WYNIK: • package → jar:jar (maven-jar-plugin)
        // WYNIK: • install → install:install (kopiuje JAR do ~/.m2/repository)
        // WYNIK: • deploy → deploy:deploy (wysyła JAR do zdalnego repozytorium)

        // Inne cykle życia: clean (usuwa target/) i site (strona dokumentacji). Typowe polecenia:
        //   ./mvnw clean verify          — od zera: wyczyść, skompiluj, testuj, spakuj, sprawdź
        //   ./mvnw dependency:tree       — sam CEL wtyczki (bez faz): drzewo zależności
        //   ./mvnw package -DskipTests   — kompiluje testy, ale ich nie uruchamia
        //   ./mvnw package -Dmaven.test.skip=true — nawet nie kompiluje testów
        //   ./mvnw -o verify             — offline (bez sieci, tylko ~/.m2)
        // Ten kurs: ./mvnw -q dependency:copy-dependencies -DoutputDirectory=temp/lib — kopiuje JAR-y zależności
        // do temp/lib (-q = quiet = cicho).
        // PUŁAPKA: ./mvnw install „żeby działało” w zwykłym projekcie — zaśmiecasz ~/.m2 własnymi SNAPSHOT-ami, które
        // potem przesłaniają nowsze wersje w innych projektach. Dlaczego: install potrzebny jest tylko wtedy,
        // gdy INNY lokalny projekt ma użyć Twojego JAR-a. Do sprawdzenia kodu wystarczy verify.
    }

    // =================================================================================================
    // 6. ZAKRESY ZALEŻNOŚCI
    // =================================================================================================

    /** Zakres zależności: gdzie jest widoczna i czy przechodzi dalej. */
    enum Scope {
        COMPILE(true, true, true, true),
        PROVIDED(true, true, false, false),
        RUNTIME(false, true, true, true),
        TEST(false, true, false, false);

        final boolean mainCompile;   // widoczna przy kompilacji kodu programu
        final boolean tests;         // widoczna w testach
        final boolean packaged;      // obecna w działającej aplikacji (pakowana / na ścieżce uruchomieniowej)
        final boolean transitive;    // przechodzi dalej do tych, którzy zależą od NAS

        Scope(boolean mainCompile, boolean tests, boolean packaged, boolean transitive) {
            this.mainCompile = mainCompile;
            this.tests = tests;
            this.packaged = packaged;
            this.transitive = transitive;
        }

        String row() {                                         // row = wiersz tabeli
            return String.format("%-9s %-9s %-7s %-9s %s", name().toLowerCase(java.util.Locale.ROOT),
                    yes(mainCompile), yes(tests), yes(packaged), yes(transitive));
        }

        private static String yes(boolean b) {
            return b ? "tak" : "nie";
        }
    }

    /**
     * 6. Zakres (scope) mówi, w których fazach zależność jest potrzebna. Dobry zakres = mniejszy JAR, mniej konfliktów
     * i brak przypadkowego użycia biblioteki testowej w kodzie produkcyjnym.
     */
    static void dependencyScopes() {
        section("6. Zakresy zależności");

        System.out.println("zakres    kod main  testy   w runtime przechodnia");
        // WYNIK: zakres    kod main  testy   w runtime przechodnia
        for (Scope scope : Scope.values()) {
            System.out.println(scope.row());
        }
        // WYNIK: compile   tak       tak     tak       tak
        // WYNIK: provided  tak       tak     nie       nie
        // WYNIK: runtime   nie       tak     tak       tak
        // WYNIK: test      nie       tak     nie       nie

        // Przykłady:
        //   compile  — Jackson, Guava: używasz ich klas w kodzie i potrzebujesz w działającym programie,
        //   provided — „dostarczy ktoś inny”: Servlet API (daje serwer, np. Tomcat), Lombok (działa tylko podczas
        //              kompilacji — t20_lombok/Lombok01Accessors),
        //   runtime  — sterownik JDBC (H2, PostgreSQL): kod używa interfejsów java.sql, sterownik potrzebny dopiero
        //              po uruchomieniu (t29),
        //   test     — JUnit, AssertJ, Mockito: tylko src/test/java.
        // Jest jeszcze import — używany wyłącznie w dependencyManagement dla BOM (sekcja 9) — oraz przestarzały system.
        // PUŁAPKA: JUnit bez <scope>test</scope> → klasy testowe widoczne w src/main/java, a JUnit ląduje w gotowej
        // aplikacji. Dlaczego: brak scope = compile.
        // PUŁAPKA: sterownik bazy w zakresie runtime, a kod importuje jego klasę (org.h2.Driver) → błąd kompilacji.
        // Dlaczego: runtime NIE jest widoczny podczas kompilacji kodu main — i dobrze, pisz do interfejsów java.sql.
    }

    // =================================================================================================
    // 7. ZALEŻNOŚCI PRZECHODNIE I KONFLIKTY
    // =================================================================================================

    /**
     * Rozwiązuje wersje jak Maven: przechodzimy drzewo wszerz (BFS = breadth-first search = przeszukiwanie wszerz);
     * pierwsza napotkana wersja artefaktu wygrywa. Wszerz = najpierw płytsze poziomy, więc wygrywa NAJBLIŻSZA
     * deklaracja, a przy równej głębokości — zadeklarowana WCZEŚNIEJ.
     */
    static Map<String, String> resolveNearest(Map<String, List<String>> tree, String root) {
        Map<String, String> resolved = new TreeMap<>();        // artefakt → wybrana wersja
        Deque<String> queue = new ArrayDeque<>(tree.getOrDefault(root, List.of()));  // queue = kolejka
        while (!queue.isEmpty()) {
            String node = queue.poll();                         // poll = pobierz z początku
            String[] parts = node.split(":");                   // "artefakt:wersja"
            if (resolved.containsKey(parts[0])) {
                continue;                                       // już wybrany bliżej — tę wersję Maven pomija
            }
            resolved.put(parts[0], parts[1]);
            queue.addAll(tree.getOrDefault(node, List.of()));
        }
        return resolved;
    }

    /**
     * 7. Dodajesz jedną bibliotekę, a dostajesz pięć — bo ona sama ma zależności (przechodnie). Gdy dwie ścieżki
     * prowadzą do RÓŻNYCH wersji tej samej biblioteki, Maven wybiera jedną: najbliższą korzeniowi („nearest wins”).
     */
    static void transitiveConflicts() {
        section("7. Zależności przechodnie i konflikty wersji");

        Map<String, List<String>> tree = new LinkedHashMap<>();
        tree.put("projekt", List.of("A:1.0", "B:1.0"));
        tree.put("A:1.0", List.of("C:1.0"));                    // projekt → A → C 1.0  (głębokość 2)
        tree.put("B:1.0", List.of("D:1.0"));
        tree.put("D:1.0", List.of("C:2.0"));                    // projekt → B → D → C 2.0  (głębokość 3)
        // ./mvnw dependency:tree wypisałby mniej więcej:
        //   pl.kurs:projekt:jar:1.0
        //   +- x:A:jar:1.0:compile
        //   |  \- x:C:jar:1.0:compile
        //   \- x:B:jar:1.0:compile
        //      \- x:D:jar:1.0:compile          (C:2.0 pominięte — konflikt z bliższym C:1.0)
        show("wybrane wersje", resolveNearest(tree, "projekt"));
        // WYNIK: wybrane wersje → {A=1.0, B=1.0, C=1.0, D=1.0}

        // PUŁAPKA: D był pisany pod C 2.0 i woła metodę, której w C 1.0 nie ma → kompilacja przechodzi, a po uruchomieniu
        // NoSuchMethodError. Dlaczego: Maven nie łączy wersji — wybiera JEDNĄ, a niezgodność wychodzi dopiero w runtime.
        // Rozwiązania:
        //   • zadeklaruj C:2.0 bezpośrednio w swoim pom.xml (głębokość 1 = najbliżej, więc wygrywa),
        //   • albo ustal wersję w <dependencyManagement> (centralne „przypięcie” wersji),
        //   • albo <exclusions> (wykluczenia) w zależności, która ciągnie złą wersję,
        //   • maven-enforcer-plugin z regułą dependencyConvergence wykrywa takie konflikty automatycznie.

        Map<String, List<String>> tie = new LinkedHashMap<>();
        tie.put("projekt", List.of("X:1.0", "Y:1.0"));
        tie.put("X:1.0", List.of("L:1.0"));
        tie.put("Y:1.0", List.of("L:2.0"));                     // ta sama głębokość co L:1.0
        show("remis głębokości", resolveNearest(tie, "projekt"));
        // WYNIK: remis głębokości → {L=1.0, X=1.0, Y=1.0}
        // Przy równej głębokości wygrywa zależność zadeklarowana wcześniej (X przed Y) — kolejność w pom.xml ma znaczenie!
        // Gradle robi inaczej: domyślnie wybiera NAJWYŻSZĄ wersję z całego grafu.
    }

    // =================================================================================================
    // 8. PUŁAPKA: ZAKRESY WERSJI
    // =================================================================================================

    /** Porównuje wersje liczbowo po kropkach: 1.10 > 1.9 (tekstowo byłoby odwrotnie!). */
    static final Comparator<String> VERSION_ORDER = (a, b) -> {
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int left = i < x.length ? Integer.parseInt(x[i]) : 0;
            int right = i < y.length ? Integer.parseInt(y[i]) : 0;
            if (left != right) {
                return Integer.compare(left, right);
            }
        }
        return 0;
    };

    /** Najwyższa dostępna wersja w zakresie [low, high) — tak Maven rozwiązuje zakres wersji. */
    static String highestInRange(List<String> available, String low, String highExclusive) {
        return available.stream()
                .filter(v -> VERSION_ORDER.compare(v, low) >= 0 && VERSION_ORDER.compare(v, highExclusive) < 0)
                .max(VERSION_ORDER)
                .orElseThrow();                                 // orElseThrow (Java 10+) = albo rzuć wyjątek
    }

    /**
     * 8. Maven pozwala napisać zakres zamiast wersji, np. [1.0,2.0) = „od 1.0 włącznie do 2.0 wyłącznie”.
     * Brzmi wygodnie, ale wynik budowania zaczyna zależeć od DNIA, w którym budujesz.
     */
    static void versionRangesPitfall() {
        section("8. Pułapka: zakresy wersji");

        List<String> monday = List.of("1.2", "1.3");
        List<String> tuesday = List.of("1.2", "1.3", "1.10");  // autor biblioteki wydał 1.10
        show("poniedziałek, [1.0,2.0)", highestInRange(monday, "1.0", "2.0"));
        // WYNIK: poniedziałek, [1.0,2.0) → 1.3
        show("wtorek, [1.0,2.0)", highestInRange(tuesday, "1.0", "2.0"));
        // WYNIK: wtorek, [1.0,2.0) → 1.10

        // PUŁAPKA: ten sam kod, ten sam pom.xml, inna wersja biblioteki — budowanie przestaje być powtarzalne,
        // a błąd „sam się pojawił”. Dlaczego: zakres wybiera NAJWYŻSZĄ dostępną wersję w chwili budowania.
        // DOBRA PRAKTYKA: zawsze dokładna wersja (5.10.2). Aktualizuj świadomie, np. ./mvnw versions:display-dependency-updates
        // (wtyczka versions pokazuje nowsze wersje) albo przez bota typu Dependabot/Renovate.

        List<String> sortedAsText = new ArrayList<>(tuesday);
        sortedAsText.sort(Comparator.naturalOrder());
        show("sortowanie tekstowe", sortedAsText);
        // WYNIK: sortowanie tekstowe → [1.10, 1.2, 1.3]
        List<String> sortedAsVersions = new ArrayList<>(tuesday);
        sortedAsVersions.sort(VERSION_ORDER);
        show("sortowanie wersji", sortedAsVersions);
        // WYNIK: sortowanie wersji → [1.2, 1.3, 1.10]
        // PUŁAPKA: numerów wersji nie porównuj jak tekstów — "1.10" < "1.2" w porządku alfabetycznym.
    }

    // =================================================================================================
    // 9. MAVEN WRAPPER, ~/.m2, GRADLE I SPRING BOOT
    // =================================================================================================

    /**
     * 9. Wrapper (nakładka) gwarantuje tę samą wersję Mavena u każdego; BOM (Bill of Materials = zestawienie
     * wersji) gwarantuje zgodne wersje bibliotek. Spring Boot korzysta z obu pomysłów.
     */
    static void wrapperGradleSpring() {
        section("9. Wrapper, repozytorium lokalne, Gradle, Spring Boot");

        // MAVEN WRAPPER — pliki w projekcie (ten kurs też je ma):
        //   mvnw (Linux/macOS), mvnw.cmd (Windows), .mvn/wrapper/maven-wrapper.properties (adres i wersja Mavena).
        //   Pierwsze ./mvnw pobiera wskazaną wersję Mavena do ~/.m2/wrapper i potem używa jej zawsze.
        //   Zysk: nie trzeba instalować Mavena globalnie, a wszyscy (i CI) budują TĄ SAMĄ wersją.
        //   Na Windows w PowerShell: .\mvnw.cmd verify    (w IntelliJ: panel Maven → Lifecycle → verify).
        //
        // REPOZYTORIUM LOKALNE: ~/.m2/repository (Windows: C:\Users\<nazwa>\.m2\repository) — pamięć podręczna
        //   pobranych JAR-ów, wspólna dla wszystkich projektów. Ustawienia (np. firmowe repozytorium, proxy):
        //   ~/.m2/settings.xml. Zepsuty plik w repozytorium? Usuń katalog tej biblioteki — Maven pobierze ją ponownie.

        // BOM / dependencyManagement — wersje ustalone w jednym miejscu, zależności bez <version>:
        Map<String, String> managed = new TreeMap<>(Map.of(
                "org.springframework.boot:spring-boot-starter-web", "3.2.5",
                "com.fasterxml.jackson.core:jackson-databind", "2.15.4"));
        show("wersja z BOM", versionFromBom(managed, "com.fasterxml.jackson.core:jackson-databind"));
        // WYNIK: wersja z BOM → 2.15.4
        expectThrows("zależność bez wersji i bez BOM", () -> versionFromBom(managed, "com.h2database:h2"));
        // WYNIK: ✔ zależność bez wersji i bez BOM → rzucono IllegalStateException: brak wersji dla com.h2database:h2
        // Prawdziwy Maven zgłasza podobny błąd: 'dependencies.dependency.version' for com.h2database:h2:jar is missing.

        // SPRING BOOT (t34_toward_spring/Spring04WhatSpringGives):
        //   <parent> spring-boot-starter-parent </parent> — dziedziczy BOM z setkami zgodnych wersji i ustawienia wtyczek,
        //   spring-boot-starter-web — „starter” = jedna zależność, która ciągnie zestaw (Spring MVC, Jackson, Tomcat),
        //   spring-boot-maven-plugin — cel repackage przepakowuje JAR w wykonywalny „gruby” JAR (Build02).
        //   Bez parenta: BOM spring-boot-dependencies dołączony w <dependencyManagement> z <type>pom</type>
        //   i <scope>import</scope>.
        String bomImport = """
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-dependencies</artifactId>
                            <version>3.2.5</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>""";
        show("linie fragmentu z BOM", bomImport.lines().count());   // lines (Java 11+) = strumień linii
        // WYNIK: linie fragmentu z BOM → 11

        // GRADLE — drugie popularne narzędzie (Android, wiele projektów Springa). Ten sam projekt w build.gradle.kts
        // (Kotlin DSL = język opisu w Kotlinie):
        //   plugins { java }
        //   java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
        //   repositories { mavenCentral() }
        //   dependencies {
        //       implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")   // ≈ compile
        //       compileOnly("org.projectlombok:lombok:1.18.30")                        // ≈ provided
        //       annotationProcessor("org.projectlombok:lombok:1.18.30")
        //       runtimeOnly("com.h2database:h2:2.2.224")                               // ≈ runtime
        //       testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")           // ≈ test
        //   }
        //   tasks.test { useJUnitPlatform() }
        // Polecenia: ./gradlew build (też z wrapperem). Gradle: skrypt w kodzie, przyrostowe budowanie i cache — szybszy
        // w dużych projektach; Maven: deklaratywny XML, prostszy i bardzo przewidywalny. Pojęcia (współrzędne, zakresy,
        // Maven Central) są wspólne.
        Map<String, String> gradleNames = new LinkedHashMap<>();
        gradleNames.put("compile", "implementation");
        gradleNames.put("provided", "compileOnly");
        gradleNames.put("runtime", "runtimeOnly");
        gradleNames.put("test", "testImplementation");
        show("Maven → Gradle", gradleNames);
        // WYNIK: Maven → Gradle → {compile=implementation, provided=compileOnly, runtime=runtimeOnly, test=testImplementation}
    }

    /** Wersja z dependencyManagement albo wyjątek (jak Maven, gdy zależność nie ma wersji). */
    static String versionFromBom(Map<String, String> managed, String groupAndArtifact) {
        String version = managed.get(groupAndArtifact);
        if (version == null) {
            throw new IllegalStateException("brak wersji dla " + groupAndArtifact);
        }
        return version;
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • współrzędne: groupId:artifactId:version; -SNAPSHOT = wersja rozwojowa (zmienna), wydanie = niezmienne
     *   • pom.xml: modelVersion, współrzędne, properties (maven.compiler.release, sourceEncoding), dependencies, build/plugins
     *   • układ: src/main/java, src/main/resources, src/test/java, target/; inny układ → sourceDirectory (jak ten kurs)
     *   • cykl życia: validate → compile → test → package → verify → install → deploy; faza uruchamia WSZYSTKIE wcześniejsze
     *   • pracę robią cele wtyczek przypięte do faz (compiler:compile, surefire:test, jar:jar); cel można wołać wprost
     *   • zakresy: compile (domyślny), provided (daje ktoś inny), runtime (sterowniki), test (JUnit)
     *   • konflikt wersji: wygrywa NAJBLIŻSZA deklaracja, przy remisie — wcześniejsza; sprawdzaj ./mvnw dependency:tree
     *   • zakresy wersji [1.0,2.0) psują powtarzalność — pisz dokładne wersje
     *   • ./mvnw (Wrapper) = ta sama wersja Mavena u wszystkich; ~/.m2/repository = lokalna pamięć JAR-ów
     *   • BOM + dependencyManagement = wersje w jednym miejscu (Spring Boot parent / spring-boot-dependencies)
     *   • Gradle: implementation / compileOnly / runtimeOnly / testImplementation; konflikty → najwyższa wersja
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się faza od celu wtyczki? Podaj przykład przypięcia.
     *   2. Co wypisze:  System.out.println(phasesRunFor("verify"));  ?
     *   3. Jaki zakres nadasz: JUnit, sterownik PostgreSQL, Lombok, Jackson? Dlaczego?
     *   4. Co wypisze:  System.out.println(Coordinates.parse("com.acme:util:2.0").repositoryPath());  ?
     *   5. ZNAJDŹ BŁĄD (testy kompilują się i przechodzą, a jednak coś jest nie tak):
     *          <dependency> org.junit.jupiter:junit-jupiter:5.10.2 <scope>runtime</scope> </dependency>
     *   6. Projekt zależy od A (→ C 1.0) i od B (→ D → C 2.0). Która wersja C trafi do programu i jaki błąd może
     *      pojawić się po uruchomieniu? Jak to naprawić?
     *   7. ZNAJDŹ BŁĄD:  <version>[1.0,)</version>  w zależności aplikacji produkcyjnej. Co jest nie tak?
     *   8. Po co projekt trzyma w repozytorium pliki mvnw i mvnw.cmd?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: ścieżka w repozytorium", "com/h2database/h2/2.2.224/h2-2.2.224.jar",
                () -> exercise1("com.h2database:h2:2.2.224"));
        Check.equal("ćw. 2: fazy dla test", List.of("validate", "compile", "test"), () -> exercise2("test"));
        Check.throwsException("ćw. 2: nieznana faza", IllegalArgumentException.class, () -> exercise2("run"));
        Check.equal("ćw. 3: zależności testowe z POM", List.of("org.junit.jupiter:junit-jupiter"),
                () -> exercise3(POM, "test"));
        Check.equal("ćw. 3: zależności compile z POM", List.of("com.fasterxml.jackson.core:jackson-databind"),
                () -> exercise3(POM, "compile"));
        Check.equal("ćw. 4: najbliższa wersja wygrywa", Map.of("A", "1.0", "B", "1.0", "C", "1.0", "D", "1.0"),
                () -> exercise4(exerciseTree(), "projekt"));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1 (wzorzec)", "com/h2database/h2/2.2.224/h2-2.2.224.jar",
                () -> solution1("com.h2database:h2:2.2.224"));
        Check.equal("ćw. 2 (wzorzec)", List.of("validate", "compile", "test"), () -> solution2("test"));
        Check.throwsException("ćw. 2 (wzorzec): nieznana faza", IllegalArgumentException.class, () -> solution2("run"));
        Check.equal("ćw. 3 (wzorzec): test", List.of("org.junit.jupiter:junit-jupiter"), () -> solution3(POM, "test"));
        Check.equal("ćw. 3 (wzorzec): compile", List.of("com.fasterxml.jackson.core:jackson-databind"),
                () -> solution3(POM, "compile"));
        Check.equal("ćw. 4 (wzorzec)", Map.of("A", "1.0", "B", "1.0", "C", "1.0", "D", "1.0"),
                () -> solution4(exerciseTree(), "projekt"));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 6 OK, ✘ 0 BŁĄD
    }

    /** Drzewo do ćwiczenia 4: C 2.0 jest głębiej niż C 1.0. */
    static Map<String, List<String>> exerciseTree() {
        Map<String, List<String>> tree = new LinkedHashMap<>();
        tree.put("projekt", List.of("B:1.0", "A:1.0"));
        tree.put("B:1.0", List.of("D:1.0"));
        tree.put("D:1.0", List.of("C:2.0"));
        tree.put("A:1.0", List.of("C:1.0"));
        return tree;
    }

    /**
     * ĆWICZENIE 1 (łatwe): zamień współrzędne "groupId:artifactId:version" na ścieżkę pliku JAR w lokalnym
     * repozytorium, np. "a.b:c:1.0" → "a/b/c/1.0/c-1.0.jar".
     * Podpowiedź: split(":"), kropki w groupId zamień na "/" (replace).
     */
    static String exercise1(String coordinates) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć listę faz, które wykona ./mvnw faza (od validate do podanej włącznie, lista PHASES).
     * Nieznana faza → IllegalArgumentException.
     * Podpowiedź: indexOf + subList; indexOf zwraca -1, gdy elementu nie ma.
     */
    static List<String> exercise2(String phase) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie, łączy z t18_io_files/Io14Xml i t16_streams): z tekstu pom.xml zwróć posortowaną listę
     * "groupId:artifactId" zależności o podanym zakresie. Brak elementu scope oznacza "compile".
     * Podpowiedź: parseSecurely(pom), getElementsByTagName("dependency"), childText(...).
     */
    static List<String> exercise3(String pom, String scope) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): napisz SAMODZIELNIE (bez resolveNearest) rozwiązywanie wersji „najbliższa wygrywa”.
     * Klucz mapy tree to węzeł ("projekt" albo "artefakt:wersja"), wartość — jego bezpośrednie zależności.
     * Wynik: artefakt → wersja (TreeMap). Przy tej samej głębokości wygrywa wcześniej zadeklarowana zależność.
     * Podpowiedź: kolejka ArrayDeque, pętla po poziomach; nie wchodź głębiej w pominiętą wersję.
     */
    static Map<String, String> exercise4(Map<String, List<String>> tree, String root) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(String coordinates) {
        String[] p = coordinates.split(":");
        return p[0].replace('.', '/') + "/" + p[1] + "/" + p[2] + "/" + p[1] + "-" + p[2] + ".jar";
    }

    static List<String> solution2(String phase) {
        int index = PHASES.indexOf(phase);
        if (index < 0) {
            throw new IllegalArgumentException("nieznana faza: " + phase);
        }
        return List.copyOf(PHASES.subList(0, index + 1));
    }

    static List<String> solution3(String pom, String scope) {
        NodeList nodes = parseSecurely(pom).getElementsByTagName("dependency");
        List<String> result = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element dep = (Element) nodes.item(i);
            String depScope = childText(dep, "scope");
            if (scope.equals(depScope == null ? "compile" : depScope)) {
                result.add(childText(dep, "groupId") + ":" + childText(dep, "artifactId"));
            }
        }
        return result.stream().sorted().toList();              // toList (Java 16+) = do niezmiennej listy
    }

    static Map<String, String> solution4(Map<String, List<String>> tree, String root) {
        Map<String, String> resolved = new TreeMap<>();
        Deque<String> queue = new ArrayDeque<>(tree.getOrDefault(root, List.of()));
        while (!queue.isEmpty()) {
            String node = queue.poll();
            String artifact = node.substring(0, node.indexOf(':'));
            if (resolved.putIfAbsent(artifact, node.substring(node.indexOf(':') + 1)) == null) {
                queue.addAll(tree.getOrDefault(node, List.of()));   // tylko zwycięzca wnosi swoje zależności
            }
        }
        return resolved;
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Faza to krok cyklu życia (np. test), cel to konkretne zadanie wtyczki (np. surefire:test). Cele są przypięte
     *      do faz: compile → compiler:compile, package → jar:jar. Fazę uruchamiasz z całą „historią”, cel — samodzielnie.
     *   2. [validate, compile, test, package, verify]
     *   3. JUnit → test, PostgreSQL → runtime (kod używa tylko java.sql), Lombok → provided (działa przy kompilacji,
     *      nie jest potrzebny w działającym programie), Jackson → compile (używasz jego klas i potrzebujesz ich w runtime).
     *   4. com/acme/util/2.0/util-2.0.jar
     *   5. Zakres runtime jest widoczny w testach (dlatego działają), ale także w działającej aplikacji: JUnit trafia
     *      do jej ścieżki klas (np. do grubego JAR-a) i przechodzi dalej do projektów, które zależą od naszego.
     *      Poprawnie: <scope>test</scope> — tylko testy, bez pakowania i bez przechodniości.
     *   6. C 1.0 — jest bliżej (głębokość 2 vs 3). Jeśli D woła metodę dodaną w C 2.0 → NoSuchMethodError w runtime.
     *      Naprawa: zadeklaruj C 2.0 bezpośrednio albo w dependencyManagement; sprawdź ./mvnw dependency:tree.
     *   7. Zakres otwarty = „najnowsza dostępna wersja” — budowanie zmienia się samo, gdy autor wyda nową wersję
     *      (także niezgodną). Trzeba podać dokładną wersję i aktualizować ją świadomie.
     *   8. Wrapper pobiera i uruchamia określoną wersję Mavena — nie trzeba go instalować, a wszyscy (także CI)
     *      budują identycznie. mvnw dla Linux/macOS, mvnw.cmd dla Windows.
     */
    // </editor-fold>
}
