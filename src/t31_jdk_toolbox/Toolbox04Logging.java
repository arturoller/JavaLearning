package t31_jdk_toolbox;

import helpers.Check;
import helpers.TempDir;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import static helpers.Console.*;

/**
 * <pre>
 * TEMAT: Logowanie w JDK — System.Logger i java.util.logging (JUL) w praktyce
 *        (logger = dziennik zdarzeń; handler = obsługa wyjścia; formatter = układ linii;
 *         filter = sito; MDC = kontekst przypięty do wątku)
 *
 * W SKRÓCIE:
 *   Lekcja t18_io_files/Io10SimpleLogger pokazała, jak działa prosty logger "od zera". Tu poznajesz
 *   to, co jest już w JDK: interfejs System.Logger (Java 9+), jego domyślne zaplecze (JUL),
 *   konfigurację (kod i plik logging.properties), hierarchię loggerów, filtry, własny format linii,
 *   kontekst wątku (MDC), logi strukturalne (JSON) i zasady wydajności. Na końcu: jak to się ma
 *   do SLF4J + Logback, czyli do tego, co działa w Spring Boot.
 *
 * ANALOGIA:
 *   Logger to dziennik pokładowy. Kapitan (kod) wpisuje zdarzenia z ważnością (od "ciekawostka"
 *   do "pożar"). Oficer wachtowy (handler) decyduje, gdzie dziennik leży: na ekranie, w pliku.
 *   Filtr to bosman, który nie dopuszcza wpisów typu "kontrola czystości". Formatter to wzór
 *   kartki: kolejność rubryk i sposób zapisu. MDC to pieczątka "rejs nr 42" przybita na każdej
 *   kartce wątku, żeby później odnaleźć wszystkie wpisy jednej sprawy.
 *
 * JAK TO DZIAŁA:
 *   kod:  System.getLogger("t31.app.db").log(Level.INFO, "Połączono z {0}", "baza")
 *           │
 *           ▼  (System.Logger → zaplecze; domyślnie java.util.logging)
 *   Logger "t31.app.db"  ── poziom? (własny albo odziedziczony) ── filtr loggera ──┐
 *           │ nie przeszedł → koniec                                                  │
 *           ▼                                                                         │
 *   handlery tego loggera, potem handlery RODZICÓW ("t31.app", "t31", root)  ◄───────┘
 *           │  każdy handler ma WŁASNY poziom i filtr, a formatter układa linię
 *           ▼
 *   System.out / plik
 *   Dwa progi: poziom loggera ORAZ poziom handlera — wiadomość musi przejść oba.
 *   Poziomy (rosnąco): TRACE, DEBUG, INFO, WARNING, ERROR  (w JUL: FINER, FINE, INFO, WARNING, SEVERE).
 *
 * SŁÓWKA:
 *   level = poziom; severity = ważność; handler = obsługa wyjścia; formatter = układ linii;
 *   filter = filtr (sito); hierarchy = hierarchia; inherit = dziedziczyć; supplier = dostawca
 *   (leniwie liczona wartość); context = kontekst; structured = strukturalny; guard = strażnik.
 *
 * ZOBACZ TEŻ: t18_io_files/Io10SimpleLogger (logger od zera),
 *   t10_exceptions/Exceptions07BestPractices (loguj albo rzucaj),
 *   t21_concurrency/Concurrency08ThreadSafetyPatterns (ThreadLocal),
 *   t30_build_modules/Build03Modules (moduł java.logging),
 *   t34_toward_spring/Spring04WhatSpringGives (Spring Boot i Logback),
 *   t31_jdk_toolbox/Toolbox03I18n (MessageFormat i apostrofy)
 * </pre>
 */
public class Toolbox04Logging {

    /** Stały moment do demonstracji formatera (zamiast now()) — wynik zawsze taki sam. */
    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-15T10:30:00Z");

    // Loggery JUL trzymamy w polach statycznych. Dlaczego: LogManager przechowuje je SŁABO —
    // logger bez żadnego odwołania może zostać zebrany przez odśmiecacz i straci ustawiony poziom.
    private static final Logger APP = Logger.getLogger("t31.app");
    private static final Logger APP_DB = Logger.getLogger("t31.app.db");
    private static final Logger APP_WEB = Logger.getLogger("t31.app.web");
    private static final Logger EX_ROOT = Logger.getLogger("t31.ex");
    private static final Logger EX_A = Logger.getLogger("t31.ex.a");
    private static final Logger EX_AB = Logger.getLogger("t31.ex.a.b");
    private static final Logger EX_C = Logger.getLogger("t31.ex.c");

    public static void main(String[] args) throws IOException {
        title("Toolbox04 — logowanie w JDK");
        setupStdout(); // setup = przygotowanie: logi trafiają na System.out, NIE na System.err
        try {
            systemLogger();        // System.Logger = interfejs logowania z JDK
            julConfiguration();    // JUL configuration = konfiguracja w kodzie
            loggingProperties();   // logging.properties = konfiguracja z pliku
            hierarchy();           // hierarchy = hierarchia i dziedziczenie poziomu
            filters();             // filters = filtry
            formatters();          // formatters = własny format linii
            mdcContext();          // MDC = kontekst wątku
            structuredLogging();   // structured logging = logi strukturalne
            levelsAndPerformance(); // levels and performance = poziomy i wydajność
            logbackInSpring();     // Logback in Spring = Logback w Springu
            exercises();           // exercises = ćwiczenia
        } finally {
            LogManager.getLogManager().reset(); // reset = zamknij handlery i przywróć stan początkowy
        }
    }

    // =================================================================================================
    // 1. SYSTEM.LOGGER
    // =================================================================================================

    /**
     * 1. {@code System.getLogger(nazwa)} (get logger = daj logger) zwraca interfejs
     * {@code System.Logger} (Java 9+, JEP 264). Biblioteka JDK i Twój kod mogą logować bez
     * zależności od konkretnej biblioteki logowania — zaplecze wybiera środowisko.
     */
    static void systemLogger() {
        section("1. System.Logger: poziomy, parametry, leniwe komunikaty");

        System.Logger logger = System.getLogger("t31.demo"); // nazwa loggera = miejsce w hierarchii
        show("nazwa loggera", logger.getName());
        // WYNIK: nazwa loggera → t31.demo

        // Poziomy System.Logger.Level i ich "ważność" (severity). Są takie same jak liczby poziomów JUL.
        List<String> levels = new ArrayList<>();
        for (System.Logger.Level level : System.Logger.Level.values()) {
            levels.add(level.getName() + "=" + level.getSeverity());
        }
        show("poziomy (nazwa=ważność)", levels);
        // WYNIK: poziomy (nazwa=ważność) → [ALL=-2147483648, TRACE=400, DEBUG=500, INFO=800, WARNING=900, ERROR=1000, OFF=2147483647]
        // ALL = wszystko, OFF = nic. W JUL: TRACE → FINER, DEBUG → FINE, ERROR → SEVERE.
        // Dlatego w liniach poniżej zobaczysz "FINE" dla DEBUG i "SEVERE" dla ERROR.

        // Domyślnie poziom korzenia to INFO, więc poniższe DEBUG jest odrzucone, a INFO przechodzi.
        show("czy DEBUG jest włączony", logger.isLoggable(System.Logger.Level.DEBUG));
        // WYNIK: czy DEBUG jest włączony → false
        logger.log(System.Logger.Level.DEBUG, "to nie zostanie wypisane");
        logger.log(System.Logger.Level.INFO, "Aplikacja wystartowała");
        // WYNIK: INFO t31.demo: Aplikacja wystartowała
        logger.log(System.Logger.Level.WARNING, "Mało miejsca na dysku");
        // WYNIK: WARNING t31.demo: Mało miejsca na dysku

        // Parametry w stylu MessageFormat: {0}, {1}. Tekst składany dopiero, gdy poziom jest włączony.
        logger.log(System.Logger.Level.INFO, "Użytkownik {0} zalogował się z adresu {1}", "ala", "10.0.0.5");
        // WYNIK: INFO t31.demo: Użytkownik ala zalogował się z adresu 10.0.0.5

        // Wyjątek jako ostatni argument: log(poziom, komunikat, throwable).
        logger.log(System.Logger.Level.ERROR, "Nie udało się zapisać", new IllegalStateException("dysk pełny"));
        // WYNIK: SEVERE t31.demo: Nie udało się zapisać | wyjątek: IllegalStateException: dysk pełny
        // (nasz formatter wypisuje tylko typ i komunikat wyjątku; domyślny SimpleFormatter z JDK
        // dopisałby też pełny ślad stosu — w prawdziwych logach to właśnie on jest najcenniejszy)

        // Zmiana poziomu: loggerem rządzi zaplecze (JUL), więc ustawiamy go po stronie JUL.
        // System.getLogger("t31.demo") i Logger.getLogger("t31.demo") to ten sam logger.
        Logger julLogger = Logger.getLogger("t31.demo");
        julLogger.setLevel(Level.FINE);
        show("czy DEBUG jest teraz włączony", logger.isLoggable(System.Logger.Level.DEBUG));
        // WYNIK: czy DEBUG jest teraz włączony → true
        logger.log(System.Logger.Level.DEBUG, "teraz DEBUG jest widoczny");
        // WYNIK: FINE t31.demo: teraz DEBUG jest widoczny
        logger.log(System.Logger.Level.TRACE, "a TRACE jeszcze nie");
        julLogger.setLevel(null); // null = dziedzicz po rodzicu (sekcja 4)

        // PUŁAPKA: parametry {0} w System.Logger/JUL są szablonem MessageFormat, więc działa tu ta sama
        // pułapka z apostrofem co w t31_jdk_toolbox/Toolbox03I18n: pojedynczy apostrof zaczyna cytowanie.
        // Dlaczego: formatowanie wykonuje MessageFormat, ale tylko gdy podasz parametry.
        logger.log(System.Logger.Level.INFO, "Użytkownik O'Brien: {0}", "x");
        // WYNIK: INFO t31.demo: Użytkownik OBrien: {0}
        logger.log(System.Logger.Level.INFO, "Użytkownik O'Brien");
        // WYNIK: INFO t31.demo: Użytkownik O'Brien
        // Bez parametrów tekst idzie dosłownie. Z parametrami apostrof znika, a {0} zostaje
        // nierozwinięte. Zasada: w szablonie z {0} podwajaj apostrofy (''). Liczby w parametrach
        // też są formatowane według języka komputera (separator tysięcy) — duże liczby i numery
        // przekazuj jako tekst.

        // KTO TO NAPRAWDĘ ZAPISUJE (zaplecze, czyli "backend"):
        //   • domyślnie, gdy w środowisku jest moduł java.logging: java.util.logging (JUL) —
        //     w naszym programie logger.getClass() to wewnętrzna otoczka na JUL,
        //   • gdy modułu java.logging nie ma (mocno odchudzony obraz jlink): prosty logger
        //     piszący na System.err,
        //   • można podmienić zaplecze przez usługę System.LoggerFinder: np. artefakt
        //     slf4j-jdk-platform-logging kieruje System.Logger do SLF4J (a stąd do Logbacka),
        //     a log4j-jpl do Log4j 2. Biblioteki piszące przez System.Logger stają się wtedy
        //     częścią Twojej konfiguracji logowania.

        // DOBRA PRAKTYKA: kod biblioteczny loguj przez System.Logger lub SLF4J — nie ustawiaj w nim
        // handlerów ani poziomów. Dlaczego: konfigurację logowania ma posiadać aplikacja, a nie
        // biblioteka; inaczej dwie biblioteki będą się przepychać o ustawienia.
    }

    // =================================================================================================
    // 2. JUL — KONFIGURACJA W KODZIE
    // =================================================================================================

    /**
     * 2. {@code java.util.logging.Logger} (JUL) ma poziom, handlery i filtr. Wiadomość musi przejść
     * poziom loggera ORAZ poziom handlera — dwa oddzielne progi.
     */
    static void julConfiguration() {
        section("2. JUL w kodzie: poziom loggera i poziom handlera");

        Logger logger = Logger.getLogger("t31.demo.config");
        logger.setUseParentHandlers(false); // false = nie przekazuj rodzicom (żeby nie dublować linii)
        StdoutHandler handler = new StdoutHandler("lokalny"); // znacznik [lokalny] w linii
        logger.addHandler(handler);

        logger.setLevel(Level.FINE);   // próg loggera: FINE i wyżej
        handler.setLevel(Level.INFO);  // próg handlera: INFO i wyżej
        logger.fine("FINE: logger przepuszcza, handler odrzuca");
        logger.info("INFO: przechodzi oba progi");
        // WYNIK: [lokalny] INFO t31.demo.config: INFO: przechodzi oba progi
        // PUŁAPKA: "ustawiłem FINE, a nic nie widać". Dlaczego: domyślne handlery (np. ConsoleHandler)
        // mają poziom INFO. Żeby zobaczyć FINE, trzeba obniżyć OBA progi: loggera i handlera.
        handler.setLevel(Level.ALL);
        logger.fine("FINE: teraz widać");
        // WYNIK: [lokalny] FINE t31.demo.config: FINE: teraz widać

        // Wygodne metody poziomów: severe, warning, info, config, fine, finer, finest. Są też wersje
        // z dostawcą: logger.fine(() -> "tekst liczony tylko, gdy FINE jest włączony").
        logger.warning("ostrzeżenie");
        // WYNIK: [lokalny] WARNING t31.demo.config: ostrzeżenie
        logger.log(Level.SEVERE, "błąd z wyjątkiem", new RuntimeException("awaria"));
        // WYNIK: [lokalny] SEVERE t31.demo.config: błąd z wyjątkiem | wyjątek: RuntimeException: awaria

        // PUŁAPKA: dublowanie linii. Dlaczego: logger domyślnie przekazuje wpisy handlerom RODZICÓW
        // (useParentHandlers = true), a root ma swój handler. Dodanie handlera do loggera BEZ
        // wyłączenia przekazywania daje każdą linię dwa razy.
        logger.setUseParentHandlers(true);
        logger.info("dwa razy: raz lokalnie, raz przez korzeń");
        // WYNIK: [lokalny] INFO t31.demo.config: dwa razy: raz lokalnie, raz przez korzeń
        // WYNIK: INFO t31.demo.config: dwa razy: raz lokalnie, raz przez korzeń
        logger.setUseParentHandlers(false);

        // DOBRA PRAKTYKA: handlery dodawaj tylko do korzenia (w jednym miejscu konfiguracji),
        // a loggery w kodzie tylko tworz i używaj. Dlaczego: łatwo zmienić gdzie trafiają logi
        // bez dotykania klas z logiką.

        // PUŁAPKA: LogManager trzyma loggery słabo (patrz pola statyczne na górze pliku).
        // Dlaczego: jeśli w metodzie zrobisz Logger.getLogger("x").setLevel(FINE) i nie zachowasz
        // referencji, odśmiecacz może usunąć logger — wraz z ustawionym poziomem.

        // Pozostałe domyślne zachowania JUL, o których warto wiedzieć:
        //   • korzeń ma ConsoleHandler piszący na System.ERR (nie na System.out!) z poziomem INFO,
        //   • SimpleFormatter pisze dwie linie na wpis (data i treść),
        //   • FileHandler bez formatera używa XMLFormatter — plik XML, a nie tekst.
        // Dlatego ta lekcja podmienia handlery na własne, piszące na System.out.
        logger.removeHandler(handler);
        handler.close();
    }

    // =================================================================================================
    // 3. LOGGING.PROPERTIES
    // =================================================================================================

    /** Szablon pliku konfiguracyjnego; %DIR% zostanie zamieniony na katalog tymczasowy. */
    private static final String PROPERTIES_TEMPLATE = """
            handlers = java.util.logging.FileHandler
            .level = WARNING
            t31.file.level = FINE
            java.util.logging.FileHandler.pattern = %DIR%/aplikacja.log
            java.util.logging.FileHandler.limit = 0
            java.util.logging.FileHandler.count = 1
            java.util.logging.FileHandler.append = false
            java.util.logging.FileHandler.encoding = UTF-8
            java.util.logging.FileHandler.level = ALL
            java.util.logging.FileHandler.formatter = t31_jdk_toolbox.Toolbox04Logging$LineFormatter
            """;

    /**
     * 3. Konfigurację można wczytać z pliku tekstowego w formacie {@code .properties}:
     * {@code LogManager.readConfiguration(InputStream)}. W prawdziwej aplikacji podajesz plik
     * parametrem JVM: {@code -Djava.util.logging.config.file=logging.properties}.
     */
    static void loggingProperties() throws IOException {
        section("3. logging.properties: konfiguracja z tekstu, zapis do pliku");

        // Text block (blok tekstowy, Java 15+) ułatwia wklejenie konfiguracji w kodzie.
        for (String line : PROPERTIES_TEMPLATE.lines().toList()) { // lines = wiersze (Java 11+)
            note(line);
        }
        // WYNIK: ℹ handlers = java.util.logging.FileHandler
        // WYNIK: ℹ .level = WARNING
        // WYNIK: ℹ t31.file.level = FINE
        // WYNIK: ℹ java.util.logging.FileHandler.pattern = %DIR%/aplikacja.log
        // WYNIK: ℹ java.util.logging.FileHandler.limit = 0
        // WYNIK: ℹ java.util.logging.FileHandler.count = 1
        // WYNIK: ℹ java.util.logging.FileHandler.append = false
        // WYNIK: ℹ java.util.logging.FileHandler.encoding = UTF-8
        // WYNIK: ℹ java.util.logging.FileHandler.level = ALL
        // WYNIK: ℹ java.util.logging.FileHandler.formatter = t31_jdk_toolbox.Toolbox04Logging$LineFormatter
        // Znaczenie linii:
        //   handlers                     — lista handlerów korzenia,
        //   .level                       — poziom korzenia (kropka bez nazwy = root),
        //   t31.file.level               — poziom loggera "t31.file" i jego potomków,
        //   ...FileHandler.pattern       — ścieżka pliku (w .properties używaj ukośników /, bo
        //                                  odwrotny ukośnik to znak ucieczki; to ważne w Windows),
        //   limit = 0 / count = 1        — bez rotacji (limit rozmiaru 0 = brak limitu),
        //   formatter                    — pełna nazwa klasy (musi być publiczna i mieć publiczny
        //                                  konstruktor bez argumentów; klasę zagnieżdżoną piszemy z $).

        Path dir = TempDir.create("tool-log");
        try {
            String config = PROPERTIES_TEMPLATE.replace("%DIR%", dir.toString().replace('\\', '/'));
            // readConfiguration czyta strumień bajtów w ISO-8859-1 — tu konfiguracja ma tylko ASCII.
            LogManager.getLogManager().readConfiguration(
                    new ByteArrayInputStream(config.getBytes(StandardCharsets.ISO_8859_1)));

            Logger file = Logger.getLogger("t31.file");
            Logger fileChild = Logger.getLogger("t31.file.child");
            Logger other = Logger.getLogger("inny.pakiet");
            file.fine("zapis FINE (poziom z pliku konfiguracyjnego)");
            fileChild.info("zapis INFO z potomka loggera");
            other.info("INFO spoza t31.file — odrzucone, bo korzeń ma poziom WARNING");
            other.warning("ostrzeżenie spoza t31.file — zapisane");

            LogManager.getLogManager().reset(); // zamyka plik i zwalnia go (ważne przed usunięciem!)

            List<String> lines = Files.readAllLines(dir.resolve("aplikacja.log"), StandardCharsets.UTF_8);
            showEach("linie w pliku", lines);
            // WYNIK: linie w pliku (liczba elementów: 3):
            // WYNIK: • FINE t31.file: zapis FINE (poziom z pliku konfiguracyjnego)
            // WYNIK: • INFO t31.file.child: zapis INFO z potomka loggera
            // WYNIK: • WARNING inny.pakiet: ostrzeżenie spoza t31.file — zapisane
            show("pliki w katalogu", listNames(dir));
            // WYNIK: pliki w katalogu → [aplikacja.log]

            // PUŁAPKA: usunięcie pliku logu, zanim handler zostanie zamknięty.
            // Dlaczego: otwarty plik jest zablokowany (w Windows nie da się go skasować),
            // a FileHandler tworzy dodatkowy plik blokady .lck. reset() / close() zamykają handlery
            // i sprzątają blokadę. Dlatego w finally najpierw reset(), potem kasowanie katalogu.
        } finally {
            LogManager.getLogManager().reset();
            TempDir.deleteRecursively(dir);
            setupStdout(); // wracamy do konfiguracji "System.out" (reset zdjął nasz handler)
        }

        // PUŁAPKA: reset() (i readConfiguration, które wywołuje reset) zeruje poziomy WSZYSTKICH loggerów.
        // Dlaczego: LogManager przywraca stan początkowy: root = INFO, reszta dziedziczy.
        // Ustawienia poziomów w kodzie rób PO wczytaniu konfiguracji, nie przed.

        // PUŁAPKA: domyślny wzorzec pliku FileHandler to "%h/java%u.log" — katalog domowy użytkownika.
        // Dlaczego: bez jawnego pattern aplikacja zaśmieca katalog domowy plikami java0.log, java1.log.
        // Zawsze podawaj pattern (a w serwerach rotację: limit i count, np. 10 MB i 5 plików).

        // DOBRA PRAKTYKA: w kodzie ustaw tylko to, co nie zależy od środowiska; poziomy i miejsce zapisu
        // trzymaj w pliku konfiguracyjnym. Dlaczego: na produkcji możesz zwiększyć szczegółowość
        // logów bez rekompilacji i bez wdrażania nowej wersji.
    }

    /** Nazwy plików w katalogu, posortowane (do deterministycznego wydruku). */
    private static List<String> listNames(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            return stream.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    // =================================================================================================
    // 4. HIERARCHIA LOGGERÓW
    // =================================================================================================

    /**
     * 4. Nazwy loggerów tworzą drzewo wg kropek: {@code t31.app.db} jest dzieckiem {@code t31.app}.
     * Logger bez własnego poziomu dziedziczy poziom najbliższego przodka, który go ma.
     * Dlatego loggery nazywa się pełną nazwą klasy: {@code Logger.getLogger(Klasa.class.getName())}.
     */
    static void hierarchy() {
        section("4. Hierarchia loggerów i dziedziczenie poziomu");

        show("rodzic t31.app.db", APP_DB.getParent().getName());
        // WYNIK: rodzic t31.app.db → t31.app
        show("rodzic t31.app (t31 nie istnieje, więc korzeń)", "'" + APP.getParent().getName() + "'");
        // WYNIK: rodzic t31.app (t31 nie istnieje, więc korzeń) → ''
        show("własny poziom t31.app.db (null = dziedziczy)", APP_DB.getLevel());
        // WYNIK: własny poziom t31.app.db (null = dziedziczy) → null

        APP_DB.fine("najpierw: FINE odrzucone (dziedziczy INFO z korzenia)");
        APP_DB.info("najpierw: INFO przechodzi");
        // WYNIK: INFO t31.app.db: najpierw: INFO przechodzi

        APP.setLevel(Level.FINE); // ustawiamy poziom RODZICA — dzieci dziedziczą
        show("własny poziom t31.app.db po zmianie rodzica", APP_DB.getLevel());
        // WYNIK: własny poziom t31.app.db po zmianie rodzica → null
        APP_DB.fine("potem: FINE potomka widoczne, bo rodzic ma FINE");
        APP_WEB.fine("potem: FINE w t31.app.web też");
        // WYNIK: FINE t31.app.db: potem: FINE potomka widoczne, bo rodzic ma FINE
        // WYNIK: FINE t31.app.web: potem: FINE w t31.app.web też

        APP_DB.setLevel(Level.WARNING); // własny poziom potomka przesłania poziom rodzica
        APP_DB.info("INFO w t31.app.db odrzucone (własny poziom WARNING)");
        APP_DB.warning("WARNING w t31.app.db przechodzi");
        APP_WEB.fine("t31.app.web nadal dziedziczy FINE po rodzicu");
        // WYNIK: WARNING t31.app.db: WARNING w t31.app.db przechodzi
        // WYNIK: FINE t31.app.web: t31.app.web nadal dziedziczy FINE po rodzicu

        // Sprawdzenie bez logowania: isLoggable(poziom) uwzględnia poziom własny lub odziedziczony.
        show("t31.app.db: INFO włączone", APP_DB.isLoggable(Level.INFO));
        // WYNIK: t31.app.db: INFO włączone → false
        show("t31.app.web: FINE włączone", APP_WEB.isLoggable(Level.FINE));
        // WYNIK: t31.app.web: FINE włączone → true

        // Zachowaj porządek: kasujemy ustawienia z tej sekcji.
        APP.setLevel(null);
        APP_DB.setLevel(null);

        // PUŁAPKA: getLevel() zwraca null dla loggera, który dziedziczy — to NIE znaczy "wyłączony".
        // Dlaczego: null = "weź od przodka". Chcąc poznać poziom EFEKTYWNY, trzeba iść w górę po
        // getParent(), aż trafisz na poziom różny od null (ćwiczenie 2), albo użyć isLoggable.

        // PUŁAPKA: literówka w nazwie loggera ("t31.aap") tworzy NOWY logger bez ustawień.
        // Dlaczego: Logger.getLogger nie sprawdza, czy nazwa "istnieje" — po prostu go zakłada.
        // Dlatego nazwy bierz z klasy: Logger.getLogger(OrderService.class.getName()).

        // DOBRA PRAKTYKA: jedna stała logger na klasę, nazwa = pełna nazwa klasy. Dlaczego:
        // poziomem całego pakietu sterujesz jedną linijką (t31.app.level = FINE), a w logu od razu
        // widać, z której klasy pochodzi wpis.
    }

    // =================================================================================================
    // 5. FILTRY
    // =================================================================================================

    /**
     * 5. {@code Filter} (filtr) to funkcja {@code LogRecord → boolean}: {@code true} = przepuść.
     * Można go założyć na loggerze lub na handlerze. Nadaje się do odrzucania "szumu".
     */
    static void filters() {
        section("5. Filtry: odrzucanie szumu");

        Logger logger = Logger.getLogger("t31.demo.filter");
        // Filter ma jedną metodę (isLoggable), więc to interfejs funkcyjny — wystarczy lambda.
        Filter noHealthChecks = record -> !record.getMessage().contains("health-check");
        logger.setFilter(noHealthChecks);
        logger.info("GET /health-check 200");
        logger.info("GET /zamowienia 200");
        // WYNIK: INFO t31.demo.filter: GET /zamowienia 200
        // Pierwsza linia została odrzucona przez filtr, druga trafiła do wyjścia.

        // Filtr na handlerze działa niezależnie od filtra loggera: sprawdza wszystko, co do niego dotrze.
        AtomicInteger seen = new AtomicInteger(); // AtomicInteger = licznik bezpieczny wątkowo
        Handler counting = new StdoutHandler("licznik");
        counting.setFilter(record -> {
            seen.incrementAndGet(); // efekt uboczny w filtrze — tylko do demonstracji
            return record.getLevel().intValue() >= Level.WARNING.intValue();
        });
        logger.setUseParentHandlers(false);
        logger.addHandler(counting);
        logger.info("INFO — filtr handlera odrzuci");
        logger.warning("WARNING — przejdzie");
        // WYNIK: [licznik] WARNING t31.demo.filter: WARNING — przejdzie
        show("ile wpisów obejrzał filtr handlera", seen.get());
        // WYNIK: ile wpisów obejrzał filtr handlera → 2
        logger.removeHandler(counting);
        logger.setFilter(null);
        logger.setUseParentHandlers(true);

        // PUŁAPKA: filtr jest wołany dla KAŻDEGO wpisu, który przeszedł poziom — musi być szybki
        // i bezpieczny wątkowo. Dlaczego: logowanie jest na gorącej ścieżce programu; wolny filtr
        // (np. zapytanie do bazy) spowolni całą aplikację. Wyjątek w filtrze też nie powinien wyciekać.

        // PUŁAPKA: filtr NIE jest dobrym miejscem na zmianę treści (np. maskowanie haseł).
        // Dlaczego: ten sam obiekt LogRecord trafia do wielu handlerów; zmiana wpływa na
        // wszystkie. Maskuj w formaterze (sekcja 6), który tworzy NOWY tekst.

        // DOBRA PRAKTYKA: odrzucanie szumu (health-check, ping) lepiej robić poziomem (DEBUG dla
        // takich wpisów) niż filtrem. Dlaczego: poziomem steruje konfiguracja, filtr siedzi w kodzie.
    }

    // =================================================================================================
    // 6. FORMATTER
    // =================================================================================================

    /**
     * 6. {@code Formatter} zamienia {@code LogRecord} w tekst linii. Poniższy {@code TimestampFormatter}
     * dopisuje czas w formacie ISO i maskuje hasła. Czas bierzemy z samego wpisu
     * ({@code record.getInstant()}), więc w demonstracji ustawiamy go ręcznie na stały moment.
     */
    static void formatters() {
        section("6. Własny Formatter: czas ISO, treść, wyjątek, maskowanie");

        TimestampFormatter formatter = new TimestampFormatter();

        LogRecord record = new LogRecord(Level.INFO, "Zalogowano użytkownika {0}"); // record = wpis
        record.setLoggerName("t31.app");
        record.setParameters(new Object[] {"ala"});
        record.setInstant(FIXED_INSTANT); // setInstant (Java 9+) — ręcznie ustawiony stały czas
        show("wpis z parametrem", formatter.format(record).trim());
        // WYNIK: wpis z parametrem → 2026-01-15T10:30:00Z INFO t31.app: Zalogowano użytkownika ala

        LogRecord failure = new LogRecord(Level.SEVERE, "Błąd zapisu zamówienia");
        failure.setLoggerName("t31.app.db");
        failure.setInstant(FIXED_INSTANT);
        failure.setThrown(new IllegalStateException("brak połączenia"));
        show("wpis z wyjątkiem", formatter.format(failure).trim());
        // WYNIK: wpis z wyjątkiem → 2026-01-15T10:30:00Z SEVERE t31.app.db: Błąd zapisu zamówienia | wyjątek: java.lang.IllegalStateException: brak połączenia

        LogRecord secret = new LogRecord(Level.INFO, "Logowanie: login=ala password=tajne123 ip=10.0.0.5");
        secret.setLoggerName("t31.app.web");
        secret.setInstant(FIXED_INSTANT);
        show("wpis z hasłem (zamaskowane)", formatter.format(secret).trim());
        // WYNIK: wpis z hasłem (zamaskowane) → 2026-01-15T10:30:00Z INFO t31.app.web: Logowanie: login=ala password=*** ip=10.0.0.5
        // Maskowanie to OSTATNIA linia obrony. Najlepiej w ogóle nie logować haseł, tokenów, numerów
        // kart ani danych osobowych. Dlaczego: logi trafiają do plików, kopii zapasowych i systemów
        // zbiorczych, gdzie zagląda wiele osób — a przepisy (RODO) traktują dane osobowe w logach
        // tak samo jak w bazie.

        // Metoda formatMessage(record) z klasy Formatter wstawia parametry {0}, {1} do szablonu.
        show("formatMessage", formatter.formatMessage(record));
        // WYNIK: formatMessage → Zalogowano użytkownika ala
        // Reguły z JUL: szablon jest przetwarzany przez MessageFormat tylko, gdy zawiera "{0" .. "{3",
        // a parametry są podane. Dlatego komunikat z nawiasami, ale bez {0}, idzie dosłownie.

        // PUŁAPKA: DateTimeFormatter / SimpleDateFormat utworzony w formaterze — zwróć uwagę na
        // wątkowość. Dlaczego: formatery logów są wołane z wielu wątków naraz. DateTimeFormatter
        // jest niezmienny i wątkowo bezpieczny; stary SimpleDateFormat NIE jest.

        // PUŁAPKA: pobieranie czasu przez Instant.now() w formaterze zamiast record.getInstant().
        // Dlaczego: wpis powstaje w jednym momencie, a formatowany jest chwilę później (zwłaszcza
        // przy buforowaniu lub asynchronicznych handlerach) — czas w linii byłby nieprawdziwy.

        // DOBRA PRAKTYKA: czas w UTC w formacie ISO-8601 (2026-01-15T10:30:00Z) — jednoznaczny,
        // sortowalny tekstowo i czytelny dla narzędzi do analizy logów, niezależnie od strefy serwera.
    }

    // =================================================================================================
    // 7. MDC — KONTEKST WĄTKU
    // =================================================================================================

    /**
     * 7. MDC (mapped diagnostic context = mapowany kontekst diagnostyczny) to "pieczątka" przypięta do
     * wątku: np. identyfikator żądania, który formatter dopisuje do KAŻDEJ linii. Dzięki niemu
     * z tysięcy wpisów odfiltrujesz te, które dotyczą jednego żądania. Biblioteki (SLF4J, Log4j)
     * mają gotowe MDC; tu własna, mała wersja oparta na {@code ThreadLocal}.
     */
    static void mdcContext() throws IOException {
        section("7. Własne MDC: kontekst przypięty do wątku");

        Logger logger = APP;
        logger.info("bez kontekstu");
        Mdc.runWith("requestId", "r-42", () -> {
            logger.info("początek obsługi żądania");
            Mdc.runWith("user", "ala", () -> logger.info("w środku, z dwoma polami kontekstu"));
            logger.info("po wyjściu z zagnieżdżenia zostało tylko requestId");
        });
        logger.info("po zakończeniu kontekst znika");
        // WYNIK: INFO t31.app: bez kontekstu
        // WYNIK: INFO [requestId=r-42] t31.app: początek obsługi żądania
        // WYNIK: INFO [requestId=r-42,user=ala] t31.app: w środku, z dwoma polami kontekstu
        // WYNIK: INFO [requestId=r-42] t31.app: po wyjściu z zagnieżdżenia zostało tylko requestId
        // WYNIK: INFO t31.app: po zakończeniu kontekst znika

        // PUŁAPKA: kontekst NIE przechodzi do nowych wątków.
        // Dlaczego: ThreadLocal jest przypisany do jednego wątku. Zadanie wysłane do puli wątków
        // albo do new Thread() zaczyna z pustym kontekstem — wpis traci identyfikator żądania.
        // Rozwiązanie: skopiować kontekst (Mdc.snapshot()) i przywrócić go na początku zadania.
        Mdc.runWith("requestId", "r-43", () -> {
            Thread thread = new Thread(() -> logger.info("wpis z innego wątku — kontekstu brak"));
            thread.start();
            joinQuietly(thread);
            Map<String, String> copy = Mdc.snapshot();
            Thread withCopy = new Thread(() -> {
                Mdc.runAll(copy, () -> logger.info("wpis z innego wątku — kontekst skopiowany"));
            });
            withCopy.start();
            joinQuietly(withCopy);
        });
        // WYNIK: INFO t31.app: wpis z innego wątku — kontekstu brak
        // WYNIK: INFO [requestId=r-43] t31.app: wpis z innego wątku — kontekst skopiowany

        // PUŁAPKA: wątki z puli są ponownie używane. Dlaczego: jeśli ustawisz kontekst i nie
        // posprzątasz (finally!), następne zadanie na tym samym wątku odziedziczy cudzy requestId
        // i logi pomieszają sprawy różnych użytkowników. runWith ma blok finally, który przywraca stan.
        show("kontekst po zakończeniu", Mdc.snapshot());
        // WYNIK: kontekst po zakończeniu → {}

        // SLF4J: org.slf4j.MDC.put("requestId", id); w wzorcu Logbacka: %X{requestId};
        // po zakończeniu żądania MDC.clear() w filtrze serwletu (w Springu zajmują się tym gotowe filtry).
        // Wątki wirtualne (Java 21+) też mają ThreadLocal, ale tworzenie milionów wątków czyni
        // go droższym — nowsze rozwiązania to ScopedValue (podgląd w nowszych wersjach).

        // DOBRA PRAKTYKA: identyfikator korelacji (requestId, traceId) ustawiaj raz na brzegu
        // systemu (filtr HTTP, konsument kolejki) i przekazuj dalej w nagłówku do innych usług.
        // Dlaczego: jedno wyszukanie w logach pokazuje całą drogę żądania przez wiele usług.
    }

    private static void joinQuietly(Thread thread) {
        try {
            thread.join(); // join = poczekaj na zakończenie wątku
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // przywróć flagę przerwania
        }
    }

    // =================================================================================================
    // 8. LOGI STRUKTURALNE
    // =================================================================================================

    /**
     * 8. Logi strukturalne to wpisy o stałym układzie pól (key=value albo JSON w jednej linii), które
     * narzędzia (Elasticsearch, Loki, CloudWatch) potrafią przeszukiwać i zliczać po polach.
     */
    static void structuredLogging() {
        section("8. Logi strukturalne: key=value i JSON w linii");

        // Najprostsza forma: stała treść + pola klucz=wartość.
        String keyValue = "zamowienie_zlozone id=42 klient=ala kwota=1234.50";
        show("key=value", keyValue);
        // WYNIK: key=value → zamowienie_zlozone id=42 klient=ala kwota=1234.50

        // Pełny JSON: jeden obiekt na linię ("JSON Lines") — łatwy do wczytania linia po linii.
        Map<String, Object> fields = new LinkedHashMap<>(); // LinkedHashMap = zachowuje kolejność dodania
        fields.put("ts", FIXED_INSTANT.toString());
        fields.put("level", "INFO");
        fields.put("logger", "t31.app.web");
        fields.put("msg", "zamowienie_zlozone");
        fields.put("orderId", 42);
        fields.put("customer", "ala");
        fields.put("paid", true);
        show("JSON w linii", toJsonLine(fields));
        // WYNIK: JSON w linii → {"ts":"2026-01-15T10:30:00Z","level":"INFO","logger":"t31.app.web","msg":"zamowienie_zlozone","orderId":42,"customer":"ala","paid":true}

        // Znaki specjalne muszą być "uciekane": cudzysłów, odwrotny ukośnik, nowa linia.
        Map<String, Object> tricky = new LinkedHashMap<>();
        tricky.put("msg", "Klient powiedział: \"hej\"\nnowa linia i tab\t.");
        tricky.put("path", "C:\\temp\\plik");
        show("znaki specjalne", toJsonLine(tricky));
        // WYNIK: znaki specjalne → {"msg":"Klient powiedział: \"hej\"\nnowa linia i tab\t.","path":"C:\\temp\\plik"}

        // ZASADY:
        //  • Treść wiadomości ("msg") zostaje STAŁA, a zmienne dane idą w osobne pola. Dlaczego:
        //    po stałym "zamowienie_zlozone" można policzyć zamówienia; po tekście "Zamówienie 42
        //    złożone przez ala" — nie, bo każdy wpis jest inny.
        //  • Nazwy pól ustal raz (ts, level, logger, msg, requestId) i nie zmieniaj.
        //  • Jedna linia = jeden wpis. Ślad stosu wstaw jako pole tekstowe z uciekaną nową linią —
        //    wielolinijkowy ślad stosu rozbija wpis na kawałki.
        //  • Nie loguj danych wrażliwych — ani w JSON, ani w tekście.

        // PUŁAPKA: ręcznie sklejany JSON bez ucieczki znaków.
        // Dlaczego: jeden cudzysłów w nazwie klienta ("Jan "Kowal" Nowak") psuje całą linię,
        // a przy tekstach wprowadzanych przez użytkownika bywa to też wektorem ataku (wstrzyknięcie
        // fałszywych wpisów do logu przez znak nowej linii). Zawsze uciekaj tekst — jak tu w jsonEscape
        // — albo użyj biblioteki (Jackson, Logstash-encoder), która robi to poprawnie i wydajnie.

        // DOBRA PRAKTYKA: w produkcji użyj gotowego "encodera JSON" do Logbacka lub Log4j 2 zamiast
        // własnego kodu — ten z tej lekcji służy do zrozumienia, jak to działa w środku.
        // Dlaczego: biblioteka obsłuży ślady stosu, MDC, typy liczbowe i zagnieżdżone obiekty.
    }

    /** Jeden wpis jako obiekt JSON w jednej linii. Wartości: liczby i wartości logiczne bez cudzysłowów. */
    static String toJsonLine(Map<String, Object> fields) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(jsonEscape(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean) { // instanceof = "jest typu"
                sb.append(value);
            } else {
                sb.append('"').append(jsonEscape(String.valueOf(value))).append('"');
            }
        }
        return sb.append('}').toString();
    }

    /** Uciekanie znaków specjalnych JSON (cudzysłów, ukośnik, znaki sterujące). */
    static String jsonEscape(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c)); // znaki sterujące jako kod
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    // =================================================================================================
    // 9. POZIOMY I WYDAJNOŚĆ
    // =================================================================================================

    /** Metoda "droga" — liczy się, ile razy ją wywołano. */
    private static String expensive(AtomicInteger counter) {
        counter.incrementAndGet();
        return "stan";
    }

    /**
     * 9. Wyłączone logowanie powinno kosztować prawie nic. Problem: argumenty są liczone ZANIM metoda
     * loggera sprawdzi poziom. Pokazujemy to licznikiem wywołań metody "drogiej".
     */
    static void levelsAndPerformance() {
        section("9. Poziomy i koszt logowania: sklejanie, Supplier, isLoggable");

        System.Logger logger = System.getLogger("t31.demo.perf"); // poziom INFO, więc DEBUG wyłączony
        AtomicInteger counter = new AtomicInteger();

        logger.log(System.Logger.Level.DEBUG, "Stan: " + expensive(counter)); // sklejanie tekstu
        int concatenated = counter.getAndSet(0);
        logger.log(System.Logger.Level.DEBUG, () -> "Stan: " + expensive(counter)); // Supplier (dostawca)
        int lazy = counter.getAndSet(0);
        if (logger.isLoggable(System.Logger.Level.DEBUG)) { // strażnik (guard)
            logger.log(System.Logger.Level.DEBUG, "Stan: " + expensive(counter));
        }
        int guarded = counter.getAndSet(0);
        logger.log(System.Logger.Level.DEBUG, "Stan: {0}", expensive(counter)); // parametr {0}
        int parameter = counter.getAndSet(0);

        show("wywołania expensive() przy wyłączonym DEBUG — sklejanie", concatenated);
        // WYNIK: wywołania expensive() przy wyłączonym DEBUG — sklejanie → 1
        show("— Supplier", lazy);
        // WYNIK: — Supplier → 0
        show("— isLoggable", guarded);
        // WYNIK: — isLoggable → 0
        show("— parametr {0} (argument liczony zawsze)", parameter);
        // WYNIK: — parametr {0} (argument liczony zawsze) → 1
        // Wniosek: sklejanie tekstu i wołanie metod w argumentach wykonuje się ZAWSZE, nawet gdy log
        // jest wyłączony. Supplier i strażnik isLoggable pomijają całą pracę. Parametr {0} oszczędza
        // sklejanie napisu, ale argument i tak jest liczony — przy kosztownych obliczeniach użyj
        // Suppliera. Proste, tanie wartości (zmienna, pole) możesz podać wprost.

        // PUŁAPKA: logowanie w ciasnej pętli (miliony wywołań). Dlaczego: nawet wyłączone, każde
        // wywołanie kosztuje; włączone — zapis na dysk dominuje czas programu. Loguj podsumowania
        // (co 10 000 rekordów) lub używaj próbkowania.

        // POLITYKA POZIOMÓW (kiedy który):
        //   ERROR (SEVERE)  — operacja się nie powiodła i trzeba coś zrobić; ktoś powinien to zobaczyć,
        //   WARNING         — nieoczekiwane, ale program sobie poradził (ponowienie, wartość domyślna),
        //   INFO            — ważne zdarzenia biznesowe i cykl życia (start, zamówienie złożone),
        //                     rzadkie — żeby dało się czytać dziennik, a nie tonąć w nim,
        //   DEBUG (FINE)    — szczegóły do diagnozy: pośrednie wartości, decyzje algorytmu,
        //   TRACE (FINER)   — wejście/wyjście z metod, bardzo szczegółowe; zwykle wyłączone.
        //
        // WYJĄTKI: loguj jeden raz — tam, gdzie je obsługujesz (zwykle na granicy: kontroler,
        // zadanie w tle), razem ze śladem stosu: log(ERROR, "komunikat", e).
        // PUŁAPKA: "loguj i rzucaj dalej". Dlaczego: ten sam błąd pojawia się w logu wiele razy
        // (po jednym na warstwę), a prawdziwa przyczyna ginie w powtórzeniach (t10_exceptions/Exceptions07BestPractices).
        // PUŁAPKA: log(ERROR, e.getMessage()) bez wyjątku. Dlaczego: tracisz ślad stosu i typ
        // błędu; często getMessage() jest nawet null.

        // DOBRA PRAKTYKA: zadaj sobie pytanie "kto to przeczyta i co zrobi?". Wpis ERROR, na który nikt
        // nie zareaguje, uczy ludzi ignorować ERROR-y. Dlaczego: alarmy mają sens tylko wtedy, gdy
        // każdy z nich oznacza realny problem.
    }

    // =================================================================================================
    // 10. LOGBACK W SPRING BOOT
    // =================================================================================================

    /** Przykładowa konfiguracja Logbacka dla Spring Boot (plik logback-spring.xml w katalogu zasobów). */
    private static final String LOGBACK_SAMPLE = """
            <configuration>
              <property name="LOG_PATTERN" value="%d{HH:mm:ss.SSS} %-5level [%X{requestId}] %logger{36} - %msg%n"/>
              <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
                <encoder><pattern>${LOG_PATTERN}</pattern></encoder>
              </appender>
              <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
                <file>logs/app.log</file>
                <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
                  <fileNamePattern>logs/app.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
                  <maxFileSize>10MB</maxFileSize>
                  <maxHistory>14</maxHistory>
                  <totalSizeCap>1GB</totalSizeCap>
                </rollingPolicy>
                <encoder><pattern>${LOG_PATTERN}</pattern></encoder>
              </appender>
              <springProfile name="prod">
                <root level="INFO"><appender-ref ref="FILE"/></root>
              </springProfile>
              <springProfile name="!prod">
                <logger name="com.example" level="DEBUG"/>
                <root level="INFO"><appender-ref ref="CONSOLE"/></root>
              </springProfile>
            </configuration>
            """;

    /**
     * 10. W projektach Spring Boot logowanie to SLF4J (interfejs, "fasada") + Logback (silnik).
     * Interfejs SLF4J wygląda inaczej niż JUL, ale zasady te same: hierarchia, poziomy, handlery
     * (tu: "appendery"), formatery (tu: "encodery" z wzorcem).
     */
    static void logbackInSpring() {
        section("10. SLF4J + Logback w Spring Boot (opis i przykład konfiguracji)");

        // Kod w klasie (nie kompilujemy go tu, bo SLF4J nie jest zależnością tego kursu):
        //   private static final Logger log = LoggerFactory.getLogger(OrderService.class);
        //   log.info("Zamówienie {} złożone przez {}", orderId, user);   // placeholdery {} (bez numeru!)
        //   log.error("Nie udało się zapisać zamówienia {}", orderId, e); // wyjątek jako OSTATNI argument
        //   if (log.isDebugEnabled()) { ... }                              // strażnik, jak isLoggable
        // Różnice względem JUL: placeholder to {} (nie {0}), nie ma problemu z apostrofem, a poziomy
        // to TRACE, DEBUG, INFO, WARN, ERROR. Wyjątek podany jako ostatni argument dostaje ślad stosu.

        // Najprostsza konfiguracja w Spring Boot — application.properties, bez XML:
        //   logging.level.root=INFO
        //   logging.level.com.example=DEBUG        (poziom pakietu, jak t31.app.level w JUL)
        //   logging.file.name=logs/app.log
        //   logging.pattern.console=%d{HH:mm:ss} %-5level %logger{20} - %msg%n
        // Dla pełnej kontroli: plik logback-spring.xml (wariant "-spring" rozumie profile Springa).
        for (String line : LOGBACK_SAMPLE.lines().toList()) {
            System.out.println("   " + line);
        }
        // WYNIK: <configuration>
        // WYNIK: <property name="LOG_PATTERN" value="%d{HH:mm:ss.SSS} %-5level [%X{requestId}] %logger{36} - %msg%n"/>
        // WYNIK: <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        // WYNIK: <encoder><pattern>${LOG_PATTERN}</pattern></encoder>
        // WYNIK: </appender>
        // WYNIK: <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        // WYNIK: <file>logs/app.log</file>
        // WYNIK: <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        // WYNIK: <fileNamePattern>logs/app.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        // WYNIK: <maxFileSize>10MB</maxFileSize>
        // WYNIK: <maxHistory>14</maxHistory>
        // WYNIK: <totalSizeCap>1GB</totalSizeCap>
        // WYNIK: </rollingPolicy>
        // WYNIK: <encoder><pattern>${LOG_PATTERN}</pattern></encoder>
        // WYNIK: </appender>
        // WYNIK: <springProfile name="prod">
        // WYNIK: <root level="INFO"><appender-ref ref="FILE"/></root>
        // WYNIK: </springProfile>
        // WYNIK: <springProfile name="!prod">
        // WYNIK: <logger name="com.example" level="DEBUG"/>
        // WYNIK: <root level="INFO"><appender-ref ref="CONSOLE"/></root>
        // WYNIK: </springProfile>
        // WYNIK: </configuration>
        // Co tu widać: appender CONSOLE i FILE (z rotacją według dnia i rozmiaru, kompresją .gz,
        // limitem historii i łącznego rozmiaru), wzorzec linii z polem MDC %X{requestId},
        // oraz profile: na produkcji tylko plik, lokalnie konsola i poziom DEBUG dla własnego pakietu.

        // MOSTY między systemami logowania (żeby wszystko trafiało w jedno miejsce):
        //   jul-to-slf4j                   — wpisy z java.util.logging przekazuje do SLF4J,
        //   slf4j-jdk-platform-logging     — System.Logger → SLF4J,
        //   log4j-over-slf4j, jcl-over-slf4j — stare biblioteki Log4j 1 i Commons Logging → SLF4J.
        // Spring Boot dołącza większość z nich sam (starter spring-boot-starter-logging).

        // PUŁAPKA: kilka silników logowania naraz (Logback + Log4j 2 + slf4j-simple) na ścieżce klas.
        // Dlaczego: SLF4J wybiera jeden, wypisuje ostrzeżenie "multiple bindings" i zachowanie zależy
        // od kolejności; część logów może zniknąć. Zostaw jeden silnik, resztę wyłącz w zależnościach.

        // PUŁAPKA: własny plik logback.xml (bez -spring) czytany zbyt wcześnie.
        // Dlaczego: nie widzi profili ani właściwości Springa. Dla Spring Boot używaj logback-spring.xml.

        // DOBRA PRAKTYKA: ten sam układ co w tej lekcji: logger na klasę, poziom w konfiguracji,
        // MDC z identyfikatorem żądania, format strukturalny (JSON) na produkcji, tekstowy lokalnie.
        // Dlaczego: logi czyta człowiek przy programowaniu i maszyna na produkcji.
    }

    // =================================================================================================
    // KLASY POMOCNICZE: HANDLER, FORMATTERY, MDC
    // =================================================================================================

    /**
     * Handler piszący na System.out (a NIE na System.err jak domyślny ConsoleHandler). Zamyka się
     * bez zamykania System.out — close() tylko opróżnia bufor. Publiczna, bo LogManager tworzy
     * klasy z konfiguracji przez refleksję.
     */
    public static class StdoutHandler extends Handler {
        public StdoutHandler(String tag) {
            setFormatter(new LineFormatter(tag));
            setLevel(Level.ALL);
        }

        @Override
        public void publish(LogRecord record) {
            if (!isLoggable(record)) { // sprawdza poziom handlera i jego filtr
                return;
            }
            System.out.print(getFormatter().format(record));
            System.out.flush();
        }

        @Override
        public void flush() {
            System.out.flush();
        }

        @Override
        public void close() {
            flush(); // NIE zamykamy System.out!
        }
    }

    /** Linia: [znacznik] POZIOM [kontekst] logger: komunikat (| wyjątek). Bez czasu — stała w testach. */
    public static class LineFormatter extends Formatter {
        private final String tag;

        /** Konstruktor bez argumentów jest potrzebny, gdy LogManager tworzy formatter z pliku konfiguracyjnego. */
        public LineFormatter() {
            this("");
        }

        public LineFormatter(String tag) {
            this.tag = tag;
        }

        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            if (!tag.isEmpty()) {
                sb.append('[').append(tag).append("] ");
            }
            sb.append(record.getLevel().getName()).append(' ');
            String context = Mdc.asText();
            if (!context.isEmpty()) {
                sb.append('[').append(context).append("] ");
            }
            sb.append(record.getLoggerName()).append(": ").append(formatMessage(record));
            if (record.getThrown() != null) {
                sb.append(" | wyjątek: ").append(record.getThrown().getClass().getSimpleName())
                        .append(": ").append(record.getThrown().getMessage());
            }
            return sb.append('\n').toString();
        }
    }

    /** Linia z czasem ISO z samego wpisu i z zamaskowanymi hasłami. */
    public static class TimestampFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String message = redactPasswords(formatMessage(record));
            StringBuilder sb = new StringBuilder();
            sb.append(DateTimeFormatter.ISO_INSTANT.format(record.getInstant())).append(' ')
                    .append(record.getLevel().getName()).append(' ')
                    .append(record.getLoggerName()).append(": ").append(message);
            if (record.getThrown() != null) {
                sb.append(" | wyjątek: ").append(record.getThrown());
            }
            return sb.append('\n').toString();
        }
    }

    /** Zamienia wartość po "password=" na ***. */
    static String redactPasswords(String text) {
        return text.replaceAll("(?i)(password)=\\S+", "$1=***");
    }

    /** Własne MDC: mapa pól przypięta do wątku (ThreadLocal = zmienna osobna dla każdego wątku). */
    static final class Mdc {
        private static final ThreadLocal<Map<String, String>> CONTEXT = ThreadLocal.withInitial(LinkedHashMap::new);

        private Mdc() { }

        /** Wykonuje akcję z dodatkowym polem kontekstu; po wyjściu przywraca poprzedni stan. */
        static void runWith(String key, String value, Runnable action) {
            Map<String, String> map = CONTEXT.get();
            String previous = map.put(key, value);
            try {
                action.run();
            } finally {
                if (previous == null) {
                    map.remove(key);
                } else {
                    map.put(key, previous);
                }
            }
        }

        /** Przywraca skopiowany kontekst w bieżącym wątku na czas akcji. */
        static void runAll(Map<String, String> copy, Runnable action) {
            Map<String, String> map = CONTEXT.get();
            Map<String, String> backup = new LinkedHashMap<>(map);
            map.putAll(copy);
            try {
                action.run();
            } finally {
                map.clear();
                map.putAll(backup);
            }
        }

        static Map<String, String> snapshot() {
            return new LinkedHashMap<>(CONTEXT.get()); // kopia: zmiany później nie wpłyną na wynik
        }

        static String asText() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : CONTEXT.get().entrySet()) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(e.getKey()).append('=').append(e.getValue());
            }
            return sb.toString();
        }
    }

    /**
     * Konfiguracja bazowa: reset LogManagera i jeden handler korzenia piszący na System.out.
     * reset() usuwa domyślny ConsoleHandler (który pisze na System.err) i zeruje poziomy loggerów.
     */
    static void setupStdout() {
        LogManager.getLogManager().reset();
        Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);
        root.addHandler(new StdoutHandler(""));
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • System.getLogger(nazwa).log(Level.INFO, "tekst {0}", arg) — interfejs z JDK 9+; zaplecze to
     *     domyślnie JUL (poziomy: TRACE=FINER, DEBUG=FINE, ERROR=SEVERE); przez LoggerFinder można
     *     przekierować do SLF4J.
     *   • JUL: wpis musi przejść poziom loggera ORAZ poziom handlera; ConsoleHandler pisze na System.err
     *     z poziomem INFO; setUseParentHandlers(false) przeciw dublowaniu.
     *   • Loggery JUL trzymaj w polach statycznych (LogManager trzyma je słabo).
     *   • logging.properties: handlers, .level, nazwa.level, handler.pattern/formatter; ścieżki z "/";
     *     readConfiguration i reset() zerują poziomy; po zapisie zamknij handlery (reset).
     *   • Hierarchia po kropkach; getLevel()==null znaczy "dziedziczy"; loggery nazywaj nazwą klasy.
     *   • Filter = LogRecord → boolean; szybki; nie zmienia treści. Formatter: czas z record.getInstant().
     *   • MDC = ThreadLocal z polami kontekstu; sprzątaj w finally; do innych wątków kopiuj ręcznie.
     *   • Logi strukturalne: stała treść + pola; JSON w linii z poprawną ucieczką znaków.
     *   • Wydajność: sklejanie i argumenty liczą się zawsze; użyj Supplier lub isLoggable.
     *   • Poziomy: ERROR wymaga reakcji, WARNING "poradziłem sobie", INFO zdarzenia biznesowe,
     *     DEBUG diagnoza; wyjątek loguj raz, ze śladem stosu; nie loguj sekretów.
     *   • Spring Boot: SLF4J + Logback; {} zamiast {0}; application.properties lub logback-spring.xml.
     *
     * PYTANIA KONTROLNE:
     *   1. Którym poziomem JUL jest System.Logger.Level.DEBUG, a którym ERROR?
     *   2. Ustawiłeś poziom loggera na FINE i handler zostawiłeś domyślny. Dlaczego FINE nie widać?
     *   3. Co wypisze:  Logger.getLogger("a.b").getLevel()  jeśli nikt nie ustawiał poziomu?
     *   4. ZNAJDŹ BŁĄD:  logger.log(System.Logger.Level.DEBUG, "Stan: " + repository.loadAll().size());
     *      (loadAll() pobiera całą tabelę z bazy)
     *   5. Dlaczego domyślny ConsoleHandler bywa kłopotliwy w programach uruchamianych z skryptów
     *      i w testach, w których sprawdzasz System.out?
     *   6. Co zostanie z wpisu  logger.log(INFO, "Użytkownik O'Brien: {0}", "x")  i dlaczego?
     *   7. Dlaczego kontekst MDC ustawiony w wątku obsługi żądania nie widnieje w logach zadania
     *      wysłanego do puli wątków? Jak temu zaradzić?
     *   8. ZNAJDŹ BŁĄD:  try { zapisz(); } catch (IOException e) { logger.log(ERROR, e.getMessage()); throw e; }
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================

    static void exercises() {
        prepareExerciseLoggers();
        section("ĆWICZENIA — Twoje rozwiązania (✘ = jeszcze do zrobienia)");
        Check.equal("ćw. 1: key=value z cudzysłowem", "level=INFO msg=\"zamowienie zlozone\" id=42",
                () -> exercise1(sampleFields()));
        Check.equal("ćw. 1: wartość z = i pusta", "a=\"x=y\" b=\"\" c=\"z\\\"q\"",
                () -> exercise1(trickyFields()));
        Check.equal("ćw. 2: efektywne poziomy", List.of("WARNING", "WARNING", "WARNING", "FINE"),
                () -> List.of(exercise2(EX_ROOT), exercise2(EX_A), exercise2(EX_AB), exercise2(EX_C)));
        Check.equal("ćw. 3: maskowanie", "login=ala password=*** token=*** ok",
                () -> exercise3("login=ala password=tajne123 token=abc.def ok"));
        Check.equal("ćw. 3: wielkość liter", "Haslo=*** secret=***",
                () -> exercise3("Haslo=xyz secret=1234"));
        Check.equal("ćw. 4: DEBUG wyłączony", 0, () -> lazyCalls(false, Level.INFO));
        Check.equal("ćw. 4: DEBUG włączony", 1, () -> lazyCalls(false, Level.FINE));
        Check.summary();

        section("ĆWICZENIA — rozwiązania wzorcowe");
        Check.equal("ćw. 1: key=value z cudzysłowem", "level=INFO msg=\"zamowienie zlozone\" id=42",
                () -> solution1(sampleFields()));
        Check.equal("ćw. 1: wartość z = i pusta", "a=\"x=y\" b=\"\" c=\"z\\\"q\"",
                () -> solution1(trickyFields()));
        Check.equal("ćw. 2: efektywne poziomy", List.of("WARNING", "WARNING", "WARNING", "FINE"),
                () -> List.of(solution2(EX_ROOT), solution2(EX_A), solution2(EX_AB), solution2(EX_C)));
        Check.equal("ćw. 3: maskowanie", "login=ala password=*** token=*** ok",
                () -> solution3("login=ala password=tajne123 token=abc.def ok"));
        Check.equal("ćw. 3: wielkość liter", "Haslo=*** secret=***",
                () -> solution3("Haslo=xyz secret=1234"));
        Check.equal("ćw. 4: DEBUG wyłączony", 0, () -> lazyCalls(true, Level.INFO));
        Check.equal("ćw. 4: DEBUG włączony", 1, () -> lazyCalls(true, Level.FINE));
        Check.summary();
        // WYNIK: PODSUMOWANIE: ✔ 7 OK, ✘ 0 BŁĄD
    }

    /** Ustawia poziomy loggerów potrzebnych w ćwiczeniu 2 (drzewo t31.ex). */
    private static void prepareExerciseLoggers() {
        EX_ROOT.setLevel(Level.WARNING);
        EX_C.setLevel(Level.FINE);
        // EX_A i EX_AB nie mają własnego poziomu — dziedziczą po t31.ex
    }

    private static Map<String, Object> sampleFields() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("level", "INFO");
        map.put("msg", "zamowienie zlozone");
        map.put("id", 42);
        return map;
    }

    private static Map<String, Object> trickyFields() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", "x=y");
        map.put("b", "");
        map.put("c", "z\"q");
        return map;
    }

    /** Wywołuje ćwiczenie 4 przy podanym poziomie loggera i zwraca, ile razy policzono "drogą" wartość. */
    private static int lazyCalls(boolean reference, Level julLevel) {
        Logger jul = Logger.getLogger("t31.ex.lazy");
        jul.setLevel(julLevel);
        System.Logger logger = System.getLogger("t31.ex.lazy");
        AtomicInteger counter = new AtomicInteger();
        if (reference) {
            solution4(logger, counter);
        } else {
            exercise4(logger, counter);
        }
        return counter.get();
    }

    /**
     * ĆWICZENIE 1 (łatwe): zamień mapę pól na tekst {@code klucz=wartość} oddzielony spacjami
     * (kolejność z mapy). Wartość zawierającą spację, cudzysłów lub znak '=' albo pustą ujmij w
     * cudzysłowy, a cudzysłów w środku poprzedź odwrotnym ukośnikiem.
     * Przykład: {@code level=INFO msg="zamowienie zlozone" id=42}.
     * Podpowiedź: String.valueOf(wartość), replace("\"", "\\\"") i StringJoiner lub StringBuilder.
     */
    static String exercise1(Map<String, Object> fields) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć nazwę EFEKTYWNEGO poziomu loggera (własnego albo odziedziczonego
     * po najbliższym przodku, który go ustawił). Nie używaj isLoggable.
     * Podpowiedź: pętla {@code for (Logger l = logger; l != null; l = l.getParent())}.
     */
    static String exercise2(Logger logger) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zamaskuj wartości pól password, haslo, token i secret w tekście
     * (wielkość liter nie ma znaczenia): {@code password=tajne123} → {@code password=***}.
     * Nazwa pola ma zostać taka, jaka była. Podpowiedź: replaceAll z (?i) i grupą $1.
     */
    static String exercise3(String text) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): PRZEPISZ wywołanie tak, aby "droga" metoda nie była liczona, gdy DEBUG
     * jest wyłączony. Stary kod:
     * <pre>{@code
     * logger.log(System.Logger.Level.DEBUG, "Stan: " + expensive(counter));
     * }</pre>
     * Podpowiedź: wersja log z dostawcą (Supplier): {@code logger.log(poziom, () -> ...)}.
     */
    static void exercise4(System.Logger logger, AtomicInteger counter) {
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ROZWIĄZANIA WZORCOWE — zajrzyj dopiero po własnej próbie!" defaultstate="collapsed">

    static String solution1(Map<String, Object> fields) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            String value = String.valueOf(entry.getValue());
            boolean needsQuotes = value.isEmpty() || value.contains(" ") || value.contains("\"") || value.contains("=");
            sb.append(entry.getKey()).append('=');
            if (needsQuotes) {
                sb.append('"').append(value.replace("\"", "\\\"")).append('"');
            } else {
                sb.append(value);
            }
        }
        return sb.toString();
    }

    static String solution2(Logger logger) {
        for (Logger current = logger; current != null; current = current.getParent()) {
            if (current.getLevel() != null) {
                return current.getLevel().getName();
            }
        }
        return "INFO";
    }

    static String solution3(String text) {
        return text.replaceAll("(?i)(password|haslo|token|secret)=\\S+", "$1=***");
    }

    static void solution4(System.Logger logger, AtomicInteger counter) {
        logger.log(System.Logger.Level.DEBUG, () -> "Stan: " + expensive(counter));
    }

    // </editor-fold>

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. DEBUG to FINE (500), ERROR to SEVERE (1000). TRACE odpowiada FINER (400).
     *   2. Domyślny handler ma poziom INFO i odrzuca FINE; wpis musi przejść poziom loggera ORAZ
     *      handlera. Trzeba obniżyć oba.
     *   3. null — logger nie ma własnego poziomu i dziedziczy po przodku.
     *   4. Metoda loadAll() zostanie wykonana (i pobierze całą tabelę) ZAWSZE, bo argument jest liczony
     *      przed sprawdzeniem poziomu. Użyj Suppliera albo isLoggable.
     *   5. Pisze na System.err (nie na System.out), z własnym formatem i poziomem INFO — w skryptach
     *      i testach, które czytają standardowe wyjście, logi nie trafiają tam, gdzie się ich spodziewasz,
     *      a konfiguracji nie widać w kodzie.
     *   6. "Użytkownik OBrien: {0}" — z parametrami tekst idzie przez MessageFormat: apostrof zaczyna
     *      cytowanie (zostaje usunięty), a {0} nie jest rozwijane. Trzeba ''.
     *   7. ThreadLocal jest przypisany do jednego wątku; zadanie w puli wykonuje się w innym wątku.
     *      Skopiuj kontekst (snapshot) przy tworzeniu zadania i przywróć go na początku zadania
     *      (z posprzątaniem w finally).
     *   8. Wyjątek jest logowany i rzucany dalej — ten sam błąd trafi do logów wiele razy; a e.getMessage()
     *      bez wyjątku gubi ślad stosu i typ. Loguj raz, na granicy, z wyjątkiem: log(ERROR, "komunikat", e).
     */
    // </editor-fold>
}
